package examples;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.aura.PlayerAuraClient;
import com.newuniverse.nusmp.client.prop.PropDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/**
 * Self-test of the render preview: a player aura with three passes over the posed model (cutout plates at exactly 1.0, a translucent shell at
 * 1.02, an additive halo at 1.08) plus PropDraw geometry in layer space: a halo ring over the head and four crystals standing round the
 * waist. Layer space has y pointing DOWN, so the crystals are turned 180 degrees about z to point up (a negative scale would flip the
 * faces). Registered under Aura.BRONZE (the real Bronze aura is a stub).
 */
public final class AuraOverlay {
    private AuraOverlay() {}

    public static void register() {
        PlayerAuraClient.register(Aura.BRONZE, c -> {
            float f = c.fade();
            boolean firstPersonSelf = Minecraft.getInstance().options.getCameraType().isFirstPerson() && c.player() == Minecraft.getInstance().player;
            // 1. plates hugging the skin, scale exactly 1.0
            AuraRender.model(c, AuraRender.cutout(AuraRender.tex("aura/preview_overlay")), 1f, 1f, 1f, 0xFFFFFFFF);
            // 2. a glassy shell, 2 % bigger
            AuraRender.model(c, AuraRender.translucent(AuraRender.tex("aura/preview_aura_glass")), 1.02f, 1.02f, 1.02f, AuraRender.alpha(0xFFFFFFFF, 0.55f * f));
            // 3. an additive rim halo, 8 % bigger, full bright
            float pulse = 0.65f + 0.2f * Mth.sin(c.age() * 0.2f);
            AuraRender.modelBright(c, AuraRender.additive(AuraRender.tex("aura/preview_aura_glow")), 1.08f, 1.08f, 1.08f, AuraRender.alpha(0xFFFFB040, 0.7f * pulse * f));
            // 4. geometry in layer space
            PoseStack pose = c.pose();
            if (!firstPersonSelf) {
                pose.pushPose();
                pose.translate(0f, -0.82f + 0.02f * Mth.sin(c.age() * 0.15f), 0f);
                PropDraw.torus(pose, c.buffers().getBuffer(AuraRender.additive(AuraRender.tex("prop/preview_glow"))), 0f, 0f, 0f, 0f, 1f, 0f, 0.42f, 0.035f, 24, 6,
                        AuraRender.alpha(0xFFFFE080, f), PropDraw.FULL_BRIGHT);
                pose.popPose();
            }
            var crystals = c.buffers().getBuffer(AuraRender.cutout(AuraRender.tex("prop/preview_checker")));
            for (int i = 0; i < 4; i++) {
                float a = c.age() * 0.05f + i * Mth.HALF_PI;
                pose.pushPose();
                pose.translate(Mth.cos(a) * 0.75f * f, 0.55f, Mth.sin(a) * 0.75f * f);
                pose.mulPose(Axis.YP.rotationDegrees(-a * Mth.RAD_TO_DEG));
                pose.mulPose(Axis.ZP.rotationDegrees(180f));                          // y is down here: this makes the tip point up
                PropDraw.crystal(pose, crystals, 0f, -0.1f, 0f, 0.07f * f, 0.22f * f, 0.14f * f, 0xFFFFFFFF, c.light());
                pose.popPose();
            }
        });
    }
}
