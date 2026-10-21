package com.newuniverse.nusmp.client.relics.djinn.tiny;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.relics.djinn.DjinnClientState;
import com.newuniverse.nusmp.client.relics.djinn.DjinnClientState.Snap;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import com.newuniverse.nusmp.relics.djinn.DjinnPhaseRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;

/**
 * The Tiny Djinn: during a Lesser Summon the Djinn's figure hovers at its owner's right shoulder, a real 3D chibi (tools/gen_tiny_djinn.py, one geo model per Djinn,
 * drawn by {@link GeoDraw} with the shared clips). It is not an entity: it is drawn from the synced Djinn state ({@link DjinnClientState}: the phase, which Djinn, the
 * time left, the Dark flag), for everyone in range, by a level-render hook after the particles ({@code RenderLevelStageEvent.AFTER_PARTICLES}, as the floating
 * grimoire is). The little state machine and the spring of each figure are {@link TinyDjinnActor} and {@link TinyDjinnMath} (pure, tested).
 *
 * <p>Cost: nothing is allocated per frame (the pose stack, the quaternion, the arrays and the mutable block position are reused; a figure is a few hundred vertices
 * through the geo drawer); a figure beyond 48 blocks is not drawn, beyond 24 it loses its additive glow pass, only the nearest {@code djinn.tinyMax} (default 16) are
 * drawn, and with none out the render hook returns at its first line.</p>
 */
public final class TinyDjinnLayer {
    private TinyDjinnLayer() {}

    private static final int CAPACITY = 128;

    /** One figure: its owner, its Djinn, its state machine and the numbers handed to it each frame. */
    private static final class Holder {
        final int owner;
        final Djinn djinn;
        final boolean dark;
        final TinyDjinnActor actor;
        final net.minecraft.core.particles.ParticleOptions light;
        final float[] anchor = new float[3], vessel = new float[3], foe = new float[3];
        int foeId = -1;
        boolean foeKnown;
        float leftFrac = 1f;
        double dist2;

        Holder(int owner, Djinn djinn, boolean dark) {
            this.owner = owner;
            this.djinn = djinn;
            this.dark = dark;
            this.actor = new TinyDjinnActor(((owner * 37 + djinn.ordinal() * 11) & 255) / 256f);
            this.light = TinyDjinnFx.dust(dark ? djinn.color() : djinn.glow(), 0.9f);
        }
    }

    private static final Map<Integer, Holder> BY_OWNER = new HashMap<>();
    private static final Holder[] LIST = new Holder[CAPACITY];
    private static int count;
    private static final double[] D2 = new double[CAPACITY];
    private static final boolean[] KEEP = new boolean[CAPACITY];

    private static final PoseStack POSE = new PoseStack();
    private static final Quaternionf TURN = new Quaternionf();
    private static final BlockPos.MutableBlockPos LIGHT_POS = new BlockPos.MutableBlockPos();
    private static final GeoSpec[][] SPECS = new GeoSpec[Djinn.values().length][2];
    private static final float[] NAME_HALF = new float[Djinn.values().length];
    private static long frame;

    public static void init() {
        NeoForge.EVENT_BUS.addListener(TinyDjinnLayer::onTick);
        NeoForge.EVENT_BUS.addListener(TinyDjinnLayer::onRender);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> clear());
    }

    public static void clear() {
        BY_OWNER.clear();
        for (int i = 0; i < count; i++) LIST[i] = null;
        count = 0;
    }

    /** True while the config shows the figure (the old bust is then not drawn). */
    public static boolean replacesBust() { return TinyDjinnConfig.enabled(); }

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", path); }

    private static GeoSpec spec(Djinn d, boolean dark) {
        GeoSpec s = SPECS[d.ordinal()][dark ? 1 : 0];
        if (s == null) {
            s = new GeoSpec(rl(TinyDjinnAssets.model(d)), rl(TinyDjinnAssets.ANIMATION), rl(TinyDjinnAssets.texture(d, dark)), rl(TinyDjinnAssets.glow(d, dark)));
            SPECS[d.ordinal()][dark ? 1 : 0] = s;
        }
        return s;
    }

    // ================================================================ the server's cue
    /** A cast at {@code foeId} by {@code ownerId}'s Lesser Summon (the {@code DjinnNet.Cue} packet): the figure points, then dashes. */
    public static void cue(int ownerId, int foeId) {
        Holder h = BY_OWNER.get(ownerId);
        Minecraft mc = Minecraft.getInstance();
        if (h == null || mc.level == null) return;
        if (h.actor.cue(mc.level.getGameTime(), foeId)) {
            h.foeId = foeId;
            h.foeKnown = false;
        }
    }

    // ================================================================ the tick: who has a figure, kill checks
    private static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            if (count > 0 || !BY_OWNER.isEmpty()) clear();
            return;
        }
        if (!TinyDjinnConfig.enabled()) {
            if (count > 0 || !BY_OWNER.isEmpty()) clear();
            return;
        }
        long gt = mc.level.getGameTime();
        boolean changed = false;
        for (AbstractClientPlayer p : mc.level.players()) {
            Snap s = DjinnClientState.of(p.getId());
            Holder h = BY_OWNER.get(p.getId());
            boolean want = s.phase() == DjinnPhaseRules.LESSER && s.djinn() != null && !p.isSpectator() && s.leftNow(gt) > 6;
            if (want) {
                if (h != null && (h.djinn != s.djinn() || h.dark != s.dark() || !h.actor.alive() || h.actor.leaving())) {
                    if (h.djinn != s.djinn() || h.dark != s.dark()) h.actor.vanish();
                    BY_OWNER.remove(p.getId());
                    h = null;
                    changed = true;
                }
                if (h == null && BY_OWNER.size() < CAPACITY) {
                    h = new Holder(p.getId(), s.djinn(), s.dark());
                    placeAnchor(h, p, 0f, false);
                    h.actor.spawn(gt, h.anchor, p.getYRot());
                    BY_OWNER.put(p.getId(), h);
                    changed = true;
                }
                if (h != null) h.leftFrac = Mth.clamp(s.leftNow(gt) / (float) DjinnPhaseRules.LESSER_TICKS, 0f, 1f);
            } else if (h != null && h.actor.alive() && !h.actor.leaving()) {
                h.actor.exit(gt);
            }
        }
        for (Holder h : BY_OWNER.values()) {
            if (h.actor.killCheckDue(gt)) {
                Entity foe = mc.level.getEntity(h.foeId);
                if (!(foe instanceof LivingEntity le) || !le.isAlive() || le.isDeadOrDying()) h.actor.markKill(gt);
            }
        }
        if (BY_OWNER.values().removeIf(h -> !h.actor.alive() || mc.level.getEntity(h.owner) == null)) changed = true;
        if (changed || count != BY_OWNER.size()) {
            for (int i = 0; i < count; i++) LIST[i] = null;
            count = 0;
            for (Holder h : BY_OWNER.values()) if (count < CAPACITY) LIST[count++] = h;
        }
    }

    /** Fills {@code h.anchor} and {@code h.vessel} from the owner at the interpolated position. */
    private static void placeAnchor(Holder h, Entity owner, float partial, boolean firstPerson) {
        double x = Mth.lerp(partial, owner.xo, owner.getX()), y = Mth.lerp(partial, owner.yo, owner.getY()), z = Mth.lerp(partial, owner.zo, owner.getZ());
        float yaw = firstPerson ? Mth.rotLerp(partial, owner.yRotO, owner.getYRot())
                : owner instanceof LivingEntity le ? Mth.rotLerp(partial, le.yBodyRotO, le.yBodyRot) : owner.getYRot();
        TinyDjinnMath.anchor(h.anchor, x, y, z, yaw, owner.getBbHeight(), owner.isCrouching(), firstPerson);
        h.vessel[0] = (float) x;
        h.vessel[1] = (float) (y + owner.getBbHeight() * 0.7);
        h.vessel[2] = (float) z;
    }

    // ================================================================ the frame
    private static void onRender(RenderLevelStageEvent event) {
        if (count == 0 || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long t0 = com.newuniverse.nusmp.perf.PerfProbe.begin();
        try {
            draw(event, mc);
        } finally {
            com.newuniverse.nusmp.perf.PerfProbe.end(PROBE, t0);
        }
    }

    private static final com.newuniverse.nusmp.perf.PerfProbe.Section PROBE = com.newuniverse.nusmp.perf.PerfProbe.section("c.djinn.tiny");

    private static void draw(RenderLevelStageEvent event, Minecraft mc) {
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float now = mc.level.getGameTime() + partial;
        Vec3 cam = event.getCamera().getPosition();
        boolean thirdOrFirst = mc.options.getCameraType().isFirstPerson();
        Entity camEntity = mc.getCameraEntity();
        frame++;
        int n = count;
        // 1. advance every figure (cheap), turn its events into particles, measure its distance
        for (int i = 0; i < n; i++) {
            Holder h = LIST[i];
            Entity owner = mc.level.getEntity(h.owner);
            if (owner == null) { D2[i] = Double.MAX_VALUE; continue; }
            boolean fp = thirdOrFirst && owner == camEntity;
            placeAnchor(h, owner, partial, fp);
            TinyDjinnActor a = h.actor;
            boolean fighting = a.state == TinyDjinnActor.State.CAST || a.state == TinyDjinnActor.State.ATTACK || a.state == TinyDjinnActor.State.HOP;
            if (fighting && h.foeId >= 0) {
                Entity foe = mc.level.getEntity(h.foeId);
                if (foe != null) {
                    h.foe[0] = (float) Mth.lerp(partial, foe.xo, foe.getX());
                    h.foe[1] = (float) (Mth.lerp(partial, foe.yo, foe.getY()) + foe.getBbHeight() * 0.55);
                    h.foe[2] = (float) Mth.lerp(partial, foe.zo, foe.getZ());
                    h.foeKnown = true;
                }
            }
            a.stiffness = fp ? 2.4f : 1f;
            // at rest it faces the camera (in first person that is its owner); a cast and an attack face the foe (the actor decides)
            float toCam = TinyDjinnMath.yawTo(cam.x - a.pos[0], cam.z - a.pos[2]);
            a.update(now, h.anchor, h.vessel, fighting && h.foeKnown ? h.foe : null, toCam);
            int ev = a.takeEvents();
            if (ev != 0) events(mc, h, a, ev);
            if ((a.state == TinyDjinnActor.State.EXIT) && (frame & 1) == 0) TinyDjinnFx.spiralMote(mc.level, a.pos[0], a.pos[1], a.pos[2], h.light);
            h.dist2 = cam.distanceToSqr(a.pos[0], a.pos[1], a.pos[2]);
            D2[i] = h.dist2;
        }
        // 2. only the nearest few are drawn
        int max = TinyDjinnConfig.max();
        if (max <= 0) return;
        if (n > max) TinyDjinnMath.cap(D2, n, max, KEEP);
        else for (int i = 0; i < n; i++) KEEP[i] = true;

        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.set(event.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        boolean names = TinyDjinnConfig.nameTag();
        boolean bars = TinyDjinnConfig.timerBar();
        for (int i = 0; i < n; i++) {
            if (!KEEP[i]) continue;
            Holder h = LIST[i];
            TinyDjinnActor a = h.actor;
            if (!a.alive() || !TinyDjinnMath.inRange(h.dist2)) continue;
            Entity owner = mc.level.getEntity(h.owner);
            if (owner == null || owner.isInvisible() || owner.isSpectator()) continue;
            boolean fp = thirdOrFirst && owner == camEntity;
            float alpha = a.alpha(now);
            if (alpha <= 0.02f) continue;
            drawFigure(mc, buffers, cam, h, a, now, alpha, fp);
            if (!fp && TinyDjinnMath.withName(h.dist2)) {
                if (names) drawName(mc, buffers, cam, h, a, alpha, bars && a.state != TinyDjinnActor.State.EXIT);
            }
        }
        buffers.endBatch();
        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    private static void drawFigure(Minecraft mc, MultiBufferSource.BufferSource buffers, Vec3 cam, Holder h, TinyDjinnActor a, float now, float alpha, boolean fp) {
        PoseStack ps = POSE;
        ps.pushPose();
        ps.translate(a.pos[0] - cam.x, a.pos[1] - cam.y, a.pos[2] - cam.z);
        TURN.identity().rotateY((float) Math.toRadians(-a.yaw));
        ps.mulPose(TURN);
        float s = TinyDjinnMath.modelScale(fp);
        ps.scale(s, s, s);
        int light = LevelRenderer.getLightColor(mc.level, LIGHT_POS.set(Mth.floor(a.pos[0]), Mth.floor(a.pos[1] + 0.2f), Mth.floor(a.pos[2])));
        int body = ((int) (255 * Math.min(1f, alpha)) << 24) | 0xFFFFFF;
        int glow = TinyDjinnMath.withGlow(h.dist2) ? ((int) (255 * Math.min(1f, a.glow(now) * alpha)) << 24) | 0xFFFFFF : 0;
        GeoDraw.paint(ps, buffers, spec(h.djinn, h.dark), a.clip(), a.clipSeconds(now), GeoDraw.Space.PROP, GeoDraw.Layer.TRANSLUCENT, light, body, glow);
        ps.popPose();
    }

    /** The floating name and (optionally) the thin bar of the seconds left under it, as a billboard. */
    private static void drawName(Minecraft mc, MultiBufferSource.BufferSource buffers, Vec3 cam, Holder h, TinyDjinnActor a, float alpha, boolean bar) {
        Font font = mc.font;
        String label = TinyDjinnAssets.label(h.djinn);
        int ord = h.djinn.ordinal();
        if (NAME_HALF[ord] == 0f) NAME_HALF[ord] = font.width(label) / 2f;
        PoseStack ps = POSE;
        ps.pushPose();
        ps.translate(a.pos[0] - cam.x, a.pos[1] - cam.y + 0.42f, a.pos[2] - cam.z);
        ps.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        ps.scale(0.0115f, -0.0115f, 0.0115f);
        Matrix4f m = ps.last().pose();
        int a8 = (int) (255 * Math.min(1f, alpha));
        int color = (a8 << 24) | brighten(h.dark ? h.djinn.glow() : h.djinn.color());
        font.drawInBatch(label, -NAME_HALF[ord], 0f, color, false, m, buffers, Font.DisplayMode.NORMAL, (int) (0.22f * a8) << 24, LightTexture.FULL_BRIGHT);
        if (bar) {
            VertexConsumer vc = buffers.getBuffer(RenderType.textBackground());
            float w = 22f, x0 = -w / 2f, y0 = 10.5f, y1 = 12.5f;
            quad(vc, m, x0 - 0.8f, y0 - 0.8f, x0 + w + 0.8f, y1 + 0.8f, 0.02f, ((int) (0.65f * a8) << 24) | 0x101018);
            quad(vc, m, x0, y0, x0 + w * h.leftFrac, y1, 0.0f, (a8 << 24) | (h.dark ? h.djinn.glow() : h.djinn.color()));
        }
        ps.popPose();
    }

    private static int brighten(int rgb) {
        int r = ((rgb >> 16) & 255), g = ((rgb >> 8) & 255), b = rgb & 255;
        r += (255 - r) * 0.4; g += (255 - g) * 0.4; b += (255 - b) * 0.4;
        return (r << 16) | (g << 8) | b;
    }

    /** A flat rectangle seen from both sides (the text background's render type culls back faces, and the billboard's side is not worth being wrong about). */
    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, float z, int argb) {
        int l = LightTexture.FULL_BRIGHT;
        vc.addVertex(m, x0, y0, z).setColor(argb).setLight(l);
        vc.addVertex(m, x1, y0, z).setColor(argb).setLight(l);
        vc.addVertex(m, x1, y1, z).setColor(argb).setLight(l);
        vc.addVertex(m, x0, y1, z).setColor(argb).setLight(l);
        vc.addVertex(m, x0, y1, z).setColor(argb).setLight(l);
        vc.addVertex(m, x1, y1, z).setColor(argb).setLight(l);
        vc.addVertex(m, x1, y0, z).setColor(argb).setLight(l);
        vc.addVertex(m, x0, y0, z).setColor(argb).setLight(l);
    }

    private static void events(Minecraft mc, Holder h, TinyDjinnActor a, int ev) {
        var level = mc.level;
        if ((ev & TinyDjinnActor.EV_POOF_IN) != 0) TinyDjinnFx.poofIn(level, a.pos[0], a.pos[1], a.pos[2], h.light);
        if ((ev & TinyDjinnActor.EV_CAST) != 0) {
            double yaw = Math.toRadians(a.yaw);
            double fx = -Math.sin(yaw), fz = Math.cos(yaw);
            TinyDjinnFx.cast(level, a.pos[0] + fx * 0.2, a.pos[1] + 0.02, a.pos[2] + fz * 0.2, fx, fz, h.light);
        }
        if ((ev & TinyDjinnActor.EV_STRIKE) != 0) {
            if (h.foeKnown) TinyDjinnFx.strike(level, h.foe[0], h.foe[1], h.foe[2], h.light);
            else TinyDjinnFx.strike(level, a.pos[0], a.pos[1], a.pos[2], h.light);
        }
        if ((ev & TinyDjinnActor.EV_HOP) != 0) TinyDjinnFx.hop(level, a.pos[0], a.pos[1] + 0.1, a.pos[2], h.light);
        if ((ev & TinyDjinnActor.EV_POOF_OUT) != 0) TinyDjinnFx.poofOut(level, a.pos[0], a.pos[1], a.pos[2], h.light);
    }
}
