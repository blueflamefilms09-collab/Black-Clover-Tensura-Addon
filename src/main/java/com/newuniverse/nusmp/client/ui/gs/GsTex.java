package com.newuniverse.nusmp.client.ui.gs;

import net.minecraft.resources.ResourceLocation;

/**
 * Every texture of the Great Spirit look, built once (so no frame ever builds a location from a string). Files:
 * {@code assets/nusmp/textures/gui/gs/<b|c>/...}, written by {@code tools/gen_gs_gui_textures.py}.
 */
public final class GsTex {
    private GsTex() {}

    private static ResourceLocation rl(String skin, String name) {
        return ResourceLocation.fromNamespaceAndPath("nusmp", "textures/gui/gs/" + skin + "/" + name + ".png");
    }

    private static ResourceLocation[] both(String name) { return new ResourceLocation[]{rl("b", name), rl("c", name)}; }

    /** 96 x 96 filigree corner (top-left; the other corners are drawn mirrored), indexed by {@link GsSkin#ordinal()}. */
    public static final ResourceLocation[] CORNER = both("corner");
    /** 96 x 96 alert crest. */
    public static final ResourceLocation[] CREST = both("crest");
    /** 64 x 64 expanding ring of the hover ripple. */
    public static final ResourceLocation[] RIPPLE = both("ripple");
    /** 96 x 96 clover frames [skin][leaves - 3]. */
    public static final ResourceLocation[][] CLOVER = {
            {rl("b", "clover3_frame"), rl("b", "clover4_frame"), rl("b", "clover5_frame")},
            {rl("c", "clover3_frame"), rl("c", "clover4_frame"), rl("c", "clover5_frame")}};
    /** Additive glow of the four- and five-leaf frames [skin][leaves - 4]. */
    public static final ResourceLocation[][] GLOW = {
            {rl("b", "clover4_glow"), rl("b", "clover5_glow")},
            {rl("c", "clover4_glow"), rl("c", "clover5_glow")}};
    /** The drips under the five-leaf frame, per skin (the five-leaf frame is skin C for everybody, but both exist). */
    public static final ResourceLocation[] DRIPS = both("clover5_drips");
    /** Skin B's parchment page, 326 x 344 texels (163 x 172 GUI pixels at 2 texels per pixel). */
    public static final ResourceLocation PAGE_B = rl("b", "page");
    public static final int PAGE_B_W = 326, PAGE_B_H = 344;
}
