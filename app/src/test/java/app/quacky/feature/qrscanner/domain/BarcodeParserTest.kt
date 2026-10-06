package app.quacky.feature.qrscanner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BarcodeParserTest {

    @Test
    fun `test URL parsing`() {
        val parsed = BarcodeParser.parse("https://quacky.app/download")
        assertEquals(BarcodeType.URL, parsed.type)
        assertEquals("quacky.app", parsed.title)
        assertEquals("https://quacky.app/download", parsed.subtitle)
    }

    @Test
    fun `test Wi-Fi parsing`() {
        val wifiString = "WIFI:S:QuackyGuest;T:WPA;P:SuperSecret123;H:false;;"
        val parsed = BarcodeParser.parse(wifiString)
        assertEquals(BarcodeType.WIFI, parsed.type)
        assertEquals("QuackyGuest", parsed.title)
        assertEquals("SuperSecret123", parsed.details["Password"])
        assertEquals("WPA", parsed.details["Security"])
    }

    @Test
    fun `test UPI parsing`() {
        val upiString = "upi://pay?pa=solarquack@okhdfcbank&pn=SolarQuack&am=250.00&cu=INR&tn=Coffee"
        val parsed = BarcodeParser.parse(upiString)
        assertEquals(BarcodeType.UPI, parsed.type)
        assertEquals("solarquack@okhdfcbank", parsed.details["VPA / UPI ID"])
        assertEquals("SolarQuack", parsed.details["Payee Name"])
        assertEquals("INR 250.00", parsed.details["Amount"])
        assertEquals("Coffee", parsed.details["Note"])
    }

    @Test
    fun `test vCard contact parsing`() {
        val vcard = """
            BEGIN:VCARD
            VERSION:3.0
            FN:John Doe
            TEL:+1234567890
            EMAIL:john@example.com
            ORG:Quacky Inc
            END:VCARD
        """.trimIndent()

        val parsed = BarcodeParser.parse(vcard)
        assertEquals(BarcodeType.CONTACT, parsed.type)
        assertEquals("John Doe", parsed.title)
        assertEquals("+1234567890", parsed.details["Phone"])
        assertEquals("john@example.com", parsed.details["Email"])
    }

    @Test
    fun `test Product barcode parsing`() {
        val ean = "8901030383792"
        val parsed = BarcodeParser.parse(ean, formatName = "EAN_13")
        assertEquals(BarcodeType.PRODUCT, parsed.type)
        assertEquals(ean, parsed.title)
    }

    @Test
    fun `test Plain text fallback`() {
        val text = "A quick brown duck jumps over the lazy dog."
        val parsed = BarcodeParser.parse(text)
        assertEquals(BarcodeType.TEXT, parsed.type)
        assertEquals(text, parsed.rawValue)
        assertEquals("43 characters", parsed.subtitle)
    }
}
