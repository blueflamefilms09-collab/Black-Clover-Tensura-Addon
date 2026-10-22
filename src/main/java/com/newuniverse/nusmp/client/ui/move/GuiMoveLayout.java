package com.newuniverse.nusmp.client.ui.move;

import com.newuniverse.nusmp.client.ui.move.GuiMoveMath.Place;

import java.util.Locale;

/**
 * Why: what the player did to one element (moved it, sized it, hid it) is kept as ONE short text per element in the layout file
 * ({@code nusmp-gui-layout-client.toml}), for example {@code p=1;a=3;cx=0;cy=0;ox=12;oy=4;s=1.25;h=0}. An empty text means "never touched": the element draws exactly where
 * and how its own code puts it. Reading is forgiving (unknown keys are ignored, numbers that do not parse fall back to the default, every value is forced into its range),
 * so a hand-edited or damaged file can never put an element out of reach or break the HUD. The text is parsed once when the file loads or changes, never per frame. Pure.
 */
public final class GuiMoveLayout {
    private GuiMoveLayout() {}

    /**
     * What is known about one element. {@code placed}: the player has set a place (and a size); without it the element is where its own code puts it ({@code scale} is then
     * 1). {@code hidden}: the player switched the element off. A state that is neither placed nor hidden does not exist (null).
     */
    public static final class State {
        public boolean placed;
        public Place place = Place.DEFAULT;
        public float scale = 1f;
        public boolean hidden;

        public State copy() {
            State s = new State();
            s.placed = placed;
            s.place = place;
            s.scale = scale;
            s.hidden = hidden;
            return s;
        }

        /** Nothing set at all: the same as no state. */
        public boolean isDefault() { return !placed && !hidden; }
    }

    /** The text of a state; empty for no state (never touched). */
    public static String format(State s) {
        if (s == null || s.isDefault()) return "";
        Place p = GuiMoveMath.sanitize(s.place);
        StringBuilder b = new StringBuilder(64);
        if (s.placed) {
            b.append("p=1;a=").append(p.anchor()).append(";cx=").append(p.centerX() ? 1 : 0).append(";cy=").append(p.centerY() ? 1 : 0)
                    .append(";ox=").append(p.offX()).append(";oy=").append(p.offY()).append(";s=").append(String.format(Locale.ROOT, "%.2f", GuiMoveMath.clampScale(s.scale)));
        }
        if (s.hidden) {
            if (b.length() > 0) b.append(';');
            b.append("h=1");
        }
        return b.toString();
    }

    /** The state of a text, or null for an empty or unusable one (nothing known: the element is left alone). */
    public static State parse(String text) {
        if (text == null) return null;
        String t = text.trim();
        if (t.isEmpty()) return null;
        State s = new State();
        int anchor = 0, ox = 0, oy = 0;
        boolean cx = false, cy = false;
        for (String part : t.split(";")) {
            int eq = part.indexOf('=');
            if (eq <= 0) continue;
            String k = part.substring(0, eq).trim().toLowerCase(Locale.ROOT), v = part.substring(eq + 1).trim();
            switch (k) {
                case "p" -> s.placed = flag(v);
                case "a" -> anchor = (int) number(v, 0);
                case "cx" -> cx = flag(v);
                case "cy" -> cy = flag(v);
                case "ox" -> ox = (int) number(v, 0);
                case "oy" -> oy = (int) number(v, 0);
                case "s" -> s.scale = GuiMoveMath.clampScale(number(v, 1));
                case "h" -> s.hidden = flag(v);
                default -> { /* a key of a later version: ignored */ }
            }
        }
        s.place = GuiMoveMath.sanitize(new Place(anchor, cx, cy, ox, oy));
        if (!s.placed) {
            s.place = Place.DEFAULT;
            s.scale = 1f;
        }
        return s.isDefault() ? null : s;
    }

    private static boolean flag(String v) { return v.equals("1") || v.equalsIgnoreCase("true"); }

    private static double number(String v, double fallback) {
        try {
            double d = Double.parseDouble(v.replace(',', '.'));
            return Double.isNaN(d) || Double.isInfinite(d) ? fallback : d;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
