package com.newuniverse.nusmp.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * 0.100: the Codex, the mod's one custom screen (it replaces the 0.3x Multiverse Status screen; its blocks live on as the Status page).
 * An open book: a spread of two pages, tabs on the right edge, a page turn between spreads, mouse-wheel scrolling, and on the first
 * spread the player's own figure in a random pose with the magic this mod has given them.
 * <ul>
 *   <li><b>Page turn:</b> the leaf folds over the spine, the outgoing half narrowing to nothing and the incoming half opening out
 *       under it, with a shadow on the moving leaf.</li>
 *   <li><b>Wheel:</b> scrolls a long page; on a page that fits (or with Shift held) it turns the page.</li>
 *   <li><b>Keys:</b> Left / Right or A / D turn pages, Up / Down scroll, R changes the pose.</li>
 * </ul>
 * Content is a {@link CodexPage} from {@link CodexPages}, so the book grows by registering pages, not by editing this screen.
 */
public class CodexScreen extends Screen {
    private static final int FLIP_TICKS = 12;

    private final Screen parent;
    private final CodexContext ctx = new CodexContext(this);
    private int index;
    private int prev = -1, dir;                 // the spread being turned away from, and the direction (+1 forward)
    private float flipAge = -1f;                // ticks since the turn began; < 0 = not turning
    private float time, scrollCur, scrollTarget;
    private int nextPoseAt = 160;
    private boolean dragging;
    private long lastNs;
    private int bx, by, bw, bh;                 // the book

    public CodexScreen(Screen parent) {
        super(Component.literal("Codex"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        bw = Math.min(width - 56, 440);
        bh = Math.min(height - 48, 260);
        bx = (width - bw) / 2 - 10;
        by = (height - bh - 20) / 2;
        if (UiPoses.ALL.size() > 0 && time == 0f) UiPoses.roll();
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("Grimoire Slot"), b -> com.newuniverse.nusmp.client.grimoire.GrimoireSlotClient.open())
                .bounds(bx + bw - 94, by + bh + 4, 88, 16).build());
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) { renderTransparentBackground(g); }

    @Override public void onClose() { minecraft.setScreen(parent); }

    @Override
    public void tick() {
        time += 1f;
        if (flipAge >= 0f) { flipAge += 1f; if (flipAge >= FLIP_TICKS) { flipAge = -1f; prev = -1; } }
        if (time >= nextPoseAt && index == 0) { UiPoses.roll(); nextPoseAt = (int) time + 140 + (int) (Math.random() * 120); }
    }

    // ---------------------------------------------------------------- geometry
    private int innerX() { return bx + 20; }
    private int innerY() { return by + 22; }
    private int innerW() { return bw - 40; }
    private int innerH() { return bh - 40; }

    private List<CodexPage> pages() { return CodexPages.all(); }

    private int maxScroll(int page) {
        return Math.max(0, pages().get(page).contentHeight(ctx, innerW()) - innerH());
    }

    // ---------------------------------------------------------------- turning
    private void go(int target) {
        int n = pages().size();
        target = Mth.clamp(target, 0, n - 1);
        if (target == index || flipAge >= 0f) return;
        prev = index;
        dir = target > index ? 1 : -1;
        index = target;
        flipAge = 0f;
        scrollCur = scrollTarget = 0f;
    }

    // ---------------------------------------------------------------- input
    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (sy == 0) return super.mouseScrolled(mx, my, sx, sy);
        int max = maxScroll(index);
        if (max > 0 && !hasShiftDown()) {
            scrollTarget = Mth.clamp(scrollTarget - (float) sy * 22f, 0f, max);
            return true;
        }
        go(index + (sy < 0 ? 1 : -1));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        int max = maxScroll(index);
        switch (key) {
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_D -> { go(index + 1); return true; }
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_A -> { go(index - 1); return true; }
            case GLFW.GLFW_KEY_DOWN -> { scrollTarget = Mth.clamp(scrollTarget + 22f, 0f, max); return true; }
            case GLFW.GLFW_KEY_UP -> { scrollTarget = Mth.clamp(scrollTarget - 22f, 0f, max); return true; }
            case GLFW.GLFW_KEY_HOME -> { scrollTarget = 0f; return true; }
            case GLFW.GLFW_KEY_END -> { scrollTarget = max; return true; }
            case GLFW.GLFW_KEY_R -> { UiPoses.roll(); return true; }
            default -> { return super.keyPressed(key, scan, mods); }
        }
    }

    private int tabAt(double mx, double my) {
        int tx = bx + bw, n = pages().size();
        for (int i = 0; i < n; i++) {
            int ty = by + 14 + i * 26;
            if (mx >= tx - 2 && mx <= tx + 22 && my >= ty && my <= ty + 24) return i;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int t = tabAt(mx, my);
        if (t >= 0) { go(t); return true; }
        if (flipAge < 0f && mx >= innerX() && mx <= innerX() + innerW() && my >= innerY() && my <= innerY() + innerH()) {
            if (pages().get(index).click(ctx, mx, my, button, innerX(), innerY(), innerW(), innerH())) { dragging = index == 0; return true; }
        }
        if (mx < bx + 14 && my > by + bh - 20) { go(index - 1); return true; }                 // the page corners
        if (mx > bx + bw - 14 && my > by + bh - 20) { go(index + 1); return true; }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging && index == 0) { ctx.figureYaw += (float) dx * 1.6f; return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    // ---------------------------------------------------------------- drawing
    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);                                                          // background and the Grimoire Slot button
        float now = time + partial;
        long ns = System.nanoTime();
        float dt = lastNs == 0 ? 0f : Math.min(3f, (ns - lastNs) / 50_000_000f);                // ticks since the last frame
        lastNs = ns;
        UiPoses.step(dt, now);
        scrollCur += (scrollTarget - scrollCur) * 0.35f;
        if (Math.abs(scrollTarget - scrollCur) < 0.3f) scrollCur = scrollTarget;

        drawBook(g, mx, my);
        drawTabs(g, mx, my);

        int x = innerX(), y = innerY(), w = innerW(), h = innerH(), spine = x + w / 2;
        CodexPage cur = pages().get(index);
        if (flipAge < 0f) {
            cur.render(ctx, g, x, y, w, h, Math.round(scrollCur), mx, my, now);
            drawScrollbar(g, x + w + 6, y, h);
        } else {
            float t = Mth.clamp((flipAge + partial) / FLIP_TICKS, 0f, 1f);
            CodexPage old = pages().get(prev);
            // base halves: forward: old left + new right; backward: new left + old right
            half(g, dir > 0 ? old : cur, 0, x, y, w, h, 0f, mx, my, now);
            half(g, dir > 0 ? cur : old, 1, x, y, w, h, 0f, mx, my, now);
            float s = t < 0.5f ? 1f - t * 2f : t * 2f - 1f;                 // the leaf's width: closes, then opens on the other side
            s = s * s * (3f - 2f * s);
            boolean oldLeaf = t < 0.5f;
            CodexPage leafPage = oldLeaf ? old : cur;
            int leafHalf = dir > 0 ? (oldLeaf ? 1 : 0) : (oldLeaf ? 0 : 1);
            g.pose().pushPose();
            g.pose().translate(spine, 0, 0);
            g.pose().scale(Math.max(0.01f, s), 1f, 1f);
            g.pose().translate(-spine, 0, 0);
            half(g, leafPage, leafHalf, x, y, w, h, 1f, mx, my, now);
            g.pose().popPose();
            int sx0 = leafHalf == 0 ? Math.round(spine - (w / 2f) * s) : spine, sx1 = leafHalf == 0 ? spine : Math.round(spine + (w / 2f) * s);
            g.fillGradient(sx0, y - 8, sx1, y + h + 8, UiDraw.withAlpha(0x000000, (int) (70 * (1f - s))), UiDraw.withAlpha(0x000000, (int) (70 * (1f - s))));
        }
    }

    /** One half of a spread (0 left, 1 right), on its own paper, clipped to the half. */
    private void half(GuiGraphics g, CodexPage page, int which, int x, int y, int w, int h, float leaf, int mx, int my, float now) {
        int spine = x + w / 2;
        int hx0 = which == 0 ? bx + 6 : spine, hx1 = which == 0 ? spine : bx + bw - 6;
        g.enableScissor(hx0, by + 6, hx1, by + bh - 6);
        g.fill(hx0, by + 6, hx1, by + bh - 6, UiDraw.PAPER);
        int sh = Math.max(0, Math.min(255, (int) (leaf * 90)));
        g.fillGradient(which == 0 ? hx1 - 18 : hx0, by + 6, which == 0 ? hx1 : hx0 + 18, by + bh - 6,
                which == 0 ? 0x00000000 : UiDraw.withAlpha(0x000000, 60 + sh / 3), which == 0 ? UiDraw.withAlpha(0x000000, 60 + sh / 3) : 0x00000000);
        int idx = pages().indexOf(page);
        page.render(ctx, g, x, y, w, h, idx == index ? Math.round(scrollCur) : 0, -1000, -1000, now);
        g.disableScissor();
    }

    private void drawBook(GuiGraphics g, int mx, int my) {
        g.fill(bx + 3, by + 4, bx + bw + 3, by + bh + 4, 0x66000000);                              // drop shadow
        g.fill(bx, by, bx + bw, by + bh, UiDraw.COVER);
        g.renderOutline(bx, by, bw, bh, UiDraw.COVER_EDGE);
        g.renderOutline(bx + 3, by + 3, bw - 6, bh - 6, UiDraw.GOLD);                            // the gilt line
        g.fill(bx + 6, by + 6, bx + bw - 6, by + bh - 6, UiDraw.PAPER);
        int spine = bx + bw / 2;
        g.fillGradient(spine - 10, by + 6, spine, by + bh - 6, 0x00000000, 0x30000000);
        g.fillGradient(spine, by + 6, spine + 10, by + bh - 6, 0x30000000, 0x00000000);
        g.fill(spine, by + 6, spine + 1, by + bh - 6, UiDraw.RULE);
        String title = pages().get(index).title().toUpperCase(java.util.Locale.ROOT);
        g.drawCenteredString(font, title, spine, by + 10, UiDraw.GOLD_INK);
        String num = (index + 1) + " / " + pages().size();
        g.drawString(font, num, bx + bw - 14 - font.width(num), by + bh - 14, UiDraw.INK_DIM, false);
        if (index > 0) g.drawString(font, "<", bx + 10, by + bh - 14, UiDraw.INK_DIM, false);
        if (index < pages().size() - 1) g.drawString(font, ">", bx + bw - 14, by + bh - 14, UiDraw.INK_DIM, false);
    }

    private void drawTabs(GuiGraphics g, int mx, int my) {
        int tx = bx + bw;
        int hover = tabAt(mx, my);
        for (int i = 0; i < pages().size(); i++) {
            int ty = by + 14 + i * 26;
            boolean on = i == index;
            int ext = on ? 22 : (i == hover ? 18 : 14);
            g.fill(tx - 2, ty, tx + ext, ty + 24, on ? UiDraw.GOLD : UiDraw.COVER);
            g.renderOutline(tx - 2, ty, ext + 2, 24, UiDraw.COVER_EDGE);
            g.drawCenteredString(font, pages().get(i).glyph(), tx + (ext - 2) / 2 + 1, ty + 8, on ? UiDraw.COVER : UiDraw.GOLD);
            if (i == hover) g.renderTooltip(font, Component.literal(pages().get(i).title()), mx, my);
        }
    }

    private void drawScrollbar(GuiGraphics g, int x, int y, int h) {
        int max = maxScroll(index);
        if (max <= 0) return;
        int total = max + innerH();
        int bar = Math.max(14, h * innerH() / total);
        int pos = (int) ((h - bar) * (scrollCur / max));
        g.fill(x, y, x + 3, y + h, UiDraw.withAlpha(UiDraw.RULE, 120));
        g.fill(x, y + pos, x + 3, y + pos + bar, UiDraw.GOLD_INK);
    }
}
