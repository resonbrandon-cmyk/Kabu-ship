package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.model.KabarakZone
import com.example.model.MarketplaceOrder
import com.example.model.OrderChannel
import com.example.model.OrderStage
import com.example.model.SellerProfile
import com.example.model.ServiceCategory
import com.example.model.SocialOrder
import com.example.ui.screens.SellerDashboardScreen
import com.example.ui.theme.SocialShipTheme
import com.example.ui.viewmodel.UiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SellerDashboardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `seller dashboard lists incoming orders and supports confirm and dispatch actions`() {
        val pendingMarketplaceOrder = MarketplaceOrder(
            id = "mkt_pending_1",
            orderCode = "KB-101",
            serviceTitle = "20L Clean Water Refill",
            category = ServiceCategory.WATER.name,
            buyerName = "Faith Chebet",
            buyerHandleOrPhone = "+254712000111",
            deliveryZone = KabarakZone.HOSTELS.name,
            deliveryDetails = "Hostel B Room 4",
            quantity = 2L,
            totalCents = 10000L,
            currencySymbol = "KSh ",
            stage = OrderStage.NEW_DM.name
        )

        val confirmedSocialOrder = SocialOrder(
            id = "dm_confirmed_1",
            orderCode = "SS-202",
            customerName = "Kevin Omondi",
            channel = OrderChannel.WHATSAPP.name,
            handleOrPhone = "+254799111222",
            shippingAddress = "Main Gate Stage",
            itemsSummary = "2x Beef Pilau",
            totalCents = 36000L,
            currencySymbol = "KSh ",
            stage = OrderStage.PAID.name
        )

        var confirmedMarketplaceOrderId: String? = null
        var dispatchedSocialOrderId: String? = null

        composeTestRule.setContent {
            SocialShipTheme {
                SellerDashboardScreen(
                    incomingMarketplaceOrdersState = UiState.Success(listOf(pendingMarketplaceOrder)),
                    directOrdersState = UiState.Success(listOf(confirmedSocialOrder)),
                    sellerProfile = SellerProfile(storeName = "Kabarak Express"),
                    unreadNotificationsCount = 2,
                    isMutating = false,
                    onBack = {},
                    onOpenNotifications = {},
                    onConfirmMarketplaceOrder = { confirmedMarketplaceOrderId = it.id },
                    onDispatchMarketplaceOrder = {},
                    onOpenMarketplaceOrder = {},
                    onConfirmDirectOrder = {},
                    onDispatchDirectOrder = { dispatchedSocialOrderId = it.id },
                    onOpenDirectOrder = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("seller_dashboard_list")
            .performScrollToNode(hasTestTag("seller_order_card_mkt_pending_1"))
        composeTestRule.onNodeWithTag("seller_order_card_mkt_pending_1").assertIsDisplayed()

        // Pending order can be confirmed
        composeTestRule.onNodeWithTag("seller_confirm_button_mkt_pending_1")
            .assertIsEnabled()
            .performClick()
        assertEquals("mkt_pending_1", confirmedMarketplaceOrderId)

        // Scroll to second order card (Confirmed direct order)
        composeTestRule.onNodeWithTag("seller_dashboard_list")
            .performScrollToNode(hasTestTag("seller_order_card_dm_confirmed_1"))
        composeTestRule.onNodeWithTag("seller_order_card_dm_confirmed_1").assertIsDisplayed()

        // Confirmed order has Confirm disabled ("Confirmed") and Dispatch enabled
        composeTestRule.onNodeWithTag("seller_confirm_button_dm_confirmed_1")
            .assertIsNotEnabled()
        composeTestRule.onNodeWithTag("seller_dispatch_button_dm_confirmed_1")
            .assertIsEnabled()
            .performClick()
        assertEquals("dm_confirmed_1", dispatchedSocialOrderId)

        // Scroll back up to filter strip and filter to Pending only
        composeTestRule.onNodeWithTag("seller_dashboard_list")
            .performScrollToNode(hasTestTag("seller_filter_pending"))
        composeTestRule.onNodeWithTag("seller_filter_pending").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("2x 20L Clean Water Refill").assertIsDisplayed()
        composeTestRule.onNodeWithText("2x Beef Pilau").assertDoesNotExist()
    }
}
