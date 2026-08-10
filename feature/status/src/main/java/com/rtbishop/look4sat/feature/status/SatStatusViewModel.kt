package com.rtbishop.look4sat.feature.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rtbishop.look4sat.core.domain.model.SatReport
import com.rtbishop.look4sat.core.domain.model.SatStatus
import com.rtbishop.look4sat.core.domain.repository.IAmSatRepository
import com.rtbishop.look4sat.core.domain.repository.IMainContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SatStatusUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val statuses: List<SatStatus> = emptyList(),
    val reports: Map<String, SatReport> = emptyMap(),
    val fetchedAtUtcMs: Long = 0L,
    val error: String? = null
)

class SatStatusViewModel(
    private val amSatRepo: IAmSatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SatStatusUiState(isLoading = true))
    val uiState: StateFlow<SatStatusUiState> = _uiState

    init {
        fetch()
    }

    fun fetch() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val page = amSatRepo.fetchStatus()
                if (page != null && page.statuses.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            statuses = page.statuses,
                            reports = page.reports,
                            fetchedAtUtcMs = page.fetchedAtUtcMs
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, isRefreshing = false, error = "fetch_failed")
                    }
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, error = exception.message ?: "fetch_failed")
                }
            }
        }
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        viewModelScope.launch {
            try {
                val page = amSatRepo.fetchStatus()
                if (page != null && page.statuses.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            statuses = page.statuses,
                            reports = page.reports,
                            fetchedAtUtcMs = page.fetchedAtUtcMs
                        )
                    }
                } else {
                    _uiState.update { it.copy(isRefreshing = false, error = "fetch_failed") }
                }
            } catch (exception: Exception) {
                _uiState.update { it.copy(isRefreshing = false, error = exception.message ?: "fetch_failed") }
            }
        }
    }

    companion object {
        fun factory(container: IMainContainer) = viewModelFactory {
            initializer {
                SatStatusViewModel(container.amSatRepo)
            }
        }
    }
}
