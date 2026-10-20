package com.newuniverse.nusmp.client.relics.item.vu;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.NURenderTypes;
import com.newuniverse.nusmp.client.relics.item.HolderTracker;
import com.newuniverse.nusmp.client.relics.item.OversizeGuard;
import com.newuniverse.nusmp.client.relics.item.VariantRules;
import com.newuniverse.nusmp.client.relics.item.WeaponClasses;
import com.newuniverse.nusmp.perf.PerfSettings;
import com.newuniverse.nusmp.perf.VfxQuality;
import com.newuniverse.nusmp.visual.genesis.VuMesh;
import com.newuniverse.nusmp.visual.genesis.VuPose;
import com.newuniverse.nusmp.visual.genesis.VuRules;
import com.newuniverse.nusmp.visual.genesis.VuSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Draws the upgraded Genesis weapons (assets/nusmp/weapon_meshes_vu, tools/visual_upgrade/genesis_weapons), called once from {@code WeaponRenderer#renderByItem} for a
 * mesh id that has an upgraded mesh; false means "draw the old mesh" (the flag {@code visual.upgrade} is off, or the weapon ships no upgrade for this mesh).
 * <p>
 * Passes, in this order (one buffer at a time, as the old renderer does): body (cutout, with the enchantment glint), the void (the dimensional void shader through rifts
 * of the metal), the core glow (additive, pulsing), the energy veins (additive, a wave of light flowing along the weapon), the halo cards (additive sprites of a group: rings,
 * shafts, arcs, each with its own tint and breathing alpha) and the flares (camera-facing stars). The animation groups of the mesh (orbiting parts, turning rings, flickering
 * arcs) are posed by {@link VuPose} from the effect data ({@link VuSpec}); a draw animation unfolds them the first frames after the weapon appears.
 * <p>
 * Performance (docs/visual_upgrade/genesis_weapons.md): the mesh arrays are parsed once per resource reload and drawn straight from flat float arrays; every matrix, vector and
 * pose is a scratch object; beyond 12 blocks (third person, frames, drops) the LOD mesh (at most 15 percent of the vertices) is drawn, beyond 24 blocks without the additive
 * passes; at low effect quality there are no halo cards, flares or group animation.
 */
public final class VuRenderer {
    private VuRenderer() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static final ResourceLocation HALO = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/weapon_vu/halo.png");

    /** One weapon's loaded data: the meshes, the effect data and the texture locations (all built once). */
    static final class Entry {
        VuMesh mesh, lod;
        VuSpec spec;
        ResourceLocation skin, glow, veins, lodSkin, lodGlow;
        Quaternionf turn;
        VuSpec.Group[] specOf;                 // per mesh group: its spec group or null
        VuSpec.Group[] lodSpecOf;
    }

    private static final Entry MISSING = new Entry();
    private static final Map<String, Entry> CACHE = new HashMap<>();
    /** The sightings of weapons per holder (entity id, -1 = none): {last seen tick, draw start tick}. */
    private static final Map<Integer, long[]> SEEN = new HashMap<>();

    private static final Matrix4f GM = new Matrix4f(), ROT4 = new Matrix4f();
    private static final Matrix3f GN = new Matrix3f(), ROT3 = new Matrix3f(), INV = new Matrix3f();
    private static final Vector3f Q = new Vector3f(), N = new Vector3f(), V1 = new Vector3f(), V2 = new Vector3f(), SCALE = new Vector3f();
    private static final VuPose POSE = new VuPose();

    public static void clear() {
        CACHE.clear();
        SEEN.clear();
    }

    // ================================================================ loading
    static Entry entry(String meshId) {
        Entry e = CACHE.get(meshId);
        if (e != null) return e == MISSING ? null : e;
        e = load(meshId);
        CACHE.put(meshId, e == null ? MISSING : e);
        return e;
    }

    private static Entry load(String meshId) {
        try {
            Minecraft mc = Minecraft.getInstance();
            Optional<Resource> res = mc.getResourceManager().getResource(rl("weapon_meshes_vu/" + meshId + ".txt"));
            if (res.isEmpty()) return null;
            Entry e = new Entry();
            try (BufferedReader in = res.get().openAsReader()) { e.mesh = VuMesh.parse(in); }
            String base = WeaponClasses.baseId(meshId);
            Optional<Resource> sp = mc.getResourceManager().getResource(rl("visual_upgrade/" + base + ".json"));
            if (sp.isPresent()) {
                try (BufferedReader in = sp.get().openAsReader()) {
                    StringBuilder sb = new StringBuilder();
                    char[] buf = new char[4096];
                    int n;
                    while ((n = in.read(buf)) > 0) sb.append(buf, 0, n);
                    e.spec = VuSpec.parse(sb.toString());
                }
            }
            e.skin = rl("textures/entity/weapon_vu/" + e.mesh.skin + ".png");
            e.glow = exists(mc, "textures/entity/weapon_vu/" + e.mesh.skin + "_glow.png") ? rl("textures/entity/weapon_vu/" + e.mesh.skin + "_glow.png") : null;
            e.veins = exists(mc, "textures/entity/weapon_vu/" + e.mesh.skin + "_veins.png") ? rl("textures/entity/weapon_vu/" + e.mesh.skin + "_veins.png") : null;
            e.turn = new Quaternionf(Axis.ZP.rotationDegrees(e.mesh.angle - 90f));
            e.specOf = specs(e.mesh, e.spec);
            if (!e.mesh.lod.isEmpty()) {
                Optional<Resource> lr = mc.getResourceManager().getResource(rl("weapon_meshes_vu/" + e.mesh.lod + ".txt"));
                if (lr.isPresent()) {
                    try (BufferedReader in = lr.get().openAsReader()) { e.lod = VuMesh.parse(in); }
                    e.lodSkin = rl("textures/entity/weapon_vu/" + e.lod.skin + ".png");
                    e.lodGlow = exists(mc, "textures/entity/weapon_vu/" + e.lod.skin + "_glow.png") ? rl("textures/entity/weapon_vu/" + e.lod.skin + "_glow.png") : null;
                    e.lodSpecOf = specs(e.lod, e.spec);
                }
            }
            return e;
        } catch (Exception ex) {
            LOG.error("[nusmp] upgraded weapon mesh {} failed to load; the old mesh is used", meshId, ex);
            return null;
        }
    }

    private static VuSpec.Group[] specs(VuMesh m, VuSpec s) {
        VuSpec.Group[] out = new VuSpec.Group[m.groups.size()];
        for (int i = 0; i < out.length; i++) out[i] = s == null ? null : s.groups.get(m.groups.get(i).name);
        return out;
    }

    private static boolean exists(Minecraft mc, String path) {
        return mc.getResourceManager().getResource(rl(path)).isPresent();
    }

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", path); }

    /** Does an upgraded mesh exist for this mesh id (and is the flag on)? Cheap after the first call. */
    public static boolean has(String meshId) { return VuOptions.upgrade() && entry(meshId) != null; }

    /** The effect data of a weapon id (null when it has none); used by the client tick effects. */
    public static VuSpec specOf(String itemId) {
        Entry e = entry(itemId);
        return e == null ? null : e.spec;
    }

    // ================================================================ drawing
    /** True when the stack was drawn here; false = the caller draws the old mesh. */
    public static boolean draw(ItemStack stack, String meshId, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!VuOptions.upgrade()) return false;
        Entry e = entry(meshId);
        if (e == null) return false;
        Minecraft mc = Minecraft.getInstance();
        boolean fp = ctx.firstPerson();
        pose.pushPose();
        Matrix4f cur = pose.last().pose();
        double distSq = fp ? 0.0 : (double) cur.m30() * cur.m30() + (double) cur.m31() * cur.m31() + (double) cur.m32() * cur.m32();
        float near = e.spec == null ? VuRules.LOD_NEAR : e.spec.lodNear, far = e.spec == null ? VuRules.ADDITIVE_FAR : e.spec.lodFar;
        boolean lodDraw = e.lod != null && VuRules.useLod(distSq, fp, near);
        boolean additive = !VuRules.additiveCulled(distSq, fp, far);
        VuMesh mesh = lodDraw ? e.lod : e.mesh;
        VuSpec.Group[] specOf = lodDraw ? e.lodSpecOf : e.specOf;
        place(mesh, e, fp, pose);
        float time = mc.level == null ? 0f : mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(false);
        float drawAge = lodDraw ? 1e9f : drawAge(ctx, mc, time);
        render(stack, mesh, e, specOf, lodDraw, additive, fp, pose, buffers, light, overlay, time, drawAge, lodDraw ? e.lodSkin : e.skin, lodDraw ? e.lodGlow : e.glow, lodDraw ? null : e.veins);
        pose.popPose();
        return true;
    }

    /** The inventory slot, when {@code visual.guiModel} is on: the same mesh tilted, no animation groups. */
    public static boolean drawGui(ItemStack stack, String meshId, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!VuOptions.upgrade() || !VuOptions.guiModel()) return false;
        Entry e = entry(meshId);
        if (e == null) return false;
        pose.pushPose();
        pose.translate(0.5f, 0.5f, 0.5f);
        pose.mulPose(Axis.YP.rotationDegrees(-32f));
        pose.mulPose(Axis.XP.rotationDegrees(14f));
        pose.translate(-0.5f, -0.5f, -0.5f);
        place(e.mesh, e, false, pose);
        float time = Minecraft.getInstance().level == null ? 0f : Minecraft.getInstance().level.getGameTime();
        render(stack, e.mesh, e, e.specOf, false, true, false, pose, buffers, light, overlay, time, 1e9f, e.skin, e.glow, e.veins);
        pose.popPose();
        return true;
    }

    private static void place(VuMesh mesh, Entry e, boolean fp, PoseStack pose) {
        if (fp) {                                                                     // the oversize rule of WeaponRenderer: no weapon goes through the camera
            float len = mesh.length();
            float k = OversizeGuard.factor(len, pose.last().pose().getScale(SCALE).x);
            if (k < 1f) {
                float along = OversizeGuard.GRIP_FRACTION * len;
                float gx = mesh.ox + along * (float) Math.cos(Math.toRadians(mesh.angle)), gy = mesh.oy + along * (float) Math.sin(Math.toRadians(mesh.angle));
                pose.translate(gx, gy, 0.5f);
                pose.scale(k, k, k);
                pose.translate(-gx, -gy, -0.5f);
            }
        }
        pose.translate(mesh.ox, mesh.oy, 0.5f);
        pose.mulPose(e.turn);
        pose.scale(mesh.scale, mesh.scale, mesh.scale);
        pose.translate(0f, -mesh.ymin, 0f);
    }

    /** Ticks since this holder's weapon was drawn: a sighting after a gap of more than 6 ticks starts the draw animation again. */
    private static float drawAge(ItemDisplayContext ctx, Minecraft mc, float time) {
        LivingEntity holder = HolderTracker.holderFor(ctx);
        int key = holder == null ? -1 : holder.getId();
        long now = (long) time;
        long[] s = SEEN.get(key);
        if (s == null) {
            if (SEEN.size() > 64) SEEN.clear();
            s = new long[]{-1L, now};
            SEEN.put(key, s);
        }
        if (VuRules.newDraw(s[0], now, 6)) s[1] = now;
        s[0] = now;
        return time - s[1];
    }

    // ---------------------------------------------------------------- the passes
    private static void render(ItemStack stack, VuMesh mesh, Entry e, VuSpec.Group[] specOf, boolean lodDraw, boolean additive, boolean fp, PoseStack pose, MultiBufferSource buffers,
                               int light, int overlay, float time, float drawAge, ResourceLocation skin, ResourceLocation glowTex, ResourceLocation veinTex) {
        VfxQuality q = PerfSettings.vfxQuality();
        boolean animate = !lodDraw && VuRules.animationAllowed(q);
        PoseStack.Pose p = pose.last();
        Matrix4f pm = p.pose();
        Matrix3f pn = p.normal();
        int n = mesh.groups.size();

        // 1 body
        VertexConsumer vc = ItemRenderer.getFoilBufferDirect(buffers, RenderType.entityCutout(skin), false, stack.hasFoil());
        for (int i = 0; i < n; i++) {
            VuMesh.Group g = mesh.groups.get(i);
            if (g.count[VuMesh.BODY] == 0) continue;
            boolean xf = groupMatrix(g, specOf[i], animate, time, drawAge, pm, pn, true);
            emit(xf ? GM : pm, xf ? GN : pn, vc, g.verts[VuMesh.BODY], g.count[VuMesh.BODY], 0xFFFFFFFF, light, overlay);
        }
        if (!additive) return;
        // 2 the void
        if (mesh.quads(VuMesh.VOID) > 0) {
            VertexConsumer vv = buffers.getBuffer(NURenderTypes.demonVoid());
            for (int i = 0; i < n; i++) {
                VuMesh.Group g = mesh.groups.get(i);
                if (g.count[VuMesh.VOID] == 0) continue;
                boolean xf = groupMatrix(g, specOf[i], animate, time, drawAge, pm, pn, false);
                Matrix4f m = xf ? GM : pm;
                float[] a = g.verts[VuMesh.VOID];
                for (int k = 0; k < g.count[VuMesh.VOID]; k += VuMesh.FLOATS) {
                    m.transformPosition(a[k], a[k + 1], a[k + 2], Q);
                    vv.addVertex(Q.x, Q.y, Q.z);
                }
            }
        }
        // 3 the core glow
        float pulse = VariantRules.pulse(time, mesh.glowSpeed, mesh.glowMin, mesh.glowMax, VariantRules.GENESIS_BOOST);
        float flash = drawFlash(drawAge, e);
        int rgb = mesh.glowRgb;
        int gr = VariantRules.channel(rgb >> 16 & 255, pulse + flash), gg = VariantRules.channel(rgb >> 8 & 255, pulse + flash), gb = VariantRules.channel(rgb & 255, pulse + flash);
        if (glowTex != null && mesh.quads(VuMesh.GLOW) > 0) {
            VertexConsumer gv = buffers.getBuffer(RenderType.eyes(glowTex));
            int col = 0xFF000000 | gr << 16 | gg << 8 | gb;
            for (int i = 0; i < n; i++) {
                VuMesh.Group g = mesh.groups.get(i);
                if (g.count[VuMesh.GLOW] == 0) continue;
                boolean xf = groupMatrix(g, specOf[i], animate, time, drawAge, pm, pn, true);
                emit(xf ? GM : pm, xf ? GN : pn, gv, g.verts[VuMesh.GLOW], g.count[VuMesh.GLOW], col, 0xF000F0, OverlayTexture.NO_OVERLAY);
            }
        }
        // 4 the energy veins: a wave of light flowing along the weapon
        if (veinTex != null && mesh.quads(VuMesh.VEINS) > 0) {
            VertexConsumer ev = buffers.getBuffer(RenderType.eyes(veinTex));
            float speed = 0.012f * (1f + 0.5f * flash), freq = 0.045f;
            for (int i = 0; i < n; i++) {
                VuMesh.Group g = mesh.groups.get(i);
                if (g.count[VuMesh.VEINS] == 0) continue;
                boolean xf = groupMatrix(g, specOf[i], animate, time, drawAge, pm, pn, true);
                emitWave(xf ? GM : pm, xf ? GN : pn, ev, g.verts[VuMesh.VEINS], g.count[VuMesh.VEINS], rgb, time, freq, speed, 0xF000F0);
            }
        }
        // 5 the halo cards and 6 the flares: medium quality and up
        if (VuRules.haloAllowed(q) && !lodDraw && e.spec != null && mesh.quads(VuMesh.HALO) > 0) {
            VertexConsumer hv = buffers.getBuffer(RenderType.eyes(HALO));
            for (int i = 0; i < n; i++) {
                VuMesh.Group g = mesh.groups.get(i);
                VuSpec.Group sg = specOf[i];
                if (g.count[VuMesh.HALO] == 0 || sg == null) continue;
                boolean xf = groupMatrix(g, sg, animate, time, drawAge, pm, pn, true);
                float a = POSE.alpha * sg.alpha;
                if (a <= 0.01f) continue;
                int col = VuRules.argb(sg.tintRgb, Math.min(1f, a));
                emit(xf ? GM : pm, xf ? GN : pn, hv, g.verts[VuMesh.HALO], g.count[VuMesh.HALO], col, 0xF000F0, OverlayTexture.NO_OVERLAY);
            }
            if (VuRules.flaresAllowed(q) && !mesh.flares.isEmpty()) flares(mesh, pm, fp, buffers, time);
        }
    }

    /** The brightness added to the glow by the draw animation: a flash that fades over its ticks. */
    private static float drawFlash(float drawAge, Entry e) {
        if (e.spec == null || e.spec.draw == null) return 0f;
        int t = e.spec.draw.ticks();
        if (drawAge >= t + 8 || drawAge < 0f) return 0f;
        float p = VuRules.drawProgress(drawAge, t + 8);
        return 0.9f * (1f - p);
    }

    /**
     * The matrices of a group: with an animation the group's pose about its pivot ({@link VuPose#eval}), else the item's own. Returns true when GM / GN hold the group's
     * matrices; the pose of the group stays in {@link #POSE} for its alpha.
     */
    private static boolean groupMatrix(VuMesh.Group g, VuSpec.Group sg, boolean animate, float time, float drawAge, Matrix4f pm, Matrix3f pn, boolean normals) {
        if (sg == null) {
            POSE.reset();
            return false;
        }
        VuPose ps = animate ? POSE.eval(sg, time, drawAge) : POSE.eval(sg, 0f, 1e9f);            // without animation (low quality, the LOD mesh) a group rests in its first pose
        if (ps.identity()) return false;
        float[] r = ps.r;
        ROT4.set(r[0], r[3], r[6], 0f, r[1], r[4], r[7], 0f, r[2], r[5], r[8], 0f, 0f, 0f, 0f, 1f);
        GM.set(pm).translate(sg.px + ps.ox, sg.py + ps.oy, sg.pz + ps.oz).mul(ROT4).scale(ps.scale).translate(-sg.px, -sg.py, -sg.pz);
        if (normals) {
            ROT3.set(r[0], r[3], r[6], r[1], r[4], r[7], r[2], r[5], r[8]);
            GN.set(pn).mul(ROT3);
        }
        return true;
    }

    private static void emit(Matrix4f m, Matrix3f nm, VertexConsumer vc, float[] a, int count, int argb, int light, int overlay) {
        int al = argb >>> 24, r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
        Vector3f q = Q, nn = N;
        for (int k = 0; k < count; k += VuMesh.FLOATS) {
            m.transformPosition(a[k], a[k + 1], a[k + 2], q);
            nm.transform(a[k + 5], a[k + 6], a[k + 7], nn).normalize();
            vc.addVertex(q.x, q.y, q.z).setColor(r, g, b, al).setUv(a[k + 3] / 128f, a[k + 4] / 128f).setOverlay(overlay).setLight(light).setNormal(nn.x, nn.y, nn.z);
        }
    }

    private static void emitWave(Matrix4f m, Matrix3f nm, VertexConsumer vc, float[] a, int count, int rgb, float time, float freq, float speed, int light) {
        int r0 = rgb >> 16 & 255, g0 = rgb >> 8 & 255, b0 = rgb & 255;
        Vector3f q = Q, nn = N;
        for (int k = 0; k < count; k += VuMesh.FLOATS) {
            float w = VuRules.veinWave(a[k + 1], time, freq, speed);
            m.transformPosition(a[k], a[k + 1], a[k + 2], q);
            nm.transform(a[k + 5], a[k + 6], a[k + 7], nn).normalize();
            vc.addVertex(q.x, q.y, q.z).setColor(VuRules.channel(r0, w), VuRules.channel(g0, w), VuRules.channel(b0, w), 255).setUv(a[k + 3] / 128f, a[k + 4] / 128f)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(nn.x, nn.y, nn.z);
        }
    }

    /** Camera-facing stars at the flare points (a cell of the halo sheet), pulsing with their own phase. */
    private static void flares(VuMesh mesh, Matrix4f pm, boolean fp, MultiBufferSource buffers, float time) {
        Minecraft mc = Minecraft.getInstance();
        float rx, ry, rz, ux, uy, uz;
        if (fp) {
            rx = 1f; ry = 0f; rz = 0f; ux = 0f; uy = 1f; uz = 0f;
        } else {
            net.minecraft.client.Camera cam = mc.gameRenderer.getMainCamera();
            Vector3f left = cam.getLeftVector(), up = cam.getUpVector();
            rx = -left.x; ry = -left.y; rz = -left.z; ux = up.x; uy = up.y; uz = up.z;
        }
        INV.set(pm).invert();                                                         // the camera's axes in the weapon's own frame
        INV.transform(rx, ry, rz, V1).normalize();
        INV.transform(ux, uy, uz, V2).normalize();
        float rx2 = V1.x, ry2 = V1.y, rz2 = V1.z, ux2 = V2.x, uy2 = V2.y, uz2 = V2.z;
        VertexConsumer fv = buffers.getBuffer(RenderType.eyes(HALO));
        for (int i = 0, n = mesh.flares.size(); i < n; i++) {
            VuMesh.Flare f = mesh.flares.get(i);
            float k = 0.6f + 0.4f * f.pulse() * 2f * (0.5f + 0.5f * (float) Math.sin(time * 0.17f + f.phase() * 6.2831f));
            float s = f.size() * (0.8f + 0.25f * k);
            int cr = VuRules.channel(f.rgb() >> 16 & 255, k), cg = VuRules.channel(f.rgb() >> 8 & 255, k), cb = VuRules.channel(f.rgb() & 255, k);
            float u0 = (f.cell() % 4) * 32f + 0.5f, v0 = (f.cell() / 4) * 32f + 0.5f, u1 = u0 + 31f, v1 = v0 + 31f;
            corner(fv, pm, f.x() - rx2 * s - ux2 * s, f.y() - ry2 * s - uy2 * s, f.z() - rz2 * s - uz2 * s, u0, v1, cr, cg, cb);
            corner(fv, pm, f.x() + rx2 * s - ux2 * s, f.y() + ry2 * s - uy2 * s, f.z() + rz2 * s - uz2 * s, u1, v1, cr, cg, cb);
            corner(fv, pm, f.x() + rx2 * s + ux2 * s, f.y() + ry2 * s + uy2 * s, f.z() + rz2 * s + uz2 * s, u1, v0, cr, cg, cb);
            corner(fv, pm, f.x() - rx2 * s + ux2 * s, f.y() - ry2 * s + uy2 * s, f.z() - rz2 * s + uz2 * s, u0, v0, cr, cg, cb);
        }
    }

    private static void corner(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float v, int r, int g, int b) {
        m.transformPosition(x, y, z, Q);
        vc.addVertex(Q.x, Q.y, Q.z).setColor(r, g, b, 255).setUv(u / 128f, v / 128f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0f, 0f, 1f);
    }
}
