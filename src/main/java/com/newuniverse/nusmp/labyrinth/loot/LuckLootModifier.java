package com.newuniverse.nusmp.labyrinth.loot;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.newuniverse.nusmp.NUSMP;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Why: the Luck-scaled loot has to apply to the loot that has ALREADY been rolled, whoever rolled it (a vanilla table, a Lootr chest, a mod's monster, a data pack's edit, a mob with
 * no table of its own), and a global loot modifier is the door for exactly that: it sees the list of stacks and the context, enlarges and adds beside what the table gave, and
 * replaces nothing (add the new, keep the old). The decisions are {@link LuckGauss}, and {@link LuckArmour}; the work on the context is {@link LuckLoot}; this
 * class is the shell, in the pattern of the anime loot modifier next to it. Registered on the mod bus as {@code nusmp:luck_loot} by {@link LootRegistry#init} and switched on by
 * the entry in {@code data/neoforge/loot_modifiers/global_loot_modifiers.json}. The loot of the modifiers that run before or after it is also seen (the order is the loader's), so it
 * only enlarges what is eligible and never touches gear, coins or keys. A fault is logged once and the loot goes through unchanged: a loot roll must never fail because of a bonus.
 */
public final class LuckLootModifier extends LootModifier {
    private static final Logger LOG = LogUtils.getLogger();
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    public static final MapCodec<LuckLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, LuckLootModifier::new));

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, NUSMP.MODID);

    /** {@code nusmp:luck_loot}, the type id the JSON names. */
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<LuckLootModifier>> TYPE = MODIFIERS.register("luck_loot", () -> CODEC);

    public static void register(IEventBus modBus) { MODIFIERS.register(modBus); }

    public LuckLootModifier(LootItemCondition[] conditions) { super(conditions); }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        try {
            LuckLoot.add(generatedLoot, context);
        } catch (RuntimeException | LinkageError ex) {
            if (WARNED.compareAndSet(false, true)) LOG.warn("[nusmp] The Luck loot modifier failed once and is skipped for that roll", ex);
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() { return CODEC; }
}
