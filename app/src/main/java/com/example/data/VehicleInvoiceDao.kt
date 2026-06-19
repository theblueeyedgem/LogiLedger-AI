package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleInvoiceDao {
    @Query("SELECT * FROM vehicle_invoices ORDER BY date DESC, createdAt DESC")
    fun getAllInvoices(): Flow<List<VehicleInvoice>>

    @Query("SELECT * FROM vehicle_invoices WHERE date = :date ORDER BY createdAt DESC")
    fun getInvoicesByDate(date: String): Flow<List<VehicleInvoice>>

    @Query("SELECT DISTINCT date FROM vehicle_invoices ORDER BY date DESC")
    fun getUniqueDates(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: VehicleInvoice)

    @Delete
    suspend fun deleteInvoice(invoice: VehicleInvoice)

    @Query("DELETE FROM vehicle_invoices WHERE id = :id")
    suspend fun deleteInvoiceById(id: Long)
}
