package app.quacky.feature.qrgenerator.presentation

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quacky.data.local.preferences.AppPreferences
import app.quacky.data.repository.HistoryRepository
import app.quacky.feature.qrgenerator.domain.BarcodeGeneratorEngine
import app.quacky.feature.qrgenerator.domain.EcLevel
import app.quacky.feature.qrgenerator.domain.GeneratorFormat
import app.quacky.feature.qrgenerator.domain.GeneratorOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

enum class InputType(val label: String) {
    TEXT("Text"),
    URL("URL"),
    WIFI("Wi-Fi"),
    CONTACT("Contact"),
    EMAIL("Email"),
    SMS("SMS"),
    PHONE("Phone"),
    UPI("UPI"),
    LOCATION("Location"),
    EVENT("Event")
}

data class QrGeneratorUiState(
    val inputType: InputType = InputType.TEXT,
    val format: GeneratorFormat = GeneratorFormat.QR_CODE,
    val ecLevel: EcLevel = EcLevel.M,
    val margin: Int = 2,
    // Fields
    val textValue: String = "https://quacky.app",
    val urlValue: String = "https://quacky.app",
    val wifiSsid: String = "",
    val wifiPassword: String = "",
    val wifiSecurity: String = "WPA",
    val wifiHidden: Boolean = false,
    val contactName: String = "",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val contactOrg: String = "",
    val emailTo: String = "",
    val emailSubject: String = "",
    val emailBody: String = "",
    val smsPhone: String = "",
    val smsMessage: String = "",
    val phoneNumber: String = "",
    val upiVpa: String = "",
    val upiName: String = "",
    val upiAmount: String = "",
    val upiNote: String = "",
    val locLat: String = "",
    val locLng: String = "",
    val eventTitle: String = "",
    val eventLocation: String = "",
    // Preview
    val previewBitmap: Bitmap? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val isGenerating: Boolean = false
)

@HiltViewModel
class QrGeneratorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: AppPreferences,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrGeneratorUiState())
    val uiState: StateFlow<QrGeneratorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.recordToolUsed("qr_generator")
            generatePreview()
        }
    }

    fun setInputType(type: InputType) {
        _uiState.value = _uiState.value.copy(inputType = type)
        generatePreview()
    }

    fun setFormat(format: GeneratorFormat) {
        _uiState.value = _uiState.value.copy(format = format)
        generatePreview()
    }

    fun setEcLevel(level: EcLevel) {
        _uiState.value = _uiState.value.copy(ecLevel = level)
        generatePreview()
    }

    fun updateField(field: String, value: String) {
        _uiState.value = when (field) {
            "text" -> _uiState.value.copy(textValue = value)
            "url" -> _uiState.value.copy(urlValue = value)
            "wifiSsid" -> _uiState.value.copy(wifiSsid = value)
            "wifiPassword" -> _uiState.value.copy(wifiPassword = value)
            "wifiSecurity" -> _uiState.value.copy(wifiSecurity = value)
            "contactName" -> _uiState.value.copy(contactName = value)
            "contactPhone" -> _uiState.value.copy(contactPhone = value)
            "contactEmail" -> _uiState.value.copy(contactEmail = value)
            "contactOrg" -> _uiState.value.copy(contactOrg = value)
            "emailTo" -> _uiState.value.copy(emailTo = value)
            "emailSubject" -> _uiState.value.copy(emailSubject = value)
            "emailBody" -> _uiState.value.copy(emailBody = value)
            "smsPhone" -> _uiState.value.copy(smsPhone = value)
            "smsMessage" -> _uiState.value.copy(smsMessage = value)
            "phoneNumber" -> _uiState.value.copy(phoneNumber = value)
            "upiVpa" -> _uiState.value.copy(upiVpa = value)
            "upiName" -> _uiState.value.copy(upiName = value)
            "upiAmount" -> _uiState.value.copy(upiAmount = value)
            "upiNote" -> _uiState.value.copy(upiNote = value)
            "locLat" -> _uiState.value.copy(locLat = value)
            "locLng" -> _uiState.value.copy(locLng = value)
            "eventTitle" -> _uiState.value.copy(eventTitle = value)
            "eventLocation" -> _uiState.value.copy(eventLocation = value)
            else -> _uiState.value
        }
        generatePreview()
    }

    fun toggleWifiHidden() {
        _uiState.value = _uiState.value.copy(wifiHidden = !_uiState.value.wifiHidden)
        generatePreview()
    }

    fun getPayloadString(): String {
        val s = _uiState.value
        return when (s.inputType) {
            InputType.TEXT -> s.textValue
            InputType.URL -> s.urlValue
            InputType.WIFI -> "WIFI:S:${s.wifiSsid};T:${s.wifiSecurity};P:${s.wifiPassword};H:${s.wifiHidden};;"
            InputType.CONTACT -> "BEGIN:VCARD\nVERSION:3.0\nFN:${s.contactName}\nTEL:${s.contactPhone}\nEMAIL:${s.contactEmail}\nORG:${s.contactOrg}\nEND:VCARD"
            InputType.EMAIL -> "mailto:${s.emailTo}?subject=${Uri.encode(s.emailSubject)}&body=${Uri.encode(s.emailBody)}"
            InputType.SMS -> "smsto:${s.smsPhone}:${s.smsMessage}"
            InputType.PHONE -> "tel:${s.phoneNumber}"
            InputType.UPI -> "upi://pay?pa=${s.upiVpa}&pn=${Uri.encode(s.upiName)}&am=${s.upiAmount}&cu=INR&tn=${Uri.encode(s.upiNote)}"
            InputType.LOCATION -> "geo:${s.locLat},${s.locLng}"
            InputType.EVENT -> "BEGIN:VEVENT\nSUMMARY:${s.eventTitle}\nLOCATION:${s.eventLocation}\nEND:VEVENT"
        }
    }

    private fun generatePreview() {
        viewModelScope.launch(Dispatchers.Default) {
            val payload = getPayloadString()
            val format = _uiState.value.format
            val validationResult = BarcodeGeneratorEngine.validateAndSanitizeInput(payload, format)

            if (validationResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = validationResult.exceptionOrNull()?.message,
                    previewBitmap = null
                )
                return@launch
            }

            val sanitizedPayload = validationResult.getOrThrow()
            val options = GeneratorOptions(
                format = format,
                ecLevel = _uiState.value.ecLevel,
                margin = _uiState.value.margin,
                width = 512,
                height = 512
            )

            try {
                val matrix = BarcodeGeneratorEngine.generateBitMatrix(sanitizedPayload, options)
                val bitmap = BarcodeGeneratorEngine.renderBitmap(matrix, options)
                _uiState.value = _uiState.value.copy(
                    previewBitmap = bitmap,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Cannot generate barcode: ${e.localizedMessage}",
                    previewBitmap = null
                )
            }
        }
    }

    fun saveToGallery(onSaved: (String) -> Unit) {
        val bitmap = _uiState.value.previewBitmap ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val filename = "Quacky_${System.currentTimeMillis()}.png"
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Quacky")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        context.contentResolver.update(uri, values, null, null)
                    }

                    // Record to Room history
                    val payload = getPayloadString()
                    val thumbStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 80, thumbStream)
                    historyRepository.addEntry(
                        toolId = "qr_generator",
                        type = _uiState.value.format.name,
                        title = "${_uiState.value.format.displayName} (${_uiState.value.inputType.label})",
                        subtitle = payload.take(60),
                        payloadJson = "{\"payload\":\"${payload.replace("\"", "\\\"")}\",\"format\":\"${_uiState.value.format.name}\"}",
                        thumbnailBytes = thumbStream.toByteArray()
                    )

                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(infoMessage = "Saved image to Gallery (Pictures/Quacky)")
                        onSaved(filename)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(errorMessage = "Save failed: ${e.localizedMessage}")
                }
            }
        }
    }

    fun exportSvg(onExported: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            val payload = getPayloadString()
            val format = _uiState.value.format
            val options = GeneratorOptions(
                format = format,
                ecLevel = _uiState.value.ecLevel,
                margin = _uiState.value.margin,
                width = 1000,
                height = 1000
            )
            try {
                val matrix = BarcodeGeneratorEngine.generateBitMatrix(payload, options)
                val svg = BarcodeGeneratorEngine.exportToSvg(matrix, options)
                withContext(Dispatchers.Main) {
                    onExported(svg)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(errorMessage = "SVG export failed: ${e.localizedMessage}")
                }
            }
        }
    }

    fun exportPdf(onExported: (ByteArray) -> Unit) {
        val bitmap = _uiState.value.previewBitmap ?: return
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val pdfBytes = BarcodeGeneratorEngine.exportToPdf(bitmap, "${_uiState.value.format.displayName} - ${_uiState.value.inputType.label}")
                withContext(Dispatchers.Main) {
                    onExported(pdfBytes)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(errorMessage = "PDF export failed: ${e.localizedMessage}")
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, infoMessage = null)
    }
}
