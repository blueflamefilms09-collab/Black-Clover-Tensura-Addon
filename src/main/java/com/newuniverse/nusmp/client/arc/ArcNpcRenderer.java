package com.newuniverse.nusmp.client.arc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcNpcEntity;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.zogratis.ZogratisGlow;
import com.newuniverse.nusmp.client.zogratis.ZogratisPose;
import com.newuniverse.nusmp.client.zogratis.ZogratisRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Why: all friendly characters of the arcs are ONE entity ({@code nusmp:arc_npc}) whose identity is a synced string, so they are drawn by ONE renderer that reads that string,
 * finds the model the art kit wrote for it ({@code arc_<id>}, see {@link ArcModelKey}) and plays the idle, walk and talk clips ({@link ArcNpcPose}: talk is the {@code cast}
 * clip, played when a player stands close). A character whose model is missing is drawn as the plain villager {@value ArcModelKey#FALLBACK}; if even that is missing nothing is
 * drawn (logged once), never an exception. The name of the character floats over its head when you are near.
 */
public class ArcNpcRenderer extends ZogratisRenderer<ArcNpcEntity> {
    private static final Logger LOG = LogUtils.getLogger();
    /** The rule that makes an NPC talk (the arc core builder may replace it with the real conversation state). The default: a player is close and the NPC stands still. */
    public static volatile Predicate<ArcNpcEntity> talkingRule = ArcNpcRenderer::playerNear;
    private static final double NAME_RANGE_SQR = 20.0 * 20.0;

    private final Map<String, ArcClips> clipsOf = new HashMap<>();
    private final Set<String> warned = new HashSet<>();

    public ArcNpcRenderer(EntityRendererProvider.Context ctx) { super(ctx, 0.45f); }

    private static boolean playerNear(ArcNpcEntity e) {
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null && ArcNpcPose.talking(e.distanceToSqr(p), true, e.isDeadOrDying());
    }

    private String modelOf(ArcNpcEntity e) {
        String want = ArcModelKey.clean(e.characterId());
        String id = ArcModelKey.resolve(e.characterId(), ArcModels::hasModel);
        if (id.isEmpty()) {
            if (warned.add("none:" + want)) LOG.warn("[nusmp] no model for the arc character '{}' and no fallback body: it is not drawn", want);
        } else if (!id.equals(want)) {
            if (warned.add("fallback:" + want)) LOG.warn("[nusmp] the arc character '{}' has no model (arc_{}): drawn as {}", want, want, id);
        }
        return id;
    }

    @Override
    public ResourceLocation getTextureLocation(ArcNpcEntity e) {
        String id = ArcModelKey.resolve(e.characterId(), ArcModels::hasModel);
        return GeoSpec.of(ArcModelKey.KEY, id.isEmpty() ? ArcModelKey.FALLBACK : id).texture();
    }

    @Override
    public boolean shouldRender(ArcNpcEntity e, Frustum frustum, double x, double y, double z) { return seen(e, frustum, x, y, z, 1.4, 1.0); }

    @Override
    public void render(ArcNpcEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        String id = modelOf(e);
        if (!id.isEmpty()) {
            GeoSpec spec = GeoSpec.of(ArcModelKey.KEY, id);
            ArcClips clips = clipsOf.computeIfAbsent(id, k -> new ArcClips(GeoSpec.of(ArcModelKey.KEY, k)));
            ZogratisPose clock = clock(e);
            ZogratisPose.Frame f = frame(e, partial);
            float[] stride = ArcModels.strides(id);
            boolean standing = clock.speed() < ZogratisPose.WALK_ON;
            boolean talking = standing && talkingRule.test(e);
            ArcNpcPose.fill(f, talking, stride[0], stride[1]);
            clock.update(f, clips);
            float fade = f.dead ? ZogratisGlow.deathFade(f.deathTicks, DEATH_TICKS) : 1f;
            int glow = ZogratisGlow.grey((0.72f + 0.12f * Mth.sin(f.age * 0.07f)) * fade);
            float body = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot), head = Mth.rotLerp(partial, e.yHeadRotO, e.yHeadRot);
            float facing = clock.smoothYaw(f.dead || !(talking || standing) ? body : head, f.age);     // it faces what it looks at while it stands, its way while it walks
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-facing));
            paint(pose, buffers, spec, clock.clip, clock.seconds, light, bodyRgb(e), fade, glow);
            pose.popPose();
        }
        super.render(e, yaw, partial, pose, buffers, light);                   // the name tag
    }

    /** The character's name shows over its head from 20 blocks (a custom name keeps vanilla's rule: it shows when you look at it). */
    @Override
    protected boolean shouldShowName(ArcNpcEntity e) {
        if (super.shouldShowName(e)) return true;
        return !e.isInvisible() && e.character() != null && this.entityRenderDispatcher.distanceToSqr(e) < NAME_RANGE_SQR;
    }

    @Override
    protected void renderNameTag(ArcNpcEntity e, Component name, PoseStack pose, MultiBufferSource buffers, int light, float partial) {
        ArcCharacter c = e.character();
        super.renderNameTag(e, e.hasCustomName() || c == null ? name : Component.literal(c.name()), pose, buffers, light, partial);
    }
}
