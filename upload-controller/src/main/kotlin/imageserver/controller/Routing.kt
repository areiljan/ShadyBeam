package imageserver.controller

import imageserver.model.ImageUploadRequest
import imageserver.service.ImageStorageService
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class ImageUploadController {
    private val logger = LoggerFactory.getLogger(ImageUploadController::class.java)
    private val storageService = ImageStorageService()

    fun registerRoutes(route: Route) {
        route.post {
            val rawBody = call.receiveText()
            logger.info("Raw request body: $rawBody")
            logger.info("Received request with content type: ${call.request.contentType()}")
            val request = Json.decodeFromString<ImageUploadRequest>(rawBody)
            logger.info("Received image upload from device: ${'$'}{request.deviceId} at ${'$'}{LocalDateTime.now()}")
            try {
                val metadata = storageService.saveImage(request)
                logger.info("Image saved: ${'$'}{metadata.filename}, Size: ${'$'}{metadata.size} bytes")
                call.respond(HttpStatusCode.OK)
            } catch (e: Exception) {
                logger.error("Error processing image upload: ${'$'}{e.message}", e)
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        route.get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val file = storageService.getImageById(id)
            if (file != null && file.exists()) {
                call.respondFile(file)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}
