package com.newuniverse.nusmp.blackclover;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.grimoire.GrimoireAppearance;
import com.newuniverse.nusmp.grimoire.GrimoireComponents;

import java.util.List;
import java.util.UUID;

/** A grimoire bound to one mage. Only its owner can cast from it. */
public class GrimoireItem extends Item {
    public GrimoireItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    /** Old signature (leaf count -> Clover cover). */
    public static ItemStack create(Player owner, int leaves, MagicType magic, Devil devil) {
        return create(owner, Kingdom.CLOVER.coverFor(leaves), magic, devil);
    }

    public static ItemStack create(Player owner, GrimoireCover cover, MagicType magic, Devil devil) {
        ItemStack stack = createUnbound(cover, magic, devil);
        if (uniqueLooks()) stack.set(GrimoireComponents.APPEARANCE.get(), GrimoireAppearance.forOwner(cover, magic, owner.getUUID()));
        CompoundTag tag = data(stack);
        tag.putUUID("Owner", owner.getUUID());
        tag.putString("OwnerName", owner.getName().getString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    /** A grimoire with no owner (creative tab). Binding it requires creative mode or operator. */
    public static ItemStack createUnbound(int leaves, MagicType magic, Devil devil) {
        return createUnbound(Kingdom.CLOVER.coverFor(leaves), magic, devil);
    }

    public static ItemStack createUnbound(GrimoireCover cover, MagicType magic, Devil devil) {
        ItemStack stack = new ItemStack(BlackCloverRegistry.GRIMOIRE.get());
        CompoundTag tag = new CompoundTag();
        tag.putInt("Leaves", cover.tier);
        tag.putString("Cover", cover.name());
        tag.putString("Magic", magic.name());
        if (devil != null) tag.putString("Devil", devil.name());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(GrimoireComponents.APPEARANCE.get(), GrimoireAppearance.fromCover(cover, magic));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(cover.displayName())
                .withStyle(cover.isForbidden() ? ChatFormatting.DARK_RED : cover.isRare() ? ChatFormatting.GOLD
                        : cover.isCracked() ? ChatFormatting.GRAY : ChatFormatting.GREEN));
        if (cover.isRare() || cover.isForbidden()) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    /**
     * A grimoire drawn with an explicit look (creative tab showcase, {@code /nusmp grimoire look}). Spell power, cost and rarity follow
     * the crest's nearest real cover; only the appearance is custom. Real grimoires are rebuilt from (cover, magic, owner), so a custom
     * look is cosmetic and lasts only as long as this stack does.
     */
    public static ItemStack createWithLook(GrimoireAppearance look, MagicType magic, Devil devil) {
        GrimoireCover cover = look.insignia().toCover();
        ItemStack stack = createUnbound(cover, magic, devil);
        stack.set(GrimoireComponents.APPEARANCE.get(), look);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(look.insignia().displayName + " Grimoire")
                .withStyle(cover.isForbidden() ? ChatFormatting.DARK_RED : cover.isRare() ? ChatFormatting.GOLD
                        : cover.isCracked() ? ChatFormatting.GRAY : ChatFormatting.GREEN));
        return stack;
    }

    /** Server config switch: each owner's grimoire gets its own cosmetic variation (default on). */
    private static boolean uniqueLooks() {
        try { return NUConfig.GRIMOIRE_UNIQUE_LOOKS.get(); } catch (IllegalStateException notLoaded) { return true; }
    }

    /** Grimoires from before 0.20 have no appearance component; give them one the first time the server sees them in an inventory. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || stack.has(GrimoireComponents.APPEARANCE.get())) return;
        CompoundTag tag = data(stack);
        if (!tag.contains("Magic")) return;
        GrimoireCover cover = cover(stack);
        MagicType magic = MagicType.byName(tag.getString("Magic"));
        stack.set(GrimoireComponents.APPEARANCE.get(), tag.hasUUID("Owner") && uniqueLooks()
                ? GrimoireAppearance.forOwner(cover, magic, tag.getUUID("Owner")) : GrimoireAppearance.fromCover(cover, magic));
    }

    public static GrimoireCover cover(ItemStack stack) {
        CompoundTag t = data(stack);
        return GrimoireCover.byName(t.getString("Cover"), t.getInt("Leaves"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag tag = data(stack);
        if (level.isClientSide || tag.hasUUID("Owner") || !tag.contains("Magic")) return InteractionResultHolder.pass(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
        if (!sp.isCreative() && !sp.hasPermissions(2)) {
            sp.displayClientMessage(Component.literal("This grimoire has not chosen you.").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        stack.shrink(1);
        GrimoireAcceptance.grantExact(sp, cover(stack), MagicType.byName(tag.getString("Magic")),
                Devil.byName(tag.getString("Devil")));
        return InteractionResultHolder.success(stack);
    }

    public static String leafName(int leaves) {
        return switch (leaves) { case 5 -> "Five-Leaf"; case 4 -> "Four-Leaf"; default -> "Three-Leaf"; };
    }

    public static CompoundTag data(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        return d == null ? new CompoundTag() : d.copyTag();
    }

    public static boolean isOwnedBy(ItemStack stack, UUID id) {
        if (!(stack.getItem() instanceof GrimoireItem)) return false;
        CompoundTag tag = data(stack);
        return tag.hasUUID("Owner") && tag.getUUID("Owner").equals(id);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = data(stack);
        if (!tag.contains("Magic")) return;
        if (!tag.hasUUID("Owner")) tooltip.add(Component.literal("Unbound - right-click to bind (creative/admin)").withStyle(ChatFormatting.DARK_GRAY));
        GrimoireCover cover = cover(stack);
        tooltip.add(Component.literal(MagicType.byName(tag.getString("Magic")).displayName).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal(cover.kingdom.displayName).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(cover.lore).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(String.format("Spell power x%.2f, cost x%.2f", cover.damage, cover.cost)).withStyle(ChatFormatting.DARK_GRAY));
        GrimoireAppearance look = GrimoireAppearance.of(stack);
        tooltip.add(Component.literal(look.cover().displayName + " cover, " + look.trimMetal().displayName + " trim, "
                + look.thickness().displayName + ", " + look.clasp().displayName).withStyle(ChatFormatting.DARK_GRAY));
        if (tag.hasUUID("Owner")) tooltip.add(Component.literal("Owner: " + tag.getString("OwnerName")).withStyle(ChatFormatting.GRAY));
        Devil devil = Devil.byName(tag.getString("Devil"));
        if (devil != null) {
            tooltip.add(Component.literal("Devil: " + devil.displayName).withStyle(ChatFormatting.DARK_RED));
        }
    }
}
