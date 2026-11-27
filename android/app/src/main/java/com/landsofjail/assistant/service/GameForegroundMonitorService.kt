package com.landsofjail.assistant.service

import com.landsofjail.assistant.MainActivity
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Base64
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.landsofjail.assistant.capture.CapturedScreenPayload
import com.landsofjail.assistant.capture.ScreenCaptureConsentActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

/**
 * Crop an ImageReader frame without recycling the returned bitmap.
 * Bitmap.createBitmap may return the source itself when the crop covers it exactly.
 */
internal fun cropFrameBitmap(fullBitmap: Bitmap, width: Int, height: Int): Bitmap {
    require(width > 0 && height > 0 && width <= fullBitmap.width && height <= fullBitmap.height)
    val cropped = try {
        Bitmap.createBitmap(fullBitmap, 0, 0, width, height)
    } catch (error: Throwable) {
        if (!fullBitmap.isRecycled) fullBitmap.recycle()
        throw error
    }
    if (cropped !== fullBitmap) fullBitmap.recycle()
    return cropped
}

class GameForegroundMonitorService : Service() {

    companion object {
        const val ACTION_START = "com.landsofjail.assistant.action.START_MONITORING"
        const val ACTION_STOP = "com.landsofjail.assistant.action.STOP_MONITORING"
        const val ACTION_CAPTURE_WITH_PROJECTION = "com.landsofjail.assistant.action.CAPTURE_WITH_PROJECTION"
        const val ACTION_START_AUTO_CAPTURE = "com.landsofjail.assistant.action.START_AUTO_CAPTURE"
        const val ACTION_STOP_AUTO_CAPTURE = "com.landsofjail.assistant.action.STOP_AUTO_CAPTURE"
        const val ACTION_AUTO_CAPTURE_CONSUMED = "com.landsofjail.assistant.action.AUTO_CAPTURE_CONSUMED"

        const val EXTRA_PACKAGE_FILTER = "extra_package_filter"
        const val EXTRA_ALWAYS_SHOW_FLOATING = "extra_always_show_floating"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_INTERVAL_MILLIS = "extra_interval_millis"

        private const val CHANNEL_MONITOR_ID = "loj_monitor_channel"
        private const val CHANNEL_PROMPT_ID = "loj_prompt_channel"
        private const val NOTIF_MONITOR_ID = 1001
        private const val NOTIF_ANALYZE_PROMPT_ID = 1002

        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()

        private val _detectedGameForeground = MutableStateFlow(false)
        val detectedGameForeground = _detectedGameForeground.asStateFlow()

        private val _isAutoCapturing = MutableStateFlow(false)
        val isAutoCapturing = _isAutoCapturing.asStateFlow()

        fun start(
            context: Context,
            packageFilter: String,
            alwaysShowFloating: Boolean = true
        ) {
            val intent = Intent(context, GameForegroundMonitorService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PACKAGE_FILTER, packageFilter)
                putExtra(EXTRA_ALWAYS_SHOW_FLOATING, alwaysShowFloating)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, GameForegroundMonitorService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun startAutoCapture(context: Context, resultCode: Int, resultData: Intent, intervalMillis: Long) {
            val intent = Intent(context, GameForegroundMonitorService::class.java).apply {
                action = ACTION_START_AUTO_CAPTURE
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
                putExtra(EXTRA_INTERVAL_MILLIS, intervalMillis)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopAutoCapture(context: Context) {
            context.startService(Intent(context, GameForegroundMonitorService::class.java).apply {
                action = ACTION_STOP_AUTO_CAPTURE
            })
        }

        fun notifyAutoCaptureConsumed(context: Context) {
            if (!_isAutoCapturing.value) return
            val intent = Intent(context, GameForegroundMonitorService::class.java).apply {
                action = ACTION_AUTO_CAPTURE_CONSUMED
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun runSingleCapture(context: Context, resultCode: Int, resultData: Intent) {
            val intent = Intent(context, GameForegroundMonitorService::class.java).apply {
                action = ACTION_CAPTURE_WITH_PROJECTION
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var monitorJob: Job? = null
    private var targetFilter: String = UsageStatsDetector.DEFAULT_GAME_PACKAGE_KEYWORD
    private var alwaysShowFloating: Boolean = true
    private var lastKnownForegroundPkg: String? = null
    private var isCapturingNow: Boolean = false
    @Volatile private var autoCaptureIntervalMillis: Long = 10_000L
    @Volatile private var autoCaptureAwaitingAnalysis: Boolean = false
    @Volatile private var nextAutoCaptureAtMillis: Long = 0L
    private var autoCaptureHandlerThread: HandlerThread? = null
    private var autoCaptureSurfaceAttached: Boolean = false
    private var autoCaptureOwnsMonitor: Boolean = false
    @Volatile private var lastAutoFrameSamples: IntArray? = null

    private var floatingContainer: LinearLayout? = null
    private var windowManager: WindowManager? = null

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopMonitoringAndSelf()
                return START_NOT_STICKY
            }
            ACTION_START_AUTO_CAPTURE -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    val interval = intent.getLongExtra(EXTRA_INTERVAL_MILLIS, 10_000L)
                    startContinuousCapture(resultCode, resultData, interval)
                }
                return START_STICKY
            }
            ACTION_STOP_AUTO_CAPTURE -> {
                stopAutoCaptureInternal()
                return if (_isRunning.value) START_STICKY else START_NOT_STICKY
            }
            ACTION_AUTO_CAPTURE_CONSUMED -> {
                if (_isAutoCapturing.value) {
                    autoCaptureAwaitingAnalysis = false
                    updateAutoCaptureSurface(_detectedGameForeground.value)
                }
                return START_STICKY
            }
            ACTION_CAPTURE_WITH_PROJECTION -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    promoteAndCaptureSingleFrame(resultCode, resultData)
                }
                return if (_isRunning.value) START_STICKY else START_NOT_STICKY
            }
            ACTION_START, null -> {
                targetFilter = intent?.getStringExtra(EXTRA_PACKAGE_FILTER)
                    ?: UsageStatsDetector.DEFAULT_GAME_PACKAGE_KEYWORD
                alwaysShowFloating = intent?.getBooleanExtra(EXTRA_ALWAYS_SHOW_FLOATING, true) ?: true

                if (!restoreMonitoringForegroundOrStop()) {
                    return START_NOT_STICKY
                }
                startPollingUsageStats()
                return START_STICKY
            }
        }
        return START_NOT_STICKY
    }

    private fun startAsMonitoringForeground() {
        val fgType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            NOTIF_MONITOR_ID,
            buildPersistentMonitoringNotification(),
            fgType
        )
    }

    private fun promoteAndCaptureSingleFrame(resultCode: Int, resultData: Intent) {
        val wasMonitoring = _isRunning.value
        isCapturingNow = true
        // Masquer la barre flottante pendant la capture pour obtenir un écran propre
        removeFloatingAnalyzeBar()

        // Obligatoire sur Android 14+ (API 34+) avant getMediaProjection()
        val projectionFgType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        } else {
            0
        }
        try {
            ServiceCompat.startForeground(
                this,
                NOTIF_MONITOR_ID,
                buildPersistentMonitoringNotification(),
                projectionFgType
            )
        } catch (_: Throwable) {
            isCapturingNow = false
            finishCaptureFlow(wasMonitoring)
            return
        }

        val projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = try {
            projectionManager.getMediaProjection(resultCode, resultData)
        } catch (_: Throwable) {
            null
        }

        if (projection == null) {
            isCapturingNow = false
            if (!wasMonitoring) stopSelf() else restoreMonitoringForegroundOrStop()
            return
        }

        mediaProjection = projection
        val mainHandler = Handler(Looper.getMainLooper())
        try {
            projection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    releaseProjection()
                }
            }, mainHandler)
        } catch (_: Throwable) {
            isCapturingNow = false
            releaseProjection()
            finishCaptureFlow(wasMonitoring)
            return
        }

        val metrics = resources.displayMetrics
        val width = metrics.widthPixels.coerceAtLeast(1)
        val height = metrics.heightPixels.coerceAtLeast(1)
        val density = metrics.densityDpi

        var handled = false
        val timeoutRunnable = Runnable {
            if (!handled) {
                handled = true
                isCapturingNow = false
                releaseProjection()
                finishCaptureFlow(wasMonitoring)
            }
        }
        mainHandler.postDelayed(timeoutRunnable, 3500L)

        // Attendre ~380ms que la fenêtre système d'autorisation se ferme complètement avant de capturer
        mainHandler.postDelayed({
            if (handled) return@postDelayed
            val activeProjection = mediaProjection ?: return@postDelayed

            val reader = try {
                ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3).also {
                    imageReader = it
                }
            } catch (_: Throwable) {
                handled = true
                mainHandler.removeCallbacks(timeoutRunnable)
                isCapturingNow = false
                releaseProjection()
                finishCaptureFlow(wasMonitoring)
                return@postDelayed
            }

            virtualDisplay = try {
                activeProjection.createVirtualDisplay(
                    "LandsOfJailSingleCapture",
                    width,
                    height,
                    density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    reader.surface,
                    null,
                    mainHandler
                )
            } catch (_: Throwable) {
                handled = true
                mainHandler.removeCallbacks(timeoutRunnable)
                isCapturingNow = false
                releaseProjection()
                finishCaptureFlow(wasMonitoring)
                return@postDelayed
            }

            var attempts = 0
            try {
                reader.setOnImageAvailableListener({ r ->
                if (handled) return@setOnImageAvailableListener
                val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
                attempts++
                var bitmapAwaitingTransfer: Bitmap? = null
                var sourceBitmapAwaitingRelease: Bitmap? = null
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
                    sourceBitmapAwaitingRelease = fullBitmap
                    fullBitmap.copyPixelsFromBuffer(buffer)
                    val cropped = cropFrameBitmap(fullBitmap, width, height)
                    sourceBitmapAwaitingRelease = null
                    bitmapAwaitingTransfer = cropped

                    // Si le tout premier frame est entièrement noir et qu'on a encore le temps, attendre le suivant
                    if (attempts == 1 && isBitmapMostlyBlank(cropped)) {
                        cropped.recycle()
                        bitmapAwaitingTransfer = null
                        return@setOnImageAvailableListener
                    }

                    handled = true
                    mainHandler.removeCallbacks(timeoutRunnable)

                    val jpegBytes = ByteArrayOutputStream().use { stream ->
                        check(cropped.compress(Bitmap.CompressFormat.JPEG, 88, stream)) {
                            "Échec de compression de la capture"
                        }
                        stream.toByteArray().also { bytes ->
                            check(bytes.isNotEmpty()) { "Capture JPEG vide" }
                        }
                    }
                    val base64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)

                    val savedUri = saveCaptureToInternalFile(jpegBytes)
                    check(savedUri.isNotBlank()) { "Impossible d'enregistrer durablement la capture" }

                    check(ScreenCaptureConsentActivity.publishCapture(
                        this@GameForegroundMonitorService,
                        CapturedScreenPayload(
                            bitmap = cropped,
                            base64Jpeg = base64,
                            capturedAtMillis = System.currentTimeMillis(),
                            imageFileUri = savedUri
                        )
                    )) { "Une capture précédente est encore en attente d'analyse" }
                    // Le bitmap est maintenant détenu par le payload affiché dans l'interface.
                    bitmapAwaitingTransfer = null
                } catch (_: Throwable) {
                    handled = true
                    mainHandler.removeCallbacks(timeoutRunnable)
                    bitmapAwaitingTransfer?.let { bitmap ->
                        if (!bitmap.isRecycled) bitmap.recycle()
                    }
                    bitmapAwaitingTransfer = null
                } finally {
                    sourceBitmapAwaitingRelease?.let { bitmap ->
                        if (!bitmap.isRecycled) bitmap.recycle()
                    }
                    bitmapAwaitingTransfer?.let { bitmap ->
                        if (!bitmap.isRecycled) bitmap.recycle()
                    }
                    // Une erreur de fermeture de l'Image ne doit pas bloquer
                    // la libération de MediaProjection ni la fin du flux.
                    runCatching { image.close() }
                    if (handled) {
                        isCapturingNow = false
                        releaseProjection()
                        finishCaptureFlow(wasMonitoring)
                    }
                }
                }, mainHandler)
            } catch (_: Throwable) {
                handled = true
                mainHandler.removeCallbacks(timeoutRunnable)
                isCapturingNow = false
                releaseProjection()
                finishCaptureFlow(wasMonitoring)
                return@postDelayed
            }
        }, 380L)
    }

    private fun startContinuousCapture(resultCode: Int, resultData: Intent, intervalMillis: Long) {
        if (_isAutoCapturing.value) return
        val wasMonitoring = _isRunning.value
        autoCaptureOwnsMonitor = !wasMonitoring
        autoCaptureIntervalMillis = intervalMillis.coerceIn(10_000L, 60_000L)
        autoCaptureAwaitingAnalysis = false
        lastAutoFrameSamples = null
        nextAutoCaptureAtMillis = System.currentTimeMillis() + 750L
        _isAutoCapturing.value = true
        removeFloatingAnalyzeBar()

        val projectionFgType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        } else {
            0
        }
        try {
            ServiceCompat.startForeground(
                this,
                NOTIF_MONITOR_ID,
                buildPersistentMonitoringNotification(),
                projectionFgType
            )
        } catch (_: Throwable) {
            _isAutoCapturing.value = false
            autoCaptureAwaitingAnalysis = false
            lastAutoFrameSamples = null
            if (autoCaptureOwnsMonitor) {
                stopSelf()
            } else {
                restoreMonitoringForegroundOrStop()
            }
            return
        }
        if (!wasMonitoring) startPollingUsageStats()

        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = try {
            projectionManager.getMediaProjection(resultCode, resultData)
        } catch (_: Throwable) {
            null
        }
        if (projection == null) {
            _isAutoCapturing.value = false
            if (autoCaptureOwnsMonitor) stopMonitoringAndSelf() else restoreMonitoringForegroundOrStop()
            return
        }

        mediaProjection = projection
        val mainHandler = Handler(Looper.getMainLooper())
        try {
            projection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    if (_isAutoCapturing.value) {
                        handleAutoProjectionStopped()
                    } else {
                        releaseProjection()
                    }
                }
            }, mainHandler)
        } catch (_: Throwable) {
            handleAutoProjectionStopped()
            return
        }

        val metrics = resources.displayMetrics
        val width = metrics.widthPixels.coerceAtLeast(1)
        val height = metrics.heightPixels.coerceAtLeast(1)
        val density = metrics.densityDpi
        val reader = try {
            ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3)
        } catch (_: Throwable) {
            handleAutoProjectionStopped()
            return
        }
        imageReader = reader
        try {
            virtualDisplay = projection.createVirtualDisplay(
                "LandsOfJailAutoCapture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                null,
                null,
                mainHandler
            )
            autoCaptureSurfaceAttached = false
        } catch (_: Throwable) {
            handleAutoProjectionStopped()
            return
        }

        val captureThread = try {
            HandlerThread("LojAutoCaptureFrames").apply { start() }
        } catch (_: Throwable) {
            handleAutoProjectionStopped()
            return
        }
        autoCaptureHandlerThread = captureThread
        try {
            reader.setOnImageAvailableListener({ imageReader ->
            val image = imageReader.acquireLatestImage() ?: return@setOnImageAvailableListener
            var bitmapAwaitingTransfer: Bitmap? = null
            var sourceBitmapAwaitingRelease: Bitmap? = null
            try {
                val now = System.currentTimeMillis()
                if (!_isAutoCapturing.value || autoCaptureAwaitingAnalysis ||
                    !_detectedGameForeground.value || now < nextAutoCaptureAtMillis
                ) return@setOnImageAvailableListener

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
                sourceBitmapAwaitingRelease = fullBitmap
                fullBitmap.copyPixelsFromBuffer(buffer)
                val cropped = cropFrameBitmap(fullBitmap, width, height)
                sourceBitmapAwaitingRelease = null
                bitmapAwaitingTransfer = cropped

                if (isBitmapMostlyBlank(cropped)) {
                    cropped.recycle()
                    bitmapAwaitingTransfer = null
                    nextAutoCaptureAtMillis = now + 1_000L
                    Handler(Looper.getMainLooper()).post { updateAutoCaptureSurface(_detectedGameForeground.value) }
                    return@setOnImageAvailableListener
                }
                if (!hasMeaningfulScreenChange(cropped)) {
                    cropped.recycle()
                    bitmapAwaitingTransfer = null
                    nextAutoCaptureAtMillis = now + autoCaptureIntervalMillis
                    Handler(Looper.getMainLooper()).post { updateAutoCaptureSurface(_detectedGameForeground.value) }
                    return@setOnImageAvailableListener
                }

                val jpegBytes = ByteArrayOutputStream().use { stream ->
                    check(cropped.compress(Bitmap.CompressFormat.JPEG, 88, stream)) {
                        "Échec de compression de la capture automatique"
                    }
                    stream.toByteArray().also { bytes ->
                        check(bytes.isNotEmpty()) { "Capture JPEG vide" }
                    }
                }
                val base64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
                val savedUri = saveCaptureToInternalFile(jpegBytes)
                check(savedUri.isNotBlank()) { "Impossible d'enregistrer durablement la capture automatique" }
                autoCaptureAwaitingAnalysis = true
                nextAutoCaptureAtMillis = now + autoCaptureIntervalMillis
                Handler(Looper.getMainLooper()).post { updateAutoCaptureSurface(_detectedGameForeground.value) }
                check(ScreenCaptureConsentActivity.publishCapture(
                    this@GameForegroundMonitorService,
                    CapturedScreenPayload(
                        bitmap = cropped,
                        base64Jpeg = base64,
                        capturedAtMillis = now,
                        imageFileUri = savedUri
                    )
                )) { "Une capture précédente est encore en attente d'analyse" }
                // Le bitmap appartient désormais au payload consommé par l'interface.
                bitmapAwaitingTransfer = null
            } catch (_: Throwable) {
                bitmapAwaitingTransfer?.let { bitmap ->
                    if (!bitmap.isRecycled) bitmap.recycle()
                }
                autoCaptureAwaitingAnalysis = false
                nextAutoCaptureAtMillis = System.currentTimeMillis() + autoCaptureIntervalMillis
                Handler(Looper.getMainLooper()).post { updateAutoCaptureSurface(_detectedGameForeground.value) }
            } finally {
                sourceBitmapAwaitingRelease?.let { bitmap ->
                    if (!bitmap.isRecycled) bitmap.recycle()
                }
                // Garder le callback de capture vivant même si Android signale
                // une erreur pendant la fermeture de l'image acquise.
                runCatching { image.close() }
            }
            }, Handler(captureThread.looper))
        } catch (_: Throwable) {
            handleAutoProjectionStopped()
            return
        }
        mainHandler.postDelayed({ updateAutoCaptureSurface(_detectedGameForeground.value) }, 750L)
    }

    private fun updateAutoCaptureSurface(gameForeground: Boolean) {
        val shouldAttach = _isAutoCapturing.value && gameForeground &&
            !autoCaptureAwaitingAnalysis && System.currentTimeMillis() >= nextAutoCaptureAtMillis
        if (shouldAttach == autoCaptureSurfaceAttached) return
        try {
            virtualDisplay?.setSurface(if (shouldAttach) imageReader?.surface else null)
            autoCaptureSurfaceAttached = shouldAttach
        } catch (_: Throwable) {
            // Une surface qui ne peut plus être modifiée rend la capture automatique
            // inutilisable : libérer la projection et restaurer/arrêter le service.
            handleAutoProjectionStopped()
        }
    }

    private fun hasMeaningfulScreenChange(bitmap: Bitmap): Boolean {
        val samples = IntArray(144)
        var index = 0
        for (row in 0 until 12) {
            for (column in 0 until 12) {
                val x = (column * bitmap.width / 12).coerceIn(0, bitmap.width - 1)
                val y = (row * bitmap.height / 12).coerceIn(0, bitmap.height - 1)
                val pixel = bitmap.getPixel(x, y)
                val red = (pixel shr 16) and 0xFF
                val green = (pixel shr 8) and 0xFF
                val blue = pixel and 0xFF
                samples[index++] = (red * 299 + green * 587 + blue * 114) / 1000
            }
        }
        val previous = lastAutoFrameSamples
        lastAutoFrameSamples = samples
        if (previous == null) return true
        var changed = 0
        for (i in samples.indices) {
            if (kotlin.math.abs(samples[i] - previous[i]) >= 22) changed++
        }
        return changed >= 4
    }

    private fun handleAutoProjectionStopped() {
        if (!_isAutoCapturing.value) {
            releaseProjection()
            return
        }
        _isAutoCapturing.value = false
        autoCaptureAwaitingAnalysis = false
        lastAutoFrameSamples = null
        releaseProjection()
        if (autoCaptureOwnsMonitor) stopMonitoringAndSelf() else restoreMonitoringForegroundOrStop()
    }

    private fun stopAutoCaptureInternal() {
        if (!_isAutoCapturing.value) return
        _isAutoCapturing.value = false
        autoCaptureAwaitingAnalysis = false
        lastAutoFrameSamples = null
        releaseProjection()
        if (autoCaptureOwnsMonitor) {
            stopMonitoringAndSelf()
        } else {
            restoreMonitoringForegroundOrStop()
        }
    }

    private fun restoreMonitoringForegroundOrStop(): Boolean {
        try {
            startAsMonitoringForeground()
            return true
        } catch (_: Throwable) {
            // Le service ne peut plus rester actif sans notification foreground.
            _isRunning.value = false
            _detectedGameForeground.value = false
            _isAutoCapturing.value = false
            isCapturingNow = false
            autoCaptureAwaitingAnalysis = false
            lastAutoFrameSamples = null
            monitorJob?.cancel()
            monitorJob = null
            runCatching { removeFloatingAnalyzeBar() }
            runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
            stopSelf()
            return false
        }
    }

    private fun finishCaptureFlow(wasMonitoring: Boolean) {
        if (wasMonitoring) {
            restoreMonitoringForegroundOrStop()
        } else {
            runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
            stopSelf()
        }
    }

    private fun isBitmapMostlyBlank(bitmap: Bitmap): Boolean {
        val samplePts = listOf(
            bitmap.width / 4 to bitmap.height / 4,
            bitmap.width / 2 to bitmap.height / 2,
            (bitmap.width * 3) / 4 to (bitmap.height * 3) / 4
        )
        var nonBlack = 0
        for ((px, py) in samplePts) {
            val c = bitmap.getPixel(px.coerceIn(0, bitmap.width - 1), py.coerceIn(0, bitmap.height - 1))
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            if (r + g + b > 24) nonBlack++
        }
        return nonBlack == 0
    }

    private fun saveCaptureToInternalFile(jpegBytes: ByteArray): String {
        val dir = File(filesDir, "captures")
        val timestamp = System.currentTimeMillis()
        val outFile = File(dir, "capture_$timestamp.jpg")
        val tempFile = File(dir, "capture_$timestamp.jpg.tmp")
        return try {
            if (!dir.exists() && !dir.mkdirs()) return ""
            // Supprimer les fichiers temporaires abandonnés depuis plus de 24 h
            // après un arrêt brutal, sans toucher aux captures JPG valides.
            val staleTempCutoff = System.currentTimeMillis() - 24L * 60L * 60L * 1000L
            dir.listFiles()
                ?.filter {
                    it.isFile && it.name.startsWith("capture_") &&
                        it.name.endsWith(".jpg.tmp") && it.lastModified() < staleTempCutoff
                }
                ?.forEach { it.delete() }
            // Écrire d'abord dans un fichier temporaire pour qu'un arrêt brutal
            // ne laisse jamais une capture partiellement écrite référencée comme valide.
            FileOutputStream(tempFile).use { fos ->
                fos.write(jpegBytes)
                fos.fd.sync()
            }
            if (!tempFile.renameTo(outFile)) {
                tempFile.delete()
                return ""
            }
            val uri = Uri.fromFile(outFile).toString()
            // La publication du payload enregistre aussi son URI de façon atomique.
            // Préserver le fichier de la capture actuellement en attente :
            // une nouvelle capture refusée ne doit pas rendre l'ancienne irrécupérable.
            val pendingUri = ScreenCaptureConsentActivity.pendingCapture.value?.imageFileUri
                ?.takeIf { it.isNotBlank() }
                ?: getSharedPreferences("loj_capture_state", Context.MODE_PRIVATE)
                    .getString("pending_capture_uri", null)
            val protectedPendingFile = pendingUri?.let { value ->
                runCatching { File(Uri.parse(value).path.orEmpty()).canonicalFile }
                    .getOrNull()
                    ?.takeIf { it.parentFile == dir.canonicalFile }
            }
            // Garder les 30 plus récentes, en plus de la capture en attente si elle est plus ancienne.
            dir.listFiles()?.filter { it.isFile && it.extension.equals("jpg", ignoreCase = true) }
                ?.filter { runCatching { it.canonicalFile != protectedPendingFile }.getOrDefault(true) }
                ?.sortedByDescending { it.lastModified() }?.drop(30)?.forEach { it.delete() }
            uri
        } catch (_: Throwable) {
            tempFile.delete()
            outFile.delete()
            ""
        }
    }

    private fun releaseProjection() {
        val projectionToStop = mediaProjection
        val displayToRelease = virtualDisplay
        val readerToClose = imageReader
        mediaProjection = null
        virtualDisplay = null
        imageReader = null
        try {
            displayToRelease?.release()
        } catch (_: Throwable) {
        }
        try {
            readerToClose?.close()
        } catch (_: Throwable) {
        }
        try {
            autoCaptureHandlerThread?.quitSafely()
        } catch (_: Throwable) {
        }
        autoCaptureHandlerThread = null
        autoCaptureSurfaceAttached = false
        try {
            projectionToStop?.stop()
        } catch (_: Throwable) {
        }
    }

    private fun startPollingUsageStats() {
        _isRunning.value = true
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            var wasGameInForeground = false
            while (isActive) {
                try {
                    val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                    val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
                    val deviceAvailable = powerManager.isInteractive && !keyguardManager.isKeyguardLocked
                    if (!deviceAvailable) lastKnownForegroundPkg = null

                    // Au démarrage, relire une journée d'événements pour retrouver un jeu
                    // déjà ouvert ; ensuite, une fenêtre courte suffit et évite de rescanner
                    // tout l'historique à chaque cycle.
                    val eventWindow = if (lastKnownForegroundPkg == null) {
                        24L * 60L * 60L * 1000L
                    } else {
                        120_000L
                    }
                    val fgPkg = withContext(Dispatchers.IO) {
                        UsageStatsDetector.getRecentForegroundPackage(applicationContext, eventWindow)
                    }
                    if (!fgPkg.isNullOrBlank() && deviceAvailable) {
                        lastKnownForegroundPkg = fgPkg
                    }
                    val effectivePkg = if (deviceAvailable) fgPkg ?: lastKnownForegroundPkg else null
                    val isSelfForeground = effectivePkg == applicationContext.packageName
                    val assistantVisible = MainActivity.isAppInForeground.value
                    val isGameNow = UsageStatsDetector.isLandsOfJailPackage(effectivePkg, targetFilter)
                    _detectedGameForeground.value = isGameNow
                    if (_isAutoCapturing.value) updateAutoCaptureSurface(isGameNow)

                    if (isGameNow && !wasGameInForeground && !_isAutoCapturing.value) {
                        showAnalyzePromptNotification()
                    }

                    // Ne jamais masquer le TopAppBar de notre propre appli avec la barre flottante,
                    // mais toujours la garder visible au-dessus du jeu / des autres apps.
                    if (_isAutoCapturing.value || isCapturingNow || assistantVisible || isSelfForeground) {
                        removeFloatingAnalyzeBar()
                    } else if (isGameNow || alwaysShowFloating) {
                        showFloatingAnalyzeBarIfAllowed()
                    } else {
                        removeFloatingAnalyzeBar()
                    }

                    wasGameInForeground = isGameNow
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    wasGameInForeground = false
                    _detectedGameForeground.value = false
                    if (_isAutoCapturing.value) {
                        runCatching { updateAutoCaptureSurface(false) }
                    }
                    runCatching { removeFloatingAnalyzeBar() }
                }
                delay(1800L)
            }
        }
    }

    /** Reads the same language preference as the Compose UI so system notifications and overlay controls match the app. */
    private fun serviceText(fr: String, en: String, de: String, tr: String, ru: String, zh: String): String {
        val languageCode = getSharedPreferences("lands_of_jail_account_v1", Context.MODE_PRIVATE)
            .getString("app_language_code", "fr")
        return when (languageCode) {
            "en" -> en
            "de" -> de
            "tr" -> tr
            "ru" -> ru
            "zh-CN" -> zh
            else -> fr
        }
    }

    private fun showAnalyzePromptNotification() {
        val analyzeIntent = Intent(this, ScreenCaptureConsentActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val analyzePendingIntent = PendingIntent.getActivity(
            this,
            201,
            analyzeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_PROMPT_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle(serviceText("Lands of Jail détecté au premier plan", "Land of Jail detected in foreground", "Land of Jail im Vordergrund erkannt", "Land of Jail ön planda algılandı", "Land of Jail обнаружена на переднем плане", "检测到 Land of Jail 正在前台运行"))
            .setContentText(serviceText("Appuyez sur « Analyser l’écran » pour lancer une capture ponctuelle autorisée.", "Tap “Analyze screen” to take an authorized one-time capture.", "Tippe auf „Bildschirm analysieren“, um eine erlaubte Einzelaufnahme zu erstellen.", "İzin verilen tek seferlik ekran görüntüsü için “Ekranı analiz et” seçeneğine dokun.", "Нажмите «Анализировать экран», чтобы выполнить разрешённый разовый снимок.", "点击“分析屏幕”以执行一次经授权的截图。"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(0, serviceText("Analyser l’écran", "Analyze screen", "Bildschirm analysieren", "Ekranı analiz et", "Анализировать экран", "分析屏幕"), analyzePendingIntent)
            .addAction(0, serviceText("Arrêter la détection", "Stop detection", "Erkennung stoppen", "Algılamayı durdur", "Остановить обнаружение", "停止检测"), buildStopPendingIntent())
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ANALYZE_PROMPT_ID, notification)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showFloatingAnalyzeBarIfAllowed() {
        if (!Settings.canDrawOverlays(this) || floatingContainer != null) return
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 180
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xEE121826.toInt())
            setPadding(12, 10, 12, 10)
        }

        val analyzeBtn = Button(this).apply {
            text = serviceText("Analyser l’écran", "Analyze screen", "Bildschirm analysieren", "Ekranı analiz et", "Анализировать экран", "分析屏幕")
            textSize = 12f
            isAllCaps = false
            setBackgroundColor(0xFFF59E0B.toInt())
            setTextColor(0xFF0B0F17.toInt())
            setPadding(24, 10, 24, 10)
        }

        val stopBtn = Button(this).apply {
            text = "✕"
            textSize = 12f
            setBackgroundColor(0xFF1E293B.toInt())
            setTextColor(0xFFF8FAFC.toInt())
            setPadding(18, 10, 18, 10)
            setOnClickListener {
                stopMonitoringAndSelf()
            }
        }

        val dragListener = object : View.OnTouchListener {
            private var startX = 0
            private var startY = 0
            private var touchStartX = 0f
            private var touchStartY = 0f
            private var moved = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = params.x
                        startY = params.y
                        touchStartX = event.rawX
                        touchStartY = event.rawY
                        moved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - touchStartX).toInt()
                        val dy = (event.rawY - touchStartY).toInt()
                        if (abs(dx) > 10 || abs(dy) > 10) {
                            moved = true
                            params.x = startX + dx
                            params.y = startY + dy
                            try {
                                windowManager?.updateViewLayout(container, params)
                            } catch (_: Throwable) {
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!moved && v === analyzeBtn) {
                            val captureIntent = Intent(
                                this@GameForegroundMonitorService,
                                ScreenCaptureConsentActivity::class.java
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(captureIntent)
                        }
                        return true
                    }
                }
                return false
            }
        }

        analyzeBtn.setOnTouchListener(dragListener)
        container.setOnTouchListener(dragListener)

        container.addView(analyzeBtn)
        container.addView(stopBtn)

        try {
            windowManager?.addView(container, params)
            floatingContainer = container
        } catch (_: Throwable) {
            floatingContainer = null
        }
    }

    private fun removeFloatingAnalyzeBar() {
        floatingContainer?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Throwable) {
            }
            floatingContainer = null
        }
    }

    private fun buildPersistentMonitoringNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            101,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val analyzeIntent = PendingIntent.getActivity(
            this,
            103,
            Intent(this, ScreenCaptureConsentActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_MONITOR_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
        if (_isAutoCapturing.value) {
            builder
                .setContentTitle(serviceText("Analyse automatique active", "Automatic analysis active", "Automatische Analyse aktiv", "Otomatik analiz etkin", "Автоматический анализ включён", "自动分析已启用"))
                .setContentText(serviceText("Intervalle cible ${autoCaptureIntervalMillis / 1000}s; une analyse à la fois, uniquement en jeu.", "Target interval ${autoCaptureIntervalMillis / 1000}s; one analysis at a time, only in-game.", "Zielintervall ${autoCaptureIntervalMillis / 1000}s; jeweils eine Analyse, nur im Spiel.", "Hedef aralık ${autoCaptureIntervalMillis / 1000}s; aynı anda tek analiz, yalnızca oyun içinde.", "Целевой интервал ${autoCaptureIntervalMillis / 1000} с; только один анализ за раз и только в игре.", "目标间隔 ${autoCaptureIntervalMillis / 1000} 秒；一次只分析一项，且仅在游戏中。"))
                .addAction(0, serviceText("Arrêter l’analyse auto", "Stop auto analysis", "Automatische Analyse stoppen", "Otomatik analizi durdur", "Остановить автоанализ", "停止自动分析"), buildStopAutoCapturePendingIntent())
                .addAction(0, serviceText("Arrêter la détection", "Stop detection", "Erkennung stoppen", "Algılamayı durdur", "Остановить обнаружение", "停止检测"), buildStopPendingIntent())
        } else {
            builder
                .setContentTitle(serviceText("Lands of Jail Assistant actif", "Land of Jail Assistant active", "Land of Jail Assistant aktiv", "Land of Jail Assistant etkin", "Land of Jail Assistant активен", "Land of Jail Assistant 已启用"))
                .setContentText(serviceText("Bouton prêt. Aucune capture sans votre accord.", "Button ready. No capture without your consent.", "Schaltfläche bereit. Keine Aufnahme ohne deine Zustimmung.", "Düğme hazır. Onayınız olmadan ekran görüntüsü alınmaz.", "Кнопка готова. Съёмка выполняется только с вашего разрешения.", "按钮已就绪。未经你同意不会进行截图。"))
                .addAction(0, serviceText("Analyser l’écran", "Analyze screen", "Bildschirm analysieren", "Ekranı analiz et", "Анализировать экран", "分析屏幕"), analyzeIntent)
                .addAction(0, serviceText("Arrêter la détection", "Stop detection", "Erkennung stoppen", "Algılamayı durdur", "Остановить обнаружение", "停止检测"), buildStopPendingIntent())
        }
        return builder.build()
    }

    private fun buildStopPendingIntent(): PendingIntent {
        val stopIntent = Intent(this, GameForegroundMonitorService::class.java).apply {
            action = ACTION_STOP
        }
        return PendingIntent.getService(
            this,
            102,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildStopAutoCapturePendingIntent(): PendingIntent {
        val intent = Intent(this, GameForegroundMonitorService::class.java).apply {
            action = ACTION_STOP_AUTO_CAPTURE
        }
        return PendingIntent.getService(
            this,
            104,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun stopMonitoringAndSelf() {
        _isAutoCapturing.value = false
        autoCaptureAwaitingAnalysis = false
        autoCaptureOwnsMonitor = false
        monitorJob?.cancel()
        runCatching { releaseProjection() }
        runCatching { removeFloatingAnalyzeBar() }
        _isRunning.value = false
        _detectedGameForeground.value = false
        runCatching {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIF_ANALYZE_PROMPT_ID)
        }
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        stopSelf()
    }

    override fun onDestroy() {
        _isAutoCapturing.value = false
        autoCaptureAwaitingAnalysis = false
        monitorJob?.cancel()
        runCatching { releaseProjection() }
        runCatching { removeFloatingAnalyzeBar() }
        _isRunning.value = false
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_MONITOR_ID,
                    serviceText("Détection du jeu", "Game detection", "Spielerkennung", "Oyun algılama", "Обнаружение игры", "游戏检测"),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_PROMPT_ID,
                    serviceText("Bouton Analyser l’écran", "Analyze screen button", "Schaltfläche „Bildschirm analysieren“", "Ekranı analiz et düğmesi", "Кнопка «Анализировать экран»", "分析屏幕按钮"),
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
    }
}
