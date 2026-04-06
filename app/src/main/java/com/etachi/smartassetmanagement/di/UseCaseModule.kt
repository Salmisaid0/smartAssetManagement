// File: di/UseCaseModule.kt
package com.etachi.smartassetmanagement.di

import com.etachi.smartassetmanagement.data.repository.AssetRepository
import com.etachi.smartassetmanagement.domain.usecase.inventory.ScanAssetUseCase
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
        inventoryRepository: com.etachi.smartassetmanagement.domain.repository.InventoryRepository,
        sessionManager: com.etachi.smartassetmanagement.utils.UserSessionManager,
        assetRepository: AssetRepository
    ): ScanAssetUseCase {
        return ScanAssetUseCase(
            inventoryRepository = inventoryRepository,
            sessionManager = sessionManager,
            getAssetByQrCode = { qrCode -> assetRepository.getAssetByQrCode(qrCode) }
        )
    }
}