package com.newuniverse.nusmp.relics.wave;

import java.util.Collection;
import java.util.List;

/**
 * Why: the numbers that decide fairness (what a blow may do to a player, who counts as holding a set, when a wish is ready again) must be provable
 * without a running game, and several classes of the wave share them.
 * <p>
 * Wave 30: the pure rules of the special wave, with no Minecraft types so they are unit-tested: the set checks (the Hand and Eye of Vecna together,
 * the Infinity Gauntlet with its Stones), the dice the tabletop rules turn into (saving throws, the Paradox's madness table, the Vecna hunger and
 * whisper rolls), the damage guards (a player is never taken below a heart, a boss takes a smaller share) and the wish clocks.
 */
final class Wave30Rules {
    private Wave30Rules() {}

    // ================================================================ sets
    /** The pair bonus of Vecna works only while the Eye and the Hand both work for the bearer (worn and attuned). */
    static boolean vecnaSet(boolean eyeWorks, boolean handWorks) { return eyeWorks && handWorks; }

    /** How many of the six Stones are in this collection of relic ids (duplicates and other relics do not count). */
    static int stoneCount(Collection<String> carriedIds) {
        int n = 0;
        for (String s : Wave30Tune.STONES) if (carriedIds.contains(s)) n++;
        return n;
    }

    /** Does the collection hold all six Stones? */
    static boolean allStones(Collection<String> carriedIds) { return stoneCount(carriedIds) == Wave30Tune.STONES.length; }

    /** The Stones do nothing for the Gauntlet's bearer until it is worn: with it, each carried Stone adds this share to the Stones' acts (range, time, damage). */
    static double gauntletBoost(boolean gauntletWorn, int stones) {
        if (!gauntletWorn) return 1.0;
        return 1.0 + Wave30Tune.STONE_BOOST_EACH * Math.max(0, Math.min(6, stones));
    }

    /** The Greater Wish and the Snap need the Gauntlet worn and all six Stones carried. */
    static boolean snapReady(boolean gauntletWorn, int stones) { return gauntletWorn && stones >= Wave30Tune.STONES.length; }

    /** Index of the stone in the gauntlet's order, or -1. */
    static int stoneIndex(String relicId) {
        for (int i = 0; i < Wave30Tune.STONES.length; i++) if (Wave30Tune.STONES[i].equals(relicId)) return i;
        return -1;
    }

    // ================================================================ dice
    /** A saving throw: d20 + bonus against the DC; a natural 20 always holds, a natural 1 always fails. */
    static boolean saves(int d20, int bonus, int dc) {
        if (d20 >= 20) return true;
        if (d20 <= 1) return false;
        return d20 + bonus >= dc;
    }

    /** Vecna's Hunger: 5% on each use of the Eye. {@code roll} is in [0, 1). */
    static boolean hungerStrikes(double roll) { return roll < Wave30Tune.HUNGER_CHANCE; }

    /** Vecna's Whisper (coercion): 20% on each use of the Hand. */
    static boolean whispers(double roll) { return roll < Wave30Tune.WHISPER_CHANCE; }

    /** The madness tiers of the card's d100 table (0 to 100 inclusive; "00" prone, 01-49 short, 50-99 long, 100 indefinite). */
    enum Madness { PRONE, SHORT, LONG, INDEFINITE }

    static Madness madness(int roll) {
        if (roll <= 0) return Madness.PRONE;
        if (roll < 50) return Madness.SHORT;
        if (roll < 100) return Madness.LONG;
        return Madness.INDEFINITE;
    }

    /** Ticks of the madness effects: short is 1d10 x 10 s, long 5 minutes (the card's hours, shortened), indefinite 10 minutes (milk or a golden apple ends it), prone 3 s. */
    static int madnessTicks(Madness m, int d10) {
        return switch (m) {
            case PRONE -> 60;
            case SHORT -> Math.max(1, Math.min(10, d10)) * 200;
            case LONG -> 6000;
            case INDEFINITE -> 12000;
        };
    }

    /** The Paradox regains 2d8 + 4 charges at dawn, up to the maximum. */
    static int paradoxRegain(int d8a, int d8b) { return clampDie(d8a, 8) + clampDie(d8b, 8) + 4; }

    /** The Paradox adds 2d4 bludgeoning to a blow (the card's 2d10 is the tabletop's; a Minecraft blow is a fraction of it) and crits on 19 and 20. */
    static float paradoxExtra(int d4a, int d4b) { return clampDie(d4a, 4) + clampDie(d4b, 4); }

    static boolean paradoxCrit(int d20) { return d20 >= 19; }

    private static int clampDie(int v, int sides) { return Math.max(1, Math.min(sides, v)); }

    // ================================================================ damage guards
    /**
     * What a blow may do to a player: at most {@code capFraction} of maximum health, and never so much that fewer than {@code leave} health is left
     * (a player at or under {@code leave} takes nothing). Never negative.
     */
    static float playerHit(float amount, float maxHealth, float health, double capFraction, float leave) {
        float cap = (float) (maxHealth * capFraction);
        float room = Math.max(0f, health - leave);
        return Math.max(0f, Math.min(amount, Math.min(cap, room)));
    }

    /** The touch that melts a skeleton (and the Snap): a share of the foe's maximum health, less on a boss; a player is also capped and kept above a heart. */
    static float jelly(float maxHealth, float health, boolean player, boolean boss) {
        double f = boss ? Wave30Tune.JELLY_BOSS_FRACTION : Wave30Tune.JELLY_FRACTION;
        float raw = (float) (maxHealth * f);
        return player ? playerHit(raw, maxHealth, health, Wave30Tune.JELLY_PLAYER_CAP, Wave30Tune.LEAVE_HEALTH) : raw;
    }

    /** The Snap takes this share of a mob's current health (a boss only a tenth of its maximum): never the whole of it. */
    static float snap(float maxHealth, float health, boolean boss) {
        float raw = boss ? (float) (maxHealth * Wave30Tune.SNAP_BOSS_FRACTION) : (float) (health * Wave30Tune.SNAP_FRACTION);
        return Math.max(0f, Math.min(raw, Math.max(0f, health - 1f)));
    }

    /** A blow from a Deck or Hunger summon on a player: never lethal (leaves a heart), and at most 30% of their maximum health. */
    static float summonHit(float amount, float maxHealth, float health) { return playerHit(amount, maxHealth, health, 0.30, Wave30Tune.LEAVE_HEALTH); }

    // ================================================================ clocks
    /** Is the 30-day Wish ready? {@code lastMillis} is zero when the player has never wished. */
    static boolean wishReady(long lastMillis, long nowMillis) {
        return lastMillis <= 0L || nowMillis < lastMillis /* the clock was set back: do not lock the player out */ || nowMillis - lastMillis >= Wave30Tune.WISH_COOLDOWN_MILLIS;
    }

    /** Whole days left on the Wish clock (0 when ready). */
    static int wishDaysLeft(long lastMillis, long nowMillis) {
        if (wishReady(lastMillis, nowMillis)) return 0;
        long left = Wave30Tune.WISH_COOLDOWN_MILLIS - (nowMillis - lastMillis);
        return (int) Math.max(1, (left + 86_399_999L) / 86_400_000L);
    }

    /** Is the surprise guard (the first blow in 20 seconds is turned aside) ready? */
    static boolean surpriseReady(long now, long readyAt) { return now >= readyAt; }

    /** Is the Soul Stone's resurrection available today (one per game day, and it needs a stored soul)? */
    static boolean soulReviveOk(int souls, long lastDay, long today) { return souls > 0 && lastDay != today; }

    /** Health a resurrection leaves: half the maximum. */
    static float revivedHealth(float maxHealth) { return Math.max(1f, maxHealth * 0.5f); }

    /** The Reality wish's undo: heals back a blow taken within the window, never past the maximum. {@code ageTicks} is how long ago it landed. */
    static float undoAmount(float lastAmount, long ageTicks, float health, float maxHealth) {
        if (lastAmount <= 0f || ageTicks < 0 || ageTicks > Wave30Tune.UNDO_WINDOW) return 0f;
        return Math.max(0f, Math.min(lastAmount, maxHealth - health));
    }

    // ================================================================ the wish menus (cycled with a sneak-use)
    /** Vecna's Wish: safe choices only. */
    enum VecnaWish {
        FULL_RESTORE("Full Restore", "all health, food and a clean slate of harmful effects"),
        DAY_OF_RESILIENCE("A Day of Resilience", "Regeneration and Resistance for a whole day"),
        CLEAR_SKIES("Clear Skies", "the weather clears"),
        SUMMON_RAIN("Summon Rain", "rain falls");
        final String title, text;
        VecnaWish(String t, String x) { title = t; text = x; }
    }

    /** The Greater Wish of the Gauntlet: the printed list, bounded. */
    enum GreatWish {
        TREASURE("Treasure", "diamonds and emeralds appear at your feet"),
        RESTORATION("Restoration", "every friend near you is healed and cleansed"),
        RESISTANCE("Resistance", "every friend near you is warded against harm for ten minutes"),
        IMMUNITY("Immunity", "every friend near you is cleansed of harmful effects, each second, for a day"),
        REROLL("Reroll", "the blow you took a moment ago is undone");
        final String title, text;
        GreatWish(String t, String x) { title = t; text = x; }
    }

    static int nextIndex(int current, int size) { return size <= 0 ? 0 : Math.floorMod(current + 1, size); }

    /** The names of the Gauntlet's six Stone acts, in order (for tests and tooltips). */
    static List<String> stoneNames() { return List.of("Space", "Mind", "Power", "Time", "Reality", "Soul"); }
}
