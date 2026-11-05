package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ProductEntity
import com.example.ui.components.BadgeTone
import com.example.ui.components.ProductImageView
import com.example.ui.components.StatusPillBadge
import com.example.ui.theme.AppOutlineLight
import com.example.ui.theme.BrandBlueContainer
import com.example.ui.theme.BrandBlueSecondary
import com.example.ui.theme.BrandOnBlueContainer
import com.example.ui.theme.BrandPurplePrimary
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextMutedLight
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight
import com.example.util.Formatters
import com.example.util.QrCodeUtils

@Composable
fun ProductsCatalogScreen(
    products: List<ProductEntity>,
    userReferralCode: String,
    onOpenQrStudio: (ProductEntity) -> Unit,
    onOrderProduct: (ProductEntity) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = remember(products) {
        listOf("All") + products.map { it.category }.distinct()
    }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { product ->
            val matchesCat = selectedCategory == "All" || product.category.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                product.name.contains(searchQuery, ignoreCase = true) ||
                product.skuCode.contains(searchQuery, ignoreCase = true) ||
                product.category.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Affiliate Product Catalog",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimaryLight
                )
                Text(
                    text = "Every product includes your embedded QR & referral link ($userReferralCode)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search earbuds, watch, SKU...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_search_input")
                )
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) }
                    )
                }
            }
        }

        if (filteredProducts.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, AppOutlineLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.ShoppingBag, contentDescription = null, tint = TextMutedLight, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No matching products found", style = MaterialTheme.typography.titleMedium, color = TextPrimaryLight)
                        Text("Try clearing your search filter.", style = MaterialTheme.typography.bodySmall, color = TextSecondaryLight)
                    }
                }
            }
        } else {
            items(filteredProducts, key = { it.id }) { product ->
                val earnPerSale = product.calculateCommission(1)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, AppOutlineLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_card_${product.skuCode}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            ) {
                                ProductImageView(
                                    imageUri = product.imageUri,
                                    contentDescription = product.name,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StatusPillBadge(text = product.category, tone = BadgeTone.BLUE)
                                    Text(
                                        text = product.skuCode,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                        color = TextMutedLight
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimaryLight,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = product.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondaryLight,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Price + Commission + Stock Strip
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8F9FE),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Customer Price", style = MaterialTheme.typography.labelSmall, color = TextMutedLight)
                                    Text(
                                        text = Formatters.formatInr(product.priceInr),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFeatureSettings = "tnum",
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = TextPrimaryLight
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Your Commission", style = MaterialTheme.typography.labelSmall, color = TextMutedLight)
                                    Text(
                                        text = "+${Formatters.formatInr(earnPerSale)} (${product.commissionBadgeText()})",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontFeatureSettings = "tnum",
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = SuccessEmerald
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Stock", style = MaterialTheme.typography.labelSmall, color = TextMutedLight)
                                    Text(
                                        text = if (product.stock > 0) "${product.stock} left" else "Sold out",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (product.stock > 0) BrandBlueSecondary else Color.Red
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onOpenQrStudio(product) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPurplePrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("product_qr_btn_${product.skuCode}")
                            ) {
                                Icon(Icons.Filled.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("QR & Share", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            OutlinedButton(
                                onClick = {
                                    val payload = QrCodeUtils.buildProductQrPayload(product.skuCode, userReferralCode)
                                    val card = QrCodeUtils.createBrandedQrShareBitmap(
                                        productName = product.name,
                                        priceText = Formatters.formatInr(product.priceInr),
                                        commissionLabel = product.commissionBadgeText(),
                                        skuCode = product.skuCode,
                                        referralCode = userReferralCode,
                                        payload = payload
                                    )
                                    QrCodeUtils.saveQrToGallery(context, card, "CW_${product.skuCode}")
                                        .onSuccess { fileName ->
                                            onShowMessage("Downloaded QR card to Pictures/CommissionWala/$fileName")
                                        }
                                        .onFailure {
                                            onShowMessage("Could not save QR image: ${it.localizedMessage}")
                                        }
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("product_download_qr_btn_${product.skuCode}")
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = "Download QR", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download QR", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val payload = QrCodeUtils.buildProductQrPayload(product.skuCode, userReferralCode)
                                    val card = QrCodeUtils.createBrandedQrShareBitmap(
                                        productName = product.name,
                                        priceText = Formatters.formatInr(product.priceInr),
                                        commissionLabel = product.commissionBadgeText(),
                                        skuCode = product.skuCode,
                                        referralCode = userReferralCode,
                                        payload = payload
                                    )
                                    QrCodeUtils.shareQrAndLink(
                                        context,
                                        card,
                                        "Check out ${product.name} at ${Formatters.formatInr(product.priceInr)}! Order via my verified CommissionWala link: $payload"
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("product_share_link_btn_${product.skuCode}")
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share Link", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            FilledTonalButton(
                                onClick = { onOrderProduct(product) },
                                enabled = product.stock > 0,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = BrandBlueContainer,
                                    contentColor = BrandOnBlueContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("product_order_btn_${product.skuCode}")
                            ) {
                                Text("Order Now", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}
