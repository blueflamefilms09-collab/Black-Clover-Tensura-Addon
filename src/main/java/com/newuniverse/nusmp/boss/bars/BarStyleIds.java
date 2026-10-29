package com.newuniverse.nusmp.boss.bars;

import java.util.List;

/**
 * Why: the ids of the custom boss bar styles, in one pure list the server can use without reading a single asset (a dedicated server has no
 * {@code assets/}): the resolver answers with one of these and the client loads the matching {@code assets/nusmp/boss_bars/<id>.json}. A JUnit test
 * checks the list against the generated {@code _index.json}, so a style cannot exist in only one of the two places. Ids are APPENDED only (a server
 * and a client of different versions may meet).
 */
public final class BarStyleIds {
    private BarStyleIds() {}

    public static final String HOLY = "holy", EVIL = "evil", UNDEAD = "undead", FIRE = "fire", ICE = "ice", STORM = "storm", VOID = "void", FOREST = "forest", WATER = "water",
            BLOOD = "blood", CLOCKWORK = "clockwork", MIRROR = "mirror", UNDERWORLD = "underworld", QLIPHOTH_GATE = "qliphoth_gate", DEVIL = "devil", SUPREME_DEVIL = "supreme_devil",
            DRAGON = "dragon", ANGEL = "angel", ARCHON = "archon", LICH = "lich", FALLEN_KING = "fallen_king", POCKET_TOMB = "pocket_tomb", PLAGUE = "plague", ZOGRATIS = "zogratis",
            MIDNIGHT_SUN = "midnight_sun", SEVEN_SINS = "seven_sins", SPADE_KINGDOM = "spade_kingdom", MAGIC_KNIGHTS = "magic_knights", DANMACHI = "danmachi", LUCIUS = "lucius",
            GOD_OF_ORDER = "god_of_order", GOLIATH = "goliath", CHARYBDIS = "charybdis", WARDEN = "warden", ZAGRED = "zagred";

    /** Every style, in the order of the generator's table ({@code tools/boss_bar_art/styles.py}). */
    public static final List<String> ALL = List.of(HOLY, EVIL, UNDEAD, FIRE, ICE, STORM, VOID, FOREST, WATER, BLOOD, CLOCKWORK, MIRROR, UNDERWORLD, QLIPHOTH_GATE, DEVIL, SUPREME_DEVIL,
            DRAGON, ANGEL, ARCHON, LICH, FALLEN_KING, POCKET_TOMB, PLAGUE, ZOGRATIS, MIDNIGHT_SUN, SEVEN_SINS, SPADE_KINGDOM, MAGIC_KNIGHTS, DANMACHI, LUCIUS, GOD_OF_ORDER, GOLIATH,
            CHARYBDIS, WARDEN, ZAGRED);

    public static boolean known(String id) { return id != null && ALL.contains(id); }
}
