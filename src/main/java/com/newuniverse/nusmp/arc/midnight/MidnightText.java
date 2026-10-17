package com.newuniverse.nusmp.arc.midnight;

/**
 * Why: the Zogratis framework that every arc boss stands on was written for the devil-bound rulers of the Spade Kingdom, and a few of its lines are spoken in
 * a devil's voice that the framework builds itself: "The Gates of the Tree of Qliphoth open!" and "... rages: 40% -> 50% of Lilith's power." A party fighting
 * Fana or Patolli must not be told about the Tree of Qliphoth or about a devil who has nothing to do with them. The Goliath solved the same problem by
 * rewriting those lines on their way to the chat; this pure class holds that rewriting (and the boss bar title, and the action-bar gauge) so the rule is
 * tested and every Midnight Sun boss says the same thing in its own words.
 */
public final class MidnightText {
    private MidnightText() {}

    /** The line the framework announces when the Gates open, word for word (it is the key of the rewrite). */
    public static final String QLIPHOTH_GATES = "The Gates of the Tree of Qliphoth open!";

    /**
     * What the party reads instead of the framework's devil-voiced text: the Gates line becomes {@code gatesLine}, and "... of Lilith's power" becomes
     * "... of their strength". Any other text is returned unchanged; null becomes an empty string.
     */
    public static String rewrite(String text, String devilName, String gatesLine) {
        if (text == null) return "";
        if (QLIPHOTH_GATES.equals(text)) return gatesLine == null ? text : gatesLine;
        if (devilName != null && !devilName.isEmpty()) return text.replace("% of " + devilName + "'s power", "% of their strength");
        return text;
    }

    /** The boss bar title: "Patolli - Light 60%", and "... - Dark Elf" once the Gates are open. */
    public static String barTitle(String name, String word, int power, boolean gatesOpen, String gatesWord) {
        return name + " - " + word + " " + power + "%" + (gatesOpen && gatesWord != null && !gatesWord.isEmpty() ? " - " + gatesWord : "");
    }

    /** A text gauge for the action bar: {@code width} cells, filled to {@code fraction} (clamped to 0..1). */
    public static String gauge(float fraction, int width) {
        int w = Math.max(1, width);
        int filled = Math.round((float) MidnightMath.clamp(fraction, 0, 1) * w);
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < w; i++) b.append(i < filled ? '|' : '.');
        return b.append(']').toString();
    }

    /** Seconds with one decimal for a tick count ("7.5"), for the countdowns the party reads. */
    public static String seconds(int ticks) { return String.format(java.util.Locale.ROOT, "%.1f", Math.max(0, ticks) / 20.0); }
}
