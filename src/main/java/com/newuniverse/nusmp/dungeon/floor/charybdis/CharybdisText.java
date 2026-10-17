package com.newuniverse.nusmp.dungeon.floor.charybdis;

/**
 * What the Charybdis Spawn and its basin say (pure, unit tested). The leviathan is a beast of the deep, not one of the Zogratis rulers, so every
 * line the fight framework would put in a devil's mouth is replaced here with the beast's own telling, as narration: nothing mentions a devil,
 * a contract or the Tree of Qliphoth.
 */
public final class CharybdisText {
    private CharybdisText() {}

    /**
     * The one line the framework writes itself when the Gates open (it names the Tree of Qliphoth). The framework follows it at once with
     * {@code gatesLine()}, which is {@link #gates}: so the leviathan drops this one.
     */
    public static final String FRAMEWORK_GATES = "The Gates of the Tree of Qliphoth open!";

    /** A line the framework announces, as the leviathan lets it be said: unchanged, or empty when it is not to be said at all. */
    public static String translate(String framework) {
        if (framework == null || FRAMEWORK_GATES.equals(framework)) return "";
        return framework;
    }

    public static String opening() { return "The water darkens and the Charybdis Spawn turns its great eye on you: the deep keeps what it guards."; }

    /** The line for the leviathan drawing more of its strength (the percentage is the framework's power steps: 40, 60, 80). */
    public static String power(int old, int now) {
        if (now >= 60 && old < 60) return "The leviathan's scales blaze and its magicules run unbound: the Maelstrom of Scales is coming.";
        if (now >= 80 && old < 80) return "The tide swells to " + now + "%: the scales fly thick and fast.";
        return "The leviathan rises with the tide: " + now + "% of its strength.";
    }

    public static String gates() { return "The Charybdis Spawn flushes bright and the whole basin shudders: it fights with everything it has!"; }

    public static String death() { return "The Charybdis Spawn sinks, its scales going dark one by one. The basin groans."; }

    /** The hint that tells a new player the one thing the fight is about. */
    public static String hint() {
        return "Magicule scales are turned, not tanked: raise a shield, open a Magebane Shield's parry or strike a scale, and it flies back at the leviathan.";
    }

    /** Said to the players in the basin when the leviathan starts to surface. */
    public static String surfacing() { return "The water shudders. Something vast is rising beneath the dais."; }

    public static String megalodons() { return "Megalodons tear loose from the basin walls and come for you."; }

    public static String maelstrom() { return "The leviathan spins the water into a Maelstrom and fills it with scales."; }

    /** The server-wide line when the leviathan falls, wherever it was called: no basin comes apart (the basin was removed with the Living Dungeon in 1.58.0). */
    public static String slain(String who) {
        return who == null ? "The Charybdis Spawn has been slain!" : who + " has slain the Charybdis Spawn!";
    }

    /** The server-wide line when the leviathan falls and the basin begins to collapse ('who' is the killer's name, or null). Not said since 1.58.0: the boss says {@link #slain}. */
    public static String fallen(String who) {
        return (who == null ? "The Charybdis Spawn has been slain!" : who + " has slain the Charybdis Spawn!")
                + " The drowned basin begins to come apart: take the relics and swim for the way out.";
    }

    /** The same when the central relic was taken first and the basin is already coming down. */
    public static String fallenDuringCollapse(String who) {
        return (who == null ? "The Charybdis Spawn has been slain" : who + " has slain the Charybdis Spawn") + ", but the basin was already coming down.";
    }
}
