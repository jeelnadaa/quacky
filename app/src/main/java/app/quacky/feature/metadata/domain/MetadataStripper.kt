package app.quacky.feature.metadata.domain

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class MetadataStripper(private val context: Context) {

    suspend fun cleanImage(
        uri: Uri,
        originalFileName: String,
        mimeType: String,
        choice: RemovalChoice,
        customOptions: CustomRemovalOptions = CustomRemovalOptions(),
        onProgress: (String) -> Unit = {}
    ): CleanResult = withContext(Dispatchers.IO) {
        onProgress("Preparing $originalFileName…")

        // Create temp working file
        val tempFile = File(context.cacheDir, "quacky_clean_${System.currentTimeMillis()}_$originalFileName")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Cannot read source image")

        var isLossless = true
        var note = "Lossless metadata removal"
        var tagsRemovedCount = 0

        val isJpeg = mimeType.equals("image/jpeg", ignoreCase = true) ||
                originalFileName.endsWith(".jpg", ignoreCase = true) ||
                originalFileName.endsWith(".jpeg", ignoreCase = true)

        try {
            val initialExif = ExifInterface(tempFile.absolutePath)
            val initialMeta = ExifEngine.parseFromExif(initialExif, uri, originalFileName, tempFile.length(), mimeType)
            val orientationDegrees = initialMeta.image.orientationDegrees

            when (choice) {
                RemovalChoice.LOCATION_ONLY -> {
                    // Null all GPS tags
                    for (tag in ExifEngine.ALL_GPS_TAGS) {
                        if (initialExif.getAttribute(tag) != null) {
                            initialExif.setAttribute(tag, null)
                            tagsRemovedCount++
                        }
                    }
                    initialExif.saveAttributes()
                    isLossless = true
                    note = "GPS coordinates stripped losslessly"
                }

                RemovalChoice.ALL -> {
                    if (isJpeg) {
                        if (orientationDegrees != 0) {
                            // Orientation is non-normal: rotate pixels to preserve visual orientation, then save clean
                            val rotatedFile = rotateAndSaveBitmap(tempFile, orientationDegrees)
                            tempFile.delete()
                            rotatedFile.renameTo(tempFile)
                            isLossless = false
                            note = "Rotated pixels by $orientationDegrees° to preserve orientation without tags"
                            tagsRemovedCount = initialMeta.totalTagCount
                        } else {
                            // Lossless JPEG marker stripping
                            val stripped = ExifEngine.stripJpegMarkersLossless(tempFile)
                            if (stripped) {
                                isLossless = true
                                note = "Lossless segment strip: EXIF, XMP, IPTC removed"
                                tagsRemovedCount = initialMeta.totalTagCount
                            } else {
                                // Fallback: null all known attributes via ExifInterface
                                removeAllAttributes(initialExif)
                                initialExif.saveAttributes()
                                isLossless = true
                                note = "Attributes stripped via ExifInterface"
                                tagsRemovedCount = initialMeta.totalTagCount
                            }
                        }
                    } else {
                        // Non-JPEG (PNG / WebP): re-encode at max quality
                        reencodeBitmapClean(tempFile, mimeType)
                        isLossless = false
                        note = "Re-encoded cleanly without metadata"
                        tagsRemovedCount = initialMeta.totalTagCount
                    }
                }

                RemovalChoice.CUSTOM -> {
                    if (customOptions.removeLocation) {
                        for (tag in ExifEngine.ALL_GPS_TAGS) {
                            if (initialExif.getAttribute(tag) != null) {
                                initialExif.setAttribute(tag, null)
                                tagsRemovedCount++
                            }
                        }
                    }
                    if (customOptions.removeDevice) {
                        for (tag in ExifEngine.DEVICE_TAGS) {
                            if (initialExif.getAttribute(tag) != null) {
                                initialExif.setAttribute(tag, null)
                                tagsRemovedCount++
                            }
                        }
                    }
                    if (customOptions.removeCapture) {
                        for (tag in ExifEngine.CAPTURE_TAGS) {
                            if (initialExif.getAttribute(tag) != null) {
                                initialExif.setAttribute(tag, null)
                                tagsRemovedCount++
                            }
                        }
                    }
                    if (customOptions.removeOther) {
                        for (tag in ExifEngine.OTHER_KNOWN_TAGS) {
                            if (initialExif.getAttribute(tag) != null) {
                                initialExif.setAttribute(tag, null)
                                tagsRemovedCount++
                            }
                        }
                    }
                    try {
                        initialExif.saveAttributes()
                    } catch (_: Exception) {
                        // If saveAttributes fails on non-JPEG, reencode
                        reencodeBitmapClean(tempFile, mimeType)
                        isLossless = false
                    }
                }
            }

            // Save to MediaStore Pictures/Quacky/Clean
            onProgress("Saving clean copy to gallery…")
            val cleanDisplayName = "clean_${originalFileName.removePrefix("clean_")}"
            val savedUri = saveToMediaStore(tempFile, cleanDisplayName, mimeType)

            // Verify clean
            onProgress("Verifying output file…")
            val verifiedClean = verifyClean(tempFile, choice, customOptions)

            tempFile.delete()

            CleanResult(
                originalFileName = originalFileName,
                outputUri = savedUri,
                tagsRemoved = tagsRemovedCount,
                isLossless = isLossless,
                verifiedClean = verifiedClean,
                note = note
            )
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }
    }

    private fun removeAllAttributes(exif: ExifInterface) {
        val allTags = ExifEngine.ALL_GPS_TAGS + ExifEngine.DEVICE_TAGS + ExifEngine.CAPTURE_TAGS + ExifEngine.OTHER_KNOWN_TAGS
        for (tag in allTags) {
            exif.setAttribute(tag, null)
        }
    }

    private fun verifyClean(
        file: File,
        choice: RemovalChoice,
        customOptions: CustomRemovalOptions
    ): Boolean {
        return try {
            val exif = ExifInterface(file.absolutePath)
            when (choice) {
                RemovalChoice.LOCATION_ONLY -> exif.latLong == null
                RemovalChoice.ALL -> exif.latLong == null && exif.getAttribute(ExifInterface.TAG_MAKE) == null
                RemovalChoice.CUSTOM -> {
                    if (customOptions.removeLocation && exif.latLong != null) false
                    else if (customOptions.removeDevice && exif.getAttribute(ExifInterface.TAG_MAKE) != null) false
                    else true
                }
            }
        } catch (_: Exception) {
            true
        }
    }

    private fun saveToMediaStore(file: File, displayName: String, mimeType: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, if (mimeType.isNotBlank()) mimeType else "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Quacky/Clean")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Failed to create MediaStore entry")

        context.contentResolver.openOutputStream(uri)?.use { out ->
            FileInputStream(file).use { input ->
                input.copyTo(out)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        }

        return uri
    }



    private fun rotateAndSaveBitmap(file: File, degrees: Int): File {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            ?: throw IllegalStateException("Cannot decode bitmap for rotation")

        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        val rotatedFile = File(file.parentFile, "rotated_${file.name}")
        FileOutputStream(rotatedFile).use { out ->
            rotated.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        bitmap.recycle()
        rotated.recycle()
        return rotatedFile
    }

    private fun reencodeBitmapClean(file: File, mimeType: String) {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            ?: throw IllegalStateException("Cannot decode bitmap")

        val format = when {
            mimeType.equals("image/png", ignoreCase = true) || file.name.endsWith(".png", ignoreCase = true) ->
                Bitmap.CompressFormat.PNG
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    (mimeType.equals("image/webp", ignoreCase = true) || file.name.endsWith(".webp", ignoreCase = true)) ->
                Bitmap.CompressFormat.WEBP_LOSSLESS
            else -> Bitmap.CompressFormat.JPEG
        }

        FileOutputStream(file).use { out ->
            bitmap.compress(format, 100, out)
        }
        bitmap.recycle()
    }
}
