package com.newuniverse.nusmp.labyrinth.mega.rift;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.gate.LabyrinthTravel;
import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.gen.ExpansionQueue;
import com.newuniverse.nusmp.labyrinth.mega.gen.SectorGenerator;
import com.newuniverse.nusmp.labyrinth.mega.party.PartyService;
import com.newuniverse.nusmp.labyrinth.mega.travel.StairTravel;
import com.newuniverse.nusmp.particle.rukh.RukhParticles;
import com.newuniverse.nusmp.quests.NusmpEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the server half of the rifts: it opens them (by themselves near players on the floors, by a relic, by an admin), keeps the list of the ones that are open, decides what stepping into
 * each kind does, and closes them. SPAWNING: every {@code checkSeconds} each player on a playable floor rolls once for a rift near them (a shortcut, or rarely a pocket); the spot is a
 * floor cell 8 to 22 blocks away, inside the same floor and clear of walls, never close to a stair pad or in a safe room, and no further rift opens while {@code maxNear} are already open within
 * 48 blocks, so the floors are not carpeted. SHORTCUT: the jump is rolled from the rift's seed (3 to 5 floors), held at the next boss floor ({@link RiftRules#shortcutTarget}) so a rift can
 * never skip a Decennial Boss's seal; if the destination floor is not built yet the rift asks the generator for it (the floors behind it, one at a time) and shows itself dim ("unstable") until it is,
 * exactly as the stairs say "still forming". The Directional Lock is asked first. The player arrives on the destination floor's arrival pad, together with party mates standing within six blocks.
 * POCKET: allocates a cell of the pocket field ({@link PocketRealm}), builds the realm and takes the player in. RETURN: the player's own waypoint ({@link WaypointService}). EXIT: back to where
 * the pocket was entered. Server thread only.
 */
public final class RiftService {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Set<Long> OPEN = new HashSet<>();                      // the positions (BlockPos long) of the rifts that are loaded and open, per dimension of the Mega-Dungeon and the Sanctuary
    private static final String NEXT_ROLL = "nusmp_rift_next";

    private RiftService() {}

    public static void register(IEventBus gameBus) {
        gameBus.addListener(RiftService::onServerTick);
        gameBus.addListener(RiftService::onStopped);
    }

    private static void onStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent e) { OPEN.clear(); }

    private static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        ServerLevel mega = MegaDungeon.level(server);
        if (mega == null || mega.players().isEmpty()) return;
        try {
            for (ServerPlayer p : new ArrayList<>(mega.players())) {
                WaypointService.tick(p);
                if ((server.getTickCount() + p.getId()) % 20 == 0) roll(mega, p);
            }
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] The rift spawner failed (further failures are logged at debug): {}", ex.toString());
            LOG.debug("rift spawner", ex);
        }
    }

    // ---------------------------------------------------------------- spawning
    private static void roll(ServerLevel mega, ServerPlayer p) {
        if (!RiftConfig.get(RiftConfig.ENABLED) || p.isSpectator() || !p.onGround()) return;
        long now = mega.getGameTime();
        var data = p.getPersistentData();
        if (now < data.getLong(NEXT_ROLL)) return;
        data.putLong(NEXT_ROLL, now + RiftConfig.get(RiftConfig.CHECK_SECONDS) * 20L);
        if (RiftRules.slotAt(p.getX(), p.getZ()) >= 0) return;
        MegaData md = MegaData.get(mega.getServer());
        Sector s = md.sectorAt(p.blockPosition());
        if (s == null || !s.isFloor() || !SectorGenerator.playable(s) || MegaDungeon.inSafeZone(mega, p.blockPosition())) return;
        if (nearCount(mega, p.blockPosition(), 48) >= RiftConfig.get(RiftConfig.MAX_NEAR)) return;
        var rnd = mega.getRandom();
        boolean pocket = RiftConfig.get(RiftConfig.POCKETS) && RiftRules.rolls(RiftConfig.get(RiftConfig.POCKET_CHANCE), rnd.nextDouble());
        boolean shortcut = !pocket && !s.isBossFloor() && RiftRules.rolls(RiftConfig.get(RiftConfig.SHORTCUT_CHANCE), rnd.nextDouble());
        if (!pocket && !shortcut) return;
        BlockPos at = findSpot(mega, s, p);
        if (at == null) return;
        DimensionalRiftBlockEntity be = open(mega, at, pocket ? RiftType.POCKET : RiftType.SHORTCUT, rnd.nextLong());
        if (be == null) return;
        if (!pocket) aimShortcut(md, s, be);
        p.displayClientMessage(Component.translatableWithFallback(pocket ? "message.nusmp.mega.rift.opened.pocket" : "message.nusmp.mega.rift.opened.shortcut",
                pocket ? "The air tears open nearby: a hidden realm hums behind it." : "A tear in the air opens nearby. It leads further on.").withStyle(pocket ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA), true);
        NusmpEvents.fire(p, "mega.rift", pocket ? "pocket" : "shortcut");
        com.newuniverse.nusmp.compat.maps.MapShare.rift(p, at, pocket);                                   // compat-maps: a clickable waypoint line for the rift (opt-in, rate limited)
    }

    private static BlockPos findSpot(ServerLevel level, Sector s, ServerPlayer p) {
        var rnd = level.getRandom();
        for (int i = 0; i < 24; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, d = 8 + rnd.nextDouble() * 14;
            int x = (int) Math.floor(p.getX() + Math.cos(a) * d), z = (int) Math.floor(p.getZ() + Math.sin(a) * d);
            for (int dy = 3; dy >= -3; dy--) {
                BlockPos feet = new BlockPos(x, (int) Math.floor(p.getY()) + dy, z);
                if (!s.containsRegion(feet) || !standable(level, feet)) continue;
                if (MegaDungeon.inSafeZone(level, feet)) continue;
                if (nearPad(s, feet)) continue;
                return feet;
            }
        }
        return null;
    }

    private static boolean nearPad(Sector s, BlockPos p) {
        for (BlockPos pad : new BlockPos[]{s.stairForward, s.stairBack, s.arrival}) if (pad != null && pad.distSqr(p) < 36) return true;
        return false;
    }

    /** A cell a rift may stand in: solid below, three cells of air above, and air on the four sides at the feet (an open spot, not a corridor corner). */
    static boolean standable(ServerLevel level, BlockPos feet) {
        if (!level.getBlockState(feet.below()).isSolidRender(level, feet.below())) return false;
        for (int i = 0; i < 3; i++) if (!level.getBlockState(feet.above(i)).isAir()) return false;
        for (net.minecraft.core.Direction d : new net.minecraft.core.Direction[]{net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.WEST})
            if (!level.getBlockState(feet.relative(d)).isAir()) return false;
        return true;
    }

    static int nearCount(ServerLevel level, BlockPos at, int radius) {
        int n = 0;
        long r2 = (long) radius * radius;
        for (long l : OPEN) if (BlockPos.of(l).distSqr(at) <= r2) n++;
        return n;
    }

    /** Opens a rift of {@code type} with its feet cell at {@code at} (server), or null when the cell is not free. */
    public static DimensionalRiftBlockEntity open(ServerLevel level, BlockPos at, RiftType type, long seed) {
        BlockState now = level.getBlockState(at);
        if (!now.isAir() && !now.canBeReplaced()) return null;
        level.setBlock(at, RiftRegistry.RIFT.get().defaultBlockState(), 3);
        BlockEntity be = level.getBlockEntity(at);
        if (!(be instanceof DimensionalRiftBlockEntity r)) return null;
        int base = RiftConfig.get(RiftConfig.RIFT_SECONDS) * 20;
        int life = switch (type) {
            case POCKET -> base * 2;
            case RETURN -> RiftConfig.get(RiftConfig.PORTAL_SECONDS) * 20;
            case EXIT -> 20 * 60 * RiftConfig.get(RiftConfig.POCKET_MINUTES) + 400;
            default -> base;
        };
        r.init(type, seed, life);
        OPEN.add(at.asLong());
        level.playSound(null, at, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.4f, 0.5f);
        burst(level, at, type);
        return r;
    }

    /** Called when a rift's block entity is loaded again after a restart: it counts as open once more. */
    public static void noteLoaded(BlockPos at) { OPEN.add(at.asLong()); }

    /** Called when a rift's block entity goes (it collapsed, or its chunk unloaded): it is no longer counted. */
    public static void noteGone(BlockPos at) { OPEN.remove(at.asLong()); }

    /** Decides where a shortcut leads, from the floor it stands on. */
    public static void aimShortcut(MegaData md, Sector from, DimensionalRiftBlockEntity be) {
        int jump = RiftRules.jumpFor(be.seed(), RiftConfig.get(RiftConfig.JUMP_MIN), RiftConfig.get(RiftConfig.JUMP_MAX));
        int target = RiftRules.shortcutTarget(from.chainIndex, jump, c -> isBoss(md, from.party, from.polarity, c));
        be.aimShortcut(from.party, from.polarity, from.chainIndex, target);
    }

    static boolean isBoss(MegaData md, int party, Polarity road, int chain) {
        Sector s = md.chainSector(party, road, chain);
        return s != null ? s.isBossFloor() : FloorMap.isBossFloor(FloorMap.floorOfSector(road, chain));
    }

    // ---------------------------------------------------------------- stability: is the destination there yet?
    public static void refreshStability(ServerLevel level, DimensionalRiftBlockEntity be) {
        MegaData md = MegaData.get(level.getServer());
        if (be.targetChain() <= be.fromChain()) {                           // nowhere to go (it was opened on a boss floor): stays unstable and goes out by itself
            be.setStable(false);
            return;
        }
        Sector t = md.chainSector(be.party(), be.polarity(), be.targetChain());
        if (t != null && SectorGenerator.playable(t)) {
            be.setStable(true);
            return;
        }
        be.setStable(false);
        if (be.age() < be.nextExpand()) return;
        be.nextExpand(be.age() + 100);
        int h = be.targetChain() - 1;                                       // the highest floor that exists below the target: ask for the one after it
        Sector from = null;
        for (; h >= be.fromChain(); h--) {
            from = md.chainSector(be.party(), be.polarity(), h);
            if (from != null) break;
        }
        if (from != null && SectorGenerator.playable(from) && !from.isBossFloor()) {
            String why = ExpansionQueue.get().tryNext(level, from);
            if (why != null) LOG.debug("[nusmp] A shortcut rift could not queue the floor after chain {}: {}", from.chainIndex, why);
        }
    }

    // ---------------------------------------------------------------- stepping in
    private static final String TOUCH = "nusmp_rift_touch";

    /** A player is inside the tear (every five ticks): carries out what this kind of rift does. */
    public static void touch(ServerLevel level, ServerPlayer p, DimensionalRiftBlockEntity be) {
        long now = level.getGameTime();
        if (now < p.getPersistentData().getLong(TOUCH)) return;
        p.getPersistentData().putLong(TOUCH, now + 60L);
        switch (be.type()) {
            case SHORTCUT -> shortcut(level, p, be);
            case POCKET -> PocketRealm.enter(level, p, be);
            case RETURN -> WaypointService.returnToWaypoint(p);
            case EXIT -> PocketRealm.leave(level, p);
        }
    }

    private static void shortcut(ServerLevel level, ServerPlayer p, DimensionalRiftBlockEntity be) {
        MegaData md = MegaData.get(level.getServer());
        if (!MegaDungeon.mayEnter(p, be.polarity())) return;                   // the lock says why
        if (!be.stable()) {
            if (be.targetChain() <= be.fromChain()) WaypointService.say(p, "shortcut.none", "This tear leads nowhere: a boss floor's seal stands in the way.", ChatFormatting.GRAY);
            else WaypointService.say(p, "shortcut.forming", "The tear ripples: the floor behind it is still forming. Wait here.", ChatFormatting.GRAY);
            return;
        }
        Sector t = md.chainSector(be.party(), be.polarity(), be.targetChain());
        BlockPos pad = t == null ? null : SectorGenerator.padOf(t);
        if (pad == null) {
            WaypointService.say(p, "shortcut.lost", "The tear collapses: the floor behind it is gone.", ChatFormatting.RED);
            return;
        }
        List<ServerPlayer> group = new ArrayList<>();
        group.add(p);
        for (ServerPlayer o : level.players())
            if (o != p && o.distanceToSqr(p) <= 36 && !o.isSpectator() && PartyService.partyOf(o) == PartyService.partyOf(p) && MegaDungeon.mayEnter(o, be.polarity())) group.add(o);
        for (ServerPlayer m : group) {
            LabyrinthTravel.arrive(m, level, pad, null);
            m.getPersistentData().putLong(StairTravel.UNTIL, level.getGameTime() + 100L);
            title(m, Component.translatableWithFallback("message.nusmp.mega.rift.floor", "Floor %s", FloorMap.label(t.firstFloor())),
                    Component.translatableWithFallback("message.nusmp.mega.rift.jump", "You fall through the tear: %s floors on.", be.targetChain() - be.fromChain()));
            NusmpEvents.fire(m, "mega.rift_jump", FloorMap.label(t.firstFloor()));
        }
        be.close();                                                         // a tear is used up by being used
    }

    static void title(ServerPlayer p, Component title, Component subtitle) {
        if (p.connection == null) return;
        p.connection.send(new ClientboundSetTitlesAnimationPacket(6, 30, 10));
        p.connection.send(new ClientboundSetTitleTextPacket(title.copy().withStyle(ChatFormatting.WHITE)));
        p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
    }

    // ---------------------------------------------------------------- closing
    /** The rift ran out (or was used up): it gutters out with a puff and the block goes. */
    public static void collapse(ServerLevel level, BlockPos pos, DimensionalRiftBlockEntity be) {
        OPEN.remove(pos.asLong());
        burst(level, pos, be.type());
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.2f, 0.6f);
        if (level.getBlockState(pos).is(RiftRegistry.RIFT.get())) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    static void burst(ServerLevel level, BlockPos at, RiftType type) {
        level.sendParticles(type == RiftType.POCKET ? RukhParticles.dark() : RukhParticles.light(), at.getX() + 0.5, at.getY() + 1.2, at.getZ() + 0.5, 24, 0.5, 0.9, 0.5, 0.02);
    }

    /** Every rift that is open now (position longs), for the admin list. */
    public static List<BlockPos> openRifts() {
        List<BlockPos> out = new ArrayList<>();
        for (long l : OPEN) out.add(BlockPos.of(l));
        return out;
    }

    // ---------------------------------------------------------------- the Return Portal on Floor 0
    /** Opens a Return Portal three blocks in front of the player (anywhere outside the dungeon's floors). Returns false when there is no room or no waypoint. */
    public static boolean openReturnPortal(ServerPlayer p) {
        if (WaypointService.get(p) == null) {
            WaypointService.say(p, "waypoint.none", "You have no waypoint: you have not stood anywhere in the dungeon yet.", ChatFormatting.GRAY);
            return false;
        }
        ServerLevel level = p.serverLevel();
        var look = p.getDirection();
        for (int dist = 3; dist >= 2; dist--) {
            BlockPos feet = p.blockPosition().relative(look, dist);
            for (int dy = 0; dy >= -1; dy--) {
                BlockPos at = feet.above(dy);
                if (level.getBlockState(at).isAir() && level.getBlockState(at.above()).isAir() && level.getBlockState(at.above(2)).isAir()) {
                    DimensionalRiftBlockEntity be = open(level, at, RiftType.RETURN, level.getRandom().nextLong());
                    if (be != null) return true;
                }
            }
        }
        WaypointService.say(p, "portal.room", "There is no room to open a portal here.", ChatFormatting.GRAY);
        return false;
    }
}
