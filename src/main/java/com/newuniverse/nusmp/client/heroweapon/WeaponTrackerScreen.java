package com.newuniverse.nusmp.client.heroweapon;

import com.newuniverse.nusmp.client.ui.theme.ThemedButton;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.heroweapon.HeroClass;
import com.newuniverse.nusmp.heroweapon.tracker.TrackerLimiter;
import com.newuniverse.nusmp.heroweapon.tracker.TrackerRules;
import com.newuniverse.nusmp.heroweapon.tracker.TrackerRules.Filter;
import com.newuniverse.nusmp.heroweapon.tracker.TrackerRules.Row;
import com.newuniverse.nusmp.heroweapon.tracker.TrackerRules.Status;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Why: the Weapon Tracker screen. It draws the table the server sent ({@link TrackerRules.Table}: one row per hero weapon with its status) and decides nothing: the chips filter the rows already in hand, the scroll
 * moves a window of seven rows over them, and Refresh asks the server again (the server answers at most once per two seconds, so the button waits as long). The player's own weapon is outlined in gold.
 * The screen asks once when it opens by key and then every ten seconds while it stays open; a reply that arrives while it is open replaces the table. Keyboard: Left / Right (or 1 to 7) pick a chip, Up / Down / Page
 * Up / Page Down / Home / End and the wheel scroll, R or F5 refreshes, Esc closes; the two buttons are reachable with Tab. Drawn in the anime theme with the toolkit ({@link UiPanel}, {@link ThemedButton}) in
 * {@code renderBackground} (never the blurring vanilla background), a modal window that is scaled by the GUI Size framework. Cost per frame: at most 7 rows of fills and strings (a few short strings per visible row; the
 * table itself is never copied per frame).
 */
public final class WeaponTrackerScreen extends Screen {
    private static final int W = 360, H = 252, ROW = 22, VISIBLE = 7, PAD = 14, AUTO_REFRESH_TICKS = 200, NO_ANSWER_TICKS = 100;
    private static final String[] CHIP_KEYS = {"all", "legendary", "seven_star", "vassal", "free", "taken", "yours"};
    private static final String[] CHIP_EN = {"All", "Legendary", "Seven Star", "Vassal", "Free", "Taken", "Yours"};

    private TrackerRules.Table table;
    private Filter filter = Filter.ALL;
    private List<Row> shown = List.of();
    private int scroll;
    private int ticks, sinceAsk, cooldown;
    private ThemedButton refresh;
    private final int[] chipX = new int[7], chipW = new int[7];

    /** {@code table} is null when the screen was opened by the key and the first answer is still on its way. */
    public WeaponTrackerScreen(TrackerRules.Table table) {
        super(Component.translatableWithFallback("anime_multiverse.weapon_tracker.title", "Weapon Tracker"));
        accept(table);
    }

    /** A fresh table from the server. */
    public void accept(TrackerRules.Table t) {
        if (t == null) return;
        this.table = t;
        this.sinceAsk = 0;
        rebuild();
    }

    private void rebuild() {
        shown = table == null ? List.of() : TrackerRules.filter(table.rows(), filter);
        scroll = TrackerRules.clampScroll(scroll, shown.size(), VISIBLE);
    }

    @Override public boolean isPauseScreen() { return false; }

    private int left() { return (width - W) / 2; }
    private int top() { return (height - H) / 2; }

    private static Component tr(String key, String fallback, Object... args) { return Component.translatableWithFallback("anime_multiverse.weapon_tracker." + key, fallback, args); }

    @Override
    protected void init() {
        int x = left(), y = top();
        refresh = new ThemedButton(x + PAD, y + H - 24, 110, 16, tr("refresh", "Refresh"), b -> ask());
        addRenderableWidget(refresh);
        addRenderableWidget(new ThemedButton(x + W - PAD - 110, y + H - 24, 110, 16, tr("close", "Close"), b -> onClose()));
        int cx = x + PAD;
        for (int i = 0; i < CHIP_KEYS.length; i++) {
            chipW[i] = font.width(chipText(i).getString()) + 10;
            chipX[i] = cx;
            cx += chipW[i] + 3;
        }
        if (table == null) ask();
    }

    private Component chipText(int i) { return tr("chip." + CHIP_KEYS[i], CHIP_EN[i]); }

    private void ask() {
        if (cooldown > 0) return;
        WeaponTrackerClient.request();
        cooldown = TrackerLimiter.GAP_TICKS;
        sinceAsk = 0;
        if (refresh != null) refresh.active = false;
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        if (cooldown > 0 && --cooldown == 0 && refresh != null) refresh.active = true;
        if (++sinceAsk >= AUTO_REFRESH_TICKS) ask();
    }

    // ================================================================ input
    private void pick(Filter f) {
        filter = f;
        scroll = 0;
        rebuild();
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        switch (key) {
            case GLFW.GLFW_KEY_LEFT -> { pick(TrackerRules.step(filter, -1)); return true; }
            case GLFW.GLFW_KEY_RIGHT -> { pick(TrackerRules.step(filter, 1)); return true; }
            case GLFW.GLFW_KEY_UP -> { scrollBy(-1); return true; }
            case GLFW.GLFW_KEY_DOWN -> { scrollBy(1); return true; }
            case GLFW.GLFW_KEY_PAGE_UP -> { scrollBy(-VISIBLE); return true; }
            case GLFW.GLFW_KEY_PAGE_DOWN -> { scrollBy(VISIBLE); return true; }
            case GLFW.GLFW_KEY_HOME -> { scrollBy(-1000); return true; }
            case GLFW.GLFW_KEY_END -> { scrollBy(1000); return true; }
            case GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_F5 -> { ask(); return true; }
            default -> {
                if (key >= GLFW.GLFW_KEY_1 && key < GLFW.GLFW_KEY_1 + Filter.values().length) { pick(Filter.values()[key - GLFW.GLFW_KEY_1]); return true; }
            }
        }
        return super.keyPressed(key, scan, mods);
    }

    private void scrollBy(int rows) { scroll = TrackerRules.clampScroll(scroll + rows, shown.size(), VISIBLE); }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (sy != 0) { scrollBy(sy > 0 ? -1 : 1); return true; }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int y = top();
        if (button == 0 && my >= y + 36 && my < y + 50) {
            for (int i = 0; i < chipX.length; i++) {
                if (mx >= chipX[i] && mx < chipX[i] + chipW[i]) { pick(Filter.values()[i]); return true; }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    // ================================================================ drawing
    private static int statusColor(Status s) {
        return switch (s) {
            case FREE -> UiTheme.GREEN;
            case TAKEN -> UiTheme.RED;
            case YOURS -> UiTheme.GOLD;
            case SHARED -> UiTheme.CYAN;
        };
    }

    private static int classColor(HeroClass c) {
        return switch (c) {
            case HOLY -> UiTheme.GOLD;
            case SEVEN_STAR -> UiTheme.VIOLET;
            case VASSAL -> UiTheme.CYAN;
        };
    }

    private static Component statusLabel(Status s) {
        return switch (s) {
            case FREE -> tr("status.free", "FREE");
            case TAKEN -> tr("status.taken", "TAKEN");
            case YOURS -> tr("status.yours", "YOURS");
            case SHARED -> tr("status.shared", "SHARED");
        };
    }

    private static Component className(HeroClass c) {
        return switch (c) {
            case HOLY -> tr("chip.legendary", "Legendary");
            case SEVEN_STAR -> tr("chip.seven_star", "Seven Star");
            case VASSAL -> tr("chip.vassal", "Vassal");
        };
    }

    private static Component weaponName(Row r) {
        return Component.translatableWithFallback("item.anime_multiverse." + r.kind().itemId(), r.kind().display());
    }

    private static Component cat(Component a, Component b) { return Component.empty().append(a).append(b); }

    private Component holderLine(Row r) {
        Component who = r.holder().isEmpty() ? tr("another", "another player") : Component.literal(r.holder());
        Component online = r.holders() > 0 && r.status() != Status.YOURS ? (r.online() ? tr("online", " (online)") : tr("offline", " (offline)")) : Component.empty();
        return switch (r.status()) {
            case FREE -> tr("line.free", "Nobody holds it");
            case TAKEN -> cat(tr("line.taken", "Held by %s", who), online);
            case SHARED -> cat(r.holder().isEmpty() ? tr("line.shared", "Shared, %s holders", r.holders()) : tr("line.shared_named", "Shared, %s holders, e.g. %s", r.holders(), who), online);
            case YOURS -> r.holders() > 1 ? tr("line.yours_shared", "Held by you, %s holders in all", r.holders()) : tr("line.yours", "Held by you");
        };
    }

    /** Everything is drawn here, after the flat dim and never the blurring vanilla background (ScreenBlurGuardTest); vanilla's render then draws the two buttons over it. */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderTransparentBackground(g);
        int x = left(), y = top();
        boolean off = table != null && !table.settings().enabled();
        UiPanel.panel(g, x, y, W, H, off ? UiTheme.RED : UiTheme.CYAN, 0.7f, 1f);
        g.drawCenteredString(font, getTitle(), x + W / 2, y + 9, UiTheme.GOLD);
        drawSummary(g, x, y);
        drawChips(g, x, y, mouseX, mouseY);
        drawList(g, x, y, mouseX, mouseY);
        g.drawCenteredString(font, font.plainSubstrByWidth(tr("hint", "Left / Right: filter   Up / Down: scroll   R: refresh").getString(), W - 2 * PAD), x + W / 2, y + H - 38, UiTheme.TEXT_FAINT);
    }

    private void drawSummary(GuiGraphics g, int x, int y) {
        if (table == null) {
            g.drawString(font, tr(ticks > NO_ANSWER_TICKS ? "no_answer" : "loading", ticks > NO_ANSWER_TICKS ? "No answer from the server yet." : "Asking the register...").getString(), x + PAD, y + 23, UiTheme.TEXT_DIM, false);
            return;
        }
        if (!table.settings().enabled()) {
            g.drawString(font, font.plainSubstrByWidth(tr("disabled", "The hero weapons are switched off on this server.").getString(), W - 2 * PAD), x + PAD, y + 23, UiTheme.RED, false);
            return;
        }
        String free = tr("free_count", "Free: %s / %s", TrackerRules.countFree(table.rows()), table.rows().size()).getString();
        g.drawString(font, free, x + PAD, y + 23, UiTheme.GREEN, false);
        int bonds = table.settings().maxBonds();
        String bond = bonds < 1 ? tr("bond_closed", "Bonds are closed by the server").getString()
                : table.own() == null ? tr("bond_none", "Your bond: none (0/%s)", bonds).getString()
                : tr("bond_own", "Your bond: %s (1/%s)", weaponName(new Row(table.own(), Status.YOURS, 1, "", true, false)), bonds).getString();
        int room = W - 2 * PAD - font.width(free) - 10;
        String fit = font.plainSubstrByWidth(bond, room);
        g.drawString(font, fit, x + W - PAD - font.width(fit), y + 23, table.own() == null ? UiTheme.TEXT_DIM : UiTheme.GOLD, false);
    }

    private void drawChips(GuiGraphics g, int x, int y, int mx, int my) {
        for (int i = 0; i < chipX.length; i++) {
            boolean active = filter.ordinal() == i;
            boolean hot = mx >= chipX[i] && mx < chipX[i] + chipW[i] && my >= y + 36 && my < y + 50;
            UiPanel.chip(g, chipX[i], y + 36, chipW[i], 14, active, hot, active ? UiTheme.CYAN : UiTheme.TEXT_DIM);
            g.drawString(font, chipText(i).getString(), chipX[i] + 5, y + 39, active ? UiTheme.WHITE : UiTheme.TEXT, false);
        }
    }

    private void drawList(GuiGraphics g, int x, int y, int mx, int my) {
        int lx = x + PAD, ly = y + 56, lw = W - 2 * PAD, lh = ROW * VISIBLE;
        UiPanel.well(g, lx, ly, lw, lh, UiTheme.CYAN, 1f);
        if (table == null) return;
        if (shown.isEmpty()) {
            g.drawCenteredString(font, tr("empty", "No weapons match this filter.").getString(), lx + lw / 2, ly + lh / 2 - 4, UiTheme.TEXT_DIM);
            return;
        }
        for (int i = 0; i < VISIBLE && scroll + i < shown.size(); i++) {
            Row r = shown.get(scroll + i);
            int ry = ly + i * ROW, rw = lw - 6;
            boolean mine = r.status() == Status.YOURS;
            if (mine) {
                g.fill(lx + 1, ry + 1, lx + rw, ry + ROW - 1, 0x33E8C468);
                g.renderOutline(lx + 1, ry + 1, rw - 1, ROW - 2, UiTheme.GOLD);
            } else if (mx >= lx && mx < lx + rw && my >= ry && my < ry + ROW) {
                UiPanel.hover(g, lx + 1, ry + 1, rw - 1, ROW - 2, 1f, UiTheme.CYAN);
            }
            g.fill(lx + 2, ry + 3, lx + 5, ry + ROW - 3, classColor(r.kind().heroClass()));
            g.drawString(font, font.plainSubstrByWidth(weaponName(r).getString(), rw - 86), lx + 10, ry + 3, mine ? UiTheme.GOLD : UiTheme.TEXT, false);
            String line = className(r.kind().heroClass()).getString() + "  -  " + holderLine(r).getString();
            g.drawString(font, font.plainSubstrByWidth(line, rw - 80), lx + 10, ry + 12, UiTheme.TEXT_DIM, false);
            int c = statusColor(r.status());
            int pw = 56, px = lx + rw - pw - 4, py = ry + 5;
            g.fill(px, py, px + pw, py + 12, (c & 0xFFFFFF) | 0x33000000);
            g.renderOutline(px, py, pw, 12, c);
            g.drawCenteredString(font, statusLabel(r.status()).getString(), px + pw / 2, py + 2, c);
        }
        if (shown.size() > VISIBLE) {                                                          // the scroll bar
            int bx = lx + lw - 4, bh = Math.max(10, lh * VISIBLE / shown.size());
            int by = ly + (lh - bh) * scroll / Math.max(1, shown.size() - VISIBLE);
            g.fill(bx, ly + 1, bx + 3, ly + lh - 1, 0x33FFFFFF);
            g.fill(bx, by, bx + 3, by + bh, UiTheme.CYAN);
        }
    }
}
