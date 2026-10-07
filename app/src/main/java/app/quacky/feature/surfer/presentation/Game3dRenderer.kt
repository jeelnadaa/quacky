package app.quacky.feature.surfer.presentation

import android.content.Context
import android.opengl.Matrix
import android.view.Surface
import com.google.android.filament.Camera
import com.google.android.filament.ColorGrading
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.Filament
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SwapChain
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.Gltfio
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import app.quacky.feature.surfer.domain.PlayerController
import app.quacky.feature.surfer.domain.RowPlanner
import app.quacky.feature.surfer.domain.SurferTuning
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Native 3D Filament rendering and simulation engine for Quacky Surfer.
 * Loads and renders real glTF/GLB models:
 * - quacky_duck.glb (with run, idle, jump, slide, lean_left, lean_right, crash animations)
 * - train.glb, barrier.glb, duct.glb (obstacle pools)
 * - breadcrumb.glb (golden spinning coins with bloom)
 * - blob_shadow.glb (ground contact shadow)
 * - env_chunk_a/b/c.glb (scrolling railway environment ring)
 */
class Game3dRenderer(
    private val context: Context,
    private val onBreadcrumbCollected: (count: Int) -> Unit,
    private val onCrash: (reason: String, distance: Float, breadcrumbs: Int, score: Int) -> Unit,
    var onTick: ((distance: Float, breadcrumbs: Int, score: Int) -> Unit)? = null
) {
    // Filament Core
    private var engine: Engine? = null
    val isEngineInitialized: Boolean
        get() = engine != null
    private var renderer: Renderer? = null
    private var scene: Scene? = null
    private var view: View? = null
    private var cameraEntity: Int = 0
    private var camera: Camera? = null
    private var swapChain: SwapChain? = null
    private var assetLoader: AssetLoader? = null
    private var resourceLoader: ResourceLoader? = null

    // Lights
    private var sunlightEntity: Int = 0
    private var ambientLightEntity: Int = 0

    // Assets & Instances
    private var duckAsset: FilamentAsset? = null
    private var blobShadowAsset: FilamentAsset? = null

    // Duck Animation Clip Indices
    private var clipRest = 0
    private var clipRun = 0
    private var clipIdle = 0
    private var clipJump = 0
    private var clipSlide = 0
    private var clipLeanLeft = 0
    private var clipLeanRight = 0
    private var clipCrash = 0

    // Animation Timers
    private var animTime = 0.0f
    private var jumpAnimTime = 0.0f
    private var slideAnimTime = 0.0f
    private var breadcrumbSpinTime = 0.0f

    // Chunks Ring (8 chunks along -Z)
    data class ChunkItem(
        val asset: FilamentAsset,
        var z: Float
    )
    private val chunkRing = mutableListOf<ChunkItem>()
    private var lastChunkType = -1

    // Object Pools
    class ActiveObstacle(
        val asset: FilamentAsset,
        val type: RowPlanner.CellType,
        val lane: Int,
        var distanceAhead: Float
    )

    class ActiveBreadcrumb(
        val asset: FilamentAsset,
        val lane: Int,
        var distanceAhead: Float,
        val isHigh: Boolean,
        var popTimer: Float = 0.0f
    )

    private val trainPool = ArrayDeque<FilamentAsset>()
    private val barrierPool = ArrayDeque<FilamentAsset>()
    private val ductPool = ArrayDeque<FilamentAsset>()
    private val breadcrumbPool = ArrayDeque<FilamentAsset>()

    private val activeObstacles = mutableListOf<ActiveObstacle>()
    private val activeBreadcrumbs = mutableListOf<ActiveBreadcrumb>()

    // Simulation State
    val player = PlayerController()
    private val rowPlanner = RowPlanner(Random(System.currentTimeMillis()))
    private var lastEmittedRow: RowPlanner.Row? = null

    var isReadyMode = true
        private set
    var isRunning = false
        private set
    var isCrashed = false
        private set

    var distanceMeters = 0.0f
        private set
    var breadcrumbsCount = 0
        private set
    var currentSpeed = SurferTuning.SPEED_MIN
        private set
    private var elapsedRunTime = 0.0f
    private var nextSpawnDistance = 0.0f

    // Camera follow
    private var camX = 0.0f
    private var camY = SurferTuning.READY_CAM_Y
    private var camZ = SurferTuning.READY_CAM_Z
    private var viewportWidth = 1080
    private var viewportHeight = 1920

    // Camera Shake
    private var shakeTimer = 0.0f

    // Temp matrices for transform updates (zero per-frame allocations)
    private val tempMatrix = FloatArray(16)
    private val transformMatrix = FloatArray(16)

    init {
        initFilament()
    }

    private fun initFilament() {
        try {
            Filament.init()
            Gltfio.init()

            val eng = Engine.create()
            engine = eng
            val rnd = eng.createRenderer()
            renderer = rnd
            val scn = eng.createScene()
            scene = scn

            // Setup View with Bloom, Fog, ACES Color Grading
            val vw = eng.createView().apply {
                this.scene = scn
                isPostProcessingEnabled = true
                antiAliasing = View.AntiAliasing.FXAA

                bloomOptions = View.BloomOptions().apply {
                    enabled = true
                    strength = 0.35f
                }

                fogOptions = View.FogOptions().apply {
                    enabled = true
                    distance = 25.0f
                    cutOffDistance = 110.0f
                    color = floatArrayOf(0.04f, 0.04f, 0.04f)
                }

                colorGrading = ColorGrading.Builder()
                    .toneMapping(ColorGrading.ToneMapping.ACES)
                    .build(eng)
            }
            view = vw

            // Black Clear Color
            rnd.clearOptions = Renderer.ClearOptions().apply {
                clear = true
                clearColor = floatArrayOf(0.04f, 0.04f, 0.04f, 1.0f)
            }

            // Camera
            val em = EntityManager.get()
            cameraEntity = em.create()
            val cam = eng.createCamera(cameraEntity)
            camera = cam
            vw.camera = cam

            // Sun Directional Light
            sunlightEntity = em.create()
            LightManager.Builder(LightManager.Type.SUN)
                .color(1.0f, 0.97f, 0.92f)
                .intensity(60_000.0f)
                .direction(-0.35f, -0.90f, -0.25f)
                .build(eng, sunlightEntity)
            scn.addEntity(sunlightEntity)

            // Ambient Indirect Light (Spherical Harmonics)
            val shBands = FloatArray(9 * 3) { 0.32f }
            val indirectLight = IndirectLight.Builder()
                .irradiance(3, shBands)
                .intensity(25_000.0f)
                .build(eng)
            scn.indirectLight = indirectLight

            // Asset Loaders
            val ubershader = UbershaderProvider(eng)
            val al = AssetLoader(eng, ubershader, em)
            assetLoader = al
            val rl = ResourceLoader(eng)
            resourceLoader = rl

            // Load 3D Models
            load3dAssets(eng, al, rl)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadGlbBuffer(filename: String): ByteBuffer {
        val bytes = context.assets.open("surfer/$filename").readBytes()
        return ByteBuffer.allocateDirect(bytes.size).apply {
            put(bytes)
            flip()
        }
    }

    private fun load3dAssets(engine: Engine, assetLoader: AssetLoader, resourceLoader: ResourceLoader) {
        val scn = scene ?: return

        // 1. Quacky Duck
        val duckBuf = loadGlbBuffer("quacky_duck.glb")
        val dAsset = assetLoader.createAsset(duckBuf)
        if (dAsset != null) {
            resourceLoader.loadResources(dAsset)
            duckAsset = dAsset
            scn.addEntities(dAsset.entities)

            // Resolve animation clips
            val animator = dAsset.instance.animator
            for (i in 0 until animator.animationCount) {
                when (animator.getAnimationName(i)) {
                    "rest" -> clipRest = i
                    "run" -> clipRun = i
                    "idle" -> clipIdle = i
                    "jump" -> clipJump = i
                    "slide" -> clipSlide = i
                    "lean_left" -> clipLeanLeft = i
                    "lean_right" -> clipLeanRight = i
                    "crash" -> clipCrash = i
                }
            }
        }

        // 2. Blob Shadow
        val shadowBuf = loadGlbBuffer("blob_shadow.glb")
        val sAsset = assetLoader.createAsset(shadowBuf)
        if (sAsset != null) {
            resourceLoader.loadResources(sAsset)
            blobShadowAsset = sAsset
            scn.addEntities(sAsset.entities)
        }

        // 3. Environment Chunks Ring (8 chunks cycling a, b, c)
        val chunkBufA = loadGlbBuffer("env_chunk_a.glb")
        val chunkBufB = loadGlbBuffer("env_chunk_b.glb")
        val chunkBufC = loadGlbBuffer("env_chunk_c.glb")

        var chunkZ = 0.0f
        for (i in 0 until SurferTuning.CHUNK_COUNT) {
            val type = pickNextChunkType()
            val buf = when (type) {
                0 -> chunkBufA.duplicate()
                1 -> chunkBufB.duplicate()
                else -> chunkBufC.duplicate()
            }
            val cAsset = assetLoader.createAsset(buf)
            if (cAsset != null) {
                resourceLoader.loadResources(cAsset)
                scn.addEntities(cAsset.entities)
                chunkRing.add(ChunkItem(cAsset, chunkZ))
                setAssetPosition(engine, cAsset, 0f, 0f, chunkZ)
            }
            chunkZ -= SurferTuning.CHUNK_LENGTH
        }

        // 4. Preallocate Obstacle Pools
        val trainBuf = loadGlbBuffer("train.glb")
        for (i in 0 until 10) {
            val a = assetLoader.createAsset(trainBuf.duplicate()) ?: continue
            resourceLoader.loadResources(a)
            trainPool.add(a)
        }

        val barrierBuf = loadGlbBuffer("barrier.glb")
        for (i in 0 until 14) {
            val a = assetLoader.createAsset(barrierBuf.duplicate()) ?: continue
            resourceLoader.loadResources(a)
            barrierPool.add(a)
        }

        val ductBuf = loadGlbBuffer("duct.glb")
        for (i in 0 until 14) {
            val a = assetLoader.createAsset(ductBuf.duplicate()) ?: continue
            resourceLoader.loadResources(a)
            ductPool.add(a)
        }

        // 5. Preallocate Breadcrumb Coin Pool
        val coinBuf = loadGlbBuffer("breadcrumb.glb")
        for (i in 0 until 140) {
            val a = assetLoader.createAsset(coinBuf.duplicate()) ?: continue
            resourceLoader.loadResources(a)
            breadcrumbPool.add(a)
        }
    }

    private fun pickNextChunkType(): Int {
        val options = listOf(0, 1, 2).filter { it != lastChunkType }
        val next = options[Random.nextInt(options.size)]
        lastChunkType = next
        return next
    }

    private fun setAssetPosition(
        engine: Engine,
        asset: FilamentAsset,
        x: Float,
        y: Float,
        z: Float,
        scaleX: Float = 1.0f,
        scaleY: Float = 1.0f,
        scaleZ: Float = 1.0f
    ) {
        val tm = engine.transformManager
        val ti = tm.getInstance(asset.root)
        Matrix.setIdentityM(transformMatrix, 0)
        Matrix.translateM(transformMatrix, 0, x, y, z)
        if (scaleX != 1.0f || scaleY != 1.0f || scaleZ != 1.0f) {
            Matrix.scaleM(transformMatrix, 0, scaleX, scaleY, scaleZ)
        }
        tm.setTransform(ti, transformMatrix)
    }

    fun setSurface(surface: Surface) {
        val eng = engine ?: return
        swapChain = eng.createSwapChain(surface)
    }

    fun destroySurface() {
        swapChain?.let { sc ->
            engine?.destroySwapChain(sc)
            swapChain = null
        }
    }

    fun setViewport(width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        view?.viewport = Viewport(0, 0, width, height)
        updateCameraProjection()
    }

    private fun updateCameraProjection() {
        val cam = camera ?: return
        val aspect = viewportWidth.toDouble() / viewportHeight.toDouble()
        val speedFraction = ((currentSpeed - SurferTuning.SPEED_MIN) / (SurferTuning.SPEED_MAX - SurferTuning.SPEED_MIN))
            .coerceIn(0.0f, 1.0f)
        val fov = (SurferTuning.FOV_MIN + speedFraction * (SurferTuning.FOV_MAX - SurferTuning.FOV_MIN)).toDouble()
        cam.setProjection(fov, aspect, 0.1, 140.0, Camera.Fov.VERTICAL)
    }

    // -------------------------------------------------------------------------
    // Game Lifecycle Controls
    // -------------------------------------------------------------------------
    fun startReadyState() {
        clearActiveObjects()
        player.reset()
        isReadyMode = true
        isRunning = false
        isCrashed = false
        distanceMeters = 0.0f
        breadcrumbsCount = 0
        currentSpeed = SurferTuning.SPEED_MIN
        elapsedRunTime = 0.0f
        nextSpawnDistance = 15.0f
        lastEmittedRow = null
        camX = 0.0f
        camY = SurferTuning.READY_CAM_Y
        camZ = SurferTuning.READY_CAM_Z
        shakeTimer = 0.0f
    }

    fun startRunning() {
        isReadyMode = false
        isRunning = true
        isCrashed = false
        player.startRunning()
    }

    fun pause() {
        isRunning = false
    }

    fun resume() {
        if (!isCrashed && !isReadyMode) {
            isRunning = true
        }
    }

    fun requestMoveLeft() {
        if (!isCrashed && !isReadyMode) {
            player.requestMoveLeft()
        }
    }

    fun requestMoveRight() {
        if (!isCrashed && !isReadyMode) {
            player.requestMoveRight()
        }
    }

    fun requestJump() {
        if (!isCrashed && !isReadyMode) {
            player.requestJump()
        }
    }

    fun requestSlide() {
        if (!isCrashed && !isReadyMode) {
            player.requestSlide()
        }
    }

    fun getCurrentScore(): Int = SurferTuning.calculateScore(distanceMeters, breadcrumbsCount)
    fun getDistance(): Float = distanceMeters
    fun getBreadcrumbs(): Int = breadcrumbsCount
    fun isGameRunning(): Boolean = isRunning
    fun isGameCrashed(): Boolean = isCrashed
    fun isGameReady(): Boolean = isReadyMode

    private fun clearActiveObjects() {
        val scn = scene ?: return
        for (obs in activeObstacles) {
            scn.removeEntities(obs.asset.entities)
            when (obs.type) {
                RowPlanner.CellType.TRAIN -> trainPool.add(obs.asset)
                RowPlanner.CellType.BARRIER -> barrierPool.add(obs.asset)
                RowPlanner.CellType.DUCT -> ductPool.add(obs.asset)
                else -> {}
            }
        }
        activeObstacles.clear()

        for (bc in activeBreadcrumbs) {
            scn.removeEntities(bc.asset.entities)
            breadcrumbPool.add(bc.asset)
        }
        activeBreadcrumbs.clear()
    }

    // -------------------------------------------------------------------------
    // Frame Render & Physics Loop (Choreographer callback)
    // -------------------------------------------------------------------------
    fun renderFrame(dt: Float) {
        val eng = engine ?: return
        val scn = scene ?: return
        val rnd = renderer ?: return
        val vw = view ?: return
        val cam = camera ?: return
        val sc = swapChain ?: return

        animTime += dt
        breadcrumbSpinTime += dt * 1.5f

        if (isRunning) {
            elapsedRunTime += dt
            currentSpeed = SurferTuning.calculateSpeed(elapsedRunTime)
            val moveDist = currentSpeed * dt
            distanceMeters += moveDist

            // Fixed physics step
            updatePhysics(dt, moveDist)
            onTick?.invoke(distanceMeters, breadcrumbsCount, SurferTuning.calculateScore(distanceMeters, breadcrumbsCount))
        }

        // Camera Update
        updateCamera(dt, cam)

        // Duck Animation Update
        updateDuckAnimation(dt)

        // Update Scene Transforms
        val tm = eng.transformManager
        tm.openLocalTransformTransaction()

        // 1. Duck Transform
        duckAsset?.let { dAsset ->
            setAssetPosition(eng, dAsset, player.x, player.y, 0f)
        }

        // 2. Blob Shadow Transform (grounded, shrinks during jump)
        blobShadowAsset?.let { sAsset ->
            val jumpScale = (1.0f - (player.y / 1.61f) * 0.4f).coerceIn(0.6f, 1.0f)
            setAssetPosition(eng, sAsset, player.x, 0.01f, 0f, jumpScale, 1.0f, jumpScale)
        }

        // 3. Environment Chunks Transform
        for (chunk in chunkRing) {
            setAssetPosition(eng, chunk.asset, 0f, 0f, chunk.z)
        }

        // 4. Active Obstacles Transform
        for (obs in activeObstacles) {
            val laneX = SurferTuning.LANES[obs.lane]
            val zPos = -obs.distanceAhead
            setAssetPosition(eng, obs.asset, laneX, 0f, zPos)
        }

        // 5. Active Breadcrumbs Transform (with spin and pop scale)
        for (bc in activeBreadcrumbs) {
            val laneX = SurferTuning.LANES[bc.lane]
            val zPos = -bc.distanceAhead
            val yPos = if (bc.isHigh) SurferTuning.Colliders.BREADCRUMB_HIGH_Y else SurferTuning.Colliders.BREADCRUMB_GROUND_Y
            val scale = if (bc.popTimer > 0f) 1.5f else 1.0f

            setAssetPosition(eng, bc.asset, laneX, yPos, zPos, scale, scale, scale)

            // Animate Coin Spin
            val animator = bc.asset.instance.animator
            if (animator.animationCount > 0) {
                animator.applyAnimation(0, breadcrumbSpinTime % 1.2f)
                animator.updateBoneMatrices()
            }
        }

        tm.commitLocalTransformTransaction()

        // Filament Draw Submission
        if (rnd.beginFrame(sc, 0L)) {
            rnd.render(vw)
            rnd.endFrame()
        }
    }

    private fun updatePhysics(dt: Float, moveDist: Float) {
        val eng = engine ?: return
        val scn = scene ?: return

        // Player physics
        player.update(dt)

        // Environment Chunks Scrolling
        for (chunk in chunkRing) {
            chunk.z += moveDist
        }
        // Recycle chunks when near edge exceeds 10m behind camera
        val nearestChunk = chunkRing.minByOrNull { it.z }
        for (chunk in chunkRing) {
            if (chunk.z > 10.0f) {
                val furthestZ = chunkRing.minOf { it.z }
                chunk.z = furthestZ - SurferTuning.CHUNK_LENGTH
            }
        }

        // Spawn New Rows ahead up to 110m
        while (nextSpawnDistance < distanceMeters + SurferTuning.SPAWN_HORIZON) {
            val newRows = rowPlanner.planNextRows(nextSpawnDistance, currentSpeed, lastEmittedRow)
            for (row in newRows) {
                spawnRowEntities(row)
                lastEmittedRow = row
            }
            val gap = SurferTuning.calculateRowGap(currentSpeed)
            nextSpawnDistance = (lastEmittedRow?.distance ?: nextSpawnDistance) + gap
        }

        // Advance Obstacles and Breadcrumbs
        for (obs in activeObstacles) {
            obs.distanceAhead -= moveDist
        }
        for (bc in activeBreadcrumbs) {
            bc.distanceAhead -= moveDist
            if (bc.popTimer > 0f) {
                bc.popTimer -= dt
            }
        }

        // Collision Detection: Breadcrumbs
        val pHalfW = SurferTuning.Colliders.PLAYER_HALF_WIDTH
        val pHeight = if (player.isSliding) SurferTuning.Colliders.PLAYER_SLIDING_HEIGHT else SurferTuning.Colliders.PLAYER_STANDING_HEIGHT
        val pYMin = player.y
        val pYMax = player.y + pHeight
        val pX = player.x

        val bcIter = activeBreadcrumbs.iterator()
        while (bcIter.hasNext()) {
            val bc = bcIter.next()
            val bcX = SurferTuning.LANES[bc.lane]
            val bcY = if (bc.isHigh) SurferTuning.Colliders.BREADCRUMB_HIGH_Y else SurferTuning.Colliders.BREADCRUMB_GROUND_Y
            val bcDist = bc.distanceAhead

            // Pickup sphere vs player box overlap
            if (abs(bcX - pX) < pHalfW + SurferTuning.Colliders.BREADCRUMB_RADIUS &&
                bcDist in -0.5f..0.5f &&
                pYMax >= bcY - SurferTuning.Colliders.BREADCRUMB_RADIUS &&
                pYMin <= bcY + SurferTuning.Colliders.BREADCRUMB_RADIUS
            ) {
                // Collected!
                breadcrumbsCount++
                onBreadcrumbCollected(breadcrumbsCount)

                scn.removeEntities(bc.asset.entities)
                breadcrumbPool.add(bc.asset)
                bcIter.remove()
                continue
            }

            // Despawn behind player > 8m
            if (bc.distanceAhead < -SurferTuning.DESPAWN_BEHIND_Z || bc.popTimer < 0f && bc.popTimer != 0f) {
                scn.removeEntities(bc.asset.entities)
                breadcrumbPool.add(bc.asset)
                bcIter.remove()
            }
        }

        // Collision Detection: Obstacles
        for (obs in activeObstacles) {
            val obsX = SurferTuning.LANES[obs.lane]
            val dist = obs.distanceAhead

            when (obs.type) {
                RowPlanner.CellType.TRAIN -> {
                    // Train extends from z 0.0 to -11.0 ahead of front
                    if (dist in -1.0f..SurferTuning.Colliders.TRAIN_LENGTH &&
                        abs(obsX - pX) < pHalfW + SurferTuning.Colliders.TRAIN_HALF_WIDTH &&
                        pYMin < SurferTuning.Colliders.TRAIN_HEIGHT
                    ) {
                        triggerCrash("Hit a train! Dodge to the side.")
                        return
                    }
                }
                RowPlanner.CellType.BARRIER -> {
                    // Low barrier (must be jumped)
                    if (dist in -SurferTuning.Colliders.BARRIER_HALF_DEPTH..SurferTuning.Colliders.BARRIER_HALF_DEPTH &&
                        abs(obsX - pX) < pHalfW + SurferTuning.Colliders.BARRIER_HALF_WIDTH &&
                        pYMin < SurferTuning.Colliders.BARRIER_HEIGHT
                    ) {
                        triggerCrash("Hit a barrier! Swipe up to jump.")
                        return
                    }
                }
                RowPlanner.CellType.DUCT -> {
                    // Overhead duct (must be slid under)
                    if (dist in -SurferTuning.Colliders.DUCT_BAR_HALF_DEPTH..SurferTuning.Colliders.DUCT_BAR_HALF_DEPTH &&
                        abs(obsX - pX) < pHalfW + SurferTuning.Colliders.DUCT_BAR_HALF_WIDTH
                    ) {
                        if (pYMax > SurferTuning.Colliders.DUCT_BAR_MIN_Y) {
                            triggerCrash("Hit an overhead duct! Swipe down to slide.")
                            return
                        }
                    }
                }
                else -> {}
            }
        }

        // Despawn passed obstacles > 8m behind
        val obsIter = activeObstacles.iterator()
        while (obsIter.hasNext()) {
            val obs = obsIter.next()
            val maxLen = if (obs.type == RowPlanner.CellType.TRAIN) SurferTuning.Colliders.TRAIN_LENGTH else 1.0f
            if (obs.distanceAhead < -(SurferTuning.DESPAWN_BEHIND_Z + maxLen)) {
                scn.removeEntities(obs.asset.entities)
                when (obs.type) {
                    RowPlanner.CellType.TRAIN -> trainPool.add(obs.asset)
                    RowPlanner.CellType.BARRIER -> barrierPool.add(obs.asset)
                    RowPlanner.CellType.DUCT -> ductPool.add(obs.asset)
                    else -> {}
                }
                obsIter.remove()
            }
        }
    }

    private fun spawnRowEntities(row: RowPlanner.Row) {
        val scn = scene ?: return
        val distAhead = row.distance - distanceMeters

        for (lane in 0 until 3) {
            when (row.cells[lane]) {
                RowPlanner.CellType.COIN -> {
                    val asset = breadcrumbPool.removeFirstOrNull() ?: continue
                    scn.addEntities(asset.entities)
                    activeBreadcrumbs.add(ActiveBreadcrumb(asset, lane, distAhead, isHigh = false))
                }
                RowPlanner.CellType.HIGH_COIN -> {
                    val asset = breadcrumbPool.removeFirstOrNull() ?: continue
                    scn.addEntities(asset.entities)
                    activeBreadcrumbs.add(ActiveBreadcrumb(asset, lane, distAhead, isHigh = true))
                }
                RowPlanner.CellType.BARRIER -> {
                    val asset = barrierPool.removeFirstOrNull() ?: continue
                    scn.addEntities(asset.entities)
                    activeObstacles.add(ActiveObstacle(asset, RowPlanner.CellType.BARRIER, lane, distAhead))

                    // Spawn coin arc over barrier (-1.2m, 0m, +1.2m)
                    for (offset in listOf(-1.2f, 0.0f, 1.2f)) {
                        val cAsset = breadcrumbPool.removeFirstOrNull() ?: continue
                        scn.addEntities(cAsset.entities)
                        activeBreadcrumbs.add(ActiveBreadcrumb(cAsset, lane, distAhead + offset, isHigh = true))
                    }
                }
                RowPlanner.CellType.DUCT -> {
                    val asset = ductPool.removeFirstOrNull() ?: continue
                    scn.addEntities(asset.entities)
                    activeObstacles.add(ActiveObstacle(asset, RowPlanner.CellType.DUCT, lane, distAhead))
                }
                RowPlanner.CellType.TRAIN -> {
                    val asset = trainPool.removeFirstOrNull() ?: continue
                    scn.addEntities(asset.entities)
                    activeObstacles.add(ActiveObstacle(asset, RowPlanner.CellType.TRAIN, lane, distAhead))
                }
                RowPlanner.CellType.EMPTY -> {}
            }
        }
    }

    private fun triggerCrash(reason: String) {
        isCrashed = true
        isRunning = false
        player.triggerCrash()
        shakeTimer = SurferTuning.CRASH_SHAKE_DURATION

        val score = SurferTuning.calculateScore(distanceMeters, breadcrumbsCount)
        onCrash(reason, distanceMeters, breadcrumbsCount, score)
    }

    private fun updateCamera(dt: Float, cam: Camera) {
        if (isReadyMode) {
            // Camera slightly lower & closer during countdown
            camX = 0.0f
            camY = SurferTuning.READY_CAM_Y
            camZ = SurferTuning.READY_CAM_Z
        } else {
            // Smoothly follow player X with spring; Y and Z at play position
            val targetX = player.x * 0.45f
            camX += (targetX - camX) * min(1.0f, dt * SurferTuning.CAM_SPRING_OMEGA)
            camY += (SurferTuning.CAM_Y - camY) * min(1.0f, dt * 4.0f)
            camZ += (SurferTuning.CAM_Z - camZ) * min(1.0f, dt * 4.0f)
        }

        // Camera Shake on crash
        var shakeX = 0.0f
        var shakeY = 0.0f
        if (shakeTimer > 0f) {
            shakeTimer -= dt
            val amp = SurferTuning.CRASH_SHAKE_AMPLITUDE * (shakeTimer / SurferTuning.CRASH_SHAKE_DURATION)
            shakeX = (Random.nextFloat() * 2f - 1f) * amp
            shakeY = (Random.nextFloat() * 2f - 1f) * amp
        }

        // Look-at: zero yaw, zero roll!
        cam.lookAt(
            (camX + shakeX).toDouble(),
            (camY + shakeY).toDouble(),
            camZ.toDouble(),
            (camX + shakeX).toDouble(),
            SurferTuning.CAM_LOOK_Y.toDouble(),
            SurferTuning.CAM_LOOK_Z.toDouble(),
            0.0,
            1.0,
            0.0
        )
    }

    private fun updateDuckAnimation(dt: Float) {
        val dAsset = duckAsset ?: return
        val animator = dAsset.instance.animator

        // 1. Reset all animated nodes to bind pose
        animator.applyAnimation(clipRest, 0.0f)

        // 2. Base Clip Application
        when (player.state) {
            PlayerController.State.IDLE_READY -> {
                animator.applyAnimation(clipIdle, (animTime * 1.0f) % 1.6f)
            }
            PlayerController.State.GROUNDED_RUN -> {
                val runSpeedScale = (currentSpeed / 10.0f).coerceIn(0.9f, 1.8f)
                animator.applyAnimation(clipRun, (animTime * runSpeedScale) % 0.5f)
            }
            PlayerController.State.JUMPING -> {
                jumpAnimTime = min(0.7f, jumpAnimTime + dt)
                animator.applyAnimation(clipJump, jumpAnimTime)
            }
            PlayerController.State.SLIDING -> {
                slideAnimTime = min(0.7f, slideAnimTime + dt)
                // Slide hold pose mapping
                val clipT = when {
                    slideAnimTime < 0.2f -> slideAnimTime
                    slideAnimTime < 0.45f -> 0.35f
                    else -> (slideAnimTime - 0.45f) + 0.5f
                }
                animator.applyAnimation(clipSlide, clipT.coerceIn(0f, 0.7f))
            }
            PlayerController.State.CRASHED -> {
                animator.applyAnimation(clipCrash, player.crashTimer.coerceAtMost(0.9f))
            }
        }

        if (player.state != PlayerController.State.JUMPING) jumpAnimTime = 0.0f
        if (player.state != PlayerController.State.SLIDING) slideAnimTime = 0.0f

        // 3. Lean Clip (Overlay on LeanPivot)
        player.activeLean?.let { lean ->
            val leanClip = if (lean == "lean_left") clipLeanLeft else clipLeanRight
            val leanProgress = (1.0f - (player.laneChangeTimer / SurferTuning.LANE_CHANGE_DURATION))
            animator.applyAnimation(leanClip, leanProgress * 0.3f)
        }

        // 4. Update Matrices
        animator.updateBoneMatrices()
    }

    fun destroy() {
        try {
            clearActiveObjects()
            val eng = engine ?: return

            duckAsset?.let { assetLoader?.destroyAsset(it) }
            blobShadowAsset?.let { assetLoader?.destroyAsset(it) }
            for (c in chunkRing) { assetLoader?.destroyAsset(c.asset) }
            for (t in trainPool) { assetLoader?.destroyAsset(t) }
            for (b in barrierPool) { assetLoader?.destroyAsset(b) }
            for (d in ductPool) { assetLoader?.destroyAsset(d) }
            for (bc in breadcrumbPool) { assetLoader?.destroyAsset(bc) }

            resourceLoader?.destroy()
            assetLoader?.destroy()

            destroySurface()

            val em = EntityManager.get()
            em.destroy(cameraEntity)
            em.destroy(sunlightEntity)
            em.destroy(ambientLightEntity)

            eng.destroy()
            engine = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
