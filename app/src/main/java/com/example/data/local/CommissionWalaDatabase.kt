package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.util.Formatters
import com.example.util.QrCodeUtils

@Database(
    entities = [
        UserEntity::class,
        ProductEntity::class,
        OrderEntity::class,
        CommissionEntity::class,
        TaskEntity::class,
        TaskSubmissionEntity::class,
        WithdrawalEntity::class,
        NotificationEntity::class,
        AppConfigEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(CommissionWalaConverters::class)
abstract class CommissionWalaDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun commissionDao(): CommissionDao
    abstract fun taskDao(): TaskDao
    abstract fun taskSubmissionDao(): TaskSubmissionDao
    abstract fun withdrawalDao(): WithdrawalDao
    abstract fun notificationDao(): NotificationDao
    abstract fun appConfigDao(): AppConfigDao

    companion object {
        @Volatile
        private var INSTANCE: CommissionWalaDatabase? = null

        fun getInstance(context: Context): CommissionWalaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CommissionWalaDatabase::class.java,
                    "commissionwala.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

object DatabaseSeeder {
    suspend fun seedIfEmpty(db: CommissionWalaDatabase) {
        if (db.userDao().getUserCount() > 0) return

        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L

        db.appConfigDao().upsertConfig(
            AppConfigEntity(
                id = 1,
                minWithdrawalInr = 100.0,
                requireManualCommissionApproval = true,
                platformAnnouncement = "Festival Boost: Up to 18% commission on Audio & Wearables!",
                updatedAt = now
            )
        )

        // Seed Admin + 3 Affiliate Users
        val adminId = "usr_admin_01"
        val aaravId = "usr_aarav_02"
        val snehaId = "usr_sneha_03"
        val rohanId = "usr_rohan_04"

        val users = listOf(
            UserEntity(
                id = adminId,
                fullName = "Vikram Mehta (Owner)",
                email = "admin@commissionwala.in",
                phone = "9800000001",
                passwordHash = Formatters.sha256Hex("admin123"),
                role = UserRole.ADMIN,
                referralCode = "CW-OWNER-0001",
                upiId = "commissionwala@icici",
                upiAccountName = "CommissionWala Official",
                createdAt = now - 30 * dayMs
            ),
            UserEntity(
                id = aaravId,
                fullName = "Aarav Sharma",
                email = "aarav@commissionwala.in",
                phone = "9876543210",
                passwordHash = Formatters.sha256Hex("user123"),
                role = UserRole.USER,
                referralCode = "CW-AARAV-1042",
                upiId = "aarav.sharma@okaxis",
                upiAccountName = "Aarav Sharma",
                createdAt = now - 20 * dayMs
            ),
            UserEntity(
                id = snehaId,
                fullName = "Sneha Verma",
                email = "sneha@commissionwala.in",
                phone = "9822334455",
                passwordHash = Formatters.sha256Hex("user123"),
                role = UserRole.USER,
                referralCode = "CW-SNEHA-2088",
                referredByCode = "CW-AARAV-1042",
                upiId = "sneha.v@ybl",
                upiAccountName = "Sneha Verma",
                createdAt = now - 14 * dayMs
            ),
            UserEntity(
                id = rohanId,
                fullName = "Rohan Kulkarni",
                email = "rohan@commissionwala.in",
                phone = "9811223344",
                passwordHash = Formatters.sha256Hex("user123"),
                role = UserRole.USER,
                referralCode = "CW-ROHAN-3319",
                upiId = "rohan.k@paytm",
                upiAccountName = "Rohan Kulkarni",
                createdAt = now - 10 * dayMs
            )
        )
        users.forEach { db.userDao().insertUser(it) }

        // Seed 6 Sample Products with varied % and fixed commissions
        val products = listOf(
            ProductEntity(
                id = "prod_01",
                skuCode = "PRD-ANC-101",
                name = "SonicPulse Pro ANC Earbuds",
                description = "48dB Hybrid Active Noise Cancellation, 42-hour battery life, spatial audio, and ultra-low latency gaming mode.",
                category = "Audio",
                priceInr = 2999.0,
                stock = 45,
                commissionType = CommissionType.PERCENTAGE,
                commissionValue = 15.0,
                returnWindowDays = 7,
                imageUri = "res://img_prod_earbuds",
                qrPayloadBase = QrCodeUtils.buildProductQrPayload("PRD-ANC-101", null),
                createdAt = now - 12 * dayMs
            ),
            ProductEntity(
                id = "prod_02",
                skuCode = "PRD-WTC-202",
                name = "AeroFit Titanium AMOLED Watch",
                description = "1.43-inch 60Hz AMOLED display, dual-band GPS, continuous SpO2 & heart-rate tracking, 14-day battery.",
                category = "Wearables",
                priceInr = 4999.0,
                stock = 28,
                commissionType = CommissionType.FIXED,
                commissionValue = 650.0,
                returnWindowDays = 7,
                imageUri = "res://img_prod_smartwatch",
                qrPayloadBase = QrCodeUtils.buildProductQrPayload("PRD-WTC-202", null),
                createdAt = now - 10 * dayMs
            ),
            ProductEntity(
                id = "prod_03",
                skuCode = "PRD-BAG-303",
                name = "NomadVault 28L Tech Backpack",
                description = "Water-repellent ballistic nylon commuter backpack with 16-inch laptop vault, USB-C pass-through, and anti-theft pocket.",
                category = "Lifestyle",
                priceInr = 2499.0,
                stock = 60,
                commissionType = CommissionType.PERCENTAGE,
                commissionValue = 14.0,
                returnWindowDays = 5,
                imageUri = "res://img_prod_backpack",
                qrPayloadBase = QrCodeUtils.buildProductQrPayload("PRD-BAG-303", null),
                createdAt = now - 8 * dayMs
            ),
            ProductEntity(
                id = "prod_04",
                skuCode = "PRD-CHG-404",
                name = "VoltCore 65W GaN Fast Charger",
                description = "3-port GaN III wall adapter supporting PD 3.0 & PPS super-fast charging for laptops, tablets, and phones.",
                category = "Electronics",
                priceInr = 1699.0,
                stock = 80,
                commissionType = CommissionType.FIXED,
                commissionValue = 250.0,
                returnWindowDays = 3,
                imageUri = "res://img_prod_earbuds",
                qrPayloadBase = QrCodeUtils.buildProductQrPayload("PRD-CHG-404", null),
                createdAt = now - 6 * dayMs
            ),
            ProductEntity(
                id = "prod_05",
                skuCode = "PRD-BND-505",
                name = "FitTrack Smart Band 8 Pro",
                description = "Ultra-slim fitness & sleep tracker with 120+ workout modes, 5ATM swim resistance, and instant notification alerts.",
                category = "Wearables",
                priceInr = 1999.0,
                stock = 35,
                commissionType = CommissionType.PERCENTAGE,
                commissionValue = 18.0,
                returnWindowDays = 7,
                imageUri = "res://img_prod_smartwatch",
                qrPayloadBase = QrCodeUtils.buildProductQrPayload("PRD-BND-505", null),
                createdAt = now - 4 * dayMs
            ),
            ProductEntity(
                id = "prod_06",
                skuCode = "PRD-SLG-606",
                name = "UrbanCross Minimalist Sling",
                description = "Compact weather-sealed everyday crossbody bag with magnetic quick-release buckle and RFID-safe passport sleeve.",
                category = "Accessories",
                priceInr = 1299.0,
                stock = 52,
                commissionType = CommissionType.FIXED,
                commissionValue = 200.0,
                returnWindowDays = 0, // Instant commission upon delivery!
                imageUri = "res://img_prod_backpack",
                qrPayloadBase = QrCodeUtils.buildProductQrPayload("PRD-SLG-606", null),
                createdAt = now - 2 * dayMs
            )
        )
        products.forEach { db.productDao().upsertProduct(it) }

        // Seed Sample Orders & Commissions across lifecycle stages
        val orders = listOf(
            OrderEntity(
                id = "ord_01",
                orderNumber = "CW-ORD-9041",
                productId = "prod_02",
                productName = "AeroFit Titanium AMOLED Watch",
                productImageUri = "res://img_prod_smartwatch",
                quantity = 2,
                unitPriceInr = 4999.0,
                totalAmountInr = 9998.0,
                customerName = "Karan Malhotra",
                customerPhone = "9900112233",
                deliveryAddress = "42 Indiranagar 100ft Road",
                city = "Bengaluru",
                pincode = "560038",
                paymentMethod = "UPI",
                referrerUserId = aaravId,
                referralCode = "CW-AARAV-1042",
                status = OrderStatus.DELIVERED,
                claimSignature = Formatters.sha256Hex("seed_ord_01"),
                createdAt = now - 9 * dayMs,
                deliveredAt = now - 8 * dayMs,
                returnWindowEndsAt = now - 1 * dayMs
            ),
            OrderEntity(
                id = "ord_02",
                orderNumber = "CW-ORD-9042",
                productId = "prod_01",
                productName = "SonicPulse Pro ANC Earbuds",
                productImageUri = "res://img_prod_earbuds",
                quantity = 1,
                unitPriceInr = 2999.0,
                totalAmountInr = 2999.0,
                customerName = "Meera Nair",
                customerPhone = "9911223344",
                deliveryAddress = "18 Bandra West, Linking Road",
                city = "Mumbai",
                pincode = "400050",
                paymentMethod = "UPI",
                referrerUserId = aaravId,
                referralCode = "CW-AARAV-1042",
                status = OrderStatus.DELIVERED,
                claimSignature = Formatters.sha256Hex("seed_ord_02"),
                createdAt = now - 4 * dayMs,
                deliveredAt = now - 2 * dayMs,
                returnWindowEndsAt = now + 5 * dayMs // Still inside 7-day return window!
            ),
            OrderEntity(
                id = "ord_03",
                orderNumber = "CW-ORD-9043",
                productId = "prod_03",
                productName = "NomadVault 28L Tech Backpack",
                productImageUri = "res://img_prod_backpack",
                quantity = 1,
                unitPriceInr = 2499.0,
                totalAmountInr = 2499.0,
                customerName = "Siddharth Joshi",
                customerPhone = "9922334455",
                deliveryAddress = "77 Koregaon Park, Lane 5",
                city = "Pune",
                pincode = "411001",
                paymentMethod = "Cash on Delivery",
                referrerUserId = aaravId,
                referralCode = "CW-AARAV-1042",
                status = OrderStatus.SHIPPED,
                claimSignature = Formatters.sha256Hex("seed_ord_03"),
                createdAt = now - 1 * dayMs
            ),
            OrderEntity(
                id = "ord_04",
                orderNumber = "CW-ORD-9044",
                productId = "prod_05",
                productName = "FitTrack Smart Band 8 Pro",
                productImageUri = "res://img_prod_smartwatch",
                quantity = 2,
                unitPriceInr = 1999.0,
                totalAmountInr = 3998.0,
                customerName = "Ananya Sen",
                customerPhone = "9933445566",
                deliveryAddress = "12 Salt Lake Sector V",
                city = "Kolkata",
                pincode = "700091",
                paymentMethod = "UPI",
                referrerUserId = snehaId,
                referralCode = "CW-SNEHA-2088",
                status = OrderStatus.DELIVERED,
                claimSignature = Formatters.sha256Hex("seed_ord_04"),
                createdAt = now - 8 * dayMs,
                deliveredAt = now - 7 * dayMs,
                returnWindowEndsAt = now - 12 * 3_600_000L
            ),
            OrderEntity(
                id = "ord_05",
                orderNumber = "CW-ORD-9045",
                productId = "prod_06",
                productName = "UrbanCross Minimalist Sling",
                productImageUri = "res://img_prod_backpack",
                quantity = 1,
                unitPriceInr = 1299.0,
                totalAmountInr = 1299.0,
                customerName = "Devansh Patel",
                customerPhone = "9944556677",
                deliveryAddress = "9 CG Road, Navrangpura",
                city = "Ahmedabad",
                pincode = "380009",
                paymentMethod = "UPI",
                referrerUserId = aaravId,
                referralCode = "CW-AARAV-1042",
                status = OrderStatus.PENDING,
                claimSignature = Formatters.sha256Hex("seed_ord_05"),
                createdAt = now - 3 * 3_600_000L
            )
        )
        orders.forEach { db.orderDao().insertOrder(it) }

        val commissions = listOf(
            CommissionEntity(
                id = "comm_01",
                orderId = "ord_01",
                orderNumber = "CW-ORD-9041",
                productId = "prod_02",
                productName = "AeroFit Titanium AMOLED Watch",
                affiliateUserId = aaravId,
                referralCode = "CW-AARAV-1042",
                saleAmountInr = 9998.0,
                commissionRateLabel = "₹650 × 2",
                commissionAmountInr = 1300.0,
                status = CommissionStatus.APPROVED_PAID,
                returnWindowEndsAt = now - 1 * dayMs,
                adminNote = "Delivered & return period completed",
                createdAt = now - 9 * dayMs,
                resolvedAt = now - 1 * dayMs
            ),
            CommissionEntity(
                id = "comm_02",
                orderId = "ord_02",
                orderNumber = "CW-ORD-9042",
                productId = "prod_01",
                productName = "SonicPulse Pro ANC Earbuds",
                affiliateUserId = aaravId,
                referralCode = "CW-AARAV-1042",
                saleAmountInr = 2999.0,
                commissionRateLabel = "15%",
                commissionAmountInr = 450.0,
                status = CommissionStatus.IN_RETURN_WINDOW,
                returnWindowEndsAt = now + 5 * dayMs,
                adminNote = "7-day return protection active",
                createdAt = now - 4 * dayMs
            ),
            CommissionEntity(
                id = "comm_03",
                orderId = "ord_03",
                orderNumber = "CW-ORD-9043",
                productId = "prod_03",
                productName = "NomadVault 28L Tech Backpack",
                affiliateUserId = aaravId,
                referralCode = "CW-AARAV-1042",
                saleAmountInr = 2499.0,
                commissionRateLabel = "14%",
                commissionAmountInr = 350.0,
                status = CommissionStatus.PENDING_DELIVERY,
                createdAt = now - 1 * dayMs
            ),
            CommissionEntity(
                id = "comm_04",
                orderId = "ord_04",
                orderNumber = "CW-ORD-9044",
                productId = "prod_05",
                productName = "FitTrack Smart Band 8 Pro",
                affiliateUserId = snehaId,
                referralCode = "CW-SNEHA-2088",
                saleAmountInr = 3998.0,
                commissionRateLabel = "18%",
                commissionAmountInr = 720.0,
                status = CommissionStatus.PAYABLE,
                returnWindowEndsAt = now - 12 * 3_600_000L,
                adminNote = "Ready for admin payout approval",
                createdAt = now - 8 * dayMs
            ),
            CommissionEntity(
                id = "comm_05",
                orderId = "ord_05",
                orderNumber = "CW-ORD-9045",
                productId = "prod_06",
                productName = "UrbanCross Minimalist Sling",
                affiliateUserId = aaravId,
                referralCode = "CW-AARAV-1042",
                saleAmountInr = 1299.0,
                commissionRateLabel = "₹200 Flat",
                commissionAmountInr = 200.0,
                status = CommissionStatus.PENDING_DELIVERY,
                createdAt = now - 3 * 3_600_000L
            )
        )
        commissions.forEach { db.commissionDao().insertCommission(it) }

        // Seed 5 Sample Tasks
        val tasks = listOf(
            TaskEntity(
                id = "task_01",
                title = "Share SonicPulse Earbuds QR on WhatsApp Status",
                description = "Post your personalized SonicPulse Pro ANC Earbuds QR card on your WhatsApp Status for 24 hours.",
                instructions = "1. Open SonicPulse Earbuds in Products tab.\n2. Tap 'QR & Share' and share to WhatsApp Status.\n3. Take a screenshot showing your status view count and upload it below.",
                category = TaskCategory.SOCIAL_SHARE,
                rewardInr = 150.0,
                deadlineAt = now + 5 * dayMs,
                imageUri = "res://img_prod_earbuds",
                createdAt = now - 5 * dayMs
            ),
            TaskEntity(
                id = "task_02",
                title = "Post 30-Sec Instagram Reel for AeroFit Watch",
                description = "Create a short vertical showcase reel highlighting the AeroFit Titanium AMOLED Watch with your referral link in bio.",
                instructions = "1. Download the AeroFit Watch QR & promo details.\n2. Publish a 15–30s Reel tagging @commissionwala.\n3. Paste your Reel URL and attach a screenshot of the published post.",
                category = TaskCategory.VIDEO_REVIEW,
                rewardInr = 350.0,
                deadlineAt = now + 9 * dayMs,
                imageUri = "res://img_prod_smartwatch",
                createdAt = now - 4 * dayMs
            ),
            TaskEntity(
                id = "task_03",
                title = "Place Printed Product QR Standee at Local Cafe/Shop",
                description = "Print and display a CommissionWala product QR card at a partner retail counter or college notice board.",
                instructions = "1. Download any high-commission product QR PNG.\n2. Print and place it on a counter/board.\n3. Upload a clear photo of the displayed QR standee.",
                category = TaskCategory.MERCHANT_ONBOARD,
                rewardInr = 500.0,
                deadlineAt = now + 14 * dayMs,
                imageUri = "res://img_hero_affiliate",
                createdAt = now - 3 * dayMs
            ),
            TaskEntity(
                id = "task_04",
                title = "Share NomadVault Backpack in 3 College/Tech Groups",
                description = "Recommend the NomadVault 28L Tech Backpack with your unique referral link in Telegram or WhatsApp communities.",
                instructions = "1. Copy your NomadVault referral link.\n2. Share in 3 relevant student or commuter groups.\n3. Upload screenshot proof of the shared messages.",
                category = TaskCategory.COMMUNITY_POST,
                rewardInr = 120.0,
                deadlineAt = now + 4 * dayMs,
                imageUri = "res://img_prod_backpack",
                createdAt = now - 2 * dayMs
            ),
            TaskEntity(
                id = "task_05",
                title = "Complete First QR Test Checkout Walkthrough",
                description = "Test the end-to-end QR scanner & referral attribution flow to verify your affiliate tracking setup.",
                instructions = "1. Share a product QR with a friend or customer.\n2. Have them scan the QR and place an order.\n3. Submit the order number below for bonus activation reward.",
                category = TaskCategory.PRODUCT_DEMO,
                rewardInr = 200.0,
                deadlineAt = now + 12 * dayMs,
                imageUri = "res://img_hero_affiliate",
                createdAt = now - 1 * dayMs
            )
        )
        tasks.forEach { db.taskDao().upsertTask(it) }

        // Seed Sample Task Submissions (1 Approved for Aarav so his Task Reward Balance is ₹150, 1 Pending for Admin review!)
        db.taskSubmissionDao().insertSubmission(
            TaskSubmissionEntity(
                id = "sub_01",
                taskId = "task_01",
                taskTitle = "Share SonicPulse Earbuds QR on WhatsApp Status",
                userId = aaravId,
                userName = "Aarav Sharma",
                userReferralCode = "CW-AARAV-1042",
                proofNotes = "Posted on WhatsApp Status — 84 views in 6 hours!",
                proofLink = "https://wa.me/status/aarav1042",
                rewardInr = 150.0,
                status = SubmissionStatus.APPROVED,
                adminNote = "Verified status screenshot. Reward credited!",
                submittedAt = now - 3 * dayMs,
                reviewedAt = now - 2 * dayMs
            )
        )
        db.taskSubmissionDao().insertSubmission(
            TaskSubmissionEntity(
                id = "sub_02",
                taskId = "task_02",
                taskTitle = "Post 30-Sec Instagram Reel for AeroFit Watch",
                userId = snehaId,
                userName = "Sneha Verma",
                userReferralCode = "CW-SNEHA-2088",
                proofNotes = "Published 25-sec AMOLED watch review reel with referral link in bio.",
                proofLink = "https://instagram.com/reel/CW2088Sneha",
                rewardInr = 350.0,
                status = SubmissionStatus.PENDING,
                submittedAt = now - 5 * 3_600_000L
            )
        )

        // Seed Sample Withdrawals (1 Approved ₹500 for Aarav -> leaving ₹1300 + ₹150 - ₹500 = ₹950 available balance! + 1 Pending ₹400 for Sneha)
        db.withdrawalDao().insertWithdrawal(
            WithdrawalEntity(
                id = "wd_01",
                userId = aaravId,
                userName = "Aarav Sharma",
                upiId = "aarav.sharma@okaxis",
                upiAccountName = "Aarav Sharma",
                amountInr = 500.0,
                status = WithdrawalStatus.APPROVED,
                utrReference = "UTR430918274610",
                adminNote = "Instant IMPS/UPI payout settled",
                requestedAt = now - 2 * dayMs,
                processedAt = now - 1 * dayMs
            )
        )
        db.withdrawalDao().insertWithdrawal(
            WithdrawalEntity(
                id = "wd_02",
                userId = snehaId,
                userName = "Sneha Verma",
                upiId = "sneha.v@ybl",
                upiAccountName = "Sneha Verma",
                amountInr = 250.0,
                status = WithdrawalStatus.PENDING,
                requestedAt = now - 4 * 3_600_000L
            )
        )

        // Seed Notifications
        val notifications = listOf(
            NotificationEntity(
                id = "notif_01",
                recipientUserId = aaravId,
                type = NotificationType.COMMISSION_EARNED,
                title = "Commission Credited: ₹1,300",
                message = "Order #CW-ORD-9041 completed its return period. ₹1,300 added to your wallet!",
                relatedId = "comm_01",
                createdAt = now - 1 * dayMs
            ),
            NotificationEntity(
                id = "notif_02",
                recipientUserId = aaravId,
                type = NotificationType.WITHDRAWAL_UPDATE,
                title = "UPI Payout Settled (₹500)",
                message = "₹500 sent to aarav.sharma@okaxis • UTR: UTR430918274610",
                relatedId = "wd_01",
                createdAt = now - 1 * dayMs
            ),
            NotificationEntity(
                id = "notif_03",
                recipientUserId = "ALL",
                type = NotificationType.NEW_PRODUCT,
                title = "New High-Margin Product Added!",
                message = "UrbanCross Minimalist Sling is live with ₹200 flat commission & 0-day return lock.",
                relatedId = "prod_06",
                createdAt = now - 2 * dayMs
            ),
            NotificationEntity(
                id = "notif_04",
                recipientUserId = "ALL",
                type = NotificationType.NEW_TASK,
                title = "New Reward Task: Earn ₹500",
                message = "Place a printed product QR standee at a local shop or campus board.",
                relatedId = "task_03",
                createdAt = now - 3 * dayMs
            ),
            NotificationEntity(
                id = "notif_05",
                recipientUserId = "ADMIN",
                type = NotificationType.WITHDRAWAL_UPDATE,
                title = "Pending UPI Withdrawal: ₹250",
                message = "Sneha Verma requested ₹250 payout to sneha.v@ybl.",
                relatedId = "wd_02",
                createdAt = now - 4 * 3_600_000L
            )
        )
        notifications.forEach { db.notificationDao().insertNotification(it) }
    }
}
