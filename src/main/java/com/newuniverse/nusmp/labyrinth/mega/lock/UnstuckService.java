package com.newuniverse.nusmp.labyrinth.mega.lock;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Marker;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.gen.StairReach;
import com.newuniverse.nusmp.labyrinth.mega.hub.HubTravel;
import com.newuniverse.nusmp.labyrinth.mega.rift.PocketRealm;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftRules;
import com.newuniverse.nusmp.labyrinth.mega.system.SystemParty;
import com.newuniverse.nusmp.labyrinth.mega.system.SystemSync;
import com.newuniverse.nusmp.labyrinth.mega.system.ToastKind;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: {@code /dungeon unstuck} is the escape that works from ANYWHERE, for every way a player can be stuck that no timer or rune covers: a room the generator closed, a hole
 * the pit trap left, a spawn in a room with no exit, the dungeon guard's unbreakable walls that make digging out impossible, a rune the player cannot find. It takes the player
 * (or, with {@code party}, everyone who plays with them) to the last safe room they stood in, else the nearest safe room by doorways, else the arrival stairs of the floor, else
 * the forward stairs, else out to the Hub. The rules ({@link UnstuckRules}) give it a cooldown and a cost in experience levels and refuse it while hostile creatures are close,
 * so it is no free teleport out of a fight, EXCEPT for a player who is really sealed in by a closed lock ({@link LockBook#isSealed}): nobody pays for something the dungeon did to
 * them. A player in a pocket realm is taken out through the realm's own exit. The last safe spot of every player is remembered once a second while they stand in a safe room
 * (a landing, a safe zone, a stairwell). Server thread only.
 */
public final class UnstuckService {
    private UnstuckService() {}

    private static final Logger LOG = LogUtils.getLogger();

    private record Spot(int sector, BlockPos pos) {}

    private static final Map<UUID, Spot> LAST_SAFE = new HashMap<>();
    private static final Map<UUID, Long> LAST_USE = new HashMap<>(), LAST_PARTY = new HashMap<>();

    /** A destination: what kind of place and where. */
    public record Target(UnstuckRules.Destination kind, BlockPos pos) {}

    // ================================================================ remembering a safe place
    public static void onServerTick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 11) return;
        ServerLevel mega = MegaDungeon.level(server);
        if (mega == null) return;
        for (ServerPlayer p : mega.players()) remember(mega, p);
    }

    static void remember(ServerLevel level, ServerPlayer p) {
        if (!p.isAlive() || p.isSpectator() || !p.onGround()) return;
        BlockPos pos = p.blockPosition();
        if (!MegaDungeon.inSafeZone(level, pos)) return;
        Sector s = MegaDungeon.sectorAt(level, pos);
        LAST_SAFE.put(p.getUUID(), new Spot(s == null ? -1 : s.id, pos.immutable()));
    }

    /** The last safe spot of a player in this sector, or null (a self-test sets one with {@link #setLastSafe}). */
    public static BlockPos lastSafe(UUID id, int sectorId) {
        Spot s = LAST_SAFE.get(id);
        return s != null && (sectorId < 0 || s.sector() == sectorId) ? s.pos() : null;
    }

    public static void setLastSafe(UUID id, int sectorId, BlockPos pos) { LAST_SAFE.put(id, new Spot(sectorId, pos.immutable())); }

    // ================================================================ choosing where to go
    /** The destination for this player, from the rules' order. Null when the dungeon has nowhere to take them (then the Hub is the last resort and is chosen by the caller). */
    public static Target destination(ServerLevel level, ServerPlayer p) {
        Sector s = MegaDungeon.sectorAt(level, p.blockPosition());
        BlockPos last = lastSafe(p.getUUID(), s == null ? -1 : s.id);
        boolean lastUsable = last != null && level.hasChunkAt(last);
        BlockPos nearest = s == null ? null : nearestSafe(s, p.blockPosition());
        BlockPos arrival = s == null ? null : s.arrival;
        BlockPos forward = s == null ? null : s.stairForwardPad;
        UnstuckRules.Destination d = UnstuckRules.choose(lastUsable, nearest != null, arrival != null, forward != null);
        return switch (d) {
            case LAST_SAFE -> new Target(d, last);
            case NEAREST_SAFE -> new Target(d, nearest);
            case ARRIVAL_STAIRS -> new Target(d, arrival);
            case FORWARD_STAIRS -> new Target(d, forward);
            case HUB -> new Target(d, null);
        };
    }

    /** The standing place in the nearest safe room, by doorways from where the player is (or from the nearest room when they stand in none). Null when there is none. */
    static BlockPos nearestSafe(Sector s, BlockPos from) {
        List<Room> rooms = s.rooms;
        if (rooms.isEmpty()) return null;
        Map<Integer, Integer> index = new HashMap<>();
        for (int i = 0; i < rooms.size(); i++) index.put(rooms.get(i).id, i);
        List<int[]> edges = new ArrayList<>();
        for (int[] e : s.edges) {
            Integer a = index.get(e[0]), b = index.get(e[1]);
            if (a != null && b != null) edges.add(new int[] {a, b});
        }
        Room here = s.roomAt(from);
        int start = -1;
        if (here != null) start = index.get(here.id);
        else {
            double best = Double.MAX_VALUE;
            for (int i = 0; i < rooms.size(); i++) {
                BlockPos c = rooms.get(i).center();
                double d = c.distSqr(from);
                if (d < best) {
                    best = d;
                    start = i;
                }
            }
        }
        int goal = StairReach.nearest(rooms.size(), edges, start, i -> rooms.get(i).kind.isSafe());
        if (goal < 0) return null;
        return standingPlace(s, rooms.get(goal));
    }

    /** Where to stand in a safe room: the arrival marker of a stairwell when it has one, else the middle of the room. */
    static BlockPos standingPlace(Sector s, Room room) {
        for (Marker m : s.markers(MarkerKind.ARRIVAL)) if (room.contains(m.pos())) return m.pos();
        return room.center();
    }

    // ================================================================ the command
    /**
     * {@code /dungeon unstuck [party]}. Returns 1 when the player was moved, 0 when it was refused (with the reason said to the player). Messages are sent here, so the
     * command only returns the code.
     */
    public static int unstuck(ServerPlayer p, boolean party) {
        ServerLevel level = p.serverLevel();
        if (!LockConfig.enabled()) return refuse(p, LockText.REFUSE_OFF);
        boolean pocket = MegaDungeon.in(level) && RiftRules.slotAt(p.getX(), p.getZ()) >= 0;
        boolean inMega = MegaDungeon.in(level);
        UnstuckRules.Config cfg = LockConfig.unstuck();
        boolean sealed = LockBook.isSealed(p);
        long now = level.getGameTime();
        Map<UUID, Long> clock = party ? LAST_PARTY : LAST_USE;
        long last = clock.getOrDefault(p.getUUID(), Long.MIN_VALUE / 2);
        int levels = p.isCreative() ? 10000 : p.experienceLevel;
        UnstuckRules.Refusal r = UnstuckRules.check(cfg, now, last, inMega, sealed || pocket, levels, hostileNear(level, p, cfg.hostileRadius()), party);
        switch (r) {
            case NOT_IN_DUNGEON -> { return refuse(p, LockText.REFUSE_NOT_HERE); }
            case COOLDOWN -> { return refuse(p, LockText.REFUSE_COOLDOWN, UnstuckRules.cooldownLeftSeconds(cfg, now, last, party)); }
            case NO_XP -> { return refuse(p, LockText.REFUSE_NO_XP, cfg.xpLevels()); }
            case HOSTILE_NEAR -> { return refuse(p, LockText.REFUSE_HOSTILE); }
            default -> { }
        }
        if (pocket) {
            PocketRealm.leaveForRescue(level, p);
            LockMsg.tell(p, LockMsg.text(LockText.UNSTUCK_OK, LockMsg.text(LockText.DEST_POCKET).getString()).withStyle(ChatFormatting.GREEN));
            return 1;
        }
        Target t = destination(level, p);
        if (t.kind() != UnstuckRules.Destination.HUB && t.pos() == null) return refuse(p, LockText.REFUSE_NOWHERE);
        int cost = UnstuckRules.cost(cfg, sealed);
        if (cost > 0 && !p.isCreative()) p.giveExperienceLevels(-cost);
        if (!sealed) clock.put(p.getUUID(), now);
        moveTo(p, t);
        LockMsg.tell(p, LockMsg.text(LockText.UNSTUCK_OK, destName(t.kind())).withStyle(ChatFormatting.GREEN));
        if (cost > 0 && !p.isCreative()) LockMsg.tell(p, LockMsg.text(LockText.UNSTUCK_COST, cost).withStyle(ChatFormatting.GRAY));
        else if (sealed) LockMsg.tell(p, LockMsg.text(LockText.UNSTUCK_FREE).withStyle(ChatFormatting.GRAY));
        SystemSync.toast(p, ToastKind.INFO, LockText.TOAST_RESCUED);
        LOG.info("[nusmp] {} used /dungeon unstuck{} -> {}", p.getGameProfile().getName(), party ? " party" : "", t.kind());
        if (party) {
            int moved = 0;
            for (ServerPlayer m : SystemParty.members(p)) {
                if (m == p || m.level() != level || !m.isAlive() || m.isSpectator()) continue;
                Target mt = destination(level, m);
                if (mt.kind() == UnstuckRules.Destination.HUB) mt = t;
                moveTo(m, mt);
                LockMsg.tell(m, LockMsg.text(LockText.UNSTUCK_OK, destName(mt.kind())).withStyle(ChatFormatting.GREEN));
                SystemSync.toast(m, ToastKind.INFO, LockText.TOAST_RESCUED);
                moved++;
            }
            LockMsg.tell(p, LockMsg.text(LockText.UNSTUCK_PARTY, moved).withStyle(ChatFormatting.GRAY));
        }
        return 1;
    }

    private static int refuse(ServerPlayer p, String key, Object... args) {
        LockMsg.tell(p, LockMsg.text(key, args).withStyle(ChatFormatting.RED));
        return 0;
    }

    private static String destName(UnstuckRules.Destination d) {
        return LockMsg.text(switch (d) {
            case LAST_SAFE -> LockText.DEST_LAST_SAFE;
            case NEAREST_SAFE -> LockText.DEST_NEAREST_SAFE;
            case ARRIVAL_STAIRS -> LockText.DEST_ARRIVAL;
            case FORWARD_STAIRS -> LockText.DEST_FORWARD;
            case HUB -> LockText.DEST_HUB;
        }).getString();
    }

    private static boolean hostileNear(ServerLevel level, ServerPlayer p, int radius) {
        if (radius <= 0) return false;
        return !level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(radius), m -> m.isAlive() && MegaDungeon.isDungeonMonster(m)).isEmpty();
    }

    /** Moves the player to a target (the Hub through the Hub's own travel). */
    public static void moveTo(ServerPlayer p, Target t) {
        if (t.kind() == UnstuckRules.Destination.HUB || t.pos() == null) {
            if (HubTravel.enabled() && HubTravel.available(p.getServer())) HubTravel.returnToPlaza(p);
            return;
        }
        teleportNear(p, t.pos());
    }

    // ================================================================ the move
    /** Teleports a player to the nearest free standing place round {@code at}, in their own level, stopping their fall and their momentum. */
    public static void teleportNear(ServerPlayer p, BlockPos at) {
        ServerLevel level = p.serverLevel();
        BlockPos spot = standable(level, at);
        level.getChunk(spot.getX() >> 4, spot.getZ() >> 4);
        p.teleportTo(level, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, p.getYRot(), p.getXRot());
        p.resetFallDistance();
        p.setDeltaMovement(Vec3.ZERO);
        level.playSound(null, spot, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.7f, 1.2f);
    }

    /** The nearest cell round {@code around} (within 6 blocks, 4 up and 3 down) with solid ground, two free cells above it and no liquid; {@code around} itself when there is none. */
    public static BlockPos standable(ServerLevel level, BlockPos around) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int r = 0; r <= 6; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    for (int dy = -3; dy <= 4; dy++) {
                        m.set(around.getX() + dx, around.getY() + dy, around.getZ() + dz);
                        if (!level.hasChunkAt(m)) continue;
                        if (isStandable(level, m)) return m.immutable();
                    }
                }
            }
        }
        return around;
    }

    private static boolean isStandable(ServerLevel level, BlockPos feet) {
        var below = level.getBlockState(feet.below());
        if (below.getCollisionShape(level, feet.below()).isEmpty() || !below.getFluidState().isEmpty()) return false;
        if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty() || !level.getFluidState(feet).isEmpty()) return false;
        return level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
    }

    // ================================================================ lifecycle
    public static void forget(UUID id) {
        LAST_SAFE.remove(id);
        LAST_USE.remove(id);
        LAST_PARTY.remove(id);
    }

    public static void clear() {
        LAST_SAFE.clear();
        LAST_USE.clear();
        LAST_PARTY.clear();
    }

    /** For the self-test and the admin command: forget the cooldowns of a player. */
    public static void resetCooldown(UUID id) {
        LAST_USE.remove(id);
        LAST_PARTY.remove(id);
    }
}
