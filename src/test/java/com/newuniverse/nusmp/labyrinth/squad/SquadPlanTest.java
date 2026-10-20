package com.newuniverse.nusmp.labyrinth.squad;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The composition rules of {@link SquadPlan}: size, roles, determinism, the faction tilt and the fitting of an encounter to a squad. */
class SquadPlanTest {
    private static final String[] FACTIONS = {"undead", "goblin", "ogre", "beast", "slime", "construct", "cultist", "demon", "elemental", "aquatic", "insect", "", null, "unknown"};

    @Test
    void everyDepthFrom1To1000GivesAValidSquad() {
        for (int depth = -1000; depth <= 1000; depth += (Math.abs(depth) < 40 ? 1 : 7)) {
            for (int party = 1; party <= 8; party++) {
                for (int band = SquadPlan.MINOR; band <= SquadPlan.AMBUSH; band++) {
                    for (String f : FACTIONS) {
                        long seed = depth * 1_000_003L + party * 7919L + band * 31L + (f == null ? 0 : f.hashCode());
                        SquadPlan.Plan p = SquadPlan.plan(seed, depth, party, band, f, "necropolis");
                        check(p, depth + "/" + party + "/" + band + "/" + f);
                    }
                }
            }
        }
    }

    private static void check(SquadPlan.Plan p, String what) {
        assertTrue(p.size() >= 5 && p.size() <= 8, what + ": size " + p.size());
        assertEquals(p.size(), p.roles().size(), what + ": one role per member");
        int tanks = p.count(SquadRole.TANK), supports = p.count(SquadRole.SUPPORT);
        assertTrue(tanks >= 1 && tanks <= 2, what + ": tanks " + tanks);
        assertTrue(supports >= 1 && supports <= 2, what + ": supports " + supports);
        assertTrue(p.count(SquadRole.LEADER) <= 1, what + ": at most one leader");
        assertTrue(p.count(SquadRole.DPS_RANGED) >= 1, what + ": a ranged damage dealer " + p.describe());
        assertTrue(p.count(SquadRole.DPS_MELEE) + p.count(SquadRole.FLANKER) >= 1, what + ": a melee kind " + p.describe());
        if (p.size() - tanks - supports - p.count(SquadRole.LEADER) >= 3) {
            assertTrue(p.count(SquadRole.FLANKER) >= 1 && p.count(SquadRole.DPS_MELEE) >= 1, what + ": room for a flanker and a melee " + p.describe());
        }
    }

    @Test
    void theSameSeedIsTheSameSquadAndSeedsVary() {
        SquadPlan.Plan a = SquadPlan.plan(42L, 25, 3, SquadPlan.HARD, "goblin", "default");
        SquadPlan.Plan b = SquadPlan.plan(42L, 25, 3, SquadPlan.HARD, "goblin", "default");
        assertEquals(a, b);
        Set<String> seen = new HashSet<>();
        for (long s = 0; s < 200; s++) seen.add(SquadPlan.plan(s, 40, 2, SquadPlan.STANDARD, "undead", "necropolis").describe());
        assertTrue(seen.size() >= 3, "seeds give different squads: " + seen);
    }

    @Test
    void roleOrderIsFormationOrder() {
        for (long s = 0; s < 100; s++) {
            List<SquadRole> r = SquadPlan.plan(s, 60, 4, SquadPlan.AMBUSH, "cultist", "").roles();
            int last = -1;
            for (SquadRole role : r) {
                int rank = switch (role) {
                    case LEADER -> 0;
                    case TANK -> 1;
                    case DPS_MELEE -> 2;
                    case FLANKER -> 3;
                    case DPS_RANGED -> 4;
                    case SUPPORT -> 5;
                };
                assertTrue(rank >= last, "front to back: " + r);
                last = rank;
            }
        }
    }

    @Test
    void sizeGrowsWithBandDepthAndPartyButStaysInRange() {
        double lo = 0, hi = 0;
        int n = 400;
        for (long s = 0; s < n; s++) {
            lo += SquadPlan.sizeFor(s, 1, 1, SquadPlan.MINOR);
            hi += SquadPlan.sizeFor(s, 500, 6, SquadPlan.AMBUSH);
        }
        assertTrue(lo / n < 5.3, "shallow solo nuisance stays small: " + lo / n);
        assertTrue(hi / n > 7.5, "deep party ambush is big: " + hi / n);
        double mid = 0, midHard = 0;
        for (long s = 0; s < n; s++) {
            mid += SquadPlan.sizeFor(s, 12, 1, SquadPlan.STANDARD);
            midHard += SquadPlan.sizeFor(s, 12, 1, SquadPlan.HARD);
        }
        assertTrue(midHard > mid, "a harder band is bigger");
    }

    @Test
    void theFactionTiltsTheMix() {
        int ogreTanks = 0, undeadSupports = 0, goblinFlankers = 0, plainFlankers = 0, plainTanks = 0, plainSupports = 0;
        for (long s = 0; s < 300; s++) {
            ogreTanks += SquadPlan.plan(s, 30, 2, SquadPlan.STANDARD, "ogre", "").count(SquadRole.TANK);
            plainTanks += SquadPlan.plan(s, 30, 2, SquadPlan.STANDARD, "goblin", "").count(SquadRole.TANK);
            undeadSupports += SquadPlan.plan(s, 80, 4, SquadPlan.AMBUSH, "cultist", "").count(SquadRole.SUPPORT);
            plainSupports += SquadPlan.plan(s, 80, 4, SquadPlan.AMBUSH, "ogre", "").count(SquadRole.SUPPORT);
            goblinFlankers += SquadPlan.plan(s, 30, 2, SquadPlan.STANDARD, "insect", "").count(SquadRole.FLANKER);
            plainFlankers += SquadPlan.plan(s, 30, 2, SquadPlan.STANDARD, "ogre", "").count(SquadRole.FLANKER);
        }
        assertTrue(ogreTanks > plainTanks, "ogres bring more tanks than goblins");
        assertTrue(undeadSupports > plainSupports, "cultists bring more healers than ogres");
        assertTrue(goblinFlankers > plainFlankers, "insects flank more than ogres");
    }

    @Test
    void aThemeStandsInForAMissingFaction() {
        assertEquals(SquadPlan.flavour("undead", ""), SquadPlan.flavour("", "necropolis"));
        assertEquals(SquadPlan.Flavour.NEUTRAL, SquadPlan.flavour(null, "no_such_theme"));
        assertEquals(SquadPlan.flavour("ogre", "necropolis"), SquadPlan.flavour("ogre", "frost"), "a faction wins over the theme");
    }

    @Test
    void fittingAnEncounterToASquad() {
        assertEquals(List.of(3, 2), asList(SquadPlan.fit(new int[]{3, 2}, 5)));
        int[] grown = SquadPlan.fit(new int[]{2, 1}, 6);
        assertEquals(6, grown[0] + grown[1]);
        assertTrue(grown[0] >= grown[1], "the first kind grows first");
        int[] shrunk = SquadPlan.fit(new int[]{6, 4, 2}, 8);
        assertEquals(8, shrunk[0] + shrunk[1] + shrunk[2]);
        assertTrue(shrunk[0] >= 1, "the first kind is never emptied");
        assertEquals(1, SquadPlan.fit(new int[]{9}, 1)[0]);
        int[] gap = SquadPlan.fit(new int[]{0, 3, 0, 1}, 7);
        assertEquals(0, gap[0]);
        assertEquals(0, gap[2]);
        assertEquals(7, gap[1] + gap[3]);
        int[] empty = SquadPlan.fit(new int[]{0, 0}, 5);
        assertEquals(0, empty[0] + empty[1], "nothing to grow from");
        assertFalse(SquadPlan.plan(1L, 1, 1, 0, null, null).roles().isEmpty());
    }

    @Test
    void aSquadThatCameOutSmallerKeepsItsEssentialRoles() {
        for (long s = 0; s < 150; s++) {
            SquadPlan.Plan p = SquadPlan.plan(s, 40, 3, SquadPlan.HARD, "cultist", "");
            for (int n = 3; n <= p.size(); n++) {
                List<SquadRole> t = SquadPlan.trim(p.roles(), n);
                assertEquals(n, t.size(), "trimmed to " + n + " from " + p.describe());
                if (n >= 4) {
                    assertTrue(t.contains(SquadRole.TANK) && t.contains(SquadRole.SUPPORT), "a tank and a healer survive " + t);
                }
                if (n >= 5) assertTrue(t.contains(SquadRole.DPS_RANGED) && (t.contains(SquadRole.FLANKER) || t.contains(SquadRole.DPS_MELEE)), "damage survives " + t);
            }
            assertEquals(p.roles(), SquadPlan.trim(p.roles(), p.size()));
        }
        assertEquals(1, SquadPlan.trim(List.of(SquadRole.TANK, SquadRole.SUPPORT), 1).size());
    }

    private static List<Integer> asList(int[] a) {
        java.util.ArrayList<Integer> l = new java.util.ArrayList<>();
        for (int x : a) l.add(x);
        return l;
    }
}
