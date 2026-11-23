package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocalLaundryService
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MarkChatUnread
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.KabarakZone
import com.example.model.MarketplaceOrder
import com.example.model.OrderChannel
import com.example.model.OrderStage
import com.example.model.PaymentMethod
import com.example.model.PaymentStatus
import com.example.model.SellerProfile
import com.example.model.ServiceCategory
import com.example.model.SocialOrder
import com.example.model.buildDmStatusUpdate
import com.example.model.buildMarketplaceDmMessage
import com.example.model.formatMoney
import com.example.model.formatTimestampShort
import com.example.ui.theme.InstagramDarkBg
import com.example.ui.theme.InstagramMagenta
import com.example.ui.theme.InstagramSoftBg
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StageCancelledColor
import com.example.ui.theme.StageDeliveredColor
import com.example.ui.theme.StageNewDmColor
import com.example.ui.theme.StageOutForDeliveryColor
import com.example.ui.theme.StagePackingColor
import com.example.ui.theme.StagePaidColor
import com.example.ui.theme.StageShippedColor
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WhatsAppDarkBg
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppSoftBg

fun stageColor(stage: OrderStage): Color = when (stage) {
    OrderStage.NEW_DM -> StageNewDmColor
    OrderStage.PAID -> StagePaidColor
    OrderStage.PACKING -> StagePackingColor
    OrderStage.SHIPPED -> StageShippedColor
    OrderStage.OUT_FOR_DELIVERY -> StageOutForDeliveryColor
    OrderStage.DELIVERED -> StageDeliveredColor
    OrderStage.CANCELLED -> StageCancelledColor
}

fun stageIcon(stage: OrderStage): ImageVector = when (stage) {
    OrderStage.NEW_DM -> Icons.Rounded.MarkChatUnread
    OrderStage.PAID -> Icons.Rounded.Payments
    OrderStage.PACKING -> Icons.Rounded.Inventory2
    OrderStage.SHIPPED -> Icons.Rounded.LocalShipping
    OrderStage.OUT_FOR_DELIVERY -> Icons.Rounded.TwoWheeler
    OrderStage.DELIVERED -> Icons.Rounded.CheckCircle
    OrderStage.CANCELLED -> Icons.Rounded.Cancel
}

@Composable
fun ChannelBadge(
    channel: OrderChannel,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = when (channel) {
        OrderChannel.WHATSAPP -> if (isDark) WhatsAppDarkBg else WhatsAppSoftBg
        OrderChannel.INSTAGRAM -> if (isDark) InstagramDarkBg else InstagramSoftBg
    }
    val fgColor = when (channel) {
        OrderChannel.WHATSAPP -> if (isDark) Color(0xFF6EE7B7) else WhatsAppGreen
        OrderChannel.INSTAGRAM -> if (isDark) Color(0xFFF9A8D4) else InstagramMagenta
    }
    val icon = when (channel) {
        OrderChannel.WHATSAPP -> Icons.AutoMirrored.Rounded.Chat
        OrderChannel.INSTAGRAM -> Icons.Rounded.AlternateEmail
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = channel.label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = fgColor
            )
        }
    }
}

@Composable
fun StagePill(
    stage: OrderStage,
    modifier: Modifier = Modifier
) {
    val color = stageColor(stage)
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = stageIcon(stage),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = stage.label,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
        }
    }
}

@Composable
fun PaymentBadge(
    status: PaymentStatus,
    method: PaymentMethod,
    modifier: Modifier = Modifier
) {
    val color = when (status) {
        PaymentStatus.PAID -> SuccessEmerald
        PaymentStatus.PARTIAL, PaymentStatus.COD_DUE -> WarningAmber
        PaymentStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
        PaymentStatus.REFUNDED -> StageCancelledColor
    }
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = "${status.label} · ${method.label}",
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

fun serviceCategoryColor(category: ServiceCategory): Color = when (category) {
    ServiceCategory.FOOD -> StagePackingColor
    ServiceCategory.WATER -> StagePaidColor
    ServiceCategory.GAS -> StageCancelledColor
    ServiceCategory.LAUNDRY -> StageShippedColor
    ServiceCategory.PRINTING -> StageNewDmColor
    ServiceCategory.GROCERIES -> StageDeliveredColor
    ServiceCategory.ERRANDS -> StageOutForDeliveryColor
    ServiceCategory.OTHER -> StageNewDmColor
}

fun serviceCategoryIcon(category: ServiceCategory): ImageVector = when (category) {
    ServiceCategory.FOOD -> Icons.Rounded.Restaurant
    ServiceCategory.WATER -> Icons.Rounded.WaterDrop
    ServiceCategory.GAS -> Icons.Rounded.LocalFireDepartment
    ServiceCategory.LAUNDRY -> Icons.Rounded.LocalLaundryService
    ServiceCategory.PRINTING -> Icons.Rounded.Print
    ServiceCategory.GROCERIES -> Icons.Rounded.ShoppingBasket
    ServiceCategory.ERRANDS -> Icons.Rounded.TwoWheeler
    ServiceCategory.OTHER -> Icons.Rounded.Storefront
}

@Composable
fun ServiceCategoryBadge(
    category: ServiceCategory,
    modifier: Modifier = Modifier
) {
    val color = serviceCategoryColor(category)
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = serviceCategoryIcon(category),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = category.label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
    }
}

@Composable
fun KabarakZoneChip(
    zone: KabarakZone,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = zone.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun OrderProgressBar(
    stage: OrderStage,
    modifier: Modifier = Modifier
) {
    val stages = OrderStage.activePipelineStages
    if (stage == OrderStage.CANCELLED) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(StageCancelledColor.copy(alpha = 0.35f))
        )
        return
    }

    val currentIndex = stage.stepIndex
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        stages.forEachIndexed { idx, stepStage ->
            val reached = idx <= currentIndex
            val barColor by animateColorAsState(
                targetValue = if (reached) stageColor(stage) else MaterialTheme.colorScheme.outlineVariant,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "stage_bar_color"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(barColor)
            )
        }
    }
}

@Composable
fun OrderCard(
    order: SocialOrder,
    onClick: () -> Unit,
    onAdvanceClick: () -> Unit,
    onDmUpdateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stage = order.stageEnum
    val nextStage = stage.nextActiveStage()

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag("order_card_${order.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChannelBadge(channel = order.channelEnum)
                    Text(
                        text = order.orderCode,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StagePill(stage = stage)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = order.handleOrPhone,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatMoney(order.totalCents, order.currencySymbol),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${order.itemCount} item${if (order.itemCount == 1L) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = order.itemsSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!order.trackingNumber.isNullOrBlank() || !order.courierName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalShipping,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = listOfNotNull(
                                order.courierName?.takeIf { it.isNotBlank() },
                                order.trackingNumber?.takeIf { it.isNotBlank() }
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OrderProgressBar(stage = stage)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    PaymentBadge(
                        status = order.paymentStatusEnum,
                        method = order.paymentMethodEnum
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatTimestampShort(order.updatedAt ?: order.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onDmUpdateClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("order_dm_button_${order.id}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send DM status update",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (nextStage != null) {
                        FilledTonalButton(
                            onClick = onAdvanceClick,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("order_advance_button_${order.id}")
                        ) {
                            Text(
                                text = nextStage.label,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = "Advance to ${nextStage.label}",
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarketplaceOrderCard(
    order: MarketplaceOrder,
    isSellerView: Boolean,
    onClick: () -> Unit,
    onQuickAdvance: (() -> Unit)?,
    onDmClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stage = order.stageEnum
    val nextStage = if (isSellerView) stage.nextActiveStage() else null
    val hasUnread = if (isSellerView) order.unreadForSeller else order.unreadForBuyer

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (hasUnread) 3.dp else 1.dp,
        border = BorderStroke(
            width = if (hasUnread) 1.5.dp else 1.dp,
            color = if (hasUnread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("marketplace_order_card_${order.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ServiceCategoryBadge(category = order.categoryEnum)
                    Text(
                        text = order.orderCode,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (hasUnread) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
                StagePill(stage = stage)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${order.quantity}x ${order.serviceTitle}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isSellerView) {
                            "Customer: ${order.buyerName} (${order.buyerHandleOrPhone})"
                        } else {
                            "Provider: ${order.sellerName}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatMoney(order.totalCents, order.currencySymbol),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    KabarakZoneChip(zone = order.deliveryZoneEnum)
                }
            }

            if (order.deliveryDetails.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Drop-off: ${order.deliveryDetails}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!order.lastMessagePreview.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = if (hasUnread) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Chat,
                            contentDescription = null,
                            tint = if (hasUnread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = order.lastMessagePreview,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OrderProgressBar(stage = stage)

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    PaymentBadge(
                        status = order.paymentStatusEnum,
                        method = order.paymentMethodEnum
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = formatTimestampShort(order.updatedAt ?: order.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onDmClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("marketplace_dm_button_${order.id}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send WhatsApp or DM update",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (isSellerView && nextStage != null && onQuickAdvance != null) {
                        FilledTonalButton(
                            onClick = onQuickAdvance,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("marketplace_advance_button_${order.id}")
                        ) {
                            Text(
                                text = nextStage.label,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = "Advance to ${nextStage.label}",
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = onClick,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Text("Chat & Track", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CourierDispatchDialog(
    order: SocialOrder,
    targetStage: OrderStage,
    onDismiss: () -> Unit,
    onConfirm: (courierName: String, trackingNumber: String, note: String) -> Unit
) {
    CourierDispatchDialog(
        orderCode = order.orderCode,
        recipientName = order.customerName,
        initialCourier = order.courierName,
        initialTracking = order.trackingNumber,
        targetStage = targetStage,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CourierDispatchDialog(
    orderCode: String,
    recipientName: String,
    initialCourier: String?,
    initialTracking: String?,
    targetStage: OrderStage,
    onDismiss: () -> Unit,
    onConfirm: (courierName: String, trackingNumber: String, note: String) -> Unit
) {
    var courier by remember { mutableStateOf(initialCourier ?: "") }
    var tracking by remember { mutableStateOf(initialTracking ?: "") }
    var note by remember { mutableStateOf("") }
    val quickCouriers = listOf("Kabarak Boda", "Campus Runner", "Store Rider", "Pickup Point", "DHL")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (targetStage == OrderStage.SHIPPED) "Dispatch $orderCode" else "Update Tracking",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Add rider / courier details for $recipientName.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickCouriers.forEach { preset ->
                        FilterChip(
                            selected = courier.equals(preset, ignoreCase = true),
                            onClick = { courier = preset },
                            label = { Text(preset, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
                OutlinedTextField(
                    value = courier,
                    onValueChange = { if (it.length <= 60) courier = it },
                    label = { Text("Courier / Boda Rider Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tracking,
                    onValueChange = { if (it.length <= 80) tracking = it },
                    label = { Text("Rider Phone / Tracking (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 250) note = it },
                    label = { Text("Dispatch Note (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(courier.trim(), tracking.trim(), note.trim()) },
                modifier = Modifier.testTag("confirm_dispatch_button")
            ) {
                Text("Mark ${targetStage.label}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun LoadingStateView(
    label: String = "Syncing orders...",
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp)
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ErrorStateView(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(20.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onRetry,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
fun EmptyArtView(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    showArtwork: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        if (showArtwork) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_orders),
                contentDescription = "Empty order box illustration",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(22.dp))
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(actionLabel)
            }
        }
    }
}

object OutreachActions {
    fun copyText(context: Context, label: String, text: String, onDone: () -> Unit) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
        onDone()
    }

    fun copyDmUpdate(
        context: Context,
        order: SocialOrder,
        profile: SellerProfile,
        onDone: () -> Unit
    ) {
        val message = buildDmStatusUpdate(order, profile.storeName, profile.defaultGreeting)
        copyText(context, "Order ${order.orderCode} update", message, onDone)
    }

    fun openWhatsAppOrShare(
        context: Context,
        order: SocialOrder,
        profile: SellerProfile
    ) {
        val message = buildDmStatusUpdate(order, profile.storeName, profile.defaultGreeting)
        val digits = order.handleOrPhone.filter { it.isDigit() }
        if (order.channelEnum == OrderChannel.WHATSAPP && digits.length >= 7) {
            val uri = Uri.parse("https://wa.me/$digits?text=${Uri.encode(message)}")
            val waIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(waIntent)
                return
            } catch (_: Exception) {
                // Fall back to system share sheet below
            }
        }
        shareText(context, message, "Send update to ${order.customerName}")
    }

    fun openWhatsAppOrShareMarketplace(
        context: Context,
        order: MarketplaceOrder,
        isSeller: Boolean
    ) {
        val message = buildMarketplaceDmMessage(order, isSeller)
        val rawContact = if (isSeller) order.buyerHandleOrPhone else order.sellerHandleOrPhone
        val digits = rawContact.filter { it.isDigit() }
        if (digits.length >= 7) {
            val normalized = if (digits.startsWith("0") && digits.length == 10) {
                "254${digits.drop(1)}"
            } else {
                digits
            }
            val uri = Uri.parse("https://wa.me/$normalized?text=${Uri.encode(message)}")
            val waIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(waIntent)
                return
            } catch (_: Exception) {
                // Fall back to share sheet
            }
        }
        val recipient = if (isSeller) order.buyerName else order.sellerName
        shareText(context, message, "Message $recipient")
    }

    fun shareText(context: Context, message: String, chooserTitle: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(chooser)
        } catch (_: Exception) {
            // No share target available
        }
    }
}
