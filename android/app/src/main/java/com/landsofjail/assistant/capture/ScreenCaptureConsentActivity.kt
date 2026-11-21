package com.landsofjail.assistant.capture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.landsofjail.assistant.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream

data class CapturedScreenPayload(
    val bitmap: Bitmap,
    val base64Jpeg: String,
    val capturedAtMillis: Long
)

/**
 * Activité lancée UNIQUEMENT lorsque l'utilisateur appuie sur « Analyser l’écran ».
 * Demande la permission système visible MediaProjection, capture UNE seule image,
 * arrête immédiatement la projection, puis renvoie la capture vers MainActivity pour l'OCR et les conseils.
 */
class ScreenCaptureConsentActivity : ComponentActivity() {

    companion object {
        private val _pendingCapture = MutableStateFlow<CapturedScreenPayload?>(null)
        val pendingCapture = _pendingCapture.asStateFlow()

        fun consumeCapture() {
            _pendingCapture.value = null
        }
    }

    private lateinit var projectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val projection = projectionManager.getMediaProjection(result.resultCode, result.data!!)
            if (projection != null) {
                captureSingleFrame(projection)
            } else {
                finish()
            }
        } else {
            // L'utilisateur a refusé l'autorisation système : aucune capture effectuée
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        // Affiche la boîte de dialogue système visible d'autorisation de capture d'écran
        projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    private fun captureSingleFrame(projection: MediaProjection) {
        mediaProjection = projection
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader = reader

        virtualDisplay = projection.createVirtualDisplay(
            "LandsOfJailSingleCapture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            null
        )

        var handled = false
        reader.setOnImageAvailableListener({ r ->
            if (handled) return@setOnImageAvailableListener
            val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
            handled = true
            try {
                val plane = image.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * width

                val fullBitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888
                )
                fullBitmap.copyPixelsFromBuffer(buffer)
                val cropped = Bitmap.createBitmap(fullBitmap, 0, 0, width, height)
                fullBitmap.recycle()

                val stream = ByteArrayOutputStream()
                cropped.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                val base64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

                _pendingCapture.value = CapturedScreenPayload(
                    bitmap = cropped,
                    base64Jpeg = base64,
                    capturedAtMillis = System.currentTimeMillis()
                )
            } finally {
                image.close()
                releaseProjection()
                openAssistantDashboard()
            }
        }, Handler(Looper.getMainLooper()))
    }

    private fun openAssistantDashboard() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        finish()
    }

    private fun releaseProjection() {
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        virtualDisplay = null
        imageReader = null
        mediaProjection = null
    }

    override fun onDestroy() {
        releaseProjection()
        super.onDestroy()
    }
}
