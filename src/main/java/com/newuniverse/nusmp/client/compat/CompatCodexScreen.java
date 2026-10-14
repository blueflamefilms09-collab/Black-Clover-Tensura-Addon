package com.newuniverse.nusmp.client.compat;

import com.newuniverse.nusmp.client.ui.UiDraw;
import com.newuniverse.nusmp.client.ui.theme.CodexSkin;
import com.newuniverse.nusmp.client.ui.theme.MagicCircle;
import com.newuniverse.nusmp.client.ui.theme.SageBlock;
import com.newuniverse.nusmp.client.ui.theme.ThemedButton;
import com.newuniverse.nusmp.client.ui.theme.UiLang;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.compat.CompatBook;
import com.newuniverse.nusmp.compat.CompatBookItem;
import com.newuniverse.nusmp.compat.CompatCodexModel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Compatibility Codex, opened by the {@code compat_codex} item: the Codex's open book (a parchment double page in the classic look, glass pages
 * with a turning magic circle and a Great-Sage status line in the anime look, config ui.theme) with the page turn of the Codex, arrow buttons, the
 * mouse wheel, the keys, and ESC to close. Each mod is a row with a live chip (Loaded / Not installed) read from the mod list once when the screen
 * opens. The text and the page rules are the pure {@link CompatCodexModel} / {@link CompatBook}; this class only measures, draws and reacts.
 * Everything here is client side; no packet.
 */
public class CompatCodexScreen extends Screen {
    private static final int FLIP_TICKS = 12, GAP = 5;
    private static final int GREEN_CLASSIC = 0xFF2E6B34;

    /** One measured item. */
    private record Laid(CompatCodexModel.Item item, List<FormattedCharSequence> lines, List<FormattedCharSequence> detail, int height) {}

    private final Screen parent;
    private final Map<String, Boolean> loaded = new HashMap<>();       // read from the mod list once per open
    private List<List<Laid>> pages = List.of(List.of());
    private int spread, prev = -1, dir;
    private float flipAge = -1f, time;
    private int bx, by, bw, bh, pw;
    private final SageBlock sage = new SageBlock();
    private ThemedButton prevBtn, nextBtn;
    private String footer = "", status = "";

    public CompatCodexScreen(Screen parent) {
        super(Component.translatable("nusmp.compat.codex.title"));
        this.parent = parent;
        for (var e : com.newuniverse.nusmp.compat.CompatCatalog.entries()) loaded.put(e.modId(), CompatBookItem.isLoaded(e.modId()));
    }

    private boolean isLoaded(String id) { return loaded.getOrDefault(id, false); }

    @Override
    protected void init() {
        bw = Math.min(width - 56, 440);
        bh = Math.min(height - 48, 260);
        bx = (width - bw) / 2 - 10;
        by = (height - bh - 20) / 2;
        pw = (innerW() - 16) / 2;
        layout();
        prevBtn = addRenderableWidget(new ThemedButton(bx + 4, by + bh + 4, 22, 16, Component.literal("<"), b -> go(spread - 1)));
        nextBtn = addRenderableWidget(new ThemedButton(bx + 30, by + bh + 4, 22, 16, Component.literal(">"), b -> go(spread + 1)));
        addRenderableWidget(new ThemedButton(bx + bw - 64, by + bh + 4, 60, 16, Component.translatable("nusmp.compat.codex.close"), b -> onClose()));
        spread = Mth.clamp(spread, 0, CompatCodexModel.spreads(pages.size()) - 1);
        int n = CompatCodexModel.loadedCount(this::isLoaded), total = com.newuniverse.nusmp.compat.CompatCatalog.entries().size();
        status = Component.translatable("nusmp.compat.codex.count", n, total).getString();
        footer = Component.translatable("nusmp.compat.codex.footer").getString();
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() { minecraft.setScreen(parent); }

    // ---------------------------------------------------------------- geometry and measuring
    private int innerX() { return bx + 20; }
    private int innerY() { return by + 22; }
    private int innerW() { return bw - 40; }
    private int innerH() { return bh - 62; }

    /** Measures every item at the current page width and lays them out in pages (once per init). */
    private void layout() {
        List<CompatCodexModel.Item> items = CompatCodexModel.items(this::isLoaded);
        IdentityHashMap<CompatCodexModel.Item, Laid> laid = new IdentityHashMap<>();
        int lh = font.lineHeight + 1;
        for (CompatCodexModel.Item it : items) {
            List<FormattedCharSequence> lines = new ArrayList<>(), detail = new ArrayList<>();
            int h;
            switch (it.type()) {
                case HEADING -> h = font.lineHeight + 5;
                case ENTRY -> {
                    for (String l : it.detail().split("\n")) if (!l.isBlank()) detail.addAll(font.split(Component.literal(l), pw - 6));
                    h = 12 + detail.size() * lh + 2;
                }
                default -> {
                    int hh = 0;
                    for (String l : it.text().split("\n", -1)) {
                        if (l.isBlank()) { lines.add(FormattedCharSequence.EMPTY); hh += 4; continue; }
                        List<FormattedCharSequence> w = font.split(Component.literal(l), pw);
                        lines.addAll(w);
                        hh += w.size() * lh;
                    }
                    h = hh;
                }
            }
            laid.put(it, new Laid(it, lines, detail, h));
        }
        List<List<CompatCodexModel.Item>> paged = CompatCodexModel.paginate(items, m -> laid.get(m).height(), innerH(), GAP);
        List<List<Laid>> out = new ArrayList<>();
        for (List<CompatCodexModel.Item> p : paged) {
            List<Laid> l = new ArrayList<>();
            for (CompatCodexModel.Item it : p) l.add(laid.get(it));
            out.add(l);
        }
        pages = out;
    }

    private int spreadCount() { return CompatCodexModel.spreads(pages.size()); }

    // ---------------------------------------------------------------- turning
    private void go(int target) {
        target = Mth.clamp(target, 0, spreadCount() - 1);
        if (target == spread || flipAge >= 0f) return;
        prev = spread;
        dir = target > spread ? 1 : -1;
        spread = target;
        flipAge = 0f;
    }

    @Override
    public void tick() {
        time += 1f;
        if (flipAge >= 0f) { flipAge += 1f; if (flipAge >= FLIP_TICKS) { flipAge = -1f; prev = -1; } }
        if (prevBtn != null) prevBtn.active = spread > 0;
        if (nextBtn != null) nextBtn.active = spread < spreadCount() - 1;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (sy == 0) return super.mouseScrolled(mx, my, sx, sy);
        go(spread + (sy < 0 ? 1 : -1));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        switch (key) {
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_PAGE_DOWN -> { go(spread + 1); return true; }
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_PAGE_UP -> { go(spread - 1); return true; }
            case GLFW.GLFW_KEY_HOME -> { go(0); return true; }
            case GLFW.GLFW_KEY_END -> { go(spreadCount() - 1); return true; }
            default -> { return super.keyPressed(key, scan, mods); }          // ESC closes
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (mx < bx + 14 && my > by + bh - 20) { go(spread - 1); return true; }                 // the page corners, as in the Codex
        if (mx > bx + bw - 14 && my > by + bh - 20) { go(spread + 1); return true; }
        return super.mouseClicked(mx, my, button);
    }

    // ---------------------------------------------------------------- drawing
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        if (UiTheme.anime()) {
            UiPanel.scrim(g, width, height, 1f);
            MagicCircle.draw(g, bx + bw / 2f, by + bh / 2f, Math.min(width, height) * 0.62f, UiTheme.now(), UiTheme.VIOLET, 0.22f, MagicCircle.CALM, 1f);
            return;
        }
        renderTransparentBackground(g);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        float now = time + partial;
        boolean anime = UiTheme.anime();
        drawBook(g);
        int x = innerX(), y = innerY(), w = innerW(), h = innerH(), mid = x + w / 2;
        if (flipAge < 0f) {
            half(g, spread, 0, 0f, mx, my, now);
            half(g, spread, 1, 0f, mx, my, now);
        } else {
            float t = Mth.clamp((flipAge + partial) / FLIP_TICKS, 0f, 1f);
            half(g, dir > 0 ? prev : spread, 0, 0f, mx, my, now);
            half(g, dir > 0 ? spread : prev, 1, 0f, mx, my, now);
            float s = t < 0.5f ? 1f - t * 2f : t * 2f - 1f;
            s = s * s * (3f - 2f * s);
            boolean oldLeaf = t < 0.5f;
            int leafSpread = oldLeaf ? prev : spread;
            int leafHalf = dir > 0 ? (oldLeaf ? 1 : 0) : (oldLeaf ? 0 : 1);
            g.pose().pushPose();
            g.pose().translate(mid, 0, 0);
            g.pose().scale(Math.max(0.01f, s), 1f, 1f);
            g.pose().translate(-mid, 0, 0);
            half(g, leafSpread, leafHalf, 1f, mx, my, now);
            g.pose().popPose();
            int sx0 = leafHalf == 0 ? Math.round(mid - (w / 2f) * s) : mid, sx1 = leafHalf == 0 ? mid : Math.round(mid + (w / 2f) * s);
            g.fill(sx0, y - 8, sx1, y + h + 8, UiDraw.withAlpha(0x000000, (int) (70 * (1f - s))));
        }
        drawFooter(g, now, anime);
    }

    /** The book's cover and furniture: the Codex's glass frame (anime) or leather, gilt line and parchment (classic). */
    private void drawBook(GuiGraphics g) {
        String title = getTitle().getString().toUpperCase(Locale.ROOT);
        if (UiTheme.anime()) {
            CodexSkin.frame(g, bx, by, bw, bh, UiTheme.now());
            CodexSkin.furniture(g, font, bx, by, bw, bh, title, spread, spreadCount());
            return;
        }
        g.fill(bx + 3, by + 4, bx + bw + 3, by + bh + 4, 0x66000000);
        g.fill(bx, by, bx + bw, by + bh, UiDraw.COVER);
        g.renderOutline(bx, by, bw, bh, UiDraw.COVER_EDGE);
        g.renderOutline(bx + 3, by + 3, bw - 6, bh - 6, UiDraw.GOLD);
        g.fill(bx + 6, by + 6, bx + bw - 6, by + bh - 6, UiDraw.PAPER);
        int spine = bx + bw / 2;
        g.fillGradient(spine - 10, by + 6, spine, by + bh - 6, 0x00000000, 0x30000000);
        g.fillGradient(spine, by + 6, spine + 10, by + bh - 6, 0x30000000, 0x00000000);
        g.fill(spine, by + 6, spine + 1, by + bh - 6, UiDraw.RULE);
        g.drawCenteredString(font, title, spine, by + 10, UiDraw.GOLD_INK);
        String num = (spread + 1) + " / " + spreadCount();
        g.drawString(font, num, bx + bw - 14 - font.width(num), by + bh - 14, UiDraw.INK_DIM, false);
        if (spread > 0) g.drawString(font, "<", bx + 10, by + bh - 14, UiDraw.INK_DIM, false);
        if (spread < spreadCount() - 1) g.drawString(font, ">", bx + bw - 14, by + bh - 14, UiDraw.INK_DIM, false);
    }

    /** Count line (the Great Sage in the anime look) and the plain statement that nothing here has been run in a game. */
    private void drawFooter(GuiGraphics g, float now, boolean anime) {
        int room = Math.max(80, bw - 120), fx = bx + 22;
        int y1 = by + bh - 36;
        if (anime) {
            sage.set(font, "", UiLang.get(UiLang.ANALYSIS_COMPLETE) + " " + status, "", room, now);
            sage.draw(g, font, fx, y1, now, 1f);
        } else {
            g.drawString(font, font.plainSubstrByWidth(status, room), fx, y1, UiDraw.ink(), false);
        }
        int y = by + bh - 25;
        for (FormattedCharSequence l : font.split(Component.literal(footer), room)) {
            g.drawString(font, l, fx, y, UiDraw.inkDim(), false);
            y += font.lineHeight + 1;
            if (y > by + bh - 6) break;
        }
    }

    /** One half of a spread (0 left, 1 right) on its own paper, clipped to the half. */
    private void half(GuiGraphics g, int sp, int which, float leaf, int mx, int my, float now) {
        int x = innerX(), w = innerW(), mid = x + w / 2;
        int hx0 = which == 0 ? bx + 6 : mid, hx1 = which == 0 ? mid : bx + bw - 6;
        g.enableScissor(hx0, by + 6, hx1, by + bh - 6);
        if (UiTheme.anime()) {
            CodexSkin.body(g, hx0, by + 6, hx1, by + bh - 6, mid, by + bh / 2f, (bh - 12) * 0.46f, now, leaf);
        } else {
            g.fill(hx0, by + 6, hx1, by + bh - 6, UiDraw.PAPER);
            int sh = Math.max(0, Math.min(255, (int) (leaf * 90)));
            g.fillGradient(which == 0 ? hx1 - 18 : hx0, by + 6, which == 0 ? hx1 : hx0 + 18, by + bh - 6,
                    which == 0 ? 0x00000000 : UiDraw.withAlpha(0x000000, 60 + sh / 3), which == 0 ? UiDraw.withAlpha(0x000000, 60 + sh / 3) : 0x00000000);
        }
        int pi = sp * 2 + which;
        if (pi < pages.size()) drawPage(g, pages.get(pi), which == 0 ? x : mid + 8, innerY(), pw);
        g.disableScissor();
    }

    private void drawPage(GuiGraphics g, List<Laid> page, int x, int y, int w) {
        for (Laid l : page) {
            CompatCodexModel.Item it = l.item();
            switch (it.type()) {
                case HEADING -> {
                    UiDraw.header(g, font, it.text(), x, y, w);
                    if (it.modId() != null) chip(g, x + w, y - 1, isLoaded(it.modId()));
                }
                case ENTRY -> {
                    g.drawString(font, font.plainSubstrByWidth(it.text(), w - 80), x, y, UiDraw.ink(), false);
                    chip(g, x + w, y - 1, isLoaded(it.modId()));
                    int dy = y + 12;
                    for (FormattedCharSequence d : l.detail()) { g.drawString(font, d, x + 3, dy, UiDraw.inkDim(), false); dy += font.lineHeight + 1; }
                }
                default -> {
                    int ty = y;
                    for (FormattedCharSequence t : l.lines()) {
                        if (t == FormattedCharSequence.EMPTY) { ty += 4; continue; }
                        g.drawString(font, t, x, ty, UiDraw.ink(), false);
                        ty += font.lineHeight + 1;
                    }
                }
            }
            y += l.height() + GAP;
        }
    }

    /** A status chip ending at {@code right}: green Loaded, grey Not installed. */
    private void chip(GuiGraphics g, int right, int y, boolean on) {
        String word = CompatBook.word(on);
        int cw = font.width(word) + 8, x = right - cw;
        boolean anime = UiTheme.anime();
        int rgb = on ? (anime ? UiTheme.GREEN : GREEN_CLASSIC) : (anime ? UiTheme.TEXT_DIM : UiDraw.INK_DIM);
        UiPanel.chip(g, x, y, cw, 11, on, false, rgb);
        g.drawString(font, word, x + 4, y + 2, on ? (anime ? UiTheme.GREEN : GREEN_CLASSIC) : UiDraw.inkDim(), false);
    }
}
