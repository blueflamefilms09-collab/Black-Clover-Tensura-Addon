package com.newuniverse.nusmp.avatar.inventory.client;

import com.newuniverse.nusmp.avatar.inventory.AvatarInventoryMenu;
import com.newuniverse.nusmp.avatar.inventory.InventoryLayout;
import com.newuniverse.nusmp.client.ui.theme.ThemedButton;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Why: the window of the Avatar's gear. It shows the Avatar itself (turning to follow the mouse, like the player's own inventory), its two hands and four armour
 * pieces beside it, the eight trinket slots, the eighteen pockets, and the creator's own inventory below; four buttons give the orders Follow, Stay, Delve and Share
 * (the chosen stance is bracketed in gold; Share is greyed out when there is nothing worth handing over). It draws in the mod's anime look when that theme is on and in
 * plain panels otherwise. Slots, clicks and orders are the menu's; this class only draws and forwards the button presses through the vanilla menu-button packet.
 */
public class AvatarInventoryScreen extends AbstractContainerScreen<AvatarInventoryMenu> {
    private static final int BG = 0xF0101016, BORDER = 0xFF3B3B4C, GOLD = 0xFFE8C468, SLOT = 0xFF1C1C26, SLOT_EDGE = 0xFF55556A, DIM = 0xFF9A9AAE;
    private static final String[] BUTTON_KEYS = {"nusmp.avatar.inv.follow", "nusmp.avatar.inv.stay", "nusmp.avatar.inv.delve", "nusmp.avatar.inv.share"};

    private final ThemedButton[] buttons = new ThemedButton[InventoryLayout.BUTTONS];

    public AvatarInventoryScreen(AvatarInventoryMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = InventoryLayout.WIDTH;
        imageHeight = InventoryLayout.HEIGHT;
        titleLabelX = 8;
        titleLabelY = 4;
        inventoryLabelX = 8;
        inventoryLabelY = InventoryLayout.PLAYER_Y - 8;                      // just under the second row of pockets (they end at y 146), just above the creator's rows
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < buttons.length; i++) {
            final int id = i;
            buttons[i] = addRenderableWidget(new ThemedButton(leftPos + InventoryLayout.buttonX(i), topPos + InventoryLayout.BUTTON_Y, InventoryLayout.BUTTON_W,
                    InventoryLayout.BUTTON_H, Component.translatable(BUTTON_KEYS[i]), b -> press(id)));
        }
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void containerTick() {
        if (buttons[InventoryLayout.BUTTONS - 1] != null) buttons[InventoryLayout.BUTTONS - 1].active = menu.valuables() > 0;
    }

    // ---------------------------------------------------------------- drawing
    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        boolean anime = UiTheme.anime();
        if (anime) {
            UiPanel.panelScan(g, x, y, imageWidth, imageHeight, UiTheme.CYAN, 0.8f, 1f, UiTheme.now());
        } else {
            g.fill(x, y, x + imageWidth, y + imageHeight, BG);
            g.renderOutline(x, y, imageWidth, imageHeight, BORDER);
        }
        for (Slot s : menu.slots) {
            int sx = x + s.x - 1, sy = y + s.y - 1;
            int accent = accentOf(s.index);
            if (anime) UiPanel.well(g, sx, sy, 18, 18, accent, 0.75f);
            else {
                g.fill(sx, sy, sx + 18, sy + 18, SLOT_EDGE);
                g.fill(sx + 1, sy + 1, sx + 17, sy + 17, SLOT);
            }
            boolean hover = mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18;
            if (hover && anime) UiPanel.hover(g, sx + 1, sy + 1, 16, 16, 1f, accent);
        }
        // the Avatar itself
        Entity e = minecraft != null && minecraft.level != null ? minecraft.level.getEntity(menu.avatarEntityId()) : null;
        if (e instanceof LivingEntity living) {
            int px = x + InventoryLayout.PREVIEW_X, py = y + InventoryLayout.PREVIEW_Y;
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, px, py, px + InventoryLayout.PREVIEW_W, py + InventoryLayout.PREVIEW_H, 28, 0.0625f, mouseX, mouseY, living);
        }
        // the chosen stance, bracketed
        int chosen = switch (menu.stance()) {
            case FOLLOW -> 0;
            case STAY -> 1;
            case DELVE -> 2;
        };
        UiPanel.brackets(g, x + InventoryLayout.buttonX(chosen) - 2, y + InventoryLayout.BUTTON_Y - 2, InventoryLayout.BUTTON_W + 4, InventoryLayout.BUTTON_H + 4, 4, GOLD);
    }

    /** The colour of a slot's frame: gold for the hands, cyan for armour, violet for trinkets, grey for pockets and the creator's own inventory. */
    private static int accentOf(int index) {
        if (index == InventoryLayout.MAIN || index == InventoryLayout.OFF) return UiTheme.GOLD;
        if (InventoryLayout.isArmor(index)) return UiTheme.CYAN;
        if (InventoryLayout.isTrinket(index)) return UiTheme.VIOLET;
        return UiMath.withAlpha(UiTheme.TEXT_DIM, 0.9f);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        boolean anime = UiTheme.anime();
        g.drawString(font, title, titleLabelX, titleLabelY, anime ? UiTheme.GOLD : GOLD, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, anime ? UiTheme.TEXT_DIM : DIM, false);
        g.drawString(font, Component.translatable("nusmp.avatar.inv.party", menu.partySize()), 90, 79, anime ? UiTheme.TEXT_DIM : DIM, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
    }
}
