package com.newuniverse.nusmp.skilltree.ability;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.mana.ManaLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: every Fairy Tail ability needs the same small set of things (foes in a ring, a line or a cone; a hit through the balance law; a heal; a cost in magicules; an effect; a sound; a line on
 * the action bar), and the owner's balance rules (the damage cap, the heal cap, one hard-control lock) only hold if they are written once. These are static helpers over the mod's own APIs
 * ({@link BalanceLaw}, {@link VfxSpawn}, the Tensura magicule pool), with no state, so a tree ability is a few lines of intent. They deliberately do NOT use anything of the Black Clover
 * grimoire (no mastery pages, no grimoire mana): the tree system's cost is the Tensura magicule pool and its power is the node level, which is the isolation the owner asked for.
 */
public final class AbilityKit {
    private AbilityKit() {}

    /** Living foes (alive, not allied to the caster, not the caster, not spectating) within {@code r} of {@code c}. */
    public static List<LivingEntity> around(ServerPlayer p, Vec3 c, double r) {
        return p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r),
                e -> e != p && e.isAlive() && !e.isSpectator() && !e.isAlliedTo(p) && e.distanceToSqr(c) <= r * r);
    }

    /** Foes within {@code width} of the segment a to b. */
    public static List<LivingEntity> along(ServerPlayer p, Vec3 a, Vec3 b, double width) {
        List<LivingEntity> out = new ArrayList<>();
        Vec3 ab = b.subtract(a);
        double len2 = Math.max(1e-6, ab.lengthSqr());
        for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(a, b).inflate(width + 1),
                e -> e != p && e.isAlive() && !e.isSpectator() && !e.isAlliedTo(p))) {
            Vec3 c = e.getBoundingBox().getCenter();
            double t = Math.max(0, Math.min(1, c.subtract(a).dot(ab) / len2));
            if (a.add(ab.scale(t)).distanceTo(c) <= width + e.getBbWidth() / 2) out.add(e);
        }
        return out;
    }

    /** Foes within {@code range} in front of the caster, inside {@code halfAngleDeg} of the look direction. */
    public static List<LivingEntity> cone(ServerPlayer p, double range, double halfAngleDeg) {
        List<LivingEntity> out = new ArrayList<>();
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        double min = Math.cos(Math.toRadians(halfAngleDeg));
        for (LivingEntity e : around(p, p.position(), range)) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            if (to.lengthSqr() < 1e-6 || to.normalize().dot(look) >= min) out.add(e);
        }
        return out;
    }

    /** A hit through the balance law: {@code raw} is the technique's base damage; {@code masteryFrac} 0..1 is how practised the skill is. False when the target is not hittable. */
    public static boolean hit(ServerPlayer p, LivingEntity target, float raw, double masteryFrac) {
        if (target == null || target == p || !target.isAlive() || target.isAlliedTo(p)) return false;
        return target.hurt(p.damageSources().playerAttack(p), BalanceLaw.damage(p, target, raw, masteryFrac));
    }

    /** A heal through the balance law. */
    public static void heal(LivingEntity target, float amount) {
        if (target != null && target.isAlive() && amount > 0f) BalanceLaw.heal(target, amount);
    }

    /** The shared hard-control gate (one stun, root or pull per 12 s per caster): call before applying any. */
    public static boolean control(ServerPlayer p) { return BalanceLaw.beginControl(p); }

    /**
     * Spends {@code share} of the caster's maximum magicules (then Aura for what they cannot cover); false and nothing spent when they cannot pay. Creative players and players without a Tensura
     * record pay nothing.
     */
    public static boolean pay(ServerPlayer p, double share) {
        if (p.isCreative() || share <= 0) return true;
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return true;
        double max = EnergyHelper.getMaxMagicule(p), cost = max * share;
        double mp = ex.getMagicule(), ap = ex.getAura();
        double fromMp = Math.min(cost, Math.max(0.0, mp));
        double rest = cost - fromMp;
        if (rest > ap) {
            say(p, "Not enough magicules.");
            return false;
        }
        ex.setMagicule(mp - fromMp);
        if (rest > 0) ex.setAura(Math.max(0.0, ap - rest));
        ex.markDirty();
        if (ex.getMagicule() < max * 0.05) ManaLaw.exhaust(p);
        return true;
    }

    /** One effect through the mod's effect system (never vanilla particle spam). {@code argb} is 0xAARRGGBB. */
    public static void fx(ServerLevel level, VfxShape shape, Vec3 from, Vec3 to, int argb, int ticks, float size) { VfxSpawn.send(level, shape, from, to, argb, ticks, size); }

    public static void sound(ServerPlayer p, SoundEvent sound, float volume, float pitch) { p.serverLevel().playSound(null, p.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch); }

    /** A line on the caster's action bar. */
    public static void say(ServerPlayer p, String text) { p.displayClientMessage(Component.literal(text), true); }
}
