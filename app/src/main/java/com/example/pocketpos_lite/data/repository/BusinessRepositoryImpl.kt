package com.example.pocketpos_lite.data.repository

import com.example.pocketpos_lite.core.common.Resource
import com.example.pocketpos_lite.core.util.ErrorUtils
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
        return try {
            val userId = auth.currentUserOrNull()?.id ?: return null
            val membership = postgrest.from("business_users")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }.decodeList<Map<String, String>>().firstOrNull()
            membership?.get("business_id")
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getBusinessProfile(): Resource<Business> {
        return try {
            val businessId = getMyBusinessId() ?: return Resource.Error("Shop profile missing")
            val business = postgrest.from("businesses")
                .select {
                    filter {
                        eq("id", businessId)
                    }
                }.decodeSingle<Business>()
            Resource.Success(business)
        } catch (e: Exception) {
            Resource.Error(ErrorUtils.cleanSupabaseError(e.message))
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
            Resource.Error(ErrorUtils.cleanSupabaseError(e.message))
        }
    }

    override suspend fun createBusiness(business: Business): Resource<Unit> {
        return try {
            val userId = auth.currentUserOrNull()?.id ?: return Resource.Error("User not logged in")
            
            // 1. Ensure Profile exists (trigger might have missed it for old users)
            val profile = mapOf("id" to userId, "full_name" to (auth.currentUserOrNull()?.userMetadata?.get("full_name") ?: "Business Owner"))
            postgrest.from("profiles").upsert(profile)

            // 2. Create Business
            val newBusiness = business.copy(owner_id = userId)
            val insertedBusiness = postgrest.from("businesses").insert(newBusiness) {
                select()
            }.decodeSingle<Business>()

            // 3. Create Membership
            val membership = mapOf(
                "business_id" to insertedBusiness.id,
                "user_id" to userId,
                "role" to "owner"
            )
            postgrest.from("business_users").insert(membership)

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(ErrorUtils.cleanSupabaseError(e.message))
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
            Resource.Error(ErrorUtils.cleanSupabaseError(e.message))
        }
    }
}
