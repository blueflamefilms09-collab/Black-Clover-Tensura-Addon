package com.newuniverse.nusmp.labyrinth.mega.eco;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: these are the turf-war rules in the exact words of the design, one test each: rivals are noticed at the faction's range and farther inside
 * its own room; the same faction and allies are never noticed; a faction that does not exist is left alone; constructs (territorial) ignore
 * everyone who is no rival unless the intruder stands inside their room; a one-way rivalry is one-way; and a file with no aggro numbers uses the
 * server config's, kept inside the hard ceilings.
 */
class TurfRulesTest {
    private static final FactionDef GOBLIN = Fx.withRange("goblin", List.of("ogre"), 14, 8);
    private static final FactionDef OGRE = Fx.withRange("ogre", List.of(), 12, 10);
    private static final FactionDef SLIME = Fx.withRange("slime", List.of(), 6, 4);
    private static final FactionDef CONSTRUCT = Fx.territorial("construct", List.of("elemental"), 8, 12);
    private static final FactionDef ELEMENTAL = Fx.withRange("elemental", List.of(), 12, 6);
    private static final FactionDef CULTIST = Fx.def("cultist", List.of(), List.of("demon"));
    private static final FactionDef DEMON = Fx.def("demon", List.of(), List.of());
    private static final FactionGraph G = FactionGraph.of(List.of(GOBLIN, OGRE, SLIME, CONSTRUCT, ELEMENTAL, CULTIST, DEMON));

    @Test void rivalsAreNoticedAtRangeAndFartherInsideTheOwnRoom() {
        assertEquals(TurfRules.Relation.RIVAL, TurfRules.relation(G, GOBLIN, "ogre", false));
        assertEquals(14.0, TurfRules.reach(G, GOBLIN, "ogre", false, 14, 8));
        assertEquals(22.0, TurfRules.reach(G, GOBLIN, "ogre", true, 14, 8), "range 14 plus the native bonus 8");
    }

    @Test void rivalryIsMirroredSoTheOgreNoticesTheGoblinToo() {
        assertEquals(TurfRules.Relation.RIVAL, TurfRules.relation(G, OGRE, "goblin", false));
        assertEquals(12.0, TurfRules.reach(G, OGRE, "goblin", false, 12, 10));
        assertEquals(22.0, TurfRules.reach(G, OGRE, "goblin", true, 12, 10));
    }

    @Test void theSameFactionAndAlliesAreNeverNoticed() {
        assertEquals(0.0, TurfRules.reach(G, GOBLIN, "goblin", true, 14, 8));
        assertEquals(0.0, TurfRules.reach(G, CULTIST, "demon", true, 14, 8));
        assertEquals(0.0, TurfRules.reach(G, DEMON, "cultist", true, 14, 8));
        assertEquals(TurfRules.Relation.NONE, TurfRules.relation(G, GOBLIN, "goblin", false));
    }

    @Test void nobodyIsNoticedWhoBelongsToNoFaction() {
        assertEquals(0.0, TurfRules.reach(G, GOBLIN, null, true, 14, 8));
        assertEquals(TurfRules.Relation.NONE, TurfRules.relation(G, null, "ogre", false));
    }

    @Test void slimesTolerateEveryone() {
        for (String other : List.of("goblin", "ogre", "construct", "elemental", "cultist", "demon")) {
            assertEquals(0.0, TurfRules.reach(G, SLIME, other, true, 6, 4), "slime vs " + other + " even in its own pool");
            assertEquals(0.0, TurfRules.reach(G, SLIME, other, false, 6, 4));
        }
        assertEquals(0.0, TurfRules.reach(G, GOBLIN, "slime", false, 14, 8), "and nobody hunts a slime");
    }

    @Test void constructsDefendTheirOwnRoomAgainstAnyIntruder() {
        assertEquals(0.0, TurfRules.reach(G, CONSTRUCT, "goblin", false, 8, 12), "a goblin outside the room is ignored");
        assertEquals(TurfRules.Relation.INTRUDER, TurfRules.relation(G, CONSTRUCT, "goblin", true));
        assertEquals(20.0, TurfRules.reach(G, CONSTRUCT, "goblin", true, 8, 12), "an intruder inside the room is met at range + bonus");
        assertEquals(0.0, TurfRules.reach(G, CONSTRUCT, "construct", true, 8, 12), "never its own kind");
    }

    @Test void aConstructStillHuntsItsRealRivalsEverywhere() {
        assertEquals(TurfRules.Relation.RIVAL, TurfRules.relation(G, CONSTRUCT, "elemental", false));
        assertEquals(8.0, TurfRules.reach(G, CONSTRUCT, "elemental", false, 8, 12));
        assertEquals(TurfRules.Relation.RIVAL, TurfRules.relation(G, ELEMENTAL, "construct", false), "mirrored");
    }

    @Test void aTerritorialFactionSparesItsAlliesInItsOwnRoom() {
        FactionDef warden = new FactionDef("warden", "warden", 0xFFFFFF, "nusmp:mega/warden", List.of(), List.of("cultist"), 8, 12, true, false, java.util.Map.of(), "raw_armor_flank", 0.25);
        FactionGraph g = FactionGraph.of(List.of(warden, CULTIST, GOBLIN));
        assertEquals(TurfRules.Relation.NONE, TurfRules.relation(g, warden, "cultist", true), "an ally walking through is no intruder");
        assertEquals(0.0, TurfRules.reach(g, warden, "cultist", true, 8, 12));
        assertEquals(TurfRules.Relation.INTRUDER, TurfRules.relation(g, warden, "goblin", true), "anyone else is");
    }

    @Test void aNonTerritorialFactionIgnoresIntrudersWhoAreNoRivals() {
        assertEquals(TurfRules.Relation.NONE, TurfRules.relation(G, GOBLIN, "slime", true));
        assertEquals(TurfRules.Relation.NONE, TurfRules.relation(G, GOBLIN, "cultist", true));
    }

    @Test void aOneWayRivalryIsOneWay() {
        FactionDef beast = Fx.oneWay("beast", List.of("slime"));
        FactionGraph g = FactionGraph.of(List.of(beast, SLIME));
        assertEquals(TurfRules.Relation.RIVAL, TurfRules.relation(g, beast, "slime", false));
        assertEquals(TurfRules.Relation.NONE, TurfRules.relation(g, SLIME, "beast", false));
    }

    @Test void aFileWithoutNumbersUsesTheConfigAndEveryoneIsClamped() {
        FactionDef plain = Fx.def("plain", List.of(), List.of());
        assertEquals(14, TurfRules.range(plain, 14));
        assertEquals(8, TurfRules.nativeBonus(plain, 8));
        assertEquals(6, TurfRules.range(SLIME, 14), "the file wins over the config");
        assertEquals(4, TurfRules.nativeBonus(SLIME, 8));
        assertEquals(FactionDef.MAX_RANGE, TurfRules.range(plain, 500));
        assertEquals(FactionDef.MAX_BONUS, TurfRules.nativeBonus(plain, 500));
        assertEquals(0, TurfRules.range(plain, -3));
        assertEquals(0, TurfRules.nativeBonus(plain, -3));
    }

    @Test void theGiveUpDistanceGrowsWithTheReach() {
        assertTrue(TurfRules.giveUpDistance(14, 8) > 22.0, "a chase may run past the notice range");
        assertTrue(TurfRules.giveUpDistance(14, 8) > TurfRules.giveUpDistance(6, 4));
        assertEquals(4.0, TurfRules.giveUpDistance(0, 0));
    }
}
