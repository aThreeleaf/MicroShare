package com.microshare.ui.screens

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.microshare.network.SocketClient
import com.microshare.service.StepService
import com.microshare.ui.navigation.Routes
import com.microshare.ui.theme.MicroShareTheme
import com.microshare.utils.TokenManager

/** Compose版单Activity入口 */
class MainActivity : ComponentActivity() {

    private val socketClient = SocketClient()
    private var notificationCount by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TokenManager.init(this)

        // 启动服务（容错）
        try { startService(Intent(this, StepService::class.java)) } catch (_: Exception) {}
        try { socketClient.connect() } catch (_: Exception) {}

        socketClient.setOnNotificationListener {
            notificationCount++
            runOnUiThread { Toast.makeText(this, it.msg, Toast.LENGTH_SHORT).show() }
        }

        val isLoggedIn = TokenManager.isLoggedIn()
        val startRoute = if (isLoggedIn) Routes.MAIN else Routes.SPLASH

        setContent {
            MicroShareTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = startRoute) {
                        composable(Routes.SPLASH) { SplashScreen(navController) }
                        composable(Routes.LOGIN) { LoginScreen(navController) }
                        composable(Routes.REGISTER) { RegisterScreen(navController) }
                        composable(Routes.MAIN) {
                            MainScreen(
                                navController = navController,
                                notificationCount = notificationCount,
                                onClearBadge = { notificationCount = 0 },
                                socketClient = socketClient
                            )
                        }
                        composable(Routes.POST_DETAIL) { backStackEntry ->
                            val postId = backStackEntry.arguments?.getString("postId")?.toIntOrNull() ?: 0
                            PostDetailScreen(navController, postId)
                        }
                        composable(Routes.POST_EDIT) { PostEditScreen(navController) }
                        composable(Routes.USER_PROFILE) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId")?.toIntOrNull() ?: 0
                            UserProfileScreen(navController, userId)
                        }
                        composable(Routes.FOLLOW_LIST) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId")?.toIntOrNull() ?: 0
                            val tab = backStackEntry.arguments?.getString("tab") ?: "following"
                            FollowListScreen(navController, userId, tab)
                        }
                        composable(Routes.MESSAGE) { MessageScreen(navController) }
                        composable(Routes.SEARCH) { SearchScreen(navController) }
                        composable(Routes.SPORT) { SportScreen(navController) }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        try { socketClient.disconnect() } catch (_: Exception) {}
        super.onDestroy()
    }
}
