package com.newuniverse.nusmp.skill;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: Gate of Babylon and Ki (3.47.0) run on a public server, so the rules of CLAUDE.md are pinned in the source: no client classes, no vanilla particles (the effects go through SkillFx), no
 * static per-player state (everything lives in the skill tag of the one player), no tick, every press bounded through the SkillTargets helpers, the hooks honour the server settings, and the
 * skills are registered after the older ones.
 */
class UltimatePair2SourceScanTest {
    private static final String[] SKILLS = {"GateOfBabylonSkill.java", "KiSkill.java"};

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
        for (String f : SKILLS) {
            String c = code(f);
            assertFalse(c.contains("net.minecraft.client") || c.contains("com.mojang.blaze3d") || c.contains("Minecraft.getInstance"), f + " reaches client code");
            assertFalse(c.contains("ParticleTypes.") || c.contains("sendParticles("), f + " spawns vanilla particles, go through SkillFx");
        }
    }

    @Test
    void noMutableStaticStateHoldsAPlayer() throws Exception {
        Pattern mutableStatic = Pattern.compile("^\\s*(?:(?:private|public|protected)\\s+)?static\\s+(?!final\\b)(?!class\\b)[^(=;]+(=|;)", Pattern.MULTILINE);
        for (String f : SKILLS) {
            assertFalse(mutableStatic.matcher(code(f)).find(), f + " keeps mutable static state");
            assertFalse(code(f).contains("Map<UUID") || code(f).contains("static List<ServerPlayer"), f + " holds players in a static collection");
        }
    }

    @Test
    void noTickNoUnboundedScanNoEntitySpawn() throws Exception {
        for (String f : SKILLS) {
            String c = code(f);
            assertFalse(c.contains("onTick(") || c.contains("canTick("), f + " has no tick: its windows are game times in the tag");
            assertFalse(c.contains("getEntitiesOfClass("), f + " finds creatures only through the bounded SkillTargets helpers");
            assertFalse(c.contains("addFreshEntity(") || c.contains("EntityType.") || c.contains("level.explode(") || c.contains("setBlock("), f + " spawns entities or edits blocks");
            assertFalse(c.contains("Level.OVERWORLD") || c.contains("changeDimension("), f + " leaves the dimension");
            assertTrue(c.contains("SkillDamage.hit("), f + " damage goes through the balance law");
            assertFalse(c.contains(".hurt("), f + " must not call hurt directly");
        }
    }

    @Test
    void eachSkillHonoursItsSwitchInThePressAndTheHook() throws Exception {
        String g = code("GateOfBabylonSkill.java"), k = code("KiSkill.java");
        assertEquals(2, g.split("NUConfig.GATE_ENABLED", -1).length - 1, "Gate reads gateEnabled in the press and in the hook");
        assertEquals(2, k.split("NUConfig.KI_ENABLED", -1).length - 1, "Ki reads kiEnabled in the press and in the hook");
    }

    @Test
    void thePerPressCapsAreUsed() throws Exception {
        String g = code("GateOfBabylonSkill.java");
        assertTrue(g.contains("GateRules.volleyMax()") && g.contains("GateRules.rainMax()") && g.contains("GateRules.gaePierce()"), "Gate uses its caps");
        assertTrue(g.contains("FX_GATES") && g.contains("FX_BEAMS"), "Gate bounds the gates and beams it draws");
        String k = code("KiSkill.java");
        assertTrue(k.contains("KiRules.kamePierce()"), "Kamehameha pierces a bounded number");
        assertTrue(k.contains("KiRules.spiritDamage(lenders)"), "Spirit Bomb is capped through the pure rule");
        assertTrue(k.contains("ControlRules.controlTicks("), "Solar Flare shortens on bosses and players");
        assertTrue(g.contains("ControlRules.controlTicks("), "Chains shorten on bosses and players");
    }

    @Test
    void theTeleportRefundsWhenItCannotMoveYou() throws Exception {
        String k = code("KiSkill.java");
        assertTrue(k.contains("SkillUtil.refund(p, cost)") && k.contains("KiRules.teleportOk("), "a blink that fails or is too short costs nothing");
    }

    @Test
    void theNewSettingsAreServerConfigsAndTheSkillsAreRegisteredAfterNen() throws Exception {
        String cfg = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/NUConfig.java"), StandardCharsets.UTF_8);
        assertTrue(cfg.contains("GATE_ENABLED") && cfg.contains("KI_ENABLED") && cfg.contains("ultimates_3_47"), "both settings exist in their own section");
        assertTrue(cfg.contains("\"gateEnabled\"") && cfg.contains("\"kiEnabled\""), "setting names");
        String reg = read("NUSkills.java");
        int nen = reg.indexOf("SKILLS.register(\"nen\", NenSkill::new)");
        int gate = reg.indexOf("SKILLS.register(\"gate_of_babylon\", GateOfBabylonSkill::new)");
        int ki = reg.indexOf("SKILLS.register(\"ki\", KiSkill::new)");
        assertTrue(nen > 0 && gate > nen && ki > gate, "registered in order after Nen (append only)");
    }
}
