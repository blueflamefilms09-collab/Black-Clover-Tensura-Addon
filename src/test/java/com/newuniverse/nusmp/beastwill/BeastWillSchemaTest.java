package com.newuniverse.nusmp.beastwill;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why (3.45.0): the four Beast Wills are data files, and a data file that the validator rejects would silently leave a will out of the game. This reads the real files from the repo and pins what the
 * owner asked for: the four wills exist, each passes the validator, only A rank or better gives one, the legendary dragon is rarer and slower than the rest, every will has both phases with abilities
 * that stay inside the bounds, and a broken file is rejected with a reason instead of being half read.
 */
class BeastWillSchemaTest {
    private static final Path DIR = Path.of("src/main/resources/data/nusmp/beast_will");
    private static final List<String> IDS = List.of("dragon_will", "shadow_panther_will", "elderwood_guardian_will", "lion_will", "thunder_fang_chimera_will");   // 3.51.0: the Thunder-Fang Chimera will of the AA beast core

    static BeastWillDef load(String id) throws Exception {
        BeastWillSchema.Parsed p = BeastWillSchema.parse(id, Files.readString(DIR.resolve(id + ".json")));
        assertTrue(p.ok(), id + ": " + p.problems());
        return p.def();
    }

    @Test
    void theWillsExistAndEveryFileInTheFolderIsOneOfThem() throws Exception {
        List<String> found = new ArrayList<>();
        for (File f : DIR.toFile().listFiles((d, n) -> n.endsWith(".json"))) found.add(f.getName().replace(".json", ""));
        for (String id : IDS) assertTrue(found.contains(id), id + " is missing");
        assertTrue(found.containsAll(IDS) && IDS.containsAll(found), "an unexpected will file: " + found);
    }

    @Test
    void everyWillPassesTheValidatorAndOnlyAnARankBeastOrBetterGivesIt() throws Exception {
        for (String id : IDS) {
            BeastWillDef d = load(id);
            assertNull(d.problem());
            assertTrue(d.minRank().atLeast(BeastRank.A), id);
            assertEquals(id, d.id());
        }
    }

    @Test
    void theDragonIsLegendaryAndRarerAndSlowerThanTheRare() throws Exception {
        BeastWillDef dragon = load("dragon_will");
        assertTrue(dragon.legendary());
        assertEquals(BeastRank.S, dragon.minRank());
        for (String id : IDS) {
            if (id.equals("dragon_will")) continue;
            BeastWillDef o = load(id);
            assertFalse(o.legendary());
            assertTrue(dragon.dropChance() < o.dropChance(), "the dragon core is rarer than " + id);
            assertTrue(dragon.assimilationSeconds() > o.assimilationSeconds(), "the dragon takes longer than " + id);
            assertTrue(dragon.assimilationDayCap() < o.assimilationDayCap(), "the dragon is capped harder per day than " + id);
            assertTrue(dragon.coreCooldownDays() > o.coreCooldownDays());
        }
    }

    @Test
    void bothPhasesHaveAbilitiesAndIntegrateNeedsFullAssimilationAndRealMastery() throws Exception {
        for (String id : IDS) {
            BeastWillDef d = load(id);
            assertFalse(d.acquire().abilities().isEmpty(), id);
            assertFalse(d.integrate().abilities().isEmpty(), id);
            assertTrue(d.acquire().unlockAssimilation() <= 0.5, id + ": acquire unlocks after the first assimilation");
            assertTrue(d.integrate().unlockAssimilation() >= 0.9 && d.integrate().unlockMastery() >= 0.3, id);
            assertTrue(d.integrate().drainPct() > d.acquire().drainPct(), id + ": integrate drains more");
            assertTrue(d.integrate().strainPerSecond() > d.acquire().strainPerSecond(), id + ": integrate strains more");
            assertTrue(d.integrate().stats().size() >= d.acquire().stats().size() - 1, id);
        }
    }

    @Test
    void theNamedAbilitiesOfTheOwnersBriefAreThere() throws Exception {
        assertNotNull(load("dragon_will").ability(BeastWillRules.INTEGRATE, 2));
        assertEquals("time_exclusion", load("dragon_will").ability(BeastWillRules.INTEGRATE, 2).kind());
        assertEquals("shadow_dash", load("shadow_panther_will").ability(BeastWillRules.INTEGRATE, 1).kind());
        assertEquals("ambush", load("shadow_panther_will").ability(BeastWillRules.INTEGRATE, 2).kind());
        assertEquals("vine_lash", load("elderwood_guardian_will").ability(BeastWillRules.ACQUIRE, 1).kind());
        assertEquals("restraining_vines", load("elderwood_guardian_will").ability(BeastWillRules.INTEGRATE, 1).kind());
        assertEquals("bark_ward", load("elderwood_guardian_will").ability(BeastWillRules.INTEGRATE, 2).kind());
        assertEquals("world_howl", load("lion_will").ability(BeastWillRules.INTEGRATE, 1).kind());
    }

    @Test
    void theTimeExclusionIsShortSmallFewAndSlowToComeBackAndNeverHoldsABoss() throws Exception {
        BeastWillDef.Ability te = load("dragon_will").ability(BeastWillRules.INTEGRATE, 2);
        assertTrue(te.durationTicks() <= 100, "at most five seconds");
        assertTrue(te.radius() <= 10 && te.maxTargets() <= 6);
        assertTrue(te.cooldownTicks() >= 600, "at least thirty seconds apart");
        assertEquals(0.0, te.bossFactor(), "a boss is immune");
    }

    @Test
    void theHardControlsKeepBossResistanceAndWindUpSoCounterplayExists() throws Exception {
        assertTrue(load("elderwood_guardian_will").ability(BeastWillRules.INTEGRATE, 1).bossFactor() <= 0.25);
        assertTrue(load("lion_will").ability(BeastWillRules.INTEGRATE, 1).bossFactor() <= 0.25);
        assertTrue(load("lion_will").ability(BeastWillRules.INTEGRATE, 1).durationTicks() <= 60, "a stagger of at most three seconds");
    }

    @Test
    void aWillBeyondTheBoundsIsRejectedWithAReasonAndNeverHalfRead() throws Exception {
        String ok = Files.readString(DIR.resolve("lion_will.json"));
        String[][] breaks = {
                {"\"minRank\": \"A\"", "\"minRank\": \"B\""},
                {"\"dropChance\": 0.02", "\"dropChance\": 0.5"},
                {"\"kind\": \"world_howl\"", "\"kind\": \"nuke\""},
                {"\"attribute\": \"minecraft:generic.attack_damage\",\n        \"op\": \"add\",\n        \"amount\": 4.0", "\"attribute\": \"minecraft:generic.attack_damage\",\n        \"op\": \"add\",\n        \"amount\": 90.0"},
                {"\"bossFactor\": 0.25", "\"bossFactor\": 7"},
                {"\"id\": \"lion_will\"", "\"id\": \"other_will\""},
        };
        int rejected = 0;
        for (String[] b : breaks) {
            if (!ok.contains(b[0])) continue;                                   // a formatting change must not turn this into a silent pass: counted below
            BeastWillSchema.Parsed p = BeastWillSchema.parse("lion_will", ok.replace(b[0], b[1]));
            assertFalse(p.ok(), b[1]);
            assertNull(p.def(), "a rejected file gives no will at all");
            assertFalse(p.problems().isEmpty());
            rejected++;
        }
        assertTrue(rejected >= 4, "the break patterns must still match the files (matched " + rejected + ")");
        assertFalse(BeastWillSchema.parse("x", "not json at all").ok());
        assertFalse(BeastWillSchema.parse("x", "[1,2]").ok());
    }

    @Test
    void creaturesMatchBySourceIdOrTheTensuraKeywordOnly() throws Exception {
        BeastWillDef dragon = load("dragon_will");
        assertTrue(dragon.fromEntity("iceandfire:fire_dragon"));
        assertTrue(dragon.fromEntity("tensura:storm_dragon"), "a Tensura creature with the keyword in its id");
        assertFalse(dragon.fromEntity("minecraft:ender_dragon"), "the keyword only counts on the tensura namespace");
        assertFalse(dragon.fromEntity("somemod:dragon_statue"), "another mod's id with the keyword does not count unless it is listed");
        assertFalse(dragon.fromEntity(null));
    }
}
