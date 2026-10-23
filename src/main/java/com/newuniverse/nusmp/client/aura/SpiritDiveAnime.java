package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.book.SpiritDiveAnimeGeometry;
import com.newuniverse.nusmp.book.SpiritDiveAnimeGeometry.Shape;
import com.newuniverse.nusmp.book.SpiritDiveAnimeRules;
import com.newuniverse.nusmp.book.SpiritDiveAnimeRules.Pal;
import com.newuniverse.nusmp.book.SpiritDiveAnimeRules.Spec;
import com.newuniverse.nusmp.book.SpiritDivePieces;
import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;
import com.newuniverse.nusmp.book.SpiritDiveTier;
import com.newuniverse.nusmp.book.SpiritDiveTier.Grade;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;
import com.newuniverse.nusmp.client.SafeAdditive;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.SealDraw;
import com.newuniverse.nusmp.perf.PerfProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

/**
 * Spirit Dive, ANIME look (the default look since 3.2): what the anime shows, drawn as smooth shapes on the wearer's own body. The wearer's real skin and clothes stay visible; over them:
 * <ol>
 *   <li>the SHELL ({@link SpiritDiveAnimeShell}): the energy of the spirit wrapped round every limb, streaks flowing along it, a bright cel outline at the silhouette;</li>
 *   <li>the HAIR: a glowing cap on the head (the hat layer, tinted strands) and curved, tapered locks that sway with the movement ({@link SpiritDiveAnimeRibbon});</li>
 *   <li>the EYES: two glowing slits on the face, in the face's own plane, so they follow the head's turn and pitch; none under a helmet;</li>
 *   <li>the HALO: a thin spiky crown of light, a camera-facing sprite that turns slowly behind the head (a half crown in the first, half dive of Wind);</li>
 *   <li>the PLUMES: big smooth translucent sheets sweeping out from the shoulder blades, layered three deep with a bright leading edge: pale wind plumes, a flame cloak, water ribbons, rock shards;</li>
 *   <li>the CHARACTER tier only: more plumes and locks, hand and foot trails, a second halo, the circle of the element standing behind the back (Yuno's circle of crosses), the faint silhouette of the
 *       spirit behind the wearer (Sylph, Salamander, Undine, Gnome: the spirit assimilated).</li>
 * </ol>
 * Entry (18 ticks): a flash, the shell grows from the feet up, the hair lifts, the halo opens with a ring pulse and the plumes burst open; the exit is the same backwards. Fuegoleon's flame arm is not
 * drawn here: when {@link FuegoleonArmAura#drawsArm} is true the shell leaves his right arm out. The server entry point, the tier plumbing and the style bits are untouched; this class only draws.
 *
 * <p>Cost: counted by {@link SpiritDiveAnimeRules#vertices}; level of detail by the distance ({@link SpiritDiveTier#lod}); at most {@link SpiritDiveTier#CHARACTER_PER_FRAME} character tiers a frame
 * ({@link SpiritDiveFrame}). No allocation per frame: statics for the scratch arrays and quaternion, cached textures and render types.</p>
 */
public final class SpiritDiveAnime {
    private SpiritDiveAnime() {}

    private static final int FB = PropDraw.FULL_BRIGHT;
    private static final Quaternionf Q = new Quaternionf();                                                  // scratch: the render thread is the only user
    private static final PerfProbe.Section SECTION = PerfProbe.section("c.spiritdive.anime");                  // timed (/multiverse perf)

    private static final ResourceLocation[] PLUME = new ResourceLocation[4], PLUME_GLOW = new ResourceLocation[4], LOCK = new ResourceLocation[4], LOCK_GLOW = new ResourceLocation[4],
            HALO = new ResourceLocation[4], HAIR = new ResourceLocation[4], SIGIL = new ResourceLocation[4], RING = new ResourceLocation[4];
    private static final ResourceLocation EYE = AuraRender.tex(SpiritDiveAnimeRules.EYE_TEXTURE), BURST = AuraRender.tex(SpiritDiveAnimeRules.BURST_TEXTURE),
            GLOW = AuraRender.tex("particle/glow"), PILLAR = AuraRender.tex("aura/sdv_pillar");
    private static final GeoSpec[][] SPIRIT = new GeoSpec[4][];
    private static final float[][] SPIRIT_POSE = {{1.18f, 0f, 3f, 0f}, {0.62f, 0f, 3f, 14f}, {1.05f, 0f, 14f, -2f}, {1.0f, 0f, 5f, 0f}};     // scale, (unused), back px, up px by element (WIND, WATER, FIRE, EARTH)

    static {
        for (Element e : Element.values()) {
            int i = e.ordinal();
            PLUME[i] = AuraRender.tex(SpiritDiveAnimeRules.plumeTexture(e));
            PLUME_GLOW[i] = AuraRender.tex(SpiritDiveAnimeRules.plumeGlowTexture(e));
            LOCK[i] = AuraRender.tex(SpiritDiveAnimeRules.lockTexture(e));
            LOCK_GLOW[i] = AuraRender.tex(SpiritDiveAnimeRules.lockGlowTexture(e));
            HALO[i] = AuraRender.tex(SpiritDiveAnimeRules.haloTexture(e));
            HAIR[i] = AuraRender.tex(SpiritDiveAnimeRules.hairTexture(e));
        }
        SIGIL[0] = AuraRender.tex("aura/sdv_sigil_wind"); SIGIL[1] = AuraRender.tex("aura/sdv_sigil_water"); SIGIL[2] = AuraRender.tex("aura/sdv_sigil_fire"); SIGIL[3] = AuraRender.tex("aura/sdv_sigil_earth");
        RING[0] = AuraRender.tex("particle/air_ring"); RING[1] = AuraRender.tex("particle/waterfx_ripple"); RING[2] = AuraRender.tex("particle/flamefx_ring"); RING[3] = AuraRender.tex("particle/earthfam_rock_ring");
        SPIRIT[0] = new GeoSpec[]{GeoSpec.of("spirit", "sylph_fairy"), GeoSpec.of("spirit", "sylph_fairy_wings")};
        SPIRIT[1] = new GeoSpec[]{GeoSpec.of("spirit", "undine_mermaid")};
        SPIRIT[2] = new GeoSpec[]{GeoSpec.of("spirit", "salamander")};
        SPIRIT[3] = new GeoSpec[]{GeoSpec.of("spirit", "gnome")};
    }

    // ------------------------------------------------------------------ the entry point
    /** Called by {@link SpiritDiveAura} for the ANIME look: one dive of {@code el} at {@code st} for the tier {@code grade}. */
    static void paint(AuraContext c, Element el, Stage st, Grade grade, float pulse) {
        Lod lod = SpiritDiveTier.lod(SpiritDiveFrame.distSq(c));
        if (lod == Lod.CULLED) return;
        long t0 = PerfProbe.begin();
        try {
            paintLod(c, el, st, grade, pulse, lod);
        } finally {
            PerfProbe.end(SECTION, t0);
        }
    }

    private static void paintLod(AuraContext c, Element el, Stage st, Grade grade, float pulse, Lod lod) {
        boolean wantsCharacter = SpiritDiveAnimeRules.character(el, grade) && SpiritDiveLook.characterEffects() && lod != Lod.FAR;
        boolean character = wantsCharacter && SpiritDiveFrame.admitCharacter();
        Grade use = character ? SpiritDiveTier.effective(el, grade) : Grade.STANDARD;
        Spec sp = SpiritDiveAnimeRules.spec(character, lod);
        Pal pal = SpiritDiveAnimeRules.pal(el, use, false);
        boolean upgrade = st == Stage.UPGRADE, full = SpiritDivePieces.full(st), half = SpiritDivePieces.halfBody(el, st);
        float progress = SpiritDiveAnimeRules.progress(c.t(), c.left());
        float kept = upgrade ? SpiritDiveAnimeRules.progress(1e4f, c.left()) : progress;                  // an upgrade keeps the half that is on
        if (Math.max(progress, kept) <= 0.01f) return;
        float flare = DiveCast.casting(c.player().getId()) ? 1f : 0f, k = 1f + 0.4f * flare;
        boolean skipArmR = el == Element.FIRE && FuegoleonArmAura.drawsArm(c.player());                    // the flame arm is its own look: no second arm
        boolean helmet = DiveHead.helmet(c.player()), self1p = firstPersonSelf(c);
        int mask = SpiritDivePieces.shellParts(el, st), keptMask = upgrade ? SpiritDivePieces.shellParts(el, Stage.HALF) & mask : 0;

        SpiritDiveAnimeShell.draw(c, el, SpiritDiveAnimeRules.shellPasses(lod), pal.tint(), pal.rim(), pal.glow(), pulse, progress, mask, full, skipArmR, keptMask, k);
        if (sp.backdrop()) backdrop(c, el, progress, pulse, pal);
        if (sp.plumeRows() > 0) plumes(c, el, st, sp, character, pal, progress, kept, pulse * k, half, full);
        if (sp.trails() > 0) trails(c, el, sp, pal, progress, pulse * k);
        if (!self1p) head(c, el, st, sp, character, pal, progress, pulse * k, half, full, helmet);
        extras(c, el, st, sp, character, pal, progress, pulse, half, lod);
    }

    // ------------------------------------------------------------------ plumes
    private static void applyPart(PoseStack pose, ModelPart part) {
        pose.translate(part.x / 16f, part.y / 16f, part.z / 16f);
        if (part.xRot != 0f || part.yRot != 0f || part.zRot != 0f) pose.mulPose(Q.rotationZYX(part.zRot, part.yRot, part.xRot));
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);                                                           // model pixels
    }

    private static float lenOf(boolean character, boolean full) { return (character ? 1.0f : 0.8f) * (full ? 1f : 0.7f); }

    /**
     * The plume sheets: for each row of the element's table, each side, each layer, a smooth strip along a Bezier spine (the layers are shorter, narrower and brighter toward the front). Two passes:
     * the translucent body with the cel bands baked in (so the dark outline reads), then the additive glow of the leading edge. The half dive of Wind draws the LEFT side only; an upgrade keeps the
     * left side steady and opens the right one.
     */
    private static void plumes(AuraContext c, Element el, Stage st, Spec sp, boolean character, Pal pal, float progress, float kept, float lit, boolean half, boolean full) {
        PoseStack pose = c.pose();
        pose.pushPose();
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
        SpiritDiveAnimeRibbon.camera(pose);
        int i = el.ordinal();
        float age = c.age(), sw = Mth.clamp(c.limbSwingAmount(), 0f, 1f), vy = (float) c.player().getDeltaMovement().y;
        int rows = full ? sp.plumeRows() : Math.min(sp.plumeRows(), 1), layers = full ? sp.plumeLayers() : 1;
        boolean upgrade = st == Stage.UPGRADE;
        float lk = lenOf(character, full);
        for (int pass = 0; pass < 2; pass++) {
            VertexConsumer vc = c.buffers().getBuffer(pass == 0 ? SafeAdditive.emissive(PLUME[i]) : SafeAdditive.additive(PLUME_GLOW[i]));
            for (int r = 0; r < rows; r++) {
                float[] row = SpiritDiveAnimeGeometry.row(el, r);
                Shape shape = SpiritDiveAnimeGeometry.shape(row[16]);
                for (int s = 0; s < 2; s++) {
                    float side = s == 0 ? 1f : -1f;
                    if (half && side < 0f) continue;                                                      // Yuno's first dive: one wing, on the left
                    float pr = upgrade && side > 0f ? kept : progress;                                    // an upgrade keeps the left side, the right one opens
                    float open = SpiritDiveAnimeRules.openScale(pr), a = SpiritDiveAnimeRules.smooth(pr * 1.6f);
                    if (a <= 0.01f) continue;
                    for (int l = 0; l < layers; l++) {
                        float lenK = lk * open * (1f - 0.2f * l), wk = (character ? 1.1f : 0.95f) * (1f - 0.26f * l);
                        int segs = sp.plumeSegs();
                        SpiritDiveAnimeGeometry.plumeSpine(SpiritDiveAnimeRibbon.SPINE, segs, row, side, lenK, age + l * 6.5f, sw, vy);
                        int argb = pass == 0 ? SealDraw.argb(Math.min(1f, (0.9f - 0.1f * l) * a), 0xFFFFFF) : SealDraw.argb(Math.min(1f, (0.75f + 0.1f * l) * a * lit), l == 0 ? pal.glow() : pal.rim());
                        SpiritDiveAnimeRibbon.strip(pose, vc, segs, row[13] * wk, shape, row[15] + age * 0.05f, 0.45f, 0f, 0.3f, 1f, argb, 0.1f, FB);
                    }
                }
            }
        }
        pose.popPose();
    }

    /** The trails of the character tier: ribbons streaming back from the hands and the ankles, longer as the wearer moves (the wind streaks of the stills). */
    private static void trails(AuraContext c, Element el, Spec sp, Pal pal, float progress, float lit) {
        PoseStack pose = c.pose();
        pose.pushPose();
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
        SpiritDiveAnimeRibbon.camera(pose);
        int i = el.ordinal();
        float age = c.age(), sw = Mth.clamp(c.limbSwingAmount(), 0f, 1f), a = SpiritDiveAnimeRules.smooth((progress - 0.5f) / 0.5f);
        if (a <= 0.01f) { pose.popPose(); return; }
        for (int pass = 0; pass < 2; pass++) {
            VertexConsumer vc = c.buffers().getBuffer(pass == 0 ? SafeAdditive.emissive(LOCK[i]) : SafeAdditive.additive(LOCK_GLOW[i]));
            for (int r = 0; r < Math.min(2, SpiritDiveAnimeGeometry.TRAIL.length); r++) {
                float[] row = SpiritDiveAnimeGeometry.TRAIL[r];
                for (int s = 0; s < 2; s++) {
                    float side = s == 0 ? 1f : -1f;
                    SpiritDiveAnimeGeometry.trailSpine(SpiritDiveAnimeRibbon.SPINE, sp.trailSegs(), row, side, age, sw, r * 1.3f + s);
                    int argb = pass == 0 ? SealDraw.argb(0.8f * a, 0xFFFFFF) : SealDraw.argb(0.7f * a * lit, pal.glow());
                    SpiritDiveAnimeRibbon.strip(pose, vc, sp.trailSegs(), row[7], Shape.SPIKE, 0f, 0.85f, 0f, 0.2f, 1f, argb, 0.2f, FB);
                }
            }
        }
        pose.popPose();
    }

    // ------------------------------------------------------------------ the head: hair, cap, eyes, halo
    private static void head(AuraContext c, Element el, Stage st, Spec sp, boolean character, Pal pal, float progress, float lit, boolean half, boolean full, boolean helmet) {
        int i = el.ordinal();
        float age = c.age(), sw = Mth.clamp(c.limbSwingAmount(), 0f, 1f), vy = (float) c.player().getDeltaMovement().y;
        float head = SpiritDiveAnimeRules.partAlpha(progress, 2);
        if (head <= 0.01f && progress < 0.99f) return;
        if (sp.cap() && !helmet) SpiritDiveAnimeShell.hat(c, SafeAdditive.additive(HAIR[i]), 0.6f, 0xFFFFFF, Math.min(1f, 0.8f * head * lit));
        PoseStack pose = c.pose();
        pose.pushPose();
        applyPart(pose, c.model().head);
        SpiritDiveAnimeRibbon.camera(pose);
        if (sp.locks() > 0 && !helmet && (full || half)) {                                                   // the locks of hair: curved tapered spikes that lift on the entry and sway with the wearer
            float lift = SpiritDiveAnimeRules.hairLift(progress);
            int n = Math.min(sp.locks(), SpiritDiveAnimeGeometry.lockRows(el));
            for (int pass = 0; pass < 2; pass++) {
                VertexConsumer vc = c.buffers().getBuffer(pass == 0 ? SafeAdditive.emissive(LOCK[i]) : SafeAdditive.additive(LOCK_GLOW[i]));
                for (int l = 0; l < n; l++) {
                    float[] row = SpiritDiveAnimeGeometry.lockRow(el, l);
                    if (half && row[0] < 0f) continue;                                                    // the first dive of Wind: the left half of the hair
                    SpiritDiveAnimeGeometry.lockSpine(SpiritDiveAnimeRibbon.SPINE, sp.lockSegs(), row, character ? 1.15f : 0.9f, lift, age, sw, vy);
                    int argb = pass == 0 ? SealDraw.argb(Math.min(1f, 0.92f * head), 0xFFFFFF) : SealDraw.argb(Math.min(1f, 0.7f * head * lit), pal.hair());
                    SpiritDiveAnimeRibbon.strip(pose, vc, sp.lockSegs(), row[10] * (character ? 1.15f : 1f), SpiritDiveAnimeGeometry.shape(row[13]), row[12] + age * 0.07f, 0.8f, 0f, 0f, 1f, argb, 0.1f, FB);
                }
            }
        }
        if (sp.eyes() && !helmet) eyes(c, pose, pal, head * lit, half);
        halo(c, pose, el, character, pal, progress, lit, half, sp.halos());
        pose.popPose();
    }

    /** The eyes: a small slanted glow in the plane of the face (z = -4.08, the face of the head box is at -4), so they turn and pitch with the head; the half dive of Wind lights the left eye only. */
    private static void eyes(AuraContext c, PoseStack pose, Pal pal, float a, boolean half) {
        VertexConsumer eye = c.buffers().getBuffer(SafeAdditive.additive(EYE));
        int col = SealDraw.argb(Math.min(1f, a), pal.eye());
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? 1f : -1f;
            if (half && side < 0f) continue;
            SealDraw.plane(pose, eye, side * 2.0f, -3.5f, -4.08f, side * 1.3f, 0f, 0f, 0f, -0.75f, 0f, side < 0f, col, FB);
        }
        VertexConsumer glow = c.buffers().getBuffer(SafeAdditive.additive(GLOW));
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? 1f : -1f;
            if (half && side < 0f) continue;
            SealDraw.glow(pose, glow, side * 2.0f, -3.5f, -4.4f, 1.9f, 0f, SealDraw.argb(0.45f * Math.min(1f, a), pal.eye()), FB);
        }
    }

    /**
     * The halo: a thin spiky crown of light, the camera-facing sprite turning slowly behind the head, opening with the entry (a pulse of light as it appears); the character tier adds a larger second
     * crown turning the other way. In the half dive of Wind it is a half crown standing behind the head on the left (not turning: a fixed vertical half disc).
     */
    private static void halo(AuraContext c, PoseStack pose, Element el, boolean character, Pal pal, float progress, float lit, boolean half, int count) {
        if (count <= 0) return;
        int i = el.ordinal();
        float age = c.age(), open = SpiritDiveAnimeRules.openScale(Math.min(1f, Math.max(0f, (progress - 0.35f) / 0.65f)));
        float a = SpiritDiveAnimeRules.smooth((progress - 0.3f) / 0.5f);
        if (a <= 0.01f) return;
        float pulse = 1f + 0.8f * Math.max(0f, 1f - Math.abs(progress - 0.7f) * 6f);                          // the flash as it opens
        float hs = (character ? 11.5f : 8.5f) * open;
        VertexConsumer vc = c.buffers().getBuffer(SafeAdditive.additive(HALO[i]));
        if (half) {
            halfCrown(pose, vc, hs, SealDraw.argb(Math.min(1f, 0.9f * a * lit), pal.halo()));
            return;
        }
        SealDraw.glow(pose, vc, 0f, -9.5f, 2.5f, hs, age * 0.018f, SealDraw.argb(Math.min(1f, 0.9f * a * lit * pulse), pal.halo()), FB);
        if (count > 1) SealDraw.glow(pose, vc, 0f, -9.5f, 2.0f, hs * 1.35f, -age * 0.011f + 1f, SealDraw.argb(Math.min(1f, 0.55f * a * lit * pulse), pal.glow()), FB);
    }

    /** Half of the crown on the left side, standing behind the head (head frame px): the right half of the texture on the +x side, drawn from both sides. */
    private static void halfCrown(PoseStack pose, VertexConsumer vc, float hs, int argb) {
        float cy = -9.5f, z = 2.5f;
        PropDraw.quad(pose, vc, 0f, cy + hs, z, hs, cy + hs, z, hs, cy - hs, z, 0f, cy - hs, z, 0.5f, 0f, 1f, 1f, argb, FB);
        PropDraw.quad(pose, vc, hs, cy + hs, z, 0f, cy + hs, z, 0f, cy - hs, z, hs, cy - hs, z, 1f, 0f, 0.5f, 1f, argb, FB);
    }

    // ------------------------------------------------------------------ extras: the entry, the ground ring, the circle behind the back
    private static void extras(AuraContext c, Element el, Stage st, Spec sp, boolean character, Pal pal, float progress, float pulse, boolean half, Lod lod) {
        PoseStack pose = c.pose();
        int i = el.ordinal();
        float t = c.t(), age = c.age(), a = SpiritDiveAnimeRules.smooth(progress);
        SealDraw.camera(pose);
        if (sp.ground()) {                                                                                  // the ring of the element on the ground, turning slowly
            pose.pushPose();
            pose.translate(0f, 1.478f, 0f);
            SealDraw.flat(pose, c.buffers().getBuffer(SafeAdditive.additive(RING[i])), 0.95f * (0.5f + 0.5f * a), age * (el == Element.EARTH ? 0.5f : 0.9f), SealDraw.argb(0.5f * a * pulse, pal.glow()), FB);
            pose.popPose();
        }
        if (lod == Lod.FULL && SpiritDivePieces.full(st) && sp.backdrop()) backSigil(c, el, a, pulse, pal);
        if (half && sp.ground()) wristSigil(c, el, a, pulse, pal);
        if (lod != Lod.FULL) return;
        float fl = SpiritDiveAnimeRules.flash(t);
        if (fl > 0f && t < 6f && st != Stage.UPGRADE) {                                                     // the flash at the chest: a star-burst
            SealDraw.glow(pose, c.buffers().getBuffer(SafeAdditive.additive(BURST)), 0f, 1.5f - 1.05f, 0f, 0.5f + 1.5f * (1f - fl), age * 0.05f, SealDraw.argb(fl * 0.95f, 0xFFFFFF), FB);
        }
        float rp = SpiritDiveAnimeRules.ringPulse(t);
        if (rp > 0f && st != Stage.UPGRADE) {                                                               // the ring pulse of the halo opening: a ring expanding from the feet
            pose.pushPose();
            pose.translate(0f, 1.47f, 0f);
            SealDraw.flat(pose, c.buffers().getBuffer(SafeAdditive.additive(RING[i])), 0.5f + 3.4f * rp, t * 3f, SealDraw.argb((1f - rp) * 0.9f, pal.rim()), FB);
            pose.popPose();
        }
        float bu = SpiritDiveAnimeRules.burst(t);
        if (bu > 0f && st != Stage.UPGRADE) {                                                               // the burst of the element: two crossed columns of wind / flame / water / dust at the feet
            VertexConsumer vc = c.buffers().getBuffer(SafeAdditive.additive(PILLAR));
            float h = 1.9f * (0.4f + 0.6f * bu);
            for (int k = 0; k < 2; k++) {
                float ang = k * Mth.HALF_PI + age * 0.05f, ux = Mth.cos(ang) * 0.5f, uz = Mth.sin(ang) * 0.5f;
                int col = SealDraw.argb(bu * 0.75f, pal.glow());
                SealDraw.plane(pose, vc, 0f, 1.5f - h, 0f, ux, 0f, uz, 0f, -h, 0f, false, col, FB);
                SealDraw.plane(pose, vc, 0f, 1.5f - h, 0f, ux, 0f, uz, 0f, -h, 0f, true, col, FB);
            }
        }
    }

    /** The circle of the element standing behind the back, turning (the circle of connected crosses of Yuno's complete form; Fuegoleon's sun of flames, Noelle's ripples, a rune ring for Earth). */
    private static void backSigil(AuraContext c, Element el, float a, float pulse, Pal pal) {
        PoseStack pose = c.pose();
        float spin = c.age() * 0.012f, r = 0.95f * (0.7f + 0.3f * a);
        float cs = Mth.cos(spin) * r, sn = Mth.sin(spin) * r;
        VertexConsumer vc = c.buffers().getBuffer(SafeAdditive.additive(SIGIL[el.ordinal()]));
        int col = SealDraw.argb(0.45f * a * pulse, pal.glow());
        float cy = 1.5f - 1.15f, cz = 0.58f;
        SealDraw.plane(pose, vc, 0f, cy, cz, cs, -sn, 0f, -sn, -cs, 0f, false, col, FB);
        SealDraw.plane(pose, vc, 0f, cy, cz, cs, -sn, 0f, -sn, -cs, 0f, true, col, FB);
    }

    /** The first dive of Wind: a small circle of crosses round the LEFT wrist. */
    private static void wristSigil(AuraContext c, Element el, float a, float pulse, Pal pal) {
        if (el != Element.WIND) return;
        PoseStack pose = c.pose();
        pose.pushPose();
        applyPart(pose, c.model().leftArm);
        SealDraw.camera(pose);
        SealDraw.glow(pose, c.buffers().getBuffer(SafeAdditive.additive(SIGIL[0])), 0f, 10.2f, 0f, 4.6f, c.age() * 0.06f, SealDraw.argb(0.75f * a * pulse, pal.glow()), FB);
        pose.popPose();
    }

    // ------------------------------------------------------------------ the spirit behind the wearer
    /** The faint silhouette of the spirit (the character tier, close up): the spirit's own geo model, additive, a quarter of its brightness, a little larger and behind the wearer. */
    private static void backdrop(AuraContext c, Element el, float progress, float pulse, Pal pal) {
        float a = SpiritDiveAnimeRules.smooth((progress - 0.4f) / 0.6f);
        if (a <= 0.01f) return;
        PoseStack pose = c.pose();
        float[] p = SPIRIT_POSE[el.ordinal()];
        GeoSpec[] specs = SPIRIT[el.ordinal()];
        pose.pushPose();
        pose.translate(0f, 1.5f - p[3] / 16f, p[2] / 16f);                                                  // the feet line, a little behind
        pose.scale(p[0], p[0], p[0]);
        pose.translate(0f, -1.5f, 0f);
        int argb = SealDraw.argb(0.25f * a * (0.85f + 0.15f * pulse), pal.rim());
        float seconds = c.age() / 20f;
        for (int k = 0; k < specs.length; k++)
            GeoDraw.paint(pose, c.buffers(), specs[k], null, seconds, GeoDraw.Space.PLAYER, GeoDraw.Layer.ADDITIVE, FB, argb, 0);
        pose.popPose();
    }

    private static boolean firstPersonSelf(AuraContext c) {
        try {
            Minecraft mc = Minecraft.getInstance();
            return c.player() == mc.player && mc.options.getCameraType().isFirstPerson();
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
