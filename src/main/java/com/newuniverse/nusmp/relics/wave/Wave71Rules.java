package com.newuniverse.nusmp.relics.wave;

import java.util.Locale;

/**
 * Why: the pure rules behind the passives of the 35 weapons of wave 71 (1.97.0: legend, god and genesis grade weapons). A passive is one {@link Rule}: when its
 * {@link Cond} holds for the blow that is being dealt, the damage grows by a fraction, a status lands on the foe, a share of the damage comes back as life
 * or the foe is set alight. No Minecraft type is used here, so the table and the arithmetic are unit-tested; {@link Wave71Events} reads the facts off the game.
 */
public final class Wave71Rules {
    private Wave71Rules() {}

    /** When a rule's bonus applies. */
    public enum Cond {
        ALWAYS, LOW_HP_FOE, UNDEAD_FIEND, WYRM, FLYING_FOE, WET_FOE, FIRE_FOE, DAYLIGHT, NIGHT, FULL_HP_SELF, LOW_HP_SELF, BOSS, SNEAKING, FALLING, BIG_FOE
    }

    /**
     * One passive.
     * @param cond      when the damage bonus applies
     * @param bonus     fraction added to the blow (0.5 = half as much again)
     * @param chance    chance (0..1) that the bonus and the status land; 1 = every blow
     * @param effect    mob effect id put on the foe ("" = none)
     * @param secs      effect length in seconds
     * @param amp       effect amplifier
     * @param lifesteal share of the blow's final damage that comes back to the wielder as life (0 = none)
     * @param ignite    seconds the foe burns (0 = none)
     */
    public record Rule(Cond cond, double bonus, double chance, String effect, int secs, int amp, double lifesteal, int ignite) {
        public static Rule of(Cond cond, double bonus) { return new Rule(cond, bonus, 1.0, "", 0, 0, 0.0, 0); }

        public Rule chance(double c) { return new Rule(cond, bonus, c, effect, secs, amp, lifesteal, ignite); }

        public Rule status(String id, int seconds, int amplifier) { return new Rule(cond, bonus, chance, id, seconds, amplifier, lifesteal, ignite); }

        public Rule steal(double share) { return new Rule(cond, bonus, chance, effect, secs, amp, share, ignite); }

        public Rule burn(int seconds) { return new Rule(cond, bonus, chance, effect, secs, amp, lifesteal, seconds); }
    }

    /** What the game says about one blow. */
    public record Facts(boolean lowHpFoe, boolean undeadOrFiend, boolean wyrm, boolean flyingFoe, boolean wetFoe, boolean fireFoe, boolean day, boolean night,
                        boolean fullHpSelf, boolean lowHpSelf, boolean boss, boolean sneaking, boolean falling, boolean bigFoe) {}

    /** Life share of the wielder under which the berserk rules wake, and the foe's share under which it is "low". */
    public static final double LOW_SELF = 0.4, LOW_FOE = 0.3, BIG_FOE_HEALTH = 100.0;
    /** The most a single blow may heal the wielder, whatever the share (a point value; the balance law caps the heal again). */
    public static final float MAX_STEAL = 60f;

    public static boolean applies(Cond c, Facts f) {
        return switch (c) {
            case ALWAYS -> true;
            case LOW_HP_FOE -> f.lowHpFoe();
            case UNDEAD_FIEND -> f.undeadOrFiend();
            case WYRM -> f.wyrm();
            case FLYING_FOE -> f.flyingFoe();
            case WET_FOE -> f.wetFoe();
            case FIRE_FOE -> f.fireFoe();
            case DAYLIGHT -> f.day();
            case NIGHT -> f.night();
            case FULL_HP_SELF -> f.fullHpSelf();
            case LOW_HP_SELF -> f.lowHpSelf();
            case BOSS -> f.boss();
            case SNEAKING -> f.sneaking();
            case FALLING -> f.falling();
            case BIG_FOE -> f.bigFoe();
        };
    }

    /** The damage multiplier of a blow: 1 + bonus when the rule applies and the chance roll (0..1) is under it. */
    public static double factor(Rule r, Facts f, double roll) {
        if (r == null || !applies(r.cond(), f) || roll >= r.chance()) return 1.0;
        return 1.0 + r.bonus();
    }

    /** Does the rule's status or burn land on this blow? Only when its condition holds and the chance roll is under the chance. */
    public static boolean lands(Rule r, Facts f, double roll) { return r != null && applies(r.cond(), f) && roll < r.chance(); }

    /** Life given back for a blow of this final damage. */
    public static float steal(Rule r, float finalDamage) {
        if (r == null || r.lifesteal() <= 0 || finalDamage <= 0) return 0f;
        return (float) Math.min(MAX_STEAL, finalDamage * r.lifesteal());
    }

    /** A dragon, a wyrm, a serpent or a hydra by its entity path. */
    public static boolean isWyrm(String path) {
        String p = path == null ? "" : path.toLowerCase(Locale.ROOT);
        return p.contains("dragon") || p.contains("wyvern") || p.contains("drake") || p.contains("wyrm") || p.contains("serpent") || p.contains("hydra") || p.contains("naga");
    }

    /** A demon, devil or other fiend by its entity path (the undead are read from the entity tag). */
    public static boolean isFiend(String path) {
        String p = path == null ? "" : path.toLowerCase(Locale.ROOT);
        return p.contains("demon") || p.contains("devil") || p.contains("fiend") || p.equals("imp") || p.endsWith("_imp") || p.contains("wraith") || p.contains("ghost") || p.contains("lich");
    }

    /** The ids whose damage comes from arrows and bolts (bows and crossbows): the passive reads the projectile's blow too. */
    public static boolean isRangedId(String id) { return id != null && (id.endsWith("_bow") || id.endsWith("_crossbow")); }
}
