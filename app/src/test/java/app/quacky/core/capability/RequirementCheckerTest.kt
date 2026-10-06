package app.quacky.core.capability

import android.content.Intent
import app.quacky.R
import app.quacky.core.registry.ToolRegistry
import app.quacky.core.registry.ToolRequirement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeDeviceCapabilities(
    override val hasBackCamera: Boolean = true,
    override val hasFlashUnit: Boolean = true,
    override val hasAccelerometer: Boolean = true,
    override val hasVibrator: Boolean = true,
    override val arCoreStatus: ArCoreStatus = ArCoreStatus.SUPPORTED_AND_READY,
    override val isDisplayMetricsPlausible: Boolean = true
) : DeviceCapabilities {
    override fun canHandle(intent: Intent): Boolean = true
}

class RequirementCheckerTest {

    @Test
    fun `fully capable device returns Ready for all tools`() {
        val capabilities = FakeDeviceCapabilities()
        val checker = RequirementChecker(capabilities)

        ToolRegistry.allTools.forEach { tool ->
            val result = checker.check(tool, isRulerCalibrated = false)
            assertTrue("Expected Ready for ${tool.id}", result is RequirementResult.Ready)
        }
    }

    @Test
    fun `missing back camera reports missing for scanner and ar ruler`() {
        val capabilities = FakeDeviceCapabilities(hasBackCamera = false)
        val checker = RequirementChecker(capabilities)

        val scannerResult = checker.check(ToolRegistry.QR_SCANNER)
        assertTrue(scannerResult is RequirementResult.Missing)
        val missing = scannerResult as RequirementResult.Missing
        assertEquals(ToolRequirement.BACK_CAMERA, missing.requirement)
        assertEquals(R.string.req_missing_scanner_camera, missing.reasonRes)

        val arResult = checker.check(ToolRegistry.AR_RULER)
        assertTrue(arResult is RequirementResult.Missing)
        assertEquals(ToolRequirement.BACK_CAMERA, (arResult as RequirementResult.Missing).requirement)

        // Tools without camera requirement should remain Ready
        val textResult = checker.check(ToolRegistry.TEXT_COUNTER)
        assertTrue(textResult is RequirementResult.Ready)
    }

    @Test
    fun `unsupported arcore reports missing with no install button`() {
        val capabilities = FakeDeviceCapabilities(arCoreStatus = ArCoreStatus.UNSUPPORTED)
        val checker = RequirementChecker(capabilities)

        val result = checker.check(ToolRegistry.AR_RULER)
        assertTrue(result is RequirementResult.Missing)
        val missing = result as RequirementResult.Missing
        assertEquals(ToolRequirement.ARCORE, missing.requirement)
        assertEquals(R.string.req_missing_arcore_unsupported, missing.reasonRes)
        assertEquals(false, missing.isArSetupRequired)
    }

    @Test
    fun `arcore needs install reports missing with install button enabled`() {
        val capabilities = FakeDeviceCapabilities(arCoreStatus = ArCoreStatus.SUPPORTED_NEEDS_INSTALL_OR_UPDATE)
        val checker = RequirementChecker(capabilities)

        val result = checker.check(ToolRegistry.AR_RULER)
        assertTrue(result is RequirementResult.Missing)
        val missing = result as RequirementResult.Missing
        assertEquals(ToolRequirement.ARCORE, missing.requirement)
        assertEquals(R.string.req_missing_arcore_needs_install, missing.reasonRes)
        assertEquals(true, missing.isArSetupRequired)
    }

    @Test
    fun `implausible display metrics locks ruler when not calibrated`() {
        val capabilities = FakeDeviceCapabilities(isDisplayMetricsPlausible = false)
        val checker = RequirementChecker(capabilities)

        val uncalibratedResult = checker.check(ToolRegistry.SCREEN_RULER, isRulerCalibrated = false)
        assertTrue(uncalibratedResult is RequirementResult.Missing)
        val missing = uncalibratedResult as RequirementResult.Missing
        assertEquals(ToolRequirement.VALID_DISPLAY_METRICS, missing.requirement)
        assertEquals(R.string.req_missing_display_metrics, missing.reasonRes)

        // Once calibrated by user, ruler should be ready
        val calibratedResult = checker.check(ToolRegistry.SCREEN_RULER, isRulerCalibrated = true)
        assertTrue(calibratedResult is RequirementResult.Ready)
    }
}
