package com.newuniverse.nusmp.vfx.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.newuniverse.nusmp.perf.PerfProbe;
import com.newuniverse.nusmp.perf.PerfSettings;
import com.newuniverse.nusmp.perf.RlCache;
import com.newuniverse.nusmp.perf.VfxQuality;
import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.fx.Anim;
import com.newuniverse.nusmp.vfx.fx.EffectLib;
import com.newuniverse.nusmp.vfx.fx.EffectSpec;
import com.newuniverse.nusmp.vfx.fx.Piece;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Renders spec-driven effects (vfx.fx). Each piece's mesh is built ONCE (white, local space) when the
 * effect starts; every frame only its animated transform, colour and alpha change. Each live piece is
 * one draw with the nusmp:fx shader (shimmer / pulse uniforms carry the fine detail).
 *
 * <p>1.99.0 (performance pass, docs/performance_1_99.md): the texture of a piece is looked up in a cache instead of being built ({@code ResourceLocation.fromNamespaceAndPath("nusmp",
 * "textures/fx/" + name + ".png")}) for every piece of every effect on every frame; the sprite and ribbon vertices are placed with scratch vectors instead of three or four new vectors per
 * vertex (the same arithmetic in the same order, so the same floats); an effect whose reach is entirely beyond the far limit of the quality is not drawn; and the number of effects that may
 * run at once follows the quality (48 on high, as before).
 */
public final class FxRenderer {
    private FxRenderer() {}

    private static ShaderInstance shader;
    private static final List<Instance> ACTIVE = new ArrayList<>();
    private static final int MAX_ACTIVE = 48;
    private static final PerfProbe.Section RENDER_SECTION = PerfProbe.section("c.fx.render");

    private static final class Instance {
        final EffectSpec spec;
        final List<float[]> meshes = new ArrayList<>();
        final Vec3 from, to, followOffset;
        final int followId;
        final long seed;
        final double reach;
        int age;
        Instance(EffectSpec spec, VfxPayload p, Vec3 followOffset) {
            this.spec = spec; this.from = p.from(); this.to = p.to(); this.followId = p.followEntity(); this.followOffset = followOffset; this.seed = p.seed();
            this.reach = 12.0 + Math.min(96.0, p.from().distanceTo(p.to())) + 4.0 * Math.min(32.0, Math.max(0.0, p.power()));      // generous: how far the pieces can lie from the end points
            for (Piece pc : spec.pieces()) meshes.add(FxMeshes.build(pc));     // built once, here
        }
    }

    public static void registerShaders(RegisterShadersEvent e) {
        try {
            e.registerShader(new ShaderInstance(e.getResourceProvider(), ResourceLocation.fromNamespaceAndPath("nusmp", "fx"), DefaultVertexFormat.POSITION_TEX_COLOR),
                    s -> shader = s);
        } catch (IOException ex) {
            com.mojang.logging.LogUtils.getLogger().error("[nusmp] FX shader failed to load, falling back to the vanilla shader", ex);
        }
    }

    public static boolean handles(com.newuniverse.nusmp.vfx.VfxShape shape) { return shape.name().startsWith("FX_"); }

    public static void spawn(VfxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        String id = p.shapeType().name().substring(3).toLowerCase();
        EffectLib.Detail detail = mc.options.particles().get() == ParticleStatus.DECREASED ? EffectLib.Detail.REDUCED : EffectLib.Detail.FULL;
        EffectSpec spec = EffectLib.build(id, detail, p.seed(), p.from().distanceTo(p.to()), p.power(), p.color());
        Entity e = p.followEntity() >= 0 ? mc.level.getEntity(p.followEntity()) : null;
        int cap = Math.min(MAX_ACTIVE, PerfSettings.vfxQuality().maxActive);
        while (ACTIVE.size() >= cap) ACTIVE.remove(0);
        ACTIVE.add(new Instance(spec, p, e == null ? null : p.from().subtract(e.position())));
        cues(ACTIVE.get(ACTIVE.size() - 1), 0);
    }

    public static void tick() {
        Iterator<Instance> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Instance in = it.next();
            in.age++;
            if (in.age >= in.spec.life()) { it.remove(); continue; }
            cues(in, in.age);
        }
    }

    /** Sounds and shake fire on the first frame of their phase. */
    private static void cues(Instance in, int tick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (EffectSpec.Cue c : in.spec.cues()) {
            if (c.tick() != tick) continue;
            var ev = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(c.sound()));
            if (ev != null) mc.level.playLocalSound(in.from.x, in.from.y, in.from.z, ev, SoundSource.PLAYERS, c.volume(), c.pitch(), false);
            if (c.shake() > 0) VfxShake.add(in.to, (float) c.shake(), 6);
        }
    }

    public static void clear() { ACTIVE.clear(); }

    // ---------------------------------------------------------------- render
    public static void render(Camera camera, Matrix4f modelView, float partial) {
        if (ACTIVE.isEmpty()) return;
        long t0 = PerfProbe.begin();
        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = camera.getPosition();
        VfxQuality quality = PerfSettings.vfxQuality();
        int culled = 0, drawn = 0;
        Vector3f camRight = new Vector3f(camera.getLeftVector()).negate(), camUp = new Vector3f(camera.getUpVector());
        var mvs = RenderSystem.getModelViewStack();
        mvs.pushMatrix(); mvs.identity(); RenderSystem.applyModelViewMatrix();
        RenderSystem.depthMask(false); RenderSystem.disableCull(); RenderSystem.enableDepthTest(); RenderSystem.enableBlend();
        for (Instance in : ACTIVE) {
            double t = in.age + partial;
            Vec3 origin = in.from;
            if (in.followOffset != null && mc.level != null) {
                Entity e = mc.level.getEntity(in.followId);
                if (e != null) origin = e.getPosition(partial).add(in.followOffset);
            }
            double nearest = Math.sqrt(Math.min(origin.distanceToSqr(cam), in.to.distanceToSqr(cam))) - in.reach;
            if (quality.culled(nearest)) {
                culled++;                                                // every piece lies farther away than anything is drawn
                continue;
            }
            drawn++;
            // effect-local frame: x = from -> to, y = up, z = side
            Vec3 dx = in.to.subtract(in.from);
            Vector3f X = dx.lengthSqr() < 1e-4 ? new Vector3f(1, 0, 0) : new Vector3f((float) dx.x, (float) dx.y, (float) dx.z).normalize();
            Vector3f ref = Math.abs(X.y) > 0.95f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
            Vector3f Y = new Vector3f(ref).sub(new Vector3f(X).mul(X.dot(ref))).normalize();
            Vector3f Z = new Vector3f(X).cross(Y).normalize();
            Vector3f o = new Vector3f((float) (origin.x - cam.x), (float) (origin.y - cam.y), (float) (origin.z - cam.z));
            for (int i = 0; i < in.spec.pieces().size(); i++) {
                Piece pc = in.spec.pieces().get(i);
                if (!pc.aliveAt(t)) continue;
                drawPiece(in, pc, in.meshes.get(i), t, o, X, Y, Z, camRight, camUp, modelView, i);
            }
        }
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.defaultBlendFunc(); RenderSystem.disableBlend(); RenderSystem.enableCull(); RenderSystem.depthMask(true);
        mvs.popMatrix(); RenderSystem.applyModelViewMatrix();
        VfxStats.fxFrame(ACTIVE.size(), drawn, culled);
        PerfProbe.end(RENDER_SECTION, t0);
    }

    private static Vector3f toWorld(Vector3f X, Vector3f Y, Vector3f Z, double x, double y, double z) {
        return new Vector3f(X).mul((float) x).add(new Vector3f(Y).mul((float) y)).add(new Vector3f(Z).mul((float) z));
    }

    private static void drawPiece(Instance in, Piece pc, float[] mesh, double t, Vector3f o, Vector3f X, Vector3f Y, Vector3f Z,
                                  Vector3f camRight, Vector3f camUp, Matrix4f modelView, int index) {
        // ---- animation state
        double alpha = 1, scale = 1, bright = 1, ox = 0, oy = 0, oz = 0;
        int[] pal = in.spec.palette();
        int col = pal[pc.color()];
        for (Anim a : pc.anims()) {
            if (t < a.start()) { if (a.type() == Anim.Type.FADE_IN) alpha = 0; continue; }
            double pr = a.progress(t);
            boolean inside = t <= a.end();
            switch (a.type()) {
                case FADE_IN -> alpha *= pr;
                case FADE_OUT -> alpha *= 1 - pr;
                case GROW -> scale = a.a() + (a.b() - a.a()) * pr;
                case DRIFT -> { ox += a.a() * pr; oy += a.b() * pr; oz += a.c() * pr; }
                case SPIRAL_IN -> {
                    double r = Math.hypot(pc.pos().x(), pc.pos().z());
                    double k = r < 1e-4 ? 0 : Math.min(1, a.a() / r) * pr, th = Math.PI * 2 * a.b() * pr;
                    double nx = (pc.pos().x() * Math.cos(th) - pc.pos().z() * Math.sin(th)) * (1 - k), nz = (pc.pos().x() * Math.sin(th) + pc.pos().z() * Math.cos(th)) * (1 - k);
                    ox += nx - pc.pos().x(); oz += nz - pc.pos().z();
                }
                case SWAY -> { if (inside) { double s = Math.sin(t / 20.0 * a.b() * Math.PI * 2 + index); ox += s * a.a(); oz += Math.cos(t / 20.0 * a.b() * Math.PI * 2 + index) * a.a() * 0.5; } }
                case FLASH -> { if (inside) bright *= 1 + (a.a() - 1) * Math.sin(Math.PI * pr); }
                case FLICKER -> { if (inside) bright *= 1 + a.a() * Math.sin(t / 20.0 * a.b() * Math.PI * 2 + index * 1.7); }
                case TINT -> col = lerpColor(pal[(int) a.a()], pal[(int) a.b()], pr);
            }
        }
        if (alpha <= 0.003 || scale <= 0.001) return;
        int a8 = (int) Math.max(0, Math.min(255, (col >>> 24) * alpha));
        int r = (col >> 16) & 255, g = (col >> 8) & 255, b = col & 255;

        // ---- transform (the mesh itself never changes)
        Vector3f pos = new Vector3f(o).add(toWorld(X, Y, Z, pc.pos().x() + ox, pc.pos().y() + oy, pc.pos().z() + oz));
        Vector3f dirW = toWorld(X, Y, Z, pc.dir().x(), pc.dir().y(), pc.dir().z()).normalize();
        Quaternionf q = new Quaternionf().rotationTo(new Vector3f(0, 1, 0), dirW);
        float s = (float) scale;

        ShaderInstance sh = shader;
        if (sh != null) {
            RenderSystem.setShader(() -> sh);
            sh.safeGetUniform("FxShimmer").set((float) pc.shimmer());
            sh.safeGetUniform("FxPulse").set((float) pc.pulse());
            sh.safeGetUniform("FxSeed").set((float) ((in.seed + index * 31) % 1000));
        } else {
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        }
        ResourceLocation tex = RlCache.fxTexture(pc.texture());                                        // built once per name, not per piece per frame
        Minecraft.getInstance().getTextureManager().getTexture(tex).setFilter(true, false);
        RenderSystem.setShaderTexture(0, tex);
        if (EffectLib.ADDITIVE.contains(pc.texture())) RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        else RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor((float) bright, (float) bright, (float) bright, 1f);

        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        Vector3f v = new Vector3f(), w = new Vector3f(), tmp = new Vector3f();
        for (int k = 0; k < mesh.length; k += 5) {
            float lx = mesh[k], ly = mesh[k + 1], lz = mesh[k + 2];
            switch (pc.mesh()) {
                case SPRITE -> { tmp.set(camUp).mul(ly * s); v.set(camRight).mul(lx * s).add(tmp).add(pos); }
                case RIBBON -> {                                                      // toWorld(X, Y, Z, lx * s, ly * s, lz * s) + pos, with one scratch vector (same order of operations)
                    v.set(X).mul(lx * s);
                    tmp.set(Y).mul(ly * s);
                    v.add(tmp);
                    tmp.set(Z).mul(lz * s);
                    v.add(tmp).add(pos);
                }
                default -> { v.set(lx * s, ly * s, lz * s); q.transform(v); v.add(pos); }
            }
            modelView.transformPosition(v, w);
            bb.addVertex(w.x, w.y, w.z).setUv(mesh[k + 3], mesh[k + 4]).setColor(r, g, b, a8);
        }
        MeshData md = bb.build();
        if (md != null) BufferUploader.drawWithShader(md);
    }

    private static int lerpColor(int c0, int c1, double t) {
        int a = (int) ((c0 >>> 24) + ((c1 >>> 24) - (c0 >>> 24)) * t), r = (int) (((c0 >> 16) & 255) + (((c1 >> 16) & 255) - ((c0 >> 16) & 255)) * t);
        int g = (int) (((c0 >> 8) & 255) + (((c1 >> 8) & 255) - ((c0 >> 8) & 255)) * t), b = (int) ((c0 & 255) + ((c1 & 255) - (c0 & 255)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
