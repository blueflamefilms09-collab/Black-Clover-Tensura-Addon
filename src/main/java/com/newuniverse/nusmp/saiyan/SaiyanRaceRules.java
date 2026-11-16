package com.newuniverse.nusmp.saiyan;

import java.util.Set;

/**
 * Why: Saiyan as a race with four STAGES (Dragon Ball rework, phase 2), as pure data and arithmetic. The registered Tensura race API could not be verified from the repo or the jars of this session
 * (docs/dragon_ball/phase_report.md says what was and was not seen), so the race is built next to the existing {@code SaiyanState.saiyan} flag, which stays the source of truth: the stage is one more saved
 * number ({@code SaiyanState.raceStage}, 0 = not set), the buffs of a stage are read by the multiverse race buffs ({@code RaceBuffs}), and the whole thing is behind a server switch that is OFF by default.
 * Stages: 1 Saiyan (the flag turned on), 2 Primal Saiyan (Super Saiyan 3 earned and a high Battle Power), 3 Divine Saiyan (the Super Saiyan God ritual), 4 Autonomous Martial God (Perfected Ultra Instinct, a
 * high Battle Power and Zenkai). A stage is never lost by a change of a requirement; it only ever rises. Names are the spec's, the first one is "Saiyan" because a Saiyan is born one.
 */
public final class SaiyanRaceRules {
    private SaiyanRaceRules() {}

    public static final int NONE = 0, SAIYAN = 1, PRIMAL = 2, DIVINE = 3, AUTONOMOUS = 4;
    public static final String[] IDS = {"", "saiyan", "primal_saiyan", "divine_saiyan", "autonomous_god"};
    public static final String[] NAMES = {"", "Saiyan", "Primal Saiyan", "Divine Saiyan", "Autonomous Martial God"};
    /** The save flag the ritual leaves (the stage 3 requirement). */
    public static final String RITUAL_FLAG = "ssg_ritual";

    public static int clampStage(int s) { return Math.max(NONE, Math.min(AUTONOMOUS, s)); }

    public static String id(int stage) { return IDS[clampStage(stage)]; }

    public static String name(int stage) { return NAMES[clampStage(stage)]; }

    public static String nameKey(int stage) { return "anime_multiverse.saiyan.racestage." + id(stage); }

    /** What the next stage asks for (the server can change every number). */
    public record Need(double primalBp, double autonomousBp, int autonomousZenkai) {
        public static final Need DEFAULT = new Need(400_000.0, 8_000_000.0, 5);
    }

    /** What a Saiyan has done, as the stages read it. */
    public record Progress(double bp, int zenkai, Set<String> unlocked, Set<String> flags) {}

    /** The stage a Saiyan with this progress reaches from {@code stage}: one step at a time, never down. 0 (not set) counts as 1 for a Saiyan. */
    public static int next(int stage, Need n, Progress p) {
        int s = Math.max(SAIYAN, clampStage(stage));
        if (s == SAIYAN && p.bp() >= n.primalBp() && p.unlocked().contains("super_saiyan_3")) s = PRIMAL;
        if (s == PRIMAL && p.flags().contains(RITUAL_FLAG)) s = DIVINE;
        if (s == DIVINE && p.bp() >= n.autonomousBp() && p.zenkai() >= n.autonomousZenkai() && p.unlocked().contains("perfected_ultra_instinct")) s = AUTONOMOUS;
        return Math.max(s, clampStage(stage));
    }

    /** What is still missing for the next stage, in words (empty at the last stage). */
    public static java.util.List<String> missing(int stage, Need n, Progress p) {
        java.util.List<String> out = new java.util.ArrayList<>();
        int s = Math.max(SAIYAN, clampStage(stage));
        if (s == SAIYAN) {
            if (p.bp() < n.primalBp()) out.add("Battle Power " + Math.round(n.primalBp()));
            if (!p.unlocked().contains("super_saiyan_3")) out.add("the Super Saiyan 3 form");
        } else if (s == PRIMAL) {
            if (!p.flags().contains(RITUAL_FLAG)) out.add("the Super Saiyan God ritual (/saiyan ritual)");
        } else if (s == DIVINE) {
            if (p.bp() < n.autonomousBp()) out.add("Battle Power " + Math.round(n.autonomousBp()));
            if (p.zenkai() < n.autonomousZenkai()) out.add(n.autonomousZenkai() + " Zenkai (you have " + p.zenkai() + ")");
            if (!p.unlocked().contains("perfected_ultra_instinct")) out.add("the Perfected Ultra Instinct form");
        }
        return out;
    }

    /** The small buff of a stage, as fractions (attack, movement speed) and flat amounts (health, armour) and a knockback resistance, before the server's scale. Always modest: Tensura's own race does the rest. */
    public record Bonus(double attack, double speed, double health, double armor, double knockback) {}

    private static final Bonus[] BONUS = {new Bonus(0, 0, 0, 0, 0), new Bonus(0.05, 0.02, 2, 0, 0.05), new Bonus(0.08, 0.04, 4, 1, 0.10), new Bonus(0.10, 0.05, 6, 2, 0.15), new Bonus(0.12, 0.06, 8, 3, 0.20)};

    /** The bonus of a stage, scaled (the scale is the server's, kept between 0 and 2). */
    public static Bonus bonus(int stage, double scale) {
        Bonus b = BONUS[clampStage(stage)];
        double k = Double.isNaN(scale) ? 0.0 : Math.max(0.0, Math.min(2.0, scale));
        return new Bonus(b.attack() * k, b.speed() * k, b.health() * k, b.armor() * k, Math.min(0.8, b.knockback() * k));
    }
}
