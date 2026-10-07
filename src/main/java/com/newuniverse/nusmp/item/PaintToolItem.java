package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.book.PaintStudio;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 0.44 Painting Magic: the mana brush and the artist's palette a Painting grimoire manifests (see {@link PaintStudio}).
 * <ul>
 *   <li><b>Brush</b> (right-click): a sweep of the current paint; brushwork draws on aura.</li>
 *   <li><b>Palette</b> (right-click): the next paint; sneak + right-click: the paint that counters the last element that hit you.
 *       Held, it takes 40% off an element its paint counters.</li>
 * </ul>
 * A manifested one dissolves when dropped, when the grimoire is put away, or in anyone else's hands. The model shows the current
 * paint through custom model data (paint ordinal + 1).
 */
public class PaintToolItem extends Item {
    public enum Kind { BRUSH, PALETTE }

    public final Kind kind;

    public PaintToolItem(Kind kind) {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant());
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);
        if (player instanceof ServerPlayer sp) {
            if (kind == Kind.BRUSH) {
                if (PaintStudio.brushStroke(sp)) player.getCooldowns().addCooldown(this, 10);
            } else {
                PaintStudio.paletteUse(sp);
                player.getCooldowns().addCooldown(this, 6);
            }
        }
        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** A manifested tool that leaves its painter's hands melts into a puddle of paint. */
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (PaintStudio.ownerOf(stack) == null) return false;
        if (entity.level() instanceof ServerLevel sl)
            VfxSpawn.send(sl, VfxShape.PAINT_SPLAT, entity.position(), entity.position().add(0, 1, 0), 0xFF3A7BFF, 30, 0.6f);
        entity.discard();
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.nusmp." + (kind == Kind.BRUSH ? "paint_brush" : "paint_palette") + ".desc").withStyle(ChatFormatting.GRAY));
        int cmd = stack.has(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA)
                ? stack.get(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA).value() : 1;
        PaintStudio.Paint p = PaintStudio.Paint.of(cmd - 1);
        tip.add(Component.literal("Paint: " + p.label).withColor(p.color & 0xFFFFFF));
    }
}
