package com.newuniverse.nusmp.client.elite.hub;

import com.newuniverse.nusmp.elite.hub.HubNet;
import com.newuniverse.nusmp.elite.hub.QuestText;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * Why: the brief's "GUI overlay to display quest objectives (e.g. '0/50 Tier 1 Abyssal Kills')". It is a GUI layer registered above everything ({@link HubClient}) that draws the player's accepted
 * quests in the top-right corner, under the potion effects: one block per quest with the progress line exactly as the brief writes it ({@code 12/50 Tier 1 Abyssal Kills}), a thin bar under it,
 * and, when the objective is met, a green "hand in at the Quest Giver" line instead. It respects what a HUD must respect: nothing is drawn when the GUI is hidden (F1), while any screen is open
 * (the dialogue and the inventory have their own), while the debug screen (F3) covers that corner, or when the player turned the tracker off with the key. It reads only what the server last sent
 * ({@link HubClient#hud}) and never touches the game state; at most five quests are shown, the rest as "+N more".
 */
final class QuestOverlay {
    private QuestOverlay() {}

    private static final int MAX_SHOWN = 5, WIDTH = 150, MARGIN = 6, TOP = 46;

    static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || (mc.screen != null && !com.newuniverse.nusmp.client.ui.move.GuiMove.editing()) || !HubClient.trackerShown) return;
        if (mc.getDebugOverlay().showDebugScreen()) return;
        List<HubNet.HudEntry> list = HubClient.hud;
        if (list.isEmpty()) return;
        Font font = mc.font;
        int x = mc.getWindow().getGuiScaledWidth() - WIDTH - MARGIN;
        int y = TOP;
        int shown = Math.min(MAX_SHOWN, list.size());
        int blockH = 22;
        com.newuniverse.nusmp.client.ui.move.GuiMove.report("quest_tracker", x - 4, y - 4, WIDTH + 8, 16 + shown * blockH + (list.size() > shown ? 10 : 0));     // GUI Move: where the tracker is drawn (the layer hook moves it)
        g.fill(x - 4, y - 4, x + WIDTH + 4, y + 12 + shown * blockH + (list.size() > shown ? 10 : 0), 0x80000000);
        g.drawString(font, "Quests", x, y, HubGui.GOLD, true);
        y += 12;
        for (int i = 0; i < shown; i++) {
            HubNet.HudEntry e = list.get(i);
            boolean ready = e.state() == 1;
            String line = ready ? HubGui.clip(font, e.title(), WIDTH) : HubGui.clip(font, QuestText.progressLine(e.progress(), e.target(), e.label()), WIDTH);
            g.drawString(font, line, x, y, ready ? HubGui.GOOD : HubGui.TEXT, true);
            if (ready) g.drawString(font, "Hand in at the Quest Giver", x, y + 10, HubGui.MUTED, true);
            else HubGui.bar(g, x, y + 11, WIDTH, 3, e.target() <= 0 ? 0.0 : e.progress() / (double) e.target(), HubGui.BAR_FG);
            y += blockH;
        }
        if (list.size() > shown) g.drawString(font, "+" + (list.size() - shown) + " more", x, y - 4, HubGui.MUTED, true);
    }
}
