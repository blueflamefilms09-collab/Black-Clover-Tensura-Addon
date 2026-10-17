package com.newuniverse.nusmp.arc.story;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcSaga;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Why: the story chain of the owner's brief ("a boss is spawnable at its arena only when its prerequisites have been defeated") is a handful of rules
 * that every part of the mod must agree on: the arena that refuses a summoning, the command that lists what is open, the dialogue that reacts to the
 * stage, the unlock command. They are pure functions over {@link StoryData} and the roster, so one set of tests proves them and nothing can drift.
 *
 * <p>A prerequisite that is not in the roster (a character another builder has not merged yet, or one removed later) is treated as satisfied, because
 * the alternative is a chain that can never be opened by anybody; the predicate {@code exists} says which ids are real.
 */
public final class StoryRules {
    private StoryRules() {}

    /** The story stage of a player: how many bosses of the roster they have defeated. */
    public static int stage(StoryData d, Collection<ArcCharacter> roster) {
        int n = 0;
        for (ArcCharacter c : roster) if (c.isBoss() && d.hasDefeated(c.id())) n++;
        return n;
    }

    /**
     * The saga a player is in: the earliest saga (by its order in the story) that still has a boss they have not defeated; once every boss is down, the
     * last saga that has bosses; with no bosses at all, the first saga.
     */
    public static ArcSaga currentSaga(StoryData d, Collection<ArcCharacter> roster) {
        ArcSaga earliestOpen = null, last = null;
        for (ArcCharacter c : roster) {
            if (!c.isBoss()) continue;
            if (last == null || c.saga().order() > last.order()) last = c.saga();
            if (!d.hasDefeated(c.id()) && (earliestOpen == null || c.saga().order() < earliestOpen.order())) earliestOpen = c.saga();
        }
        if (earliestOpen != null) return earliestOpen;
        return last != null ? last : ArcSaga.ELF_REINCARNATION;
    }

    /** The prerequisites of {@code c} the player has not defeated yet, in the order the character lists them (unregistered ids are skipped). */
    public static List<String> missing(StoryData d, ArcCharacter c, Predicate<String> exists) {
        List<String> out = new ArrayList<>();
        for (String p : c.prerequisites()) if (exists.test(p) && !d.hasDefeated(p)) out.add(p);
        return out;
    }

    /** Are all the prerequisites of {@code c} defeated? (True for a character with none.) */
    public static boolean unlocked(StoryData d, ArcCharacter c, Predicate<String> exists) {
        return missing(d, c, exists).isEmpty();
    }

    /** Is the boss open to be summoned? The gate being off opens everything; otherwise at least one of the players present must have it unlocked. */
    public static boolean gateOpen(boolean gateOn, boolean anyPlayerUnlocked) {
        return !gateOn || anyPlayerUnlocked;
    }

    /** The bosses the player can fight now and has not yet beaten, in roster order. */
    public static List<ArcCharacter> available(StoryData d, Collection<ArcCharacter> roster, Predicate<String> exists) {
        List<ArcCharacter> out = new ArrayList<>();
        for (ArcCharacter c : roster) if (c.isBoss() && !d.hasDefeated(c.id()) && unlocked(d, c, exists)) out.add(c);
        return out;
    }

    /** The bosses that became available between two states of the same player (what a victory opened). */
    public static List<ArcCharacter> newlyAvailable(StoryData before, StoryData after, Collection<ArcCharacter> roster, Predicate<String> exists) {
        Set<String> was = new LinkedHashSet<>();
        for (ArcCharacter c : available(before, roster, exists)) was.add(c.id());
        List<ArcCharacter> out = new ArrayList<>();
        for (ArcCharacter c : available(after, roster, exists)) if (!was.contains(c.id())) out.add(c);
        return out;
    }

    /**
     * Every character that must fall before {@code c} (its prerequisites, theirs, and so on), earliest first, without {@code c} itself. Safe against a
     * cycle in the data: a character already seen is not followed again. Used by the unlock command.
     */
    public static List<String> closure(ArcCharacter c, Function<String, ArcCharacter> lookup) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        Set<String> seen = new LinkedHashSet<>();
        seen.add(c.id());
        collect(c, lookup, out, seen);
        out.remove(c.id());
        return new ArrayList<>(out);
    }

    /** Is {@code c} among its own ancestors (a loop in the prerequisites, which would make the chain impossible to open)? */
    public static boolean cyclic(ArcCharacter c, Function<String, ArcCharacter> lookup) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> todo = new ArrayList<>(c.prerequisites());
        while (!todo.isEmpty()) {
            String id = todo.remove(todo.size() - 1);
            if (id.equals(c.id())) return true;
            if (!seen.add(id)) continue;
            ArcCharacter p = lookup.apply(id);
            if (p != null) todo.addAll(p.prerequisites());
        }
        return false;
    }

    private static void collect(ArcCharacter c, Function<String, ArcCharacter> lookup, LinkedHashSet<String> out, Set<String> seen) {
        for (String p : c.prerequisites()) {
            ArcCharacter pc = lookup.apply(p);
            if (pc == null || !seen.add(pc.id())) continue;
            collect(pc, lookup, out, seen);
            out.add(pc.id());
        }
    }
}
