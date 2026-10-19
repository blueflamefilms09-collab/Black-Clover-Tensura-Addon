package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

/**
 * Mercury Magic (Nozel Silva), 1.93.0 rebuild: the temporary armour (Aura.MERCURY), THE SILVER GUARDIAN. After the wiki: the user concentrates their mercury into a defensive coating over
 * the whole body, solid enough to block repeated strikes and to crack when broken, flexible enough not to hinder movement. Eleven geo pieces in smooth chrome built from stacked slabs:
 * a flowing crown with a swept crest and a bead of mercury at the brow (head); a rounded breastplate stepping in toward the waist round a quicksilver core (body); a skirt of hanging
 * drops (body); big domed shoulders with a dripping point (arms); bracers with a double-edged blade fin along the forearm (arms); greaves with pooled boots (legs); a PAIR OF EAGLE WINGS
 * on the back (two articulated segments each, they beat; the Silver Eagle, the coat becomes the bird) and a ring of orbiting drops over the head. Every piece is anchored on the model
 * part it was built for and FORGES IN AND SHATTERS OUT exactly like the Djinn full equip ({@link EquipAnimation}): 0 to 12 ticks of stagger, a fly-in from its own side, 85 to 100 percent
 * growth over 8 ticks, a shatter over the last 18 ticks (the shards are the pieces drifting apart and fading).
 * <p>Passes, in draw order: 1. the pieces, solid (entityCutoutNoCull) once forged, alpha blended (entityTranslucent) while forging or shattering, each with its own additive full-bright
 * glow map (RenderType.eyes); 2. the liquid coat: the player's posed model once more, 3 percent larger, alpha blended (entityTranslucent, lit) with rippled chrome and a few cracks
 * (textures/aura/mercury_coat.png); 3. the sheen: the posed model again, 4.5 percent larger, additive (RenderType.eyes, full bright) with bands of light travelling down the body
 * (textures/aura/mercury_sheen.png). Style 1 (the short armour after a signature cast) leaves out the wings and the orbiting drops. The head pieces are skipped for the local player
 * in first person.</p>
 */
public final class MercuryAura {
    private MercuryAura() {}

    private static final GeoSpec HELM = GeoSpec.of("mercury", "helm"), CHEST = GeoSpec.of("mercury", "chest"), TASSET = GeoSpec.of("mercury", "tasset"),
            PAUL_R = GeoSpec.of("mercury", "pauldron_r"), PAUL_L = GeoSpec.of("mercury", "pauldron_l"),
            VAMB_R = GeoSpec.of("mercury", "vambrace_r"), VAMB_L = GeoSpec.of("mercury", "vambrace_l"),
            GREAVE_R = GeoSpec.of("mercury", "greave_r"), GREAVE_L = GeoSpec.of("mercury", "greave_l"),
            BACK = GeoSpec.of("mercury", "back"), HALO = GeoSpec.of("mercury", "halo");
    private static ResourceLocation coat, sheen;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        coat = AuraRender.tex("aura/mercury_coat");
        sheen = AuraRender.tex("aura/mercury_sheen");
        PlayerAuraClient.register(Aura.MERCURY, MercuryAura::paint);
    }

    private static void paint(AuraContext c) {
        if (c.player().isInvisible()) return;
        Minecraft mc = Minecraft.getInstance();
        boolean self1p = c.player() == mc.player && mc.options.getCameraType().isFirstPerson();
        PlayerModel<?> m = c.model();
        float pulse = 0.75f + 0.25f * Mth.sin(c.age() * 0.1f);
        boolean full = c.style() != 1;
        // the wings beat faster while the wearer runs
        boolean fast = c.limbSwingAmount() > 0.7f;
        if (!self1p) piece(c, HELM, m.head, 0f, 0f, -14f, 0f, pulse, "idle");
        piece(c, CHEST, m.body, 2f, 0f, 0f, -14f, pulse, "idle");
        piece(c, TASSET, m.body, 4f, 0f, 10f, 0f, pulse, "idle");
        piece(c, PAUL_R, m.rightArm, 3f, -14f, -4f, 0f, pulse, "idle");
        piece(c, PAUL_L, m.leftArm, 3f, 14f, -4f, 0f, pulse, "idle");
        piece(c, VAMB_R, m.rightArm, 5f, -12f, 2f, -6f, pulse, "idle");
        piece(c, VAMB_L, m.leftArm, 5f, 12f, 2f, -6f, pulse, "idle");
        piece(c, GREAVE_R, m.rightLeg, 6f, -3f, 14f, -6f, pulse, "idle");
        piece(c, GREAVE_L, m.leftLeg, 6f, 3f, 14f, -6f, pulse, "idle");
        if (full) {
            piece(c, BACK, m.body, 8f, 0f, 0f, 14f, pulse, fast ? "flap" : "idle");
            if (!self1p) piece(c, HALO, m.head, 12f, 0f, -16f, 0f, pulse, "idle");
        }
        float shell = EquipAnimation.alpha(EquipAnimation.assembled(c, 2f));
        if (shell > 0.01f) {
            AuraRender.model(c, AuraRender.translucent(coat), 1.03f, 1.02f, 1.03f, AuraRender.alpha(0xFFFFFFFF, 0.5f * shell));
            AuraRender.modelBright(c, AuraRender.additive(sheen), 1.045f, 1.03f, 1.045f, AuraRender.alpha(0xFFFFFFFF, 0.5f * shell * pulse));
        }
    }

    /** The rest position (model pixels) of the vanilla player model's part the piece is anchored on. */
    private static float[] rest(ModelPart part, PlayerModel<?> m) {
        if (part == m.rightArm) return new float[]{-5f, 2f, 0f};
        if (part == m.leftArm) return new float[]{5f, 2f, 0f};
        if (part == m.rightLeg) return new float[]{-1.9f, 12f, 0f};
        if (part == m.leftLeg) return new float[]{1.9f, 12f, 0f};
        return new float[]{0f, 0f, 0f};
    }

    /** One geo piece authored in the humanoid's file frame, at the pose of the limb it belongs to, forged in from the direction (dx, dy, dz) in model pixels. */
    private static void piece(AuraContext c, GeoSpec spec, ModelPart part, float delay, float dx, float dy, float dz, float pulse, String clip) {
        float a = EquipAnimation.assembled(c, delay);
        if (a == 0f) return;
        PoseStack pose = c.pose();
        float[] rest = rest(part, c.model());
        pose.pushPose();
        pose.translate(part.x / 16f, part.y / 16f, part.z / 16f);
        if (part.xRot != 0f || part.yRot != 0f || part.zRot != 0f) pose.mulPose(new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot));
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
        float alpha = EquipAnimation.begin(pose, a, dx, dy, dz);
        pose.scale(16f, 16f, 16f);
        pose.translate(-rest[0] / 16f, -rest[1] / 16f, -rest[2] / 16f);
        GeoDraw.Layer layer = a >= 1f ? GeoDraw.Layer.CUTOUT : GeoDraw.Layer.TRANSLUCENT;
        GeoDraw.paint(pose, c.buffers(), spec, clip, c.age() / 20f, GeoDraw.Space.PLAYER, layer, c.light(), AuraRender.alpha(0xFFFFFFFF, alpha),
                AuraRender.alpha(0xFFFFFFFF, Math.min(1f, alpha * pulse * 0.7f)));
        pose.popPose();
    }
}
