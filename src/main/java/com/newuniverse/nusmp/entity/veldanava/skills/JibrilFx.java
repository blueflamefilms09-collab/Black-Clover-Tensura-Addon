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
 * Why: every sight and sound of Jibril goes out from one place, so the cost is easy to read: one effect packet per ability use (the Kingdom of God and the wings: one every
 * {@link JibrilRules#KINGDOM_FX_EVERY_TICKS} / {@link JibrilRules#WINGS_FX_EVERY_TICKS} ticks while they last), always through {@link VfxSpawn}, which sends to nearby players only and goes through the
 * effect-packet budget. The shapes are drawn by the client's {@code JibrilLayer}. Server thread only.
 */
public final class JibrilFx {
    private JibrilFx() {}

    /** Warm white-gold, the tint every Jibril effect starts from (the layer adds the sky-blue shadow and the rose accents). */
    public static final int GOLD = 0xFFFFE9A8;

    private static void sound(ServerLevel l, Vec3 at, SoundEvent s, float volume, float pitch) {
        l.playSound(null, at.x, at.y, at.z, s, SoundSource.PLAYERS, volume, pitch);
    }

    private static Vec3 centre(LivingEntity e) { return e.position().add(0.0, e.getBbHeight() * 0.5, 0.0); }

    private static float size(LivingEntity e) { return (float) Math.max(1.0, e.getBbWidth() * 1.6); }

    /** Absolute Command: the halt glyph over the target for as long as it is held. */
    public static void command(ServerLevel l, LivingEntity target, int ticks) {
        VfxSpawn.sendFollowing(l, VfxShape.JIBRIL_COMMAND, target, centre(target), GOLD, Math.max(10, Math.min(100, ticks)), size(target));
        sound(l, target.position(), SoundEvents.BELL_RESONATE, 1.4f, 0.6f);
    }

    /** Judgment of Heaven: the pillar of light on the ground point under the target. */
    public static void judgment(ServerLevel l, Vec3 ground) {
        VfxSpawn.send(l, VfxShape.JIBRIL_JUDGMENT, ground, ground.add(0.0, 1.0, 0.0), GOLD, 40, (float) JibrilRules.JUDGMENT_RADIUS);
        sound(l, ground, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.6f, 1.6f);
        sound(l, ground, SoundEvents.BEACON_ACTIVATE, 1.8f, 1.4f);
    }

    /** Heavenly Purge: the holy wave out to the radius. */
    public static void purge(ServerLevel l, Vec3 centre, float radius) {
        VfxSpawn.send(l, VfxShape.JIBRIL_PURGE, centre, centre.add(0.0, 1.0, 0.0), GOLD, 36, radius);
        sound(l, centre, SoundEvents.BEACON_POWER_SELECT, 2.0f, 1.2f);
    }

    /** Holy Shackles: the chains on the target, the stolen light flowing to the caster's chest. */
    public static void shackles(ServerLevel l, LivingEntity target, LivingEntity caster) {
        VfxSpawn.sendFollowing(l, VfxShape.JIBRIL_SHACKLES, target, caster.position().add(0.0, caster.getBbHeight() * 0.7, 0.0), GOLD, 60, size(target));
        sound(l, target.position(), SoundEvents.CHAIN_PLACE, 1.6f, 0.7f);
    }

    /** Word of Law: the glyph in front of the caster for the length of the parry window. */
    public static void word(ServerLevel l, LivingEntity caster, int ticks) {
        VfxSpawn.sendFollowing(l, VfxShape.JIBRIL_WORD, caster, centre(caster), GOLD, Math.max(8, Math.min(20, ticks)), 1.8f);
        sound(l, caster.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.6f, 0.5f);
    }

    /** The parry landed: a flash on the caster and beams to the attacker. */
    public static void reflect(ServerLevel l, LivingEntity caster, LivingEntity attacker) {
        VfxSpawn.send(l, VfxShape.JIBRIL_REFLECT, centre(caster), centre(attacker), GOLD, 14, 1.0f);
        sound(l, caster.position(), SoundEvents.SHIELD_BLOCK, 1.6f, 1.5f);
    }

    /** Kingdom of God: the sanctuary round the caster. Sent when it is raised and again every {@link JibrilRules#KINGDOM_FX_EVERY_TICKS} ticks; the sound only the first time. */
    public static void kingdom(ServerLevel l, LivingEntity caster, boolean first) {
        VfxSpawn.sendFollowing(l, VfxShape.JIBRIL_KINGDOM, caster, centre(caster), GOLD, 100, (float) JibrilRules.KINGDOM_RADIUS);
        if (first) sound(l, caster.position(), SoundEvents.BEACON_ACTIVATE, 3.0f, 0.8f);
    }

    /** Wings of Revelation: drawn behind the player while they are in the air. */
    public static void wings(ServerLevel l, LivingEntity player, boolean first) {
        VfxSpawn.sendFollowing(l, VfxShape.JIBRIL_WINGS, player, centre(player), GOLD, 100, 1.0f);
        if (first) sound(l, player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 1.3f);
    }
}
