package com.newuniverse.nusmp.book;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Modification Magic (Morris Libardirt), 1.93.0 rebuild: the pure numbers and ids of the book and the adaptation ledger of Re-Structure (no Minecraft types: ModificationRulesTest).
 * The five generic page ids (attribute_bolt ... attribute_guard) keep their order and mastery bits; five new pages are appended after them.
 */
public final class ModificationRules {
    private ModificationRules() {}

    public static final String BOLT = "attribute_bolt", CUT = "attribute_cut", FIELD = "attribute_field", DOMAIN = "attribute_domain", GUARD = "attribute_guard";
    public static final String THREAD = "specimen_thread", REWRITE = "spell_rewrite", SUBJECTS = "test_subjects", THEATRE = "dissection_theatre", FRAME = "dissector_frame";
    public static final List<String> OLD_IDS = List.of(BOLT, CUT, FIELD, DOMAIN, GUARD);
    public static final List<String> NEW_IDS = List.of(THREAD, REWRITE, SUBJECTS, THEATRE, FRAME);

    public static final int FRAME_TICKS = 400, GUARD_TICKS = 180, SHORT_ARMOUR_TICKS = 100, SUBJECT_TICKS = 600, THREAD_TICKS = 140, REWRITE_TICKS = 80, THEATRE_TICKS = 100;
    public static final int ADAPT_HALF_LIFE = 100;

    /** The kinds of damage the body can be re-structured against (the same three as the Spade boss's ledger). */
    public enum Kind { MELEE, RANGED, MAGIC }

    /** Needles in the fan of Dissection Needles: 3, plus one for every two EP modifier steps (0..4), so 3 to 5. */
    public static int needleCount(int modifier) { return 3 + Math.max(0, Math.min(4, modifier)) / 2; }

    /** Tendrils of Clawed Tendrils: 2 to 4. */
    public static int tendrilCount(int modifier) { return 2 + Math.max(0, Math.min(4, modifier)) / 2; }

    /** Test subjects: 2, or 3 from a mighty caster (modifier 3). */
    public static int subjectCount(int modifier) { return modifier >= 3 ? 3 : 2; }

    /** The cut Re-Structure gives against the kind that has been hurting: 25 % at 1,000 EP, 2 % more per EP step, never above 40 %. */
    public static float adaptCut(double epScale) { return (float) Math.max(0.25, Math.min(0.40, 0.25 + 0.02 * (epScale - 1))); }

    /** Every other kind finds a hole, as in the Spade boss's tuning: a small penalty for the caster who relies on the guard alone. */
    public static final float OTHER_KIND_PENALTY = 1.1f;

    /** The Dissector's Frame fires one needle at the nearest foe every this many ticks (a mighty caster fires faster). */
    public static int autoNeedleInterval(int modifier) { return Math.max(24, 44 - 5 * Math.max(0, Math.min(4, modifier))); }

    /** The pull of the Specimen Thread per tick: 0.03 growing to 0.10 over the thread's life, always under a walker's 0.216 (a specimen can always walk away). */
    public static double threadPull(int age, int life) { return 0.03 + 0.07 * Math.max(0, Math.min(1, age / (double) Math.max(1, life))); }

    /** The radius of the Dissection Theatre's ring at tick 'age' of 'life': it closes from 8 to 3 blocks, slower than a walk. */
    public static double theatreRadius(int age, int life) { return 8.0 - 5.0 * Math.max(0, Math.min(1, age / (double) Math.max(1, life))); }

    /**
     * The ledger of Re-Structure: every blow that lands is noted by kind with a half-life of {@link #ADAPT_HALF_LIFE} ticks, so "recent" needs no list. The kind that leads (the largest
     * decayed total) is the one the body has adapted to; asked for the multiplier of a blow, the ledger answers (1 - cut) for the leading kind, {@link #OTHER_KIND_PENALTY} for the
     * other kinds once something leads, 1 when nothing has been noted. Pure, one per entity.
     */
    public static final class Ledger {
        private final Map<Kind, double[]> notes = new EnumMap<>(Kind.class);     // kind -> {total, tick}

        public void note(Kind kind, double amount, long tick) {
            if (amount <= 0) return;
            double[] n = notes.computeIfAbsent(kind, k -> new double[]{0, tick});
            n[0] = decayed(n, tick) + amount;
            n[1] = tick;
        }

        private static double decayed(double[] n, long tick) {
            double dt = Math.max(0, tick - n[1]);
            return n[0] * Math.pow(0.5, dt / ADAPT_HALF_LIFE);
        }

        public double total(Kind kind, long tick) {
            double[] n = notes.get(kind);
            return n == null ? 0 : decayed(n, tick);
        }

        /** The kind that leads, or null when the ledger is empty (under 0.5 of damage in all). */
        public Kind leading(long tick) {
            Kind best = null;
            double top = 0.5;
            for (Kind k : Kind.values()) {
                double t = total(k, tick);
                if (t > top) { top = t; best = k; }
            }
            return best;
        }

        /** The multiplier on a blow of this kind: (1 - cut) against the leading kind, the penalty against the others, 1 when nothing leads. */
        public float multiplier(Kind kind, long tick, float cut) {
            Kind lead = leading(tick);
            if (lead == null) return 1f;
            return lead == kind ? 1f - cut : OTHER_KIND_PENALTY;
        }

        public void clear() { notes.clear(); }
    }
}
