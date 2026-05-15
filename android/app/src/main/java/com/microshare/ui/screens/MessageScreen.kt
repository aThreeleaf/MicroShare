package com.microshare.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.microshare.network.SocketClient
import com.microshare.ui.navigation.Routes
import com.microshare.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageScreen(navController: NavController) {
    val notifications = remember { SocketClient.notificationQueue.toMutableList() }

    BackHandler { navController.popBackStack() }

    LaunchedEffect(Unit) {
        SocketClient.notificationQueue.clear()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("消息通知", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = White)
            )
        }
    ) { padding ->
        if (notifications.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).background(Background), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔔", fontSize = 48.sp)
                    Text("暂无消息通知", fontSize = 14.sp, color = TextHint)
                    Text("等待互动提醒...", fontSize = 12.sp, color = TextHint)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).background(Background)) {
                items(notifications.toList()) { n ->
                    val icon = when (n.type) { "new_comment" -> "💬"; "new_like" -> "❤️"; "new_follower" -> "👤"; else -> "🔔" }
                    Surface(
                        modifier = Modifier.fillMaxWidth()
                            .then(
                                if (n.post_id > 0) Modifier.clickableSingle {
                                    navController.navigate(Routes.postDetail(n.post_id))
                                }
                                else if (n.user_id > 0) Modifier.clickableSingle {
                                    navController.navigate(Routes.userProfile(n.user_id))
                                }
                                else Modifier
                            ),
                        color = Surface
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(icon, fontSize = 22.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(n.msg, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                            Text("刚刚", fontSize = 11.sp, color = TextHint)
                        }
                    }
                    HorizontalDivider(color = Divider, thickness = 0.5.dp)
                }
            }
        }
    }
}
