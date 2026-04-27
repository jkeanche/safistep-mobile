package com.safistep.ui.screens.history

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safistep.data.local.entity.BlockHistoryEntity
import com.safistep.data.repository.ReportRepository
import com.safistep.ui.components.*
import com.safistep.ui.screens.home.BlockEventCard
import com.safistep.ui.theme.SafiColors
import com.safistep.utils.formatCurrency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(reportRepo: ReportRepository) : ViewModel() {
    val history      = reportRepo.recentHistory
    val blockedCount = reportRepo.blockedCount
    val totalSaved   = reportRepo.totalSaved
}

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val history      by viewModel.history.collectAsState(emptyList())
    val blockedCount by viewModel.blockedCount.collectAsState(0)
    val totalSaved   by viewModel.totalSaved.collectAsState(null)

    SafiScaffold {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            SafiTopBar(title = "Block History", onBack = onBack)

            // Summary stats
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label    = "Total Blocked",
                    value    = blockedCount.toString(),
                    color    = SafiColors.Danger,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label    = "Amount Saved",
                    value    = if ((totalSaved ?: 0.0) > 0) formatCurrency(totalSaved) else "—",
                    color    = SafiColors.Success,
                    modifier = Modifier.weight(1f)
                )
            }

            if (history.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon        = Icons.Outlined.CheckCircle,
                        title       = "No Activity Yet",
                        description = "SafiStep will show intercepted payment attempts here."
                    )
                }
            } else {
                LazyColumn(
                    contentPadding        = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    items(history, key = { it.id }) { event ->
                        BlockEventCard(event = event)
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(SafiColors.Card, RoundedCornerShape(14.dp))
            .border(1.dp, SafiColors.CardBorder, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall.copy(color = color, fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = SafiColors.OnSurfaceVar))
    }
}
