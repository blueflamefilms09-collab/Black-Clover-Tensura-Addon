package com.newuniverse.nusmp.ultimate;

import com.newuniverse.nusmp.ultimate.UltimateDef.Debuff;

import java.util.List;

/**
 * Why: group 3 of the Ultimates of the remaining attributes (docs/magic/ultimate_audit.md section "proposals", rows 49 to 72, in table order; the full table with sources is docs/magic/ultimate_g34.md). One file per
 * group so builders add rows without touching the same lines. Pure data (no Minecraft types); every row stays inside the bounds of {@link UltimateRules}, uses only existing {@code VfxShape} constants (never a Time shape)
 * and vanilla potion effects, and is ORIGINAL: no per-attribute canon Ultimate Magic of these attributes was found on the English wiki or the Japanese pages (see the doc). Rows are only ever appended at the END.
 */
final class UltimateDefs3 {
    private UltimateDefs3() {}

    private static List<Debuff> d(Debuff... l) { return List.of(l); }

    private static Debuff slow(int t, int a) { return new Debuff("minecraft:slowness", t, a); }

    private static Debuff weak(int t, int a) { return new Debuff("minecraft:weakness", t, a); }

    private static Debuff fatigue(int t, int a) { return new Debuff("minecraft:mining_fatigue", t, a); }

    private static final List<UltimateDef> ROWS = List.of(
            new UltimateDef("ultimate_key", "book_key", "KEY", "Ultimate Key Magic: Gate of the Last Lock", "key", "original", 130, 14.0, 26.0f,
                    d(slow(100, 3), fatigue(100, 3), weak(200, 1), new Debuff("minecraft:unluck", 300, 1)), 0, 0, -0.3, 0.0, 14.0f, 4.2f, 5.0f, "KEY_FX2", "KEY_FX3", "KEY_FX2"),
            new UltimateDef("ultimate_chain", "book_chain", "CHAIN", "Ultimate Chain Magic: Prison of a Thousand Links", "chain", "original", 150, 16.0, 34.0f,
                    d(slow(140, 2), weak(260, 2), fatigue(200, 1)), 0, 0, -0.8, 0.0, 16.0f, 4.8f, 2.9f, "CHAIN_FX2", "CHAIN_FX3", "CHAIN_FX3"),
            new UltimateDef("ultimate_butoh", "book_butoh", "BUTOH", "Ultimate Butoh Magic: Dance of the Final Curtain", "butoh", "original", 120, 15.0, 36.0f,
                    d(new Debuff("minecraft:nausea", 160, 0), slow(80, 1), weak(160, 1)), 0, 0, 1.8, 0.3, 15.0f, 4.5f, 8.0f, "BUTOH_FX2", "BUTOH_FX3", "BUTOH_FX2"),
            new UltimateDef("ultimate_briar", "book_briar", "BRIAR", "Ultimate Briar Magic: Thorn Empress Garden", "briar", "original", 170, 17.0, 30.0f,
                    d(new Debuff("minecraft:poison", 200, 1), slow(140, 2), weak(160, 1)), 0, 0, 0.0, 0.0, 17.0f, 5.1f, 8.0f, "BRIAR_FX2", "BRIAR_FX3", "BRIAR_FX2"),
            new UltimateDef("ultimate_cherryblossom", "book_cherry_blossom", "CHERRY_BLOSSOM", "Ultimate Cherry Blossom Magic: Ten Thousand Petals", "blossom", "original", 140, 18.0, 40.0f,
                    d(new Debuff("minecraft:blindness", 120, 0), weak(160, 1), new Debuff("minecraft:nausea", 100, 0)), 0, 0, 0.6, 0.55, 18.0f, 5.4f, 3.2f, "CHERRY_BLOSSOM_FX2", "CHERRY_BLOSSOM_FX3", "CHERRY_BLOSSOM_FX3"),
            new UltimateDef("ultimate_fungus", "book_fungus", "FUNGUS", "Ultimate Fungus Magic: Spore Apocalypse", "fungus", "original", 180, 19.0, 26.0f,
                    d(new Debuff("minecraft:poison", 240, 1), new Debuff("minecraft:nausea", 200, 0), weak(240, 1)), 0, 0, 0.2, 0.1, 19.0f, 5.7f, 9.0f, "FUNGUS_FX2", "FUNGUS_FX3", "FUNGUS_FX2"),
            new UltimateDef("ultimate_food", "book_food", "FOOD", "Ultimate Food Magic: Grand Banquet of Ruin", "food", "original", 190, 16.0, 24.0f,
                    d(new Debuff("minecraft:mining_fatigue", 300, 3), new Debuff("minecraft:nausea", 160, 0), weak(200, 1)), 0, 0, -0.5, 0.0, 16.0f, 5.0f, 3.0f, "FOOD_FX2", "FOOD_MAW", "FOOD_FX3"),
            new UltimateDef("ultimate_crystal", "book_crystal", "CRYSTAL", "Ultimate Crystal Magic: Prism Cathedral", "crystal", "original", 150, 15.0, 42.0f,
                    d(new Debuff("minecraft:glowing", 300, 0), new Debuff("minecraft:blindness", 60, 0), slow(80, 1), weak(160, 1)), 0, 0, 1.0, 0.35, 15.0f, 4.5f, 3.0f, "CRYSTAL_FX2", "CRYSTAL_FX3", "CRYSTAL_FX3"),
            new UltimateDef("ultimate_corundum", "book_corundum", "CORUNDUM", "Ultimate Corundum Magic: Ruby Sun Cutter", "corundum", "original", 130, 13.0, 46.0f,
                    d(weak(240, 2), slow(100, 1), new Debuff("minecraft:glowing", 200, 0)), 6, 0, 0.7, 0.2, 13.0f, 3.9f, 3.0f, "CORUNDUM_FX2", "CORUNDUM_FX3", "CORUNDUM_FX3"),
            new UltimateDef("ultimate_bronze", "book_bronze", "BRONZE", "Ultimate Bronze Magic: Colossus of the Old Age", "bronze", "original", 190, 12.0, 44.0f,
                    d(slow(160, 2), weak(200, 1), fatigue(200, 2)), 0, 0, 2.2, 0.25, 12.0f, 4.0f, 3.0f, "BRONZE_FX2", "BRONZE_FX3", "BRONZE_FX3"),
            new UltimateDef("ultimate_copper", "book_copper", "COPPER", "Ultimate Copper Magic: Conduit Overload", "copper", "original", 120, 17.0, 36.0f,
                    d(slow(100, 2), weak(240, 1), new Debuff("minecraft:blindness", 40, 0)), 3, 0, 0.5, 0.1, 17.0f, 5.1f, 3.1f, "COPPER_FX2", "COPPER_FX3", "COPPER_FX3"),
            new UltimateDef("ultimate_iron", "book_iron", "IRON", "Ultimate Iron Magic: Meteor Anvil", "iron", "original", 170, 11.0, 48.0f,
                    d(slow(120, 2), weak(180, 1), fatigue(160, 1)), 0, 0, 1.7, 0.0, 11.0f, 4.0f, 3.5f, "IRON_FX2", "IRON_FX3", "IRON_FX3"),
            new UltimateDef("ultimate_blackoil", "book_black_oil", "BLACK_OIL", "Ultimate Black Oil Magic: Tar Ocean Ignition", "black oil", "original", 170, 17.0, 38.0f,
                    d(slow(200, 3), weak(160, 1), new Debuff("minecraft:blindness", 80, 0)), 14, 0, 0.0, 0.0, 17.0f, 5.1f, 8.0f, "BLACK_OIL_FX2", "BLACK_OIL_FX3", "BLACK_OIL_FX2"),
            new UltimateDef("ultimate_gel", "book_gel", "GEL", "Ultimate Gel Magic: Great Slime Engulf", "gel", "original", 150, 14.0, 22.0f,
                    d(slow(240, 3), fatigue(240, 2), weak(160, 1)), 0, 0, -0.6, 0.0, 14.0f, 4.2f, 7.0f, "GEL_FX2", "GEL_FX3", "GEL_FX2"),
            new UltimateDef("ultimate_glass", "book_glass", "GLASS", "Ultimate Glass Magic: Shattered Sky", "glass", "original", 140, 18.0, 44.0f,
                    d(new Debuff("minecraft:blindness", 100, 0), slow(60, 1), weak(160, 1)), 0, 0, 0.9, 0.3, 18.0f, 5.4f, 3.2f, "GLASS_FX2", "GLASS_FX3", "GLASS_FX3"),
            new UltimateDef("ultimate_bubble", "book_bubble", "BUBBLE", "Ultimate Bubble Magic: Million Bubble Prison", "bubble", "original", 160, 15.0, 20.0f,
                    d(new Debuff("minecraft:levitation", 60, 0), slow(140, 2), weak(160, 1)), 0, 0, -0.4, 0.9, 15.0f, 4.5f, 7.0f, "BUBBLE_FX2", "BUBBLE_FX3", "BUBBLE_FX2"),
            new UltimateDef("ultimate_icewedge", "book_ice_wedge", "ICE_WEDGE", "Ultimate Ice Wedge Magic: Glacier Spear Barrage", "wedge", "original", 150, 16.0, 42.0f,
                    d(slow(120, 2), weak(160, 1), fatigue(120, 1)), 0, 260, 0.9, 0.0, 16.0f, 4.8f, 3.0f, "ICE_WEDGE_FX2", "ICE_WEDGE_FX3", "ICE_WEDGE_FX3"),
            new UltimateDef("ultimate_legion", "book_legion", "LEGION", "Ultimate Legion Magic: Army of the Chessboard", "legion", "original", 200, 20.0, 36.0f,
                    d(slow(100, 2), weak(200, 2), new Debuff("minecraft:glowing", 300, 0)), 0, 0, 1.1, 0.0, 20.0f, 6.0f, 3.6f, "LEGION_FX2", "LEGION_FX3", "LEGION_FX3"),
            new UltimateDef("ultimate_air", "book_air", "AIR", "Ultimate Air Magic: Pressure Collapse", "air", "original", 130, 18.0, 36.0f,
                    d(weak(240, 2), slow(80, 1), new Debuff("minecraft:nausea", 100, 0)), 0, 0, -1.4, 0.0, 18.0f, 5.4f, 3.2f, "AIR_FX2", "AIR_FX3", "AIR_FX3"),
            new UltimateDef("ultimate_hair", "book_hair", "HAIR", "Ultimate Hair Magic: Thousand Strand Binding", "hair", "original", 150, 14.0, 28.0f,
                    d(slow(180, 2), weak(160, 1), fatigue(160, 1)), 0, 0, -0.7, 0.0, 14.0f, 4.2f, 6.0f, "HAIR_FX2", "HAIR_FX3", "HAIR_FX2"),
            new UltimateDef("ultimate_memory", "book_memory", "MEMORY", "Ultimate Memory Magic: Total Recall Erasure", "memory", "original", 190, 20.0, 14.0f,
                    d(fatigue(300, 3), weak(300, 2), new Debuff("minecraft:nausea", 160, 0)), 0, 0, 0.0, 0.0, 20.0f, 6.0f, 3.6f, "MEMORY_FX2", "MEMORY_FX3", "MEMORY_FX3"),
            new UltimateDef("ultimate_mineral", "book_mineral", "MINERAL", "Ultimate Mineral Magic: Vein Eruption", "mineral", "original", 170, 15.0, 42.0f,
                    d(slow(120, 2), weak(160, 1), fatigue(200, 2)), 0, 0, 0.9, 0.55, 15.0f, 4.5f, 3.0f, "EARTHFAM_ZONE", "EARTHFAM_BURST", "EARTHFAM_BURST"),
            new UltimateDef("ultimate_modification", "book_modification", "MODIFICATION", "Ultimate Modification Magic: Absolute Adjustment", "modification", "original", 200, 17.0, 18.0f,
                    d(slow(200, 3), weak(300, 3), fatigue(200, 2)), 0, 0, 0.0, 0.0, 17.0f, 5.1f, 3.1f, "MODIFICATION_FX2", "MODIFICATION_FX3", "MODIFICATION_FX3"),
            new UltimateDef("ultimate_mucus", "book_mucus", "MUCUS", "Ultimate Mucus Magic: Slime Tide", "mucus", "original", 160, 17.0, 24.0f,
                    d(slow(240, 3), weak(160, 1), new Debuff("minecraft:nausea", 120, 0)), 0, 0, 0.9, 0.0, 17.0f, 5.1f, 8.0f, "MUCUS_FX2", "MUCUS_FX3", "MUCUS_FX2"));

    static List<UltimateDef> all() { return ROWS; }
}
