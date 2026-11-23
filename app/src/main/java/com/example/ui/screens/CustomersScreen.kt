package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Customer
import com.example.model.OrderChannel
import com.example.model.SellerProfile
import com.example.model.formatMoney
import com.example.ui.components.ChannelBadge
import com.example.ui.components.EmptyArtView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.UiState

@Composable
fun CustomersScreen(
    customersState: UiState<List<Customer>>,
    sellerProfile: SellerProfile,
    isMutating: Boolean,
    onSaveCustomer: (
        existingId: String?,
        displayName: String,
        channel: OrderChannel,
        handleOrPhone: String,
        email: String?,
        shippingAddress: String?,
        notes: String?,
        onDone: () -> Unit
    ) -> Unit,
    onDeleteCustomer: (customerId: String, name: String) -> Unit,
    onFilterOrdersByCustomer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var search by remember { mutableStateOf("") }
    var showCustomerForm by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<Customer?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "REPEAT BUYERS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Customers",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            editingCustomer = null
                            showCustomerForm = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("add_customer_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Add Buyer")
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search name, @handle, phone, or address...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            when (customersState) {
                is UiState.Loading -> item { LoadingStateView("Loading directory...") }
                is UiState.Error -> item { ErrorStateView(customersState.message) }
                is UiState.Success -> {
                    val trimmed = search.trim().lowercase()
                    val filtered = customersState.data.filter { c ->
                        trimmed.isEmpty() ||
                            c.displayName.lowercase().contains(trimmed) ||
                            c.handleOrPhone.lowercase().contains(trimmed) ||
                            (c.shippingAddress?.lowercase()?.contains(trimmed) == true)
                    }
                    if (filtered.isEmpty()) {
                        item {
                            EmptyArtView(
                                title = stringResource(R.string.empty_customers_title),
                                subtitle = stringResource(R.string.empty_customers_subtitle),
                                actionLabel = "Add Customer",
                                onAction = {
                                    editingCustomer = null
                                    showCustomerForm = true
                                },
                                showArtwork = false
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { customer ->
                            CustomerCard(
                                customer = customer,
                                currencySymbol = sellerProfile.currencySymbol,
                                onEdit = {
                                    editingCustomer = customer
                                    showCustomerForm = true
                                },
                                onDelete = { onDeleteCustomer(customer.id, customer.displayName) },
                                onViewOrders = { onFilterOrdersByCustomer(customer.handleOrPhone) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCustomerForm) {
        CustomerFormSheet(
            existing = editingCustomer,
            isSaving = isMutating,
            onDismiss = { showCustomerForm = false },
            onSave = { name, ch, handle, email, address, notes ->
                onSaveCustomer(
                    editingCustomer?.id,
                    name,
                    ch,
                    handle,
                    email,
                    address,
                    notes
                ) {
                    showCustomerForm = false
                }
            }
        )
    }
}

@Composable
private fun CustomerCard(
    customer: Customer,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewOrders: () -> Unit
) {
    Surface(
        onClick = onViewOrders,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_card_${customer.id}")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customer.displayName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = customer.handleOrPhone,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ChannelBadge(customer.channelEnum)
            }

            if (!customer.shippingAddress.isNullOrBlank()) {
                Text(
                    text = customer.shippingAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!customer.notes.isNullOrBlank()) {
                Text(
                    text = customer.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${customer.orderCount} order${if (customer.orderCount == 1L) "" else "s"} · ${formatMoney(customer.totalSpentCents, currencySymbol)}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit customer")
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete customer",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerFormSheet(
    existing: Customer?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        displayName: String,
        channel: OrderChannel,
        handleOrPhone: String,
        email: String?,
        shippingAddress: String?,
        notes: String?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(existing?.displayName ?: "") }
    var channel by remember { mutableStateOf(existing?.channelEnum ?: OrderChannel.WHATSAPP) }
    var handle by remember { mutableStateOf(existing?.handleOrPhone ?: "") }
    var email by remember { mutableStateOf(existing?.email ?: "") }
    var address by remember { mutableStateOf(existing?.shippingAddress ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (existing == null) "Add Social Buyer" else "Edit Buyer",
                style = MaterialTheme.typography.headlineSmall
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                OrderChannel.entries.forEachIndexed { index, ch ->
                    SegmentedButton(
                        selected = channel == ch,
                        onClick = { channel = ch },
                        shape = SegmentedButtonDefaults.itemShape(index, OrderChannel.entries.size)
                    ) {
                        Text(ch.label)
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 80) name = it },
                label = { Text("Customer Name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = handle,
                onValueChange = { if (it.length <= 60) handle = it },
                label = { Text(if (channel == OrderChannel.WHATSAPP) "WhatsApp Phone *" else "Instagram @handle *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email,
                onValueChange = { if (it.length <= 120) email = it },
                label = { Text("Email (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = address,
                onValueChange = { if (it.length <= 250) address = it },
                label = { Text("Default Shipping Address (Optional)") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { if (it.length <= 400) notes = it },
                label = { Text("Buyer Notes (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (error != null) {
                Text(
                    text = error ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 20.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        when {
                            name.isBlank() -> error = "Enter customer name."
                            handle.isBlank() -> error = "Enter WhatsApp phone or @handle."
                            else -> {
                                error = null
                                onSave(
                                    name.trim(),
                                    channel,
                                    handle.trim(),
                                    email.trim().takeIf { it.isNotEmpty() },
                                    address.trim().takeIf { it.isNotEmpty() },
                                    notes.trim().takeIf { it.isNotEmpty() }
                                )
                            }
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(50.dp)
                        .testTag("save_customer_button")
                ) {
                    Text(if (isSaving) "Saving..." else "Save Buyer")
                }
            }
        }
    }
}
