package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppConfigEntity
import com.example.data.local.CommissionEntity
import com.example.data.local.CommissionType
import com.example.data.local.NotificationEntity
import com.example.data.local.OrderEntity
import com.example.data.local.OrderStatus
import com.example.data.local.ProductEntity
import com.example.data.local.TaskCategory
import com.example.data.local.TaskEntity
import com.example.data.local.TaskSubmissionEntity
import com.example.data.local.UserEntity
import com.example.data.local.UserRole
import com.example.data.local.UserSession
import com.example.data.local.WithdrawalEntity
import com.example.data.repository.AdminDashboardSummary
import com.example.data.repository.CommissionWalaRepository
import com.example.data.repository.OrderPlacementOutcome
import com.example.data.repository.WalletSummary
import com.example.util.Formatters
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QrModalContext(
    val product: ProductEntity,
    val referralCode: String?,
    val affiliateName: String?
)

data class CheckoutDraft(
    val product: ProductEntity,
    val referralCode: String?,
    val referrerName: String?,
    val scannedFromQr: Boolean
)

data class TransientUiState(
    val isInitializing: Boolean = true,
    val isBusy: Boolean = false,
    val authError: String? = null,
    val snackbarMessage: String? = null,
    val qrModal: QrModalContext? = null,
    val checkoutDraft: CheckoutDraft? = null,
    val lastOrderOutcome: OrderPlacementOutcome? = null,
    val showNotificationsSheet: Boolean = false
)

data class CommissionWalaUiState(
    val isInitializing: Boolean = true,
    val isBusy: Boolean = false,
    val currentUser: UserEntity? = null,
    val isAdminMode: Boolean = false,
    val products: List<ProductEntity> = emptyList(),
    val orders: List<OrderEntity> = emptyList(),
    val commissions: List<CommissionEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val taskSubmissions: List<TaskSubmissionEntity> = emptyList(),
    val withdrawals: List<WithdrawalEntity> = emptyList(),
    val allUsers: List<UserEntity> = emptyList(),
    val notifications: List<NotificationEntity> = emptyList(),
    val unreadNotificationCount: Int = 0,
    val appConfig: AppConfigEntity = AppConfigEntity(),
    val walletSummary: WalletSummary = WalletSummary(),
    val adminSummary: AdminDashboardSummary = AdminDashboardSummary(),
    val authError: String? = null,
    val snackbarMessage: String? = null,
    val qrModal: QrModalContext? = null,
    val checkoutDraft: CheckoutDraft? = null,
    val lastOrderOutcome: OrderPlacementOutcome? = null,
    val showNotificationsSheet: Boolean = false
)

private data class CoreCatalogSlice(
    val session: UserSession,
    val user: UserEntity?,
    val products: List<ProductEntity>,
    val orders: List<OrderEntity>,
    val commissions: List<CommissionEntity>
)

private data class OperationsSlice(
    val tasks: List<TaskEntity>,
    val submissions: List<TaskSubmissionEntity>,
    val withdrawals: List<WithdrawalEntity>,
    val notifications: List<NotificationEntity>,
    val config: AppConfigEntity
)

class MainViewModel(
    private val repository: CommissionWalaRepository
) : ViewModel() {

    private val transientState = MutableStateFlow(TransientUiState())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentUserFlow = repository.sessionFlow.flatMapLatest { session ->
        val uid = session.userId
        if (uid == null) flowOf(null) else repository.observeUserById(uid)
    }

    private val coreSliceFlow = combine(
        repository.sessionFlow,
        currentUserFlow,
        repository.allProductsFlow,
        repository.allOrdersFlow,
        repository.allCommissionsFlow
    ) { session, user, products, orders, commissions ->
        CoreCatalogSlice(session, user, products, orders, commissions)
    }

    private val opsSliceFlow = combine(
        repository.allTasksFlow,
        repository.allSubmissionsFlow,
        repository.allWithdrawalsFlow,
        repository.allNotificationsFlow,
        repository.appConfigFlow
    ) { tasks, submissions, withdrawals, notifications, config ->
        OperationsSlice(tasks, submissions, withdrawals, notifications, config)
    }

    val uiState: StateFlow<CommissionWalaUiState> = combine(
        coreSliceFlow,
        opsSliceFlow,
        repository.allUsersFlow,
        transientState
    ) { core, ops, allUsers, transient ->
        val user = core.user
        val isAdmin = user?.role == UserRole.ADMIN && core.session.workspaceMode == "ADMIN"

        val wallet = if (user != null) {
            repository.computeWalletSummary(
                userId = user.id,
                commissions = core.commissions,
                submissions = ops.submissions,
                withdrawals = ops.withdrawals
            )
        } else {
            WalletSummary()
        }

        val adminSummary = repository.computeAdminSummary(
            users = allUsers,
            orders = core.orders,
            commissions = core.commissions,
            submissions = ops.submissions,
            withdrawals = ops.withdrawals
        )

        val relevantNotifications = if (user == null) {
            emptyList()
        } else if (isAdmin) {
            ops.notifications.filter { it.recipientUserId == "ADMIN" || it.recipientUserId == user.id }
        } else {
            ops.notifications.filter { it.recipientUserId == user.id || it.recipientUserId == "ALL" }
        }

        CommissionWalaUiState(
            isInitializing = transient.isInitializing,
            isBusy = transient.isBusy,
            currentUser = user,
            isAdminMode = isAdmin,
            products = core.products,
            orders = core.orders,
            commissions = core.commissions,
            tasks = ops.tasks,
            taskSubmissions = ops.submissions,
            withdrawals = ops.withdrawals,
            allUsers = allUsers,
            notifications = relevantNotifications,
            unreadNotificationCount = relevantNotifications.count { !it.isRead },
            appConfig = ops.config,
            walletSummary = wallet,
            adminSummary = adminSummary,
            authError = transient.authError,
            snackbarMessage = transient.snackbarMessage,
            qrModal = transient.qrModal,
            checkoutDraft = transient.checkoutDraft,
            lastOrderOutcome = transient.lastOrderOutcome,
            showNotificationsSheet = transient.showNotificationsSheet
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = CommissionWalaUiState()
    )

    init {
        viewModelScope.launch {
            repository.initialize()
            transientState.update { it.copy(isInitializing = false) }
        }
    }

    fun clearSnackbar() {
        transientState.update { it.copy(snackbarMessage = null) }
    }

    fun showMessage(message: String) {
        transientState.update { it.copy(snackbarMessage = message) }
    }

    fun clearAuthError() {
        transientState.update { it.copy(authError = null) }
    }

    // --- AUTH & WORKSPACE ---

    fun login(email: String, password: String, asAdmin: Boolean) {
        viewModelScope.launch {
            transientState.update { it.copy(isBusy = true, authError = null) }
            val result = repository.login(email, password, requireAdminRole = asAdmin)
            result.onSuccess { user ->
                transientState.update {
                    it.copy(
                        isBusy = false,
                        authError = null,
                        snackbarMessage = "Welcome back, ${user.fullName}!"
                    )
                }
            }.onFailure { err ->
                transientState.update {
                    it.copy(isBusy = false, authError = err.message ?: "Sign in failed")
                }
            }
        }
    }

    fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        referredByCode: String,
        upiId: String
    ) {
        viewModelScope.launch {
            transientState.update { it.copy(isBusy = true, authError = null) }
            val result = repository.registerUser(fullName, email, phone, password, referredByCode, upiId)
            result.onSuccess { user ->
                transientState.update {
                    it.copy(
                        isBusy = false,
                        authError = null,
                        snackbarMessage = "Account created! Referral ID: ${user.referralCode}"
                    )
                }
            }.onFailure { err ->
                transientState.update {
                    it.copy(isBusy = false, authError = err.message ?: "Registration failed")
                }
            }
        }
    }

    fun quickSwitchDemoUser(userId: String, targetMode: String) {
        viewModelScope.launch {
            repository.quickSignInDemoUser(userId, targetMode)
                .onSuccess { user ->
                    val label = if (targetMode == "ADMIN") "Admin Command Center" else user.fullName
                    transientState.update { it.copy(snackbarMessage = "Switched to $label") }
                }
                .onFailure { err ->
                    transientState.update { it.copy(snackbarMessage = err.message) }
                }
        }
    }

    fun toggleAdminWorkspace(enableAdmin: Boolean) {
        viewModelScope.launch {
            val user = uiState.value.currentUser
            if (enableAdmin && user?.role != UserRole.ADMIN) {
                // Switch to seeded Owner Admin account when requested from role switcher
                repository.quickSignInDemoUser("usr_admin_01", "ADMIN")
                transientState.update { it.copy(snackbarMessage = "Entered Admin Command Center") }
            } else {
                repository.setWorkspaceMode(if (enableAdmin) "ADMIN" else "USER")
                transientState.update {
                    it.copy(snackbarMessage = if (enableAdmin) "Admin Mode Active" else "Affiliate User Mode Active")
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            transientState.update {
                it.copy(
                    qrModal = null,
                    checkoutDraft = null,
                    lastOrderOutcome = null,
                    showNotificationsSheet = false
                )
            }
        }
    }

    // --- QR STUDIO & SCANNER ---

    fun openQrModal(product: ProductEntity, asAdminMasterQr: Boolean = false) {
        val user = uiState.value.currentUser
        val refCode = if (asAdminMasterQr) null else user?.referralCode
        val affiliateName = if (asAdminMasterQr) null else user?.fullName
        transientState.update {
            it.copy(qrModal = QrModalContext(product, refCode, affiliateName))
        }
    }

    fun dismissQrModal() {
        transientState.update { it.copy(qrModal = null) }
    }

    fun handleScannedOrEnteredQr(rawQrText: String) {
        viewModelScope.launch {
            repository.resolveProductFromQr(rawQrText)
                .onSuccess { (product, referrer) ->
                    transientState.update {
                        it.copy(
                            qrModal = null,
                            lastOrderOutcome = null,
                            checkoutDraft = CheckoutDraft(
                                product = product,
                                referralCode = referrer?.referralCode,
                                referrerName = referrer?.fullName,
                                scannedFromQr = true
                            )
                        )
                    }
                }
                .onFailure { err ->
                    transientState.update {
                        it.copy(snackbarMessage = err.message ?: "Could not recognize QR code")
                    }
                }
        }
    }

    fun openCheckoutForProduct(product: ProductEntity, referralCodeOverride: String? = null) {
        val user = uiState.value.currentUser
        val codeToUse = referralCodeOverride ?: user?.referralCode
        val referrer = uiState.value.allUsers.find {
            it.referralCode.equals(codeToUse, ignoreCase = true)
        }
        transientState.update {
            it.copy(
                qrModal = null,
                lastOrderOutcome = null,
                checkoutDraft = CheckoutDraft(
                    product = product,
                    referralCode = referrer?.referralCode ?: codeToUse,
                    referrerName = referrer?.fullName,
                    scannedFromQr = false
                )
            )
        }
    }

    fun dismissCheckout() {
        transientState.update { it.copy(checkoutDraft = null, lastOrderOutcome = null) }
    }

    fun placeOrder(
        productId: String,
        quantity: Int,
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        city: String,
        pincode: String,
        paymentMethod: String,
        referralCodeInput: String
    ) {
        viewModelScope.launch {
            transientState.update { it.copy(isBusy = true) }
            val result = repository.placeReferralOrder(
                productId = productId,
                quantity = quantity,
                customerName = customerName,
                customerPhone = customerPhone,
                deliveryAddress = deliveryAddress,
                city = city,
                pincode = pincode,
                paymentMethod = paymentMethod,
                referralCodeInput = referralCodeInput,
                currentLoggedInUserId = uiState.value.currentUser?.id
            )
            result.onSuccess { outcome ->
                transientState.update {
                    it.copy(
                        isBusy = false,
                        lastOrderOutcome = outcome,
                        snackbarMessage = "Order ${outcome.order.orderNumber} placed successfully!"
                    )
                }
            }.onFailure { err ->
                transientState.update {
                    it.copy(isBusy = false, snackbarMessage = err.message ?: "Order failed")
                }
            }
        }
    }

    // --- ADMIN PRODUCT CRUD ---

    fun saveProduct(
        existingId: String?,
        name: String,
        description: String,
        category: String,
        priceInr: Double,
        stock: Int,
        commissionType: CommissionType,
        commissionValue: Double,
        returnWindowDays: Int,
        imageUri: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.saveProduct(
                existingId = existingId,
                name = name,
                description = description,
                category = category,
                priceInr = priceInr,
                stock = stock,
                commissionType = commissionType,
                commissionValue = commissionValue,
                returnWindowDays = returnWindowDays,
                imageUri = imageUri
            ).onSuccess { product ->
                transientState.update {
                    it.copy(snackbarMessage = "Saved '${product.name}' (${product.skuCode})")
                }
                onSuccess()
            }.onFailure { err ->
                transientState.update {
                    it.copy(snackbarMessage = err.message ?: "Failed to save product")
                }
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
            transientState.update { it.copy(snackbarMessage = "Product deleted from catalog") }
        }
    }

    // --- ADMIN ORDER & COMMISSION MANAGEMENT ---

    fun updateOrderStatus(orderId: String, status: OrderStatus) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, status)
                .onSuccess {
                    transientState.update {
                        it.copy(snackbarMessage = "Order updated to ${status.label}")
                    }
                }
                .onFailure { err ->
                    transientState.update { it.copy(snackbarMessage = err.message) }
                }
        }
    }

    fun approveCommission(commissionId: String) {
        viewModelScope.launch {
            repository.approveCommission(commissionId)
                .onSuccess {
                    transientState.update {
                        it.copy(snackbarMessage = "Commission approved and credited to affiliate wallet!")
                    }
                }
                .onFailure { err ->
                    transientState.update { it.copy(snackbarMessage = err.message) }
                }
        }
    }

    fun rejectCommission(commissionId: String, reason: String) {
        viewModelScope.launch {
            repository.rejectCommission(commissionId, reason)
                .onSuccess {
                    transientState.update { it.copy(snackbarMessage = "Commission claim rejected") }
                }
                .onFailure { err ->
                    transientState.update { it.copy(snackbarMessage = err.message) }
                }
        }
    }

    // --- TASK MANAGEMENT & PROOF SUBMISSION ---

    fun saveTask(
        existingId: String?,
        title: String,
        description: String,
        instructions: String,
        category: TaskCategory,
        rewardInr: Double,
        deadlineDays: Int,
        imageUri: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.saveTask(
                existingId = existingId,
                title = title,
                description = description,
                instructions = instructions,
                category = category,
                rewardInr = rewardInr,
                deadlineDaysFromNow = deadlineDays,
                imageUri = imageUri
            ).onSuccess { task ->
                transientState.update {
                    it.copy(snackbarMessage = "Task '${task.title}' published!")
                }
                onSuccess()
            }.onFailure { err ->
                transientState.update {
                    it.copy(snackbarMessage = err.message ?: "Failed to save task")
                }
            }
        }
    }

    fun toggleTaskActive(taskId: String, active: Boolean) {
        viewModelScope.launch {
            repository.toggleTaskActive(taskId, active)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
            transientState.update { it.copy(snackbarMessage = "Task deleted") }
        }
    }

    fun submitTaskProof(
        taskId: String,
        proofImageUri: String?,
        proofNotes: String,
        proofLink: String,
        onSuccess: () -> Unit
    ) {
        val uid = uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.submitTaskProof(taskId, uid, proofImageUri, proofNotes, proofLink)
                .onSuccess {
                    transientState.update {
                        it.copy(snackbarMessage = "Proof submitted! Admin will review & credit reward.")
                    }
                    onSuccess()
                }
                .onFailure { err ->
                    transientState.update {
                        it.copy(snackbarMessage = err.message ?: "Submission failed")
                    }
                }
        }
    }

    fun reviewTaskSubmission(submissionId: String, approve: Boolean, adminNote: String) {
        viewModelScope.launch {
            repository.reviewTaskSubmission(submissionId, approve, adminNote)
                .onSuccess {
                    transientState.update {
                        it.copy(
                            snackbarMessage = if (approve) {
                                "Task approved & reward credited to user wallet!"
                            } else {
                                "Task submission rejected"
                            }
                        )
                    }
                }
                .onFailure { err ->
                    transientState.update { it.copy(snackbarMessage = err.message) }
                }
        }
    }

    // --- WALLET, UPI & WITHDRAWALS ---

    fun updateUpiDetails(upiId: String, accountName: String) {
        val uid = uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.updateUserUpi(uid, upiId, accountName)
                .onSuccess {
                    transientState.update { it.copy(snackbarMessage = "UPI ID updated: $upiId") }
                }
                .onFailure { err ->
                    transientState.update { it.copy(snackbarMessage = err.message) }
                }
        }
    }

    fun requestWithdrawal(amountInr: Double) {
        val uid = uiState.value.currentUser?.id ?: return
        val available = uiState.value.walletSummary.availableBalanceInr
        viewModelScope.launch {
            repository.requestWithdrawal(uid, amountInr, available)
                .onSuccess {
                    transientState.update {
                        it.copy(
                            snackbarMessage = "Withdrawal request of ${Formatters.formatInr(amountInr)} submitted!"
                        )
                    }
                }
                .onFailure { err ->
                    transientState.update { it.copy(snackbarMessage = err.message) }
                }
        }
    }

    fun processWithdrawal(withdrawalId: String, approve: Boolean, utrReference: String, adminNote: String) {
        viewModelScope.launch {
            repository.processWithdrawal(withdrawalId, approve, utrReference, adminNote)
                .onSuccess {
                    transientState.update {
                        it.copy(
                            snackbarMessage = if (approve) "Withdrawal approved & marked paid!" else "Withdrawal rejected & refunded"
                        )
                    }
                }
                .onFailure { err ->
                    transientState.update { it.copy(snackbarMessage = err.message) }
                }
        }
    }

    fun toggleUserActive(userId: String, active: Boolean) {
        viewModelScope.launch {
            repository.toggleUserActive(userId, active)
                .onSuccess {
                    transientState.update {
                        it.copy(snackbarMessage = if (active) "User activated" else "User suspended")
                    }
                }
                .onFailure { err ->
                    transientState.update { it.copy(snackbarMessage = err.message) }
                }
        }
    }

    fun updateAdminConfig(minWithdrawalInr: Double, requireManualCommissionApproval: Boolean) {
        viewModelScope.launch {
            repository.updateAdminConfig(minWithdrawalInr, requireManualCommissionApproval)
                .onSuccess {
                    transientState.update {
                        it.copy(snackbarMessage = "Settings updated (Min withdrawal: ${Formatters.formatInr(minWithdrawalInr)})")
                    }
                }
                .onFailure { err ->
                    transientState.update { it.copy(snackbarMessage = err.message) }
                }
        }
    }

    // --- NOTIFICATIONS ---

    fun toggleNotificationsSheet(show: Boolean) {
        transientState.update { it.copy(showNotificationsSheet = show) }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    fun markAllNotificationsRead() {
        val uid = uiState.value.currentUser?.id ?: return
        val recipient = if (uiState.value.isAdminMode) "ADMIN" else uid
        viewModelScope.launch {
            repository.markAllNotificationsRead(recipient)
        }
    }

    class Factory(private val repository: CommissionWalaRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
