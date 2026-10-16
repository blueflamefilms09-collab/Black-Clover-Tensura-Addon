package com.newuniverse.nusmp.thread;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * Why: Rouge guards only the people Vanessa cares about, and that list has two halves: the ones the game knows by itself (the
 * caster, her pets, her squad) and the ones she chose by hand. The hand-picked half must be capped, must survive in the grimoire's
 * skill tag as plain text, and must never let an enemy in, so those rules are pure code with tests; the glue
 * ({@link RougeManager}) only answers the game-side questions (is this a pet, is this a squad mate) and passes the answers here.
 */
public final class ProtectedSet {
    private ProtectedSet() {}

    public enum Toggle { ADDED, REMOVED, FULL }

    /** Adds 'id' to the list, or removes it if it is there; refuses to grow past 'cap'. The list is changed in place. */
    public static Toggle toggle(List<UUID> list, UUID id, int cap) {
        if (list.remove(id)) return Toggle.REMOVED;
        if (list.size() >= Math.max(0, cap)) return Toggle.FULL;
        list.add(id);
        return Toggle.ADDED;
    }

    /** The saved form: ids separated by commas (a skill tag holds one string). */
    public static String format(List<UUID> list) {
        StringBuilder sb = new StringBuilder();
        for (UUID u : list) { if (sb.length() > 0) sb.append(','); sb.append(u); }
        return sb.toString();
    }

    /** Reads the saved form back; bad pieces are skipped and repeats collapsed, the order kept. */
    public static List<UUID> parse(String text) {
        LinkedHashSet<UUID> out = new LinkedHashSet<>();
        if (text != null) for (String s : text.split(",")) {
            String t = s.trim();
            if (t.isEmpty()) continue;
            try { out.add(UUID.fromString(t)); } catch (IllegalArgumentException bad) { /* a damaged entry is dropped */ }
        }
        return new ArrayList<>(out);
    }

    /** Cuts the list down to the cap when the cap was lowered in the config (the oldest entries stay). */
    public static List<UUID> clamp(List<UUID> list, int cap) {
        int c = Math.max(0, cap);
        return list.size() <= c ? list : new ArrayList<>(list.subList(0, c));
    }

    /**
     * The one question: is this living thing under Rouge's watch? The caster always is. Anyone else needs to be a hand-picked name, the
     * caster's own pet or a squad mate, and must not be fighting the caster right now (an enemy is never protected, however it got on
     * the list). Rouge herself and dead things are never protected.
     */
    public static boolean isProtected(boolean isCaster, boolean whitelisted, boolean ownedPet, boolean squadMate, boolean attackingCaster, boolean isRouge) {
        if (isRouge) return false;
        if (isCaster) return true;
        if (attackingCaster) return false;
        return whitelisted || ownedPet || squadMate;
    }
}
