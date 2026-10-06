package app.quacky.feature.random.randomnumber

import java.security.SecureRandom
import java.util.SplittableRandom

object RngEngine {

    /**
     * Hashes string seed into a 64-bit Long for SplittableRandom seeding.
     */
    fun hashSeed(seed: String): Long {
        var h = 1125899906842597L
        for (i in seed.indices) {
            h = 31 * h + seed[i].code.toLong()
        }
        return h
    }

    /**
     * Generate list of random integers.
     */
    fun generateIntegers(
        min: Long,
        max: Long,
        count: Int,
        allowDuplicates: Boolean,
        seed: String? = null,
        sequenceIndex: Long = 0
    ): List<Long> {
        val rangeSize = (max - min) + 1
        val finalCount = if (!allowDuplicates) minOf(count.toLong(), rangeSize).toInt() else count

        if (seed != null && seed.isNotBlank()) {
            val baseSeed = hashSeed(seed) xor sequenceIndex
            val rng = SplittableRandom(baseSeed)
            return if (allowDuplicates) {
                List(finalCount) { rng.nextLong(min, max + 1) }
            } else {
                val set = mutableSetOf<Long>()
                while (set.size < finalCount) {
                    set.add(rng.nextLong(min, max + 1))
                }
                set.toList()
            }
        } else {
            val secureRandom = SecureRandom()
            return if (allowDuplicates) {
                List(finalCount) {
                    val r = (secureRandom.nextDouble() * rangeSize).toLong() + min
                    r.coerceIn(min, max)
                }
            } else {
                val set = mutableSetOf<Long>()
                while (set.size < finalCount) {
                    val r = (secureRandom.nextDouble() * rangeSize).toLong() + min
                    set.add(r.coerceIn(min, max))
                }
                set.toList()
            }
        }
    }
}
