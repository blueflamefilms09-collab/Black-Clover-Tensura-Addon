package com.newuniverse.nusmp.skill.rules;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.skill.rules.FaithGodRules.God;
import com.newuniverse.nusmp.skill.rules.FaithGodRules.Move;
import com.newuniverse.nusmp.skill.rules.FaithGodRules.Ritual;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the eight Faith gods (docs/faith_gods.md) are a table plus rules. This pins the table (eight gods, eight franchises, real names, the ability counts of each class), the strength and ritual maths, and reads the
 * sources and assets as files (no Minecraft class): the ids are registered last, every god has its two icons and its lang keys, nothing client-side is touched, the feature sits behind one SERVER setting.
 */
class FaithGodRulesTest {
    private static String read(String p) throws Exception { return Files.readString(Path.of(p), StandardCharsets.UTF_8); }

    @Test void eightGodsFromEightFranchisesWithRealNames() {
        assertEquals(8, FaithGodRules.GODS.size());
        Set<String> franchises = new HashSet<>(), ids = new HashSet<>(), names = new HashSet<>();
        for (God g : FaithGodRules.GODS) {
            assertTrue(franchises.add(g.franchise()), "one god per franchise: " + g.franchise());
            assertTrue(ids.add(g.uniqueId()) && ids.add(g.ultimateId()), "ids repeat: " + g.id());
            assertTrue(names.add(g.name()) && names.add(g.ultimateName()), "names repeat: " + g.id());
            assertTrue(g.name().startsWith("Faith of "), g.name());
            assertTrue(g.uniqueId().matches("[a-z0-9_]+") && g.ultimateId().matches("[a-z0-9_]+"), "registry id shape: " + g.id());
            assertFalse(g.canon().isBlank() || g.source().isBlank() || g.domain().isBlank());
            assertTrue(g.rgb() >= 0 && g.rgb() <= 0xFFFFFF);
            assertTrue(g.sigil() >= 0 && g.sigil() <= 7, "a SkillAnimeFx sigil style");
            assertTrue(g.offeringCount() >= 1 && g.offeringCount() <= 64 && g.offering().startsWith("minecraft:"));
            assertEquals(FaithGodRules.POOL_ACTIVES, g.moves().size());
            assertEquals(FaithGodRules.POOL_PASSIVES, g.passives().size());
            Set<String> mv = new HashSet<>();
            for (Move m : g.moves()) {
                assertTrue(mv.add(m.id()), g.id() + " repeats move " + m.id());
                assertTrue(m.faith() >= 1 && m.faith() <= 5 && m.mp() > 0 && m.cooldownTicks() >= 100 && m.radius() >= 0 && m.power() >= 0);
                assertFalse(m.text().isBlank());
            }
        }
        for (String banned : new String[]{"Healer", "Warrior", "Harvest", "Hearth", "Trickster", "Forge", "Oracle", "Abyss"})
            for (God g : FaithGodRules.GODS) assertFalse(g.name().contains(banned) || g.ultimateName().contains(banned), "a generic role name: " + g.name());
    }

    @Test void theClassTableFollowsTheNightmaresWikiAndGrows() {
        for (UltimateClass c : UltimateClass.values()) assertNotNull(FaithGodRules.spec(c));
        assertEquals(4, FaithGodRules.spec(UltimateClass.LORD).actives());
        assertEquals(2, FaithGodRules.spec(UltimateClass.LORD).passives());
        assertEquals(3, FaithGodRules.spec(UltimateClass.KING).passives());
        assertEquals(5, FaithGodRules.spec(UltimateClass.GOD).actives());
        assertTrue(FaithGodRules.spec(UltimateClass.GOD).actives() >= 3 && FaithGodRules.spec(UltimateClass.GOD).actives() <= 7, "God: 3 to 7 actives");
        assertTrue(FaithGodRules.spec(UltimateClass.GOD).passives() >= 2 && FaithGodRules.spec(UltimateClass.GOD).passives() <= 3, "God: 2 to 3 passives");
        assertTrue(FaithGodRules.spec(UltimateClass.LORD).epGate() < FaithGodRules.spec(UltimateClass.KING).epGate());
        assertTrue(FaithGodRules.spec(UltimateClass.KING).epGate() < FaithGodRules.spec(UltimateClass.GOD).epGate());
        assertTrue(FaithGodRules.spec(UltimateClass.LORD).power() < FaithGodRules.spec(UltimateClass.KING).power());
        assertTrue(FaithGodRules.spec(UltimateClass.KING).power() < FaithGodRules.spec(UltimateClass.GOD).power());
        for (God g : FaithGodRules.GODS) {
            assertTrue(FaithGodRules.activeCount(g, true) >= FaithGodRules.activeCount(g, false), g.id());
            assertTrue(FaithGodRules.passiveCount(g, true) >= FaithGodRules.passiveCount(g, false), g.id());
            assertEquals(FaithGodRules.activeCount(g, true), FaithGodRules.actives(g, true).size());
            assertEquals(3, FaithGodRules.actives(g, false).size());
            assertEquals(1, FaithGodRules.passives(g, false).size());
        }
        long perClass = FaithGodRules.GODS.stream().map(God::cls).distinct().count();
        assertEquals(4, perClass, "all four classes are used: Lord, King, God and the Dragon track");
    }

    @Test void strengthGrowsWithTheFormAndTheWorshippersButIsCapped() {
        God g = FaithGodRules.god("femto");
        assertSame(g, FaithGodRules.god("faith_of_femto"));
        assertSame(g, FaithGodRules.god("FEMTO_GOD_OF_INCARNATION"));
        assertNull(FaithGodRules.god("nobody"));
        assertNull(FaithGodRules.god(null));
        assertEquals(1.0, FaithGodRules.devotion(0, 4));
        assertEquals(1.0, FaithGodRules.devotion(10, 0));
        assertEquals(1.16, FaithGodRules.devotion(4, 4), 1e-9);
        assertEquals(2.0, FaithGodRules.devotion(1_000_000, 20), 1e-9, "never above x2");
        double unique = FaithGodRules.strength(g, false, 0, 4, 100), ult = FaithGodRules.strength(g, true, 0, 4, 100);
        assertTrue(unique < ult);
        assertTrue(FaithGodRules.strength(g, true, 8, 4, 100) > ult);
        assertEquals(ult * 2, FaithGodRules.strength(g, true, 0, 4, 200), 1e-9);
        assertEquals(ult * 0.1, FaithGodRules.strength(g, true, 0, 4, 1), 1e-9, "the percent is clamped to 10");
        assertEquals(0, FaithGodRules.passiveAmp(FaithGodRules.god("mavis"), false, 0));
        assertEquals(1, FaithGodRules.passiveAmp(FaithGodRules.god("mavis"), false, 8));
        assertEquals(2, FaithGodRules.passiveAmp(g, true, 16), "never above 2");
    }

    @Test void theEvolutionGateIsTheWikisEpTimesThePercent() {
        God lord = FaithGodRules.god("aqua"), king = FaithGodRules.god("luminus"), god = FaithGodRules.god("femto");
        assertEquals(2_000_000.0, FaithGodRules.epGate(lord, 100));
        assertEquals(15_000_000.0, FaithGodRules.epGate(king, 100));
        assertEquals(30_000_000.0, FaithGodRules.epGate(god, 100));
        assertEquals(15_000_000.0, FaithGodRules.epGate(god, 50));
        assertTrue(FaithGodRules.epReady(god, 0, 0), "0 percent = no gate");
        assertFalse(FaithGodRules.epReady(god, 29_999_999, 100));
        assertTrue(FaithGodRules.epReady(god, 30_000_000, 100));
        assertFalse(FaithGodRules.epReady(god, Double.NaN, 100));
    }

    @Test void theRitualNeedsEverythingAndSaysWhyInOrder() {
        assertEquals(3, FaithGodRules.proofs(true, 4, 20, false, 4, 20));
        assertEquals(2, FaithGodRules.proofs(false, 4, 19, true, 4, 20));
        assertEquals(4, FaithGodRules.proofs(true, 9, 99, true, 4, 20));
        assertEquals(Ritual.OK, FaithGodRules.ritual(true, true, false, 0, 1, true, true, true, 3, 40, 40));
        assertEquals(Ritual.DISABLED, FaithGodRules.ritual(false, true, false, 0, 1, true, true, true, 3, 40, 40));
        assertEquals(Ritual.NOT_A_FAITH_HOLDER, FaithGodRules.ritual(true, false, false, 0, 1, true, true, true, 3, 40, 40));
        assertEquals(Ritual.ALREADY, FaithGodRules.ritual(true, true, true, 1, 1, true, true, true, 3, 40, 40));
        assertEquals(Ritual.TOO_MANY_LINES, FaithGodRules.ritual(true, true, false, 1, 1, true, true, true, 3, 40, 40));
        assertEquals(Ritual.OK, FaithGodRules.ritual(true, true, false, 1, 2, true, true, true, 3, 40, 40));
        assertEquals(Ritual.COOLDOWN, FaithGodRules.ritual(true, true, false, 0, 1, false, true, true, 3, 40, 40));
        assertEquals(Ritual.NO_SHRINE, FaithGodRules.ritual(true, true, false, 0, 1, true, false, true, 3, 40, 40));
        assertEquals(Ritual.NO_OFFERING, FaithGodRules.ritual(true, true, false, 0, 1, true, true, false, 3, 40, 40));
        assertEquals(Ritual.FEW_PROOFS, FaithGodRules.ritual(true, true, false, 0, 1, true, true, true, 2, 40, 40));
        assertEquals(Ritual.NO_FAITH, FaithGodRules.ritual(true, true, false, 0, 1, true, true, true, 3, 39, 40));
        assertTrue(FaithGodRules.ritualReady(-1, 5, 6000));
        assertFalse(FaithGodRules.ritualReady(100, 200, 6000));
        assertTrue(FaithGodRules.ritualReady(100, 6100, 6000));
        assertTrue(FaithGodRules.ritualReady(900, 100, 6000), "a clock that went back is ready");
    }

    // ------------------------------------------------------------------ source and asset scans
    private static final String S = "src/main/java/com/newuniverse/nusmp/";

    @Test void theGodsAreRegisteredLastBehindOneServerSetting() throws Exception {
        String reg = read(S + "skill/NUSkills.java").trim();
        assertTrue(reg.endsWith("FAITH_GODS = FaithGods.registerAll(SKILLS);\n}") || reg.contains("FAITH_GODS = FaithGods.registerAll(SKILLS);"), "registered through the table");
        assertTrue(read(S + "skill/SkillEvolution.java").contains("FaithGods.evolve(player, skills);"), "the evolution check is called");
        String cfg = read(S + "NUConfig.java");
        assertTrue(cfg.contains("b.push(\"faithGods\")") && cfg.contains("define(\"enabled\", true)"), "own SERVER section, on by default");
        assertTrue(read(S + "NUSMP.java").contains("FaithGodSelfTests.register()") && read(S + "NUSMP.java").contains("FaithGods::onLogout"), "self-test and the logout cleanup are wired");
        String smoke = read(".github/workflows/smoke.yml");
        assertTrue(smoke.contains(",gods,scaling,boss.rework1,boss.rework3,boss.stats,tier\""), "the smoke list expects gods, tail unchanged");
    }

    @Test void nothingClientSideAndNoVanillaParticleInTheGodFiles() throws Exception {
        for (String f : new String[]{"skill/FaithGods.java", "skill/FaithGodSkill.java", "skill/FaithGodMoves.java", "skill/FaithGodSelfTests.java", "skill/rules/FaithGodRules.java"}) {
            String src = read(S + f);
            assertFalse(src.contains("net.minecraft.client") || src.contains("com.mojang.blaze3d") || src.contains("nusmp.client") || src.contains("Minecraft.getInstance"), f + " is common code");
            assertFalse(src.contains("ParticleTypes.") || src.contains("sendParticles("), f + " uses a vanilla particle");
            assertFalse(src.contains("static ServerPlayer") || src.contains("static Player "), f + " keeps a 'current player'");
        }
        assertFalse(read(S + "skill/rules/FaithGodRules.java").contains("net.minecraft"), "the rules are pure");
        String moves = read(S + "skill/FaithGodMoves.java");
        assertTrue(moves.contains("SkillDamage.hit(") && moves.contains("BalanceLaw.heal("), "damage and healing go through the balance law");
        assertFalse(moves.contains(".hurt(") || moves.contains(".setHealth(") || moves.contains(".heal(") && !moves.contains("BalanceLaw.heal("), "no direct damage or heal");
    }

    @Test void everyGodHasTwoIconsAndLangForItsNames() throws Exception {
        JsonObject lang = JsonParser.parseString(read("tools/lang/faith_gods.json")).getAsJsonObject();
        for (God g : FaithGodRules.GODS) {
            for (String id : new String[]{g.uniqueId(), g.ultimateId()}) {
                File f = new File("src/main/resources/assets/anime_multiverse/textures/skill/" + id + ".png");
                assertTrue(f.isFile(), "icon missing: " + id);
                var img = ImageIO.read(f);
                assertEquals(32, img.getWidth());
                assertEquals(32, img.getHeight());
                assertTrue(lang.has("anime_multiverse.skill." + id) && lang.has("anime_multiverse.skill." + id + ".description"), "lang missing: " + id);
            }
            for (Move m : g.moves()) {
                assertTrue(lang.has("anime_multiverse.skill.mode." + g.uniqueId() + "." + m.id()) && lang.has("anime_multiverse.skill.mode." + g.ultimateId() + "." + m.id()), "mode lang missing: " + g.id() + "." + m.id());
            }
        }
        for (var e : lang.entrySet()) assertFalse(e.getValue().getAsString().isBlank(), e.getKey());
    }
}
