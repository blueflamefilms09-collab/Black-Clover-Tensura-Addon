package com.newuniverse.nusmp.elite.hub;

import java.util.Locale;

/**
 * Why: the brief leaves it to the author whether the Dungeon Map is used up ("one use per floor or durable (your call, configurable, documented)"). Three modes are offered and the choice is the
 * server owner's: {@link #DURABLE} (the default: the map is a tool, never used up; the floor it shows is always the one you stand on), {@link #PER_FLOOR} (one map is used up the first time you map
 * a floor; opening it again on a floor you already mapped is free, so the price is per floor, and a map is a thing to run out of on a long dive) and {@link #CONSUMABLE} (every use uses one up).
 * The default is durable because the Hub shop already charges 15 silver for it and a daily limit of eight, and because a map that eats itself while a player is lost in a maze punishes exactly the
 * player it is meant to help. Pure: the parse and the cost are tested.
 */
public enum MapMode {
    DURABLE("durable"), PER_FLOOR("per_floor"), CONSUMABLE("consumable");

    private final String key;

    MapMode(String key) { this.key = key; }

    public String key() { return key; }

    /** The mode a config word names (case-insensitive, spaces and dashes read as underscores), {@link #DURABLE} for null or anything unknown. */
    public static MapMode parse(String s) {
        if (s == null) return DURABLE;
        String t = s.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (MapMode m : values()) if (m.key.equals(t)) return m;
        return DURABLE;
    }

    /** How many maps one use consumes: durable none, consumable one, per-floor one the first time on a floor and none after. */
    public int cost(boolean alreadyMapped) {
        return switch (this) {
            case DURABLE -> 0;
            case CONSUMABLE -> 1;
            case PER_FLOOR -> alreadyMapped ? 0 : 1;
        };
    }
}
