package com.example.ui

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AppDatabase
import com.example.data.InvoiceRepository
import com.example.data.VehicleInvoice
import com.example.network.GeminiClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class InvoiceViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = InvoiceRepository(database.vehicleInvoiceDao())

    // Live Streams
    val allInvoices: StateFlow<List<VehicleInvoice>> = repository.allInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uniqueDates: StateFlow<List<String>> = repository.uniqueDates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Form Fields
    var formDate by mutableStateOf(getTodayDateString())
    var formVehicleNumber by mutableStateOf("")
    var formGatePassNumber by mutableStateOf("")
    var formNetWeight by mutableStateOf("")
    val formBalances = mutableStateListOf<String>("") // Starts with one empty element
    var formTransportCharges by mutableStateOf("")
    var formCommissionRate by mutableStateOf("")

    var editingInvoiceId by mutableStateOf<Long?>(null)

    // Clipboard/Quick paste
    var pasteBoxText by mutableStateOf("")
    var isParsingText by mutableStateOf(false)
    var parseErrorMessage by mutableStateOf<String?>(null)

    // Current History Date Filter
    var historyDateFilter by mutableStateOf(getTodayDateString())
    
    // UI Panels and overlays State
    var selectedInvoiceDetail by mutableStateOf<VehicleInvoice?>(null)
    var showEndOfDaySummary by mutableStateOf(false)

    // Filtered Flow based on selected date
    val filteredInvoices: StateFlow<List<VehicleInvoice>> = snapshotFlow { historyDateFilter }
        .flatMapLatest { date ->
            repository.getInvoicesByDate(date)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getTodayDateString(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    fun loadInvoiceForEditing(invoice: VehicleInvoice) {
        editingInvoiceId = invoice.id
        formDate = invoice.date
        formVehicleNumber = invoice.vehicleNumber
        formGatePassNumber = invoice.gatePassNumber
        formNetWeight = invoice.netWeight.toString()
        formTransportCharges = invoice.transportCharges.toString()
        formCommissionRate = invoice.commissionRate.toString()
        formBalances.clear()
        if (invoice.balances.isEmpty()) {
            formBalances.add("")
        } else {
            formBalances.addAll(invoice.balances.map { it.toString() })
        }
    }

    // Invoice computation on the fly
    fun getPreviewInvoice(): VehicleInvoice {
        val weight = formNetWeight.toDoubleOrNull() ?: 0.0
        val transport = formTransportCharges.toDoubleOrNull() ?: 0.0
        val rate = formCommissionRate.toDoubleOrNull() ?: 0.0
        val parsedBalances = formBalances.mapNotNull { it.trim().toDoubleOrNull() }

        return VehicleInvoice(
            id = editingInvoiceId ?: 0L,
            date = formDate,
            vehicleNumber = formVehicleNumber.trim(),
            gatePassNumber = formGatePassNumber.trim(),
            netWeight = weight,
            balances = parsedBalances,
            transportCharges = transport,
            commissionRate = rate
        )
    }

    // Adding dynamic input fields
    fun addBalanceField() {
        formBalances.add("")
    }

    fun removeBalanceFieldAt(index: Int) {
        if (formBalances.size > 1) {
            formBalances.removeAt(index)
        } else {
            formBalances[0] = ""
        }
    }

    fun updateBalanceFieldAt(index: Int, value: String) {
        if (index in formBalances.indices) {
            formBalances[index] = value
        }
    }

    // Clear form inputs
    fun clearForm() {
        formDate = getTodayDateString()
        formVehicleNumber = ""
        formGatePassNumber = ""
        formNetWeight = ""
        formBalances.clear()
        formBalances.add("")
        formTransportCharges = ""
        formCommissionRate = ""
        parseErrorMessage = null
        editingInvoiceId = null
    }

    // Save calculation to database
    fun saveInvoice() {
        val invoice = getPreviewInvoice()
        if (invoice.vehicleNumber.trim().isEmpty()) return
        
        viewModelScope.launch {
            repository.insertInvoice(invoice)
            historyDateFilter = invoice.date // Sync date selection with save
            clearForm()
        }
    }

    // Delete records
    fun deleteInvoice(invoice: VehicleInvoice) {
        viewModelScope.launch {
            repository.deleteInvoice(invoice)
        }
    }
    
    fun deleteInvoiceById(id: Long) {
        viewModelScope.launch {
            repository.deleteInvoiceById(id)
        }
    }

    // Direct parser calling
    fun triggerGeminiParse() {
        if (pasteBoxText.trim().isEmpty()) {
            parseErrorMessage = "Please enter some unstructured text to parse."
            return
        }
        isParsingText = true
        parseErrorMessage = null

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey == "MY_GEMINI_API_KEY" || apiKey.trim().isEmpty()) {
                    parseErrorMessage = "Gemini API key is not configured. Please add GEMINI_API_KEY in the Secrets panel in AI Studio UI."
                    isParsingText = false
                    return@launch
                }

                val result = GeminiClient.parseLogisticsText(apiKey, pasteBoxText, formDate)
                if (result != null) {
                    result.date?.let { if (it.isNotEmpty()) formDate = it }
                    result.vehicle_number?.let { if (it.isNotEmpty()) formVehicleNumber = it }
                    result.gate_pass_number?.let { if (it.isNotEmpty()) formGatePassNumber = it }
                    result.net_weight?.let { formNetWeight = it.toString() }
                    result.transport_charges?.let { formTransportCharges = it.toString() }
                    result.commission_rate?.let { formCommissionRate = it.toString() }
                    
                    if (!result.balances.isNullOrEmpty()) {
                        formBalances.clear()
                        result.balances.forEach {
                            formBalances.add(it.toString())
                        }
                    }
                    pasteBoxText = "" // Clear pasted contents upon successful parse
                } else {
                    parseErrorMessage = "Gemini returned empty or could not map fields. Please check format."
                }
            } catch (e: Exception) {
                parseErrorMessage = "Parsing failed: ${e.localizedMessage ?: e.message}"
            } finally {
                isParsingText = false
            }
        }
    }

    fun triggerGeminiImageParse(base64Image: String) {
        isParsingText = true
        parseErrorMessage = null

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey == "MY_GEMINI_API_KEY" || apiKey.trim().isEmpty()) {
                    parseErrorMessage = "Gemini API key is not configured. Please add GEMINI_API_KEY in the Secrets panel in AI Studio UI."
                    isParsingText = false
                    return@launch
                }

                val result = GeminiClient.parseLogisticsImage(apiKey, base64Image, formDate)
                if (result != null) {
                    result.date?.let { if (it.isNotEmpty()) formDate = it }
                    result.vehicle_number?.let { if (it.isNotEmpty()) formVehicleNumber = it }
                    result.gate_pass_number?.let { if (it.isNotEmpty()) formGatePassNumber = it }
                    result.net_weight?.let { formNetWeight = it.toString() }
                    result.transport_charges?.let { formTransportCharges = it.toString() }
                    result.commission_rate?.let { formCommissionRate = it.toString() }
                    
                    if (!result.balances.isNullOrEmpty()) {
                        formBalances.clear()
                        result.balances.forEach {
                            formBalances.add(it.toString())
                        }
                    }
                } else {
                    parseErrorMessage = "Gemini returned empty or could not map fields from image. Please try again."
                }
            } catch (e: Exception) {
                parseErrorMessage = "Image Parsing failed: ${e.localizedMessage ?: e.message}"
            } finally {
                isParsingText = false
            }
        }
    }
}
