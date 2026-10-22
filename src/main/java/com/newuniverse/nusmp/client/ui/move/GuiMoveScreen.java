package com.newuniverse.nusmp.client.ui.move;

import com.newuniverse.nusmp.client.ui.move.GuiMoveMath.Rect;
import com.newuniverse.nusmp.client.ui.move.GuiMoveRegistry.Element;
import com.newuniverse.nusmp.client.ui.theme.ThemedButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: this is "edit mode" of the GUI Move system: one screen, opened by the GUI Move key (F9) or {@code /nusmp gui move}, that frees the cursor and lets the player place
 * every custom GUI of the mod. Every registered HUD element that was on screen shows a dashed outline and its name; the ones that are not on screen right now (a Djinn panel
 * without a Djinn, the quest tracker without a quest) show dimmed, so they can be placed too (the {@code Show all} switch). The panels of the mod's screens (the codex, the
 * forge ...) are all centred and would lie on top of each other, so they are picked one at a time with Tab once {@code Panels} is on, and the picked one takes its place when
 * its screen opens. <b>Drag</b> an outline to move the GUI (it snaps to the screen's edges and middle and to the other GUIs; hold Alt to place it freely), <b>wheel</b> or
 * + / - to size it (0.5 to 2.5, Shift for big steps), <b>arrow keys</b> to nudge one pixel (Shift: eight), <b>R</b> to put the selected one back, <b>H</b> or a right click
 * to hide or show it, <b>Tab</b> to select the next one, <b>Reset all</b> and <b>Done</b> below. An element can never be dragged off the screen (at least 24 pixels always
 * stay in reach) and its place is kept as a corner or middle anchor with an offset, so it stays put when the window or the GUI scale changes ({@link GuiMoveMath}). The layout
 * is written when a drag ends and when the screen closes, never per mouse move. Drawn in raw GUI pixels on a flat dim (never the blurring vanilla background,
 * {@code ScreenBlurGuardTest}); nothing here runs while the screen is closed.
 */
public final class GuiMoveScreen extends Screen {
    private static final int BTN_W = 80, BTN_H = 16, DASH = 4, GAP = 3;
    private static final int LIVE = 0xFF5FE0FF, GHOST = 0x905FE0FF, SELECTED = 0xFFFFB84D, HIDDEN = 0xFFFF5A5A, TEXT = 0xFFE8F4FF, TEXT_DIM = 0xFF9DB4C4, GUIDE = 0xFFFF7BD5;

    private final Screen parent;
    /** What is drawn and can be hit this frame. */
    private final List<Element> shown = new ArrayList<>();
    /** What Tab goes through: everything shown, and the panels (when {@code Panels} is on). */
    private final List<Element> cycle = new ArrayList<>();
    /** The HUD elements that were on screen when edit mode opened (many stop drawing while a screen is open, so this is the picture of what the player was looking at). */
    private final List<Element> live = new ArrayList<>();
    private Element selected, dragging;
    private double grabX, grabY;
    private double guideX = Double.NaN, guideY = Double.NaN;
    private boolean dirty;
    private ThemedButton snapButton, showButton, panelsButton;
    private int footTop;

    public GuiMoveScreen(Screen parent) {
        super(tr("gui_move.title", "Move GUIs"));
        this.parent = parent;
        for (Element e : GuiMoveRegistry.all()) if (e.kind == GuiMoveRegistry.Kind.HUD && GuiMove.visibleNow(e)) live.add(e);
    }

    public static Component tr(String key, String english, Object... args) { return Component.translatableWithFallback(key, english, args); }

    private boolean isLive(Element e) { return e.kind == GuiMoveRegistry.Kind.HUD && (live.contains(e) || GuiMove.visibleNow(e)); }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void removed() {
        GuiMove.setEditing(false);
        super.removed();
    }

    // ================================================================ widgets
    @Override
    protected void init() {
        GuiMove.setEditing(true);
        int gap = 4, perRow = Math.max(1, Math.min(5, (width - 8 + gap) / (BTN_W + gap)));
        int rows = (5 + perRow - 1) / perRow;
        footTop = height - rows * (BTN_H + gap) - 4;
        snapButton = button(0, perRow, gap, snapLabel(), b -> { GuiMoveOptions.setSnap(!GuiMoveOptions.snap()); b.setMessage(snapLabel()); });
        showButton = button(1, perRow, gap, showLabel(), b -> { GuiMoveOptions.setShowAll(!GuiMoveOptions.showAll()); b.setMessage(showLabel()); });
        panelsButton = button(2, perRow, gap, panelsLabel(), b -> { GuiMoveOptions.setShowPanels(!GuiMoveOptions.showPanels()); b.setMessage(panelsLabel()); });
        button(3, perRow, gap, tr("gui_move.reset_all", "Reset all"), b -> resetAll());
        button(4, perRow, gap, tr("gui_move.done", "Done"), b -> onClose());
    }

    /** Button {@code i} of five, laid out in rows of {@code perRow}, centred, at the bottom. */
    private ThemedButton button(int i, int perRow, int gap, Component label, net.minecraft.client.gui.components.Button.OnPress press) {
        int row = i / perRow, inRow = Math.min(perRow, 5 - row * perRow);
        int total = inRow * BTN_W + (inRow - 1) * gap;
        int x = (width - total) / 2 + (i % perRow) * (BTN_W + gap), y = footTop + row * (BTN_H + gap);
        return addRenderableWidget(new ThemedButton(x, y, BTN_W, BTN_H, label, press));
    }

    private static Component onOff(boolean on) { return on ? tr("gui_move.on", "ON") : tr("gui_move.off", "OFF"); }

    private static Component snapLabel() { return tr("gui_move.snap", "Snap: %s", onOff(GuiMoveOptions.snap()).getString()); }

    private static Component showLabel() { return tr("gui_move.show_all", "Show all: %s", onOff(GuiMoveOptions.showAll()).getString()); }

    private static Component panelsLabel() { return tr("gui_move.panels", "Panels: %s", onOff(GuiMoveOptions.showPanels()).getString()); }

    private void resetAll() {
        GuiMove.resetAll();
        selected = null;
        dragging = null;
        dirty = false;
    }

    // ================================================================ closing
    @Override
    public void onClose() {
        dragging = null;
        save();
        minecraft.setScreen(parent);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && parent == null) mc.player.displayClientMessage(tr("gui_move.saved", "GUI layout saved."), true);
    }

    private void save() {
        if (!dirty) return;
        dirty = false;
        GuiMoveOptions.save();
    }

    // ================================================================ what is shown
    private void collect() {
        shown.clear();
        cycle.clear();
        boolean all = GuiMoveOptions.showAll(), panels = GuiMoveOptions.showPanels();
        for (Element e : GuiMoveRegistry.all()) {
            GuiMove.syncGs(e);
            if (e.kind == GuiMoveRegistry.Kind.HUD) {
                if (isLive(e) || all || e.placed() || e.hidden()) { shown.add(e); cycle.add(e); }
            } else if (e.placed() || (panels && e == selected)) {
                shown.add(e);
                cycle.add(e);
            } else if (panels) {
                cycle.add(e);
            }
        }
        if (selected != null && !cycle.contains(selected)) selected = null;
    }

    private Element at(double mx, double my) {
        if (selected != null && shown.contains(selected) && GuiMove.rectOf(selected, width, height).contains(mx, my)) return selected;
        for (int i = shown.size() - 1; i >= 0; i--) {
            Element e = shown.get(i);
            if (GuiMove.rectOf(e, width, height).contains(mx, my)) return e;
        }
        return null;
    }

    // ================================================================ drawing
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, 0x60000000);                                    // a plain dim, never the blurring vanilla background (ScreenBlurGuardTest)
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        collect();
        List<FormattedCharSequence> hint = font.split(tr("gui_move.hint", "Drag a GUI to move it. Wheel or + / -: size. Arrows: nudge (Shift: 8 px). R: reset it. H or right click: hide / show. Tab: next. Alt: no snapping. Esc or Done: save and close."), Math.max(80, width - 24));
        List<FormattedCharSequence> hint2 = font.split(tr("gui_move.hint2", "Dimmed: not on screen right now. For the panel of a screen (codex, forge ...) turn Panels on and press Tab: it takes its place when that screen opens."), Math.max(80, width - 24));
        int bandH = 22 + (hint.size() + hint2.size()) * 10;
        boolean showHint = dragging == null && my >= bandH;                         // the words step aside while a GUI is dragged or the mouse is up there, so the GUIs at the top stay reachable
        if (showHint) g.fill(0, 0, width, bandH, 0xA0000000);                       // a plain band behind the words, so they stay readable over any outline
        for (Element e : shown) if (e != selected) outline(g, e, false);
        if (selected != null && shown.contains(selected)) outline(g, selected, true);
        if (dragging != null) {
            if (!Double.isNaN(guideX)) g.fill((int) Math.round(guideX), 0, (int) Math.round(guideX) + 1, height, GUIDE);
            if (!Double.isNaN(guideY)) g.fill(0, (int) Math.round(guideY), width, (int) Math.round(guideY) + 1, GUIDE);
        }
        g.drawCenteredString(font, title, width / 2, 6, SELECTED);
        int y = 18;
        for (FormattedCharSequence line : hint) {
            if (showHint) g.drawString(font, line, Math.max(4, (width - font.width(line)) / 2), y, TEXT, true);
            y += 10;
        }
        for (FormattedCharSequence line : hint2) {
            if (showHint) g.drawString(font, line, Math.max(4, (width - font.width(line)) / 2), y, TEXT_DIM, true);
            y += 10;
        }
        if (selected != null) {
            Rect r = GuiMove.rectOf(selected, width, height);
            Component info = tr("gui_move.info", "%s   x %s  y %s   size %s%%", name(selected), Math.round(r.x()), Math.round(r.y()), Math.round(GuiMove.scaleOf(selected) * 100));
            int iw = font.width(info);
            g.fill((width - iw) / 2 - 4, footTop - 14, (width + iw) / 2 + 4, footTop - 3, 0xA0000000);
            g.drawCenteredString(font, info, width / 2, footTop - 12, TEXT);
        }
    }

    private Component name(Element e) { return tr(e.nameKey(), e.english); }

    private void outline(GuiGraphics g, Element e, boolean sel) {
        Rect r = GuiMove.rectOf(e, width, height);
        boolean hidden = e.hidden(), alive = isLive(e);
        int color = sel ? SELECTED : hidden ? HIDDEN : alive ? LIVE : GHOST;
        int x1 = (int) Math.round(r.x()), y1 = (int) Math.round(r.y()), x2 = (int) Math.round(r.right()), y2 = (int) Math.round(r.bottom());
        if (sel || alive) g.fill(x1, y1, x2, y2, (sel ? 0x28000000 : 0x14000000) | (color & 0xFFFFFF));
        int gap = alive || sel ? GAP : GAP + 3;
        dashH(g, x1, x2, y1, color, gap);
        dashH(g, x1, x2, y2 - 1, color, gap);
        dashV(g, y1, y2, x1, color, gap);
        dashV(g, y1, y2, x2 - 1, color, gap);
        Component label = name(e);
        if (hidden) label = Component.literal(label.getString() + " (" + tr("gui_move.hidden", "hidden").getString() + ")");
        else if (e.kind == GuiMoveRegistry.Kind.PANEL) label = Component.literal(label.getString() + " (" + tr("gui_move.panel", "screen").getString() + ")");
        int lw = font.width(label) + 6;
        int lx = Math.max(0, Math.min(width - lw, x1)), ly = y1 >= 12 ? y1 - 11 : y1 + 1;
        g.fill(lx, ly, lx + lw, ly + 10, 0xB0000000);
        g.fill(lx, ly, lx + 2, ly + 10, color);
        g.drawString(font, label, lx + 4, ly + 1, sel ? SELECTED : hidden ? HIDDEN : TEXT, false);
    }

    private static void dashH(GuiGraphics g, int x1, int x2, int y, int color, int gap) {
        for (int x = x1; x < x2; x += DASH + gap) g.fill(x, y, Math.min(x2, x + DASH), y + 1, color);
    }

    private static void dashV(GuiGraphics g, int y1, int y2, int x, int color, int gap) {
        for (int y = y1; y < y2; y += DASH + gap) g.fill(x, y, x + 1, Math.min(y2, y + DASH), color);
    }

    // ================================================================ the mouse
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;                       // the buttons first
        Element e = at(mx, my);
        if (button == 1) {
            if (e != null && e.kind == GuiMoveRegistry.Kind.HUD) {
                selected = e;
                GuiMove.setHidden(e, !e.hidden());
                dirty = true;
                save();
            }
            return e != null;
        }
        if (button != 0) return false;
        selected = e;
        if (e == null) return false;
        Rect r = GuiMove.rectOf(e, width, height);
        dragging = e;
        grabX = mx - r.x();
        grabY = my - r.y();
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button != 0 || dragging == null) return super.mouseDragged(mx, my, button, dx, dy);
        try {
            Rect r = GuiMove.rectOf(dragging, width, height);
            Rect moving = new Rect(mx - grabX, my - grabY, r.w(), r.h());
            guideX = Double.NaN;
            guideY = Double.NaN;
            if (GuiMoveOptions.snap() && !hasAltDown()) {
                Rect[] others = new Rect[shown.size()];
                for (int i = 0; i < others.length; i++) others[i] = shown.get(i) == dragging ? null : GuiMove.rectOf(shown.get(i), width, height);
                GuiMoveMath.Snapped s = GuiMoveMath.snap(moving, others, width, height, GuiMoveMath.SNAP_DISTANCE);
                moving = new Rect(s.x(), s.y(), r.w(), r.h());
                guideX = s.guideX();
                guideY = s.guideY();
            }
            GuiMove.moveTo(dragging, width, height, moving.x(), moving.y());
            dirty = true;
        } catch (RuntimeException e) {
            dragging = null;                                                       // never break the game over a drag
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0 && dragging != null) {
            dragging = null;
            guideX = Double.NaN;
            guideY = Double.NaN;
            save();
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (scrollY == 0.0 || dragging != null) return super.mouseScrolled(mx, my, scrollX, scrollY);
        Element e = at(mx, my);
        if (e == null) e = selected;
        if (e == null) return super.mouseScrolled(mx, my, scrollX, scrollY);
        selected = e;
        resize(e, scrollY, hasShiftDown(), mx, my);
        return true;
    }

    private void resize(Element e, double dir, boolean big, double mx, double my) {
        Rect r = GuiMove.rectOf(e, width, height);
        GuiMoveMath.Resized z = GuiMoveMath.wheel(r.x(), r.y(), GuiMove.widthOf(e), GuiMove.heightOf(e), GuiMove.scaleOf(e), e.gs, dir, big, mx, my);
        GuiMove.resizeTo(e, width, height, z.moveScale(), z.left(), z.top());
        dirty = true;
        save();
    }

    // ================================================================ the keyboard
    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (GuiMove.KEY.matches(key, scan)) {
            onClose();
            return true;
        }
        if (key == GLFW.GLFW_KEY_TAB) {
            cycle(hasShiftDown() ? -1 : 1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_S && !hasControlDown()) {
            GuiMoveOptions.setSnap(!GuiMoveOptions.snap());
            if (snapButton != null) snapButton.setMessage(snapLabel());
            return true;
        }
        if (key == GLFW.GLFW_KEY_A && !hasControlDown()) {
            GuiMoveOptions.setShowAll(!GuiMoveOptions.showAll());
            if (showButton != null) showButton.setMessage(showLabel());
            return true;
        }
        if (key == GLFW.GLFW_KEY_P && !hasControlDown()) {
            GuiMoveOptions.setShowPanels(!GuiMoveOptions.showPanels());
            if (panelsButton != null) panelsButton.setMessage(panelsLabel());
            return true;
        }
        Element e = selected;
        if (e != null) {
            int step = hasShiftDown() ? GuiMoveMath.BIG_NUDGE : GuiMoveMath.NUDGE;
            switch (key) {
                case GLFW.GLFW_KEY_LEFT -> { nudge(e, -step, 0); return true; }
                case GLFW.GLFW_KEY_RIGHT -> { nudge(e, step, 0); return true; }
                case GLFW.GLFW_KEY_UP -> { nudge(e, 0, -step); return true; }
                case GLFW.GLFW_KEY_DOWN -> { nudge(e, 0, step); return true; }
                case GLFW.GLFW_KEY_R -> { GuiMove.reset(e); dirty = true; save(); return true; }
                case GLFW.GLFW_KEY_H -> {
                    if (e.kind == GuiMoveRegistry.Kind.HUD) { GuiMove.setHidden(e, !e.hidden()); dirty = true; save(); }
                    return true;
                }
                case GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_ADD -> { Rect r = GuiMove.rectOf(e, width, height); resize(e, 1, hasShiftDown(), r.cx(), r.cy()); return true; }
                case GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT -> { Rect r = GuiMove.rectOf(e, width, height); resize(e, -1, hasShiftDown(), r.cx(), r.cy()); return true; }
                default -> { }
            }
        }
        return super.keyPressed(key, scan, mods);
    }

    private void nudge(Element e, int dx, int dy) {
        Rect r = GuiMove.rectOf(e, width, height);
        GuiMove.moveTo(e, width, height, r.x() + dx, r.y() + dy);
        dirty = true;
    }

    @Override
    public boolean keyReleased(int key, int scan, int mods) {
        if (dirty && (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN)) save();
        return super.keyReleased(key, scan, mods);
    }

    private void cycle(int dir) {
        collect();
        if (cycle.isEmpty()) return;
        int i = selected == null ? (dir > 0 ? -1 : 0) : cycle.indexOf(selected);
        selected = cycle.get(Math.floorMod(i + dir, cycle.size()));
    }
}
