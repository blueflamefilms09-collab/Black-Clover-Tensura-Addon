package com.newuniverse.nusmp.spiritspace;

import java.util.function.Predicate;

/**
 * Why: the Spirit of Space lets a player keep a few named waypoints and open a portal to one of them. Everything that decides whether a waypoint is acceptable, whether it is still valid and
 * whether a trip to it is allowed is here, pure (no game classes), so it is unit tested and the server code (the storage in {@link WaypointStore}, the flow in {@link SpiritSpace}) only asks. The
 * rules, in the order the server applies them to a trip: the slot exists, the waypoint is set, it is in the dimension the player stands in (no cross-dimension travel exists), it is within the range
 * of the portal, and later the server checks its chunk is loaded and finds a safe landing ({@link SpaceLanding}).
 */
public final class Waypoints {
    private Waypoints() {}

    /** One saved place. {@code dim} is the dimension's id as text ({@code minecraft:overworld}). */
    public record Waypoint(String name, String dim, double x, double y, double z, float yaw) {}

    /** The reasons a trip or a new waypoint is refused (language keys end with these). */
    public static final String OK = null, EMPTY = "empty", OTHER_DIMENSION = "dimension", TOO_FAR = "far", BAD_SLOT = "slot", BAD_SPOT = "spot";

    private static boolean finite(double v) { return !Double.isNaN(v) && !Double.isInfinite(v); }

    /** The name as it is stored: trimmed, only letters, digits, space, underscore, hyphen and apostrophe, at most {@link SpiritSpaceRules#NAME_MAX} characters; a blank result becomes "Waypoint N" (N = slot + 1). */
    public static String cleanName(String raw, int slot) {
        StringBuilder sb = new StringBuilder();
        if (raw != null) {
            for (int i = 0; i < raw.length() && sb.length() < SpiritSpaceRules.NAME_MAX; i++) {
                char c = raw.charAt(i);
                boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == ' ' || c == '_' || c == '-' || c == '\'';
                if (ok) sb.append(c);
            }
        }
        String s = sb.toString().strip();
        return s.isEmpty() ? "Waypoint " + (slot + 1) : s;
    }

    /** Is the waypoint well formed: a finite place inside the build height of its dimension, a dimension id, a finite yaw, a name that is already clean. */
    public static boolean valid(Waypoint w, int minY, int maxY) {
        if (w == null || w.dim() == null || w.dim().isBlank() || w.name() == null || w.name().isBlank()) return false;
        if (!finite(w.x()) || !finite(w.y()) || !finite(w.z()) || !Float.isFinite(w.yaw())) return false;
        if (Math.abs(w.x()) > 3.0E7 || Math.abs(w.z()) > 3.0E7) return false;
        if (w.y() < minY || w.y() > maxY) return false;
        return w.name().length() <= SpiritSpaceRules.NAME_MAX;
    }

    /**
     * The book cleaned: a slot beyond {@code slots} is dropped, a malformed waypoint or one whose dimension no longer exists is removed (set to null), a name that is not clean is cleaned. Returns a new
     * array of exactly {@code slots} entries; the input is not changed.
     */
    public static Waypoint[] clean(Waypoint[] in, int slots, int minY, int maxY, Predicate<String> dimensionKnown) {
        Waypoint[] out = new Waypoint[Math.max(0, slots)];
        if (in == null) return out;
        for (int i = 0; i < out.length && i < in.length; i++) {
            Waypoint w = in[i];
            if (w == null || w.dim() == null || w.name() == null) continue;
            if (!finite(w.x()) || !finite(w.y()) || !finite(w.z()) || !Float.isFinite(w.yaw())) continue;
            Waypoint c = new Waypoint(cleanName(w.name(), i), w.dim(), w.x(), w.y(), w.z(), w.yaw());
            if (!valid(c, minY, maxY) || !dimensionKnown.test(c.dim())) continue;
            out[i] = c;
        }
        return out;
    }

    /** How many slots of the book are set. */
    public static int count(Waypoint[] book) {
        int n = 0;
        if (book != null) for (Waypoint w : book) if (w != null) n++;
        return n;
    }

    /**
     * Is a trip to slot {@code slot} allowed from where the player stands? Returns {@link #OK} (null) or the reason. The player's place is (px, py, pz) in dimension {@code here}; {@code range} is the
     * portal's range in blocks (the server config, already clamped).
     */
    public static String checkTrip(Waypoint[] book, int slot, String here, double px, double py, double pz, double range) {
        if (book == null || !SpiritSpaceRules.validSlot(slot, book.length)) return BAD_SLOT;
        Waypoint w = book[slot];
        if (w == null) return EMPTY;
        if (here == null || !here.equals(w.dim())) return OTHER_DIMENSION;
        if (!SpiritSpaceRules.inRange(px, py, pz, w.x(), w.y(), w.z(), range)) return TOO_FAR;
        return OK;
    }

    /** Is a place where a new waypoint is to be set acceptable (finite, inside the build height)? */
    public static String checkSpot(double x, double y, double z, int minY, int maxY) {
        return finite(x) && finite(y) && finite(z) && y >= minY && y <= maxY && Math.abs(x) <= 3.0E7 && Math.abs(z) <= 3.0E7 ? OK : BAD_SPOT;
    }
}
