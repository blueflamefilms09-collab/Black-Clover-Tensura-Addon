package com.newuniverse.nusmp.skill;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the Ultimate classes (docs/ultimate_classes.md) route the NUSkill family's costs through Tensura and nothing here can be played, so the wiring is pinned by reading the source: no NUSkill
 * pays with SkillUtil.spendMagicules any more (only the shared helper's flatCosts fallback and the two legacy grimoire skills outside the family), the helper charges once through
 * getMagiculeCost + EnergyHelper.isOutOfEnergy, the classification matches the doc, the settings are in the SERVER config, the pure class names no game class, and the self-test is expected.
 */
class UltimateClassSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String code(Path p) throws IOException {
        return Files.readString(p, StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)(?<!:)//.*$", "");
    }

    private static String skill(String f) throws IOException { return code(SRC.resolve("skill/" + f + ".java")); }

    private static int count(String s, String needle) {
        int n = 0;
        for (int i = s.indexOf(needle); i >= 0; i = s.indexOf(needle, i + 1)) n++;
        return n;
    }

    @Test
    void theNuSkillFamilyPaysThroughTheHelper() throws IOException {
        List<String> converted = List.of("TheAlmightySkill", "TheVisionarySkill", "AllFictionSkill", "GateOfBabylonSkill", "KiSkill", "NenSkill", "NenHatsu", "AdollaBurstSkill",
                "DevilsFootprintsSkill", "ImmortalWillSkill", "FireboltSkill", "AirielSkill", "ArgonautSkill", "GodSkill", "DbSkill", "BeastSkill", "ToggleAuraSkill", "SummonerSkill", "FaithSkill",
                "BookMakerSkill", "ChantedMagicSkill");
        int sites = 0;
        for (String f : converted) {
            String c = skill(f);
            assertFalse(c.contains("spendMagicules("), f + " still writes magicules directly");
            int n = count(c, "pay(") + count(c, "charge(instance") + count(c, "charge(inst");
            assertTrue(n > 0, f + " does not pay through NUSkill.pay / charge");
            sites += count(c, "pay(instance, ") + count(c, "pay(inst, ") + count(c, "charge(instance, ");
        }
        assertTrue(sites >= 58, "converted call sites: " + sites);
        // only the shared fallback and the two legacy grimoire skills (they extend Tensura's Skill, not NUSkill) still spend directly
        try (var w = Files.walk(SRC)) {
            for (Path f : w.filter(p -> p.toString().endsWith(".java")).toList()) {
                String name = f.getFileName().toString();
                if (name.equals("SkillUtil.java") || name.equals("GrimoirePageSkill.java") || name.equals("GrimoireMagicSkill.java") || name.equals("LegacyPay.java")) continue;   // guide P2: LegacyPay is their flatCosts fallback
                int n = count(code(f), "SkillUtil.spendMagicules(");
                if (name.equals("NUSkill.java")) assertEquals(1, n, "NUSkill: only the flatCosts fallback");
                else assertEquals(0, n, name + " spends magicules directly");
            }
        }
    }

    @Test
    void theHelperChargesOnceThroughTensura() throws IOException {
        String n = skill("NUSkill");
        assertEquals(1, count(n, "EnergyHelper.isOutOfEnergy(player, instance, mode)"), "one Tensura charge");
        assertTrue(n.contains("public double getMagiculeCost(LivingEntity entity, ManasSkillInstance instance, int mode)"), "the price is what getMagiculeCost answers");
        assertTrue(n.contains("PAYING.remove()") && n.contains("finally"), "the pending price never outlives the call");
        assertTrue(n.contains("NUConfig.ucFlatCosts()"), "the old flat cost is one setting away");
        assertTrue(n.contains("BalanceLaw.classCost(player, classOf(instance), flat)"), "the size comes from the balance law");
        assertTrue(n.contains("public int getMaxMastery()") && n.contains("NUConfig.ucMaxMastery(c)"), "the class sets the mastery");
        assertTrue(n.contains("Tier.of(mastered, ultimate || c != null, c != null && NUConfig.ucMasteredTier())"), "the old boolean path is kept for the unclassified");
        assertTrue(n.contains("UltimateClass.milestone(masteryFrac(instance))") && n.contains("TAG_MILESTONE"), "one shared milestone, announced once");
        assertFalse(n.contains("net.minecraft.client") || n.contains("Minecraft.getInstance"), "server safe");
        assertTrue(code(SRC.resolve("NUSMP.java")).contains("NUSkill::onLogin") && code(SRC.resolve("NUSMP.java")).contains("UltimateClassSelfTests.register()"));
    }

    @Test
    void theClassificationMatchesTheDoc() throws IOException {
        String[][] want = {{"TheAlmightySkill", "GOD"}, {"TheVisionarySkill", "GOD"}, {"AllFictionSkill", "GOD"}, {"GateOfBabylonSkill", "KING"}, {"HellConquerorSkill", "KING"},
                {"LordOfSummonsSkill", "KING"}, {"GoddessOfFaithSkill", "KING"}, {"AdollaBurstSkill", "LORD"}, {"BlackWingsSkill", "LORD"}, {"HerosArgonautSkill", "LORD"},
                {"ImmortalWillSkill", "LORD"}, {"KiSkill", "LORD"}, {"NenSkill", "LORD"}, {"ShishouUltimateSkill", "LORD"}, {"LordOfGluttonousGreedSkill", "LORD"}};
        String doc = Files.readString(Path.of("docs/ultimate_classes.md"), StandardCharsets.UTF_8);
        for (String[] w : want) {
            assertTrue(skill(w[0]).contains("ultimateClass() { return UltimateClass." + w[1] + "; }"), w[0] + " is not " + w[1]);
            assertTrue(doc.contains("`" + w[0] + "`"), w[0] + " missing from docs/ultimate_classes.md");
        }
        for (String f : new String[]{"DbSkill", "BeastSkill"}) assertTrue(skill(f).contains("ult ? com.newuniverse.nusmp.skill.rules.UltimateClass.LORD : null"), f);
    }

    @Test
    void settingsAreServerSettingsAndTheClassIsPure() throws IOException {
        String cfg = code(SRC.resolve("NUConfig.java"));
        assertTrue(cfg.contains("b.push(\"ultimateClasses\")"), "own section");
        for (String k : new String[]{"\"flatCosts\", false", "\"classMastery\", true", "\"masteredTier\", true", "\"milestones\", true", "\"classDamage\", true", "\"lateGameShare\", 3.0",
                "\"plainCostPercent\", 1.0", "\"epCap\"", "\"costPercent\"", "\"costReference\"", "\"maxMastery\"", "\"killBelow\"", "\"shredBelow\""}) assertTrue(cfg.contains(k), k);
        assertTrue(code(SRC.resolve("NUSMP.java")).contains("registerConfig(ModConfig.Type.SERVER, NUConfig.SPEC)"), "NUConfig is the SERVER config");
        String pure = code(SRC.resolve("skill/rules/UltimateClass.java"));
        assertFalse(pure.contains("net.minecraft") || pure.contains("net.neoforged"), "the class math is pure");
        String law = code(SRC.resolve("balance/BalanceLaw.java"));
        assertTrue(law.contains("public static double epScale(LivingEntity caster, UltimateClass cls)") && law.contains("if (cls == null) return epScale(caster);"), "the old epScale stays for the unclassified");
        assertTrue(law.contains("UltimateClass.totalScale(") && law.contains("NUConfig.ucLateShare()"), "the combined cap is applied");
    }

    @Test
    void theSelfTestIsExpected() throws IOException {
        assertTrue(skill("UltimateClassSelfTests").contains("\"ultclass.rules\"") && skill("UltimateClassSelfTests").contains("\"ultclass.skills\""));
        String smoke = Files.readString(Path.of(".github/workflows/smoke.yml"), StandardCharsets.UTF_8);
        assertTrue(smoke.contains(",ultclass,"), "ultclass right after greed");
        assertTrue(smoke.contains(",scaling,") && smoke.contains(",attune,") && smoke.contains(",scaling,boss.rework1,boss.stats,tier\""), "the pinned entries stay");
    }
}
