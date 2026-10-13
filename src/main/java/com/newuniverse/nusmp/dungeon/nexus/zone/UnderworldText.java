package com.newuniverse.nusmp.dungeon.nexus.zone;

/** The words of the Underworld Gate: what the ruin says to those who walk in it. Pure, so the wording is checked in tests. */
public final class UnderworldText {
    private UnderworldText() {}

    /** The Devil that guards the Treasury's gate, and the name of its bar. */
    public static final String GATEKEEPER = "Gatekeeper of the Underworld";

    /** Said once, when someone first steps into the hall. */
    public static final String ENTER = "The gravity of this ruin is warped. Watch the glow in the floor: red presses down, blue lets go, violet turns it over.";

    /** What the floor says while it glows a warning (the gravity is about to change). */
    public static String warning(GravityCycle.Mode coming) {
        return switch (coming) {
            case HEAVY -> "The floor glows red: gravity is about to crush down.";
            case LIGHT -> "The floor glows blue: gravity is about to let go.";
            case REVERSAL -> "The floor glows violet: gravity is about to turn over.";
            case CALM -> "";
        };
    }

    /** What it says while the gravity is changed. */
    public static String active(GravityCycle.Mode mode) {
        return switch (mode) {
            case HEAVY -> "Gravity presses you down.";
            case LIGHT -> "Gravity lets go of you.";
            case REVERSAL -> "Gravity turns over!";
            case CALM -> "";
        };
    }

    /** The name of a Dark Disciple that channels a Devil (used when the stand-in Devil is spawned instead). */
    public static String discipleName(String devil, int percent) { return "Dark Disciple of " + devil + " (" + percent + "%)"; }
}
