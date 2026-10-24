package com.newuniverse.nusmp.ultimate;

import com.newuniverse.nusmp.ultimate.UltimateDef.Debuff;

import java.util.List;

/**
 * Why: group 1 of the Ultimates of the remaining attributes (docs/magic/ultimate_audit.md section "proposals", rows 1 to 24 of the table, in table order; built by the g12 builder, documented in
 * docs/magic/ultimate_g12.md). One file per group so four builders can add rows without touching the same lines. Pure data (no Minecraft types), the same row format as the eight core rows of
 * {@link UltimateRegistry}: id, book, magic, name, element, origin, wind-up, radius, damage, debuffs, fire / freeze / push (negative = pulled in) / lift, three picture powers, three existing {@code VfxShape}s
 * (never a TIME_ shape). Every row is ORIGINAL: the canon Ultimates of these attributes (Licht's Conquering Eon for Sword, Mimosa's Flower Princess Utopia for Plant) already exist as pages and are kept.
 * Rows are only ever appended at the END of the list.
 */
final class UltimateDefs1 {
    private UltimateDefs1() {}

    private static List<Debuff> d(Debuff... l) { return List.of(l); }

    private static Debuff slow(int t, int a) { return new Debuff("minecraft:slowness", t, a); }

    private static Debuff weak(int t, int a) { return new Debuff("minecraft:weakness", t, a); }

    private static Debuff fatigue(int t, int a) { return new Debuff("minecraft:mining_fatigue", t, a); }

    private static final List<UltimateDef> ROWS = List.of(
            new UltimateDef("ultimate_explosion", "book_explosion", "EXPLOSION", "Ultimate Explosion Magic: Dawn Detonation", "explosion", "original", 150, 17.0, 42f,
                    d(new Debuff("minecraft:nausea", 240, 0), slow(60, 1), weak(140, 0), new Debuff("minecraft:blindness", 40, 0)), 4, 0, 2.3, 0.6, 14f, 8f, 6f, "FIRE_WILD", "FLAME_EXPLOSION", "FIRE_BURST"),
            new UltimateDef("ultimate_magma", "book_magma", "MAGMA", "Ultimate Magma Magic: Planet's Core Eruption", "magma", "original", 190, 15.0, 40f,
                    d(slow(180, 2), weak(160, 1), fatigue(120, 1)), 12, 0, 0.3, 0.9, 15f, 7f, 5f, "EARTHFAM_ZONE", "FIRE_PILLAR", "FLAME_EXPLOSION"),
            new UltimateDef("ultimate_ice", "book_ice", "ICE", "Ultimate Ice Magic: Absolute Zero Coffin", "ice", "original", 170, 16.0, 30f,
                    d(slow(200, 3), weak(200, 1), fatigue(160, 1)), 0, 300, 0.0, 0.0, 16f, 7f, 4f, "ICE_FX2", "ICE_FX3", "ICE_FX3"),
            new UltimateDef("ultimate_mercury", "book_mercury", "MERCURY", "Ultimate Mercury Magic: Silver Tide Requiem", "mercury", "original", 160, 18.0, 32f,
                    d(slow(220, 3), weak(260, 1), fatigue(100, 0)), 0, 0, -1.0, 0.1, 17f, 12f, 6f, "MERCURY_FX2", "MERCURY_FX3", "MERCURY_DOME"),
            new UltimateDef("ultimate_mist", "book_mist", "MIST", "Ultimate Mist Magic: Veil of the Thousand Fogs", "mist", "original", 140, 20.0, 26f,
                    d(new Debuff("minecraft:blindness", 160, 0), new Debuff("minecraft:nausea", 180, 0), weak(140, 0), slow(80, 0)), 0, 0, 0.2, 0.1, 18f, 10f, 6f, "SOUL_VORTEX", "MIST_MAGIC", "MIST_MAGIC"),
            new UltimateDef("ultimate_star", "book_star", "STAR", "Ultimate Star Magic: Stellar Rain of Ruin", "star", "original", 170, 19.0, 40f,
                    d(new Debuff("minecraft:glowing", 400, 0), slow(80, 1), weak(200, 1), new Debuff("minecraft:blindness", 40, 0)), 0, 0, 0.6, 0.3, 17f, 12f, 3f, "BORN_NOVA", "STAR_MAGIC", "LIGHT_FLARE"),
            new UltimateDef("ultimate_storm", "book_storm", "STORM", "Ultimate Storm Magic: Heaven's Wrath Typhoon", "storm", "original", 130, 19.0, 36f,
                    d(new Debuff("minecraft:levitation", 30, 0), slow(100, 1), weak(180, 1), new Debuff("minecraft:nausea", 100, 0)), 0, 0, 1.2, 0.7, 17f, 5f, 3f, "WATER_NEST", "LIGHTNING_GOD", "WIND_RING"),
            new UltimateDef("ultimate_plant", "book_plant", "PLANT", "Ultimate Plant Magic: Forest of the World", "plant", "original", 190, 16.0, 30f,
                    d(new Debuff("minecraft:poison", 200, 1), slow(240, 3), fatigue(160, 1)), 0, 0, -0.4, 0.0, 16f, 7f, 4f, "PLANT_FX2", "PLANT_FX3", "PLANT_FX3"),
            new UltimateDef("ultimate_sand", "book_sand", "SAND", "Ultimate Sand Magic: Desert Sovereign's Grave", "sand", "original", 180, 17.0, 34f,
                    d(new Debuff("minecraft:blindness", 160, 0), slow(160, 2), fatigue(240, 1)), 0, 0, -0.8, 0.0, 16f, 12f, 8f, "ASH_FORMATION", "SAND_MAGIC", "SAND_MAGIC"),
            new UltimateDef("ultimate_sword", "book_sword", "SWORD", "Ultimate Sword Magic: Thousand Blades Heaven", "sword", "original", 150, 15.0, 38f,
                    d(new Debuff("minecraft:wither", 120, 0), slow(80, 1), weak(160, 1)), 0, 0, 0.3, 0.2, 4f, 14f, 8f, "WEAPON_CONSTRUCTS", "KOTO_SWORDS", "KOTO_SWORDS"),
            new UltimateDef("ultimate_shadow", "book_shadow", "SHADOW", "Ultimate Shadow Magic: Endless Night Banquet", "shadow", "original", 170, 16.0, 34f,
                    d(new Debuff("minecraft:darkness", 200, 0), slow(140, 2), new Debuff("minecraft:wither", 100, 0), weak(120, 0)), 0, 0, -0.6, 0.0, 16f, 10f, 8f, "SHADOW_POOL", "SHADOW_UNITE", "SHADOW_POOL"),
            new UltimateDef("ultimate_poison", "book_poison", "POISON", "Ultimate Poison Magic: Ten Thousand Venoms", "poison", "original", 160, 17.0, 28f,
                    d(new Debuff("minecraft:poison", 300, 2), new Debuff("minecraft:mining_fatigue", 300, 1), weak(240, 1), new Debuff("minecraft:nausea", 100, 0)), 0, 0, 0.1, 0.0, 4f, 12f, 6f, "KOTO_SLUDGE", "POISON_BREATH", "POISON_CURTAIN"),
            new UltimateDef("ultimate_mirror", "book_mirror", "MIRROR", "Ultimate Mirror Magic: Infinite Reflection Hall", "mirror", "original", 150, 15.0, 34f,
                    d(new Debuff("minecraft:blindness", 120, 0), new Debuff("minecraft:nausea", 200, 0), weak(160, 1), new Debuff("minecraft:glowing", 200, 0)), 0, 0, 0.0, 0.2, 15f, 7f, 6f, "MIRROR_FX2", "MIRROR_FX3", "MIRROR_SHATTER"),
            new UltimateDef("ultimate_gravity", "book_gravity", "GRAVITY", "Ultimate Gravity Magic: Collapse of the Star", "gravity", "original", 200, 18.0, 44f,
                    d(slow(220, 3), weak(200, 2), fatigue(240, 2)), 0, 0, -2.0, 0.0, 18f, 12f, 14f, "GRAVITY_WELL", "GRAVITY_SINGULARITY", "GRAVITY_IMPACT"),
            new UltimateDef("ultimate_sealing", "book_sealing", "SEALING", "Ultimate Sealing Magic: Grand Seal of Ages", "sealing", "original", 210, 14.0, 14f,
                    d(slow(240, 3), weak(300, 2), fatigue(300, 2)), 0, 0, 0.0, 0.0, 14f, 6f, 5f, "SEALING_FX2", "SEALING_FX3", "SEAL_ORBIT"),
            new UltimateDef("ultimate_reinforcement", "book_reinforcement", "REINFORCEMENT", "Ultimate Reinforcement Magic: Titan's Final Fist", "reinforcement", "original", 140, 12.0, 48f,
                    d(weak(300, 2), slow(80, 1), fatigue(200, 1)), 0, 0, 2.5, 0.35, 5f, 2f, 1.5f, "SPIRIT_AURA", "FX_SPIRIT_NOVA", "FX_SPIRIT_NOVA"),
            new UltimateDef("ultimate_beast", "book_beast", "BEAST", "Ultimate Beast Magic: King of the Hunt", "beast", "original", 150, 16.0, 38f,
                    d(new Debuff("minecraft:wither", 140, 0), slow(120, 2), weak(160, 1)), 0, 0, -0.5, 0.2, 16f, 7f, 5f, "BEAST_FX2", "BEAST_FX3", "BEAST_FX3"),
            new UltimateDef("ultimate_bone", "book_bone", "BONE", "Ultimate Bone Magic: Ossuary Cathedral", "bone", "original", 180, 15.0, 36f,
                    d(slow(160, 2), weak(140, 1), fatigue(200, 2)), 0, 0, 0.0, 0.5, 15f, 7f, 5f, "BONE_FX2", "BONE_FX3", "BONE_FX3"),
            new UltimateDef("ultimate_blood", "book_blood", "BLOOD", "Ultimate Blood Magic: Crimson Sea Reaping", "blood", "original", 170, 17.0, 36f,
                    d(new Debuff("minecraft:wither", 200, 1), new Debuff("minecraft:mining_fatigue", 300, 1), weak(200, 1), slow(100, 1)), 0, 0, -0.5, 0.0, 17f, 12f, 9f, "BLOOD_SIGIL", "BLOOD_DOWNPOUR", "BLOOD_SIGIL"),
            new UltimateDef("ultimate_creation", "book_creation", "CREATION", "Ultimate Creation Magic: Genesis Armoury", "creation", "original", 160, 16.0, 40f,
                    d(slow(100, 1), weak(180, 1), new Debuff("minecraft:glowing", 240, 0), fatigue(120, 1)), 0, 0, 0.4, 0.0, 14f, 8f, 8f, "ALCHEMY_CIRCLE", "WEAPON_CONSTRUCTS", "DREAM_MANIFEST"),
            new UltimateDef("ultimate_copy", "book_copy", "COPY", "Ultimate Copy Magic: Mirror of Every Art", "copy", "original", 150, 15.0, 34f,
                    d(weak(240, 2), fatigue(200, 1), new Debuff("minecraft:glowing", 300, 0), slow(80, 1)), 0, 0, 0.0, 0.0, 14f, 7f, 3f, "SEAL_BASIC", "MIRROR_SHATTER", "MIRROR_FRAME"),
            new UltimateDef("ultimate_illusion", "book_illusion", "ILLUSION", "Ultimate Illusion Magic: World of False Dreams", "illusion", "original", 170, 18.0, 20f,
                    d(new Debuff("minecraft:blindness", 140, 0), new Debuff("minecraft:nausea", 240, 0), weak(140, 0), new Debuff("minecraft:darkness", 100, 0)), 0, 0, 0.0, 0.15, 18f, 12f, 4f, "DREAM_TRANSITION", "DREAM_MANIFEST", "MIRROR_DOUBLE"),
            new UltimateDef("ultimate_dream", "book_dream", "DREAM", "Ultimate Dream Magic: Eternal Nightmare", "dream", "original", 200, 18.0, 22f,
                    d(new Debuff("minecraft:darkness", 240, 0), weak(240, 2), slow(160, 2), new Debuff("minecraft:blindness", 60, 0)), 0, 0, 0.0, 0.0, 18f, 12f, 6f, "DREAM_DOME", "DREAM_SHATTER", "DREAM_MANIFEST"),
            new UltimateDef("ultimate_antimagic", "book_anti_magic", "ANTI_MAGIC", "Ultimate Anti-Magic: Null Zone", "antimagic", "original", 180, 15.0, 14f,
                    d(weak(400, 3), fatigue(300, 2), slow(100, 1)), 0, 0, 0.0, 0.0, 15f, 7f, 8f, "NIHILITY_ZONE", "ANTI_MAGIC_SLASH", "NIHILITY_ZONE")
    );

    static List<UltimateDef> all() { return ROWS; }
}
