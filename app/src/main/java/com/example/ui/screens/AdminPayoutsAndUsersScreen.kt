package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CommissionEntity
import com.example.data.local.CommissionStatus
import com.example.data.local.UserEntity
import com.example.data.local.UserRole
import com.example.data.local.WithdrawalEntity
import com.example.data.local.WithdrawalStatus
import com.example.ui.components.BadgeTone
import com.example.ui.components.StatusPillBadge
import com.example.ui.theme.AppOutlineLight
import com.example.ui.theme.BrandBlueSecondary
import com.example.ui.theme.BrandPurplePrimary
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextMutedLight
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight
import com.example.util.Formatters

@Composable
fun AdminPayoutsAndUsersScreen(
    commissions: List<CommissionEntity>,
    withdrawals: List<WithdrawalEntity>,
    users: List<UserEntity>,
    onApproveCommission: (String) -> Unit,
    onRejectCommission: (String, String) -> Unit,
    onProcessWithdrawal: (withdrawalId: String, approve: Boolean, utr: String, note: String) -> Unit,
    onToggleUserActive: (userId: String, active: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Commissions, 1 = UPI Withdrawals, 2 = Users

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Commissions, Payouts & Users",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimaryLight
                )
                Text(
                    text = "Approve referral commissions, settle UPI withdrawals, and manage registered affiliates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight
                )
            }
        }

        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = BrandPurplePrimary,
                modifier = Modifier.clip(RoundedCornerShape(14.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Commissions (${commissions.size})", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.testTag("admin_payouts_tab_commissions")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Withdrawals (${withdrawals.size})", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.testTag("admin_payouts_tab_withdrawals")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Users (${users.size})", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.testTag("admin_payouts_tab_users")
                )
            }
        }

        if (selectedTab == 0) {
            items(commissions, key = { it.id }) { comm ->
                val affiliate = users.firstOrNull { it.id == comm.affiliateUserId }
                val tone = when (comm.status) {
                    CommissionStatus.APPROVED_PAID -> BadgeTone.SUCCESS
                    CommissionStatus.PAYABLE -> BadgeTone.BLUE
                    CommissionStatus.IN_RETURN_WINDOW -> BadgeTone.WARNING
                    CommissionStatus.PENDING_DELIVERY -> BadgeTone.PURPLE
                    CommissionStatus.REJECTED -> BadgeTone.ERROR
                }
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, AppOutlineLight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_commission_${comm.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = comm.orderNumber,
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                                    color = BrandBlueSecondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                StatusPillBadge(text = comm.status.label, tone = tone)
                            }
                            Text(
                                text = Formatters.formatInr(comm.commissionAmountInr),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFeatureSettings = "tnum",
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = SuccessEmerald
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(comm.productName, style = MaterialTheme.typography.titleSmall, color = TextPrimaryLight)
                        Text(
                            text = "Affiliate: ${affiliate?.fullName ?: "Partner"} (${comm.referralCode}) • Sale: ${Formatters.formatInr(comm.saleAmountInr)} (${comm.commissionRateLabel})",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryLight
                        )
                        if (comm.adminNote.isNotBlank()) {
                            Text(
                                text = comm.adminNote,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMutedLight
                            )
                        }

                        if (comm.status == CommissionStatus.PAYABLE || comm.status == CommissionStatus.IN_RETURN_WINDOW) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onApproveCommission(comm.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("approve_commission_${comm.id}")
                                ) {
                                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (comm.status == CommissionStatus.IN_RETURN_WINDOW) {
                                            "Release & Credit Now"
                                        } else {
                                            "Approve & Credit"
                                        }
                                    )
                                }
                                OutlinedButton(
                                    onClick = { onRejectCommission(comm.id, "Rejected by Admin audit") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Text("Reject", color = ErrorCrimson)
                                }
                            }
                        }
                    }
                }
            }
        } else if (selectedTab == 1) {
            if (withdrawals.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, AppOutlineLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No UPI withdrawal requests yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryLight,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
            } else {
                items(withdrawals, key = { it.id }) { wd ->
                    val tone = when (wd.status) {
                        WithdrawalStatus.APPROVED -> BadgeTone.SUCCESS
                        WithdrawalStatus.PENDING -> BadgeTone.WARNING
                        WithdrawalStatus.REJECTED -> BadgeTone.ERROR
                    }
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, AppOutlineLight),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_withdrawal_${wd.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusPillBadge(text = wd.status.label, tone = tone)
                                Text(
                                    text = Formatters.formatInr(wd.amountInr),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFeatureSettings = "tnum",
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = TextPrimaryLight
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(wd.userName, style = MaterialTheme.typography.titleMedium, color = TextPrimaryLight)
                            Text(
                                text = "UPI ID: ${wd.upiId} (${wd.upiAccountName})",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = BrandBlueSecondary
                            )
                            Text(
                                text = if (wd.utrReference.isNotBlank()) {
                                    "UTR: ${wd.utrReference} • ${Formatters.formatRelativeTime(wd.requestedAt)}"
                                } else {
                                    "Requested ${Formatters.formatRelativeTime(wd.requestedAt)}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMutedLight
                            )

                            if (wd.status == WithdrawalStatus.PENDING) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            onProcessWithdrawal(wd.id, true, "", "Paid via instant UPI")
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurplePrimary),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("approve_withdrawal_${wd.id}")
                                    ) {
                                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Approve & Mark Paid")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onProcessWithdrawal(wd.id, false, "", "Rejected by Admin")
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(44.dp)
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = null, tint = ErrorCrimson, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Reject", color = ErrorCrimson)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            items(users, key = { it.id }) { usr ->
                Card(
                    shape = RoundedCornerShape(18.dp),
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
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusPillBadge(
                                    text = if (usr.role == UserRole.ADMIN) "Owner Admin" else "Affiliate",
                                    tone = if (usr.role == UserRole.ADMIN) BadgeTone.BLUE else BadgeTone.PURPLE
                                )
                                StatusPillBadge(
                                    text = if (usr.isActive) "Active" else "Suspended",
                                    tone = if (usr.isActive) BadgeTone.SUCCESS else BadgeTone.ERROR
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(usr.fullName, style = MaterialTheme.typography.titleMedium, color = TextPrimaryLight)
                            Text(
                                text = "Ref: ${usr.referralCode} • ${usr.email}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = TextSecondaryLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (usr.upiId.isNotBlank()) {
                                Text(
                                    text = "UPI: ${usr.upiId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandBlueSecondary
                                )
                            }
                        }

                        if (usr.role != UserRole.ADMIN) {
                            Switch(
                                checked = usr.isActive,
                                onCheckedChange = { onToggleUserActive(usr.id, it) }
                            )
                        }
                    }
                }
            }
        }
    }
}
