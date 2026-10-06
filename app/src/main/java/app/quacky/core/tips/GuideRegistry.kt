package app.quacky.core.tips

import app.quacky.core.registry.ToolRegistry

object GuideRegistry {

    private val guides: Map<String, ToolGuide> = mapOf(
        ToolRegistry.QR_SCANNER.id to ToolGuide(
            toolId = ToolRegistry.QR_SCANNER.id,
            steps = listOf(
                TipStep(
                    visualType = VisualType.SCANNER_SWEEP,
                    title = "Point at the code",
                    body = "Hold your camera over a QR code or barcode. It scans by itself, no button to press.",
                    a11yDescription = "Camera viewfinder scanning a code"
                ),
                TipStep(
                    visualType = VisualType.PINCH_ZOOM,
                    title = "Dark or far away?",
                    body = "Tap the torch for light. Pinch the screen to zoom in.",
                    a11yDescription = "Pinch gesture to zoom into a code"
                ),
                TipStep(
                    visualType = VisualType.GALLERY_PICK,
                    title = "Scan from an image",
                    body = "Have a screenshot or saved photo? Tap the gallery icon and choose it.",
                    a11yDescription = "Selecting an image from the gallery to scan"
                ),
                TipStep(
                    visualType = VisualType.ACTION_SHEET,
                    title = "Choose what to do",
                    body = "After scanning, pick an action: open a link, copy text, join Wi-Fi, or save a contact.",
                    a11yDescription = "Result sheet displaying contextual actions"
                ),
                TipStep(
                    visualType = VisualType.LOCK_URL,
                    title = "Good to know",
                    body = "Links never open on their own. You always see the full address first.",
                    a11yDescription = "Secure preview of scanned link address"
                )
            )
        ),
        ToolRegistry.QR_GENERATOR.id to ToolGuide(
            toolId = ToolRegistry.QR_GENERATOR.id,
            steps = listOf(
                TipStep(
                    visualType = VisualType.CHIP_SELECT,
                    title = "Pick the type",
                    body = "Choose what the code is for. Each type gives you the right fields.",
                    a11yDescription = "Selecting QR code data type chips"
                ),
                TipStep(
                    visualType = VisualType.LIVE_TYPE,
                    title = "Fill in details",
                    body = "Type your details. The code updates as you type.",
                    a11yDescription = "Typing in form fields with live code preview"
                ),
                TipStep(
                    visualType = VisualType.EXPORT_FORMATS,
                    title = "Save or share",
                    body = "Save as PNG, SVG or PDF, or share it. Tap Save as template to reuse it later.",
                    a11yDescription = "Exporting code to PNG, SVG, or PDF"
                )
            )
        ),
        ToolRegistry.SCREEN_RULER.id to ToolGuide(
            toolId = ToolRegistry.SCREEN_RULER.id,
            steps = listOf(
                TipStep(
                    visualType = VisualType.RULER_ALIGN,
                    title = "Line it up",
                    body = "Lay your object flat on the screen with one end at 0.",
                    a11yDescription = "Aligning a physical item with on-screen zero"
                ),
                TipStep(
                    visualType = VisualType.RULER_CALIBRATE,
                    title = "Calibrate once",
                    body = "For the most accurate scale, calibrate once using any bank card.",
                    a11yDescription = "Bank card outline calibration slider"
                ),
                TipStep(
                    visualType = VisualType.RULER_MARKERS,
                    title = "Use the markers",
                    body = "Drag the two markers to measure the gap between them.",
                    a11yDescription = "Moving dual distance markers across the ruler"
                ),
                TipStep(
                    visualType = VisualType.RULER_FLIP,
                    title = "Left-handed?",
                    body = "Tap the flip icon to move 0 to the other edge.",
                    a11yDescription = "Ruler flipping edges for left-handed use"
                )
            )
        ),
        ToolRegistry.TEXT_COUNTER.id to ToolGuide(
            toolId = ToolRegistry.TEXT_COUNTER.id,
            steps = listOf(
                TipStep(
                    visualType = VisualType.TEXT_TYPE,
                    title = "Type or paste",
                    body = "Type, paste, or import a text file. Counts update live.",
                    a11yDescription = "Text area updating statistics as you type"
                ),
                TipStep(
                    visualType = VisualType.TEXT_OPTIONS,
                    title = "Choose what to count",
                    body = "Turn options on or off to count with or without spaces and more.",
                    a11yDescription = "Toggling punctuation, spaces, and emoji options"
                ),
                TipStep(
                    visualType = VisualType.TEXT_LIMIT,
                    title = "Check a limit",
                    body = "Pick a preset like X, Instagram or SMS to see how many characters you have left.",
                    a11yDescription = "Character count progress bar filling toward a platform limit"
                )
            )
        ),
        ToolRegistry.DATE_CALC.id to ToolGuide(
            toolId = ToolRegistry.DATE_CALC.id,
            steps = listOf(
                TipStep(
                    visualType = VisualType.DATE_TABS,
                    title = "Choose a calculation",
                    body = "Pick what you want to find out: Age, Difference, Add/Subtract, or Day Info.",
                    a11yDescription = "Selecting date calculator tabs"
                ),
                TipStep(
                    visualType = VisualType.DATE_PICK,
                    title = "Pick the dates",
                    body = "Choose dates from the calendar or type them in.",
                    a11yDescription = "Selecting calendar dates"
                ),
                TipStep(
                    visualType = VisualType.DATE_COUNTDOWN,
                    title = "Save a countdown",
                    body = "Save an important date and see how many days are left, any time you open this tool.",
                    a11yDescription = "Pinned countdown card showing days remaining"
                )
            )
        ),
        ToolRegistry.DICE.id to ToolGuide(
            toolId = ToolRegistry.DICE.id,
            steps = listOf(
                TipStep(
                    visualType = VisualType.DICE_CONFIG,
                    title = "Set up your dice",
                    body = "Choose how many dice and how many sides each has.",
                    a11yDescription = "Selecting dice sides and count"
                ),
                TipStep(
                    visualType = VisualType.DICE_ROLL,
                    title = "Roll",
                    body = "Tap Roll or shake your phone to generate a roll.",
                    a11yDescription = "Die rolling and settling on a number"
                ),
                TipStep(
                    visualType = VisualType.DICE_SAVE,
                    title = "Save favorites",
                    body = "Save a roll you use often and roll it again with one tap.",
                    a11yDescription = "Preset chips for quick rolls"
                )
            )
        ),
        ToolRegistry.COIN_FLIP.id to ToolGuide(
            toolId = ToolRegistry.COIN_FLIP.id,
            steps = listOf(
                TipStep(
                    visualType = VisualType.COIN_FLIP,
                    title = "Flip",
                    body = "Tap the coin to flip it.",
                    a11yDescription = "2D coin flipping and landing"
                ),
                TipStep(
                    visualType = VisualType.COIN_LABELS,
                    title = "Use your own labels",
                    body = "Rename the sides, like Yes and No, to settle a decision.",
                    a11yDescription = "Customizing Heads and Tails labels"
                ),
                TipStep(
                    visualType = VisualType.COIN_COUNTERS,
                    title = "Track results",
                    body = "See how many times each side came up. Reset any time.",
                    a11yDescription = "Flip session counter updating"
                )
            )
        ),
        ToolRegistry.RANDOM_NUMBER.id to ToolGuide(
            toolId = ToolRegistry.RANDOM_NUMBER.id,
            steps = listOf(
                TipStep(
                    visualType = VisualType.RNG_RANGE,
                    title = "Set the range",
                    body = "Enter the smallest and largest number you want, both included.",
                    a11yDescription = "Setting minimum and maximum numbers"
                ),
                TipStep(
                    visualType = VisualType.RNG_COUNT,
                    title = "How many?",
                    body = "Choose how many numbers to generate. Pick 2 for a quick pair.",
                    a11yDescription = "Number count stepper"
                ),
                TipStep(
                    visualType = VisualType.RNG_SEED,
                    title = "Use a seed (optional)",
                    body = "A seed is a starting code. Using the same seed always gives the same numbers for fair draws.",
                    a11yDescription = "Reproducible draw seed input"
                )
            )
        ),
        ToolRegistry.PICKER_WHEEL.id to ToolGuide(
            toolId = ToolRegistry.PICKER_WHEEL.id,
            steps = listOf(
                TipStep(
                    visualType = VisualType.WHEEL_OPTIONS,
                    title = "Add options",
                    body = "Type your options, one per line. The wheel updates as you add them.",
                    a11yDescription = "Adding wheel options"
                ),
                TipStep(
                    visualType = VisualType.WHEEL_SPIN,
                    title = "Spin",
                    body = "Tap Spin or flick the wheel. The slice under the pointer wins.",
                    a11yDescription = "Spinning wheel slowing down to stop"
                ),
                TipStep(
                    visualType = VisualType.WHEEL_ELIMINATE,
                    title = "Spin again without repeats",
                    body = "Tap Remove and spin again to eliminate winners one by one.",
                    a11yDescription = "Eliminating previous winner slice"
                )
            )
        ),
        ToolRegistry.TEAM_SPLITTER.id to ToolGuide(
            toolId = ToolRegistry.TEAM_SPLITTER.id,
            steps = listOf(
                TipStep(
                    visualType = VisualType.TEAM_NAMES,
                    title = "Add names",
                    body = "Type names, paste a list, or load a saved group.",
                    a11yDescription = "Entering player names"
                ),
                TipStep(
                    visualType = VisualType.TEAM_SPLIT,
                    title = "Choose teams or size",
                    body = "Pick how many teams, or how many people per team. Uneven groups are split as evenly as possible.",
                    a11yDescription = "Splitting into balanced teams"
                ),
                TipStep(
                    visualType = VisualType.TEAM_SHUFFLE,
                    title = "Shuffle",
                    body = "Tap Shuffle for a new random split.",
                    a11yDescription = "Shuffling teams into cards"
                )
            )
        )
    )

    fun getGuideForTool(toolId: String): ToolGuide {
        return guides[toolId] ?: ToolGuide(
            toolId = toolId,
            steps = listOf(
                TipStep(
                    visualType = VisualType.CHIP_SELECT,
                    title = "Getting Started",
                    body = "Configure your preferences and tap to run the tool offline.",
                    a11yDescription = "Overview of tool controls"
                )
            )
        )
    }

    val allGuides: List<ToolGuide> = ToolRegistry.allTools.map { getGuideForTool(it.id) }
}
