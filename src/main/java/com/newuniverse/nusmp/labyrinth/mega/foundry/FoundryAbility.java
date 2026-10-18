package com.newuniverse.nusmp.labyrinth.mega.foundry;

import java.util.Locale;

/**
 * Why: a foundry mob's abilities are ROWS OF DATA over a small set of pure kinds, so 50 abilities cost 25 pieces of code and a designer adds a fifty-first by writing a line in
 * {@code _abilities.json}. This record is one row; {@link Kind} is the kind (what the world adapter {@link FoundryAbilityRunner} does); the methods are the pure questions the AI
 * asks before it spends a cast: is it ready, is the target at the right distance, is the mob hurt enough, is the situation one this ability is for. Nothing here touches the
 * game, so every rule is unit tested ({@code FoundryAbilityTest}).
 *
 * @param cd     cooldown in ticks (0 only for a passive: {@code burst})
 * @param min    the nearest the target may be (blocks)
 * @param max    the farthest the target may be; 0 = the ability needs no target
 * @param wind   wind-up ticks: the mob stands, the cast clip plays and a Rukh gathers; 20 or more plays the big cast clip
 * @param pow    multiplier of the mob's attack damage (the share of max health for {@link Kind#HEAL}, of the target's reach for others: see the runner)
 * @param dur    duration in ticks (effects, auras, dashes)
 * @param n      count (arrows, summons)
 * @param r      radius in blocks
 * @param amp    effect amplifier (for {@link Kind#CLEAVE} the arc in degrees)
 * @param hp     the ability is only used while health is under this share (0 = always)
 * @param align  "road" (light on the Holy road, dark on the Abyssal one), "light" or "dark"
 */
public record FoundryAbility(String id, Kind kind, int cd, double min, double max, int wind, double pow, int dur, int n, double r, int amp, double hp, String align, String sound) {

    /** What an ability does; the order is never saved (ids are). */
    public enum Kind {
        DASH, LEAP, GUARD, VOLLEY, HEAL, SUMMON, DRAIN, ROOT, BLINK, ENRAGE, AURA, SLAM, BOLT, CLEAVE, CURSE, WARD, SPIT, PULL, BASH, RALLY, REGEN, THORNS, MARK, DIVE, BURST;

        public static Kind of(String name) {
            if (name == null) return null;
            try {
                return valueOf(name.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    /** Hard caps: a data pack that writes a silly number is clamped, not obeyed. */
    public static final int MAX_COUNT = 8, MAX_DURATION = 600, MAX_WIND = 60;
    public static final double MAX_RADIUS = 12.0, MAX_POWER = 3.0;

    /** A passive (death burst) is never cast; everything else has a cooldown. */
    public boolean passive() { return kind == Kind.BURST; }

    /** Does this ability want a target at all? (A guard, a heal or an aura does not.) */
    public boolean needsTarget() { return max > 0.0; }

    /** Is the cast the big one (the long clip)? */
    public boolean big() { return wind >= 20; }

    /** Cooldown over? {@code readyAt} is the tick the mob may use it again. */
    public boolean ready(long readyAt, long now) { return now >= readyAt; }

    /** Is the target at a distance this ability works at? A negative distance means no target. */
    public boolean inWindow(double dist) {
        if (!needsTarget()) return true;
        return dist >= min && dist <= max;
    }

    /** Is the mob hurt enough (or not too hurt: {@code hp} is the share under which it fires)? */
    public boolean hpOk(double healthShare) { return hp <= 0.0 || healthShare < hp; }

    /** Damage of one use for a mob whose attack damage is {@code attack}. */
    public double damage(double attack) { return Math.max(0.0, Math.min(MAX_POWER, pow)) * Math.max(0.0, attack); }

    /** Is the rukh of this ability the dark one for a mob of the Abyssal road ({@code abyssal}) ? */
    public boolean dark(boolean abyssal) {
        return switch (align == null ? "road" : align) {
            case "dark" -> true;
            case "light" -> false;
            default -> abyssal;
        };
    }

    /** The situation this ability is FOR: a heal needs someone hurt, a summon needs room for more, an aura needs someone close, a rally needs allies, a mark needs an unmarked target. */
    public boolean fits(Situation s) {
        if (!hpOk(s.healthShare())) return false;
        if (needsTarget() && !(s.dist() >= 0.0 && inWindow(s.dist()))) return false;
        return switch (kind) {
            case HEAL -> s.healthShare() < 0.85 || s.weakestAllyShare() < 0.8;
            case SUMMON -> s.minions() < Math.max(1, n) * 2 && s.hasTarget();
            case AURA -> s.hasTarget() && s.dist() >= 0.0 && s.dist() <= r + 3.0 && !s.auraActive();
            case GUARD -> s.hasTarget() && s.dist() >= 0.0 && s.dist() <= 12.0 && !s.guarded();
            case ENRAGE -> s.hasTarget() && !s.enraged();
            case RALLY, WARD -> s.hasTarget() && s.allies() > 0;
            case MARK -> s.hasTarget() && !s.targetMarked();
            case THORNS -> s.hasTarget() && s.dist() >= 0.0 && s.dist() <= 6.0 && !s.thorned();
            case REGEN -> s.healthShare() < 0.9 && !s.regenerating();
            case BLINK -> s.hasTarget();
            case BURST -> false;
            default -> s.hasTarget();
        };
    }

    /** What the AI knows when it asks: all plain numbers, so the rule is testable. */
    public record Situation(double dist, double healthShare, boolean hasTarget, int allies, double weakestAllyShare, int minions, boolean guarded, boolean enraged, boolean auraActive,
                            boolean targetMarked, boolean thorned, boolean regenerating) {
        public static Situation of(double dist, double healthShare) {
            return new Situation(dist, healthShare, dist >= 0.0, 0, 1.0, 0, false, false, false, false, false, false);
        }
    }

    /** Clamps every field of a parsed row into its range (used by the parser). */
    public static FoundryAbility clamped(String id, Kind kind, int cd, double min, double max, int wind, double pow, int dur, int n, double r, int amp, double hp, String align, String sound) {
        return new FoundryAbility(id, kind, Math.max(0, Math.min(6000, cd)), Math.max(0.0, min), Math.max(0.0, Math.min(40.0, max)), Math.max(0, Math.min(MAX_WIND, wind)),
                Math.max(0.0, Math.min(MAX_POWER, pow)), Math.max(0, Math.min(MAX_DURATION, dur)), Math.max(0, Math.min(MAX_COUNT, n)), Math.max(0.0, Math.min(MAX_RADIUS, r)),
                Math.max(0, Math.min(360, amp)), Math.max(0.0, Math.min(1.0, hp)), align == null || align.isBlank() ? "road" : align, sound == null ? "" : sound);
    }
}
