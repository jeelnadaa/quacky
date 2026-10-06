package app.quacky.core.util

import android.content.Context
import app.quacky.core.registry.ToolDefinition
import app.quacky.core.registry.ToolRegistry
import kotlin.math.min

object SearchMatcher {

    /**
     * Matches tools against a search query using prefix matching and 1-typo tolerance for words > 4 chars.
     * Returns matching tools ordered by relevance score (higher score first).
     */
    fun searchTools(context: Context, query: String): List<ToolDefinition> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return emptyList()

        return ToolRegistry.allTools
            .mapNotNull { tool ->
                val name = context.getString(tool.nameRes).lowercase()
                val desc = context.getString(tool.descriptionRes).lowercase()
                val score = computeMatchScore(trimmed, name, desc, tool.keywords)
                if (score > 0) Pair(tool, score) else null
            }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    private fun computeMatchScore(
        query: String,
        name: String,
        desc: String,
        keywords: List<String>
    ): Int {
        // Exact name match
        if (name == query) return 100
        // Prefix name match
        if (name.startsWith(query)) return 80
        // Name contains query
        if (name.contains(query)) return 60

        // Keyword matches
        for (kw in keywords) {
            val kwLower = kw.lowercase()
            if (kwLower == query) return 50
            if (kwLower.startsWith(query)) return 40
            if (kwLower.contains(query)) return 30
        }

        // Description matches
        if (desc.contains(query)) return 20

        // Fuzzy match: if query length > 4, allow Levenshtein distance of 1
        if (query.length > 4) {
            val words = (name.split(" ") + keywords)
            for (w in words) {
                if (levenshteinDistance(query, w) <= 1) {
                    return 15
                }
            }
        }

        return 0
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[m][n]
    }
}
