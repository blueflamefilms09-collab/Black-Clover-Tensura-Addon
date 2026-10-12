package com.newuniverse.nusmp.dungeon.nexus.vault;

import com.newuniverse.nusmp.dungeon.DungeonBlocks;
import com.newuniverse.nusmp.dungeon.DungeonConfig;
import com.newuniverse.nusmp.dungeon.DungeonTier;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import com.newuniverse.nusmp.dungeon.nexus.SpatialDistortion;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Dungeon's spatial distortions at work (docs/living_dungeon.md, "Spatial Warping"; the pockets themselves are the pure
 * {@link SpatialDistortion}). Three rules, none of which touches a player wearing an attuned Band of the Spatial Anchor:
 * <ul>
 *   <li><b>Warp Traps</b> are laid lazily in the pockets' floors of the three upper tiers, a few to a chunk ({@link TrapPlan}), never inside
 *       a vault, a shrine, near a gate or a Root Gate, or beside a player.</li>
 *   <li><b>Gravity pulses</b>: in the heart of a pocket (strength 0.6 and more) the space gives a warning, a spiral of particles for a second
 *       and a half, and then throws the player up or slams them down ({@link GravityPulse}). A lift is cushioned (slow falling), a slam
 *       does no damage, and nothing happens in creative, in spectator or when the pocket is left.</li>
 *   <li><b>Block rejection</b>: a block a player places in a pocket is refused, with a line saying so.</li>
 * </ul>
 */
public final class Distortion {
    private Distortion() {}

    /** How much distortion refuses a placed block. */
    private static final double REJECT_STRENGTH = 0.2;
    /** A gravity pulse looks this far up for room, in blocks. */
    private static final int HEADROOM = 6;
    private static final Map<UUID, GravityPulse> PULSES = new HashMap<>();

    /** The Dungeon's hazards switch (config dungeon.hazards) also turns the spatial distortions off: no pulses, no traps, no refused blocks. */
    static boolean hazards() { return DungeonConfig.get(DungeonConfig.HAZARDS); }

    /** Forgets every pulse in progress (a new world, or the Dungeon not loaded): none may fire from a warning of another world's clock. */
    static void reset() { PULSES.clear(); }

    // ---------------------------------------------------------------- gravity pulses
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 4 != 2) return;
        if (!LivingDungeon.in(p) || !p.isAlive() || p.isSpectator() || p.isCreative() || !hazards()) {
            if (!PULSES.isEmpty()) PULSES.remove(p.getUUID());
            return;
        }
        double strength = SpatialDistortion.strength(p.getBlockX(), p.getBlockY(), p.getBlockZ());
        GravityPulse pulse = PULSES.get(p.getUUID());
        if (pulse == null) {
            if (strength < GravityPulse.MIN_STRENGTH) return;
            pulse = new GravityPulse();
            PULSES.put(p.getUUID(), pulse);
        }
        ServerLevel level = p.serverLevel();
        boolean headroom = level.noCollision(p, p.getBoundingBox().expandTowards(0.0, HEADROOM, 0.0)) && !lavaAbove(level, p.blockPosition());
        GravityPulse.Act act = pulse.step(level.getGameTime(), strength, Anchor.worn(p), headroom, level.random.nextInt(100), level.random.nextInt(100));
        switch (act) {
            case TELEGRAPH -> {
                level.playSound(null, p.blockPosition(), SoundEvents.PORTAL_AMBIENT, SoundSource.PLAYERS, 0.6f, pulse.isLift() ? 1.6f : 0.5f);
                p.displayClientMessage(Component.literal(pulse.isLift() ? "The air thins beneath you..." : "The air grows heavy...").withStyle(ChatFormatting.DARK_PURPLE), true);
                spiral(level, p, pulse.isLift(), level.getGameTime());
            }
            case SPIRAL -> spiral(level, p, pulse.isLift(), level.getGameTime());
            case LIFT -> lift(level, p, strength);
            case CRUSH -> crush(level, p, strength);
            default -> { }
        }
        if (act == GravityPulse.Act.NONE && strength < GravityPulse.MIN_STRENGTH && !pulse.armed()) PULSES.remove(p.getUUID());
    }

    /** Is there lava in the column a lift would carry the player through? (Then the pulse is a slam, which only presses down.) */
    private static boolean lavaAbove(ServerLevel level, BlockPos from) {
        for (int i = 1; i <= HEADROOM + 1; i++) if (level.getFluidState(from.above(i)).is(FluidTags.LAVA)) return true;
        return false;
    }

    /** A helix of particles winding round the player: upwards for a lift, downwards for a slam. */
    private static void spiral(ServerLevel level, ServerPlayer p, boolean lift, long now) {
        ParticleOptions type = lift ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.PORTAL;
        double base = (now % 30) / 30.0 * Math.PI * 2.0;
        for (int i = 0; i < 6; i++) {
            double a = base + i * 1.0472, h = lift ? i * 0.35 : 2.1 - i * 0.35;
            level.sendParticles(type, p.getX() + Math.cos(a) * 0.9, p.getY() + 0.1 + h, p.getZ() + Math.sin(a) * 0.9, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void lift(ServerLevel level, ServerPlayer p, double strength) {
        Vec3 m = p.getDeltaMovement();
        p.setDeltaMovement(m.x * 0.4, 0.55, m.z * 0.4);
        p.hurtMarked = true;
        p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 14, 4 + (int) Math.round(strength * 3.0), false, false));
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, false, false));
        p.resetFallDistance();
        level.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 1.7f);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 0.2, p.getZ(), 30, 0.4, 0.1, 0.4, 0.4);
    }

    private static void crush(ServerLevel level, ServerPlayer p, double strength) {
        Vec3 m = p.getDeltaMovement();
        p.setDeltaMovement(m.x * 0.4, -GravityPulse.impulse(strength) * 1.1, m.z * 0.4);
        p.hurtMarked = true;
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false));
        p.resetFallDistance();
        level.playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.4f, 0.5f);
        level.sendParticles(ParticleTypes.POOF, p.getX(), p.getY() + 0.1, p.getZ(), 16, 0.5, 0.05, 0.5, 0.05);
    }

    // ---------------------------------------------------------------- block rejection
    /** A block placed in a distorted pocket is refused, unless the player wears an attuned Band of the Spatial Anchor. */
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(event.getLevel() instanceof Level level) || !LivingDungeon.in(level) || !hazards()) return;
        BlockPos at = event.getPos();
        if (SpatialDistortion.strength(at.getX(), at.getY(), at.getZ()) < REJECT_STRENGTH || Anchor.worn(p)) return;
        event.setCanceled(true);
        p.displayClientMessage(Component.literal("Space here rejects what you build.").withStyle(ChatFormatting.DARK_PURPLE), true);
        if (level instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.REVERSE_PORTAL, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.2);
    }

    // ---------------------------------------------------------------- laying traps
    /** Lays the Warp Traps of one loaded chunk (once: the chunk is then remembered). Returns how many were laid. */
    static int layTraps(ServerLevel level, VaultSave save, ChunkPos cp) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(cp.x, cp.z);
        if (chunk == null) return 0;
        save.markTrapsLaid(cp.toLong());
        int laid = 0;
        int top = level.getMaxBuildHeight() - 6, bottom = level.getMinBuildHeight() + 6;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (TrapPlan.Column col : TrapPlan.columns(cp.x, cp.z)) {
            if (laid >= TrapPlan.MAX_PER_CHUNK) break;
            if (!TrapPlan.wanted(col)) continue;
            List<Integer> floors = new ArrayList<>();
            for (int y = top; y > bottom; y--) {
                int below = WorldTerrain.classify(chunk.getBlockState(pos.set(col.x(), y, col.z())));
                if (below != VaultTerrain.STONE && below != VaultTerrain.DEEPSLATE) continue;
                if (SpatialDistortion.strength(col.x(), y, col.z()) < TrapPlan.MIN_STRENGTH) continue;
                int a = WorldTerrain.classify(chunk.getBlockState(pos.set(col.x(), y + 1, col.z()))), b = WorldTerrain.classify(chunk.getBlockState(pos.set(col.x(), y + 2, col.z())));
                if (!VaultTerrain.open(a) || !VaultTerrain.open(b)) continue;
                if (DungeonTier.at(col.x(), y, col.z()) == DungeonTier.GOLDEN_CITY || save.insideAnySite(col.x(), y, col.z(), 2)) continue;
                floors.add(y);
            }
            int pick = TrapPlan.pickFloor(col, floors.size());
            if (pick < 0) continue;
            BlockPos floor = new BlockPos(col.x(), floors.get(pick), col.z());
            if (GateIndex.nearest(level, floor, 6) != null || nearPortal(level, floor) || level.getNearestPlayer(floor.getX() + 0.5, floor.getY() + 1.0, floor.getZ() + 0.5, 4.0, false) != null) continue;
            BlockState was = chunk.getBlockState(floor);
            WarpTrap.lay(level, floor, was);
            laid++;
        }
        return laid;
    }

    /** Is a Root Gate, a Maw or a Descent Gate within three blocks? A trap never lies where someone arrives. A chunk that is not loaded is not looked into. */
    private static boolean nearPortal(ServerLevel level, BlockPos floor) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (!level.isLoaded(pos.set(floor.getX() + dx, floor.getY(), floor.getZ() + dz))) continue;
                for (int dy = 0; dy <= 2; dy++) {
                    BlockState s = level.getBlockState(pos.set(floor.getX() + dx, floor.getY() + dy, floor.getZ() + dz));
                    if (s.is(DungeonBlocks.DUNGEON_GATE.get()) || s.is(DungeonBlocks.DUNGEON_MAW.get()) || s.is(NexusBlocks.DESCENT_GATE.get())) return true;
                }
            }
        }
        return false;
    }
}
