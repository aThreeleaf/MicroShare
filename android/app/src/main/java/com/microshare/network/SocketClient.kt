package com.microshare.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.microshare.config.AppConfig
import com.microshare.model.Notification
import com.microshare.utils.TokenManager
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket

/**
 * Socket长连接客户端
 */
class SocketClient {

    private var socket: Socket? = null
    private var writer: OutputStreamWriter? = null
    private var reader: BufferedReader? = null
    private var running = false
    private var notificationListener: ((Notification) -> Unit)? = null
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "SocketClient"
        /** 通知累积队列，供 MessageActivity 读取 */
        val notificationQueue = mutableListOf<Notification>()
        private const val MAX_QUEUE_SIZE = 100
    }

    fun setOnNotificationListener(listener: (Notification) -> Unit) {
        notificationListener = listener
    }

    fun connect() {
        if (running) return
        running = true

        Thread {
            try {
                socket = Socket(AppConfig.SOCKET_HOST, AppConfig.SOCKET_PORT)
                socket?.soTimeout = 0
                writer = OutputStreamWriter(socket!!.getOutputStream(), "UTF-8")
                reader = BufferedReader(InputStreamReader(socket!!.getInputStream(), "UTF-8"))

                // 注册用户ID
                val registerMsg = JSONObject().apply {
                    put("type", "register")
                    put("user_id", TokenManager.getUserId())
                }
                send(registerMsg.toString())

                // 启动心跳线程
                startHeartbeat()

                // 读取服务端消息
                var line: String?
                while (running && reader != null) {
                    line = reader?.readLine() ?: break
                    Log.d(TAG, "收到消息: $line")
                    try {
                        val json = JSONObject(line)
                        val type = json.optString("type", "")
                        val msg = json.optString("msg", "")
                        val uid = json.optInt("user_id", 0)
                        val pid = json.optInt("post_id", 0)
                        val notification = Notification(type, msg, uid, pid)
                        // 累积到队列
                        synchronized(notificationQueue) {
                            if (notificationQueue.size >= MAX_QUEUE_SIZE) {
                                notificationQueue.removeAt(0)
                            }
                            notificationQueue.add(notification)
                        }
                        handler.post {
                            notificationListener?.invoke(notification)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "解析消息失败: $e")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Socket连接失败: $e")
            } finally {
                disconnect()
                // 自动重连
                handler.postDelayed({ connect() }, 5000)
            }
        }.start()
    }

    fun send(message: String) {
        try {
            writer?.write(message + "\n")
            writer?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "发送消息失败: $e")
        }
    }

    private fun startHeartbeat() {
        Thread {
            while (running) {
                Thread.sleep(AppConfig.HEARTBEAT_INTERVAL)
                val heartbeat = JSONObject().apply {
                    put("type", "heartbeat")
                }
                send(heartbeat.toString())
            }
        }.start()
    }

    fun disconnect() {
        running = false
        try { reader?.close() } catch (_: Exception) {}
        try { writer?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        reader = null
        writer = null
        socket = null
    }
}
