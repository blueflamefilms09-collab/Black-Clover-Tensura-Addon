package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.prop.EarthConstruct;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.EarthFxStyle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1.93.0 How the constructs of the earth-family magics look ({@link PropKind#EARTH_CONSTRUCT}, one painter for nine geo models, see tools/gen_earth_family_constructs.py):
 * earth_golem (Clay Guardian), rock_titan (Boulder Titan), stone_sentinel, sandstone_sphinx, sandstone_cage (the Variable Hardness Prison block), red_ochre_totem,
 * mineral_spire, rock_boulder and mineral_lance. The clip comes from the synced state word ({@link EarthConstruct}): idle / walk by its movement, attack, roar or block
 * from the tick the state began; the cage plays form, idle, crumble by its age; the spire grows. Every construct rises out of the ground over 12 ticks (the cage and
 * the projectiles do not) and sinks and shrinks over the last 22; a soft ground shadow sits 2 cm over the floor.
 *
 * <p>Render types: the model is CUTOUT (solid, drawn first), its {@code _glow} map additive and full bright (drawn by GeoDraw right after), the shadow and the dust are
 * translucent sprites, the sigils, glints and streaks additive, always after the solid parts. Extras per magic: sand pouring off the sphinx and the cage with the
 * hourglass sigil turning under it, pigment drops and a floating handprint over the totem, glints and a lattice ring round the spire, a dust trail behind the boulder
 * and a streak behind the lance.</p>
 */
public final class EarthPropPainter {
    private EarthPropPainter() {}

    private static final GeoSpec[] SPEC = {
            GeoSpec.of("earth", "golem"), GeoSpec.of("rock", "titan"), GeoSpec.of("stone", "sentinel"), GeoSpec.of("sandstone", "sphinx"), GeoSpec.of("sandstone", "cage"),
            GeoSpec.of("red_ochre", "totem"), GeoSpec.of("mineral", "spire"), GeoSpec.of("rock", "boulder"), GeoSpec.of("mineral", "lance")};
    private static final int FB = PropDraw.FULL_BRIGHT;

    private static ResourceLocation tex(int style, String name) { return AuraRender.tex("particle/earthfam_" + EarthFxStyle.KEYS[style] + "_" + name); }

    private static ResourceLocation shadow;

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        shadow = AuraRender.tex("particle/glow");
        PropPainters.register(PropKind.EARTH_CONSTRUCT, EarthPropPainter::paint);
    }

    private static float smooth(float t) { t = Mth.clamp(t, 0f, 1f); return t * t * (3f - 2f * t); }

    private static boolean moving(MagicPropEntity e) {
        double dx = e.getX() - e.xo, dz = e.getZ() - e.zo;
        return dx * dx + dz * dz > 1.0e-4;
    }

    private static void shade(PoseStack pose, MultiBufferSource buffers, float r, float alpha, int light) {
        VertexConsumer vc = buffers.getBuffer(AuraRender.translucent(shadow));
        int a = Mth.clamp((int) (alpha * 255f), 0, 255) << 24;
        PropDraw.quad(pose, vc, -r, 0.02f, r, r, 0.02f, r, r, 0.02f, -r, -r, 0.02f, -r, 0f, 0f, 1f, 1f, a | 0x101008, light);
    }

    private static void paint(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        int p = e.param(), type = EarthConstruct.type(p);
        if (!EarthConstruct.valid(type)) return;
        int style = EarthConstruct.style(type);
        float s = Math.max(0.3f, e.scale());
        float maxL = Math.max(1f, e.maxLife());
        long now = e.level().getGameTime();
        int st = EarthConstruct.state(p), since = EarthConstruct.since(p, now);
        boolean shot = type == EarthConstruct.BOULDER || type == EarthConstruct.LANCE, cage = type == EarthConstruct.CAGE;
        float rise = shot || cage ? 1f : smooth(age / 12f);
        float leave = Mth.clamp((age - (maxL - 22f)) / 22f, 0f, 1f);                                     // 0 while it stands, 1 when it is gone
        float k = leave > 0f && !shot ? 1f - 0.35f * leave : 1f;
        String clip = "idle";
        float secs = age / 20f;
        switch (type) {
            case EarthConstruct.GOLEM, EarthConstruct.SPHINX -> {
                if (st == EarthConstruct.ACT && since < 24) { clip = "attack"; secs = (since + partial) / 20f; } else if (moving(e)) clip = "walk";
            }
            case EarthConstruct.TITAN -> {
                if (st == EarthConstruct.ACT && since < 24) { clip = "attack"; secs = (since + partial) / 20f; }
                else if (st == EarthConstruct.ACT2 && since < 24) { clip = "roar"; secs = (since + partial) / 20f; } else if (moving(e)) clip = "walk";
            }
            case EarthConstruct.SENTINEL -> {
                if (st == EarthConstruct.ACT && since < 24) { clip = "attack"; secs = (since + partial) / 20f; }
                else if (st == EarthConstruct.ACT2 && since < 24) { clip = "block"; secs = (since + partial) / 20f; } else if (moving(e)) clip = "walk";
            }
            case EarthConstruct.CAGE -> {
                float t = age + partial;
                if (t < 14f) { clip = "form"; secs = t / 20f; } else if (maxL - t < 18f) { clip = "crumble"; secs = (t - (maxL - 18f)) / 20f; }
            }
            case EarthConstruct.TOTEM -> { if (st == EarthConstruct.ACT && since < 14) { clip = "attack"; secs = (since + partial) / 20f; } }
            case EarthConstruct.SPIRE -> {
                if (age < 11f) { clip = "grow"; secs = (age + partial) / 20f; } else if (st == EarthConstruct.ACT && since < 10) { clip = "pulse"; secs = (since + partial) / 20f; }
            }
            default -> { }
        }
        if (!shot && !cage) shade(pose, buffers, 0.8f * s * (0.4f + 0.6f * rise), 0.35f * rise * (1f - leave), light);
        pose.pushPose();
        if (shot) {                                                                                       // turn the body to follow its flight (pitch from the movement)
            double dx = e.getX() - e.xo, dy = e.getY() - e.yo, dz = e.getZ() - e.zo;
            float pitch = (float) Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz) + 1e-4));
            pose.translate(0f, 0.45f * s, 0f);
            pose.mulPose(Axis.XP.rotationDegrees(-pitch));
            pose.translate(0f, -0.45f * s, 0f);
        }
        pose.translate(0f, -(1f - rise) * 2.4f * s - leave * 2.2f * s, 0f);
        float sc = s * k * (shot ? 0.8f : 1f);
        pose.scale(sc, sc, sc);
        float pulse = 0.65f + 0.35f * Mth.sin(age * 0.12f + e.seed());
        int glowRgb = EarthFxStyle.glow(style);
        int glow = AuraRender.alpha(glowRgb, pulse * (1f - leave) * (clip.equals("attack") || clip.equals("pulse") ? 1.4f : 1f));
        GeoDraw.paint(pose, buffers, SPEC[type], clip, secs, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, glow);
        pose.popPose();
        extras(e, partial, age, pose, buffers, type, style, s, leave, st, since, light);
    }

    // ================================================================================================ the extras of each magic
    private static void extras(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int type, int style, float s, float leave, int st, int since, int light) {
        SealDraw.camera(pose);
        float fade = 1f - leave;
        int glowRgb = EarthFxStyle.glow(style), base = EarthFxStyle.color(style) & 0xFFFFFF;
        float seed = e.seed() * 0.37f;
        switch (type) {
            case EarthConstruct.SPHINX, EarthConstruct.CAGE -> {                                         // sand pouring: strands falling off the body / the roof
                VertexConsumer vc = buffers.getBuffer(AuraRender.translucent(tex(EarthFxStyle.SANDSTONE, "stream")));
                int n = type == EarthConstruct.CAGE ? 6 : 4;
                float w = type == EarthConstruct.CAGE ? 1.0f : 0.55f, top = type == EarthConstruct.CAGE ? 2.05f : 1.3f;
                for (int i = 0; i < n; i++) {
                    float ph = (age * 0.03f + i * 0.37f) % 1f, a = i * 2.399f + seed;
                    float x = Mth.cos(a) * w * s, z = Mth.sin(a) * w * s;
                    SealDraw.ribbon(pose, vc, x, top * s, z, x, (top - 0.9f - 0.7f * ph) * s, z, 0.06f * s, 0f, 1f, SealDraw.argb(0.7f * fade * Mth.sin(ph * Mth.PI), base), light);
                }
                if (type == EarthConstruct.CAGE && age > 8f) {                                            // the hourglass sigil turning under the block
                    VertexConsumer sg = buffers.getBuffer(AuraRender.additive(tex(EarthFxStyle.SANDSTONE, "sigil")));
                    pose.pushPose();
                    pose.translate(0f, 0.04f, 0f);
                    SealDraw.flat(pose, sg, 1.75f * s, age * 2.2f, SealDraw.argb(0.7f * fade * smooth((age - 8f) / 8f), glowRgb), FB);
                    pose.popPose();
                }
            }
            case EarthConstruct.TOTEM -> {                                                                // pigment drops falling off the totem, a handprint hanging over it
                VertexConsumer dr = buffers.getBuffer(AuraRender.translucent(tex(EarthFxStyle.RED_OCHRE, "drop")));
                for (int i = 0; i < 4; i++) {
                    float ph = (age * 0.025f + i * 0.27f) % 1f, a = i * 1.7f + seed;
                    SealDraw.glow(pose, dr, Mth.cos(a) * 0.5f * s, (2.2f - 1.8f * ph) * s, Mth.sin(a) * 0.5f * s, 0.07f * s, 0f, SealDraw.argb(0.8f * fade * (1f - ph), base), light);
                }
                VertexConsumer hd = buffers.getBuffer(AuraRender.additive(tex(EarthFxStyle.RED_OCHRE, "hand")));
                float lift = 4.0f * s + 0.12f * Mth.sin(age * 0.1f);
                SealDraw.glow(pose, hd, 0f, lift, 0f, (0.55f + (st == EarthConstruct.ACT && since < 12 ? 0.25f : 0f)) * s, Mth.sin(age * 0.05f) * 0.2f, SealDraw.argb(0.55f * fade, glowRgb), FB);
            }
            case EarthConstruct.SPIRE -> {                                                                // glints and a lattice ring on the ground
                VertexConsumer fl = buffers.getBuffer(AuraRender.additive(tex(EarthFxStyle.MINERAL, "flare")));
                for (int i = 0; i < 5; i++) {
                    float tw = Mth.sin(age * (0.14f + 0.03f * i) + i * 2f + seed);
                    tw = tw * tw * tw;
                    if (tw <= 0.05f) continue;
                    float a = i * 2.399f + seed;
                    SealDraw.glow(pose, fl, Mth.cos(a) * 0.45f * s, (0.6f + 0.55f * i) * s, Mth.sin(a) * 0.45f * s, 0.32f * s * tw, age * 0.02f, SealDraw.argb(tw * fade, glowRgb), FB);
                }
                VertexConsumer rg = buffers.getBuffer(AuraRender.additive(tex(EarthFxStyle.MINERAL, "circle")));
                pose.pushPose();
                pose.translate(0f, 0.04f, 0f);
                float pul = st == EarthConstruct.ACT && since < 12 ? 1.25f - since * 0.02f : 1f;
                SealDraw.flat(pose, rg, 1.8f * s * pul, age * 1.2f, SealDraw.argb(0.55f * fade * smooth(age / 10f), glowRgb), FB);
                pose.popPose();
            }
            case EarthConstruct.BOULDER -> {                                                              // a dust trail pulled out behind the body (the prop's forward is +z)
                VertexConsumer du = buffers.getBuffer(AuraRender.translucent(tex(EarthFxStyle.ROCK, "dust")));
                for (int i = 0; i < 4; i++) {
                    float t = i * 0.3f;
                    SealDraw.glow(pose, du, 0f, (0.45f + 0.04f * i) * s, -(0.7f + t * 1.3f) * s, (0.38f + 0.12f * i) * s, i * 1.3f + age * 0.1f, SealDraw.argb(0.45f * (1f - i * 0.22f), base), light);
                }
            }
            case EarthConstruct.LANCE -> {
                VertexConsumer sk = buffers.getBuffer(AuraRender.additive(tex(EarthFxStyle.MINERAL, "flare")));
                for (int i = 0; i < 4; i++)
                    SealDraw.glow(pose, sk, 0f, 0.3f * s, -(0.4f + i * 0.55f) * s, (0.5f - i * 0.1f) * s, age * 0.2f + i, SealDraw.argb(0.8f - i * 0.18f, glowRgb), FB);
            }
            case EarthConstruct.GOLEM -> {                                                                // motes of soil lifted off the shoulders, a gold core pulse
                VertexConsumer cl = buffers.getBuffer(AuraRender.translucent(tex(EarthFxStyle.EARTH, "clod")));
                for (int i = 0; i < 3; i++) {
                    float ph = (age * 0.02f + i * 0.33f) % 1f, a = i * 2.1f + seed;
                    SealDraw.glow(pose, cl, Mth.cos(a) * 0.9f * s, (2.0f + ph * 0.9f) * s, Mth.sin(a) * 0.5f * s, 0.07f * s, age * 0.1f + i, SealDraw.argb(0.8f * fade * Mth.sin(ph * Mth.PI), base), light);
                }
            }
            case EarthConstruct.TITAN -> {                                                                // grit falling off the crags
                VertexConsumer ch = buffers.getBuffer(AuraRender.translucent(tex(EarthFxStyle.ROCK, "chunk")));
                for (int i = 0; i < 4; i++) {
                    float ph = (age * 0.03f + i * 0.27f) % 1f, a = i * 1.7f + seed;
                    SealDraw.glow(pose, ch, Mth.cos(a) * 1.0f * s, (2.8f - 2.6f * ph) * s, Mth.sin(a) * 0.7f * s, 0.07f * s, age * 0.2f + i, SealDraw.argb(0.85f * fade * (1f - ph), 0x606670), light);
                }
                if (st == EarthConstruct.ACT2 && since < 20) {
                    VertexConsumer rg = buffers.getBuffer(AuraRender.additive(tex(EarthFxStyle.ROCK, "ring")));
                    pose.pushPose();
                    pose.translate(0f, 0.05f, 0f);
                    SealDraw.flat(pose, rg, (1.5f + since * 0.2f) * s, 0f, SealDraw.argb(0.7f * (1f - since / 20f), glowRgb), FB);
                    pose.popPose();
                }
            }
            case EarthConstruct.SENTINEL -> {
                if (st == EarthConstruct.ACT2 && since < 14) {
                    VertexConsumer rn = buffers.getBuffer(AuraRender.additive(tex(EarthFxStyle.STONE, "circle")));
                    pose.pushPose();
                    pose.translate(0f, 1.4f * s, -1.1f * s);
                    SealDraw.glow(pose, rn, 0f, 0f, 0f, 1.3f * s, since * 0.1f, SealDraw.argb(0.8f * (1f - since / 14f), glowRgb), FB);
                    pose.popPose();
                }
            }
            default -> { }
        }
    }
}
