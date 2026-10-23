package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.book.SpiritDiveAnimeRules;
import com.newuniverse.nusmp.book.SpiritDiveMerge2Rules;
import com.newuniverse.nusmp.book.SpiritDiveMergeMesh;
import com.newuniverse.nusmp.book.SpiritDiveMergeRules;
import com.newuniverse.nusmp.book.SpiritDivePieces;
import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;
import com.newuniverse.nusmp.book.SpiritDiveTier;
import com.newuniverse.nusmp.book.SpiritDiveTier.Grade;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;
import com.newuniverse.nusmp.book.SpiritDiveWind3Mesh;
import com.newuniverse.nusmp.client.SafeAdditive;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.SealDraw;
import com.newuniverse.nusmp.perf.PerfBudget;
import com.newuniverse.nusmp.perf.PerfProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Spirit Dive MERGE: the diver MERGES WITH THE SPIRIT, like the anime. One additive client-only layer, hooked ONCE in {@link SpiritDiveAura#paint} so it runs for every look and every element, in
 * both tiers. The rules (the clock, the path, every strength, the budgets) are {@link SpiritDiveMergeRules}; this class only draws.
 * <ol>
 *   <li><b>Phase 1 (clock 0..8):</b> the spirit of the element (Sylph, Undine, Salamander, Gnome: a ghost of its own shape, in the colour of the dive) appears beside and above the wearer, rising into place.</li>
 *   <li><b>Phase 2 (8..18):</b> it shrinks and spirals into the chest along a curved path with trailing strips of light; at the contact (14.5) a flash and a ring, and the body takes the colour for a moment.</li>
 *   <li><b>Phase 3 (15 on):</b> a very faint silhouette of the spirit round the torso (at most 12 percent alpha, 288 vertices, FULL level of detail only), breathing slowly: the spirit is within him.</li>
 *   <li><b>Exit:</b> the same clock backwards: the spirit separates from the chest, floats out and fades.</li>
 * </ol>
 * The character tier (Yuno with the Sylph, Fuegoleon with the Salamander, Noelle with the Undine) has the larger spirit in its canon form (the Sylph fairy with its wings spread, the Salamander coiled with the
 * neck raised, the Undine as the mermaid) and five strips instead of three.
 *
 * <p>Cost (rule 11): the ghost (at most 696 vertices) and its strips (at most 120) and the contact sprites (12) and the body cover (144) only while the entry / exit clock is inside 0..18.6 and the diver is within 32
 * blocks; the inner silhouette (288) only at FULL level of detail (24 blocks); at most 3 merges a frame ({@link SpiritDiveMergeRules#MERGES_PER_FRAME}); none in first person for the wearer. No allocation per
 * frame (statics for every scratch object), no packet, no server work; the state is a function of the dive's own clock, so there is nothing to clean up per player.</p>
 */
public final class SpiritDiveMerge {
    private SpiritDiveMerge() {}

    private static final int FB = PropDraw.FULL_BRIGHT;
    private static final PerfProbe.Section SECTION = PerfProbe.section("c.spiritdive.merge");                // timed (/multiverse perf)
    private static final PerfBudget BUDGET = new PerfBudget("spirit-dive-merge", SpiritDiveMergeRules.MERGES_PER_FRAME);
    private static final ResourceLocation SKIN = AuraRender.tex(SpiritDiveMergeRules.SKIN), TRAIL = AuraRender.tex(SpiritDiveMergeRules.TRAIL), FLASH = AuraRender.tex(SpiritDiveMergeRules.FLASH),
            SOFT = AuraRender.tex(SpiritDiveAnimeRules.SOFT_TEXTURE);
    private static final ResourceLocation[] RING = {AuraRender.tex("particle/air_ring"), AuraRender.tex("particle/waterfx_ripple"), AuraRender.tex("particle/flamefx_ring"),
            AuraRender.tex("particle/earthfam_rock_ring")};

    private static volatile boolean enabled = SpiritDiveMergeRules.DEFAULT_MERGE;
    private static long seenFrame, seenAt;
    private static final Quaternionf Q = new Quaternionf();                                                 // scratch: the render thread is the only user
    private static final Vector3f P = new Vector3f(), N = new Vector3f();
    private static final float[] PATH = new float[4], PTS = new float[(SpiritDiveMergeRules.STRIP_SEGS + 1) * 3];

    /** The client option {@code spiritDive.merge}: false draws no merge at all (the dive is as before). */
    public static boolean enabled() { return enabled; }

    public static void setEnabled(boolean on) { enabled = on; }

    /** At most {@link SpiritDiveMergeRules#MERGES_PER_FRAME} merges a frame; with no frame event (the preview harness, a mod that swallows the stage) every merge is admitted. */
    private static boolean admit() {
        long f = SpiritDiveFrame.frame(), now = System.nanoTime();
        if (f != seenFrame) { seenFrame = f; seenAt = now; }
        boolean live = f != 0L && now - seenAt < 500_000_000L;
        return !live || BUDGET.tryTake(f, 1);
    }

    // ------------------------------------------------------------------ the entry point
    /** Called by {@link SpiritDiveAura#paint} once per dive, before the look is chosen: the dive of {@code el} at {@code st} for the tier {@code grade}. */
    static void paint(AuraContext c, Element el, Stage st, Grade grade) {
        if (!enabled) return;
        float m = SpiritDiveMergeRules.clock(c.t(), c.left());
        if (m <= 0.05f) return;
        boolean flow = SpiritDiveMergeRules.flowing(m) || SpiritDiveMergeRules.bodyGlow(m) > 0.01f || SpiritDiveMergeRules.flash(m) > 0.01f;
        boolean core = m > 15.05f;
        if (!flow && !core) return;
        if (firstPersonSelf(c)) return;
        double d2 = SpiritDiveFrame.distSq(c);
        Lod lod = SpiritDiveTier.lod(d2);
        if (lod == Lod.CULLED) return;
        boolean ghostRange = d2 <= SpiritDiveMergeRules.GHOST_RANGE2;
        flow &= ghostRange;
        core &= lod == Lod.FULL;
        if (!flow && !core) return;
        if (!admit()) return;
        long t0 = PerfProbe.begin();
        try {
            boolean character = SpiritDiveMergeRules.character(el, grade) && SpiritDiveLook.characterEffects();
            if (flow) cover(c, el, m);
            PoseStack pose = c.pose();
            pose.pushPose();
            pose.scale(1f / 16f, 1f / 16f, 1f / 16f);                                                         // model pixels, y down, the origin at the neck line (feet at y = 24)
            SealDraw.camera(pose);
            if (flow) ghost(c, el, m, character);
            if (core) inner(c, el, m);
            pose.popPose();
        } finally {
            PerfProbe.end(SECTION, t0);
        }
    }

    /** The painter of the preview tool's solo scenes ({@code examples.SdmSolo}): only the merge, no look under it (the style number is decoded as the dive's own painter does). */
    public static void paintSolo(AuraContext c) {
        int style = c.style();
        paint(c, SpiritDivePieces.element(style), SpiritDivePieces.stage(style), SpiritDivePieces.grade(style));
    }

    // ------------------------------------------------------------------ the body takes the colour (before the pixel frame: the shell passes work in blocks)
    private static void cover(AuraContext c, Element el, float m) {
        float g = SpiritDiveMergeRules.bodyGlow(m);
        if (g <= 0.01f) return;
        boolean skipArmR = el == Element.FIRE && FuegoleonArmAura.drawsArm(c.player());                       // the flame arm is its own look
        SpiritDiveAnimeShell.pass(c, SafeAdditive.additive(SOFT), false, 0.5f, SpiritDiveMergeRules.rgb(el), 0.6f * g, 1f, SpiritDivePieces.SHELL_ALL, false, skipArmR, 0, WaterAuraRig.slim(c.model()));
    }

    // ------------------------------------------------------------------ phases 1 and 2: the ghost, its strips, the flash
    private static void ghost(AuraContext c, Element el, float m, boolean character) {
        PoseStack pose = c.pose();
        float side = SpiritDiveMergeRules.side(c.player().getUUID().hashCode());
        float a = SpiritDiveMergeRules.ghostAlpha(m) * SpiritDiveMergeRules.GHOST_ALPHA;
        int rgb = SpiritDiveMergeRules.rgb(el);
        if (a > 0.01f) {
            SpiritDiveMergeRules.path(m, side, character, PATH);
            float bob = m < SpiritDiveMergeRules.APPEAR_END + 2f ? 1.6f * Mth.sin(c.age() * 0.13f) : 0f;
            float s = SpiritDiveMergeRules.ghostSize(el, character) * SpiritDiveMergeRules.ghostScale(m);
            pose.pushPose();
            pose.translate(PATH[0], PATH[1] + bob, PATH[2]);
            pose.mulPose(Q.rotationY(PATH[3]));
            pose.scale(s, s, s);
            mesh(c, ghostMesh(el, character), a * (1f + 0.5f * SpiritDiveMergeRules.flash(m)));
            pose.popPose();
        }
        float sa = SpiritDiveMergeRules.strips(m);
        if (sa > 0.01f) strips(c, el, m, side, character, sa);
        float f = SpiritDiveMergeRules.flash(m), rg = SpiritDiveMergeRules.ring(m);
        if (f > 0.01f || rg > 0.01f) {
            float k = character ? 1.3f : 1f;
            VertexConsumer glow = c.buffers().getBuffer(SafeAdditive.additive(FLASH));
            if (f > 0.01f) {
                SealDraw.glow(pose, glow, SpiritDiveMergeRules.CHEST_X, SpiritDiveMergeRules.CHEST_Y, -3f, (9f + 15f * f) * k, 0f, SealDraw.argb(0.85f * f, SpiritDiveMergeRules.rgb(el)), FB);
                SealDraw.glow(pose, glow, SpiritDiveMergeRules.CHEST_X, SpiritDiveMergeRules.CHEST_Y, -3.5f, (4f + 6f * f) * k, 0f, SealDraw.argb(0.95f * f, SpiritDiveMergeRules.flashRgb(el)), FB);
            }
            if (rg > 0.01f)
                SealDraw.glow(pose, c.buffers().getBuffer(SafeAdditive.additive(RING[el.ordinal()])), SpiritDiveMergeRules.CHEST_X, SpiritDiveMergeRules.CHEST_Y, -3f, (5f + 17f * rg) * k, c.age() * 0.1f,
                        SealDraw.argb(0.7f * (1f - rg), rgb), FB);
        }
    }

    /** The strips of light that trail the spirit along its spiral: every strip follows the spirit's path a little earlier (or, on the exit, later) in the clock, each with its own sideways offset. */
    private static void strips(AuraContext c, Element el, float m, float side, boolean character, float sa) {
        PoseStack pose = c.pose();
        int n = SpiritDiveMergeRules.strips(character), segs = SpiritDiveMergeRules.STRIP_SEGS;
        float dir = SpiritDiveMergeRules.exiting(c.t(), c.left()) ? 1f : -1f;                                            // exit: the trail runs the other way
        int rgb = SealDraw.mix(SpiritDiveMergeRules.rgb(el), 0xFFFFFF, 0.35f);
        VertexConsumer vc = c.buffers().getBuffer(SafeAdditive.additive(TRAIL));
        float hw = character ? 2.4f : 1.8f;
        for (int k = 0; k < n; k++) {
            float o = k - (n - 1) * 0.5f;
            for (int j = 0; j <= segs; j++) {
                float mj = m + dir * j * 0.75f;
                SpiritDiveMergeRules.path(mj, side, character, PATH);
                float sc = SpiritDiveMergeRules.ghostSize(el, character) * SpiritDiveMergeRules.ghostScale(mj);
                float off = o * 3.4f * sc, cy = Mth.cos(PATH[3]), sy = Mth.sin(PATH[3]);
                PTS[j * 3] = PATH[0] + off * cy;
                PTS[j * 3 + 1] = PATH[1] + 2.2f * sc * Mth.sin(j * 0.9f + k * 1.7f) + o * 1.2f;
                PTS[j * 3 + 2] = PATH[2] - off * sy;
            }
            for (int j = 0; j < segs; j++) {
                float fade = 1f - j / (float) segs;
                SealDraw.ribbon(pose, vc, PTS[(j + 1) * 3], PTS[(j + 1) * 3 + 1], PTS[(j + 1) * 3 + 2], PTS[j * 3], PTS[j * 3 + 1], PTS[j * 3 + 2], hw * (0.4f + 0.6f * fade),
                        1f - (j + 1) / (float) segs, 1f - j / (float) segs, SealDraw.argb(0.8f * sa * fade, rgb), FB);
            }
        }
    }

    // ------------------------------------------------------------------ phase 3: the spirit within him
    private static void inner(AuraContext c, Element el, float m) {
        float a = SpiritDiveMergeRules.inner(m, c.age()) * SpiritDiveMergeRules.INNER_ALPHA;
        if (a <= 0.004f) return;
        PoseStack pose = c.pose();
        float s = SpiritDiveMergeRules.coreSize(el) * (1f + 0.03f * Mth.sin(c.age() * 0.06f));
        pose.pushPose();
        pose.translate(0f, SpiritDiveMergeRules.CORE_Y, SpiritDiveMergeRules.CORE_Z);
        if (el == Element.FIRE) pose.mulPose(Q.rotationY(c.age() * 0.012f));                                  // the Salamander coils slowly round the torso
        pose.scale(s, s, s);
        mesh(c, SpiritDiveMerge2.slim() ? SpiritDiveMerge2Rules.mesh(SpiritDiveMerge2Rules.slimCore(el)) : SpiritDiveMergeMesh.get(SpiritDiveMergeRules.coreMesh(el)), a);
        pose.popPose();
    }

    // ------------------------------------------------------------------ drawing a mesh (the tints are baked)
    /** The ghost mesh of the chosen models: the slim spirit of merge 2 (default) or the boxy one of the first merge ({@code spiritDive.mergeModels}). */
    static SpiritDiveWind3Mesh ghostMesh(Element el, boolean character) {
        return SpiritDiveMerge2.slim() ? SpiritDiveMerge2Rules.mesh(SpiritDiveMerge2Rules.slimMesh(el, character)) : SpiritDiveMergeMesh.get(SpiritDiveMergeRules.ghostMesh(el, character));
    }

    private static void mesh(AuraContext c, SpiritDiveWind3Mesh m, float alpha) { draw(c.pose(), c.buffers(), m, alpha); }

    /**
     * Draws a spirit mesh in the pose's frame, all additive: the slim mesh has two layers (the solid body at {@link SpiritDiveMerge2Rules#SOLID_K}, the soft glow shell at {@link SpiritDiveMerge2Rules#GLOW_K} of
     * the alpha), the boxy mesh only layer 1. Shared by the third person merge and the first person merge. No allocation (static scratch).
     */
    static void draw(PoseStack ps, MultiBufferSource buffers, SpiritDiveWind3Mesh m, float alpha) {
        if (alpha <= 0.004f) return;
        PoseStack.Pose pose = ps.last();
        VertexConsumer vc = buffers.getBuffer(SafeAdditive.additive(SKIN));
        if (m.quads(SpiritDiveWind3Mesh.SOLID) > 0) emit(pose, vc, m, SpiritDiveWind3Mesh.SOLID, alpha * SpiritDiveMerge2Rules.SOLID_K);
        emit(pose, vc, m, SpiritDiveWind3Mesh.GLOW, alpha * (m.quads(SpiritDiveWind3Mesh.SOLID) > 0 ? SpiritDiveMerge2Rules.GLOW_K : 1f));
    }

    private static void emit(PoseStack.Pose pose, VertexConsumer vc, SpiritDiveWind3Mesh m, int layer, float alpha) {
        int quads = m.quads(layer);
        if (quads == 0 || alpha <= 0.004f) return;
        int a = Math.min(255, (int) (alpha * 255f + 0.5f));
        Matrix4f mat = pose.pose();
        float[] pos = m.pos[layer], uv = m.uv[layer], nrm = m.nrm[layer];
        int[] tint = m.tint[layer];
        for (int q = 0; q < quads; q++) {
            int t = tint[q];
            pose.transformNormal(nrm[q * 3], nrm[q * 3 + 1], nrm[q * 3 + 2], N);
            for (int k = 0; k < 4; k++) {
                int i = q * 12 + k * 3;
                mat.transformPosition(pos[i], pos[i + 1], pos[i + 2], P);
                vc.addVertex(P.x, P.y, P.z).setColor(t >> 16 & 255, t >> 8 & 255, t & 255, a).setUv(uv[q * 8 + k * 2], uv[q * 8 + k * 2 + 1]).setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(FB).setNormal(N.x, N.y, N.z);
            }
        }
    }

    private static boolean firstPersonSelf(AuraContext c) {
        try {
            Minecraft mc = Minecraft.getInstance();
            return c.player() == mc.player && mc.options.getCameraType().isFirstPerson();
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
