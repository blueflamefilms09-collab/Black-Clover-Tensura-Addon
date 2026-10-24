package com.newuniverse.nusmp.client.ui.gs;

import com.mojang.brigadier.Command;
import com.newuniverse.nusmp.client.ui.theme.ThemedButton;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.client.ui.theme.UiThemeOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/**
 * The crest move screen: drag the alert crest to where you want it, then Save (or Enter); Reset puts it back in the top-right corner; Test notice
 * shows a sample notice beside it. Opened by the client command {@code /nusmp_gui_crest} or the "Alert crest" button of the GUI Size screen.
 * Client only: the place goes into {@code nusmp-ui-client.toml} ({@code ui.greatSpirit.noticePos}) and nothing is sent to the server. The crest is
 * kept fully on screen at every moment ({@link GsCrestPos}).
 */
public final class GsCrestScreen extends Screen {
    private static boolean openRequested;
    private static int testCounter;

    private final Screen parent;
    private int cx, cy;                                   // top-left of the crest in GUI pixels
    private boolean dragging;
    private int grabX, grabY;

    public GsCrestScreen(Screen parent) {
        super(Component.translatableWithFallback("nusmp.ui.crest.title", "Alert crest"));
        this.parent = parent;
    }

    /** Registers the command and the tick that opens the screen (a flag, so the chat screen closing cannot swallow it). Called from {@link GsNotices#init}. */
    static void register() {
        NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent e) ->
                e.getDispatcher().register(Commands.literal("nusmp_gui_crest").executes(c -> {
                    openRequested = true;
                    return Command.SINGLE_SUCCESS;
                })));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            if (!openRequested) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) { openRequested = false; return; }
            if (mc.screen == null || mc.screen.getClass().getSimpleName().equals("ChatScreen")) {
                openRequested = false;
                mc.setScreen(new GsCrestScreen(null));
            }
        });
    }

    @Override
    protected void init() {
        GsCrestPos.Place p = UiThemeOptions.crestPlace();
        int[] xy = GsCrestPos.resolve(p, width, height, GsDraw.CREST, GsDraw.CREST);
        if (!dragging) { cx = xy[0]; cy = xy[1]; }
        else { cx = clampX(cx); cy = clampY(cy); }
        int bw = 74, gap = 4, total = bw * 4 + gap * 3, x0 = (width - total) / 2, y = height - 28;
        addRenderableWidget(new ThemedButton(x0, y, bw, 18, Component.translatableWithFallback("nusmp.ui.crest.save", "Save"), b -> { save(); onClose(); }));
        addRenderableWidget(new ThemedButton(x0 + bw + gap, y, bw, 18, Component.translatableWithFallback("nusmp.ui.crest.reset", "Reset"), b -> {
            int[] d = GsCrestPos.resolve(GsCrestPos.Place.DEFAULT, width, height, GsDraw.CREST, GsDraw.CREST);
            cx = d[0];
            cy = d[1];
        }));
        addRenderableWidget(new ThemedButton(x0 + 2 * (bw + gap), y, bw, 18, Component.translatableWithFallback("nusmp.ui.crest.test", "Test notice"), b ->
                GsNotices.offer("nusmp.ui.notice.test:" + (testCounter++), Component.translatableWithFallback("nusmp.ui.crest.test_text", "This is where Great Sage will tell you what it has recorded.").getString(), GsNoticeQueue.Kind.INFO)));
        addRenderableWidget(new ThemedButton(x0 + 3 * (bw + gap), y, bw, 18, Component.translatableWithFallback("nusmp.ui.crest.cancel", "Cancel"), b -> onClose()));
    }

    private int clampX(int x) { return Math.max(0, Math.min(width - GsDraw.CREST, x)); }

    private int clampY(int y) { return Math.max(0, Math.min(height - GsDraw.CREST, y)); }

    private GsCrestPos.Place place() { return GsCrestPos.fromPosition(cx, cy, width, height, GsDraw.CREST, GsDraw.CREST); }

    private void save() { UiThemeOptions.setCrestPlace(place()); }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        float now = UiTheme.now();
        GsNotices.QUEUE.update(UiTheme.realNow());
        GsSkin skin = GsUser.skin();
        int s = skin.ordinal();
        // the four corner guides and the crest's own box
        int ne = UiMath.withAlpha(GsDraw.NEON[s], 0.55f);
        UiPanel.brackets(g, 4, 4, width - 8, height - 8, 12, ne);
        boolean over = mouseX >= cx && mouseX < cx + GsDraw.CREST && mouseY >= cy && mouseY < cy + GsDraw.CREST;
        g.renderOutline(cx - 2, cy - 2, GsDraw.CREST + 4, GsDraw.CREST + 4, UiMath.withAlpha(GsDraw.NEON2[s], dragging ? 0.95f : over ? 0.8f : 0.4f));
        GsNotices.draw(g, font, width, height, place(), now, 1f, false);
        g.drawCenteredString(font, Component.translatableWithFallback("nusmp.ui.crest.hint", "Drag the crest where you want it. Arrow keys nudge it (Shift = 8). Enter saves, Esc cancels."), width / 2, 12, UiMath.withAlpha(GsDraw.TEXT[s], 1f) | 0xFF000000);
        if (!UiTheme.greatSpirit())
            g.drawCenteredString(font, Component.translatableWithFallback("nusmp.ui.crest.theme_hint", "The crest only shows in game when ui.theme is great_spirit."), width / 2, 24, 0xFFFFB04A);
        GsCrestPos.Place p = place();
        g.drawCenteredString(font, p.corner().name() + "  +" + p.dx() + ", +" + p.dy(), width / 2, height - 42, UiMath.withAlpha(GsDraw.DIM[s], 1f) | 0xFF000000);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderTransparentBackground(g);
        UiPanel.scrim(g, width, height, 0.55f);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button == 0 && mx >= cx && mx < cx + GsDraw.CREST && my >= cy && my < cy + GsDraw.CREST) {
            dragging = true;
            grabX = (int) mx - cx;
            grabY = (int) my - cy;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging && button == 0) {
            cx = clampX((int) mx - grabX);
            cy = clampY((int) my - grabY);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging && button == 0) {
            dragging = false;
            int[] xy = GsCrestPos.resolve(place(), width, height, GsDraw.CREST, GsDraw.CREST);      // settle on the canonical spot of the stored place
            cx = xy[0];
            cy = xy[1];
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        int step = (mods & GLFW.GLFW_MOD_SHIFT) != 0 ? 8 : 1;
        switch (key) {
            case GLFW.GLFW_KEY_LEFT -> { cx = clampX(cx - step); return true; }
            case GLFW.GLFW_KEY_RIGHT -> { cx = clampX(cx + step); return true; }
            case GLFW.GLFW_KEY_UP -> { cy = clampY(cy - step); return true; }
            case GLFW.GLFW_KEY_DOWN -> { cy = clampY(cy + step); return true; }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> { save(); onClose(); return true; }
            default -> { return super.keyPressed(key, scan, mods); }
        }
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }

    @Override
    public boolean isPauseScreen() { return false; }
}
