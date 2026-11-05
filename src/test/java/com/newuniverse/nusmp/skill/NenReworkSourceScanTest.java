package com.newuniverse.nusmp.skill;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the Nen rework (3.57.0, docs/nen_rework.md) adds NenHatsu (the signature moves) and NenFx (every effect send). They run on a public server, so the rules of CLAUDE.md are pinned in the
 * source: no client classes, no vanilla particles, no static state, bounded creature queries, damage through the balance law, control time through BalanceLaw.controlTicks, entities held as UUIDs.
 */
class NenReworkSourceScanTest {
    private static String code(String rel) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (String line : Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/" + rel), StandardCharsets.UTF_8).split("\n")) {
            String t = line.strip();
            if (t.startsWith("*") || t.startsWith("/*") || t.startsWith("//")) continue;
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private static final String[] COMMON = {"skill/NenHatsu.java", "skill/NenFx.java", "skill/NenSkill.java", "skill/rules/NenRules.java"};

    @Test
    void theCommonSideNeverTouchesClientClassesOrVanillaParticles() throws Exception {
        for (String f : COMMON) {
            String c = code(f);
            assertFalse(c.contains("net.minecraft.client") || c.contains("com.mojang.blaze3d") || c.contains("com.newuniverse.nusmp.client") || c.contains("Minecraft.getInstance"), f + " reaches client code");
            assertFalse(c.contains("ParticleTypes.") || c.contains("sendParticles("), f + " spawns vanilla particles");
        }
    }

    @Test
    void noMutableStaticStateAndNoHeldPlayersOrEntities() throws Exception {
        Pattern mutableStatic = Pattern.compile("^\\s*(?:(?:private|public|protected)\\s+)?static\\s+(?!final\\b)(?!class\\b)[^(=;]+(=|;)", Pattern.MULTILINE);
        for (String f : COMMON) {
            String c = code(f);
            assertFalse(mutableStatic.matcher(c).find(), f + " keeps mutable static state");
            assertFalse(c.contains("Map<UUID") || c.contains("static List<") || c.contains("WeakHashMap") || c.contains("HashMap<ServerPlayer"), f + " holds players in a collection");
        }
    }

    @Test
    void everyQueryIsBoundedAndEveryHitGoesThroughTheBalanceLaw() throws Exception {
        String c = code("skill/NenHatsu.java");
        assertFalse(c.contains("getEntitiesOfClass") || c.contains("getEntities(") || c.contains("EntityTypeTest"), "creatures are only found through the bounded SkillTargets helpers");
        assertTrue(c.contains("SkillTargets.foes(") && c.contains("SkillTargets.foesInCone("));
        assertTrue(c.contains("SkillDamage.hit("), "damage goes through the balance law");
        assertFalse(c.contains(".hurt("), "no raw damage that skips the balance law");
        assertTrue(c.contains("BalanceLaw.controlTicks("), "chains, needle and gum hold only as long as the balance law allows");
        assertTrue(c.contains("getEntity(") && c.contains("UUID"), "constructs hold creatures as UUIDs, looked up each time");
        assertFalse(c.contains("new Thread") || c.contains("Executor") || c.contains("schedule("), "everything runs from the owner's tick");
    }

    @Test
    void theHatsuTickIsGatedAndCleanedUp() throws Exception {
        String h = code("skill/NenHatsu.java");
        assertTrue(h.contains("NenBusy"), "an idle player costs one tag read (the busy key)");
        assertTrue(h.contains("static void clearAll("), "constructs are cleared");
        String s = code("skill/NenSkill.java");
        assertTrue(s.contains("NenHatsu.tick(") && s.contains("NenHatsu.clearAll("), "the skill ticks the constructs and clears them on death");
        assertTrue(s.contains("tickCount % AURA_EVERY") && s.contains("AURA_EVERY = 40"), "the aura refresh stays gated");
        assertTrue(s.contains("NenFx.MAX") || s.contains("MAX_AURAS_SENT"), "the number of auras sent per refresh is capped");
    }

    @Test
    void everyEffectGoesThroughNenFxAndItsSwitches() throws Exception {
        String f = code("skill/NenFx.java");
        assertTrue(f.contains("NUConfig.NEN_VFX.get()") && f.contains("NUConfig.NEN_VFX_V2.get()"), "both looks keep their switch");
        assertTrue(f.contains("VfxSpawn.") || f.contains("SkillFx."), "effects are sent to nearby players only, through the existing senders");
        for (String old : new String[]{"NEN_AURA", "NEN_EN", "NEN_GYO", "NEN_IMPACT", "NEN_BEAM"}) assertTrue(f.contains(old), "the old shape " + old + " is still reachable with nenVfxV2=false");
        String cfg = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/NUConfig.java"), StandardCharsets.UTF_8);
        assertTrue(cfg.contains("\"nenVfxV2\""), "the setting exists");
    }

    @Test
    void theRemadeLayerIsRegisteredAndBounded() throws Exception {
        String m = code("vfx/client/VfxManager.java");
        assertTrue(m.contains("register(new NenArtLayer())") && m.contains("register(new NenLayer())"), "the old layer is kept next to the new one");
        String l = code("vfx/client/layer/NenArtLayer.java");
        assertTrue(l.contains("ctx.seg("), "every repeated part has a level of detail");
    }
}
