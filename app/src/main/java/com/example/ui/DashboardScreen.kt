package com.example.ui

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VehicleInvoice
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import android.net.Uri

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: InvoiceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) } // 0: Form & Review, 1: Ledger History, 2: AI Parser

    // Observe streams
    val allInvoices by viewModel.allInvoices.collectAsState()
    val uniqueDates by viewModel.uniqueDates.collectAsState()
    val filteredInvoices by viewModel.filteredInvoices.collectAsState()

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            val stream = ByteArrayOutputStream()
            it.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val base64Image = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)
            viewModel.triggerGeminiImageParse(base64Image)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            bytes?.let { b ->
                val base64Image = Base64.encodeToString(b, Base64.DEFAULT)
                viewModel.triggerGeminiImageParse(base64Image)
            }
        }
    }

    // Context Calendar helper
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(year, month, dayOfMonth)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formattedDate = dateFormat.format(selectedCalendar.time)
            viewModel.formDate = formattedDate
            viewModel.historyDateFilter = formattedDate
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalShipping,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LOGILEDGER",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Elegant horizontal slider / streaks
            DecorativeStreaks(modifier = Modifier.fillMaxWidth().height(4.dp))

            // Summary Info Quick Board
            DashboardHeaderCard(
                totalInvoicesCount = filteredInvoices.size,
                grandProfitSum = filteredInvoices.sumOf { it.calculateFinalBalance() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // High priority segment selection tabs
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Log entry", fontWeight = FontWeight.Medium) },
                    icon = { Icon(Icons.Outlined.EditCalendar, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    modifier = Modifier.testTag("tab_form").minimumInteractiveComponentSize()
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("History", fontWeight = FontWeight.Medium) },
                    icon = { Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    modifier = Modifier.testTag("tab_history").minimumInteractiveComponentSize()
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("AI Parser", fontWeight = FontWeight.Medium) },
                    icon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    modifier = Modifier.testTag("tab_ai_parser").minimumInteractiveComponentSize()
                )
            }

            // Central tab controller workspace
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    0 -> {
                        // Split layout on large screens, scrollable column on compact devices.
                        RowOrColumnLayout(
                            leftContent = {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        item {
                                            // Date Picker Input
                                            Box(modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { datePickerDialog.show() }
                                                .testTag("input_form_date")
                                            ) {
                                                OutlinedTextField(
                                                    value = viewModel.formDate,
                                                    onValueChange = {},
                                                    label = { Text("Log Date (YYYY-MM-DD)") },
                                                    enabled = false, 
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Default.DateRange,
                                                            contentDescription = null,
                                                            modifier = Modifier.minimumInteractiveComponentSize()
                                                        )
                                                    },
                                                    trailingIcon = {
                                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Date")
                                                    },
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                    ),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }

                                        item {
                                            // Beautiful Custom license plate vehicle display card input
                                            LicensePlateCard(
                                                value = viewModel.formVehicleNumber,
                                                onValueChange = { viewModel.formVehicleNumber = it },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        item {
                                            OutlinedTextField(
                                                value = viewModel.formGatePassNumber,
                                                onValueChange = { viewModel.formGatePassNumber = it },
                                                label = { Text("Gate Pass Number") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("input_gate_pass_number"),
                                                leadingIcon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = null) }
                                            )
                                        }

                                        item {
                                            OutlinedTextField(
                                                value = viewModel.formNetWeight,
                                                onValueChange = { viewModel.formNetWeight = it },
                                                label = { Text("Net Weight (Tons)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("input_net_weight"),
                                                leadingIcon = { Icon(Icons.Default.Scale, contentDescription = null) }
                                            )
                                        }

                                        // Dynamic Balance Repeater
                                        item {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .border(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.outlineVariant,
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = "Partial Balances",
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = "Driver payments & receipts",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }

                                                    Button(
                                                        onClick = { viewModel.addBalanceField() },
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                        ),
                                                        modifier = Modifier.testTag("btn_add_balance_field")
                                                    ) {
                                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Add", fontSize = 12.sp)
                                                    }
                                                }

                                                viewModel.formBalances.forEachIndexed { idx, itemText ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .animateContentSize(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        TextField(
                                                            value = itemText,
                                                            onValueChange = { viewModel.updateBalanceFieldAt(idx, it) },
                                                            placeholder = { Text("Amount (Rs)") },
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .testTag("input_balance_field_$idx"),
                                                            maxLines = 1,
                                                            singleLine = true,
                                                            colors = TextFieldDefaults.colors(
                                                                focusedContainerColor = Color.Transparent,
                                                                unfocusedContainerColor = Color.Transparent
                                                            )
                                                        )

                                                        IconButton(
                                                            onClick = { viewModel.removeBalanceFieldAt(idx) },
                                                            modifier = Modifier
                                                                .testTag("btn_remove_balance_field_$idx")
                                                                .minimumInteractiveComponentSize(),
                                                            colors = IconButtonDefaults.iconButtonColors(
                                                                contentColor = MaterialTheme.colorScheme.error
                                                            )
                                                        ) {
                                                            Icon(Icons.Default.Delete, contentDescription = "Delete input entry")
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        item {
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                border = borderAccent()
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .padding(12.dp)
                                                        .fillMaxWidth(),
                                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    OutlinedTextField(
                                                        value = viewModel.formTransportCharges,
                                                        onValueChange = { viewModel.formTransportCharges = it },
                                                        label = { Text("Transport Charges (Rs)") },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .testTag("input_transport_charges"),
                                                        leadingIcon = { Icon(Icons.Default.LocalShipping, contentDescription = null) }
                                                    )

                                                    OutlinedTextField(
                                                        value = viewModel.formCommissionRate,
                                                        onValueChange = { viewModel.formCommissionRate = it },
                                                        label = { Text("Commission Rate (Per Ton)") },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .testTag("input_commission_rate"),
                                                        leadingIcon = { Icon(Icons.Default.Percent, contentDescription = null) }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.clearForm() },
                                            modifier = Modifier
                                                .weight(0.4f)
                                                .height(52.dp)
                                                .testTag("btn_clear_form"),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("RESET")
                                        }

                                        Button(
                                            onClick = { viewModel.saveInvoice() },
                                            modifier = Modifier
                                                .weight(0.6f)
                                                .height(52.dp)
                                                .testTag("btn_save_invoice"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            ),
                                            enabled = viewModel.formVehicleNumber.trim().isNotEmpty()
                                        ) {
                                            Icon(Icons.Default.Save, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("SAVE RECORD", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            },
                            rightContent = {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "LIVE RECEIPT PREVIEW",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    ) {
                                        ThermalReceiptCard(
                                            invoice = viewModel.getPreviewInvoice(),
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        )
                    }

                    1 -> {
                        // History tracking section
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Ledger History Logs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            if (uniqueDates.isEmpty()) {
                                EmptyHistoryPlaceholder(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                )
                            } else {
                                // Horizontal calendar list scroll
                                Text(
                                    text = "Select Date Ledger Book:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                SingleSelectionRow(
                                    items = uniqueDates,
                                    selectedItem = viewModel.historyDateFilter,
                                    onItemSelected = { viewModel.historyDateFilter = it },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Current filtered records summary
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "DAY SUMMARY: ${viewModel.historyDateFilter}",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${filteredInvoices.size} Vehicles Handled",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val context = androidx.compose.ui.platform.LocalContext.current
                                            androidx.compose.material3.IconButton(
                                                onClick = {
                                                    exportSummaryToPdf(context, viewModel.historyDateFilter, filteredInvoices)
                                                },
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(8.dp))
                                                    .size(40.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.PictureAsPdf,
                                                    contentDescription = "Export to PDF",
                                                    tint = MaterialTheme.colorScheme.onTertiary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            Button(
                                                onClick = { viewModel.showEndOfDaySummary = true },
                                                modifier = Modifier.testTag("btn_view_end_of_day"),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondary,
                                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Icon(Icons.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Grand Ledger")
                                            }
                                        }
                                    }
                                }

                                // List of vehicles
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filteredInvoices, key = { it.id }) { invoice ->
                                        VehicleHistoryItem(
                                            invoice = invoice,
                                            onClicked = { viewModel.selectedInvoiceDetail = invoice },
                                            onDelete = { viewModel.deleteInvoice(invoice) },
                                            modifier = Modifier.animateItem()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // AI parser workspace
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Gemini Intelligent Parser",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Dictate, write, or paste messy logs. Our AI agent extracts fields instantly and populates the calculations.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = viewModel.pasteBoxText,
                                onValueChange = { viewModel.pasteBoxText = it },
                                placeholder = {
                                    Text(
                                        text = "Example: Truck 55AB Arrived on June 18 with 12 tons weight. Balances are 5k and 2.5k. Transport expenses was 1200, commission rate 45."
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .testTag("ai_paste_field"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            viewModel.parseErrorMessage?.let { err ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                        Text(err, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { viewModel.triggerGeminiParse() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .testTag("btn_ai_analyse"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    enabled = !viewModel.isParsingText
                                ) {
                                    if (viewModel.isParsingText) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Processing...", maxLines = 1)
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("AUTO-ANALYZE", fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }

                                IconButton(
                                    onClick = { cameraLauncher.launch(null) },
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
                                        .fillMaxHeight()
                                        .width(52.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PhotoCamera,
                                        contentDescription = "Scan Document using Camera",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }

                                IconButton(
                                    onClick = { galleryLauncher.launch("image/*") },
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
                                        .fillMaxHeight()
                                        .width(52.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Image,
                                        contentDescription = "Scan Document from Gallery",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            // Sample template helper
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("💡 Pro-Tip:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "Pasted content will merge into 'Log entry' values. You can preview calculation details on the receipt preview before locking it in.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Invoice Detail Sheet Custom Overlay
    viewModel.selectedInvoiceDetail?.let { detail ->
        AlertDialog(
            onDismissRequest = { viewModel.selectedInvoiceDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Vehicle ${detail.vehicleNumber}",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RowValueText("Log Date:", detail.date)
                    if (detail.gatePassNumber.isNotBlank()) {
                        RowValueText("Gate Pass Number:", detail.gatePassNumber)
                    }
                    RowValueText("Net Weight:", "${detail.netWeight} Tons")
                    RowValueText("Subton Balances:", detail.balances.joinToString(" + ") { "Rs $it" })
                    RowValueText("Gross Revenue:", "Rs ${detail.calculateGrossRevenue()}")
                    RowValueText("Transport deduction:", "- Rs ${detail.transportCharges}")
                    RowValueText("Subtotal 1:", "Rs ${detail.calculateSubtotal1()}")
                    RowValueText("Commission weight charge:", "- Rs ${detail.calculateCommission()} (${detail.netWeight} ton x Rs ${detail.commissionRate})")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    RowValueText(
                        label = "FINAL NET PROFIT:",
                        value = "Rs ${detail.calculateFinalBalance()}",
                        color = MaterialTheme.colorScheme.primary,
                        isBold = true
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteInvoice(detail)
                        viewModel.selectedInvoiceDetail = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            viewModel.loadInvoiceForEditing(detail)
                            viewModel.selectedInvoiceDetail = null
                            activeTab = 0
                        }
                    ) {
                        Text("Edit")
                    }
                    TextButton(
                        onClick = { viewModel.selectedInvoiceDetail = null },
                        modifier = Modifier.testTag("btn_close_invoice_dialog")
                    ) {
                        Text("OK")
                    }
                }
            }
        )
    }

    // End-Of-Day Grand Summary Bottom sheet Custom Dialog
    if (viewModel.showEndOfDaySummary) {
        val totalVehicles = filteredInvoices.size
        val dayGrossRevenue = filteredInvoices.sumOf { it.calculateGrossRevenue() }
        val dayTransportCharges = filteredInvoices.sumOf { it.transportCharges }
        val dayCommission = filteredInvoices.sumOf { it.calculateCommission() }
        val dayFinalProfit = filteredInvoices.sumOf { it.calculateFinalBalance() }

        AlertDialog(
            onDismissRequest = { viewModel.showEndOfDaySummary = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Grand Ledger Summary",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "JOURNAL DATE: ${viewModel.historyDateFilter}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider()

                    RowValueText("Total Vehicles Processed:", "$totalVehicles")
                    RowValueText("Cumulative Gross sums:", "Rs ${dayGrossRevenue}")
                    RowValueText("Cumulative Transport deductions:", "- Rs ${dayTransportCharges}")
                    RowValueText("Cumulative Commissions charge:", "- Rs ${dayCommission}")

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GRAND LEDGER PROFIT:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Rs ${dayFinalProfit}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Simple breakdown listing
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Vehicles processed on ledger:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .heightIn(max = 150.dp)
                            .fillMaxWidth()
                    ) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredInvoices) { invoice ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "• ${invoice.vehicleNumber}",
                                        fontSize = 13.sp,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Rs ${invoice.calculateFinalBalance()}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.showEndOfDaySummary = false },
                    modifier = Modifier.testTag("btn_close_summary_dialog"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("DONE")
                }
            }
        )
    }
}

@Composable
fun RowValueText(
    label: String,
    value: String,
    color: Color = Color.Unspecified,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SingleSelectionRow(
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color.Transparent),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.take(8).forEach { dateStr ->
            val isSelected = dateStr == selectedItem
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .clickable { onItemSelected(dateStr) }
                    .testTag("date_selection_chip_$dateStr")
                    .weight(1.0f)
            ) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun borderAccent() = androidx.compose.foundation.BorderStroke(
    width = 1.dp,
    color = MaterialTheme.colorScheme.outlineVariant
)

@Composable
fun RowOrColumnLayout(
    leftContent: @Composable () -> Unit,
    rightContent: @Composable () -> Unit
) {
    // Basic screen adapter layout. 
    // Usually vertically stacked, but we want horizontal side-by-side if screen is medium/large.
    // Let's use simple container constraints for maximum compatibility.
    BoxWithConstraints {
        val isWide = maxWidth > 650.dp
        if (isWide) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.weight(1.0f).fillMaxHeight()) {
                    leftContent()
                }
                Box(modifier = Modifier.weight(0.9f).fillMaxHeight()) {
                    rightContent()
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                    leftContent()
                }
                Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                    rightContent()
                }
            }
        }
    }
}

@Composable
fun DecorativeStreaks(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    Canvas(modifier = modifier) {
        val strokeWidth = size.height
        drawLine(
            color = primaryColor,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width * 0.6f, size.height / 2),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = secondaryColor,
            start = Offset(size.width * 0.6f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = strokeWidth
        )
    }
}

@Composable
fun DashboardHeaderCard(
    totalInvoicesCount: Int,
    grandProfitSum: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("dashboard_header_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Lanes and Dispatch Ledger",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Rs $grandProfitSum",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Processed",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$totalInvoicesCount Cars",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun LicensePlateCard(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Beautiful yellow licensing plate container
    Card(
        modifier = modifier
            .border(3.dp, Color(0xFF1C1D1F), RoundedCornerShape(8.dp))
            .padding(1.dp),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFD43F) // Deep solid dynamic license plate yellow
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "VEHICLE IDENTIFICATION",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Black.copy(alpha = 0.7f),
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = "ENTER PLATE ID",
                        color = Color.Black.copy(alpha = 0.35f),
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("license_plate_input"),
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    letterSpacing = 3.sp
                ),
                singleLine = true,
                maxLines = 1,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("★ LOGISTICAL HUB", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black.copy(alpha = 0.6f))
                Text("DEPOT OK ★", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun ThermalReceiptCard(
    invoice: VehicleInvoice,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(8.dp)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFEFA) // High contrast off-white invoice paper tone
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Receipt Header Brand
            Text(
                text = "📄 INVOICE ESTIMATION SUMMARY",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Date: " + invoice.date,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))
            DashedSeparator(modifier = Modifier.fillMaxWidth().height(1.dp))
            Spacer(modifier = Modifier.height(6.dp))

            // Plate representation
            Text(
                text = "TRUCK ID: [ " + (invoice.vehicleNumber.uppercase().ifEmpty { "PENDING" }) + " ]",
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))
            DashedSeparator(modifier = Modifier.fillMaxWidth().height(1.dp))
            Spacer(modifier = Modifier.height(10.dp))

            // Body breakdown math
            InvoiceReceiptRow(label = "Net Weight:", value = "${invoice.netWeight} Tons")

            val partialBalancesSumText = if (invoice.balances.isEmpty()) {
                "Rs 0.00"
            } else {
                "Rs " + invoice.balances.sum() + " (" + invoice.balances.joinToString("+") { "Rs $it" } + ")"
            }

            InvoiceReceiptRow(
                label = "Total balances sum:",
                value = partialBalancesSumText
            )

            InvoiceReceiptRow(
                label = "Transport Charges:",
                value = "- Rs " + invoice.transportCharges
            )

            Spacer(modifier = Modifier.height(4.dp))
            DashedSeparator(modifier = Modifier.fillMaxWidth().height(1.dp), color = Color.LightGray)
            Spacer(modifier = Modifier.height(4.dp))

            InvoiceReceiptRow(
                label = "Subtotal (T1 deduction):",
                value = "Rs " + invoice.calculateSubtotal1(),
                isBold = true
            )

            InvoiceReceiptRow(
                label = "Commission charge:",
                value = "- Rs " + invoice.calculateCommission() + " (${invoice.netWeight} weight * Rs ${invoice.commissionRate} rate)"
            )

            Spacer(modifier = Modifier.weight(1f))
            DashedSeparator(modifier = Modifier.fillMaxWidth().height(2.dp))
            Spacer(modifier = Modifier.height(10.dp))

            // Final Result Balance Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FINAL NET RESULT:",
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = "Rs " + invoice.calculateFinalBalance(),
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1B5E20) // Deep dark thermal green text
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "LogiLedger Math sequence finalized successfully.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun InvoiceReceiptRow(
    label: String,
    value: String,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = Color.DarkGray,
            modifier = Modifier.weight(0.5f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = Color.Black,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.5f)
        )
    }
}

@Composable
fun DashedSeparator(
    modifier: Modifier = Modifier,
    color: Color = Color.Gray
) {
    Canvas(modifier = modifier) {
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            pathEffect = pathEffect,
            strokeWidth = 2f
        )
    }
}

@Composable
fun VehicleHistoryItem(
    invoice: VehicleInvoice,
    onClicked: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClicked() }
            .testTag("vehicle_history_item_${invoice.vehicleNumber}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = borderAccent()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Truck Logo Avatar representation
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = invoice.vehicleNumber.uppercase(),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = if (invoice.gatePassNumber.isNotBlank()) "Gate Pass: ${invoice.gatePassNumber}" else "No Gate Pass",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Net: ${invoice.netWeight} Tons | Gross: Rs ${invoice.calculateGrossRevenue()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Final balance calculations
                Text(
                    text = "Rs " + invoice.calculateFinalBalance(),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .testTag("delete_invoice_${invoice.id}")
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete invoice item",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyHistoryPlaceholder(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.History,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No logistics records found yet.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Try logging your first vehicle entry from the form!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
