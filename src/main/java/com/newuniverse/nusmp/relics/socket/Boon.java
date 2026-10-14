package com.newuniverse.nusmp.relics.socket;

/**
 * One passive gift of a bound socket, as plain data (no Minecraft types, so the table is tested). {@link SocketEngine} turns it into a mob effect,
 * a lent Tensura skill, an attribute modifier, a damage resistance or a life steal, once a second while the relic is equipped and working.
 *
 * @param type   what sort of gift
 * @param key    effect id, skill id, attribute id or damage type id
 * @param amount attribute amount, resisted fraction (0 to 0.9) or life-steal fraction of the damage dealt
 * @param amp    effect amplifier, or the attribute operation's ordinal (0 add, 1 multiply base, 2 multiply total)
 */
public record Boon(Type type, String key, double amount, int amp) {
    public enum Type { EFFECT, SKILL, ATTR, RESIST, LIFESTEAL }

    public static final int OP_ADD = 0, OP_MULTIPLY_BASE = 1, OP_MULTIPLY_TOTAL = 2;

    public static Boon effect(String id, int amp) { return new Boon(Type.EFFECT, id, 0, Math.max(0, amp)); }

    public static Boon skill(String id) { return new Boon(Type.SKILL, id, 0, 0); }

    public static Boon attr(String id, double amount, int op) { return new Boon(Type.ATTR, id, amount, Math.max(0, Math.min(2, op))); }

    public static Boon resist(String damageTypeId, double fraction) { return new Boon(Type.RESIST, damageTypeId, Math.max(0.0, Math.min(0.9, fraction)), 0); }

    /** {@code fraction} of the damage the wearer deals comes back as health (see {@link SocketRules#lifesteal}). */
    public static Boon lifesteal(double fraction) { return new Boon(Type.LIFESTEAL, "lifesteal", Math.max(0.0, Math.min(0.25, fraction)), 0); }
}
