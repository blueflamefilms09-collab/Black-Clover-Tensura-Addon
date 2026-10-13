package com.newuniverse.nusmp.relics.wave;

/**
 * Wave 1 of the Relics of Many Worlds: the pure rules (no Minecraft imports) that the hand-held staves, wands, swords and tools
 * share: which spell is selected, per-spell cooldowns, "uses per day", and the charge pools that refill at dawn. Every number
 * lives in a {@link Bag}, which the game side backs with the stack's custom data, so a relic keeps its state in its own item.
 * <p>
 * Tabletop becomes Minecraft here: a "long rest" is a Minecraft day (24000 ticks), "dawn" is the day counter turning over, and
 * a die is rolled from the day number so that reading a tooltip and casting never disagree and nothing needs a random source.
 */
public final class Wave1Rules {
    private Wave1Rules() {}

    public static final long DAY = 24000L;

    /** A long-valued key/value store; an absent key reads as 0. */
    public interface Bag {
        long get(String key);

        void put(String key, long value);
    }

    /** A charge pool: {@code max} charges, and at each dawn it regains {@code base + 1d(die)}; die 0 means a full refill. */
    public record Pool(String key, int max, int base, int die) {}

    /** True for damage a spell deals (vanilla magic, Tensura's elemental and holy types): what Astralis' Arcana Astra strengthens. */
    public static boolean isSpellPath(String damageTypePath) {
        if (damageTypePath == null) return false;
        return damageTypePath.contains("magic") || damageTypePath.endsWith("_elemental") || damageTypePath.equals("holy_damage")
                || damageTypePath.equals("curse");
    }

    // ---------------------------------------------------------------- the selected spell (sneak + use cycles it)
    public static int selected(Bag b, int count) {
        return count <= 0 ? 0 : (int) Math.floorMod(b.get("sel"), (long) count);
    }

    public static int cycle(Bag b, int count) {
        int next = count <= 0 ? 0 : (selected(b, count) + 1) % count;
        b.put("sel", next);
        return next;
    }

    // ---------------------------------------------------------------- per-spell cooldowns
    public static boolean ready(Bag b, String key, long now) {
        return now >= b.get("cd_" + key);
    }

    public static int remaining(Bag b, String key, long now) {
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, b.get("cd_" + key) - now));
    }

    public static void startCooldown(Bag b, String key, long now, int ticks) {
        b.put("cd_" + key, now + Math.max(0, ticks));
    }

    // ---------------------------------------------------------------- uses per day ("per long rest")
    public static int usesLeft(Bag b, String key, long now, int perDay) {
        long day = now / DAY;
        if (b.get("ud_" + key) != day + 1) return perDay;
        return (int) Math.max(0L, perDay - b.get("un_" + key));
    }

    public static boolean takeUse(Bag b, String key, long now, int perDay) {
        if (usesLeft(b, key, now, perDay) <= 0) return false;
        long day = now / DAY;
        if (b.get("ud_" + key) != day + 1) {
            b.put("ud_" + key, day + 1);
            b.put("un_" + key, 1);
        } else {
            b.put("un_" + key, b.get("un_" + key) + 1);
        }
        return true;
    }

    // ---------------------------------------------------------------- charge pools
    /** A deterministic die from a seed: 1..sides. */
    public static int roll(long seed, int sides) {
        if (sides <= 1) return 1;
        long h = seed * 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return 1 + (int) Math.floorMod(h, (long) sides);
    }

    /** What the pool regains at the dawn that opens {@code day} (the pool's size for a full refill). */
    public static int dawnGain(Pool p, long day) {
        if (p.die() <= 0) return p.max();
        return p.base() + roll(day * 31L + p.key().hashCode(), p.die());
    }

    /** The charges the pool holds at {@code now}: a new pool starts full, and every dawn since the last look refills it. */
    public static int charges(Bag b, long now, Pool p) {
        String k = p.key();
        long day = now / DAY;
        long seen = b.get(k + "_d");
        if (seen == 0) {
            b.put(k, p.max());
            b.put(k + "_d", day + 1);
            return p.max();
        }
        long last = seen - 1;
        if (day > last) {
            long cur = b.get(k);
            for (long d = Math.max(last + 1, day - 6); d <= day; d++) {       // at most a week of dawns is replayed
                if (b.get(k + "_dim") == 1) {
                    b.put(k + "_dim", 0);                                   // a guttered staff skips one dawn
                    continue;
                }
                cur = Math.min(p.max(), cur + dawnGain(p, d));
            }
            b.put(k, cur);
            b.put(k + "_d", day + 1);
        }
        return (int) Math.max(0L, Math.min(p.max(), b.get(k)));
    }

    public static boolean dimmed(Bag b, Pool p) {
        return b.get(p.key() + "_dim") == 1;
    }

    public static void dim(Bag b, Pool p) {
        b.put(p.key() + "_dim", 1);
    }

    /** Spends {@code cost} charges if the pool holds them (and is not guttered); true if they were spent. */
    public static boolean spend(Bag b, long now, Pool p, int cost) {
        int have = charges(b, now, p);
        if (cost <= 0 || have < cost) return false;
        b.put(p.key(), have - cost);
        return true;
    }

    /** True when the pool was just emptied by a spend (the Staff of Moonlight may gutter on its last charge). */
    public static boolean empty(Bag b, long now, Pool p) {
        return charges(b, now, p) <= 0;
    }
}
