package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.beastwill.BeastWillLook;
import com.newuniverse.nusmp.client.prop.SealDraw;
import com.newuniverse.nusmp.perf.PerfProbe;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Why: the look of a Beast Will form, {@code Aura.BEAST_WILL}. The server keeps the lease alive while the form is on and passes {@code style} = phase (bits 24..25), look kind (bits 26..29, see
 * {@link BeastWillLook}) and tint (bits 0..23), so this painter needs no copy of the data files.
 * <ul>
 *   <li>OLD look (3.45.0, KEPT, still used for a will whose kind is unknown): two glowing eyes, slow motes, and in phase 2 a haze and two turning rings. Unchanged.</li>
 *   <li>NEW look (3.57.0): the same eyes and motes (fewer in phase 2), plus the animal spirit of that will behind the body: phase 1 (Acquire) a translucent silhouette of the beast; phase 2 (Integrate) a
 *       bigger, brighter, animated spirit with its accent layer (dragon wings, panther smoke, antlers and roots, lion mane, frost moon, dust and speed lines, raptor wing blades, serpent flames,
 *       chimera lightning) and a few specks that move the way that beast's element does (embers rise, leaves fall, shards orbit, dust rolls, bolts flicker, wind ribbons orbit). The textures are
 *       white glow maps made by tools/beastwill/gen_spirit_textures.py and tinted with the will's colour.</li>
 * </ul>
 * Cost per player per frame (nothing at all beyond 48 blocks, for an invisible player; no spirit from the owner's own first-person camera): FULL detail (at most {@link #FULL_PER_FRAME} players a frame,
 * within 24 blocks, phase 2): 2 eye quads + 6 motes + 1 body + 1 accent + 8 specks + 4 ring quads = at most 22 quads (the raptor's 4 ribbons replace the 8 specks: 18). LIGHT detail (phase 1, between 24 and
 * 48 blocks, or over the per-frame budget): 2 eyes + 4 or 5 motes + 1 body (+ 1 accent in phase 2) = at most 8. So with N players in view the worst frame is 6 * 22 + (N - 6) * 8 quads. A will without a
 * spirit keeps the old look: at most 17 quads. No allocation (maths on locals, sprite locations built once).
 */
public final class BeastWillAura {
    private BeastWillAura() {}

    private static final PerfProbe.Section SECTION = PerfProbe.section("c.beastwill.aura");
    private static final PerfProbe.Section SPIRIT = PerfProbe.section("c.beastwill.spirit");
    /** At most this many players a frame draw the full spirit (accent layer, specks); the rest draw the light one. */
    public static final int FULL_PER_FRAME = 6;
    private static final int KINDS = BeastWillLook.COUNT;

    private static ResourceLocation glint, orb, ring, haze;
    private static final ResourceLocation[] BODY = new ResourceLocation[KINDS], ACC = new ResourceLocation[KINDS], SPECK = new ResourceLocation[KINDS];

    /** The accent colour of each kind (the accent layer and the specks are the will's tint mixed toward it). Index = kind. */
    private static final int[] ACCENT = {0, 0xFF8A3A, 0x9A6CFF, 0x8FE06A, 0xFFE6A0, 0xE8F6FF, 0xC89B5A, 0xFFFFFF, 0xFFC24A, 0xDDEEFF};
    /** How the specks of each kind move: 0 rise (embers, smoke), 1 fall (leaves), 2 orbit (shards), 3 flicker (bolts), 4 roll along the ground (dust), 5 wind ribbons. Index = kind. */
    private static final int[] MODE = {0, 0, 0, 1, 2, 2, 4, 5, 0, 3};

    private static long frame, lastMove, taken;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        glint = AuraRender.tex("particle/air_glint");
        orb = AuraRender.tex("particle/air_orb");
        ring = AuraRender.tex("particle/air_ring");
        haze = AuraRender.tex("particle/beast_haze");
        // one sprite pair per will (kind order of BeastWillLook), the specks they share
        BODY[BeastWillLook.DRAGON] = AuraRender.tex("particle/bwspirit_dragon_a");
        ACC[BeastWillLook.DRAGON] = AuraRender.tex("particle/bwspirit_dragon_b");
        BODY[BeastWillLook.PANTHER] = AuraRender.tex("particle/bwspirit_panther_a");
        ACC[BeastWillLook.PANTHER] = AuraRender.tex("particle/bwspirit_panther_b");
        BODY[BeastWillLook.ELDERWOOD] = AuraRender.tex("particle/bwspirit_elder_a");
        ACC[BeastWillLook.ELDERWOOD] = AuraRender.tex("particle/bwspirit_elder_b");
        BODY[BeastWillLook.LION] = AuraRender.tex("particle/bwspirit_lion_a");
        ACC[BeastWillLook.LION] = AuraRender.tex("particle/bwspirit_lion_b");
        BODY[BeastWillLook.WOLF] = AuraRender.tex("particle/bwspirit_wolf_a");
        ACC[BeastWillLook.WOLF] = AuraRender.tex("particle/bwspirit_wolf_b");
        BODY[BeastWillLook.BOAR] = AuraRender.tex("particle/bwspirit_boar_a");
        ACC[BeastWillLook.BOAR] = AuraRender.tex("particle/bwspirit_boar_b");
        BODY[BeastWillLook.RAPTOR] = AuraRender.tex("particle/bwspirit_raptor_a");
        ACC[BeastWillLook.RAPTOR] = AuraRender.tex("particle/bwspirit_raptor_b");
        BODY[BeastWillLook.SERPENT] = AuraRender.tex("particle/bwspirit_serpent_a");
        ACC[BeastWillLook.SERPENT] = AuraRender.tex("particle/bwspirit_serpent_b");
        BODY[BeastWillLook.CHIMERA] = AuraRender.tex("particle/bwspirit_chimera_a");
        ACC[BeastWillLook.CHIMERA] = AuraRender.tex("particle/bwspirit_chimera_b");
        ResourceLocation ember = AuraRender.tex("particle/bwspirit_ember"), dust = AuraRender.tex("particle/bwspirit_dust"), leaf = AuraRender.tex("particle/bwspirit_leaf"),
                shard = AuraRender.tex("particle/bwspirit_shard"), bolt = AuraRender.tex("particle/bwspirit_bolt"), ribbon = AuraRender.tex("particle/bwspirit_ribbon");
        SPECK[BeastWillLook.DRAGON] = ember;
        SPECK[BeastWillLook.PANTHER] = dust;
        SPECK[BeastWillLook.ELDERWOOD] = leaf;
        SPECK[BeastWillLook.LION] = shard;
        SPECK[BeastWillLook.WOLF] = shard;
        SPECK[BeastWillLook.BOAR] = dust;
        SPECK[BeastWillLook.RAPTOR] = ribbon;
        SPECK[BeastWillLook.SERPENT] = ember;
        SPECK[BeastWillLook.CHIMERA] = bolt;
        PlayerAuraClient.register(Aura.BEAST_WILL, BeastWillAura::paint);
    }

    private static float frac(float x) { return x - (float) Math.floor(x); }

    /** May this player draw the full spirit in this frame? (the frame counter is the one the Spirit Dive painters share; when it stops moving for half a second every player is admitted) */
    private static boolean admitFull() {
        long f = SpiritDiveFrame.frame(), now = System.nanoTime();
        if (f != frame) {
            frame = f;
            taken = 0;
            lastMove = now;
        } else if (now - lastMove > 500_000_000L) {
            return true;
        }
        return taken++ < FULL_PER_FRAME;
    }

    private static void paint(AuraContext c) {
        int phase = BeastWillLook.phase(c.style());
        float f = c.fade();
        if (f <= 0.002f || phase < 1 || phase > 2 || c.player().isInvisible()) return;
        double d2 = SpiritDiveFrame.distSq(c);
        if (d2 > 48.0 * 48.0) return;
        long t0 = PerfProbe.begin();
        try {
            draw(c, phase, BeastWillLook.kind(c.style()), BeastWillLook.tint(c.style()), f, d2 > 24.0 * 24.0);
        } finally {
            PerfProbe.end(SECTION, t0);
        }
    }

    private static void draw(AuraContext c, int phase, int kind, int rgb, float f, boolean far) {
        PoseStack pose = c.pose();
        float age = c.age(), pulse = 0.8f + 0.2f * Mth.sin(age * 0.2f);
        boolean self1p = RebuildSetKit.firstPersonSelf(c);
        int light = LightTexture.FULL_BRIGHT;
        int bright = SealDraw.mix(rgb, 0xFFFFFF, 0.35f);
        boolean strong = phase == 2;
        boolean spirit = kind > 0 && BODY[kind] != null;
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

        // 2. motes drifting up round the body (fewer when the spirit is there to carry the picture)
        int motes = far ? 4 : strong ? (spirit ? 6 : 10) : 5;
        VertexConsumer mv = c.buffers().getBuffer(AuraRender.additive(orb));
        for (int i = 0; i < motes; i++) {
            float ph = frac(age * (strong ? 0.016f : 0.011f) + i * 0.137f), a = i * 2.399f + age * 0.025f * (i % 2 == 0 ? 1f : -1f);
            float r = (strong ? 0.55f : 0.48f) + 0.08f * Mth.sin(i * 1.7f + age * 0.05f), y = 1.4f - ph * 1.9f;
            if (self1p && y < 0.35f) continue;
            float size = (strong ? 0.075f : 0.055f) * (0.6f + 0.6f * Mth.sin(ph * Mth.PI));
            SealDraw.glow(pose, mv, Mth.cos(a) * r, y, Mth.sin(a) * r * 0.85f, size, 0f, SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.8f * f, rgb), light);
        }

        // 3. the animal spirit (known wills only; never from the owner's own first-person camera)
        if (spirit && !self1p) {
            long s0 = PerfProbe.begin();
            try {
                spirit(c, pose, phase, kind, rgb, f, pulse, age, light, !far && strong && admitFull());
            } finally {
                PerfProbe.end(SPIRIT, s0);
            }
        }
        if (!strong || far) return;

        // 4. phase 2: the old haze (only for a will without a spirit) and the two rings turning on the ground
        if (!spirit) {
            VertexConsumer hv = c.buffers().getBuffer(AuraRender.additive(haze));
            SealDraw.glow(pose, hv, 0f, 0.15f, 0.05f, 0.95f, age * 0.004f, SealDraw.argb(0.16f * f * pulse, rgb), light);
        }
        VertexConsumer rv = c.buffers().getBuffer(AuraRender.additive(ring));
        pose.pushPose();
        pose.translate(0f, 1.47f, 0f);
        SealDraw.flat(pose, rv, 0.95f, age * 1.1f, SealDraw.argb(0.55f * f, rgb), light);
        SealDraw.flat(pose, rv, 0.62f, -age * 1.8f, SealDraw.argb(0.45f * f, bright), light);
        pose.popPose();
    }

    /** A camera-facing sprite with its own width and height (half extents), turned by rot radians. y is down in this pose space. */
    private static void sprite(PoseStack pose, VertexConsumer vc, float cx, float cy, float cz, float hw, float hh, float rot, int argb, int light) {
        float c = Mth.cos(rot), s = Mth.sin(rot);
        float[] k = SealDraw.CAM;
        SealDraw.plane(pose, vc, cx, cy, cz, (k[0] * c + k[3] * s) * hw, (k[1] * c + k[4] * s) * hw, (k[2] * c + k[5] * s) * hw,
                (-k[0] * s + k[3] * c) * hh, (-k[1] * s + k[4] * c) * hh, (-k[2] * s + k[5] * c) * hh, false, argb, light);
    }

    /**
     * The spirit sits behind the body (z is behind, y = 1.47 - height above the feet), a little to one side as it sways. Phase 1: one body quad, soft. Phase 2: the accent layer behind it, a bigger
     * brighter body that breathes, and (full detail only) eight specks or four ribbons.
     */
    private static void spirit(AuraContext c, PoseStack pose, int phase, int kind, int rgb, float f, float pulse, float age, int light, boolean full) {
        boolean strong = phase == 2;
        int acc = SealDraw.mix(rgb, ACCENT[kind], 0.7f);
        float sway = Mth.sin(age * 0.045f) * 0.07f, bob = Mth.sin(age * 0.07f) * 0.05f;
        float half = strong ? 1.45f : 0.95f, cy = 1.47f - (strong ? 1.45f : 1.15f) + bob, cz = strong ? 0.75f : 0.6f;
        float breathe = strong ? 1f + 0.04f * Mth.sin(age * 0.13f) : 1f;

        // the accent layer first (further back): dragon wings flap, the lion's mane and the serpent's flames turn slowly, the rest hold still and breathe
        if (strong && ACC[kind] != null) {
            float w = 1.55f, h = 1.55f, rot = 0f;
            if (kind == BeastWillLook.DRAGON) w *= 0.88f + 0.12f * Mth.sin(age * 0.16f);
            if (kind == BeastWillLook.LION) rot = age * 0.006f;
            if (kind == BeastWillLook.SERPENT || kind == BeastWillLook.CHIMERA) rot = age * 0.012f;
            if (kind == BeastWillLook.RAPTOR) { h *= 0.92f + 0.08f * Mth.sin(age * 0.2f); w *= 1.1f; }
            if (kind == BeastWillLook.PANTHER) h *= 1f + 0.06f * Mth.sin(age * 0.05f);
            sprite(pose, c.buffers().getBuffer(AuraRender.additive(ACC[kind])), sway, cy - 0.05f, cz + 0.12f, w * breathe, h * breathe, rot, SealDraw.argb(0.62f * f * pulse, acc), light);
        }
        sprite(pose, c.buffers().getBuffer(AuraRender.additive(BODY[kind])), sway, cy, cz, half * breathe, half * breathe, 0f, SealDraw.argb((strong ? 0.78f : 0.40f) * f, SealDraw.mix(rgb, 0xFFFFFF, strong ? 0.25f : 0.1f)), light);
        if (!full) return;

        // the specks that show the beast's element
        int mode = MODE[kind];
        VertexConsumer sv = c.buffers().getBuffer(AuraRender.additive(SPECK[kind]));
        if (mode == 5) {                                                                                   // wind ribbons orbiting the body, one ribbon = one quad
            for (int i = 0; i < 4; i++) {
                float a = age * 0.05f * (i % 2 == 0 ? 1f : -1f) + i * 1.571f, a0 = a - 0.9f;
                float yy = 0.55f + 0.45f * Mth.sin(age * 0.03f + i * 1.9f);
                SealDraw.ribbon(pose, sv, Mth.cos(a0) * 1.05f, yy, Mth.sin(a0) * 0.95f + 0.1f, Mth.cos(a) * 1.05f, yy - 0.1f, Mth.sin(a) * 0.95f + 0.1f, 0.11f, 0f, 1f, SealDraw.argb(0.7f * f, acc), light);
            }
            return;
        }
        int seed = kind * 131;
        for (int i = 0; i < 8; i++) {
            float ph = frac(age * 0.011f + i * 0.125f + SealDraw.rnd(seed, i, 1) * 0.3f), fade = Mth.sin(ph * Mth.PI), sx = (SealDraw.rnd(seed, i, 2) - 0.5f) * 1.7f, x, y, z = cz - 0.1f, rot = 0f, size = 0.09f + 0.05f * SealDraw.rnd(seed, i, 3);
            switch (mode) {
                case 1 -> { x = sx * 0.7f + Mth.sin(ph * 7f + i) * 0.25f; y = -0.2f + ph * 2.5f; rot = ph * 6f + i; }                          // leaves fall and tumble
                case 2 -> { float a = age * 0.03f * (i % 2 == 0 ? 1f : -1f) + i * 0.785f; x = Mth.cos(a) * 1.15f; y = 0.2f + Mth.sin(a) * 0.8f; rot = a; fade = 0.6f + 0.4f * Mth.sin(age * 0.2f + i); }   // shards circle
                case 3 -> { int s = (int) (age * 0.3f); x = (SealDraw.rnd(seed + s, i, 4) - 0.5f) * 2.2f; y = 0.5f - SealDraw.rnd(seed + s, i, 5) * 1.6f; rot = (SealDraw.rnd(seed + s, i, 6) - 0.5f) * 0.8f; size = 0.2f; fade = ((s + i) % 3 == 0) ? 0f : 0.9f; }   // bolts jump
                case 4 -> { x = 1.1f - ph * 2.4f; y = 1.25f + SealDraw.rnd(seed, i, 7) * 0.1f; size = 0.12f + ph * 0.28f; fade *= 0.8f; }        // dust rolls back along the ground
                default -> { x = sx * 0.8f + Mth.sin(age * 0.05f + i) * 0.1f; y = 1.35f - ph * 2.3f; size *= 0.6f + 0.6f * fade; }                // embers and smoke rise
            }
            if (fade <= 0.01f) continue;
            sprite(pose, sv, x + sway, y, z, size, size, rot, SealDraw.argb(0.8f * f * fade, acc), light);
        }
    }
}
