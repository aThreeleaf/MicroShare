package com.microshare.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController) {
    var keyword by remember { mutableStateOf("") }
    var posts by remember { mutableStateOf(listOf<Post>()) }
    var users by remember { mutableStateOf(listOf<User>()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var hasSearched by remember { mutableStateOf(false) }

    BackHandler { navController.popBackStack() }

    fun search() {
        if (keyword.isBlank()) return
        hasSearched = true
        if (selectedTab == 0) {
            ApiClient.get("/api/search?keyword=$keyword") { result ->
                try {
                    if (result != null) {
                        val json = JSONObject(result)
                        if (json.optInt("code") == 200) {
                            val arr = json.optJSONArray("data") ?: JSONArray()
                            posts = (0 until arr.length()).mapNotNull { i ->
                                parsePost(arr.getJSONObject(i))
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        } else {
            ApiClient.get("/api/search/user?keyword=$keyword") { result ->
                try {
                    if (result != null) {
                        val json = JSONObject(result)
                        if (json.optInt("code") == 200) {
                            val arr = json.optJSONArray("data") ?: JSONArray()
                            users = (0 until arr.length()).mapNotNull { i ->
                                parseUser(arr.getJSONObject(i))
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("搜索发现", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = White)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(Background)) {
            Row(Modifier.fillMaxWidth().background(Surface).padding(12.dp)) {
                OutlinedTextField(
                    value = keyword, onValueChange = { keyword = it },
                    placeholder = { Text("搜索帖子或用户...", color = TextHint) },
                    modifier = Modifier.weight(1f), singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { search() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) { Text("搜索", color = White) }
            }

            TabRow(selectedTabIndex = selectedTab, containerColor = Surface, contentColor = Primary) {
                listOf("帖子", "用户").forEachIndexed { i, t ->
                    Tab(selected = selectedTab == i, onClick = { selectedTab = i; search() }, text = { Text(t) })
                }
            }

            if (!hasSearched) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("输入关键词搜索", color = TextHint, fontSize = 14.sp)
                }
            } else if (selectedTab == 0) {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(posts) { post ->
                        PostCard(
                            post = post,
                            onClick = { navController.navigate(Routes.postDetail(post.id)) },
                            onLike = {}, onAvatarClick = {}
                        )
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(users) { user ->
                        Card(
                            onClick = { navController.navigate(Routes.userProfile(user.id)) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Surface)
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(44.dp).clip(CircleShape).background(PrimaryLight), contentAlignment = Alignment.Center) {
                                    Text((user.nickname.firstOrNull()?.toString() ?: "U"), color = White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(user.nickname, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                                    if (user.bio.isNotEmpty()) Text(user.bio, fontSize = 12.sp, color = TextHint)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun parsePost(json: JSONObject): Post {
    val imagesJson = json.optJSONArray("images")
    val images = mutableListOf<String>()
    if (imagesJson != null) {
        for (i in 0 until imagesJson.length()) {
            images.add(imagesJson.optString(i, ""))
        }
    }
    return Post(
        id = json.optInt("id", 0),
        user_id = json.optInt("user_id", 0),
        content = json.optString("content", ""),
        topic = json.optString("topic", ""),
        like_count = json.optInt("like_count", 0),
        comment_count = json.optInt("comment_count", 0),
        favorite_count = json.optInt("favorite_count", 0),
        nickname = json.optString("nickname", ""),
        avatar = json.optString("avatar", ""),
        created_at = json.optString("created_at", ""),
        _images = images
    )
}

private fun parseUser(json: JSONObject): User {
    return User(
        id = json.optInt("id", 0),
        username = json.optString("username", ""),
        nickname = json.optString("nickname", ""),
        avatar = json.optString("avatar", ""),
        bio = json.optString("bio", "")
    )
}
