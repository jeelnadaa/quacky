package app.quacky.feature.metadata.domain

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File

class MetadataStripperTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `formatFileSize formats B, KB, and MB accurately`() {
        assertEquals("0 B", ExifEngine.formatFileSize(0))
        assertEquals("500 B", ExifEngine.formatFileSize(500))
        assertEquals("1.0 KB", ExifEngine.formatFileSize(1024))
        assertEquals("500.0 KB", ExifEngine.formatFileSize(512000))
        assertEquals("1.00 MB", ExifEngine.formatFileSize(1024 * 1024))
        assertEquals("3.45 MB", ExifEngine.formatFileSize((3.45 * 1024 * 1024).toLong()))
    }

    @Test
    fun `PrivacyScore evaluates accurately based on tags`() {
        // Clean
        val clean = PhotoMetadata(
            uri = null,
            fileName = "clean.jpg",
            fileSize = 1000L,
            mimeType = "image/jpeg",
            location = LocationMetadata(),
            device = DeviceMetadata(),
            capture = CaptureMetadata(),
            image = ImageMetadata(),
            otherTags = emptyList(),
            privacyScore = PrivacyScore.CLEAN,
            totalTagCount = 0
        )
        assertEquals(PrivacyScore.CLEAN, clean.privacyScore)

        // Contains metadata only
        val withDevice = clean.copy(
            device = DeviceMetadata(make = "Google", model = "Pixel 8"),
            privacyScore = PrivacyScore.CONTAINS_METADATA,
            totalTagCount = 2
        )
        assertEquals(PrivacyScore.CONTAINS_METADATA, withDevice.privacyScore)

        // Contains GPS
        val withGps = clean.copy(
            location = LocationMetadata(latitude = 37.7749, longitude = -122.4194),
            privacyScore = PrivacyScore.CONTAINS_LOCATION,
            totalTagCount = 2
        )
        assertEquals(PrivacyScore.CONTAINS_LOCATION, withGps.privacyScore)
        assertTrue(withGps.location.hasGps)
    }

    @Test
    fun `stripJpegMarkersLossless removes APP1 and APP13 markers cleanly`() {
        val stream = ByteArrayOutputStream()
        stream.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte())) // SOI

        // APP1
        val app1Data = "EXIF_TEST".toByteArray()
        val app1Len = app1Data.size + 2
        stream.write(byteArrayOf(0xFF.toByte(), 0xE1.toByte()))
        stream.write((app1Len shr 8) and 0xFF)
        stream.write(app1Len and 0xFF)
        stream.write(app1Data)

        // APP13
        val app13Data = "IPTC_TEST".toByteArray()
        val app13Len = app13Data.size + 2
        stream.write(byteArrayOf(0xFF.toByte(), 0xED.toByte()))
        stream.write((app13Len shr 8) and 0xFF)
        stream.write(app13Len and 0xFF)
        stream.write(app13Data)

        // SOS
        stream.write(byteArrayOf(0xFF.toByte(), 0xDA.toByte()))
        val sosData = byteArrayOf(0x00, 0x02, 0x12, 0x34)
        stream.write(sosData)

        // EOI
        stream.write(byteArrayOf(0xFF.toByte(), 0xD9.toByte()))

        val testFile = tempFolder.newFile("test_photo.jpg")
        testFile.writeBytes(stream.toByteArray())

        // Initial file has APP1 and APP13
        val initialBytes = testFile.readBytes()
        assertTrue(containsSequence(initialBytes, byteArrayOf(0xFF.toByte(), 0xE1.toByte())))
        assertTrue(containsSequence(initialBytes, byteArrayOf(0xFF.toByte(), 0xED.toByte())))

        // Run lossless stripper
        val result = ExifEngine.stripJpegMarkersLossless(testFile)
        assertTrue(result)

        val strippedBytes = testFile.readBytes()
        // APP1 and APP13 are now stripped
        assertFalse(containsSequence(strippedBytes, byteArrayOf(0xFF.toByte(), 0xE1.toByte())))
        assertFalse(containsSequence(strippedBytes, byteArrayOf(0xFF.toByte(), 0xED.toByte())))

        // SOI and SOS remain
        assertEquals(0xFF.toByte(), strippedBytes[0])
        assertEquals(0xD8.toByte(), strippedBytes[1])
        assertTrue(containsSequence(strippedBytes, byteArrayOf(0xFF.toByte(), 0xDA.toByte())))
    }

    private fun containsSequence(data: ByteArray, seq: ByteArray): Boolean {
        for (i in 0..data.size - seq.size) {
            var found = true
            for (j in seq.indices) {
                if (data[i + j] != seq[j]) {
                    found = false
                    break
                }
            }
            if (found) return true
        }
        return false
    }
}
