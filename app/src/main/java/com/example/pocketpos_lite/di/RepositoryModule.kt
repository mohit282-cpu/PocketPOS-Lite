package com.example.pocketpos_lite.di

import com.example.pocketpos_lite.data.repository.AuthRepositoryImpl
import com.example.pocketpos_lite.data.repository.BusinessRepositoryImpl
import com.example.pocketpos_lite.data.repository.DashboardRepositoryImpl
import com.example.pocketpos_lite.data.repository.ProductRepositoryImpl
import com.example.pocketpos_lite.data.repository.SalesRepositoryImpl
import com.example.pocketpos_lite.domain.repository.AuthRepository
import com.example.pocketpos_lite.domain.repository.BusinessRepository
import com.example.pocketpos_lite.domain.repository.DashboardRepository
import com.example.pocketpos_lite.domain.repository.ProductRepository
import com.example.pocketpos_lite.domain.repository.SalesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindBusinessRepository(
        businessRepositoryImpl: BusinessRepositoryImpl
    ): BusinessRepository

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(
        dashboardRepositoryImpl: DashboardRepositoryImpl
    ): DashboardRepository

    @Binds
    @Singleton
    abstract fun bindSalesRepository(
        salesRepositoryImpl: SalesRepositoryImpl
    ): SalesRepository

    @Binds
    @Singleton
    abstract fun bindProductRepository(
        productRepositoryImpl: ProductRepositoryImpl
    ): ProductRepository
}
