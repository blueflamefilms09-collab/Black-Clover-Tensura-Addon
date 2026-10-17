package com.newuniverse.nusmp.client.avatar;

import com.newuniverse.nusmp.avatar.menu.ForgeLayout.Rect;
import com.newuniverse.nusmp.client.ui.theme.UiBar;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Why: the Appearance page (and any other page that is a plain list of settings) is made of the same few controls: a heading, a "< value >" stepper, a row of
 * colour swatches, a slider, a button. They are drawn by hand (so the module can slide and the look matches the rest of the screen) and need a place to stand
 * when the window is too short for all of them: a {@link ControlList} stacks them top to bottom, scrolls them with the wheel and clips them to the module.
 * A control only knows its own box and how to draw and answer a click; the list does the arithmetic and the routing. Client only.
 */
final class ForgeControls {
    private ForgeControls() {}

    interface Control {
        /** Puts the control at ({@code x}, {@code y}) with width {@code w}; returns the height it takes. */
        int place(int x, int y, int w);

        Rect rect();

        /** {@code mx}, {@code my} are in the control's own (content) space, i.e. already corrected for the scroll. */
        void draw(ForgeHost h, GuiGraphics g, int mx, int my);

        default boolean click(ForgeHost h, double mx, double my, int button) { return false; }

        default boolean drag(double mx, double my) { return false; }

        default void release() {}
    }

    // ================================================================ the list
    static final class ControlList {
        private final List<Control> items = new ArrayList<>();
        private Rect area = Rect.EMPTY;
        private int contentH, scroll;
        private Control grabbed;

        void clear() { items.clear(); contentH = 0; scroll = 0; grabbed = null; }

        void add(Control c) { items.add(c); }

        /** Stacks the controls top to bottom inside {@code a}, {@code gap} pixels apart; keeps the scroll position inside the new range. */
        void layout(Rect a, int gap) {
            area = a;
            int reserve = 0;
            int y = a.y();
            for (Control c : items) y += c.place(a.x(), y, a.w() - reserve) + gap;
            contentH = Math.max(0, y - gap - a.y());
            if (contentH > a.h()) {                                         // it overflows: make room for the scroll bar and place again
                reserve = 5;
                y = a.y();
                for (Control c : items) y += c.place(a.x(), y, a.w() - reserve) + gap;
                contentH = Math.max(0, y - gap - a.y());
            }
            scroll = clampScroll(scroll);
        }

        int maxScroll() { return Math.max(0, contentH - area.h()); }

        private int clampScroll(int s) { return Math.max(0, Math.min(maxScroll(), s)); }

        boolean overflows() { return maxScroll() > 0; }

        void render(ForgeHost h, GuiGraphics g, int mx, int my) {
            if (area.isEmpty()) return;
            h.scissor(g, area);
            g.pose().pushPose();
            g.pose().translate(0f, -scroll, 0f);
            boolean inside = area.contains(mx, my);
            int cy = my + scroll;
            for (Control c : items) {
                Rect r = c.rect();
                if (r.bottom() < area.y() + scroll || r.y() > area.bottom() + scroll) continue;           // wholly out of view
                c.draw(h, g, inside || grabbed == c ? mx : -9999, inside || grabbed == c ? cy : -9999);
            }
            g.pose().popPose();
            h.endScissor(g);
            if (maxScroll() > 0) {
                ForgeUi.scrollbar(g, new Rect(area.right() - 3, area.y(), 3, area.h()), scroll, maxScroll(), area.h(), contentH, 0x5CD8FF);
            }
        }

        boolean click(ForgeHost h, double mx, double my, int button) {
            if (!area.contains(mx, my)) return false;
            double cy = my + scroll;
            for (Control c : items) {
                if (c.click(h, mx, cy, button)) { grabbed = c; return true; }
            }
            return false;
        }

        boolean drag(double mx, double my) { return grabbed != null && grabbed.drag(mx, my + scroll); }

        void release() {
            if (grabbed != null) grabbed.release();
            grabbed = null;
        }

        boolean scroll(double mx, double my, double dy) {
            if (!area.contains(mx, my) || maxScroll() <= 0) return false;
            scroll = clampScroll(scroll - (int) Math.signum(dy) * 14);
            return true;
        }
    }

    // ================================================================ controls
    /** A small coloured heading with a rule under it. */
    static final class Heading implements Control {
        private final Supplier<String> text;
        private final int rgb;
        private Rect r = Rect.EMPTY;

        Heading(Supplier<String> text, int rgb) { this.text = text; this.rgb = rgb; }

        @Override public int place(int x, int y, int w) { r = new Rect(x, y, w, 12); return 12; }

        @Override public Rect rect() { return r; }

        @Override
        public void draw(ForgeHost h, GuiGraphics g, int mx, int my) {
            Font f = h.font();
            ForgeUi.text(g, f, ForgeUi.fit(f, text.get(), r.w()), r.x(), r.y(), rgb);
            g.fill(r.x(), r.y() + f.lineHeight + 1, r.right(), r.y() + f.lineHeight + 2, ForgeUi.argb(rgb, 0.25f));
            g.fill(r.x(), r.y() + f.lineHeight + 1, r.x() + Math.min(r.w(), 24), r.y() + f.lineHeight + 2, ForgeUi.argb(rgb, 0.9f));
        }
    }

    /** Empty space. */
    static final class Spacer implements Control {
        private final int height;
        private Rect r = Rect.EMPTY;

        Spacer(int height) { this.height = height; }

        @Override public int place(int x, int y, int w) { r = new Rect(x, y, w, height); return height; }

        @Override public Rect rect() { return r; }

        @Override public void draw(ForgeHost h, GuiGraphics g, int mx, int my) {}
    }

    /** "Label   <  value  >": clicking an arrow steps the value by -1 or +1 (the value box too steps forward). */
    static final class Cycler implements Control {
        private final Supplier<String> label, value;
        private final IntConsumer step;
        private Rect r = Rect.EMPTY, prev = Rect.EMPTY, next = Rect.EMPTY, val = Rect.EMPTY;

        Cycler(Supplier<String> label, Supplier<String> value, IntConsumer step) { this.label = label; this.value = value; this.step = step; }

        @Override
        public int place(int x, int y, int w) {
            r = new Rect(x, y, w, 15);
            int labelW = Math.max(30, Math.min(72, Math.round(w * 0.32f)));
            int rx = x + labelW;
            prev = new Rect(rx, y, 14, 15);
            next = new Rect(x + w - 14, y, 14, 15);
            val = new Rect(prev.right() + 1, y, Math.max(0, next.x() - prev.right() - 2), 15);
            return 15;
        }

        @Override public Rect rect() { return r; }

        @Override
        public void draw(ForgeHost h, GuiGraphics g, int mx, int my) {
            Font f = h.font();
            ForgeUi.text(g, f, ForgeUi.fit(f, label.get(), prev.x() - r.x() - 2), r.x(), r.y() + 4, UiTheme.TEXT_DIM);
            ForgeUi.button(g, f, prev, "<", true, h.hover(prev, prev.contains(mx, my)), 0x5CD8FF);
            ForgeUi.button(g, f, next, ">", true, h.hover(next, next.contains(mx, my)), 0x5CD8FF);
            g.fill(val.x(), val.y(), val.right(), val.bottom(), ForgeUi.argb(0x03050A, 0.7f));
            ForgeUi.centered(g, f, ForgeUi.fit(f, value.get(), val.w() - 2), val.cx(), r.y() + 4, UiTheme.TEXT);
        }

        @Override
        public boolean click(ForgeHost h, double mx, double my, int button) {
            if (button != 0) return false;
            if (prev.contains(mx, my)) { h.click(); step.accept(-1); return true; }
            if (next.contains(mx, my) || val.contains(mx, my)) { h.click(); step.accept(1); return true; }
            return false;
        }
    }

    /** A wrapped grid of colour swatches; the current one is outlined. */
    static final class Swatches implements Control {
        private final int[] palette;
        private final IntSupplier current;
        private final IntConsumer pick;
        private final int size;
        private Rect r = Rect.EMPTY;
        private int cols = 1;

        Swatches(int[] palette, IntSupplier current, IntConsumer pick) { this(palette, current, pick, 11); }

        Swatches(int[] palette, IntSupplier current, IntConsumer pick, int size) { this.palette = palette; this.current = current; this.pick = pick; this.size = size; }

        @Override
        public int place(int x, int y, int w) {
            cols = Math.max(1, (w + 2) / (size + 2));
            int rows = (palette.length + cols - 1) / cols;
            int hgt = rows * (size + 2) - 2;
            r = new Rect(x, y, w, Math.max(size, hgt));
            return r.h();
        }

        @Override public Rect rect() { return r; }

        private Rect cell(int i) { return new Rect(r.x() + (i % cols) * (size + 2), r.y() + (i / cols) * (size + 2), size, size); }

        @Override
        public void draw(ForgeHost h, GuiGraphics g, int mx, int my) {
            int cur = current.getAsInt() & 0xFFFFFF;
            for (int i = 0; i < palette.length; i++) {
                Rect c = cell(i);
                boolean over = c.contains(mx, my);
                float hv = h.hover(palette[i] * 31 + i * 7 + System.identityHashCode(this), over);
                g.fill(c.x(), c.y(), c.right(), c.bottom(), 0xFF000000 | palette[i]);
                boolean on = (palette[i] & 0xFFFFFF) == cur;
                g.renderOutline(c.x() - 1, c.y() - 1, c.w() + 2, c.h() + 2, on ? 0xFFFFFFFF : ForgeUi.argb(0xFFFFFF, 0.15f + 0.6f * hv));
            }
        }

        @Override
        public boolean click(ForgeHost h, double mx, double my, int button) {
            if (button != 0) return false;
            for (int i = 0; i < palette.length; i++) if (cell(i).contains(mx, my)) { h.click(); pick.accept(palette[i]); return true; }
            return false;
        }
    }

    /** A 0..1 slider with a label that follows the value. */
    static final class Slider implements Control {
        private final DoubleFunction<String> label;
        private final DoubleConsumer onChange;
        private final java.util.function.DoubleSupplier source;
        private final int rgb;
        private Rect r = Rect.EMPTY;
        private boolean dragging;
        private double value;

        /** {@code source} gives the current value (the look may change it from elsewhere); the slider follows it except while it is being dragged. */
        Slider(int rgb, java.util.function.DoubleSupplier source, DoubleFunction<String> label, DoubleConsumer onChange) {
            this.rgb = rgb; this.source = source; this.label = label; this.onChange = onChange;
        }

        @Override public int place(int x, int y, int w) { r = new Rect(x, y, w, 13); return 13; }

        @Override public Rect rect() { return r; }

        @Override
        public void draw(ForgeHost h, GuiGraphics g, int mx, int my) {
            if (!dragging) value = Math.max(0.0, Math.min(1.0, source.getAsDouble()));
            boolean over = r.contains(mx, my) || dragging;
            float hv = h.hover(this, over);
            g.fill(r.x(), r.y(), r.right(), r.bottom(), ForgeUi.argb(0x060912, 0.9f));
            g.renderOutline(r.x(), r.y(), r.w(), r.h(), ForgeUi.argb(0x5CD8FF, 0.35f));
            UiBar.draw(g, r.x() + 1, r.y() + 1, r.w() - 2, r.h() - 2, (float) value, rgb, h.now(), 0.7f + 0.3f * hv);
            Font f = h.font();
            ForgeUi.centered(g, f, ForgeUi.fit(f, label.apply(value), r.w() - 4), r.cx(), r.y() + (r.h() - f.lineHeight) / 2 + 1, UiTheme.TEXT);
        }

        private void apply(double mx) {
            value = Math.max(0.0, Math.min(1.0, (mx - r.x() - 2) / Math.max(1, r.w() - 4)));
            onChange.accept(value);
        }

        @Override
        public boolean click(ForgeHost h, double mx, double my, int button) {
            if (button != 0 || !r.contains(mx, my)) return false;
            dragging = true;
            apply(mx);
            return true;
        }

        @Override
        public boolean drag(double mx, double my) {
            if (!dragging) return false;
            apply(mx);
            return true;
        }

        @Override public void release() { dragging = false; }
    }

    /** A plain button. */
    static final class Button implements Control {
        private final Supplier<String> label;
        private final Runnable action;
        private final int rgb;
        private Rect r = Rect.EMPTY;

        Button(Supplier<String> label, int rgb, Runnable action) { this.label = label; this.rgb = rgb; this.action = action; }

        @Override public int place(int x, int y, int w) { r = new Rect(x, y, w, 16); return 16; }

        @Override public Rect rect() { return r; }

        @Override
        public void draw(ForgeHost h, GuiGraphics g, int mx, int my) {
            ForgeUi.button(g, h.font(), r, label.get(), true, h.hover(this, r.contains(mx, my)), rgb);
        }

        @Override
        public boolean click(ForgeHost h, double mx, double my, int button) {
            if (button != 0 || !r.contains(mx, my)) return false;
            h.click();
            action.run();
            return true;
        }
    }

    /** A static line of text (or several: it wraps) with a fixed number of lines reserved. */
    static final class Note implements Control {
        private final Supplier<Component> text;
        private final int rgb, lines;
        private Rect r = Rect.EMPTY;

        Note(Supplier<Component> text, int rgb, int lines) { this.text = text; this.rgb = rgb; this.lines = Math.max(1, lines); }

        @Override public int place(int x, int y, int w) { r = new Rect(x, y, w, lines * 10); return r.h(); }

        @Override public Rect rect() { return r; }

        @Override
        public void draw(ForgeHost h, GuiGraphics g, int mx, int my) {
            ForgeUi.wrapped(g, h.font(), text.get().getString(), r.x(), r.y(), r.w(), rgb, lines);
        }
    }
}
