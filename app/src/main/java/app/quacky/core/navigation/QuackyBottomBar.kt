package app.quacky.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.R
import app.quacky.core.designsystem.theme.QuackyAccent
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackyTextTertiary
import app.quacky.core.designsystem.theme.SatoshiFontFamily
import app.quacky.core.haptics.rememberQuackyHaptics

sealed class BottomNavItem(
    val route: String,
    val titleRes: Int,
    val icon: ImageVector
) {
    data object Home : BottomNavItem(NavRoutes.HOME, R.string.nav_home, Icons.Rounded.Home)
    data object History : BottomNavItem(NavRoutes.HISTORY, R.string.nav_history, Icons.Rounded.History)
    data object Settings : BottomNavItem(NavRoutes.SETTINGS, R.string.nav_settings, Icons.Rounded.Settings)
}

val BottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.History,
    BottomNavItem.Settings
)

@Composable
fun QuackyBottomBar(
    currentRoute: String?,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberQuackyHaptics()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(QuackyBackground)
    ) {
        HorizontalDivider(color = QuackyOutline, thickness = 1.dp)
        NavigationBar(
            containerColor = QuackyBackground,
            modifier = Modifier.height(68.dp)
        ) {
            BottomNavItems.forEach { item ->
                val selected = currentRoute == item.route
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        haptics.click()
                        onNavigateToRoute(item.route)
                    },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = stringResource(item.titleRes)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(item.titleRes),
                            fontFamily = SatoshiFontFamily,
                            fontSize = 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = QuackyAccent,
                        selectedTextColor = QuackyAccent,
                        unselectedIconColor = QuackyTextTertiary,
                        unselectedTextColor = QuackyTextTertiary,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}
