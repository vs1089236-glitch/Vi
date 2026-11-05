package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppConfigEntity
import com.example.data.local.CommissionEntity
import com.example.data.local.CommissionStatus
import com.example.data.local.CommissionType
import com.example.data.local.CommissionWalaDatabase
import com.example.data.local.DatabaseSeeder
import com.example.data.local.NotificationEntity
import com.example.data.local.NotificationType
import com.example.data.local.OrderEntity
import com.example.data.local.OrderStatus
import com.example.data.local.ProductEntity
import com.example.data.local.SessionPreferences
import com.example.data.local.SubmissionStatus
import com.example.data.local.TaskCategory
import com.example.data.local.TaskEntity
import com.example.data.local.TaskSubmissionEntity
import com.example.data.local.UserEntity
import com.example.data.local.UserRole
import com.example.data.local.UserSession
import com.example.data.local.WithdrawalEntity
import com.example.data.local.WithdrawalStatus
import com.example.util.Formatters
import com.example.util.QrCodeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.random.Random

data class WalletSummary(
    val creditedCommissionInr: Double = 0.0,
    val pendingCommissionInr: Double = 0.0,
    val taskRewardBalanceInr: Double = 0.0,
    val totalLifetimeEarningsInr: Double = 0.0,
    val totalWithdrawnInr: Double = 0.0,
    val heldWithdrawalInr: Double = 0.0,
    val availableBalanceInr: Double = 0.0,
    val totalReferredSalesCount: Int = 0
)

data class AdminDashboardSummary(
    val totalSalesInr: Double = 0.0,
    val totalOrdersCount: Int = 0,
    val pendingOrdersCount: Int = 0,
    val totalCommissionsLiabilityInr: Double = 0.0,
    val pendingCommissionApprovalsCount: Int = 0,
    val pendingWithdrawalsInr: Double = 0.0,
    val pendingWithdrawalsCount: Int = 0,
    val completedWithdrawalsInr: Double = 0.0,
    val pendingTaskReviewsCount: Int = 0,
    val totalRegisteredUsersCount: Int = 0
)

data class OrderPlacementOutcome(
    val order: OrderEntity,
    val commissionTracked: CommissionEntity?,
    val antiFraudNote: String?
)

class CommissionWalaRepository(
    private val db: CommissionWalaDatabase,
    private val sessionPrefs: SessionPreferences
) {
    val sessionFlow: Flow<UserSession> = sessionPrefs.sessionFlow
    val allUsersFlow: Flow<List<UserEntity>> = db.userDao().observeAllUsers()
    val allProductsFlow: Flow<List<ProductEntity>> = db.productDao().observeAllProducts()
    val allOrdersFlow: Flow<List<OrderEntity>> = db.orderDao().observeAllOrders()
    val allCommissionsFlow: Flow<List<CommissionEntity>> = db.commissionDao().observeAllCommissions()
    val allTasksFlow: Flow<List<TaskEntity>> = db.taskDao().observeAllTasks()
    val allSubmissionsFlow: Flow<List<TaskSubmissionEntity>> = db.taskSubmissionDao().observeAllSubmissions()
    val allWithdrawalsFlow: Flow<List<WithdrawalEntity>> = db.withdrawalDao().observeAllWithdrawals()
    val allNotificationsFlow: Flow<List<NotificationEntity>> = db.notificationDao().observeAllNotifications()
    val appConfigFlow: Flow<AppConfigEntity> = db.appConfigDao().observeConfig().map {
        it ?: AppConfigEntity()
    }

    suspend fun initialize() {
        DatabaseSeeder.seedIfEmpty(db)
        syncReturnWindowsAndUnlockCommissions()
    }

    fun observeUserById(userId: String): Flow<UserEntity?> = db.userDao().observeUserById(userId)

    suspend fun setWorkspaceMode(mode: String) {
        sessionPrefs.setWorkspaceMode(mode)
    }

    suspend fun logout() {
        sessionPrefs.saveSession(null, "USER")
    }

    suspend fun login(email: String, password: String, requireAdminRole: Boolean = false): Result<UserEntity> {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty() || password.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please enter both email and password."))
        }
        val user = db.userDao().getUserByEmail(cleanEmail)
            ?: return Result.failure(IllegalArgumentException("No account found with this email."))
        if (!user.isActive) {
            return Result.failure(IllegalStateException("This account is currently suspended by Admin."))
        }
        if (user.passwordHash != Formatters.sha256Hex(password)) {
            return Result.failure(IllegalArgumentException("Incorrect password. Please try again."))
        }
        if (requireAdminRole && user.role != UserRole.ADMIN) {
            return Result.failure(IllegalStateException("Access denied: This account does not have Admin privileges."))
        }
        val mode = if (user.role == UserRole.ADMIN) "ADMIN" else "USER"
        sessionPrefs.saveSession(user.id, mode)
        return Result.success(user)
    }

    suspend fun quickSignInDemoUser(userId: String, targetMode: String): Result<UserEntity> {
        val user = db.userDao().getUserById(userId)
            ?: return Result.failure(IllegalStateException("Demo user not found"))
        if (!user.isActive) {
            return Result.failure(IllegalStateException("Account suspended"))
        }
        val safeMode = if (targetMode == "ADMIN" && user.role == UserRole.ADMIN) "ADMIN" else "USER"
        sessionPrefs.saveSession(user.id, safeMode)
        return Result.success(user)
    }

    suspend fun registerUser(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        referredByCode: String?,
        upiId: String
    ): Result<UserEntity> {
        val cleanName = fullName.trim()
        val cleanEmail = email.trim().lowercase(Locale.ROOT)
        val cleanPhone = phone.trim()
        val cleanUpi = upiId.trim()

        if (cleanName.length < 2) {
            return Result.failure(IllegalArgumentException("Enter your full name."))
        }
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return Result.failure(IllegalArgumentException("Enter a valid email address."))
        }
        if (cleanPhone.length < 10) {
            return Result.failure(IllegalArgumentException("Enter a valid 10-digit mobile number."))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }
        if (cleanUpi.isNotEmpty() && !Formatters.isValidUpiId(cleanUpi)) {
            return Result.failure(IllegalArgumentException("Invalid UPI ID format (e.g. name@upi)."))
        }
        if (db.userDao().getUserByEmail(cleanEmail) != null) {
            return Result.failure(IllegalArgumentException("An account with this email already exists."))
        }

        val validInviterCode = referredByCode?.trim()?.takeIf { it.isNotEmpty() }?.let { code ->
            db.userDao().getActiveUserByReferralCode(code)?.referralCode
                ?: return Result.failure(IllegalArgumentException("Referral ID '$code' was not found."))
        }

        val prefix = cleanName.uppercase(Locale.ROOT)
            .replace(Regex("[^A-Z]"), "")
            .take(5)
            .ifEmpty { "USER" }
        var generatedCode: String
        do {
            val digits = Random.nextInt(1000, 9999)
            generatedCode = "CW-$prefix-$digits"
        } while (db.userDao().getActiveUserByReferralCode(generatedCode) != null)

        val userId = "usr_${UUID.randomUUID().toString().take(8)}"
        val newUser = UserEntity(
            id = userId,
            fullName = cleanName,
            email = cleanEmail,
            phone = cleanPhone,
            passwordHash = Formatters.sha256Hex(password),
            role = UserRole.USER,
            referralCode = generatedCode,
            referredByCode = validInviterCode,
            upiId = cleanUpi,
            upiAccountName = if (cleanUpi.isNotEmpty()) cleanName else ""
        )
        db.userDao().insertUser(newUser)
        sessionPrefs.saveSession(newUser.id, "USER")

        db.notificationDao().insertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                recipientUserId = newUser.id,
                type = NotificationType.COMMISSION_EARNED,
                title = "Welcome to CommissionWala!",
                message = "Your unique Referral ID $generatedCode is active. Share product QRs to start earning!",
                relatedId = newUser.id
            )
        )
        return Result.success(newUser)
    }

    suspend fun updateUserUpi(userId: String, upiId: String, accountName: String): Result<Unit> {
        val cleanUpi = upiId.trim()
        val cleanAcc = accountName.trim()
        if (!Formatters.isValidUpiId(cleanUpi)) {
            return Result.failure(IllegalArgumentException("Enter a valid UPI ID (e.g. rahul@okaxis or 9876543210@ybl)."))
        }
        val user = db.userDao().getUserById(userId)
            ?: return Result.failure(IllegalStateException("User not found"))
        db.userDao().updateUser(
            user.copy(
                upiId = cleanUpi,
                upiAccountName = cleanAcc.ifEmpty { user.fullName }
            )
        )
        return Result.success(Unit)
    }

    suspend fun toggleUserActive(userId: String, isActive: Boolean): Result<Unit> {
        val user = db.userDao().getUserById(userId)
            ?: return Result.failure(IllegalStateException("User not found"))
        if (user.role == UserRole.ADMIN && !isActive) {
            return Result.failure(IllegalStateException("Primary Admin account cannot be deactivated."))
        }
        db.userDao().updateUser(user.copy(isActive = isActive))
        return Result.success(Unit)
    }

    // --- PRODUCT MANAGEMENT (ADMIN) ---

    suspend fun saveProduct(
        existingId: String?,
        name: String,
        description: String,
        category: String,
        priceInr: Double,
        stock: Int,
        commissionType: CommissionType,
        commissionValue: Double,
        returnWindowDays: Int,
        imageUri: String
    ): Result<ProductEntity> {
        val cleanName = name.trim()
        val cleanDesc = description.trim()
        val cleanCat = category.trim().ifEmpty { "General" }

        if (cleanName.length < 3) {
            return Result.failure(IllegalArgumentException("Product name must be at least 3 characters."))
        }
        if (priceInr <= 0.0) {
            return Result.failure(IllegalArgumentException("Price must be greater than ₹0."))
        }
        if (stock < 0) {
            return Result.failure(IllegalArgumentException("Stock cannot be negative."))
        }
        if (commissionValue <= 0.0) {
            return Result.failure(IllegalArgumentException("Commission value must be greater than 0."))
        }
        if (commissionType == CommissionType.PERCENTAGE && commissionValue > 80.0) {
            return Result.failure(IllegalArgumentException("Percentage commission cannot exceed 80%."))
        }

        val now = System.currentTimeMillis()
        val existing = existingId?.let { db.productDao().getProductById(it) }
        val isNew = existing == null
        val id = existing?.id ?: "prod_${UUID.randomUUID().toString().take(8)}"
        val sku = existing?.skuCode ?: "PRD-${cleanCat.take(3).uppercase(Locale.ROOT)}-${Random.nextInt(100, 999)}"
        val qrPayload = QrCodeUtils.buildProductQrPayload(sku, null)

        val product = ProductEntity(
            id = id,
            skuCode = sku,
            name = cleanName,
            description = cleanDesc.ifEmpty { "High-demand affiliate catalog product with verified QR tracking." },
            category = cleanCat,
            priceInr = priceInr,
            stock = stock,
            commissionType = commissionType,
            commissionValue = commissionValue,
            returnWindowDays = returnWindowDays.coerceIn(0, 30),
            imageUri = imageUri.ifEmpty { "res://img_prod_earbuds" },
            qrPayloadBase = qrPayload,
            isActive = existing?.isActive ?: true,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )

        db.productDao().upsertProduct(product)

        if (isNew) {
            db.notificationDao().insertNotification(
                NotificationEntity(
                    id = "notif_${UUID.randomUUID().toString().take(8)}",
                    recipientUserId = "ALL",
                    type = NotificationType.NEW_PRODUCT,
                    title = "New Product: ${product.name}",
                    message = "Earn ${product.commissionBadgeText()} per sale! QR code is ready to share.",
                    relatedId = product.id
                )
            )
        }
        return Result.success(product)
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        db.productDao().deleteProductById(productId)
        return Result.success(Unit)
    }

    // --- QR RESOLUTION & CHECKOUT WITH ANTI-FRAUD REFERRAL ATTRIBUTION ---

    suspend fun resolveProductFromQr(rawQrText: String): Result<Pair<ProductEntity, UserEntity?>> {
        val parsed = QrCodeUtils.parseCommissionWalaQr(rawQrText)
            ?: return Result.failure(IllegalArgumentException("Invalid QR code or link format."))
        val product = db.productDao().getProductBySku(parsed.skuCode)
            ?: db.productDao().getProductById(parsed.skuCode)
            ?: return Result.failure(IllegalArgumentException("Product '${parsed.skuCode}' not found in catalog."))
        val referrer = parsed.referralCode?.let { db.userDao().getActiveUserByReferralCode(it) }
        return Result.success(product to referrer)
    }

    suspend fun placeReferralOrder(
        productId: String,
        quantity: Int,
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        city: String,
        pincode: String,
        paymentMethod: String,
        referralCodeInput: String?,
        currentLoggedInUserId: String?
    ): Result<OrderPlacementOutcome> {
        val cleanName = customerName.trim()
        val cleanPhone = customerPhone.trim()
        val cleanAddress = deliveryAddress.trim()
        val cleanCity = city.trim()
        val cleanPin = pincode.trim()

        if (cleanName.length < 2) {
            return Result.failure(IllegalArgumentException("Enter customer full name."))
        }
        if (cleanPhone.length < 10) {
            return Result.failure(IllegalArgumentException("Enter a valid 10-digit customer phone number."))
        }
        if (cleanAddress.length < 5 || cleanCity.isEmpty() || cleanPin.length < 5) {
            return Result.failure(IllegalArgumentException("Complete the delivery address, city, and PIN code."))
        }
        if (quantity <= 0) {
            return Result.failure(IllegalArgumentException("Quantity must be at least 1."))
        }

        val cleanRef = referralCodeInput?.trim()?.takeIf { it.isNotEmpty() }
        val referrer = cleanRef?.let { db.userDao().getActiveUserByReferralCode(it) }
        if (cleanRef != null && referrer == null) {
            return Result.failure(IllegalArgumentException("Referral ID '$cleanRef' is invalid or inactive."))
        }

        // Duplicate / rapid-replay claim prevention (1-minute window per product + phone + referral)
        val minuteBucket = System.currentTimeMillis() / 60_000L
        val claimSig = Formatters.sha256Hex("$productId|$cleanPhone|${cleanRef.orEmpty()}|$minuteBucket")
        if (db.orderDao().existsByClaimSignature(claimSig)) {
            return Result.failure(
                IllegalStateException("Duplicate order/claim detected for this phone & product. Please wait a moment before placing another order.")
            )
        }

        return try {
            val outcome = db.withTransaction {
                val product = db.productDao().getProductById(productId)
                    ?: throw IllegalStateException("Product no longer exists.")
                if (product.stock < quantity) {
                    throw IllegalStateException("Insufficient stock (${product.stock} left).")
                }

                // Anti-Fraud Self-Referral Guard:
                // Prevent an affiliate from earning commission on their own phone/account purchase!
                val isSelfReferral = referrer != null && (
                    referrer.id == currentLoggedInUserId ||
                        referrer.phone == cleanPhone
                    )

                val effectiveReferrer = if (isSelfReferral) null else referrer
                val effectiveRefCode = effectiveReferrer?.referralCode

                val now = System.currentTimeMillis()
                val orderId = "ord_${UUID.randomUUID().toString().take(8)}"
                val orderNum = "CW-ORD-${Random.nextInt(1000, 9999)}"
                val totalAmount = (product.priceInr * quantity * 100.0).roundToInt() / 100.0

                val order = OrderEntity(
                    id = orderId,
                    orderNumber = orderNum,
                    productId = product.id,
                    productName = product.name,
                    productImageUri = product.imageUri,
                    quantity = quantity,
                    unitPriceInr = product.priceInr,
                    totalAmountInr = totalAmount,
                    customerName = cleanName,
                    customerPhone = cleanPhone,
                    deliveryAddress = cleanAddress,
                    city = cleanCity,
                    pincode = cleanPin,
                    paymentMethod = paymentMethod,
                    referrerUserId = effectiveReferrer?.id,
                    referralCode = effectiveRefCode,
                    status = OrderStatus.PENDING,
                    claimSignature = claimSig,
                    createdAt = now
                )

                db.productDao().upsertProduct(product.copy(stock = product.stock - quantity, updatedAt = now))
                db.orderDao().insertOrder(order)

                var createdCommission: CommissionEntity? = null
                if (effectiveReferrer != null) {
                    val rawComm = product.calculateCommission(quantity)
                    val roundedComm = (rawComm * 100.0).roundToInt() / 100.0
                    val commission = CommissionEntity(
                        id = "comm_${UUID.randomUUID().toString().take(8)}",
                        orderId = order.id,
                        orderNumber = order.orderNumber,
                        productId = product.id,
                        productName = product.name,
                        affiliateUserId = effectiveReferrer.id,
                        referralCode = effectiveReferrer.referralCode,
                        saleAmountInr = totalAmount,
                        commissionRateLabel = product.commissionBadgeText(),
                        commissionAmountInr = roundedComm,
                        status = CommissionStatus.PENDING_DELIVERY,
                        adminNote = "Tracked via QR/Link (${effectiveReferrer.referralCode})",
                        createdAt = now
                    )
                    db.commissionDao().insertCommission(commission)
                    createdCommission = commission

                    db.notificationDao().insertNotification(
                        NotificationEntity(
                            id = "notif_${UUID.randomUUID().toString().take(8)}",
                            recipientUserId = effectiveReferrer.id,
                            type = NotificationType.COMMISSION_EARNED,
                            title = "New Referral Sale: +${Formatters.formatInr(roundedComm)}",
                            message = "$cleanName ordered ${quantity}× ${product.name} using your code ${effectiveReferrer.referralCode}. Unlocks after delivery & ${product.returnWindowDays}d return period.",
                            relatedId = commission.id
                        )
                    )
                }

                db.notificationDao().insertNotification(
                    NotificationEntity(
                        id = "notif_${UUID.randomUUID().toString().take(8)}",
                        recipientUserId = "ADMIN",
                        type = NotificationType.ORDER_UPDATE,
                        title = "New Order ${order.orderNumber} (${Formatters.formatInr(totalAmount)})",
                        message = if (effectiveReferrer != null) {
                            "Customer: $cleanName • Referred by ${effectiveReferrer.fullName} (${effectiveReferrer.referralCode})"
                        } else {
                            "Customer: $cleanName • Direct catalog order"
                        },
                        relatedId = order.id
                    )
                )

                val fraudNote = if (isSelfReferral) {
                    "Self-referral guard active: Commission was not credited because buyer matches affiliate account/phone."
                } else null

                OrderPlacementOutcome(order, createdCommission, fraudNote)
            }
            Result.success(outcome)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- ORDER LIFECYCLE & COMMISSION APPROVALS ---

    suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus): Result<Unit> {
        return try {
            db.withTransaction {
                val order = db.orderDao().getOrderById(orderId)
                    ?: throw IllegalStateException("Order not found")
                if (order.status == newStatus) return@withTransaction

                val now = System.currentTimeMillis()
                val product = db.productDao().getProductById(order.productId)
                val returnDays = product?.returnWindowDays ?: 7

                // Restore stock if transitioning to CANCELLED from an active state
                if (newStatus == OrderStatus.CANCELLED && order.status != OrderStatus.CANCELLED && product != null) {
                    db.productDao().upsertProduct(product.copy(stock = product.stock + order.quantity, updatedAt = now))
                }

                val deliveredAt = if (newStatus == OrderStatus.DELIVERED) now else order.deliveredAt
                val returnEndsAt = if (newStatus == OrderStatus.DELIVERED) {
                    now + returnDays * 86_400_000L
                } else {
                    order.returnWindowEndsAt
                }

                val updatedOrder = order.copy(
                    status = newStatus,
                    deliveredAt = deliveredAt,
                    returnWindowEndsAt = returnEndsAt
                )
                db.orderDao().updateOrder(updatedOrder)

                val commission = db.commissionDao().getCommissionByOrderId(order.id)
                if (commission != null) {
                    val config = db.appConfigDao().getConfig() ?: AppConfigEntity()
                    val updatedComm = when (newStatus) {
                        OrderStatus.DELIVERED -> {
                            if (returnDays == 0) {
                                if (config.requireManualCommissionApproval) {
                                    commission.copy(
                                        status = CommissionStatus.PAYABLE,
                                        returnWindowEndsAt = now,
                                        adminNote = "Delivered (0d return window) — Ready for approval"
                                    )
                                } else {
                                    commission.copy(
                                        status = CommissionStatus.APPROVED_PAID,
                                        returnWindowEndsAt = now,
                                        resolvedAt = now,
                                        adminNote = "Auto-credited on delivery"
                                    )
                                }
                            } else {
                                commission.copy(
                                    status = CommissionStatus.IN_RETURN_WINDOW,
                                    returnWindowEndsAt = returnEndsAt,
                                    adminNote = "${returnDays}-day return/refund window active"
                                )
                            }
                        }
                        OrderStatus.CANCELLED -> {
                            commission.copy(
                                status = CommissionStatus.REJECTED,
                                resolvedAt = now,
                                adminNote = "Order cancelled — commission voided"
                            )
                        }
                        else -> commission
                    }
                    db.commissionDao().updateCommission(updatedComm)
                }

                if (order.referrerUserId != null) {
                    db.notificationDao().insertNotification(
                        NotificationEntity(
                            id = "notif_${UUID.randomUUID().toString().take(8)}",
                            recipientUserId = order.referrerUserId,
                            type = NotificationType.ORDER_UPDATE,
                            title = "Order ${order.orderNumber} is now ${newStatus.label}",
                            message = when (newStatus) {
                                OrderStatus.DELIVERED -> "Delivered! Commission unlocks after the return window."
                                OrderStatus.CANCELLED -> "Order was cancelled; pending commission has been voided."
                                else -> "Customer order status updated to ${newStatus.label}."
                            },
                            relatedId = order.id
                        )
                    )
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncReturnWindowsAndUnlockCommissions() {
        val now = System.currentTimeMillis()
        val config = db.appConfigDao().getConfig() ?: AppConfigEntity()
        val inReturn = db.commissionDao().getCommissionsInReturnWindow()
        inReturn.forEach { comm ->
            val end = comm.returnWindowEndsAt ?: 0L
            if (end in 1L..now) {
                val nextStatus = if (config.requireManualCommissionApproval) {
                    CommissionStatus.PAYABLE
                } else {
                    CommissionStatus.APPROVED_PAID
                }
                db.commissionDao().updateCommission(
                    comm.copy(
                        status = nextStatus,
                        resolvedAt = if (nextStatus == CommissionStatus.APPROVED_PAID) now else null,
                        adminNote = if (nextStatus == CommissionStatus.APPROVED_PAID) {
                            "Return window elapsed — auto-credited to wallet"
                        } else {
                            "Return window completed — ready for Admin approval"
                        }
                    )
                )
            }
        }
    }

    suspend fun approveCommission(commissionId: String): Result<Unit> {
        val comm = db.commissionDao().getCommissionById(commissionId)
            ?: return Result.failure(IllegalStateException("Commission record not found"))
        if (comm.status == CommissionStatus.APPROVED_PAID) {
            return Result.success(Unit)
        }
        val now = System.currentTimeMillis()
        db.commissionDao().updateCommission(
            comm.copy(
                status = CommissionStatus.APPROVED_PAID,
                resolvedAt = now,
                adminNote = "Approved & credited by Admin"
            )
        )
        db.notificationDao().insertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                recipientUserId = comm.affiliateUserId,
                type = NotificationType.COMMISSION_EARNED,
                title = "Commission Credited: ${Formatters.formatInr(comm.commissionAmountInr)}",
                message = "Admin approved your commission for Order #${comm.orderNumber}. Funds are now withdrawable!",
                relatedId = comm.id
            )
        )
        return Result.success(Unit)
    }

    suspend fun rejectCommission(commissionId: String, reason: String): Result<Unit> {
        val comm = db.commissionDao().getCommissionById(commissionId)
            ?: return Result.failure(IllegalStateException("Commission record not found"))
        val now = System.currentTimeMillis()
        val cleanReason = reason.trim().ifEmpty { "Rejected during audit" }
        db.commissionDao().updateCommission(
            comm.copy(
                status = CommissionStatus.REJECTED,
                resolvedAt = now,
                adminNote = cleanReason
            )
        )
        db.notificationDao().insertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                recipientUserId = comm.affiliateUserId,
                type = NotificationType.COMMISSION_EARNED,
                title = "Commission Claim Rejected",
                message = "Order #${comm.orderNumber}: $cleanReason",
                relatedId = comm.id
            )
        )
        return Result.success(Unit)
    }

    // --- TASK MANAGEMENT & PROOF REVIEW ---

    suspend fun saveTask(
        existingId: String?,
        title: String,
        description: String,
        instructions: String,
        category: TaskCategory,
        rewardInr: Double,
        deadlineDaysFromNow: Int,
        imageUri: String
    ): Result<TaskEntity> {
        val cleanTitle = title.trim()
        val cleanDesc = description.trim()
        val cleanInstr = instructions.trim()
        if (cleanTitle.length < 4) {
            return Result.failure(IllegalArgumentException("Task title must be at least 4 characters."))
        }
        if (rewardInr <= 0.0) {
            return Result.failure(IllegalArgumentException("Reward amount must be greater than ₹0."))
        }

        val now = System.currentTimeMillis()
        val existing = existingId?.let { db.taskDao().getTaskById(it) }
        val isNew = existing == null
        val id = existing?.id ?: "task_${UUID.randomUUID().toString().take(8)}"
        val deadline = now + deadlineDaysFromNow.coerceIn(1, 90) * 86_400_000L

        val task = TaskEntity(
            id = id,
            title = cleanTitle,
            description = cleanDesc.ifEmpty { "Complete the promotional task and upload screenshot proof to earn instant wallet reward." },
            instructions = cleanInstr.ifEmpty { "1. Complete the activity.\n2. Capture a clear screenshot.\n3. Submit proof before deadline." },
            category = category,
            rewardInr = rewardInr,
            deadlineAt = deadline,
            imageUri = imageUri.ifEmpty { "res://img_hero_affiliate" },
            isActive = existing?.isActive ?: true,
            createdAt = existing?.createdAt ?: now
        )
        db.taskDao().upsertTask(task)

        if (isNew) {
            db.notificationDao().insertNotification(
                NotificationEntity(
                    id = "notif_${UUID.randomUUID().toString().take(8)}",
                    recipientUserId = "ALL",
                    type = NotificationType.NEW_TASK,
                    title = "New Task: Earn ${Formatters.formatInr(rewardInr)}",
                    message = task.title,
                    relatedId = task.id
                )
            )
        }
        return Result.success(task)
    }

    suspend fun toggleTaskActive(taskId: String, active: Boolean) {
        val task = db.taskDao().getTaskById(taskId) ?: return
        db.taskDao().upsertTask(task.copy(isActive = active))
    }

    suspend fun deleteTask(taskId: String) {
        db.taskDao().deleteTaskById(taskId)
    }

    suspend fun submitTaskProof(
        taskId: String,
        userId: String,
        proofImageUri: String?,
        proofNotes: String,
        proofLink: String
    ): Result<TaskSubmissionEntity> {
        val task = db.taskDao().getTaskById(taskId)
            ?: return Result.failure(IllegalStateException("Task not found"))
        val user = db.userDao().getUserById(userId)
            ?: return Result.failure(IllegalStateException("User not found"))

        if (!task.isActive) {
            return Result.failure(IllegalStateException("This task is no longer accepting submissions."))
        }
        if (System.currentTimeMillis() > task.deadlineAt) {
            return Result.failure(IllegalStateException("Task deadline has expired."))
        }
        if (db.taskSubmissionDao().hasActiveOrApprovedSubmission(taskId, userId)) {
            return Result.failure(IllegalStateException("You already have a pending or approved submission for this task."))
        }
        val cleanNotes = proofNotes.trim()
        val cleanLink = proofLink.trim()
        if (proofImageUri.isNullOrBlank() && cleanNotes.length < 5 && cleanLink.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please attach a screenshot or enter proof notes/link."))
        }

        val submission = TaskSubmissionEntity(
            id = "sub_${UUID.randomUUID().toString().take(8)}",
            taskId = task.id,
            taskTitle = task.title,
            userId = user.id,
            userName = user.fullName,
            userReferralCode = user.referralCode,
            proofImageUri = proofImageUri,
            proofNotes = cleanNotes.ifEmpty { "Screenshot proof attached" },
            proofLink = cleanLink,
            rewardInr = task.rewardInr,
            status = SubmissionStatus.PENDING
        )
        db.taskSubmissionDao().insertSubmission(submission)

        db.notificationDao().insertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                recipientUserId = "ADMIN",
                type = NotificationType.NEW_TASK,
                title = "Task Proof Submitted by ${user.fullName}",
                message = "${task.title} • Reward: ${Formatters.formatInr(task.rewardInr)}",
                relatedId = submission.id
            )
        )
        return Result.success(submission)
    }

    suspend fun reviewTaskSubmission(
        submissionId: String,
        approve: Boolean,
        adminNote: String
    ): Result<Unit> {
        val sub = db.taskSubmissionDao().getSubmissionById(submissionId)
            ?: return Result.failure(IllegalStateException("Submission not found"))
        if (sub.status != SubmissionStatus.PENDING) {
            return Result.failure(IllegalStateException("Submission has already been reviewed."))
        }

        val now = System.currentTimeMillis()
        val nextStatus = if (approve) SubmissionStatus.APPROVED else SubmissionStatus.REJECTED
        val note = adminNote.trim().ifEmpty {
            if (approve) "Proof verified. Reward added to wallet!" else "Proof insufficient. Please resubmit with clear screenshot."
        }

        db.taskSubmissionDao().updateSubmission(
            sub.copy(
                status = nextStatus,
                adminNote = note,
                reviewedAt = now
            )
        )

        db.notificationDao().insertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                recipientUserId = sub.userId,
                type = NotificationType.COMMISSION_EARNED,
                title = if (approve) "Task Reward Credited: +${Formatters.formatInr(sub.rewardInr)}" else "Task Proof Rejected",
                message = "${sub.taskTitle} — $note",
                relatedId = sub.id
            )
        )
        return Result.success(Unit)
    }

    // --- WALLET & UPI WITHDRAWALS ---

    fun computeWalletSummary(
        userId: String,
        commissions: List<CommissionEntity>,
        submissions: List<TaskSubmissionEntity>,
        withdrawals: List<WithdrawalEntity>
    ): WalletSummary {
        val userComms = commissions.filter { it.affiliateUserId == userId }
        val creditedComm = userComms
            .filter { it.status == CommissionStatus.APPROVED_PAID }
            .sumOf { it.commissionAmountInr }
        val pendingComm = userComms
            .filter {
                it.status == CommissionStatus.PENDING_DELIVERY ||
                    it.status == CommissionStatus.IN_RETURN_WINDOW ||
                    it.status == CommissionStatus.PAYABLE
            }
            .sumOf { it.commissionAmountInr }

        val taskRewards = submissions
            .filter { it.userId == userId && it.status == SubmissionStatus.APPROVED }
            .sumOf { it.rewardInr }

        val userWithdrawals = withdrawals.filter { it.userId == userId }
        val withdrawnPaid = userWithdrawals
            .filter { it.status == WithdrawalStatus.APPROVED }
            .sumOf { it.amountInr }
        val heldWithdrawals = userWithdrawals
            .filter { it.status == WithdrawalStatus.PENDING }
            .sumOf { it.amountInr }

        val lifetime = creditedComm + taskRewards
        val available = (lifetime - withdrawnPaid - heldWithdrawals).coerceAtLeast(0.0)

        return WalletSummary(
            creditedCommissionInr = creditedComm,
            pendingCommissionInr = pendingComm,
            taskRewardBalanceInr = taskRewards,
            totalLifetimeEarningsInr = lifetime,
            totalWithdrawnInr = withdrawnPaid,
            heldWithdrawalInr = heldWithdrawals,
            availableBalanceInr = available,
            totalReferredSalesCount = userComms.count { it.status != CommissionStatus.REJECTED }
        )
    }

    suspend fun requestWithdrawal(
        userId: String,
        amountInr: Double,
        availableBalanceInr: Double
    ): Result<WithdrawalEntity> {
        val user = db.userDao().getUserById(userId)
            ?: return Result.failure(IllegalStateException("User not found"))
        val config = db.appConfigDao().getConfig() ?: AppConfigEntity()

        if (!Formatters.isValidUpiId(user.upiId)) {
            return Result.failure(IllegalArgumentException("Please save a valid UPI ID before requesting a withdrawal."))
        }
        if (amountInr < config.minWithdrawalInr) {
            return Result.failure(
                IllegalArgumentException("Minimum withdrawal limit is ${Formatters.formatInr(config.minWithdrawalInr)}.")
            )
        }
        if (amountInr > availableBalanceInr) {
            return Result.failure(
                IllegalArgumentException("Insufficient available wallet balance (${Formatters.formatInr(availableBalanceInr)}).")
            )
        }

        val withdrawal = WithdrawalEntity(
            id = "wd_${UUID.randomUUID().toString().take(8)}",
            userId = user.id,
            userName = user.fullName,
            upiId = user.upiId,
            upiAccountName = user.upiAccountName.ifEmpty { user.fullName },
            amountInr = amountInr,
            status = WithdrawalStatus.PENDING
        )
        db.withdrawalDao().insertWithdrawal(withdrawal)

        db.notificationDao().insertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                recipientUserId = "ADMIN",
                type = NotificationType.WITHDRAWAL_UPDATE,
                title = "New UPI Withdrawal Request: ${Formatters.formatInr(amountInr)}",
                message = "${user.fullName} requested payout to ${user.upiId}",
                relatedId = withdrawal.id
            )
        )
        return Result.success(withdrawal)
    }

    suspend fun processWithdrawal(
        withdrawalId: String,
        approve: Boolean,
        utrReference: String,
        adminNote: String
    ): Result<Unit> {
        val wd = db.withdrawalDao().getWithdrawalById(withdrawalId)
            ?: return Result.failure(IllegalStateException("Withdrawal request not found"))
        if (wd.status != WithdrawalStatus.PENDING) {
            return Result.failure(IllegalStateException("Withdrawal already processed."))
        }

        val now = System.currentTimeMillis()
        val nextStatus = if (approve) WithdrawalStatus.APPROVED else WithdrawalStatus.REJECTED
        val cleanUtr = if (approve) {
            utrReference.trim().ifEmpty { "UTR${Random.nextInt(100000, 999999)}${Random.nextInt(100000, 999999)}" }
        } else ""
        val cleanNote = adminNote.trim().ifEmpty {
            if (approve) "Paid via UPI" else "Rejected by Admin — amount refunded to wallet"
        }

        db.withdrawalDao().updateWithdrawal(
            wd.copy(
                status = nextStatus,
                utrReference = cleanUtr,
                adminNote = cleanNote,
                processedAt = now
            )
        )

        db.notificationDao().insertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                recipientUserId = wd.userId,
                type = NotificationType.WITHDRAWAL_UPDATE,
                title = if (approve) "UPI Payout Paid: ${Formatters.formatInr(wd.amountInr)}" else "Withdrawal Rejected: ${Formatters.formatInr(wd.amountInr)}",
                message = if (approve) "Sent to ${wd.upiId} • UTR: $cleanUtr" else "$cleanNote. Balance restored.",
                relatedId = wd.id
            )
        )
        return Result.success(Unit)
    }

    suspend fun updateAdminConfig(minWithdrawalInr: Double, requireManualCommissionApproval: Boolean): Result<Unit> {
        if (minWithdrawalInr < 10.0) {
            return Result.failure(IllegalArgumentException("Minimum withdrawal limit must be at least ₹10."))
        }
        val current = db.appConfigDao().getConfig() ?: AppConfigEntity()
        db.appConfigDao().upsertConfig(
            current.copy(
                minWithdrawalInr = minWithdrawalInr,
                requireManualCommissionApproval = requireManualCommissionApproval,
                updatedAt = System.currentTimeMillis()
            )
        )
        return Result.success(Unit)
    }

    suspend fun markNotificationRead(id: String) {
        db.notificationDao().markAsRead(id)
    }

    suspend fun markAllNotificationsRead(recipientId: String) {
        db.notificationDao().markAllAsReadForUser(recipientId)
    }

    fun computeAdminSummary(
        users: List<UserEntity>,
        orders: List<OrderEntity>,
        commissions: List<CommissionEntity>,
        submissions: List<TaskSubmissionEntity>,
        withdrawals: List<WithdrawalEntity>
    ): AdminDashboardSummary {
        val activeOrders = orders.filter { it.status != OrderStatus.CANCELLED }
        val totalSales = activeOrders.sumOf { it.totalAmountInr }
        val pendingOrders = orders.count { it.status == OrderStatus.PENDING || it.status == OrderStatus.CONFIRMED }

        val validComms = commissions.filter { it.status != CommissionStatus.REJECTED }
        val totalCommLiability = validComms.sumOf { it.commissionAmountInr }
        val pendingCommCount = commissions.count {
            it.status == CommissionStatus.PAYABLE || it.status == CommissionStatus.IN_RETURN_WINDOW
        }

        val pendingWd = withdrawals.filter { it.status == WithdrawalStatus.PENDING }
        val completedWd = withdrawals.filter { it.status == WithdrawalStatus.APPROVED }

        val pendingTaskReviews = submissions.count { it.status == SubmissionStatus.PENDING }

        return AdminDashboardSummary(
            totalSalesInr = totalSales,
            totalOrdersCount = orders.size,
            pendingOrdersCount = pendingOrders,
            totalCommissionsLiabilityInr = totalCommLiability,
            pendingCommissionApprovalsCount = pendingCommCount,
            pendingWithdrawalsInr = pendingWd.sumOf { it.amountInr },
            pendingWithdrawalsCount = pendingWd.size,
            completedWithdrawalsInr = completedWd.sumOf { it.amountInr },
            pendingTaskReviewsCount = pendingTaskReviews,
            totalRegisteredUsersCount = users.count { it.role == UserRole.USER }
        )
    }
}
