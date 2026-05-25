package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Int): Product?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("UPDATE products SET stockQuantity = stockQuantity - :quantity WHERE id = :productId")
    suspend fun decrementStock(productId: Int, quantity: Int)

    @Query("UPDATE products SET stockQuantity = stockQuantity + :quantity WHERE id = :productId")
    suspend fun incrementStock(productId: Int, quantity: Int)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE synced = 0")
    suspend fun getUnsyncedTransactions(): List<Transaction>

    @Query("UPDATE transactions SET synced = 1 WHERE transactionId = :txnId")
    suspend fun markAsSynced(txnId: String)
}

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff_users")
    fun getAllStaff(): Flow<List<StaffUser>>

    @Query("SELECT * FROM staff_users WHERE staffId = :id LIMIT 1")
    suspend fun getStaffById(id: String): StaffUser?

    @Query("SELECT * FROM staff_users WHERE passcode = :passcode LIMIT 1")
    suspend fun getStaffByPasscode(passcode: String): StaffUser?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: StaffUser)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(staff: List<StaffUser>)
}

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shift_sessions WHERE isActive = 1 LIMIT 1")
    fun getActiveShiftFlow(): Flow<ShiftSession?>

    @Query("SELECT * FROM shift_sessions WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveShift(): ShiftSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: ShiftSession)

    @Update
    suspend fun updateShift(shift: ShiftSession)

    @Query("SELECT * FROM shift_sessions ORDER BY startTime DESC")
    fun getAllShiftsFlow(): Flow<List<ShiftSession>>
}
