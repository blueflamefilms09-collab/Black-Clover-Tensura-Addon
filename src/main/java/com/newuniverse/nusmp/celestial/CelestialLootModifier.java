package com.newuniverse.nusmp.celestial;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Why: Celestial Keys are rare loot in the dungeon's vaults and chests. As with the gear of the Mega-Dungeon, a global loot modifier works on the loot that has already been
 * rolled (a plain chest, a Lootr container, a table another pack adds), adds AT MOST ONE key beside what the table gave and removes nothing. It applies in the Mega-Dungeon only
 * (the JSON carries the dimension condition, the code asks again), the chance is {@link CelestialRules#dropChance} for a vault table ({@code relic_vault}, {@code treasure}, {@code vault}
 * in the path) or any other chest, and {@link CelestialRules#pickKey} picks which key, preferring one the opener does not own. Switched on by the entry
 * {@code nusmp:celestial_keys} in data/neoforge/loot_modifiers/global_loot_modifiers.json.
 */
public final class CelestialLootModifier extends LootModifier {
    private static final Logger LOG = LogUtils.getLogger();
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    public static final MapCodec<CelestialLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, CelestialLootModifier::new));

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, NUSMP.MODID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<CelestialLootModifier>> TYPE = MODIFIERS.register("celestial_keys", () -> CODEC);

    public static void register(IEventBus modBus) { MODIFIERS.register(modBus); }

    public CelestialLootModifier(LootItemCondition[] conditions) { super(conditions); }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        try {
            add(generatedLoot, context);
        } catch (RuntimeException | LinkageError ex) {
            if (WARNED.compareAndSet(false, true)) LOG.warn("[nusmp] celestial key loot failed once and is skipped for that roll", ex);
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() { return CODEC; }

    /** Which source a chest table is: a vault, or any other chest table ("chests/" in the path); null when it is no chest table. */
    public static CelestialRules.Source sourceOf(ResourceLocation table) {
        if (table == null) return null;
        String p = table.getPath();
        if (!p.startsWith("chests/")) return null;
        return p.contains("vault") || p.contains("treasure") || p.contains("relic") ? CelestialRules.Source.VAULT_CHEST : CelestialRules.Source.CHEST;
    }

    /** The whole decision. Returns the number of keys added: 0 or 1. */
    public static int add(ObjectArrayList<ItemStack> loot, LootContext context) {
        ServerLevel level = context.getLevel();
        if (level == null || !MegaDungeon.in(level)) return 0;
        CelestialRules.Source src = sourceOf(context.getQueriedLootTableId());
        if (src == null) return 0;
        Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
        int floor = origin == null ? 0 : MegaDungeon.floorAt(level, BlockPos.containing(origin));
        int luck = (int) Math.round(context.getLuck());
        if (context.getRandom().nextDouble() >= CelestialRules.dropChance(src, floor, luck)) return 0;
        int owned = context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof ServerPlayer opener ? CelestialAttachments.of(opener).owned() : 0;
        CelestialKey key = CelestialRules.pickKey(context.getRandom().nextDouble(), owned, floor);
        if (key == null) return 0;
        loot.add(CelestialItems.stack(key));
        return 1;
    }
}
