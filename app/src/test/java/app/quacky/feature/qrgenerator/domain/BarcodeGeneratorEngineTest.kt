package app.quacky.feature.qrgenerator.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeGeneratorEngineTest {

    @Test
    fun `test QR code BitMatrix generation`() {
        val options = GeneratorOptions(
            format = GeneratorFormat.QR_CODE,
            width = 300,
            height = 300
        )
        val matrix = BarcodeGeneratorEngine.generateBitMatrix("https://quacky.app", options)
        assertNotNull(matrix)
        assertTrue(matrix.width > 0)
        assertTrue(matrix.height > 0)
    }

    @Test
    fun `test SVG export string`() {
        val options = GeneratorOptions(
            format = GeneratorFormat.QR_CODE,
            width = 100,
            height = 100
        )
        val matrix = BarcodeGeneratorEngine.generateBitMatrix("Hello Quacky", options)
        val svg = BarcodeGeneratorEngine.exportToSvg(matrix, options)
        assertTrue(svg.startsWith("<svg"))
        assertTrue(svg.endsWith("</svg>"))
        assertTrue(svg.contains("<rect"))
        // By default includeLogo is true for QR codes, so duck path is rendered
        assertTrue(svg.contains("M 120,280"))
    }

    @Test
    fun `test SVG export without logo`() {
        val options = GeneratorOptions(
            format = GeneratorFormat.QR_CODE,
            width = 100,
            height = 100,
            includeLogo = false
        )
        val matrix = BarcodeGeneratorEngine.generateBitMatrix("Hello Quacky", options)
        val svg = BarcodeGeneratorEngine.exportToSvg(matrix, options)
        assertTrue(svg.startsWith("<svg"))
        assertTrue(svg.endsWith("</svg>"))
        assertTrue(svg.contains("<rect"))
        assertTrue(!svg.contains("M 120,280"))
    }

    @Test
    fun `test SVG export 1D barcode does not include logo`() {
        val options = GeneratorOptions(
            format = GeneratorFormat.CODE_128,
            width = 300,
            height = 100,
            includeLogo = true
        )
        val matrix = BarcodeGeneratorEngine.generateBitMatrix("QUACKY123", options)
        val svg = BarcodeGeneratorEngine.exportToSvg(matrix, options)
        assertTrue(!svg.contains("M 120,280"))
    }

    @Test
    fun `test EAN-13 check digit automatic calculation`() {
        // Given 12 digits: 890103038379
        val res = BarcodeGeneratorEngine.validateAndSanitizeInput("890103038379", GeneratorFormat.EAN_13)
        assertTrue(res.isSuccess)
        val sanitized = res.getOrThrow()
        assertEquals(13, sanitized.length)
        assertEquals("8901030383793", sanitized)
    }

    @Test
    fun `test EAN-8 check digit automatic calculation`() {
        // Given 7 digits: 9638507
        val res = BarcodeGeneratorEngine.validateAndSanitizeInput("9638507", GeneratorFormat.EAN_8)
        assertTrue(res.isSuccess)
        val sanitized = res.getOrThrow()
        assertEquals(8, sanitized.length)
        assertEquals("96385074", sanitized)
    }

    @Test
    fun `test Code 39 valid and invalid chars`() {
        val valid = BarcodeGeneratorEngine.validateAndSanitizeInput("QUACKY-123", GeneratorFormat.CODE_39)
        assertTrue(valid.isSuccess)

        val invalid = BarcodeGeneratorEngine.validateAndSanitizeInput("invalid!char@", GeneratorFormat.CODE_39)
        assertTrue(invalid.isFailure)
    }
}
