package com.newuniverse.nusmp.avatar.ai;

/**
 * Why: "the Avatar dynamically switches attack goals based on what it is holding (Bow -> Ranged, Sword -> Melee, Grimoire -> Magic)" is a decision table,
 * and a decision table is worth testing on its own: which style a pair of held items gives, how far a shooter or a mage likes to stand from its target
 * (a Cowardly Avatar farther, an Aggressive one closer), when a shield goes up, how hard a bow is drawn and how steady the aim is. No game types are
 * used, so the goals only read the world and call these functions; the numbers are exercised by {@code CombatPlannerTest}.
 */
public final class CombatPlanner {
    private CombatPlanner() {}

    /** What the planner needs to know: the kinds in both hands, whether a bow or crossbow has something to shoot, whether the Avatar can cast anything at all. */
    public record Facts(WeaponKind main, WeaponKind off, boolean hasAmmo, boolean canCast) {
        public Facts {
            main = main == null ? WeaponKind.NONE : main;
            off = off == null ? WeaponKind.NONE : off;
        }
    }

    /**
     * The way to fight. A bow or crossbow in the main hand wins when there is ammunition; otherwise a focus (grimoire, charter grimoire, staff) in either hand
     * makes a mage of an Avatar that can cast; everything else is melee (an empty bow is a club, a mage with nothing to cast draws a sword).
     */
    public static CombatStyle choose(Facts f) {
        if (f.main() == WeaponKind.BOW && f.hasAmmo()) return CombatStyle.BOW;
        if (f.main() == WeaponKind.CROSSBOW && f.hasAmmo()) return CombatStyle.CROSSBOW;
        if (f.canCast() && (f.main() == WeaponKind.FOCUS || f.off() == WeaponKind.FOCUS)) return CombatStyle.MAGE;
        return CombatStyle.MELEE;
    }

    // ---------------------------------------------------------------- distances
    /** The distance band a ranged fighter tries to keep from its target, in blocks. */
    public record Range(double min, double max) {
        public Range {
            if (min < 0) min = 0;
            if (max < min) max = min;
        }
    }

    /**
     * The band for a style. {@code cfgMin}/{@code cfgMax} are the configured bow band (6 to 18); a crossbow stands a little closer, a mage in the middle.
     * Cowardly stands farther, Aggressive closer. Melee has no band (0, 0).
     */
    public static Range range(CombatStyle style, boolean cowardly, boolean aggressive, double cfgMin, double cfgMax) {
        if (style == null || style == CombatStyle.MELEE) return new Range(0, 0);
        double min = cfgMin, max = cfgMax;
        switch (style) {
            case CROSSBOW -> { min -= 1; max -= 2; }
            case MAGE -> { min += 1; max -= 3; }
            default -> { }
        }
        if (cowardly) { min += 3; max += 2; }
        if (aggressive) { min = Math.max(2, min - 3); max = Math.max(min + 4, max - 4); }
        min = Math.max(2, Math.min(25, min));
        max = Math.min(28, Math.max(min + 3, max));
        return new Range(min, max);
    }

    /** Is the target closer than the band allows (the shooter should back off)? */
    public static boolean tooClose(double distance, Range r) { return r.max() > 0 && distance < r.min(); }

    /** Is the target inside the band's far edge (the shooter may stand and shoot)? */
    public static boolean inRange(double distance, Range r) { return distance <= r.max(); }

    /**
     * The strafing hysteresis of vanilla's skeleton: back away once the target is inside a quarter of the squared reach, stop backing away outside three
     * quarters, otherwise keep doing what it did.
     */
    public static boolean strafeBackwards(double distanceSq, double reach, boolean current) {
        double r2 = reach * reach;
        if (distanceSq > r2 * 0.75) return false;
        if (distanceSq < r2 * 0.25) return true;
        return current;
    }

    // ---------------------------------------------------------------- the shield
    /** Length of one shield cycle in ticks, and how many of them the shield is held up (the rest of the cycle the Avatar can strike). */
    public static final int SHIELD_CYCLE = 50, SHIELD_UP = 30;

    /** Is the shield up at this moment? Only with a shield in hand and a melee enemy near; {@code offset} (the Avatar's id) keeps a group from blocking in step. */
    public static boolean shieldUp(boolean hasShield, boolean meleeEnemyNear, long tick, int offset) {
        if (!hasShield || !meleeEnemyNear) return false;
        return Math.floorMod(tick + offset, (long) SHIELD_CYCLE) < SHIELD_UP;
    }

    // ---------------------------------------------------------------- the bow
    /** Ticks a bow must be drawn before a shot may be loosed, and the least ticks between two shots. */
    public static final int BOW_DRAW_TICKS = 20;

    /** Vanilla's draw curve: (f^2 + 2f) / 3 with f = ticks / 20, at most 1. */
    public static float bowPower(int drawnTicks) {
        float f = Math.max(0, drawnTicks) / 20.0f;
        f = (f * f + f * 2.0f) / 3.0f;
        return Math.min(1.0f, f);
    }

    /** Ticks a shooter waits after a shot before it starts the next one. */
    public static int shotInterval(CombatStyle style, boolean aggressive) {
        int base = style == CombatStyle.CROSSBOW ? 14 : style == CombatStyle.BOW ? 22 : 40;
        return aggressive ? Math.max(8, base - 4) : base;
    }

    /** Arrows hit as hard as the shooter's body: a plain 4-point attacker shoots vanilla arrows, a stronger one up to four times as hard. */
    public static double arrowDamageScale(double attackDamage) { return Math.max(1.0, Math.min(4.0, attackDamage / 4.0)); }

    /** Arrow spread in degrees: a high DEX modifier makes a steadier hand, never below 1.5 degrees, never above 14 (vanilla's easy skeleton). */
    public static float inaccuracy(int dexterityScore) {
        int modifier = Math.floorDiv(dexterityScore - 10, 2);
        return (float) Math.max(1.5, Math.min(14.0, 10.0 - 1.5 * modifier));
    }
}
