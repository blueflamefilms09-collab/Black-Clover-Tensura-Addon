package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import com.newuniverse.nusmp.labyrinth.mega.enc.Band;
import com.newuniverse.nusmp.labyrinth.mega.enc.Tension;

/**
 * Why: the lockdown stops a player from walking out of a fight, but a dungeon that only locks doors can be cheated three other ways: leave the room with its monsters
 * still alive (the door was never shut, or the party out-ran them), run through the corridors so fast that the Dungeon Master never gets a roll, or teleport past a
 * whole stretch of the floor. The ambush director answers all three the way a game master would: the dungeon does not punish, it ANSWERS. Each suspicious act is a
 * signal worth some heat; heat fades by itself (a party that fights fairly never reaches the threshold); when the heat passes the threshold and the budget allows,
 * a squad is waiting in the corridor ahead, one band stronger each time the same player does it again (the Dungeon Master's own pity timer, {@link Tension}, is the
 * ladder). The budget is what keeps it fair: a cooldown, a count per window, a cap on how many of the Director's monsters may already live round the party, and the
 * hard exclusions (safe rooms, the Hub, a party that is itself locked in a room, Creative, Peaceful). All of it is arithmetic on numbers, so it is a unit test; the
 * game class {@link DynamicAmbushDirector} only gathers the facts. Pure.
 */
public final class AmbushRules {
    private AmbushRules() {}

    /** What a player did. */
    public enum Signal { LEFT_LIVE_ROOM, FAST_TRAVEL, TELEPORT, ESCAPED_LOCKDOWN }

    /**
     * The numbers. Heat is in points: {@code threshold} fires an ambush, {@code decayPerSecond} cools it, each signal adds its own. {@code speedLimit} is blocks per second
     * sustained over {@code speedWindowTicks} (a sprinting player runs about 5.6, a sprint jump 7, a speed potion II about 8); {@code teleportBlocks} is the jump in one
     * sample that is a teleport; {@code minLiveMobs} is how many live wave monsters make a room "not cleared" when its player leaves it.
     */
    public record Config(double threshold, double decayPerSecond, double leftBase, double leftPerMob, double fastPerSecond, double teleport, double escaped,
                         double speedLimit, int speedWindowTicks, double teleportBlocks, int minLiveMobs,
                         long cooldownTicks, int budgetMax, long budgetWindowTicks, int crowdCap, long strikeWindowTicks) {
        public static final Config DEFAULT = new Config(6.0, 0.25, 3.0, 0.5, 1.5, 4.0, 5.0, 8.5, 60, 24.0, 2, 1800L, 3, 12000L, 8, 12000L);

        public Config clamped() {
            return new Config(c(threshold, 1.0, 100.0), c(decayPerSecond, 0.0, 20.0), c(leftBase, 0.0, 50.0), c(leftPerMob, 0.0, 10.0), c(fastPerSecond, 0.0, 50.0),
                    c(teleport, 0.0, 50.0), c(escaped, 0.0, 50.0), c(speedLimit, 6.0, 60.0), (int) c(speedWindowTicks, 20, 400), c(teleportBlocks, 8.0, 512.0),
                    (int) c(minLiveMobs, 1, 64), (long) c(cooldownTicks, 100, 72000), (int) c(budgetMax, 1, 20), (long) c(budgetWindowTicks, 600, 240000),
                    (int) c(crowdCap, 1, 64), (long) c(strikeWindowTicks, 600, 240000));
        }

        private static double c(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
    }

    /** The heat one signal is worth. {@code liveMobs} only matters for {@link Signal#LEFT_LIVE_ROOM} (a room left with ten alive is worse than with two). */
    public static double points(Signal s, int liveMobs, Config c) {
        return switch (s) {
            case LEFT_LIVE_ROOM -> c.leftBase() + c.leftPerMob() * Math.max(0, liveMobs);
            case FAST_TRAVEL -> c.fastPerSecond();
            case TELEPORT -> c.teleport();
            case ESCAPED_LOCKDOWN -> c.escaped();
        };
    }

    /** Heat after {@code ticks} ticks of cooling. Never below 0. */
    public static double cooled(double heat, long ticks, Config c) {
        return Math.max(0.0, heat - c.decayPerSecond() * Math.max(0L, ticks) / 20.0);
    }

    /** Why an ambush did, or did not, fire. {@link #FIRE} is the only one that does. */
    public enum Why { FIRE, OFF, PEACEFUL, NOT_SURVIVAL, WRONG_PLACE, SAFE, LOCKED_IN, COOLDOWN, BUDGET, CROWDED, HEAT, NO_CELLS }

    /** Everything {@link #decide} reads. */
    public record Facts(boolean enabled, boolean peaceful, boolean survival, boolean inMega, boolean safe, boolean lockedIn, long cooldownLeft, int firedInWindow,
                        int liveNear, double heat) {}

    public static Why decide(Facts f, Config c) {
        if (!f.enabled()) return Why.OFF;
        if (f.peaceful()) return Why.PEACEFUL;
        if (!f.survival()) return Why.NOT_SURVIVAL;
        if (!f.inMega()) return Why.WRONG_PLACE;
        if (f.safe()) return Why.SAFE;
        if (f.lockedIn()) return Why.LOCKED_IN;
        if (f.heat() < c.threshold()) return Why.HEAT;
        if (f.cooldownLeft() > 0L) return Why.COOLDOWN;
        if (f.firedInWindow() >= c.budgetMax()) return Why.BUDGET;
        if (f.liveNear() >= c.crowdCap()) return Why.CROWDED;
        return Why.FIRE;
    }

    /**
     * The band of the ambush. The first one a player earns is STANDARD; each earlier one in the strike window raises it by one point of the Dungeon Master's own
     * tension ladder ({@link Tension#bonus}), and every eight floors of depth add one more; the ladder tops out at AMBUSH after the fifth, so a player who keeps
     * running meets the biggest squad the table has, and one who fights fairly never meets it.
     */
    public static Band band(int earlierStrikes, int depth) {
        int margin = 2 + Tension.bonus(Math.max(0, earlierStrikes), 1, 6) + Math.min(3, Math.max(0, depth) / 8);
        Band b = Band.ofMargin(margin);
        return b == Band.NOTHING ? Band.STANDARD : b;
    }
}
