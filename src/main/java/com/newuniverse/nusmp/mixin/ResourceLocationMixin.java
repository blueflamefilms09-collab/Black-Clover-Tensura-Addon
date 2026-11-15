package com.newuniverse.nusmp.mixin;

import com.newuniverse.nusmp.migrate.LegacyId;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 3.84.0: every ResourceLocation is built through one constructor, so an id saved before the rename (old worlds, skills, item components, data packs, commands) is read as its new
 * id here. Optional (require = 0): if the constructor ever changes the game still runs, only the migration is off.
 */
@Mixin(ResourceLocation.class)
public abstract class ResourceLocationMixin {
    @ModifyVariable(method = "<init>(Ljava/lang/String;Ljava/lang/String;)V", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private static String anime_multiverse$legacyNamespace(String namespace) {
        return LegacyId.namespace(namespace);
    }
}
