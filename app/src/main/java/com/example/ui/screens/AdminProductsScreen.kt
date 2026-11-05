package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CommissionType
import com.example.data.local.ProductEntity
import com.example.ui.components.BadgeTone
import com.example.ui.components.ProductImageView
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
import com.example.util.MediaStorageHelper
import com.example.util.QrCodeUtils

@Composable
fun AdminProductsScreen(
    products: List<ProductEntity>,
    onOpenMasterQr: (ProductEntity) -> Unit,
    onSaveProduct: (
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
    ) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddEditSheet by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Product & QR Catalog",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimaryLight
                    )
                    Text(
                        text = "${products.size} active products • Auto-generated unique QR codes",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryLight
                    )
                }
                Button(
                    onClick = {
                        editingProduct = null
                        showAddEditSheet = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurplePrimary),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("admin_add_product_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Product")
                }
            }
        }

        items(products, key = { it.id }) { product ->
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, AppOutlineLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_product_${product.skuCode}")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(14.dp))
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
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimaryLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${Formatters.formatInr(product.priceInr)} • Stock: ${product.stock} • ${product.commissionBadgeText()} (${product.returnWindowDays}d return)",
                                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                color = SuccessEmerald
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onOpenMasterQr(product) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlueSecondary),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("admin_qr_btn_${product.skuCode}")
                        ) {
                            Icon(Icons.Filled.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Master QR", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        OutlinedButton(
                            onClick = {
                                val payload = QrCodeUtils.buildProductQrPayload(product.skuCode, null)
                                val card = QrCodeUtils.createBrandedQrShareBitmap(
                                    productName = product.name,
                                    priceText = Formatters.formatInr(product.priceInr),
                                    commissionLabel = product.commissionBadgeText(),
                                    skuCode = product.skuCode,
                                    referralCode = null,
                                    payload = payload
                                )
                                QrCodeUtils.saveQrToGallery(context, card, "CW_MASTER_${product.skuCode}")
                                    .onSuccess { fileName ->
                                        onShowMessage("Downloaded Master QR to Pictures/CommissionWala/$fileName")
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
                                .testTag("admin_download_qr_btn_${product.skuCode}")
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = "Download Master QR", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        OutlinedButton(
                            onClick = {
                                editingProduct = product
                                showAddEditSheet = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("admin_edit_btn_${product.skuCode}")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit product", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit")
                        }

                        IconButton(
                            onClick = { onDeleteProduct(product.id) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("admin_delete_btn_${product.skuCode}")
                        ) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete product", tint = ErrorCrimson)
                        }
                    }
                }
            }
        }
    }

    if (showAddEditSheet) {
        AddEditProductSheet(
            existing = editingProduct,
            onDismiss = { showAddEditSheet = false },
            onSave = { name, desc, cat, price, stock, commType, commVal, returnDays, imgUri ->
                onSaveProduct(
                    editingProduct?.id,
                    name,
                    desc,
                    cat,
                    price,
                    stock,
                    commType,
                    commVal,
                    returnDays,
                    imgUri
                ) {
                    showAddEditSheet = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditProductSheet(
    existing: ProductEntity?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        description: String,
        category: String,
        priceInr: Double,
        stock: Int,
        commissionType: CommissionType,
        commissionValue: Double,
        returnWindowDays: Int,
        imageUri: String
    ) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    var category by remember { mutableStateOf(existing?.category ?: "Electronics") }
    var priceText by remember { mutableStateOf(existing?.priceInr?.toInt()?.toString().orEmpty()) }
    var stockText by remember { mutableStateOf(existing?.stock?.toString() ?: "25") }
    var commType by remember { mutableStateOf(existing?.commissionType ?: CommissionType.PERCENTAGE) }
    var commValText by remember { mutableStateOf(existing?.commissionValue?.toInt()?.toString() ?: "15") }
    var returnDaysText by remember { mutableStateOf(existing?.returnWindowDays?.toString() ?: "7") }
    var imageUri by remember { mutableStateOf(existing?.imageUri ?: "res://img_prod_earbuds") }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            MediaStorageHelper.persistPickedImage(context, uri, "prod")?.let { savedPath ->
                imageUri = savedPath
            }
        }
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
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existing == null) "Add New Product" else "Edit Product",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimaryLight
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProductImageView(
                    imageUri = imageUri,
                    contentDescription = "Product preview",
                    modifier = Modifier
                        .size(74.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("admin_pick_product_image")
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload Product Photo")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Preset Studio Photos
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val presets = listOf(
                    "Earbuds" to "res://img_prod_earbuds",
                    "Smartwatch" to "res://img_prod_smartwatch",
                    "Backpack" to "res://img_prod_backpack"
                )
                items(presets) { (label, resKey) ->
                    FilterChip(
                        selected = imageUri == resKey,
                        onClick = { imageUri = resKey },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Product Name") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_product_name_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Product Description") },
                minLines = 2,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_product_desc_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text("Category", style = MaterialTheme.typography.labelLarge, color = TextPrimaryLight)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Audio", "Wearables", "Lifestyle", "Electronics", "Accessories")) { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Price (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_product_price_input")
                )
                OutlinedTextField(
                    value = stockText,
                    onValueChange = { stockText = it.filter { c -> c.isDigit() } },
                    label = { Text("Stock Qty") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_product_stock_input")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Affiliate Commission Structure", style = MaterialTheme.typography.labelLarge, color = TextPrimaryLight)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = commType == CommissionType.PERCENTAGE,
                    onClick = { commType = CommissionType.PERCENTAGE },
                    label = { Text("Percentage (%)") }
                )
                FilterChip(
                    selected = commType == CommissionType.FIXED,
                    onClick = { commType = CommissionType.FIXED },
                    label = { Text("Fixed Amount (₹)") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = commValText,
                    onValueChange = { commValText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = {
                        Text(if (commType == CommissionType.PERCENTAGE) "Commission (%)" else "Fixed Reward (₹)")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_product_commission_input")
                )
                OutlinedTextField(
                    value = returnDaysText,
                    onValueChange = { returnDaysText = it.filter { c -> c.isDigit() } },
                    label = { Text("Return Lock (Days)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_product_returndays_input")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onSave(
                        name,
                        description,
                        category,
                        priceText.toDoubleOrNull() ?: 0.0,
                        stockText.toIntOrNull() ?: 0,
                        commType,
                        commValText.toDoubleOrNull() ?: 0.0,
                        returnDaysText.toIntOrNull() ?: 7,
                        imageUri
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPurplePrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("admin_save_product_submit")
            ) {
                Text(if (existing == null) "Save Product & Generate QR" else "Update Product")
            }
        }
    }
}
