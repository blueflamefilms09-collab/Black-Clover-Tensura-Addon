package com.newuniverse.nusmp.grimoire;

import com.newuniverse.nusmp.blackclover.GrimoireCover;
import com.newuniverse.nusmp.blackclover.MagicType;

import java.util.List;

/** Looks that no regular grimoire has, shown in the creative tab so every crest, thickness and clasp can be seen without a command. */
public final class GrimoireShowcase {
    public record Entry(GrimoireAppearance look, MagicType magic) {}

    private GrimoireShowcase() {}

    private static GrimoireAppearance look(GrimoireCover cover, MagicType magic, InsigniaType insignia, CoverMaterial material,
                                           TrimMetal metal, Thickness thickness, ClaspType clasp) {
        GrimoireAppearance base = GrimoireAppearance.fromCover(cover, magic);
        return new GrimoireAppearance(insignia.kingdom != null ? insignia.kingdom : base.kingdom(), insignia, insignia.count, material, metal,
                base.primaryColor(), metal.color, base.auraColor(), thickness, clasp);
    }

    public static List<Entry> entries() {
        return List.of(
                // the four crests that exist only as looks
                new Entry(look(GrimoireCover.TWO_HEART, MagicType.DREAM, InsigniaType.TIERED_HEART, CoverMaterial.LEATHER_STITCHED, TrimMetal.GOLD, Thickness.STANDARD, ClaspType.SINGLE_BUCKLE), MagicType.DREAM),
                new Entry(look(GrimoireCover.FIVE_SIDED, MagicType.MIRROR, InsigniaType.DUAL_DIAMOND, CoverMaterial.METALLIC_TRIM, TrimMetal.SILVER, Thickness.TOME, ClaspType.SINGLE_BUCKLE), MagicType.MIRROR),
                new Entry(look(GrimoireCover.FOUR_LEAF, MagicType.TIME, InsigniaType.BOUNDLESS, CoverMaterial.PLAIN, TrimMetal.SILVER, Thickness.TOME, ClaspType.OPEN), MagicType.TIME),
                new Entry(look(GrimoireCover.TRIPLE_SPADE, MagicType.DARK, InsigniaType.FORBIDDEN_RUNES, CoverMaterial.DRAGON_HIDE, TrimMetal.RUNED, Thickness.TOME, ClaspType.DUAL_CHAINS), MagicType.DARK),
                // thickness: the single loose page
                new Entry(look(GrimoireCover.SPADE, MagicType.SWORD, InsigniaType.SPADE, CoverMaterial.LEATHER_STITCHED, TrimMetal.IRON, Thickness.SINGLE_PAGE, ClaspType.OPEN), MagicType.SWORD),
                // clasps
                new Entry(look(GrimoireCover.THREE_LEAF, MagicType.WIND, InsigniaType.THREE_LEAF, CoverMaterial.PLAIN, TrimMetal.BRONZE, Thickness.STANDARD, ClaspType.SINGLE_BUCKLE), MagicType.WIND),
                new Entry(look(GrimoireCover.THREE_LEAF, MagicType.WATER, InsigniaType.THREE_LEAF, CoverMaterial.LEATHER_STITCHED, TrimMetal.SILVER, Thickness.TOME, ClaspType.DUAL_CHAINS), MagicType.WATER),
                new Entry(look(GrimoireCover.CRACKED_HEART, MagicType.BLOOD, InsigniaType.CRACKED_HEART, CoverMaterial.TATTERED, TrimMetal.OBSIDIAN, Thickness.THIN, ClaspType.LOCK_AND_KEY), MagicType.BLOOD));
    }
}
