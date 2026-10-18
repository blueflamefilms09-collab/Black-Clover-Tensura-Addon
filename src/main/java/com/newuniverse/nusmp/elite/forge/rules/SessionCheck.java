package com.newuniverse.nusmp.elite.forge.rules;

import java.util.List;

/**
 * Why: the server-side validation of a forging session, the part that makes the minigames "server-validated". The client reports, for each step of the recipe's
 * sequence, the input it played; this class re-scores every step with the pure minigame ({@link Minigames}), and judges the whole against the real time the server
 * saw pass between handing out the seeds and receiving the result:
 * <ul>
 * <li>the number of inputs must equal the number of steps, and every input must have a valid shape (else the result is refused);</li>
 * <li>the time the inputs CLAIM to have taken (strike times, whole sampled durations, lock times) may not be more than the real elapsed time plus a slack of
 * 3 seconds and 5% (a result cannot arrive faster than it can be played);</li>
 * <li>the real elapsed time may not exceed the sum of the steps' time limits plus two minutes (an old session is refused: seeds are not kept forever);</li>
 * <li>each step's score is clamped to 0..1000 and the total is their mean, so no score above what the sequence allows can come out of any input.</li>
 * </ul>
 * This stops forged packets, out-of-range numbers and results that were not played for the time they claim; it cannot tell a person from a macro that plays the
 * game perfectly in real time (nothing on the server can): that is the honest limit and the design relies on the station ceilings and costs, not on secrecy.
 * No Minecraft classes.
 */
public final class SessionCheck {
    private SessionCheck() {}

    /** One step of a recipe's sequence. */
    public record Step(MinigameType type, int difficulty) {
        public Step {
            difficulty = Minigame.clampDifficulty(difficulty);
        }
    }

    /** The verdict: accepted or not (with a reason), the per-step scores and the total, all in permille. */
    public record Verdict(boolean ok, String reason, int[] stepScores, int total) {
        public static Verdict refuse(String reason) { return new Verdict(false, reason, new int[0], 0); }

        /** The total as 0..1. */
        public double fraction() { return total / (double) Minigame.MAX_SCORE; }
    }

    /** Fixed slack allowed between the claimed and the real time, ms. */
    public static final long SLACK_MS = 3000L;
    /** Extra real time allowed after the steps' limits, ms. */
    public static final long GRACE_MS = 120_000L;

    /** The longest a session may stay open: the sum of the steps' limits plus the grace. */
    public static long maxElapsedMillis(List<Step> steps) {
        long sum = GRACE_MS;
        for (Step s : steps) sum += Minigames.of(s.type()).limitMillis(s.difficulty());
        return sum;
    }

    public static Verdict evaluate(List<Step> steps, long[] seeds, List<int[]> inputs, long elapsedMillis) {
        if (steps == null || steps.isEmpty()) return Verdict.refuse("no steps");
        if (seeds == null || seeds.length != steps.size()) return Verdict.refuse("seeds do not match the steps");
        if (inputs == null || inputs.size() != steps.size()) return Verdict.refuse("wrong number of step results");
        if (elapsedMillis < 0) return Verdict.refuse("negative time");
        if (elapsedMillis > maxElapsedMillis(steps)) return Verdict.refuse("the session ran out of time");
        long claimed = 0L;
        int[] scores = new int[steps.size()];
        long sum = 0L;
        for (int i = 0; i < steps.size(); i++) {
            Step s = steps.get(i);
            Minigame g = Minigames.of(s.type());
            int[] in = inputs.get(i);
            if (in == null) return Verdict.refuse("step " + (i + 1) + ": no input");
            if (in.length > g.maxInputLength(s.difficulty())) return Verdict.refuse("step " + (i + 1) + ": too much input");
            String why = g.validate(seeds[i], s.difficulty(), in);
            if (why != null) return Verdict.refuse("step " + (i + 1) + ": " + why);
            claimed += g.claimedMillis(seeds[i], s.difficulty(), in);
            int sc = Math.max(0, Math.min(Minigame.MAX_SCORE, g.score(seeds[i], s.difficulty(), in)));
            scores[i] = sc;
            sum += sc;
        }
        if (claimed > elapsedMillis + SLACK_MS + elapsedMillis / 20) return Verdict.refuse("the result came faster than it can be played");
        int total = (int) Math.round(sum / (double) steps.size());
        return new Verdict(true, "", scores, Math.max(0, Math.min(Minigame.MAX_SCORE, total)));
    }
}
