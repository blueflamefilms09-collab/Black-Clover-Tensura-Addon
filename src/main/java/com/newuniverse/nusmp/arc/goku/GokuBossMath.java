package com.newuniverse.nusmp.arc.goku;

import java.util.Locale;

/**
 * Why: every number that scales the Goku boss, as pure arithmetic with no game type, so the tests can prove it (docs/goku_boss.md). The owner's rule: his health, damage and form pace
 * follow the Tensura Existence Points (EP) of the strongest players near him, as a smooth, capped function; with no EP data he reads the strongest Saiyan Battle Power; with neither he uses
 * a fixed value of the difficulty preset. The result is never 0 and never below the preset's own floor, so a weak party still meets a boss that is ahead of it.
 * <p>The EP curve is the balance law's own ({@code BalanceLaw.epScale}: 1 at 1,000 EP, +2.5 per decade) taken from a plain number; the EP itself is read by the game side with the repo's one
 * reader ({@code TensuraBridge.maxEp}, the one the Saiyan code uses for a victim's EP).
 */
public final class GokuBossMath {
    private GokuBossMath() {}

    /** The most players that count toward the boss's strength (the owner's 10-player cap). */
    public static final int MAX_PLAYERS = 10;
    /** Ticks between two reads of the players near him (and at spawn): one cached read per player per interval. */
    public static final int RESCAN_TICKS = 100;
    /** Battle Power to EP: the inverse of the Saiyan setting {@code epWeight} (0.05 of a victim's EP counts as its power), so 1 BP is worth 20 EP. */
    public static final double BP_TO_EP = 20.0;
    /** Hard ceilings, whatever the settings say. */
    public static final double MAX_HEALTH_FACTOR = 60.0, MAX_DAMAGE_FACTOR = 24.0, MIN_FACTOR = 0.1;
    /** The share of the EP growth that reaches his damage (his health takes all of it): damage must not outrun what the pvp cap lets through. */
    public static final double DAMAGE_EP_SHARE = 0.5;
    /** The share of the per-player bonus that reaches his damage. */
    public static final double DAMAGE_PLAYER_SHARE = 0.4;
    /** Ticks of form pace he can gain at most (the enrage time shrinks by this share at the EP cap). */
    public static final double MAX_PACE_GAIN = 0.4;

    /** The difficulty presets: base health and damage, how much of the chart's form bonuses count, and the fixed EP he is built for when nothing else is known. */
    public enum Preset {
        NORMAL(0.5, 0.5, 0.6, 5_000.0),
        OVERPOWERED(1.0, 1.0, 1.0, 30_000.0),
        ANIME(2.0, 1.6, 1.3, 300_000.0);

        private final double health, damage, formStrength, floorEp;

        Preset(double health, double damage, double formStrength, double floorEp) {
            this.health = health;
            this.damage = damage;
            this.formStrength = formStrength;
            this.floorEp = floorEp;
        }

        public double health() { return health; }
        public double damage() { return damage; }
        public double formStrength() { return formStrength; }
        /** The EP-equivalent he is built for with no player data (the last step of the fallback chain; never 0). */
        public double floorEp() { return floorEp; }
        public String id() { return name().toLowerCase(Locale.ROOT); }

        /** The preset of a setting value; anything unknown is OVERPOWERED (the default). */
        public static Preset parse(String s) {
            if (s != null) for (Preset p : values()) if (p.id().equalsIgnoreCase(s.trim())) return p;
            return OVERPOWERED;
        }
    }

    /** Where the strength reading came from. */
    public enum Source { EP, BATTLE_POWER, PRESET }

    /** The EP-equivalent he is scaled to, and where it came from. {@code ep} is always above 0. */
    public record Reading(double ep, Source source) {}

    /** Safe positive number: NaN, infinities and non-positive values read as 0. */
    static double pos(double v) { return Double.isNaN(v) || Double.isInfinite(v) || v <= 0.0 ? 0.0 : v; }

    /**
     * The fallback chain. With {@code epScaling} on and a top player's EP above 0 it is that EP; else the strongest Battle Power (times {@link #BP_TO_EP}) if above 0; else the preset's floor. The
     * answer is never below the preset's floor (he is ahead of a weak party) and never 0.
     */
    public static Reading resolve(boolean epScaling, double topEp, double topBp, Preset preset) {
        double floor = preset.floorEp();
        double ep = epScaling ? pos(topEp) : 0.0;
        if (ep > 0.0) return new Reading(Math.max(ep, floor), Source.EP);
        double bp = pos(topBp) * BP_TO_EP;
        if (bp > 0.0) return new Reading(Math.max(bp, floor), Source.BATTLE_POWER);
        return new Reading(floor, Source.PRESET);
    }

    /** The balance law's EP curve from a number: 1 at 1,000 EP and below, +2.5 per decade, at most {@code cap} (itself 1..12). */
    public static double epCurve(double ep, double cap) {
        double c = Math.max(1.0, Math.min(12.0, Double.isNaN(cap) ? 12.0 : cap));
        double e = Math.max(1.0, pos(ep));
        return Math.min(c, 1.0 + Math.max(0.0, Math.log10(e) - 3.0) * 2.5);
    }

    /** How many players count: at least 1, at most {@code limit} (itself 1..{@link #MAX_PLAYERS}). */
    public static int countedPlayers(int n, int limit) {
        return Math.max(1, Math.min(n, Math.max(1, Math.min(MAX_PLAYERS, limit))));
    }

    /** The growth of one more player: 1 + bonus per player beyond the first (bonus 0..2). */
    public static double playerFactor(int counted, double bonus) {
        return 1.0 + Math.min(2.0, pos(bonus)) * (Math.max(1, counted) - 1);
    }

    /** The inputs of the scaling, from the settings. */
    public record Params(Preset preset, double healthMultiplier, double damageMultiplier, boolean epScaling, double epWeight, double epCap, double perPlayerBonus, int playerCap) {}

    /** His health factor: preset x setting x (1 + epWeight x (curve - 1)) x players, kept between {@link #MIN_FACTOR} and {@link #MAX_HEALTH_FACTOR}. */
    public static double healthFactor(Reading r, int players, Params p) {
        double curve = epCurve(r.ep(), p.epCap());
        double f = p.preset().health() * clampPos(p.healthMultiplier(), 1.0) * (1.0 + Math.min(5.0, pos(p.epWeight())) * (curve - 1.0))
                * playerFactor(countedPlayers(players, p.playerCap()), p.perPlayerBonus());
        return Math.max(MIN_FACTOR, Math.min(MAX_HEALTH_FACTOR, f));
    }

    /** His damage factor: preset x setting x (1 + {@link #DAMAGE_EP_SHARE} x epWeight x (curve - 1)) x (1 + share of the player bonus), between {@link #MIN_FACTOR} and {@link #MAX_DAMAGE_FACTOR}. */
    public static double damageFactor(Reading r, int players, Params p) {
        double curve = epCurve(r.ep(), p.epCap());
        double pf = 1.0 + DAMAGE_PLAYER_SHARE * (playerFactor(countedPlayers(players, p.playerCap()), p.perPlayerBonus()) - 1.0);
        double f = p.preset().damage() * clampPos(p.damageMultiplier(), 1.0) * (1.0 + DAMAGE_EP_SHARE * Math.min(5.0, pos(p.epWeight())) * (curve - 1.0)) * pf;
        return Math.max(MIN_FACTOR, Math.min(MAX_DAMAGE_FACTOR, f));
    }

    /** How fast the forms come: 1 at the lowest EP curve, down to 1 - {@link #MAX_PACE_GAIN} at the cap (multiplies the enrage time). */
    public static double paceFactor(Reading r, Params p) {
        double cap = Double.isNaN(p.epCap()) ? 12.0 : Math.max(1.0, Math.min(12.0, p.epCap()));
        if (cap <= 1.0) return 1.0;
        double t = Math.max(0.0, Math.min(1.0, (epCurve(r.ep(), cap) - 1.0) / (cap - 1.0)));
        return 1.0 - MAX_PACE_GAIN * t;
    }

    private static double clampPos(double v, double dflt) { return pos(v) > 0.0 ? v : dflt; }

    /** One hit on a player may take at most this share of their maximum health (a share 0..1, the setting {@code maxHitPercent}). */
    public static float capHit(float dealt, float victimMaxHealth, double share) {
        double s = Math.max(0.01, Math.min(1.0, Double.isNaN(share) ? 0.3 : share));
        return (float) Math.min(dealt, Math.max(1.0, victimMaxHealth * s));
    }

    /** The chance of an automatic dodge: the form's own, never above the cap setting (0..0.6), never negative. */
    public static double dodgeChance(double formChance, double cap) {
        return Math.min(Math.min(0.6, pos(cap)), pos(formChance));
    }

    /** The health to keep after a change of the maximum: the same share of the bar. */
    public static float keepShare(float health, float before, float after) {
        if (!(before > 0f) || !(after > 0f)) return Math.min(health, Math.max(after, 1f));
        return Math.max(1f, Math.min(after, health * (after / before)));
    }

    /** 4.96.0: the factor on his health when every form has its own bar: {@code share} when the bars are on, else 1. */
    public static double barFactor(boolean bars, double share) { return bars ? Math.max(0.05, Math.min(1.0, share)) : 1.0; }

    /** 4.98.0: the damage of one physical blow: {@code base} times the punch multiplier (1 when punches are off or the blow is magic). */
    public static float punchDamage(float base, boolean punches, boolean magic, double mul) {
        return punches && !magic ? (float) (base * Math.max(1.0, Math.min(50.0, mul))) : base;
    }

    /** 4.98.0: the share of the victim's health one blow may take: the higher of the normal share and the punch share for a physical blow of the punch kind. */
    public static double hitShare(double normal, boolean punches, boolean magic, double punchShare) {
        return punches && !magic ? Math.max(normal, punchShare) : normal;
    }

    /** The enrage time in ticks: minutes x 1200 x the pace factor, at least 600. */
    public static long enrageTicks(double minutes, double pace) {
        return Math.max(600L, Math.round(Math.max(1.0, minutes) * 1200.0 * Math.max(0.3, Math.min(1.0, pace))));
    }

    /** Distance from a point to a segment (a beam), all in blocks. */
    public static double distToSegment(double px, double py, double pz, double ax, double ay, double az, double bx, double by, double bz) {
        double dx = bx - ax, dy = by - ay, dz = bz - az;
        double len2 = dx * dx + dy * dy + dz * dz;
        double t = len2 < 1e-9 ? 0.0 : Math.max(0.0, Math.min(1.0, ((px - ax) * dx + (py - ay) * dy + (pz - az) * dz) / len2));
        double cx = ax + dx * t - px, cy = ay + dy * t - py, cz = az + dz * t - pz;
        return Math.sqrt(cx * cx + cy * cy + cz * cz);
    }

    // ------------------------------------------------------------------ 4.103.0: true and spiritual damage
    /** How much of the split settings applies in a form: 40% in Base, rising evenly to 100% in the top form. */
    public static double splitScale(int idx, int top) {
        double t = top <= 0 ? 1.0 : Math.max(0.0, Math.min(1.0, idx / (double) top));
        return 0.4 + 0.6 * t;
    }

    /**
     * Splits one blow into {@code {normal, true, spiritual}}: the true and spiritual shares (each 0 to 0.8, together at most 0.9, bad numbers read as 0) are taken off, the rest stays a normal blow.
     * The three always add up to the blow (to float rounding), so the hit cap that came before still holds for the sum.
     */
    public static float[] split(float dealt, double trueShare, double spiritShare) {
        float d = Float.isNaN(dealt) || dealt < 0f ? 0f : dealt;
        double t = Double.isNaN(trueShare) ? 0.0 : Math.max(0.0, Math.min(0.8, trueShare));
        double s = Double.isNaN(spiritShare) ? 0.0 : Math.max(0.0, Math.min(0.8, spiritShare));
        if (t + s > 0.9) { double k = 0.9 / (t + s); t *= k; s *= k; }
        float tp = (float) (d * t), sp = (float) (d * s);
        return new float[] {d - tp - sp, tp, sp};
    }

    // ------------------------------------------------------------------ 4.107.0: true damage that always counts
    /** The most of a victim's health the guaranteed floor of the true part may be (a blow's true part never takes more than this share of max health on top of the blow). */
    public static final double MAX_TRUE_FLOOR = 0.10;

    /**
     * The true part of one blow: the share of the blow ({@code share} x {@code k}), but at least {@code floor} x {@code k} of the victim's MAXIMUM health, so a victim with a huge EP (whose normal
     * blow is cut to almost nothing) still feels the true damage. Bad numbers read as 0; the floor is held to {@link #MAX_TRUE_FLOOR}. {@code floor} 0 = the plain share (the 4.103.0 behaviour).
     */
    public static float trueAmount(float dealt, double share, double k, float victimMaxHealth, double floor) {
        float d = Float.isNaN(dealt) || dealt < 0f ? 0f : dealt;
        double sh = Double.isNaN(share) ? 0.0 : Math.max(0.0, Math.min(0.8, share));
        double kk = Double.isNaN(k) ? 0.0 : Math.max(0.0, Math.min(1.0, k));
        double fl = Double.isNaN(floor) ? 0.0 : Math.max(0.0, Math.min(MAX_TRUE_FLOOR, floor));
        float mh = Float.isNaN(victimMaxHealth) || victimMaxHealth < 0f ? 0f : victimMaxHealth;
        return (float) Math.max(d * sh * kk, mh * fl * kk);
    }

    /** How much of an intended true hit another mod (or a rule) ate: intended minus the health really lost, never negative; losses within 5% count as full. */
    public static float missing(float intended, float lost) {
        if (Float.isNaN(intended) || intended <= 0f) return 0f;
        float l = Float.isNaN(lost) || lost < 0f ? 0f : lost;
        float m = intended - l;
        return m <= intended * 0.05f ? 0f : m;
    }

    /** 4.107.0: his punch gap with the power-up: 20% shorter, never below 6 ticks. */
    public static int quickGap(int gap, boolean powerUp) {
        return powerUp ? Math.max(6, (int) Math.round(gap * 0.8)) : gap;
    }

    /** 4.107.0: a factor with the power-up: x{@code mul} when on (bad mul reads as 1), then held to {@code max}. */
    public static double powered(double factor, double mul, boolean on, double max) {
        double m = Double.isNaN(mul) || mul <= 0.0 ? 1.0 : mul;
        return Math.min(max, on ? factor * m : factor);
    }
}
