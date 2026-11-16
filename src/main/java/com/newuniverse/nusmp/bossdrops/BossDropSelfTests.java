package com.newuniverse.nusmp.bossdrops;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Why: the real game proves what a unit test cannot. The CI server smoke test runs these on a dedicated server: {@code bossdrops.items} (every trophy and armour piece is registered under the id its model and
 * recipe use), {@code bossdrops.tables} (all 23 data tables loaded, every row names a registered item, no table pays more than its caps), {@code bossdrops.pay} (a kill with a rigged dice pays a stand-in
 * player, the first-kill row pays exactly once, the stack and piece caps hold). Server safe: no client class.
 */
public final class BossDropSelfTests {
    private BossDropSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("bossdrops.items", s -> items());
        SelfTests.register("bossdrops.tables", s -> tables());
        SelfTests.register("bossdrops.pay", BossDropSelfTests::pay);
    }

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("anime_multiverse", path)).orElse(null);
    }

    private static Result items() {
        for (String b : BossDropCatalog.BOSSES) if (item(BossDropCatalog.trophy(b)) == null) return Result.fail("trophy not registered: " + b);
        for (String id : BossDropCatalog.LUCIFERO_PIECES) if (item(id) == null) return Result.fail("piece not registered: " + id);
        for (String id : BossDropCatalog.PLATE_PIECES) if (item(id) == null) return Result.fail("piece not registered: " + id);
        if (BossDropItems.renderables().size() != BossDropCatalog.BOSSES.size()) return Result.fail("the renderable list is not one per trophy");
        return Result.ok(BossDropCatalog.BOSSES.size() + " trophies and 8 armour pieces are registered");
    }

    private static Result tables() {
        if (!BossDropConfig.enabled()) return Result.ok("skipped: bossdrops.enabled is false");
        for (String b : BossDropCatalog.BOSSES) {
            BossDropTable.Table t = BossDropData.table(b);
            if (t == null) return Result.fail("no drop table loaded for " + b);
            boolean trophy = false;
            for (BossDropTable.Row r : t.rows()) {
                ResourceLocation id = ResourceLocation.tryParse(r.item());
                if (id == null || BuiltInRegistries.ITEM.getOptional(id).isEmpty()) return Result.fail(b + " names a missing item " + r.item());
                if (r.item().equals("anime_multiverse:" + BossDropCatalog.trophy(b))) trophy = true;
            }
            if (!trophy) return Result.fail(b + " does not drop its own trophy");
            if (t.rows().size() > BossDropTable.MAX_ROWS || t.maxStacks() > BossDropTable.HARD_MAX_STACKS) return Result.fail(b + " is over the table caps");
        }
        return Result.ok(BossDropData.size() + " drop tables loaded, every row names a real item");
    }

    private static Result pay(MinecraftServer server) {
        if (!BossDropConfig.enabled()) return Result.ok("skipped: bossdrops.enabled is false");
        ServerLevel level = server.overworld();
        ServerPlayer p = SelfTests.standIn(level, "bossdrops_a", 0.5, level.getMaxBuildHeight() - 40, 0.5);
        String trophy = "anime_multiverse:" + BossDropCatalog.trophy("meliodas");
        BossDropTable.Table synth = new BossDropTable.Table("selftest_" + System.nanoTime(), 6, List.of(
                new BossDropTable.Row(trophy, 1.0, 1, 1, true), new BossDropTable.Row(trophy, 1.0, 2, 2, false)));
        int first = BossDropEvents.pay(server, level, p, synth, () -> 0.0).size();
        int again = BossDropEvents.pay(server, level, p, synth, () -> 0.0).size();
        if (first != 2) return Result.fail("the first kill paid " + first + " piece(s), expected 2");
        if (again != 1) return Result.fail("the repeat kill paid " + again + " piece(s), expected 1 (the first-kill row must pay once)");
        BossDropTable.Table t = BossDropData.table(BossDropCatalog.LUCIFERO);
        if (t == null) return Result.fail("no Lucifero table");
        List<ItemStack> paid = BossDropEvents.pay(server, level, p, t, () -> 0.0);
        if (paid.isEmpty() || paid.size() > BossDropTable.MAX_PIECES) return Result.fail("the Lucifero kill paid " + paid.size() + " piece(s)");
        return Result.ok("the first-kill row pays once; a Lucifero kill with the lowest dice pays " + paid.size() + " piece(s), under the cap");
    }
}
