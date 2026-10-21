package com.newuniverse.nusmp.client.loot;

import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.labyrinth.loot.backpack.BackpackLayout;
import com.newuniverse.nusmp.labyrinth.loot.backpack.BackpackMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Why: the window of a backpack of any tier, drawn from the menu's own slot list so it cannot disagree with the server about where a slot is: the panel is the width of a chest and
 * as tall as the tier's rows need ({@link BackpackLayout#height}), every slot gets a well, and the player's inventory sits under the rows like a chest's. It decides nothing: clicks,
 * shift-clicks and the rules about what may go in are the vanilla container protocol over {@link BackpackMenu}. With the anime GUI theme on it uses the theme's scanned panel and
 * gold wells; with it off, a plain dark panel with the same slots. Client only.
 */
public class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {
    private static final int BG = 0xF0101016, BORDER = 0xFF3B3B4C, GOLD = 0xFFE8C468, SLOT = 0xFF1C1C26, SLOT_EDGE = 0xFF55556A, DIM = 0xFF9A9AAE;

    public BackpackScreen(BackpackMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = BackpackLayout.WIDTH;
        imageHeight = BackpackLayout.height(menu.rows());
        titleLabelY = 6;
        inventoryLabelY = BackpackLayout.inventoryLabelY(menu.rows());
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        boolean anime = UiTheme.anime();
        if (anime) {
            UiPanel.panelScan(g, x, y, imageWidth, imageHeight, UiTheme.VIOLET, 0.6f, 1f, UiTheme.now());
        } else {
            g.fill(x, y, x + imageWidth, y + imageHeight, BG);
            g.renderOutline(x, y, imageWidth, imageHeight, BORDER);
        }
        int pack = menu.backpackSlots();
        for (Slot s : menu.slots) {
            int sx = x + s.x - 1, sy = y + s.y - 1;
            boolean mine = s.index < pack;
            boolean hover = mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18;
            if (anime) {
                UiPanel.well(g, sx, sy, 18, 18, mine ? UiTheme.GOLD : UiTheme.CYAN, mine ? 0.9f : 0.6f);
                if (hover) UiPanel.hover(g, sx + 1, sy + 1, 16, 16, 1f, UiTheme.GOLD);
            } else {
                g.fill(sx, sy, sx + 18, sy + 18, mine ? GOLD : SLOT_EDGE);
                g.fill(sx + 1, sy + 1, sx + 17, sy + 17, SLOT);
            }
        }
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
        renderTooltip(g, mouseX, mouseY);
    }
}
