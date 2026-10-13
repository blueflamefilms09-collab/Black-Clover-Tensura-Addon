package com.newuniverse.nusmp.relics;

import com.newuniverse.nusmp.relics.order.UniqueLedger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Where relics come from in the wild. {@link #rollUnissued} hands out a relic the world does not have yet (the God of Order's ledger decides, so
 * two chests, two Lootr openings or two Mimics can never produce the same one). Two sources use it: the chests of structures
 * ({@link RelicLootModifier}, which runs whenever a loot table under "chests/" is rolled, Lootr's per-player rolls included) and Mimics (any
 * entity whose type path is "mimic", or in the tag nusmp:relic_mimic_types: the Artifacts mod's and the Relics mod's mimics).
 */
public final class RelicLoot {
    private RelicLoot() {}

    /** Chance a dying Mimic drops a relic of RARE to LEGEND; and the chance it drops the tame Cloak Mimic instead. */
    static final float MIMIC_CHANCE = 0.06f, CLOAK_CHANCE = 0.015f;
    static final String CLOAK_ID = "cloak_mimic_tame";
    static final TagKey<EntityType<?>> MIMIC_TAG = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("nusmp", "relic_mimic_types"));

    /** Game-bus listeners. */
    public static void register(IEventBus bus) { bus.addListener(RelicLoot::onDrops); }

    /**
     * A new copy of a relic of grade {@code min} to {@code max} that no one has yet (stamped in the ledger), chosen by the grades' loot weights;
     * EMPTY if every relic of that range already lives. Consumables are never rolled.
     */
    public static ItemStack rollUnissued(ServerLevel level, RandomSource rng, Grade min, Grade max) {
        if (level == null || level.getServer() == null) return ItemStack.EMPTY;
        Set<String> live = UniqueLedger.liveIds(level.getServer());
        List<RelicDef> pool = new ArrayList<>();
        for (RelicDef d : Relics.all()) {
            if (!d.isUnique() || live.contains(d.id())) continue;
            if (d.grade().ordinal() < min.ordinal() || d.grade().ordinal() > max.ordinal()) continue;
            pool.add(d);
        }
        for (int tries = 0; tries < 8 && !pool.isEmpty(); tries++) {
            RelicDef d = pick(pool, rng);
            ItemStack s = RelicCatalog.newStack(d);
            if (!s.isEmpty() && UniqueLedger.issue(level, d.id(), s)) return s;
            pool.remove(d);                                                            // not available after all: try another
        }
        return ItemStack.EMPTY;
    }

    private static RelicDef pick(List<RelicDef> pool, RandomSource rng) {
        double total = 0;
        for (RelicDef d : pool) total += d.grade().weight();
        double r = rng.nextDouble() * total;
        for (RelicDef d : pool) {
            r -= d.grade().weight();
            if (r <= 0) return d;
        }
        return pool.get(pool.size() - 1);
    }

    // ---------------------------------------------------------------- mimics
    private static void onDrops(LivingDropsEvent e) {
        if (!(e.getEntity().level() instanceof ServerLevel level)) return;
        EntityType<?> type = e.getEntity().getType();
        if (!"mimic".equals(BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath()) && !type.is(MIMIC_TAG)) return;
        Vec3 at = e.getEntity().position();
        RandomSource rng = level.getRandom();
        ItemStack drop = ItemStack.EMPTY;
        if (rng.nextFloat() < CLOAK_CHANCE) {
            RelicDef cloak = Relics.byId(CLOAK_ID);
            ItemStack s = cloak == null ? ItemStack.EMPTY : RelicCatalog.newStack(cloak);
            if (!s.isEmpty() && UniqueLedger.issue(level, CLOAK_ID, s)) drop = s;
        }
        if (drop.isEmpty() && rng.nextFloat() < MIMIC_CHANCE) drop = rollUnissued(level, rng, Grade.RARE, Grade.LEGEND);
        if (!drop.isEmpty()) e.getDrops().add(new ItemEntity(level, at.x, at.y + 0.3, at.z, drop));
    }
}
