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
 * Why: the owner asks for a {@code GlobalLootModifier} that adds anime-dungeon loot (functional foods, pre-rolled gear, monster parts) to what monsters drop, and that is the
 * right tool for the same reason it is for the chest gear of 1.55.0: it works on the loot that has ALREADY been rolled, whatever rolled it. A monster of another mod, one
 * whose table a data pack edited, a monster that has no table at all, a Foundry mob: the modifier sees the list of stacks and the context of the kill, and adds beside what
 * the table gave (add the new, keep the old). It extends {@link LootModifier}, so the JSON carries the standard {@code conditions} list; the shipped entry has none, because
 * the place rules are code ({@link LootDrops}: the dungeon dimensions, or the mod's own monsters) and a condition list cannot say "a monster of ours". The decision is the
 * pure {@link KillLoot}; this class is the shell. The codec is registered on the mod bus by {@link LootRegistry#init} and switched on by the entry
 * {@code nusmp:anime_loot} in {@code data/neoforge/loot_modifiers/global_loot_modifiers.json}. A fault is logged once and the loot goes through unchanged: a loot roll must
 * never fail because of a goblin ear.
 */
public final class AnimeLootModifier extends LootModifier {
    private static final Logger LOG = LogUtils.getLogger();
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    public static final MapCodec<AnimeLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, AnimeLootModifier::new));

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, NUSMP.MODID);

    /** {@code nusmp:anime_loot}, the type id the JSON names. */
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<AnimeLootModifier>> TYPE = MODIFIERS.register("anime_loot", () -> CODEC);

    public static void register(IEventBus modBus) { MODIFIERS.register(modBus); }

    public AnimeLootModifier(LootItemCondition[] conditions) { super(conditions); }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        try {
            LootDrops.add(generatedLoot, context);
        } catch (RuntimeException | LinkageError ex) {
            if (WARNED.compareAndSet(false, true)) LOG.warn("[nusmp] The anime loot modifier failed once and is skipped for that kill", ex);
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() { return CODEC; }
}
