package com.newuniverse.nusmp.avatar.betrayal;

import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.AvatarManager;
import com.newuniverse.nusmp.avatar.outcome.Outcome;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Why: a betrayed Avatar must be able to find its creator wherever they go, and a loyal one must follow them through a portal, but an entity in an
 * unloaded chunk cannot act and an entity cannot walk through a portal on its own. This class does the two things that make cross-dimension tracking work on
 * a server: {@link #teleportNear} moves an Avatar next to a player in any dimension (a plain teleport in the same level, a 1.21.1 dimension transition
 * across levels, which re-creates the entity from its saved NBT, so the returned entity is the one to keep using), and {@link #wake} reads the world-wide
 * {@link AvatarManager} (last known dimension and position of every Avatar) and puts a short chunk ticket on the position of an Avatar whose chunk is not
 * loaded, so it wakes up, ticks and its goals can teleport it. The ticket is the vanilla portal ticket (it expires by itself after 300 ticks), so nothing
 * stays loaded for good and nothing has to be cleaned up.
 */
public final class BetrayalDimensionHandler {
    private BetrayalDimensionHandler() {}

    /** Ticks between two passes of {@link #wake}. */
    public static final int WAKE_INTERVAL = 100;
    /** An Avatar is woken at most this often (ticks). */
    private static final long WAKE_COOLDOWN = 300L;

    /**
     * Puts the Avatar beside the target player in the target's dimension. Returns the Avatar that now stands there (a new instance after a dimension
     * change) or null when it could not be moved.
     */
    public static AvatarEntity teleportNear(AvatarEntity a, ServerPlayer target) {
        if (!(target.level() instanceof ServerLevel dest) || !(a.level() instanceof ServerLevel here) || !a.isAlive()) return null;
        Vec3 spot = findSpot(dest, target.position(), a);
        if (here == dest) {
            a.getNavigation().stop();
            a.teleportTo(spot.x, spot.y, spot.z);
            return a;
        }
        Entity moved = a.changeDimension(new DimensionTransition(dest, spot, Vec3.ZERO, a.getYRot(), a.getXRot(), DimensionTransition.DO_NOTHING));
        if (moved instanceof AvatarEntity av) {
            AvatarManager.get(dest.getServer()).update(av);
            return av;
        }
        return null;
    }

    /** A standable spot two to four blocks from {@code around}: solid floor, room for the Avatar's body; the point itself when none is found. */
    static Vec3 findSpot(ServerLevel level, Vec3 around, AvatarEntity a) {
        var rnd = level.getRandom();
        var dims = a.getDimensions(Pose.STANDING);
        for (int i = 0; i < 20; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2, d = 2.0 + rnd.nextDouble() * 2.0;
            double x = around.x + Math.cos(ang) * d, z = around.z + Math.sin(ang) * d;
            for (int dy : new int[]{0, 1, -1, 2, -2, 3, -3}) {
                BlockPos pos = BlockPos.containing(x, around.y + dy, z);
                BlockPos below = pos.below();
                if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) && level.noCollision(dims.makeBoundingBox(x, pos.getY(), z))) return new Vec3(x, pos.getY(), z);
            }
        }
        return around;
    }

    /**
     * The wake pass (call every {@link #WAKE_INTERVAL} ticks from the server tick): for each recorded Avatar whose creator is online but which is not
     * loaded anywhere, and which has a reason to move (betrayed and hunting, or loyal and left behind in another dimension), put a portal ticket on its last
     * known chunk so it loads and its own goals take over.
     */
    public static void wake(MinecraftServer server) {
        AvatarManager mgr = AvatarManager.get(server);
        long now = server.overworld().getGameTime();
        for (AvatarManager.Row r : mgr.all()) {
            try {
                Outcome state = Outcome.byName(r.state);
                boolean betrayed = state == Outcome.BETRAYAL;
                boolean follows = state == Outcome.PENDING || state == Outcome.SKIPPED_NAMED || state == Outcome.SKIPPED_UNIQUE;
                if (!betrayed && !follows) continue;
                ServerPlayer owner = server.getPlayerList().getPlayer(r.creator);
                if (owner == null || owner.isSpectator()) continue;
                ResourceLocation dimId = ResourceLocation.tryParse(r.dim);
                if (dimId == null) continue;
                ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimId));
                if (level == null || level.getEntity(r.id) != null) continue;                          // unknown level, or it is loaded already
                boolean otherLevel = owner.level() != level;
                if (!betrayed && !otherLevel) continue;                                                // a loyal one only needs waking across dimensions
                if (now - r.lastWake < WAKE_COOLDOWN) continue;
                BlockPos at = BlockPos.containing(r.x, r.y, r.z);
                level.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(at), 3, at);
                mgr.touchWake(r.id, now);
            } catch (RuntimeException skip) {
                // one bad row must not stop the pass
            }
        }
    }

    public static boolean sameLevel(Level a, Level b) { return a == b; }
}
