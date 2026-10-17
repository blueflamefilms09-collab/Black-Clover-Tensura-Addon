package com.newuniverse.nusmp.labyrinth.mega.gear;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.gear.GearSpec.Ench;
import com.newuniverse.nusmp.labyrinth.mega.gear.GearTables.GradeTable;
import com.newuniverse.nusmp.labyrinth.mega.gear.GearTables.Pool;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Why: the heart of the standard-gear drops is two decisions, and both are plain arithmetic on a random stream, so both live here with no Minecraft type
 * in sight and are tested by the thousand. First, DOES this kill leave a piece and of what grade: each grade has a base chance (config) multiplied by a
 * curve over the depth of the floor (data), so that going deeper moves weight from Normal toward Special and Rare (Normal falls, Special peaks about
 * half-way, Rare keeps rising) while the total stays roughly where it was, capped per grade and in all; Looting adds an absolute chance to the total
 * and the grades share it in proportion, so Looting makes drops more frequent but never changes what they are. At most ONE piece per kill: the three
 * chances are laid end to end on a single roll. Second, WHAT is the piece: a slot by weight, a base item from that grade's list, a percentage bonus,
 * a durability bonus, a few enchantments and (for weapons) Tensura engravings drawn from the grade's pools, and a magic resistance. Everything draws
 * from a {@link Dice}, so a seeded generator replays any roll (the admin command's histogram, the self-tests and the statistical unit tests rely on
 * that), and the result is a {@link GearSpec}: a decision, not an item.
 */
public final class GearRoller {
    private GearRoller() {}

    /**
     * How far along the depth curves a floor is, 0..1: {@code floorShift} per floor above the first, so the default 0.02 reaches 1 at floor 51 (the shift is
     * complete after five sectors; deeper floors keep the end values). {@code floorShift} 0 turns the shift off. Floor 0 and the gaps between sectors count as floor 1.
     */
    public static double progress(GearRates r, int floor) {
        double x = Math.max(0.0, r.floorShift()) * (FloorMap.threat(floor) - 1);
        return Math.min(1.0, x);
    }

    /**
     * The chance per kill of each grade at this floor with this much Looting, indexed by {@link GearGrade#ordinal()}. A grade the tables do not offer has
     * chance 0. Each is base times the curve at {@link #progress}, held to the grade's own cap; Looting then adds {@code lootingBonus} per level to the total
     * (shared out in proportion); the total never exceeds the tables' total cap (the three are scaled down together) and no grade exceeds its cap.
     */
    public static double[] chances(GearRates r, GearTables t, int floor, int looting) {
        GearGrade[] grades = GearGrade.values();
        double[] w = new double[grades.length];
        double total = 0.0;
        double x = progress(r, floor);
        for (GearGrade g : grades) {
            double base = t.offers(g) ? Math.max(0.0, r.base(g)) : 0.0;
            double v = Math.min(base * t.curve(g).at(x), t.maxChance(g));
            w[g.ordinal()] = Math.max(0.0, v);
            total += w[g.ordinal()];
        }
        if (total <= 0.0) return w;
        double boosted = Math.min(total + Math.max(0, looting) * Math.max(0.0, r.lootingBonus()), t.totalCap());
        double scale = boosted / total;
        double[] p = new double[grades.length];
        for (GearGrade g : grades) p[g.ordinal()] = Math.min(w[g.ordinal()] * scale, t.maxChance(g));
        return p;
    }

    /** The sum of the chances: how often a kill at this floor drops anything at all. */
    public static double total(double[] chances) {
        double s = 0.0;
        for (double c : chances) s += c;
        return s;
    }

    /** One roll laid over the three chances end to end: the grade that drops, or null when nothing does (the usual case). Always consumes exactly one draw. */
    public static GearGrade pickGrade(Dice d, double[] chances) {
        double u = d.next();
        double edge = 0.0;
        GearGrade[] grades = GearGrade.values();
        for (GearGrade g : grades) {
            edge += chances[g.ordinal()];
            if (u < edge) return g;
        }
        return null;
    }

    /** Does a kill at this floor leave a piece, and which: the grade decision followed by {@link #make}. */
    public static Optional<GearSpec> roll(Dice d, int floor, int looting, GearTables t, GearRates r) {
        GearGrade g = pickGrade(d, chances(r, t, floor, looting));
        return g == null ? Optional.empty() : make(d, g, null, t);
    }

    /**
     * A piece of this grade: the given slot, or (null) one drawn by the slot weights among the slots the grade offers. Empty when the grade offers nothing
     * or does not offer the forced slot. The same random stream gives the same piece.
     */
    public static Optional<GearSpec> make(Dice d, GearGrade grade, GearSlot forced, GearTables t) {
        GradeTable gt = t.grade(grade);
        if (gt == null) return Optional.empty();
        GearSlot slot;
        if (forced != null) slot = gt.offers(forced) ? forced : null;
        else slot = pickSlot(d, gt, t);
        if (slot == null) return Optional.empty();
        List<String> items = gt.items(slot);
        String item = items.get(d.nextInt(items.size()));
        int power = slot.hasPowerStat() ? gt.power().pick(d) : 0;
        int durability = gt.durability().pick(d);
        List<Ench> enchants = draw(d, gt.enchantCount(), gt.enchantPool(slot), gt.enchantLevelCap());
        List<Ench> engravings = slot.engraves() ? draw(d, gt.engraveCount(), gt.engravePool(slot), Integer.MAX_VALUE) : List.of();
        int resist = gt.resist(slot).pick(d);
        return Optional.of(new GearSpec(grade, slot, item, power, durability, enchants, engravings, resist));
    }

    /** A slot by weight among those the grade offers; uniform among them when every weight is zero; null when it offers none. */
    static GearSlot pickSlot(Dice d, GradeTable gt, GearTables t) {
        GearSlot[] slots = GearSlot.values();
        double sum = 0.0;
        int offered = 0;
        for (GearSlot s : slots) {
            if (!gt.offers(s)) continue;
            offered++;
            sum += Math.max(0.0, t.slotWeight(s));
        }
        if (offered == 0) return null;
        if (sum <= 0.0) {
            int k = d.nextInt(offered);
            for (GearSlot s : slots) if (gt.offers(s) && k-- == 0) return s;
            return null;
        }
        double u = d.next() * sum, edge = 0.0;
        GearSlot last = null;
        for (GearSlot s : slots) {
            if (!gt.offers(s)) continue;
            double w = Math.max(0.0, t.slotWeight(s));
            if (w <= 0.0) continue;
            last = s;
            edge += w;
            if (u < edge) return s;
        }
        return last;
    }

    /** Distinct entries from a pool: how many comes from the range (never more than the pool holds), each at a level in its own range held to {@code levelCap}. */
    static List<Ench> draw(Dice d, GearTables.IntRange count, List<Pool> pool, int levelCap) {
        if (pool.isEmpty() || count.max() <= 0) return List.of();
        int n = Math.min(count.pick(d), pool.size());
        if (n <= 0) return List.of();
        List<Pool> left = new ArrayList<>(pool);
        List<Ench> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Pool p = left.remove(d.nextInt(left.size()));
            int hi = Math.max(p.min(), Math.min(p.max(), levelCap));
            int level = p.min() + d.nextInt(hi - p.min() + 1);
            out.add(new Ench(p.id(), Math.max(1, Math.min(level, levelCap))));
        }
        return out;
    }
}
