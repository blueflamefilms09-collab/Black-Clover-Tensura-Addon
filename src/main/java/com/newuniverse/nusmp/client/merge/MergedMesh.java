package com.newuniverse.nusmp.client.merge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoModelData;
import com.newuniverse.nusmp.client.geo.GeoModels;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.core.magic.merge.ColorBlend;
import com.newuniverse.nusmp.core.magic.merge.MergedBlend;
import com.newuniverse.nusmp.core.magic.merge.MergedDef;
import com.newuniverse.nusmp.core.magic.merge.MergedModelPlan;
import com.newuniverse.nusmp.core.magic.merge.MeshMath;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The 3D model of a merged magic, baked ONCE per merged id: the pieces of {@link MergedModelPlan} (real geo models of the parents that
 * have them, procedural crystals of every parent, the fusion gem) are drawn once into a recording vertex consumer, centred, normalised to
 * one block and turned into the armour layer's y-down space, and stored as flat float arrays (x y z u v nx ny nz per vertex, four vertices
 * a quad). Drawing it every frame ({@link #replay}) only streams those arrays into the frame's buffer through the current pose: nothing
 * is rebuilt and nothing is allocated by this code.
 */
public final class MergedMesh {
    /** One baked piece: its vertices, its textures and where it floats. */
    public static final class Part {
        public final MergedModelPlan.Kind kind;
        public final int parent;
        /** The texture the piece is drawn with (null = the shared glow texture, additive). */
        public final ResourceLocation texture, glow;
        public final boolean cutout;
        final float[] vertices;
        final int count;
        public final float angle, radius, height, scale, spin, lean;
        /** ARGB tint of the piece (alpha 255). */
        public final int tint;

        Part(MergedModelPlan.Kind kind, int parent, ResourceLocation texture, ResourceLocation glow, boolean cutout, float[] vertices, MergedModelPlan.Piece piece, int tint) {
            this.kind = kind; this.parent = parent; this.texture = texture; this.glow = glow; this.cutout = cutout;
            this.vertices = vertices; this.count = vertices.length / 8;
            this.angle = piece.angle(); this.radius = piece.radius(); this.height = piece.height(); this.scale = piece.scale();
            this.spin = piece.spin(); this.lean = piece.lean(); this.tint = tint;
        }

        public int vertexCount() { return count; }
    }

    public final MergedDef def;
    public final Part[] parts;

    private MergedMesh(MergedDef def, Part[] parts) { this.def = def; this.parts = parts; }

    public int vertexCount() {
        int n = 0;
        for (Part p : parts) n += p.count;
        return n;
    }

    // ================================================================ baking
    /** Builds the mesh of a merged magic. Never throws: a piece that fails to bake is left out (the gem always bakes). */
    public static MergedMesh bake(MergedDef def, MergedBlend blend) {
        List<Part> parts = new ArrayList<>();
        for (MergedModelPlan.Piece piece : MergedModelPlan.plan(def)) {
            try {
                Part p = bakePiece(def, blend, piece);
                if (p != null) parts.add(p);
            } catch (RuntimeException e) {
                com.newuniverse.nusmp.core.magic.profile.SafeCast.clientFault("merge-bake:" + def.id(), e);
            }
        }
        return new MergedMesh(def, parts.toArray(new Part[0]));
    }

    private static Part bakePiece(MergedDef def, MergedBlend blend, MergedModelPlan.Piece piece) {
        int parent = piece.parent();
        int own = parent < 0 ? blend.primary : blend.tinted(parent);
        if (piece.kind() == MergedModelPlan.Kind.GEO) {
            GeoSpec spec = GeoSpec.of(piece.geoKey(), piece.geoName());
            GeoModelData model = GeoModels.model(spec.model());
            if (model != null && !model.roots.isEmpty()) {
                Recorder rec = new Recorder();
                PoseStack ps = new PoseStack();
                GeoDraw.draw(ps, rec, model, null, 0f, GeoDraw.Space.PROP, PropDraw.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
                float[] v = MeshMath.normalise(rec.toArray());
                if (v.length > 0) return new Part(MergedModelPlan.Kind.GEO, parent, spec.texture(), spec.glow(), true, v, piece, ColorBlend.tint(0xFFFFFFFF, own, 0.25f));
            }
            // the model is missing: this piece becomes a crystal of the same parent
        }
        Recorder rec = new Recorder();
        PoseStack ps = new PoseStack();
        if (piece.kind() == MergedModelPlan.Kind.GEM) {
            PropDraw.crystal(ps, rec, 0f, 0f, 0f, 0.13f, 0.20f, 0.34f, 0xFFFFFFFF, PropDraw.FULL_BRIGHT);
            ps.mulPose(Axis.XP.rotationDegrees(180f));
            PropDraw.crystal(ps, rec, 0f, 0f, 0f, 0.13f, 0.20f, 0.34f, 0xFFFFFFFF, PropDraw.FULL_BRIGHT);
        } else {
            PropDraw.crystal(ps, rec, 0f, 0f, 0f, 0.10f, 0.45f, 0.30f, 0xFFFFFFFF, PropDraw.FULL_BRIGHT);
        }
        float[] v = MeshMath.normalise(rec.toArray());
        if (v.length == 0) return null;
        return new Part(piece.kind() == MergedModelPlan.Kind.GEM ? MergedModelPlan.Kind.GEM : MergedModelPlan.Kind.CRYSTAL, parent, AuraRender.tex("particle/glow"), null, false, v, piece, own | 0xFF000000);
    }

    // ================================================================ drawing
    /** Streams a part into the buffer through the pose's current transform. Allocates nothing. */
    public static void replay(PoseStack.Pose pose, VertexConsumer vc, Part part, int argb, int light) {
        float[] v = part.vertices;
        int r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255, a = argb >>> 24;
        for (int i = 0, n = part.count; i < n; i++) {
            int o = i * 8;
            vc.addVertex(pose, v[o], v[o + 1], v[o + 2]).setColor(r, g, b, a).setUv(v[o + 3], v[o + 4])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, v[o + 5], v[o + 6], v[o + 7]);
        }
    }

    // ================================================================ the recording consumer
    /** A vertex consumer that keeps position, uv and normal of every vertex it is given (colour, light and overlay are applied when replaying). */
    static final class Recorder implements VertexConsumer {
        private float[] data = new float[8 * 256];
        private int n = -1;

        private void ensure() {
            int need = (n + 1) * 8;
            if (need > data.length) data = Arrays.copyOf(data, Math.max(need, data.length * 2));
        }

        @Override public VertexConsumer addVertex(float x, float y, float z) {
            n++;
            ensure();
            int o = n * 8;
            data[o] = x; data[o + 1] = y; data[o + 2] = z;
            return this;
        }

        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }

        @Override public VertexConsumer setUv(float u, float v) {
            if (n >= 0) { data[n * 8 + 3] = u; data[n * 8 + 4] = v; }
            return this;
        }

        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }

        @Override public VertexConsumer setNormal(float x, float y, float z) {
            if (n >= 0) { data[n * 8 + 5] = x; data[n * 8 + 6] = y; data[n * 8 + 7] = z; }
            return this;
        }

        float[] toArray() { return Arrays.copyOf(data, (n + 1) * 8); }
    }
}
