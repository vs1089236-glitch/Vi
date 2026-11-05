package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.repository.OrderPlacementOutcome
import com.example.ui.theme.AppOutlineLight
import com.example.ui.theme.BrandBlueSecondary
import com.example.ui.theme.BrandOnPurpleContainer
import com.example.ui.theme.BrandPurpleContainer
import com.example.ui.theme.BrandPurplePrimary
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.OnSuccessContainer
import com.example.ui.theme.OnWarningContainer
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight
import com.example.ui.theme.WarningContainer
import com.example.ui.viewmodel.CheckoutDraft
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutSheet(
    draft: CheckoutDraft,
    outcome: OrderPlacementOutcome?,
    currentUserId: String?,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onPlaceOrder: (
        productId: String,
        quantity: Int,
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        city: String,
        pincode: String,
        paymentMethod: String,
        referralCodeInput: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val product = draft.product

    var quantity by remember { mutableIntStateOf(1) }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var deliveryAddress by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("UPI") }
    var referralCode by remember { mutableStateOf(draft.referralCode.orEmpty()) }

    val totalAmount = product.priceInr * quantity
    val estimatedCommission = product.calculateCommission(quantity)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (outcome != null) "Order Confirmed!" else "Customer Checkout",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimaryLight
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close checkout")
                }
            }

            if (outcome != null) {
                // Order Confirmation Receipt View
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SuccessContainer.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, SuccessEmerald.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(SuccessEmerald),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Order placed",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = outcome.order.orderNumber,
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily),
                            color = OnSuccessContainer
                        )
                        Text(
                            text = "${outcome.order.quantity}× ${outcome.order.productName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimaryLight
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Total Paid: ${Formatters.formatInr(outcome.order.totalAmountInr)} (${outcome.order.paymentMethod})",
                            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                            color = OnSuccessContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (outcome.commissionTracked != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = BrandPurpleContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = null,
                                tint = BrandPurplePrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Affiliate Commission Tracked: ${Formatters.formatInr(outcome.commissionTracked.commissionAmountInr)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = BrandOnPurpleContainer
                                )
                                Text(
                                    text = "Attributed to ${outcome.commissionTracked.referralCode} • Unlocks after delivery & ${product.returnWindowDays}d return window.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BrandOnPurpleContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                } else if (outcome.antiFraudNote != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = WarningContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = OnWarningContainer
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = outcome.antiFraudNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = OnWarningContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurplePrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Done")
                }
            } else {
                // Product Summary Header
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8F9FE),
                    border = BorderStroke(1.dp, AppOutlineLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProductImageView(
                            imageUri = product.imageUri,
                            contentDescription = product.name,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimaryLight
                            )
                            Text(
                                text = "${Formatters.formatInr(product.priceInr)} / unit • Stock: ${product.stock}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryLight
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (quantity > 1) quantity-- },
                                enabled = quantity > 1
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity")
                            }
                            Text(
                                text = quantity.toString(),
                                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                                color = TextPrimaryLight
                            )
                            IconButton(
                                onClick = { if (quantity < product.stock) quantity++ },
                                enabled = quantity < product.stock
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase quantity")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Referral Attribution & Anti-Fraud Status Banner
                val isSelfRef = !draft.referrerName.isNullOrBlank() &&
                    draft.referralCode != null &&
                    currentUserId != null &&
                    draft.scannedFromQr.not() // If affiliate clicks Order on own catalog, hint them to enter a customer's phone

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (referralCode.isNotBlank()) BrandPurpleContainer.copy(alpha = 0.65f) else Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = if (referralCode.isNotBlank()) BrandPurplePrimary else TextSecondaryLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (referralCode.isNotBlank()) {
                                    "QR Referral Tracking: ${referralCode.uppercase()} • Commission: ${Formatters.formatInr(estimatedCommission)}"
                                } else {
                                    "Direct Order (No Referral Code)"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = BrandOnPurpleContainer
                            )
                            Text(
                                text = if (isSelfRef) {
                                    "Anti-fraud note: Orders matching the affiliate's own account/phone do not earn self-commission. Switch to another affiliate's QR to test commission payout!"
                                } else {
                                    "Protected by duplicate-claim signature & ${product.returnWindowDays}-day return verification."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryLight
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = referralCode,
                    onValueChange = { referralCode = it },
                    label = { Text("Affiliate Referral ID (from QR / Link)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("checkout_referral_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Customer Full Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("checkout_customer_name")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it.filter { ch -> ch.isDigit() }.take(10) },
                    label = { Text("Customer 10-Digit Mobile Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("checkout_customer_phone")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = deliveryAddress,
                    onValueChange = { deliveryAddress = it },
                    label = { Text("House / Flat / Street Address") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("checkout_delivery_address")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("checkout_city")
                    )
                    OutlinedTextField(
                        value = pincode,
                        onValueChange = { pincode = it.filter { ch -> ch.isDigit() }.take(6) },
                        label = { Text("PIN Code") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(0.8f)
                            .testTag("checkout_pincode")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimaryLight
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("UPI", "Cash on Delivery", "Card").forEach { method ->
                        FilterChip(
                            selected = paymentMethod == method,
                            onClick = { paymentMethod = method },
                            label = { Text(method) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onPlaceOrder(
                            product.id,
                            quantity,
                            customerName,
                            customerPhone,
                            deliveryAddress,
                            city,
                            pincode,
                            paymentMethod,
                            referralCode
                        )
                    },
                    enabled = !isBusy && product.stock >= quantity,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlueSecondary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("place_order_submit_button")
                ) {
                    Text(
                        text = if (product.stock <= 0) "Out of Stock" else "Confirm Order • ${Formatters.formatInr(totalAmount)}",
                        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum")
                    )
                }
            }
        }
    }
}
