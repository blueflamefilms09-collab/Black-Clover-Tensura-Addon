package com.newuniverse.nusmp.skill.rules;

/**
 * Why: Ki (Dragon Ball, 3.47.0) is an Ultimate with ten modes, and every number and decision of it is pure, so it is tested without a world. Ki Blast is the quick shot and Kamehameha the beam;
 * Instant Transmission is a blink, Solar Flare blinds, Afterimage lets one blow pass through you; Super Saiyan, Kaioken and Ultra Instinct are the three forms (strength with some protection, a
 * strain for a burst of power, and the body that dodges alone); Spirit Bomb grows with the living who lend it energy and Ki Sense shows who is near. Windows are stored as the game time they end
 * at, so nothing ticks and nothing needs cleaning up. Worst case of any press: at most 24 creatures looked at, 24 hit (Spirit Bomb, 90-second cooldown); the damage hook is a few comparisons.
 */
public final class KiRules {
    private KiRules() {}

    public static final int BLAST = 0, KAME = 1, TELEPORT = 2, FLARE = 3, AFTERIMAGE = 4, SSJ = 5, SPIRIT = 6, KAIOKEN = 7, UI = 8, SENSE = 9;
    public static final int MODES = 10;
    /** The ids of the modes, in order (the lang keys are nusmp.skill.mode.ki.&lt;id&gt;). */
    public static final String[] IDS = {"blast", "kame", "teleport", "flare", "afterimage", "ssj", "spirit", "kaioken", "ui", "sense"};

    public static boolean valid(int mode) { return mode >= 0 && mode < MODES; }

    /** The skill-tag key holding the game time a window of this mode ends at. */
    public static String untilKey(int mode) { return "KiUntil" + mode; }

    /** The skill-tag key holding the game time of the last blow Ultra Instinct dodged. */
    public static final String UI_DODGE_KEY = "KiUiDodgeAt";

    // ---- cost (Magicule) and cooldown (ticks) ----
    public static double cost(int mode) {
        return switch (mode) {
            case BLAST -> 400; case KAME -> 2500; case TELEPORT -> 1200; case FLARE -> 800; case AFTERIMAGE -> 900;
            case SSJ -> 3000; case SPIRIT -> 6000; case KAIOKEN -> 1500; case UI -> 4000; case SENSE -> 600; default -> 0;
        };
    }

    public static int cooldown(int mode) {
        return switch (mode) {
            case BLAST -> 40; case KAME -> 400; case TELEPORT -> 200; case FLARE -> 400; case AFTERIMAGE -> 400;
            case SSJ -> 1200; case SPIRIT -> 1800; case KAIOKEN -> 900; case UI -> 1800; case SENSE -> 300; default -> 0;
        };
    }

    /** How long the window of a timed mode lasts, in ticks (0 = the mode is an instant action). */
    public static int duration(int mode) {
        return switch (mode) { case AFTERIMAGE -> 80; case SSJ -> 600; case KAIOKEN -> 200; case UI -> 160; default -> 0; };
    }

    public static boolean timed(int mode) { return duration(mode) > 0; }

    /** Windows that a second press ends early (the cooldown still starts). Afterimage waits for the blow it turns away. */
    public static boolean cancellable(int mode) { return mode == SSJ || mode == KAIOKEN || mode == UI; }

    // ---- windows ----
    public static boolean active(long until, long now) { return until > now; }

    public static long endsAt(long now, int mode) { return now + duration(mode); }

    // ---- defence ----
    /** Afterimage turns the next blow to a fifth. */
    public static double afterimageMultiplier() { return 0.2; }

    /**
     * What a hit is multiplied by while forms are open: Super Saiyan 0.85, Kaioken 1.25 (the strain), Ultra Instinct 0.5. Never below 0.1 and never above 1.5. Afterimage and Ultra Instinct's
     * dodge are handled apart, since they refuse a blow outright.
     */
    public static double takenMultiplier(boolean ssj, boolean kaioken, boolean ui) {
        double m = 1.0;
        if (ssj) m *= 0.85;
        if (kaioken) m *= 1.25;
        if (ui) m *= 0.5;
        return Math.max(0.1, Math.min(1.5, m));
    }

    /** Ultra Instinct dodges a blow outright when the last blow it dodged was at least a second ago. */
    public static int uiDodgeGap() { return 20; }
    public static boolean uiDodges(long lastDodge, long now) { return now - lastDodge >= uiDodgeGap(); }

    // ---- offence ----
    public static double blastDamage() { return 10; }
    public static double blastRange() { return 32; }
    public static double blastWidth() { return 0.8; }

    /** Kamehameha: a line 28 blocks long; it pierces six. */
    public static double kameDamage() { return 22; }
    public static double kameRange() { return 28; }
    public static double kameWidth() { return 1.6; }
    public static int kamePierce() { return 6; }

    /** Instant Transmission: the longest blink (the shared safe-blink helper caps it at 32), and the shortest that counts. */
    public static double teleportRange() { return 32; }
    public static double teleportMin() { return 1.5; }
    public static boolean teleportOk(double moved) { return moved >= teleportMin(); }

    /** Solar Flare: foes in a cone in front of you (within 12 blocks, at most 24) are blinded and slowed; a player is blinded half as long. */
    public static double flareRange() { return 12; }
    public static double flareCone() { return 0.4; }
    public static int flareBlindTicks() { return 80; }
    public static int flareSlowTicks() { return 40; }

    /** Super Saiyan: Strength II and Speed I for the window. */
    public static int ssjStrengthAmp() { return 1; }
    public static int ssjSpeedAmp() { return 0; }

    /** Kaioken: Strength III and Speed II for the window, paid for with health up front (never to the last heart). */
    public static int kaiokenStrengthAmp() { return 2; }
    public static int kaiokenSpeedAmp() { return 1; }
    public static double kaiokenHealthShare() { return 0.15; }

    /** Kaioken needs more than two hearts to burn: at four health points or fewer it refuses. */
    public static boolean kaiokenAllowed(float health) { return health > 4f; }

    /** The health Kaioken costs, leaving at least two points: 15% of the maximum, never more than {@code health - 2}, never negative. */
    public static float kaiokenHealthCost(float health, float maxHealth) {
        float cost = (float) (maxHealth * kaiokenHealthShare());
        return Math.max(0f, Math.min(cost, health - 2f));
    }

    /** Spirit Bomb: it is thrown where you look (within 32 blocks) with this radius, and grows with each living ally lending energy (at most eight). */
    public static double spiritReach() { return 32; }
    public static double spiritRadius() { return 8; }
    public static int spiritLendMax() { return 8; }
    public static double spiritBase() { return 24; }
    public static double spiritPerLender() { return 4; }

    /** The damage of a Spirit Bomb fed by {@code lenders} living allies (you do not count): 24 + 4 per lender, at most eight lenders. */
    public static double spiritDamage(int lenders) {
        int n = Math.max(0, Math.min(spiritLendMax(), lenders));
        return spiritBase() + spiritPerLender() * n;
    }

    public static double spiritLendRadius() { return 24; }

    /** Ki Sense: every hostile creature within this radius (at most 24) glows for ten seconds. */
    public static double senseRadius() { return 32; }
    public static int senseGlowTicks() { return 200; }
}
