package com.newuniverse.nusmp.sinweapons;

import com.newuniverse.nusmp.vfx.VfxShape;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the ten extra techniques, the five passives and the themed effects of the Berserk weapons (docs/sin_weapons_abilities.md) are pure rules ({@link SinRules.Ability}, {@link SinVfx}) and a few files, so every cap,
 * clock, shape and name is pinned here without the game: each weapon has exactly two extra techniques in slots 1 and 2, bounded in targets, cooldown, damage and packets; the shapes hit what they should; the clocks
 * and shoves are clamped; the effect power round-trips; the skill's modes are appended in order; every new name has its lang text in the fragment and in en_us.json; the old constants did not move; the new code keeps
 * rules 9 and 11 (no client class in common code, no world scan beyond the one box query, damage only through the balance law, one effect packet per picture). Pure: java.nio only, CWD = repo root.
 */
class SinAbilitiesTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Pattern CLIENT = Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|net\\.neoforged\\.neoforge\\.client\\.|Minecraft\\.getInstance\\(");

    private static String code(String s) { return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)(?<!:)//.*$", ""); }

    private static String read(String rel) throws IOException { return Files.readString(SRC.resolve(rel), StandardCharsets.UTF_8); }

    private static String file(String rel) throws IOException { return Files.readString(Path.of(rel), StandardCharsets.UTF_8); }

    @Test
    void everyWeaponHasExactlyTwoBoundedExtraTechniques() {
        assertEquals(10, SinRules.Ability.values().length);
        Set<String> keys = new HashSet<>();
        for (SinRules.Kind k : SinRules.Kind.values()) {
            SinRules.Ability a1 = SinRules.ability(k, 1), a2 = SinRules.ability(k, 2);
            assertNotNull(a1, k + " slot 1");
            assertNotNull(a2, k + " slot 2");
            assertNull(SinRules.ability(k, 0));
            assertNull(SinRules.ability(k, 3));
            assertEquals(SinRules.ABILITIES_PER_KIND, 2);
            assertTrue(a1 != a2 && a1.kind == k && a2.kind == k);
        }
        for (SinRules.Ability a : SinRules.Ability.values()) {
            assertTrue(keys.add(a.key()), "unique key " + a.key());
            assertTrue(a.cap >= 1 && a.cap <= 8, a + " target cap");
            assertTrue(a.cooldown >= 60 && a.cooldown <= 400, a + " cooldown");
            assertTrue(a.damage >= 0f && a.damage <= 20f, a + " damage");
            assertTrue(a.reach > 0.0 && a.reach <= 28.0, a + " reach");
            assertTrue(SinRules.worstPacketsPerAbility(a) <= 8, a + " packets per use");
            assertTrue(a.slot == 1 || a.slot == 2);
            assertEquals(a.key() + ".desc", a.descKey());
            if (a.shape == SinRules.Shape.LINE || a.shape == SinRules.Shape.BOTH_WAYS) assertTrue(a.thick > 0.0, a + " line thickness");
        }
        assertTrue(SinRules.themedPacketsPerUse() <= 6, "one cast, four hit flashes, one puff");
    }

    @Test
    void theShapesHitWhatTheyShould() {
        SinRules.Ability area = SinRules.Ability.SLOTH_STILLNESS, line = SinRules.Ability.ENVY_EYES, both = SinRules.Ability.VANITY_REFLECT, cone = SinRules.Ability.VANITY_NARCISSUS;
        assertTrue(SinRules.inAbility(area, 3, 0, 3, 0.3));
        assertTrue(SinRules.inAbility(area, -3, 0, 3, 0.3), "an area has no front");
        assertFalse(SinRules.inAbility(area, 6, 0, 6, 0.3), "past the radius");
        assertTrue(SinRules.inAbility(line, 15, 1.0, 15.1, 0.3));
        assertFalse(SinRules.inAbility(line, -2, 0, 2, 0.3), "a line has no back");
        assertFalse(SinRules.inAbility(line, 15, 3.0, 15.3, 0.3), "beside the line");
        assertTrue(SinRules.inAbility(both, 5, 0.5, 5.0, 0.3), "ahead");
        assertTrue(SinRules.inAbility(both, -5, 0.5, 5.0, 0.3), "behind: the reflection");
        assertFalse(SinRules.inAbility(both, -9.5, 0, 9.5, 0.3), "past the reflection's tip");
        assertTrue(SinRules.inAbility(cone, 5, 1, 5.1, 0.3), "inside the cone");
        assertFalse(SinRules.inAbility(cone, 2, 6, 6.3, 0.3), "outside the half angle");
        assertFalse(SinRules.inAbility(cone, -4, 0, 4, 0.3), "behind");
        assertFalse(SinRules.inAbility(cone, 9, 0, 9, 0.3), "past the reach");
        assertTrue(SinRules.inCone(0, 0, 0.5), "the wielder's own spot");
    }

    @Test
    void clocksHealingShovesAndPassivesAreClamped() {
        assertTrue(SinRules.clockReady(100, 100, 200));
        assertFalse(SinRules.clockReady(100, 150, 200));
        assertTrue(SinRules.clockReady(100, 1_000_000, 200), "a clock set back does not lock the wielder out");
        assertEquals(50, SinRules.ticksLeft(100, 150, 200));
        assertEquals(0, SinRules.ticksLeft(100, 100, 200));
        assertEquals(0, SinRules.ticksLeft(100, 1_000_000, 200));
        assertEquals(1.5f, SinRules.lifeHeal(1), 1e-6);
        assertEquals(SinRules.LIFE_HEAL_MAX, SinRules.lifeHeal(8), 1e-6, "Stolen Life heals at most 6 in one use");
        assertEquals(0f, SinRules.lifeHeal(-3), 1e-6);
        assertEquals(SinRules.MAX_PUSH, SinRules.push(99), 1e-9);
        assertEquals(0.0, SinRules.push(-4), 1e-9);
        assertEquals(0.0, SinRules.push(Double.NaN), 1e-9);
        assertTrue(SinRules.GRAVITY_DOWN <= SinRules.MAX_PUSH && SinRules.BIND_PULL <= SinRules.MAX_PUSH && SinRules.GROUND_PUSH <= SinRules.MAX_PUSH);
        assertEquals(SinRules.HIT_SLOW_AMP, SinRules.hitSlowAmp(false), "Sinking Weight: a first hit slows as before");
        assertEquals(SinRules.HIT_SLOW_TICKS, SinRules.hitSlowTicks(false));
        assertTrue(SinRules.hitSlowAmp(true) > SinRules.hitSlowAmp(false) && SinRules.hitSlowTicks(true) > SinRules.hitSlowTicks(false), "an already slowed creature is slowed deeper and longer");
        assertTrue(SinRules.passiveGap(SinRules.Kind.VANITY) > 0 && SinRules.passiveGap(SinRules.Kind.CHAKRAMS) > 0, "absorption and healing are rate limited");
        assertTrue(SinRules.STILL_AMP <= 9 && SinRules.STILL_TICKS <= 100 && SinRules.EYES_STEAL_MAX <= 3 && SinRules.CLEANSE_MAX <= 8, "the effect caps");
        assertEquals(10f * 1.5f, SinRules.abilityDamage(SinRules.Ability.HOLY_PURGE, 100, true), 1e-4);
        assertTrue(SinRules.abilityDamage(SinRules.Ability.SLOTH_GRAVITY, 1000, false) <= 400f);
        assertEquals(0f, SinRules.abilityDamage(SinRules.Ability.SLOTH_STILLNESS, 100, false), "Stillness only freezes");
        for (SinRules.Kind k : SinRules.Kind.values()) assertFalse(SinRules.passiveName(k).isBlank());
    }

    @Test
    void theOldConstantsDidNotMove() {
        assertEquals(160, SinRules.Kind.SLOTH.cooldown);
        assertEquals(60, SinRules.Kind.ENVY.cooldown);
        assertEquals(80, SinRules.Kind.VANITY.cooldown);
        assertEquals(70, SinRules.Kind.CHAKRAMS.cooldown);
        assertEquals(100, SinRules.Kind.HOLY.cooldown);
        assertEquals(80, SinRules.WEIGHT_TICKS);
        assertEquals(2, SinRules.WEIGHT_SLOW_AMP);
        assertEquals(40, SinRules.HIT_SLOW_TICKS);
        assertEquals(0, SinRules.HIT_SLOW_AMP);
        assertEquals(200, SinRules.COVET_MAX_TICKS);
        assertEquals(SinRules.Kind.values()[0], SinRules.Kind.SLOTH, "the kinds were not reordered");
        assertEquals(SinRules.Kind.HOLY, SinRules.Kind.values()[4]);
    }

    @Test
    void theEffectPowerRoundTripsAndStaysInRange() {
        for (SinRules.Kind k : SinRules.Kind.values()) {
            for (int style = 0; style < SinVfx.STYLES; style++) {
                float p = SinVfx.power(k, style);
                assertTrue(p >= 1f, "a power of 0 would read as 1");
                assertEquals(k.ordinal(), SinVfx.kindOf(p));
                assertEquals(style, SinVfx.styleOf(p));
            }
        }
        assertEquals(0, SinVfx.kindOf(0f));
        assertEquals(0, SinVfx.kindOf(9999f), "out of range reads as the first weapon");
        assertEquals(0, SinVfx.styleOf(-5f));
        assertEquals(SinVfx.ABILITY_A, SinVfx.styleOfSlot(1));
        assertEquals(SinVfx.ABILITY_B, SinVfx.styleOfSlot(2));
        assertTrue(SinVfx.STEAL < SinVfx.STYLES);
        for (int style = 0; style < SinVfx.STYLES; style++) assertTrue(SinVfx.duration(style) >= 5 && SinVfx.duration(style) <= 40);
        for (SinRules.Ability a : SinRules.Ability.values()) assertEquals(a.reach, SinVfx.reach(a.kind, SinVfx.styleOfSlot(a.slot)), 1e-9, "the picture is as big as the technique");
        assertEquals(SinRules.Kind.SLOTH.reach, SinVfx.reach(SinRules.Kind.SLOTH, SinVfx.TECH), 1e-9);
        SinVfx.setClassic(true);
        assertTrue(SinVfx.classic());
        SinVfx.setClassic(false);
        assertFalse(SinVfx.classic());
    }

    @Test
    void theShapeIsAppendedAfterGiantAvatarAndDrawnByOneRegisteredLayer() throws IOException {
        assertTrue(VfxShape.SIN_WEAPON.ordinal() > VfxShape.GIANT_AVATAR.ordinal(), "appended after the last shape of the earlier work");
        String layer = code(read("vfx/client/layer/SinWeaponLayer.java"));
        assertTrue(layer.contains("EnumSet.of(VfxShape.SIN_WEAPON)"));
        assertTrue(layer.contains("if (SinVfx.classic()) return;"), "the client setting hides it");
        assertFalse(layer.contains("ParticleTypes") || layer.contains("Minecraft.getInstance"), "no vanilla particles and no game singleton in the layer");
        assertFalse(layer.contains("hardVertexCap") || layer.contains("vertexBudget"), "the layer keeps the engine's 400-vertex cap per effect");
        assertTrue(code(read("vfx/client/VfxManager.java")).contains("register(new com.newuniverse.nusmp.vfx.client.layer.SinWeaponLayer())"));
        assertTrue(code(read("NUSMP.java")).contains("SinWeaponVfxConfig.init(modEventBus, modContainer)"), "the client config is registered");
        assertTrue(code(read("vfx/client/SinWeaponVfxConfig.java")).contains("SinVfx.setClassic(CLASSIC.get())") && read("vfx/client/SinWeaponVfxConfig.java").contains("\"nusmp-sinweapons-vfx-client.toml\""));
        String scenes = file("tools/vfx_preview/scenes.d/sin_weapons.json");
        for (SinRules.Kind k : SinRules.Kind.values()) assertTrue(scenes.contains("\"sin_" + k.name().toLowerCase() + "_"), "a preview scene for " + k);
        for (int style = 0; style <= SinVfx.STEAL; style++) {
            String shape = read("vfx/VfxShape.java");
            assertTrue(shape.contains("SIN_WEAPON;") && shape.indexOf("    GIANT_AVATAR,") < shape.indexOf("    SIN_WEAPON;"));
        }
    }

    @Test
    void theSettingsAreServerForRulesAndClientForLooksAndRestoreTheOldBehaviour() throws IOException {
        String cfg = read("sinweapons/SinWeaponConfig.java");
        assertTrue(cfg.contains("define(\"abilities\", true)") && cfg.contains("public static boolean abilities()"));
        assertTrue(cfg.contains("define(\"themedVfx\", true)") && cfg.contains("public static boolean themedVfx()"));
        assertTrue(cfg.contains("define(\"soulBound\", true)") && cfg.contains("define(\"techniques\", true)"), "the old settings are still there");
        String item = code(read("sinweapons/SinWeaponItem.java"));
        assertTrue(item.contains("!SinWeaponConfig.abilities()"), "the extra techniques need the setting");
        assertTrue(item.contains("} else trail(level, eye, look);"), "with themedVfx off the old trail is drawn");
        assertTrue(item.contains("if (!themed) spray(level, at, SinRules.PARTICLES_PER_TARGET, FxTone.DARK);"), "and the old per-target puff");
        assertTrue(item.contains("boolean passive = SinWeaponConfig.abilities() && SinWeaponConfig.techniques() && SinSkillService.mayUse(sp, stack);"), "the passives need the setting and the owner");
        assertTrue(code(read("vfx/client/SinWeaponVfxConfig.java")).contains("ModConfig.Type.CLIENT"), "the look-only setting is a client config");
    }

    @Test
    void theNewCodeIsBoundedAndServerSafe() throws IOException {
        for (String f : new String[]{"sinweapons/SinFx.java", "sinweapons/SinVfx.java", "sinweapons/SinRules.java", "sinweapons/SinWeaponItem.java", "sinweapons/SinSkillService.java", "sinweapons/SinWeaponSelfTests.java"}) {
            String c = code(read(f));
            assertFalse(CLIENT.matcher(c).find(), f + " uses a client class");
            assertFalse(Pattern.compile("static\\s+(?:final\\s+)?(?:Hash|Concurrent|Linked|Identity)?Map<").matcher(c).find(), f + " keeps a static map");
            assertFalse(c.contains("PacketDistributor") || c.contains("CustomPacketPayload"), f + " sends a packet of its own");
        }
        String item = code(read("sinweapons/SinWeaponItem.java"));
        int from = item.indexOf("boolean runAbility(");
        String body = item.substring(from, item.indexOf("private static void abilityDamage("));
        assertEquals(1, body.split("Fx\\.foesAround\\(", -1).length - 1, "one box query per use");
        assertFalse(body.contains("getEntitiesOfClass") || body.contains("getChunk(") || body.contains("getBlockState("), "no other world scan");
        assertFalse(body.contains(".hurt("), "damage only through the balance law");
        assertTrue(body.contains("Math.min(a.cap, 24)"), "the target cap");
        assertTrue(item.contains("Fx.damage(p, t, SinRules.abilityDamage("), "damage goes through relics.Fx.damage");
        assertTrue(item.contains("shown < SinRules.HIT_FX_MAX"), "hit flashes are capped");
        assertTrue(item.contains("SinRules.clockReady(now, ready, a.cooldown)"), "each extra technique has its own clock");
        assertTrue(code(read("sinweapons/SinSkillService.java")).contains("w.ability(p, slot)") && code(read("sinweapons/SinSkillService.java")).contains("if (!mayUse(p, s))"), "the skill asks the owner check first");
        String fx = code(read("sinweapons/SinFx.java"));
        assertEquals(1, fx.split("VfxSpawn\\.send\\(", -1).length - 1, "every picture is one packet through the effect budget");
    }

    @Test
    void theSkillModesAreAppendedInOrderAndEveryNameHasItsText() throws IOException {
        String skill = code(read("skill/SinWeaponSkill.java"));
        assertTrue(skill.contains("{\"technique\", \"manifest\", \"ability_a\", \"ability_b\"}"), "the two old modes first, the two extra techniques appended");
        assertTrue(skill.contains("else SinSkillService.ability(p, kind, m - 1);") && skill.contains("if (m == 0) SinSkillService.technique(p, kind);") && skill.contains("else if (m == 1) SinSkillService.manifest(p, kind);"), "mode order");
        assertTrue(skill.contains("SinRules.ability(kind, m - 1)"), "the mode name is the technique's own name");
        String frag = file("tools/lang/berserk_abilities.json"), en = file("src/main/resources/assets/anime_multiverse/lang/en_us.json");
        for (SinRules.Ability a : SinRules.Ability.values()) {
            for (String key : new String[]{a.key(), a.descKey()}) {
                assertTrue(frag.contains("\"" + key + "\""), "tools/lang/berserk_abilities.json lacks " + key);
                assertTrue(en.contains("\"" + key + "\""), "en_us.json lacks " + key);
            }
            assertTrue(frag.contains("\"" + a.key() + "\": \"" + a.english + "\""), "the lang name is the enum's english name: " + a.english);
        }
        for (SinRules.Kind k : SinRules.Kind.values()) {
            assertTrue(frag.contains("\"" + SinRules.passiveKey(k) + "\": \"" + SinRules.passiveName(k) + "\""), "passive name of " + k);
            assertTrue(en.contains("\"" + SinRules.passiveKey(k) + ".desc\""), "passive description of " + k);
            assertTrue(skill.contains("modes 3 and 4") && skill.contains(SinRules.passiveName(k)), "the skill text of " + k + " names its passive");
        }
        for (String key : new String[]{"sinweapons.ability.wait", "sinweapons.tooltip.abilities", "sinweapons.tooltip.passive", "sinskill.msg.abilities_off"}) {
            assertTrue(frag.contains("\"" + key + "\"") && en.contains("\"" + key + "\""), key);
        }
    }
}
