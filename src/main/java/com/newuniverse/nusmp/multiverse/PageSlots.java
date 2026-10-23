package com.newuniverse.nusmp.multiverse;

/**
 * Why: the number of grimoire pages a player can have open at once (the "slots") is pure arithmetic, so it lives here, without a Minecraft class, and is unit tested. {@link MasteryPages#slots} reads the server
 * config and the grimoire and hands the numbers in.
 *
 * <p>"15 spells for every grimoire": the defaults are floor 6, ceiling 15, contract bonus 4, cap 19. The ceiling is never below the number of pages the grimoire can show ({@code visible}), so at full mastery EVERY
 * visible page can be open, and an old config file that still says 12 / 16 keeps working (the larger value wins). A book that shows fewer pages than the configured ceiling behaves exactly as before.
 */
public final class PageSlots {
    private PageSlots() {}

    public static int slots(int floor, int configuredCeiling, int contractBonus, int configuredCap, int visible, double masteryFrac, boolean contract) {
        int ceil = Math.max(Math.max(floor, configuredCeiling), visible);
        int s = floor + (int) Math.floor(Math.max(0.0, Math.min(1.0, masteryFrac)) * (ceil - floor));
        if (contract) s += contractBonus;
        int cap = Math.max(configuredCap, visible);      // the cap never blocks a visible page
        return Math.min(s, cap);
    }
}
