package app.quacky.feature.metadata.domain

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream
import java.util.Locale

object ExifEngine {

    val ALL_GPS_TAGS = listOf(
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
        ExifInterface.TAG_GPS_SPEED,
        ExifInterface.TAG_GPS_SPEED_REF,
        ExifInterface.TAG_GPS_TRACK,
        ExifInterface.TAG_GPS_TRACK_REF,
        ExifInterface.TAG_GPS_IMG_DIRECTION,
        ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
        ExifInterface.TAG_GPS_DEST_BEARING,
        ExifInterface.TAG_GPS_DEST_BEARING_REF,
        ExifInterface.TAG_GPS_DEST_DISTANCE,
        ExifInterface.TAG_GPS_DEST_DISTANCE_REF,
        ExifInterface.TAG_GPS_AREA_INFORMATION,
        ExifInterface.TAG_GPS_DOP
    )

    val DEVICE_TAGS = listOf(
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_LENS_MAKE,
        ExifInterface.TAG_LENS_MODEL,
        ExifInterface.TAG_LENS_SPECIFICATION,
        ExifInterface.TAG_CAMERA_OWNER_NAME,
        ExifInterface.TAG_BODY_SERIAL_NUMBER
    )

    val CAPTURE_TAGS = listOf(
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_SUBSEC_TIME,
        ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
        ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
        ExifInterface.TAG_EXPOSURE_TIME,
        ExifInterface.TAG_F_NUMBER,
        ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
        ExifInterface.TAG_FOCAL_LENGTH,
        ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
        ExifInterface.TAG_FLASH,
        ExifInterface.TAG_WHITE_BALANCE,
        ExifInterface.TAG_METERING_MODE,
        ExifInterface.TAG_EXPOSURE_PROGRAM,
        ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
        ExifInterface.TAG_MAX_APERTURE_VALUE,
        ExifInterface.TAG_LIGHT_SOURCE,
        ExifInterface.TAG_SENSING_METHOD,
        ExifInterface.TAG_DIGITAL_ZOOM_RATIO
    )

    val OTHER_KNOWN_TAGS = listOf(
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_COPYRIGHT,
        ExifInterface.TAG_USER_COMMENT,
        ExifInterface.TAG_IMAGE_DESCRIPTION,
        ExifInterface.TAG_X_RESOLUTION,
        ExifInterface.TAG_Y_RESOLUTION,
        ExifInterface.TAG_RESOLUTION_UNIT,
        ExifInterface.TAG_CONTRAST,
        ExifInterface.TAG_SATURATION,
        ExifInterface.TAG_SHARPNESS,
        ExifInterface.TAG_SUBJECT_DISTANCE,
        ExifInterface.TAG_SUBJECT_DISTANCE_RANGE,
        ExifInterface.TAG_SPECTRAL_SENSITIVITY,
        ExifInterface.TAG_BRIGHTNESS_VALUE,
        ExifInterface.TAG_SHUTTER_SPEED_VALUE,
        ExifInterface.TAG_APERTURE_VALUE,
        ExifInterface.TAG_CFA_PATTERN
    )

    fun parse(inputStream: InputStream, uri: Uri, fileName: String, fileSize: Long, mimeType: String): PhotoMetadata {
        val exif = ExifInterface(inputStream)
        return parseFromExif(exif, uri, fileName, fileSize, mimeType)
    }

    fun parseFromExif(
        exif: ExifInterface,
        uri: Uri,
        fileName: String,
        fileSize: Long,
        mimeType: String
    ): PhotoMetadata {
        // Location
        val latLong = exif.latLong
        val lat = latLong?.get(0)
        val lng = latLong?.get(1)
        val alt = if (exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE) != null) {
            exif.getAltitude(0.0)
        } else null

        val formattedCoords = if (lat != null && lng != null) {
            val latDir = if (lat >= 0) "N" else "S"
            val lngDir = if (lng >= 0) "E" else "W"
            String.format(Locale.US, "%.5f° %s, %.5f° %s", Math.abs(lat), latDir, Math.abs(lng), lngDir)
        } else ""

        val mapsUri = if (lat != null && lng != null) {
            "geo:$lat,$lng?q=$lat,$lng($fileName)"
        } else ""

        val locationMeta = LocationMetadata(
            latitude = lat,
            longitude = lng,
            altitude = alt,
            formattedCoordinates = formattedCoords,
            mapsUri = mapsUri
        )

        // Device
        val deviceMeta = DeviceMetadata(
            make = exif.getAttribute(ExifInterface.TAG_MAKE),
            model = exif.getAttribute(ExifInterface.TAG_MODEL),
            software = exif.getAttribute(ExifInterface.TAG_SOFTWARE),
            lensModel = exif.getAttribute(ExifInterface.TAG_LENS_MODEL)
        )

        // Capture
        val captureMeta = CaptureMetadata(
            dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME),
            exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let { formatExposureTime(it) },
            fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { "f/$it" },
            iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
                ?: exif.getAttribute("ISOSpeedRatings"),
            focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let { "$it mm" },
            flash = formatFlash(exif.getAttributeInt(ExifInterface.TAG_FLASH, -1)),
            whiteBalance = formatWhiteBalance(exif.getAttributeInt(ExifInterface.TAG_WHITE_BALANCE, -1))
        )

        // Image
        val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0).takeIf { it > 0 }
            ?: exif.getAttributeInt(ExifInterface.TAG_PIXEL_X_DIMENSION, 0).takeIf { it > 0 }
        val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0).takeIf { it > 0 }
            ?: exif.getAttributeInt(ExifInterface.TAG_PIXEL_Y_DIMENSION, 0).takeIf { it > 0 }

        val orientationInt = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val (orientationDesc, orientationDegrees) = parseOrientation(orientationInt)

        val colorSpaceInt = exif.getAttributeInt(ExifInterface.TAG_COLOR_SPACE, -1)
        val colorSpaceDesc = when (colorSpaceInt) {
            ExifInterface.COLOR_SPACE_S_RGB -> "sRGB"
            ExifInterface.COLOR_SPACE_UNCALIBRATED -> "Uncalibrated / Wide Gamut"
            else -> exif.getAttribute(ExifInterface.TAG_COLOR_SPACE)
        }

        val imageMeta = ImageMetadata(
            width = width,
            height = height,
            orientation = orientationDesc,
            orientationDegrees = orientationDegrees,
            colorSpace = colorSpaceDesc,
            fileSize = fileSize,
            formattedFileSize = formatFileSize(fileSize),
            mimeType = mimeType,
            fileName = fileName
        )

        // Other tags
        val otherTags = mutableListOf<OtherTag>()
        for (tag in OTHER_KNOWN_TAGS) {
            val value = exif.getAttribute(tag)
            if (!value.isNullOrBlank()) {
                otherTags.add(OtherTag(tag, value))
            }
        }

        // Count tags found
        var tagCount = 0
        if (locationMeta.hasGps) tagCount += 2
        if (locationMeta.altitude != null) tagCount++
        if (deviceMeta.make != null) tagCount++
        if (deviceMeta.model != null) tagCount++
        if (deviceMeta.software != null) tagCount++
        if (deviceMeta.lensModel != null) tagCount++
        if (captureMeta.dateTime != null) tagCount++
        if (captureMeta.exposureTime != null) tagCount++
        if (captureMeta.fNumber != null) tagCount++
        if (captureMeta.iso != null) tagCount++
        if (captureMeta.focalLength != null) tagCount++
        tagCount += otherTags.size

        val privacyScore = when {
            locationMeta.hasGps -> PrivacyScore.CONTAINS_LOCATION
            tagCount > 0 -> PrivacyScore.CONTAINS_METADATA
            else -> PrivacyScore.CLEAN
        }

        return PhotoMetadata(
            uri = uri,
            fileName = fileName,
            fileSize = fileSize,
            mimeType = mimeType,
            location = locationMeta,
            device = deviceMeta,
            capture = captureMeta,
            image = imageMeta,
            otherTags = otherTags,
            privacyScore = privacyScore,
            totalTagCount = tagCount
        )
    }

    fun getFileDetails(context: Context, uri: Uri): Pair<String, Long> {
        var name = "photo.jpg"
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }
        } catch (_: Exception) {
            name = uri.lastPathSegment ?: "photo.jpg"
        }
        return Pair(name, size)
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(Locale.US, "%.2f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    private fun formatExposureTime(raw: String): String {
        return try {
            val d = raw.toDouble()
            if (d < 1.0 && d > 0.0) {
                val denominator = Math.round(1.0 / d)
                "1/$denominator s"
            } else {
                "$raw s"
            }
        } catch (_: Exception) {
            "$raw s"
        }
    }

    private fun formatFlash(flashCode: Int): String? {
        if (flashCode == -1) return null
        return if ((flashCode and 1) != 0) "Fired" else "Did not fire"
    }

    private fun formatWhiteBalance(wb: Int): String? {
        return when (wb) {
            ExifInterface.WHITE_BALANCE_AUTO.toInt() -> "Auto"
            ExifInterface.WHITE_BALANCE_MANUAL.toInt() -> "Manual"
            else -> null
        }
    }

    private fun parseOrientation(orientation: Int): Pair<String, Int> {
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> Pair("Rotated 90°", 90)
            ExifInterface.ORIENTATION_ROTATE_180 -> Pair("Rotated 180°", 180)
            ExifInterface.ORIENTATION_ROTATE_270 -> Pair("Rotated 270°", 270)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> Pair("Flipped horizontally", 0)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> Pair("Flipped vertically", 0)
            ExifInterface.ORIENTATION_TRANSPOSE -> Pair("Transposed", 90)
            ExifInterface.ORIENTATION_TRANSVERSE -> Pair("Transverse", 270)
            else -> Pair("Normal", 0)
        }
    }

    /**
     * Lossless JPEG APP marker stripper.
     * Removes APP1 (Exif/XMP), APP2, APP13 (IPTC), and COM markers while leaving image data intact.
     */
    fun stripJpegMarkersLossless(inputFile: java.io.File): Boolean {
        return try {
            val bytes = inputFile.readBytes()
            if (bytes.size < 4 || bytes[0] != 0xFF.toByte() || bytes[1] != 0xD8.toByte()) {
                return false // Not a valid JPEG SOI
            }

            val out = java.io.ByteArrayOutputStream(bytes.size)
            // Write SOI
            out.write(0xFF)
            out.write(0xD8)

            var i = 2
            while (i < bytes.size - 1) {
                if (bytes[i] == 0xFF.toByte()) {
                    val marker = bytes[i + 1].toInt() and 0xFF

                    // End of Image or Start of Scan -> write rest of file as-is
                    if (marker == 0xDA || marker == 0xD9) {
                        out.write(bytes, i, bytes.size - i)
                        break
                    }

                    // Standalone markers without length: RST0-7, SOI
                    if (marker in 0xD0..0xD7 || marker == 0x01) {
                        out.write(0xFF)
                        out.write(marker)
                        i += 2
                        continue
                    }

                    // Markers with 2-byte length
                    if (i + 3 >= bytes.size) break
                    val length = ((bytes[i + 2].toInt() and 0xFF) shl 8) or (bytes[i + 3].toInt() and 0xFF)

                    // Check if marker should be stripped:
                    // 0xE1 = APP1 (EXIF / XMP)
                    // 0xED = APP13 (IPTC / Photoshop)
                    // 0xFE = COM (Comment)
                    val shouldStrip = marker == 0xE1 || marker == 0xED || marker == 0xFE

                    if (!shouldStrip) {
                        out.write(bytes, i, length + 2)
                    }
                    i += length + 2
                } else {
                    out.write(bytes[i].toInt())
                    i++
                }
            }

            val resultBytes = out.toByteArray()
            if (resultBytes.size > 2) {
                inputFile.writeBytes(resultBytes)
                true
            } else false
        } catch (_: Exception) {
            false
        }
    }
}
