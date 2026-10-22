package com.newuniverse.nusmp.spiritcontrol;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the registry every spirit package registers with is the heart of the control plane, so its rules are pinned without Minecraft: order is kept, an id is registered once, the very same object twice is
 * a no-op (a Boot that runs twice), a different object with the same id is refused, a spirit with too many abilities is refused, lookup ignores case and the book lookup finds the right spirits.
 */
class SpiritRegistryTest {
    private record Fake(String id, Set<String> magics, int abilityCount) implements SpiritInfo {
        @Override public String display() { return id; }
        @Override public List<SpiritAbilityDef> abilities() {
            return java.util.stream.IntStream.range(0, abilityCount).mapToObj(i -> new SpiritAbilityDef("a" + i, "A" + i, 0.0)).toList();
        }
        @Override public List<SpiritSpellDef> spells() { return SpiritCatalog.FIRE_SPELLS; }
    }

    @Test
    void orderIsKeptAndLookupIgnoresCase() {
        SpiritRegistry<Fake> r = new SpiritRegistry<>();
        Fake a = new Fake("sylph", Set.of("WIND"), 1), b = new Fake("lord", Set.of("WIND", "FLAME"), 0);
        assertTrue(r.register(a));
        assertTrue(r.register(b));
        assertEquals(List.of(a, b), r.all());
        assertSame(a, r.get("SYLPH"));
        assertNull(r.get("nobody"));
        assertNull(r.get(null));
        assertEquals(2, r.size());
    }

    @Test
    void theSameObjectTwiceIsIgnoredAndADifferentOneWithTheSameIdIsRefused() {
        SpiritRegistry<Fake> r = new SpiritRegistry<>();
        Fake a = new Fake("gnome", Set.of("EARTH"), 1);
        assertTrue(r.register(a));
        assertFalse(r.register(a));
        boolean refused = false;
        try { r.register(new Fake("gnome", Set.of("EARTH"), 1)); } catch (IllegalStateException e) { refused = true; }
        assertTrue(refused, "a second spirit with the id gnome");
        assertEquals(1, r.size());
    }

    @Test
    void anEmptyIdOrTooManyAbilitiesAreRefused() {
        SpiritRegistry<Fake> r = new SpiritRegistry<>();
        boolean a = false, b = false, c = false;
        try { r.register(new Fake("", Set.of("WIND"), 0)); } catch (IllegalArgumentException e) { a = true; }
        try { r.register(new Fake("big", Set.of("WIND"), PageGate.MAX_ABILITIES + 1)); } catch (IllegalArgumentException e) { b = true; }
        try { r.register(null); } catch (IllegalArgumentException e) { c = true; }
        assertTrue(a && b && c);
        assertEquals(0, r.size());
        assertTrue(r.register(new Fake("exact", Set.of("WIND"), PageGate.MAX_ABILITIES)), "the limit itself is allowed");
    }

    @Test
    void theBookLookupFindsTheSpiritsOfAMagicInRegistrationOrder() {
        SpiritRegistry<Fake> r = new SpiritRegistry<>();
        Fake fire = new Fake("salamander", Set.of("FLAME"), 1), wind = new Fake("sylph", Set.of("WIND"), 1), lord = new Fake("lord", Set.of("FLAME", "WIND"), 0);
        r.register(fire);
        r.register(wind);
        r.register(lord);
        assertEquals(List.of(fire, lord), r.forMagic("FLAME"));
        assertEquals(List.of(wind, lord), r.forMagic("WIND"));
        assertTrue(r.forMagic("TIME").isEmpty());
        assertTrue(r.forMagic(null).isEmpty());
    }
}
