package com.newuniverse.nusmp.client.hub;

import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.client.ui.theme.UiThemeOptions;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Why: the owner wants the new Multiverse Hub to sit in the Tensura menu's "Coming Soon" spot the same way the old menu did (the Four Kingdoms logo on the magic categories screen, the compact panel on the
 * status screen). This draws the hub's own picture there: a small turning ring with the eight tab nodes in their colours around a core, and a compact title card. Pure client drawing, only while one of those two
 * screens is open and {@code ui.hub} is on ({@code ui.hub = false} keeps the old logo and panel). Budget: the emblem is one {@link HubFx} dotted ring (at most 48 fills), 8 small octagons (about 8 x 3 fills) and one
 * MagicCircle (at most 8 textured quads); the card adds one panel and two lines of text. No allocation per frame, nothing when the screen is closed, no packet.
 */
public final class HubEmblem {
    private HubEmblem() {}

    /** Radians per second the ring turns (0 with {@code ui.hubFx} or {@code ui.motion} off). */
    private static final float TURN = 0.35f;

    /** True when the emblem replaces the old picture ({@code ui.hub} on). */
    public static boolean on() { return UiThemeOptions.hub(); }

    /** The ring emblem inside the rectangle (x, y, w, h), centred and square. Nodes sit evenly on the ring, node 0 at the top, in the order of {@link HubNodes.Tab}. */
    public static void draw(GuiGraphics g, int x, int y, int w, int h, boolean hover) {
        float cx = x + w / 2f, cy = y + h / 2f, r = Math.min(w, h) / 2f - 2f;
        if (r < 6f) return;
        float now = UiTheme.now();
        float rot = UiThemeOptions.hubFx() && UiTheme.motion() ? now * TURN : 0f;
        HubFx.core(g, cx, cy, r * 0.55f, now, hover ? UiTheme.GOLD : UiTheme.CYAN, hover ? 1f : 0.6f, 1f);
        HubFx.ringB(g, cx, cy, r, rot, hover ? UiTheme.GOLD : UiTheme.CYAN, hover ? 1f : 0.7f, 1f);
        int nr = Math.max(2, Math.round(r / 6f));
        for (int i = 0; i < HubNodes.COUNT; i++) {
            double a = -Math.PI / 2 + i * 2 * Math.PI / HubNodes.COUNT + rot;
            int px = Math.round(cx + r * (float) Math.cos(a)), py = Math.round(cy + r * (float) Math.sin(a));
            HubNodes.Tab t = HubNodes.at(i);
            HubFx.octagon(g, px, py, nr, UiMath.withAlpha(UiMath.mix(0xFF0A1024, 0xFF000000 | t.rgb, 0.55f), 0.95f));
            HubFx.octagonEdge(g, px, py, nr, UiMath.withAlpha(t.rgb, hover ? 1f : 0.85f));
        }
        int cr = Math.max(2, nr - 1);
        HubFx.octagon(g, Math.round(cx), Math.round(cy), cr, UiMath.withAlpha(hover ? UiTheme.GOLD : UiTheme.WHITE, 0.9f));
    }

    /** The compact card for the status screen's placeholder area: the emblem on the left, the title and the way to open it on the right. */
    public static void drawCard(GuiGraphics g, Font font, int x, int y, int w, int h, boolean hover) {
        if (UiTheme.anime()) UiPanel.panel(g, x, y, w, h, hover ? UiTheme.GOLD : UiTheme.CYAN, hover ? 1f : 0.4f, 1f);
        else {
            g.fill(x, y, x + w, y + h, 0xE0101016);
            g.renderOutline(x, y, w, h, hover ? UiTheme.GOLD : 0xFF3B3B4C);
        }
        int side = h - 4;
        draw(g, x + 2, y + 2, side, side, hover);
        int tx = x + side + 8, tw = x + w - 4 - tx;
        if (tw < 20) return;
        Component title = Component.translatable("anime_multiverse.hub.title");
        Component hint = Component.translatable("anime_multiverse.hub.slot.hint", MultiverseHubClient.KEY.getTranslatedKeyMessage());
        float sc = Math.min(1f, tw / (float) Math.max(1, font.width(title)));
        g.pose().pushPose();
        g.pose().translate(tx, y + h / 2f - 10, 0);
        g.pose().scale(sc, sc, 1f);
        g.drawString(font, title, 0, 0, hover ? 0xFFFFE9A8 : UiTheme.GOLD, true);
        g.pose().popPose();
        float sh = Math.min(0.8f, tw / (float) Math.max(1, font.width(hint)));
        g.pose().pushPose();
        g.pose().translate(tx, y + h / 2f + 2, 0);
        g.pose().scale(sh, sh, 1f);
        g.drawString(font, hint, 0, 0, 0xFF9A9AAE, false);
        g.pose().popPose();
    }
}
