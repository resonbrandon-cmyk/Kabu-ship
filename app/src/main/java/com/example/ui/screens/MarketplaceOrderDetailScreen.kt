package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.model.MarketplaceOrder
import com.example.model.MarketplaceOrderEvent
import com.example.model.OrderMessage
import com.example.model.OrderStage
import com.example.model.PaymentMethod
import com.example.model.PaymentStatus
import com.example.model.buildMarketplaceDmMessage
import com.example.model.formatMoney
import com.example.model.formatTimestampShort
import com.example.ui.components.CourierDispatchDialog
import com.example.ui.components.ErrorStateView
import com.example.ui.components.KabarakZoneChip
import com.example.ui.components.LoadingStateView
import com.example.ui.components.OrderProgressBar
import com.example.ui.components.OutreachActions
import com.example.ui.components.PaymentBadge
import com.example.ui.components.ServiceCategoryBadge
import com.example.ui.components.StagePill
import com.example.ui.components.stageColor
import com.example.ui.components.stageIcon
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MarketplaceOrderDetailScreen(
    currentUserId: String,
    currentUserName: String,
    orderState: UiState<MarketplaceOrder?>,
    eventsState: UiState<List<MarketplaceOrderEvent>>,
    messagesState: UiState<List<OrderMessage>>,
    isMutating: Boolean,
    onBack: () -> Unit,
    onAdvanceStage: (MarketplaceOrder, OrderStage, String?, String?, String?) -> Unit,
    onUpdatePayment: (MarketplaceOrder, PaymentStatus, PaymentMethod) -> Unit,
    onSendMessage: (MarketplaceOrder, String, Boolean, String, () -> Unit) -> Unit,
    onNotifyMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val order = (orderState as? UiState.Success)?.data
    val isSeller = order?.sellerUid == currentUserId
    val isBuyer = order?.buyerUid == currentUserId
    val isSelfTest = isSeller && isBuyer

    var chatInput by remember { mutableStateOf("") }
    var sendAsSellerInSelfTest by remember { mutableStateOf(true) }
    var showCourierDialog by remember { mutableStateOf(false) }
    var showCancelConfirm by remember { mutableStateOf(false) }

    val sendAsSeller = if (isSelfTest) sendAsSellerInSelfTest else isSeller

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = order?.orderCode ?: "Service Order",
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFontFamily)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("marketplace_detail_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (order != null) {
                        IconButton(
                            onClick = { OutreachActions.openWhatsAppOrShareMarketplace(context, order, isSeller) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = "Send WhatsApp / DM message")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (order != null && !order.stageEnum.isTerminal) {
                Surface(
                    tonalElevation = 3.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        if (isSelfTest) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Chat as:", style = MaterialTheme.typography.labelSmall)
                                FilterChip(
                                    selected = sendAsSellerInSelfTest,
                                    onClick = { sendAsSellerInSelfTest = true },
                                    label = { Text("Provider") }
                                )
                                FilterChip(
                                    selected = !sendAsSellerInSelfTest,
                                    onClick = { sendAsSellerInSelfTest = false },
                                    label = { Text("Customer") }
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = chatInput,
                                onValueChange = { if (it.length <= 500) chatInput = it },
                                placeholder = {
                                    Text(
                                        if (sendAsSeller) "Message customer (e.g. Ready at Main Gate)..."
                                        else "Message provider (e.g. Gate / room instructions)..."
                                    )
                                },
                                maxLines = 3,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("marketplace_chat_input")
                            )
                            FilledTonalIconButton(
                                onClick = {
                                    val trimmed = chatInput.trim()
                                    if (trimmed.isNotEmpty()) {
                                        val senderDisplay = when {
                                            sendAsSeller -> order.sellerName
                                            order.buyerName.isNotBlank() -> order.buyerName
                                            else -> currentUserName
                                        }
                                        onSendMessage(order, senderDisplay, sendAsSeller, trimmed) {
                                            chatInput = ""
                                        }
                                    }
                                },
                                enabled = chatInput.trim().isNotEmpty(),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("marketplace_chat_send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = "Send chat message"
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        when (orderState) {
            is UiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                LoadingStateView("Loading live order...")
            }

            is UiState.Error -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                ErrorStateView(orderState.message)
            }

            is UiState.Success -> {
                if (order == null) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(24.dp)
                    ) {
                        Text("Order not found.", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onBack) { Text("Back") }
                    }
                    return@Scaffold
                }

                val currentStage = order.stageEnum
                val nextStage = currentStage.nextActiveStage()

                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = innerPadding.calculateTopPadding() + 8.dp,
                            bottom = innerPadding.calculateBottomPadding() + 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        // Role Banner
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = when {
                                        isSelfTest -> "Self-Order Preview · You can test both Provider fulfillment & Customer messaging"
                                        isSeller -> "Service Provider View · Updates notify ${order.buyerName} immediately"
                                        else -> "Customer View · Live updates from ${order.sellerName}"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }

                        // Order Summary & Interactive Controls
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
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            ServiceCategoryBadge(order.categoryEnum)
                                            KabarakZoneChip(order.deliveryZoneEnum)
                                        }
                                        StagePill(currentStage)
                                    }

                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${order.quantity}x ${order.serviceTitle}",
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                            Text(
                                                text = "Provider: ${order.sellerName} (${order.sellerHandleOrPhone})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Customer: ${order.buyerName} (${order.buyerHandleOrPhone})",
                                                style = MaterialTheme.typography.bodySmall,
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

                                    Text(
                                        text = "Drop-off: ${order.deliveryZoneEnum.label} · ${order.deliveryDetails}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (!order.notes.isNullOrBlank()) {
                                        Text(
                                            text = "Note: ${order.notes}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (!order.courierName.isNullOrBlank() || !order.trackingNumber.isNullOrBlank()) {
                                        Text(
                                            text = "Rider: ${order.courierName ?: "—"} · ${order.trackingNumber ?: "—"}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    OrderProgressBar(currentStage)
                                    PaymentBadge(order.paymentStatusEnum, order.paymentMethodEnum)

                                    if (currentStage.isTerminal) {
                                        Surface(
                                            color = stageColor(currentStage).copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Order completed/closed (${currentStage.label}). In-app chat is closed; use WhatsApp/DM above for follow-up.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = stageColor(currentStage),
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    } else {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (isSeller && nextStage != null) {
                                                Button(
                                                    onClick = {
                                                        if (nextStage == OrderStage.SHIPPED) {
                                                            showCourierDialog = true
                                                        } else {
                                                            onAdvanceStage(order, nextStage, order.courierName, order.trackingNumber, null)
                                                        }
                                                    },
                                                    enabled = !isMutating,
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier
                                                        .minimumInteractiveComponentSize()
                                                        .testTag("marketplace_detail_advance_button")
                                                ) {
                                                    Text("Advance to ${nextStage.label}")
                                                }
                                            }
                                            if (isSeller) {
                                                FilledTonalButton(
                                                    onClick = { showCourierDialog = true },
                                                    enabled = !isMutating,
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.minimumInteractiveComponentSize()
                                                ) {
                                                    Icon(Icons.Rounded.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Assign Rider")
                                                }
                                            }
                                            if (isSeller || (isBuyer && currentStage == OrderStage.NEW_DM)) {
                                                OutlinedButton(
                                                    onClick = { showCancelConfirm = true },
                                                    enabled = !isMutating,
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                    modifier = Modifier.minimumInteractiveComponentSize()
                                                ) {
                                                    Icon(Icons.Rounded.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Cancel")
                                                }
                                            }
                                        }

                                        if (isSeller) {
                                            Text("Payment Status", style = MaterialTheme.typography.labelMedium)
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

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                val msg = buildMarketplaceDmMessage(order, isSeller)
                                                OutreachActions.copyText(context, "Order update", msg) {
                                                    onNotifyMessage("Copied message")
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Copy Text")
                                        }
                                        FilledTonalButton(
                                            onClick = { OutreachActions.openWhatsAppOrShareMarketplace(context, order, isSeller) },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("WhatsApp / DM")
                                        }
                                    }
                                }
                            }
                        }

                        // Live Interactive Chat Section
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Chat,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text("Live Provider & Customer Chat", style = MaterialTheme.typography.titleMedium)
                            }
                        }

                        when (messagesState) {
                            is UiState.Loading -> item { LoadingStateView("Loading chat...") }
                            is UiState.Error -> item { ErrorStateView(messagesState.message) }
                            is UiState.Success -> {
                                val msgs = messagesState.data
                                if (msgs.isEmpty()) {
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "No messages yet. Say hello below to coordinate pickup or delivery!",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(14.dp)
                                            )
                                        }
                                    }
                                } else {
                                    items(msgs, key = { it.id }) { msg ->
                                        val isMine = if (isSelfTest) {
                                            (msg.senderRole == "SELLER") == sendAsSellerInSelfTest
                                        } else {
                                            msg.senderUid == currentUserId
                                        }
                                        ChatBubbleRow(message = msg, isMine = isMine)
                                    }
                                }
                            }
                        }

                        // Timeline Events Section
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Order Timeline", style = MaterialTheme.typography.titleMedium)
                        }

                        when (eventsState) {
                            is UiState.Loading -> item { LoadingStateView("Loading timeline...") }
                            is UiState.Error -> item { ErrorStateView(eventsState.message) }
                            is UiState.Success -> {
                                items(eventsState.data, key = { it.id }) { ev ->
                                    val color = stageColor(ev.stageEnum)
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
                                                imageVector = stageIcon(ev.stageEnum),
                                                contentDescription = null,
                                                tint = color,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(ev.title, style = MaterialTheme.typography.titleSmall)
                                                if (!ev.note.isNullOrBlank()) {
                                                    Text(
                                                        text = ev.note,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Text(
                                                    text = formatTimestampShort(ev.createdAt),
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

                if (showCourierDialog) {
                    CourierDispatchDialog(
                        orderCode = order.orderCode,
                        recipientName = order.buyerName,
                        initialCourier = order.courierName,
                        initialTracking = order.trackingNumber,
                        targetStage = if (currentStage.stepIndex < OrderStage.SHIPPED.stepIndex) OrderStage.SHIPPED else currentStage,
                        onDismiss = { showCourierDialog = false },
                        onConfirm = { courier, tracking, note ->
                            val target = if (currentStage.stepIndex < OrderStage.SHIPPED.stepIndex) {
                                OrderStage.SHIPPED
                            } else {
                                currentStage
                            }
                            onAdvanceStage(
                                order,
                                target,
                                courier.ifBlank { null },
                                tracking.ifBlank { null },
                                note.ifBlank { null }
                            )
                            showCourierDialog = false
                        }
                    )
                }

                if (showCancelConfirm) {
                    AlertDialog(
                        onDismissRequest = { showCancelConfirm = false },
                        title = { Text("Cancel ${order.orderCode}?") },
                        text = { Text("Both provider and customer will be notified.") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showCancelConfirm = false
                                    onAdvanceStage(
                                        order,
                                        OrderStage.CANCELLED,
                                        order.courierName,
                                        order.trackingNumber,
                                        if (isSeller) "Cancelled by service provider" else "Cancelled by customer"
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Cancel Order")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCancelConfirm = false }) { Text("Keep") }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleRow(
    message: OrderMessage,
    isMine: Boolean
) {
    Row(
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            color = if (isMine) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Text(
                    text = "${message.senderName} · ${if (message.senderRole == "SELLER") "Provider" else "Customer"}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isMine) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMine) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = formatTimestampShort(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
