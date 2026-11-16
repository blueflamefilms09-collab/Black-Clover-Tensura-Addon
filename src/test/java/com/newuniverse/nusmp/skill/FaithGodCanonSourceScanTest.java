package com.newuniverse.nusmp.skill;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.skill.rules.FaithGodRules;
import com.newuniverse.nusmp.skill.rules.FaithGodRules.God;
import com.newuniverse.nusmp.skill.rules.FaithGodRules.Move;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the Faith gods canon pass (docs/faith_gods.md "Canon pass") must stay server-only, bounded, switchable, append-only and wired into the registry, the self-test, the lang fragment and the doc. The checks read
 * the sources as files (no Minecraft class is loaded).
 */
class FaithGodCanonSourceScanTest {
    private static final Path MAIN = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String read(String rel) throws IOException { return Files.readString(MAIN.resolve(rel), StandardCharsets.UTF_8); }

    private static String readRoot(String rel) throws IOException { return Files.readString(Path.of(rel), StandardCharsets.UTF_8); }

    @Test
    void theSettingIsServerSideOnByDefaultAndGuarded() throws IOException {
        String cfg = read("NUConfig.java");
        assertTrue(cfg.contains("FG_CANON") && cfg.contains(".define(\"canon\", true)"), "faithGods.canon exists and is on by default");
        assertTrue(cfg.contains("public static boolean faithGodCanon() { return live(FG_CANON); }"), "read through the guarded accessor");
        int sec = cfg.indexOf("b.push(\"faithGods\")"), at = cfg.indexOf(".define(\"canon\", true)");
        assertTrue(sec > 0 && at > sec, "inside the faithGods section");
    }

    @Test
    void theWiringReadsTheCanonRowAtRunTimeAndKeepsTheOldRowAsTheFallback() throws IOException {
        String skill = read("skill/FaithGodSkill.java");
        assertTrue(skill.contains("FaithGodRules.pick(god, NUConfig.faithGodCanon())"), "the skill picks its table at run time");
        assertTrue(skill.contains("FaithGodRules.unlocked(") && skill.contains("FaithGodRules.enoughWorshippers(") && skill.contains("FaithGodRules.masteryGain("), "unlocks, worshipper minimums and mastery gain are enforced on the server");
        assertTrue(skill.contains("FaithGodRules.activeCount(god, ult)"), "the mode count comes from the registered row, so it never changes");
        String gods = read("skill/FaithGods.java");
        assertTrue(gods.contains("FaithGodRules.pick(FaithGodRules.god(godId), NUConfig.faithGodCanon())") && gods.contains("FaithGodRules.ritualWorshippersOk("), "the ritual reads the canon row and its worshipper minimum");
        assertTrue(gods.contains("new FaithGodSkill(g, false)") && gods.contains("new FaithGodSkill(g, true)"), "registration still reads the 4.6.0 table, so ids and saves are stable");
        String moves = read("skill/FaithGodMoves.java");
        for (String v : new String[]{"case WELL", "case PRESS", "case HOLY", "case LAW", "case LIFE_BEAM"}) assertTrue(moves.contains(v), v);
        assertTrue(moves.contains("FaithGodRules.tollApplies("), "the Super Saiyan God toll is applied");
        assertFalse(moves.contains(".setHealth("), "the old moves file never touches health");
    }

    @Test
    void theNewVerbsAreBoundedAndServerSafe() throws IOException {
        String f = read("skill/FaithGodCanonMoves.java");
        assertFalse(f.contains("net.minecraft.client") || f.contains("com.mojang.blaze3d") || f.contains("nusmp.client") || f.contains("Minecraft.getInstance"), "common code");
        assertFalse(f.contains("ParticleTypes.") || f.contains("sendParticles(") || f.contains("static ServerPlayer") || f.contains("static Player "), "no vanilla particle, no current player");
        assertFalse(f.contains("static final java.util.Map") || f.contains("new java.util.HashMap") || f.contains("new HashMap") || f.contains("ConcurrentHashMap"), "no state is kept");
        assertFalse(f.contains(".kill(") || f.contains("discard(") || f.contains(".hurt(") || f.contains("addFreshEntity(") || f.contains("getEntitiesOfClass("), "no direct damage, no spawns, no own world scan: targets come from SkillTargets");
        assertTrue(f.contains("SkillTargets.foes(") && f.contains("SkillDamage.hit("), "targets are capped by SkillTargets, damage goes through the balance law");
        assertTrue(f.contains("FaithGodRules.hpPaid(") && f.contains("p.isCreative()"), "the health price is bounded and free in creative");
        assertTrue(f.contains("pay(p, paid);"), "health is taken after the move worked");
        String rules = read("skill/rules/FaithGodRules.java");
        assertFalse(rules.contains("net.minecraft"), "the rules stay Minecraft-free");
        assertTrue(rules.contains("DOMAIN,") && rules.contains("WELL, PRESS, HOLY, LAW, LIFE_BEAM"), "the new verbs are appended after the old ones");
    }

    @Test
    void theSelfTestIsRegisteredAndTheSmokeListAlreadyRunsTheGodsPrefix() throws IOException {
        String t = read("skill/FaithGodSelfTests.java");
        assertTrue(t.contains("SelfTests.register(\"gods.canon\""), "gods.canon is registered");
        assertTrue(t.contains("SelfTests.register(\"gods.rules\"") && t.contains("SelfTests.register(\"gods.skills\""), "the old self-tests stay");
        assertTrue(readRoot(".github/workflows/smoke.yml").contains(",gods,"), "the smoke list expects the gods prefix (order-independent)");
    }

    @Test
    void theLangFragmentNamesEveryCanonMoveForBothFormsAndTheNewMessages() throws IOException {
        JsonObject lang = JsonParser.parseString(readRoot("tools/lang/faith_gods_canon.json")).getAsJsonObject();
        for (God g : FaithGodRules.CANON_GODS) {
            for (String id : new String[]{g.uniqueId(), g.ultimateId()}) {
                assertTrue(lang.has("anime_multiverse.skill." + id + ".description"), "lang description missing: " + id);
                if (id.equals(g.ultimateId())) assertTrue(lang.has("anime_multiverse.skill." + id), "the renamed god form needs its name: " + id);      // the Unique keeps its 4.6.0 name
                for (Move m : g.moves()) assertTrue(lang.has("anime_multiverse.skill.mode." + id + "." + m.id()), "mode lang missing: " + id + "." + m.id());
            }
        }
        for (String k : new String[]{"gods.locked", "gods.need_worshippers", "gods.need_ritual_worshippers"}) assertTrue(lang.has("anime_multiverse.skill.msg." + k), k);
        for (var e : lang.entrySet()) {
            String v = e.getValue().getAsString();
            assertFalse(v.isBlank(), e.getKey());
            assertFalse(v.matches(".*%[^s%].*"), "a stray format code in " + e.getKey());
        }
    }

    @Test
    void theDocHasTheCanonPassForEveryGod() throws IOException {
        String doc = readRoot("docs/faith_gods.md");
        assertTrue(doc.contains("Canon pass") && doc.contains("faithGods.canon"), "the doc has the section and names the setting");
        int at = doc.indexOf("Canon pass");
        String tail = doc.substring(at);
        for (God g : FaithGodRules.CANON_GODS) assertTrue(tail.contains(g.id()) || tail.contains(g.ultimateName()), "the canon pass covers " + g.id());
        assertTrue(tail.contains("NEW") && tail.contains("REPLACED") && tail.contains("KEPT"), "NEW / REPLACED / KEPT");
        assertTrue(tail.contains("Worst case"), "the worst-case cost is stated");
    }
}
