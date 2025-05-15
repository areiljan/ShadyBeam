package imageserver.model

import kotlinx.serialization.Serializable

@Serializable
data class ImageUploadRequest(
    val image: String,
    val timestamp: Long,
    val deviceId: String
)
