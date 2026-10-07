package app.quacky.feature.documentscanner.domain

/**
 * All tunable parameters for document scanner edge detection and refinement.
 */
object ScanTuning {

    // Resolution Targets
    const val ANALYSIS_TARGET_WIDTH = 1280
    const val ANALYSIS_TARGET_HEIGHT = 960
    const val DETECTION_LONG_EDGE = 640

    // Frame Border Safety Margin (pixels at detection resolution)
    const val FRAME_BORDER_MARGIN_PX = 8

    // Morphological Kernel Sizes
    const val ILLUMINATION_BLUR_KSIZE = 51
    const val DENOISE_BLUR_KSIZE = 3
    const val CLOSE_KERNEL_SIZE = 3
    const val ERODE_KERNEL_SIZE = 3

    // Contour & Quad Geometric Filters
    const val MIN_CONTOUR_AREA_FRACTION = 0.12f
    const val MAX_CONTOUR_AREA_FRACTION = 0.98f
    const val MIN_INTERIOR_ANGLE_DEG = 55.0
    const val MAX_INTERIOR_ANGLE_DEG = 125.0
    const val MIN_OPPOSITE_SIDE_RATIO = 0.40
    const val MAX_OPPOSITE_SIDE_RATIO = 2.50

    // ApproxPolyDP Epsilon Sweeps (multiplied by contour perimeter)
    val EPSILON_SWEEPS = doubleArrayOf(0.008, 0.012, 0.02, 0.03, 0.04)

    // Line Candidate Segment Clustering
    const val LINE_MERGE_ANGLE_DIFF_DEG = 3.0
    const val LINE_MERGE_OFFSET_PX = 4.0
    const val LINE_MERGE_GAP_PX = 25.0

    // Quad Scoring Weights
    const val WEIGHT_MEAN_EDGE_SUPPORT = 0.38f
    const val WEIGHT_MIN_EDGE_SUPPORT = 0.22f
    const val WEIGHT_POLARITY = 0.15f
    const val WEIGHT_CONTRAST = 0.10f
    const val WEIGHT_GEOMETRY = 0.10f
    const val WEIGHT_TEMPORAL = 0.05f

    // Temporal Hysteresis
    const val TRACKED_SWITCH_THRESHOLD = 0.15f
    const val TRACKED_SWITCH_CONSECUTIVE_FRAMES = 3

    // Sub-Pixel Refinement
    const val REFINEMENT_SAMPLES_PER_SIDE = 50
    const val REFINEMENT_CORNER_MARGIN_RATIO = 0.10f
    const val REFINEMENT_WINDOW_DIAGONAL_RATIO = 0.015f
    const val REFINEMENT_MIN_WINDOW_PX = 6
    const val REFINEMENT_PEAK_THRESHOLD_RATIO = 0.60f
    const val REFINEMENT_MIN_INLIER_RATIO = 0.40f
    const val REFINEMENT_MAX_DEVIATION_DIAGONAL_RATIO = 0.04f

    // One Euro Filter (Live Corner Smoothing)
    const val ONE_EURO_MIN_CUTOFF_HZ = 1.0f
    const val ONE_EURO_BETA = 0.02f
    const val ONE_EURO_DERIVATIVE_CUTOFF_HZ = 1.0f
    const val ONE_EURO_RESET_JUMP_DIAGONAL_RATIO = 0.08f

    // Auto-Capture Lock Condition
    const val LOCKED_MIN_CONSECUTIVE_FRAMES = 5
    const val LOCKED_MIN_IOU = 0.95f
    const val LOCKED_MIN_SCORE = 0.70f
}
