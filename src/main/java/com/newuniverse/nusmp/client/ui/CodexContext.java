package com.newuniverse.nusmp.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

/** 0.100: what a Codex page may read: the font, the player and the server's last status message. */
public final class CodexContext {
    public final Minecraft mc = Minecraft.getInstance();
    public final CodexScreen screen;
    /** Where the figure was last drawn, for hit tests (set by the overview page). */
    public int figureX, figureY, figureW, figureH;
    /** The yaw the figure is turned to (dragged by the mouse). */
    public float figureYaw = 25f;

    CodexContext(CodexScreen screen) { this.screen = screen; }

    /** Hub v2: a context with no book screen, for pages the Multiverse Hub hosts in its own body ({@link #screen} is null; no stock page reads it). */
    public CodexContext() { this.screen = null; }

    /**
     * Hub v2: where the page's coordinates sit on the screen. The book draws pages at scale 1 from (0, 0) (these stay 1, 0, 0), the hub draws them inside its scaled window and sets the real scale and offset, because a
     * scissor takes screen units, not the units of the pose.
     */
    public float viewScale = 1f, viewX = 0f, viewY = 0f;

    /** Clips to a rectangle given in the page's own coordinates (the same as {@code g.enableScissor} in the book). Pair it with {@code g.disableScissor()}. */
    public void scissor(GuiGraphics g, int x0, int y0, int x1, int y1) {
        g.enableScissor(Math.round(viewX + x0 * viewScale), Math.round(viewY + y0 * viewScale), Math.round(viewX + x1 * viewScale), Math.round(viewY + y1 * viewScale));
    }

    public Font font() { return mc.font; }

    public LivingEntity player() { return mc.player; }

    public CompoundTag status() { return com.newuniverse.nusmp.client.multiverse.MultiverseStatusClient.status(); }
}
