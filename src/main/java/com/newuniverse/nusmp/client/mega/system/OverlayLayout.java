package com.newuniverse.nusmp.client.mega.system;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

import static com.newuniverse.nusmp.client.mega.system.OverlayStyle.*;

/**
 * Why: this is the whole HOLOGRAPHIC WINDOW of the System as one pure function: a description of what to say (the translated words and the numbers) goes in, and
 * out comes the list of marks to draw ({@link Op}) and the rectangle the window takes. Nothing here knows the game, so the layout is unit tested to the pixel (it
 * never leaves its panel, the bar adds up to its width, the title wraps, every anchor stays on screen, the toasts slide in from the left and end faded), it is cheap
 * (a few dozen marks, no textures, no shaders: only fills, one gradient and the default font, as the brief asks) and the preview script can run the very same
 * code to show the owner the window before the game ever starts. The look follows the Solo Leveling "System" window: a see-through dark-cyan panel, a thin bright
 * cyan border with four corner brackets, faint scanlines that creep down and a soft sweep, a header "[ SYSTEM ]" with a blinking cursor that glitches for a
 * moment every few seconds, the floor, the road and the threat, the quest with a segmented bar (or a clock, or a state word), and the five stats. Toasts slide in
 * from the left, type themselves out and fade. The width of text comes in as a function (the font in the game), so the same arithmetic holds in tests and preview.
 */
public final class OverlayLayout {
    public static final int Q_NONE = 0, Q_ACTIVE = 1, Q_DONE = 2, Q_FAILED = 3;
    /** How a quest shows its progress: a segmented bar with a count, a single state word, or a clock. */
    public static final int MODE_COUNT = 0, MODE_STATE = 1, MODE_CLOCK = 2;

    private final ToIntFunction<String> width;

    public OverlayLayout(ToIntFunction<String> width) { this.width = width; }

    // ================================================================ what goes in
    /**
     * The screen and the player's settings.
     *
     * @param guiW   the screen width in GUI pixels
     * @param guiH   the screen height in GUI pixels
     * @param scale  the window's size setting (0.5 to 2): the window is drawn in its own pixels and scaled as a whole
     * @param anchor 0 top-left, 1 top-right, 2 bottom-left, 3 bottom-right
     * @param alpha  the opacity setting (0.1 to 1)
     * @param time   the game time in ticks plus the partial tick (drives every animation)
     */
    public record View(int guiW, int guiH, float scale, int anchor, float alpha, float time) {
        /** The screen width in the window's own (scaled) pixels. */
        public int w() { return Math.max(1, Math.round(guiW / Math.max(0.1f, scale))); }

        /** The screen height in the window's own (scaled) pixels. */
        public int h() { return Math.max(1, Math.round(guiH / Math.max(0.1f, scale))); }
    }

    /** What the quest block says. {@code state} is one of {@code Q_*}, {@code mode} one of {@code MODE_*}. A null quest means "no quest". */
    public record Quest(int state, int mode, String title, int titleChars, boolean titleCursor, String statusWord, String doneWord, String failedWord, String foreignFloor,
                        int progress, int target, String timeWord, int timeTicks, int timeTotalTicks, boolean timeRunning) {}

    /** The stat row: five labels and five values. */
    public record Stats(String[] labels, int[] values) {}

    /**
     * What the panel says, already translated.
     *
     * @param header       "[ SYSTEM ]"
     * @param floor        the signed floor
     * @param road         0 none, 1 Holy, 2 Abyssal
     * @param boss         a boss floor: no quest, the {@code bossText} is shown
     * @param quest        the quest, or null for none (then {@code noQuestText} is shown)
     * @param stats        the stat row, or null to leave it out
     */
    public record Panel(String header, String floorWord, int floor, int road, String roadWord, String threatWord, int threat, boolean boss, String bossText,
                        String noQuestText, Quest quest, Stats stats) {}

    /** A pop-up line: its tag ("[SYSTEM]"), its message, its kind code (see {@code ToastKind}) and its age in ticks. */
    public record ToastView(String prefix, String message, int kind, float age) {}

    /** The marks of the panel and the rectangle it takes (screen pixels of the window's own space). */
    public record Frame(List<Op> ops, int x, int y, int w, int h) {}

    // ================================================================ the panel
    public Frame panel(Panel p, View v) {
        float crisp = crisp(v.alpha());
        String floorText = label(p.floor());
        int wFloor = width.applyAsInt(p.floorWord() + " " + floorText);
        int wRoad = p.road() == 0 ? 0 : width.applyAsInt(p.roadWord());
        int wThreat = width.applyAsInt(p.threatWord() + " " + p.threat());
        int items = p.road() == 0 ? 2 : 3;
        int w = Math.max(PANEL_MIN_W, wFloor + wRoad + wThreat + (items - 1) * 6 + 2 * PAD_X);
        int cell = statCell(p.stats());
        if (p.stats() != null) w = Math.max(w, Math.min(STAT_CELLS, p.stats().labels().length) * cell + 2 * PAD_X);
        int inner = w - 2 * PAD_X;

        List<Op> fg = new ArrayList<>(80);
        int y = PAD_TOP;

        // the header: the brief's "[ SYSTEM ]", a blinking cursor, a glitch for a moment now and then, and the road's diamond at the right
        boolean glitch = OverlayMath.glitch(v.time());
        if (glitch) {
            text(fg, PAD_X - 1, y, p.header(), GLITCH_A, 0.85f * crisp, false);
            text(fg, PAD_X + 1, y, p.header(), GLITCH_B, 0.85f * crisp, false);
        }
        text(fg, PAD_X, y, p.header(), CYAN_BRIGHT, crisp, !glitch);
        if (OverlayMath.blink(v.time())) fill(fg, PAD_X + width.applyAsInt(p.header()) + 3, y, PAD_X + width.applyAsInt(p.header()) + 8, y + TEXT_H - 1, CYAN, crisp);
        diamond(fg, w - PAD_X - 2, y + 1, roadColour(p.road()), crisp);
        y += TEXT_H + GAP;

        // the floor, the road, the threat
        int gap = Math.max(6, Math.min(14, (inner - wFloor - wRoad - wThreat) / (items - 1)));
        int x = PAD_X;
        text(fg, x, y, p.floorWord(), CYAN_DIM, crisp, true);
        text(fg, x + width.applyAsInt(p.floorWord() + " "), y, floorText, TEXT, crisp, true);
        x += wFloor + gap;
        if (p.road() != 0) {
            text(fg, x, y, p.roadWord(), roadColour(p.road()), crisp, true);
            x += wRoad + gap;
        }
        text(fg, x, y, p.threatWord(), CYAN_DIM, crisp, true);
        text(fg, x + width.applyAsInt(p.threatWord() + " "), y, Integer.toString(p.threat()), threatColour(p.threat()), crisp, true);
        y += TEXT_H + GAP;
        divider(fg, y, w, crisp);
        y += 1 + GAP;

        // the quest block
        if (p.boss()) {
            text(fg, PAD_X, y, p.bossText(), GOLD, crisp, true);
            y += LINE;
        } else if (p.quest() == null) {
            text(fg, PAD_X, y, p.noQuestText(), CYAN_DIM, crisp, true);
            y += LINE;
        } else {
            y = quest(fg, p.quest(), v, w, inner, y, crisp);
        }

        // the stat row
        if (p.stats() != null) {
            y += GAP - 1;
            divider(fg, y, w, crisp);
            y += 1 + GAP;
            y = stats(fg, p.stats(), cell, w, y, crisp);
        }
        int h = y + PAD_BOTTOM;

        // the panel's own body goes under the content: fill, scanlines, sweep; the border and brackets go over it
        List<Op> all = new ArrayList<>(fg.size() + 40);
        all.add(new Op.Gradient(0, 0, w, h, OverlayMath.withAlpha(BG_TOP, v.alpha()), OverlayMath.withAlpha(BG_BOTTOM, v.alpha())));
        scan(all, w, h, v);
        all.addAll(fg);
        border(all, w, h, crisp);

        int x0 = OverlayMath.originX(v.anchor(), v.w(), w, MARGIN);
        int y0 = OverlayMath.originY(v.anchor(), v.h(), h, MARGIN);
        return new Frame(shift(all, x0, y0), x0, y0, w, h);
    }

    /** The quest block; returns the y below it. */
    private int quest(List<Op> out, Quest q, View v, int w, int inner, int y0, float crisp) {
        int y = y0;
        boolean foreign = q.foreignFloor() != null && !q.foreignFloor().isEmpty();
        float dim = foreign ? 0.6f : 1f;
        int titleRgb = q.state() == Q_DONE ? GREEN_TEXT : q.state() == Q_FAILED ? RED_TEXT : TEXT;

        // the title, typed out when the quest is new, with the cursor at the end of what is typed
        List<String> lines = TextWrap.wrap(q.title(), inner, width, MAX_TITLE_LINES);
        int shown = q.titleChars() < 0 ? Integer.MAX_VALUE : q.titleChars();
        int consumed = 0, cursorX = -1, cursorY = y;
        for (String line : lines) {
            int n = Math.max(0, Math.min(line.length(), shown - consumed));
            if (n > 0) text(out, PAD_X, y, line.substring(0, n), titleRgb, crisp * dim, true);
            if (shown != Integer.MAX_VALUE && shown >= consumed && shown <= consumed + line.length()) {
                cursorX = PAD_X + width.applyAsInt(line.substring(0, n)) + 1;
                cursorY = y;
            }
            consumed += line.length() + 1;
            y += LINE;
        }
        if (q.titleCursor() && cursorX >= 0) fill(out, cursorX, cursorY, cursorX + 4, cursorY + TEXT_H - 1, CYAN, crisp);
        y += 1;                                                              // a line pitch leaves 2 pixels under the text; the bar wants 3

        switch (q.mode()) {
            case MODE_COUNT -> {
                bar(out, q, inner, y, crisp * dim);
                String right = q.state() == Q_DONE ? q.doneWord() : q.state() == Q_FAILED ? q.failedWord() : q.progress() + " / " + q.target();
                int rgb = q.state() == Q_DONE ? GREEN : q.state() == Q_FAILED ? RED : TEXT;
                text(out, w - PAD_X - width.applyAsInt(right), y - 1, right, rgb, crisp * dim, true);
                y += BAR_H + 3;
            }
            case MODE_STATE -> {
                String word = q.state() == Q_DONE ? q.doneWord() : q.state() == Q_FAILED ? q.failedWord() : q.statusWord();
                int rgb = q.state() == Q_DONE ? GREEN : q.state() == Q_FAILED ? RED : GREEN_TEXT;
                if (q.state() == Q_ACTIVE && OverlayMath.blink(v.time() * 0.5f)) fill(out, PAD_X, y + 2, PAD_X + 4, y + 6, GREEN, crisp * dim);
                text(out, PAD_X + 8, y, word, rgb, crisp * dim, true);
                y += LINE;
            }
            default -> {
                if (q.state() == Q_ACTIVE) {
                    String time = q.timeWord() + " " + OverlayMath.countdown(q.timeTicks());
                    int rgb = clockColour(q.timeTicks());
                    boolean urgent = q.timeRunning() && q.timeTicks() <= 20 * 20 && !OverlayMath.blink(v.time() * 2f);
                    text(out, PAD_X, y, time, urgent ? AMBER : q.timeRunning() ? rgb : CYAN_DIM, crisp * dim, true);
                    y += TEXT_H + 2;
                    clock(out, q, inner, y, crisp * dim);
                    y += CLOCK_H + 2;
                } else {
                    String word = q.state() == Q_DONE ? q.doneWord() : q.failedWord();
                    text(out, PAD_X, y, word, q.state() == Q_DONE ? GREEN : RED, crisp, true);
                    y += LINE;
                }
            }
        }
        if (foreign) {
            text(out, PAD_X, y, q.foreignFloor(), AMBER, crisp * 0.9f, true);
            y += LINE;
        }
        return y;
    }

    /** The segmented progress bar: the segments add up to the bar's width exactly, the lit ones are cyan (green when done, red when failed). */
    private void bar(List<Op> out, Quest q, int inner, int y, float alpha) {
        int barW = inner - BAR_RESERVE;
        int n = OverlayMath.totalSegments(q.target());
        int lit = q.state() == Q_DONE ? n : OverlayMath.litSegments(q.progress(), q.target());
        int gap = n > 1 ? BAR_GAP : 0;
        int usable = barW - gap * (n - 1);
        int base = usable / n, extra = usable % n;
        int rgb = q.state() == Q_DONE ? GREEN : q.state() == Q_FAILED ? RED : CYAN;
        int x = PAD_X;
        for (int i = 0; i < n; i++) {
            int sw = base + (i < extra ? 1 : 0);
            if (i < lit) {
                fill(out, x, y, x + sw, y + BAR_H, rgb, alpha);
                fill(out, x, y, x + sw, y + 1, 0xFFFFFF, alpha * 0.45f);
            } else {
                fill(out, x, y, x + sw, y + BAR_H, CYAN_FAINT, alpha * 0.55f);
            }
            x += sw + gap;
        }
    }

    /** The thin bar that drains while a timed quest runs. */
    private void clock(List<Op> out, Quest q, int inner, int y, float alpha) {
        fill(out, PAD_X, y, PAD_X + inner, y + CLOCK_H, CYAN_FAINT, alpha * 0.55f);
        int total = Math.max(1, q.timeTotalTicks());
        int w = Math.round(inner * Math.max(0f, Math.min(1f, q.timeTicks() / (float) total)));
        if (w > 0) fill(out, PAD_X, y, PAD_X + w, y + CLOCK_H, clockColour(q.timeTicks()), alpha);
    }

    /** The stat row: five cells, the label over the value; a stat at 0 is dim. Returns the y below it. */
    private int stats(List<Op> out, Stats s, int cell, int w, int y, float crisp) {
        int n = Math.min(STAT_CELLS, Math.min(s.labels().length, s.values().length));
        int total = n * cell;
        int left = (w - total) / 2;
        for (int i = 0; i < n; i++) {
            int cx = left + i * cell + cell / 2;
            String label = s.labels()[i];
            String value = Integer.toString(s.values()[i]);
            text(out, cx - width.applyAsInt(label) / 2, y, label, CYAN_DIM, crisp, true);
            text(out, cx - width.applyAsInt(value) / 2, y + 9, value, s.values()[i] > 0 ? TEXT : CYAN_FAINT, crisp, true);
        }
        return y + 9 + TEXT_H;
    }

    /** The width of one stat cell: the standard one, or wider when a label or a value of this font would touch its neighbour. */
    private int statCell(Stats s) {
        int cell = STAT_CELL_W;
        if (s == null) return cell;
        for (int i = 0; i < Math.min(s.labels().length, s.values().length); i++) {
            cell = Math.max(cell, Math.max(width.applyAsInt(s.labels()[i]), width.applyAsInt(Integer.toString(s.values()[i]))) + 4);
        }
        return cell;
    }

    // ================================================================ the pieces of the frame
    private static void divider(List<Op> out, int y, int w, float crisp) { fill(out, PAD_X, y, w - PAD_X, y + 1, CYAN, 0.30f * crisp); }

    /** A small diamond, 5 pixels across, in five fills. */
    private static void diamond(List<Op> out, int cx, int y, int rgb, float crisp) {
        fill(out, cx, y, cx + 1, y + 1, rgb, crisp);
        fill(out, cx - 1, y + 1, cx + 2, y + 2, rgb, crisp);
        fill(out, cx - 2, y + 2, cx + 3, y + 3, rgb, crisp);
        fill(out, cx - 1, y + 3, cx + 2, y + 4, rgb, crisp);
        fill(out, cx, y + 4, cx + 1, y + 5, rgb, crisp);
    }

    /** Faint scanlines that creep down the panel, and one soft band that sweeps across it. */
    private static void scan(List<Op> out, int w, int h, View v) {
        float a = 0.05f + 0.05f * v.alpha();
        for (int yy = 1 + OverlayMath.scanOffset(v.time(), SCAN_SPACING); yy < h - 1; yy += SCAN_SPACING) fill(out, 1, yy, w - 1, yy + 1, SCAN, a);
        float t = (v.time() % SWEEP_TICKS) / SWEEP_TICKS;
        int top = Math.round(-SWEEP_H + t * (h + SWEEP_H));
        int mid = top + SWEEP_H / 2, bottom = top + SWEEP_H;
        int peak = OverlayMath.withAlpha(CYAN, 0.16f * (0.4f + 0.6f * v.alpha()));
        int none = OverlayMath.withAlpha(CYAN, 0f);
        gradient(out, 1, top, w - 1, mid, 1, h - 1, none, peak);
        gradient(out, 1, mid, w - 1, bottom, 1, h - 1, peak, none);
    }

    /** A gradient clipped to the rows [lo, hi): skipped when nothing is left. */
    private static void gradient(List<Op> out, int x1, int y1, int x2, int y2, int lo, int hi, int top, int bottom) {
        int a = Math.max(y1, lo), b = Math.min(y2, hi);
        if (b > a) out.add(new Op.Gradient(x1, a, x2, b, top, bottom));
    }

    /** The one-pixel border and the four corner brackets. */
    private static void border(List<Op> out, int w, int h, float crisp) {
        int line = OverlayMath.withAlpha(CYAN, crisp);
        out.add(new Op.Fill(0, 0, w, 1, line));
        out.add(new Op.Fill(0, h - 1, w, h, line));
        out.add(new Op.Fill(0, 1, 1, h - 1, line));
        out.add(new Op.Fill(w - 1, 1, w, h - 1, line));
        int b = OverlayMath.withAlpha(CYAN_BRIGHT, 1f);
        int len = BRACKET_LEN, t = BRACKET_THICK;
        out.add(new Op.Fill(-1, -1, len, t - 1, b));
        out.add(new Op.Fill(-1, -1, t - 1, len, b));
        out.add(new Op.Fill(w - len, -1, w + 1, t - 1, b));
        out.add(new Op.Fill(w - t + 1, -1, w + 1, len, b));
        out.add(new Op.Fill(-1, h - t + 1, len, h + 1, b));
        out.add(new Op.Fill(-1, h - len, t - 1, h + 1, b));
        out.add(new Op.Fill(w - len, h - t + 1, w + 1, h + 1, b));
        out.add(new Op.Fill(w - t + 1, h - len, w + 1, h + 1, b));
    }

    // ================================================================ the toasts
    /**
     * The pop-up lines: each slides in from the left edge, types itself out, holds and fades. {@code x} and {@code y} are where the first one rests; the next
     * ones stack below it. At most {@link OverlayStyle#TOAST_MAX} are drawn.
     */
    public List<Op> toasts(List<ToastView> toasts, int x, int y) {
        List<Op> out = new ArrayList<>(24);
        int row = 0;
        for (ToastView t : toasts) {
            if (row >= TOAST_MAX) break;
            float alpha = ToastTimeline.alpha(t.age());
            if (alpha <= 0f) continue;
            String full = t.prefix() + " " + t.message();
            int bw = Math.max(TOAST_MIN_W, width.applyAsInt(full) + 2 * TOAST_PAD + 4);
            int bx = x - Math.round((1f - ToastTimeline.slide(t.age())) * (bw + x));
            int by = y + row * (TOAST_H + TOAST_GAP);
            int accent = toastAccent(t.kind());
            fill(out, bx, by, bx + bw, by + TOAST_H, TOAST_BG, 0.82f * alpha);
            fill(out, bx, by, bx + 2, by + TOAST_H, accent, alpha);
            fill(out, bx + 2, by, bx + bw, by + 1, accent, 0.35f * alpha);
            fill(out, bx + 2, by + TOAST_H - 1, bx + bw, by + TOAST_H, accent, 0.35f * alpha);
            int n = ToastTimeline.visibleChars(t.age(), full.length());
            int prefixLen = t.prefix().length() + 1;
            int tx = bx + TOAST_PAD, ty = by + (TOAST_H - TEXT_H) / 2;
            if (n > 0) text(out, tx, ty, t.prefix().substring(0, Math.min(n, t.prefix().length())), CYAN_BRIGHT, alpha, true);
            if (n > prefixLen) text(out, tx + width.applyAsInt(t.prefix() + " "), ty, t.message().substring(0, Math.min(n - prefixLen, t.message().length())), toastText(t.kind()), alpha, true);
            if (ToastTimeline.cursor(t.age(), full.length())) {
                int typed = n <= prefixLen ? width.applyAsInt(full.substring(0, Math.min(n, t.prefix().length()))) : width.applyAsInt(t.prefix() + " ") + width.applyAsInt(t.message().substring(0, Math.min(n - prefixLen, t.message().length())));
                fill(out, tx + typed + 2, ty, tx + typed + 6, ty + TEXT_H - 1, accent, alpha);
            }
            row++;
        }
        return out;
    }

    // ================================================================ helpers
    /** "+12", "-3", "0". */
    static String label(int floor) { return floor > 0 ? "+" + floor : Integer.toString(floor); }

    /** A rectangle at the given opacity; nothing at all when it would be invisible or empty. */
    private static void fill(List<Op> out, int x1, int y1, int x2, int y2, int rgb, float alpha) {
        int argb = OverlayMath.withAlpha(rgb, alpha);
        if (x2 > x1 && y2 > y1 && (argb >>> 24) > 0) out.add(new Op.Fill(x1, y1, x2, y2, argb));
    }

    /** A line of text at the given opacity; nothing when the game would draw it fully opaque by mistake (alpha under 8) or when it is empty. */
    private static void text(List<Op> out, int x, int y, String s, int rgb, float alpha, boolean shadow) {
        int argb = OverlayMath.withAlpha(rgb, alpha);
        if (s != null && !s.isEmpty() && OverlayMath.drawable(argb)) out.add(new Op.Text(x, y, s, argb, shadow));
    }

    private static List<Op> shift(List<Op> ops, int dx, int dy) {
        List<Op> out = new ArrayList<>(ops.size());
        for (Op op : ops) {
            if (op instanceof Op.Fill f) out.add(new Op.Fill(f.x1() + dx, f.y1() + dy, f.x2() + dx, f.y2() + dy, f.argb()));
            else if (op instanceof Op.Gradient g) out.add(new Op.Gradient(g.x1() + dx, g.y1() + dy, g.x2() + dx, g.y2() + dy, g.top(), g.bottom()));
            else if (op instanceof Op.Text t) out.add(new Op.Text(t.x() + dx, t.y() + dy, t.text(), t.argb(), t.shadow()));
        }
        return out;
    }
}
