package com.newuniverse.nusmp.entity.zogratis;

import java.util.List;

/**
 * Dante's move table (pure, unit tested): every move's numbers in one place, as {@link MoveSpec}s the scheduler can read without
 * Minecraft. {@code DanteBoss} turns each entry into a {@link Move} by adding its telegraph and its code, looked up by id.
 * <p>
 * The shape of the fight: a bruiser who wants to be close (the fist, the stomp, the leap), with Lucifero's gravity as the
 * area pressure (domain, pull, weight), compressed rock swords and stone spikes as the ranged answer to a party that keeps its
 * distance, Body Magic as the utility (surge) and the Dark Disciples as the crowd; the Gravity Singularity is the only ultimate and
 * needs 60% power, when his mana is unbound.
 */
public final class DanteTable {
    private DanteTable() {}

    public static final String ROCK_SWORDS = "rock_swords", TITAN_FIST = "titan_fist", CRUSHING_STOMP = "crushing_stomp",
            GRAVITY_DOMAIN = "gravity_domain", GRAVITY_PULL = "gravity_pull", WEIGHTED_LEAP = "weighted_leap", STONE_SPIKES = "stone_spikes",
            LUCIFERO_WEIGHT = "lucifero_weight", BODY_SURGE = "body_surge", CALL_DISCIPLES = "call_disciples",
            TENSURA_GRAVITY = "tensura_gravity", SINGULARITY = "singularity";

    /** Base damage of the moves (before the boss's damage law: power tuning, victim EP, the balance law's caps). */
    public static final float FIST = 19f, STOMP = 13f, LEAP = 15f, WEIGHT = 15f, SPIKES = 12f, SWORD = 6.5f, PULL_SLAM = 14f;

    /** One row of the table. The record's accessors are the {@link MoveSpec} methods. */
    public record Spec(String id, String label, int minPower, int windUpTicks, int cooldownTicks, double minRange, double maxRange,
                       int weight, boolean ultimate, boolean crowd, int recovery) implements MoveSpec {
        /** The mana an ordinary move costs below 60% power (the same rule as {@link Move}): 6 + a sixteenth of its cooldown, at most 32; 60 for an ultimate. */
        @Override public double manaCost() { return ultimate ? 60 : Math.min(32, 6 + cooldownTicks / 16.0); }
    }

    private static final List<Spec> SPECS = List.of(
            new Spec(ROCK_SWORDS, "Compressed Rock Swords", 20, 22, 120, 6, 34, 5, false, false, 8),
            new Spec(TITAN_FIST, "Titan Fist", 20, 14, 70, 0, 5.5, 6, false, false, 10),
            new Spec(CRUSHING_STOMP, "Crushing Stomp", 20, 20, 150, 0, 9, 4, false, true, 10),
            new Spec(GRAVITY_DOMAIN, "Lucifero's Domain", 20, 28, 280, 5, 32, 4, false, true, 12),
            new Spec(GRAVITY_PULL, "Gravity Pull", 40, 22, 220, 8, 30, 3, false, true, 14),
            new Spec(WEIGHTED_LEAP, "Weighted Leap", 20, 26, 200, 9, 40, 3, false, false, 10),
            new Spec(STONE_SPIKES, "Stone Spikes", 20, 18, 130, 4, 26, 4, false, false, 8),
            new Spec(LUCIFERO_WEIGHT, "Lucifero's Weight", 40, 24, 180, 0, 36, 3, false, false, 8),
            new Spec(BODY_SURGE, "Body Magic: Surge", 40, 20, 520, 0, 48, 1, false, false, 12),
            new Spec(CALL_DISCIPLES, "Call the Disciples", 20, 30, 700, 0, 48, 2, false, false, 10),
            new Spec(TENSURA_GRAVITY, "Gravity Magic", 20, 16, 260, 6, 30, 2, false, false, 8),
            new Spec(SINGULARITY, "Gravity Singularity", 60, 56, 1200, 0, 40, 5, true, false, 60));

    /** The table, in the order of the boss's move list (the client reads move names by index). */
    public static List<Spec> specs() { return SPECS; }

    public static Spec byId(String id) {
        for (Spec s : SPECS) if (s.id().equals(id)) return s;
        return null;
    }
}
