package com.newuniverse.nusmp.vfx.client;

import net.minecraft.resources.ResourceLocation;

/** VFX textures (assets/nusmp/textures/particle). All are white so vertex color tints them. */
public final class VfxTextures {
    private VfxTextures() {}

    private static ResourceLocation t(String n) { return ResourceLocation.fromNamespaceAndPath("nusmp", "textures/particle/" + n + ".png"); }

    public static final ResourceLocation MAGIC_CIRCLE = t("magic_circle");
    public static final ResourceLocation RUNE_RING = t("rune_ring");
    public static final ResourceLocation GLOW = t("glow");
    public static final ResourceLocation MAGIC_EXPLOSION = t("magic_explosion");
    public static final ResourceLocation FLAME = t("flame_trail");
    public static final ResourceLocation WIND_SLASH = t("wind_slash");
    public static final ResourceLocation ANTI_MAGIC_SLASH = t("anti_magic_slash");
    public static final ResourceLocation ELF_CIRCLE = t("elf_circle");
    public static final ResourceLocation DEVIL_CIRCLE = t("devil_circle");
    public static final ResourceLocation WATER_SPLASH = t("water_splash");
    public static final ResourceLocation SPARK = t("spark");
    public static final ResourceLocation SHARD = t("shard");
    public static final ResourceLocation SPIRIT_SPIRAL = t("spirit_spiral");
    public static final ResourceLocation CARD_FRAME = t("card_frame");
    public static final ResourceLocation CONSTRUCT_SWORD = t("construct_sword");
    public static final ResourceLocation CONSTRUCT_AXE = t("construct_axe");
    public static final ResourceLocation CONSTRUCT_SHIELD = t("construct_shield");
    public static final ResourceLocation MANA_MOTE = t("mana_mote");

    /** Lookup by short name (used by vfx/effects.json). */
    public static ResourceLocation byName(String name) { return t(name); }
}
