package com.newuniverse.nusmp.client.ui.codex;

/**
 * Why: how a rule's live value is written, in one pure place. The server configs are not synced to the client; when the client can read the
 * value (a single-player world, or a server that syncs it) it is shown as is, otherwise the config's default is shown and labelled
 * "(default)" so the Codex never claims a value it does not know.
 */
public final class CodexRuleValue {
    private CodexRuleValue() {}

    public static final String DEFAULT_TAG = "(default)";

    public static String bool(boolean value, boolean live) { return tag(value ? "ON" : "OFF", live); }

    public static String percent(double fraction, boolean live) { return tag(trim(fraction * 100.0) + "%", live); }

    public static String number(double value, String unit, boolean live) { return tag(trim(value) + (unit == null || unit.isEmpty() ? "" : " " + unit), live); }

    private static String tag(String v, boolean live) { return live ? v : v + " " + DEFAULT_TAG; }

    /** 6.0 -> "6", 2.5 -> "2.5", 0.123456 -> "0.12". */
    static String trim(double v) {
        double r = Math.round(v * 100.0) / 100.0;
        return r == Math.rint(r) ? Long.toString((long) r) : Double.toString(r);
    }
}
