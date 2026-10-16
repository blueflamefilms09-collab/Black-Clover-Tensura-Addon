package com.newuniverse.nusmp.boss.god;

import java.util.EnumMap;
import java.util.Map;

/**
 * Why: the owner wants Lucius to be theatrical: he speaks when the fight starts, when a stage ends, when he winds up an ultimate, when
 * he reverses one, when he punishes a channel, when his gem shatters and when he falls. The lines are plain data in one pure table (at
 * least four per event, unit tested) so that the words can be changed without touching the fight, and the pick is a pure function of
 * (event, the fight's seed, how many times that event has happened) so a replay, a reload or a test always hears the same sequence:
 * nothing here draws a random number.
 */
public final class LuciusLines {
    private LuciusLines() {}

    /** The moments he speaks at. Entries are only appended. */
    public enum Event { FIGHT_START, STAGE_2, STAGE_3, ULTIMATE_WINDUP, ULTIMATE_REVERSAL, CHECKMATE_COUNTER, GEM_SHATTER, DEFEAT }

    private static final Map<Event, String[]> LINES = new EnumMap<>(Event.class);

    static {
        LINES.put(Event.FIGHT_START, new String[]{
                "A king does not hurry to meet his guests. He lets the stage fill, and then he raises the curtain.",
                "You came all this way to be witnesses. Sit. Watch. The play has only one ending, and I wrote it.",
                "Heroes, demon lords, saints. How many titles it takes to hide how small a person is.",
                "Three acts. I will be generous: I will let you believe in the second."});
        LINES.put(Event.STAGE_2, new String[]{
                "Act one was a courtesy. You have earned the second, so kneel: the world is heavy, and I am the one who weighs it.",
                "Impressive. You made a god bleed. Now watch what he does with the blood.",
                "I rewound the hour, and the hour obeyed. Gravity and blood, then. Do try to stand.",
                "You broke the first curtain. The second is made of weight, and it falls on everyone."});
        LINES.put(Event.STAGE_3, new String[]{
                "One of me was never enough for you. Then let there be a crowd, and let every one of us be the real one.",
                "Space folds. Time returns. The last act has no audience, only the ones who remain standing.",
                "You are still here. Remarkable. Then I will stop holding the door open and close it.",
                "Final act. Look closely: whichever Lucius you strike, I was already behind you."});
        LINES.put(Event.ULTIMATE_WINDUP, new String[]{
                "Do not look away. This one is worth remembering.",
                "Hold your breath. The world is about to be edited.",
                "You will want to run. Running is also in the script.",
                "A grand finale needs a grand silence. Here it comes."});
        LINES.put(Event.ULTIMATE_REVERSAL, new String[]{
                "Your finest card, and I am the one who shuffles. Take it back.",
                "A power that great deserves to be returned to its owner. With interest.",
                "Time reverses, space reflects. Tell me, how does your own ultimate feel?",
                "Predictable. The strongest hand is always the one I have already seen."});
        LINES.put(Event.CHECKMATE_COUNTER, new String[]{
                "Checkmate. You spent so long preparing your last move that I had time to prepare mine.",
                "Ah. The moment someone gathers everything into one spell, they forget they have a back.",
                "A long chant is an invitation. I accept.",
                "You looked up to cast. I looked behind you to end it."});
        LINES.put(Event.GEM_SHATTER, new String[]{
                "Impossible... that was the core of every copy of me!",
                "You found the seam in the illusion. Whoever taught you to look there?",
                "My resonance... broken! Do not mistake this for the end.",
                "The copies fade. The man does not."});
        LINES.put(Event.DEFEAT, new String[]{
                "So this is how it ends. Julius... did you win after all?",
                "Three acts, and the villain reads his last line. Well played, all of you.",
                "The stage goes quiet. Strange. I expected to mind.",
                "Remember me as the one you could not beat... until you did."});
    }

    /** How many lines an event has (always at least four). */
    public static int count(Event e) { return LINES.get(e).length; }

    /** Every line of an event (a copy). */
    public static String[] all(Event e) { return LINES.get(e).clone(); }

    /**
     * The line for the n-th time this event happens in the fight with this seed. Deterministic; the sequence for a seed is a rotation
     * of the whole list, so no line repeats before every line has been heard once.
     */
    public static String pick(Event e, long fightSeed, int occurrence) {
        String[] lines = LINES.get(e);
        long h = fightSeed * 0x9E3779B97F4A7C15L + e.ordinal() * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 29;
        int start = (int) Math.floorMod(h, (long) lines.length);
        return lines[Math.floorMod(start + Math.max(0, occurrence), lines.length)];
    }

    /** The title shown on the screen when a stage begins. */
    public static String stageTitle(int stage) {
        return switch (Math.max(1, Math.min(3, stage))) {
            case 1 -> "ACT I";
            case 2 -> "ACT II";
            default -> "ACT III";
        };
    }

    /** The subtitle under it: the arsenal that opens. */
    public static String stageSubtitle(int stage) { return LuciusStage.arsenal(stage); }
}
