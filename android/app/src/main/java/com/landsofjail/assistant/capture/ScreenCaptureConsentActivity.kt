package com.landsofjail.assistant.capture

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.File
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
    val capturedAtMillis: Long,
    val imageFileUri: String = ""
)

/**
 * Activité lancée après une action explicite de l'utilisateur : capture unique
 * ou analyse automatique. Affiche l'autorisation système MediaProjection puis
 * délègue la session au service au premier plan; l'arrêt reste accessible dans l'app
 * et dans la notification persistante.
 */
class ScreenCaptureConsentActivity : ComponentActivity() {

    companion object {
        private val _pendingCapture = MutableStateFlow<CapturedScreenPayload?>(null)
        val pendingCapture = _pendingCapture.asStateFlow()

        fun publishCapture(payload: CapturedScreenPayload) {
            _pendingCapture.value = payload
        }

        const val EXTRA_AUTO_CAPTURE = "extra_auto_capture"
        const val EXTRA_INTERVAL_MILLIS = "extra_auto_capture_interval_millis"

        private const val PREFS_CAPTURE = "loj_capture_state"
        private const val KEY_PENDING_URI = "pending_capture_uri"
        private const val KEY_PENDING_AT = "pending_capture_at"

        fun restorePendingCapture(context: Context) {
            if (_pendingCapture.value != null) return
            val prefs = context.getSharedPreferences(PREFS_CAPTURE, Context.MODE_PRIVATE)
            val uriText = prefs.getString(KEY_PENDING_URI, null) ?: return
            val uri = runCatching { Uri.parse(uriText) }.getOrNull() ?: return
            val file = uri.path?.let(::File) ?: return
            if (!file.exists()) return
            runCatching {
                val bytes = file.readBytes()
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return
                _pendingCapture.value = CapturedScreenPayload(
                    bitmap = bitmap,
                    base64Jpeg = Base64.encodeToString(bytes, Base64.NO_WRAP),
                    capturedAtMillis = prefs.getLong(KEY_PENDING_AT, file.lastModified()),
                    imageFileUri = uriText
                )
            }
        }

        fun consumeCapture(context: Context) {
            val consumed = _pendingCapture.value
            _pendingCapture.value = null
            consumed?.bitmap?.let { bitmap -> if (!bitmap.isRecycled) bitmap.recycle() }
            context.getSharedPreferences(PREFS_CAPTURE, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_PENDING_URI)
                .remove(KEY_PENDING_AT)
                .apply()
            GameForegroundMonitorService.notifyAutoCaptureConsumed(context)
        }
    }

    private lateinit var projectionManager: MediaProjectionManager
    private var autoCaptureRequested: Boolean = false
    private var autoCaptureIntervalMillis: Long = 10_000L

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            if (autoCaptureRequested) {
                GameForegroundMonitorService.startAutoCapture(
                    context = applicationContext,
                    resultCode = result.resultCode,
                    resultData = result.data!!,
                    intervalMillis = autoCaptureIntervalMillis
                )
            } else {
                GameForegroundMonitorService.runSingleCapture(
                    context = applicationContext,
                    resultCode = result.resultCode,
                    resultData = result.data!!
                )
            }
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        autoCaptureRequested = intent.getBooleanExtra(EXTRA_AUTO_CAPTURE, false)
        autoCaptureIntervalMillis = intent.getLongExtra(EXTRA_INTERVAL_MILLIS, 10_000L)
            .coerceIn(10_000L, 60_000L)
        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        // ActivityResultRegistry restaure une demande déjà en cours après une recréation
        // (rotation / changement de configuration). Ne pas ouvrir une seconde boîte
        // de consentement MediaProjection dans ce cas.
        if (savedInstanceState == null) {
            projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
        }
    }
}
