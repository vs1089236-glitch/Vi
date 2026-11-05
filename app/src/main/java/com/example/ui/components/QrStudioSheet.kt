package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppOutlineLight
import com.example.ui.theme.BrandBlueContainer
import com.example.ui.theme.BrandBlueSecondary
import com.example.ui.theme.BrandOnBlueContainer
import com.example.ui.theme.BrandOnPurpleContainer
import com.example.ui.theme.BrandPurpleContainer
import com.example.ui.theme.BrandPurplePrimary
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight
import com.example.ui.viewmodel.QrModalContext
import com.example.util.Formatters
import com.example.util.QrCodeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrStudioSheet(
    qrContext: QrModalContext,
    onDismiss: () -> Unit,
    onShowMessage: (String) -> Unit,
    onSimulateCustomerCheckout: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val product = qrContext.product
    val refCode = qrContext.referralCode
    val payload = remember(product.skuCode, refCode) {
        QrCodeUtils.buildProductQrPayload(product.skuCode, refCode)
    }
    val qrBitmap = remember(payload) {
        QrCodeUtils.generateQrBitmap(payload, sizePx = 640)
    }
    val perSaleCommission = remember(product) {
        product.calculateCommission(1)
    }

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
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.QrCode2,
                        contentDescription = null,
                        tint = BrandPurplePrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (refCode != null) "Your Affiliate QR Studio" else "Master Product QR",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimaryLight
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close QR studio")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // High-Contrast QR Card Preview
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                border = BorderStroke(1.5.dp, AppOutlineLight),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StatusPillBadge(
                        text = if (refCode != null) {
                            "Tracked Referral: $refCode"
                        } else {
                            "Admin Catalog Master QR • ${product.skuCode}"
                        },
                        tone = if (refCode != null) BadgeTone.PURPLE else BadgeTone.BLUE
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR code for ${product.name}",
                            modifier = Modifier.size(204.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimaryLight,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${Formatters.formatInr(product.priceInr)}  •  Earn ${Formatters.formatInr(perSaleCommission)} (${product.commissionBadgeText()})",
                        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                        color = BrandBlueSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8F9FE),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = payload,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = JetBrainsMonoFamily
                                ),
                                color = TextSecondaryLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(payload))
                                    onShowMessage("Affiliate link copied to clipboard!")
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("copy_qr_link_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy affiliate link",
                                    tint = BrandPurplePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Actions: Download PNG + System Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val cardBmp = QrCodeUtils.createBrandedQrShareBitmap(
                            productName = product.name,
                            priceText = Formatters.formatInr(product.priceInr),
                            commissionLabel = product.commissionBadgeText(),
                            skuCode = product.skuCode,
                            referralCode = refCode,
                            payload = payload
                        )
                        QrCodeUtils.saveQrToGallery(context, cardBmp, "CW_${product.skuCode}")
                            .onSuccess { fileName ->
                                onShowMessage("Saved QR card to Pictures/CommissionWala/$fileName")
                            }
                            .onFailure {
                                onShowMessage("Could not save QR image: ${it.localizedMessage}")
                            }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("download_qr_button")
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download QR")
                }

                Button(
                    onClick = {
                        val cardBmp = QrCodeUtils.createBrandedQrShareBitmap(
                            productName = product.name,
                            priceText = Formatters.formatInr(product.priceInr),
                            commissionLabel = product.commissionBadgeText(),
                            skuCode = product.skuCode,
                            referralCode = refCode,
                            payload = payload
                        )
                        val msg = buildString {
                            append("Check out ${product.name} for just ${Formatters.formatInr(product.priceInr)} on CommissionWala!\n")
                            append("Scan the attached QR or order directly here: $payload")
                            if (refCode != null) append("\nReferral Code: $refCode")
                        }
                        QrCodeUtils.shareQrAndLink(context, cardBmp, msg)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurplePrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("share_qr_button")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share QR & Link")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Social Platform Direct Share Buttons: WhatsApp & Instagram
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        val cardBmp = QrCodeUtils.createBrandedQrShareBitmap(
                            productName = product.name,
                            priceText = Formatters.formatInr(product.priceInr),
                            commissionLabel = product.commissionBadgeText(),
                            skuCode = product.skuCode,
                            referralCode = refCode,
                            payload = payload
                        )
                        val msg = "🔥 ${product.name} at ${Formatters.formatInr(product.priceInr)}! Order via my verified link: $payload"
                        QrCodeUtils.shareQrAndLink(context, cardBmp, msg, targetPackage = "com.whatsapp")
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = SuccessContainer,
                        contentColor = SuccessEmerald
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("share_whatsapp_button")
                ) {
                    Text("WhatsApp", fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = {
                        val cardBmp = QrCodeUtils.createBrandedQrShareBitmap(
                            productName = product.name,
                            priceText = Formatters.formatInr(product.priceInr),
                            commissionLabel = product.commissionBadgeText(),
                            skuCode = product.skuCode,
                            referralCode = refCode,
                            payload = payload
                        )
                        val msg = "✨ ${product.name} — ${Formatters.formatInr(product.priceInr)}! Scan QR or tap: $payload"
                        QrCodeUtils.shareQrAndLink(context, cardBmp, msg, targetPackage = "com.instagram.android")
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = BrandPurpleContainer,
                        contentColor = BrandOnPurpleContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("share_instagram_button")
                ) {
                    Text("Instagram", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Customer Checkout via this QR
            FilledTonalButton(
                onClick = onSimulateCustomerCheckout,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = BrandBlueContainer,
                    contentColor = BrandOnBlueContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("qr_open_checkout_button")
            ) {
                Icon(Icons.Filled.ShoppingCartCheckout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Customer Checkout with This QR", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
