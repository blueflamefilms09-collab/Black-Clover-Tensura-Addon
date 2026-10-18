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
 * 1.35.0: while {@code labyrinth.relicsExclusive} is on, Mimics drop relics only inside the Labyrinth or the legacy Living Dungeon
 * ({@code labyrinth.vault.RelicExclusivity}); off, the old behaviour above is unchanged.
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
        List<ItemStack> all = rollUnissuedAll(level, rng, min, max);
        return all.isEmpty() ? ItemStack.EMPTY : all.get(0);
    }

    /**
     * The same, as every stack the roll yields: one for a plain relic, BOTH halves for a pair (1.29.0). A pair is rolled only while neither half lives,
     * and counts as one relic in the weights; the left half is never rolled on its own.
     */
    public static List<ItemStack> rollUnissuedAll(ServerLevel level, RandomSource rng, Grade min, Grade max) {
        List<ItemStack> none = new ArrayList<>();
        if (level == null || level.getServer() == null) return none;
        if (!com.newuniverse.nusmp.labyrinth.vault.RelicExclusivity.allowsWildRoll(level, max)) return none;      // 1.54.0: in the Mega-Dungeon a Crucible is the only source of the capped grades; 1.75.0: a roll of Rare or lower (a hidden cache) is allowed
        List<RelicDef> pool = candidates(level, min, max);
        for (int tries = 0; tries < 8 && !pool.isEmpty(); tries++) {
            RelicDef d = pick(pool, rng);
            if (d.hasPair()) {
                List<ItemStack> pair = com.newuniverse.nusmp.relics.pair.PairGrant.issueNewPair(level, d);
                if (!pair.isEmpty()) return pair;
            } else {
                ItemStack s = RelicCatalog.newStack(d);
                if (!s.isEmpty() && UniqueLedger.issue(level, d.id(), s)) {
                    none.add(s);
                    return none;
                }
            }
            pool.remove(d);                                                            // not available after all: try another
        }
        return none;
    }

    /**
     * 1.35.0: whether any relic of the range could still be rolled (the dry run of {@link #rollUnissuedAll}, nothing is stamped). The
     * {@code nusmp:relic} loot entry asks this in {@code expand} so an exhausted range drops out of a pool and a fallback entry can run.
     */
    public static boolean anyUnissued(ServerLevel level, Grade min, Grade max) {
        if (level == null || level.getServer() == null) return false;
        if (!com.newuniverse.nusmp.labyrinth.vault.RelicExclusivity.allowsWildRoll(level, max)) return false;      // 1.54.0 / 1.75.0: see rollUnissuedAll
        return !candidates(level, min, max).isEmpty();
    }

    /**
     * 1.54.0: up to {@code count} DISTINCT relics of the range that nobody has yet, picked by the grades' loot weights, WITHOUT issuing any of them (the
     * ledger is not touched). The Crucible of Choice shows several relics on pedestals and only the one that is taken may be issued: rolling the offer with
     * {@link #rollUnissuedAll} would stamp every offered relic as alive and burn the ones the player did not take. The candidate rules are the same as the
     * roll's (unique, not alive, never the left half of a pair, within the grades); {@code filter} narrows them further (the Crucible drops pairs, which
     * have two halves, and relics already on offer in another vault). Fewer than {@code count} come back when fewer qualify.
     */
    public static List<RelicDef> peekUnissued(ServerLevel level, RandomSource rng, Grade min, Grade max, int count, java.util.function.Predicate<RelicDef> filter) {
        List<RelicDef> out = new ArrayList<>();
        if (level == null || level.getServer() == null || rng == null || min == null || max == null || count <= 0) return out;
        List<RelicDef> pool = candidates(level, min, max);
        if (filter != null) pool.removeIf(d -> !filter.test(d));
        while (out.size() < count && !pool.isEmpty()) {
            RelicDef d = pick(pool, rng);
            out.add(d);
            pool.remove(d);
        }
        return out;
    }

    /** The defs the roll may pick: unique, not alive (neither half of a pair alive), never a left half on its own, within the grades. */
    private static List<RelicDef> candidates(ServerLevel level, Grade min, Grade max) {
        Set<String> live = UniqueLedger.liveIds(level.getServer());
        List<RelicDef> pool = new ArrayList<>();
        for (RelicDef d : Relics.all()) {
            if (d.slot() == RelicSlot.CONSUMABLE || d.isPairLeft()) continue;
            if (d.isUnique() && live.contains(d.id())) continue;                          // 1.68.0: a relic the God of Order's list lets exist many times is never 'alive'
            if (d.isUnique() && d.hasPair() && live.contains(d.pairPartnerId())) continue;
            if (d.grade().ordinal() < min.ordinal() || d.grade().ordinal() > max.ordinal()) continue;
            pool.add(d);
        }
        return pool;
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
        if (!com.newuniverse.nusmp.labyrinth.vault.RelicExclusivity.allowsMobDrop(level)) return;      // 1.35.0: relics only in the dungeons (config relicsExclusive)
        Vec3 at = e.getEntity().position();
        RandomSource rng = level.getRandom();
        ItemStack drop = ItemStack.EMPTY;
        List<ItemStack> drops = new ArrayList<>();
        if (rng.nextFloat() < CLOAK_CHANCE) {
            RelicDef cloak = Relics.byId(CLOAK_ID);
            ItemStack s = cloak == null ? ItemStack.EMPTY : RelicCatalog.newStack(cloak);
            if (!s.isEmpty() && UniqueLedger.issue(level, CLOAK_ID, s)) drop = s;
        }
        if (!drop.isEmpty()) drops.add(drop);
        else if (rng.nextFloat() < MIMIC_CHANCE) drops.addAll(rollUnissuedAll(level, rng, Grade.RARE, Grade.LEGEND));      // a pair drops both halves
        for (ItemStack d : drops) e.getDrops().add(new ItemEntity(level, at.x, at.y + 0.3, at.z, d));
    }
}
