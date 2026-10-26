package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: every sight and sound of the Judgment skills goes out from one place, so the cost is easy to read: one effect packet per ability use (the Verdict field: one for its whole life, it is
 * drawn by the client for as long as it lasts), always through {@link VfxSpawn}, which sends to nearby players only and goes through the effect-packet budget. The shapes are drawn by the client's
 * {@code JudgmentLayer}. Server thread only.
 */
public final class JudgmentFx {
    private JudgmentFx() {}

    /** Pale gold, the colour every Judgment effect is tinted with (the layer adds white and a hairline of black). */
    public static final int GOLD = 0xFFF2DFA0;

    private static void sound(ServerLevel l, Vec3 at, SoundEvent s, float volume, float pitch) {
        l.playSound(null, at.x, at.y, at.z, s, SoundSource.PLAYERS, volume, pitch);
    }

    public static void bullet(ServerLevel l, Vec3 from, Vec3 to) {
        VfxSpawn.send(l, VfxShape.JUDGMENT_BULLET, from, to, GOLD, 12, 1.0f);
        sound(l, from, SoundEvents.WARDEN_SONIC_BOOM, 0.8f, 1.9f);
    }

    public static void sigil(ServerLevel l, LivingEntity target) {
        Vec3 at = target.position().add(0.0, 0.05, 0.0);
        VfxSpawn.sendFollowing(l, VfxShape.JUDGMENT_SIGIL, target, at.add(0.0, 1.0, 0.0), GOLD, 600, (float) Math.max(1.0, target.getBbWidth() * 1.4));
        sound(l, at, SoundEvents.BELL_RESONATE, 1.0f, 0.7f);
    }

    public static void edict(ServerLevel l, Vec3 center) {
        VfxSpawn.send(l, VfxShape.JUDGMENT_EDICT, center, center.add(0.0, 1.0, 0.0), GOLD, 24, (float) JudgmentRules.EDICT_RADIUS);
        sound(l, center, SoundEvents.BEACON_DEACTIVATE, 1.6f, 0.5f);
    }

    public static void cleave(ServerLevel l, Vec3 from, Vec3 to) {
        VfxSpawn.send(l, VfxShape.JUDGMENT_CLEAVE, from, to, GOLD, 10, (float) JudgmentRules.CLEAVE_REACH);
        sound(l, from, SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f, 0.55f);
    }

    /** The Verdict field: drawn once and followed by the client for its whole life. {@code follow} may be null (a field fixed in the ground). */
    public static void field(ServerLevel l, LivingEntity follow, Vec3 center, double radius, int ticks) {
        if (follow != null) VfxSpawn.sendFollowing(l, VfxShape.JUDGMENT_FIELD, follow, center, GOLD, ticks, (float) radius);
        else VfxSpawn.send(l, VfxShape.JUDGMENT_FIELD, center, center.add(0.0, 1.0, 0.0), GOLD, ticks, (float) radius);
        sound(l, center, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.2f, 0.6f);
    }

    public static void cross(ServerLevel l, LivingEntity target) {
        Vec3 at = target.position();
        VfxSpawn.send(l, VfxShape.JUDGMENT_CROSS, at, at.add(0.0, 1.0, 0.0), GOLD, 70, 2.0f);
        sound(l, at, SoundEvents.BEACON_ACTIVATE, 2.0f, 0.5f);
    }

    /** The last toll of a Verdict of Ruin that ran out (3.21.0): a bell, no packet. */
    public static void toll(ServerLevel l, Vec3 at) {
        sound(l, at, SoundEvents.BELL_BLOCK, 1.6f, 0.5f);
    }

    public static void boom(ServerLevel l, Vec3 at) {
        sound(l, at, SoundEvents.GENERIC_EXPLODE.value(), 2.0f, 0.6f);
    }
}
