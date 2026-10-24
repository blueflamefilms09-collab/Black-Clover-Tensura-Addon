package com.newuniverse.nusmp.client.ui;

import com.newuniverse.nusmp.client.ui.scale.GuiScaleMath;
import com.newuniverse.nusmp.client.ui.scale.GuiScaleOptions;
import com.newuniverse.nusmp.client.ui.scale.GuiScaleRegistry;
import com.newuniverse.nusmp.client.ui.theme.ThemedButton;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;

/**
 * Why: the "GUI Size" screen of the owner's request: a slider for the global size, a list with one slider per GUI (Auto = follow the global one), the
 * fit-to-window toggle, and a live preview of what the chosen size does to a window. Opened from the Multiverse status screen, with the GUI Size key (F8 by
 * default) and from the mod's entry in the mod list (it is also the mod's config screen; "More options" leads to the plain config editor).
 * Values are written to the config as the sliders move (the HUD reacts at once) and saved when the screen closes. This screen is never scaled itself.
 */
public class GuiSizeScreen extends Screen {
    private static final int ROW_H = 22, LABEL_W = 118, SLIDER_W = 104, AUTO_W = 34, PREVIEW_W = 132;

    private final Screen parent;
    private final ModContainer container;
    private final List<AbstractWidget> rowWidgets = new ArrayList<>();
    /** One line of the list: a header (entry == null) or a GUI. */
    private record Row(String header, GuiScaleRegistry.Entry entry) {}
    private final List<Row> rows = new ArrayList<>();
    private int scroll;
    private String selected;                                  // the id the preview shows; null = the global size
    private int x0, listTop, listBottom, listW;
    private boolean preview;

    public GuiSizeScreen(Screen parent, ModContainer container) {
        super(tr("title", "GUI Size"));
        this.parent = parent;
        this.container = container;
        rows.add(new Row(tr("screens", "Screens").getString(), null));
        for (GuiScaleRegistry.Entry e : GuiScaleRegistry.of(GuiScaleRegistry.Kind.SCREEN)) rows.add(new Row(null, e));
        rows.add(new Row(tr("hud", "HUD").getString(), null));
        for (GuiScaleRegistry.Entry e : GuiScaleRegistry.of(GuiScaleRegistry.Kind.HUD)) rows.add(new Row(null, e));
    }

    static Component tr(String key, String english) {
        return Component.translatableWithFallback("nusmp.gui_scale." + key, english);
    }

    static Component nameOf(GuiScaleRegistry.Entry e) {
        return Component.translatableWithFallback("nusmp.gui_scale.gui." + e.id(), e.english());
    }

    @Override
    protected void init() {
        int w = Math.min(width - 16, 430);
        x0 = (width - w) / 2;
        preview = w >= 420;
        listW = preview ? w - PREVIEW_W - 8 : w;
        listTop = 76;
        listBottom = height - 32;

        addRenderableWidget(new ScaleSlider(x0, 22, Math.min(listW, 240), 18, GuiScaleOptions.global(), v -> {
            GuiScaleOptions.setGlobal(v);
            selected = null;
        }, () -> tr("global", "Global size").getString()));
        addRenderableWidget(new ThemedButton(x0, 44, 150, 18, fitLabel(), b -> {
            GuiScaleOptions.setFit(!GuiScaleOptions.fit());
            b.setMessage(fitLabel());
        }));
        addRenderableWidget(new ThemedButton(x0 + 154, 44, Math.max(110, listW - 154), 18, hudLabel(), b -> {
            GuiScaleOptions.setHudFollowsGlobal(!GuiScaleOptions.hudFollowsGlobal());
            b.setMessage(hudLabel());
        }));

        // GUI Move: the sibling of this screen. Sizes are set here, places are set in edit mode (also on the F9 key and /nusmp gui move)
        addRenderableWidget(new ThemedButton(x0 + listW - 100, 5, 100, 15, com.newuniverse.nusmp.client.ui.move.GuiMoveScreen.tr("gui_move.open", "Move GUIs"),
                b -> minecraft.setScreen(new com.newuniverse.nusmp.client.ui.move.GuiMoveScreen(this))));
        // 2.x: the alert crest of the great_spirit look has its own place and move screen (also /nusmp_gui_crest)
        addRenderableWidget(new ThemedButton(x0 + listW - 204, 5, 100, 15, Component.translatableWithFallback("nusmp.ui.crest.open", "Alert crest"),
                b -> minecraft.setScreen(new com.newuniverse.nusmp.client.ui.gs.GsCrestScreen(this))));
        int bw = Math.min(100, (listW - 8) / 3);
        addRenderableWidget(new ThemedButton(x0, height - 26, bw, 18, tr("done", "Done"), b -> onClose()));
        addRenderableWidget(new ThemedButton(x0 + bw + 4, height - 26, bw, 18, tr("reset", "Reset all"), b -> {
            GuiScaleOptions.setGlobal(1.0);
            for (GuiScaleRegistry.Entry e : GuiScaleRegistry.all()) GuiScaleOptions.setOverride(e.id(), 0.0);
            selected = null;
            rebuildWidgets();
        }));
        if (container != null) {
            addRenderableWidget(new ThemedButton(x0 + 2 * (bw + 4), height - 26, bw, 18, tr("more", "More options"),
                    b -> minecraft.setScreen(new ConfigurationScreen(container, this))));
        }
        rowWidgets.clear();
        buildRows();
    }

    private Component fitLabel() {
        return Component.literal(tr("fit", "Fit to window").getString() + ": " + (GuiScaleOptions.fit() ? "ON" : "OFF"));
    }

    private Component hudLabel() {
        return Component.literal(tr("hud_follow", "HUD follows global").getString() + ": " + (GuiScaleOptions.hudFollowsGlobal() ? "ON" : "OFF"));
    }

    private int visibleRows() { return Math.max(1, (listBottom - listTop) / ROW_H); }

    private void buildRows() {
        for (AbstractWidget w : rowWidgets) removeWidget(w);
        rowWidgets.clear();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - visibleRows())));
        for (int i = 0; i < visibleRows() && scroll + i < rows.size(); i++) {
            Row r = rows.get(scroll + i);
            if (r.entry() == null) continue;
            GuiScaleRegistry.Entry e = r.entry();
            int y = listTop + i * ROW_H;
            double own = GuiScaleOptions.override(e.id());
            double shown = GuiScaleMath.hasOverride(own) ? own : GuiScale.requested(e);
            ThemedButton auto = new ThemedButton(x0 + LABEL_W + SLIDER_W + 4, y, AUTO_W, 18, tr("auto", "Auto"), b -> {
                GuiScaleOptions.setOverride(e.id(), 0.0);
                selected = e.id();
                buildRows();
            });
            auto.active = GuiScaleMath.hasOverride(own);
            ScaleSlider s = new ScaleSlider(x0 + LABEL_W, y, SLIDER_W, 18, shown, v -> {
                GuiScaleOptions.setOverride(e.id(), v);
                selected = e.id();
                auto.active = true;
            }, () -> GuiScaleMath.hasOverride(GuiScaleOptions.override(e.id())) ? "" : tr("auto", "Auto").getString());
            rowWidgets.add(s);
            rowWidgets.add(auto);
            addRenderableWidget(s);
            addRenderableWidget(auto);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (my >= listTop && my <= listBottom && mx >= x0 && mx <= x0 + listW) {
            int before = scroll;
            scroll += dy > 0 ? -1 : 1;
            scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - visibleRows())));
            if (scroll != before) buildRows();
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, 0xB0000000);                           // a plain dim (not the blurring vanilla background: ScreenBlurGuardTest)
        UiPanel.panel(g, x0 - 6, 4, listW + 12 + (preview ? PREVIEW_W + 8 : 0), height - 8, UiTheme.CYAN, 0.35f, 0.9f);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        g.drawString(font, title, x0, 9, 0xFFE8C468, true);
        for (int i = 0; i < visibleRows() && scroll + i < rows.size(); i++) {
            Row r = rows.get(scroll + i);
            int y = listTop + i * ROW_H;
            if (r.entry() == null) g.drawString(font, r.header(), x0, y + 5, 0xFF8FD8FF, false);
            else g.drawString(font, font.plainSubstrByWidth(nameOf(r.entry()).getString(), LABEL_W - 4), x0, y + 5, r.entry().id().equals(selected) ? 0xFFFFFFFF : 0xFFC8C8D8, false);
        }
        if (rows.size() > visibleRows()) {
            int trackH = listBottom - listTop, barH = Math.max(12, trackH * visibleRows() / rows.size());
            int barY = listTop + (trackH - barH) * scroll / Math.max(1, rows.size() - visibleRows());
            g.fill(x0 + listW - 3, barY, x0 + listW, barY + barH, 0x90FFFFFF);
        }
        if (preview) drawPreview(g, x0 + listW + 8, 22);
    }

    /** A small mock of the window: the box a GUI occupies, at the size chosen (grey outline = the unscaled box, bright = what is drawn). */
    private void drawPreview(GuiGraphics g, int px, int py) {
        GuiScaleRegistry.Entry sel = selected == null ? null : GuiScaleRegistry.byId(selected);
        boolean hud = sel != null && sel.kind() == GuiScaleRegistry.Kind.HUD;
        double asked = sel == null ? GuiScaleOptions.global() : GuiScale.requested(sel);
        double ratio = (double) PREVIEW_W / Math.max(1, width);
        int ph = (int) Math.round(height * ratio);
        int sw, sh;
        if (hud) { sw = 100; sh = 34; }
        else if (sel == null) { sw = 220; sh = 140; }
        else if (sel.designW() > 0) { sw = sel.designW(); sh = sel.designH(); }
        else if (sel.designW() < 0) { sw = 176; sh = 166; }
        else { sw = width; sh = height; }
        double shown = hud ? asked : GuiScaleMath.effective(asked, GuiScaleOptions.fit(), sel != null && sel.designW() == 0 ? 0 : sw, sel != null && sel.designW() == 0 ? 0 : sh, width, height);
        g.fill(px, py, px + PREVIEW_W, py + ph, 0xFF0B0B12);
        UiPanel.brackets(g, px, py, PREVIEW_W, ph, 5, 0xFF5A6A8A);
        GuiScaleMath.Anchor a = hud ? sel.anchor() : GuiScaleMath.Anchor.CENTER;
        double pvx = a.pivotX(width), pvy = a.pivotY(height);
        double bx = a.pivotX(width - sw), by = a.pivotY(height - sh);                 // the unscaled box, placed against its anchor
        drawBox(g, px, py, ratio, pvx, pvy, bx, by, sw, sh, 1.0, 0x80A0A0B0, false);
        drawBox(g, px, py, ratio, pvx, pvy, bx, by, sw, sh, shown, 0xFF6FE0FF, true);
        int ty = py + ph + 6;
        g.drawString(font, sel == null ? tr("global", "Global size").getString() : nameOf(sel).getString(), px, ty, 0xFFFFFFFF, false);
        g.drawString(font, tr("asked", "Asked").getString() + " " + GuiScaleMath.percent(asked), px, ty + 11, 0xFFC8C8D8, false);
        boolean limited = Math.abs(shown - GuiScaleMath.clamp(asked)) > 1e-6;
        g.drawString(font, tr("shown", "Shown").getString() + " " + GuiScaleMath.percent(shown), px, ty + 22, limited ? 0xFFE8C468 : 0xFFC8C8D8, false);
        if (limited) g.drawString(font, tr("limited", "limited by the window").getString(), px, ty + 33, 0xFFE8C468, false);
    }

    private static void drawBox(GuiGraphics g, int px, int py, double ratio, double pvx, double pvy, double bx, double by, int sw, int sh,
                                double s, int argb, boolean fill) {
        int x1 = px + (int) Math.round(GuiScaleMath.toScreen(bx, pvx, s) * ratio), y1 = py + (int) Math.round(GuiScaleMath.toScreen(by, pvy, s) * ratio);
        int x2 = px + (int) Math.round(GuiScaleMath.toScreen(bx + sw, pvx, s) * ratio), y2 = py + (int) Math.round(GuiScaleMath.toScreen(by + sh, pvy, s) * ratio);
        if (fill) g.fill(x1, y1, x2, y2, (argb & 0x00FFFFFF) | 0x40000000);
        g.fill(x1, y1, x2, y1 + 1, argb);
        g.fill(x1, y2 - 1, x2, y2, argb);
        g.fill(x1, y1, x1 + 1, y2, argb);
        g.fill(x2 - 1, y1, x2, y2, argb);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        GuiScaleOptions.save();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    /** A slider from MIN to MAX in steps of the GUI scale step; reports every change. */
    private static final class ScaleSlider extends AbstractSliderButton {
        private final DoubleConsumer onChange;
        private final java.util.function.Supplier<String> prefix;

        ScaleSlider(int x, int y, int w, int h, double scale, DoubleConsumer onChange, java.util.function.Supplier<String> prefix) {
            super(x, y, w, h, Component.empty(), (GuiScaleMath.clamp(scale) - GuiScaleMath.MIN) / (GuiScaleMath.MAX - GuiScaleMath.MIN));
            this.onChange = onChange;
            this.prefix = prefix;
            updateMessage();
        }

        private double scale() { return GuiScaleMath.snap(GuiScaleMath.MIN + value * (GuiScaleMath.MAX - GuiScaleMath.MIN)); }

        @Override
        protected void updateMessage() {
            String p = prefix.get();
            setMessage(Component.literal((p.isEmpty() ? "" : p + " ") + GuiScaleMath.percent(scale())));
        }

        @Override
        protected void applyValue() {
            onChange.accept(scale());
        }
    }
}
