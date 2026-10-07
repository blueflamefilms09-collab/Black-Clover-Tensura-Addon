package com.newuniverse.nusmp.mixin.client;

import com.newuniverse.nusmp.client.SwordDrawClient;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 0.52: after vanilla has posed a player model, the Grimoire Sword Draw (see {@link SwordDrawClient}) overlays its keyframes while
 * that player is drawing a sword. Optional (require = 0): if the target ever changes the game still runs, without the pose.
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"), require = 0)
    private void nusmp$swordDraw(LivingEntity e, float limbSwing, float limbAmount, float age, float yaw, float pitch, CallbackInfo ci) {
        SwordDrawClient.pose((PlayerModel<?>) (Object) this, e, age);
        com.newuniverse.nusmp.client.CastAnimClient.pose((PlayerModel<?>) (Object) this, e, age);       // 0.54: the casting body animations
    }
}
