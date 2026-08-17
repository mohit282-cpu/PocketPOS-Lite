package com.example.pocketpos_lite.feature.pos

import com.example.pocketpos_lite.domain.model.Product

data class CartItem(
    val product: Product,
    val quantity: Double
) {
    val subtotal: Double get() = product.price * quantity
}
