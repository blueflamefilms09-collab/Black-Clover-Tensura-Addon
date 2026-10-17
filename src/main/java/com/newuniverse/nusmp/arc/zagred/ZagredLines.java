package com.newuniverse.nusmp.arc.zagred;

/**
 * Why: Zagred is a character who fights with speech, so what he SAYS is part of the fight, and the lines should be one reviewable list in his voice (cold,
 * precise, amused, a devil who treats spells as sentences and people as the readers of them) instead of strings scattered through the boss class. The boss
 * class picks one at random and speaks it with a cooldown so the chat is not a wall; the Words themselves are shown on the screen by the title packets, so a
 * line here is flavour, never information. The lines are original; they use the story's facts (the Word Soul, the heart that rebuilds the body, the
 * anti-magic that reads nothing) and none of the text of the series.
 */
public final class ZagredLines {
    private ZagredLines() {}

    /** Spoken when the fight begins (the character record carries the main opening; these vary the greeting for later fights). */
    public static final String OPENING = "Every spell you know is a sentence I wrote first. Listen, and learn how it ends.";
    /** Spoken when the Gates open and the body is given up for the devil's. */
    public static final String GATES = "Enough. The body was only a quotation. Now hear the one who speaks it.";
    /** The last words. */
    public static final String DEATH = "A word... that I did not choose. Then let it be the last.";

    /** What he says as the fight reaches act II, III and IV (index 0 is act II). */
    public static final String[][] ACT = {
            {"Your magic is already in my book. Let me turn the page.", "You read slowly. I will speak slowly... for now."},
            {"Now I will name you, one at a time.", "A verdict for each of you. Choose your answers well."},
            {"Enough reading. I will write the ending myself.", "The sentence is mine to rewrite, and I begin."}
    };

    /** One short line per Word, spoken as it is cast. */
    public static String[] forWord(ZagredWord w) {
        return switch (w) {
            case HALT -> new String[]{"Halt.", "Be still.", "Not one step more."};
            case FALL -> new String[]{"Fall.", "Down.", "Even the sky obeys the word for down."};
            case TRIDENT -> new String[]{"Pierce.", "A word with a point."};
            case SWORDS -> new String[]{"Swords.", "Let them fall."};
            case SEAL -> new String[]{"Seal.", "Be quiet. Your spell ends here."};
            case REDACT -> new String[]{"Redact.", "Let me strike you out.", "I erase this line."};
            case SHATTER -> new String[]{"Shatter.", "Your arrows are only words, and I correct them."};
            case REVERSE -> new String[]{"Reverse.", "Strike me, and strike yourself."};
            case BANISH -> new String[]{"Banish.", "Too close."};
            case VERDICT -> new String[]{"Verdict.", "Each of you, hear your word."};
            case OVERWRITE -> new String[]{"Overwrite.", "This is the law now."};
            case COWER -> new String[]{"Cower."};
        };
    }

    /** An anti-magic blow cut his sentence. */
    public static final String[] CUT = {"You cut it. A blade that reads nothing and breaks everything.", "Anti-magic. Of course. It has no grammar to answer."};

    /** The ward is broken and he writes it again. */
    public static final String[] UNWRITTEN = {"Again.", "I will write it again. Slower."};

    /** A verdict was answered by everyone it named. */
    public static final String[] ANSWERED = {"You answered. Impressive, for a quotation.", "Correct. Do not grow fond of it."};

    /** Three stones broke the sentence. */
    public static final String STONES = "My stones... you read the margins. Clever.";

    /** The heart opened. */
    public static final String HEART = "Look, then. This is what the body was hiding.";
}
