package com.newuniverse.nusmp.dungeon.nexus.relic;

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
import java.util.UUID;

/**
 * One of the Treasury's five relics (docs/living_dungeon.md section 7). The item carries the attunement in its data components and
 * explains itself in its tooltip; the effects live in the handlers ({@link RelicEvents}). In the hand, using a relic does the one thing it
 * can: sneak-use lets go of an attunement, a Chalice with raw meat in the off-hand is fed, the Page in the off-hand is cast.
 */
public class RelicItem extends Item {
    public final Relic relic;

    public RelicItem(Relic relic, Item.Properties props) {
        super(props);
        this.relic = relic;
    }

    /** What using it in a hand would do. */
    enum Use { NONE, RELEASE, FEED, CAST, WRONG_HAND }

    static Use useFor(Relic relic, Player p, InteractionHand hand, ItemStack stack) {
        if (hand == InteractionHand.MAIN_HAND && p.isShiftKeyDown() && Attunement.attunedToMe(stack, p)) return Use.RELEASE;
        if (relic == Relic.GAMBLERS_GRIMOIRE_PAGE) return hand == InteractionHand.OFF_HAND ? Use.CAST : Use.WRONG_HAND;
        if (relic == Relic.GLUTTONS_CHALICE && hand == InteractionHand.MAIN_HAND && ChaliceHandler.isRawMeat(p.getOffhandItem())) return Use.FEED;
        return Use.NONE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Use use = useFor(relic, player, hand, stack);
        if (use == Use.NONE) return InteractionResultHolder.pass(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        switch (use) {
            case RELEASE -> {
                Attunement.release(sp, stack);
                sp.displayClientMessage(Component.literal("You let go of " + relic.label + ": it is unattuned.").withStyle(ChatFormatting.GRAY), true);
            }
            case FEED -> ChaliceHandler.feedByHand(sp, stack);
            case CAST -> GamblerPage.use(sp, stack);
            case WRONG_HAND -> sp.displayClientMessage(Component.literal("The page answers only from the off-hand.").withStyle(ChatFormatting.GRAY), true);
            default -> { }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) { return stack.get(RelicComponents.ATTUNED_TO.get()) != null; }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal(relic.slot == Relic.Slot.OFF_HAND ? "Artifact - held in the off-hand" : "Wondrous item - worn in the " + relic.slot.label + " slot")
                .withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal(RelicText.effect(relic)).withStyle(ChatFormatting.AQUA));
        String curse = RelicText.curse(relic);
        if (curse != null) tip.add(Component.literal("Curse - " + relic.curse + ": " + curse).withStyle(ChatFormatting.RED));
        else tip.add(Component.literal("No curse.").withStyle(ChatFormatting.DARK_GREEN));
        String hint = RelicText.hint(relic);
        if (hint != null) tip.add(Component.literal(hint).withStyle(ChatFormatting.DARK_AQUA));
        state(stack, tip);
        UUID to = stack.get(RelicComponents.ATTUNED_TO.get());
        String name = stack.get(RelicComponents.ATTUNED_NAME.get());
        if (to != null) {
            tip.add(Component.literal("Attuned to " + (name == null ? "someone" : name)).withStyle(ChatFormatting.GOLD));
            tip.add(Component.literal("It works only for the one it is attuned to. Sneak-use it to let go.").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tip.add(Component.literal("Unattuned: hold and sneak for 10 s").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** The lines only some relics have: the Sheath's charge, the Chalice's hunger. */
    private void state(ItemStack stack, List<Component> tip) {
        if (relic == Relic.DEVIL_BOUND_SHEATH) {
            Integer charge = stack.get(RelicComponents.CHARGE.get());
            int c = charge == null ? 0 : charge;
            tip.add(Component.literal("Charge: " + c + " / " + SheathMath.MAX + (SheathMath.full(c) ? "  (full: strike!)" : "")).withStyle(ChatFormatting.LIGHT_PURPLE));
        } else if (relic == Relic.GLUTTONS_CHALICE) {
            Integer phase = stack.get(RelicComponents.HUNGER.get());
            HungerClock.Phase[] all = HungerClock.Phase.values();
            HungerClock.Phase ph = phase == null || phase < 0 || phase >= all.length ? HungerClock.Phase.FED : all[phase];
            String line = switch (ph) {
                case FED -> "It is fed.";
                case STIRRING -> "It stirs: it will want feeding soon.";
                case HUNGRY -> "It is hungry. Feed it before it feeds on you.";
                case STARVING -> "It is starving, and drinks from its wearer!";
            };
            tip.add(Component.literal(line).withStyle(ph == HungerClock.Phase.FED ? ChatFormatting.GREEN : ChatFormatting.DARK_RED));
        }
    }
}
