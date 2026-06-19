package com.example.data

import kotlinx.coroutines.flow.Flow

class InvoiceRepository(private val dao: VehicleInvoiceDao) {
    val allInvoices: Flow<List<VehicleInvoice>> = dao.getAllInvoices()
    val uniqueDates: Flow<List<String>> = dao.getUniqueDates()

    fun getInvoicesByDate(date: String): Flow<List<VehicleInvoice>> {
        return dao.getInvoicesByDate(date)
    }

    suspend fun insertInvoice(invoice: VehicleInvoice) {
        dao.insertInvoice(invoice)
    }

    suspend fun deleteInvoice(invoice: VehicleInvoice) {
        dao.deleteInvoice(invoice)
    }

    suspend fun deleteInvoiceById(id: Long) {
        dao.deleteInvoiceById(id)
    }
}
