package com.newuniverse.nusmp.arena;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.entity.zogratis.LuciusBoss;
import com.newuniverse.nusmp.entity.zogratis.ZogratisCloneEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.slf4j.Logger;

/**
 * Why: this is the gate that makes Lucius exist only in his arena (1.46.0, {@code luciusArenaOnly}), and the guard that keeps the arena's
 * courtyard as it was built. Every road a mob can take into a world ends in an event: a spawn egg, {@code /summon}, a structure, a spawner or another
 * mod's code ends in {@code EntityJoinLevelEvent} (cancelled here by the pure table of {@link SpawnGate}, with a message to the players nearby so a spawn egg that
 * "does nothing" explains itself); a portal, the Dimensional Nexus or a spatial gate ends in {@code EntityTravelToDimensionEvent}; a teleport in
 * {@code EntityTeleportEvent}. Natural spawning in the arena is refused ({@code FinalizeSpawnEvent}): the room has one guest. The same class refuses to
 * place anything in the arena (a block, a bucket) because the courtyard is the fight's furniture; breaking is already refused by the dungeon guard,
 * which holds this dimension too. An entity loaded from disk is never refused (a Lucius from an older version is not deleted by this update).
 * All of it is server side and costs one comparison for every entity that is not his.
 */
public final class ArenaSpawnEvents {
    private ArenaSpawnEvents() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final String HINT = "He will only answer in his arena: use the Hourglass of the Final Hour.";

    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.HIGHEST, ArenaSpawnEvents::onJoin);
        bus.addListener(EventPriority.HIGHEST, ArenaSpawnEvents::onFinalize);
        bus.addListener(EventPriority.HIGHEST, ArenaSpawnEvents::onTravel);
        bus.addListener(EventPriority.HIGHEST, ArenaSpawnEvents::onTeleport);
        bus.addListener(EventPriority.HIGHEST, ArenaSpawnEvents::onPlace);
        bus.addListener(EventPriority.HIGHEST, ArenaSpawnEvents::onUse);
    }

    /** Lucius himself or one of his clones. */
    static boolean family(Entity e) { return e instanceof LuciusBoss || e instanceof ZogratisCloneEntity; }

    private static boolean tagged(Entity e) { return e.getPersistentData().getBoolean(SpawnGate.TAG); }

    private static void onJoin(EntityJoinLevelEvent e) {
        Entity en = e.getEntity();
        if (e.getLevel().isClientSide() || !family(en)) return;
        SpawnGate.Verdict v = SpawnGate.decide(true, ArenaConfig.arenaOnly(), e.loadedFromDisk(), ZogratisArena.in(e.getLevel()), tagged(en));
        if (v != SpawnGate.Verdict.DENY) return;
        e.setCanceled(true);
        LOG.info("[nusmp] Refused to spawn {} at {} in {}: Lucius exists only in the arena (luciusArenaOnly)", net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(en.getType()), en.blockPosition(), e.getLevel().dimension().location());
        if (e.getLevel() instanceof ServerLevel sl) {
            Vec3 at = en.position();
            for (ServerPlayer p : sl.players()) if (p.distanceToSqr(at) <= 24.0 * 24.0) p.displayClientMessage(Component.literal(HINT).withStyle(ChatFormatting.GOLD), false);
        }
    }

    private static void onFinalize(FinalizeSpawnEvent e) {
        MobSpawnType t = e.getSpawnType();
        boolean natural = t == MobSpawnType.NATURAL || t == MobSpawnType.CHUNK_GENERATION || t == MobSpawnType.PATROL || t == MobSpawnType.REINFORCEMENT;
        if (SpawnGate.refusesNatural(ZogratisArena.in(e.getLevel().getLevel()), natural)) e.setSpawnCancelled(true);
    }

    private static void onTravel(EntityTravelToDimensionEvent e) {
        Entity en = e.getEntity();
        if (en.level().isClientSide || !family(en)) return;
        if (!SpawnGate.mayLeave(true, ArenaConfig.arenaOnly(), e.getDimension() == ZogratisArena.DIMENSION)) {
            e.setCanceled(true);
            LOG.info("[nusmp] Lucius was refused a trip to {}: he cannot be carried out of the arena", e.getDimension().location());
        }
    }

    private static void onTeleport(EntityTeleportEvent e) {
        Entity en = e.getEntity();
        if (en.level().isClientSide || !family(en) || !ArenaConfig.arenaOnly() || !ZogratisArena.in(en.level())) return;
        double dx = e.getTargetX() - 0.5, dz = e.getTargetZ() - 0.5;
        if (dx * dx + dz * dz > (double) ArenaLayout.RADIUS * ArenaLayout.RADIUS) e.setCanceled(true);
    }

    private static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (e.getLevel() instanceof net.minecraft.world.level.Level l && ZogratisArena.in(l) && !l.isClientSide()) e.setCanceled(true);
    }

    /** A block, a bucket of anything or a snow-like item in hand does not place: the item part of the click is denied, the block part (the sigil) still works. */
    private static void onUse(PlayerInteractEvent.RightClickBlock e) {
        if (!ZogratisArena.in(e.getLevel())) return;
        ItemStack s = e.getItemStack();
        if (s.getItem() instanceof BlockItem || s.getItem() instanceof BucketItem || s.getItem() instanceof SolidBucketItem) e.setUseItem(TriState.FALSE);
    }
}
