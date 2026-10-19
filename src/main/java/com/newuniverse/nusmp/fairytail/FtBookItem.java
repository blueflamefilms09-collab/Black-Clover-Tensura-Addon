package com.newuniverse.nusmp.fairytail;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Why: the owner wants every Fairy Tail skill obtained from a skill book: using the book teaches the skill directly (no tree, no points).
 * One item, {@code fairy_tail_skill_book}, carries the skill in its custom data ({@code Skill}: a {@link FtLine} id), so a new skill is a
 * new table row and not a new item. The book is used up only after the skill was really learned; a player who already has the skill keeps
 * the book. Dungeon vaults, loot tables and the admin command hand these books out.
 */
public class FtBookItem extends Item {
    public FtBookItem() { super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)); }

    public static ItemStack of(FtLine line) {
        ItemStack s = new ItemStack(FairyTailSystem.BOOK.get());
        CompoundTag tag = new CompoundTag();
        tag.putString("Skill", line.id);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return s;
    }

    /** The line a book teaches, or null for a blank or unknown book. */
    public static FtLine lineOf(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        return d == null ? null : FtLine.byId(d.copyTag().getString("Skill"));
    }

    @Override
    public Component getName(ItemStack stack) {
        FtLine line = lineOf(stack);
        Component base = Component.translatableWithFallback("item.nusmp.fairy_tail_skill_book", "Fairy Tail Skill Book");
        if (line == null) return base;
        return Component.translatableWithFallback("item.nusmp.fairy_tail_skill_book.of", "Skill Book: %s", Component.translatableWithFallback("nusmp.skill." + line.id, line.name));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return InteractionResultHolder.success(stack);
        FtLine line = lineOf(stack);
        String why = line == null ? "This book is blank." : learn(p, line);
        if (why != null) {
            p.displayClientMessage(Component.literal(why).withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    /** Teaches the skill. Null on success, otherwise the reason it was refused (nothing was changed). */
    public static String learn(ServerPlayer p, FtLine line) {
        ManasSkill skill = FairyTailSystem.skill(line);
        if (skill == null) return "That skill is not available.";
        var skills = SkillAPI.getSkillsFrom(p);
        if (skills.getSkill(skill.getRegistryName()).isPresent()) return "You already know " + line.name + ".";
        FtLine up = line.evolvesInto();
        if (up != null && skills.getSkill(FairyTailSystem.skill(up).getRegistryName()).isPresent()) return "You already hold its evolution.";
        boolean ok = skills.learnSkill(skill.createDefaultInstance(),
                Component.translatableWithFallback("nusmp.fairytail.learned", "You learned %s.", Component.translatableWithFallback("nusmp.skill." + line.id, line.name)).withStyle(ChatFormatting.GOLD));
        return ok ? null : "The skill could not be learned.";
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        FtLine line = lineOf(stack);
        if (line == null) return;
        tooltip.add(Component.literal(line.ultimate ? "Ultimate Skill" : "Unique Skill").withStyle(line.ultimate ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GOLD));
        tooltip.add(Component.literal(line.modes() + " spells; use to learn").withStyle(ChatFormatting.GRAY));
    }
}
