package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.arc.sds.kit.FourSinsStory;

/**
 * Why: the character record of Merlin lives in a PURE class, apart from the boss that wears it, so a unit test can check the record (tier, floors, Charter
 * skills against the real Tensura id list, prerequisites) without loading an entity class, which needs the game. {@link MerlinBoss#CHARACTER} is this record.
 * Nothing here registers anything: the lead lists the character in {@code ArcCharacters.bootstrap()}.
 * <p>
 * Merlin, the Boar's Sin of Gluttony, the greatest mage of Britannia (The Seven Deadly Sins). Tier 3: 2500 health, a caster who keeps away, so a light body
 * (armor 9) and a modest blow (attack 16, used only by the plain melee swat). Her floors are the deep middle and the end (50 to 90, either road); she favours the
 * themes of stars, folded space and forbidden books. The Charter Grimoire lends skills that express Infinity, Absolute Cancel, the Perfect Cube and
 * her teleports (no Unique or Ultimate skill: the Sin skill "Gluttony" is an Ultimate and is deliberately not lent).
 */
public final class MerlinSheet {
    private MerlinSheet() {}

    /** Violet of her spells and the gold of Aldan, 0xRRGGBB. */
    public static final int RGB = 0x7A5CFF, ACCENT = 0xFFD86B;
    public static final double HEALTH = 2500, ATTACK = 16, SPEED = 0.33, ARMOR = 9, TOUGHNESS = 6;
    public static final float WIDTH = 0.62f, HEIGHT = 1.85f;

    public static final ArcCharacter CHARACTER = ArcCharacter.boss("merlin", "Merlin")
            .title("the Boar's Sin of Gluttony")
            .source("The Seven Deadly Sins")
            .saga(ArcSaga.SEVEN_DEADLY_SINS)
            .tier(3)
            .colors(RGB, ACCENT)
            .magic("spatial")
            .stats(HEALTH, ATTACK, SPEED, ARMOR)
            .floors(null, 50, 90)
            .themes("astral", "spatial", "cursed_archive", "crystal_geode")
            .after(FourSinsStory.prerequisites())
            .charter(0.30, "magic_jamming", "chant_annulment", "mana_manipulation", "thought_acceleration", "warp_portal", "teleport",
                    "multilayer_barrier", "magic_sense")
            .lines("Another party that came all this way. Show me everything you know, and I will show you what I have learned.",
                    "Infinity, unbound. Nothing I cast from here on will end.",
                    "Erased... no. Beaten. I will remember how you did it.")
            .build();
}
