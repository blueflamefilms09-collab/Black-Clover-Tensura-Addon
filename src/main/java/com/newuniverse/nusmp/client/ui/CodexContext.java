package com.newuniverse.nusmp.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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

    public Font font() { return mc.font; }

    public LivingEntity player() { return mc.player; }

    public CompoundTag status() { return com.newuniverse.nusmp.client.multiverse.MultiverseStatusClient.status(); }
}
