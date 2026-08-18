package com.example.pocketpos_lite.data.repository

import com.example.pocketpos_lite.core.common.Resource
import com.example.pocketpos_lite.domain.model.DashboardStats
import com.example.pocketpos_lite.domain.model.Sale
import com.example.pocketpos_lite.domain.repository.DashboardRepository
import com.example.pocketpos_lite.core.util.ErrorUtils
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

class DashboardRepositoryImpl @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest
) : DashboardRepository {

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

    override suspend fun getDashboardStats(): Resource<DashboardStats> {
        return try {
            val businessId = getMyBusinessId() ?: return Resource.Error("Shop not found. Please complete your business profile.")
            
            // In a real app, I'd use an RPC or multiple async calls. 
            // For foundation, let's just do sequential or return defaults if tables are empty.
            
            val totalProducts = postgrest.from("products").select {
                filter { eq("business_id", businessId) }
                count(Count.EXACT)
            }.countOrNull()?.toInt() ?: 0

            val lowStockCount = postgrest.from("products").select {
                filter { 
                    eq("business_id", businessId)
                    lte("stock_quantity", 5) // Hardcoded threshold for now
                }
                count(Count.EXACT)
            }.countOrNull()?.toInt() ?: 0

            // Today's Sales
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
            val sales = postgrest.from("sales").select {
                filter {
                    eq("business_id", businessId)
                    gte("created_at", today)
                }
            }.decodeList<Map<String, Double>>()
            
            val todaySales = sales.sumOf { it["net_amount"] ?: 0.0 }
            val todayTransactions = sales.size

            // Today's Expenses
            val expenses = postgrest.from("expenses").select {
                filter {
                    eq("business_id", businessId)
                    eq("expense_date", today)
                }
            }.decodeList<Map<String, Double>>()
            val todayExpenses = expenses.sumOf { it["amount"] ?: 0.0 }

            Resource.Success(DashboardStats(
                todaySales = todaySales,
                todayTransactions = todayTransactions,
                totalProducts = totalProducts,
                lowStockProducts = lowStockCount,
                todayExpenses = todayExpenses,
                estimatedProfit = todaySales - todayExpenses // Simplified profit for foundation
            ))
        } catch (e: Exception) {
            Resource.Error(ErrorUtils.cleanSupabaseError(e.message))
        }
    }

    override suspend fun getRecentSales(limit: Int): Resource<List<Sale>> {
        return try {
            val businessId = getMyBusinessId() ?: return Resource.Error("Shop not found")
            val sales = postgrest.from("sales").select {
                filter { eq("business_id", businessId) }
                order("created_at", Order.DESCENDING)
                limit(limit.toLong())
            }.decodeList<Sale>()
            Resource.Success(sales)
        } catch (e: Exception) {
            Resource.Error(ErrorUtils.cleanSupabaseError(e.message))
        }
    }
}
