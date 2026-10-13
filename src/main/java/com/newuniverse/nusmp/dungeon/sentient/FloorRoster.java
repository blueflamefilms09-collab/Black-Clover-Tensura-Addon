package com.newuniverse.nusmp.dungeon.sentient;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.entity.zogratis.DarkDiscipleEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Who is born on each floor: the Upper Catacombs birth goblins, slimes and (the kobolds of the design, which Tensura does not have) hound
 * dogs; the Deep Waterways lizardmen, aqua slimes and water spirits; the Underworld Gate lesser devils and Dark Disciples. The lists are the
 * entity-type tags {@code nusmp:dungeon/born_catacombs}, {@code born_waterways} and {@code born_underworld} (a modpack edits them; every
 * entry is optional, so a mod that is not installed is simply not there), with the weights of {@link FloorWeights}; if a tag cannot be
 * found the built-in list stands. The mobs are made here and added by {@link WallBirth}, never by this class.
 */
public final class FloorRoster {
    private FloorRoster() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** How long a list read from the tags is kept (a datapack reload shows up by itself), and how soon an empty one is read again. */
    private static final long KEEP_MS = 30_000L, RETRY_MS = 5_000L;
    private static final Devil[] DEVILS = {Devil.LUCIFERO, Devil.MEGICULA, Devil.BEELZEBUB};

    /** One kind that can be born on a floor, with its weight. */
    private record Choice(String id, EntityType<?> type, int weight) {}

    private record Cached(List<Choice> list, long builtAt) {}

    private static final Map<FloorKind, Cached> CACHE = new EnumMap<>(FloorKind.class);
    private static final Set<String> WARNED = new HashSet<>();

    /**
     * A positioned mob of the floor's spawn list that is NOT yet added to the level, or null if nothing fits (a floor with no births, or
     * none of the kinds is in this game). Finalized as an event spawn.
     */
    public static Mob create(ServerLevel level, FloorKind kind, RandomSource rnd, BlockPos spot) {
        if (level == null || kind == null || rnd == null || spot == null) return null;
        List<Choice> list = choices(kind);
        if (list.isEmpty()) return null;
        List<String> ids = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        for (Choice c : list) {
            ids.add(c.id());
            weights.add(c.weight());
        }
        Dice dice = rnd::nextDouble;
        String id = FloorWeights.pick(ids, weights, dice);
        if (id == null) return null;
        for (Choice c : list) {
            if (!c.id().equals(id)) continue;
            try {
                if (FloorWeights.DISCIPLE.equals(c.id())) return spawnDisciple(level, spot, rnd);
                Mob m = make(level, c.type(), spot, rnd);
                if (m != null && FloorWeights.aqua(kind, c.id())) makeAqua(m);
                return m;
            } catch (RuntimeException | LinkageError failure) {
                if (WARNED.add(c.id() + failure.getClass().getName())) LOG.warn("[nusmp] Could not make {} for {}", c.id(), kind.label(), failure);
                return null;
            }
        }
        return null;
    }

    public static boolean isBorn(Entity e) { return e != null && e.getTags().contains(SentientTags.BORN); }

    /**
     * A mob of this type standing at the spot, finalized as an event spawn (a direct finalizeSpawn does not fire the spawn event, so the
     * Dungeon's own hook that cancels the world's spawns leaves it alone) and NOT added to the level. Null if the type is not a mob.
     */
    public static Mob make(ServerLevel level, EntityType<?> type, BlockPos spot, RandomSource rnd) {
        Entity made = type.create(level);
        if (!(made instanceof Mob m)) {
            if (made != null) made.discard();
            return null;
        }
        m.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, rnd.nextFloat() * 360f, 0f);
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.EVENT, null);
        return m;
    }

    /** The Tensura slime made at home in water: it breathes there and swims fast, and is called an Aqua Slime (the name is not shown above its head). */
    private static void makeAqua(Mob m) {
        m.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, MobEffectInstance.INFINITE_DURATION, 0, true, false));
        m.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, MobEffectInstance.INFINITE_DURATION, 0, true, false));
        m.setCustomName(Component.literal(SentientText.AQUA_SLIME));
        m.setCustomNameVisible(false);
        m.addTag(SentientTags.AQUA);
    }

    /**
     * A free-standing Dark Disciple of the Zogratis family at the spot, NOT yet added to the level: a devil of Lucifero, Megicula and Beelzebub
     * chosen at random, channelling 8 to 20 percent of its power. Null if the entity cannot be created.
     */
    public static Mob spawnDisciple(ServerLevel level, BlockPos spot, RandomSource rnd) {
        Devil devil = DEVILS[rnd.nextInt(DEVILS.length)];
        float fraction = 0.08f + rnd.nextFloat() * 0.12f;
        return DarkDiscipleEntity.makeFree(level, new Vec3(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5), devil, fraction);
    }

    // ---------------------------------------------------------------- the lists
    private static String tagPath(FloorKind kind) {
        return switch (kind) {
            case CATACOMBS -> "dungeon/born_catacombs";
            case WATERWAYS -> "dungeon/born_waterways";
            case UNDERWORLD -> "dungeon/born_underworld";
            default -> null;
        };
    }

    private static synchronized List<Choice> choices(FloorKind kind) {
        long now = System.currentTimeMillis();
        Cached c = CACHE.get(kind);
        if (c != null && now - c.builtAt() < (c.list().isEmpty() ? RETRY_MS : KEEP_MS)) return c.list();
        List<Choice> list = build(kind);
        CACHE.put(kind, new Cached(list, now));
        return list;
    }

    private static List<Choice> build(FloorKind kind) {
        List<Choice> out = new ArrayList<>();
        String path = tagPath(kind);
        if (path == null) return out;
        TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("nusmp", path));
        Optional<HolderSet.Named<EntityType<?>>> set = BuiltInRegistries.ENTITY_TYPE.getTag(tag);
        if (set.isPresent()) {                                                       // the tag is the list, even if every entry of it is a mod that is not here
            for (Holder<EntityType<?>> h : set.get()) {
                EntityType<?> t = h.value();
                String id = BuiltInRegistries.ENTITY_TYPE.getKey(t).toString();
                out.add(new Choice(id, t, FloorWeights.weight(kind, id)));
            }
            return out;
        }
        for (FloorWeights.Entry e : FloorWeights.defaults(kind)) {                   // no tag: the built-in list
            ResourceLocation id = ResourceLocation.tryParse(e.id());
            if (id == null) continue;
            Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
            if (type.isPresent()) out.add(new Choice(e.id(), type.get(), e.weight()));
        }
        return out;
    }
}
