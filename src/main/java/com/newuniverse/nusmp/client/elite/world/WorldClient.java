package com.newuniverse.nusmp.client.elite.world;

import com.newuniverse.nusmp.elite.world.WorldEntities;
import com.newuniverse.nusmp.elite.world.entity.CalamityBoss;
import com.newuniverse.nusmp.elite.world.entity.GaiaSpikeEntity;
import com.newuniverse.nusmp.elite.world.entity.GraveboundColossus;
import com.newuniverse.nusmp.elite.world.entity.LichBoss;
import com.newuniverse.nusmp.elite.world.entity.PhylacteryEntity;
import com.newuniverse.nusmp.elite.world.entity.VoidBoltProjectile;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * Why: the client half of the world engine, called from {@code WorldRegistry} only on the client: the renderers of the five entities (all {@link GeoWorldRenderer}s with a small {@code Look}
 * each) and the Magicule Gauge's HUD layer. The models are GeoLite models built by {@code tools/elite/gen_world_models.py}, key {@code elite} (files {@code elite_<name>.geo.json},
 * {@code .animation.json}, {@code .png}, {@code _glow.png}).
 */
public final class WorldClient {
    private WorldClient() {}

    private static final String KEY = "elite";

    public static void init(IEventBus modBus) {
        modBus.addListener(WorldClient::renderers);
        modBus.addListener(WorldClient::layers);
    }

    private static void layers(RegisterGuiLayersEvent e) {
        e.registerAboveAll(ResourceLocation.fromNamespaceAndPath("nusmp", "magicule_gauge"), new MagiculeHud());
    }

    private static float time(CalamityBoss b, float partial) {
        return b.tickCount / 20f + partial / 20f;
    }

    /** The clip of a boss and its clock: one-shot clips run from the moment the server started the animation, the loops from the entity's age. */
    private static String bossClip(CalamityBoss b) {
        if (b.isDeadOrDying()) return "death";
        return switch (b.anim()) {
            case CalamityBoss.ANIM_WINDUP -> "windup";
            case CalamityBoss.ANIM_STRIKE -> "strike";
            case CalamityBoss.ANIM_TRANSFORM -> "transform";
            case CalamityBoss.ANIM_CAST -> "cast";
            default -> b.walkAnimation.speed() > 0.05f ? "walk" : "idle";
        };
    }

    private static float bossSeconds(CalamityBoss b, float partial) {
        if (b.isDeadOrDying()) return (b.deathTime + partial) / 20f;
        return switch (b.anim()) {
            case CalamityBoss.ANIM_WINDUP, CalamityBoss.ANIM_STRIKE, CalamityBoss.ANIM_TRANSFORM, CalamityBoss.ANIM_CAST ->
                    Math.max(0f, (b.level().getGameTime() - b.animStart() + partial) / 20f);
            default -> time(b, partial);
        };
    }

    private static int pulse(CalamityBoss b, float partial, int rgb, float base, float amp) {
        float a = Mth.clamp(base + amp * Mth.sin((b.tickCount + partial) * 0.12f), 0f, 1f);
        return ((int) (a * 255f) << 24) | (rgb & 0xFFFFFF);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(WorldEntities.GRAVEBOUND_COLOSSUS.get(), ctx -> new GeoWorldRenderer<GraveboundColossus>(ctx, GeoSpec.of(KEY, "gravebound_colossus"), new GeoWorldRenderer.Look<>() {
            @Override public String clip(GraveboundColossus b, float partial) { return bossClip(b); }

            @Override public float seconds(GraveboundColossus b, float partial) { return bossSeconds(b, partial); }

            @Override public int glow(GraveboundColossus b, float partial) { return pulse(b, partial, 0xFFE9B0, 0.75f, 0.25f); }
        }, 1.6f));

        e.registerEntityRenderer(WorldEntities.LICH_BOSS.get(), ctx -> new GeoWorldRenderer<LichBoss>(ctx, GeoSpec.of(KEY, "lich"), new GeoWorldRenderer.Look<>() {
            @Override public String clip(LichBoss b, float partial) { return bossClip(b); }

            @Override public float seconds(LichBoss b, float partial) { return bossSeconds(b, partial); }

            @Override public float lift(LichBoss b, float partial) { return b.isDeadOrDying() ? 0f : 0.35f + 0.10f * Mth.sin((b.tickCount + partial) * 0.09f); }

            @Override public int glow(LichBoss b, float partial) { return pulse(b, partial, 0xC9A8FF, 0.8f, 0.2f); }

            @Override public int shell(LichBoss b, float partial) { return b.shielded() ? pulse(b, partial, 0x7A4CFF, 0.35f, 0.15f) : 0; }
        }, 0.7f));

        e.registerEntityRenderer(WorldEntities.PHYLACTERY.get(), ctx -> new GeoWorldRenderer<PhylacteryEntity>(ctx, GeoSpec.of(KEY, "phylactery"), new GeoWorldRenderer.Look<>() {
            @Override public String clip(PhylacteryEntity p, float partial) { return "idle"; }

            @Override public float seconds(PhylacteryEntity p, float partial) { return (p.tickCount + partial) / 20f; }

            @Override public float lift(PhylacteryEntity p, float partial) { return 0.1f * Mth.sin((p.tickCount + partial) * 0.1f); }

            @Override public float yaw(PhylacteryEntity p, float partial) { return (p.tickCount + partial) * 3f; }
        }, 0.4f));

        e.registerEntityRenderer(WorldEntities.GAIA_SPIKE.get(), ctx -> new GeoWorldRenderer<GaiaSpikeEntity>(ctx, GeoSpec.of(KEY, "gaia_spike"), new GeoWorldRenderer.Look<>() {
            @Override public boolean visible(GaiaSpikeEntity s, float partial) { return s.tickCount + partial >= s.delay() - 1; }

            @Override public String clip(GaiaSpikeEntity s, float partial) { return s.tickCount + partial > s.delay() + s.upTicks() - 6 ? "sink" : "rise"; }

            @Override public float seconds(GaiaSpikeEntity s, float partial) {
                float since = s.tickCount + partial - s.delay();
                return Math.max(0f, s.tickCount + partial > s.delay() + s.upTicks() - 6 ? since - (s.upTicks() - 6) : since) / 20f;
            }

            @Override public float yaw(GaiaSpikeEntity s, float partial) { return (s.getId() * 47) % 360; }
        }, 0f));

        e.registerEntityRenderer(WorldEntities.VOID_BOLT.get(), ctx -> new GeoWorldRenderer<VoidBoltProjectile>(ctx, GeoSpec.of(KEY, "void_bolt"), new GeoWorldRenderer.Look<>() {
            @Override public String clip(VoidBoltProjectile b, float partial) { return "spin"; }

            @Override public float seconds(VoidBoltProjectile b, float partial) { return (b.tickCount + partial) / 20f; }

            @Override public float lift(VoidBoltProjectile b, float partial) { return 0.0f; }

            @Override public float yaw(VoidBoltProjectile b, float partial) { return -Mth.rotLerp(partial, b.yRotO, b.getYRot()); }
        }, 0f));
    }
}
