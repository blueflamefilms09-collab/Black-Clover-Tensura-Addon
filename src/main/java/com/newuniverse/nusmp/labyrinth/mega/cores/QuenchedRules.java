package com.newuniverse.nusmp.labyrinth.mega.cores;

/**
 * Why (pure, no Minecraft class): the numbers and text of the data-driven Telesma / Malice bridge ({@link QuenchedBridge}). The other mod (Tensura No Index, "Quenched") has no API this repo can read,
 * so the bridge does not call it: the server owner gives the one command their mod answers to, and these rules turn an Ascendant EP gain into the amount that goes into it and make sure the command
 * text is safe to run (one line, a valid player name, a bounded amount).
 */
public final class QuenchedRules {
    private QuenchedRules() {}

    /** The longest command the bridge will run (a vanilla chat line is 256). */
    public static final int MAX_COMMAND = 256;
    /** One Telesma / Malice payment per player at most this often (ticks). */
    public static final int MIN_INTERVAL_TICKS = 10;

    /** The amount for an EP gain: {@code gain x perEp}, rounded down, 0 for a bad number, never above {@code maxPerKill}. */
    public static long amount(double epGain, double perEp, long maxPerKill) {
        if (!(epGain > 0.0) || !(perEp > 0.0) || maxPerKill <= 0 || Double.isInfinite(epGain) || Double.isInfinite(perEp)) return 0L;
        double v = Math.floor(epGain * perEp);
        if (!(v >= 1.0)) return 0L;
        return v >= maxPerKill ? maxPerKill : (long) v;
    }

    /** A vanilla player name: 1-16 letters, digits or underscores (it is written into a command). */
    public static boolean safeName(String name) {
        if (name == null || name.isEmpty() || name.length() > 16) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_')) return false;
        }
        return true;
    }

    /**
     * The command to run: the template with {@code {player}} and {@code {amount}} filled in and one leading slash dropped. Null when there is nothing to run: a blank template, a bad name, no
     * amount, a template with a line break or a control character, or a result over {@link #MAX_COMMAND}.
     */
    public static String render(String template, String playerName, long amount) {
        if (template == null || amount <= 0 || !safeName(playerName)) return null;
        String t = template.strip();
        if (t.startsWith("/")) t = t.substring(1);
        if (t.isEmpty()) return null;
        for (int i = 0; i < t.length(); i++) if (t.charAt(i) < ' ' || t.charAt(i) == 0x7f) return null;
        String out = t.replace("{player}", playerName).replace("{amount}", Long.toString(amount));
        return out.length() > MAX_COMMAND ? null : out;
    }

    /** May a payment run now? (the last one at {@code lastTick}, or a negative number for none). A game clock that went backwards allows it. */
    public static boolean due(long lastTick, long now) {
        return lastTick < 0 || now < lastTick || now - lastTick >= MIN_INTERVAL_TICKS;
    }
}
