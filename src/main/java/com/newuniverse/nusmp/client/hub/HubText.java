package com.newuniverse.nusmp.client.hub;

import java.util.function.ToIntFunction;

/**
 * Why: every text of the hub must fit its box at every window size and in every language, and a text that is cut must say so (the hub shows the full text in a tooltip). This is the pure fitting rule, with the
 * width measure passed in (the game's font in the screen, six pixels a character in the tests), so it is unit tested without Minecraft. It runs when a page is cooked (a sync, an open, a width change), never
 * per frame.
 */
public final class HubText {
    private HubText() {}

    public static final String ELLIPSIS = "...";

    /** A text ready to draw: what is shown, the whole text, and whether the shown text is a cut of it. */
    public record Fit(String shown, String full, boolean cut) {}

    /** The text itself when it fits {@code maxW}, else its longest prefix that fits with "..." after it (just the dots when not even one letter fits, "" when {@code maxW} is under the dots). */
    public static Fit fit(String text, int maxW, ToIntFunction<String> width) {
        String s = text == null ? "" : text;
        if (width.applyAsInt(s) <= maxW) return new Fit(s, s, false);
        int dots = width.applyAsInt(ELLIPSIS);
        if (dots > maxW) return new Fit("", s, true);
        int lo = 0, hi = s.length();                                              // the longest n with width(prefix n) + dots <= maxW; widths only grow with n
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (width.applyAsInt(s.substring(0, mid)) + dots <= maxW) lo = mid;
            else hi = mid - 1;
        }
        String head = s.substring(0, lo).stripTrailing();
        return new Fit(head + ELLIPSIS, s, true);
    }
}
