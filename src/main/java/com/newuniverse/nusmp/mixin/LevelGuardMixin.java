package com.newuniverse.nusmp.mixin;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.19.0: the net under the Dungeon's unbreakable blocks. Every mod that clears, replaces or burns a block does it through one of these
 * three writers of {@link Level}; in the Living Dungeon (server side only, and only while its {@code unbreakable} switch is on) a call that
 * would take a held block away is answered "nothing changed" before it starts, so nothing drops and nothing is copied. The first thing
 * each handler does is {@code DungeonGuard}'s cheap test (side, dimension, switch); the old block is read last. All three are optional
 * (require = 0): if a target ever changes the game still runs, with the events as the only guard.
 */
@Mixin(Level.class)
public abstract class LevelGuardMixin {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void nusmp$guardSetBlock(BlockPos pos, BlockState state, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> cir) {
        if (DungeonGuard.refusesSet((Level) (Object) this, pos, state)) cir.setReturnValue(false);
    }

    @Inject(method = "removeBlock(Lnet/minecraft/core/BlockPos;Z)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void nusmp$guardRemoveBlock(BlockPos pos, boolean moving, CallbackInfoReturnable<Boolean> cir) {
        if (DungeonGuard.refusesRemoval((Level) (Object) this, pos)) cir.setReturnValue(false);
    }

    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void nusmp$guardDestroyBlock(BlockPos pos, boolean drop, Entity breaker, int recursionLeft, CallbackInfoReturnable<Boolean> cir) {
        if (DungeonGuard.refusesRemoval((Level) (Object) this, pos)) {
            DungeonGuard.tell(breaker);
            cir.setReturnValue(false);
        }
    }
}
