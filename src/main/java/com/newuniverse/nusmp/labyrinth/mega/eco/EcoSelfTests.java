package com.newuniverse.nusmp.labyrinth.mega.eco;

import com.newuniverse.nusmp.dungeon.DungeonFood;
import com.newuniverse.nusmp.labyrinth.dnd.DndConfig;
import com.newuniverse.nusmp.labyrinth.mega.Marker;
import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Why: nothing in this package can be played by its author, so each part of it has a SELF-TEST that the CI server smoke test runs in the real game
 * ({@code /multiverse mega selftest}), against sector 0 that the smoke script has just generated. They drive the same public entry points the event
 * handlers use, and they are written to fail loudly with the reason: {@code eco.tags} (the eleven entity-type tags resolve and hold registered
 * creatures), {@code eco.factions} (the faction files loaded through the real data reload, the rivalry graph mirrors, the tags classify a zombie as
 * undead and a slime as a slime), {@code eco.native} (after sector 0 was generated every non-safe, non-circulation room has a native faction and honours
 * its template's marker, and safe rooms and corridors have none), {@code eco.target_guard} (the real {@code LivingChangeTargetEvent} cancels a target
 * change to the same faction or an ally, and lets a rival, a player and a servant's target through), {@code eco.turfwar} (a zombie and a vindicator, two
 * rival factions, ten blocks apart with no player near: the join event gives them the goal and, ticked one tick at a time, one of them targets the
 * other; a zombie and a skeleton, one faction, stay at peace), {@code eco.player_priority} (the same pair with a player six blocks away: nothing
 * happens; once the player is gone they fight) and {@code eco.drops} (a killed zombie in sector 0 run through {@link FactionDrops}: the meat and the
 * stone bonus appear, never above their caps, and a death no player caused pays nothing). The mobs fight in an ARENA IN THE AIR above sector 0's
 * entry (no gravity, no blocks changed, nothing of the dungeon touched), because a self-test must not depend on what the templates put where, and
 * they are ticked by hand ({@code tickCount} up, then {@code tick()}), because the server clock does not advance inside one command. Every spawned
 * entity is discarded and the fake player is taken out of the level again, whatever happens.
 */
public final class EcoSelfTests {
    private EcoSelfTests() {}

    private static final String PLAYER = "eco_tester";
    private static final Set<String> STONES = Set.of(FactionLoot.STONE_PATH, FactionLoot.LOW_PATH, FactionLoot.MEDIUM_PATH, FactionLoot.HIGH_PATH);

    /** Registers the tests; called once from {@code EcoRegistry.init}. */
    public static void register() {
        SelfTests.register("eco.tags", EcoSelfTests::tags);
        SelfTests.register("eco.factions", EcoSelfTests::factions);
        SelfTests.register("eco.native", EcoSelfTests::nativeRooms);
        SelfTests.register("eco.turfwar", EcoSelfTests::turfWar);
        SelfTests.register("eco.player_priority", EcoSelfTests::playerPriority);
        SelfTests.register("eco.target_guard", EcoSelfTests::targetGuard);
        SelfTests.register("eco.drops", EcoSelfTests::drops);
    }

    private static SelfTests.Result ok(String detail) { return SelfTests.Result.ok(detail); }

    private static SelfTests.Result fail(String detail) { return SelfTests.Result.fail(detail); }

    private static Sector sector0(MinecraftServer server) { return MegaData.get(server).sector(0); }

    private static String firstFew(List<String> problems) {
        return problems.size() + " problem(s): " + String.join(" | ", problems.subList(0, Math.min(4, problems.size())));
    }

    /** The common start of the tests that need the dungeon: null when the test may go on, else the result to return (a failure, or a skip because a switch is off). */
    private static SelfTests.Result gate(MinecraftServer server, boolean turfWars) {
        ServerLevel level = SelfTests.mega(server);
        if (level == null) return fail("no mega dimension");
        if (sector0(server) == null) return fail("sector 0 does not exist: run /multiverse mega expand first");
        if (turfWars && !com.newuniverse.nusmp.labyrinth.mega.poise.Passivity.turfWarsRun()) return ok("skipped: the turf wars are off in this config (eco.turfWarsEnabled is false, or foundry.poise.dungeonPassive keeps the dungeon passive)");
        if (turfWars && level.getDifficulty() == Difficulty.PEACEFUL) return ok("skipped: the difficulty is peaceful, hostile mobs would be removed at once");
        return null;
    }

    // ------------------------------------------------------------------------------------------------ data
    private static SelfTests.Result tags(MinecraftServer server) {
        List<String> empty = new ArrayList<>();
        StringBuilder counts = new StringBuilder();
        for (String id : FactionIds.CONTRACT) {
            TagKey<EntityType<?>> key = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("nusmp", "mega/" + id));
            int n = BuiltInRegistries.ENTITY_TYPE.getTag(key).map(HolderSet::size).orElse(0);
            if (counts.length() > 0) counts.append(' ');
            counts.append(id).append('=').append(n);
            if (n < 1) empty.add(id);
        }
        return empty.isEmpty() ? ok("registered members per tag: " + counts) : fail("no registered member in the tag(s) of " + empty + " (" + counts + ")");
    }

    private static SelfTests.Result factions(MinecraftServer server) {
        FactionGraph g = FactionRegistry.graph();
        List<String> bad = new ArrayList<>();
        for (String id : FactionIds.CONTRACT) if (!g.knows(id)) bad.add("faction " + id + " did not load");
        for (String id : g.ids()) {
            FactionDef d = g.def(id);
            for (String r : d.rivals()) if (g.knows(r) && !d.oneWay() && !g.hostile(r, id)) bad.add(id + " lists rival " + r + " but " + r + " does not hunt back");
            for (String a : d.allies()) if (g.knows(a) && !g.allied(a, id)) bad.add(id + " lists ally " + a + " but the alliance is not mutual");
        }
        bad.addAll(g.problems());
        if (!g.hostile("undead", "goblin") || !g.hostile("goblin", "undead")) bad.add("undead and goblin are not rivals both ways");
        if (!g.allied("cultist", "demon")) bad.add("cultist and demon are not allied");
        Map<EntityType<?>, String> expected = new LinkedHashMap<>();
        expected.put(EntityType.ZOMBIE, "undead");
        expected.put(EntityType.SLIME, "slime");
        expected.put(EntityType.SPIDER, "insect");
        expected.put(EntityType.VINDICATOR, "goblin");
        expected.put(EntityType.WITCH, "cultist");
        expected.put(EntityType.BLAZE, "elemental");
        for (Map.Entry<EntityType<?>, String> e : expected.entrySet()) {
            String got = FactionRegistry.factionOf(e.getKey());
            if (!e.getValue().equals(got)) bad.add(BuiltInRegistries.ENTITY_TYPE.getKey(e.getKey()) + " should be " + e.getValue() + " but is " + got);
        }
        if (FactionRegistry.factionOf(EntityType.PIG) != null) bad.add("a pig has a faction");
        if (FactionRegistry.factionOf(EntityType.PLAYER) != null) bad.add("the player has a faction");
        return bad.isEmpty() ? ok(g.size() + " factions loaded, rivalry mirrored, zombie=undead slime=slime vindicator=goblin") : fail(firstFew(bad));
    }

    private static SelfTests.Result nativeRooms(MinecraftServer server) {
        SelfTests.Result stop = gate(server, false);
        if (stop != null) return stop;
        Sector s = sector0(server);
        if (s.state == Sector.State.GENERATING) return fail("sector 0 is still generating");
        FactionGraph g = FactionRegistry.graph();
        if (g.size() == 0) return fail("no faction is loaded");
        List<Marker> marks = s.markers(MarkerKind.FACTION);
        List<String> bad = new ArrayList<>();
        Map<String, Integer> used = new LinkedHashMap<>();
        int eligible = 0, unowned = 0, byMarker = 0;
        for (Room r : s.rooms) {
            if (NativeFactions.eligible(r.kind)) {
                eligible++;
                if (r.faction == null || !g.knows(r.faction)) {
                    bad.add("room #" + r.id + " (" + r.template + ", " + r.kind + ") has no native faction");
                    continue;
                }
                used.merge(r.faction, 1, Integer::sum);
                String hint = null;
                for (Marker m : marks) {
                    if (r.contains(m.pos())) {
                        hint = m.arg("faction", "").trim().toLowerCase(Locale.ROOT);
                        break;
                    }
                }
                if (hint != null && g.knows(hint)) {
                    byMarker++;
                    if (!hint.equals(r.faction)) bad.add("room #" + r.id + " (" + r.template + ") has a marker for " + hint + " but is native to " + r.faction);
                }
            } else {
                unowned++;
                if (r.faction != null) bad.add("room #" + r.id + " (" + r.template + ", " + r.kind + ") must have no native faction but has " + r.faction);
            }
        }
        if (eligible == 0) return fail("sector 0 has no room that should get a native faction (" + s.rooms.size() + " rooms)");
        if (eligible >= 12 && used.size() < 2) bad.add("all " + eligible + " rooms got the same faction: " + used);
        return bad.isEmpty() ? ok(eligible + " rooms native (" + byMarker + " by template marker), " + unowned + " safe or corridor rooms with none; " + used)
                : fail(firstFew(bad));
    }

    // ------------------------------------------------------------------------------------------------ the target guard
    private static SelfTests.Result targetGuard(MinecraftServer server) {
        SelfTests.Result stop = gate(server, true);
        if (stop != null) return stop;
        ServerLevel level = SelfTests.mega(server);
        Zombie zombie = EntityType.ZOMBIE.create(level);
        Skeleton skeleton = EntityType.SKELETON.create(level);
        Vindicator vindicator = EntityType.VINDICATOR.create(level);
        Witch witch = EntityType.WITCH.create(level);
        Vex vex = EntityType.VEX.create(level);
        if (zombie == null || skeleton == null || vindicator == null || witch == null || vex == null) return fail("a test mob could not be created");
        ServerPlayer player = SelfTests.player(level, PLAYER, 0, 110, 0);
        List<String> bad = new ArrayList<>();

        zombie.setTarget(skeleton);
        if (zombie.getTarget() != null) bad.add("zombie -> skeleton (the same faction) was not cancelled");
        zombie.setTarget(vindicator);
        if (zombie.getTarget() != vindicator) bad.add("zombie -> vindicator (rival factions) was cancelled");
        zombie.setTarget(null);
        if (zombie.getTarget() != null) bad.add("clearing a target was refused");
        witch.setTarget(vex);
        if (witch.getTarget() != null) bad.add("witch (cultist) -> vex (demon, an ally) was not cancelled");
        zombie.setTarget(player);
        if (zombie.getTarget() != player) bad.add("zombie -> player was cancelled: a player target must never be");
        zombie.setTarget(null);
        zombie.getPersistentData().putBoolean("nusmp_shadow", true);
        zombie.setTarget(skeleton);
        if (zombie.getTarget() != skeleton) bad.add("a servant (shadow soldier) zombie was shielded from hunting a skeleton");
        zombie.setTarget(null);
        zombie.getPersistentData().remove("nusmp_shadow");
        return bad.isEmpty() ? ok("same faction and ally cancelled; rival, player, clearing and a servant's target pass") : fail(firstFew(bad));
    }

    // ------------------------------------------------------------------------------------------------ the turf wars
    private static SelfTests.Result turfWar(MinecraftServer server) {
        SelfTests.Result stop = gate(server, true);
        if (stop != null) return stop;
        FactionGraph g = FactionRegistry.graph();
        if (!g.hostile("undead", "goblin") || !g.hostile("goblin", "undead")) return fail("the faction table no longer makes undead and goblin rivals: this test needs the pair zombie / vindicator");
        ServerLevel level = SelfTests.mega(server);
        try (Arena arena = new Arena(level, sector0(server))) {
            if (level.getNearestPlayer(arena.x, arena.y, arena.z, 64.0, false) != null) return ok("skipped: a player is within 64 blocks of the test arena");
            Zombie zombie = arena.spawn(EntityType.ZOMBIE, 0);
            Vindicator vindicator = arena.spawn(EntityType.VINDICATOR, 10);
            arena.ensureVisible(zombie);
            if (!TurfWars.hasGoal(zombie) || !TurfWars.hasGoal(vindicator))
                return fail("the join event did not give the turf-war goal (zombie " + TurfWars.hasGoal(zombie) + ", vindicator " + TurfWars.hasGoal(vindicator) + "): the mob's target selector could not be read or the faction is unknown");
            int ticks = fight(zombie, vindicator, 200);
            if (ticks < 0) return fail("200 ticks, no player near, and neither the zombie (target " + zombie.getTarget() + ") nor the vindicator (target " + vindicator.getTarget() + ") went for the other");
            String who = zombie.getTarget() == vindicator ? "the zombie" : "the vindicator";
            arena.clear();
            Zombie sameA = arena.spawn(EntityType.ZOMBIE, 0);
            Skeleton sameB = arena.spawn(EntityType.SKELETON, 10);
            arena.ensureVisible(sameA);
            if (!TurfWars.hasGoal(sameA) || !TurfWars.hasGoal(sameB)) return fail("the control pair (zombie, skeleton) did not get the goal");
            if (fight(sameA, sameB, 100) >= 0) return fail("a zombie and a skeleton, one faction, went for each other (targets " + sameA.getTarget() + " / " + sameB.getTarget() + ")");
            return ok("zombie (undead) and vindicator (goblin), 10 blocks apart: " + who + " targeted the other after " + ticks + " ticks; a zombie and a skeleton (one faction) stayed at peace for 100 ticks");
        }
    }

    private static SelfTests.Result playerPriority(MinecraftServer server) {
        SelfTests.Result stop = gate(server, true);
        if (stop != null) return stop;
        ServerLevel level = SelfTests.mega(server);
        if (EcoConfig.get(EcoConfig.PLAYER_YIELD) < 8) return ok("skipped: eco.playerYieldRadius is below 8 blocks");
        try (Arena arena = new Arena(level, sector0(server))) {
            Zombie zombie = arena.spawn(EntityType.ZOMBIE, 0);
            Vindicator vindicator = arena.spawn(EntityType.VINDICATOR, 10);
            arena.ensureVisible(zombie);
            ServerPlayer player = SelfTests.player(level, PLAYER, arena.x + 5.5, arena.y, arena.z + 5.5);
            arena.listPlayer(player);
            double gap = Math.sqrt(player.distanceToSqr(zombie));
            if (fight(zombie, vindicator, 100) >= 0)
                return fail("the zombie and the vindicator went for each other although a player stood " + String.format(Locale.ROOT, "%.1f", gap) + " blocks away (targets " + zombie.getTarget() + " / " + vindicator.getTarget() + ")");
            arena.unlistPlayer();
            zombie.setTarget(null);
            vindicator.setTarget(null);
            int later = fight(zombie, vindicator, 200);
            if (later < 0) return fail("the pair never fought even after the player left, so the quiet before proves nothing");
            return ok("no quarrel for 100 ticks with a player " + String.format(Locale.ROOT, "%.1f", gap) + " blocks away; after it left they fought within " + later + " ticks");
        }
    }

    /** Ticks both mobs one tick at a time (as the level does: {@code tickCount} up, then {@code tick}) until one targets the other. Returns the ticks used, or -1 when {@code max} passed in peace. */
    private static int fight(Mob a, Mob b, int max) {
        for (int i = 1; i <= max; i++) {
            tickOnce(a, i);
            tickOnce(b, i);
            if (a.getTarget() == b || b.getTarget() == a) return i;
        }
        return -1;
    }

    private static void tickOnce(Mob m, int round) {
        if (m.isRemoved()) throw new IllegalStateException(BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()) + " was removed during the test (tick " + round + ")");
        m.tickCount++;
        m.tick();
    }

    // ------------------------------------------------------------------------------------------------ the drops
    private static SelfTests.Result drops(MinecraftServer server) {
        SelfTests.Result stop = gate(server, false);
        if (stop != null) return stop;
        if (!EcoConfig.get(EcoConfig.FACTION_DROPS) || !DndConfig.get(DndConfig.DROPS)) return ok("skipped: eco.factionDropsEnabled or dnd.drops is false in the config");
        ServerLevel level = SelfTests.mega(server);
        Sector s = sector0(server);
        if (s.rooms.isEmpty()) return fail("sector 0 has no rooms");
        BlockPos at = s.rooms.get(0).center();
        int threat = MegaDungeon.threatAt(level, at);
        if (threat < 1) return fail("the threat at the centre of room #" + s.rooms.get(0).id + " is " + threat + ": MegaDungeon.threatAt does not know the position");
        FactionDef undead = FactionRegistry.graph().def("undead");
        if (undead == null) return fail("the undead faction is not loaded");
        Item cut = DungeonFood.rawFor(undead.cutKind());
        ServerPlayer killer = SelfTests.player(level, PLAYER, at.getX(), at.getY(), at.getZ());
        Zombie zombie = EntityType.ZOMBIE.create(level);
        Skeleton skeleton = EntityType.SKELETON.create(level);
        if (zombie == null || skeleton == null) return fail("a test mob could not be created");
        zombie.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        skeleton.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        int cap = EcoConfig.get(EcoConfig.MAX_EXTRA_MEAT);
        int kills = 1500, meatKills = 0, stoneKills = 0, mostMeat = 0;
        DamageSource byPlayer = level.damageSources().playerAttack(killer);
        for (int i = 0; i < kills; i++) {
            List<ItemEntity> out = new ArrayList<>();
            FactionDrops.onDrops(new LivingDropsEvent(zombie, byPlayer, out, true));
            int meat = 0, stones = 0;
            for (ItemEntity e : out) {
                ItemStack stack = e.getItem();
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (stack.getItem() == cut) meat += stack.getCount();
                else if (id.getNamespace().equals("tensura") && STONES.contains(id.getPath())) stones += stack.getCount();
                else return fail("an unexpected item dropped: " + id);
            }
            if (meat > cap) return fail("a kill dropped " + meat + " cuts, above maxExtraMeat " + cap);
            if (stones > 1) return fail("a kill dropped " + stones + " bonus stones, the bonus is one item");
            if (meat > 0) meatKills++;
            if (stones > 0) stoneKills++;
            mostMeat = Math.max(mostMeat, meat);
        }
        if (cap > 0 && meatKills == 0) return fail("no cut in " + kills + " kills of a zombie (undead chance " + undead.meatChance() + "): check the log for a faction drops warning");
        if (stoneKills == 0 && EcoConfig.get(EcoConfig.STONE_THREAT_BONUS) > 0.0) return fail("no bonus stone in " + kills + " kills at threat " + threat + " (chance " + FactionLoot.stoneBonusChance(threat, EcoConfig.get(EcoConfig.STONE_THREAT_BONUS)) + ")");
        DamageSource noPlayer = level.damageSources().generic();
        for (int i = 0; i < 300; i++) {
            List<ItemEntity> out = new ArrayList<>();
            FactionDrops.onDrops(new LivingDropsEvent(zombie, noPlayer, out, false));
            if (!out.isEmpty()) return fail("a death no player caused still dropped " + out.size() + " item(s): turf-war kills must pay nothing extra");
        }
        for (int i = 0; i < 600; i++) {
            List<ItemEntity> out = new ArrayList<>();
            FactionDrops.onDrops(new LivingDropsEvent(skeleton, byPlayer, out, true));
            for (ItemEntity e : out) if (e.getItem().getItem() == cut) return fail("a skeleton dropped a raw cut: creatures with no flesh must leave none");
        }
        return ok(kills + " zombie kills at threat " + threat + ": cut in " + meatKills + " (most " + mostMeat + ", cap " + cap + "), bonus stone in " + stoneKills + "; no kill by a player-less death, no cut from a skeleton");
    }

    // ------------------------------------------------------------------------------------------------ the arena
    /**
     * Why: a self-test must not depend on what the templates put where, so the mobs get a pair of positions in the AIR above the entry of sector 0
     * (y 110, above every room), on a chunk loaded for the test. They have no gravity and hang there, and nothing of the dungeon (a block, a room, an
     * encounter) is touched. Closing the arena discards everything that was spawned and puts the fake player back out of the level.
     */
    private static final class Arena implements AutoCloseable {
        final ServerLevel level;
        final int x, y, z, cx, cz;
        private final List<Entity> spawned = new ArrayList<>();
        private ServerPlayer player;
        private boolean listed;

        Arena(ServerLevel level, Sector sector) {
            this.level = level;
            BlockPos entry = sector.entry;
            cx = entry.getX() >> 4;
            cz = entry.getZ() >> 4;
            x = (cx << 4) + 2;
            z = (cz << 4) + 6;
            y = Math.min(110, level.getMaxBuildHeight() - 8);
            level.getChunk(cx, cz);
        }

        <T extends Mob> T spawn(EntityType<T> type, int dx) {
            T mob = type.create(level);
            if (mob == null) throw new IllegalStateException(type.getDescriptionId() + " cannot be created");
            mob.moveTo(x + dx + 0.5, y, z + 0.5, 0.0f, 0.0f);
            mob.setNoGravity(true);
            mob.setPersistenceRequired();
            if (!level.addFreshEntity(mob)) throw new IllegalStateException("the level refused " + type.getDescriptionId() + " (a handler cancelled its join?)");
            spawned.add(mob);
            return mob;
        }

        boolean visible(Mob m) {
            return level.getEntitiesOfClass(Mob.class, m.getBoundingBox().inflate(1.0)).contains(m);
        }

        /**
         * A chunk that was loaded a moment ago has not had its status applied to the entity storage yet (that is queued on the server thread), so the
         * entities in it are invisible to entity queries until the server runs its queue. Loading chunks that are NOT loaded yet makes the server work
         * its queue while it waits for them, so rings of neighbours (and, as a last resort, a few far chunks of the empty dimension) are loaded until
         * the probe mob can be found. A chunk that was loaded long ago is visible at once and nothing extra is loaded.
         */
        void ensureVisible(Mob probe) {
            for (int ring = 1; ring <= 3 && !visible(probe); ring++) {
                for (int d = -ring; d <= ring; d++) {
                    level.getChunk(cx + d, cz + ring);
                    level.getChunk(cx + d, cz - ring);
                    level.getChunk(cx + ring, cz + d);
                    level.getChunk(cx - ring, cz + d);
                }
            }
            for (int far = 0; far < 3 && !visible(probe); far++) level.getChunk(cx + 48 + 8 * far, cz + 48);
            if (!visible(probe)) throw new IllegalStateException("mobs in the arena chunk (" + cx + ", " + cz + ") stay invisible to entity queries: the chunk status was not applied");
        }

        /** A fake player is not in the level's player list (nothing adds it), so mobs would never see it; the test lists it for as long as it needs it. */
        void listPlayer(ServerPlayer p) {
            player = p;
            p.setInvulnerable(true);
            p.setInvisible(true);
            List<ServerPlayer> all = playerList();
            if (!all.contains(p)) {
                all.add(p);
                listed = true;
            }
        }

        void unlistPlayer() {
            if (listed && player != null) playerList().remove(player);
            listed = false;
        }

        /** The level's own list of players (the one the mobs' player searches walk). */
        @SuppressWarnings("unchecked")
        private List<ServerPlayer> playerList() {
            return (List<ServerPlayer>) (List<?>) level.players();
        }

        /** Discards every mob spawned so far (the player stays). */
        void clear() {
            for (Entity e : spawned) if (!e.isRemoved()) e.discard();
            spawned.clear();
        }

        @Override
        public void close() {
            clear();
            unlistPlayer();
            if (player != null) {
                player.setInvulnerable(false);
                player.setInvisible(false);
            }
        }
    }
}
