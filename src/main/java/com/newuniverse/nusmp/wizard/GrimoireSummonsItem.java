package com.newuniverse.nusmp.wizard;

import com.newuniverse.nusmp.multiverse.WorldSites;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/** The summons to the Grimoire Tower. Read it to learn where the nearest tower is; show it to the Tower Wizard to claim your grimoire. */
public class GrimoireSummonsItem extends Item {
    public GrimoireSummonsItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return InteractionResultHolder.pass(stack);
        if (com.newuniverse.nusmp.multiverse.MultiverseConfig.get(com.newuniverse.nusmp.multiverse.MultiverseConfig.WIZARD_MODE)) {
            if (WizardLibrary.inLibrary(p)) {
                p.displayClientMessage(Component.literal("Speak to the Tower Wizard in the middle of the room (right-click him).").withStyle(ChatFormatting.GOLD), true);
                return InteractionResultHolder.success(stack);
            }
            if (WizardLibrary.enter(p)) return InteractionResultHolder.success(stack);       // 1.11.0: the letter carries you to his library
        }
        Optional<WorldSites.Site> site = WorldSites.get(p.getServer()).nearest(WorldSites.Kind.TOWER, level.dimension(), p.blockPosition());
        if (site.isEmpty()) {
            p.displayClientMessage(Component.literal("The summons names a Grimoire Tower, but you do not know where it stands yet. Ask around, or find one.").withStyle(ChatFormatting.GRAY), true);
        } else {
            BlockPos t = site.get().pos();
            int dx = t.getX() - p.getBlockX(), dz = t.getZ() - p.getBlockZ();
            String dir = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? "east" : "west") : (dz > 0 ? "south" : "north");
            p.displayClientMessage(Component.literal("The Grimoire Tower is " + (int) Math.sqrt((double) dx * dx + (double) dz * dz) + " blocks to the " + dir
                    + " (" + t.getX() + ", " + t.getZ() + "). Speak to the Tower Wizard.").withStyle(ChatFormatting.GOLD), true);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("You are summoned to the Grimoire Tower to claim your grimoire.").withStyle(ChatFormatting.GOLD));
        tip.add(Component.literal("  Right-click: the letter carries you to the Tower Wizard's library. Speak to him there.").withStyle(ChatFormatting.GRAY));
    }
}
