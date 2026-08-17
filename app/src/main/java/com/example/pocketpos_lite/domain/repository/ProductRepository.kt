package com.example.pocketpos_lite.domain.repository

import com.example.pocketpos_lite.core.common.Resource
import com.example.pocketpos_lite.domain.model.Product

interface ProductRepository {
    suspend fun getProducts(): Resource<List<Product>>
    suspend fun addProduct(product: Product): Resource<Unit>
}
