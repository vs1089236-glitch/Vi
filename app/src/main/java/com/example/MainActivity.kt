package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.UserEntity
import com.example.ui.components.CheckoutSheet
import com.example.ui.components.CommissionWalaTopBar
import com.example.ui.components.NotificationsSheet
import com.example.ui.components.QrStudioSheet
import com.example.ui.screens.AdminOrdersScreen
import com.example.ui.screens.AdminOverviewScreen
import com.example.ui.screens.AdminPayoutsAndUsersScreen
import com.example.ui.screens.AdminProductsScreen
import com.example.ui.screens.AdminTasksScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ProductsCatalogScreen
import com.example.ui.screens.QrScannerScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.UserHomeScreen
import com.example.ui.screens.WalletAndProfileScreen
import com.example.ui.theme.AppBackgroundLight
import com.example.ui.theme.BrandBlueSecondary
import com.example.ui.theme.BrandOnPurpleContainer
import com.example.ui.theme.BrandPurpleContainer
import com.example.ui.theme.BrandPurplePrimary
import com.example.ui.theme.CommissionWalaTheme
import com.example.ui.theme.TextMutedLight
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.viewmodel.CommissionWalaUiState
import com.example.ui.viewmodel.MainViewModel
import kotlinx.serialization.Serializable

// --- TYPE-SAFE NAVIGATION ROUTES ---
@Serializable object HomeRoute
@Serializable object ProductsRoute
@Serializable object ScannerRoute
@Serializable object TasksRoute
@Serializable object WalletRoute

@Serializable object AdminOverviewRoute
@Serializable object AdminProductsRoute
@Serializable object AdminOrdersRoute
@Serializable object AdminTasksRoute
@Serializable object AdminPayoutsRoute

private data class NavTabItem<T : Any>(
    val route: T,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val userNavTabs = listOf(
    NavTabItem(HomeRoute, "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_user_home"),
    NavTabItem(ProductsRoute, "Catalog", Icons.Filled.Storefront, Icons.Outlined.Storefront, "nav_user_products"),
    NavTabItem(ScannerRoute, "Scan QR", Icons.Filled.QrCodeScanner, Icons.Outlined.QrCodeScanner, "nav_user_scanner"),
    NavTabItem(TasksRoute, "Tasks", Icons.Filled.AssignmentTurnedIn, Icons.Outlined.AssignmentTurnedIn, "nav_user_tasks"),
    NavTabItem(WalletRoute, "Wallet", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_user_wallet")
)

private val adminNavTabs = listOf(
    NavTabItem(AdminOverviewRoute, "Overview", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_admin_overview"),
    NavTabItem(AdminProductsRoute, "Products", Icons.Filled.Inventory2, Icons.Outlined.Inventory2, "nav_admin_products"),
    NavTabItem(AdminOrdersRoute, "Orders", Icons.Filled.LocalShipping, Icons.Outlined.LocalShipping, "nav_admin_orders"),
    NavTabItem(AdminTasksRoute, "Tasks", Icons.Filled.AssignmentTurnedIn, Icons.Outlined.AssignmentTurnedIn, "nav_admin_tasks"),
    NavTabItem(AdminPayoutsRoute, "Payouts", Icons.Filled.Payments, Icons.Outlined.Payments, "nav_admin_payouts")
)

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory((application as CommissionWalaApp).repository)
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingDeepLink(intent)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(uiState.snackbarMessage) {
                val msg = uiState.snackbarMessage
                if (!msg.isNullOrBlank()) {
                    snackbarHostState.showSnackbar(
                        message = msg,
                        duration = SnackbarDuration.Short
                    )
                    viewModel.clearSnackbar()
                }
            }

            CommissionWalaTheme {
                when {
                    uiState.isInitializing && uiState.currentUser == null -> {
                        SplashLoadingScreen()
                    }
                    uiState.currentUser == null -> {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = AppBackgroundLight,
                            contentWindowInsets = WindowInsets.safeDrawing,
                            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                        ) { innerPadding ->
                            AuthScreen(
                                isBusy = uiState.isBusy,
                                authError = uiState.authError,
                                onLogin = viewModel::login,
                                onRegister = viewModel::register,
                                onQuickDemoLogin = viewModel::quickSwitchDemoUser,
                                onClearError = viewModel::clearAuthError,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                    else -> {
                        AuthenticatedAppShell(
                            user = uiState.currentUser!!,
                            uiState = uiState,
                            widthSizeClass = windowSizeClass.widthSizeClass,
                            snackbarHostState = snackbarHostState,
                            viewModel = viewModel
                        )
                    }
                }

                // Modal Sheets (QR Studio, Checkout, Notifications)
                uiState.qrModal?.let { qrContext ->
                    QrStudioSheet(
                        qrContext = qrContext,
                        onDismiss = viewModel::dismissQrModal,
                        onShowMessage = viewModel::showMessage,
                        onSimulateCustomerCheckout = {
                            viewModel.openCheckoutForProduct(
                                product = qrContext.product,
                                referralCodeOverride = qrContext.referralCode
                            )
                        }
                    )
                }

                uiState.checkoutDraft?.let { draft ->
                    CheckoutSheet(
                        draft = draft,
                        outcome = uiState.lastOrderOutcome,
                        currentUserId = uiState.currentUser?.id,
                        isBusy = uiState.isBusy,
                        onDismiss = viewModel::dismissCheckout,
                        onPlaceOrder = viewModel::placeOrder
                    )
                }

                if (uiState.showNotificationsSheet) {
                    NotificationsSheet(
                        notifications = uiState.notifications,
                        onMarkRead = viewModel::markNotificationRead,
                        onMarkAllRead = viewModel::markAllNotificationsRead,
                        onDismiss = { viewModel.toggleNotificationsSheet(false) }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingDeepLink(intent)
    }

    private fun handleIncomingDeepLink(intent: Intent?) {
        val dataUri = intent?.dataString ?: return
        if (dataUri.contains("commissionwala", ignoreCase = true)) {
            viewModel.handleScannedOrEnteredQr(dataUri)
        }
    }
}

@Composable
private fun SplashLoadingScreen() {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppBackgroundLight,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(listOf(BrandPurplePrimary, BrandBlueSecondary))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CW",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(color = BrandPurplePrimary)
            }
        }
    }
}

@Composable
private fun AuthenticatedAppShell(
    user: UserEntity,
    uiState: CommissionWalaUiState,
    widthSizeClass: WindowWidthSizeClass,
    snackbarHostState: SnackbarHostState,
    viewModel: MainViewModel
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isCompact = widthSizeClass == WindowWidthSizeClass.Compact

    // Keep NavHost synced when switching between Affiliate User mode and Admin Command Center
    LaunchedEffect(uiState.isAdminMode) {
        val targetStart: Any = if (uiState.isAdminMode) AdminOverviewRoute else HomeRoute
        val alreadyInMode = if (uiState.isAdminMode) {
            adminNavTabs.any { currentDestination?.hasRoute(it.route::class) == true }
        } else {
            userNavTabs.any { currentDestination?.hasRoute(it.route::class) == true }
        }
        if (!alreadyInMode) {
            navController.navigate(targetStart) {
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }

    // Ensure back button returns to User Home if currently in Admin Overview
    BackHandler(enabled = uiState.isAdminMode && currentDestination?.hasRoute(AdminOverviewRoute::class) == true) {
        viewModel.toggleAdminWorkspace(false)
    }

    val topTitle = if (uiState.isAdminMode) "Admin Studio" else "CommissionWala"
    val topSubtitle = if (uiState.isAdminMode) {
        "Owner Dashboard • ${uiState.adminSummary.pendingOrdersCount} pending orders"
    } else {
        "${user.fullName} • ${user.referralCode}"
    }

    val navigateToTab: (Any) -> Unit = { destination ->
        navController.navigate(destination) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val userCommissions = remember(uiState.commissions, user.id) {
        uiState.commissions.filter { it.affiliateUserId == user.id }
    }
    val userSubmissions = remember(uiState.taskSubmissions, user.id) {
        uiState.taskSubmissions.filter { it.userId == user.id }
    }
    val userWithdrawals = remember(uiState.withdrawals, user.id) {
        uiState.withdrawals.filter { it.userId == user.id }
    }

    val navHostContent: @Composable (Modifier) -> Unit = { contentModifier ->
        Box(
            modifier = contentModifier
                .fillMaxSize()
                .background(AppBackgroundLight),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 960.dp)
                    .fillMaxSize()
            ) {
                NavHost(
                    navController = navController,
                    startDestination = if (uiState.isAdminMode) AdminOverviewRoute else HomeRoute
                ) {
                    composable<HomeRoute> {
                        UserHomeScreen(
                            user = user,
                            wallet = uiState.walletSummary,
                            announcement = uiState.appConfig.platformAnnouncement,
                            products = uiState.products,
                            userCommissions = userCommissions,
                            userWithdrawals = userWithdrawals,
                            activeTasksCount = uiState.tasks.count { it.isActive },
                            onOpenScanner = { navigateToTab(ScannerRoute) },
                            onOpenProducts = { navigateToTab(ProductsRoute) },
                            onOpenTasks = { navigateToTab(TasksRoute) },
                            onOpenWallet = { navigateToTab(WalletRoute) },
                            onOpenProductQr = { prod -> viewModel.openQrModal(prod, asAdminMasterQr = false) },
                            onShowMessage = viewModel::showMessage
                        )
                    }
                    composable<ProductsRoute> {
                        ProductsCatalogScreen(
                            products = uiState.products,
                            userReferralCode = user.referralCode,
                            onOpenQrStudio = { prod -> viewModel.openQrModal(prod, asAdminMasterQr = false) },
                            onOrderProduct = { prod -> viewModel.openCheckoutForProduct(prod) },
                            onShowMessage = viewModel::showMessage
                        )
                    }
                    composable<ScannerRoute> {
                        QrScannerScreen(
                            products = uiState.products,
                            allUsers = uiState.allUsers,
                            currentUser = user,
                            onQrDecoded = viewModel::handleScannedOrEnteredQr,
                            onShowMessage = viewModel::showMessage
                        )
                    }
                    composable<TasksRoute> {
                        TasksScreen(
                            tasks = uiState.tasks,
                            userSubmissions = userSubmissions,
                            onSubmitProof = viewModel::submitTaskProof
                        )
                    }
                    composable<WalletRoute> {
                        WalletAndProfileScreen(
                            user = user,
                            wallet = uiState.walletSummary,
                            minWithdrawalInr = uiState.appConfig.minWithdrawalInr,
                            userCommissions = userCommissions,
                            userWithdrawals = userWithdrawals,
                            allUsers = uiState.allUsers,
                            onUpdateUpi = viewModel::updateUpiDetails,
                            onRequestWithdrawal = viewModel::requestWithdrawal,
                            onQuickSwitchUser = viewModel::quickSwitchDemoUser,
                            onLogout = viewModel::logout,
                            onShowMessage = viewModel::showMessage
                        )
                    }

                    // --- ADMIN ROUTES ---
                    composable<AdminOverviewRoute> {
                        AdminOverviewScreen(
                            summary = uiState.adminSummary,
                            appConfig = uiState.appConfig,
                            products = uiState.products,
                            orders = uiState.orders,
                            commissions = uiState.commissions,
                            withdrawals = uiState.withdrawals,
                            onNavigateToOrders = { navigateToTab(AdminOrdersRoute) },
                            onNavigateToTasks = { navigateToTab(AdminTasksRoute) },
                            onNavigateToPayouts = { navigateToTab(AdminPayoutsRoute) },
                            onSaveConfig = viewModel::updateAdminConfig,
                            onLogout = viewModel::logout,
                            onShowMessage = viewModel::showMessage
                        )
                    }
                    composable<AdminProductsRoute> {
                        AdminProductsScreen(
                            products = uiState.products,
                            onOpenMasterQr = { prod -> viewModel.openQrModal(prod, asAdminMasterQr = true) },
                            onSaveProduct = viewModel::saveProduct,
                            onDeleteProduct = viewModel::deleteProduct,
                            onShowMessage = viewModel::showMessage
                        )
                    }
                    composable<AdminOrdersRoute> {
                        AdminOrdersScreen(
                            orders = uiState.orders,
                            commissions = uiState.commissions,
                            users = uiState.allUsers,
                            onUpdateOrderStatus = viewModel::updateOrderStatus
                        )
                    }
                    composable<AdminTasksRoute> {
                        AdminTasksScreen(
                            tasks = uiState.tasks,
                            submissions = uiState.taskSubmissions,
                            onSaveTask = viewModel::saveTask,
                            onToggleTaskActive = viewModel::toggleTaskActive,
                            onDeleteTask = viewModel::deleteTask,
                            onReviewSubmission = viewModel::reviewTaskSubmission
                        )
                    }
                    composable<AdminPayoutsRoute> {
                        AdminPayoutsAndUsersScreen(
                            commissions = uiState.commissions,
                            withdrawals = uiState.withdrawals,
                            users = uiState.allUsers,
                            onApproveCommission = viewModel::approveCommission,
                            onRejectCommission = viewModel::rejectCommission,
                            onProcessWithdrawal = viewModel::processWithdrawal,
                            onToggleUserActive = viewModel::toggleUserActive
                        )
                    }
                }
            }
        }
    }

    if (isCompact) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = AppBackgroundLight,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp
                ) {
                    CommissionWalaTopBar(
                        title = topTitle,
                        subtitle = topSubtitle,
                        isAdminMode = uiState.isAdminMode,
                        unreadNotifications = uiState.unreadNotificationCount,
                        onNotificationsClick = { viewModel.toggleNotificationsSheet(true) },
                        onSwitchWorkspaceClick = { viewModel.toggleAdminWorkspace(!uiState.isAdminMode) },
                        modifier = Modifier.statusBarsPadding()
                    )
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 2.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    if (uiState.isAdminMode) {
                        adminNavTabs.forEach { tab ->
                            val selected = currentDestination?.hasRoute(tab.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = BrandPurplePrimary,
                                    selectedTextColor = BrandPurplePrimary,
                                    indicatorColor = BrandPurpleContainer,
                                    unselectedIconColor = TextMutedLight,
                                    unselectedTextColor = TextMutedLight
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag(tab.testTag)
                            )
                        }
                    } else {
                        userNavTabs.forEach { tab ->
                            val selected = currentDestination?.hasRoute(tab.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = BrandPurplePrimary,
                                    selectedTextColor = BrandPurplePrimary,
                                    indicatorColor = BrandPurpleContainer,
                                    unselectedIconColor = TextMutedLight,
                                    unselectedTextColor = TextMutedLight
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            navHostContent(Modifier.padding(innerPadding))
        }
    } else {
        // Medium & Expanded (Tablets / Foldables): NavigationRail + Content Scaffold
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackgroundLight)
        ) {
            NavigationRail(
                containerColor = Color.White,
                modifier = Modifier
                    .fillMaxHeight()
                    .safeDrawingPadding()
                    .padding(vertical = 8.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                if (uiState.isAdminMode) {
                    adminNavTabs.forEach { tab ->
                        val selected = currentDestination?.hasRoute(tab.route::class) == true
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateToTab(tab.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = BrandPurplePrimary,
                                selectedTextColor = BrandPurplePrimary,
                                indicatorColor = BrandPurpleContainer
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag(tab.testTag)
                        )
                    }
                } else {
                    userNavTabs.forEach { tab ->
                        val selected = currentDestination?.hasRoute(tab.route::class) == true
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateToTab(tab.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = BrandPurplePrimary,
                                selectedTextColor = BrandPurplePrimary,
                                indicatorColor = BrandPurpleContainer
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag(tab.testTag)
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                containerColor = AppBackgroundLight,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp
                    ) {
                        CommissionWalaTopBar(
                            title = topTitle,
                            subtitle = topSubtitle,
                            isAdminMode = uiState.isAdminMode,
                            unreadNotifications = uiState.unreadNotificationCount,
                            onNotificationsClick = { viewModel.toggleNotificationsSheet(true) },
                            onSwitchWorkspaceClick = { viewModel.toggleAdminWorkspace(!uiState.isAdminMode) },
                            modifier = Modifier.statusBarsPadding()
                        )
                    }
                }
            ) { innerPadding ->
                navHostContent(Modifier.padding(innerPadding))
            }
        }
    }
}
