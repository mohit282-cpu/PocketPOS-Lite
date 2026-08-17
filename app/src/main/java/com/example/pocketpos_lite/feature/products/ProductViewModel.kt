package com.example.pocketpos_lite.feature.products

import androidx.lifecycle.viewModelScope
import com.example.pocketpos_lite.core.common.BaseViewModel
import com.example.pocketpos_lite.core.common.Resource
import com.example.pocketpos_lite.domain.model.Category
import com.example.pocketpos_lite.domain.model.Product
import com.example.pocketpos_lite.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductUiState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val error: String? = null,
    val isProcessing: Boolean = false
)

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val repository: ProductRepository
) : BaseViewModel<ProductUiState>(ProductUiState()) {

    private val _event = MutableSharedFlow<ProductEvent>()
    val event = _event.asSharedFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            val categoriesResult = repository.getCategories()
            if (categoriesResult is Resource.Success) {
                updateState { it.copy(categories = categoriesResult.data ?: emptyList()) }
            }
            loadProducts()
        }
    }

    fun loadProducts() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            when (val result = repository.getProducts(uiState.value.selectedCategoryId, uiState.value.searchQuery)) {
                is Resource.Success -> updateState { it.copy(isLoading = false, products = result.data ?: emptyList()) }
                is Resource.Error -> updateState { it.copy(isLoading = false, error = result.message) }
                else -> Unit
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        updateState { it.copy(searchQuery = query) }
        loadProducts()
    }

    fun onCategoryFilterChanged(categoryId: String?) {
        updateState { it.copy(selectedCategoryId = categoryId) }
        loadProducts()
    }

    fun addProduct(product: Product, imageBytes: ByteArray?, fileName: String?) {
        viewModelScope.launch {
            updateState { it.copy(isProcessing = true, error = null) }
            when (val result = repository.addProduct(product, imageBytes, fileName)) {
                is Resource.Success -> {
                    loadProducts()
                    _event.emit(ProductEvent.Success("Product added successfully"))
                }
                is Resource.Error -> updateState { it.copy(isProcessing = false, error = result.message) }
                else -> Unit
            }
            updateState { it.copy(isProcessing = false) }
        }
    }

    fun updateProduct(product: Product, newImageBytes: ByteArray?, fileName: String?) {
        viewModelScope.launch {
            updateState { it.copy(isProcessing = true, error = null) }
            when (val result = repository.updateProduct(product, newImageBytes, fileName)) {
                is Resource.Success -> {
                    loadProducts()
                    _event.emit(ProductEvent.Success("Product updated successfully"))
                }
                is Resource.Error -> updateState { it.copy(isProcessing = false, error = result.message) }
                else -> Unit
            }
            updateState { it.copy(isProcessing = false) }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            updateState { it.copy(isProcessing = true, error = null) }
            when (val result = repository.deleteProduct(product.id!!, product.image_url)) {
                is Resource.Success -> {
                    loadProducts()
                    _event.emit(ProductEvent.Success("Product deleted successfully"))
                }
                is Resource.Error -> updateState { it.copy(isProcessing = false, error = result.message) }
                else -> Unit
            }
            updateState { it.copy(isProcessing = false) }
        }
    }

    sealed class ProductEvent {
        data class Success(val message: String) : ProductEvent()
    }
}
