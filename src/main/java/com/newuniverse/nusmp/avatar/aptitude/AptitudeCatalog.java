package com.newuniverse.nusmp.avatar.aptitude;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Why: one list of the aptitudes that exist, filled from data ({@code data/nusmp/avatar_aptitudes/*.json}, read by a reload listener the Soul Forge builder writes)
 * so a data pack can add or retune aptitudes, and read by everyone through these static methods. Loading replaces the whole content at once (a reload never leaves
 * a half-filled list), reading is lock-free on an immutable snapshot.
 */
public final class AptitudeCatalog {
    private AptitudeCatalog() {}

    private static volatile Map<String, Aptitude> byId = Map.of();

    /** Replaces the catalog (called by the data loader on every reload). */
    public static void replaceAll(List<Aptitude> list) {
        Map<String, Aptitude> m = new LinkedHashMap<>();
        if (list != null) for (Aptitude a : list) if (a != null) m.putIfAbsent(a.id(), a);
        byId = Collections.unmodifiableMap(m);
    }

    public static List<Aptitude> all() { return new ArrayList<>(byId.values()); }

    public static Aptitude byId(String id) { return id == null ? null : byId.get(id.toLowerCase(Locale.ROOT)); }

    public static int size() { return byId.size(); }
}
