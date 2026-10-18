package com.newuniverse.nusmp.heroweapon;

import java.util.Locale;

/**
 * Why: the Curse Series of the weapons are tied to the seven deadly sins (the owner's text). The curse engine, the forms data and the weapon builders all name
 * a series by its sin, so the seven sins are one enum with a stable id (the JSON name), a label and the colour the curse look uses. Canon (the fandom wiki, search
 * summaries only, unverified in full): the Shield's series is Wrath (called Rage), the Sword's Greed and Gluttony, the Spear's Lust, the Bow's Pride; Envy and
 * Sloth are not tied to a Holy Weapon in what could be read, and the vassal weapons can use cursed forms too, so those two are given to vassal weapons by design.
 */
public enum Sin {
    WRATH("Wrath", 0xC2181B),
    PRIDE("Pride", 0x7A3FB5),
    LUST("Lust", 0xD94A8C),
    ENVY("Envy", 0x2E9F5B),
    GREED("Greed", 0xD9A520),
    SLOTH("Sloth", 0x6A7B8C),
    GLUTTONY("Gluttony", 0x8B4A1F);

    private final String label;
    private final int rgb;

    Sin(String label, int rgb) {
        this.label = label;
        this.rgb = rgb;
    }

    /** The id used in JSON and commands: the lower-case name. */
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public String label() { return label; }

    /** 0xRRGGBB of the curse look and HUD of this sin. */
    public int rgb() { return rgb; }

    /** The sin with this id (case-insensitive), or null. */
    public static Sin byId(String id) {
        if (id == null) return null;
        for (Sin s : values()) if (s.id().equalsIgnoreCase(id)) return s;
        return null;
    }
}
