package com.etachi.smartassetmanagement.di

import com.etachi.smartassetmanagement.data.repository.AssetRepository
import com.etachi.smartassetmanagement.domain.repository.InventoryRepository
import com.etachi.smartassetmanagement.domain.repository.LocationRepository
import com.etachi.smartassetmanagement.domain.usecase.GetInventorySessionsUseCase
import com.etachi.smartassetmanagement.domain.usecase.GetSessionByIdUseCase
import com.etachi.smartassetmanagement.domain.usecase.UpdateSessionStatusUseCase
import com.etachi.smartassetmanagement.domain.usecase.inventory.CompleteInventorySessionUseCase
import com.etachi.smartassetmanagement.domain.usecase.inventory.GetMissingAssetsUseCase
import com.etachi.smartassetmanagement.domain.usecase.inventory.GetRoomAssetsUseCase
import com.etachi.smartassetmanagement.domain.usecase.inventory.ScanAssetUseCase
import com.etachi.smartassetmanagement.domain.usecase.inventory.StartInventorySessionUseCase
import com.etachi.smartassetmanagement.utils.UserSessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideScanAssetUseCase(
        inventoryRepository: InventoryRepository,
        sessionManager: UserSessionManager,
        assetRepository: AssetRepository
    ): ScanAssetUseCase {
        return ScanAssetUseCase(
            inventoryRepository = inventoryRepository,
            sessionManager = sessionManager,
            assetRepository = assetRepository
        )
    }

    @Provides
    @Singleton
    fun provideGetInventorySessionsUseCase(
        inventoryRepository: InventoryRepository
    ): GetInventorySessionsUseCase {
        return GetInventorySessionsUseCase(inventoryRepository)
    }

    @Provides
    @Singleton
    fun provideGetSessionByIdUseCase(
        inventoryRepository: InventoryRepository
    ): GetSessionByIdUseCase {
        return GetSessionByIdUseCase(inventoryRepository)
    }

    @Provides
    @Singleton
    fun provideUpdateSessionStatusUseCase(
        inventoryRepository: InventoryRepository
    ): UpdateSessionStatusUseCase {
        return UpdateSessionStatusUseCase(inventoryRepository)
    }

    @Provides
    @Singleton
    fun provideStartInventorySessionUseCase(
        inventoryRepository: InventoryRepository,
        locationRepository: LocationRepository,
        sessionManager: UserSessionManager
    ): StartInventorySessionUseCase {
        return StartInventorySessionUseCase(
            inventoryRepository = inventoryRepository,
            locationRepository = locationRepository,
            sessionManager = sessionManager
        )
    }

    @Provides
    @Singleton
    fun provideCompleteInventorySessionUseCase(
        inventoryRepository: InventoryRepository,
        sessionManager: UserSessionManager
    ): CompleteInventorySessionUseCase {
        return CompleteInventorySessionUseCase(
            inventoryRepository = inventoryRepository,
            sessionManager = sessionManager
        )
    }

    @Provides
    @Singleton
    fun provideGetMissingAssetsUseCase(
        inventoryRepository: InventoryRepository
    ): GetMissingAssetsUseCase {
        return GetMissingAssetsUseCase(inventoryRepository)
    }

    @Provides
    @Singleton
    fun provideGetRoomAssetsUseCase(
        inventoryRepository: InventoryRepository
    ): GetRoomAssetsUseCase {
        return GetRoomAssetsUseCase(inventoryRepository)
    }
}
