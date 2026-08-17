package com.example.pocketpos_lite.data.repository

import com.example.pocketpos_lite.core.common.Resource
import com.example.pocketpos_lite.domain.model.Product
import com.example.pocketpos_lite.domain.repository.ProductRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest
) : ProductRepository {

    private suspend fun getMyBusinessId(): String? {
        val userId = auth.currentUserOrNull()?.id ?: return null
        val membership = postgrest.from("business_users")
            .select {
                filter { eq("user_id", userId) }
            }.decodeList<Map<String, String>>().firstOrNull()
        return membership?.get("business_id")
    }

    override suspend fun getProducts(): Resource<List<Product>> {
        return try {
            val businessId = getMyBusinessId() ?: return Resource.Error("Business not found")
            val products = postgrest.from("products").select {
                filter { eq("business_id", businessId) }
            }.decodeList<Product>()
            Resource.Success(products)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load products")
        }
    }

    override suspend fun addProduct(product: Product): Resource<Unit> {
        return try {
            val businessId = getMyBusinessId() ?: return Resource.Error("Business not found")
            postgrest.from("products").insert(product.copy(business_id = businessId))
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add product")
        }
    }
}
