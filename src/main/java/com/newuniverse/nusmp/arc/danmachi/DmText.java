package com.newuniverse.nusmp.arc.danmachi;

/**
 * Why: the DanMachi monsters talk to the players through short lines (the boss bar, the notice at the Gates, the hints that teach a counter), and the
 * Dungeon Level notice is the one place where a player learns that the fight depends on how strong the party is. Keeping the wording in one pure class
 * means the numbers in the lines are always the numbers the rules used, and a test can check that no line is ever empty or shows a wrong Level.
 */
public final class DmText {
    private DmText() {}

    /** The boss bar: "Minotaur Lv.2 - 40%", then "- Gates open" at 100%, or "- Leveled" when the Gates also Levelled it up. */
    public static String bar(String name, int level, int power, boolean gatesOpen, boolean leveled) {
        String tail = !gatesOpen ? "" : leveled ? " - Leveled" : " - Gates open";
        return name + " " + DungeonLevel.label(level) + " - " + power + "%" + tail;
    }

    /** The notice when the Gates Level a monster up. */
    public static String levelUp(String name, int from, int to, int steps) {
        return "The Dungeon answers your strength: " + name + " Levels up, " + DungeonLevel.label(from) + " to " + DungeonLevel.label(to)
                + (steps > 1 ? " (" + steps + " steps)." : ".");
    }

    /** The notice when the party is too weak for the monster to need it (so the player knows the rule exists, and what to do about it). */
    public static String holds(String name, int partyLevel, int bossLevel, int margin) {
        int need = bossLevel + margin;
        return "Dungeon Level: your party is " + (partyLevel <= 0 ? "unmeasured" : DungeonLevel.label(partyLevel)) + "; " + name + " stays "
                + DungeonLevel.label(bossLevel) + " until the party reaches " + DungeonLevel.label(Math.min(DungeonLevel.MAX_LEVEL, Math.max(DungeonLevel.MIN_LEVEL, need))) + ".";
    }

    /** What the player sees (action bar) when a plain blow glances off the Black Goliath's hide. */
    public static String glance() { return "Your blow glances off the black hide. Strike hard, or use a spell."; }

    /** The notice when a plate of the black hide splits. */
    public static String plate(int split, int needed) { return "A plate of the black hide splits (" + split + "/" + needed + ")."; }

    /** The line when the black hide breaks. */
    public static String hideBroken(String name) { return "The black hide of " + name + " breaks! Everything lands in full."; }

    /** The line when a rib cracks. */
    public static String ribCracked(int cracks, int needed) { return "A rib cracks (" + cracks + "/" + needed + "): strike it from another side."; }

    /** The line when the cage breaks. */
    public static String cageBroken(String name) { return "The rib cage of " + name + " breaks open: the magic stone shows!"; }

    /** The line when the red head is cut. */
    public static String headCut(String name) { return "The red head of " + name + " is cut off! The mist is gone and magic works again."; }

    /** The line when the party steals the Minotaur's mark. */
    public static String markStolen(String hunted) { return hunted + " draws the Minotaur's eye."; }

    /** The line to the one the Minotaur has marked. */
    public static String marked() { return "The Minotaur has marked you. Make a louder prey of someone else, or run it into a wall."; }

    /** The line when the Minotaur rams a wall. */
    public static String crashed(String name) { return name + " crashes into the wall and is stunned!"; }
}
