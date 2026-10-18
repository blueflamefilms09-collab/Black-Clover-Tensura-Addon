package com.newuniverse.nusmp.skilltree.loot;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftRules;
import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.item.SkillBookItem;
import com.newuniverse.nusmp.skilltree.item.SkillItems;
import com.newuniverse.nusmp.skilltree.state.IPlayerSkillTree;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * Why: the unique-lines loot is WIRING again (a loot modifier type, a JSON, a line in the global list, a cache keyed by the registry, a memory per player) and wiring is what a unit test
 * cannot see. {@code tree.lines.loot}, run by the CI server smoke test with a stand-in player, drives the real things: the type {@code nusmp:rift_books} is registered; the weight table is
 * built once per registry version and a thousand draws do not rebuild it and pick only lines of positive weight; the rift modifier's entry point adds one route book for a listed table
 * inside a pocket cell and NOTHING for an unlisted table, a position outside the pockets, the Overworld, a scale of 0 or a chance of 0; a made-up CHEST table carrying the real relic-vault id
 * is rolled through the whole loot pipeline (so the codec, the JSON and the global list are exercised) and must give books in a pocket cell and none outside it; and the pity rule
 * hands a player whose last book was an owned line only unowned lines. Where the world lacks something (fewer than two lines, no mega dimension) the answer says so and checks what it can.
 */
public final class LinesLootSelfTests {
    private LinesLootSelfTests() {}

    private static final ResourceLocation RELIC_VAULT = ResourceLocation.fromNamespaceAndPath("nusmp", "chests/dungeon_relic_vault");

    public static void register() { SelfTests.register("tree.lines.loot", LinesLootSelfTests::loot); }

    private static SelfTests.Result fail(String d) { return SelfTests.Result.fail(d); }

    private static LootParams chestParams(ServerLevel level, Vec3 origin, ServerPlayer opener) {
        LootParams.Builder b = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, origin).withOptionalParameter(LootContextParams.THIS_ENTITY, opener);
        return b.create(LootContextParamSets.CHEST);
    }

    private static LootTable chestTable() {
        LootTable table = LootTable.lootTable().setParamSet(LootContextParamSets.CHEST)
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1f)).add(LootItem.lootTableItem(Items.BREAD))).build();
        table.setLootTableId(RELIC_VAULT);
        return table;
    }

    private static SelfTests.Result loot(MinecraftServer server) {
        ServerLevel mega = SelfTests.mega(server);
        if (mega == null) return fail("no mega dimension");
        if (!NeoForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS.containsKey(ResourceLocation.fromNamespaceAndPath("nusmp", "rift_books"))) return fail("the loot modifier type nusmp:rift_books is not registered");
        StringBuilder said = new StringBuilder();
        int positive = 0;
        long total = 0;
        for (SkillRoute r : RouteRegistry.all()) {
            if (r.lootWeight() > 0) {
                positive++;
                total += r.lootWeight();
            }
        }
        // 1. the weight table: once per registry version
        RouteTable.Snap snap = RouteTable.current();
        if (snap.table().size() != positive || snap.table().total() != total) return fail("the weight table has " + snap.table().size() + " lines / " + snap.table().total() + " weight, the registry has " + positive + " / " + total);
        int builds = RouteTable.builds();
        Random random = new Random(11);
        Dice dice = random::nextDouble;
        int bad = 0;
        for (int i = 0; i < 1000; i++) {
            SkillRoute r = snap.at(snap.table().pick(dice));
            if (positive > 0 && (r == null || r.lootWeight() <= 0)) bad++;
        }
        if (bad != 0) return fail(bad + " of 1000 draws picked a line with no weight (or nothing)");
        if (RouteTable.builds() != builds || RouteTable.current() != snap) return fail("the weight table was rebuilt during 1000 draws (the cache is not keyed by the registry version)");
        said.append(positive).append(" lines weight ").append(total).append(", built once for 1000 draws; ");

        // 2. the rift modifier's entry point
        ServerPlayer opener = SelfTests.player(mega, "lines_loot_opener", 4_000_000, 96, 4_000_000);
        Vec3 inPocket = new Vec3(RiftRules.pocketX(0) + 18, RiftRules.POCKET_Y, RiftRules.POCKET_Z), outside = new Vec3(0, 100, 0);
        Map<String, Double> sure = Map.of(RELIC_VAULT.toString(), 1.0);
        RandomSource rs = RandomSource.create(5L);
        boolean routes = positive > 0 && LootConfig.get(LootConfig.ENABLED) && LootConfig.get(LootConfig.RIFT_BOOKS);
        LootTable table = chestTable();
        int added = RiftBookLootModifier.add(bread(), context(mega, inPocket, opener, rs), sure, 1.0, dice);
        if (added != (routes ? 1 : 0)) return fail("a listed table in a pocket cell with chance 1 added " + added + " books, expected " + (routes ? 1 : 0));
        if (RiftBookLootModifier.add(bread(), context(mega, outside, opener, rs), sure, 1.0, dice) != 0) return fail("a chest OUTSIDE the pockets paid a rift book");
        if (RiftBookLootModifier.add(bread(), context(mega, inPocket, opener, rs), Map.of("nusmp:chests/other", 1.0), 1.0, dice) != 0) return fail("an unlisted table paid a rift book");
        if (RiftBookLootModifier.add(bread(), context(mega, inPocket, opener, rs), sure, 0.0, dice) != 0) return fail("a scale of 0 still paid a rift book");
        ServerLevel overworld = server.overworld();
        if (RiftBookLootModifier.add(bread(), context(overworld, inPocket, null, rs), sure, 1.0, dice) != 0) return fail("a chest of the Overworld paid a rift book");
        said.append("rift entry point ok; ");

        // 3. the whole pipeline with the shipped JSON: books inside a pocket cell, none outside
        if (routes && LootConfig.get(LootConfig.RIFT_BOOK_SCALE) > 0) {
            int books = 0, outsideBooks = 0, n = 1500;
            for (int i = 0; i < n; i++) {
                for (ItemStack s : table.getRandomItems(chestParams(mega, inPocket, opener), rs)) if (s.getItem() instanceof SkillBookItem) books++;
                for (ItemStack s : table.getRandomItems(chestParams(mega, outside, opener), rs)) if (s.getItem() instanceof SkillBookItem) outsideBooks++;
            }
            if (books == 0) return fail("no rift book in " + n + " rolls of the relic-vault table inside a pocket cell: is nusmp:rift_books named in data/neoforge/loot_modifiers/global_loot_modifiers.json and defined in data/nusmp/loot_modifiers/rift_books.json?");
            if (outsideBooks != 0) return fail(outsideBooks + " rift books fell outside the pockets");
            said.append("pipeline ").append(n).append(" pocket rolls: ").append(books).append(" books; ");
        }

        // 4. the pity rule
        if (positive >= 2 && LootConfig.get(LootConfig.PITY)) {
            SkillRoute owned = null;
            for (SkillRoute r : RouteRegistry.all()) {
                if (r.lootWeight() > 0) {
                    owned = r;
                    break;
                }
            }
            IPlayerSkillTree tree = SkillTreeApi.of(opener);
            boolean had = tree.hasRoute(owned.id());
            try {
                tree.addRoute(owned.id());
                BookPity.GLOBAL.forget(opener.getUUID());
                boolean previousOwned = false;
                int twice = 0, ownedDraws = 0;
                for (int i = 0; i < 400; i++) {
                    ItemStack book = LootDrops.routeBook(dice, opener);
                    SkillRoute got = RouteRegistry.byBook(SkillItems.bookIdOf(book));
                    boolean isOwned = got != null && tree.hasRoute(got.id());
                    if (isOwned) ownedDraws++;
                    if (isOwned && previousOwned) twice++;
                    previousOwned = isOwned;
                }
                if (twice != 0) return fail("the pity rule handed an owned line " + twice + " times right after an owned line");
                said.append("pity: ").append(ownedDraws).append(" owned of 400, never two in a row; ");
            } finally {
                if (!had) tree.removeRoute(owned.id());
                BookPity.GLOBAL.forget(opener.getUUID());
            }
        } else {
            said.append("pity not driven (fewer than two lines, or switched off); ");
        }
        return SelfTests.Result.ok(said.toString());
    }

    private static ObjectArrayList<ItemStack> bread() {
        ObjectArrayList<ItemStack> loot = new ObjectArrayList<>();
        loot.add(new ItemStack(Items.BREAD));
        return loot;
    }

    private static LootContext context(ServerLevel level, Vec3 origin, ServerPlayer opener, RandomSource rs) {
        LootContext c = new LootContext.Builder(chestParams(level, origin, opener)).withOptionalRandomSource(rs).create(Optional.empty());
        c.setQueriedLootTableId(RELIC_VAULT);
        return c;
    }
}
