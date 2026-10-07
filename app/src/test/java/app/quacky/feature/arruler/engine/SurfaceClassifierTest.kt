package app.quacky.feature.arruler.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class SurfaceClassifierTest {

    @Test
    fun `classifies surfaces correctly according to gravity-up cosUp table`() {
        val classifier = SurfaceClassifier()
        classifier.updateFloorY(-1.00)

        // Wall: normal is horizontal (|normal.y| < 0.15)
        val wallNormal = Vec3(1.0, 0.05, 0.0).normalized()
        val wallKind = classifier.classify(Vec3(0.0, 0.0, -1.0), wallNormal, isPlanar = true)
        assertEquals(SurfaceKind.WALL, wallKind)

        // Floor: normal.y > 0.9 and near floorY (-1.00)
        val floorNormal = Vec3(0.0, 0.98, 0.0).normalized()
        val floorKind = classifier.classify(Vec3(0.0, -0.98, -1.0), floorNormal, isPlanar = true)
        assertEquals(SurfaceKind.FLOOR, floorKind)

        // Top surface: normal.y > 0.9 and > 0.15m above floorY
        val tableKind = classifier.classify(Vec3(0.0, -0.20, -1.0), floorNormal, isPlanar = true)
        assertEquals(SurfaceKind.TOP_SURFACE, tableKind)

        // Ceiling: normal.y < -0.9
        val ceilingNormal = Vec3(0.0, -0.98, 0.0).normalized()
        val ceilingKind = classifier.classify(Vec3(0.0, 1.50, -1.0), ceilingNormal, isPlanar = true)
        assertEquals(SurfaceKind.CEILING, ceilingKind)

        // Curved object: isPlanar is false
        val curvedKind = classifier.classify(Vec3(0.0, 0.0, -1.0), wallNormal, isPlanar = false)
        assertEquals(SurfaceKind.CURVED_OBJECT, curvedKind)
    }
}
