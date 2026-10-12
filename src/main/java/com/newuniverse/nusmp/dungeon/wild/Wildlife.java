package com.newuniverse.nusmp.dungeon.wild;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonCreature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Who lives in the Living Dungeon (docs/living_dungeon.md section 1): the real mobs of the base Tensura mod and, when it is installed, of
 * Ice and Fire. They are listed by <b>entity-type tags</b> (data/nusmp/tags/entity_type/dungeon/: tier1, tier2, tier3, legendary, and
 * excluded as a veto; aquatic and grazers as notes), so a modpack edits the roster without touching code, and every entry of a tag that
 * belongs to a mod is optional: a missing mod or a wrong id never breaks loading. Neither mod is on the compile classpath for this: the
 * types are found by registry id at run time.
 *
 * <p>The surviving 1.18.0 creatures ({@link DungeonCreature}) are not in the tags and keep answering with their own tier.
 */
public final class Wildlife {
    private Wildlife() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();

    public static final TagKey<EntityType<?>> TIER1 = tag("dungeon/tier1"), TIER2 = tag("dungeon/tier2"), TIER3 = tag("dungeon/tier3"),
            LEGENDARY = tag("dungeon/legendary"), EXCLUDED = tag("dungeon/excluded"), AQUATIC = tag("dungeon/aquatic"), GRAZERS = tag("dungeon/grazers");

    private static TagKey<EntityType<?>> tag(String path) {
        return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("nusmp", path));
    }

    // ---------------------------------------------------------------- the contract other packages call
    /** 0 = not a member of the roster; 1 to 3 by tier tag; a legendary creature counts as 3; the old custom creatures answer with their own tier. */
    public static int tierOf(Entity e) {
        if (e == null) return 0;
        if (e instanceof DungeonCreature c) return c.tier();
        return tierOf(e.getType());
    }

    /** The same for an entity type. */
    public static int tierOf(EntityType<?> t) {
        return Roster.tierFor(t.is(TIER1), t.is(TIER2), t.is(TIER3), t.is(LEGENDARY), t.is(EXCLUDED));
    }

    /** Is this an apex predator: tier 3, or legendary? */
    public static boolean isApex(Entity e) { return tierOf(e) >= 3; }

    /**
     * Spawns one tier-2 creature of the roster standing at {@code at}, for a guard (the Armorsaurus, which replaces the old Living Armor, when
     * it is a tier-2 member; otherwise another by weight). Returns true if it spawned.
     */
    public static boolean spawnGuard(ServerLevel level, BlockPos at) { return WildSpawn.guard(level, at); }

    // ---------------------------------------------------------------- what else the rest of the mod may want to know
    public static boolean isLegendary(Entity e) {
        return e != null && !(e instanceof DungeonCreature) && e.getType().is(LEGENDARY) && !e.getType().is(EXCLUDED);
    }

    /** A creature that never hunts: it grazes the magicules, the mycelium and the carcasses whatever its tier. */
    public static boolean isGrazer(Entity e) { return e != null && e.getType().is(GRAZERS); }

    /** Which Carcass it leaves when it dies (CarcassBlock.KIND). */
    public static int carcassKind(Entity e) {
        if (e instanceof DungeonCreature c) return c.carcassKind();
        return e == null ? 4 : WildRules.carcassKind(idOf(e.getType()));
    }

    /** The weight its Carcass is left with (its maximum health: bigger things leave more meat). */
    public static float carcassWeight(Entity e) {
        return e instanceof LivingEntity l ? WildRules.carcassWeight(l.getMaxHealth()) : 0f;
    }

    /** What it weighs in the chunk's biomass. */
    public static float massOf(Entity e) {
        if (e instanceof DungeonCreature c) return c.weight();
        return e instanceof LivingEntity l ? WildRules.massOf(l.getMaxHealth()) : 0f;
    }

    /** Its hunger (0-100), or -1 if it is not a creature of the Dungeon. */
    public static int hunger(Entity e) {
        if (e instanceof DungeonCreature c) return c.hunger();
        if (e != null && tierOf(e) > 0 && e.hasData(Dweller.DWELLER.get())) return e.getData(Dweller.DWELLER).appetite.hunger();
        return -1;
    }

    /** The registry id of a type as text ("tensura:slime"). */
    public static String idOf(EntityType<?> t) { return BuiltInRegistries.ENTITY_TYPE.getKey(t).toString(); }

    /** The entity type with this id, or null if it is not registered (a mod that is not installed). */
    public static EntityType<?> typeOf(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return null;
        return BuiltInRegistries.ENTITY_TYPE.getOptional(rl).orElse(null);
    }

    // ---------------------------------------------------------------- the roster, read from the tags
    private static Roster cached;
    private static long builtAt;
    private static String lastReport = "";

    /** The roster as the tags say now (rebuilt every half minute, so a datapack reload shows up by itself). */
    public static synchronized Roster roster() {
        long now = System.currentTimeMillis();
        long keep = cached == null || cached.all().isEmpty() ? 5_000L : 30_000L;
        if (cached == null || now - builtAt > keep) {
            cached = build();
            builtAt = now;
            report(cached);
        }
        return cached;
    }

    /** Every id of the roster (for command suggestions). */
    public static List<String> rosterIds() {
        List<String> out = new ArrayList<>();
        for (Roster.Entry e : roster().all()) out.add(e.id());
        return out;
    }

    private static Roster build() {
        List<Roster.Entry> out = new ArrayList<>();
        Set<ResourceLocation> seen = new HashSet<>();
        add(out, seen, LEGENDARY, 3, true);                                  // the rarest first: a type listed twice keeps its highest place
        add(out, seen, TIER3, 3, false);
        add(out, seen, TIER2, 2, false);
        add(out, seen, TIER1, 1, false);
        return new Roster(out);
    }

    private static void add(List<Roster.Entry> out, Set<ResourceLocation> seen, TagKey<EntityType<?>> tag, int tier, boolean legendary) {
        Optional<HolderSet.Named<EntityType<?>>> set = BuiltInRegistries.ENTITY_TYPE.getTag(tag);
        if (set.isEmpty()) return;
        for (Holder<EntityType<?>> h : set.get()) {
            EntityType<?> t = h.value();
            if (t.is(EXCLUDED) || t.getCategory() == MobCategory.MISC) continue;
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(t);
            if (seen.add(id)) out.add(new Roster.Entry(id.toString(), tier, legendary, t.is(AQUATIC), t.is(GRAZERS)));
        }
    }

    private static boolean loaded(String modId) {
        ModList list = ModList.get();
        return list != null && list.isLoaded(modId);
    }

    /** Logs which mods the roster found and how many creatures each tier has (once, and again if it changes). */
    private static void report(Roster r) {
        boolean tensura = loaded("tensura"), fire = loaded("iceandfire");
        String line = "Living Dungeon wildlife: tier 1 x" + r.count(1) + ", tier 2 x" + r.count(2) + ", tier 3 x" + r.count(3) + ", legendary x" + r.legendCount()
                + "; Tensura " + (tensura ? "found" : "NOT found") + " (" + r.countNamespace("tensura") + " creatures), Ice and Fire "
                + (fire ? "found" : "not installed") + " (" + r.countNamespace("iceandfire") + " creatures)";
        if (line.equals(lastReport)) return;
        lastReport = line;
        LOG.info(line);
        if (tensura) {
            for (String core : new String[]{"slime", "armorsaurus", "direwolf", "basilisk"}) {
                boolean listed = false;
                for (Roster.Entry e : r.all()) if (e.id().equals("tensura:" + core)) listed = true;
                if (!listed) LOG.warn("Living Dungeon wildlife: tensura:{} is not in any roster tag (missing from Tensura, or vetoed by nusmp:dungeon/excluded)", core);
            }
        }
    }
}
