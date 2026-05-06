package com.videonatics.smartcapture.feature.analysis.presenation.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LightbulbCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.videonatics.smartcapture.feature.analysis.domain.model.AnalysisAttempt
import com.videonatics.smartcapture.feature.analysis.domain.model.AnalysisCheck
import com.videonatics.smartcapture.feature.analysis.domain.model.AnalysisValidatedCheck
import com.videonatics.smartcapture.feature.analysis.domain.model.OcrCandidate
import com.videonatics.smartcapture.feature.analysis.presenation.viewmodel.AnalysisUiState
import com.videonatics.smartcapture.feature.analysis.presenation.viewmodel.AnalysisViewModel
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme

private val terminalPassedStatuses = setOf("PASSED", "OVERRIDE_APPROVED", "EXEMPTED")
private val frozenStatuses = setOf("OVERRIDE_APPROVED", "EXEMPTED")
private val resultOptions = listOf("PASS", "FAIL", "WARN")

// ── Pill/chip style helpers ───────────────────────────────────────────────────

private data class PillStyle(val label: String, val fg: Color, val bg: Color)

private fun resultPill(result: String): PillStyle = when (result.uppercase()) {
    "PASS", "PASSED" -> PillStyle("PASS", Color(0xFF2E7D32), Color(0xFFE8F5E9))
    "FAIL", "FAILED" -> PillStyle("FAIL", Color(0xFFC62828), Color(0xFFFFEBEE))
    "WARN", "WARNING" -> PillStyle("WARN", Color(0xFFE65100), Color(0xFFFFF3E0))
    else -> PillStyle(result.uppercase(), Color(0xFF1565C0), Color(0xFFE3F2FD))
}

private fun stagePill(status: String): PillStyle = when (status.uppercase()) {
    "PASSED" -> PillStyle("PASSED", Color(0xFF2E7D32), Color(0xFFE8F5E9))
    "FAILED" -> PillStyle("FAILED", Color(0xFFC62828), Color(0xFFFFEBEE))
    "IN_PROGRESS" -> PillStyle("IN PROGRESS", Color(0xFF1565C0), Color(0xFFE3F2FD))
    "OVERRIDE_APPROVED" -> PillStyle("OVERRIDE APPROVED", Color(0xFF00695C), Color(0xFFE0F2F1))
    "OVERRIDE_PENDING" -> PillStyle("OVERRIDE PENDING", Color(0xFFE65100), Color(0xFFFFF3E0))
    "OVERRIDE_REJECTED" -> PillStyle("OVERRIDE REJECTED", Color(0xFFC62828), Color(0xFFFFEBEE))
    "EXEMPTED" -> PillStyle("EXEMPTED", Color(0xFF6A1B9A), Color(0xFFF3E5F5))
    "ESCALATED" -> PillStyle("ESCALATED", Color(0xFFBF360C), Color(0xFFFBE9E7))
    else -> PillStyle(status.uppercase().replace("_", " "), Color(0xFF546E7A), Color(0xFFECEFF1))
}

private fun taskPill(status: String): PillStyle = when (status.uppercase()) {
    "COMPLETED", "APPROVED" -> PillStyle("COMPLETED", Color(0xFF2E7D32), Color(0xFFE8F5E9))
    "IN_PROGRESS" -> PillStyle("IN PROGRESS", Color(0xFF1565C0), Color(0xFFE3F2FD))
    "FAILED", "REJECTED" -> PillStyle("FAILED", Color(0xFFC62828), Color(0xFFFFEBEE))
    else -> PillStyle(status.uppercase().replace("_", " "), Color(0xFF546E7A), Color(0xFFECEFF1))
}

private fun String.toDisplayName(): String =
    split("_").joinToString(" ") { it.replaceFirstChar { c -> c.titlecase() } }

private fun formatTimestamp(iso: String): String = try {
    val months = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val date = iso.substringBefore("T")
    val time = iso.substringAfter("T").substringBefore(".")
    val (y, m, d) = date.split("-")
    val (hh, mm) = time.split(":")
    "$d ${months[m.toInt()]} $y, $hh:$mm"
} catch (_: Exception) { iso }

// ── Data chips builder ────────────────────────────────────────────────────────

private fun buildDataChips(checkName: String, data: Map<String, String>): List<Pair<String, String>> {
    if (data.isEmpty()) return emptyList()
    return when (checkName) {
        "box_status" -> listOfNotNull(
            data["observed"]?.let { "observed" to it },
            data["expected"]?.let { "expected" to it }
        )
        "fat_ne_id_ocr" -> listOfNotNull(
            data["primaryNumber"]?.takeIf { it.isNotEmpty() }?.let { "primary" to it }
        )
        "variant_detect" -> listOfNotNull(
            data["variant"]?.let { "variant" to it },
            data["basis"]?.let { "basis" to it }
        )
        "damage_assess" -> listOfNotNull(
            data["damageType"]?.let { "type" to it },
            data["severity"]?.let { "severity" to it }
        )
        "wiring_compliance" -> listOfNotNull(
            data["scope"]?.let { "scope" to it },
            data["violations"]?.takeIf { it.isNotEmpty() }?.let { "violations" to it }
        )
        "dsl_tag_check" -> buildList {
            val tags = data["tags"]; val expected = data["expectedTags"]
            if (tags != null) add("tags" to "${tags}${if (expected != null) " / $expected" else ""}")
            data["attachedPorts"]?.let { add("attached" to it) }
        }
        "port_count" -> listOfNotNull(
            data["totalPorts"]?.let { "total" to it },
            data["attachedPorts"]?.let { "attached" to it },
            data["freePorts"]?.let { "free" to it },
            data["powerPorts"]?.let { "power" to it }
        )
        "cleanliness_score" -> listOfNotNull(
            data["dirtLevel"]?.let { "dirt level" to it }
        )
        else -> emptyList()
    }
}

// ── Page entry ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisPage(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onRetake: () -> Unit = {},
    vm: AnalysisViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var editingCheck by remember { mutableStateOf<AnalysisCheck?>(null) }

    LaunchedEffect(Unit) {
        vm.events.collect { snackbarHostState.showSnackbar(it) }
    }

    editingCheck?.let { check ->
        CheckEditDialog(
            check = check,
            isSubmitting = state.isSubmitting,
            onDismiss = { editingCheck = null },
            onSubmit = { result, note ->
                vm.editCheckpoint(check.id, result, note.ifBlank { null })
                editingCheck = null
            },
            onRevert = {
                vm.revertCheckpoint(check.id)
                editingCheck = null
            }
        )
    }

    AnalysisScaffold(
        modifier = modifier,
        state = state,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onRefresh = { vm.refresh() },
        onRetake = onRetake,
        onEditCheck = { editingCheck = it },
        onOverrideRequest = { vm.showOverrideDialog() },
        onOverrideDismiss = { vm.dismissOverrideDialog() },
        onOverrideConfirm = { vm.confirmOverride() },
        onOverrideReasonChange = { vm.setOverrideReason(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalysisScaffold(
    modifier: Modifier = Modifier,
    state: AnalysisUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit,
    onRetake: () -> Unit,
    onEditCheck: (AnalysisCheck) -> Unit,
    onOverrideRequest: () -> Unit,
    onOverrideDismiss: () -> Unit,
    onOverrideConfirm: () -> Unit,
    onOverrideReasonChange: (String) -> Unit
) {
    if (state.showOverrideDialog) {
        OverrideDialog(
            reason = state.overrideReason,
            isSubmitting = state.isSubmitting,
            onReasonChange = onOverrideReasonChange,
            onDismiss = onOverrideDismiss,
            onConfirm = onOverrideConfirm
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("Stage Result", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { TextButton(onClick = onRefresh) { Text("Refresh") } }
            )
        }
    ) { paddingValues ->
        when {
            state.isLoading && state.stageType.isEmpty() && !state.hasAnalysis -> {
                Box(Modifier.fillMaxSize().padding(paddingValues), Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null && !state.hasAnalysis -> {
                Box(Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), Alignment.Center) {
                    Text("Error: ${state.error}", color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
            else -> {
                AnalysisContent(
                    state = state,
                    paddingValues = paddingValues,
                    onEditCheck = onEditCheck,
                    onRetake = onRetake,
                    onOverrideRequest = onOverrideRequest
                )
            }
        }
    }
}

@Composable
private fun AnalysisContent(
    state: AnalysisUiState,
    paddingValues: PaddingValues,
    onEditCheck: (AnalysisCheck) -> Unit,
    onRetake: () -> Unit,
    onOverrideRequest: () -> Unit
) {
    val stageStatusUpper = state.stageStatus.uppercase()
    val isTerminalPassed = stageStatusUpper in terminalPassedStatuses
    val isFrozen = stageStatusUpper in frozenStatuses
    val hasIssue = state.checks.any { it.effectiveResult.uppercase() !in setOf("PASS", "PASSED") }
    val showActions = !isTerminalPassed && (if (state.hasAnalysis) hasIssue else true)
    val atCap = state.attemptCount >= 5

    Column(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status pills
        if (state.stageStatus.isNotEmpty() || state.taskStatus.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (state.stageStatus.isNotEmpty()) {
                    val p = stagePill(state.stageStatus)
                    AnalysisStatusPill(p.label, p.fg, p.bg)
                }
                if (state.taskStatus.isNotEmpty()) {
                    val p = taskPill(state.taskStatus)
                    AnalysisStatusPill(p.label, p.fg, p.bg)
                }
            }
        }

        // Stage type heading
        if (state.stageType.isNotEmpty()) {
            Text(
                text = state.stageType.toDisplayName(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Waiting spinner
        if (!state.hasAnalysis) { WaitingRow() }

        // Analysis checks
        if (state.hasAnalysis && state.checks.isNotEmpty()) {
            ChecksSectionCard(
                title = "ANALYSIS",
                checks = state.checks,
                isFrozen = isFrozen,
                onEditCheck = onEditCheck
            )
        }

        // Attempt history
        if (state.attempts.isNotEmpty()) {
            AttemptHistorySection(attempts = state.attempts)
        }

        // Success banner
        if (isTerminalPassed) { SuccessBanner() }

        // Action surface
        if (showActions) {
            Spacer(Modifier.height(4.dp))
            if (atCap) { CapReachedBanner(); Spacer(Modifier.height(8.dp)) }
            if (!atCap) {
                FullWidthButton(onRetake, !state.isSubmitting, Icons.Default.CameraAlt, "Retake Image", filled = true)
                Spacer(Modifier.height(8.dp))
            }
            FullWidthButton(onOverrideRequest, !state.isSubmitting, Icons.Outlined.Flag, "Request Override", filled = atCap)
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Checks section ────────────────────────────────────────────────────────────

@Composable
private fun ChecksSectionCard(
    title: String,
    checks: List<AnalysisCheck>,
    isFrozen: Boolean,
    onEditCheck: (AnalysisCheck) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionLabel(title)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                checks.forEachIndexed { index, check ->
                    CheckRow(check = check, isFrozen = isFrozen, onEditClick = { onEditCheck(check) })
                    if (index < checks.size - 1) {
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckRow(check: AnalysisCheck, isFrozen: Boolean, onEditClick: () -> Unit) {
    val effective = check.effectiveResult
    val isPass = effective.uppercase() in setOf("PASS", "PASSED")
    val pill = resultPill(effective)
    val dataChips = buildDataChips(check.checkName, check.effectiveData)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {

        // ── Name + badges + edit icon + result pill ──
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = check.checkName.toDisplayName(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            // gateOverridden badge
            if (check.gateOverridden) {
                MetaChip("gate override", fg = Color(0xFFB71C1C), bg = Color(0xFFFFEBEE))
                Spacer(Modifier.width(4.dp))
            }
            // edit icon (hidden when frozen or no id)
            if (check.id.isNotEmpty() && !isFrozen) {
                IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(16.dp),
                        tint = if (check.isEdited) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(2.dp))
            }
            ResultPill(pill.label, pill.fg, pill.bg)
        }

        Spacer(Modifier.height(6.dp))

        // ── Confidence + non-fixable + carryForward chips ──
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MetaChip("Confidence ${(check.confidence * 100).toInt()}%")
            if (!check.fixable) MetaChip("non-fixable", fg = MaterialTheme.colorScheme.primary, bg = MaterialTheme.colorScheme.primaryContainer)
            if (check.carryForward) MetaChip("✓ validated", fg = Color(0xFF2E7D32), bg = Color(0xFFE8F5E9))
        }

        // ── Edited indicator: "System: FAIL · note" ──
        if (check.isEdited) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("System:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF78909C))
                MetaChip(check.result.uppercase())
                if (!check.editNote.isNullOrEmpty()) {
                    Text(
                        "· ${check.editNote}", style = MaterialTheme.typography.labelSmall,
                        fontStyle = FontStyle.Italic, color = Color(0xFF78909C), maxLines = 2,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }

        // ── Reason text ──
        if (check.reason.isNotEmpty() && !(check.isEdited && isPass)) {
            Spacer(Modifier.height(6.dp))
            Text(check.reason, style = MaterialTheme.typography.bodySmall, color = Color(0xFF546E7A), lineHeight = 17.sp)
        }

        // ── Corrective action ──
        if (!isPass && !check.correctiveAction.isNullOrEmpty()) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.LightbulbCircle, contentDescription = null,
                    modifier = Modifier.size(14.dp).padding(top = 1.dp), tint = Color(0xFFE65100))
                Text(check.correctiveAction, style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic, color = Color(0xFFE65100), lineHeight = 17.sp)
            }
        }

        // ── Per-check data chips ──
        if (dataChips.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                dataChips.forEach { (label, value) ->
                    DataValueChip(label = label, value = value)
                }
            }
        }

        // ── OCR candidates expander (fat_ne_id_ocr only, ≥2 candidates) ──
        if (check.ocrCandidates.size >= 2) {
            Spacer(Modifier.height(4.dp))
            OcrMatchesExpander(candidates = check.ocrCandidates)
        }
    }
}

// ── OCR matches expander ──────────────────────────────────────────────────────

@Composable
private fun OcrMatchesExpander(candidates: List<OcrCandidate>) {
    var open by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .clickable { open = !open }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (open) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF78909C)
            )
            Text("${candidates.size} OCR matches",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF78909C)
            )
        }
        if (open) {
            Column(modifier = Modifier.padding(start = 18.dp, top = 2.dp)) {
                candidates.forEach { m ->
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Color(0xFF37474F))) {
                                append(m.text)
                            }
                            withStyle(SpanStyle(color = Color(0xFF78909C))) {
                                append("  ·  ${(m.confidence * 100).toInt()}%")
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}

// ── Check edit dialog ─────────────────────────────────────────────────────────

@Composable
private fun CheckEditDialog(
    check: AnalysisCheck,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (result: String, note: String) -> Unit,
    onRevert: () -> Unit
) {
    var selectedResult by remember { mutableStateOf(check.effectiveResult.uppercase()) }
    var note by remember { mutableStateOf(check.editNote.orEmpty()) }
    var noteError by remember { mutableStateOf<String?>(null) }

    val isFlippingVerdict = selectedResult != check.result.uppercase()

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text(check.checkName.toDisplayName(), fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // "System said: FAIL"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("System said:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val p = resultPill(check.result)
                    ResultPill(p.label, p.fg, p.bg)
                }

                // Reason (if any)
                if (check.reason.isNotEmpty()) {
                    Text(check.reason, style = MaterialTheme.typography.bodySmall, color = Color(0xFF546E7A))
                }

                HorizontalDivider()

                // Operator verdict header
                Text("OPERATOR VERDICT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                // Radio group: Keep system + PASS / WARN / FAIL
                Column(modifier = Modifier.selectableGroup()) {
                    // "Keep system value" option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedResult == check.result.uppercase(),
                                onClick = { selectedResult = check.result.uppercase() },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(selected = selectedResult == check.result.uppercase(), onClick = null)
                        Text("Keep system value (${check.result.uppercase()})",
                            style = MaterialTheme.typography.bodySmall)
                    }

                    resultOptions.forEach { option ->
                        val p = resultPill(option)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedResult == option &&
                                            option != check.result.uppercase(),
                                    onClick = { selectedResult = option },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = selectedResult == option && option != check.result.uppercase(),
                                onClick = null
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(p.bg)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(option, color = p.fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Note field — required (≥10 chars) when flipping verdict
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it; if (noteError != null) noteError = null },
                    label = { Text(if (isFlippingVerdict) "Note (required, ≥10 chars)" else "Note (optional)") },
                    placeholder = { Text("Reason for override…") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2, maxLines = 3,
                    isError = noteError != null,
                    supportingText = noteError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                )

                // Revert option
                if (check.isEdited) {
                    TextButton(
                        onClick = { if (!isSubmitting) onRevert() },
                        enabled = !isSubmitting
                    ) {
                        Text("Revert to system result",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isFlippingVerdict && note.trim().length < 10) {
                        noteError = "Note must be at least 10 characters when changing the verdict."
                        return@TextButton
                    }
                    onSubmit(selectedResult, note)
                },
                enabled = !isSubmitting
            ) {
                if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text("Cancel") }
        }
    )
}

// ── Attempt history ───────────────────────────────────────────────────────────

@Composable
private fun AttemptHistorySection(attempts: List<AnalysisAttempt>) {
    val sorted = attempts.sortedByDescending { it.attemptNo }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF78909C))
            SectionLabel("ATTEMPT HISTORY · ${attempts.size}/5")
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                sorted.forEachIndexed { index, attempt ->
                    AttemptRow(attempt = attempt, isLatest = index == 0)
                    if (index < sorted.size - 1) {
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun AttemptRow(attempt: AnalysisAttempt, isLatest: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Attempt ${attempt.attemptNo}",
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface)
                if (isLatest) MetaChip("latest", fg = MaterialTheme.colorScheme.primary, bg = MaterialTheme.colorScheme.primaryContainer)
            }
            if (attempt.createdAt.isNotEmpty()) {
                Text(formatTimestamp(attempt.createdAt),
                    style = MaterialTheme.typography.bodySmall, color = Color(0xFF78909C))
            }
        }
        when (attempt.guardrailPassed) {
            true  -> { val p = resultPill("PASS"); ResultPill("GATE PASS", p.fg, p.bg) }
            false -> { val p = resultPill("FAIL"); ResultPill("GATE FAIL", p.fg, p.bg) }
            null  -> Unit
        }
    }
}

// ── Banners ───────────────────────────────────────────────────────────────────

@Composable
private fun SuccessBanner() {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFE8F5E9)).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("✓", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("Stage passed successfully",
            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1B5E20))
    }
}

@Composable
private fun CapReachedBanner() {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFFFF3E0)).padding(14.dp)
    ) {
        Text("Attempt limit reached",
            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
            color = Color(0xFFBF360C))
        Spacer(Modifier.height(4.dp))
        Text("You have used all 5 attempts. Please submit an override request for supervisor review.",
            style = MaterialTheme.typography.bodySmall, color = Color(0xFF424242))
    }
}

@Composable
private fun WaitingRow() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Text("Waiting for analysis…", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF78909C))
    }
}

// ── Override dialog ───────────────────────────────────────────────────────────

@Composable
private fun OverrideDialog(
    reason: String,
    isSubmitting: Boolean,
    onReasonChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text("Request Override Review", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Submit an override request? A supervisor will review this stage.",
                    style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = reason, onValueChange = onReasonChange,
                    label = { Text("Reason") },
                    placeholder = { Text("Describe why you need an override…") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isSubmitting) {
                if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Submit", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text("Cancel") }
        }
    )
}

// ── Shared button ─────────────────────────────────────────────────────────────

@Composable
private fun FullWidthButton(
    onClick: () -> Unit, enabled: Boolean, icon: ImageVector, label: String, filled: Boolean
) {
    if (filled) {
        Button(onClick = onClick, enabled = enabled,
            modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(8.dp)) {
            Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.SemiBold)
        }
    } else {
        OutlinedButton(onClick = onClick, enabled = enabled,
            modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(8.dp)) {
            Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Atomic widgets ────────────────────────────────────────────────────────────

@Composable
fun AnalysisStatusPill(label: String, fg: Color, bg: Color) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) { Text(label, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp) }
}

@Composable
private fun ResultPill(label: String, fg: Color, bg: Color) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) { Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun MetaChip(label: String, fg: Color = Color(0xFF546E7A), bg: Color = Color(0xFFECEFF1)) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) { Text(label, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Medium) }
}

@Composable
private fun DataValueChip(label: String, value: String) {
    Text(
        buildAnnotatedString {
            if (label.isNotEmpty()) {
                withStyle(SpanStyle(color = Color(0xFF78909C))) { append("$label: ") }
            }
            withStyle(SpanStyle(color = Color(0xFF37474F), fontWeight = FontWeight.SemiBold)) {
                append(value)
            }
        },
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFECEFF1))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp, color = Color(0xFF78909C))
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFFF5F7FA)
@Composable
private fun AnalysisPagePreview() {
    SmartCaptureTheme {
        AnalysisScaffold(
            state = AnalysisUiState(
                isLoading = false,
                taskStatus = "IN_PROGRESS",
                stageType = "CLOSED_PRE",
                stageStatus = "FAILED",
                attempts = listOf(
                    AnalysisAttempt("a1", 1, "2024-01-15T08:12:00.000Z", guardrailPassed = true),
                    AnalysisAttempt("a2", 2, "2024-01-15T09:44:00.000Z", guardrailPassed = false),
                    AnalysisAttempt("a3", 3, "2024-01-15T10:30:00.000Z", guardrailPassed = true)
                ),
                hasAnalysis = true,
                latestAnalysisPassed = false,
                checks = listOf(
                    AnalysisCheck(
                        id = "1", checkName = "box_status", result = "PASS", confidence = 0.97,
                        reason = "Box detected in closed state.",
                        data = mapOf("observed" to "covered", "expected" to "covered")
                    ),
                    AnalysisCheck(
                        id = "2", checkName = "fat_ne_id_ocr", result = "FAIL", confidence = 0.62,
                        reason = "OCR could not read the FAT NE ID label clearly.",
                        correctiveAction = "Ensure the label is fully visible and not obstructed.",
                        data = mapOf("primaryNumber" to "64G-56-96-12-46"),
                        ocrCandidates = listOf(
                            OcrCandidate("64G-56-96-12-46", 0.88),
                            OcrCandidate("64G-56-96-12-4B", 0.62),
                            OcrCandidate("64G-56-96-12-46", 0.55)
                        )
                    ),
                    AnalysisCheck(
                        id = "3", checkName = "port_count", result = "PASS", confidence = 0.95,
                        reason = "Port count matches expected configuration.",
                        data = mapOf("totalPorts" to "24", "attachedPorts" to "20",
                            "freePorts" to "3", "powerPorts" to "1")
                    ),
                    AnalysisCheck(
                        id = "4", checkName = "wiring", result = "PASS",
                        editedResult = "FAIL", editNote = "Operator observed crossing wires",
                        confidence = 0.99, reason = "All wiring connections are properly secured.",
                        carryForward = true
                    )
                ),
                validatedChecks = listOf(
                    AnalysisValidatedCheck("v1", "variant_detect", "PASS", 0.95, "system")
                )
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onBackClick = {}, onRefresh = {}, onRetake = {}, onEditCheck = {},
            onOverrideRequest = {}, onOverrideDismiss = {}, onOverrideConfirm = {},
            onOverrideReasonChange = {}
        )
    }
}