package com.newuniverse.nusmp.skilltree.item;

import net.minecraft.ChatFormatting;
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

/**
 * Why: Skill Points are the currency of the whole tree, and the owner wants cheap ways to get a few of them besides the big route books: the little primers on the
 * Black Market's shelf (Beginner Archery, Elemental, Magic Booster, the Jack-in-the-Box gamble) and the better ones that fall in the dungeon. Like the Skill Book this is
 * ONE item whose data components say what it is ({@code nusmp:sp_kind} the primer, {@code nusmp:sp_amount} the points), so five sprites, five names and five flavour
 * lines need no extra registry entries and a datapack's shop can sell a stack with another number. The kind table is {@link SpKind}; what reading does is
 * {@link BookRules#spBookAmount} (the amount, the gamble, the clamps) carried out by {@link BookUse}, on the SERVER only, through {@code SkillTreeApi.addSp} so the
 * cap, the quest hook and the sync are done once, by the one door every Skill Point goes through. Names and tooltips are translatable with an English fallback in the
 * code, so a stack is readable even before the language file is merged. {@link #stack} is the factory the creative tab, the dungeon drops and the shop use.
 */
public class SPBookItem extends Item {
    public SPBookItem(Properties properties) {
        super(properties);
    }

    /**
     * An SP Book of one kind, with the kind's own amount (the floor of the roll for the gamble). An unknown or empty kind gives an EMPTY stack, on purpose: a typo in a
     * shop file must surface as a missing item, not as a silently sold book of something else.
     */
    public static ItemStack stack(String kindId) {
        SpKind kind = SpKind.byId(kindId);
        return kind == null ? ItemStack.EMPTY : stack(kind);
    }

    /** An SP Book of one kind, with the kind's own amount. */
    public static ItemStack stack(SpKind kind) {
        if (kind == null) return ItemStack.EMPTY;
        ItemStack s = new ItemStack(SkillItems.SP_BOOK.get());
        s.set(SkillItems.SP_KIND.get(), kind.id());
        s.set(SkillItems.SP_AMOUNT.get(), kind.amount());
        return s;
    }

    /** A plain SP Book that gives exactly {@code amount} Skill Points and belongs to no kind (an admin's or a quest reward's). A non-positive amount gives an empty stack. */
    public static ItemStack ofAmount(int amount) {
        if (amount <= 0) return ItemStack.EMPTY;
        ItemStack s = new ItemStack(SkillItems.SP_BOOK.get());
        s.set(SkillItems.SP_AMOUNT.get(), Math.min(amount, BookRules.MAX_AMOUNT));
        return s;
    }

    @Override
    public Component getName(ItemStack stack) {
        SpKind kind = SpKind.byId(SkillItems.spKindOf(stack));
        if (kind == null) return Component.translatableWithFallback("item.nusmp.sp_book", "SP Book");
        return Component.translatableWithFallback("item.nusmp.sp_book." + kind.id(), displayName(kind));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SpKind kind = SpKind.byId(SkillItems.spKindOf(stack));
        int amount = SkillItems.spAmountOf(stack);
        if (kind != null) tooltip.add(Component.translatableWithFallback("tooltip.nusmp.sp_book." + kind.id(), flavour(kind)).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (kind != null && kind.isGamble()) {
            int floor = amount >= 0 ? Math.max(JackInTheBox.FLOOR, Math.min(JackInTheBox.TOP, amount)) : JackInTheBox.FLOOR;
            tooltip.add(Component.translatableWithFallback("tooltip.nusmp.sp_book.gamble", "Gives %s to %s Skill Points, and rarely %s.",
                    floor, JackInTheBox.TOP, JackInTheBox.JACKPOT).withStyle(ChatFormatting.GOLD));
        } else {
            int shown = amount >= 0 ? amount : kind == null ? 0 : kind.amount();
            if (shown > 0) tooltip.add(Component.translatableWithFallback("tooltip.nusmp.sp_book.amount", "Gives %s Skill Points.", shown).withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatableWithFallback("tooltip.nusmp.sp_book.use", "Right-click to read it. The book is used up.").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer server) BookUse.readSpBook(server, stack, server.getRandom()::nextDouble);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** The English name of a kind (the fallback of the translatable name, and the line the language file carries). */
    static String displayName(SpKind kind) {
        return switch (kind) {
            case BEGINNER_ARCHERY -> "Beginner Archery Primer";
            case ELEMENTAL_PRIMER -> "Elemental Primer";
            case MAGIC_BOOSTER -> "Magic Booster";
            case JACK_IN_THE_BOX -> "Jack-in-the-Box";
            case GREATER_PRIMER -> "Greater Primer";
        };
    }

    /** The English flavour line of a kind. */
    static String flavour(SpKind kind) {
        return switch (kind) {
            case BEGINNER_ARCHERY -> "Chapter one: do not shoot yourself in the foot. Cheap, common, and surprisingly useful.";
            case ELEMENTAL_PRIMER -> "A student's guide to the four elements. Half the pages are scorched, the other half are damp.";
            case MAGIC_BOOSTER -> "Dense drills, careful diagrams and one stubborn stain. Reading it to the end is the hard part.";
            case JACK_IN_THE_BOX -> "Something rattles inside. Maybe a little. Maybe a lot.";
            case GREATER_PRIMER -> "A thick, well-thumbed grimoire of advanced drills, expensive to copy and worth every coin.";
        };
    }
}
