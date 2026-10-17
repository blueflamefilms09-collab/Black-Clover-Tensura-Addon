package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;

import java.util.Collection;

/**
 * Why: a theme's encounter table is a data file, and a data pack may remove one (or a new theme may be added to the enum before anyone writes its table). The
 * old answer to a missing file was {@code default.json}, a generic dungeon crowd. Since 1.55.0 the two roads have a character of their own, so the fallback
 * should keep it: a sector on the Holy road whose theme has no table borrows {@code radiant.json} (constructs, choirs and the Rukh-bright air), one on the
 * Abyssal road borrows {@code abyss.json} (the dark organic ecosystems), and only a sector with no polarity, or a road whose own table is gone as well,
 * falls back to {@code default.json}. The theme's own file always wins: a Holy sector that drew the Storm theme meets storm monsters. The rule is a pure
 * function of the names of the tables that exist, so a test can pin every combination without the game.
 */
public final class TableChoice {
    private TableChoice() {}

    public static final String DEFAULT = "default", HOLY = "radiant", ABYSSAL = "abyss";

    /**
     * The name of the table a sector uses, or null when none of the candidates exists (the Director then rolls nothing). The candidates in order: the
     * theme's own table, the road's table (HOLY: radiant, ABYSSAL: abyss), the default.
     */
    public static String pick(Collection<String> available, String themeId, Polarity polarity) {
        if (available == null || available.isEmpty()) return null;
        if (themeId != null && available.contains(themeId)) return themeId;
        if (polarity == Polarity.HOLY && available.contains(HOLY)) return HOLY;
        if (polarity == Polarity.ABYSSAL && available.contains(ABYSSAL)) return ABYSSAL;
        return available.contains(DEFAULT) ? DEFAULT : null;
    }
}
