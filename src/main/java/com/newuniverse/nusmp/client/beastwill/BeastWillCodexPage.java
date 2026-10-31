package com.newuniverse.nusmp.client.beastwill;

import com.newuniverse.nusmp.beastwill.BeastWillNet;
import com.newuniverse.nusmp.client.ui.CodexContext;
import com.newuniverse.nusmp.client.ui.CodexPage;
import com.newuniverse.nusmp.client.ui.UiDraw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Codex page of the Beast Will system (3.45.0): how to get a will, how it is learned, what the two forms are, what strain does and which keys do what, plus the wills the player holds
 * with their progress. Two columns of titled paragraphs, like the other text pages; the texts are lang keys {@code nusmp.beastwill.codex.*}. Appended after the stock pages (pages only ever go at the
 * end). Rebuilt each time the Codex opens, so the progress is the last one the server sent.
 */
public final class BeastWillCodexPage implements CodexPage {
    private static final String[] TOPICS = {"what", "get", "learn", "forms", "strain", "keys", "wills"};
    private List<String[]> entries;

    @Override public String title() { return Component.translatable("nusmp.beastwill.codex.title").getString(); }

    @Override public String glyph() { return "Bw"; }

    @Override public void opened() { entries = null; }

    private List<String[]> build() {
        if (entries != null) return entries;
        List<String[]> out = new ArrayList<>();
        BeastWillNet.Sync s = BeastWillClient.sync();
        if (s != null) {
            StringBuilder sb = new StringBuilder();
            for (BeastWillNet.Entry e : s.wills()) {
                sb.append(e.id().equals(s.active()) ? "▶ " : "  ").append(BeastWillClient.name(e.id())).append(": ")
                        .append(Component.translatable("nusmp.beastwill.codex.progress", e.assimilation(), e.mastery()).getString()).append('\n');
            }
            out.add(new String[]{Component.translatable("nusmp.beastwill.codex.yours").getString(), sb.toString().stripTrailing()});
        }
        for (String t : TOPICS) {
            String body = t.equals("keys")
                    ? Component.translatable("nusmp.beastwill.codex.keys.text", BeastWillClient.ACQUIRE.getTranslatedKeyMessage().getString(), BeastWillClient.INTEGRATE.getTranslatedKeyMessage().getString(),
                            BeastWillClient.ABILITY1.getTranslatedKeyMessage().getString(), BeastWillClient.ABILITY2.getTranslatedKeyMessage().getString(),
                            BeastWillClient.SWAP.getTranslatedKeyMessage().getString()).getString()
                    : Component.translatable("nusmp.beastwill.codex." + t + ".text").getString();
            out.add(new String[]{Component.translatable("nusmp.beastwill.codex." + t + ".title").getString(), body});
        }
        entries = out;
        return out;
    }

    @Override
    public int contentHeight(CodexContext ctx, int width) {
        List<String[]> list = build();
        int[] col = new int[2];
        for (int i = 0; i < list.size(); i++) col[i % 2] += 18 + UiDraw.wrappedHeight(ctx.font(), list.get(i)[1], width / 2 - 16) + 10;
        return Math.max(col[0], col[1]);
    }

    @Override
    public void render(CodexContext ctx, GuiGraphics g, int x, int y, int w, int h, int scroll, int mx, int my, float time) {
        List<String[]> list = build();
        int cw = w / 2 - 8;
        int[] cy = {y - scroll, y - scroll};
        for (int i = 0; i < list.size(); i++) {
            int col = i % 2, bx = col == 0 ? x : x + w / 2 + 8;
            int ny = UiDraw.header(g, ctx.font(), list.get(i)[0], bx, cy[col], cw - 6);
            cy[col] = UiDraw.wrapped(g, ctx.font(), list.get(i)[1], bx + 2, ny, cw - 10, UiDraw.ink()) + 8;
        }
    }
}
