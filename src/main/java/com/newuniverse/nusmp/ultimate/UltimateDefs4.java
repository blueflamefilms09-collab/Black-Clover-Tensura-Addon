package com.newuniverse.nusmp.ultimate;

import com.newuniverse.nusmp.ultimate.UltimateDef.Debuff;

import java.util.List;

/**
 * Why: group 4 of the Ultimates of the remaining attributes (docs/magic/ultimate_audit.md section "proposals", rows 73 to 96, in table order; the full table with sources is docs/magic/ultimate_g34.md). One file per
 * group so builders add rows without touching the same lines. Pure data (no Minecraft types); every row stays inside the bounds of {@link UltimateRules}, uses only existing {@code VfxShape} constants (never a Time shape)
 * and vanilla potion effects, and is ORIGINAL: no per-attribute canon Ultimate Magic of these attributes was found on the English wiki or the Japanese pages (see the doc). Rows are only ever appended at the END.
 */
final class UltimateDefs4 {
    private UltimateDefs4() {}

    private static List<Debuff> d(Debuff... l) { return List.of(l); }

    private static Debuff slow(int t, int a) { return new Debuff("minecraft:slowness", t, a); }

    private static Debuff weak(int t, int a) { return new Debuff("minecraft:weakness", t, a); }

    private static Debuff fatigue(int t, int a) { return new Debuff("minecraft:mining_fatigue", t, a); }

    private static final List<UltimateDef> ROWS = List.of(
            new UltimateDef("ultimate_mud", "book_mud", "MUD", "Ultimate Mud Magic: Swamp of Despair", "mud", "original", 180, 19.0, 26.0f,
                    d(slow(260, 3), fatigue(260, 2), weak(140, 1)), 0, 0, -0.5, 0.0, 19.0f, 3.5f, 9.0f, "MUD_FX2", "MUD_FX3", "MUD_FX2"),
            new UltimateDef("ultimate_nail", "book_nail", "NAIL", "Ultimate Nail Magic: Iron Maiden Rain", "nail", "original", 130, 13.0, 46.0f,
                    d(new Debuff("minecraft:wither", 100, 0), slow(100, 1), weak(160, 1)), 0, 0, 0.2, 0.0, 13.0f, 3.5f, 3.0f, "NAIL_FX2", "NAIL_FX3", "NAIL_FX3"),
            new UltimateDef("ultimate_permeation", "book_permeation", "PERMEATION", "Ultimate Permeation Magic: Phase Through All", "permeation", "original", 140, 12.0, 54.0f,
                    d(weak(240, 2), new Debuff("minecraft:darkness", 120, 0), slow(80, 1)), 0, 0, 0.0, 0.0, 12.0f, 3.5f, 3.0f, "PERMEATION_FX2", "PERMEATION_FX3", "PERMEATION_FX3"),
            new UltimateDef("ultimate_poisonplant", "book_poison_plant", "POISON_PLANT", "Ultimate Poison Plant Magic: Venom Bloom", "poison plant", "original", 170, 16.0, 32.0f,
                    d(new Debuff("minecraft:poison", 260, 2), new Debuff("minecraft:wither", 120, 1), slow(100, 1)), 0, 0, 0.0, 0.0, 16.0f, 3.5f, 8.0f, "POISON_PLANT_FX2", "POISON_PLANT_FX3", "POISON_PLANT_FX2"),
            new UltimateDef("ultimate_redochre", "book_red_ochre", "RED_OCHRE", "Ultimate Red Ochre Magic: Crimson Earth Rite", "red ochre", "original", 180, 15.0, 34.0f,
                    d(weak(300, 2), slow(140, 2), new Debuff("minecraft:glowing", 300, 0)), 0, 0, 0.8, 0.4, 15.0f, 4.0f, 3.0f, "EARTHFAM_ZONE", "EARTHFAM_BURST", "EARTHFAM_BURST"),
            new UltimateDef("ultimate_rock", "book_rock", "ROCK", "Ultimate Rock Magic: Mountain Fall", "rock", "original", 200, 10.0, 52.0f,
                    d(slow(100, 2), fatigue(160, 2), weak(120, 1)), 0, 0, 2.4, 0.0, 10.0f, 6.5f, 5.0f, "EARTHFAM_ZONE", "EARTHFAM_BURST", "EARTHFAM_BURST"),
            new UltimateDef("ultimate_sandstone", "book_sandstone", "SANDSTONE", "Ultimate Sandstone Magic: Pharaoh's Tomb Collapse", "sandstone", "original", 180, 16.0, 36.0f,
                    d(slow(160, 2), new Debuff("minecraft:blindness", 100, 0), fatigue(200, 2)), 0, 0, -0.6, 0.0, 16.0f, 4.0f, 8.0f, "EARTHFAM_ZONE", "EARTHFAM_BURST", "EARTHFAM_ZONE"),
            new UltimateDef("ultimate_scale", "book_scale", "SCALE", "Ultimate Scale Magic: Judgement Scale", "scale", "original", 160, 14.0, 40.0f,
                    d(weak(300, 2), slow(100, 1), new Debuff("minecraft:glowing", 200, 0)), 0, 0, 0.6, 0.0, 14.0f, 3.5f, 3.0f, "SCALE_FX2", "SCALE_FX3", "SCALE_FX3"),
            new UltimateDef("ultimate_shakudo", "book_shakudo", "SHAKUDO", "Ultimate Shakudo Magic: Black Gold Flame", "shakudo", "original", 150, 14.0, 44.0f,
                    d(new Debuff("minecraft:wither", 100, 1), weak(160, 1), new Debuff("minecraft:blindness", 40, 0)), 10, 0, 0.6, 0.0, 14.0f, 3.5f, 3.0f, "SHAKUDO_FX2", "SHAKUDO_FX3", "SHAKUDO_FX3"),
            new UltimateDef("ultimate_skin", "book_skin", "SKIN", "Ultimate Skin Magic: Second Skin Rend", "skin", "original", 140, 13.0, 38.0f,
                    d(weak(260, 2), new Debuff("minecraft:wither", 140, 1), slow(100, 1)), 0, 0, 0.3, 0.0, 13.0f, 3.5f, 3.0f, "WIKI_MAGIC_FIELD", "WIKI_MAGIC_BURST", "WIKI_MAGIC_BURST"),
            new UltimateDef("ultimate_smoke", "book_smoke", "SMOKE", "Ultimate Smoke Magic: Great Burning Veil", "smoke", "original", 140, 19.0, 28.0f,
                    d(new Debuff("minecraft:blindness", 160, 0), slow(120, 1), weak(120, 1)), 5, 0, 0.4, 0.2, 19.0f, 3.5f, 8.0f, "SMOKE_FX2", "SMOKE_FX3", "SMOKE_FX2"),
            new UltimateDef("ultimate_snow", "book_snow", "SNOW", "Ultimate Snow Magic: Whiteout Blizzard", "snow", "original", 170, 21.0, 30.0f,
                    d(slow(160, 2), new Debuff("minecraft:blindness", 120, 0), weak(120, 1)), 0, 280, 0.5, 0.0, 21.0f, 3.5f, 10.0f, "SNOW_FX2", "SNOW_FX3", "SNOW_FX2"),
            new UltimateDef("ultimate_song", "book_song", "SONG", "Ultimate Song Magic: Requiem of the World", "song", "original", 200, 22.0, 32.0f,
                    d(slow(120, 2), weak(200, 2), new Debuff("minecraft:nausea", 100, 0)), 0, 0, 0.0, 0.0, 22.0f, 3.5f, 10.0f, "SONG_FX2", "SONG_FX3", "SONG_RING"),
            new UltimateDef("ultimate_soulcorpse", "book_soul_corpse", "SOUL_CORPSE", "Ultimate Soul Corpse Magic: Legion of the Dead", "soul corpse", "original", 190, 17.0, 34.0f,
                    d(new Debuff("minecraft:wither", 180, 1), slow(140, 2), weak(160, 1), new Debuff("minecraft:darkness", 100, 0)), 0, 0, -0.7, 0.0, 17.0f, 3.5f, 2.5f, "SOUL_CORPSE_FX2", "SOUL_CORPSE_FX3", "CORPSE_RAISE"),
            new UltimateDef("ultimate_soul", "book_soul", "SOUL", "Ultimate Soul Magic: Soul Harvest", "soul", "original", 180, 18.0, 36.0f,
                    d(new Debuff("minecraft:wither", 200, 1), weak(240, 2), slow(60, 1)), 0, 0, -1.0, 0.0, 18.0f, 3.5f, 8.0f, "SOUL_FX2", "SOUL_FX3", "SOUL_VORTEX"),
            new UltimateDef("ultimate_sound", "book_sound", "SOUND", "Ultimate Sound Magic: Resonance Collapse", "sound", "original", 110, 21.0, 38.0f,
                    d(new Debuff("minecraft:nausea", 200, 0), weak(120, 1), slow(80, 1)), 0, 0, 2.1, 0.2, 21.0f, 3.5f, 3.0f, "SOUND_FX2", "SOUND_FX3", "SOUND_FX3"),
            new UltimateDef("ultimate_spike", "book_spike", "SPIKE", "Ultimate Spike Magic: Forest of Spears", "spike", "original", 140, 13.0, 46.0f,
                    d(slow(100, 2), weak(100, 1), fatigue(140, 1)), 0, 0, 0.3, 0.65, 13.0f, 3.5f, 7.0f, "SPIKE_FX2", "SPIKE_FX3", "SPIKE_FX2"),
            new UltimateDef("ultimate_switching", "book_switching", "SWITCHING", "Ultimate Switching Magic: Total Swap", "switching", "original", 160, 16.0, 24.0f,
                    d(new Debuff("minecraft:nausea", 240, 0), new Debuff("minecraft:blindness", 60, 0), slow(80, 1), weak(160, 1)), 0, 0, -1.2, 0.45, 16.0f, 5.0f, 4.0f, "WIKI_MAGIC_FIELD", "SPACE_RINGGATE", "SPACE_CRACK"),
            new UltimateDef("ultimate_tongue", "book_tongue", "TONGUE", "Ultimate Tongue Magic: Great Toad Lash", "tongue", "original", 110, 15.0, 40.0f,
                    d(slow(80, 1), weak(200, 1), new Debuff("minecraft:hunger", 160, 1)), 0, 0, -2.2, 0.0, 15.0f, 3.5f, 2.5f, "WIKI_MAGIC_FIELD", "WIKI_MAGIC_BURST", "WIKI_MAGIC_CAST"),
            new UltimateDef("ultimate_tree", "book_tree", "TREE", "Ultimate Tree Magic: Ancient Grove Wrath", "tree", "original", 200, 19.0, 40.0f,
                    d(slow(160, 2), weak(180, 1), fatigue(160, 1)), 0, 0, 1.0, 0.6, 19.0f, 3.5f, 9.0f, "TREE_FX2", "TREE_FX3", "TREE_FX2"),
            new UltimateDef("ultimate_stone", "book_stone", "STONE", "Ultimate Stone Magic: Monolith Rain", "stone", "original", 170, 14.0, 48.0f,
                    d(slow(120, 2), fatigue(120, 2), weak(100, 1)), 0, 0, 1.5, 0.0, 14.0f, 3.0f, 5.0f, "EARTHFAM_ZONE", "BOSS_STONE_COLUMN", "EARTHFAM_BURST"),
            new UltimateDef("ultimate_vine", "book_vine", "VINE", "Ultimate Vine Magic: Jungle Strangle", "vine", "original", 170, 16.0, 24.0f,
                    d(slow(260, 3), new Debuff("minecraft:poison", 160, 1), weak(140, 1)), 0, 0, -0.8, 0.0, 16.0f, 3.5f, 8.0f, "VINE_FX2", "VINE_FX3", "VINE_FX2"),
            new UltimateDef("ultimate_vortex", "book_vortex", "VORTEX", "Ultimate Vortex Magic: Maelstrom Abyss", "vortex", "original", 150, 20.0, 34.0f,
                    d(new Debuff("minecraft:nausea", 200, 0), slow(60, 1), weak(120, 1)), 0, 0, -2.4, 0.4, 20.0f, 3.5f, 10.0f, "VORTEX_FX2", "VORTEX_FX3", "VORTEX_FX2"),
            new UltimateDef("ultimate_wing", "book_wing", "WING", "Ultimate Wing Magic: Skyfall Feathers", "wing", "original", 130, 18.0, 38.0f,
                    d(new Debuff("minecraft:levitation", 50, 0), new Debuff("minecraft:blindness", 60, 0), weak(160, 1)), 0, 0, 0.9, 0.85, 18.0f, 3.5f, 3.0f, "WING_FX2", "WING_FX3", "WING_FX3"));

    static List<UltimateDef> all() { return ROWS; }
}
