package com.microshare.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.microshare.model.ApiResponse
import com.microshare.network.ApiClient
import com.microshare.network.parseJson
import com.microshare.ui.theme.*
import com.microshare.utils.TokenManager

@Composable
fun LoginScreen(navController: NavController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🌿", fontSize = 48.sp)
            Text("微分享社区", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Primary)
            Spacer(Modifier.height(8.dp))
            Text("登录你的账号", fontSize = 14.sp, color = TextHint)
            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = username, onValueChange = { username = it },
                label = { Text("用户名") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("密码") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (username.isEmpty() || password.isEmpty()) return@Button
                    loading = true
                    ApiClient.post("/api/login", mapOf("username" to username, "password" to password)) { result ->
                        loading = false
                        try {
                            val resp = result.parseJson<ApiResponse<Map<String, Any>>>()
                            if (resp?.code == 200 && resp.data != null) {
                                val d = resp.data!!
                                val uid = when (val v = d["user_id"]) {
                                    is Double -> v.toInt()
                                    is Int -> v
                                    is Long -> v.toInt()
                                    else -> 0
                                }
                                TokenManager.saveLogin(
                                    uid, d["token"] as? String ?: "",
                                    d["nickname"] as? String ?: "", d["avatar"] as? String ?: ""
                                )
                                navController.navigate("main") { popUpTo("login") { inclusive = true } }
                            } else {
                                Toast.makeText(context, resp?.msg ?: "登录失败", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "登录失败: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !loading,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp)
            ) { Text(if (loading) "登录中..." else "登录", color = White, fontSize = 16.sp) }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { navController.navigate("register") }) {
                Text("没有账号？立即注册", color = Primary)
            }
        }
    }
}
