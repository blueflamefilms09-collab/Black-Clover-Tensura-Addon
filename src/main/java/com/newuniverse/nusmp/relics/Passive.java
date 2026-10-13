package com.newuniverse.nusmp.relics;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Something a relic does by merely being worn (and attuned, if it needs that): an attribute modifier, an ambient effect, a damage resistance, or a
 * named switch that a wave's own handlers read with {@link RelicFlags#has}, or a Tensura skill lent while worn ({@link #skill}). Built with the four factories; ids are resolved at run time and a
 * missing attribute, effect or damage type is skipped silently (an optional mod may be absent).
 *
 * @param kind   what sort of passive
 * @param key    attribute id, mob effect id, damage type id or tag, the flag name, or the skill id
 * @param amount attribute amount, or the resisted fraction (0 to 0.9)
 * @param amp    effect amplifier, or the attribute operation's ordinal
 */
public record Passive(Kind kind, String key, double amount, int amp) {
    public enum Kind { ATTR, EFFECT, RESIST, FLAG, SKILL }

    /** An attribute modifier while worn. {@code attributeId} is a registry id such as "minecraft:generic.max_health" or "tensura:max_aura". */
    public static Passive attr(String attributeId, double amount, AttributeModifier.Operation op) {
        return new Passive(Kind.ATTR, attributeId, amount, op.ordinal());
    }

    /** A mob effect kept alive while worn (ambient, no particles); amplifier 0 is level I. */
    public static Passive effect(String mobEffectId, int amplifier) { return new Passive(Kind.EFFECT, mobEffectId, 0, Math.max(0, amplifier)); }

    /** Cuts damage of this type or tag ("minecraft:is_fire", "minecraft:lightning_bolt") by a fraction, 0 to 0.9. */
    public static Passive resist(String damageTypeIdOrTag, double fraction) {
        return new Passive(Kind.RESIST, damageTypeIdOrTag.startsWith("#") ? damageTypeIdOrTag.substring(1) : damageTypeIdOrTag,
                Math.max(0.0, Math.min(0.9, fraction)), 0);
    }

    /** A named switch for the wave's own handlers. */
    public static Passive flag(String key) { return new Passive(Kind.FLAG, key, 0, 0); }

    /**
     * A Tensura skill lent while the relic is worn (and attuned, if it needs that): the engine keeps a 60-tick lease on it, refreshed every second, so
     * it is gone a few seconds after the relic is taken off. A skill the player owns for good is left alone; an id the skill registry does not know
     * does nothing. {@code skillId} is a skill id such as "tensura:magic_sense".
     */
    public static Passive skill(String skillId) { return new Passive(Kind.SKILL, skillId, 0, 0); }

    /** The operation of an ATTR passive. */
    public AttributeModifier.Operation operation() {
        AttributeModifier.Operation[] all = AttributeModifier.Operation.values();
        return all[Math.max(0, Math.min(all.length - 1, amp))];
    }
}
