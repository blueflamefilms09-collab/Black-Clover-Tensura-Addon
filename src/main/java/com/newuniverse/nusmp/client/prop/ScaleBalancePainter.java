package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.scalecast.ScaleFrame;
import com.newuniverse.nusmp.client.scalecast.ScaleHud;
import com.newuniverse.nusmp.client.scalecast.ScaleRender;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.scalecast.ScaleCastRules;
import com.newuniverse.nusmp.scalecast.ScaleLod;
import com.newuniverse.nusmp.scalecast.ScaleVisualConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Why: how the Scale Magic balance scale looks and moves (docs/magic/scale_casting.md; the server side is scalecast.ScaleCastKit, the geometry scalecast.ScaleMesh, previewed by
 * tools/scalecast/preview_scale.py). The prop word is decoded here and nothing else is asked of the server:
 * <ul>
 *   <li>the beam tilts toward the heavier side ({@link ScaleCastRules#tilt}: the foe's pan, on the right, goes down for a heavier foe) on a damped spring, so a new foe makes it swing and settle;
 *       with no foe it rests level with a faint sway;</li>
 *   <li>a Scale page cast (the pulse bits change) starts the weighing animation: the beam swings and rings down ({@link ScaleCastRules#weighSwing}) and the pans bob ({@link ScaleCastRules#panBob});
 *       the animation is tied to the tick the cast reached the server, not to the key press;</li>
 *   <li>the two pans hold an orb each, sized by that side's share of the heavier weight, with a glow in the colour of its side;</li>
 *   <li>the beam's runes, the jewel, the halo (stronger while the aura is on) glow through SafeAdditive; a thin line runs from the jewel to the weighed foe and a seal ring turns under its feet
 *       while the aura presses it;</li>
 *   <li>a small readout floats above the Scale (the weights, the percent taken) and, for the local player's own Scale, a plate over the hotbar ({@link ScaleHud}).</li>
 * </ul>
 * Everything is skipped when {@code visual.scale3d} is off. Each paint asks {@link ScaleFrame} for a level of detail first (distance, behind-the-camera cull, the per-frame quad budget). No
 * allocation per frame: the per-prop state lives in a weak map, the texts are built when the word changes.
 */
public final class ScaleBalancePainter {
    private ScaleBalancePainter() {}

    /** The client-side animation state of one Scale (weak: a removed prop frees it). */
    private static final class Anim {
        final float[] spring = new float[2];
        float last = Float.NaN, pulseAge = -100f, sign = 1f, fadeAge = -1f;
        int pulse = -1, textWord = 0, textTarget = Integer.MIN_VALUE;
        boolean init, text;
        String line1 = "", line2 = "";
        float width1, width2;
    }

    private static final Map<Entity, Anim> ANIMS = new WeakHashMap<>();
    private static final com.newuniverse.nusmp.perf.PerfProbe.Section PROBE_PAINT = com.newuniverse.nusmp.perf.PerfProbe.section("c.scale.paint");
    /** How far (blocks) the readout above a Scale is drawn. */
    private static final double READOUT_RANGE_SQ = 14.0 * 14.0;

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        ScaleFrame.init();
        PropPainters.register(PropKind.SCALE_BALANCE, ScaleBalancePainter::paint);
    }

    private static void paint(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        long t0 = com.newuniverse.nusmp.perf.PerfProbe.begin();
        try {
            paint0(e, partial, age, pose, buf, light);
        } finally {
            com.newuniverse.nusmp.perf.PerfProbe.end(PROBE_PAINT, t0);
        }
    }

    private static void paint0(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        if (!ScaleVisualConfig.scale3d()) return;
        int word = e.param();
        Anim a = ANIMS.computeIfAbsent(e, k -> new Anim());
        float fadeSince = -1f;
        if (ScaleCastRules.fading(word)) {
            if (a.fadeAge < 0f) a.fadeAge = age;
            fadeSince = age - a.fadeAge;
        } else {
            a.fadeAge = -1f;
        }
        float k = ScaleCastRules.opacity(age, fadeSince);
        if (k <= 0.01f) return;
        int lod = ScaleFrame.level(e);
        if (lod >= ScaleLod.CULLED) return;                                                    // too far, behind the camera, or the frame's quad budget is spent on nearer ones
        boolean hasFoe = ScaleCastRules.hasFoe(word), aura = ScaleCastRules.aura(word);
        int cq = ScaleCastRules.casterQ(word), fq = ScaleCastRules.foeQ(word);
        // the weighing animation starts when the pulse bits change (a Scale page reached the server); the first word seen starts nothing
        int pulse = ScaleCastRules.pulse(word);
        if (!a.init) {
            a.init = true;
            a.pulse = pulse;
            a.last = age;
            a.spring[0] = ScaleCastRules.tilt(cq, fq, hasFoe) * -1f + 12f;                       // it first swings in from a lifted side, the way a hung beam settles
            a.spring[1] = 0f;
        } else if (pulse != a.pulse) {
            a.pulse = pulse;
            a.pulseAge = age;
            a.sign = -a.sign;
        }
        float since = age - a.pulseAge;
        // the beam: a damped spring toward the tilt of the two weights (a heavier foe turns it clockwise: its pan, on the right, goes down)
        float goal = -ScaleCastRules.tilt(cq, fq, hasFoe) + (hasFoe ? 0.8f * Mth.sin(age * 0.11f) : 2.2f * Mth.sin(age * 0.05f));
        ScaleCastRules.spring(a.spring, goal, age - a.last);
        a.last = age;
        float angle = a.spring[0] + ScaleCastRules.weighSwing(since, a.sign);
        float bob = ScaleCastRules.panBob(since) * a.sign + 0.012f * Mth.sin(age * 0.21f);
        float loadC = hasFoe ? ScaleCastRules.load(cq, fq) : 0.55f, loadF = hasFoe ? ScaleCastRules.load(fq, cq) : 0f;
        float pop = ScaleCastRules.popIn(age, ScaleCastRules.FADE_IN);
        float s = e.scale() * Math.max(0.05f, pop) * (0.8f + 0.2f * k);
        float yaw = Mth.rotLerp(partial, e.yRotO, e.getYRot());
        float bodyBob = 0.06f * Mth.sin(age * 0.08f), sway = 6f * Mth.sin(age * 0.03f);
        pose.pushPose();
        pose.translate(0f, bodyBob, 0f);
        pose.mulPose(Axis.YP.rotationDegrees(sway));
        pose.scale(s, s, s);
        SealDraw.camera(pose);
        float glow = 1f + (aura ? 0.25f : 0f) + (since < 20f ? 0.5f * (1f - since / 20f) : 0f) + 0.1f * Mth.sin(age * 0.2f);
        ScaleRender.scale(pose, buf, light, angle, bob, -bob, loadC, loadF, k, glow, lod);
        if (lod <= ScaleLod.MEDIUM) ScaleRender.halo(pose, buf, age, k, aura);
        pose.popPose();
        if (lod > ScaleLod.MEDIUM) return;
        // the foe: the measuring line, the seal ring under it (drawn in a pose without the prop's yaw, so offsets are world offsets)
        Entity t = hasFoe ? e.target() : null;
        if (t != null) {
            double fx = Mth.lerp(partial, t.xo, t.getX()) - Mth.lerp(partial, e.xo, e.getX());
            double fy = Mth.lerp(partial, t.yo, t.getY()) - Mth.lerp(partial, e.yo, e.getY());
            double fz = Mth.lerp(partial, t.zo, t.getZ()) - Mth.lerp(partial, e.zo, e.getZ());
            if (fx * fx + fy * fy + fz * fz < 24.0 * 24.0) {
                pose.pushPose();
                pose.mulPose(Axis.YP.rotationDegrees(yaw));                                    // undo the renderer's -yaw
                SealDraw.camera(pose);
                float mid = (float) (fy + t.getBbHeight() * 0.5);
                ScaleRender.line(pose, buf, 0f, 0.05f, 0f, (float) fx, mid, (float) fz, 0.03f, 0.22f * k, ScaleRender.TEAL);
                if (aura && ScaleCastRules.percent(word) > 0) {
                    pose.translate(fx, fy + 0.04, fz);
                    float strength = Mth.clamp(ScaleCastRules.percent(word) / 35f, 0.2f, 1f);
                    ScaleRender.groundRing(pose, buf, 0.8f + t.getBbWidth() * 0.7f, age * 0.04f, (0.35f + 0.5f * strength) * k * (0.85f + 0.15f * Mth.sin(age * 0.25f)), ScaleRender.GOLD);
                }
                pose.popPose();
            }
        }
        // the readout above the Scale
        boolean mine = near(e);
        if (ScaleVisualConfig.readout() && hasFoe && (mine || ScaleFrame.distSq(e) < READOUT_RANGE_SQ)) {
            buildText(a, word, t);
            if (mine) ScaleHud.note(a.line1, a.line2, e.level().getGameTime());
            if (lod <= ScaleLod.MEDIUM && ScaleFrame.distSq(e) < READOUT_RANGE_SQ) readout(a, e, yaw, s, k, pose, buf);
        }
    }

    /** Builds the two lines of the readout when the word or the foe changed (a few times a second at most): the weights and what the aura takes. */
    private static void buildText(Anim a, int word, Entity foe) {
        int target = foe == null ? -1 : foe.getId();
        if (a.text && a.textWord == word && a.textTarget == target) return;
        a.text = true;
        a.textWord = word;
        a.textTarget = target;
        String you = Component.translatableWithFallback("nusmp.scale.you", "You").getString(), who = foe == null ? Component.translatableWithFallback("nusmp.scale.foe", "Foe").getString() : foe.getName().getString();
        a.line1 = you + " " + ScaleCastRules.format(ScaleCastRules.dequantize(ScaleCastRules.casterQ(word))) + "   |   " + who + " " + ScaleCastRules.format(ScaleCastRules.dequantize(ScaleCastRules.foeQ(word)));
        int pct = ScaleCastRules.percent(word);
        a.line2 = pct > 0 && ScaleCastRules.aura(word) ? Component.translatableWithFallback("nusmp.scale.pressed", "Pressed down: -%s%% blows", pct).getString()
                : Component.translatableWithFallback("nusmp.scale.level", "Weighed, not pressed").getString();
        Font font = Minecraft.getInstance().font;
        a.width1 = font.width(a.line1);
        a.width2 = font.width(a.line2);
    }

    /** The billboard text above the Scale: drawn in a pose without the prop's yaw, turned to the camera. */
    private static void readout(Anim a, MagicPropEntity e, float yaw, float s, float k, PoseStack pose, MultiBufferSource buf) {
        Font font = Minecraft.getInstance().font;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(0f, 0.62f * s + 0.35f, 0f);
        pose.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        pose.scale(-0.014f, -0.014f, 0.014f);
        int alpha = Mth.clamp((int) (k * 255f), 4, 255);
        font.drawInBatch(a.line1, -a.width1 / 2f, 0f, (alpha << 24) | 0xFFE8B0, false, pose.last().pose(), buf, Font.DisplayMode.NORMAL, 0, ScaleRender.FB);
        font.drawInBatch(a.line2, -a.width2 / 2f, 10f, (alpha << 24) | 0x9CF0E4, false, pose.last().pose(), buf, Font.DisplayMode.NORMAL, 0, ScaleRender.FB);
        pose.popPose();
    }

    /** True when this prop floats where the local player's own Scale floats (the client does not know the owner; the place says it). */
    static boolean near(MagicPropEntity e) {
        LocalPlayer lp = Minecraft.getInstance().player;
        if (lp == null) return false;
        double dx = e.getX() - ScaleCastRules.positionX(lp.getX(), lp.yBodyRot), dy = e.getY() - ScaleCastRules.positionY(lp.getY()), dz = e.getZ() - ScaleCastRules.positionZ(lp.getZ(), lp.yBodyRot);
        return dx * dx + dy * dy + dz * dz < 4.0;
    }
}
