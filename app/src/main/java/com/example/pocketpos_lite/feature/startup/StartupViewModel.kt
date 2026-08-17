package com.example.pocketpos_lite.feature.startup

import com.example.pocketpos_lite.core.common.BaseViewModel
import com.example.pocketpos_lite.data.repository.DummyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class StartupUiState(
    val appName: String = ""
)

@HiltViewModel
class StartupViewModel @Inject constructor(
    private val repository: DummyRepository
) : BaseViewModel<StartupUiState>(StartupUiState()) {

    init {
        updateState { it.copy(appName = repository.getProjectName()) }
    }
}
