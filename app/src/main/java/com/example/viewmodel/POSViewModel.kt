package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class POSViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = POSRepository(application)

    // Room DB Flow bounds
    val products: StateFlow<List<Product>> = repository.productDao.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = repository.transactionDao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val staffUsers: StateFlow<List<StaffUser>> = repository.staffDao.getAllStaff()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeShift: StateFlow<ShiftSession?> = repository.shiftDao.getActiveShiftFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allShifts: StateFlow<List<ShiftSession>> = repository.shiftDao.getAllShiftsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active User State
    private val _currentStaff = MutableStateFlow<StaffUser?>(null)
    val currentStaff: StateFlow<StaffUser?> = _currentStaff.asStateFlow()

    // Interactive Cart States
    private val _cart = MutableStateFlow<List<CartProduct>>(emptyList())
    val cart: StateFlow<List<CartProduct>> = _cart.asStateFlow()

    private val _discount = MutableStateFlow(0.0)
    val discount: StateFlow<Double> = _discount.asStateFlow()

    private val _taxRate = MutableStateFlow(8.25) // Default 8.25% retail tax
    val taxRate: StateFlow<Double> = _taxRate.asStateFlow()

    // Online/Offline & Sync simulation states mapping
    val isOnline: StateFlow<Boolean> = repository.isOnline
    val isSyncing: StateFlow<Boolean> = repository.isSyncing
    val syncLogs: StateFlow<List<String>> = repository.syncLogs

    // AI Insight states
    private val _aiInsight = MutableStateFlow<String?>(null)
    val aiInsight: StateFlow<String?> = _aiInsight.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    // Navigation and Drawer selection helpers
    private val _selectedCategory = MutableStateFlow("ALL")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Selected Transaction for receipts modal
    private val _selectedTransaction = MutableStateFlow<Transaction?>(null)
    val selectedTransaction: StateFlow<Transaction?> = _selectedTransaction.asStateFlow()

    init {
        viewModelScope.launch {
            // Inserts default catalog and core staff members on initial launch
            repository.initializeData()
            // Auto-login Tom Admin as default for high-speed friction-free onboarding
            val admin = repository.staffDao.getStaffById("STF-001")
            _currentStaff.value = admin
        }
    }

    // Toggle simulated internet
    fun toggleNetworkMode() {
        repository.setNetworkOnline(!isOnline.value)
        if (isOnline.value) {
            viewModelScope.launch {
                repository.triggerCloudSync()
            }
        }
    }

    // Staff user PIN Authentication
    fun loginWithPin(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val user = repository.staffDao.getStaffByPasscode(pin)
            if (user != null) {
                _currentStaff.value = user
                repository.log("STAFF SIGN-IN: Welcome back, ${user.name} (${user.role}).")
                onResult(true)
            } else {
                repository.log("SECURITY WARNING: Failed sign-in attempt with code [$pin].")
                onResult(false)
            }
        }
    }

    fun logoutStaff() {
        val oldStaffName = _currentStaff.value?.name ?: "Unknown"
        _currentStaff.value = null
        repository.log("STAFF DE-AUTHENTICATION: $oldStaffName logged out.")
    }

    // Add new customizable user or product on the fly
    fun addProduct(name: String, price: Double, costPrice: Double, stock: Int, category: String, barcode: String) {
        viewModelScope.launch {
            val product = Product(
                name = name,
                sku = "PRD-${category.take(3).uppercase()}-${UUID.randomUUID().toString().take(4).uppercase()}",
                barcode = barcode.ifBlank { (1000..9999).random().toString() },
                price = price,
                costPrice = costPrice,
                stockQuantity = stock,
                category = category,
                lowStockThreshold = 5
            )
            repository.productDao.insertProduct(product)
            repository.log("CATALOG ADDITION: ${product.name} listed in $category at $${String.format("%.2f", price)}.")
        }
    }

    fun updateProductStock(product: Product, newStock: Int) {
        viewModelScope.launch {
            val updated = product.copy(stockQuantity = newStock)
            repository.productDao.insertProduct(updated)
            repository.log("INVENTORY MANUAL EDIT: stock updated for ${product.name} to $newStock.")
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.productDao.deleteProduct(product)
            repository.log("CATALOG DELETION: ${product.name} removed from active database.")
        }
    }

    // Cash shift Drawer operation
    fun openShift(startingCash: Double) {
        viewModelScope.launch {
            val staff = _currentStaff.value
            val staffId = staff?.staffId ?: "STF-001"
            val staffName = staff?.name ?: "Tom (Admin)"
            val newShift = ShiftSession(
                shiftId = "SHIFT-${System.currentTimeMillis() / 1000}",
                staffId = staffId,
                staffName = staffName,
                startTime = System.currentTimeMillis(),
                startingCash = startingCash,
                isActive = true
            )
            repository.shiftDao.insertShift(newShift)
            repository.log("CASH DRAWER SHIFT OPENED: Cashier ${staffName} initialized till balance with $${String.format("%.2f", startingCash)}.")
        }
    }

    fun closeShift(endingCash: Double) {
        viewModelScope.launch {
            val active = repository.shiftDao.getActiveShift()
            if (active != null) {
                val updatedShift = active.copy(
                    endTime = System.currentTimeMillis(),
                    endingCash = endingCash,
                    isActive = false
                )
                repository.shiftDao.updateShift(updatedShift)
                repository.log("CASH DRAWER SHIFT TERMINATED: Till verified and locked. Total Sales processed: $${String.format("%.2f", active.totalSales)}.")
            }
        }
    }

    // Active Cart Controls
    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addToCart(product: Product, variant: String? = null) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.productId == product.id && it.selectedVariant == variant }
        if (index >= 0) {
            val item = currentList[index]
            currentList[index] = item.copy(quantity = item.quantity + 1)
        } else {
            currentList.add(
                CartProduct(
                    productId = product.id,
                    name = product.name,
                    price = product.price,
                    quantity = 1,
                    selectedVariant = variant ?: product.variants.split(",").firstOrNull()?.trim()
                )
            )
        }
        _cart.value = currentList
        repository.log("CART UPDATE: Added '${product.name}' to current sale order.")
    }

    fun updateCartQty(item: CartProduct, newQty: Int) {
        if (newQty <= 0) {
            _cart.value = _cart.value.filterNot { it == item }
            repository.log("CART UPDATE: Removed '${item.name}' from active sale order.")
        } else {
            _cart.value = _cart.value.map {
                if (it == item) it.copy(quantity = newQty) else it
            }
        }
    }

    fun clearCart() {
        _cart.value = emptyList()
        _discount.value = 0.0
    }

    fun applyDiscount(amount: Double) {
        _discount.value = amount
        repository.log("CART INVOICE MODIFIER: Applied flat promo discount of $${String.format("%.2f", amount)}.")
    }

    fun selectTransactionReceipt(txn: Transaction?) {
        _selectedTransaction.value = txn
    }

    // Final checkout payment processor
    fun checkoutCart(paymentMethod: String, onComplete: (Boolean) -> Unit) {
        val items = _cart.value
        val discountVal = _discount.value
        val taxRateVal = _taxRate.value
        val staff = _currentStaff.value ?: StaffUser("STF-001", "Tom (Admin)", "ADMIN", "1111")

        if (items.isEmpty()) {
            onComplete(false)
            return
        }

        viewModelScope.launch {
            try {
                val txn = repository.processSale(
                    cartItems = items,
                    paymentMethod = paymentMethod,
                    discountAmount = discountVal,
                    taxPercent = taxRateVal,
                    staffId = staff.staffId,
                    staffName = staff.name
                )
                _selectedTransaction.value = txn
                clearCart()
                onComplete(true)
            } catch (e: Exception) {
                repository.log("TRANSACTION FAILED: Database lock fault: ${e.message}")
                onComplete(false)
            }
        }
    }

    // Trigger Refund
    fun triggerRefund(transaction: Transaction) {
        viewModelScope.launch {
            repository.processRefund(transaction)
            // Re-select refunded transaction to show updated status
            val updated = transaction.copy(paymentStatus = "REFUNDED")
            _selectedTransaction.value = updated
        }
    }

    // Request AI Sales strategist insights
    fun fetchAiInsights(customTheme: String? = null) {
        _aiLoading.value = true
        _aiInsight.value = null
        viewModelScope.launch {
            val response = repository.getAiSalesIntelligence(customTheme)
            _aiInsight.value = response
            _aiLoading.value = false
        }
    }
}
