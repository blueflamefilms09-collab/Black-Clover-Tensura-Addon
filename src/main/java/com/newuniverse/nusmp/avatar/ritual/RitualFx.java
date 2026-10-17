package com.newuniverse.nusmp.avatar.ritual;

import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.AvatarSoulForgeConfig;
import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the ritual is a spectacle (the owner's "cinematic" requirement), and every picture of it must come from the mod's own effect system, never from
 * vanilla particle spam (purged in 1.36.0). This class is the one place that asks for them, all through {@link VfxSpawn}: the great circle on the ground
 * ({@code DIVINE_SIGIL}, a turning sigil whose rim closes over its life), the charging circle that bursts when the beam lands
 * ({@code MAGIC_CIRCLE_EXPLOSION}), the beam itself ({@code DIVINE_RAY}, thin and pulsing as a warning, then wide) with its column of light
 * ({@code DIVINE_PILLAR}), the anti-magic dome ({@code NIHILITY_ZONE}) when the Avatar carries Anti-Magic, and the aura of the Avatar's first ability
 * ({@code SPIRIT_AURA}). The Time Magic effects are never used. The telegraph form of a shape that has one is chosen by the lowest bit of the seed, which this
 * class sets itself instead of leaving it to chance. Sounds are vanilla sound events (not particles). The whole class is silent when the config's
 * {@code effects} switch is off, so the mechanics can run without the show.
 */
public final class RitualFx {
    private RitualFx() {}

    public static final int VIOLET = 0xFF9A7CFF, CYAN = 0xFF5CD8FF, GOLD = 0xFFE8C468, ANTI = 0xFF7A2CA0;

    private static void send(ServerLevel level, VfxShape shape, Vec3 from, Vec3 to, int argb, int duration, float power, boolean telegraph) {
        long seed = level.random.nextLong();
        seed = telegraph ? (seed | 1L) : (seed & ~1L);
        VfxSpawn.send(level, new VfxPayload(shape.ordinal(), from, to, argb, duration, power, -1, seed));
    }

    private static void sound(ServerLevel level, Vec3 at, SoundEvent ev, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, ev, SoundSource.BLOCKS, volume, pitch);
    }

    /** The great circle drawn on the ground at {@code center} for {@code ticks} (the charge, and again while the screen is open). */
    public static void circle(ServerLevel level, Vec3 center, int ticks) {
        if (!AvatarSoulForgeConfig.effects()) return;
        send(level, VfxShape.DIVINE_SIGIL, center, center.add(0, 1, 0), VIOLET, Math.max(10, ticks), AvatarSoulForgeConfig.sigilRadius(), false);
    }

    /** The first moment of the charge: the forge wakes. */
    public static void begin(ServerLevel level, Vec3 center, int chargeTicks) {
        if (!AvatarSoulForgeConfig.effects()) return;
        circle(level, center, chargeTicks + 20);
        sound(level, center, SoundEvents.BEACON_ACTIVATE, 1.2f, 0.7f);
        sound(level, center, SoundEvents.ENCHANTMENT_TABLE_USE, 1.0f, 0.6f);
    }

    /** The ritual ended without a birth: the circle is let go. */
    public static void cancelled(ServerLevel level, Vec3 center) {
        if (!AvatarSoulForgeConfig.effects()) return;
        sound(level, center, SoundEvents.BEACON_DEACTIVATE, 1.0f, 0.8f);
    }

    /** The warning of the beam: the circle charges (it bursts at {@code strikeTick}) and a thin line of light stands over it. */
    public static void tell(ServerLevel level, Vec3 center, int strikeTick, boolean anti) {
        if (!AvatarSoulForgeConfig.effects()) return;
        int color = anti ? ANTI : VIOLET;
        send(level, VfxShape.MAGIC_CIRCLE_EXPLOSION, center, center.add(0, 1, 0), color, (int) Math.ceil(strikeTick / 0.6), 2.6f, false);
        send(level, VfxShape.DIVINE_RAY, center.add(0, 26, 0), center, color, Math.max(6, strikeTick), 0.8f, true);
        sound(level, center, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.4f, 0.8f);
    }

    /** The beam of magicules (or of anti-magic) strikes the circle. */
    public static void strike(ServerLevel level, Vec3 center, boolean anti) {
        if (!AvatarSoulForgeConfig.effects()) return;
        int color = anti ? ANTI : CYAN;
        send(level, VfxShape.DIVINE_RAY, center.add(0, 26, 0), center, color, 16, 2.2f, false);
        send(level, VfxShape.DIVINE_PILLAR, center, center.add(0, 1, 0), anti ? ANTI : VIOLET, 24, 2.4f, false);
        if (anti) send(level, VfxShape.NIHILITY_ZONE, center, center.add(0, 1, 0), ANTI, 36, 4.5f, false);
        sound(level, center, SoundEvents.LIGHTNING_BOLT_THUNDER, 3.0f, 0.9f);
        sound(level, center, SoundEvents.BEACON_POWER_SELECT, 1.4f, 0.6f);
    }

    /** The Avatar steps out of the circle. */
    public static void emerge(ServerLevel level, Vec3 at, int argb) {
        if (!AvatarSoulForgeConfig.effects()) return;
        send(level, VfxShape.MAGIC_CIRCLE_EXPLOSION, at, at.add(0, 1, 0), argb, 14, 1.0f, false);
        sound(level, at, SoundEvents.AMETHYST_BLOCK_CHIME, 1.6f, 0.7f);
        sound(level, at, SoundEvents.TOTEM_USE, 0.5f, 1.4f);
    }

    /** The aura of the Avatar's first ability, following it for two seconds. */
    public static void novaAura(ServerLevel level, AvatarEntity avatar, int argb) {
        if (!AvatarSoulForgeConfig.effects()) return;
        VfxSpawn.sendFollowing(level, VfxShape.SPIRIT_AURA, avatar, avatar.position().add(0, 1, 0), argb, 40, 1.4f);
        sound(level, avatar.position(), SoundEvents.EVOKER_CAST_SPELL, 1.0f, 1.3f);
    }
}
