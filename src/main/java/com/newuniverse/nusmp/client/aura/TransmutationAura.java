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
 * Transmutation Magic (Grey), 1.93.0 rebuild: the temporary armour (Aura.TRANSMUTATION), the ALCHEMIST'S GILDED PLATE. A set of eleven geo pieces cast in brass and dark
 * iron, engraved with transmutation circles whose lines glow teal-green: a circlet with a gem and swept-back fins (head), a breastplate with the great circle and a
 * gorget (body), hanging tassets (body), a shoulder lame with a circle standing on it that spins about its own axis (arms), a vambrace with the alchemist's glove and a
 * hovering ring at the elbow (arms), greaves with a knee guard and sabatons (legs), THE GREAT CIRCLE behind the back (three discs and six glyph stones turning against
 * each other, body) and a halo of two counter-turning rings over the head. Every piece is anchored on the model part it was built for (so it follows crouching, swimming
 * and swinging) and FORGES IN AND SHATTERS OUT exactly like the Djinn full equip ({@link EquipAnimation}): staggered by 0..12 ticks, flying in from its own side, fading
 * up and growing 85 to 100 percent over 8 ticks, shattering out over the last 18 ticks.
 * <p>Passes, in draw order (every one explicit): 1. the pieces, solid (entityCutoutNoCull) once forged, alpha blended (entityTranslucent) while they forge in or shatter
 * out, each with its own additive full-bright glow map (RenderType.eyes); 2. the glyph shell: the player's posed model once more, 4.5 percent larger, additive
 * (RenderType.eyes, full bright) with the engraved glyphs of textures/aura/transmutation_glyphs.png, so the lines run over the skin under the plate. Style 1 (the short
 * armour after a signature cast) draws the same set without the back circle and the halo. The head pieces are skipped for the local player in first person.</p>
 */
public final class TransmutationAura {
    private TransmutationAura() {}

    private static final GeoSpec HELM = GeoSpec.of("transmutation", "helm"), CHEST = GeoSpec.of("transmutation", "chest"), TASSET = GeoSpec.of("transmutation", "tasset"),
            PAUL_R = GeoSpec.of("transmutation", "pauldron_r"), PAUL_L = GeoSpec.of("transmutation", "pauldron_l"),
            VAMB_R = GeoSpec.of("transmutation", "vambrace_r"), VAMB_L = GeoSpec.of("transmutation", "vambrace_l"),
            GREAVE_R = GeoSpec.of("transmutation", "greave_r"), GREAVE_L = GeoSpec.of("transmutation", "greave_l"),
            BACK = GeoSpec.of("transmutation", "back"), HALO = GeoSpec.of("transmutation", "halo");
    private static ResourceLocation glyphs;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        glyphs = AuraRender.tex("aura/transmutation_glyphs");
        PlayerAuraClient.register(Aura.TRANSMUTATION, TransmutationAura::paint);
    }

    private static void paint(AuraContext c) {
        if (c.player().isInvisible()) return;
        Minecraft mc = Minecraft.getInstance();
        boolean self1p = c.player() == mc.player && mc.options.getCameraType().isFirstPerson();
        PlayerModel<?> m = c.model();
        float pulse = 0.78f + 0.22f * Mth.sin(c.age() * 0.12f);
        boolean full = c.style() != 1;
        // 1. the pieces, in the order they forge: head, body, arms, legs, back (delays 0 to 12 ticks)
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
        // 2. the glyph shell over the skin (additive, full bright)
        float shell = EquipAnimation.alpha(EquipAnimation.assembled(c, 2f));
        if (shell > 0.01f)
            AuraRender.modelBright(c, AuraRender.additive(glyphs), 1.045f, 1.03f, 1.045f, AuraRender.alpha(0xFFFFFFFF, 0.55f * shell * pulse));
    }

    /** The rest position (model pixels) of the vanilla player model's part the piece is anchored on: head and body at the origin, arms at (-+5, 2, 0), legs at (-+1.9, 12, 0). */
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
                AuraRender.alpha(0xFFFFFFFF, Math.min(1f, alpha * pulse * 0.8f)));
        pose.popPose();
    }
}
