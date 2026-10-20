package com.newuniverse.nusmp.client.requip;

import com.newuniverse.nusmp.client.ui.theme.MagicCircle;
import com.newuniverse.nusmp.client.ui.theme.ThemedButton;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.requip.ArmouryRules;
import com.newuniverse.nusmp.requip.LoadoutRules;
import com.newuniverse.nusmp.requip.PartialArmourMask;
import com.newuniverse.nusmp.requip.RequipArmoury;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Why: the window of the Requip Armoury, in the mod's GUI theme (docs/gui_theme.md): the anime look (glass panel, a gold circle turning behind the grid, glowing slot frames) or the classic flat look,
 * both the same in behaviour. Three tabs. <b>Pocket</b>: the 54-cell grid of the pocket space above the player's inventory (cells past the capacity are dimmed: they take nothing new but still give
 * theirs back; shift-click stores and takes). <b>Loadouts</b>: six rows with Save (what is worn now), Equip, Clear and the star that marks the Requip Reflex loadout. <b>Partial</b>: the eleven piece
 * families of an Erza armour as switches, the presets, On/Off and Apply. All state is the server's (the menu mirrors it); a button only sends the vanilla menu-button packet. The window is registered in
 * the GUI Size system ({@code requip_armoury}), so it can be resized in the options.
 */
public class RequipArmouryScreen extends AbstractContainerScreen<RequipArmoury.Menu> {
    private static final int BG = 0xF0101016, BORDER = 0xFF3B3B4C, GOLD = 0xFFE8C468, SLOT = 0xFF1C1C26, SLOT_EDGE = 0xFF55556A, DIM = 0xFF9A9AAE, LOCKED = 0xCC000000;
    private static final String[] SHORT = {"Helm", "Gorget", "Chest+back", "Belt+skirt", "Pauldrons", "Vambraces", "Gauntlets", "Cuisses", "Greaves", "Sabatons", "Extras"};

    private final ThemedButton[] tabs = new ThemedButton[3];
    private final ThemedButton[][] rows = new ThemedButton[LoadoutRules.MAX_LOADOUTS][4];     // save, equip, clear, reflex star
    private final ThemedButton[] families = new ThemedButton[PartialArmourMask.FAMILIES];
    private ThemedButton preset, onOff, apply;

    public RequipArmouryScreen(RequipArmoury.Menu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = RequipArmoury.Menu.WIDTH;
        imageHeight = RequipArmoury.Menu.HEIGHT;
        titleLabelX = 8;
        titleLabelY = -10;                                                                 // the title sits above the panel, the tabs are inside it
        inventoryLabelX = 8;
        inventoryLabelY = RequipArmoury.Menu.INV_Y - 11;
    }

    private static Component t(String key, String english, Object... args) { return Component.translatableWithFallback(key, english, args); }

    @Override
    protected void init() {
        super.init();
        String[] tabNames = {"Pocket", "Loadouts", "Partial"};
        for (int i = 0; i < 3; i++) {
            final int id = RequipArmoury.Menu.BTN_TAB + i;
            tabs[i] = addRenderableWidget(new ThemedButton(leftPos + 8 + i * 54, topPos + 6, 52, 14, t("nusmp.requip.tab." + tabNames[i].toLowerCase(), tabNames[i]), b -> press(id)));
        }
        for (int n = 0; n < LoadoutRules.MAX_LOADOUTS; n++) {
            final int row = n;
            int y = topPos + 34 + n * 17;
            rows[n][0] = addRenderableWidget(new ThemedButton(leftPos + 58, y, 30, 15, t("nusmp.requip.save", "Save"), b -> press(RequipArmoury.Menu.BTN_SAVE + row)));
            rows[n][1] = addRenderableWidget(new ThemedButton(leftPos + 90, y, 32, 15, t("nusmp.requip.equip", "Equip"), b -> press(RequipArmoury.Menu.BTN_EQUIP + row)));
            rows[n][2] = addRenderableWidget(new ThemedButton(leftPos + 124, y, 28, 15, t("nusmp.requip.clear", "Clear"), b -> press(RequipArmoury.Menu.BTN_CLEAR + row)));
            rows[n][3] = addRenderableWidget(new ThemedButton(leftPos + 154, y, 14, 15, Component.literal("*"), b -> press(RequipArmoury.Menu.BTN_REFLEX + row)));
        }
        for (int f = 0; f < PartialArmourMask.FAMILIES; f++) {
            final int bit = f;
            int x = leftPos + 8 + (f % 2) * 82, y = topPos + 32 + (f / 2) * 15;
            families[f] = addRenderableWidget(new ThemedButton(x, y, 78, 13, Component.literal(SHORT[f]), b -> press(RequipArmoury.Menu.BTN_FAMILY + bit)));
        }
        preset = addRenderableWidget(new ThemedButton(leftPos + 8, topPos + 124, 52, 14, t("nusmp.requip.preset_button", "Preset"), b -> press(RequipArmoury.Menu.BTN_PRESET)));
        onOff = addRenderableWidget(new ThemedButton(leftPos + 62, topPos + 124, 52, 14, Component.empty(), b -> press(RequipArmoury.Menu.BTN_PARTIAL_ON)));
        apply = addRenderableWidget(new ThemedButton(leftPos + 116, topPos + 124, 52, 14, t("nusmp.requip.apply", "Apply"), b -> press(RequipArmoury.Menu.BTN_APPLY)));
        updateWidgets();
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void containerTick() { updateWidgets(); }

    private void updateWidgets() {
        int tab = menu.tab();
        for (int i = 0; i < 3; i++) tabs[i].active = tab != i;
        for (int n = 0; n < LoadoutRules.MAX_LOADOUTS; n++) {
            boolean saved = menu.loadoutSaved(n);
            for (int k = 0; k < 4; k++) rows[n][k].visible = tab == RequipArmoury.Menu.TAB_LOADOUTS;
            rows[n][1].active = saved;
            rows[n][2].active = saved;
            rows[n][3].active = saved;
        }
        for (int f = 0; f < PartialArmourMask.FAMILIES; f++) {
            families[f].visible = tab == RequipArmoury.Menu.TAB_PARTIAL;
            boolean on = (menu.mask() & (1 << f)) != 0;
            families[f].setMessage(Component.literal((on ? "[x] " : "[ ] ") + SHORT[f]));
        }
        preset.visible = onOff.visible = apply.visible = tab == RequipArmoury.Menu.TAB_PARTIAL;
        onOff.setMessage(menu.partialOn() ? t("nusmp.requip.partial_on", "On") : t("nusmp.requip.partial_off", "Off"));
    }

    // ---------------------------------------------------------------- drawing
    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        boolean anime = UiTheme.anime();
        float now = UiTheme.now();
        if (anime) {
            UiPanel.panelScan(g, x, y, imageWidth, imageHeight, UiTheme.CYAN, 0.8f, 1f, now);
            g.enableScissor(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1);
            MagicCircle.draw(g, x + imageWidth / 2f, y + 86f, 62f, now, UiTheme.GOLD, 0.30f, MagicCircle.CALM, 1f);
            g.disableScissor();
        } else {
            g.fill(x, y, x + imageWidth, y + imageHeight, BG);
            g.renderOutline(x, y, imageWidth, imageHeight, BORDER);
        }
        int cap = menu.capacity();
        for (Slot s : menu.slots) {
            boolean cell = s.index < RequipArmoury.Menu.CELLS;
            if (cell && !s.isActive()) continue;
            int sx = x + s.x - 1, sy = y + s.y - 1;
            boolean locked = cell && s.index >= cap;
            boolean hover = mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18;
            if (anime) {
                UiPanel.well(g, sx, sy, 18, 18, locked ? UiTheme.TEXT_FAINT : UiTheme.CYAN, locked ? 0.4f : 0.7f);
                if (hover && !locked) UiPanel.hover(g, sx + 1, sy + 1, 16, 16, 1f, UiTheme.CYAN);
            } else {
                g.fill(sx, sy, sx + 18, sy + 18, SLOT_EDGE);
                g.fill(sx + 1, sy + 1, sx + 17, sy + 17, SLOT);
            }
            if (locked) g.fill(sx + 1, sy + 1, sx + 17, sy + 17, LOCKED);
            if (cell && s.index == menu.selectedCell()) UiPanel.brackets(g, sx - 1, sy - 1, 20, 20, 5, UiMath.withAlpha(UiTheme.GOLD, 1f));
            if (cell && s.index == menu.bladeCell()) UiPanel.brackets(g, sx - 1, sy - 1, 20, 20, 3, UiMath.withAlpha(UiTheme.CYAN, 0.9f));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        boolean anime = UiTheme.anime();
        int ink = anime ? UiTheme.GOLD : GOLD, dim = anime ? UiTheme.TEXT_DIM : DIM;
        g.drawString(font, title, titleLabelX, titleLabelY, anime ? UiTheme.GOLD : 0xFFFFFFFF, true);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, dim, false);
        int tab = menu.tab();
        if (tab == RequipArmoury.Menu.TAB_POCKET) {
            Component cap = t("nusmp.requip.capacity", "%s / %s cells", menu.capacity(), ArmouryRules.MAX_SLOTS);
            g.drawString(font, cap, imageWidth - 8 - font.width(cap), RequipArmoury.Menu.Y0 + ArmouryRules.ROWS * 18 + 2, dim, false);
            g.drawString(font, t("nusmp.requip.hint_pocket", "Shift-click stores and takes"), 8, RequipArmoury.Menu.Y0 + ArmouryRules.ROWS * 18 + 2, dim, false);
        } else if (tab == RequipArmoury.Menu.TAB_LOADOUTS) {
            for (int n = 0; n < LoadoutRules.MAX_LOADOUTS; n++) {
                boolean saved = menu.loadoutSaved(n), active = menu.activeLoadout() == n, reflex = menu.reflexLoadout() == n;
                Component label = saved ? t("nusmp.requip.row_saved", "%s: %s pcs", n + 1, menu.loadoutPieces(n)) : t("nusmp.requip.row_empty", "%s: empty", n + 1);
                g.drawString(font, (active ? "> " : "") + label.getString(), 8, 38 + n * 17, active ? ink : dim, false);
                if (reflex) g.drawString(font, "*", 172 - 3, 38 + n * 17 - 8, ink, false);
            }
            g.drawString(font, t("nusmp.requip.hint_loadouts", "Save stores what you wear. * = Reflex loadout."), 8, 38 + 6 * 17, dim, false);
        } else {
            g.drawString(font, t("nusmp.requip.hint_partial", "Pieces of an Erza armor; Apply forges them."), 8, 142, dim, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
    }
}
