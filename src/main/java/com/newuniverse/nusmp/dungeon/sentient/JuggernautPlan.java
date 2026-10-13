package com.newuniverse.nusmp.dungeon.sentient;

import java.util.List;
import java.util.Set;

/**
 * The rules of the Juggernaut that the Dungeon's immune system sends: which kinds of creature it is made from on each floor, how much it
 * is grown (the health attribute stops at 1024), how many may live at once and how soon the same player can draw another. Pure.
 */
public final class JuggernautPlan {
    private JuggernautPlan() {}

    /** The most Juggernauts alive (or being born) in the Dungeon at once. */
    public static final int MAX_ALIVE = 3;
    /** The ceiling of the health attribute. */
    public static final double HEALTH_CAP = 1024.0;
    private static final int TICKS_PER_MINUTE = 1200;

    /**
     * How a Juggernaut is grown, as attribute modifiers: healthExtra and damageExtra and speedExtra are shares of the base value (3.0 = four
     * times the health), armor and knockback are added, and the follow range is raised to followRange.
     */
    public record Profile(double healthExtra, double damageExtra, double speedExtra, double armor, double knockback, double followRange) {}

    private static final Profile STANDARD = new Profile(3.0, 1.2, 0.12, 6.0, 0.6, 64.0);

    /** Health x4, attack damage x2.2, speed +12%, armor +6, knockback resistance +0.6, follow range 64 (the same on every floor for now). */
    public static Profile profile(FloorKind kind) { return STANDARD; }

    /** The share of base health to add so that a creature whose health is now {@code current} stays at or under the attribute's ceiling. */
    public static double healthExtra(double current, double wanted) {
        if (current <= 0.0 || wanted <= 0.0) return 0.0;
        return Math.max(0.0, Math.min(wanted, HEALTH_CAP / current - 1.0));
    }

    /** May another Juggernaut be born: fewer than the cap alive or on the way, and the player's cooldown over. */
    public static boolean mayBirth(int alive, int pending, long now, long cooldownUntil) { return alive + pending < MAX_ALIVE && now >= cooldownUntil; }

    /** The game time a player may draw the next Juggernaut, given the cooldown in minutes. */
    public static long cooldownUntil(long now, int minutes) { return now + (long) Math.max(0, minutes) * TICKS_PER_MINUTE; }

    /** The path of the entity-type tag that lists the kinds for a floor (a modpack edits it; anything that is not a floor of its own uses the Catacombs'). */
    public static String tagPath(FloorKind kind) {
        return switch (kind) {
            case WATERWAYS -> "dungeon/juggernaut_waterways";
            case UNDERWORLD -> "dungeon/juggernaut_underworld";
            default -> "dungeon/juggernaut_catacombs";
        };
    }

    /** The kinds of the floor if the tag cannot be read (the same as the tag files say). */
    public static List<String> defaults(FloorKind kind) {
        return switch (kind) {
            case WATERWAYS -> List.of("tensura:megalodon", "tensura:sissie");
            case UNDERWORLD -> List.of("tensura:greater_daemon", "tensura:lesser_daemon");
            default -> List.of("tensura:giant_bear", "tensura:horned_bear", "tensura:war_gnome", "tensura:orc", "tensura:direwolf");
        };
    }

    private static final Set<String> WATER_ONLY = Set.of("megalodon", "sissie", "charybdis", "giant_cod", "giant_salmon", "sea_serpent", "siren");

    /** Does the creature (by the path of its id, "megalodon") live only in water, so that it must be born into water? */
    public static boolean needsWater(String path) { return WATER_ONLY.contains(path); }
}
