package com.newuniverse.nusmp.client.weaponpage;

import com.newuniverse.nusmp.client.multiverse.MultiverseClientConfig;
import com.newuniverse.nusmp.client.ui.CodexContext;
import com.newuniverse.nusmp.client.ui.CodexPage;
import com.newuniverse.nusmp.client.ui.UiDraw;
import com.newuniverse.nusmp.client.ui.theme.UiBar;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.weaponpage.WeaponPageRules;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Why: the Weapon page of the Codex (docs/ui_weapon_page.md), appended last of the stock pages. It shows the hero / vassal, Lord or Black Sword weapon the player holds (or the one picked from the chips): name,
 * grade, level, form, the upgrade options the weapons already have (change form, learn a form, feed the gem, Half-Starvation), and the mastery bars: the weapon's own, then the other tracks the mod already has
 * (the Grimoire's mastery, each Beast Will's). A click on a ready option sends the one validated packet; the server answers with a fresh page. Everything it draws was cooked once when the data arrived
 * ({@link WeaponPageClient}): a frame draws strings and fills from arrays, builds no strings, no lists and no resource locations; the layout record is rebuilt only when the area moves. It can be hidden with the
 * client setting {@code weaponpage.show} (looks only; the weapons and their other screens do not change).
 */
public final class WeaponCodexPage implements CodexPage {
    private WeaponPageLayout layout;
    private int lastScroll;
    /** Texts translated when the Codex opens (a frame must not build strings), and the one message the page may show, split to its width once. */
    private String headMastery = "", headOthers = "", msgEmpty = "", msgLoading = "";
    private String msgText = "";
    private int msgWidth = -1;
    private List<FormattedCharSequence> msgLines = List.of();

    private String titleText = "";

    @Override
    public String title() {
        if (titleText.isEmpty()) titleText = Component.translatable("anime_multiverse.weaponpage.title").getString();
        return titleText;
    }

    @Override public String glyph() { return "Wp"; }

    @Override public boolean hidden() { return !MultiverseClientConfig.get(MultiverseClientConfig.WEAPON_PAGE); }

    @Override
    public void opened() {
        titleText = Component.translatable("anime_multiverse.weaponpage.title").getString();
        headMastery = Component.translatable("anime_multiverse.weaponpage.heading.mastery").getString();
        headOthers = Component.translatable("anime_multiverse.weaponpage.heading.others").getString();
        msgEmpty = Component.translatable("anime_multiverse.weaponpage.empty").getString();
        msgLoading = Component.translatable("anime_multiverse.weaponpage.loading").getString();
        msgWidth = -1;
        WeaponPageClient.opened();
    }

    private WeaponPageLayout layoutAt(int x, int y, int w) {
        WeaponPageLayout l = layout;
        if (l == null || l.x() != x || l.y() != y || l.w() != Math.max(88, w)) layout = l = WeaponPageLayout.of(x, y, w);
        return l;
    }

    private static WeaponPageClient.Page current() {
        int i = WeaponPageClient.selected();
        return i < 0 ? null : WeaponPageClient.pages()[i];
    }

    private static int chips() {
        int n = WeaponPageClient.pages().length;
        return n > 1 ? n : 0;
    }

    @Override
    public int contentHeight(CodexContext ctx, int width) {
        WeaponPageClient.Page p = current();
        WeaponPageLayout l = WeaponPageLayout.of(0, 0, width);
        return l.height(chips(), p == null ? 0 : p.opts.length, p == null ? 0 : p.barLabel.length, WeaponPageClient.extraLabels().length) + 6;
    }

    @Override
    public void render(CodexContext ctx, GuiGraphics g, int x, int y, int w, int h, int scroll, int mx, int my, float time) {
        if (mx > -1000) {                                                      // the live page, not a leaf of the page turn
            WeaponPageClient.ensureAsked();
            lastScroll = scroll;
        }
        Font font = ctx.font();
        WeaponPageLayout l = layoutAt(x, y - scroll, w);
        WeaponPageClient.Page[] pages = WeaponPageClient.pages();
        WeaponPageClient.Page p = current();
        int chips = chips(), top = y - 14, bottom = y + h;
        if (!WeaponPageClient.received() || p == null) {
            String msg = WeaponPageClient.received() ? msgEmpty : msgLoading;
            if (msgWidth != l.leftW() || !msg.equals(msgText)) {                 // split again only when the text or the width changed
                msgText = msg;
                msgWidth = l.leftW();
                msgLines = font.split(Component.literal(msg), Math.max(20, l.leftW() - 4));
            }
            int ty = l.y();
            for (FormattedCharSequence line : msgLines) {
                g.drawString(font, line, l.x(), ty, UiDraw.inkDim(), false);
                ty += font.lineHeight + 1;
            }
        } else {
            int sel = WeaponPageClient.selected();
            int cw = l.chipW(chips);
            for (int i = 0; i < chips; i++) {
                int cx = l.chipX(i, chips);
                button(ctx, g, font, cx, l.y(), cw, WeaponPageLayout.CHIP_H, pages[i].chip, i == sel, inside(mx, my, cx, l.y(), cw, WeaponPageLayout.CHIP_H), true, pages[i].rgb);
            }
            int hx = l.x(), hy = l.bodyY(chips);
            ctx.scissor(g, hx, hy, hx + l.leftW(), hy + WeaponPageLayout.HEAD_H);                // Hub v2: the page may be drawn inside the hub's scaled window
            g.drawString(font, p.title, hx, hy, UiDraw.goldInk(), false);
            g.drawString(font, p.grade, hx, hy + 10, 0xFF000000 | p.rgb, false);
            g.drawString(font, p.level, hx, hy + 20, UiDraw.ink(), false);
            g.drawString(font, p.form, hx, hy + 30, UiDraw.inkDim(), false);
            g.disableScissor();
            int rh = WeaponPageLayout.ROW_H - 1;
            for (int i = 0; i < p.opts.length; i++) {
                int ry = l.optionY(i, chips);
                if (ry + rh < top || ry > bottom) continue;
                WeaponPageClient.Row row = p.opts[i];
                button(ctx, g, font, l.x(), ry, l.leftW(), rh, row.text, row.state == WeaponPageRules.ST_CURRENT, inside(mx, my, l.x(), ry, l.leftW(), rh) && row.clickable(),
                        row.clickable() || row.state == WeaponPageRules.ST_CURRENT, p.rgb);
            }
            UiDraw.header(g, font, headMastery, l.rightX(), l.barsHeadingY(chips), l.rightW());
            for (int i = 0; i < p.barLabel.length; i++) {
                int by = l.barY(i, chips);
                if (by + WeaponPageLayout.BAR_H >= top && by <= bottom) bar(ctx, g, font, l.rightX(), by, l.rightW(), p.barLabel[i], p.barValue[i], p.barFrac[i], p.rgb);
            }
        }
        String[] el = WeaponPageClient.extraLabels();
        if (el.length > 0 && WeaponPageClient.received()) {
            int bars = p == null ? 0 : p.barLabel.length;
            UiDraw.header(g, font, headOthers, l.rightX(), l.extraHeadingY(bars, chips), l.rightW());
            String[] ev = WeaponPageClient.extraValues();
            float[] ef = WeaponPageClient.extraFracs();
            for (int i = 0; i < el.length; i++) {
                int by = l.extraY(i, bars, chips);
                if (by + WeaponPageLayout.BAR_H >= top && by <= bottom) bar(ctx, g, font, l.rightX(), by, l.rightW(), el[i], ev[i], ef[i], 0xB39CF0);
            }
        }
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) { return mx >= x && my >= y && mx < x + w && my < y + h; }

    /** A label line with the value at the right and the bar under it. */
    private static void bar(CodexContext ctx, GuiGraphics g, Font font, int x, int y, int w, String label, String value, float frac, int rgb) {
        ctx.scissor(g, x, y, x + w, y + 10);
        g.drawString(font, label, x, y, UiDraw.ink(), false);
        g.disableScissor();
        g.drawString(font, value, x + w - font.width(value), y, UiDraw.inkDim(), false);
        int by = y + 10;
        if (UiTheme.anime()) UiBar.draw(g, x, by, w, 4, frac, 0xFF000000 | rgb, UiTheme.now(), 1f);
        else {
            g.fill(x, by, x + w, by + 4, UiDraw.withAlpha(UiDraw.INK, 90));
            g.fill(x, by, x + Math.round(w * frac), by + 4, 0xFF000000 | rgb);
        }
    }

    /** A chip or an option row in the theme's look; the text is clipped to the row, never wrapped. */
    private static void button(CodexContext ctx, GuiGraphics g, Font font, int x, int y, int w, int h, String text, boolean on, boolean hover, boolean enabled, int rgb) {
        if (UiTheme.anime()) UiPanel.chip(g, x, y, w, h, on, hover, 0xFF000000 | rgb);
        else {
            g.fill(x, y, x + w, y + h, on ? UiDraw.GOLD : hover ? UiDraw.PAPER_SHADE : UiDraw.withAlpha(UiDraw.PAPER_SHADE, 110));
            g.renderOutline(x, y, w, h, UiDraw.RULE);
        }
        int color = !enabled ? UiDraw.inkDim() : on && !UiTheme.anime() ? UiDraw.COVER : UiDraw.ink();
        ctx.scissor(g, x + 2, y, x + w - 3, y + h);
        g.drawString(font, text, x + 4, y + (h - 8) / 2, color, false);
        g.disableScissor();
    }

    @Override
    public boolean click(CodexContext ctx, double mx, double my, int button, int x, int y, int w, int h) {
        if (button != 0 || !WeaponPageClient.received()) return false;
        WeaponPageClient.Page p = current();
        if (p == null) return false;
        WeaponPageLayout l = WeaponPageLayout.of(x, y - lastScroll, w);
        int chips = chips();
        if (chips > 0) {
            int c = l.chipAt(mx, my, chips);
            if (c >= 0) {
                WeaponPageClient.select(c);
                return true;
            }
        }
        int o = l.optionAt(mx, my, p.opts.length, chips);
        if (o < 0) return false;
        WeaponPageClient.act(p.opts[o], WeaponPageClient.selected());
        return true;
    }
}
