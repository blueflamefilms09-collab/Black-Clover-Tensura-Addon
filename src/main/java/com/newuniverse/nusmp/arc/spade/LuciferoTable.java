package com.newuniverse.nusmp.arc.spade;

import com.newuniverse.nusmp.entity.zogratis.MoveSpec;

import java.util.List;

/**
 * Why: every number of Lucifero's moves in one place, as {@link MoveSpec}s the scheduler can read without Minecraft, so the shape of the raid (what is
 * available at which power and distance, how long each telegraph gives the party, how the two ultimates are paced) is pinned by tests and tuned in one
 * file. {@code LuciferoBoss} turns each row into a framework {@code Move} by adding its telegraph and its code, looked up by id. The order is the order
 * of the boss's move list and must never change (the client reads move names by index); a new move is appended.
 * <p>
 * The shape of the fight: a king who barely moves and makes the room do the work. The Crushing Palm and the Falling Heavens are his answers at hand and
 * at range, the Distortion Step is how he leaves a foe that clings; The Bow, the Gravity Comb, the Gravity Grasp and the Wandering Weight are the rules
 * of the room (kneel, lanes, a hand, a well that follows you); the Gravity Shroud is his defence against what flies; Flatten the Air takes jumping and
 * gliding away for a while; the Lesser Devils are the followers he calls and swallows. Kneel Before the King (60%) and the Event Horizon (80%) are the
 * two ultimates, and once the Gates are open the four wings become arms beside his two hands: Six Limbs.
 */
public final class LuciferoTable {
    private LuciferoTable() {}

    public static final String CRUSHING_PALM = "crushing_palm", FALLING_HEAVENS = "falling_heavens", DISTORTION_STEP = "distortion_step",
            THE_BOW = "the_bow", GRAVITY_SHROUD = "gravity_shroud", LESSER_DEVILS = "lesser_devils", GRAVITY_COMB = "gravity_comb",
            GRAVITY_GRASP = "gravity_grasp", WANDERING_WEIGHT = "wandering_weight", FLATTEN_AIR = "flatten_air",
            KNEEL_BEFORE_THE_KING = "kneel_before_the_king", EVENT_HORIZON = "event_horizon", SIX_LIMBS = "six_limbs";

    /** Base damage of the moves (before the boss's damage law: power tuning, victim EP, the balance law's caps). */
    public static final float PALM = 22f, HEAVEN = 11f, LENS = 7f, BOW = 20f, COMB = 17f, COMB_PULSE = 8f, GRASP = 26f, WANDER = 3.2f, ARM = 8f,
            KNEEL = 34f, HORIZON = 36f;

    /** One row of the table. The record's accessors are the {@link MoveSpec} methods. */
    public record Spec(String id, String label, int minPower, int windUpTicks, int cooldownTicks, double minRange, double maxRange,
                       int weight, boolean ultimate, boolean crowd, int recovery) implements MoveSpec {
        /** The mana an ordinary move costs below 60% power (the same rule as the framework's Move): 6 + a sixteenth of its cooldown, at most 32; 60 for an ultimate. */
        @Override public double manaCost() { return ultimate ? 60 : Math.min(32, 6 + cooldownTicks / 16.0); }
    }

    private static final List<Spec> SPECS = List.of(
            new Spec(CRUSHING_PALM, "Crushing Palm", 20, 16, 70, 0, 6.5, 6, false, false, 10),
            new Spec(FALLING_HEAVENS, "Falling Heavens", 20, 24, 130, 6, 34, 5, false, false, 10),
            new Spec(DISTORTION_STEP, "Distortion Step", 20, 14, 180, 0, 14, 3, false, false, 8),
            new Spec(THE_BOW, "The Bow", 20, 40, 900, 0, 48, 3, false, false, 40),
            new Spec(GRAVITY_SHROUD, "Gravity Shroud", 20, 16, 480, 0, 48, 2, false, false, 10),
            new Spec(LESSER_DEVILS, "Lesser Devils", 20, 30, 700, 0, 48, 2, false, false, 10),
            new Spec(GRAVITY_COMB, "Gravity Comb", 40, 30, 260, 4, 30, 4, false, true, 12),
            new Spec(GRAVITY_GRASP, "Gravity Grasp", 40, 20, 240, 3, 18, 3, false, false, 12),
            new Spec(WANDERING_WEIGHT, "Wandering Weight", 40, 26, 300, 4, 32, 3, false, false, 12),
            new Spec(FLATTEN_AIR, "Flatten the Air", 40, 24, 420, 0, 48, 2, false, false, 8),
            new Spec(KNEEL_BEFORE_THE_KING, "Kneel Before the King", 60, 70, 1500, 0, 56, 5, true, false, 60),
            new Spec(EVENT_HORIZON, "Event Horizon", 80, 72, 1500, 0, 56, 5, true, false, 60),
            new Spec(SIX_LIMBS, "Six Limbs", 100, 34, 260, 0, 9, 5, false, false, 12));

    /** The table, in the order of the boss's move list (the client reads move names by index). */
    public static List<Spec> specs() { return SPECS; }

    public static Spec byId(String id) {
        for (Spec s : SPECS) if (s.id().equals(id)) return s;
        return null;
    }
}
