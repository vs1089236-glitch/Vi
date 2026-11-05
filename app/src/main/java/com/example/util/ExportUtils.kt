package com.example.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.local.CommissionEntity
import com.example.data.local.OrderEntity
import com.example.data.local.UserEntity
import com.example.data.local.WithdrawalEntity
import com.example.data.repository.AdminDashboardSummary
import com.example.data.repository.WalletSummary
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {

    private fun csvEscape(raw: String): String {
        val escaped = raw.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun formatCsvTimestamp(epochMs: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        return sdf.format(Date(epochMs))
    }

    private fun writeCsvToDownloads(context: Context, fileName: String, csvContent: String): Result<String> {
        return try {
            val bytes = csvContent.toByteArray(Charsets.UTF_8)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/CommissionWala")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        out.write(bytes)
                    }
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    return Result.success(fileName)
                }
            }

            val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            val outDir = File(baseDir, "CommissionWala").apply { mkdirs() }
            val outFile = File(outDir, fileName)
            FileOutputStream(outFile).use { out ->
                out.write(bytes)
            }
            Result.success(fileName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportAffiliateStatementCsv(
        context: Context,
        user: UserEntity,
        wallet: WalletSummary,
        commissions: List<CommissionEntity>,
        withdrawals: List<WithdrawalEntity>
    ): Result<String> {
        val sb = StringBuilder()
        sb.appendLine("CommissionWala Affiliate Earnings & Payout Statement")
        sb.appendLine("Affiliate Name,${csvEscape(user.fullName)}")
        sb.appendLine("Email,${csvEscape(user.email)}")
        sb.appendLine("Referral ID,${csvEscape(user.referralCode)}")
        sb.appendLine("Verified UPI ID,${csvEscape(user.upiId.ifBlank { "Not configured" })}")
        sb.appendLine("Available Wallet Balance (INR),${wallet.availableBalanceInr}")
        sb.appendLine("Credited Commissions (INR),${wallet.creditedCommissionInr}")
        sb.appendLine("Task Rewards (INR),${wallet.taskRewardBalanceInr}")
        sb.appendLine("Pending Commissions (INR),${wallet.pendingCommissionInr}")
        sb.appendLine("Total Withdrawn (INR),${wallet.totalWithdrawnInr}")
        sb.appendLine()
        sb.appendLine("COMMISSION LEDGER")
        sb.appendLine("Order Number,Product Name,Sale Amount (INR),Commission Rate,Commission Earned (INR),Status,Referral Code,Date")
        commissions.forEach { c ->
            sb.appendLine(
                listOf(
                    csvEscape(c.orderNumber),
                    csvEscape(c.productName),
                    c.saleAmountInr.toString(),
                    csvEscape(c.commissionRateLabel),
                    c.commissionAmountInr.toString(),
                    csvEscape(c.status.label),
                    csvEscape(c.referralCode),
                    csvEscape(formatCsvTimestamp(c.createdAt))
                ).joinToString(",")
            )
        }
        sb.appendLine()
        sb.appendLine("UPI WITHDRAWAL HISTORY")
        sb.appendLine("Request ID,UPI ID,Account Holder,Amount (INR),Status,UTR Reference,Requested Date")
        withdrawals.forEach { w ->
            sb.appendLine(
                listOf(
                    csvEscape(w.id),
                    csvEscape(w.upiId),
                    csvEscape(w.upiAccountName),
                    w.amountInr.toString(),
                    csvEscape(w.status.label),
                    csvEscape(w.utrReference.ifBlank { "-" }),
                    csvEscape(formatCsvTimestamp(w.requestedAt))
                ).joinToString(",")
            )
        }

        val safeRef = user.referralCode.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val fileName = "CommissionWala_Statement_${safeRef}_${System.currentTimeMillis()}.csv"
        return writeCsvToDownloads(context, fileName, sb.toString())
    }

    fun exportAdminReportCsv(
        context: Context,
        summary: AdminDashboardSummary,
        orders: List<OrderEntity>,
        commissions: List<CommissionEntity>,
        withdrawals: List<WithdrawalEntity>
    ): Result<String> {
        val sb = StringBuilder()
        sb.appendLine("CommissionWala Owner Platform Report")
        sb.appendLine("Total Active Sales (INR),${summary.totalSalesInr}")
        sb.appendLine("Total Orders,${summary.totalOrdersCount}")
        sb.appendLine("Pending Orders,${summary.pendingOrdersCount}")
        sb.appendLine("Total Commission Liability (INR),${summary.totalCommissionsLiabilityInr}")
        sb.appendLine("Pending UPI Withdrawals (INR),${summary.pendingWithdrawalsInr}")
        sb.appendLine("Completed UPI Payouts (INR),${summary.completedWithdrawalsInr}")
        sb.appendLine("Registered Affiliates,${summary.totalRegisteredUsersCount}")
        sb.appendLine()
        sb.appendLine("CUSTOMER ORDERS & REFERRAL ATTRIBUTION")
        sb.appendLine("Order Number,Product Name,Quantity,Total Amount (INR),Customer Name,City,Payment Method,Referral Code,Order Status,Date")
        orders.forEach { o ->
            sb.appendLine(
                listOf(
                    csvEscape(o.orderNumber),
                    csvEscape(o.productName),
                    o.quantity.toString(),
                    o.totalAmountInr.toString(),
                    csvEscape(o.customerName),
                    csvEscape(o.city),
                    csvEscape(o.paymentMethod),
                    csvEscape(o.referralCode ?: "DIRECT"),
                    csvEscape(o.status.label),
                    csvEscape(formatCsvTimestamp(o.createdAt))
                ).joinToString(",")
            )
        }
        sb.appendLine()
        sb.appendLine("AFFILIATE COMMISSIONS")
        sb.appendLine("Commission ID,Order Number,Referral Code,Product,Commission (INR),Status,Note")
        commissions.forEach { c ->
            sb.appendLine(
                listOf(
                    csvEscape(c.id),
                    csvEscape(c.orderNumber),
                    csvEscape(c.referralCode),
                    csvEscape(c.productName),
                    c.commissionAmountInr.toString(),
                    csvEscape(c.status.label),
                    csvEscape(c.adminNote)
                ).joinToString(",")
            )
        }
        sb.appendLine()
        sb.appendLine("UPI WITHDRAWAL REQUESTS")
        sb.appendLine("Withdrawal ID,Affiliate,UPI ID,Amount (INR),Status,UTR Reference")
        withdrawals.forEach { w ->
            sb.appendLine(
                listOf(
                    csvEscape(w.id),
                    csvEscape(w.userName),
                    csvEscape(w.upiId),
                    w.amountInr.toString(),
                    csvEscape(w.status.label),
                    csvEscape(w.utrReference.ifBlank { "-" })
                ).joinToString(",")
            )
        }

        val fileName = "CommissionWala_Admin_Report_${System.currentTimeMillis()}.csv"
        return writeCsvToDownloads(context, fileName, sb.toString())
    }
}
