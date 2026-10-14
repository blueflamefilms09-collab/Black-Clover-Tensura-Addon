package com.newuniverse.nusmp.client.relics;

import com.newuniverse.nusmp.client.relics.RelicTooltipLayout.Para;

import java.util.List;
import java.util.function.Supplier;

/**
 * The safety net of the relic tooltip. An exception while the tooltip is built makes the game drop the whole tooltip (or worse), so every build goes through
 * {@link #safe}: if the full text cannot be made, the plain grade and slot lines are shown instead, and the tooltip is never empty. Pure.
 */
public final class RelicTooltipFallback {
    private RelicTooltipFallback() {}

    /** Paragraph tags of {@link RelicTooltip}: 1 is the grade colour, 2 the slot colour. */
    public static final int TAG_GRADE = 1, TAG_SLOT = 2;

    /** The two plain lines: "stars label" and "Slot: x". Null arguments become empty text, never an exception. */
    public static List<Para> lines(String stars, String gradeLabel, String slotLabel, boolean curio) {
        String grade = ((stars == null ? "" : stars) + "  " + (gradeLabel == null ? "" : gradeLabel)).trim();
        String slot = "Slot: " + (slotLabel == null ? "?" : slotLabel) + (curio ? " (Curios)" : "");
        return List.of(new Para(grade, TAG_GRADE), new Para(slot, TAG_SLOT));
    }

    /** The full lines, or the fallback if building them throws; if even the fallback throws, one blank-free placeholder line. Never null, never empty. */
    public static List<Para> safe(Supplier<List<Para>> full, Supplier<List<Para>> fallback) {
        try {
            List<Para> l = full.get();
            if (l != null && !l.isEmpty()) return l;
        } catch (RuntimeException | LinkageError ignored) {
            // fall through to the plain lines
        }
        try {
            List<Para> l = fallback.get();
            if (l != null && !l.isEmpty()) return l;
        } catch (RuntimeException | LinkageError ignored) {
            // fall through
        }
        return List.of(new Para("Relic", TAG_GRADE));
    }
}
