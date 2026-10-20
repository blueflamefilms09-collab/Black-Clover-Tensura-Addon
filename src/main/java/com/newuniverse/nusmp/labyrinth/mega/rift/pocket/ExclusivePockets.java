package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.item.ElsdociaEvents;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import com.newuniverse.nusmp.labyrinth.mega.party.PartyService;
import com.newuniverse.nusmp.labyrinth.mega.rift.DimensionalRiftBlockEntity;
import com.newuniverse.nusmp.labyrinth.mega.rift.PocketRealm;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftConfig;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftRules;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftService;
import com.newuniverse.nusmp.labyrinth.mega.rift.WaypointService;
import com.newuniverse.nusmp.labyrinth.vault.RelicPedestalBlockEntity;
import com.newuniverse.nusmp.quests.NusmpEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Why: the life of an exclusive tomb, from the tear to the empty cell, in one place the pocket realm hands over to. {@link #enter} is what stepping into a mega-rare tear does: it finds the tomb that tear
 * already opened, or makes one (a free slot of the pocket field, within the cap on concurrent tombs), builds its entry hall at once so there is a floor under the first delver, and takes the party in
 * at a safe landing with the way home remembered. {@link #tick} is the realm's second-by-second clock for a tomb (expiry, an empty tomb, the boss, the primary relic, the countdown of a collapse);
 * {@link #fast} is the every-tick work (the build, the collapse and the sweep as jobs, the gravity, the traps, the waking of rooms, the aura, the net under anyone who falls out of the world).
 * {@link #collapse} throws everyone out and starts the sweep that gives the cell back; the record stays in the realm until the sweep is done, so a slot is never reused over a half-cleared tomb.
 * The seal of the treasury ({@link #addForce}, {@link #openSeal}), the death of the boss ({@link #bossDied}) and the relic taken ({@link #relicTaken}) are decided here too. Server thread only; every public
 * entry is guarded so a fault in one tomb is logged and never reaches the tick loop.
 */
public final class ExclusivePockets {
    private ExclusivePockets() {}

    private static final Logger LOG = LogUtils.getLogger();
    /** Tombs whose definition is gone (a datapack dropped it) are swept by a job of their own, keyed by slot. */
    private static final Map<Integer, PocketClear> ORPHANS = new HashMap<>();
    public static final int CLEAR_BUDGET = 16384;
    private static final int CRACK_ID = -7_100_000;
    /** The marks a delver who dies in a tomb carries to the respawn: the items they keep, and that they died inside. */
    public static final String KEPT = "nusmp_tomb_kept", DIED = "nusmp_tomb_died";

    // ================================================================ entering
    /** Stepping into a mega-rare tear. */
    public static void enter(ServerLevel from, ServerPlayer p, DimensionalRiftBlockEntity rift) {
        MinecraftServer server = from.getServer();
        ServerLevel mega = MegaDungeon.level(server);
        PocketDef def = PocketDefs.get(rift.pocketId());
        if (mega == null || def == null) {
            WaypointService.say(p, "tomb.lost", "The tear flickers: the tomb behind it is lost to time.", ChatFormatting.GRAY);
            if (def == null) rift.close();
            return;
        }
        PocketRealm realm = PocketRealm.get(server);
        PocketRealm.Pocket pocket = rift.slot() >= 0 ? realm.at(rift.slot()) : null;
        if (pocket != null && (!pocket.exclusive() || pocket.seed != rift.seed() || !pocket.pocketId.equals(def.id()))) pocket = null;
        if (pocket == null && rift.slot() >= 0) {                                              // this tear opened a tomb once and it has fallen: one tear, one tomb
            WaypointService.say(p, "tomb.spent", "The tear is spent: the tomb behind it has fallen.", ChatFormatting.GRAY);
            rift.close();
            return;
        }
        if (pocket == null) {
            if (realm.exclusiveCount() >= RiftConfig.get(RiftConfig.EXCLUSIVE_MAX)) {
                WaypointService.say(p, "tomb.full", "The tear will not open: too many tombs stand open at once. Wait for one to fall.", ChatFormatting.GRAY);
                return;
            }
            int floor = 1;
            if (Labyrinth.inMega(from)) {
                Sector s = MegaData.get(server).sectorAt(rift.getBlockPos());
                if (s != null) floor = Math.abs(s.firstFloor());
            }
            pocket = create(mega, realm, def, rift.seed(), floor);
            if (pocket == null) {
                WaypointService.say(p, "tomb.failed", "The tear shudders and lets go of you. (The tomb could not be raised; the cause is in the server log.)", ChatFormatting.RED);
                return;
            }
            rift.aimPocket(pocket.slot);
            rift.limitLife(20 * 180);
        }
        if (pocket.phase >= 2) {
            WaypointService.say(p, "tomb.falling", "The tomb behind the tear is coming down. There is nothing to enter.", ChatFormatting.GRAY);
            return;
        }
        List<ServerPlayer> group = new ArrayList<>();
        group.add(p);
        for (ServerPlayer o : from.players())
            if (o != p && o.distanceToSqr(p) <= 36 && !o.isSpectator() && PartyService.partyOf(o) == PartyService.partyOf(p)) group.add(o);
        join(mega, realm, pocket, def, group, rift.getBlockPos());
    }

    /** Takes players into a tomb: the way home remembered, put down at a safe landing in the entry hall. */
    public static void join(ServerLevel mega, PocketRealm realm, PocketRealm.Pocket pocket, PocketDef def, List<ServerPlayer> group, BlockPos riftPos) {
        PocketRuntime rt = PocketRuntime.of(pocket);
        if (rt == null) return;
        BlockPos at = landing(mega, rt);
        for (ServerPlayer m : group) {
            CompoundTag r = new CompoundTag();
            r.putString("Dim", m.level().dimension().location().toString());
            r.putDouble("X", m.getX());
            r.putDouble("Y", m.getY());
            r.putDouble("Z", m.getZ());
            r.putFloat("Yaw", m.getYRot());
            r.putFloat("Pitch", m.getXRot());
            r.putInt("Slot", pocket.slot);
            if (riftPos != null) {
                r.putInt("RX", riftPos.getX());
                r.putInt("RY", riftPos.getY());
                r.putInt("RZ", riftPos.getZ());
            }
            m.getPersistentData().put(PocketRealm.RETURN, r);
            mega.getChunk(at.getX() >> 4, at.getZ() >> 4);
            PocketRealm.move(m, mega, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, -90f, 0f);
            m.resetFallDistance();
            RiftService.title(m, Component.translatableWithFallback("pocket.nusmp." + def.id() + ".name", def.name()),
                    Component.translatableWithFallback("pocket.nusmp." + def.id() + ".lore", def.lore()));
            mega.playSound(null, at, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.5f, 0.6f);
            NusmpEvents.fire(m, "mega.pocket", "tomb");
            NusmpEvents.fire(m, "mega.tomb_entered", def.id());
        }
        pocket.lastOccupied = mega.getGameTime();
        realm.setDirty();
    }

    /** A free, solid, two-block-high spot in the entry hall, nearest the arrival tile (a spiral of rings), falling back to the tile itself. */
    public static BlockPos landing(ServerLevel mega, PocketRuntime rt) {
        BlockPos base = rt.arrival != null ? rt.arrival : new BlockPos(PocketCell.centreX(rt.slot), PocketPlan.Y0, PocketCell.centreZ());
        for (int ring = 0; ring <= 4; ring++)
            for (int dx = -ring; dx <= ring; dx++)
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    BlockPos feet = base.offset(dx, 0, dz);
                    if (mega.getBlockState(feet.below()).blocksMotion() && mega.getBlockState(feet).getCollisionShape(mega, feet).isEmpty() && mega.getBlockState(feet.above()).getCollisionShape(mega, feet.above()).isEmpty()
                            && mega.getBlockState(feet).getFluidState().isEmpty()) return feet;
                }
        return base;
    }

    // ================================================================ making a tomb
    /** Raises a tomb of {@code def} in the first free slot (null when none is free or the entry hall could not be built). The caller checks the cap. */
    public static PocketRealm.Pocket create(ServerLevel mega, PocketRealm realm, PocketDef def, long seed, int originFloor) { return create(mega, realm, def, seed, originFloor, -1); }

    /** The same in a chosen slot ({@code -1}: the first free one; a slot that is taken or outside the field gives null). The self-tests use the last-but-one slot so they never touch a live tomb. */
    public static PocketRealm.Pocket create(ServerLevel mega, PocketRealm realm, PocketDef def, long seed, int originFloor, int wantSlot) {
        int slot = wantSlot >= 0 ? (wantSlot < RiftRules.MAX_SLOTS && !realm.pockets().containsKey(wantSlot) ? wantSlot : -1) : RiftRules.firstFreeSlot(realm.pockets().keySet(), RiftRules.MAX_SLOTS);
        if (slot < 0) return null;
        long now = mega.getGameTime();
        PocketRealm.Pocket p = new PocketRealm.Pocket();
        p.slot = slot;
        p.floor = Math.max(1, Math.abs(originFloor));
        p.variant = "boss";                                                                       // the old kinds' field: kept so nothing that reads it breaks
        p.seed = seed;
        p.createdAt = now;
        p.expiresAt = now + 20L * 60L * RiftConfig.get(RiftConfig.EXCLUSIVE_MINUTES);
        p.pocketId = def.id();
        p.phase = 0;
        p.sealHp = def.seal().forceHp();
        p.originFloor = Math.max(1, Math.abs(originFloor));
        p.lastOccupied = now;
        PocketRuntime.forget(slot);
        ORPHANS.remove(slot);
        PocketRuntime rt = PocketRuntime.of(p);
        if (rt == null) return null;
        rt.build = new PocketBuild(rt);
        try {
            rt.build.entry(mega);
        } catch (RuntimeException | LinkageError ex) {
            LOG.warn("[nusmp] The entry hall of the tomb {} could not be built (the cell is cleared): {}", def.id(), ex.toString());
            LOG.debug("tomb entry", ex);
            p.phase = 3;
            rt.clear = new PocketClear(slot);
            realm.pockets().put(slot, p);
            realm.setDirty();
            return null;
        }
        if (rt.arrival != null) p.entry = rt.arrival;
        if (rt.exit != null) p.exit = rt.exit;
        if (rt.bossSpawn != null) p.bossSpawn = rt.bossSpawn;
        realm.pockets().put(slot, p);
        realm.setDirty();
        LOG.info("[nusmp] The tomb {} rises in slot {} ({} rooms, seed {}).", def.id(), slot, rt.layout.rooms().size(), seed);
        return p;
    }

    // ================================================================ the clock (every second)
    /** One second of a tomb. Returns true when it should collapse now. */
    public static boolean tick(ServerLevel mega, PocketRealm realm, PocketRealm.Pocket p, List<ServerPlayer> in, long now, int warn) {
        PocketRuntime rt = PocketRuntime.of(p);
        if (p.phase >= 3) {
            finishClear(mega, realm, p, rt);
            return false;
        }
        if (rt == null) {                                                                         // the definition is gone: eject, then sweep the cell
            LOG.warn("[nusmp] The tomb '{}' in slot {} has no definition any more; it is collapsed.", p.pocketId, p.slot);
            return true;
        }
        if (!in.isEmpty()) {
            if (p.lastOccupied < 0 || now - p.lastOccupied >= 200) realm.setDirty();
            p.lastOccupied = now;
        }
        if (p.phase == 0 && rt.build == null) {                                                    // a restart in the middle of forming: the build starts again (placing the same blocks again is harmless)
            rt.build = new PocketBuild(rt);
            rt.build.entry(mega);
        }
        if (p.phase >= 1 && !p.relicTaken && relicGone(mega, rt)) relicTaken(mega, realm, p, rt, in, now);
        if (p.phase == 2 && rt.collapse == null) rt.collapse = new PocketCollapse(rt, p.collapseStart < 0 ? now : p.collapseStart);
        if (!p.bossDead && p.phase >= 1 && PocketBoss.sweep(mega, rt, p, in, now)) bossLost(mega, realm, p, rt, in);
        if (p.phase == 2 && rt.collapse != null) {
            long left = rt.collapse.totalTicks() - rt.collapse.elapsed(now);
            int w = RiftRules.warnSeconds(left, rt.lastQuiet == 0 ? left + 20 : rt.lastQuiet);
            rt.lastQuiet = left;
            if (w > 0) for (ServerPlayer m : in) m.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.pocket.collapse.clock", "The tomb falls in %s seconds.", w).withStyle(w <= 10 ? ChatFormatting.RED : ChatFormatting.GOLD), true);
            if (rt.collapse.finished(now)) return true;
        } else if (warn > 0) {
            for (ServerPlayer m : in) m.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.rift.pocket.collapse", "The realm is collapsing: %s seconds.", warn).withStyle(warn <= 10 ? ChatFormatting.RED : ChatFormatting.LIGHT_PURPLE), true);
        }
        if (now >= p.expiresAt) return true;
        long since = p.lastOccupied < 0 ? now - p.createdAt : now - p.lastOccupied;
        return PocketRules.emptyCollapse(in.isEmpty(), since, now - p.createdAt, p.phase == 2);
    }

    /** Has the primary relic been lifted off its pedestal? */
    private static boolean relicGone(ServerLevel mega, PocketRuntime rt) {
        if (rt.primary == null || !mega.isLoaded(rt.primary)) return false;
        BlockEntity be = mega.getBlockEntity(rt.primary);
        return be instanceof RelicPedestalBlockEntity ped && ped.isTaken();
    }

    /** The primary artifact was taken: the tomb begins to fall. */
    public static void relicTaken(ServerLevel mega, PocketRealm realm, PocketRealm.Pocket p, PocketRuntime rt, List<ServerPlayer> in, long now) {
        p.relicTaken = true;
        for (ServerPlayer m : in) NusmpEvents.fire(m, "mega.tomb_relic", p.pocketId);
        startCollapse(mega, realm, p, rt, now);
    }

    /** Starts the timeline of the collapse (the relic taken, or an admin). False when it was already under way. */
    public static boolean startCollapse(ServerLevel mega, PocketRealm realm, PocketRealm.Pocket p, PocketRuntime rt, long now) {
        if (p.phase != 1 && p.phase != 0) return false;
        p.phase = 2;
        p.collapseStart = now;
        rt.collapse = new PocketCollapse(rt, now);
        rt.lastQuiet = 0;
        realm.setDirty();
        if (rt.exit != null) {                                                                      // the way out outlives the tomb
            BlockEntity be = mega.getBlockEntity(rt.exit);
            if (be instanceof DimensionalRiftBlockEntity er) er.extendLife(rt.collapse.totalTicks() + 400);
        }
        return true;
    }

    /** The boss could not be kept in its lair: the way is opened so nobody is locked out. */
    private static void bossLost(ServerLevel mega, PocketRealm realm, PocketRealm.Pocket p, PocketRuntime rt, List<ServerPlayer> in) {
        p.bossDead = true;
        realm.setDirty();
        for (ServerPlayer m : in) m.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.pocket.boss.lost", "The master of the tomb is gone. Its treasury is yours to open.").withStyle(ChatFormatting.GOLD), true);
    }

    // ================================================================ collapse and clean-up
    /** Throws everybody out and starts the sweep of the cell. The record stays until the sweep is done. */
    public static void collapse(ServerLevel mega, PocketRealm realm, PocketRealm.Pocket p) {
        if (p.phase >= 3) return;
        for (ServerPlayer m : PocketRealm.playersIn(mega, p.slot)) {
            m.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.rift.pocket.gone", "The realm folds shut behind you.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            PocketGravity.clear(m);
            PocketRealm.leave(mega, m);
        }
        p.phase = 3;
        realm.setDirty();
        PocketRuntime rt = PocketRuntime.of(p);
        PocketClear job = new PocketClear(p.slot);
        if (rt != null) rt.clear = job;
        else ORPHANS.put(p.slot, job);
    }

    private static PocketClear clearJob(PocketRealm.Pocket p, PocketRuntime rt) {
        if (rt != null) {
            if (rt.clear == null) rt.clear = new PocketClear(p.slot);
            return rt.clear;
        }
        return ORPHANS.computeIfAbsent(p.slot, PocketClear::new);
    }

    private static void finishClear(ServerLevel mega, PocketRealm realm, PocketRealm.Pocket p, PocketRuntime rt) {
        for (ServerPlayer m : PocketRealm.playersIn(mega, p.slot)) {                                // anyone still there (a login in a cleared cell) is sent home
            PocketGravity.clear(m);
            PocketRealm.leave(mega, m);
        }
        if (!clearJob(p, rt).done()) return;
        PocketRuntime.forget(p.slot);
        ORPHANS.remove(p.slot);
        realm.pockets().remove(p.slot);
        realm.setDirty();
        LOG.info("[nusmp] The tomb {} in slot {} is gone and its cell is free.", p.pocketId, p.slot);
    }

    /** Server stop: the runtimes and the orphan jobs are not kept. */
    public static void stopped() {
        PocketRuntime.forgetAll();
        ORPHANS.clear();
    }

    // ================================================================ every tick
    /** The every-tick work of all tombs. */
    public static void fast(MinecraftServer server) {
        ServerLevel mega = MegaDungeon.level(server);
        if (mega == null) return;
        PocketRealm realm = PocketRealm.get(server);
        long now = mega.getGameTime();
        boolean any = false;
        for (PocketRealm.Pocket p : realm.pockets().values()) {
            if (!p.exclusive()) continue;
            any = true;
            try {
                jobs(mega, realm, p, now);
            } catch (RuntimeException ex) {
                LOG.warn("[nusmp] A job of the tomb {} in slot {} failed (further detail at debug): {}", p.pocketId, p.slot, ex.toString());
                LOG.debug("tomb job", ex);
            }
        }
        int t = server.getTickCount();
        if (t % 5 != 0 || mega.players().isEmpty()) return;
        for (ServerPlayer pl : new ArrayList<>(mega.players())) {
            try {
                player(mega, realm, pl, now, t, any);
            } catch (RuntimeException ex) {
                LOG.warn("[nusmp] The tomb tick failed for {} (further detail at debug): {}", pl.getGameProfile().getName(), ex.toString());
                LOG.debug("tomb player tick", ex);
            }
        }
    }

    private static void jobs(ServerLevel mega, PocketRealm realm, PocketRealm.Pocket p, long now) {
        PocketRuntime rt = PocketRuntime.of(p);
        if (p.phase >= 3) {
            clearJob(p, rt).step(mega, CLEAR_BUDGET);
            return;
        }
        if (rt == null) return;
        if (p.phase == 0 && rt.build != null && (rt.build.done() || rt.build.step(mega, RiftConfig.get(RiftConfig.BUILD_PER_TICK)))) {       // the tomb stands: its doorways are open
            p.phase = 1;
            realm.setDirty();
        }
        if (p.phase == 2 && rt.collapse != null) rt.collapse.step(mega, p, PocketRealm.playersIn(mega, p.slot), now);
    }

    private static void player(ServerLevel mega, PocketRealm realm, ServerPlayer pl, long now, int serverTick, boolean any) {
        int slot = RiftRules.slotAt(pl.getX(), pl.getZ());
        PocketRealm.Pocket pk = slot >= 0 ? realm.at(slot) : null;
        if (pk == null || !pk.exclusive() || pk.phase >= 3) {
            if (PocketGravity.marked(pl)) PocketGravity.clear(pl);
            return;
        }
        PocketRuntime rt = PocketRuntime.of(pk);
        if (rt == null || pl.isSpectator()) {
            if (PocketGravity.marked(pl)) PocketGravity.clear(pl);
            return;
        }
        if (!pl.isCreative() && (pl.getY() < PocketPlan.Y0 - 14 || pl.getY() > PocketCell.Y_MAX + 24)) {                 // fell out of the world, or flew out of it: back to the entry hall
            BlockPos at = landing(mega, rt);
            PocketRealm.move(pl, mega, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, pl.getYRot(), 0f);
            pl.resetFallDistance();
            return;
        }
        PocketGravity.apply(pl, rt.zoneAt(pl.getX(), pl.getY(), pl.getZ()), rt.def.gravity().base(), RiftConfig.get(RiftConfig.GRAVITY_ON));
        if (RiftConfig.get(RiftConfig.TRAPS_ON)) PocketTraps.tick(mega, rt, pl, now, serverTick % 20 == 0);
        if (pk.phase >= 1) wake(mega, realm, pk, rt, pl);
        if (serverTick % 10 == 0) PocketMobs.aura(mega, rt, pl.getX(), pl.getY(), pl.getZ());
        if (!pk.sealOpen && serverTick % 20 == 0) sealHint(mega, pk, rt, pl, now);
    }

    // ================================================================ rooms and the lair
    private static void wake(ServerLevel mega, PocketRealm realm, PocketRealm.Pocket pk, PocketRuntime rt, ServerPlayer pl) {
        int room = rt.roomAt(pl.getX(), pl.getZ());
        if (room < 0 || !PocketRuntime.wake(pk, room)) return;
        realm.setDirty();
        List<ServerPlayer> in = PocketRealm.playersIn(mega, pk.slot);
        PocketLayout.Room r = rt.layout.room(room);
        switch (r.kind()) {
            case BOSS -> {
                if (!pk.bossDead && PocketBoss.spawn(mega, rt, pk) != null) PocketBoss.intro(mega, rt, in);
            }
            case KEY -> {
                PocketMobs.keeper(mega, rt, pk);
                PocketMobs.fillRoom(mega, rt, pk, room, Math.max(1, in.size()));
            }
            case ENTRY, TREASURY, ALCOVE -> { }
            default -> PocketMobs.fillRoom(mega, rt, pk, room, Math.max(1, in.size()));
        }
    }

    // ================================================================ the seal
    /** A line of the seal's three answers for a delver who stands near it (once in twenty seconds). */
    private static void sealHint(ServerLevel mega, PocketRealm.Pocket pk, PocketRuntime rt, ServerPlayer pl, long now) {
        BlockPos c = sealCentre(rt);
        if (c == null || pl.distanceToSqr(c.getX() + 0.5, c.getY() + 1.0, c.getZ() + 0.5) > 36.0) return;
        Long last = rt.sealWarned.get(pl.getUUID());
        if (last != null && now - last < 400) return;
        rt.sealWarned.put(pl.getUUID(), now);
        pl.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.pocket.seal.hint", "A seal of condensed mana: it answers to %s magic, to the %s, or to force (%s of %s left).",
                Component.translatableWithFallback("pocket.nusmp." + rt.def.id() + ".seal", rt.def.seal().label()), Component.translatableWithFallback("pocket.nusmp." + rt.def.id() + ".sigil", rt.def.seal().sigil()), pk.sealHp, rt.def.seal().forceHp())
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }

    /** One block of the seal (its first), for the hint and the effects; null when the plan has none. */
    public static BlockPos sealCentre(PocketRuntime rt) {
        if (rt.sealBlocks.isEmpty()) return null;
        long sx = 0, sy = 0, sz = 0;
        for (long l : rt.sealBlocks) {
            BlockPos b = BlockPos.of(l);
            sx += b.getX();
            sy += b.getY();
            sz += b.getZ();
        }
        int n = rt.sealBlocks.size();
        return new BlockPos((int) (sx / n), (int) (sy / n), (int) (sz / n));
    }

    /** Which of the three answers does this delver give the seal? "sigil", "magic", or "" (none of them). */
    public static String answer(ServerPlayer by, PocketRealm.Pocket pk, PocketDef def) {
        if (PocketSigil.matches(by.getMainHandItem(), pk.pocketId, pk.seed)) return "sigil";
        String magic = "";
        try {
            magic = ElsdociaEvents.magicOf(by);
        } catch (RuntimeException | LinkageError ignored) {
            // a mage's magic is read against the grimoire; without one the seal is simply not unravelled
        }
        return PocketRules.unravels(magic, def.seal().magic()) ? "magic" : "";
    }

    /** Puts force into the seal. Returns true when it opened. {@code force} may be a fraction (the hold of a button). */
    public static boolean addForce(ServerLevel mega, PocketRealm realm, PocketRealm.Pocket pk, PocketRuntime rt, ServerPlayer by, double force) {
        if (pk.sealOpen || force <= 0.0) return false;
        double total = force + rt.sealCarry;
        int whole = (int) Math.floor(total);
        rt.sealCarry = total - whole;
        if (whole <= 0) return false;
        pk.sealHp = Math.max(0, pk.sealHp - whole);
        realm.setDirty();
        BlockPos c = sealCentre(rt);
        int stage = PocketRules.crackStage(pk.sealHp, rt.def.seal().forceHp());
        if (stage != rt.crackShown) {
            rt.crackShown = stage;
            int k = 0;
            for (long l : rt.sealBlocks) mega.destroyBlockProgress(CRACK_ID - k++, BlockPos.of(l), stage);
        }
        if (c != null) {
            mega.sendParticles(ParticleTypes.CRIT, c.getX() + 0.5, c.getY() + 0.5, c.getZ() + 0.5, 6, 0.8, 1.0, 0.8, 0.1);
            mega.playSound(null, c, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 1.0f, 0.6f);
        }
        if (by != null) by.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.pocket.seal.force", "The seal holds: %s%% left.",
                Math.max(0, pk.sealHp * 100 / Math.max(1, rt.def.seal().forceHp()))).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        if (pk.sealHp <= 0) return openSeal(mega, realm, pk, rt, by, "force");
        return false;
    }

    /** Opens the seal (by magic, by the Sigil or by force): its blocks go, the delvers are told, and the grimoire of a resonant mage may be written to. False when it was open already. */
    public static boolean openSeal(ServerLevel mega, PocketRealm realm, PocketRealm.Pocket pk, PocketRuntime rt, ServerPlayer by, String how) {
        if (pk.sealOpen) return false;
        pk.sealOpen = true;
        pk.sealHp = 0;
        realm.setDirty();
        int k = 0;
        for (long l : rt.sealBlocks) {
            BlockPos b = BlockPos.of(l);
            mega.destroyBlockProgress(CRACK_ID - k++, b, -1);
            mega.setBlock(b, Blocks.AIR.defaultBlockState(), 2 | 16 | 32);
        }
        BlockPos c = sealCentre(rt);
        if (c != null) {
            mega.sendParticles(ParticleTypes.END_ROD, c.getX() + 0.5, c.getY() + 0.5, c.getZ() + 0.5, 80, 1.5, 1.8, 1.5, 0.15);
            mega.playSound(null, c, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 2.0f, 0.5f);
            mega.playSound(null, c, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 2.0f, 0.4f);
        }
        Random rnd = new Random(rt.seed ^ 0x5EA1L);
        for (ServerPlayer m : PocketRealm.playersIn(mega, pk.slot)) {
            m.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.pocket.seal.open." + how, how.equals("force") ? "The seal gives way to sheer force. The treasury is open." : how.equals("sigil") ? "The Sigil is spent. The treasury is open." : "The seal unravels at the touch of your magic. The treasury is open.")
                    .withStyle(ChatFormatting.GOLD), false);
            if (rt.resonated.add(m.getUUID())) AncientResonance.inscribe(m, rt.def, rnd.nextDouble());
        }
        if (by != null) NusmpEvents.fire(by, "mega.tomb_seal", rt.def.id());
        return true;
    }

    // ================================================================ the master dies
    /** The mega-boss of a tomb died: the treasury's loot stage opens, the words, and the boss's own drops are rolled (the table checks the gate itself). */
    public static void bossDied(ServerLevel mega, PocketRealm realm, PocketRealm.Pocket pk, PocketRuntime rt, LabyrinthGuardianEntity boss) {
        if (pk.bossDead) return;
        pk.bossDead = true;
        realm.setDirty();
        List<ServerPlayer> in = PocketRealm.playersIn(mega, pk.slot);
        PocketBoss.death(mega, rt, in);
        for (ServerPlayer m : in) {
            m.displayClientMessage(Component.translatableWithFallback("message.nusmp.mega.pocket.boss.dead", "The master of the tomb falls. The seal still holds the treasury.").withStyle(ChatFormatting.GOLD), true);
            NusmpEvents.fire(m, "mega.tomb_boss", rt.def.id());
        }
        // the remains of the lair's court
        for (Mob e : mega.getEntitiesOfClass(Mob.class, PocketCell.bounds(pk.slot), x -> x.isAlive() && x.getTags().contains(PocketMobs.TAG_ADD))) e.discard();
        rollLoot(mega, rt, boss);
    }

    private static void rollLoot(ServerLevel mega, PocketRuntime rt, LabyrinthGuardianEntity boss) {
        try {
            ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath("nusmp", "pocket/" + rt.def.id() + "/boss"));
            LootTable table = mega.getServer().reloadableRegistries().getLootTable(key);
            LootParams params = new LootParams.Builder(mega).withParameter(LootContextParams.ORIGIN, boss.position()).withParameter(LootContextParams.THIS_ENTITY, boss).create(LootContextParamSets.GIFT);
            for (ItemStack s : table.getRandomItems(params)) {
                if (s == null || s.isEmpty()) continue;
                ItemEntity ie = new ItemEntity(mega, boss.getX(), boss.getY() + 0.8, boss.getZ(), s);
                ie.setDeltaMovement((mega.random.nextDouble() - 0.5) * 0.2, 0.3, (mega.random.nextDouble() - 0.5) * 0.2);
                mega.addFreshEntity(ie);
            }
        } catch (RuntimeException | LinkageError ex) {
            LOG.warn("[nusmp] The loot of the master of {} could not be rolled: {}", rt.def.id(), ex.toString());
        }
    }

    // ================================================================ queries for the commands and tests
    /** The runtime and record of the tomb a position stands in (both null when it stands in none). */
    public static PocketRealm.Pocket pocketAt(MinecraftServer server, double x, double z) {
        int slot = RiftRules.slotAt(x, z);
        if (slot < 0) return null;
        PocketRealm.Pocket p = PocketRealm.get(server).at(slot);
        return p != null && p.exclusive() ? p : null;
    }

    /** The vector the delver's attack damage is read at (main hand included): what a swing at the seal is worth. */
    public static double attackOf(ServerPlayer p) { return p.getAttributeValue(Attributes.ATTACK_DAMAGE); }

    /** Where the primary relic lies, for the admin list. */
    public static Vec3 primaryAt(PocketRuntime rt) { return rt.primary == null ? null : Vec3.atCenterOf(rt.primary); }
}
