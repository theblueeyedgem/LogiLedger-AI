package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "vehicle_invoices")
data class VehicleInvoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: String,             // Format: "YYYY-MM-DD"
    val vehicleNumber: String,
    val gatePassNumber: String = "",
    val netWeight: Double,
    val balances: List<Double>,   // Saved using Converters
    val transportCharges: Double,
    val commissionRate: Double,
    val createdAt: Long = System.currentTimeMillis()
) {
    // Math sequences as requested:
    // 1. Gross Revenue
    fun calculateGrossRevenue(): Double = balances.sum()

    // 2. Transport Deduction
    fun calculateSubtotal1(): Double = calculateGrossRevenue() - transportCharges

    // 3. Commission Calculation
    fun calculateCommission(): Double = netWeight * commissionRate

    // 4. Final Net Profit
    fun calculateFinalBalance(): Double = calculateSubtotal1() - calculateCommission()
}

class Converters {
    @TypeConverter
    fun fromDoubleList(value: List<Double>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toDoubleList(value: String?): List<Double>? {
        if (value.isNullOrEmpty()) return emptyList()
        return value.split(",").mapNotNull { it.trim().toDoubleOrNull() }
    }
}
