package com.example.flashlightapplication

import android.widget.Button
import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.graphics.Color
import android.widget.ImageView

class FlashLight : AppCompatActivity(){
    private var isFlashOn = false
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList.first {
            cameraManager.getCameraCharacteristics(it)
                .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }

        val button = findViewById<Button>(R.id.flashlightButton)
        val rootLayout = findViewById<View>(R.id.rootLayout)
        val image = findViewById<ImageView>(R.id.backgroundImage)
        button.setOnClickListener {
            isFlashOn = !isFlashOn
            toggleFlashLight(isFlashOn)
            button.text = if (isFlashOn) "Turn Off" else "Turn On"

            val color = if (isFlashOn) Color.WHITE else Color.BLACK
            rootLayout.setBackgroundColor(color)

            if (isFlashOn) {
                image.visibility = View.VISIBLE
            } else {
                image.visibility = View.INVISIBLE
            }
        }
    }

    private fun toggleFlashLight(state: Boolean) {
        cameraManager.setTorchMode(cameraId, state);

    }
}