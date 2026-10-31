package com.newuniverse.nusmp.client.beastwill;

import com.newuniverse.nusmp.beastwill.BeastWillNet;
import com.newuniverse.nusmp.client.ui.theme.ThemedButton;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: absorbing a Beast Will is a choice that can not be taken back, so before anything is consumed the player is shown what it is (3.45.0): the will and the beast it came from, the beast's rank and
 * affinity, how well their race fits, the energy (EP) they have against the energy it asks for, and, if the server refused, the reason in words. "Absorb" is only enabled when the server said it is
 * allowed; the server checks again when the player confirms (and takes the core only then), so closing this screen, pressing Decline or running out the clock costs nothing. Closing it in any way sends
 * "decline" unless the player already answered. Modal; a text window with two buttons, so it is a fixed screen of the GUI Move framework (nothing to move).
 */
public final class BeastWillConfirmScreen extends Screen {
    private final BeastWillNet.Preview p;
    private boolean answered;
    private ThemedButton yes;

    public BeastWillConfirmScreen(BeastWillNet.Preview p) {
        super(Component.translatable("nusmp.beastwill.confirm.title"));
        this.p = p;
    }

    @Override public boolean isPauseScreen() { return false; }

    private static final int W = 270, H = 190;

    @Override
    protected void init() {
        int x = (width - W) / 2, y = (height - H) / 2;
        yes = new ThemedButton(x + 14, y + H - 28, 118, 18, Component.translatable("nusmp.beastwill.confirm.absorb"), b -> answer(true));
        yes.active = p.ok();
        addRenderableWidget(yes);
        addRenderableWidget(new ThemedButton(x + W - 14 - 118, y + H - 28, 118, 18, Component.translatable("nusmp.beastwill.confirm.decline"), b -> answer(false)));
    }

    private void answer(boolean absorb) {
        if (!answered) {
            answered = true;
            BeastWillClient.answer(absorb && p.ok());
        }
        onClose();
    }

    @Override
    public void onClose() {
        if (!answered) {
            answered = true;
            BeastWillClient.answer(false);
        }
        super.onClose();
    }

    /** The panel is the background (vanilla's render draws the two buttons over it): a flat dim, never the blurring vanilla background, so the panel is not blurred (ScreenBlurGuardTest). */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderTransparentBackground(g);
        int x = (width - W) / 2, y = (height - H) / 2;
        UiPanel.panel(g, x, y, W, H, p.ok() ? UiTheme.CYAN : UiTheme.RED, 0.8f, 1f);
        Font font = this.font;
        int cx = x + 14, iw = W - 28, cy = y + 12;
        g.drawCenteredString(font, getTitle(), x + W / 2, cy, UiTheme.GOLD);
        cy += 16;
        g.drawString(font, BeastWillClient.name(p.will()), cx, cy, UiTheme.WHITE, true);
        String rank = Component.translatable("nusmp.beastwill.hud.rank", BeastWillClient.rankName(p.rank())).getString();
        g.drawString(font, rank, x + W - 14 - font.width(rank), cy, UiTheme.GOLD, false);
        cy += 12;
        g.drawString(font, font.plainSubstrByWidth(Component.translatable("nusmp.beastwill.confirm.from", p.beast(), p.affinity()).getString(), iw), cx, cy, UiTheme.TEXT_DIM, false);
        cy += 11;
        g.drawString(font, Component.translatable(p.source() == 0 ? "nusmp.beastwill.confirm.source_core" : "nusmp.beastwill.confirm.source_offer"), cx, cy, UiTheme.TEXT_DIM, false);
        cy += 16;

        boolean epOk = p.haveEp() >= p.needEp();
        g.drawString(font, Component.translatable("nusmp.beastwill.confirm.ep", Math.round(p.haveEp()), Math.round(p.needEp())), cx, cy, epOk ? UiTheme.GREEN : UiTheme.RED, false);
        cy += 11;
        g.drawString(font, Component.translatable("nusmp.beastwill.confirm.fit", p.fitPercent()), cx, cy, p.fitPercent() >= 100 ? UiTheme.GREEN : UiTheme.GOLD, false);
        cy += 14;
        if (!p.ok()) {
            for (String line : wrap(Component.translatable("nusmp.beastwill.msg.no." + p.reason()).getString(), iw)) {
                g.drawString(font, line, cx, cy, UiTheme.RED, false);
                cy += 10;
            }
        } else {
            for (String line : wrap(Component.translatable("nusmp.beastwill.confirm.warning").getString(), iw)) {
                g.drawString(font, line, cx, cy, UiTheme.TEXT, false);
                cy += 10;
            }
        }
    }

    private List<String> wrap(String text, int width) {
        List<String> out = new ArrayList<>();
        for (var seq : font.split(Component.literal(text), width)) {
            StringBuilder sb = new StringBuilder();
            seq.accept((i, style, cp) -> { sb.appendCodePoint(cp); return true; });
            out.add(sb.toString());
            if (out.size() >= 5) break;
        }
        return out;
    }
}
