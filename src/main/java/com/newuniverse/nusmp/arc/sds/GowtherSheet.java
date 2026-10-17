package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcLook;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.arc.sds.kit.FourSinsStory;

/**
 * Why: the character record of Gowther in a PURE class, apart from the boss that wears it, so a unit test can check the record without loading an entity class
 * ({@link GowtherBoss#CHARACTER} is this record; nothing here registers anything, the lead lists the character in {@code ArcCharacters.bootstrap()}).
 * <p>
 * Gowther, the Goat's Sin of Lust, a doll with the power of Invasion (The Seven Deadly Sins). Tier 3: 2300 health, quick and light (speed 0.35, armor 8: he is hit
 * easily and hits from range). Floors 40 to 80 on either road, in the places of mirrors, dolls and dreams. His Charter Grimoire lends the skills of illusion and
 * thought (no Unique or Ultimate skill: the Sin skill "Lust" is an Ultimate and is deliberately not lent, and nothing that takes a mob's mind is lent as a way to
 * take a player's).
 */
public final class GowtherSheet {
    private GowtherSheet() {}

    /** The pink of Invasion and its pale accent, 0xRRGGBB. */
    public static final int RGB = 0xE0559B, ACCENT = 0xFFC4E8;
    public static final double HEALTH = 2300, ATTACK = 15, SPEED = 0.35, ARMOR = 8, TOUGHNESS = 5;
    /** The hitbox the art kit recommends for the model (tools/arc/manifest.json: recommended_hitbox_blocks). */
    public static final float WIDTH = 0.55f, HEIGHT = 1.84f;
    /** The numbers of tools/arc/manifest.json (arc_look); the bigger of the two forms for the culling box and the shadow (the wings and halo of the second form). */
    public static final ArcLook LOOK = new ArcLook("gowther", 1.84f, 1.11f, 1.97f, 0.69f, 1.49f, 1.2f, true);

    public static final ArcCharacter CHARACTER = ArcCharacter.boss("gowther", "Gowther")
            .title("the Goat's Sin of Lust")
            .source("The Seven Deadly Sins")
            .saga(ArcSaga.SEVEN_DEADLY_SINS)
            .tier(3)
            .colors(RGB, ACCENT)
            .magic("memory")
            .stats(HEALTH, ATTACK, SPEED, ARMOR)
            .floors(null, 40, 80)
            .themes("haunted_manor", "shadow", "feywild_grove", "cursed_archive")
            .after(FourSinsStory.prerequisites())
            .charter(0.30, "mirage", "confusion", "mental_crush", "charm", "thought_communication", "doppelganger", "invisible", "clairvoyance")
            .lines("I wish to understand what you feel when you are afraid. Please, be afraid.",
                    "My heart... is that what this is? Fascinating. Let me show you.",
                    "So this is what losing feels like. I do not know the word for it yet.")
            .build();
}
