package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.AppConfigEntity
import com.example.data.local.CommissionEntity
import com.example.data.local.CommissionStatus
import com.example.data.local.OrderEntity
import com.example.data.local.OrderStatus
import com.example.data.local.ProductEntity
import com.example.data.local.WithdrawalEntity
import com.example.data.repository.AdminDashboardSummary
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.PlatformRevenueBarChart
import com.example.ui.theme.AppOutlineLight
import com.example.ui.theme.BrandBlueContainer
import com.example.ui.theme.BrandBlueSecondary
import com.example.ui.theme.BrandPurpleContainer
import com.example.ui.theme.BrandPurplePrimary
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningContainer
import com.example.util.ExportUtils
import com.example.util.Formatters

@Composable
fun AdminOverviewScreen(
    summary: AdminDashboardSummary,
    appConfig: AppConfigEntity,
    products: List<ProductEntity>,
    orders: List<OrderEntity>,
    commissions: List<CommissionEntity>,
    withdrawals: List<WithdrawalEntity>,
    onNavigateToOrders: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToPayouts: () -> Unit,
    onSaveConfig: (minWithdrawalInr: Double, requireManualCommissionApproval: Boolean) -> Unit,
    onLogout: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var minWithdrawalInput by remember(appConfig.minWithdrawalInr) {
        mutableStateOf(appConfig.minWithdrawalInr.toInt().toString())
    }
    var manualApproval by remember(appConfig.requireManualCommissionApproval) {
        mutableStateOf(appConfig.requireManualCommissionApproval)
    }

    val categoryBars = remember(products, orders, commissions) {
        val categories = products.map { it.category }.distinct().take(4)
        categories.map { cat ->
            val catProductIds = products.filter { it.category == cat }.map { it.id }.toSet()
            val catSales = orders
                .filter { it.productId in catProductIds && it.status != OrderStatus.CANCELLED }
                .sumOf { it.totalAmountInr }
            val catComm = commissions
                .filter { it.productId in catProductIds && it.status != CommissionStatus.REJECTED }
                .sumOf { it.commissionAmountInr }
            Triple(cat, catSales, catComm)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Owner Analytics & Controls",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimaryLight
                )
                Text(
                    text = "${summary.totalRegisteredUsersCount} registered affiliates • ${summary.totalOrdersCount} total orders",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight
                )
            }
        }

        // 2x2 KPI Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiMetricCard(
                        title = "Total Sales",
                        value = Formatters.formatInr(summary.totalSalesInr),
                        subtitle = "${summary.pendingOrdersCount} orders active",
                        icon = Icons.Filled.TrendingUp,
                        accentColor = BrandBlueSecondary,
                        containerColor = BrandBlueContainer,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "Commissions",
                        value = Formatters.formatInr(summary.totalCommissionsLiabilityInr),
                        subtitle = "${summary.pendingCommissionApprovalsCount} awaiting review",
                        icon = Icons.Filled.CurrencyRupee,
                        accentColor = BrandPurplePrimary,
                        containerColor = BrandPurpleContainer,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiMetricCard(
                        title = "Pending Payouts",
                        value = Formatters.formatInr(summary.pendingWithdrawalsInr),
                        subtitle = "${summary.pendingWithdrawalsCount} UPI requests",
                        icon = Icons.Filled.HourglassTop,
                        accentColor = WarningAmber,
                        containerColor = WarningContainer,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "Completed Payouts",
                        value = Formatters.formatInr(summary.completedWithdrawalsInr),
                        subtitle = "Settled via UPI",
                        icon = Icons.Filled.CheckCircle,
                        accentColor = SuccessEmerald,
                        containerColor = SuccessContainer,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedButton(
                    onClick = {
                        ExportUtils.exportAdminReportCsv(
                            context = context,
                            summary = summary,
                            orders = orders,
                            commissions = commissions,
                            withdrawals = withdrawals
                        ).onSuccess { fileName ->
                            onShowMessage("Downloaded report to Downloads/CommissionWala/$fileName")
                        }.onFailure {
                            onShowMessage("Could not export report: ${it.localizedMessage}")
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_download_report_btn")
                ) {
                    Icon(Icons.Filled.Download, contentDescription = "Download sales report", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download Sales & Payout Report (CSV)")
                }
            }
        }

        // Pending Action Queue
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, AppOutlineLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Action Queue",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimaryLight
                    )
                    AdminActionQueueRow(
                        title = "Orders to Fulfill",
                        count = summary.pendingOrdersCount,
                        icon = Icons.Filled.LocalShipping,
                        onClick = onNavigateToOrders,
                        tag = "admin_queue_orders"
                    )
                    AdminActionQueueRow(
                        title = "Task Proofs to Review",
                        count = summary.pendingTaskReviewsCount,
                        icon = Icons.Filled.AssignmentTurnedIn,
                        onClick = onNavigateToTasks,
                        tag = "admin_queue_tasks"
                    )
                    AdminActionQueueRow(
                        title = "Commissions & UPI Withdrawals",
                        count = summary.pendingCommissionApprovalsCount + summary.pendingWithdrawalsCount,
                        icon = Icons.Filled.AccountBalanceWallet,
                        onClick = onNavigateToPayouts,
                        tag = "admin_queue_payouts"
                    )
                }
            }
        }

        // Grouped Revenue & Commission Bar Chart
        item {
            PlatformRevenueBarChart(bars = categoryBars)
        }

        // Platform Settings Card (Configurable Minimum Withdrawal Limit)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, AppOutlineLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Settings, contentDescription = null, tint = BrandPurplePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Payout & Commission Rules",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryLight
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = minWithdrawalInput,
                        onValueChange = { minWithdrawalInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Minimum UPI Withdrawal Limit (₹)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_min_withdrawal_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Manual Commission Approval",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimaryLight
                            )
                            Text(
                                text = "Require Admin sign-off after delivery & return period",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryLight
                            )
                        }
                        Switch(
                            checked = manualApproval,
                            onCheckedChange = { manualApproval = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val minVal = minWithdrawalInput.toDoubleOrNull() ?: 100.0
                            onSaveConfig(minVal, manualApproval)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurplePrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_save_config_button")
                    ) {
                        Text("Save Platform Rules")
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onLogout,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ErrorCrimson.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("admin_logout_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = ErrorCrimson)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out of Admin Account", color = ErrorCrimson)
            }
        }
    }
}

@Composable
private fun AdminActionQueueRow(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8F9FE),
        border = BorderStroke(1.dp, AppOutlineLight),
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = BrandPurplePrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimaryLight)
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = if (count > 0) BrandPurplePrimary else Color(0xFFE2E8F0)
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (count > 0) Color.White else TextSecondaryLight,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
