package com.landsofjail.assistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import androidx.core.app.NotificationCompat
import com.landsofjail.assistant.MainActivity
import com.landsofjail.assistant.capture.ScreenCaptureConsentActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameForegroundMonitorService : Service() {

    companion object {
        const val ACTION_START = "com.landsofjail.assistant.action.START_MONITORING"
        const val ACTION_STOP = "com.landsofjail.assistant.action.STOP_MONITORING"
        const val EXTRA_PACKAGE_FILTER = "extra_package_filter"

        private const val CHANNEL_MONITOR_ID = "loj_monitor_channel"
        private const val CHANNEL_PROMPT_ID = "loj_prompt_channel"
        private const val NOTIF_MONITOR_ID = 1001
        private const val NOTIF_ANALYZE_PROMPT_ID = 1002

        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()

        private val _detectedGameForeground = MutableStateFlow(false)
        val detectedGameForeground = _detectedGameForeground.asStateFlow()

        fun start(context: Context, packageFilter: String) {
            val intent = Intent(context, GameForegroundMonitorService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PACKAGE_FILTER, packageFilter)
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
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var monitorJob: Job? = null
    private var targetFilter: String = UsageStatsDetector.DEFAULT_GAME_PACKAGE_KEYWORD
    private var floatingButton: Button? = null
    private var windowManager: WindowManager? = null

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
            ACTION_START, null -> {
                targetFilter = intent?.getStringExtra(EXTRA_PACKAGE_FILTER)
                    ?: UsageStatsDetector.DEFAULT_GAME_PACKAGE_KEYWORD
                startForeground(NOTIF_MONITOR_ID, buildPersistentMonitoringNotification())
                startPollingUsageStats()
                return START_STICKY
            }
        }
        return START_NOT_STICKY
    }

    private fun startPollingUsageStats() {
        _isRunning.value = true
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            var wasGameInForeground = false
            while (isActive) {
                val fgPkg = withContext(Dispatchers.IO) {
                    UsageStatsDetector.getRecentForegroundPackage(applicationContext)
                }
                val isGameNow = UsageStatsDetector.isLandsOfJailPackage(fgPkg, targetFilter)
                _detectedGameForeground.value = isGameNow

                if (isGameNow && !wasGameInForeground) {
                    // Le jeu vient de passer au premier plan : on propose le bouton/notification « Analyser l’écran »
                    // Sans jamais capturer l'écran automatiquement !
                    showAnalyzePromptNotification()
                    showFloatingAnalyzeButtonIfAllowed()
                } else if (!isGameNow && wasGameInForeground) {
                    removeFloatingAnalyzeButton()
                }
                wasGameInForeground = isGameNow
                delay(2500L)
            }
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

        val stopPendingIntent = buildStopPendingIntent()

        val notification = NotificationCompat.Builder(this, CHANNEL_PROMPT_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Lands of Jail détecté au premier plan")
            .setContentText("Appuyez sur « Analyser l’écran » pour lancer une capture ponctuelle autorisée.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(0, "Analyser l’écran", analyzePendingIntent)
            .addAction(0, "Arrêter la détection", stopPendingIntent)
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ANALYZE_PROMPT_ID, notification)
    }

    private fun showFloatingAnalyzeButtonIfAllowed() {
        if (!Settings.canDrawOverlays(this) || floatingButton != null) return
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 24
            y = 180
        }

        val btn = Button(this).apply {
            text = "Analyser l’écran"
            setBackgroundColor(0xFFF59E0B.toInt())
            setTextColor(0xFF0B0F17.toInt())
            setPadding(32, 16, 32, 16)
            setOnClickListener {
                val intent = Intent(this@GameForegroundMonitorService, ScreenCaptureConsentActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
        }
        try {
            windowManager?.addView(btn, params)
            floatingButton = btn
        } catch (_: Throwable) {
            floatingButton = null
        }
    }

    private fun removeFloatingAnalyzeButton() {
        floatingButton?.let { btn ->
            try {
                windowManager?.removeView(btn)
            } catch (_: Throwable) {
            }
            floatingButton = null
        }
    }

    private fun buildPersistentMonitoringNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            101,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_MONITOR_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Détection Lands of Jail active")
            .setContentText("En attente du lancement du jeu. Aucune capture sans votre accord.")
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .addAction(0, "Arrêter la détection", buildStopPendingIntent())
            .build()
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

    private fun stopMonitoringAndSelf() {
        monitorJob?.cancel()
        removeFloatingAnalyzeButton()
        _isRunning.value = false
        _detectedGameForeground.value = false
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIF_ANALYZE_PROMPT_ID)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        removeFloatingAnalyzeButton()
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
                    "Détection du jeu",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_PROMPT_ID,
                    "Bouton Analyser l’écran",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
    }
}
