package com.newuniverse.nusmp.labyrinth.mega.hub;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.eco.TurfMarks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the three Sanctuary Rules of the brief, kept by EVENT HANDLERS because a handler sees every path while data sees only the paths it names. (1) Only
 * passive mobs: the biome lists no monsters and the biome modifier ({@link SanctuaryPeaceModifier}) strips the ones other mods add, but a spawn that
 * reaches the world another way (a mod's own spawner, a summon, a block that releases a mob) is only stopped by looking at the MOB, so a cancelled
 * {@code FinalizeSpawnEvent} stops the spawns that fire it and a sparse sweep removes what slipped in without one. (2) Players can build: nothing is done
 * (the Dungeon Guard does not hold this dimension) except to refuse changes inside the protected volumes of {@link HubProtect}, for players who are not in
 * creative mode, on every path a change takes: breaking, placing (single and multi-block), explosions, and the buckets and fire a player carries.
 * (3) No foreign structures: the real guarantee is the generator, which places none; this class is the alarm, a one-time warning when a chunk of the
 * Sanctuary loads with a structure start in it. Everything is a no-op outside the Sanctuary and on the client, never throws out of an event, and holds
 * no state beyond one warning flag cleared when the server stops.
 */
public final class HubRules {
    private HubRules() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** The sweep for hostile mobs that slipped in runs once in this many server ticks (five seconds). */
    public static final int SWEEP_EVERY = 100;
    /** Blocks round each player the sweep looks at: a little over the usual simulation distance's inner part, so every mob a player could meet is covered. */
    public static final double SWEEP_RADIUS = 128.0;

    private static volatile boolean warnedStructure;
    private static volatile boolean warnedFailure;

    public static void register(IEventBus gameBus) {
        gameBus.addListener(EventPriority.HIGH, HubRules::onFinalizeSpawn);
        gameBus.addListener(HubRules::onBreak);
        gameBus.addListener(HubRules::onPlace);
        gameBus.addListener(HubRules::onExplosion);
        gameBus.addListener(HubRules::onLeftClick);
        gameBus.addListener(HubRules::onRightClick);
        gameBus.addListener(HubRules::onChunkLoad);
        gameBus.addListener(HubRules::onServerTick);
        gameBus.addListener(HubRules::onServerStopped);
    }

    // ---------------------------------------------------------------- passive mobs only
    /** Is this mob hostile: an {@link Enemy}, or registered in the monster category (a mod's monster that forgot the interface)? */
    public static boolean isHostile(Mob m) { return m instanceof Enemy || m.getType().getCategory() == MobCategory.MONSTER; }

    /** Is this spawn one an admin asked for (a command or a spawn egg), which the Sanctuary lets through? */
    public static boolean isAdminSpawn(MobSpawnType type) { return type == MobSpawnType.COMMAND || type == MobSpawnType.SPAWN_EGG; }

    /** Does this mob belong to somebody: tamed, owned, or marked as a servant by one of the mod's servant systems (Paladins, shadow minions, relic summons)? */
    public static boolean isOwned(Mob m) {
        if (m instanceof TamableAnimal t && t.isTame()) return true;
        if (m instanceof OwnableEntity o && o.getOwnerUUID() != null) return true;
        if (TurfMarks.anyOf(m.getTags(), TurfMarks.SERVANT_TAGS)) return true;
        CompoundTag data = m.getPersistentData();
        if (data.isEmpty()) return false;
        for (String key : TurfMarks.SERVANT_KEYS) if (data.contains(key)) return true;
        return false;
    }

    /**
     * The spawn-cancel decision for a mob of the given spawn type, with the switch read from the config: true = the spawn must be stopped. Public and
     * free of events so the self-test can ask it about a zombie and a cow.
     */
    public static boolean cancelsSpawn(MobSpawnType type, boolean hostile, boolean owned) {
        return HubSpawnPolicy.cancels(HubConfig.get(HubConfig.PASSIVE_ONLY), hostile, isAdminSpawn(type), owned);
    }

    /** The handler (public so the self-test can drive it with a constructed event; the bus calls it too). */
    public static void onFinalizeSpawn(FinalizeSpawnEvent e) {
        try {
            if (!Labyrinth.inSanctuary(e.getLevel().getLevel())) return;
            Mob mob = e.getEntity();
            if (!isHostile(mob)) return;
            if (cancelsSpawn(e.getSpawnType(), true, isOwned(mob))) {
                e.setSpawnCancelled(true);
                e.setCanceled(true);
            } else if (isAdminSpawn(e.getSpawnType()) && HubConfig.get(HubConfig.PASSIVE_ONLY)) {
                mob.getPersistentData().putBoolean(HubKeys.ALLOWED, true);              // what an admin asked for stays: the sweep reads this mark
            }
        } catch (RuntimeException ex) {
            failed("spawn filter", ex);
        }
    }

    /** One pass of the sweep round a point: removes the hostile mobs there that nobody owns, named or kept. Returns how many it removed. Public for the self-test. */
    public static int sweepAround(ServerLevel level, double x, double z) {
        if (!HubConfig.get(HubConfig.PASSIVE_ONLY)) return 0;
        AABB box = new AABB(x - SWEEP_RADIUS, level.getMinBuildHeight(), z - SWEEP_RADIUS, x + SWEEP_RADIUS, level.getMaxBuildHeight(), z + SWEEP_RADIUS);
        int removed = 0;
        for (Mob m : level.getEntitiesOfClass(Mob.class, box, HubRules::sweepable)) {
            m.discard();
            removed++;
        }
        return removed;
    }

    private static boolean sweepable(Mob m) {
        if (!m.isAlive() || !isHostile(m)) return false;                         // the cheap tests first: nearly every mob stops here
        return HubSpawnPolicy.sweeps(true, true, isOwned(m), m.hasCustomName(), m.isPersistenceRequired(), m.getPersistentData().getBoolean(HubKeys.ALLOWED));
    }

    private static void onServerTick(ServerTickEvent.Post e) {
        try {
            MinecraftServer server = e.getServer();
            if (server.getTickCount() % SWEEP_EVERY != 0) return;
            ServerLevel hub = server.getLevel(Labyrinth.SANCTUARY_DIMENSION);
            if (hub == null || hub.players().isEmpty()) return;
            for (ServerPlayer p : hub.players()) sweepAround(hub, p.getX(), p.getZ());
        } catch (RuntimeException ex) {
            failed("hostile sweep", ex);
        }
    }

    // ---------------------------------------------------------------- building, and what may not be built on
    /** Must a change at this block be refused to a player who is not in creative mode, under the current config? Public for the self-test. */
    public static boolean refuses(BlockPos pos) {
        return HubProtect.refuses(HubConfig.get(HubConfig.BUILD_ENABLED), HubConfig.get(HubConfig.PROTECT_PLAZA), pos.getX(), pos.getY(), pos.getZ());
    }

    private static boolean serverSanctuary(Level level) { return level != null && !level.isClientSide() && Labyrinth.inSanctuary(level); }

    private static void onBreak(BlockEvent.BreakEvent e) {
        try {
            if (!(e.getLevel() instanceof Level level) || !serverSanctuary(level)) return;
            Player p = e.getPlayer();
            if ((p != null && p.isCreative()) || !refuses(e.getPos())) return;
            e.setCanceled(true);
            tell(p, e.getPos());
        } catch (RuntimeException ex) {
            failed("break guard", ex);
        }
    }

    /** Also receives the multi-block placements (beds, doors, tall plants), which extend this event: every block they replace is checked, not just the clicked one. */
    private static void onPlace(BlockEvent.EntityPlaceEvent e) {
        try {
            LevelAccessor acc = e.getLevel();
            if (!(acc instanceof Level level) || !serverSanctuary(level)) return;
            Entity who = e.getEntity();
            if (who instanceof Player p && p.isCreative()) return;
            boolean refused = refuses(e.getPos());
            if (!refused && e instanceof BlockEvent.EntityMultiPlaceEvent multi) {
                for (BlockSnapshot s : multi.getReplacedBlockSnapshots()) {
                    if (refuses(s.getPos())) {
                        refused = true;
                        break;
                    }
                }
            }
            if (!refused) return;
            e.setCanceled(true);
            if (who instanceof Player p) tell(p, e.getPos());
        } catch (RuntimeException ex) {
            failed("place guard", ex);
        }
    }

    /** A blast still hurts whoever stands in it; it takes no block of a protected volume. */
    private static void onExplosion(ExplosionEvent.Detonate e) {
        try {
            Level level = e.getLevel();
            if (!serverSanctuary(level)) return;
            e.getAffectedBlocks().removeIf(HubRules::refuses);
        } catch (RuntimeException ex) {
            failed("explosion guard", ex);
        }
    }

    /** Stops the crack animation on a protected block (the break event alone would let the client start breaking it and then put it back). */
    private static void onLeftClick(PlayerInteractEvent.LeftClickBlock e) {
        try {
            Level level = e.getLevel();
            if (!serverSanctuary(level) || e.getEntity().isCreative() || !refuses(e.getPos())) return;
            e.setCanceled(true);
            tell(e.getEntity(), e.getPos());
        } catch (RuntimeException ex) {
            failed("left-click guard", ex);
        }
    }

    /** A bucket, a flint and steel or a fire charge used on or against a protected block: water and lava would flow over the paving, fire would spread. */
    private static void onRightClick(PlayerInteractEvent.RightClickBlock e) {
        try {
            Level level = e.getLevel();
            if (!serverSanctuary(level) || e.getEntity().isCreative()) return;
            Item item = e.getItemStack().getItem();
            if (!(item instanceof BucketItem || item instanceof FlintAndSteelItem || item instanceof FireChargeItem)) return;
            BlockPos hit = e.getPos();
            BlockPos beside = e.getFace() == null ? hit : hit.relative(e.getFace());
            if (!refuses(hit) && !refuses(beside)) return;
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.FAIL);
            tell(e.getEntity(), hit);
        } catch (RuntimeException ex) {
            failed("right-click guard", ex);
        }
    }

    /** Tells the player (action bar, at most once per {@link HubProtect#MESSAGE_INTERVAL} ticks) why nothing happened. */
    private static void tell(Player p, BlockPos pos) {
        if (p == null) return;
        long now = p.level().getGameTime();
        CompoundTag data = p.getPersistentData();
        if (!HubProtect.messageDue(now, data.getLong(HubKeys.TOLD))) return;
        data.putLong(HubKeys.TOLD, now);
        boolean readOnly = !HubConfig.get(HubConfig.BUILD_ENABLED) && !HubProtect.isProtected(pos.getX(), pos.getY(), pos.getZ());
        p.displayClientMessage(readOnly
                ? Component.translatableWithFallback("message.nusmp.mega.mhb.readonly", "The Sanctuary is not open to building.")
                : Component.translatableWithFallback("message.nusmp.mega.mhb.protected", "This ground is protected: the square, the way home and the gates cannot be changed."), true);
    }

    // ---------------------------------------------------------------- no foreign structures (the alarm)
    private static void onChunkLoad(ChunkEvent.Load e) {
        try {
            if (warnedStructure || !(e.getLevel() instanceof Level level) || !serverSanctuary(level)) return;
            ChunkAccess chunk = e.getChunk();
            if (chunk.getAllStarts().isEmpty()) return;
            warnedStructure = true;
            Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
            List<String> ids = new ArrayList<>();
            for (Structure s : chunk.getAllStarts().keySet()) {
                ResourceLocation id = registry.getKey(s);
                ids.add(id == null ? "(unregistered)" : id.toString());
            }
            LOG.warn("A chunk of the Sanctuary ({}) loaded with structure starts {}: another mod's structure generates here. The Sanctuary's own generator places none, "
                    + "so something else is adding them (see docs/labyrinth/mega_hub.md). This warning is logged once per run.", chunk.getPos(), ids);
        } catch (RuntimeException ex) {
            failed("structure alarm", ex);
        }
    }

    private static void onServerStopped(ServerStoppedEvent e) {
        warnedStructure = false;
        warnedFailure = false;
    }

    private static void failed(String what, RuntimeException ex) {
        if (warnedFailure) return;
        warnedFailure = true;
        LOG.warn("The Sanctuary {} failed once and was skipped: {}", what, ex.toString());
    }
}
