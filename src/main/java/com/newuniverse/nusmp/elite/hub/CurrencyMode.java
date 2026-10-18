package com.newuniverse.nusmp.elite.hub;

import java.util.Locale;

/**
 * Why: the owner replaced the generic Magic Stone economy with Tensura's coins but the old behaviour must stay available (rule 2: add the new, keep the old), so ONE config value
 * ({@code economy.currency}) chooses what the Mega-Dungeon pays and what merchants charge. This enum is that value without any game type, so the parse and the defaults are unit tested and a typo in
 * a hand-edited config can never crash a server: an unknown word is the default, {@link #TENSURA_COINS}.
 */
public enum CurrencyMode {
    /** The default: mobs, chests and bosses pay Tensura coins, shops and quests pay and charge coins. */
    TENSURA_COINS("tensura_coins"),
    /** The old behaviour: Magic Stones and crystals drop; the Hub's trades are charged in Magic Stones at {@code economy.stoneValue} bronze each. */
    MAGIC_STONES("magic_stones");

    private final String key;

    CurrencyMode(String key) { this.key = key; }

    /** The config word. */
    public String key() { return key; }

    /** The mode a config word names (case-insensitive, spaces and dashes ignored), or {@link #TENSURA_COINS} for null and anything unknown. */
    public static CurrencyMode parse(String s) {
        if (s == null) return TENSURA_COINS;
        String t = s.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (CurrencyMode m : values()) if (m.key.equals(t)) return m;
        return TENSURA_COINS;
    }
}
