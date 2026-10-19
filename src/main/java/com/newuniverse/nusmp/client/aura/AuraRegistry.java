package com.newuniverse.nusmp.client.aura;

/** 0.53: generated: registers every attribute's aura painter. */
final class AuraRegistry {
    private AuraRegistry() {}

    static void registerAll() {
        BeastAura.register();
        DemonBeastAura.register();
        BodyAura.register();
        BloodAura.register();
        BoneAura.register();
        EyeAura.register();
        EyeballAura.register();
        CurseAura.register();
        CurseWardingAura.register();
        DemonFireAura.register();
        DemonIceAura.register();
        DemonLightAura.register();
        DemonWaterAura.register();
        GravityAura.register();
        SealAura.register();
        BarrierAura.register();
        ImitationAura.register();
        KeyAura.register();
        ChainAura.register();
        ButohAura.register();
        BriarAura.register();
        CherryBlossomAura.register();
        FungusAura.register();
        FoodAura.register();
        CrystalAura.register();
        CorundumAura.register();
        BronzeAura.register();
        CopperAura.register();
        IronAura.register();
        BlackOilAura.register();
        GelAura.register();
        GlassAura.register();
        BubbleAura.register();
        IceAura.register();
        IceWedgeAura.register();
        LegionAura.register();
        LightAura.register();
        PaintingAura.register();
        KotodamaAura.register();
        WorldTreeAura.register();
        DiceAura.register();
        SlashAura.register();
        CompassAura.register();
        MercuryAura.register();
        WikiAura.register();                 // 1.27.0: the 30 wiki magics
        EarthAura.register();                // 1.93.0: Sandstone, Rock, Red Ochre, Stone, Earth, Mineral armours (answers its own styles, hands the rest to WikiAura)
        ShakudoAura.register();              // 1.93.0: the rebuilt armour of Shakudo, Scale, Spike, Nail and Hair Magic
        ScaleAura.register();
        SpikeAura.register();
        NailAura.register();
        HairAura.register();
        SongAura.register();                 // 1.93.0: the rebuilt Song, Sound, Soul, Soul Corpse and Memory armours
        SoundAura.register();
        SoulAura.register();
        SoulCorpseAura.register();
        MemoryAura.register();
        SmokeAura.register();                // 1.93.0: the rebuilt armour sets of Smoke, Snow, Wing, Vortex, Air and Ash Magic
        AshAura.register();
        AirAura.register();
        VortexAura.register();
        WingAura.register();
        SnowAura.register();
        SpatialAura.register();              // 1.93.0 magic rebuild (Spatial, Gravity, Permeation, Mirror, Slash): the new armours
        MirrorAura.register();
        PermeationAura.register();
    }
}
