package com.example.pocketpos_lite.feature.dashboard

import androidx.lifecycle.viewModelScope
import com.example.pocketpos_lite.core.common.BaseViewModel
import com.example.pocketpos_lite.core.common.Resource
import com.example.pocketpos_lite.domain.model.DashboardStats
import com.example.pocketpos_lite.domain.model.Sale
import com.example.pocketpos_lite.domain.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = false,
    val stats: DashboardStats = DashboardStats(),
    val recentSales: List<Sale> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository
) : BaseViewModel<DashboardUiState>(DashboardUiState()) {

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            
            val statsResult = repository.getDashboardStats()
            val salesResult = repository.getRecentSales()

            if (statsResult is Resource.Success && salesResult is Resource.Success) {
                updateState { it.copy(
                    isLoading = false,
                    stats = statsResult.data!!,
                    recentSales = salesResult.data!!
                ) }
            } else {
                val error = statsResult.message ?: salesResult.message ?: "An error occurred"
                updateState { it.copy(isLoading = false, error = error) }
            }
        }
    }
}
