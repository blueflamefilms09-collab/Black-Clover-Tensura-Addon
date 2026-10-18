package com.newuniverse.nusmp.elite.races.state;

import java.util.Locale;

/**
 * Why: a transformation changes attributes, and only by transient modifiers (never the base value), so a relog or a respawn can never leave a permanent buff. The attribute is a
 * short key ({@code attack_damage}, {@code movement_speed}, {@code scale}, {@code tensura:max_magicule} ...), the operation one of three.
 */
public record StateModifier(String attribute, double amount, Op op) {
    /** ADD = flat; MULT_BASE = percentage of the base value (all added together, like vanilla); MULT_TOTAL = percentage of the running total (multiplied in turn). */
    public enum Op {
        ADD, MULT_BASE, MULT_TOTAL;

        public static Op byId(String s) {
            if (s == null) return null;
            String t = s.trim().toLowerCase(Locale.ROOT);
            for (Op o : values()) if (o.name().toLowerCase(Locale.ROOT).equals(t)) return o;
            return null;
        }
    }

    public StateModifier scaled(double factor) { return new StateModifier(attribute, amount * factor, op); }
}
