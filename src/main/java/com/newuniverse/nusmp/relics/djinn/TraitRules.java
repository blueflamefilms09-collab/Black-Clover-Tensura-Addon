package com.newuniverse.nusmp.relics.djinn;

/**
 * What a Half Equip's blow is worth, as numbers (pure, so DjinnTraitRulesTest can check every one): how much each Djinn special changes the share and
 * the cap of the rider by what it learns about the target and the bearer, and the clamp that protects players.
 *
 * <p>A rider is {@code min(blow * frac, cap)} extra damage on a melee hit. Against a player it is never more than {@link #PLAYER_RIDER_CAP} points
 * (30 percent of a 20 HP player), whatever the row's cap or a dark Djinn's multiplier says: the caps of the rows are for mobs.
 */
public final class TraitRules {
    private TraitRules() {}

    /** The most a rider adds to a hit on a player. */
    public static final float PLAYER_RIDER_CAP = 6.0f;

    /** Half Equip blows in a row (Bloodlust) count while each comes within this many ticks of the one before. */
    public static final int STREAK_WINDOW = 80;

    /** The most a row's share can grow by a special (a Bloodlust at its peak), as a multiple. */
    public static final double MAX_FACTOR = 2.0;

    /**
     * What the special learns about the blow. {@code healthFrac} is the target's health as a share of its maximum, {@code ailments} how many harmful
     * effects it carries, {@code bearerSpeed} the bearer's horizontal speed in blocks per tick (a sprint is about 0.28), {@code streak} how many blows
     * in a row came before this one (0 for the first).
     */
    public record Facts(boolean undead, int armor, double healthFrac, boolean invisible, int ailments, double bearerSpeed, int streak) {}

    /** The multiple of the row's share this blow gets (1.0 for a special that does not read the target). */
    public static double fracFactor(Djinn.Special s, Facts f) {
        return switch (s) {
            case SMITE -> f.undead() ? 2.0 : 1.0;
            case CRUSH -> 1.0 + Math.min(20.0, Math.max(0, f.armor())) / 20.0;
            case HUNT -> f.healthFrac() < 0.5 ? 1.5 : 1.0;
            case TRAMPLE -> 1.0 + Math.min(0.8, Math.max(0.0, f.bearerSpeed() - 0.05) * 3.0);
            case REVEAL -> f.invisible() ? 1.5 : 1.0;
            case CARRION -> f.ailments() >= 1 ? 1.5 : 1.0;
            case BLOODLUST -> 1.0 + 0.1 * Math.min(4, Math.max(0, f.streak()));
            case DRUMBEAT -> drumbeat(f.streak()) ? 2.0 : 1.0;
            default -> 1.0;
        };
    }

    /** The multiple of the row's cap: the same as the share's for every special that reads the target, except Crush, whose cap is the row's. */
    public static double capFactor(Djinn.Special s, Facts f) { return s == Djinn.Special.CRUSH ? 1.0 : fracFactor(s, f); }

    /** Is this blow a drumbeat: every third blow of a streak (the first two of each three are 0 and 1). */
    public static boolean drumbeat(int streak) { return streak >= 0 && streak % 3 == 2; }

    /** The rider of a blow: the share of it, capped, and for a player target never more than {@link #PLAYER_RIDER_CAP}. */
    public static float rider(float blow, double frac, double cap, boolean targetIsPlayer) {
        float bonus = (float) Math.min(Math.max(0f, blow) * frac, cap);
        if (targetIsPlayer) bonus = Math.min(bonus, PLAYER_RIDER_CAP);
        return Math.max(0f, bonus);
    }
}
