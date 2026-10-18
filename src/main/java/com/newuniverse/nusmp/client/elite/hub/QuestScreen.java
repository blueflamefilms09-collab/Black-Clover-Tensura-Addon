package com.newuniverse.nusmp.client.elite.hub;

import com.newuniverse.nusmp.elite.hub.CoinMath;
import com.newuniverse.nusmp.elite.hub.HubMenus;
import com.newuniverse.nusmp.elite.hub.HubNet;
import com.newuniverse.nusmp.elite.hub.QuestText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Why: the Quest Giver's dialogue. A right click on the NPC opens this screen with today's board: the quests the player carries (with their progress), today's daily quests, the next steps of the
 * progression chain, and the dailies already done; each row has ONE button that is the thing you can do with it (Accept, Hand in, Abandon). The screen decides nothing: a click only sends the
 * quest's id ({@code HubNet.QuestAction}), the server checks everything again and answers with a fresh board, which replaces what is drawn. The text of a quest comes from the packet (it is
 * data, there is no lang key per quest); the chrome is plain English with a translatable fallback. The list scrolls with the mouse wheel; hovering a row shows the quest's description.
 */
public class QuestScreen extends AbstractContainerScreen<HubMenus.QuestMenu> {
    private static final int W = 330, H = 236, ROW_H = 36, LIST_Y = 34, ROWS = 5, BTN_W = 66, BTN_H = 16;

    private int scroll;

    public QuestScreen(HubMenus.QuestMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = W;
        this.imageHeight = H;
    }

    private List<HubNet.BoardEntry> entries() { return HubClient.board.entries(); }

    private int maxScroll() { return Math.max(0, entries().size() - ROWS); }

    private int rowY(int i) { return topPos + LIST_Y + i * ROW_H; }

    private int btnX() { return leftPos + W - 8 - 6 - BTN_W - 4; }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) { }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        HubGui.panel(g, leftPos, topPos, W, H);
        g.drawString(font, title.getString(), leftPos + 8, topPos + 8, HubGui.GOLD, true);
        String purse = "Purse: " + CoinMath.describe(HubClient.board.balance());
        g.drawString(font, purse, leftPos + W - 8 - font.width(purse), topPos + 8, HubGui.TEXT, false);
        g.fill(leftPos + 6, topPos + 22, leftPos + W - 6, topPos + 23, HubGui.PANEL_EDGE);
        List<HubNet.BoardEntry> list = entries();
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        if (list.isEmpty()) g.drawString(font, "Nothing on the board today. Come back tomorrow.", leftPos + 12, topPos + LIST_Y + 8, HubGui.MUTED, false);
        for (int i = 0; i < ROWS; i++) {
            int idx = scroll + i;
            if (idx >= list.size()) break;
            drawRow(g, list.get(idx), rowY(i), mouseX, mouseY);
        }
        if (maxScroll() > 0) {                                                                      // the scroll thumb
            int trackY = topPos + LIST_Y, trackH = ROWS * ROW_H - 2;
            g.fill(leftPos + W - 12, trackY, leftPos + W - 8, trackY + trackH, HubGui.BAR_BG);
            int thumbH = Math.max(12, trackH * ROWS / list.size());
            int thumbY = trackY + (trackH - thumbH) * scroll / maxScroll();
            g.fill(leftPos + W - 12, thumbY, leftPos + W - 8, thumbY + thumbH, HubGui.PANEL_EDGE);
        }
        g.drawString(font, "Hand in finished quests here. Esc to leave.", leftPos + 8, topPos + H - 14, HubGui.MUTED, false);
    }

    private void drawRow(GuiGraphics g, HubNet.BoardEntry e, int y, int mx, int my) {
        int x = leftPos + 8, w = W - 16 - 8;
        boolean hover = HubGui.inside(mx, my, x, y, w, ROW_H - 2);
        int bg = e.state() == HubNet.READY ? 0xB01E3A28 : hover ? HubGui.ROW_HOVER : HubGui.ROW;
        g.fill(x, y, x + w, y + ROW_H - 2, bg);
        String tag = e.type() == 0 ? "[Daily] " : "[Quest] ";
        int titleColor = e.state() == HubNet.DONE ? HubGui.MUTED : e.state() == HubNet.OFFER ? HubGui.GOLD : HubGui.TEXT;
        int textW = w - BTN_W - 16;
        g.drawString(font, HubGui.clip(font, tag + e.title(), textW), x + 4, y + 4, titleColor, false);
        String progress = e.state() == HubNet.OFFER || e.state() == HubNet.DONE ? e.label() + " x" + e.target() : QuestText.progressLine(e.progress(), e.target(), e.label());
        g.drawString(font, HubGui.clip(font, progress, textW), x + 4, y + 15, HubGui.MUTED, false);
        g.drawString(font, "Reward: " + CoinMath.describe(e.reward()), x + 4, y + 25, e.state() == HubNet.DONE ? HubGui.MUTED : HubGui.GOOD, false);
        int bx = btnX(), by = y + 9;
        String label = switch (e.state()) {
            case HubNet.OFFER -> "Accept";
            case HubNet.ACTIVE -> "Abandon";
            case HubNet.READY -> "Hand in";
            default -> "Done";
        };
        HubGui.button(g, font, bx, by, BTN_W, BTN_H, label, e.state() != HubNet.DONE, HubGui.inside(mx, my, bx, by, BTN_W, BTN_H));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        List<HubNet.BoardEntry> list = entries();
        for (int i = 0; i < ROWS; i++) {
            int idx = scroll + i;
            if (idx >= list.size()) break;
            HubNet.BoardEntry e = list.get(idx);
            if (HubGui.inside(mouseX, mouseY, leftPos + 8, rowY(i), W - 16 - 8 - BTN_W - 12, ROW_H - 2) && !e.desc().isEmpty()) {
                g.renderTooltip(font, font.split(Component.literal(e.desc()), 220), mouseX, mouseY);
                break;
            }
        }
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            List<HubNet.BoardEntry> list = entries();
            for (int i = 0; i < ROWS; i++) {
                int idx = scroll + i;
                if (idx >= list.size()) break;
                HubNet.BoardEntry e = list.get(idx);
                if (e.state() != HubNet.DONE && HubGui.inside(mx, my, btnX(), rowY(i) + 9, BTN_W, BTN_H)) {
                    int action = e.state() == HubNet.OFFER ? HubNet.ACT_ACCEPT : e.state() == HubNet.READY ? HubNet.ACT_TURN_IN : HubNet.ACT_ABANDON;
                    PacketDistributor.sendToServer(new HubNet.QuestAction(action, e.id()));
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (maxScroll() > 0) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }
}
