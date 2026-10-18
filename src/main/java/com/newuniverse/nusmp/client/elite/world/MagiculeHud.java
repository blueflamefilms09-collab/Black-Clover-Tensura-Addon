package com.newuniverse.nusmp.client.elite.world;

import com.newuniverse.nusmp.elite.world.MagiculeClientCache;
import com.newuniverse.nusmp.elite.world.WorldItems;
import com.newuniverse.nusmp.elite.world.rules.MagiculeModel;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Why: the readout of the Magicule Gauge. While the gauge is in either hand a small panel at the top of the screen shows the density of the chunk you stand in (a bar and the band's name) and
 * the 3 x 3 chunks around you as a tiny map, so you can walk toward the rich ones. It only reads {@link MagiculeClientCache}, which the throttled sync fills; with no reading it says so.
 * Nothing is drawn without the item in hand, and nothing with the HUD hidden (F1).
 */
public final class MagiculeHud implements LayeredDraw.Layer {
    private static final int PANEL = 0xA0141020, FRAME = 0xFF5B4A8A, TRACK = 0xFF2A2440;

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.options.hideGui) return;
        if (!p.getMainHandItem().is(WorldItems.MAGICULE_GAUGE.get()) && !p.getOffhandItem().is(WorldItems.MAGICULE_GAUGE.get())) return;
        Font font = mc.font;
        int w = g.guiWidth(), x = w / 2 - 70, y = 12, pw = 140, ph = 40;
        g.fill(x - 1, y - 1, x + pw + 1, y + ph + 1, FRAME);
        g.fill(x, y, x + pw, y + ph, PANEL);
        if (!MagiculeClientCache.valid()) {
            g.drawString(font, Component.translatable("nusmp.elite.magicule.hud.none"), x + 6, y + 6, 0xFF9A93B8, false);
            return;
        }
        double d = MagiculeClientCache.own();
        MagiculeModel.Band band = MagiculeModel.Band.of(d);
        g.drawString(font, Component.translatable("nusmp.elite.magicule.hud.title"), x + 6, y + 5, 0xFFE6DDFF, false);
        String pct = String.format(Locale.ROOT, "%.0f%%", d * 100.0);
        g.drawString(font, Component.literal(pct + " ").append(Component.translatable("nusmp.elite.magicule.band." + band.id)), x + 6, y + 17, color(band), false);
        int bx = x + 6, by = y + 29, bw = 84;
        g.fill(bx, by, bx + bw, by + 5, TRACK);
        g.fill(bx, by, bx + (int) Math.round(bw * d), by + 5, color(band));
        // the 3 x 3 map: the player's chunk in the middle, north up
        int mx = x + pw - 40, my = y + 6;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                double c = MagiculeClientCache.cell(dx, dz);
                int cx = mx + (dx + 1) * 11, cy = my + (dz + 1) * 11;
                g.fill(cx, cy, cx + 10, cy + 10, color(MagiculeModel.Band.of(c)));
                if (dx == 0 && dz == 0) {
                    g.fill(cx, cy, cx + 10, cy + 1, 0xFFFFFFFF);
                    g.fill(cx, cy + 9, cx + 10, cy + 10, 0xFFFFFFFF);
                    g.fill(cx, cy, cx + 1, cy + 10, 0xFFFFFFFF);
                    g.fill(cx + 9, cy, cx + 10, cy + 10, 0xFFFFFFFF);
                }
            }
        }
    }

    private static int color(MagiculeModel.Band b) {
        return switch (b) {
            case THIN -> 0xFF5A5A7A;
            case NORMAL -> 0xFF7A6AD0;
            case RICH -> 0xFFA060E0;
            case DENSE -> 0xFFD060E8;
            case OVERFLOWING -> 0xFFFFE08A;
        };
    }
}
