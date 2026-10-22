package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.book.FuegoleonArmRules;
import com.newuniverse.nusmp.book.SpiritDiveTier;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.prop.SealDraw;
import com.newuniverse.nusmp.perf.PerfProbe;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

/**
 * Spirit Dive remake: Fuegoleon's flaming right arm in third person (and in the inventory-less view of other players): {@code Aura.FUEGOLEON_ARM}. The server (book.FuegoleonArm) keeps the lease
 * alive while the Salamander is his (the contract, the spirit out, or the Fire dive); this painter draws a molten arm over the player's own right arm, as in the series (the official profile: he has a
 * flaming right arm): the 3D model {@code spiritdive_arm_flame_arm_r} (shoulder flare, upper arm, elbow, forearm and fist of lava, flame tongues that sway), the animated flame sheets round it, embers, a heat
 * haze and a mane of fire strands streaming off the shoulder ({@link FuegoleonArmFx}). The flames leap when the wearer casts (the cast animation, {@link DiveCast}) and burn highest during the Fire dive
 * ({@code Aura.SPIRIT_DIVE} running). The viewer's setting ({@link SpiritDiveLook#armView}) can turn the arm off or limit it to the moments of casting and diving. The first person arm is
 * {@link FireArmFirstPerson}. Level of detail by distance ({@link SpiritDiveTier#lod}): the full arm up to 24 blocks, the model and half the flames up to 48, nothing beyond.
 * The dive of the Fuegoleon grade does not draw an arm of its own: the server keeps this arm alive during the Fire dive. No allocation per frame.
 */
public final class FuegoleonArmAura {
    private FuegoleonArmAura() {}

    private static final GeoSpec SPEC = GeoSpec.of("spiritdive", "arm_flame_arm_r");
    private static final Quaternionf ROT = new Quaternionf();                                   // scratch: the render thread is the only user
    private static final PerfProbe.Section SECTION = PerfProbe.section("c.fuegoleon.arm");           // timed (/multiverse perf)

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        PlayerAuraClient.register(Aura.FUEGOLEON_ARM, FuegoleonArmAura::paint);
    }

    private static void paint(AuraContext c) {
        if (!FuegoleonArmRules.valid(c.style()) || c.player().isInvisible()) return;
        long t0 = PerfProbe.begin();
        try {
            paintArm(c);
        } finally {
            PerfProbe.end(SECTION, t0);
        }
    }

    private static void paintArm(AuraContext c) {
        FuegoleonArmRules.View view = SpiritDiveLook.armView();
        if (view == FuegoleonArmRules.View.OFF) return;
        int id = c.player().getId();
        boolean casting = DiveCast.casting(id), diving = PlayerAuraClient.has(id, Aura.SPIRIT_DIVE);
        float k = FuegoleonArmRules.strength(view, casting, diving, c.style() == FuegoleonArmRules.STYLE_LINKED) * c.fade();
        if (k <= 0.01f) return;
        Lod lod = SpiritDiveTier.lod(SpiritDiveFrame.distSq(c));
        if (lod == Lod.FAR || lod == Lod.CULLED) return;
        float flare = FuegoleonArmRules.flare(casting, diving);
        PoseStack pose = c.pose();
        ModelPart arm = c.model().rightArm;
        PartPose rest = arm.getInitialPose();
        boolean slim = WaterAuraRig.slim(c.model());
        float age = c.age();
        pose.pushPose();                                                                           // the molten model, anchored on the right arm (it follows every swing, crouch and cast)
        pose.translate(arm.x / 16f, arm.y / 16f, arm.z / 16f);
        if (arm.xRot != 0f || arm.yRot != 0f || arm.zRot != 0f) pose.mulPose(ROT.rotationZYX(arm.zRot, arm.yRot, arm.xRot));
        if (slim) pose.scale(0.8f, 1f, 1f);
        pose.translate(-rest.x / 16f, -rest.y / 16f, -rest.z / 16f);                             // the model is authored in whole-body coordinates
        int white = SealDraw.argb(c.fade(), 0xFFFFFF);                                           // the body is solid at every strength (it covers the skin); only the flames and the glow follow the strength
        int glow = SealDraw.argb(Mth.clamp(k * (1.0f + 0.5f * flare), 0f, 1f), 0xFF8A30);
        boolean solid = c.fade() >= 0.97f;                                                       // opaque once grown in, translucent while it fades in and out
        GeoDraw.paint(pose, c.buffers(), SPEC, "idle", age / 20f, GeoDraw.Space.PLAYER, solid ? GeoDraw.Layer.CUTOUT : GeoDraw.Layer.TRANSLUCENT, SealDraw.lift(c.light(), 8), white, glow);
        pose.popPose();
        pose.pushPose();                                                                           // the flames, in the arm frame (model pixels)
        pose.translate(arm.x / 16f, arm.y / 16f, arm.z / 16f);
        if (arm.xRot != 0f || arm.yRot != 0f || arm.zRot != 0f) pose.mulPose(ROT.rotationZYX(arm.zRot, arm.yRot, arm.xRot));
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
        boolean full = lod == Lod.FULL;
        FuegoleonArmFx.draw(pose, c.buffers(), age, k, flare, full, false);
        if (full) FuegoleonArmFx.mane(pose, c.buffers(), age, k, flare, c.limbSwingAmount());
        pose.popPose();
    }
}
