package app.quacky.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Ensures strict offline compliance for Quacky:
 * - Manifest MUST NOT declare android.permission.INTERNET
 * - Manifest MUST NOT declare network-related permissions
 */
class OfflineManifestTest {

    @Test
    fun manifestContainsZeroInternetPermissions() {
        val candidates = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml"),
            File("../app/src/main/AndroidManifest.xml")
        )
        val manifestFile = candidates.firstOrNull { it.exists() }
        assertTrue("AndroidManifest.xml should be discoverable", manifestFile != null && manifestFile.exists())

        val content = manifestFile!!.readText()

        assertFalse("Quacky must never request INTERNET permission", content.contains("android.permission.INTERNET"))
        assertFalse("Quacky must never request ACCESS_NETWORK_STATE permission", content.contains("android.permission.ACCESS_NETWORK_STATE"))
        assertFalse("Quacky must never request ACCESS_WIFI_STATE permission", content.contains("android.permission.ACCESS_WIFI_STATE"))
        assertFalse("Quacky must never request CHANGE_NETWORK_STATE permission", content.contains("android.permission.CHANGE_NETWORK_STATE"))
    }
}
