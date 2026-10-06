package app.quacky.feature.qrscanner.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.R
import app.quacky.core.designsystem.component.QuackyButton
import app.quacky.core.designsystem.component.QuackyButtonStyle
import app.quacky.core.designsystem.theme.QuackyAccent
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackySurfaceElevated
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.QuackyTextTertiary
import app.quacky.feature.qrscanner.domain.BarcodeType
import app.quacky.feature.qrscanner.domain.ParsedBarcode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultSheet(
    barcode: ParsedBarcode,
    sheetState: SheetState,
    canHandleIntent: (Intent) -> Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = QuackySurface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row: Type Badge & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(QuackySurfaceElevated)
                        .border(1.dp, QuackyOutline, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${barcode.type.displayName} • ${barcode.formatName}",
                        color = QuackyTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_close),
                        tint = QuackyTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Title & Subtitle
            Text(
                text = barcode.title,
                color = QuackyTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            if (barcode.subtitle.isNotBlank() && barcode.subtitle != barcode.title) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = barcode.subtitle,
                    color = QuackyTextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            // Structured Details (if available)
            if (barcode.details.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(QuackyBackground, RoundedCornerShape(10.dp))
                        .border(1.dp, QuackyOutline, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    barcode.details.forEach { (key, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = key,
                                color = QuackyTextTertiary,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(0.4f)
                            )
                            Text(
                                text = value,
                                color = QuackyTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Contextual Action Buttons (Strictly filtered by canHandleIntent)
            when (barcode.type) {
                BarcodeType.URL -> {
                    val urlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(barcode.rawValue))
                    if (canHandleIntent(urlIntent)) {
                        QuackyButton(
                            onClick = { context.startActivity(urlIntent) },
                            style = QuackyButtonStyle.Primary,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open in Browser")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                BarcodeType.UPI -> {
                    val upiIntent = Intent(Intent.ACTION_VIEW, Uri.parse(barcode.rawValue))
                    if (canHandleIntent(upiIntent)) {
                        QuackyButton(
                            onClick = { context.startActivity(upiIntent) },
                            style = QuackyButtonStyle.Primary,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pay with UPI App")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                BarcodeType.PHONE -> {
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${barcode.title}"))
                    if (canHandleIntent(dialIntent)) {
                        QuackyButton(
                            onClick = { context.startActivity(dialIntent) },
                            style = QuackyButtonStyle.Primary,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Call Number")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                BarcodeType.SMS -> {
                    val smsNumber = barcode.details["Recipient"] ?: barcode.title
                    val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$smsNumber")).apply {
                        barcode.details["Message"]?.let { putExtra("sms_body", it) }
                    }
                    if (canHandleIntent(smsIntent)) {
                        QuackyButton(
                            onClick = { context.startActivity(smsIntent) },
                            style = QuackyButtonStyle.Primary,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send SMS")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                BarcodeType.EMAIL -> {
                    val emailTo = barcode.details["To"] ?: barcode.title
                    val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$emailTo")).apply {
                        barcode.details["Subject"]?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
                        barcode.details["Body"]?.let { putExtra(Intent.EXTRA_TEXT, it) }
                    }
                    if (canHandleIntent(emailIntent)) {
                        QuackyButton(
                            onClick = { context.startActivity(emailIntent) },
                            style = QuackyButtonStyle.Primary,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send Email")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                BarcodeType.CONTACT -> {
                    val contactIntent = Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI).apply {
                        barcode.details["Name"]?.let { putExtra(ContactsContract.Intents.Insert.NAME, it) }
                        barcode.details["Phone"]?.let { putExtra(ContactsContract.Intents.Insert.PHONE, it) }
                        barcode.details["Email"]?.let { putExtra(ContactsContract.Intents.Insert.EMAIL, it) }
                        barcode.details["Organization"]?.let { putExtra(ContactsContract.Intents.Insert.COMPANY, it) }
                    }
                    if (canHandleIntent(contactIntent)) {
                        QuackyButton(
                            onClick = { context.startActivity(contactIntent) },
                            style = QuackyButtonStyle.Primary,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add to Contacts")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                else -> Unit
            }

            // Universal Copy & Share Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuackyButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("Barcode", barcode.rawValue))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    style = QuackyButtonStyle.Secondary,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_copy))
                }

                QuackyButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, barcode.rawValue)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share code"))
                    },
                    style = QuackyButtonStyle.Secondary,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_share))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
