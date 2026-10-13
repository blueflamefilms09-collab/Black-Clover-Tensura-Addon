package com.newuniverse.nusmp.dungeon.nexus.vault;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.dungeon.DungeonTier;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import com.newuniverse.nusmp.dungeon.nexus.NexusState;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;
import com.newuniverse.nusmp.dungeon.nexus.NexusZones;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Going through a Descent Gate (docs/living_dungeon.md, "Navigation"): down to the next floor of the Dungeon at the same x and z, or back up
 * by the gate at the other end. The floors are the Upper Catacombs (y 24 and up), the Deep Waterways (24 down to -40, flooded), and the
 * Variable Core (below -40, in the core cells). Where the way leads:
 * <ul>
 *   <li>Upper to Deep: the flooded floor has no dry ground, so the gate cuts a small sealed chamber into it ({@link VaultPlan#chamber}),
 *       with a gate that leads back up and one wall of Illusion Stone to swim out through. It is made once; the gate always leads to it.</li>
 *   <li>Deep to Core: only above a core cell, and only once that Nexus stands ({@link NexusZones#stateOf}); the player is set down where
 *       the Nexus's plan says arrivals go, beside a gate that leads back up.</li>
 *   <li>Up: to the gate it came from, or to dry ground above if that is gone.</li>
 * </ul>
 * A player who has just travelled (by a Maw, a Root Gate or this) is not sent again for four seconds, so the gate they arrive beside cannot
 * send them straight back. A way that cannot be taken says why and costs nothing.
 */
final class GateTravel {
    private GateTravel() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static final String COOL = "nusmp_vault_gate";
    private static final int COOLDOWN = 80, FAILED = 40;
    /** Where a sealed chamber's floor may be cut, relative to the Deep Waterways (clear of the lava below y -54 and of the Core's roof). */
    private static final int CHAMBER_MIN = -34, CHAMBER_MAX = 14;

    static void use(ServerPlayer p, ServerLevel level, BlockPos gate, boolean rising) {
        if (!LivingDungeon.in(level) || p.isSpectator() || !p.isAlive()) return;
        long now = level.getGameTime();
        if (LivingDungeon.justTravelled(p) || now < p.getPersistentData().getLong(COOL)) return;
        BlockPos stand;
        try {
            stand = rising ? ascend(level, gate, p) : descend(level, gate, p);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] A Descent Gate at {} failed", gate, failure);
            say(p, "The gate flickers, and goes dark for a moment.");
            stand = null;
        }
        if (stand == null) {
            p.getPersistentData().putLong(COOL, now + FAILED);
            return;
        }
        p.getPersistentData().putLong(COOL, now + COOLDOWN);
        level.sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1.0, p.getZ(), 50, 0.4, 0.9, 0.4, 0.7);
        level.playSound(null, p.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.5f, rising ? 1.2f : 0.7f);
        p.teleportTo(level, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, p.getYRot(), 0f);
        p.resetFallDistance();
        level.sendParticles(rising ? ParticleTypes.END_ROD : ParticleTypes.REVERSE_PORTAL, stand.getX() + 0.5, stand.getY() + 1.0, stand.getZ() + 0.5, 40, 0.4, 0.9, 0.4, 0.3);
        level.playSound(null, stand, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.4f, rising ? 1.4f : 0.9f);
    }

    private static void say(ServerPlayer p, String text) { p.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.GRAY), true); }

    /** 0 the Upper Catacombs, 1 the Deep Waterways, 2 the Variable Core. */
    static int floorOf(int y) { return y >= DungeonTier.UPPER_Y ? 0 : y >= DungeonTier.CITY_Y ? 1 : 2; }

    // ---------------------------------------------------------------- down
    private static BlockPos descend(ServerLevel level, BlockPos gate, ServerPlayer p) {
        int floor = floorOf(gate.getY());
        if (floor >= 2) {
            say(p, "Nothing lies below this gate.");
            return null;
        }
        GateIndex idx = GateIndex.get(level.getServer());
        BlockPos linked = idx.arrivalOf(gate);
        if (linked != null) {
            level.getChunk(linked.getX() >> 4, linked.getZ() >> 4);
            BlockState there = level.getBlockState(linked);
            if (there.is(NexusBlocks.DESCENT_GATE.get()) && there.getValue(DescentGateBlock.RISING)) {
                BlockPos beside = standBeside(level, linked);
                if (beside != null) return beside;
            }
        }
        return floor == 0 ? chamberBelow(level, idx, gate, p) : coreBelow(level, idx, gate, p);
    }

    /** The sealed chamber under an upper gate: cut once, then reused. */
    private static BlockPos chamberBelow(ServerLevel level, GateIndex idx, BlockPos gate, ServerPlayer p) {
        int x = gate.getX(), z = gate.getZ(), minY = CHAMBER_MIN;
        int cx = Math.floorDiv(x, NexusZone.CELL), cz = Math.floorDiv(z, NexusZone.CELL);
        if (DungeonTier.isCityCell(cx, cz)) minY = Math.max(minY, coreTop(level, cx, cz) + 6);        // keep clear of the Core's roof
        if (minY > CHAMBER_MAX) {
            say(p, "The stone below this gate is older than the gate.");
            return null;
        }
        VaultSave save = VaultSave.get(level.getServer());
        VaultPlan.Result r = VaultPlan.chamber(x, z, minY, CHAMBER_MAX, (int) gate.asLong(), (xx, yy, zz) -> !save.insideAnySite(xx, yy, zz, 2), new WorldTerrain(level, true));
        if (r.status() != VaultPlan.Status.FOUND) {
            say(p, "The way below is choked with old stone.");
            return null;
        }
        VaultSite site = r.site();
        for (ServerPlayer other : level.players()) {
            if (other != p && !other.isSpectator() && site.contains(other.getBlockX(), other.getBlockY(), other.getBlockZ(), 3)) {
                say(p, "The way below is crowded. Wait a moment.");
                return null;
            }
        }
        VaultLayout layout = VaultPlan.chamberLayout(site);
        VaultSink sink = new VaultSink(level);
        DungeonGuard.internal(() -> new VaultCursor(layout, 0, 0).run(sink, Long.MAX_VALUE));
        VaultLayout.Spot g = layout.first(VaultLayout.Role.GATE), st = layout.first(VaultLayout.Role.STAND);
        BlockPos arrival = new BlockPos(g.x(), g.y(), g.z());
        GateIndex.noted(level, arrival, true);
        idx.link(gate, arrival);
        p.sendSystemMessage(Component.literal("A dry chamber in the flooded dark. The gate beside you leads back up. One wall of the chamber is only an illusion: "
                + "swim through it, and a Ki-Resonance Lantern will burn blue before it.").withStyle(ChatFormatting.DARK_AQUA));
        return new BlockPos(st.x(), st.y(), st.z());
    }

    /** The roof of the Nexus in a core cell, as its plan says (a default if the plan cannot be read). */
    private static int coreTop(ServerLevel level, int cx, int cz) {
        try {
            return NexusZones.planOf(level.getServer(), cx, cz).topY;
        } catch (RuntimeException | LinkageError unreadable) {
            return -26;
        }
    }

    /** Where arrivals are set down in the Variable Core under a deep gate, with a gate that leads back up beside it. */
    private static BlockPos coreBelow(ServerLevel level, GateIndex idx, BlockPos gate, ServerPlayer p) {
        int cx = Math.floorDiv(gate.getX(), NexusZone.CELL), cz = Math.floorDiv(gate.getZ(), NexusZone.CELL);
        if (!DungeonTier.isCityCell(cx, cz)) {
            say(p, "The stone below this gate is unbroken. No Core lies beneath it.");
            return null;
        }
        NexusState state = NexusZones.stateOf(level.getServer(), cx, cz);
        if (state == NexusState.COLLAPSED) {
            say(p, "The Core below has collapsed. There is nothing left to descend to.");
            return null;
        }
        if (state != NexusState.DONE) {
            say(p, "The Core below is still taking shape. Wait by the gate.");
            return null;
        }
        ZonePlan.Spot spot = NexusZones.planOf(level.getServer(), cx, cz).rescuePoint();
        BlockPos stand = new BlockPos(spot.x(), spot.y(), spot.z());
        level.getChunk(stand.getX() >> 4, stand.getZ() >> 4);
        if (!standable(level, stand)) {                                             // the plan's spot is not dry standing room (water, rubble): the nearest place that is
            BlockPos beside = standBeside(level, stand);
            if (beside != null) stand = beside;
        }
        BlockPos arrival = risingGateNear(level, stand);
        if (arrival != null) idx.link(gate, arrival);
        p.sendSystemMessage(Component.literal("The Core takes you in. " + (arrival != null ? "A gate nearby leads back up." : "No gate would form here: find another way out.")).withStyle(ChatFormatting.DARK_AQUA));
        return stand;
    }

    /** Sets a gate that leads up on free floor 2 or 3 blocks from `stand`; null if there is no room. */
    private static BlockPos risingGateNear(ServerLevel level, BlockPos stand) {
        for (int r = 2; r <= 3; r++) {
            for (Direction d : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
                BlockPos g = stand.relative(d, r);
                if (!standable(level, g)) continue;
                BlockState gate = NexusBlocks.DESCENT_GATE.get().defaultBlockState().setValue(DescentGateBlock.RISING, true);
                DungeonGuard.internal(() -> level.setBlock(g, gate, 3));
                return g;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- up
    private static BlockPos ascend(ServerLevel level, BlockPos gate, ServerPlayer p) {
        GateIndex idx = GateIndex.get(level.getServer());
        BlockPos origin = idx.originOf(gate);
        if (origin != null) {
            level.getChunk(origin.getX() >> 4, origin.getZ() >> 4);
            BlockState there = level.getBlockState(origin);
            if (there.is(NexusBlocks.DESCENT_GATE.get()) && !there.getValue(DescentGateBlock.RISING)) {
                BlockPos beside = standBeside(level, origin);
                if (beside != null) return beside;
            }
        }
        int floor = floorOf(gate.getY());
        if (floor == 0) {
            say(p, "Nothing lies above this gate.");
            return null;
        }
        int minY = floor == 2 ? DungeonTier.CITY_Y : DungeonTier.UPPER_Y, maxY = floor == 2 ? DungeonTier.UPPER_Y - 1 : level.getMaxBuildHeight() - 8;
        BlockPos up = findStand(level, gate.getX(), gate.getZ(), minY, maxY);
        if (up == null) say(p, "The way up has closed behind you.");
        return up;
    }

    // ---------------------------------------------------------------- finding ground
    /** Dry standing room: two air cells over a sturdy floor, no fluid in or under them. */
    static boolean standable(ServerLevel level, BlockPos p) {
        BlockPos below = p.below();
        return level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir() && level.getFluidState(p).isEmpty() && level.getFluidState(below).isEmpty()
                && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    /** A place to stand within two blocks of a gate, on the same level. */
    static BlockPos standBeside(ServerLevel level, BlockPos gate) {
        for (int r = 1; r <= 2; r++) {
            for (Direction d : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
                BlockPos p = gate.relative(d, r);
                if (standable(level, p)) return p;
            }
        }
        return null;
    }

    /** The highest dry standing place in the column (or the ones round it) between minY and maxY, outside every hidden room; null if there is none. */
    private static BlockPos findStand(ServerLevel level, int x, int z, int minY, int maxY) {
        VaultSave save = VaultSave.get(level.getServer());
        for (int ring = 0; ring <= 4; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    int cx = x + dx * 3, cz = z + dz * 3;
                    level.getChunk(cx >> 4, cz >> 4);
                    for (int y = maxY; y >= minY; y--) {
                        BlockPos p = new BlockPos(cx, y, cz);
                        if (standable(level, p) && !save.insideAnyRoom(cx, y, cz, 1)) return p;
                    }
                }
            }
        }
        return null;
    }
}
