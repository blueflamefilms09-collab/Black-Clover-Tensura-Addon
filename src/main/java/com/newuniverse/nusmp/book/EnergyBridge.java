package com.newuniverse.nusmp.book;

import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 0.45 shared Tensura bridge for the extended attributes (Light, World Tree, Dice, Slash, Compass, Mercury).
 * <ul>
 *   <li><b>Pools:</b> pages spend magicules through Tensura as always; physical enhancement and high-speed movement
 *       (Light Speed, Ripper Dash, forearm blades) draw on <b>Aura</b> here, falling back to magicules.</li>
 *   <li><b>EP scaling:</b> {@link #power} (EP log + armour, 0.8..2.4, shared with Mirror Magic) and the gentler {@link #scale}.</li>
 *   <li><b>Effects:</b> Tensura status effects by id, spiritual damage, energy drain with transfer, Aura barriers.</li>
 * </ul>
 */
public final class EnergyBridge {
    private EnergyBridge() {}

    /** EP + armour + equipment, 0.8..2.4. */
    public static float power(LivingEntity e) { return MirrorWorks.power(e); }

    /** The same mapped to x0.9..x1.5 (damage and size). */
    public static float scale(LivingEntity e) { return 0.9f + (power(e) - 0.8f) / 1.6f * 0.6f; }

    /** A D&D-style modifier from EP: +0 .. +4. */
    public static int modifier(LivingEntity e) { return Mth.clamp((int) Math.floor((power(e) - 0.8f) / 0.4f), 0, 4); }

    /** Spends frac of max Aura (at least floor), else the same share of magicules. Creative / no Tensura data: free. */
    public static boolean aura(ServerPlayer p, double frac, double floor) {
        if (p.isCreative()) return true;
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return true;
        double auraCost = Math.max(floor, EnergyHelper.getMaxAura(p) * frac), mpCost = Math.max(floor, EnergyHelper.getMaxMagicule(p) * frac);
        if (ex.getAura() >= auraCost) ex.setAura(ex.getAura() - auraCost);
        else if (ex.getMagicule() >= mpCost) ex.setMagicule(ex.getMagicule() - mpCost);
        else return false;
        ex.markDirty();
        return true;
    }

    /** Energy Drain: takes frac of the target's max magicules and gives what was taken to 'to' (may be null). Returns the amount. */
    public static double drain(LivingEntity from, LivingEntity to, double frac) {
        var ex = TensuraStorages.getExistenceFrom(from);
        if (ex == null) return 0;
        double take = Math.min(ex.getMagicule(), EnergyHelper.getMaxMagicule(from) * frac);
        if (take <= 0) return 0;
        ex.setMagicule(ex.getMagicule() - take);
        ex.markDirty();
        if (to != null) {
            var ey = TensuraStorages.getExistenceFrom(to);
            if (ey != null) {
                ey.setMagicule(Math.min(EnergyHelper.getMaxMagicule(to), ey.getMagicule() + take));
                ey.markDirty();
            }
        }
        return take;
    }

    /** Spiritual damage: takes 'amount' spiritual health (never below 1). */
    public static void spirit(LivingEntity t, double amount) {
        var ex = TensuraStorages.getExistenceFrom(t);
        if (ex == null) return;
        ex.setSpiritualHealth(Math.max(1, ex.getSpiritualHealth() - amount));
        ex.markDirty();
    }

    /** A Tensura status effect by id (silence = magic jamming, fragility = resistance shred, ...), if Tensura has it. */
    public static void effect(LivingEntity t, String id, int ticks, int amp) { MirrorWorks.tensuraEffect(t, id, ticks, amp); }

    /** An Aura barrier: the target's aura is at least 30% full (what it takes to block Light Magic). */
    public static boolean auraBarrier(LivingEntity t) {
        var ex = TensuraStorages.getExistenceFrom(t);
        if (ex == null) return false;
        double max = EnergyHelper.getMaxAura(t);
        return max > 0 && ex.getAura() >= max * 0.3;
    }

    /** Burns some of the target's aura (an Aura barrier taking a hit). */
    public static void burnAura(LivingEntity t, double frac) {
        var ex = TensuraStorages.getExistenceFrom(t);
        if (ex == null) return;
        ex.setAura(Math.max(0, ex.getAura() - EnergyHelper.getMaxAura(t) * frac));
        ex.markDirty();
    }

    /**
     * How much to multiply raw damage so that 'bypass' (0..1) of the target's armour is ignored: armour reduces a hit to
     * after = f(raw); we scale raw by (raw / after) ^ bypass. Capped at x3.
     */
    public static float armourBypass(LivingEntity t, DamageSource src, float raw, float bypass) {
        if (bypass <= 0 || raw <= 0) return 1f;
        float armour = (float) t.getArmorValue(), tough = (float) t.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float after = CombatRules.getDamageAfterAbsorb(t, raw, src, armour, tough);
        if (after <= 0.01f) return 3f;
        return Mth.clamp((float) Math.pow(raw / after, Mth.clamp(bypass, 0, 1)), 1f, 3f);
    }
}
