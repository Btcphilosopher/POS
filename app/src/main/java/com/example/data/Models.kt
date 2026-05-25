package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "products")
@JsonClass(generateAdapter = true)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val sku: String,
    val barcode: String,
    val price: Double,
    val costPrice: Double,
    val stockQuantity: Int,
    val category: String,
    val imageUrl: String? = null,
    val variants: String = "" , // Delimited or JSON, e.g., "Regular, Large"
    val lowStockThreshold: Int = 5
)

@Entity(tableName = "transactions")
@JsonClass(generateAdapter = true)
data class Transaction(
    @PrimaryKey val transactionId: String,
    val timestamp: Long,
    val totalAmount: Double,
    val taxAmount: Double,
    val discountAmount: Double,
    val paymentMethod: String, // "CARD", "NFC", "QR", "CASH", "BITCOIN"
    val paymentStatus: String, // "SUCCESS", "FAILED", "REFUNDED"
    val itemsJson: String, // Moshi serialized List<CartProduct>
    val staffId: String,
    val staffName: String,
    val isOffline: Boolean,
    val synced: Boolean = false
)

@Entity(tableName = "staff_users")
@JsonClass(generateAdapter = true)
data class StaffUser(
    @PrimaryKey val staffId: String,
    val name: String,
    val role: String, // "CASHIER", "MANAGER", "ADMIN"
    val passcode: String // 4-digit PIN for access
)

@Entity(tableName = "shift_sessions")
@JsonClass(generateAdapter = true)
data class ShiftSession(
    @PrimaryKey val shiftId: String,
    val staffId: String,
    val staffName: String,
    val startTime: Long,
    val endTime: Long? = null,
    val startingCash: Double,
    val endingCash: Double? = null,
    val totalSales: Double = 0.0,
    val isActive: Boolean = true
)

// In-Memory Helper Models
@JsonClass(generateAdapter = true)
data class CartProduct(
    val productId: Int,
    val name: String,
    val price: Double,
    val quantity: Int,
    val selectedVariant: String? = null
)
