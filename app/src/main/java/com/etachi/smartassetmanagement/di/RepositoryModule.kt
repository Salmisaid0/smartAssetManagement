package com.etachi.smartassetmanagement.di

import com.etachi.smartassetmanagement.data.repository.InventoryRepositoryImpl
import com.etachi.smartassetmanagement.data.repository.LocationRepositoryImpl
import com.etachi.smartassetmanagement.domain.repository.InventoryRepository
import com.etachi.smartassetmanagement.domain.repository.LocationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // ═══════════════════════════════════════════════════════════════
    // INVENTORY REPOSITORY
    // ═══════════════════════════════════════════════════════════════

    @Binds
    @Singleton
    abstract fun bindInventoryRepository(
        impl: InventoryRepositoryImpl
    ): InventoryRepository

    // ═══════════════════════════════════════════════════════════════
    // LOCATION REPOSITORY (✅ ADDED - THIS WAS MISSING!)
    // ═══════════════════════════════════════════════════════════════

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        impl: LocationRepositoryImpl
    ): LocationRepository
}
