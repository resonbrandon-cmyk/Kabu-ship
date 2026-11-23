package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.Customer
import com.example.model.OrderChannel
import com.example.model.OrderStage
import com.example.model.PaymentMethod
import com.example.model.PaymentStatus
import com.example.model.SocialOrder
import com.example.model.generateOrderCode
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OrderFormSheet(
    existingOrder: SocialOrder?,
    customers: List<Customer>,
    currencySymbol: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmitCreate: (
        orderCode: String,
        customerId: String?,
        customerName: String,
        channel: OrderChannel,
        handleOrPhone: String,
        shippingAddress: String?,
        itemsSummary: String,
        itemCount: Long,
        totalCents: Long,
        paymentMethod: PaymentMethod,
        paymentStatus: PaymentStatus,
        stage: OrderStage,
        courierName: String?,
        trackingNumber: String?,
        notes: String?,
        dmMessageSnapshot: String?,
        saveToCustomerDirectory: Boolean
    ) -> Unit,
    onSubmitUpdate: (
        order: SocialOrder,
        customerName: String,
        channel: OrderChannel,
        handleOrPhone: String,
        shippingAddress: String?,
        itemsSummary: String,
        itemCount: Long,
        totalCents: Long,
        paymentMethod: PaymentMethod,
        paymentStatus: PaymentStatus,
        stage: OrderStage,
        courierName: String?,
        trackingNumber: String?,
        notes: String?,
        dmMessageSnapshot: String?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isEditing = existingOrder != null

    var orderCode by remember { mutableStateOf(existingOrder?.orderCode ?: generateOrderCode()) }
    var selectedCustomerId by remember { mutableStateOf(existingOrder?.customerId) }
    var customerName by remember { mutableStateOf(existingOrder?.customerName ?: "") }
    var channel by remember { mutableStateOf(existingOrder?.channelEnum ?: OrderChannel.WHATSAPP) }
    var handleOrPhone by remember { mutableStateOf(existingOrder?.handleOrPhone ?: "") }
    var shippingAddress by remember { mutableStateOf(existingOrder?.shippingAddress ?: "") }
    var itemsSummary by remember { mutableStateOf(existingOrder?.itemsSummary ?: "") }
    var itemCount by remember { mutableLongStateOf(existingOrder?.itemCount ?: 1L) }
    var totalAmountText by remember {
        mutableStateOf(
            if (existingOrder != null) {
                String.format(java.util.Locale.US, "%.2f", existingOrder.totalCents / 100.0)
            } else ""
        )
    }
    var paymentMethod by remember { mutableStateOf(existingOrder?.paymentMethodEnum ?: PaymentMethod.PREPAID) }
    var paymentStatus by remember { mutableStateOf(existingOrder?.paymentStatusEnum ?: PaymentStatus.PENDING) }
    var stage by remember { mutableStateOf(existingOrder?.stageEnum ?: OrderStage.NEW_DM) }
    var courierName by remember { mutableStateOf(existingOrder?.courierName ?: "") }
    var trackingNumber by remember { mutableStateOf(existingOrder?.trackingNumber ?: "") }
    var notes by remember { mutableStateOf(existingOrder?.notes ?: "") }
    var dmSnippet by remember { mutableStateOf(existingOrder?.dmMessageSnapshot ?: "") }
    var saveToDirectory by remember { mutableStateOf(true) }
    var validationError by remember { mutableStateOf<String?>(null) }

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
                text = if (existingOrder != null) "Edit Order ${existingOrder.orderCode}" else "Log Social Order",
                style = MaterialTheme.typography.headlineSmall
            )

            // Channel Selector
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                OrderChannel.entries.forEachIndexed { index, ch ->
                    SegmentedButton(
                        selected = channel == ch,
                        onClick = { channel = ch },
                        shape = SegmentedButtonDefaults.itemShape(index, OrderChannel.entries.size),
                        modifier = Modifier.testTag("form_channel_${ch.name.lowercase()}")
                    ) {
                        Text(ch.label)
                    }
                }
            }

            // Quick-Pick Existing Customer
            if (!isEditing && customers.isNotEmpty()) {
                Text(
                    text = "Quick-fill saved buyer",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(customers, key = { it.id }) { cust ->
                        FilterChip(
                            selected = selectedCustomerId == cust.id,
                            onClick = {
                                if (selectedCustomerId == cust.id) {
                                    selectedCustomerId = null
                                } else {
                                    selectedCustomerId = cust.id
                                    customerName = cust.displayName
                                    channel = cust.channelEnum
                                    handleOrPhone = cust.handleOrPhone
                                    shippingAddress = cust.shippingAddress ?: ""
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("${cust.displayName} (${cust.handleOrPhone})") }
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = orderCode,
                    onValueChange = { if (!isEditing && it.length <= 32) orderCode = it },
                    enabled = !isEditing,
                    label = { Text("Order Code *") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.42f)
                        .testTag("order_code_input")
                )
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { if (it.length <= 80) customerName = it },
                    label = { Text("Customer Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.58f)
                        .testTag("customer_name_input")
                )
            }

            OutlinedTextField(
                value = handleOrPhone,
                onValueChange = { if (it.length <= 60) handleOrPhone = it },
                label = {
                    Text(
                        if (channel == OrderChannel.WHATSAPP) "WhatsApp Phone *" else "Instagram @handle *"
                    )
                },
                placeholder = {
                    Text(if (channel == OrderChannel.WHATSAPP) "+1 415 555 0198" else "@maya_styles")
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("handle_or_phone_input")
            )

            OutlinedTextField(
                value = itemsSummary,
                onValueChange = { if (it.length <= 300) itemsSummary = it },
                label = { Text("Ordered Items *") },
                placeholder = { Text("e.g. 2x Linen Tote Bag, 1x Ceramic Vase") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("items_summary_input")
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = totalAmountText,
                    onValueChange = { input ->
                        if (input.length <= 12 && input.all { it.isDigit() || it == '.' }) {
                            totalAmountText = input
                        }
                    },
                    label = { Text("Total ($currencySymbol) *") },
                    placeholder = { Text("49.50") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("total_amount_input")
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        IconButton(
                            onClick = { if (itemCount > 1L) itemCount -= 1L },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.Remove, contentDescription = "Decrease item count")
                        }
                        Text(
                            text = "$itemCount qty",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(
                            onClick = { if (itemCount < 10000L) itemCount += 1L },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Increase item count")
                        }
                    }
                }
            }

            // Payment Method
            Text("Payment Method", style = MaterialTheme.typography.labelMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PaymentMethod.entries.forEach { method ->
                    FilterChip(
                        selected = paymentMethod == method,
                        onClick = {
                            paymentMethod = method
                            if (method == PaymentMethod.COD && paymentStatus == PaymentStatus.PENDING) {
                                paymentStatus = PaymentStatus.COD_DUE
                            }
                        },
                        label = { Text(method.label) }
                    )
                }
            }

            // Payment Status
            Text("Payment Status", style = MaterialTheme.typography.labelMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PaymentStatus.entries.forEach { status ->
                    FilterChip(
                        selected = paymentStatus == status,
                        onClick = { paymentStatus = status },
                        label = { Text(status.label) }
                    )
                }
            }

            // Pipeline Stage (only non-terminal during form edit/create unless already chosen)
            Text("Pipeline Stage", style = MaterialTheme.typography.labelMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OrderStage.activePipelineStages.forEach { st ->
                    FilterChip(
                        selected = stage == st,
                        onClick = { stage = st },
                        label = { Text(st.label) }
                    )
                }
            }

            OutlinedTextField(
                value = shippingAddress,
                onValueChange = { if (it.length <= 250) shippingAddress = it },
                label = { Text("Delivery Address (Optional)") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = courierName,
                    onValueChange = { if (it.length <= 60) courierName = it },
                    label = { Text("Courier") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = trackingNumber,
                    onValueChange = { if (it.length <= 80) trackingNumber = it },
                    label = { Text("Tracking #") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = dmSnippet,
                onValueChange = { if (it.length <= 600) dmSnippet = it },
                label = { Text("Pasted DM / Chat Snippet (Optional)") },
                placeholder = { Text("Paste the customer's WhatsApp or IG order message") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { if (it.length <= 400) notes = it },
                label = { Text("Packing / Delivery Notes (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (!isEditing && selectedCustomerId == null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = saveToDirectory,
                        onCheckedChange = { saveToDirectory = it }
                    )
                    Text(
                        text = "Also save buyer to Customer Directory",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (validationError != null) {
                Text(
                    text = validationError ?: "",
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
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val parsedAmount = totalAmountText.toDoubleOrNull()
                        when {
                            orderCode.trim().length < 2 ->
                                validationError = "Order code must be at least 2 characters."
                            customerName.isBlank() ->
                                validationError = "Please enter the customer's name."
                            handleOrPhone.isBlank() ->
                                validationError = "Please enter a WhatsApp phone or Instagram @handle."
                            itemsSummary.isBlank() ->
                                validationError = "Please describe the ordered items."
                            parsedAmount == null || parsedAmount < 0.0 ->
                                validationError = "Enter a valid order total (e.g. 49.50)."
                            else -> {
                                validationError = null
                                val totalCents = (parsedAmount * 100.0).roundToLong()
                                if (existingOrder == null) {
                                    onSubmitCreate(
                                        orderCode.trim(),
                                        selectedCustomerId,
                                        customerName.trim(),
                                        channel,
                                        handleOrPhone.trim(),
                                        shippingAddress.trim().takeIf { it.isNotEmpty() },
                                        itemsSummary.trim(),
                                        itemCount,
                                        totalCents,
                                        paymentMethod,
                                        paymentStatus,
                                        stage,
                                        courierName.trim().takeIf { it.isNotEmpty() },
                                        trackingNumber.trim().takeIf { it.isNotEmpty() },
                                        notes.trim().takeIf { it.isNotEmpty() },
                                        dmSnippet.trim().takeIf { it.isNotEmpty() },
                                        saveToDirectory
                                    )
                                } else {
                                    onSubmitUpdate(
                                        existingOrder,
                                        customerName.trim(),
                                        channel,
                                        handleOrPhone.trim(),
                                        shippingAddress.trim().takeIf { it.isNotEmpty() },
                                        itemsSummary.trim(),
                                        itemCount,
                                        totalCents,
                                        paymentMethod,
                                        paymentStatus,
                                        stage,
                                        courierName.trim().takeIf { it.isNotEmpty() },
                                        trackingNumber.trim().takeIf { it.isNotEmpty() },
                                        notes.trim().takeIf { it.isNotEmpty() },
                                        dmSnippet.trim().takeIf { it.isNotEmpty() }
                                    )
                                }
                            }
                        }
                    },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(50.dp)
                        .testTag("save_order_button")
                ) {
                    Text(if (isSaving) "Saving..." else if (isEditing) "Save Changes" else "Log Order")
                }
            }
        }
    }
}
