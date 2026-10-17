package com.newuniverse.nusmp.client.mega.system;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.system.SystemText;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.slf4j.Logger;

/**
 * Why: the owner wants to move the System window on the screen with keybinds, so something has to keep the one piece of state that makes that possible, "edit
 * mode is on", and turn the player's keys into moves. This is that something, and it is deliberately small and safe. Edit mode is toggled by a key mapping and
 * opens NO screen: the window keeps drawing on the HUD, with an amber frame and a hint box ({@link OverlayLayout#edit}). While it is on and no screen is open, the
 * four nudge mappings (the arrow keys by default) move the window one pixel a step (ten with Shift), a held key repeats ({@link KeyRepeat}), the reset mapping puts
 * it back in its default corner, and the corner mapping walks it round the four corners; every step goes through the pure {@link OverlayMath}, which clamps the
 * window to the screen, so it can never be lost off its edge. The mouse needs a free cursor, and in the game the cursor is captured, so dragging works while a
 * screen that frees it is open (chat, inventory, the pause menu) and edit mode is on: a press inside the window picks it up, moving the mouse drags it, the release
 * lets go (the three events are cancelled only for a drag that started on the window, so the screen below never sees them). Nothing here grabs or releases the mouse
 * by itself. The place is kept in the client config in memory at once and written to the file when edit mode ends, a drag ends, or the player leaves the world
 * (never on every step, which would hit the disk twenty times a second). Client thread only.
 */
public final class SystemEditMode {
    private SystemEditMode() {}

    private static final Logger LOG = LogUtils.getLogger();

    private static boolean on;
    private static boolean dirty;
    private static boolean dragging;
    /** Where inside the window the mouse took hold of it, in GUI pixels. */
    private static double grabX, grabY;

    /** The size of the window as last drawn, in its own pixels (nudges clamp against it); a plausible size until the first draw. */
    private static int panelW = OverlayStyle.PANEL_MIN_W, panelH = 80;
    /** The rectangle of the window as last drawn, in GUI pixels (the mouse is tested against it). */
    private static float rectX, rectY, rectW, rectH;
    private static boolean rectKnown;

    private static final KeyRepeat[] REPEAT = {new KeyRepeat(), new KeyRepeat(), new KeyRepeat(), new KeyRepeat()};

    // ================================================================ state
    /** Is edit mode on? */
    public static boolean on() { return on; }

    /** Called by the overlay after it laid the window out: remembers the window's size and its rectangle in GUI pixels (for the nudges and the mouse). */
    static void drawn(OverlayLayout.Frame frame, float scale) {
        panelW = frame.w();
        panelH = frame.h();
        rectX = frame.x() * scale;
        rectY = frame.y() * scale;
        rectW = frame.w() * scale;
        rectH = frame.h() * scale;
        rectKnown = true;
    }

    /** The window is not drawn (hidden GUI, another dimension): the mouse has nothing to pick up. */
    static void notDrawn() { rectKnown = false; }

    /** Turns edit mode on or off with a line on the action bar; turning it off writes the config file. */
    static void setOn(Minecraft mc, boolean value) {
        if (on == value) return;
        on = value;
        dragging = false;
        for (KeyRepeat r : REPEAT) r.reset();
        if (!value) save();
        if (mc.player != null) {
            String key = value ? SystemText.EDIT_ON : SystemText.EDIT_OFF;
            mc.player.displayClientMessage(Component.translatableWithFallback(key, SystemText.english(key)), true);
        }
    }

    /** The player left the world (or the game is closing): edit mode ends and the place is written. */
    static void leave() {
        on = false;
        dragging = false;
        rectKnown = false;
        for (KeyRepeat r : REPEAT) r.reset();
        save();
    }

    private static void save() {
        if (!dirty) return;
        dirty = false;
        SystemClientConfig.save();
    }

    // ================================================================ the keys (once per client tick)
    /** Reads the edit keys. Cheap when edit mode is off: it only drains the mappings so presses made meanwhile do not pile up. */
    static void tick(Minecraft mc) {
        try {
            if (mc.player == null) {
                if (on) leave();
                drainAll();
                return;
            }
            while (SystemClient.EDIT.consumeClick()) setOn(mc, !on);
            if (dragging && mc.screen == null) {                                  // the screen closed under a drag (Escape): drop the window
                dragging = false;
                save();
            }
            if (!on || mc.screen != null) {
                drainAll();
                for (KeyRepeat r : REPEAT) r.reset();
                return;
            }
            int step = shiftHeld(mc) ? 10 : 1;
            int dx = 0, dy = 0;
            dx -= step * REPEAT[0].step(drain(SystemClient.LEFT), SystemClient.LEFT.isDown());
            dx += step * REPEAT[1].step(drain(SystemClient.RIGHT), SystemClient.RIGHT.isDown());
            dy -= step * REPEAT[2].step(drain(SystemClient.UP), SystemClient.UP.isDown());
            dy += step * REPEAT[3].step(drain(SystemClient.DOWN), SystemClient.DOWN.isDown());
            if (dx != 0 || dy != 0) nudge(mc, dx, dy);
            if (drain(SystemClient.RESET)) place(OverlayMath.DEFAULT_ANCHOR, OverlayMath.Offset.ZERO);
            if (drain(SystemClient.CORNER)) place(OverlayMath.nextAnchor(SystemClientConfig.anchor()), OverlayMath.Offset.ZERO);
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("[nusmp] The System window's edit keys failed and edit mode is switched off", e);
            on = false;
            dragging = false;
        }
    }

    private static boolean drain(KeyMapping m) {
        boolean any = false;
        while (m.consumeClick()) any = true;
        return any;
    }

    private static void drainAll() {
        drain(SystemClient.LEFT);
        drain(SystemClient.RIGHT);
        drain(SystemClient.UP);
        drain(SystemClient.DOWN);
        drain(SystemClient.RESET);
        drain(SystemClient.CORNER);
    }

    private static boolean shiftHeld(Minecraft mc) {
        long window = mc.getWindow().getWindow();
        return InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    // ================================================================ moving
    /** The size of the screen in the window's own pixels (the unit the offsets are kept in). */
    private static int screenW(Minecraft mc) { return OverlayMath.windowSize(mc.getWindow().getGuiScaledWidth(), SystemClientConfig.scale()); }

    private static int screenH(Minecraft mc) { return OverlayMath.windowSize(mc.getWindow().getGuiScaledHeight(), SystemClientConfig.scale()); }

    private static void nudge(Minecraft mc, int dx, int dy) {
        OverlayMath.Offset o = OverlayMath.nudge(SystemClientConfig.anchor(), screenW(mc), screenH(mc), panelW, panelH, OverlayStyle.MARGIN,
                SystemClientConfig.offsetX(), SystemClientConfig.offsetY(), dx, dy);
        place(SystemClientConfig.anchor(), o);
    }

    private static void place(int anchor, OverlayMath.Offset o) {
        SystemClientConfig.setPlacement(anchor, o.x(), o.y());
        dirty = true;
    }

    // ================================================================ the mouse (only while a screen frees the cursor)
    /** A press inside the window while edit mode is on picks it up. */
    static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre e) {
        try {
            if (!on || e.getButton() != 0 || !rectKnown || !OverlayMath.contains(rectX, rectY, rectW, rectH, e.getMouseX(), e.getMouseY())) return;
            dragging = true;
            grabX = e.getMouseX() - rectX;
            grabY = e.getMouseY() - rectY;
            e.setCanceled(true);
        } catch (RuntimeException ex) {
            dragging = false;
        }
    }

    /** Moving the mouse with the window picked up drags it: the corner under the mouse is the window's top-left, clamped to the screen. */
    static void onMouseDragged(ScreenEvent.MouseDragged.Pre e) {
        try {
            if (!dragging) return;
            Minecraft mc = Minecraft.getInstance();
            float scale = SystemClientConfig.scale();
            int x = Math.round((float) ((e.getMouseX() - grabX) / scale)), y = Math.round((float) ((e.getMouseY() - grabY) / scale));
            OverlayMath.Offset o = OverlayMath.offsetFor(SystemClientConfig.anchor(), screenW(mc), screenH(mc), panelW, panelH, OverlayStyle.MARGIN, x, y);
            place(SystemClientConfig.anchor(), o);
            e.setCanceled(true);
        } catch (RuntimeException ex) {
            dragging = false;
        }
    }

    /** Letting go of the button drops the window and writes the place. */
    static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre e) {
        try {
            if (!dragging || e.getButton() != 0) return;
            dragging = false;
            save();
            e.setCanceled(true);
        } catch (RuntimeException ex) {
            dragging = false;
        }
    }
}
