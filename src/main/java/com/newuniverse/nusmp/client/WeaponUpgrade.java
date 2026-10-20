package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.client.upgrade.UpgradeConfig;
import com.newuniverse.nusmp.relics.upgrade.UpgradeItems;
import com.newuniverse.nusmp.client.upgrade.UpgradeSpec;
import com.newuniverse.nusmp.client.upgrade.UpgradeSpecs;
import com.newuniverse.nusmp.perf.PerfSettings;
import com.newuniverse.nusmp.perf.VfxQuality;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the upgraded look of the weapons that have one (docs/visual_upgrade/god_melee_a.md), drawn on top of {@link WeaponRenderer}. The weapon's old mesh, skin and glow stay in the jar and
 * are used whenever {@code visual.upgrade} is off ({@link UpgradeConfig}), a weapon has no upgraded mesh, or the upgraded one fails to load.
 * <ul>
 *   <li><b>Mesh:</b> weapon_meshes/up/&lt;id&gt;.txt with its own 1024 skin (textures/entity/weapon/up/), found by {@link #mesh}; {@link WeaponRenderer#mesh} asks here first.</li>
 *   <li><b>Level of detail:</b> weapon_meshes/up/&lt;id&gt;_lod.txt (at most 15% of the quads) from 12 blocks, and without the glow pass from 24 blocks ({@link #pick}); the full mesh near the camera.</li>
 *   <li><b>Orbiting parts:</b> the mesh file's {@code orbit} lines name groups of quads (layer 3 and up) that turn about a pivot: rings, shards, ravens. Drawn here, one push of the pose each.</li>
 *   <li><b>Extra passes:</b> the second emissive map (veins) drawn additive with a brightness wave that runs along the weapon, and a halo shell (the glow map a little larger, faint, breathing).</li>
 *   <li><b>Flicker:</b> a slow flutter of the core glow ({@link #flicker}).</li>
 * </ul>
 * Cost: render thread only; two scratch vectors and one scratch quaternion, no allocation per frame; the passes are skipped on the low effect quality (halo from medium, orbiting glow on low).
 */
final class WeaponUpgrade {
    private WeaponUpgrade() {}

    /** One orbiting group: the quads of layer {@code layer}, turned {@code speed} degrees per tick about the axis through the pivot, starting at {@code phase} degrees. */
    static final class Orbit {
        final int layer;
        final float px, py, pz, ax, ay, az, speed, phase;
        final boolean glow;
        final List<float[]> verts = new ArrayList<>();

        Orbit(int layer, float px, float py, float pz, float ax, float ay, float az, float speed, float phase, boolean glow) {
            this.layer = layer;
            this.px = px;
            this.py = py;
            this.pz = pz;
            this.ax = ax;
            this.ay = ay;
            this.az = az;
            this.speed = speed;
            this.phase = phase;
            this.glow = glow;
        }
    }

    /** What an upgraded mesh carries beyond a plain one. */
    static final class Data {
        UpgradeSpec spec;
        ResourceLocation veinsTex;
        final List<Orbit> orbits = new ArrayList<>();
        WeaponRenderer.Mesh lod, lodDark;
    }

    private static final WeaponRenderer.Mesh NONE = new WeaponRenderer.Mesh();
    private static final Map<String, WeaponRenderer.Mesh> UP = new HashMap<>();
    private static final Vector3f SCRATCH_POS = new Vector3f(), SCRATCH_NORMAL = new Vector3f();
    private static final Quaternionf SCRATCH_Q = new Quaternionf();
    private static final float[] SIN = new float[1024];

    static {
        for (int i = 0; i < SIN.length; i++) SIN[i] = (float) Math.sin(2.0 * Math.PI * i / SIN.length);
    }

    private static float sin(float radians) {
        int i = (int) (radians * (SIN.length / (2f * (float) Math.PI)));
        return SIN[i & (SIN.length - 1)];
    }

    static void clear() {
        UP.clear();
        UpgradeSpecs.clear();
    }

    private static ResourceLocation tex(String path) { return DemonSlayerRenderer.rl("textures/entity/weapon/" + path); }

    // ================================================================ loading
    /** The upgraded mesh of a mesh id (a weapon or one of its draw states), or null: option off, no upgrade, or it did not load. */
    static WeaponRenderer.Mesh mesh(String id) {
        if (!UpgradeConfig.enabled()) return null;
        WeaponRenderer.Mesh cached = UP.get(id);                                   // asked several times per frame for every weapon drawn: a map hit, nothing else
        if (cached != null) return cached == NONE ? null : cached;
        String base = UpgradeItems.baseId(id);
        if (!UpgradeItems.has(base)) {
            UP.put(id, NONE);
            return null;
        }
        WeaponRenderer.Mesh m = null;
        try {
            UpgradeSpec spec = UpgradeSpecs.get(base);
            if (spec != null) {
                m = WeaponRenderer.read(id, "weapon_meshes/up/" + id + ".txt", tex(spec.skin), tex(spec.glow));
                if (m != null) {
                    Data d = data(m);
                    d.spec = spec;
                    d.veinsTex = tex(spec.veins);
                    if (id.equals(base)) {
                        WeaponRenderer.Mesh lod = WeaponRenderer.read(id, "weapon_meshes/up/" + id + "_lod.txt", tex(spec.lodSkin), tex(spec.lodGlow));       // the low-detail mesh is classed and placed as its weapon
                        if (lod != null) {
                            d.lod = lod;
                            d.lodDark = darkCopy(lod);
                        }
                    } else {
                        WeaponRenderer.Mesh rest = mesh(base);                                 // a draw state shares the level of detail of its rest mesh
                        if (rest != null && rest.up != null) {
                            d.lod = rest.up.lod;
                            d.lodDark = rest.up.lodDark;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            com.mojang.logging.LogUtils.getLogger().error("[nusmp] upgraded weapon mesh {} failed to load; the old one is used", id, ex);
            m = null;
        }
        UP.put(id, m == null ? NONE : m);
        return m;
    }

    static Data data(WeaponRenderer.Mesh m) {
        if (m.up == null) m.up = new Data();
        return m.up;
    }

    /** The orbit group of a layer number (3 and up), made on first use by an {@code orbit} line; quads of a layer without such a line are kept apart and never drawn. */
    static List<float[]> orbitLayer(WeaponRenderer.Mesh m, int layer) {
        Data d = data(m);
        for (Orbit o : d.orbits) if (o.layer == layer) return o.verts;
        Orbit o = new Orbit(layer, 0, 0, 0, 0, 1, 0, 0, 0, false);
        d.orbits.add(o);
        return o.verts;
    }

    /** `orbit <layer> <px py pz> <ax ay az> <degrees per tick> <phase degrees> <glow 0|1>`: replaces the placeholder group of that layer (the lines come before the quads). */
    static void addOrbit(WeaponRenderer.Mesh m, String[] v) {
        if (v.length < 11) return;
        Data d = data(m);
        int layer = Integer.parseInt(v[1]);
        d.orbits.removeIf(o -> o.layer == layer);
        d.orbits.add(new Orbit(layer, Float.parseFloat(v[2]), Float.parseFloat(v[3]), Float.parseFloat(v[4]), Float.parseFloat(v[5]), Float.parseFloat(v[6]), Float.parseFloat(v[7]),
                Float.parseFloat(v[8]), Float.parseFloat(v[9]), v[10].equals("1")));
    }

    /** The same quads with no glow texture: what is drawn from 24 blocks. */
    private static WeaponRenderer.Mesh darkCopy(WeaponRenderer.Mesh m) {
        WeaponRenderer.Mesh c = new WeaponRenderer.Mesh();
        c.ox = m.ox;
        c.oy = m.oy;
        c.angle = m.angle;
        c.scale = m.scale;
        c.ymin = m.ymin;
        c.ymax = m.ymax;
        c.length = m.length;
        c.gripX = m.gripX;
        c.gripY = m.gripY;
        c.manual = m.manual;
        c.glowSpeed = m.glowSpeed;
        c.glowMin = m.glowMin;
        c.glowMax = m.glowMax;
        c.glowRgb = m.glowRgb;
        c.tex = m.tex;
        c.glowTex = null;
        for (int i = 0; i < m.layers.length; i++) c.layers[i].addAll(m.layers[i]);
        return c;
    }

    // ================================================================ level of detail
    /** The mesh to draw for the pose the item is drawn in: the full one near the camera, the low-detail one from the spec's near distance, that without glow from its glow-off distance. */
    static WeaponRenderer.Mesh pick(WeaponRenderer.Mesh m, Matrix4f pose) {
        Data d = m.up;
        if (d == null || d.lod == null) return m;
        float x = pose.m30(), y = pose.m31(), z = pose.m32();
        float d2 = x * x + y * y + z * z;
        float near = d.spec.lodNear;
        if (d2 < near * near) return m;
        float off = d.spec.lodGlowOff;
        return d2 < off * off ? d.lod : d.lodDark;
    }

    /** Distance squared of the pose's origin from the camera. */
    static float distSq(Matrix4f pose) {
        float x = pose.m30(), y = pose.m31(), z = pose.m32();
        return x * x + y * y + z * z;
    }

    // ================================================================ idle animations of the core glow
    /** The flutter of the core glow (1 for a weapon without it): two slow sines multiplied, so it flickers rather than breathes. */
    static float flicker(WeaponRenderer.Mesh m, float time) {
        Data d = m.up;
        if (d == null || !d.spec.idles("rune_flicker")) return 1f;
        float s = d.spec.flickerSpeed;
        return 1f + d.spec.flickerAmount * sin(time * s * 1.7f) * sin(time * s * 0.53f + 1.3f);
    }

    // ================================================================ the extra drawing
    /** Draws the orbiting parts and the extra passes of an upgraded mesh, in the pose the body was drawn in. */
    static void draw(WeaponRenderer.Mesh mesh, PoseStack pose, MultiBufferSource buffers, int light, int overlay, float time) {
        Data d = mesh.up;
        if (d == null) return;
        VfxQuality q = PerfSettings.vfxQuality();
        UpgradeSpec s = d.spec;
        if (!d.orbits.isEmpty()) {
            float turn = UpgradeConfig.orbits() && s.idles("orbit_parts") ? time : 0f;                  // off: the groups stand still at their start angle
            VertexConsumer body = buffers.getBuffer(RenderType.entityCutout(mesh.tex));
            float k = com.newuniverse.nusmp.client.relics.item.VariantRules.pulse(time, mesh.glowSpeed, mesh.glowMin, mesh.glowMax, 1f);
            int g = com.newuniverse.nusmp.client.relics.item.VariantRules.channel(255, k);
            for (Orbit o : d.orbits) {
                if (o.verts.isEmpty()) continue;
                pose.pushPose();
                pose.translate(o.px, o.py, o.pz);
                pose.mulPose(SCRATCH_Q.rotationAxis((float) Math.toRadians(o.phase + o.speed * turn), o.ax, o.ay, o.az));
                pose.translate(-o.px, -o.py, -o.pz);
                emitQuads(pose.last(), body, o.verts, 0xFFFFFFFF, light, overlay);
                if (o.glow && mesh.glowTex != null && q != VfxQuality.LOW) {
                    emitQuads(pose.last(), buffers.getBuffer(RenderType.eyes(mesh.glowTex)), o.verts, 0xFF000000 | g << 16 | g << 8 | g, 0xF000F0, OverlayTexture.NO_OVERLAY);
                }
                pose.popPose();
            }
        }
        if (!UpgradeConfig.extraPasses() || mesh.glowTex == null || mesh.layers[WeaponRenderer.GLOW].isEmpty()) return;
        if (s.idles("flow_veins") && d.veinsTex != null) {
            emitWave(pose.last(), buffers.getBuffer(RenderType.eyes(d.veinsTex)), mesh.layers[WeaponRenderer.GLOW], s, time);
        }
        if (s.idles("halo_breath") && q != VfxQuality.LOW && q != VfxQuality.MEDIUM) {
            pose.pushPose();
            pose.scale(s.haloScale, 1f, s.haloScale);
            float t = 0.5f + 0.5f * sin(time * s.haloSpeed * 2f);
            int a = Math.round(s.haloAlphaMin + (s.haloAlphaMax - s.haloAlphaMin) * t);
            emitQuads(pose.last(), buffers.getBuffer(RenderType.entityTranslucentEmissive(mesh.glowTex)), mesh.layers[WeaponRenderer.GLOW], a << 24 | (s.haloRgb & 0xFFFFFF), 0xF000F0, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }

    private static void emitQuads(PoseStack.Pose p, VertexConsumer vc, List<float[]> verts, int argb, int light, int overlay) {
        Matrix4f m = p.pose();
        Matrix3f nm = p.normal();
        int a = argb >>> 24, r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
        Vector3f q = SCRATCH_POS, n = SCRATCH_NORMAL;
        for (int i = 0, size = verts.size(); i < size; i++) {
            float[] v = verts.get(i);
            m.transformPosition(v[0], v[1], v[2], q);
            nm.transform(v[5], v[6], v[7], n).normalize();
            vc.addVertex(q.x, q.y, q.z).setColor(r, g, b, a).setUv(v[3] / 128f, v[4] / 128f).setOverlay(overlay).setLight(light).setNormal(n.x, n.y, n.z);
        }
    }

    /** The veins pass: the same quads as the glow, each vertex lit by a wave that runs along the weapon (model y), so the energy seems to flow through it. */
    private static void emitWave(PoseStack.Pose p, VertexConsumer vc, List<float[]> verts, UpgradeSpec s, float time) {
        Matrix4f m = p.pose();
        Matrix3f nm = p.normal();
        Vector3f q = SCRATCH_POS, n = SCRATCH_NORMAL;
        float k = 2f * (float) Math.PI / Math.max(4f, s.veinsWavelength);
        float phase = time * s.veinsSpeed;
        float lo = s.veinsMin, hi = s.veinsMax;
        for (int i = 0, size = verts.size(); i < size; i++) {
            float[] v = verts.get(i);
            m.transformPosition(v[0], v[1], v[2], q);
            nm.transform(v[5], v[6], v[7], n).normalize();
            float w = 0.5f + 0.5f * sin(v[1] * k - phase);
            int a = Math.max(0, Math.min(255, Math.round(255f * (lo + (hi - lo) * w))));
            vc.addVertex(q.x, q.y, q.z).setColor(255, 255, 255, a).setUv(v[3] / 128f, v[4] / 128f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(n.x, n.y, n.z);
        }
    }
}
