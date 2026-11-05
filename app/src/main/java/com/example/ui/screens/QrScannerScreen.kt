package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.ProductEntity
import com.example.data.local.UserEntity
import com.example.data.local.UserRole
import com.example.ui.components.BadgeTone
import com.example.ui.components.StatusPillBadge
import com.example.ui.theme.AppOutlineLight
import com.example.ui.theme.BrandBlueContainer
import com.example.ui.theme.BrandBlueSecondary
import com.example.ui.theme.BrandPurpleContainer
import com.example.ui.theme.BrandPurplePrimary
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight
import com.example.util.Formatters
import com.example.util.QrCodeUtils
import java.util.concurrent.Executors

@Composable
fun QrScannerScreen(
    products: List<ProductEntity>,
    allUsers: List<UserEntity>,
    currentUser: UserEntity,
    onQrDecoded: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            onShowMessage("Camera permission denied. You can still scan QR images from Gallery or paste a QR link below!")
        }
    }

    val qrPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val decoded = QrCodeUtils.decodeQrFromUri(context, uri)
            if (decoded != null) {
                onQrDecoded(decoded)
            } else {
                onShowMessage("No readable QR code found in the selected image.")
            }
        }
    }

    var manualQrInput by remember { mutableStateOf("") }
    var lastScanTimestamp by remember { mutableLongStateOf(0L) }

    val infiniteTransition = rememberInfiniteTransition(label = "qr_laser")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    // Pick a fellow affiliate's referral code for quick-test QR simulation so user can test earning without self-referral block!
    val samplePartner = remember(allUsers, currentUser) {
        allUsers.firstOrNull { it.id != currentUser.id && it.role == UserRole.USER && it.isActive }
            ?: currentUser
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Scan Product QR Code",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimaryLight
                )
                Text(
                    text = "Scan a CommissionWala QR via camera, gallery image, or link to verify referral attribution & order.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight
                )
            }
        }

        // CameraX Live Viewfinder or Permission Prompt
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                if (hasCameraPermission) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val previewView = remember { PreviewView(context) }
                        val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

                        DisposableEffect(lifecycleOwner) {
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                            cameraProviderFuture.addListener({
                                try {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }
                                    val analyzer = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                        .also { imageAnalysis ->
                                            imageAnalysis.setAnalyzer(analysisExecutor) { proxy ->
                                                val text = QrCodeUtils.decodeQrFromImageProxy(proxy)
                                                proxy.close()
                                                if (!text.isNullOrBlank()) {
                                                    val now = System.currentTimeMillis()
                                                    if (now - lastScanTimestamp > 2000L) {
                                                        lastScanTimestamp = now
                                                        previewView.post {
                                                            onQrDecoded(text)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        analyzer
                                    )
                                } catch (_: Exception) {
                                    // Handled gracefully on virtual emulators without back camera
                                }
                            }, ContextCompat.getMainExecutor(context))

                            onDispose {
                                try {
                                    cameraProviderFuture.get().unbindAll()
                                } catch (_: Exception) {
                                }
                                analysisExecutor.shutdown()
                            }
                        }

                        AndroidView(
                            factory = { previewView },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Animated Purple-Blue Scanner Reticle Overlay
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val boxSize = minOf(size.width, size.height) * 0.65f
                            val left = (size.width - boxSize) / 2f
                            val top = (size.height - boxSize) / 2f
                            val cornerLen = 28.dp.toPx()
                            val strokeW = 4.dp.toPx()

                            // Top-Left
                            drawLine(Color(0xFFA78BFA), Offset(left, top), Offset(left + cornerLen, top), strokeW)
                            drawLine(Color(0xFFA78BFA), Offset(left, top), Offset(left, top + cornerLen), strokeW)
                            // Top-Right
                            drawLine(Color(0xFF60A5FA), Offset(left + boxSize - cornerLen, top), Offset(left + boxSize, top), strokeW)
                            drawLine(Color(0xFF60A5FA), Offset(left + boxSize, top), Offset(left + boxSize, top + cornerLen), strokeW)
                            // Bottom-Left
                            drawLine(Color(0xFFA78BFA), Offset(left, top + boxSize - cornerLen), Offset(left, top + boxSize), strokeW)
                            drawLine(Color(0xFFA78BFA), Offset(left, top + boxSize), Offset(left + cornerLen, top + boxSize), strokeW)
                            // Bottom-Right
                            drawLine(Color(0xFF60A5FA), Offset(left + boxSize - cornerLen, top + boxSize), Offset(left + boxSize, top + boxSize), strokeW)
                            drawLine(Color(0xFF60A5FA), Offset(left + boxSize, top + boxSize - cornerLen), Offset(left + boxSize, top + boxSize), strokeW)

                            // Laser Line
                            val laserY = top + boxSize * laserProgress
                            drawLine(
                                color = Color(0xFF38BDF8),
                                start = Offset(left + 10.dp.toPx(), laserY),
                                end = Offset(left + boxSize - 10.dp.toPx(), laserY),
                                strokeWidth = 2.5.dp.toPx()
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = "Align product QR code inside frame",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(BrandPurplePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = "Camera scanner",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Camera QR Scanner Ready",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Grant camera access to scan physical QR codes, or pick a saved QR image below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlueSecondary),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("enable_camera_button")
                        ) {
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enable Camera Scanner")
                        }
                    }
                }
            }
        }

        // Scan QR Image from Gallery (Zero-Permission Photo Picker) + Paste Link
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, AppOutlineLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedButton(
                        onClick = {
                            qrPhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("scan_qr_from_gallery_button")
                    ) {
                        Icon(Icons.Filled.ImageSearch, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Decode QR Image from Gallery")
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Or Enter / Paste QR Link or SKU",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimaryLight
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualQrInput,
                            onValueChange = { manualQrInput = it },
                            placeholder = { Text("https://commissionwala.app/p/PRD-ANC-101?ref=...") },
                            leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_qr_input")
                        )
                        Button(
                            onClick = { onQrDecoded(manualQrInput) },
                            enabled = manualQrInput.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPurplePrimary),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("manual_qr_submit_button")
                        ) {
                            Text("Open")
                        }
                    }
                }
            }
        }

        // Quick-Test Scannable QR Payloads (helpful on Streaming Emulator!)
        item {
            Column {
                Text(
                    text = "Quick-Scan Partner QR Payloads",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimaryLight
                )
                Text(
                    text = "Tap any QR payload below to simulate scanning ${samplePartner.fullName}'s shared product QR:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight
                )
            }
        }

        items(products.take(4), key = { it.id }) { product ->
            val qrUrl = QrCodeUtils.buildProductQrPayload(product.skuCode, samplePartner.referralCode)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, AppOutlineLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onQrDecoded(qrUrl) }
                    .testTag("quick_scan_${product.skuCode}")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BrandPurpleContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = BrandPurplePrimary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimaryLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${product.skuCode} • ref=${samplePartner.referralCode}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = BrandBlueSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    StatusPillBadge(
                        text = Formatters.formatInr(product.priceInr),
                        tone = BadgeTone.PURPLE
                    )
                }
            }
        }
    }
}
