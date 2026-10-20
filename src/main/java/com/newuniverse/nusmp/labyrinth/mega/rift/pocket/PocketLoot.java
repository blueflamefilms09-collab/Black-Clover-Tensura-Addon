package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the registry of the two pieces of loot machinery the hard gate needs: the condition type {@code nusmp:in_pocket} and the pool entry type {@code nusmp:pocket_relic}. They live in the vanilla
 * registries {@code LOOT_CONDITION_TYPE} and {@code LOOT_POOL_ENTRY_TYPE}, so a datapack loot table can name them like {@code minecraft:random_chance} (the pattern of the Treasure Vault's
 * {@code nusmp:relic}). Registered on the mod bus from {@code RiftRegistry.init}.
 */
public final class PocketLoot {
    private PocketLoot() {}

    public static final DeferredRegister<LootItemConditionType> CONDITIONS = DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, NUSMP.MODID);
    public static final DeferredRegister<LootPoolEntryType> ENTRIES = DeferredRegister.create(Registries.LOOT_POOL_ENTRY_TYPE, NUSMP.MODID);

    /** {@code nusmp:in_pocket}: true only inside the live tomb of a pocket, at a stage. */
    public static final DeferredHolder<LootItemConditionType, LootItemConditionType> IN_POCKET = CONDITIONS.register("in_pocket", () -> new LootItemConditionType(InPocketCondition.CODEC));
    /** {@code nusmp:pocket_relic}: one named relic of one tomb. */
    public static final DeferredHolder<LootPoolEntryType, LootPoolEntryType> RELIC_ENTRY = ENTRIES.register("pocket_relic", () -> new LootPoolEntryType(PocketRelicEntry.CODEC));

    public static void init(IEventBus modBus) {
        CONDITIONS.register(modBus);
        ENTRIES.register(modBus);
    }
}
