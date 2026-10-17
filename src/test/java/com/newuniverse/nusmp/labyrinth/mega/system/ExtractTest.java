package com.newuniverse.nusmp.labyrinth.mega.system;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the extraction rules are the owner's list of exclusions (bosses, other owners' servants, named and persistent mobs) plus the memory that makes the Shadow
 * Core usable only on a creature just slain by that player, nearby, inside the window. Both are pure, so each exclusion, each edge of the window and the radius,
 * the bound of the ledger and the one-time announcement are pinned.
 */
class ExtractTest {
    static final UUID ME = new UUID(1, 1), YOU = new UUID(2, 2);

    static ExtractLedger.Fallen fell(String type, UUID killer, double x, long tick) {
        return new ExtractLedger.Fallen(type, "Name", 20.0, 3.0, x, 65, 0, "nusmp:mega_dungeon", killer, tick);
    }

    static ExtractRules.Facts good() { return new ExtractRules.Facts(true, true, true, true, true, false, false, false, false, false); }

    // ---------------------------------------------------------------- the rules
    @Test void anOrdinaryHostileSlainOnTheHolyRoadMayBeExtracted() {
        assertNull(ExtractRules.refusal(good()));
        assertTrue(ExtractRules.allowed(good()));
    }

    @Test void eachExclusionOfTheBriefIsRefused() {
        assertEquals("not slain by a player", ExtractRules.refusal(new ExtractRules.Facts(false, true, true, true, true, false, false, false, false, false)));
        assertEquals("not on the Holy floors", ExtractRules.refusal(new ExtractRules.Facts(true, false, true, true, true, false, false, false, false, false)));
        assertEquals("not a creature that can follow", ExtractRules.refusal(new ExtractRules.Facts(true, true, true, false, true, false, false, false, false, false)));
        assertEquals("not hostile", ExtractRules.refusal(new ExtractRules.Facts(true, true, false, true, true, false, false, false, false, false)));
        assertEquals("a boss", ExtractRules.refusal(new ExtractRules.Facts(true, true, true, true, true, true, false, false, false, false)));
        assertEquals("already someone's servant", ExtractRules.refusal(new ExtractRules.Facts(true, true, true, true, true, false, true, false, false, false)));
        assertEquals("named", ExtractRules.refusal(new ExtractRules.Facts(true, true, true, true, true, false, false, true, false, false)));
        assertEquals("persistent", ExtractRules.refusal(new ExtractRules.Facts(true, true, true, true, true, false, false, false, true, false)));
        assertEquals("forbidden by the datapack", ExtractRules.refusal(new ExtractRules.Facts(true, true, true, true, true, false, false, false, false, true)));
        assertEquals("cannot fight", ExtractRules.refusal(new ExtractRules.Facts(true, true, true, true, false, false, false, false, false, false)));
    }

    @Test void aBossThatIsAlsoNamedIsRefusedAsABoss() {
        assertEquals("a boss", ExtractRules.refusal(new ExtractRules.Facts(true, true, true, true, true, true, true, true, true, true)));
    }

    // ---------------------------------------------------------------- the ledger
    @Test void aDeathIsFoundByItsKillerNearbyAndInsideTheWindow() {
        ExtractLedger l = new ExtractLedger(8);
        ExtractLedger.Fallen f = fell("minecraft:zombie", ME, 10, 100);
        l.record(f);
        assertSame(f, l.nearest(ME, "nusmp:mega_dungeon", 12, 65, 0, 150, 100, 12));
        assertNull(l.nearest(YOU, "nusmp:mega_dungeon", 12, 65, 0, 150, 100, 12), "only the killer can extract it");
        assertNull(l.nearest(ME, "minecraft:overworld", 12, 65, 0, 150, 100, 12), "another dimension");
        assertNull(l.nearest(ME, "nusmp:mega_dungeon", 40, 65, 0, 150, 100, 12), "too far away");
        assertSame(f, l.nearest(ME, "nusmp:mega_dungeon", 22, 65, 0, 150, 100, 12), "exactly 12 blocks is still in reach");
        assertNull(l.nearest(ME, "nusmp:mega_dungeon", 22.1, 65, 0, 150, 100, 12));
        assertSame(f, l.nearest(ME, "nusmp:mega_dungeon", 12, 65, 0, 200, 100, 12), "exactly the window is still inside it");
        assertNull(l.nearest(ME, "nusmp:mega_dungeon", 12, 65, 0, 201, 100, 12), "one tick later it is gone");
    }

    @Test void theNearestOfSeveralDeathsWins() {
        ExtractLedger l = new ExtractLedger(8);
        ExtractLedger.Fallen far = fell("minecraft:zombie", ME, 10, 100), near = fell("minecraft:skeleton", ME, 3, 90);
        l.record(far);
        l.record(near);
        assertSame(near, l.nearest(ME, "nusmp:mega_dungeon", 0, 65, 0, 110, 100, 12));
    }

    @Test void takingAMemoryUsesItUp() {
        ExtractLedger l = new ExtractLedger(8);
        ExtractLedger.Fallen f = fell("minecraft:zombie", ME, 1, 10);
        l.record(f);
        assertTrue(l.take(f));
        assertFalse(l.take(f));
        assertNull(l.nearest(ME, "nusmp:mega_dungeon", 0, 65, 0, 20, 100, 12));
        assertTrue(l.isEmpty());
    }

    @Test void theLedgerIsBoundedAndForgetsTheOldest() {
        ExtractLedger l = new ExtractLedger(3);
        ExtractLedger.Fallen a = fell("a", ME, 1, 1), b = fell("b", ME, 2, 2), c = fell("c", ME, 3, 3), d = fell("d", ME, 4, 4);
        l.record(a);
        l.record(b);
        l.record(c);
        l.record(d);
        assertEquals(3, l.size());
        assertFalse(l.take(a), "the oldest was pushed out");
        assertTrue(l.take(d));
    }

    @Test void expiryDropsWhatIsOlderThanTheWindow() {
        ExtractLedger l = new ExtractLedger(8);
        l.record(fell("a", ME, 1, 0));
        l.record(fell("b", ME, 1, 90));
        l.expire(150, 100);
        assertEquals(1, l.size());
        l.expire(500, 100);
        assertEquals(0, l.size());
    }

    @Test void anAnnouncementIsMadeOnlyOnceForOneMemory() {
        ExtractLedger l = new ExtractLedger(8);
        ExtractLedger.Fallen a = fell("a", ME, 1, 0), b = fell("b", ME, 1, 0);
        l.record(a);
        l.record(b);
        assertTrue(l.markNotified(a));
        assertFalse(l.markNotified(a));
        assertTrue(l.markNotified(b));
        assertFalse(l.markNotified(fell("zzz", ME, 1, 0)), "a memory the ledger does not hold");
    }

    @Test void clearEmptiesIt() {
        ExtractLedger l = new ExtractLedger(8);
        l.record(fell("a", ME, 1, 0));
        l.clear();
        assertEquals(0, l.size());
    }
}
