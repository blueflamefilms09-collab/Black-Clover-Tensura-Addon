package com.newuniverse.nusmp.client.relics.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.relics.fx.RelicFxOptions;
import com.newuniverse.nusmp.client.relics.worn.lib.WornLibDraw;
import com.newuniverse.nusmp.relics.RelicBearer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

import java.util.HashSet;
import java.util.Set;

/**
 * Why: a relic that is not a weapon was a flat 16x16 sprite the moment the player took it in hand, dropped it or hung it in a frame, while the weapons beside it are real
 * 3D objects. This is the one item renderer that gives every such relic (Curios relics, consumables, relic armour) a held form: the models of the worn library, or a hand
 * model of its own, placed by a hold style from transforms.txt ({@link HandTransforms}: a necklace hangs from the fingers, a hood is held by the brim, a flask is gripped).
 * In the inventory (and whenever there is no form, the option is off, or anything fails) it draws the old sprite, stored as the model {@code item/<id>_icon}, so the
 * item can never draw nothing and never crashes the frame. The item json of such an item is {@code builtin/entity} with no display block: everything the display block
 * did is a line of the sprite style in transforms.txt, so the placement is data, not Java per item.
 */
public final class HandItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static HandItemRenderer instance;
    private static final Quaternionf Q = new Quaternionf();
    private static final float DEG = (float) (Math.PI / 180.0);
    private static final Set<String> REPORTED = new HashSet<>();

    private HandItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static HandItemRenderer instance() {
        if (instance == null) instance = new HandItemRenderer();
        return instance;
    }

    static ModelResourceLocation icon(String path) { return ModelResourceLocation.standalone(HandModels.rl("item/" + path + "_icon")); }

    /** The context of the vanilla display context, or null for one that is drawn as the sprite does (none, GUI). */
    static HandTransforms.Ctx ctxOf(ItemDisplayContext c) {
        return switch (c) {
            case GROUND -> HandTransforms.Ctx.GROUND;
            case FIXED -> HandTransforms.Ctx.FIXED;
            case THIRD_PERSON_RIGHT_HAND -> HandTransforms.Ctx.TP_RIGHT;
            case THIRD_PERSON_LEFT_HAND -> HandTransforms.Ctx.TP_LEFT;
            case FIRST_PERSON_RIGHT_HAND -> HandTransforms.Ctx.FP_RIGHT;
            case FIRST_PERSON_LEFT_HAND -> HandTransforms.Ctx.FP_LEFT;
            case HEAD -> HandTransforms.Ctx.HEAD;
            default -> null;
        };
    }

    /** Applies a display transform the way a model json display block is applied (translation in sixteenths, rotation about x, y, z, then scale). */
    private static void apply(PoseStack pose, HandTransforms.Xf x) {
        pose.translate(x.tx() / 16f, x.ty() / 16f, x.tz() / 16f);
        pose.mulPose(Q.rotationXYZ(x.rx() * DEG, x.ry() * DEG, x.rz() * DEG));
        pose.scale(x.s(), x.s(), x.s());
    }

    /**
     * Puts the pose where a flat sprite's display block would have put it (the sprite style of transforms.txt), for code that draws an older form which expects the item json's
     * display block to have been applied. Leaves the pose as the older form expects it (the cube corner at the origin).
     */
    public static void applySpritePreset(PoseStack pose, ItemDisplayContext ctx) {
        HandTransforms.Ctx hc = ctxOf(ctx);
        HandTransforms.Style s = HandModels.transforms().style(HandTransforms.SPRITE);
        if (hc == null || s == null) return;
        pose.translate(0.5f, 0.5f, 0.5f);
        apply(pose, s.xf(hc));
        pose.translate(-0.5f, -0.5f, -0.5f);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        HandForms.Form form = null;
        HandTransforms.Ctx hc = ctxOf(ctx);
        if (hc != null && stack.getItem() instanceof RelicBearer b && RelicFxOptions.models() && RelicFxOptions.handModels()) {
            try {
                form = HandForms.of(b.relic());
            } catch (RuntimeException ex) {
                if (REPORTED.add(path)) LogUtils.getLogger().error("[nusmp] the hand form of relic {} failed; its sprite is used", path, ex);
            }
        }
        if (form != null && form.ensemble().pieces.isEmpty()) form = null;
        if (form != null) {
            try {
                drawForm(form, hc, pose, buffers, light);
                return;
            } catch (RuntimeException ex) {
                if (REPORTED.add(path)) LogUtils.getLogger().error("[nusmp] drawing the hand form of relic {} failed; its sprite is used", path, ex);
            }
        }
        sprite(stack, hc, path, pose, buffers, light, overlay);
    }

    /** The flat sprite: in the inventory as it is, anywhere else placed by the sprite style (the transforms a vanilla generated item has). */
    private void sprite(ItemStack stack, HandTransforms.Ctx hc, String path, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel icon = mc.getModelManager().getModel(icon(path));
        pose.pushPose();
        pose.translate(0.5f, 0.5f, 0.5f);                                              // render() translates by -0.5 again
        if (hc != null) {
            HandTransforms.Style s = HandModels.transforms().style(HandTransforms.SPRITE);
            if (s != null) apply(pose, s.xf(hc));
        }
        mc.getItemRenderer().render(stack, ItemDisplayContext.GUI, false, pose, buffers, light, overlay, icon);
        pose.popPose();
    }

    /** The relic's models in the pose its style gives for this context. */
    private static void drawForm(HandForms.Form form, HandTransforms.Ctx hc, PoseStack pose, MultiBufferSource buffers, int light) {
        Minecraft mc = Minecraft.getInstance();
        float time = mc.level == null ? 0f : mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(false);
        HandTransforms.Fit fit = form.style().fit();
        float[] b = form.ensemble().bounds();
        float ex = b[3] - b[0], ey = b[4] - b[1], ez = b[5] - b[2];
        float sc = fit.size() / Math.max(0.001f, Math.max(ex, Math.max(ey, ez)));
        boolean inHand = hc == HandTransforms.Ctx.TP_RIGHT || hc == HandTransforms.Ctx.TP_LEFT || hc == HandTransforms.Ctx.FP_RIGHT || hc == HandTransforms.Ctx.FP_LEFT;
        pose.pushPose();
        try {
            pose.translate(0.5f, 0.5f, 0.5f);
            apply(pose, form.style().xf(hc));
            if (fit.prx() != 0f || fit.pry() != 0f || fit.prz() != 0f) pose.mulPose(Q.rotationXYZ(fit.prx() * DEG, fit.pry() * DEG, fit.prz() * DEG));
            if (inHand) {                                                              // the hold point and the sway are about the hand, so only in a hand
                if (fit.sway() != 0f) pose.mulPose(Q.rotationZ(fit.sway() * DEG * (float) Math.sin(time * 0.11f)));
                pose.translate(-fit.hx() * ex * sc * 0.5f, -fit.hy() * ey * sc * 0.5f, -fit.hz() * ez * sc * 0.5f);
            }
            pose.mulPose(Axis.XP.rotationDegrees(180f));                              // the library's layer space has y down: stand the object upright, its front to +z
            pose.scale(sc, sc, sc);                                                    // one unit is now one model pixel
            pose.translate(-(b[0] + b[3]) * 0.5f, -(b[1] + b[4]) * 0.5f, -(b[2] + b[5]) * 0.5f);
            WornLibDraw.drawItem(form.ensemble(), pose, buffers, light, time);
        } finally {
            pose.popPose();
        }
    }
}
