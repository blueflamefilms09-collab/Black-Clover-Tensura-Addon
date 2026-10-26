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
 * Why: every sight and sound of Hercules goes out from one place, so the cost is easy to read: one effect packet per ability use (Invictus: one every {@link HerculesRules#INVICTUS_PULSE_TICKS} x
 * {@link HerculesRules#INVICTUS_FX_EVERY_PULSES} ticks while it lasts; the passives: at most one shockwave picture per player every {@link HerculesRules#SHOCK_GAP_TICKS} ticks, one plate picture every
 * {@link HerculesRules#BODY_FX_GAP_TICKS}, one momentum picture per stage reached), always through {@link VfxSpawn}, which sends to nearby players only and goes through the effect-packet budget. The shapes
 * are drawn by the client's {@code HerculesLayer}; its palette is its own, the colour passed here is only a tint hint. Server thread only.
 */
public final class HerculesFx {
    private HerculesFx() {}

    /** Burnished gold, the tint every Hercules effect starts from (the layer adds the bronze, the ivory and the ember). */
    public static final int GOLD = 0xFFE8B84A;

    private static void sound(ServerLevel l, Vec3 at, SoundEvent s, float volume, float pitch) {
        l.playSound(null, at.x, at.y, at.z, s, SoundSource.PLAYERS, volume, pitch);
    }

    private static Vec3 centre(LivingEntity e) { return e.position().add(0.0, e.getBbHeight() * 0.5, 0.0); }

    /** Earth-Shattering Tremor, the take-off. */
    public static void leap(ServerLevel l, LivingEntity caster, Vec3 aimed) {
        VfxSpawn.send(l, VfxShape.HERCULES_LEAP, caster.position(), aimed, GOLD, 16, 1.0f);
        sound(l, caster.position(), SoundEvents.IRON_GOLEM_ATTACK, 1.6f, 0.6f);
    }

    /** Earth-Shattering Tremor, the landing. */
    public static void tremor(ServerLevel l, Vec3 at, double radius) {
        VfxSpawn.send(l, VfxShape.HERCULES_TREMOR, at, at, GOLD, 44, (float) radius);
        sound(l, at, SoundEvents.GENERIC_EXPLODE.value(), 2.4f, 0.5f);
        sound(l, at, SoundEvents.ANVIL_LAND, 2.0f, 0.5f);
    }

    /** Invictus: the laurel and the flame round the owner, sent when it starts and again every 80 ticks. */
    public static void invictus(ServerLevel l, LivingEntity caster, boolean first) {
        VfxSpawn.sendFollowing(l, VfxShape.HERCULES_INVICTUS, caster, centre(caster), GOLD, 100, 1.6f);
        if (first) sound(l, caster.position(), SoundEvents.BEACON_ACTIVATE, 2.0f, 0.5f);
    }

    /** Titan's Grip: the hand closes on the creature and the chains haul it to the owner. */
    public static void grip(ServerLevel l, LivingEntity caster, LivingEntity target) {
        VfxSpawn.send(l, VfxShape.HERCULES_GRIP, centre(caster), centre(target), GOLD, 24, 1.0f);
        sound(l, target.position(), SoundEvents.CHAIN_BREAK, 1.8f, 0.5f);
    }

    /** Devastating Throw: the streak from where the creature or chunk was lifted to where it will land. */
    public static void thrown(ServerLevel l, Vec3 from, Vec3 to) { thrown(l, from, to, false); }

    /** Devastating Throw; {@code chunk} = a lump of ground is hurled (power 2: the client draws the boulder), not a creature (power 1). */
    public static void thrown(ServerLevel l, Vec3 from, Vec3 to, boolean chunk) {
        VfxSpawn.send(l, VfxShape.HERCULES_THROW, from, to, GOLD, 30, chunk ? 2.0f : 1.0f);
        sound(l, from, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 2.0f, 0.5f);
    }

    /** The place a throw lands: a thud (the picture of the landing is part of the throw's packet). */
    public static void thud(ServerLevel l, Vec3 at) {
        sound(l, at, SoundEvents.GENERIC_EXPLODE.value(), 1.8f, 0.7f);
    }

    /** Battle Roar: the roar runs out to the radius. */
    public static void roar(ServerLevel l, LivingEntity caster) {
        VfxSpawn.send(l, VfxShape.HERCULES_ROAR, centre(caster), centre(caster), GOLD, 36, (float) HerculesRules.ROAR_RADIUS);
        sound(l, caster.position(), SoundEvents.RAVAGER_ROAR, 3.0f, 0.6f);
        sound(l, caster.position(), SoundEvents.ENDER_DRAGON_GROWL, 1.5f, 0.5f);
    }

    /** World-Breaking Strike: the winding fist, the punch, the cracked air. */
    public static void strike(ServerLevel l, LivingEntity caster, Vec3 point) {
        VfxSpawn.send(l, VfxShape.HERCULES_STRIKE, centre(caster), point, GOLD, 44, 1.0f);
        sound(l, caster.position(), SoundEvents.ENDER_DRAGON_HURT, 2.5f, 0.5f);
        sound(l, point, SoundEvents.GLASS_BREAK, 3.0f, 0.5f);
        sound(l, point, SoundEvents.GENERIC_EXPLODE.value(), 3.0f, 0.5f);
    }

    /** God of Strength: the ring of a heavy blow. */
    public static void shockwave(ServerLevel l, Vec3 at) {
        VfxSpawn.send(l, VfxShape.HERCULES_SHOCKWAVE, at, at, GOLD, 12, (float) HerculesRules.SHOCK_RADIUS);
    }

    /** Adamantine Body turned a blow aside. */
    public static void body(ServerLevel l, LivingEntity wearer) {
        VfxSpawn.send(l, VfxShape.HERCULES_BODY, centre(wearer), centre(wearer), GOLD, 12, (float) Math.max(1.0, wearer.getBbWidth() * 1.6));
        sound(l, wearer.position(), SoundEvents.ANVIL_PLACE, 0.9f, 1.6f);
    }

    /** Unstoppable Momentum grew to a stage. */
    public static void momentum(ServerLevel l, LivingEntity runner, int stage) {
        VfxSpawn.sendFollowing(l, VfxShape.HERCULES_MOMENTUM, runner, centre(runner), GOLD, 40, (float) stage);
    }
}
