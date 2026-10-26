package com.newuniverse.nusmp.client.veldanava;

import com.newuniverse.nusmp.client.ui.theme.MagicCircle;
import com.newuniverse.nusmp.client.ui.theme.ThemedButton;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.entity.veldanava.skills.JudgmentRules;
import com.newuniverse.nusmp.entity.veldanava.skills.VeldanavaSkillNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Why: the Will of the Creator offers one ultimate skill of five, once in a player's life, and a player should SEE the choice: five tall cards in a row (title, epithet, a seal on the ones this
 * release cannot give yet) and one click on a card to take it. It only asks: the click sends {@code nusmp:veldanava_skills_claim} with the card id and the server judges it (the right
 * orb, the card unsealed, nothing taken before) and answers in chat. A player who already holds a skill sees one button for the loot alone. Closing the screen costs nothing: the same choice stays
 * in chat as clickable lines and the orb waits.
 *
 * <p>The panel is drawn in {@link #renderBackground}, never in render() (the blur rule of the 1.51.0 menus, kept by ScreenBlurGuardTest). Client only; nothing is drawn per frame beyond fills and text.
 */
public class VeldanavaSkillScreen extends Screen {
    private static final int CARD_W = 74, CARD_H = 150, GAP = 8, PAD = 16, HEAD = 40, FOOT = 40;
    private final String chosen;
    private boolean sent;

    public VeldanavaSkillScreen(String chosen) {
        super(Component.translatable("nusmp.veldanava.skills.title"));
        this.chosen = chosen;
    }

    private boolean already() { return !chosen.isEmpty(); }

    private int panelW() { return JudgmentRules.CARDS.size() * CARD_W + (JudgmentRules.CARDS.size() - 1) * GAP + PAD * 2; }

    private int panelH() { return HEAD + CARD_H + FOOT; }

    private int px() { return (width - panelW()) / 2; }

    private int py() { return (height - panelH()) / 2; }

    private int cardX(int i) { return px() + PAD + i * (CARD_W + GAP); }

    private int cardY() { return py() + HEAD; }

    @Override
    protected void init() {
        AnimatedIcons.openCards();                                       // 3.17.0: the animated HD icons tick only while this screen is open
        int y = py() + panelH() - 28;
        if (already()) {
            addRenderableWidget(new ThemedButton(px() + PAD, y, 150, 16, Component.translatable("nusmp.veldanava.skills.loot"), b -> send("loot")));
        }
        addRenderableWidget(new ThemedButton(px() + panelW() - PAD - 100, y, 100, 16, Component.translatable("nusmp.veldanava.skills.later"), b -> onClose()));
    }

    @Override
    public void removed() {
        AnimatedIcons.closeCards();
        super.removed();
    }

    private void send(String id) {
        if (sent) return;
        sent = true;                                                     // one claim per opening; the server answers in chat and the orb is done with this player
        PacketDistributor.sendToServer(new VeldanavaSkillNet.Claim(id));
        onClose();
    }

    private int hovered(double mx, double my) {
        List<JudgmentRules.Card> cards = JudgmentRules.CARDS;
        for (int i = 0; i < cards.size(); i++) {
            int x = cardX(i), y = cardY();
            if (mx >= x && mx < x + CARD_W && my >= y && my < y + CARD_H) return i;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && !already()) {
            int i = hovered(mx, my);
            if (i >= 0 && JudgmentRules.CARDS.get(i).available()) { send(JudgmentRules.CARDS.get(i).id()); return true; }
        }
        return super.mouseClicked(mx, my, button);
    }

    /** The panel and the cards are drawn here, right after the background, never in render() (see the class comment). */
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        super.renderBackground(g, mx, my, partial);
        float now = UiTheme.now();
        int x0 = px(), y0 = py();
        UiPanel.panelScan(g, x0, y0, panelW(), panelH(), UiTheme.GOLD, 0.8f, 1f, now);
        g.enableScissor(x0 + 1, y0 + 1, x0 + panelW() - 1, y0 + panelH() - 1);
        MagicCircle.draw(g, x0 + panelW() / 2, y0 + panelH() / 2, 120f, now, UiTheme.GOLD, 0.12f, MagicCircle.CALM, 1f);
        g.disableScissor();
        g.drawCenteredString(font, Component.translatable("nusmp.veldanava.skills.title"), x0 + panelW() / 2, y0 + 10, UiTheme.GOLD);
        g.drawCenteredString(font, Component.translatable(already() ? "nusmp.veldanava.skills.have" : "nusmp.veldanava.skills.hint"), x0 + panelW() / 2, y0 + 23, UiTheme.TEXT_DIM);

        int hover = already() ? -1 : hovered(mx, my);
        List<JudgmentRules.Card> cards = JudgmentRules.CARDS;
        for (int i = 0; i < cards.size(); i++) {
            JudgmentRules.Card c = cards.get(i);
            int x = cardX(i), y = cardY();
            boolean mine = already() && c.id().equals(chosen);
            int accent = c.available() ? UiTheme.GOLD : UiTheme.TEXT_FAINT;
            UiPanel.well(g, x, y, CARD_W, CARD_H, accent, c.available() ? 1f : 0.6f);
            if (i == hover && c.available()) UiPanel.hover(g, x, y, CARD_W, CARD_H, 0.6f, 0xE8C468);
            if (mine) UiPanel.brackets(g, x, y, CARD_W, CARD_H, 8, UiTheme.GREEN);
            int cx = x + CARD_W / 2;
            MagicCircle.draw(g, cx, y + 44, 29f, now + i * 7f, accent, c.available() ? 0.55f : 0.2f, MagicCircle.CALM, 1f);
            AnimatedIcons.Pick icon = AnimatedIcons.cardIcon(c.id());                    // 3.17.0: the painted icon in the ring (animated, still or classic by the client option)
            if (!c.available()) g.setColor(0.42f, 0.42f, 0.52f, 1f);                     // a sealed card shows its icon dimmed
            g.blit(icon.texture(), cx - 24, y + 20, 48, 48, 0f, 0f, icon.size(), icon.size(), icon.size(), icon.size());
            g.setColor(1f, 1f, 1f, 1f);
            g.drawCenteredString(font, Component.literal(c.title()), cx, y + 78, c.available() ? UiTheme.TEXT : UiTheme.TEXT_FAINT);
            int ty = y + 92;
            for (var line : font.split(Component.literal(c.epithet()), CARD_W - 8)) {
                g.drawCenteredString(font, line, cx, ty, c.available() ? UiTheme.TEXT_DIM : UiTheme.TEXT_FAINT);
                ty += 10;
            }
            Component tag = mine ? Component.translatable("nusmp.veldanava.skills.yours")
                    : Component.translatable(c.available() ? "nusmp.veldanava.skills.take" : "nusmp.veldanava.skills.sealed");
            g.drawCenteredString(font, tag, cx, y + CARD_H - 14, mine ? UiTheme.GREEN : c.available() ? UiTheme.GOLD : UiTheme.TEXT_FAINT);
        }
    }

    @Override public boolean isPauseScreen() { return false; }
}
