package app.quacky.feature.arruler.engine

/**
 * Central configuration holding every tunable threshold and constant for the AR measurement engine.
 */
object MeasureTuning {

    // 5. Tracking and Motion
    const val MIN_PARALLAX_LATERAL_METERS = 0.15
    const val MAX_LINEAR_SPEED_MPS = 0.50
    const val MAX_ANGULAR_SPEED_RADPS = 1.00

    const val MIN_TARGET_DISTANCE_METERS = 0.25
    const val WARN_TARGET_DISTANCE_METERS = 5.00
    const val MAX_TARGET_DISTANCE_METERS = 8.00

    // 6. Depth Sampling Window
    const val DEPTH_WINDOW_RADIUS_DEFAULT = 7 // 15x15
    const val DEPTH_WINDOW_RADIUS_CLOSE = 4   // 9x9 when < 0.6 m
    const val DEPTH_CLOSE_THRESHOLD_METERS = 0.60
    const val DEPTH_MIN_CONFIDENCE_THRESHOLD = 0.50f * 255f
    const val DEPTH_MIN_RANGE_METERS = 0.20
    const val DEPTH_MAX_RANGE_METERS = 8.00

    // Depth Readiness
    const val DEPTH_READY_MIN_VALID_RATIO = 0.30
    const val DEPTH_READY_MIN_MEAN_CONFIDENCE = 0.50

    // 7. Plane Fitting RANSAC
    const val RANSAC_ITERATIONS = 48
    const val RANSAC_BASE_THRESHOLD_METERS = 0.004
    const val RANSAC_DISTANCE_SCALE = 0.006
    const val PLANE_MIN_INLIER_RATIO = 0.55
    const val PLANE_MAX_RMS_RATIO = 0.50

    // 8. Fusion
    const val FUSION_BASE_TOLERANCE_METERS = 0.015
    const val FUSION_DISTANCE_SCALE = 0.015
    const val LARGE_PLANE_MIN_EXTENT_SQM = 1.00
    const val DISAGREEMENT_HIGH_QUALITY_FIT = 0.70

    // 8.4 Gravity Surface Classification
    const val SURFACE_FLOOR_COS_UP = 0.90
    const val SURFACE_CEILING_COS_UP = -0.90
    const val SURFACE_WALL_MAX_COS_UP = 0.15
    const val FLOOR_PROXIMITY_METERS = 0.10
    const val TOP_SURFACE_MIN_HEIGHT_ABOVE_FLOOR_METERS = 0.15

    // 9. Snapping
    const val SNAP_PLANES_MAX_DISTANCE_METERS = 0.10
    const val SNAP_PLANES_MIN_ANGLE_DEG = 30.0
    const val SNAP_TWO_PLANES_LINE_MAX_DIST_METERS = 0.04
    const val SNAP_THREE_PLANES_VERTEX_MAX_DIST_METERS = 0.06
    const val SNAP_THREE_PLANES_MIN_DET = 0.20

    const val SNAP_EDGE_SEARCH_RADIUS_DP = 14
    const val SNAP_EDGE_DEPTH_DISCONTINUITY_BASE_METERS = 0.03
    const val SNAP_EDGE_DEPTH_DISCONTINUITY_SCALE = 0.03
    const val SNAP_RELEASE_DISTANCE_MULTIPLIER = 1.5

    // 10. Point Stabilization
    const val STABILIZER_BUFFER_SIZE = 12
    const val TARGET_CHANGE_DISTANCE_METERS = 0.015 // 1.5 cm
    const val MAD_OUTLIER_MULTIPLIER = 3.0
    const val MAD_BASE_OFFSET_METERS = 0.002
    const val LOCKED_MIN_SAMPLES = 6
    const val LOCKED_MAX_JITTER_METERS = 0.003 // 3 mm
    const val LOCKED_MIN_DURATION_MS = 300L

    // 11A. Live Line Hysteresis and Refresh
    const val LIVE_VALUE_HYSTERESIS_METERS = 0.0005 // 0.5 mm
    const val LIVE_MAX_UPDATE_RATE_HZ = 10
    const val LIVE_STALE_TIMEOUT_MS = 500L
    const val LEVEL_PLUMB_TOLERANCE_DEG = 2.0
    const val CLOSE_SHAPE_SNAP_DISTANCE_METERS = 0.03 // 3 cm

    // 12. Confidence Scoring
    const val CONFIDENCE_HIGH_THRESHOLD = 0.75f
    const val CONFIDENCE_MEDIUM_THRESHOLD = 0.50f
    const val DRIFT_DECAY_RATE_PER_METER = 0.99
    const val DRIFT_LONG_TIME_PENALTY = 0.90
    const val DRIFT_LONG_TIME_THRESHOLD_MS = 20000L // 20 s

    // Weights for geometric mean (sum to 1.0)
    const val CONF_W_TRACKING = 0.15
    const val CONF_W_DEPTH_QUALITY = 0.15
    const val CONF_W_FIT_QUALITY = 0.15
    const val CONF_W_RANGE = 0.15
    const val CONF_W_STABILITY = 0.15
    const val CONF_W_AGREEMENT = 0.10
    const val CONF_W_PARALLAX = 0.10
    const val CONF_W_SURFACE_TYPE = 0.05

    // 13. Scale Check
    const val SCALE_FACTOR_MIN = 0.90
    const val SCALE_FACTOR_MAX = 1.10
    const val CARD_LENGTH_METERS = 0.08560
    const val A4_LONG_EDGE_METERS = 0.2970
    const val A4_SHORT_EDGE_METERS = 0.2100
    const val US_LETTER_LONG_METERS = 0.2794
    const val US_LETTER_SHORT_METERS = 0.2159
}
