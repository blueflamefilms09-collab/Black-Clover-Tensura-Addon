package com.newuniverse.nusmp.dungeon.nexus.vault;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.dungeon.DungeonConfig;
import com.newuniverse.nusmp.dungeon.DungeonEntities;
import com.newuniverse.nusmp.dungeon.DungeonFood;
import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.dungeon.DungeonTier;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;
import com.newuniverse.nusmp.dungeon.nexus.NexusZones;
import com.newuniverse.nusmp.dungeon.nexus.relic.Relic;
import com.newuniverse.nusmp.dungeon.nexus.relic.Relics;
import com.newuniverse.nusmp.dungeon.nexus.vault.VaultLayout.Role;
import com.newuniverse.nusmp.dungeon.nexus.vault.VaultLayout.Spot;
import com.newuniverse.nusmp.dungeon.wild.Dweller;
import com.newuniverse.nusmp.dungeon.wild.Roster;
import com.newuniverse.nusmp.dungeon.wild.WildSpawn;
import com.newuniverse.nusmp.dungeon.wild.Wildlife;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Builds and runs the Dungeon's hidden rooms (docs/living_dungeon.md, "Hidden Mana Vaults"): Puzzle Vaults, Nutrient Gardens, Guardian
 * Vaults and the Descent Shrines. Everything is lazy and bounded. Every 20 ticks it looks at the cells of the three grids round each player
 * ({@link VaultPlan}): a cell that rolled a room tries one round of candidate sites per sweep while a player is within 80 blocks (112 for the
 * shrines) and the chunks are loaded (an unloaded world is never guessed at; the attempt waits). A site, once found, is saved
 * ({@link VaultSave}) and built when a player comes within 48 blocks (80 for a shrine), a few hundred blocks a tick, inside the Dungeon's stone guard, and populated (a relic pedestal or a
 * cache, a Vault Guardian). It also lays the Warp Traps of the chunks round the players ({@link Distortion}), runs the plate puzzles, keeps
 * the guardians at their post, and keeps the gate index true. It writes only into the Dungeon's own level, only in loaded chunks.
 */
public final class VaultDirector {
    private VaultDirector() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** Units of work a tick (a write is 4, a block that is already right is 1): a room is a few hundred blocks, so one or two ticks. */
    private static final long BUDGET = 6000L;
    private static final int SWEEP = 20, PUZZLE_EVERY = 4, GUARD_EVERY = 20, REPAIR_EVERY = 400, TRAP_CHUNKS_PER_SWEEP = 2, TRAP_RADIUS = 2;
    /** Searches a sweep may start: one that settles (found, or none here) uses the sweep up, one that must wait for chunks does not, up to this many. */
    private static final int SEARCH_TRIES = 3;
    /** How near a player must be for a cell to be searched, and for a found room to be built: shrines reach further, so the lantern has gates to find. */
    private static final double VAULT_SCAN = 80.0, SHRINE_SCAN = 112.0, VAULT_BUILD = 48.0, SHRINE_BUILD = 80.0;
    /** Ticks a flashed plate stays lit, and between two flashes of the hint. */
    private static final int FLASH = 9, FLASH_STEP = 14, HINT_GAP = 100, STALLED = 600;
    private static final String GUARD_TAG = "nusmp_vault_guardian";
    private static final ResourceLocation GUARD_HEALTH = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "vault_guardian_health");
    private static final ResourceLocation GUARD_DAMAGE = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "vault_guardian_damage");

    private static Job job;
    private static ServerLevel seen;
    private static long pausedFrom, pausedUntil;
    private static final Map<Long, Puzzle> PUZZLES = new HashMap<>();
    private static final Map<String, Long> LAST_WARN = new HashMap<>();

    /** The room being built right now. */
    private static final class Job {
        final VaultSave.Entry entry;
        final VaultLayout layout;
        final VaultCursor cursor;
        final VaultSink sink;
        int idle;

        Job(ServerLevel level, VaultSave.Entry entry) {
            this.entry = entry;
            this.layout = VaultPlan.layout(entry.site);
            this.cursor = entry.state == VaultSave.BUILDING && entry.op > 0 ? VaultCursor.resume(layout, entry.op) : new VaultCursor(layout, 0, 0);
            this.sink = new VaultSink(level);
        }
    }

    // ---------------------------------------------------------------- the server tick
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!DungeonConfig.get(DungeonConfig.ENABLED)) return;
        MinecraftServer server = event.getServer();
        ServerLevel level = server.getLevel(LivingDungeon.DIMENSION);
        if (level == null) {
            forgetAll();
            seen = null;
            return;
        }
        long tick = server.getTickCount();
        if (level != seen) {                                                // a new world: forget what was left over from another
            forgetAll();
            seen = level;
            pausedFrom = 0;
            pausedUntil = 0;
        }
        if (tick >= pausedFrom && tick < pausedUntil) return;
        try {
            if (job != null) {
                if (!work(server, level, job)) job = null;
            } else if (tick % SWEEP == 13) {
                sweep(server, level);
            }
        } catch (RuntimeException | LinkageError failure) {
            LOG.error("[nusmp] The vault builder failed; the job is dropped and tried again in a minute", failure);
            job = null;
            pausedFrom = tick;
            pausedUntil = tick + 1200;
        }
        if (tick % PUZZLE_EVERY == 1) guarded("the plate puzzles", () -> puzzles(server, level), tick);
        if (tick % GUARD_EVERY == 7) guarded("the vault guardians", () -> guardians(server, level), tick);
        if (tick % REPAIR_EVERY == 211) guarded("the gate index", () -> repair(server, level), tick);
    }

    private static void forgetAll() {
        job = null;
        PUZZLES.clear();
        Distortion.reset();
    }

    private static void guarded(String what, Runnable r, long tick) {
        try {
            r.run();
        } catch (RuntimeException | LinkageError failure) {
            Long last = LAST_WARN.get(what);
            if (last != null && tick >= last && tick - last < 1200) return;
            LAST_WARN.put(what, tick);
            LOG.warn("[nusmp] Trouble with {} (logged once a minute)", what, failure);
        }
    }

    // ---------------------------------------------------------------- finding sites and starting jobs
    private static VaultKind kindOf(int grid, int cx, int cz) {
        return switch (grid) {
            case VaultPlan.GRID_VAULT -> VaultPlan.vaultAt(cx, cz);
            case VaultPlan.GRID_GATE -> VaultPlan.gateAt(cx, cz) ? VaultKind.GATE : null;
            default -> DungeonTier.isCityCell(cx, cz) ? VaultKind.GATE : null;
        };
    }

    private static void sweep(MinecraftServer server, ServerLevel level) {
        if (level.players().isEmpty()) return;
        VaultSave save = VaultSave.get(server);
        int[] searched = {0};                                                                 // searches started this sweep (SEARCH_TRIES once one has settled)
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            int px = p.getBlockX(), pz = p.getBlockZ();
            for (int grid = 0; grid < VaultPlan.CELL.length; grid++) {
                int size = VaultPlan.CELL[grid], rings = (int) Math.ceil(scanRange(grid) / size);
                int pcx = Math.floorDiv(px, size), pcz = Math.floorDiv(pz, size);
                for (int dx = -rings; dx <= rings; dx++) {
                    for (int dz = -rings; dz <= rings; dz++) {
                        int cx = pcx + dx, cz = pcz + dz;
                        if (gap(px, cx * size, cx * size + size - 1) > scanRange(grid) || gap(pz, cz * size, cz * size + size - 1) > scanRange(grid)) continue;
                        VaultKind kind = kindOf(grid, cx, cz);
                        if (kind == null) continue;
                        VaultSave.Entry e = save.lookup(grid, cx, cz);
                        if (e == null) e = save.entry(grid, cx, cz, kind);
                        if (visit(server, level, save, e, p, searched)) return;
                    }
                }
            }
        }
        if (!Distortion.hazards()) return;                                                    // traps are a hazard: none are laid while the hazards are off
        int chunks = 0;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            ChunkPos pc = p.chunkPosition();
            for (int dx = -TRAP_RADIUS; dx <= TRAP_RADIUS; dx++) {
                for (int dz = -TRAP_RADIUS; dz <= TRAP_RADIUS; dz++) {
                    ChunkPos cp = new ChunkPos(pc.x + dx, pc.z + dz);
                    if (save.trapsLaid(cp.toLong()) || level.getChunkSource().getChunkNow(cp.x, cp.z) == null) continue;
                    Distortion.layTraps(level, save, cp);
                    if (++chunks >= TRAP_CHUNKS_PER_SWEEP) return;
                }
            }
        }
    }

    private static double scanRange(int grid) { return grid == VaultPlan.GRID_VAULT ? VAULT_SCAN : SHRINE_SCAN; }

    private static double buildRange(VaultKind kind) { return kind == VaultKind.GATE ? SHRINE_BUILD : VAULT_BUILD; }

    private static int gap(int c, int lo, int hi) { return c < lo ? lo - c : Math.max(0, c - hi); }

    /** One look at a cell's record. True if a build job was started (nothing more is done this sweep). */
    private static boolean visit(MinecraftServer server, ServerLevel level, VaultSave save, VaultSave.Entry e, ServerPlayer near, int[] searched) {
        switch (e.state) {
            case VaultSave.SCANNING -> {
                if (searched[0] < SEARCH_TRIES) search(server, level, save, e, near, searched);
                return false;
            }
            case VaultSave.PLANNED, VaultSave.BUILDING -> {
                return begin(level, e);
            }
            case VaultSave.BUILT -> {
                if (e.site != null && loaded(level, e.site)) populate(level, save, e, VaultPlan.layout(e.site));
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    /** The heights a floor may be at, per grid. Above the Variable Core the roof of its Nexus is the lowest floor. */
    private static int[] range(MinecraftServer server, VaultSave.Entry e) {
        return switch (e.grid) {
            case VaultPlan.GRID_VAULT -> new int[]{-44, 108};
            case VaultPlan.GRID_GATE -> new int[]{33, 108};
            default -> {
                int min = -26;
                try {
                    min = Math.max(min, NexusZones.planOf(server, e.cellX, e.cellZ).topY + 6);
                } catch (RuntimeException | LinkageError unreadable) {
                    // the default stands
                }
                yield new int[]{min, 18};
            }
        };
    }

    /** No vault stands in a core cell's column (the Variable Core owns it), and none in the Core's own tier. */
    private static VaultPlan.Rules rules(int grid) {
        return (x, y, z) -> DungeonTier.at(x, y, z) != DungeonTier.GOLDEN_CITY
                && (grid == VaultPlan.GRID_CORE || !DungeonTier.isCityCell(Math.floorDiv(x, NexusZone.CELL), Math.floorDiv(z, NexusZone.CELL)));
    }

    private static void search(MinecraftServer server, ServerLevel level, VaultSave save, VaultSave.Entry e, ServerPlayer near, int[] searched) {
        for (VaultPlan.Candidate c : VaultPlan.round(e.grid, e.kind, e.cellX, e.cellZ, e.attempt)) {
            double dx = near.getX() - c.centreX(), dz = near.getZ() - c.centreZ();
            if (dx * dx + dz * dz > scanRange(e.grid) * scanRange(e.grid)) return;                // wait until every column of the round is close enough to be loaded
        }
        searched[0]++;
        int[] range = range(server, e);
        VaultPlan.Result r = VaultPlan.search(e.grid, e.kind, e.cellX, e.cellZ, e.attempt, range[0], range[1], rules(e.grid), new WorldTerrain(level, false));
        if (r.status() != VaultPlan.Status.UNKNOWN) searched[0] = SEARCH_TRIES;               // one settled round a sweep; a cell still waiting for its chunks must not starve the nearer ones
        switch (r.status()) {
            case UNKNOWN -> { }
            case NONE -> miss(save, e);
            case FOUND -> {
                if (save.overlaps(r.site())) {
                    miss(save, e);
                } else {
                    e.site = r.site();
                    e.state = VaultSave.PLANNED;
                    e.op = 0;
                    e.cell = 0;
                    save.dirty();
                }
            }
        }
    }

    private static void miss(VaultSave save, VaultSave.Entry e) {
        e.attempt++;
        if (e.attempt >= VaultPlan.ATTEMPTS) e.state = VaultSave.NONE;
        save.dirty();
    }

    private static boolean loaded(ServerLevel level, VaultSite s) {
        for (int[] c : s.chunks()) if (level.getChunkSource().getChunkNow(c[0], c[1]) == null) return false;
        return true;
    }

    private static boolean someoneWithin(ServerLevel level, VaultSite s, double range) {
        for (ServerPlayer p : level.players()) {
            double dx = p.getX() - s.centreX(), dz = p.getZ() - s.centreZ();
            if (dx * dx + dz * dz <= range * range) return true;
        }
        return false;
    }

    /** Is a player in the room, its shell or the doorway? Nothing is built round them. */
    private static boolean someoneIn(ServerLevel level, VaultSite s) {
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            if (s.contains(p.getBlockX(), p.getBlockY(), p.getBlockZ(), 2)) return true;
        }
        return false;
    }

    private static boolean begin(ServerLevel level, VaultSave.Entry e) {
        if (job != null || e.site == null) return false;
        VaultSite s = e.site;
        if (!someoneWithin(level, s, buildRange(e.kind)) || !loaded(level, s) || someoneIn(level, s)) return false;
        e.state = VaultSave.BUILDING;
        job = new Job(level, e);
        return true;
    }

    // ---------------------------------------------------------------- a job's tick
    /** One tick of work. False once the job is over (done, or given up because nobody is about). */
    private static boolean work(MinecraftServer server, ServerLevel level, Job j) {
        VaultSite s = j.entry.site;
        boolean ready = loaded(level, s);
        if (ready && someoneWithin(level, s, buildRange(j.entry.kind) + 48.0)) j.idle = 0;
        else if (++j.idle > 400) return false;                      // nobody about, or the rock went out of memory and stays so: put the job down (it is picked up again from its saved progress)
        if (!ready) return true;
        VaultSave save = VaultSave.get(server);
        DungeonGuard.internal(() -> j.cursor.run(j.sink, BUDGET));
        j.entry.op = j.cursor.op();
        j.entry.cell = j.cursor.cell();
        save.dirty();
        if (!j.cursor.done()) return true;
        j.entry.state = VaultSave.BUILT;
        save.dirty();
        rescue(level, j.layout);
        populate(level, save, j.entry, j.layout);
        return false;
    }

    /** Anyone caught inside the new stonework is set down outside the door. */
    private static void rescue(ServerLevel level, VaultLayout layout) {
        Spot door = layout.first(Role.DOOR);
        if (door == null) return;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || !layout.site.contains(p.getBlockX(), p.getBlockY(), p.getBlockZ(), 0)) continue;
            if (!level.noCollision(p, p.getBoundingBox())) p.teleportTo(level, door.x() + 0.5, door.y(), door.z() + 0.5, p.getYRot(), p.getXRot());
        }
    }

    // ---------------------------------------------------------------- what lives in a finished room
    private static Relic relicOf(VaultSite s) {
        Relic[] all = Relic.pool(com.newuniverse.nusmp.dungeon.sentient.SentientMode.active());
        return all[Math.floorMod(s.variant() >>> 4, all.length)];
    }

    /** Does a Puzzle Vault's cache hold food instead of a relic? */
    static boolean foodCache(VaultSite s) { return Math.floorMod(s.variant() >>> 20, 100) < 35; }

    private static Direction outward(VaultSite s) {
        return switch (s.facing()) {
            case VaultSite.EAST -> Direction.EAST;
            case VaultSite.SOUTH -> Direction.SOUTH;
            case VaultSite.WEST -> Direction.WEST;
            default -> Direction.NORTH;
        };
    }

    /** Places what a finished room holds; idempotent, and only the last step marks the room done. */
    private static void populate(ServerLevel level, VaultSave save, VaultSave.Entry e, VaultLayout layout) {
        VaultSite s = e.site;
        switch (e.kind) {
            case PUZZLE -> {
                Spot ped = layout.first(Role.PEDESTAL);
                BlockPos at = new BlockPos(ped.x(), ped.y(), ped.z());
                if (foodCache(s) || !placeRelic(level, at, relicOf(s), false)) placeFoodChest(level, at, s);
            }
            case MINIBOSS -> {
                Spot ped = layout.first(Role.PEDESTAL);
                BlockPos at = new BlockPos(ped.x(), ped.y(), ped.z());
                if (!placeRelic(level, at, relicOf(s), false)) placeFoodChest(level, at, s);
                spawnGuardian(level, layout);
            }
            case GATE -> {
                Spot g = layout.first(Role.GATE);
                GateIndex.noted(level, new BlockPos(g.x(), g.y(), g.z()), false);
            }
            default -> { }
        }
        e.state = VaultSave.DONE;
        save.dirty();
    }

    private static boolean placeRelic(ServerLevel level, BlockPos at, Relic relic, boolean central) {
        try {
            Relics.placePedestal(level, at, relic, central);
            return true;
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] A relic pedestal could not be placed at {}", at, failure);
            return false;
        }
    }

    /** A chest of the Dungeon's good food, at the back of the room facing out. */
    private static void placeFoodChest(ServerLevel level, BlockPos at, VaultSite s) {
        BlockState chest = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, outward(s));
        DungeonGuard.internal(() -> level.setBlock(at, chest, 3));
        BlockEntity be = level.getBlockEntity(at);
        if (!(be instanceof ChestBlockEntity box)) return;
        int v = Math.floorMod(s.variant() >>> 9, 4);
        List<ItemStack> loot = new ArrayList<>();
        loot.add(new ItemStack(NexusBlocks.NUTRIENT_NECTAR.get(), 3 + v));
        loot.add(new ItemStack(DungeonFood.PURIFIED_MEAT.get(), 4));
        loot.add(new ItemStack(DungeonFood.CANDIED_SLIME_JELLY.get(), 2));
        loot.add(new ItemStack(v < 2 ? DungeonFood.GRILLED_ARMOR_FLANK.get() : DungeonFood.ROAST_DIREWOLF_HAUNCH.get(), 2));
        loot.add(new ItemStack(DungeonFood.SEARED_MONSTER_MEAT.get(), 3));
        for (int i = 0; i < loot.size(); i++) box.setItem(i + 2, loot.get(i));
        box.setChanged();
    }

    // ---------------------------------------------------------------- the Vault Guardian
    private static void spawnGuardian(ServerLevel level, VaultLayout layout) {
        VaultSite s = layout.site;
        AABB room = new AABB(s.ix() - 1, s.iy() - 1, s.iz() - 1, s.maxX() + 2, s.maxY() + 2, s.maxZ() + 2);
        if (!level.getEntitiesOfClass(Mob.class, room, m -> m.getPersistentData().getBoolean(GUARD_TAG)).isEmpty()) return;      // already there
        Spot g = layout.first(Role.GUARDIAN);
        BlockPos post = new BlockPos(g.x(), g.y(), g.z());
        RandomSource r = level.getRandom();
        Roster.Entry pick = Wildlife.roster().pick(3, DungeonTier.at(g.x(), g.y(), g.z()).ordinal(), false, r.nextDouble());
        BlockPos[] tries = {post, post.north(2), post.south(2), post.east(2), post.west(2), post.north(3), post.east(3)};
        Mob m = null;
        if (pick != null) for (int i = 0; i < tries.length && m == null; i++) m = WildSpawn.spawnId(level, pick.id(), tries[i], r);
        for (int i = 0; i < tries.length && m == null; i++) m = WildSpawn.spawnType(level, DungeonEntities.BASILISK.get(), tries[i], r, MobSpawnType.EVENT, true);
        if (m == null) {
            LOG.warn("[nusmp] The Vault Guardian of the room at {} could not be spawned", post);
            return;
        }
        m.setCustomName(Component.literal("Vault Guardian").withStyle(ChatFormatting.DARK_AQUA));
        m.getPersistentData().putBoolean(GUARD_TAG, true);
        buff(m, Attributes.MAX_HEALTH, GUARD_HEALTH, 0.6);
        buff(m, Attributes.ATTACK_DAMAGE, GUARD_DAMAGE, 0.25);
        m.setHealth(m.getMaxHealth());
    }

    /** The guardian shows itself (its name and a glow) once someone is in its room, not before: from outside it must not give the room away through the wall. */
    private static void awaken(Mob m) {
        if (m.isCustomNameVisible()) return;
        m.setCustomNameVisible(true);
        m.addEffect(new MobEffectInstance(MobEffects.GLOWING, 1_728_000, 0, false, false));
    }

    private static void buff(Mob m, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance inst = m.getAttribute(attribute);
        if (inst != null && !inst.hasModifier(id)) inst.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    /** Keeps each guardian fed (it must neither starve nor fall asleep), pointed at whoever is in its room, and near its post. */
    private static void guardians(MinecraftServer server, ServerLevel level) {
        if (level.players().isEmpty()) return;
        VaultSave save = VaultSave.get(server);
        for (VaultSave.Entry e : save.all()) {
            if (e.kind != VaultKind.MINIBOSS || e.state != VaultSave.DONE || e.site == null) continue;
            VaultSite s = e.site;
            if (!someoneWithin(level, s, 96.0) || !loaded(level, s)) continue;
            AABB zone = new AABB(s.ix() - 16, s.iy() - 8, s.iz() - 16, s.maxX() + 17, s.maxY() + 10, s.maxZ() + 17);
            for (Mob m : level.getEntitiesOfClass(Mob.class, zone, x -> x.getPersistentData().getBoolean(GUARD_TAG) && x.isAlive())) {
                if (m.hasData(Dweller.DWELLER.get())) {
                    Dweller w = m.getData(Dweller.DWELLER);
                    if (w.appetite.hunger() < 50) w.appetite.set(60);
                    if (w.asleep) {
                        w.asleep = false;
                        m.setNoAi(false);
                    }
                }
                ServerPlayer prey = null;
                for (ServerPlayer p : level.players()) {
                    if (p.isSpectator() || p.isCreative() || !s.insideInterior(p.getBlockX(), p.getBlockY(), p.getBlockZ(), 3)) continue;
                    if (prey == null || p.distanceToSqr(m) < prey.distanceToSqr(m)) prey = p;
                }
                if (prey != null) {
                    awaken(m);
                    if (m.getTarget() == null) m.setTarget(prey);
                }
                double cx = s.centreX() + 0.5, cz = s.centreZ() + 0.5;
                double away = Math.hypot(m.getX() - cx, m.getZ() - cz);
                if (m.getTarget() == null && away > 12.0) m.getNavigation().moveTo(cx, s.iy(), cz, 1.0);
            }
        }
    }

    // ---------------------------------------------------------------- the plate puzzles
    /** A puzzle someone is working on. Not saved: a restart shows the hint again. */
    private static final class Puzzle {
        final VaultSave.Entry entry;
        final VaultLayout layout;
        final PlatePuzzle logic;
        final int[] shown = new int[PlatePuzzle.PLATES];
        long age, lastCorrect;
        int idle;

        Puzzle(VaultSave.Entry entry) {
            this.entry = entry;
            this.layout = VaultPlan.layout(entry.site);
            int v = entry.site.variant();
            this.logic = new PlatePuzzle(PlatePuzzle.sequenceFor(v * 31L + 7L, PlatePuzzle.lengthFor(v)));
            java.util.Arrays.fill(shown, -1);                                       // unknown: every plate is set once
        }
    }

    private static void puzzles(MinecraftServer server, ServerLevel level) {
        if (level.players().isEmpty() && PUZZLES.isEmpty()) return;
        VaultSave save = VaultSave.get(server);
        Map<Long, VaultSave.Entry> entries = new HashMap<>();
        Map<Long, List<ServerPlayer>> inside = new HashMap<>();
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            for (VaultSave.Entry e : save.sitesNear(p.getBlockX(), p.getBlockZ())) {
                if (e.kind != VaultKind.PUZZLE || e.state != VaultSave.DONE || e.solved) continue;
                if (!e.site.insideInterior(p.getBlockX(), p.getBlockY(), p.getBlockZ(), 0)) continue;
                long key = VaultSave.key(e.grid, e.cellX, e.cellZ);
                entries.put(key, e);
                inside.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
            }
        }
        List<Long> solved = new ArrayList<>();
        for (Map.Entry<Long, List<ServerPlayer>> me : inside.entrySet()) {
            Puzzle pz = PUZZLES.computeIfAbsent(me.getKey(), k -> new Puzzle(entries.get(k)));
            if (tick(level, save, pz, me.getValue())) solved.add(me.getKey());
        }
        for (long key : solved) PUZZLES.remove(key);
        Iterator<Map.Entry<Long, Puzzle>> it = PUZZLES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Puzzle> me = it.next();
            if (inside.containsKey(me.getKey())) continue;
            Puzzle pz = me.getValue();
            pz.idle += PUZZLE_EVERY;
            if (pz.idle < 100) continue;
            pz.logic.reset();
            if (loaded(level, pz.entry.site)) {                                    // never load a room's chunks just to dim its plates: a new visit sets every plate again
                for (int k = 0; k < PlatePuzzle.PLATES; k++) if (pz.shown[k] != 0) showPlate(level, pz, k, 0);
            }
            it.remove();
        }
    }

    private static int plateUnder(Puzzle pz, ServerPlayer p) {
        if (!p.onGround()) return -1;
        for (Spot sp : pz.layout.spots(Role.PLATE)) {
            if (p.getBlockX() == sp.x() && p.getBlockZ() == sp.z() && p.getBlockY() == sp.y() + 1) return sp.index();
        }
        return -1;
    }

    /** One step of a puzzle. True once it is solved. */
    private static boolean tick(ServerLevel level, VaultSave save, Puzzle pz, List<ServerPlayer> players) {
        pz.idle = 0;
        pz.age += PUZZLE_EVERY;
        ServerPlayer lead = players.get(0);
        int plate = plateUnder(pz, lead);
        PlatePuzzle.Result result = pz.logic.step(plate);
        switch (result) {
            case CORRECT -> {
                pz.lastCorrect = pz.age;
                level.playSound(null, lead.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 0.7f + 0.15f * pz.logic.progress());
            }
            case WRONG -> {
                pz.age = 0;                                                         // the hint starts over from its pause
                zap(level, lead);
            }
            case SOLVED -> {
                solve(level, save, pz, players);
                return true;
            }
            default -> { }
        }
        if (pz.logic.progress() > 0 && pz.age - pz.lastCorrect > STALLED) {        // someone stopped half way: the run is dropped and the hint comes back
            pz.logic.reset();
            pz.age = 0;
        }
        int lit = -1;
        if (pz.logic.progress() == 0) {
            long cycle = HINT_GAP + (long) pz.logic.length() * FLASH_STEP, t = pz.age % cycle - HINT_GAP;
            if (t >= 0 && t % FLASH_STEP < FLASH) lit = pz.logic.plateAt((int) (t / FLASH_STEP));
        }
        for (int k = 0; k < PlatePuzzle.PLATES; k++) {
            int want = pz.logic.done(k) ? 2 : k == lit ? 1 : 0;
            if (pz.shown[k] == want) continue;
            showPlate(level, pz, k, want);
            if (want == 1) level.playSound(null, lead.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.7f, 0.6f + 0.12f * k);
        }
        return false;
    }

    private static void showPlate(ServerLevel level, Puzzle pz, int k, int state) {
        pz.shown[k] = state;
        Spot sp = pz.layout.spots(Role.PLATE).get(k);
        VaultOp.Pal pal = state == 2 ? VaultOp.Pal.PLATE_DONE : state == 1 ? VaultOp.Pal.PLATE_LIT : VaultOp.Pal.PLATE_IDLE;
        BlockPos at = new BlockPos(sp.x(), sp.y(), sp.z());
        DungeonGuard.internal(() -> level.setBlock(at, VaultSink.state(pal, false), VaultSink.FLAGS));
    }

    /** A wrong plate: a spark and a light sting (a heart, never the last three), and the run starts over. */
    private static void zap(ServerLevel level, ServerPlayer p) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 0.3, p.getZ(), 20, 0.3, 0.3, 0.3, 0.25);
        level.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS, 0.25f, 1.9f);
        if (p.getHealth() > 6.0f) p.hurt(level.damageSources().magic(), 2.0f);
        p.displayClientMessage(Component.literal("The plates hiss. The pattern begins again.").withStyle(ChatFormatting.GRAY), true);
    }

    private static void solve(ServerLevel level, VaultSave save, Puzzle pz, List<ServerPlayer> players) {
        pz.entry.solved = true;
        save.dirty();
        DungeonGuard.internal(() -> {
            for (Spot b : pz.layout.spots(Role.BARS)) level.setBlock(new BlockPos(b.x(), b.y(), b.z()), Blocks.AIR.defaultBlockState(), 3);
            for (Spot sp : pz.layout.spots(Role.PLATE)) level.setBlock(new BlockPos(sp.x(), sp.y(), sp.z()), VaultSink.state(VaultOp.Pal.PLATE_DONE, false), VaultSink.FLAGS);
        });
        Spot ped = pz.layout.first(Role.PEDESTAL);
        level.playSound(null, new BlockPos(ped.x(), ped.y(), ped.z()), SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.2f);
        level.sendParticles(ParticleTypes.END_ROD, ped.x() + 0.5, ped.y() + 1.0, ped.z() + 0.5, 30, 0.3, 0.6, 0.3, 0.05);
        for (ServerPlayer p : players) p.displayClientMessage(Component.literal("Iron grinds aside in the wall behind the plates.").withStyle(ChatFormatting.AQUA), true);
    }

    // ---------------------------------------------------------------- keeping the gate index true
    private static void repair(MinecraftServer server, ServerLevel level) {
        GateIndex.repair(level, 48);
        if (level.players().isEmpty()) return;
        VaultSave save = VaultSave.get(server);
        for (ServerPlayer p : level.players()) {
            for (VaultSave.Entry e : save.sitesNear(p.getBlockX(), p.getBlockZ())) {
                if (e.kind != VaultKind.GATE || e.state != VaultSave.DONE || e.site == null || !loaded(level, e.site)) continue;
                Spot g = VaultPlan.layout(e.site).first(Role.GATE);
                BlockPos at = new BlockPos(g.x(), g.y(), g.z());
                BlockState there = level.getBlockState(at);
                if (there.is(NexusBlocks.DESCENT_GATE.get())) GateIndex.noted(level, at, there.getValue(DescentGateBlock.RISING));
            }
        }
    }

    // ---------------------------------------------------------------- for the admin command
    /** The outcome of an admin build: whether it worked, and what to tell the executor. */
    public record Built(boolean ok, String message) {}

    /**
     * Builds a room now, in one go: the cell under (x, z) is searched with the chunks loaded as needed, the room is built and populated at
     * once. {@code want} null takes the cell's own kind (a puzzle vault if the cell rolled none). A cell already built is left alone.
     */
    public static Built buildNow(ServerLevel level, int x, int z, VaultKind want) {
        if (!LivingDungeon.in(level)) return new Built(false, "The Dungeon is not loaded.");
        VaultSave save = VaultSave.get(level.getServer());
        int grid = want == VaultKind.GATE ? VaultPlan.GRID_GATE : VaultPlan.GRID_VAULT;
        int size = VaultPlan.CELL[grid], cx = Math.floorDiv(x, size), cz = Math.floorDiv(z, size);
        VaultKind kind = want;
        if (kind == null) {
            kind = VaultPlan.vaultAt(cx, cz);
            if (kind == null) kind = VaultKind.PUZZLE;
        }
        VaultSave.Entry e = save.lookup(grid, cx, cz);
        if (e != null && e.kind != kind) return new Built(false, "This cell of " + size + " blocks already belongs to a " + e.kind.label + ". Try another place.");
        if (e != null && e.state >= VaultSave.BUILT && e.site != null) return new Built(false, "The " + e.kind.label + " of this cell is built already: " + describe(e) + ".");
        if (e == null) e = save.entry(grid, cx, cz, kind);
        if (job != null && job.entry == e) job = null;
        VaultSite site = e.site;
        if (site == null) {
            int[] range = range(level.getServer(), e);
            for (int a = 0; a < VaultPlan.ATTEMPTS && site == null; a++) {
                VaultPlan.Result r = VaultPlan.search(grid, kind, cx, cz, a, range[0], range[1], rules(grid), new WorldTerrain(level, true));
                if (r.status() == VaultPlan.Status.FOUND && !save.overlaps(r.site())) site = r.site();
            }
        }
        if (site == null) {
            e.attempt = VaultPlan.ATTEMPTS;
            e.state = VaultSave.NONE;
            save.dirty();
            return new Built(false, "No rock beside a cave in this cell of " + size + " blocks (x " + cx * size + " to " + (cx * size + size - 1) + ", z " + cz * size + " to "
                    + (cz * size + size - 1) + ") fits a " + kind.label + ". Try another place.");
        }
        e.site = site;
        e.state = VaultSave.PLANNED;
        e.op = 0;
        e.cell = 0;
        for (int[] c : site.chunks()) level.getChunk(c[0], c[1]);
        VaultLayout layout = VaultPlan.layout(site);
        VaultSink sink = new VaultSink(level);
        DungeonGuard.internal(() -> new VaultCursor(layout, 0, 0).run(sink, Long.MAX_VALUE));
        e.state = VaultSave.BUILT;
        e.op = layout.ops.size();
        save.dirty();
        rescue(level, layout);
        populate(level, save, e, layout);
        return new Built(true, "Built a " + kind.label + ": " + describe(e) + ".");
    }

    /** "door (x, y, z), 7 x 5 x 9, facing north": where to find a room. */
    public static String describe(VaultSave.Entry e) {
        VaultSite s = e.site;
        if (s == null) return "no site yet";
        String[] facing = {"north", "east", "south", "west"};
        return "door at (" + s.doorX() + ", " + s.iy() + ", " + s.doorZ() + "), room " + s.width() + " x " + s.height() + " x " + s.depth() + " facing " + facing[s.facing()]
                + (s.hidden() ? ", behind an illusion wall" : ", an open doorway") + (e.kind == VaultKind.PUZZLE ? (e.solved ? ", solved" : ", unsolved") : "");
    }
}
