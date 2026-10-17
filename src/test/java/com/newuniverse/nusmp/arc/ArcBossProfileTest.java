package com.newuniverse.nusmp.arc;

import com.newuniverse.nusmp.boss.BossBuffTable;
import com.newuniverse.nusmp.boss.BossRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The tier ladder of the global buff engine for the arc bosses, and how an arc boss joins the (otherwise fixed) boss tables without disturbing the old ones. */
class ArcBossProfileTest {
    private static int strength(ArcBossProfile.Template t) {
        int s = 0;
        for (int v : t.levels().values()) s += v;
        return s;
    }

    @Test
    void theTierIsClampedToTheFiveRungs() {
        assertEquals(1, ArcBossProfile.forTier(-3).tier());
        assertEquals(1, ArcBossProfile.forTier(0).tier());
        assertEquals(3, ArcBossProfile.forTier(3).tier());
        assertEquals(5, ArcBossProfile.forTier(5).tier());
        assertEquals(5, ArcBossProfile.forTier(99).tier());
    }

    @Test
    void everyRungGivesSomethingUsesOnlyAllowedThingsAndNeverGetsWeaker() {
        int last = 0, lastAura = 0;
        for (int tier = ArcBossProfile.MIN_TIER; tier <= ArcBossProfile.MAX_TIER; tier++) {
            ArcBossProfile.Template t = ArcBossProfile.forTier(tier);
            assertFalse(t.levels().isEmpty(), "tier " + tier);
            for (String effect : t.levels().keySet()) {
                assertTrue(BossBuffTable.EFFECT_IDS.contains(effect), tier + ": " + effect + " is an engine effect");
                assertNotEquals(BossBuffTable.INSTANT_REGENERATION, effect);       // the boss rules forbid it; it would only be stripped
            }
            for (int level : t.levels().values()) assertTrue(level >= 1 && level <= BossBuffTable.MAX_LEVEL, "tier " + tier);
            for (String skill : t.skills()) {
                assertNotEquals(BossBuffTable.STARVED, skill);                       // Unique skills are forbidden to bosses
                assertNotEquals(BossBuffTable.MATHEMATICIAN, skill);
            }
            assertTrue(strength(t) >= last, "tier " + tier + " is not weaker than the one below");
            assertTrue(t.auraRadius() >= lastAura);
            last = strength(t);
            lastAura = t.auraRadius();
        }
        assertTrue(strength(ArcBossProfile.forTier(5)) > strength(ArcBossProfile.forTier(1)));
    }

    @Test
    void aProfileIsBuiltForTheBossId() {
        BossBuffTable.Profile p = ArcBossProfile.profileFor("heath_grice", 2);
        assertEquals("heath_grice", p.bossId());
        assertEquals(2, p.tier());
        assertEquals(ArcBossProfile.forTier(2).levels(), p.levels());
        ArcCharacter c = ArcCharacter.boss("zagred", "Zagred").tier(4).build();
        assertEquals(ArcBossProfile.profileFor("zagred", 4), ArcBossProfile.profileFor(c));
    }

    @Test
    void anArcBossJoinsTheBuffTableAndTheBossRegistryWithoutTouchingTheOldRows() {
        int oldRows = BossBuffTable.all().size(), oldIds = BossRegistry.ids().size();
        try {
            BossBuffTable.Profile p = ArcBossProfile.profileFor("probe_boss", 3);
            assertTrue(BossBuffTable.addArc(p));
            assertTrue(BossBuffTable.addArc(p), "the same row again is harmless");
            assertFalse(BossBuffTable.addArc(ArcBossProfile.profileFor("probe_boss", 5)), "a different row under a taken id is refused");
            assertFalse(BossBuffTable.addArc(ArcBossProfile.profileFor(BossRegistry.GOLIATH, 5)), "an old boss's row cannot be replaced");
            assertEquals(p, BossBuffTable.profile("probe_boss"));
            assertEquals(oldRows + 1, BossBuffTable.all().size());
            assertFalse(BossBuffTable.plan("probe_boss", 1.0, false).isEmpty(), "the engine has something to apply");
            assertEquals(1, BossBuffTable.profile(BossRegistry.GOLIATH).tier(), "Goliath keeps its own row");

            assertTrue(BossRegistry.addArc("probe_boss", "probe_boss"));
            assertTrue(BossRegistry.addArc("probe_boss", "probe_boss"), "the same entry again is harmless");
            assertFalse(BossRegistry.addArc("probe_boss", "other"), "a path under a taken path is refused");
            assertFalse(BossRegistry.addArc("goliath", "probe_two"), "an old boss's path cannot be taken");
            assertFalse(BossRegistry.addArc("probe_two", BossRegistry.GOLIATH), "an old boss's id cannot be taken");
            assertFalse(BossRegistry.addArc("", "x"));
            assertEquals("probe_boss", BossRegistry.idOf("nusmp", "probe_boss").orElseThrow());
            assertTrue(BossRegistry.idOf("minecraft", "probe_boss").isEmpty(), "only this mod's namespace");
            assertTrue(BossRegistry.known("probe_boss"));
            assertEquals(oldIds + 1, BossRegistry.ids().size());
            assertEquals("goliath", BossRegistry.idOf("nusmp", "goliath").orElseThrow());
        } finally {
            BossBuffTable.clearArcForTests();
            BossRegistry.clearArcForTests();
        }
        assertEquals(oldRows, BossBuffTable.all().size());
        assertEquals(oldIds, BossRegistry.ids().size());
        assertTrue(BossRegistry.idOf("nusmp", "probe_boss").isEmpty());
        assertNull(BossBuffTable.profile("probe_boss"));
        assertEquals(List.of("dante", "vanica", "zenon", "lucius", "devil_avatar", "goliath", "charybdis_spawn"), BossRegistry.ids());
    }
}
