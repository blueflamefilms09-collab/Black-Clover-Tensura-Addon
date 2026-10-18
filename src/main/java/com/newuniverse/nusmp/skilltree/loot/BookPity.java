package com.newuniverse.nusmp.skilltree.loot;

import com.newuniverse.nusmp.dungeon.sentient.Dice;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntPredicate;

/**
 * Why: a route book of a line the player already owns is only worth its Skill Points, and getting one twice in a row from a rare drop feels like the game ignoring you. The old loot code
 * had no notion of this, so the rule is simple and lives here: remember, per player, whether the LAST route book handed to them was of a line they already owned; if it was, the NEXT
 * route book is drawn again (the same weighted draw, up to {@code tries} times, then a scan: {@link WeightedTable#pickAvoiding}) until it is a line they do not own. So an owned
 * line can drop, but never twice in a row, and a player who owns every line is simply handed what the plain draw gave. An unowned drop clears the memory. The state is one
 * boolean per online player in memory (a restart forgets it, which only means one more chance of a duplicate; nothing is saved to the player, so a datapack removal or a player
 * file never carries a stale flag), dropped on logout and on server stop by {@link LinesLoot}. Pure apart from the UUID: ownership is a predicate the caller supplies, so the
 * rule is unit tested without a player, a registry or a world.
 */
public final class BookPity {
    /** The one instance the game uses. */
    public static final BookPity GLOBAL = new BookPity();

    private final ConcurrentHashMap<UUID, Boolean> lastWasOwned = new ConcurrentHashMap<>();

    /** True when the player's previous route book was of a line they already owned. */
    public boolean avoidOwned(UUID player) { return player != null && lastWasOwned.containsKey(player); }

    /** Records what the book just handed out was: owned (the next one avoids owned lines) or not (the memory is cleared). */
    public void record(UUID player, boolean wasOwned) {
        if (player == null) return;
        if (wasOwned) lastWasOwned.put(player, Boolean.TRUE);
        else lastWasOwned.remove(player);
    }

    public void forget(UUID player) { if (player != null) lastWasOwned.remove(player); }

    public void clear() { lastWasOwned.clear(); }

    public int size() { return lastWasOwned.size(); }

    /**
     * One draw for a player: the plain weighted pick, or (after an owned drop) a pick that avoids owned lines; then the memory is updated with whether the result is owned.
     *
     * @param owned  does the player own the line at this index of the table's original array
     * @param player the player, or null for a drop with no player (then no memory is read or written)
     * @return the index, or -1 when the table is empty
     */
    public int pick(WeightedTable table, Dice dice, UUID player, IntPredicate owned, int tries) {
        if (player == null || owned == null) return table.pick(dice);
        int pick = avoidOwned(player) ? table.pickAvoiding(dice, owned, tries) : table.pick(dice);
        if (pick >= 0) record(player, owned.test(pick));
        return pick;
    }
}
