package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

/**
 * Recombination Magic (Henry Legolant), 1.93.0 rebuild: the temporary armour (Aura.RECOMBINATION), THE HOUSE WORN AS ARMOUR. The magic house of the Black Bulls taken apart and put
 * back together on the caster, eleven geo pieces in dark oak, iron brackets, red-brown roof tiles and grey foundation stone, every window glowing warm orange: a gabled cap with a
 * chimney, a lamp on the brow and the horns of the Black Bulls (head); a wall plate with a swinging door, two windows with shutters and a roof-beam collar (body); a skirt of
 * floorboards (body); gable-end shoulders with a swinging lantern (arms); log sleeves with iron straps and a stone gauntlet (arms); stilt boots on foundation blocks (legs); the
 * HOUSE ITSELF on the back, two storeys with a lit window, smoke puffing out of its chimney and three rooms circling it (body); and a ring of six loose planks over the head.
 * Every piece is anchored on the model part it was built for and FORGES IN AND SHATTERS OUT exactly like the Djinn full equip ({@link EquipAnimation}), 0 to 12 ticks of stagger.
 * <p>Passes, in draw order: 1. the pieces, solid (entityCutoutNoCull) once forged, alpha blended (entityTranslucent) while forging or shattering, each with its own additive
 * full-bright glow map (RenderType.eyes: windows, lamps, the eyes of the horns). Style 1 (the short armour after a signature cast) leaves out the back house and the planks. The
 * head pieces are skipped for the local player in first person.</p>
 */
public final class RecombinationAura {
    private RecombinationAura() {}

    private static final GeoSpec HELM = GeoSpec.of("recombination", "helm"), CHEST = GeoSpec.of("recombination", "chest"), TASSET = GeoSpec.of("recombination", "tasset"),
            PAUL_R = GeoSpec.of("recombination", "pauldron_r"), PAUL_L = GeoSpec.of("recombination", "pauldron_l"),
            VAMB_R = GeoSpec.of("recombination", "vambrace_r"), VAMB_L = GeoSpec.of("recombination", "vambrace_l"),
            GREAVE_R = GeoSpec.of("recombination", "greave_r"), GREAVE_L = GeoSpec.of("recombination", "greave_l"),
            BACK = GeoSpec.of("recombination", "back"), HALO = GeoSpec.of("recombination", "halo");

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        PlayerAuraClient.register(Aura.RECOMBINATION, RecombinationAura::paint);
    }

    private static void paint(AuraContext c) {
        if (c.player().isInvisible()) return;
        Minecraft mc = Minecraft.getInstance();
        boolean self1p = c.player() == mc.player && mc.options.getCameraType().isFirstPerson();
        PlayerModel<?> m = c.model();
        float pulse = 0.8f + 0.2f * Mth.sin(c.age() * 0.1f);
        boolean full = c.style() != 1;
        if (!self1p) piece(c, HELM, m.head, 0f, 0f, -14f, 0f, pulse);
        piece(c, CHEST, m.body, 2f, 0f, 0f, -14f, pulse);
        piece(c, TASSET, m.body, 4f, 0f, 10f, 0f, pulse);
        piece(c, PAUL_R, m.rightArm, 3f, -14f, -4f, 0f, pulse);
        piece(c, PAUL_L, m.leftArm, 3f, 14f, -4f, 0f, pulse);
        piece(c, VAMB_R, m.rightArm, 5f, -12f, 2f, -6f, pulse);
        piece(c, VAMB_L, m.leftArm, 5f, 12f, 2f, -6f, pulse);
        piece(c, GREAVE_R, m.rightLeg, 6f, -3f, 14f, -6f, pulse);
        piece(c, GREAVE_L, m.leftLeg, 6f, 3f, 14f, -6f, pulse);
        if (full) {
            piece(c, BACK, m.body, 8f, 0f, 0f, 14f, pulse);
            if (!self1p) piece(c, HALO, m.head, 12f, 0f, -16f, 0f, pulse);
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
    private static void piece(AuraContext c, GeoSpec spec, ModelPart part, float delay, float dx, float dy, float dz, float pulse) {
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
        GeoDraw.paint(pose, c.buffers(), spec, "idle", c.age() / 20f, GeoDraw.Space.PLAYER, layer, c.light(), AuraRender.alpha(0xFFFFFFFF, alpha),
                AuraRender.alpha(0xFFFFFFFF, Math.min(1f, alpha * pulse * 0.85f)));
        pose.popPose();
    }
}
