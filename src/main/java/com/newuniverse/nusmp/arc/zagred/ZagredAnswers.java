package com.newuniverse.nusmp.arc.zagred;

import java.util.List;

/**
 * Why: a raid fight is only fair when the right answer to a mechanic is something a player can DO and the server can CHECK. Zagred's verdicts put a word of
 * their own over a player's head and give three seconds to answer it; this class is the single list of answers (keep moving, jump, raise a shield,
 * break his line of sight, hold anti-magic, meet his gaze) and the pure test "does this player's last half second satisfy that answer". The game side only
 * has to fill a {@link Snapshot} of facts about the player; every rule about what counts lives here, so it is written down once, unit tested, and
 * cannot silently differ between the on-screen text and the check.
 *
 * <p>The thresholds are lenient on purpose: they look at the last half second, not at one tick, because a player's movement reaches the server in
 * packets and a check on a single tick would punish lag instead of mistakes.
 */
public final class ZagredAnswers {
    private ZagredAnswers() {}

    /** Blocks a player must have covered in the last ten ticks to count as "moving" (walking pace is about 2.1; sneaking about 0.7). */
    public static final double MOVE_NEEDED = 1.2;
    /** Dot product of the player's view and the direction to his eyes that counts as "looking at him" (about 37 degrees). */
    public static final double GAZE_NEEDED = 0.80;

    /** What a player can do about a word. */
    public enum Answer {
        KEEP_MOVING("Keep moving"),
        JUMP("Jump"),
        SHIELD("Raise your shield"),
        BREAK_SIGHT("Break his line of sight"),
        ANTI_MAGIC("Hold anti-magic"),
        MEET_GAZE("Look straight at him");

        private final String hint;

        Answer(String hint) { this.hint = hint; }

        /** The instruction shown to the marked player (English; the language file may translate it). */
        public String hint() { return hint; }

        /** The language key of the instruction. */
        public String hintKey() { return "message.nusmp.zagred.answer." + name().toLowerCase(java.util.Locale.ROOT); }
    }

    /**
     * The facts about one player over the last half second.
     *
     * @param airborne  was off the ground at some point in the last five ticks
     * @param moved     blocks of horizontal distance covered in the last ten ticks
     * @param shielding was blocking with a shield at some point in the last ten ticks
     * @param seesBoss  has a clear line of sight to him now
     * @param antiMagic holds anti-magic (a demon blade, the Anti-Magic Lord, an anti-magic grimoire)
     * @param gaze      dot product of the player's view direction and the direction to his eyes (1 = looking straight at him)
     */
    public record Snapshot(boolean airborne, double moved, boolean shielding, boolean seesBoss, boolean antiMagic, double gaze) {}

    /** Does the snapshot satisfy this one answer? */
    public static boolean met(Answer a, Snapshot s) {
        if (a == null || s == null) return false;
        return switch (a) {
            case KEEP_MOVING -> s.moved() >= MOVE_NEEDED;
            case JUMP -> s.airborne();
            case SHIELD -> s.shielding();
            case BREAK_SIGHT -> !s.seesBoss();
            case ANTI_MAGIC -> s.antiMagic();
            case MEET_GAZE -> s.gaze() >= GAZE_NEEDED;
        };
    }

    /** The answers that spare a player from this word when it is written over their head (empty for a word that is never a verdict). */
    public static List<Answer> answersOf(ZagredWord w) {
        if (w == null) return List.of();
        return switch (w) {
            case HALT -> List.of(Answer.KEEP_MOVING);
            case FALL -> List.of(Answer.JUMP);
            case SEAL -> List.of(Answer.BREAK_SIGHT);
            case SWORDS -> List.of(Answer.SHIELD);
            case COWER -> List.of(Answer.MEET_GAZE);
            default -> List.of();
        };
    }

    /** The answer the marked player is TOLD to give (the first of {@link #answersOf}), or null. */
    public static Answer told(ZagredWord w) {
        List<Answer> a = answersOf(w);
        return a.isEmpty() ? null : a.get(0);
    }

    /** Is this player spared from the word? Anti-magic spares from the words that are magic of the mind, whatever else they do. */
    public static boolean spared(ZagredWord w, Snapshot s) {
        if (w == null || s == null) return false;
        if (w.antiMagicExempt() && s.antiMagic()) return true;
        for (Answer a : answersOf(w)) if (met(a, s)) return true;
        return false;
    }
}
