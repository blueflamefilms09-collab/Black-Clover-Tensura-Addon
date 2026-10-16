package com.newuniverse.nusmp.soul;

/**
 * Why: whether a mob may be purified, and with what chance, is the part of Paladin Conversion that has to be fair and must never
 * depend on Minecraft types or on dice a test cannot fix. The owner's rules: never a player, never a boss (of this mod or any), never
 * a creature on the {@code nusmp:paladin_immune} tag, never a named Tensura boss-like, never another caster's Paladin, never the
 * Soul Paladins of Lucius's fight, never someone's tamed pet. The chance comes from the target's health fraction, the gap between
 * its energy and the caster's, its magic resistance and the caster's mastery; a hostile mob below half health converts reliably.
 * The glue reads the facts off the entity into a {@link Subject} and rolls once; this class decides.
 */
public final class PaladinRules {
    private PaladinRules() {}

    /** Why a target is refused (OK = it may be purified). */
    public enum Verdict { OK, PLAYER, BOSS, DENIED, NAMED, ALREADY, OTHER_PALADIN, BOSS_PALADIN, PET, DEAD, TOO_STRONG, FAMILY }

    /** Everything the rules look at, read off the target by the glue. */
    public record Subject(boolean player, boolean boss, boolean denyTag, boolean namedBoss, boolean family, boolean bossPaladin,
                          boolean paladinOfCaster, boolean paladinOfOther, boolean pet, boolean alive,
                          double hpFraction, double targetEp, double casterEp, double resistance, boolean hostile) {}

    /** A target with more than this many times the caster's energy cannot be purified at all. */
    public static final double MAX_EP_RATIO = 10.0;
    /** Ticks of the channel: three seconds. */
    public static final int CHANNEL_TICKS = 60;
    /** The most the caster may stand from the target while channelling (blocks), before the channel breaks. */
    public static final double LEASH = 20.0;
    /** A hostile mob at or below this health fraction converts reliably. */
    public static final double RELIABLE_HP = 0.5;
    public static final double RELIABLE_CHANCE = 0.9, MIN_CHANCE = 0.05, MAX_CHANCE = 0.97;

    public static Verdict verdict(Subject s) {
        if (!s.alive()) return Verdict.DEAD;
        if (s.player()) return Verdict.PLAYER;
        if (s.family()) return Verdict.FAMILY;
        if (s.bossPaladin()) return Verdict.BOSS_PALADIN;
        if (s.paladinOfCaster()) return Verdict.ALREADY;
        if (s.paladinOfOther()) return Verdict.OTHER_PALADIN;
        if (s.boss()) return Verdict.BOSS;
        if (s.denyTag()) return Verdict.DENIED;
        if (s.namedBoss()) return Verdict.NAMED;
        if (s.pet()) return Verdict.PET;
        if (s.casterEp() > 0 && s.targetEp() > s.casterEp() * MAX_EP_RATIO) return Verdict.TOO_STRONG;
        return Verdict.OK;
    }

    public static boolean allowed(Subject s) { return verdict(s) == Verdict.OK; }

    /** The message for a refusal, for the caster's action bar. */
    public static String refusal(Verdict v) {
        return switch (v) {
            case PLAYER -> "A living soul of a player cannot be purified.";
            case BOSS, NAMED, FAMILY -> "This soul is too great and too fixed to be turned.";
            case DENIED -> "Something shields this soul from purification.";
            case ALREADY -> "That creature already follows you.";
            case OTHER_PALADIN, BOSS_PALADIN -> "That soul already kneels to another.";
            case PET -> "That creature belongs to someone.";
            case DEAD -> "There is no soul left to purify.";
            case TOO_STRONG -> "Its soul is far too strong for you to turn.";
            case OK -> "";
        };
    }

    /**
     * The chance that the purification takes, 0..1. Base 35% to 75% by mastery; up to +25% for a weakened target; plus 10% for a
     * hostile creature; +15% at most (-35% at least) from the energy gap (a tenth of the caster's energy to ten times it, in log steps);
     * minus up to 50% for magic resistance. A hostile mob at or below half health never goes under {@link #RELIABLE_CHANCE} unless it
     * resists almost completely. Always inside {@link #MIN_CHANCE} .. {@link #MAX_CHANCE}.
     *
     * @param mastery 0..1 mastery of the Soul grimoire
     * @param resistance 0..1 magic resistance of the target (already normalised by the glue)
     */
    public static double chance(double hpFraction, double targetEp, double casterEp, double resistance, double mastery, boolean hostile) {
        double m = clamp01(mastery);
        double hp = clamp01(hpFraction);
        double c = 0.35 + 0.40 * m;
        c += (1.0 - hp) * 0.5;
        if (hostile) c += 0.10;
        c += epTerm(targetEp, casterEp);
        c -= 0.5 * clamp01(resistance);
        if (hostile && hp <= RELIABLE_HP && clamp01(resistance) < 0.8) c = Math.max(c, RELIABLE_CHANCE);
        return Math.max(MIN_CHANCE, Math.min(MAX_CHANCE, c));
    }

    /** +0.15 when the target has a tenth of the caster's energy, 0 at parity, -0.35 at ten times (0.15 per decade down, 0.35 up, log scale). */
    static double epTerm(double targetEp, double casterEp) {
        if (targetEp <= 0 || casterEp <= 0) return 0.0;
        double decades = Math.log10(casterEp / targetEp);          // positive: the caster is stronger
        double t = decades >= 0 ? decades * 0.15 : decades * 0.35;
        return Math.max(-0.35, Math.min(0.15, t));
    }

    /** True if the channel must break: the caster went too far, took damage, died, or the target is gone. */
    public static boolean interrupted(double distance, boolean casterHurt, boolean casterAlive, boolean targetAlive, boolean sameLevel) {
        return !casterAlive || !targetAlive || !sameLevel || casterHurt || distance > LEASH;
    }

    private static double clamp01(double v) { return Double.isNaN(v) ? 0.0 : Math.max(0.0, Math.min(1.0, v)); }

    /** Weakness (ticks) a failed purification leaves on the target: 4 seconds, 6 with high mastery. */
    public static int failWeaknessTicks(double mastery) { return clamp01(mastery) >= 0.5 ? 120 : 80; }
}
