package imageserver.model

import java.time.LocalDateTime

data class ImageMetadata(
    val id: String,
    val filename: String,
    val size: Long,
    val uploadedAt: LocalDateTime,
    val deviceId: String,
    val width: Int? = null,
    val height: Int? = null,
    val format: String? = null
)
