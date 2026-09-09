package com.jeffsuebpeng.dailyreminder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jeffsuebpeng.dailyreminder.notifications.NotificationHelper
import com.jeffsuebpeng.dailyreminder.ui.AddEditTaskScreen
import com.jeffsuebpeng.dailyreminder.ui.SettingsScreen
import com.jeffsuebpeng.dailyreminder.ui.StatsScreen
import com.jeffsuebpeng.dailyreminder.ui.TaskListScreen
import com.jeffsuebpeng.dailyreminder.ui.theme.DailyReminderTheme

class MainActivity : ComponentActivity() {

    private val viewModel: TaskViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* No-op: notifications simply won't show if the user denies. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.createNotificationChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val deepLinkTaskId = intent.getLongExtra("open_task_id", -1L).takeIf { it != -1L }

        setContent {
            DailyReminderTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(viewModel, deepLinkTaskId)
                }
            }
        }
    }
}

@Composable
fun AppNavHost(viewModel: TaskViewModel, deepLinkTaskId: Long?) {
    val navController = rememberNavController()
    val startDestination = if (deepLinkTaskId != null) "edit/$deepLinkTaskId" else "list"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("list") {
            TaskListScreen(
                viewModel = viewModel,
                onAddTask = { navController.navigate("edit/-1") },
                onEditTask = { taskId -> navController.navigate("edit/$taskId") },
                onOpenStats = { navController.navigate("stats") },
                onOpenSettings = { navController.navigate("settings") }
            )
        }
        composable(
            "edit/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId") ?: -1L
            AddEditTaskScreen(
                viewModel = viewModel,
                existingTaskId = if (taskId == -1L) null else taskId,
                onDone = { if (!navController.popBackStack()) navController.navigate("list") }
            )
        }
        composable("stats") { StatsScreen(onBack = { navController.popBackStack() }) }
        composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }
    }
}
