package com.newuniverse.nusmp.skilltree.ft.sky;

/**
 * Why: the Sky Dragon Slayer route is named in three places that must never disagree (its JSON file, the ManasCore skill ids that the file points at, and the passives that look a
 * node up by id), and a typo in any of them would make a technique silently uncastable, so every id and every colour of the route is spelled once, here, as a constant that the
 * skills, the passives, the self-test and the unit test all read. It is a plain holder with no logic and no Minecraft type, so it can be used from the pure tests and from the
 * game classes alike. The colours are the route's look: a pale sky blue for the wind, a soft mint for healing and a white-blue for the drill, chosen so that the wind effects of this
 * route read as air and light and not as the green wind of the Black Clover grimoire.
 */
public final class SkyRoute {
    private SkyRoute() {}

    /** The route id: the JSON file {@code data/nusmp/skill_trees/sky_dragon_slayer.json}. */
    public static final String ID = "nusmp:sky_dragon_slayer";

    /** The class id the route sets (the {@code class_id} of the file). */
    public static final String CLASS_ID = "sky_dragon_slayer";

    public static final String ROAR = "roar", WING_ATTACK = "wing_attack", WAVE_WIND = "wave_wind", BREATH = "breath", TROIA = "troia",
            ENCHANT = "enchant", CUSHION = "cushion", AURA = "aura", SKY_DRILL = "sky_drill";

    /** The ManasCore skill ids (registry path under {@code nusmp}); the route file names the same strings with the namespace. */
    public static final String SKILL_ROAR = "ft_sky_dragons_roar", SKILL_WING_ATTACK = "ft_sky_dragons_wing_attack", SKILL_WAVE_WIND = "ft_sky_dragons_wave_wind",
            SKILL_BREATH = "ft_sky_dragons_breath", SKILL_TROIA = "ft_sky_troia", SKILL_ENCHANT = "ft_sky_arms_vernier", SKILL_DRILL = "ft_sky_drill";

    /** The passive ids the route file lists under {@code passives}. */
    public static final String PASSIVE_CUSHION = "sky_cushion", PASSIVE_AURA = "sky_aura";

    /** Effect colours, 0xAARRGGBB (for {@code VfxSpawn}). */
    public static final int WIND = 0xFFBFE8FF, SKY = 0xFF6EC3F7, MINT = 0xFF9CFFD6, WHITE = 0xFFEAF8FF;

    /** Telegraph colour, 0xRRGGBB (for {@code Tells}). */
    public static final int TELL = 0x9ADBFF;
}
