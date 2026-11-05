package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class UserRole {
    USER,
    ADMIN
}

enum class CommissionType {
    PERCENTAGE,
    FIXED
}

enum class OrderStatus(val label: String) {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled")
}

enum class CommissionStatus(val label: String) {
    PENDING_DELIVERY("Pending Delivery"),
    IN_RETURN_WINDOW("Return Period Lock"),
    PAYABLE("Payable"),
    APPROVED_PAID("Credited"),
    REJECTED("Rejected")
}

enum class TaskCategory(val label: String) {
    SOCIAL_SHARE("Social Share"),
    VIDEO_REVIEW("Video Reel"),
    MERCHANT_ONBOARD("Store Onboarding"),
    COMMUNITY_POST("Community Post"),
    PRODUCT_DEMO("Product Demo")
}

enum class SubmissionStatus(val label: String) {
    PENDING("Under Review"),
    APPROVED("Approved"),
    REJECTED("Rejected")
}

enum class WithdrawalStatus(val label: String) {
    PENDING("Pending Approval"),
    APPROVED("Paid via UPI"),
    REJECTED("Rejected")
}

enum class NotificationType(val label: String) {
    NEW_PRODUCT("New Product"),
    NEW_TASK("New Task"),
    COMMISSION_EARNED("Commission"),
    WITHDRAWAL_UPDATE("Withdrawal"),
    ORDER_UPDATE("Order")
}

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["referralCode"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val passwordHash: String,
    val role: UserRole = UserRole.USER,
    val referralCode: String,
    val referredByCode: String? = null,
    val upiId: String = "",
    val upiAccountName: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "products",
    indices = [Index(value = ["skuCode"], unique = true)]
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val skuCode: String,
    val name: String,
    val description: String,
    val category: String,
    val priceInr: Double,
    val stock: Int,
    val commissionType: CommissionType,
    val commissionValue: Double,
    val returnWindowDays: Int = 7,
    val imageUri: String = "res://img_prod_earbuds",
    val qrPayloadBase: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun calculateCommission(quantity: Int): Double {
        return when (commissionType) {
            CommissionType.PERCENTAGE -> (priceInr * quantity) * (commissionValue / 100.0)
            CommissionType.FIXED -> commissionValue * quantity
        }
    }

    fun commissionBadgeText(): String {
        return when (commissionType) {
            CommissionType.PERCENTAGE -> "${commissionValue.toInt()}% Comm."
            CommissionType.FIXED -> "₹${commissionValue.toInt()} Flat"
        }
    }
}

@Entity(
    tableName = "orders",
    indices = [
        Index(value = ["orderNumber"], unique = true),
        Index(value = ["claimSignature"], unique = true)
    ]
)
data class OrderEntity(
    @PrimaryKey val id: String,
    val orderNumber: String,
    val productId: String,
    val productName: String,
    val productImageUri: String,
    val quantity: Int,
    val unitPriceInr: Double,
    val totalAmountInr: Double,
    val customerName: String,
    val customerPhone: String,
    val deliveryAddress: String,
    val city: String,
    val pincode: String,
    val paymentMethod: String,
    val referrerUserId: String? = null,
    val referralCode: String? = null,
    val status: OrderStatus = OrderStatus.PENDING,
    val claimSignature: String,
    val createdAt: Long = System.currentTimeMillis(),
    val deliveredAt: Long? = null,
    val returnWindowEndsAt: Long? = null
)

@Entity(
    tableName = "commissions",
    indices = [Index(value = ["orderId"], unique = true)]
)
data class CommissionEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val orderNumber: String,
    val productId: String,
    val productName: String,
    val affiliateUserId: String,
    val referralCode: String,
    val saleAmountInr: Double,
    val commissionRateLabel: String,
    val commissionAmountInr: Double,
    val status: CommissionStatus = CommissionStatus.PENDING_DELIVERY,
    val returnWindowEndsAt: Long? = null,
    val adminNote: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val instructions: String,
    val category: TaskCategory,
    val rewardInr: Double,
    val deadlineAt: Long,
    val imageUri: String = "res://img_hero_affiliate",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "task_submissions")
data class TaskSubmissionEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val taskTitle: String,
    val userId: String,
    val userName: String,
    val userReferralCode: String,
    val proofImageUri: String? = null,
    val proofNotes: String,
    val proofLink: String = "",
    val rewardInr: Double,
    val status: SubmissionStatus = SubmissionStatus.PENDING,
    val adminNote: String = "",
    val submittedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null
)

@Entity(tableName = "withdrawals")
data class WithdrawalEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val upiId: String,
    val upiAccountName: String,
    val amountInr: Double,
    val status: WithdrawalStatus = WithdrawalStatus.PENDING,
    val utrReference: String = "",
    val adminNote: String = "",
    val requestedAt: Long = System.currentTimeMillis(),
    val processedAt: Long? = null
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val recipientUserId: String, // "ALL", "ADMIN", or specific userId
    val type: NotificationType,
    val title: String,
    val message: String,
    val relatedId: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey val id: Int = 1,
    val minWithdrawalInr: Double = 100.0,
    val requireManualCommissionApproval: Boolean = true,
    val platformAnnouncement: String = "Earn up to 18% instant referral commission + daily task rewards!",
    val updatedAt: Long = System.currentTimeMillis()
)

class CommissionWalaConverters {
    @TypeConverter fun fromUserRole(v: UserRole): String = v.name
    @TypeConverter fun toUserRole(v: String): UserRole = UserRole.valueOf(v)

    @TypeConverter fun fromCommissionType(v: CommissionType): String = v.name
    @TypeConverter fun toCommissionType(v: String): CommissionType = CommissionType.valueOf(v)

    @TypeConverter fun fromOrderStatus(v: OrderStatus): String = v.name
    @TypeConverter fun toOrderStatus(v: String): OrderStatus = OrderStatus.valueOf(v)

    @TypeConverter fun fromCommissionStatus(v: CommissionStatus): String = v.name
    @TypeConverter fun toCommissionStatus(v: String): CommissionStatus = CommissionStatus.valueOf(v)

    @TypeConverter fun fromTaskCategory(v: TaskCategory): String = v.name
    @TypeConverter fun toTaskCategory(v: String): TaskCategory = TaskCategory.valueOf(v)

    @TypeConverter fun fromSubmissionStatus(v: SubmissionStatus): String = v.name
    @TypeConverter fun toSubmissionStatus(v: String): SubmissionStatus = SubmissionStatus.valueOf(v)

    @TypeConverter fun fromWithdrawalStatus(v: WithdrawalStatus): String = v.name
    @TypeConverter fun toWithdrawalStatus(v: String): WithdrawalStatus = WithdrawalStatus.valueOf(v)

    @TypeConverter fun fromNotificationType(v: NotificationType): String = v.name
    @TypeConverter fun toNotificationType(v: String): NotificationType = NotificationType.valueOf(v)
}
