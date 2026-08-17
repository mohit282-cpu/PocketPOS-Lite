package com.example.pocketpos_lite.feature.products

import androidx.lifecycle.viewModelScope
import com.example.pocketpos_lite.core.common.BaseViewModel
import com.example.pocketpos_lite.core.common.Resource
import com.example.pocketpos_lite.domain.model.Category
import com.example.pocketpos_lite.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryUiState(
    val isLoading: Boolean = false,
    val categories: List<Category> = emptyList(),
    val error: String? = null,
    val isProcessing: Boolean = false
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: ProductRepository
) : BaseViewModel<CategoryUiState>(CategoryUiState()) {

    private val _event = MutableSharedFlow<CategoryEvent>()
    val event = _event.asSharedFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            when (val result = repository.getCategories()) {
                is Resource.Success -> updateState { it.copy(isLoading = false, categories = result.data ?: emptyList()) }
                is Resource.Error -> updateState { it.copy(isLoading = false, error = result.message) }
                else -> Unit
            }
        }
    }

    fun addCategory(name: String, description: String?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            updateState { it.copy(isProcessing = true) }
            val category = Category(business_id = "", name = name, description = description)
            when (val result = repository.addCategory(category)) {
                is Resource.Success -> {
                    loadCategories()
                    _event.emit(CategoryEvent.Success("Category added"))
                }
                is Resource.Error -> updateState { it.copy(isProcessing = false, error = result.message) }
                else -> Unit
            }
            updateState { it.copy(isProcessing = false) }
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            updateState { it.copy(isProcessing = true) }
            when (val result = repository.updateCategory(category)) {
                is Resource.Success -> {
                    loadCategories()
                    _event.emit(CategoryEvent.Success("Category updated"))
                }
                is Resource.Error -> updateState { it.copy(isProcessing = false, error = result.message) }
                else -> Unit
            }
            updateState { it.copy(isProcessing = false) }
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            updateState { it.copy(isProcessing = true) }
            when (val result = repository.deleteCategory(id)) {
                is Resource.Success -> {
                    loadCategories()
                    _event.emit(CategoryEvent.Success("Category deleted"))
                }
                is Resource.Error -> updateState { it.copy(isProcessing = false, error = result.message) }
                else -> Unit
            }
            updateState { it.copy(isProcessing = false) }
        }
    }

    sealed class CategoryEvent {
        data class Success(val message: String) : CategoryEvent()
    }
}
