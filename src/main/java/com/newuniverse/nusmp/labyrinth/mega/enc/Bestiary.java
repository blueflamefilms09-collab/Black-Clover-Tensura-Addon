package com.newuniverse.nusmp.labyrinth.mega.enc;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Why: the encounter tables name monsters by entity id and by faction, and the game must turn both into entity TYPES without ever assuming that
 * an optional mod is installed or that a tag is filled. This is the one place that does it. An id is looked up in the registry (absent = the
 * entry is not usable, never an exception). A faction is the entity-type tag {@code nusmp:mega/<faction>} (written by the ecology; every member
 * optional), and its members are filtered to things that can really stand in a dungeon room and fight: no bosses (the {@code c:bosses} tag, our
 * {@code nusmp:boss} tag and the dungeon's own {@code excluded} tag), nothing that is not a living creature with attributes, nothing that
 * needs water to live, no marker entities. Each member's toughness is its default maximum health, read from {@code DefaultAttributes} (20 when
 * the type has none), which is what {@link MobPicker} sorts by. Member lists are cached for a minute and are dropped whenever the data reloads,
 * because a tag can change without a restart and a stale list would spawn things the pack just removed.
 */
final class Bestiary {
    private Bestiary() {}

    private static final TagKey<EntityType<?>> BOSS = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("nusmp", "boss"));
    private static final TagKey<EntityType<?>> EXCLUDED = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("nusmp", "dungeon/excluded"));
    private static final long KEEP_MS = 60_000L;

    private record Cached(List<MobPicker.Candidate> members, long at, int version) {}

    private static final Map<String, Cached> CACHE = new HashMap<>();

    /** The entity type with this id, or null when it is not registered (a mod that is not installed, a typo). */
    static EntityType<?> type(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id == null ? "" : id);
        if (rl == null) return null;
        Optional<EntityType<?>> t = BuiltInRegistries.ENTITY_TYPE.getOptional(rl);
        return t.orElse(null);
    }

    /** Can this type be a monster of an encounter: a living creature the game knows the attributes of, that is not a marker or a projectile? */
    static boolean usable(EntityType<?> t) {
        return t != null && t.canSummon() && t.getCategory() != MobCategory.MISC && DefaultAttributes.hasSupplier(t);
    }

    /** The default maximum health of a type, or {@link MobPicker#DEFAULT_TOUGHNESS} when it cannot be read. */
    @SuppressWarnings("unchecked")
    static double toughness(EntityType<?> t) {
        try {
            if (!DefaultAttributes.hasSupplier(t)) return MobPicker.DEFAULT_TOUGHNESS;
            AttributeSupplier s = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) t);
            if (s == null || !s.hasAttribute(Attributes.MAX_HEALTH)) return MobPicker.DEFAULT_TOUGHNESS;
            double h = s.getBaseValue(Attributes.MAX_HEALTH);
            return h > 0.0 ? h : MobPicker.DEFAULT_TOUGHNESS;
        } catch (RuntimeException | LinkageError unreadable) {
            return MobPicker.DEFAULT_TOUGHNESS;
        }
    }

    /** The registry id of a type as text. */
    static String id(EntityType<?> t) { return BuiltInRegistries.ENTITY_TYPE.getKey(t).toString(); }

    /** The members of a faction that may appear in an encounter (see the class comment); empty for an unknown faction or an empty tag. */
    static synchronized List<MobPicker.Candidate> members(String faction) {
        if (faction == null || faction.isEmpty()) return List.of();
        long now = System.currentTimeMillis();
        Cached c = CACHE.get(faction);
        if (c != null && c.version() == EncData.version() && now - c.at() < KEEP_MS && !(c.members().isEmpty() && now - c.at() > 5_000L)) return c.members();
        List<MobPicker.Candidate> out = new ArrayList<>();
        try {
            TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("nusmp", "mega/" + faction));
            Optional<HolderSet.Named<EntityType<?>>> set = BuiltInRegistries.ENTITY_TYPE.getTag(tag);
            if (set.isPresent()) {
                for (Holder<EntityType<?>> h : set.get()) {
                    EntityType<?> t = h.value();
                    if (!usable(t) || isWaterBound(t) || t.is(Tags.EntityTypes.BOSSES) || t.is(BOSS) || t.is(EXCLUDED)) continue;
                    out.add(new MobPicker.Candidate(id(t), toughness(t)));
                }
            }
        } catch (RuntimeException e) {
            out.clear();
        }
        List<MobPicker.Candidate> frozen = List.copyOf(out);
        CACHE.put(faction, new Cached(frozen, now, EncData.version()));
        return frozen;
    }

    static boolean isWaterBound(EntityType<?> t) {
        MobCategory c = t.getCategory();
        return c == MobCategory.WATER_CREATURE || c == MobCategory.WATER_AMBIENT || c == MobCategory.UNDERGROUND_WATER_CREATURE;
    }

    static synchronized void clear() { CACHE.clear(); }
}
