package com.newuniverse.nusmp.client.elite.records;

import com.newuniverse.nusmp.elite.records.RecordsNet;
import com.newuniverse.nusmp.elite.records.RecordsText;
import com.newuniverse.nusmp.elite.records.rule.Rarity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Why: the owner asked for "a toast when a record unlocks". The vanilla advancement toast cannot show a rarity, so this is a small overlay of the mod's own, drawn as a GUI layer (the same way the Arts
 * bar and the page-unlock panel are): a dark card with a bar and an edge in the rarity colour that slides in from the right edge, stays about four seconds, slides out, and shows the next queued toast
 * (several unlocks in one beat queue up instead of overlapping). The server sends one {@code Toast} packet per unlock; this class only draws what {@link RecordsClientState} queued. A draw that throws
 * switches the overlay off for the session with one log line instead of one per frame. Client thread only.
 */
final class RecordToastOverlay {
    private RecordToastOverlay() {}

    private static final int W = 190, H = 34, SLIDE_MS = 350, HOLD_MS = 4000;
    private static RecordsNet.Toast current;
    private static long startedAt;
    private static boolean broken;

    static void reset() {
        current = null;
        broken = false;
    }

    static void render(GuiGraphics g, DeltaTracker delta) {
        if (broken) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options.hideGui) return;
            long now = System.currentTimeMillis();
            if (current == null) {
                current = RecordsClientState.pollToast();
                if (current == null) return;
                startedAt = now;
            }
            long age = now - startedAt;
            long total = SLIDE_MS * 2L + HOLD_MS;
            if (age >= total) {
                current = null;
                return;
            }
            double slide = age < SLIDE_MS ? age / (double) SLIDE_MS : age > SLIDE_MS + HOLD_MS ? 1.0 - (age - SLIDE_MS - HOLD_MS) / (double) SLIDE_MS : 1.0;
            slide = 1.0 - Math.pow(1.0 - slide, 3);                                  // ease out
            int sw = g.guiWidth();
            int x = sw - (int) Math.round((W + 6) * slide), y = 8;
            draw(g, mc.font, x, y, current);
        } catch (RuntimeException e) {
            broken = true;
            com.mojang.logging.LogUtils.getLogger().warn("[nusmp] The record toast overlay failed and is switched off for this session", e);
        }
    }

    private static void draw(GuiGraphics g, Font font, int x, int y, RecordsNet.Toast t) {
        int color = 0xFF000000 | Rarity.byOrdinal(t.rarity()).color;
        g.fill(x, y, x + W, y + H, 0xF0101018);
        g.fill(x, y, x + 4, y + H, color);
        g.fill(x + 4, y, x + W, y + 1, color & 0x80FFFFFF | 0x80000000);
        g.fill(x + 4, y + H - 1, x + W, y + H, color & 0x80FFFFFF | 0x80000000);
        g.fill(x + W - 1, y, x + W, y + H, color & 0x80FFFFFF | 0x80000000);
        String head = RecordsText.str(t.kind() == 0 ? RecordsText.TOAST_RECORD : RecordsText.TOAST_TITLE);
        g.drawString(font, head, x + 10, y + 6, 0xFF9A9AB0, false);
        String rarity = RecordsText.str("gui.nusmp.records.rarity." + Rarity.byOrdinal(t.rarity()).id());
        g.drawString(font, rarity, x + W - 6 - font.width(rarity), y + 6, color, false);
        String name = font.plainSubstrByWidth(t.name(), W - 18);
        g.drawString(font, name, x + 10, y + 19, color, false);
    }
}
