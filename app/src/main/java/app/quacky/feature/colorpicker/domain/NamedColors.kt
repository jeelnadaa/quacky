package app.quacky.feature.colorpicker.domain

data class NamedColorEntry(
    val name: String,
    val hex: String,
    val colorInt: Int
)

object NamedColors {

    private val colorList: List<NamedColorEntry> by lazy {
        rawColors.map { (name, hex) ->
            val colorInt = ColorMath.parseHex(hex)
            NamedColorEntry(name, hex, colorInt)
        }
    }

    /**
     * Finds the nearest color name using CIELAB Delta E Euclidean distance.
     * Returns Pair(Name, DeltaE distance).
     */
    fun findNearest(colorInt: Int): Pair<String, Float> {
        var minDistance = Float.MAX_VALUE
        var closestName = "Unknown"

        for (entry in colorList) {
            val dist = ColorMath.deltaE(colorInt, entry.colorInt)
            if (dist < minDistance) {
                minDistance = dist
                closestName = entry.name
                if (dist == 0f) break
            }
        }

        return Pair(closestName, minDistance)
    }

    // Curated rich color names covering the entire spectrum
    private val rawColors = listOf(
        "Black" to "#000000",
        "Night" to "#0C090A",
        "Charcoal" to "#343837",
        "Oil" to "#3B3131",
        "Dark Slate Gray" to "#2F4F4F",
        "Slate Gray" to "#708090",
        "Light Slate Gray" to "#778899",
        "Dim Gray" to "#696969",
        "Gray" to "#808080",
        "Dark Gray" to "#A9A9A9",
        "Silver" to "#C0C0C0",
        "Light Gray" to "#D3D3D3",
        "Gainsboro" to "#DCDCDC",
        "White Smoke" to "#F5F5F5",
        "Ghost White" to "#F8F8FF",
        "Alice Blue" to "#F0F8FF",
        "Snow" to "#FFFAFA",
        "White" to "#FFFFFF",
        "Maroon" to "#800000",
        "Dark Red" to "#8B0000",
        "Brown" to "#A52A2A",
        "Firebrick" to "#B22222",
        "Crimson" to "#DC143C",
        "Red" to "#FF0000",
        "Tomato" to "#FF6347",
        "Coral" to "#FF7F50",
        "Indian Red" to "#CD5C5C",
        "Light Coral" to "#F08080",
        "Dark Salmon" to "#E9967A",
        "Salmon" to "#FA8072",
        "Light Salmon" to "#FFA07A",
        "Orange Red" to "#FF4500",
        "Dark Orange" to "#FF8C00",
        "Orange" to "#FFA500",
        "Amber" to "#FFBF00",
        "Gold" to "#FFD700",
        "Dark Goldenrod" to "#B8860B",
        "Goldenrod" to "#DAA520",
        "Pale Goldenrod" to "#EEE8AA",
        "Dark Khaki" to "#BDB76B",
        "Khaki" to "#F0E68C",
        "Olive" to "#808000",
        "Yellow" to "#FFFF00",
        "Yellow Green" to "#9ACD32",
        "Dark Olive Green" to "#556B2F",
        "Olive Drab" to "#6B8E23",
        "Lawn Green" to "#7CFC00",
        "Chartreuse" to "#7FFF00",
        "Green Yellow" to "#ADFF2F",
        "Dark Green" to "#006400",
        "Green" to "#008000",
        "Forest Green" to "#228B22",
        "Lime" to "#00FF00",
        "Lime Green" to "#32CD32",
        "Light Green" to "#90EE90",
        "Pale Green" to "#98FB98",
        "Dark Sea Green" to "#8FBC8F",
        "Medium Spring Green" to "#00FA9A",
        "Spring Green" to "#00FF7F",
        "Sea Green" to "#2E8B57",
        "Medium Sea Green" to "#3CB371",
        "Light Sea Green" to "#20B2AA",
        "Dark Slate Blue" to "#483D8B",
        "Teal" to "#008080",
        "Dark Cyan" to "#008B8B",
        "Aqua" to "#00FFFF",
        "Cyan" to "#00FFFF",
        "Light Cyan" to "#E0FFFF",
        "Dark Turquoise" to "#00CED1",
        "Turquoise" to "#40E0D0",
        "Medium Turquoise" to "#48D1CC",
        "Pale Turquoise" to "#AFEEEE",
        "Aquamarine" to "#7FFFD4",
        "Powder Blue" to "#B0E0E6",
        "Cadet Blue" to "#5F9EA0",
        "Steel Blue" to "#4682B4",
        "Cornflower Blue" to "#6495ED",
        "Deep Sky Blue" to "#00BFFF",
        "Dodger Blue" to "#1E90FF",
        "Light Blue" to "#ADD8E6",
        "Sky Blue" to "#87CEEB",
        "Light Sky Blue" to "#87CEFA",
        "Midnight Blue" to "#191970",
        "Navy" to "#000080",
        "Dark Blue" to "#00008B",
        "Medium Blue" to "#0000CD",
        "Blue" to "#0000FF",
        "Royal Blue" to "#4169E1",
        "Blue Violet" to "#8A2BE2",
        "Indigo" to "#4B0082",
        "Slate Blue" to "#6A5ACD",
        "Medium Slate Blue" to "#7B68EE",
        "Medium Purple" to "#9370DB",
        "Dark Magenta" to "#8B008B",
        "Dark Violet" to "#9400D3",
        "Dark Orchid" to "#9932CC",
        "Medium Orchid" to "#BA55D3",
        "Purple" to "#800080",
        "Thistle" to "#D8BFD8",
        "Plum" to "#DDA0DD",
        "Violet" to "#EE82EE",
        "Magenta" to "#FF00FF",
        "Fuchsia" to "#FF00FF",
        "Orchid" to "#DA70D6",
        "Medium Violet Red" to "#C71585",
        "Pale Violet Red" to "#DB7093",
        "Deep Pink" to "#FF1493",
        "Hot Pink" to "#FF69B4",
        "Light Pink" to "#FFB6C1",
        "Pink" to "#FFC0CB",
        "Antique White" to "#FAEBD7",
        "Beige" to "#F5F5DC",
        "Bisque" to "#FFE4C4",
        "Blanched Almond" to "#FFEBCD",
        "Wheat" to "#F5DEB3",
        "Cornsilk" to "#FFF8DC",
        "Lemon Chiffon" to "#FFFACD",
        "Light Goldenrod Yellow" to "#FAFAD2",
        "Light Yellow" to "#FFFFE0",
        "Saddle Brown" to "#8B4513",
        "Sienna" to "#A0522D",
        "Chocolate" to "#D2691E",
        "Peru" to "#CD853F",
        "Sandy Brown" to "#F4A460",
        "Burly Wood" to "#DEB887",
        "Tan" to "#D2B48C",
        "Rosy Brown" to "#BC8F8F",
        "Moccasin" to "#FFE4B5",
        "Navajo White" to "#FFDEAD",
        "Peach Puff" to "#FFDAB9",
        "Misty Rose" to "#FFE4E1",
        "Lavender Blush" to "#FFF0F5",
        "Linen" to "#FAF0E6",
        "Old Lace" to "#FDF5E6",
        "Papaya Whip" to "#FFEFD5",
        "Sea Shell" to "#FFF5EE",
        "Mint Cream" to "#F5FFFA",
        "Floral White" to "#FFFAF0",
        "Ivory" to "#FFFFF0",
        "Honeydew" to "#F0FFF0"
    )
}
