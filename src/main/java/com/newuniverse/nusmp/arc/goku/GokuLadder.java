package com.newuniverse.nusmp.arc.goku;

import com.newuniverse.nusmp.saiyan.SaiyanForms;

import java.util.List;

/**
 * Why: the form schedule of the Goku boss as pure data and pure functions, so the entity, the tests, the self-test and the doc all read one table (docs/goku_boss.md).
 * Goku walks the canon order Base, Super Saiyan, Super Saiyan 2, Super Saiyan 3, Super Saiyan God, Super Saiyan Blue, Blue Kaio-ken, Ultra Instinct Sign, Perfected Ultra Instinct and
 * True Ultra Instinct. He enters a form when his health falls under the step's line OR the fight has lasted long enough (the soft enrage timer), one step at a time and never faster
 * than {@link #MIN_DWELL} ticks per step, and never goes back. The numbers of a form (damage, defence, speed, colour) are NOT repeated here: they are the rows of {@link SaiyanForms}
 * (the owner's chart "All Saiyan Forms of Goku"), read by id, so a retuned chart retunes the boss. The Kaio-ken is not a step: it is a burst of his moves ({@code GokuMoves}).
 * No Minecraft type.
 */
public final class GokuLadder {
    private GokuLadder() {}

    /**
     * One step. {@code hpBelow}: enter when health is under this share of the maximum. {@code timeFrac}: enter when the fight has lasted this share of the enrage time.
     * {@code canonRatio}: how many times stronger the series says this form is than the one before it (0 = no number remembered; informational, see the doc: the game uses the chart).
     * {@code skin}: which of the {@link #SKINS} he wears. {@code line}: what he shouts.
     */
    public record Step(String id, double hpBelow, double timeFrac, double canonRatio, int skin, String line) {
        /** The chart row of the form, or null for Base. */
        public SaiyanForms.Form form() { return SaiyanForms.byId(id); }
    }

    /** Ticks a form lasts at least before the next one (a burst of damage never skips two steps at once). */
    public static final int MIN_DWELL = 120;
    /** Ticks of the visible power-up before a form takes hold (he can be hit all the time). */
    public static final int WINDUP = 40;
    /** The cap on how much of the chart's resistance counts as damage taken less (a share, 0..1). */
    public static final double MAX_REDUCTION = 0.6;

    /** The skin textures (textures/entity/arc_goku*.png), in {@link Step#skin()} order. */
    public static final List<String> SKINS = List.of("arc_goku", "arc_goku_ssj", "arc_goku_god", "arc_goku_blue", "arc_goku_ui");

    /** Index 0 is Base; the last index is True Ultra Instinct. Append-only. */
    public static final List<Step> STEPS = List.of(
            new Step("base", 2.0, 0.00, 1.0, 0, "Alright. Let's see what you've got!"),
            new Step("super_saiyan", 0.85, 0.10, 50.0, 1, "HAAAAAA! Super Saiyan!"),
            new Step("super_saiyan_2", 0.72, 0.20, 2.0, 1, "Super Saiyan 2! Feel this!"),
            new Step("super_saiyan_3", 0.60, 0.30, 4.0, 1, "AAAAAAARGH! Super Saiyan 3!"),
            new Step("super_saiyan_god", 0.50, 0.40, 0.0, 2, "A calm, divine power... Super Saiyan God."),
            new Step("super_saiyan_blue", 0.40, 0.50, 50.0, 3, "Super Saiyan Blue! Don't blink."),
            new Step("super_saiyan_blue_kaioken", 0.30, 0.60, 10.0, 3, "Kaio-ken times ten, on top of Blue!"),
            new Step("ultra_instinct_sign", 0.20, 0.70, 0.0, 4, "My body... is moving by itself."),
            new Step("perfected_ultra_instinct", 0.10, 0.82, 0.0, 4, "This is it. Perfected Ultra Instinct."),
            new Step("true_ultra_instinct", 0.04, 0.95, 0.0, 4, "True Ultra Instinct. Come on, give me everything!"));

    public static int size() { return STEPS.size(); }

    public static int last() { return STEPS.size() - 1; }

    public static Step step(int idx) { return STEPS.get(clamp(idx)); }

    public static int clamp(int idx) { return Math.max(0, Math.min(last(), idx)); }

    /** The step index of a form id, or -1. */
    public static int indexOf(String id) {
        for (int i = 0; i < STEPS.size(); i++) if (STEPS.get(i).id().equals(id)) return i;
        return -1;
    }

    /** The highest step the situation asks for: health under its line, or the fight old enough. Pure and monotonic in both inputs. */
    public static int wanted(double hpFraction, long fightTicks, long enrageTicks) {
        double hp = Double.isNaN(hpFraction) ? 1.0 : hpFraction;
        int w = 0;
        for (int i = 1; i < STEPS.size(); i++) {
            Step s = STEPS.get(i);
            if (hp < s.hpBelow() || fightTicks >= Math.round(s.timeFrac() * Math.max(1L, enrageTicks))) w = i;
        }
        return w;
    }

    /** The step he is in after this tick: at most one step up, only after {@link #MIN_DWELL} ticks in the current one, never down. */
    public static int next(int current, double hpFraction, long fightTicks, long enrageTicks, long ticksInForm) {
        int cur = clamp(current);
        if (cur >= last() || ticksInForm < MIN_DWELL) return cur;
        return wanted(hpFraction, fightTicks, enrageTicks) > cur ? cur + 1 : cur;
    }

    // ------------------------------------------------------------------ what a form does (read from the chart)
    /**
     * The damage multiplier of a step: 1 for Base, else 1 + the chart's damage bonus x strength (the difficulty's form strength). It never falls below an earlier step's (the chart's
     * Ultra Instinct Sign is a dodging form with a lower damage cell than Blue Kaio-ken; the boss keeps the higher number so every step is at least as dangerous as the last).
     */
    public static double damageMul(int idx, double strength) {
        double best = 0.0;
        for (int i = 1; i <= clamp(idx); i++) {
            SaiyanForms.Form f = STEPS.get(i).form();
            if (f != null) best = Math.max(best, f.damage());
        }
        return 1.0 + best * Math.max(0.0, strength);
    }

    /** The speed multiplier of a step (chart's speed bonus x strength; Super Saiyan 3 is slower in the chart and stays so). */
    public static double speedMul(int idx, double strength) {
        SaiyanForms.Form f = step(idx).form();
        return f == null ? 1.0 : Math.max(0.5, 1.0 + f.speed() * Math.max(0.0, strength));
    }

    /** The chart's defence of a step (0 for Base), never below an earlier step's. */
    public static double defense(int idx) {
        double best = 0.0;
        for (int i = 1; i <= clamp(idx); i++) {
            SaiyanForms.Form f = STEPS.get(i).form();
            if (f != null) best = Math.max(best, f.defense());
        }
        return best;
    }

    /** The share of every hit he takes less in this step: defence / (1 + defence), at most {@link #MAX_REDUCTION}. */
    public static double reduction(int idx) { return Math.min(MAX_REDUCTION, defense(idx) / (1.0 + defense(idx))); }

    /** 0xRRGGBB of the step's aura (a pale blue for Base). */
    public static int rgb(int idx) {
        SaiyanForms.Form f = step(idx).form();
        return f == null ? 0x9FD8FF : f.rgb() & 0xFFFFFF;
    }

    /** Does the step have crackling lightning (Super Saiyan 2 and 3, Blue Kaio-ken and the Ultra Instinct forms)? */
    public static boolean electric(int idx) {
        int i = clamp(idx);
        return i == 2 || i == 3 || i == 6 || i == 8 || i == 9;
    }

    /** The cumulative canon power of a step over Base (a ratio of 0 counts as x1). Informational: the doc prints it. */
    public static double canonPower(int idx) {
        double p = 1.0;
        for (int i = 1; i <= clamp(idx); i++) p *= STEPS.get(i).canonRatio() > 0 ? STEPS.get(i).canonRatio() : 1.0;
        return p;
    }

    /** The skin texture name of a step. */
    public static String skin(int idx) { return SKINS.get(Math.max(0, Math.min(SKINS.size() - 1, step(idx).skin()))); }
}
