package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.nexus.zone.HiveTactics.Kind;

/**
 * The numbers of the swarm of the Tempest Mirage that are not tactics: what each kind of member is made of (the mutation), what it is
 * called, which Tensura creature it is, and the budget that keeps the whole hive's blows on one player from ever being a stun-lock. Pure.
 */
public final class HiveRules {
    private HiveRules() {}

    /** The most a mutant's health and attack damage may come to, whatever it was before: the mutation is finite. */
    public static final double MAX_HEALTH = 160.0, MAX_ATTACK = 14.0;
    /** A blow of the swarm takes at most this share of the victim's maximum health, and all the blows of one second together at most the second share. */
    public static final double HIT_SHARE = 0.10, WINDOW_SHARE = 0.20;
    public static final int WINDOW = 20;
    /** The smallest cap on a single blow, so a very frail victim is not made immune. */
    public static final double MIN_HIT = 1.0;

    /** What a mutant is made of, as factors on what its kind had: health, attack damage, speed, size. */
    public record Stats(double health, double attack, double speed, double scale) {}

    /** The mutation of a kind; {@code roll} (0 to 1) varies it a little from one individual to the next (plus or minus 8 per cent). */
    public static Stats mutate(Kind kind, double roll) {
        double v = 0.92 + 0.16 * Math.max(0.0, Math.min(1.0, roll));
        return switch (kind) {
            case ORC -> new Stats(1.8 * v, 1.25, 0.95, 1.15);
            case LIZARDMAN -> new Stats(1.6 * v, 1.2, 1.08, 1.10);
            case SLIME -> new Stats(2.2 * v, 1.1, 1.0, 1.25);
            case GOBLIN -> new Stats(2.0 * v, 1.2, 1.12, 1.0);
            case HOUND -> new Stats(1.7 * v, 1.2, 1.12, 1.1);
        };
    }

    /** The new total of an attribute that had `base`: base times the factor, never below what it was and never above `cap`. */
    public static double total(double base, double factor, double cap) { return Math.max(base, Math.min(cap, base * factor)); }

    /** The registry id of the Tensura creature of a kind. */
    public static String typeId(Kind kind) {
        return switch (kind) {
            case ORC -> "tensura:orc";
            case LIZARDMAN -> "tensura:lizardman";
            case SLIME -> "tensura:slime";
            case GOBLIN -> "tensura:goblin";
            case HOUND -> "tensura:hound_dog";
        };
    }

    public static String name(Kind kind) {
        return switch (kind) {
            case ORC -> "Mutated Orc";
            case LIZARDMAN -> "Mutated Lizardman";
            case SLIME -> "Mutated Slime";
            case GOBLIN -> "Mutated Goblin";
            case HOUND -> "Mutated Hound Dog";
        };
    }

    /** The kind with this registry id, or null. */
    public static Kind kindOf(String id) {
        for (Kind k : Kind.values()) if (typeId(k).equals(id)) return k;
        return null;
    }

    /** How much one player counts for against the hive: 1 for twenty health and no armour, up to 4 for a heavy, well armoured one. */
    public static double power(double maxHealth, double armour) {
        double p = 0.5 + maxHealth / 40.0 + armour / 40.0;
        return Math.max(0.6, Math.min(4.0, p));
    }

    /** One victim's budget of hive damage: a cap on each blow, and on the blows of any one second (a sliding window of {@link #WINDOW} ticks). */
    public static final class Budget {
        private final java.util.ArrayDeque<double[]> recent = new java.util.ArrayDeque<>();

        /** How much of this blow lands: the blow capped, then what the last second still has room for (0 if none). */
        public double take(long tick, double amount, double maxHealth) {
            if (amount <= 0) return 0.0;
            if (!recent.isEmpty() && tick < recent.peekLast()[0]) recent.clear();                  // the clock went back: a stale budget is not carried over
            while (!recent.isEmpty() && tick - recent.peekFirst()[0] >= WINDOW) recent.pollFirst();
            double spent = 0.0;
            for (double[] blow : recent) spent += blow[1];
            double cap = Math.min(amount, Math.max(MIN_HIT, HIT_SHARE * maxHealth));
            double allowed = Math.min(cap, Math.max(0.0, WINDOW_SHARE * maxHealth - spent));
            if (allowed > 1e-6) recent.addLast(new double[]{tick, allowed});
            else allowed = 0.0;
            return allowed;
        }
    }
}
