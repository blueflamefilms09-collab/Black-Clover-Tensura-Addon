package com.newuniverse.nusmp.core.magic.merge;

import com.newuniverse.nusmp.core.magic.profile.MagicKeys;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * Pure id helpers of merged magics. The id of a merged magic is built from its parent SET, never from the order the parents were
 * typed: {@code merged:<a>+<b>[+<c>[+<d>]]} with the parents' lower-case enum names in alphabetical order. The same parents therefore
 * always give the same id (so creating it twice is idempotent) and the same canonical parent order (so every derived value is the same).
 */
public final class MergedKeys {
    private MergedKeys() {}

    /** The canonical parents of a set of raw names: enum spelling, de-duplicated, alphabetical. Blank names are dropped. */
    public static List<String> canonicalParents(Collection<String> raw) {
        TreeSet<String> set = new TreeSet<>();
        if (raw != null) for (String r : raw) { String k = MagicKeys.enumKey(r); if (!k.isEmpty()) set.add(k); }
        return new ArrayList<>(set);
    }

    /** "merged:flame+water" for canonical parents. */
    public static String idOf(List<String> canonicalParents) {
        StringBuilder sb = new StringBuilder(MagicKeys.MERGED_PREFIX);
        for (int i = 0; i < canonicalParents.size(); i++) {
            if (i > 0) sb.append('+');
            sb.append(canonicalParents.get(i).toLowerCase(java.util.Locale.ROOT));
        }
        return sb.toString();
    }

    /** The parents back out of an id ("merged:flame+water" -> [FLAME, WATER]); empty for anything that is not a merged id. */
    public static List<String> parentsOf(String id) {
        List<String> out = new ArrayList<>();
        if (!MagicKeys.isMerged(id)) return out;
        String body = id.trim().substring(MagicKeys.MERGED_PREFIX.length());
        for (String part : body.split("\\+")) { String k = MagicKeys.enumKey(part); if (!k.isEmpty()) out.add(k); }
        return out;
    }

    /** A stable 32-bit key of an id (FNV-1a), never 0: the high half of a merged effect's seed. */
    public static int hash(String id) {
        int h = 0x811C9DC5;
        String c = MagicKeys.canonical(id);
        for (int i = 0; i < c.length(); i++) { h ^= c.charAt(i); h *= 0x01000193; }
        return h == 0 ? 1 : h;
    }

    /** The seed of a merged effect: the id's key in the high 32 bits, the effect's own randomness in the low 32. */
    public static long seed(String id, long random) { return ((long) hash(id) << 32) | (random & 0xFFFFFFFFL); }

    /** The id key out of a seed built by {@link #seed}. */
    public static int keyOfSeed(long seed) { return (int) (seed >>> 32); }
}
