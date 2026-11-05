package com.newuniverse.nusmp.client.beastwill;

import com.newuniverse.nusmp.beastwill.BeastWillNet;
import com.newuniverse.nusmp.beastwill.BeastWillRules;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: 3.52.0, the Beast Will lives in the main menu (the Multiverse status screen) instead of its own HUD panel and pop-up. This is the read-only door the menu uses: it draws the same panel as
 * the old HUD ({@link BeastWillClient#paint}), draws the absorb ask as a page, and forwards the buttons to the same packets the keys use. It keeps no state of its own (everything is
 * {@link BeastWillClient}'s cache), decides nothing (the server validates every ask) and costs nothing while the menu is closed.
 */
public final class BeastWillMenu {
    private BeastWillMenu() {}

    public static final int W = 176;

    /** True when the player holds a Beast Will (the buttons make sense). */
    public static boolean has() { BeastWillNet.Sync s = BeastWillClient.sync(); return s != null && !s.active().isEmpty(); }

    /** True while an absorb ask waits for Absorb / Decline. */
    public static boolean asking() { return BeastWillClient.pending() != null; }

    /** The ask may be accepted (the server said ok). */
    public static boolean askOk() { BeastWillNet.Preview p = BeastWillClient.pending(); return p != null && p.ok(); }

    public static void acquire() { BeastWillClient.ask(BeastWillNet.OP_FORM, BeastWillRules.ACQUIRE); }

    public static void integrate() { BeastWillClient.ask(BeastWillNet.OP_FORM, BeastWillRules.INTEGRATE); }

    public static void swap() { BeastWillClient.ask(BeastWillNet.OP_SWAP, 1); }

    public static void answer(boolean absorb) { BeastWillNet.Preview p = BeastWillClient.pending(); if (p != null) BeastWillClient.answer(absorb && p.ok()); }

    /** Closing the menu with the ask unanswered declines it, as closing the old pop-up did. */
    public static void declineIfOpen() { if (asking()) BeastWillClient.answer(false); }

    /** Two lines for the status grid / compact panel (null = no will): the will with its form, and the three numbers. */
    public static String[] summary() {
        BeastWillNet.Sync s = BeastWillClient.sync();
        if (s == null || s.active().isEmpty()) return null;
        return new String[]{BeastWillClient.name(s.active()) + "  " + BeastWillClient.t("form", BeastWillClient.t("phase." + (s.phase() == BeastWillRules.INTEGRATE ? "integrate" : s.phase() == BeastWillRules.ACQUIRE ? "acquire" : "off"))),
                BeastWillClient.t("assimilation", s.assimilation()) + "  " + BeastWillClient.t("mastery", s.mastery()), BeastWillClient.t("strain", s.strain(), "")};
    }

    /** Height of what {@link #draw} paints. */
    public static int height() {
        BeastWillNet.Sync s = BeastWillClient.sync();
        if (BeastWillClient.pending() != null) return 150;
        if (s == null || s.active().isEmpty()) return 40;
        Minecraft mc = Minecraft.getInstance();
        return BeastWillClient.height(lines(s, mc.level == null ? 0 : mc.level.getGameTime()));
    }

    private static BeastWillClient.Lines lines(BeastWillNet.Sync s, long now) { return BeastWillClient.build(s, BeastWillClient.info(), now); }

    /** Draws the page with its top-left at (x, y): the ask if one waits, else the will, else a line saying there is none. */
    public static void draw(GuiGraphics g, Font font, int x, int y) {
        BeastWillNet.Preview p = BeastWillClient.pending();
        if (p != null) { ask(g, font, x, y, p); return; }
        BeastWillNet.Sync s = BeastWillClient.sync();
        if (s == null || s.active().isEmpty()) {
            UiPanel.panel(g, x, y, W, 40, UiTheme.CYAN, 0.3f, 0.92f);
            int cy = y + 8;
            for (String line : wrap(font, Component.translatable("nusmp.beastwill.menu.none").getString(), W - 14)) {
                g.drawString(font, line, x + 7, cy, UiTheme.TEXT_DIM, false);
                cy += 10;
            }
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        BeastWillClient.paint(g, font, x, y, lines(s, mc.level == null ? 0 : mc.level.getGameTime()), s, BeastWillClient.info());
    }

    private static void ask(GuiGraphics g, Font font, int x, int y, BeastWillNet.Preview p) {
        UiPanel.panel(g, x, y, W, 150, p.ok() ? UiTheme.CYAN : UiTheme.RED, 0.8f, 0.95f);
        int cx = x + 7, iw = W - 14, cy = y + 6;
        g.drawString(font, BeastWillClient.name(p.will()), cx, cy, UiTheme.WHITE, true);
        String rank = Component.translatable("nusmp.beastwill.hud.rank", BeastWillClient.rankName(p.rank())).getString();
        g.drawString(font, rank, x + W - 7 - font.width(rank), cy, UiTheme.GOLD, false);
        cy += 11;
        g.drawString(font, font.plainSubstrByWidth(Component.translatable("nusmp.beastwill.confirm.from", p.beast(), p.affinity()).getString(), iw), cx, cy, UiTheme.TEXT_DIM, false);
        cy += 10;
        g.drawString(font, Component.translatable(p.source() == 0 ? "nusmp.beastwill.confirm.source_core" : "nusmp.beastwill.confirm.source_offer"), cx, cy, UiTheme.TEXT_DIM, false);
        cy += 13;
        g.drawString(font, Component.translatable("nusmp.beastwill.confirm.ep", Math.round(p.haveEp()), Math.round(p.needEp())), cx, cy, p.haveEp() >= p.needEp() ? UiTheme.GREEN : UiTheme.RED, false);
        cy += 10;
        g.drawString(font, Component.translatable("nusmp.beastwill.confirm.fit", p.fitPercent()), cx, cy, p.fitPercent() >= 100 ? UiTheme.GREEN : UiTheme.GOLD, false);
        cy += 13;
        String text = p.ok() ? Component.translatable("nusmp.beastwill.confirm.warning").getString() : Component.translatable("nusmp.beastwill.msg.no." + p.reason()).getString();
        for (String line : wrap(font, text, iw)) {
            g.drawString(font, line, cx, cy, p.ok() ? UiTheme.TEXT : UiTheme.RED, false);
            cy += 10;
        }
    }

    private static List<String> wrap(Font font, String text, int width) {
        List<String> out = new ArrayList<>();
        for (var seq : font.split(Component.literal(text), width)) {
            StringBuilder sb = new StringBuilder();
            seq.accept((i, style, cp) -> { sb.appendCodePoint(cp); return true; });
            out.add(sb.toString());
            if (out.size() >= 6) break;
        }
        return out;
    }
}
