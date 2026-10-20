package com.newuniverse.nusmp.balance;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: the owner's hunger ban ("absolutely no accelerated hunger or exhaustion mechanics": nothing in the mod may make the food bar drain faster or force
 * the player to eat more often; rests and every other system recover health, mana and cooldowns only) is a rule of arithmetic, so it lives here as plain
 * numbers that JUnit can pin. A {@link Bar} is one reading of a player's food bar (food level, saturation, exhaustion); {@link #violations} compares two readings
 * taken with no vanilla tick between them and names everything that got worse, so the server self-test {@code dungeon.nohunger} can drive the old triggers
 * (a rest, the gloom clock, a curse, a trap) and assert that our code never lowered the food or saturation or raised the exhaustion. The effect ids the mod
 * may never put on a player are named here too, so the choke points that apply an effect by id ask one place. See {@code docs/labyrinth/hunger_ban.md}.
 */
public final class HungerLaw {
    private HungerLaw() {}

    /** The vanilla Hunger effect (it adds exhaustion every tick, so it is an accelerated hunger). Never applied to a player by this mod. */
    public static final String HUNGER_EFFECT = "minecraft:hunger";

    /** One reading of a food bar. */
    public record Bar(int food, float saturation, float exhaustion) {}

    /** Is this effect id the Hunger effect (with or without the namespace, any case)? */
    public static boolean isHungerEffectId(String id) {
        if (id == null) return false;
        String s = id.trim().toLowerCase(Locale.ROOT);
        return s.equals(HUNGER_EFFECT) || s.equals("hunger");
    }

    /**
     * What got worse between two readings of the same bar: the food level or the saturation lower, the exhaustion higher. A drop of the exhaustion is allowed
     * (a food point was spent by vanilla itself or the bar was fed), so the check is only meaningful when no vanilla tick ran between the readings.
     * Empty means the ban held.
     */
    public static List<String> violations(Bar before, Bar after) {
        List<String> out = new ArrayList<>(3);
        if (before == null || after == null) return out;
        if (after.food() < before.food()) out.add("food level fell " + before.food() + " -> " + after.food());
        if (after.saturation() < before.saturation() - 1.0e-4f) out.add("saturation fell " + before.saturation() + " -> " + after.saturation());
        if (after.exhaustion() > before.exhaustion() + 1.0e-4f) out.add("exhaustion rose " + before.exhaustion() + " -> " + after.exhaustion());
        return out;
    }

    /** The reading a repaired bar gets: never worse than {@code before} on any of the three numbers (what the guard writes back). */
    public static Bar repaired(Bar before, Bar after) {
        return new Bar(Math.max(before.food(), after.food()), Math.max(before.saturation(), after.saturation()), Math.min(before.exhaustion(), after.exhaustion()));
    }
}
