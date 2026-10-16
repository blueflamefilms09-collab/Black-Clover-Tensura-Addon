package com.newuniverse.nusmp.avatar;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Why: what a personality MEANS for movement, fighting and speech has to be one readable table that is unit-testable without a game, instead of
 * magic numbers scattered over goals. The enum {@link AvatarPersonality} stays a pure save format; this class holds the numbers and the rule for
 * mixing two personalities (primary + secondary): numbers that describe distance or cooldown take the average or the better one as documented on
 * {@link #combine}, yes/no behaviours are OR-ed, so a hybrid has every flavour of both parents.
 */
public final class AvatarPersonalityTraits {
    private AvatarPersonalityTraits() {}

    /**
     * @param followDistance   blocks the Avatar likes to keep from its creator (a hint to the personality goals, AV1's follow goal owns the real one)
     * @param aggroRange       blocks within which it notices enemies
     * @param retreatHp        health fraction below which it backs off (0 = never)
     * @param attackSpeed      multiplier on its melee attack speed
     * @param cooldownMult     multiplier on skill cooldowns (below 1 = casts more often)
     * @param protectBelowHp   when the creator is below this health fraction it gets a protective bonus (0 = none)
     * @param styles           speech style tags (used by the chatter mixer and the docs)
     */
    public record Traits(double followDistance, double aggroRange, double retreatHp, double attackSpeed, double cooldownMult, double protectBelowHp,
                         boolean avoidsWeakPoints, boolean fleesBosses, boolean pullsLoot, boolean eatsFood, boolean idlesWithCreator,
                         boolean oneWordSpeech, boolean renamesAttacks, boolean defendsVillagers, boolean commentsOnRelics, boolean frontLoaded,
                         Set<String> styles) {
        public Traits {
            styles = Set.copyOf(new TreeSet<>(styles));
        }
    }

    /** Fraction by which a creator below {@link Traits#protectBelowHp()} boosts the protective Tsundere reflex (damage and speed). */
    public static final double PROTECTIVE_BONUS = 0.25;

    private static final Map<AvatarPersonality, Traits> TABLE = new EnumMap<>(AvatarPersonality.class);

    private static void put(AvatarPersonality p, double follow, double aggro, double retreat, double atk, double cd, double protect,
                            String flags, String... styles) {
        TABLE.put(p, new Traits(follow, aggro, retreat, atk, cd, protect,
                flags.contains("W"), flags.contains("B"), flags.contains("L"), flags.contains("F"), flags.contains("Z"), flags.contains("1"),
                flags.contains("R"), flags.contains("V"), flags.contains("S"), flags.contains("^"), Set.of(styles)));
    }

    static {
        //                          follow aggro retreat atk   cd    protect flags: W weak-point-avoid B boss-flee L loot F food Z idle 1 one-word R rename V villagers S relic-scholar ^ speech-lead
        put(AvatarPersonality.LOYAL,       3,  16, 0.15, 1.00, 1.00, 0.0,  "",  "formal", "devoted");
        put(AvatarPersonality.INDEPENDENT, 8,  16, 0.20, 1.00, 1.00, 0.0,  "^", "aloof", "dry");
        put(AvatarPersonality.AGGRESSIVE,  4,  24, 0.05, 1.15, 0.95, 0.0,  "^", "taunting", "loud");
        put(AvatarPersonality.MERCHANT,    4,  12, 0.30, 1.00, 1.00, 0.0,  "L", "haggling", "polite");
        put(AvatarPersonality.HEROIC,      4,  20, 0.10, 1.00, 1.00, 0.0,  "V", "righteous", "shouting");
        put(AvatarPersonality.TSUNDERE,    5,  16, 0.20, 1.00, 1.00, 0.5,  "^", "insulting", "flustered");
        put(AvatarPersonality.YANDERE,     2,  20, 0.00, 1.10, 1.00, 0.0,  "^", "sweet", "possessive");
        put(AvatarPersonality.LAZY,        6,  10, 0.30, 0.60, 0.70, 0.0,  "Z^", "sleepy", "yawning");
        put(AvatarPersonality.SCHOLAR,     5,  14, 0.35, 1.00, 0.90, 0.0,  "S", "analytical", "lecturing");
        put(AvatarPersonality.SADISTIC,    4,  20, 0.10, 1.00, 1.00, 0.0,  "W^", "teasing", "cruel");
        put(AvatarPersonality.CHUNIBYO,    4,  16, 0.15, 1.00, 1.00, 0.0,  "R", "grandiose", "dramatic");
        put(AvatarPersonality.GLUTTONOUS,  4,  14, 0.25, 1.00, 1.00, 0.0,  "F", "hungry", "cheerful");
        put(AvatarPersonality.COWARDLY,    2,   8, 0.60, 1.00, 1.00, 0.0,  "B^", "stuttering", "panicked");
        put(AvatarPersonality.STOIC,       4,  16, 0.10, 1.00, 1.00, 0.0,  "1^", "terse", "flat");
    }

    public static Traits of(AvatarPersonality p) {
        Traits t = p == null ? null : TABLE.get(p);
        return t != null ? t : TABLE.get(AvatarPersonality.LOYAL);
    }

    /**
     * The traits of an Avatar with a primary and an optional secondary personality (null/absent secondary = the primary alone).
     * Rules: follow distance, attack speed, retreat HP and the creator-protection threshold are AVERAGED (a hybrid sits between its parents);
     * aggro range takes the LARGER one (it notices what the sharper parent would); the cooldown multiplier takes the LOWER one (the faster
     * caster wins); every yes/no behaviour is OR-ed; style tags are united; "speech lead" is true when the primary leads.
     */
    public static Traits combine(AvatarPersonality primary, AvatarPersonality secondary) {
        Traits a = of(primary);
        if (secondary == null || secondary == primary) return a;
        Traits b = of(secondary);
        Set<String> tags = new TreeSet<>(a.styles());
        tags.addAll(b.styles());
        return new Traits((a.followDistance + b.followDistance) / 2, Math.max(a.aggroRange, b.aggroRange), (a.retreatHp + b.retreatHp) / 2,
                (a.attackSpeed + b.attackSpeed) / 2, Math.min(a.cooldownMult, b.cooldownMult),
                avgNonZero(a.protectBelowHp, b.protectBelowHp),
                a.avoidsWeakPoints || b.avoidsWeakPoints, a.fleesBosses || b.fleesBosses, a.pullsLoot || b.pullsLoot, a.eatsFood || b.eatsFood,
                a.idlesWithCreator || b.idlesWithCreator, a.oneWordSpeech || b.oneWordSpeech, a.renamesAttacks || b.renamesAttacks,
                a.defendsVillagers || b.defendsVillagers, a.commentsOnRelics || b.commentsOnRelics, a.frontLoaded, tags);
    }

    private static double avgNonZero(double x, double y) {
        if (x <= 0) return y;
        if (y <= 0) return x;
        return (x + y) / 2;
    }

    /** D&D ability modifier of a score (10-11 = 0, 12-13 = +1, 8-9 = -1). */
    public static int modifier(int score) { return Math.floorDiv(score - 10, 2); }

    /**
     * Multiplier on skill cooldowns from the Avatar's INT and WIS: every point of combined modifier shaves 3%, between x0.5 and x1.25. A genius
     * wizard casts faster, a dullard slower; a plain 10/10 Avatar is exactly x1.
     */
    public static double mentalCooldownMult(int intelligence, int wisdom) {
        double m = 1.0 - 0.03 * (modifier(intelligence) + modifier(wisdom));
        return Math.max(0.5, Math.min(1.25, m));
    }
}
