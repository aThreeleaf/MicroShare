package com.microshare.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.microshare.model.ApiResponse
import com.microshare.model.Post
import com.microshare.network.ApiClient
import com.microshare.ui.navigation.Routes
import com.microshare.ui.theme.*
import com.microshare.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(navController: NavController) {
    var posts by remember { mutableStateOf(listOf<Post>()) }
    var loadError by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var likedPostIds by remember { mutableStateOf(setOf<Int>()) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // 数据加载函数
    suspend fun fetchPosts() {
        try {
            val result = withContext(Dispatchers.IO) {
                ApiClient.getSync("/api/posts?page=1")
            }
            if (result != null) {
                val resp = Gson().fromJson(result, ApiResponse::class.java)
                if (resp != null && resp.code == 200 && resp.data != null) {
                    val type = object : TypeToken<List<Post>>() {}.type
                    val list: List<Post> = Gson().fromJson(Gson().toJson(resp.data), type)
                        ?: emptyList()
                    posts = list
                    loadError = false
                    return
                }
            }
            loadError = true
        } catch (_: Exception) {
            loadError = true
        }
    }

    // 首次加载 + 刷新触发
    LaunchedEffect(refreshTrigger) {
        fetchPosts()
    }

    Column(Modifier.fillMaxSize().background(Background)) {
        // 顶部刷新按钮
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(onClick = { refreshTrigger++ }) {
                Text("↻ 点击刷新", fontSize = 13.sp, color = Primary)
            }
        }

        if (loadError && posts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("加载失败", color = TextHint, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { refreshTrigger++ }) {
                        Text("点击重试", color = Primary)
                    }
                }
            }
        } else if (posts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📭", fontSize = 48.sp)
                    Text("还没有帖子，快去发布第一条吧！", color = TextHint, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                items(posts, key = { it.id }) { post ->
                    val isLiked = likedPostIds.contains(post.id)
                    PostCard(
                        post = post,
                        isLiked = isLiked,
                        onClick = { navController.navigate(Routes.postDetail(post.id)) },
                        onLike = {
                            scope.launch {
                                try {
                                    val result = withContext(Dispatchers.IO) {
                                        ApiClient.postSync("/api/like", mapOf(
                                            "user_id" to TokenManager.getUserId(),
                                            "post_id" to post.id
                                        ))
                                    }
                                    if (result != null) {
                                        val resp = Gson().fromJson(result, ApiResponse::class.java)
                                        if (resp != null && resp.code == 200) {
                                            val liked = (resp.data as? Map<*, *>)?.get("liked") as? Boolean ?: false
                                            likedPostIds = if (liked) likedPostIds + post.id else likedPostIds - post.id
                                            val idx = posts.indexOfFirst { it.id == post.id }
                                            if (idx >= 0) {
                                                posts = posts.toMutableList().also {
                                                    it[idx] = it[idx].copy(
                                                        like_count = if (liked) post.like_count + 1
                                                        else (post.like_count - 1).coerceAtLeast(0)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                        },
                        onAvatarClick = { navController.navigate(Routes.userProfile(post.user_id)) }
                    )
                }
            }
        }
    }
}
