package com.newuniverse.nusmp.heroweapon.bond;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Why: the register of who holds which weapon is what makes a Holy Weapon unique across offline players, so its rules are pinned: a claim is idempotent,
 * releasing frees only the releaser, a second holder of a shared weapon keeps the first, a stale claim can be swept, and the register survives the round trip
 * through the map the saved-data class writes.
 */
class HeroRegistryDataTest {
    @Test
    void aClaimIsIdempotentAndTheFirstHolderIsRemembered() {
        HeroRegistryData r = new HeroRegistryData();
        assertTrue(r.claim("legendary_shield", "u1"));
        assertFalse(r.claim("legendary_shield", "u1"), "the same claim again changes nothing");
        assertEquals("u1", r.firstHolder("legendary_shield"));
        assertNull(r.firstHolder("legendary_sword"));
        assertEquals(List.of("u1"), r.holdersOf("legendary_shield"));
        assertTrue(r.holdersOf("legendary_sword").isEmpty());
    }

    @Test
    void otherHolderIsWhatAUniquenessGateAsks() {
        HeroRegistryData r = new HeroRegistryData();
        assertNull(r.otherHolder("legendary_bow", "me"), "nobody holds it");
        r.claim("legendary_bow", "me");
        assertNull(r.otherHolder("legendary_bow", "me"), "only I hold it");
        r.claim("legendary_bow", "you");
        assertEquals("you", r.otherHolder("legendary_bow", "me"));
        assertEquals("me", r.otherHolder("legendary_bow", "you"));
    }

    @Test
    void releasingFreesOnlyTheReleaser() {
        HeroRegistryData r = new HeroRegistryData();
        r.claim("vassal_fan", "a");
        r.claim("vassal_fan", "b");
        assertTrue(r.release("vassal_fan", "a"));
        assertFalse(r.release("vassal_fan", "a"), "already free");
        assertEquals(List.of("b"), r.holdersOf("vassal_fan"));
        assertTrue(r.release("vassal_fan", "b"));
        assertTrue(r.isEmpty(), "a weapon with no holder is not listed");
        assertFalse(r.release("vassal_fan", "b"));
    }

    @Test
    void releaseAllExceptKeepsOnlyTheNamedWeapon() {
        HeroRegistryData r = new HeroRegistryData();
        r.claim("legendary_shield", "u");
        r.claim("legendary_sword", "u");
        r.claim("legendary_sword", "v");
        assertTrue(r.releaseAllExcept("u", "legendary_sword"));
        assertEquals(List.of(), r.holdersOf("legendary_shield"));
        assertEquals(List.of("u", "v"), r.holdersOf("legendary_sword"));
        assertFalse(r.releaseAllExcept("u", "legendary_sword"), "nothing left to free");
        assertTrue(r.releaseAllExcept("u", null));
        assertEquals(List.of("v"), r.holdersOf("legendary_sword"));
    }

    @Test
    void sweepDropsStaleClaimsOnly() {
        HeroRegistryData r = new HeroRegistryData();
        r.claim("legendary_shield", "gone");
        r.claim("legendary_shield", "here");
        r.claim("legendary_bow", "gone");
        int n = r.sweep((w, h) -> h.equals("gone"));
        assertEquals(2, n);
        assertEquals(List.of("here"), r.holdersOf("legendary_shield"));
        assertTrue(r.holdersOf("legendary_bow").isEmpty());
        assertEquals(1, r.weapons().size());
    }

    @Test
    void clearWeaponDropsEveryHolderOfOneWeapon() {
        HeroRegistryData r = new HeroRegistryData();
        r.claim("vassal_chain", "a");
        r.claim("vassal_chain", "b");
        r.claim("vassal_fan", "a");
        assertEquals(2, r.clearWeapon("vassal_chain"));
        assertEquals(0, r.clearWeapon("vassal_chain"));
        assertEquals(List.of("a"), r.holdersOf("vassal_fan"));
    }

    @Test
    void theRegisterSurvivesTheMapRoundTrip() {
        HeroRegistryData r = new HeroRegistryData();
        r.claim("legendary_shield", "u1");
        r.claim("vassal_fan", "u2");
        r.claim("vassal_fan", "u3");
        Map<String, List<String>> saved = r.toMap();
        HeroRegistryData back = new HeroRegistryData();
        back.claim("stale", "x");
        back.load(saved);
        assertEquals(saved, back.toMap());
        assertTrue(back.holdersOf("stale").isEmpty(), "load replaces what was there");
        back.load(null);
        assertTrue(back.isEmpty());
    }

    @Test
    void emptyNamesAreNeverRecorded() {
        HeroRegistryData r = new HeroRegistryData();
        assertFalse(r.claim("", "u"));
        assertFalse(r.claim("legendary_shield", ""));
        assertFalse(r.claim(null, "u"));
        assertFalse(r.claim("legendary_shield", null));
        assertTrue(r.isEmpty());
    }
}
