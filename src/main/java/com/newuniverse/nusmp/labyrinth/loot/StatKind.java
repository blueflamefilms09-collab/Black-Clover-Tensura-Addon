package com.newuniverse.nusmp.labyrinth.loot;

import java.util.Locale;

/**
 * Why: a stat line of loot gear is one of eight things, and what each IS decides almost everything about it: which kinds of piece can roll it (a sword has an attack speed,
 * a ring has not), how big it is at a given rarity and depth (the base range, and a hard cap that no rarity or depth ever passes), the step its value is rounded to (so a
 * tooltip reads "+1.5" and not "+1.4917"), and which attribute modifier it becomes. The critical chance has no vanilla attribute, so it stays a number on the stack that the
 * critical-hit handler reads. Pure: the attribute each kind maps to is chosen by the game side, which keeps this class testable and the table in one place. Entries are only
 * ever appended (a saved stack stores the id, never the ordinal).
 */
public enum StatKind {
    /** Weapons only: a share of the headroom between the weapon's base damage and the top of its grade's damage range. */
    ATTACK_DAMAGE("attack_damage", Op.ADD, 0.08, 0.22, 0.90, 0.5, true, false, false),
    ATTACK_SPEED("attack_speed", Op.ADD, 0.04, 0.10, 0.35, 0.01, true, false, false),
    ARMOR("armor", Op.ADD, 0.5, 1.5, 8.0, 0.25, false, true, true),
    MAX_HEALTH("max_health", Op.ADD, 1.0, 2.5, 14.0, 0.5, true, true, true),
    MOVE_SPEED("move_speed", Op.MULTIPLY_BASE, 0.01, 0.03, 0.15, 0.005, true, true, true),
    CRIT_CHANCE("crit_chance", Op.CHANCE, 0.01, 0.03, 0.30, 0.005, true, true, true),
    MANA("mana", Op.MULTIPLY_TOTAL, 0.02, 0.05, 0.45, 0.005, true, true, true),
    MANA_REGEN("mana_regen", Op.MULTIPLY_TOTAL, 0.04, 0.10, 0.90, 0.01, true, true, true);

    /** How the value is applied: added, multiplied on the base, multiplied on the total, or kept as a chance. */
    public enum Op { ADD, MULTIPLY_BASE, MULTIPLY_TOTAL, CHANCE }

    private final String id;
    private final Op op;
    private final double min, max, cap, step;
    private final boolean weapon, armour, trinket;

    StatKind(String id, Op op, double min, double max, double cap, double step, boolean weapon, boolean armour, boolean trinket) {
        this.id = id;
        this.op = op;
        this.min = min;
        this.max = max;
        this.cap = cap;
        this.step = step;
        this.weapon = weapon;
        this.armour = armour;
        this.trinket = trinket;
    }

    public String id() { return id; }

    public Op op() { return op; }

    /** The hard cap on the value of one line, whatever the rarity and the depth. */
    public double cap() { return cap; }

    /** The base range of one line at depth 0, common rarity (for ATTACK_DAMAGE: the share of the headroom). */
    public double baseMin() { return min; }

    public double baseMax() { return max; }

    public double step() { return step; }

    /** Can a piece of this kind roll this stat? */
    public boolean allowedOn(GearKind kind) {
        return switch (kind) {
            case WEAPON -> weapon;
            case ARMOUR -> armour;
            case TRINKET -> trinket;
        };
    }

    /** The range one line can take at this rarity and depth: base times the rarity's magnitude times the depth factor, held to the cap. {@code {low, high}}. */
    public double[] range(LootRarity rarity, int depth) {
        double k = rarity.magnitude() * depthFactor(depth);
        return new double[]{Math.min(cap, min * k), Math.min(cap, max * k)};
    }

    /** 1 at depth 0 growing towards 1.6 on the deepest floors: {@code 1 + 0.6 (1 - e^(-depth / 150))}. */
    public static double depthFactor(int depth) { return 1.0 + 0.6 * (1.0 - Math.exp(-Math.max(0, depth) / 150.0)); }

    /** The value rounded to this stat's step. */
    public double quantize(double v) { return Math.round(v / step) * step; }

    public static StatKind fromId(String id) {
        if (id == null) return null;
        String s = id.trim().toLowerCase(Locale.ROOT);
        for (StatKind k : values()) if (k.id.equals(s)) return k;
        return null;
    }
}
