package com.newuniverse.nusmp.elite.nations.core;

/**
 * Why: the result of every nation action (command, screen button, API call), pure: success or failure plus a translation key and its arguments. The command and the screen turn it into a chat line with
 * {@code Msg}; tests and the API read {@code ok()} and {@code key()}. Keys live in {@link NationText} (English fallbacks) and in {@code tools/elite/nations_lang.json}.
 */
public record Res(boolean ok, String key, Object[] args) {
    public static Res ok(String key, Object... args) { return new Res(true, key, args); }

    public static Res fail(String key, Object... args) { return new Res(false, key, args); }

    @Override public String toString() { return (ok ? "ok " : "fail ") + key; }
}
