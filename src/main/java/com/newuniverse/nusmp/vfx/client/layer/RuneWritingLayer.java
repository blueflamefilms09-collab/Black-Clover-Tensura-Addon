package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil;
import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil.Frame;
import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil.Params;
import com.newuniverse.nusmp.mana.method.RuneGlyphLayout;
import com.newuniverse.nusmp.mana.method.RuneGlyphLayout.Layout;
import com.newuniverse.nusmp.mana.method.RuneHoldView;
import com.newuniverse.nusmp.mana.method.RuneSpec;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.AbstractVfxLayer;
import com.newuniverse.nusmp.vfx.client.VfxBlend;
import com.newuniverse.nusmp.vfx.client.VfxInstance;
import com.newuniverse.nusmp.vfx.client.VfxRenderContext;
import com.newuniverse.nusmp.vfx.client.VfxTextures;
import com.newuniverse.nusmp.vfx.client.VfxVertexBuffer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * The Mana Method rune, as the owner described it: actual rune symbols WRITTEN in the air. The maths is {@link RuneGlyphLayout} (pure, tested; see its class comment for the look); this layer only
 * turns the quads it fills into vertex-buffer quads: a strip per stroke segment with the stroke texture of the element (a yellow core and an orange-red edge, tinted per element), a glow on the
 * pen, a thin glowing circle under each ring, camera-facing lightning strips and a glow behind the open grimoire (the book itself is the mod's real model, drawn by the client holder).
 *
 * <p>Budget: ONE instance per rune, at most {@value RuneGlyphLayout#MAX_QUADS} quads = 1,200 vertices (this layer says so through {@link #hardVertexCap}; every other layer stays at 400), fewer
 * at the lower levels of detail the manager gives it by distance and quality ({@code ctx.detail}); the manager keeps the nearest effects when a frame's vertices would pass the quality's cap.
 * The layout is filled into one preallocated {@link Layout} (a rune is drawn at a time), the corner vectors are four reused scratch vectors, the frame and the parameters of a rune are kept
 * for its whole life (eight slots): nothing is allocated per frame.
 *
 * <p>Protocol (never sent over the network; {@code client.manamethod.RuneClient} spawns it when the one rune packet arrives): from = the rune's centre, to = centre + the caster's look,
 * colour = the element's tint, seed = the packed {@link RuneSpec} (bits 0..5), duration = charge + {@value #TAIL} ticks.
 *
 * <p>3.5.0, the hold: when bits 8..15 of the seed hold a key ({@link RuneHoldView#keyOf}), the rune belongs to a HELD page and is drawn from the view the client holder keeps for it
 * ({@link RuneGlyphLayout#fillStaged}): the rings are written as long as the key is held, stop where it was let go, and only the tiers that were reached are completed. No key: the fixed
 * rune of 3.4.0, drawn exactly as before. The view is looked up once per instance and the progress arrays are four preallocated numbers: nothing is allocated per frame.
 */
public class RuneWritingLayer extends AbstractVfxLayer {
    /** The ticks the rune lives after its charge: the release flare, then it fades over the last {@value #FADE}. */
    public static final int TAIL = 10, FADE = 4;

    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    /** One stroke texture per tint: gold (True Magic, the reference look), flame, water, wind, earth, lightning, plant. */
    private static final ResourceLocation[] STROKE = {t("rune_stroke_gold"), t("rune_stroke_flame"), t("rune_stroke_water"), t("rune_stroke_wind"), t("rune_stroke_earth"), t("rune_stroke_lightning"), t("rune_stroke_plant")};
    /** The core colour of each tint (ARGB without alpha): the circle lines, the arcs, the pen. */
    private static final int[] CORE = {0xFFEE6E, 0xFFD660, 0xBEF0FF, 0xE4FFEE, 0xFFE8AA, 0xFFFFBE, 0xDCFFAA};
    private static final ResourceLocation LINE = t("rune_line_ring"), ARC = t("rune_arc_strip"), GLOW = VfxTextures.GLOW;
    private static final int[] TINT_OF_ELEMENT = {1, 2, 3, 4, 5, 6};          // FIRE, WATER, WIND, EARTH, LIGHTNING, PLANT -> stroke texture index (0 is gold)

    /** The strokes are drawn at 0.85 x their light (the light goes to 1.7 at the pen), so the glow at rest is bright and the writing head is full. */
    private static final float ALPHA_PER_LIGHT = 0.85f;

    private static final int SLOTS = 16;                                                  // 3.6.0: six cast runes and eight painted runes may be up together

    /** The parts of a rune that do not change during its life, kept per instance (the manager gives a layer no place to put them). */
    private static final class Slot {
        VfxInstance inst;
        RuneSpec spec;
        Params params;
        Frame frame;
        int tint;
        RuneHoldView view;
    }

    private final Layout layout = new Layout();
    private final Slot[] slots = new Slot[SLOTS];
    private int nextSlot;
    private final double[] prog = new double[RuneGlyphLayout.RING_GROUPS], scale = new double[RuneGlyphLayout.RING_GROUPS], written = new double[RuneGlyphLayout.RING_GROUPS];
    private final Vector3f p0 = new Vector3f(), p1 = new Vector3f(), p2 = new Vector3f(), p3 = new Vector3f();

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.RUNE_WRITING); }

    @Override public int defaultDuration(VfxShape shape) { return 28; }

    @Override public int defaultColor(VfxShape shape) { return 0xFFFFE06A; }

    @Override public int vertexBudget(VfxShape shape) { return RuneGlyphLayout.MAX_QUADS * 4; }

    @Override public int hardVertexCap(VfxShape shape) { return RuneGlyphLayout.MAX_QUADS * 4; }

    private Slot slotOf(VfxInstance inst, VfxRenderContext ctx) {
        for (Slot s : slots) if (s != null && s.inst == inst) return s;
        Slot s = slots[nextSlot];
        if (s == null) slots[nextSlot] = s = new Slot();
        nextSlot = (nextSlot + 1) % SLOTS;
        s.inst = inst;
        s.spec = RuneSpec.unpack((int) (inst.seed & 63L));
        s.params = RuneGeneratorUtil.paramsFor(s.spec);
        float scale = inst.power;                                                        // 3.6.0: a painted rune (brush or body) asks for another size through the effect's power; the held and cast runes send 1
        if (scale > 0.05f && Math.abs(scale - 1f) > 0.001f) s.params = new Params(s.params.radius() * scale, s.params.complexity(), s.params.ringParticle(), s.params.glyphParticle(), s.params.tint());
        Vec3 from = inst.from(ctx), to = inst.to(ctx);
        double lx = to.x - from.x, ly = to.y - from.y, lz = to.z - from.z;
        s.frame = RuneGeneratorUtil.frameFacing(from.x, from.y, from.z, lx, ly, lz);
        s.view = RuneHoldView.lookup(RuneHoldView.keyOf(inst.seed));
        s.tint = s.spec.trueMagic() ? 0 : TINT_OF_ELEMENT[Math.min(s.spec.element().ordinal(), TINT_OF_ELEMENT.length - 1)];
        return s;
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        int level = RuneGlyphLayout.levelFor(ctx.detail);
        if (level == RuneGlyphLayout.LEVEL_NONE) return;
        Slot s = slotOf(inst, ctx);
        double age = inst.ageTicks(ctx.partialTick);
        double cx = ctx.camPos.x, cy = ctx.camPos.y, cz = ctx.camPos.z;
        float fade;
        if (s.view != null) {                                                            // a held rune: the picture follows the hold
            RuneHoldView v = s.view;
            fade = v.fade(age);
            if (fade <= 0f) return;
            v.progress(age, prog, scale, written);
            RuneGlyphLayout.fillStaged(layout, s.spec, s.params, v.frame, age, prog, scale, v.flare(age), level, cx, cy, cz);
            if (v.paintTicks > 0) layout.bookLight = 0f;                                 // 3.6.0: a painted rune (brush, body) has no grimoire: no gold glow where the book would be
        } else {
            int charge = Math.max(1, inst.duration - TAIL);
            fade = (float) Math.max(0.0, Math.min(1.0, (inst.duration - age) / FADE));
            if (fade <= 0f) return;
            RuneGlyphLayout.fill(layout, s.spec, s.params, s.frame, age, charge, level, cx, cy, cz);
        }
        ResourceLocation stroke = STROKE[s.tint];
        int core = CORE[s.tint];
        // the circle lines first (they lie under the glyphs), then the strokes, the pens, the lightning and the glow behind the book
        for (int i = 0; i < layout.lines; i++) {
            load(layout.lq, i * 12, cx, cy, cz);
            buf.quad(LINE, VfxBlend.ADD, p0, p1, p2, p3, 0f, 0f, 1f, 1f, argb(layout.lineLight[i] * fade, core), argb(layout.lineLight[i] * fade, core));
        }
        for (int i = 0; i < layout.strips; i++) {
            load(layout.sq, i * 12, cx, cy, cz);
            int c = argb(layout.stripLight[i] * ALPHA_PER_LIGHT * fade, 0xFFFFFF);
            buf.quad(stroke, VfxBlend.ADD, p0, p1, p2, p3, 0f, 0f, 1f, 1f, c, c);
        }
        for (int i = 0; i < layout.pens; i++) {
            load(layout.pq, i * 12, cx, cy, cz);
            int c = argb(0.9f * fade, VfxVertexBuffer.whiten(0xFF000000 | core, 0.6f) & 0xFFFFFF);
            buf.quad(GLOW, VfxBlend.ADD, p0, p1, p2, p3, 0f, 0f, 1f, 1f, c, c);
        }
        for (int i = 0; i < layout.arcQuads; i++) {
            load(layout.aq, i * 12, cx, cy, cz);
            int c = argb(layout.arcLight[i] * fade, VfxVertexBuffer.whiten(0xFF000000 | core, 0.35f) & 0xFFFFFF);
            buf.quad(ARC, VfxBlend.ADD, p0, p1, p2, p3, 0f, 0f, 1f, 1f, c, c);
        }
        if (layout.bookLight > 0f) {                                                   // the gold glow of the real grimoire the client draws at this point
            p0.set((float) (layout.bookX - cx), (float) (layout.bookY - cy), (float) (layout.bookZ - cz));
            buf.billboard(ctx, GLOW, VfxBlend.ADD, p0, 1.5f, 0f, argb(0.8f * layout.bookLight * fade, 0xFFC850));
        }
    }

    /** Corner k of the quad at {@code a[at..at+11]} into the scratch vectors, camera-relative. */
    private void load(float[] a, int at, double cx, double cy, double cz) {
        p0.set((float) (a[at] - cx), (float) (a[at + 1] - cy), (float) (a[at + 2] - cz));
        p1.set((float) (a[at + 3] - cx), (float) (a[at + 4] - cy), (float) (a[at + 5] - cz));
        p2.set((float) (a[at + 6] - cx), (float) (a[at + 7] - cy), (float) (a[at + 8] - cz));
        p3.set((float) (a[at + 9] - cx), (float) (a[at + 10] - cy), (float) (a[at + 11] - cz));
    }

    /** An ARGB colour from an alpha 0..1 and an RGB. */
    static int argb(float alpha, int rgb) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    /** The element a rune of this spec is drawn in (the tint of its strokes): for the tests. */
    public static int tintIndex(RuneSpec spec) {
        return spec.trueMagic() ? 0 : TINT_OF_ELEMENT[Math.min(spec.element().ordinal(), TINT_OF_ELEMENT.length - 1)];
    }

    /** The element names in tint order, for the texture script and the tests: gold first. */
    public static String[] tintNames() { return new String[]{"gold", "flame", "water", "wind", "earth", "lightning", "plant"}; }
}
