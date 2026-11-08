package top.yukonga.miuix.kmp.glass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class GlassUnderlayBlurTest {
    @Test
    fun noUnderlayPreservesOriginalMaterial() {
        val base = GlassMaterials.PuredThinGlassLight
        assertSame(base, base.withUnderlayBlur(null))
    }

    @Test
    fun lightTopBarMatchesNativeVarianceComposition() {
        val base = GlassMaterials.PuredThinGlassLight
        val combined = base.withUnderlayBlur(GlassMaterials.ActionBarMaskLight)
        assertEquals(44.72136f, combined.blurRadius.value, 0.001f)
        assertEquals(base.first, combined.first)
        assertEquals(base.second, combined.second)
        assertEquals(base.third, combined.third)
    }

    @Test
    fun darkTopBarMatchesNativeVarianceComposition() {
        val base = GlassMaterials.PuredThinGlassDark
        val combined = base.withUnderlayBlur(GlassMaterials.ActionBarMaskDark)
        assertEquals(63.24555f, combined.blurRadius.value, 0.001f)
        assertEquals(base.first, combined.first)
        assertEquals(base.second, combined.second)
        assertEquals(base.third, combined.third)
    }
}
