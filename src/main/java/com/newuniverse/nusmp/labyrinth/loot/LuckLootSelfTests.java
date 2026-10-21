package com.newuniverse.nusmp.labyrinth.loot;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.elite.hub.CoinLoot;
import com.newuniverse.nusmp.elite.hub.CoinLootModifier;
import com.newuniverse.nusmp.elite.hub.Currency;
import com.newuniverse.nusmp.elite.hub.HubConfig;
import com.newuniverse.nusmp.labyrinth.loot.backpack.BackpackItem;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * Why: the Luck-scaled loot cannot be played by its author, so the CI server smoke test runs it in the real game, through the REAL pipeline (a loot table rolled with a chest's
 * parameters, so every global loot modifier of the game acts on it, ours among them): {@code dungeon.loot_luck} proves that the modifier type is registered and switched on, that a
 * made-up chest of four rotten flesh yields about four with no Luck and clearly more with a lot of it (never past a stack, never fewer), and that Luck makes a backpack show up
 * more often; {@code dungeon.loot_luck.coins} proves that the Tensura coin payout with no Luck is bit-for-bit the old roll and rises with Luck (amount and chance), through the pure
 * roll and through the coin modifier itself; {@code dungeon.loot_luck.armour} proves that a lucky deep chest or boss yields randomised dungeon armour that carries a real roll
 * (lines, rarity, depth), that two rolls differ, and that the rarity follows the Luck. Nothing here touches the food bar.
 */
final class LuckLootSelfTests {
    private LuckLootSelfTests() {}

    static void register() {
        SelfTests.register("dungeon.loot_luck", LuckLootSelfTests::counts);
        SelfTests.register("dungeon.loot_luck.coins", LuckLootSelfTests::coins);
        SelfTests.register("dungeon.loot_luck.armour", LuckLootSelfTests::armour);
    }

    private static SelfTests.Result fail(String detail) { return SelfTests.Result.fail(detail); }

    private static final ResourceLocation TABLE_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "chests/selftest_luck_loot");

    /** A chest table that is in no registry: four rotten flesh per roll, the chest parameter set, and an id of its own (NeoForge hands the id to the modifiers). */
    private static LootTable madeUp() {
        LootTable table = LootTable.lootTable().setParamSet(LootContextParamSets.CHEST)
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1f)).add(LootItem.lootTableItem(Items.ROTTEN_FLESH).apply(SetItemCountFunction.setCount(ConstantValue.exactly(4f))))).build();
        table.setLootTableId(TABLE_ID);
        return table;
    }

    private static LootParams chest(ServerLevel level, BlockPos at, float luck, FakePlayer opener) {
        LootParams.Builder b = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(at)).withLuck(luck);
        if (opener != null) b.withParameter(LootContextParams.THIS_ENTITY, opener);
        return b.create(LootContextParamSets.CHEST);
    }

    // ---------------------------------------------------------------- counts and backpacks
    private static SelfTests.Result counts(MinecraftServer server) {
        if (!LuckLootModifier.TYPE.isBound()) return fail("the modifier type nusmp:luck_loot is not registered");
        try {
            var res = server.getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("neoforge", "loot_modifiers/global_loot_modifiers.json"));
            if (res.isPresent()) {
                String text = new String(res.get().open().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                if (!text.contains("nusmp:luck_loot")) return fail("global_loot_modifiers.json does not list nusmp:luck_loot");
            }
        } catch (java.io.IOException | RuntimeException ex) {
            return fail("could not read the global loot modifier list: " + ex.getMessage());
        }
        if (LuckGauss.extra(0.0, 0, () -> 0.5, LuckGauss.ITEMS) != 0.0) return fail("no Luck and no depth must give no extra");
        ServerLevel mega = SelfTests.mega(server);
        if (mega == null) return fail("no mega dimension");
        BlockPos at = new BlockPos(0, 70, 0);
        FakePlayer opener = SelfTests.player(mega, "luck_opener", 0.5, 70, 0.5);
        LootTable table = madeUp();
        int rolls = 1500;
        double[] sums = new double[2];
        int[] backpacks = new int[2], maxCount = new int[2], minCount = {Integer.MAX_VALUE, Integer.MAX_VALUE};
        float[] lucks = {0f, 15f};
        RandomSource random = RandomSource.create(20260107L);
        for (int k = 0; k < 2; k++) {
            opener.getAttribute(Attributes.LUCK).setBaseValue(lucks[k]);
            for (int i = 0; i < rolls; i++) {
                int flesh = 0;
                for (ItemStack s : table.getRandomItems(chest(mega, at, lucks[k], opener), random)) {
                    if (s.is(Items.ROTTEN_FLESH)) flesh += s.getCount();
                    if (s.getItem() instanceof BackpackItem) backpacks[k]++;
                }
                if (flesh == 0) return fail("the four rotten flesh of the table vanished (luck " + lucks[k] + ")");
                sums[k] += flesh;
                maxCount[k] = Math.max(maxCount[k], flesh);
                minCount[k] = Math.min(minCount[k], flesh);
            }
        }
        opener.getAttribute(Attributes.LUCK).setBaseValue(0.0);
        double plain = sums[0] / rolls, lucky = sums[1] / rolls;
        if (minCount[0] < 4 || minCount[1] < 4) return fail("a count fell below the table's four (min " + minCount[0] + " / " + minCount[1] + "): Luck must only add");
        if (maxCount[1] > Items.ROTTEN_FLESH.getDefaultMaxStackSize()) return fail("a count went past a stack: " + maxCount[1]);
        double depthMean = LuckGauss.mean(0.0, com.newuniverse.nusmp.labyrinth.mega.MegaDungeon.floorAt(mega, at), LootConfig.luckItems());
        if (plain > 4.0 * (1.0 + 2.0 * depthMean) + 0.15) return fail("with no Luck the average count is " + String.format(java.util.Locale.ROOT, "%.2f", plain) + ", the table says 4 (depth term " + String.format(java.util.Locale.ROOT, "%.3f", depthMean) + ")");
        if (lucky < plain + 1.0) return fail("with Luck 15 the average count is only " + String.format(java.util.Locale.ROOT, "%.2f", lucky) + " (no Luck: " + String.format(java.util.Locale.ROOT, "%.2f", plain) + ")");
        if (backpacks[1] <= backpacks[0]) return fail("Luck did not raise the backpack drops: " + backpacks[0] + " without, " + backpacks[1] + " with Luck 15");
        return SelfTests.Result.ok(String.format(java.util.Locale.ROOT, "chest count %.2f -> %.2f with Luck 15 (max %d); backpacks %d -> %d in %d chests", plain, lucky, maxCount[1], backpacks[0], backpacks[1], rolls));
    }

    // ---------------------------------------------------------------- coins
    private static SelfTests.Result coins(MinecraftServer server) {
        CoinLoot.Params p = HubConfig.lootParams();
        int n = 4000;
        // 1. with no Luck the new roll is the old roll, draw for draw
        SplittableRandom a = new SplittableRandom(7), b = new SplittableRandom(7);
        for (CoinLoot.Source src : CoinLoot.Source.values()) {
            for (int i = 0; i < 300; i++) {
                long old = CoinLoot.roll(src, 5, 2, p, a::nextDouble);
                long now = CoinLoot.roll(src, 5, 2, 0.0, LuckGauss.COINS, p, b::nextDouble);
                if (old != now) return fail("with no Luck the " + src + " payout changed: " + old + " vs " + now);
            }
        }
        // 2. Luck raises the amount and the chance of a mob paying
        double sum0 = 0, sum15 = 0;
        int paid0 = 0, paid15 = 0;
        SplittableRandom r0 = new SplittableRandom(11), r15 = new SplittableRandom(11);
        for (int i = 0; i < n; i++) {
            long v0 = CoinLoot.roll(CoinLoot.Source.MOB, 5, 0, 0.0, LuckGauss.COINS, p, r0::nextDouble);
            long v15 = CoinLoot.roll(CoinLoot.Source.MOB, 5, 0, 15.0, LuckGauss.COINS, p, r15::nextDouble);
            sum0 += v0;
            sum15 += v15;
            if (v0 > 0) paid0++;
            if (v15 > 0) paid15++;
        }
        if (paid15 <= paid0) return fail("Luck 15 did not raise the chance that a mob pays: " + paid0 + " vs " + paid15 + " of " + n);
        if (sum15 < sum0 * 1.3) return fail("Luck 15 did not raise the coins: " + (long) sum0 + " vs " + (long) sum15);
        // 3. through the coin modifier itself: a chest in the Mega-Dungeon
        String via = "skipped (the currency is not the Tensura coins)";
        ServerLevel mega = SelfTests.mega(server);
        if (mega != null && Currency.coins()) {
            BlockPos at = new BlockPos(0, 70, 0);
            FakePlayer opener = SelfTests.player(mega, "luck_payer", 0.5, 70, 0.5);
            long[] total = new long[2];
            float[] lucks = {0f, 15f};
            for (int k = 0; k < 2; k++) {
                SplittableRandom rnd = new SplittableRandom(99);
                Dice dice = rnd::nextDouble;
                for (int i = 0; i < 400; i++) {
                    LootContext ctx = new LootContext.Builder(chest(mega, at, lucks[k], opener)).withQueriedLootTableId(TABLE_ID).create(Optional.empty());
                    total[k] += CoinLootModifier.add(new ObjectArrayList<>(), ctx, p, dice);
                }
            }
            if (total[0] > 0 && total[1] < total[0] * 1.3) return fail("the coin modifier did not scale with Luck: " + total[0] + " vs " + total[1] + " bronze in 400 chests");
            via = "chest payout " + total[0] + " -> " + total[1] + " bronze";
        }
        return SelfTests.Result.ok(String.format(java.util.Locale.ROOT, "no-Luck roll unchanged; a mob pays %d -> %d of %d, %d -> %d bronze; %s", paid0, paid15, n, (long) sum0, (long) sum15, via));
    }

    // ---------------------------------------------------------------- armour
    private static SelfTests.Result armour(MinecraftServer server) {
        ServerLevel mega = SelfTests.mega(server);
        if (mega == null) return fail("no mega dimension");
        RandomSource random = RandomSource.create(5150L);
        Dice dice = random::nextDouble;
        int found = 0, rolledLines = 0;
        int[] hits = new int[2];
        Set<String> distinct = new HashSet<>();
        double[] rarity = new double[2];
        for (int k = 0; k < 2; k++) {
            double luck = k == 0 ? 0.0 : 20.0;
            for (int i = 0; i < 400; i++) {
                ObjectArrayList<ItemStack> loot = new ObjectArrayList<>();
                LuckLoot.Result r = LuckLoot.apply(loot, dice, mega, luck, 400, LuckGauss.Kind.BOSS);
                if (!r.armour()) continue;
                ItemStack piece = null;
                for (ItemStack s : loot) if (s.getItem() instanceof LootArmorItem) piece = s;
                if (piece == null) return fail("the armour flag was set but there is no armour piece in the loot");
                GearRoll roll = GearStacks.rollOf(piece);
                if (roll == null || roll.lines().size() < 2) return fail("an armour piece came without a roll of at least two lines");
                if (piece.getMaxStackSize() != 1) return fail("an armour piece stacks");
                rarity[k] += roll.rarity().ordinal();
                hits[k]++;
                if (k == 1) {
                    found++;
                    rolledLines += roll.lines().size();
                    distinct.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(piece.getItem()) + "/" + roll.seed());
                }
            }
        }
        if (found < 20) return fail("a Luck 20 boss on floor 400 left only " + found + " armour pieces in 400 rolls");
        if (distinct.size() < found / 2) return fail("the armour rolls are not randomised: " + distinct.size() + " distinct of " + found);
        if (hits[0] > 0 && rarity[1] / hits[1] <= rarity[0] / hits[0]) return fail("Luck did not raise the armour's rarity: " + String.format(java.util.Locale.ROOT, "%.2f", rarity[0] / hits[0]) + " vs " + String.format(java.util.Locale.ROOT, "%.2f", rarity[1] / hits[1]));
        return SelfTests.Result.ok(found + " armour pieces in 400 boss rolls at Luck 20, " + distinct.size() + " distinct, " + String.format(java.util.Locale.ROOT, "%.1f", rolledLines / (double) found) + " lines each");
    }
}
