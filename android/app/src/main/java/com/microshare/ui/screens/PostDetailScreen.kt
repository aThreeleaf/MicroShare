package com.microshare.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.microshare.model.ApiResponse
import com.microshare.model.Comment
import com.microshare.model.Post
import com.microshare.network.ApiClient
import com.microshare.ui.navigation.Routes
import com.microshare.ui.theme.*
import com.microshare.utils.TokenManager
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(navController: NavController, postId: Int) {
    var post by remember { mutableStateOf<Post?>(null) }
    var comments by remember { mutableStateOf(listOf<Comment>()) }
    var commentText by remember { mutableStateOf("") }
    var commentSending by remember { mutableStateOf(false) }
    var liked by remember { mutableStateOf(false) }
    var favorited by remember { mutableStateOf(false) }
    var localLikeCount by remember { mutableIntStateOf(0) }
    var localFavCount by remember { mutableIntStateOf(0) }
    var refreshing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    BackHandler { navController.popBackStack() }

    fun reloadComments() {
        ApiClient.get("/api/post/$postId/comments") { result ->
            try {
                val resp = Gson().fromJson(result, ApiResponse::class.java)
                if (resp.code == 200 && resp.data != null) {
                    val type = object : TypeToken<List<Comment>>() {}.type
                    comments = Gson().fromJson(Gson().toJson(resp.data), type)
                }
            } catch (_: Exception) {}
        }
    }

    fun reloadPost() {
        refreshing = true
        ApiClient.get("/api/post/$postId?viewer_id=${TokenManager.getUserId()}") { result ->
            try {
                val resp = Gson().fromJson(result, ApiResponse::class.java)
                if (resp.code == 200 && resp.data != null) {
                    val data = resp.data as Map<*, *>
                    post = Gson().fromJson(Gson().toJson(resp.data), Post::class.java)
                    liked = data["is_liked"] as? Boolean ?: false
                    favorited = data["is_favorited"] as? Boolean ?: false
                    localLikeCount = (data["like_count"] as? Double)?.toInt() ?: 0
                    localFavCount = (data["favorite_count"] as? Double)?.toInt() ?: 0
                }
            } catch (_: Exception) {}
            reloadComments()
            refreshing = false
        }
    }

    LaunchedEffect(postId) { reloadPost() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("帖子详情", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = White)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 4.dp) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = commentText, onValueChange = { commentText = it },
                        placeholder = { Text("写下你的评论...", color = TextHint) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary),
                        maxLines = 2
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (commentText.isBlank() || commentSending) return@IconButton
                            commentSending = true
                            ApiClient.post("/api/comment", mapOf(
                                "user_id" to TokenManager.getUserId(), "post_id" to postId,
                                "content" to commentText, "parent_id" to 0
                            )) { result ->
                                commentSending = false
                                try {
                                    if (result == null) {
                                        Toast.makeText(context, "网络异常，评论可能已发送", Toast.LENGTH_SHORT).show()
                                        reloadComments()
                                    } else {
                                    val json = JSONObject(result)
                                    val code = json.optInt("code", -1)
                                    if (code == 200) {
                                        commentText = ""
                                        focusManager.clearFocus()
                                        Toast.makeText(context, "评论成功", Toast.LENGTH_SHORT).show()
                                        reloadComments()
                                    } else {
                                        val msg = json.optString("msg", "评论失败")
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        // 如果是网络延迟导致code异常但实际数据已写入，也尝试刷新
                                        reloadComments()
                                    }
                                    }
                                } catch (_: Exception) {
                                    Toast.makeText(context, "评论失败，请重试", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !commentSending
                    ) { Icon(Icons.AutoMirrored.Filled.Send, "发送", tint = Primary) }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).background(Background)
        ) {
            post?.let { p ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(42.dp).clip(CircleShape).background(PrimaryLight)
                                    .clickableSingle { navController.navigate(Routes.userProfile(p.user_id)) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text((p.nickname.firstOrNull()?.toString() ?: "U"), color = White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.clickableSingle { navController.navigate(Routes.userProfile(p.user_id)) }) {
                                Text(p.nickname, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(p.created_at.take(16), fontSize = 11.sp, color = TextHint)
                            }
                        }
                        if (p.topic.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Surface(shape = RoundedCornerShape(8.dp), color = TagBg) {
                                Text("#${p.topic}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 11.sp, color = TagText)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(p.content, fontSize = 16.sp, color = TextPrimary, lineHeight = 24.sp)

                        // 显示图片
                        val imgs = p.images.filter { it.startsWith("http") }
                        if (imgs.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            imgs.forEach { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                                    contentScale = ContentScale.FillWidth
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = Divider, thickness = 0.5.dp)
                        Row(Modifier.padding(top = 8.dp)) {
                            IconButton(onClick = {
                                ApiClient.post("/api/like", mapOf("user_id" to TokenManager.getUserId(), "post_id" to postId)) { result ->
                                    try {
                                        val resp = Gson().fromJson(result, ApiResponse::class.java)
                                        if (resp.code == 200) {
                                            val nowLiked = (resp.data as? Map<*, *>)?.get("liked") as? Boolean ?: false
                                            liked = nowLiked
                                            localLikeCount = if (nowLiked) localLikeCount + 1 else (localLikeCount - 1).coerceAtLeast(0)
                                        }
                                    } catch (_: Exception) {}
                                }
                            }) {
                                Icon(
                                    if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    null, tint = LikedColor, modifier = Modifier.size(20.dp)
                                )
                            }
                            Text("$localLikeCount", fontSize = 13.sp, color = TextHint, modifier = Modifier.align(Alignment.CenterVertically))
                            Spacer(Modifier.width(20.dp))
                            IconButton(onClick = {
                                ApiClient.post("/api/favorite", mapOf("user_id" to TokenManager.getUserId(), "post_id" to postId)) { r ->
                                    try {
                                        val resp = Gson().fromJson(r, ApiResponse::class.java)
                                        if (resp.code == 200) {
                                            val nowFav = (resp.data as? Map<*, *>)?.get("favorited") as? Boolean ?: false
                                            favorited = nowFav
                                            localFavCount = if (nowFav) localFavCount + 1 else (localFavCount - 1).coerceAtLeast(0)
                                        }
                                        Toast.makeText(context, resp.msg, Toast.LENGTH_SHORT).show()
                                    } catch (_: Exception) {}
                                }
                            }) {
                                Icon(
                                    if (favorited) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                    null, tint = SavedColor, modifier = Modifier.size(20.dp)
                                )
                            }
                            Text("$localFavCount", fontSize = 13.sp, color = TextHint, modifier = Modifier.align(Alignment.CenterVertically))
                        }
                    }
                }
            }

            Text("全部评论 ${comments.size}", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 13.sp, color = TextHint)

            comments.forEach { comment ->
                Surface(modifier = Modifier.fillMaxWidth(), color = Surface, shadowElevation = 0.5.dp) {
                    Row(Modifier.padding(12.dp, 10.dp).padding(start = if (comment.parent_id > 0) 40.dp else 0.dp)) {
                        Box(
                            Modifier.size(32.dp).clip(CircleShape).background(TagBg)
                                .clickableSingle { navController.navigate(Routes.userProfile(comment.user_id)) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text((comment.nickname.firstOrNull()?.toString() ?: "U"), fontSize = 12.sp, color = Primary)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(comment.nickname, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(comment.content, fontSize = 14.sp, color = TextSecondary)
                            Text(comment.created_at.take(16), fontSize = 10.sp, color = TextHint)
                        }
                    }
                }
                HorizontalDivider(color = Divider, thickness = 0.5.dp)
            }
        }
    }
}
