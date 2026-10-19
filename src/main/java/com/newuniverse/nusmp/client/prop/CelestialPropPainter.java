package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.celestial.CelestialKey;
import com.newuniverse.nusmp.celestial.CelestialProps;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.EnumMap;
import java.util.Map;

/**
 * Celestial Spirit Magic: how the spirits, the Gate and the summoned Spirit King look. Every spirit is a GeoLite model of its own ({@code celestial_<id>}, built by
 * tools/celestial/gen_celestial_geo.py) with four clips (summon, cast, idle, leave) and an emissive glow map, and it always stands in its magic circle:
 * <ol>
 *   <li>the circle on the ground: the zodiac wheel (twelve constellations round two rings of runes) turning slowly, a second ring turning against it and the constellation
 *       of the spirit in the middle, all additive and full bright, in the colour of the key; they open while the spirit steps out and close when it leaves;</li>
 *   <li>a shaft of starlight that rises through the circle while the spirit steps out and fades;</li>
 *   <li>the model, rising out of the circle (summon), casting (a clip of 1.2 s starting at the cast), standing (idle) and sinking back (leave), its glow map pulsing;</li>
 *   <li>a camera-facing star glint at the heart at the moment of the cast.</li>
 * </ol>
 * CELESTIAL_2 (param bit 0): 0 the Gate (a tall arched door of gold and white stone with a keyhole of light, opening as the ritual runs, a large circle round it), 1 the
 * Spirit King called as a summon (his third-phase model, a circle six blocks wide). Nothing here allocates beyond vertex data; every texture is made by
 * tools/celestial/gen_celestial_art.py (particle/celestial_*.png) and tools/celestial/gen_celestial_geo.py (entity/celestial_*.png).
 */
public final class CelestialPropPainter {
    private CelestialPropPainter() {}

    private static final int FB = PropDraw.FULL_BRIGHT, GOLD = 0xFFE27A, WHITE = 0xFFFFFF;
    private static final Map<CelestialKey, GeoSpec> SPECS = new EnumMap<>(CelestialKey.class);
    private static final Map<CelestialKey, ResourceLocation> SIGNS = new EnumMap<>(CelestialKey.class);
    private static final GeoSpec GATE = GeoSpec.of("celestial", "gate"), KING = GeoSpec.of("celestial", "king_p3");
    private static ResourceLocation circle, ring, glint, shaft, door, kingSign;

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        circle = particle("celestial_circle");
        ring = particle("celestial_ring");
        glint = particle("celestial_glint");
        shaft = particle("celestial_shaft");
        door = particle("celestial_door");
        kingSign = particle("celestial_sign_king");
        for (CelestialKey k : CelestialKey.values()) {
            if (!k.summons()) continue;
            SPECS.put(k, GeoSpec.of("celestial", k.id));
            SIGNS.put(k, particle("celestial_sign_" + k.id));
        }
        PropPainters.register(PropKind.CELESTIAL_1, CelestialPropPainter::spirit);
        PropPainters.register(PropKind.CELESTIAL_2, CelestialPropPainter::gate);
    }

    private static ResourceLocation particle(String n) { return AuraRender.tex("particle/" + n); }

    // ================================================================ a spirit
    private static void spirit(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        CelestialKey key = CelestialKey.byOrdinal(e.param() & 0xFF);
        GeoSpec spec = key == null ? null : SPECS.get(key);
        if (spec == null) return;
        int phase = (e.param() >> CelestialProps.PHASE_SHIFT) & 3, since = (e.param() >> CelestialProps.SINCE_SHIFT) & 255;
        float life = e.life() + partial, max = Math.max(1, e.maxLife()), s = e.scale();
        float in = KeyDraw.smooth(life / CelestialProps.ENTER), out = KeyDraw.smooth((max - life) / CelestialProps.LEAVE);
        float k = Math.min(in, out);                                                          // 0 hidden .. 1 fully there
        if (k <= 0.01f) return;
        float pulse = 0.75f + 0.25f * Mth.sin(age * 0.2f);
        int rgb = key.rgb;
        KeyDraw.camera(pose);

        // 1. the circle on the ground: the wheel, the counter ring and the sign of the spirit
        float open = Math.min(KeyDraw.smooth(life / 10f), KeyDraw.smooth((max - life) / 10f));
        pose.pushPose();
        pose.translate(0f, 0.04f, 0f);
        KeyDraw.flat(pose, buf.getBuffer(AuraRender.additive(circle)), 2.2f * s * (0.55f + 0.45f * open), age * 0.9f, KeyDraw.argb(0.85f * open, KeyDraw.mix(rgb, WHITE, 0.25f)), FB);
        pose.translate(0f, 0.01f, 0f);
        KeyDraw.flat(pose, buf.getBuffer(AuraRender.additive(ring)), 1.5f * s * (0.55f + 0.45f * open), -age * 1.7f, KeyDraw.argb(0.65f * open * pulse, KeyDraw.mix(rgb, GOLD, 0.5f)), FB);
        pose.translate(0f, 0.01f, 0f);
        KeyDraw.flat(pose, buf.getBuffer(AuraRender.additive(SIGNS.get(key))), 0.75f * s, 0f, KeyDraw.argb(0.9f * open, WHITE), FB);
        pose.popPose();

        // 2. the shaft of starlight while the spirit steps out
        if (life < CelestialProps.ENTER + 8) {
            float sh = Mth.clamp(1f - Math.max(0f, life - CelestialProps.ENTER * 0.5f) / (CelestialProps.ENTER * 0.5f + 8f), 0f, 1f) * in;
            KeyDraw.upright(pose, buf.getBuffer(AuraRender.additive(shaft)), 0f, 1.9f * s, 0f, 0.55f * s, 1.9f * s, KeyDraw.argb(0.8f * sh, KeyDraw.mix(rgb, WHITE, 0.4f)), FB);
        }

        // 3. the model: rising out of the circle, casting, standing, sinking back
        String clip;
        float clock;
        if (phase == 0) { clip = "summon"; clock = life / 20f; }
        else if (phase == 1) { clip = "cast"; clock = (since + partial) / 20f; }
        else if (phase == 2) { clip = "idle"; clock = age / 20f; }
        else { clip = "leave"; clock = Math.max(0f, life - (max - CelestialProps.LEAVE)) / 20f; }
        float glowA = Mth.clamp(k * (0.7f + 0.3f * pulse), 0f, 1f);
        pose.pushPose();
        pose.translate(0f, -(1f - in) * 0.9f * s - (1f - out) * 0.5f * s, 0f);
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buf, spec, clip, clock, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, KeyDraw.argb(glowA, KeyDraw.mix(WHITE, rgb, 0.45f)));
        pose.popPose();

        // 4. a star glint at the heart at the cast
        if (phase == 1 && since < 14) {
            float g = 1f - since / 14f;
            KeyDraw.glow(pose, buf.getBuffer(AuraRender.additive(glint)), 0f, 1.1f * s, 0f, (0.5f + 1.5f * g) * s, age * 0.06f, KeyDraw.argb(0.9f * g * k, KeyDraw.mix(rgb, WHITE, 0.5f)), FB);
        }
        KeyDraw.glow(pose, buf.getBuffer(AuraRender.additive(glint)), 0f, 1.1f * s, 0f, 0.35f * s * pulse, -age * 0.03f, KeyDraw.argb(0.4f * k, rgb), FB);
    }

    // ================================================================ the Gate and the king's summon
    private static void gate(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        boolean king = (e.param() & 1) != 0;
        float life = e.life() + partial, max = Math.max(1, e.maxLife()), s = e.scale();
        float in = KeyDraw.smooth(life / 24f), out = KeyDraw.smooth((max - life) / 20f), k = Math.min(in, out);
        if (k <= 0.01f) return;
        float pulse = 0.75f + 0.25f * Mth.sin(age * 0.17f);
        KeyDraw.camera(pose);
        float radius = (king ? 3.2f : 3.0f) * (king ? s * 0.45f : 1f);
        pose.pushPose();
        pose.translate(0f, 0.05f, 0f);
        KeyDraw.flat(pose, buf.getBuffer(AuraRender.additive(circle)), radius * (0.5f + 0.5f * in), age * 0.5f, KeyDraw.argb(0.9f * k, GOLD), FB);
        pose.translate(0f, 0.01f, 0f);
        KeyDraw.flat(pose, buf.getBuffer(AuraRender.additive(ring)), radius * 0.62f * (0.5f + 0.5f * in), -age * 1.1f, KeyDraw.argb(0.7f * k * pulse, KeyDraw.mix(GOLD, WHITE, 0.4f)), FB);
        pose.translate(0f, 0.01f, 0f);
        KeyDraw.flat(pose, buf.getBuffer(AuraRender.additive(kingSign)), radius * 0.32f, age * 0.25f, KeyDraw.argb(0.9f * k, WHITE), FB);
        pose.popPose();

        if (king) {
            String clip = life < 36f ? "summon" : "idle";
            pose.pushPose();
            pose.translate(0f, -(1f - in) * 2.0f * s, 0f);
            pose.scale(s * 0.5f, s * 0.5f, s * 0.5f);
            GeoDraw.paint(pose, buf, KING, clip, life < 36f ? life / 20f : age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, KeyDraw.argb(k * pulse, GOLD));
            pose.popPose();
            KeyDraw.upright(pose, buf.getBuffer(AuraRender.additive(shaft)), 0f, 4.0f * s * 0.5f, 0f, 1.1f * s * 0.5f, 4.0f * s * 0.5f, KeyDraw.argb(0.45f * k * pulse, GOLD), FB);
        } else {
            float open = KeyDraw.smooth((life - 24f) / 30f) * out;
            String clip = life < 24f ? "idle" : life < 60f ? "open" : "open_hold";
            float clock = life < 24f ? age / 20f : life < 60f ? (life - 24f) / 20f : age / 20f;
            pose.pushPose();
            pose.translate(0f, -(1f - in) * 3.4f * s, 0f);
            pose.scale(s, s, s);
            GeoDraw.paint(pose, buf, GATE, clip, clock, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, KeyDraw.argb(Mth.clamp(k * (0.7f + 0.3f * pulse), 0f, 1f), GOLD));
            pose.popPose();
            if (open > 0.02f) {
                VertexConsumer dv = buf.getBuffer(AuraRender.additive(door));
                KeyDraw.upright(pose, dv, 0f, 2.3f * s, -0.04f * s, 1.0f * s, 2.0f * s, KeyDraw.argb(Mth.clamp(open * (0.55f + 0.35f * pulse), 0f, 1f), KeyDraw.mix(GOLD, WHITE, 0.5f)), FB);
                KeyDraw.glow(pose, buf.getBuffer(AuraRender.additive(glint)), 0f, 2.2f * s, 0.1f * s, (0.9f + 0.5f * pulse) * s, age * 0.04f, KeyDraw.argb(open * 0.8f, WHITE), FB);
            }
            KeyDraw.upright(pose, buf.getBuffer(AuraRender.additive(shaft)), 0f, 4.4f * s, 0f, 0.9f * s, 4.4f * s, KeyDraw.argb(0.35f * k * pulse, GOLD), FB);
        }
        // twelve stars orbiting the circle
        VertexConsumer gv = buf.getBuffer(AuraRender.additive(glint));
        for (int i = 0; i < 12; i++) {
            float a = age * 0.03f + i * Mth.TWO_PI / 12f;
            KeyDraw.glow(pose, gv, Mth.cos(a) * radius * 0.9f, 0.4f + 0.25f * Mth.sin(age * 0.07f + i), Mth.sin(a) * radius * 0.9f, 0.2f + 0.08f * Mth.sin(age * 0.2f + i), a * 3f,
                    KeyDraw.argb(0.7f * k, KeyDraw.mix(GOLD, WHITE, (i % 3) / 3f)), FB);
        }
    }
}
