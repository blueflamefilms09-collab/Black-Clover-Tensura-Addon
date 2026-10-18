package com.newuniverse.nusmp.labyrinth.mega.cyclic;

/**
 * Why: every number of the cyclic pass is a trade-off the owner may want to move (how many dead ends are tolerable, how long a transitional hallway may be, how
 * many loops are enough), so they live in ONE immutable value that the game reads from {@code generation.cyclic.*} in the server config, the tests build with
 * {@link #defaults()} and the doc explains line by line. Pure (no config system here): the adapter copies the config values in.
 *
 * @param deadEndTarget    the most dead ends, as a share of all rooms, the pass tolerates after it ran (0.08 = 8 %); terminals and required rooms do not count (see {@link Roles})
 * @param cycleTarget      once the dead ends are under the target, loops are added until this share of rooms lies on a cycle (0 = never add a loop that fixes no dead end)
 * @param maxHallway       the longest transitional hallway, in module cells (0 = only doorways that already line up, never a hallway room)
 * @param hallwayShare     hallway rooms may be at most this share of all rooms (the Transition share of the dungeon must not be swamped by them)
 * @param maxLinks         the most links (doorways or hallways) the pass adds to one floor
 * @param minCycleFix      a link that cures a dead end must close a loop of at least this many doorways (4 = no triangles)
 * @param minCycle         a link added only for the sake of a loop must close at least this many doorways (6 = a real detour, not a shortcut round a corner)
 * @param terminalMask     roles that are not dead ends ({@link Roles#TERMINAL})
 * @param lockedMask       roles into which no doorway may be cut ({@link Roles#LOCKED})
 * @param protectedMask    roles the pruner may not remove ({@link Roles#PROTECTED})
 * @param prune            may the pass remove dangling rooms that no loop can cure (never a terminal, never a required room)
 * @param overgrow         the generator may place this many times its room budget before the pass trims it (the dangling rooms are the first to go)
 * @param lookahead        how many fitting candidates the placement step compares by {@link SocketRules#placementScore} (1 = take the first, the old behaviour)
 * @param balanceWeight    how strongly a placement and a prune are nudged towards the target category shares (0 = not at all)
 */
public record LoopConfig(double deadEndTarget, double cycleTarget, int maxHallway, double hallwayShare, int maxLinks, int minCycleFix, int minCycle, int terminalMask,
                         int lockedMask, int protectedMask, boolean prune, double overgrow, int lookahead, double balanceWeight) {

    public LoopConfig {
        deadEndTarget = clamp(deadEndTarget, 0.0, 1.0);
        cycleTarget = clamp(cycleTarget, 0.0, 1.0);
        maxHallway = Math.max(0, Math.min(6, maxHallway));
        hallwayShare = clamp(hallwayShare, 0.0, 1.0);
        maxLinks = Math.max(0, maxLinks);
        minCycleFix = Math.max(3, minCycleFix);
        minCycle = Math.max(3, minCycle);
        overgrow = Math.max(1.0, Math.min(2.0, overgrow));
        lookahead = Math.max(1, Math.min(32, lookahead));
        balanceWeight = Math.max(0.0, balanceWeight);
    }

    /** The shipped defaults: at most 8 % dead ends, hallways of up to 3 modules, loops until 60 % of the rooms lie on a cycle. */
    public static LoopConfig defaults() {
        return new LoopConfig(0.08, 0.60, 3, 0.20, 16, 4, 6, Roles.TERMINAL, Roles.LOCKED, Roles.PROTECTED, true, 1.25, 6, 1.0);
    }

    public LoopConfig withDeadEndTarget(double v) { return new LoopConfig(v, cycleTarget, maxHallway, hallwayShare, maxLinks, minCycleFix, minCycle, terminalMask, lockedMask, protectedMask, prune, overgrow, lookahead, balanceWeight); }

    public LoopConfig withCycleTarget(double v) { return new LoopConfig(deadEndTarget, v, maxHallway, hallwayShare, maxLinks, minCycleFix, minCycle, terminalMask, lockedMask, protectedMask, prune, overgrow, lookahead, balanceWeight); }

    public LoopConfig withMaxHallway(int v) { return new LoopConfig(deadEndTarget, cycleTarget, v, hallwayShare, maxLinks, minCycleFix, minCycle, terminalMask, lockedMask, protectedMask, prune, overgrow, lookahead, balanceWeight); }

    public LoopConfig withHallwayShare(double v) { return new LoopConfig(deadEndTarget, cycleTarget, maxHallway, v, maxLinks, minCycleFix, minCycle, terminalMask, lockedMask, protectedMask, prune, overgrow, lookahead, balanceWeight); }

    public LoopConfig withMaxLinks(int v) { return new LoopConfig(deadEndTarget, cycleTarget, maxHallway, hallwayShare, v, minCycleFix, minCycle, terminalMask, lockedMask, protectedMask, prune, overgrow, lookahead, balanceWeight); }

    public LoopConfig withPrune(boolean v) { return new LoopConfig(deadEndTarget, cycleTarget, maxHallway, hallwayShare, maxLinks, minCycleFix, minCycle, terminalMask, lockedMask, protectedMask, v, overgrow, lookahead, balanceWeight); }

    public LoopConfig withOvergrow(double v) { return new LoopConfig(deadEndTarget, cycleTarget, maxHallway, hallwayShare, maxLinks, minCycleFix, minCycle, terminalMask, lockedMask, protectedMask, prune, v, lookahead, balanceWeight); }

    public LoopConfig withLookahead(int v) { return new LoopConfig(deadEndTarget, cycleTarget, maxHallway, hallwayShare, maxLinks, minCycleFix, minCycle, terminalMask, lockedMask, protectedMask, prune, overgrow, v, balanceWeight); }

    public LoopConfig withBalanceWeight(double v) { return new LoopConfig(deadEndTarget, cycleTarget, maxHallway, hallwayShare, maxLinks, minCycleFix, minCycle, terminalMask, lockedMask, protectedMask, prune, overgrow, lookahead, v); }

    private static double clamp(double v, double lo, double hi) { return Double.isNaN(v) ? lo : Math.max(lo, Math.min(hi, v)); }
}
