package com.newuniverse.nusmp.client.grimoire;

import com.newuniverse.nusmp.blackclover.GrimoireSlot;
import com.newuniverse.nusmp.client.multiverse.MultiverseStatusClient;
import com.newuniverse.nusmp.client.ui.theme.MagicCircle;
import com.newuniverse.nusmp.client.ui.theme.SageBlock;
import com.newuniverse.nusmp.client.ui.theme.UiLang;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The Grimoire Slot screen: one gold-framed slot above the player's inventory. Classic look: the Multiverse panel colours (no texture).
 * Anime look (config ui.theme, default): a translucent panel with a gold magic circle turning behind the slot, glowing slot frames, a hover
 * highlight, and a Great-Sage readout panel under the window. The slot, menu and clicks are the same in both.
 */
public class GrimoireSlotScreen extends AbstractContainerScreen<GrimoireSlot.Menu> {
    private static final int BG = 0xF0101016, BORDER = 0xFF3B3B4C, GOLD = 0xFFE8C468, SLOT = 0xFF1C1C26, SLOT_EDGE = 0xFF55556A, DIM = 0xFF9A9AAE;

    public GrimoireSlotScreen(GrimoireSlot.Menu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = GrimoireSlot.Menu.INV_Y + 58 + 24;
        inventoryLabelY = GrimoireSlot.Menu.INV_Y - 11;
        titleLabelY = 6;
    }

    private final SageBlock sage = new SageBlock();

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        if (UiTheme.anime()) { renderBgAnime(g, mouseX, mouseY); return; }
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

    /** The anime look of the window (drawing only). */
    private void renderBgAnime(GuiGraphics g, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        float now = UiTheme.now();
        UiPanel.panelScan(g, x, y, imageWidth, imageHeight, UiTheme.CYAN, 0.8f, 1f, now);
        Slot book = menu.slots.get(0);
        float cx = x + book.x + 8f, cy = y + book.y + 8f;
        g.enableScissor(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1);
        MagicCircle.draw(g, cx, cy, 34f, now, UiTheme.GOLD, book.hasItem() ? 0.65f : 0.35f, MagicCircle.STANDARD, 1f);
        g.disableScissor();
        for (Slot s : menu.slots) {
            int sx = x + s.x - 1, sy = y + s.y - 1;
            boolean hover = mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18;
            if (s.index == 0) {
                UiPanel.brackets(g, sx - 4, sy - 4, 26, 26, 6, UiMath.withAlpha(UiTheme.GOLD, hover ? 1f : 0.85f));
                UiPanel.well(g, sx - 1, sy - 1, 20, 20, UiTheme.GOLD, 1f);
            } else {
                UiPanel.well(g, sx, sy, 18, 18, UiTheme.CYAN, 0.7f);
            }
            if (hover) UiPanel.hover(g, sx + 1, sy + 1, 16, 16, 1f, s.index == 0 ? UiTheme.GOLD : UiTheme.CYAN);
        }
        // the Great-Sage readout under the window
        CompoundTag gr = MultiverseStatusClient.status().getCompound("Grimoire");
        String head, body, result = "";
        if (book.hasItem()) {
            head = UiLang.get(UiLang.ANALYSIS_COMPLETE);
            body = UiLang.get("nusmp.ui.slot.bound");
            if (gr.getInt("Pages") > 0) result = UiLang.get(UiLang.RESULT, UiLang.get("nusmp.ui.slot.pages", gr.getInt("Unlocked"), gr.getInt("Pages")));
        } else {
            head = UiLang.get(UiLang.ANALYZING);
            body = UiLang.get(UiLang.NOTICE, UiLang.get("nusmp.ui.slot.empty"));
        }
        sage.set(font, head, body, result, imageWidth - 16, now);
        int ph = sage.height(font) + 10, py = y + imageHeight + 5;
        UiPanel.panel(g, x, py, imageWidth, ph, UiTheme.CYAN, 0.5f, 1f);
        sage.draw(g, font, x + 8, py + 5, now, 1f);
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
