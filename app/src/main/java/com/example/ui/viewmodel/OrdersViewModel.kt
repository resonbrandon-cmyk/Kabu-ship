package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SocialShipRepository
import com.example.model.AppNotification
import com.example.model.CartEntryItem
import com.example.model.Customer
import com.example.model.KabarakZone
import com.example.model.MarketplaceOrder
import com.example.model.MarketplaceOrderEvent
import com.example.model.OrderChannel
import com.example.model.OrderEvent
import com.example.model.OrderMessage
import com.example.model.OrderStage
import com.example.model.PaymentMethod
import com.example.model.PaymentStatus
import com.example.model.SellerProfile
import com.example.model.ServiceCategory
import com.example.model.ServiceListing
import com.example.model.SocialOrder
import com.example.model.generateOrderCode
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class StageFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    NEW_DM("Placed / New"),
    PAID("Accepted / Paid"),
    PACKING("Preparing"),
    SHIPPED("Dispatched"),
    OUT_FOR_DELIVERY("Out for Delivery"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled")
}

class OrdersViewModel(
    private val repository: SocialShipRepository,
    val currentUserId: String
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _channelFilter = MutableStateFlow<OrderChannel?>(null)
    val channelFilter: StateFlow<OrderChannel?> = _channelFilter.asStateFlow()

    private val _stageFilter = MutableStateFlow(StageFilter.ALL)
    val stageFilter: StateFlow<StageFilter> = _stageFilter.asStateFlow()

    private val _serviceSearch = MutableStateFlow("")
    val serviceSearch: StateFlow<String> = _serviceSearch.asStateFlow()

    private val _serviceCategoryFilter = MutableStateFlow<ServiceCategory?>(null)
    val serviceCategoryFilter: StateFlow<ServiceCategory?> = _serviceCategoryFilter.asStateFlow()

    private val _serviceZoneFilter = MutableStateFlow<KabarakZone?>(null)
    val serviceZoneFilter: StateFlow<KabarakZone?> = _serviceZoneFilter.asStateFlow()

    private val _cartSelectedCategory = MutableStateFlow(ServiceCategory.FOOD)
    val cartSelectedCategory: StateFlow<ServiceCategory> = _cartSelectedCategory.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartEntryItem>>(emptyList())
    val cartItems: StateFlow<List<CartEntryItem>> = _cartItems.asStateFlow()

    private val _isMutating = MutableStateFlow(false)
    val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private fun <T> Flow<T>.unwrapFirestoreError(fallbackMsg: String): Flow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, fallbackMsg, rootError)
                emit(UiState.Error(rootError.message ?: fallbackMsg))
            }

    val sellerProfile: StateFlow<SellerProfile> = repository.observeSellerProfile(currentUserId)
        .map { it ?: SellerProfile(userId = currentUserId) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing seller profile", firestoreError ?: error)
            emit(SellerProfile(userId = currentUserId))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SellerProfile(userId = currentUserId))

    val ordersState: StateFlow<UiState<List<SocialOrder>>> = repository.observeOrders(currentUserId)
        .unwrapFirestoreError("Unable to sync DM orders")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val filteredOrdersState: StateFlow<UiState<List<SocialOrder>>> = combine(
        ordersState,
        _searchQuery,
        _channelFilter,
        _stageFilter
    ) { state, query, channel, stage ->
        when (state) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> state
            is UiState.Success -> {
                val trimmed = query.trim().lowercase()
                val filtered = state.data.filter { order ->
                    val matchesChannel = channel == null || order.channelEnum == channel
                    val matchesStage = when (stage) {
                        StageFilter.ALL -> true
                        StageFilter.ACTIVE -> !order.stageEnum.isTerminal
                        StageFilter.NEW_DM -> order.stageEnum == OrderStage.NEW_DM
                        StageFilter.PAID -> order.stageEnum == OrderStage.PAID
                        StageFilter.PACKING -> order.stageEnum == OrderStage.PACKING
                        StageFilter.SHIPPED -> order.stageEnum == OrderStage.SHIPPED
                        StageFilter.OUT_FOR_DELIVERY -> order.stageEnum == OrderStage.OUT_FOR_DELIVERY
                        StageFilter.DELIVERED -> order.stageEnum == OrderStage.DELIVERED
                        StageFilter.CANCELLED -> order.stageEnum == OrderStage.CANCELLED
                    }
                    val matchesQuery = trimmed.isEmpty() ||
                        order.orderCode.lowercase().contains(trimmed) ||
                        order.customerName.lowercase().contains(trimmed) ||
                        order.handleOrPhone.lowercase().contains(trimmed) ||
                        order.itemsSummary.lowercase().contains(trimmed) ||
                        (order.trackingNumber?.lowercase()?.contains(trimmed) == true)
                    matchesChannel && matchesStage && matchesQuery
                }
                UiState.Success(filtered)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val customersState: StateFlow<UiState<List<Customer>>> = repository.observeCustomers(currentUserId)
        .unwrapFirestoreError("Unable to sync customers")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val availableServicesState: StateFlow<UiState<List<ServiceListing>>> = repository.observeAvailableServices(currentUserId)
        .unwrapFirestoreError("Unable to load Kabarak services")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val filteredServicesState: StateFlow<UiState<List<ServiceListing>>> = combine(
        availableServicesState,
        _serviceSearch,
        _serviceCategoryFilter,
        _serviceZoneFilter
    ) { state, query, category, zone ->
        when (state) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> state
            is UiState.Success -> {
                val q = query.trim().lowercase()
                UiState.Success(
                    state.data.filter { svc ->
                        (category == null || svc.categoryEnum == category) &&
                            (zone == null || svc.zoneEnum == zone) &&
                            (q.isEmpty() ||
                                svc.title.lowercase().contains(q) ||
                                svc.providerName.lowercase().contains(q) ||
                                svc.description.lowercase().contains(q) ||
                                svc.zoneEnum.label.lowercase().contains(q))
                    }
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val myServicesState: StateFlow<UiState<List<ServiceListing>>> = repository.observeMyServices(currentUserId)
        .unwrapFirestoreError("Unable to load your service listings")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val incomingMarketplaceOrdersState: StateFlow<UiState<List<MarketplaceOrder>>> =
        repository.observeIncomingMarketplaceOrders(currentUserId)
            .unwrapFirestoreError("Unable to load incoming service orders")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val myPlacedOrdersState: StateFlow<UiState<List<MarketplaceOrder>>> =
        repository.observeMyPlacedOrders(currentUserId)
            .unwrapFirestoreError("Unable to load your placed orders")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val notificationsState: StateFlow<List<AppNotification>> =
        repository.observeNotifications(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    fun observeOrderEvents(orderId: String): Flow<UiState<List<OrderEvent>>> =
        repository.observeOrderEvents(currentUserId, orderId)
            .unwrapFirestoreError("Unable to load timeline")

    fun observeMarketplaceOrder(orderId: String): Flow<UiState<MarketplaceOrder?>> =
        repository.observeMarketplaceOrder(currentUserId, orderId)
            .unwrapFirestoreError("Unable to load order details")

    fun observeMarketplaceEvents(order: MarketplaceOrder): Flow<UiState<List<MarketplaceOrderEvent>>> =
        repository.observeMarketplaceEvents(currentUserId, order)
            .unwrapFirestoreError("Unable to load order timeline")

    fun observeOrderMessages(order: MarketplaceOrder): Flow<UiState<List<OrderMessage>>> =
        repository.observeOrderMessages(currentUserId, order)
            .unwrapFirestoreError("Unable to load order chat")

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setChannelFilter(channel: OrderChannel?) { _channelFilter.value = channel }
    fun setStageFilter(stage: StageFilter) { _stageFilter.value = stage }
    fun setServiceSearch(query: String) { _serviceSearch.value = query }
    fun setServiceCategoryFilter(cat: ServiceCategory?) { _serviceCategoryFilter.value = cat }
    fun setServiceZoneFilter(zone: KabarakZone?) { _serviceZoneFilter.value = zone }

    fun notifyMessage(message: String) { _snackbarEvents.tryEmit(message) }

    // --- SocialOrder Actions ---

    fun createOrder(
        orderCode: String,
        customerId: String?,
        customerName: String,
        channel: OrderChannel,
        handleOrPhone: String,
        shippingAddress: String?,
        itemsSummary: String,
        itemCount: Long,
        totalCents: Long,
        currencySymbol: String,
        paymentMethod: PaymentMethod,
        paymentStatus: PaymentStatus,
        stage: OrderStage,
        courierName: String?,
        trackingNumber: String?,
        notes: String?,
        dmMessageSnapshot: String?,
        saveToCustomerDirectory: Boolean,
        onSuccess: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.createOrder(
                userId = currentUserId,
                orderCode = orderCode,
                customerId = customerId,
                customerName = customerName,
                channel = channel,
                handleOrPhone = handleOrPhone,
                shippingAddress = shippingAddress,
                itemsSummary = itemsSummary,
                itemCount = itemCount,
                totalCents = totalCents,
                currencySymbol = currencySymbol,
                paymentMethod = paymentMethod,
                paymentStatus = paymentStatus,
                stage = stage,
                courierName = courierName,
                trackingNumber = trackingNumber,
                notes = notes,
                dmMessageSnapshot = dmMessageSnapshot,
                saveToCustomerDirectory = saveToCustomerDirectory
            )
            _isMutating.value = false
            result.onSuccess { newOrderId ->
                _snackbarEvents.tryEmit("Order $orderCode logged")
                onSuccess(newOrderId)
            }.onFailure { e ->
                _snackbarEvents.tryEmit(e.message ?: "Could not save order")
            }
        }
    }

    fun advanceOrderStage(
        order: SocialOrder,
        targetStage: OrderStage,
        courierName: String? = order.courierName,
        trackingNumber: String? = order.trackingNumber,
        paymentStatus: PaymentStatus = order.paymentStatusEnum,
        eventNote: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.advanceOrderStage(
                userId = currentUserId,
                order = order,
                targetStage = targetStage,
                courierName = courierName,
                trackingNumber = trackingNumber,
                paymentStatus = paymentStatus,
                eventNote = eventNote
            )
            _isMutating.value = false
            result.onSuccess {
                _snackbarEvents.tryEmit("${order.orderCode} moved to ${targetStage.label}")
                onSuccess()
            }.onFailure { e ->
                _snackbarEvents.tryEmit(e.message ?: "Failed to update stage")
            }
        }
    }

    fun updateOrderPayment(order: SocialOrder, paymentStatus: PaymentStatus, paymentMethod: PaymentMethod) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.updateOrderPayment(currentUserId, order, paymentStatus, paymentMethod)
            _isMutating.value = false
            result.onSuccess {
                _snackbarEvents.tryEmit("Payment marked ${paymentStatus.label}")
            }.onFailure { e ->
                _snackbarEvents.tryEmit(e.message ?: "Failed to update payment")
            }
        }
    }

    fun updateOrderDetails(
        order: SocialOrder,
        customerName: String,
        channel: OrderChannel,
        handleOrPhone: String,
        shippingAddress: String?,
        itemsSummary: String,
        itemCount: Long,
        totalCents: Long,
        paymentMethod: PaymentMethod,
        paymentStatus: PaymentStatus,
        stage: OrderStage,
        courierName: String?,
        trackingNumber: String?,
        notes: String?,
        dmMessageSnapshot: String?,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.updateOrderDetails(
                userId = currentUserId,
                order = order,
                customerName = customerName,
                channel = channel,
                handleOrPhone = handleOrPhone,
                shippingAddress = shippingAddress,
                itemsSummary = itemsSummary,
                itemCount = itemCount,
                totalCents = totalCents,
                paymentMethod = paymentMethod,
                paymentStatus = paymentStatus,
                stage = stage,
                courierName = courierName,
                trackingNumber = trackingNumber,
                notes = notes,
                dmMessageSnapshot = dmMessageSnapshot
            )
            _isMutating.value = false
            result.onSuccess {
                _snackbarEvents.tryEmit("Updated ${order.orderCode}")
                onSuccess()
            }.onFailure { e ->
                _snackbarEvents.tryEmit(e.message ?: "Failed to update order")
            }
        }
    }

    fun deleteOrder(orderId: String, orderCode: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.deleteOrder(currentUserId, orderId)
            _isMutating.value = false
            result.onSuccess {
                _snackbarEvents.tryEmit("Deleted $orderCode")
                onSuccess()
            }.onFailure { e ->
                _snackbarEvents.tryEmit(e.message ?: "Failed to delete order")
            }
        }
    }

    fun saveCustomer(
        existingCustomerId: String?,
        displayName: String,
        channel: OrderChannel,
        handleOrPhone: String,
        email: String?,
        shippingAddress: String?,
        notes: String?,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.saveCustomer(
                userId = currentUserId,
                existingCustomerId = existingCustomerId,
                displayName = displayName,
                channel = channel,
                handleOrPhone = handleOrPhone,
                email = email,
                shippingAddress = shippingAddress,
                notes = notes
            )
            _isMutating.value = false
            result.onSuccess {
                _snackbarEvents.tryEmit(
                    if (existingCustomerId == null) "Saved customer $displayName" else "Updated $displayName"
                )
                onSuccess()
            }.onFailure { e ->
                _snackbarEvents.tryEmit(e.message ?: "Failed to save customer")
            }
        }
    }

    fun deleteCustomer(customerId: String, name: String) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.deleteCustomer(currentUserId, customerId)
            _isMutating.value = false
            result.onSuccess {
                _snackbarEvents.tryEmit("Removed $name")
            }.onFailure { e ->
                _snackbarEvents.tryEmit(e.message ?: "Failed to delete customer")
            }
        }
    }

    fun saveSellerProfile(
        storeName: String,
        currencySymbol: String,
        defaultGreeting: String?,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.saveSellerProfile(currentUserId, storeName, currencySymbol, defaultGreeting)
            _isMutating.value = false
            result.onSuccess {
                _snackbarEvents.tryEmit("Store preferences saved")
                onSuccess()
            }.onFailure { e ->
                _snackbarEvents.tryEmit(e.message ?: "Failed to save preferences")
            }
        }
    }

    // --- Kabarak Services, Interactive Marketplace Orders & Notifications Actions ---

    fun saveServiceListing(
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
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.saveServiceListing(
                userId = currentUserId,
                existingServiceId = existingServiceId,
                providerName = providerName,
                title = title,
                category = category,
                zone = zone,
                description = description,
                priceCents = priceCents,
                currencySymbol = currencySymbol,
                unitLabel = unitLabel,
                etaLabel = etaLabel,
                contactChannel = contactChannel,
                contactHandle = contactHandle,
                available = available
            )
            _isMutating.value = false
            result.onSuccess {
                _snackbarEvents.tryEmit(if (existingServiceId == null) "Published service \"$title\"" else "Updated \"$title\"")
                onSuccess()
            }.onFailure { e ->
                _snackbarEvents.tryEmit(e.message ?: "Could not publish service")
            }
        }
    }

    fun toggleServiceAvailability(service: ServiceListing) {
        viewModelScope.launch {
            val result = repository.toggleServiceAvailability(currentUserId, service)
            result.onSuccess {
                _snackbarEvents.tryEmit(
                    if (!service.available) "${service.title} is now Live" else "${service.title} paused"
                )
            }.onFailure { e ->
                _snackbarEvents.tryEmit(e.message ?: "Could not update availability")
            }
        }
    }

    fun deleteServiceListing(service: ServiceListing) {
        viewModelScope.launch {
            val result = repository.deleteServiceListing(currentUserId, service.id)
            result.onSuccess {
                _snackbarEvents.tryEmit("Deleted ${service.title}")
            }.onFailure { e ->
                _snackbarEvents.tryEmit(e.message ?: "Could not delete service")
            }
        }
    }

    fun placeMarketplaceOrder(
        service: ServiceListing,
        orderCode: String,
        buyerName: String,
        buyerHandleOrPhone: String,
        deliveryZone: KabarakZone,
        deliveryDetails: String,
        quantity: Long,
        paymentMethod: PaymentMethod,
        notes: String?,
        onSuccess: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.placeMarketplaceOrder(
                userId = currentUserId,
                service = service,
                orderCode = orderCode,
                buyerName = buyerName,
                buyerHandleOrPhone = buyerHandleOrPhone,
                deliveryZone = deliveryZone,
                deliveryDetails = deliveryDetails,
                quantity = quantity,
                paymentMethod = paymentMethod,
                notes = notes
            )
            _isMutating.value = false
            result.onSuccess { newOrderId ->
                _snackbarEvents.tryEmit("Order $orderCode placed! Provider notified.")
                onSuccess(newOrderId)
            }.onFailure { e ->
                _snackbarEvents.tryEmit(e.message ?: "Failed to place order")
            }
        }
    }

    fun advanceMarketplaceOrderStage(
        order: MarketplaceOrder,
        targetStage: OrderStage,
        courierName: String? = order.courierName,
        trackingNumber: String? = order.trackingNumber,
        paymentStatus: PaymentStatus = order.paymentStatusEnum,
        note: String? = null
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.advanceMarketplaceOrderStage(
                userId = currentUserId,
                order = order,
                targetStage = targetStage,
                courierName = courierName,
                trackingNumber = trackingNumber,
                paymentStatus = paymentStatus,
                note = note
            )
            _isMutating.value = false
            result.onSuccess {
                _snackbarEvents.tryEmit("${order.orderCode} updated to ${targetStage.label}")
            }.onFailure { e ->
                _snackbarEvents.tryEmit(e.message ?: "Failed to update order")
            }
        }
    }

    fun updateMarketplaceOrderPayment(
        order: MarketplaceOrder,
        paymentStatus: PaymentStatus,
        paymentMethod: PaymentMethod
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.updateMarketplaceOrderPayment(currentUserId, order, paymentStatus, paymentMethod)
            _isMutating.value = false
            result.onSuccess {
                _snackbarEvents.tryEmit("Payment marked ${paymentStatus.label}")
            }.onFailure { e ->
                _snackbarEvents.tryEmit(e.message ?: "Failed to update payment")
            }
        }
    }

    fun sendOrderMessage(
        order: MarketplaceOrder,
        senderName: String,
        asSeller: Boolean,
        text: String,
        onSent: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.sendOrderMessage(currentUserId, order, senderName, asSeller, text)
            result.onSuccess {
                onSent()
            }.onFailure { e ->
                _snackbarEvents.tryEmit(e.message ?: "Could not send message")
            }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(currentUserId, notificationId)
        }
    }

    fun markAllNotificationsRead(unreadIds: List<String>) {
        viewModelScope.launch {
            repository.markAllNotificationsRead(currentUserId, unreadIds)
        }
    }

    // --- Cart & Order Entry Actions ---

    fun selectCartCategory(category: ServiceCategory) {
        _cartSelectedCategory.value = category
    }

    fun addServiceToCart(service: ServiceListing, quantity: Long = 1L) {
        if (quantity <= 0L) return
        val key = "svc_${service.id}"
        val current = _cartItems.value.toMutableList()
        val idx = current.indexOfFirst { it.id == key }
        if (idx >= 0) {
            val existing = current[idx]
            current[idx] = existing.copy(quantity = (existing.quantity + quantity).coerceAtMost(999L))
        } else {
            current.add(
                CartEntryItem(
                    id = key,
                    title = service.title,
                    category = service.categoryEnum,
                    unitPriceCents = service.priceCents,
                    currencySymbol = service.currencySymbol,
                    unitLabel = service.unitLabel,
                    quantity = quantity.coerceAtMost(999L),
                    serviceListing = service
                )
            )
        }
        _cartItems.value = current
    }

    fun addCustomItemToCart(
        title: String,
        category: ServiceCategory,
        unitPriceCents: Long,
        currencySymbol: String,
        unitLabel: String = "item",
        quantity: Long = 1L
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty() || quantity <= 0L || unitPriceCents < 0L) return
        val item = CartEntryItem(
            id = "custom_${System.nanoTime()}",
            title = cleanTitle,
            category = category,
            unitPriceCents = unitPriceCents,
            currencySymbol = currencySymbol,
            unitLabel = unitLabel.trim().ifEmpty { "item" },
            quantity = quantity.coerceAtMost(999L),
            serviceListing = null
        )
        _cartItems.value = _cartItems.value + item
    }

    fun updateCartItemQuantity(itemId: String, newQuantity: Long) {
        if (newQuantity <= 0L) {
            removeCartItem(itemId)
            return
        }
        _cartItems.value = _cartItems.value.map { item ->
            if (item.id == itemId) item.copy(quantity = newQuantity.coerceAtMost(999L)) else item
        }
    }

    fun removeCartItem(itemId: String) {
        _cartItems.value = _cartItems.value.filterNot { it.id == itemId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun submitCartOrder(
        buyerName: String,
        buyerPhoneOrHandle: String,
        channel: OrderChannel,
        deliveryZone: KabarakZone,
        deliveryDetails: String,
        paymentMethod: PaymentMethod,
        currencySymbol: String,
        notes: String?,
        onSuccess: (destinationRoute: String) -> Unit
    ) {
        val itemsSnapshot = _cartItems.value
        if (itemsSnapshot.isEmpty()) {
            _snackbarEvents.tryEmit("Add at least one item to your cart before submitting.")
            return
        }

        viewModelScope.launch {
            _isMutating.value = true
            val marketplaceItems = itemsSnapshot.filter { it.serviceListing != null }
            val customItems = itemsSnapshot.filter { it.serviceListing == null }

            val createdMarketplaceIds = mutableListOf<String>()
            var createdSocialId: String? = null
            var failureMessage: String? = null

            for (entry in marketplaceItems) {
                val service = entry.serviceListing ?: continue
                val code = generateOrderCode("KB")
                val res = repository.placeMarketplaceOrder(
                    userId = currentUserId,
                    service = service,
                    orderCode = code,
                    buyerName = buyerName,
                    buyerHandleOrPhone = buyerPhoneOrHandle,
                    deliveryZone = deliveryZone,
                    deliveryDetails = deliveryDetails,
                    quantity = entry.quantity,
                    paymentMethod = paymentMethod,
                    notes = notes
                )
                res.onSuccess { id ->
                    createdMarketplaceIds.add(id)
                }.onFailure { e ->
                    failureMessage = e.message ?: "Failed to submit ${entry.title}"
                }
                if (failureMessage != null) break
            }

            if (failureMessage == null && customItems.isNotEmpty()) {
                val code = generateOrderCode("KB")
                val itemsSummary = customItems.joinToString(", ") {
                    "${it.quantity}x ${it.title} [${it.category.label}]"
                }.take(300)
                val totalQty = customItems.sumOf { it.quantity }.coerceIn(1L, 10000L)
                val totalCents = customItems.sumOf { it.lineTotalCents }.coerceAtMost(1_000_000_000L)
                val paymentStatus = if (paymentMethod == PaymentMethod.COD) PaymentStatus.COD_DUE else PaymentStatus.PENDING
                val address = "${deliveryZone.label}: ${deliveryDetails.trim()}".take(250)

                val res = repository.createOrder(
                    userId = currentUserId,
                    orderCode = code,
                    customerId = null,
                    customerName = buyerName,
                    channel = channel,
                    handleOrPhone = buyerPhoneOrHandle,
                    shippingAddress = address,
                    itemsSummary = itemsSummary,
                    itemCount = totalQty,
                    totalCents = totalCents,
                    currencySymbol = currencySymbol,
                    paymentMethod = paymentMethod,
                    paymentStatus = paymentStatus,
                    stage = OrderStage.NEW_DM,
                    courierName = null,
                    trackingNumber = null,
                    notes = notes,
                    dmMessageSnapshot = null,
                    saveToCustomerDirectory = false
                )
                res.onSuccess { id ->
                    createdSocialId = id
                }.onFailure { e ->
                    failureMessage = e.message ?: "Failed to submit custom cart items"
                }
            }

            _isMutating.value = false

            if (failureMessage != null) {
                _snackbarEvents.tryEmit(failureMessage!!)
            } else {
                _cartItems.value = emptyList()
                val totalOrders = createdMarketplaceIds.size + if (createdSocialId != null) 1 else 0
                _snackbarEvents.tryEmit(
                    if (createdMarketplaceIds.isNotEmpty()) {
                        "Order submitted to Firestore! Provider notified."
                    } else {
                        "Order submitted to Firestore ($totalOrders order logged)."
                    }
                )
                val targetRoute = when {
                    createdMarketplaceIds.size == 1 && createdSocialId == null ->
                        "marketplace_order/${createdMarketplaceIds.first()}"
                    createdMarketplaceIds.isEmpty() && createdSocialId != null ->
                        "order_detail/$createdSocialId"
                    else -> "pipeline"
                }
                onSuccess(targetRoute)
            }
        }
    }

    private companion object {
        const val TAG = "OrdersViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
