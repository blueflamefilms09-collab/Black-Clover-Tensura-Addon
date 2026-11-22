package com.newuniverse.nusmp.arc.goku;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** 4.103.0: Goku's answers to the Devil x Slime (DxD) mod and the mods in the same list. */
class GokuCountersTest {
    @Test void theListIsParsedSafely() {
        assertEquals(List.of("dxdtensura", "other"), GokuCounters.parse(" DxDTensura , other,, dxdtensura "));
        assertTrue(GokuCounters.parse(null).isEmpty());
        assertTrue(GokuCounters.parse(" , ").isEmpty());
        assertEquals(16, GokuCounters.parse("a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s").size());
    }

    @Test void idsMatchByNamespace() {
        List<String> m = GokuCounters.parse(GokuCounters.DEFAULT_MODS);
        assertTrue(GokuCounters.listed(m, "dxdtensura:divided"));
        assertTrue(GokuCounters.listed(m, "DXDTENSURA:reduced"));
        assertFalse(GokuCounters.listed(m, "minecraft:slowness"));
        assertFalse(GokuCounters.listed(m, "tensura:burn"));
        assertFalse(GokuCounters.listed(m, null));
        assertFalse(GokuCounters.listed(List.of(), "dxdtensura:divided"));
    }

    @Test void damageFactorIsTheSettingOnlyForListedMods() {
        assertEquals(0.5f, GokuCounters.damageFactor(true, 0.5), 1e-6f);
        assertEquals(1f, GokuCounters.damageFactor(false, 0.5), 0f);
        assertEquals(0.05f, GokuCounters.damageFactor(true, 0.0), 1e-6f);
        assertEquals(1f, GokuCounters.damageFactor(true, 9.0), 0f);
        assertEquals(1f, GokuCounters.damageFactor(true, Double.NaN), 0f);
    }

    @Test void theFlareHasACooldown() {
        assertTrue(GokuCounters.mayFlare(1000, -1000));
        assertFalse(GokuCounters.mayFlare(1050, 1000));
        assertTrue(GokuCounters.mayFlare(1100, 1000));
    }
}
