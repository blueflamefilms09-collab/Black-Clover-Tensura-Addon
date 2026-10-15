package com.newuniverse.nusmp.labyrinth.seal;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.labyrinth.LabyrinthConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Why: the floor boss is spawned ON APPROACH by this block entity, not written into the template as an entity. An entity in a structure
 * template is placed when the chunk generates, long before any player stands there: it would wander off the platform, fall into the void,
 * be despawned, or already be fighting when the party arrives, and it would be chosen at world-gen with no way to read the server config.
 * Spawning when the first survival player comes within {@code radius} (every 10 ticks, cheap) means the boss is picked at that moment from
 * the roster in the template's NBT ({@link BossRoster}, so the roster is data), finalised as a STRUCTURE spawn with persistence and the
 * {@code nusmp_peak_boss} tag, and the fight starts when the party is there to see it. The altar is a three-state machine saved in NBT:
 * IDLE (waiting), ACTIVE (its boss is alive), DONE (the boss died: the Vault's {@code gatekeeper_door[kind=boss]} are opened by ONE scan of a
 * box around the altar, because a rotated template leaves no usable stored offsets, and the block is its own anchor). DONE is permanent:
 * a floor is cleared once per world. While ACTIVE, a party that leaves (no player within {@code radius + 16} for 120 s) gets the boss
 * discarded and the altar reset to IDLE: a persistent boss left half-dead on a platform nobody is on would otherwise sit there forever, a
 * Zogratis could walk off and keep hunting elsewhere, and a party that wiped deserves a fresh fight, not a boss camped on the stairs.
 */
public class PeakAltarBlockEntity extends BlockEntity {
    public enum State { IDLE, ACTIVE, DONE }

    /** The tag every boss the altar called carries (other systems can recognise a Peak boss without knowing its type). */
    public static final String BOSS_TAG = "nusmp_peak_boss";
    private static final int POLL = 10, ABANDON_TICKS = 120 * 20, SPAWN_GRACE = 100;
    private static final int DOOR_RANGE = 40, DOOR_RANGE_Y = 12, ANNOUNCE_RANGE = 48, LEAVE_MARGIN = 16;

    private int radius = 16;
    private final List<BossRoster.Entry> bosses = new ArrayList<>();
    private State state = State.IDLE;
    private UUID boss;
    /** Where the boss was last seen: if that spot is loaded and the entity is gone, the boss is gone. */
    private BlockPos lastBossPos;
    /** Game time a survival player was last within reach while ACTIVE, and when the current state began. */
    private long idleSince = -1, since;
    private int ticks;

    public PeakAltarBlockEntity(BlockPos pos, BlockState state) {
        super(SealRegistry.PEAK_ALTAR_BE.get(), pos, state);
    }

    public State state() { return state; }

    public static void serverTick(Level level, BlockPos pos, BlockState blockState, PeakAltarBlockEntity altar) {
        if (!(level instanceof ServerLevel sl)) return;
        if (++altar.ticks % POLL != 0) return;
        altar.tick(sl);
    }

    private void tick(ServerLevel sl) {
        long now = sl.getGameTime();
        switch (state) {
            case IDLE -> { if (playerWithin(sl, radius)) summon(sl, now); }
            case ACTIVE -> {
                Entity e = boss != null ? sl.getEntity(boss) : null;
                if (e != null) {
                    lastBossPos = e.blockPosition();
                    if (!e.isAlive() || (e instanceof LivingEntity le && le.isDeadOrDying())) { finish(sl); return; }
                } else if (now - since > SPAWN_GRACE && (lastBossPos == null || sl.isLoaded(lastBossPos))) {
                    finish(sl);                                                            // gone while its ground is loaded: killed, cleared or never made it
                    return;
                }
                if (playerWithin(sl, radius + LEAVE_MARGIN)) idleSince = now;
                else if (idleSince >= 0 && now - idleSince >= ABANDON_TICKS) reset(sl, e);
            }
            case DONE -> {}                                                                // permanent
        }
    }

    // ---------------------------------------------------------------- IDLE -> ACTIVE
    private void summon(ServerLevel sl, long now) {
        long seed = BossRoster.seed(worldPosition.asLong(), now);
        String id = LabyrinthConfig.get(LabyrinthConfig.BOSS_ROSTER_ENABLED)
                ? BossRoster.pickOrFallback(bosses, seed, s -> EntityType.byString(s).isPresent()) : BossRoster.FALLBACK;
        Entity e = create(sl, id);
        if (e == null && !BossRoster.FALLBACK.equals(id)) e = create(sl, BossRoster.FALLBACK);
        if (e == null) return;                                                             // nothing could be made: stay IDLE and try again next poll
        BlockPos at = worldPosition.above();
        sl.sendParticles(ParticleTypes.END_ROD, at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5, 80, 1.5, 2.0, 1.5, 0.05);
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 60, 1.0, 1.5, 1.0, 0.1);
        sl.playSound(null, worldPosition, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.0f, 0.6f);
        broadcast(sl, ANNOUNCE_RANGE, Component.translatable("message.nusmp.peak_altar.answers").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        boss = e.getUUID();
        lastBossPos = at;
        state = State.ACTIVE;
        since = now;
        idleSince = now;
        setChanged();
    }

    /** The trial's spawn pattern: by id, STRUCTURE-finalised, persistent, tagged; null when the id is unknown or the entity cannot be made. */
    private Entity create(ServerLevel sl, String id) {
        Optional<EntityType<?>> type = EntityType.byString(id);
        if (type.isEmpty()) return null;
        Entity e = type.get().create(sl);
        if (e == null) return null;
        BlockPos at = worldPosition.above();
        e.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, sl.getRandom().nextFloat() * 360f, 0f);
        if (e instanceof Mob m) {
            m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
            m.setPersistenceRequired();
        }
        e.addTag(BOSS_TAG);
        return sl.addFreshEntity(e) ? e : null;
    }

    // ---------------------------------------------------------------- ACTIVE -> DONE
    private void finish(ServerLevel sl) {
        state = State.DONE;
        boss = null;
        openBossDoors(sl);
        sl.playSound(null, worldPosition, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.5f, 1.0f);
        sl.sendParticles(ParticleTypes.END_ROD, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5, 60, 0.8, 1.2, 0.8, 0.08);
        broadcast(sl, ANNOUNCE_RANGE, Component.translatable("message.nusmp.peak_altar.unseals").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        setChanged();
    }

    /** One scan of the box +-40 x +-12 y +-40 around the altar: every closed {@code kind=boss} Gatekeeper block opens. */
    private int openBossDoors(ServerLevel sl) {
        int[] opened = {0};
        BlockPos a = worldPosition.offset(-DOOR_RANGE, -DOOR_RANGE_Y, -DOOR_RANGE), b = worldPosition.offset(DOOR_RANGE, DOOR_RANGE_Y, DOOR_RANGE);
        DungeonGuard.internal(() -> {
            for (BlockPos p : BlockPos.betweenClosed(a, b)) {
                if (GatekeeperDoorBlock.openOne(sl, p.immutable(), GatekeeperDoorBlock.Kind.BOSS)) opened[0]++;
            }
        });
        return opened[0];
    }

    // ---------------------------------------------------------------- ACTIVE -> IDLE (abandoned)
    private void reset(ServerLevel sl, Entity e) {
        if (e != null) {
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, e.getX(), e.getY() + 1.0, e.getZ(), 40, 0.6, 1.0, 0.6, 0.05);
            e.discard();
        }
        boss = null;
        lastBossPos = null;
        idleSince = -1;
        state = State.IDLE;
        since = sl.getGameTime();
        setChanged();
    }

    // ---------------------------------------------------------------- helpers
    private boolean playerWithin(ServerLevel sl, int r) {
        double cx = worldPosition.getX() + 0.5, cy = worldPosition.getY() + 0.5, cz = worldPosition.getZ() + 0.5;
        for (ServerPlayer p : sl.players()) {
            if (!p.isSpectator() && !p.isCreative() && p.distanceToSqr(cx, cy, cz) <= (double) r * r) return true;
        }
        return false;
    }

    private void broadcast(ServerLevel sl, int r, Component line) {
        double cx = worldPosition.getX() + 0.5, cy = worldPosition.getY() + 0.5, cz = worldPosition.getZ() + 0.5;
        for (ServerPlayer p : sl.players()) if (p.distanceToSqr(cx, cy, cz) <= (double) r * r) p.displayClientMessage(line, false);
    }

    // ---------------------------------------------------------------- NBT (the template writes radius + bosses; the rest is runtime)
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("radius", radius);
        ListTag list = new ListTag();
        for (BossRoster.Entry e : bosses) {
            CompoundTag c = new CompoundTag();
            c.putString("id", e.id());
            c.putInt("weight", e.weight());
            list.add(c);
        }
        tag.put("bosses", list);
        tag.putString("state", state.name());
        if (boss != null) tag.putUUID("boss", boss);
        if (lastBossPos != null) tag.putLong("bossPos", lastBossPos.asLong());
        tag.putLong("idleSince", idleSince);
        tag.putLong("since", since);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        radius = tag.contains("radius") ? Math.max(1, tag.getInt("radius")) : 16;
        bosses.clear();
        ListTag list = tag.getList("bosses", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            if (c.contains("id")) bosses.add(new BossRoster.Entry(c.getString("id"), c.contains("weight") ? c.getInt("weight") : 1));
        }
        state = State.IDLE;
        if (tag.contains("state")) try { state = State.valueOf(tag.getString("state")); } catch (IllegalArgumentException unknown) { state = State.IDLE; }
        boss = tag.hasUUID("boss") ? tag.getUUID("boss") : null;
        lastBossPos = tag.contains("bossPos") ? BlockPos.of(tag.getLong("bossPos")) : null;
        idleSince = tag.contains("idleSince") ? tag.getLong("idleSince") : -1;
        since = tag.getLong("since");
        if (state == State.ACTIVE && boss == null) state = State.IDLE;                       // an ACTIVE altar with no boss id cannot finish: start over
    }
}
