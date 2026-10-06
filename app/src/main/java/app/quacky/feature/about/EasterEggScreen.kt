package app.quacky.feature.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.DeveloperInfo
import app.quacky.R
import app.quacky.core.brand.QuackyMark
import app.quacky.core.designsystem.component.QuackyButton
import app.quacky.core.designsystem.component.QuackyButtonStyle
import app.quacky.core.designsystem.theme.CardCornerRadius
import app.quacky.core.designsystem.theme.PillCornerRadius
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.QuackyTextTertiary
import app.quacky.core.designsystem.theme.SatoshiFontFamily
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EasterEggScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var visibleParagraphCount by remember { mutableIntStateOf(0) }

    // Gentle idle blink every ~4 seconds
    val transition = rememberInfiniteTransition(label = "blink_trans")
    val blinkProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4000
                0f at 0
                0f at 3800
                1f at 3900 using LinearEasing
                0f at 4000 using LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "blink"
    )

    // Stagger paragraph fade-in
    LaunchedEffect(Unit) {
        for (i in 1..8) {
            delay(300)
            visibleParagraphCount = i
        }
    }

    val paragraphs = listOf(
        stringResource(R.string.easter_egg_p1, DeveloperInfo.DEVELOPER_NAME),
        stringResource(R.string.easter_egg_p2),
        stringResource(R.string.easter_egg_p3),
        stringResource(R.string.easter_egg_p4),
        stringResource(R.string.easter_egg_p5),
        stringResource(R.string.easter_egg_p6),
        stringResource(R.string.easter_egg_p7),
        stringResource(R.string.easter_egg_signoff, DeveloperInfo.DEVELOPER_NAME)
    )

    Scaffold(
        modifier = modifier,
        containerColor = QuackyBackground,
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.action_close),
                            tint = QuackyTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = QuackyBackground)
            )
        }
    ) { innerPadding ->
        SelectionContainer {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .clickable { visibleParagraphCount = paragraphs.size }, // tap anywhere to reveal all
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header: Animated Quacky Mascot & Title
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        QuackyMark(
                            size = 64.dp,
                            tint = QuackyTextPrimary,
                            blinkProgress = blinkProgress
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = stringResource(R.string.easter_egg_title),
                            fontFamily = SatoshiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = QuackyTextPrimary
                        )
                    }
                }

                // Paragraphs
                paragraphs.forEachIndexed { index, paragraphText ->
                    item(key = "p_$index") {
                        AnimatedVisibility(
                            visible = visibleParagraphCount > index,
                            enter = fadeIn()
                        ) {
                            Text(
                                text = paragraphText,
                                fontFamily = SatoshiFontFamily,
                                fontWeight = if (index == paragraphs.size - 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                color = QuackyTextPrimary
                            )
                        }
                    }
                }

                // Footer Block
                item {
                    AnimatedVisibility(
                        visible = visibleParagraphCount >= paragraphs.size,
                        enter = fadeIn()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            HorizontalDivider(color = QuackyOutline, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = stringResource(R.string.developed_by, DeveloperInfo.DEVELOPER_NAME),
                                fontFamily = SatoshiFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = QuackyTextSecondary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Email Row with Tap & Long-Press
                            Surface(
                                shape = RoundedCornerShape(CardCornerRadius),
                                color = QuackySurface,
                                border = BorderStroke(1.dp, QuackyOutline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                data = Uri.parse("mailto:${DeveloperInfo.DEVELOPER_EMAIL}")
                                                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.easter_egg_feedback_subject))
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "Quacky v1.0.0\nAndroid ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n\n"
                                                )
                                            }
                                            try {
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onLongClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Quacky Email", DeveloperInfo.DEVELOPER_EMAIL))
                                            Toast.makeText(context, "Copied email address", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Email,
                                        contentDescription = null,
                                        tint = QuackyTextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = DeveloperInfo.DEVELOPER_EMAIL,
                                        fontFamily = SatoshiFontFamily,
                                        fontSize = 14.sp,
                                        color = QuackyTextPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Pill Tagline
                            Surface(
                                shape = RoundedCornerShape(PillCornerRadius),
                                color = QuackySurface,
                                border = BorderStroke(1.dp, QuackyOutline),
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text(
                                    text = stringResource(R.string.app_tagline),
                                    fontFamily = SatoshiFontFamily,
                                    fontSize = 12.sp,
                                    color = QuackyTextSecondary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            QuackyButton(
                                onClick = onClose,
                                style = QuackyButtonStyle.Primary,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = stringResource(R.string.action_close))
                            }
                        }
                    }
                }
            }
        }
    }
}
