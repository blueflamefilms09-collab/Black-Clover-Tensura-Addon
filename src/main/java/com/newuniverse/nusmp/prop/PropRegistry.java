package com.newuniverse.nusmp.prop;

/** 0.53: generated: registers every attribute's prop behaviours. */
final class PropRegistry {
    private PropRegistry() {}

    static void registerAll() {
        BeastProps.init();
        DemonBeastProps.init();
        BodyProps.init();
        BloodProps.init();
        BoneProps.init();
        EyeProps.init();
        EyeballProps.init();
        CurseProps.init();
        CurseWardingProps.init();
        DemonFireProps.init();
        DemonIceProps.init();
        DemonLightProps.init();
        DemonWaterProps.init();
        GravityProps.init();
        SealProps.init();
        BarrierProps.init();
        ImitationProps.init();
        KeyProps.init();
        ChainProps.init();
        ButohProps.init();
        BriarProps.init();
        CherryBlossomProps.init();
        FungusProps.init();
        FoodProps.init();
        CrystalProps.init();
        CorundumProps.init();
        BronzeProps.init();
        CopperProps.init();
        IronProps.init();
        BlackOilProps.init();
        GelProps.init();
        GlassProps.init();
        BubbleProps.init();
        IceProps.init();
        IceWedgeProps.init();
        LegionProps.init();
        LightProps.init();
        PaintingProps.init();
        KotodamaProps.init();
        WorldTreeProps.init();
        DiceProps.init();
        SlashProps.init();
        CompassProps.init();
        MercuryProps.init();
        SandProps.init();
        RecombinationProps.init();
        com.newuniverse.nusmp.shadow.ShadowPackProps.init();
        EarthConstructProps.init();          // 1.93.0: Earth / Rock / Stone / Sandstone / Red Ochre / Mineral constructs
        ShakudoProps.init();                 // 1.93.0: the rebuilt wiki magics
        ScaleProps.init();
        SpikeProps.init();
        NailProps.init();
        HairProps.init();
        SongRebuildProps.init();                 // 1.93.0: Song, Sound, Soul, Soul Corpse and Memory
        SoundRebuildProps.init();
        SoulRebuildProps.init();
        SoulCorpseRebuildProps.init();
        MemoryRebuildProps.init();
    }
}
