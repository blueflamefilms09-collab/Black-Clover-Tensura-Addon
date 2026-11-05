package com.newuniverse.nusmp.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NenShapesTest {
    @Test
    void nenShapesAreAppendedAfterTheOlderOnes() {
        VfxShape[] v = VfxShape.values();
        assertEquals(VfxShape.NEN_DOMAIN, v[v.length - 1], "3.57.0: the remade Nen shapes are the last ones");
        assertTrue(VfxShape.NEN_AURA.ordinal() > VfxShape.ULT_THOUSAND_ELF.ordinal());
        assertEquals(VfxShape.NEN_AURA.ordinal() + 4, VfxShape.NEN_BEAM.ordinal());
    }

    @Test
    void theRemadeNenShapesFollowTheOldBeamInOrder() {
        VfxShape[] order = {VfxShape.NEN_FLOW, VfxShape.NEN_BURST, VfxShape.NEN_SENSE, VfxShape.NEN_EYES, VfxShape.NEN_FIST,
                VfxShape.NEN_BULLET, VfxShape.NEN_SURGE, VfxShape.NEN_LINK, VfxShape.NEN_NEEDLE, VfxShape.NEN_DOMAIN};
        for (int i = 0; i < order.length; i++) assertEquals(VfxShape.NEN_BEAM.ordinal() + 1 + i, order[i].ordinal(), "appended in order: " + order[i]);
    }
}
