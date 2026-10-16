package com.newuniverse.nusmp.avatar.naming;

import java.util.Collection;
import java.util.Locale;

/**
 * Why: names reach the server from a text box on the client, and a name ends up in chat barks, a name tag and the save. The rules (length 2 to 24, letters,
 * digits, spaces, apostrophes and hyphens, unique per creator) are checked here on the server and are pure so the GUI can show the same verdict.
 */
public final class NameRules {
    private NameRules() {}

    public static final int MIN = 2, MAX = 24;

    public enum Verdict { OK, TOO_SHORT, TOO_LONG, BAD_CHARACTERS, DUPLICATE }

    /** Trims, collapses runs of spaces and returns the cleaned name (null becomes empty). */
    public static String clean(String raw) { return raw == null ? "" : raw.trim().replaceAll("\\s+", " "); }

    public static Verdict check(String raw, Collection<String> namesOfCreator) {
        String name = clean(raw);
        if (name.length() < MIN) return Verdict.TOO_SHORT;
        if (name.length() > MAX) return Verdict.TOO_LONG;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == ' ' || c == '\'' || c == '-')) return Verdict.BAD_CHARACTERS;
        }
        if (namesOfCreator != null) {
            String key = name.toLowerCase(Locale.ROOT);
            for (String other : namesOfCreator) if (other != null && other.toLowerCase(Locale.ROOT).equals(key)) return Verdict.DUPLICATE;
        }
        return Verdict.OK;
    }

    public static String message(Verdict v) {
        return switch (v) {
            case OK -> "";
            case TOO_SHORT -> "A name needs at least " + MIN + " characters.";
            case TOO_LONG -> "A name can be at most " + MAX + " characters.";
            case BAD_CHARACTERS -> "Names use letters, digits, spaces, apostrophes and hyphens only.";
            case DUPLICATE -> "You already have an Avatar with that name.";
        };
    }
}
