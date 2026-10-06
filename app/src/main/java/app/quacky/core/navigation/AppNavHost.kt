package app.quacky.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.quacky.core.capability.MissingRequirementScreen
import app.quacky.core.capability.RequirementChecker
import app.quacky.core.capability.RequirementResult
import app.quacky.core.components.ToolScaffold
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.registry.ToolCategory
import app.quacky.core.registry.ToolDefinition
import app.quacky.core.registry.ToolRegistry
import app.quacky.data.local.preferences.AppPreferences
import app.quacky.feature.category.CategoryScreen
import app.quacky.feature.home.HomeScreen
import app.quacky.feature.home.HomeViewModel

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    requirementChecker: RequirementChecker,
    preferences: AppPreferences,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val pinnedToolIds by preferences.pinnedToolIds.collectAsState(initial = emptyList())
    val isRulerCalibrated by preferences.isRulerCalibrated.collectAsState(initial = false)

    // Show bottom bar only on top-level destinations
    val isTopLevelDestination = currentRoute in listOf(
        NavRoutes.HOME,
        NavRoutes.HISTORY,
        NavRoutes.SETTINGS
    )

    Scaffold(
        modifier = modifier,
        containerColor = QuackyBackground,
        bottomBar = {
            if (isTopLevelDestination) {
                QuackyBottomBar(
                    currentRoute = currentRoute,
                    onNavigateToRoute = { route ->
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(NavRoutes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Home Screen
            composable(NavRoutes.HOME) {
                val homeViewModel: HomeViewModel = hiltViewModel()
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToTool = { tool ->
                        navController.navigate(NavRoutes.tool(tool.id))
                    },
                    onNavigateToCategory = { category ->
                        navController.navigate(NavRoutes.category(category.id))
                    },
                    onOpenHowToUse = { tool ->
                        // Guide viewer route (wired in Phase 3)
                    }
                )
            }

            // 2. Global History (Stub for Phase 3)
            composable(NavRoutes.HISTORY) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Global History (Phase 3)", color = QuackyTextSecondary)
                }
            }

            // 3. Settings Screen (Stub for Phase 3)
            composable(NavRoutes.SETTINGS) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Settings (Phase 3)", color = QuackyTextSecondary)
                }
            }

            // 4. Category Screen
            composable(
                route = NavRoutes.CATEGORY_PATTERN,
                arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
            ) { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getString("categoryId")
                val category = ToolCategory.entries.firstOrNull { it.id == categoryId } ?: ToolCategory.SCAN_GENERATE
                CategoryScreen(
                    category = category,
                    onBack = { navController.popBackStack() },
                    onNavigateToTool = { tool ->
                        navController.navigate(NavRoutes.tool(tool.id))
                    },
                    onOpenHowToUse = { tool ->
                        // Guide viewer route
                    },
                    isToolPinned = { id -> pinnedToolIds.contains(id) },
                    onTogglePin = { id ->
                        // toggle pin in preferences
                    }
                )
            }

            // 5. Tool Screen with Hardware Capability Honesty Gating (Section 5A)
            composable(
                route = NavRoutes.TOOL_PATTERN,
                arguments = listOf(navArgument("toolId") { type = NavType.StringType })
            ) { backStackEntry ->
                val toolId = backStackEntry.arguments?.getString("toolId")
                val tool = ToolRegistry.getById(toolId ?: "") ?: ToolRegistry.TEXT_COUNTER

                // Gating check: Section 5A
                val checkResult = remember(tool, isRulerCalibrated) {
                    requirementChecker.check(tool, isRulerCalibrated)
                }

                when (checkResult) {
                    is RequirementResult.Missing -> {
                        MissingRequirementScreen(
                            tool = tool,
                            missingResult = checkResult,
                            onBack = { navController.popBackStack() },
                            onArInstallRequested = {
                                // Hand off to system ARCore install flow
                            }
                        )
                    }
                    RequirementResult.Ready -> {
                        // Tool placeholder content (implemented tool by tool in steps 4-12)
                        ToolScaffold(
                            tool = tool,
                            onBack = { navController.popBackStack() },
                            isPinned = pinnedToolIds.contains(tool.id)
                        ) { toolPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(toolPadding),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${tool.id} - Ready to build",
                                    color = QuackyTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
