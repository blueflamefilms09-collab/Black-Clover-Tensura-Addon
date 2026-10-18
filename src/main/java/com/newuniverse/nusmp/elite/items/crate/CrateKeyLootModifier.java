package com.newuniverse.nusmp.elite.items.crate;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.elite.items.EliteItems;
import com.newuniverse.nusmp.elite.items.EliteItemsConfig;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Why: puts crate keys into dungeon loot the way the Skill Books got into the rift chests: a Global Loot Modifier that works on the loot that has ALREADY been rolled, so every chest keeps everything it
 * gave and a key is ADDED beside it. Its numbers are DATA ({@code data/nusmp/loot_modifiers/elite_crate_keys.json}): for each loot table the chance of a Common, a Rare and an Elite key, so a datapack
 * can add a table or retune a number with no code; the operator's config scales all of them at once ({@code keyChanceScale}) and has a MASTER SWITCH ({@code keyInjection}, off = no key is ever
 * added). The decision is {@link KeyRules} (pure, tested). Server side; an exception is logged once and the loot goes through unchanged: a loot roll must never fail because of a key.
 */
public final class CrateKeyLootModifier extends LootModifier {
    private static final Logger LOG = LogUtils.getLogger();
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    private static final Codec<Map<String, Double>> CHANCES = Codec.unboundedMap(Codec.STRING, Codec.doubleRange(0.0, 1.0));

    public static final MapCodec<CrateKeyLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst)
            .and(Codec.unboundedMap(ResourceLocation.CODEC, CHANCES).fieldOf("tables").forGetter(m -> m.raw))
            .apply(inst, CrateKeyLootModifier::new));

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, NUSMP.MODID);

    /** {@code nusmp:elite_crate_keys}, the type id the JSON names. */
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<CrateKeyLootModifier>> TYPE = MODIFIERS.register("elite_crate_keys", () -> CODEC);

    public static void register(IEventBus modBus) { MODIFIERS.register(modBus); }

    private final Map<ResourceLocation, Map<String, Double>> raw;
    private final Map<String, Map<String, Double>> tables;

    public CrateKeyLootModifier(LootItemCondition[] conditions, Map<ResourceLocation, Map<String, Double>> tables) {
        super(conditions);
        this.raw = Map.copyOf(tables);
        Map<String, Map<String, Double>> byString = new HashMap<>();
        for (Map.Entry<ResourceLocation, Map<String, Double>> e : tables.entrySet()) byString.put(e.getKey().toString(), Map.copyOf(e.getValue()));
        this.tables = Map.copyOf(byString);
    }

    /** The tables this modifier pays for and their per-tier chances, by id string. */
    public Map<String, Map<String, Double>> tables() { return tables; }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        try {
            if (!EliteItemsConfig.enabled() || !EliteItemsConfig.get(EliteItemsConfig.CRATE_KEY_INJECTION)) return generatedLoot;
            ResourceLocation table = context.getQueriedLootTableId();
            if (table == null) return generatedLoot;
            Map<String, Double> perTier = tables.get(table.toString());
            if (perTier == null) return generatedLoot;
            for (CrateTier t : KeyRules.roll(perTier, EliteItemsConfig.get(EliteItemsConfig.CRATE_KEY_SCALE), context.getRandom()::nextDouble)) {
                ItemStack key = new ItemStack(EliteItems.key(t).get());
                if (!key.isEmpty()) generatedLoot.add(key);
            }
        } catch (RuntimeException | LinkageError ex) {
            if (WARNED.compareAndSet(false, true)) LOG.warn("[nusmp] the crate key loot modifier failed once and is skipped for that roll", ex);
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() { return CODEC; }
}
