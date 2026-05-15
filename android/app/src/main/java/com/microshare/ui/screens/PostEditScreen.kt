package com.microshare.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.gson.Gson
import com.microshare.model.ApiResponse
import com.microshare.network.ApiClient
import com.microshare.ui.theme.*
import com.microshare.utils.TokenManager
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostEditScreen(navController: NavController) {
    var content by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
    }

    BackHandler { navController.popBackStack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("发布帖子", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(Background)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = content, onValueChange = { content = it },
                placeholder = { Text("分享你的想法...", color = TextHint) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = topic, onValueChange = { topic = it },
                placeholder = { Text("添加话题标签（选填）", color = TextHint) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { imagePicker.launch("image/*") },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
            ) {
                Icon(Icons.Filled.Image, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("添加图片")
            }

            imageUri?.let {
                Spacer(Modifier.height(8.dp))
                Text("已选择图片: ${it.lastPathSegment}", fontSize = 12.sp, color = TextHint)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (content.isBlank()) return@Button
                    loading = true
                    // 辅助函数：发布帖子
                    fun doPost(images: List<String>) {
                        val body = mutableMapOf<String, Any>(
                            "user_id" to TokenManager.getUserId(),
                            "content" to content
                        )
                        if (topic.isNotBlank()) body["topic"] = topic
                        if (images.isNotEmpty()) body["images"] = images
                        ApiClient.post("/api/post", body) { result ->
                            loading = false
                            try {
                                val resp = Gson().fromJson(result, ApiResponse::class.java)
                                Toast.makeText(context, resp.msg, Toast.LENGTH_SHORT).show()
                                if (resp.code == 200) navController.popBackStack()
                            } catch (_: Exception) {
                                Toast.makeText(context, "发布失败，请重试", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    // 有图片先上传
                    val uri = imageUri
                    if (uri != null) {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        if (inputStream != null) {
                            val bytes = inputStream.readBytes()
                            inputStream.close()
                            val fileName = uri.lastPathSegment ?: "image.jpg"
                            ApiClient.uploadImage(bytes, fileName) { result ->
                                if (result != null) {
                                    try {
                                        val json = JSONObject(result)
                                        if (json.optInt("code", -1) == 200) {
                                            val dataObj = json.optJSONObject("data")
                                            val url = dataObj?.optString("url", "") ?: ""
                                            doPost(if (url.isNotEmpty()) listOf(url) else emptyList())
                                        } else {
                                            loading = false
                                            Toast.makeText(context, "图片上传失败: ${json.optString("msg", "")}", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (_: Exception) {
                                        loading = false
                                        Toast.makeText(context, "图片上传失败，请重试", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    loading = false
                                    Toast.makeText(context, "图片上传失败，请检查网络", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            loading = false
                            Toast.makeText(context, "无法读取图片文件", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        doPost(emptyList())
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !loading,
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (loading) "发布中..." else "发布",
                    color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
