package com.newuniverse.nusmp.client.elite.hub;

import com.newuniverse.nusmp.elite.hub.CoinMath;
import com.newuniverse.nusmp.elite.hub.HubMenus;
import com.newuniverse.nusmp.elite.hub.HubNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the Merchant's trading screen: the custom GUI of the brief where the player spends coins on gear, food, utility items and the Dungeon Map. Tabs filter by category (the categories come
 * from the shop data, so a datapack's new category gets its tab), each row shows the item, how many come in the stack, the price in coins and how many the player may still buy today, and one
 * button buys ONE stack. A click only sends the offer's id ({@code HubNet.Buy}): the server reads the price from its own table, checks the stock, the floor lock and the purse, and answers with a
 * fresh whole picture, so the screen can never show a stale price or a purse that is not true. A ware that is locked behind a floor, sold out for today or too dear is greyed with the reason.
 * The purse is drawn at the bottom. Hovering a row shows the item's own tooltip.
 */
public class ShopScreen extends AbstractContainerScreen<HubMenus.ShopMenu> {
    private static final int W = 330, H = 236, ROW_H = 24, LIST_Y = 52, ROWS = 7, BTN_W = 52, BTN_H = 16, TAB_H = 14;

    private int scroll;
    private String tab = "all";

    public ShopScreen(HubMenus.ShopMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = W;
        this.imageHeight = H;
    }

    private List<String> tabs() {
        Set<String> cats = new LinkedHashSet<>();
        for (HubNet.ShopEntry e : HubClient.shop.entries()) cats.add(e.category());
        List<String> out = new ArrayList<>();
        out.add("all");
        out.addAll(cats);
        return out;
    }

    private List<HubNet.ShopEntry> visible() {
        List<HubNet.ShopEntry> out = new ArrayList<>();
        for (HubNet.ShopEntry e : HubClient.shop.entries()) if (tab.equals("all") || tab.equals(e.category())) out.add(e);
        return out;
    }

    private int maxScroll() { return Math.max(0, visible().size() - ROWS); }

    private int rowY(int i) { return topPos + LIST_Y + i * ROW_H; }

    private int btnX() { return leftPos + W - 8 - 8 - BTN_W; }

    private static String cap(String s) { return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }

    private static ItemStack stackOf(HubNet.ShopEntry e) {
        ResourceLocation id = ResourceLocation.tryParse(e.item());
        Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        return item == null ? ItemStack.EMPTY : new ItemStack(item, e.count());
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) { }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        HubGui.panel(g, leftPos, topPos, W, H);
        g.drawString(font, title.getString(), leftPos + 8, topPos + 8, HubGui.GOLD, true);
        String purse = "Purse: " + CoinMath.describe(HubClient.shop.balance());
        g.drawString(font, purse, leftPos + W - 8 - font.width(purse), topPos + 8, HubGui.TEXT, false);
        // the tabs
        int tx = leftPos + 8;
        for (String t : tabs()) {
            String label = cap(t);
            int w = font.width(label) + 12;
            boolean on = t.equals(tab);
            g.fill(tx, topPos + 24, tx + w, topPos + 24 + TAB_H, on ? HubGui.BUTTON_HOVER : HubGui.BUTTON);
            if (on) g.fill(tx, topPos + 24 + TAB_H - 1, tx + w, topPos + 24 + TAB_H, HubGui.PANEL_EDGE);
            g.drawString(font, label, tx + 6, topPos + 24 + 3, on ? HubGui.GOLD : HubGui.TEXT, false);
            tx += w + 3;
        }
        g.fill(leftPos + 6, topPos + 24 + TAB_H + 1, leftPos + W - 6, topPos + 24 + TAB_H + 2, HubGui.PANEL_EDGE);
        List<HubNet.ShopEntry> list = visible();
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        if (list.isEmpty()) g.drawString(font, "The Merchant has nothing to sell.", leftPos + 12, topPos + LIST_Y + 8, HubGui.MUTED, false);
        for (int i = 0; i < ROWS; i++) {
            int idx = scroll + i;
            if (idx >= list.size()) break;
            drawRow(g, list.get(idx), rowY(i), mouseX, mouseY);
        }
        if (maxScroll() > 0) {
            int trackY = topPos + LIST_Y, trackH = ROWS * ROW_H - 2;
            g.fill(leftPos + W - 12, trackY, leftPos + W - 8, trackY + trackH, HubGui.BAR_BG);
            int thumbH = Math.max(12, trackH * ROWS / list.size());
            int thumbY = trackY + (trackH - thumbH) * scroll / maxScroll();
            g.fill(leftPos + W - 12, thumbY, leftPos + W - 8, thumbY + thumbH, HubGui.PANEL_EDGE);
        }
        g.drawString(font, "Every price is in " + HubClient.shop.unit() + ". Stock is yours, per day.", leftPos + 8, topPos + H - 14, HubGui.MUTED, false);
    }

    private void drawRow(GuiGraphics g, HubNet.ShopEntry e, int y, int mx, int my) {
        int x = leftPos + 8, w = W - 16 - 8;
        boolean locked = e.lockedFloor() > 0, soldOut = e.remaining() == 0, poor = HubClient.shop.balance() < e.price();
        boolean canBuy = !locked && !soldOut && !poor;
        g.fill(x, y, x + w, y + ROW_H - 2, HubGui.inside(mx, my, x, y, w, ROW_H - 2) ? HubGui.ROW_HOVER : HubGui.ROW);
        ItemStack stack = stackOf(e);
        if (!stack.isEmpty()) g.renderItem(stack, x + 3, y + 3);
        String name = stack.isEmpty() ? e.item() : stack.getHoverName().getString();
        String line1 = e.count() > 1 ? name + " x" + e.count() : name;
        int textW = w - BTN_W - 30;
        g.drawString(font, HubGui.clip(font, line1, textW), x + 24, y + 3, canBuy ? HubGui.TEXT : HubGui.MUTED, false);
        String sub = locked ? "Reach floor " + e.lockedFloor() + " first" : soldOut ? "Sold out for today" : CoinMath.describe(e.price()) + (e.remaining() > 0 ? "  (" + e.remaining() + " left today)" : "");
        g.drawString(font, HubGui.clip(font, sub, textW), x + 24, y + 12, locked || soldOut ? HubGui.WARN : poor ? HubGui.WARN : HubGui.GOOD, false);
        int bx = btnX(), by = y + 3;
        HubGui.button(g, font, bx, by, BTN_W, BTN_H, "Buy", canBuy, canBuy && HubGui.inside(mx, my, bx, by, BTN_W, BTN_H));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        List<HubNet.ShopEntry> list = visible();
        for (int i = 0; i < ROWS; i++) {
            int idx = scroll + i;
            if (idx >= list.size()) break;
            if (HubGui.inside(mouseX, mouseY, leftPos + 8, rowY(i), 22, ROW_H - 2)) {
                ItemStack s = stackOf(list.get(idx));
                if (!s.isEmpty()) g.renderTooltip(font, s, mouseX, mouseY);
                break;
            }
        }
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int tx = leftPos + 8;
            for (String t : tabs()) {
                int w = font.width(cap(t)) + 12;
                if (HubGui.inside(mx, my, tx, topPos + 24, w, TAB_H)) {
                    tab = t;
                    scroll = 0;
                    return true;
                }
                tx += w + 3;
            }
            List<HubNet.ShopEntry> list = visible();
            for (int i = 0; i < ROWS; i++) {
                int idx = scroll + i;
                if (idx >= list.size()) break;
                HubNet.ShopEntry e = list.get(idx);
                boolean canBuy = e.lockedFloor() == 0 && e.remaining() != 0 && HubClient.shop.balance() >= e.price();
                if (canBuy && HubGui.inside(mx, my, btnX(), rowY(i) + 3, BTN_W, BTN_H)) {
                    PacketDistributor.sendToServer(new HubNet.Buy(e.id()));
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
