package com.newuniverse.nusmp.client.wizard;

import com.newuniverse.nusmp.client.ui.UiDraw;
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

/** The Tower Wizard's questionnaire: one question at a time on a sheet of parchment; the answers go back to the server. */
public class QuestionnaireScreen extends Screen {
    private final List<String> texts = new ArrayList<>();
    private final List<List<String>> options = new ArrayList<>();
    private final int[] answers;
    private int index;

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
            addRenderableWidget(Button.builder(Component.literal(opts.get(i)), b -> choose(pick)).bounds(x, y + i * 24, w, 20).build());
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

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g, mx, my, partial);
        int w = Math.min(360, width - 20), x = (width - w) / 2, top = height / 2 - 100, bottom = height / 2 + 90;
        g.fill(x - 4, top - 4, x + w + 4, bottom + 4, UiDraw.COVER);
        g.fill(x, top, x + w, bottom, UiDraw.PAPER);
        g.drawCenteredString(font, "The Tower Wizard asks  (" + (index + 1) + " / " + texts.size() + ")", width / 2, top + 6, UiDraw.GOLD_INK);
        UiDraw.wrapped(g, font, texts.get(index), x + 12, top + 22, w - 24, UiDraw.INK);
        super.render(g, mx, my, partial);
    }

    @Override public boolean isPauseScreen() { return false; }
}
