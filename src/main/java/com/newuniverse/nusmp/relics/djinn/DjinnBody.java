package com.newuniverse.nusmp.relics.djinn;

import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;

/**
 * Trust and Assimilation run on Tensura, not on a private number (the owner's rule: no new standalone stats). Each is a persistent attribute
 * modifier with a stable id, saved with the player's attributes and kept through death by vanilla; the compound of {@link DjinnState} only holds a
 * copy as a fallback if the attribute is missing.
 * <ul>
 *   <li><b>Trust</b> = the Djinn's lesson in magic: a persistent {@code +0.2% ability mastery gain per point} on
 *       {@link TensuraAttributes#ABILITY_MASTERY_GAIN} (id {@code nusmp:djinn_trust}); 100 trust is +20% mastery gain.</li>
 *   <li><b>Assimilation</b> = the body turning into the Djinn's kind: a persistent {@code -0.5% ability learning gain per point} on
 *       {@link TensuraAttributes#ABILITY_LEARNING_GAIN} (id {@code nusmp:djinn_assimilation}; a mind that is less its own learns less), plus,
 *       re-applied while it lasts, a flat resistance of the Djinn's element (up to +0.3) and, from the mark, +5% of its element boost.</li>
 * </ul>
 */
final class DjinnBody {
    private DjinnBody() {}

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", path); }

    static final ResourceLocation TRUST = id("djinn_trust"), ASSIM = id("djinn_assimilation"), RESIST = id("djinn_assim_resist"), MARK = id("djinn_mark");
    static final double TRUST_PER_POINT = 0.002, ASSIM_PER_POINT = 0.005, RESIST_AT_FULL = 0.3;

    // ---------------------------------------------------------------- the persistent modifiers
    static int trust(Player p, CompoundTag fallback) {
        AttributeInstance a = p.getAttribute(TensuraAttributes.ABILITY_MASTERY_GAIN);
        AttributeModifier m = a == null ? null : a.getModifier(TRUST);
        if (m != null) return (int) Math.round(m.amount() / TRUST_PER_POINT);
        return fallback.contains("trust") ? fallback.getInt("trust") : DjinnPhaseRules.START_TRUST;
    }

    static void setTrust(Player p, CompoundTag tag, int v) {
        v = Math.max(0, Math.min(100, v));
        tag.putInt("trust", v);
        put(p, TensuraAttributes.ABILITY_MASTERY_GAIN, TRUST, v * TRUST_PER_POINT, true);
    }

    static float assimilation(Player p, CompoundTag fallback) {
        AttributeInstance a = p.getAttribute(TensuraAttributes.ABILITY_LEARNING_GAIN);
        AttributeModifier m = a == null ? null : a.getModifier(ASSIM);
        if (m != null) return (float) (-m.amount() / ASSIM_PER_POINT);
        return fallback.getFloat("assim");
    }

    static void setAssimilation(Player p, CompoundTag tag, float v) {
        v = Math.max(0f, Math.min(100f, v));
        tag.putFloat("assim", v);
        put(p, TensuraAttributes.ABILITY_LEARNING_GAIN, ASSIM, -v * ASSIM_PER_POINT, true);
    }

    /** Puts a modifier on, or off when the amount is zero; a persistent one is saved with the player. Skips the write when nothing changed. */
    private static void put(Player p, Holder<Attribute> attr, ResourceLocation id, double amount, boolean persistent) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        AttributeModifier old = inst.getModifier(id);
        if (Math.abs(amount) < 1.0E-9) {
            if (old != null) inst.removeModifier(id);
            return;
        }
        if (old != null && Math.abs(old.amount() - amount) < 1.0E-9) return;
        if (old != null) inst.removeModifier(id);
        if (persistent) inst.addPermanentModifier(new AttributeModifier(id, amount, Operation.ADD_MULTIPLIED_BASE));
        else inst.addTransientModifier(new AttributeModifier(id, amount, Operation.ADD_MULTIPLIED_BASE));
    }

    // ---------------------------------------------------------------- what assimilation does to the body, while it lasts
    /** The Tensura resistance of the Djinn's kind (the Djinn of Life, Strength, Sound or Lightning has no element resistance: magic resistance). */
    static Holder<Attribute> resistance(Affinity a) {
        return switch (a) {
            case HEAT -> TensuraAttributes.FLAME_RESISTANCE;
            case WATER -> TensuraAttributes.WATER_RESISTANCE;
            case WIND -> TensuraAttributes.WIND_RESISTANCE;
            case LIGHT -> TensuraAttributes.LIGHT_RESISTANCE;
            default -> TensuraAttributes.MAGIC_RESISTANCE;
        };
    }

    /** Re-applies the transient part: the resistance (flat, up to +0.3 at full assimilation) and, from the mark, +5% of the element boost. */
    static void syncTransient(Player p, Djinn d, float assimilation) {
        for (Affinity a : Affinity.values()) {
            Holder<Attribute> r = resistance(a);
            AttributeInstance inst = p.getAttribute(r);
            if (inst != null && inst.hasModifier(RESIST) && (d == null || r != resistance(d.affinity()))) inst.removeModifier(RESIST);
            Holder<Attribute> b = DjinnFx.boost(a);
            AttributeInstance bi = b == null ? null : p.getAttribute(b);
            if (bi != null && bi.hasModifier(MARK) && (d == null || b != DjinnFx.boost(d.affinity()))) bi.removeModifier(MARK);
        }
        if (d == null) return;
        double res = RESIST_AT_FULL * assimilation / 100.0;
        AttributeInstance inst = p.getAttribute(resistance(d.affinity()));
        if (inst != null) {
            AttributeModifier old = inst.getModifier(RESIST);
            if (res < 0.005) { if (old != null) inst.removeModifier(RESIST); }
            else if (old == null || Math.abs(old.amount() - res) > 0.004) {
                if (old != null) inst.removeModifier(RESIST);
                inst.addTransientModifier(new AttributeModifier(RESIST, res, Operation.ADD_VALUE));
            }
        }
        Holder<Attribute> b = DjinnFx.boost(d.affinity());
        AttributeInstance bi = b == null ? null : p.getAttribute(b);
        if (bi != null) {
            boolean on = assimilation >= DjinnPhaseRules.MARK_AT;
            if (on && !bi.hasModifier(MARK)) bi.addTransientModifier(new AttributeModifier(MARK, 0.05, Operation.ADD_MULTIPLIED_BASE));
            else if (!on && bi.hasModifier(MARK)) bi.removeModifier(MARK);
        }
    }

    /** Takes every Djinn modifier off the player (the Djinn is gone). */
    static void clear(Player p) {
        put(p, TensuraAttributes.ABILITY_MASTERY_GAIN, TRUST, 0, true);
        put(p, TensuraAttributes.ABILITY_LEARNING_GAIN, ASSIM, 0, true);
        syncTransient(p, null, 0f);
        for (Affinity a : Affinity.values()) {
            AttributeInstance inst = p.getAttribute(resistance(a));
            if (inst != null && inst.hasModifier(RESIST)) inst.removeModifier(RESIST);
        }
    }
}
