package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.book.SpiritDivePieces;
import com.newuniverse.nusmp.book.SpiritDivePieces.Anchor;
import com.newuniverse.nusmp.book.SpiritDivePieces.Def;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;

/**
 * 1.99.0 Great Spirits: the Spirit Dive (Spirit Assimilation) of Wind, Water, Fire and Earth as a render layer, {@code Aura.SPIRIT_DIVE}. The mage merges the spirit into the body; the body is
 * altered and cloaked in dense elemental mana (the owner's canon). The server keeps the dive alive with the same lease the armours use ({@code book.SpiritDiveForm}: 60 more ticks every second,
 * 20 more ticks when it ends, so the pieces shatter out inside the 18 tick window of {@link EquipAnimation}); the STYLE number says which dive this is:
 * {@link SpiritDivePieces#style} = element + stage (HALF, FULL, UPGRADE).
 * <ul>
 *   <li><b>Wind</b>, first dive: only the LEFT half of the body transforms (one honeycomb wind wing, half a crown, a halo ring round the wrist, a half mantle, sleeve and greave). The full
 *       dive: two wings, the full crown, rings on both wrists, symmetrical. UPGRADE: the left half stays on and the missing right half is forged in, piece by piece, by EquipAnimation.</li>
 *   <li><b>Water</b>: the old Valkyrie armour pieces (key water, tinted), and at the full stage the Saint Valkyrie Dress (feathered wings, a halo, feathered forearms, a gold sash,
 *       white - gold - blue).</li>
 *   <li><b>Fire</b>: arms cloaked in fire mana with draconic scales, claws and fiery wings (small ones, then the full pair). <b>Earth</b>: generic mossy stone plates.</li>
 * </ul>
 * Passes (as in the other armours): 1 a translucent mana skin over the parts of the body that transformed (the head is left alone, the face stays the player's own); 2 the pieces, each its
 * own geo model anchored on the model part it was built for, forged in / shattered out by {@link EquipAnimation}; 3 their glow maps; 4 an additive haze; 5 extras: a ring on the ground and
 * a small, capped stream of motes flowing INTO the body (the passive absorption of ambient mana: at most {@link #MOTES} sprites and {@link #BITS} rising bits, none beyond 24 blocks).
 * The pieces come from tools/gen_spirit_dive_geo.py (and tools/gen_water_aura_geo.py for the Valkyrie set); the lists per element and stage are book.SpiritDivePieces.
 *
 * <p><b>The remade look (Spirit Dive remake, the default).</b> The same layer, a lighter and more elemental dive in two tiers ({@link SpiritDiveTier}): the STANDARD tier for every mage, and the CHARACTER
 * tier (Yuno on Wind, Fuegoleon on Fire, Noelle on Water: the server puts the grade in the style number, {@link SpiritDivePieces#grade}) with the character's own pieces and effects ({@link SpiritDiveFx}:
 * columns of light, helices, a sigil on the ground and behind the back, orbiting rune rings, a shockwave and a pillar of light on entry, glowing eyes and hands) in the colours of the character. Every
 * count comes from {@link SpiritDiveTier#budget}; the level of detail follows the distance ({@link SpiritDiveTier#lod}); at most {@link SpiritDiveTier#CHARACTER_PER_FRAME} dives a frame draw the character
 * tier ({@link SpiritDiveFrame}). The client option {@code spiritDive.classicLook} ({@link SpiritDiveLook}) brings back the 1.99 look above, unchanged. Fuegoleon's molten arm is not a piece of the dive: the server keeps
 * his arm lease alive during the Fire dive and {@link FuegoleonArmAura} draws it, burning highest then.</p>
 */
public final class SpiritDiveAura {
    private SpiritDiveAura() {}

    /** The most mana motes streaming into the body, and the most rising element bits: the whole extras pass is at most this many sprites. */
    public static final int MOTES = 10, BITS = 8;

    private static final int FB = PropDraw.FULL_BRIGHT;
    private static final double DETAIL2 = 24.0 * 24.0, FAR2 = 64.0 * 64.0;
    private static final ResourceLocation GLOW = AuraRender.tex("particle/glow");
    private static final ResourceLocation RING_WIND = AuraRender.tex("particle/air_ring"), RING_WATER = AuraRender.tex("particle/waterfx_ripple"),
            RING_FIRE = AuraRender.tex("particle/flamefx_ring"), RING_EARTH = AuraRender.tex("particle/earthfam_rock_ring");
    private static final ResourceLocation BIT_FIRE = AuraRender.tex("particle/flamefx_ember"), BIT_WATER = AuraRender.tex("particle/waterfx_drop"),
            BIT_EARTH = AuraRender.tex("particle/earthfam_rock_dust");

    /** The vein layer of the remade look, by element ordinal (WIND, WATER, FIRE, EARTH): additive, drawn over the haze. */
    private static final ResourceLocation[] VEINS = {AuraRender.tex("aura/spirit2_wind_veins"), AuraRender.tex("aura/spirit2_water_veins"), AuraRender.tex("aura/spirit2_fire_veins"),
            AuraRender.tex("aura/spirit2_earth_veins")};
    /** The mana skin, its glow map and the haze by element ordinal (WIND, WATER, FIRE, EARTH); the remade fire dive has its own warmer skin (SHELL2). */
    private static final ResourceLocation[] SHELL = {AuraRender.tex("aura/spirit_wind_shell"), AuraRender.tex("aura/spirit_water_shell"), AuraRender.tex("aura/spirit_fire_shell"),
            AuraRender.tex("aura/spirit_earth_shell")},
            SHELL_GLOW = {AuraRender.tex("aura/spirit_wind_shell_glow"), AuraRender.tex("aura/spirit_water_shell_glow"), AuraRender.tex("aura/spirit_fire_shell_glow"),
                    AuraRender.tex("aura/spirit_earth_shell_glow")},
            HAZE = {AuraRender.tex("aura/spirit_wind_haze"), AuraRender.tex("aura/spirit_water_haze"), AuraRender.tex("aura/spirit_fire_haze"), AuraRender.tex("aura/spirit_earth_haze")},
            SHELL2 = {SHELL[0], SHELL[1], AuraRender.tex("aura/spirit2_fire_shell"), SHELL[3]},
            SHELL2_GLOW = {SHELL_GLOW[0], SHELL_GLOW[1], AuraRender.tex("aura/spirit2_fire_shell_glow"), SHELL_GLOW[3]};
    private static final Quaternionf ROT = new Quaternionf();                                       // scratch: the render thread is the only user
    private static final PerfProbe.Section SECTION = PerfProbe.section("c.spiritdive.paint");        // the remade look, timed (/multiverse perf)

    private static final ModelPart[] PARTS = new ModelPart[12];
    private static final boolean[] WAS = new boolean[12];
    private static final Map<String, GeoSpec> SPECS = new HashMap<>();
    private static final Map<String, Def> DEFS = new HashMap<>();

    static {
        for (Def d : SpiritDivePieces.all()) DEFS.put(d.id(), d);
    }

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        PlayerAuraClient.register(Aura.SPIRIT_DIVE, SpiritDiveAura::paint);
    }

    // ------------------------------------------------------------------ the colours of each dive
    private static int glowRgb(Element e, Stage s) {
        return switch (e) {
            case WIND -> 0x98FFD0;
            case WATER -> SpiritDivePieces.saint(e, s) ? 0xFFE49A : 0xCFF8FF;
            case FIRE -> 0xFF8A2A;
            case EARTH -> 0xFFB040;
        };
    }

    private static int tintRgb(Element e, Stage s) {
        return e == Element.WATER ? (SpiritDivePieces.saint(e, s) ? 0xE9F0FF : 0xA8C8FF) : 0xFFFFFF;
    }

    private static int moteRgb(Element e, Stage s) {
        return switch (e) {
            case WIND -> 0xB8FFE0;
            case WATER -> SpiritDivePieces.saint(e, s) ? 0xFFEFB8 : 0xBFEFFF;
            case FIRE -> 0xFFB050;
            case EARTH -> 0xE8D8A0;
        };
    }


    // ------------------------------------------------------------------ the painter
    private static void paint(AuraContext c) {
        if (c.left() <= 0.01f) return;                                                            // (no fade test: an upgrade restarts the clock, and the pieces fade by themselves)
        int style = c.style();
        Element el = SpiritDivePieces.element(style);
        Stage st = SpiritDivePieces.stage(style);
        float age = c.age(), pulse = 0.8f + 0.2f * Mth.sin(age * 0.17f) + 0.08f * Mth.sin(age * 0.53f);
        if (!SpiritDiveLook.classic()) { remade(c, el, st, SpiritDivePieces.grade(style), pulse); return; }
        // the classic look (the 1.99 dive; the client option spiritDive.classicLook)
        shell(c, el, st, pulse, false, false, false);
        drawPieces(c, el, st, Grade.STANDARD, false, Lod.FULL, glowRgb(el, st), 0.75f * pulse + 0.35f, tintRgb(el, st));
        shell(c, el, st, pulse, true, false, false);
        extras(c, el, st, pulse);
    }

    /**
     * The remade look (default): the mana skin, the pieces of the tier (standard: the light set; the mage whose grimoire is Yuno's, Fuegoleon's or Noelle's: the character set), a veined haze, and
     * the effects of {@link SpiritDiveFx}, all at the level of detail of the distance ({@link SpiritDiveTier#lod}). At most {@link SpiritDiveTier#CHARACTER_PER_FRAME} dives per frame draw the
     * character tier (the rest draw the standard tier); beyond 24 blocks the small detail goes, beyond 48 only a ring is left, beyond 64 nothing.
     */
    private static void remade(AuraContext c, Element el, Stage st, Grade grade, float pulse) {
        Lod lod = SpiritDiveTier.lod(dist2(c));
        if (lod == Lod.CULLED) return;
        long t0 = PerfProbe.begin();
        try {
            remadeLod(c, el, st, grade, pulse, lod);
        } finally {
            PerfProbe.end(SECTION, t0);
        }
    }

    private static void remadeLod(AuraContext c, Element el, Stage st, Grade grade, float pulse, Lod lod) {
        boolean self1p = firstPersonSelf(c);
        boolean character = SpiritDiveTier.character(el, grade) && SpiritDiveLook.characterEffects() && lod != Lod.FAR && SpiritDiveFrame.admitCharacter();
        Grade use = character ? SpiritDiveTier.effective(el, grade) : Grade.STANDARD;
        float flare = DiveCast.casting(c.player().getId()) ? 1f : 0f;
        float f = st == Stage.UPGRADE ? EquipAnimation.alpha(EquipAnimation.assembled(1e4f, c.left(), 0f)) : WaterAuraRig.forge(c, 0f);
        boolean saint = SpiritDivePieces.saint(el, st);
        if (lod != Lod.FAR) {
            shell(c, el, st, pulse, false, true, false);
            drawPieces(c, el, st, use, true, lod, SpiritDiveTier.glow(el, use, saint), (0.75f * pulse + 0.35f) * (el == Element.FIRE ? 0.75f : 1f) * (1f + 0.4f * flare), tintRgb(el, st));
            shell(c, el, st, pulse, true, true, lod == Lod.FULL);
        }
        SpiritDiveFx.paint(c, el, st, use, lod, character, f, pulse, self1p, flare);
    }

    // ------------------------------------------------------------------ the mana skin and the haze
    /**
     * The translucent skin of mana over the parts that transformed (and, with {@code haze}, the additive haze over them). In an UPGRADE the parts of the half dive stay on at full strength
     * (they only shatter when the dive ends) and the newly transformed parts fade in with the forge.
     */
    private static void shell(AuraContext c, Element el, Stage st, float pulse, boolean haze, boolean remade, boolean veins) {
        int parts = SpiritDivePieces.shellParts(el, st);
        int kept = st == Stage.UPGRADE ? SpiritDivePieces.shellParts(el, Stage.HALF) & parts : 0;
        float forge = WaterAuraRig.forge(c, 0f);
        float steady = st == Stage.UPGRADE ? EquipAnimation.alpha(EquipAnimation.assembled(1e4f, c.left(), 0f)) : forge;
        shellParts(c, el, parts & ~kept, forge, pulse, haze, remade, veins);
        if (kept != 0) shellParts(c, el, kept, steady, pulse, haze, remade, veins);
    }

    private static void shellParts(AuraContext c, Element el, int mask, float k, float pulse, boolean haze, boolean remade, boolean veins) {
        if (mask == 0 || k <= 0.01f) return;
        float age = c.age();
        PlayerModel<AbstractClientPlayer> m = c.model();
        ModelPart[] all = PARTS;                                                              // scratch arrays: the render thread is the only user and the method does not re-enter
        all[0] = m.head; all[1] = m.hat; all[2] = m.body; all[3] = m.jacket; all[4] = m.rightArm; all[5] = m.rightSleeve;
        all[6] = m.leftArm; all[7] = m.leftSleeve; all[8] = m.rightLeg; all[9] = m.rightPants; all[10] = m.leftLeg; all[11] = m.leftPants;
        boolean[] was = WAS;
        for (int i = 0; i < all.length; i++) was[i] = all[i].visible;
        try {
            all[0].visible = false;                                                           // the face stays the player's own
            all[1].visible = false;
            all[2].visible = all[3].visible = (mask & SpiritDivePieces.SHELL_BODY) != 0;
            all[4].visible = all[5].visible = (mask & SpiritDivePieces.SHELL_ARM_R) != 0;
            all[6].visible = all[7].visible = (mask & SpiritDivePieces.SHELL_ARM_L) != 0;
            all[8].visible = all[9].visible = (mask & SpiritDivePieces.SHELL_LEG_R) != 0;
            all[10].visible = all[11].visible = (mask & SpiritDivePieces.SHELL_LEG_L) != 0;
            if (!haze) {
                AuraRender.model(c, AuraRender.translucent((remade ? SHELL2 : SHELL)[el.ordinal()]), 1.04f, 1.025f, 1.04f, AuraRender.alpha(0xFFFFFFFF, 0.6f * k));
                AuraRender.modelBright(c, AuraRender.additive((remade ? SHELL2_GLOW : SHELL_GLOW)[el.ordinal()]), 1.045f, 1.03f, 1.045f, AuraRender.alpha(0xFFFFFFFF, 0.55f * pulse * k));
            } else {
                float g = 1.085f + 0.012f * Mth.sin(age * 0.15f);
                AuraRender.modelBright(c, AuraRender.additive(HAZE[el.ordinal()]), g, 1.05f, g, AuraRender.alpha(0xFFFFFFFF, 0.2f * pulse * k));
                if (veins) {                                                                       // the remade look: stream lines / lava / currents / crystal veins running over the body
                    float gv = 1.06f + 0.01f * Mth.sin(age * 0.11f + 1f);
                    AuraRender.modelBright(c, AuraRender.additive(VEINS[el.ordinal()]), gv, 1.04f, gv, AuraRender.alpha(0xFFFFFFFF, 0.55f * pulse * k));
                }
            }
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

    private static double dist2(AuraContext c) {
        try {
            var cam = Minecraft.getInstance().gameRenderer.getMainCamera();
            return cam.getPosition().distanceToSqr(c.player().getX(), c.player().getY(), c.player().getZ());
        } catch (RuntimeException ex) {
            return 0.0;
        }
    }

    private static boolean firstPersonSelf(AuraContext c) {
        try {
            Minecraft mc = Minecraft.getInstance();
            return c.player() == mc.player && mc.options.getCameraType().isFirstPerson();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static GeoSpec spec(Def d) {
        return SPECS.computeIfAbsent(d.id(), k -> GeoSpec.of(d.key(), "arm_" + d.name()));
    }

    /**
     * Draws the pieces of the dive: each flies in from its own side while it fades up and grows from 85 to 100 percent over 8 ticks, staggered by its delay, and shatters out over the last
     * 18 ticks ({@link EquipAnimation}). A piece that was already on when a half dive became full ({@link SpiritDivePieces#alreadyOn}) does not forge again. Slim arms (Alex): arm pieces are
     * narrowed to 80 percent about the arm's own pivot.
     */
    private static void drawPieces(AuraContext c, Element el, Stage st, Grade grade, boolean remade, Lod lod, int glowRgb, float glowK, int tintRgb) {
        double d2 = remade ? 0.0 : dist2(c);
        if (remade ? lod == Lod.FAR || lod == Lod.CULLED : d2 > FAR2) return;
        boolean near = remade ? lod == Lod.FULL : d2 <= DETAIL2, self1p = firstPersonSelf(c);
        boolean slim = WaterAuraRig.slim(c.model());
        boolean air = !c.player().onGround() && !c.player().isInWater();
        PoseStack pose = c.pose();
        int lit = SealDraw.lift(c.light(), 6);
        java.util.List<String> list = remade ? SpiritDivePieces.pieces(el, st, grade) : SpiritDivePieces.pieces(el, st);
        for (int pi = 0, pn = list.size(); pi < pn; pi++) {
            String id = list.get(pi);
            Def p = DEFS.get(id);
            if (p == null || (p.detail() && !near) || (p.head() && self1p)) continue;
            boolean already = remade ? SpiritDivePieces.alreadyOn(el, st, grade, id) : SpiritDivePieces.alreadyOn(el, st, id);
            float a = EquipAnimation.assembled(already ? 1e4f : c.t(), c.left(), p.delay());
            if (a == 0f) continue;
            ModelPart part = part(c.model(), p.anchor());
            PartPose rest = part.getInitialPose();
            pose.pushPose();
            pose.translate(part.x / 16f, part.y / 16f, part.z / 16f);                             // the part's own pose (ModelPart.translateAndRotate without its scale)
            if (part.xRot != 0f || part.yRot != 0f || part.zRot != 0f) pose.mulPose(ROT.rotationZYX(part.zRot, part.yRot, part.xRot));
            pose.scale(1f / 16f, 1f / 16f, 1f / 16f);                                              // model pixels: what EquipAnimation.begin's numbers are in
            float alpha = EquipAnimation.begin(pose, a, p.dx(), p.dy(), p.dz());
            pose.scale(16f, 16f, 16f);
            if (slim && (p.anchor() == Anchor.ARM_R || p.anchor() == Anchor.ARM_L)) pose.scale(0.8f, 1f, 1f);
            pose.translate(-rest.x / 16f, -rest.y / 16f, -rest.z / 16f);                           // the model is authored in whole-body coordinates
            String clip = p.clip().isEmpty() ? null : p.clip();
            float seconds = c.age() / 20f * (p.flap() && air ? 2.3f : 1f) + (id.hashCode() & 7) * 0.11f;
            int white = ((int) (Mth.clamp(alpha, 0f, 1f) * 255f + 0.5f) << 24) | (tintRgb & 0xFFFFFF);
            int glow = ((int) (Mth.clamp(alpha * glowK * p.glow(), 0f, 1f) * 255f + 0.5f) << 24) | (glowRgb & 0xFFFFFF);
            boolean translucent = p.glass() || a < 1f;
            GeoDraw.paint(pose, c.buffers(), spec(p), clip, seconds, GeoDraw.Space.PLAYER, translucent ? GeoDraw.Layer.TRANSLUCENT : GeoDraw.Layer.CUTOUT, translucent ? lit : c.light(), white, glow);
            pose.popPose();
        }
    }

    // ------------------------------------------------------------------ the ring on the ground, the mana flowing in and the bits rising
    private static void extras(AuraContext c, Element el, Stage st, float pulse) {
        float f = st == Stage.UPGRADE ? EquipAnimation.alpha(EquipAnimation.assembled(1e4f, c.left(), 0f)) : WaterAuraRig.forge(c, 0f);          // an upgrade keeps the ring and the motes steady
        if (f <= 0.01f) return;
        float age = c.age();
        PoseStack pose = c.pose();
        SealDraw.camera(pose);
        float grow = 0.55f + 0.45f * f;
        int accent = glowRgb(el, st), mote = moteRgb(el, st);
        ResourceLocation ringTex = switch (el) { case WIND -> RING_WIND; case WATER -> RING_WATER; case FIRE -> RING_FIRE; case EARTH -> RING_EARTH; };
        pose.pushPose();                                                                          // a ring on the ground, turning (layer space: the feet are at y = 1.5)
        pose.translate(0f, 1.478f, 0f);
        VertexConsumer ring = c.buffers().getBuffer(AuraRender.additive(ringTex));
        SealDraw.flat(pose, ring, 0.95f * grow, age * (el == Element.EARTH ? 0.5f : 0.9f), SealDraw.argb(0.7f * f * pulse, accent), FB);
        pose.popPose();
        if (WaterAuraRig.far(c)) return;
        boolean half = SpiritDivePieces.halfBody(el, st);                                          // the wind half dive draws on its left side only
        VertexConsumer glow = c.buffers().getBuffer(AuraRender.additive(GLOW));
        // the passive absorption of ambient mana: motes spiral in from the air round the wearer into the chest
        for (int i = 0; i < MOTES; i++) {
            float ph = frac(age * 0.022f * (1f + 0.25f * (i % 4)) + i * 0.173f), e = ph * ph * (3f - 2f * ph);
            float a = i * 2.399f + age * 0.012f + e * 2.2f, r0 = 1.45f + 0.5f * Mth.sin(i * 1.7f), h0 = 0.25f + 1.8f * frac(i * 0.381f);
            float r = r0 * (1f - e), h = h0 + (1.05f - h0) * e;
            float x = Mth.cos(a) * r, z = Mth.sin(a) * r;
            if (half) x = Math.abs(x);
            SealDraw.glow(pose, glow, x, 1.5f - h, z, 0.075f * (1f - 0.55f * e) + 0.02f, a, SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.85f * f, mote), FB);
        }
        // the element's own bits rising round the body
        ResourceLocation bitTex = switch (el) { case WIND -> GLOW; case WATER -> BIT_WATER; case FIRE -> BIT_FIRE; case EARTH -> BIT_EARTH; };
        VertexConsumer bits = c.buffers().getBuffer(AuraRender.additive(bitTex));
        for (int i = 0; i < BITS; i++) {
            float ph = frac(age * 0.016f * (1f + 0.35f * (i % 3)) + i * 0.37f), a = i * 2.399f + age * 0.035f * (el == Element.WIND ? 2.2f : 1f);
            float r = 0.45f + 0.22f * Mth.sin(i * 1.9f) + 0.1f * ph, h = el == Element.EARTH ? 0.2f + 1.5f * ph : 0.05f + 2.0f * ph;
            float x = Mth.cos(a) * r, z = Mth.sin(a) * r;
            if (half) x = Math.abs(x);
            SealDraw.glow(pose, bits, x, 1.5f - h, z, 0.05f + 0.04f * (1f - ph), a, SealDraw.argb(Mth.sin(ph * Mth.PI) * f * (el == Element.WIND ? 0.7f : 1f), accent), FB);
        }
    }

    private static float frac(float x) { return x - (float) Math.floor(x); }
}
