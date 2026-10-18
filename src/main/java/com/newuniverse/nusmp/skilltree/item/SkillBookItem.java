package com.newuniverse.nusmp.skilltree.item;

import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Why: this is the item of the owner's mechanic "a specific Skill Book permanently unlocks that whole Skill Tree route". It is ONE item for every route: the data
 * component {@code nusmp:skill_book_id} names the book and the route whose {@code book} field is that id is the one it opens, because items cannot be registered by a
 * datapack but routes can, so a new magic (a JSON file) must not need a new Java class or a new item. Everything a player sees comes from the route, looked up in the
 * {@code RouteRegistry} (the server's loaded copy, or the copy the server synced to the client): the name is the route's {@code book_name}, the cover tint its colour,
 * the lore its {@code book_lore}; a stack whose route is unknown (a datapack was removed, or the copy has not arrived yet) is named plainly "Skill Book" and reads as
 * blank pages, never a crash. Reading is decided on the SERVER only ({@link #use} hands over to {@link BookUse}); the client just plays the arm swing, because the
 * server is the only one allowed to touch the player's tree (the rule of the whole skill-tree design: the server decides, the client shows). The item glints like
 * an enchanted book so a stack of knowledge is never mistaken for a plain item. {@link #stack} is the factory the creative tab, the dungeon drops and the Black Market
 * all use, so a book is built the same way everywhere.
 */
public class SkillBookItem extends Item {
    public SkillBookItem(Properties properties) {
        super(properties);
    }

    /**
     * A Skill Book for the book id of a route ({@code nusmp:fire_dragon_slayer_book}). An empty or null id gives an empty stack (there is no such thing as a book of
     * nothing); an id no route lists still gives a stack, which reads as blank pages, so a datapack that is removed later does not delete players' items.
     */
    public static ItemStack stack(String bookId) {
        if (bookId == null || bookId.isEmpty()) return ItemStack.EMPTY;
        ItemStack s = new ItemStack(SkillItems.SKILL_BOOK.get());
        s.set(SkillItems.BOOK_ID.get(), bookId);
        return s;
    }

    /** A Skill Book for a route (its {@code book} id). */
    public static ItemStack stackFor(SkillRoute route) { return route == null ? ItemStack.EMPTY : stack(route.book()); }

    @Override
    public Component getName(ItemStack stack) {
        SkillRoute route = RouteRegistry.byBook(SkillItems.bookIdOf(stack));
        if (route == null || route.bookName() == null || route.bookName().isEmpty()) return Component.translatableWithFallback("item.nusmp.skill_book", "Skill Book");
        return Component.literal(route.bookName()).withStyle(Style.EMPTY.withColor(route.color() & 0xFFFFFF));
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String bookId = SkillItems.bookIdOf(stack);
        SkillRoute route = RouteRegistry.byBook(bookId);
        if (route == null) {
            tooltip.add(Component.translatableWithFallback("tooltip.nusmp.skill_book.blank", "The pages are blank.").withStyle(ChatFormatting.GRAY));
        } else {
            if (route.bookLore() != null && !route.bookLore().isEmpty()) tooltip.add(Component.literal(route.bookLore()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            tooltip.add(Component.translatableWithFallback("tooltip.nusmp.skill_book.opens", "Opens the route: %s",
                    Component.literal(route.name()).withStyle(Style.EMPTY.withColor(route.color() & 0xFFFFFF))).withStyle(ChatFormatting.GRAY));
            if (route.duplicateSp() > 0) {
                tooltip.add(Component.translatableWithFallback("tooltip.nusmp.skill_book.duplicate", "A copy of a route you already know is worth %s Skill Points.",
                        route.duplicateSp()).withStyle(ChatFormatting.DARK_GRAY));
            }
            tooltip.add(Component.translatableWithFallback("tooltip.nusmp.skill_book.use", "Right-click to read it. The book is used up.").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (flag.isAdvanced() && !bookId.isEmpty()) tooltip.add(Component.literal(bookId).withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer server) BookUse.readSkillBook(server, stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
