package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.SubmissionStatus
import com.example.data.local.TaskCategory
import com.example.data.local.TaskEntity
import com.example.data.local.TaskSubmissionEntity
import com.example.ui.components.BadgeTone
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
import java.io.File

@Composable
fun AdminTasksScreen(
    tasks: List<TaskEntity>,
    submissions: List<TaskSubmissionEntity>,
    onSaveTask: (
        existingId: String?,
        title: String,
        description: String,
        instructions: String,
        category: TaskCategory,
        rewardInr: Double,
        deadlineDays: Int,
        imageUri: String,
        onSuccess: () -> Unit
    ) -> Unit,
    onToggleTaskActive: (String, Boolean) -> Unit,
    onDeleteTask: (String) -> Unit,
    onReviewSubmission: (submissionId: String, approve: Boolean, adminNote: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Tasks, 1 = Review Submissions
    var showTaskSheet by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }

    val pendingSubmissionsCount = submissions.count { it.status == SubmissionStatus.PENDING }

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
                        text = "Task & Proof Manager",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimaryLight
                    )
                    Text(
                        text = "Create unlimited reward tasks & approve affiliate completion proofs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryLight
                    )
                }
                Button(
                    onClick = {
                        editingTask = null
                        showTaskSheet = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurplePrimary),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("admin_add_task_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Task")
                }
            }
        }

        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = BrandPurplePrimary,
                modifier = Modifier.clip(RoundedCornerShape(14.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Tasks (${tasks.size})", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.testTag("admin_tasks_tab_list")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Proofs ($pendingSubmissionsCount pending)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.testTag("admin_tasks_tab_proofs")
                )
            }
        }

        if (selectedTab == 0) {
            items(tasks, key = { it.id }) { task ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, AppOutlineLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPillBadge(text = task.category.label, tone = BadgeTone.BLUE)
                            Text(
                                text = "+${Formatters.formatInr(task.rewardInr)}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFeatureSettings = "tnum",
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = SuccessEmerald
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(task.title, style = MaterialTheme.typography.titleMedium, color = TextPrimaryLight)
                        Text(
                            text = "Deadline: ${Formatters.formatCountdown(task.deadlineAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedLight
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Active", style = MaterialTheme.typography.labelMedium, color = TextSecondaryLight)
                                Spacer(modifier = Modifier.width(6.dp))
                                Switch(
                                    checked = task.isActive,
                                    onCheckedChange = { onToggleTaskActive(task.id, it) }
                                )
                            }
                            Row {
                                OutlinedButton(
                                    onClick = {
                                        editingTask = task
                                        showTaskSheet = true
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(42.dp)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit task", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit")
                                }
                                IconButton(onClick = { onDeleteTask(task.id) }) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete task", tint = ErrorCrimson)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (submissions.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, AppOutlineLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No task submissions received yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryLight,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
            } else {
                items(submissions, key = { it.id }) { sub ->
                    val subTone = when (sub.status) {
                        SubmissionStatus.PENDING -> BadgeTone.WARNING
                        SubmissionStatus.APPROVED -> BadgeTone.SUCCESS
                        SubmissionStatus.REJECTED -> BadgeTone.ERROR
                    }
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, AppOutlineLight),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_submission_${sub.id}")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusPillBadge(text = sub.status.label, tone = subTone)
                                Text(
                                    text = "+${Formatters.formatInr(sub.rewardInr)}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFeatureSettings = "tnum",
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = SuccessEmerald
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(sub.taskTitle, style = MaterialTheme.typography.titleMedium, color = TextPrimaryLight)
                            Text(
                                text = "Submitted by ${sub.userName} (${sub.userReferralCode}) • ${Formatters.formatRelativeTime(sub.submittedAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandBlueSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(sub.proofNotes, style = MaterialTheme.typography.bodyMedium, color = TextPrimaryLight)

                            if (sub.proofLink.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Link: ${sub.proofLink}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = BrandPurplePrimary
                                )
                            }

                            if (!sub.proofImageUri.isNullOrBlank()) {
                                val file = File(sub.proofImageUri)
                                if (file.exists()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    AsyncImage(
                                        model = file,
                                        contentDescription = "Submitted task proof screenshot",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                    )
                                }
                            }

                            if (sub.status == SubmissionStatus.PENDING) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            onReviewSubmission(sub.id, true, "Verified proof! Reward credited.")
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp)
                                            .testTag("approve_submission_${sub.id}")
                                    ) {
                                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Approve & Credit")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            onReviewSubmission(sub.id, false, "Screenshot unclear — please resubmit.")
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(0.75f)
                                            .height(46.dp)
                                            .testTag("reject_submission_${sub.id}")
                                    ) {
                                        Text("Reject", color = ErrorCrimson)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTaskSheet) {
        AddEditTaskSheet(
            existing = editingTask,
            onDismiss = { showTaskSheet = false },
            onSave = { title, desc, instr, cat, reward, deadlineDays, imgUri ->
                onSaveTask(editingTask?.id, title, desc, instr, cat, reward, deadlineDays, imgUri) {
                    showTaskSheet = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditTaskSheet(
    existing: TaskEntity?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        instructions: String,
        category: TaskCategory,
        rewardInr: Double,
        deadlineDays: Int,
        imageUri: String
    ) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    var instructions by remember { mutableStateOf(existing?.instructions.orEmpty()) }
    var category by remember { mutableStateOf(existing?.category ?: TaskCategory.SOCIAL_SHARE) }
    var rewardText by remember { mutableStateOf(existing?.rewardInr?.toInt()?.toString() ?: "200") }
    var deadlineDaysText by remember { mutableStateOf("7") }
    var imageUri by remember { mutableStateOf(existing?.imageUri ?: "res://img_hero_affiliate") }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            MediaStorageHelper.persistPickedImage(context, uri, "task")?.let { path ->
                imageUri = path
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
                    text = if (existing == null) "Create Reward Task" else "Edit Task",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimaryLight
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    imagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Task Cover Image")
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task Title") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_task_title_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Task Description") },
                minLines = 2,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_task_desc_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = instructions,
                onValueChange = { instructions = it },
                label = { Text("Step-by-step Instructions") },
                minLines = 3,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_task_instructions_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text("Task Category", style = MaterialTheme.typography.labelLarge, color = TextPrimaryLight)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TaskCategory.entries) { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = rewardText,
                    onValueChange = { rewardText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Reward (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_task_reward_input")
                )
                OutlinedTextField(
                    value = deadlineDaysText,
                    onValueChange = { deadlineDaysText = it.filter { c -> c.isDigit() } },
                    label = { Text("Deadline (Days)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_task_deadline_input")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onSave(
                        title,
                        description,
                        instructions,
                        category,
                        rewardText.toDoubleOrNull() ?: 0.0,
                        deadlineDaysText.toIntOrNull() ?: 7,
                        imageUri
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPurplePrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("admin_save_task_submit")
            ) {
                Text(if (existing == null) "Publish Task & Notify Users" else "Update Task")
            }
        }
    }
}
