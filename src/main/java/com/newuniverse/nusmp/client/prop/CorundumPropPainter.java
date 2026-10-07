package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * 0.54 Corundum Magic: how the two props look (both are GeoLite models, see tools/gen_corundum_geo.py). Both models are lowered so that the entity's
 * position is their centre (the server moves the centre).
 * CORUNDUM_1, the Ideal Closer fist (param bit 0 = gem: 0 ruby, 1 sapphire; bit 1 = hammer-fist):
 *   1. a soft additive halo behind the gem on the back of the hand (RenderType.eyes, full bright, camera-facing);
 *   2. the gauntlet model, cutout (entityCutoutNoCull) with its glow map additive on top (GeoDraw's own second pass): clip idle while it rises, punch or
 *      slam from the draw-back to the recovery, idle after; it grows in over 10 ticks and shrinks out over the last 8;
 *   3. during the strike: a streak behind the fist and a flash star on the knuckles, both additive (eyes), full bright, drawn after every solid pass.
 * CORUNDUM_2, a gem shard (param bit 6 = gem, bits 8-9 = phase from the server: 0 orbit, 1 darting, 2 returning):
 *   1. the shard model, cutout + glow additive, pitched at its foe while it darts (clip fire) and idling (clip idle) otherwise;
 *   2. a glint on its tip and, while it darts, a streak behind it, additive (eyes), full bright.
 * Nothing here allocates per frame beyond the vertex data; every texture exists (particle/corundum_*.png, entity/corundum_gauntlet*, entity/corundum_gem_shard*).
 */
public final class CorundumPropPainter {
    private CorundumPropPainter() {}

    private static final GeoSpec FIST = GeoSpec.of("corundum", "gauntlet"), SHARD = GeoSpec.of("corundum", "gem_shard");
    private static final int FB = PropDraw.FULL_BRIGHT, RUBY = 0xFF6A7A, SAPPHIRE = 0x6AA6FF, WHITE = 0xFFFFFF;
    /** The fist's timeline, the same numbers as prop.CorundumProps (ticks): the draw-back starts at 14, the blow lands at 22 (24 for the hammer-fist). */
    private static final int FIST_WINDUP = 14, FIST_HIT = 22, FIST_HIT_SLAM = 24;
    private static ResourceLocation star, streak, halo;

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        star = AuraRender.tex("particle/corundum_star");
        streak = AuraRender.tex("particle/corundum_streak");
        halo = AuraRender.tex("particle/glow");
        PropPainters.register(PropKind.CORUNDUM_1, CorundumPropPainter::fist);
        PropPainters.register(PropKind.CORUNDUM_2, CorundumPropPainter::shard);
    }

    // ================================================================ the fist
    private static void fist(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        float life = e.life() + partial, max = Math.max(1, e.maxLife());
        float k = CorundumDraw.smooth(life / 10f) * Mth.clamp((max - life) / 8f, 0f, 1f);
        if (k <= 0.01f) return;
        boolean slam = (e.param() & 2) != 0, sapphire = (e.param() & 1) != 0;
        int gem = sapphire ? SAPPHIRE : RUBY, hit = slam ? FIST_HIT_SLAM : FIST_HIT;
        float s = e.scale() * (0.4f + 0.6f * k);
        float since = life - FIST_WINDUP;
        boolean act = since >= 0f && since < 20f;
        String clip = act ? (slam ? "slam" : "punch") : "idle";
        float clock = act ? since / 20f : age / 20f;
        float pulse = 0.75f + 0.25f * Mth.sin(age * 0.3f);
        float flash = Mth.clamp(1f - Math.abs(life - hit - 0.5f) / 5f, 0f, 1f);
        float strike = act ? Mth.clamp(1f - Math.abs(life - (hit - 1.5f)) / 4.5f, 0f, 1f) : 0f;
        int tint = sapphire ? 0xFFE6EEFF : 0xFFFFE8E8;

        CorundumDraw.camera(pose);
        VertexConsumer additive;
        // 1. the model: lowered so its centre is the entity's position; the glow map is its second, additive pass
        pose.pushPose();
        pose.translate(0f, -1.5f * s, 0f);
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buf, FIST, clip, clock, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, tint,
                CorundumDraw.argb(Mth.clamp(k * (0.7f + 0.3f * pulse + 0.7f * flash), 0f, 1f), WHITE));
        pose.popPose();

        // 2. additive light after every solid pass: the halo on the back of the hand, the streak behind the blow and the flash on the knuckles
        additive = buf.getBuffer(AuraRender.additive(halo));
        CorundumDraw.glow(pose, additive, 0f, 1.0f * s, -0.5f * s, (0.85f + 0.25f * pulse + 0.5f * flash) * s, age * 0.03f,
                CorundumDraw.argb(0.42f * k * (0.6f + 0.4f * pulse), gem), FB);
        if (strike > 0.02f) {
            additive = buf.getBuffer(AuraRender.additive(streak));
            CorundumDraw.streak(pose, additive, 0f, 0.1f * s, -3.6f * s, 0f, 0.1f * s, 0.9f * s, 0.7f * s, CorundumDraw.argb(0.85f * strike * k, gem), FB);
        }
        if (flash > 0.02f) {
            additive = buf.getBuffer(AuraRender.additive(star));
            CorundumDraw.glow(pose, additive, 0f, 0f, 1.25f * s, (1.0f + 1.6f * flash) * s, age * 0.1f, CorundumDraw.argb(0.95f * flash * k, CorundumDraw.mix(gem, WHITE, 0.55f)), FB);
        }
    }

    // ================================================================ the gem shard
    private static void shard(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        float life = e.life() + partial, max = Math.max(1, e.maxLife());
        float k = CorundumDraw.smooth(life / 8f) * Mth.clamp((max - life) / 8f, 0f, 1f);
        if (k <= 0.01f) return;
        int slot = e.param() & 7, phase = (e.param() >> 8) & 3;
        boolean sapphire = (e.param() >> 6 & 1) != 0, dart = phase == 1;
        int gem = sapphire ? SAPPHIRE : RUBY;
        float s = e.scale() * k;
        Entity foe = e.target();
        pose.pushPose();
        if (dart && foe != null) {                                                       // pitch the tip at the foe (the yaw is the entity's)
            double dx = foe.getX() - e.getX(), dy = foe.getY() + foe.getBbHeight() * 0.5 - e.getY(), dz = foe.getZ() - e.getZ();
            double flat = Math.sqrt(dx * dx + dz * dz);
            pose.mulPose(Axis.XP.rotationDegrees(-(float) Math.toDegrees(Mth.atan2(dy, Math.max(0.01, flat)))));
        } else {
            pose.mulPose(Axis.XP.rotationDegrees(-6f));
        }
        CorundumDraw.camera(pose);
        float pulse = 0.75f + 0.25f * Mth.sin(age * 0.35f + slot);
        // 1. the shard (its centre sits 16 px over its origin: the entity's position is its centre)
        pose.pushPose();
        pose.translate(0f, -1.0f * s, 0f);
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buf, SHARD, dart ? "fire" : "idle", age / 20f + slot * 0.37f, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light,
                sapphire ? 0xFFE6EEFF : 0xFFFFE8E8, CorundumDraw.argb(Mth.clamp(k * (dart ? 1f : pulse), 0f, 1f), WHITE));
        pose.popPose();
        // 2. a glint on the tip (the model's -z is its front, 19 px from the centre) and, while darting, the streak behind it
        float tip = 1.35f * s;
        CorundumDraw.glow(pose, buf.getBuffer(AuraRender.additive(star)), 0f, 0f, tip, (0.32f + 0.5f * (dart ? 1f : 0f)) * s * pulse, age * 0.07f,
                CorundumDraw.argb(0.8f * k, CorundumDraw.mix(gem, WHITE, 0.5f)), FB);
        if (dart) CorundumDraw.streak(pose, buf.getBuffer(AuraRender.additive(streak)), 0f, 0f, -3.2f * s, 0f, 0f, 0.5f * s, 0.34f * s, CorundumDraw.argb(0.75f * k, gem), FB);
        pose.popPose();
    }
}
