package com.example.pocketpos_lite.feature.business

import androidx.lifecycle.viewModelScope
import com.example.pocketpos_lite.core.common.BaseViewModel
import com.example.pocketpos_lite.core.common.Resource
import com.example.pocketpos_lite.domain.model.Business
import com.example.pocketpos_lite.domain.repository.BusinessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BusinessUiState(
    val isLoading: Boolean = false,
    val business: Business? = null,
    val isUpdating: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class BusinessViewModel @Inject constructor(
    private val repository: BusinessRepository
) : BaseViewModel<BusinessUiState>(BusinessUiState()) {

    init {
        loadBusinessProfile()
    }

    fun loadBusinessProfile() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            when (val result = repository.getBusinessProfile()) {
                is Resource.Success -> updateState { it.copy(isLoading = false, business = result.data) }
                is Resource.Error -> updateState { it.copy(isLoading = false, error = result.message) }
                else -> Unit
            }
        }
    }

    fun updateBusinessProfile(business: Business) {
        viewModelScope.launch {
            updateState { it.copy(isUpdating = true, error = null, successMessage = null) }
            val result = if (business.id == null) {
                repository.createBusiness(business)
            } else {
                repository.updateBusinessProfile(business)
            }
            
            when (result) {
                is Resource.Success -> {
                    loadBusinessProfile()
                    updateState { it.copy(isUpdating = false, successMessage = if (business.id == null) "Business created successfully" else "Profile updated successfully") }
                }
                is Resource.Error -> updateState { it.copy(isUpdating = false, error = result.message) }
                else -> Unit
            }
        }
    }

    fun uploadLogo(fileName: String, byteArray: ByteArray) {
        viewModelScope.launch {
            updateState { it.copy(isUpdating = true, error = null) }
            when (val result = repository.uploadLogo(fileName, byteArray)) {
                is Resource.Success -> {
                    val updatedBusiness = uiState.value.business?.copy(logo_url = result.data)
                    if (updatedBusiness != null) {
                        updateBusinessProfile(updatedBusiness)
                    } else {
                        updateState { it.copy(isUpdating = false) }
                    }
                }
                is Resource.Error -> updateState { it.copy(isUpdating = false, error = result.message) }
                else -> Unit
            }
        }
    }
}
