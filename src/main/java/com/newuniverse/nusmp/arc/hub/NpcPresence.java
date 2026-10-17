package com.newuniverse.nusmp.arc.hub;

import java.util.List;

/**
 * Why: the friendly characters of the arcs stand in the Hub when the story has brought them there, and the pass that keeps them standing runs every five seconds
 * on a world that is saved, restarted and half unloaded at any time. Two things can go wrong with that, and both are rules rather than code: a character must
 * never be made twice (a restart must not double a crowd, and a character that wandered into a chunk the pass cannot see must not be re-made beside the post), and a
 * pass must never act on what it cannot see. {@link #wanted} says whether a character should stand (always, or no prerequisites, or at least one online player has
 * opened it); {@link #decide} turns the wish and what the pass saw round the post into exactly one act. Pure.
 */
public final class NpcPresence {
    private NpcPresence() {}

    /** What the pass does for one character. */
    public enum Act {
        /** Nothing to do, or nothing can be seen. */
        KEEP,
        /** Make one, at its post. */
        SPAWN,
        /** More than one stand at the post: remove the extras (all but the one nearest the post). */
        DEDUPE,
        /** Nobody should stand there: remove them. */
        REMOVE
    }

    /** Should this character stand in the Hub now? {@code unlockedFor} is the number of online players for whom its prerequisites are defeated. */
    public static boolean wanted(boolean alwaysThere, List<String> prerequisites, int unlockedFor) {
        return alwaysThere || prerequisites == null || prerequisites.isEmpty() || unlockedFor > 0;
    }

    /**
     * The act for one character.
     *
     * @param wanted     {@link #wanted}
     * @param seeable    the post and the ground round it (as far as the character wanders) are loaded, so what the pass counts is everything there is
     * @param standing   how many of this character the pass found within reach of the post
     */
    public static Act decide(boolean wanted, boolean seeable, int standing) {
        if (!seeable) return Act.KEEP;
        if (wanted) return standing == 0 ? Act.SPAWN : standing > 1 ? Act.DEDUPE : Act.KEEP;
        return standing > 0 ? Act.REMOVE : Act.KEEP;
    }
}
