package com.newuniverse.nusmp.elite.hub;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.HubLayout;
import com.newuniverse.nusmp.labyrinth.mega.hub.HubData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.slf4j.Logger;

import java.util.List;

/**
 * Why: "spawn them at the plaza, persist, never despawn" needs one class that owns the PLACING. Every five seconds, in the Sanctuary dimension only, when the plaza has been stamped, a player is
 * near it and the plaza's chunks are fully loaded (entity ticking: an entity that was saved with the chunk is loaded by then, so its absence is real and not a race that would spawn a duplicate),
 * it looks for each NPC inside the plaza's box: none, and it places one on the walkable ground at its spot ({@link HubNpcLayout}); more than one, and it removes the extras (so a double
 * placement can never stack). Placed NPCs are saved with the chunk and never despawn ({@link HubNpcBase}), so after the first placement this is a cheap look-up that finds them. If an admin
 * kills one, it comes back; {@code npcs.spawn = false} switches the automatic placement off (the eggs and {@code /summon} still work). The ground is found by scanning the column, so the NPC stands right on
 * the template's paving, a stall's carpet or the fallback plaza alike. Server thread only.
 */
public final class HubNpcSpawner {
    private HubNpcSpawner() {}

    private static final Logger LOG = LogUtils.getLogger();
    /** A player must be within this many blocks of the plaza for the NPCs to be looked after. */
    private static final double NEAR = 96.0;

    public static void register(IEventBus gameBus) { gameBus.addListener(HubNpcSpawner::onLevelTick); }

    static void onLevelTick(LevelTickEvent.Post e) {
        try {
            if (!(e.getLevel() instanceof ServerLevel level) || !Labyrinth.inSanctuary(level) || level.getGameTime() % 100 != 11) return;
            if (HubConfig.enabled() && HubConfig.get(HubConfig.NPC_SPAWN)) ensure(level);
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] the Hub NPCs could not be looked after: {}", ex.toString());
        }
    }

    /** Makes sure both NPCs stand on the plaza; returns how many were placed (and removed extras do not count). */
    public static int ensure(ServerLevel level) {
        if (!HubData.get(level).built()) return 0;
        BlockPos centre = new BlockPos(HubLayout.PLAZA_X, HubLayout.SURFACE_Y, HubLayout.PLAZA_Z);
        boolean near = false;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5) <= NEAR * NEAR) {
                near = true;
                break;
            }
        }
        if (!near || !level.isPositionEntityTicking(centre)) return 0;
        int placed = 0;
        placed += place(level, QuestGiverNPC.class, HubNpcs.QUEST_GIVER.get(), HubNpcLayout.questGiver());
        placed += place(level, MerchantNPC.class, HubNpcs.MERCHANT.get(), HubNpcLayout.merchant());
        return placed;
    }

    private static <T extends HubNpcBase> int place(ServerLevel level, Class<T> cls, EntityType<T> type, HubNpcLayout.Spot spot) {
        double[] b = HubNpcLayout.searchBox();
        List<T> found = level.getEntitiesOfClass(cls, new AABB(b[0], b[1], b[2], b[3], b[4], b[5]));
        for (int i = 1; i < found.size(); i++) found.get(i).discard();
        if (!found.isEmpty()) return 0;
        BlockPos column = BlockPos.containing(spot.x(), HubLayout.SURFACE_Y, spot.z());
        level.getChunk(column.getX() >> 4, column.getZ() >> 4);
        int y = groundY(level, column);
        T npc = type.create(level);
        if (npc == null) return 0;
        npc.moveTo(spot.x(), y, spot.z(), spot.yaw(), 0.0f);
        npc.setYHeadRot(spot.yaw());
        npc.setYBodyRot(spot.yaw());
        npc.setHome(BlockPos.containing(spot.x(), y, spot.z()));
        level.addFreshEntity(npc);
        LOG.info("[nusmp] placed {} on the Hub plaza at {} {} {}", type.getDescriptionId(), spot.x(), y, spot.z());
        return 1;
    }

    /** The y a creature stands at in the column: the first cell, scanning up from just below the plaza surface, that is free with a solid cell under it. */
    static int groundY(ServerLevel level, BlockPos column) {
        for (int y = HubLayout.SURFACE_Y - 2; y <= HubLayout.SURFACE_Y + 6; y++) {
            BlockPos under = new BlockPos(column.getX(), y - 1, column.getZ());
            BlockPos here = under.above();
            BlockState below = level.getBlockState(under);
            if (!below.getCollisionShape(level, under).isEmpty() && free(level, here) && free(level, here.above())) return y;
        }
        return HubLayout.SURFACE_Y;
    }

    private static boolean free(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return s.getCollisionShape(level, pos).isEmpty() && s.getFluidState().isEmpty();
    }
}
