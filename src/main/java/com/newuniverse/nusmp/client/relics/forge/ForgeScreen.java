package com.newuniverse.nusmp.client.relics.forge;

import com.newuniverse.nusmp.client.ui.theme.MagicCircle;
import com.newuniverse.nusmp.client.ui.theme.SageBlock;
import com.newuniverse.nusmp.client.ui.theme.ThemedButton;
import com.newuniverse.nusmp.client.ui.theme.UiBar;
import com.newuniverse.nusmp.client.ui.theme.UiLang;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.forge.ForgeRules;
import com.newuniverse.nusmp.relics.forge.ForgeRules.Button;
import com.newuniverse.nusmp.relics.forge.ForgeView;
import com.newuniverse.nusmp.relics.forge.RelicForgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.EnumMap;
import java.util.Map;

/**
 * Why: the screen of the Relic Forge shows what the server computed and sends button ids, nothing else; it decides no rule. Everything it prints comes
 * from {@link ForgeView} (the pure formatter over the synced container data), so the numbers cannot disagree with the server. The buttons stay
 * clickable even when they would be refused (they are only dimmed): a click on a dimmed button reaches the server, which answers with a Great Sage
 * refusal line in the panel under the window, so nothing is ever silently ignored. Anime look when {@link UiTheme#anime()} (panel scan, a turning
 * magic circle behind the slots, a tier bar), a plain dark panel otherwise; the slots, clicks and server logic are the same in both.
 */
public class ForgeScreen extends AbstractContainerScreen<RelicForgeMenu> {
    private static final int BG = 0xF0101016, BORDER = 0xFF3B3B4C, GOLD = 0xFFE8C468, SLOT = 0xFF1C1C26, SLOT_EDGE = 0xFF55556A, DIM = 0xFF9A9AAE, TEXT = 0xFFE6E6F0;
    private static final int TAB_UPGRADE = 0, TAB_ENGRAVE = 1;

    private final SageBlock sage = new SageBlock();
    private final UiBar tierBar = new UiBar();
    private final Map<Button, net.minecraft.client.gui.components.Button> buttons = new EnumMap<>(Button.class);
    private net.minecraft.client.gui.components.Button tabUpgrade, tabEngrave;
    private int tab = TAB_UPGRADE;

    public ForgeScreen(RelicForgeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = RelicForgeMenu.WIDTH;
        imageHeight = RelicForgeMenu.HEIGHT;
        titleLabelY = 4;
        inventoryLabelY = RelicForgeMenu.INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        buttons.clear();
        int x = leftPos + 8, w = (imageWidth - 16 - 4) / 2;
        add(Button.UPGRADE, x, topPos + 112, w, 14, "nusmp.forge.button.upgrade");
        add(Button.SOCKET, x + w + 4, topPos + 112, w, 14, "nusmp.forge.button.socket");
        add(Button.DRAW_OUT, x, topPos + 128, w, 14, "nusmp.forge.button.draw_out");
        add(Button.CYCLE, x + w + 4, topPos + 128, w, 14, "nusmp.forge.button.cycle");
        add(Button.ENGRAVE, x, topPos + 112, imageWidth - 16, 14, "nusmp.forge.button.engrave");
        tabUpgrade = addRenderableWidget(new ThemedButton(leftPos, topPos - 16, 84, 14, Component.translatable("nusmp.forge.tab.upgrade"), b -> setTab(TAB_UPGRADE)));
        tabEngrave = addRenderableWidget(new ThemedButton(leftPos + 88, topPos - 16, 88, 14, Component.translatable("nusmp.forge.tab.engrave"), b -> setTab(TAB_ENGRAVE)));
        setTab(tab);
    }

    private void add(Button id, int x, int y, int w, int h, String key) {
        buttons.put(id, addRenderableWidget(new ThemedButton(x, y, w, h, Component.translatable(key), b -> press(id))));
    }

    private void press(Button b) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, b.id());
    }

    private void setTab(int t) {
        tab = t;
        for (var e : buttons.entrySet()) e.getValue().visible = (e.getKey() == Button.ENGRAVE) == (t == TAB_ENGRAVE);
    }

    private static String t(ForgeView.Line l) { return UiLang.get(l.key(), l.args()); }

    // ---------------------------------------------------------------- drawing
    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        ForgeView.View v = menu.view();
        float now = UiTheme.now();
        int x = leftPos, y = topPos;
        boolean anime = UiTheme.anime();
        if (anime) {
            UiPanel.panelScan(g, x, y, imageWidth, imageHeight, UiTheme.VIOLET, 0.8f, 1f, now);
            g.enableScissor(x + 1, y + 1, x + imageWidth - 1, y + 56);
            MagicCircle.draw(g, x + imageWidth / 2f, y + 30f, 46f, now, UiTheme.VIOLET, v.hasSubject() ? 0.55f : 0.25f, MagicCircle.STANDARD, 1f);
            g.disableScissor();
        } else {
            g.fill(x, y, x + imageWidth, y + imageHeight, BG);
            g.renderOutline(x, y, imageWidth, imageHeight, BORDER);
        }
        for (Slot s : menu.slots) {
            int sx = x + s.x - 1, sy = y + s.y - 1;
            boolean table = s.index < ForgeRules.TABLE_SLOTS;
            boolean hover = mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18;
            if (anime) {
                UiPanel.well(g, sx, sy, 18, 18, table ? UiTheme.GOLD : UiTheme.CYAN, table ? 1f : 0.7f);
                if (hover) UiPanel.hover(g, sx + 1, sy + 1, 16, 16, 1f, UiTheme.GOLD);
            } else {
                g.fill(sx, sy, sx + 18, sy + 18, table ? GOLD : SLOT_EDGE);
                g.fill(sx + 1, sy + 1, sx + 17, sy + 17, SLOT);
            }
        }
        slotLabel(g, "nusmp.forge.slot.relic", RelicForgeMenu.RELIC_X + 8, RelicForgeMenu.RELIC_Y + 20);
        slotLabel(g, "nusmp.forge.slot.catalyst", RelicForgeMenu.CAT_A_X + 8, RelicForgeMenu.CAT_B_Y + 20);
        slotLabel(g, "nusmp.forge.slot.core", RelicForgeMenu.CORE_X + 8, RelicForgeMenu.CORE_Y + 20);
        slotLabel(g, "nusmp.forge.slot.hint", RelicForgeMenu.PREVIEW_X + 8, RelicForgeMenu.PREVIEW_Y + 20);

        int bx = x + 13, bw = imageWidth - 26;
        if (tab == TAB_UPGRADE) {
            g.drawString(font, t(v.tierLine()), bx, y + 62, anime ? UiTheme.TEXT : TEXT, false);
            if (v.isRelic() && v.tier() >= 0 && v.tier() < Grade.values().length) {
                Grade gr = Grade.values()[v.tier()];
                String name = gr.shortName();
                g.drawString(font, name, x + imageWidth - 13 - font.width(name), y + 62, 0xFF000000 | gr.rgb(), false);
            }
            tierBar.update(v.tierFraction(), now, false);
            if (anime) tierBar.draw(g, bx, y + 74, bw, 6, UiTheme.VIOLET, now, 1f);
            else {
                g.fill(bx, y + 74, bx + bw, y + 80, SLOT);
                g.fill(bx, y + 74, bx + Math.round(bw * v.tierFraction()), y + 80, GOLD);
            }
            g.drawString(font, t(v.costLine()), bx, y + 86, anime ? UiTheme.TEXT_DIM : DIM, false);
            g.drawString(font, t(v.chanceLine()), bx, y + 96, anime ? UiTheme.TEXT_DIM : DIM, false);
            g.drawString(font, t(v.socketLine()), x + 92, y + 86, anime ? UiTheme.TEXT_DIM : DIM, false);
            g.drawString(font, t(v.cycleLine()), x + 92, y + 96, anime ? UiTheme.TEXT_DIM : DIM, false);
            ForgeView.Line cd = v.cooldownLine();
            if (cd != null) g.drawString(font, t(cd), bx, y + 104, anime ? UiTheme.RED : 0xFFFF6A6A, false);
        } else {
            g.drawString(font, t(v.engraveLine()), bx, y + 66, anime ? UiTheme.TEXT : TEXT, false);
            g.drawString(font, Component.translatable("nusmp.forge.engrave.help1"), bx, y + 80, anime ? UiTheme.TEXT_DIM : DIM, false);
            g.drawString(font, Component.translatable("nusmp.forge.engrave.help2"), bx, y + 90, anime ? UiTheme.TEXT_DIM : DIM, false);
        }
        renderSage(g, v, now);
    }

    private void slotLabel(GuiGraphics g, String key, int cx, int ty) {
        Component c = Component.translatable(key);
        g.drawString(font, c, leftPos + cx - font.width(c) / 2, topPos + ty, UiTheme.anime() ? UiTheme.TEXT_FAINT : DIM, false);
    }

    /** The Great Sage line under the window: the last answer, or the reason the next press would be refused. */
    private void renderSage(GuiGraphics g, ForgeView.View v, float now) {
        ForgeRules.Msg m = v.msg();
        String head, body, result = "";
        if (m == ForgeRules.Msg.OK) {
            head = UiLang.get(UiLang.ANALYZING);
            body = UiLang.get("nusmp.forge.msg.idle");
        } else if (m.good()) {
            head = UiLang.get(UiLang.ANALYSIS_COMPLETE);
            body = "";
            result = UiLang.get(UiLang.RESULT, t(v.messageLine()));
        } else {
            head = UiLang.get(UiLang.WARNING, "");
            body = t(v.messageLine());
        }
        sage.set(font, head, body, result, imageWidth - 16, now);
        int ph = sage.height(font) + 10, py = topPos + imageHeight + 5;
        if (UiTheme.anime()) UiPanel.panel(g, leftPos, py, imageWidth, ph, UiTheme.CYAN, 0.5f, 1f);
        else {
            g.fill(leftPos, py, leftPos + imageWidth, py + ph, BG);
            g.renderOutline(leftPos, py, imageWidth, ph, BORDER);
        }
        sage.draw(g, font, leftPos + 8, py + 5, now, 1f);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        boolean anime = UiTheme.anime();
        g.drawString(font, title, titleLabelX, titleLabelY, anime ? UiTheme.GOLD : GOLD, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, anime ? UiTheme.TEXT_DIM : DIM, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        ForgeView.View v = menu.view();
        for (var e : buttons.entrySet()) {                                              // dim what the server would refuse (still clickable: the click is answered with a reason)
            var b = e.getValue();
            if (b.visible && !v.enabled(e.getKey())) g.fill(b.getX() + 1, b.getY() + 1, b.getX() + b.getWidth() - 1, b.getY() + b.getHeight() - 1, UiMath.withAlpha(0x000000, 0.45f));
        }
        renderTooltip(g, mouseX, mouseY);
    }
}
