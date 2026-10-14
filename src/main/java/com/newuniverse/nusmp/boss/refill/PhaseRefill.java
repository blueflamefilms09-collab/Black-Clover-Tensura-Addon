package com.newuniverse.nusmp.boss.refill;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The "second health bar" of a boss's final mode (pure: no Minecraft types, deterministic). When a boss enters its 100% mode (the Gates
 * of the Tree of Qliphoth, an enrage, a true form, a last stage) it calls {@link #begin}: the mode is written into the set of modes this
 * fight has already refilled (so it can never refill twice), and for {@link #DEFAULT_TICKS} ticks {@link #tick} hands out the health the
 * boss should have, easing from where it stood up to its maximum. While {@link #active} the boss is invulnerable (it is changing form).
 * <p>
 * The state is tiny and is saved with the boss ({@link #save} / {@link #load}), so reloading the world in the middle of a fight neither
 * repeats a refill nor loses one in progress. {@link #reset} is for the end of a fight (abandon / full reset); a boss that dies never
 * needs it. The class only computes numbers; the boss applies the health with its damage budget bypassed.
 */
public final class PhaseRefill {
    /** About two seconds. */
    public static final int DEFAULT_TICKS = 40;
    /** Ticks of the bright flash at the start of a refill (bar tint and VFX). */
    public static final int FLASH_TICKS = 10;
    /** Steps the bar glow is quantised to, so the bar style is re-sent only a handful of times per refill. */
    public static final int GLOW_STEPS = 4;
    /** Most modes remembered (a guard for corrupt save data). */
    private static final int MAX_MODES = 16;

    private final Set<String> done = new LinkedHashSet<>();
    private String active;
    private int total, left;
    private float from, to;

    /** True if 'mode' has not been refilled in this fight and nothing else is refilling now. */
    public boolean canBegin(String mode) {
        return mode != null && !mode.isBlank() && active == null && !done.contains(mode) && done.size() < MAX_MODES;
    }

    /**
     * Starts the refill of 'mode' from the health 'current' to 'max' over 'ticks' (at least 1). Returns false (and changes nothing) when
     * the mode was already refilled, another refill runs, or the numbers make no sense. A boss already at full health still consumes the
     * mode (no later refill), but has nothing to heal: the refill ends on the first tick.
     */
    public boolean begin(String mode, float current, float max, int ticks) {
        if (!canBegin(mode) || !(max > 0f) || Float.isNaN(current)) return false;
        done.add(mode);
        active = mode;
        total = Math.max(1, ticks);
        left = total;
        from = Math.max(0f, Math.min(current, max));
        to = max;
        return true;
    }

    /** True from {@link #begin} until the refill is finished: the boss takes no damage meanwhile. */
    public boolean active() { return active != null; }

    public String mode() { return active; }

    /** True if 'mode' has been refilled already in this fight (also true while it is running). */
    public boolean done(String mode) { return done.contains(mode); }

    public int doneCount() { return done.size(); }

    public int ticksLeft() { return left; }

    public int totalTicks() { return total; }

    /** 0..1 over the refill (0 when idle). */
    public float progress() { return active == null ? 0f : 1f - left / (float) Math.max(1, total); }

    /** Follows a changing maximum (a buff that raised max health mid-refill): the target is always the current maximum. */
    public void retarget(float max) { if (active != null && max > 0f) to = max; }

    /**
     * One tick: the health the boss should have now, or -1 when no refill runs. Never lower than the health the refill started at; the
     * last tick returns exactly the maximum and ends the refill.
     */
    public float tick() {
        if (active == null) return -1f;
        left = Math.max(0, left - 1);
        float k = progress();
        float eased = k * k * (3f - 2f * k);
        float hp = left == 0 ? to : from + (to - from) * eased;
        if (left == 0) active = null;
        return Math.max(from, Math.min(to, hp));
    }

    /** Ends a refill at once and returns the health to set (the maximum), or -1 when none runs. */
    public float finish() {
        if (active == null) return -1f;
        active = null;
        left = 0;
        return to;
    }

    /** 0..1 strength of the glow: a bright flash at the start that fades out, and a small second pulse at the very end. */
    public float glow() {
        if (active == null) return 0f;
        int elapsed = total - left;
        float flash = elapsed < FLASH_TICKS ? 1f - elapsed / (float) FLASH_TICKS : 0f;
        float bright = 0.35f * (float) Math.sin(Math.PI * progress());      // a steady shimmer while it fills
        float endPulse = left <= 6 ? 0.5f * (6 - left) / 6f : 0f;
        return Math.max(0f, Math.min(1f, Math.max(flash, Math.max(bright, endPulse))));
    }

    /** The glow rounded to {@link #GLOW_STEPS} steps (0 .. 1). */
    public static float glowStep(float glow) {
        float g = Math.max(0f, Math.min(1f, glow));
        return Math.round(g * GLOW_STEPS) / (float) GLOW_STEPS;
    }

    /** The bar tint 'rgb' lightened toward white by 'glow' (at most 80%). */
    public static int lift(int rgb, float glow) {
        float k = Math.max(0f, Math.min(1f, glow)) * 0.8f;
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        r = Math.round(r + (255 - r) * k);
        g = Math.round(g + (255 - g) * k);
        b = Math.round(b + (255 - b) * k);
        return (r << 16) | (g << 8) | b;
    }

    /** Forgets everything: the fight ended (abandoned and reset) and a new one may refill again. */
    public void reset() {
        done.clear();
        active = null;
        total = left = 0;
        from = to = 0f;
    }

    // ---------------------------------------------------------------- save data
    /** Everything worth saving, as plain values: the modes done (comma separated), the running mode, its ticks and its numbers. */
    public record Saved(String doneCsv, String activeMode, int total, int left, float from, float to) {}

    public Saved save() {
        return new Saved(String.join(",", done), active == null ? "" : active, total, left, from, to);
    }

    /** Restores a saved state (tolerates null, blanks and nonsense: it then simply starts empty). */
    public void load(Saved s) {
        reset();
        if (s == null) return;
        if (s.doneCsv() != null && !s.doneCsv().isBlank())
            for (String m : s.doneCsv().split(",")) if (!m.isBlank() && done.size() < MAX_MODES) done.add(m.trim());
        if (s.activeMode() != null && !s.activeMode().isBlank() && s.left() > 0 && s.total() >= s.left() && s.to() > 0f) {
            active = s.activeMode();
            done.add(active);
            total = s.total();
            left = s.left();
            from = Math.max(0f, s.from());
            to = s.to();
        }
    }
}
