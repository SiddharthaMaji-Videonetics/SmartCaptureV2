package com.videonatics.smartcapture.feature.analysis.presenation.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.videonatics.smartcapture.feature.analysis.domain.model.AnalysisCheck
import com.videonatics.smartcapture.feature.analysis.domain.repository.AnalysisRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────────────────────────

data class AttemptDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val attemptNo: Int = 0,
    val totalAttempts: Int = 0,
    val createdAt: String = "",
    val imageUrl: String? = null,
    val guardrailPassed: Boolean? = null,
    val checks: List<AnalysisCheck> = emptyList(),
    val stageFrozen: Boolean = false
)

@HiltViewModel
class AttemptDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: AnalysisRepository
) : ViewModel() {

    private val taskId: String = checkNotNull(savedState["taskId"])
    private val stageId: String = checkNotNull(savedState["stageId"])
    private val attemptId: String = checkNotNull(savedState["attemptId"])

    private val _state = MutableStateFlow(AttemptDetailUiState())
    val state: StateFlow<AttemptDetailUiState> = _state.asStateFlow()

    init { load() }

    private fun load() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        repository.getTaskDetail(taskId).fold(
            onSuccess = { detail ->
                val stage = detail.stages.find { it.id == stageId }
                    ?: detail.stages.lastOrNull()
                val attempt = stage?.attempts?.find { it.id == attemptId }
                val analysis = stage?.analysisForAttempt(attemptId)
                val frozenStatuses = setOf("OVERRIDE_APPROVED", "EXEMPTED")
                _state.update {
                    it.copy(
                        isLoading = false,
                        attemptNo = attempt?.attemptNo ?: 0,
                        totalAttempts = stage?.attemptCount ?: 0,
                        createdAt = attempt?.createdAt.orEmpty(),
                        imageUrl = attempt?.imageUrl,
                        guardrailPassed = attempt?.guardrailPassed,
                        checks = analysis?.checks ?: emptyList(),
                        stageFrozen = stage?.status?.uppercase() in frozenStatuses
                    )
                }
            },
            onFailure = { e ->
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        )
    }
}

// ── Page ──────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttemptDetailPage(
    onBackClick: () -> Unit = {},
    vm: AttemptDetailViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val label = if (state.totalAttempts > 0)
                        "Attempt ${state.attemptNo} / ${state.totalAttempts}"
                    else "Attempt Detail"
                    Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding), Alignment.Center
            ) { CircularProgressIndicator() }

            state.error != null -> Box(
                Modifier.fillMaxSize().padding(padding).padding(16.dp), Alignment.Center
            ) { Text("Error: ${state.error}", color = MaterialTheme.colorScheme.error) }

            else -> AttemptDetailContent(state = state, paddingValues = padding)
        }
    }
}

@Composable
private fun AttemptDetailContent(state: AttemptDetailUiState, paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Attempt image ──────────────────────────────────────────────────
        if (state.imageUrl != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
            ) {
                AuthenticatedNetworkImage(
                    url = state.imageUrl,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // ── Gate pill + timestamp ─────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (state.guardrailPassed != null) {
                val (label, fg, bg) = if (state.guardrailPassed)
                    Triple("GATE PASS", Color(0xFF2E7D32), Color(0xFFE8F5E9))
                else
                    Triple("GATE FAIL", Color(0xFFC62828), Color(0xFFFFEBEE))
                Box(
                    Modifier.clip(RoundedCornerShape(4.dp)).background(bg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(label, color = fg, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            if (state.createdAt.isNotEmpty()) {
                Text(formatTimestamp(state.createdAt), style = MaterialTheme.typography.bodySmall, color = Color(0xFF78909C))
            }
        }

        // ── Analysis checks (read-only) ────────────────────────────────────
        if (state.checks.isNotEmpty()) {
            Text("ANALYSIS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF78909C))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    state.checks.forEachIndexed { index, check ->
                        CheckRow(
                            check = check,
                            isFrozen = true,
                            onEditClick = {},
                            onConfirmClick = {}
                        )
                        if (index < state.checks.size - 1) {
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        } else {
            Text(
                "No analysis available for this attempt.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF78909C)
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}
