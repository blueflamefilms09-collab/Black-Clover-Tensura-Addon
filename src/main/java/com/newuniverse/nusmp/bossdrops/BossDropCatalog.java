package com.newuniverse.nusmp.bossdrops;

import java.util.List;

/**
 * Why: the one pure list of the bosses that have a drop table and a trophy (docs/bosses/boss_drops.md): the 23 grimoire bosses, by their Charter id. Item registration, the self-test and the unit tests all read
 * it, and {@code tools/boss_drops/bosses.json} (the colours the trophy generator tints with) carries the same ids; a test keeps the two lists equal. Append only: a boss added later is a new line here and a
 * new row in that file.
 */
public final class BossDropCatalog {
    private BossDropCatalog() {}

    public static final List<String> BOSSES = List.of("meliodas", "escanor", "ban", "diane", "king", "gowther", "merlin", "fana", "rhya", "rades_spirito", "patolli", "heath_grice",
            "lotus_whomalt", "mars", "revchi_salik", "vetto", "minotaur", "black_goliath", "amphisbaena", "udaeus", "morris_libardirt", "lucifero", "vanica");

    public static final String LUCIFERO = "lucifero";
    public static final List<String> LUCIFERO_PIECES = List.of("lucifero_helmet", "lucifero_chestplate", "lucifero_leggings", "lucifero_boots");
    public static final List<String> PLATE_PIECES = List.of("trophy_plate_helmet", "trophy_plate_chestplate", "trophy_plate_leggings", "trophy_plate_boots");

    /** The trophy item path of a boss. */
    public static String trophy(String boss) { return "trophy_" + boss; }
}
