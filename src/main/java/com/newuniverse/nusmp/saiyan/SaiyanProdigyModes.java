package com.newuniverse.nusmp.saiyan;

import com.newuniverse.nusmp.skill.modes.ModeBundle;
import com.newuniverse.nusmp.skill.modes.ModeSpec;
import com.newuniverse.nusmp.skill.modes.ModeSpec.CostClass;

import java.util.List;

/**
 * The ONE data table of the Saiyan Prodigy skill (docs/saiyan_prodigy.md): every Saiyan ability is a mode of one intrinsic skill in one slot, so a Saiyan needs one key (plus the mode key) and not one key per
 * ability. Pure: no game type, no state, unit-tested ({@code SaiyanProdigyModesTest}). The mode numbers are saved by Tensura, so the order is permanent: new modes are only appended.
 * <p>
 * An {@code action} is a Saiyan op ({@link SaiyanNet} 1 to 21, {@link SaiyanNet2} 22 to 35) sent through the same server entry the old keys use ({@code SaiyanProdigyHub#act}), so every old rule (awakening, the Goku lock,
 * Ki costs, cooldowns, the 4-tick rate limit) stays exactly as it was; or one of the three prodigy actions below, which the skill itself answers.
 */
public final class SaiyanProdigyModes {
    private SaiyanProdigyModes() {}

    /** Prodigy's own actions (above every Saiyan op): perform the selected art, select the next art, switch the Dragon Ball fighting style. */
    public static final int ACT_ART = 100, ACT_ART_NEXT = 101, ACT_STYLE = 102, ACT_STYLE_NEXT = 103;

    /** Unlock codes of {@link ModeSpec#need()}: open to everyone, open once the Ki is awakened, open for the awakened Goku line (the old Goku lock of Kamehameha, Instant Transmission ...). */
    public static final int NEED_NONE = 0, NEED_AWAKENED = 1, NEED_GOKU = 2;

    /** The mode numbers (append only). */
    public static final int KI_BLAST = 0, KI_BARRAGE = 1, KI_FLIGHT = 2, SENSES = 3, KI_BARRIER = 4, DEFLECTION = 5, ENERGY_TRANSFER = 6, KI_WAVE = 7, FORM = 8, ART = 9, DRAGON_FIST = 10,
            KAMEHAMEHA = 11, RSP = 12, TRANSMISSION = 13, SPIRIT_BOMB = 14, METEOR_COMBO = 15, GOD_BIND = 16, STYLE = 17;

    public static final ModeBundle BUNDLE = new ModeBundle("saiyan_prodigy", List.of(
            ModeSpec.of("ki_blast", "Ki Blast", CostClass.LIGHT, SaiyanNet.OP_BLAST).needs(NEED_AWAKENED),
            ModeSpec.of("ki_barrage", "Ki Barrage", CostClass.MEDIUM, SaiyanNet.OP_BARRAGE).needs(NEED_AWAKENED),
            ModeSpec.of("ki_flight", "Ki Flight", CostClass.LIGHT, SaiyanNet.OP_FLIGHT).sneak(SaiyanNet.OP_BURST).needs(NEED_AWAKENED),
            ModeSpec.of("senses", "Ki Senses", CostClass.FREE, SaiyanNet.OP_SENSE).sneak(SaiyanNet.OP_SUPPRESS),
            ModeSpec.of("ki_barrier", "Ki Barrier", CostClass.MEDIUM, SaiyanNet.OP_BARRIER).needs(NEED_AWAKENED),
            ModeSpec.of("deflection", "Ki Deflection", CostClass.MEDIUM, SaiyanNet.OP_DEFLECT).needs(NEED_AWAKENED),
            ModeSpec.of("energy_transfer", "Energy Transfer", CostClass.MEDIUM, SaiyanNet.OP_ENERGY).needs(NEED_AWAKENED),
            ModeSpec.of("ki_wave", "Ki Wave", CostClass.HEAVY, SaiyanNet2.OP_WAVE_START).hold(SaiyanNet2.OP_WAVE_RELEASE).needs(NEED_AWAKENED),
            ModeSpec.of("form", "Form", CostClass.MEDIUM, SaiyanNet.OP_FORM_TOGGLE).sneak(SaiyanNet.OP_FORM_NEXT).needs(NEED_AWAKENED),
            ModeSpec.of("art", "Art", CostClass.MEDIUM, ACT_ART).sneak(ACT_ART_NEXT),
            ModeSpec.of("dragon_fist", "Dragon Fist", CostClass.HEAVY, SaiyanNet.OP_DRAGON).needs(NEED_AWAKENED),
            ModeSpec.of("kamehameha", "Kamehameha", CostClass.HEAVY, SaiyanNet.OP_KAME_START).hold(SaiyanNet.OP_KAME_RELEASE).needs(NEED_GOKU),
            ModeSpec.of("rsp", "Rock-Scissors-Paper", CostClass.MEDIUM, SaiyanNet.OP_COMBO).needs(NEED_GOKU),
            ModeSpec.of("instant_transmission", "Instant Transmission", CostClass.MEDIUM, SaiyanNet.OP_TRANSMIT).needs(NEED_GOKU),
            ModeSpec.of("spirit_bomb", "Spirit Bomb", CostClass.ULTIMATE, SaiyanNet.OP_SPIRIT_START).hold(SaiyanNet.OP_SPIRIT_RELEASE).needs(NEED_GOKU),
            ModeSpec.of("meteor_combination", "Meteor Combination", CostClass.HEAVY, SaiyanNet.OP_METEOR).needs(NEED_GOKU),
            ModeSpec.of("god_bind", "God Bind", CostClass.HEAVY, SaiyanNet.OP_BIND).needs(NEED_GOKU),
            ModeSpec.of("style", "Fighting Style", CostClass.FREE, ACT_STYLE).sneak(ACT_STYLE_NEXT)));

    /** One art of the Art mode: a permanent id (saved in the skill tag), its English name and the Saiyan op it performs. */
    public record Art(String id, String name, int op) {}

    /** The Dragon Ball rework techniques ({@link SaiyanNet2#CYCLE}) as arts, so the wheel stays short: one Art mode selects and performs them. Order is permanent (append only). */
    public static final List<Art> SAIYAN_ARTS = List.of(
            new Art("meteor_strike", "Meteor Strike", SaiyanNet2.OP_STRIKE), new Art("vanishing_step", "Vanishing Step", SaiyanNet2.OP_VANISH), new Art("aerial_rush", "Aerial Rush", SaiyanNet2.OP_RUSH),
            new Art("warriors_counter", "Warrior's Counter", SaiyanNet2.OP_COUNTER), new Art("ground_breaker", "Ground Breaker", SaiyanNet2.OP_BREAKER), new Art("combat_instinct", "Combat Instinct", SaiyanNet2.OP_INSTINCT),
            new Art("ki_sphere", "Charged Ki Sphere", SaiyanNet2.OP_SPHERE), new Art("ki_reinforcement", "Ki Reinforcement", SaiyanNet2.OP_REINFORCE), new Art("giant_ki_avatar", "Giant Ki Avatar", SaiyanNet2.OP_AVATAR),
            new Art("afterimage", "Afterimage Technique", SaiyanNet2.OP_AFTERIMAGE), new Art("gravity_training", "Gravity Training", SaiyanNet2.OP_GRAVITY));

    /** The Saiyan art with this id, or null. */
    public static Art saiyanArt(String id) {
        for (Art a : SAIYAN_ARTS) if (a.id().equals(id)) return a;
        return null;
    }

    /**
     * Every Saiyan op 1 to 35 is reachable from the skill (as a mode action, a sneak action, a release action or an art) EXCEPT {@link #UNREACHED}: the previous form (the form list wraps, so "next" reaches every
     * form) and the branch key (an owner decision of the Dragon Ball rework, off by default). Pure check used by the tests and the self-test.
     */
    public static boolean reaches(int op) {
        for (ModeSpec m : BUNDLE.modes()) if (m.action() == op || m.sneakAction() == op || m.releaseAction() == op) return true;
        for (Art a : SAIYAN_ARTS) if (a.op() == op) return true;
        return false;
    }

    public static final int[] UNREACHED = {SaiyanNet.OP_FORM_PREV, SaiyanNet2.OP_BRANCH};

    /** True when a mode is open for this Saiyan. {@code awakened}: the Ki awakening is done; {@code goku}: the Goku line (the Ultimate of the old lock) is held. */
    public static boolean open(ModeSpec m, boolean awakened, boolean goku) {
        return switch (m.need()) {
            case NEED_NONE -> true;
            case NEED_AWAKENED -> awakened;
            case NEED_GOKU -> awakened && goku;
            default -> false;
        };
    }

    // ---------------------------------------------------------------------------------------------------- the Dragon Ball arts
    /**
     * The Battle Power at which a Dragon Ball character's arts open for a Saiyan: 1000 for the weakest (scale 0.30, Bulma) rising by a factor of about 1900 to the strongest (scale 1.0). Goku (0.97) is about
     * 1.44 million, Gohan (1.0) about 2 million. {@code bpScale} is the server's multiplier (setting {@code dbArtsBpScale}).
     */
    public static double dbUnlockBp(double scale, double bpScale) {
        double s = Math.max(0.30, Math.min(1.0, scale));
        return 1000.0 * Math.pow(10.0, 3.3 * (s - 0.30) / 0.70) * Math.max(0.0, bpScale);
    }

    /** The strength of a Dragon Ball art: Ultimate strength (x1) from 4 times the unlock, the Unique's half below it. */
    public static boolean dbUltimateStrength(double bp, double unlockBp, boolean mastered, boolean ownsUltimate) { return mastered || ownsUltimate || bp >= 4.0 * unlockBp; }

    /** The mastery finisher of a character opens from 16 times its unlock (or a mastered Prodigy, or an owned Ultimate). */
    public static boolean dbFinisher(double bp, double unlockBp, boolean mastered, boolean ownsUltimate) { return mastered || ownsUltimate || bp >= 16.0 * unlockBp; }

    /** A character's arts are open: Battle Power at the unlock, or the old skill of that character is owned (kept, never taken). */
    public static boolean dbOpen(double bp, double unlockBp, boolean ownsOld) { return ownsOld || bp >= unlockBp; }

    /** The most arts the wheel ever holds (the 11 Saiyan arts and the open Dragon Ball arts); bounds the size of the list built on a press. */
    public static final int MAX_ARTS = 128;
}
