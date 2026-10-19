package com.newuniverse.nusmp.celestial;

import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Why: Celestial Spirit Magic is a Tensura Unique skill and it is learned from this book, not from a grimoire or a skill tree: using it teaches the skill directly (a
 * second copy is refused and the book is kept). The book is used up only after the skill was learned.
 */
public final class CelestialSkillBookItem extends Item {
    public CelestialSkillBookItem() { super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return InteractionResultHolder.success(stack);
        var skills = SkillAPI.getSkillsFrom(p);
        var skill = NUSkills.CELESTIAL_SPIRIT_MAGIC.get();
        if (skills.getSkill(skill.getRegistryName()).isPresent()) {
            p.displayClientMessage(Component.translatableWithFallback("message.nusmp.celestial.known", "You already know Celestial Spirit Magic.").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResultHolder.fail(stack);
        }
        ManasSkillInstance inst = skill.createDefaultInstance();
        boolean ok = skills.learnSkill(inst, Component.translatableWithFallback("message.nusmp.celestial.learned", "The stars answer: Celestial Spirit Magic is yours.").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        if (!ok) {
            p.displayClientMessage(Component.translatableWithFallback("message.nusmp.celestial.not_learned", "The book stays shut.").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatableWithFallback("tooltip.nusmp.celestial.book", "Teaches the Unique skill Celestial Spirit Magic.").withStyle(ChatFormatting.AQUA));
        tip.add(Component.translatableWithFallback("tooltip.nusmp.celestial.book2", "Call the spirits of the zodiac with their keys; Star Dress forms open with mastery.").withStyle(ChatFormatting.GRAY));
    }
}
