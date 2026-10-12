package com.newuniverse.nusmp.dungeon.nexus.relic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The attunements a player holds, as a list of bond ids (docs/living_dungeon.md section 7, "a 5e rule"): at most {@link #CAP} relics at once,
 * and a fourth needs one to be released first. A bond whose relic is no longer on the player (lost, destroyed, left in a chest) is pruned
 * when the cap is hit, so a lost relic can never hold a slot for ever. Pure: the glue keeps the list in the player's persistent data.
 */
public final class Bonds {
    private Bonds() {}

    public static final int CAP = 3;

    public enum Verdict { OK, FULL, ALREADY }

    public static boolean has(List<String> bonds, String id) { return bonds.contains(id); }

    public static int free(List<String> bonds) { return Math.max(0, CAP - bonds.size()); }

    /** May this bond be added? ALREADY if it is in the list, FULL when the cap is reached. */
    public static Verdict check(List<String> bonds, String id) {
        if (bonds.contains(id)) return Verdict.ALREADY;
        return bonds.size() >= CAP ? Verdict.FULL : Verdict.OK;
    }

    /** The list with this bond added at the end (unchanged if it is already there or the cap is reached). */
    public static List<String> add(List<String> bonds, String id) {
        if (check(bonds, id) != Verdict.OK) return List.copyOf(bonds);
        List<String> out = new ArrayList<>(bonds);
        out.add(id);
        return List.copyOf(out);
    }

    public static List<String> remove(List<String> bonds, String id) {
        List<String> out = new ArrayList<>(bonds);
        out.remove(id);
        return List.copyOf(out);
    }

    /** Keeps the bonds whose relic is still carried, in their order. */
    public static List<String> prune(List<String> bonds, Set<String> carried) {
        List<String> out = new ArrayList<>();
        for (String b : bonds) if (carried.contains(b)) out.add(b);
        return List.copyOf(out);
    }

    /** The bond held the longest, or null. */
    public static String oldest(List<String> bonds) { return bonds.isEmpty() ? null : bonds.get(0); }
}
