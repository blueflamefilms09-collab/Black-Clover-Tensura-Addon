package com.newuniverse.nusmp.client.mega.system;

import com.newuniverse.nusmp.labyrinth.mega.system.FloorCardLines;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Why: the System's floor report is a second window in the same holographic style as the status window (a translucent dark-cyan panel, a one-pixel bright cyan border, four corner
 * brackets, the header "[ SYSTEM ]"), and like the status window it is laid out by a PURE class that produces a list of marks ({@link Op}), so the game class only replays the list.
 * That keeps three promises testable without a game: nothing leaves the screen at any size (the panel is clamped to the screen and the lines that do not fit are dropped, footer
 * first, with a "..." marker), every line is wrapped to the panel's width (a datapack mechanic can have a description of any length), and the fade multiplies every colour by one
 * factor. The words come in already composed and translated ({@link FloorCardLines}), tagged with what each line is, so this class decides only colour, indent, wrap and spacing.
 * The window is centred under the top edge, away from the status window's usual corners. Pure.
 */
public final class FloorCardLayout {
    /** The panel is this wide at most, in GUI pixels (it narrows on a small screen). */
    public static final int MAX_W = 276;
    private static final int PAD = 8, MARGIN = 14;

    private final ToIntFunction<String> width;

    public FloorCardLayout(ToIntFunction<String> width) { this.width = width; }

    /** The laid-out window: the marks (absolute screen coordinates) and the rectangle they fill. */
    public record Frame(List<Op> ops, int x, int y, int w, int h) {}

    /** How many wrapped lines a kind of line may take at most. */
    static int maxLines(FloorCardLines.Kind k) {
        return switch (k) {
            case DESC, BODY -> 3;
            case TITLE, SUB, MORE -> 1;
            default -> 2;
        };
    }

    static int indent(FloorCardLines.Kind k) {
        return switch (k) {
            case MECH, MORE -> 4;
            case STATE, BODY, COUNTER -> 11;
            default -> 0;
        };
    }

    /** The colour (RGB) of a kind of line; {@code road}: 1 for the Holy road, 2 for the Abyssal one. */
    static int colour(FloorCardLines.Kind k, int road) {
        return switch (k) {
            case TITLE -> OverlayStyle.roadColour(road);
            case SUB, NOTE, STATE, MORE -> OverlayStyle.CYAN_DIM;
            case BOSS -> OverlayStyle.RED_TEXT;
            case THEME -> OverlayStyle.CYAN_BRIGHT;
            case ALIGN -> OverlayStyle.AMBER;
            case DESC, BODY -> OverlayStyle.TEXT;
            case HEAD -> OverlayStyle.CYAN;
            case MECH -> OverlayStyle.GOLD;
            case COUNTER -> OverlayStyle.GREEN_TEXT;
            case DEPTH -> 0xD2B8FF;
            case FOOT -> OverlayStyle.CYAN_FAINT;
        };
    }

    /** Space left above a line of this kind (a new block of the report starts a little lower). */
    static int gapBefore(FloorCardLines.Kind k) {
        return switch (k) {
            case THEME, HEAD, DEPTH, FOOT -> OverlayStyle.GAP;
            case MECH -> 3;
            default -> 0;
        };
    }

    /**
     * Lays the report out.
     *
     * @param header the window's header text ("[ SYSTEM ]")
     * @param road   1 Holy, 2 Abyssal, 0 none (the title's colour)
     * @param fade   0..1, the whole window's opacity factor (the fade in and out)
     * @param alpha  the player's panel opacity setting
     */
    public Frame layout(List<FloorCardLines.Line> lines, String header, int road, int guiW, int guiH, float alpha, float fade, float time) {
        int w = Math.max(120, Math.min(MAX_W, guiW - 2 * MARGIN));
        int textW = w - 2 * PAD;
        int maxH = Math.max(40, guiH - 2 * MARGIN);
        List<Row> rows = new ArrayList<>();
        int h = PAD + OverlayStyle.LINE + 3;                                             // the header row
        boolean cut = false;
        for (FloorCardLines.Line l : lines) {
            int ind = indent(l.kind());
            List<String> wrapped = TextWrap.wrap(l.text(), Math.max(20, textW - ind), width, maxLines(l.kind()));
            if (wrapped.isEmpty()) continue;
            int need = gapBefore(l.kind()) + wrapped.size() * OverlayStyle.LINE;
            if (h + need + PAD > maxH) {
                cut = true;
                break;
            }
            int top = h + gapBefore(l.kind());
            for (int i = 0; i < wrapped.size(); i++) rows.add(new Row(top + i * OverlayStyle.LINE, ind, wrapped.get(i), colour(l.kind(), road)));
            h += need;
        }
        if (cut && h + OverlayStyle.LINE + PAD <= maxH) {
            rows.add(new Row(h, 0, "...", OverlayStyle.CYAN_DIM));
            h += OverlayStyle.LINE;
        }
        h += PAD;
        int x = Math.max(0, (guiW - w) / 2), y = Math.min(MARGIN + 6, Math.max(0, guiH - h));
        List<Op> out = new ArrayList<>(rows.size() * 2 + 24);
        float f = Math.max(0f, Math.min(1f, fade));
        float a = Math.max(0.1f, Math.min(1f, alpha));
        float crisp = OverlayStyle.crisp(a) * f;
        out.add(new Op.Gradient(x, y, x + w, y + h, OverlayMath.withAlpha(OverlayStyle.BG_TOP, a * f), OverlayMath.withAlpha(OverlayStyle.BG_BOTTOM, a * f)));
        float scan = (0.05f + 0.05f * a) * f;
        for (int yy = y + 1 + OverlayMath.scanOffset(time, OverlayStyle.SCAN_SPACING); yy < y + h - 1; yy += OverlayStyle.SCAN_SPACING) {
            out.add(new Op.Fill(x + 1, yy, x + w - 1, yy + 1, OverlayMath.withAlpha(OverlayStyle.SCAN, scan)));
        }
        int line = OverlayMath.withAlpha(OverlayStyle.CYAN, crisp);
        out.add(new Op.Fill(x, y, x + w, y + 1, line));
        out.add(new Op.Fill(x, y + h - 1, x + w, y + h, line));
        out.add(new Op.Fill(x, y + 1, x + 1, y + h - 1, line));
        out.add(new Op.Fill(x + w - 1, y + 1, x + w, y + h - 1, line));
        int b = OverlayMath.withAlpha(OverlayStyle.CYAN_BRIGHT, f);
        int len = OverlayStyle.BRACKET_LEN, t = OverlayStyle.BRACKET_THICK;
        out.add(new Op.Fill(x, y, x + len, y + t - 1, b));
        out.add(new Op.Fill(x, y, x + t - 1, y + len, b));
        out.add(new Op.Fill(x + w - len, y, x + w, y + t - 1, b));
        out.add(new Op.Fill(x + w - t + 1, y, x + w, y + len, b));
        out.add(new Op.Fill(x, y + h - t + 1, x + len, y + h, b));
        out.add(new Op.Fill(x, y + h - len, x + t - 1, y + h, b));
        out.add(new Op.Fill(x + w - len, y + h - t + 1, x + w, y + h, b));
        out.add(new Op.Fill(x + w - t + 1, y + h - len, x + w, y + h, b));
        out.add(new Op.Text(x + PAD, y + PAD - 1, header, OverlayMath.withAlpha(OverlayStyle.CYAN_BRIGHT, crisp), true));
        out.add(new Op.Fill(x + PAD, y + PAD + OverlayStyle.TEXT_H + 1, x + w - PAD, y + PAD + OverlayStyle.TEXT_H + 2, OverlayMath.withAlpha(OverlayStyle.CYAN_DIM, 0.45f * f)));
        for (Row r : rows) out.add(new Op.Text(x + PAD + r.indent, y + r.top, r.text, OverlayMath.withAlpha(r.rgb, crisp), true));
        return new Frame(out, x, y, w, h);
    }

    private record Row(int top, int indent, String text, int rgb) {}

    /** The fade of the window at an age: in over 6 ticks, out over the last 20 of {@code total} ticks. 0 when it is over. */
    public static float fade(float age, int total) {
        if (age < 0 || age >= total) return 0f;
        float in = Math.min(1f, age / 6f);
        float out = Math.min(1f, (total - age) / 20f);
        return Math.max(0f, Math.min(in, out));
    }
}
