package com.newuniverse.nusmp.elite.hub;

import java.util.Locale;

/**
 * Why: the brief names four things a quest can ask (kill N mobs of a tier, road or faction; clear K floors; deliver items; reach floor F) and they differ in exactly the ways the state machine has to
 * care about: how the quest is FED (a kill event, a floor cleared, a floor entered, nothing at all for a delivery, which is counted from the inventory when the player hands it in) and when it can
 * be handed in. The one place that says what each means, read by the rules, the parser, the labels and the HUD. Saved by name.
 */
public enum ObjectiveKind {
    /** Defeat N monsters that match the filters (tier, road, faction, foundry or boss). Fed by kills. */
    KILL(true),
    /** Clear K floors (move on from a floor to a deeper one of the same road) that match the tier and road filters. Fed by floor-cleared events. */
    CLEAR_FLOORS(true),
    /** Hand in N of an item. Not fed by events: counted from the inventory at the Quest Giver. */
    DELIVER(false),
    /** Reach floor F (signed: Holy positive, Abyssal negative) or deeper along that road. Fed by floor-entered events. */
    REACH_FLOOR(true);

    private final boolean eventFed;

    ObjectiveKind(boolean eventFed) { this.eventFed = eventFed; }

    /** True when game events move the progress (false for a delivery). */
    public boolean eventFed() { return eventFed; }

    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** The kind with this name (case-insensitive, dashes and spaces read as underscores), or null. */
    public static ObjectiveKind fromId(String s) {
        if (s == null) return null;
        String t = s.trim().replace('-', '_').replace(' ', '_');
        for (ObjectiveKind k : values()) if (k.name().equalsIgnoreCase(t)) return k;
        return null;
    }
}
