package com.newuniverse.nusmp.labyrinth.loot;

import com.newuniverse.nusmp.dungeon.sentient.Dice;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: whether a kill leaves a Goblin Ear, a stew or a rare sword is a decision with five inputs (the monster's family, the depth of the floor, whether it was a boss, the
 * killer's luck, the server's rates) and a few rules, and every rule is something an owner will want to rely on: AT MOST {@code maxPerKill} drops from one kill (so a horde is
 * not a jackpot; a boss kill may leave {@code maxPerKill + 3}, because its guaranteed rows are three more than the ordinary rolls), NEVER the same item twice, at most ONE piece of gear and ONE food per kill, only items of the monster's own family (and the generic ones), deeper floors a
 * little richer and a luckier killer a little richer still, and a boss ALWAYS leaves rare materials, a piece of gear (one tier better) and a strong tonic. So the decision is one
 * pure function of a {@link Dice}, and the game side only builds the input and turns the answer into stacks. Hunger is not a concept here: foods are items, and the items are
 * restoratives.
 */
public final class KillLoot {
    private KillLoot() {}

    /** What the kill was: the monster's family, the floor's depth, a boss or not, the killer's luck (0 for none). */
    public record Input(String family, int depth, boolean boss, double luck) {}

    /** The server's rates: the multiplier of all drops, of gear and of foods; the cap per kill; and how much more a boss's ordinary rows are worth. */
    public record Rates(double drop, double gear, double food, int maxPerKill, double boss) {
        public static final Rates DEFAULT = new Rates(1.0, 1.0, 1.0, 3, 3.0);
    }

    /** One drop: the item, what it is, how many, and for gear the rarity and the seed its stats are rolled from. */
    public record Drop(String id, LootTables.Kind kind, int count, LootRarity rarity, long seed) {}

    /** The hard ceiling on one row's chance, whatever the multipliers say. */
    public static final double MAX_CHANCE = 0.5;

    public static List<Drop> roll(Dice dice, Input in, LootTables tables, Rates rates) {
        int band = LootBand.of(in.depth());
        int cap = Math.max(0, rates.maxPerKill());
        List<Drop> out = new ArrayList<>();
        Set<String> used = new HashSet<>();
        boolean[] got = new boolean[LootTables.Kind.values().length];
        double luck = Math.max(0.0, Math.min(10.0, in.luck()));
        double boost = (1.0 + 0.08 * band) * (1.0 + 0.08 * luck) * (in.boss() ? Math.max(1.0, rates.boss()) : 1.0);

        if (in.boss() && cap > 0) bossDrops(dice, in, tables, band, luck, out, used, got);

        List<LootTables.Entry> pool = new ArrayList<>();
        for (LootTables.Entry e : tables.entries(in.family())) if (e.minBand() <= band) pool.add(e);
        for (int i = pool.size() - 1; i > 0; i--) {                              // shuffled, so no row is favoured by its place in the list
            int j = dice.nextInt(i + 1);
            LootTables.Entry tmp = pool.get(i);
            pool.set(i, pool.get(j));
            pool.set(j, tmp);
        }
        int limit = cap + (in.boss() ? 3 : 0);
        for (LootTables.Entry e : pool) {
            if (out.size() >= limit) break;
            if (used.contains(e.id()) || (e.kind() != LootTables.Kind.MATERIAL && got[e.kind().ordinal()])) continue;
            double kindRate = e.kind() == LootTables.Kind.GEAR ? rates.gear() : e.kind() == LootTables.Kind.FOOD ? rates.food() : 1.0;
            double p = Math.min(MAX_CHANCE, e.chance() * Math.max(0.0, rates.drop()) * Math.max(0.0, kindRate) * boost);
            if (dice.next() >= p) continue;
            out.add(make(dice, e.id(), e.kind(), band, luck, 0));
            used.add(e.id());
            got[e.kind().ordinal()] = true;
        }
        return out;
    }

    /** A boss always leaves two rare materials, a piece of gear a tier better than a common kill's, and a strong tonic or elixir. */
    private static void bossDrops(Dice dice, Input in, LootTables t, int band, double luck, List<Drop> out, Set<String> used, boolean[] got) {
        List<String> mats = new ArrayList<>(t.bossMaterialsFor(in.family()));
        for (int n = 0; n < 2 && !mats.isEmpty(); n++) {
            String id = mats.remove(n == 0 ? 0 : dice.nextInt(mats.size()));
            out.add(make(dice, id, LootTables.Kind.MATERIAL, band, luck, 1));
            used.add(id);
        }
        List<String> gear = t.gearFor(in.family());
        if (!gear.isEmpty()) {
            String id = gear.get(dice.nextInt(gear.size()));
            out.add(make(dice, id, LootTables.Kind.GEAR, band, luck + 1.0, 1));
            used.add(id);
            got[LootTables.Kind.GEAR.ordinal()] = true;
        }
        if (!t.bossFoods().isEmpty()) {
            String id = t.bossFoods().get(dice.nextInt(t.bossFoods().size()));
            out.add(make(dice, id, LootTables.Kind.FOOD, band, luck, 0));
            used.add(id);
            got[LootTables.Kind.FOOD.ordinal()] = true;
        }
    }

    private static Drop make(Dice dice, String id, LootTables.Kind kind, int band, double luck, int bandBonus) {
        if (kind == LootTables.Kind.GEAR) {
            LootRarity r = LootRarity.pick(dice.next(), band + bandBonus, luck);
            long seed = ((long) (dice.next() * (1L << 31)) << 31) ^ (long) (dice.next() * (1L << 31));
            return new Drop(id, kind, 1, r, seed);
        }
        int count = 1;
        if (kind == LootTables.Kind.MATERIAL) {
            if (band >= 2 && dice.next() < 0.35) count++;
            if (band >= 4 && dice.next() < 0.25) count++;
        }
        return new Drop(id, kind, count, null, 0L);
    }
}
