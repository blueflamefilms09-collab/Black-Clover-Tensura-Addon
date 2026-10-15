package com.newuniverse.nusmp.client.labyrinth.gate;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

/**
 * Why: the Labyrinth's sky. The dimension type names its effects {@code nusmp:labyrinth}, and a {@link DimensionSpecialEffects} registered under
 * that id in {@link RegisterDimensionSpecialEffectsEvent} (the NeoForge hook; the pattern of {@code client/dungeon/DungeonSky}) is what the
 * renderer uses for clouds, horizon and fog colour. The look is a sea of clouds under a floating island: {@code SkyType.NORMAL} keeps the sun,
 * moon, stars and sunrise band; the cloud layer is put at y = 24, UNDER the Spire's base (built at y = 40), so from the arrival pad the clouds
 * are a floor below you, not a ceiling through the lower tiers; {@code hasGround = false} because the dark "earth" disc the Overworld draws below
 * the horizon would read as ground under a sky island, and without it the fog colour continues all the way down as it does in the End. The fog
 * colour is a deep indigo scaled by the sky brightness so night is darker violet, never black; {@code isFoggyAt} is false because the Spire is
 * meant to be seen from its base to its crown. No game-bus fog events are needed: the biome's own fog and sky colours do the rest.
 */
public final class GateClient {
    private GateClient() {}

    public static final ResourceLocation EFFECTS_ID = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "labyrinth");
    /** The cloud layer, under the Spire's base at y = 40. */
    static final float CLOUD_HEIGHT = 24f;
    static final Vec3 INDIGO = new Vec3(0.16, 0.11, 0.36);

    public static void init(IEventBus modBus) {
        modBus.addListener(GateClient::registerEffects);
    }

    private static void registerEffects(RegisterDimensionSpecialEffectsEvent e) {
        e.register(EFFECTS_ID, new SeaOfClouds());
    }

    /** The Overworld's kind of sky with the clouds moved under the island, no ground disc, and indigo fog. */
    public static final class SeaOfClouds extends DimensionSpecialEffects {
        public SeaOfClouds() { super(CLOUD_HEIGHT, false, DimensionSpecialEffects.SkyType.NORMAL, false, false); }

        @Override
        public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
            return INDIGO.scale(brightness * 0.75 + 0.25);
        }

        @Override
        public boolean isFoggyAt(int x, int z) { return false; }
    }
}
