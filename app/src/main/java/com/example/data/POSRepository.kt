package com.example.data

import android.content.Context
import com.example.data.gemini.GeminiClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

class POSRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    val productDao = database.productDao()
    val transactionDao = database.transactionDao()
    val staffDao = database.staffDao()
    val shiftDao = database.shiftDao()

    // Moshi for serializing list of cart products bought
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val cartListType = Types.newParameterizedType(List::class.java, CartProduct::class.java)
    private val cartAdapter = moshi.adapter<List<CartProduct>>(cartListType)

    // Simulating internet connectivity (interactive toggle in UI)
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    // Syncing state
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Logs of synchronization activities shown to user in real-time monospace terminal console!
    private val _syncLogs = MutableStateFlow<List<String>>(
        listOf("SYSTEM INITIALIZED: Terminal Online. Core diagnostic OK.")
    )
    val syncLogs: StateFlow<List<String>> = _syncLogs.asStateFlow()

    fun log(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        _syncLogs.value = listOf("[$timestamp] $message") + _syncLogs.value.take(49)
    }

    fun setNetworkOnline(online: Boolean) {
        _isOnline.value = online
        if (online) {
            log("NETWORK RESTORED: Cloud sync services connected.")
        } else {
            log("NETWORK OFFLINE: Local transactions will queue in fail-safe sqlite terminal.")
        }
    }

    suspend fun initializeData() = withContext(Dispatchers.IO) {
        // Pre-populate core staff
        val existingStaff = staffDao.getStaffById("STF-001")
        if (existingStaff == null) {
            log("PRE-POPULATING SYSTEMS: Initializing users, credentials & catalog...")
            val defaultStaff = listOf(
                StaffUser("STF-001", "Tom (Admin)", "ADMIN", "1111"),
                StaffUser("STF-002", "Alice (Manager)", "MANAGER", "2222"),
                StaffUser("STF-003", "Bob (Cashier)", "CASHIER", "1234")
            )
            staffDao.insertAll(defaultStaff)
        }

        // Pre-populate catalog
        val allProductsFlow = productDao.getAllProducts()
        val existingCount = withContext(Dispatchers.IO) {
            // Check if product list is empty
            var count = 0
            val pList = database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM products")
            if (pList.moveToFirst()) {
                count = pList.getInt(0)
            }
            pList.close()
            count
        }

        if (existingCount == 0) {
            val defaultProducts = listOf(
                Product(name = "Flat White", sku = "BEV-FLAT", barcode = "1001", price = 3.80, costPrice = 0.60, stockQuantity = 95, category = "Beverages", variants = "Standard, Oat Milk, Almond Milk"),
                Product(name = "Espresso Shot", sku = "BEV-ESP", barcode = "1002", price = 2.50, costPrice = 0.40, stockQuantity = 120, category = "Beverages", variants = "Solo, Doppio"),
                Product(name = "Sourdough Croissant", sku = "BAK-CRO", barcode = "1003", price = 4.00, costPrice = 1.10, stockQuantity = 12, category = "Bakery", variants = "Regular, Warmed"),
                Product(name = "Avocado Toast", sku = "FUD-AVO", barcode = "1004", price = 8.50, costPrice = 2.50, stockQuantity = 18, category = "Food", variants = "Chili Flakes, Poached Egg"),
                Product(name = "Cyberpunk Energy Soda", sku = "BEV-CYB", barcode = "1005", price = 4.50, costPrice = 1.20, stockQuantity = 4, category = "Beverages"), // low stock
                Product(name = "Monospace Cap", sku = "APP-MCAP", barcode = "1006", price = 22.00, costPrice = 7.00, stockQuantity = 25, category = "Merchandise", variants = "Dark Steel, Retro Tan"),
                Product(name = "Thermal Rolls (10 pk)", sku = "SUP-ROLL", barcode = "1007", price = 15.00, costPrice = 4.00, stockQuantity = 8, category = "Supplies"),
                Product(name = "Bitcoin Ledger POS", sku = "HRD-BTC", barcode = "1008", price = 49.00, costPrice = 20.00, stockQuantity = 15, category = "Hardware")
            )
            productDao.insertAll(defaultProducts)
            log("POS CATALOG: Inserted default stock items & categories successfully.")
        }
    }

    // Process a POS sale
    suspend fun processSale(
        cartItems: List<CartProduct>,
        paymentMethod: String,
        discountAmount: Double,
        taxPercent: Double,
        staffId: String,
        staffName: String
    ): Transaction = withContext(Dispatchers.IO) {
        val totalBeforeTax = cartItems.sumOf { it.price * it.quantity } - discountAmount
        val taxAmount = totalBeforeTax * (taxPercent / 100.0)
        val finalAmount = totalBeforeTax + taxAmount

        val itemsJson = cartAdapter.toJson(cartItems) ?: "[]"
        val txnId = "TXN-${System.currentTimeMillis() / 1000}-${(100..999).random()}"

        val txn = Transaction(
            transactionId = txnId,
            timestamp = System.currentTimeMillis(),
            totalAmount = finalAmount,
            taxAmount = taxAmount,
            discountAmount = discountAmount,
            paymentMethod = paymentMethod,
            paymentStatus = "SUCCESS",
            itemsJson = itemsJson,
            staffId = staffId,
            staffName = staffName,
            isOffline = !isOnline.value,
            synced = isOnline.value
        )

        // Atomic transaction insert & stock decrement (offline-first decrement immediately!)
        transactionDao.insertTransaction(txn)
        for (item in cartItems) {
            productDao.decrementStock(item.productId, item.quantity)
        }

        // Accumulate sales for our current active session
        val activeShift = shiftDao.getActiveShift()
        if (activeShift != null) {
            val updatedShift = activeShift.copy(
                totalSales = activeShift.totalSales + finalAmount
            )
            shiftDao.updateShift(updatedShift)
        }

        log("TRANSACTION REGISTERED: $txnId ($paymentMethod). Charge $${String.format("%.2f", finalAmount)}. Stock decremented.")

        // Auto trigger synchronizer task if online
        if (isOnline.value) {
            triggerCloudSync()
        }

        txn
    }

    // Cloud upload sync queue job
    suspend fun triggerCloudSync() = withContext(Dispatchers.IO) {
        if (!isOnline.value) {
            log("SYNC BLOCKED: Device offline. Transactions remain secured in SQLite cache.")
            return@withContext
        }

        val unsynced = transactionDao.getUnsyncedTransactions()
        if (unsynced.isEmpty()) {
            return@withContext
        }

        _isSyncing.value = true
        log("CLOUD SYNC INITIATED: Processing ${unsynced.size} local cached sale logs...")

        try {
            // Simulate network latency & cloud save
            kotlinx.coroutines.delay(1200)
            for (txn in unsynced) {
                transactionDao.markAsSynced(txn.transactionId)
                log("SYNC COMPLETE: Uploaded trans ${txn.transactionId} successfully.")
            }
            log("CLOUD STANDBY: Databases in full synchronization.")
        } catch (e: Exception) {
            log("SYNC ERROR: Link failure: ${e.message}")
        } finally {
            _isSyncing.value = false
        }
    }

    // Process payment refund
    suspend fun processRefund(transaction: Transaction) = withContext(Dispatchers.IO) {
        val updatedTxn = transaction.copy(paymentStatus = "REFUNDED")
        transactionDao.insertTransaction(updatedTxn)

        // Restore stock levels!
        val items = cartAdapter.fromJson(transaction.itemsJson) ?: emptyList()
        for (item in items) {
            productDao.incrementStock(item.productId, item.quantity)
        }

        // Deduct from current active shift
        val activeShift = shiftDao.getActiveShift()
        if (activeShift != null) {
            val updatedShift = activeShift.copy(
                totalSales = (activeShift.totalSales - transaction.totalAmount).coerceAtLeast(0.0)
            )
            shiftDao.updateShift(updatedShift)
        }

        log("REFUND CONFIRMED: ${transaction.transactionId} refunded. Stock restocked. Shift revised.")
    }

    // Ask AI Intelligence Service
    suspend fun getAiSalesIntelligence(customPrompt: String? = null): String {
        log("INTELLIGENCE AUDIT: Prompting Gemini-3.5-flash analytics...")

        // Gather statistics to build professional context
        var totalAmount = 0.0
        var totalTransactions = 0
        var cashSales = 0.0
        var digitalSales = 0.0
        val salesBreakdownItems = mutableMapOf<String, Int>()

        // Get snapshot from database in thread-safe manner
        val allTxns = withContext(Dispatchers.IO) {
            val list = mutableListOf<Transaction>()
            // Using low-level or standard sync list pull, or database query
            // Let's do standard transactionDao retrieval safely
            // (transactions are returned as flows usually, let's query raw count)
            database.openHelper.readableDatabase.query("SELECT * FROM transactions").use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getString(cursor.getColumnIndexOrThrow("transactionId"))
                    val total = cursor.getDouble(cursor.getColumnIndexOrThrow("totalAmount"))
                    val method = cursor.getString(cursor.getColumnIndexOrThrow("paymentMethod"))
                    val itemsJsonStr = cursor.getString(cursor.getColumnIndexOrThrow("itemsJson"))
                    val status = cursor.getString(cursor.getColumnIndexOrThrow("paymentStatus"))

                    if (status == "SUCCESS") {
                        totalAmount += total
                        totalTransactions++
                        if (method == "CASH") cashSales += total else digitalSales += total

                        try {
                            val items = cartAdapter.fromJson(itemsJsonStr) ?: emptyList()
                            for (item in items) {
                                salesBreakdownItems[item.name] = (salesBreakdownItems[item.name] ?: 0) + item.quantity
                            }
                        } catch (e: Exception) {}
                    }
                }
            }
        }

        // Fetch low stock items
        val lowStockList = mutableListOf<String>()
        val productsCatalog = mutableListOf<String>()
        database.openHelper.readableDatabase.query("SELECT * FROM products").use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val stock = cursor.getInt(cursor.getColumnIndexOrThrow("stockQuantity"))
                val price = cursor.getDouble(cursor.getColumnIndexOrThrow("price"))
                val cost = cursor.getDouble(cursor.getColumnIndexOrThrow("costPrice"))
                val limit = cursor.getInt(cursor.getColumnIndexOrThrow("lowStockThreshold"))

                productsCatalog.add("- Product: $name, Stock: $stock, Price: $$price, Cost: $$cost")
                if (stock <= limit) {
                    lowStockList.add("- Item: $name is low on stock! Currently only $stock items left.")
                }
            }
        }

        val contextPrompt = """
            You are "Register AI Assistant", an intelligence terminal embedded inside our retail POS terminal.
            The current local time is: 2026-05-25.
            
            Here's the current live state of our retail business:
            - Connected network state: ${if (isOnline.value) "ONLINE (Fully Cloud Synced)" else "OFFLINE (Queued in-memory)"}
            - Total Cumulative Revenue: $${String.format("%.2f", totalAmount)}
            - Total Completed checkouts: $totalTransactions trans
            - Cash intake: $${String.format("%.2f", cashSales)}
            - Digital / NFC / Bitcoin intake: $${String.format("%.2f", digitalSales)}
            - Average Transaction: $${if (totalTransactions > 0) String.format("%.2f", totalAmount / totalTransactions) else "0.00"}
            
            Current Low Stock Inventory Alerts:
            ${if (lowStockList.isEmpty()) "All product categories have healthy stock reserves." else lowStockList.joinToString("\n")}
            
            Full Product Catalog:
            ${productsCatalog.joinToString("\n")}
            
            Popular products sold:
            ${if (salesBreakdownItems.isEmpty()) "No product sales logged yet." else salesBreakdownItems.entries.joinToString("\n") { "- ${it.key}: ${it.value} quantity sold" }}
            
            Merchant Request: ${customPrompt ?: "Establish an executive summary of this store's performance. Focus on (1) Hot Selling items, (2) Low Stock Urgency re-ordering recommendations, (3) Peak optimization tips to scale average transaction size. Keep it professional, highly visual, formatted with crisp monospace headers and clear merchant action steps."}
        """.trimIndent()

        val aiResult = GeminiClient.generate(contextPrompt)
        log("INTELLIGENCE AUDIT COMPLETED: Insights received from cloud model.")
        return aiResult
    }
}
