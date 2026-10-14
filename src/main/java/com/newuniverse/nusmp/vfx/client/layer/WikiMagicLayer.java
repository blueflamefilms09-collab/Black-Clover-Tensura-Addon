package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.WikiBooks;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.WikiStyles;
import com.newuniverse.nusmp.vfx.WikiStyles.Group;
import com.newuniverse.nusmp.vfx.client.AbstractVfxLayer;
import com.newuniverse.nusmp.vfx.client.VfxBlend;
import com.newuniverse.nusmp.vfx.client.VfxInstance;
import com.newuniverse.nusmp.vfx.client.VfxPose;
import com.newuniverse.nusmp.vfx.client.VfxRenderContext;
import com.newuniverse.nusmp.vfx.client.VfxTextures;
import com.newuniverse.nusmp.vfx.client.VfxVertexBuffer;
import com.newuniverse.nusmp.vfx.client.WikiTextures;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * The layered effects of the 30 wiki magics (WIKI_MAGIC_CAST / FIELD / BURST). Full documentation: docs/wiki_magic_vfx.md.
 *
 * <p>Every magic belongs to one of seven visual groups ({@link WikiStyles}); the group chooses the texture, the blend AND the shape
 * language, and the magic's own place inside the group varies the details (counts, sweeps, drift, its accent sprite):
 * <pre>
 *   MOTES     Air, Snow, Smoke                     alpha motes that drift (hang, fall or billow)
 *   TRAILS    Hair, Poison Plant, Vine, Wing       crescent slashes and a streak, additive
 *   CIRCLES   Memory ... Tree                      rotating rune rings with tick marks, additive
 *   SHARDS    Mineral ... Stone (+ Sandstone)      radial crystal spikes, additive
 *   FLUID     Mucus, Mud, Tongue                   droplets on arcs and splash crowns, alpha
 *   SPIRAL    Skin, Song, Vortex                   helices of motes round the path, additive
 *   INFERNAL  Soul Corpse                          a dark alpha circle with red embers
 * </pre>
 * The colour of the payload names the magic (WikiBooks' palette, read once into {@link #PALETTE}); an unknown colour draws as Air, as
 * before. Textures come from {@link WikiTextures}: a missing file draws as the additive glow, and nothing in render() can throw (a
 * failure is logged once and the effect skips its frame). The per-variant tables below are indexed by the magic's place in its group
 * (WikiStyles table order, pinned by WikiStylesTest).
 */
public final class WikiMagicLayer extends AbstractVfxLayer {
    private static final Set<VfxShape> SHAPES = EnumSet.of(
            VfxShape.WIKI_MAGIC_CAST, VfxShape.WIKI_MAGIC_FIELD, VfxShape.WIKI_MAGIC_BURST);

    @Override public Set<VfxShape> shapes() { return SHAPES; }
    @Override public int defaultDuration(VfxShape shape) { return shape == VfxShape.WIKI_MAGIC_FIELD ? 42 : 22; }
    @Override public int defaultColor(VfxShape shape) { return 0xFF8AD8E8; }

    // ------------------------------------------------------------------ colour -> style (no allocation, null-safe)
    /** RGB palette of the 30 magics in style order (-1 = unavailable). */
    private static final int[] PALETTE = buildPalette();

    private static int[] buildPalette() {
        int[] p = new int[WikiStyles.COUNT];
        Arrays.fill(p, -1);
        for (int i = 0; i < p.length; i++) {
            try {
                p[i] = WikiBooks.color(MagicType.valueOf(WikiStyles.name(i))) & 0xFFFFFF;
            } catch (RuntimeException ignored) {
                // this magic keeps -1 and is never matched
            }
        }
        return p;
    }

    /** The RGB of a magic's palette (what the payload colour carries); a pale cyan when the style is not one of the 30. */
    public static int rgbOf(int style) { return style >= 0 && style < PALETTE.length && PALETTE[style] >= 0 ? PALETTE[style] : 0x9FE8FF; }

    /** The same, lifted so it shows when added (dark palettes such as Shakudo): what the armour passes tint with. */
    public static int glowRgbOf(int style) { return WikiStyles.lift(rgbOf(style)); }

    /** The style index of a payload colour; 0 (Air) for a colour that is not a wiki palette, as the first layer did. */
    public static int styleForColor(int color) {
        int rgb = color & 0xFFFFFF;
        for (int i = 0; i < PALETTE.length; i++) if (PALETTE[i] == rgb) return i;
        return 0;
    }

    // ------------------------------------------------------------------ per-variant tables (indexed by the magic's place in its group)
    /** MOTES (Air, Smoke, Snow): vertical drift. */
    private static final float[] RISE = {0f, 0.45f, -0.35f};
    /** TRAILS (Hair, Poison Plant, Vine, Wing): crescents per cast and their sweep in radians. */
    private static final int[] CRESCENTS = {3, 2, 2, 2};
    private static final float[] SWEEP = {1.1f, 1.4f, 1.7f, 2.1f};
    /** SPIRAL (Skin, Song, Vortex): strands and helix radius. */
    private static final int[] STRANDS = {2, 2, 3};
    private static final float[] HELIX = {0.30f, 0.38f, 0.48f};

    private static final int EMBER = 0xFFFF5232, SKULL = 0xFFE86A58;

    // ------------------------------------------------------------------ one frame's state (render thread only: reused, never allocated)
    private static final class Look {
        VfxRenderContext ctx;
        VfxVertexBuffer buf;
        int style, variant;
        Group group;
        ResourceLocation bulk, accent;
        VfxBlend bulkBlend, accentBlend;
        float age, dur, progress, fade, size;
        int tint, pale;
        final Vector3f from = new Vector3f(), to = new Vector3f(), look = new Vector3f();
    }

    private static final Look L = new Look();
    private static final Vector3f TIP = new Vector3f(), TAIL = new Vector3f(), DIR = new Vector3f(), SIDE = new Vector3f(), UPV = new Vector3f(),
            P = new Vector3f(), Q = new Vector3f(), C = new Vector3f(),
            R0 = new Vector3f(), R1 = new Vector3f(), R2 = new Vector3f(), R3 = new Vector3f(), R4 = new Vector3f(), R5 = new Vector3f();
    private static boolean warned;

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        try {
            Look l = L;
            l.ctx = ctx;
            l.buf = buf;
            l.age = inst.ageTicks(ctx.partialTick);
            l.dur = inst.duration;
            l.progress = Mth.clamp(l.age / inst.duration, 0, 1);
            l.fade = 1f - l.progress;
            l.size = Mth.clamp(Math.abs(inst.power), 0.45f, 5f);
            l.from.set(ctx.rel(inst.from(ctx)));
            l.to.set(ctx.rel(inst.to(ctx)));
            l.look.set(ctx.camera.getLookVector());
            l.style = styleForColor(inst.color);
            l.group = WikiStyles.group(l.style);
            int base = l.group.additive ? (inst.color & 0xFF000000) | WikiStyles.lift(inst.color) : inst.color;       // a dark palette still shows when added
            l.tint = VfxVertexBuffer.withAlpha(base, l.fade);
            l.pale = VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(base, 0.68f), l.fade);
            l.variant = WikiStyles.variant(l.style);
            l.bulk = WikiTextures.bulk(l.group);
            l.bulkBlend = WikiTextures.bulkBlend(l.group);
            l.accent = WikiTextures.accent(l.style);
            l.accentBlend = WikiTextures.accentBlend(l.style);
            switch (inst.shape) {
                case WIKI_MAGIC_CAST -> cast(l);
                case WIKI_MAGIC_FIELD -> field(l);
                case WIKI_MAGIC_BURST -> burst(l);
                default -> { }
            }
        } catch (RuntimeException e) {
            if (!warned) {
                warned = true;
                com.mojang.logging.LogUtils.getLogger().warn("[nusmp] WikiMagicLayer skipped a frame", e);
            }
        }
    }

    // ------------------------------------------------------------------ helpers
    private static float pick(float[] t, int v) { return t[Math.floorMod(v, t.length)]; }
    private static int pick(int[] t, int v) { return t[Math.floorMod(v, t.length)]; }
    private static float frac(float x) { return x - (float) Math.floor(x); }

    /** The colour with its RGB pulled toward 'rgb' by k, alpha kept. */
    private static int shade(int argb, int rgb, float k) {
        return (argb & 0xFF000000) | (VfxVertexBuffer.lerpColor(argb, 0xFF000000 | rgb, k) & 0xFFFFFF);
    }

    private static void bill(Look l, ResourceLocation tex, VfxBlend blend, Vector3f at, float size, float rot, int argb) {
        l.buf.billboard(l.ctx, tex, blend, at, size, rot, argb);
    }

    /** Point on a pose plane, written to 'out' without allocating. */
    private static Vector3f place(VfxPose p, float x, float y, Vector3f out) {
        return out.set(p.origin()).add(p.right().x * x + p.up().x * y, p.right().y * x + p.up().y * y, p.right().z * x + p.up().z * y);
    }

    /** A camera-facing ribbon from a to b with the texture's U running along it (slash streaks, the tongue). */
    private static void streak(Look l, ResourceLocation tex, VfxBlend blend, Vector3f a, Vector3f b, float width, int argb) {
        R0.set(b).sub(a);
        R1.set(R0).cross(l.look);
        float len = R1.length();
        if (len < 1e-5f) R1.set(l.ctx.camRight); else R1.div(len);
        R1.mul(width * 0.5f);
        R2.set(a).sub(R1);
        R3.set(b).sub(R1);
        R4.set(b).add(R1);
        R5.set(a).add(R1);
        l.buf.quad(tex, blend, R2, R3, R4, R5, 0f, 0f, 1f, 1f, argb, argb);          // the texture itself is hot at the head and faint at the tail
    }

    /** One crescent on the camera plane whose apex sits on 'at' and bulges toward screen angle theta. */
    private static void crescent(Look l, Vector3f at, float theta, float radius, float width, float sweep, float lateral, int argb) {
        float ct = Mth.cos(theta), st = Mth.sin(theta);
        C.set(at).fma(-ct * radius - st * lateral, l.ctx.camRight).fma(-st * radius + ct * lateral, l.ctx.camUp);
        VfxPose pose = new VfxPose(C, l.ctx.camRight, l.ctx.camUp, l.look);
        l.buf.arc(l.bulk, l.bulkBlend, pose, radius, width, theta - sweep * 0.5f, sweep, l.ctx.seg(18, 8), argb);
    }

    // ================================================================== CAST: a bolt that travels over most of its life, with a short tail
    private static void cast(Look l) {
        VfxRenderContext ctx = l.ctx;
        float k = Mth.clamp(l.age / Math.max(4f, l.dur * 0.8f), 0, 1);
        TIP.set(l.from).lerp(l.to, k);
        float length = TIP.distance(l.from);
        TAIL.set(l.from);
        if (length > 1e-3f) TAIL.set(TIP).lerp(l.from, Math.min(1f, (3.2f * l.size + 0.8f) / length));
        DIR.set(TIP).sub(TAIL);
        float dl = DIR.length();
        if (dl < 1e-4f) DIR.set(l.look); else DIR.div(dl);
        SIDE.set(DIR).cross(l.look);
        float sl = SIDE.length();
        if (sl < 1e-4f) SIDE.set(ctx.camRight); else SIDE.div(sl);
        UPV.set(DIR).cross(SIDE);
        switch (l.group) {
            case TRAILS -> castTrails(l);
            case CIRCLES -> castCircles(l);
            case SHARDS -> castShards(l);
            case FLUID -> castFluid(l);
            case SPIRAL -> castSpiral(l);
            case INFERNAL -> castInfernal(l);
            default -> castMotes(l);
        }
    }

    /** Drifting motes: a loose cloud strung along the path that wanders sideways and rises, hangs or falls by magic. */
    private static void castMotes(Look l) {
        int v = l.variant, n = l.ctx.seg(9 + 2 * v, 5);
        float rise = pick(RISE, v);
        int body = v == 1 ? shade(l.pale, 0x4A4654, 0.45f) : l.pale;                          // smoke is dusky
        for (int i = 0; i < n; i++) {
            float t = (i + 0.5f) / n;
            float ph = l.age * 0.16f + i * 1.9f + v * 0.8f, amp = l.size * (0.18f + 0.3f * (1f - t));
            P.set(TAIL).lerp(TIP, t).fma(Mth.sin(ph) * amp, SIDE)
                    .fma(Mth.cos(ph * 0.7f) * amp * 0.6f + rise * (1f - t) * l.size * 0.55f, l.ctx.camUp);
            bill(l, l.bulk, l.bulkBlend, P, l.size * (0.22f + 0.28f * (1f - t)) * (v == 1 ? 1.3f : 1f), ph * 0.3f,
                    VfxVertexBuffer.withAlpha(body, 0.35f + 0.65f * t));
        }
        bill(l, l.accent, l.accentBlend, TIP, l.size * 0.7f, l.age * 0.1f * (v % 2 == 0 ? 1 : -1), l.tint);
    }

    /** Slashing trails: crescents bulging along the flight, a streak behind, accent sprites (thorn, leaf, feather) shed along the path. */
    private static void castTrails(Look l) {
        int v = l.variant, crescents = pick(CRESCENTS, v);
        float theta = (float) Math.atan2(DIR.dot(l.ctx.camUp), DIR.dot(l.ctx.camRight)), sweep = pick(SWEEP, v);
        float radius = l.size * 1.1f;
        for (int c = 0; c < crescents; c++) {
            float lateral = (c - (crescents - 1) * 0.5f) * l.size * 0.38f * (v == 2 ? Mth.sin(l.age * 0.3f + c) : 1f);
            crescent(l, TIP, theta + (v == 3 ? (c % 2 == 0 ? 0.35f : -0.35f) : 0f), radius * (1f - 0.2f * c), l.size * (0.2f + 0.04f * c), sweep, lateral,
                    VfxVertexBuffer.withAlpha(l.tint, 1f - 0.22f * c));
        }
        streak(l, l.bulk, l.bulkBlend, TAIL, TIP, l.size * 0.5f, l.tint);
        int shed = l.ctx.seg(3, 2);
        for (int i = 0; i < shed; i++) {
            float t = (i + 1f) / (shed + 1f);
            P.set(TAIL).lerp(TIP, t).fma((i % 2 == 0 ? 1 : -1) * l.size * 0.3f, SIDE);
            bill(l, l.accent, l.accentBlend, P, l.size * 0.38f, theta + i * 0.9f, VfxVertexBuffer.withAlpha(l.pale, t));
        }
    }

    /** Circular patterns: the rune ring stands across the flight like a launch gate, with two fading gates behind it. */
    private static void castCircles(Look l) {
        int v = l.variant;
        float half = l.size * (0.75f + 0.04f * v);
        VfxPose gate = VfxPose.facing(TIP, DIR);
        l.buf.plane(l.bulk, l.bulkBlend, gate.spin(l.age * 0.12f * (v % 2 == 0 ? 1 : -1)), half, l.tint);
        l.buf.plane(l.accent, l.accentBlend, gate.lift(0.18f * l.size).spin(-l.age * 0.18f + v), half * 0.62f, l.pale);
        for (int g = 1; g <= 2; g++) {
            P.set(TAIL).lerp(TIP, 1f - 0.3f * g);
            l.buf.plane(l.bulk, l.bulkBlend, VfxPose.facing(P, DIR).spin(l.age * 0.1f + g), half * (1f - 0.26f * g), VfxVertexBuffer.withAlpha(l.tint, 1f - 0.36f * g));
        }
    }

    /** Crystalline shards: a spear of light with a starburst head and shards flung out round the path, pointing away from it. */
    private static void castShards(Look l) {
        int v = l.variant;
        l.buf.beam(l.ctx, VfxTextures.SHARD, VfxBlend.ADD, TAIL, TIP, (0.3f + 0.04f * (v % 5)) * l.size, 0.05f, 4, 0f, l.tint, l.pale);
        bill(l, l.bulk, l.bulkBlend, TIP, l.size * (0.9f + 0.05f * (v % 4)), l.age * 0.08f * (v % 2 == 0 ? 1 : -1), l.pale);
        int n = l.ctx.seg(5 + v % 4, 3);
        for (int i = 0; i < n; i++) {
            float t = (i + 0.5f) / n, phi = i * 2.399f + v, cx = Mth.cos(phi), cy = Mth.sin(phi), off = l.size * (0.22f + 0.2f * (1f - t));
            P.set(TAIL).lerp(TIP, t).fma(cx * off, SIDE).fma(cy * off, UPV);
            Q.set(SIDE).mul(cx).fma(cy, UPV);
            float screen = (float) Math.atan2(Q.dot(l.ctx.camUp), Q.dot(l.ctx.camRight));
            bill(l, l.accent, l.accentBlend, P, l.size * (0.34f + 0.12f * (i % 3)), screen - Mth.HALF_PI, VfxVertexBuffer.withAlpha(l.pale, 0.5f + 0.5f * t));
        }
    }

    /** Wet and fluid: droplets that sag the further back they are, a splash at the head; mud is heavier, the tongue is a curved ribbon. */
    private static void castFluid(Look l) {
        int v = l.variant, n = l.ctx.seg(8 + v, 5);
        float heavy = v == 1 ? 1.4f : 1f;
        int body = v == 1 ? shade(l.pale, 0x4A2E1E, 0.4f) : l.pale;
        if (v == 2) {                                                                          // tongue: three ribbon pieces on a sine path
            Q.set(TAIL);
            for (int i = 1; i <= 3; i++) {
                float t = i / 3f;
                P.set(TAIL).lerp(TIP, t).fma(Mth.sin(t * 5f + l.age * 0.25f) * l.size * 0.25f * (1f - t * 0.4f), l.ctx.camUp);
                streak(l, l.accent, l.accentBlend, Q, P, l.size * 0.55f, l.tint);
                Q.set(P);
            }
        }
        for (int i = 0; i < n; i++) {
            float t = (i + 0.5f) / n, sag = (1f - t) * (1f - t) * l.size * 0.9f;
            P.set(TAIL).lerp(TIP, t).fma(Mth.sin(i * 2.1f + l.age * 0.2f) * l.size * 0.14f, SIDE);
            P.y -= sag;
            bill(l, l.bulk, l.bulkBlend, P, l.size * (0.14f + 0.18f * t) * heavy, 0f, VfxVertexBuffer.withAlpha(body, 0.4f + 0.6f * t));
        }
        bill(l, l.accent, l.accentBlend, TIP, l.size * 0.8f, 0f, l.tint);
    }

    /** Flowing spirals: strands of motes winding round the path (a cone for the vortex), the accent sprite spinning at the head. */
    private static void castSpiral(Look l) {
        int v = l.variant, strands = pick(STRANDS, v), n = l.ctx.seg(8, 4);
        float radius = l.size * pick(HELIX, v);
        for (int s = 0; s < strands; s++) {
            for (int i = 0; i < n; i++) {
                float t = (i + 0.5f) / n;
                float phi = t * Mth.TWO_PI * 1.6f + l.age * 0.32f + s * Mth.TWO_PI / strands;
                float r = radius * (v == 2 ? 1.1f - 0.7f * t : 0.65f + 0.35f * Mth.sin(t * Mth.PI));
                P.set(TAIL).lerp(TIP, t).fma(Mth.cos(phi) * r, SIDE).fma(Mth.sin(phi) * r, UPV);
                bill(l, l.bulk, l.bulkBlend, P, l.size * 0.22f * (0.5f + t), phi, VfxVertexBuffer.withAlpha(l.pale, 0.3f + 0.7f * t));
                if (v == 1 && i % 3 == 1 && s == 0) bill(l, l.accent, l.accentBlend, P, l.size * 0.3f, 0f, l.tint);     // the song's notes
            }
        }
        bill(l, l.accent, l.accentBlend, TIP, l.size * 0.8f, l.age * 0.2f, l.tint);
    }

    /** Dark and infernal: a dark seal at the head, a red ember trail rising behind it, the skull in the middle. */
    private static void castInfernal(Look l) {
        int dark = shade(l.tint, 0x14060A, 0.75f);
        bill(l, l.bulk, l.bulkBlend, TIP, l.size * 1.1f, l.age * 0.06f, dark);
        bill(l, l.accent, l.accentBlend, TIP, l.size * 0.55f, 0f, VfxVertexBuffer.withAlpha(SKULL, l.fade));
        int n = l.ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {
            float t = (i + 0.5f) / n, rise = frac(l.age * 0.05f + i * 0.37f);
            P.set(TAIL).lerp(TIP, t).fma(Mth.sin(i * 2.7f + l.age * 0.2f) * l.size * 0.25f, SIDE).fma(rise * l.size * 0.5f, l.ctx.camUp);
            bill(l, VfxTextures.SPARK, VfxBlend.ADD, P, l.size * 0.16f, l.age * 0.1f + i, VfxVertexBuffer.withAlpha(EMBER, l.fade * Mth.sin(rise * Mth.PI)));
        }
    }

    // ================================================================== FIELD: an area on the ground; callers pass the damage radius / 3 as the power
    private static void field(Look l) {
        VfxRenderContext ctx = l.ctx;
        C.set(l.from).lerp(l.to, 0.5f);
        VfxPose plane = VfxPose.ground(new Vector3f(C));
        float pulse = 0.88f + 0.12f * Mth.sin(l.age * 0.14f);
        float radius = l.size * 3f * pulse;
        // the edge of the real area, the same for every group so the sigil stays readable next to it
        l.buf.ring(VfxTextures.GLOW, VfxBlend.ADD, plane.spin(-l.age * 0.025f), radius * 0.96f, radius, ctx.seg(28, 12), 1f, 0f,
                VfxVertexBuffer.withAlpha(l.tint, l.fade * 0.35f));
        switch (l.group) {
            case TRAILS -> fieldTrails(l, plane, radius);
            case CIRCLES -> fieldCircles(l, plane, radius);
            case SHARDS -> fieldShards(l, plane, radius);
            case FLUID -> fieldFluid(l, plane, radius);
            case SPIRAL -> fieldSpiral(l, plane, radius);
            case INFERNAL -> fieldInfernal(l, plane, radius);
            default -> fieldMotes(l, plane, radius);
        }
    }

    private static void fieldMotes(Look l, VfxPose plane, float radius) {
        int v = l.variant, n = l.ctx.seg(16 + 4 * v, 8);
        float rise = pick(RISE, v);
        int body = v == 1 ? shade(l.pale, 0x4A4654, 0.45f) : l.pale;
        for (int i = 0; i < n; i++) {
            float ph = frac(l.age * 0.012f * (1f + 0.3f * v) + i * 0.618034f), ang = i * 2.399f + l.age * 0.02f * (i % 2 == 0 ? 1 : -1);
            float rr = radius * (0.25f + 0.7f * frac(i * 0.37f + 0.11f)), wob = Mth.sin(i * 1.3f);
            place(plane, Mth.cos(ang) * rr, Mth.sin(ang) * rr, P);
            P.y += (rise < 0f ? 1f - ph : ph) * l.size * 1.6f;
            bill(l, l.bulk, l.bulkBlend, P, l.size * (0.2f + 0.2f * wob * wob), ang, VfxVertexBuffer.withAlpha(body, Mth.sin(ph * Mth.PI) * l.fade));
        }
    }

    private static void fieldTrails(Look l, VfxPose plane, float radius) {
        int v = l.variant;
        float sweep = pick(SWEEP, v);
        for (int k = 0; k < 3; k++) {
            float spin = l.age * 0.06f * (v % 2 == 0 ? 1 : -1) + k * Mth.TWO_PI / 3f, r = radius * (0.9f - 0.1f * k);
            VfxPose arc = plane.spin(spin);
            l.buf.arc(l.bulk, l.bulkBlend, arc, r, l.size * (0.28f + 0.03f * v), 0f, sweep, l.ctx.seg(18, 8), VfxVertexBuffer.withAlpha(l.tint, l.fade * 0.85f));
            place(arc, Mth.cos(sweep) * r, Mth.sin(sweep) * r, P);
            bill(l, l.accent, l.accentBlend, P, l.size * 0.34f, spin + sweep, VfxVertexBuffer.withAlpha(l.pale, l.fade));
        }
    }

    private static void fieldCircles(Look l, VfxPose plane, float radius) {
        int v = l.variant;
        l.buf.plane(l.bulk, l.bulkBlend, plane.spin(l.age * (v % 2 == 0 ? 0.02f : -0.02f)), radius * 1.02f, VfxVertexBuffer.withAlpha(l.tint, l.fade * 0.8f));
        l.buf.plane(l.accent, l.accentBlend, plane.lift(0.01f).spin(-l.age * 0.035f + v), radius * 0.66f, VfxVertexBuffer.withAlpha(l.pale, l.fade * 0.75f));
        for (int i = 0; i < 6; i++) {
            float a = l.age * 0.04f + i * Mth.TWO_PI / 6f;
            place(plane, Mth.cos(a) * radius * 0.96f, Mth.sin(a) * radius * 0.96f, P);
            bill(l, VfxTextures.GLOW, VfxBlend.ADD, P, 0.3f + 0.04f * l.size, a, VfxVertexBuffer.withAlpha(l.pale, l.fade));
        }
    }

    private static void fieldShards(Look l, VfxPose plane, float radius) {
        int v = l.variant, n = l.ctx.seg(8 + v % 5, 5);
        float grow = Mth.clamp(l.age / Math.max(4f, l.dur * 0.25f), 0f, 1f);
        grow = grow * grow * (3f - 2f * grow);
        l.buf.plane(l.bulk, l.bulkBlend, plane.spin(l.age * 0.02f), radius * 0.45f, VfxVertexBuffer.withAlpha(l.tint, l.fade * 0.5f));
        for (int i = 0; i < n; i++) {
            float a = i * Mth.TWO_PI / n + 0.35f * v, sz = l.size * (0.7f + 0.35f * ((i * 7) % 3) / 2f) * grow;
            place(plane, Mth.cos(a) * radius * 0.82f, Mth.sin(a) * radius * 0.82f, P);
            P.y += sz * 0.45f;
            bill(l, l.accent, l.accentBlend, P, sz, 0.35f * Mth.sin(i * 2.7f), VfxVertexBuffer.withAlpha(l.pale, l.fade));
        }
    }

    private static void fieldFluid(Look l, VfxPose plane, float radius) {
        int v = l.variant, n = l.ctx.seg(10 + 2 * v, 5);
        int puddle = v == 1 ? shade(l.tint, 0x4A2E1E, 0.4f) : l.tint;
        l.buf.ring(VfxTextures.GLOW, VfxBlend.ALPHA, plane, radius * 0.55f, radius * 0.98f, l.ctx.seg(20, 10), 1f, 0f, VfxVertexBuffer.withAlpha(puddle, l.fade * 0.35f));
        for (int k = 0; k < 2; k++) {                                                          // ripples spreading over the puddle
            float ph = frac(l.age * 0.03f + k * 0.5f), rr = radius * ph;
            l.buf.ring(VfxTextures.GLOW, VfxBlend.ADD, plane, rr * 0.93f, rr, l.ctx.seg(16, 8), 1f, 0f, VfxVertexBuffer.withAlpha(l.pale, (1f - ph) * 0.45f * l.fade));
        }
        for (int i = 0; i < n; i++) {                                                          // droplets hopping
            float ph = frac(l.age * 0.045f + i * 0.381966f), a = i * 2.399f, rr = radius * (0.2f + 0.7f * frac(i * 0.618f));
            place(plane, Mth.cos(a) * rr, Mth.sin(a) * rr, P);
            P.y += 4f * ph * (1f - ph) * l.size * 0.9f * (0.6f + 0.2f * (i % 3));
            bill(l, l.bulk, l.bulkBlend, P, l.size * 0.24f, 0f, VfxVertexBuffer.withAlpha(l.pale, l.fade));
        }
    }

    private static void fieldSpiral(Look l, VfxPose plane, float radius) {
        int v = l.variant, strands = pick(STRANDS, v), n = l.ctx.seg(10, 5);
        float helix = pick(HELIX, v) / 0.38f;
        l.buf.plane(l.accent, l.accentBlend, plane.spin(l.age * 0.05f), radius * 0.5f, VfxVertexBuffer.withAlpha(l.tint, l.fade * 0.5f));
        for (int s = 0; s < strands; s++) {
            for (int i = 0; i < n; i++) {
                float t = (i + 0.5f) / n, phi = l.age * 0.09f + t * Mth.TWO_PI * 1.3f + s * Mth.TWO_PI / strands + v;
                float r = radius * 0.6f * helix * (v == 2 ? 1f - 0.5f * t : 0.75f + 0.25f * Mth.sin(t * Mth.PI));
                place(plane, Mth.cos(phi) * r, Mth.sin(phi) * r, P);
                P.y += t * l.size * 1.9f;
                bill(l, l.bulk, l.bulkBlend, P, l.size * 0.22f, phi, VfxVertexBuffer.withAlpha(l.pale, (1f - t * 0.6f) * l.fade));
            }
        }
    }

    private static void fieldInfernal(Look l, VfxPose plane, float radius) {
        int dark = shade(l.tint, 0x14060A, 0.75f);
        l.buf.plane(l.bulk, l.bulkBlend, plane.spin(l.age * 0.015f), radius * 1.02f, VfxVertexBuffer.withAlpha(dark, l.fade * 0.85f));
        l.buf.plane(l.accent, VfxBlend.ADD, plane.lift(0.02f).spin(-l.age * 0.02f), radius * 0.36f, VfxVertexBuffer.withAlpha(SKULL, l.fade * 0.55f));
        int n = l.ctx.seg(18, 8);
        for (int i = 0; i < n; i++) {
            float ph = frac(l.age * 0.03f + i * 0.37f), rr = radius * 0.95f * frac(i * 0.618f), a = i * 2.399f;
            place(plane, Mth.cos(a) * rr, Mth.sin(a) * rr, P);
            P.y += ph * l.size * 1.6f;
            bill(l, VfxTextures.SPARK, VfxBlend.ADD, P, l.size * 0.16f, a + l.age * 0.1f, VfxVertexBuffer.withAlpha(EMBER, Mth.sin(ph * Mth.PI) * l.fade));
        }
    }

    // ================================================================== BURST: a nova facing the camera
    private static void burst(Look l) {
        C.set(l.from).lerp(l.to, 0.5f);
        float radius = l.size * 3f * (0.28f + l.progress * 0.9f);
        VfxPose face = VfxPose.facing(new Vector3f(C), l.look);
        switch (l.group) {
            case TRAILS -> burstTrails(l, face, radius);
            case CIRCLES -> burstCircles(l, face, radius);
            case SHARDS -> burstShards(l, face, radius);
            case FLUID -> burstFluid(l, face, radius);
            case SPIRAL -> burstSpiral(l, face, radius);
            case INFERNAL -> burstInfernal(l, face, radius);
            default -> burstMotes(l, face, radius);
        }
    }

    private static void burstMotes(Look l, VfxPose face, float radius) {
        int v = l.variant, n = l.ctx.seg(14, 8);
        float rise = pick(RISE, v);
        int body = v == 1 ? shade(l.pale, 0x4A4654, 0.45f) : l.pale;
        for (int i = 0; i < n; i++) {
            float a = i * 2.399f + l.age * 0.03f, rr = radius * (0.4f + 0.6f * frac(i * 0.37f)), wob = Mth.sin(i * 1.7f);
            place(face, Mth.cos(a) * rr, Mth.sin(a) * rr + rise * l.progress * l.size * 0.6f, P);
            bill(l, l.bulk, l.bulkBlend, P, l.size * (0.3f + 0.2f * wob * wob), a, VfxVertexBuffer.withAlpha(body, 0.8f * l.fade));
        }
        bill(l, l.accent, l.accentBlend, face.origin(), radius * 0.8f, l.age * 0.05f, VfxVertexBuffer.withAlpha(l.tint, 0.5f));
    }

    private static void burstTrails(Look l, VfxPose face, float radius) {
        int v = l.variant;
        float sweep = pick(SWEEP, v);
        for (int k = 0; k < 3; k++) {
            float spin = l.age * 0.04f * (v % 2 == 0 ? 1 : -1) + k * Mth.TWO_PI / 3f, r = radius * (0.7f + 0.12f * k);
            VfxPose arc = face.spin(spin);
            l.buf.arc(l.bulk, l.bulkBlend, arc, r, l.size * 0.2f, 0f, sweep, l.ctx.seg(18, 8), VfxVertexBuffer.withAlpha(l.tint, l.fade * 0.9f));
            place(arc, Mth.cos(sweep) * r, Mth.sin(sweep) * r, P);
            bill(l, l.accent, l.accentBlend, P, l.size * 0.36f, spin + sweep, VfxVertexBuffer.withAlpha(l.pale, l.fade));
        }
    }

    private static void burstCircles(Look l, VfxPose face, float radius) {
        int v = l.variant;
        l.buf.plane(l.bulk, l.bulkBlend, face.spin(l.age * 0.05f * (v % 2 == 0 ? 1 : -1)), radius * 1.05f, VfxVertexBuffer.withAlpha(l.tint, l.fade * 0.9f));
        l.buf.plane(l.accent, l.accentBlend, face.lift(0.02f).spin(-l.age * 0.08f), radius * 0.7f, VfxVertexBuffer.withAlpha(l.pale, l.fade * 0.85f));
    }

    private static void burstShards(Look l, VfxPose face, float radius) {
        int v = l.variant, n = l.ctx.seg(8 + v % 5, 6);
        bill(l, l.bulk, l.bulkBlend, face.origin(), radius * 2.2f, l.age * 0.03f, VfxVertexBuffer.withAlpha(l.pale, l.fade * 0.8f));
        for (int i = 0; i < n; i++) {
            float a = i * Mth.TWO_PI / n + 0.3f * v, rr = radius * (0.6f + 0.4f * frac(i * 0.618f));
            place(face, Mth.cos(a) * rr, Mth.sin(a) * rr, P);
            bill(l, l.accent, l.accentBlend, P, l.size * 0.5f, a - Mth.HALF_PI, VfxVertexBuffer.withAlpha(l.pale, l.fade));
        }
    }

    private static void burstFluid(Look l, VfxPose face, float radius) {
        int v = l.variant, n = l.ctx.seg(14, 8);
        float heavy = v == 1 ? 1.35f : 1f, ease = 1f - (1f - l.progress) * (1f - l.progress);
        int body = v == 1 ? shade(l.pale, 0x4A2E1E, 0.4f) : l.pale;
        l.buf.ring(VfxTextures.GLOW, VfxBlend.ALPHA, face, radius * 0.9f, radius, l.ctx.seg(28, 12), 1f, 0f, VfxVertexBuffer.withAlpha(l.tint, l.fade * 0.4f));
        for (int i = 0; i < n; i++) {                                                          // a splash crown thrown out and falling back
            float a = i * Mth.TWO_PI / n, out = radius * (0.3f + 0.7f * ease);
            place(face, Mth.cos(a) * out, Mth.sin(a) * out, P);
            P.y -= l.progress * l.progress * radius * 0.4f;
            bill(l, l.bulk, l.bulkBlend, P, l.size * 0.3f * heavy, a - Mth.HALF_PI, VfxVertexBuffer.withAlpha(body, l.fade));
        }
        bill(l, l.accent, l.accentBlend, face.origin(), radius * 0.7f, 0f, VfxVertexBuffer.withAlpha(l.tint, l.fade * 0.6f));
    }

    private static void burstSpiral(Look l, VfxPose face, float radius) {
        int v = l.variant, n = l.ctx.seg(8, 4);
        l.buf.plane(l.bulk, l.bulkBlend, face.spin(l.age * 0.12f), radius * 1.1f, VfxVertexBuffer.withAlpha(l.tint, l.fade * 0.85f));
        l.buf.plane(l.accent, l.accentBlend, face.lift(0.02f).spin(-l.age * 0.2f), radius * 0.6f, VfxVertexBuffer.withAlpha(l.pale, l.fade * 0.8f));
        for (int i = 0; i < n; i++) {
            float a = l.age * 0.12f + i * Mth.TWO_PI / n + v, rr = radius * (0.5f + 0.5f * i / n);
            place(face, Mth.cos(a) * rr, Mth.sin(a) * rr, P);
            bill(l, l.bulk, l.bulkBlend, P, l.size * 0.26f, a, VfxVertexBuffer.withAlpha(l.pale, l.fade));
        }
    }

    private static void burstInfernal(Look l, VfxPose face, float radius) {
        int dark = shade(l.tint, 0x14060A, 0.75f);
        l.buf.plane(l.bulk, l.bulkBlend, face.spin(l.age * 0.03f), radius * 1.05f, VfxVertexBuffer.withAlpha(dark, l.fade * 0.85f));
        l.buf.ring(VfxTextures.GLOW, VfxBlend.ADD, face, radius * 0.94f, radius * 1.02f, l.ctx.seg(28, 12), 1f, 0f, VfxVertexBuffer.withAlpha(EMBER, l.fade * 0.6f));
        int n = l.ctx.seg(14, 6);
        for (int i = 0; i < n; i++) {
            float a = i * Mth.TWO_PI / n + 0.4f, rr = radius * (0.5f + 0.5f * frac(i * 0.618f));
            place(face, Mth.cos(a) * rr, Mth.sin(a) * rr, P);
            bill(l, VfxTextures.SPARK, VfxBlend.ADD, P, l.size * 0.2f, a, VfxVertexBuffer.withAlpha(EMBER, l.fade));
        }
        bill(l, l.accent, VfxBlend.ADD, face.origin(), radius * 0.5f, 0f, VfxVertexBuffer.withAlpha(SKULL, l.fade * 0.55f));
    }
}
