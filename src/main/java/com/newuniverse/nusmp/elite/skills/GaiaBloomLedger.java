package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.skill.rules.SkillMath;

/**
 * Why: Gaia can make the ground grow veins of ore, which would be a bottomless mine if it were free. The ledger is the brake: bloom points are EARNED by living in the domain
 * (a point a second for the owner, five for a kill) and SPENT to bloom a vein, and a daily cap limits how many veins one owner can grow per game day, whatever they have saved. It is
 * Minecraft-free (day = game time / 24000) so JUnit pins the rules, and it serialises to four numbers the skill keeps in its own tag.
 */
public final class GaiaBloomLedger {
    public static final int POINT_CAP = 600;
    public static final int VEIN_COST = 120;

    private int points;
    private int veinsToday;
    private long day;
    private int total;

    public GaiaBloomLedger(int points, int veinsToday, long day, int total) {
        this.points = SkillMath.clamp(points, 0, POINT_CAP);
        this.veinsToday = Math.max(0, veinsToday);
        this.day = day;
        this.total = Math.max(0, total);
    }

    public static GaiaBloomLedger fresh(long day) { return new GaiaBloomLedger(0, 0, day, 0); }

    public int points() { return points; }

    public int veinsToday() { return veinsToday; }

    public long day() { return day; }

    public int total() { return total; }

    /** A new game day resets the daily count (never the points). */
    public void rollover(long today) {
        if (today != day) { day = today; veinsToday = 0; }
    }

    public void earn(int gain) { points = SkillMath.clamp(points + Math.max(0, gain), 0, POINT_CAP); }

    public boolean canBloom(long today, int dailyCap) {
        rollover(today);
        return points >= VEIN_COST && veinsToday < Math.max(0, dailyCap);
    }

    /** Spends a vein's worth of points and counts the vein; false and nothing changed if it cannot. */
    public boolean bloom(long today, int dailyCap) {
        if (!canBloom(today, dailyCap)) return false;
        points -= VEIN_COST;
        veinsToday++;
        total++;
        return true;
    }

    /** Veins still allowed today. */
    public int remainingToday(long today, int dailyCap) {
        rollover(today);
        return Math.max(0, dailyCap - veinsToday);
    }

    public int[] toInts() { return new int[]{points, veinsToday, (int) (day & 0x7fffffffL), total}; }

    public static GaiaBloomLedger fromInts(int[] a, long today) {
        if (a == null || a.length < 4) return fresh(today);
        return new GaiaBloomLedger(a[0], a[1], a[2], a[3]);
    }
}
