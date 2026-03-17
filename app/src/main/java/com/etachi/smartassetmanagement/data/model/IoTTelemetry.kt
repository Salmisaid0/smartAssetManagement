package com.etachi.smartassetmanagement.data.model

data class IoTTelemetry(
    val deviceId: String,
    val temperature: Double,
    val battery: Int,
    val isOnline: Boolean,
    val timestamp: String
)