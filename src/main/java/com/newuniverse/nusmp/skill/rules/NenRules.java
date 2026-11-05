package com.newuniverse.nusmp.skill.rules;

/**
 * Why: Nen (Hunter x Hunter, 3.46.0; reworked 3.57.0, docs/nen_rework.md) is an Ultimate with seventeen modes, and every number and decision of it is pure, so it is tested without a world.
 * The modes follow the series: the four basics (Ten keeps the aura round the body, Zetsu shuts it, Ren pours it out, Ko puts all of it in one place), the advanced uses (Gyo into the eyes,
 * En as a sphere, Ken as a stance, Shu into a held thing, In to hide it, Ryu to move it) and Hatsu, the personal ability, in the six types of the wheel, plus Vow and Limitation: a self-made
 * restriction that makes the ability stronger. Windows (Ten, Zetsu, Ren, En, Ken, Ko, Vow, Shu, In, Ryu) are stored as the game time they end at. 3.57.0: the six hatsu are signature moves
 * (Enhancement a fist that spends the aura Ko stored, Emission a homing volley, Transmutation electricity / gum / rubber by stance, Conjuration chains, Manipulation a needle, Specialization a
 * field that steals buffs); their short running state (bullets left, chains held, needle, field) lives in the owner's skill tag too, is bounded by the numbers below and expires by game time.
 * Worst case of any press: at most 24 creatures looked at, at most six bullets, five chains, one needle, one field of nine pulses.
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
            case TEN -> 600; case ZETSU -> 500; case REN -> 1600; case GYO -> 400; case EN -> 1200; case KEN -> 1200;
            case KO -> 2000; case ENHANCE -> 1800; case EMIT -> 2000;
            case SHU -> 800; case IN -> 700; case RYU -> 1200; case TRANS -> 2000; case CONJ -> 2200; case MANIP -> 2200; case SPEC -> 2600; default -> 0;   // a vow costs no energy, it costs freedom
        };
    }

    public static int cooldown(int mode) {
        return switch (mode) {
            case TEN -> 200; case ZETSU -> 400; case REN -> 600; case GYO -> 300; case EN -> 400; case KEN -> 500;
            case KO -> 600; case ENHANCE -> 200; case EMIT -> 260; case VOW -> 1200;
            case SHU -> 300; case IN -> 300; case RYU -> 500; case TRANS -> 240; case CONJ -> 300; case MANIP -> 420; case SPEC -> 600; default -> 0;
        };
    }

    /** How long the window of a timed mode lasts, in ticks (0 = the mode is an instant action). 3.57.0: En is a window now (the sphere stays open for 12 s). */
    public static int duration(int mode) {
        return switch (mode) {
            case TEN -> 900; case ZETSU -> 400; case REN -> 400; case EN -> 240; case KEN -> 160; case KO -> 200; case VOW -> 600; case SHU -> 400; case IN -> 400; case RYU -> 300; default -> 0;
        };
    }

    public static boolean timed(int mode) { return duration(mode) > 0; }

    /** Windows that a second press ends early (the cooldown still starts). Ko waits for its strike and ends when it lands. */
    public static boolean cancellable(int mode) { return mode == TEN || mode == ZETSU || mode == REN || mode == EN || mode == KEN || mode == VOW || mode == SHU || mode == IN || mode == RYU; }

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
     * 3.49.0: the same with Zetsu (the body is open, a hit does 1.3) and Ryu (the aura flows to where the blow lands, a hit does 0.8 since 3.57.0, was 0.85). The clamp is the same [0.25, 1.5].
     */
    public static double takenMultiplier(boolean ten, boolean ken, boolean ko, boolean vow, boolean zetsu, boolean ryu) {
        double m = takenMultiplier(ten, ken, ko, vow);
        if (zetsu) m *= 1.3;
        if (ryu) m *= 0.8;
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

    /** Ren: Strength II and Speed II for the window (3.57.0: Speed was I). */
    public static int renStrengthAmp() { return 1; }
    public static int renSpeedAmp() { return 1; }
    /** Ken is a stance: Slowness I while it holds (3.57.0: was II, the stance reflects now instead of only hurting to move). */
    public static int kenSlowAmp() { return 0; }

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

    /** Shu: the aura on the held weapon adds this much to every blow (3.57.0: was 4). */
    public static double shuBonus() { return 6; }
    /** In: the hatsu you cannot see hit harder (3.57.0: 1.3, was 1.15). */
    public static double inMultiplier() { return 1.3; }
    public static double ryuDealtMultiplier() { return 1.25; }
    public static double transDamage() { return 9; }
    public static double transRange() { return 12; }
    public static int transMax() { return 4; }
    public static int transSlowTicks() { return 60; }
    public static double conjDamage() { return 8; }
    public static double conjRange() { return 16; }
    public static int conjRootTicks() { return 80; }
    public static double manipRange() { return 16; }
    public static double manipSearch() { return 10; }
    public static int manipTicks() { return 120; }
    public static double specRadius() { return 9; }
    public static int specMax() { return 8; }
    public static double specDamage() { return 6; }

    // ======================================================================== 3.57.0: the rework (everything below is new)
    /** Seconds-of-effect numbers of the basics. */
    public static double tenHeal() { return 1.0; }                 // health every two seconds while Ten holds
    public static int tenHealEvery() { return 40; }
    public static double zetsuHeal() { return 0.75; }              // health every second while Zetsu holds (the body rests)
    public static double zetsuAmbush() { return 1.75; }            // the blow that breaks Zetsu
    public static double renBurstRadius() { return 5.0; }
    public static double renBurstDamage() { return 4.0; }
    public static int renBurstMax() { return 12; }
    public static double renBurstKnockback() { return 1.0; }
    public static int enPulseEvery() { return 20; }                // En looks once a second while open
    public static int enGlowPulseTicks() { return 40; }
    public static int enDodgeGap() { return 120; }                 // the aura feels a blow coming once every six seconds
    public static double enDodgeMultiplier() { return 0.5; }
    public static boolean enDodgeReady(long last, long now) { return now - last >= enDodgeGap(); }
    public static double kenReflect() { return 0.3; }              // of the blow taken (before Ken cut it), thrown back at whoever struck
    public static double kenReflectMax() { return 12; }
    public static int kenReflectGap() { return 10; }
    public static double gyoMarkRange() { return 24; }
    public static int gyoMarkTicks() { return 160; }
    public static float gyoMarkMultiplier() { return 1.2f; }

    // ---- Ko stores the aura, Enhancement spends it ----
    /** Ticks of waiting it takes Ko to be full. */
    public static int koFullTicks() { return 60; }

    /** 0..1: how much aura Ko has gathered. {@code until} is the end of the window, {@code now} the game time. A closed window has gathered nothing. */
    public static double koCharge(long until, long now) {
        if (!active(until, now)) return 0.0;
        double waited = now - (until - duration(KO));
        return Math.max(0.0, Math.min(1.0, waited / koFullTicks()));
    }

    /** What Ko's follow-up strike does (raw, before the balance law): 22 when it was struck at once, 44 when it waited the full second. */
    public static double koStrike(double charge) { return 22 + 22 * clamp01(charge); }

    /** Enhancement's fist: 1x with no stored aura up to 2.4x with Ko full. */
    public static double fistMultiplier(double charge) { return 1 + 1.4 * clamp01(charge); }

    /** Share of the maximum durability each armour piece loses to the fist (never the last point: nothing breaks). */
    public static double shatterFraction(double charge, boolean vow) {
        double f = 0.08 + 0.17 * clamp01(charge);
        return Math.min(0.5, vow ? f * 2 : f);
    }

    public static double fistReach() { return 6.0; }
    public static double fistLunge() { return 0.9; }
    public static int fistMarkTicks() { return 100; }
    public static float fistMarkMultiplier() { return 1.15f; }

    // ---- Emission: the volley ----
    public static int volleyBullets(boolean vow) { return vow ? 6 : 4; }
    public static int volleyGap() { return 3; }
    public static double bulletDamage() { return 5.5; }
    public static double bulletRange() { return 24; }
    public static double bulletCone() { return 0.55; }              // cos of the half-angle a bullet will bend into
    /** Which of the foes in front the n-th bullet chooses: they are dealt out in turn so a volley spreads over a crowd. */
    public static int bulletPick(int n, int foes) { return foes <= 0 ? -1 : Math.floorMod(n, foes); }

    // ---- Transmutation: three natures, chosen by stance ----
    public static final int ELECTRIC = 0, GUM = 1, RUBBER = 2;

    /** Sneaking: gum. In the air: rubber. Otherwise: electricity. */
    public static int transProperty(boolean sneaking, boolean airborne) { return sneaking ? GUM : airborne ? RUBBER : ELECTRIC; }

    public static double elecDamage() { return 10; }
    public static double elecFalloff() { return 0.8; }
    public static double elecJumpRange() { return 6; }
    public static int elecJumps(boolean vow) { return vow ? 5 : 3; }
    public static double elecRange() { return 14; }
    public static int elecSlowTicks() { return 40; }
    public static double gumDamage() { return 4; }
    public static double gumRange() { return 9; }
    public static int gumMax(boolean vow) { return vow ? 8 : 6; }
    public static int gumRootTicks() { return 50; }
    public static double gumPull() { return 0.9; }
    public static double rubberDamage() { return 7; }
    public static double rubberRadius() { return 5.5; }
    public static int rubberMax() { return 12; }
    public static double rubberKnockback() { return 1.3; }
    public static double rubberLeap() { return 0.95; }
    public static int rubberFallTicks() { return 80; }

    /** The power sent with the surge effect: 1 + the nature (the client reads a power of 0 as 1), and for rubber the radius in tenths in the fraction. */
    public static float surgePower(int property, double radius) { return property + 1 + (float) Math.min(0.9, Math.max(0.0, radius / 10.0)); }
    public static int surgeProperty(float power) { return Math.max(0, Math.min(2, (int) power - 1)); }
    public static float surgeRadius(float power) { return (power - (int) power) * 10f; }

    // ---- Conjuration: chains ----
    public static int chains(boolean vow) { return vow ? 5 : 3; }
    public static int chainTicks() { return 100; }                  // asked of BalanceLaw.controlTicks, which caps it (3 s player, 5 s mob, 1 s boss)
    public static int chainEvery() { return 5; }                    // ticks between holds
    public static int chainHitEvery() { return 20; }
    public static double chainDamage() { return 3; }
    public static double chainBurstDamage() { return 6; }
    public static double chainLeash() { return 30; }                // the chain lets go of anything this far from its owner
    public static int chainSlots() { return 5; }

    // ---- Manipulation: the needle ----
    public static int needleTicks(boolean vow) { return vow ? 150 : 100; }   // asked of BalanceLaw.controlTicks
    public static int needleEvery() { return 10; }
    public static float needleMark(boolean vow) { return vow ? 1.35f : 1.2f; }

    // ---- Specialization: the field ----
    public static double fieldRadius(boolean vow) { return vow ? 11 : 9; }
    public static int fieldTicks(boolean vow) { return vow ? 180 : 120; }
    public static int fieldEvery() { return 20; }
    public static int fieldSteals() { return 2; }                   // buffs taken per pulse
    public static double fieldDamage() { return 3; }
    public static int fieldWeakTicks() { return 40; }
    public static int fieldPulses(boolean vow) { return fieldTicks(vow) / fieldEvery(); }

    /** How long a stolen buff lasts on its new owner (never infinite, never more than 12.5 s). */
    public static int stealTicks(int remaining) { return remaining <= 0 ? 200 : Math.min(remaining, 250); }

    /** A stolen buff keeps its strength up to Level III. */
    public static int stealAmp(int amp) { return Math.max(0, Math.min(2, amp)); }

    // ---- the aura kinds the visual layer knows (the windows that show an aura) ----
    /** The modes that show an aura, in the order of the visual layer's kinds (0 Ten, 1 Zetsu, 2 Ren, 3 Ken, 4 Ko, 5 Vow, 6 Shu, 7 Ryu). */
    public static final int[] AURA_MODES = {TEN, ZETSU, REN, KEN, KO, VOW, SHU, RYU};
    public static final int MAX_AURAS_SENT = 3;

    /** The colour (ARGB) of a Nen type, for the aura of its user. */
    public static int typeColor(int type) {
        return switch (Math.floorMod(type, 6)) {
            case T_ENHANCE -> 0xFFFFB038;      // amber
            case T_TRANS -> 0xFF7C8CFF;        // indigo
            case T_CONJ -> 0xFF48E8C0;         // teal
            case T_SPEC -> 0xFFFF6E9E;         // rose
            case T_MANIP -> 0xFFC05CFF;        // orchid
            default -> 0xFF58D8FF;             // sky (emission)
        };
    }

    /** Ko's colour is always the fist's fire; Zetsu is always grey; the vow always violet; the rest wear the user's type. */
    public static int auraColor(int kind, int type) {
        return switch (kind) { case 1 -> 0xFF8C93A8; case 4 -> 0xFFFF8A4A; case 5 -> 0xFFB070FF; default -> typeColor(type); };
    }

    private static double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }
}
