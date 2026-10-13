package com.newuniverse.nusmp.relics;

import net.minecraft.ChatFormatting;

import java.util.Locale;

/**
 * The Tensura equipment grades (docs: Tensura rarity system), lowest to highest. Entries are only appended. The ordinal is the order;
 * {@link #tier()} counts from 1 so it can be multiplied.
 * <p>
 * The grade decides how a relic looks (name colour, glint from LEGEND, aura from GOD), how much it lifts the wearer's existence points
 * ({@link #epBonus()}), how often it turns up in loot ({@link #weight()}) and whether it may carry a Djinn ({@link #djinnCapable()}).
 */
public enum Grade {
    NORMAL("Normal", "GRAY", 0xAAAAAA, 0.0, 40.0),
    SPECIAL("Special", "GREEN", 0x55FF55, 0.0, 28.0),
    RARE("Rare", "AQUA", 0x55FFFF, 0.005, 16.0),
    UNIQUE("Unique", "LIGHT_PURPLE", 0xFF77FF, 0.01, 8.0),
    LEGEND("Legend", "GOLD", 0xFFAA00, 0.025, 4.0),
    GOD("God", "RED", 0xFF5555, 0.06, 1.5),
    GENESIS("Genesis", "WHITE", 0xFFFFFF, 0.12, 0.5);

    private final String shortName, chat;
    private final int rgb;
    private final double ep, weight;

    Grade(String shortName, String chat, int rgb, double ep, double weight) {
        this.shortName = shortName;
        this.chat = chat;
        this.rgb = rgb;
        this.ep = ep;
        this.weight = weight;
    }

    /** 1 (NORMAL) to 7 (GENESIS). */
    public int tier() { return ordinal() + 1; }

    /** "Rare Grade". */
    public String label() { return shortName + " Grade"; }

    /** "Rare". */
    public String shortName() { return shortName; }

    /** The chat colour of the name and the grade line. */
    public ChatFormatting colour() { return ChatFormatting.valueOf(chat); }

    /** The same colour as 0xRRGGBB, for custom drawing and particles. */
    public int rgb() { return rgb; }

    public boolean atLeast(Grade g) { return ordinal() >= g.ordinal(); }

    /** Only LEGEND and above can carry a Djinn (they are the Metal Vessels). */
    public boolean djinnCapable() { return atLeast(LEGEND); }

    /** Share of the wearer's base EP (max magicules and max aura) a worn relic of this grade adds. */
    public double epBonus() { return ep; }

    /** Relative chance in the loot tables (higher is commoner). */
    public double weight() { return weight; }

    /** The filled and empty stars of the tooltip, seven in all. */
    public String stars() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 7; i++) b.append(i < tier() ? '★' : '☆');
        return b.toString();
    }

    /**
     * The grade a card's printed rarity becomes: common NORMAL, uncommon SPECIAL, rare RARE, very rare UNIQUE, epic and legendary LEGEND;
     * anything else RARE. (GOD and GENESIS are never printed: the lead sets them by hand.)
     */
    public static Grade ofCard(String printedRarity) {
        if (printedRarity == null) return RARE;
        String s = printedRarity.trim().toLowerCase(Locale.ROOT).replace('_', ' ').replace('-', ' ');
        return switch (s) {
            case "common" -> NORMAL;
            case "uncommon" -> SPECIAL;
            case "rare" -> RARE;
            case "very rare" -> UNIQUE;
            case "epic", "legendary" -> LEGEND;
            default -> RARE;
        };
    }

    /** A grade by its name, any case ("legend", "Legend Grade"); null if there is none. */
    public static Grade byName(String name) {
        if (name == null) return null;
        String s = name.trim().toLowerCase(Locale.ROOT);
        if (s.endsWith(" grade")) s = s.substring(0, s.length() - 6).trim();
        for (Grade g : values()) if (g.shortName.toLowerCase(Locale.ROOT).equals(s) || g.name().toLowerCase(Locale.ROOT).equals(s)) return g;
        return null;
    }

    /** The bigger of two grades (null counts as lower). */
    public static Grade max(Grade a, Grade b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.ordinal() >= b.ordinal() ? a : b;
    }
}
