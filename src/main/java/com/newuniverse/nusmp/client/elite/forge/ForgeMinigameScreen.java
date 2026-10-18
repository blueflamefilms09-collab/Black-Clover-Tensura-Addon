package com.newuniverse.nusmp.client.elite.forge;

import com.newuniverse.nusmp.client.ui.theme.ThemedButton;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.elite.forge.net.ForgeNet;
import com.newuniverse.nusmp.elite.forge.rules.MinigameType;
import com.newuniverse.nusmp.elite.forge.rules.Minigames;
import com.newuniverse.nusmp.elite.forge.rules.Quality;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the screen that PLAYS a forging session. The server sent the steps and one seed per step ({@link ForgeNet.Start}); this screen runs them one after the other
 * (each begins with a click, so the player is ready), shows the score of each step, then sends only what was PLAYED ({@link ForgeNet.Result}: strike times, mouse
 * samples, button samples, ring angles) and waits for the server's verdict ({@link ForgeNet.Done}), which it then shows: the item, its quality, the points it was made
 * of, whether the station held it back, the engravings. It never computes or sends a score. Closing the screen before the result is sent tells the server to forget the
 * session (nothing was taken from the player, so nothing is lost). The panel is drawn in {@code renderBackground} (after the vanilla blur, as the screen-blur guard
 * test requires), the live game in {@code render}.
 */
public class ForgeMinigameScreen extends Screen {
    private enum Phase { INTRO, PLAY, RECAP, JUDGING, RESULT }

    private static final int W = 330, H = 236;

    private final ForgeNet.Start start;
    private final List<MinigameView> views = new ArrayList<>();
    private final List<int[]> inputs = new ArrayList<>();
    private final List<Integer> scores = new ArrayList<>();
    private Phase phase = Phase.INTRO;
    private int step;
    private int phaseTicks;
    private boolean sent;
    private boolean mouseDown;
    private int mx, my;
    private ForgeNet.Done done;
    private ForgeNet.OpenStation pending;

    public ForgeMinigameScreen(ForgeNet.Start start) {
        super(Component.translatable("nusmp.eforge.mg.title"));
        this.start = start;
        for (ForgeNet.StepView s : start.steps()) views.add(make(MinigameType.byId(s.type())));
    }

    private static MinigameView make(MinigameType t) {
        return switch (t == null ? MinigameType.HAMMER_TIMING : t) {
            case HAMMER_TIMING -> new HammerTimingView();
            case MAGICULE_FLOW -> new MagiculeFlowView();
            case RUNE_ALIGNMENT -> new RuneAlignmentView();
            case TEMPERATURE_FORGE -> new TemperatureForgeView();
        };
    }

    private int px() { return (width - W) / 2; }

    private int py() { return (height - H) / 2; }

    /** The game area inside the panel. */
    private int gx() { return px() + 16; }

    private int gy() { return py() + 66; }

    private int gw() { return W - 32; }

    private int gh() { return 140; }

    @Override
    protected void init() {
        if (phase == Phase.RESULT) {
            int y = py() + H - 28;
            addRenderableWidget(new ThemedButton(px() + 16, y, 140, 16, Component.translatable("nusmp.eforge.mg.back"), b -> backToStation()));
            addRenderableWidget(new ThemedButton(px() + W - 156, y, 140, 16, Component.translatable("nusmp.eforge.mg.close"), b -> onClose()));
        }
    }

    /** The refreshed station data arrived while the result was on screen. */
    void setPending(ForgeNet.OpenStation p) { this.pending = p; }

    void setDone(ForgeNet.Done d) {
        this.done = d;
        this.phase = Phase.RESULT;
        this.phaseTicks = 0;
        rebuildWidgets();
    }

    private void backToStation() {
        Minecraft mc = Minecraft.getInstance();
        if (pending != null) mc.setScreen(new ForgeStationScreen(pending));
        else onClose();
    }

    private MinigameType currentType() { return MinigameType.byId(start.steps().get(step).type()); }

    private void beginStep() {
        ForgeNet.StepView s = start.steps().get(step);
        views.get(step).begin(start.seeds()[step], s.difficulty());
        phase = Phase.PLAY;
        phaseTicks = 0;
    }

    @Override
    public void tick() {
        super.tick();
        phaseTicks++;
        if (phase == Phase.PLAY) {
            MinigameView v = views.get(step);
            v.tick(mx, my, mouseDown);
            if (v.finished()) {
                int[] in = v.input();
                inputs.add(in);
                MinigameType t = currentType();
                ForgeNet.StepView s = start.steps().get(step);
                int sc = t == null ? 0 : Minigames.of(t).score(start.seeds()[step], s.difficulty(), in);
                scores.add(sc);
                phase = Phase.RECAP;
                phaseTicks = 0;
                mouseDown = false;
            }
        } else if (phase == Phase.RECAP && phaseTicks >= 30) {
            step++;
            if (step >= views.size()) {
                PacketDistributor.sendToServer(new ForgeNet.Result(start.session(), List.copyOf(inputs)));
                sent = true;
                phase = Phase.JUDGING;
            } else {
                phase = Phase.INTRO;
            }
            phaseTicks = 0;
        } else if (phase == Phase.JUDGING && phaseTicks > 200) {
            setDone(ForgeNet.Done.fail("nusmp.eforge.msg.no_answer"));
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        mx = (int) x;
        my = (int) y;
        if (phase == Phase.INTRO && button == 0) {
            beginStep();
            return true;
        }
        if (phase == Phase.PLAY) {
            if (button == 0) mouseDown = true;
            if (views.get(step).click(gx(), gy(), gw(), gh(), x, y, button)) return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        mx = (int) x;
        my = (int) y;
        if (phase == Phase.PLAY) {
            views.get(step).drag(gx(), gy(), gw(), gh(), x, y);
            return true;
        }
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (button == 0) mouseDown = false;
        if (phase == Phase.PLAY) views.get(step).release(x, y, button);
        return super.mouseReleased(x, y, button);
    }

    @Override
    public void mouseMoved(double x, double y) {
        mx = (int) x;
        my = (int) y;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (phase == Phase.INTRO && (keyCode == 32 || keyCode == 257)) {
            beginStep();
            return true;
        }
        if (phase == Phase.PLAY && views.get(step).key(keyCode)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        if (!sent && phase != Phase.RESULT) PacketDistributor.sendToServer(new ForgeNet.Abort(start.session()));
        sent = true;
        super.removed();
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.renderBackground(g, mouseX, mouseY, partial);
        UiPanel.panelScan(g, px(), py(), W, H, UiTheme.GOLD, 0.8f, 1f, UiTheme.now());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        mx = mouseX;
        my = mouseY;
        super.render(g, mouseX, mouseY, partial);
        int x = px(), y = py();
        ResourceLocation outId = ResourceLocation.tryParse(recipeOutItem());
        ItemStack out = outId == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(outId));
        g.renderItem(out, x + 14, y + 12);
        g.drawString(font, Component.translatable("nusmp.eforge.mg.heading", out.isEmpty() ? Component.literal("?") : out.getHoverName()), x + 36, y + 10, UiTheme.GOLD, false);
        g.drawString(font, Component.translatable("nusmp.eforge.mg.station", Component.translatable("nusmp.eforge.tier." + tierId())), x + 36, y + 22, UiTheme.TEXT_DIM, false);
        // the step pips
        for (int i = 0; i < views.size(); i++) {
            int col = i < scores.size() ? scoreColor(scores.get(i)) : i == step && phase != Phase.RESULT && phase != Phase.JUDGING ? ForgeDraw.CYAN : ForgeDraw.FAINT;
            g.fill(x + W - 16 - (views.size() - i) * 14, y + 14, x + W - 16 - (views.size() - i) * 14 + 10, y + 24, col);
        }
        switch (phase) {
            case INTRO -> renderIntro(g);
            case PLAY -> renderPlay(g, mouseX, mouseY);
            case RECAP -> renderRecap(g);
            case JUDGING -> g.drawCenteredString(font, Component.translatable("nusmp.eforge.mg.judging"), x + W / 2, y + H / 2, UiTheme.TEXT);
            case RESULT -> renderResult(g);
        }
    }

    private String recipeOutItem() { return start.outItem(); }

    private String tierId() {
        return switch (start.tier()) {
            case 1 -> "magisteel";
            case 2 -> "mythril";
            case 3 -> "dragonforge";
            default -> "basic";
        };
    }

    private static int scoreColor(int sc) { return sc >= 900 ? ForgeDraw.GOLD : sc >= 650 ? ForgeDraw.GREEN : sc >= 350 ? ForgeDraw.ORANGE : ForgeDraw.RED; }

    private Component stepName() {
        ForgeNet.StepView s = start.steps().get(Math.min(step, start.steps().size() - 1));
        return Component.translatable("nusmp.eforge.mg.type." + s.type());
    }

    private void renderIntro(GuiGraphics g) {
        int x = px(), y = py();
        ForgeNet.StepView s = start.steps().get(step);
        g.drawCenteredString(font, Component.translatable("nusmp.eforge.mg.step", step + 1, views.size(), stepName()), x + W / 2, y + 70, UiTheme.GOLD);
        g.drawCenteredString(font, Component.translatable("nusmp.eforge.mg.difficulty", s.difficulty()), x + W / 2, y + 84, UiTheme.TEXT_DIM);
        g.drawCenteredString(font, Component.translatable(views.get(step).hintKey()), x + W / 2, y + 108, UiTheme.TEXT);
        g.drawCenteredString(font, Component.translatable("nusmp.eforge.mg.click_to_begin"), x + W / 2, y + 150, ForgeDraw.alpha(ForgeDraw.CYAN, 0.6f + 0.4f * (float) Math.sin(UiTheme.now() / 6f)));
    }

    private void renderPlay(GuiGraphics g, int mouseX, int mouseY) {
        int x = px(), y = py();
        g.drawCenteredString(font, Component.translatable("nusmp.eforge.mg.step", step + 1, views.size(), stepName()), x + W / 2, y + 36, UiTheme.GOLD);
        g.drawCenteredString(font, Component.translatable(views.get(step).hintKey()), x + W / 2, y + 50, UiTheme.TEXT_DIM);
        UiPanel.well(g, gx() - 2, gy() - 2, gw() + 4, gh() + 4, UiTheme.CYAN, 0.9f);
        g.enableScissor(gx(), gy(), gx() + gw(), gy() + gh());
        views.get(step).render(g, font, gx(), gy(), gw(), gh(), mouseX, mouseY, mouseDown);
        g.disableScissor();
    }

    private void renderRecap(GuiGraphics g) {
        int x = px(), y = py();
        int sc = scores.get(scores.size() - 1);
        g.drawCenteredString(font, Component.translatable("nusmp.eforge.mg.step_done", stepName()), x + W / 2, y + 90, UiTheme.TEXT);
        g.drawCenteredString(font, Component.translatable("nusmp.eforge.mg.score", sc / 10), x + W / 2, y + 110, scoreColor(sc));
    }

    private void renderResult(GuiGraphics g) {
        int x = px(), y = py();
        if (done == null) return;
        if (!done.ok()) {
            g.drawCenteredString(font, Component.translatable("nusmp.eforge.mg.failed"), x + W / 2, y + 80, UiTheme.RED);
            g.drawCenteredString(font, Component.translatable(done.message()), x + W / 2, y + 100, UiTheme.TEXT);
            return;
        }
        Quality q = Quality.ofLevel(done.quality());
        ResourceLocation rl = ResourceLocation.tryParse(done.outItem());
        ItemStack out = rl == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(rl), done.outCount());
        g.pose().pushPose();
        g.pose().translate(x + W / 2 - 16, y + 44, 0);
        g.pose().scale(2f, 2f, 1f);
        g.renderItem(out, 0, 0);
        g.pose().popPose();
        int col = q == null ? UiTheme.TEXT : 0xFF000000 | q.rgb();
        g.drawCenteredString(font, out.getHoverName(), x + W / 2, y + 80, UiTheme.TEXT);
        g.drawCenteredString(font, Component.translatable("nusmp.eforge.quality." + (q == null ? "common" : q.id())), x + W / 2, y + 92, col);
        g.drawCenteredString(font, Component.translatable("nusmp.eforge.mg.result_points", done.score() / 10, done.points10() / 10), x + W / 2, y + 108, UiTheme.TEXT_DIM);
        int ly = y + 122;
        if (done.capped()) {
            g.drawCenteredString(font, Component.translatable("nusmp.eforge.mg.capped"), x + W / 2, ly, ForgeDraw.ORANGE);
            ly += 12;
        }
        if (!done.engravings().isEmpty()) {
            g.drawCenteredString(font, Component.translatable("nusmp.eforge.mg.engraved", String.join(", ", done.engravings())), x + W / 2, ly, ForgeDraw.VIOLET);
            ly += 12;
        }
        for (int i = 0; i < scores.size() && ly < y + H - 40; i++) {
            g.drawCenteredString(font, Component.translatable("nusmp.eforge.mg.step_line", i + 1, Component.translatable("nusmp.eforge.mg.type." + start.steps().get(i).type()), scores.get(i) / 10), x + W / 2, ly, scoreColor(scores.get(i)));
            ly += 10;
        }
    }
}
