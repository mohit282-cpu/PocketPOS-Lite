package com.example.pocketpos_lite.domain.repository

import com.example.pocketpos_lite.core.common.Resource
import com.example.pocketpos_lite.domain.model.Product
import com.example.pocketpos_lite.domain.model.Customer
import com.example.pocketpos_lite.domain.model.Sale

interface SalesRepository {
    suspend fun searchProducts(query: String): Resource<List<Product>>
    suspend fun getCustomers(): Resource<List<Customer>>
    suspend fun createSale(
        customerId: String?,
        totalAmount: Double,
        discountAmount: Double,
        taxAmount: Double,
        netAmount: Double,
        paymentMethod: String,
        items: List<com.example.pocketpos_lite.domain.model.SaleItem>
    ): Resource<String>
}
