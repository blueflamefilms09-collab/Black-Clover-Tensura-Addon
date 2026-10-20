package com.newuniverse.nusmp.labyrinth.mega.rift;

/**
 * Why: the decisions behind the release lever of the hidden realms and the exclusive tombs, as plain numbers so they are unit-tested without the game (1.101.0, docs/labyrinth/pocket_fix.md). A realm's
 * vault (iron bars) and a tomb's treasury (the seal) each have a LEVER in a chamber that is not behind the door it opens. The lever answers at once when the door's own challenge is done (the secret boss
 * is dead, the plates were pressed in order); before that it is stiff for {@code unlockTicks} after the realm formed, and the door opens by itself at {@code autoTicks} while somebody is inside, so a delver
 * is never stuck behind a boss that cannot be beaten or a puzzle that cannot be read. The exit rift is always there as well. Pure.
 */
public final class PocketSwitchRules {
    private PocketSwitchRules() {}

    /** The least gap between the moment the lever works and the moment the door opens by itself, in ticks. */
    public static final long AUTO_GAP_TICKS = 20L * 30L;

    /** What the lever does when it is pulled now. */
    public enum State {
        /** Stiff: the door's challenge is not done and the delay has not run out. */
        LOCKED,
        /** It works: pulling it opens the door. */
        READY,
        /** The door is open already; the lever only clicks. */
        SPENT
    }

    /** The state of the lever: {@code opened} is the door's own state, {@code cleared} the challenge (boss dead / puzzle solved), {@code age} the ticks since the realm formed. */
    public static State state(boolean opened, boolean cleared, long age, long unlockTicks) {
        if (opened) return State.SPENT;
        return cleared || age >= unlockTicks ? State.READY : State.LOCKED;
    }

    /** The tick at which the door opens by itself: the configured one, but never sooner than {@link #AUTO_GAP_TICKS} after the lever works. */
    public static long autoAt(long unlockTicks, long autoTicks) { return Math.max(autoTicks, unlockTicks + AUTO_GAP_TICKS); }

    /** Does the door open by itself now? Only while somebody is inside (an empty realm just collapses on its own clock). */
    public static boolean autoOpens(boolean opened, long age, long unlockTicks, long autoTicks, boolean occupied) {
        return !opened && occupied && age >= autoAt(unlockTicks, autoTicks);
    }

    /** Whole seconds until a stiff lever gives (0 when it works already). */
    public static int secondsToUnlock(boolean cleared, long age, long unlockTicks) {
        if (cleared || age >= unlockTicks) return 0;
        return (int) ((unlockTicks - age + 19) / 20);
    }

    /** Whole seconds until the door opens by itself (0 when it does or has). */
    public static int secondsToAuto(long age, long unlockTicks, long autoTicks) {
        long at = autoAt(unlockTicks, autoTicks);
        return age >= at ? 0 : (int) ((at - age + 19) / 20);
    }

    /** Is the lever delay shorter than the pocket's life, so the door can open before the realm collapses? */
    public static boolean fits(long lifeTicks, long unlockTicks, long autoTicks) { return autoAt(unlockTicks, autoTicks) < lifeTicks; }
}
