package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.relics.item.HolderTracker;
import com.newuniverse.nusmp.client.vupgrade.godb.UpgradeConfig;
import com.newuniverse.nusmp.client.vupgrade.godb.UpgradeMath;
import com.newuniverse.nusmp.client.vupgrade.godb.UpgradeSpec;
import com.newuniverse.nusmp.client.vupgrade.godb.UpgradeSpecs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

/**
 * The extra look of the nine upgraded God melee weapons (release 1.102.0, slice god_melee_b; assets/nusmp/visual_upgrade/&lt;id&gt;.json, docs/visual_upgrade/god_melee_b.md). It lives in
 * the package of {@link WeaponRenderer} because it draws with that class's meshes; WeaponRenderer calls it from five places (the mesh id, the tier of a draw, the glow flicker, the
 * extra passes, the inventory model) and nothing else of the renderer changed. Everything is switched by the client option {@code visual.upgrade}
 * ({@link UpgradeConfig}); with it off every weapon is drawn from its old mesh as before 1.102.0.
 * <ul>
 *   <li><b>Mesh:</b> {@code up/<id>} (the full mesh) near the camera, {@code up/<id>_lod} (at most 14% of the quads) from 12 blocks.</li>
 *   <li><b>Passes</b> after the body and the core glow: the energy-vein pass (the second emissive map, a translucent emissive layer whose brightness is a wave running up the weapon),
 *       the halo shell (the level-of-detail glow quads scaled about the axis, breathing), the orbiting pieces (body and additive glow) and, while the holder swings, the ghost trail.
 *       No additive pass beyond 24 blocks.</li>
 *   <li><b>Cost:</b> one table of 128 floats per frame for the wave, two scratch vectors for every vertex, no allocation per vertex or per piece; the orbiting pieces turn the matrices of the
 *       pose in place; at most 12 pieces; the halo and the ghosts use the level-of-detail quads.</li>
 * </ul>
 */
public final class GodMeleeBRender {
    private GodMeleeBRender() {}

    private static final Vector3f POS = new Vector3f(), NRM = new Vector3f();
    private static final float[] ORBIT = new float[5];
    private static final float[] TABLE = new float[128];
    private static final int FULL_BRIGHT = 0xF000F0;

    // ---------------------------------------------------------------- the five hooks of WeaponRenderer
    /** Hook 1 (WeaponRenderer.meshId): the mesh id of an item: the upgraded one while {@code visual.upgrade} is on and it loads, else {@code id}. */
    static String meshFor(String id) {
        if (!UpgradeConfig.upgrade()) return id;
        UpgradeSpecs.Entry e = UpgradeSpecs.get(id);
        if (e == null) return id;
        return UpgradeMath.meshFor(true, true, WeaponRenderer.mesh(e.spec.mesh()) != null, id, e.spec.mesh());
    }

    /** Hook 2: the level-of-detail tier of this draw (the distance of the pose from the camera), or -1 when the mesh is not an upgraded one. */
    static int tier(String meshId, PoseStack pose) {
        if (!meshId.startsWith("up/")) return -1;
        UpgradeSpecs.Entry e = UpgradeSpecs.byMesh(meshId);
        if (e == null) return -1;
        Matrix4f m = pose.last().pose();
        double dx = m.m30(), dy = m.m31(), dz = m.m32();
        return UpgradeMath.tier(dx * dx + dy * dy + dz * dz, e.spec.lodNearBlocks(), e.spec.additiveBlocks());
    }

    /** Hook 2b: the level-of-detail twin of an upgraded mesh (the mesh itself when the twin is missing). */
    static WeaponRenderer.Mesh lod(WeaponRenderer.Mesh mesh, String meshId) {
        UpgradeSpecs.Entry e = UpgradeSpecs.byMesh(meshId);
        WeaponRenderer.Mesh l = e == null ? null : WeaponRenderer.mesh(e.spec.lodMesh());
        return l != null ? l : mesh;
    }

    /** Hook 3: the multiplier of the core glow (a slow flicker); 1 for everything that is not an upgraded draw. */
    static float flicker(int tier, String meshId, float time) {
        if (tier < 0) return 1f;
        UpgradeSpecs.Entry e = UpgradeSpecs.byMesh(meshId);
        return e == null ? 1f : UpgradeMath.flicker(time, e.spec.coreFlicker(), e.spec.coreFlickerSpeed());
    }

    /** Hook 4: the passes after the core glow. {@code pose} is the pose the body was drawn with (the model space of the mesh). */
    static void extras(ItemStack stack, String meshId, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, WeaponRenderer.Mesh drawn, int tier, float time, int light, int overlay) {
        UpgradeSpecs.Entry e = UpgradeSpecs.byMesh(meshId);
        if (e == null || tier >= UpgradeMath.TIER_FAR) return;
        UpgradeSpec s = e.spec;
        PoseStack.Pose p = pose.last();
        List<float[]> glowQuads = drawn.layers[WeaponRenderer.GLOW];
        // ---- the energy veins: the second emissive map, brightness = a wave running up the weapon
        if (!glowQuads.isEmpty()) {
            UpgradeMath.waveTable(time, s.veinSpeed(), s.veinWavelength(), s.veinBase(), s.veinPeak(), s.axisY0(), s.axisY1(), TABLE);
            emitWave(p, buffers.getBuffer(RenderType.entityTranslucentEmissive(e.veinTexture)), glowQuads, s.axisY0(), s.axisY1());
        }
        // ---- the halo shell and the swing ghosts use the level-of-detail glow quads
        WeaponRenderer.Mesh lodMesh = drawn;
        if (drawn != null && tier == UpgradeMath.TIER_FULL) {
            WeaponRenderer.Mesh l = WeaponRenderer.mesh(s.lodMesh());
            if (l != null) lodMesh = l;
        }
        if (lodMesh.glowTex != null && !lodMesh.layers[WeaponRenderer.GLOW].isEmpty()) {
            float sc = UpgradeMath.haloScale(s.haloScale(), time, s.haloBreath(), s.haloSpeed());
            int a = UpgradeMath.haloAlpha(s.haloAlpha(), time, s.haloBreath(), s.haloSpeed() * 7f);
            pose.pushPose();
            pose.scale(sc, 1f, sc);
            WeaponRenderer.emit(pose.last(), buffers.getBuffer(RenderType.entityTranslucentEmissive(lodMesh.glowTex)), lodMesh.layers[WeaponRenderer.GLOW], a << 24 | s.coreRgb(), FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            pose.popPose();
            swing(s, ctx, pose, buffers, lodMesh, time);
        }
        orbiters(s, pose, buffers, time, light, overlay);
    }

    /** Hook 5: the inventory. With {@code visual.guiModel} on the upgraded model is drawn tilted into the slot; returns whether it drew (false = the caller draws the sprite as always). */
    static boolean gui(String id, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!UpgradeConfig.upgrade() || !UpgradeConfig.guiModel()) return false;
        String up = meshFor(id);
        if (!up.startsWith("up/")) return false;
        WeaponRenderer.Mesh mesh = WeaponRenderer.mesh(up);
        if (mesh == null) return false;
        float k = Math.min(1f, 1.35f / Math.max(0.05f, mesh.length));
        double ang = Math.toRadians(mesh.angle);
        float mx = mesh.ox + (float) Math.cos(ang) * mesh.length * 0.5f, my = mesh.oy + (float) Math.sin(ang) * mesh.length * 0.5f;
        pose.pushPose();
        pose.translate(0.5f, 0.5f, 0.5f);
        pose.mulPose(Axis.YP.rotationDegrees(-24f));
        pose.mulPose(Axis.XP.rotationDegrees(10f));
        pose.scale(k, k, k);
        pose.translate(-mx, -my, -0.5f);
        pose.translate(mesh.ox, mesh.oy, 0.5f);
        pose.mulPose(Axis.ZP.rotationDegrees(mesh.angle - 90f));
        pose.scale(mesh.scale, mesh.scale, mesh.scale);
        pose.translate(0f, -mesh.ymin, 0f);
        PoseStack.Pose p = pose.last();
        WeaponRenderer.emit(p, buffers.getBuffer(RenderType.entityCutout(mesh.tex)), mesh.layers[WeaponRenderer.BODY], 0xFFFFFFFF, light, overlay);
        if (mesh.glowTex != null && !mesh.layers[WeaponRenderer.GLOW].isEmpty()) {
            WeaponRenderer.emit(p, buffers.getBuffer(RenderType.eyes(mesh.glowTex)), mesh.layers[WeaponRenderer.GLOW], 0xFFFFFFFF, FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        }
        pose.popPose();
        return true;
    }

    // ---------------------------------------------------------------- passes
    /** The vein pass: every vertex of the glow quads gets its alpha from the wave at its height. */
    private static void emitWave(PoseStack.Pose p, VertexConsumer vc, List<float[]> verts, float y0, float y1) {
        Matrix4f m = p.pose();
        Matrix3f nm = p.normal();
        Vector3f q = POS, n = NRM;
        for (int i = 0, size = verts.size(); i < size; i++) {
            float[] v = verts.get(i);
            m.transformPosition(v[0], v[1], v[2], q);
            nm.transform(v[5], v[6], v[7], n).normalize();
            int argb = UpgradeMath.argb(UpgradeMath.lookup(TABLE, v[1], y0, y1));
            vc.addVertex(q.x, q.y, q.z).setColor(argb >> 16 & 255, argb >> 8 & 255, argb & 255, argb >>> 24).setUv(v[3] / 128f, v[4] / 128f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(n.x, n.y, n.z);
        }
    }

    /** The ghost copies behind the blade while its holder swings: the level-of-detail glow quads turned about the grip by a growing lag, fading. */
    private static void swing(UpgradeSpec s, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, WeaponRenderer.Mesh lodMesh, float time) {
        UpgradeSpec.Trail t = s.trail();
        if (t.ghosts() <= 0 || t.alpha() <= 0) return;
        LivingEntity holder = HolderTracker.holderFor(ctx);
        if (holder == null || !holder.swinging) return;
        float progress = holder.getAttackAnim(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        float pivot = s.axisY0() + 0.22f * (s.axisY1() - s.axisY0());
        for (int g = 1; g <= t.ghosts(); g++) {
            int a = UpgradeMath.swingAlpha(progress, t.alpha(), g, t.ghosts());
            if (a <= 0) continue;
            pose.pushPose();
            pose.translate(0f, pivot, 0f);
            PoseStack.Pose q = pose.last();
            float rad = (float) Math.toRadians(-t.lagDeg() * g);
            q.pose().rotateZ(rad);
            q.normal().rotateZ(rad);
            pose.translate(0f, -pivot, 0f);
            WeaponRenderer.emit(pose.last(), buffers.getBuffer(RenderType.entityTranslucentEmissive(lodMesh.glowTex)), lodMesh.layers[WeaponRenderer.GLOW], a << 24 | t.rgb(), FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }

    /** The orbiting pieces: all bodies first, then all glows (one render type at a time, so the buffer source does not flush between pieces). */
    private static void orbiters(UpgradeSpec s, PoseStack pose, MultiBufferSource buffers, float time, int light, int overlay) {
        if (s.orbiters().isEmpty()) return;
        WeaponRenderer.Mesh om = WeaponRenderer.mesh(s.orbitMesh());
        if (om == null) return;
        for (int pass = 0; pass < 2; pass++) {
            if (pass == 1 && (om.glowTex == null || om.layers[WeaponRenderer.GLOW].isEmpty())) break;
            VertexConsumer vc = pass == 0 ? buffers.getBuffer(RenderType.entityCutout(om.tex)) : buffers.getBuffer(RenderType.eyes(om.glowTex));
            List<float[]> quads = om.layers[pass == 0 ? WeaponRenderer.BODY : WeaponRenderer.GLOW];
            for (UpgradeSpec.Orbiter ob : s.orbiters()) {
                for (int i = 0; i < ob.count(); i++) {
                    UpgradeMath.orbit(ob, i, time, s.axisY0(), s.axisY1(), ORBIT);
                    pose.pushPose();
                    pose.translate(ORBIT[0], ORBIT[1], ORBIT[2]);
                    PoseStack.Pose q = pose.last();
                    float ry = -ORBIT[3] + ORBIT[4];
                    q.pose().rotateY(ry).rotateX(ob.tilt()).scale(ob.scale());
                    q.normal().rotateY(ry).rotateX(ob.tilt());
                    WeaponRenderer.emit(q, vc, quads, 0xFFFFFFFF, pass == 0 ? light : FULL_BRIGHT, pass == 0 ? overlay : OverlayTexture.NO_OVERLAY);
                    pose.popPose();
                }
            }
        }
    }
}
