package com.microshare.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * 加速度传感器计步检测器
 * 使用峰值检测算法：检测加速度向量幅值的波峰，过滤无效抖动
 */
class StepDetector : SensorEventListener {

    private var stepCount = 0
    private var lastMagnitude = 0f
    private var lastPeakTime = 0L
    private var onStepListener: ((Int) -> Unit)? = null

    // 阈值常量
    private val ACCEL_THRESHOLD = 10.5f     // 加速度幅值阈值（步行动作触发点）
    private val PEAK_MIN_INTERVAL = 300L     // 两步间最小间隔(ms)，防止重复计数
    private val PEAK_MAX_INTERVAL = 2000L    // 两步间最大间隔(ms)，超时视为停止

    // 平滑滤波窗口
    private val filterWindow = mutableListOf<Float>()
    private val WINDOW_SIZE = 5

    fun setOnStepListener(listener: (Int) -> Unit) {
        onStepListener = listener
    }

    fun getStepCount() = stepCount

    fun resetSteps() {
        stepCount = 0
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // 计算加速度向量幅值（去除重力影响）
        val magnitude = sqrt(x * x + y * y + z * z)

        // 平滑滤波
        filterWindow.add(magnitude)
        if (filterWindow.size > WINDOW_SIZE) filterWindow.removeAt(0)
        val smoothed = filterWindow.average().toFloat()

        val now = System.currentTimeMillis()
        val timeSinceLastPeak = now - lastPeakTime

        // 峰值检测
        if (smoothed > ACCEL_THRESHOLD && timeSinceLastPeak > PEAK_MIN_INTERVAL) {
            stepCount++
            lastPeakTime = now
            lastMagnitude = smoothed
            onStepListener?.invoke(stepCount)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
