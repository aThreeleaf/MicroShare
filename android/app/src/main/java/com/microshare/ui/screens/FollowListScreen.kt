package com.microshare.ui.screens

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
import com.microshare.model.User
import com.microshare.network.ApiClient
import com.microshare.ui.navigation.Routes
import com.microshare.ui.theme.*
import com.microshare.utils.TokenManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowListScreen(navController: NavController, userId: Int, tab: String) {
    var users by remember { mutableStateOf(listOf<User>()) }
    var followingSet by remember { mutableStateOf(setOf<Int>()) }
    val context = LocalContext.current
    val title = if (tab == "followers") "粉丝列表" else "关注列表"

    BackHandler { navController.popBackStack() }

    LaunchedEffect(userId, tab) {
        val path = if (tab == "followers") "/api/user/$userId/followers" else "/api/user/$userId/following"
        ApiClient.get(path) { result ->
            try {
                val resp = Gson().fromJson(result, ApiResponse::class.java)
                if (resp.code == 200 && resp.data != null) {
                    val type = object : TypeToken<List<User>>() {}.type
                    users = Gson().fromJson(Gson().toJson(resp.data), type)
                }
            } catch (_: Exception) {}
        }
        // 查询当前登录用户关注的人
        ApiClient.get("/api/user/${TokenManager.getUserId()}/following") { result ->
            try {
                val resp = Gson().fromJson(result, ApiResponse::class.java)
                if (resp.code == 200 && resp.data != null) {
                    val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                    val list: List<Map<String, Any>> = Gson().fromJson(Gson().toJson(resp.data), type)
                    followingSet = list.mapNotNull {
                        when (val v = it["id"]) {
                            is Double -> v.toInt()
                            is Int -> v
                            is Long -> v.toInt()
                            else -> null
                        }
                    }.toSet()
                }
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = White)
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(users) { user ->
                val isFollowing = followingSet.contains(user.id)
                Surface(modifier = Modifier.fillMaxWidth(), color = Surface) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(PrimaryLight)
                                .clickableSingle { navController.navigate(Routes.userProfile(user.id)) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text((user.nickname.firstOrNull()?.toString() ?: "U"), color = White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(
                            Modifier.weight(1f)
                                .clickableSingle { navController.navigate(Routes.userProfile(user.id)) }
                        ) {
                            Text(user.nickname, fontWeight = FontWeight.Bold, color = TextPrimary)
                            if (user.bio.isNotEmpty()) Text(user.bio, fontSize = 12.sp, color = TextHint)
                        }
                        if (user.id != TokenManager.getUserId()) {
                            Button(
                                onClick = {
                                    ApiClient.post("/api/follow", mapOf("follower_id" to TokenManager.getUserId(), "followed_id" to user.id)) { result ->
                                        try {
                                            val resp = Gson().fromJson(result, ApiResponse::class.java)
                                            val followed = (resp.data as? Map<*, *>)?.get("followed") as? Boolean ?: false
                                            followingSet = if (followed) followingSet + user.id else followingSet - user.id
                                            Toast.makeText(context, resp.msg, Toast.LENGTH_SHORT).show()
                                        } catch (_: Exception) {}
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (isFollowing) TextHint else Primary)
                            ) { Text(if (isFollowing) "已关注" else "关注", color = White, fontSize = 13.sp) }
                        }
                    }
                }
                HorizontalDivider(color = Divider, thickness = 0.5.dp)
            }
        }
    }
}
