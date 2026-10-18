package com.newuniverse.nusmp.elite.forge.rules;

/**
 * Why: the four crafting minigames of the inventory (HammerTiming, MagiculeFlow, RuneAlignment, TemperatureForge). The id is what a recipe file names in its
 * {@code minigames} list, so it is part of the data format. Pure Java.
 */
public enum MinigameType {
    HAMMER_TIMING("hammer_timing"),
    MAGICULE_FLOW("magicule_flow"),
    RUNE_ALIGNMENT("rune_alignment"),
    TEMPERATURE_FORGE("temperature_forge");

    private final String id;

    MinigameType(String id) { this.id = id; }

    public String id() { return id; }

    public static MinigameType byId(String id) {
        if (id == null) return null;
        for (MinigameType t : values()) if (t.id.equalsIgnoreCase(id)) return t;
        return null;
    }
}
