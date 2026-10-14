package com.newuniverse.nusmp.boss;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * The glue of {@link BossRegistry}: asks the entity registry for an entity's type key and lets the pure table decide. Everything the
 * boss overhaul does starts with this check, so a vanilla or base-Tensura entity (any namespace but this mod's) is never touched.
 */
public final class BossEntities {
    private BossEntities() {}

    /** The boss id of an entity ("lucius", "goliath", ...), or null when it is not one of this mod's registry-listed bosses. */
    public static String bossId(Entity e) {
        if (e == null) return null;
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return key == null ? null : BossRegistry.idOf(key.getNamespace(), key.getPath()).orElse(null);
    }

    public static boolean isModBoss(Entity e) { return bossId(e) != null; }
}
