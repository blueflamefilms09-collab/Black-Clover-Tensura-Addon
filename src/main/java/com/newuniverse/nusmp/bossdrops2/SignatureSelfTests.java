package com.newuniverse.nusmp.bossdrops2;

import com.newuniverse.nusmp.boss.BossRegistry;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.relics.RelicCatalog;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.order.UniqueLedger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the real game proves what a unit test cannot. The CI server smoke test runs these on a dedicated server ({@code bossdrops2.*}): {@code tables} (every boss of the catalog and every boss of the
 * registry has a table, every extra, effect and relic of every table exists in the real registries), {@code items} (both items are registered and every boss has a signature stack with its icon),
 * {@code pay} (a stand-in fighter is paid with a rigged dice, the piece cap holds, a creative-free kill pays a signature), {@code unique} (the first kill issues the one-of-one through the relic ledger, the
 * second pays an echo, and the relic goes back into the pool afterwards so the world is left as it was). Server safe: no client class.
 */
public final class SignatureSelfTests {
    private SignatureSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("bossdrops2.tables", s -> tables());
        SelfTests.register("bossdrops2.items", s -> items());
        SelfTests.register("bossdrops2.pay", SignatureSelfTests::pay);
        SelfTests.register("bossdrops2.unique", SignatureSelfTests::unique);
    }

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("anime_multiverse", path)).orElse(null);
    }

    private static Result tables() {
        for (String boss : BossSigCatalog.BOSSES) {
            SignatureTable.Table t = SignatureData.table(boss);
            if (t == null) return Result.fail("no signature table loaded for " + boss);
            if (t.tier() != BossSigCatalog.tier(boss)) return Result.fail(boss + ": the catalog tier differs from the file");
            if (!t.hasUnique() != BossSigCatalog.relic(boss).isEmpty()) return Result.fail(boss + ": the catalog one-of-one differs from the file");
            for (SignatureTable.Row r : t.extras()) {
                ResourceLocation id = ResourceLocation.tryParse(r.item());
                if (id == null || BuiltInRegistries.ITEM.getOptional(id).isEmpty()) return Result.fail(boss + " names a missing item " + r.item());
            }
            List<SignatureTable.Boon> boons = new java.util.ArrayList<>(t.signature().boon());
            if (t.hasUnique()) boons.addAll(t.unique().echoBoon());
            for (SignatureTable.Boon b : boons) {
                ResourceLocation id = ResourceLocation.tryParse(b.effect());
                if (id == null || BuiltInRegistries.MOB_EFFECT.getOptional(id).isEmpty()) return Result.fail(boss + " names a missing effect " + b.effect());
            }
            if (t.hasUnique()) {
                String why = relicProblem(t.unique().relic());
                if (why != null) return Result.fail(boss + ": " + why);
            }
        }
        for (String path : BossRegistry.paths()) {
            if (SignatureData.table(path) == null) return Result.fail("the boss " + path + " is in the boss registry but has no signature table");
        }
        return Result.ok(SignatureData.size() + " signature tables loaded; every item, effect and relic exists; every registered boss has a table");
    }

    /** Null when the relic can be a one-of-one, else the reason. */
    private static String relicProblem(String id) {
        RelicDef d = Relics.byId(id);
        if (d == null) return "the relic " + id + " does not exist";
        if (!d.isUnique()) return "the relic " + id + " is not unique";
        if (d.isPairHalf()) return "the relic " + id + " is half of a pair";
        if (RelicCatalog.newStack(d).isEmpty()) return "the relic " + id + " has no registered item";
        return null;
    }

    private static Result items() {
        if (item("boss_signature") == null) return Result.fail("boss_signature is not registered");
        if (item("boss_echo") == null) return Result.fail("boss_echo is not registered");
        for (String boss : BossSigCatalog.BOSSES) {
            ItemStack s = BossSigItems.signature(boss, BossSigCatalog.tier(boss), 1);
            if (!boss.equals(BossSigItems.bossOf(s))) return Result.fail("the signature stack of " + boss + " does not remember its boss");
            var cmd = s.get(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA);
            if (cmd == null || cmd.value() != BossSigCatalog.modelData(boss) || cmd.value() < 1) return Result.fail("the signature stack of " + boss + " has no icon index");
        }
        return Result.ok("both items are registered; " + BossSigCatalog.BOSSES.size() + " signature stacks carry their boss and icon");
    }

    private static ServerPlayer player(ServerLevel level, String name) {
        return SelfTests.standIn(level, "bossdrops2_" + name, 0.5, level.getMaxBuildHeight() - 40, 0.5);
    }

    private static Result pay(MinecraftServer server) {
        if (!SignatureConfig.worthwhile()) return Result.ok("skipped: bossDrops.worthwhile is false");
        ServerLevel level = server.overworld();
        ServerPlayer a = player(level, "a"), b = player(level, "b"), c = player(level, "c");
        try {
            SignatureTable.Table real = SignatureData.table("meliodas");
            if (real == null) return Result.fail("no Meliodas table");
            SignatureTable.Table plain = new SignatureTable.Table("selftest", real.tier(), real.signature(), real.extras(), null);
            List<Fighter> fighters = List.of(new Fighter(a.getUUID(), 90.0, false), new Fighter(b.getUUID(), 60.0, true), new Fighter(c.getUUID(), 30.0, false));
            Map<UUID, ServerPlayer> players = Map.of(a.getUUID(), a, b.getUUID(), b, c.getUUID(), c);
            List<ItemStack> paid = SignatureEvents.pay(server, level, plain, Component.literal("Self-test boss"), fighters, players, () -> 0.0);
            if (paid.isEmpty() || paid.size() > SignaturePlan.MAX_PIECES) return Result.fail("a three-fighter kill paid " + paid.size() + " piece(s); expected 1 to " + SignaturePlan.MAX_PIECES);
            for (ServerPlayer p : List.of(a, b, c)) {
                boolean sig = false;
                for (ItemStack s : p.getInventory().items) if (s.getItem() == item("boss_signature")) sig = true;
                if (!sig) return Result.fail(p.getGameProfile().getName() + " fought the boss and got no signature (every fighter rolls their own)");
            }
            return Result.ok("a three-fighter kill paid " + paid.size() + " piece(s) (cap " + SignaturePlan.MAX_PIECES + "), every fighter got a signature");
        } finally {
            for (ServerPlayer p : List.of(a, b, c)) p.getInventory().clearContent();
        }
    }

    private static Result unique(MinecraftServer server) {
        if (!SignatureConfig.worthwhile()) return Result.ok("skipped: bossDrops.worthwhile is false");
        ServerLevel level = server.overworld();
        SignatureTable.Table t = SignatureData.table("meliodas");
        if (t == null || !t.hasUnique()) return Result.fail("Meliodas has no one-of-one table");
        String relic = t.unique().relic();
        ServerPlayer a = player(level, "u");
        boolean wasLive = UniqueLedger.liveIds(server).contains(relic);
        if (wasLive) return Result.ok("skipped: " + relic + " already exists in this world (the first-kill path was not exercised)");
        try {
            List<Fighter> one = List.of(new Fighter(a.getUUID(), 50.0, true));
            Map<UUID, ServerPlayer> players = Map.of(a.getUUID(), a);
            List<ItemStack> first = SignatureEvents.pay(server, level, t, Component.literal("Self-test boss"), one, players, () -> 0.0);
            Item relicItem = RelicCatalog.newStack(Relics.byId(relic)).getItem();
            if (first.stream().noneMatch(s -> s.getItem() == relicItem)) return Result.fail("the first kill did not pay the one-of-one " + relic);
            if (!UniqueLedger.liveIds(server).contains(relic)) return Result.fail("the relic ledger does not know the issued " + relic);
            List<ItemStack> second = SignatureEvents.pay(server, level, t, Component.literal("Self-test boss"), one, players, () -> 0.0);
            if (second.stream().anyMatch(s -> s.getItem() == relicItem)) return Result.fail("the second kill paid a second copy of " + relic);
            if (second.stream().noneMatch(s -> s.getItem() == item("boss_echo"))) return Result.fail("the second kill did not pay an echo");
            return Result.ok("the first kill issued " + relic + " through the ledger; the second paid an echo and no second copy (" + first.size() + " and " + second.size() + " pieces)");
        } finally {
            a.getInventory().clearContent();
            UniqueLedger.releaseToPool(server, relic);
        }
    }
}
