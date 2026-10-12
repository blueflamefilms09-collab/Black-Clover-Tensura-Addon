package com.newuniverse.nusmp.mixin;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.19.0: the second net under the Dungeon's unbreakable blocks (see {@link LevelGuardMixin}). A mod that writes a loaded chunk directly
 * skips the world's block writers; the chunk's own writer answers null, which means "nothing changed" to every caller, under the same rule.
 * Optional (require = 0).
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkGuardMixin {
    @Inject(method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void nusmp$guardChunkWrite(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir) {
        if (DungeonGuard.refusesSet(((LevelChunk) (Object) this).getLevel(), pos, state)) cir.setReturnValue(null);
    }
}
