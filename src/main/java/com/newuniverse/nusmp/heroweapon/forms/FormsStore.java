package com.newuniverse.nusmp.heroweapon.forms;

import com.newuniverse.nusmp.heroweapon.HeroKind;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the forms of a weapon have two homes. On a server (dedicated, or the integrated one) they are read from the data pack by {@link FormsLoader}; on a client
 * that joined a remote server they arrive in a packet, because a client cannot read the server's data. Everything that asks "what are the forms of this weapon?"
 * must not care which, so both live here as immutable snapshots behind volatile fields (a reload swaps the whole map; a reader never sees half of one), and
 * {@link HeroForms#of} reads them in the order: the loaded data, then the synced copy, then the synthetic base form. This is the cache of immutable data that the
 * server rules allow to be static: it is cleared when the server stops and when the client leaves a world.
 */
final class FormsStore {
    private FormsStore() {}

    private static final Map<HeroKind, List<HeroForm>> SYNTHETIC = new EnumMap<>(HeroKind.class);

    static {
        for (HeroKind k : HeroKind.values()) SYNTHETIC.put(k, List.of(HeroForm.base(k)));
    }

    private static volatile Map<HeroKind, List<HeroForm>> loaded = Map.of();
    private static volatile Map<HeroKind, List<HeroForm>> synced = Map.of();
    private static volatile Map<HeroKind, FormsSchema.Parsed> reports = Map.of();
    private static volatile List<String> stray = List.of();

    /** The forms of a weapon: the loaded data, else the synced copy, else the one synthetic base form. Never null, never empty. */
    static List<HeroForm> forms(HeroKind kind) {
        List<HeroForm> l = loaded.get(kind);
        if (l == null) l = synced.get(kind);
        return l != null ? l : SYNTHETIC.get(kind);
    }

    static boolean hasLoaded(HeroKind kind) { return loaded.containsKey(kind); }

    /** What the loader found at the last reload, by weapon. */
    static Map<HeroKind, FormsSchema.Parsed> reports() { return reports; }

    /** File names under the forms folder that match no weapon of the catalogue. */
    static List<String> stray() { return stray; }

    static void install(Map<HeroKind, List<HeroForm>> forms, Map<HeroKind, FormsSchema.Parsed> found, List<String> strayFiles) {
        Map<HeroKind, List<HeroForm>> nextForms = new EnumMap<>(HeroKind.class);
        nextForms.putAll(forms);
        Map<HeroKind, FormsSchema.Parsed> nextReports = new EnumMap<>(HeroKind.class);
        nextReports.putAll(found);
        loaded = Collections.unmodifiableMap(nextForms);
        reports = Collections.unmodifiableMap(nextReports);
        stray = List.copyOf(strayFiles);
    }

    static void clearLoaded() {
        loaded = Map.of();
        reports = Map.of();
        stray = List.of();
    }

    /** The client side: the forms the server sent for the weapon (replaces an older copy of the same weapon). */
    static void setSynced(HeroKind kind, List<HeroForm> forms) {
        Map<HeroKind, List<HeroForm>> next = new EnumMap<>(HeroKind.class);
        next.putAll(synced);
        next.put(kind, List.copyOf(forms));
        synced = Collections.unmodifiableMap(next);
    }

    static boolean hasSynced(HeroKind kind) { return synced.containsKey(kind); }

    /** The weapon whose forms the client holds (a client is bonded to one weapon), or null. */
    static HeroKind anySynced() {
        for (HeroKind k : synced.keySet()) return k;
        return null;
    }

    static void clearSynced() { synced = Map.of(); }

    /** The checksum of the forms of a weapon as {@link HeroForms#of} answers them (for the "does the client have the current copy" question). */
    static int checksum(HeroKind kind) { return FormsWire.checksum(forms(kind)); }

    // ------------------------------------------------------------------------------------------------ test seam
    /** Replaces the loaded forms of one weapon and returns what was there (null for none); the self-tests put it back with {@link #restoreLoaded}. */
    static synchronized List<HeroForm> swapLoaded(HeroKind kind, List<HeroForm> forms) {
        Map<HeroKind, List<HeroForm>> next = new EnumMap<>(HeroKind.class);
        next.putAll(loaded);
        List<HeroForm> before = next.put(kind, List.copyOf(forms));
        loaded = Collections.unmodifiableMap(next);
        return before;
    }

    static synchronized void restoreLoaded(HeroKind kind, List<HeroForm> before) {
        Map<HeroKind, List<HeroForm>> next = new EnumMap<>(HeroKind.class);
        next.putAll(loaded);
        if (before == null) next.remove(kind);
        else next.put(kind, before);
        loaded = Collections.unmodifiableMap(next);
    }

    /** A copy of the reports map in file order, for the commands. */
    static Map<HeroKind, FormsSchema.Parsed> reportsCopy() { return new LinkedHashMap<>(reports); }
}
