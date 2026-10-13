package com.newuniverse.nusmp.dungeon.sentient;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

/**
 * When the Dungeon looks at the room a player stands in, and what comes of the look: nothing if the room was dealt with already, a wait if
 * the player drew a birth a moment ago or too many born mobs are about, nothing for a floor that births no mobs, the roll and the births
 * otherwise. The costly questions (is a collapse under way here, how many born mobs are near) are asked only if the cheap ones leave the
 * room standing. Pure; {@link DungeonRoomManager} puts the answers into the world.
 */
public final class RoomGate {
    private RoomGate() {}

    /** The least ticks between two room births drawn by one player, and how long to wait before looking again when the walls are busy. */
    public static final int GAP = 200, RETRY = 40;
    /** How many times a populated room is looked at again when no wall of it is the right distance from the player, and the ticks between those looks. */
    public static final int MAX_TRIES = 3, TRY_AGAIN = 60;
    private static final int TICKS_PER_MINUTE = 1200;

    /** What to do about the room now: look no further (SKIP), call it dealt with (HANDLED), look again at {@code nextTry} (WAIT), or go into it (ENTER). */
    public enum Verdict { SKIP, HANDLED, WAIT, ENTER }

    public record Decision(Verdict verdict, long nextTry) {
        static final Decision SKIP = new Decision(Verdict.SKIP, 0L);
        static final Decision HANDLED = new Decision(Verdict.HANDLED, 0L);
        static final Decision ENTER = new Decision(Verdict.ENTER, 0L);

        static Decision waitUntil(long until) { return new Decision(Verdict.WAIT, until); }
    }

    /** What came of going into a room: nothing more to do there, look again soon, no wall is the right distance away (a few more looks), or the births are scheduled. */
    public enum Result { DONE, LATER, NOWHERE, BIRTHED }

    /**
     * @param handled   the room the player stood in when it was last dealt with
     * @param nextTry   the game time before which the Dungeon does not look again
     * @param suppressed asked only when everything cheaper has passed: is a collapse under way in this room
     * @param bornNear   asked only then too: how many born mobs (and births on their way) are within 64 blocks of the player
     */
    public static Decision decide(long room, long handled, long now, long nextTry, boolean floorBirths, long lastBirth,
                                  BooleanSupplier suppressed, IntSupplier bornNear, int cap) {
        if (room == handled || now < nextTry) return Decision.SKIP;
        if (!floorBirths) return Decision.HANDLED;
        if (now - lastBirth < GAP) return Decision.waitUntil(lastBirth + GAP);
        if (suppressed.getAsBoolean()) return Decision.HANDLED;
        if (bornNear.getAsInt() >= cap) return Decision.waitUntil(now + RETRY);
        return Decision.ENTER;
    }

    /** What the player's state becomes after a go at a room: {@code handled}, the time before which not to look, and whether a birth was drawn. */
    public record After(boolean handled, long nextTry, boolean birthed, int tries) {}

    public static After after(Result result, int tries, long now) {
        return switch (result) {
            case BIRTHED -> new After(true, 0L, true, tries);
            case LATER -> new After(false, now + RETRY, false, tries);
            case NOWHERE -> tries + 1 >= MAX_TRIES ? new After(true, 0L, false, tries + 1) : new After(false, now + TRY_AGAIN, false, tries + 1);
            default -> new After(true, 0L, false, tries);
        };
    }

    /**
     * Is a populated room due to birth again: the config's minutes have passed since its last birth, a player is inside, and none of its
     * mobs is left alive ({@code living} is asked only if the time has passed)? 0 minutes = rooms never refill.
     */
    public static boolean rebirthDue(long lastBirth, int minutes, long now, IntSupplier living) {
        return minutes > 0 && now - lastBirth >= (long) minutes * TICKS_PER_MINUTE && living.getAsInt() == 0;
    }
}
