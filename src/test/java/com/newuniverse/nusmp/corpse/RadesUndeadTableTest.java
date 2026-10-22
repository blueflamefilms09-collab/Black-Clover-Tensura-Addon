package com.newuniverse.nusmp.corpse;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The table of Rades Spirito's numbered undead: numbers, names, roles, the signature moves and their caps (no game types). */
class RadesUndeadTableTest {
    @Test
    void fiveNumberedUndeadWithTheSeriesNumbersAndNames() {
        assertEquals(5, NamedCorpses.values().length);
        assertEquals("No.4 - Jimmy", NamedCorpses.JIMMY.title());
        assertEquals("No.3 - David", NamedCorpses.DAVID.title());
        assertEquals("No.2 - Alfredo", NamedCorpses.ALFREDO.title());
        assertEquals("No.1 - Carl", NamedCorpses.CARL.title());
        assertEquals("No.0 - Michael Caesal", NamedCorpses.MICHAEL_CAESAL.title());
        Set<Integer> numbers = new HashSet<>();
        Set<String> ids = new HashSet<>();
        for (NamedCorpses n : NamedCorpses.values()) {
            assertTrue(numbers.add(n.number) && n.number >= 0 && n.number <= 4);
            assertTrue(ids.add(n.id) && n.id.startsWith("corpse_"));
            assertSame(n, NamedCorpses.byId(n.id));
        }
    }

    @Test
    void theOldThreeKeepTheirIdsAndStatsAndTheNewTwoAreAppended() {
        // the 1.43.0 rows, unchanged (saves, balance and the registry ids depend on them)
        assertEquals(java.util.List.of("corpse_jimmy", 70.0, 7.0, 0.36, 2.0, 0.10), row(NamedCorpses.JIMMY));
        assertEquals(java.util.List.of("corpse_alfredo", 260.0, 12.0, 0.20, 10.0, 0.90), row(NamedCorpses.ALFREDO));
        assertEquals(java.util.List.of("corpse_carl", 56.0, 3.0, 0.26, 0.0, 0.0), row(NamedCorpses.CARL));
        assertEquals(NamedCorpses.JIMMY, NamedCorpses.values()[0]);
        assertEquals(NamedCorpses.ALFREDO, NamedCorpses.values()[1]);
        assertEquals(NamedCorpses.CARL, NamedCorpses.values()[2], "enum entries are only ever appended (ordinals are used by tables and renderers)");
        assertEquals(NamedCorpses.DAVID, NamedCorpses.values()[3]);
        assertEquals(NamedCorpses.MICHAEL_CAESAL, NamedCorpses.values()[4]);
        assertEquals("corpse_david", NamedCorpses.DAVID.id);
        assertEquals("corpse_michael_caesal", NamedCorpses.MICHAEL_CAESAL.id);
        for (NamedCorpses o : new NamedCorpses[]{NamedCorpses.JIMMY, NamedCorpses.ALFREDO, NamedCorpses.CARL}) assertEquals(1.0, o.lifeFactor, "the old three stay the full configured life");
    }

    private static java.util.List<Object> row(NamedCorpses n) { return java.util.List.of(n.id, n.health, n.attack, n.speed, n.armor, n.knockbackResistance); }

    @Test
    void theRolesAreDistinct() {
        NamedCorpses j = NamedCorpses.JIMMY, d = NamedCorpses.DAVID, a = NamedCorpses.ALFREDO, c = NamedCorpses.CARL, m = NamedCorpses.MICHAEL_CAESAL;
        // David: huge, slow, a wall of health
        for (NamedCorpses o : new NamedCorpses[]{j, a, c, m}) {
            assertTrue(d.health > o.health, "David has the most health");
            assertTrue(d.speed <= o.speed, "David is the slowest");
            assertTrue(d.height > o.height && d.width > o.width, "David is the biggest");
        }
        // Michael Caesal: the strongest blow and the fastest feet, the elite
        for (NamedCorpses o : new NamedCorpses[]{j, d, a, c}) {
            assertTrue(m.attack > o.attack, "Michael Caesal hits hardest");
            assertTrue(m.speed >= o.speed, "Michael Caesal is the fastest");
        }
        assertTrue(m.elite() && !j.elite() && !d.elite() && !a.elite() && !c.elite());
        assertTrue(m.lifeFactor < 1.0 && d.lifeFactor < 1.0, "the heavy and the elite stay shorter");
        // Carl is the only ranged one, Jimmy is the fast brawler of the first three
        assertTrue(c.ranged() && !m.ranged() && !d.ranged() && !a.ranged() && !j.ranged());
        assertTrue(j.speed > a.speed && j.speed > c.speed);
        // every one has its own signature or its own role move
        assertEquals(NamedCorpses.Signature.SOUL_BARRIER, a.signature);
        assertEquals(NamedCorpses.Signature.GROUND_SLAM, d.signature);
        assertEquals(NamedCorpses.Signature.SOUL_BARRAGE, m.signature);
        assertEquals(NamedCorpses.Signature.NONE, j.signature);
        assertEquals(NamedCorpses.Signature.NONE, c.signature, "Carl keeps his shards and bolts");
    }

    @Test
    void theSignatureMovesStayInsideTheBalanceCaps() {
        // soul barrage: a fixed number of bolts of fixed damage (no percent of anyone's maximum health), one target
        double total = NamedCorpses.BARRAGE_BOLTS * NamedCorpses.BARRAGE_DAMAGE;
        assertTrue(total <= 40, "a barrage is at most 40 raw damage in all: " + total);
        assertTrue(NamedCorpses.BARRAGE_EVERY_TICKS >= 160, "no more often than every 8 seconds");
        assertTrue(NamedCorpses.BARRAGE_BOLTS * NamedCorpses.BARRAGE_GAP_TICKS < NamedCorpses.BARRAGE_EVERY_TICKS);
        // ground slam: part of a blow, a capped number of victims
        assertTrue(NamedCorpses.SLAM_FACTOR <= 1.0 && NamedCorpses.SLAM_MAX_TARGETS <= 8 && NamedCorpses.SLAM_RADIUS <= 4.0);
        assertTrue(NamedCorpses.SLAM_EVERY_TICKS >= 120);
        // soul barrier: a shield of Absorption II at most, a capped number of people, never more often than every 15 seconds, shorter than its own cooldown
        assertTrue(NamedCorpses.BARRIER_AMPLIFIER <= 1 && NamedCorpses.BARRIER_MAX_TARGETS <= 8 && NamedCorpses.BARRIER_EVERY_TICKS >= 300);
        assertTrue(NamedCorpses.BARRIER_TICKS < NamedCorpses.BARRIER_EVERY_TICKS);
        // none opens with its move, and the elite page has the long cooldown
        assertTrue(NamedCorpses.SIGNATURE_OPENING_TICKS >= 40);
        assertTrue(NamedCorpses.ELITE_PAGE_COOLDOWN >= 3000 && NamedCorpses.DAVID_PAGE_COOLDOWN > 160);
        // the three at once cap lets the first three puppets coexist, as before, and never five elites
        assertEquals(3, NamedCorpses.MAX_AT_ONCE);
    }

    @Test
    void lifeFollowsTheFactorAndNeverVanishesInstantly() {
        assertEquals(3600, NamedCorpses.JIMMY.lifeTicks(3600));
        assertEquals(2700, NamedCorpses.DAVID.lifeTicks(3600));
        assertEquals(1800, NamedCorpses.MICHAEL_CAESAL.lifeTicks(3600));
        assertTrue(NamedCorpses.MICHAEL_CAESAL.lifeTicks(1) >= 20);
    }
}
