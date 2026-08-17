package com.example.pocketpos_lite.feature.products

import androidx.lifecycle.viewModelScope
import com.example.pocketpos_lite.core.common.BaseViewModel
import com.example.pocketpos_lite.core.common.Resource
import com.example.pocketpos_lite.domain.model.Product
import com.example.pocketpos_lite.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductUiState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val repository: ProductRepository
) : BaseViewModel<ProductUiState>(ProductUiState()) {

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            when (val result = repository.getProducts()) {
                is Resource.Success -> updateState { it.copy(isLoading = false, products = result.data ?: emptyList()) }
                is Resource.Error -> updateState { it.copy(isLoading = false, error = result.message) }
                else -> Unit
            }
        }
    }

    fun addProduct(name: String, price: Double, stock: Double) {
        viewModelScope.launch {
            val product = Product(
                business_id = "", // Will be set in repository
                name = name,
                price = price,
                stock_quantity = stock
            )
            when (val result = repository.addProduct(product)) {
                is Resource.Success -> loadProducts()
                is Resource.Error -> updateState { it.copy(error = result.message) }
                else -> Unit
            }
        }
    }
}
