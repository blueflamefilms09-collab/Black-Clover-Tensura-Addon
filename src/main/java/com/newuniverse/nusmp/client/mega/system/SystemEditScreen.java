package com.newuniverse.nusmp.client.mega.system;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.client.mega.system.StatScreenLayout.Rect;
import com.newuniverse.nusmp.labyrinth.mega.system.PointsText;
import com.newuniverse.nusmp.labyrinth.mega.system.SystemText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

/**
 * Why: the owner wants to move and resize the System window with the MOUSE (and no keys). In the game the cursor is captured, so something has to free it: this
 * screen is what "edit mode" is now. The edit key opens it (and closes it again), it shows the REAL window (or a labelled sample when the real one is not on, so
 * the place can be set anywhere) at the place and size in the client config as they are right now, and everything is the mouse: press inside the window and drag
 * to move it (it is anchored to the nearest of nine points, the corners, the edge middles and the centre, so the place survives a change of resolution or GUI
 * scale: {@link OverlayDrag#snapGui}); press on a corner handle and drag to resize it (the opposite corner stays put; {@link OverlayDrag#corner}); or scroll
 * over it (Shift: bigger steps) to resize round the point under the mouse. The size is 0.5 to 2.5 times the standard. [RESET] puts the window back in the
 * top-left corner at the standard size, [STAT POINTS] opens {@link SystemStatsScreen}, [DONE] (or the edit key, or Escape) saves and closes. The place and the size
 * are written to the client config when a drag ends and when the screen closes, never on every mouse move. All arithmetic is in the pure {@link OverlayDrag}.
 */
public final class SystemEditScreen extends Screen {
    private static final Logger LOG = LogUtils.getLogger();
    private static final int NONE = 0, MOVING = 1, RESIZING = 2;
    private static final int HANDLE = 6, BTN_W = 96, BTN_H = 14;

    private final Screen parent;
    private OverlayLayout.Frame frame;
    private int drag = NONE, corner = -1;
    private double grabX, grabY, startLeft, startTop;
    private float startScale;
    private boolean dirty;

    public SystemEditScreen(Screen parent) {
        super(Component.translatableWithFallback(PointsText.EDIT_TITLE, PointsText.all().get(PointsText.EDIT_TITLE)));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private static String tr(String key, Object... args) { return Component.translatableWithFallback(key, SystemText.english(key), args).getString(); }

    // ================================================================ closing
    @Override
    public void onClose() {
        drag = NONE;
        save();
        minecraft.setScreen(parent);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && parent == null) mc.player.displayClientMessage(Component.translatableWithFallback(PointsText.EDIT_SAVED, PointsText.all().get(PointsText.EDIT_SAVED)), true);
    }

    private void save() {
        if (!dirty) return;
        dirty = false;
        SystemClientConfig.save();
    }

    // ================================================================ geometry (GUI pixels)
    private float scale() { return SystemClientConfig.scale(); }

    private double left() { return frame == null ? 0.0 : frame.x() * (double) scale(); }

    private double top() { return frame == null ? 0.0 : frame.y() * (double) scale(); }

    private double w() { return frame == null ? 0.0 : frame.w() * (double) scale(); }

    private double h() { return frame == null ? 0.0 : frame.h() * (double) scale(); }

    private Rect[] buttons() {
        int y = height - BTN_H - 8, gap = 6;
        int total = 3 * BTN_W + 2 * gap;
        int x = (width - total) / 2;
        return new Rect[]{new Rect(x, y, BTN_W, BTN_H), new Rect(x + BTN_W + gap, y, BTN_W, BTN_H), new Rect(x + 2 * (BTN_W + gap), y, BTN_W, BTN_H)};
    }

    // ================================================================ drawing
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, 0x70000000);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        try {
            renderBackground(g, mx, my, partial);
            OverlayLayout.Frame f = SystemOverlay.drawPreview(g, Minecraft.getInstance(), partial);
            if (f != null) frame = f;
            if (frame != null) drawHandles(g, mx, my);
            g.drawCenteredString(font, tr(PointsText.EDIT_TITLE), width / 2, 10, OverlayMath.withAlpha(OverlayStyle.AMBER, 1f));
            g.drawCenteredString(font, tr(PointsText.EDIT_HINT), width / 2, 22, OverlayMath.withAlpha(OverlayStyle.TEXT, 1f));
            g.drawCenteredString(font, tr(PointsText.EDIT_SIZE, Math.round(scale() * 100) + "%"), width / 2, 34, OverlayMath.withAlpha(OverlayStyle.CYAN_BRIGHT, 1f));
            Rect[] b = buttons();
            button(g, b[0], tr(PointsText.EDIT_RESET), b[0].contains(mx, my));
            button(g, b[1], tr(PointsText.EDIT_STATS), b[1].contains(mx, my));
            button(g, b[2], tr(PointsText.EDIT_DONE), b[2].contains(mx, my));
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] The window edit screen failed to draw and is closed", e);
            onClose();
        }
    }

    private static void fill(GuiGraphics g, int x1, int y1, int x2, int y2, int rgb, float a) {
        if (x2 > x1 && y2 > y1) g.fill(x1, y1, x2, y2, OverlayMath.withAlpha(rgb, a));
    }

    /** The amber frame round the window and a square handle on each corner (the one under the mouse, or being dragged, is lit). */
    private void drawHandles(GuiGraphics g, int mx, int my) {
        int x1 = (int) Math.floor(left()) - 2, y1 = (int) Math.floor(top()) - 2, x2 = (int) Math.ceil(left() + w()) + 2, y2 = (int) Math.ceil(top() + h()) + 2;
        float pulse = 0.7f + 0.3f * (float) Math.sin(System.nanoTime() / 1.0e8);
        fill(g, x1, y1, x2, y1 + 1, OverlayStyle.AMBER, pulse);
        fill(g, x1, y2 - 1, x2, y2, OverlayStyle.AMBER, pulse);
        fill(g, x1, y1 + 1, x1 + 1, y2 - 1, OverlayStyle.AMBER, pulse);
        fill(g, x2 - 1, y1 + 1, x2, y2 - 1, OverlayStyle.AMBER, pulse);
        int hover = drag == RESIZING ? corner : OverlayDrag.cornerAt(mx, my, left(), top(), w(), h(), HANDLE);
        int[][] pts = {{x1, y1}, {x2, y1}, {x1, y2}, {x2, y2}};
        for (int i = 0; i < 4; i++) {
            int hs = i == hover ? HANDLE / 2 + 1 : HANDLE / 2;
            fill(g, pts[i][0] - hs, pts[i][1] - hs, pts[i][0] + hs, pts[i][1] + hs, i == hover ? OverlayStyle.TEXT : OverlayStyle.AMBER, 1f);
        }
    }

    private void button(GuiGraphics g, Rect r, String label, boolean hover) {
        fill(g, r.x(), r.y(), r.right(), r.bottom(), OverlayStyle.CYAN, hover ? 0.38f : 0.18f);
        fill(g, r.x(), r.y(), r.right(), r.y() + 1, OverlayStyle.CYAN, hover ? 1f : 0.75f);
        fill(g, r.x(), r.bottom() - 1, r.right(), r.bottom(), OverlayStyle.CYAN, hover ? 1f : 0.75f);
        fill(g, r.x(), r.y() + 1, r.x() + 1, r.bottom() - 1, OverlayStyle.CYAN, hover ? 1f : 0.75f);
        fill(g, r.right() - 1, r.y() + 1, r.right(), r.bottom() - 1, OverlayStyle.CYAN, hover ? 1f : 0.75f);
        g.drawString(font, label, r.x() + (r.w() - font.width(label)) / 2, r.y() + (r.h() - 8) / 2 + 1, OverlayMath.withAlpha(OverlayStyle.TEXT, 1f), true);
    }

    // ================================================================ the mouse
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        Rect[] b = buttons();
        if (b[0].contains(mx, my)) {
            reset();
            return true;
        }
        if (b[1].contains(mx, my)) {
            save();
            minecraft.setScreen(new SystemStatsScreen(this));
            return true;
        }
        if (b[2].contains(mx, my)) {
            onClose();
            return true;
        }
        if (frame == null) return super.mouseClicked(mx, my, button);
        startLeft = left();
        startTop = top();
        startScale = scale();
        int c = OverlayDrag.cornerAt(mx, my, left(), top(), w(), h(), HANDLE);
        if (c >= 0) {
            drag = RESIZING;
            corner = c;
            return true;
        }
        if (OverlayMath.contains((float) left(), (float) top(), (float) w(), (float) h(), mx, my)) {
            drag = MOVING;
            grabX = mx - left();
            grabY = my - top();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button != 0 || drag == NONE || frame == null) return super.mouseDragged(mx, my, button, dx, dy);
        try {
            if (drag == MOVING) {
                SystemClientConfig.setPlace(OverlayDrag.drag(width, height, scale(), frame.w(), frame.h(), OverlayStyle.MARGIN, mx, my, grabX, grabY));
            } else {
                OverlayDrag.Resized r = OverlayDrag.corner(corner, mx, my, startLeft, startTop, frame.w(), frame.h(), startScale);
                apply(r);
            }
            dirty = true;
        } catch (RuntimeException e) {
            drag = NONE;
        }
        return true;
    }

    /** Sets the new size and the place that keeps the resized window where the resize put it. */
    private void apply(OverlayDrag.Resized r) {
        SystemClientConfig.setScale(r.scale());
        SystemClientConfig.setPlace(OverlayDrag.snapGui(width, height, r.scale(), frame.w(), frame.h(), OverlayStyle.MARGIN, r.left(), r.top()));
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0 && drag != NONE) {
            drag = NONE;
            corner = -1;
            save();
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (frame == null || scrollY == 0.0 || drag != NONE) return super.mouseScrolled(mx, my, scrollX, scrollY);
        apply(OverlayDrag.wheel(left(), top(), frame.w(), frame.h(), scale(), scrollY, hasShiftDown(), mx, my));
        dirty = true;
        save();
        return true;
    }

    private void reset() {
        if (frame != null) {
            SystemClientConfig.setScale(1.0f);
            SystemClientConfig.setPlace(OverlayDrag.Place.DEFAULT);
            dirty = true;
            save();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (SystemClient.EDIT.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
