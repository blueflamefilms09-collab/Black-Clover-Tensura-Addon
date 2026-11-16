package com.newuniverse.nusmp.client.bossmesh;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.client.SafeAdditive;
import com.newuniverse.nusmp.mesh.EntityMeshData;
import com.newuniverse.nusmp.mesh.MeshText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.util.Optional;

/**
 * Boss rework wave 2 (docs/bosses/rework_wave2_models.md): draws a Blender-made entity body ({@link EntityMeshData}, {@code entity_meshes/<id>.txt}, skins in
 * {@code textures/entity/boss_mesh/}) on an entity, each part turned about its pivot by {@link #pose} (procedural animation, {@link MeshPose}). One instance per entity
 * renderer: the renderer is made again on a resource reload, so the mesh is read again then (once, on the first frame it is drawn; a missing or broken file is logged
 * once and {@link #draw} answers false so the caller draws its old model).
 *
 * <p>Cost per entity per frame (rule 11): the body quads once (the Goliath: 2416 near, 1152 with the low-detail copy beyond {@link #LOD_RANGE} blocks) and the glow quads
 * once within {@link #GLOW_RANGE} blocks (330); at most {@link EntityMeshData#MAX_QUADS} in all, two draw batches (cutout, additive). The part matrices are worked out
 * into fields made once, nothing is allocated per frame or per vertex. Render thread only.
 */
public final class BossMeshDraw {
    public static final double LOD_RANGE = 40.0, GLOW_RANGE = 48.0;

    private final ResourceLocation file, lodFile, tex, glowTex;
    private boolean loaded;
    private EntityMeshData full, lod;
    private int[] rolesFull, rolesLod;
    private Matrix4f[] mats = new Matrix4f[0];
    private Matrix3f[] norms = new Matrix3f[0];
    private float[] alphas = new float[0];
    private final Matrix4f base = new Matrix4f();
    private final Matrix3f baseN = new Matrix3f();
    private final Quaternionf turn = new Quaternionf();
    private final Vector3f pos = new Vector3f(), nrm = new Vector3f();
    /** Set it (solve) before {@link #draw}. */
    public final MeshPose pose = new MeshPose();

    /** {@code id}: the file {@code entity_meshes/<id>.txt} (and {@code <id>_lod.txt} if it exists) and the skins {@code textures/entity/boss_mesh/<id>(_glow).png}. */
    public BossMeshDraw(String id) {
        this.file = rl("entity_meshes/" + id + ".txt");
        this.lodFile = rl("entity_meshes/" + id + "_lod.txt");
        this.tex = rl("textures/entity/boss_mesh/" + id + ".png");
        this.glowTex = rl("textures/entity/boss_mesh/" + id + "_glow.png");
    }

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("anime_multiverse", path); }

    /** True when the mesh file was read (reads it on the first call). */
    public boolean available() {
        if (!loaded) load();
        return full != null;
    }

    private void load() {
        loaded = true;
        full = read(file, true);
        lod = full == null ? null : read(lodFile, false);
        if (full == null) return;
        rolesFull = roles(full);
        rolesLod = lod == null ? null : roles(lod);
        int n = Math.max(full.parts.length, lod == null ? 0 : lod.parts.length);
        mats = new Matrix4f[n];
        norms = new Matrix3f[n];
        alphas = new float[n];
        for (int i = 0; i < n; i++) {
            mats[i] = new Matrix4f();
            norms[i] = new Matrix3f();
        }
    }

    private static int[] roles(EntityMeshData m) {
        int[] r = new int[m.parts.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = -1;
            for (int k = 0; k < MeshPose.ALL_ROLES; k++) if (MeshPose.ALL_ROLE_NAMES[k].equals(m.parts[i])) r[i] = k;
        }
        return r;
    }

    private static EntityMeshData read(ResourceLocation at, boolean required) {
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(at);
            if (res.isEmpty()) {
                if (required) LogUtils.getLogger().warn("[anime_multiverse] entity mesh {} not found; the old model is drawn", at);
                return null;
            }
            try (BufferedReader in = MeshText.reader(res.get())) {
                return EntityMeshData.parse(in);
            }
        } catch (Exception ex) {
            LogUtils.getLogger().error("[anime_multiverse] entity mesh {} failed to load; the old model is drawn", at, ex);
            return null;
        }
    }

    /**
     * Draws the body with the current {@link #pose}, feet on the pose's origin, face toward +z, in blocks. {@code bodyArgb}: tint and alpha of the skin; {@code glowArgb}: the
     * glow pass's tint (0 = none); {@code distSqr}: squared distance to the camera (the low-detail copy and the glow cut-off). False when there is no mesh.
     */
    public boolean draw(PoseStack ps, MultiBufferSource buffers, int light, int bodyArgb, int glowArgb, double distSqr) {
        if (!available()) return false;
        boolean far = lod != null && distSqr > LOD_RANGE * LOD_RANGE;
        EntityMeshData m = far ? lod : full;
        int[] roles = far ? rolesLod : rolesFull;
        PoseStack.Pose p = ps.last();
        base.set(p.pose()).scale(m.scale).translate(0f, pose.rootY, 0f).rotateX(pose.rootPitch);
        baseN.set(p.normal()).rotateX(pose.rootPitch);
        for (int i = 0; i < m.parts.length; i++) {
            int par = m.parent[i];
            Matrix4f mi = mats[i].set(par < 0 ? base : mats[par]);
            Matrix3f ni = norms[i].set(par < 0 ? baseN : norms[par]);
            int r = roles[i];
            if (r < 0) continue;
            float rx = pose.get(r, 0), ry = pose.get(r, 1), rz = pose.get(r, 2);
            if (rx == 0f && ry == 0f && rz == 0f) continue;
            turn.rotationZYX(rz, ry, rx);                                       // X first, then Y, then Z (Blender's XYZ Euler, as the previews: tools/blender/bl_entity.py)
            float px = m.pivot[i * 3], py = m.pivot[i * 3 + 1], pz = m.pivot[i * 3 + 2];
            mi.translate(px, py, pz).rotate(turn).translate(-px, -py, -pz);
            ni.rotate(turn);
        }
        int a = bodyArgb >>> 24;
        VertexConsumer vc = buffers.getBuffer(a < 250 ? RenderType.entityTranslucent(tex) : RenderType.entityCutout(tex));
        for (int i = 0; i < m.parts.length; i++) emit(mats[i], norms[i], vc, m.body[i], bodyArgb, light);
        if (glowArgb != 0 && !far && distSqr < GLOW_RANGE * GLOW_RANGE) {
            VertexConsumer g = buffers.getBuffer(SafeAdditive.additive(glowTex));
            for (int i = 0; i < m.parts.length; i++) emit(mats[i], norms[i], g, m.glow[i], glowArgb, 0xF000F0);
        }
        return true;
    }

    /**
     * Devil forms (docs/devil_forms.md): places every part with {@code placer} instead of the procedural pose, so a part can follow a body it is anchored on (the player's), and draws each with
     * its own opacity (forging in, shattering out). A part whose role the placer does not know ({@code -1}) follows its parent. {@code tintRgb} is multiplied into the skin, {@code glowRgb} (0 = none)
     * tints the glow pass, {@code glowK} (0 to 1.5) is its strength, {@code distSqr} picks the low-detail copy and the glow cut-off as in {@link #draw}. The pose stack is at the mesh's origin (feet), y up, face +z.
     * Draws the parts that are fully on in one cutout batch, the others in one translucent batch, the glow in one additive batch; allocates nothing. False when there is no mesh.
     */
    public boolean drawParts(PoseStack ps, MultiBufferSource buffers, int light, int tintRgb, int glowRgb, float glowK, double distSqr, Placer placer) {
        if (!available()) return false;
        boolean far = lod != null && distSqr > LOD_RANGE * LOD_RANGE;
        EntityMeshData m = far ? lod : full;
        int[] roles = far ? rolesLod : rolesFull;
        PoseStack.Pose p = ps.last();
        base.set(p.pose()).scale(m.scale);
        baseN.set(p.normal());
        boolean partial = false;
        for (int i = 0; i < m.parts.length; i++) {
            int r = roles[i], par = m.parent[i];
            if (r < 0) {
                mats[i].set(par < 0 ? base : mats[par]);
                norms[i].set(par < 0 ? baseN : norms[par]);
                alphas[i] = par < 0 ? 1f : alphas[par];
            } else {
                alphas[i] = placer.place(r, m.scale, m.pivot[i * 3], m.pivot[i * 3 + 1], m.pivot[i * 3 + 2], base, baseN, mats[i], norms[i]);
            }
            if (alphas[i] > 0f && alphas[i] < 0.999f) partial = true;
        }
        int rgb = tintRgb & 0xFFFFFF;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(tex));
        for (int i = 0; i < m.parts.length; i++) if (alphas[i] >= 0.999f) emit(mats[i], norms[i], vc, m.body[i], 0xFF000000 | rgb, light);
        if (partial) {
            VertexConsumer vt = buffers.getBuffer(RenderType.entityTranslucent(tex));
            for (int i = 0; i < m.parts.length; i++) if (alphas[i] > 0f && alphas[i] < 0.999f) emit(mats[i], norms[i], vt, m.body[i], (((int) (alphas[i] * 255f + 0.5f)) << 24) | rgb, light);
        }
        if (glowRgb != 0 && glowK > 0.01f && !far && distSqr < GLOW_RANGE * GLOW_RANGE) {
            VertexConsumer g = buffers.getBuffer(SafeAdditive.additive(glowTex));
            int grgb = glowRgb & 0xFFFFFF;
            for (int i = 0; i < m.parts.length; i++) {
                float a = alphas[i] * glowK;
                if (a > 0.01f) emit(mats[i], norms[i], g, m.glow[i], (Math.min(255, (int) (a * 255f + 0.5f)) << 24) | grgb, 0xF000F0);
            }
        }
        return true;
    }

    /** Places one part for {@link #drawParts}: writes its matrix and normal matrix (from {@code base} / {@code baseN}) and returns its opacity, 0 = not drawn. {@code scale}: model units to blocks; pivot in model units. */
    public interface Placer {
        float place(int role, float scale, float px, float py, float pz, Matrix4f base, Matrix3f baseN, Matrix4f out, Matrix3f outN);
    }

    private void emit(Matrix4f m, Matrix3f n, VertexConsumer vc, float[] v, int argb, int light) {
        int a = argb >>> 24, r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
        for (int i = 0; i + EntityMeshData.STRIDE <= v.length; i += EntityMeshData.STRIDE) {
            m.transformPosition(v[i], v[i + 1], v[i + 2], pos);
            n.transform(v[i + 5], v[i + 6], v[i + 7], nrm).normalize();
            vc.addVertex(pos.x, pos.y, pos.z).setColor(r, g, b, a).setUv(v[i + 3] / 128f, v[i + 4] / 128f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                    .setNormal(nrm.x, nrm.y, nrm.z);
        }
    }
}
