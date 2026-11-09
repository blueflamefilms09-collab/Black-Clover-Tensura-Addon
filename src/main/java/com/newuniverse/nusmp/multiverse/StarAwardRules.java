package com.newuniverse.nusmp.multiverse;

/**
 * Why: the Star Awards Festival (3.68.0): in the anime the Magic Knights hold a festival at the end of the year that names the squad with the most stars. Here it comes every N in-game days; the top three squads
 * share gold stars among their online members. Pure numbers, unit tested. Cost: one integer division when it is checked (every 10 seconds).
 */
public final class StarAwardRules {
    private StarAwardRules() {}

    public static final int MAX_DAYS = 3650;

    /** The festival period a game time lies in (period 0 is the first {@code days} days). */
    public static int period(long gameTime, int days) {
        long len = 24000L * Math.max(1, Math.min(MAX_DAYS, days));
        return (int) Math.max(0L, gameTime / len);
    }

    /** Gold stars for a place (1 = best): the three settings, nothing from the fourth place on. */
    public static int reward(int place, int first, int second, int third) {
        return place == 1 ? Math.max(0, first) : place == 2 ? Math.max(0, second) : place == 3 ? Math.max(0, third) : 0;
    }

    /** True if a festival is due: the saved period is set (not -1) and the current one is later. */
    public static boolean due(int saved, int now) { return saved >= 0 && now > saved; }
}
