package com.newuniverse.nusmp.labyrinth.loot;

import com.newuniverse.nusmp.relics.GradeDamage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SplittableRandom;

/**
 * Why: a drop must be PRE-ROLLED, which means the stats are decided once, at the moment the piece is made, and are the same forever after; and a roll must be reproducible,
 * because a test, an admin command and a bug report all need "the piece that this seed makes". So the roll is one pure function of (seed, floor, rarity, kind of piece, the
 * piece's primary stat, the headroom of its damage range) that draws from a seeded generator and nothing else: the line count from the rarity (two to four), the primary stat
 * first (a weapon's attack damage, a body piece's armour, a trinket's own stat), the others distinct and drawn from the kinds the piece may roll, each value inside the range
 * {@link StatKind#range} gives for the rarity and the depth, rounded to the stat's step and never over its cap. A weapon's attack damage is a share of the HEADROOM between its
 * base damage and the top of its grade's range ({@code relics/GradeDamage}), so a rolled weapon is always inside the range of its grade: the roll can make it better than
 * another of its tier, never a different tier. Hunger is nowhere in the kinds; nothing a roll can produce touches the food bar.
 */
public final class GearRollMath {
    private GearRollMath() {}

    /** Headroom below which a weapon rolls no attack damage line (a greatsword already sits at the top of its grade). */
    public static final double MIN_HEADROOM = 1.0;

    // ---------------------------------------------------------------- weapon damage
    /** The base damage of a weapon of this type at this rarity: the owner's grade table, placed by the weapon type. */
    public static double baseDamage(LootRarity rarity, String type) { return GradeDamage.damage(rarity.grade(), type); }

    /** How far the damage can still rise inside the grade's range. */
    public static double headroom(LootRarity rarity, String type) { return Math.max(0.0, GradeDamage.range(rarity.grade())[1] - baseDamage(rarity, type)); }

    /** The weapon's total attack damage with its rolled line: base plus the attack damage line, never above the top of the grade's range. */
    public static double totalDamage(LootRarity rarity, String type, GearRoll roll) {
        double top = GradeDamage.range(rarity.grade())[1];
        return Math.min(top, baseDamage(rarity, type) + (roll == null ? 0.0 : roll.sum(StatKind.ATTACK_DAMAGE)));
    }

    /** The primary stat a weapon with this headroom rolls first: attack damage, or its attack speed when its damage is already at the top of the range. */
    public static StatKind weaponPrimary(double headroom) { return headroom >= MIN_HEADROOM ? StatKind.ATTACK_DAMAGE : StatKind.ATTACK_SPEED; }

    // ---------------------------------------------------------------- the roll
    /**
     * Rolls a piece. {@code primary} (null for none) is always the first line when the kind of piece may roll it; {@code headroom} is the weapon's damage headroom (0 for
     * armour and trinkets). Deterministic: the same arguments give the same roll.
     */
    public static GearRoll roll(long seed, int depth, LootRarity rarity, GearKind kind, StatKind primary, double headroom) {
        SplittableRandom r = new SplittableRandom(seed);
        int n = rarity.minLines() + (rarity.maxLines() > rarity.minLines() ? r.nextInt(rarity.maxLines() - rarity.minLines() + 1) : 0);
        List<StatKind> pool = eligible(kind, headroom);
        List<GearRoll.Line> lines = new ArrayList<>(n);
        if (primary != null && pool.contains(primary)) {
            lines.add(new GearRoll.Line(primary, value(r, primary, rarity, depth, headroom)));
            pool.remove(primary);
        }
        while (lines.size() < n && !pool.isEmpty()) {
            StatKind k = pool.remove(r.nextInt(pool.size()));
            lines.add(new GearRoll.Line(k, value(r, k, rarity, depth, headroom)));
        }
        return new GearRoll(rarity, seed, depth, lines, false);
    }

    /** The stat kinds a piece of this kind may roll, in a stable order. */
    public static List<StatKind> eligible(GearKind kind, double headroom) {
        List<StatKind> pool = new ArrayList<>();
        for (StatKind k : StatKind.values()) {
            if (!k.allowedOn(kind)) continue;
            if (k == StatKind.ATTACK_DAMAGE && headroom < MIN_HEADROOM) continue;
            pool.add(k);
        }
        return pool;
    }

    private static double value(SplittableRandom r, StatKind k, LootRarity rarity, int depth, double headroom) {
        double[] range = k.range(rarity, depth);
        double u = range[0] + r.nextDouble() * (range[1] - range[0]);
        double v = k == StatKind.ATTACK_DAMAGE ? u * headroom : u;
        double cap = k == StatKind.ATTACK_DAMAGE ? Math.max(k.step(), headroom * k.cap()) : k.cap();
        return Math.min(cap, Math.max(k.step(), k.quantize(v)));
    }

    /**
     * The extra line a Shadow Core Shard works into a piece at an anvil: a kind the piece does not have yet when there is one, at the middle of its range for the piece's rarity
     * and depth. Deterministic from the roll's own seed.
     */
    public static GearRoll.Line temperLine(GearRoll roll, GearKind kind, double headroom) {
        SplittableRandom r = new SplittableRandom(roll.seed() ^ 0x5DEECE66DL);
        List<StatKind> pool = eligible(kind, headroom);
        List<StatKind> fresh = new ArrayList<>();
        for (StatKind k : pool) if (!roll.has(k)) fresh.add(k);
        StatKind pick = fresh.isEmpty() ? pool.get(r.nextInt(pool.size())) : fresh.get(r.nextInt(fresh.size()));
        double[] range = pick.range(roll.rarity(), roll.depth());
        double mid = (range[0] + range[1]) / 2.0;
        double v = pick == StatKind.ATTACK_DAMAGE ? mid * headroom : mid;
        return new GearRoll.Line(pick, Math.max(pick.step(), Math.min(pick == StatKind.ATTACK_DAMAGE ? headroom * pick.cap() : pick.cap(), pick.quantize(v))));
    }

    // ---------------------------------------------------------------- money and words
    /** What a piece sells for: its base value times the tier's factor, a little more for a deep floor. */
    public static long sellValue(int baseValue, LootRarity rarity, int depth) {
        return Math.max(1L, Math.round(baseValue * rarity.sellFactor() * (1.0 + 0.004 * Math.min(Math.max(0, depth), 500))));
    }

    /** The number as the tooltip prints it: the percent kinds times 100, everything trimmed of trailing zeros. */
    public static String format(StatKind kind, double value) {
        boolean percent = kind.op() != StatKind.Op.ADD;
        double v = percent ? value * 100.0 : value;
        String s = String.format(Locale.ROOT, "%.2f", v);
        if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }
}
