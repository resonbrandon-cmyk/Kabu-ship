package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.example.model.KabarakZone
import com.example.model.SellerProfile
import com.example.model.ServiceCategory
import com.example.model.ServiceListing
import com.example.ui.screens.OrderEntryScreen
import com.example.ui.theme.SocialShipTheme
import com.example.ui.viewmodel.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OrderEntryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `search bar filters available services by name or category`() {
        val foodService = ServiceListing(
            id = "svc_food_1",
            title = "Beef Pilau Plate",
            providerName = "Main Gate Kitchen",
            category = ServiceCategory.FOOD.name,
            zone = KabarakZone.MAIN_GATE.name,
            priceCents = 18000L
        )
        val waterService = ServiceListing(
            id = "svc_water_1",
            title = "20L Clean Drinking Water",
            providerName = "Kabarak Aqua",
            category = ServiceCategory.WATER.name,
            zone = KabarakZone.HOSTELS.name,
            priceCents = 5000L
        )
        val laundryService = ServiceListing(
            id = "svc_laundry_1",
            title = "Express Wash & Fold",
            providerName = "Rafiki Fresh",
            category = ServiceCategory.LAUNDRY.name,
            zone = KabarakZone.RAFIKI.name,
            priceCents = 25000L
        )

        composeTestRule.setContent {
            var selectedCategory by remember { mutableStateOf(ServiceCategory.FOOD) }
            SocialShipTheme {
                OrderEntryScreen(
                    selectedCategory = selectedCategory,
                    availableServicesState = UiState.Success(
                        listOf(foodService, waterService, laundryService)
                    ),
                    cartItems = emptyList(),
                    sellerProfile = SellerProfile(storeName = "Kabarak Express"),
                    defaultBuyerName = "Student",
                    isMutating = false,
                    onBack = {},
                    onSelectCategory = { selectedCategory = it },
                    onAddServiceToCart = { _, _ -> },
                    onAddCustomItemToCart = { _, _, _, _, _, _ -> },
                    onUpdateCartQuantity = { _, _ -> },
                    onRemoveCartItem = {},
                    onClearCart = {},
                    onSubmitOrder = { _, _, _, _, _, _, _, _ -> }
                )
            }
        }

        composeTestRule.waitForIdle()

        // Initially defaults to FOOD category
        composeTestRule.onNodeWithTag("order_entry_list")
            .performScrollToNode(hasTestTag("order_entry_service_svc_food_1"))
        composeTestRule.onNodeWithTag("order_entry_service_svc_food_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("order_entry_service_svc_water_1").assertDoesNotExist()

        // Search by service name across categories ("Water")
        composeTestRule.onNodeWithTag("order_entry_list")
            .performScrollToNode(hasTestTag("order_entry_search_input"))
        composeTestRule.onNodeWithTag("order_entry_search_input").performTextInput("Water")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("order_entry_service_svc_water_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("order_entry_service_svc_food_1").assertDoesNotExist()

        // Clear search and search by category label ("Laundry")
        composeTestRule.onNodeWithTag("order_entry_search_clear").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("order_entry_search_input").performTextInput("Laundry")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("order_entry_service_svc_laundry_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("order_entry_service_svc_water_1").assertDoesNotExist()

        // Clear search and tap "All" tile to show all categories
        composeTestRule.onNodeWithTag("order_entry_search_clear").performClick()
        composeTestRule.onNodeWithTag("order_entry_category_all").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("order_entry_service_svc_food_1")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("order_entry_service_svc_water_1")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("order_entry_service_svc_laundry_1")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
