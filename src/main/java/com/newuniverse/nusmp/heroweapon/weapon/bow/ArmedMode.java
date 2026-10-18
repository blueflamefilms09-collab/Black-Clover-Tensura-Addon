package com.newuniverse.nusmp.heroweapon.weapon.bow;

import java.util.Locale;

/**
 * Why: the Legendary Bow is drawn like a bow, so its techniques on the sneak and hero keys do not replace the shot, they CHANGE the next one (a sniper
 * arrow, a net, a mark, a burst, a seeker, a second arrow, Pride's command). Which change is armed, and how full a draw it needs before it is loosed, is
 * a small fixed table; keeping it as a pure enum lets the bond remember the armed mode by its text id and lets a test pin every threshold. A draw weaker
 * than the mode asks does not waste the arming: a plain arrow flies and the arming waits for a fuller draw.
 */
public enum ArmedMode {
    NONE("none", 0.0, ""),
    SNIPER("sniper", 0.9, "Eagle Piercing Shot"),
    NET("net", 0.5, "Bind Arrow"),
    MARK("mark", 0.3, "Marking Arrow"),
    BURST("burst", 0.5, "Burst Arrow"),
    SEEK("seek", 0.4, "Seeker Arrow"),
    SECOND("second", 0.5, "Second Arrow"),
    COMMAND("command", 0.4, "Pride's Command");

    private final String id, label;
    private final double minFraction;

    ArmedMode(String id, double minFraction, String label) {
        this.id = id;
        this.minFraction = minFraction;
        this.label = label;
    }

    /** The text the bond stores. */
    public String id() { return id; }

    /** The share of a full draw this mode needs (0 to 1). */
    public double minFraction() { return minFraction; }

    /** The technique's name, for the action bar. */
    public String label() { return label; }

    /** The mode with this stored id; {@link #NONE} for null, empty or unknown text (an old or hand-edited save). */
    public static ArmedMode byId(String id) {
        if (id == null || id.isEmpty()) return NONE;
        String s = id.toLowerCase(Locale.ROOT);
        for (ArmedMode m : values()) if (m.id.equals(s)) return m;
        return NONE;
    }
}
