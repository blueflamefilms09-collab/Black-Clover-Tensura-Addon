package com.newuniverse.nusmp.skill.rules;

/**
 * Why: Firebolt was folded into Argonaut (docs/argonaut_rework.md). Argonaut (and Hero's Argonaut) gain a Firebolt mode next to the charged
 * strike, and a charged release carries fire. Which mode index means what, how many modes there are, how long a charged strike burns and when a
 * saved Firebolt holder is migrated are pure functions, so they are unit-tested instead of discovered by playing. The old mode numbers never move:
 * STRIKE 0 and (Hero) BEAM 1 stay, FIREBOLT is appended (Argonaut: index 1, Hero's Argonaut: index 2).
 */
public final class ArgonautMergeRules {
    private ArgonautMergeRules() {}

    /** Charges needed before a release also sets the struck creatures alight (a single bell is still a plain strike). */
    public static final int FIRE_MIN_CHARGE = 2;
    /** The most seconds a charged strike can burn a target. */
    public static final int MAX_IGNITE_SECONDS = 12;
    /** Mastery a Firebolt that hit teaches Argonaut: half of what the standalone Firebolt taught, so rapid casting cannot outrun the charged strike. */
    public static final double FIREBOLT_MASTERY_PER_HIT = 0.5;

    /** Mastery (0..1) from which a Unique may charge while walking (wiki: Bell first could not, later could). The Ultimate always can. */
    public static final double MOVING_CHARGE_MASTERY = 0.25;
    /** Mastery from which a Unique has Dual Charge (two charges at once = Argo Vesta) and a 4-bell cap (3 : 4 : 5). The Ultimate always has it. */
    public static final double DUAL_CHARGE_MASTERY = 0.5;
    /** Blocks a charging caster may drift from where the charge began before it breaks, while "charge while moving" is not learned. */
    public static final double STILL_RADIUS = 1.0;
    /** Firebolt's weak long range (wiki): full damage out to this many blocks, then it falls to {@link #FALLOFF_FLOOR} at the end of its range. */
    public static final double SHORT_RANGE = 16.0;
    public static final double FALLOFF_FLOOR = 0.5;

    /** How many modes: the Unique has Strike (+ Firebolt); the Ultimate has Strike, Beam (+ Firebolt). {@code merged} is the server setting. */
    public static int modes(boolean ultimate, boolean merged) { return modes(ultimate, merged, false); }

    /** As above; {@code canon} (setting argonautWikiCanon, only with the merge) appends Charged Firebolt and Argo Vesta after Firebolt. */
    public static int modes(boolean ultimate, boolean merged, boolean canon) { return (ultimate ? 2 : 1) + (merged ? 1 : 0) + (merged && canon ? 2 : 0); }

    /** What a mode index does. An index outside the live range (a saved mode after the setting went off) is the plain Strike. */
    public static ChargeRules.Strike kind(int mode, boolean ultimate, boolean merged) { return kind(mode, ultimate, merged, false); }

    /**
     * Unique: Strike 0, Firebolt 1, (canon) Charged Firebolt 2, Argo Vesta 3. Ultimate: Strike 0, Beam 1, Firebolt 2, (canon) Charged Firebolt 3, Argo Vesta 4.
     * Every older index keeps its meaning; the new ones are appended.
     */
    public static ChargeRules.Strike kind(int mode, boolean ultimate, boolean merged, boolean canon) {
        if (mode < 0 || mode >= modes(ultimate, merged, canon)) return ChargeRules.Strike.STRIKE;
        int i = ultimate ? mode : (mode == 0 ? 0 : mode + 1);               // the Unique has no Beam: shift past index 1
        return switch (i) {
            case 0 -> ChargeRules.Strike.STRIKE;
            case 1 -> ChargeRules.Strike.BEAM;
            case 2 -> ChargeRules.Strike.FIREBOLT;
            case 3 -> ChargeRules.Strike.CHARGED_FIREBOLT;
            default -> ChargeRules.Strike.ARGO_VESTA;
        };
    }

    /** The mode after {@code mode} (or before, reversed), wrapping around the live modes. */
    public static int nextMode(int mode, boolean reverse, boolean ultimate, boolean merged) { return nextMode(mode, reverse, ultimate, merged, false); }

    public static int nextMode(int mode, boolean reverse, boolean ultimate, boolean merged, boolean canon) {
        int n = modes(ultimate, merged, canon);
        int cur = Math.max(0, Math.min(n - 1, mode));
        return reverse ? (cur - 1 + n) % n : (cur + 1) % n;
    }

    /** Seconds a charged strike burns what it hits: 0 below {@link #FIRE_MIN_CHARGE} charges, then 2 + the charge, capped. */
    public static int chargedIgniteSeconds(int charge) {
        return charge < FIRE_MIN_CHARGE ? 0 : Math.min(MAX_IGNITE_SECONDS, 2 + charge);
    }

    /** Which releases set what they hit alight. Canon (wiki): only the Firebolt-infused ones, Argo Vesta and the Afterglow beam. Not canon (4.4.0): the plain strike and the beam. */
    public static boolean burns(ChargeRules.Strike kind, boolean canon) {
        return kind == ChargeRules.Strike.BEAM || kind == ChargeRules.Strike.ARGO_VESTA || (!canon && kind == ChargeRules.Strike.STRIKE);
    }

    /** Argo Vesta's damage over the plain strike of the same charge (the Firebolt infused into the blade). */
    public static double vestaFactor() { return 1.25; }

    /** Argo Vesta's shockwave: the knockback over the plain strike's. */
    public static double vestaKnockback() { return 1.5; }

    /** Stamina (vanilla exhaustion) a bell costs in canon: the wiki says a charge spends stamina and mind. A full Unique charge is 4 exhaustion = one food point. */
    public static float staminaPerBell() { return 0.8f; }

    /** A charge breaks when the caster loses health while holding it (wiki: the charge dissipates when Bell is attacked). */
    public static boolean brokenByHit(float healthBefore, float healthNow) { return healthNow < healthBefore - 0.001f; }

    /** Whether a Unique at this mastery may charge while moving; {@code canon} off = always (the 4.4.0 rule). */
    public static boolean canChargeMoving(boolean canon, boolean ultimate, double masteryFrac) { return !canon || ultimate || masteryFrac >= MOVING_CHARGE_MASTERY; }

    /** Whether a mode kind is learned yet: only Argo Vesta (Dual Charge) waits for mastery, and only in canon. */
    public static boolean unlocked(ChargeRules.Strike kind, boolean canon, boolean ultimate, double masteryFrac) {
        return kind != ChargeRules.Strike.ARGO_VESTA || !canon || ultimate || masteryFrac >= DUAL_CHARGE_MASTERY;
    }

    /** True when the caster has drifted farther than {@link #STILL_RADIUS} from the spot the charge began (squared distances in blocks). */
    public static boolean movedTooFar(double dx, double dz) { return dx * dx + dz * dz > STILL_RADIUS * STILL_RADIUS; }

    /** Wear on the charged weapon (wiki: recoil damages whatever is charged, worse the longer): 0 for a single bell, charge - 1 after, halved for the Ultimate. */
    public static int recoilWear(int charge, boolean ultimate) {
        int w = Math.max(0, charge - 1);
        return ultimate ? w / 2 : w;
    }

    /** Damage share of an uncharged Firebolt that reaches {@code distance} blocks of a bolt whose range is {@code range}: 1 up to {@link #SHORT_RANGE}, falling linearly to {@link #FALLOFF_FLOOR}. */
    public static double rangeFalloff(double distance, double range) {
        if (distance <= SHORT_RANGE || range <= SHORT_RANGE) return 1.0;
        double t = Math.min(1.0, (distance - SHORT_RANGE) / (range - SHORT_RANGE));
        return 1.0 - (1.0 - FALLOFF_FLOOR) * t;
    }

    /** Mastery a Firebolt cast teaches Argonaut: only a shot that hit something. */
    public static double fireboltMastery(boolean hit) { return hit ? FIREBOLT_MASTERY_PER_HIT : 0.0; }

    /** A saved Firebolt holder is migrated (Firebolt forgotten, Argonaut kept or granted) only while the merge is on. */
    public static boolean migrates(boolean merged, boolean holdsFirebolt) { return merged && holdsFirebolt; }

    /** Argonaut is granted only to a holder who has neither Argonaut nor its Ultimate (the Ultimate already carries the Firebolt mode). */
    public static boolean grantsArgonaut(boolean holdsArgonaut, boolean holdsHeros) { return !holdsArgonaut && !holdsHeros; }

    /** With the merge turned off again, a holder whose Firebolt was migrated gets the old standalone Firebolt back. */
    public static boolean restores(boolean merged, boolean wasMigrated, boolean holdsFirebolt) { return !merged && wasMigrated && !holdsFirebolt; }
}
