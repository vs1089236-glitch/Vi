package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CommissionEntity
import com.example.data.local.UserEntity
import com.example.data.local.UserRole
import com.example.data.local.WithdrawalEntity
import com.example.data.local.WithdrawalStatus
import com.example.data.repository.WalletSummary
import com.example.ui.components.BadgeTone
import com.example.ui.components.StatusPillBadge
import com.example.ui.components.WalletHeroGradientCard
import com.example.ui.theme.AppOutlineLight
import com.example.ui.theme.BrandBlueContainer
import com.example.ui.theme.BrandBlueSecondary
import com.example.ui.theme.BrandPurpleContainer
import com.example.ui.theme.BrandPurplePrimary
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextMutedLight
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight
import com.example.util.ExportUtils
import com.example.util.Formatters

@Composable
fun WalletAndProfileScreen(
    user: UserEntity,
    wallet: WalletSummary,
    minWithdrawalInr: Double,
    userCommissions: List<CommissionEntity>,
    userWithdrawals: List<WithdrawalEntity>,
    allUsers: List<UserEntity>,
    onUpdateUpi: (upiId: String, accountName: String) -> Unit,
    onRequestWithdrawal: (amountInr: Double) -> Unit,
    onQuickSwitchUser: (userId: String, mode: String) -> Unit,
    onLogout: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Withdraw & UPI, 1 = Sales History, 2 = Payouts & Profile

    var upiInput by remember(user.upiId) { mutableStateOf(user.upiId) }
    var upiHolderInput by remember(user.upiAccountName) {
        mutableStateOf(user.upiAccountName.ifEmpty { user.fullName })
    }
    var withdrawAmountInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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

        item {
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
                        onShowMessage("Could not export statement: ${it.localizedMessage}")
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("wallet_download_statement_btn")
            ) {
                Icon(Icons.Filled.Download, contentDescription = "Download statement", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Download Earnings & UPI Statement (CSV)")
            }
        }

        item {
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = Color.White,
                contentColor = BrandPurplePrimary,
                modifier = Modifier.clip(RoundedCornerShape(14.dp))
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = { Text("UPI Withdraw", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.testTag("wallet_tab_withdraw")
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = { Text("Sales (${userCommissions.size})", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.testTag("wallet_tab_sales")
                )
                Tab(
                    selected = selectedSubTab == 2,
                    onClick = { selectedSubTab = 2 },
                    text = { Text("History & Profile", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.testTag("wallet_tab_profile")
                )
            }
        }

        if (selectedSubTab == 0) {
            // UPI ID Setup Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, AppOutlineLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = BrandPurplePrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("UPI Payout Account", style = MaterialTheme.typography.titleMedium, color = TextPrimaryLight)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Instant bank transfers via GPay, PhonePe, Paytm, or BHIM UPI.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryLight
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = upiInput,
                            onValueChange = { upiInput = it },
                            label = { Text("UPI ID (e.g. aarav@okaxis)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("upi_id_input")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = upiHolderInput,
                            onValueChange = { upiHolderInput = it },
                            label = { Text("Account Holder Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("upi_holder_input")
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { onUpdateUpi(upiInput, upiHolderInput) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_upi_button")
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Verified UPI ID")
                        }
                    }
                }
            }

            // Withdraw Request Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, AppOutlineLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Payments, contentDescription = null, tint = BrandBlueSecondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Withdraw Money", style = MaterialTheme.typography.titleMedium, color = TextPrimaryLight)
                            }
                            StatusPillBadge(
                                text = "Min ${Formatters.formatInr(minWithdrawalInr)}",
                                tone = BadgeTone.BLUE
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = withdrawAmountInput,
                            onValueChange = { withdrawAmountInput = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Withdrawal Amount (₹)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("withdraw_amount_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(100, 250, 500).forEach { preset ->
                                FilterChip(
                                    selected = withdrawAmountInput == preset.toString(),
                                    onClick = { withdrawAmountInput = preset.toString() },
                                    label = { Text("₹$preset") }
                                )
                            }
                            FilterChip(
                                selected = false,
                                onClick = {
                                    withdrawAmountInput = wallet.availableBalanceInr.toInt().toString()
                                },
                                label = { Text("Max") }
                            )
                        }

                        if (wallet.heldWithdrawalInr > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Pending payout in review: ${Formatters.formatInr(wallet.heldWithdrawalInr)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryLight
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val amt = withdrawAmountInput.toDoubleOrNull() ?: 0.0
                                onRequestWithdrawal(amt)
                                withdrawAmountInput = ""
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPurplePrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("request_withdrawal_button")
                        ) {
                            Text("Request UPI Withdrawal")
                        }
                    }
                }
            }
        } else if (selectedSubTab == 1) {
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
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No referral sales yet", style = MaterialTheme.typography.titleMedium, color = TextPrimaryLight)
                            Text("Share product QRs to track every customer order & commission here.", style = MaterialTheme.typography.bodySmall, color = TextSecondaryLight)
                        }
                    }
                }
            } else {
                items(userCommissions, key = { it.id }) { comm ->
                    CommissionActivityCard(comm = comm)
                }
            }
        } else {
            // Payment & Withdrawal History
            item {
                Text(
                    text = "UPI Withdrawal History",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimaryLight
                )
            }

            if (userWithdrawals.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, AppOutlineLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No withdrawal requests submitted yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryLight,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(userWithdrawals, key = { it.id }) { wd ->
                    val wdTone = when (wd.status) {
                        WithdrawalStatus.APPROVED -> BadgeTone.SUCCESS
                        WithdrawalStatus.PENDING -> BadgeTone.WARNING
                        WithdrawalStatus.REJECTED -> BadgeTone.ERROR
                    }
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, AppOutlineLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                StatusPillBadge(text = wd.status.label, tone = wdTone)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "To: ${wd.upiId}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = TextPrimaryLight
                                )
                                Text(
                                    text = if (wd.utrReference.isNotBlank()) {
                                        "UTR: ${wd.utrReference} • ${Formatters.formatRelativeTime(wd.requestedAt)}"
                                    } else {
                                        "${wd.adminNote.ifEmpty { "Requested" }} • ${Formatters.formatRelativeTime(wd.requestedAt)}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMutedLight
                                )
                            }
                            Text(
                                text = Formatters.formatInr(wd.amountInr),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFeatureSettings = "tnum",
                                    fontWeight = FontWeight.Bold
                                ),
                                color = TextPrimaryLight
                            )
                        }
                    }
                }
            }

            // Personal Profile & Role Switcher Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, AppOutlineLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(BrandPurpleContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = BrandPurplePrimary)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.fullName, style = MaterialTheme.typography.titleMedium, color = TextPrimaryLight)
                                Text("${user.email} • ${user.phone}", style = MaterialTheme.typography.bodySmall, color = TextSecondaryLight)
                            }
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(user.referralCode))
                                    onShowMessage("Copied ${user.referralCode}")
                                }
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy referral ID", tint = BrandPurplePrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Quick Switch Account / Workspace",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimaryLight
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onQuickSwitchUser("usr_admin_01", "ADMIN") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("switch_to_admin_btn")
                            ) {
                                Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Admin Panel", maxLines = 1)
                            }

                            val otherAffiliate = allUsers.firstOrNull {
                                it.id != user.id && it.role == UserRole.USER && it.isActive
                            }
                            if (otherAffiliate != null) {
                                OutlinedButton(
                                    onClick = { onQuickSwitchUser(otherAffiliate.id, "USER") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                ) {
                                    Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(otherAffiliate.fullName.substringBefore(" "), maxLines = 1)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onLogout,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorCrimson),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("logout_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign Out")
                        }
                    }
                }
            }
        }
    }
}
