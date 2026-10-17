package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.Marker;
import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.quests.NusmpEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Why: this is the half of the Dungeon Master that makes players HIDE. A static spawner teaches nothing but "kill the thing in the room"; a big
 * monster that walks the halls of a sector, room by room, along the doorways, and hunts by what it sees and hears, teaches the tabletop's other
 * lesson: sometimes the answer to a monster is not to be there. Every few minutes, in a sector that has players, the chance rolls ({@link
 * WandererRules#spawnChance}); a winner is drawn from the roster ({@code wanderers.json}), placed at a room far from the party and out of its
 * sight, named, scaled by the sector's threat, and made persistent while a player is in the sector. From then on every half second this class
 * lets the boss think: it builds what the boss PERCEIVES (the players of its sector with their line of sight, crouching, light and stillness,
 * the loudest thing it heard, a blow it took), asks the pure {@link WandererMind} for an order, and carries it out with the polite steering that
 * works on goal-driven and brain-driven mobs alike ({@link MobSteer}). Four rules are enforced here because they are about the world, not the
 * mind: it never stands in a Sanctuary room (it is walked out, and teleported out after a second and a half if it is stuck inside), it never
 * leaves its sector, it shows no boss bar (a bar would be a radar; its {@code stopSeenByPlayer} is called on every player of the sector), and
 * it vanishes, after half a minute, when nobody is left in its sector. When a chase ends because the party got away (the boss lost the target
 * or the target reached a safe room) the quest key {@code mega.wanderer_survived} is fired for that player. The stand-in players of the
 * self-tests are passed in explicitly, so {@link #spawn} and {@link #think} take the player list as an argument.
 */
public final class WandererDirector {
    private WandererDirector() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** The entity tag of a wandering boss, and the persistent data that binds it to its sector. */
    public static final String TAG = "nusmp_wanderer", SECTOR = "nusmp_wanderer_sector";
    static final ResourceLocation HEALTH_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "mega_wanderer_health"), SPEED_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "mega_wanderer_speed");
    private static final int THINK_EVERY = 10, GRACE = 600, MIN_CHASE = 60;
    private static final double SPAWN_FAR = 40.0, SPAWN_NEAR = 28.0, METER_HEARD = 3.0;

    private static final Map<UUID, Wanderer> LIVE = new LinkedHashMap<>();
    /** The game time each sector last had a wandering boss appear, die or leave: the minimum gap runs from here. */
    private static final Map<Integer, Long> LAST_END = new HashMap<>();
    private static final Map<UUID, double[]> LAST_POS = new HashMap<>();
    private static final Set<UUID> STILL = new HashSet<>();
    private record CachedGraph(int key, PatrolGraph graph) {}
    private static final Map<Integer, CachedGraph> GRAPHS = new HashMap<>();
    private static boolean warned;

    /** The result of a spawn: the boss, or null with the reason. */
    public record Spawned(Mob mob, String text) {}

    // ---------------------------------------------------------------- the tick
    public static void onServerTick(ServerTickEvent.Post e) {
        try {
            tick(e.getServer());
        } catch (Throwable t) {
            warn("tick", t);
        }
    }

    private static void tick(MinecraftServer server) {
        ServerLevel level = MegaDungeon.level(server);
        if (level == null) return;
        int tick = server.getTickCount();
        if (!EncConfig.get(EncConfig.WANDERERS)) {
            if (!LIVE.isEmpty() && tick % 100 == 0) clear(level);
            return;
        }
        long now = level.getGameTime();
        List<ServerPlayer> players = level.players();
        if (!LIVE.isEmpty() && tick % THINK_EVERY == 3) thinkAll(level, players, now);
        int check = EncConfig.get(EncConfig.WANDERER_CHECK);
        if (!players.isEmpty() && tick % check == 71 % check) checkSpawns(level, players, now);
    }

    // ---------------------------------------------------------------- appearing
    private static void checkSpawns(ServerLevel level, List<? extends ServerPlayer> players, long now) {
        Map<Integer, List<ServerPlayer>> bySector = new LinkedHashMap<>();
        for (ServerPlayer p : players) {
            if (!p.isAlive() || p.isSpectator() || p.isCreative()) continue;
            Sector s = MegaDungeon.sectorAt(level, p.blockPosition());
            if (s != null) bySector.computeIfAbsent(s.id, k -> new ArrayList<>()).add(p);
        }
        if (bySector.isEmpty()) return;
        Random rnd = new Random(level.getRandom().nextLong());
        double base = EncConfig.get(EncConfig.WANDERER_CHANCE);
        long gap = EncConfig.get(EncConfig.WANDERER_MIN_GAP);
        for (Map.Entry<Integer, List<ServerPlayer>> en : bySector.entrySet()) {
            Sector s = MegaDungeon.data(level.getServer()).sector(en.getKey());
            if (s == null || s.state == Sector.State.GENERATING || s.state == Sector.State.FAILED || hasLive(s.id)) continue;
            long since = now - LAST_END.computeIfAbsent(s.id, k -> now);          // the first sight of players starts the clock
            double chance = WandererRules.spawnChance(base, s.threat, since, gap);
            if (chance <= 0.0 || rnd.nextDouble() >= chance) continue;
            spawn(level, s, en.getValue(), rnd);
            return;                                                              // at most one new boss per check
        }
    }

    /**
     * Brings a wandering boss into a sector: a kind from the roster, a room far from {@code avoid} (the party) and out of its sight. Never throws;
     * the result says why nothing came. Used by the check, by the admin command and by the self-test.
     */
    public static Spawned spawn(ServerLevel level, Sector sector, List<? extends ServerPlayer> avoid, Random rnd) {
        try {
            return spawnInner(level, sector, avoid, rnd);
        } catch (Throwable t) {
            warn("spawn", t);
            return new Spawned(null, "the wandering boss failed to spawn: " + t);
        }
    }

    private static Spawned spawnInner(ServerLevel level, Sector sector, List<? extends ServerPlayer> avoid, Random rnd) {
        if (hasLive(sector.id)) return new Spawned(null, "sector " + sector.id + " has a wandering boss already");
        PatrolGraph g = graphOf(sector);
        if (g.size() < 3) return new Spawned(null, "sector " + sector.id + " has fewer than three rooms a wandering boss may enter");
        WandererRoster.Entry kind = EncData.roster().pick(sector.threat, e -> Bestiary.usable(Bestiary.type(e.entity())), rnd);
        if (kind == null) return new Spawned(null, "no usable kind of wandering boss in the roster for threat " + sector.threat);
        EntityType<?> type = Bestiary.type(kind.entity());
        List<double[]> points = new ArrayList<>();
        List<SpawnPicker.Eye> eyes = new ArrayList<>();
        for (ServerPlayer p : avoid) {
            points.add(new double[] {p.getX(), p.getY(), p.getZ()});
            Vec3 eye = p.getEyePosition();
            eyes.add(new SpawnPicker.Eye(eye.x, eye.y, eye.z));
        }
        SpawnPicker.LineOfSight sight = avoid.isEmpty() ? null : new LevelSight(level, avoid.get(0));
        for (int attempt = 0; attempt < 4; attempt++) {
            int node = g.spawnNode(points, SPAWN_FAR, rnd);
            if (node < 0) break;
            PatrolGraph.Node n = g.node(node);
            Entity made = type.create(level);
            if (!(made instanceof Mob mob)) {
                if (made != null) made.discard();
                return new Spawned(null, kind.entity() + " is not a mob");
            }
            EncounterSpawner.Rules rules = new EncounterSpawner.Rules(eyes, SPAWN_NEAR, Double.MAX_VALUE, sight, new int[] {6});
            if (!EncounterSpawner.placeNear(level, mob, BlockPos.containing(n.x(), n.y(), n.z()), rules, rnd)) {
                mob.discard();
                continue;
            }
            stamp(level, mob, sector, kind);
            Wanderer w = new Wanderer(mob.getUUID(), sector.id, level.getGameTime(), kind.name());
            w.lastBlock = mob.blockPosition().asLong();
            LIVE.put(mob.getUUID(), w);
            if (!level.addFreshEntity(mob)) {
                LIVE.remove(mob.getUUID());
                mob.discard();
                continue;
            }
            LAST_END.put(sector.id, level.getGameTime());
            announce(level, avoid);
            Room where = sector.roomAt(mob.blockPosition());
            return new Spawned(mob, kind.name() + " (" + kind.entity() + ") appears in room " + (where == null ? "?" : Integer.toString(where.id)) + " of sector " + sector.id);
        }
        return new Spawned(null, "no room of sector " + sector.id + " could take " + kind.entity() + " (chunks not loaded, or every place too near or in sight of the party)");
    }

    /** Names it, binds it to its sector, scales it, and makes it persistent (it is removed by the Director, not by the game's despawner). */
    private static void stamp(ServerLevel level, Mob mob, Sector sector, WandererRoster.Entry kind) {
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.EVENT, null);
        mob.setPersistenceRequired();
        mob.addTag(TAG);
        CompoundTag data = mob.getPersistentData();
        data.putInt(SECTOR, sector.id);
        mob.setCustomName(Component.literal(kind.name()));
        mob.setCustomNameVisible(false);
        EncounterSpawner.scale(mob, ThreatScale.amount(sector.threat, EncConfig.get(EncConfig.HEALTH_PER_LEVEL)), ThreatScale.amount(sector.threat, EncConfig.get(EncConfig.DAMAGE_PER_LEVEL)));
        AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null && kind.healthScale() != 1.0) hp.addOrReplacePermanentModifier(new AttributeModifier(HEALTH_ID, kind.healthScale() - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        AttributeInstance sp = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (sp != null && kind.speed() != 1.0) sp.addOrReplacePermanentModifier(new AttributeModifier(SPEED_ID, kind.speed() - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        mob.setHealth(mob.getMaxHealth());
    }

    /** A low rumble and one line: it is not shown on a bar or a map, the party is only told that something heavy walks. */
    private static void announce(ServerLevel level, List<? extends ServerPlayer> who) {
        if (!EncConfig.get(EncConfig.ANNOUNCE)) return;
        SoundEvent rumble = sound("minecraft:entity.warden.nearby_closest");
        for (ServerPlayer p : who) {
            if (rumble != null) level.playSound(null, p.getX(), p.getY(), p.getZ(), rumble, SoundSource.AMBIENT, 0.7f, 0.5f);
            EncounterCue.tell(p, "message.nusmp.mega.enc.wanderer_spawn");
        }
    }

    // ---------------------------------------------------------------- thinking
    private static void thinkAll(ServerLevel level, List<? extends ServerPlayer> players, long now) {
        sample(players);
        for (UUID id : new ArrayList<>(LIVE.keySet())) think(level, id, players, now);
    }

    /** Remembers whether each player moved since the last pass: a player who stands still is seen from a fifth less far. */
    static void sample(List<? extends ServerPlayer> players) {
        for (ServerPlayer p : players) {
            double[] last = LAST_POS.get(p.getUUID());
            if (last == null) {
                LAST_POS.put(p.getUUID(), new double[] {p.getX(), p.getY(), p.getZ()});
                STILL.remove(p.getUUID());
                continue;
            }
            double dx = p.getX() - last[0], dy = p.getY() - last[1], dz = p.getZ() - last[2];
            if (dx * dx + dy * dy + dz * dz < 0.15 * 0.15) STILL.add(p.getUUID());
            else STILL.remove(p.getUUID());
            last[0] = p.getX();
            last[1] = p.getY();
            last[2] = p.getZ();
        }
    }

    /**
     * One pass of one wandering boss: perceive, think, act. {@code players} are the players it may perceive (those of its sector; others are
     * ignored). Returns the order it was given, or null when the boss is gone or has nobody to think about. Never throws.
     */
    public static WandererMind.Order think(ServerLevel level, UUID id, List<? extends ServerPlayer> players, long now) {
        Wanderer w = LIVE.get(id);
        if (w == null) return null;
        try {
            return thinkInner(level, w, players, now);
        } catch (Throwable t) {
            warn("think", t);
            return null;
        }
    }

    private static WandererMind.Order thinkInner(ServerLevel level, Wanderer w, List<? extends ServerPlayer> players, long now) {
        Entity found = level.getEntity(w.id);
        if (found == null) {
            if (w.lastBlock != 0L && level.hasChunkAt(BlockPos.of(w.lastBlock))) end(w, now);          // its chunk is loaded and it is not there
            return null;
        }
        if (!(found instanceof Mob mob) || !mob.isAlive() || mob.isRemoved()) {
            end(w, now);
            return null;
        }
        w.lastBlock = mob.blockPosition().asLong();
        Sector sector = MegaDungeon.data(level.getServer()).sector(w.sector);
        if (sector == null || sector.state == Sector.State.FAILED) {
            mob.discard();
            end(w, now);
            return null;
        }
        List<ServerPlayer> present = new ArrayList<>(), targets = new ArrayList<>();
        for (ServerPlayer p : players) {
            if (!p.isAlive() || p.isSpectator() || !sector.containsRegion(p.blockPosition())) continue;
            present.add(p);
            if (!p.isCreative()) targets.add(p);
            mob.stopSeenByPlayer(p);                                             // hunted by sound, not announced: no boss bar for anyone
        }
        if (present.isEmpty()) {
            if (now - w.lastPlayerAt > GRACE) {
                mob.discard();
                end(w, now);
            }
            return null;
        }
        w.lastPlayerAt = now;
        PatrolGraph g = graphOf(sector);
        WandererRules.Tuning t = tuning();
        List<WandererRules.Sighting> seen = new ArrayList<>(targets.size());
        for (ServerPlayer p : targets) seen.add(sighting(level, sector, mob, w, p, t));
        WandererRules.Heard heard = hear(sector, mob, targets, w, t, now);
        UUID hurtBy = null;
        LivingEntity struck = mob.getLastHurtByMob();
        int stamp = mob.getLastHurtByMobTimestamp();
        if (struck instanceof ServerPlayer hp && stamp != w.lastHurtStamp) hurtBy = hp.getUUID();
        w.lastHurtStamp = stamp;
        WandererMind.Input in = new WandererMind.Input(now, mob.getX(), mob.getY(), mob.getZ(), mob.getNavigation().isDone(), seen, heard, hurtBy);
        WandererMind.Order order = w.mind.step(in, t, g, new Random(level.getRandom().nextLong()));
        w.order = order;
        w.lastThink = now;
        keepOutOfSanctuary(sector, g, mob, w);
        apply(mob, w, order, targets);
        react(level, mob, order, players);
        return order;
    }

    private static WandererRules.Tuning tuning() {
        return WandererRules.of(EncConfig.get(EncConfig.SIGHT_RANGE), EncConfig.get(EncConfig.HEAR_RANGE), EncConfig.get(EncConfig.LOSE_TICKS));
    }

    private static WandererRules.Sighting sighting(ServerLevel level, Sector sector, Mob mob, Wanderer w, ServerPlayer p, WandererRules.Tuning t) {
        double d = Math.sqrt(mob.distanceToSqr(p));
        boolean current = p.getUUID().equals(w.mind.target());
        boolean near = d <= t.sightRange() * 1.05 || (current && d <= t.sightRange() * 3.0);
        boolean los = near && mob.hasLineOfSight(p);
        boolean dark = level.getBrightness(LightLayer.BLOCK, BlockPos.containing(p.getEyePosition())) <= LightRules.DARK_LIMIT && !EncounterDirector.heldLight(p);
        Room r = sector.roomAt(p.blockPosition());
        boolean safe = r != null && Sanctuary.is(r.kind);
        return new WandererRules.Sighting(p.getUUID(), p.getX(), p.getY(), p.getZ(), d, los, p.isCrouching(), dark, STILL.contains(p.getUUID()), safe);
    }

    /** The most attractive thing it could hear since its last pass: a loud event of the sector, or a player whose own noise level is high and close. Never one from a Sanctuary room. */
    private static WandererRules.Heard hear(Sector sector, Mob mob, List<ServerPlayer> targets, Wanderer w, WandererRules.Tuning t, long now) {
        long after = w.lastThink == Long.MIN_VALUE ? now - 20 : w.lastThink;
        WandererRules.Heard best = NoiseEvents.BURSTS.loudest(mob.getX(), mob.getY(), mob.getZ(), after, t);
        double bestScore = 0.0;
        if (best != null) {
            BlockPos at = BlockPos.containing(best.x(), best.y(), best.z());
            Room r = sector.roomAt(at);
            if (!sector.containsRegion(at) || (r != null && Sanctuary.is(r.kind))) best = null;
            else bestScore = WandererRules.score(best.loudness(), best.distance());
        }
        for (ServerPlayer p : targets) {
            double reading = NoiseEvents.reading(p.getUUID(), now);
            if (reading < METER_HEARD) continue;
            double d = Math.sqrt(mob.distanceToSqr(p));
            if (d > t.hearRange() * 0.5) continue;
            Room r = sector.roomAt(p.blockPosition());
            if (r != null && Sanctuary.is(r.kind)) continue;
            double s = WandererRules.score(reading, d);
            if (best == null || s > bestScore) {
                best = new WandererRules.Heard(p.getX(), p.getY(), p.getZ(), d, reading);
                bestScore = s;
            }
        }
        return best;
    }

    // ---------------------------------------------------------------- acting
    /** Carries out an order: hunt the perceived player, or walk, and above all never hunt what it does not perceive. */
    private static void apply(Mob mob, Wanderer w, WandererMind.Order o, List<ServerPlayer> targets) {
        if (o.chase() != null) {
            for (ServerPlayer p : targets) {
                if (!p.getUUID().equals(o.chase())) continue;
                if (mob.getTarget() != p) MobSteer.aim(mob, p);
                if (mob.distanceToSqr(p) > 4.0) MobSteer.follow(mob, p, o.speed());
                return;
            }
        }
        if (mob.getTarget() != null) MobSteer.release(mob);
        if (o.move()) walk(mob, w, false);
        else mob.getNavigation().stop();
    }

    /**
     * Sends the boss along its walk. A new path is made when the goal moved, the old walk ended, or the path went stale (two seconds); never
     * when it is already at the goal. Called by the Director's pass and by the {@link PatrolGoal}.
     */
    static void walk(Mob mob, Wanderer w, boolean force) {
        WandererMind.Order o = w.order;
        if (o == null || !o.move()) return;
        double dx = mob.getX() - o.gx(), dz = mob.getZ() - o.gz();
        if (dx * dx + dz * dz <= WandererMind.REACH * WandererMind.REACH) return;
        long now = mob.level().getGameTime();
        boolean moved = Double.isNaN(w.walkX) || Math.abs(w.walkX - o.gx()) > 1.5 || Math.abs(w.walkZ - o.gz()) > 1.5;
        if (!force && !moved && !mob.getNavigation().isDone() && now - w.walkAt < 40) return;
        MobSteer.walk(mob, o.gx(), o.gy(), o.gz(), o.speed());
        w.walkX = o.gx();
        w.walkZ = o.gz();
        w.walkAt = now;
    }

    /** It never stands in a Sanctuary room and never leaves its sector: walk out, and after three passes in a row inside, step out. */
    private static void keepOutOfSanctuary(Sector sector, PatrolGraph g, Mob mob, Wanderer w) {
        Room r = sector.roomAt(mob.blockPosition());
        boolean inside = r != null && Sanctuary.is(r.kind);
        boolean outside = !sector.containsRegion(mob.blockPosition());
        if (!inside && !outside) {
            w.inside = 0;
            return;
        }
        int n = g.nearest(mob.getX(), mob.getZ());
        if (n < 0) return;
        PatrolGraph.Node node = g.node(n);
        if (++w.inside >= 3 || outside) {
            mob.getNavigation().stop();
            mob.teleportTo(node.x(), node.y(), node.z());
            w.inside = 0;
        } else {
            MobSteer.release(mob);
            MobSteer.walk(mob, node.x(), node.y(), node.z(), 1.0);
        }
    }

    private static void react(ServerLevel level, Mob mob, WandererMind.Order o, List<? extends ServerPlayer> players) {
        switch (o.event()) {
            case SPOTTED -> roar(level, mob);
            case LOST, REFUGE, GAVE_UP -> survived(o, players);
            default -> { }
        }
    }

    /** A roar when it sets off after someone: heard by everyone near, other wandering bosses included. */
    private static void roar(ServerLevel level, Mob mob) {
        SoundEvent roar = sound("minecraft:entity.ravager.roar");
        if (roar != null) level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), roar, SoundSource.HOSTILE, 1.4f, 0.55f);
        NoiseEvents.emit(level, mob.getX(), mob.getY(), mob.getZ(), NoiseKind.ROAR);
    }

    /** The quest hook: the player was really chased (at least three seconds) and got away. Fired once per chase, because a chase ends once. */
    private static void survived(WandererMind.Order o, List<? extends ServerPlayer> players) {
        if (o.eventPlayer() == null || o.chaseTicks() < MIN_CHASE) return;
        for (ServerPlayer p : players) {
            if (p.getUUID().equals(o.eventPlayer()) && p.isAlive() && Labyrinth.inMega(p.level())) NusmpEvents.fire(p, "mega.wanderer_survived");
        }
    }

    private static SoundEvent sound(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        Optional<SoundEvent> s = rl == null ? Optional.empty() : BuiltInRegistries.SOUND_EVENT.getOptional(rl);
        return s.orElse(null);
    }

    // ---------------------------------------------------------------- the room graph of a sector
    /** The patrol graph of a sector: every room is a node (allowed unless it is a Sanctuary), each standing at its PATROL marker, else its ROOM_CENTER marker, else its middle. Cached while the sector does not change. */
    static PatrolGraph graphOf(Sector s) {
        int key = s.rooms.size() * 31 + s.edges.size() * 17 + s.markers.size();
        CachedGraph c = GRAPHS.get(s.id);
        if (c != null && c.key() == key) return c.graph();
        Map<Integer, BlockPos> patrol = new HashMap<>(), centre = new HashMap<>();
        for (Marker m : s.markers) {
            if (m.kind() != MarkerKind.PATROL && m.kind() != MarkerKind.ROOM_CENTER) continue;
            Room r = s.roomAt(m.pos());
            if (r == null) continue;
            Map<Integer, BlockPos> into = m.kind() == MarkerKind.PATROL ? patrol : centre;
            BlockPos old = into.get(r.id), mid = r.center();
            if (old == null || m.pos().distSqr(mid) < old.distSqr(mid)) into.put(r.id, m.pos());          // the marker nearest the middle
        }
        List<PatrolGraph.Node> nodes = new ArrayList<>(s.rooms.size());
        for (Room r : s.rooms) {
            BlockPos p = patrol.getOrDefault(r.id, centre.getOrDefault(r.id, r.center()));
            nodes.add(new PatrolGraph.Node(r.id, p.getX() + 0.5, p.getY(), p.getZ() + 0.5, !Sanctuary.is(r.kind), r.kind.isCirculation()));
        }
        PatrolGraph g = PatrolGraph.build(nodes, s.edges);
        GRAPHS.put(s.id, new CachedGraph(key, g));
        return g;
    }

    // ---------------------------------------------------------------- events and housekeeping
    /** A wandering boss joins the world: it gets its patrol goal; one saved with its chunk is adopted again, or removed when its sector is gone or has another. */
    public static void onJoin(EntityJoinLevelEvent e) {
        try {
            Entity en = e.getEntity();
            if (e.getLevel().isClientSide() || !(en instanceof Mob mob) || !Labyrinth.inMega(e.getLevel()) || !en.getTags().contains(TAG)) return;
            Wanderer w = LIVE.get(en.getUUID());
            if (w == null) {
                int sid = en.getPersistentData().getInt(SECTOR);
                ServerLevel sl = e.getLevel() instanceof ServerLevel s ? s : null;
                Sector sector = sl == null ? null : MegaDungeon.data(sl.getServer()).sector(sid);
                if (sector == null || hasLive(sid) || !EncConfig.get(EncConfig.WANDERERS)) {
                    e.setCanceled(true);
                    return;
                }
                w = new Wanderer(en.getUUID(), sid, sl.getGameTime(), en.hasCustomName() ? en.getCustomName().getString() : "wandering boss");
                w.lastBlock = en.blockPosition().asLong();
                LIVE.put(en.getUUID(), w);
            }
            w.goalInstalled = GoalAccess.add(mob, 1, new PatrolGoal(mob, w));
        } catch (Throwable t) {
            warn("join", t);
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { forgetPlayer(e.getEntity().getUUID()); }

    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent e) { forgetPlayer(e.getEntity().getUUID()); }

    public static void onServerStopped(ServerStoppedEvent e) { reset(); }

    static void forgetPlayer(UUID id) {
        LAST_POS.remove(id);
        STILL.remove(id);
    }

    static void reset() {
        LIVE.clear();
        LAST_END.clear();
        LAST_POS.clear();
        STILL.clear();
        GRAPHS.clear();
    }

    private static void end(Wanderer w, long now) {
        LAST_END.put(w.sector, now);
        LIVE.remove(w.id);
    }

    private static boolean hasLive(int sector) {
        for (Wanderer w : LIVE.values()) if (w.sector == sector) return true;
        return false;
    }

    /** Is this entity one of the live wandering bosses? (The guard events ask.) */
    static Wanderer of(Entity e) { return e == null ? null : LIVE.get(e.getUUID()); }

    /** The mind of a live boss (for the self-test and the status command), or null. */
    static WandererMind mindOf(UUID id) {
        Wanderer w = LIVE.get(id);
        return w == null ? null : w.mind;
    }

    /** Removes every wandering boss of the level (command, self-tests, switch off). Returns how many were removed. */
    public static int clear(ServerLevel level) {
        int n = 0;
        List<Entity> found = new ArrayList<>();
        for (Entity e : level.getAllEntities()) if (e.getTags().contains(TAG)) found.add(e);
        for (Entity e : found) {
            e.discard();
            n++;
        }
        for (Wanderer w : new ArrayList<>(LIVE.values())) end(w, level.getGameTime());
        return n;
    }

    /** Lines for the status command: each live boss with its kind, place, state and target. */
    public static List<String> status(ServerLevel level) {
        List<String> out = new ArrayList<>();
        for (Wanderer w : LIVE.values()) {
            Entity e = level.getEntity(w.id);
            String where = e == null ? "(not loaded)" : e.blockPosition().getX() + " " + e.blockPosition().getY() + " " + e.blockPosition().getZ();
            out.add(w.name + " in sector " + w.sector + " at " + where + ": " + w.mind.state().name().toLowerCase(java.util.Locale.ROOT)
                    + (w.mind.target() == null ? "" : ", after " + w.mind.target()) + ", goal node " + w.mind.goalNode() + ", goal installed " + w.goalInstalled);
        }
        if (out.isEmpty()) out.add("No wandering boss is alive.");
        return out;
    }

    private static void warn(String what, Throwable t) {
        if (warned) return;
        warned = true;
        LOG.warn("[nusmp] The wandering boss director failed in {} (further failures are not logged)", what, t);
    }
}
