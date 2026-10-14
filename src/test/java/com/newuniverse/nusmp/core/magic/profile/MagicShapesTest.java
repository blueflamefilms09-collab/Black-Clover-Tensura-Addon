package com.newuniverse.nusmp.core.magic.profile;

import com.newuniverse.nusmp.vfx.VfxShape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MagicShapesTest {
    @Test
    void aMagicWithItsOwnFxTripleDrawsIt() {
        MagicShapes.ShapeSet s = MagicShapes.of("BONE", false);
        assertEquals(VfxShape.BONE_FX1, s.cast());
        assertEquals(VfxShape.BONE_FX2, s.field());
        assertEquals(VfxShape.BONE_FX3, s.burst());
        assertEquals(VfxStyle.NATIVE, s.style());
        assertTrue(MagicShapes.hasFxTriple("bone"));
        assertFalse(MagicShapes.hasFxTriple("FLAME"));
    }

    @Test
    void theLegacyMagicsKeepTheirOwnSets() {
        assertEquals(VfxShape.FLAME_TRAIL, MagicShapes.of("FLAME", false).cast());
        assertEquals(VfxShape.WATER_NEST, MagicShapes.of("WATER", false).field());
        assertEquals(VfxShape.TIME_STASIS, MagicShapes.of("TIME", false).field());
        assertEquals(VfxStyle.NATIVE, MagicShapes.of("EARTH", false).style());
        assertTrue(MagicShapes.hasLegacySet("flame"));
    }

    @Test
    void wikiAttributesUseTheSharedLayersAndEverythingElseTheGenericOnes() {
        assertEquals(VfxStyle.WIKI, MagicShapes.of("AIR", true).style());
        assertEquals(VfxShape.WIKI_MAGIC_CAST, MagicShapes.of("AIR", true).cast());
        assertEquals(VfxStyle.GENERIC, MagicShapes.of("AIR", false).style());
        assertSame(MagicShapes.GENERIC, MagicShapes.of("SOMETHING_NEW", false));
        assertSame(MagicShapes.GENERIC, MagicShapes.of("", true));
        assertSame(MagicShapes.GENERIC, MagicShapes.of(null, true));
        assertSame(MagicShapes.GENERIC, MagicShapes.of("merged:a+b", false));
    }

    @Test
    void shapeNamedNeverThrows() {
        assertNull(MagicShapes.shapeNamed(null));
        assertNull(MagicShapes.shapeNamed(""));
        assertNull(MagicShapes.shapeNamed("NOT_A_SHAPE"));
        assertEquals(VfxShape.WIKI_MAGIC_BURST, MagicShapes.shapeNamed("wiki_magic_burst"));
    }

    @Test
    void everyShapeInEveryAnswerIsPresent() {
        for (String n : new String[]{"FLAME", "BONE", "AIR", "NOPE", "TIME", "ANTI_MAGIC", "COTTON", "GAME"}) {
            for (boolean wiki : new boolean[]{false, true}) {
                MagicShapes.ShapeSet s = MagicShapes.of(n, wiki);
                assertNotNull(s.cast(), n);
                assertNotNull(s.field(), n);
                assertNotNull(s.burst(), n);
                assertNotNull(s.style(), n);
            }
        }
    }
}
