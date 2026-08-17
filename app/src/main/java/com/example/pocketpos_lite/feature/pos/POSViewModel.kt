package com.example.pocketpos_lite.feature.pos

import androidx.lifecycle.viewModelScope
import com.example.pocketpos_lite.core.common.BaseViewModel
import com.example.pocketpos_lite.core.common.Resource
import com.example.pocketpos_lite.domain.model.Customer
import com.example.pocketpos_lite.domain.model.Product
import com.example.pocketpos_lite.domain.model.SaleItem
import com.example.pocketpos_lite.domain.repository.SalesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class POSUiState(
    val isLoading: Boolean = false,
    val searchResults: List<Product> = emptyList(),
    val cartItems: List<CartItem> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val selectedCustomer: Customer? = null,
    val discountAmount: Double = 0.0,
    val paymentMethod: String = "cash",
    val error: String? = null,
    val isProcessing: Boolean = false
) {
    val subtotal: Double get() = cartItems.sumOf { it.subtotal }
    val total: Double get() = (subtotal - discountAmount).coerceAtLeast(0.0)
}

@HiltViewModel
class POSViewModel @Inject constructor(
    private val repository: SalesRepository
) : BaseViewModel<POSUiState>(POSUiState()) {

    private val _event = MutableSharedFlow<POSEvent>()
    val event = _event.asSharedFlow()

    private var searchJob: Job? = null

    init {
        loadCustomers()
    }

    fun searchProducts(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            updateState { it.copy(searchResults = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            when (val result = repository.searchProducts(query)) {
                is Resource.Success -> updateState { it.copy(searchResults = result.data ?: emptyList()) }
                is Resource.Error -> updateState { it.copy(error = result.message) }
                else -> Unit
            }
        }
    }

    private fun loadCustomers() {
        viewModelScope.launch {
            when (val result = repository.getCustomers()) {
                is Resource.Success -> updateState { it.copy(customers = result.data ?: emptyList()) }
                else -> Unit
            }
        }
    }

    fun addToCart(product: Product) {
        val currentCart = uiState.value.cartItems.toMutableList()
        val existingIndex = currentCart.indexOfFirst { it.product.id == product.id }
        if (existingIndex != -1) {
            val item = currentCart[existingIndex]
            currentCart[existingIndex] = item.copy(quantity = item.quantity + 1)
        } else {
            currentCart.add(CartItem(product, 1.0))
        }
        updateState { it.copy(cartItems = currentCart) }
    }

    fun updateQuantity(product: Product, quantity: Double) {
        val currentCart = uiState.value.cartItems.toMutableList()
        val index = currentCart.indexOfFirst { it.product.id == product.id }
        if (index != -1) {
            if (quantity <= 0) {
                currentCart.removeAt(index)
            } else {
                currentCart[index] = currentCart[index].copy(quantity = quantity)
            }
            updateState { it.copy(cartItems = currentCart) }
        }
    }

    fun removeFromCart(product: Product) {
        val currentCart = uiState.value.cartItems.filterNot { it.product.id == product.id }
        updateState { it.copy(cartItems = currentCart) }
    }

    fun setDiscount(amount: Double) {
        updateState { it.copy(discountAmount = amount) }
    }

    fun selectCustomer(customer: Customer?) {
        updateState { it.copy(selectedCustomer = customer) }
    }

    fun setPaymentMethod(method: String) {
        updateState { it.copy(paymentMethod = method) }
    }

    fun completeSale() {
        val state = uiState.value
        if (state.cartItems.isEmpty()) return

        viewModelScope.launch {
            updateState { it.copy(isProcessing = true, error = null) }
            
            val saleItems = state.cartItems.map {
                SaleItem(
                    product_id = it.product.id!!,
                    quantity = it.quantity,
                    unit_price = it.product.price,
                    subtotal = it.subtotal
                )
            }

            val result = repository.createSale(
                customerId = state.selectedCustomer?.id,
                totalAmount = state.subtotal,
                discountAmount = state.discountAmount,
                taxAmount = 0.0,
                netAmount = state.total,
                paymentMethod = state.paymentMethod,
                items = saleItems
            )

            when (result) {
                is Resource.Success -> {
                    updateState { it.copy(cartItems = emptyList(), discountAmount = 0.0, selectedCustomer = null, isProcessing = false) }
                    _event.emit(POSEvent.SaleCompleted(result.data!!))
                }
                is Resource.Error -> {
                    updateState { it.copy(isProcessing = false, error = result.message) }
                }
                else -> Unit
            }
        }
    }

    sealed class POSEvent {
        data class SaleCompleted(val saleId: String) : POSEvent()
    }
}
