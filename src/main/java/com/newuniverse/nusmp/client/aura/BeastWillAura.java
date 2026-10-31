package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.client.prop.SealDraw;
import com.newuniverse.nusmp.perf.PerfProbe;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Why: the look of a Beast Will form (3.45.0), {@code Aura.BEAST_WILL}. The server keeps the lease alive while the form is on and passes {@code style = (phase << 24) | tint}, so this painter needs no copy
 * of the data files: phase 1 (Acquire) is subtle (two glowing eyes in the will's colour and a few slow motes round the body); phase 2 (Integrate) is stronger (bigger eyes, ten motes, a haze behind
 * the body and two turning rings on the ground). TEMPORARY art: every sprite is an existing white-ish glow texture tinted by the will's colour (air_glint, air_orb, air_ring, beast_haze); a
 * per-creature model (mane, panther ears, antlers, horns) is the next step and needs real art. Additive and full bright, nothing is drawn over the face from the player's own camera. Cost per player
 * per frame: at most 2 eye quads, 10 mote quads, 1 haze quad and 2 flat rings (4 quads), and nothing beyond 48 blocks; between 24 and 48 blocks only the eyes and 4 motes. No allocation.
 */
public final class BeastWillAura {
    private BeastWillAura() {}

    private static final PerfProbe.Section SECTION = PerfProbe.section("c.beastwill.aura");
    private static ResourceLocation glint, orb, ring, haze;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        glint = AuraRender.tex("particle/air_glint");
        orb = AuraRender.tex("particle/air_orb");
        ring = AuraRender.tex("particle/air_ring");
        haze = AuraRender.tex("particle/beast_haze");
        PlayerAuraClient.register(Aura.BEAST_WILL, BeastWillAura::paint);
    }

    private static float frac(float x) { return x - (float) Math.floor(x); }

    private static void paint(AuraContext c) {
        int phase = (c.style() >>> 24) & 0xFF;
        float f = c.fade();
        if (f <= 0.002f || phase < 1 || phase > 2 || c.player().isInvisible()) return;
        double d2 = SpiritDiveFrame.distSq(c);
        if (d2 > 48.0 * 48.0) return;
        long t0 = PerfProbe.begin();
        try {
            draw(c, phase, c.style() & 0xFFFFFF, f, d2 > 24.0 * 24.0);
        } finally {
            PerfProbe.end(SECTION, t0);
        }
    }

    private static void draw(AuraContext c, int phase, int rgb, float f, boolean far) {
        PoseStack pose = c.pose();
        float age = c.age(), pulse = 0.8f + 0.2f * Mth.sin(age * 0.2f);
        boolean self1p = RebuildSetKit.firstPersonSelf(c);
        int light = LightTexture.FULL_BRIGHT;
        int bright = SealDraw.mix(rgb, 0xFFFFFF, 0.35f);
        boolean strong = phase == 2;
        SealDraw.camera(pose);

        // 1. the eyes: two glints at eye height in front of the face, turned with the head
        if (!self1p) {
            VertexConsumer ev = c.buffers().getBuffer(AuraRender.additive(glint));
            float yaw = c.netHeadYaw() * Mth.DEG_TO_RAD, cs = Mth.cos(yaw), sn = Mth.sin(yaw);
            float size = (strong ? 0.13f : 0.075f) * pulse;
            for (int side = -1; side <= 1; side += 2) {
                float lx = 0.1f * side, lz = -0.27f;
                SealDraw.glow(pose, ev, lx * cs + lz * sn, -0.12f, -lx * sn + lz * cs, size, 0f, SealDraw.argb(0.95f * f, bright), light);
            }
        }

        // 2. motes drifting up round the body
        int motes = far ? 4 : strong ? 10 : 5;
        VertexConsumer mv = c.buffers().getBuffer(AuraRender.additive(orb));
        for (int i = 0; i < motes; i++) {
            float ph = frac(age * (strong ? 0.016f : 0.011f) + i * 0.137f), a = i * 2.399f + age * 0.025f * (i % 2 == 0 ? 1f : -1f);
            float r = (strong ? 0.55f : 0.48f) + 0.08f * Mth.sin(i * 1.7f + age * 0.05f), y = 1.4f - ph * 1.9f;
            if (self1p && y < 0.35f) continue;
            float size = (strong ? 0.075f : 0.055f) * (0.6f + 0.6f * Mth.sin(ph * Mth.PI));
            SealDraw.glow(pose, mv, Mth.cos(a) * r, y, Mth.sin(a) * r * 0.85f, size, 0f, SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.8f * f, rgb), light);
        }
        if (!strong || far) return;

        // 3. phase 2: a haze behind the body and two rings turning on the ground
        VertexConsumer hv = c.buffers().getBuffer(AuraRender.additive(haze));
        SealDraw.glow(pose, hv, 0f, 0.15f, 0.05f, 0.95f, age * 0.004f, SealDraw.argb(0.16f * f * pulse, rgb), light);
        VertexConsumer rv = c.buffers().getBuffer(AuraRender.additive(ring));
        pose.pushPose();
        pose.translate(0f, 1.47f, 0f);
        SealDraw.flat(pose, rv, 0.95f, age * 1.1f, SealDraw.argb(0.55f * f, rgb), light);
        SealDraw.flat(pose, rv, 0.62f, -age * 1.8f, SealDraw.argb(0.45f * f, bright), light);
        pose.popPose();
    }
}
