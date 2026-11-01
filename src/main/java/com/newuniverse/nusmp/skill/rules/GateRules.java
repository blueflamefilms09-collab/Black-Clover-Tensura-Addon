package com.newuniverse.nusmp.skill.rules;

/**
 * Why: Gate of Babylon (Fate, 3.47.0) is an Ultimate with ten modes, and every number and decision of it is pure, so it is tested without a world. It is the King of Heroes' vault: a gate opens in
 * the air and a weapon falls (Gate), a volley of them (Volley) or a rain over a place (Rain); Enkidu's Chains of Heaven hold a creature, the golden armour guards you, Vimana lifts you, the King's
 * Aura lifts those who follow you and weighs on those who do not, Gae Bolg is a spear thrown through a line, Ea (Enuma Elish) is the one great blow, and the Elixir heals. Windows (the armour, the
 * Vimana) are stored as the game time they end at, so nothing ticks and nothing needs cleaning up. Worst case of any press: at most 24 creatures looked at, 12 hit (24 for Ea, which is on a 120-second
 * cooldown); the damage hook is one comparison.
 */
public final class GateRules {
    private GateRules() {}

    public static final int GATE = 0, VOLLEY = 1, RAIN = 2, CHAINS = 3, ARMOR = 4, VIMANA = 5, KING = 6, GAE = 7, EA = 8, ELIXIR = 9;
    public static final int MODES = 10;
    /** The ids of the modes, in order (the lang keys are nusmp.skill.mode.gate.&lt;id&gt;). */
    public static final String[] IDS = {"gate", "volley", "rain", "chains", "armor", "vimana", "king", "gae", "ea", "elixir"};

    public static boolean valid(int mode) { return mode >= 0 && mode < MODES; }

    /** The skill-tag key holding the game time a window of this mode ends at. */
    public static String untilKey(int mode) { return "GateUntil" + mode; }

    // ---- cost (Magicule) and cooldown (ticks) ----
    public static double cost(int mode) {
        return switch (mode) {
            case GATE -> 1200; case VOLLEY -> 2000; case RAIN -> 3000; case CHAINS -> 1800; case ARMOR -> 2000;
            case VIMANA -> 1500; case KING -> 2500; case GAE -> 2200; case EA -> 8000; case ELIXIR -> 2500; default -> 0;
        };
    }

    public static int cooldown(int mode) {
        return switch (mode) {
            case GATE -> 120; case VOLLEY -> 300; case RAIN -> 600; case CHAINS -> 500; case ARMOR -> 800;
            case VIMANA -> 600; case KING -> 900; case GAE -> 260; case EA -> 2400; case ELIXIR -> 1200; default -> 0;
        };
    }

    /** How long the window of a timed mode lasts, in ticks (0 = the mode is an instant action). */
    public static int duration(int mode) {
        return switch (mode) { case ARMOR -> 400; case VIMANA -> 300; default -> 0; };
    }

    public static boolean timed(int mode) { return duration(mode) > 0; }

    /** Windows that a second press ends early (the cooldown still starts). */
    public static boolean cancellable(int mode) { return mode == ARMOR || mode == VIMANA; }

    // ---- windows ----
    public static boolean active(long until, long now) { return until > now; }

    public static long endsAt(long now, int mode) { return now + duration(mode); }

    // ---- defence ----
    /** The golden armour: a hit does 0.7 of what it would. */
    public static double takenMultiplier(boolean armor) { return armor ? 0.7 : 1.0; }

    /** The armour also gives Absorption II (amplifier 1) for its window. */
    public static int armorAbsorptionAmp() { return 1; }

    // ---- offence ----
    /** Gate: one weapon falls on what you look at. */
    public static double gateDamage() { return 16; }
    public static double gateReach() { return 32; }

    /** Volley: up to six creatures in a cone in front of you are each struck by a weapon. */
    public static double volleyDamage() { return 11; }
    public static double volleyRange() { return 24; }
    public static double volleyCone() { return 0.7; }
    public static int volleyMax() { return 6; }

    /** Rain: a place you look at (within 32 blocks), radius 6; up to twelve creatures there are struck. */
    public static double rainDamage() { return 9; }
    public static double rainReach() { return 32; }
    public static double rainRadius() { return 6; }
    public static int rainMax() { return 12; }

    /** Chains of Heaven: the look-target is held (Slowness VII, so it cannot move) for this many ticks before {@link ControlRules#controlTicks}, and weakened. */
    public static double chainsReach() { return 24; }
    public static int chainsTicks() { return 100; }
    public static int chainsSlowAmp() { return 6; }
    public static int chainsWeakTicks() { return 200; }

    /** Vimana: a forward leap and Slow Falling for the window; the speed of the leap and its lift. */
    public static double vimanaSpeed() { return 1.8; }
    public static double vimanaLift() { return 0.55; }

    /** King's Aura: allies within this radius (at most 24) gain Strength I and Resistance I for twenty seconds; foes within it are weakened for ten. */
    public static double kingRadius() { return 12; }
    public static int kingBuffTicks() { return 400; }
    public static int kingWeakTicks() { return 200; }

    /** Gae Bolg: a spear thrown down a line; it pierces three. */
    public static double gaeDamage() { return 24; }
    public static double gaeRange() { return 24; }
    public static double gaeWidth() { return 1.0; }
    public static int gaePierce() { return 3; }

    /** Ea (Enuma Elish): one great line, 40 blocks long and wide; everything in it (at most 24) is struck. */
    public static double eaDamage() { return 48; }
    public static double eaRange() { return 40; }
    public static double eaWidth() { return 2.2; }

    /** Elixir: heals this share of your maximum health, cleanses, and regenerates for ten seconds. */
    public static double elixirHeal() { return 0.3; }
    public static int elixirRegenTicks() { return 200; }
}
