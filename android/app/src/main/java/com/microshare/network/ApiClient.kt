package com.microshare.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.microshare.config.AppConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * OkHttp网络请求封装（单例）
 */
object ApiClient {
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val gson = Gson()
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun url(path: String) = "${AppConfig.BASE_URL}$path"

    /** 确保回调始终在主线程执行 */
    private fun onMain(callback: (String?) -> Unit): (String?) -> Unit = { result ->
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            callback(result)
        } else {
            mainHandler.post { callback(result) }
        }
    }

    // ---- GET 请求 ----
    fun get(path: String, callback: (String?) -> Unit) {
        val cb = onMain(callback)
        val request = Request.Builder().url(url(path)).get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { cb(null) }
            override fun onResponse(call: Call, response: Response) {
                cb(response.body?.string())
            }
        })
    }

    // ---- POST 请求（JSON） ----
    fun post(path: String, body: Any, callback: (String?) -> Unit) {
        val cb = onMain(callback)
        val json = gson.toJson(body)
        val requestBody = json.toRequestBody(JSON)
        val request = Request.Builder().url(url(path)).post(requestBody).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { cb(null) }
            override fun onResponse(call: Call, response: Response) {
                cb(response.body?.string())
            }
        })
    }

    // ---- PUT 请求（JSON） ----
    fun put(path: String, body: Any, callback: (String?) -> Unit) {
        val cb = onMain(callback)
        val json = gson.toJson(body)
        val requestBody = json.toRequestBody(JSON)
        val request = Request.Builder().url(url(path)).put(requestBody).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { cb(null) }
            override fun onResponse(call: Call, response: Response) {
                cb(response.body?.string())
            }
        })
    }

    // ---- DELETE 请求 ----
    fun delete(path: String, callback: (String?) -> Unit) {
        val cb = onMain(callback)
        val request = Request.Builder().url(url(path)).delete().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { cb(null) }
            override fun onResponse(call: Call, response: Response) {
                cb(response.body?.string())
            }
        })
    }

    fun createUrl(path: String) = url(path)

    // ---- 同步 GET（用于协程） ----
    fun getSync(path: String): String? {
        val request = Request.Builder().url(url(path)).get().build()
        return try {
            val response = client.newCall(request).execute()
            response.body?.string()
        } catch (_: Exception) { null }
    }

    // ---- 同步 POST（用于协程） ----
    fun postSync(path: String, body: Any): String? {
        val json = gson.toJson(body)
        val requestBody = json.toRequestBody(JSON)
        val request = Request.Builder().url(url(path)).post(requestBody).build()
        return try {
            val response = client.newCall(request).execute()
            response.body?.string()
        } catch (_: Exception) { null }
    }

    // ---- 文件上传（multipart/form-data） ----
    fun uploadImage(fileBytes: ByteArray, fileName: String, callback: (String?) -> Unit) {
        val cb = onMain(callback)
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName,
                fileBytes.toRequestBody("image/*".toMediaType()))
            .build()
        val request = Request.Builder().url(url("/api/upload")).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { cb(null) }
            override fun onResponse(call: Call, response: Response) {
                cb(response.body?.string())
            }
        })
    }
}

/**
 * 通用JSON解析
 */
inline fun <reified T> String?.parseJson(): T? {
    return try {
        Gson().fromJson(this, object : TypeToken<T>() {}.type)
    } catch (e: Exception) {
        null
    }
}

/**
 * 在UI线程执行回调的工具扩展函数
 */
fun runOnUiThread(action: () -> Unit) {
    android.os.Handler(android.os.Looper.getMainLooper()).post(action)
}
