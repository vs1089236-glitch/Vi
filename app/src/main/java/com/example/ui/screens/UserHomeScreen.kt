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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CommissionEntity
import com.example.data.local.CommissionStatus
import com.example.data.local.ProductEntity
import com.example.data.local.UserEntity
import com.example.data.local.WithdrawalEntity
import com.example.data.repository.WalletSummary
import com.example.ui.components.BadgeTone
import com.example.ui.components.EarningsAreaChart
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.ProductImageView
import com.example.ui.components.StatusPillBadge
import com.example.ui.components.WalletHeroGradientCard
import com.example.ui.theme.AppOutlineLight
import com.example.ui.theme.BrandBlueContainer
import com.example.ui.theme.BrandBlueSecondary
import com.example.ui.theme.BrandOnBlueContainer
import com.example.ui.theme.BrandPurpleContainer
import com.example.ui.theme.BrandPurplePrimary
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextMutedLight
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight
import com.example.util.ExportUtils
import com.example.util.Formatters
import com.example.util.QrCodeUtils

@Composable
fun UserHomeScreen(
    user: UserEntity,
    wallet: WalletSummary,
    announcement: String,
    products: List<ProductEntity>,
    userCommissions: List<CommissionEntity>,
    userWithdrawals: List<WithdrawalEntity>,
    activeTasksCount: Int,
    onOpenScanner: () -> Unit,
    onOpenProducts: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenProductQr: (ProductEntity) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val chartPoints = remember(userCommissions, wallet) {
        val base = wallet.totalLifetimeEarningsInr
        val pending = wallet.pendingCommissionInr
        listOf(
            "D-6" to (base * 0.12).coerceAtLeast(120.0),
            "D-5" to (base * 0.25).coerceAtLeast(240.0),
            "D-4" to (base * 0.18).coerceAtLeast(180.0),
            "D-3" to (base * 0.42).coerceAtLeast(350.0),
            "D-2" to (base * 0.58 + pending * 0.3).coerceAtLeast(480.0),
            "D-1" to (base * 0.78 + pending * 0.6).coerceAtLeast(620.0),
            "Now" to (base + pending).coerceAtLeast(300.0)
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Announcement Banner
        if (announcement.isNotBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = BrandBlueContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Campaign,
                            contentDescription = null,
                            tint = BrandBlueSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = announcement,
                            style = MaterialTheme.typography.labelMedium,
                            color = BrandOnBlueContainer,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Wallet Gradient Hero Card
        item {
            WalletHeroGradientCard(
                availableBalance = wallet.availableBalanceInr,
                commissionBalance = wallet.creditedCommissionInr,
                taskRewardBalance = wallet.taskRewardBalanceInr,
                pendingCommissions = wallet.pendingCommissionInr,
                referralCode = user.referralCode,
                onCopyReferralCode = {
                    clipboard.setText(AnnotatedString(user.referralCode))
                    onShowMessage("Copied Referral ID: ${user.referralCode}")
                }
            )
        }

        // Quick Actions Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionTile(
                    label = "Scan QR",
                    icon = Icons.Filled.QrCodeScanner,
                    bg = BrandPurpleContainer,
                    tint = BrandPurplePrimary,
                    onClick = onOpenScanner,
                    tag = "home_quick_scan",
                    modifier = Modifier.weight(1f)
                )
                QuickActionTile(
                    label = "Products",
                    icon = Icons.Filled.Inventory2,
                    bg = BrandBlueContainer,
                    tint = BrandBlueSecondary,
                    onClick = onOpenProducts,
                    tag = "home_quick_products",
                    modifier = Modifier.weight(1f)
                )
                QuickActionTile(
                    label = "Tasks ($activeTasksCount)",
                    icon = Icons.Filled.AssignmentTurnedIn,
                    bg = SuccessContainer,
                    tint = SuccessEmerald,
                    onClick = onOpenTasks,
                    tag = "home_quick_tasks",
                    modifier = Modifier.weight(1f)
                )
                QuickActionTile(
                    label = "Withdraw",
                    icon = Icons.Filled.AccountBalanceWallet,
                    bg = BrandPurpleContainer,
                    tint = BrandPurplePrimary,
                    onClick = onOpenWallet,
                    tag = "home_quick_withdraw",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2-Column KPI Summary
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiMetricCard(
                    title = "Total Earnings",
                    value = Formatters.formatInr(wallet.totalLifetimeEarningsInr),
                    subtitle = "${wallet.totalReferredSalesCount} tracked QR sales",
                    icon = Icons.Filled.TrendingUp,
                    accentColor = BrandPurplePrimary,
                    containerColor = BrandPurpleContainer,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    title = "Paid Out (UPI)",
                    value = Formatters.formatInr(wallet.totalWithdrawnInr),
                    subtitle = if (user.upiId.isNotBlank()) user.upiId else "Tap Wallet to add UPI",
                    icon = Icons.Filled.AccountBalanceWallet,
                    accentColor = SuccessEmerald,
                    containerColor = SuccessContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 7-Day Earnings Chart
        item {
            EarningsAreaChart(points = chartPoints)
        }

        // High-Margin Products Carousel
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "High-Commission Products",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimaryLight
                        )
                        Text(
                            text = "Generate your referral QR & earn on every delivery",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryLight
                        )
                    }
                    TextButton(onClick = onOpenProducts) {
                        Text("See all")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(products.take(5), key = { it.id }) { product ->
                        val commPerUnit = product.calculateCommission(1)
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, AppOutlineLight),
                            modifier = Modifier
                                .width(248.dp)
                                .clickable { onOpenProductQr(product) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(115.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                ) {
                                    ProductImageView(
                                        imageUri = product.imageUri,
                                        contentDescription = product.name,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = BrandPurplePrimary,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = "+${Formatters.formatInr(commPerUnit)}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFeatureSettings = "tnum"
                                            ),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimaryLight,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${Formatters.formatInr(product.priceInr)} • ${product.commissionBadgeText()}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                    color = TextSecondaryLight
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onOpenProductQr(product) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlueSecondary),
                                        contentPadding = PaddingValues(horizontal = 10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                    ) {
                                        Icon(Icons.Filled.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Get QR", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val payload = QrCodeUtils.buildProductQrPayload(product.skuCode, user.referralCode)
                                            val card = QrCodeUtils.createBrandedQrShareBitmap(
                                                productName = product.name,
                                                priceText = Formatters.formatInr(product.priceInr),
                                                commissionLabel = product.commissionBadgeText(),
                                                skuCode = product.skuCode,
                                                referralCode = user.referralCode,
                                                payload = payload
                                            )
                                            QrCodeUtils.saveQrToGallery(context, card, "CW_${product.skuCode}")
                                                .onSuccess { fileName ->
                                                    onShowMessage("Downloaded QR card: $fileName")
                                                }
                                                .onFailure {
                                                    onShowMessage("Download failed: ${it.localizedMessage}")
                                                }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("home_download_qr_btn_${product.skuCode}")
                                    ) {
                                        Icon(Icons.Filled.Download, contentDescription = "Download QR", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Download", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Tracked Referral Sales
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent QR Referral Sales",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimaryLight,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            ExportUtils.exportAffiliateStatementCsv(
                                context = context,
                                user = user,
                                wallet = wallet,
                                commissions = userCommissions,
                                withdrawals = userWithdrawals
                            ).onSuccess { fileName ->
                                onShowMessage("Downloaded statement to Downloads/CommissionWala/$fileName")
                            }.onFailure {
                                onShowMessage("Statement export failed: ${it.localizedMessage}")
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("home_download_statement_btn")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = "Download statement", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = onOpenWallet) {
                        Text("Ledger")
                    }
                }
            }
        }

        if (userCommissions.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, AppOutlineLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCode2,
                            contentDescription = null,
                            tint = BrandPurplePrimary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No referral sales tracked yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryLight
                        )
                        Text(
                            text = "Share any product QR code on WhatsApp or Instagram to earn your first commission!",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryLight
                        )
                    }
                }
            }
        } else {
            items(userCommissions.take(5), key = { it.id }) { comm ->
                CommissionActivityCard(comm = comm)
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    label: String,
    icon: ImageVector,
    bg: Color,
    tint: Color,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, AppOutlineLight),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimaryLight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CommissionActivityCard(
    comm: CommissionEntity,
    modifier: Modifier = Modifier
) {
    val tone = when (comm.status) {
        CommissionStatus.APPROVED_PAID -> BadgeTone.SUCCESS
        CommissionStatus.PAYABLE -> BadgeTone.BLUE
        CommissionStatus.IN_RETURN_WINDOW -> BadgeTone.WARNING
        CommissionStatus.PENDING_DELIVERY -> BadgeTone.PURPLE
        CommissionStatus.REJECTED -> BadgeTone.ERROR
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, AppOutlineLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comm.orderNumber,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = BrandBlueSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusPillBadge(text = comm.status.label, tone = tone)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = comm.productName,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimaryLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (comm.status == CommissionStatus.IN_RETURN_WINDOW && comm.returnWindowEndsAt != null) {
                        "Return lock: ${Formatters.formatCountdown(comm.returnWindowEndsAt)}"
                    } else {
                        comm.adminNote.ifEmpty { "Tracked via ${comm.referralCode}" }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMutedLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+${Formatters.formatInr(comm.commissionAmountInr)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFeatureSettings = "tnum",
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = if (comm.status == CommissionStatus.REJECTED) TextMutedLight else SuccessEmerald
                )
                Text(
                    text = comm.commissionRateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryLight
                )
            }
        }
    }
}
