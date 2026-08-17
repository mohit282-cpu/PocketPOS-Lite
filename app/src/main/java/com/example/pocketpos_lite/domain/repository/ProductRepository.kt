package com.example.pocketpos_lite.domain.repository

import com.example.pocketpos_lite.core.common.Resource
import com.example.pocketpos_lite.domain.model.Category
import com.example.pocketpos_lite.domain.model.Product

interface ProductRepository {
    suspend fun getProducts(categoryId: String? = null, searchQuery: String? = null): Resource<List<Product>>
    suspend fun addProduct(product: Product, imageBytes: ByteArray?, fileName: String?): Resource<Unit>
    suspend fun updateProduct(product: Product, newImageBytes: ByteArray?, fileName: String?): Resource<Unit>
    suspend fun deleteProduct(id: String, imageUrl: String?): Resource<Unit>
    
    suspend fun getCategories(): Resource<List<Category>>
    suspend fun addCategory(category: Category): Resource<Category>
    suspend fun updateCategory(category: Category): Resource<Unit>
    suspend fun deleteCategory(id: String): Resource<Unit>
}
