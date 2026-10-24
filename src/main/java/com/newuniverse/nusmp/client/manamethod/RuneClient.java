package com.newuniverse.nusmp.client.manamethod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.core.magic.grimoire.GrimoireCarry;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.mana.method.RuneElement;
import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil;
import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil.Frame;
import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil.Params;
import com.newuniverse.nusmp.mana.method.RuneGlyphLayout;
import com.newuniverse.nusmp.mana.method.RuneGlyphLayout.Layout;
import com.newuniverse.nusmp.mana.method.RuneNet;
import com.newuniverse.nusmp.mana.method.RuneSpec;
import com.newuniverse.nusmp.particle.fx.FxCatalog;
import com.newuniverse.nusmp.particle.fx.FxConfig;
import com.newuniverse.nusmp.particle.fx.FxMath;
import com.newuniverse.nusmp.particle.fx.FxTypes;
import com.newuniverse.nusmp.particle.fx.TintedParticleOptions;
import com.newuniverse.nusmp.particle.fx.TintedParticleType;
import com.newuniverse.nusmp.perf.PerfBudgets;
import com.newuniverse.nusmp.perf.PerfSettings;
import com.newuniverse.nusmp.perf.VfxQuality;
import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.VfxManager;
import com.newuniverse.nusmp.vfx.client.layer.RuneWritingLayer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * The client-only holder of the Mana Method rune. The picture of the rune is the vfx layer {@link RuneWritingLayer} (runes written in the air by a pen, strip quads through the VFX vertex buffer,
 * so the effect quality, the distance levels and the frame vertex cap apply); this class keeps the tiny state of the runes near the camera (at most {@link RuneGeneratorUtil#MAX_RUNES_DRAWN}) and
 * adds the two things the layer cannot:
 * <ul>
 *   <li><b>the real grimoire</b>: one open, glowing grimoire per rune, the mod's own book model (the item renderer, as the summoned book of {@code GrimoireFloatClient}: the open V, the glowing pages
 *       toward its owner), hovering at the caster's side while the rune is written; only within {@value RuneGeneratorUtil#BOOK_DISTANCE} blocks of the camera, only at full detail;</li>
 *   <li><b>sparks</b>: while a pen writes, at most {@link RuneGlyphLayout#MAX_SPARKS_PER_TICK} mana motes per rune per tick fly off its tip, each batch asking the client particle budget
 *       ({@code PerfBudgets.clientParticles}), thinned by the effect quality and the distance; none beyond 40 blocks.</li>
 * </ul>
 * Cost: a tick handler that returns at once with no rune; per rune per tick, while it is being written, one layout fill into a preallocated array (the same maths the layer runs) and at most three particles;
 * per frame, one item render per rune near the camera. Nothing is allocated per frame or per tick but what the particle engine takes. The old dot-ring particles of the first version are gone.
 */
public final class RuneClient {
    private RuneClient() {}

    private static final class CRune {
        RuneSpec spec;
        Params params;
        Frame frame;
        long born;
        int charge, lifetime;
        boolean own;
        double yawDeg;
        ItemStack book;
        final Layout scratch = new Layout();
        ParticleOptions spark;
        final RandomSource random = RandomSource.create();
    }

    private static final List<CRune> RUNES = new ArrayList<>();
    private static final double[] BOOK_AT = new double[3];
    private static final PoseStack POSE = new PoseStack();                              // one stack, reused (push and pop in pairs)
    /** The open grimoire of each element, built once (a render-only copy at the full V). */
    private static final ItemStack[] BOOKS = new ItemStack[RuneElement.values().length];
    private static final double SPARK_DISTANCE = 40.0;

    public static void init() {
        NeoForge.EVENT_BUS.addListener(RuneClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(RuneClient::onRender);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> RUNES.clear());
    }

    /** A rune packet arrived: validate it, start the writing effect and keep the small state the book and the sparks need. */
    public static void onRune(RuneNet.RunePayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !RuneNet.valid(p)) return;
        long now = mc.level.getGameTime();
        for (int i = RUNES.size() - 1; i >= 0; i--) if (now - RUNES.get(i).born > RUNES.get(i).lifetime) RUNES.remove(i);
        if (RUNES.size() >= RuneGeneratorUtil.MAX_RUNES_DRAWN) return;                      // at most six at once
        RuneSpec spec = RuneSpec.unpack(p.spec());
        CRune r = new CRune();
        r.spec = spec;
        r.params = RuneGeneratorUtil.paramsFor(spec);
        r.frame = RuneGeneratorUtil.frameFacing(p.x(), p.y(), p.z(), p.lx(), p.ly(), p.lz());
        r.born = now;
        r.charge = p.charge();
        r.lifetime = p.charge() + RuneWritingLayer.TAIL;
        r.own = mc.player != null && mc.player.getId() == p.ownerId();
        r.yawDeg = Math.toDegrees(Math.atan2(-p.lx(), p.lz()));
        r.book = book(spec.element());
        r.spark = options(RuneElement.RING_PARTICLE, spec.element().tint, 0.9f);
        RUNES.add(r);
        Vec3 from = new Vec3(p.x(), p.y(), p.z());
        VfxManager.get().spawn(new VfxPayload(VfxShape.RUNE_WRITING.ordinal(), from, from.add(p.lx(), p.ly(), p.lz()), 0xFF000000 | spec.element().tint, r.lifetime, 1.0f, -1, spec.pack()));
    }

    private static ItemStack book(RuneElement e) {
        ItemStack s = BOOKS[e.ordinal()];
        if (s == null) {
            MagicType magic = switch (e) {
                case FIRE -> MagicType.FLAME;
                case WATER -> MagicType.WATER;
                case WIND -> MagicType.WIND;
                case EARTH -> MagicType.EARTH;
                case LIGHTNING -> MagicType.LIGHTNING;
                case PLANT -> MagicType.PLANT;
            };
            s = GrimoireItem.withOpenView(GrimoireItem.createUnbound(5, magic, null), GrimoireCarry.OPEN_STEPS);
            BOOKS[e.ordinal()] = s;
        }
        return s;
    }

    /** The mana mote of the catalog in the rune's colour; plain dust of that colour when the custom particles are off or not bound yet. */
    private static ParticleOptions options(String id, int rgb, float scale) {
        if (FxConfig.clientCustom()) {
            TintedParticleType type = FxTypes.type(id);
            FxCatalog.Def d = FxCatalog.byId(id);
            if (type != null && d != null) return new TintedParticleOptions(type, d.tint() ? (rgb & 0xFFFFFF) : -1, scale, false, d.tint());
        }
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), 0.8f * scale);
    }

    // ---------------------------------------------------------------- sparks off the pens
    private static void onClientTick(ClientTickEvent.Post e) {
        if (RUNES.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) { RUNES.clear(); return; }
        SafeCast.client("mana-method-rune", () -> tick(mc, level));
    }

    private static void tick(Minecraft mc, Level level) {
        long now = level.getGameTime();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        VfxQuality q = PerfSettings.vfxQuality();
        for (int k = RUNES.size() - 1; k >= 0; k--) {
            CRune r = RUNES.get(k);
            long age = now - r.born;
            if (age < 0 || age > r.lifetime) { RUNES.remove(k); continue; }
            if (age > r.charge) continue;                                                       // only the pen throws sparks
            double dx = r.frame.cx() - cam.x, dy = r.frame.cy() - cam.y, dz = r.frame.cz() - cam.z, d2 = dx * dx + dy * dy + dz * dz;
            if (!r.own && d2 > SPARK_DISTANCE * SPARK_DISTANCE) continue;
            float detail = Math.min(q.detail, RuneGeneratorUtil.distanceDetailSq(d2, r.own));
            if (!(detail > 0.45f)) continue;                                                    // the lower levels draw no sparks
            RuneGlyphLayout.fill(r.scratch, r.spec, r.params, r.frame, age, r.charge, RuneGlyphLayout.LEVEL_FULL, cam.x, cam.y, cam.z);
            int want = Math.min(RuneGlyphLayout.MAX_SPARKS_PER_TICK, r.scratch.pens);
            int n = PerfBudgets.clientParticles(now, want);
            for (int i = 0; i < n; i++) {
                double x = r.scratch.penPos[i * 3], y = r.scratch.penPos[i * 3 + 1], z = r.scratch.penPos[i * 3 + 2];
                level.addParticle(r.spark, x, y, z, FxMath.clampSpeed((r.random.nextDouble() - 0.5) * 0.06), FxMath.clampSpeed(0.02 + r.random.nextDouble() * 0.04), FxMath.clampSpeed((r.random.nextDouble() - 0.5) * 0.06));
            }
        }
    }

    // ---------------------------------------------------------------- the real grimoire
    private static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || RUNES.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        SafeCast.client("mana-method-book", () -> renderBooks(mc, event));
    }

    private static void renderBooks(Minecraft mc, RenderLevelStageEvent event) {
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        long now = mc.level.getGameTime();
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        int drawn = 0;
        boolean started = false;
        MultiBufferSource.BufferSource buffers = null;
        Matrix4fStack modelView = null;
        for (CRune r : RUNES) {
            double age = now - r.born + partial;
            if (age < r.charge * 0.08 || age > r.lifetime - 2) continue;
            double dx = r.frame.cx() - cam.x, dy = r.frame.cy() - cam.y, dz = r.frame.cz() - cam.z, d2 = dx * dx + dy * dy + dz * dz;
            if (d2 > RuneGeneratorUtil.BOOK_DISTANCE * RuneGeneratorUtil.BOOK_DISTANCE) continue;       // the book is drawn within 32 blocks, the caster's own included
            if (!started) {
                started = true;
                modelView = RenderSystem.getModelViewStack();
                modelView.pushMatrix();
                modelView.set(event.getModelViewMatrix());
                RenderSystem.applyModelViewMatrix();
                buffers = mc.renderBuffers().bufferSource();
            }
            RuneGlyphLayout.bookPos(r.frame, r.params.radius(), age, BOOK_AT);
            float in = (float) Math.min(1.0, (age - r.charge * 0.08) / 3.0), out = (float) Math.min(1.0, (r.lifetime - 2 - age) / 3.0);
            float s = GrimoireCarry.HAND.scale() * 0.85f * Mth.clamp(Math.min(in, out), 0f, 1f);
            if (s <= 0.01f) continue;
            POSE.pushPose();
            POSE.translate(BOOK_AT[0] - cam.x, BOOK_AT[1] - cam.y, BOOK_AT[2] - cam.z);
            POSE.mulPose(Axis.YP.rotationDegrees((float) -r.yawDeg + GrimoireCarry.HAND.yaw() + Mth.sin((float) age * 0.05f) * 4f));     // the pages glow toward the caster, as a summoned book's do
            POSE.mulPose(Axis.XP.rotationDegrees(GrimoireCarry.HAND.pitch()));
            POSE.scale(s, s, s);
            mc.getItemRenderer().renderStatic(r.book, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, POSE, buffers, mc.level, 0);
            POSE.popPose();
            if (++drawn >= RuneGeneratorUtil.MAX_RUNES_DRAWN) break;
        }
        if (started) {
            buffers.endBatch();
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }
}
