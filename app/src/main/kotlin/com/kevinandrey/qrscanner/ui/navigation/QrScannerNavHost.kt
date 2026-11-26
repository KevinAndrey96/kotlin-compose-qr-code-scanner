package com.kevinandrey.qrscanner.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.kevinandrey.qrscanner.R
import com.kevinandrey.qrscanner.data.ScanRepository
import com.kevinandrey.qrscanner.ui.detail.ScanDetailScreen
import com.kevinandrey.qrscanner.ui.detail.ScanDetailViewModel
import com.kevinandrey.qrscanner.ui.generator.GeneratorScreen
import com.kevinandrey.qrscanner.ui.generator.GeneratorViewModel
import com.kevinandrey.qrscanner.ui.history.HistoryScreen
import com.kevinandrey.qrscanner.ui.history.HistoryViewModel
import com.kevinandrey.qrscanner.ui.scanner.ScannerScreen
import com.kevinandrey.qrscanner.ui.scanner.ScannerViewModel
import kotlinx.serialization.Serializable

@Serializable
data object ScannerRoute

@Serializable
data object GeneratorRoute

@Serializable
data object HistoryRoute

@Serializable
data class ScanDetailRoute(
    val id: Long,
)

private enum class TopLevelDestination(
    val route: Any,
    @param:StringRes val label: Int,
    val icon: ImageVector,
) {
    SCAN(ScannerRoute, R.string.nav_scan, Icons.Outlined.QrCodeScanner),
    CREATE(GeneratorRoute, R.string.nav_create, Icons.Outlined.QrCode2),
    HISTORY(HistoryRoute, R.string.nav_history, Icons.Outlined.History),
}

@Composable
fun QrScannerNavHost(
    repository: ScanRepository,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = destination?.hasRoute<ScanDetailRoute>() != true

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { item ->
                        val selected = destination?.hierarchy?.any { it.hasRoute(item.route::class) } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(bottom = padding.calculateBottomPadding()).consumeWindowInsets(padding)) {
            NavHost(navController = navController, startDestination = ScannerRoute) {
                composable<ScannerRoute> {
                    ScannerScreen(
                        viewModel = viewModel { ScannerViewModel(repository) },
                        onOpenScan = { id -> navController.navigate(ScanDetailRoute(id)) },
                    )
                }
                composable<GeneratorRoute> {
                    GeneratorScreen(viewModel = viewModel { GeneratorViewModel() })
                }
                composable<HistoryRoute> {
                    HistoryScreen(
                        viewModel = viewModel { HistoryViewModel(repository) },
                        onOpenScan = { id -> navController.navigate(ScanDetailRoute(id)) },
                    )
                }
                composable<ScanDetailRoute> { entry ->
                    val id = entry.toRoute<ScanDetailRoute>().id
                    ScanDetailScreen(
                        viewModel = viewModel { ScanDetailViewModel(id, repository) },
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
