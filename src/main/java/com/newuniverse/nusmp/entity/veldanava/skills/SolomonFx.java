package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: every sight and sound of Solomon goes out from one place, so the cost is easy to read: one effect packet per ability use (the Realm of Magic: one every
 * {@link SolomonRules#REALM_FX_EVERY_TICKS} ticks while it stands; the 72 Pillars: one packet per marked target, at most {@link SolomonRules#PILLAR_TARGETS_MAX}, plus one mark each), always through
 * {@link VfxSpawn}, which sends to nearby players only and goes through the effect-packet budget. The shapes are drawn by the client's {@code SolomonLayer}; its palette is its own, the colour
 * passed here is only a tint hint. Server thread only.
 */
public final class SolomonFx {
    private SolomonFx() {}

    /** Antique gold, the tint every Solomon effect starts from (the layer adds the indigo night and the six element colours). */
    public static final int GOLD = 0xFFE6C36A;

    private static void sound(ServerLevel l, Vec3 at, SoundEvent s, float volume, float pitch) {
        l.playSound(null, at.x, at.y, at.z, s, SoundSource.PLAYERS, volume, pitch);
    }

    private static Vec3 centre(LivingEntity e) { return e.position().add(0.0, e.getBbHeight() * 0.5, 0.0); }

    /** Grand Spell Copy: the scan from the copied caster's place to the caster. {@code source} is the caster's own place when the spell came from the grimoire. */
    public static void copy(ServerLevel l, LivingEntity caster, Vec3 source) {
        VfxSpawn.send(l, VfxShape.SOLOMON_COPY, centre(caster), source, GOLD, 30, 1.0f);
        sound(l, caster.position(), SoundEvents.ENCHANTMENT_TABLE_USE, 1.6f, 0.8f);
    }

    /** Arcane Convergence: the ring that closes in. */
    public static void convergence(ServerLevel l, LivingEntity caster) {
        VfxSpawn.send(l, VfxShape.SOLOMON_CONVERGENCE, centre(caster), centre(caster), GOLD, 30, (float) SolomonRules.CONVERGENCE_RADIUS);
        sound(l, caster.position(), SoundEvents.BEACON_POWER_SELECT, 1.8f, 1.6f);
    }

    /** Realm of Magic: the domain round the caster, sent when it is raised and again every {@link SolomonRules#REALM_FX_EVERY_TICKS} ticks. */
    public static void realm(ServerLevel l, LivingEntity caster, boolean first) {
        VfxSpawn.sendFollowing(l, VfxShape.SOLOMON_REALM, caster, centre(caster), GOLD, 100, (float) SolomonRules.REALM_RADIUS);
        if (first) sound(l, caster.position(), SoundEvents.BEACON_ACTIVATE, 3.0f, 0.6f);
    }

    /** Spell Chain: three circles in the line the caster looks along. */
    public static void chain(ServerLevel l, LivingEntity caster) {
        Vec3 look = caster.getLookAngle();
        Vec3 from = centre(caster);
        VfxSpawn.send(l, VfxShape.SOLOMON_CHAIN, from, from.add(look.scale(9.0)), GOLD, 24, 1.0f);
        sound(l, caster.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.8f, 1.2f);
    }

    /** Mana Zone Erasure: the seal and the wave that un-writes. */
    public static void erasure(ServerLevel l, LivingEntity caster) {
        VfxSpawn.send(l, VfxShape.SOLOMON_ERASURE, centre(caster), centre(caster), GOLD, 30, (float) SolomonRules.ERASURE_RADIUS);
        sound(l, caster.position(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), 1.8f, 0.7f);
    }

    /** The mark of the 72 Pillars: a seal that closes under the target and follows it. */
    public static void mark(ServerLevel l, LivingEntity target) {
        VfxSpawn.sendFollowing(l, VfxShape.SOLOMON_MARK, target, centre(target), GOLD, 40, (float) Math.max(1.0, target.getBbWidth() * 1.5));
        sound(l, target.position(), SoundEvents.ENCHANTMENT_TABLE_USE, 1.4f, 0.5f);
    }

    /**
     * The 72 Pillars on one target: one packet, the pillars are drawn from its seed ({@link SolomonRules#packSeed}, which also carries the count) ({@link SolomonRules#pillarSpawnTick}, {@link SolomonRules#pillarElement},
     * {@link SolomonRules#pillarOffset}), the same functions the server's impacts use, so the picture and the damage agree without further packets.
     */
    public static void pillars(ServerLevel l, Vec3 ground, long seed) {
        VfxSpawn.send(l, new VfxPayload(VfxShape.SOLOMON_PILLAR.ordinal(), ground, ground.add(0.0, 1.0, 0.0), GOLD,
                SolomonRules.PILLAR_SPAN_TICKS + SolomonRules.PILLAR_DELAY_TICKS, (float) SolomonRules.PILLAR_SPREAD, -1, seed));
    }

    /** The cast of the 72 Pillars: the sound that opens it. */
    public static void pillarsCast(ServerLevel l, LivingEntity caster) {
        sound(l, caster.position(), SoundEvents.END_PORTAL_SPAWN, 2.0f, 1.2f);
    }

    /** One pillar has landed (a thunder at the impact; at most one sound for every few pillars is needed, the caller thins them). */
    public static void impact(ServerLevel l, Vec3 at) {
        sound(l, at, SoundEvents.GENERIC_EXPLODE.value(), 1.2f, 1.1f);
    }

    /** Mana Skin saved a life. */
    public static void skin(ServerLevel l, LivingEntity wearer) {
        VfxSpawn.send(l, VfxShape.SOLOMON_SKIN, centre(wearer), centre(wearer), GOLD, 16, (float) Math.max(1.0, wearer.getBbWidth() * 1.6));
        sound(l, wearer.position(), SoundEvents.AMETHYST_CLUSTER_BREAK, 1.6f, 0.6f);
    }

    /** Elemental Sovereignty took a projectile. */
    public static void absorb(ServerLevel l, LivingEntity wearer, Vec3 projectile) {
        VfxSpawn.send(l, VfxShape.SOLOMON_ABSORB, projectile, wearer.position().add(0.0, wearer.getBbHeight() * 0.7, 0.0), GOLD, 14, 1.0f);
    }
}
