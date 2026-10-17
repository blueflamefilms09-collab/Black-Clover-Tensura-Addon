package com.newuniverse.nusmp.avatar.delve;

import com.newuniverse.nusmp.avatar.AvatarAiConfig;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.Stance;
import com.newuniverse.nusmp.avatar.TensuraBridge;
import com.newuniverse.nusmp.avatar.ai.AvatarGates;
import com.newuniverse.nusmp.boss.rules.BossIdentity;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: the owner wants Avatars that can "use the dungeon independently to navigate floors, fight bosses, and collect loot". This is the goal that does it, as an
 * errand with every limit written down in {@link DelveRules}: an Avatar whose stance is Delve, healthy, rested, with a creator who is online and a dungeon that
 * exists, spends two seconds opening a portal, arrives on the floor the creator last reached ({@link AvatarDelve#target}), and then walks the floor room by room (the
 * nearest room it has not seen, found on the floor's own room graph), attacks the monsters of the dungeon that are within its strength (a boss only when it is no
 * stronger than the Avatar) and takes what they drop (the looting goal runs above this one). It turns back when it is hurt, when its pockets fill, when its time
 * (ten minutes by default) is up, when something stronger than it is near, when its creator calls it back or comes into the dungeon, or when the creator goes
 * offline, and on the way home it is put beside its creator with its valuables handed over. Those limits are checked by the watcher goal ({@link AvatarDelve#watch}),
 * not here: this goal is stopped by every fight, and a limit must hold in the middle of one. The trip lives in the Avatar's persistent data, so the new instance
 * that the dimension change creates, or the Avatar after a restart, simply goes on with it. It runs below the fight and the looting goals and above following.
 */
public final class AvatarDelveGoal extends Goal {
    private enum Phase { IDLE, CHANNEL, TRIP }

    private static final int CHANNEL_TICKS = 40;

    private final AvatarEntity avatar;
    private Phase phase = Phase.IDLE;
    private int channel;
    private long nextExploreAt, nextScanAt, nextSaveAt, nextStuckAt;
    private int sectorLoaded = -1, roomTarget = -1;
    private Map<Integer, List<Integer>> graph = new HashMap<>();
    private final Set<Integer> visited = new HashSet<>();
    private Vec3 lastSpot = Vec3.ZERO;

    public AvatarDelveGoal(AvatarEntity avatar) {
        this.avatar = avatar;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    // ---------------------------------------------------------------- starting
    @Override
    public boolean canUse() {
        if (avatar.level().isClientSide || !(avatar.level() instanceof ServerLevel sl)) return false;
        if (AvatarDelve.onTrip(avatar)) {                                              // a trip is on: this is the new instance after the dimension change, or the world was reloaded
            phase = Phase.TRIP;
            return true;
        }
        if (!AvatarGates.slot(avatar.tickCount, 40) || !AvatarAiConfig.delveEnabled() || avatar.stance() != Stance.DELVE) return false;
        if (avatar.getTarget() != null || avatar.frenzied() || MegaDungeon.in(avatar.level())) return false;
        ServerPlayer creator = avatar.creatorPlayer();
        MinecraftServer server = sl.getServer();
        if (creator == null || AvatarDelve.dungeon(server) == null || creator.level() != avatar.level()) return false;     // the creator is here to see it off
        boolean available = AvatarDelve.target(server, creator) != null;
        double hp = avatar.getHealth() / Math.max(1.0f, avatar.getMaxHealth());
        if (!DelveRules.mayStart(AvatarAiConfig.delveEnabled(), available, true, avatar.isLoyal(), hp, AvatarAiConfig.delveMinHealth(), sl.getGameTime(), AvatarDelve.restUntil(avatar))) return false;
        phase = Phase.CHANNEL;
        channel = 0;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return switch (phase) {
            case CHANNEL -> avatar.getTarget() == null && avatar.stance() == Stance.DELVE;
            case TRIP -> AvatarDelve.onTrip(avatar);
            case IDLE -> false;
        };
    }

    @Override
    public void start() {
        avatar.getNavigation().stop();
        nextExploreAt = nextScanAt = nextSaveAt = nextStuckAt = 0;
        sectorLoaded = -1;
        roomTarget = -1;
        visited.clear();
        if (phase == Phase.CHANNEL && avatar.level() instanceof ServerLevel sl) {
            Vec3 at = avatar.position();
            VfxSpawn.send(sl, VfxShape.SPACE_PORTAL, at.add(0, 0.05, 0), at.add(0, 1.05, 0), 0xFF7A5CFF, CHANNEL_TICKS + 10, 1.4f);
        }
    }

    @Override
    public void stop() {
        phase = Phase.IDLE;
        avatar.getNavigation().stop();
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    // ---------------------------------------------------------------- ticking
    @Override
    public void tick() {
        if (phase == Phase.CHANNEL) {
            avatar.getNavigation().stop();
            if (++channel >= CHANNEL_TICKS) depart();
            return;
        }
        if (phase == Phase.TRIP) trip();
    }

    private void depart() {
        phase = Phase.IDLE;
        if (!(avatar.level() instanceof ServerLevel sl)) return;
        ServerPlayer creator = avatar.creatorPlayer();
        ServerLevel mega = AvatarDelve.dungeon(sl.getServer());
        Sector target = creator == null ? null : AvatarDelve.target(sl.getServer(), creator);
        if (mega == null || target == null) return;
        Vec3 at = avatar.position();
        VfxSpawn.send(sl, VfxShape.SPACE_PORTAL, at.add(0, 0.05, 0), at.add(0, 1.05, 0), 0xFF7A5CFF, 20, 1.4f);
        AvatarDelve.depart(avatar, mega, target, creator);             // the old instance is gone after this; the new one resumes the trip from its saved record
    }

    private void trip() {
        if (!(avatar.level() instanceof ServerLevel sl)) return;
        long now = sl.getGameTime();
        if (avatar.getTarget() != null) return;                              // the fight has the legs (it outranks this goal; this is only a guard)
        if (now >= nextScanAt) {
            nextScanAt = now + 10;
            pickFoe(sl);
        }
        if (avatar.getTarget() == null && now >= nextExploreAt) {
            nextExploreAt = now + 10;
            explore(sl, now);
        }
        if (now >= nextSaveAt) {
            nextSaveAt = now + 100;
            saveVisited();
        }
    }

    /** Picks the nearest monster within its strength and makes it the target; the combat goals do the fighting. (The limits are the watcher's, see {@link AvatarDelve#watch}.) */
    private void pickFoe(ServerLevel sl) {
        double mine = TensuraBridge.maxEp(avatar);
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, avatar.getBoundingBox().inflate(14), x -> AvatarDelve.hostile(avatar, x))) {
            if (!DelveRules.worthEngaging(mine, TensuraBridge.maxEp(e), BossIdentity.isBoss(e)) || !avatar.getSensing().hasLineOfSight(e)) continue;
            double d = e.distanceToSqr(avatar);
            if (d < bestD) { bestD = d; best = e; }
        }
        if (best != null) avatar.setTarget(best);
    }

    // ---------------------------------------------------------------- the walk
    private void explore(ServerLevel sl, long now) {
        Sector s = AvatarDelve.sector(avatar);
        if (s == null) return;
        if (sectorLoaded != s.id) loadSector(s);
        Room here = nearestRoom(s);
        if (here == null) return;
        // a room counts as seen when the Avatar stands in it
        for (Room r : s.rooms) {
            BlockPos c = r.center();
            if (Math.abs(c.getX() + 0.5 - avatar.getX()) < 5.0 && Math.abs(c.getZ() + 0.5 - avatar.getZ()) < 5.0 && Math.abs(c.getY() - avatar.getY()) < 4.0) visited.add(r.id);
        }
        if (now >= nextStuckAt) {                                               // hardly moved in five seconds: give up on that room
            double moved = avatar.position().distanceTo(lastSpot);
            if (roomTarget >= 0 && DelveRules.stuck(moved, 100)) { visited.add(roomTarget); roomTarget = -1; }
            lastSpot = avatar.position();
            nextStuckAt = now + 100;
        }
        if (roomTarget < 0 || visited.contains(roomTarget)) {
            roomTarget = DelveRules.nextRoom(graph, here.id, visited);
            if (roomTarget < 0) {                                              // every reachable room has been seen: the round is done
                saveVisited();
                AvatarDelve.finish(avatar, DelveRules.Reason.NONE);
                return;
            }
        }
        Room target = null;
        for (Room r : s.rooms) if (r.id == roomTarget) { target = r; break; }
        if (target == null) { visited.add(roomTarget); roomTarget = -1; return; }
        BlockPos c = target.center();
        if (!avatar.getNavigation().moveTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 1.0)) { visited.add(roomTarget); roomTarget = -1; }
    }

    private void loadSector(Sector s) {
        sectorLoaded = s.id;
        graph = new HashMap<>();
        for (Room r : s.rooms) graph.put(r.id, new ArrayList<>());
        for (int[] e : s.edges) {
            if (e.length < 2) continue;
            graph.computeIfAbsent(e[0], k -> new ArrayList<>()).add(e[1]);
            graph.computeIfAbsent(e[1], k -> new ArrayList<>()).add(e[0]);
        }
        visited.clear();
        for (int id : AvatarDelve.trip(avatar).getIntArray("Visited")) visited.add(id);
    }

    private Room nearestRoom(Sector s) {
        Room best = null;
        double bestD = Double.MAX_VALUE;
        for (Room r : s.rooms) {
            BlockPos c = r.center();
            double dx = c.getX() + 0.5 - avatar.getX(), dz = c.getZ() + 0.5 - avatar.getZ();
            double d = dx * dx + dz * dz;
            if (d < bestD) { bestD = d; best = r; }
        }
        return best;
    }

    private void saveVisited() {
        if (!AvatarDelve.onTrip(avatar) || visited.size() > 400) return;
        int[] ids = new int[visited.size()];
        int i = 0;
        for (int id : visited) ids[i++] = id;
        AvatarDelve.trip(avatar).putIntArray("Visited", ids);
    }
}
