package com.newuniverse.nusmp.labyrinth.mega.guard;

import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.guard.AltarRules.Act;
import com.newuniverse.nusmp.labyrinth.mega.guard.AltarRules.State;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Why: the Guardian is spawned ON APPROACH by this block entity, not written into the template as an entity, for the reasons the Peak Altar
 * gave in 1.35.0 (an entity in a template is placed when the chunk generates, long before a party arrives; it would wander, fall, be
 * despawned or already be fighting) and one more of the Mega-Dungeon's: the sector's threat and theme are only known once the generator
 * has saved the sector, which is after its pieces were placed. So the altar reads the sector at the moment a survival player comes within
 * the approach radius ({@code MegaDungeon.sectorAt}: no roster in NBT, the boss is always the Labyrinth Guardian), builds the Guardian for
 * that sector and is a three-state machine saved in NBT: IDLE, ACTIVE (its Guardian lives), DONE. The DECISIONS are the pure
 * {@link AltarRules} (tested); this class gathers what the altar sees and carries the decision out. It differs from the Peak Altar in
 * three ways. It does not scan for doors: the sector's Gatekeeper opens only to a Mana Core. DONE forges that core above the altar (an
 * indestructible, glowing, never-despawning item, so a party can never be robbed of it) and calls {@code MegaDungeon.guardianDefeated};
 * only a confirmed death finishes the fight (a Guardian that merely vanished resets it). And DONE is not final for the CORE: while the
 * sector's door is still sealed and no core of the sector exists, a player can ask the spent altar for a new one after
 * {@code reforgeMinutes} ({@link GuardianScaling#canReforge}), the cure for a core lost to the void or a log-out. An altar in a sector
 * that is not ACTIVE (an admin's test, a regenerated sector) works the same way: the state machine never depends on the sector's state,
 * and {@code guardianDefeated} itself ignores a sector that is not ACTIVE.
 */
public class GuardianAltarBlockEntity extends BlockEntity {
    private int radiusOverride = -1;
    private State state = State.IDLE;
    private UUID boss;
    /** Where the boss was last seen: if that spot is loaded and the entity is gone, it is gone. */
    private BlockPos lastBossPos;
    /** The Guardian told the altar it died. */
    private boolean bossDied;
    /** Game time a survival player was last within the wide radius while ACTIVE, when the state began, and when a core was last forged (-1 never). */
    private long idleSince = -1, since, lastForge = -1;
    private boolean coreForged;
    private int ticks;

    public GuardianAltarBlockEntity(BlockPos pos, BlockState state) {
        super(GuardRegistry.ALTAR_BE.get(), pos, state);
    }

    public State state() { return state; }

    public boolean coreForged() { return coreForged; }

    private int radius() { return radiusOverride > 0 ? radiusOverride : GuardConfig.get(GuardConfig.APPROACH_RADIUS); }

    public static void serverTick(Level level, BlockPos pos, BlockState blockState, GuardianAltarBlockEntity altar) {
        if (!(level instanceof ServerLevel sl)) return;
        if (++altar.ticks % AltarRules.POLL_TICKS != 0) return;
        try {
            altar.tick(sl);
        } catch (RuntimeException e) {
            GuardLog.failed("altar tick", e);                                              // a ticker that throws would crash the server tick: log it and poll again later
        }
    }

    private void tick(ServerLevel sl) {
        if (state == State.DONE) return;                                                   // permanent, except for the re-forge a click asks for
        long now = sl.getGameTime();
        Sector sector = MegaDungeon.sectorAt(sl, worldPosition);
        int r = radius();
        boolean near = playerWithin(sl, r), wide = playerWithin(sl, r + AltarRules.LEAVE_MARGIN);
        if (state == State.ACTIVE) idleSince = AltarRules.idleSince(idleSince, wide, now);
        Entity e = boss != null ? sl.getEntity(boss) : null;
        boolean dead = e != null && (!e.isAlive() || (e instanceof LivingEntity le && le.isDeadOrDying()));
        if (e != null) lastBossPos = e.blockPosition();
        boolean ready = sector == null || (sector.state != Sector.State.GENERATING && sector.state != Sector.State.FAILED);
        AltarRules.Look look = new AltarRules.Look(state, near, wide, ready, e != null, dead, bossDied, lastBossPos != null,
                lastBossPos != null && sl.isLoaded(lastBossPos), now, since, idleSince, AltarRules.abandonTicks(GuardConfig.get(GuardConfig.ABANDON_SECONDS)));
        Act act = AltarRules.decide(look);
        switch (act) {
            case SUMMON -> summon(sl, sector, now);
            case FINISH -> finish(sl, sector, now);
            case RESET -> reset(sl, e, now);
            default -> { }
        }
    }

    /** One poll of the state machine, now, whatever the tick counter says (what the in-game self-tests call: the same code as the block's own ticker). */
    public void pollNow(ServerLevel sl) { tick(sl); }

    /**
     * Calls the Guardian now, without waiting for a player to come near (the in-game self-tests and admin tooling): the same {@link #summon} the
     * approach triggers. Returns the Guardian, or null when this altar is not IDLE or the sector cannot host a fight.
     */
    public LabyrinthGuardianEntity summonNow(ServerLevel sl) {
        if (state != State.IDLE) return null;
        Sector sector = MegaDungeon.sectorAt(sl, worldPosition);
        if (sector != null && (sector.state == Sector.State.GENERATING || sector.state == Sector.State.FAILED)) return null;
        return summon(sl, sector, sl.getGameTime());
    }

    /** The UUID of the Guardian this altar called, or null. */
    public UUID bossId() { return boss; }

    // ---------------------------------------------------------------- IDLE -> ACTIVE
    private LabyrinthGuardianEntity summon(ServerLevel sl, Sector sector, long now) {
        BlockPos at = worldPosition.above();
        LabyrinthGuardianEntity g = LabyrinthGuardianEntity.create(sl, at, sector, worldPosition);
        if (g == null) return null;                                                        // nothing could be made: stay IDLE and try again next poll
        if (!sl.addFreshEntity(g)) return null;
        sl.sendParticles(ParticleTypes.END_ROD, at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5, 80, 1.5, 2.0, 1.5, 0.05);
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 60, 1.0, 1.5, 1.0, 0.1);
        sl.playSound(null, worldPosition, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.0f, 0.6f);
        broadcast(sl, 48, Component.translatable("message.nusmp.mega.mgd.altar.answers").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        boss = g.getUUID();
        lastBossPos = at;
        bossDied = false;
        state = State.ACTIVE;
        since = now;
        idleSince = now;
        setChanged();
        return g;
    }

    // ---------------------------------------------------------------- ACTIVE -> DONE
    private void finish(ServerLevel sl, Sector sector, long now) {
        state = State.DONE;
        boss = null;
        bossDied = false;
        if (sector != null) {
            forge(sl, sector, now);
            MegaDungeon.guardianDefeated(sl, sector.id);                                   // ACTIVE -> GUARDIAN_DEAD (a no-op for any other state)
            broadcast(sl, 48, Component.translatable("message.nusmp.mega.mgd.altar.core_forged", sector.id).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        } else {
            broadcast(sl, 48, Component.translatable("message.nusmp.mega.mgd.altar.no_sector").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        sl.playSound(null, worldPosition, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.5f, 1.0f);
        sl.sendParticles(ParticleTypes.END_ROD, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5, 60, 0.8, 1.2, 0.8, 0.08);
        setChanged();
    }

    /** The Guardian's own report of its death: the next poll finishes the fight (a death that happens between two polls cannot be missed). */
    public void onBossDied(UUID who) {
        if (state != State.ACTIVE || boss == null || !boss.equals(who)) return;
        bossDied = true;
        setChanged();
    }

    // ---------------------------------------------------------------- the core
    /** Spawns a Mana Core of the sector floating above the altar: never despawns, nothing can destroy it, it glows through walls. */
    private void forge(ServerLevel sl, Sector sector, long now) {
        ItemEntity core = new ItemEntity(sl, worldPosition.getX() + 0.5, worldPosition.getY() + 1.45, worldPosition.getZ() + 0.5, ManaCoreItem.forSector(sector.id, sector.theme));
        ManaCoreItem.protect(core);
        core.setNoGravity(true);
        core.setDeltaMovement(Vec3.ZERO);
        core.setPickUpDelay(30);
        sl.addFreshEntity(core);
        lastForge = now;
        coreForged = true;
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, worldPosition.getX() + 0.5, worldPosition.getY() + 1.45, worldPosition.getZ() + 0.5, 50, 0.4, 0.6, 0.4, 0.08);
        sl.playSound(null, worldPosition, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.4f, 0.8f);
        setChanged();
    }

    /** The admin's forge: no rules, a core of the sector at once (a spent altar or not). Returns false when the altar belongs to no sector. */
    public boolean forgeNow(ServerLevel sl) {
        Sector sector = MegaDungeon.sectorAt(sl, worldPosition);
        if (sector == null) return false;
        forge(sl, sector, sl.getGameTime());
        return true;
    }

    /** Does a core of this sector exist where a player could still reach it: an item entity near the altar, or in the pack of any online player? */
    private boolean coreExists(ServerLevel sl, int sectorId) {
        AABB box = new AABB(worldPosition).inflate(64.0);
        if (!sl.getEntitiesOfClass(ItemEntity.class, box, ie -> ManaCoreItem.sectorOf(ie.getItem()) == sectorId).isEmpty()) return true;
        for (ServerPlayer p : sl.getServer().getPlayerList().getPlayers()) {
            if (p.getInventory().contains(s -> ManaCoreItem.sectorOf(s) == sectorId)) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- a click
    /** What a right click does: say where the altar stands; a spent altar with an empty hand may re-forge a lost core. */
    public void interact(ServerLevel sl, ServerPlayer player) {
        try {
            switch (state) {
                case IDLE -> say(player, "message.nusmp.mega.mgd.altar.idle");
                case ACTIVE -> say(player, "message.nusmp.mega.mgd.altar.active");
                case DONE -> reforge(sl, player);
            }
        } catch (RuntimeException e) {
            GuardLog.failed("altar click", e);                                             // a click that throws would disconnect the player
        }
    }

    private void reforge(ServerLevel sl, ServerPlayer player) {
        Sector sector = MegaDungeon.sectorAt(sl, worldPosition);
        if (sector == null) { say(player, "message.nusmp.mega.mgd.altar.no_sector"); return; }
        if (!player.getMainHandItem().isEmpty() || player.isCreative()) { say(player, "message.nusmp.mega.mgd.altar.done"); return; }
        boolean sealed = sector.state == Sector.State.GUARDIAN_DEAD;
        if (!sealed) { say(player, "message.nusmp.mega.mgd.altar.done"); return; }
        boolean exists = coreExists(sl, sector.id);
        long now = sl.getGameTime();
        int minutes = GuardConfig.get(GuardConfig.REFORGE_MINUTES);
        if (GuardianScaling.canReforge(true, true, exists, lastForge, now, minutes)) {
            forge(sl, sector, now);
            say(player, "message.nusmp.mega.mgd.altar.reforged");
        } else if (exists) {
            say(player, "message.nusmp.mega.mgd.altar.core_exists");
        } else {
            long seconds = (GuardianScaling.reforgeWaitTicks(lastForge, now, minutes) + 19) / 20;
            player.displayClientMessage(Component.translatable("message.nusmp.mega.mgd.altar.wait", seconds).withStyle(ChatFormatting.DARK_PURPLE), true);
        }
    }

    private static void say(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE), true);
    }

    // ---------------------------------------------------------------- ACTIVE -> IDLE (abandoned, or the boss vanished)
    private void reset(ServerLevel sl, Entity e, long now) {
        if (e != null) {
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, e.getX(), e.getY() + 1.0, e.getZ(), 40, 0.6, 1.0, 0.6, 0.05);
            e.discard();
        }
        boss = null;
        lastBossPos = null;
        bossDied = false;
        idleSince = -1;
        state = State.IDLE;
        since = now;
        setChanged();
    }

    // ---------------------------------------------------------------- helpers
    /**
     * Test seam, set only by {@link GuardSelfTests}: while it is not null these persons are the only people any altar can see. The stand-in for a
     * person in a self-test is in no level's player list (nothing adds it there, and adding it would show a connection-less player to every other
     * system), so the approach check could never find it; the tests of the approach rule hand it over here instead.
     */
    static volatile List<Player> testPlayers;

    private boolean playerWithin(ServerLevel sl, int r) {
        double cx = worldPosition.getX() + 0.5, cy = worldPosition.getY() + 0.5, cz = worldPosition.getZ() + 0.5;
        List<? extends Player> seen = testPlayers != null ? testPlayers : sl.players();
        for (Player p : seen) {
            if (!p.isSpectator() && !p.isCreative() && p.distanceToSqr(cx, cy, cz) <= (double) r * r) return true;
        }
        return false;
    }

    private void broadcast(ServerLevel sl, int r, Component line) {
        double cx = worldPosition.getX() + 0.5, cy = worldPosition.getY() + 0.5, cz = worldPosition.getZ() + 0.5;
        for (ServerPlayer p : sl.players()) if (p.distanceToSqr(cx, cy, cz) <= (double) r * r) p.displayClientMessage(line, false);
    }

    // ---------------------------------------------------------------- NBT
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (radiusOverride > 0) tag.putInt("radius", radiusOverride);
        tag.putString("state", state.name());
        if (boss != null) tag.putUUID("boss", boss);
        if (lastBossPos != null) tag.putLong("bossPos", lastBossPos.asLong());
        tag.putBoolean("bossDied", bossDied);
        tag.putLong("idleSince", idleSince);
        tag.putLong("since", since);
        tag.putLong("lastForge", lastForge);
        tag.putBoolean("coreForged", coreForged);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        radiusOverride = tag.contains("radius") ? Math.max(1, tag.getInt("radius")) : -1;
        State loaded = State.IDLE;
        if (tag.contains("state")) try { loaded = State.valueOf(tag.getString("state")); } catch (IllegalArgumentException unknown) { loaded = State.IDLE; }
        boss = tag.hasUUID("boss") ? tag.getUUID("boss") : null;
        state = AltarRules.sanitize(loaded, boss != null);                                 // an ACTIVE altar with no boss id cannot finish: start over
        lastBossPos = tag.contains("bossPos") ? BlockPos.of(tag.getLong("bossPos")) : null;
        bossDied = tag.getBoolean("bossDied");
        idleSince = tag.contains("idleSince") ? tag.getLong("idleSince") : -1;
        since = tag.getLong("since");
        lastForge = tag.contains("lastForge") ? tag.getLong("lastForge") : -1;
        coreForged = tag.getBoolean("coreForged");
    }
}
