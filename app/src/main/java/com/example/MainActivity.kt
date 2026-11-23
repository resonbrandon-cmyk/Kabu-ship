package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.auth.attemptAutoSignIn
import com.example.auth.signOut
import com.example.data.SocialShipRepository
import com.example.model.OrderStage
import com.example.model.SocialOrder
import com.example.notifications.LocalNotificationHelper
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DispatchScreen
import com.example.ui.screens.MarketplaceOrderDetailScreen
import com.example.ui.screens.NotificationsSheet
import com.example.ui.screens.OrderDetailScreen
import com.example.ui.screens.OrderEntryScreen
import com.example.ui.screens.OrderFormSheet
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.PipelineScreen
import com.example.ui.screens.SellerDashboardScreen
import com.example.ui.screens.ServicesScreen
import com.example.ui.theme.SocialShipTheme
import com.example.ui.viewmodel.OrdersViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialOrderId = intent?.getStringExtra("extra_order_id")
        setContent {
            SocialShipTheme {
                AppContent(initialOrderId = initialOrderId)
            }
        }
    }
}

private enum class TopTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    SERVICES("services", R.string.nav_services, Icons.Filled.Storefront, Icons.Outlined.Storefront),
    PIPELINE("pipeline", R.string.nav_pipeline, Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong),
    DISPATCH("dispatch", R.string.nav_dispatch, Icons.Filled.LocalShipping, Icons.Outlined.LocalShipping),
    CUSTOMERS("customers", R.string.nav_customers, Icons.Filled.People, Icons.Outlined.People),
    OVERVIEW("overview", R.string.nav_overview, Icons.Filled.Analytics, Icons.Outlined.Analytics)
}

@Composable
fun AppContent(initialOrderId: String? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        val uid = user.uid
        key(uid) {
            SocialShipShell(
                currentUserId = uid,
                userDisplayName = user.displayName ?: user.email?.substringBefore("@") ?: "Kabarak User",
                userEmail = user.email,
                initialOrderId = initialOrderId,
                onSignOut = {
                    signOut(credentialManager = credentialManager, scope = scope) {
                        currentUser = null
                    }
                }
            )
        }
    }
}

@Composable
private fun SocialShipShell(
    currentUserId: String,
    userDisplayName: String,
    userEmail: String?,
    initialOrderId: String?,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val ordersViewModel: OrdersViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                OrdersViewModel(
                    repository = SocialShipRepository(db),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val ordersState by ordersViewModel.ordersState.collectAsStateWithLifecycle()
    val filteredOrdersState by ordersViewModel.filteredOrdersState.collectAsStateWithLifecycle()
    val customersState by ordersViewModel.customersState.collectAsStateWithLifecycle()
    val sellerProfile by ordersViewModel.sellerProfile.collectAsStateWithLifecycle()
    val searchQuery by ordersViewModel.searchQuery.collectAsStateWithLifecycle()
    val channelFilter by ordersViewModel.channelFilter.collectAsStateWithLifecycle()
    val stageFilter by ordersViewModel.stageFilter.collectAsStateWithLifecycle()
    val isMutating by ordersViewModel.isMutating.collectAsStateWithLifecycle()

    val availableServicesState by ordersViewModel.availableServicesState.collectAsStateWithLifecycle()
    val filteredServicesState by ordersViewModel.filteredServicesState.collectAsStateWithLifecycle()
    val myServicesState by ordersViewModel.myServicesState.collectAsStateWithLifecycle()
    val serviceSearch by ordersViewModel.serviceSearch.collectAsStateWithLifecycle()
    val serviceCategoryFilter by ordersViewModel.serviceCategoryFilter.collectAsStateWithLifecycle()
    val serviceZoneFilter by ordersViewModel.serviceZoneFilter.collectAsStateWithLifecycle()
    val incomingMarketplaceOrdersState by ordersViewModel.incomingMarketplaceOrdersState.collectAsStateWithLifecycle()
    val myPlacedOrdersState by ordersViewModel.myPlacedOrdersState.collectAsStateWithLifecycle()
    val notificationsState by ordersViewModel.notificationsState.collectAsStateWithLifecycle()
    val cartSelectedCategory by ordersViewModel.cartSelectedCategory.collectAsStateWithLifecycle()
    val cartItems by ordersViewModel.cartItems.collectAsStateWithLifecycle()

    val cartTotalQuantity = remember(cartItems) { cartItems.sumOf { it.quantity } }

    val unreadNotificationsCount = remember(notificationsState) {
        notificationsState.count { !it.read }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(ordersViewModel) {
        ordersViewModel.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(initialOrderId) {
        if (!initialOrderId.isNullOrBlank()) {
            navController.navigate("marketplace_order/$initialOrderId") {
                launchSingleTop = true
            }
        }
    }

    // Live seller & customer notification dispatcher
    val seenNotificationIds = remember(currentUserId) { mutableSetOf<String>() }
    var liveAlertsReady by remember(currentUserId) { mutableStateOf(false) }

    LaunchedEffect(currentUserId) {
        LocalNotificationHelper.ensureChannel(context)
        delay(1200L)
        notificationsState.forEach { seenNotificationIds.add(it.id) }
        liveAlertsReady = true
    }

    LaunchedEffect(notificationsState, liveAlertsReady) {
        if (!liveAlertsReady) {
            notificationsState.forEach { seenNotificationIds.add(it.id) }
        } else {
            val freshAlerts = notificationsState.filter { !it.read && seenNotificationIds.add(it.id) }
            freshAlerts.firstOrNull()?.let { latest ->
                LocalNotificationHelper.showOrderAlertNotification(context, latest)
                val result = snackbarHostState.showSnackbar(
                    message = "${latest.title}: ${latest.body}",
                    actionLabel = if (latest.orderId.isNotBlank()) "View" else null,
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed && latest.orderId.isNotBlank()) {
                    ordersViewModel.markNotificationRead(latest.id)
                    navController.navigate("marketplace_order/${latest.orderId}") {
                        launchSingleTop = true
                    }
                }
            }
            notificationsState.forEach { seenNotificationIds.add(it.id) }
        }
    }

    var showOrderForm by remember { mutableStateOf(false) }
    var editingOrder by remember { mutableStateOf<SocialOrder?>(null) }
    var showNotificationsSheet by remember { mutableStateOf(false) }

    val isTopLevelRoute = TopTab.entries.any { it.route == currentRoute }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (isTopLevelRoute && !useRail) {
                    NavigationBar {
                        TopTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(TopTab.SERVICES.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    if (tab == TopTab.PIPELINE && unreadNotificationsCount > 0) {
                                        BadgedBox(
                                            badge = { Badge { Text(unreadNotificationsCount.toString()) } }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = stringResource(tab.labelRes)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes)
                                        )
                                    }
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                when (currentRoute) {
                    TopTab.SERVICES.route -> {
                        ExtendedFloatingActionButton(
                            onClick = { navController.navigate("order_entry") },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (cartTotalQuantity > 0L) {
                                            Badge { Text(cartTotalQuantity.toString()) }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Rounded.ShoppingCart, contentDescription = null)
                                }
                            },
                            text = { Text(if (cartTotalQuantity > 0L) "Order Cart ($cartTotalQuantity)" else "Order Cart") },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("open_order_entry_fab")
                        )
                    }
                    TopTab.PIPELINE.route, TopTab.DISPATCH.route -> {
                        ExtendedFloatingActionButton(
                            onClick = {
                                editingOrder = null
                                showOrderForm = true
                            },
                            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                            text = { Text(stringResource(R.string.action_new_order)) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("new_order_fab")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isTopLevelRoute && useRail) {
                    NavigationRail(
                        header = {
                            if (currentRoute == TopTab.SERVICES.route) {
                                FloatingActionButton(
                                    onClick = { navController.navigate("order_entry") },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.testTag("open_order_entry_rail_fab")
                                ) {
                                    Icon(Icons.Rounded.ShoppingCart, contentDescription = "Order Cart")
                                }
                            } else {
                                FloatingActionButton(
                                    onClick = {
                                        editingOrder = null
                                        showOrderForm = true
                                    },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.testTag("new_order_rail_fab")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.action_new_order))
                                }
                            }
                        }
                    ) {
                        TopTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(TopTab.SERVICES.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    if (tab == TopTab.PIPELINE && unreadNotificationsCount > 0) {
                                        BadgedBox(
                                            badge = { Badge { Text(unreadNotificationsCount.toString()) } }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = stringResource(tab.labelRes)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes)
                                        )
                                    }
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag("rail_tab_${tab.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = TopTab.SERVICES.route
                    ) {
                        composable(TopTab.SERVICES.route) {
                            ServicesScreen(
                                currentUserId = currentUserId,
                                defaultBuyerName = userDisplayName,
                                filteredServicesState = filteredServicesState,
                                myServicesState = myServicesState,
                                sellerProfile = sellerProfile,
                                serviceSearch = serviceSearch,
                                categoryFilter = serviceCategoryFilter,
                                zoneFilter = serviceZoneFilter,
                                unreadNotificationsCount = unreadNotificationsCount,
                                isMutating = isMutating,
                                onSearchChange = ordersViewModel::setServiceSearch,
                                onCategoryFilterChange = ordersViewModel::setServiceCategoryFilter,
                                onZoneFilterChange = ordersViewModel::setServiceZoneFilter,
                                onOpenNotifications = { showNotificationsSheet = true },
                                onSaveServiceListing = { existingId, provider, title, cat, zone, desc, priceCents, currSym, unit, eta, ch, handle, avail, onDone ->
                                    ordersViewModel.saveServiceListing(
                                        existingServiceId = existingId,
                                        providerName = provider,
                                        title = title,
                                        category = cat,
                                        zone = zone,
                                        description = desc,
                                        priceCents = priceCents,
                                        currencySymbol = currSym,
                                        unitLabel = unit,
                                        etaLabel = eta,
                                        contactChannel = ch,
                                        contactHandle = handle,
                                        available = avail,
                                        onSuccess = onDone
                                    )
                                },
                                onToggleServiceAvailability = ordersViewModel::toggleServiceAvailability,
                                onDeleteServiceListing = ordersViewModel::deleteServiceListing,
                                onPlaceMarketplaceOrder = { svc, code, buyerName, phone, zone, details, qty, method, notes, onSuccess ->
                                    ordersViewModel.placeMarketplaceOrder(
                                        service = svc,
                                        orderCode = code,
                                        buyerName = buyerName,
                                        buyerHandleOrPhone = phone,
                                        deliveryZone = zone,
                                        deliveryDetails = details,
                                        quantity = qty,
                                        paymentMethod = method,
                                        notes = notes,
                                        onSuccess = onSuccess
                                    )
                                },
                                onOrderPlacedNavigate = { newOrderId ->
                                    navController.navigate("marketplace_order/$newOrderId")
                                },
                                onAddToCart = { service ->
                                    ordersViewModel.selectCartCategory(service.categoryEnum)
                                    ordersViewModel.addServiceToCart(service, 1L)
                                    ordersViewModel.notifyMessage("Added ${service.title} to Order Cart")
                                },
                                onOpenSellerDashboard = { navController.navigate("seller_dashboard") }
                            )
                        }

                        composable("order_entry") {
                            OrderEntryScreen(
                                selectedCategory = cartSelectedCategory,
                                availableServicesState = availableServicesState,
                                cartItems = cartItems,
                                sellerProfile = sellerProfile,
                                defaultBuyerName = userDisplayName,
                                isMutating = isMutating,
                                onBack = { navController.popBackStack() },
                                onSelectCategory = ordersViewModel::selectCartCategory,
                                onAddServiceToCart = ordersViewModel::addServiceToCart,
                                onAddCustomItemToCart = ordersViewModel::addCustomItemToCart,
                                onUpdateCartQuantity = ordersViewModel::updateCartItemQuantity,
                                onRemoveCartItem = ordersViewModel::removeCartItem,
                                onClearCart = ordersViewModel::clearCart,
                                onSubmitOrder = { buyerName, phoneOrHandle, ch, zone, details, method, currSym, notes ->
                                    ordersViewModel.submitCartOrder(
                                        buyerName = buyerName,
                                        buyerPhoneOrHandle = phoneOrHandle,
                                        channel = ch,
                                        deliveryZone = zone,
                                        deliveryDetails = details,
                                        paymentMethod = method,
                                        currencySymbol = currSym,
                                        notes = notes
                                    ) { destinationRoute ->
                                        navController.navigate(destinationRoute) {
                                            popUpTo(TopTab.SERVICES.route)
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            )
                        }

                        composable(TopTab.PIPELINE.route) {
                            PipelineScreen(
                                ordersState = ordersState,
                                filteredOrdersState = filteredOrdersState,
                                incomingMarketplaceOrdersState = incomingMarketplaceOrdersState,
                                myPlacedOrdersState = myPlacedOrdersState,
                                unreadNotificationsCount = unreadNotificationsCount,
                                sellerProfile = sellerProfile,
                                searchQuery = searchQuery,
                                channelFilter = channelFilter,
                                stageFilter = stageFilter,
                                onSearchChange = ordersViewModel::setSearchQuery,
                                onChannelFilterChange = ordersViewModel::setChannelFilter,
                                onStageFilterChange = ordersViewModel::setStageFilter,
                                onOpenNotifications = { showNotificationsSheet = true },
                                onBrowseServicesClick = {
                                    navController.navigate(TopTab.SERVICES.route) {
                                        popUpTo(TopTab.SERVICES.route) { saveState = true }
                                        launchSingleTop = true
                                    }
                                },
                                onMarketplaceOrderClick = { order ->
                                    navController.navigate("marketplace_order/${order.id}")
                                },
                                onAdvanceMarketplaceOrder = { order, nextStage, courier, tracking, note ->
                                    ordersViewModel.advanceMarketplaceOrderStage(
                                        order = order,
                                        targetStage = nextStage,
                                        courierName = courier,
                                        trackingNumber = tracking,
                                        note = note
                                    )
                                },
                                onOrderClick = { order ->
                                    navController.navigate("order_detail/${order.id}")
                                },
                                onCreateOrderClick = {
                                    editingOrder = null
                                    showOrderForm = true
                                },
                                onAdvanceOrder = { order, nextStage, courier, tracking, note ->
                                    ordersViewModel.advanceOrderStage(
                                        order = order,
                                        targetStage = nextStage,
                                        courierName = courier,
                                        trackingNumber = tracking,
                                        eventNote = note
                                    )
                                },
                                onNotifyMessage = ordersViewModel::notifyMessage,
                                onOpenSellerDashboard = { navController.navigate("seller_dashboard") }
                            )
                        }

                        composable(TopTab.DISPATCH.route) {
                            DispatchScreen(
                                ordersState = ordersState,
                                incomingMarketplaceOrdersState = incomingMarketplaceOrdersState,
                                sellerProfile = sellerProfile,
                                onMarketplaceOrderClick = { order ->
                                    navController.navigate("marketplace_order/${order.id}")
                                },
                                onAdvanceMarketplaceOrder = { order, nextStage, courier, tracking, note ->
                                    ordersViewModel.advanceMarketplaceOrderStage(
                                        order = order,
                                        targetStage = nextStage,
                                        courierName = courier,
                                        trackingNumber = tracking,
                                        note = note
                                    )
                                },
                                onOrderClick = { order ->
                                    navController.navigate("order_detail/${order.id}")
                                },
                                onAdvanceOrder = { order, nextStage, courier, tracking, note ->
                                    ordersViewModel.advanceOrderStage(
                                        order = order,
                                        targetStage = nextStage,
                                        courierName = courier,
                                        trackingNumber = tracking,
                                        eventNote = note
                                    )
                                },
                                onNotifyMessage = ordersViewModel::notifyMessage
                            )
                        }

                        composable(TopTab.CUSTOMERS.route) {
                            CustomersScreen(
                                customersState = customersState,
                                sellerProfile = sellerProfile,
                                isMutating = isMutating,
                                onSaveCustomer = { existingId, name, ch, handle, email, addr, notes, onDone ->
                                    ordersViewModel.saveCustomer(
                                        existingCustomerId = existingId,
                                        displayName = name,
                                        channel = ch,
                                        handleOrPhone = handle,
                                        email = email,
                                        shippingAddress = addr,
                                        notes = notes,
                                        onSuccess = onDone
                                    )
                                },
                                onDeleteCustomer = ordersViewModel::deleteCustomer,
                                onFilterOrdersByCustomer = { handleOrPhone ->
                                    ordersViewModel.setSearchQuery(handleOrPhone)
                                    navController.navigate(TopTab.PIPELINE.route) {
                                        popUpTo(TopTab.SERVICES.route) { saveState = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        composable(TopTab.OVERVIEW.route) {
                            OverviewScreen(
                                ordersState = ordersState,
                                incomingMarketplaceOrdersState = incomingMarketplaceOrdersState,
                                myPlacedOrdersState = myPlacedOrdersState,
                                unreadNotificationsCount = unreadNotificationsCount,
                                sellerProfile = sellerProfile,
                                userEmail = userEmail,
                                isMutating = isMutating,
                                onOpenNotifications = { showNotificationsSheet = true },
                                onSaveProfile = { name, sym, greeting ->
                                    ordersViewModel.saveSellerProfile(name, sym, greeting)
                                },
                                onSignOut = onSignOut,
                                onOpenSellerDashboard = { navController.navigate("seller_dashboard") }
                            )
                        }

                        composable("seller_dashboard") {
                            SellerDashboardScreen(
                                incomingMarketplaceOrdersState = incomingMarketplaceOrdersState,
                                directOrdersState = ordersState,
                                sellerProfile = sellerProfile,
                                unreadNotificationsCount = unreadNotificationsCount,
                                isMutating = isMutating,
                                onBack = { navController.popBackStack() },
                                onOpenNotifications = { showNotificationsSheet = true },
                                onConfirmMarketplaceOrder = { order ->
                                    ordersViewModel.advanceMarketplaceOrderStage(
                                        order = order,
                                        targetStage = OrderStage.PAID,
                                        note = "Confirmed on Seller Dashboard"
                                    )
                                },
                                onDispatchMarketplaceOrder = { order ->
                                    ordersViewModel.advanceMarketplaceOrderStage(
                                        order = order,
                                        targetStage = OrderStage.SHIPPED,
                                        note = "Dispatched on Seller Dashboard"
                                    )
                                },
                                onOpenMarketplaceOrder = { order ->
                                    navController.navigate("marketplace_order/${order.id}")
                                },
                                onConfirmDirectOrder = { order ->
                                    ordersViewModel.advanceOrderStage(
                                        order = order,
                                        targetStage = OrderStage.PAID,
                                        eventNote = "Confirmed on Seller Dashboard"
                                    )
                                },
                                onDispatchDirectOrder = { order ->
                                    ordersViewModel.advanceOrderStage(
                                        order = order,
                                        targetStage = OrderStage.SHIPPED,
                                        eventNote = "Dispatched on Seller Dashboard"
                                    )
                                },
                                onOpenDirectOrder = { order ->
                                    navController.navigate("order_detail/${order.id}")
                                }
                            )
                        }

                        composable(
                            route = "marketplace_order/{orderId}",
                            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                        ) { entry ->
                            val orderId = entry.arguments?.getString("orderId").orEmpty()
                            val orderState by remember(orderId) {
                                ordersViewModel.observeMarketplaceOrder(orderId)
                            }.collectAsStateWithLifecycle(initialValue = UiState.Loading)

                            val loadedOrder = (orderState as? UiState.Success)?.data
                            val eventsState by remember(loadedOrder?.id, loadedOrder?.buyerUid, loadedOrder?.sellerUid) {
                                if (loadedOrder != null) {
                                    ordersViewModel.observeMarketplaceEvents(loadedOrder)
                                } else {
                                    flowOf(UiState.Success(emptyList()))
                                }
                            }.collectAsStateWithLifecycle(initialValue = UiState.Loading)

                            val messagesState by remember(loadedOrder?.id, loadedOrder?.buyerUid, loadedOrder?.sellerUid) {
                                if (loadedOrder != null) {
                                    ordersViewModel.observeOrderMessages(loadedOrder)
                                } else {
                                    flowOf(UiState.Success(emptyList()))
                                }
                            }.collectAsStateWithLifecycle(initialValue = UiState.Loading)

                            MarketplaceOrderDetailScreen(
                                currentUserId = currentUserId,
                                currentUserName = userDisplayName,
                                orderState = orderState,
                                eventsState = eventsState,
                                messagesState = messagesState,
                                isMutating = isMutating,
                                onBack = { navController.popBackStack() },
                                onAdvanceStage = { targetOrder, nextStage, courier, tracking, note ->
                                    ordersViewModel.advanceMarketplaceOrderStage(
                                        order = targetOrder,
                                        targetStage = nextStage,
                                        courierName = courier,
                                        trackingNumber = tracking,
                                        note = note
                                    )
                                },
                                onUpdatePayment = ordersViewModel::updateMarketplaceOrderPayment,
                                onSendMessage = ordersViewModel::sendOrderMessage,
                                onNotifyMessage = ordersViewModel::notifyMessage
                            )
                        }

                        composable(
                            route = "order_detail/{orderId}",
                            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                        ) { entry ->
                            val orderId = entry.arguments?.getString("orderId").orEmpty()
                            val order = (ordersState as? UiState.Success)?.data?.firstOrNull { it.id == orderId }
                            val eventsState by remember(orderId) {
                                ordersViewModel.observeOrderEvents(orderId)
                            }.collectAsStateWithLifecycle(initialValue = UiState.Loading)

                            OrderDetailScreen(
                                order = order,
                                eventsState = eventsState,
                                sellerProfile = sellerProfile,
                                isMutating = isMutating,
                                onBack = { navController.popBackStack() },
                                onEditOrder = { toEdit ->
                                    editingOrder = toEdit
                                    showOrderForm = true
                                },
                                onAdvanceStage = { targetOrder, nextStage, courier, tracking, note ->
                                    ordersViewModel.advanceOrderStage(
                                        order = targetOrder,
                                        targetStage = nextStage,
                                        courierName = courier,
                                        trackingNumber = tracking,
                                        eventNote = note
                                    )
                                },
                                onUpdatePayment = ordersViewModel::updateOrderPayment,
                                onDeleteOrder = { targetOrder ->
                                    ordersViewModel.deleteOrder(targetOrder.id, targetOrder.orderCode) {
                                        navController.popBackStack()
                                    }
                                },
                                onNotifyMessage = ordersViewModel::notifyMessage
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNotificationsSheet) {
        NotificationsSheet(
            notifications = notificationsState,
            onDismiss = { showNotificationsSheet = false },
            onNotificationClick = { alert ->
                if (!alert.read) {
                    ordersViewModel.markNotificationRead(alert.id)
                }
                showNotificationsSheet = false
                if (alert.orderId.isNotBlank()) {
                    navController.navigate("marketplace_order/${alert.orderId}") {
                        launchSingleTop = true
                    }
                }
            },
            onMarkAllRead = { unreadIds ->
                ordersViewModel.markAllNotificationsRead(unreadIds)
            }
        )
    }

    if (showOrderForm) {
        val customersList = (customersState as? UiState.Success)?.data.orEmpty()
        OrderFormSheet(
            existingOrder = editingOrder,
            customers = customersList,
            currencySymbol = sellerProfile.currencySymbol,
            isSaving = isMutating,
            onDismiss = {
                showOrderForm = false
                editingOrder = null
            },
            onSubmitCreate = { orderCode, custId, custName, ch, handle, addr, items, count, cents, method, payStatus, st, courier, tracking, notes, dm, saveDir ->
                ordersViewModel.createOrder(
                    orderCode = orderCode,
                    customerId = custId,
                    customerName = custName,
                    channel = ch,
                    handleOrPhone = handle,
                    shippingAddress = addr,
                    itemsSummary = items,
                    itemCount = count,
                    totalCents = cents,
                    currencySymbol = sellerProfile.currencySymbol,
                    paymentMethod = method,
                    paymentStatus = payStatus,
                    stage = st,
                    courierName = courier,
                    trackingNumber = tracking,
                    notes = notes,
                    dmMessageSnapshot = dm,
                    saveToCustomerDirectory = saveDir
                ) {
                    showOrderForm = false
                    editingOrder = null
                }
            },
            onSubmitUpdate = { target, custName, ch, handle, addr, items, count, cents, method, payStatus, st, courier, tracking, notes, dm ->
                ordersViewModel.updateOrderDetails(
                    order = target,
                    customerName = custName,
                    channel = ch,
                    handleOrPhone = handle,
                    shippingAddress = addr,
                    itemsSummary = items,
                    itemCount = count,
                    totalCents = cents,
                    paymentMethod = method,
                    paymentStatus = payStatus,
                    stage = st,
                    courierName = courier,
                    trackingNumber = tracking,
                    notes = notes,
                    dmMessageSnapshot = dm
                ) {
                    showOrderForm = false
                    editingOrder = null
                }
            }
        )
    }
}
