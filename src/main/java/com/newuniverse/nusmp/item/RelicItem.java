package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.blackclover.GrimoireCover;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.Kingdom;
import com.newuniverse.nusmp.book.GrimoireBook;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/** Black Clover relics. Each one does something real; descriptions come from lang + config numbers. */
public class RelicItem extends Item {
    public enum Kind {
        COMMUNICATION(1), RUNE_STONE(16), SPIRIT_CHARM(16), BOND_THREAD(16), FORTUNE_DIE(16), GRIMOIRE_CHAIN(1),
        ANTI_BIRD(1), RECOVERY_SALVE(16), CONSENT(1), DEVIL_CONTRACT(1);
        final int stack;
        Kind(int stack) { this.stack = stack; }
    }
    public final Kind kind;

    public RelicItem(Kind kind, Item.Properties props) { super(props); this.kind = kind; }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.nusmp." + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(this).getPath() + ".desc").withStyle(ChatFormatting.GRAY));
        if (kind == Kind.FORTUNE_DIE) tip.add(Component.literal(com.newuniverse.nusmp.NUConfig.GEAR_FORTUNE_SUCCESS.get() + "% five-sided, "
                + com.newuniverse.nusmp.NUConfig.GEAR_FORTUNE_CRACK.get() + "% cracked").withStyle(ChatFormatting.GOLD));
        if (kind == Kind.COMMUNICATION && stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA))
            tip.add(Component.literal("Linked channel: " + MagicGear.channel(stack)).withStyle(ChatFormatting.AQUA));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return InteractionResultHolder.pass(stack);
        switch (kind) {
            case FORTUNE_DIE -> {
                var g = GrimoirePages.grimoireOf(p);
                if (g.isEmpty() || GrimoirePages.coverOf(g.get()) != GrimoireCover.DIAMOND) { GrimoireBook.fail(p, "Only a single-diamond grimoire answers the die."); return InteractionResultHolder.fail(stack); }
                stack.shrink(1);
                double roll = p.getRandom().nextDouble() * 100;
                int ok = com.newuniverse.nusmp.NUConfig.GEAR_FORTUNE_SUCCESS.get(), crack = com.newuniverse.nusmp.NUConfig.GEAR_FORTUNE_CRACK.get();
                if (roll < ok) MagicGear.setCover(p, g.get(), GrimoireCover.FIVE_SIDED);
                else if (roll < ok + crack) MagicGear.setCover(p, g.get(), GrimoireCover.CRACKED_DIAMOND);
                else p.displayClientMessage(Component.literal("The die settles on nothing. Fortune is undecided.").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.success(stack);
            }
            case RECOVERY_SALVE -> {
                var g = GrimoirePages.grimoireOf(p);
                p.getPersistentData().putLong("nusmp_misfortune_until", 0);
                if (g.isPresent() && GrimoirePages.coverOf(g.get()).isCracked()) {
                    MagicGear.setCover(p, g.get(), GrimoirePages.coverOf(g.get()) == GrimoireCover.CRACKED_HEART ? GrimoireCover.HEART : GrimoireCover.DIAMOND);
                    stack.shrink(1);
                    return InteractionResultHolder.success(stack);
                }
                GrimoireBook.fail(p, "There is no crack to heal.");
                return InteractionResultHolder.fail(stack);
            }
            case RUNE_STONE -> {
                var g = GrimoirePages.grimoireOf(p);
                if (g.isEmpty()) { GrimoireBook.fail(p, "You have no grimoire page to store."); return InteractionResultHolder.fail(stack); }
                MagicGear.storeRune(p, stack, g.get());
                return InteractionResultHolder.success(stack);
            }
            case SPIRIT_CHARM -> {
                if (!p.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
                var slots = com.newuniverse.nusmp.book.SpiritSlots.get(p.getServer());
                com.newuniverse.nusmp.entity.SpiritLordEntity.Kind kind = null;
                for (String sp : new String[]{"Salamander", "Undine", "Sylph", "Gnome"})
                    if (p.getUUID().equals(slots.owner(sp))) kind = com.newuniverse.nusmp.entity.SpiritLordEntity.Kind.forSpirit(sp);
                if (kind == null && com.newuniverse.nusmp.antimagic.AntiMagic.lord(p).isPresent()) kind = com.newuniverse.nusmp.entity.SpiritLordEntity.Kind.BLACK_DEVIL;
                if (kind == null) { GrimoireBook.fail(p, "No spirit has chosen you yet. Spirit Dive with a Spirit Lord contract first."); return InteractionResultHolder.fail(stack); }
                if (kind != com.newuniverse.nusmp.entity.SpiritLordEntity.Kind.BLACK_DEVIL && !com.newuniverse.nusmp.book.SpiritBond.incarnate(p, stack))
                    return InteractionResultHolder.fail(stack);
                com.newuniverse.nusmp.entity.SpiritLordEntity.toggle(p, kind);
                p.getCooldowns().addCooldown(this, 100);
                return InteractionResultHolder.success(stack);
            }
            case DEVIL_CONTRACT -> {
                p.sendSystemMessage(Component.literal("A voice from the fifth leaf: \"What will you pay?\"").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
                for (String[] o : new String[][]{{"maxhp", "Your life (-" + com.newuniverse.nusmp.NUConfig.FORBIDDEN_MAXHP_PRICE.get() + " max HP)"},
                        {"seal", "A page (one unlocked page sealed forever)"}, {"tax", "Your mana (+" + (int) (com.newuniverse.nusmp.NUConfig.FORBIDDEN_MANA_TAX.get() * 100) + "% grimoire cost forever)"}}) {
                    p.sendSystemMessage(Component.literal(" [" + o[1] + "]").withStyle(s -> s.withColor(ChatFormatting.RED)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/devilpact accept " + o[0]))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Bind the devil and pay this price")))));
                }
                return InteractionResultHolder.success(stack);
            }
            default -> { return InteractionResultHolder.pass(stack); }
        }
    }

    /** Sneak-use on another player: Bond Thread ritual, Communication Device link. */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide || !(player instanceof ServerPlayer p) || !(target instanceof ServerPlayer other)) return InteractionResult.PASS;
        if (kind == Kind.BOND_THREAD && p.isShiftKeyDown()) { MagicGear.bondRequest(p, other); return InteractionResult.SUCCESS; }
        if (kind == Kind.COMMUNICATION) { MagicGear.link(p, stack, other); return InteractionResult.SUCCESS; }
        return InteractionResult.PASS;
    }

    /** Rune stone: place a stored page as a trap. */
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (kind != Kind.RUNE_STONE || ctx.getLevel().isClientSide || !(ctx.getPlayer() instanceof ServerPlayer p)) return InteractionResult.PASS;
        BlockPos pos = ctx.getClickedPos().above();
        return MagicGear.placeRune(p, ctx.getItemInHand(), pos) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }
}
