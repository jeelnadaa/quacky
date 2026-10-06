package app.quacky.core.navigation

object NavRoutes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val EASTER_EGG = "easter_egg"
    const val ALL_GUIDES = "all_guides"

    fun category(id: String) = "category/$id"
    fun tool(id: String) = "tool/$id"

    const val CATEGORY_PATTERN = "category/{categoryId}"
    const val TOOL_PATTERN = "tool/{toolId}"
}
