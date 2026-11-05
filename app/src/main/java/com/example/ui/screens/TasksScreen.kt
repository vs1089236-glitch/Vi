package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.SubmissionStatus
import com.example.data.local.TaskEntity
import com.example.data.local.TaskSubmissionEntity
import com.example.ui.components.BadgeTone
import com.example.ui.components.ProductImageView
import com.example.ui.components.StatusPillBadge
import com.example.ui.theme.AppOutlineLight
import com.example.ui.theme.BrandBlueContainer
import com.example.ui.theme.BrandBlueSecondary
import com.example.ui.theme.BrandPurpleContainer
import com.example.ui.theme.BrandPurplePrimary
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextMutedLight
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight
import com.example.util.Formatters
import com.example.util.MediaStorageHelper
import java.io.File

@Composable
fun TasksScreen(
    tasks: List<TaskEntity>,
    userSubmissions: List<TaskSubmissionEntity>,
    onSubmitProof: (
        taskId: String,
        proofImageUri: String?,
        proofNotes: String,
        proofLink: String,
        onSuccess: () -> Unit
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var activeTaskForProof by remember { mutableStateOf<TaskEntity?>(null) }

    val activeTasks = remember(tasks) { tasks.filter { it.isActive } }

    val filteredTasks = remember(activeTasks, userSubmissions, selectedFilter) {
        activeTasks.filter { task ->
            val sub = userSubmissions.firstOrNull { it.taskId == task.id }
            when (selectedFilter) {
                "Available" -> sub == null || sub.status == SubmissionStatus.REJECTED
                "Pending" -> sub?.status == SubmissionStatus.PENDING
                "Approved" -> sub?.status == SubmissionStatus.APPROVED
                else -> true
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Earn Extra Task Rewards",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimaryLight
                )
                Text(
                    text = "Complete promotional missions, upload proof screenshots, and get instant wallet credits.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryLight
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("All", "Available", "Pending", "Approved")) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) }
                        )
                    }
                }
            }
        }

        if (filteredTasks.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, AppOutlineLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.AssignmentTurnedIn, contentDescription = null, tint = TextMutedLight, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No tasks in this view", style = MaterialTheme.typography.titleMedium, color = TextPrimaryLight)
                        Text("Check other filter tabs or wait for Admin to publish new missions.", style = MaterialTheme.typography.bodySmall, color = TextSecondaryLight)
                    }
                }
            }
        } else {
            items(filteredTasks, key = { it.id }) { task ->
                val submission = userSubmissions.firstOrNull { it.taskId == task.id }
                val isExpired = System.currentTimeMillis() > task.deadlineAt

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, AppOutlineLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_card_${task.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusPillBadge(text = task.category.label, tone = BadgeTone.BLUE)
                                if (submission != null) {
                                    val subTone = when (submission.status) {
                                        SubmissionStatus.APPROVED -> BadgeTone.SUCCESS
                                        SubmissionStatus.PENDING -> BadgeTone.WARNING
                                        SubmissionStatus.REJECTED -> BadgeTone.ERROR
                                    }
                                    StatusPillBadge(text = submission.status.label, tone = subTone)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = SuccessContainer
                            ) {
                                Text(
                                    text = "+${Formatters.formatInr(task.rewardInr)}",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = SuccessEmerald,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProductImageView(
                                imageUri = task.imageUri,
                                contentDescription = task.title,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimaryLight
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = TextMutedLight, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isExpired) "Deadline passed" else Formatters.formatCountdown(task.deadlineAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isExpired) Color.Red else TextMutedLight
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryLight
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8F9FE),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Step-by-step Instructions",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BrandPurplePrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = task.instructions,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimaryLight
                                )
                            }
                        }

                        if (submission != null && submission.adminNote.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Reviewer note: ${submission.adminNote}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (submission.status == SubmissionStatus.APPROVED) SuccessEmerald else BrandBlueSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        when {
                            submission?.status == SubmissionStatus.APPROVED -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessEmerald, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Reward of ${Formatters.formatInr(task.rewardInr)} credited to your Wallet!",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SuccessEmerald
                                    )
                                }
                            }
                            submission?.status == SubmissionStatus.PENDING -> {
                                OutlinedButton(
                                    onClick = {},
                                    enabled = false,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                ) {
                                    Text("Proof Submitted • Pending Admin Review")
                                }
                            }
                            else -> {
                                Button(
                                    onClick = { activeTaskForProof = task },
                                    enabled = !isExpired,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurplePrimary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("submit_task_btn_${task.id}")
                                ) {
                                    Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (submission?.status == SubmissionStatus.REJECTED) {
                                            "Resubmit Proof • Earn ${Formatters.formatInr(task.rewardInr)}"
                                        } else {
                                            "Submit Proof • Earn ${Formatters.formatInr(task.rewardInr)}"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    activeTaskForProof?.let { task ->
        TaskProofSubmissionSheet(
            task = task,
            onDismiss = { activeTaskForProof = null },
            onSubmit = { imagePath, notes, link ->
                onSubmitProof(task.id, imagePath, notes, link) {
                    activeTaskForProof = null
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskProofSubmissionSheet(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onSubmit: (imagePath: String?, notes: String, link: String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var proofImagePath by remember { mutableStateOf<String?>(null) }
    var proofNotes by remember { mutableStateOf("") }
    var proofLink by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            proofImagePath = MediaStorageHelper.persistPickedImage(context, uri, "proof")
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
                    text = "Submit Completion Proof",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimaryLight
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Text(
                text = "${task.title} (+${Formatters.formatInr(task.rewardInr)})",
                style = MaterialTheme.typography.titleSmall,
                color = BrandPurplePrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = {
                    imagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("pick_proof_screenshot_button")
            ) {
                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (proofImagePath == null) "Attach Screenshot Proof" else "Change Screenshot")
            }

            if (proofImagePath != null) {
                Spacer(modifier = Modifier.height(10.dp))
                AsyncImage(
                    model = File(proofImagePath!!),
                    contentDescription = "Selected proof screenshot",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = proofLink,
                onValueChange = { proofLink = it },
                label = { Text("Post / Reel / Status Link (optional)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("proof_link_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = proofNotes,
                onValueChange = { proofNotes = it },
                label = { Text("Completion Notes (e.g. views, group names)") },
                minLines = 3,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("proof_notes_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onSubmit(proofImagePath, proofNotes, proofLink) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlueSecondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("confirm_submit_proof_button")
            ) {
                Text("Submit Proof for Admin Approval")
            }
        }
    }
}
