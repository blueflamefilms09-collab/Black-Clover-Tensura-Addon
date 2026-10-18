package com.newuniverse.nusmp.client.skilltree;

import com.newuniverse.nusmp.client.skilltree.layout.PassiveRows;
import com.newuniverse.nusmp.client.skilltree.layout.Rect;
import com.newuniverse.nusmp.client.skilltree.layout.ScreenLayout;
import com.newuniverse.nusmp.client.ui.theme.ThemedButton;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.skilltree.PlayerTreeState;
import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.line.LineCatalog;
import com.newuniverse.nusmp.skilltree.line.LineText;
import com.newuniverse.nusmp.skilltree.state.IPlayerSkillTree;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: the owner wants Tensura's way of managing passives: every passive and stat bonus you own in one scrollable list, each with an on/off switch, so a player with the passives of many lines can turn a
 * clumsy one off without losing it. A player may own the passives of a hundred lines, so this is built like a list for thousands of rows, not like a menu of ten: the rows are made ONCE by the pure
 * {@link PassiveRows} when something changed (a purchase, a switch, the search text, a resize), never per frame; every row has a fixed height so the rows on screen for a scroll offset are found by
 * binary search and only those are drawn (a dozen, whatever the total); the clipped line and node names are precomputed with the rows, so a frame allocates nothing for the list. The tree and the registry are read
 * through a cheap signature once per tick, so a purchase made in the Skill Tree screen, an unlock, or the server's answer to a click shows in the list within a tick and no refresh is needed. The equipped
 * line is listed first; the lines that are not equipped are dimmed and say so, because their passives are asleep. Hovering a row shows what the passive is (its description, the kind of effect, the numbers at the player's level).
 * A click sends the switch to the server ({@code TogglePassivePayload}); the screen flips its own copy at once so the click feels instant, and the server's answer (or its refusal, which sends the tree
 * again) puts the truth back. Typing narrows the list; the wheel, the arrow keys, Page Up/Down, Home/End and dragging the bar scroll it. Escape or Back returns to the screen that opened it.
 */
public class PassivesScreen extends Screen {
    private static final int SWITCH_W = 24, SWITCH_H = 11, ROW_PAD = 4;

    private final Screen parent;
    private ScreenLayout lay;
    private Rect listRect = new Rect(0, 0, 0, 0), searchRect = new Rect(0, 0, 0, 0);
    private ThemedButton closeBtn;
    private EditBox search;

    private List<PassiveRows.Row> rows = List.of();
    private PassiveRows.Layout layout = new PassiveRows.Layout(List.of());
    private String[] shownName = new String[0], shownLine = new String[0], shownSide = new String[0];
    private String countText = "", hintText = "";
    private String filter = "";
    private int scroll, signature;
    private boolean dirty = true, draggingBar;
    private int hoverRow = -1, tipRow = -1;
    private List<FormattedCharSequence> tipLines = List.of(), emptyLines = List.of();

    public PassivesScreen(Screen parent) {
        super(LineText.tr(LineText.PASSIVES_TITLE));
        this.parent = parent;
    }

    @Override
    protected void init() {
        lay = ScreenLayout.of(width, height);
        searchRect = new Rect(lay.header.x(), lay.routes.y(), lay.header.w(), 14);
        listRect = new Rect(lay.header.x(), searchRect.bottom() + 3, lay.header.w(), Math.max(20, lay.footer.y() - 3 - (searchRect.bottom() + 3)));
        search = new EditBox(font, searchRect.x(), searchRect.y(), searchRect.w(), searchRect.h(), Component.empty());
        search.setMaxLength(40);
        search.setValue(filter);
        search.setResponder(text -> {
            filter = text;
            dirty = true;
            scroll = 0;
        });
        addRenderableWidget(search);
        closeBtn = addRenderableWidget(new ThemedButton(lay.closeButton.x(), lay.closeButton.y(), lay.closeButton.w(), lay.closeButton.h(), LineText.tr(LineText.PASSIVES_CLOSE), b -> onClose()));
        dirty = true;
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }

    @Override
    public boolean isPauseScreen() { return false; }

    // ------------------------------------------------------------------ the model
    private IPlayerSkillTree tree() { return minecraft.player == null ? null : SkillTreeApi.of(minecraft.player); }

    /** A cheap digest of everything the list depends on. */
    private int signatureNow() {
        IPlayerSkillTree t = tree();
        if (t == null) return 0;
        int h = t.passiveToggles().hashCode();
        h = h * 31 + t.nodeLevels().hashCode();
        h = h * 31 + t.activeClass().hashCode();
        h = h * 31 + t.unlockedRoutes().hashCode();
        return h * 31 + System.identityHashCode(RouteRegistry.all());
    }

    @Override
    public void tick() {
        int s = signatureNow();
        if (s != signature) dirty = true;
        if (dirty) rebuild(s);
    }

    private void rebuild(int sig) {
        IPlayerSkillTree t = tree();
        dirty = false;
        signature = sig;
        if (t == null) {
            rows = List.of();
        } else {
            rows = PassiveRows.build(RouteRegistry.all(), new PlayerTreeState(t), t::isPassiveEnabled, filter);
        }
        layout = new PassiveRows.Layout(rows);
        int nameW = Math.max(30, listRect.w() - 4 - 22 - SWITCH_W - 8 - 44), lineW = Math.max(30, listRect.w() - 40);
        shownName = new String[rows.size()];
        shownLine = new String[rows.size()];
        shownSide = new String[rows.size()];
        for (int i = 0; i < rows.size(); i++) {                          // everything a frame will draw as text is made here, once
            PassiveRows.Row r = rows.get(i);
            shownName[i] = font.plainSubstrByWidth(r.name(), r.header() ? lineW : nameW);
            shownLine[i] = r.header() ? font.plainSubstrByWidth(r.line(), lineW - 120) : "";
            shownSide[i] = r.header() ? LineText.tr(r.equipped() ? LineText.PASSIVES_EQUIPPED : LineText.PASSIVES_COUNT, r.count()).getString() : LineText.tr(LineText.PASSIVES_LEVEL, r.level()).getString();
        }
        countText = LineText.tr(LineText.PASSIVES_COUNT, PassiveRows.entries(rows)).getString();
        hintText = font.plainSubstrByWidth(LineText.tr(LineText.PASSIVES_SCROLL_HINT).getString(), lay.footer.w() - 4);
        scroll = layout.clampScroll(scroll, listRect.h());
        tipRow = -1;
        emptyLines = rows.isEmpty() ? font.split(LineText.tr(filter.isEmpty() ? LineText.PASSIVES_EMPTY : LineText.PASSIVES_NO_MATCH), Math.max(40, listRect.w() - 30)) : List.of();
    }

    // ------------------------------------------------------------------ input
    private int rowAtMouse(double mx, double my) {
        if (!listRect.contains(mx, my)) return -1;
        return layout.rowAt((int) my - listRect.y() + scroll);
    }

    private Rect barRect() { return new Rect(listRect.right() - 6, listRect.y(), 6, listRect.h()); }

    private void scrollTo(int px) {
        int span = Math.max(1, layout.total() - listRect.h());
        int bar = Math.max(12, listRect.h() * listRect.h() / Math.max(1, layout.total()));
        float f = (px - listRect.y() - bar / 2f) / Math.max(1f, listRect.h() - bar);
        scroll = layout.clampScroll(Math.round(f * span), listRect.h());
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button != 0) return false;
        if (layout.total() > listRect.h() && barRect().contains(mx, my)) {
            draggingBar = true;
            scrollTo((int) my);
            return true;
        }
        int i = rowAtMouse(mx, my);
        if (i >= 0 && !rows.get(i).header()) {
            toggle(rows.get(i));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingBar && button == 0) {
            scrollTo((int) my);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        draggingBar = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (listRect.contains(mx, my)) {
            scroll = layout.clampScroll(scroll - (int) Math.round(sy * PassiveRows.ENTRY_H * 2), listRect.h());
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) return super.keyPressed(key, scan, mods);
        int page = Math.max(20, listRect.h() - 20);
        switch (key) {
            case GLFW.GLFW_KEY_DOWN -> { scroll = layout.clampScroll(scroll + PassiveRows.ENTRY_H, listRect.h()); return true; }
            case GLFW.GLFW_KEY_UP -> { scroll = layout.clampScroll(scroll - PassiveRows.ENTRY_H, listRect.h()); return true; }
            case GLFW.GLFW_KEY_PAGE_DOWN -> { scroll = layout.clampScroll(scroll + page, listRect.h()); return true; }
            case GLFW.GLFW_KEY_PAGE_UP -> { scroll = layout.clampScroll(scroll - page, listRect.h()); return true; }
            case GLFW.GLFW_KEY_HOME -> { if (!search.isFocused()) { scroll = 0; return true; } }
            case GLFW.GLFW_KEY_END -> { if (!search.isFocused()) { scroll = layout.clampScroll(Integer.MAX_VALUE / 2, listRect.h()); return true; } }
            default -> { }
        }
        return super.keyPressed(key, scan, mods);
    }

    private void toggle(PassiveRows.Row r) {
        IPlayerSkillTree t = tree();
        if (t == null || !SkillTreeApi.isEnabled()) return;
        boolean now = t.isPassiveEnabled(r.route().id(), r.node().id());
        TreeActions.toggle(r.route().id(), r.node().id(), !now);
        t.setPassiveEnabled(r.route().id(), r.node().id(), !now);          // our copy only; the server's tree overwrites it, and a refusal sends the tree again
        TreeSounds.click();
        dirty = true;
    }

    // ------------------------------------------------------------------ drawing
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        int accent = UiTheme.CYAN & 0xFFFFFF;
        if (TreeSkin.anime()) {
            UiPanel.scrim(g, width, height, 0.9f);
            UiPanel.panelScan(g, lay.panel.x(), lay.panel.y(), lay.panel.w(), lay.panel.h(), accent, 0.9f, 1f, UiTheme.now());
        } else {
            renderTransparentBackground(g);
            TreeSkin.panel(g, lay.panel, accent, 0.9f);
        }
        TreeSkin.well(g, listRect, accent);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);                                 // the background, the search box, the Back button
        Font f = font;
        Component title = LineText.tr(LineText.PASSIVES_TITLE).withStyle(ChatFormatting.BOLD);
        g.drawString(f, title, lay.header.x() + 4, lay.header.y() + 6, TreeSkin.gold(), false);
        g.drawString(f, countText, lay.closeButton.x() - 10 - f.width(countText), lay.header.y() + 6, TreeSkin.dim(), false);

        hoverRow = rowAtMouse(mx, my);
        drawList(g, f, mx, my);
        g.drawString(f, hintText, lay.footer.x() + 2, lay.footer.y() + 1, TreeSkin.faint(), false);
        if (hoverRow >= 0 && !rows.get(hoverRow).header()) {
            if (tipRow != hoverRow) {
                tipRow = hoverRow;
                tipLines = tooltipFor(rows.get(hoverRow));
            }
            g.renderTooltip(f, tipLines, mx, my);
        }
    }

    private void drawList(GuiGraphics g, Font f, int mx, int my) {
        Rect a = listRect;
        g.enableScissor(a.x() + 1, a.y() + 1, a.right() - 1, a.bottom() - 1);
        if (rows.isEmpty()) {
            int y = a.y() + a.h() / 2 - emptyLines.size() * 5;
            for (FormattedCharSequence l : emptyLines) {
                g.drawCenteredString(f, l, a.x() + a.w() / 2, y, TreeSkin.dim());
                y += f.lineHeight + 2;
            }
        } else {
            int first = Math.max(0, layout.rowAt(scroll));
            for (int i = first; i < rows.size(); i++) {
                int y = a.y() + layout.top(i) - scroll;
                if (y >= a.bottom()) break;
                PassiveRows.Row r = rows.get(i);
                if (r.header()) drawHeader(g, f, a, i, r, y);
                else drawEntry(g, f, a, i, r, y);
            }
        }
        g.disableScissor();
        if (layout.total() > a.h()) {
            int bar = Math.max(12, a.h() * a.h() / layout.total());
            int span = Math.max(1, layout.total() - a.h());
            TreeSkin.scrollbar(g, a.right() - 5, a.y() + 1, a.h() - 2, (int) ((a.h() - 2 - bar) * (scroll / (float) span)), bar);
        }
    }

    private void drawHeader(GuiGraphics g, Font f, Rect a, int i, PassiveRows.Row r, int y) {
        int x = a.x() + 3, w = a.w() - 12;
        g.fill(x, y + 2, x + 3, y + PassiveRows.HEADER_H - 1, TreeSkin.opaque(r.color()));
        int nameColor = r.equipped() ? TreeSkin.opaque(r.color()) : TreeSkin.faint();
        g.drawString(f, shownLine[i], x + 7, y + 3, nameColor, false);
        g.drawString(f, shownSide[i], x + w - f.width(shownSide[i]), y + 3, r.equipped() ? TreeSkin.gold() : TreeSkin.faint(), false);
        g.hLine(x, x + w, y + PassiveRows.HEADER_H - 1, UiMath.withAlpha(r.color(), 0.35f));
    }

    private void drawEntry(GuiGraphics g, Font f, Rect a, int i, PassiveRows.Row r, int y) {
        int x = a.x() + 3, w = a.w() - 12, h = PassiveRows.ENTRY_H;
        boolean hover = i == hoverRow;
        boolean asleep = !r.equipped();
        TreeSkin.chip(g, x, y + 1, w, h - 2, false, hover, asleep ? TreeSkin.SEALED & 0xFFFFFF : r.color());
        int iy = y + (h - 16) / 2;
        g.renderItem(ItemIcons.of(r.node().icon(), r.route().icon()), x + ROW_PAD, iy);
        if (asleep) {                                                    // the icon of a sleeping line is dimmed (items are drawn at a depth of about 150, so the veil goes over them)
            g.pose().pushPose();
            g.pose().translate(0f, 0f, 200f);
            g.fill(x + ROW_PAD, iy, x + ROW_PAD + 16, iy + 16, UiMath.withAlpha(0x05070E, 0.55f));
            g.pose().popPose();
        }
        int textColor = asleep ? TreeSkin.faint() : r.on() ? TreeSkin.text() : TreeSkin.dim();
        g.drawString(f, shownName[i], x + ROW_PAD + 20, y + (h - 8) / 2, textColor, false);
        g.drawString(f, shownSide[i], x + w - SWITCH_W - 10 - f.width(shownSide[i]), y + (h - 8) / 2, TreeSkin.faint(), false);
        drawSwitch(g, x + w - SWITCH_W - 4, y + (h - SWITCH_H) / 2, r.on(), asleep);
    }

    private void drawSwitch(GuiGraphics g, int x, int y, boolean on, boolean asleep) {
        int track = on ? (asleep ? 0xFF4A5A4E : 0xFF2E9E5B) : 0xFF3A3A48;
        g.fill(x, y, x + SWITCH_W, y + SWITCH_H, track);
        g.renderOutline(x, y, SWITCH_W, SWITCH_H, on ? 0xFF7CE0A4 : 0xFF6A6A7C);
        int knob = on ? x + SWITCH_W - SWITCH_H + 1 : x + 1;
        g.fill(knob, y + 1, knob + SWITCH_H - 2, y + SWITCH_H - 1, on ? 0xFFF4FFF8 : 0xFF9A9AAE);
    }

    /** What the passive is, in words: built only for the row under the mouse, and only when that row changes. */
    private List<FormattedCharSequence> tooltipFor(PassiveRows.Row r) {
        SkillNode n = r.node();
        List<FormattedCharSequence> out = new ArrayList<>(font.split(Component.literal(n.name()).withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD), 200));
        out.addAll(font.split(Component.literal(r.line() + " - " + LineText.tr(r.stat() ? LineText.PASSIVES_TYPE_STAT : LineText.PASSIVES_TYPE_PASSIVE).getString()).withStyle(ChatFormatting.AQUA), 200));
        out.addAll(font.split(LineText.tr(LineText.PASSIVES_LEVEL, r.level()).withStyle(ChatFormatting.GRAY), 200));
        if (!n.desc().isEmpty()) out.addAll(font.split(Component.literal(n.desc()), 200));
        LineCatalog.Kind kind = LineCatalog.get().kindForType(n.type(), n.kind());
        if (kind != null && !kind.desc().isEmpty() && !kind.desc().equals(n.desc())) out.addAll(font.split(Component.literal(kind.desc()).withStyle(ChatFormatting.DARK_GRAY), 200));
        StringBuilder nums = new StringBuilder();
        for (var e : kind == null ? java.util.Map.<String, LineCatalog.Num>of().entrySet() : kind.nums().entrySet()) {
            if (!n.values().containsKey(e.getKey())) continue;
            if (nums.length() > 0) nums.append("  ");
            nums.append(e.getKey().replace('_', ' ')).append(' ').append(shortNumber(n.value(e.getKey(), r.level(), 0)));
        }
        if (n.hasToggle() && "attr".equals(n.kind())) nums.append("  ").append(n.param("attribute", "")).append(' ').append(n.param("op", "add"));
        if (nums.length() > 0) out.addAll(font.split(Component.literal(nums.toString().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.GREEN), 200));
        if (!r.equipped()) out.addAll(font.split(LineText.tr(LineText.PASSIVES_NOT_EQUIPPED).withStyle(ChatFormatting.RED), 200));
        return out;
    }

    private static String shortNumber(double d) {
        if (d == Math.rint(d) && Math.abs(d) < 1e7) return Long.toString((long) d);
        return String.format(Locale.ROOT, "%.2f", d);
    }
}
