package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.AppOutlineLight
import com.example.ui.theme.BrandBlueContainer
import com.example.ui.theme.BrandBlueSecondary
import com.example.ui.theme.BrandOnBlueContainer
import com.example.ui.theme.BrandOnPurpleContainer
import com.example.ui.theme.BrandPurpleContainer
import com.example.ui.theme.BrandPurpleDeep
import com.example.ui.theme.BrandPurplePrimary
import com.example.ui.theme.BrandPurpleVibrant
import com.example.ui.theme.ErrorContainer
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.OnErrorContainer
import com.example.ui.theme.OnSuccessContainer
import com.example.ui.theme.OnWarningContainer
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextMutedLight
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningContainer
import com.example.util.Formatters
import java.io.File

@Composable
fun CommissionWalaTopBar(
    title: String,
    subtitle: String,
    isAdminMode: Boolean,
    unreadNotifications: Int,
    onNotificationsClick: () -> Unit,
    onSwitchWorkspaceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(BrandPurplePrimary, BrandBlueSecondary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CW",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimaryLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isAdminMode) BrandBlueContainer else BrandPurpleContainer,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onSwitchWorkspaceClick)
                        .testTag("workspace_switch_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isAdminMode) Icons.Filled.Person else Icons.Filled.AdminPanelSettings,
                            contentDescription = "Switch workspace role",
                            tint = if (isAdminMode) BrandOnBlueContainer else BrandOnPurpleContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isAdminMode) "User View" else "Admin",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isAdminMode) BrandOnBlueContainer else BrandOnPurpleContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("notifications_bell_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadNotifications > 0) {
                                Badge(
                                    containerColor = ErrorCrimson,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = if (unreadNotifications > 9) "9+" else unreadNotifications.toString(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notifications",
                            tint = TextPrimaryLight
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WalletHeroGradientCard(
    availableBalance: Double,
    commissionBalance: Double,
    taskRewardBalance: Double,
    pendingCommissions: Double,
    referralCode: String,
    onCopyReferralCode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        BrandPurpleDeep,
                        BrandPurpleVibrant,
                        BrandBlueSecondary
                    )
                )
            )
    ) {
        // Subtle geometric glow circles on Canvas
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = size.height * 0.65f,
                center = Offset(size.width * 0.92f, size.height * 0.15f)
            )
            drawCircle(
                color = Color(0xFF38BDF8).copy(alpha = 0.14f),
                radius = size.height * 0.45f,
                center = Offset(size.width * 0.12f, size.height * 0.95f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Available Wallet Balance",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Formatters.formatInr(availableBalance),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFeatureSettings = "tnum",
                            color = Color.White
                        ),
                        modifier = Modifier.testTag("wallet_available_balance")
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.18f),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onCopyReferralCode)
                        .testTag("copy_referral_code_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "REFERRAL ID",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = Color.White.copy(alpha = 0.78f)
                            )
                            Text(
                                text = referralCode,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    color = Color.White
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy referral ID",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WalletMiniStat(
                    label = "Commissions",
                    value = Formatters.formatInr(commissionBalance),
                    modifier = Modifier.weight(1f)
                )
                WalletMiniStat(
                    label = "Task Rewards",
                    value = Formatters.formatInr(taskRewardBalance),
                    modifier = Modifier.weight(1f)
                )
                WalletMiniStat(
                    label = "Pending",
                    value = Formatters.formatInr(pendingCommissions),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WalletMiniStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.78f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFeatureSettings = "tnum",
                color = Color.White,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun KpiMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, AppOutlineLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondaryLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFeatureSettings = "tnum",
                    fontWeight = FontWeight.ExtraBold
                ),
                color = TextPrimaryLight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMutedLight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

enum class BadgeTone {
    PURPLE, BLUE, SUCCESS, WARNING, ERROR, NEUTRAL
}

@Composable
fun StatusPillBadge(
    text: String,
    tone: BadgeTone,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (tone) {
        BadgeTone.PURPLE -> BrandPurpleContainer to BrandOnPurpleContainer
        BadgeTone.BLUE -> BrandBlueContainer to BrandOnBlueContainer
        BadgeTone.SUCCESS -> SuccessContainer to OnSuccessContainer
        BadgeTone.WARNING -> WarningContainer to OnWarningContainer
        BadgeTone.ERROR -> ErrorContainer to OnErrorContainer
        BadgeTone.NEUTRAL -> Color(0xFFF1F5F9) to TextSecondaryLight
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bg
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ProductImageView(
    imageUri: String?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val drawableRes = when (imageUri) {
        "res://img_prod_earbuds" -> R.drawable.img_prod_earbuds
        "res://img_prod_smartwatch" -> R.drawable.img_prod_smartwatch
        "res://img_prod_backpack" -> R.drawable.img_prod_backpack
        "res://img_hero_affiliate" -> R.drawable.img_hero_affiliate
        else -> null
    }

    if (drawableRes != null) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else if (!imageUri.isNullOrBlank()) {
        val file = File(imageUri)
        AsyncImage(
            model = if (file.exists()) file else imageUri,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.img_prod_earbuds),
            error = painterResource(id = R.drawable.img_prod_earbuds),
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.img_prod_earbuds),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

@Composable
fun EarningsAreaChart(
    points: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, AppOutlineLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "7-Day Commission & Task Trend",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimaryLight
                    )
                    Text(
                        text = "Daily tracked affiliate & task earnings (₹)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryLight
                    )
                }
                StatusPillBadge(text = "Live", tone = BadgeTone.PURPLE)
            }

            Spacer(modifier = Modifier.height(16.dp))

            val safePoints = if (points.isEmpty()) {
                listOf("Mon" to 0.0, "Tue" to 0.0, "Wed" to 0.0, "Thu" to 0.0, "Fri" to 0.0, "Sat" to 0.0, "Sun" to 0.0)
            } else points

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
            ) {
                val maxVal = (safePoints.maxOfOrNull { it.second } ?: 100.0).coerceAtLeast(100.0)
                val padX = 16.dp.toPx()
                val padTop = 12.dp.toPx()
                val padBottom = 12.dp.toPx()
                val chartW = (size.width - padX * 2).coerceAtLeast(1f)
                val chartH = (size.height - padTop - padBottom).coerceAtLeast(1f)

                // Horizontal dashed gridlines
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                for (i in 0..2) {
                    val y = padTop + chartH * (i / 2f)
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(padX, y),
                        end = Offset(size.width - padX, y),
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = dashEffect
                    )
                }

                val coords = safePoints.mapIndexed { index, pair ->
                    val x = if (safePoints.size == 1) {
                        padX + chartW / 2f
                    } else {
                        padX + (index.toFloat() / (safePoints.size - 1)) * chartW
                    }
                    val ratio = (pair.second / maxVal).toFloat().coerceIn(0f, 1f)
                    val y = padTop + chartH * (1f - ratio)
                    Offset(x, y)
                }

                if (coords.size >= 2) {
                    val linePath = Path().apply {
                        moveTo(coords.first().x, coords.first().y)
                        for (i in 0 until coords.size - 1) {
                            val p0 = coords[i]
                            val p1 = coords[i + 1]
                            val cx = (p0.x + p1.x) / 2f
                            cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        }
                    }

                    val fillPath = Path().apply {
                        addPath(linePath)
                        lineTo(coords.last().x, padTop + chartH)
                        lineTo(coords.first().x, padTop + chartH)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                BrandPurplePrimary.copy(alpha = 0.28f),
                                BrandBlueSecondary.copy(alpha = 0.03f)
                            ),
                            startY = padTop,
                            endY = padTop + chartH
                        )
                    )

                    drawPath(
                        path = linePath,
                        brush = Brush.horizontalGradient(
                            listOf(BrandPurplePrimary, BrandBlueSecondary)
                        ),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                coords.forEach { pt ->
                    drawCircle(
                        color = Color.White,
                        radius = 5.5.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = BrandPurplePrimary,
                        radius = 3.5.dp.toPx(),
                        center = pt
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                safePoints.forEach { (day, _) ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMutedLight
                    )
                }
            }
        }
    }
}

@Composable
fun PlatformRevenueBarChart(
    bars: List<Triple<String, Double, Double>>, // Label, Sales, Commissions
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, AppOutlineLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Sales vs Affiliate Commissions",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimaryLight
                    )
                    Text(
                        text = "Revenue & commission payout breakdown by category",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryLight
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(BrandBlueSecondary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sales", style = MaterialTheme.typography.labelSmall, color = TextSecondaryLight)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(BrandPurplePrimary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Comm.", style = MaterialTheme.typography.labelSmall, color = TextSecondaryLight)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val safeBars = if (bars.isEmpty()) {
                listOf(
                    Triple("Audio", 3000.0, 450.0),
                    Triple("Watch", 9998.0, 1300.0),
                    Triple("Bags", 3798.0, 550.0),
                    Triple("Tech", 3998.0, 720.0)
                )
            } else bars

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
            ) {
                val maxVal = (safeBars.maxOfOrNull { maxOf(it.second, it.third) } ?: 1000.0)
                    .coerceAtLeast(500.0)
                val groupW = size.width / safeBars.size.coerceAtLeast(1)
                val barW = (groupW * 0.26f).coerceIn(10.dp.toPx(), 24.dp.toPx())
                val gap = 4.dp.toPx()
                val topPad = 10.dp.toPx()
                val chartH = (size.height - topPad).coerceAtLeast(1f)

                safeBars.forEachIndexed { idx, (_, sales, comm) ->
                    val centerX = groupW * idx + groupW / 2f
                    val salesH = ((sales / maxVal).toFloat().coerceIn(0.05f, 1f)) * chartH
                    val commH = ((comm / maxVal).toFloat().coerceIn(0.05f, 1f)) * chartH

                    drawRoundRect(
                        color = BrandBlueSecondary,
                        topLeft = Offset(centerX - barW - gap / 2f, size.height - salesH),
                        size = Size(barW, salesH),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                    drawRoundRect(
                        color = BrandPurplePrimary,
                        topLeft = Offset(centerX + gap / 2f, size.height - commH),
                        size = Size(barW, commH),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                safeBars.forEach { (label, _, _) ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMutedLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
