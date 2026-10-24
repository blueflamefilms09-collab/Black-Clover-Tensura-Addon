package com.newuniverse.nusmp.client.companion.gs;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.newuniverse.nusmp.client.ui.gs.GsCompileText;
import com.newuniverse.nusmp.client.ui.gs.GsSkin;
import com.newuniverse.nusmp.client.ui.gs.GsTex;
import com.newuniverse.nusmp.client.ui.theme.UiThemeOptions;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.function.IntFunction;

/**
 * The game's {@link GsCanvas}: every call goes straight to a {@code GuiGraphics}. One instance lives in the screen and is re-pointed at the frame's graphics each frame; it builds no location, no string
 * and no object while drawing (the textures are the static ones of {@link GsTex}, the portrait's skin location is built once by the screen, the compile block is cached by the array it was built from).
 * Items are asked of the screen by reference ({@link GsItemRef}).
 */
final class GsCanvasImpl implements GsCanvas {
    private GuiGraphics g;
    private Font font;
    private final IntFunction<ItemStack> items;
    private ResourceLocation faceTexture;
    private int faceTexH = 64;

    private String[] cachedSource;
    private int cachedCount = -1;
    private GsCompileText.Lines cached;

    GsCanvasImpl(IntFunction<ItemStack> items) { this.items = items; }

    void set(GuiGraphics graphics, Font f, ResourceLocation face, int faceH) {
        g = graphics;
        font = f;
        faceTexture = face;
        faceTexH = faceH;
    }

    /** Points the canvas at a font only (to measure text before a frame is drawn). */
    void font(Font f) { font = f; }

    @Override public void fill(int x1, int y1, int x2, int y2, int argb) { g.fill(x1, y1, x2, y2, argb); }

    @Override public void text(String s, int x, int y, int argb) { g.drawString(font, s, x, y, argb, false); }

    @Override public int textWidth(String s) { return font.width(s); }

    @Override public void gradient(int x1, int y1, int x2, int y2, int top, int bottom) { g.fillGradient(x1, y1, x2, y2, top, bottom); }

    @Override public void clip(int x1, int y1, int x2, int y2) { g.enableScissor(x1, y1, x2, y2); }

    @Override public void unclip() { g.disableScissor(); }

    @Override
    public void sprite(GsSprite s, GsSkin skin, int x, int y, int w, int h, int u, int v, int uw, int vh, int tint, boolean additive) {
        ResourceLocation tex;
        int texW, texH;
        int k = skin.ordinal();
        switch (s) {
            case CORNER -> { tex = GsTex.CORNER[k]; texW = texH = GsSprite.SHEET; }
            case CLOVER3 -> { tex = GsTex.CLOVER[k][0]; texW = texH = GsSprite.SHEET; }
            case CLOVER4 -> { tex = GsTex.CLOVER[k][1]; texW = texH = GsSprite.SHEET; }
            case CLOVER5 -> { tex = GsTex.CLOVER[k][2]; texW = texH = GsSprite.SHEET; }
            case GLOW4 -> { tex = GsTex.GLOW[k][0]; texW = texH = GsSprite.SHEET; }
            case GLOW5 -> { tex = GsTex.GLOW[k][1]; texW = texH = GsSprite.SHEET; }
            case DRIPS -> { tex = GsTex.DRIPS[k]; texW = texH = GsSprite.SHEET; }
            case RIPPLE -> { tex = GsTex.RIPPLE[k]; texW = texH = GsSprite.RIPPLE_SHEET; }
            default -> {
                if (faceTexture == null) return;
                tex = faceTexture;
                texW = 64;
                texH = faceTexH;
            }
        }
        float a = ((tint >>> 24) & 255) / 255f, r = ((tint >> 16) & 255) / 255f, gr = ((tint >> 8) & 255) / 255f, b = (tint & 255) / 255f;
        if (additive) {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        }
        g.setColor(r, gr, b, a);
        g.blit(tex, x, y, w, h, (float) u, (float) v, uw, vh, texW, texH);
        g.setColor(1f, 1f, 1f, 1f);
        if (additive) RenderSystem.defaultBlendFunc();
    }

    @Override
    public void item(int ref, int x, int y) {
        ItemStack st = items.apply(ref);
        if (st == null || st.isEmpty()) return;
        g.renderItem(st, x, y);
        g.renderItemDecorations(font, st, x, y);
    }

    @Override
    public void compiled(String[] lines, int count, int x, int y, int lineH, float age, int ink, int settled, int data) {
        if (count <= 0) return;
        if (cached == null || cachedSource != lines || cachedCount != count) {                // built once per set of lines
            cached = GsCompileText.ofLines(lines, count, lineH);
            cachedSource = lines;
            cachedCount = count;
        }
        GsCompileText.draw(g, font, cached, x, y, lineH, age, ink, settled, data, UiThemeOptions.highQuality());
    }
}
