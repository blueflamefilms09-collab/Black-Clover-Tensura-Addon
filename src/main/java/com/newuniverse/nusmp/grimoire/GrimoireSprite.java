package com.newuniverse.nusmp.grimoire;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.resources.ResourceLocation;

/** Every base texture of the grimoire system: 33 files in assets/nusmp/textures/item/grimoire3d (made by tools/gen_grimoire_textures.py). */
public enum GrimoireSprite {
    COVER_PLAIN("cover_plain"), COVER_STITCHED("cover_stitched"), COVER_DRAGONHIDE("cover_dragonhide"),
    COVER_TATTERED("cover_tattered"), COVER_METALLIC("cover_metallic"),
    TRIM_PLAIN("trim_plain"), TRIM_HEAVY("trim_heavy"), TRIM_RUNED("trim_runed"), TRIM_BORDER("trim_border"),
    CLASP_STRAP("clasp_strap"), CLASP_BUCKLE("clasp_buckle"), CLASP_CHAIN("clasp_chain"), CLASP_LOCK("clasp_lock"),
    PAGES("pages"),
    INSIGNIA_THREE_LEAF("insignia_three_leaf"), INSIGNIA_FOUR_LEAF("insignia_four_leaf"), INSIGNIA_FIVE_LEAF("insignia_five_leaf"),
    INSIGNIA_HEART("insignia_heart"), INSIGNIA_TWO_HEART("insignia_two_heart"), INSIGNIA_TIERED_HEART("insignia_tiered_heart"),
    INSIGNIA_CRACKED_HEART("insignia_cracked_heart"),
    INSIGNIA_SPADE("insignia_spade"), INSIGNIA_DOUBLE_SPADE("insignia_double_spade"), INSIGNIA_TRIPLE_SPADE("insignia_triple_spade"),
    INSIGNIA_DIAMOND("insignia_diamond"), INSIGNIA_DUAL_DIAMOND("insignia_dual_diamond"), INSIGNIA_FIVE_SIDED("insignia_five_sided"),
    INSIGNIA_CRACKED_DIAMOND("insignia_cracked_diamond"),
    INSIGNIA_BOUNDLESS("insignia_boundless"), INSIGNIA_FORBIDDEN_RUNES("insignia_forbidden_runes"),
    AURA_HALO("aura_halo"), AURA_SIGIL("aura_sigil"), AURA_SPARKS("aura_sparks");

    public final String file;

    GrimoireSprite(String file) { this.file = file; }

    /** Atlas location, e.g. nusmp:item/grimoire3d/cover_plain */
    public ResourceLocation texture() { return ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "item/grimoire3d/" + file); }

    private static final GrimoireSprite[] INSIGNIA = new GrimoireSprite[InsigniaType.values().length];
    static {
        for (InsigniaType t : InsigniaType.values()) INSIGNIA[t.ordinal()] = valueOf("INSIGNIA_" + t.name());
    }

    public static GrimoireSprite insignia(InsigniaType t) { return INSIGNIA[t.ordinal()]; }
}
