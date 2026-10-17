package com.newuniverse.nusmp.client.avatar;

import com.newuniverse.nusmp.avatar.appearance.AppearanceData;
import com.newuniverse.nusmp.avatar.aptitude.ForgeContext;
import com.newuniverse.nusmp.avatar.aptitude.ForgeRules;
import com.newuniverse.nusmp.avatar.menu.DiceSpin;
import com.newuniverse.nusmp.avatar.menu.ForgeLayout.Rect;
import com.newuniverse.nusmp.avatar.menu.ForgeState;
import com.newuniverse.nusmp.avatar.net.RaceEntry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Why: the five pages of the Soul Forge screen each draw and handle their own controls, but they all need the same few things from the screen that holds them
 * (the creator's choices, the rules and reserves the server sent, the Avatar's look, the clock, the eased hover glow, a place to queue a tooltip, the way to
 * send a request or make the Avatar on the stage react). This is that narrow doorway: the pages know the interface, not the screen, so each page stays a
 * small class and the screen stays the only place that talks to the network and the sound system.
 */
interface ForgeHost {
    ForgeState state();

    Font font();

    /** The animation clock in ticks (frozen when the player switched UI motion off). */
    float now();

    /** False when the player switched UI motion off: pages should show end states instead of animating. */
    boolean motion();

    ForgeRules rules();

    ForgeContext reserves();

    int rollsLeft();

    AppearanceData look();

    void setLook(AppearanceData look);

    /** The race the creator chose, or null. */
    RaceEntry raceEntry();

    /** The size icons in a grid prefer, and the size of the big round slots, for this window ({@code ForgeLayout.Layout}). */
    int iconSize();

    int slotSize();

    /** Clips drawing to a box of the module (given in the module's own frame; the screen adds the slide), until {@link #endScissor}. */
    void scissor(GuiGraphics g, Rect r);

    void endScissor(GuiGraphics g);

    /** The eased 0..1 hover glow of a control; call once per control per frame. */
    float hover(Object key, boolean hovered);

    /** Queues a tooltip for this frame (the last one queued wins); drawn above everything. */
    void tooltip(List<Component> lines);

    void click();

    /** The soft tick of a reel passing a cell, at this pitch. */
    void tickSound(float pitch);

    /** Called after any choice changed, so the cost panel and the problems list are rebuilt. */
    void changed();

    /** The Avatar on the stage reacts: a ring of this colour (0xRRGGBB) opens round it. */
    void pulse(int rgb);

    /** For this frame the stage's aura leans toward this colour (a hovered aptitude or skill); call each frame the pointer is on it. */
    void leanTo(int rgb);

    void requestStatRoll();

    void requestAptitudeRoll();

    void requestBark();

    DiceSpin dice();

    /** The name box (one per screen); the Identity page places and draws it. */
    EditBox nameBox();

    /** What the server last answered, to show under the stage (empty when nothing). */
    void say(Component message, boolean good);
}
