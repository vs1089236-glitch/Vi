package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CommissionStatus
import com.example.data.local.CommissionType
import com.example.data.local.CommissionWalaDatabase
import com.example.data.local.OrderStatus
import com.example.data.local.SessionPreferences
import com.example.data.local.TaskCategory
import com.example.data.local.WithdrawalStatus
import com.example.data.repository.CommissionWalaRepository
import com.example.util.ExportUtils
import com.example.util.Formatters
import com.example.util.QrCodeUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: CommissionWalaDatabase
    private lateinit var repository: CommissionWalaRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, CommissionWalaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val prefs = SessionPreferences(context)
        repository = CommissionWalaRepository(db, prefs)
        runBlocking {
            repository.initialize()
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `app name matches CommissionWala`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("CommissionWala", appName)
    }

    @Test
    fun `qr payload generation and parsing preserves sku and referral id`() {
        val url = QrCodeUtils.buildProductQrPayload("PRD-AUD-101", "CW-AARAV-1042")
        assertEquals("https://commissionwala.app/p/PRD-AUD-101?ref=CW-AARAV-1042", url)

        val parsedUrl = QrCodeUtils.parseCommissionWalaQr(url)
        assertNotNull(parsedUrl)
        assertEquals("PRD-AUD-101", parsedUrl?.skuCode)
        assertEquals("CW-AARAV-1042", parsedUrl?.referralCode)

        val parsedPipe = QrCodeUtils.parseCommissionWalaQr("PRD-WTC-204|CW-SNEHA-2088")
        assertNotNull(parsedPipe)
        assertEquals("PRD-WTC-204", parsedPipe?.skuCode)
        assertEquals("CW-SNEHA-2088", parsedPipe?.referralCode)

        val bitmap = QrCodeUtils.generateQrBitmap(url, sizePx = 256)
        assertEquals(256, bitmap.width)
        assertEquals(256, bitmap.height)
    }

    @Test
    fun `referral order placement credits affiliate commission and blocks self-referral and duplicate replay`() = runBlocking {
        val products = repository.allProductsFlow.first()
        assertTrue(products.isNotEmpty())
        val product = products.first()
        val initialStock = product.stock

        // Valid customer order through Aarav's referral QR
        val orderResult = repository.placeReferralOrder(
            productId = product.id,
            quantity = 1,
            customerName = "Karan Verma",
            customerPhone = "9123456789",
            deliveryAddress = "42 MG Road, Indiranagar",
            city = "Bengaluru",
            pincode = "560038",
            paymentMethod = "UPI",
            referralCodeInput = "CW-AARAV-1042",
            currentLoggedInUserId = "usr_sneha_03"
        )
        assertTrue(orderResult.isSuccess)
        val outcome = orderResult.getOrThrow()
        assertNotNull(outcome.commissionTracked)
        assertEquals("usr_aarav_02", outcome.commissionTracked?.affiliateUserId)
        assertEquals(CommissionStatus.PENDING_DELIVERY, outcome.commissionTracked?.status)

        val updatedProduct = repository.allProductsFlow.first().first { it.id == product.id }
        assertEquals(initialStock - 1, updatedProduct.stock)

        // Duplicate replay check (same product + phone + referral within 1-minute bucket)
        val duplicateResult = repository.placeReferralOrder(
            productId = product.id,
            quantity = 1,
            customerName = "Karan Verma",
            customerPhone = "9123456789",
            deliveryAddress = "42 MG Road, Indiranagar",
            city = "Bengaluru",
            pincode = "560038",
            paymentMethod = "UPI",
            referralCodeInput = "CW-AARAV-1042",
            currentLoggedInUserId = "usr_sneha_03"
        )
        assertTrue(duplicateResult.isFailure)

        // Self-referral check: logged in as Aarav ordering with Aarav's own referral code
        val selfRefResult = repository.placeReferralOrder(
            productId = product.id,
            quantity = 1,
            customerName = "Aarav Test",
            customerPhone = "9876501111",
            deliveryAddress = "10 Park Street",
            city = "Mumbai",
            pincode = "400001",
            paymentMethod = "UPI",
            referralCodeInput = "CW-AARAV-1042",
            currentLoggedInUserId = "usr_aarav_02"
        )
        assertTrue(selfRefResult.isSuccess)
        val selfOutcome = selfRefResult.getOrThrow()
        assertNull(selfOutcome.commissionTracked)
        assertNotNull(selfOutcome.antiFraudNote)
    }

    @Test
    fun `admin order delivery, commission approval, task reward, and upi withdrawal flow`() = runBlocking {
        // 1. Admin creates a 0-day return product and places a referral order
        val prodRes = repository.saveProduct(
            existingId = null,
            name = "Fast Digital Course",
            description = "Instant delivery product",
            category = "Digital",
            priceInr = 1000.0,
            stock = 20,
            commissionType = CommissionType.FIXED,
            commissionValue = 250.0,
            returnWindowDays = 0,
            imageUri = "res://img_prod_earbuds"
        )
        val prod = prodRes.getOrThrow()

        val ordRes = repository.placeReferralOrder(
            productId = prod.id,
            quantity = 1,
            customerName = "Meera Nair",
            customerPhone = "9988776655",
            deliveryAddress = "14 Banjara Hills",
            city = "Hyderabad",
            pincode = "500034",
            paymentMethod = "UPI",
            referralCodeInput = "CW-SNEHA-2088",
            currentLoggedInUserId = null
        ).getOrThrow()

        val commId = ordRes.commissionTracked!!.id

        // Mark order delivered -> unlocks commission to PAYABLE (since manual approval = true)
        repository.updateOrderStatus(ordRes.order.id, OrderStatus.DELIVERED).getOrThrow()
        val commAfterDelivery = repository.allCommissionsFlow.first().first { it.id == commId }
        assertEquals(CommissionStatus.PAYABLE, commAfterDelivery.status)

        // Admin approves commission -> APPROVED_PAID
        repository.approveCommission(commId).getOrThrow()

        // 2. Admin creates a task & Sneha submits proof & Admin approves
        val task = repository.saveTask(
            existingId = null,
            title = "Share Product Reel",
            description = "Post a 15s product demo reel",
            instructions = "Tag @CommissionWala",
            category = TaskCategory.SOCIAL_SHARE,
            rewardInr = 150.0,
            deadlineDaysFromNow = 5,
            imageUri = ""
        ).getOrThrow()

        val sub = repository.submitTaskProof(
            taskId = task.id,
            userId = "usr_sneha_03",
            proofImageUri = null,
            proofNotes = "Posted reel and tagged official handle",
            proofLink = "https://instagram.com/p/demo"
        ).getOrThrow()
        repository.reviewTaskSubmission(sub.id, approve = true, adminNote = "Verified").getOrThrow()

        // Verify Sneha's wallet reflects ₹250 commission + ₹150 task reward minus ₹250 seeded held withdrawal = ₹150 available
        val walletBeforeWd = repository.computeWalletSummary(
            userId = "usr_sneha_03",
            commissions = repository.allCommissionsFlow.first(),
            submissions = repository.allSubmissionsFlow.first(),
            withdrawals = repository.allWithdrawalsFlow.first()
        )
        assertEquals(250.0, walletBeforeWd.heldWithdrawalInr, 0.01)
        assertEquals(150.0, walletBeforeWd.availableBalanceInr, 0.01)

        // 3. Sneha updates UPI ID; over-balance withdrawal is blocked, valid ₹150 withdrawal succeeds
        assertTrue(Formatters.isValidUpiId("sneha@okicici"))
        repository.updateUserUpi("usr_sneha_03", "sneha@okicici", "Sneha Kulkarni").getOrThrow()

        val overLimitWd = repository.requestWithdrawal("usr_sneha_03", 300.0, walletBeforeWd.availableBalanceInr)
        assertTrue(overLimitWd.isFailure)

        val wd = repository.requestWithdrawal("usr_sneha_03", 150.0, walletBeforeWd.availableBalanceInr).getOrThrow()
        assertEquals(WithdrawalStatus.PENDING, wd.status)

        repository.processWithdrawal(wd.id, approve = true, utrReference = "UTR99887766", adminNote = "Paid").getOrThrow()
        val updatedWd = repository.allWithdrawalsFlow.first().first { it.id == wd.id }
        assertEquals(WithdrawalStatus.APPROVED, updatedWd.status)
        assertEquals("UTR99887766", updatedWd.utrReference)
    }

    @Test
    fun `qr card image download and csv statement export succeed cleanly`() = runBlocking {
        val products = repository.allProductsFlow.first()
        val product = products.first()
        val users = repository.allUsersFlow.first()
        val aarav = users.first { it.id == "usr_aarav_02" }

        val payload = QrCodeUtils.buildProductQrPayload(product.skuCode, aarav.referralCode)
        val qrCard = QrCodeUtils.createBrandedQrShareBitmap(
            productName = product.name,
            priceText = Formatters.formatInr(product.priceInr),
            commissionLabel = product.commissionBadgeText(),
            skuCode = product.skuCode,
            referralCode = aarav.referralCode,
            payload = payload
        )
        val savedQr = QrCodeUtils.saveQrToGallery(context, qrCard, "CW_${product.skuCode}")
        assertTrue(savedQr.isSuccess)
        assertTrue(savedQr.getOrThrow().endsWith(".png"))

        val comms = repository.allCommissionsFlow.first()
        val subs = repository.allSubmissionsFlow.first()
        val wds = repository.allWithdrawalsFlow.first()
        val orders = repository.allOrdersFlow.first()

        val wallet = repository.computeWalletSummary(aarav.id, comms, subs, wds)
        val statementCsv = ExportUtils.exportAffiliateStatementCsv(
            context = context,
            user = aarav,
            wallet = wallet,
            commissions = comms.filter { it.affiliateUserId == aarav.id },
            withdrawals = wds.filter { it.userId == aarav.id }
        )
        assertTrue(statementCsv.isSuccess)
        assertTrue(statementCsv.getOrThrow().endsWith(".csv"))

        val adminSummary = repository.computeAdminSummary(users, orders, comms, subs, wds)
        val adminCsv = ExportUtils.exportAdminReportCsv(
            context = context,
            summary = adminSummary,
            orders = orders,
            commissions = comms,
            withdrawals = wds
        )
        assertTrue(adminCsv.isSuccess)
        assertTrue(adminCsv.getOrThrow().endsWith(".csv"))
    }
}
