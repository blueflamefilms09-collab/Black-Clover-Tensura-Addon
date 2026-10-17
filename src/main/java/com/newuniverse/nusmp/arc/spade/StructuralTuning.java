package com.newuniverse.nusmp.arc.spade;

/**
 * Why: Morris's Modification Magic rewrites the structure of things, himself included. As a fight rule that is a trade, never a free wall: he re-forms his
 * body against the kind of hit that has been hurting him most (melee, ranged or magic) and so takes less from it, and the price is that every OTHER kind
 * finds the new structure weaker. A party that keeps doing the same thing loses a third of its damage for twelve seconds; a party that switches kind
 * gains. This is the pure bookkeeping: a ledger of recent damage by kind (half a blow's weight is forgotten every {@value #HALF_LIFE} ticks, so
 * "recent" needs no list), which kind leads it, and the tuned state with its factors. Anti-magic and damage without an attacker are never tuned and never
 * recorded: modification cannot touch what anti-magic touches, and a field on the ground is nobody's blow. Nothing here knows what a block or an entity
 * is; {@code MorrisBoss} reads the numbers.
 */
public final class StructuralTuning {
    /** Ticks after which a recorded blow counts half. */
    public static final int HALF_LIFE = 100;
    /** How long a tuning lasts. */
    public static final int TUNE_TICKS = 240;
    /** The damage factor of every untuned kind while a tuning stands: the new structure has a hole where the old one was. */
    public static final float HOLE = 1.15f;
    /** The most a tuning may take off a hit: the framework's step clamp and damage budget stay in charge, this is only the kind-specific cut. */
    public static final double MAX_REDUCTION = 0.40;

    private final double[] sum = new double[3];
    private final long[] stamp = new long[3];
    private DamageKind last;
    private DamageKind tuned;
    private long until;
    private double reduction;

    private static int slot(DamageKind k) {
        return switch (k) {
            case MELEE -> 0;
            case RANGED -> 1;
            case MAGIC -> 2;
            default -> -1;
        };
    }

    private static final DamageKind[] BY_SLOT = {DamageKind.MELEE, DamageKind.RANGED, DamageKind.MAGIC};

    private double decayed(int s, long now) {
        double age = Math.max(0, now - stamp[s]);
        return sum[s] * Math.pow(0.5, age / HALF_LIFE);
    }

    /** Notes a blow that landed. Kinds that cannot be tuned are ignored. */
    public void record(long now, DamageKind kind, double amount) {
        int s = kind == null ? -1 : slot(kind);
        if (s < 0 || !(amount > 0)) return;
        sum[s] = decayed(s, now) + amount;
        stamp[s] = now;
        last = kind;
    }

    /** The recent total of all kinds, decayed to {@code now}. */
    public double recent(long now) {
        double t = 0;
        for (int s = 0; s < 3; s++) t += decayed(s, now);
        return t;
    }

    /**
     * The kind that has hurt most lately, or null when the whole recent total is under {@code minTotal} (too little to learn from). A tie goes to the
     * kind that struck last.
     */
    public DamageKind dominant(long now, double minTotal) {
        if (recent(now) < minTotal) return null;
        int best = -1;
        double top = -1;
        for (int s = 0; s < 3; s++) {
            double v = decayed(s, now);
            if (v > top + 1e-9 || (Math.abs(v - top) <= 1e-9 && BY_SLOT[s] == last)) {
                top = v;
                best = s;
            }
        }
        return best < 0 || top <= 0 ? null : BY_SLOT[best];
    }

    /** The kind that struck last (null before the first blow). */
    public DamageKind last() { return last; }

    /**
     * Tunes the body against {@code kind} for {@link #TUNE_TICKS} ticks, taking {@code cut} (0..{@link #MAX_REDUCTION}) off that kind. False for a kind
     * that cannot be tuned.
     */
    public boolean tune(DamageKind kind, long now, double cut) {
        if (kind == null || !kind.tunable()) return false;
        tuned = kind;
        until = now + TUNE_TICKS;
        reduction = Math.max(0, Math.min(MAX_REDUCTION, cut));
        return true;
    }

    public boolean active(long now) { return tuned != null && now < until; }

    /** The kind the body is tuned against right now, or null. */
    public DamageKind tuned(long now) { return active(now) ? tuned : null; }

    public long until() { return until; }

    public double reduction() { return reduction; }

    /** The factor on a hit of {@code kind}: the cut for the tuned kind, {@link #HOLE} for the other tunable kinds, 1 for everything else and for no tuning. */
    public float factor(DamageKind kind, long now) {
        if (!active(now) || kind == null || !kind.tunable()) return 1f;
        return kind == tuned ? (float) (1.0 - reduction) : HOLE;
    }

    /** Forgets the tuning (broken, expired, the fight ended). The ledger stays: it is what he will learn from next time. */
    public void drop() {
        tuned = null;
        until = 0;
        reduction = 0;
    }

    /** Forgets everything (a new fight). */
    public void reset() {
        drop();
        for (int s = 0; s < 3; s++) {
            sum[s] = 0;
            stamp[s] = 0;
        }
        last = null;
    }

    /**
     * The cut of a tuning at a stage of the power curve (0 = 20%, 4 = the Gates), from a base cut (the config, default 0.30): a little less than the base
     * at 40% power, the base at 60%, more at 80% and the Gates, never above {@link #MAX_REDUCTION}.
     */
    public static double cutFor(double base, int stage) {
        double k = 0.85 + 0.15 * (Math.max(1, Math.min(4, stage)) - 1);
        return Math.max(0, Math.min(MAX_REDUCTION, base * k));
    }
}
