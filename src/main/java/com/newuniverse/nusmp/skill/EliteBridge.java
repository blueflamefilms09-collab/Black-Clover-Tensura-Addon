package com.newuniverse.nusmp.skill;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: the 1.83.0 EliteTensuraAddon skills live in {@code elite/skills}, but the rebuilt skills' helpers (who a skill may hurt, a hit through the balance law, the Rukh-only effects, the
 * zone scheduler, the pack-wide cost and cooldown multipliers) are package-private in this package on purpose: they are the one vetted way to do those things. Rather than copy them
 * (two copies would drift) this tiny public facade hands the elite skills the same functions, and nothing else. It adds no behaviour; every method delegates one line. It is NEW in 1.83.0
 * and changes nothing of the existing classes.
 */
public final class EliteBridge {
    private EliteBridge() {}

    // ---------------------------------------------------------------- who
    public static boolean isFoe(ServerPlayer p, LivingEntity e) { return SkillTargets.isFoe(p, e); }

    public static boolean isAimable(ServerPlayer p, LivingEntity e) { return SkillTargets.isAimable(p, e); }

    public static List<LivingEntity> foes(ServerPlayer p, Vec3 center, double radius) { return SkillTargets.foes(p, center, radius); }

    public static List<LivingEntity> foesInCone(ServerPlayer p, double range, double minDot) { return SkillTargets.foesInCone(p, range, minDot); }

    public static List<LivingEntity> foesAlong(ServerPlayer p, Vec3 from, Vec3 to, double width) { return SkillTargets.foesAlong(p, from, to, width); }

    public static List<LivingEntity> alliesAround(ServerPlayer p, Vec3 center, double radius) { return SkillTargets.alliesAround(p, center, radius); }

    public static LivingEntity lookFoe(ServerPlayer p, double range) { return SkillTargets.lookFoe(p, range); }

    public static LivingEntity lookAlly(ServerPlayer p, double range) { return SkillTargets.lookAlly(p, range); }

    /** Where the crosshair hits a block, or the point at {@code range}. */
    public static Vec3 lookPoint(ServerPlayer p, double range) { return DMUtil.lookPoint(p, range); }

    // ---------------------------------------------------------------- damage, cost
    /** A hit from the caster through the balance law (EP scaling, PvP cap, DanMachi multiplier). Returns the health actually removed. */
    public static float hit(ServerPlayer caster, LivingEntity target, double raw, double masteryFrac, boolean magic) {
        return SkillDamage.hit(caster, target, raw, masteryFrac, magic ? SkillDamage.Kind.MAGIC : SkillDamage.Kind.ATTACK);
    }

    /** Cost after the pack-wide multiplier. */
    public static double cost(double base) { return DMUtil.cost(base); }

    /** Cooldown ticks after the pack-wide multiplier. */
    public static int cooldown(int baseTicks) { return DMUtil.cooldown(baseTicks); }

    public static boolean hasMagicules(ServerPlayer p, double cost) { return SkillUtil.hasMagicules(p, cost); }

    /** Spends magicules, or tells the player and returns false. */
    public static boolean spendMagicules(ServerPlayer p, double cost) { return SkillUtil.spendMagicules(p, cost); }

    public static void refund(ServerPlayer p, double amount) { SkillUtil.refund(p, amount); }

    public static int cleanse(LivingEntity e) { return SkillUtil.cleanse(e); }

    // ---------------------------------------------------------------- messages
    public static void actionbar(ServerPlayer p, Component msg) { SkillUtil.actionbar(p, msg); }

    public static void fail(ServerPlayer p, Component msg) { SkillUtil.fail(p, msg); }

    // ---------------------------------------------------------------- effects (Rukh and the mod's shapes only)
    public static void burst(ServerLevel level, Vec3 at, boolean dark, int count, double spread) { SkillFx.burst(level, at, dark, count, spread); }

    public static void spray(ServerLevel level, boolean dark, Vec3 at, int count, double dx, double dy, double dz, double speed) { SkillFx.spray(level, dark, at, count, dx, dy, dz, speed); }

    public static void aura(ServerLevel level, Entity e, boolean dark) { SkillFx.aura(level, e, dark); }

    public static void beam(ServerLevel level, Vec3 from, Vec3 to, boolean dark) { SkillFx.beam(level, from, to, dark); }

    public static void ring(ServerLevel level, Vec3 at, double radius, int rgb) { SkillFx.ring(level, at, radius, rgb); }

    public static void shape(ServerLevel level, com.newuniverse.nusmp.vfx.VfxShape shape, Vec3 from, Vec3 to, int argb, int duration, float power) { SkillFx.shape(level, shape, from, to, argb, duration, power); }

    public static void follow(ServerLevel level, com.newuniverse.nusmp.vfx.VfxShape shape, Entity who, Vec3 to, int argb, int duration, float power) { SkillFx.follow(level, shape, who, to, argb, duration, power); }

    public static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) { SkillFx.sound(level, at, sound, volume, pitch); }

    /** The Black Clover-style cast circle under the caster. */
    public static void castVfx(ServerPlayer p, int argb) { SkillUtil.castVfx(p, argb); }

    // ---------------------------------------------------------------- zones
    /** One pulse of a lingering zone; {@code index} counts from 0. */
    @FunctionalInterface
    public interface ZonePulse { void run(ServerPlayer owner, ServerLevel level, int index); }

    /** Starts a zone on the owner's own tick (at most four per owner; the oldest is dropped). */
    public static void startZone(ServerPlayer owner, int interval, int pulses, ZonePulse pulse) {
        SkillZones.start(owner, interval, pulses, pulse::run);
    }
}
