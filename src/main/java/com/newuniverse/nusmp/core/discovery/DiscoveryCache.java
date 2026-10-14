package com.newuniverse.nusmp.core.discovery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The client's copy of what its player has discovered, filled by the {@code nusmp:discovery_sync} packet. It holds plain strings and statics
 * only (no client classes), so touching it from a common class is safe on a dedicated server too. Every UI asks it, never the server.
 * <p>
 * Until the first sync of a session arrives ({@link #synced()} false: a server without the mod, or a screen opened too early) the cache shows
 * everything, so the old behaviour is what you get whenever the system is not running. The reference to the set is replaced, never edited,
 * so the render thread can read it while the network thread writes.
 */
public final class DiscoveryCache {
    private DiscoveryCache() {}

    private static volatile Set<String> known = Set.of();
    private static volatile boolean showAll, synced;
    private static final List<Consumer<List<String>>> LISTENERS = new CopyOnWriteArrayList<>();

    public static boolean synced() { return synced; }

    public static boolean showsAll() { return showAll || !synced; }

    /** True if an entry with this key may be drawn with its real name and text. */
    public static boolean visible(String key) { return DiscoveryRules.visible(known, showsAll(), key); }

    /** An entry gated by several keys: any one shows it; no keys = never gated. */
    public static boolean visibleAny(Collection<String> keys) { return DiscoveryRules.visibleAny(known, showsAll(), keys); }

    /** "a|b" keys form (see {@link DiscoveryRules#splitKeys}). */
    public static boolean visibleAny(String joinedKeys) { return visibleAny(DiscoveryRules.splitKeys(joinedKeys)); }

    /** The real name when the key is known, otherwise {@code ???}. */
    public static String label(String key, String realName) { return DiscoveryRules.label(known, showsAll(), key, realName); }

    public static int count() { return known.size(); }

    /** A UI may listen for newly found keys (a toast, a glow on a tab). Called on the client thread with the new keys. */
    public static void addListener(Consumer<List<String>> l) { LISTENERS.add(l); }

    /**
     * Network handler (client thread): a full set (replace) or a delta, with the viewer's show-all state. {@code announce} marks a delta of
     * things found just now (the listeners hear about those; the chunks of a login sync are not announced).
     */
    public static void receive(boolean replace, boolean all, boolean announce, Collection<String> keys) {
        Set<String> next = new HashSet<>(replace ? Set.of() : known);
        List<String> fresh = new ArrayList<>();
        for (String k : keys) if (DiscoveryRules.validKey(k) && next.add(k)) fresh.add(k);
        known = Set.copyOf(next);
        showAll = all;
        synced = true;
        if (announce && !replace && !fresh.isEmpty()) for (Consumer<List<String>> l : LISTENERS) l.accept(fresh);
    }

    /** Logout, or a different server: back to "not synced" (everything shown until the next sync). */
    public static void clear() {
        known = Set.of();
        showAll = false;
        synced = false;
    }
}
