package com.microshare.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.microshare.network.SocketClient
import com.microshare.ui.navigation.Routes
import com.microshare.ui.theme.*
import com.microshare.utils.TokenManager

data class BottomNavItem(val label: String, val icon: ImageVector, val route: String)

@Composable
fun MainScreen(
    navController: NavController,
    notificationCount: Int,
    onClearBadge: () -> Unit,
    socketClient: SocketClient
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    // 拦截系统返回键：主屏幕按返回时优雅退出而非崩溃
    BackHandler {
        (context as? android.app.Activity)?.moveTaskToBack(true)
    }

    val bottomItems = listOf(
        BottomNavItem("首页", Icons.Filled.Home, "home"),
        BottomNavItem("发布", Icons.Outlined.Edit, "publish"),
        BottomNavItem("消息", Icons.Outlined.Notifications, "message"),
        BottomNavItem("运动", Icons.Outlined.DirectionsRun, "sport"),
        BottomNavItem("我的", Icons.Outlined.Person, "profile"),
    )

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().background(Primary).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text("微分享社区", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White, modifier = Modifier.weight(1f))
                IconButton(onClick = { navController.navigate(Routes.SEARCH) }) {
                    Icon(Icons.Filled.Search, "搜索", tint = White)
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Surface) {
                bottomItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = {
                            when (item.route) {
                                "home" -> selectedIndex = 0
                                "publish" -> navController.navigate(Routes.POST_EDIT)
                                "message" -> {
                                    onClearBadge()
                                    navController.navigate(Routes.MESSAGE)
                                }
                                "sport" -> navController.navigate(Routes.SPORT)
                                "profile" -> navController.navigate(Routes.userProfile(TokenManager.getUserId()))
                            }
                            if (item.route == "home") selectedIndex = 0
                        },
                        icon = {
                            if (item.route == "message" && notificationCount > 0) {
                                BadgedBox(badge = { Badge { Text("$notificationCount") } }) {
                                    Icon(item.icon, item.label)
                                }
                            } else {
                                Icon(item.icon, item.label)
                            }
                        },
                        label = { Text(item.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary,
                            selectedTextColor = Primary,
                            indicatorColor = PrimaryLight.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            HomeScreen(navController)
        }
    }
}
