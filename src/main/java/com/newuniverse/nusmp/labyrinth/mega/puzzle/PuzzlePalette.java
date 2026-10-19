package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: {@link Palettes} names the blocks as text so the pure tests can check them; this is the one place the text becomes a {@link BlockState} ({@code minecraft:lime_glazed_terracotta[facing=east]}),
 * cached, with an unknown block or property falling back to stone or to the block's default state rather than throwing (a typo in a palette must never break a room).
 */
final class PuzzlePalette {
    private PuzzlePalette() {}

    private static final Map<String, BlockState> CACHE = new ConcurrentHashMap<>();

    /** The block of face {@code face} of a palette, with the theme flavour's dark base for the palettes that use one. */
    static BlockState state(String palette, int face, String flavour) {
        String spec = Palettes.block(palette, face);
        if (spec == null) spec = "minecraft:stone";
        if (face == 0 && Palettes.usesBase(palette)) spec = Palettes.base(flavour);
        return CACHE.computeIfAbsent(spec, PuzzlePalette::parse);
    }

    static BlockState parse(String spec) {
        int br = spec.indexOf('[');
        String id = br < 0 ? spec : spec.substring(0, br);
        Block b = Blocks.STONE;
        try {
            Block found = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
            if (found != null) b = found;
        } catch (RuntimeException ignored) {
            // an unknown id stays stone
        }
        BlockState st = b.defaultBlockState();
        if (br >= 0 && spec.endsWith("]")) {
            for (String kv : spec.substring(br + 1, spec.length() - 1).split(",")) {
                int eq = kv.indexOf('=');
                if (eq < 0) continue;
                Property<?> prop = b.getStateDefinition().getProperty(kv.substring(0, eq).trim());
                if (prop != null) st = with(st, prop, kv.substring(eq + 1).trim());
            }
        }
        return st;
    }

    private static <T extends Comparable<T>> BlockState with(BlockState st, Property<T> prop, String value) {
        Optional<T> v = prop.getValue(value);
        return v.isPresent() ? st.setValue(prop, v.get()) : st;
    }
}
