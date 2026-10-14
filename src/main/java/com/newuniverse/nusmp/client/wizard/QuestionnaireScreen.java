package com.newuniverse.nusmp.client.wizard;

import com.newuniverse.nusmp.client.ui.UiDraw;
import com.newuniverse.nusmp.client.ui.theme.MagicCircle;
import com.newuniverse.nusmp.client.ui.theme.SageBlock;
import com.newuniverse.nusmp.client.ui.theme.ThemedButton;
import com.newuniverse.nusmp.client.ui.theme.UiBar;
import com.newuniverse.nusmp.client.ui.theme.UiLang;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.wizard.WizardNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * The Tower Wizard's questionnaire: one question at a time; the answers go back to the server. Classic look: a sheet of parchment. Anime look
 * (config ui.theme, default): a translucent analysis panel with a magic circle behind the question, a progress bar and a Great-Sage line.
 */
public class QuestionnaireScreen extends Screen {
    private final List<String> texts = new ArrayList<>();
    private final List<List<String>> options = new ArrayList<>();
    private final int[] answers;
    private int index;
    private final SageBlock sage = new SageBlock();
    private final UiBar progress = new UiBar();

    public static void open(CompoundTag sheet) { Minecraft.getInstance().setScreen(new QuestionnaireScreen(sheet)); }

    private QuestionnaireScreen(CompoundTag sheet) {
        super(Component.literal("The Tower Wizard's Questions"));
        ListTag qs = sheet.getList("Q", Tag.TAG_COMPOUND);
        for (int i = 0; i < qs.size(); i++) {
            CompoundTag q = qs.getCompound(i);
            texts.add(q.getString("T"));
            List<String> o = new ArrayList<>();
            ListTag ol = q.getList("O", Tag.TAG_STRING);
            for (int k = 0; k < ol.size(); k++) o.add(ol.getString(k));
            options.add(o);
        }
        answers = new int[texts.size()];
    }

    @Override
    protected void init() {
        int w = Math.min(320, width - 40), x = (width - w) / 2, y = height / 2 - 20;
        List<String> opts = options.get(index);
        for (int i = 0; i < opts.size(); i++) {
            final int pick = i;
            addRenderableWidget(new ThemedButton(x, y + i * 24, w, 20, Component.literal(opts.get(i)), b -> choose(pick)));
        }
    }

    private void choose(int pick) {
        answers[index] = pick;
        index++;
        if (index >= texts.size()) {
            CompoundTag t = new CompoundTag();
            t.putIntArray("A", answers);
            PacketDistributor.sendToServer(new WizardNet.AnswerPayload(t));
            onClose();
        } else {
            rebuildWidgets();
        }
    }

    /** The sheet is part of the background layer: Screen.render calls this once before the buttons, so the blur only covers what is behind the sheet. */
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        super.renderBackground(g, mx, my, partial);
        int w = Math.min(360, width - 20), x = (width - w) / 2, top = height / 2 - 100, bottom = height / 2 + 90;
        if (UiTheme.anime()) {
            UiPanel.panelScan(g, x - 4, top - 4, w + 8, bottom - top + 8, UiTheme.CYAN, 0.9f, 1f, UiTheme.now());
            g.enableScissor(x - 4, top - 4, x + w + 4, bottom + 4);
            MagicCircle.draw(g, width / 2f, (top + bottom) / 2f, 130f, UiTheme.now(), UiTheme.VIOLET, 0.28f, MagicCircle.CALM, 1f);
            g.disableScissor();
            return;
        }
        g.fill(x - 4, top - 4, x + w + 4, bottom + 4, UiDraw.COVER);
        g.fill(x, top, x + w, bottom, UiDraw.PAPER);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);                      // background + sheet, then the answer buttons
        int w = Math.min(360, width - 20), x = (width - w) / 2, top = height / 2 - 100;
        g.drawCenteredString(font, "The Tower Wizard asks  (" + (index + 1) + " / " + texts.size() + ")", width / 2, top + 6, UiDraw.goldInk());
        UiDraw.wrapped(g, font, texts.get(index), x + 12, top + 22, w - 24, UiDraw.ink());
        if (UiTheme.anime()) {                                                  // 1.27.0: progress and the Great Sage's line
            float now = UiTheme.now();
            progress.update(index / (float) texts.size(), now, true);
            progress.draw(g, x + 12, top + 17, w - 24, 3, UiTheme.VIOLET, now, 1f);
            sage.set(font, "", font.plainSubstrByWidth(UiLang.get(UiLang.NOTICE, UiLang.get("nusmp.ui.wizard.hint")), w - 24), "", w - 24, now);
            sage.draw(g, font, x + 12, height / 2 + 79, now, 1f);
        }
    }

    @Override public boolean isPauseScreen() { return false; }
}
