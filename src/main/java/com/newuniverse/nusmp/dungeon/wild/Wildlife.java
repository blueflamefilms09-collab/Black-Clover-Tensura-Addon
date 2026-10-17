package com.newuniverse.nusmp.dungeon.wild;

import com.newuniverse.nusmp.dungeon.DungeonCreature;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * Why: the dungeon's monsters are listed by <b>entity-type tags</b> (data/nusmp/tags/entity_type/dungeon/: tier1, tier2, tier3, legendary, and
 * excluded as a veto), not by code, so a modpack edits the roster without touching a class, and every entry of a tag that belongs to another
 * mod (Tensura, Ice and Fire) is optional: a missing mod or a wrong id never breaks loading. This class is the one reader of those tags, and
 * what the economy needs from them is the tier ({@link #tierOf}): it grades the Magic Stone a kill leaves. Until 1.58.0 it was also the roster
 * of the Living Dungeon's ecosystem (who lives there, what hunts what, which carcass a kill leaves); that machinery went with the Living
 * Dungeon, the tags and the tier stayed.
 *
 * <p>The surviving 1.18.0 creatures ({@link DungeonCreature}) are not in the tags and answer with their own tier.
 */
public final class Wildlife {
    private Wildlife() {}

    public static final TagKey<EntityType<?>> TIER1 = tag("dungeon/tier1"), TIER2 = tag("dungeon/tier2"), TIER3 = tag("dungeon/tier3"),
            LEGENDARY = tag("dungeon/legendary"), EXCLUDED = tag("dungeon/excluded");

    private static TagKey<EntityType<?>> tag(String path) {
        return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("nusmp", path));
    }

    // ---------------------------------------------------------------- the contract other packages call
    /** 0 = not a monster of the dungeon; 1 to 3 by tier tag; a legendary creature counts as 3; the old custom creatures answer with their own tier. */
    public static int tierOf(Entity e) {
        if (e == null) return 0;
        if (e instanceof DungeonCreature c) return c.tier();
        int t = tierOfType(e.getType());
        // a shadow soldier is a servant of a master, not a monster of the dungeon (the marks the old Monarch's soldiers and the Underworld servants carried)
        if (t > 0 && e instanceof net.minecraft.world.entity.Mob && (e.getPersistentData().contains("nusmp_shadow") || e.getTags().contains("nusmp_underworld"))) return 0;
        return t;
    }

    /** The same for an entity type (named apart so that tierOf(null) stays unambiguous). */
    public static int tierOfType(EntityType<?> t) {
        return TierRule.tierFor(t.is(TIER1), t.is(TIER2), t.is(TIER3), t.is(LEGENDARY), t.is(EXCLUDED));
    }

    /** The registry id of a type as text ("tensura:slime"). */
    public static String idOf(EntityType<?> t) { return BuiltInRegistries.ENTITY_TYPE.getKey(t).toString(); }

    /** The entity type with this id, or null if it is not registered (a mod that is not installed). */
    public static EntityType<?> typeOf(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return null;
        return BuiltInRegistries.ENTITY_TYPE.getOptional(rl).orElse(null);
    }
}
