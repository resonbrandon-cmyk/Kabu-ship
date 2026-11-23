package com.example

import com.example.model.CartEntryItem
import com.example.model.KabarakZone
import com.example.model.MarketplaceOrder
import com.example.model.OrderChannel
import com.example.model.OrderStage
import com.example.model.PaymentMethod
import com.example.model.PaymentStatus
import com.example.model.ServiceCategory
import com.example.model.SocialOrder
import com.example.model.buildDmStatusUpdate
import com.example.model.buildMarketplaceDmMessage
import com.example.model.formatMoney
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialShipModelsTest {

    @Test
    fun `safe enum deserializers fall back gracefully on unknown values`() {
        assertEquals(OrderChannel.WHATSAPP, OrderChannel.fromRaw("UNKNOWN_CHANNEL"))
        assertEquals(OrderChannel.INSTAGRAM, OrderChannel.fromRaw("INSTAGRAM"))
        assertEquals(OrderStage.NEW_DM, OrderStage.fromRaw(null))
        assertEquals(PaymentMethod.MPESA, PaymentMethod.fromRaw("CRYPTO"))
        assertEquals(PaymentStatus.PENDING, PaymentStatus.fromRaw("INVALID"))
        assertEquals(ServiceCategory.OTHER, ServiceCategory.fromRaw("UNKNOWN_SVC"))
        assertEquals(ServiceCategory.WATER, ServiceCategory.fromRaw("WATER"))
        assertEquals(KabarakZone.MAIN_GATE, KabarakZone.fromRaw("UNKNOWN_ZONE"))
        assertEquals(KabarakZone.HOSTELS, KabarakZone.fromRaw("HOSTELS"))
    }

    @Test
    fun `order stage progression stops at terminal states`() {
        assertEquals(OrderStage.PAID, OrderStage.NEW_DM.nextActiveStage())
        assertEquals(OrderStage.PACKING, OrderStage.PAID.nextActiveStage())
        assertEquals(OrderStage.SHIPPED, OrderStage.PACKING.nextActiveStage())
        assertEquals(OrderStage.OUT_FOR_DELIVERY, OrderStage.SHIPPED.nextActiveStage())
        assertEquals(OrderStage.DELIVERED, OrderStage.OUT_FOR_DELIVERY.nextActiveStage())
        assertNull(OrderStage.DELIVERED.nextActiveStage())
        assertNull(OrderStage.CANCELLED.nextActiveStage())
    }

    @Test
    fun `formatMoney formats cents accurately`() {
        assertEquals("$64.00", formatMoney(6400L, "$"))
        assertEquals("€12.50", formatMoney(1250L, "€"))
        assertEquals("KSh 250.00", formatMoney(25000L, "KSh "))
    }

    @Test
    fun `buildDmStatusUpdate includes courier and tracking when shipped`() {
        val order = SocialOrder(
            id = "ord1",
            orderCode = "SS-1042",
            customerName = "Maya Lin",
            channel = OrderChannel.INSTAGRAM.name,
            handleOrPhone = "@maya_styles",
            itemsSummary = "2x Tote Bag",
            totalCents = 5800L,
            currencySymbol = "$",
            stage = OrderStage.SHIPPED.name,
            courierName = "DHL",
            trackingNumber = "DHL-9921"
        )
        val message = buildDmStatusUpdate(order, "Loom & Clay")
        assertTrue(message.contains("Maya"))
        assertTrue(message.contains("SS-1042"))
        assertTrue(message.contains("DHL"))
        assertTrue(message.contains("DHL-9921"))
        assertTrue(message.contains("Loom & Clay"))
    }

    @Test
    fun `buildMarketplaceDmMessage tailors copy for provider and customer`() {
        val mktOrder = MarketplaceOrder(
            id = "mkt1",
            orderCode = "KB-4410",
            serviceTitle = "20L Clean Drinking Water",
            category = ServiceCategory.WATER.name,
            buyerName = "Brian Kiprop",
            sellerName = "Kabarak Aqua",
            deliveryZone = KabarakZone.HOSTELS.name,
            deliveryDetails = "Hostel C Room 12",
            quantity = 2L,
            totalCents = 10000L,
            stage = OrderStage.OUT_FOR_DELIVERY.name,
            courierName = "Kabarak Boda"
        )
        val sellerMsg = buildMarketplaceDmMessage(mktOrder, isSeller = true)
        assertTrue(sellerMsg.contains("Brian"))
        assertTrue(sellerMsg.contains("KB-4410"))
        assertTrue(sellerMsg.contains("2x 20L Clean Drinking Water"))
        assertTrue(sellerMsg.contains("Kabarak Boda"))

        val buyerMsg = buildMarketplaceDmMessage(mktOrder, isSeller = false)
        assertTrue(buyerMsg.contains("Kabarak Aqua"))
        assertTrue(buyerMsg.contains("Student Hostels"))
        assertTrue(buyerMsg.contains("Hostel C Room 12"))
    }

    @Test
    fun `cart entry item computes line totals accurately`() {
        val cart = listOf(
            CartEntryItem(
                id = "c1",
                title = "Pilau & Beef",
                category = ServiceCategory.FOOD,
                unitPriceCents = 18000L,
                currencySymbol = "KSh ",
                unitLabel = "plate",
                quantity = 2L
            ),
            CartEntryItem(
                id = "c2",
                title = "20L Refill Water",
                category = ServiceCategory.WATER,
                unitPriceCents = 5000L,
                currencySymbol = "KSh ",
                unitLabel = "jerrican",
                quantity = 3L
            )
        )
        assertEquals(36000L, cart[0].lineTotalCents)
        assertEquals(15000L, cart[1].lineTotalCents)
        assertEquals(51000L, cart.sumOf { it.lineTotalCents })
        assertEquals(5L, cart.sumOf { it.quantity })
    }
}
