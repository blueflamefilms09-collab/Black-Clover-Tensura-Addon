package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.client.SafeAdditive;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.spiritspace.SpaceMoodRules;
import com.newuniverse.nusmp.spiritspace.SpiritSpaceLod;
import com.newuniverse.nusmp.spiritspace.SpiritSpaceMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

/**
 * Why: the drawing of the Spirit of Space (docs/magic/spirit_space.md), apart from the entity glue ({@link SpiritSpacePropPainter}), so the headless preview (tools/render_preview, example
 * SpiritSpaceProp) draws exactly what the game draws. Everything here is primitive math into a pose and a buffer: no entity, no world, no allocation per frame (static scratch arrays and
 * tables built once; {@link SpiritSpaceMesh} holds every loop count, so the quads of a level of detail are the numbers the doc states).
 *
 * <p><b>The orb</b> is a tear in reality: a UV sphere of dark sky and, over it, three shells of starfield (far, mid, near) that scroll at different speeds and also slide with the viewer's angle (the
 * parallax), in the rift ring of cracked glass (geo model {@code spirit_space_orb}, additive) with a violet halo behind. <b>The wisp</b> is a long body of the same sky along a spine that undulates
 * (shells of tube), cosmic fins along the spine, and a small horned head (geo model {@code spirit_space_wisp} with its glow-only model). <b>The portal ring</b> is two bands and two starfield windows.
 * The starfield layers are tileable textures; the scroll is in the vertex UVs (the textures repeat), so no custom render type is needed: every additive pass goes through {@link SafeAdditive}.</p>
 */
public final class SpiritSpaceDraw {
    private SpiritSpaceDraw() {}

    public static final int FB = PropDraw.FULL_BRIGHT;
    private static final ResourceLocation FAR = AuraRender.tex("entity/spirit_space/stars_far"), MID = AuraRender.tex("entity/spirit_space/stars_mid"), NEAR = AuraRender.tex("entity/spirit_space/stars_near"),
            CORE = AuraRender.tex("entity/spirit_space/core"), BODY = AuraRender.tex("entity/spirit_space/body"), FIN = AuraRender.tex("entity/spirit_space/fin"),
            PORTAL_RING = AuraRender.tex("entity/spirit_space/portal_ring"), PORTAL_DISC = AuraRender.tex("entity/spirit_space/portal_disc"), HALO = AuraRender.tex("entity/spirit_space/halo");

    private static GeoSpec lod(GeoSpec base, String modelId) {
        return new GeoSpec(ResourceLocation.fromNamespaceAndPath("nusmp", "geo/entity/" + modelId + ".geo.json"), base.animations(),
                ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/" + modelId + ".png"), null);
    }

    private static final GeoSpec ORB = GeoSpec.of("spirit_space", "orb"), WISP = GeoSpec.of("spirit_space", "wisp");
    private static final GeoSpec[] ORB_LOD = {ORB.noGlow(), lod(ORB, "spirit_space_orb_lod1"), lod(ORB, "spirit_space_orb_lod2")};
    private static final GeoSpec[] WISP_LOD = {WISP.noGlow(), lod(WISP, "spirit_space_wisp_lod1"), lod(WISP, "spirit_space_wisp_lod2")};
    private static final GeoSpec WISP_FX = lod(WISP, "spirit_space_wisp_fx");

    public static final int VIOLET = 0x8A5CFF, CYAN = 0x4FE3FF, PALE = 0xEAF6FF;
    /** The geo models are authored at four times their size. */
    private static final float GEO_SCALE = 0.25f, HEAD_SCALE = 0.32f;

    // ------------------------------------------------------------------------------------------------ the starfield layers (index: 0 base, 1 far, 2 mid, 3 near)
    private static final ResourceLocation[] ORB_TEX = {CORE, FAR, MID, NEAR};
    private static final float[] TILE_U = {1f, 2f, 2f, 1.5f}, TILE_V = {1f, 1f, 1f, 1f}, DU = {0f, 0.010f, 0.018f, 0.030f}, DV = {0f, 0.003f, -0.006f, 0.009f}, PARALLAX = {0f, 0.10f, 0.22f, 0.40f};
    /** Which layers each level of detail draws (the base is always the first). */
    private static final int[][] ORB_ORDER = {{0, 1, 2, 3}, {0, 2, 3}, {0, 3}};
    private static final ResourceLocation[] WISP_TEX = {BODY, FAR, MID};
    private static final float[] W_DU = {0f, 0.020f, 0.035f}, W_DV = {0f, 0.060f, 0.120f};
    private static final int[][] WISP_ORDER = {{0, 1, 2}, {0, 2}, {0}};

    private static final Quaternionf TURN = new Quaternionf();

    private static void vert(PoseStack.Pose p, VertexConsumer vc, float x, float y, float z, float u, float v, int argb, int light, float nx, float ny, float nz) {
        vc.addVertex(p, x, y, z).setColor((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, argb >>> 24)
                .setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
    }

    // ================================================================================================ the orb
    private static final float[] POSE = new float[SpaceMoodRules.P_COUNT];

    /** A layer's colour: the expression's tint at the layer's alpha scaled by the expression's strength. */
    private static int layerArgb(float alpha, SpaceMoodRules.Expression expr) {
        return SealDraw.argb(Mth.clamp(alpha * SpaceMoodRules.strength(expr), 0f, 1f), SpaceMoodRules.tint(expr));
    }

    /**
     * One orb centred on the pose's origin. {@code lod} 0..2, {@code age} the clock in ticks, {@code fade} 0..1 (forming and fading), {@code mood} and {@code expr} the pose and the expression in effect
     * ({@link SpaceMoodRules}), {@code since} the ticks since the pose began, {@code camAz} and {@code camEl} the direction of the viewer from the orb in turns (the parallax of the starfield layers).
     */
    public static void orb(PoseStack pose, MultiBufferSource buffers, int lod, float age, float fade, SpaceMoodRules.Mood mood, SpaceMoodRules.Expression expr, float since, float camAz, float camEl) {
        if (lod >= SpiritSpaceLod.CULL || fade <= 0.01f) return;
        SpaceMoodRules.pose(POSE, mood, since);
        fade *= POSE[SpaceMoodRules.P_ALPHA];
        if (fade <= 0.01f) return;
        boolean cast = POSE[SpaceMoodRules.P_CAST] > 0.5f;
        float s = age / 20f, size = (0.7f + 0.3f * fade) * (1f + 0.025f * Mth.sin(age * 0.2f)) * POSE[SpaceMoodRules.P_SCALE], squash = POSE[SpaceMoodRules.P_SQUASH];
        pose.pushPose();
        if (POSE[SpaceMoodRules.P_SHAKE] > 0f) pose.translate(Mth.sin(age * 13f) * POSE[SpaceMoodRules.P_SHAKE], Mth.cos(age * 11f) * POSE[SpaceMoodRules.P_SHAKE], 0f);
        if (lod <= SpiritSpaceLod.HALF) {                                                         // the halo: one soft sprite behind the orb, in the expression's colour
            SealDraw.camera(pose);
            SealDraw.glow(pose, buffers.getBuffer(SafeAdditive.additive(HALO)), 0f, 0f, 0f, 0.7f * size, age * 0.02f, layerArgb(0.5f * fade, expr), FB);
        }
        pose.pushPose();
        pose.scale(1f, squash, 1f);
        int[] order = ORB_ORDER[lod];
        for (int k = 0; k < order.length; k++) {
            int layer = order[k];
            float r = SpiritSpaceMesh.CORE_RADIUS * size * SpiritSpaceMesh.SHELL[layer];
            float uo = Mth.frac(s * DU[layer] + PARALLAX[layer] * camAz), vo = Mth.frac(s * DV[layer] + PARALLAX[layer] * camEl * 0.5f);
            if (layer == 0) {
                sphere(pose, buffers.getBuffer(AuraRender.cutout(CORE)), lod, r, 1f, 1f, 0f, 0f, 0xFFFFFFFF, FB);
            } else {
                sphere(pose, buffers.getBuffer(SafeAdditive.additive(ORB_TEX[layer])), lod, r, TILE_U[layer], TILE_V[layer], uo, vo, layerArgb(fade, expr), FB);
            }
        }
        pose.popPose();
        pose.pushPose();                                                                          // the rift ring: cracked glass round the core, additive; its size follows the pose and the expression
        float ring = GEO_SCALE * size * POSE[SpaceMoodRules.P_RING] * SpaceMoodRules.ringShape(expr);
        pose.scale(ring, ring, ring);
        GeoDraw.paint(pose, buffers, ORB_LOD[lod], cast ? "cast" : "idle", cast ? Math.min(since / 20f, 0.99f) : s * (expr == SpaceMoodRules.Expression.SLEEPY ? 0.4f : 1f), GeoDraw.Space.PROP, GeoDraw.Layer.ADDITIVE, FB, layerArgb(fade, expr), 0);
        pose.popPose();
        pose.popPose();
    }

    /** A UV sphere shell of the level's grid with the given radius; the texture is tiled and scrolled by the UV arguments. */
    private static void sphere(PoseStack pose, VertexConsumer vc, int lod, float r, float tileU, float tileV, float uOff, float vOff, int argb, int light) {
        PoseStack.Pose p = pose.last();
        float[] xyz = SpiritSpaceMesh.sphereXyz(lod), uv = SpiritSpaceMesh.sphereUv(lod);
        int rings = SpiritSpaceMesh.SPHERE_RINGS[lod], segs = SpiritSpaceMesh.SPHERE_SEGS[lod], row = segs + 1;
        for (int i = 0; i < rings; i++) {
            for (int j = 0; j < segs; j++) {
                int a = i * row + j;
                sphereVert(p, vc, xyz, uv, a, r, tileU, tileV, uOff, vOff, argb, light);                 // (ring, seg), (ring, seg + 1), (ring + 1, seg + 1), (ring + 1, seg): outward
                sphereVert(p, vc, xyz, uv, a + 1, r, tileU, tileV, uOff, vOff, argb, light);
                sphereVert(p, vc, xyz, uv, a + row + 1, r, tileU, tileV, uOff, vOff, argb, light);
                sphereVert(p, vc, xyz, uv, a + row, r, tileU, tileV, uOff, vOff, argb, light);
            }
        }
    }

    private static void sphereVert(PoseStack.Pose p, VertexConsumer vc, float[] xyz, float[] uv, int k, float r, float tileU, float tileV, float uOff, float vOff, int argb, int light) {
        float x = xyz[k * 3], y = xyz[k * 3 + 1], z = xyz[k * 3 + 2];
        vert(p, vc, x * r, y * r, z * r, uv[k * 2] * tileU + uOff, uv[k * 2 + 1] * tileV + vOff, argb, light, x, y, z);
    }

    // ================================================================================================ the wisp
    private static final int MAXN = SpiritSpaceMesh.TUBE_SEGS[0];
    private static final float[] CEN = new float[(MAXN + 1) * 3], SID = new float[(MAXN + 1) * 3], UPV = new float[(MAXN + 1) * 3], RAD = new float[MAXN + 1];
    private static final float[] COS = new float[SpiritSpaceMesh.TUBE_SIDES[0] + 1], SIN = new float[SpiritSpaceMesh.TUBE_SIDES[0] + 1];
    private static final float[] TMP = new float[3];

    /**
     * One wisp, its body's middle on the pose's origin, facing +z: the tube of starfield along the undulating spine, the fins, and the head. {@code mood}, {@code expr} and {@code since} as for
     * {@link #orb}: the pose coils the body, lifts it, rears the head; the expression tints the sky and the dragon's eyes.
     */
    public static void wisp(PoseStack pose, MultiBufferSource buffers, int lod, float age, float fade, SpaceMoodRules.Mood mood, SpaceMoodRules.Expression expr, float since) {
        if (lod >= SpiritSpaceLod.CULL || fade <= 0.01f) return;
        SpaceMoodRules.pose(POSE, mood, since);
        fade *= POSE[SpaceMoodRules.P_ALPHA];
        if (fade <= 0.01f) return;
        int n = SpiritSpaceMesh.TUBE_SEGS[lod], sides = SpiritSpaceMesh.TUBE_SIDES[lod];
        spine(n, sides, age, (0.8f + 0.2f * fade) * POSE[SpaceMoodRules.P_SCALE], POSE[SpaceMoodRules.P_COIL]);
        pose.pushPose();
        float rise = POSE[SpaceMoodRules.P_RISE];
        if (rise > 0f) {
            pose.translate(0f, rise, 0f);
            pose.mulPose(TURN.rotationX(-0.45f * rise / 0.9f));                                   // the nose lifts as it climbs
        }
        float s = age / 20f;
        int white = layerArgb(fade, expr);
        int[] order = WISP_ORDER[lod];
        for (int k = 0; k < order.length; k++) {
            int layer = order[k];
            float rs = SpiritSpaceMesh.WISP_SHELL[layer];
            if (layer == 0) tube(pose, buffers.getBuffer(AuraRender.cutout(BODY)), n, sides, rs, 2f, 6f, 0f, 0f, 0xFFFFFFFF);
            else tube(pose, buffers.getBuffer(SafeAdditive.additive(WISP_TEX[layer])), n, sides, rs, 2f, 3f * layer, Mth.frac(s * W_DU[layer]), Mth.frac(s * W_DV[layer]), white);
        }
        if (SpiritSpaceMesh.FIN_STRIPS[lod] > 0) fins(pose, buffers.getBuffer(SafeAdditive.emissive(FIN)), n, age, SealDraw.argb(0.85f * fade, SpaceMoodRules.tint(expr)));
        // the head: at the neck, along the spine's end tangent
        int e = n * 3, p = (n - 1) * 3;
        float tx = CEN[e] - CEN[p], ty = CEN[e + 1] - CEN[p + 1], tz = CEN[e + 2] - CEN[p + 2], tl = Mth.sqrt(tx * tx + ty * ty + tz * tz);
        if (tl < 1e-5f) { tx = 0f; ty = 0f; tz = 1f; tl = 1f; }
        pose.pushPose();
        pose.translate(CEN[e], CEN[e + 1], CEN[e + 2]);
        pose.mulPose(TURN.rotationY((float) Math.atan2(tx, tz)));
        pose.mulPose(TURN.rotationX(-(float) Math.asin(Mth.clamp(ty / tl, -1f, 1f))));
        float hs = HEAD_SCALE * (0.8f + 0.2f * fade) * POSE[SpaceMoodRules.P_SCALE];
        pose.scale(hs, hs, hs);
        boolean casting = POSE[SpaceMoodRules.P_CAST] > 0.5f;
        String clip = casting ? "cast" : "idle";
        float secs = casting ? Math.min(since / 20f, 1.19f) : s;
        GeoDraw.paint(pose, buffers, WISP_LOD[lod], clip, secs, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, FB, 0xFFFFFFFF, 0);
        if (lod <= SpiritSpaceLod.HALF) GeoDraw.paint(pose, buffers, WISP_FX, clip, secs, GeoDraw.Space.PROP, GeoDraw.Layer.ADDITIVE, FB,
                SealDraw.argb(Mth.clamp(fade * (0.85f + 0.15f * Mth.sin(age * 0.4f)) * SpaceMoodRules.strength(expr), 0f, 1f), SpaceMoodRules.eye(expr)), 0);
        pose.popPose();
        pose.popPose();
    }

    /** Works out the spine's nodes, their frames (side, up) and radii into the scratch arrays. {@code grow} scales the body's radius while it forms. */
    private static void spine(int n, int sides, float age, float grow, float coil) {
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            SpiritSpaceMesh.spine(TMP, t, age, coil);
            CEN[i * 3] = TMP[0]; CEN[i * 3 + 1] = TMP[1]; CEN[i * 3 + 2] = TMP[2];
            RAD[i] = SpiritSpaceMesh.bodyRadius(t) * grow;
        }
        for (int i = 0; i <= n; i++) {
            int a = Math.max(0, i - 1) * 3, b = Math.min(n, i + 1) * 3;
            float tx = CEN[b] - CEN[a], ty = CEN[b + 1] - CEN[a + 1], tz = CEN[b + 2] - CEN[a + 2];
            float tl = Mth.sqrt(tx * tx + ty * ty + tz * tz);
            if (tl < 1e-6f) { tx = 0f; ty = 0f; tz = 1f; tl = 1f; }
            tx /= tl; ty /= tl; tz /= tl;
            float sx = tz, sz = -tx, sl = Mth.sqrt(sx * sx + sz * sz);                       // side = up x tangent (level), up' = tangent x side
            if (sl < 1e-4f) { sx = 1f; sz = 0f; sl = 1f; }
            sx /= sl; sz /= sl;
            float ux = ty * sz, uy = tz * sx - tx * sz, uz = -ty * sx;
            SID[i * 3] = sx; SID[i * 3 + 1] = 0f; SID[i * 3 + 2] = sz;
            UPV[i * 3] = ux; UPV[i * 3 + 1] = uy; UPV[i * 3 + 2] = uz;
        }
        for (int j = 0; j <= sides; j++) {
            double f = Math.PI * 2 * j / sides;
            COS[j] = (float) Math.cos(f);
            SIN[j] = (float) Math.sin(f);
        }
    }

    /** One shell of the tube: {@code n} segments of {@code sides} quads, radius scaled by {@code rs}, the texture tiled and scrolled. */
    private static void tube(PoseStack pose, VertexConsumer vc, int n, int sides, float rs, float tileU, float tileV, float uOff, float vOff, int argb) {
        PoseStack.Pose p = pose.last();
        for (int i = 0; i < n; i++) {
            float v0 = i / (float) n * tileV + vOff, v1 = (i + 1) / (float) n * tileV + vOff;
            for (int j = 0; j < sides; j++) {
                float u0 = j / (float) sides * tileU + uOff, u1 = (j + 1) / (float) sides * tileU + uOff;
                ringVert(p, vc, i, j, rs, u0, v0, argb);                                         // (i, j), (i, j + 1), (i + 1, j + 1), (i + 1, j): outward
                ringVert(p, vc, i, j + 1, rs, u1, v0, argb);
                ringVert(p, vc, i + 1, j + 1, rs, u1, v1, argb);
                ringVert(p, vc, i + 1, j, rs, u0, v1, argb);
            }
        }
    }

    private static void ringVert(PoseStack.Pose p, VertexConsumer vc, int i, int j, float rs, float u, float v, int argb) {
        int a = i * 3;
        float nx = SID[a] * COS[j] + UPV[a] * SIN[j], ny = SID[a + 1] * COS[j] + UPV[a + 1] * SIN[j], nz = SID[a + 2] * COS[j] + UPV[a + 2] * SIN[j];
        float r = RAD[i] * rs;
        vert(p, vc, CEN[a] + nx * r, CEN[a + 1] + ny * r, CEN[a + 2] + nz * r, u, v, argb, FB, nx, ny, nz);
    }

    /** The cosmic fins: a strip on each side (broad over the body, a fan at the tail) and a crest of teeth along the back, one quad per segment each. */
    private static void fins(PoseStack pose, VertexConsumer vc, int n, float age, int argb) {
        PoseStack.Pose p = pose.last();
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < n; i++) {
                int a = i * 3, b = (i + 1) * 3;
                float ta = i / (float) n, tb = (i + 1) / (float) n;
                float wa = SpiritSpaceMesh.finWidth(ta), wb = SpiritSpaceMesh.finWidth(tb);
                float fa = 0.05f * Mth.sin(age * 0.16f - i * 0.55f), fb = 0.05f * Mth.sin(age * 0.16f - (i + 1) * 0.55f);
                float ra = RAD[i] * 0.85f, rb = RAD[i + 1] * 0.85f;
                float v0 = ta * 3f, v1 = tb * 3f;
                if (side < 0) {
                    finVert(p, vc, i, side, ra, 0f, 0f, v0, argb);
                    finVert(p, vc, i, side, ra + wa, fa, 1f, v0, argb);
                    finVert(p, vc, i + 1, side, rb + wb, fb, 1f, v1, argb);
                    finVert(p, vc, i + 1, side, rb, 0f, 0f, v1, argb);
                } else {
                    finVert(p, vc, i + 1, side, rb, 0f, 0f, v1, argb);
                    finVert(p, vc, i + 1, side, rb + wb, fb, 1f, v1, argb);
                    finVert(p, vc, i, side, ra + wa, fa, 1f, v0, argb);
                    finVert(p, vc, i, side, ra, 0f, 0f, v0, argb);
                }
            }
        }
        for (int i = 0; i < n; i++) {                                                            // the crest
            int a = i * 3, b = (i + 1) * 3;
            float ha = SpiritSpaceMesh.crestHeight(i, n), hb = SpiritSpaceMesh.crestHeight(i + 1, n);
            float ra = RAD[i] * 0.9f, rb = RAD[i + 1] * 0.9f;
            float v0 = i / (float) n * 4f, v1 = (i + 1) / (float) n * 4f;
            vert(p, vc, CEN[a] + UPV[a] * ra, CEN[a + 1] + UPV[a + 1] * ra, CEN[a + 2] + UPV[a + 2] * ra, 0f, v0, argb, FB, SID[a], SID[a + 1], SID[a + 2]);
            vert(p, vc, CEN[a] + UPV[a] * (ra + ha), CEN[a + 1] + UPV[a + 1] * (ra + ha), CEN[a + 2] + UPV[a + 2] * (ra + ha), 1f, v0, argb, FB, SID[a], SID[a + 1], SID[a + 2]);
            vert(p, vc, CEN[b] + UPV[b] * (rb + hb), CEN[b + 1] + UPV[b + 1] * (rb + hb), CEN[b + 2] + UPV[b + 2] * (rb + hb), 1f, v1, argb, FB, SID[b], SID[b + 1], SID[b + 2]);
            vert(p, vc, CEN[b] + UPV[b] * rb, CEN[b + 1] + UPV[b + 1] * rb, CEN[b + 2] + UPV[b + 2] * rb, 0f, v1, argb, FB, SID[b], SID[b + 1], SID[b + 2]);
        }
    }

    private static void finVert(PoseStack.Pose p, VertexConsumer vc, int node, int side, float dist, float lift, float u, float v, int argb) {
        int a = node * 3;
        vert(p, vc, CEN[a] + SID[a] * side * dist + UPV[a] * lift, CEN[a + 1] + SID[a + 1] * side * dist + UPV[a + 1] * lift, CEN[a + 2] + SID[a + 2] * side * dist + UPV[a + 2] * lift, u, v, argb, FB, UPV[a], UPV[a + 1], UPV[a + 2]);
    }

    // ================================================================================================ the portal ring
    /**
     * One ring of a portal on the pose's origin (its feet), facing +z: two bands and two starfield windows. {@code open} is 0 (shut) to 1 (open): it scales the ring from its centre, so it opens
     * and closes like an iris; {@code age} is the clock in ticks.
     */
    public static void portal(PoseStack pose, MultiBufferSource buffers, float age, float open, float fade) {
        if (open <= 0.01f || fade <= 0.01f) return;
        float s = age / 20f, ease = open * (2f - open);
        pose.pushPose();
        pose.translate(0f, SpiritSpaceMesh.PORTAL_RY * ease, 0f);
        pose.scale(ease, ease, 1f);
        VertexConsumer vc = buffers.getBuffer(SafeAdditive.emissive(PORTAL_DISC));
        disc(pose, vc, 0.94f, s * 0.35f, 0f, 0.012f, 1.0f, SealDraw.argb(0.95f * fade, 0xFFFFFF));
        disc(pose, vc, 0.80f, -s * 0.55f, s * 0.04f, -0.012f, 1.6f, SealDraw.argb(0.55f * fade, 0xFFFFFF));
        VertexConsumer rim = buffers.getBuffer(SafeAdditive.emissive(PORTAL_RING));
        band(pose, rim, 0.94f, 1.12f, s * 0.12f, 0f, SealDraw.argb(fade, 0xFFFFFF));
        band(pose, rim, 1.04f, 1.24f, -s * 0.2f, 0.02f, SealDraw.argb(0.6f * fade, 0xB8A0FF));
        pose.popPose();
    }

    /** A starfield window of {@link SpiritSpaceMesh#DISC_RINGS} rings of half the segments each, the sample point turned by {@code turn} radians and scrolled; {@code z} lifts it off the others. */
    private static void disc(PoseStack pose, VertexConsumer vc, float radius, float turn, float scroll, float z, float tile, int argb) {
        PoseStack.Pose p = pose.last();
        int segs = SpiritSpaceMesh.PORTAL_SEGS / 2, rings = SpiritSpaceMesh.DISC_RINGS;
        float rx = SpiritSpaceMesh.PORTAL_RX, ry = SpiritSpaceMesh.PORTAL_RY;
        for (int k = 0; k < rings; k++) {
            float r0 = radius * k / rings, r1 = radius * (k + 1) / rings;
            for (int j = 0; j < segs; j++) {
                float a0 = (float) (Math.PI * 2 * j / segs), a1 = (float) (Math.PI * 2 * (j + 1) / segs);
                discVert(p, vc, r0, a0, turn, scroll, z, tile, rx, ry, argb);
                discVert(p, vc, r0, a1, turn, scroll, z, tile, rx, ry, argb);
                discVert(p, vc, r1, a1, turn, scroll, z, tile, rx, ry, argb);
                discVert(p, vc, r1, a0, turn, scroll, z, tile, rx, ry, argb);
            }
        }
    }

    private static void discVert(PoseStack.Pose p, VertexConsumer vc, float r, float a, float turn, float scroll, float z, float tile, float rx, float ry, int argb) {
        float c = Mth.cos(a), s = Mth.sin(a);
        float u = 0.5f + Mth.cos(a + turn) * r * 0.5f * tile + scroll, v = 0.5f + Mth.sin(a + turn) * r * 0.5f * tile;
        vert(p, vc, c * r * rx, s * r * ry, z, u, v, argb, FB, 0f, 0f, -1f);
    }

    /** A flat band round the ring between two radii (the x radius and the y radius scale by the oval), the texture running round it twice. */
    private static void band(PoseStack pose, VertexConsumer vc, float r0, float r1, float scroll, float z, int argb) {
        PoseStack.Pose p = pose.last();
        int segs = SpiritSpaceMesh.PORTAL_SEGS;
        float rx = SpiritSpaceMesh.PORTAL_RX, ry = SpiritSpaceMesh.PORTAL_RY;
        for (int j = 0; j < segs; j++) {
            float a0 = (float) (Math.PI * 2 * j / segs), a1 = (float) (Math.PI * 2 * (j + 1) / segs);
            float u0 = j / (float) segs * 2f + scroll, u1 = (j + 1) / (float) segs * 2f + scroll;
            vert(p, vc, Mth.cos(a0) * r0 * rx, Mth.sin(a0) * r0 * ry, z, u0, 0f, argb, FB, 0f, 0f, -1f);
            vert(p, vc, Mth.cos(a1) * r0 * rx, Mth.sin(a1) * r0 * ry, z, u1, 0f, argb, FB, 0f, 0f, -1f);
            vert(p, vc, Mth.cos(a1) * r1 * rx, Mth.sin(a1) * r1 * ry, z, u1, 1f, argb, FB, 0f, 0f, -1f);
            vert(p, vc, Mth.cos(a0) * r1 * rx, Mth.sin(a0) * r1 * ry, z, u0, 1f, argb, FB, 0f, 0f, -1f);
        }
    }
}
