package com.newuniverse.nusmp.skill.rules;

/**
 * Why: Nen (Hunter x Hunter, 3.46.0) is an Ultimate with ten modes, and every number and decision of it is pure, so it is tested without a world. The modes follow the series: the four
 * basics (Ten keeps the aura round the body, Zetsu shuts it, Ren pours it out, Ko puts all of it in one place), the advanced uses (Gyo into the eyes, En as a sphere, Ken as a stance) and Hatsu,
 * the personal ability, as Enhancement and Emission, plus Vow and Limitation: a self-made restriction that makes the ability stronger. Windows (Ten, Zetsu, Ren, Ken, Ko, Vow) are stored as
 * the game time they end at, so nothing ticks and nothing needs cleaning up. Worst case of any press: at most 24 creatures looked at, three hit; the damage hook is a few comparisons.
 */
public final class NenRules {
    private NenRules() {}

    public static final int TEN = 0, ZETSU = 1, REN = 2, GYO = 3, EN = 4, KEN = 5, KO = 6, ENHANCE = 7, EMIT = 8, VOW = 9;
    /** 3.49.0, appended: the advanced techniques the first version left out (Shu, In, Ryu) and the four hatsu types it did not have. */
    public static final int SHU = 10, IN = 11, RYU = 12, TRANS = 13, CONJ = 14, MANIP = 15, SPEC = 16;
    public static final int MODES = 17;
    /** The ids of the modes, in order (the lang keys are nusmp.skill.mode.nen.&lt;id&gt;). */
    public static final String[] IDS = {"ten", "zetsu", "ren", "gyo", "en", "ken", "ko", "enhance", "emit", "vow", "shu", "in", "ryu", "trans", "conj", "manip", "spec"};

    public static boolean valid(int mode) { return mode >= 0 && mode < MODES; }

    /** The skill-tag key holding the game time a window of this mode ends at. */
    public static String untilKey(int mode) { return "NenUntil" + mode; }

    // ---- cost (Magicule) and cooldown (ticks) ----
    public static double cost(int mode) {
        return switch (mode) {
            case TEN -> 600; case ZETSU -> 500; case REN -> 1500; case GYO -> 400; case EN -> 1000; case KEN -> 1200;
            case KO -> 2000; case ENHANCE -> 1500; case EMIT -> 1800;
            case SHU -> 800; case IN -> 700; case RYU -> 1200; case TRANS -> 1700; case CONJ -> 1800; case MANIP -> 2000; case SPEC -> 2200; default -> 0;   // a vow costs no energy, it costs freedom
        };
    }

    public static int cooldown(int mode) {
        return switch (mode) {
            case TEN -> 200; case ZETSU -> 400; case REN -> 600; case GYO -> 300; case EN -> 400; case KEN -> 500;
            case KO -> 600; case ENHANCE -> 160; case EMIT -> 200; case VOW -> 1200;
            case SHU -> 300; case IN -> 300; case RYU -> 500; case TRANS -> 220; case CONJ -> 260; case MANIP -> 400; case SPEC -> 500; default -> 0;
        };
    }

    /** How long the window of a timed mode lasts, in ticks (0 = the mode is an instant action). */
    public static int duration(int mode) {
        return switch (mode) {
            case TEN -> 900; case ZETSU -> 400; case REN -> 400; case KEN -> 160; case KO -> 200; case VOW -> 600; case SHU -> 400; case IN -> 400; case RYU -> 300; default -> 0;
        };
    }

    public static boolean timed(int mode) { return duration(mode) > 0; }

    /** Windows that a second press ends early (the cooldown still starts). Ko waits for its strike and ends when it lands. */
    public static boolean cancellable(int mode) { return mode == TEN || mode == ZETSU || mode == REN || mode == KEN || mode == VOW || mode == SHU || mode == IN || mode == RYU; }

    // ---- windows ----
    public static boolean active(long until, long now) { return until > now; }

    public static long endsAt(long now, int mode) { return now + duration(mode); }

    // ---- defence ----
    /** What a hit is multiplied by while a window is open: Ten 0.8, Ken 0.4, Ko 1.2 (all the aura is in the fist), Vow 1.25. Never below 0.25 and never above 1.5. */
    public static double takenMultiplier(boolean ten, boolean ken, boolean ko, boolean vow) {
        double m = 1.0;
        if (ten) m *= 0.8;
        if (ken) m *= 0.4;
        if (ko) m *= 1.2;
        if (vow) m *= 1.25;
        return Math.max(0.25, Math.min(1.5, m));
    }

    /**
     * 3.49.0: the same with Zetsu (the body is open, a hit does 1.3) and Ryu (the aura flows to where the blow lands, a hit does 0.85). The clamp is the same [0.25, 1.5].
     */
    public static double takenMultiplier(boolean ten, boolean ken, boolean ko, boolean vow, boolean zetsu, boolean ryu) {
        double m = takenMultiplier(ten, ken, ko, vow);
        if (zetsu) m *= 1.3;
        if (ryu) m *= 0.85;
        return Math.max(0.25, Math.min(1.5, m));
    }

    /** True when the vow forbids this mode: the defensive ones (Ten, Zetsu, Ken). */
    public static boolean vowForbids(int mode) { return mode == TEN || mode == ZETSU || mode == KEN; }

    // ---- offence ----
    /** Hatsu abilities (Enhancement, Emission, Ko's strike) are multiplied while a vow holds. */
    public static double hatsuMultiplier(boolean vow) { return vow ? 1.75 : 1.0; }

    public static double enhanceDamage() { return 14; }
    public static double enhanceReach() { return 4.5; }
    public static double enhanceKnockback() { return 1.2; }
    public static double emitDamage() { return 12; }
    public static double emitRange() { return 24; }
    public static double emitWidth() { return 1.2; }
    public static int emitPierce() { return 3; }
    public static double koDamage() { return 30; }

    /** Ren: Strength II and Speed I for the window. */
    public static int renStrengthAmp() { return 1; }
    public static int renSpeedAmp() { return 0; }
    /** Ken is a stance: Slowness II while it holds. */
    public static int kenSlowAmp() { return 1; }

    // ---- senses ----
    public static double enRadius() { return 16; }
    public static int enGlowTicks() { return 200; }
    public static double gyoRadius() { return 24; }
    public static int gyoNightTicks() { return 1200; }
    public static int gyoRevealTicks() { return 160; }
    public static int gyoMaxRevealed() { return 16; }
    /** Zetsu: foes this near that were hunting you forget you. */
    public static double zetsuRadius() { return 16; }

    // ---- 3.49.0: the six types (Water Divination), the wheel and the new modes ----
    /** The types in the order of the series' wheel (Enhancement at the top, then clockwise). */
    public static final int T_ENHANCE = 0, T_TRANS = 1, T_CONJ = 2, T_SPEC = 3, T_MANIP = 4, T_EMIT = 5;
    public static final String[] TYPE_IDS = {"enhancement", "transmutation", "conjuration", "specialization", "manipulation", "emission"};

    /** The type the Water Divination gives a player: stable for the player (their UUID), the same on every server. */
    public static int divine(long uuidMost, long uuidLeast) { return Math.floorMod((int) ((uuidMost * 31L) ^ uuidLeast ^ (uuidMost >>> 32) ^ (uuidLeast >>> 29)), 6); }

    /** The type of a hatsu mode, or -1 for a mode that is not a hatsu. */
    public static int typeOfMode(int mode) {
        return switch (mode) { case ENHANCE -> T_ENHANCE; case TRANS -> T_TRANS; case CONJ -> T_CONJ; case SPEC -> T_SPEC; case MANIP -> T_MANIP; case EMIT -> T_EMIT; default -> -1; };
    }

    public static boolean hatsu(int mode) { return typeOfMode(mode) >= 0; }

    /** How well a user of type {@code own} uses a hatsu of type {@code used}: 100 % on their own, 80 % next to it on the wheel, 60 %, 40 % opposite. */
    public static double efficiency(int own, int used) {
        if (used < 0) return 1.0;
        int d = Math.floorMod(own - used, 6);
        d = Math.min(d, 6 - d);
        return switch (d) { case 0 -> 1.0; case 1 -> 0.8; case 2 -> 0.6; default -> 0.4; };
    }

    public static double shuBonus() { return 4; }
    /** In: the hatsu you cannot see hit harder (a quarter of a vow's gain). */
    public static double inMultiplier() { return 1.15; }
    public static double ryuDealtMultiplier() { return 1.2; }
    public static double transDamage() { return 9; }
    public static double transRange() { return 12; }
    public static int transMax() { return 4; }
    public static int transSlowTicks() { return 60; }
    public static double conjDamage() { return 8; }
    public static double conjRange() { return 16; }
    public static int conjRootTicks() { return 80; }
    public static double manipRange() { return 16; }
    public static double manipSearch() { return 8; }
    public static int manipTicks() { return 120; }
    public static double specRadius() { return 12; }
    public static int specMax() { return 8; }
    public static double specDamage() { return 6; }
}
