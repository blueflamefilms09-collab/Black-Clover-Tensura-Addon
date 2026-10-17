package com.newuniverse.nusmp.arc.dialogue;

import java.util.Set;

/**
 * Why: one thing a character can say, together with the conditions under which it may be said. Several lines for the same situation are the "variants"
 * of the brief; the conditions are what make a character react to the story (Yami knows Heath Grice has fallen only after the player beat him). The line
 * keeps its English text AND an optional language key: with a key the game asks the language file first and falls back to the text (so a translation
 * works), without a key the text is sent as it is (so a datapack that replaces the file with new words is heard exactly as written, which a key would
 * have prevented, because a translation always wins over a fallback).
 *
 * <p>Text may contain {@code {player}}, {@code {npc}} and {@code {saga}}; {@link #template()} turns them into the positional arguments a language entry
 * uses ({@code %1$s}, {@code %2$s}, {@code %3$s}).
 *
 * @param key        the language key ({@code arc.dialogue.<id>.<n>}) or empty
 * @param text       the English words
 * @param minStage   least story stage the line needs ({@link #NO_MIN} = none)
 * @param maxStage   greatest story stage it can still be said at ({@link #NO_MAX} = none)
 * @param sagas      the line is for these sagas only (empty = any)
 * @param defeated   every one of these characters must have been beaten
 * @param notDefeated none of these may have been beaten
 * @param minQuest   least quest stage with this character
 * @param maxQuest   greatest quest stage with this character
 * @param first      {@link #FIRST_ANY}, {@link #FIRST_ONLY} (first meeting only) or {@link #FIRST_NEVER}
 * @param priority   the higher wins; defaults to the number of conditions, so the most specific line is chosen
 * @param weight     how likely the line is among equals (at least 1)
 */
public record DialogueLine(String key, String text, int minStage, int maxStage, Set<String> sagas, Set<String> defeated, Set<String> notDefeated,
                           int minQuest, int maxQuest, int first, int priority, int weight) {
    public static final int NO_MIN = Integer.MIN_VALUE, NO_MAX = Integer.MAX_VALUE;
    public static final int FIRST_ANY = -1, FIRST_NEVER = 0, FIRST_ONLY = 1;
    /** The placeholders a text may use. */
    public static final java.util.List<String> PLACEHOLDERS = java.util.List.of("player", "npc", "saga");

    public DialogueLine {
        key = key == null ? "" : key.trim();
        text = text == null ? "" : text;
        sagas = sagas == null ? Set.of() : Set.copyOf(sagas);
        defeated = defeated == null ? Set.of() : Set.copyOf(defeated);
        notDefeated = notDefeated == null ? Set.of() : Set.copyOf(notDefeated);
        weight = Math.max(1, weight);
    }

    /** A line with no conditions at all. */
    public static DialogueLine unconditional(String key, String text) {
        return new DialogueLine(key, text, NO_MIN, NO_MAX, Set.of(), Set.of(), Set.of(), NO_MIN, NO_MAX, FIRST_ANY, 0, 1);
    }

    /** How many conditions the line carries (the default priority). */
    public static int countConditions(int minStage, int maxStage, Set<String> sagas, Set<String> defeated, Set<String> notDefeated, int minQuest, int maxQuest, int first) {
        int n = 0;
        if (minStage != NO_MIN) n++;
        if (maxStage != NO_MAX) n++;
        if (!sagas.isEmpty()) n++;
        if (!defeated.isEmpty()) n++;
        if (!notDefeated.isEmpty()) n++;
        if (minQuest != NO_MIN) n++;
        if (maxQuest != NO_MAX) n++;
        if (first != FIRST_ANY) n++;
        return n;
    }

    /** Does the situation satisfy every condition of the line? */
    public boolean matches(DialogueSituation s) {
        if (s.stage() < minStage || s.stage() > maxStage) return false;
        if (s.questStage() < minQuest || s.questStage() > maxQuest) return false;
        if (!sagas.isEmpty() && !sagas.contains(s.saga())) return false;
        if (first == FIRST_ONLY && !s.first()) return false;
        if (first == FIRST_NEVER && s.first()) return false;
        if (!s.defeated().containsAll(defeated)) return false;
        for (String id : notDefeated) if (s.defeated().contains(id)) return false;
        return true;
    }

    /**
     * The text as a language template: {@code %} is doubled and the placeholders become {@code %1$s} (player), {@code %2$s} (npc), {@code %3$s} (saga).
     * This is also what the language file of the line must contain.
     */
    public String template() {
        String t = text.replace("%", "%%");
        t = t.replace("{player}", "%1$s").replace("{npc}", "%2$s").replace("{saga}", "%3$s");
        return t;
    }

    /** The text with the placeholders filled in (for a line sent without a language key, and for logs). */
    public String plain(String player, String npc, String saga) {
        return text.replace("{player}", player == null ? "" : player).replace("{npc}", npc == null ? "" : npc).replace("{saga}", saga == null ? "" : saga);
    }
}
