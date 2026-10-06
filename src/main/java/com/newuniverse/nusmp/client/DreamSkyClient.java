package com.newuniverse.nusmp.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

/**
 * 0.41: the dream dimension's sky (data/nusmp/dimension_type/dream.json, effects "nusmp:dream"): the fixed starry dusk of the
 * overworld sky with a pastel pink-lilac fog that shifts toward sky blue, so the void round the dome reads as a dreamscape.
 */
public final class DreamSkyClient {
    private DreamSkyClient() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(DreamSkyClient::register);
    }

    private static void register(RegisterDimensionSpecialEffectsEvent e) {
        e.register(ResourceLocation.fromNamespaceAndPath("nusmp", "dream"), new DimensionSpecialEffects(Float.NaN, true, DimensionSpecialEffects.SkyType.NORMAL, false, false) {
            @Override
            public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
                double t = 0.5 + 0.5 * Mth.sin((float) (System.currentTimeMillis() % 60000L) / 60000f * Mth.TWO_PI);
                Vec3 pink = new Vec3(0.98, 0.72, 0.9), sky = new Vec3(0.72, 0.8, 1.0);
                return pink.lerp(sky, t * 0.6);
            }

            @Override
            public boolean isFoggyAt(int x, int z) { return false; }
        });
    }
}
