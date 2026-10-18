package com.newuniverse.nusmp.compat.maps;

import java.util.Locale;
import java.util.Optional;

/**
 * Why: map mods understand a plain-text chat format for a shared waypoint and turn it into a clickable "add waypoint" message on the client, so a waypoint that the
 * server prints in the right format needs no code in the map mod and no dependency on it. This class builds (and parses back) those strings. PURE: no Minecraft
 * import, so every detail is unit-tested.
 *
 * <p><b>Xaero's Minimap / World Map</b>, written form (the hyphen prefix), fields separated by ':':
 * {@code xaero-waypoint:<name>:<initials>:<x>:<y>:<z>:<colour 0-15>:<disabled true|false>:<type int>:Internal-<dimension key>-waypoints}.
 * Certainty (written from memory of Xaero's source and of the owner's spec; no Xaero's jar was available): the field ORDER and count are the part I am surest of; the
 * ':' escape {@code ^col^} in a name is moderately sure; the dimension key is medium sure: {@code overworld}, {@code the-nether}, {@code the-end} for vanilla and
 * {@code dim%<namespace>$<path>} for any other dimension. Newer Xaero's versions may add fields before the set name (a rotation flag, a yaw): {@link #parse} skips
 * unknown extra fields and finds the set by its {@code Internal-} prefix; this class never writes them. The older {@code xaero_waypoint:} prefix (underscore) is
 * accepted by {@link #parse} but never written. A y of {@code ~} (unknown height) is accepted by {@link #parse}.
 *
 * <p><b>JourneyMap</b>, optional form: {@code [name:"Gate", x:12, y:64, z:-30, dim:minecraft:overworld]} (medium certainty).
 */
public final class WaypointShare {
    private WaypointShare() {}

    public static final String XAERO_PREFIX = "xaero-waypoint", XAERO_OLD_PREFIX = "xaero_waypoint";
    /** What a ':' inside a name is written as (the string is colon-separated). */
    public static final String COLON = "^col^";
    public static final int MAX_NAME = 32;
    /** Parsed y when the string said {@code ~}. */
    public static final int UNKNOWN_Y = Integer.MIN_VALUE;

    /** What a shared place is, with its Xaero's colour index (0 black, 1 dark blue, 2 dark green, 3 dark aqua, 4 dark red, 5 dark purple, 6 gold, 7 gray, 8 dark gray, 9 blue, 10 green, 11 aqua, 12 red, 13 light purple, 14 yellow, 15 white). */
    public enum Kind {
        HERE(15), GATE(11), HUB(14), RETURN(10), SHORTCUT(13), POCKET(5), SAFE(2);

        public final int colour;

        Kind(int colour) { this.colour = colour; }
    }

    /** One waypoint as the format carries it. {@code set} is the full last field ({@code Internal-...-waypoints}) or empty when none was given. */
    public record Waypoint(String name, String initials, int x, int y, int z, int colour, boolean disabled, int type, String set) {}

    // ---------------------------------------------------------------- names
    /** A name safe for the format: control characters and section signs removed, trimmed, at most {@link #MAX_NAME} characters, never empty, ':' written as {@link #COLON}. */
    public static String escapeName(String name) {
        String s = clean(name);
        if (s.length() > MAX_NAME) s = s.substring(0, MAX_NAME).trim();
        if (s.isEmpty()) s = "Waypoint";
        return s.replace(":", COLON);
    }

    public static String unescapeName(String escaped) { return escaped == null ? "" : escaped.replace(COLON, ":"); }

    private static String clean(String name) {
        if (name == null) return "";
        StringBuilder b = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '§') { i++; continue; }                       // a formatting code: the sign and the letter after it
            if (c < 32 || c == 127) continue;
            b.append(c);
        }
        return b.toString().trim();
    }

    /** The initials Xaero's shows on the map marker: the first letter or digit of the name, upper case ({@code W} when there is none). */
    public static String initials(String name) {
        String s = clean(name);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c)) return String.valueOf(Character.toUpperCase(c));
        }
        return "W";
    }

    // ---------------------------------------------------------------- dimensions
    /**
     * Xaero's name of a dimension in a waypoint set: {@code overworld}, {@code the-nether}, {@code the-end}, otherwise {@code dim%<namespace>$<path>} (a '/' in the path
     * becomes '%'). Medium certainty, see the class comment. A null or malformed id gives {@code overworld}.
     */
    public static String xaeroDimensionKey(String dimensionId) {
        String id = dimensionId == null ? "" : dimensionId.trim().toLowerCase(Locale.ROOT);
        int c = id.indexOf(':');
        if (c <= 0 || c == id.length() - 1) return "overworld";
        String ns = id.substring(0, c), path = id.substring(c + 1);
        if (ns.equals("minecraft")) {
            switch (path) {
                case "overworld": return "overworld";
                case "the_nether": return "the-nether";
                case "the_end": return "the-end";
                default: break;
            }
        }
        return "dim%" + ns + "$" + path.replace('/', '%');
    }

    /** The last field of the Xaero's string: {@code Internal-<key>-waypoints}. */
    public static String xaeroSet(String dimensionId) { return "Internal-" + xaeroDimensionKey(dimensionId) + "-waypoints"; }

    // ---------------------------------------------------------------- build
    /** The Xaero's chat string for a place. {@code colour} is clamped to 0..15. */
    public static String xaero(String name, int x, int y, int z, int colour, String dimensionId) {
        String n = escapeName(name);
        return XAERO_PREFIX + ":" + n + ":" + initials(n.replace(COLON, ":")) + ":" + x + ":" + y + ":" + z + ":" + Math.max(0, Math.min(15, colour)) + ":false:0:" + xaeroSet(dimensionId);
    }

    public static String xaero(String name, int x, int y, int z, Kind kind, String dimensionId) { return xaero(name, x, y, z, kind.colour, dimensionId); }

    /** The JourneyMap chat form: {@code [name:"Gate", x:12, y:64, z:-30, dim:minecraft:overworld]}. A '"' in the name becomes a single quote. */
    public static String journeyMap(String name, int x, int y, int z, String dimensionId) {
        String n = clean(name).replace('"', '\'');
        if (n.length() > MAX_NAME) n = n.substring(0, MAX_NAME).trim();
        if (n.isEmpty()) n = "Waypoint";
        String dim = dimensionId == null || dimensionId.isBlank() ? "minecraft:overworld" : dimensionId.trim();
        return "[name:\"" + n + "\", x:" + x + ", y:" + y + ", z:" + z + ", dim:" + dim + "]";
    }

    // ---------------------------------------------------------------- parse (for the self-test and for round-trip checks)
    /** Reads a Xaero's string (either prefix); empty when it is not one or a number is malformed. Extra fields between the type and the set name are ignored. */
    public static Optional<Waypoint> parse(String text) {
        if (text == null) return Optional.empty();
        String[] f = text.trim().split(":", -1);
        if (f.length < 9 || !(f[0].equals(XAERO_PREFIX) || f[0].equals(XAERO_OLD_PREFIX))) return Optional.empty();
        try {
            int y = f[4].equals("~") ? UNKNOWN_Y : Integer.parseInt(f[4]);
            String set = "";
            for (int i = 9; i < f.length; i++) if (f[i].startsWith("Internal-")) { set = f[i]; break; }
            return Optional.of(new Waypoint(unescapeName(f[1]), f[2], Integer.parseInt(f[3]), y, Integer.parseInt(f[5]), Integer.parseInt(f[6]), Boolean.parseBoolean(f[7]), Integer.parseInt(f[8]), set));
        } catch (NumberFormatException bad) {
            return Optional.empty();
        }
    }

    /** True when {@code set} (an {@code Internal-...-waypoints} field) names the dimension {@code dimensionId}. */
    public static boolean setMatches(String set, String dimensionId) { return xaeroSet(dimensionId).equals(set); }
}
