package com.newuniverse.nusmp.labyrinth.mega.crucible;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: a sector's Relic Theme should be felt in what its vault offers (a Frost sector offers Frostfang, a Storm sector Mjolnir) without a table that
 * would have to be edited for every relic ever added: the theme names KEYWORDS ({@code "frost", "snow", "winter"}) and a relic counts as "of the theme"
 * when a word of its id or card name BEGINS with one. Word-prefix matching rather than plain substring because three-letter keywords hide in common
 * words ({@code ice} is in "Voice of the Mountain", {@code sun} in "Sunder", {@code star} would match "start"); a prefix of a whole word keeps
 * {@code frost} matching "Frostfang" and {@code moon} matching "Moonlight" while {@code ice} no longer matches "Voice". Pure, so the rule is
 * tested with real relic names.
 */
public final class RelicKeywords {
    private RelicKeywords() {}

    /** The lower-case words of a text: every run of letters and digits ({@code "Staff of Moonlight"} and {@code "staff_of_moonlight"} both give staff, of, moonlight). */
    public static List<String> words(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = Character.toLowerCase(text.charAt(i));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) b.append(c);
            else if (b.length() > 0) { out.add(b.toString()); b.setLength(0); }
        }
        if (b.length() > 0) out.add(b.toString());
        return out;
    }

    /** Does any word of the relic's id or card name begin with any keyword? An empty keyword list matches nothing (the caller then uses the whole band). */
    public static boolean matches(String relicId, String cardName, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return false;
        List<String> words = words(relicId);
        words.addAll(words(cardName));
        for (String k : keywords) {
            String key = k == null ? "" : k.trim().toLowerCase(Locale.ROOT);
            if (key.isEmpty()) continue;
            for (String w : words) if (w.startsWith(key)) return true;
        }
        return false;
    }
}
