package com.newuniverse.nusmp.client.companion.gs;

/**
 * The five expressions of a companion's portrait. Which one shows is decided on the client from what the server already said (the key of her last line, the rapport tier): nothing is sent for it. The
 * constants are only ever appended (the order is the order of the face rectangles in {@link PortraitPlan}).
 */
public enum Mood {
    NEUTRAL, HAPPY, SHY, ANGRY, HUNGRY;

    /** The mood of the portrait when nothing has just happened: strangers are shy, close friends smile softly, the rest are neutral. */
    public static Mood resting(int tier) {
        return tier <= 0 ? SHY : tier >= 3 ? HAPPY : NEUTRAL;
    }
}
