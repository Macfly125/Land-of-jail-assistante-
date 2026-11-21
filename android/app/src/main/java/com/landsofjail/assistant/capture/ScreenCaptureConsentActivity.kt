package com.landsofjail.assistant.capture

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.landsofjail.assistant.service.GameForegroundMonitorService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CapturedScreenPayload(
    val bitmap: Bitmap,
    val base64Jpeg: String,
    val capturedAtMillis: Long
)

/**
 * Activité lancée UNIQUEMENT lorsque l'utilisateur appuie sur « Analyser l’écran ».
 * Affiche la demande d'autorisation système visible MediaProjection, puis délègue
 * la capture unique au service au premier plan conforme Android 14/15.
 */
class ScreenCaptureConsentActivity : ComponentActivity() {

    companion object {
        private val _pendingCapture = MutableStateFlow<CapturedScreenPayload?>(null)
        val pendingCapture = _pendingCapture.asStateFlow()

        fun publishCapture(payload: CapturedScreenPayload) {
            _pendingCapture.value = payload
        }

        fun consumeCapture() {
            _pendingCapture.value = null
        }
    }

    private lateinit var projectionManager: MediaProjectionManager

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            GameForegroundMonitorService.runSingleCapture(
                context = applicationContext,
                resultCode = result.resultCode,
                resultData = result.data!!
            )
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }
}
