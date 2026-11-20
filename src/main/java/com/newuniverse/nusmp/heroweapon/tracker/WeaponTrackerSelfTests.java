package com.newuniverse.nusmp.heroweapon.tracker;

import com.newuniverse.nusmp.heroweapon.HeroConfig;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Why: the tracker cannot be played by its author, so the CI server smoke test runs {@code weapons.tracker.rules} (the pure rules on the real catalogue of 21 weapons: every status, hidden names, unique off,
 * the filters, the limiter, the packet round trip) and {@code weapons.tracker.player} (the rules against the REAL register: two stand-in players bind a weapon each, the table each of them sees must say YOURS
 * for their own, TAKEN or SHARED (by the unique setting) for the other's, FREE after a release, the holder's name only when {@code trackerShowNames} is on; and the command's open is answered once, then
 * dropped inside two seconds). Server safe: no client class. A stand-in is released before and after.
 */
public final class WeaponTrackerSelfTests {
    private WeaponTrackerSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("weapons.tracker.rules", s -> rules());
        SelfTests.register("weapons.tracker.player", WeaponTrackerSelfTests::player);
    }

    private static SelfTests.Result rules() {
        TrackerRules.Settings on = new TrackerRules.Settings(true, true, false, true, 1);
        String me = "00000000-0000-0000-0000-000000000001", other = "00000000-0000-0000-0000-000000000002";
        Map<String, List<String>> reg = Map.of(HeroKind.SHIELD.itemId(), List.of(other), HeroKind.HAMMER.itemId(), List.of(other, me), HeroKind.FAN.itemId(), List.of(other));
        TrackerRules.Table t = TrackerRules.build(reg, on, new TrackerRules.Viewer(me, HeroKind.HAMMER), u -> "Someone", u -> true);
        if (t.rows().size() != HeroKind.values().length || t.rows().size() != 21) return SelfTests.Result.fail("the table has " + t.rows().size() + " rows, not one per weapon");
        if (t.own() != HeroKind.HAMMER) return SelfTests.Result.fail("the viewer's own weapon is not reported");
        if (row(t, HeroKind.SHIELD).status() != TrackerRules.Status.TAKEN) return SelfTests.Result.fail("a unique Legendary Weapon held by someone else is not TAKEN");
        if (row(t, HeroKind.HAMMER).status() != TrackerRules.Status.YOURS || row(t, HeroKind.HAMMER).holders() != 2) return SelfTests.Result.fail("the shared weapon the viewer holds is not YOURS with 2 holders");
        if (row(t, HeroKind.FAN).status() != TrackerRules.Status.SHARED) return SelfTests.Result.fail("a Vassal Weapon held by someone else with vassalUnique off is not SHARED");
        if (row(t, HeroKind.SPEAR).status() != TrackerRules.Status.FREE) return SelfTests.Result.fail("an unheld weapon is not FREE");
        if (!"Someone".equals(row(t, HeroKind.SHIELD).holder())) return SelfTests.Result.fail("a holder's name is missing while trackerShowNames is on");
        TrackerRules.Table hidden = TrackerRules.build(reg, new TrackerRules.Settings(true, true, false, false, 1), new TrackerRules.Viewer(me, null), u -> "Someone", u -> false);
        if (!row(hidden, HeroKind.SHIELD).holder().isEmpty()) return SelfTests.Result.fail("a holder's name leaks while trackerShowNames is off");
        if (hidden.own() != HeroKind.HAMMER) return SelfTests.Result.fail("the register's claim does not give the viewer an own weapon");
        if (TrackerRules.filter(t.rows(), TrackerRules.Filter.LEGENDARY).size() != 4 || TrackerRules.filter(t.rows(), TrackerRules.Filter.SEVEN_STAR).size() != 7
                || TrackerRules.filter(t.rows(), TrackerRules.Filter.VASSAL).size() != 10) return SelfTests.Result.fail("the class filters do not give 4 / 7 / 10 weapons");
        TrackerLimiter lim = new TrackerLimiter();
        java.util.UUID id = java.util.UUID.nameUUIDFromBytes("tracker".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (!lim.allow(id, 100) || lim.allow(id, 100 + TrackerLimiter.GAP_TICKS - 1) || !lim.allow(id, 100 + TrackerLimiter.GAP_TICKS)) return SelfTests.Result.fail("the limiter does not allow one request per gap");
        io.netty.buffer.ByteBuf raw = io.netty.buffer.Unpooled.buffer();
        net.minecraft.network.FriendlyByteBuf buf = new net.minecraft.network.FriendlyByteBuf(raw);
        WeaponTrackerNet.write(buf, new WeaponTrackerNet.Reply(true, t));
        int size = buf.readableBytes();
        WeaponTrackerNet.Reply back = WeaponTrackerNet.read(buf);
        if (!back.open() || !back.table().equals(t)) return SelfTests.Result.fail("the reply does not survive its codec");
        raw.release();
        return SelfTests.Result.ok("21 rows, every status, names hidden, filters 4/7/10, limiter, codec round trip (" + size + " bytes)");
    }

    private static TrackerRules.Row row(TrackerRules.Table t, HeroKind k) {
        for (TrackerRules.Row r : t.rows()) if (r.kind() == k) return r;
        throw new IllegalStateException("no row for " + k);
    }

    private static HeroKind freeOf(ServerPlayer viewer, boolean holy) {
        for (TrackerRules.Row r : WeaponTrackerService.snapshot(viewer).rows()) if (r.status() == TrackerRules.Status.FREE && r.kind().isHoly() == holy) return r.kind();
        return null;
    }

    private static SelfTests.Result player(MinecraftServer server) {
        if (!HeroConfig.enabled()) return SelfTests.Result.ok("skipped: heroweapon.enabled is false in the config");
        if (HeroConfig.maxBonds() < 1) return SelfTests.Result.ok("skipped: heroweapon.maxBonds is 0 in the config");
        ServerLevel level = server.overworld();
        FakePlayer a = SelfTests.player(level, "tracker_a", 0.5, 100, 0.5);
        FakePlayer b = SelfTests.player(level, "tracker_b", 0.5, 100, 0.5);
        release(a);
        release(b);
        WeaponTrackerService.forget(a.getUUID());
        List<String> bad = new ArrayList<>();
        try {
            HeroKind mine = freeOf(a, false), his = freeOf(a, true);
            if (mine == null || his == null) return SelfTests.Result.ok("skipped: no free Vassal and Legendary Weapon left in the register of this world");
            TrackerRules.Table before = WeaponTrackerService.snapshot(a);
            if (before.rows().size() != 21) bad.add("the snapshot has " + before.rows().size() + " rows");
            if (before.own() != null) bad.add("a player with no bond has an own weapon");
            String why = HeroBonds.bind(a, mine);
            if (why != null) return SelfTests.Result.ok("skipped: " + mine + " could not be bound to a stand-in (" + why + ")");
            String why2 = HeroBonds.bind(b, his);
            if (why2 != null) return SelfTests.Result.ok("skipped: " + his + " could not be bound to a stand-in (" + why2 + ")");
            TrackerRules.Table forA = WeaponTrackerService.snapshot(a), forB = WeaponTrackerService.snapshot(b);
            if (row(forA, mine).status() != TrackerRules.Status.YOURS || forA.own() != mine) bad.add("A does not see their own weapon as YOURS");
            if (row(forB, his).status() != TrackerRules.Status.YOURS || forB.own() != his) bad.add("B does not see their own weapon as YOURS");
            TrackerRules.Status wantMine = HeroConfig.vassalUnique() ? TrackerRules.Status.TAKEN : TrackerRules.Status.SHARED;
            TrackerRules.Status wantHis = HeroConfig.holyUnique() ? TrackerRules.Status.TAKEN : TrackerRules.Status.SHARED;
            if (row(forB, mine).status() != wantMine) bad.add("B sees A's weapon as " + row(forB, mine).status() + ", expected " + wantMine);
            if (row(forA, his).status() != wantHis) bad.add("A sees B's weapon as " + row(forA, his).status() + ", expected " + wantHis);
            String name = row(forB, mine).holder();
            if (HeroConfig.trackerShowNames() ? !"tracker_a".equals(name) : !name.isEmpty()) bad.add("the holder name is '" + name + "' with trackerShowNames " + HeroConfig.trackerShowNames());
            if (row(forB, mine).holders() < 1) bad.add("the holder count of a held weapon is " + row(forB, mine).holders());
            HeroBonds.release(a);
            HeroBonds.release(b);
            TrackerRules.Table after = WeaponTrackerService.snapshot(a);
            if (row(after, mine).status() != TrackerRules.Status.FREE || row(after, his).status() != TrackerRules.Status.FREE) bad.add("a released weapon is not FREE again");
            if (after.own() != null) bad.add("a released player still has an own weapon");
            int first = WeaponTrackerService.open(a);
            int second = WeaponTrackerService.open(a);
            if (first != 1 || second != 0) bad.add("open answered " + first + " then " + second + " inside one tick (expected 1 then 0)");
        } finally {
            release(a);
            release(b);
            WeaponTrackerService.forget(a.getUUID());
            WeaponTrackerService.forget(b.getUUID());
        }
        return bad.isEmpty() ? SelfTests.Result.ok("the table follows the real register: YOURS / TAKEN or SHARED / FREE, names by trackerShowNames, one open per two seconds")
                : SelfTests.Result.fail(String.join("; ", bad));
    }

    private static void release(ServerPlayer p) {
        if (HeroBonds.has(p)) HeroBonds.release(p);
    }
}
