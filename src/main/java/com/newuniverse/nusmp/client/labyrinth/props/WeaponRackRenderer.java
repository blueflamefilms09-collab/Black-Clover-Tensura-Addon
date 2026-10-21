package com.newuniverse.nusmp.client.labyrinth.props;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.labyrinth.props.RackLayout;
import com.newuniverse.nusmp.labyrinth.props.WeaponRackBlock;
import com.newuniverse.nusmp.labyrinth.props.WeaponRackBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the picture of a weapon rack, drawn here and not by an entity. A rack is a block with a model (the frame) and five item stacks; this renderer draws the stacks, and only as many as the
 * distance deserves ({@link RackLayout#visibleSlots}: five up to 14 blocks, three up to 28, two up to 40, none beyond, which is also the view distance the engine culls by). Nothing is allocated by
 * this class in a frame: the slot poses are flat float arrays ({@link RackLayout}), the camera position is read from the dispatcher's own vector, the item is drawn with the vanilla item
 * renderer in its FIXED display context (the transform of an item frame, so every item, modded ones too, already has a sensible pose), and a blade-like sprite (sword, axe, bow, trident ...) is
 * rolled 45 degrees to hang upright from the rail while a shield, a helm or a plate is left as it is. The rack is turned by the block's facing; the light is the block's own.
 */
public final class WeaponRackRenderer implements BlockEntityRenderer<WeaponRackBlockEntity> {
    private final ItemRenderer items;
    private final BlockEntityRenderDispatcher dispatcher;

    public WeaponRackRenderer(BlockEntityRendererProvider.Context ctx) {
        this.items = ctx.getItemRenderer();
        this.dispatcher = ctx.getBlockEntityRenderDispatcher();
    }

    @Override
    public int getViewDistance() { return (int) RackLayout.VIEW_DISTANCE; }

    @Override
    public void render(WeaponRackBlockEntity be, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (be.count() == 0 || be.getLevel() == null) return;
        int visible = RackLayout.SLOTS;
        if (dispatcher != null && dispatcher.camera != null) {
            Vec3 cam = dispatcher.camera.getPosition();
            double dx = cam.x - (be.getBlockPos().getX() + 0.5), dy = cam.y - (be.getBlockPos().getY() + 0.8), dz = cam.z - (be.getBlockPos().getZ() + 0.5);
            visible = RackLayout.visibleSlots(dx * dx + dy * dy + dz * dz);
        }
        if (visible == 0) return;
        float turn = -be.getBlockState().getValue(WeaponRackBlock.FACING).toYRot();
        long seed = be.getBlockPos().asLong();
        pose.pushPose();
        pose.translate(0.5, 0.0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(turn));
        pose.translate(-0.5, 0.0, -0.5);
        for (int i = 0; i < visible; i++) {
            ItemStack stack = be.get(i);
            if (stack.isEmpty()) continue;
            pose.pushPose();
            pose.translate(RackLayout.x(i), RackLayout.y(i), RackLayout.z(i));
            float roll = RackLayout.roll(i);
            if (i < RackLayout.BAR && bladeLike(stack)) roll += RackLayout.DIAGONAL_FIX;
            if (roll != 0f) pose.mulPose(Axis.ZP.rotationDegrees(roll));
            float s = RackLayout.scale(i);
            pose.scale(s, s, s);
            items.renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, be.getLevel(), (int) (seed + i));
            pose.popPose();
        }
        pose.popPose();
    }

    /** Is this the kind of item whose sprite is drawn diagonally (a tool, a blade, a bow)? Tests are instanceof only: no allocation. */
    private static boolean bladeLike(ItemStack stack) {
        net.minecraft.world.item.Item it = stack.getItem();
        return it instanceof TieredItem || it instanceof BowItem || it instanceof CrossbowItem || it instanceof TridentItem;
    }
}
