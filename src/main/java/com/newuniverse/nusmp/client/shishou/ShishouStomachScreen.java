package com.newuniverse.nusmp.client.shishou;

import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.shishou.ShishouGuiLayout;
import com.newuniverse.nusmp.shishou.ShishouNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;

/**
 * Why: the Stomach (Shishou's internal storage) in the Akashic look (2.1.0): a dark star-field panel with a glowing cyan frame, fine prismatic line-work, drifting light motes, the sword emblem, and his
 * level, experience and mana as two glowing gauges in the title row. The menu is vanilla's chest menu (the server opens {@code ChestMenu} over Stomach, so nothing changes on the server and nothing is sent
 * that was not before): {@link ShishouClient} swaps the vanilla chest screen for this one when its title is Stomach's. The slots stay exactly where the menu puts them; only the drawing is new. Draw cost is
 * bounded ({@link AkashicTheme}); the panel is registered as movable and sizeable ({@code shishou_stomach}), and nothing is drawn before {@code super.render} (the blur guard).
 */
public class ShishouStomachScreen extends AbstractContainerScreen<ChestMenu> {
    private final int rows;

    public ShishouStomachScreen(ChestMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.rows = menu.getRowCount();
        this.imageWidth = ShishouGuiLayout.STOMACH_WIDTH;
        this.imageHeight = ShishouGuiLayout.stomachHeight(rows);
        this.inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        SafeCast.client("shishou.stomach.draw", () -> drawBg(g));
    }

    private void drawBg(GuiGraphics g) {
        boolean flat = !UiTheme.anime();
        float now = UiTheme.now();
        int x = leftPos, y = topPos;
        AkashicTheme.panel(g, x, y, imageWidth, imageHeight, now, flat, ShishouGuiLayout.FIRST_SLOT_Y - 2);
        if (!flat) AkashicTheme.motes(g, x + 2, y + 2, imageWidth - 4, imageHeight - 4, now, ShishouGuiLayout.MOTES_PANEL);
        for (Slot s : menu.slots) if (s.index % 9 == 0) AkashicTheme.slotRow(g, x + s.x - 1, y + s.y - 1);       // one blit per row of nine cells
        AkashicTheme.emblem(g, x + ShishouGuiLayout.EMBLEM_X, y + ShishouGuiLayout.EMBLEM_Y, ShishouGuiLayout.EMBLEM_SIZE);
        ShishouNet.Sync s = ShishouClientState.state();
        float exp = s == null ? 0f : s.expShare() / 100f, mana = s == null || s.maxMana() <= 0 ? 0f : s.mana() / (float) s.maxMana();
        boolean low = s != null && (s.flags() & ShishouNet.Sync.F_LOW) != 0;
        int gx = x + ShishouGuiLayout.GAUGE_X;
        AkashicTheme.gauge(g, gx, y + ShishouGuiLayout.EXP_GAUGE_Y, ShishouGuiLayout.GAUGE_W, ShishouGuiLayout.GAUGE_H, exp, ShishouGuiLayout.EXP_LOW, ShishouGuiLayout.EXP_HIGH, now, flat);
        AkashicTheme.gauge(g, gx, y + ShishouGuiLayout.MANA_GAUGE_Y, ShishouGuiLayout.GAUGE_W, ShishouGuiLayout.GAUGE_H, mana, low ? 0x8A2A2A : ShishouGuiLayout.MANA_LOW, low ? ShishouGuiLayout.LOW : ShishouGuiLayout.MANA_HIGH, now, flat);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        SafeCast.client("shishou.stomach.labels", () -> {
            ShishouNet.Sync s = ShishouClientState.state();
            g.drawString(font, Component.translatableWithFallback("shishou.gui.stomach", "Stomach"), ShishouGuiLayout.TITLE_X, ShishouGuiLayout.TITLE_Y, 0xFF000000 | ShishouGuiLayout.CYAN_HI, true);
            if (s != null) {
                String lv = Component.translatableWithFallback("shishou.hud.level", "Lv %s", s.level()).getString();
                g.drawString(font, lv, ShishouGuiLayout.LEVEL_X, ShishouGuiLayout.TITLE_Y, 0xFF000000 | ShishouGuiLayout.GOLD, true);
            }
            g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, UiTheme.TEXT_DIM, false);
        });
    }
}
