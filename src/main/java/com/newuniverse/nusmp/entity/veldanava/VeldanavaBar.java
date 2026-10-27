package com.newuniverse.nusmp.entity.veldanava;

/**
 * Why (3.30.0): the boss bar of Veldanava has its own look for each of his three bodies and is alive: the symbols and the title pulse between two colours, the bar flashes while he
 * changes body, turns red while his regeneration is broken and yellow while he is exposed, and the line under the title tells the state of the fight. All of it is decided here,
 * with no game class, from the form, the state and the tick; {@link VeldanavaBoss} turns the answer into a {@code ServerBossEvent}. Colour and overlay are named by the vanilla enum
 * names ({@code BLUE}, {@code NOTCHED_10} ...), and the boss reads them with {@code valueOf}.
 *
 * <p>Cost: the boss refreshes the bar every {@link #REFRESH} ticks and a viewer only gets a packet when something it shows changed (health text, status or pulse step), so at most
 * 4 small packets a second for each player who sees him.
 */
public final class VeldanavaBar {
    private VeldanavaBar() {}

    /** The bar is looked at again every this many ticks. */
    public static final int REFRESH = 5;
    /** One pulse (base colour to accent and back) takes this many ticks. */
    public static final int PULSE_PERIOD = 40;

    /** The look of one body: the vanilla colour and overlay of the bar, the two colours of the pulse (RGB), the symbol on each side of the title, and the title. */
    public record Style(String color, String overlay, int base, int accent, String symbol, String title) {}

    private static final Style[] STYLES = {
            new Style("BLUE", "NOTCHED_10", 0x3EE0D0, 0xB8FFF7, "✦", "Star King Dragon"),
            new Style("WHITE", "NOTCHED_12", 0xF2F2FF, 0xF2D27A, "✧", "Creator God"),
            new Style("PURPLE", "NOTCHED_20", 0xB58CFF, 0xFF3070, "✶", "Dark Avatar of God"),
    };

    public static Style style(int form) { return STYLES[VeldanavaRules.clampForm(form)]; }

    /** The what-he-is-doing part of the bar's state. */
    public static final int CALM = 0, REGENERATING = 1, BROKEN = 2, CASTING = 3, TRANSFORMING = 4, EXPOSED = 5, GUARDING = 6, DEAD = 7;

    /** The state to show: the loudest thing he is doing wins. */
    public static int state(boolean dead, boolean transforming, boolean casting, boolean guarding, boolean exposed, boolean brokenRegen, boolean regenerating) {
        if (dead) return DEAD;
        if (transforming) return TRANSFORMING;
        if (guarding) return GUARDING;
        if (exposed) return EXPOSED;
        if (casting) return CASTING;
        if (brokenRegen) return BROKEN;
        return regenerating ? REGENERATING : CALM;
    }

    /** The vanilla bar colour at this tick: the body's own, except while he changes body (it flashes white), is exposed (yellow) or his regeneration is broken (red). */
    public static String color(int form, int state, long tick) {
        return switch (state) {
            case TRANSFORMING -> (tick / 5) % 2 == 0 ? style(form).color() : "WHITE";
            case EXPOSED -> "YELLOW";
            case BROKEN -> "RED";
            case GUARDING -> "YELLOW";
            default -> style(form).color();
        };
    }

    /** The line under the title for a state ('moveLabel' names the running move when casting). */
    public static String status(int state, String moveLabel) {
        return switch (state) {
            case REGENERATING -> "Ultraspeed Regeneration";
            case BROKEN -> "Regeneration broken";
            case CASTING -> moveLabel == null || moveLabel.isEmpty() ? "Casting" : moveLabel;
            case TRANSFORMING -> "Changing form";
            case EXPOSED -> "Exposed";
            case GUARDING -> "Justice King Michael";
            case DEAD -> "Defeated";
            default -> "";
        };
    }

    /** 0..1..0 over {@link #PULSE_PERIOD} ticks (a smooth triangle-free sine). */
    public static double pulse(long tick) { return 0.5 - 0.5 * Math.cos(2.0 * Math.PI * (tick % PULSE_PERIOD) / PULSE_PERIOD); }

    /** The pulsing colour (RGB) of the title and the symbols: the base colour and the accent mixed by the pulse; faster when his health is low or he is casting. */
    public static int pulseColor(int form, int state, long tick, float healthFraction) {
        Style s = style(form);
        long t = tick;
        if (state == CASTING || state == TRANSFORMING || healthFraction < 0.25f) t = tick * 2;
        return mix(s.base(), state == BROKEN ? 0xFF4040 : s.accent(), pulse(t));
    }

    /** The symbols on both sides of the title; they swap between two shapes every half pulse so the title seems to breathe. */
    public static String symbols(int form, long tick) {
        Style s = style(form);
        return (tick % PULSE_PERIOD) < PULSE_PERIOD / 2 ? s.symbol() : "✵";
    }

    /** 'a' to 'b' by 't' (0..1), per channel. */
    public static int mix(int a, int b, double t) {
        double k = Math.max(0.0, Math.min(1.0, t));
        int r = (int) Math.round(((a >> 16) & 0xFF) * (1 - k) + ((b >> 16) & 0xFF) * k);
        int g = (int) Math.round(((a >> 8) & 0xFF) * (1 - k) + ((b >> 8) & 0xFF) * k);
        int bl = (int) Math.round((a & 0xFF) * (1 - k) + (b & 0xFF) * k);
        return (r << 16) | (g << 8) | bl;
    }

    /** "4,999,200 / 5,000,000". */
    public static String healthText(long health, long max) { return group(health) + " / " + group(max); }

    /** A number with a comma between every three digits (no locale involved). */
    public static String group(long v) {
        String s = Long.toString(Math.max(0L, v));
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && (s.length() - i) % 3 == 0) out.append(',');
            out.append(s.charAt(i));
        }
        return out.toString();
    }
}
