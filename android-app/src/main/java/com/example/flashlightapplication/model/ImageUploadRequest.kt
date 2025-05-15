package com.example.flashlightapplication.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@InternalSerializationApi @Serializable
data class ImageUploadRequest(
    val image: String,
    val timestamp: Long,
    val deviceId: String
)
