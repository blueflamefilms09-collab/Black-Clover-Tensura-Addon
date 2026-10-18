package com.newuniverse.nusmp.client.elite.records;

import com.newuniverse.nusmp.client.ui.theme.ThemedButton;
import com.newuniverse.nusmp.elite.records.RecordsNet;
import com.newuniverse.nusmp.elite.records.RecordsNet.Prog;
import com.newuniverse.nusmp.elite.records.RecordsNet.RecordInfo;
import com.newuniverse.nusmp.elite.records.RecordsNet.SynergyInfo;
import com.newuniverse.nusmp.elite.records.RecordsNet.TitleInfo;
import com.newuniverse.nusmp.elite.records.RecordsText;
import com.newuniverse.nusmp.elite.records.rule.Category;
import com.newuniverse.nusmp.elite.records.rule.Describe;
import com.newuniverse.nusmp.elite.records.rule.Rarity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Records screen the owner asked for: client-side, driven by the mouse, with the categories as tabs, rarity colours and progress bars. Left: one tab per record category (with how many are
 * unlocked), then Titles and Synergies, and a filter button that cycles all / unlocked / locked. Right: the rows of the chosen tab in a scrolling list (mouse wheel, or drag the scroll bar). A record row
 * shows its rarity box (points), name, description and either the unlock date or a progress bar with the numbers; a record the source of which does not exist on this server says so instead of showing a
 * bar stuck at zero. A title row shows what it gives and what it needs (with how many requirements are met) and, once held, the button that makes it the one ACTIVE title (click the row again to remove
 * it). A synergy row shows whether it is in effect or how many of its titles are held. Hovering any row shows the full text. Everything shown is what the server last sent ({@link RecordsClientState});
 * the screen asks for a fresh state when it opens and every two seconds while open, and chooses the active title only by sending a request, never by deciding itself. The panel is drawn without
 * the blur background so the world stays readable. Client thread only.
 */
public final class RecordsScreen extends Screen {
    private static final int TAB_H = 15, TAB_STEP = 17, TAB_W = 98, ROW_RECORD = 32, ROW_TITLE = 46, ROW_SYNERGY = 42;
    private static final int TEXT = 0xFFE6E6F0, DIM = 0xFF8A8AA0, FAINT = 0xFF5A5A70, GREEN = 0xFF55E070, GOLD = 0xFFFFC040, RED = 0xFFFF6A6A, PANEL = 0xF0141420, EDGE = 0xFF3A3A58;
    private static int rememberedTab;

    private final Screen parent;
    private int tab = rememberedTab, filter;
    private double scroll;
    private int x0, y0, pw, ph, listX, listY, listW, listH, contentH, lastVersion = -1, ticks;
    private boolean dragging;
    private ThemedButton filterButton;

    public RecordsScreen(Screen parent) {
        super(RecordsText.tr(RecordsText.TITLE));
        this.parent = parent;
    }

    private static final int TABS = Category.values().length + 2;

    @Override
    protected void init() {
        pw = Math.min(width - 12, 450);
        ph = Math.min(height - 12, 276);
        x0 = (width - pw) / 2;
        y0 = (height - ph) / 2;
        listX = x0 + 6 + TAB_W + 6;
        listW = pw - (listX - x0) - 12;
        listY = y0 + 30;
        listH = ph - 30 - 24;
        addRenderableWidget(new ThemedButton(x0 + pw - 62, y0 + 5, 56, 16, RecordsText.tr(RecordsText.CLOSE), b -> onClose()));
        filterButton = addRenderableWidget(new ThemedButton(x0 + 6, y0 + ph - 22, TAB_W, 16, filterLabel(), b -> {
            filter = (filter + 1) % 3;
            scroll = 0;
            b.setMessage(filterLabel());
        }));
        lastVersion = -1;
    }

    private Component filterLabel() { return RecordsText.tr(filter == 0 ? RecordsText.FILTER_ALL : filter == 1 ? RecordsText.FILTER_UNLOCKED : RecordsText.FILTER_LOCKED); }

    @Override
    public void tick() {
        if (++ticks % 40 == 0) RecordsClient.request();
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        rememberedTab = tab;
        minecraft.setScreen(parent);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (RecordsClient.OPEN.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    // ------------------------------------------------------------------ rows
    private boolean passes(boolean got) { return filter == 0 || (filter == 1) == got; }

    private List<RecordInfo> recordRows() {
        List<RecordInfo> out = new ArrayList<>();
        Category c = Category.byOrdinal(tab);
        for (RecordInfo r : RecordsClientState.records()) {
            if (Category.byOrdinal(r.category()) == c && passes(RecordsClientState.unlocked(r.id()))) out.add(r);
        }
        return out;
    }

    private List<TitleInfo> titleRows() {
        List<TitleInfo> out = new ArrayList<>();
        for (TitleInfo t : RecordsClientState.titles()) if (passes(RecordsClientState.held(t.id()))) out.add(t);
        return out;
    }

    private List<SynergyInfo> synergyRows() {
        List<SynergyInfo> out = new ArrayList<>();
        for (SynergyInfo s : RecordsClientState.synergies()) if (passes(RecordsClientState.synergyActive(s.id()))) out.add(s);
        return out;
    }

    private int rowHeight() { return tab < Category.values().length ? ROW_RECORD : tab == Category.values().length ? ROW_TITLE : ROW_SYNERGY; }

    private int rowCount() {
        if (tab < Category.values().length) return recordRows().size();
        return tab == Category.values().length ? titleRows().size() : synergyRows().size();
    }

    private double maxScroll() { return Math.max(0, rowCount() * rowHeight() - listH); }

    // ------------------------------------------------------------------ drawing
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0xB0000008);
        g.fill(x0, y0, x0 + pw, y0 + ph, PANEL);
        g.renderOutline(x0, y0, pw, ph, EDGE);
        g.fill(x0 + 6 + TAB_W + 2, y0 + 28, x0 + 6 + TAB_W + 3, y0 + ph - 8, EDGE);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        if (lastVersion != RecordsClientState.version()) {
            lastVersion = RecordsClientState.version();
            scroll = Math.max(0, Math.min(scroll, maxScroll()));
        }
        super.render(g, mx, my, pt);
        g.drawString(font, RecordsText.tr(RecordsText.TITLE), x0 + 8, y0 + 9, TEXT, false);
        if (RecordsClientState.haveState()) {
            String sum = RecordsText.str(RecordsText.SUMMARY, RecordsClientState.unlockedCount(), RecordsClientState.points(), RecordsClientState.totalPoints());
            g.drawString(font, fit(sum, pw - 190), x0 + 130, y0 + 9, DIM, false);
        }
        drawTabs(g, mx, my);
        if (!RecordsClientState.haveState()) {
            String msg = RecordsText.str(ticks > 100 ? RecordsText.OFF : RecordsText.WAITING);
            g.drawCenteredString(font, msg, listX + listW / 2, listY + listH / 2, DIM);
            return;
        }
        List<Component> tip = drawList(g, mx, my);
        drawFooter(g, mx, my);
        if (tip != null) g.renderComponentTooltip(font, tip, mx, my);
    }

    private void drawTabs(GuiGraphics g, int mx, int my) {
        for (int i = 0; i < TABS; i++) {
            int x = x0 + 6, y = y0 + 28 + i * TAB_STEP;
            boolean sel = i == tab, hov = mx >= x && mx < x + TAB_W && my >= y && my < y + TAB_H;
            g.fill(x, y, x + TAB_W, y + TAB_H, sel ? 0xFF2A2A48 : hov ? 0xFF22223A : 0xFF191928);
            if (sel) g.fill(x, y, x + 2, y + TAB_H, GOLD);
            String label;
            String count;
            if (i < Category.values().length) {
                Category c = Category.byOrdinal(i);
                int total = 0, got = 0;
                for (RecordInfo r : RecordsClientState.records()) {
                    if (Category.byOrdinal(r.category()) != c) continue;
                    total++;
                    if (RecordsClientState.unlocked(r.id())) got++;
                }
                label = RecordsText.str("gui.nusmp.records.category." + c.id());
                count = got + "/" + total;
            } else if (i == Category.values().length) {
                label = RecordsText.str(RecordsText.TAB_TITLES);
                count = RecordsClientState.heldCount() + "/" + RecordsClientState.titles().size();
            } else {
                label = RecordsText.str(RecordsText.TAB_SYNERGIES);
                int on = 0;
                for (SynergyInfo s : RecordsClientState.synergies()) if (RecordsClientState.synergyActive(s.id())) on++;
                count = on + "/" + RecordsClientState.synergies().size();
            }
            g.drawString(font, label, x + 6, y + 4, sel ? TEXT : DIM, false);
            g.drawString(font, count, x + TAB_W - 4 - font.width(count), y + 4, FAINT, false);
        }
    }

    /** Draws the visible rows; returns the tooltip of the row under the mouse, or null. */
    private List<Component> drawList(GuiGraphics g, int mx, int my) {
        int rh = rowHeight(), n = rowCount();
        contentH = n * rh;
        if (n == 0) {
            g.drawCenteredString(font, RecordsText.str(RecordsText.EMPTY), listX + listW / 2, listY + 20, DIM);
            return null;
        }
        List<Component> tip = null;
        int first = (int) (scroll / rh), last = Math.min(n, (int) ((scroll + listH) / rh) + 1);
        g.enableScissor(listX, listY, listX + listW, listY + listH);
        for (int i = first; i < last; i++) {
            int y = listY + (int) (i * rh - scroll);
            boolean hover = mx >= listX && mx < listX + listW && my >= Math.max(y, listY) && my < Math.min(y + rh, listY + listH);
            if (tab < Category.values().length) {
                RecordInfo r = recordRows().get(i);
                drawRecord(g, r, listX, y, listW, hover);
                if (hover) tip = recordTip(r);
            } else if (tab == Category.values().length) {
                TitleInfo t = titleRows().get(i);
                drawTitle(g, t, listX, y, listW, hover, mx, my);
                if (hover) tip = titleTip(t);
            } else {
                SynergyInfo s = synergyRows().get(i);
                drawSynergy(g, s, listX, y, listW, hover);
                if (hover) tip = synergyTip(s);
            }
        }
        g.disableScissor();
        double max = maxScroll();
        if (max > 0) {
            int tx = listX + listW + 3, th = Math.max(12, (int) (listH * (listH / (double) contentH)));
            int ty = listY + (int) ((listH - th) * (scroll / max));
            g.fill(tx, listY, tx + 3, listY + listH, 0xFF1C1C2C);
            g.fill(tx, ty, tx + 3, ty + th, dragging ? GOLD : 0xFF6A6A88);
        }
        return tip;
    }

    private void drawFooter(GuiGraphics g, int mx, int my) {
        int y = y0 + ph - 18;
        TitleInfo act = RecordsClientState.title(RecordsClientState.active());
        String left = act == null ? RecordsText.str(RecordsText.NO_ACTIVE_TITLE) : RecordsText.str(RecordsText.ACTIVE_TITLE, act.name());
        g.drawString(font, fit(left, 150), listX, y + 4, act == null ? FAINT : 0xFF000000 | Rarity.byOrdinal(act.rarity()).color, false);
        List<String> lines = RecordsClientState.bonusLines();
        if (!lines.isEmpty()) {
            String s = RecordsText.str(RecordsText.BONUSES_NOW) + ": " + String.join(", ", lines);
            int bx = listX + 158, bw = listX + listW - bx;
            g.drawString(font, fit(s, bw), bx, y + 4, DIM, false);
            if (mx >= bx && mx < bx + bw && my >= y && my < y + 14) {
                List<Component> tip = new ArrayList<>();
                tip.add(RecordsText.tr(RecordsText.BONUSES_NOW).withStyle(net.minecraft.ChatFormatting.GOLD));
                for (String l : lines) tip.add(Component.literal(l));
                g.renderComponentTooltip(font, tip, mx, my);
            }
        }
    }

    private void drawRecord(GuiGraphics g, RecordInfo r, int x, int y, int w, boolean hover) {
        Rarity ra = Rarity.byOrdinal(r.rarity());
        int col = 0xFF000000 | ra.color;
        boolean got = RecordsClientState.unlocked(r.id());
        g.fill(x, y, x + w, y + ROW_RECORD - 2, hover ? 0x40FFFFFF : 0x28FFFFFF);
        g.fill(x + 3, y + 3, x + 27, y + 27, got ? col : (col & 0x00FFFFFF) | 0x50000000);
        g.fill(x + 4, y + 4, x + 26, y + 26, 0xFF14141E);
        String pts = String.valueOf(ra.points);
        g.drawString(font, pts, x + 15 - font.width(pts) / 2, y + 11, got ? col : FAINT, false);
        g.drawString(font, fit(r.name(), w - 100), x + 32, y + 3, got ? col : 0xFFC4C4D4, false);
        String rl = RecordsText.str("gui.nusmp.records.rarity." + ra.id());
        g.drawString(font, rl, x + w - 4 - font.width(rl), y + 3, got ? col : FAINT, false);
        g.drawString(font, fit(r.description(), w - 38), x + 32, y + 13, DIM, false);
        if (got) {
            g.drawString(font, RecordsText.str(RecordsText.UNLOCKED_ON, date(RecordsClientState.unlockedAt(r.id()))), x + 32, y + 22, GREEN, false);
            return;
        }
        Prog p = RecordsClientState.recordProgress(r.id());
        if (p == null) return;
        if (p.state() == 2) {
            g.drawString(font, RecordsText.str(RecordsText.UNAVAILABLE), x + 32, y + 22, RED, false);
            return;
        }
        int bx = x + 32, bw = w - 32 - 84;
        g.fill(bx, y + 24, bx + bw, y + 28, 0xFF0C0C14);
        g.fill(bx, y + 24, bx + (int) Math.round(bw * p.fraction()), y + 28, col);
        String num = Describe.num(Math.floor(p.current())) + " / " + Describe.num(p.target());
        g.drawString(font, num, x + w - 4 - font.width(num), y + 21, DIM, false);
    }

    private void drawTitle(GuiGraphics g, TitleInfo t, int x, int y, int w, boolean hover, int mx, int my) {
        Rarity ra = Rarity.byOrdinal(t.rarity());
        int col = 0xFF000000 | ra.color;
        boolean held = RecordsClientState.held(t.id()), act = t.id().equals(RecordsClientState.active());
        g.fill(x, y, x + w, y + ROW_TITLE - 2, act ? 0x40FFC040 : hover ? 0x40FFFFFF : 0x28FFFFFF);
        g.fill(x + 3, y + 3, x + 31, y + 31, held ? col : (col & 0x00FFFFFF) | 0x50000000);
        g.fill(x + 4, y + 4, x + 30, y + 30, 0xFF14141E);
        String ini = t.name().isEmpty() ? "?" : t.name().substring(0, 1);
        g.drawString(font, ini, x + 17 - font.width(ini) / 2, y + 13, held ? col : FAINT, false);
        g.drawString(font, fit(t.name(), w - 150), x + 36, y + 3, held ? col : 0xFFC4C4D4, false);
        String badge = act ? RecordsText.str(RecordsText.ACTIVE_TAG) : held ? RecordsText.str(RecordsText.HELD) : RecordsText.str("gui.nusmp.records.rarity." + ra.id());
        g.drawString(font, badge, x + w - 4 - font.width(badge), y + 3, act ? GOLD : held ? GREEN : FAINT, false);
        g.drawString(font, fit(RecordsText.str(RecordsText.GIVES) + " " + String.join(", ", t.bonusLines()), w - 42), x + 36, y + 14, DIM, false);
        if (held) {
            g.drawString(font, fit(String.join(" ", t.tags()), w - 120), x + 36, y + 26, FAINT, false);
            int bx = x + w - 74, by = y + 25;
            boolean over = mx >= bx && mx < bx + 70 && my >= by && my < by + 14;
            g.fill(bx, by, bx + 70, by + 14, over ? 0xFF3A3A60 : 0xFF26263C);
            g.renderOutline(bx, by, 70, 14, act ? GOLD : EDGE);
            String bl = RecordsText.str(act ? RecordsText.CLEAR_ACTIVE : RecordsText.SET_ACTIVE);
            g.drawString(font, bl, bx + 35 - font.width(bl) / 2, by + 3, act ? GOLD : TEXT, false);
        } else {
            List<Prog> prog = RecordsClientState.titleProgress(t.id());
            int met = 0;
            for (Prog p : prog) if (p.state() == 0) met++;
            g.drawString(font, fit(RecordsText.str(RecordsText.REQUIRES) + " " + String.join("; ", t.requirements()), w - 42), x + 36, y + 25, DIM, false);
            int bx = x + 36, bw = w - 36 - 8;
            g.fill(bx, y + 37, bx + bw, y + 40, 0xFF0C0C14);
            double f = 0;
            for (Prog p : prog) f += p.fraction();
            f = prog.isEmpty() ? 0 : f / prog.size();
            g.fill(bx, y + 37, bx + (int) Math.round(bw * f), y + 40, col);
        }
    }

    private void drawSynergy(GuiGraphics g, SynergyInfo s, int x, int y, int w, boolean hover) {
        boolean on = RecordsClientState.synergyActive(s.id());
        int[] p = RecordsClientState.synergyProgress(s.id());
        int col = on ? GOLD : 0xFFC4C4D4;
        g.fill(x, y, x + w, y + ROW_SYNERGY - 2, on ? 0x40FFC040 : hover ? 0x40FFFFFF : 0x28FFFFFF);
        g.drawString(font, fit(s.name(), w - 110), x + 6, y + 3, col, false);
        String badge = on ? RecordsText.str(RecordsText.SYN_ACTIVE) : RecordsText.str(RecordsText.SYN_PROGRESS, p[0], p[1]);
        g.drawString(font, badge, x + w - 4 - font.width(badge), y + 3, on ? GOLD : FAINT, false);
        g.drawString(font, fit(needs(s), w - 12), x + 6, y + 14, DIM, false);
        g.drawString(font, fit(RecordsText.str(RecordsText.GIVES) + " " + String.join(", ", s.bonusLines()), w - 12), x + 6, y + 24, on ? TEXT : FAINT, false);
        int bw = w - 12;
        g.fill(x + 6, y + 35, x + 6 + bw, y + 38, 0xFF0C0C14);
        g.fill(x + 6, y + 35, x + 6 + (p[1] <= 0 ? 0 : (int) Math.round(bw * Math.min(1.0, p[0] / (double) p[1]))), y + 38, on ? GOLD : 0xFF6A6A88);
    }

    private static String needs(SynergyInfo s) {
        List<String> parts = new ArrayList<>();
        if (!s.requires().isEmpty()) parts.add(RecordsText.str(RecordsText.SYN_NEEDS_TITLES, String.join(", ", s.requires())));
        if (!s.tag().isEmpty() && s.tagCount() > 0) {
            parts.add(s.tag().equals("*") ? RecordsText.str(RecordsText.SYN_NEEDS_ANY, s.tagCount()) : RecordsText.str(RecordsText.SYN_NEEDS_TAG, s.tagCount(), s.tag()));
        }
        return String.join("; ", parts);
    }

    // ------------------------------------------------------------------ tooltips
    private List<Component> recordTip(RecordInfo r) {
        Rarity ra = Rarity.byOrdinal(r.rarity());
        List<Component> t = new ArrayList<>();
        t.add(Component.literal(r.name()).withStyle(s -> s.withColor(ra.color)));
        t.add(Component.literal(RecordsText.str("gui.nusmp.records.rarity." + ra.id()) + " - " + RecordsText.str("gui.nusmp.records.category." + Category.byOrdinal(r.category()).id())
                + " - " + RecordsText.str(RecordsText.POINTS, ra.points)).withStyle(net.minecraft.ChatFormatting.GRAY));
        for (String l : wrap(r.description(), 230)) t.add(Component.literal(l));
        Prog p = RecordsClientState.recordProgress(r.id());
        if (p != null && p.state() == 1) t.add(Component.literal(Describe.num(Math.floor(p.current())) + " / " + Describe.num(p.target())).withStyle(net.minecraft.ChatFormatting.AQUA));
        return t;
    }

    private List<Component> titleTip(TitleInfo ti) {
        Rarity ra = Rarity.byOrdinal(ti.rarity());
        List<Component> t = new ArrayList<>();
        t.add(Component.literal(ti.name()).withStyle(s -> s.withColor(ra.color)));
        t.add(Component.literal(RecordsText.str("gui.nusmp.records.rarity." + ra.id()) + (ti.tags().isEmpty() ? "" : " - " + String.join(" ", ti.tags()))).withStyle(net.minecraft.ChatFormatting.GRAY));
        for (String l : wrap(ti.description(), 230)) t.add(Component.literal(l));
        t.add(RecordsText.tr(RecordsText.REQUIRES).withStyle(net.minecraft.ChatFormatting.GOLD));
        List<Prog> prog = RecordsClientState.titleProgress(ti.id());
        for (int i = 0; i < ti.requirements().size(); i++) {
            Prog p = i < prog.size() ? prog.get(i) : null;
            boolean ok = RecordsClientState.held(ti.id()) || (p != null && p.state() == 0);
            String tail = p == null || ok || p.state() == 2 ? (p != null && p.state() == 2 ? "  (" + RecordsText.str(RecordsText.UNAVAILABLE) + ")" : "") : "  (" + Describe.num(Math.floor(p.current())) + " / " + Describe.num(p.target()) + ")";
            t.add(Component.literal((ok ? "[x] " : "[ ] ") + ti.requirements().get(i) + tail).withStyle(ok ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.GRAY));
        }
        t.add(RecordsText.tr(RecordsText.GIVES).withStyle(net.minecraft.ChatFormatting.GOLD));
        for (String l : ti.bonusLines()) t.add(Component.literal(" " + l));
        return t;
    }

    private List<Component> synergyTip(SynergyInfo s) {
        List<Component> t = new ArrayList<>();
        t.add(Component.literal(s.name()).withStyle(net.minecraft.ChatFormatting.GOLD));
        for (String l : wrap(s.description(), 230)) t.add(Component.literal(l));
        t.add(Component.literal(needs(s)).withStyle(net.minecraft.ChatFormatting.GRAY));
        t.add(RecordsText.tr(RecordsText.GIVES).withStyle(net.minecraft.ChatFormatting.GOLD));
        for (String l : s.bonusLines()) t.add(Component.literal(" " + l));
        return t;
    }

    // ------------------------------------------------------------------ mouse
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (int i = 0; i < TABS; i++) {
                int x = x0 + 6, y = y0 + 28 + i * TAB_STEP;
                if (mx >= x && mx < x + TAB_W && my >= y && my < y + TAB_H) {
                    tab = i;
                    rememberedTab = i;
                    scroll = 0;
                    return true;
                }
            }
            if (maxScroll() > 0 && mx >= listX + listW + 1 && mx < listX + listW + 8 && my >= listY && my < listY + listH) {
                dragging = true;
                dragTo(my);
                return true;
            }
            if (tab == Category.values().length && mx >= listX && mx < listX + listW && my >= listY && my < listY + listH && RecordsClientState.haveState()) {
                int idx = (int) ((my - listY + scroll) / ROW_TITLE);
                List<TitleInfo> rows = titleRows();
                if (idx >= 0 && idx < rows.size() && RecordsClientState.held(rows.get(idx).id())) {
                    String id = rows.get(idx).id();
                    PacketDistributor.sendToServer(new RecordsNet.SetActive(id.equals(RecordsClientState.active()) ? "" : id));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private void dragTo(double my) {
        double th = Math.max(12, listH * (listH / (double) Math.max(1, contentH)));
        double f = (my - listY - th / 2) / Math.max(1, listH - th);
        scroll = Math.max(0, Math.min(maxScroll(), f * maxScroll()));
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            dragTo(my);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - sy * 20));
        return true;
    }

    // ------------------------------------------------------------------ helpers
    private String fit(String s, int width) {
        if (width <= 0) return "";
        if (font.width(s) <= width) return s;
        return font.plainSubstrByWidth(s, Math.max(0, width - font.width("..."))) + "...";
    }

    private List<String> wrap(String text, int width) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() > 0 && font.width(line + " " + word) > width) {
                out.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }

    private static String date(long unixSec) {
        if (unixSec <= 0) return "-";
        try {
            return LocalDate.ofInstant(Instant.ofEpochSecond(unixSec), ZoneId.systemDefault()).toString();
        } catch (RuntimeException e) {
            return "-";
        }
    }
}
