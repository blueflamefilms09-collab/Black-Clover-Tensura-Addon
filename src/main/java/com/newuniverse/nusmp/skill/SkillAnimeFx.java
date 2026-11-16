package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the one place the Unique / Ultimate skills send their anime-style effects (the {@code SKILL_*} shapes drawn by {@code SkillAnimeLayer}, VFX wave 3, docs/vfx_remake/skill_anime.md). Each call is ONE
 * effect packet through {@link VfxSpawn} (the per-tick effect budget, players within 96 blocks only), made once per cast next to the skill's old {@link SkillFx} calls, which stay (they are what the client
 * setting {@code skills.classicVfx} shows). Never per target, never per tick: the only repeating senders are the Argonaut chime (one per charge step, at most the charge cap), the Black Wings glide (one
 * every 40 ticks while gliding) and the Hell Conqueror hellfire (one per hellfire pulse, a second apart). Never throws: an effect is cosmetic and must not break a cast.
 * The power numbers below are read back by {@code vfx.client.layer.SkillAnimeVfx} (this class may not name it: it is client code); SkillAnimeVfxAssetTest pins the two against each other.
 */
public final class SkillAnimeFx {
    private SkillAnimeFx() {}

    /** A power carries a mode in steps of this much (SkillAnimeVfx.STEP). */
    static final float STEP = 20f;
    static final int GATE_RAIN = 1, GATE_CHAINS = 2, GATE_EA = 3;
    static final int ARGO_STRIKE = 1, ARGO_BEAM = 2;
    static final int FIRE_FIELD = 1;
    /** SKILL_SIGIL's styles, in the order of SkillAnimeVfx.SIGIL_STYLES (power = style + 1). */
    static final int FAITH = 0, GODDESS = 1, FORGE = 2, MALLET = 3, WILL = 4, HELL = 5, GRAVITY = 6, FIRE_FORCE = 7;

    static final int GOLD = 0xFFFFD060;
    static final int EA_RED = 0xFFFF4020;
    static final int LIGHT = 0xFFFFF2B0;
    static final int FICTION = 0xFF5A2A7A;
    /** No effect lasts longer than this (ticks), whatever the skill's zone lasts. */
    static final int MAX_TICKS = 200;

    /** mode * STEP + rest, the rest kept inside its step. */
    static float mode(int mode, double rest) { return mode * STEP + (float) Math.max(0.0, Math.min(STEP - 0.5, rest)); }

    static int ticks(int t) { return Math.max(1, Math.min(MAX_TICKS, t)); }

    static void send(ServerLevel level, VfxShape shape, Vec3 from, Vec3 to, int argb, int duration, float power) {
        if (level == null || shape == null || from == null || to == null) return;
        try {
            VfxSpawn.send(level, shape, from, to, argb, ticks(duration), power);
        } catch (RuntimeException | LinkageError ignored) {
            // cosmetic only: a closed connection must not break the cast
        }
    }

    static void follow(ServerLevel level, VfxShape shape, Entity who, Vec3 to, int argb, int duration, float power) {
        if (level == null || shape == null || who == null || to == null) return;
        try {
            VfxSpawn.sendFollowing(level, shape, who, to, argb, ticks(duration), power);
        } catch (RuntimeException | LinkageError ignored) {
            // cosmetic only
        }
    }

    // ---------------------------------------------------------------- Gate of Babylon
    /** {@code count} gates open behind {@code from} and fire their weapons at {@code to}. */
    static void gate(ServerLevel level, Vec3 from, Vec3 to, int count) { send(level, VfxShape.SKILL_GATE, from, to, GOLD, 30, mode(0, Math.max(1, Math.min(8, count)))); }

    static void gateRain(ServerLevel level, Vec3 sky, Vec3 ground, double radius) { send(level, VfxShape.SKILL_GATE, sky, ground, GOLD, 34, mode(GATE_RAIN, radius)); }

    static void enkidu(ServerLevel level, Vec3 from, Vec3 target) { send(level, VfxShape.SKILL_GATE, from, target, GOLD, 30, mode(GATE_CHAINS, 0)); }

    static void ea(ServerLevel level, Vec3 from, Vec3 to) { send(level, VfxShape.SKILL_GATE, from, to, EA_RED, 24, mode(GATE_EA, 0)); }

    // ---------------------------------------------------------------- Firebolt (and Adolla's Light Speed)
    static void firebolt(ServerLevel level, Vec3 from, Vec3 to, int argb, float width) { send(level, VfxShape.SKILL_FIREBOLT, from, to, argb, 12, width); }

    // ---------------------------------------------------------------- Argonaut
    /** One chime of the charge on the caster (follows). */
    static void chime(ServerLevel level, Entity who, int charge) { follow(level, VfxShape.SKILL_ARGONAUT, who, who.position().add(0, 1, 0), LIGHT, 18, mode(0, Math.max(1, charge))); }

    static void argoStrike(ServerLevel level, Vec3 from, Vec3 to, int charge) { send(level, VfxShape.SKILL_ARGONAUT, from, to, LIGHT, 16, mode(ARGO_STRIKE, Math.max(1, charge))); }

    static void heroBeam(ServerLevel level, Vec3 from, Vec3 to, int charge) { send(level, VfxShape.SKILL_ARGONAUT, from, to, LIGHT, 18, mode(ARGO_BEAM, Math.max(1, charge))); }

    // ---------------------------------------------------------------- the chants and the gods
    static void frost(ServerLevel level, Vec3 centre, double radius, int ticks) { send(level, VfxShape.SKILL_FROST, centre, centre.add(0, 1, 0), 0xFFCFF3FF, ticks, (float) Math.min(14.0, radius)); }

    static void hearth(ServerLevel level, Vec3 at, double radius, int argb) { send(level, VfxShape.SKILL_HOLY_FIRE, at, at.add(0, 1, 0), argb, 30, mode(0, radius)); }

    /** Rea Laevateinn's field on the caster (follows, like the waves that are centred on the caster each time). */
    static void flameField(ServerLevel level, Entity who, double radius, int ticks, int argb) {
        follow(level, VfxShape.SKILL_HOLY_FIRE, who, who.position().add(0, 1, 0), argb, ticks, mode(FIRE_FIELD, radius));
    }

    static void allure(ServerLevel level, Vec3 at, double radius, int argb) { send(level, VfxShape.SKILL_ALLURE, at, at.add(0, 1, 0), argb, 30, (float) Math.min(16.0, radius)); }

    // ---------------------------------------------------------------- All Fiction, Black Wings
    static void screwShot(ServerLevel level, Vec3 from, Vec3 to) { send(level, VfxShape.SKILL_SCREW, from, to, FICTION, 30, 1f); }

    static void screwRing(ServerLevel level, Vec3 feet, int count) { send(level, VfxShape.SKILL_SCREW, feet, feet, FICTION, 30, Math.max(3, Math.min(5, count))); }

    /** The wings on the gliding wearer (follows); sent again every 40 ticks while the glide lasts, each one 42 ticks long, so they cross-fade. */
    static void wings(ServerLevel level, Entity who, float span) { follow(level, VfxShape.SKILL_WINGS, who, who.position().add(0, 1, 0), 0xFF6A6070, 42, span); }

    // ---------------------------------------------------------------- the smaller skills
    /** A SKILL_SIGIL style at {@code at}; the radius rides on the from -> to distance. */
    static void sigil(ServerLevel level, int style, Vec3 at, double radius, int argb, int duration) {
        if (at == null) return;
        send(level, VfxShape.SKILL_SIGIL, at, at.add(Math.max(0.8, Math.min(24.0, radius)), 0, 0), argb, duration, style + 1);
    }

    // ------------------------------------------------------------------ CI server smoke test (prefix skill.vfx in SMOKE_EXPECT)
    private static boolean registered;

    public static void registerSelfTests() {
        if (registered) return;
        registered = true;
        SelfTests.register("skill.vfx", SkillAnimeFx::selfTest);
    }

    /** Every SKILL_ shape goes out through the effect budget on a dedicated server without throwing (to nobody: no player is near the spawn in CI), the shapes are the nine expected, after DEVIL_HIT. */
    private static SelfTests.Result selfTest(MinecraftServer server) {
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        int sent = 0;
        for (VfxShape s : VfxShape.values()) {
            if (!s.name().startsWith("SKILL_")) continue;
            send(level, s, at, at.add(0, 0, 6), GOLD, 10, 1f);
            sent++;
        }
        if (sent != 9) return SelfTests.Result.fail("expected the 9 SKILL_ shapes, found " + sent);
        if (VfxShape.SKILL_GATE.ordinal() != VfxShape.DEVIL_HIT.ordinal() + 1) return SelfTests.Result.fail("the skill shapes moved (they must follow DEVIL_HIT)");
        if (mode(GATE_RAIN, 6) != 26f || mode(ARGO_BEAM, 99) >= 3 * STEP) return SelfTests.Result.fail("the power encoding drifted");
        sigil(level, FIRE_FORCE, at, 4, GOLD, 10);
        gateRain(level, at.add(0, 7, 0), at, 6);
        return SelfTests.Result.ok("the 9 skill shapes sent through VfxSpawn, appended after DEVIL_HIT; the mode encoding holds");
    }
}
