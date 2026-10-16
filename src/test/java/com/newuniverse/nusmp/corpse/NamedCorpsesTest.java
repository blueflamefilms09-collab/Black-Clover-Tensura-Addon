package com.newuniverse.nusmp.corpse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The stat table of the named puppets and the roster that caps live bodies. */
class NamedCorpsesTest {
    @Test
    void rolesAreDistinct() {
        NamedCorpses j = NamedCorpses.JIMMY, a = NamedCorpses.ALFREDO, c = NamedCorpses.CARL;
        assertTrue(j.speed > a.speed && j.speed > c.speed, "Jimmy is the fast one");
        assertTrue(a.health > j.health * 2 && a.health > c.health * 2, "Alfredo is the wall");
        assertTrue(a.armor > j.armor && a.knockbackResistance > 0.5);
        assertTrue(c.ranged() && !j.ranged() && !a.ranged());
        assertTrue(c.shardDamage > 0 && j.shardDamage == 0 && a.shardDamage == 0);
        assertTrue(a.width > j.width && a.height > j.height);
    }

    @Test
    void idsAndLookup() {
        for (NamedCorpses n : NamedCorpses.values()) {
            assertSame(n, NamedCorpses.byId(n.id));
            assertTrue(n.id.startsWith("corpse_"));
            assertTrue(n.health > 0 && n.attack > 0 && n.speed > 0);
        }
        assertNull(NamedCorpses.byId("corpse_nobody"));
    }

    @Test
    void rosterEvictsTheOldest() {
        CorpseRoster<Integer, Integer> r = new CorpseRoster<>();
        assertTrue(r.add(1, 10, 2).isEmpty());
        assertTrue(r.add(1, 11, 2).isEmpty());
        assertEquals(java.util.List.of(10), r.add(1, 12, 2));
        assertEquals(2, r.count(1));
        assertEquals(0, r.count(2));
        assertTrue(r.remove(1, 11));
        assertFalse(r.remove(1, 11));
        r.remove(1, 12);
        assertTrue(r.isEmpty());
    }
}
