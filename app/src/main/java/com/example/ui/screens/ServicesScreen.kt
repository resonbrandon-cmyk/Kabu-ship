package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.KabarakZone
import com.example.model.OrderChannel
import com.example.model.PaymentMethod
import com.example.model.SellerProfile
import com.example.model.ServiceCategory
import com.example.model.ServiceListing
import com.example.model.formatMoney
import com.example.model.generateOrderCode
import com.example.ui.components.ChannelBadge
import com.example.ui.components.EmptyArtView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.KabarakZoneChip
import com.example.ui.components.LoadingStateView
import com.example.ui.components.ServiceCategoryBadge
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SuccessEmerald
import com.example.ui.viewmodel.UiState
import kotlin.math.roundToLong

private enum class ServicesSubTab(val label: String) {
    BROWSE("Browse Services"),
    MY_LISTINGS("My Listings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(
    currentUserId: String,
    defaultBuyerName: String,
    filteredServicesState: UiState<List<ServiceListing>>,
    myServicesState: UiState<List<ServiceListing>>,
    sellerProfile: SellerProfile,
    serviceSearch: String,
    categoryFilter: ServiceCategory?,
    zoneFilter: KabarakZone?,
    unreadNotificationsCount: Int,
    isMutating: Boolean,
    onSearchChange: (String) -> Unit,
    onCategoryFilterChange: (ServiceCategory?) -> Unit,
    onZoneFilterChange: (KabarakZone?) -> Unit,
    onOpenNotifications: () -> Unit,
    onSaveServiceListing: (
        existingServiceId: String?,
        providerName: String,
        title: String,
        category: ServiceCategory,
        zone: KabarakZone,
        description: String,
        priceCents: Long,
        currencySymbol: String,
        unitLabel: String,
        etaLabel: String,
        contactChannel: OrderChannel,
        contactHandle: String,
        available: Boolean,
        onSuccess: () -> Unit
    ) -> Unit,
    onToggleServiceAvailability: (ServiceListing) -> Unit,
    onDeleteServiceListing: (ServiceListing) -> Unit,
    onPlaceMarketplaceOrder: (
        service: ServiceListing,
        orderCode: String,
        buyerName: String,
        buyerHandleOrPhone: String,
        deliveryZone: KabarakZone,
        deliveryDetails: String,
        quantity: Long,
        paymentMethod: PaymentMethod,
        notes: String?,
        onSuccess: (String) -> Unit
    ) -> Unit,
    onOrderPlacedNavigate: (String) -> Unit,
    onAddToCart: (ServiceListing) -> Unit = {},
    onOpenSellerDashboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableStateOf(ServicesSubTab.BROWSE) }
    var showPublishSheet by remember { mutableStateOf(false) }
    var editingService by remember { mutableStateOf<ServiceListing?>(null) }
    var orderingService by remember { mutableStateOf<ServiceListing?>(null) }
    var deletingService by remember { mutableStateOf<ServiceListing?>(null) }

    val myServices = (myServicesState as? UiState.Success)?.data.orEmpty()

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "KABARAK UNIVERSITY & COMMUNITY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Campus & Local Services",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("services_notifications_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotificationsCount > 0) {
                                        Badge { Text(unreadNotificationsCount.toString()) }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Notifications,
                                    contentDescription = "Open notifications"
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = {
                                editingService = null
                                showPublishSheet = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("offer_service_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.action_publish_service))
                        }
                    }
                }
            }

            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ServicesSubTab.entries.forEachIndexed { index, tab ->
                        val label = if (tab == ServicesSubTab.MY_LISTINGS) {
                            "${tab.label} (${myServices.size})"
                        } else {
                            tab.label
                        }
                        SegmentedButton(
                            selected = activeSubTab == tab,
                            onClick = { activeSubTab = tab },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = ServicesSubTab.entries.size
                            ),
                            modifier = Modifier.testTag("services_tab_${tab.name.lowercase()}")
                        ) {
                            Text(label)
                        }
                    }
                }
            }

            if (activeSubTab == ServicesSubTab.BROWSE) {
                item {
                    OutlinedTextField(
                        value = serviceSearch,
                        onValueChange = onSearchChange,
                        placeholder = { Text("Search food, water, gas, hostel delivery...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (serviceSearch.isNotEmpty()) {
                                IconButton(onClick = { onSearchChange("") }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("services_search_input")
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = categoryFilter == null,
                                    onClick = { onCategoryFilterChange(null) },
                                    label = { Text("All Categories") }
                                )
                            }
                            items(ServiceCategory.entries) { cat ->
                                FilterChip(
                                    selected = categoryFilter == cat,
                                    onClick = {
                                        onCategoryFilterChange(if (categoryFilter == cat) null else cat)
                                    },
                                    label = { Text(cat.label) },
                                    modifier = Modifier.testTag("service_cat_${cat.name.lowercase()}")
                                )
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = zoneFilter == null,
                                    onClick = { onZoneFilterChange(null) },
                                    label = { Text("All Kabarak Zones") }
                                )
                            }
                            items(KabarakZone.entries) { zone ->
                                FilterChip(
                                    selected = zoneFilter == zone,
                                    onClick = {
                                        onZoneFilterChange(if (zoneFilter == zone) null else zone)
                                    },
                                    label = { Text(zone.label) },
                                    modifier = Modifier.testTag("service_zone_${zone.name.lowercase()}")
                                )
                            }
                        }
                    }
                }

                when (filteredServicesState) {
                    is UiState.Loading -> item { LoadingStateView("Loading Kabarak services...") }
                    is UiState.Error -> item { ErrorStateView(filteredServicesState.message) }
                    is UiState.Success -> {
                        val list = filteredServicesState.data
                        if (list.isEmpty()) {
                            item {
                                val hasFilters = serviceSearch.isNotBlank() || categoryFilter != null || zoneFilter != null
                                EmptyArtView(
                                    title = if (hasFilters) "No matching services" else stringResource(R.string.empty_services_title),
                                    subtitle = if (hasFilters) {
                                        "Try clearing your search or choosing a different category or Kabarak zone."
                                    } else {
                                        stringResource(R.string.empty_services_subtitle)
                                    },
                                    actionLabel = if (hasFilters) "Clear Filters" else stringResource(R.string.action_publish_service),
                                    onAction = {
                                        if (hasFilters) {
                                            onSearchChange("")
                                            onCategoryFilterChange(null)
                                            onZoneFilterChange(null)
                                        } else {
                                            editingService = null
                                            showPublishSheet = true
                                        }
                                    },
                                    showArtwork = !hasFilters
                                )
                            }
                        } else {
                            items(list, key = { it.id }) { service ->
                                BrowseServiceCard(
                                    service = service,
                                    isOwnListing = service.ownerUid == currentUserId,
                                    onAddToCartClick = { onAddToCart(service) },
                                    onOrderClick = { orderingService = service },
                                    onEditClick = {
                                        editingService = service
                                        showPublishSheet = true
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    FilledTonalButton(
                        onClick = onOpenSellerDashboard,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("services_seller_dashboard_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Dashboard,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Seller Dashboard")
                    }
                }
                when (myServicesState) {
                    is UiState.Loading -> item { LoadingStateView("Loading your listings...") }
                    is UiState.Error -> item { ErrorStateView(myServicesState.message) }
                    is UiState.Success -> {
                        if (myServices.isEmpty()) {
                            item {
                                EmptyArtView(
                                    title = "You haven't offered any services yet",
                                    subtitle = "List food meals, 20L clean water delivery, cooking gas, laundry pickup, or campus errands so Kabarak students and locals can order and notify you instantly.",
                                    actionLabel = stringResource(R.string.action_publish_service),
                                    onAction = {
                                        editingService = null
                                        showPublishSheet = true
                                    },
                                    showArtwork = false
                                )
                            }
                        } else {
                            items(myServices, key = { "mine_${it.id}" }) { service ->
                                MyServiceCard(
                                    service = service,
                                    onToggleAvailable = { onToggleServiceAvailability(service) },
                                    onEdit = {
                                        editingService = service
                                        showPublishSheet = true
                                    },
                                    onDelete = { deletingService = service },
                                    onTestOrder = { orderingService = service }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPublishSheet) {
        PublishServiceSheet(
            existing = editingService,
            defaultProviderName = sellerProfile.storeName,
            defaultCurrency = sellerProfile.currencySymbol,
            isSaving = isMutating,
            onDismiss = {
                showPublishSheet = false
                editingService = null
            },
            onSave = { providerName, title, cat, zone, desc, priceCents, unitLabel, etaLabel, channel, handle, available ->
                onSaveServiceListing(
                    editingService?.id,
                    providerName,
                    title,
                    cat,
                    zone,
                    desc,
                    priceCents,
                    sellerProfile.currencySymbol,
                    unitLabel,
                    etaLabel,
                    channel,
                    handle,
                    available
                ) {
                    showPublishSheet = false
                    editingService = null
                }
            }
        )
    }

    val activeOrderTarget = orderingService
    if (activeOrderTarget != null) {
        PlaceOrderSheet(
            service = activeOrderTarget,
            defaultBuyerName = defaultBuyerName,
            isPlacing = isMutating,
            onDismiss = { orderingService = null },
            onPlaceOrder = { code, buyerName, phone, zone, details, qty, method, notes ->
                onPlaceMarketplaceOrder(
                    activeOrderTarget,
                    code,
                    buyerName,
                    phone,
                    zone,
                    details,
                    qty,
                    method,
                    notes
                ) { newOrderId ->
                    orderingService = null
                    onOrderPlacedNavigate(newOrderId)
                }
            }
        )
    }

    val pendingDelete = deletingService
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { deletingService = null },
            title = { Text("Delete \"${pendingDelete.title}\"?") },
            text = { Text("Students and community users will no longer see this listing.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteServiceListing(pendingDelete)
                        deletingService = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingService = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun BrowseServiceCard(
    service: ServiceListing,
    isOwnListing: Boolean,
    onAddToCartClick: () -> Unit,
    onOrderClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("service_card_${service.id}")
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ServiceCategoryBadge(service.categoryEnum)
                    KabarakZoneChip(service.zoneEnum)
                }
                if (isOwnListing) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Your Listing",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = service.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = service.providerName,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatMoney(service.priceCents, service.currencySymbol),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = service.unitLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (service.description.isNotBlank()) {
                Text(
                    text = service.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
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
                    ChannelBadge(service.channelEnum)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = service.etaLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isOwnListing) {
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit service")
                        }
                    }
                    IconButton(
                        onClick = onAddToCartClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("add_to_cart_button_${service.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddShoppingCart,
                            contentDescription = "Add to cart",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(
                        onClick = onOrderClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("order_service_button_${service.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ShoppingBag,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_order_now))
                    }
                }
            }
        }
    }
}

@Composable
private fun MyServiceCard(
    service: ServiceListing,
    onToggleAvailable: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTestOrder: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("my_service_card_${service.id}")
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ServiceCategoryBadge(service.categoryEnum)
                    KabarakZoneChip(service.zoneEnum)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (service.available) "Live" else "Paused",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (service.available) SuccessEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = service.available,
                        onCheckedChange = { onToggleAvailable() }
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(service.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${service.providerName} · ${service.contactHandle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${formatMoney(service.priceCents, service.currencySymbol)} / ${service.unitLabel}",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onTestOrder,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                ) {
                    Text("Place Test Order")
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit listing")
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete listing",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PublishServiceSheet(
    existing: ServiceListing?,
    defaultProviderName: String,
    defaultCurrency: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        providerName: String,
        title: String,
        category: ServiceCategory,
        zone: KabarakZone,
        description: String,
        priceCents: Long,
        unitLabel: String,
        etaLabel: String,
        contactChannel: OrderChannel,
        contactHandle: String,
        available: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var providerName by remember { mutableStateOf(existing?.providerName ?: defaultProviderName) }
    var category by remember { mutableStateOf(existing?.categoryEnum ?: ServiceCategory.FOOD) }
    var zone by remember { mutableStateOf(existing?.zoneEnum ?: KabarakZone.MAIN_GATE) }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var priceText by remember {
        mutableStateOf(
            if (existing != null && existing.priceCents > 0L) {
                String.format("%.2f", existing.priceCents / 100.0)
            } else ""
        )
    }
    var unitLabel by remember { mutableStateOf(existing?.unitLabel ?: "per order") }
    var etaLabel by remember { mutableStateOf(existing?.etaLabel ?: "15-25 mins") }
    var contactChannel by remember { mutableStateOf(existing?.channelEnum ?: OrderChannel.WHATSAPP) }
    var contactHandle by remember { mutableStateOf(existing?.contactHandle ?: "") }
    var available by remember { mutableStateOf(existing?.available ?: true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = if (existing == null) "Offer a Kabarak Service" else "Edit Service Listing",
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 120) title = it; errorMessage = null },
                label = { Text("Service Title *") },
                placeholder = { Text("e.g. Chapati & Beef Stew Combo / 20L Clean Water") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("service_title_input")
            )

            OutlinedTextField(
                value = providerName,
                onValueChange = { if (it.length <= 80) providerName = it; errorMessage = null },
                label = { Text("Business / Provider Name *") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("service_provider_input")
            )

            Text("Category", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ServiceCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat.label) }
                    )
                }
            }

            Text("Primary Kabarak Zone", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                KabarakZone.entries.forEach { z ->
                    FilterChip(
                        selected = zone == z,
                        onClick = { zone = z },
                        label = { Text(z.label) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { if (it.length <= 12) priceText = it; errorMessage = null },
                    label = { Text("Price (${defaultCurrency.trim()}) *") },
                    placeholder = { Text("150") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("service_price_input")
                )
                OutlinedTextField(
                    value = unitLabel,
                    onValueChange = { if (it.length <= 30) unitLabel = it },
                    label = { Text("Unit") },
                    placeholder = { Text("20L jerrican / plate") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = etaLabel,
                    onValueChange = { if (it.length <= 30) etaLabel = it },
                    label = { Text("Delivery ETA") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = contactHandle,
                    onValueChange = { if (it.length <= 60) contactHandle = it; errorMessage = null },
                    label = { Text("WhatsApp / IG Handle *") },
                    placeholder = { Text("+2547... or @handle") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("service_contact_input")
                )
            }

            Text("Preferred Outreach Channel", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OrderChannel.entries.forEach { ch ->
                    FilterChip(
                        selected = contactChannel == ch,
                        onClick = { contactChannel = ch },
                        label = { Text(ch.label) }
                    )
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 500) description = it },
                label = { Text("Service Details & Menu / Notes") },
                placeholder = { Text("Include operating hours, free hostel delivery details, or bundle options") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Available for immediate orders", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = available, onCheckedChange = { available = it })
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    val parsed = priceText.trim().toDoubleOrNull()
                    when {
                        title.isBlank() -> errorMessage = "Enter a service title."
                        providerName.isBlank() -> errorMessage = "Enter a provider or shop name."
                        parsed == null || parsed < 0.0 -> errorMessage = "Enter a valid numeric price."
                        contactHandle.isBlank() -> errorMessage = "Enter a WhatsApp phone or Instagram handle."
                        else -> {
                            onSave(
                                providerName.trim(),
                                title.trim(),
                                category,
                                zone,
                                description.trim(),
                                (parsed * 100.0).roundToLong(),
                                unitLabel.trim().ifBlank { "per order" },
                                etaLabel.trim().ifBlank { "15-25 mins" },
                                contactChannel,
                                contactHandle.trim(),
                                available
                            )
                        }
                    }
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_service_button")
            ) {
                Text(if (isSaving) "Publishing..." else if (existing == null) "Publish Service" else "Save Changes")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PlaceOrderSheet(
    service: ServiceListing,
    defaultBuyerName: String,
    isPlacing: Boolean,
    onDismiss: () -> Unit,
    onPlaceOrder: (
        orderCode: String,
        buyerName: String,
        buyerPhone: String,
        zone: KabarakZone,
        deliveryDetails: String,
        quantity: Long,
        paymentMethod: PaymentMethod,
        notes: String?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val orderCode = remember(service.id) { generateOrderCode("KB") }
    var buyerName by remember { mutableStateOf(defaultBuyerName) }
    var buyerPhone by remember { mutableStateOf("") }
    var zone by remember { mutableStateOf(service.zoneEnum) }
    var deliveryDetails by remember { mutableStateOf("") }
    var quantity by remember { mutableLongStateOf(1L) }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.MPESA) }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Place Kabarak Order", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "${service.title} · ${service.providerName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = orderCode,
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Quantity & Live Total
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Column {
                        Text("Quantity (${service.unitLabel})", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = formatMoney(service.priceCents * quantity, service.currencySymbol),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { if (quantity > 1L) quantity -= 1L },
                            enabled = quantity > 1L,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.Remove, contentDescription = "Decrease quantity")
                        }
                        Text(
                            text = quantity.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily)
                        )
                        FilledTonalIconButton(
                            onClick = { if (quantity < 99L) quantity += 1L },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Increase quantity")
                        }
                    }
                }
            }

            OutlinedTextField(
                value = buyerName,
                onValueChange = { if (it.length <= 80) buyerName = it; errorMessage = null },
                label = { Text("Your Name *") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("order_buyer_name_input")
            )

            OutlinedTextField(
                value = buyerPhone,
                onValueChange = { if (it.length <= 60) buyerPhone = it; errorMessage = null },
                label = { Text("Phone / WhatsApp Number *") },
                placeholder = { Text("+2547... or 07...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("order_buyer_phone_input")
            )

            Text("Kabarak Delivery Zone", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                KabarakZone.entries.forEach { z ->
                    FilterChip(
                        selected = zone == z,
                        onClick = { zone = z },
                        label = { Text(z.label) }
                    )
                }
            }

            OutlinedTextField(
                value = deliveryDetails,
                onValueChange = { if (it.length <= 160) deliveryDetails = it; errorMessage = null },
                label = { Text("Hostel / Room / Estate / Gate Spot *") },
                placeholder = { Text("e.g. Men's Hostel B Room 14, or Rafiki Stage") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("order_delivery_details_input")
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
                onValueChange = { if (it.length <= 240) notes = it },
                label = { Text("Special Instructions (Optional)") },
                placeholder = { Text("Extra sauce, call at gate, bring change...") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    when {
                        buyerName.isBlank() -> errorMessage = "Enter your name."
                        buyerPhone.isBlank() -> errorMessage = "Enter a phone or WhatsApp contact."
                        deliveryDetails.isBlank() -> errorMessage = "Enter your hostel, gate, or drop-off details."
                        else -> onPlaceOrder(
                            orderCode,
                            buyerName.trim(),
                            buyerPhone.trim(),
                            zone,
                            deliveryDetails.trim(),
                            quantity,
                            paymentMethod,
                            notes.trim().takeIf { it.isNotEmpty() }
                        )
                    }
                },
                enabled = !isPlacing,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("confirm_place_order_button")
            ) {
                Text(if (isPlacing) "Notifying Provider..." else "Place Order & Notify Provider")
            }
        }
    }
}
