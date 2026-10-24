package com.newuniverse.nusmp.ultimate;

import com.newuniverse.nusmp.ultimate.UltimateDef.Debuff;

import java.util.List;

/**
 * Why: group 2 of the Ultimates of the remaining attributes (docs/magic/ultimate_audit.md section "proposals", rows 25 to 48 of the table, in table order; built by the g12 builder, documented in
 * docs/magic/ultimate_g12.md). One file per group so four builders can add rows without touching the same lines. Pure data (no Minecraft types), the same row format as the eight core rows of
 * {@link UltimateRegistry}: id, book, magic, name, element, origin, wind-up, radius, damage, debuffs, fire / freeze / push (negative = pulled in) / lift, three picture powers, three existing {@code VfxShape}s
 * (never a TIME_ shape). Every row is ORIGINAL: the canon Ultimates of these attributes (Licht's Conquering Eon for Sword, Mimosa's Flower Princess Utopia for Plant) already exist as pages and are kept.
 * Rows are only ever appended at the END of the list.
 */
final class UltimateDefs2 {
    private UltimateDefs2() {}

    private static List<Debuff> d(Debuff... l) { return List.of(l); }

    private static Debuff slow(int t, int a) { return new Debuff("minecraft:slowness", t, a); }

    private static Debuff weak(int t, int a) { return new Debuff("minecraft:weakness", t, a); }

    private static Debuff fatigue(int t, int a) { return new Debuff("minecraft:mining_fatigue", t, a); }

    private static final List<UltimateDef> ROWS = List.of(
            new UltimateDef("ultimate_steel", "book_steel", "STEEL", "Ultimate Steel Magic: Iron Fortress Fall", "steel", "original", 170, 14.0, 44f,
                    d(slow(160, 2), weak(140, 1), fatigue(220, 2)), 0, 0, 1.2, 0.0, 14f, 11f, 5f, "SEAL_CUBE", "EARTH_CLAWS", "WEAPON_CONSTRUCTS"),
            new UltimateDef("ultimate_thread", "book_thread", "THREAD", "Ultimate Thread Magic: Crimson Loom of Fate", "thread", "original", 160, 17.0, 30f,
                    d(slow(220, 2), weak(220, 1), fatigue(120, 1)), 0, 0, -0.4, 0.0, 17f, 6f, 2f, "THREAD_WEB", "THREAD_COCOON", "ROUGE_CAT"),
            new UltimateDef("ultimate_transmutation", "book_transmutation", "TRANSMUTATION", "Ultimate Transmutation Magic: Alchemical Reversal", "transmutation", "original", 180, 16.0, 26f,
                    d(weak(400, 3), slow(120, 1), fatigue(200, 1), new Debuff("minecraft:nausea", 100, 0)), 0, 0, 0.0, 0.0, 16f, 10f, 8f, "TRANSMUTATION_FX2", "TRANSMUTATION_FX3", "ALCHEMY_CIRCLE"),
            new UltimateDef("ultimate_ash", "book_ash", "ASH", "Ultimate Ash Magic: Cinder Apocalypse", "ash", "original", 190, 19.0, 34f,
                    d(new Debuff("minecraft:blindness", 140, 0), new Debuff("minecraft:wither", 120, 1), weak(140, 0)), 5, 0, 0.5, 0.4, 19f, 10f, 7f, "ASH_FX2", "ASH_FX3", "ASH_FX3"),
            new UltimateDef("ultimate_cotton", "book_cotton", "COTTON", "Ultimate Cotton Magic: Cloud Sheep Dreamland", "cotton", "original", 150, 16.0, 16f,
                    d(slow(240, 3), weak(260, 2), fatigue(200, 1)), 0, 0, -0.2, 0.3, 10f, 10f, 8f, "ELF_CIRCLE", "COTTON_SHEEP", "DREAM_TRANSITION"),
            new UltimateDef("ultimate_recombination", "book_recombination", "RECOMBINATION", "Ultimate Recombination Magic: Molecular Rewrite", "recombination", "original", 170, 14.0, 32f,
                    d(weak(300, 2), fatigue(240, 2), slow(100, 1)), 0, 0, 0.8, 0.3, 14f, 6f, 6f, "RECOMBINATION_FX2", "RECOMBINATION_FX3", "RECOMBINE_CONSTRUCT"),
            new UltimateDef("ultimate_painting", "book_painting", "PAINTING", "Ultimate Painting Magic: The Masterpiece Canvas", "painting", "original", 180, 17.0, 34f,
                    d(slow(120, 1), new Debuff("minecraft:blindness", 80, 0), new Debuff("minecraft:nausea", 120, 0), weak(140, 1)), 0, 0, 0.0, 0.0, 16f, 6f, 5f, "PAINT_SPLAT", "PAINT_BEAST", "PAINT_PALETTE"),
            new UltimateDef("ultimate_worldtree", "book_world_tree", "WORLD_TREE", "Ultimate World Tree Magic: Yggdrasil Awakening", "worldtree", "original", 210, 19.0, 36f,
                    d(slow(200, 2), new Debuff("minecraft:poison", 160, 1), weak(200, 1)), 0, 0, -0.3, 0.5, 17f, 12f, 10f, "TREE_ROOTS", "TREE_CANOPY", "TREE_ROOTS"),
            new UltimateDef("ultimate_dice", "book_dice", "DICE", "Ultimate Dice Magic: All-or-Nothing Jackpot", "dice", "original", 130, 15.0, 36f,
                    d(new Debuff("minecraft:unluck", 400, 1), weak(200, 1), slow(100, 1), new Debuff("minecraft:nausea", 100, 0)), 0, 0, 0.5, 0.2, 9f, 12f, 5f, "ELF_CIRCLE", "DICE_D20", "DICE_D6"),
            new UltimateDef("ultimate_slash", "book_slash", "SLASH", "Ultimate Slash Magic: Dimension-Cleaving Heaven", "slash", "original", 140, 17.0, 42f,
                    d(slow(100, 1), weak(160, 1), new Debuff("minecraft:wither", 100, 0)), 0, 0, 0.5, 0.0, 17f, 8f, 12f, "SLASH_FX2", "SLASH_FX3", "SLASH_SCYTHE"),
            new UltimateDef("ultimate_compass", "book_compass", "COMPASS", "Ultimate Compass Magic: Lodestar Rift", "compass", "original", 160, 18.0, 30f,
                    d(slow(180, 2), weak(140, 1), new Debuff("minecraft:glowing", 240, 0), new Debuff("minecraft:nausea", 100, 0)), 0, 0, -2.2, 0.0, 18f, 10f, 6f, "SEAL_BASIC", "SPACE_RINGGATE", "SEAL_ORBIT"),
            new UltimateDef("ultimate_kotodama", "book_kotodama", "KOTODAMA", "Ultimate Kotodama: Word That Ends the World", "kotodama", "original", 220, 16.0, 34f,
                    d(slow(240, 3), fatigue(240, 3), weak(120, 1)), 0, 0, 0.0, 0.0, 2.5f, 8f, 2.5f, "KOTO_AURA", "KOTO_SHATTER", "KOTO_AURA"),
            new UltimateDef("ultimate_game", "book_game", "GAME", "Ultimate Game Magic: Final Boss Raid", "game", "original", 150, 17.0, 38f,
                    d(slow(100, 1), weak(160, 1), new Debuff("minecraft:unluck", 300, 1), new Debuff("minecraft:glowing", 300, 0)), 0, 0, 0.0, 0.0, 17f, 6f, 8f, "GAME_BOARD", "LIGHT_FLARE", "GAME_BOARD"),
            new UltimateDef("ultimate_demonbeast", "book_demon_beast", "DEMON_BEAST", "Ultimate Demon Beast Magic: Hellhound Stampede", "demonbeast", "original", 170, 17.0, 40f,
                    d(new Debuff("minecraft:wither", 160, 1), slow(140, 2), weak(160, 1)), 3, 0, 0.6, 0.2, 17f, 8f, 5f, "DEMON_BEAST_FX2", "DEMON_BEAST_FX3", "DEMON_BEAST_FX3"),
            new UltimateDef("ultimate_body", "book_body", "BODY", "Ultimate Body Magic: Overdrive Apex", "body", "original", 120, 12.0, 46f,
                    d(weak(240, 2), slow(60, 1), fatigue(160, 1)), 0, 0, 2.5, 0.5, 12f, 7f, 4f, "BODY_FX2", "BODY_FX3", "BODY_FX3"),
            new UltimateDef("ultimate_eye", "book_eye", "EYE", "Ultimate Eye Magic: Gaze of the Thousand Eyes", "eye", "original", 160, 20.0, 30f,
                    d(new Debuff("minecraft:glowing", 400, 0), new Debuff("minecraft:blindness", 100, 0), weak(180, 1), slow(60, 0)), 0, 0, 0.0, 0.0, 20f, 8f, 5f, "EYE_FX2", "EYE_FX3", "EYE_FX3"),
            new UltimateDef("ultimate_eyeball", "book_eyeball", "EYEBALL", "Ultimate Eyeball Magic: Argus Vigil", "eyeball", "original", 180, 18.0, 34f,
                    d(new Debuff("minecraft:glowing", 300, 0), weak(160, 1), new Debuff("minecraft:nausea", 100, 0), new Debuff("minecraft:blindness", 60, 0)), 0, 0, 0.0, 0.0, 18f, 7f, 4f, "EYEBALL_FX2", "EYEBALL_FX3", "EYEBALL_FX3"),
            new UltimateDef("ultimate_curse", "book_curse", "CURSE", "Ultimate Curse Magic: Doom of the Seven Generations", "curse", "original", 200, 17.0, 32f,
                    d(new Debuff("minecraft:wither", 240, 2), weak(300, 2), new Debuff("minecraft:unluck", 400, 2), slow(80, 1)), 0, 0, 0.0, 0.0, 17f, 8f, 5f, "CURSE_FX2", "CURSE_FX3", "CURSE_FX3"),
            new UltimateDef("ultimate_cursewarding", "book_curse_warding", "CURSE_WARDING", "Ultimate Curse Warding: Great Purification", "cursewarding", "original", 190, 16.0, 20f,
                    d(weak(240, 1), slow(120, 1), new Debuff("minecraft:glowing", 240, 0)), 0, 0, 0.0, 0.2, 16f, 10f, 6f, "CURSE_WARDING_FX2", "CURSE_WARDING_FX3", "CURSE_WARDING_FX3"),
            new UltimateDef("ultimate_demonfire", "book_demon_fire", "DEMON_FIRE", "Ultimate Demon Fire: Hellfire Throne", "demonfire", "original", 170, 17.0, 42f,
                    d(new Debuff("minecraft:wither", 140, 1), weak(160, 1), slow(80, 1)), 10, 0, 0.3, 0.4, 17f, 8f, 5f, "DEMON_FIRE_FX2", "DEMON_FIRE_FX3", "DEMON_FIRE_FX3"),
            new UltimateDef("ultimate_demonice", "book_demon_ice", "DEMON_ICE", "Ultimate Demon Ice: Cocytus Descent", "demonice", "original", 180, 16.0, 34f,
                    d(new Debuff("minecraft:wither", 120, 1), slow(180, 3), weak(140, 1)), 0, 280, 0.0, 0.0, 16f, 8f, 5f, "DEMON_ICE_FX2", "DEMON_ICE_FX3", "DEMON_ICE_FX3"),
            new UltimateDef("ultimate_demonlight", "book_demon_light", "DEMON_LIGHT", "Ultimate Demon Light: Fallen Dawn", "demonlight", "original", 180, 19.0, 36f,
                    d(new Debuff("minecraft:blindness", 140, 0), new Debuff("minecraft:wither", 140, 1), weak(200, 1), new Debuff("minecraft:glowing", 200, 0)), 0, 0, -0.4, 0.3, 19f, 8f, 5f, "DEMON_LIGHT_FX2", "DEMON_LIGHT_FX3", "DEMON_LIGHT_FX3"),
            new UltimateDef("ultimate_demonwater", "book_demon_water", "DEMON_WATER", "Ultimate Demon Water: Styx Flood", "demonwater", "original", 180, 18.0, 36f,
                    d(new Debuff("minecraft:nausea", 200, 0), new Debuff("minecraft:wither", 120, 1), slow(160, 2), weak(120, 1)), 0, 0, 1.2, 0.0, 18f, 8f, 5f, "DEMON_WATER_FX2", "DEMON_WATER_FX3", "DEMON_WATER_FX3"),
            new UltimateDef("ultimate_barrier", "book_barrier", "BARRIER", "Ultimate Barrier Magic: Absolute Cage", "barrier", "original", 190, 15.0, 24f,
                    d(slow(220, 2), weak(200, 2), fatigue(140, 1)), 0, 0, -1.5, 0.0, 15f, 7f, 4f, "BARRIER_FX2", "BARRIER_FX3", "BARRIER_FX3")
    );

    static List<UltimateDef> all() { return ROWS; }
}
