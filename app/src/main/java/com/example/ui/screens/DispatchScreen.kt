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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.model.OrderStage
import com.example.model.SellerProfile
import com.example.model.SocialOrder
import com.example.ui.components.ChannelBadge
import com.example.ui.components.CourierDispatchDialog
import com.example.ui.components.EmptyArtView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.KabarakZoneChip
import com.example.ui.components.LoadingStateView
import com.example.ui.components.OutreachActions
import com.example.ui.components.ServiceCategoryBadge
import com.example.ui.components.StagePill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.UiState

@Composable
fun DispatchScreen(
    ordersState: UiState<List<SocialOrder>>,
    incomingMarketplaceOrdersState: UiState<List<MarketplaceOrder>>,
    sellerProfile: SellerProfile,
    onMarketplaceOrderClick: (MarketplaceOrder) -> Unit,
    onAdvanceMarketplaceOrder: (MarketplaceOrder, OrderStage, String?, String?, String?) -> Unit,
    onOrderClick: (SocialOrder) -> Unit,
    onAdvanceOrder: (SocialOrder, OrderStage, String?, String?, String?) -> Unit,
    onNotifyMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var shipDialogOrder by remember { mutableStateOf<SocialOrder?>(null) }
    var shipDialogMarketplaceOrder by remember { mutableStateOf<MarketplaceOrder?>(null) }

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
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "FULFILLMENT & KABARAK RUNNERS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Dispatch Queue",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }

            if (ordersState is UiState.Loading && incomingMarketplaceOrdersState is UiState.Loading) {
                item { LoadingStateView("Loading dispatch queue...") }
            } else if (ordersState is UiState.Error && incomingMarketplaceOrdersState is UiState.Error) {
                item { ErrorStateView(ordersState.message) }
            } else {
                val socialList = (ordersState as? UiState.Success)?.data.orEmpty()
                val marketplaceList = (incomingMarketplaceOrdersState as? UiState.Success)?.data.orEmpty()

                val kabarakActiveDispatch = marketplaceList.filter {
                    it.stageEnum == OrderStage.PAID ||
                        it.stageEnum == OrderStage.PACKING ||
                        it.stageEnum == OrderStage.SHIPPED ||
                        it.stageEnum == OrderStage.OUT_FOR_DELIVERY
                }
                val toPack = socialList.filter {
                    it.stageEnum == OrderStage.PAID || it.stageEnum == OrderStage.PACKING
                }
                val inTransit = socialList.filter {
                    it.stageEnum == OrderStage.SHIPPED || it.stageEnum == OrderStage.OUT_FOR_DELIVERY
                }

                if (kabarakActiveDispatch.isEmpty() && toPack.isEmpty() && inTransit.isEmpty()) {
                    item {
                        EmptyArtView(
                            title = stringResource(R.string.empty_dispatch_title),
                            subtitle = stringResource(R.string.empty_dispatch_subtitle),
                            showArtwork = true
                        )
                    }
                } else {
                    if (kabarakActiveDispatch.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Kabarak Service Deliveries",
                                count = kabarakActiveDispatch.size,
                                icon = Icons.Rounded.Storefront
                            )
                        }
                        items(kabarakActiveDispatch, key = { "mkt_disp_${it.id}" }) { order ->
                            val nextStage = order.stageEnum.nextActiveStage() ?: OrderStage.DELIVERED
                            val primaryLabel = when (order.stageEnum) {
                                OrderStage.PAID -> "Start Preparing"
                                OrderStage.PACKING -> "Assign Rider & Ship"
                                OrderStage.SHIPPED -> "Out for Delivery"
                                else -> "Mark Delivered"
                            }
                            MarketplaceDispatchCard(
                                order = order,
                                primaryActionLabel = primaryLabel,
                                onCardClick = { onMarketplaceOrderClick(order) },
                                onPrimaryAction = {
                                    if (order.stageEnum == OrderStage.PACKING) {
                                        shipDialogMarketplaceOrder = order
                                    } else {
                                        onAdvanceMarketplaceOrder(order, nextStage, order.courierName, order.trackingNumber, null)
                                    }
                                },
                                onSendUpdate = {
                                    OutreachActions.openWhatsAppOrShareMarketplace(context, order, isSeller = true)
                                }
                            )
                        }
                    }

                    if (toPack.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "DM Orders to Pack & Ship",
                                count = toPack.size,
                                icon = Icons.Rounded.Inventory2
                            )
                        }
                        items(toPack, key = { "pack_${it.id}" }) { order ->
                            DispatchCard(
                                order = order,
                                primaryActionLabel = if (order.stageEnum == OrderStage.PAID) "Start Packing" else "Assign Courier & Ship",
                                onCardClick = { onOrderClick(order) },
                                onPrimaryAction = {
                                    if (order.stageEnum == OrderStage.PAID) {
                                        onAdvanceOrder(order, OrderStage.PACKING, order.courierName, order.trackingNumber, null)
                                    } else {
                                        shipDialogOrder = order
                                    }
                                },
                                onCopyTracking = {
                                    val code = order.trackingNumber ?: order.orderCode
                                    OutreachActions.copyText(context, "Order code", code) {
                                        onNotifyMessage("Copied $code")
                                    }
                                },
                                onSendUpdate = {
                                    OutreachActions.openWhatsAppOrShare(context, order, sellerProfile)
                                }
                            )
                        }
                    }

                    if (inTransit.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            SectionHeader(
                                title = "DM Orders in Transit",
                                count = inTransit.size,
                                icon = Icons.Rounded.LocalShipping
                            )
                        }
                        items(inTransit, key = { "transit_${it.id}" }) { order ->
                            val nextStage = order.stageEnum.nextActiveStage() ?: OrderStage.DELIVERED
                            DispatchCard(
                                order = order,
                                primaryActionLabel = if (nextStage == OrderStage.DELIVERED) "Mark Delivered" else "Out for Delivery",
                                onCardClick = { onOrderClick(order) },
                                onPrimaryAction = {
                                    onAdvanceOrder(order, nextStage, order.courierName, order.trackingNumber, null)
                                },
                                onCopyTracking = {
                                    val copyValue = order.trackingNumber ?: order.orderCode
                                    OutreachActions.copyText(context, "Tracking", copyValue) {
                                        onNotifyMessage("Copied $copyValue")
                                    }
                                },
                                onSendUpdate = {
                                    OutreachActions.openWhatsAppOrShare(context, order, sellerProfile)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    val activeShipOrder = shipDialogOrder
    if (activeShipOrder != null) {
        CourierDispatchDialog(
            order = activeShipOrder,
            targetStage = OrderStage.SHIPPED,
            onDismiss = { shipDialogOrder = null },
            onConfirm = { courier, tracking, note ->
                onAdvanceOrder(
                    activeShipOrder,
                    OrderStage.SHIPPED,
                    courier.ifBlank { null },
                    tracking.ifBlank { null },
                    note.ifBlank { null }
                )
                shipDialogOrder = null
            }
        )
    }

    val activeMktShipOrder = shipDialogMarketplaceOrder
    if (activeMktShipOrder != null) {
        CourierDispatchDialog(
            orderCode = activeMktShipOrder.orderCode,
            recipientName = activeMktShipOrder.buyerName,
            initialCourier = activeMktShipOrder.courierName,
            initialTracking = activeMktShipOrder.trackingNumber,
            targetStage = OrderStage.SHIPPED,
            onDismiss = { shipDialogMarketplaceOrder = null },
            onConfirm = { courier, tracking, note ->
                onAdvanceMarketplaceOrder(
                    activeMktShipOrder,
                    OrderStage.SHIPPED,
                    courier.ifBlank { null },
                    tracking.ifBlank { null },
                    note.ifBlank { null }
                )
                shipDialogMarketplaceOrder = null
            }
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = "$title ($count)",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun MarketplaceDispatchCard(
    order: MarketplaceOrder,
    primaryActionLabel: String,
    onCardClick: () -> Unit,
    onPrimaryAction: () -> Unit,
    onSendUpdate: () -> Unit
) {
    Surface(
        onClick = onCardClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("marketplace_dispatch_card_${order.id}")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ServiceCategoryBadge(order.categoryEnum)
                    KabarakZoneChip(order.deliveryZoneEnum)
                }
                StagePill(order.stageEnum)
            }

            Text(
                text = "${order.orderCode} · ${order.quantity}x ${order.serviceTitle}",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "${order.buyerName} (${order.buyerHandleOrPhone}) · ${order.deliveryDetails}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledTonalButton(
                    onClick = onSendUpdate,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Notify")
                }

                Button(
                    onClick = onPrimaryAction,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(primaryActionLabel)
                }
            }
        }
    }
}

@Composable
private fun DispatchCard(
    order: SocialOrder,
    primaryActionLabel: String,
    onCardClick: () -> Unit,
    onPrimaryAction: () -> Unit,
    onCopyTracking: () -> Unit,
    onSendUpdate: () -> Unit
) {
    Surface(
        onClick = onCardClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dispatch_card_${order.id}")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChannelBadge(order.channelEnum)
                    Text(
                        text = order.orderCode,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                StagePill(order.stageEnum)
            }

            Text(
                text = "${order.customerName} · ${order.handleOrPhone}",
                style = MaterialTheme.typography.titleSmall
            )

            Text(
                text = order.itemsSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!order.shippingAddress.isNullOrBlank()) {
                Text(
                    text = "Ship to: ${order.shippingAddress}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!order.courierName.isNullOrBlank() || !order.trackingNumber.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = listOfNotNull(order.courierName, order.trackingNumber).joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onCopyTracking,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy tracking number",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledTonalButton(
                    onClick = onSendUpdate,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Notify")
                }

                Button(
                    onClick = onPrimaryAction,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("dispatch_primary_${order.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(primaryActionLabel)
                }
            }
        }
    }
}
