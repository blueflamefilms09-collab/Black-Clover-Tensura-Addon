package com.newuniverse.nusmp.unique;

import com.newuniverse.nusmp.charter.CharterItems;
import com.newuniverse.nusmp.charter.CharterTable;
import com.newuniverse.nusmp.dragonballs.BallSet;
import com.newuniverse.nusmp.dragonballs.DragonBallItem;
import com.newuniverse.nusmp.greed.GreedItems;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.relics.RelicCatalog;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.shishou.ShishouItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Why: nothing of the Unique Guardian can be played by its author, so what can be proved without a person is proved by the real game: the CI server smoke test boots the mod and runs these (keys
 * {@code unique.rules}, {@code unique.kinds}, {@code unique.world}) on a dedicated server with stand-in players. {@code unique.rules}: the state machine (lost to pool, theft, no second release, the flap guard) and the
 * config. {@code unique.kinds}: every kind of 1-of-1 is wired (an item of each is identified by exactly the right adapter, its id round-trips and is a valid id). {@code unique.world}: with a stand-in kind (so no real unique
 * is touched) two stand-in players carry, lose, steal and destroy a unique through the real scan and the real clocks: a held copy is seen, a stolen copy is not a loss, a destroyed copy goes back into the pool exactly once,
 * and a new valid copy afterwards is tracked again. Server safe: no client class. The world is left as it was found.
 */
public final class UniqueSelfTests {
    private UniqueSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("unique.rules", s -> rules());
        SelfTests.register("unique.kinds", s -> kinds());
        SelfTests.register("unique.world", UniqueSelfTests::world);
    }

    // ---------------------------------------------------------------- pure rules
    private static Result rules() {
        UniqueData d = new UniqueData();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        d.see("t:x", UniqueSpot.held(a, "minecraft:overworld", 0, 64, 0), 0, 1_000);
        d.markMissing("t:x", 10, true);
        if (UniqueRules.verdict(d.get("t:x"), 10 + UniqueRules.CONFIRM_TICKS, 6000) != UniqueRules.Verdict.RELEASE) return Result.fail("a destroyed copy was not confirmed");
        if (d.release("t:x", 2_000, "destroyed") != UniqueData.Release.RELEASED || d.get("t:x").state != UniqueState.POOL) return Result.fail("a lost copy did not return to the pool");
        if (d.release("t:x", 3_000, "destroyed") != UniqueData.Release.UNKNOWN) return Result.fail("a unique was returned twice");
        d.see("t:y", UniqueSpot.held(a, "minecraft:overworld", 0, 64, 0), 0, 1_000);
        d.markMissing("t:y", 10, false);
        d.see("t:y", UniqueSpot.held(b, "minecraft:overworld", 0, 64, 0), 20, 2_000);
        if (d.get("t:y").missing() || !b.equals(d.get("t:y").holder)) return Result.fail("theft was counted as a loss");
        if (!UniqueRules.oldCopy(0, 1) || UniqueRules.oldCopy(-1, 1) || UniqueRules.oldCopy(1, 1)) return Result.fail("the stamp rule is wrong");
        if (UniqueConfig.graceSeconds() < 30 || UniqueConfig.absenceDays() < 0) return Result.fail("the settings are out of range");
        return Result.ok("lost to pool, never twice, theft is not loss, old copies and the settings hold");
    }

    // ---------------------------------------------------------------- every kind is wired
    private static Result kinds() {
        StringBuilder done = new StringBuilder();
        String bad;
        if ((bad = expect(GreedItems.creativeStack(), "greed", "greed")) != null) return Result.fail(bad);
        if ((bad = expect(ShishouItems.creativeStack(), "shishou", "shishou")) != null) return Result.fail(bad);
        done.append("greed, shishou");
        for (CharterTable.Boss boss : CharterTable.all()) {
            Item it = CharterItems.item(boss.id());
            if (it == null) continue;
            if ((bad = expect(new ItemStack(it), "charter", "charter:" + boss.id())) != null) return Result.fail(bad);
            if (!UniqueKinds.kindOfId("charter:" + boss.id()).stamped()) return Result.fail("the charter kind does not stamp");
            done.append(", charter");
            break;
        }
        if ((bad = expect(DragonBallItem.make(BallSet.EARTH, 1, -1, false), "dragonball", "dragonball:earth:1")) != null) return Result.fail(bad);
        if ((bad = expect(DragonBallItem.make(BallSet.SUPER, 7, -1, false), "dragonball", "dragonball:super:7")) != null) return Result.fail(bad);
        done.append(", dragonball");
        for (RelicDef def : Relics.all()) {
            if (!def.isUnique() || def.isPairHalf()) continue;
            ItemStack s = RelicCatalog.newStack(def);
            if (s.isEmpty() || Relics.defOf(s) != def) continue;
            UniqueKinds.Ident i = UniqueKinds.identify(s);
            if (i != null && i.kind().name().equals("relic")) {
                if ((bad = expect(s, "relic", "relic:" + def.id())) != null) return Result.fail(bad);
                done.append(", relic");
                break;
            }
        }
        if (HeroUnique.ids().size() != 4) return Result.fail("the four Holy Weapons are not all known: " + HeroUnique.ids());
        for (String id : HeroUnique.ids()) if (!UniqueRules.validId(id) || !HeroUnique.owns(id)) return Result.fail("bad hero id " + id);
        return Result.ok("wired: " + done + ", four Holy Weapons");
    }

    private static String expect(ItemStack s, String kind, String id) {
        UniqueKinds.Ident i = UniqueKinds.identify(s);
        if (i == null) return id + ": not identified as a unique";
        if (!i.kind().name().equals(kind) || !i.id().equals(id)) return id + ": identified as " + i.kind().name() + "/" + i.id();
        if (!UniqueRules.validId(i.id())) return i.id() + " is not a valid unique id";
        UniqueKind k = UniqueKinds.kindOfId(id);
        if (k != i.kind() || !k.owns(id) || k.label(id).isEmpty() || k.pool(id).isEmpty()) return id + ": the kind does not own its own id";
        return null;
    }

    // ---------------------------------------------------------------- the guardian in the real game, with a stand-in unique
    private static final class TestKind implements UniqueKind {
        static final String ID = "selftest:one", TAG = "ug_selftest";
        boolean alive = true;
        int released;

        ItemStack stack() {
            ItemStack s = new ItemStack(Items.STICK);
            CustomData.update(DataComponents.CUSTOM_DATA, s, t -> t.putBoolean(TAG, true));
            return s;
        }

        @Override public String name() { return "selftest"; }
        @Override public boolean owns(String id) { return ID.equals(id); }
        @Override public String idOf(ItemStack s) {
            CustomData cd = s.get(DataComponents.CUSTOM_DATA);
            return cd != null && cd.contains(TAG) ? ID : null;
        }
        @Override public boolean valid(MinecraftServer server, String id, ItemStack s) { return true; }
        @Override public boolean exists(MinecraftServer server, String id) { return alive; }
        @Override public boolean release(MinecraftServer server, String id) {
            alive = false;
            released++;
            return true;
        }
        @Override public String label(String id) { return "Self-test unique"; }
        @Override public String pool(String id) { return "nowhere (a self-test)"; }
    }

    private static Result world(MinecraftServer server) {
        if (!UniqueConfig.enabled()) return Result.ok("skipped: unique.enabled is false");
        if (!UniqueGuardian.bound()) UniqueGuardian.bind(server);
        UniqueSaved sd = UniqueGuardian.saved();
        UniqueData d = sd.data();
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 3, 0);
        ServerPlayer a = SelfTests.standIn(level, "unique_a", at.x, at.y, at.z), b = SelfTests.standIn(level, "unique_b", at.x + 1, at.y, at.z);
        TestKind k = new TestKind();
        UniqueKinds.addForTest(k);
        long grace = UniqueRules.graceTicks(UniqueConfig.graceSeconds());
        try {
            UniqueConfig.testFloorOnly = false;                                                   // the 4.23.0 clocks first
            a.getInventory().clearContent();
            b.getInventory().clearContent();
            // a carries the unique: it is tracked as held by a, and a scan that finds it leaves it accounted for
            a.getInventory().add(k.stack());
            UniqueGuardian.scan(a, null);
            UniqueRecord r = d.get(TestKind.ID);
            if (r == null || r.state != UniqueState.HELD || !a.getUUID().equals(r.holder) || r.missing()) return Result.fail("a held unique was not tracked as held by its holder");
            // theft: it leaves a, b has it. a's scan starts a grace, b's scan is a sighting that cancels it: not a loss, however long we wait
            long t0 = level.getGameTime();
            a.getInventory().clearContent();
            b.getInventory().add(k.stack());
            UniqueGuardian.scan(a, null);
            if (!d.get(TestKind.ID).missing()) return Result.fail("a copy gone from its holder did not start the grace");
            UniqueGuardian.scan(b, null);
            r = d.get(TestKind.ID);
            if (r.missing() || !b.getUUID().equals(r.holder)) return Result.fail("theft was counted as a loss");
            UniqueGuardian.confirmPass(server, t0 + grace * 3, TestKind.ID);
            if (k.released != 0 || !k.alive) return Result.fail("a unique that another player holds was returned to the pool");
            // destroyed: b's copy is deleted. Not yet before the grace has run, then exactly once
            b.getInventory().clearContent();
            long t1 = level.getGameTime();
            UniqueGuardian.scan(b, null);
            if (!d.get(TestKind.ID).missing()) return Result.fail("a deleted copy did not start the grace");
            UniqueGuardian.confirmPass(server, t1 + grace - 1, TestKind.ID);
            if (k.released != 0) return Result.fail("the unique returned before the grace ran out");
            UniqueGuardian.confirmPass(server, t1 + grace, TestKind.ID);
            if (k.released != 1 || k.alive || d.get(TestKind.ID).state != UniqueState.POOL || d.epoch(TestKind.ID) != 1) return Result.fail("a destroyed unique did not go back into the pool once");
            UniqueGuardian.confirmPass(server, t1 + grace * 5, TestKind.ID);
            if (k.released != 1) return Result.fail("a unique was returned to the pool twice");
            // the pool gives it again: a new valid copy is tracked again, and nothing is created by the guardian
            k.alive = true;
            b.getInventory().add(k.stack());
            UniqueGuardian.scan(b, null);
            r = d.get(TestKind.ID);
            if (r.state != UniqueState.HELD || !b.getUUID().equals(r.holder)) return Result.fail("a new valid copy after the return was not tracked");
            for (int i = 0; i < a.getInventory().getContainerSize(); i++) if (!a.getInventory().getItem(i).isEmpty()) return Result.fail("the guardian put an item into an inventory");
            // a destroyed item entity is confirmed after five seconds, and a sighting during them cancels it
            long t2 = level.getGameTime();
            d.markMissing(TestKind.ID, t2, true);
            UniqueGuardian.scan(b, null);
            if (d.get(TestKind.ID).missing()) return Result.fail("a sighting did not cancel the confirm clock");
            d.markMissing(TestKind.ID, t2, true);
            UniqueGuardian.confirmPass(server, t2 + UniqueRules.CONFIRM_TICKS, TestKind.ID);
            if (k.released != 2) return Result.fail("a confirmed destroyed copy did not go back into the pool");
            // 4.103.2 floorOnly: a copy that is just not seen (a backpack) never goes back; only a destroyed floor drop does
            UniqueConfig.testFloorOnly = true;
            k.alive = true;
            b.getInventory().clearContent();
            b.getInventory().add(k.stack());
            UniqueGuardian.scan(b, null);
            b.getInventory().clearContent();                                                      // into a backpack the scan cannot see
            long t3 = level.getGameTime();
            UniqueGuardian.scan(b, null);
            if (d.get(TestKind.ID).missing()) return Result.fail("floorOnly: a copy that is not seen was counted as missing");
            d.markMissing(TestKind.ID, t3, false);
            UniqueGuardian.confirmPass(server, t3 + grace * 5, TestKind.ID);
            if (k.released != 2) return Result.fail("floorOnly: an unconfirmed copy went back into the pool");
            d.markMissing(TestKind.ID, t3, true);
            UniqueGuardian.confirmPass(server, t3 + UniqueRules.CONFIRM_TICKS, TestKind.ID);
            if (k.released != 3) return Result.fail("floorOnly: a destroyed floor drop did not go back into the pool");
            return Result.ok("held, theft, grace, destroyed once, tracked again, the confirm clock and floor-only hold");
        } finally {
            UniqueConfig.testFloorOnly = null;
            UniqueKinds.removeForTest(k);
            d.remove(TestKind.ID);
            sd.setDirty();
            a.getInventory().clearContent();
            b.getInventory().clearContent();
        }
    }
}
