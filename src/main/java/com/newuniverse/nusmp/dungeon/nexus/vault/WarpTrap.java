package com.newuniverse.nusmp.dungeon.nexus.vault;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.dungeon.DungeonTier;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * What stepping on a Warp Trap does (docs/living_dungeon.md, "Spatial Warping"): the floor lets go and the player lands 8 to 24 blocks
 * away on the same floor of the Dungeon, at a spot that is safe to stand in (never in lava, never in rock, never inside a hidden room),
 * dizzy for five seconds. A player is thrown once in ten seconds. It does no damage; the Band of the Spatial Anchor ignores it.
 */
final class WarpTrap {
    private WarpTrap() {}

    private static final String COOL = "nusmp_warp_trap", NOTE = "nusmp_warp_trap_note";
    private static final int COOLDOWN = 200, NAUSEA = 100;

    static void trigger(ServerLevel level, ServerPlayer p, BlockPos trap) {
        if (!LivingDungeon.in(level) || p.isSpectator() || !p.isAlive() || !Distortion.hazards()) return;
        long now = level.getGameTime();
        CompoundTag data = p.getPersistentData();
        if (Anchor.worn(p)) {
            if (now >= data.getLong(NOTE)) {
                data.putLong(NOTE, now + 100);
                p.displayClientMessage(Component.literal("The Anchor holds you. The floor twists, and finds nothing to take.").withStyle(ChatFormatting.GRAY), true);
            }
            return;
        }
        if (now < data.getLong(COOL)) return;
        data.putLong(COOL, now + COOLDOWN);
        BlockPos to = destination(level, p, trap);
        if (to == null) {
            p.displayClientMessage(Component.literal("The space round you ripples, and settles.").withStyle(ChatFormatting.DARK_PURPLE), true);
            return;
        }
        level.sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.4, 0.8, 0.4, 0.6);
        level.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 0.6f);
        p.teleportTo(level, to.getX() + 0.5, to.getY(), to.getZ() + 0.5, p.getYRot(), p.getXRot());
        p.resetFallDistance();
        p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NAUSEA, 0, false, true));
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, to.getX() + 0.5, to.getY() + 1.0, to.getZ() + 0.5, 40, 0.4, 0.8, 0.4, 0.3);
        level.playSound(null, to, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.2f);
        p.displayClientMessage(Component.literal("Space folds. You are somewhere else.").withStyle(ChatFormatting.DARK_PURPLE), true);
    }

    /** A safe place 8 to 24 blocks from the trap, on the same floor and out of every hidden room; null if the tries found none. */
    static BlockPos destination(ServerLevel level, ServerPlayer p, BlockPos trap) {
        boolean wet = level.getFluidState(p.blockPosition()).is(FluidTags.WATER);
        DungeonTier tier = DungeonTier.at(trap.getX(), trap.getY(), trap.getZ());
        VaultSave save = VaultSave.get(level.getServer());
        VaultTerrain terrain = new WorldTerrain(level, false);
        List<WarpPlan.Offset> offsets = WarpPlan.offsets(level.getGameTime() * 31L + p.getId() * 7919L + trap.asLong(), WarpPlan.TRIES);
        for (WarpPlan.Offset o : offsets) {
            int x = trap.getX() + o.dx(), z = trap.getZ() + o.dz();
            int y = WarpPlan.spotInColumn(terrain, x, trap.getY() + 1 + o.dy(), z, wet);
            if (y == Integer.MIN_VALUE) continue;
            if (DungeonTier.at(x, y, z) != tier || save.insideAnySite(x, y, z, 1)) continue;
            return new BlockPos(x, y, z);
        }
        return null;
    }

    /** Lays a trap at a floor block: stone or deepslate to match. Inside the guard, because it replaces a held block. */
    static void lay(ServerLevel level, BlockPos floor, BlockState was) {
        BlockState trap = NexusBlocks.WARP_TRAP.get().defaultBlockState().setValue(WarpTrapBlock.DEEP, was.is(net.minecraft.world.level.block.Blocks.DEEPSLATE));
        DungeonGuard.internal(() -> level.setBlock(floor, trap, VaultSink.FLAGS));
    }
}
