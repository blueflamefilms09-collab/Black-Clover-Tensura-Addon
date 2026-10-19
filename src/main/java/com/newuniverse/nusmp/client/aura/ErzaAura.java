package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.client.aura.WaterAuraRig.Anchor;
import com.newuniverse.nusmp.client.aura.WaterAuraRig.Piece;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.SealDraw;
import com.newuniverse.nusmp.requip.Armour;
import com.newuniverse.nusmp.requip.ArmourSets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Requip: The Knight (Erza Scarlet): the render layer of {@link Aura#ERZA}. The style the server sets is the worn armour's ordinal in {@link Armour}; the painter draws that armour's set of pieces,
 * each its own geo model (assets/nusmp/geo/entity/erza_&lt;armour&gt;_arm_&lt;piece&gt;, written by tools/gen_erza_requip_geo.py, with a glow map) anchored on the model part it was built for, through
 * {@link WaterAuraRig}, which forges the pieces in and shatters them out with {@link EquipAnimation}: the Djinn Full Equip. The armour is never in an armour slot. The pieces and their delays
 * and fly-in sides are the table {@link ArmourSets}. Extras (additive, full bright, after the pieces): a ring of light at the feet that opens while the armour forges in, a slow ring turning with the
 * armour's glow colour, and motes in its theme colour rising off it.
 */
public final class ErzaAura {
    private ErzaAura() {}

    private static final ResourceLocation RING = AuraRender.tex("particle/flamefx_ring"), MOTE = AuraRender.tex("particle/flamefx_ember");
    private static final int FB = PropDraw.FULL_BRIGHT;

    private static volatile WaterAuraRig.Set[] sets;

    private static WaterAuraRig.Set[] sets() {
        WaterAuraRig.Set[] s = sets;
        if (s == null) {
            Armour[] all = Armour.values();
            s = new WaterAuraRig.Set[all.length];
            for (Armour a : all) s[a.ordinal()] = build(a);
            sets = s;
        }
        return s;
    }

    private static Anchor anchor(ArmourSets.Anchor a) {
        return switch (a) {
            case HEAD -> Anchor.HEAD;
            case BODY -> Anchor.BODY;
            case ARM_R -> Anchor.ARM_R;
            case ARM_L -> Anchor.ARM_L;
            case LEG_R -> Anchor.LEG_R;
            case LEG_L -> Anchor.LEG_L;
        };
    }

    /** The rig set of an armour, from the table. */
    static WaterAuraRig.Set build(Armour a) {
        var pieces = ArmourSets.of(a);
        Piece[] out = new Piece[pieces.size()];
        for (int i = 0; i < out.length; i++) {
            ArmourSets.Piece p = pieces.get(i);
            Piece w = Piece.of(p.name(), anchor(p.anchor()), p.delay(), p.dx(), p.dy(), p.dz());
            if (p.glass()) w.glass();
            if (p.detail()) w.detail();
            if (p.head()) w.head();
            if (p.clip() != null) w.clip(p.clip(), 1f);
            if (p.flap()) w.flap();
            w.glow(p.glow());
            out[i] = w;
        }
        return new WaterAuraRig.Set(a.geoKey(), out);
    }

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        PlayerAuraClient.register(Aura.ERZA, ErzaAura::paint);
    }

    private static void paint(AuraContext c) {
        Armour a = Armour.byOrdinal(c.style());
        if (a == null || c.fade() <= 0.002f) return;
        float age = c.age(), pulse = 0.8f + 0.2f * Mth.sin(age * 0.21f) + 0.08f * Mth.sin(age * 0.67f);
        float forge = WaterAuraRig.forge(c, 0f);
        WaterAuraRig.draw(c, sets()[a.ordinal()], a.glow(), 0.75f * pulse + 0.3f);
        extras(c, a, forge, pulse);
    }

    private static void extras(AuraContext c, Armour a, float forge, float pulse) {
        if (forge <= 0.01f) return;
        float age = c.age(), t = c.t();
        PoseStack pose = c.pose();
        SealDraw.camera(pose);
        VertexConsumer ring = c.buffers().getBuffer(AuraRender.additive(RING));
        // the flash of the swap: a ring of light opening at the feet over the first 16 ticks, then a slow turning ring while the armour stays on
        pose.pushPose();
        pose.translate(0f, 1.475f, 0f);
        if (t < 16f) {
            float k = t / 16f;
            SealDraw.flat(pose, ring, 0.5f + 1.7f * k, age * 4f, SealDraw.argb((1f - k) * 0.9f, 0xFFFFFF), FB);
            SealDraw.flat(pose, ring, 0.4f + 1.3f * k, -age * 3f, SealDraw.argb((1f - k) * 0.9f, a.glow()), FB);
        }
        SealDraw.flat(pose, ring, 0.95f, age * 1.4f, SealDraw.argb(0.45f * forge * pulse, a.glow()), FB);
        pose.popPose();
        if (WaterAuraRig.far(c)) return;
        VertexConsumer mote = c.buffers().getBuffer(AuraRender.additive(MOTE));
        for (int i = 0; i < 10; i++) {
            float ph = frac(age * 0.014f * (1f + 0.3f * (i % 3)) + i * 0.41f), ang = i * 2.399f + age * 0.03f;
            float r = 0.5f + 0.2f * Mth.sin(i * 1.7f);
            SealDraw.glow(pose, mote, Mth.cos(ang) * r, 1.4f - ph * 2.0f, Mth.sin(ang) * r, 0.05f + 0.04f * (1f - ph), ang,
                    SealDraw.argb(Mth.sin(ph * Mth.PI) * forge, i % 2 == 0 ? a.theme() : a.glow()), FB);
        }
    }

    private static float frac(float x) { return x - (float) Math.floor(x); }
}
