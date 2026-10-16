package com.newuniverse.nusmp.vfx.client.portal;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.client.NUShaders;
import com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment;
import com.newuniverse.nusmp.client.particle.rukh.RukhClientFx;
import com.newuniverse.nusmp.vfx.client.VfxRenderContext;
import com.newuniverse.nusmp.vfx.client.VfxTextures;
import com.newuniverse.nusmp.vfx.client.layer.SpatialPortalLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Random;

/**
 * Why: everything about the 3D gate that needs the running game (the shader, the screen copy, particles, sounds) lives here so the
 * rest of the portal code stays pure and previewable. It plugs into {@link SpatialPortalLayer} as its {@code client} hook.
 * <ul>
 *   <li>Once per frame, when a gate is within 16 blocks and {@code spatial.refraction} is on, the main render target is blitted into a
 *       temporary target and the event horizon is drawn with the portal shader sampling that copy through a swirl offset (the "look
 *       through" effect). Within 3 blocks of a gate a faint full-screen warp ripple (config {@code spatial.warpStrength}) is drawn first.</li>
 *   <li>If the shader is missing, or a draw throws once, the hook stops drawing (logged once) and the layer uses its plain translucent
 *       buffer path; sounds and particles carry on.</li>
 *   <li>Rukh (dark near the core, light near the rim) is emitted at the event horizon through the budgeted {@link RukhClientFx}; no vanilla
 *       particle types are used.</li>
 * </ul>
 * The copy target is created lazily and destroyed when the last gate closes.
 */
public final class PortalClient implements SpatialPortalLayer.Client {
    private static final Random RND = new Random();
    private static boolean broken;
    private static TextureTarget copy;
    private static VfxRenderContext lastFrame;
    private static boolean copyReady;
    private static final Vector3f TMP = new Vector3f();
    private static final GlSink SINK = new GlSink();
    private static final ResourceLocation[] TEX = new ResourceLocation[PortalPass.values().length];

    private PortalClient() {}

    /** Called when the shaders register (whether or not the portal shader built). */
    public static void install() {
        SpatialPortalLayer.client = new PortalClient();
    }

    // ------------------------------------------------------------------ sounds, particles
    @Override
    public void spawned(SpatialPortalLayer.State s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        mc.level.playLocalSound(s.x, s.y + s.centreHeight(), s.z, SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.35f, 1.5f, false);
    }

    @Override
    public void tick(SpatialPortalLayer.State s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        int age = s.inst.age(), dur = s.inst.duration;
        double cy = s.y + s.centreHeight();
        if (age == dur - PortalMath.CLOSE_TICKS) mc.level.playLocalSound(s.x, cy, s.z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.4f, 0.7f, false);
        if (age % 70 == 20 && age > PortalMath.OPEN_TICKS && age < dur - PortalMath.CLOSE_TICKS)
            mc.level.playLocalSound(s.x, cy, s.z, SoundEvents.PORTAL_AMBIENT, SoundSource.PLAYERS, 0.25f, 1.6f, false);
        if (!PortalSettings.particles() || mc.options.particles().get() == ParticleStatus.MINIMAL) return;
        if (age < 6 || age > dur - PortalMath.CLOSE_TICKS) return;
        double dx = mc.player.getX() - s.x, dy = mc.player.getY() - cy, dz = mc.player.getZ() - s.z;
        if (dx * dx + dy * dy + dz * dz > 40 * 40) return;
        int n = (age & 1) == 0 && dx * dx + dz * dz < 20 * 20 ? 2 : 1;
        float hw = 0.9f * s.size, hh = 1.35f * s.size, rx = -s.fz, rz = s.fx;
        for (int i = 0; i < n; i++) {
            double a = RND.nextDouble() * Math.PI * 2, rr = 0.5 + 0.5 * RND.nextDouble();
            double lx = Math.cos(a) * hw * rr, ly = Math.sin(a) * hh * rr;
            double tx = -Math.sin(a) * hw, ty = Math.cos(a) * hh, tl = Math.sqrt(tx * tx + ty * ty);
            double sp = 0.035, depth = (RND.nextBoolean() ? 1 : -1) * 0.02;
            double vx = (tx / tl * sp - Math.cos(a) * 0.008) * rx + depth * s.fx;
            double vy = ty / tl * sp * 0.8 + 0.004;
            double vz = (tx / tl * sp - Math.cos(a) * 0.008) * rz + depth * s.fz;
            RukhClientFx.spray(mc.level, Alignment.of(rr < 0.7), s.x + rx * lx, cy + ly, s.z + rz * lx, 1, vx, vy, vz, 1.0);
        }
    }

    @Override
    public void idle() {
        if (copy != null) { copy.destroyBuffers(); copy = null; }
        lastFrame = null;
        copyReady = false;
    }

    // ------------------------------------------------------------------ drawing
    @Override
    public boolean draw(VfxRenderContext ctx, SpatialPortalLayer.State s, PortalFrame f, int tier) {
        ShaderInstance sh = NUShaders.spatialPortal();
        if (sh == null || broken) return false;
        try {
            if (ctx != lastFrame) { lastFrame = ctx; frameSetup(ctx, sh); }
            f.shader = true;
            PortalSpec spec = PortalSpec.tier(tier);
            SINK.clear();
            SpatialPortalLayer.composer().compose(PortalMesh.of(spec), f, SINK);
            SpatialPortalLayer.thread(ctx, s, f, spec.threadSegments(), SINK);
            SINK.flush(ctx.modelView, sh, copyReady);
            return true;
        } catch (RuntimeException | LinkageError e) {
            broken = true;                                            // once: from now on the plain translucent path is used
            LogUtils.getLogger().error("[nusmp] portal shader draw failed; the plain translucent portal is used instead", e);
            RenderSystem.defaultBlendFunc();
            return false;
        }
    }

    /** Per frame, before the first gate is drawn: the screen copy (if a gate is near) and the warp pulse (if one is very near). */
    private static void frameSetup(VfxRenderContext ctx, ShaderInstance sh) {
        copyReady = false;
        double nearest = Double.MAX_VALUE;
        for (SpatialPortalLayer.State g : SpatialPortalLayer.live()) {
            double dx = g.x - ctx.camPos.x, dy = g.y + g.centreHeight() - ctx.camPos.y, dz = g.z - ctx.camPos.z;
            nearest = Math.min(nearest, Math.sqrt(dx * dx + dy * dy + dz * dz));
        }
        if (nearest > 16.0) return;
        if (!PortalSettings.refraction() && PortalSettings.warpStrength() <= 0f) return;
        copyScreen();
        copyReady = copy != null;
        float warp = PortalSettings.warpStrength();
        if (copyReady && warp > 0f && nearest < 3.0) {
            float strength = warp * (float) (1.0 - nearest / 3.0) * (0.55f + 0.45f * (float) Math.sin(System.nanoTime() * 3.0E-9));
            if (strength > 0.01f) drawWarp(sh, strength);
        }
        if (!PortalSettings.refraction()) copyReady = false;
    }

    private static void copyScreen() {
        Minecraft mc = Minecraft.getInstance();
        var main = mc.getMainRenderTarget();
        if (copy == null) {
            copy = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
            copy.setFilterMode(9729);                                  // GL_LINEAR
        } else if (copy.width != main.width || copy.height != main.height) {
            copy.resize(main.width, main.height, Minecraft.ON_OSX);
        }
        GlStateManager._glBindFramebuffer(36008, main.frameBufferId);      // GL_READ_FRAMEBUFFER
        GlStateManager._glBindFramebuffer(36009, copy.frameBufferId);      // GL_DRAW_FRAMEBUFFER
        GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, copy.width, copy.height, 16384, 9728);   // colour, nearest
        main.bindWrite(false);
    }

    private static void drawWarp(ShaderInstance sh, float strength) {
        RenderSystem.setShader(() -> sh);
        RenderSystem.setShaderTexture(0, VfxTextures.byName("portal_swirl"));
        RenderSystem.setShaderTexture(1, copy.getColorTextureId());
        sh.safeGetUniform("PortalMode").set(1.0f);
        sh.safeGetUniform("PortalRefract").set(0.0f);
        sh.safeGetUniform("PortalWarp").set(strength);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        var mvs = RenderSystem.getModelViewStack();
        mvs.pushMatrix(); mvs.identity(); RenderSystem.applyModelViewMatrix();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bb.addVertex(-1, -1, 0).setUv(0, 0).setColor(255, 255, 255, 255);
        bb.addVertex(1, -1, 0).setUv(1, 0).setColor(255, 255, 255, 255);
        bb.addVertex(1, 1, 0).setUv(1, 1).setColor(255, 255, 255, 255);
        bb.addVertex(-1, 1, 0).setUv(0, 1).setColor(255, 255, 255, 255);
        MeshData mesh = bb.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
        sh.safeGetUniform("PortalMode").set(0.0f);
        mvs.popMatrix(); RenderSystem.applyModelViewMatrix();
        RenderSystem.enableDepthTest();
    }

    // ------------------------------------------------------------------ the direct-draw sink
    /** Collects a gate's quads per pass (reused arrays, grown once) and draws them back to front. */
    private static final class GlSink implements PortalSink {
        private final float[][] data = new float[PortalPass.values().length][6 * 4 * 64];
        private final int[] size = new int[PortalPass.values().length];

        void clear() { java.util.Arrays.fill(size, 0); }

        @Override
        public void quad(PortalPass pass, float[] p, float u0, float u1, float vInner, float vOuter, int cInner, int cOuter) {
            int k = pass.ordinal();
            if (size[k] + 24 > data[k].length) data[k] = java.util.Arrays.copyOf(data[k], data[k].length * 2);
            float[] d = data[k];
            int o = size[k];
            o = put(d, o, p[0], p[1], p[2], u0, vInner, cInner);
            o = put(d, o, p[3], p[4], p[5], u1, vInner, cInner);
            o = put(d, o, p[6], p[7], p[8], u1, vOuter, cOuter);
            o = put(d, o, p[9], p[10], p[11], u0, vOuter, cOuter);
            size[k] = o;
        }

        private static int put(float[] d, int o, float x, float y, float z, float u, float v, int argb) {
            d[o] = x; d[o + 1] = y; d[o + 2] = z; d[o + 3] = u; d[o + 4] = v; d[o + 5] = Float.intBitsToFloat(argb);
            return o + 6;
        }

        void flush(Matrix4f modelView, ShaderInstance sh, boolean refract) {
            var mvs = RenderSystem.getModelViewStack();
            mvs.pushMatrix(); mvs.identity(); RenderSystem.applyModelViewMatrix();      // vertices are pre-transformed to view space
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();
            for (PortalPass pass : PortalPass.values()) {
                int n = size[pass.ordinal()];
                if (n == 0) continue;
                ResourceLocation tex = TEX[pass.ordinal()];
                if (tex == null) tex = TEX[pass.ordinal()] = VfxTextures.byName(pass.texture);
                Minecraft.getInstance().getTextureManager().getTexture(tex).setFilter(true, false);
                RenderSystem.setShaderTexture(0, tex);
                if (pass == PortalPass.HORIZON) {
                    RenderSystem.setShader(() -> sh);
                    RenderSystem.setShaderTexture(1, refract && copy != null ? copy.getColorTextureId() : Minecraft.getInstance().getTextureManager().getTexture(tex).getId());
                    sh.safeGetUniform("PortalMode").set(0.0f);
                    sh.safeGetUniform("PortalRefract").set(refract ? 1.0f : 0.0f);
                    sh.safeGetUniform("PortalWarp").set(0.0f);
                } else {
                    RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
                    if (pass.additive) RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                    else RenderSystem.defaultBlendFunc();
                }
                BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                float[] d = data[pass.ordinal()];
                for (int i = 0; i < n; i += 6) {
                    modelView.transformPosition(d[i], d[i + 1], d[i + 2], TMP);
                    int c = Float.floatToRawIntBits(d[i + 5]);
                    bb.addVertex(TMP.x, TMP.y, TMP.z).setUv(d[i + 3], d[i + 4]).setColor((c >> 16) & 255, (c >> 8) & 255, c & 255, c >>> 24);
                }
                MeshData mesh = bb.build();
                if (mesh != null) BufferUploader.drawWithShader(mesh);
            }
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            mvs.popMatrix(); RenderSystem.applyModelViewMatrix();
        }
    }
}
