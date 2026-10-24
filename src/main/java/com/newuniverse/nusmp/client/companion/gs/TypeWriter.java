package com.newuniverse.nusmp.client.companion.gs;

/**
 * The typed-out dialogue of the Great Spirit screen: her words appear a few characters a tick; a click on the box (or switching the effect off) shows them all at once. Counts characters over the whole
 * block of wrapped lines so the painter can cut each line without building a new text block. Pure, unit tested.
 */
public final class TypeWriter {
    private TypeWriter() {}

    /** Characters revealed per tick. */
    public static final float CHARS_PER_TICK = 1.9f;
    /** The longest dialogue that is typed at all: a longer one shows at once (a page of text is not a typewriter's job). */
    public static final int MAX_TYPED = 420;

    /** Total characters of the lines. */
    public static int total(String[] lines) {
        int n = 0;
        if (lines != null) for (String l : lines) n += l == null ? 0 : l.length();
        return n;
    }

    /** How many characters are showing 'age' ticks after the text arrived ({@code Integer.MAX_VALUE} when typing is off or the text is too long). */
    public static int shown(int total, float age, boolean enabled) {
        if (!enabled || total > MAX_TYPED) return Integer.MAX_VALUE;
        if (age <= 0f) return 0;
        long n = (long) Math.floor(age * CHARS_PER_TICK);
        return (int) Math.min(n, Integer.MAX_VALUE - 1);
    }

    public static boolean done(int total, float age, boolean enabled) { return shown(total, age, enabled) >= total; }

    /** The ticks the whole text takes to type. */
    public static float duration(int total) { return total / CHARS_PER_TICK; }

    /** How many characters of line 'line' show when 'chars' characters of the whole block are showing. */
    public static int ofLine(String[] lines, int line, int chars) {
        if (lines == null || line < 0 || line >= lines.length) return 0;
        int before = 0;
        for (int i = 0; i < line; i++) before += lines[i].length();
        int left = chars == Integer.MAX_VALUE ? Integer.MAX_VALUE : chars - before;
        return Math.max(0, Math.min(lines[line].length(), left));
    }
}
