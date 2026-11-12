package com.newuniverse.nusmp.devil;

/**
 * Why: the forbidden magic a devil lends (3.48.0). In this release it is data: stored with the contract, shown on the HUD and in /devil status, and chosen from the magic of the grimoire the contract was
 * made on ({@link #forMagic}). Since 3.76.0 every attribute has a combat effect while the devil is let in ({@link DevilRules#effect}). Stored BY NAME, appended only.
 */
public enum DevilAttribute {
    NONE, GRAVITY, SPATIAL, CURSE, ANTI_MAGIC, FLAME, WORD, TIME,
    /** 3.76.0, the devils of the other worlds (appended). */
    BEAST, FORESIGHT, GHOST, SOUL, ROT, EARTH, WATER, LIGHT, ETHER, CAUSALITY;

    public static DevilAttribute byName(String s) {
        if (s != null) for (DevilAttribute a : values()) if (a.name().equals(s)) return a;
        return NONE;
    }

    public static DevilAttribute byOrdinal(int i) { return i > 0 && i < values().length ? values()[i] : NONE; }

    public String key() { return name().toLowerCase(java.util.Locale.ROOT); }

    /**
     * The attribute for the name of a {@code MagicType} (a string, so this class stays free of the grimoire code and can be tested alone). Gravity, earth and sand are Gravemaw's, space is spatial,
     * time and sealing are Kairovore's, the flame family is Ignivar's, the dark family is a curse, anything else is the word devil's (Verbael).
     */
    public static DevilAttribute forMagic(String magic) {
        if (magic == null) return WORD;
        return switch (magic) {
            case "GRAVITY", "EARTH", "SAND" -> GRAVITY;
            case "SPATIAL", "MIRROR" -> SPATIAL;
            case "TIME", "SEALING" -> TIME;
            case "FLAME", "EXPLOSION", "MAGMA" -> FLAME;
            case "DARK", "SHADOW", "POISON", "BLOOD", "BONE" -> CURSE;
            case "ANTI_MAGIC", "ANTIMAGIC" -> ANTI_MAGIC;
            default -> WORD;
        };
    }
}
