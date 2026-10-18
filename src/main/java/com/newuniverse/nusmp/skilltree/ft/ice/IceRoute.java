package com.newuniverse.nusmp.skilltree.ft.ice;

/**
 * Why: the Ice-Make route is named in three places that must never disagree (its JSON file, the ManasCore skill ids the file points at, and the passive that looks a node up by id), and a
 * typo in any of them would make a technique silently uncastable, so every id and every colour of the route is spelled once, here, as a constant that the skills, the passive, the self-test
 * and the unit test all read. It is a plain holder with no logic and no Minecraft type, so the pure tests can use it as well as the game classes. The colours are the route's look: a pale
 * cyan-white for clear ice (the same family as the mod's Ice Magic effects, so the route reads as ice at once), a deeper blue for the shadows of a construct and a frost white for flashes.
 */
public final class IceRoute {
    private IceRoute() {}

    /** The route id: the JSON file {@code data/nusmp/skill_trees/ice_make.json}. */
    public static final String ID = "nusmp:ice_make";

    /** The class id the route sets (the {@code class_id} of the file). */
    public static final String CLASS_ID = "ice_make";

    public static final String LANCE = "lance", FLOOR = "floor", SHIELD = "shield", CANNON = "cannon", FROST_BODY = "frost_body",
            SAUCER = "saucer", EAGLE = "eagle", HAMMER = "hammer", EXCALIBUR = "excalibur";

    /** The ManasCore skill ids (registry path under {@code nusmp}); the route file names the same strings with the namespace. */
    public static final String SKILL_LANCE = "ft_ice_make_lance", SKILL_FLOOR = "ft_ice_make_floor", SKILL_SHIELD = "ft_ice_make_shield", SKILL_CANNON = "ft_ice_make_cannon",
            SKILL_SAUCER = "ft_ice_make_saucer", SKILL_EAGLE = "ft_ice_make_eagle", SKILL_HAMMER = "ft_ice_make_hammer", SKILL_EXCALIBUR = "ft_ice_make_excalibur";

    /** The passive id the route file lists under {@code passives}. */
    public static final String PASSIVE_FROST_BODY = "ice_frost_body";

    /** Effect colours, 0xAARRGGBB (for {@code VfxSpawn}). */
    public static final int ICE = 0xFFBDEFFF, DEEP = 0xFF6FC8F0, WHITE = 0xFFEAFBFF;

    /** Telegraph colour, 0xRRGGBB (for {@code Tells}). */
    public static final int TELL = 0xA8E6FF;
}
