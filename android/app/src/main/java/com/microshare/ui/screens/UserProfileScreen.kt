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
import com.microshare.model.Post
import com.microshare.model.User
import com.microshare.network.ApiClient
import com.microshare.ui.navigation.Routes
import com.microshare.ui.theme.*
import com.microshare.utils.TokenManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(navController: NavController, userId: Int) {
    var user by remember { mutableStateOf<User?>(null) }
    var posts by remember { mutableStateOf(listOf<Post>()) }
    var favorites by remember { mutableStateOf(listOf<Post>()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isFollowing by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var deleteTargetPost by remember { mutableStateOf<Post?>(null) }
    val context = LocalContext.current
    val isOwnProfile = userId == TokenManager.getUserId()

    // 防止返回键崩溃
    BackHandler { navController.popBackStack() }

    LaunchedEffect(userId) {
        ApiClient.get("/api/user/$userId?viewer_id=${TokenManager.getUserId()}") { result ->
            try {
                val resp = Gson().fromJson(result, ApiResponse::class.java)
                if (resp.code == 200 && resp.data != null) {
                    user = Gson().fromJson(Gson().toJson(resp.data), User::class.java)
                    isFollowing = (resp.data as? Map<*, *>)?.get("is_following") as? Boolean ?: false
                }
            } catch (_: Exception) {}
        }
        ApiClient.get("/api/user/$userId/posts") { result ->
            try {
                val resp = Gson().fromJson(result, ApiResponse::class.java)
                if (resp.code == 200 && resp.data != null) {
                    val type = object : TypeToken<List<Post>>() {}.type
                    posts = Gson().fromJson(Gson().toJson(resp.data), type)
                }
            } catch (_: Exception) {}
        }
        ApiClient.get("/api/user/$userId/favorites") { result ->
            try {
                val resp = Gson().fromJson(result, ApiResponse::class.java)
                if (resp.code == 200 && resp.data != null) {
                    val type = object : TypeToken<List<Post>>() {}.type
                    favorites = Gson().fromJson(Gson().toJson(resp.data), type)
                }
            } catch (_: Exception) {}
        }
    }

    val tabs = listOf("帖子", "收藏")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(user?.nickname ?: "用户主页", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryDark, titleContentColor = White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(Background)) {
            Box(modifier = Modifier.fillMaxWidth().background(PrimaryLight.copy(alpha = 0.3f)).padding(20.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(64.dp).clip(CircleShape).background(Primary), contentAlignment = Alignment.Center) {
                        Text((user?.nickname?.firstOrNull()?.toString() ?: "U"), color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(user?.nickname ?: "", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    if (user?.bio?.isNotEmpty() == true) Text(user!!.bio, fontSize = 13.sp, color = TextHint)

                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("${user?.post_count ?: 0}", fontWeight = FontWeight.Bold, color = Primary)
                            Text("帖子", fontSize = 11.sp, color = TextHint)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { navController.navigate(Routes.followList(userId, "following")) }) {
                            Text("${user?.following_count ?: 0}", fontWeight = FontWeight.Bold, color = Primary)
                            Text("关注", fontSize = 11.sp, color = TextHint)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { navController.navigate(Routes.followList(userId, "followers")) }) {
                            Text("${user?.follower_count ?: 0}", fontWeight = FontWeight.Bold, color = Primary)
                            Text("粉丝", fontSize = 11.sp, color = TextHint)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    if (isOwnProfile) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { showLogoutDialog = true },
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.height(36.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Error)
                            ) { Text("退出登录", fontSize = 13.sp) }
                        }
                    } else {
                        Button(
                            onClick = {
                                ApiClient.post("/api/follow", mapOf("follower_id" to TokenManager.getUserId(), "followed_id" to userId)) { result ->
                                    try {
                                        val resp = Gson().fromJson(result, ApiResponse::class.java)
                                        if (resp.code == 200) isFollowing = !isFollowing
                                        Toast.makeText(context, resp.msg, Toast.LENGTH_SHORT).show()
                                    } catch (_: Exception) {}
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isFollowing) TextHint else Primary),
                            shape = RoundedCornerShape(18.dp), modifier = Modifier.height(36.dp)
                        ) { Text(if (isFollowing) "已关注" else "+ 关注", color = White, fontSize = 13.sp) }
                    }
                }
            }

            TabRow(selectedTabIndex = selectedTab, containerColor = Surface, contentColor = Primary) {
                tabs.forEachIndexed { i, t -> Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(t) }) }
            }

            LazyColumn(Modifier.fillMaxSize()) {
                val list = if (selectedTab == 0) posts else favorites
                if (list.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(60.dp), contentAlignment = Alignment.Center) {
                            Text(
                                if (selectedTab == 0) "还没有发布帖子" else "还没有收藏帖子",
                                color = TextHint, fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(list) { p ->
                        Box {
                            PostCard(post = p, onClick = { navController.navigate(Routes.postDetail(p.id)) }, onAvatarClick = {}, onLike = {})
                            // 自己的帖子显示删除按钮
                            if (isOwnProfile && selectedTab == 0) {
                                IconButton(
                                    onClick = { deleteTargetPost = p },
                                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(32.dp)
                                ) {
                                    Text("🗑", fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 退出登录确认对话框
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("确定要退出当前账号吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    TokenManager.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }) { Text("确定", color = Error) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("取消") }
            }
        )
    }

    // 删除帖子确认对话框
    if (deleteTargetPost != null) {
        val target = deleteTargetPost!!
        AlertDialog(
            onDismissRequest = { deleteTargetPost = null },
            title = { Text("删除帖子") },
            text = { Text("确定要删除这条帖子吗？删除后无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    val pid = target.id
                    deleteTargetPost = null
                    ApiClient.delete("/api/post/$pid") { result ->
                        try {
                            val resp = Gson().fromJson(result, ApiResponse::class.java)
                            if (resp.code == 200) {
                                posts = posts.filter { it.id != pid }
                                Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, resp.msg, Toast.LENGTH_SHORT).show()
                            }
                        } catch (_: Exception) {
                            Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text("确定", color = Error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetPost = null }) { Text("取消") }
            }
        )
    }
}
