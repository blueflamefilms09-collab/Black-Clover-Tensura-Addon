package com.newuniverse.nusmp.skilltree.unique;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Why: the decision of the login migration is the part that must never be wrong in a way nobody notices (a player who owned a line and does not get its skill, or who gets a skill of a magic that was
 * removed), so it is a pure function with no Minecraft type, tested without a game: given the route ids the old Skill Tree record says a player owned and the bare ids of the books that exist, it answers which
 * unique skills to teach (sorted, no duplicates) and which owned lines are dropped (a removed magic, or an id that no book has). {@link LegacyMigration} carries it out.
 */
public final class MigrationPlan {
    private MigrationPlan() {}

    /** What to do with the lines a player owned: the books to teach (bare ids, sorted) and the lines dropped (removed magics or unknown ids). */
    public record Plan(List<String> grant, List<String> dropped) {}

    /** The mapping from owned route ids to the skills to teach. Pure. */
    public static Plan plan(Collection<String> ownedRoutes, Collection<String> knownBares) {
        Set<String> known = new LinkedHashSet<>(knownBares);
        TreeSet<String> grant = new TreeSet<>();
        TreeSet<String> dropped = new TreeSet<>();
        if (ownedRoutes != null) {
            for (String id : ownedRoutes) {
                if (id == null || id.isEmpty()) continue;
                boolean ours = id.indexOf(':') < 0 || id.startsWith(LineIndex.NAMESPACE + ":");      // a route of another namespace (a data pack's) has no book of ours
                String bare = LineIndex.bare(id);
                if (ours && !LegacyIds.isRemovedLine(bare) && known.contains(bare)) grant.add(bare);
                else dropped.add(ours ? bare : id);
            }
        }
        return new Plan(List.copyOf(grant), List.copyOf(dropped));
    }

    /** {@code route#node} to {@code route}. */
    public static String routePart(String key) {
        int i = key.indexOf('#');
        return i < 0 ? key : key.substring(0, i);
    }
}
