package com.newuniverse.nusmp.dungeon.floor.goliath;

/**
 * The Goliath's words (pure, unit tested): its lines in the chat, the narration of its fight, what the server is told when it wakes and when
 * it falls, and the name on its boss bar. The framework the Goliath is built on writes its own lines in the voice of a devil's contractor
 * (the Gates of the Tree of Qliphoth, "of Astaroth's power"); {@link #rewrite} turns the ones it cannot be told not to write into the
 * Goliath's own, so nothing a player reads names a devil.
 */
public final class GoliathText {
    private GoliathText() {}

    public static final String NAME = "The Goliath";

    /** Words of the framework's devil voice that must never reach a player in the Goliath's fight. */
    private static final String[] DEVIL_WORDS = {"Qliphoth", "Lucifero", "Megicula", "Beelzebub", "Astaroth", "devil", "Devil", "Zogratis", "Dark Disciple"};

    // ---------------------------------------------------------------- spoken in the chat
    public static String opening() { return "You walk on my floor."; }

    /** The line when its power rises from `old` to `now` percent (the steps of the curve: 20, 40, 60, 80). */
    public static String power(int old, int now) {
        if (now >= 80) return "Heavier still. The whole pit leans on you.";
        if (now >= 60) return "Stone does not tire.";
        if (now >= 40) return "The floor wakes. It is mine.";
        return "Weight, and more weight.";
    }

    public static String gates() { return "Break me, then. I am only bedrock."; }

    public static String death() { return "The seal lets go. Take what lies under it."; }

    // ---------------------------------------------------------------- narration
    /** What the nearby players read when the seal begins to crack. */
    public static String cracking() { return "The bedrock seal in the middle of the pit is cracking."; }

    public static String risen() { return "The Goliath has risen."; }

    /** The line that replaces the framework's announcement of the Gates opening. */
    public static String gatesNarration() { return "The Goliath's stone splits open. Amber light bleeds through the cracks, and it moves faster."; }

    public static String hintWaves() { return "The Goliath fights with its weight: jump the rings that cross the floor, and leave every marked circle."; }

    public static String hintExposed() { return "It is slow. Keep moving, and strike it when it recovers. At a quarter of its health its stone cracks and it fights harder."; }

    // ---------------------------------------------------------------- told to the server
    /** The server-wide line when it wakes: `player` is who woke it, or null. */
    public static String woken(String player) {
        String who = player == null || player.isBlank() ? "Something" : player;
        return "The bedrock under the Upper Catacombs has cracked. The Goliath is rising, and " + who + " woke it.";
    }

    /** The server-wide line when it falls and the pit it stood in begins to come down. Not said since 1.58.0 (the pit was removed): the boss says {@link #slain}. */
    public static String fallen(String player) {
        return player == null || player.isBlank() ? "The Goliath has fallen. Its pit begins to come down."
                : "The Goliath has fallen to " + player + ". Its pit begins to come down.";
    }

    /** The server-wide line when it falls, wherever it was called: no pit comes down (the pit was removed with the Living Dungeon in 1.58.0). */
    public static String slain(String player) {
        return player == null || player.isBlank() ? "The Goliath has fallen." : "The Goliath has fallen to " + player + ".";
    }

    /** The server-wide line when it falls after the pit has already begun to come down (a relic was taken first). */
    public static String fallenDuringCollapse(String player) {
        return player == null || player.isBlank() ? "The Goliath has fallen." : "The Goliath has fallen to " + player + ".";
    }

    // ---------------------------------------------------------------- the boss bar
    /** "The Goliath - Fury 40%", and "- Cracked" once its stone has split. */
    public static String bar(int power, boolean gatesOpen) { return NAME + " - Fury " + Math.max(0, power) + "%" + (gatesOpen ? " - Cracked" : ""); }

    // ---------------------------------------------------------------- the framework's own lines
    /** Does this text name a devil (or the framework's devil lore)? */
    public static boolean leaks(String text) {
        if (text == null) return false;
        for (String w : DEVIL_WORDS) if (text.contains(w)) return true;
        return false;
    }

    /**
     * What the Goliath says in place of a line the framework wrote: the Gates become its stone splitting, any other line that names a devil
     * becomes a plain growl of rage, and everything else is left as it is.
     */
    public static String rewrite(String text) {
        if (text == null) return "";
        if (text.contains("Qliphoth")) return gatesNarration();
        if (leaks(text)) return "The Goliath's stone shakes with a new fury.";
        return text;
    }
}
