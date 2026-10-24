package com.newuniverse.nusmp.client.ui.gs;

import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.client.ui.theme.UiThemeOptions;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The alert crest and its notices ("[Notice... Great Sage has ...]") of the great_spirit look. Client only: it reads what the client already has
 * (the synced status, chat lines it receives) and sends nothing. A GUI layer above everything draws the crest where the player put it
 * ({@code ui.greatSpirit.noticePos}, moved on {@link GsCrestScreen}) and up to {@link GsNoticeQueue#VISIBLE} notices beside it; the queue is
 * bounded (16 waiting, 4 new per second, duplicates refused). Chat is only mirrored for the keys of {@link GsNoticeAllow} and is never cancelled.
 * Cleared on logout. Worst case per frame: 2 crest quads, 3 cards (3 fills, 3 outlines, 3 to 6 short text lines).
 */
public final class GsNotices {
    private GsNotices() {}

    static final GsNoticeQueue QUEUE = new GsNoticeQueue();
    static final int CARD_W = 168, LINE_H = 10, GAP = 3;
    private static float lastOfferAt = -1000f;
    private static float lastShownAt = -1000f;

    /** Mod bus + game bus hooks. Called once from the client section of the mod constructor (through UiThemeClient). */
    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterGuiLayersEvent e) ->
                e.registerAboveAll(ResourceLocation.fromNamespaceAndPath("nusmp", "gs_notices"), GsNotices::render));
        NeoForge.EVENT_BUS.addListener(GsNotices::onChat);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> { if (QUEUE.empty()) return; QUEUE.update(UiTheme.realNow()); });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> clear());
        GsCrestScreen.register();
    }

    static void clear() {
        QUEUE.clear();
        lastOfferAt = -1000f;
        lastShownAt = -1000f;
    }

    /** True when the crest and notices are on at all (theme great_spirit and ui.greatSpirit.notices). */
    static boolean on() { return UiTheme.greatSpirit() && UiThemeOptions.notices(); }

    /** The same answer for other client code (the companion screen offers its events only when the crest is on). */
    public static boolean active() { return on(); }

    /** The chat mirror: a line whose translation key is on the allow-list becomes a notice. The chat line itself is never touched. */
    private static void onChat(ClientChatReceivedEvent e) {
        if (!on()) return;
        Component m = e.getMessage();
        if (m == null || !(m.getContents() instanceof TranslatableContents tc) || !GsNoticeAllow.allowed(tc.getKey())) return;
        offer(tc.getKey(), m.getString(), GsNoticeAllow.kind(tc.getKey()));
    }

    /** A grimoire page opened through mastery (called by the page-unlock tracker, client side). */
    public static void pageOpened(String id, String name) {
        if (!on() || name == null || name.isEmpty()) return;
        offer("nusmp.ui.notice.page:" + id, Component.translatableWithFallback("nusmp.ui.notice.page_open", "A grimoire page opened: %s", name).getString(), GsNoticeQueue.Kind.OK);
    }

    /** Offers a notice to the bounded queue; false when refused. */
    public static boolean offer(String key, String text, GsNoticeQueue.Kind kind) {
        float now = UiTheme.realNow();
        boolean ok = QUEUE.offer(key, text, kind, now);
        if (ok) lastOfferAt = now;
        return ok;
    }

    // ---------------------------------------------------------------- drawing
    static void render(GuiGraphics g, DeltaTracker delta) {
        if (!on()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || mc.screen instanceof GsCrestScreen) return;
        float now = UiTheme.realNow();
        QUEUE.update(now);
        boolean active = !QUEUE.empty();
        if (active) lastShownAt = now;
        float fade = active ? 1f : 1f - UiMath.clamp01((now - lastShownAt) / 30f);
        if (fade <= 0.01f) return;
        draw(g, mc.font, g.guiWidth(), g.guiHeight(), UiThemeOptions.crestPlace(), now, fade, false);
    }

    /** Draws the crest at its place and the visible notices beside it. 'demo' is the move screen (no queue state is needed there). */
    static void draw(GuiGraphics g, Font font, int sw, int sh, GsCrestPos.Place place, float now, float fade, boolean demo) {
        GsSkin skin = GsUser.skin();
        int[] xy = GsCrestPos.resolve(place, sw, sh, GsDraw.CREST, GsDraw.CREST);
        int cx = xy[0], cy = xy[1];
        float ping = UiMath.clamp01(1f - (now - lastOfferAt) / 24f);
        GsDraw.crest(g, cx, cy, skin, fade, now, ping);
        int queued = QUEUE.queuedCount();
        if (queued > 0 && !demo) {                                                       // the stack counter on the crest
            String n = "+" + queued;
            int nw = font.width(n);
            g.fill(cx + GsDraw.CREST - nw - 3, cy + GsDraw.CREST - 10, cx + GsDraw.CREST, cy + GsDraw.CREST, UiMath.withAlpha(0x000000, 0.7f * fade));
            g.drawString(font, n, cx + GsDraw.CREST - nw - 1, cy + GsDraw.CREST - 9, UiMath.scaleAlpha(0xFF000000 | GsDraw.NEON2[skin.ordinal()], fade), false);
        }
        if (demo) return;
        // the cards: below the crest in the top half of the screen, above it in the bottom half; right-aligned to the crest on the right side
        boolean below = !place.corner().bottom();
        boolean rightSide = place.corner().right();
        int y = below ? cy + GsDraw.CREST + GAP : cy - GAP;
        int x = rightSide ? Math.max(0, Math.min(sw - CARD_W, cx + GsDraw.CREST - CARD_W)) : Math.max(0, Math.min(sw - CARD_W, cx));
        for (int i = 0; i < GsNoticeQueue.VISIBLE; i++) {
            GsNoticeQueue.Notice n = QUEUE.slot(i);
            if (n == null) continue;
            y = card(g, font, n, x, y, below, rightSide, now, skin);
        }
    }

    /** One card; returns the y where the next one goes. */
    private static int card(GuiGraphics g, Font font, GsNoticeQueue.Notice n, int x, int y, boolean down, boolean rightSide, float now, GsSkin skin) {
        float age = now - n.shownAt;
        float a = GsNoticeQueue.alpha(age);
        if (a <= 0.01f) return y;
        if (n.lines == null) {
            n.lines = new String[2];
            var split = font.getSplitter().splitLines(n.text, CARD_W - 16, net.minecraft.network.chat.Style.EMPTY);
            n.lines[0] = split.isEmpty() ? "" : split.get(0).getString();
            n.lines[1] = split.size() > 1 ? split.get(1).getString() : "";
            n.head = Component.translatableWithFallback("nusmp.ui.notice.head", "[Notice... Great Sage has recorded]").getString();
        }
        int lines = n.lines[1].isEmpty() ? 1 : 2;
        int h = 6 + LINE_H + lines * LINE_H;
        float slide = GsNoticeQueue.slide(age);
        int dx = Math.round((1f - slide) * 18f) * (rightSide ? 1 : -1);
        int top = down ? y : y - h;
        int s = skin.ordinal();
        int kind = switch (n.kind) { case OK -> GsDraw.OK[s]; case WARN -> GsDraw.WARN[s]; case BAD -> GsDraw.BAD[s]; default -> GsDraw.NEON[s]; };
        int x0 = x + dx;
        g.fillGradient(x0, top, x0 + CARD_W, top + h, UiMath.withAlpha(GsDraw.PANEL_TOP[s], 0.93f * a), UiMath.withAlpha(GsDraw.PANEL_BOTTOM[s], 0.95f * a));
        g.renderOutline(x0, top, CARD_W, h, UiMath.withAlpha(GsDraw.NEON[s], 0.85f * a));
        g.fill(x0, top + 1, x0 + 2, top + h - 1, UiMath.withAlpha(kind, a));
        int tx = x0 + 7;
        int headColor = UiMath.scaleAlpha(0xFF000000 | GsDraw.NEON2[s], a);
        if ((headColor >>> 24) >= 8) g.drawString(font, n.head, tx, top + 3, headColor, false);        // alpha under 4 would draw opaque
        int typed = UiThemeOptions.typing() && UiTheme.motion() ? UiMath.typed(age - 4f, 2.2f, n.lines[0].length() + n.lines[1].length()) : Integer.MAX_VALUE;
        int c0 = UiMath.scaleAlpha(0xFF000000 | GsDraw.TEXT[s], a);
        if ((c0 >>> 24) >= 8) {
            String l0 = n.lines[0], l1 = n.lines[1];
            g.drawString(font, typed >= l0.length() ? l0 : l0.substring(0, typed), tx, top + 3 + LINE_H, c0, false);
            if (lines > 1 && typed > l0.length()) g.drawString(font, typed >= l0.length() + l1.length() ? l1 : l1.substring(0, typed - l0.length()), tx, top + 3 + 2 * LINE_H, c0, false);
        }
        return down ? top + h + GAP : top - GAP;
    }
}
