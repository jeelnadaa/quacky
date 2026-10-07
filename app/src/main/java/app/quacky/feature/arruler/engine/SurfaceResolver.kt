package app.quacky.feature.arruler.engine

import com.google.ar.core.DepthPoint
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import kotlin.math.abs
import kotlin.math.max

data class HitCandidate(
    val point: Vec3,
    val normal: Vec3,
    val distance: Double,
    val isPlanar: Boolean,
    val quality: Double, // 0..1
    val isLargePlane: Boolean = false
)

class SurfaceResolver(
    private val planeFitter: PlaneFitter = PlaneFitter(),
    private val surfaceClassifier: SurfaceClassifier = SurfaceClassifier()
) {

    fun resolve(
        cameraPosition: Vec3,
        rayDirection: Vec3, // Unit vector
        depthSamples: List<WeightedSample>,
        isDepthReady: Boolean,
        depthMeanConfidence: Float,
        arCoreHits: List<HitResult>
    ): SurfaceHit? {
        val reasons = mutableSetOf<ConfidenceReason>()

        // -------------------------------------------------------------
        // Candidate A: Depth-fit hit
        // -------------------------------------------------------------
        var candidateA: HitCandidate? = null
        if (depthSamples.isNotEmpty()) {
            val medianDist = depthSamples.map { (it.point - cameraPosition).length() }.sorted().let {
                if (it.isNotEmpty()) it[it.size / 2] else 1.0
            }

            val fitResult = planeFitter.fitPlane(
                samples = depthSamples,
                cameraPosition = cameraPosition,
                medianDistance = medianDist
            )

            if (fitResult != null && fitResult.isPlanar) {
                // Intersect camera ray with fitted plane: normal . (cameraPosition + t * rayDirection) = offset
                val denom = fitResult.normal.dot(rayDirection)
                if (abs(denom) > 1e-4) {
                    val t = (fitResult.offset - fitResult.normal.dot(cameraPosition)) / denom
                    if (t in MeasureTuning.DEPTH_MIN_RANGE_METERS..MeasureTuning.DEPTH_MAX_RANGE_METERS) {
                        val worldPt = cameraPosition + rayDirection * t
                        val dist = (worldPt - cameraPosition).length()
                        candidateA = HitCandidate(
                            point = worldPt,
                            normal = fitResult.normal,
                            distance = dist,
                            isPlanar = true,
                            quality = fitResult.inlierRatio
                        )
                    }
                }
            } else {
                // Not planar: take weighted median point
                val sortedByDist = depthSamples.sortedBy { (it.point - cameraPosition).length() }
                if (sortedByDist.isNotEmpty()) {
                    val medSample = sortedByDist[sortedByDist.size / 2]
                    val dist = (medSample.point - cameraPosition).length()
                    // Normal facing camera
                    val normal = (cameraPosition - medSample.point).normalized()
                    candidateA = HitCandidate(
                        point = medSample.point,
                        normal = normal,
                        distance = dist,
                        isPlanar = false,
                        quality = 0.4
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // Candidate B: ARCore plane hit
        // -------------------------------------------------------------
        var candidateB: HitCandidate? = null
        for (hit in arCoreHits) {
            val trackable = hit.trackable
            if (trackable is Plane && trackable.subsumedBy == null &&
                trackable.trackingState == TrackingState.TRACKING
            ) {
                val pose = hit.hitPose
                val inPolygon = trackable.isPoseInPolygon(pose)
                val inExtents = trackable.isPoseInExtents(pose)

                if (inPolygon || inExtents) {
                    val pt = Vec3(pose.tx().toDouble(), pose.ty().toDouble(), pose.tz().toDouble())
                    val dist = (pt - cameraPosition).length()

                    val normalAxis = pose.yAxis
                    var norm = Vec3(normalAxis[0].toDouble(), normalAxis[1].toDouble(), normalAxis[2].toDouble())
                    if (norm.dot(cameraPosition - pt) < 0) {
                        norm = -norm
                    }

                    val extentSqm = (trackable.extentX * trackable.extentZ).toDouble()
                    val isLarge = extentSqm >= MeasureTuning.LARGE_PLANE_MIN_EXTENT_SQM

                    // Update floor tracking if large horizontal plane
                    if (isLarge && norm.y > MeasureTuning.SURFACE_FLOOR_COS_UP) {
                        surfaceClassifier.updateFloorY(pt.y)
                    }

                    candidateB = HitCandidate(
                        point = pt,
                        normal = norm,
                        distance = dist,
                        isPlanar = true,
                        quality = if (isLarge) 0.9 else 0.6,
                        isLargePlane = isLarge
                    )
                    break
                }
            }
        }

        // -------------------------------------------------------------
        // Candidate C: ARCore DepthPoint hit
        // -------------------------------------------------------------
        var candidateC: HitCandidate? = null
        for (hit in arCoreHits) {
            val trackable = hit.trackable
            if (trackable is DepthPoint && trackable.trackingState == TrackingState.TRACKING) {
                val pose = hit.hitPose
                val pt = Vec3(pose.tx().toDouble(), pose.ty().toDouble(), pose.tz().toDouble())
                val dist = (pt - cameraPosition).length()
                val norm = (cameraPosition - pt).normalized()
                candidateC = HitCandidate(
                    point = pt,
                    normal = norm,
                    distance = dist,
                    isPlanar = false,
                    quality = 0.5
                )
                break
            }
        }

        // -------------------------------------------------------------
        // Fusion
        // -------------------------------------------------------------
        val refDist = candidateA?.distance ?: candidateB?.distance ?: candidateC?.distance ?: return null
        val tol = max(
            MeasureTuning.FUSION_BASE_TOLERANCE_METERS,
            MeasureTuning.FUSION_DISTANCE_SCALE * refDist
        )

        var finalPoint: Vec3
        var finalNormal: Vec3
        var finalSource: HitSource
        var finalDistance: Double
        var finalConfidence = 0.5f
        var isPlanar = true

        if (candidateA != null && candidateB != null) {
            val distDiff = abs(candidateA.distance - candidateB.distance)
            if (distDiff <= tol) {
                // Agree: weighted blend
                val wA = 0.6 + 0.4 * candidateA.quality
                val wB = if (candidateB.isLargePlane) 0.8 else 0.5
                val sumW = wA + wB

                finalPoint = (candidateA.point * wA + candidateB.point * wB) / sumW
                finalNormal = (candidateA.normal * wA + candidateB.normal * wB).normalized()
                finalSource = HitSource.FUSED
                finalDistance = (finalPoint - cameraPosition).length()
                finalConfidence = 0.85f
                isPlanar = true
            } else {
                // Disagree
                reasons.add(ConfidenceReason.SURFACES_DISAGREED)
                if (isDepthReady && candidateA.quality >= MeasureTuning.DISAGREEMENT_HIGH_QUALITY_FIT) {
                    finalPoint = candidateA.point
                    finalNormal = candidateA.normal
                    finalSource = HitSource.DEPTH_FIT
                    finalDistance = candidateA.distance
                    finalConfidence = 0.65f
                    isPlanar = candidateA.isPlanar
                } else if (candidateB.isLargePlane) {
                    finalPoint = candidateB.point
                    finalNormal = candidateB.normal
                    finalSource = HitSource.PLANE
                    finalDistance = candidateB.distance
                    finalConfidence = 0.70f
                    isPlanar = true
                } else {
                    finalPoint = candidateA.point
                    finalNormal = candidateA.normal
                    finalSource = HitSource.DEPTH_FIT
                    finalDistance = candidateA.distance
                    finalConfidence = 0.50f
                    isPlanar = candidateA.isPlanar
                }
            }
        } else if (candidateA != null) {
            finalPoint = candidateA.point
            finalNormal = candidateA.normal
            finalSource = HitSource.DEPTH_FIT
            finalDistance = candidateA.distance
            finalConfidence = if (candidateA.isPlanar) 0.75f else 0.45f
            isPlanar = candidateA.isPlanar
        } else if (candidateB != null) {
            finalPoint = candidateB.point
            finalNormal = candidateB.normal
            finalSource = HitSource.PLANE
            finalDistance = candidateB.distance
            finalConfidence = if (candidateB.isLargePlane) 0.80f else 0.60f
            isPlanar = true
        } else if (candidateC != null) {
            finalPoint = candidateC.point
            finalNormal = candidateC.normal
            finalSource = HitSource.DEPTH_POINT
            finalDistance = candidateC.distance
            finalConfidence = 0.40f
            isPlanar = false
            reasons.add(ConfidenceReason.LOW_DEPTH_DATA)
        } else {
            return null
        }

        if (finalDistance > MeasureTuning.WARN_TARGET_DISTANCE_METERS) {
            reasons.add(ConfidenceReason.FAR_AWAY)
        } else if (finalDistance < MeasureTuning.MIN_TARGET_DISTANCE_METERS) {
            reasons.add(ConfidenceReason.TOO_CLOSE)
        }

        val kind = surfaceClassifier.classify(finalPoint, finalNormal, isPlanar)

        return SurfaceHit(
            worldPoint = finalPoint,
            normal = finalNormal,
            kind = kind,
            source = finalSource,
            distanceFromCamera = finalDistance.toFloat(),
            confidence = finalConfidence,
            reasons = reasons
        )
    }
}
