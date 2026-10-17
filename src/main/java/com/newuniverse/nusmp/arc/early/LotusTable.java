package com.newuniverse.nusmp.arc.early;

import java.util.List;

/**
 * Why: Lotus Whomalt's move table and numbers in one pure place (read by the boss class, checked by {@code EarlyTablesTest}). After the anime (the Dungeon
 * Exploration Arc): a Diamond Kingdom general who would rather nap, stays at the edge of the fight and lets his subordinates work, and fights with Smoke
 * Magic: the Smoke Garden of Usurpation (a slow weakening poison), the Smoke Binding Cross Prison, the Degenerate King's Smoke Prison and Hermit's Thick Smoke.
 * The fight is the slow burn made readable (the {@link DoseLedger}) with a sideline general who steps away when he is approached (the Smoke Burst shoves a
 * party that reaches him and he backs off); its answers are to stay out of the smoke, to break his line of sight, to catch him (his Smoke Step is on a
 * cooldown), and to break the Full Bloom before it opens.
 */
public final class LotusTable {
    private LotusTable() {}

    public static final String PUFF = "smoke_puff", GARDEN = "smoke_garden", CROSS = "cross_prison", PUPPETS = "smoke_puppets", STEP = "smoke_step",
            VEIL = "thick_smoke_veil", PULSE = "weakening_pulse", SPHERE = "degenerate_prison", BLOOM = "full_bloom", BURST = "smoke_burst";

    /** Base damage of the moves. */
    public static final float PUFF_HIT = 6f, BURST_HIT = 7f, CROSS_HIT = 8f, PUPPET_HIT = 7f, SPHERE_HIT = 12f, PULSE_PER_DOSE = 2.2f, BLOOM_PER_DOSE = 1.6f;

    /** Dose per second in a garden, in the veil and in the full bloom; and the trickle of his sight: one every {@value #AURA_PERIOD} ticks, within {@value #AURA_REACH} blocks. */
    public static final int GARDEN_GAIN = 1, VEIL_GAIN = 2, BLOOM_GAIN = 3, AURA_PERIOD = 100;
    public static final double AURA_REACH = 22;
    /** The dose falls by one every this many ticks while a foe gains nothing. */
    public static final int DECAY_PERIOD = 60;
    /** How many discs of smoke may stand at once. */
    public static final int CLOUD_CAP = 4;

    /** Radii and lifetimes (ticks) of the smoke discs. */
    public static final double BURST_RADIUS = 5.2, GARDEN_RADIUS = 7, VEIL_RADIUS = 9, BLOOM_RADIUS = 14, SPHERE_RADIUS = 3.2, CROSS_ARM = 9, CROSS_HALF = 1.4, PUPPET_RADIUS = 2.2;
    public static final int GARDEN_LIFE = 220, VEIL_LIFE = 140, BLOOM_LIFE = 180;
    /** The share of his maximum health that breaks the Full Bloom while it is wound up. */
    public static final double BLOOM_BREAK = 0.12;
    /** Ticks of stagger after a broken cast: he takes {@value #STAGGER_BONUS} times the damage and starts nothing. */
    public static final int STAGGER_TICKS = 60;
    public static final float STAGGER_BONUS = 1.25f;

    private static final List<MoveRow> ROWS = List.of(
            MoveRow.of(PUFF, "Smoke Puff", 20, 16, 60, 4, 26, 6),
            MoveRow.of(GARDEN, "Smoke Garden of Usurpation", 20, 24, 220, 6, 30, 4),
            MoveRow.of(CROSS, "Smoke Binding Cross Prison", 20, 28, 260, 4, 24, 4),
            MoveRow.crowd(PUPPETS, "Smoke Puppets", 20, 20, 200, 6, 30, 4),
            MoveRow.of(BURST, "Smoke Burst", 20, 16, 140, 0, 6, 5),
            MoveRow.of(STEP, "Smoke Step", 20, 16, 160, 0, 30, 3),
            MoveRow.crowd(VEIL, "Hermit's Thick Smoke", 40, 30, 340, 6, 30, 3),
            MoveRow.crowd(PULSE, "Weakening Bloom", 40, 26, 300, 0, 32, 3),
            MoveRow.of(SPHERE, "Degenerate King's Smoke Prison", 60, 30, 280, 3, 22, 3),
            MoveRow.ultimate(BLOOM, "Full Bloom of Usurpation", 60, 56, 1500, 0, 30, 5));

    public static List<MoveRow> rows() { return ROWS; }

    public static MoveRow byId(String id) { return MoveRow.find(ROWS, id); }

    /** Puppets of the Smoke Puppets at a power's cast slots (1 to 4): two to five. */
    public static int puppets(int castSlots) { return 1 + Math.max(1, Math.min(4, castSlots)); }
}
