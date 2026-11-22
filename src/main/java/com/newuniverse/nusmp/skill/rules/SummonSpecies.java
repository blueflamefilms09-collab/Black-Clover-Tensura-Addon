package com.newuniverse.nusmp.skill.rules;

import com.newuniverse.nusmp.skill.rules.SummonRules.Card;

/**
 * Why: the Summoner's creatures (docs/summoner_creatures.md) are the Hell Mode summons Allen calls, not vanilla stand-ins. Which one a card calls, how big, how fast, how strong
 * and what its blow does are pure numbers, so they live here and are unit-tested. Append-only: the index is saved in the creature, so an entry is never moved or removed.
 * The power factor is the "very overpowered" part: the Unique's creature is Soldier-grade, the mastered Unique stronger, the Ultimate's twice the Unique's.
 */
public enum SummonSpecies {
    HAYATE("hayate", 1.00, 1.00, 0.36, false, 0.9f, 1.30f, Blow.NONE),
    CERORIN("cerorin", 1.60, 1.40, 0.34, false, 1.2f, 1.50f, Blow.BITE),
    ROCANEL("rocanel", 1.50, 1.00, 0.24, false, 0.8f, 1.80f, Blow.KNOCK),
    MIRA("mira", 2.40, 1.50, 0.22, false, 1.0f, 2.20f, Blow.KNOCK),
    HATCH("hatch", 0.60, 0.80, 0.30, true, 0.7f, 1.70f, Blow.POISON),
    TSUBAME("tsubame", 0.40, 0.60, 0.32, true, 0.6f, 0.60f, Blow.NONE),
    SOLARIN("solarin", 0.80, 0.30, 0.22, false, 0.7f, 1.10f, Blow.NONE),
    GENBU("genbu", 3.00, 0.80, 0.20, false, 1.4f, 1.00f, Blow.KNOCK),
    ELLIE("ellie", 0.70, 1.10, 0.28, true, 0.6f, 1.00f, Blow.WEAKEN),
    OKIYOSAN("okiyosan", 0.90, 1.30, 0.28, true, 0.6f, 1.00f, Blow.WEAKEN),
    DRADRA("dradra", 2.20, 1.80, 0.30, true, 1.6f, 1.90f, Blow.FIRE),
    MERUS("merus", 1.80, 1.60, 0.30, true, 0.7f, 1.80f, Blow.SLOW);

    /** What a blow of the creature adds to its damage: nothing, a bite (bleeding-style weakness-free extra), poison, burning, slowness, weakness, a knock-back. */
    public enum Blow { NONE, BITE, POISON, FIRE, SLOW, WEAKEN, KNOCK }

    public final String id;
    public final double hpMul, dmgMul, speed;
    public final boolean flying;
    public final float width, height;
    public final Blow blow;

    SummonSpecies(String id, double hp, double dmg, double speed, boolean flying, float width, float height, Blow blow) {
        this.id = id;
        this.hpMul = hp;
        this.dmgMul = dmg;
        this.speed = speed;
        this.flying = flying;
        this.width = width;
        this.height = height;
        this.blow = blow;
    }

    /** The species of a stored index, or null when it is out of range (a damaged save never crashes the creature). */
    public static SummonSpecies byIndex(int i) {
        SummonSpecies[] all = values();
        return i < 0 || i >= all.length ? null : all[i];
    }

    public static SummonSpecies byId(String id) {
        for (SummonSpecies s : values()) if (s.id.equals(id)) return s;
        return null;
    }

    /** The creature a card calls at a Synthesis rank: Beast and Legion are Hayate (Cerorin from rank 2), Stone is Rocanel (Mira from rank 2), Wraith is Ellie (Okiyosan from rank 1). */
    public static SummonSpecies forCard(Card card, int rank) {
        return switch (card) {
            case BEAST, LEGION -> rank >= 2 ? CERORIN : HAYATE;
            case STONE -> rank >= 2 ? MIRA : ROCANEL;
            case WRAITH -> rank >= 1 ? OKIYOSAN : ELLIE;
        };
    }

    /** Health and damage factor of the tier: 1, 1.25 (mastered Unique), 2, 2.5 (mastered Ultimate). */
    public static double power(Tier tier) {
        return switch (tier) {
            case BASE -> 1.0;
            case MASTERED -> 1.25;
            case ULTIMATE -> 2.0;
            case ULTIMATE_MASTERED -> 2.5;
        };
    }

    /** Body scale a performer takes from the tier (the Ultimate's Dragon and Angel loom). */
    public static float performerScale(Tier tier) { return tier.ultimate() ? 1.35f : 1.0f; }

    /** How long a performer (the creature a cast shows) stays, in ticks: three seconds, four for the Ultimate. Never forever. */
    public static int performerTicks(Tier tier) { return tier.ultimate() ? 80 : 60; }
}
