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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.MarketplaceOrder
import com.example.model.OrderChannel
import com.example.model.OrderStage
import com.example.model.PaymentStatus
import com.example.model.SellerProfile
import com.example.model.SocialOrder
import com.example.model.formatMoney
import com.example.ui.components.CourierDispatchDialog
import com.example.ui.components.EmptyArtView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.components.MarketplaceOrderCard
import com.example.ui.components.OrderCard
import com.example.ui.components.OutreachActions
import com.example.ui.theme.InstagramMagenta
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.StageFilter
import com.example.ui.viewmodel.UiState

private enum class PipelineSegment(val label: String) {
    INCOMING("Incoming"),
    MY_REQUESTS("My Requests"),
    DM_LOG("DM Log")
}

@Composable
fun PipelineScreen(
    ordersState: UiState<List<SocialOrder>>,
    filteredOrdersState: UiState<List<SocialOrder>>,
    incomingMarketplaceOrdersState: UiState<List<MarketplaceOrder>>,
    myPlacedOrdersState: UiState<List<MarketplaceOrder>>,
    unreadNotificationsCount: Int,
    sellerProfile: SellerProfile,
    searchQuery: String,
    channelFilter: OrderChannel?,
    stageFilter: StageFilter,
    onSearchChange: (String) -> Unit,
    onChannelFilterChange: (OrderChannel?) -> Unit,
    onStageFilterChange: (StageFilter) -> Unit,
    onOpenNotifications: () -> Unit,
    onBrowseServicesClick: () -> Unit,
    onMarketplaceOrderClick: (MarketplaceOrder) -> Unit,
    onAdvanceMarketplaceOrder: (MarketplaceOrder, OrderStage, String?, String?, String?) -> Unit,
    onOrderClick: (SocialOrder) -> Unit,
    onCreateOrderClick: () -> Unit,
    onAdvanceOrder: (SocialOrder, OrderStage, String?, String?, String?) -> Unit,
    onNotifyMessage: (String) -> Unit,
    onOpenSellerDashboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeSegment by remember { mutableStateOf(PipelineSegment.INCOMING) }
    var dispatchTargetSocialOrder by remember { mutableStateOf<SocialOrder?>(null) }
    var dispatchTargetMarketplaceOrder by remember { mutableStateOf<MarketplaceOrder?>(null) }

    val allSocialOrders = (ordersState as? UiState.Success)?.data.orEmpty()
    val allIncoming = (incomingMarketplaceOrdersState as? UiState.Success)?.data.orEmpty()
    val allPlaced = (myPlacedOrdersState as? UiState.Success)?.data.orEmpty()

    val activeCount = allIncoming.count { !it.stageEnum.isTerminal } +
        allSocialOrders.count { !it.stageEnum.isTerminal }
    val unpaidCount = allIncoming.count {
        !it.stageEnum.isTerminal && (it.paymentStatusEnum == PaymentStatus.PENDING || it.paymentStatusEnum == PaymentStatus.PARTIAL)
    } + allSocialOrders.count {
        !it.stageEnum.isTerminal && (it.paymentStatusEnum == PaymentStatus.PENDING || it.paymentStatusEnum == PaymentStatus.PARTIAL)
    }
    val inTransitCount = allIncoming.count {
        it.stageEnum == OrderStage.SHIPPED || it.stageEnum == OrderStage.OUT_FOR_DELIVERY
    } + allSocialOrders.count {
        it.stageEnum == OrderStage.SHIPPED || it.stageEnum == OrderStage.OUT_FOR_DELIVERY
    }
    val collectedCents = allIncoming.filter { it.paymentStatusEnum == PaymentStatus.PAID }.sumOf { it.totalCents } +
        allSocialOrders.filter { it.paymentStatusEnum == PaymentStatus.PAID }.sumOf { it.totalCents }

    val filterMarketplaceList: (List<MarketplaceOrder>) -> List<MarketplaceOrder> = { list ->
        val q = searchQuery.trim().lowercase()
        list.filter { order ->
            val matchesStage = when (stageFilter) {
                StageFilter.ALL -> true
                StageFilter.ACTIVE -> !order.stageEnum.isTerminal
                StageFilter.NEW_DM -> order.stageEnum == OrderStage.NEW_DM
                StageFilter.PAID -> order.stageEnum == OrderStage.PAID
                StageFilter.PACKING -> order.stageEnum == OrderStage.PACKING
                StageFilter.SHIPPED -> order.stageEnum == OrderStage.SHIPPED
                StageFilter.OUT_FOR_DELIVERY -> order.stageEnum == OrderStage.OUT_FOR_DELIVERY
                StageFilter.DELIVERED -> order.stageEnum == OrderStage.DELIVERED
                StageFilter.CANCELLED -> order.stageEnum == OrderStage.CANCELLED
            }
            val matchesQuery = q.isEmpty() ||
                order.orderCode.lowercase().contains(q) ||
                order.serviceTitle.lowercase().contains(q) ||
                order.buyerName.lowercase().contains(q) ||
                order.sellerName.lowercase().contains(q) ||
                order.deliveryZoneEnum.label.lowercase().contains(q) ||
                order.deliveryDetails.lowercase().contains(q)
            matchesStage && matchesQuery
        }
    }

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
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = sellerProfile.storeName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Order Pipeline",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onOpenSellerDashboard,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("open_seller_dashboard_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Dashboard,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dashboard")
                        }

                        IconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("pipeline_notifications_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotificationsCount > 0) {
                                        Badge { Text(unreadNotificationsCount.toString()) }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Notifications,
                                    contentDescription = "Notifications"
                                )
                            }
                        }
                    }
                }
            }

            // KPI Strip
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PipelineKpiCard(
                        label = "Active",
                        value = activeCount.toString(),
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    PipelineKpiCard(
                        label = "Unpaid",
                        value = unpaidCount.toString(),
                        accent = WarningAmber,
                        modifier = Modifier.weight(1f)
                    )
                    PipelineKpiCard(
                        label = "In Transit",
                        value = inTransitCount.toString(),
                        accent = InstagramMagenta,
                        modifier = Modifier.weight(1f)
                    )
                    PipelineKpiCard(
                        label = "Collected",
                        value = formatMoney(collectedCents, sellerProfile.currencySymbol),
                        accent = SuccessEmerald,
                        modifier = Modifier.weight(1.35f)
                    )
                }
            }

            // Segment Switcher: Incoming (Provider) / My Requests (Customer) / DM Log
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    PipelineSegment.entries.forEachIndexed { idx, seg ->
                        val count = when (seg) {
                            PipelineSegment.INCOMING -> allIncoming.size
                            PipelineSegment.MY_REQUESTS -> allPlaced.size
                            PipelineSegment.DM_LOG -> allSocialOrders.size
                        }
                        SegmentedButton(
                            selected = activeSegment == seg,
                            onClick = { activeSegment = seg },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = idx,
                                count = PipelineSegment.entries.size
                            ),
                            modifier = Modifier.testTag("pipeline_segment_${seg.name.lowercase()}")
                        ) {
                            Text("${seg.label} ($count)", maxLines = 1)
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search order code, customer, service, or hostel...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("orders_search_input")
                )
            }

            // Channel & Stage Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (activeSegment == PipelineSegment.DM_LOG) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = channelFilter == null,
                                    onClick = { onChannelFilterChange(null) },
                                    label = { Text("All Channels") },
                                    modifier = Modifier.testTag("filter_channel_all")
                                )
                            }
                            item {
                                FilterChip(
                                    selected = channelFilter == OrderChannel.WHATSAPP,
                                    onClick = {
                                        onChannelFilterChange(
                                            if (channelFilter == OrderChannel.WHATSAPP) null else OrderChannel.WHATSAPP
                                        )
                                    },
                                    label = { Text("WhatsApp (${allSocialOrders.count { it.channelEnum == OrderChannel.WHATSAPP }})") },
                                    modifier = Modifier.testTag("filter_channel_whatsapp")
                                )
                            }
                            item {
                                FilterChip(
                                    selected = channelFilter == OrderChannel.INSTAGRAM,
                                    onClick = {
                                        onChannelFilterChange(
                                            if (channelFilter == OrderChannel.INSTAGRAM) null else OrderChannel.INSTAGRAM
                                        )
                                    },
                                    label = { Text("Instagram (${allSocialOrders.count { it.channelEnum == OrderChannel.INSTAGRAM }})") },
                                    modifier = Modifier.testTag("filter_channel_instagram")
                                )
                            }
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(StageFilter.entries) { filter ->
                            FilterChip(
                                selected = stageFilter == filter,
                                onClick = { onStageFilterChange(filter) },
                                label = { Text(filter.label) },
                                modifier = Modifier.testTag("filter_stage_${filter.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            when (activeSegment) {
                PipelineSegment.INCOMING -> {
                    when (incomingMarketplaceOrdersState) {
                        is UiState.Loading -> item { LoadingStateView("Syncing incoming Kabarak orders...") }
                        is UiState.Error -> item { ErrorStateView(incomingMarketplaceOrdersState.message) }
                        is UiState.Success -> {
                            val filtered = filterMarketplaceList(incomingMarketplaceOrdersState.data)
                            if (filtered.isEmpty()) {
                                val hasFilters = searchQuery.isNotBlank() || stageFilter != StageFilter.ALL
                                item {
                                    EmptyArtView(
                                        title = if (hasFilters) "No matching incoming orders" else "No incoming service orders yet",
                                        subtitle = if (hasFilters) {
                                            "Clear search or stage filters to see all customer requests."
                                        } else {
                                            "Offer food, water, laundry, or local Kabarak services in the Services tab. Whenever a customer orders, you'll be notified instantly here."
                                        },
                                        actionLabel = if (hasFilters) "Clear Filters" else "Go to Services",
                                        onAction = {
                                            if (hasFilters) {
                                                onSearchChange("")
                                                onStageFilterChange(StageFilter.ALL)
                                            } else {
                                                onBrowseServicesClick()
                                            }
                                        },
                                        showArtwork = !hasFilters
                                    )
                                }
                            } else {
                                items(filtered, key = { "in_${it.id}" }) { order ->
                                    MarketplaceOrderCard(
                                        order = order,
                                        isSellerView = true,
                                        onClick = { onMarketplaceOrderClick(order) },
                                        onQuickAdvance = {
                                            val next = order.stageEnum.nextActiveStage() ?: return@MarketplaceOrderCard
                                            if (next == OrderStage.SHIPPED) {
                                                dispatchTargetMarketplaceOrder = order
                                            } else {
                                                onAdvanceMarketplaceOrder(order, next, order.courierName, order.trackingNumber, null)
                                            }
                                        },
                                        onDmClick = {
                                            OutreachActions.openWhatsAppOrShareMarketplace(context, order, isSeller = true)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                PipelineSegment.MY_REQUESTS -> {
                    when (myPlacedOrdersState) {
                        is UiState.Loading -> item { LoadingStateView("Loading your placed orders...") }
                        is UiState.Error -> item { ErrorStateView(myPlacedOrdersState.message) }
                        is UiState.Success -> {
                            val filtered = filterMarketplaceList(myPlacedOrdersState.data)
                            if (filtered.isEmpty()) {
                                val hasFilters = searchQuery.isNotBlank() || stageFilter != StageFilter.ALL
                                item {
                                    EmptyArtView(
                                        title = if (hasFilters) "No matching orders" else "You haven't placed any service orders",
                                        subtitle = if (hasFilters) {
                                            "Clear search or stage filters to view all your requests."
                                        } else {
                                            "Browse food, clean water delivery, gas, laundry, and other services around Kabarak University."
                                        },
                                        actionLabel = if (hasFilters) "Clear Filters" else "Browse Services",
                                        onAction = {
                                            if (hasFilters) {
                                                onSearchChange("")
                                                onStageFilterChange(StageFilter.ALL)
                                            } else {
                                                onBrowseServicesClick()
                                            }
                                        },
                                        showArtwork = !hasFilters
                                    )
                                }
                            } else {
                                items(filtered, key = { "out_${it.id}" }) { order ->
                                    MarketplaceOrderCard(
                                        order = order,
                                        isSellerView = false,
                                        onClick = { onMarketplaceOrderClick(order) },
                                        onQuickAdvance = null,
                                        onDmClick = {
                                            OutreachActions.openWhatsAppOrShareMarketplace(context, order, isSeller = false)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                PipelineSegment.DM_LOG -> {
                    when (filteredOrdersState) {
                        is UiState.Loading -> item { LoadingStateView() }
                        is UiState.Error -> item { ErrorStateView(filteredOrdersState.message) }
                        is UiState.Success -> {
                            val list = filteredOrdersState.data
                            if (list.isEmpty()) {
                                item {
                                    val hasFilters = searchQuery.isNotBlank() || channelFilter != null || stageFilter != StageFilter.ALL
                                    EmptyArtView(
                                        title = if (hasFilters) "No matching orders" else stringResource(R.string.empty_orders_title),
                                        subtitle = if (hasFilters) {
                                            "Try clearing your search or switching channel and stage filters."
                                        } else {
                                            stringResource(R.string.empty_orders_subtitle)
                                        },
                                        actionLabel = if (hasFilters) "Clear Filters" else "Log DM Order",
                                        onAction = {
                                            if (hasFilters) {
                                                onSearchChange("")
                                                onChannelFilterChange(null)
                                                onStageFilterChange(StageFilter.ALL)
                                            } else {
                                                onCreateOrderClick()
                                            }
                                        },
                                        showArtwork = !hasFilters
                                    )
                                }
                            } else {
                                items(list, key = { it.id }) { order ->
                                    OrderCard(
                                        order = order,
                                        onClick = { onOrderClick(order) },
                                        onAdvanceClick = {
                                            val next = order.stageEnum.nextActiveStage() ?: return@OrderCard
                                            if (next == OrderStage.SHIPPED) {
                                                dispatchTargetSocialOrder = order
                                            } else {
                                                onAdvanceOrder(order, next, order.courierName, order.trackingNumber, null)
                                            }
                                        },
                                        onDmUpdateClick = {
                                            OutreachActions.copyDmUpdate(context, order, sellerProfile) {
                                                onNotifyMessage("Copied DM update for ${order.orderCode}")
                                            }
                                            OutreachActions.openWhatsAppOrShare(context, order, sellerProfile)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val pendingSocial = dispatchTargetSocialOrder
    if (pendingSocial != null) {
        CourierDispatchDialog(
            order = pendingSocial,
            targetStage = OrderStage.SHIPPED,
            onDismiss = { dispatchTargetSocialOrder = null },
            onConfirm = { courier, tracking, note ->
                onAdvanceOrder(
                    pendingSocial,
                    OrderStage.SHIPPED,
                    courier.ifBlank { null },
                    tracking.ifBlank { null },
                    note.ifBlank { null }
                )
                dispatchTargetSocialOrder = null
            }
        )
    }

    val pendingMarketplace = dispatchTargetMarketplaceOrder
    if (pendingMarketplace != null) {
        CourierDispatchDialog(
            orderCode = pendingMarketplace.orderCode,
            recipientName = pendingMarketplace.buyerName,
            initialCourier = pendingMarketplace.courierName,
            initialTracking = pendingMarketplace.trackingNumber,
            targetStage = OrderStage.SHIPPED,
            onDismiss = { dispatchTargetMarketplaceOrder = null },
            onConfirm = { courier, tracking, note ->
                onAdvanceMarketplaceOrder(
                    pendingMarketplace,
                    OrderStage.SHIPPED,
                    courier.ifBlank { null },
                    tracking.ifBlank { null },
                    note.ifBlank { null }
                )
                dispatchTargetMarketplaceOrder = null
            }
        )
    }
}

@Composable
private fun PipelineKpiCard(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = accent,
                maxLines = 1
            )
        }
    }
}
