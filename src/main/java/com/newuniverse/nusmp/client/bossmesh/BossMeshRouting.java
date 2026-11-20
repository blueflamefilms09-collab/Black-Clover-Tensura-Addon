package com.newuniverse.nusmp.client.bossmesh;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.client.playermodel.PlayerBodies;
import com.newuniverse.nusmp.client.playermodel.PlayerModelOptions;
import com.newuniverse.nusmp.client.playermodel.PlayerModelRenderer;
import com.newuniverse.nusmp.client.zogratis.ZogratisGlow;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.relics.order.GodOfOrder;
import com.newuniverse.nusmp.relics.order.OrderEntities;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.slf4j.Logger;

import java.util.Locale;
import java.util.function.Function;

/**
 * Boss rework wave 3 (docs/bosses/rework_wave3.md): routes Lucifero and the God of Order to their Blender-made bodies ({@code tools/blender/boss_lucifero.py},
 * {@code boss_god_of_order.py}). Both were drawn by the player-model kit; the kit's renderer stays as the fallback (client settings {@code luciferoBlenderModel} and
 * {@code godOfOrderBlenderModel}, and a missing or broken mesh file). Called from {@code PlayerModelRegistry.register}, after the kit registered its own renderers (the last
 * registration of a type wins). Client only; nothing here is reached from common code.
 *
 * <p>Cost per boss per frame (rule 11): one {@link BossMeshDraw} draw, at most 2569 body quads (1136 beyond 40 blocks) plus 489 glow quads within 48 blocks; the pose is
 * solved into fields, nothing is allocated per frame.
 */
public final class BossMeshRouting {
    private static final Logger LOG = LogUtils.getLogger();
    private static final float DEATH_TICKS = 20f;

    private BossMeshRouting() {}

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        try {
            for (ArcEntities.Entry b : ArcEntities.bosses()) {
                if (!"lucifero".equals(b.id())) continue;
                @SuppressWarnings("unchecked") EntityType<ArcBoss> type = (EntityType<ArcBoss>) b.type().get();
                e.registerEntityRenderer(type, ctx -> new MeshBossRenderer<ArcBoss>(ctx, new PlayerModelRenderer<>(ctx, new PlayerBodies.Arc()),
                        en -> luciferoMesh(PlayerModelOptions.luciferoNewBody(), en.power(), en.gatesOpen(), en.form() != ZogratisBoss.FORM_BASE), 0xA66BFF, 3.0));
            }
        } catch (RuntimeException | LinkageError ex) {
            LOG.error("[anime_multiverse] the Blender body of Lucifero could not be registered: {}", ex.toString());
        }
        try {                                                                  // Kakashi Hatake (docs/bosses/kakashi.md): his own Blender body "kakashi"; the shared person body is the fallback
            for (ArcEntities.Entry b : ArcEntities.bosses()) {
                if (!"kakashi_hatake".equals(b.id())) continue;
                @SuppressWarnings("unchecked") EntityType<ArcBoss> type = (EntityType<ArcBoss>) b.type().get();
                e.registerEntityRenderer(type, ctx -> new MeshBossRenderer<ArcBoss>(ctx, new PlayerModelRenderer<>(ctx, new PlayerBodies.Arc()), en -> "kakashi", 0xFF2420, 2.0));
            }
        } catch (RuntimeException | LinkageError ex) {
            LOG.error("[anime_multiverse] the Blender body of Kakashi could not be registered: {}", ex.toString());
        }
        try {
            EntityType<GodOfOrder> type = OrderEntities.GOD_OF_ORDER.get();
            e.registerEntityRenderer(type, ctx -> new MeshBossRenderer<GodOfOrder>(ctx, new PlayerModelRenderer<>(ctx, new PlayerBodies.Order()),
                    en -> en.gatesOpen() ? "god_of_order_gates" : "god_of_order", 0xE8B923, 2.0));
        } catch (RuntimeException | LinkageError ex) {
            LOG.error("[anime_multiverse] the Blender body of the God of Order could not be registered: {}", ex.toString());
        }
    }

    /**
     * Which of Lucifero's bodies is drawn (pure). v2 (default): the king until 80 percent power, then wings spread (the moment the fight says "his wings unfold"), then the Gates body
     * (four wings turned into arms and a halo of horns). Not v2: the wave 3 body, as before (base, or the hybrid form).
     */
    static String luciferoMesh(boolean newBody, int power, boolean gatesOpen, boolean hybrid) {
        if (!newBody) return hybrid ? "lucifero_form2" : "lucifero";
        return gatesOpen ? "lucifero_v2_gates" : power >= 80 ? "lucifero_v2_form2" : "lucifero_v2";
    }

    /** What a casting move looks like in the body: the staff levelled, the bow, the ledger read, or a heavy cast. Pure. */
    static int actionOf(String moveId) {
        if (moveId == null) return MeshPose.SLAM;
        String id = moveId.toLowerCase(Locale.ROOT);
        if (id.contains("raikiri") || id.contains("beast_fang") || id.contains("purple")) return MeshPose.RAIKIRI;          // Kakashi's moves (appended)
        if (id.contains("kunai") || id.contains("shuriken")) return MeshPose.THROW;
        if (id.contains("water_dragon") || id.contains("clone") || id.contains("mist") || id.contains("earth_wall") || id.contains("ninken")) return MeshPose.SEALS;
        if (id.contains("sharingan") || id.contains("kamui_step") || id.contains("kamui_vortex")) return MeshPose.KAMUI_EYE;
        if (id.contains("susanoo")) return MeshPose.SUSANOO;
        if (id.contains("bow") || id.contains("kneel")) return MeshPose.BOW;
        if (id.contains("beam") || id.contains("ray") || id.contains("lance") || id.contains("smite") || id.contains("judg")) return MeshPose.BEAM;
        if (id.contains("ledger") || id.contains("verdict") || id.contains("erase") || id.contains("law") || id.contains("roar")) return MeshPose.ROAR;
        return MeshPose.SLAM;
    }

    /** Draws one of the boss's Blender bodies (chosen per frame by {@code meshOf}: the form), or the kit's renderer when it is off or has no file. */
    static final class MeshBossRenderer<T extends ZogratisBoss> extends EntityRenderer<T> {
        private final EntityRenderer<T> fallback;
        private final Function<T, String> meshOf;
        private final int glowRgb;
        private final double grow;
        private final java.util.Map<String, BossMeshDraw> draws = new java.util.HashMap<>();

        MeshBossRenderer(EntityRendererProvider.Context ctx, EntityRenderer<T> fallback, Function<T, String> meshOf, int glowRgb, double grow) {
            super(ctx);
            this.fallback = fallback;
            this.meshOf = meshOf;
            this.glowRgb = glowRgb;
            this.grow = grow;
        }

        private BossMeshDraw drawFor(T e) {
            String id = meshOf.apply(e);
            if (!PlayerModelOptions.blender(id)) return null;
            BossMeshDraw d = draws.computeIfAbsent(id, BossMeshDraw::new);
            return d.available() ? d : null;
        }

        @Override
        public ResourceLocation getTextureLocation(T e) { return fallback.getTextureLocation(e); }

        /** Wings, halos and staffs reach past the hitbox: cull by a bigger box. */
        @Override
        public boolean shouldRender(T e, Frustum frustum, double x, double y, double z) {
            if (drawFor(e) == null) return fallback.shouldRender(e, frustum, x, y, z);
            if (!e.shouldRender(x, y, z)) return false;
            if (e.noCulling) return true;
            AABB box = e.getBoundingBoxForCulling().inflate(grow, grow * 0.5, grow);
            return frustum.isVisible(box);
        }

        @Override
        public void render(T e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
            BossMeshDraw d = drawFor(e);
            if (d == null) {
                fallback.render(e, yaw, partial, pose, buffers, light);
                return;
            }
            boolean dead = e.deathTime > 0 || e.state() == ZogratisBoss.STATE_DEAD;
            boolean casting = e.state() == ZogratisBoss.STATE_CASTING, transforming = e.transforming();
            Move move = casting ? e.castingMoveOrNull() : null;
            float age = e.tickCount + partial;
            float phase = e.phaseProgress();
            int action = dead ? MeshPose.DEATH : transforming ? MeshPose.ROAR : casting ? actionOf(move == null ? null : move.id()) : MeshPose.NONE;
            float t = dead ? Math.min(1f, (e.deathTime + partial) / DEATH_TICKS) : (casting || transforming) ? phase : 0f;
            float stride = e.walkAnimation.position(partial) * 0.2f;
            float walk = Math.min(1f, e.walkAnimation.speed(partial) * 2.5f);
            d.pose.solve(age, stride - (float) Math.floor(stride), walk, action, t);
            int mode = dead ? ZogratisGlow.NORMAL : transforming ? ZogratisGlow.TRANSFORMING : casting ? (move != null && move.ultimate() ? ZogratisGlow.ULTIMATE : ZogratisGlow.CASTING)
                    : e.exposed() ? ZogratisGlow.EXPOSED : ZogratisGlow.NORMAL;
            float fade = dead ? 1f - Math.min(1f, (e.deathTime + partial) / DEATH_TICKS) : 1f;
            float level = ZogratisGlow.level(e.power(), age, mode, phase) * fade;
            float size = e.getBbHeight() / e.getType().getHeight();
            float facing = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot);
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-facing));
            pose.scale(size, size, size);
            d.draw(pose, buffers, light, ZogratisGlow.withAlpha(0xFFFFFF, fade), ZogratisGlow.scaled(glowRgb, Math.max(level, 0.35f * fade)),
                    entityRenderDispatcher.distanceToSqr(e));
            pose.popPose();
            super.render(e, yaw, partial, pose, buffers, light);                      // the name tag
        }
    }
}
