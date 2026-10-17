package com.newuniverse.nusmp.labyrinth.mega.system;

import java.util.List;
import java.util.Objects;

/**
 * Why: a floor quest is DATA, not a branch in code: the brief's example ("Clear 5 rooms without taking magic damage") is one row of
 * {@code data/nusmp/mega/system/quests.json}, and a datapack can retune a number, add a ninth quest or silence one (weight 0) without a mod update. This record
 * is one parsed row. The target the player sees is {@code clamp(round(base + perFloor * threat), min, max)} (see {@link QuestRoller#target}), so a quest grows
 * with the depth and a negative {@code perFloor} makes a timed quest shorter the deeper you are. {@code title} is the English text with {@code {n}} (and
 * {@code {t}}) standing for that number; the lang key {@code message.nusmp.mega.system.quest.<id>} takes over where a translation exists, and the English
 * text travels with the sync so a datapack quest with no lang entry still reads properly. {@code stats} and {@code points} say what a success pays when the
 * reward roll picks a stat point, {@code shadowCore} says whether a Shadow Core may be paid instead. Immutable; the constructor normalises, so no consumer has
 * to defend against an empty list or a negative number.
 *
 * @param noMagic true for the "without taking magic damage" variant of the counting kinds: the first magic hit fails the quest
 */
public record QuestTemplate(String id, QuestKind kind, String title, double base, double perFloor, int min, int max, int weight,
                            boolean noMagic, List<Stat> stats, int points, boolean shadowCore, String why) {
    public QuestTemplate {
        Objects.requireNonNull(kind, "kind");
        id = id == null ? "" : id;
        title = title == null ? "" : title;
        why = why == null ? "" : why;
        min = Math.max(1, min);
        max = Math.max(min, max);
        weight = Math.max(0, weight);
        points = Math.max(1, points);
        stats = stats == null || stats.isEmpty() ? kind.defaultStats() : List.copyOf(stats);
    }

    /** The lang key of this quest's title; its format takes the target number as {@code %1$s}. */
    public String langKey() { return SystemText.PREFIX + "quest." + id; }

    /** The English title with the target number put in place of {@code {n}} and {@code {t}}. */
    public String englishTitle(int target) {
        if (title.isEmpty()) return kind.defaultTitle(target);
        String n = Integer.toString(target);
        return title.replace("{n}", n).replace("{t}", n);
    }
}
