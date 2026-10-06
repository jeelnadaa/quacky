package app.quacky.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import app.quacky.core.tips.GuideRegistry
import app.quacky.core.tips.HowToSheet
import app.quacky.data.local.preferences.AppPreferences
import app.quacky.feature.about.EasterEggScreen
import app.quacky.feature.category.CategoryScreen
import app.quacky.feature.help.AllGuidesScreen
import app.quacky.feature.history.GlobalHistoryScreen
import app.quacky.feature.history.HistoryViewModel
import app.quacky.feature.home.HomeScreen
import app.quacky.feature.home.HomeViewModel
import app.quacky.feature.settings.SettingsScreen
import app.quacky.feature.settings.SettingsViewModel

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

    var activeGuideTool by remember { mutableStateOf<ToolDefinition?>(null) }

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
                        activeGuideTool = tool
                    }
                )
            }

            // 2. Global History
            composable(NavRoutes.HISTORY) {
                val historyViewModel: HistoryViewModel = hiltViewModel()
                GlobalHistoryScreen(
                    viewModel = historyViewModel,
                    onNavigateToToolWithEntry = { tool, _ ->
                        navController.navigate(NavRoutes.tool(tool.id))
                    }
                )
            }

            // 3. Settings Screen
            composable(NavRoutes.SETTINGS) {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateToAllGuides = { navController.navigate(NavRoutes.ALL_GUIDES) },
                    onNavigateToEasterEgg = { navController.navigate(NavRoutes.EASTER_EGG) }
                )
            }

            // 4. All Guides Screen
            composable(NavRoutes.ALL_GUIDES) {
                AllGuidesScreen(onBack = { navController.popBackStack() })
            }

            // 5. Easter Egg Screen
            composable(NavRoutes.EASTER_EGG) {
                EasterEggScreen(onClose = { navController.popBackStack() })
            }

            // 6. Category Screen
            composable(
                route = NavRoutes.CATEGORY_PATTERN,
                arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
            ) { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getString("categoryId")
                val category = ToolCategory.entries.firstOrNull { it.id == categoryId } ?: ToolCategory.SCAN_GENERATE
                val homeViewModel: HomeViewModel = hiltViewModel()
                CategoryScreen(
                    category = category,
                    onBack = { navController.popBackStack() },
                    onNavigateToTool = { tool ->
                        navController.navigate(NavRoutes.tool(tool.id))
                    },
                    onOpenHowToUse = { tool ->
                        activeGuideTool = tool
                    },
                    isToolPinned = { id -> pinnedToolIds.contains(id) },
                    onTogglePin = { id -> homeViewModel.togglePin(id) }
                )
            }

            // 7. Tool Screen with Hardware Capability Honesty Gating (Section 5A)
            composable(
                route = NavRoutes.TOOL_PATTERN,
                arguments = listOf(navArgument("toolId") { type = NavType.StringType })
            ) { backStackEntry ->
                val toolId = backStackEntry.arguments?.getString("toolId")
                val tool = ToolRegistry.getById(toolId ?: "") ?: ToolRegistry.TEXT_COUNTER
                val homeViewModel: HomeViewModel = hiltViewModel()

                val checkResult = remember(tool, isRulerCalibrated) {
                    requirementChecker.check(tool, isRulerCalibrated)
                }

                when (checkResult) {
                    is RequirementResult.Missing -> {
                        MissingRequirementScreen(
                            tool = tool,
                            missingResult = checkResult,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    RequirementResult.Ready -> {
                        when (tool.id) {
                            ToolRegistry.TEXT_COUNTER.id -> {
                                val textCounterViewModel: app.quacky.feature.textcounter.presentation.TextCounterViewModel = hiltViewModel()
                                app.quacky.feature.textcounter.presentation.TextCounterScreen(
                                    viewModel = textCounterViewModel,
                                    onBack = { navController.popBackStack() },
                                    onOpenHowToUse = { activeGuideTool = tool }
                                )
                            }
                            ToolRegistry.DATE_CALC.id -> {
                                val dateCalcViewModel: app.quacky.feature.datecalc.presentation.DateCalcViewModel = hiltViewModel()
                                app.quacky.feature.datecalc.presentation.DateCalcScreen(
                                    viewModel = dateCalcViewModel,
                                    onBack = { navController.popBackStack() },
                                    onOpenHowToUse = { activeGuideTool = tool }
                                )
                            }
                            ToolRegistry.DICE.id -> {
                                val diceViewModel: app.quacky.feature.random.dice.DiceViewModel = hiltViewModel()
                                app.quacky.feature.random.dice.DiceScreen(
                                    viewModel = diceViewModel,
                                    onBack = { navController.popBackStack() },
                                    onOpenHowToUse = { activeGuideTool = tool }
                                )
                            }
                            ToolRegistry.COIN_FLIP.id -> {
                                val coinFlipViewModel: app.quacky.feature.random.coinflip.CoinFlipViewModel = hiltViewModel()
                                app.quacky.feature.random.coinflip.CoinFlipScreen(
                                    viewModel = coinFlipViewModel,
                                    onBack = { navController.popBackStack() },
                                    onOpenHowToUse = { activeGuideTool = tool }
                                )
                            }
                            ToolRegistry.RANDOM_NUMBER.id -> {
                                val rngViewModel: app.quacky.feature.random.randomnumber.RandomNumberViewModel = hiltViewModel()
                                app.quacky.feature.random.randomnumber.RandomNumberScreen(
                                    viewModel = rngViewModel,
                                    onBack = { navController.popBackStack() },
                                    onOpenHowToUse = { activeGuideTool = tool }
                                )
                            }
                            ToolRegistry.PICKER_WHEEL.id -> {
                                val pickerWheelViewModel: app.quacky.feature.random.pickerwheel.PickerWheelViewModel = hiltViewModel()
                                app.quacky.feature.random.pickerwheel.PickerWheelScreen(
                                    viewModel = pickerWheelViewModel,
                                    onBack = { navController.popBackStack() },
                                    onOpenHowToUse = { activeGuideTool = tool }
                                )
                            }
                            ToolRegistry.TEAM_SPLITTER.id -> {
                                val teamSplitterViewModel: app.quacky.feature.random.teamsplitter.TeamSplitterViewModel = hiltViewModel()
                                app.quacky.feature.random.teamsplitter.TeamSplitterScreen(
                                    viewModel = teamSplitterViewModel,
                                    onBack = { navController.popBackStack() },
                                    onOpenHowToUse = { activeGuideTool = tool }
                                )
                            }
                            ToolRegistry.SCREEN_RULER.id -> {
                                val rulerViewModel: app.quacky.feature.screenruler.presentation.ScreenRulerViewModel = hiltViewModel()
                                app.quacky.feature.screenruler.presentation.ScreenRulerScreen(
                                    viewModel = rulerViewModel,
                                    onBack = { navController.popBackStack() },
                                    onOpenHowToUse = { activeGuideTool = tool }
                                )
                            }
                            ToolRegistry.QR_SCANNER.id -> {
                                val scannerViewModel: app.quacky.feature.qrscanner.presentation.QrScannerViewModel = hiltViewModel()
                                app.quacky.feature.qrscanner.presentation.QrScannerScreen(
                                    viewModel = scannerViewModel,
                                    onBack = { navController.popBackStack() },
                                    onOpenHowToUse = { activeGuideTool = tool }
                                )
                            }
                            ToolRegistry.QR_GENERATOR.id -> {
                                val generatorViewModel: app.quacky.feature.qrgenerator.presentation.QrGeneratorViewModel = hiltViewModel()
                                app.quacky.feature.qrgenerator.presentation.QrGeneratorScreen(
                                    viewModel = generatorViewModel,
                                    onBack = { navController.popBackStack() },
                                    onOpenHowToUse = { activeGuideTool = tool }
                                )
                            }
                            ToolRegistry.COLOR_PICKER.id -> {
                                val colorPickerViewModel: app.quacky.feature.colorpicker.presentation.ColorPickerViewModel = hiltViewModel()
                                app.quacky.feature.colorpicker.presentation.ColorPickerScreen(
                                    viewModel = colorPickerViewModel,
                                    onBack = { navController.popBackStack() },
                                    onOpenHowToUse = { activeGuideTool = tool }
                                )
                            }
                            else -> {
                                ToolScaffold(
                                    tool = tool,
                                    onBack = { navController.popBackStack() },
                                    isPinned = pinnedToolIds.contains(tool.id),
                                    onTogglePin = { homeViewModel.togglePin(tool.id) },
                                    onHelpClick = { activeGuideTool = tool }
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
    }

    activeGuideTool?.let { tool ->
        val guide = remember(tool) { GuideRegistry.getGuideForTool(tool.id) }
        HowToSheet(
            guide = guide,
            onDismiss = { activeGuideTool = null }
        )
    }
}
