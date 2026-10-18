package com.newuniverse.nusmp.client.skilltree.layout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: this is the client half of the fog of war, and the rule for "which Unique Skill Lines does Tensura's list show". The server only teaches a player the stand-ins of the lines they own, which
 * already hides every other line in a multiplayer world; but a single-player world, a stand-in that arrived some other way, and the operators' ADMIN INSPECT mode all need the SCREEN to decide too, and
 * the decision has to be the same everywhere. So it is one pure function: the lines to list are the lines the player owns, plus every line when (and only when) the player may inspect AND has turned
 * the inspect switch on. "May inspect" is the same rule as the server's {@code LineSync.isAdmin} and the existing {@code InspectMode.available} (an operator, {@code hasPermissions(2)}, or a creative
 * player), so a client that forces the switch on without the permission lists nothing extra. {@link #diff} then turns "what should be listed" and "what is listed now" into the additions and removals
 * for the screen's list, so the screen touches its list only when something changed (not every frame) and never duplicates an entry.
 */
public final class MenuEntries {
    private MenuEntries() {}

    /** Whether the player may use ADMIN INSPECT: an operator or a creative player (the server's own rule). */
    public static boolean mayInspect(boolean op2, boolean creative) { return op2 || creative; }

    /** Inspect mode is in force only for a player who may use it. */
    public static boolean inspecting(boolean mayInspect, boolean switchedOn) { return mayInspect && switchedOn; }

    /**
     * The route ids Tensura's list should show, in the order of {@code allInOrder} (every line that has a stand-in, in the order the list wants). Owned lines always; every line when inspecting.
     * An owned id that is not in {@code allInOrder} (a route without a stand-in) is not listed: there is nothing to list it as.
     */
    public static List<String> wanted(List<String> allInOrder, Collection<String> owned, boolean inspecting) {
        if (inspecting) return List.copyOf(allInOrder);
        Set<String> own = new HashSet<>(owned);
        List<String> out = new ArrayList<>();
        for (String id : allInOrder) if (own.contains(id)) out.add(id);
        return out;
    }

    /** What to add to and remove from the screen's list, both in a stable order. */
    public record Diff(List<String> add, List<String> remove) {
        public boolean isEmpty() { return add.isEmpty() && remove.isEmpty(); }
    }

    public static Diff diff(Collection<String> present, List<String> wanted) {
        Set<String> have = new HashSet<>(present), want = new HashSet<>(wanted);
        List<String> add = new ArrayList<>();
        for (String id : wanted) if (!have.contains(id)) add.add(id);
        List<String> remove = new ArrayList<>();
        for (String id : present) if (!want.contains(id) && !remove.contains(id)) remove.add(id);
        return new Diff(List.copyOf(add), List.copyOf(remove));
    }
}
