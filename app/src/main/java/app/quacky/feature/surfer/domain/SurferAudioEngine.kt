package app.quacky.feature.surfer.domain

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * High-performance, offline audio engine for Quacky Surfer.
 * Synthesizes arcade retro-sound effects and an upbeat looping background music track:
 * - Jump (spring / pitch whoosh)
 * - Slide / Roll (low friction whoosh)
 * - Swipe Left & Right (snappy lateral dodge)
 * - Crash (heavy metal Crunch & sub-bass impact)
 * - Breadcrumb / Coin (golden dual chime)
 * - Countdown tick & Go buzzer
 * - Upbeat endless runner rhythmic synth background music
 *
 * Uses SoundPool for zero-latency gameplay SFX and MediaPlayer for seamless looping BGM.
 */
class SurferAudioEngine(private val context: Context) {

    private val audioDir = File(context.cacheDir, "surfer_audio").apply { mkdirs() }

    private val soundPool: SoundPool
    private var soundJump = 0
    private var soundSlide = 0
    private var soundSwipeLeft = 0
    private var soundSwipeRight = 0
    private var soundCrash = 0
    private var soundCoin = 0
    private var soundCountdown = 0
    private var soundCountdownGo = 0

    private var bgmPlayer: MediaPlayer? = null
    var isSoundEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) {
                bgmPlayer?.pause()
            } else if (isBgmSupposedToPlay) {
                bgmPlayer?.start()
            }
        }

    private var isBgmSupposedToPlay = false

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attrs)
            .build()

        ensureSoundFilesExist()
        loadSounds()
        initBgm()
    }

    private fun ensureSoundFilesExist() {
        generateWavIfMissing("jump.wav", generateJumpSamples())
        generateWavIfMissing("slide.wav", generateSlideSamples())
        generateWavIfMissing("swipe_left.wav", generateSwipeSamples(isLeft = true))
        generateWavIfMissing("swipe_right.wav", generateSwipeSamples(isLeft = false))
        generateWavIfMissing("crash.wav", generateCrashSamples())
        generateWavIfMissing("coin.wav", generateCoinSamples())
        generateWavIfMissing("countdown.wav", generateCountdownSamples())
        generateWavIfMissing("countdown_go.wav", generateCountdownGoSamples())
        generateWavIfMissing("bgm_runner.wav", generateBgmSamples())
    }

    private fun loadSounds() {
        soundJump = soundPool.load(File(audioDir, "jump.wav").absolutePath, 1)
        soundSlide = soundPool.load(File(audioDir, "slide.wav").absolutePath, 1)
        soundSwipeLeft = soundPool.load(File(audioDir, "swipe_left.wav").absolutePath, 1)
        soundSwipeRight = soundPool.load(File(audioDir, "swipe_right.wav").absolutePath, 1)
        soundCrash = soundPool.load(File(audioDir, "crash.wav").absolutePath, 1)
        soundCoin = soundPool.load(File(audioDir, "coin.wav").absolutePath, 1)
        soundCountdown = soundPool.load(File(audioDir, "countdown.wav").absolutePath, 1)
        soundCountdownGo = soundPool.load(File(audioDir, "countdown_go.wav").absolutePath, 1)
    }

    private fun initBgm() {
        try {
            val bgmFile = File(audioDir, "bgm_runner.wav")
            bgmPlayer = MediaPlayer().apply {
                setDataSource(bgmFile.absolutePath)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                setVolume(0.50f, 0.50f)
                prepare()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playJump() {
        if (!isSoundEnabled) return
        soundPool.play(soundJump, 0.85f, 0.85f, 1, 0, 1.0f)
    }

    fun playSlide() {
        if (!isSoundEnabled) return
        soundPool.play(soundSlide, 0.80f, 0.80f, 1, 0, 1.0f)
    }

    fun playSwipeLeft() {
        if (!isSoundEnabled) return
        // Slightly panned left for spatial perception
        soundPool.play(soundSwipeLeft, 0.90f, 0.60f, 1, 0, 1.0f)
    }

    fun playSwipeRight() {
        if (!isSoundEnabled) return
        // Slightly panned right for spatial perception
        soundPool.play(soundSwipeRight, 0.60f, 0.90f, 1, 0, 1.0f)
    }

    fun playCrash() {
        if (!isSoundEnabled) return
        soundPool.play(soundCrash, 1.0f, 1.0f, 2, 0, 1.0f)
    }

    fun playCoin() {
        if (!isSoundEnabled) return
        soundPool.play(soundCoin, 0.70f, 0.70f, 1, 0, 1.0f)
    }

    fun playCountdownTick() {
        if (!isSoundEnabled) return
        soundPool.play(soundCountdown, 0.75f, 0.75f, 1, 0, 1.0f)
    }

    fun playCountdownGo() {
        if (!isSoundEnabled) return
        soundPool.play(soundCountdownGo, 0.90f, 0.90f, 1, 0, 1.0f)
    }

    fun startBgm() {
        isBgmSupposedToPlay = true
        if (isSoundEnabled) {
            try {
                bgmPlayer?.let { player ->
                    if (!player.isPlaying) {
                        player.seekTo(0)
                        player.start()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun pauseBgm() {
        try {
            bgmPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resumeBgm() {
        if (isBgmSupposedToPlay && isSoundEnabled) {
            try {
                bgmPlayer?.let { player ->
                    if (!player.isPlaying) {
                        player.start()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopBgm() {
        isBgmSupposedToPlay = false
        try {
            bgmPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    player.seekTo(0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            stopBgm()
            bgmPlayer?.release()
            bgmPlayer = null
            soundPool.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // -------------------------------------------------------------------------
    // Procedural Audio Synthesizers (16-bit PCM, 22050 Hz)
    // -------------------------------------------------------------------------
    private fun generateWavIfMissing(filename: String, samples: ShortArray) {
        val file = File(audioDir, filename)
        if (file.exists() && file.length() > 44) return

        val sampleRate = 22050
        val dataSize = samples.size * 2
        val totalSize = 36 + dataSize

        val header = ByteBuffer.allocate(44).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            put('R'.code.toByte()); put('I'.code.toByte()); put('F'.code.toByte()); put('F'.code.toByte())
            putInt(totalSize)
            put('W'.code.toByte()); put('A'.code.toByte()); put('V'.code.toByte()); put('E'.code.toByte())
            put('f'.code.toByte()); put('m'.code.toByte()); put('t'.code.toByte()); put(' '.code.toByte())
            putInt(16) // Subchunk1Size (16 for PCM)
            putShort(1) // AudioFormat (1 = PCM)
            putShort(1) // NumChannels (1 = Mono)
            putInt(sampleRate)
            putInt(sampleRate * 2) // ByteRate
            putShort(2) // BlockAlign
            putShort(16) // BitsPerSample
            put('d'.code.toByte()); put('a'.code.toByte()); put('t'.code.toByte()); put('a'.code.toByte())
            putInt(dataSize)
        }.array()

        val pcmBytes = ByteArray(dataSize)
        val pcmBuffer = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
        for (s in samples) {
            pcmBuffer.putShort(s)
        }

        FileOutputStream(file).use { out ->
            out.write(header)
            out.write(pcmBytes)
        }
    }

    /**
     * Jump: energetic upward pitch whoosh (280 Hz -> 680 Hz)
     */
    private fun generateJumpSamples(): ShortArray {
        val sampleRate = 22050
        val duration = 0.16f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)

        var phase = 0.0
        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            val freq = 280.0 + (680.0 - 280.0) * (t * t)
            phase += 2.0 * PI * freq / sampleRate
            val amp = (1.0 - t) * 0.75
            val s = (sin(phase) * amp * 32767.0).toInt().coerceIn(-32768, 32767)
            samples[i] = s.toShort()
        }
        return samples
    }

    /**
     * Slide: low friction swoosh (260 Hz -> 90 Hz)
     */
    private fun generateSlideSamples(): ShortArray {
        val sampleRate = 22050
        val duration = 0.20f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)

        var phase = 0.0
        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            val freq = 260.0 + (90.0 - 260.0) * t
            phase += 2.0 * PI * freq / sampleRate
            val noise = ((Math.random() * 2.0 - 1.0) * 0.25)
            val amp = (1.0 - t * 0.8) * 0.70
            val s = ((sin(phase) + noise) * amp * 32767.0).toInt().coerceIn(-32768, 32767)
            samples[i] = s.toShort()
        }
        return samples
    }

    /**
     * Swipe dodge whoosh (420 Hz -> 580 Hz for left, 580 Hz -> 420 Hz for right)
     */
    private fun generateSwipeSamples(isLeft: Boolean): ShortArray {
        val sampleRate = 22050
        val duration = 0.10f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)

        var phase = 0.0
        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            val freq = if (isLeft) 420.0 + 160.0 * t else 580.0 - 160.0 * t
            phase += 2.0 * PI * freq / sampleRate
            val amp = sin(t * PI) * 0.65
            val s = (sin(phase) * amp * 32767.0).toInt().coerceIn(-32768, 32767)
            samples[i] = s.toShort()
        }
        return samples
    }

    /**
     * Crash: heavy metallic crunch & sub-bass rumble
     */
    private fun generateCrashSamples(): ShortArray {
        val sampleRate = 22050
        val duration = 0.50f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)

        var phaseLow = 0.0
        var phaseMid = 0.0
        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            val freqLow = 65.0 - 30.0 * t // Sub-bass drop
            val freqMid = 160.0 - 80.0 * t
            phaseLow += 2.0 * PI * freqLow / sampleRate
            phaseMid += 2.0 * PI * freqMid / sampleRate

            val noise = (Math.random() * 2.0 - 1.0) * exp(-t * 8.0) * 0.60
            val body = (sin(phaseLow) * 0.50 + (if (sin(phaseMid) > 0) 0.35 else -0.35)) * exp(-t * 5.0)
            val s = ((body + noise) * 32767.0).toInt().coerceIn(-32768, 32767)
            samples[i] = s.toShort()
        }
        return samples
    }

    /**
     * Breadcrumb Coin: Dual sparkle ding (1320 Hz + 1760 Hz)
     */
    private fun generateCoinSamples(): ShortArray {
        val sampleRate = 22050
        val duration = 0.14f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)

        val split = (numSamples * 0.40).toInt()
        var phase1 = 0.0
        var phase2 = 0.0

        for (i in 0 until numSamples) {
            val s = if (i < split) {
                val t = i.toFloat() / split
                phase1 += 2.0 * PI * 1320.0 / sampleRate
                (sin(phase1) * (1.0 - t * 0.7) * 22000.0).toInt()
            } else {
                val t = (i - split).toFloat() / (numSamples - split)
                phase2 += 2.0 * PI * 1760.0 / sampleRate
                (sin(phase2) * (1.0 - t * 0.8) * 24000.0).toInt()
            }
            samples[i] = s.coerceIn(-32768, 32767).toShort()
        }
        return samples
    }

    /**
     * Countdown Beep (880 Hz metronome tick)
     */
    private fun generateCountdownSamples(): ShortArray {
        val sampleRate = 22050
        val duration = 0.08f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)

        var phase = 0.0
        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            phase += 2.0 * PI * 880.0 / sampleRate
            val amp = (1.0 - t) * 0.60
            val s = (sin(phase) * amp * 32767.0).toInt().coerceIn(-32768, 32767)
            samples[i] = s.toShort()
        }
        return samples
    }

    /**
     * Countdown GO chime (1320 Hz)
     */
    private fun generateCountdownGoSamples(): ShortArray {
        val sampleRate = 22050
        val duration = 0.18f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)

        var phase = 0.0
        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            phase += 2.0 * PI * 1320.0 / sampleRate
            val amp = (1.0 - t * 0.7) * 0.75
            val s = (sin(phase) * amp * 32767.0).toInt().coerceIn(-32768, 32767)
            samples[i] = s.toShort()
        }
        return samples
    }

    /**
     * Upbeat rhythmic endless runner BGM loop (125 BPM, 8-bar loop, ~3.84s)
     * Funky bassline + four-on-the-floor kick & hi-hat percussion + arpeggio synth
     */
    private fun generateBgmSamples(): ShortArray {
        val sampleRate = 22050
        val bpm = 125.0
        val beatDuration = 60.0 / bpm // 0.48s per beat
        val totalBeats = 8
        val totalDuration = beatDuration * totalBeats // 3.84s
        val numSamples = (sampleRate * totalDuration).toInt()
        val samples = ShortArray(numSamples)

        // Bass frequencies in D Minor: D2 (73.4), F2 (87.3), G2 (98.0), A2 (110.0), C3 (130.8)
        val bassNotes = doubleArrayOf(73.4, 73.4, 87.3, 98.0, 110.0, 110.0, 98.0, 73.4)
        // Melody arpeggio: D4 (293.7), F4 (349.2), A4 (440.0), C5 (523.3)
        val arpNotes = doubleArrayOf(293.7, 349.2, 440.0, 523.3, 440.0, 349.2, 293.7, 349.2)

        var bassPhase = 0.0
        var arpPhase = 0.0

        for (i in 0 until numSamples) {
            val tTotal = i.toDouble() / sampleRate
            val beatIndex = ((tTotal / beatDuration).toInt()) % totalBeats
            val tInBeat = (tTotal % beatDuration) / beatDuration

            // 1. Four-on-the-floor Kick drum
            val kickT = tInBeat
            val kickFreq = 140.0 * exp(-kickT * 18.0) + 45.0
            val kickAmp = exp(-kickT * 12.0) * 0.45
            val kick = sin(2.0 * PI * kickFreq * kickT) * kickAmp

            // 2. Hi-Hat on every off-beat (tInBeat ~ 0.5)
            val hatT = ((tInBeat + 0.5) % 1.0)
            val hatAmp = exp(-hatT * 25.0) * 0.15
            val hat = (Math.random() * 2.0 - 1.0) * hatAmp

            // 3. Driving Bass
            val bassFreq = bassNotes[beatIndex]
            bassPhase += 2.0 * PI * bassFreq / sampleRate
            val bassAmp = (1.0 - tInBeat * 0.4) * 0.35
            val bass = sin(bassPhase) * bassAmp

            // 4. Synth Arp (changes every half beat)
            val arpIndex = ((tTotal / (beatDuration / 2.0)).toInt()) % arpNotes.size
            val arpFreq = arpNotes[arpIndex]
            arpPhase += 2.0 * PI * arpFreq / sampleRate
            val arpInStep = (tTotal % (beatDuration / 2.0)) / (beatDuration / 2.0)
            val arpAmp = exp(-arpInStep * 4.0) * 0.18
            val arp = sin(arpPhase) * arpAmp

            val mixed = (kick + hat + bass + arp) * 28000.0
            samples[i] = mixed.toInt().coerceIn(-32768, 32767).toShort()
        }
        return samples
    }
}
