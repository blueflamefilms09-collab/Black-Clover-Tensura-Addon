package com.newuniverse.nusmp.relics.wave;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Wave 3 (head, hands, belt and consumables): the numbers and small state machines behind the thirteen cards, kept free of Minecraft so they
 * can be tested. The glue ({@link Wave3}, {@link Wave3Util}, {@link Wave3Field}) only reads and writes the values these produce.
 * <ul>
 *   <li>{@link Motes}: the three arcane motes of the Arcanist's Circlet (spent one at a time, all back a minute after the last).</li>
 *   <li>{@link Daily}: the D&amp;D "once per long rest" as once per Minecraft day.</li>
 *   <li>{@link Pool}: the charges of the Dreamwalker Cowl and the Captain's Tricorn.</li>
 *   <li>{@link Element} / {@link Thermo}: the elements of the Hand Wraps and the two Thermo-Gloves.</li>
 *   <li>{@link Gauntlet}: the beams (and the backlash) of the Gauntlets of Eldritch Ferocity.</li>
 *   <li>{@link Godking}: the ten minutes of the Godking Crown and what ends them.</li>
 *   <li>{@link MarbleField}: one pour of Mad Marbles. {@link Slots} and {@link CharmBook}: bounded lists.</li>
 * </ul>
 */
final class Wave3Rules {
    private Wave3Rules() {}

    static final int DAY = 24000;

    // The named switches the cards carry (Passive.flag) and the handlers ask for. Every card of the wave also carries F_ANY, so a handler can leave
    // early for a player who wears none of them.
    static final String F_ANY = "w3_any", F_MOTES = "w3_motes", F_PRIDE = "w3_pride", F_DREAMER = "w3_dreamer", F_TRICORN = "w3_tricorn",
            F_GODKING = "w3_godking", F_CLAWS = "w3_claws", F_CLEAN = "w3_clean", F_WRAPS = "w3_wraps", F_THERMO = "w3_thermo",
            F_GAUNTLETS = "w3_gauntlets", F_ALCHEMIST = "w3_alchemist";

    /** The day number of a world clock reading (turns over at dawn, like the sun). */
    static long dayOf(long dayTime) { return Math.floorDiv(dayTime, (long) DAY); }

    static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }

    // ------------------------------------------------------------------ the Circlet's motes
    static final int MOTES = 3, MOTE_REFILL = 1200;
    /** One mote's force damage ("half your level" at the lowest levels; three of them are 9, under half a player's health). */
    static final float MOTE_DAMAGE = 3.0f;

    /**
     * How many motes still circle, and when the spent set comes back. Stored as two plain values; a player who never fired has (0, 0), which
     * reads as full because the refill time lies in the past.
     */
    record Motes(int left, long refillAt) {
        static final Motes FRESH = new Motes(MOTES, 0L);

        Motes at(long now) {
            if (left <= 0 && now >= refillAt) return FRESH;
            return new Motes(clamp(left, 0, MOTES), refillAt);
        }

        boolean canFire(long now) { return at(now).left() > 0; }

        /** One mote flies; when it was the last one, the clock for the whole set starts. */
        Motes fire(long now) {
            Motes m = at(now);
            if (m.left() <= 0) return m;
            int l = m.left() - 1;
            return new Motes(l, l == 0 ? now + MOTE_REFILL : m.refillAt());
        }

        /** Whole seconds until the motes are back (0 when they are not recharging). */
        int secondsToRefill(long now) {
            Motes m = at(now);
            return m.left() > 0 ? 0 : (int) Math.max(0, (m.refillAt() - now + 19) / 20);
        }
    }

    // ------------------------------------------------------------------ once per day
    /** What is stored for a daily use: 0 for never, else the day it was last used plus one. */
    static long dailyMark(long today) { return today + 1; }

    static boolean dailyReady(long stored, long today) { return stored == 0 || stored - 1 < today; }

    // ------------------------------------------------------------------ charges (Dreamwalker Cowl 3, Captain's Tricorn 8)
    static final int DREAM_MAX = 3, TRICORN_MAX = 8, TRICORN_DAWN = 2;

    /** A charge count is stored as -1 until first looked at; the pool then starts full. */
    static int normalize(int stored, int max) { return stored < 0 ? max : Math.min(stored, max); }

    static boolean canSpend(int have, int cost) { return cost > 0 && have >= cost; }

    static int spend(int have, int cost) { return Math.max(0, have - Math.max(0, cost)); }

    /** What the Tricorn gains at dawn: 2, or 1d6+2 when its wearer is aboard a vessel. {@code d6} is 1..6. */
    static int tricornDawn(boolean aboard, int d6) { return aboard ? clamp(d6, 1, 6) + 2 : TRICORN_DAWN; }

    /** The charges after the days that passed since the pool last recovered (at most one recovery, a clock set back owes nothing). */
    static int dawnRefill(int have, int max, long lastDay, long today, int gain) {
        if (today <= lastDay) return Math.min(have, max);
        return Math.min(max, have + Math.max(0, gain));
    }

    /** The Captain's Order costs 2 charges per target; how many targets {@code have} charges can buy, up to {@code cap}. */
    static int orderTargets(int have, int cap) { return clamp(have / 2, 0, cap); }

    // ------------------------------------------------------------------ the Hand Wraps' elements
    /** Off first: the card lets you "end this effect" with another bonus action. */
    enum Element {
        OFF("none"), FIRE("fire"), COLD("cold"), LIGHTNING("lightning"), THUNDER("thunder"), ACID("acid");

        final String label;

        Element(String label) { this.label = label; }

        Element next() { Element[] v = values(); return v[(ordinal() + 1) % v.length]; }

        static Element byOrdinal(int i) { Element[] v = values(); return i < 0 || i >= v.length ? OFF : v[i]; }

        boolean active() { return this != OFF; }
    }

    /** The extra damage of an elemental unarmed strike of the very rare wraps (the card's +3d6, as 3 points) and the Thermo-Gloves' +2. */
    static final float WRAPS_EXTRA = 3.0f, THERMO_EXTRA = 2.0f;

    /** The two modes of the Thermo-Gloves. */
    enum Thermo {
        FIRE("fire"), COLD("cold");

        final String label;

        Thermo(String label) { this.label = label; }

        Thermo next() { return this == FIRE ? COLD : FIRE; }

        static Thermo byOrdinal(int i) { return i == 1 ? COLD : FIRE; }
    }

    // ------------------------------------------------------------------ Gauntlets of Eldritch Ferocity
    /** The card's "treat a 1 as a 2": a d4 beam is never under 2. A single beam peaks at 4; each of twin beams at 3, so a cast never passes 6. */
    static final class Gauntlet {
        private Gauntlet() {}

        static final int BACKLASH_MAX_HP = 6, BACKLASH_TICKS = 12000, MIN_MAX_HP = 4;
        static final float BACKLASH_MAX = 6f;

        static float beam(int d4, boolean twin) {
            int rolled = Math.max(2, clamp(d4, 1, 4));
            return twin ? Math.min(3, rolled) : rolled;
        }

        /** An extra beam that rolls a 1 or 2 on its d20 turns on its caster. */
        static boolean backlash(int d20) { return d20 <= 2; }

        /** 3d12 of necrotic damage, scaled: three d4 halved (1.5 to 6), and never enough to kill. */
        static float backlashDamage(int d4a, int d4b, int d4c, float health) {
            float raw = (clamp(d4a, 1, 4) + clamp(d4b, 1, 4) + clamp(d4c, 1, 4)) * 0.5f;
            return Math.max(0f, Math.min(Math.min(raw, BACKLASH_MAX), health - 1f));
        }

        /** Hit points taken off the maximum "until you rest": the damage rounded, never below {@link #MIN_MAX_HP} left. */
        static float maxHealthPenalty(float damage, float baseMax) {
            float want = Math.min(BACKLASH_MAX_HP, Math.round(damage));
            return Math.max(0f, Math.min(want, baseMax - MIN_MAX_HP));
        }
    }

    // ------------------------------------------------------------------ Godking Crown
    /** Ten minutes of flight and +5 on everything, then the radiant reckoning (unless the crown came off first). */
    static final class Godking {
        private Godking() {}

        static final int ASCEND_TICKS = 12000, FALL_GRACE = 300, CLEAN_FALL_TICKS = 240;
        static final double BURST_RADIUS = 6.0;
        static final float BURST_OTHERS = 12f, BURST_SELF = 8f;

        /** 0 none; 1 one minute left; 2 thirty seconds; 3 ten seconds; 4 five seconds. */
        static int warnLevel(int remaining) {
            if (remaining <= 100) return 4;
            if (remaining <= 200) return 3;
            if (remaining <= 600) return 2;
            if (remaining <= 1200) return 1;
            return 0;
        }

        /** The warning to speak now, or -1 if the last one spoken still stands. */
        static int newWarning(int spoken, int remaining) {
            int w = warnLevel(remaining);
            return w > spoken ? w : -1;
        }

        /** The wearer's share of the burst: never so much that it kills. */
        static float selfBurst(float health) { return Math.max(0f, Math.min(BURST_SELF, health - 1f)); }

        static String warnText(int level) {
            return switch (level) {
                case 1 -> "one minute";
                case 2 -> "thirty seconds";
                case 3 -> "ten seconds";
                default -> "five seconds";
            };
        }
    }

    // ------------------------------------------------------------------ Mad Marbles
    /** What a creature stepping on the marbles gets. */
    enum Step { NONE, POP, SLIP }

    /**
     * One pour of marbles: a 3x3 patch of ground that lasts half a minute. The first creature onto it falls and the marbles go off in sparks
     * (it is blinded); the patch stays slippery for the others, who fall again at most every three seconds each.
     */
    static final class MarbleField {
        static final int LIFE = 600, SLIP_GAP = 60;
        static final double HALF = 1.5;

        final double cx, cy, cz;
        final long expireAt;
        final UUID owner;
        private boolean popped;
        private final Map<Integer, Long> nextSlip = new HashMap<>();

        MarbleField(double cx, double cy, double cz, long now, UUID owner) {
            this.cx = cx;
            this.cy = cy;
            this.cz = cz;
            this.expireAt = now + LIFE;
            this.owner = owner;
        }

        boolean expired(long now) { return now >= expireAt; }

        /** Is a creature standing on the patch (feet inside the 3x3 square, about the ground)? */
        boolean contains(double x, double y, double z) {
            return Math.abs(x - cx) <= HALF && Math.abs(z - cz) <= HALF && y >= cy - 0.6 && y <= cy + 1.0;
        }

        Step step(int entityId, long now) {
            if (expired(now)) return Step.NONE;
            Long gate = nextSlip.get(entityId);
            if (gate != null && now < gate) return Step.NONE;
            nextSlip.put(entityId, now + SLIP_GAP);
            if (nextSlip.size() > 32) nextSlip.keySet().removeIf(k -> nextSlip.get(k) < now);
            if (!popped) {
                popped = true;
                return Step.POP;
            }
            return Step.SLIP;
        }

        boolean popped() { return popped; }
    }

    /** A blinded time of "1d4 rounds", a round counted as two seconds. {@code d4} is 1..4. */
    static int blindTicks(int d4) { return clamp(d4, 1, 4) * 40; }

    // ------------------------------------------------------------------ bounded lists
    /** A list that never holds more than {@code cap} items: adding to a full one drops the oldest. */
    static final class Slots<T> {
        private final int cap;
        private final List<T> items = new ArrayList<>();

        Slots(int cap) { this.cap = Math.max(1, cap); }

        /** Returns the item pushed out, or null. */
        T add(T item) {
            items.add(item);
            return items.size() > cap ? items.remove(0) : null;
        }

        List<T> items() { return items; }

        int size() { return items.size(); }

        void clear() { items.clear(); }

        int count(java.util.function.Predicate<T> p) {
            int n = 0;
            for (T t : items) if (p.test(t)) n++;
            return n;
        }
    }

    /**
     * Creatures that have been charmed or calmed: they stop hunting the one who did it until a time runs out or that one strikes them. A
     * bounded book (at most {@code cap} entries, oldest out) so a crowd cannot make it grow.
     */
    static final class CharmBook {
        /** {@code calm}: ignores everyone's plea (calm emotions), not only the owner. */
        record Entry(UUID owner, long until, boolean calm) {}

        private final int cap;
        private final Map<UUID, Entry> map = new java.util.LinkedHashMap<>();

        CharmBook(int cap) { this.cap = Math.max(1, cap); }

        void add(UUID mob, UUID owner, long until, boolean calm) {
            map.remove(mob);
            map.put(mob, new Entry(owner, until, calm));
            while (map.size() > cap) {
                UUID first = map.keySet().iterator().next();
                map.remove(first);
            }
        }

        /** Should this mob leave this target alone? */
        boolean spares(UUID mob, UUID target, long now) {
            Entry e = map.get(mob);
            if (e == null || now >= e.until()) return false;
            return e.calm() || e.owner().equals(target);
        }

        /** The owner hit it: the charm is broken. Returns true if there was one. */
        boolean breakBy(UUID mob, UUID attacker) {
            Entry e = map.get(mob);
            if (e == null || !e.owner().equals(attacker)) return false;
            map.remove(mob);
            return true;
        }

        void purge(long now) { map.values().removeIf(e -> now >= e.until()); }

        /** Forget this creature (it died or was freed). */
        void remove(UUID mob) { map.remove(mob); }

        List<UUID> mobs() { return new ArrayList<>(map.keySet()); }

        Entry get(UUID mob) { return map.get(mob); }

        int size() { return map.size(); }

        void clear() { map.clear(); }
    }

    // ------------------------------------------------------------------ small tables
    /** The d20 of "Gloves of Instant Cleanliness": a d20 of snaps a day, played as ten (the median). */
    static final int CLEAN_USES = 10, STEAM_USES = 2;

    /** Duration of the Genie's wish: one Minecraft day, which is twenty real minutes. */
    static final int WISH_TICKS = 24000;

    /** The Hubris's Fall reflection: fear for five seconds. */
    static final int FEAR_TICKS = 100;

    /**
     * The saving throw, played as existence points (Tensura's measure of how much a creature is): a creature whose maximum EP is more than three
     * times the caster's shrugs off a word, a halt or a charm. A caster with no EP of its own (no Tensura data) is believed: nothing resists.
     */
    static final double SAVE_FACTOR = 3.0;

    static boolean resists(double casterEp, double targetEp) { return casterEp > 0 && targetEp > casterEp * SAVE_FACTOR; }

    /** Intimidation: the aura of Pride's Crown bites only creatures whose existence does not exceed the wearer's (or when the wearer's is unknown). */
    static boolean outranks(double casterEp, double targetEp) { return casterEp <= 0 || casterEp >= targetEp; }

    /** True if the given hit is the player's own fists: the main hand is empty. */
    static boolean unarmed(boolean mainHandEmpty) { return mainHandEmpty; }

    /** Undead Fortitude: a blow that would kill is cut to leave one health point, once per potion. */
    static float fortitudeCut(float incoming, float health, boolean spent) {
        if (spent || incoming < health) return incoming;
        return Math.max(0f, health - 1f);
    }
}
