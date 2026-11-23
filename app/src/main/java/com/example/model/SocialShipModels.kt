package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

enum class OrderChannel(val label: String) {
    WHATSAPP("WhatsApp"),
    INSTAGRAM("Instagram");

    companion object {
        fun fromRaw(raw: String?): OrderChannel =
            entries.firstOrNull { it.name == raw } ?: WHATSAPP
    }
}

enum class OrderStage(val label: String, val stepIndex: Int, val isTerminal: Boolean = false) {
    NEW_DM("Placed / New", 0),
    PAID("Accepted / Paid", 1),
    PACKING("Preparing", 2),
    SHIPPED("Dispatched", 3),
    OUT_FOR_DELIVERY("Out for Delivery", 4),
    DELIVERED("Delivered", 5, isTerminal = true),
    CANCELLED("Cancelled", -1, isTerminal = true);

    fun nextActiveStage(): OrderStage? = when (this) {
        NEW_DM -> PAID
        PAID -> PACKING
        PACKING -> SHIPPED
        SHIPPED -> OUT_FOR_DELIVERY
        OUT_FOR_DELIVERY -> DELIVERED
        DELIVERED, CANCELLED -> null
    }

    companion object {
        fun fromRaw(raw: String?): OrderStage =
            entries.firstOrNull { it.name == raw } ?: NEW_DM

        val activePipelineStages: List<OrderStage> =
            listOf(NEW_DM, PAID, PACKING, SHIPPED, OUT_FOR_DELIVERY, DELIVERED)
    }
}

enum class PaymentMethod(val label: String) {
    MPESA("M-Pesa"),
    COD("Cash on Delivery"),
    PREPAID("Prepaid"),
    BANK_TRANSFER("Bank Transfer"),
    LINK("Payment Link");

    companion object {
        fun fromRaw(raw: String?): PaymentMethod =
            entries.firstOrNull { it.name == raw } ?: MPESA
    }
}

enum class PaymentStatus(val label: String) {
    PENDING("Unpaid"),
    PARTIAL("Deposit Paid"),
    PAID("Paid"),
    COD_DUE("COD Due"),
    REFUNDED("Refunded");

    companion object {
        fun fromRaw(raw: String?): PaymentStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

enum class ServiceCategory(val label: String) {
    FOOD("Food & Meals"),
    WATER("Water Delivery"),
    GAS("Cooking Gas"),
    LAUNDRY("Laundry"),
    PRINTING("Printing & Tech"),
    GROCERIES("Groceries"),
    ERRANDS("Errands & Boda"),
    OTHER("Other Services");

    companion object {
        fun fromRaw(raw: String?): ServiceCategory =
            entries.firstOrNull { it.name == raw } ?: OTHER
    }
}

enum class KabarakZone(val label: String) {
    MAIN_GATE("Main Gate"),
    HOSTELS("Student Hostels"),
    RAFIKI("Rafiki Centre"),
    OLIVE("Olive Area"),
    KIAMUNYI("Kiamunyi"),
    STAFF_QUARTERS("Staff Quarters"),
    COMMUNITY("Kabarak Community");

    companion object {
        fun fromRaw(raw: String?): KabarakZone =
            entries.firstOrNull { it.name == raw } ?: MAIN_GATE
    }
}

enum class NotificationType {
    NEW_ORDER,
    STAGE_UPDATE,
    NEW_MESSAGE;

    companion object {
        fun fromRaw(raw: String?): NotificationType =
            entries.firstOrNull { it.name == raw } ?: NEW_ORDER
    }
}

@Keep
@IgnoreExtraProperties
data class SellerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val storeName: String = "Kabarak Express Shop",
    val currencySymbol: String = "KSh ",
    val defaultGreeting: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Customer(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val channel: String = OrderChannel.WHATSAPP.name,
    val handleOrPhone: String = "",
    val email: String? = null,
    val shippingAddress: String? = null,
    val notes: String? = null,
    val orderCount: Long = 0L,
    val totalSpentCents: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val channelEnum: OrderChannel get() = OrderChannel.fromRaw(channel)
}

@Keep
@IgnoreExtraProperties
data class SocialOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderCode: String = "",
    val customerId: String? = null,
    val customerName: String = "",
    val channel: String = OrderChannel.WHATSAPP.name,
    val handleOrPhone: String = "",
    val shippingAddress: String? = null,
    val itemsSummary: String = "",
    val itemCount: Long = 1L,
    val totalCents: Long = 0L,
    val currencySymbol: String = "KSh ",
    val paymentMethod: String = PaymentMethod.MPESA.name,
    val paymentStatus: String = PaymentStatus.PENDING.name,
    val stage: String = OrderStage.NEW_DM.name,
    val courierName: String? = null,
    val trackingNumber: String? = null,
    val notes: String? = null,
    val dmMessageSnapshot: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val channelEnum: OrderChannel get() = OrderChannel.fromRaw(channel)
    val stageEnum: OrderStage get() = OrderStage.fromRaw(stage)
    val paymentMethodEnum: PaymentMethod get() = PaymentMethod.fromRaw(paymentMethod)
    val paymentStatusEnum: PaymentStatus get() = PaymentStatus.fromRaw(paymentStatus)
}

@Keep
@IgnoreExtraProperties
data class OrderEvent(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderId: String = "",
    val stage: String = OrderStage.NEW_DM.name,
    val title: String = "",
    val note: String? = null,
    val createdAt: Timestamp? = null
) {
    val stageEnum: OrderStage get() = OrderStage.fromRaw(stage)
}

@Keep
@IgnoreExtraProperties
data class ServiceListing(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val providerName: String = "",
    val title: String = "",
    val category: String = ServiceCategory.FOOD.name,
    val zone: String = KabarakZone.MAIN_GATE.name,
    val description: String = "",
    val priceCents: Long = 0L,
    val currencySymbol: String = "KSh ",
    val unitLabel: String = "per order",
    val etaLabel: String = "15-25 mins",
    val contactChannel: String = OrderChannel.WHATSAPP.name,
    val contactHandle: String = "",
    val available: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val categoryEnum: ServiceCategory get() = ServiceCategory.fromRaw(category)
    val zoneEnum: KabarakZone get() = KabarakZone.fromRaw(zone)
    val channelEnum: OrderChannel get() = OrderChannel.fromRaw(contactChannel)
}

@Keep
@IgnoreExtraProperties
data class MarketplaceOrder(
    @DocumentId val id: String = "",
    val orderCode: String = "",
    val serviceId: String = "",
    val serviceTitle: String = "",
    val category: String = ServiceCategory.FOOD.name,
    val buyerUid: String = "",
    val buyerName: String = "",
    val buyerHandleOrPhone: String = "",
    val sellerUid: String = "",
    val sellerName: String = "",
    val sellerHandleOrPhone: String = "",
    val deliveryZone: String = KabarakZone.HOSTELS.name,
    val deliveryDetails: String = "",
    val quantity: Long = 1L,
    val unitPriceCents: Long = 0L,
    val totalCents: Long = 0L,
    val currencySymbol: String = "KSh ",
    val paymentMethod: String = PaymentMethod.MPESA.name,
    val paymentStatus: String = PaymentStatus.PENDING.name,
    val stage: String = OrderStage.NEW_DM.name,
    val courierName: String? = null,
    val trackingNumber: String? = null,
    val notes: String? = null,
    val lastMessagePreview: String? = null,
    val unreadForSeller: Boolean = false,
    val unreadForBuyer: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val categoryEnum: ServiceCategory get() = ServiceCategory.fromRaw(category)
    val deliveryZoneEnum: KabarakZone get() = KabarakZone.fromRaw(deliveryZone)
    val paymentMethodEnum: PaymentMethod get() = PaymentMethod.fromRaw(paymentMethod)
    val paymentStatusEnum: PaymentStatus get() = PaymentStatus.fromRaw(paymentStatus)
    val stageEnum: OrderStage get() = OrderStage.fromRaw(stage)
}

@Keep
@IgnoreExtraProperties
data class MarketplaceOrderEvent(
    @DocumentId val id: String = "",
    val orderId: String = "",
    val buyerUid: String = "",
    val sellerUid: String = "",
    val actorUid: String = "",
    val stage: String = OrderStage.NEW_DM.name,
    val title: String = "",
    val note: String? = null,
    val createdAt: Timestamp? = null
) {
    val stageEnum: OrderStage get() = OrderStage.fromRaw(stage)
}

@Keep
@IgnoreExtraProperties
data class OrderMessage(
    @DocumentId val id: String = "",
    val orderId: String = "",
    val buyerUid: String = "",
    val sellerUid: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val senderRole: String = "BUYER",
    val text: String = "",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val recipientUid: String = "",
    val senderUid: String = "",
    val orderId: String = "",
    val orderCode: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = NotificationType.NEW_ORDER.name,
    val read: Boolean = false,
    val createdAt: Timestamp? = null
) {
    val typeEnum: NotificationType get() = NotificationType.fromRaw(type)
}

data class CartEntryItem(
    val id: String,
    val title: String,
    val category: ServiceCategory,
    val unitPriceCents: Long,
    val currencySymbol: String,
    val unitLabel: String,
    val quantity: Long = 1L,
    val serviceListing: ServiceListing? = null
) {
    val lineTotalCents: Long get() = unitPriceCents * quantity
}

fun formatMoney(cents: Long, currencySymbol: String): String {
    val major = cents / 100.0
    val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "$currencySymbol${numberFormat.format(major)}"
}

fun formatTimestampShort(timestamp: Timestamp?): String {
    val date = (timestamp ?: Timestamp.now()).toDate()
    return SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(date)
}

fun buildDmStatusUpdate(
    order: SocialOrder,
    storeName: String,
    greeting: String? = null
): String {
    val firstName = order.customerName.trim().substringBefore(" ").ifBlank { "there" }
    val hello = greeting?.takeIf { it.isNotBlank() } ?: "Hi $firstName!"
    val statusLine = when (order.stageEnum) {
        OrderStage.NEW_DM -> "We've logged your order ${order.orderCode} (${order.itemsSummary}) for ${formatMoney(order.totalCents, order.currencySymbol)}."
        OrderStage.PAID -> "Payment confirmed for order ${order.orderCode}! We're getting your items ready."
        OrderStage.PACKING -> "Your order ${order.orderCode} (${order.itemsSummary}) is being prepared right now."
        OrderStage.SHIPPED -> {
            val courierPart = order.courierName?.takeIf { it.isNotBlank() }?.let { " via $it" } ?: ""
            val trackPart = order.trackingNumber?.takeIf { it.isNotBlank() }?.let { " Tracking: $it." } ?: ""
            "Great news! Your order ${order.orderCode} is on the way$courierPart.$trackPart"
        }
        OrderStage.OUT_FOR_DELIVERY -> {
            val courierPart = order.courierName?.takeIf { it.isNotBlank() }?.let { " with $it" } ?: ""
            "Your order ${order.orderCode} is out for delivery right now$courierPart! Please keep your phone handy."
        }
        OrderStage.DELIVERED -> "Your order ${order.orderCode} has been delivered! Thank you for ordering with $storeName."
        OrderStage.CANCELLED -> "Your order ${order.orderCode} has been cancelled. Message us if you need any help."
    }
    return "$hello $statusLine — $storeName"
}

fun buildMarketplaceDmMessage(
    order: MarketplaceOrder,
    isSeller: Boolean
): String {
    return if (isSeller) {
        val firstName = order.buyerName.trim().substringBefore(" ").ifBlank { "there" }
        val riderPart = order.courierName?.takeIf { it.isNotBlank() }?.let { " Rider: $it." } ?: ""
        "Hi $firstName! Update on your Kabarak order ${order.orderCode} (${order.quantity}x ${order.serviceTitle}): status is now ${order.stageEnum.label}.$riderPart — ${order.sellerName}"
    } else {
        "Hi ${order.sellerName}! Following up on my order ${order.orderCode} (${order.quantity}x ${order.serviceTitle}) for ${order.deliveryZoneEnum.label} (${order.deliveryDetails})."
    }
}

fun generateOrderCode(prefix: String = "KB"): String {
    val suffix = (System.currentTimeMillis() % 9000L + 1000L).toInt()
    return "$prefix-$suffix"
}
