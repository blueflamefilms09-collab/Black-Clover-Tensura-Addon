package com.newuniverse.nusmp.relics.wave;

import java.util.function.IntUnaryOperator;

/**
 * Why: wave 29 (Pocket Portal, Potion of Vampirism, Romlo's Potion of Sleep, Spider Brew) turns tabletop dice and distances into Minecraft numbers; this
 * pure class (no game types) holds every one of them, so the rules can be tested without a world (docs: relics4/doc_wave29.md).
 * <p>
 * Time scale: a round is 6 s (120 ticks), 5 ft is one block, a long rest is a Minecraft day (24000 ticks). The brews add their own scale: a Spider Brew
 * "hour" is five minutes, a Sleep "hour" one minute (a sleeper is helpless, so the potion is shorter than its card).
 */
final class Wave29Rules {
    private Wave29Rules() {}

    static final int ROUND = 120, DAY = 24000;

    // ================================================================ dice
    /** A die with {@code sides} faces from a roller that answers 0..n-1. */
    static int die(IntUnaryOperator roll, int sides) {
        return 1 + Math.max(0, Math.min(sides - 1, roll.applyAsInt(sides)));
    }

    static int d20(IntUnaryOperator roll) { return die(roll, 20); }

    static int dice(IntUnaryOperator roll, int count, int sides) {
        int sum = 0;
        for (int i = 0; i < count; i++) sum += die(roll, sides);
        return sum;
    }

    // ================================================================ Pocket Portal
    /** Open until the end of your next turn: two rounds. */
    static final int PORTAL_TICKS = 2 * ROUND;
    /** 10 ft radius = 2 blocks, and a creature of any height inside it counts as entering. */
    static final double PORTAL_RADIUS = 2.0, PORTAL_ABOVE = 2.5, PORTAL_BELOW = 1.0;
    /** How far the orb can be smashed (the throw), how often the portals are swept, how many creatures one sweep may carry, how many portals exist at once. */
    static final double PORTAL_REACH = 12.0;
    static final int PORTAL_SWEEP = 5, PORTAL_CARRY = 8, PORTAL_MAX = 16;
    /** After arriving a creature is ignored by portals for three seconds, so a destination inside a portal's ring does not bounce it back. */
    static final int ARRIVAL_GRACE = 60;
    /** "Within 5 feet of the destination": one block. */
    static final double ARRIVAL_SPREAD = 1.0;
    /** A creature this healthy is no ordinary traveller (a boss) and is never carried. */
    static final double PORTAL_MAX_HEALTH = 300.0;

    /** Is a creature at this offset from the portal's centre inside the 10 ft disc (and within its height band)? */
    static boolean inPortal(double dx, double dy, double dz) {
        return dx * dx + dz * dz <= PORTAL_RADIUS * PORTAL_RADIUS && dy >= -PORTAL_BELOW && dy <= PORTAL_ABOVE;
    }

    // ================================================================ Potion of Vampirism
    enum Vampire { CURSE, CHILDREN, REGEN }

    /** The card's d20: 1-12 cursed, 13-18 Children of the Night, 19-20 Regeneration. */
    static Vampire vampire(int d20) { return d20 <= 12 ? Vampire.CURSE : d20 <= 18 ? Vampire.CHILDREN : Vampire.REGEN; }

    /** "3 days" and "five days" are Minecraft days (24000 ticks each): 72000 and 120000 ticks. */
    static final int CURSE_TICKS = 3 * DAY, CHILDREN_TICKS = 5 * DAY;
    /** The pack answers at most once in five minutes, arrives 1d4 rounds after the call, and stays five minutes (the relics' summon limit; the card says an hour). */
    static final int PACK_COOLDOWN = 6000, PACK_LIFE = 6000, PACK_MAX = 6;

    /** Ticks until the pack arrives: 1d4 rounds. */
    static int packDelay(int d4) { return Math.max(1, Math.min(4, d4)) * ROUND; }

    /**
     * How many creatures answer: the card says 2d4 swarms of bats or rats, and 3d6 wolves while outdoors. Each swarm or wolf is one creature here,
     * the roll halved (rounded up) to stay playable, and capped at the six summons a player may have.
     */
    static int packSize(boolean outdoors, IntUnaryOperator roll) {
        int raw = outdoors ? dice(roll, 3, 6) : dice(roll, 2, 4);
        return Math.max(1, Math.min(PACK_MAX, (raw + 1) / 2));
    }

    /** The cursed vampire burns for one health a second in daylight, but never below two hearts. */
    static final float BURN = 1.0f, BURN_FLOOR = 4.0f;

    static boolean burns(float health) { return health > BURN_FLOOR; }

    /** Sunlight reaches you: it is day, not raining, and you can see the sky. */
    static boolean sunlit(boolean day, boolean raining, boolean seesSky) { return day && !raining && seesSky; }

    /** Radiant damage hurts a cursed vampire twice as much. */
    static final float RADIANT_MULT = 2.0f;

    /** Regeneration: every other second heals this much (a round of 20 health at the table is too much for a player); radiant harm pauses it for a round. */
    static final float REGEN_HEAL = 3.0f;
    static final int REGEN_HOLD = ROUND;

    /** Holy, radiant, sacred damage types (the path of the damage type id). */
    static boolean isRadiantPath(String path) {
        if (path == null) return false;
        return path.contains("holy") || path.contains("radiant") || path.contains("sacred") || path.contains("light_elemental");
    }

    // ================================================================ Romlo's Potion of Sleep
    /** "5 hours" at one minute an hour. */
    static final int SLEEP_TICKS = 5 * 1200, DROWSY_TICKS = 200;
    static final int SLEEP_DC = 15;
    static final double POUR_REACH = 4.0;
    /** A creature with this much health or more cannot be put to sleep (a boss). */
    static final double SLEEP_MAX_HEALTH = 100.0;

    /**
     * The Wisdom saving throw: d20 + the target's bonus against DC 15; a natural 20 always saves and a natural 1 never does. The bonus is one per ten
     * health (at most ten) plus up to three for a full pool of magicules.
     */
    static boolean saves(int d20, int bonus) {
        if (d20 >= 20) return true;
        if (d20 <= 1) return false;
        return d20 + bonus >= SLEEP_DC;
    }

    static int saveBonus(double maxHealth, double magiculeFraction) {
        int b = (int) Math.min(10, Math.floor(maxHealth / 10.0));
        return b + (int) Math.floor(Math.max(0, Math.min(1, magiculeFraction)) * 3.0);
    }

    // ================================================================ Spider Brew
    static final int SPIDER_HOUR = 6000;

    /** 1d4 hours. */
    static int spiderTicks(int d4) { return Math.max(1, Math.min(4, d4)) * SPIDER_HOUR; }

    /** The giant spider's body: 26 health to a player's 20, a tougher hide, a bite, a spring in the legs, a low long body (a scale below one). */
    static final double SPIDER_HEALTH = 6.0, SPIDER_ARMOR = 3.0, SPIDER_ATTACK = 2.0, SPIDER_JUMP = 0.3, SPIDER_SCALE = -0.15;
    /** A bare-mouthed bite: extra damage when the main hand is empty; poison on every melee hit. */
    static final float BITE = 3.0f;
    static final int POISON_TICKS = 120;
    /** Wall climbing speed (blocks per tick, vertical velocity). */
    static final double CLIMB = 0.2;

    /** Vertical velocity for a spider pressed against a wall: clinging still while sneaking, climbing otherwise; NaN when not touching a wall. */
    static double climb(boolean touchingWall, boolean sneaking) {
        if (!touchingWall) return Double.NaN;
        return sneaking ? 0.0 : CLIMB;
    }
}
