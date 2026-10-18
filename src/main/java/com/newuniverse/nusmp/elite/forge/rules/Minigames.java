package com.newuniverse.nusmp.elite.forge.rules;

/**
 * Why: one lookup from a {@link MinigameType} to the stateless rules object that plays and scores it, used by the server check, the client screens and the tests.
 */
public final class Minigames {
    private Minigames() {}

    public static final HammerTiming HAMMER = new HammerTiming();
    public static final MagiculeFlow FLOW = new MagiculeFlow();
    public static final RuneAlignment RUNES = new RuneAlignment();
    public static final TemperatureForge TEMPERATURE = new TemperatureForge();

    public static Minigame of(MinigameType type) {
        return switch (type) {
            case HAMMER_TIMING -> HAMMER;
            case MAGICULE_FLOW -> FLOW;
            case RUNE_ALIGNMENT -> RUNES;
            case TEMPERATURE_FORGE -> TEMPERATURE;
        };
    }
}
