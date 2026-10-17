package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcLook;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.arc.sds.kit.FourSinsStory;

/**
 * Why: the character record of King in a PURE class, apart from the boss that wears it, so a unit test can check the record (tier, health band, floors, Charter
 * skills against the real Tensura id list, prerequisites) without loading an entity class ({@link KingBoss#CHARACTER} is this record; nothing here registers anything,
 * the lead lists the character in {@code ArcCharacters.bootstrap()}).
 * <p>
 * King, the Grizzly's Sin of Sloth (The Seven Deadly Sins), the Fairy King Harlequin who guards the forest of the Sacred Tree. Tier 3: 2400 health, a fast and light
 * flier (speed 0.30, armor 7). His floors are the middle of either road (30 to 70), in groves, the sky and the courts of light. The Charter Grimoire lends flight,
 * wind, earth, barrier and spirit-guard skills (no Unique or Ultimate skill: the Sin skill "Sloth" is an Ultimate and is deliberately not lent).
 */
public final class KingSheet {
    private KingSheet() {}

    /** The green of the Sacred Tree's leaves and the gold of its pollen, 0xRRGGBB. */
    public static final int RGB = 0x6BBF4E, ACCENT = 0xFFD76A;
    public static final double HEALTH = 2400, ATTACK = 16, SPEED = 0.30, ARMOR = 7, TOUGHNESS = 5;
    /** The hitbox the art kit recommends for the model (tools/arc/manifest.json: recommended_hitbox_blocks). */
    public static final float WIDTH = 0.5f, HEIGHT = 1.59f;
    /** The numbers of tools/arc/manifest.json (arc_look): his wings and the spear reach past the hitbox, hence the big growth numbers. */
    public static final ArcLook LOOK = new ArcLook("king", 1.59f, 0.99f, 1.73f, 0.67f, 1.45f, 1.51f, true);

    public static final ArcCharacter CHARACTER = ArcCharacter.boss("king", "King")
            .title("the Grizzly's Sin of Sloth")
            .source("The Seven Deadly Sins")
            .saga(ArcSaga.SEVEN_DEADLY_SINS)
            .tier(3)
            .colors(RGB, ACCENT)
            .magic("plant")
            .stats(HEALTH, ATTACK, SPEED, ARMOR)
            .floors(null, 30, 70)
            .themes("feywild_grove", "verdant", "sky_citadel", "celestial_court")
            .after(FourSinsStory.prerequisites())
            .charter(0.30, "air_flight", "float", "gravity_flight", "wind_manipulation", "earth_manipulation", "multilayer_barrier", "spiritual_attack_resistance",
                    "poison_resistance")
            .lines("A party in my forest. Fine. I will protect it, even if I would rather be asleep.",
                    "Enough. I have been holding back, and now the Sacred Tree is watching.",
                    "I... did not want to hurt anyone. Go on. The forest will let you pass.")
            .build();
}
