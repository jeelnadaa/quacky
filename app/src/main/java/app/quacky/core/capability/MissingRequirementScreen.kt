package app.quacky.core.capability

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.R
import app.quacky.core.designsystem.component.QuackyButton
import app.quacky.core.designsystem.component.QuackyButtonStyle
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.SatoshiFontFamily
import app.quacky.core.registry.ToolDefinition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissingRequirementScreen(
    tool: ToolDefinition,
    missingResult: RequirementResult.Missing,
    onBack: () -> Unit,
    onArInstallRequested: (() -> Unit)? = null
) {
    Scaffold(
        containerColor = QuackyBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(tool.nameRes),
                        fontFamily = SatoshiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = QuackyTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = QuackyTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = QuackyBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = QuackyTextSecondary,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Honest, plain message explaining why the tool cannot run on this device
            Text(
                text = stringResource(missingResult.reasonRes),
                fontFamily = SatoshiFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = QuackyTextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (missingResult.isArSetupRequired && onArInstallRequested != null) {
                Spacer(modifier = Modifier.height(24.dp))
                QuackyButton(
                    onClick = onArInstallRequested,
                    style = QuackyButtonStyle.Primary
                ) {
                    Text(text = stringResource(R.string.req_missing_arcore_install_button))
                }
            }
        }
    }
}
