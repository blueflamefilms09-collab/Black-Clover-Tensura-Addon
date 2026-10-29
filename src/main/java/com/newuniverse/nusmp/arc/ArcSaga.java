package com.newuniverse.nusmp.arc;

/**
 * Why: the owner's briefs organise the characters by story arc (the Black Clover sagas, the Seven Deadly Sins, the DanMachi dungeon), and the arcs decide
 * the order of the story unlock, the threat tier and the floors a boss may stand on. A saga is therefore data on a character, not a class hierarchy.
 * Only ever APPEND entries (ids are saved in player progress).
 */
public enum ArcSaga {
    /** Black Clover, Saga 1: Magic Knights Entrance to the Elf Reincarnation (Revchi, Heath Grice, Lotus, Mars, Vetto, Fana, Patolli, Zagred, Rades ...). */
    ELF_REINCARNATION("Elf Reincarnation Saga", 1),
    /** Black Clover, Saga 2: Heart Kingdom to the Spade Kingdom Raid (the Dark Triad, Morris, Lucifero). */
    SPADE_RAID("Spade Kingdom Raid Saga", 2),
    /** Black Clover, Saga 3: the Ultimate Wizard King arc (Lucius Zogratis). */
    WIZARD_KING("Ultimate Wizard King Arc", 3),
    /** The Seven Deadly Sins (Meliodas, Ban, King, Diane, Gowther, Merlin, Escanor). */
    SEVEN_DEADLY_SINS("The Seven Deadly Sins", 4),
    /** DanMachi: the dungeon of Orario and what lives in it. */
    DANMACHI("DanMachi", 5),
    /** Allies and townsfolk (Magic Knights and others) that are not tied to one arc's boss list. */
    ALLIES("Allies", 6),
    /** 3.33.0: the owner's boss tier list (docs/bosses/tierlist.md): characters of many series who stand as floor bosses of the Mega-Dungeon, weak ones first. */
    TIER_LIST("The Tier List", 7);

    private final String title;
    private final int order;

    ArcSaga(String title, int order) {
        this.title = title;
        this.order = order;
    }

    /** The display name of the saga. */
    public String title() { return title; }

    /** Position in the story (1 first); the story unlock walks the sagas in this order. */
    public int order() { return order; }

    /** The lower-case id used in saved data, JSON and lang keys. */
    public String id() { return name().toLowerCase(java.util.Locale.ROOT); }

    /** The saga with this id, or {@link #ALLIES} for null or an unknown id. */
    public static ArcSaga fromId(String id) {
        if (id != null) for (ArcSaga s : values()) if (s.id().equalsIgnoreCase(id)) return s;
        return ALLIES;
    }
}
