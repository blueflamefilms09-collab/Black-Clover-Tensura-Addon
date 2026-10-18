package com.newuniverse.nusmp.heroweapon.gem;

import com.newuniverse.nusmp.heroweapon.HeroKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: the acquisition system adds nineteen items (the Holy Calling Gem, the Gem Fragment and one Vassal Seal for each of the seventeen weapons that are not
 * Holy), and the item register, the creative tab, the models, the icons, the recipes, the lang file and the tests must all agree on their ids, names and
 * colours. This pure class (no Minecraft types, so JUnit can run it) is that one agreement. The Seals are derived from the {@link HeroKind} enum, never
 * listed by hand, so a weapon appended to the enum later gets its Seal item automatically (and the asset test names the model, icon and recipe it still
 * lacks). The gem colours are the canon jewel colours of the four Holy Weapons as the fandom wiki's pages for the Legendary Shield, Sword, Spear and Bow
 * give them (search summaries, web novel pages): green, blue, red and yellow; the Seven Star and the other Vassal Weapons have no canon colour, so their
 * seals take an amber and an aqua tint by design. {@code tools/heroweapon/gen_gem_icons.py} repeats the same RGB values for the icons.
 */
public final class GemCatalog {
    private GemCatalog() {}

    /** Registry path of the Holy Calling Gem. */
    public static final String HOLY_ID = "calling_gem";
    /** Registry path of the Gem Fragment, the cheap shard every Seal is made from. */
    public static final String FRAGMENT_ID = "gem_fragment";
    /** Every Seal id starts with this and ends with the lower-case enum name of its weapon, e.g. {@code vassal_seal_sewing_set}. */
    public static final String SEAL_PREFIX = "vassal_seal_";

    /** The Legendary Shield's jewel (canon: green), 0xRRGGBB. */
    public static final int SHIELD_GREEN = 0x38C86A;
    /** The Legendary Sword's jewel (canon: blue). */
    public static final int SWORD_BLUE = 0x3F7FE8;
    /** The Legendary Spear's jewel (canon: red). */
    public static final int SPEAR_RED = 0xE23B3B;
    /** The Legendary Bow's jewel (canon: yellow). */
    public static final int BOW_YELLOW = 0xF2C62B;
    /** The neutral light of a Holy Calling Gem before a weapon has answered. */
    public static final int HOLY_WHITE = 0xFFE9B0;
    /** The tint of the Seals of the seven Seven Star Weapons (a design choice, no canon colour). */
    public static final int SEVEN_STAR_AMBER = 0xE8A838;
    /** The tint of the Seals of the ten other Vassal Weapons (a design choice, no canon colour). */
    public static final int VASSAL_AQUA = 0x6CC8D8;

    /** The four Holy jewels in the order of the Holy Weapons in the enum: Shield, Sword, Spear, Bow. The Holy Gem pulses through them while it is held. */
    public static final int[] FOUR_COLOURS = {SHIELD_GREEN, SWORD_BLUE, SPEAR_RED, BOW_YELLOW};

    /** True when the weapon has a Vassal Seal (all of them except the four Holy Weapons, which only the Holy Calling Gem can choose for you). */
    public static boolean hasSeal(HeroKind kind) { return kind != null && !kind.isHoly(); }

    /** The registry path of the Seal of a weapon, e.g. {@code vassal_seal_scythe}. */
    public static String sealId(HeroKind kind) { return SEAL_PREFIX + kind.name().toLowerCase(Locale.ROOT); }

    /** The weapons that have a Seal, in catalogue order (seventeen). */
    public static List<HeroKind> sealKinds() {
        List<HeroKind> out = new ArrayList<>();
        for (HeroKind k : HeroKind.values()) if (hasSeal(k)) out.add(k);
        return out;
    }

    /** All nineteen item ids in creative-tab order: the Holy Calling Gem, the fragment, then the Seals in catalogue order. */
    public static List<String> allItemIds() {
        List<String> out = new ArrayList<>();
        out.add(HOLY_ID);
        out.add(FRAGMENT_ID);
        for (HeroKind k : sealKinds()) out.add(sealId(k));
        return out;
    }

    /** The weapon without the family word in front: {@code Scythe} for the Vassal Scythe, {@code Musical Instrument} for the Vassal Musical Instrument. */
    public static String shortName(HeroKind kind) {
        String d = kind.display();
        return d.startsWith("Vassal ") ? d.substring("Vassal ".length()) : d;
    }

    /** The English item name of a Seal: {@code Vassal Seal: Scythe}. */
    public static String sealName(HeroKind kind) { return "Vassal Seal: " + shortName(kind); }

    /** The light of a weapon, 0xRRGGBB: the canon jewel colour of a Holy Weapon, an amber for a Seven Star Weapon, an aqua for a Vassal Weapon. */
    public static int colour(HeroKind kind) {
        return switch (kind.heroClass()) {
            case HOLY -> switch (kind) {
                case SHIELD -> SHIELD_GREEN;
                case SWORD -> SWORD_BLUE;
                case SPEAR -> SPEAR_RED;
                case BOW -> BOW_YELLOW;
                default -> HOLY_WHITE;
            };
            case SEVEN_STAR -> SEVEN_STAR_AMBER;
            case VASSAL -> VASSAL_AQUA;
        };
    }

    /** The same light as an opaque ARGB value for the effect payloads. */
    public static int argb(int rgb) { return 0xFF000000 | (rgb & 0xFFFFFF); }
}
