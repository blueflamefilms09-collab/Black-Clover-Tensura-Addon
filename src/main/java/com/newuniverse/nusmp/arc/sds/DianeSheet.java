package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcLook;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.arc.sds.kit.FourSinsStory;

/**
 * Why: the character record of Diane in a PURE class, apart from the boss that wears it, so a unit test can check the record (tier, health band, floors, Charter skills
 * against the real Tensura id list, prerequisites) without loading an entity class ({@link DianeBoss#CHARACTER} is this record; nothing here registers anything, the lead
 * lists the character in {@code ArcCharacters.bootstrap()}).
 * <p>
 * Diane, the Serpent's Sin of Envy (The Seven Deadly Sins), a giant of the Giant Clan. Tier 3: 2600 health, the slowest and hardest of the four (speed 0.26, armor 11,
 * toughness 7) with a heavy blow (attack 17, used by the plain swat). Her floors are the first of the four Sins' (20 to 60, either road), in mountains, forges and
 * crystal caves. The Charter Grimoire lends skills of strength, stone and size (no Unique or Ultimate skill: the Sin skill "Envy" is an Ultimate and is deliberately not
 * lent).
 */
public final class DianeSheet {
    private DianeSheet() {}

    /** The orange of her suit and the pale gold of her hammer's glow, 0xRRGGBB. */
    public static final int RGB = 0xE8832A, ACCENT = 0xFFD27A;
    public static final double HEALTH = 2600, ATTACK = 17, SPEED = 0.26, ARMOR = 11, TOUGHNESS = 7;
    /** The human-size hitbox the art kit recommends (tools/arc/manifest.json: recommended_hitbox_blocks); a giant is this times {@link DianeTable#SCALE_GIANT}. */
    public static final float WIDTH = 0.7f, HEIGHT = 2.0f;
    /**
     * The numbers of tools/arc/manifest.json (arc_look) for the human size; the culling box and the shadow are the giant's (1.95 times), since a boss that grows must not
     * be culled at the edge of the screen or have a human shadow under a giant.
     */
    public static final ArcLook LOOK = new ArcLook("diane", 2.0f, 1.21f, 2.15f, 1.0f, 2.6f, 1.8f, true);

    public static final ArcCharacter CHARACTER = ArcCharacter.boss("diane", "Diane")
            .title("the Serpent's Sin of Envy")
            .source("The Seven Deadly Sins")
            .saga(ArcSaga.SEVEN_DEADLY_SINS)
            .tier(3)
            .colors(RGB, ACCENT)
            .magic("earth")
            .stats(HEALTH, ATTACK, SPEED, ARMOR)
            .floors(null, 20, 60)
            .themes("mountain_monastery", "dwarven_forgehold", "crystal_geode", "verdant")
            .after(FourSinsStory.prerequisites())
            .charter(0.30, "giantification", "strengthen_body", "body_armor", "earth_manipulation", "earth_wall", "earthshatter_kick", "stone_shot", "reinforced_barrier")
            .lines("Hi! You came all this way to fight me? Okay, but I am not going to hold back just because you are small!",
                    "Now I get to be serious! Watch the ground, it is mine!",
                    "Ow... you are really strong. I wanted to dance a bit longer, though.")
            .build();
}
