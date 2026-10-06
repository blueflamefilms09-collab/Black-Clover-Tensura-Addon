package com.newuniverse.nusmp.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/**
 * Compatibility fix for Tensura More Overlord 0.2.3: its undead check crashes the whole game
 * ("Value missing: tensuramoreoverlord:skeleton_mage") when a race lookup fails. We wrap that one
 * method: if it throws, the entity is simply treated as "not an Overlord undead".
 * Only applies when More Overlord is installed; their jar is not changed.
 */
@Pseudo
@Mixin(targets = "com.github.wal_bos.moreoverlord.handler.OverlordUndeadHandler", remap = false)
public abstract class OverlordUndeadHandlerMixin {
    @WrapMethod(method = "isOverlordUndead", remap = false)
    private static boolean nusmp$safeUndeadCheck(LivingEntity entity, Operation<Boolean> original) {
        try {
            return original.call(entity);
        } catch (RuntimeException e) {
            return false;
        }
    }
}
