package imageserver.service

import imageserver.model.ImageUploadRequest
import imageserver.model.ImageMetadata
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.UUID
import javax.imageio.ImageIO

class ImageStorageService {
    private val logger = LoggerFactory.getLogger(ImageStorageService::class.java)
    private val storageLocation = System.getenv("STORAGE_LOCATION") ?: "./uploads"

    init {
        Files.createDirectories(Paths.get(storageLocation))
    }

    fun saveImage(request: ImageUploadRequest): ImageMetadata {
        val imageBytes = Base64.getDecoder().decode(request.image)
        val filename = "flashlight_${'$'}{request.deviceId}_${'$'}timestamp_${'$'}{UUID.randomUUID()}.jpg"
        val file = File("$storageLocation/$filename")
        file.writeBytes(imageBytes)
        logger.info("Saved image to ${'$'}{file.absolutePath}")

        val (width, height) = try {
            val bimg = ImageIO.read(ByteArrayInputStream(imageBytes))
            Pair(bimg.width, bimg.height)
        } catch (e: Exception) {
            logger.warn("Could not read image dimensions: ${'$'}{e.message}")
            Pair(null, null)
        }

        return ImageMetadata(
            id = UUID.randomUUID().toString(),
            filename = filename,
            size = file.length(),
            uploadedAt = LocalDateTime.now(),
            deviceId = request.deviceId,
            width = width,
            height = height,
            format = "JPEG"
        )
    }

    fun getImageById(id: String): File? {
        val dir = File(storageLocation)
        return dir.listFiles { f -> f.isFile && f.name.contains(id) }?.firstOrNull()
    }
}
