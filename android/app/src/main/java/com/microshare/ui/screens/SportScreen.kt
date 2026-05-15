package com.microshare.ui.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.microshare.model.ApiResponse
import com.microshare.model.SportData
import com.microshare.network.ApiClient
import com.microshare.ui.navigation.Routes
import com.microshare.service.StepService
import com.microshare.ui.theme.*
import com.microshare.utils.TokenManager
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportScreen(navController: NavController) {
    var steps by remember { mutableIntStateOf(0) }
    var ranks by remember { mutableStateOf(listOf<SportData>()) }
    var loading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    BackHandler { navController.popBackStack() }

    DisposableEffect(Unit) {
        val conn = object : ServiceConnection {
            var svc: StepService? = null
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                svc = (service as StepService.StepBinder).getService()
                svc?.setOnStepUpdateListener { steps = it }
                steps = svc?.getCurrentSteps() ?: 0
            }
            override fun onServiceDisconnected(name: ComponentName?) { svc = null }
        }
        context.bindService(Intent(context, StepService::class.java), conn, Context.BIND_AUTO_CREATE)
        onDispose { try { context.unbindService(conn) } catch (_: Exception) {} }
    }

    LaunchedEffect(Unit) {
        ApiClient.get("/api/sport/rank") { result ->
            try {
                if (result != null) {
                    val json = JSONObject(result)
                    if (json.optInt("code") == 200) {
                        val arr = json.optJSONArray("data") ?: JSONArray()
                        ranks = (0 until arr.length()).mapNotNull { i ->
                            val obj = arr.getJSONObject(i)
                            SportData(
                                id = obj.optInt("id", 0),
                                user_id = obj.optInt("user_id", 0),
                                steps = obj.optInt("steps", 0),
                                distance = obj.optDouble("distance", 0.0).toFloat(),
                                calories = obj.optDouble("calories", 0.0).toFloat(),
                                record_date = obj.optString("record_date", ""),
                                nickname = obj.optString("nickname", ""),
                                avatar = obj.optString("avatar", "")
                            )
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("运动数据", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = White)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(Background)) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Primary)
            ) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("今日步数", fontSize = 13.sp, color = White.copy(alpha = 0.85f))
                    Text("$steps", fontSize = 52.sp, fontWeight = FontWeight.Bold, color = White)
                    Text("步", fontSize = 14.sp, color = White.copy(alpha = 0.7f))

                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(String.format("%.2f", steps * 0.0007f), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = White)
                            Text("公里", fontSize = 11.sp, color = White.copy(alpha = 0.7f))
                        }
                        Box(Modifier.width(1.dp).height(36.dp).background(White.copy(alpha = 0.2f)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("${(steps * 0.04f).toInt()}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = White)
                            Text("千卡", fontSize = 11.sp, color = White.copy(alpha = 0.7f))
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = {
                            if (steps <= 0) return@Button
                            loading = true
                            ApiClient.post("/api/sport/upload", mapOf(
                                "user_id" to TokenManager.getUserId(), "steps" to steps,
                                "distance" to steps * 0.0007f, "calories" to steps * 0.04f
                            )) { result ->
                                loading = false
                                try {
                                    if (result != null) {
                                        val json = JSONObject(result)
                                        val msg = json.optString("msg", "上传完成")
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "上传失败，请检查网络", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (_: Exception) {
                                    Toast.makeText(context, "上传失败", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !loading,
                        colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Primary),
                        shape = RoundedCornerShape(20.dp)
                    ) { Text(if (loading) "上传中..." else "上传数据", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                }
            }

            Text("🏆 步数排行榜", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            LazyColumn(Modifier.fillMaxSize()) {
                items(ranks) { item ->
                    val rank = ranks.indexOf(item) + 1
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                when (rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "$rank" },
                                fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp)
                            )
                            Box(
                                Modifier.size(36.dp).clip(CircleShape).background(PrimaryLight)
                                    .clickableSingle { navController.navigate(Routes.userProfile(item.user_id)) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text((item.nickname.firstOrNull()?.toString() ?: "U"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = White)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(
                                Modifier.weight(1f)
                                    .clickableSingle { navController.navigate(Routes.userProfile(item.user_id)) }
                            ) {
                                Text(item.nickname, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                Text("${String.format("%.2f", item.distance)}km · ${item.calories.toInt()}kcal", fontSize = 11.sp, color = TextHint)
                            }
                            Text("${item.steps}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Primary)
                            Text("步", fontSize = 11.sp, color = TextHint, modifier = Modifier.align(Alignment.Bottom))
                        }
                    }
                }
            }
        }
    }
}
