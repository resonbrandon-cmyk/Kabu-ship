package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CartEntryItem
import com.example.model.KabarakZone
import com.example.model.OrderChannel
import com.example.model.PaymentMethod
import com.example.model.SellerProfile
import com.example.model.ServiceCategory
import com.example.model.ServiceListing
import com.example.model.formatMoney
import com.example.ui.components.ErrorStateView
import com.example.ui.components.KabarakZoneChip
import com.example.ui.components.LoadingStateView
import com.example.ui.components.ServiceCategoryBadge
import com.example.ui.components.serviceCategoryColor
import com.example.ui.components.serviceCategoryIcon
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.UiState
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OrderEntryScreen(
    selectedCategory: ServiceCategory,
    availableServicesState: UiState<List<ServiceListing>>,
    cartItems: List<CartEntryItem>,
    sellerProfile: SellerProfile,
    defaultBuyerName: String,
    isMutating: Boolean,
    onBack: () -> Unit,
    onSelectCategory: (ServiceCategory) -> Unit,
    onAddServiceToCart: (ServiceListing, Long) -> Unit,
    onAddCustomItemToCart: (
        title: String,
        category: ServiceCategory,
        unitPriceCents: Long,
        currencySymbol: String,
        unitLabel: String,
        quantity: Long
    ) -> Unit,
    onUpdateCartQuantity: (itemId: String, newQuantity: Long) -> Unit,
    onRemoveCartItem: (itemId: String) -> Unit,
    onClearCart: () -> Unit,
    onSubmitOrder: (
        buyerName: String,
        buyerPhoneOrHandle: String,
        channel: OrderChannel,
        deliveryZone: KabarakZone,
        deliveryDetails: String,
        paymentMethod: PaymentMethod,
        currencySymbol: String,
        notes: String?
    ) -> Unit
) {
    var serviceSearchQuery by remember { mutableStateOf("") }
    var showAllCategories by remember { mutableStateOf(false) }
    var categoryLockedForSearch by remember { mutableStateOf(false) }

    var customTitle by remember { mutableStateOf("") }
    var customPriceText by remember { mutableStateOf("") }
    var customUnitLabel by remember { mutableStateOf("item") }
    var customQuantity by remember { mutableLongStateOf(1L) }
    var customItemError by remember { mutableStateOf<String?>(null) }

    var buyerName by remember { mutableStateOf(defaultBuyerName) }
    var buyerContact by remember { mutableStateOf("") }
    var channel by remember { mutableStateOf(OrderChannel.WHATSAPP) }
    var deliveryZone by remember { mutableStateOf(KabarakZone.HOSTELS) }
    var deliveryDetails by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.MPESA) }
    var notes by remember { mutableStateOf("") }
    var checkoutError by remember { mutableStateOf<String?>(null) }

    val effectiveCategoryFilter: ServiceCategory? = when {
        showAllCategories -> null
        serviceSearchQuery.isNotBlank() && !categoryLockedForSearch -> null
        else -> selectedCategory
    }

    val totalCartQuantity = remember(cartItems) { cartItems.sumOf { it.quantity } }
    val totalCartCents = remember(cartItems) { cartItems.sumOf { it.lineTotalCents } }
    val hasCustomItems = remember(cartItems) { cartItems.any { it.serviceListing == null } }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Order Entry",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Select category, build cart & submit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("order_entry_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            BadgedBox(
                                badge = {
                                    if (totalCartQuantity > 0L) {
                                        Badge { Text(totalCartQuantity.toString()) }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ShoppingCart,
                                    contentDescription = "Cart items",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = formatMoney(totalCartCents, sellerProfile.currencySymbol),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
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
                .imePadding()
                .navigationBarsPadding()
        ) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp)
                    .align(Alignment.TopCenter)
                    .testTag("order_entry_list")
            ) {
                // Quick Search Bar (by service name, provider, or category)
                item {
                    OutlinedTextField(
                        value = serviceSearchQuery,
                        onValueChange = { next ->
                            serviceSearchQuery = next
                            if (next.isBlank()) {
                                categoryLockedForSearch = false
                            }
                        },
                        placeholder = { Text("Search services by name or category...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (serviceSearchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        serviceSearchQuery = ""
                                        categoryLockedForSearch = false
                                    },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("order_entry_search_clear")
                                ) {
                                    Icon(imageVector = Icons.Rounded.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("order_entry_search_input")
                    )
                }

                // Step 1: Select Service Category
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "1 · SELECT SERVICE CATEGORY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            item {
                                AllCategorySelectorTile(
                                    selected = effectiveCategoryFilter == null,
                                    onClick = {
                                        showAllCategories = true
                                        categoryLockedForSearch = false
                                    }
                                )
                            }
                            items(ServiceCategory.entries) { category ->
                                CategorySelectorTile(
                                    category = category,
                                    selected = effectiveCategoryFilter == category,
                                    onClick = {
                                        showAllCategories = false
                                        categoryLockedForSearch = true
                                        onSelectCategory(category)
                                    }
                                )
                            }
                        }
                    }
                }

                // Step 2: Live Services in Selected/Searched Categories + Custom Item Builder
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val trimmedQuery = serviceSearchQuery.trim()
                        val matchingServices = (availableServicesState as? UiState.Success)?.data?.filter { service ->
                            val matchesCategory = effectiveCategoryFilter == null || service.categoryEnum == effectiveCategoryFilter
                            val matchesSearch = trimmedQuery.isEmpty() ||
                                service.title.contains(trimmedQuery, ignoreCase = true) ||
                                service.providerName.contains(trimmedQuery, ignoreCase = true) ||
                                service.categoryEnum.label.contains(trimmedQuery, ignoreCase = true) ||
                                service.categoryEnum.name.contains(trimmedQuery, ignoreCase = true) ||
                                service.zoneEnum.label.contains(trimmedQuery, ignoreCase = true) ||
                                service.description.contains(trimmedQuery, ignoreCase = true)
                            matchesCategory && matchesSearch
                        }.orEmpty()

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = when {
                                    effectiveCategoryFilter == null && trimmedQuery.isNotEmpty() ->
                                        "2 · MATCHING SERVICES"
                                    effectiveCategoryFilter == null ->
                                        "2 · ALL AVAILABLE SERVICES"
                                    else ->
                                        "2 · ADD ${effectiveCategoryFilter.label.uppercase()} ITEMS"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (availableServicesState is UiState.Success) {
                                Text(
                                    text = "${matchingServices.size} available",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        when (availableServicesState) {
                            is UiState.Loading -> LoadingStateView("Loading ${effectiveCategoryFilter?.label ?: "available"} providers...")
                            is UiState.Error -> ErrorStateView(availableServicesState.message)
                            is UiState.Success -> {
                                if (matchingServices.isEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(14.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Storefront,
                                                    contentDescription = null,
                                                    tint = effectiveCategoryFilter?.let { serviceCategoryColor(it) }
                                                        ?: MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = if (trimmedQuery.isNotEmpty()) {
                                                        "No services match \"$trimmedQuery\"${effectiveCategoryFilter?.let { " in ${it.label}" } ?: ""}."
                                                    } else {
                                                        "No vendor catalog items in ${effectiveCategoryFilter?.label ?: "this view"} yet. Add custom items below to build your order."
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (trimmedQuery.isNotEmpty()) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    if (effectiveCategoryFilter != null) {
                                                        TextButton(
                                                            onClick = {
                                                                showAllCategories = true
                                                                categoryLockedForSearch = false
                                                            },
                                                            modifier = Modifier.minimumInteractiveComponentSize()
                                                        ) {
                                                            Text("Search All Categories")
                                                        }
                                                    }
                                                    TextButton(
                                                        onClick = {
                                                            serviceSearchQuery = ""
                                                            categoryLockedForSearch = false
                                                        },
                                                        modifier = Modifier.minimumInteractiveComponentSize()
                                                    ) {
                                                        Text("Clear Search")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        matchingServices.forEach { service ->
                                            val cartQty = cartItems.firstOrNull { it.id == "svc_${service.id}" }?.quantity ?: 0L
                                            CatalogServiceAddCard(
                                                service = service,
                                                quantityInCart = cartQty,
                                                showCategoryBadge = effectiveCategoryFilter == null,
                                                onAdd = { onAddServiceToCart(service, 1L) },
                                                onDecrement = {
                                                    onUpdateCartQuantity("svc_${service.id}", cartQty - 1L)
                                                },
                                                onIncrement = {
                                                    onUpdateCartQuantity("svc_${service.id}", cartQty + 1L)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Custom Item Entry Card (always available under selected category)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
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
                                    Text(
                                        text = "Add Custom ${selectedCategory.label} Item",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    ServiceCategoryBadge(selectedCategory)
                                }

                                OutlinedTextField(
                                    value = customTitle,
                                    onValueChange = { if (it.length <= 100) customTitle = it; customItemError = null },
                                    label = { Text("Item Name *") },
                                    placeholder = {
                                        Text(
                                            when (selectedCategory) {
                                                ServiceCategory.FOOD -> "e.g. Pilau & Beef Stew"
                                                ServiceCategory.WATER -> "e.g. 20L Refill Jerrican"
                                                ServiceCategory.GAS -> "e.g. 6kg K-Gas Refill"
                                                ServiceCategory.LAUNDRY -> "e.g. Full Basket Wash & Fold"
                                                ServiceCategory.PRINTING -> "e.g. Assignment Binding & Print"
                                                ServiceCategory.GROCERIES -> "e.g. Fresh Milk & Bread Bundle"
                                                ServiceCategory.ERRANDS -> "e.g. Main Gate Parcel Pickup"
                                                ServiceCategory.OTHER -> "e.g. Room Cleaning Service"
                                            }
                                        )
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("custom_item_name_input")
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = customPriceText,
                                        onValueChange = { if (it.length <= 12) customPriceText = it; customItemError = null },
                                        label = { Text("Unit Price (${sellerProfile.currencySymbol.trim()}) *") },
                                        placeholder = { Text("150") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("custom_item_price_input")
                                    )
                                    OutlinedTextField(
                                        value = customUnitLabel,
                                        onValueChange = { if (it.length <= 24) customUnitLabel = it },
                                        label = { Text("Unit") },
                                        singleLine = true,
                                        modifier = Modifier.weight(0.75f)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Qty:", style = MaterialTheme.typography.labelLarge)
                                        FilledTonalIconButton(
                                            onClick = { if (customQuantity > 1L) customQuantity -= 1L },
                                            enabled = customQuantity > 1L,
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Rounded.Remove, contentDescription = "Decrease custom quantity")
                                        }
                                        Text(
                                            text = customQuantity.toString(),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        FilledTonalIconButton(
                                            onClick = { if (customQuantity < 99L) customQuantity += 1L },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Rounded.Add, contentDescription = "Increase custom quantity")
                                        }
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            val parsedPrice = customPriceText.trim().toDoubleOrNull()
                                            when {
                                                customTitle.isBlank() -> customItemError = "Enter an item name."
                                                parsedPrice == null || parsedPrice < 0.0 -> customItemError = "Enter a valid unit price."
                                                else -> {
                                                    onAddCustomItemToCart(
                                                        customTitle.trim(),
                                                        selectedCategory,
                                                        (parsedPrice * 100.0).roundToLong(),
                                                        sellerProfile.currencySymbol,
                                                        customUnitLabel.trim().ifEmpty { "item" },
                                                        customQuantity
                                                    )
                                                    customTitle = ""
                                                    customPriceText = ""
                                                    customQuantity = 1L
                                                    customItemError = null
                                                    checkoutError = null
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("add_custom_item_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AddShoppingCart,
                                            contentDescription = null,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add to Cart")
                                    }
                                }

                                if (customItemError != null) {
                                    Text(
                                        text = customItemError!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                // Step 3: Shopping Cart Summary
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "3 · YOUR CART ($totalCartQuantity)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (cartItems.isNotEmpty()) {
                                TextButton(
                                    onClick = onClearCart,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("cart_clear_button")
                                ) {
                                    Text("Clear Cart")
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = if (cartItems.isNotEmpty()) 2.dp else 0.dp,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (cartItems.isNotEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (cartItems.isEmpty()) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ShoppingCart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = "Your cart is empty",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Tap Add on a service above or enter a custom item to start your order.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    cartItems.forEachIndexed { index, item ->
                                        CartItemRow(
                                            item = item,
                                            onDecrement = { onUpdateCartQuantity(item.id, item.quantity - 1L) },
                                            onIncrement = { onUpdateCartQuantity(item.id, item.quantity + 1L) },
                                            onRemove = { onRemoveCartItem(item.id) }
                                        )
                                        if (index < cartItems.lastIndex) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Order Total ($totalCartQuantity item${if (totalCartQuantity == 1L) "" else "s"})",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = formatMoney(totalCartCents, sellerProfile.currencySymbol),
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Step 4: Delivery Details & Submit to Firestore
                item {
                    AnimatedVisibility(
                        visible = cartItems.isNotEmpty(),
                        enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "4 · DELIVERY & PAYMENT DETAILS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                OutlinedTextField(
                                    value = buyerName,
                                    onValueChange = { if (it.length <= 80) buyerName = it; checkoutError = null },
                                    label = { Text("Customer / Your Name *") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("order_entry_name_input")
                                )

                                OutlinedTextField(
                                    value = buyerContact,
                                    onValueChange = { if (it.length <= 60) buyerContact = it; checkoutError = null },
                                    label = { Text("WhatsApp Phone or @handle *") },
                                    placeholder = { Text("+2547... or @username") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("order_entry_phone_input")
                                )

                                if (hasCustomItems) {
                                    Text("Order Channel", style = MaterialTheme.typography.labelLarge)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OrderChannel.entries.forEach { ch ->
                                            FilterChip(
                                                selected = channel == ch,
                                                onClick = { channel = ch },
                                                label = { Text(ch.label) }
                                            )
                                        }
                                    }
                                }

                                Text("Kabarak Delivery Zone", style = MaterialTheme.typography.labelLarge)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    KabarakZone.entries.forEach { z ->
                                        FilterChip(
                                            selected = deliveryZone == z,
                                            onClick = { deliveryZone = z },
                                            label = { Text(z.label) }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = deliveryDetails,
                                    onValueChange = { if (it.length <= 160) deliveryDetails = it; checkoutError = null },
                                    label = { Text("Hostel / Room / Estate / Gate Spot *") },
                                    placeholder = { Text("e.g. Men's Hostel B Room 14, or Main Gate") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("order_entry_location_input")
                                )

                                Text("Payment Method", style = MaterialTheme.typography.labelLarge)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    PaymentMethod.entries.forEach { pm ->
                                        FilterChip(
                                            selected = paymentMethod == pm,
                                            onClick = { paymentMethod = pm },
                                            label = { Text(pm.label) }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = notes,
                                    onValueChange = { if (it.length <= 300) notes = it },
                                    label = { Text("Delivery Instructions (Optional)") },
                                    maxLines = 3,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("order_entry_notes_input")
                                )

                                if (checkoutError != null) {
                                    Text(
                                        text = checkoutError!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                Button(
                                    onClick = {
                                        when {
                                            cartItems.isEmpty() -> checkoutError = "Your cart is empty."
                                            buyerName.isBlank() -> checkoutError = "Enter a customer name."
                                            buyerContact.isBlank() -> checkoutError = "Enter a phone number or @handle."
                                            deliveryDetails.isBlank() -> checkoutError = "Enter hostel, room, or drop-off details."
                                            else -> {
                                                checkoutError = null
                                                onSubmitOrder(
                                                    buyerName.trim(),
                                                    buyerContact.trim(),
                                                    channel,
                                                    deliveryZone,
                                                    deliveryDetails.trim(),
                                                    paymentMethod,
                                                    sellerProfile.currencySymbol,
                                                    notes.trim().takeIf { it.isNotEmpty() }
                                                )
                                            }
                                        }
                                    },
                                    enabled = !isMutating,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("submit_cart_order_button")
                                ) {
                                    if (isMutating) {
                                        CircularProgressIndicator(
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Submitting Order...")
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Submit Order · ${formatMoney(totalCartCents, sellerProfile.currencySymbol)}"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AllCategorySelectorTile(
    selected: Boolean,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
        tonalElevation = if (selected) 2.dp else 0.dp,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("order_entry_category_all")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Apps,
                contentDescription = null,
                tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "All",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) accent else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CategorySelectorTile(
    category: ServiceCategory,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accent = serviceCategoryColor(category)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
        tonalElevation = if (selected) 2.dp else 0.dp,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("order_entry_category_${category.name.lowercase()}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = serviceCategoryIcon(category),
                contentDescription = null,
                tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = category.label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) accent else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CatalogServiceAddCard(
    service: ServiceListing,
    quantityInCart: Long,
    showCategoryBadge: Boolean,
    onAdd: () -> Unit,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(
            width = if (quantityInCart > 0L) 1.5.dp else 1.dp,
            color = if (quantityInCart > 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_entry_service_${service.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(14.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (showCategoryBadge) {
                    ServiceCategoryBadge(service.categoryEnum)
                }
                Text(
                    text = service.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = service.providerName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    KabarakZoneChip(service.zoneEnum)
                }
                Text(
                    text = "${formatMoney(service.priceCents, service.currencySymbol)} / ${service.unitLabel} · ${service.etaLabel}",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (quantityInCart == 0L) {
                FilledTonalButton(
                    onClick = onAdd,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("add_listing_to_cart_${service.id}")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = onDecrement,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Decrease quantity")
                    }
                    Text(
                        text = quantityInCart.toString(),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    FilledTonalIconButton(
                        onClick = onIncrement,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Increase quantity")
                    }
                }
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartEntryItem,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cart_item_${item.id}")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ServiceCategoryBadge(item.category)
                if (item.serviceListing != null) {
                    Text(
                        text = item.serviceListing.providerName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${formatMoney(item.unitPriceCents, item.currencySymbol)} / ${item.unitLabel} → ${formatMoney(item.lineTotalCents, item.currencySymbol)}",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(
                onClick = onDecrement,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Rounded.Remove, contentDescription = "Decrease item quantity")
            }
            Text(
                text = item.quantity.toString(),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                )
            )
            IconButton(
                onClick = onIncrement,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Increase item quantity")
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Remove item from cart",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
