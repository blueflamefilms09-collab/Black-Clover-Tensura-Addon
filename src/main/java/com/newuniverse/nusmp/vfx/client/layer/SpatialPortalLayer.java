package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.client.VfxInstance;
import com.newuniverse.nusmp.vfx.client.VfxRenderContext;
import com.newuniverse.nusmp.vfx.client.VfxVertexBuffer;
import com.newuniverse.nusmp.vfx.client.portal.PortalBufferSink;
import com.newuniverse.nusmp.vfx.client.portal.PortalComposer;
import com.newuniverse.nusmp.vfx.client.portal.PortalFrame;
import com.newuniverse.nusmp.vfx.client.portal.PortalMath;
import com.newuniverse.nusmp.vfx.client.portal.PortalMesh;
import com.newuniverse.nusmp.vfx.client.portal.PortalSettings;
import com.newuniverse.nusmp.vfx.client.portal.PortalSink;
import com.newuniverse.nusmp.vfx.client.portal.PortalSpec;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: this is the 3D animated dimensional gate for {@code VfxShape.SPACE_PORTAL} (Fallen Angel Gate, Door of Fate, the Unopening
 * Red Room, Zenon's and the wave boss's gates). It is not a layer of its own: {@link ArcaneSpellLayer} already owns the shape, so
 * it hands the three calls (spawn, tick, render) here when {@code spatial.portal3d} is on and keeps its old flat drawing for when it is
 * off, and no payload or registration changes. The recipe is in {@link PortalComposer}; this class holds what is per gate (position,
 * facing, link to the other gate of its pair) and chooses how the quads reach the screen:
 * <ul>
 *   <li>normally the {@link Client} hook (set by the game-side {@code PortalClient}) draws them directly with the portal shader, the
 *       screen-copy refraction, the Rukh particles and the sounds;</li>
 *   <li>without it (the headless preview, a shader that failed to load, or when the hook declines) they go through the shared
 *       effect buffer as plain translucent quads at the highest tier that fits its budget.</li>
 * </ul>
 * This file has no GL or world types so the preview can compile and run it.
 */
public final class SpatialPortalLayer {
    private SpatialPortalLayer() {}

    /** One gate as the client knows it. */
    public static final class State {
        public final VfxInstance inst;
        public final long seed;
        public final long order;
        /** Gate foot position (world), unit facing on the XZ plane (the side it shows its face to), size (the cast's power) and colour. */
        public final double x, y, z;
        public float fx, fz;
        public final float size;
        public final int rgb;
        /** The other gate of the pair (same payload seed), or null. */
        public State partner;
        public boolean faceFromCamera;
        /** Whether the game-side hook has already played the opening sound / the closing sound. */
        public boolean openedSound, closedSound;

        State(VfxInstance inst, long order) {
            this.inst = inst;
            this.seed = inst.seed;
            this.order = order;
            this.x = inst.payload.from().x; this.y = inst.payload.from().y; this.z = inst.payload.from().z;
            this.size = inst.power;
            this.rgb = inst.color;
            double dx = inst.payload.to().x - x, dz = inst.payload.to().z - z;
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len < 1.0E-4) { faceFromCamera = true; fx = 0; fz = 1; }
            else { fx = (float) (dx / len); fz = (float) (dz / len); }
        }

        /** Height of the opening's centre above the foot. */
        public float centreHeight() { return 1.3f * size; }
        public boolean alive() { return !inst.done(); }
    }

    /** What the game side adds on top: shader drawing, refraction, particles and sounds. Null in the preview. */
    public interface Client {
        void spawned(State s);
        void tick(State s);
        /** Draws the gate (and its thread) now; false = not drawn, the caller uses the buffer path. */
        boolean draw(VfxRenderContext ctx, State s, PortalFrame f, int tier);
        /** The last gate has closed: free anything held. */
        void idle();
    }

    public static volatile Client client;

    private static final Map<VfxInstance, State> STATES = new IdentityHashMap<>();
    private static final List<State> LIVE = new ArrayList<>();
    private static long counter;
    private static final PortalFrame FRAME = new PortalFrame();
    private static final PortalComposer COMPOSER = new PortalComposer();
    private static PortalBufferSink bufferSink;
    private static VfxVertexBuffer bufferSinkFor;

    /** Whether the 3D portal replaces the old flat one ({@code spatial.portal3d}). */
    public static boolean active() { return PortalSettings.portal3d(); }

    /** The live gates (read by the game-side hook for the warp pulse and the refraction copy). */
    public static List<State> live() { return LIVE; }

    public static PortalComposer composer() { return COMPOSER; }

    // ------------------------------------------------------------------ lifecycle
    public static void onSpawn(VfxInstance inst) {
        purge();
        State s = new State(inst, counter++);
        for (State o : LIVE) {
            if (o.partner == null && o.seed == s.seed && o.alive()) { o.partner = s; s.partner = o; break; }
        }
        STATES.put(inst, s);
        LIVE.add(s);
        Client c = client;
        if (c != null) c.spawned(s);
    }

    public static void onTick(VfxInstance inst) {
        State s = STATES.get(inst);
        if (s == null) return;
        Client c = client;
        if (c != null) c.tick(s);
        if (inst.age() + 1 >= inst.duration) {
            STATES.remove(inst);
            LIVE.remove(s);
            if (s.partner != null && s.partner.partner == s) s.partner.partner = null;
            if (LIVE.isEmpty() && c != null) c.idle();
        }
    }

    private static void purge() {
        for (int i = LIVE.size() - 1; i >= 0; i--) {
            State s = LIVE.get(i);
            if (s.inst.done()) {
                LIVE.remove(i);
                STATES.remove(s.inst);
                if (s.partner != null && s.partner.partner == s) s.partner.partner = null;
            }
        }
    }

    // ------------------------------------------------------------------ drawing
    public static void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        State s = STATES.get(inst);
        if (s == null) { onSpawn(inst); s = STATES.get(inst); }
        if (s.faceFromCamera) {                                   // no usable 'to': face the way the old gate did, across the camera's right
            float fx = -ctx.camRight.z, fz = ctx.camRight.x, l = (float) Math.sqrt(fx * fx + fz * fz);
            if (l > 1e-4f) { s.fx = fx / l; s.fz = fz / l; }
            s.faceFromCamera = false;
        }
        float age = inst.ageTicks(ctx.partialTick);
        fill(FRAME, s, ctx, age, inst.duration);
        float dist = (float) Math.sqrt(FRAME.cx * FRAME.cx + FRAME.cy * FRAME.cy + FRAME.cz * FRAME.cz);
        int tier = dist < 24f ? 0 : dist < 48f ? 1 : 2;
        if (ctx.detail < 1f) tier++;
        Client c = client;
        if (c != null && c.draw(ctx, s, FRAME, tier)) return;
        // buffer path: the highest tier whose worst case still fits what the effect has left
        PortalSpec spec = PortalSpec.tier(tier);
        while (spec.tier() < PortalSpec.TIERS.length - 1 && !buf.hasBudget(spec.maxVertices())) spec = PortalSpec.tier(spec.tier() + 1);
        FRAME.shader = false;
        PortalSink sink = sinkFor(buf);
        COMPOSER.compose(PortalMesh.of(spec), FRAME, sink);
        thread(ctx, s, FRAME, spec.threadSegments(), sink);
    }

    private static PortalSink sinkFor(VfxVertexBuffer buf) {
        if (bufferSink == null || bufferSinkFor != buf) { bufferSink = new PortalBufferSink(buf); bufferSinkFor = buf; }
        return bufferSink;
    }

    /** Fills the frame for a gate: placement, orientation, opening animation and the viewer's place in the gate's own axes. */
    public static void fill(PortalFrame f, State s, VfxRenderContext ctx, float age, float duration) {
        f.cx = (float) (s.x - ctx.camPos.x); f.cy = (float) (s.y - ctx.camPos.y) + s.centreHeight(); f.cz = (float) (s.z - ctx.camPos.z);
        f.nx = s.fx; f.ny = 0; f.nz = s.fz;                      // front normal
        f.rx = -s.fz; f.ry = 0; f.rz = s.fx;                     // right = face x up
        f.ux = 0; f.uy = 1; f.uz = 0;
        f.size = s.size;
        f.hw = 0.9f * s.size * PortalMath.openW(age, duration);
        f.hh = 1.35f * s.size * PortalMath.openH(age, duration);
        f.age = age; f.duration = duration;
        f.fade = PortalMath.fade(age, duration);
        f.argb = s.rgb | 0xFF000000;
        f.seed = s.seed;
        f.viewerFromCamera();
    }

    /**
     * The thin spatial thread between the two rims of a linked pair, drawn by the gate that opened first and only while the viewer
     * is within 24 blocks of either gate.
     */
    public static void thread(VfxRenderContext ctx, State s, PortalFrame f, int segments, PortalSink sink) {
        State o = s.partner;
        if (o == null || !o.alive() || s.order > o.order) return;
        float ax = f.cx, ay = f.cy, az = f.cz;
        float bx = (float) (o.x - ctx.camPos.x), by = (float) (o.y - ctx.camPos.y) + o.centreHeight(), bz = (float) (o.z - ctx.camPos.z);
        float da = (float) Math.sqrt(ax * ax + ay * ay + az * az), db = (float) Math.sqrt(bx * bx + by * by + bz * bz);
        float near = Math.min(da, db);
        if (near > 24f) return;
        float dx = bx - ax, dy = by - ay, dz = bz - az, len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 2f) return;
        float inA = 0.9f * s.size, inB = 0.9f * o.size;          // start and end on the rims, not in the middles
        float ex = dx / len, ey = dy / len, ez = dz / len;
        float age = o.inst.ageTicks(ctx.partialTick);
        float fadeB = PortalMath.fade(age, o.inst.duration);
        float alpha = Math.min(f.fade, fadeB) * (1f - near / 24f) * 0.8f;
        COMPOSER.thread(sink, f.argb, ax + ex * inA, ay + ey * inA, az + ez * inA, bx - ex * inB, by - ey * inB, bz - ez * inB,
                f.age, segments, 0.06f * Math.max(1f, s.size), alpha);
    }
}
