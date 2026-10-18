package com.newuniverse.nusmp.elite.forge.rules;

/**
 * Why: the contract every crafting minigame keeps so that the server can trust none of them and still run all of them. A minigame is a PURE function of
 * {@code (seed, difficulty, input)}: the client plays it (drawing the state those same functions give) and reports its {@code input}, a short list of integers
 * (strike times, mouse samples, button samples or ring angles; each game documents its own); the server then {@link #validate validates} the shape of the
 * input, {@link #score re-scores} it with the very same code and reads how much time it {@link #claimedMillis claims}, which {@code SessionCheck} compares with the
 * real time that passed. The client never reports a score, so a score above what the sequence allows (1000) or from impossible input cannot exist. Physical limits
 * (how fast a hand can move, how often a button can toggle) are applied inside {@code score} (the input is coerced), not by rejecting, so an honest player is never
 * refused for a twitch. {@code perfectInput} gives an input that scores (nearly) the maximum: tests and the in-game self-test use it, and so can a bot, which is why
 * the honest description of the guarantee is "no forged, rushed or out-of-range result", not "no macro" (see docs/elite/forge.md). No Minecraft classes.
 */
public interface Minigame {
    /** Scores are in permille: 0..1000. */
    int MAX_SCORE = 1000;

    MinigameType type();

    /** The difficulty clamped to 1..5. */
    static int clampDifficulty(int d) { return Math.max(1, Math.min(5, d)); }

    /** Null when the input has a valid shape for this seed and difficulty, otherwise a short reason. Must not throw for any input. */
    String validate(long seed, int difficulty, int[] input);

    /** The score of a valid input, 0..1000. Must not throw for any input that {@link #validate} accepted. */
    int score(long seed, int difficulty, int[] input);

    /** How many milliseconds of play the input claims to have taken (never negative). */
    long claimedMillis(long seed, int difficulty, int[] input);

    /** The longest the step may take, in milliseconds (the client ends the step at this time). */
    int limitMillis(int difficulty);

    /** The most integers the input may hold (the payload cap). */
    int maxInputLength(int difficulty);

    /** An input that scores (close to) the maximum. */
    int[] perfectInput(long seed, int difficulty);
}
