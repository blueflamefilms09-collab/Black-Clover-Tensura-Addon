package com.newuniverse.nusmp.labyrinth.mega.eco;

import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: every system that cares about factions asks the same question, "which faction is this creature?", thousands of times a minute (every
 * mob that joins, every target change, every candidate of every turf-war scan), so the answer comes from one place that is cheap. A creature's
 * faction is its entity TYPE's membership in one of the entity-type tags {@code nusmp:mega/<faction>} (the tags are the contract with the
 * encounter director, which spawns "faction X" from the same tag), resolved once per type and cached; the cache is dropped whenever tags or the
 * faction files reload, because a stale "no faction" would silently switch the wars off for a type a datapack just added. The loaded
 * {@link FactionGraph} is swapped in one assignment so a reload in progress never exposes a half-built graph. A type listed in two faction tags
 * belongs to the first by sorted faction id (the shipped tags keep every type in exactly one, which a test enforces). Server thread.
 */
public final class FactionRegistry {
    private FactionRegistry() {}

    /** "No faction" in the cache, because a ConcurrentHashMap holds no null. */
    private static final String NONE = "";
    private static final Map<EntityType<?>, String> CACHE = new ConcurrentHashMap<>();
    private static volatile FactionGraph graph = FactionGraph.EMPTY;
    private static volatile Map<String, TagKey<EntityType<?>>> tags = Map.of();

    /** The factions as the last data reload left them ({@link FactionGraph#EMPTY} before the first reload). */
    public static FactionGraph graph() { return graph; }

    /** Replaces the loaded factions (called by the reload listener and by tests) and forgets every cached answer. */
    static void install(FactionGraph g) {
        Map<String, TagKey<EntityType<?>>> t = new LinkedHashMap<>();
        for (String id : g.ids()) {
            ResourceLocation rl = ResourceLocation.tryParse(g.def(id).tag());
            if (rl == null) rl = ResourceLocation.fromNamespaceAndPath("nusmp", "mega/" + id);
            t.put(id, TagKey.create(Registries.ENTITY_TYPE, rl));
        }
        tags = t;
        graph = g;
        CACHE.clear();
    }

    /** Do members of {@code a} hunt members of {@code b} wherever they meet? (Directional only for a faction whose file says {@code oneWay}.) */
    public static boolean rivals(String a, String b) { return graph.hostile(a, b); }

    /** Two different factions that never fight each other by agreement. */
    public static boolean allies(String a, String b) { return graph.allied(a, b); }

    /** How much faction {@code id} likes a region of this theme (see {@link FactionGraph#themeWeight}). */
    public static double themeWeight(String id, String themeId) { return graph.themeWeight(id, themeId); }

    /** Forgets every cached answer (tags changed, a world closed). */
    public static void clearCache() { CACHE.clear(); }

    public static void onTagsUpdated(TagsUpdatedEvent e) { CACHE.clear(); }

    /** The faction id of an entity type, or null when no faction tag lists it. */
    public static String factionOf(EntityType<?> type) {
        if (type == null) return null;
        String cached = CACHE.get(type);
        if (cached == null) {
            cached = NONE;
            for (Map.Entry<String, TagKey<EntityType<?>>> e : tags.entrySet()) {
                if (type.is(e.getValue())) {
                    cached = e.getKey();
                    break;
                }
            }
            CACHE.put(type, cached);
        }
        return cached.isEmpty() ? null : cached;
    }

    public static String factionOf(Entity e) { return e == null ? null : factionOf(e.getType()); }

    /** The entity-type tag that lists the members of a faction, or null for an unknown id. */
    public static TagKey<EntityType<?>> tagOf(String id) { return id == null ? null : tags.get(id); }

    /** How many entity types that exist in this game are listed in the faction's tag (tag entries of mods that are not installed do not count). */
    public static int memberCount(String id) {
        TagKey<EntityType<?>> key = tagOf(id);
        if (key == null) return 0;
        return BuiltInRegistries.ENTITY_TYPE.getTag(key).map(HolderSet::size).orElse(0);
    }

    /** The faction's name in its own colour: the lang key {@code message.nusmp.mega.mec.faction.<id>}, falling back to the English name in the file. */
    public static MutableComponent name(String id) {
        FactionDef d = graph.def(id);
        MutableComponent c = Component.translatableWithFallback("message.nusmp.mega.mec.faction." + id, d != null ? d.name() : String.valueOf(id));
        return d == null ? c : c.withStyle(s -> s.withColor(d.color()));
    }
}
