package com.newuniverse.nusmp.dungeon.nexus;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Turns the block names and property strings of a plan ({@link NexusOp}) into block states, remembering each. A block that is not
 * registered (a mod that is not installed) becomes stone bricks and is logged once; a property the block does not have, or a value it
 * does not take, is ignored. Server thread only.
 */
public final class NexusStates {
    private NexusStates() {}

    private static final Map<String, BlockState> CACHE = new HashMap<>();
    private static final Set<String> MISSING = new HashSet<>();
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    public static BlockState of(String block, String props) {
        String key = props.isEmpty() ? block : block + "[" + props + "]";
        BlockState s = CACHE.get(key);
        if (s == null) {
            s = resolve(block, props);
            CACHE.put(key, s);
        }
        return s;
    }

    private static BlockState resolve(String block, String props) {
        ResourceLocation id = ResourceLocation.tryParse(block);
        Block b = id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
        if (b == null) {
            if (MISSING.add(block)) LOG.warn("[nusmp] A Nexus plan names the block {}, which is not registered; stone bricks stand in for it", block);
            return Blocks.STONE_BRICKS.defaultBlockState();
        }
        BlockState s = b.defaultBlockState();
        for (String[] kv : BlockProps.parse(props)) {
            Property<?> p = b.getStateDefinition().getProperty(kv[0]);
            if (p != null) s = with(s, p, kv[1]);
        }
        return s;
    }

    private static <T extends Comparable<T>> BlockState with(BlockState s, Property<T> p, String value) {
        Optional<T> v = p.getValue(value);
        return v.isPresent() ? s.setValue(p, v.get()) : s;
    }
}
