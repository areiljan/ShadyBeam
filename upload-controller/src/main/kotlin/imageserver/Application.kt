package imageserver

import imageserver.controller.ImageUploadController
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import io.ktor.server.routing.*
import org.slf4j.event.Level


fun main() {
    embeddedServer(Netty, host="0.0.0.0", port = 8080, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(CallLogging) {
    level = Level.INFO
    filter { call -> true }
    }

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                allowStructuredMapKeys = true
            }
        )
    }

    routing {
        route("/upload") {
            ImageUploadController().registerRoutes(this)
        }
    }
}
