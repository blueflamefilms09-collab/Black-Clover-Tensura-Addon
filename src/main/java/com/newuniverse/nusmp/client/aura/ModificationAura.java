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
 * Modification Magic (Morris Libardirt), 1.93.0 rebuild: the temporary armour (Aura.MODIFICATION), THE DISSECTOR'S FRAME. The body laid open and rebuilt: eleven geo pieces in
 * bone-white plates and ribs over dark steel with orange rune bands (0xFF9B58): round tinted lenses on a thin frame under a skull-cap with a crown of five needles (head); a ribcage
 * laid over the chest around an orange core, with a spine of vertebrae (body); a belt of hanging scalpels and glass vials (body); a vertebra stack on each shoulder with a short
 * tendril that sways (arms); a bone sleeve whose three long claws grow out of the knuckles (arms); bone-plated greaves with a heel spur (legs); FOUR CLAWED TENDRILS from the upper back,
 * four segments each, swaying on their own beat (body); and a ring of eight dissection needles turning over the head. Every piece is anchored on the model part it was built for and
 * FORGES IN AND SHATTERS OUT exactly like the Djinn full equip ({@link EquipAnimation}): 0 to 12 ticks of stagger, a fly-in from its own side, 85 to 100 percent growth over 8 ticks.
 * <p>Passes, in draw order: 1. the pieces, solid (entityCutoutNoCull) once forged, alpha blended (entityTranslucent) while forging or shattering, each with its own additive
 * full-bright glow map (RenderType.eyes); 2. the vein shell: the player's posed model once more, 4 percent larger, additive (RenderType.eyes, full bright), orange veins of
 * rewritten structure (textures/aura/modification_veins.png) running over the skin under the frame. Style 1 (the short armour after a signature cast) leaves out the tendrils and the
 * needle ring. The head pieces are skipped for the local player in first person.</p>
 */
public final class ModificationAura {
    private ModificationAura() {}

    private static final GeoSpec HELM = GeoSpec.of("modification", "helm"), CHEST = GeoSpec.of("modification", "chest"), TASSET = GeoSpec.of("modification", "tasset"),
            PAUL_R = GeoSpec.of("modification", "pauldron_r"), PAUL_L = GeoSpec.of("modification", "pauldron_l"),
            VAMB_R = GeoSpec.of("modification", "vambrace_r"), VAMB_L = GeoSpec.of("modification", "vambrace_l"),
            GREAVE_R = GeoSpec.of("modification", "greave_r"), GREAVE_L = GeoSpec.of("modification", "greave_l"),
            BACK = GeoSpec.of("modification", "back"), HALO = GeoSpec.of("modification", "halo");
    private static ResourceLocation veins;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        veins = AuraRender.tex("aura/modification_veins");
        PlayerAuraClient.register(Aura.MODIFICATION, ModificationAura::paint);
    }

    private static void paint(AuraContext c) {
        if (c.player().isInvisible()) return;
        Minecraft mc = Minecraft.getInstance();
        boolean self1p = c.player() == mc.player && mc.options.getCameraType().isFirstPerson();
        PlayerModel<?> m = c.model();
        float pulse = 0.78f + 0.22f * Mth.sin(c.age() * 0.14f);
        boolean full = c.style() != 1;
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
            piece(c, BACK, m.body, 8f, 0f, 0f, 14f, pulse, "idle");
            if (!self1p) piece(c, HALO, m.head, 12f, 0f, -16f, 0f, pulse, "idle");
        }
        float shell = EquipAnimation.alpha(EquipAnimation.assembled(c, 2f));
        if (shell > 0.01f)
            AuraRender.modelBright(c, AuraRender.additive(veins), 1.04f, 1.025f, 1.04f, AuraRender.alpha(0xFFFFFFFF, 0.6f * shell * pulse));
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
                AuraRender.alpha(0xFFFFFFFF, Math.min(1f, alpha * pulse * 0.85f)));
        pose.popPose();
    }
}
