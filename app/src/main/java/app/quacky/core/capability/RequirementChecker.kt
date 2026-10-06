package app.quacky.core.capability

import androidx.annotation.StringRes
import app.quacky.R
import app.quacky.core.registry.ToolDefinition
import app.quacky.core.registry.ToolRegistry
import app.quacky.core.registry.ToolRequirement
import javax.inject.Inject
import javax.inject.Singleton

sealed interface RequirementResult {
    data object Ready : RequirementResult
    data class Missing(
        val requirement: ToolRequirement,
        @StringRes val reasonRes: Int,
        val isArSetupRequired: Boolean = false
    ) : RequirementResult
}

@Singleton
class RequirementChecker @Inject constructor(
    private val capabilities: DeviceCapabilities
) {
    fun check(tool: ToolDefinition, isRulerCalibrated: Boolean = false): RequirementResult {
        for (req in tool.requirements) {
            when (req) {
                ToolRequirement.BACK_CAMERA -> {
                    if (!capabilities.hasBackCamera) {
                        val messageRes = if (tool.id == ToolRegistry.QR_SCANNER.id) {
                            R.string.req_missing_scanner_camera
                        } else {
                            R.string.req_missing_back_camera
                        }
                        return RequirementResult.Missing(req, messageRes)
                    }
                }
                ToolRequirement.ARCORE -> {
                    when (capabilities.arCoreStatus) {
                        ArCoreStatus.SUPPORTED_AND_READY -> Unit
                        ArCoreStatus.SUPPORTED_NEEDS_INSTALL_OR_UPDATE -> {
                            return RequirementResult.Missing(
                                requirement = req,
                                reasonRes = R.string.req_missing_arcore_needs_install,
                                isArSetupRequired = true
                            )
                        }
                        ArCoreStatus.UNSUPPORTED -> {
                            return RequirementResult.Missing(
                                requirement = req,
                                reasonRes = R.string.req_missing_arcore_unsupported,
                                isArSetupRequired = false
                            )
                        }
                        ArCoreStatus.CHECKING -> {
                            return RequirementResult.Missing(
                                requirement = req,
                                reasonRes = R.string.req_missing_arcore_needs_install,
                                isArSetupRequired = true
                            )
                        }
                    }
                }
                ToolRequirement.VALID_DISPLAY_METRICS -> {
                    if (!isRulerCalibrated && !capabilities.isDisplayMetricsPlausible) {
                        return RequirementResult.Missing(
                            requirement = req,
                            reasonRes = R.string.req_missing_display_metrics,
                            isArSetupRequired = false
                        )
                    }
                }
                ToolRequirement.FLASH -> {
                    if (!capabilities.hasFlashUnit) {
                        return RequirementResult.Missing(req, R.string.req_missing_flash)
                    }
                }
                ToolRequirement.ACCELEROMETER -> {
                    if (!capabilities.hasAccelerometer) {
                        return RequirementResult.Missing(req, R.string.req_missing_accelerometer)
                    }
                }
                ToolRequirement.VIBRATOR -> {
                    if (!capabilities.hasVibrator) {
                        return RequirementResult.Missing(req, R.string.req_missing_vibrator)
                    }
                }
            }
        }
        return RequirementResult.Ready
    }
}
