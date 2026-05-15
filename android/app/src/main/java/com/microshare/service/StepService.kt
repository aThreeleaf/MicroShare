package com.microshare.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.SensorManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.microshare.sensor.StepDetector

/**
 * 计步前台服务 - 使用加速度传感器实时计步
 */
class StepService : Service() {

    private val binder = StepBinder()
    private lateinit var sensorManager: SensorManager
    private lateinit var stepDetector: StepDetector
    private var stepCount = 0
    private var onStepUpdate: ((Int) -> Unit)? = null

    inner class StepBinder : Binder() {
        fun getService(): StepService = this@StepService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        try {
            stepDetector = StepDetector()
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val accelerometer = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
            if (accelerometer != null) {
                sensorManager.registerListener(
                    stepDetector, accelerometer, SensorManager.SENSOR_DELAY_NORMAL
                )
            }
            stepDetector.setOnStepListener { steps ->
                stepCount = steps
                onStepUpdate?.invoke(steps)
            }
        } catch (e: Exception) {
            Log.e("StepService", "onCreate error: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            startForeground()
        } catch (e: Exception) {
            Log.e("StepService", "startForeground failed: ${e.message}")
        }
        return START_STICKY
    }

    private fun startForeground() {
        val channelId = "step_channel"
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId, "计步服务", NotificationManager.IMPORTANCE_LOW
                )
                val manager = getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(channel)
            }
        } catch (_: Exception) {}

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("微分享社区")
            .setContentText("计步服务运行中...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    fun getCurrentSteps() = stepCount

    fun setOnStepUpdateListener(listener: (Int) -> Unit) {
        onStepUpdate = listener
    }

    override fun onDestroy() {
        try {
            sensorManager.unregisterListener(stepDetector)
        } catch (_: Exception) {}
        super.onDestroy()
    }
}
