package com.newuniverse.nusmp.labyrinth.gen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The wood-family suffix rule of the palette processor (pure, no Minecraft). */
class PaletteSwapTest {
    @Test
    void everyVanillaShapeMapsIntoTheFamily() {
        assertEquals("minecraft:spruce_planks", PaletteSwap.wood("minecraft:dark_oak_planks", "minecraft:spruce_planks"));
        assertEquals("minecraft:spruce_fence", PaletteSwap.wood("minecraft:dark_oak_fence", "minecraft:spruce_planks"));
        assertEquals("minecraft:spruce_stairs", PaletteSwap.wood("minecraft:dark_oak_stairs", "minecraft:spruce_planks"));
        assertEquals("minecraft:spruce_slab", PaletteSwap.wood("minecraft:dark_oak_slab", "minecraft:spruce_planks"));
        assertEquals("minecraft:spruce_fence_gate", PaletteSwap.wood("minecraft:dark_oak_fence_gate", "minecraft:spruce_planks"));
        assertEquals("minecraft:spruce_trapdoor", PaletteSwap.wood("minecraft:dark_oak_trapdoor", "minecraft:spruce_planks"));
        assertEquals("minecraft:spruce_door", PaletteSwap.wood("minecraft:dark_oak_door", "minecraft:spruce_planks"));
        assertEquals("minecraft:spruce_button", PaletteSwap.wood("minecraft:dark_oak_button", "minecraft:spruce_planks"));
        assertEquals("minecraft:spruce_pressure_plate", PaletteSwap.wood("minecraft:dark_oak_pressure_plate", "minecraft:spruce_planks"));
    }

    @Test
    void fenceGateIsNotReadAsFence() {
        assertEquals("_fence_gate", PaletteSwap.suffix("minecraft:dark_oak_fence_gate"));
        assertEquals("_fence", PaletteSwap.suffix("minecraft:dark_oak_fence"));
        assertEquals("_trapdoor", PaletteSwap.suffix("minecraft:dark_oak_trapdoor"));
        assertEquals("_door", PaletteSwap.suffix("minecraft:dark_oak_door"));
        assertEquals("_pressure_plate", PaletteSwap.suffix("minecraft:dark_oak_pressure_plate"));
    }

    @Test
    void theFamilyKeepsTheWoodsNamespace() {
        assertEquals("biomesoplenty:hellbark_fence", PaletteSwap.wood("minecraft:dark_oak_fence", "biomesoplenty:hellbark_planks"));
        assertEquals("biomesoplenty:hellbark_planks", PaletteSwap.wood("minecraft:dark_oak_planks", "biomesoplenty:hellbark_planks"));
        assertEquals("biomesoplenty:hellbark", PaletteSwap.stem("biomesoplenty:hellbark_planks"));
    }

    @Test
    void aNonPlanksWoodIsItsOwnStem() {
        assertEquals("minecraft:bamboo_mosaic", PaletteSwap.stem("minecraft:bamboo_mosaic"));
        assertEquals("minecraft:bamboo_mosaic_stairs", PaletteSwap.wood("minecraft:dark_oak_stairs", "minecraft:bamboo_mosaic"));
        assertEquals("minecraft:bamboo_mosaic", PaletteSwap.wood("minecraft:dark_oak_planks", "minecraft:bamboo_mosaic"));
        assertEquals("minecraft:bamboo_mosaic_fence", PaletteSwap.wood("minecraft:dark_oak_fence", "minecraft:bamboo_mosaic"), "a candidate the processor's registry check will reject and keep the original for");
    }

    @Test
    void aBlockWithoutAWoodShapeIsNotSwapped() {
        assertNull(PaletteSwap.suffix("minecraft:stone_bricks"));
        assertNull(PaletteSwap.wood("minecraft:stone_bricks", "minecraft:spruce_planks"));
        assertNull(PaletteSwap.wood("minecraft:dark_oak_log", "minecraft:spruce_planks"));
        assertNull(PaletteSwap.wood(null, "minecraft:spruce_planks"));
        assertNull(PaletteSwap.wood("minecraft:dark_oak_fence", null));
        assertNull(PaletteSwap.wood("minecraft:dark_oak_fence", ""));
        assertNull(PaletteSwap.suffix(null));
        assertNull(PaletteSwap.stem(null));
    }

    @Test
    void theSameFamilyIsAFixedPoint() {
        assertEquals("minecraft:dark_oak_fence", PaletteSwap.wood("minecraft:dark_oak_fence", "minecraft:dark_oak_planks"));
        assertEquals("minecraft:dark_oak_planks", PaletteSwap.wood("minecraft:dark_oak_planks", "minecraft:dark_oak_planks"));
    }
}
