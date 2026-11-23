package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.MarketplaceOrder
import com.example.model.OrderStage
import com.example.model.SellerProfile
import com.example.model.SocialOrder
import com.example.model.formatMoney
import com.example.model.formatTimestampShort
import com.example.ui.components.ChannelBadge
import com.example.ui.components.EmptyArtView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.KabarakZoneChip
import com.example.ui.components.LoadingStateView
import com.example.ui.components.ServiceCategoryBadge
import com.example.ui.components.stageColor
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StageNewDmColor
import com.example.ui.theme.StagePaidColor
import com.example.ui.theme.StageShippedColor
import com.example.ui.theme.SuccessEmerald
import com.example.ui.viewmodel.UiState

private enum class SellerDashboardFilter(val label: String) {
    ALL("All Incoming"),
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    DISPATCHED("Dispatched")
}

private sealed interface DashboardOrderItem {
    val key: String
    val orderCode: String
    val stage: OrderStage
    val sortEpochSeconds: Long

    data class Marketplace(val order: MarketplaceOrder) : DashboardOrderItem {
        override val key: String = "mkt_${order.id}"
        override val orderCode: String = order.orderCode
        override val stage: OrderStage = order.stageEnum
        override val sortEpochSeconds: Long = (order.updatedAt ?: order.createdAt)?.seconds ?: 0L
    }

    data class Direct(val order: SocialOrder) : DashboardOrderItem {
        override val key: String = "dm_${order.id}"
        override val orderCode: String = order.orderCode
        override val stage: OrderStage = order.stageEnum
        override val sortEpochSeconds: Long = (order.updatedAt ?: order.createdAt)?.seconds ?: 0L
    }
}

private fun dashboardStatusLabel(stage: OrderStage): String = when (stage) {
    OrderStage.NEW_DM -> "Pending"
    OrderStage.PAID, OrderStage.PACKING -> "Confirmed"
    OrderStage.SHIPPED, OrderStage.OUT_FOR_DELIVERY -> "Dispatched"
    OrderStage.DELIVERED -> "Delivered"
    OrderStage.CANCELLED -> "Cancelled"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerDashboardScreen(
    incomingMarketplaceOrdersState: UiState<List<MarketplaceOrder>>,
    directOrdersState: UiState<List<SocialOrder>>,
    sellerProfile: SellerProfile,
    unreadNotificationsCount: Int,
    isMutating: Boolean,
    onBack: () -> Unit,
    onOpenNotifications: () -> Unit,
    onConfirmMarketplaceOrder: (MarketplaceOrder) -> Unit,
    onDispatchMarketplaceOrder: (MarketplaceOrder) -> Unit,
    onOpenMarketplaceOrder: (MarketplaceOrder) -> Unit,
    onConfirmDirectOrder: (SocialOrder) -> Unit,
    onDispatchDirectOrder: (SocialOrder) -> Unit,
    onOpenDirectOrder: (SocialOrder) -> Unit
) {
    var activeFilter by remember { mutableStateOf(SellerDashboardFilter.ALL) }

    val marketplaceOrders = (incomingMarketplaceOrdersState as? UiState.Success)?.data.orEmpty()
    val directOrders = (directOrdersState as? UiState.Success)?.data.orEmpty()

    val combinedOrders = remember(marketplaceOrders, directOrders) {
        (marketplaceOrders.map { DashboardOrderItem.Marketplace(it) } +
            directOrders.map { DashboardOrderItem.Direct(it) })
            .sortedByDescending { it.sortEpochSeconds }
    }

    val pendingCount = remember(combinedOrders) {
        combinedOrders.count { it.stage == OrderStage.NEW_DM }
    }
    val confirmedCount = remember(combinedOrders) {
        combinedOrders.count { it.stage == OrderStage.PAID || it.stage == OrderStage.PACKING }
    }
    val dispatchedCount = remember(combinedOrders) {
        combinedOrders.count { it.stage == OrderStage.SHIPPED || it.stage == OrderStage.OUT_FOR_DELIVERY }
    }

    val filteredOrders = remember(combinedOrders, activeFilter) {
        combinedOrders.filter { item ->
            when (activeFilter) {
                SellerDashboardFilter.ALL -> true
                SellerDashboardFilter.PENDING -> item.stage == OrderStage.NEW_DM
                SellerDashboardFilter.CONFIRMED -> item.stage == OrderStage.PAID || item.stage == OrderStage.PACKING
                SellerDashboardFilter.DISPATCHED -> item.stage == OrderStage.SHIPPED || item.stage == OrderStage.OUT_FOR_DELIVERY
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Seller Dashboard",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = sellerProfile.storeName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("seller_dashboard_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("seller_dashboard_notifications_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationsCount > 0) {
                                    Badge { Text(unreadNotificationsCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.Notifications, contentDescription = "Open notifications")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp)
                    .align(Alignment.TopCenter)
                    .testTag("seller_dashboard_list")
            ) {
                // Summary Counts
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DashboardMetricTile(
                            label = "Pending",
                            count = pendingCount,
                            accent = StageNewDmColor,
                            modifier = Modifier.weight(1f)
                        )
                        DashboardMetricTile(
                            label = "Confirmed",
                            count = confirmedCount,
                            accent = StagePaidColor,
                            modifier = Modifier.weight(1f)
                        )
                        DashboardMetricTile(
                            label = "Dispatched",
                            count = dispatchedCount,
                            accent = StageShippedColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Filter Strip
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SellerDashboardFilter.entries) { filter ->
                            FilterChip(
                                selected = activeFilter == filter,
                                onClick = { activeFilter = filter },
                                label = { Text(filter.label) },
                                modifier = Modifier.testTag("seller_filter_${filter.name.lowercase()}")
                            )
                        }
                    }
                }

                val isLoading = incomingMarketplaceOrdersState is UiState.Loading &&
                    directOrdersState is UiState.Loading
                val errorMessage = (incomingMarketplaceOrdersState as? UiState.Error)?.message
                    ?: (directOrdersState as? UiState.Error)?.message

                if (isLoading) {
                    item { LoadingStateView("Loading incoming orders from Firestore...") }
                } else if (errorMessage != null && combinedOrders.isEmpty()) {
                    item { ErrorStateView(errorMessage) }
                } else if (filteredOrders.isEmpty()) {
                    item {
                        EmptyArtView(
                            title = if (activeFilter == SellerDashboardFilter.ALL) {
                                "No incoming orders yet"
                            } else {
                                "No ${activeFilter.label.lowercase()} orders"
                            },
                            subtitle = if (activeFilter == SellerDashboardFilter.ALL) {
                                "Incoming Kabarak customer orders and logged DM orders from Firestore will appear here ready to Confirm or Dispatch."
                            } else {
                                "Switch to All Incoming to view orders in other stages."
                            },
                            actionLabel = if (activeFilter != SellerDashboardFilter.ALL) "Show All Incoming" else null,
                            onAction = if (activeFilter != SellerDashboardFilter.ALL) {
                                { activeFilter = SellerDashboardFilter.ALL }
                            } else null,
                            showArtwork = activeFilter == SellerDashboardFilter.ALL
                        )
                    }
                } else {
                    items(filteredOrders, key = { it.key }) { item ->
                        when (item) {
                            is DashboardOrderItem.Marketplace -> {
                                val order = item.order
                                SellerOrderActionCard(
                                    orderId = order.id,
                                    orderCode = order.orderCode,
                                    title = "${order.quantity}x ${order.serviceTitle}",
                                    customerLine = "${order.buyerName} · ${order.buyerHandleOrPhone}",
                                    locationLine = "${order.deliveryZoneEnum.label} · ${order.deliveryDetails}",
                                    totalFormatted = formatMoney(order.totalCents, order.currencySymbol),
                                    timestampLabel = formatTimestampShort(order.updatedAt ?: order.createdAt),
                                    stage = order.stageEnum,
                                    isMutating = isMutating,
                                    badgeContent = {
                                        ServiceCategoryBadge(order.categoryEnum)
                                        KabarakZoneChip(order.deliveryZoneEnum)
                                    },
                                    onCardClick = { onOpenMarketplaceOrder(order) },
                                    onConfirmClick = { onConfirmMarketplaceOrder(order) },
                                    onDispatchClick = { onDispatchMarketplaceOrder(order) }
                                )
                            }

                            is DashboardOrderItem.Direct -> {
                                val order = item.order
                                SellerOrderActionCard(
                                    orderId = order.id,
                                    orderCode = order.orderCode,
                                    title = order.itemsSummary,
                                    customerLine = "${order.customerName} · ${order.handleOrPhone}",
                                    locationLine = order.shippingAddress?.takeIf { it.isNotBlank() } ?: "Direct DM Order",
                                    totalFormatted = formatMoney(order.totalCents, order.currencySymbol),
                                    timestampLabel = formatTimestampShort(order.updatedAt ?: order.createdAt),
                                    stage = order.stageEnum,
                                    isMutating = isMutating,
                                    badgeContent = {
                                        ChannelBadge(order.channelEnum)
                                    },
                                    onCardClick = { onOpenDirectOrder(order) },
                                    onConfirmClick = { onConfirmDirectOrder(order) },
                                    onDispatchClick = { onDispatchDirectOrder(order) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardMetricTile(
    label: String,
    count: Int,
    accent: Color,
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = accent
            )
        }
    }
}

@Composable
private fun SellerOrderActionCard(
    orderId: String,
    orderCode: String,
    title: String,
    customerLine: String,
    locationLine: String,
    totalFormatted: String,
    timestampLabel: String,
    stage: OrderStage,
    isMutating: Boolean,
    badgeContent: @Composable () -> Unit,
    onCardClick: () -> Unit,
    onConfirmClick: () -> Unit,
    onDispatchClick: () -> Unit
) {
    val isPending = stage == OrderStage.NEW_DM
    val isConfirmedOrBeyond = stage.stepIndex >= OrderStage.PAID.stepIndex && !stage.isTerminal || stage == OrderStage.DELIVERED
    val isDispatchedOrBeyond = stage.stepIndex >= OrderStage.SHIPPED.stepIndex
    val canConfirm = !isMutating && isPending
    val canDispatch = !isMutating && !stage.isTerminal && stage.stepIndex < OrderStage.SHIPPED.stepIndex
    val statusColor = stageColor(stage)

    Surface(
        onClick = onCardClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isPending) 2.dp else 1.dp,
        border = BorderStroke(
            width = if (isPending) 1.5.dp else 1.dp,
            color = if (isPending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("seller_order_card_$orderId")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    badgeContent()
                    Text(
                        text = orderCode,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = dashboardStatusLabel(stage),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = customerLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = locationLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = totalFormatted,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = timestampLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (stage != OrderStage.CANCELLED) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilledTonalButton(
                        onClick = onConfirmClick,
                        enabled = canConfirm,
                        shape = RoundedCornerShape(12.dp),
                        colors = if (isConfirmedOrBeyond) {
                            ButtonDefaults.filledTonalButtonColors(
                                disabledContainerColor = SuccessEmerald.copy(alpha = 0.14f),
                                disabledContentColor = SuccessEmerald
                            )
                        } else {
                            ButtonDefaults.filledTonalButtonColors()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("seller_confirm_button_$orderId")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isConfirmedOrBeyond) "Confirmed" else "Confirm")
                    }

                    Button(
                        onClick = onDispatchClick,
                        enabled = canDispatch,
                        shape = RoundedCornerShape(12.dp),
                        colors = if (isDispatchedOrBeyond) {
                            ButtonDefaults.buttonColors(
                                disabledContainerColor = StageShippedColor.copy(alpha = 0.16f),
                                disabledContentColor = StageShippedColor
                            )
                        } else {
                            ButtonDefaults.buttonColors()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("seller_dispatch_button_$orderId")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalShipping,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isDispatchedOrBeyond) "Dispatched" else "Dispatch")
                    }
                }
            }
        }
    }
}
