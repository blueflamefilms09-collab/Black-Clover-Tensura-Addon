package com.newuniverse.nusmp.elite.races.race;

import com.newuniverse.nusmp.elite.races.data.RaceTemplate;

/** Why: English fallbacks for a race's name and description when the language file has no entry (the fragment tools/elite/races_lang.json is merged by the lead), so a player never sees a raw key. */
public final class RaceText {
    private RaceText() {}

    /** {@code lesser_saiyan} to {@code Lesser Saiyan}. */
    public static String title(String id) {
        StringBuilder sb = new StringBuilder();
        for (String part : id.split("_")) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    public static String describe(RaceTemplate t) {
        if (t == null) return "";
        return title(t.id()) + ": tier " + t.tier() + " of the " + title(t.line()) + " line.";
    }
}
