package com.newuniverse.nusmp.client.avatar;

import com.newuniverse.nusmp.avatar.menu.BuilderState;
import com.newuniverse.nusmp.avatar.menu.ForgeLayout.Rect;
import com.newuniverse.nusmp.avatar.net.S2C_RollPayload;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/**
 * Why: the first category of the Soul Forge, "Identity": the optional name of the Avatar and the roll of its six ability scores. The stats are the server's
 * (the screen only asks for the roll and shows what came back, one card settling after another), the name is checked by the same rule as the classic screen
 * ({@link BuilderState#nameOk}). The twenty-sided die that tumbles while the server answers and then lands on the best score is drawn by the screen on the
 * stage; this page only starts it and tells it the number.
 */
final class IdentityPage extends ForgePage {
    private static final String[] STATS = {"str", "dex", "con", "int", "wis", "cha"};
    private static final int ROLL_ANIM = 26;

    private Rect nameRect = Rect.EMPTY, rollRect = Rect.EMPTY, cardsArea = Rect.EMPTY;
    private int footerY;
    private boolean awaiting;
    private int awaitTicks, rollAnim;

    IdentityPage(ForgeHost host) { super(host); }

    @Override int accent() { return 0x5CD8FF; }

    @Override String title() { return ForgeText.s("nusmp.avatar.forge.page.identity", "Identity"); }

    @Override
    void layout(Rect area) {
        super.layout(area);
        int labelH = h.font().lineHeight + 2;
        nameRect = new Rect(area.x(), area.y() + labelH, area.w(), 16);
        rollRect = new Rect(area.x(), nameRect.bottom() + 8, area.w(), 18);
        int top = rollRect.bottom() + 6;
        int footerLines = area.h() >= 150 ? 4 : 3;
        footerY = area.bottom() - footerLines * (h.font().lineHeight + 1);
        int avail = Math.max(0, footerY - top - 2);
        if (avail < 2 * 24 + 4) { footerY = area.bottom(); avail = Math.max(0, area.bottom() - top); }       // too short for the footer: the cards win
        cardsArea = new Rect(area.x(), top, area.w(), avail);
        EditBox box = h.nameBox();
        box.setX(nameRect.x() + 5);
        box.setY(nameRect.y() + (nameRect.h() - 8) / 2);
        box.setWidth(nameRect.w() - 10);
    }

    /** The server's roll arrived: the cards settle one after another. */
    void rollArrived() {
        awaiting = false;
        rollAnim = h.motion() ? ROLL_ANIM : 0;
    }

    /** The server refused the roll (or never answered). */
    void rollRefused() { awaiting = false; }

    boolean awaiting() { return awaiting; }

    @Override
    void tick() {
        if (rollAnim > 0) rollAnim--;
        if (awaiting && ++awaitTicks > 80) {                         // the answer never came: stop waiting, let the die settle
            awaiting = false;
            h.dice().roll(h.dice().value(), 16);
        }
    }

    @Override
    void hidden() {
        EditBox box = h.nameBox();
        box.setFocused(false);
    }

    private void roll() {
        if (awaiting) return;
        awaiting = true;
        awaitTicks = 0;
        h.click();
        h.dice().tumble();
        h.requestStatRoll();
    }

    @Override
    boolean mouseClicked(double mx, double my, int button) {
        EditBox box = h.nameBox();
        if (nameRect.contains(mx, my)) {
            double cy = Math.max(box.getY(), Math.min(box.getY() + box.getHeight() - 1, my));
            box.setFocused(true);
            box.mouseClicked(mx, cy, button);
            return true;
        }
        box.setFocused(false);
        if (button == 0 && rollRect.contains(mx, my)) { roll(); return true; }
        return false;
    }

    @Override
    boolean keyPressed(int key, int scan, int mods) {
        EditBox box = h.nameBox();
        if (box.isFocused()) { box.keyPressed(key, scan, mods); return true; }          // typing an 'e' must not close the screen as the inventory key would
        return false;
    }

    @Override
    void render(GuiGraphics g, int mx, int my, float partial) {
        var font = h.font();
        float now = h.now();
        S2C_RollPayload r = AvatarClientData.roll;
        // ---- the name
        ForgeUi.text(g, font, ForgeText.s("nusmp.avatar.forge.name", "Name (optional)"), body.x(), body.y(), UiTheme.TEXT_DIM);
        EditBox box = h.nameBox();
        boolean nameHover = nameRect.contains(mx, my);
        float nh = h.hover("name", nameHover || box.isFocused());
        g.fill(nameRect.x(), nameRect.y(), nameRect.right(), nameRect.bottom(), ForgeUi.argb(0x03050A, 0.85f));
        g.renderOutline(nameRect.x(), nameRect.y(), nameRect.w(), nameRect.h(), ForgeUi.argb(box.isFocused() ? 0xE8C468 : 0x5CD8FF, 0.35f + 0.5f * nh));
        box.render(g, mx, my, partial);
        if (!BuilderState.nameOk(box.getValue())) {
            ForgeUi.text(g, font, ForgeUi.fit(font, Component.translatable("nusmp.avatar.gui.confirm.name_bad").getString(), body.w()), body.x(), nameRect.bottom() + 1, UiTheme.RED);
        }
        // ---- the roll button
        boolean rolled = h.state().rolled();
        String label = awaiting ? ForgeText.s("nusmp.avatar.forge.rolling", "Rolling...")
                : Component.translatable(rolled ? "nusmp.avatar.gui.reroll" : "nusmp.avatar.gui.roll").getString();
        boolean over = rollRect.contains(mx, my);
        ForgeUi.button(g, font, rollRect, label, !awaiting, h.hover("roll", over && !awaiting), UiTheme.CYAN & 0xFFFFFF);
        if (over) h.tooltip(List.of(Component.translatable("nusmp.avatar.gui.stats.title"), ForgeText.t("nusmp.avatar.forge.tip.roll",
                "The server rolls your six scores. Luck can earn you a fifth die. Nothing here can be chosen.").withStyle(net.minecraft.ChatFormatting.GRAY)));
        // ---- the six cards
        if (cardsArea.h() >= 24) drawCards(g, r, now);
        // ---- the footer
        if (footerY < body.bottom() && r != null) {
            int lh = font.lineHeight + 1;
            int room = Math.max(1, (body.bottom() - footerY) / lh);
            int y = footerY;
            int dieLines = room >= 4 ? 2 : 1;
            String dice = Component.translatable(r.fiveDice() ? "nusmp.avatar.gui.dice.five" : "nusmp.avatar.gui.dice.four").getString();
            if (dieLines == 1 && font.width(dice) > body.w() && r.fiveDice()) dice = ForgeText.s("nusmp.avatar.forge.dice.five_short", "Lucky roll: 5d6");        // the long line would be cut
            y = ForgeUi.wrapped(g, font, dice, body.x(), y, body.w(), r.fiveDice() ? UiTheme.GOLD : UiTheme.TEXT, dieLines);
            if (y + font.lineHeight <= body.bottom() + 1) {
                ForgeUi.text(g, font, ForgeUi.fit(font, Component.translatable("nusmp.avatar.gui.luck", String.format(Locale.ROOT, "%.1f", r.luck())).getString(), body.w()), body.x(), y, UiTheme.VIOLET);
                y += lh;
            }
            if (r.rerollCost() > 0 && y + font.lineHeight <= body.bottom() + 1) {
                ForgeUi.text(g, font, ForgeUi.fit(font, Component.translatable("nusmp.avatar.gui.reroll_cost", r.rerollCost()).getString(), body.w()), body.x(), y, UiTheme.TEXT_DIM);
            }
        } else if (footerY < body.bottom() && r == null) {
            ForgeUi.wrapped(g, font, Component.translatable("nusmp.avatar.gui.stats.hint").getString(), body.x(), footerY, body.w(), UiTheme.TEXT_DIM, Math.max(1, (body.bottom() - footerY) / (font.lineHeight + 1)));
        }
    }

    private void drawCards(GuiGraphics g, S2C_RollPayload r, float now) {
        var font = h.font();
        int gap = 4;
        int cw = (cardsArea.w() - 2 * gap) / 3;
        int ch = Math.max(24, Math.min(46, (cardsArea.h() - gap) / 2));
        boolean abbreviate = false;                                  // one rule for all six: either every card shows the full name or every card shows STR, DEX ...
        for (String k : STATS) if (font.width(Component.translatable("nusmp.avatar.gui.stat." + k).getString()) > cw - 4) abbreviate = true;
        for (int i = 0; i < 6; i++) {
            int cx = cardsArea.x() + (i % 3) * (cw + gap), cy = cardsArea.y() + (i / 3) * (ch + gap);
            Rect c = new Rect(cx, cy, cw, ch);
            int value = r == null ? 0 : r.stats()[i];
            boolean settled = r != null && rollAnim <= 0;
            int accent = r == null ? 0x5C6E88 : value >= 16 ? 0xE8C468 : value <= 8 ? 0xFF6A6A : 0x5CD8FF;
            g.fill(c.x(), c.y(), c.right(), c.bottom(), ForgeUi.argb(0x060912, 0.85f));
            g.renderOutline(c.x(), c.y(), c.w(), c.h(), ForgeUi.argb(accent, settled ? 0.7f : 0.35f));
            String name = abbreviate ? STATS[i].toUpperCase(Locale.ROOT) : Component.translatable("nusmp.avatar.gui.stat." + STATS[i]).getString();
            ForgeUi.centered(g, font, ForgeUi.fit(font, name, cw - 2), c.cx(), c.y() + 2, UiTheme.TEXT_DIM);
            String num;
            int col = UiTheme.TEXT;
            if (r == null) { num = "--"; col = UiTheme.TEXT_FAINT; }
            else {
                int settle = ROLL_ANIM - 4 * i - 6;                    // the stats settle one after another
                boolean rolling = rollAnim > settle && settle > 0;
                num = rolling ? String.valueOf(3 + (int) ((now * 7 + i * 5 + rollAnim * 13) % 16)) : String.valueOf(value);
                if (!rolling) col = value >= 16 ? UiTheme.GOLD : value <= 8 ? UiTheme.RED : UiTheme.TEXT;
            }
            float s = ch >= 36 ? 1.8f : 1.3f;
            g.pose().pushPose();
            g.pose().translate(c.cx(), c.y() + 3 + font.lineHeight + (ch - 3 - font.lineHeight - 8 * s) / 2f - 1f, 0f);
            g.pose().scale(s, s, 1f);
            g.drawString(font, num, -font.width(num) / 2, 0, 0xFF000000 | (col & 0xFFFFFF), false);
            g.pose().popPose();
            if (r != null && rollAnim <= 0 && ch >= 34) {
                int mod = Math.floorDiv(value - 10, 2);
                ForgeUi.centered(g, font, (mod >= 0 ? "+" : "") + mod, c.cx(), c.bottom() - font.lineHeight - 1, mod >= 0 ? UiTheme.GREEN : UiTheme.RED);
            }
        }
    }
}
