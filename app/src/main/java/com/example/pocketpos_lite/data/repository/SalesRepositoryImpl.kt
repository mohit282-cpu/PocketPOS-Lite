package com.example.pocketpos_lite.data.repository

import com.example.pocketpos_lite.core.common.Resource
import com.example.pocketpos_lite.domain.model.Customer
import com.example.pocketpos_lite.domain.model.Product
import com.example.pocketpos_lite.domain.model.SaleItem
import com.example.pocketpos_lite.domain.repository.SalesRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class SalesRepositoryImpl @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest
) : SalesRepository {

    private suspend fun getMyBusinessId(): String? {
        val userId = auth.currentUserOrNull()?.id ?: return null
        val membership = postgrest.from("business_users")
            .select {
                filter { eq("user_id", userId) }
            }.decodeList<Map<String, String>>().firstOrNull()
        return membership?.get("business_id")
    }

    override suspend fun searchProducts(query: String): Resource<List<Product>> {
        return try {
            val businessId = getMyBusinessId() ?: return Resource.Error("Business not found")
            val products = postgrest.from("products").select {
                filter {
                    eq("business_id", businessId)
                    or {
                        ilike("name", "%$query%")
                        ilike("sku", "%$query%")
                        ilike("barcode", "%$query%")
                    }
                }
            }.decodeList<Product>()
            Resource.Success(products)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Search failed")
        }
    }

    override suspend fun getCustomers(): Resource<List<Customer>> {
        return try {
            val businessId = getMyBusinessId() ?: return Resource.Error("Business not found")
            val customers = postgrest.from("customers").select {
                filter { eq("business_id", businessId) }
            }.decodeList<Customer>()
            Resource.Success(customers)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load customers")
        }
    }

    override suspend fun createSale(
        customerId: String?,
        totalAmount: Double,
        discountAmount: Double,
        taxAmount: Double,
        netAmount: Double,
        paymentMethod: String,
        items: List<SaleItem>
    ): Resource<String> {
        return try {
            val businessId = getMyBusinessId() ?: return Resource.Error("Business not found")
            
            val itemsJson = buildJsonArray {
                items.forEach { item ->
                    add(buildJsonObject {
                        put("product_id", item.product_id)
                        put("quantity", item.quantity)
                        put("unit_price", item.unit_price)
                        put("subtotal", item.subtotal)
                    })
                }
            }

            val result = postgrest.rpc(
                "create_sale",
                buildJsonObject {
                    put("p_business_id", businessId)
                    put("p_customer_id", customerId)
                    put("p_total_amount", totalAmount)
                    put("p_discount_amount", discountAmount)
                    put("p_tax_amount", taxAmount)
                    put("p_net_amount", netAmount)
                    put("p_payment_method", paymentMethod)
                    put("p_items", itemsJson)
                }
            ).data.replace("\"", "")

            Resource.Success(result)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Sale creation failed")
        }
    }
}
