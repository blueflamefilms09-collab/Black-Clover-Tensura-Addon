package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.book.SpiritDiveDjinnPieces;
import com.newuniverse.nusmp.book.SpiritDiveDjinnPieces.Def;
import com.newuniverse.nusmp.book.SpiritDivePieces;
import com.newuniverse.nusmp.book.SpiritDivePieces.Anchor;
import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;
import com.newuniverse.nusmp.book.SpiritDiveTier;
import com.newuniverse.nusmp.book.SpiritDiveTier.Grade;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.SealDraw;
import com.newuniverse.nusmp.perf.PerfProbe;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spirit Dive, Djinn-equip style (the {@code DJINN} look, the default of {@link SpiritDiveLook#look()}): the Spirit Dive of Wind, Fire, Water and Earth drawn as a FULL SET OF PLATES on the player's own
 * body, forged on piece by piece, the way the Djinn Full Equip is (client.relics.djinn.DjinnArmorLayer). The pieces are listed by {@link SpiritDiveDjinnPieces}; each is a geo model with an engraved
 * glow map ({@code assets/nusmp/geo/entity/sdj_<element>_<s|c>_arm_<piece>}, made by tools/gen_spirit_dive_djinn.py), anchored on the model part it was built for (so it follows every crouch, swing
 * and swim) and forged in / shattered out by {@link EquipAnimation}, the Djinn's own arithmetic: staggered by the piece's delay, flying in from its own side while it fades up and grows from 85 to
 * 100 percent over 8 ticks, shattering out over the last 18 ticks of the lease. Nothing is put in an armour slot.
 *
 * <p>Layers, in this order: (1) a veins pass, the player's model drawn once with the additive vein texture of the element over the parts that are armoured (the Djinn's body art: glow veins on the
 * skin that show between the plates; never the head, the face stays the player's own) and a faint haze; (2) the pieces (cutout when fully on, translucent while forging; the glow map additive and full
 * bright); (3) the extras, restrained as the Djinn's are: a ring of light opening at the feet while the set forges (white and the element's colour, a third larger one for the character tier), one
 * flare at the chest at the first tick, a slow ring on the ground and a few motes rising off the plates.</p>
 *
 * <p>Cost, bounded by {@link SpiritDiveDjinnPieces}: the pieces are at most the Djinn Full Equip's vertex budget (the character set its largest set, the standard set its smallest), the extras at most
 * {@link SpiritDiveDjinnPieces#extrasQuads}; up to 24 blocks everything, up to 48 blocks no detail pieces and no extras but the ring and a few motes, up to 64 blocks the silhouette pieces without their glow,
 * nothing beyond; at most {@link SpiritDiveTier#CHARACTER_PER_FRAME} dives per frame draw the character set ({@link SpiritDiveFrame}). No allocation per frame: the scratch quaternion, the geo specs and the
 * texture locations are statics.</p>
 */
public final class SpiritDiveDjinn {
    private SpiritDiveDjinn() {}

    private static final int FB = PropDraw.FULL_BRIGHT;
    private static final ResourceLocation GLOW = AuraRender.tex("particle/glow");
    private static final ResourceLocation[] RING = {AuraRender.tex("particle/air_ring"), AuraRender.tex("particle/waterfx_ripple"), AuraRender.tex("particle/flamefx_ring"),
            AuraRender.tex("particle/earthfam_rock_ring")};                                                 // by element ordinal (WIND, WATER, FIRE, EARTH)
    private static final ResourceLocation[] VEINS = {AuraRender.tex("aura/spirit2_wind_veins"), AuraRender.tex("aura/spirit2_water_veins"), AuraRender.tex("aura/spirit2_fire_veins"),
            AuraRender.tex("aura/spirit2_earth_veins")};
    private static final ResourceLocation[] HAZE = {AuraRender.tex("aura/spirit_wind_haze"), AuraRender.tex("aura/spirit_water_haze"), AuraRender.tex("aura/spirit_fire_haze"),
            AuraRender.tex("aura/spirit_earth_haze")};
    private static final Quaternionf ROT = new Quaternionf();                                               // scratch: the render thread is the only user
    private static final PerfProbe.Section SECTION = PerfProbe.section("c.spiritdive.djinn");                // the Djinn-style dive of one player, timed (/multiverse perf)

    private static final ModelPart[] PARTS = new ModelPart[12];
    private static final boolean[] WAS = new boolean[12];
    private static final Map<String, GeoSpec> SPECS = new HashMap<>();

    static GeoSpec specOf(Element e, boolean character, Def d) {
        String id = SpiritDiveDjinnPieces.modelId(e, character, d.name());
        GeoSpec s = SPECS.get(id);
        if (s == null) {                                                                                     // once per piece, ever (the table is small and fixed)
            s = GeoSpec.of(SpiritDiveDjinnPieces.key(e, character), SpiritDiveDjinnPieces.modelName(d.name()));
            SPECS.put(id, s);
        }
        return s;
    }

    // ------------------------------------------------------------------ the painter
    /**
     * Draws the dive of {@code el} at {@code st}; called by {@link SpiritDiveAura} for the DJINN look. The grade is the tier the server sent; the character set is drawn when the grade belongs to the
     * element, the viewer allows it, the dive is close enough and the frame still has a character slot.
     */
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
        boolean wantsCharacter = SpiritDiveDjinnPieces.character(el, grade) && SpiritDiveLook.characterEffects() && lod != Lod.FAR;
        boolean character = wantsCharacter && SpiritDiveFrame.admitCharacter();
        Grade use = character ? SpiritDiveTier.effective(el, grade) : Grade.STANDARD;
        float flare = DiveCast.casting(c.player().getId()) ? 1f : 0f;
        float f = st == Stage.UPGRADE ? EquipAnimation.alpha(EquipAnimation.assembled(1e4f, c.left(), 0f)) : WaterAuraRig.forge(c, 0f);          // the steady strength: an upgrade keeps what is on
        if (lod == Lod.FULL) veins(c, el, st, grade, f, pulse);
        float glowK = (0.75f * pulse + 0.35f) * (1f + 0.4f * flare);
        pieces(c, el, st, grade, character, lod, SpiritDiveDjinnPieces.glowRgb(el, use), glowK);
        extras(c, el, st, use, character, lod, f, pulse);
    }

    // ------------------------------------------------------------------ the veins on the skin
    private static void veins(AuraContext c, Element el, Stage st, Grade grade, float k, float pulse) {
        int mask = SpiritDiveDjinnPieces.veinParts(el, st, grade);
        if (mask == 0 || k <= 0.01f) return;
        float age = c.age();
        PlayerModel<AbstractClientPlayer> m = c.model();
        ModelPart[] all = PARTS;                                                                             // scratch arrays: the render thread is the only user and the method does not re-enter
        all[0] = m.head; all[1] = m.hat; all[2] = m.body; all[3] = m.jacket; all[4] = m.rightArm; all[5] = m.rightSleeve;
        all[6] = m.leftArm; all[7] = m.leftSleeve; all[8] = m.rightLeg; all[9] = m.rightPants; all[10] = m.leftLeg; all[11] = m.leftPants;
        boolean[] was = WAS;
        for (int i = 0; i < all.length; i++) was[i] = all[i].visible;
        try {
            all[0].visible = false;                                                                          // the face stays the player's own
            all[1].visible = false;
            all[2].visible = all[3].visible = (mask & SpiritDivePieces.SHELL_BODY) != 0;
            all[4].visible = all[5].visible = (mask & SpiritDivePieces.SHELL_ARM_R) != 0;
            all[6].visible = all[7].visible = (mask & SpiritDivePieces.SHELL_ARM_L) != 0;
            all[8].visible = all[9].visible = (mask & SpiritDivePieces.SHELL_LEG_R) != 0;
            all[10].visible = all[11].visible = (mask & SpiritDivePieces.SHELL_LEG_L) != 0;
            float g = 1.05f + 0.01f * Mth.sin(age * 0.11f + 1f);
            AuraRender.modelBright(c, AuraRender.additive(VEINS[el.ordinal()]), g, 1.03f, g, AuraRender.alpha(0xFFFFFFFF, 0.5f * pulse * k));
            AuraRender.modelBright(c, AuraRender.additive(HAZE[el.ordinal()]), g + 0.03f, 1.05f, g + 0.03f, AuraRender.alpha(0xFFFFFFFF, 0.14f * pulse * k));
        } finally {
            for (int i = 0; i < all.length; i++) all[i].visible = was[i];
        }
    }

    // ------------------------------------------------------------------ the pieces (the rig of the Djinn full equip, with the "already on" of an upgrade)
    private static ModelPart part(PlayerModel<AbstractClientPlayer> m, Anchor a) {
        return switch (a) {
            case HEAD -> m.head;
            case BODY -> m.body;
            case ARM_R -> m.rightArm;
            case ARM_L -> m.leftArm;
            case LEG_R -> m.rightLeg;
            case LEG_L -> m.leftLeg;
        };
    }

    private static boolean firstPersonSelf(AuraContext c) {
        try {
            Minecraft mc = Minecraft.getInstance();
            return c.player() == mc.player && mc.options.getCameraType().isFirstPerson();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /**
     * Draws the pieces: each flies in from its own side while it fades up and grows from 85 to 100 percent over 8 ticks, staggered by its delay, and shatters out over the last 18 ticks
     * ({@link EquipAnimation}). A piece that was already on when a half dive became full does not forge again ({@link SpiritDiveDjinnPieces#alreadyOn}). Slim arms (Alex): arm pieces are narrowed
     * to 80 percent about the arm's own pivot. At the far level of detail only the silhouette pieces are drawn, without their glow pass.
     */
    private static void pieces(AuraContext c, Element el, Stage st, Grade grade, boolean character, Lod lod, int glowRgb, float glowK) {
        boolean self1p = firstPersonSelf(c);
        boolean slim = WaterAuraRig.slim(c.model());
        boolean air = !c.player().onGround() && !c.player().isInWater();
        PoseStack pose = c.pose();
        int lit = SealDraw.lift(c.light(), 6);
        List<Def> list = SpiritDiveDjinnPieces.pieces(el, st, grade, character);
        for (int pi = 0, pn = list.size(); pi < pn; pi++) {
            Def p = list.get(pi);
            if (!SpiritDiveDjinnPieces.drawn(p, lod) || (p.head() && self1p)) continue;
            boolean already = SpiritDiveDjinnPieces.alreadyOn(el, st, grade, character, p.name());
            float a = EquipAnimation.assembled(already ? 1e4f : c.t(), c.left(), p.delay());
            if (a == 0f) continue;
            ModelPart part = part(c.model(), p.anchor());
            PartPose rest = part.getInitialPose();
            pose.pushPose();
            pose.translate(part.x / 16f, part.y / 16f, part.z / 16f);                                       // the part's own pose (ModelPart.translateAndRotate without its scale)
            if (part.xRot != 0f || part.yRot != 0f || part.zRot != 0f) pose.mulPose(ROT.rotationZYX(part.zRot, part.yRot, part.xRot));
            pose.scale(1f / 16f, 1f / 16f, 1f / 16f);                                                        // model pixels: what EquipAnimation.begin's numbers are in
            float alpha = EquipAnimation.begin(pose, a, p.dx(), p.dy(), p.dz());
            pose.scale(16f, 16f, 16f);
            if (slim && (p.anchor() == Anchor.ARM_R || p.anchor() == Anchor.ARM_L)) pose.scale(0.8f, 1f, 1f);
            pose.translate(-rest.x / 16f, -rest.y / 16f, -rest.z / 16f);                                     // the model is authored in whole-body coordinates
            String clip = p.clip().isEmpty() ? null : p.clip();
            float seconds = c.age() / 20f * (p.flap() && air ? 2.3f : 1f) + (p.name().hashCode() & 7) * 0.11f;
            int white = ((int) (Mth.clamp(alpha, 0f, 1f) * 255f + 0.5f) << 24) | 0xFFFFFF;
            float g = lod == Lod.FAR ? 0f : alpha * glowK * p.glow();                                         // no glow pass at the far level (and none for a piece without glow)
            int glow = ((int) (Mth.clamp(g, 0f, 1f) * 255f + 0.5f) << 24) | (glowRgb & 0xFFFFFF);
            boolean translucent = p.glass() || a < 1f;
            GeoDraw.paint(pose, c.buffers(), specOf(el, character, p), clip, seconds, GeoDraw.Space.PLAYER, translucent ? GeoDraw.Layer.TRANSLUCENT : GeoDraw.Layer.CUTOUT,
                    translucent ? lit : c.light(), white, glow);
            pose.popPose();
        }
    }

    // ------------------------------------------------------------------ the extras: restrained, as the Djinn's are
    private static void extras(AuraContext c, Element el, Stage st, Grade use, boolean character, Lod lod, float f, float pulse) {
        if (f <= 0.01f) return;
        float age = c.age(), t = c.t();
        PoseStack pose = c.pose();
        SealDraw.camera(pose);
        int accent = SpiritDiveDjinnPieces.glowRgb(el, use), ringCol = SpiritDiveDjinnPieces.ringRgb(el, use);
        VertexConsumer ring = c.buffers().getBuffer(AuraRender.additive(RING[el.ordinal()]));
        pose.pushPose();                                                                                     // the flash of the forge: rings of light opening at the feet, then a slow ring while the set stays on
        pose.translate(0f, 1.475f, 0f);
        float flash = lod == Lod.FULL ? SpiritDiveDjinnPieces.flash(t) : 0f;
        if (flash > 0f) {
            float k = 1f - flash, big = SpiritDiveDjinnPieces.bigForge(character);
            SealDraw.flat(pose, ring, 0.5f + 1.7f * k, age * 4f, SealDraw.argb(flash * 0.9f, 0xFFFFFF), FB);
            SealDraw.flat(pose, ring, 0.4f + 1.3f * k, -age * 3f, SealDraw.argb(flash * 0.9f, accent), FB);
            if (character) SealDraw.flat(pose, ring, (0.6f + 2.4f * k) * big, age * 2f, SealDraw.argb(flash * 0.7f, ringCol), FB);
        }
        SealDraw.flat(pose, ring, 0.95f, age * 1.4f, SealDraw.argb(0.4f * f * pulse, accent), FB);
        pose.popPose();
        if (lod == Lod.FULL && t < 12f) {                                                                    // one flare at the chest on the first ticks
            VertexConsumer glow = c.buffers().getBuffer(AuraRender.additive(GLOW));
            float k = t / 12f;
            SealDraw.glow(pose, glow, 0f, 1.05f, 0f, (0.35f + 0.9f * k) * (character ? 1.3f : 1f), age, SealDraw.argb((1f - k) * 0.8f, accent), FB);
        }
        int n = SpiritDiveDjinnPieces.motes(character, lod);
        if (n == 0) return;
        VertexConsumer mote = c.buffers().getBuffer(AuraRender.additive(GLOW));
        for (int i = 0; i < n; i++) {
            float ph = frac(age * 0.014f * (1f + 0.3f * (i % 3)) + i * 0.41f), ang = i * 2.399f + age * 0.03f;
            float r = 0.5f + 0.2f * Mth.sin(i * 1.7f);
            SealDraw.glow(pose, mote, Mth.cos(ang) * r, 1.4f - ph * 2.0f, Mth.sin(ang) * r, 0.05f + 0.04f * (1f - ph), ang,
                    SealDraw.argb(Mth.sin(ph * Mth.PI) * f * 0.9f, i % 2 == 0 ? ringCol : accent), FB);
        }
    }

    private static float frac(float x) { return x - (float) Math.floor(x); }
}
