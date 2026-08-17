package com.example.pocketpos_lite.data.repository

import com.example.pocketpos_lite.core.common.Resource
import com.example.pocketpos_lite.domain.model.Category
import com.example.pocketpos_lite.domain.model.Product
import com.example.pocketpos_lite.domain.repository.ProductRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.Storage
import java.util.UUID
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest,
    private val storage: Storage
) : ProductRepository {

    private val bucketName = "product-images"

    private suspend fun getMyBusinessId(): String? {
        val userId = auth.currentUserOrNull()?.id ?: return null
        val membership = postgrest.from("business_users")
            .select {
                filter { eq("user_id", userId) }
            }.decodeList<Map<String, String>>().firstOrNull()
        return membership?.get("business_id")
    }

    private suspend fun uploadImage(bytes: ByteArray, fileName: String): String {
        val path = "${UUID.randomUUID()}_$fileName"
        val bucket = storage.from(bucketName)
        bucket.upload(path, bytes) { upsert = true }
        return bucket.publicUrl(path)
    }

    private suspend fun deleteImageFromStorage(imageUrl: String) {
        try {
            val fileName = imageUrl.substringAfterLast("/")
            if (fileName.isNotBlank()) {
                storage.from(bucketName).delete(listOf(fileName))
            }
        } catch (_: Exception) {
            // Ignore storage deletion errors gracefully
        }
    }

    override suspend fun getProducts(categoryId: String?, searchQuery: String?): Resource<List<Product>> {
        return try {
            val businessId = getMyBusinessId() ?: return Resource.Error("Business not found")
            val cleanQuery = searchQuery?.trim()

            val products = postgrest.from("products").select {
                filter {
                    eq("business_id", businessId)
                    if (!categoryId.isNullOrBlank()) {
                        eq("category_id", categoryId)
                    }
                    if (!cleanQuery.isNullOrEmpty()) {
                        or {
                            ilike("name", "%$cleanQuery%")
                            ilike("sku", "%$cleanQuery%")
                            ilike("barcode", "%$cleanQuery%")
                        }
                    }
                }
                order("created_at", order = Order.DESCENDING)
            }.decodeList<Product>()

            Resource.Success(products)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load products")
        }
    }

    override suspend fun addProduct(product: Product, imageBytes: ByteArray?, fileName: String?): Resource<Unit> {
        return try {
            val businessId = getMyBusinessId() ?: return Resource.Error("Business not found")
            
            var imageUrl = product.image_url
            if (imageBytes != null && fileName != null) {
                imageUrl = uploadImage(imageBytes, fileName)
            }

            val newProduct = product.copy(
                business_id = businessId,
                image_url = imageUrl
            )

            postgrest.from("products").insert(newProduct)
            Resource.Success(Unit)
        } catch (e: Exception) {
            val msg = e.message ?: "Failed to add product"
            val userMsg = when {
                msg.contains("unique", ignoreCase = true) || msg.contains("sku", ignoreCase = true) ->
                    "Product with this SKU already exists!"
                else -> msg
            }
            Resource.Error(userMsg)
        }
    }

    override suspend fun updateProduct(product: Product, newImageBytes: ByteArray?, fileName: String?): Resource<Unit> {
        return try {
            val businessId = getMyBusinessId() ?: return Resource.Error("Business not found")
            val productId = product.id ?: return Resource.Error("Invalid product ID")

            var imageUrl = product.image_url
            if (newImageBytes != null && fileName != null) {
                // Upload new image
                val uploadedUrl = uploadImage(newImageBytes, fileName)
                // Delete old image if present
                if (!imageUrl.isNullOrBlank()) {
                    deleteImageFromStorage(imageUrl)
                }
                imageUrl = uploadedUrl
            }

            val updatedProduct = product.copy(
                business_id = businessId,
                image_url = imageUrl
            )

            postgrest.from("products").update(updatedProduct) {
                filter {
                    eq("id", productId)
                    eq("business_id", businessId)
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            val msg = e.message ?: "Failed to update product"
            val userMsg = when {
                msg.contains("unique", ignoreCase = true) || msg.contains("sku", ignoreCase = true) ->
                    "Product with this SKU already exists!"
                else -> msg
            }
            Resource.Error(userMsg)
        }
    }

    override suspend fun deleteProduct(id: String, imageUrl: String?): Resource<Unit> {
        return try {
            val businessId = getMyBusinessId() ?: return Resource.Error("Business not found")
            
            postgrest.from("products").delete {
                filter {
                    eq("id", id)
                    eq("business_id", businessId)
                }
            }

            if (!imageUrl.isNullOrBlank()) {
                deleteImageFromStorage(imageUrl)
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete product")
        }
    }

    override suspend fun getCategories(): Resource<List<Category>> {
        return try {
            val businessId = getMyBusinessId() ?: return Resource.Error("Business not found")
            val categories = postgrest.from("categories").select {
                filter { eq("business_id", businessId) }
                order("name", order = Order.ASCENDING)
            }.decodeList<Category>()
            Resource.Success(categories)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load categories")
        }
    }

    override suspend fun addCategory(category: Category): Resource<Category> {
        return try {
            val businessId = getMyBusinessId() ?: return Resource.Error("Business not found")
            val newCategory = category.copy(business_id = businessId)
            val inserted = postgrest.from("categories").insert(newCategory) {
                select()
            }.decodeSingle<Category>()
            Resource.Success(inserted)
        } catch (e: Exception) {
            val msg = e.message ?: "Failed to create category"
            val userMsg = if (msg.contains("unique", ignoreCase = true)) "Category already exists!" else msg
            Resource.Error(userMsg)
        }
    }

    override suspend fun updateCategory(category: Category): Resource<Unit> {
        return try {
            val businessId = getMyBusinessId() ?: return Resource.Error("Business not found")
            val catId = category.id ?: return Resource.Error("Invalid category ID")
            postgrest.from("categories").update(category) {
                filter {
                    eq("id", catId)
                    eq("business_id", businessId)
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update category")
        }
    }

    override suspend fun deleteCategory(id: String): Resource<Unit> {
        return try {
            val businessId = getMyBusinessId() ?: return Resource.Error("Business not found")
            postgrest.from("categories").delete {
                filter {
                    eq("id", id)
                    eq("business_id", businessId)
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete category")
        }
    }
}
