package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.OrderEvent
import com.example.model.OrderStage
import com.example.model.PaymentMethod
import com.example.model.PaymentStatus
import com.example.model.SellerProfile
import com.example.model.SocialOrder
import com.example.model.buildDmStatusUpdate
import com.example.model.formatMoney
import com.example.model.formatTimestampShort
import com.example.ui.components.ChannelBadge
import com.example.ui.components.CourierDispatchDialog
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.components.OrderProgressBar
import com.example.ui.components.OutreachActions
import com.example.ui.components.PaymentBadge
import com.example.ui.components.StagePill
import com.example.ui.components.stageColor
import com.example.ui.components.stageIcon
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OrderDetailScreen(
    order: SocialOrder?,
    eventsState: UiState<List<OrderEvent>>,
    sellerProfile: SellerProfile,
    isMutating: Boolean,
    onBack: () -> Unit,
    onEditOrder: (SocialOrder) -> Unit,
    onAdvanceStage: (SocialOrder, OrderStage, String?, String?, String?) -> Unit,
    onUpdatePayment: (SocialOrder, PaymentStatus, PaymentMethod) -> Unit,
    onDeleteOrder: (SocialOrder) -> Unit,
    onNotifyMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var showCourierDialog by remember { mutableStateOf(false) }
    var targetDialogStage by remember { mutableStateOf(OrderStage.SHIPPED) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showCancelConfirm by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = order?.orderCode ?: "Order Detail",
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFontFamily)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("detail_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (order != null && !order.stageEnum.isTerminal) {
                        IconButton(
                            onClick = { onEditOrder(order) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("detail_edit_button")
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit order")
                        }
                    }
                    if (order != null) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("detail_delete_button")
                        ) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete order",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (order == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Order not found or deleted.", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onBack) { Text("Return to Orders") }
            }
            return@Scaffold
        }

        val currentStage = order.stageEnum
        val nextStage = currentStage.nextActiveStage()
        val dmPreview = remember(order, sellerProfile) {
            buildDmStatusUpdate(order, sellerProfile.storeName, sellerProfile.defaultGreeting)
        }

        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
        ) {
            // Header & Stage Progress Card
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ChannelBadge(order.channelEnum)
                            StagePill(currentStage)
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(order.customerName, style = MaterialTheme.typography.headlineSmall)
                                Text(
                                    text = order.handleOrPhone,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = formatMoney(order.totalCents, order.currencySymbol),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        OrderProgressBar(currentStage)

                        if (currentStage.isTerminal) {
                            Surface(
                                color = stageColor(currentStage).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Order is finalized (${currentStage.label}) and locked against edits.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = stageColor(currentStage),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (nextStage != null) {
                                    Button(
                                        onClick = {
                                            if (nextStage == OrderStage.SHIPPED) {
                                                targetDialogStage = OrderStage.SHIPPED
                                                showCourierDialog = true
                                            } else {
                                                onAdvanceStage(order, nextStage, order.courierName, order.trackingNumber, null)
                                            }
                                        },
                                        enabled = !isMutating,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("detail_advance_button")
                                    ) {
                                        Text("Advance to ${nextStage.label}")
                                    }
                                }
                                FilledTonalButton(
                                    onClick = {
                                        targetDialogStage = if (currentStage.stepIndex < OrderStage.SHIPPED.stepIndex) {
                                            OrderStage.SHIPPED
                                        } else {
                                            currentStage
                                        }
                                        showCourierDialog = true
                                    },
                                    enabled = !isMutating,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Rounded.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Courier / Tracking")
                                }
                                OutlinedButton(
                                    onClick = { showCancelConfirm = true },
                                    enabled = !isMutating,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Rounded.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Cancel Order")
                                }
                            }
                        }
                    }
                }
            }

            // Customer DM Outreach Card
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text("Customer DM Status Update", style = MaterialTheme.typography.titleMedium)
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = dmPreview,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    OutreachActions.copyText(context, "DM Update", dmPreview) {
                                        onNotifyMessage("DM update copied")
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("detail_copy_dm_button")
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy")
                            }
                            Button(
                                onClick = { OutreachActions.openWhatsAppOrShare(context, order, sellerProfile) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("detail_send_dm_button")
                            ) {
                                Icon(
                                    imageVector = if (order.channelEnum == com.example.model.OrderChannel.WHATSAPP) {
                                        Icons.AutoMirrored.Rounded.Send
                                    } else {
                                        Icons.Rounded.Share
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (order.channelEnum == com.example.model.OrderChannel.WHATSAPP) "WhatsApp" else "Share DM")
                            }
                        }
                    }
                }
            }

            // Order Items, Payment & Shipping Details
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text("Items & Fulfillment", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${order.itemsSummary} (${order.itemCount} unit${if (order.itemCount == 1L) "" else "s"})",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        if (!order.shippingAddress.isNullOrBlank()) {
                            Text(
                                text = "Address: ${order.shippingAddress}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!order.courierName.isNullOrBlank() || !order.trackingNumber.isNullOrBlank()) {
                            Text(
                                text = "Courier: ${order.courierName ?: "—"} · Tracking: ${order.trackingNumber ?: "—"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (!order.notes.isNullOrBlank()) {
                            Text(
                                text = "Notes: ${order.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!order.dmMessageSnapshot.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Original DM", style = MaterialTheme.typography.labelSmall)
                                    Text(order.dmMessageSnapshot, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        PaymentBadge(order.paymentStatusEnum, order.paymentMethodEnum)
                        if (currentStage != OrderStage.CANCELLED) {
                            Text("Quick Payment Update", style = MaterialTheme.typography.labelMedium)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                PaymentStatus.entries.forEach { st ->
                                    FilterChip(
                                        selected = order.paymentStatusEnum == st,
                                        onClick = {
                                            if (order.paymentStatusEnum != st) {
                                                onUpdatePayment(order, st, order.paymentMethodEnum)
                                            }
                                        },
                                        label = { Text(st.label) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Timeline Events
            item {
                Text("Delivery Timeline", style = MaterialTheme.typography.titleMedium)
            }

            when (eventsState) {
                is UiState.Loading -> item { LoadingStateView("Loading timeline...") }
                is UiState.Error -> item { ErrorStateView(eventsState.message) }
                is UiState.Success -> {
                    items(eventsState.data, key = { it.id }) { event ->
                        val color = stageColor(event.stageEnum)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Icon(
                                    imageVector = stageIcon(event.stageEnum),
                                    contentDescription = null,
                                    tint = color,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(event.title, style = MaterialTheme.typography.titleSmall)
                                    if (!event.note.isNullOrBlank()) {
                                        Text(
                                            text = event.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = formatTimestampShort(event.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
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

    if (showCourierDialog && order != null) {
        CourierDispatchDialog(
            order = order,
            targetStage = targetDialogStage,
            onDismiss = { showCourierDialog = false },
            onConfirm = { courier, tracking, note ->
                onAdvanceStage(
                    order,
                    targetDialogStage,
                    courier.ifBlank { null },
                    tracking.ifBlank { null },
                    note.ifBlank { null }
                )
                showCourierDialog = false
            }
        )
    }

    if (showCancelConfirm && order != null) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text("Cancel ${order.orderCode}?") },
            text = { Text("Cancelling marks this order as finalized and locks further stage changes.") },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelConfirm = false
                        onAdvanceStage(order, OrderStage.CANCELLED, order.courierName, order.trackingNumber, "Order cancelled by seller")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cancel Order")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) { Text("Keep Order") }
            }
        )
    }

    if (showDeleteConfirm && order != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${order.orderCode}?") },
            text = { Text("This permanently deletes the order and its timeline history.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteOrder(order)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
