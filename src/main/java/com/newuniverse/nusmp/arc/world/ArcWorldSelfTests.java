package com.newuniverse.nusmp.arc.world;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.arc.ArcSpawner;
import com.newuniverse.nusmp.arc.arena.ArcArena;
import com.newuniverse.nusmp.arc.arena.ArcArenaBuilder;
import com.newuniverse.nusmp.arc.arena.ArcArenaSave;
import com.newuniverse.nusmp.arc.arena.ArenaGrid;
import com.newuniverse.nusmp.arc.arena.ArenaPart;
import com.newuniverse.nusmp.arc.arena.ArenaShape;
import com.newuniverse.nusmp.arc.arena.ArenaShapes;
import com.newuniverse.nusmp.arc.arena.BlockSpec;
import com.newuniverse.nusmp.arc.arena.ChroniclePick;
import com.newuniverse.nusmp.arc.arena.ChronicleView;
import com.newuniverse.nusmp.arc.hub.ChronicleSave;
import com.newuniverse.nusmp.arc.hub.ChronicleSite;
import com.newuniverse.nusmp.arc.hub.HubPosts;
import com.newuniverse.nusmp.arc.hub.SitePart;
import com.newuniverse.nusmp.arc.story.ArcStory;
import com.newuniverse.nusmp.arc.story.StoryData;
import com.newuniverse.nusmp.boss.BossEntities;
import com.newuniverse.nusmp.boss.BossRegistry;
import com.newuniverse.nusmp.charter.CharterTable;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Why: nothing of the places where the characters appear can be played by its author, so each part has a SELF-TEST that the CI server smoke test runs in the real
 * game ({@code /multiverse mega selftest}, keys {@code arcworld.*}; add the prefix {@code arcworld} to the smoke script's expected list): the seven older bosses
 * are in the roster and resolve to real entity types, the registry and the Charter table; the floor-boss choice is deterministic on the live roster and falls back
 * to the plain Guardian whenever it should; every block of every arena and of the Hub's site exists in the game and the arena dimension is loaded; the arena
 * save and the Hub's record survive a round trip through a tag; the Chronicle reads the live roster; the Hub's posts take every friend of the roster; a saga's
 * reward is paid once, to a stand-in player, with its book; and a boss of the older kind can be made and is recognised by the boss registry. With an empty
 * roster the tests still pass and say so. A test that spawns something removes it again, and the stand-in player's story is put back.
 */
public final class ArcWorldSelfTests {
    private ArcWorldSelfTests() {}

    private static final String PLAYER = "arcworld_tester";

    public static void register() {
        SelfTests.register("arcworld.roster", ArcWorldSelfTests::roster);
        SelfTests.register("arcworld.floorboss", ArcWorldSelfTests::floorBoss);
        SelfTests.register("arcworld.arena", ArcWorldSelfTests::arena);
        SelfTests.register("arcworld.chronicle", ArcWorldSelfTests::chronicle);
        SelfTests.register("arcworld.hub", ArcWorldSelfTests::hub);
        SelfTests.register("arcworld.reward", ArcWorldSelfTests::reward);
        SelfTests.register("arcworld.spawn", ArcWorldSelfTests::spawn);
    }

    private static SelfTests.Result ok(String detail) { return SelfTests.Result.ok(detail); }

    private static SelfTests.Result fail(String detail) { return SelfTests.Result.fail(detail); }

    private static SelfTests.Result problems(List<String> p) {
        return fail(p.size() + " problem(s): " + String.join(" | ", p.subList(0, Math.min(6, p.size()))));
    }

    // ================================================================ roster
    private static SelfTests.Result roster(MinecraftServer server) {
        ServerLevel level = server.overworld();
        List<String> bad = new ArrayList<>();
        for (ArcCharacter c : ExistingBosses.all()) {
            ArcCharacter in = ArcRoster.byId(c.id());
            if (in == null) { bad.add(c.id() + " is not in the roster (ArcCharacters.bootstrap or ArcWorldSystem.init did not register it)"); continue; }
            if (!in.equals(c)) bad.add(c.id() + ": the roster holds another record under this id");
            if (!BossRegistry.known(c.id())) bad.add(c.id() + " is not in the boss registry");
            if (c.hasCharter() && CharterTable.byId(c.charterId()) == null) bad.add(c.id() + " declares a grimoire the Charter table does not have");
            EntityType<?> type = ExistingBossSpawner.typeOf(c.id());
            if (type == null) { bad.add(c.id() + " has no entity type (nusmp:" + ExistingBosses.entityPath(c.id()) + ")"); continue; }
            var probe = type.create(level);
            if (!(probe instanceof ZogratisBoss)) bad.add(c.id() + ": its entity is not a boss of the framework");
            else if (!c.id().equals(BossEntities.bossId(probe))) bad.add(c.id() + ": the boss registry does not recognise its entity");
            if (probe != null) probe.discard();
        }
        if (!bad.isEmpty()) return problems(bad);
        return ok(ExistingBosses.all().size() + " older bosses are characters, with entity types, registry ids and (where they had one) Charter rows");
    }

    // ================================================================ floor boss
    private static SelfTests.Result floorBoss(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        int hosted = 0, plain = 0;
        for (Polarity road : Polarity.values()) {
            for (int floor = 10; floor <= 100; floor += 10) {
                List<ArcCharacter> standing = ArcRoster.floorBosses(floor, road);
                FloorBossChoice.Request r = new FloorBossChoice.Request(0xA5A5L, 7, floor, road, "radiant", 1.0, true, standing, c -> true);
                ArcCharacter a = FloorBossChoice.choose(r), b = FloorBossChoice.choose(r);
                if (!Objects.equals(a, b)) bad.add("floor " + floor + " " + road + ": the choice is not deterministic");
                if (a != null && !a.standsOn(floor, road)) bad.add(a.id() + " was chosen for floor " + floor + " " + road + " but may not stand there");
                if (a != null && standing.isEmpty()) bad.add("floor " + floor + " " + road + ": a boss with an empty roster");
                if (FloorBossChoice.choose(new FloorBossChoice.Request(0xA5A5L, 7, floor, road, "radiant", 0.0, true, standing, c -> true)) != null) bad.add("chance 0 still hosted a boss");
                if (FloorBossChoice.choose(new FloorBossChoice.Request(0xA5A5L, 7, floor, road, "radiant", 1.0, false, standing, c -> true)) != null) bad.add("a disabled layer still hosted a boss");
                if (FloorBossChoice.choose(new FloorBossChoice.Request(0xA5A5L, 7, floor, road, "radiant", 1.0, true, standing, c -> false)) != null) bad.add("a locked story still let a boss through");
                if (a == null) plain++;
                else hosted++;
            }
        }
        if (!bad.isEmpty()) return problems(bad);
        return ok(hosted + " of " + (hosted + plain) + " decennial floors would host an arc boss at chance 1 (the rest keep the Guardian); deterministic, gated, switchable");
    }

    // ================================================================ arena
    private static SelfTests.Result arena(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        if (server.getLevel(ArcArena.DIMENSION) == null) bad.add("the dimension " + ArcArena.DIMENSION.location() + " is not loaded (data/nusmp/dimension/arc_arena.json and dimension_type/arc_arena.json)");
        int blocks = 0;
        for (ArenaShape s : ArenaShapes.all()) {
            for (Map.Entry<ArenaPart, String> e : s.palette().entrySet()) {
                BlockSpec spec = BlockSpec.parse(e.getValue());
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(spec.namespace(), spec.path());
                if (!BuiltInRegistries.BLOCK.containsKey(id)) { bad.add(s.id() + " " + e.getKey() + ": no block " + id); continue; }
                BlockState st = ArcArenaBuilder.stateOf(e.getValue());
                for (String prop : spec.properties().keySet()) {
                    if (st.getProperties().stream().noneMatch(p -> p.getName().equals(prop))) bad.add(s.id() + " " + e.getKey() + ": " + id + " has no property " + prop);
                }
                blocks++;
            }
            if (s.sigils().length == 0) bad.add(s.id() + " has no way home");
        }
        for (ArcCharacter c : ArcRoster.bosses()) if (ArenaShapes.forCharacter(c) == null) bad.add(c.id() + " has no arena");
        // the save survives a round trip through a tag
        ArcArenaSave save = new ArcArenaSave();
        int a = save.slotFor("selftest_a"), b = save.slotFor("selftest_b");
        if (a != 0 || b != 1 || save.slotFor("selftest_a") != 0) bad.add("slots are not given first-free and kept");
        save.markBuilt(a, ArcArenaSave.BUILD_VERSION);
        save.state(b).defeat(1000L, 500L);
        java.util.UUID boss = java.util.UUID.nameUUIDFromBytes("arcworld-selftest".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        save.bossId(b, boss);
        save.pick(boss, "selftest_a");
        CompoundTag tag = save.save(new CompoundTag(), server.registryAccess());
        ArcArenaSave back = ArcArenaSave.load(tag, server.registryAccess());
        if (back.slotOf("selftest_b") != 1 || back.builtVersion(0) != ArcArenaSave.BUILD_VERSION || !boss.equals(back.bossId(1)) || !"selftest_a".equals(back.pick(boss))
                || back.state(1).phase() != com.newuniverse.nusmp.arena.ArenaState.Phase.COOLDOWN) bad.add("the arena save changed on the way through its tag");
        if (!bad.isEmpty()) return problems(bad);
        return ok(ArenaShapes.all().size() + " arenas, " + blocks + " palette blocks exist, the dimension is loaded, the save round-trips; " + ArenaGrid.MAX_SLOTS + " sites");
    }

    // ================================================================ chronicle
    private static SelfTests.Result chronicle(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        for (Map.Entry<SitePart, String> e : ChronicleSite.palette().entrySet()) {
            BlockSpec spec = BlockSpec.parse(e.getValue());
            if (!BuiltInRegistries.BLOCK.containsKey(ResourceLocation.fromNamespaceAndPath(spec.namespace(), spec.path()))) bad.add(e.getKey() + ": no block " + spec.path());
        }
        List<ChronicleView.Row> fresh = ChronicleView.rows(ArcRoster.all(), new StoryData(), true);
        if (fresh.size() != ArcRoster.bosses().size()) bad.add("the Chronicle lists " + fresh.size() + " bosses of " + ArcRoster.bosses().size());
        int open = 0;
        for (ChronicleView.Row r : fresh) if (r.state() == ChronicleView.State.OPEN) open++;
        if (!fresh.isEmpty() && open == 0) bad.add("a fresh story has no boss open: the chain has no start");
        String pick = ChroniclePick.choose(fresh, null, true);
        if (!fresh.isEmpty() && pick == null) bad.add("a fresh story has nobody to be carried to");
        if (pick != null && ArcRoster.byId(pick) == null) bad.add("the pick " + pick + " is not in the roster");
        List<ChronicleView.Row> open2 = ChronicleView.rows(ArcRoster.all(), new StoryData(), false);
        for (ChronicleView.Row r : open2) if (r.state() != ChronicleView.State.OPEN) bad.add(r.id() + " is not open with the gate off");
        if (!bad.isEmpty()) return problems(bad);
        return ok(fresh.size() + " bosses on " + ChronicleView.pages(fresh).size() + " pages, " + open + " open to a newcomer, the lectern is a lectern, the site's blocks exist");
    }

    // ================================================================ hub
    private static SelfTests.Result hub(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        ServerLevel hub = server.getLevel(Labyrinth.SANCTUARY_DIMENSION);
        if (hub == null) return fail("the Sanctuary dimension is not loaded");
        List<String> ids = new ArrayList<>();
        for (ArcCharacter c : ArcRoster.npcs()) ids.add(c.id());
        HubPosts.Assignment a = HubPosts.assign(ids, ArcWorldConfig.hubGuide(), ChronicleSite.posts().size());
        if (!a.unplaced().isEmpty()) bad.add(a.unplaced().size() + " friendly characters found no post: " + a.unplaced());
        if (ChronicleSite.posts().size() < 40) bad.add("the Promenade has only " + ChronicleSite.posts().size() + " posts");
        ChronicleSave s = new ChronicleSave();
        s.version(3);
        CompoundTag t = s.save(new CompoundTag(), server.registryAccess());
        if (t.getInt("Version") != 3) bad.add("the Hub's record does not write its version");
        if (!bad.isEmpty()) return problems(bad);
        ChronicleSave live = ChronicleSave.get(hub);
        return ok(ids.size() + " friendly characters, " + ChronicleSite.posts().size() + " posts, the site is " + (live.version() >= com.newuniverse.nusmp.arc.hub.ChronicleBuilder.BUILD_VERSION ? "laid" : "not laid yet (it is laid on the first visit)"));
    }

    // ================================================================ reward
    private static SelfTests.Result reward(MinecraftServer server) {
        if (!ArcWorldConfig.sagaRewards()) return ok("skipped: arc.sagaRewards is off");
        ServerLevel level = server.overworld();
        ArcSaga saga = null;
        for (ArcSaga s : ArcSaga.values()) if (!SagaRewards.reward(s).isEmpty() && !SagaRewards.bossesOf(s, ArcRoster.all()).isEmpty()) { saga = s; break; }
        if (saga == null) return ok("skipped: no saga with bosses and a reward is in the roster");
        FakePlayer fp = SelfTests.player(level, PLAYER, 0.5, 100, 0.5);
        StoryData saved = ArcStory.data(fp).copy();
        try {
            ArcStory.reset(fp);
            fp.getInventory().clearContent();
            for (ArcCharacter b : SagaRewards.bossesOf(saga, ArcRoster.all())) ArcStory.defeated(fp, b.id());
            int paid = ArcWorldCredit.settle(fp);
            if (paid < 1) return fail("clearing " + saga.id() + " paid nothing");
            if (ArcWorldCredit.settle(fp) != 0) return fail("the reward of " + saga.id() + " was paid twice");
            if (!SagaRewards.paid(ArcStory.data(fp), saga)) return fail("the story does not remember that " + saga.id() + " was paid");
            boolean book = false;
            for (int i = 0; i < fp.getInventory().getContainerSize(); i++) if (fp.getInventory().getItem(i).is(Items.WRITTEN_BOOK)) book = true;
            if (!book) return fail("the commemorative book was not handed over");
            for (SagaRewards.Stack st : SagaRewards.reward(saga)) {
                ResourceLocation id = ResourceLocation.tryParse(st.item());
                if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return fail("the reward names the item " + st.item() + ", which does not exist");
                int have = 0;
                for (int i = 0; i < fp.getInventory().getContainerSize(); i++) {
                    ItemStack in = fp.getInventory().getItem(i);
                    if (BuiltInRegistries.ITEM.getKey(in.getItem()).equals(id)) have += in.getCount();
                }
                if (have < st.count()) return fail("only " + have + " of " + st.count() + " " + st.item() + " reached the pack");
            }
            return ok(saga.id() + " paid its reward and its book once, and remembers it");
        } finally {
            fp.getInventory().clearContent();
            StoryData live = ArcStory.data(fp);
            live.clear();
            for (String s : saved.defeatedList()) live.defeat(s);
            for (String s : saved.metList()) live.meet(s);
            for (Map.Entry<String, Integer> e : saved.questMap().entrySet()) live.setQuestStage(e.getKey(), e.getValue());
            live.setSaga(saved.saga());
        }
    }

    // ================================================================ spawn
    private static SelfTests.Result spawn(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos at = level.getSharedSpawnPos();
        if (!level.hasChunkAt(at)) return ok("skipped: the spawn chunk is not loaded");
        ZogratisBoss boss = null;
        try {
            ExistingBossSpawner.Made m = ExistingBossSpawner.make(level, ExistingBosses.GOLIATH, Vec3.atBottomCenterOf(at.above(2)), 0f, false);
            boss = m.boss();
            if (m.outcome() != ArcSpawner.Outcome.SPAWNED || boss == null) return fail("making the Goliath gave " + m.outcome());
            if (!"goliath".equals(BossEntities.bossId(boss))) return fail("the made Goliath is not recognised by the boss registry");
            if (!boss.arena().equals(at.above(2))) return fail("the made Goliath's arena is not where it was made");
            if (ExistingBossSpawner.standing(level, "goliath", boss.position()).isEmpty()) return fail("the made Goliath is not found standing");
            ArcSpawner.Result r = ExistingBossSpawner.summon(level, ExistingBosses.GOLIATH, boss.position(), 0f, List.of(), false);
            if (r.outcome() == ArcSpawner.Outcome.SPAWNED) return fail("a second Goliath was made at the same place without an operator's force");
            return ok("the Goliath is made, recognised, found, and not made twice");
        } finally {
            if (boss != null) boss.discard();
        }
    }
}
