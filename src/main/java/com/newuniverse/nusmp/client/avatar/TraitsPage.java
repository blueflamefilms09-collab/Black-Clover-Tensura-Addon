package com.newuniverse.nusmp.client.avatar;

import com.newuniverse.nusmp.avatar.AvatarPersonality;
import com.newuniverse.nusmp.avatar.menu.ForgeLayout.Rect;
import com.newuniverse.nusmp.avatar.menu.ForgeState;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/**
 * Why: the third category of the Soul Forge, "Traits": the Avatar's personality, chosen as a PRIMARY and an optional SECONDARY trope out of the fourteen the
 * chatter engine and the combat AI know. Picking one fills the module with what it means, and asks the server for a sample line (the chatter lines are data
 * that only the server has); the line appears in a speech bubble over the Avatar on the stage, so the creator hears the personality before committing.
 * The two-step choice (a mode switch, then one grid) keeps all fourteen on screen in a small window; the rules (no personality twice) are
 * {@link ForgeState}'s.
 */
final class TraitsPage extends ForgePage {
    private static final int CHIP_H = 14, GAP = 3;
    private static final AvatarPersonality[] ALL = AvatarPersonality.values();

    private boolean secondaryMode;
    private final Rect[] modeRects = {Rect.EMPTY, Rect.EMPTY};
    private Rect grid = Rect.EMPTY, text = Rect.EMPTY, hear = Rect.EMPTY;
    private int cols = 2, chipW = 40, scroll;

    TraitsPage(ForgeHost host) { super(host); }

    @Override int accent() { return 0xE8C468; }

    @Override String title() { return ForgeText.s("nusmp.avatar.forge.page.traits", "Personality"); }

    @Override
    void layout(Rect area) {
        super.layout(area);
        int mw = (area.w() - GAP) / 2;
        modeRects[0] = new Rect(area.x(), area.y(), mw, CHIP_H);
        modeRects[1] = new Rect(area.x() + mw + GAP, area.y(), area.w() - mw - GAP, CHIP_H);
        int hearH = 16;
        int textH = area.h() >= 150 ? 3 * 10 : area.h() >= 120 ? 2 * 10 : 10;
        hear = new Rect(area.x(), area.bottom() - hearH, area.w(), hearH);
        text = new Rect(area.x(), hear.y() - 3 - textH, area.w(), textH);
        int gridTop = modeRects[0].bottom() + 4;
        grid = new Rect(area.x(), gridTop, area.w(), Math.max(0, text.y() - 3 - gridTop));
        cols = area.w() >= 190 ? 3 : 2;
        chipW = (area.w() - (cols - 1) * GAP) / cols;
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
    }

    private int rows() { return (ALL.length + cols - 1) / cols; }

    private int visibleRows() { return Math.max(1, (grid.h() + GAP) / (CHIP_H + GAP)); }

    private int maxScroll() { return Math.max(0, rows() - visibleRows()); }

    private Rect chip(int i) {
        int r = i / cols - scroll, c = i % cols;
        return new Rect(grid.x() + c * (chipW + GAP), grid.y() + r * (CHIP_H + GAP), chipW, CHIP_H);
    }

    private boolean inView(int i) {
        int r = i / cols - scroll;
        return r >= 0 && r < visibleRows();
    }

    /** A steady, pleasant colour for each personality (spread round the colour wheel). */
    static int colourOf(AvatarPersonality p) {
        float hue = (p.ordinal() * 0.073f + 0.02f) % 1f;
        return hsv(hue, 0.55f, 1.0f);
    }

    private static int hsv(float h, float s, float v) {
        float r = 0, g = 0, b = 0;
        int i = (int) Math.floor(h * 6f);
        float f = h * 6f - i, p = v * (1f - s), q = v * (1f - f * s), t = v * (1f - (1f - f) * s);
        switch (((i % 6) + 6) % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
    }

    @Override
    void render(GuiGraphics g, int mx, int my, float partial) {
        var font = h.font();
        ForgeState st = h.state();
        ForgeUi.chip(g, font, modeRects[0], ForgeText.s("nusmp.avatar.forge.traits.primary", "Primary"), !secondaryMode, h.hover("mode0", modeRects[0].contains(mx, my)), 0xE8C468);
        ForgeUi.chip(g, font, modeRects[1], ForgeText.s("nusmp.avatar.forge.traits.secondary", "Secondary"), secondaryMode, h.hover("mode1", modeRects[1].contains(mx, my)), 0x9A7CFF);
        if (modeRects[0].contains(mx, my)) h.tooltip(List.of(Component.translatable("nusmp.avatar.gui.primary").withStyle(ChatFormatting.GOLD)));
        if (modeRects[1].contains(mx, my)) h.tooltip(List.of(Component.translatable("nusmp.avatar.gui.secondary").withStyle(ChatFormatting.LIGHT_PURPLE)));
        AvatarPersonality hovered = null;
        for (int i = 0; i < ALL.length; i++) {
            if (!inView(i)) continue;
            AvatarPersonality p = ALL[i];
            Rect c = chip(i);
            boolean prim = p == st.primary(), sec = p == st.secondary(), over = c.contains(mx, my);
            if (over) hovered = p;
            float hv = h.hover("trait" + i, over);
            int rgb = prim ? 0xE8C468 : sec ? 0x9A7CFF : colourOf(p);
            ForgeUi.chip(g, font, c, p.displayName, prim || sec, hv, rgb);
            if (prim) ForgeUi.text(g, font, "1", c.x() + 3, c.y() + 3, 0xFFFFFF);
            if (sec) ForgeUi.text(g, font, "2", c.x() + 3, c.y() + 3, 0xFFFFFF);
        }
        ForgeUi.scrollbar(g, new Rect(grid.right() - 2, grid.y(), 2, grid.h()), scroll, maxScroll(), visibleRows(), rows(), 0xE8C468);
        if (hovered != null) {
            h.leanTo(colourOf(hovered));
            h.tooltip(List.of(Component.literal(hovered.displayName).withStyle(ChatFormatting.GOLD),
                    Component.translatable("nusmp.avatar.persona." + hovered.name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.GRAY)));
        }
        // what the chosen personality means
        AvatarPersonality p = st.primary(), s = st.secondary();
        if (text.h() >= 10) {
            int y = text.y();
            if (p == null) ForgeUi.text(g, font, ForgeUi.fit(font, ForgeText.s("nusmp.avatar.forge.traits.hint", "Choose how your Avatar behaves."), text.w()), text.x(), y, UiTheme.TEXT_DIM);
            else {
                List<String> lines = ForgeUi.wrapLines(font, Component.translatable("nusmp.avatar.persona." + p.name().toLowerCase(Locale.ROOT)).getString(), text.w(), Math.max(1, text.h() / 10 - (s != null ? 1 : 0)));
                for (String l : lines) { ForgeUi.text(g, font, l, text.x(), y, UiTheme.TEXT_DIM); y += 10; }
                if (s != null && y + 9 <= text.bottom() + 1) ForgeUi.text(g, font, ForgeUi.fit(font, Component.translatable("nusmp.avatar.gui.hybrid", p.displayName, s.displayName).getString(), text.w()), text.x(), y, UiTheme.VIOLET);
            }
        }
        boolean can = st.primary() != null;
        boolean over = hear.contains(mx, my);
        ForgeUi.button(g, font, hear, ForgeText.s("nusmp.avatar.forge.traits.hear", "Hear a sample line"), can, h.hover("hear", over && can), 0xE8C468);
        if (over && !can) h.tooltip(List.of(ForgeText.t("nusmp.avatar.forge.traits.hear_tip", "Choose a primary personality first.").withStyle(ChatFormatting.GRAY)));
    }

    @Override
    boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        for (int m = 0; m < 2; m++) if (modeRects[m].contains(mx, my)) { h.click(); secondaryMode = m == 1; return true; }
        if (hear.contains(mx, my)) {
            if (h.state().primary() != null) { h.click(); h.requestBark(); }
            return true;
        }
        if (!grid.contains(mx, my)) return false;
        ForgeState st = h.state();
        for (int i = 0; i < ALL.length; i++) {
            if (!inView(i) || !chip(i).contains(mx, my)) continue;
            AvatarPersonality p = ALL[i];
            h.click();
            if (secondaryMode) st.chooseSecondary(st.secondary() == p ? null : p);
            else st.choosePrimary(p);
            h.pulse(colourOf(p));
            h.changed();
            if (st.primary() != null) h.requestBark();
            return true;
        }
        return true;
    }

    @Override
    boolean mouseScrolled(double mx, double my, double dy) {
        if (!grid.contains(mx, my) || maxScroll() <= 0) return false;
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(dy)));
        return true;
    }
}
