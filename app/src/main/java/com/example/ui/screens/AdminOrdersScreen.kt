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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CommissionEntity
import com.example.data.local.OrderEntity
import com.example.data.local.OrderStatus
import com.example.data.local.UserEntity
import com.example.ui.components.BadgeTone
import com.example.ui.components.StatusPillBadge
import com.example.ui.theme.AppOutlineLight
import com.example.ui.theme.BrandBlueSecondary
import com.example.ui.theme.BrandOnPurpleContainer
import com.example.ui.theme.BrandPurpleContainer
import com.example.ui.theme.BrandPurplePrimary
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextMutedLight
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight
import com.example.util.Formatters

@Composable
fun AdminOrdersScreen(
    orders: List<OrderEntity>,
    commissions: List<CommissionEntity>,
    users: List<UserEntity>,
    onUpdateOrderStatus: (orderId: String, status: OrderStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<OrderStatus?>(null) }

    val filteredOrders = remember(orders, selectedFilter) {
        if (selectedFilter == null) orders else orders.filter { it.status == selectedFilter }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Order & Referral Fulfillment",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimaryLight
                )
                Text(
                    text = "Track customer orders, referring affiliates, and trigger delivery/return windows.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedFilter == null,
                            onClick = { selectedFilter = null },
                            label = { Text("All (${orders.size})") }
                        )
                    }
                    items(OrderStatus.entries) { status ->
                        val count = orders.count { it.status == status }
                        FilterChip(
                            selected = selectedFilter == status,
                            onClick = { selectedFilter = status },
                            label = { Text("${status.label} ($count)") }
                        )
                    }
                }
            }
        }

        if (filteredOrders.isEmpty()) {
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
                        Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = TextMutedLight, modifier = Modifier.size(38.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No orders in this status", style = MaterialTheme.typography.titleMedium, color = TextPrimaryLight)
                    }
                }
            }
        } else {
            items(filteredOrders, key = { it.id }) { order ->
                val linkedComm = commissions.firstOrNull { it.orderId == order.id }
                val affiliate = order.referrerUserId?.let { uid -> users.firstOrNull { it.id == uid } }
                val statusTone = when (order.status) {
                    OrderStatus.PENDING -> BadgeTone.WARNING
                    OrderStatus.CONFIRMED -> BadgeTone.PURPLE
                    OrderStatus.SHIPPED -> BadgeTone.BLUE
                    OrderStatus.DELIVERED -> BadgeTone.SUCCESS
                    OrderStatus.CANCELLED -> BadgeTone.ERROR
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, AppOutlineLight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_order_${order.orderNumber}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = order.orderNumber,
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = BrandBlueSecondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                StatusPillBadge(text = order.status.label, tone = statusTone)
                            }
                            Text(
                                text = Formatters.formatInr(order.totalAmountInr),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFeatureSettings = "tnum",
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = TextPrimaryLight
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${order.quantity}× ${order.productName}",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryLight
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Customer: ${order.customerName} (${order.customerPhone}) • ${order.paymentMethod}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryLight
                        )
                        Text(
                            text = "Address: ${order.deliveryAddress}, ${order.city} - ${order.pincode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedLight
                        )

                        if (affiliate != null && linkedComm != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = BrandPurpleContainer.copy(alpha = 0.65f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = Icons.Filled.PersonPin,
                                            contentDescription = null,
                                            tint = BrandPurplePrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = "Affiliate: ${affiliate.fullName} (${affiliate.referralCode})",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = BrandOnPurpleContainer
                                            )
                                            Text(
                                                text = "Commission: ${Formatters.formatInr(linkedComm.commissionAmountInr)} • ${linkedComm.status.label}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = BrandPurplePrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (order.status != OrderStatus.DELIVERED && order.status != OrderStatus.CANCELLED) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val nextStatus = when (order.status) {
                                    OrderStatus.PENDING -> OrderStatus.CONFIRMED
                                    OrderStatus.CONFIRMED -> OrderStatus.SHIPPED
                                    OrderStatus.SHIPPED -> OrderStatus.DELIVERED
                                    else -> OrderStatus.DELIVERED
                                }
                                Button(
                                    onClick = { onUpdateOrderStatus(order.id, nextStatus) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurplePrimary),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("order_advance_${order.orderNumber}")
                                ) {
                                    Text("Mark ${nextStatus.label}")
                                }

                                if (order.status != OrderStatus.SHIPPED) {
                                    Button(
                                        onClick = { onUpdateOrderStatus(order.id, OrderStatus.DELIVERED) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald),
                                        modifier = Modifier
                                            .height(44.dp)
                                            .testTag("order_deliver_${order.orderNumber}")
                                    ) {
                                        Text("Deliver Now")
                                    }
                                }

                                OutlinedButton(
                                    onClick = { onUpdateOrderStatus(order.id, OrderStatus.CANCELLED) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Text("Cancel", color = ErrorCrimson)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
