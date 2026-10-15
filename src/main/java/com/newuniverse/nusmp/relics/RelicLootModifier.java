package com.newuniverse.nusmp.relics;

import com.mojang.serialization.MapCodec;
import com.newuniverse.nusmp.NUSMP;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * A NeoForge global loot modifier, {@code nusmp:relic_chests} (enabled by data/neoforge/loot_modifiers/global_loot_modifiers.json): whenever a loot
 * table whose path contains "chests/" and a dungeon-like word (dungeon, vault, stronghold, bastion, ancient_city, trial) is rolled, there is a small
 * chance an unissued relic is added. Because it works on the rolled loot it behaves the same in a plain chest and in Lootr's per-player containers
 * (Lootr rolls the table once per opening player), and the ledger makes sure no two rolls give the same relic.
 * 1.35.0: with {@code labyrinth.relicsExclusive} on (the default) the modifier first asks {@code labyrinth.vault.RelicExclusivity}: only
 * {@code nusmp:chests/dungeon_relic_vault} and {@code nusmp:chests/labyrinth_pedestal} pass, so the bonus relic lands in the Labyrinth's vault
 * only; with it off, the behaviour above is unchanged. (The Socket cores are not relics and are added before the gate as before.)
 */
public final class RelicLootModifier implements IGlobalLootModifier {
    public static final MapCodec<RelicLootModifier> CODEC = MapCodec.unit(RelicLootModifier::new);

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, NUSMP.MODID);
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<RelicLootModifier>> TYPE = MODIFIERS.register("relic_chests", () -> CODEC);

    /** Registers the modifier type (mod bus). */
    public static void register(IEventBus modBus) { MODIFIERS.register(modBus); }

    /** The words of a chest table's path that mark the loot as worth a relic, and what range and chance each carries. */
    static Tier tierOf(String path) {
        if (path == null || !path.contains("chests/")) return null;
        if (path.contains("ancient_city") || path.contains("bastion") || path.contains("trial") || path.contains("vault")) return new Tier(Grade.RARE, Grade.LEGEND, 0.05f);
        if (path.contains("stronghold")) return new Tier(Grade.SPECIAL, Grade.UNIQUE, 0.04f);
        if (path.contains("dungeon")) return new Tier(Grade.NORMAL, Grade.UNIQUE, 0.03f);
        return null;
    }

    record Tier(Grade min, Grade max, float chance) {}

    @Override
    public ObjectArrayList<ItemStack> apply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        try {
            ResourceLocation table = context.getQueriedLootTableId();
            if (table != null) com.newuniverse.nusmp.relics.socket.SocketLoot.addToChest(generatedLoot, table.getPath(), context.getRandom());     // 1.29.0: a Spirit or Devil Core, at a low weight
            if (!com.newuniverse.nusmp.labyrinth.vault.RelicExclusivity.allowsChest(table)) return generatedLoot;      // 1.35.0: relics only in the Labyrinth's vault tables (config relicsExclusive)
            Tier tier = table == null ? null : tierOf(table.getPath());
            if (tier == null || context.getRandom().nextFloat() >= tier.chance()) return generatedLoot;
            ServerLevel level = context.getLevel();
            generatedLoot.addAll(RelicLoot.rollUnissuedAll(level, context.getRandom(), tier.min(), tier.max()));      // a pair adds both halves
        } catch (RuntimeException | LinkageError ignored) {
            // loot must never fail because of a relic
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() { return CODEC; }
}
