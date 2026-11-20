package com.newuniverse.nusmp.lords;

import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the one place the Lord weapons and Greed's new states send their effects (the {@code LORD_*} shapes drawn by {@code LordLayer}). Each call is ONE packet through {@link VfxSpawn} (the per-tick effect budget,
 * players within 96 blocks only). Worst case per use: one art picture plus at most {@link #MAX_HIT_FLASHES} small hit flashes; the strained aura is one packet per wielder per three seconds; a level-up one packet. Never per
 * tick, never per target beyond the cap. With the server setting {@code themedVfx} off nothing here is sent.
 */
public final class LordFx {
    private LordFx() {}

    public static final int MAX_HIT_FLASHES = 3;
    /** The ticks between two strained auras of one wielder. */
    public static final int STATE_GAP_TICKS = 60;

    static VfxShape shape(LordRules.Sin s) {
        return switch (s) {
            case SLOTH -> VfxShape.LORD_SLOTH;
            case ENVY -> VfxShape.LORD_ENVY;
            case VANITY -> VfxShape.LORD_VANITY;
            case HOLY -> VfxShape.LORD_HOLY;
            case GREED -> VfxShape.LORD_GREED;
        };
    }

    /** One picture of {@code style} of {@code sin}, from {@code from} to {@code to}; {@code radius} is the size of an area picture (0 for a line). */
    public static void send(ServerLevel level, LordRules.Sin sin, int style, Vec3 from, Vec3 to, float radius) {
        if (!LordConfig.themedVfx()) return;
        VfxSpawn.send(level, shape(sin), from, to, 0xFF000000 | sin.rgb, LordVfxCodes.duration(style), LordVfxCodes.power(sin, style, radius));
    }

    /** A picture that follows {@code e} (kept at the same offset from it). */
    public static void follow(ServerLevel level, LordRules.Sin sin, int style, Entity e, Vec3 at, float radius) {
        if (!LordConfig.themedVfx()) return;
        VfxSpawn.send(level, new VfxPayload(shape(sin).ordinal(), at, at, 0xFF000000 | sin.rgb, LordVfxCodes.duration(style), LordVfxCodes.power(sin, style, radius), e.getId(), level.random.nextLong()));
    }

    public static Vec3 chest(Entity e) { return e.position().add(0.0, e.getBbHeight() * 0.6, 0.0); }

    public static Vec3 hand(LivingEntity p) { return p.getEyePosition().add(0.0, -0.35, 0.0).add(p.getLookAngle().scale(0.6)); }
}
