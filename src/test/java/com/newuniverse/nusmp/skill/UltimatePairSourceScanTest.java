package com.newuniverse.nusmp.skill;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: Nen and the rebuilt All Fiction (3.46.0) run on a public server, so the rules of CLAUDE.md are pinned in the source: no client classes, no vanilla particles (the effects go through
 * SkillFx), no static per-player state (everything lives in the skill tag of the one player), the tick is gated to once a second, every press is bounded, the new hooks honour the server settings,
 * and the old modes, ids and the death passive are still there.
 */
class UltimatePairSourceScanTest {
    private static String read(String file) throws Exception {
        return Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/skill/" + file), StandardCharsets.UTF_8);
    }

    private static String code(String file) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (String line : read(file).split("\n")) {
            String t = line.strip();
            if (t.startsWith("*") || t.startsWith("/*") || t.startsWith("//")) continue;
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    @Test
    void neitherSkillTouchesClientClassesOrVanillaParticles() throws Exception {
        for (String f : new String[]{"NenSkill.java", "AllFictionSkill.java", "UltimatePairSelfTests.java"}) {
            String c = code(f);
            assertFalse(c.contains("net.minecraft.client") || c.contains("com.mojang.blaze3d") || c.contains("Minecraft.getInstance"), f + " reaches client code");
            assertFalse(c.contains("ParticleTypes.") || c.contains("sendParticles("), f + " spawns vanilla particles, go through SkillFx");
        }
    }

    @Test
    void noMutableStaticStateHoldsAPlayer() throws Exception {
        // a field is a line with 'static' and no '(' before its '=' or ';' (a method has its '(' first); only 'static final' (constants) is allowed
        Pattern mutableStatic = Pattern.compile("^\\s*(?:(?:private|public|protected)\\s+)?static\\s+(?!final\\b)(?!class\\b)[^(=;]+(=|;)", Pattern.MULTILINE);
        for (String f : new String[]{"NenSkill.java", "AllFictionSkill.java"}) {
            var m = mutableStatic.matcher(code(f));
            assertFalse(m.find(), f + " keeps mutable static state");
            assertFalse(code(f).contains("Map<UUID") || code(f).contains("static List<ServerPlayer"), f + " holds players in a static collection");
        }
    }

    @Test
    void theFictionTickIsGatedToOnceASecondAndTheZoneToItsWindow() throws Exception {
        String c = code("AllFictionSkill.java");
        assertTrue(c.contains("living.tickCount % 20 != 0"), "the tick must be gated to once a second");
        assertTrue(c.contains("FictionRules.worldPulseDue("), "the zone pulses through the pure rule");
        assertTrue(c.contains("FictionRules.worldPulseMaxProjectiles()") && c.contains("FictionRules.unmakeMax()") && c.contains("FictionRules.shareMax()"), "the per-press caps are used");
        assertTrue(c.contains("rebuilt()"), "the new hooks honour the allFictionRebuilt setting");
    }

    @Test
    void theOldFictionModesAndThePassiveAreStillThere() throws Exception {
        String c = code("AllFictionSkill.java");
        assertTrue(c.contains("\"fiction.undo\"") && c.contains("\"fiction.erase\"") && c.contains("\"fiction.cooldown\"") && c.contains("\"fiction.death\""), "old lang keys kept");
        for (String key : new String[]{"H\" + i", "DeathReadyAt", "oldestIndex()", "undoHealth(", "erases(", "deathCooldown()"}) assertTrue(c.contains(key), "old state or rule dropped: " + key);
        assertTrue(c.contains("public boolean onDeath("), "the death passive is kept");
    }

    @Test
    void nenHonoursItsSwitchAndKeepsItsPressesBounded() throws Exception {
        String c = code("NenSkill.java");
        assertTrue(c.contains("NUConfig.NEN_ENABLED"), "Nen reads its server setting");
        assertTrue(c.contains("SkillTargets."), "Nen finds creatures through the bounded SkillTargets helpers");
        assertTrue(c.split("getEntitiesOfClass\\(", -1).length - 1 <= 1, "at most one box query of its own (Gyo's, a fixed 24-block radius, at most 16 revealed)");
        assertTrue(c.contains("SkillDamage.hit("), "Nen damage goes through the balance law");
        // 3.48.5: the only tick is the aura refresh, gated to once every AURA_EVERY (40) ticks and by the nenVfx switch; the windows themselves are still game times in the tag
        assertTrue(!c.contains("onTick(") || (c.contains("tickCount % AURA_EVERY") && c.contains("AURA_EVERY = 40") && c.contains("newFx()")), "Nen's tick must be the gated aura refresh only");
    }

    @Test
    void theNewSettingsAreServerConfigsAndTheSkillIsRegisteredLast() throws Exception {
        String cfg = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/NUConfig.java"), StandardCharsets.UTF_8);
        assertTrue(cfg.contains("NEN_ENABLED") && cfg.contains("ALL_FICTION_REBUILT"), "both settings exist");
        assertTrue(cfg.contains("ultimates_3_46"), "the settings sit in their own section");
        String reg = read("NUSkills.java");
        assertTrue(reg.contains("SKILLS.register(\"nen\", NenSkill::new)"), "Nen is registered");
        assertTrue(reg.contains("SKILLS.register(\"all_fiction\", AllFictionSkill::new)"), "All Fiction keeps its id");
    }
}
