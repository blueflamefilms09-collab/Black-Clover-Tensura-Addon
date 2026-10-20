package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: a plan says "wall here, light there"; the palette is the pocket's answer to what a wall and a light are made of. It resolves the block ids of the definition once, when the tomb is made, and
 * keeps the ones the registry does not know (a typo in a datapack, a block of an absent mod) in a list instead of failing: such a block is replaced by stone bricks so the tomb still stands, and the
 * self-test {@code rift.pockets} fails loudly with the id, which is how a bad file is found without ever crashing a server. The seal is always crying obsidian: it is the one block every tomb
 * shares, because it is the one thing a delver learns to read.
 */
public final class PocketPalette {
    private final BlockState wall, floor, ceiling, pillar, accent, light, glass, liquid, seal, air;
    private final List<String> unresolved = new ArrayList<>();

    private PocketPalette(PocketDef.Palette p) {
        wall = resolve(p.wall());
        floor = resolve(p.floor());
        ceiling = resolve(p.ceiling());
        pillar = resolve(p.pillar());
        accent = resolve(p.accent());
        light = resolve(p.light());
        glass = resolve(p.glass());
        liquid = resolve(p.liquid());
        seal = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        air = Blocks.AIR.defaultBlockState();
    }

    public static PocketPalette of(PocketDef d) { return new PocketPalette(d.palette()); }

    /** The block ids of the definition that the registry does not know. */
    public List<String> unresolved() { return unresolved; }

    private BlockState resolve(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl != null) {
            var found = BuiltInRegistries.BLOCK.getOptional(rl);
            if (found.isPresent() && found.get() != Blocks.AIR) return found.get().defaultBlockState();
        }
        unresolved.add(id);
        return Blocks.STONE_BRICKS.defaultBlockState();
    }

    /** The block of a material role. */
    public BlockState state(PocketPlan.Mat m) {
        return switch (m) {
            case AIR -> air;
            case WALL -> wall;
            case FLOOR -> floor;
            case CEIL -> ceiling;
            case PILLAR -> pillar;
            case ACCENT -> accent;
            case LIGHT -> light;
            case GLASS -> glass;
            case LIQUID -> liquid;
            case SEAL -> seal;
        };
    }

    /** The rune block at the middle of a trap tile of an element (stone bricks if the registry lacks it). */
    public static BlockState rune(PocketElement e) {
        ResourceLocation rl = ResourceLocation.tryParse(e.rune());
        if (rl != null) {
            var found = BuiltInRegistries.BLOCK.getOptional(rl);
            if (found.isPresent()) return found.get().defaultBlockState();
        }
        return Blocks.STONE_BRICKS.defaultBlockState();
    }

    /** Is a state a liquid source of the palette's river? */
    public boolean isLiquid(BlockState s) { return s.getBlock() == liquid.getBlock(); }

    /** A block by registry id, or null (the self-test checks the table of runes and the palettes with it). */
    public static Block block(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? null : BuiltInRegistries.BLOCK.getOptional(rl).orElse(null);
    }
}
