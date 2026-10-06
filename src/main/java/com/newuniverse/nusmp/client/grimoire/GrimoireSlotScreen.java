package com.newuniverse.nusmp.client.grimoire;

import com.newuniverse.nusmp.blackclover.GrimoireSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** The Grimoire Slot screen: one gold-framed slot above the player's inventory, drawn in the Multiverse panel colours (no texture). */
public class GrimoireSlotScreen extends AbstractContainerScreen<GrimoireSlot.Menu> {
    private static final int BG = 0xF0101016, BORDER = 0xFF3B3B4C, GOLD = 0xFFE8C468, SLOT = 0xFF1C1C26, SLOT_EDGE = 0xFF55556A, DIM = 0xFF9A9AAE;

    public GrimoireSlotScreen(GrimoireSlot.Menu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = GrimoireSlot.Menu.INV_Y + 58 + 24;
        inventoryLabelY = GrimoireSlot.Menu.INV_Y - 11;
        titleLabelY = 6;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, BG);
        g.renderOutline(x, y, imageWidth, imageHeight, BORDER);
        for (Slot s : menu.slots) {
            int sx = x + s.x - 1, sy = y + s.y - 1;
            boolean grimoire = s.index == 0;
            if (grimoire) {
                g.fill(sx - 3, sy - 3, sx + 21, sy + 21, GOLD);
                g.fill(sx - 2, sy - 2, sx + 20, sy + 20, BG);
            }
            g.fill(sx, sy, sx + 18, sy + 18, SLOT_EDGE);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, SLOT);
        }
        if (!menu.slots.get(0).hasItem())
            g.drawCenteredString(font, "empty: no grimoire bound", x + imageWidth / 2, y + GrimoireSlot.Menu.SLOT_Y + 22, DIM);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, GOLD, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, DIM, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
    }
}
