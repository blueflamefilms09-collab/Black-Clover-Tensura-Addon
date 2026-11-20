package com.newuniverse.nusmp.client.hub;

import com.mojang.blaze3d.systems.RenderSystem;
import com.newuniverse.nusmp.client.ui.theme.UiThemeOptions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: Hub v2 replaces the two-letter glyphs of the hub nodes and the Codex navigation with real icon textures (docs/ui_hub.md, "Hub v2"). The icons are 32 x 32 PNGs painted by {@code tools/ui_icons/gen_hub_icons.py} under
 * {@code textures/gui/hub/}: {@code node_<tab key>.png} for the eight nodes, {@code codex_<page id>.png} for the Codex pages ({@link HubCodexNav}) and {@code cat_<category>.png} for the categories, each with a
 * {@code _glow.png} variant for the hovered or open one. They are drawn scaled (24 on the ring, 16 on the rail) with a {@code .png.mcmeta} that asks for filtering. One textured quad per icon. The
 * {@link ResourceLocation}s are built once per name (a small map filled the first time an icon is asked for) and then reused, so a frame allocates nothing. {@code ui.hubIcons = false} makes {@link #on} false and the
 * callers draw the old glyphs. This class is the one place of the hub that names textures.
 */
final class HubIcons {
    private HubIcons() {}

    static final int SRC = 32;
    private static final String DIR = "textures/gui/hub/";
    private static final Map<String, ResourceLocation> CACHE = new HashMap<>(), GLOW = new HashMap<>();
    private static final String[] NODE_NAMES = new String[HubNodes.COUNT];

    static {
        for (HubNodes.Tab t : HubNodes.Tab.values()) NODE_NAMES[t.ordinal()] = "node_" + t.key;
    }

    /** True when icons are drawn ({@code ui.hubIcons}, default true). */
    static boolean on() { return UiThemeOptions.hubIcons(); }

    /** The icon file name of a hub node: {@code node_<key>}. */
    static String node(HubNodes.Tab t) { return NODE_NAMES[t.ordinal()]; }

    /** The texture of icon {@code name} ({@code node_status}, {@code codex_spells}, {@code cat_magic}, ...), its glow variant when {@code glow}. */
    static ResourceLocation tex(String name, boolean glow) {
        Map<String, ResourceLocation> m = glow ? GLOW : CACHE;
        ResourceLocation r = m.get(name);                                              // the lookup key is the caller's own string: nothing is built after the first call
        if (r == null) {
            r = ResourceLocation.fromNamespaceAndPath("anime_multiverse", DIR + name + (glow ? "_glow" : "") + ".png");
            m.put(name, r);
        }
        return r;
    }

    /**
     * Draws icon {@code name} centred on (cx, cy), {@code size} units square, at {@code alpha}; the glow variant is blended over it by {@code glow} (0..1: 0 = the plain icon only). Two quads at most, none when
     * {@code alpha} is under 0.02.
     */
    static void draw(GuiGraphics g, String name, int cx, int cy, int size, float alpha, float glow) {
        if (alpha <= 0.02f) return;
        int x = cx - size / 2, y = cy - size / 2;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1f, 1f, 1f, Math.min(1f, alpha));
        g.blit(tex(name, false), x, y, size, size, 0f, 0f, SRC, SRC, SRC, SRC);
        if (glow > 0.02f) {
            g.setColor(1f, 1f, 1f, Math.min(1f, alpha * glow));
            g.blit(tex(name, true), x, y, size, size, 0f, 0f, SRC, SRC, SRC, SRC);
        }
        g.setColor(1f, 1f, 1f, 1f);
    }
}
