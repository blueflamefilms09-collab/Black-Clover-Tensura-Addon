package com.newuniverse.nusmp.client.dungeon;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

/**
 * Why: the dimension special effects (no sky, thick fog, constant ambient light) that the Mega-Dungeon's dimension type names with
 * {@code "effects": "nusmp:living_dungeon"}. The id keeps its old spelling on purpose: the dimension type JSON of the Mega-Dungeon (and of the
 * old dimension that stays registered so a world that remembers it still decodes) points at it, and an id that no client registers falls back
 * to the Overworld's sky and a daylight horizon inside a sealed dungeon. Until 1.58.0 this class also eased the fog colour toward the mood of
 * the Living Dungeon's tiers and drifted its particles round the player; that mood layer belonged to the removed dungeon and went with it
 * (it never ran in the Mega-Dungeon: it asked whether the level was the Living Dungeon's).
 */
public final class DungeonSky {
    private DungeonSky() {}

    public static final ResourceLocation EFFECTS_ID = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "living_dungeon");

    // ---------------------------------------------------------------- the dimension's effects (mod bus)
    public static void registerEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(EFFECTS_ID, new Effects());
    }

    /** Like the Nether's effects: no sky, thick fog, constant ambient light, and the fog colour the biome gives. */
    public static final class Effects extends DimensionSpecialEffects {
        public Effects() { super(Float.NaN, true, DimensionSpecialEffects.SkyType.NONE, false, true); }

        @Override
        public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) { return color; }

        @Override
        public boolean isFoggyAt(int x, int z) { return true; }
    }
}
