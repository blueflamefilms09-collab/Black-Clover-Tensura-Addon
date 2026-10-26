package com.newuniverse.nusmp.skilltree.item;

import com.newuniverse.nusmp.skilltree.rarity.BookRarity;
import com.newuniverse.nusmp.skilltree.unique.LegacyIds;
import com.newuniverse.nusmp.skilltree.unique.LineIndex;
import com.newuniverse.nusmp.skilltree.unique.UniqueBooks;
import com.newuniverse.nusmp.skilltree.unique.UniqueSkills;
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
 * Why: this is the item of the owner's mechanic "reading a Skill Book teaches the UNIQUE skill of that book". It is ONE item for every book: the data component {@code nusmp:skill_book_id} names the book
 * ({@code nusmp:lady_luck_book}), and the unique skill registered for that book ({@link UniqueSkills}) is the one it teaches. Everything a player sees comes from the jar's own table of books
 * ({@link LineIndex}): the name is the book's name, the cover tint its colour, the lore its lore, so a book looks right on both sides and in any chest, with no data pack and no server round trip. A stack whose
 * book is unknown (a book id from a removed pack) reads as blank pages, never a crash; a stack of a REMOVED magic (an old stack a world still holds) says so and is swapped for a book the reader does not own
 * when read. Reading is decided on the SERVER only ({@link #use} hands over to {@link BookUse}); the client just plays the arm swing, because the server is the only one allowed to touch the player's skills. The
 * item glints like an enchanted book only when it is Epic or Mythic (3.26.0; the old always-glint is the client setting {@code legacyLook}), and its name wears the rarity colour. {@link #stack} is the factory the creative tab, the dungeon drops and the Black Market all use, so a book is built the
 * same way everywhere.
 */
public class SkillBookItem extends Item {
    public SkillBookItem(Properties properties) {
        super(properties);
    }

    /**
     * A Skill Book for a book id ({@code nusmp:lady_luck_book}). An empty or null id gives an empty stack (there is no such thing as a book of nothing); an id no book lists still gives a stack, which reads as blank pages,
     * so a removed pack does not delete players' items.
     */
    public static ItemStack stack(String bookId) {
        if (bookId == null || bookId.isEmpty()) return ItemStack.EMPTY;
        ItemStack s = new ItemStack(SkillItems.SKILL_BOOK.get());
        s.set(SkillItems.BOOK_ID.get(), bookId);
        return s;
    }

    /** A Skill Book for a line of the table. */
    public static ItemStack stackFor(LineIndex.Line line) { return line == null ? ItemStack.EMPTY : stack(line.book()); }

    @Override
    public Component getName(ItemStack stack) {
        String bookId = SkillItems.bookIdOf(stack);
        LineIndex.Line line = UniqueSkills.lineOfBook(bookId);
        if (line != null && !line.bookName().isEmpty()) return Component.literal(line.bookName()).withStyle(Style.EMPTY.withColor(SkillItems.rarityOf(stack).color()));      // 3.26.0: the name wears the RARITY colour, the line's own colour stays on the "Teaches" row
        if (LegacyIds.isRemovedBook(bookId)) return Component.translatableWithFallback("item.nusmp.skill_book.faded", "Faded Skill Book").withStyle(ChatFormatting.DARK_GRAY);
        return Component.translatableWithFallback("item.nusmp.skill_book", "Skill Book");
    }

    /** 3.26.0: epic and mythic books glint; the client setting {@code legacyLook} (read through {@link LegacyLook}, never a client class) brings back the old always-glint. */
    @Override
    public boolean isFoil(ItemStack stack) { return LegacyLook.enabled() || SkillItems.rarityOf(stack).glint(); }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String bookId = SkillItems.bookIdOf(stack);
        LineIndex.Line line = UniqueSkills.lineOfBook(bookId);
        if (line != null) {
            BookRarity rarity = SkillItems.rarityOf(stack);
            tooltip.add(Component.translatableWithFallback("tooltip.nusmp.skill_book.rarity", "Rarity: %s",
                    Component.translatableWithFallback(rarity.langKey(), rarity.label()).withStyle(Style.EMPTY.withColor(rarity.color()))).withStyle(ChatFormatting.GRAY));
            if (rarity == BookRarity.MYTHIC) tooltip.add(Component.translatableWithFallback("tooltip.nusmp.skill_book.mythic_tag", "Mythic").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            if (!line.lore().isEmpty()) tooltip.add(Component.literal(line.lore()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            tooltip.add(Component.translatableWithFallback("tooltip.nusmp.skill_book.teaches", "Teaches the unique skill: %s",
                    Component.literal(line.name()).withStyle(Style.EMPTY.withColor(line.color()))).withStyle(ChatFormatting.GRAY));
            if (!line.franchise().isEmpty()) {
                tooltip.add(Component.translatableWithFallback("tooltip.nusmp.skill_book.franchise", "From: %s",
                        Component.translatableWithFallback("franchise.nusmp." + line.franchise(), line.franchise())).withStyle(ChatFormatting.DARK_AQUA));
            }
            tooltip.add(Component.translatableWithFallback("tooltip.nusmp.skill_book.use", "Right-click to read it. The book is used up.").withStyle(ChatFormatting.DARK_GRAY));
        } else if (LegacyIds.isRemovedBook(bookId)) {
            tooltip.add(Component.translatableWithFallback("tooltip.nusmp.skill_book.removed", "The magic in this book was removed. Right-click to swap it for another book.").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatableWithFallback("tooltip.nusmp.skill_book.blank", "The pages are blank.").withStyle(ChatFormatting.GRAY));
        }
        if (flag.isAdvanced() && !bookId.isEmpty()) tooltip.add(Component.literal(bookId).withStyle(ChatFormatting.DARK_GRAY));
    }

    /** True when the (client or server) player already owns the skill of this stack's book; the tooltip handler of the client uses it. */
    public static boolean ownedBy(Player player, ItemStack stack) {
        LineIndex.Line line = UniqueSkills.lineOfBook(SkillItems.bookIdOf(stack));
        return line != null && UniqueBooks.owns(player, line.bare());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer server) BookUse.readSkillBook(server, stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
