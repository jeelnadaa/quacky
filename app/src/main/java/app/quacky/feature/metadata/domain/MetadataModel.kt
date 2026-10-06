package app.quacky.feature.metadata.domain

import android.net.Uri

enum class PrivacyScore {
    CLEAN,
    CONTAINS_METADATA,
    CONTAINS_LOCATION
}

data class LocationMetadata(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val formattedCoordinates: String = "",
    val mapsUri: String = ""
) {
    val hasGps: Boolean get() = latitude != null && longitude != null
}

data class DeviceMetadata(
    val make: String? = null,
    val model: String? = null,
    val software: String? = null,
    val lensModel: String? = null
) {
    val hasData: Boolean get() = !make.isNullOrBlank() || !model.isNullOrBlank() || !software.isNullOrBlank() || !lensModel.isNullOrBlank()
}

data class CaptureMetadata(
    val dateTime: String? = null,
    val exposureTime: String? = null,
    val fNumber: String? = null,
    val iso: String? = null,
    val focalLength: String? = null,
    val flash: String? = null,
    val whiteBalance: String? = null
) {
    val hasData: Boolean get() = !dateTime.isNullOrBlank() || !exposureTime.isNullOrBlank() || !fNumber.isNullOrBlank() || !iso.isNullOrBlank()
}

data class ImageMetadata(
    val width: Int? = null,
    val height: Int? = null,
    val orientation: String? = null,
    val orientationDegrees: Int = 0,
    val colorSpace: String? = null,
    val fileSize: Long = 0L,
    val formattedFileSize: String = "",
    val mimeType: String = "",
    val fileName: String = ""
)

data class OtherTag(
    val tag: String,
    val value: String
)

data class PhotoMetadata(
    val uri: Uri? = null,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val location: LocationMetadata,
    val device: DeviceMetadata,
    val capture: CaptureMetadata,
    val image: ImageMetadata,
    val otherTags: List<OtherTag>,
    val privacyScore: PrivacyScore,
    val totalTagCount: Int
)

enum class RemovalChoice {
    ALL,
    LOCATION_ONLY,
    CUSTOM
}

data class CustomRemovalOptions(
    val removeLocation: Boolean = true,
    val removeDevice: Boolean = true,
    val removeCapture: Boolean = true,
    val removeOther: Boolean = true
)

data class CleanResult(
    val originalFileName: String,
    val outputUri: Uri,
    val tagsRemoved: Int,
    val isLossless: Boolean,
    val verifiedClean: Boolean,
    val note: String = ""
)
