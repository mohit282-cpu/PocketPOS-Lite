package com.example.pocketpos_lite.data.repository

import com.example.pocketpos_lite.core.common.Resource
import com.example.pocketpos_lite.domain.model.Business
import com.example.pocketpos_lite.domain.repository.BusinessRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import javax.inject.Inject

class BusinessRepositoryImpl @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest,
    private val storage: Storage
) : BusinessRepository {

    private suspend fun getMyBusinessId(): String? {
        val userId = auth.currentUserOrNull()?.id ?: return null
        val membership = postgrest.from("business_users")
            .select {
                filter {
                    eq("user_id", userId)
                }
            }.decodeList<Map<String, String>>().firstOrNull()
        return membership?.get("business_id")
    }

    override suspend fun getBusinessProfile(): Resource<Business> {
        return try {
            val businessId = getMyBusinessId() ?: return Resource.Error("Business not found")
            val business = postgrest.from("businesses")
                .select {
                    filter {
                        eq("id", businessId)
                    }
                }.decodeSingle<Business>()
            Resource.Success(business)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load business profile")
        }
    }

    override suspend fun updateBusinessProfile(business: Business): Resource<Unit> {
        return try {
            val businessId = business.id ?: return Resource.Error("Business ID missing")
            postgrest.from("businesses").update(business) {
                filter {
                    eq("id", businessId)
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update profile")
        }
    }

    override suspend fun uploadLogo(fileName: String, byteArray: ByteArray): Resource<String> {
        return try {
            val bucket = storage.from("logos")
            val path = "${auth.currentUserOrNull()?.id}/$fileName"
            bucket.upload(path, byteArray) {
                upsert = true
            }
            val url = bucket.publicUrl(path)
            Resource.Success(url)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to upload logo")
        }
    }
}
