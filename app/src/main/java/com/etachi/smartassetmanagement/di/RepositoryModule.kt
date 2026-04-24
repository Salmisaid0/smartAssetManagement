package com.etachi.smartassetmanagement.di

import com.etachi.smartassetmanagement.data.repository.AssetRepository
import com.etachi.smartassetmanagement.data.repository.InventoryRepositoryImpl
import com.etachi.smartassetmanagement.data.repository.LocationRepositoryImpl
import com.etachi.smartassetmanagement.data.repository.RelocationRepository
import com.etachi.smartassetmanagement.data.repository.ScheduledInventoryRepository
import com.etachi.smartassetmanagement.domain.repository.InventoryRepository
import com.etachi.smartassetmanagement.domain.repository.LocationRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // ═══════════════════════════════════════════════════════════════
    // INTERFACE-BASED REPOSITORIES (Use @Binds)
    // ═══════════════════════════════════════════════════════════════

    @Binds
    @Singleton
    abstract fun bindInventoryRepository(
        impl: InventoryRepositoryImpl
    ): InventoryRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        impl: LocationRepositoryImpl
    ): LocationRepository

    // ═══════════════════════════════════════════════════════════════
    // CONCRETE REPOSITORIES (Use @Provides in companion object)
    // ═══════════════════════════════════════════════════════════════

    companion object {

        @Provides
        @Singleton
        fun provideAssetRepository(
            db: com.google.firebase.firestore.FirebaseFirestore,
            sessionManager: com.etachi.smartassetmanagement.utils.UserSessionManager
        ): AssetRepository {
            return AssetRepository(db, sessionManager)
        }

        @Provides
        @Singleton
        fun provideScheduledInventoryRepository(
            db: com.google.firebase.firestore.FirebaseFirestore
        ): ScheduledInventoryRepository {
            return ScheduledInventoryRepository(db)
        }

        @Provides
        @Singleton
        fun provideRelocationRepository(
            db: com.google.firebase.firestore.FirebaseFirestore
        ): RelocationRepository {
            return RelocationRepository(db)
        }
    }
}
