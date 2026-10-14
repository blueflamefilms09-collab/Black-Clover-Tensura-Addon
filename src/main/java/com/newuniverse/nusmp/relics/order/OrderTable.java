package com.newuniverse.nusmp.relics.order;

import java.util.List;

/**
 * The numbers of the God of Order (pure, unit tested): its body, the moves of its fight and where it stands. The God is not a boss to farm: it
 * comes to take a duplicate, and it only fights the one who raises a hand against it. Hits go through the Zogratis framework's damage law, which
 * caps one hit at the pvp share of a player's health, so these are bases, not final damage.
 */
public final class OrderTable {
    private OrderTable() {}

    public static final double HEALTH = 1500, ATTACK = 16, SPEED = 0.30, ARMOR = 11, TOUGHNESS = 6;
    /** Experience it gives when it dies (no loot of any kind). */
    public static final int XP = 400;

    // ---- the four moves of the fight
    public static final String DECREE = "decree", WRIT = "writ", JUDGEMENT = "judgement", ERASURE = "erasure";

    /** One move's numbers (the entity builds a framework Move from it). */
    public record Spec(String id, String label, int minPower, int windUp, int cooldown, double minRange, double maxRange, int weight,
                       boolean ultimate, int recovery) {}

    private static final List<Spec> SPECS = List.of(
            new Spec(DECREE, "Decree of Binding", 20, 28, 160, 0, 30, 4, false, 10),
            new Spec(WRIT, "Writ of Light", 20, 24, 120, 3, 26, 4, false, 10),
            new Spec(JUDGEMENT, "Circle of Judgement", 40, 40, 240, 0, 28, 3, false, 14),
            new Spec(ERASURE, "Erasure", 20, 60, 2_400, 0, 40, 1, true, 20));

    public static List<Spec> specs() { return SPECS; }

    // ---- Decree: a ring of binding glyphs that slows
    public static final double DECREE_RADIUS = 5.0;
    public static final float DECREE_DAMAGE = 5f;
    public static final int DECREE_SLOW_TICKS = 80, DECREE_SLOW_AMP = 2;

    // ---- Writ: a line of light
    public static final double WRIT_LENGTH = 20.0, WRIT_HALF_WIDTH = 1.4;
    public static final float WRIT_DAMAGE = 12f;

    // ---- Judgement: a marked circle, one heavy bounded hit
    public static final double JUDGEMENT_RADIUS = 3.5;
    public static final float JUDGEMENT_DAMAGE = 20f;

    // ---- Erasure: it takes the duplicates of whoever fought it, and leaves
    public static final double ERASURE_REACH = 40.0;
    /** The fight is cut short by an Erasure after this many ticks of combat (75 s), or when its health is under the fraction below. */
    public static final long ERASURE_AFTER_TICKS = 1_500L;
    public static final float ERASURE_HEALTH_FRACTION = 0.35f;
    public static final int ERASURE_TELL_TICKS = 60;

    public static boolean forcedErasureDue(long combatTicks, float healthFraction) {
        return combatTicks >= ERASURE_AFTER_TICKS || healthFraction <= ERASURE_HEALTH_FRACTION;
    }

    /**
     * 1.35.0: the same rule with the Open Ledger in mind. The God used to end the matter at 35% health, which is above the 25% at which its Gates
     * (the 100% mode, with the refilled health bar) open, so the Gates could hardly ever be seen. Now it ends the matter by time (75 s of combat,
     * {@link #GATES_EXTRA_TICKS} more once the ledger is open) or, with the ledger open, when it is down to {@link #GATES_ERASURE_FRACTION} of the
     * refilled bar. Before the ledger opens, health alone never ends the matter.
     */
    public static boolean forcedErasureDue(long combatTicks, float healthFraction, boolean gatesOpen) {
        long limit = ERASURE_AFTER_TICKS + (gatesOpen ? GATES_EXTRA_TICKS : 0);
        return combatTicks >= limit || (gatesOpen && healthFraction <= GATES_ERASURE_FRACTION);
    }

    public static final long GATES_EXTRA_TICKS = 600L;
    public static final float GATES_ERASURE_FRACTION = 0.15f;

    /** 1.35.0, the Open Ledger: the Decree's ring and the Judgement's circle are wider (the wind-up draws them so). */
    public static double decreeRadius(boolean gates) { return gates ? DECREE_RADIUS * 1.3 : DECREE_RADIUS; }

    public static double judgementRadius(boolean gates) { return gates ? JUDGEMENT_RADIUS * 1.25 : JUDGEMENT_RADIUS; }

    // ---- where it appears
    /** Distances from the holder, in blocks, tried in this order for a standing spot (the first is the 8 blocks of the design). */
    public static final double[] SPAWN_RADII = {8, 7, 9, 6, 10, 5, 4, 3};
    public static final int SPAWN_ANGLES = 12;
    /** The witnesses of a visit: players this close to the spot hear it and may be named. */
    public static final double WITNESS_RADIUS = 48.0;
    /** How far from the God the holder of the duplicate may stray before the visit is called off (blocks). */
    public static final double HOLDER_LEASH = 64.0;
    /** The waiting list holds this many relic ids. */
    public static final int QUEUE_CAPACITY = 16;
    /** A God that belongs to no visit (a spawn egg, a summon) leaves after this long without a fight (ten minutes). */
    public static final int FREE_LIFETIME_TICKS = 12_000;
}
