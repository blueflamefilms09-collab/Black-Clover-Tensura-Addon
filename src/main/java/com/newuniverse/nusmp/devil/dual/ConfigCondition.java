package com.newuniverse.nusmp.devil.dual;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.newuniverse.nusmp.devil.DevilConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Why: a data file (the recipe of the craftable Devil Contract, its advancements) must be switchable by a SERVER setting, and NeoForge conditions have no config condition of their own. This one is
 * {@code {"type": "anime_multiverse:config", "key": "devil.contractCraftable"}}; an unknown key is false (the file is skipped, never a crash). The setting is read when the data packs load (world start and
 * {@code /reload}), so a change in the server config applies at the next of those. Pure lookup, no state.
 */
public record ConfigCondition(String key) implements ICondition {
    public static final MapCodec<ConfigCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(Codec.STRING.fieldOf("key").forGetter(ConfigCondition::key)).apply(i, ConfigCondition::new));

    /** The answer for a key; false for a key nobody knows. */
    public static boolean value(String key) {
        if ("arc.arenas".equals(key)) return com.newuniverse.nusmp.arc.world.ArcWorldConfig.arenas();      // 5.7.0: the Chronicle Sigil recipe exists only while the story arenas do
        return "devil.contractCraftable".equals(key) && DevilConfig.contractCraftable();
    }

    @Override public boolean test(IContext context) { return value(key); }

    @Override public MapCodec<? extends ICondition> codec() { return CODEC; }

    private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS = DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, "anime_multiverse");

    public static void register(IEventBus modBus) {
        CONDITIONS.register("config", () -> CODEC);
        CONDITIONS.register(modBus);
    }
}
