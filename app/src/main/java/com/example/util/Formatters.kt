package com.example.util

import java.security.MessageDigest
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object Formatters {
    private val indianLocale = Locale("en", "IN")

    fun formatInr(amount: Double, showDecimals: Boolean = false): String {
        val formatter = NumberFormat.getCurrencyInstance(indianLocale).apply {
            minimumFractionDigits = if (showDecimals) 2 else 0
            maximumFractionDigits = if (showDecimals) 2 else 0
        }
        return formatter.format(amount)
    }

    fun formatDate(epochMs: Long): String {
        if (epochMs <= 0L) return "—"
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date(epochMs))
    }

    fun formatShortDate(epochMs: Long): String {
        if (epochMs <= 0L) return "—"
        val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
        return sdf.format(Date(epochMs))
    }

    fun formatRelativeTime(epochMs: Long, nowMs: Long = System.currentTimeMillis()): String {
        val diff = abs(nowMs - epochMs)
        val minutes = diff / 60_000L
        val hours = minutes / 60L
        val days = hours / 24L
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> formatShortDate(epochMs)
        }
    }

    fun formatCountdown(targetMs: Long, nowMs: Long = System.currentTimeMillis()): String {
        val remaining = targetMs - nowMs
        if (remaining <= 0L) return "Ended"
        val hours = remaining / 3_600_000L
        val days = hours / 24L
        val remHours = hours % 24L
        return if (days > 0) "${days}d ${remHours}h left" else "${hours.coerceAtLeast(1)}h left"
    }

    fun sha256Hex(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun isValidUpiId(upi: String): Boolean {
        val trimmed = upi.trim()
        val regex = Regex("^[a-zA-Z0-9._\\-]{2,64}@[a-zA-Z0-9]{2,32}$")
        return regex.matches(trimmed)
    }
}
