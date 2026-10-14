package com.newuniverse.nusmp.charter;

import com.newuniverse.nusmp.relics.Grade;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * A Charter Grimoire: the book of one boss, a world-exclusive drop (see {@link ModWorldDropData}). Held in a hand or worn in a Curios charm slot it
 * lends that boss's Tensura skills (see {@link CharterMirror}); the lease ends by itself a few seconds after the book is put away and nothing of
 * the holder's own is ever touched. Its name is drawn in the Tensura grade colour of the book. Dropped, it cannot burn, blow up or despawn, and one
 * that falls out of the world is lifted back to the surface.
 */
public class CharterGrimoireItem extends Item {
    private final CharterTable.Boss boss;

    public CharterGrimoireItem(CharterTable.Boss boss, Item.Properties props) {
        super(props);
        this.boss = boss;
    }

    public CharterTable.Boss boss() { return boss; }

    private Grade grade() {
        try { return Grade.valueOf(boss.grade()); } catch (IllegalArgumentException unknown) { return Grade.LEGEND; }
    }

    @Override
    public Component getName(ItemStack stack) { return super.getName(stack).copy().withStyle(grade().colour()); }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!(entity.level() instanceof ServerLevel sl)) return false;
        entity.setUnlimitedLifetime();
        entity.setInvulnerable(true);
        if (entity.getY() < sl.getMinBuildHeight() - 8) {
            int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING, entity.getBlockX(), entity.getBlockZ());
            BlockPos up = new BlockPos(entity.getBlockX(), Math.max(y, sl.getMinBuildHeight()) + 1, entity.getBlockZ());
            entity.setPos(up.getX() + 0.5, up.getY(), up.getZ() + 0.5);
            entity.setDeltaMovement(Vec3.ZERO);
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        Grade g = grade();
        tip.add(Component.literal(g.label() + "  " + g.stars()).withStyle(g.colour()));
        tip.add(Component.literal(boss.lore()).withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Hold it, or wear it in a charm slot: it lends").withStyle(ChatFormatting.DARK_GRAY));
        tip.add(Component.literal("these skills while you do, and never takes any.").withStyle(ChatFormatting.DARK_GRAY));
        for (String s : boss.skills()) tip.add(Component.literal(" - " + CharterTable.pretty(s)).withStyle(ChatFormatting.AQUA));
        CharterClaims.Claim c = CharterClaims.get(boss.id());
        if (c == null) tip.add(Component.literal("Unclaimed").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        else tip.add(Component.literal("Claimed by " + c.name() + " on day " + c.day()).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tip.add(Component.literal("Only one exists in this world.").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }
}
