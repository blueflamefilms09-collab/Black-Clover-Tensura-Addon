package com.newuniverse.nusmp.client.hub;

import com.newuniverse.nusmp.client.heroweapon.forms.BookLayout.Rect;
import com.newuniverse.nusmp.client.ui.theme.UiBar;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.client.weaponpage.WeaponPageClient;
import com.newuniverse.nusmp.weaponpage.WeaponPageRules;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Why: the Weapons tab of the hub: the Weapon page (docs/ui_weapon_page.md) redrawn in the hub's style, over exactly the same data, packet and rules as the page inside the old Codex ({@link WeaponPageClient}: one
 * validated {@code weaponpage_act} packet, the server decides everything, the client only draws and asks). Weapon chips, a head card (name, grade, level, form), the upgrade option rows, the mastery bars and the other
 * masteries, stacked in one column by {@link HubWeaponLayout} so nothing can overlap at any size or count, scrolled by the screen when taller than the body. Texts are fitted once when the page arrives or the width
 * changes (a cut text shows in full in a tooltip); a frame draws from the cooked arrays and builds no string, list or location. Local coordinates: (0, 0) is the top-left of the scrolled content.
 */
final class HubWeaponsBody {
    private HubWeaponLayout layout;
    private WeaponPageClient.Page[] fitPages;
    private WeaponPageClient.Page fitPage;
    private int fitW = -1;
    private HubText.Fit[] chipFit = new HubText.Fit[0], optFit = new HubText.Fit[0], barFit = new HubText.Fit[0], extraFit = new HubText.Fit[0];
    private HubText.Fit nameFit, gradeFit, levelFit, formFit;
    private String headOptions = "", headMastery = "", headOthers = "", msgEmpty = "", msgLoading = "", msgText = "";
    private List<FormattedCharSequence> msgLines = List.of();
    private int msgW = -1;

    /** Translates the fixed texts (call when the tab opens: the language may have changed). */
    void opened() {
        headOptions = Component.translatable("anime_multiverse.hub.weapons.options").getString();
        headMastery = Component.translatable("anime_multiverse.weaponpage.heading.mastery").getString();
        headOthers = Component.translatable("anime_multiverse.weaponpage.heading.others").getString();
        msgEmpty = Component.translatable("anime_multiverse.weaponpage.empty").getString();
        msgLoading = Component.translatable("anime_multiverse.weaponpage.loading").getString();
        fitW = -1;
        msgW = -1;
    }

    private static WeaponPageClient.Page current() {
        int i = WeaponPageClient.selected();
        return i < 0 ? null : WeaponPageClient.pages()[i];
    }

    private static boolean haveWeapons() { return WeaponPageClient.received() && current() != null; }

    // ---------------------------------------------------------------- cooking and layout (only when the page, the width or a count changed)
    private void ensure(Font font, int w) {
        WeaponPageClient.Page p = current();
        WeaponPageClient.Page[] pages = WeaponPageClient.pages();
        int chips = pages.length, options = p == null ? 0 : p.opts.length, bars = p == null ? 0 : p.barLabel.length, extras = WeaponPageClient.extraLabels().length;
        if (layout != null && fitW == w && fitPages == pages && fitPage == p && layout.chips() == chips && layout.options() == options && layout.bars() == bars && layout.extras() == extras) return;
        layout = HubWeaponLayout.of(w, chips, options, bars, extras);
        fitW = w;
        fitPages = pages;
        fitPage = p;
        java.util.function.ToIntFunction<String> width = font::width;
        chipFit = new HubText.Fit[chips];
        for (int i = 0; i < chips; i++) chipFit[i] = HubText.fit(pages[i].chip, layout.chipW() - 8, width);
        if (p == null) return;
        nameFit = HubText.fit(p.title, w - 12, width);
        gradeFit = HubText.fit(p.grade, w - 12, width);
        levelFit = HubText.fit(p.level, w - 12, width);
        formFit = HubText.fit(p.form, w - 12, width);
        optFit = new HubText.Fit[options];
        for (int i = 0; i < options; i++) optFit[i] = HubText.fit(p.opts[i].text, w - 10, width);
        barFit = new HubText.Fit[bars];
        for (int i = 0; i < bars; i++) barFit[i] = HubText.fit(p.barLabel[i], w - 8 - font.width(p.barValue[i]) - 6, width);
        String[] el = WeaponPageClient.extraLabels(), ev = WeaponPageClient.extraValues();
        extraFit = new HubText.Fit[extras];
        for (int i = 0; i < extras; i++) extraFit[i] = HubText.fit(el[i], w - 8 - font.width(ev[i]) - 6, width);
    }

    private List<FormattedCharSequence> message(Font font, int w) {
        String msg = WeaponPageClient.received() ? msgEmpty : msgLoading;
        if (msgW != w || !msg.equals(msgText)) {
            msgText = msg;
            msgW = w;
            msgLines = font.split(Component.literal(msg), Math.max(20, w - 8));
        }
        return msgLines;
    }

    int height(Font font, int w) {
        if (!haveWeapons()) return 4 + message(font, w).size() * 10;
        ensure(font, w);
        return layout.height();
    }

    // ---------------------------------------------------------------- drawing
    void draw(GuiGraphics g, Font font, int w, int top, int bottom, int mx, int my, float now) {
        if (!haveWeapons()) {
            int y = 2;
            for (FormattedCharSequence line : message(font, w)) {
                g.drawString(font, line, 2, y, UiTheme.TEXT_DIM, false);
                y += 10;
            }
            return;
        }
        ensure(font, w);
        WeaponPageClient.Page p = current();
        int sel = WeaponPageClient.selected(), rgb = p.rgb;
        if (layout.hasChips()) {
            for (int i = 0; i < layout.chips(); i++) {
                Rect r = layout.chip(i);
                if (r.bottom() < top || r.y() > bottom) continue;
                boolean hover = r.contains(mx, my);
                UiPanel.chip(g, r.x(), r.y(), r.w(), r.h(), i == sel, hover, 0xFF000000 | WeaponPageClient.pages()[i].rgb);
                g.drawString(font, chipFit[i].shown(), r.x() + 4, r.y() + (r.h() - 8) / 2, i == sel || hover ? UiTheme.WHITE : UiTheme.TEXT, false);
            }
        }
        Rect head = layout.head();
        if (head.bottom() >= top && head.y() <= bottom) {
            UiPanel.well(g, head.x(), head.y(), head.w(), head.h(), 0xFF000000 | rgb, 1f);
            g.fill(head.x(), head.y(), head.x() + 2, head.bottom(), UiMath.withAlpha(rgb, 0.9f));
            g.drawString(font, nameFit.shown(), head.x() + 7, head.y() + 4, UiTheme.GOLD, false);
            g.drawString(font, gradeFit.shown(), head.x() + 7, head.y() + 15, 0xFF000000 | rgb, false);
            g.drawString(font, levelFit.shown(), head.x() + 7, head.y() + 26, UiTheme.TEXT, false);
            g.drawString(font, formFit.shown(), head.x() + 7, head.y() + 36, UiTheme.TEXT_DIM, false);
        }
        if (p.opts.length > 0 && layout.optionsHeadingY() + 12 >= top && layout.optionsHeadingY() <= bottom) heading(g, font, headOptions, layout.optionsHeadingY(), w, UiTheme.GOLD);
        for (int i = 0; i < p.opts.length; i++) {
            Rect r = layout.option(i);
            if (r.bottom() < top || r.y() > bottom) continue;
            WeaponPageClient.Row row = p.opts[i];
            boolean current = row.state == WeaponPageRules.ST_CURRENT, hot = row.clickable() && r.contains(mx, my);
            UiPanel.chip(g, r.x(), r.y(), r.w(), r.h(), current, hot, 0xFF000000 | rgb);
            if (hot) UiPanel.hover(g, r.x() + 1, r.y() + 1, r.w() - 2, r.h() - 2, 1f, 0xFF000000 | rgb);
            g.drawString(font, optFit[i].shown(), r.x() + 5, r.y() + (r.h() - 8) / 2, row.clickable() || current ? UiTheme.TEXT : UiTheme.TEXT_FAINT, false);
        }
        if (p.barLabel.length > 0 && layout.barsHeadingY() + 12 >= top && layout.barsHeadingY() <= bottom) heading(g, font, headMastery, layout.barsHeadingY(), w, UiTheme.CYAN);
        for (int i = 0; i < p.barLabel.length; i++) {
            Rect r = layout.bar(i);
            if (r.bottom() >= top && r.y() <= bottom) bar(g, font, r, barFit[i].shown(), p.barValue[i], p.barFrac[i], 0xFF000000 | rgb, now);
        }
        String[] ev = WeaponPageClient.extraValues();
        float[] ef = WeaponPageClient.extraFracs();
        if (ev.length > 0 && layout.extrasHeadingY() + 12 >= top && layout.extrasHeadingY() <= bottom) heading(g, font, headOthers, layout.extrasHeadingY(), w, UiTheme.VIOLET);
        for (int i = 0; i < ev.length; i++) {
            Rect r = layout.extra(i);
            if (r.bottom() >= top && r.y() <= bottom) bar(g, font, r, extraFit[i].shown(), ev[i], ef[i], UiTheme.VIOLET, now);
        }
    }

    private static void heading(GuiGraphics g, Font font, String text, int y, int w, int rgb) { UiPanel.heading(g, font, text, 0, y, w, rgb); }

    /** A label line with the value at the right, and the bar under it. */
    private static void bar(GuiGraphics g, Font font, Rect r, String label, String value, float frac, int argb, float now) {
        g.drawString(font, label, r.x() + 2, r.y(), UiTheme.TEXT, false);
        g.drawString(font, value, r.right() - 2 - font.width(value), r.y(), UiTheme.TEXT_DIM, false);
        UiBar.draw(g, r.x() + 2, r.y() + 11, r.w() - 4, 4, frac, argb, now, 1f);
    }

    // ---------------------------------------------------------------- input
    /** A click in local coordinates: a chip picks the weapon, a ready option sends the one packet. Returns true when it hit something. */
    boolean click(Font font, int w, double lx, double ly) {
        if (!haveWeapons()) return false;
        ensure(font, w);
        int c = layout.chipAt(lx, ly);
        if (c >= 0) {
            WeaponPageClient.select(c);
            return true;
        }
        int o = layout.optionAt(lx, ly);
        WeaponPageClient.Page p = current();
        if (o < 0 || p == null) return false;
        WeaponPageClient.act(p.opts[o], WeaponPageClient.selected());
        return true;
    }

    /** The full text of a cut text under the local point (a chip, an option, a bar label, the head card), or null. */
    String tipAt(Font font, int w, double lx, double ly) {
        if (!haveWeapons()) return null;
        ensure(font, w);
        int o = layout.optionAt(lx, ly);
        if (o >= 0 && optFit[o].cut()) return optFit[o].full();
        int c = layout.chipAt(lx, ly);
        if (c >= 0 && chipFit[c].cut()) return chipFit[c].full();
        for (int i = 0; i < barFit.length; i++) if (layout.bar(i).contains(lx, ly) && barFit[i].cut()) return barFit[i].full();
        for (int i = 0; i < extraFit.length; i++) if (layout.extra(i).contains(lx, ly) && extraFit[i].cut()) return extraFit[i].full();
        if (layout.head().contains(lx, ly)) {
            if (nameFit != null && nameFit.cut()) return nameFit.full();
            if (levelFit != null && levelFit.cut()) return levelFit.full();
            if (formFit != null && formFit.cut()) return formFit.full();
        }
        return null;
    }
}
