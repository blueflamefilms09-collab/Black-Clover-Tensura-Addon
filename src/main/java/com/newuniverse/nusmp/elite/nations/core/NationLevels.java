package com.newuniverse.nusmp.elite.nations.core;

/**
 * Why: the level maths of a nation ("leveling, metrics"), pure. A nation's SCORE is a weighted sum of its metrics (members, treasury, completed contracts, held area score, war wins) plus the nation XP
 * handed out by contracts and other systems; its LEVEL is how many thresholds the score has reached. The thresholds follow {@code base * (n-1)^growth} for level n (n >= 2), so levels get further apart.
 * The treasury part of the score is capped (a rich nation does not level on money alone). The level is derived, never trusted from a client; it may fall if the metrics fall (members leave), and
 * {@code NationLevelUpEvent} is only fired when a nation passes the highest level it ever announced.
 */
public final class NationLevels {
    private NationLevels() {}

    /** The weights of the metrics. {@code coinsPerPoint} is how many copper coins make one treasury point, {@code treasuryPointCap} the most points the treasury can give. */
    public record Weights(double perMember, long coinsPerPoint, double treasuryPointCap, double perContract, double perArea, double perWarWin) {
        public static Weights defaults() { return new Weights(20.0, 1000L, 300.0, 10.0, 1.0, 50.0); }
    }

    /** The raw metrics of a nation. */
    public record Metrics(int members, long treasury, int completedContracts, int areaScore, int warWins, long xp) {}

    public static double score(Weights w, Metrics m) {
        double treasuryPoints = w.coinsPerPoint() <= 0 ? 0.0 : Math.min(w.treasuryPointCap(), Math.max(0L, m.treasury()) / (double) w.coinsPerPoint());
        double s = m.members() * w.perMember() + treasuryPoints + m.completedContracts() * w.perContract() + Math.max(0, m.areaScore()) * w.perArea() + m.warWins() * w.perWarWin() + Math.max(0L, m.xp());
        return Math.max(0.0, s);
    }

    /** thresholds[i] is the score needed for level i + 2 (so the array has maxLevel - 1 entries); level 1 needs nothing. */
    public static long[] thresholds(int maxLevel, double base, double growth) {
        int n = Math.max(0, maxLevel - 1);
        long[] t = new long[n];
        long prev = 0;
        for (int i = 0; i < n; i++) {
            long v = Math.round(Math.max(1.0, base) * Math.pow(i + 1, Math.max(0.1, growth)));
            if (v <= prev) v = prev + 1;
            t[i] = v;
            prev = v;
        }
        return t;
    }

    /** The level for a score: 1 plus the number of thresholds reached. */
    public static int levelOf(double score, long[] thresholds) {
        int level = 1;
        for (long t : thresholds) {
            if (score >= t) level++;
            else break;
        }
        return level;
    }

    /** Score still missing for the next level, or -1 at the top. */
    public static long missingForNext(double score, long[] thresholds) {
        int level = levelOf(score, thresholds);
        if (level - 1 >= thresholds.length) return -1L;
        return (long) Math.ceil(thresholds[level - 1] - score);
    }

    /** Progress inside the current level, 0..1 (1 at the top level). */
    public static double progress(double score, long[] thresholds) {
        int level = levelOf(score, thresholds);
        if (level - 1 >= thresholds.length) return 1.0;
        long lo = level == 1 ? 0 : thresholds[level - 2], hi = thresholds[level - 1];
        return Math.max(0.0, Math.min(1.0, (score - lo) / Math.max(1.0, hi - lo)));
    }
}
