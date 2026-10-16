package com.newuniverse.nusmp.arena;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the Hourglass of the Final Hour carries players into the arena, and the way back must be exactly where each of them stood, in
 * the dimension they stood in, facing the way they faced, even after a restart and even if they were carried by somebody else's
 * hourglass. This pure book (no Minecraft types, unit tested) keeps one {@link Entry} per player UUID and turns it into a plain string and
 * back (a dimension id has a colon in it, so the separator is a bar), so the saved data class is only a thin wrapper that writes the strings
 * into a tag. A string that does not parse is dropped, never thrown at the player. At most {@value #MAX} entries are kept (the oldest goes first), so
 * a server where thousands of players once visited does not grow without end.
 */
public final class ReturnBook {
    public static final int MAX = 4096;

    /** Where a player stood: dimension id (as "namespace:path"), position, rotation. */
    public record Entry(String dimension, double x, double y, double z, float yaw, float pitch) {
        public String encode() { return dimension + "|" + x + "|" + y + "|" + z + "|" + yaw + "|" + pitch; }

        /** Parses {@link #encode}'s output; null for anything else (wrong count, not a number, NaN, an empty dimension). */
        public static Entry decode(String s) {
            if (s == null) return null;
            String[] p = s.split("\\|", -1);
            if (p.length != 6 || p[0].isEmpty()) return null;
            try {
                double x = Double.parseDouble(p[1]), y = Double.parseDouble(p[2]), z = Double.parseDouble(p[3]);
                float yaw = Float.parseFloat(p[4]), pitch = Float.parseFloat(p[5]);
                if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z) || Float.isNaN(yaw) || Float.isNaN(pitch)
                        || Double.isInfinite(x) || Double.isInfinite(y) || Double.isInfinite(z)) return null;
                return new Entry(p[0], x, y, z, yaw, pitch);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    private final Map<UUID, Entry> map = new LinkedHashMap<>();

    /** Remembers where a player stood (replacing an older entry of theirs; a player already in the arena is never recorded by the director). */
    public void put(UUID who, Entry e) {
        if (who == null || e == null) return;
        map.remove(who);
        map.put(who, e);
        while (map.size() > MAX) map.remove(map.keySet().iterator().next());
    }

    public Entry get(UUID who) { return map.get(who); }

    /** Takes the entry out (the player is going home now). */
    public Entry take(UUID who) { return map.remove(who); }

    public boolean has(UUID who) { return map.containsKey(who); }

    public int size() { return map.size(); }

    public void clear() { map.clear(); }

    /** The book as plain strings by UUID string, for the saved data. */
    public Map<String, String> toStrings() {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<UUID, Entry> e : map.entrySet()) out.put(e.getKey().toString(), e.getValue().encode());
        return out;
    }

    /** Loads strings written by {@link #toStrings}; bad keys and bad values are skipped. */
    public void fromStrings(Map<String, String> in) {
        map.clear();
        if (in == null) return;
        for (Map.Entry<String, String> e : in.entrySet()) {
            Entry v = Entry.decode(e.getValue());
            if (v == null) continue;
            try {
                put(UUID.fromString(e.getKey()), v);
            } catch (IllegalArgumentException bad) {
                // a key that is not a UUID is dropped
            }
        }
    }
}
