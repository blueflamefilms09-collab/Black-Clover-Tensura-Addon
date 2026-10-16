package com.newuniverse.nusmp.corpse;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/**
 * Why: which creatures can never be raised is a content decision (bosses, the dragon, villagers and other NPCs, things that explode
 * or are not really bodies), and a data tag lets a pack or an admin change it without a rebuild. The shipped list is
 * {@code data/nusmp/tags/entity_type/corpse_immune.json}; bosses are also refused in code ({@link Corpses#raisable}).
 */
public final class CorpseTags {
    private CorpseTags() {}

    public static final TagKey<EntityType<?>> IMMUNE = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "corpse_immune"));
}
