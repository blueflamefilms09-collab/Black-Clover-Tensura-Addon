package com.newuniverse.nusmp.elite.hub;

import java.util.Locale;

/**
 * Why: the quest overlay must show the owner's example exactly ("0/50 Tier 1 Abyssal Kills") and the dialogue needs a title for every quest, including a datapack quest that wrote none, so the words
 * are built here from the quest's own fields, in English, with no game type. {@link #label} is the noun phrase the HUD puts after the count (the count comes from the player's progress);
 * {@link #title} is the line the Quest Giver shows: the quest's own title with {@code {n}} (the target), {@code {tier}}, {@code {road}}, {@code {faction}}, {@code {item}} and {@code {floor}} filled in, or a
 * generated one. The English text travels in the sync packets, so a client needs no lang entry per quest (the same approach as the System's floor quests).
 */
public final class QuestText {
    private QuestText() {}

    /** The HUD noun phrase, e.g. {@code Tier 1 Abyssal Kills}, {@code Foundry Kills}, {@code Floors Cleared}, {@code Iron Ingot Delivered}, {@code Reach Floor +10}. */
    public static String label(QuestDef q) {
        StringBuilder sb = new StringBuilder();
        switch (q.kind()) {
            case KILL -> {
                if (q.tier() > 0) sb.append("Tier ").append(q.tier()).append(' ');
                if (!q.road().isEmpty()) sb.append(cap(q.road())).append(' ');
                if (!q.faction().isEmpty()) sb.append(cap(q.faction())).append(' ');
                if (q.mob().equals("foundry")) sb.append("Foundry ");
                if (q.mob().equals("boss")) sb.append("Guardian ");
                sb.append("Kills");
            }
            case CLEAR_FLOORS -> {
                if (q.tier() > 0) sb.append("Tier ").append(q.tier()).append(' ');
                if (!q.road().isEmpty()) sb.append(cap(q.road())).append(' ');
                sb.append("Floors Cleared");
            }
            case DELIVER -> sb.append(itemName(q.item())).append(" Delivered");
            case REACH_FLOOR -> sb.append("Reach Floor ").append(floorLabel(q.floor()));
        }
        return sb.toString();
    }

    /** The quest's title with the placeholders filled in, or a generated one when it has none. */
    public static String title(QuestDef q) {
        if (!q.title().isEmpty()) return fill(q.title(), q);
        String label = label(q);
        return switch (q.kind()) {
            case KILL -> "Defeat " + q.target() + " " + (label.equals("Kills") ? "monsters" : label.substring(0, label.length() - " Kills".length()) + " monsters");
            case CLEAR_FLOORS -> "Clear " + q.target() + " " + (label.equals("Floors Cleared") ? "floors" : label.substring(0, label.length() - " Floors Cleared".length()) + " floors");
            case DELIVER -> "Deliver " + q.target() + " " + itemName(q.item());
            case REACH_FLOOR -> "Reach floor " + floorLabel(q.floor());
        };
    }

    /** Replaces the placeholders of a title or description. */
    public static String fill(String text, QuestDef q) {
        return text.replace("{n}", Integer.toString(q.target()))
                .replace("{tier}", Integer.toString(q.tier()))
                .replace("{road}", cap(q.effectiveRoad()))
                .replace("{faction}", cap(q.faction()))
                .replace("{item}", itemName(q.item()))
                .replace("{floor}", floorLabel(q.floor()));
    }

    /** {@code +12}, {@code -3}; the Hub is {@code 0}. */
    public static String floorLabel(int floor) { return floor > 0 ? "+" + floor : Integer.toString(floor); }

    /** {@code minecraft:iron_ingot} as {@code Iron Ingot}. */
    public static String itemName(String id) {
        if (id == null || id.isEmpty()) return "Item";
        String path = id.substring(id.indexOf(':') + 1);
        StringBuilder sb = new StringBuilder();
        for (String w : path.split("[_/]")) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(cap(w));
        }
        return sb.toString();
    }

    private static String cap(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    /** {@code 12/50 Tier 1 Abyssal Kills}: the line of the overlay. */
    public static String progressLine(int progress, int target, String label) { return progress + "/" + target + " " + label; }
}
