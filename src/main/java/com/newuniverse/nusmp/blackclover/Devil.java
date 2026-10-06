package com.newuniverse.nusmp.blackclover;

/** Devils that can inhabit a five-leaf grimoire. */
public enum Devil {
    LIEBE("Liebe", "Anti-Magic: magic and spell damage can't touch you."),
    ZAGRED("Zagred", "Word Soul: enemies around you are commanded to kneel."),
    MEGICULA("Megicula", "Curse: your foes wither, and the curse spreads."),
    LUCIFERO("Lucifero", "Gravity: everything around you is crushed down."),
    BEELZEBUB("Beelzebub", "Spatial: your attacks tear through space for extra damage."),
    ASTAROTH("Astaroth", "Time: enemies slow to a crawl while you accelerate."),
    LILITH("Lilith", "Ice: a freezing aura locks enemies in place."),
    NAHAMAH("Nahamah", "Fire: a burning aura scorches everything nearby."),
    WALGNER("Walgner", "Blood: your attacks drain life from your foes.");

    public final String displayName;
    public final String power;

    Devil(String displayName, String power) {
        this.displayName = displayName;
        this.power = power;
    }

    public static Devil byName(String name) {
        try { return valueOf(name); } catch (Exception e) { return null; }
    }
}
