package com.newuniverse.nusmp.core.magic.merge;

import com.newuniverse.nusmp.core.magic.profile.MagicKeys;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The pure registry of the merged magics of one world: creation (validated, deterministic, idempotent), lookup by id and by slot,
 * removal on request, and a line codec that the SavedData and the sync packet both use. It knows nothing of the game: whether a
 * parent is a real magic, and a parent's short name, are passed in.
 *
 * <p>Slots: every merged magic is cast by one of {@link MergeLimits#MAX_MERGED} pre-registered skills. A new magic takes the lowest slot
 * that was never used; only when all 64 have been used once does it reuse a slot freed by a removal (the grant tag of the skill names
 * the id it was granted for, so a reused slot never gives an old holder the new magic).</p>
 */
public final class MergeStore {
    public enum Status { CREATED, EXISTS, REFUSED }

    /** The outcome of {@link #create}. {@code def} is set for CREATED and EXISTS; {@code check} always says why. */
    public record Outcome(Status status, MergedDef def, MergeCheck.Result check) {}

    private final Map<String, MergedDef> byId = new LinkedHashMap<>();
    private long everUsed;                       // bit k set: slot k was used by some merged magic once

    // ---------------------------------------------------------------- queries
    public int size() { return byId.size(); }

    public MergedDef byId(String id) { return byId.get(MagicKeys.canonical(id)); }

    public MergedDef bySlot(int slot) {
        for (MergedDef d : byId.values()) if (d.slot() == slot) return d;
        return null;
    }

    public MergedDef byKey(int key) {
        for (MergedDef d : byId.values()) if (d.key() == key) return d;
        return null;
    }

    public List<MergedDef> all() { return Collections.unmodifiableList(new ArrayList<>(byId.values())); }

    /** Lower-case names of the merged magics (for the name-taken check). */
    public Set<String> names() {
        Set<String> s = new HashSet<>();
        for (MergedDef d : byId.values()) s.add(d.name().toLowerCase(Locale.ROOT));
        return s;
    }

    // ---------------------------------------------------------------- creation
    /**
     * Creates the merged magic of a parent set. The same set always gives the same id and name, and creating it again changes nothing
     * (status EXISTS, the existing definition is returned, a different custom name is ignored).
     * @param reservedNames lower-case names that are not available (the game's own magics' display names)
     */
    public Outcome create(List<String> rawParents, String customName, Predicate<String> isRealMagic, Set<String> reservedNames,
                          Function<String, String> partName) {
        Set<String> taken = names();
        if (reservedNames != null) taken.addAll(reservedNames);
        MergeCheck.Result r = MergeCheck.check(rawParents, customName, byId.size(), id -> byId.containsKey(id), isRealMagic, taken, partName);
        if (!r.ok()) return new Outcome(Status.REFUSED, null, r);
        MergedDef old = byId.get(r.id());
        if (old != null) return new Outcome(Status.EXISTS, old, r);
        int slot = freeSlot();
        if (slot < 0) return new Outcome(Status.REFUSED, null, new MergeCheck.Result(MergeCheck.Refusal.LIMIT_REACHED, String.valueOf(MergeLimits.MAX_MERGED), List.of(), "", ""));
        boolean custom = customName != null && !customName.isBlank();
        MergedDef def = new MergedDef(r.id(), r.name(), r.parents(), slot, custom);
        byId.put(def.id(), def);
        everUsed |= 1L << slot;
        return new Outcome(Status.CREATED, def, r);
    }

    private int freeSlot() {
        for (int k = 0; k < MergeLimits.MAX_MERGED; k++) if ((everUsed & (1L << k)) == 0 && bySlot(k) == null) return k;
        for (int k = 0; k < MergeLimits.MAX_MERGED; k++) if (bySlot(k) == null) return k;
        return -1;
    }

    /** Removes a merged magic (only ever on request). True if it existed. Its slot stays marked as used. */
    public boolean remove(String id) { return byId.remove(MagicKeys.canonical(id)) != null; }

    public void clear() { byId.clear(); everUsed = 0; }

    // ---------------------------------------------------------------- codec
    private static final String RETIRED = "#used|";

    /** One line per definition, then the used-slot mask: {@code id|slot|custom|name|PARENT,PARENT}. */
    public List<String> encode() {
        List<String> lines = new ArrayList<>();
        for (MergedDef d : byId.values())
            lines.add(d.id() + "|" + d.slot() + "|" + (d.custom() ? 1 : 0) + "|" + d.name() + "|" + String.join(",", d.parents()));
        lines.add(RETIRED + everUsed);
        return lines;
    }

    /**
     * The inverse of {@link #encode}. Tolerant: a malformed, duplicate or out-of-range line is skipped (never an exception), the limit of
     * 64 and the slot rules are enforced, and an id that does not match its parents is rebuilt from them.
     */
    public static MergeStore decode(List<String> lines) {
        MergeStore s = new MergeStore();
        if (lines == null) return s;
        for (String line : lines) {
            if (line == null) continue;
            if (line.startsWith(RETIRED)) {
                try { s.everUsed |= Long.parseLong(line.substring(RETIRED.length()).trim()); } catch (NumberFormatException ignored) { /* skip */ }
                continue;
            }
            String[] f = line.split("\\|", -1);
            if (f.length < 5) continue;
            int slot;
            try { slot = Integer.parseInt(f[1].trim()); } catch (NumberFormatException e) { continue; }
            if (slot < 0 || slot >= MergeLimits.MAX_MERGED || s.bySlot(slot) != null || s.byId.size() >= MergeLimits.MAX_MERGED) continue;
            List<String> parents = MergedKeys.canonicalParents(List.of(f[4].split(",")));
            if (parents.size() < MergeLimits.MIN_PARENTS || parents.size() > MergeLimits.MAX_PARENTS) continue;
            String id = MergedKeys.idOf(parents);
            if (s.byId.containsKey(id)) continue;
            String name = f[3].trim();
            if (name.isEmpty()) name = MergeNaming.autoName(parents, null);
            s.byId.put(id, new MergedDef(id, name, parents, slot, "1".equals(f[2].trim())));
            s.everUsed |= 1L << slot;
        }
        return s;
    }
}
