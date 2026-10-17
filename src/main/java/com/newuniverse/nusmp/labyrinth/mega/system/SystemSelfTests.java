package com.newuniverse.nusmp.labyrinth.mega.system;

import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: nothing in this package can be played by its author, so each part of it has a SELF-TEST that the CI server smoke test runs in the real game
 * ({@code /multiverse mega selftest}) with a fake player standing in for a person. {@code system.quest} walks the whole quest lifecycle through the REAL
 * floor-listener facade ({@code MegaDungeon.fireFloorEntered}): floor 1 gives an ACTIVE quest, completing it pays a reward, floor 2 gives a different kind,
 * leaving it unfinished and entering floor 3 fails it and rolls the next, a revisit of floor 2 starts nothing, a boss floor has no quest. {@code system.stats}
 * gives stat points and checks the attribute modifier with the FIXED id exists with the table's amount, that applying twice changes nothing, that the cap
 * holds and that removing works. {@code system.extract} remembers a slain zombie in sector 0 through the real death handler's recording, uses a core, and
 * checks that a shadow exists with the servant tag, the owner and scaled health, that bosses, named creatures and non-player kills are refused, that the
 * charge was spent and that a fourth shadow releases the first. {@code system.sync} round-trips both payloads through the real stream codecs and builds the
 * window's state for a player in and out of the dungeon (a fake player has no connection, so the packet is tested, not the send). {@code system.data} checks
 * that the shipped quest and stat files loaded through the real reload, every kind and stat is there, the item, the attachment and the magic-damage rule
 * exist. Each test cleans up (shadows discarded, the fake player's state reset) whatever happens.
 */
public final class SystemSelfTests {
    private SystemSelfTests() {}

    private static SelfTests.Result ok(String detail) { return SelfTests.Result.ok(detail); }

    private static SelfTests.Result fail(String detail) { return SelfTests.Result.fail(detail); }

    /** Registers the tests; called once from {@code SystemRegistry.init}. */
    public static void register() {
        SelfTests.register("system.data", SystemSelfTests::data);
        SelfTests.register("system.sync", SystemSelfTests::sync);
        SelfTests.register("system.quest", SystemSelfTests::quest);
        SelfTests.register("system.stats", SystemSelfTests::stats);
        SelfTests.register("system.extract", SystemSelfTests::extract);
    }

    private static void reset(FakePlayer p) {
        SystemStats.remove(p);
        p.setData(SystemAttachments.STATE, new SystemState());
        p.getInventory().clearContent();
        SystemQuests.forget(p.getUUID());
    }

    // ------------------------------------------------------------------------------------------------ data
    private static SelfTests.Result data(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        QuestCatalog cat = SystemData.quests();
        StatTable table = SystemData.stats();
        if (cat.size() < 5) bad.add("only " + cat.size() + " quest templates loaded");
        for (QuestKind k : QuestKind.values()) if (!cat.hasKind(k)) bad.add("no quest of kind " + k);
        for (QuestTemplate t : cat.all()) {
            if (t.title().isEmpty()) bad.add("quest " + t.id() + " has no title");
            if (QuestRoller.target(t, 1) < 1 || QuestRoller.target(t, 400) > t.max()) bad.add("quest " + t.id() + " has a target out of range");
        }
        int unresolved = 0;
        for (Stat s : Stat.values()) {
            StatTable.Def def = table.def(s);
            if (def == null || def.effects().isEmpty()) {
                bad.add("stat " + s + " has no effect in the table");
                continue;
            }
            for (StatTable.Effect e : def.effects()) {
                ResourceLocation rl = ResourceLocation.tryParse(e.attribute());
                boolean there = rl != null && BuiltInRegistries.ATTRIBUTE.containsKey(rl);
                if (!there) unresolved++;
                if (!there && e.attribute().startsWith("minecraft:")) bad.add("the vanilla attribute " + e.attribute() + " of " + s + " does not exist");
            }
        }
        if (BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("nusmp", "shadow_core")).isEmpty()) bad.add("the item nusmp:shadow_core is not registered");
        if (!NeoForgeRegistries.ATTACHMENT_TYPES.containsKey(ResourceLocation.fromNamespaceAndPath("nusmp", "system_state"))) bad.add("the attachment nusmp:system_state is not registered");
        ServerLevel level = SelfTests.mega(server);
        if (level != null) {
            if (!SystemQuests.isMagicDamage(level.damageSources().magic())) bad.add("vanilla magic damage is not recognised as magic");
            if (SystemQuests.isMagicDamage(level.damageSources().generic())) bad.add("generic damage is recognised as magic");
            if (SystemQuests.isMagicDamage(level.damageSources().fall())) bad.add("fall damage is recognised as magic");
        }
        return bad.isEmpty()
                ? ok(cat.size() + " quest templates (all " + QuestKind.values().length + " kinds), " + table.size() + " stats, " + unresolved + " optional attribute(s) of absent mods skipped, item, attachment and magic rule present")
                : fail(String.join(" | ", bad.subList(0, Math.min(5, bad.size()))));
    }

    // ------------------------------------------------------------------------------------------------ sync
    private static SelfTests.Result sync(MinecraftServer server) {
        SystemNet.Sync a = new SystemNet.Sync(true, -17, "abyssal", 17, false, SystemNet.Q_ACTIVE, -16, "speed", "message.nusmp.mega.system.quest.swift_ascent",
                "Reach the next floor within 266 seconds", 3, 266, 4210, true, List.of(1, 2, 3, 4, 5));
        SystemNet.Sync back = roundTrip(a);
        if (!a.equals(back)) return fail("the sync payload changed in a round trip: " + a + " became " + back);
        String longTitle = "x".repeat(400);
        SystemNet.Sync wordy = new SystemNet.Sync(true, 3, "holy", 3, false, SystemNet.Q_ACTIVE, 3, "kill_n", "message.nusmp.mega.system.quest.long", longTitle, 0, 5, -1, false, List.of(0, 0, 0, 0, 0));
        if (roundTrip(wordy).titleFallback().length() != 256) return fail("a quest title over the wire limit was not cut to 256 characters (it would kick the player)");
        SystemNet.Sync hidden = roundTrip(SystemNet.Sync.hidden());
        if (hidden.active() || hidden.stats().size() != Stat.COUNT) return fail("the hidden payload is not hidden or has " + hidden.stats().size() + " stats");
        SystemNet.Toast t = SystemNet.Toast.of(ToastKind.REWARD, SystemText.TOAST_REWARD_STAT, "2", "STR");
        SystemNet.Toast tb = roundTrip(t);
        if (!t.equals(tb)) return fail("the toast payload changed in a round trip: " + t + " became " + tb);
        if (ToastKind.fromCode(tb.kind()) != ToastKind.REWARD) return fail("the toast kind code did not map back");
        ServerLevel level = SelfTests.mega(server);
        if (level == null) return fail("no mega dimension");
        Sector s0 = MegaData.get(server).sector(0);
        FakePlayer outside = SelfTests.player(level, "system_tester_sync_out", 5_000_000, 70, 5_000_000);
        try {
            SystemNet.Sync far = SystemSync.build(outside);
            if (far.active()) return fail("a player far from every sector got an active window");
            if (s0 == null || s0.rooms.isEmpty()) return ok("round trips pass; no sector 0 to build an in-dungeon packet from");
            BlockPos at = s0.rooms.get(0).center();
            FakePlayer inside = SelfTests.player(level, "system_tester_sync_in", at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            try {
                SystemState st = new SystemState();
                st.setPoints(Stat.VIT, 7);
                inside.setData(SystemAttachments.STATE, st);
                SystemNet.Sync mine = SystemSync.build(inside);
                if (!SystemConfig.enabled()) return ok("round trips pass; the System is switched off, so the in-dungeon window is hidden: " + !mine.active());
                int expected = MegaDungeon.floorAt(level, inside.blockPosition());
                if (!mine.active() || mine.floor() != expected) return fail("the window for a player in sector 0 should be active on floor " + expected + " but is " + mine);
                if (mine.stats().get(Stat.VIT.ordinal()) != 7) return fail("the packet carries " + mine.stats() + " but the player has 7 VIT");
                if (!"holy".equals(mine.polarity())) return fail("sector 0 is the Holy road but the packet says '" + mine.polarity() + "'");
                if (!mine.equals(roundTrip(mine))) return fail("the built packet does not survive a round trip");
                return ok("both payloads round-trip; a player in sector 0 gets an active window on floor " + expected + " (holy, 7 VIT); one far away gets a hidden one");
            } finally {
                reset(inside);
            }
        } finally {
            reset(outside);
        }
    }

    private static SystemNet.Sync roundTrip(SystemNet.Sync p) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        SystemNet.Sync.CODEC.encode(buf, p);
        return SystemNet.Sync.CODEC.decode(buf);
    }

    private static SystemNet.Toast roundTrip(SystemNet.Toast p) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        SystemNet.Toast.CODEC.encode(buf, p);
        return SystemNet.Toast.CODEC.decode(buf);
    }

    // ------------------------------------------------------------------------------------------------ quests
    private static SelfTests.Result quest(MinecraftServer server) {
        ServerLevel level = SelfTests.mega(server);
        if (level == null) return fail("no mega dimension");
        if (!SystemConfig.enabled()) return ok("skipped: system.enabled is false in the config");
        if (SystemData.quests().isEmpty()) return fail("no quest templates are loaded");
        FakePlayer p = SelfTests.player(level, "system_tester_quest", 0.5, 70, 0.5);
        reset(p);
        try {
            SystemState s = SystemQuests.state(p);
            MegaDungeon.fireFloorEntered(p, 1, 0);
            QuestProgress q1 = s.quest();
            if (q1 == null || !q1.isActive() || q1.floor() != 1) return fail("floor 1 did not start an ACTIVE quest on floor 1: " + describe(q1));
            if (s.maxFloor() != 1) return fail("the deepest floor should be 1 but is " + s.maxFloor());
            if (!SystemQuests.complete(p) || q1.state() != QuestProgress.State.DONE) return fail("completing the quest did not make it DONE: " + describe(q1));
            if (s.done() != 1) return fail("one quest is done but the counter says " + s.done());
            boolean paid = s.totalPoints() > 0 || s.cores() > 0;
            if (!paid) return fail("a completed quest paid no stat point and no Shadow Core");
            if (SystemQuests.complete(p)) return fail("a finished quest was completed a second time (it would pay twice)");
            int paidPoints = s.totalPoints(), paidCores = s.cores();

            MegaDungeon.fireFloorEntered(p, 2, 1);
            QuestProgress q2 = s.quest();
            if (q2 == null || q2 == q1 || !q2.isActive() || q2.floor() != 2) return fail("floor 2 did not start a new ACTIVE quest: " + describe(q2));
            if (q2.kind() == q1.kind() && SystemData.quests().ofKind(q1.kind()).size() < SystemData.quests().size()) return fail("two quests in a row of the same kind " + q1.kind());
            if (s.totalPoints() != paidPoints || s.cores() != paidCores) return fail("entering floor 2 paid something");

            MegaDungeon.fireFloorEntered(p, 3, 2);
            QuestProgress q3 = s.quest();
            if (q2.kind().completesOnArrival() ? q2.state() != QuestProgress.State.DONE : q2.state() != QuestProgress.State.FAILED)
                return fail("entering floor 3 with the floor 2 quest (" + q2.kind() + ") unfinished left it " + q2.state());
            if (q3 == null || q3 == q2 || !q3.isActive() || q3.floor() != 3) return fail("floor 3 did not start a new ACTIVE quest: " + describe(q3));

            MegaDungeon.fireFloorEntered(p, 2, 3);
            if (s.quest() != q3) return fail("a revisit of floor 2 changed the quest (it must start nothing: the depth was reached before)");

            MegaDungeon.fireFloorEntered(p, 10, 3);
            if (s.quest() != null) return fail("a boss floor (+10) has a quest: " + describe(s.quest()));
            if (s.maxFloor() != 10) return fail("the deepest floor should be 10 but is " + s.maxFloor());
            return ok("floor 1 ACTIVE, completed (paid " + (s.cores() > 0 ? "a core" : "a stat point") + "), floor 2 a new " + q2.kind() + ", floor 3 left it " + q2.state()
                    + " and started " + q3.kind() + ", a revisit started nothing, the boss floor +10 has no quest");
        } finally {
            reset(p);
        }
    }

    private static String describe(QuestProgress q) {
        return q == null ? "no quest" : q.id() + " " + q.kind() + " " + q.state() + " " + q.progress() + "/" + q.target() + " floor " + q.floor();
    }

    // ------------------------------------------------------------------------------------------------ stats
    private static SelfTests.Result stats(MinecraftServer server) {
        ServerLevel level = SelfTests.mega(server);
        if (level == null) return fail("no mega dimension");
        StatTable.Def str = SystemData.stats().def(Stat.STR);
        if (str == null || str.effects().isEmpty()) return fail("the stat table has no STR row");
        FakePlayer p = SelfTests.player(level, "system_tester_stats", 0.5, 70, 0.5);
        reset(p);
        try {
            SystemState s = SystemQuests.state(p);
            AttributeInstance atk = p.getAttribute(Attributes.ATTACK_DAMAGE);
            if (atk == null) return fail("the fake player has no attack damage attribute");
            StatTable.Effect eff = str.effects().get(0);
            if (!"minecraft:generic.attack_damage".equals(eff.attribute())) return fail("the first STR effect is " + eff.attribute() + ", this test expects attack damage");
            int cap = SystemConfig.get(SystemConfig.MAX_STAT_POINTS);
            int added = SystemRewards.giveStat(p, s, Stat.STR, 3, cap);
            if (added != 3 || s.points(Stat.STR) != 3) return fail("three points of STR were not added: " + s.points(Stat.STR));
            AttributeModifier m = atk.getModifier(SystemStats.modifierId(Stat.STR));
            if (m == null) return fail("no modifier with the fixed id " + SystemStats.modifierId(Stat.STR) + " after giving points");
            if (Math.abs(m.amount() - eff.amount(3)) > 1e-9) return fail("the modifier is " + m.amount() + " but three points are worth " + eff.amount(3));
            int before = atk.getModifiers().size();
            SystemStats.apply(p);
            SystemStats.apply(p, s);
            if (atk.getModifiers().size() != before) return fail("applying twice changed the number of modifiers from " + before + " to " + atk.getModifiers().size());
            double valueWith = atk.getValue();
            if (!(valueWith > atk.getBaseValue())) return fail("the attack damage did not rise with the modifier (" + valueWith + " against base " + atk.getBaseValue() + ")");
            s.addPoints(Stat.STR, 500, cap);
            SystemStats.apply(p, s);
            if (s.points(Stat.STR) != cap) return fail("the per-stat cap " + cap + " did not hold: " + s.points(Stat.STR));
            if (Math.abs(atk.getModifier(SystemStats.modifierId(Stat.STR)).amount() - eff.amount(cap)) > 1e-9) return fail("at the cap the modifier is not the table's ceiling value");
            if (s.addPoints(Stat.STR, 1, cap) != 0) return fail("a point was added above the cap");
            SystemStats.remove(p);
            if (atk.getModifier(SystemStats.modifierId(Stat.STR)) != null) return fail("remove() left the modifier on the attribute");
            SystemStats.apply(p, s);
            if (atk.getModifier(SystemStats.modifierId(Stat.STR)) == null) return fail("apply() after remove() did not bring the modifier back (the points are the truth)");
            return ok("3 STR -> modifier " + m.amount() + " with id " + SystemStats.modifierId(Stat.STR) + ", idempotent over " + before + " modifiers, cap " + cap + " holds, remove and re-apply work");
        } finally {
            reset(p);
        }
    }

    // ------------------------------------------------------------------------------------------------ extraction
    private static SelfTests.Result extract(MinecraftServer server) {
        ServerLevel level = SelfTests.mega(server);
        if (level == null) return fail("no mega dimension");
        Sector s0 = MegaData.get(server).sector(0);
        if (s0 == null || s0.rooms.isEmpty()) return fail("sector 0 does not exist: run /multiverse mega expand first");
        if (!SystemConfig.enabled() || SystemConfig.get(SystemConfig.MAX_MINIONS) < 1) return ok("skipped: the System is off or maxMinions is 0 in the config");
        Room room = s0.rooms.get(0);
        BlockPos at = room.center();
        Polarity pol = MegaDungeon.polarityAt(level, at);
        if (pol == null || !SystemQuests.covers(pol)) return fail("the centre of room #" + room.id + " of sector 0 is on the road " + pol + ", which the System does not cover");
        level.getChunk(at.getX() >> 4, at.getZ() >> 4);
        FakePlayer p = SelfTests.player(level, "system_tester_extract", at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        reset(p);
        ShadowExtraction.clear();
        ShadowMinions.dismissAll(server, p.getUUID());
        List<Mob> toClean = new ArrayList<>();
        try {
            DamageSource byPlayer = level.damageSources().playerAttack(p);
            Zombie z = zombie(level, at, 3.5);
            if (!ShadowExtraction.record(z, byPlayer)) return fail("a zombie slain by the player in sector 0 was not remembered: " + ShadowExtraction.refusal(z, true));
            Zombie named = zombie(level, at, 3.5);
            named.setCustomName(net.minecraft.network.chat.Component.literal("Somebody"));
            Zombie persistent = zombie(level, at, 3.5);
            persistent.setPersistenceRequired();
            Zombie servant = zombie(level, at, 3.5);
            servant.addTag(ShadowMinions.TAG);
            Zombie tagged = zombie(level, at, 3.5);
            tagged.addTag("nusmp_wanderer");
            int before = ShadowExtraction.remembered();
            if (ShadowExtraction.record(named, byPlayer) || ShadowExtraction.record(persistent, byPlayer) || ShadowExtraction.record(servant, byPlayer)
                    || ShadowExtraction.record(tagged, byPlayer)) return fail("a named, persistent, servant or wandering-boss zombie was remembered");
            if (ShadowExtraction.record(zombie(level, at, 3.5), level.damageSources().generic())) return fail("a death no player caused was remembered");
            if (ShadowExtraction.remembered() != before) return fail("refused creatures were written into the ledger");
            if (ShadowExtraction.pending(p) == null) return fail("the zombie that fell 3.5 blocks away is not pending for the player");

            ItemStack core = ShadowCoreItem.create();
            p.setItemInHand(InteractionHand.MAIN_HAND, core);
            if (ShadowCoreItem.charges(core) != CoreCharges.START) return fail("a new core has " + ShadowCoreItem.charges(core) + " charges");
            ShadowExtraction.Result r = ShadowExtraction.use(p, core);
            if (r != ShadowExtraction.Result.RAISED) return fail("using the core next to a fresh kill gave " + r);
            List<Mob> shadows = ShadowMinions.of(server, p.getUUID());
            toClean.addAll(shadows);
            if (shadows.size() != 1) return fail("expected one shadow, found " + shadows.size());
            Mob shadow = shadows.get(0);
            if (!shadow.getTags().contains("nusmp_servant")) return fail("the shadow lacks the entity tag nusmp_servant");
            if (!p.getUUID().equals(ShadowMinions.ownerOf(shadow)) || !p.getUUID().equals(ShadowMinions.principal(shadow))) return fail("the shadow's owner is not the player");
            if (shadow.getType() != EntityType.ZOMBIE) return fail("the shadow is a " + shadow.getType() + ", not a zombie");
            double want = z.getMaxHealth() * SystemConfig.get(SystemConfig.MINION_STRENGTH);
            if (Math.abs(shadow.getMaxHealth() - want) > 0.01) return fail("the shadow has " + shadow.getMaxHealth() + " health, expected " + want);
            if (!ShadowMinions.isServant(shadow)) return fail("the faction rules would not recognise the shadow as a servant");
            if (ShadowCoreItem.charges(core) != CoreCharges.START - 1) return fail("the core has " + ShadowCoreItem.charges(core) + " charges after one extraction");
            if (ShadowExtraction.use(p, core) != ShadowExtraction.Result.NOTHING) return fail("using the core with nothing fallen did not report NOTHING");
            if (ShadowCoreItem.charges(core) != CoreCharges.START - 1) return fail("a failed use spent a charge");

            // the cap: a second and third shadow, then a fourth releases the first
            int cap = SystemConfig.get(SystemConfig.MAX_MINIONS);
            ItemStack spare = ShadowCoreItem.create();
            p.setItemInHand(InteractionHand.MAIN_HAND, spare);
            int raised = 1;
            for (int i = 0; i < cap; i++) {
                ShadowExtraction.record(zombie(level, at, 2.0 + i * 0.1), byPlayer);
                if (ShadowExtraction.use(p, spare) == ShadowExtraction.Result.RAISED) raised++;
            }
            List<Mob> now = ShadowMinions.of(server, p.getUUID());
            toClean.addAll(now);
            if (now.size() != Math.min(raised, cap)) return fail("after " + raised + " extractions the cap " + cap + " allows " + Math.min(raised, cap) + " shadows but " + now.size() + " exist");
            if (raised > cap && now.contains(shadow)) return fail("the oldest shadow was not released when the cap was passed");

            // a spent core is gone
            ItemStack last = ShadowCoreItem.create(1);
            p.setItemInHand(InteractionHand.MAIN_HAND, last);
            ShadowExtraction.record(zombie(level, at, 2.0), byPlayer);
            ShadowExtraction.use(p, last);
            toClean.addAll(ShadowMinions.of(server, p.getUUID()));
            if (!last.isEmpty()) return fail("a core with one charge left was not used up by its last extraction");
            return ok("a zombie slain by the player became a shadow (tag nusmp_servant, health " + shadow.getMaxHealth() + " of " + z.getMaxHealth() + "); named, persistent, servant, wandering-boss and non-player kills were refused; the charge was spent; the cap " + cap + " held; the last charge used the core up");
        } finally {
            for (Mob m : toClean) ShadowMinions.dismiss(m, false);
            ShadowMinions.dismissAll(server, p.getUUID());
            ShadowExtraction.clear();
            reset(p);
        }
    }

    private static Zombie zombie(ServerLevel level, BlockPos at, double dx) {
        Zombie z = EntityType.ZOMBIE.create(level);
        if (z == null) throw new IllegalStateException("a test zombie could not be created");
        z.moveTo(at.getX() + dx, at.getY(), at.getZ() + 0.5, 0f, 0f);
        return z;
    }
}
