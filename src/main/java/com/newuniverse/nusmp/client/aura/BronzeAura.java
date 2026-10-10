package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

/**
 * 0.53 Bronze Magic: the player render layer (Aura.BRONZE).
 * <ul>
 *   <li>Style 0, bronze armour: plates and rivets cut out of the player's skin layout (textures/aura/bronze_plate.png, solid with alpha holes,
 *       verdigris on the rims, polished highlights), laid over the posed body at 1.07; real 3D pieces on top of it that follow the limbs: a
 *       layered pauldron on each shoulder, an open helm with a comb (not in first person) and a gorget, medallion and belt on the body; a faint
 *       warm glow map additive over everything.</li>
 *   <li>Style 1, bronze statue sheen: the whole body covered in a smooth cast-bronze skin (textures/aura/bronze_cast.png) at 1.045 with a
 *       verdigris crust (translucent, 1.06) and a pulsing highlight (additive, 1.075).</li>
 * </ul>
 * Passes, in draw order: cutout skin, geo pieces (cutout, with their own additive glow), translucent patina, additive glow. Every solid pass is
 * scaled over the one under it by 1.5 % or more so nothing is coplanar. The head, the hat and the helm are skipped for the local player in
 * first person. Layer space: y points down, the middle of the body is at 0.75 blocks.
 */
public final class BronzeAura {
    private BronzeAura() {}

    private static final GeoSpec PAULDRON_R = GeoSpec.of("bronze", "pauldron_r"), PAULDRON_L = GeoSpec.of("bronze", "pauldron_l"),
            CREST = GeoSpec.of("bronze", "crest"), BREAST = GeoSpec.of("bronze", "breast");
    private static ResourceLocation plate, plateGlow, cast, castGlow, patina, sheen;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        plate = AuraRender.tex("aura/bronze_plate");
        plateGlow = AuraRender.tex("aura/bronze_plate_glow");
        cast = AuraRender.tex("aura/bronze_cast");
        castGlow = AuraRender.tex("aura/bronze_cast_glow");
        patina = AuraRender.tex("aura/bronze_patina");
        sheen = AuraRender.tex("aura/bronze_sheen");
        PlayerAuraClient.register(Aura.BRONZE, BronzeAura::paint);
    }

    private static void paint(AuraContext c) {
        float f = c.fade();
        if (f <= 0.002f) return;
        Minecraft mc = Minecraft.getInstance();
        boolean self1p = c.player() == mc.player && mc.options.getCameraType().isFirstPerson();
        PlayerModel<?> m = c.model();
        boolean head = m.head.visible, hat = m.hat.visible;
        if (self1p) { m.head.visible = false; m.hat.visible = false; }
        try {
            if (c.style() == 1) sheen(c, f); else armour(c, f, self1p);
        } finally {
            m.head.visible = head;
            m.hat.visible = hat;
        }
    }

    // ================================================================================================ style 0: plates and rivets
    private static void armour(AuraContext c, float f, boolean self1p) {
        float age = c.age(), pulse = 0.7f + 0.3f * Mth.sin(age * 0.1f);
        float k = 0.07f * f;
        // 1. the plate skin, solid with holes (entityCutoutNoCull), 7 % over the body; it forges in by growing from the skin
        AuraRender.model(c, AuraRender.cutout(plate), 1f + k, 1f + k * 0.45f, 1f + k, 0xFFFFFFFF);
        // 2. the 3D pieces, cutout, each with its own additive glow map
        int glow = AuraRender.alpha(0xFFFFB060, 0.55f * pulse * f);
        PlayerModel<?> m = c.model();
        piece(c, PAULDRON_R, m.rightArm, glow);
        piece(c, PAULDRON_L, m.leftArm, glow);
        piece(c, BREAST, m.body, glow);
        if (!self1p) piece(c, CREST, m.head, glow);
        // 3. the faint warm glow of the rivets and the polished rims (additive, full bright, 9 % over the body)
        AuraRender.modelBright(c, AuraRender.additive(plateGlow), 1f + 0.09f * f, 1f + 0.05f * f, 1f + 0.09f * f, AuraRender.alpha(0xFFFFA040, 0.5f * pulse * f));
    }

    /** Draws a geo piece authored in the humanoid's file frame at the pose of the limb it belongs to. */
    private static void piece(AuraContext c, GeoSpec spec, ModelPart part, int glow) {
        PoseStack pose = c.pose();
        PartPose rest = part.getInitialPose();
        pose.pushPose();
        pose.translate(part.x / 16f, part.y / 16f, part.z / 16f);
        if (part.xRot != 0f || part.yRot != 0f || part.zRot != 0f) pose.mulPose(new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot));
        pose.translate(-rest.x / 16f, -rest.y / 16f, -rest.z / 16f);
        GeoDraw.paint(pose, c.buffers(), spec, "idle", c.age() / 20f, GeoDraw.Space.PLAYER, GeoDraw.Layer.CUTOUT, c.light(), 0xFFFFFFFF, glow);
        pose.popPose();
    }

    // ================================================================================================ style 1: the cast-bronze statue skin
    private static void sheen(AuraContext c, float f) {
        float age = c.age(), pulse = 0.5f + 0.5f * Mth.sin(age * 0.07f);
        float k = 0.045f * f;
        // 1. the smooth cast skin, solid (entityCutoutNoCull), 4.5 % over the body
        AuraRender.model(c, AuraRender.cutout(cast), 1f + k, 1f + k * 0.5f, 1f + k, 0xFFFFFFFF);
        // 2. the verdigris crust, alpha blended, 6 % over the body (above the cast skin)
        AuraRender.model(c, AuraRender.translucent(patina), 1f + 0.06f * f, 1f + 0.03f * f, 1f + 0.06f * f, AuraRender.alpha(0xFFFFFFFF, f));
        // 3. warm glow of the cast and a highlight that breathes (additive, full bright, 7.5 % over the body)
        AuraRender.modelBright(c, AuraRender.additive(castGlow), 1f + 0.075f * f, 1f + 0.04f * f, 1f + 0.075f * f, AuraRender.alpha(0xFFFFB060, 0.35f * f));
        AuraRender.modelBright(c, AuraRender.additive(sheen), 1f + 0.08f * f, 1f + 0.042f * f, 1f + 0.08f * f, AuraRender.alpha(0xFFFFE0A0, (0.10f + 0.22f * pulse) * f));
    }
}
