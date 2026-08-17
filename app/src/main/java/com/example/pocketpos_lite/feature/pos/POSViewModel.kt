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
    val products: List<Product> = emptyList(),
    val searchResults: List<Product> = emptyList(),
    val cartItems: List<CartItem> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val selectedCustomer: Customer? = null,
    val discountAmount: Double = 0.0,
    val paymentMethod: String = "cash",
    val searchQuery: String = "",
    val error: String? = null,
    val isProcessing: Boolean = false
) {
    val subtotal: Double get() = cartItems.sumOf { it.subtotal }
    val total: Double get() = (subtotal - discountAmount).coerceAtLeast(0.0)
    val totalItemCount: Int get() = cartItems.sumOf { it.quantity }.toInt()
}

@HiltViewModel
class POSViewModel @Inject constructor(
    private val repository: SalesRepository
) : BaseViewModel<POSUiState>(POSUiState()) {

    private val _event = MutableSharedFlow<POSEvent>()
    val event = _event.asSharedFlow()

    private var searchJob: Job? = null

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            loadProductsInternal()
            loadCustomersInternal()
            updateState { it.copy(isLoading = false) }
        }
    }

    private suspend fun loadProductsInternal() {
        when (val result = repository.getProducts()) {
            is Resource.Success -> {
                val list = result.data ?: emptyList()
                updateState { it.copy(products = list, searchResults = list) }
            }
            is Resource.Error -> updateState { it.copy(error = result.message) }
            else -> Unit
        }
    }

    private suspend fun loadCustomersInternal() {
        when (val result = repository.getCustomers()) {
            is Resource.Success -> updateState { it.copy(customers = result.data ?: emptyList()) }
            else -> Unit
        }
    }

    fun onSearchQueryChanged(query: String) {
        updateState { it.copy(searchQuery = query) }
        searchJob?.cancel()
        
        if (query.isBlank()) {
            updateState { it.copy(searchResults = it.products) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(250)
            val clean = query.trim()
            
            // Check for exact barcode/SKU match first for auto-add barcode scanning
            val exactBarcodeMatch = uiState.value.products.firstOrNull { 
                it.barcode?.equals(clean, ignoreCase = true) == true || 
                it.sku?.equals(clean, ignoreCase = true) == true 
            }

            if (exactBarcodeMatch != null) {
                addToCart(exactBarcodeMatch)
                updateState { it.copy(searchQuery = "", searchResults = it.products) }
                _event.emit(POSEvent.BarcodeScanned(exactBarcodeMatch.name))
                return@launch
            }

            when (val result = repository.searchProducts(clean)) {
                is Resource.Success -> updateState { it.copy(searchResults = result.data ?: emptyList()) }
                is Resource.Error -> updateState { it.copy(error = result.message) }
                else -> Unit
            }
        }
    }

    fun addToCart(product: Product) {
        val availableStock = product.stock_quantity
        if (availableStock <= 0) {
            viewModelScope.launch {
                _event.emit(POSEvent.ShowMessage("Cannot add '${product.name}': Out of stock!"))
            }
            return
        }

        val currentCart = uiState.value.cartItems.toMutableList()
        val existingIndex = currentCart.indexOfFirst { it.product.id == product.id }
        
        if (existingIndex != -1) {
            val item = currentCart[existingIndex]
            val newQty = item.quantity + 1.0
            if (newQty > availableStock) {
                viewModelScope.launch {
                    _event.emit(POSEvent.ShowMessage("Cannot add more '${product.name}'. Max stock available: ${availableStock.toInt()}"))
                }
                return
            }
            currentCart[existingIndex] = item.copy(quantity = newQty)
        } else {
            currentCart.add(CartItem(product, 1.0))
        }

        updateState { it.copy(cartItems = currentCart) }
    }

    fun updateQuantity(product: Product, quantity: Double) {
        if (quantity > product.stock_quantity) {
            viewModelScope.launch {
                _event.emit(POSEvent.ShowMessage("Quantity for '${product.name}' cannot exceed available stock (${product.stock_quantity.toInt()})"))
            }
            return
        }

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

    fun clearCart() {
        updateState { it.copy(cartItems = emptyList(), discountAmount = 0.0, selectedCustomer = null) }
    }

    fun setDiscount(amount: Double) {
        val validDiscount = amount.coerceAtLeast(0.0).coerceAtMost(uiState.value.subtotal)
        updateState { it.copy(discountAmount = validDiscount) }
    }

    fun selectCustomer(customer: Customer?) {
        updateState { it.copy(selectedCustomer = customer) }
    }

    fun setPaymentMethod(method: String) {
        updateState { it.copy(paymentMethod = method.lowercase()) }
    }

    fun clearError() {
        updateState { it.copy(error = null) }
    }

    fun completeSale() {
        val state = uiState.value
        if (state.cartItems.isEmpty()) {
            viewModelScope.launch {
                _event.emit(POSEvent.ShowMessage("Cart is empty!"))
            }
            return
        }

        // Pre-check stock levels in cart
        for (item in state.cartItems) {
            if (item.quantity > item.product.stock_quantity) {
                viewModelScope.launch {
                    _event.emit(POSEvent.ShowMessage("Insufficient stock for '${item.product.name}'. Stock available: ${item.product.stock_quantity.toInt()}"))
                }
                return
            }
        }

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
                    updateState { 
                        it.copy(
                            cartItems = emptyList(), 
                            discountAmount = 0.0, 
                            selectedCustomer = null, 
                            isProcessing = false
                        ) 
                    }
                    // Refresh products list to show updated stock counts
                    loadProductsInternal()
                    _event.emit(POSEvent.SaleCompleted(result.data ?: "SUCCESS"))
                }
                is Resource.Error -> {
                    val err = result.message ?: "Transaction failed"
                    updateState { it.copy(isProcessing = false, error = err) }
                    _event.emit(POSEvent.ShowMessage("Sale Failed: $err"))
                }
                else -> updateState { it.copy(isProcessing = false) }
            }
        }
    }

    sealed class POSEvent {
        data class SaleCompleted(val saleId: String) : POSEvent()
        data class BarcodeScanned(val productName: String) : POSEvent()
        data class ShowMessage(val message: String) : POSEvent()
    }
}
