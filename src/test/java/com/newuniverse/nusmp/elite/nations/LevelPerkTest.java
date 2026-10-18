package com.newuniverse.nusmp.elite.nations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.elite.nations.core.GuardRules;
import com.newuniverse.nusmp.elite.nations.core.NationLevels;
import com.newuniverse.nusmp.elite.nations.core.Perk;
import com.newuniverse.nusmp.elite.nations.core.PerkTable;
import com.newuniverse.nusmp.elite.nations.rep.ReputationBook;
import com.newuniverse.nusmp.elite.nations.rep.ReputationTier;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Why: level maths, perks, guard scaling and reputation tiers are numbers the whole system hangs on. */
class LevelPerkTest {
    @Test
    void scoreWeightsAndTreasuryCap() {
        var w = NationLevels.Weights.defaults();
        var m = new NationLevels.Metrics(3, 5_000, 2, 10, 1, 100);
        // 3*20 + 5 + 2*10 + 10*1 + 1*50 + 100 = 245
        assertEquals(245.0, NationLevels.score(w, m), 1e-9);
        var rich = new NationLevels.Metrics(0, 1_000_000_000L, 0, 0, 0, 0);
        assertEquals(w.treasuryPointCap(), NationLevels.score(w, rich), 1e-9, "treasury points are capped");
        assertEquals(0.0, NationLevels.score(w, new NationLevels.Metrics(0, -5, 0, -3, 0, -9)), 1e-9);
        assertEquals(60.0, NationLevels.score(new NationLevels.Weights(20, 0, 300, 10, 1, 50), new NationLevels.Metrics(3, 99999, 0, 0, 0, 0)), 1e-9, "coinsPerPoint 0 disables the treasury part");
    }

    @Test
    void thresholdsAreStrictlyIncreasingAndLevelOfCountsThem() {
        long[] t = NationLevels.thresholds(10, 100, 1.5);
        assertEquals(9, t.length);
        assertEquals(100L, t[0]);
        for (int i = 1; i < t.length; i++) assertTrue(t[i] > t[i - 1]);
        assertEquals(1, NationLevels.levelOf(0, t));
        assertEquals(1, NationLevels.levelOf(99.9, t));
        assertEquals(2, NationLevels.levelOf(100, t));
        assertEquals(10, NationLevels.levelOf(1e9, t));
        assertEquals(1, NationLevels.levelOf(5, new long[0]));
        long[] tiny = NationLevels.thresholds(5, 0.0, 0.0);
        for (int i = 1; i < tiny.length; i++) assertTrue(tiny[i] > tiny[i - 1], "degenerate config still increases");
    }

    @Test
    void progressAndMissing() {
        long[] t = {100, 300};
        assertEquals(0.0, NationLevels.progress(0, t), 1e-9);
        assertEquals(0.5, NationLevels.progress(50, t), 1e-9);
        assertEquals(0.5, NationLevels.progress(200, t), 1e-9);
        assertEquals(1.0, NationLevels.progress(900, t), 1e-9);
        assertEquals(100L, NationLevels.missingForNext(200, t));
        assertEquals(-1L, NationLevels.missingForNext(300, t));
    }

    @Test
    void perksAreCumulativeAndSummed() {
        PerkTable p = new PerkTable().add(2, new Perk(Perk.TREASURY_CAP, "", 100)).add(3, new Perk(Perk.TREASURY_CAP, "", 50)).add(3, new Perk(Perk.MEMBER_CAP, "", 4));
        assertEquals(0.0, p.sum(Perk.TREASURY_CAP, 1), 1e-9);
        assertEquals(100.0, p.sum(Perk.TREASURY_CAP, 2), 1e-9);
        assertEquals(150.0, p.sum(Perk.TREASURY_CAP, 9), 1e-9);
        assertEquals(1, p.at(2).size());
        assertEquals(3, p.perkCount());
        assertEquals(3, p.maxLevelWithPerks());
        p.add(1, new Perk(Perk.MEMBER_CAP, "", 1));
        assertEquals(3, p.perkCount(), "level 1 has no perks");
        assertEquals(1, p.ofType(Perk.MEMBER_CAP, 3).size());
    }

    @Test
    void perksParseAndSkipBadEntries() {
        JsonObject o = JsonParser.parseString("{\"levels\":[{\"level\":2,\"perks\":[{\"type\":\"treasury_cap\",\"value\":10},{\"type\":\"fly\",\"value\":1},{\"type\":\"attribute\",\"value\":1},"
                + "{\"type\":\"attribute\",\"arg\":\"minecraft:generic.armor\",\"value\":2},{\"value\":3}]},{\"level\":1,\"perks\":[]},{\"level\":4}]}").getAsJsonObject();
        PerkTable p = PerkTable.parse(o);
        assertEquals(2, p.perkCount());
        assertEquals(2.0, p.sum(Perk.ATTRIBUTE, 2), 1e-9);
        assertFalse(p.warnings().isEmpty());
        assertTrue(PerkTable.parse(new JsonObject()).warnings().size() == 1);
        assertTrue(PerkTable.defaults().perkCount() > 10);
    }

    @Test
    void guardScaling() {
        assertEquals(1, GuardRules.tierOf(1));
        assertEquals(2, GuardRules.tierOf(3));
        assertEquals(5, GuardRules.tierOf(9));
        assertEquals(5, GuardRules.tierOf(99));
        assertEquals(2, GuardRules.desired(1, 2, 0.5, 0, 10));
        assertEquals(4, GuardRules.desired(5, 2, 0.5, 0, 10));
        assertEquals(10, GuardRules.desired(50, 2, 0.5, 3, 10), "hard cap");
        assertEquals(0, GuardRules.desired(1, 0, 0, 0, 10));
        assertEquals(2, GuardRules.toSpawn(5, 3, 3, 0, 100));
        assertEquals(3, GuardRules.toSpawn(10, 3, 3, 0, 100));
        assertEquals(0, GuardRules.toSpawn(3, 5, 3, 0, 100));
        assertEquals(1, GuardRules.toSpawn(10, 0, 3, 99, 100), "global cap");
        assertEquals(1.0, GuardRules.healthFactor(1), 1e-9);
        assertEquals(2.0, GuardRules.healthFactor(5), 1e-9);
        assertEquals(4.0, GuardRules.damageBonus(5), 1e-9);
        assertTrue(GuardRules.inZone(10, 64, 0, 0, 64, 0, 10, 8));
        assertFalse(GuardRules.inZone(11, 64, 0, 0, 64, 0, 10, 8));
        assertFalse(GuardRules.inZone(0, 80, 0, 0, 64, 0, 10, 8));
    }

    @Test
    void reputationTiers() {
        assertEquals(ReputationTier.HATED, ReputationTier.of(-1000));
        assertEquals(ReputationTier.HOSTILE, ReputationTier.of(-750));
        assertEquals(ReputationTier.UNFRIENDLY, ReputationTier.of(-300));
        assertEquals(ReputationTier.NEUTRAL, ReputationTier.of(0));
        assertEquals(ReputationTier.FRIENDLY, ReputationTier.of(100));
        assertEquals(ReputationTier.HONORED, ReputationTier.of(350));
        assertEquals(ReputationTier.REVERED, ReputationTier.of(1000));
        assertEquals(-1, ReputationTier.REVERED.nextMin());
        assertEquals(-300, ReputationTier.HOSTILE.nextMin());
        assertTrue(ReputationTier.REVERED.shopPercent < ReputationTier.NEUTRAL.shopPercent);
        assertTrue(ReputationTier.HATED.guardsHostile);
        assertFalse(ReputationTier.NEUTRAL.guardsHostile);
        assertFalse(ReputationTier.HOSTILE.mayUseBoard);
        assertEquals(ReputationTier.NEUTRAL, ReputationTier.parse("nonsense"));
        assertEquals(1000, ReputationTier.clamp(99999));
        assertEquals(5, ReputationTier.decay(10, 5));
        assertEquals(0, ReputationTier.decay(3, 5));
        assertEquals(-5, ReputationTier.decay(-10, 5));
        assertEquals(0, ReputationTier.decay(-3, 5));
    }

    @Test
    void reputationBook() {
        ReputationBook b = new ReputationBook();
        UUID p = UUID.nameUUIDFromBytes("p".getBytes());
        assertEquals(0, b.get(p, "n"));
        assertEquals(40, b.add(p, "n", 40));
        assertEquals(1000, b.add(p, "n", 5000), "clamped");
        assertEquals(ReputationTier.REVERED, b.tier(p, "n"));
        assertEquals(0, b.set(p, "n", 0));
        assertTrue(b.of(p).isEmpty(), "zero values are dropped");
        assertEquals(0, b.add(null, "n", 5));
        b.add(p, "a", 10);
        b.add(p, "b", -10);
        assertEquals(2, b.decayAll(4));
        assertEquals(6, b.get(p, "a"));
        assertEquals(-6, b.get(p, "b"));
        b.removeNation("a");
        assertEquals(1, b.of(p).size());
        b.decayAll(100);
        assertTrue(b.raw().isEmpty());
    }
}
