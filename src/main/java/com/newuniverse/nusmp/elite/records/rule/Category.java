package com.newuniverse.nusmp.elite.records.rule;

import java.util.Locale;

/**
 * Why: the records screen groups its 170+ records into tabs, and a title requirement can ask for "N records of a category". The nine categories follow the owner's list of unlock conditions
 * (exploration: biomes, structures, distance; combat: kills and hits; survival: deaths; power: EP and the Demon Lord / Higher Being / True Hero statuses; skills; crafting: the forge;
 * dominion: FTB Chunks claims; war: war kills and wins and Walpurgis; calamity). Pure. Enum order is the tab order and the wire order: append only.
 */
public enum Category {
    EXPLORATION, COMBAT, SURVIVAL, POWER, SKILLS, CRAFTING, DOMINION, WAR, CALAMITY;

    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static Category parse(String s, Category fallback) {
        if (s == null) return fallback;
        String t = s.trim().toUpperCase(Locale.ROOT);
        for (Category c : values()) if (c.name().equals(t)) return c;
        return fallback;
    }

    public static Category byOrdinal(int i) { return values()[Math.max(0, Math.min(values().length - 1, i))]; }
}
