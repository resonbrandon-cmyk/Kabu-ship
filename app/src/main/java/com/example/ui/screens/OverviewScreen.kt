package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.MarketplaceOrder
import com.example.model.OrderChannel
import com.example.model.OrderStage
import com.example.model.PaymentStatus
import com.example.model.SellerProfile
import com.example.model.SocialOrder
import com.example.model.formatMoney
import com.example.ui.components.OutreachActions
import com.example.ui.components.stageColor
import com.example.ui.theme.InstagramMagenta
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OverviewScreen(
    ordersState: UiState<List<SocialOrder>>,
    incomingMarketplaceOrdersState: UiState<List<MarketplaceOrder>>,
    myPlacedOrdersState: UiState<List<MarketplaceOrder>>,
    unreadNotificationsCount: Int,
    sellerProfile: SellerProfile,
    userEmail: String?,
    isMutating: Boolean,
    onOpenNotifications: () -> Unit,
    onSaveProfile: (storeName: String, currencySymbol: String, defaultGreeting: String?) -> Unit,
    onSignOut: () -> Unit,
    onOpenSellerDashboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var storeName by remember(sellerProfile.storeName) { mutableStateOf(sellerProfile.storeName) }
    var currencySymbol by remember(sellerProfile.currencySymbol) { mutableStateOf(sellerProfile.currencySymbol) }
    var greeting by remember(sellerProfile.defaultGreeting) { mutableStateOf(sellerProfile.defaultGreeting ?: "") }
    var shareAppLink by remember { mutableStateOf("") }

    LaunchedEffect(sellerProfile) {
        storeName = sellerProfile.storeName
        currencySymbol = sellerProfile.currencySymbol
        greeting = sellerProfile.defaultGreeting ?: ""
    }

    val orders = (ordersState as? UiState.Success)?.data.orEmpty()
    val incomingOrders = (incomingMarketplaceOrdersState as? UiState.Success)?.data.orEmpty()
    val placedOrders = (myPlacedOrdersState as? UiState.Success)?.data.orEmpty()

    val whatsappOrders = orders.filter { it.channelEnum == OrderChannel.WHATSAPP }
    val instagramOrders = orders.filter { it.channelEnum == OrderChannel.INSTAGRAM }
    val totalRevenueCents = orders.filter { it.stageEnum != OrderStage.CANCELLED }.sumOf { it.totalCents } +
        incomingOrders.filter { it.stageEnum != OrderStage.CANCELLED }.sumOf { it.totalCents }
    val paidRevenueCents = orders.filter { it.paymentStatusEnum == PaymentStatus.PAID }.sumOf { it.totalCents } +
        incomingOrders.filter { it.paymentStatusEnum == PaymentStatus.PAID }.sumOf { it.totalCents }
    val deliveredCount = orders.count { it.stageEnum == OrderStage.DELIVERED } +
        incomingOrders.count { it.stageEnum == OrderStage.DELIVERED }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "ANALYTICS & KABARAK STORE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Store & Channel Overview",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }

            // Kabarak Marketplace Summary Card
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
                            Text(
                                text = "Kabarak Services Activity",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            FilledTonalButton(
                                onClick = onOpenNotifications,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (unreadNotificationsCount > 0) "$unreadNotificationsCount Alerts" else "Alerts")
                            }
                        }
                        FilledTonalButton(
                            onClick = onOpenSellerDashboard,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("overview_seller_dashboard_button")
                        ) {
                            Icon(Icons.Rounded.Dashboard, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Seller Dashboard")
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SummaryBox(
                                label = "Incoming Jobs",
                                value = incomingOrders.size.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            SummaryBox(
                                label = "My Requests",
                                value = placedOrders.size.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            SummaryBox(
                                label = "Total Paid",
                                value = formatMoney(paidRevenueCents, sellerProfile.currencySymbol),
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                    }
                }
            }

            // Channel Split Chart Card
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            text = "WhatsApp vs Instagram DM Split",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ChannelDonutChart(
                                whatsappCount = whatsappOrders.size,
                                instagramCount = instagramOrders.size,
                                modifier = Modifier.size(104.dp)
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                ChannelStatRow(
                                    label = "WhatsApp",
                                    count = whatsappOrders.size,
                                    revenue = formatMoney(
                                        whatsappOrders.filter { it.stageEnum != OrderStage.CANCELLED }.sumOf { it.totalCents },
                                        sellerProfile.currencySymbol
                                    ),
                                    color = WhatsAppGreen
                                )
                                ChannelStatRow(
                                    label = "Instagram",
                                    count = instagramOrders.size,
                                    revenue = formatMoney(
                                        instagramOrders.filter { it.stageEnum != OrderStage.CANCELLED }.sumOf { it.totalCents },
                                        sellerProfile.currencySymbol
                                    ),
                                    color = InstagramMagenta
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SummaryBox(
                                label = "Gross Booked",
                                value = formatMoney(totalRevenueCents, sellerProfile.currencySymbol),
                                modifier = Modifier.weight(1f)
                            )
                            SummaryBox(
                                label = "Paid Revenue",
                                value = formatMoney(paidRevenueCents, sellerProfile.currencySymbol),
                                modifier = Modifier.weight(1f)
                            )
                            SummaryBox(
                                label = "Delivered",
                                value = deliveredCount.toString(),
                                modifier = Modifier.weight(0.75f)
                            )
                        }
                    }
                }
            }

            // Stage Funnel Breakdown
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
                        Text(
                            text = "Combined Delivery Stage Funnel",
                            style = MaterialTheme.typography.titleMedium
                        )
                        val maxCount = OrderStage.activePipelineStages
                            .maxOfOrNull { st ->
                                orders.count { it.stageEnum == st } + incomingOrders.count { it.stageEnum == st }
                            }
                            ?.coerceAtLeast(1) ?: 1

                        OrderStage.activePipelineStages.forEach { st ->
                            val count = orders.count { it.stageEnum == st } + incomingOrders.count { it.stageEnum == st }
                            val ratio = count.toFloat() / maxCount.toFloat()
                            val barColor = stageColor(st)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(st.label, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = count.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                ) {
                                    drawRoundRect(
                                        color = barColor.copy(alpha = 0.16f),
                                        size = size,
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                                    )
                                    if (count > 0) {
                                        drawRoundRect(
                                            color = barColor,
                                            size = Size(size.width * ratio.coerceIn(0.06f, 1f), size.height),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Store Profile Settings
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Storefront,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Store & DM Template Settings",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        OutlinedTextField(
                            value = storeName,
                            onValueChange = { if (it.length <= 60) storeName = it },
                            label = { Text("Store / Provider Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Currency Symbol", style = MaterialTheme.typography.labelMedium)
                        val currencies = listOf("KSh ", "$", "€", "£", "₹", "₦")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            currencies.forEach { sym ->
                                FilterChip(
                                    selected = currencySymbol == sym,
                                    onClick = { currencySymbol = sym },
                                    label = { Text(sym.trim()) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = greeting,
                            onValueChange = { if (it.length <= 200) greeting = it },
                            label = { Text("Custom DM Greeting (Optional)") },
                            placeholder = { Text("e.g. Hey! Quick update from Kabarak Express:") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (storeName.isNotBlank()) {
                                    onSaveProfile(
                                        storeName.trim(),
                                        currencySymbol.ifEmpty { "KSh " },
                                        greeting.trim().takeIf { it.isNotEmpty() }
                                    )
                                }
                            },
                            enabled = !isMutating && storeName.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_store_profile_button")
                        ) {
                            Text(if (isMutating) "Saving..." else "Save Store Preferences")
                        }
                    }
                }
            }

            // Share kabu-ship with Friends
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Share kabu-ship with Friends",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = "Paste your GitHub Release, Firebase App Distribution, or Google Drive APK link to invite Kabarak students and local customers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = shareAppLink,
                            onValueChange = { if (it.length <= 240) shareAppLink = it },
                            label = { Text("APK Download Link (Optional)") },
                            placeholder = { Text("https://github.com/.../releases/latest/download/kabu-ship.apk") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("share_app_link_input")
                        )
                        FilledTonalButton(
                            onClick = {
                                val linkLine = shareAppLink.trim().takeIf { it.isNotEmpty() }
                                    ?.let { "\nDownload APK: $it" }
                                    ?: ""
                                val inviteMessage = "Join kabu-ship — order food, 20L clean water, gas, laundry & campus services around Kabarak University with live order updates!$linkLine"
                                OutreachActions.shareText(
                                    context = context,
                                    message = inviteMessage,
                                    chooserTitle = "Share kabu-ship"
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("share_app_invite_button")
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share App Invite")
                        }
                    }
                }
            }

            // Account & Sign Out
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Column {
                                Text("Signed in with Google", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = userEmail ?: "Authenticated Account",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = onSignOut,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sign out")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelDonutChart(
    whatsappCount: Int,
    instagramCount: Int,
    modifier: Modifier = Modifier
) {
    val total = whatsappCount + instagramCount
    val trackColor = MaterialTheme.colorScheme.outlineVariant

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            if (total == 0) {
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )
            } else {
                val waSweep = 360f * (whatsappCount.toFloat() / total.toFloat())
                val igSweep = 360f - waSweep
                if (waSweep > 0f) {
                    drawArc(
                        color = WhatsAppGreen,
                        startAngle = -90f,
                        sweepAngle = waSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                }
                if (igSweep > 0f) {
                    drawArc(
                        color = InstagramMagenta,
                        startAngle = -90f + waSweep,
                        sweepAngle = igSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = total.toString(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                )
            )
            Text("orders", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ChannelStatRow(
    label: String,
    count: Int,
    revenue: String,
    color: androidx.compose.ui.graphics.Color
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
            Canvas(modifier = Modifier.size(10.dp)) {
                drawCircle(color = color)
            }
            Text("$label ($count)", style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = revenue,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun SummaryBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(10.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
