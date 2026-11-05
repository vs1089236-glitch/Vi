package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun observeUserById(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE UPPER(referralCode) = UPPER(:code) AND isActive = 1 LIMIT 1")
    suspend fun getActiveUserByReferralCode(code: String): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY createdAt DESC")
    fun observeAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE UPPER(skuCode) = UPPER(:skuCode) LIMIT 1")
    suspend fun getProductBySku(skuCode: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :productId")
    suspend fun deleteProductById(productId: String)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun observeAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: String): OrderEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM orders WHERE claimSignature = :signature)")
    suspend fun existsByClaimSignature(signature: String): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOrder(order: OrderEntity)

    @Update
    suspend fun updateOrder(order: OrderEntity)
}

@Dao
interface CommissionDao {
    @Query("SELECT * FROM commissions ORDER BY createdAt DESC")
    fun observeAllCommissions(): Flow<List<CommissionEntity>>

    @Query("SELECT * FROM commissions WHERE orderId = :orderId LIMIT 1")
    suspend fun getCommissionByOrderId(orderId: String): CommissionEntity?

    @Query("SELECT * FROM commissions WHERE id = :id LIMIT 1")
    suspend fun getCommissionById(id: String): CommissionEntity?

    @Query("SELECT * FROM commissions WHERE status = 'IN_RETURN_WINDOW'")
    suspend fun getCommissionsInReturnWindow(): List<CommissionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCommission(commission: CommissionEntity)

    @Update
    suspend fun updateCommission(commission: CommissionEntity)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)
}

@Dao
interface TaskSubmissionDao {
    @Query("SELECT * FROM task_submissions ORDER BY submittedAt DESC")
    fun observeAllSubmissions(): Flow<List<TaskSubmissionEntity>>

    @Query("SELECT * FROM task_submissions WHERE id = :id LIMIT 1")
    suspend fun getSubmissionById(id: String): TaskSubmissionEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM task_submissions WHERE taskId = :taskId AND userId = :userId AND status != 'REJECTED')")
    suspend fun hasActiveOrApprovedSubmission(taskId: String, userId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmission(submission: TaskSubmissionEntity)

    @Update
    suspend fun updateSubmission(submission: TaskSubmissionEntity)
}

@Dao
interface WithdrawalDao {
    @Query("SELECT * FROM withdrawals ORDER BY requestedAt DESC")
    fun observeAllWithdrawals(): Flow<List<WithdrawalEntity>>

    @Query("SELECT * FROM withdrawals WHERE id = :id LIMIT 1")
    suspend fun getWithdrawalById(id: String): WithdrawalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithdrawal(withdrawal: WithdrawalEntity)

    @Update
    suspend fun updateWithdrawal(withdrawal: WithdrawalEntity)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun observeAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE notifications SET isRead = 1 WHERE recipientUserId = :recipientId OR recipientUserId = 'ALL'")
    suspend fun markAllAsReadForUser(recipientId: String)
}

@Dao
interface AppConfigDao {
    @Query("SELECT * FROM app_config WHERE id = 1")
    fun observeConfig(): Flow<AppConfigEntity?>

    @Query("SELECT * FROM app_config WHERE id = 1")
    suspend fun getConfig(): AppConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConfig(config: AppConfigEntity)
}
