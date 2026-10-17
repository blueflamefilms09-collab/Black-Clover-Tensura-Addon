package com.newuniverse.nusmp.labyrinth.gen;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Map;
import java.util.Optional;

/**
 * Why: when a processor swaps one block for another it must keep what the template said about the block's SHAPE: {@code facing}, {@code half},
 * {@code part}, {@code lit} ... Copying properties by NAME, parsing each value by its serialized name, does that between blocks that do not share a
 * class (a campfire for a stove, a stair-chair for a sofa) and never throws on a property the target lacks or a value it does not know. It lived
 * in the Spire's biome palette processor until 1.58.0; the Spire was removed and the Mega-Dungeon's compat processor ({@code ModCompatProcessor}) still
 * needs it, so it stands here on its own.
 */
public final class StateCarry {
    private StateCarry() {}

    /** Copies every property of {@code from} whose NAME the target block also has, parsing the value by its serialized name. */
    public static BlockState carryProperties(BlockState from, BlockState to) {
        StateDefinition<Block, BlockState> definition = to.getBlock().getStateDefinition();
        BlockState out = to;
        for (Map.Entry<Property<?>, Comparable<?>> e : from.getValues().entrySet()) {
            Property<?> mine = definition.getProperty(e.getKey().getName());
            if (mine == null) continue;
            out = carry(out, e.getKey(), e.getValue(), mine);
        }
        return out;
    }

    private static <A extends Comparable<A>, B extends Comparable<B>> BlockState carry(BlockState out, Property<A> from, Comparable<?> value, Property<B> to) {
        String name = from.getName(from.getValueClass().cast(value));
        Optional<B> parsed = to.getValue(name);
        return parsed.isPresent() ? out.trySetValue(to, parsed.get()) : out;
    }
}
