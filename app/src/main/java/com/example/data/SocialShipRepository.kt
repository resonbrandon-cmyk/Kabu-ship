package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AppNotification
import com.example.model.Customer
import com.example.model.KabarakZone
import com.example.model.MarketplaceOrder
import com.example.model.MarketplaceOrderEvent
import com.example.model.NotificationType
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
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SocialShipRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireMatchingUserId(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user access." }
        return uid
    }

    private fun handleStreamError(error: Throwable, uid: String, op: OperationType, path: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }

    // --- Seller Profile & Private Orders ---

    fun observeSellerProfile(userId: String): Flow<SellerProfile?> = flow {
        val uid = requireMatchingUserId(userId)
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            SellerProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.GET, path)
                    throw error
                }
        )
    }

    fun observeOrders(userId: String): Flow<List<SocialOrder>> = flow {
        val uid = requireMatchingUserId(userId)
        val path = "users/$uid/orders"
        emitAll(
            db.collection("users").document(uid).collection("orders")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SocialOrder::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeCustomers(userId: String): Flow<List<Customer>> = flow {
        val uid = requireMatchingUserId(userId)
        val path = "users/$uid/customers"
        emitAll(
            db.collection("users").document(uid).collection("customers")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Customer::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeOrderEvents(userId: String, orderId: String): Flow<List<OrderEvent>> = flow {
        val uid = requireMatchingUserId(userId)
        require(orderId.isNotBlank()) { "Order ID must not be blank" }
        val path = "users/$uid/orders/$orderId/events"
        emitAll(
            db.collection("users").document(uid)
                .collection("orders").document(orderId)
                .collection("events")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        OrderEvent::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    // --- Kabarak Marketplace Services, Interactive Orders & Notifications ---

    fun observeAvailableServices(userId: String): Flow<List<ServiceListing>> = flow {
        val uid = requireMatchingUserId(userId)
        val path = "services"
        emitAll(
            db.collection("services")
                .whereEqualTo("available", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ServiceListing::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeMyServices(userId: String): Flow<List<ServiceListing>> = flow {
        val uid = requireMatchingUserId(userId)
        val path = "services"
        emitAll(
            db.collection("services")
                .whereEqualTo("ownerUid", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ServiceListing::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeIncomingMarketplaceOrders(userId: String): Flow<List<MarketplaceOrder>> = flow {
        val uid = requireMatchingUserId(userId)
        val path = "marketplace_orders"
        emitAll(
            db.collection("marketplace_orders")
                .whereEqualTo("sellerUid", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MarketplaceOrder::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeMyPlacedOrders(userId: String): Flow<List<MarketplaceOrder>> = flow {
        val uid = requireMatchingUserId(userId)
        val path = "marketplace_orders"
        emitAll(
            db.collection("marketplace_orders")
                .whereEqualTo("buyerUid", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MarketplaceOrder::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeMarketplaceOrder(userId: String, orderId: String): Flow<MarketplaceOrder?> = flow {
        val uid = requireMatchingUserId(userId)
        require(orderId.isNotBlank()) { "Order ID must not be blank" }
        val path = "marketplace_orders/$orderId"
        emitAll(
            db.collection("marketplace_orders").document(orderId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            MarketplaceOrder::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.GET, path)
                    throw error
                }
        )
    }

    fun observeMarketplaceEvents(userId: String, order: MarketplaceOrder): Flow<List<MarketplaceOrderEvent>> = flow {
        val uid = requireMatchingUserId(userId)
        val roleField = if (order.sellerUid == uid) "sellerUid" else "buyerUid"
        val path = "marketplace_orders/${order.id}/events"
        emitAll(
            db.collection("marketplace_orders").document(order.id).collection("events")
                .whereEqualTo(roleField, uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MarketplaceOrderEvent::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeOrderMessages(userId: String, order: MarketplaceOrder): Flow<List<OrderMessage>> = flow {
        val uid = requireMatchingUserId(userId)
        val roleField = if (order.sellerUid == uid) "sellerUid" else "buyerUid"
        val path = "marketplace_orders/${order.id}/messages"
        emitAll(
            db.collection("marketplace_orders").document(order.id).collection("messages")
                .whereEqualTo(roleField, uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        OrderMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeNotifications(userId: String): Flow<List<AppNotification>> = flow {
        val uid = requireMatchingUserId(userId)
        val path = "users/$uid/notifications"
        emitAll(
            db.collection("users").document(uid).collection("notifications")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AppNotification::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    // --- Mutations: Seller Profile & Private SocialOrders ---

    suspend fun saveSellerProfile(
        userId: String,
        storeName: String,
        currencySymbol: String,
        defaultGreeting: String?
    ): Result<Unit> {
        var docPath = "users/$userId"
        return try {
            val uid = requireMatchingUserId(userId)
            val cleanStore = storeName.trim()
            val cleanCurrency = currencySymbol.trim()
            val cleanGreeting = defaultGreeting?.trim()?.takeIf { it.isNotEmpty() }
            require(cleanStore.length in 1..60) { "Store name must be 1 to 60 characters" }
            require(cleanCurrency.length in 1..5) { "Currency symbol must be 1 to 5 characters" }
            cleanGreeting?.let { require(it.length <= 200) { "Greeting must be 200 characters or fewer" } }

            val profileRef = db.collection("users").document(uid)
            docPath = profileRef.path
            val existingSnap = profileRef.get().await()
            if (existingSnap.exists()) {
                val updates = mapOf(
                    "storeName" to cleanStore,
                    "currencySymbol" to cleanCurrency,
                    "defaultGreeting" to (cleanGreeting ?: FieldValue.delete()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                profileRef.update(updates).await()
            } else {
                val payload = buildMap<String, Any> {
                    put("userId", uid)
                    put("storeName", cleanStore)
                    put("currencySymbol", cleanCurrency)
                    if (cleanGreeting != null) put("defaultGreeting", cleanGreeting)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                profileRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createOrder(
        userId: String,
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
        saveToCustomerDirectory: Boolean
    ): Result<String> {
        var docPath = "users/$userId/orders"
        return try {
            val uid = requireMatchingUserId(userId)
            val cleanCode = orderCode.trim()
            val cleanCustomerName = customerName.trim()
            val cleanHandle = handleOrPhone.trim()
            val cleanItems = itemsSummary.trim()
            val cleanCurrency = currencySymbol.trim().ifEmpty { "KSh " }
            val cleanAddress = shippingAddress?.trim()?.takeIf { it.isNotEmpty() }
            val cleanCourier = courierName?.trim()?.takeIf { it.isNotEmpty() }
            val cleanTracking = trackingNumber?.trim()?.takeIf { it.isNotEmpty() }
            val cleanNotes = notes?.trim()?.takeIf { it.isNotEmpty() }
            val cleanDm = dmMessageSnapshot?.trim()?.takeIf { it.isNotEmpty() }

            require(cleanCode.length in 2..32) { "Order code must be 2 to 32 characters" }
            require(cleanCustomerName.length in 1..80) { "Customer name must be 1 to 80 characters" }
            require(cleanHandle.length in 1..60) { "Handle or phone must be 1 to 60 characters" }
            require(cleanItems.length in 1..300) { "Items summary must be 1 to 300 characters" }
            require(itemCount in 1L..10000L) { "Item count must be between 1 and 10,000" }
            require(totalCents in 0L..1_000_000_000L) { "Order total is out of allowed range" }
            require(cleanCurrency.length in 1..5) { "Currency symbol must be 1 to 5 characters" }

            val userRef = db.collection("users").document(uid)
            val orderRef = userRef.collection("orders").document()
            docPath = orderRef.path
            val eventRef = orderRef.collection("events").document()

            val batch = db.batch()
            var resolvedCustomerId = customerId?.trim()?.takeIf { it.isNotEmpty() }

            if (resolvedCustomerId != null) {
                val custRef = userRef.collection("customers").document(resolvedCustomerId)
                batch.update(
                    custRef,
                    mapOf(
                        "orderCount" to FieldValue.increment(1L),
                        "totalSpentCents" to FieldValue.increment(totalCents),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else if (saveToCustomerDirectory) {
                val newCustRef = userRef.collection("customers").document()
                resolvedCustomerId = newCustRef.id
                val customerPayload = mapOf(
                    "userId" to uid,
                    "displayName" to cleanCustomerName,
                    "channel" to channel.name,
                    "handleOrPhone" to cleanHandle,
                    "shippingAddress" to cleanAddress,
                    "orderCount" to 1L,
                    "totalSpentCents" to totalCents,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                batch.set(newCustRef, customerPayload)
            }

            val orderPayload = mapOf(
                "userId" to uid,
                "orderCode" to cleanCode,
                "customerId" to resolvedCustomerId,
                "customerName" to cleanCustomerName,
                "channel" to channel.name,
                "handleOrPhone" to cleanHandle,
                "shippingAddress" to cleanAddress,
                "itemsSummary" to cleanItems,
                "itemCount" to itemCount,
                "totalCents" to totalCents,
                "currencySymbol" to cleanCurrency,
                "paymentMethod" to paymentMethod.name,
                "paymentStatus" to paymentStatus.name,
                "stage" to stage.name,
                "courierName" to cleanCourier,
                "trackingNumber" to cleanTracking,
                "notes" to cleanNotes,
                "dmMessageSnapshot" to cleanDm,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            val eventPayload = mapOf(
                "userId" to uid,
                "orderId" to orderRef.id,
                "stage" to stage.name,
                "title" to "Order logged from ${channel.label}",
                "note" to buildString {
                    append("$cleanCode · ${paymentStatus.label}")
                    if (cleanTracking != null) append(" · Tracking: $cleanTracking")
                }.take(300),
                "createdAt" to FieldValue.serverTimestamp()
            )

            batch.set(orderRef, orderPayload)
            batch.set(eventRef, eventPayload)
            batch.commit().await()
            Result.success(orderRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun advanceOrderStage(
        userId: String,
        order: SocialOrder,
        targetStage: OrderStage,
        courierName: String? = order.courierName,
        trackingNumber: String? = order.trackingNumber,
        paymentStatus: PaymentStatus = order.paymentStatusEnum,
        eventNote: String? = null
    ): Result<Unit> {
        var docPath = "users/$userId/orders/${order.id}"
        return try {
            val uid = requireMatchingUserId(userId)
            require(order.id.isNotBlank()) { "Order ID must not be blank" }
            require(!order.stageEnum.isTerminal) {
                "Finalized orders (${order.stageEnum.label}) cannot be modified."
            }

            val cleanCourier = courierName?.trim()?.takeIf { it.isNotEmpty() }
            val cleanTracking = trackingNumber?.trim()?.takeIf { it.isNotEmpty() }
            val cleanNote = eventNote?.trim()?.takeIf { it.isNotEmpty() }

            val orderRef = db.collection("users").document(uid)
                .collection("orders").document(order.id)
            docPath = orderRef.path
            val eventRef = orderRef.collection("events").document()

            val orderUpdates = buildMap<String, Any> {
                put("stage", targetStage.name)
                put("paymentStatus", paymentStatus.name)
                if (cleanCourier != null) put("courierName", cleanCourier)
                if (cleanTracking != null) put("trackingNumber", cleanTracking)
                put("updatedAt", FieldValue.serverTimestamp())
            }

            val title = when (targetStage) {
                OrderStage.NEW_DM -> "Order logged"
                OrderStage.PAID -> "Order accepted / paid"
                OrderStage.PACKING -> "Preparing order"
                OrderStage.SHIPPED -> cleanCourier?.let { "Dispatched via $it" } ?: "Dispatched"
                OrderStage.OUT_FOR_DELIVERY -> "Out for delivery"
                OrderStage.DELIVERED -> "Delivered to customer"
                OrderStage.CANCELLED -> "Order cancelled"
            }

            val eventPayload = mapOf(
                "userId" to uid,
                "orderId" to order.id,
                "stage" to targetStage.name,
                "title" to title.take(100),
                "note" to (cleanNote ?: if (targetStage == OrderStage.SHIPPED && cleanTracking != null) "Tracking: $cleanTracking" else null)?.take(300),
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            val batch = db.batch()
            batch.update(orderRef, orderUpdates)
            batch.set(eventRef, eventPayload)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateOrderPayment(
        userId: String,
        order: SocialOrder,
        paymentStatus: PaymentStatus,
        paymentMethod: PaymentMethod
    ): Result<Unit> {
        var docPath = "users/$userId/orders/${order.id}"
        return try {
            val uid = requireMatchingUserId(userId)
            require(order.id.isNotBlank()) { "Order ID must not be blank" }
            require(order.stageEnum != OrderStage.CANCELLED) { "Cancelled orders cannot change payment" }

            val orderRef = db.collection("users").document(uid)
                .collection("orders").document(order.id)
            docPath = orderRef.path
            val eventRef = orderRef.collection("events").document()

            val batch = db.batch()
            batch.update(
                orderRef,
                mapOf(
                    "paymentStatus" to paymentStatus.name,
                    "paymentMethod" to paymentMethod.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                eventRef,
                mapOf(
                    "userId" to uid,
                    "orderId" to order.id,
                    "stage" to order.stage,
                    "title" to "Payment updated: ${paymentStatus.label}",
                    "note" to "Method: ${paymentMethod.label}",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateOrderDetails(
        userId: String,
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
        dmMessageSnapshot: String?
    ): Result<Unit> {
        var docPath = "users/$userId/orders/${order.id}"
        return try {
            val uid = requireMatchingUserId(userId)
            require(order.id.isNotBlank()) { "Order ID must not be blank" }
            require(!order.stageEnum.isTerminal) { "Finalized orders cannot be edited" }

            val cleanCustomer = customerName.trim()
            val cleanHandle = handleOrPhone.trim()
            val cleanItems = itemsSummary.trim()
            val cleanAddress = shippingAddress?.trim()?.takeIf { it.isNotEmpty() }
            val cleanCourier = courierName?.trim()?.takeIf { it.isNotEmpty() }
            val cleanTracking = trackingNumber?.trim()?.takeIf { it.isNotEmpty() }
            val cleanNotes = notes?.trim()?.takeIf { it.isNotEmpty() }
            val cleanDm = dmMessageSnapshot?.trim()?.takeIf { it.isNotEmpty() }

            val orderRef = db.collection("users").document(uid)
                .collection("orders").document(order.id)
            docPath = orderRef.path

            val updates = mapOf(
                "customerName" to cleanCustomer,
                "channel" to channel.name,
                "handleOrPhone" to cleanHandle,
                "shippingAddress" to (cleanAddress ?: FieldValue.delete()),
                "itemsSummary" to cleanItems,
                "itemCount" to itemCount,
                "totalCents" to totalCents,
                "paymentMethod" to paymentMethod.name,
                "paymentStatus" to paymentStatus.name,
                "stage" to stage.name,
                "courierName" to (cleanCourier ?: FieldValue.delete()),
                "trackingNumber" to (cleanTracking ?: FieldValue.delete()),
                "notes" to (cleanNotes ?: FieldValue.delete()),
                "dmMessageSnapshot" to (cleanDm ?: FieldValue.delete()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            orderRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteOrder(userId: String, orderId: String): Result<Unit> {
        var docPath = "users/$userId/orders/$orderId"
        return try {
            val uid = requireMatchingUserId(userId)
            require(orderId.isNotBlank()) { "Order ID must not be blank" }
            val orderRef = db.collection("users").document(uid)
                .collection("orders").document(orderId)
            docPath = orderRef.path
            val eventsSnap = orderRef.collection("events").get().await()
            val batch = db.batch()
            for (eventDoc in eventsSnap.documents) {
                batch.delete(eventDoc.reference)
            }
            batch.delete(orderRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveCustomer(
        userId: String,
        existingCustomerId: String?,
        displayName: String,
        channel: OrderChannel,
        handleOrPhone: String,
        email: String?,
        shippingAddress: String?,
        notes: String?
    ): Result<String> {
        var docPath = "users/$userId/customers"
        return try {
            val uid = requireMatchingUserId(userId)
            val cleanName = displayName.trim()
            val cleanHandle = handleOrPhone.trim()
            val cleanEmail = email?.trim()?.takeIf { it.isNotEmpty() }
            val cleanAddress = shippingAddress?.trim()?.takeIf { it.isNotEmpty() }
            val cleanNotes = notes?.trim()?.takeIf { it.isNotEmpty() }

            require(cleanName.length in 1..80) { "Customer name must be 1 to 80 characters" }
            require(cleanHandle.length in 1..60) { "Handle or phone must be 1 to 60 characters" }

            val colRef = db.collection("users").document(uid).collection("customers")
            if (existingCustomerId.isNullOrBlank()) {
                val newRef = colRef.document()
                docPath = newRef.path
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "channel" to channel.name,
                    "handleOrPhone" to cleanHandle,
                    "email" to cleanEmail,
                    "shippingAddress" to cleanAddress,
                    "notes" to cleanNotes,
                    "orderCount" to 0L,
                    "totalSpentCents" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                newRef.set(payload).await()
                Result.success(newRef.id)
            } else {
                val docRef = colRef.document(existingCustomerId)
                docPath = docRef.path
                val updates = mapOf(
                    "displayName" to cleanName,
                    "channel" to channel.name,
                    "handleOrPhone" to cleanHandle,
                    "email" to (cleanEmail ?: FieldValue.delete()),
                    "shippingAddress" to (cleanAddress ?: FieldValue.delete()),
                    "notes" to (cleanNotes ?: FieldValue.delete()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
                Result.success(existingCustomerId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomer(userId: String, customerId: String): Result<Unit> {
        var docPath = "users/$userId/customers/$customerId"
        return try {
            val uid = requireMatchingUserId(userId)
            require(customerId.isNotBlank()) { "Customer ID must not be blank" }
            val docRef = db.collection("users").document(uid)
                .collection("customers").document(customerId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- Mutations: Kabarak Marketplace Services, Interactive Orders & Notifications ---

    suspend fun saveServiceListing(
        userId: String,
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
        available: Boolean
    ): Result<String> {
        var docPath = "services"
        return try {
            val uid = requireMatchingUserId(userId)
            val cleanProvider = providerName.trim()
            val cleanTitle = title.trim()
            val cleanDesc = description.trim()
            val cleanCurrency = currencySymbol.trim().ifEmpty { "KSh " }
            val cleanUnit = unitLabel.trim()
            val cleanEta = etaLabel.trim()
            val cleanHandle = contactHandle.trim()

            require(cleanProvider.length in 1..80) { "Provider name must be 1 to 80 characters" }
            require(cleanTitle.length in 2..90) { "Service title must be 2 to 90 characters" }
            require(cleanDesc.length in 1..400) { "Description must be 1 to 400 characters" }
            require(priceCents in 0L..1_000_000_000L) { "Price out of range" }
            require(cleanUnit.length in 1..40) { "Unit label must be 1 to 40 characters" }
            require(cleanEta.length in 1..40) { "ETA label must be 1 to 40 characters" }
            require(cleanHandle.length in 1..60) { "Contact handle/phone must be 1 to 60 characters" }

            val colRef = db.collection("services")
            if (existingServiceId.isNullOrBlank()) {
                val docRef = colRef.document()
                docPath = docRef.path
                val payload = mapOf(
                    "ownerUid" to uid,
                    "providerName" to cleanProvider,
                    "title" to cleanTitle,
                    "category" to category.name,
                    "zone" to zone.name,
                    "description" to cleanDesc,
                    "priceCents" to priceCents,
                    "currencySymbol" to cleanCurrency,
                    "unitLabel" to cleanUnit,
                    "etaLabel" to cleanEta,
                    "contactChannel" to contactChannel.name,
                    "contactHandle" to cleanHandle,
                    "available" to available,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                Result.success(docRef.id)
            } else {
                val docRef = colRef.document(existingServiceId)
                docPath = docRef.path
                docRef.update(
                    mapOf(
                        "providerName" to cleanProvider,
                        "title" to cleanTitle,
                        "category" to category.name,
                        "zone" to zone.name,
                        "description" to cleanDesc,
                        "priceCents" to priceCents,
                        "currencySymbol" to cleanCurrency,
                        "unitLabel" to cleanUnit,
                        "etaLabel" to cleanEta,
                        "contactChannel" to contactChannel.name,
                        "contactHandle" to cleanHandle,
                        "available" to available,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(existingServiceId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleServiceAvailability(userId: String, service: ServiceListing): Result<Unit> {
        val docPath = "services/${service.id}"
        return try {
            requireMatchingUserId(userId)
            db.collection("services").document(service.id).update(
                mapOf(
                    "available" to !service.available,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteServiceListing(userId: String, serviceId: String): Result<Unit> {
        val docPath = "services/$serviceId"
        return try {
            requireMatchingUserId(userId)
            db.collection("services").document(serviceId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun placeMarketplaceOrder(
        userId: String,
        service: ServiceListing,
        orderCode: String,
        buyerName: String,
        buyerHandleOrPhone: String,
        deliveryZone: KabarakZone,
        deliveryDetails: String,
        quantity: Long,
        paymentMethod: PaymentMethod,
        notes: String?
    ): Result<String> {
        var docPath = "marketplace_orders"
        return try {
            val uid = requireMatchingUserId(userId)
            val cleanCode = orderCode.trim()
            val cleanBuyerName = buyerName.trim()
            val cleanBuyerHandle = buyerHandleOrPhone.trim()
            val cleanDetails = deliveryDetails.trim()
            val cleanNotes = notes?.trim()?.takeIf { it.isNotEmpty() }

            require(cleanCode.length in 2..32) { "Order code must be 2 to 32 characters" }
            require(cleanBuyerName.length in 1..80) { "Your name must be 1 to 80 characters" }
            require(cleanBuyerHandle.length in 1..60) { "Phone or @handle must be 1 to 60 characters" }
            require(cleanDetails.length in 1..250) { "Delivery location details are required (max 250 chars)" }
            require(quantity in 1L..1000L) { "Quantity must be between 1 and 1,000" }
            cleanNotes?.let { require(it.length <= 400) { "Notes must be 400 characters or fewer" } }

            val totalCents = (service.priceCents * quantity).coerceAtMost(1_000_000_000L)
            val paymentStatus = if (paymentMethod == PaymentMethod.COD) PaymentStatus.COD_DUE else PaymentStatus.PENDING

            val orderRef = db.collection("marketplace_orders").document()
            docPath = orderRef.path
            val eventRef = orderRef.collection("events").document()
            val notifRef = db.collection("users").document(service.ownerUid)
                .collection("notifications").document()

            val batch = db.batch()
            val preview = cleanNotes?.take(200)

            val orderPayload = mapOf(
                "orderCode" to cleanCode,
                "serviceId" to service.id,
                "serviceTitle" to service.title.take(90),
                "category" to service.category,
                "buyerUid" to uid,
                "buyerName" to cleanBuyerName,
                "buyerHandleOrPhone" to cleanBuyerHandle,
                "sellerUid" to service.ownerUid,
                "sellerName" to service.providerName.take(80),
                "sellerHandleOrPhone" to service.contactHandle.take(60),
                "deliveryZone" to deliveryZone.name,
                "deliveryDetails" to cleanDetails,
                "quantity" to quantity,
                "unitPriceCents" to service.priceCents,
                "totalCents" to totalCents,
                "currencySymbol" to service.currencySymbol,
                "paymentMethod" to paymentMethod.name,
                "paymentStatus" to paymentStatus.name,
                "stage" to OrderStage.NEW_DM.name,
                "notes" to cleanNotes,
                "lastMessagePreview" to preview,
                "unreadForSeller" to true,
                "unreadForBuyer" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            val eventPayload = mapOf(
                "orderId" to orderRef.id,
                "buyerUid" to uid,
                "sellerUid" to service.ownerUid,
                "actorUid" to uid,
                "stage" to OrderStage.NEW_DM.name,
                "title" to "Order placed by $cleanBuyerName".take(100),
                "note" to "${quantity}x ${service.title} · ${deliveryZone.label}: $cleanDetails".take(300),
                "createdAt" to FieldValue.serverTimestamp()
            )

            batch.set(orderRef, orderPayload)
            batch.set(eventRef, eventPayload)

            if (cleanNotes != null) {
                val msgRef = orderRef.collection("messages").document()
                batch.set(
                    msgRef,
                    mapOf(
                        "orderId" to orderRef.id,
                        "buyerUid" to uid,
                        "sellerUid" to service.ownerUid,
                        "senderUid" to uid,
                        "senderName" to cleanBuyerName,
                        "senderRole" to "BUYER",
                        "text" to cleanNotes.take(500),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            val notifPayload = mapOf(
                "recipientUid" to service.ownerUid,
                "senderUid" to uid,
                "orderId" to orderRef.id,
                "orderCode" to cleanCode,
                "title" to "New Order: ${service.title}".take(100),
                "body" to "$cleanBuyerName ordered ${quantity}x · ${deliveryZone.label}".take(240),
                "type" to NotificationType.NEW_ORDER.name,
                "read" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            batch.set(notifRef, notifPayload)

            batch.commit().await()
            Result.success(orderRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun advanceMarketplaceOrderStage(
        userId: String,
        order: MarketplaceOrder,
        targetStage: OrderStage,
        courierName: String? = order.courierName,
        trackingNumber: String? = order.trackingNumber,
        paymentStatus: PaymentStatus = order.paymentStatusEnum,
        note: String? = null
    ): Result<Unit> {
        val docPath = "marketplace_orders/${order.id}"
        return try {
            val uid = requireMatchingUserId(userId)
            require(!order.stageEnum.isTerminal) { "Finalized orders cannot be modified" }
            require(order.sellerUid == uid || (order.buyerUid == uid && order.stageEnum == OrderStage.NEW_DM && targetStage == OrderStage.CANCELLED)) {
                "Only the service provider can update order stages"
            }

            val cleanCourier = courierName?.trim()?.takeIf { it.isNotEmpty() }
            val cleanTracking = trackingNumber?.trim()?.takeIf { it.isNotEmpty() }
            val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() }

            val orderRef = db.collection("marketplace_orders").document(order.id)
            val eventRef = orderRef.collection("events").document()

            val batch = db.batch()
            if (order.buyerUid == uid && order.sellerUid != uid && targetStage == OrderStage.CANCELLED) {
                batch.update(
                    orderRef,
                    mapOf(
                        "stage" to OrderStage.CANCELLED.name,
                        "unreadForSeller" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                val updates = buildMap<String, Any> {
                    put("stage", targetStage.name)
                    put("paymentStatus", paymentStatus.name)
                    put("unreadForBuyer", true)
                    if (cleanCourier != null) put("courierName", cleanCourier)
                    if (cleanTracking != null) put("trackingNumber", cleanTracking)
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                batch.update(orderRef, updates)
            }

            val eventTitle = when (targetStage) {
                OrderStage.NEW_DM -> "Order placed"
                OrderStage.PAID -> "Order accepted by ${order.sellerName}"
                OrderStage.PACKING -> "Preparing ${order.serviceTitle}"
                OrderStage.SHIPPED -> cleanCourier?.let { "Dispatched with $it" } ?: "Dispatched for delivery"
                OrderStage.OUT_FOR_DELIVERY -> "Out for delivery to ${order.deliveryZoneEnum.label}"
                OrderStage.DELIVERED -> "Delivered to ${order.buyerName}"
                OrderStage.CANCELLED -> "Order cancelled"
            }

            val eventPayload = mapOf(
                "orderId" to order.id,
                "buyerUid" to order.buyerUid,
                "sellerUid" to order.sellerUid,
                "actorUid" to uid,
                "stage" to targetStage.name,
                "title" to eventTitle.take(100),
                "note" to (cleanNote ?: if (cleanTracking != null) "Rider/Ref: $cleanTracking" else null)?.take(300),
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            batch.set(eventRef, eventPayload)

            val recipientUid = if (uid == order.sellerUid) order.buyerUid else order.sellerUid
            val notifRef = db.collection("users").document(recipientUid)
                .collection("notifications").document()
            batch.set(
                notifRef,
                mapOf(
                    "recipientUid" to recipientUid,
                    "senderUid" to uid,
                    "orderId" to order.id,
                    "orderCode" to order.orderCode,
                    "title" to "${order.orderCode}: ${targetStage.label}".take(100),
                    "body" to eventTitle.take(240),
                    "type" to NotificationType.STAGE_UPDATE.name,
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateMarketplaceOrderPayment(
        userId: String,
        order: MarketplaceOrder,
        paymentStatus: PaymentStatus,
        paymentMethod: PaymentMethod
    ): Result<Unit> {
        val docPath = "marketplace_orders/${order.id}"
        return try {
            val uid = requireMatchingUserId(userId)
            require(!order.stageEnum.isTerminal) { "Finalized orders cannot be modified" }
            require(order.sellerUid == uid) { "Only the service provider can confirm payment" }

            val orderRef = db.collection("marketplace_orders").document(order.id)
            val eventRef = orderRef.collection("events").document()
            val notifRef = db.collection("users").document(order.buyerUid).collection("notifications").document()

            val batch = db.batch()
            batch.update(
                orderRef,
                mapOf(
                    "paymentStatus" to paymentStatus.name,
                    "paymentMethod" to paymentMethod.name,
                    "unreadForBuyer" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                eventRef,
                mapOf(
                    "orderId" to order.id,
                    "buyerUid" to order.buyerUid,
                    "sellerUid" to order.sellerUid,
                    "actorUid" to uid,
                    "stage" to order.stage,
                    "title" to "Payment marked ${paymentStatus.label}",
                    "note" to "Method: ${paymentMethod.label}",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                notifRef,
                mapOf(
                    "recipientUid" to order.buyerUid,
                    "senderUid" to uid,
                    "orderId" to order.id,
                    "orderCode" to order.orderCode,
                    "title" to "${order.orderCode} Payment: ${paymentStatus.label}".take(100),
                    "body" to "${order.sellerName} updated payment to ${paymentStatus.label}".take(240),
                    "type" to NotificationType.STAGE_UPDATE.name,
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun sendOrderMessage(
        userId: String,
        order: MarketplaceOrder,
        senderName: String,
        asSeller: Boolean,
        text: String
    ): Result<Unit> {
        val docPath = "marketplace_orders/${order.id}/messages"
        return try {
            val uid = requireMatchingUserId(userId)
            require(!order.stageEnum.isTerminal) { "Order is finalized and chat is closed" }
            val cleanText = text.trim()
            val cleanSenderName = senderName.trim().ifEmpty { if (asSeller) order.sellerName else order.buyerName }
            require(cleanText.length in 1..500) { "Message must be 1 to 500 characters" }

            val orderRef = db.collection("marketplace_orders").document(order.id)
            val msgRef = orderRef.collection("messages").document()
            val recipientUid = if (asSeller) order.buyerUid else order.sellerUid
            val notifRef = db.collection("users").document(recipientUid).collection("notifications").document()

            val batch = db.batch()
            batch.set(
                msgRef,
                mapOf(
                    "orderId" to order.id,
                    "buyerUid" to order.buyerUid,
                    "sellerUid" to order.sellerUid,
                    "senderUid" to uid,
                    "senderName" to cleanSenderName.take(80),
                    "senderRole" to if (asSeller) "SELLER" else "BUYER",
                    "text" to cleanText,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                orderRef,
                mapOf(
                    "lastMessagePreview" to cleanText.take(200),
                    "unreadForBuyer" to asSeller,
                    "unreadForSeller" to !asSeller,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                notifRef,
                mapOf(
                    "recipientUid" to recipientUid,
                    "senderUid" to uid,
                    "orderId" to order.id,
                    "orderCode" to order.orderCode,
                    "title" to "Message from $cleanSenderName (${order.orderCode})".take(100),
                    "body" to cleanText.take(240),
                    "type" to NotificationType.NEW_MESSAGE.name,
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(userId: String, notificationId: String): Result<Unit> {
        val docPath = "users/$userId/notifications/$notificationId"
        return try {
            val uid = requireMatchingUserId(userId)
            db.collection("users").document(uid)
                .collection("notifications").document(notificationId)
                .update("read", true)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsRead(userId: String, unreadIds: List<String>): Result<Unit> {
        val docPath = "users/$userId/notifications"
        return try {
            val uid = requireMatchingUserId(userId)
            if (unreadIds.isEmpty()) return Result.success(Unit)
            val batch = db.batch()
            val col = db.collection("users").document(uid).collection("notifications")
            unreadIds.take(200).forEach { id ->
                batch.update(col.document(id), "read", true)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }
}
