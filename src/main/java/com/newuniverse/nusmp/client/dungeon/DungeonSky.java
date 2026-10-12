package com.newuniverse.nusmp.client.dungeon;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.dungeon.DungeonAtmosphere;
import com.newuniverse.nusmp.dungeon.DungeonTier;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Vector3f;

/**
 * The Living Dungeon's own sky, fog and drifting air (docs/living_dungeon.md section 7; moods in {@link DungeonAtmosphere}). The dimension type
 * names the effects "nusmp:living_dungeon" (it used to borrow "minecraft:the_nether"): no sky, thick fog. While the camera is in the Dungeon the fog
 * colour and distance ease toward the mood of the tier it is in, over about three seconds, and the tier's particles drift round the player.
 * Fog under lava, water and powder snow, and under Blindness or Darkness, is left to the game.
 */
public final class DungeonSky {
    private DungeonSky() {}

    public static final ResourceLocation EFFECTS_ID = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "living_dungeon");

    /** Seconds to cover 63% of the way to a new mood (about 95% after three). */
    private static final float MOOD_TAU = 1.0f;
    /** A long frame (a pause, a lag spike) counts as at most this many seconds of easing. */
    private static final float MAX_FRAME_SECONDS = 0.25f;
    /** Terrain fog never ends nearer than this (blocks), unless the game's own fog already did. */
    private static final float MIN_FOG_FAR = 24f;
    private static final int PARTICLE_REACH = 10, PARTICLE_DOWN = 3, PARTICLE_UP = 5, PARTICLE_TRIES = 6;

    private static final DustParticleOptions CATACOMB_DUST = new DustParticleOptions(new Vector3f(0.62f, 0.70f, 0.80f), 0.7f);
    private static final DustParticleOptions EMBER_DUST = new DustParticleOptions(new Vector3f(1.0f, 0.42f, 0.12f), 0.8f);
    private static final DustParticleOptions GOLD_DUST = new DustParticleOptions(new Vector3f(0.98f, 0.78f, 0.28f), 0.9f);

    /** The fog colour on show and how much of the usual fog distances it keeps; the colour is null while the camera is outside the Dungeon. */
    private static float[] fog;
    private static float nearScale = 1f, farScale = 1f;
    private static long lastFrame;

    // ---------------------------------------------------------------- the dimension's effects (mod bus)
    public static void registerEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(EFFECTS_ID, new Effects());
    }

    /** Like the Nether's effects (no sky, thick fog, constant ambient light), with the fog colour taken from the tier's mood. */
    public static final class Effects extends DimensionSpecialEffects {
        public Effects() { super(Float.NaN, true, DimensionSpecialEffects.SkyType.NONE, false, true); }

        @Override
        public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
            float[] shown = fog;
            return shown == null ? color : new Vec3(shown[0], shown[1], shown[2]);
        }

        @Override
        public boolean isFoggyAt(int x, int z) { return true; }
    }

    // ---------------------------------------------------------------- fog (game bus)
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !LivingDungeon.in(mc.level)) {
            fog = null;
            return;
        }
        Camera camera = event.getCamera();
        ease(camera.getPosition());
        if (camera.getFluidInCamera() != FogType.NONE || impaired(camera)) return;
        event.setRed(fog[0]);
        event.setGreen(fog[1]);
        event.setBlue(fog[2]);
    }

    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (fog == null || mc.level == null || !LivingDungeon.in(mc.level)) return;
        if (event.getMode() != FogRenderer.FogMode.FOG_TERRAIN || event.getType() != FogType.NONE || impaired(event.getCamera())) return;
        float far = Math.max(event.getFarPlaneDistance() * farScale, Math.min(event.getFarPlaneDistance(), MIN_FOG_FAR));
        float near = Math.min(event.getNearPlaneDistance() * nearScale, far * 0.5f);
        event.setNearPlaneDistance(near);
        event.setFarPlaneDistance(far);
        event.setCanceled(true);                                                     // the game only takes the event's distances when it is cancelled
    }

    /** Blindness and Darkness set their own fog; the Dungeon's mood stays out of the way. */
    private static boolean impaired(Camera camera) {
        return camera.getEntity() instanceof LivingEntity e && (e.hasEffect(MobEffects.BLINDNESS) || e.hasEffect(MobEffects.DARKNESS));
    }

    /** Moves the colour and distances on show toward the mood of the tier at this position by the time since the last frame; the first frame in the Dungeon starts on the mood. */
    private static void ease(Vec3 at) {
        DungeonAtmosphere.Mood mood = DungeonAtmosphere.mood(DungeonTier.at(Mth.floor(at.x), Mth.floor(at.y), Mth.floor(at.z)).ordinal());
        long now = System.nanoTime();
        float dt = lastFrame == 0L ? 0f : Math.min(MAX_FRAME_SECONDS, (now - lastFrame) / 1.0e9f);
        lastFrame = now;
        if (fog == null) {
            fog = mood.fog();
            nearScale = mood.fogNear();
            farScale = mood.fogFar();
            return;
        }
        float k = DungeonAtmosphere.smoothing(dt, MOOD_TAU);
        fog = DungeonAtmosphere.lerpColor(fog, mood.fog(), k);
        nearScale = DungeonAtmosphere.lerp(nearScale, mood.fogNear(), k);
        farScale = DungeonAtmosphere.lerp(farScale, mood.fogFar(), k);
    }

    // ---------------------------------------------------------------- the air (game bus)
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null) {                                       // no world open: the next one starts from its own mood
            fog = null;
            return;
        }
        if (mc.isPaused() || !LivingDungeon.in(level)) return;
        ParticleStatus status = mc.options.particles().get();
        if (status == ParticleStatus.MINIMAL) return;
        DungeonAtmosphere.Mood mood = DungeonAtmosphere.mood(DungeonTier.at(player.getBlockX(), player.getBlockY(), player.getBlockZ()).ordinal());
        RandomSource random = level.random;
        float share = status == ParticleStatus.DECREASED ? 0.5f : 1f;
        for (DungeonAtmosphere.Drift d : mood.particles()) {
            int n = DungeonAtmosphere.particleCount(d.rate() * share, random.nextFloat());
            for (int i = 0; i < n; i++) drift(level, player, d.kind(), random);
        }
    }

    /** Adds one particle of this kind at a fitting spot near the player, if one is found in a few tries. */
    private static void drift(ClientLevel level, LocalPlayer player, String kind, RandomSource random) {
        for (int attempt = 0; attempt < PARTICLE_TRIES; attempt++) {
            BlockPos pos = new BlockPos(player.getBlockX() + random.nextInt(2 * PARTICLE_REACH + 1) - PARTICLE_REACH,
                    player.getBlockY() + random.nextInt(PARTICLE_DOWN + PARTICLE_UP + 1) - PARTICLE_DOWN,
                    player.getBlockZ() + random.nextInt(2 * PARTICLE_REACH + 1) - PARTICLE_REACH);
            if (!fits(level, pos, kind)) continue;
            double x = pos.getX() + random.nextDouble(), y = pos.getY() + random.nextDouble(), z = pos.getZ() + random.nextDouble();
            double sway = (random.nextDouble() - 0.5) * 0.02;
            switch (kind) {
                case DungeonAtmosphere.DUST -> level.addParticle(CATACOMB_DUST, x, y, z, sway, -0.004, sway);
                case DungeonAtmosphere.DRIP -> level.addParticle(ParticleTypes.DRIPPING_DRIPSTONE_WATER, x, pos.getY() + 0.95, z, 0.0, 0.0, 0.0);
                case DungeonAtmosphere.BUBBLE -> level.addParticle(ParticleTypes.BUBBLE, x, y, z, 0.0, 0.05, 0.0);
                case DungeonAtmosphere.SMOKE -> level.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, sway, 0.03, sway);
                case DungeonAtmosphere.EMBER -> level.addParticle(EMBER_DUST, x, y, z, sway, 0.06, sway);
                case DungeonAtmosphere.GOLD -> level.addParticle(GOLD_DUST, x, y, z, sway, 0.004, sway);
                case DungeonAtmosphere.GLINT -> level.addParticle(ParticleTypes.WAX_ON, x, y, z, 0.0, 0.0, 0.0);
                default -> { }
            }
            return;
        }
    }

    /** Bubbles rise in water, drips hang under a ceiling, everything else drifts in open air. */
    private static boolean fits(ClientLevel level, BlockPos pos, String kind) {
        if (kind.equals(DungeonAtmosphere.BUBBLE)) return level.getFluidState(pos).is(FluidTags.WATER);
        if (!level.getBlockState(pos).isAir()) return false;
        return !kind.equals(DungeonAtmosphere.DRIP) || level.getBlockState(pos.above()).isFaceSturdy(level, pos.above(), Direction.DOWN);
    }
}
