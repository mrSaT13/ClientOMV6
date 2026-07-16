package com.omv.client.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.omv.client.ui.dashboard.DashboardScreen
import com.omv.client.ui.docker.DockerScreen
import com.omv.client.ui.login.LoginScreen
import com.omv.client.ui.monitoring.MonitoringScreen
import com.omv.client.ui.notifications.NotificationsScreen
import com.omv.client.ui.onboarding.OnboardingScreen
import com.omv.client.ui.services.ServicesScreen
import com.omv.client.ui.settings.SettingsScreen
import com.omv.client.ui.backup.BackupScreen
import com.omv.client.ui.plugins.PluginScreen
import com.omv.client.ui.smart.SmartScreen
import com.omv.client.ui.splash.SplashScreen
import com.omv.client.ui.terminal.TerminalScreen

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object Login : Screen("login")
    data object Dashboard : Screen("dashboard")
    data object Services : Screen("services")
    data object Monitoring : Screen("monitoring")
    data object Docker : Screen("docker")
    data object Smart : Screen("smart")
    data object Plugins : Screen("plugins")
    data object Terminal : Screen("terminal")
    data object Notifications : Screen("notifications")
    data object Settings : Screen("settings")
    data object Backup : Screen("backup")
}

data class NavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

val bottomNavItems = listOf(
    NavItem(Screen.Dashboard, Icons.Default.Dashboard, "Главная"),
    NavItem(Screen.Services, Icons.Default.Memory, "Сервисы"),
    NavItem(Screen.Monitoring, Icons.Default.Speed, "Мониторинг"),
    NavItem(Screen.Docker, Icons.Default.Inventory2, "Docker"),
    NavItem(Screen.Settings, Icons.Default.Settings, "Настройки")
)

@Composable
fun Modifier.swipeBetweenTabs(
    currentIndex: Int,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
): Modifier = this.pointerInput(currentIndex) {
    detectHorizontalDragGestures { _, dragAmount ->
        if (dragAmount < -80 && currentIndex < bottomNavItems.lastIndex) {
            onSwipeLeft()
        } else if (dragAmount > 80 && currentIndex > 0) {
            onSwipeRight()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmvNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.screen.route }

    fun navigateToTab(index: Int) {
        navController.navigate(bottomNavItems[index].screen.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEachIndexed { index, item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.screen.route
                        } == true

                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label, modifier = Modifier.size(22.dp)) },
                            label = {
                                Text(
                                    item.label,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            },
                            selected = selected,
                            onClick = { navigateToTab(index) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onSplashComplete = { isFirstLaunch, hasCredentials ->
                        if (isFirstLaunch) {
                            navController.navigate(Screen.Onboarding.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        } else if (hasCredentials) {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToServices = { navigateToTab(1) },
                    onNavigateToMonitoring = { navigateToTab(2) },
                    onNavigateToSmart = { navController.navigate(Screen.Smart.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToTerminal = { navController.navigate(Screen.Terminal.route) },
                    onNavigateToBackup = { navController.navigate(Screen.Backup.route) },
                    modifier = Modifier.swipeBetweenTabs(0, { navigateToTab(1) }, { navigateToTab(1) })
                )
            }

            composable(Screen.Services.route) {
                ServicesScreen(
                    modifier = Modifier.swipeBetweenTabs(1, { navigateToTab(2) }, { navigateToTab(0) })
                )
            }

            composable(Screen.Monitoring.route) {
                MonitoringScreen(
                    modifier = Modifier.swipeBetweenTabs(2, { navigateToTab(3) }, { navigateToTab(1) })
                )
            }

            composable(Screen.Docker.route) {
                DockerScreen(
                    modifier = Modifier.swipeBetweenTabs(3, { navigateToTab(4) }, { navigateToTab(2) })
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    modifier = Modifier.swipeBetweenTabs(4, {}, { navigateToTab(3) })
                )
            }

            composable(Screen.Smart.route) { SmartScreen() }
            composable(Screen.Plugins.route) { PluginScreen() }
            composable(Screen.Terminal.route) { TerminalScreen() }
            composable(Screen.Notifications.route) { NotificationsScreen() }
            composable(Screen.Backup.route) { BackupScreen() }
        }
    }
}
