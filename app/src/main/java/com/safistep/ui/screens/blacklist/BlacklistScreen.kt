package com.safistep.ui.screens.blacklist

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safistep.data.remote.dto.PlatformDto
import com.safistep.data.repository.BlacklistRepository
import com.safistep.ui.components.*
import com.safistep.ui.theme.SafiColors
import com.safistep.utils.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlacklistViewModel @Inject constructor(
    private val blacklistRepo: BlacklistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BlacklistUiState>(BlacklistUiState.Loading)
    val uiState: StateFlow<BlacklistUiState> = _uiState.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    var searchQuery by mutableStateOf("")
        private set

    init {
        loadBlacklist()
    }

    fun loadBlacklist() {
        viewModelScope.launch {
            _uiState.value = BlacklistUiState.Loading
            when (val result = blacklistRepo.getLocalBlacklist()) {
                is ApiResult.Success -> {
                    _uiState.value = BlacklistUiState.Success(result.data.platforms)
                }
                is ApiResult.Error -> {
                    _uiState.value =
                        BlacklistUiState.Error(result.message ?: "Failed to load blacklist")
                }
                is ApiResult.NetworkError -> {
                    _uiState.value =
                        BlacklistUiState.Error("Network error. Please check your connection.")
                }
            }
        }
    }

    fun syncBlacklist() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            when (val result = blacklistRepo.syncBlacklist()) {
                is ApiResult.Success -> {
                    _syncState.value = SyncState.Success
                    loadBlacklist()
                }
                is ApiResult.Error -> {
                    _syncState.value = SyncState.Error(result.message ?: "Sync failed")
                }
                is ApiResult.NetworkError -> {
                    _syncState.value =
                        SyncState.Error("Network error. Please check your connection.")
                }
            }
        }
    }

    fun resetSyncState() {
        _syncState.value = SyncState.Idle
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun getFilteredPlatforms(): List<PlatformDto> {
        val currentState = _uiState.value
        return if (currentState is BlacklistUiState.Success) {
            if (searchQuery.isBlank()) {
                currentState.platforms
            } else {
                currentState.platforms.filter { platform ->
                    platform.name.contains(searchQuery, true) ||
                            platform.category.contains(searchQuery, true) ||
                            platform.keywords.any { it.contains(searchQuery, true) }
                }
            }
        } else emptyList()
    }
}

sealed class BlacklistUiState {
    object Loading : BlacklistUiState()
    data class Success(val platforms: List<PlatformDto>) : BlacklistUiState()
    data class Error(val message: String) : BlacklistUiState()
}

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    object Success : SyncState()
    data class Error(val message: String) : SyncState()
}

@Composable
fun BlacklistScreen(
    onBack: () -> Unit,
    viewModel: BlacklistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val searchQuery by remember { derivedStateOf { viewModel.searchQuery } }
    val filteredPlatforms = remember(searchQuery, uiState) {
        viewModel.getFilteredPlatforms()
    }

    LaunchedEffect(syncState) {
        if (syncState is SyncState.Success) {
            kotlinx.coroutines.delay(2000)
            viewModel.resetSyncState()
        }
    }

    SafiScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            SafiTopBar(title = "Blacklisted Platforms", onBack = onBack)

            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {

                SafiTextField(
                    value = searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    label = "",
                    placeholder = "Search platforms...",
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, null, tint = SafiColors.Hint)
                    }
                )

                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SafiButton(
                        text = "Sync from Server",
                        onClick = viewModel::syncBlacklist,
                        enabled = syncState !is SyncState.Syncing,
                        loading = syncState is SyncState.Syncing,
                        icon = Icons.Outlined.Sync,
                        modifier = Modifier.weight(1f)
                    )

                    SafiOutlinedButton(
                        text = "Refresh",
                        onClick = viewModel::loadBlacklist,
                        enabled = syncState !is SyncState.Syncing,
                        icon = Icons.Outlined.Refresh,
                        modifier = Modifier.weight(1f)
                    )
                }

                when (val state = syncState) {
                    is SyncState.Success -> {
                        Text("Blacklist synced successfully!")
                    }
                    is SyncState.Error -> {
                        Text(state.message)
                    }
                    else -> {}
                }
            }

            Spacer(Modifier.height(16.dp))

            when (val state = uiState) {
                is BlacklistUiState.Loading -> FullScreenLoading()

                is BlacklistUiState.Error -> {
                    Text(state.message)
                }

                is BlacklistUiState.Success -> {
                    LazyColumn {
                        items(filteredPlatforms) {
                            PlatformCard(it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlatformCard(platform: PlatformDto) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SafiColors.CardBorder) // ✅ FIXED
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(platform.name)
        }
    }
}