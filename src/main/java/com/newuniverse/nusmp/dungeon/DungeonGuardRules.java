package com.newuniverse.nusmp.dungeon;

/**
 * The rule behind "every block of the Dungeon is unbreakable" (see {@link DungeonGuard}), as plain booleans so it can be tested without the
 * game. A block state is protected unless it is exempt: air, a pure fluid, fire (a hazard that must be able to burn out), or anything at all
 * while the mod's own builders are at work (the internal depth is above zero). A change is refused when it takes a protected block away: the
 * new state is air, a fluid or fire. (Until 1.58.0 the Living Dungeon's own living fixtures, a Carcass and a veil of Mycelium, were exempt
 * too; they went with the Living Dungeon.) Pure.
 */
public final class DungeonGuardRules {
    private DungeonGuardRules() {}

    /** Game ticks between two "does not yield" lines to the same player. */
    public static final long MESSAGE_INTERVAL = 60L;

    /** Is this old state outside the guard? {@code internalDepth} above zero means the mod itself is building. */
    public static boolean exempt(int internalDepth, boolean air, boolean fluidOnly, boolean fire) {
        return internalDepth > 0 || air || fluidOnly || fire;
    }

    /** Does the guard hold this old state? */
    public static boolean isProtected(int internalDepth, boolean air, boolean fluidOnly, boolean fire) {
        return !exempt(internalDepth, air, fluidOnly, fire);
    }

    /** Would a block replaced by this new state be taken away (air, a fluid, fire)? */
    public static boolean takesAway(boolean newAir, boolean newFluid, boolean newFire) {
        return newAir || newFluid || newFire;
    }

    /** The whole decision for a block change: refused when a protected block would be replaced by air, a fluid or fire. */
    public static boolean refusesChange(int internalDepth, boolean oldAir, boolean oldFluid, boolean oldFire,
                                        boolean newAir, boolean newFluid, boolean newFire) {
        return isProtected(internalDepth, oldAir, oldFluid, oldFire) && takesAway(newAir, newFluid, newFire);
    }

    /** May a player be told again that the stone does not yield? A clock that went backwards (another world) counts as due. */
    public static boolean messageDue(long now, long last, long interval) {
        return last == 0L || now < last || now - last >= interval;
    }

    /** How deep inside the mod's own world builders the current thread is. Never goes below zero. */
    public static final class Depth {
        private int depth;

        public void begin() { depth++; }

        public void end() { if (depth > 0) depth--; }

        public int get() { return depth; }
    }
}
