package com.newuniverse.nusmp.multiverse;

/**
 * Why: the numbers of the Magic Knight Mission Board (3.68.0), with no Minecraft type, so they are unit tested. Every in-game day (20 real minutes) a player is offered three missions out of four kinds, scaled to
 * their rank; one is taken at a time, kills count by themselves, and it pays gold stars (the anime gives stars by the difficulty of the task). Offers come from the player's id and the day, so they need no storage.
 * Cost: a few integer operations per offer; nothing per tick.
 */
public final class MissionBoardRules {
    private MissionBoardRules() {}

    public enum Kind {
        HOSTILE("Slay hostile creatures", 12, 6), UNDEAD("Put down the undead", 10, 5), ARTHROPOD("Clear out spiders and other crawlers", 8, 4), RAIDER("Drive off raiders", 6, 3);
        public final String title;
        public final int base, killsPerStar;
        Kind(String title, int base, int killsPerStar) { this.title = title; this.base = base; this.killsPerStar = killsPerStar; }
    }

    public static final int OFFERS = 3, MAX_NEED = 200, MAX_REWARD = 20, DAY_TICKS = 24000;

    /** The day number of a game time. */
    public static int day(long gameTime) { return (int) Math.max(0L, gameTime / DAY_TICKS); }

    /** A seed from a player's id (its two halves) and the day. */
    public static long seed(long idMost, long idLeast, int day) {
        long h = idMost * 0x9E3779B97F4A7C15L ^ idLeast * 0xC2B2AE3D27D4EB4FL ^ (long) day * 0x165667B19E3779F9L;
        h ^= h >>> 29; h *= 0xBF58476D1CE4E5B9L; h ^= h >>> 32;
        return h;
    }

    /** The three offered kinds: one of the four is left out, chosen by the seed, so every day differs and no kind is offered twice. */
    public static Kind[] offerKinds(long seed) {
        Kind[] all = Kind.values();
        int skip = (int) Math.floorMod(seed, (long) all.length);
        Kind[] out = new Kind[OFFERS];
        int n = 0;
        for (int i = 0; i < all.length; i++) if (i != skip && n < OFFERS) out[n++] = all[i];
        return out;
    }

    /** How many kills: the kind's base, 15 percent more per rank step (0 = Junior 5th ... 16), at most {@link #MAX_NEED}. */
    public static int need(Kind k, int step) { return (int) Math.min(MAX_NEED, Math.round(k.base * (1.0 + 0.15 * Math.max(0, Math.min(16, step))))); }

    /** Gold stars for the mission: one per {@code killsPerStar} kills, at least 1, at most {@link #MAX_REWARD}, times the setting (percent). */
    public static int reward(Kind k, int need, int percent) {
        long r = Math.round(Math.max(1, need) / (double) k.killsPerStar * Math.max(0, percent) / 100.0);
        return (int) Math.max(1L, Math.min(MAX_REWARD, r));
    }

    /** True while the mission has time left. */
    public static boolean open(long now, long until) { return now < until; }
}
