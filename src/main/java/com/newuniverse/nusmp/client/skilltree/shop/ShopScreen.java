package com.newuniverse.nusmp.client.skilltree.shop;

import com.mojang.blaze3d.systems.RenderSystem;
import com.newuniverse.nusmp.client.skilltree.shop.ShopLayout.Rect;
import com.newuniverse.nusmp.client.ui.theme.MagicCircle;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.shop.BuyPayload;
import com.newuniverse.nusmp.skilltree.shop.GoldFormat;
import com.newuniverse.nusmp.skilltree.shop.SellAllPayload;
import com.newuniverse.nusmp.skilltree.shop.SellPayload;
import com.newuniverse.nusmp.skilltree.shop.ShopRules;
import com.newuniverse.nusmp.skilltree.shop.ShopSnapshot;
import com.newuniverse.nusmp.skilltree.shop.ShopSyncPayload;
import com.newuniverse.nusmp.skilltree.shop.StockEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Black Market must feel like a hidden back room (dim plum glass, amber lamplight, cobwebs, dust, the old witch's portrait and her voice), and it must be impossible for
 * the screen to disagree with the server. So this class DECIDES NOTHING: every number it draws (gold, prices, stock, what the player carries and what the witch pays) comes from the
 * one picture ({@link ShopSnapshot}) the server last sent, a click only sends a request ({@link BuyPayload}, {@link SellPayload}, {@link SellAllPayload}) and the next picture
 * shows what really happened, and the Buy button is greyed from the same numbers purely as a courtesy (the server refuses a poor player anyway). It is a plain {@link Screen} drawn
 * by hand rather than a tree of vanilla widgets because both lists scroll and every row is a custom row (an item, a name, a price, a button), which widgets would only make
 * harder to keep in step with the layout; all rectangles and hit tests come from the pure, tested {@link ShopLayout}, and all the look from {@link ShopPainter}, so the preview tool
 * can mirror them. It uses the mod's own panel kit and magic circle so it reads as part of the same UI. It does not pause the game (the server is the authority and other players
 * go on), and closes itself when the witch is out of reach.
 */
public final class ShopScreen extends Screen {
    /** How long (ticks) the witch's answer to the last deal stays on her lips before she goes back to commenting on what the mouse touches. */
    private static final float REPLY_TICKS = 100f;
    private static final int SPEECH = 0xFFEFE2FA;

    private ShopSnapshot snap;
    private ShopLayout lay;
    private int shelfScroll, goodsScroll, lost;
    /**
     * True once the player has been seen NOT holding shift since the screen opened. The shop opens on a sneaking right click, so shift is still down when it appears, and a click on
     * Sell would otherwise sell the whole stack by accident; shift only counts as "sell the stack" after it has been let go once.
     */
    private boolean shiftSeenUp;
    private String voice;
    private float voiceAt;
    private String replyLine = "";
    private float replyAt = -1000f;
    /** The bulk-sale buttons for the current picture and layout (see {@link #planBulk}): label and rectangle, index for index. */
    private final List<String> bulkLabels = new ArrayList<>();
    private List<Rect> bulkRects = List.of();

    public ShopScreen(ShopSnapshot snapshot) {
        super(Component.translatable("gui.nusmp.shop.title"));
        this.snap = snapshot;
        this.voice = voiceOf(snapshot);
        this.voiceAt = UiTheme.realNow();
    }

    // ================================================================ data
    /** A new picture from the server (and, after a deal, its verdict). */
    public void update(ShopSnapshot s, ShopSyncPayload payload) {
        this.snap = s;
        if (payload == null) {
            voice = voiceOf(s);
            voiceAt = UiTheme.realNow();
        } else {
            answer(payload.verdict(), payload.variant());
        }
        clampScrolls();
        planBulk();
    }

    /** The witch's reply to a deal, in one of her two voices for it. */
    private void answer(ShopRules.Reply r, int variant) {
        if (r == ShopRules.Reply.NONE) return;
        String key = r.key() + "." + (variant % 2 + 1);
        replyLine = I18n.exists(key) ? I18n.get(key) : "";
        replyAt = UiTheme.realNow();
    }

    private static String voiceOf(ShopSnapshot s) {
        String t = s.voice() == null ? "" : s.voice().getString();
        return t.isBlank() ? Component.translatable("gui.nusmp.shop.welcome").getString() : t;
    }

    private void clampScrolls() {
        if (lay == null) return;
        shelfScroll = ShopLayout.clampScroll(shelfScroll, snap.offers().size(), lay.shelfVisible);
        goodsScroll = ShopLayout.clampScroll(goodsScroll, snap.sells().size(), lay.goodsVisible);
    }

    // ================================================================ lifecycle
    @Override
    protected void init() {
        lay = ShopLayout.of(width, height);
        clampScrolls();
        planBulk();
    }

    @Override
    public void tick() {
        if (!hasShiftDown()) shiftSeenUp = true;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || snap.witch() < 0) {
            onClose();
            return;
        }
        Entity witch = mc.level.getEntity(snap.witch());
        if (witch == null || witch.distanceTo(mc.player) > ShopRules.REACH + 2.0f) {
            if (++lost > 10) onClose();
        } else {
            lost = 0;
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }

    // ================================================================ drawing
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderTransparentBackground(g);
        if (UiTheme.anime()) {
            UiPanel.scrim(g, width, height, 0.85f);
            MagicCircle.draw(g, width / 2f, height / 2f, Math.min(width, height) * 0.62f, UiTheme.now() + partial, ShopPainter.PLUM, 0.16f, MagicCircle.CALM, 1f);
        } else {
            g.fillGradient(0, 0, width, height, 0xC0100A16, 0xE0060309);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        if (lay == null) lay = ShopLayout.of(width, height);
        float now = UiTheme.now() + partial;
        Rect p = lay.panel;
        // the room: the theme's panel with an amber edge, tinted towards plum, then the webs and the dust over it
        UiPanel.panel(g, p.x(), p.y(), p.w(), p.h(), ShopPainter.AMBER, 0.55f, 1f);
        g.fillGradient(p.x() + 1, p.y() + 1, p.right() - 1, p.bottom() - 1, UiMath.withAlpha(0x2A1030, 0.30f), UiMath.withAlpha(0x0A0408, 0.40f));
        ShopPainter.cobwebs(g, p);
        ShopPainter.dust(g, p, now, 16);

        header(g);
        int hoverOffer = shelf(g, mouseX, mouseY, now);
        int hoverGood = goods(g, mouseX, mouseY);
        footer(g, mouseX, mouseY, hoverOffer, hoverGood);
        tooltips(g, mouseX, mouseY, hoverOffer, hoverGood);
    }

    private void header(GuiGraphics g) {
        Rect pr = lay.portrait;
        g.fill(pr.x() - 1, pr.y() - 1, pr.right() + 1, pr.bottom() + 1, UiMath.withAlpha(ShopPainter.AMBER, 0.55f));
        g.fill(pr.x(), pr.y(), pr.right(), pr.bottom(), 0xFF0E0814);
        RenderSystem.enableBlend();
        g.blit(ShopPainter.PORTRAIT, pr.x(), pr.y(), ShopPainter.PORTRAIT_W, ShopPainter.PORTRAIT_H, 0, 0, ShopPainter.PORTRAIT_W, ShopPainter.PORTRAIT_H, ShopPainter.PORTRAIT_W, ShopPainter.PORTRAIT_H);
        int tx = pr.right() + 8;
        g.drawString(font, Component.translatable("gui.nusmp.shop.title").withStyle(s -> s.withBold(true)), tx, lay.panel.y() + 8, ShopPainter.AMBER, true);
        Rect pu = lay.purse;
        String gold = GoldFormat.full(snap.gold());
        int gw = font.width(gold);
        String clock = Component.translatable("gui.nusmp.shop.restock", GoldFormat.clock(snap.ticksToRestock())).getString();
        String lim = snap.dailyLeft() >= 0 ? Component.translatable("gui.nusmp.shop.limit", snap.dailyLeft()).getString() : "";
        // the sub-title only appears when the whole of it fits beside the purse (a cut-off line of flavour text looks worse than none)
        int purseW = Math.max(gw + 11, Math.max(font.width(clock), font.width(lim)));
        String sub = Component.translatable("gui.nusmp.shop.sub").getString();
        if (font.width(sub) <= pu.right() - purseW - 8 - tx) g.drawString(font, sub, tx, lay.panel.y() + 21, ShopPainter.DIM, false);
        ShopPainter.coin(g, pu.right() - gw - 11, pu.y() + 1, true);
        g.drawString(font, gold, pu.right() - gw, pu.y() + 1, ShopPainter.AMBER, true);
        g.drawString(font, clock, pu.right() - font.width(clock), pu.y() + 13, ShopPainter.DIM, false);
        if (!lim.isEmpty()) g.drawString(font, lim, pu.right() - font.width(lim), pu.y() + 23, snap.dailyLeft() > 0 ? ShopPainter.DIM : ShopPainter.BAD, false);
        g.fill(lay.panel.x() + ShopLayout.PAD, lay.panel.y() + ShopLayout.HEAD_H - 1, lay.panel.right() - ShopLayout.PAD, lay.panel.y() + ShopLayout.HEAD_H, UiMath.withAlpha(ShopPainter.AMBER, 0.25f));
    }

    /** Draws the shelf; returns the index of the offer under the mouse, or -1. */
    private int shelf(GuiGraphics g, int mx, int my, float now) {
        Rect box = lay.shelfBox;
        UiPanel.well(g, box.x(), box.y(), box.w(), box.h(), ShopPainter.AMBER, 0.9f);
        UiPanel.heading(g, font, Component.translatable("gui.nusmp.shop.shelves").getString(), box.x() + 4, box.y() + 2, box.w() - 8, ShopPainter.AMBER);
        List<ShopSnapshot.Offer> offers = snap.offers();
        if (offers.isEmpty()) {
            int y = box.y() + ShopLayout.HEADING_H + 6;
            for (FormattedCharSequence line : font.split(Component.translatable("gui.nusmp.shop.empty_shelf"), box.w() - 12)) {
                g.drawString(font, line, box.x() + 6, y, ShopPainter.DIM, false);
                y += 10;
            }
            return -1;
        }
        int hover = ShopLayout.rowAt(lay.shelfList, ShopLayout.SHELF_ROW, shelfScroll, offers.size(), mx, my);
        Rect list = lay.shelfList;
        g.enableScissor(list.x(), list.y(), list.right(), list.bottom());
        for (int i = shelfScroll; i < Math.min(offers.size(), shelfScroll + lay.shelfVisible + 1); i++) {
            ShopSnapshot.Offer o = offers.get(i);
            Rect row = lay.shelfRow(i, shelfScroll);
            boolean over = i == hover;
            boolean soldOut = o.left() <= 0;
            boolean affordable = !soldOut && snap.gold() >= o.price();
            ShopPainter.row(g, row, over, o.flagship(), now);
            Rect ic = ShopLayout.icon(row);
            if (!o.stack().isEmpty()) g.renderItem(o.stack(), ic.x(), ic.y());
            Rect buy = ShopLayout.buyButton(row);
            int textX = ic.right() + 5;
            g.drawString(font, fit(nameOf(o), buy.x() - textX - 4), textX, row.y() + 4, nameColor(o), false);
            int ly = row.y() + 13;
            ShopPainter.coin(g, textX, ly, affordable);
            String price = GoldFormat.full(o.price());
            g.drawString(font, price, textX + 10, ly, soldOut ? ShopPainter.FAINT : affordable ? ShopPainter.GOOD : ShopPainter.BAD, false);
            int sx = textX + 10 + font.width(price) + 6;
            String stock = soldOut ? Component.translatable("gui.nusmp.shop.sold_out").getString() : Component.translatable("gui.nusmp.shop.left", o.left()).getString();
            if (sx + font.width(stock) <= buy.x() - 2) g.drawString(font, stock, sx, ly, soldOut ? ShopPainter.BAD : ShopPainter.DIM, false);
            ShopPainter.button(g, font, buy, Component.translatable("gui.nusmp.shop.buy").getString(), affordable, buy.contains(mx, my), ShopPainter.AMBER);
        }
        g.disableScissor();
        ShopPainter.scrollbar(g, lay.shelfBar, ShopLayout.thumb(offers.size(), lay.shelfVisible, shelfScroll, lay.shelfBar.h()));
        return hover;
    }

    /** Draws the player's goods; returns the index of the item under the mouse, or -1. */
    private int goods(GuiGraphics g, int mx, int my) {
        Rect box = lay.goodsBox;
        UiPanel.well(g, box.x(), box.y(), box.w(), box.h(), ShopPainter.PLUM, 0.9f);
        UiPanel.heading(g, font, Component.translatable("gui.nusmp.shop.goods").getString(), box.x() + 4, box.y() + 2, box.w() - 8, ShopPainter.PLUM);
        for (int i = 0; i < bulkRects.size(); i++) {
            ShopPainter.button(g, font, bulkRects.get(i), bulkLabels.get(i), true, bulkRects.get(i).contains(mx, my), ShopPainter.PLUM);
        }
        List<ShopSnapshot.Sell> sells = snap.sells();
        Rect list = lay.goodsList;
        if (sells.isEmpty()) {
            int y = list.y() + 2;
            for (FormattedCharSequence line : font.split(Component.translatable("gui.nusmp.shop.empty_goods"), list.w() - 4)) {
                g.drawString(font, line, list.x() + 3, y, ShopPainter.DIM, false);
                y += 10;
            }
            return -1;
        }
        int hover = ShopLayout.rowAt(list, ShopLayout.GOODS_ROW, goodsScroll, sells.size(), mx, my);
        g.enableScissor(list.x(), list.y(), list.right(), list.bottom());
        for (int i = goodsScroll; i < Math.min(sells.size(), goodsScroll + lay.goodsVisible + 1); i++) {
            ShopSnapshot.Sell s = sells.get(i);
            Rect row = lay.goodsRow(i, goodsScroll);
            ShopPainter.row(g, row, i == hover, false, 0f);
            Rect ic = ShopLayout.icon(row);
            g.renderItem(s.stack(), ic.x(), ic.y());                                         // no count badge on the icon: the line under the name already says "x13"
            Rect sell = ShopLayout.sellButton(row);
            int textX = ic.right() + 4;
            g.drawString(font, fit(s.stack().getHoverName().getString(), sell.x() - textX - 3), textX, row.y() + 3, ShopPainter.PAPER, false);
            g.drawString(font, fit("x" + s.stack().getCount() + "  " + GoldFormat.compact(s.unit()) + " ea", sell.x() - textX - 3), textX, row.y() + 12, ShopPainter.AMBER, false);
            ShopPainter.button(g, font, sell, Component.translatable("gui.nusmp.shop.sell").getString(), true, sell.contains(mx, my), ShopPainter.PLUM);
        }
        g.disableScissor();
        ShopPainter.scrollbar(g, lay.goodsBar, ShopLayout.thumb(sells.size(), lay.goodsVisible, goodsScroll, lay.goodsBar.h()));
        return hover;
    }

    private void footer(GuiGraphics g, int mx, int my, int hoverOffer, int hoverGood) {
        Rect sp = lay.speech;
        ShopPainter.bubble(g, sp);
        float now = UiTheme.realNow();
        String text;
        float typedFor;
        if (now - replyAt < REPLY_TICKS && !replyLine.isEmpty()) {
            text = replyLine;
            typedFor = now - replyAt;
        } else if (hoverOffer >= 0) {
            text = blurb(snap.offers().get(hoverOffer));
            typedFor = 1000f;                                                                  // a comment on what the mouse touches appears at once; only her own lines are typed
        } else if (hoverGood >= 0) {
            boolean overSell = ShopLayout.sellButton(lay.goodsRow(hoverGood, goodsScroll)).contains(mx, my);       // over the button she says how to sell a whole stack
            text = Component.translatable(overSell ? "gui.nusmp.shop.blurb.sell" : "gui.nusmp.shop.blurb.goods", GoldFormat.full(snap.sells().get(hoverGood).unit())).getString();
            typedFor = 1000f;
        } else {
            text = voice;
            typedFor = now - voiceAt;
        }
        List<String> lines = wrapSpeech(text, sp.w() - 10, ShopLayout.speechLines(sp));
        int chars = 0;
        for (String line : lines) chars += line.length();
        int left = UiMath.typed(typedFor, 2.4f, chars);
        int y = sp.y() + Math.max(2, (sp.h() - 9 * lines.size()) / 2);                            // one line sits in the middle of the bubble, two fill it
        for (String line : lines) {
            if (left <= 0) break;
            g.drawString(font, left >= line.length() ? line : line.substring(0, left), sp.x() + 5, y, SPEECH, false);
            left -= line.length();
            y += 9;
        }
        ShopPainter.button(g, font, lay.closeButton, Component.translatable("gui.nusmp.shop.close").getString(), true, lay.closeButton.contains(mx, my), ShopPainter.AMBER);
    }

    private void tooltips(GuiGraphics g, int mx, int my, int hoverOffer, int hoverGood) {
        if (hoverOffer >= 0) {
            ShopSnapshot.Offer o = snap.offers().get(hoverOffer);
            Rect row = lay.shelfRow(hoverOffer, shelfScroll);
            if (!o.stack().isEmpty() && mx < ShopLayout.buyButton(row).x()) g.renderTooltip(font, o.stack(), mx, my);
        } else if (hoverGood >= 0) {
            Rect row = lay.goodsRow(hoverGood, goodsScroll);
            if (mx < ShopLayout.sellButton(row).x()) g.renderTooltip(font, snap.sells().get(hoverGood).stack(), mx, my);
        }
    }

    // ================================================================ bulk buttons
    /**
     * Works out the labels and rectangles of the bulk-sale buttons for this picture and this layout, once per change instead of once per frame and per click, so drawing and
     * clicking use the very same list. The roomy form ("All stones", loose padding) when it fits the strip above the goods, then the bare category name, then the bare name with
     * tight padding, and in the last form whatever still does not fit is left out (a button hanging out of its box would be worse than a missing one at a window size nobody uses).
     */
    private void planBulk() {
        bulkLabels.clear();
        bulkRects = List.of();
        if (lay == null || snap.bulk().isEmpty()) return;
        boolean[] longForm = {true, false, false};
        for (int attempt = 0; attempt < 3; attempt++) {
            List<String> labels = new ArrayList<>();
            int[] widths = new int[snap.bulk().size()];
            for (int i = 0; i < widths.length; i++) {
                labels.add(bulkLabel(snap.bulk().get(i), longForm[attempt]));
                widths[i] = font.width(labels.get(i));
            }
            boolean tight = attempt == 2;
            List<Rect> rects = ShopLayout.bulkRects(lay.bulkRow, widths, tight ? ShopLayout.BULK_PAD_TIGHT : ShopLayout.BULK_PAD, tight ? ShopLayout.BULK_GAP_TIGHT : ShopLayout.BULK_GAP);
            if (rects.size() == widths.length || tight) {
                bulkLabels.addAll(labels.subList(0, rects.size()));
                bulkRects = rects;
                return;
            }
        }
    }

    private String bulkLabel(String category, boolean withAll) {
        String key = "gui.nusmp.shop.category." + category;
        String name = I18n.exists(key) ? I18n.get(key) : category;
        if (withAll) return Component.translatable("gui.nusmp.shop.sell_all", name).getString();
        return name.isEmpty() ? name : Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    // ================================================================ text
    private String nameOf(ShopSnapshot.Offer o) {
        return o.stack().isEmpty() ? o.ref() : o.stack().getHoverName().getString();
    }

    private int nameColor(ShopSnapshot.Offer o) {
        if (o.kind() == StockEntry.Kind.ROUTE_BOOK.ordinal()) {
            SkillRoute r = RouteRegistry.byId(o.ref());
            if (r != null) return 0xFF000000 | ShopColors.lift(r.color());                    // the route's own colour, lifted so it reads on the dark glass
        }
        return o.flagship() ? ShopPainter.AMBER : ShopPainter.PAPER;
    }

    /** What the witch murmurs about the offer the mouse is over. */
    private String blurb(ShopSnapshot.Offer o) {
        String key;
        if (o.kind() == StockEntry.Kind.ROUTE_BOOK.ordinal()) key = o.flagship() ? "gui.nusmp.shop.blurb.flagship" : "gui.nusmp.shop.blurb.route";
        else key = I18n.exists("gui.nusmp.shop.blurb.sp." + o.ref()) ? "gui.nusmp.shop.blurb.sp." + o.ref() : "gui.nusmp.shop.blurb.sp";
        return I18n.get(key);
    }

    /**
     * The speech wrapped ONCE, as a whole, to the bubble's width and to at most {@code maxLines} lines (the last one ends in "..." when the text is longer): a line that is typed out
     * letter by letter therefore never re-wraps while it grows, and a long sentence can never run out of the bubble. The wrapping itself is {@link ShopLayout#wrap}, which is pure and
     * tested; only the pixel width of a string comes from the font.
     */
    private List<String> wrapSpeech(String text, int maxW, int maxLines) {
        return ShopLayout.wrap(text, maxW, maxLines, s -> font.width(s));
    }

    /** The text cut to a width in pixels, with an ellipsis. */
    private String fit(String s, int maxW) {
        if (maxW <= 0) return "";
        if (font.width(s) <= maxW) return s;
        String dots = "...";
        return font.plainSubstrByWidth(s, Math.max(0, maxW - font.width(dots))) + dots;
    }

    // ================================================================ input
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (lay == null || (button != 0 && button != 1)) return super.mouseClicked(mx, my, button);
        if (lay.closeButton.contains(mx, my)) {
            click();
            onClose();
            return true;
        }
        int offer = ShopLayout.rowAt(lay.shelfList, ShopLayout.SHELF_ROW, shelfScroll, snap.offers().size(), mx, my);
        if (offer >= 0 && ShopLayout.buyButton(lay.shelfRow(offer, shelfScroll)).contains(mx, my)) {
            buy(snap.offers().get(offer));
            return true;
        }
        int good = ShopLayout.rowAt(lay.goodsList, ShopLayout.GOODS_ROW, goodsScroll, snap.sells().size(), mx, my);
        if (good >= 0 && ShopLayout.sellButton(lay.goodsRow(good, goodsScroll)).contains(mx, my)) {
            ShopSnapshot.Sell s = snap.sells().get(good);
            boolean stack = button == 1 || (shiftSeenUp && hasShiftDown());
            click();
            PacketDistributor.sendToServer(new SellPayload(s.slot(), BuiltInRegistries.ITEM.getKey(s.stack().getItem()).toString(), stack ? s.stack().getCount() : 1));
            return true;
        }
        for (int i = 0; i < bulkRects.size(); i++) {
            if (bulkRects.get(i).contains(mx, my)) {
                click();
                PacketDistributor.sendToServer(new SellAllPayload(snap.bulk().get(i)));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private void buy(ShopSnapshot.Offer o) {
        if (o.left() <= 0 || snap.gold() < o.price()) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1.0f));
            answer(o.left() <= 0 ? ShopRules.Reply.SOLD_OUT : ShopRules.Reply.POOR, (int) UiTheme.realNow() & 0xFF);
            return;
        }
        click();
        PacketDistributor.sendToServer(new BuyPayload(o.id(), o.price()));
    }

    private static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (lay == null || dy == 0) return false;
        int step = dy > 0 ? -1 : 1;
        if (lay.shelfBox.contains(mx, my)) shelfScroll = ShopLayout.clampScroll(shelfScroll + step, snap.offers().size(), lay.shelfVisible);
        else if (lay.goodsBox.contains(mx, my)) goodsScroll = ShopLayout.clampScroll(goodsScroll + step, snap.sells().size(), lay.goodsVisible);
        return true;
    }
}
