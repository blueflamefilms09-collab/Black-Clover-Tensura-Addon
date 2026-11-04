package com.newuniverse.nusmp.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NenShapesTest {
    @Test
    void nenShapesAreAppendedAfterTheOlderOnes() {
        VfxShape[] v = VfxShape.values();
        assertEquals(VfxShape.NEN_BEAM, v[v.length - 1]);
        assertTrue(VfxShape.NEN_AURA.ordinal() > VfxShape.ULT_THOUSAND_ELF.ordinal());
        assertEquals(VfxShape.NEN_AURA.ordinal() + 4, VfxShape.NEN_BEAM.ordinal());
    }
}
