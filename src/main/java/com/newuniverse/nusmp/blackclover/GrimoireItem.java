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
import com.newuniverse.nusmp.grimoire.BookLook;
import com.newuniverse.nusmp.grimoire.CanonBook;
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
        CompoundTag tag = data(stack);
        UUID id = owner.getUUID();
        tag.putInt("LookSeed", uniqueLooks() ? (int) (id.getMostSignificantBits() ^ id.getLeastSignificantBits()) | 1 : 0);   // each mage's book is a shade of its own
        tag.putUUID("Owner", owner.getUUID());
        tag.putString("OwnerName", owner.getName().getString());
        tag.putUUID("GrimoireId", UUID.randomUUID());     // every granted grimoire is its own book, made before anything is consumed
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
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(cover.displayName())
                .withStyle(cover.isForbidden() ? ChatFormatting.DARK_RED : cover.isRare() ? ChatFormatting.GOLD
                        : cover.isCracked() ? ChatFormatting.GRAY : ChatFormatting.GREEN));
        if (cover.isRare() || cover.isForbidden()) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    /**
     * A named canon grimoire from the Black Clover grimoire tables (creative tab, /multiverse grimoire canon): its cover, magic and
     * look follow the original owner's book. Binding it makes it yours and keeps the look.
     */
    public static ItemStack createCanon(CanonBook book) {
        GrimoireCover cover = GrimoireCover.byName(book.cover, 3);
        MagicType magic = MagicType.byName(book.magic);
        ItemStack stack = createUnbound(cover, magic, cover.isForbidden() ? (magic == MagicType.ANTI_MAGIC ? Devil.LIEBE : Devil.MEGICULA) : null);
        setCanon(stack, book.id());
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(book.owner + "'s Grimoire")
                .withStyle(cover.isForbidden() ? ChatFormatting.DARK_RED : cover.isRare() ? ChatFormatting.GOLD : ChatFormatting.GREEN));
        return stack;
    }

    /** Copy-on-write: marks the stack as a canon book's look. */
    public static void setCanon(ItemStack stack, String canonId) {
        CompoundTag tag = data(stack);
        if (canonId == null || canonId.isEmpty()) tag.remove("Canon"); else tag.putString("Canon", canonId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static final ThreadLocal<Object[]> LAST_LOOK = ThreadLocal.withInitial(() -> new Object[2]);

    /** How this grimoire looks (cover, magic, canon book, owner shade). Remembered per thread for the same data object. */
    public static BookLook look(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        if (d == null) return BookLook.DEFAULT;
        Object[] memo = LAST_LOOK.get();
        if (memo[0] == d) return (BookLook) memo[1];
        CompoundTag t = d.copyTag();
        int seed = t.contains("LookSeed") ? t.getInt("LookSeed")
                : t.hasUUID("Owner") ? (int) (t.getUUID("Owner").getMostSignificantBits() ^ t.getUUID("Owner").getLeastSignificantBits()) | 1 : 0;
        BookLook look = BookLook.resolve(cover(stack).name(), t.getString("Magic"), t.getString("Canon"), seed);
        memo[0] = d;
        memo[1] = look;
        return look;
    }

    /** Server config switch: each owner's grimoire gets its own cosmetic variation (default on). */
    private static boolean uniqueLooks() {
        try { return NUConfig.GRIMOIRE_UNIQUE_LOOKS.get(); } catch (IllegalStateException notLoaded) { return true; }
    }

    /** Grimoires from before 0.21 still carry the removed look component; drop it the first time the server sees the stack. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && stack.has(GrimoireComponents.LEGACY_APPEARANCE.get())) stack.remove(GrimoireComponents.LEGACY_APPEARANCE.get());
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
        // A grimoire chooses one mage. Someone who already has one keeps it (pages, mastery and all) and keeps this
        // unbound copy too: binding would otherwise wipe their own grimoire. Admins reset first: /nusmp grimoire reset <player>.
        var existing = GrimoirePages.grimoireOf(sp);
        if (existing.isPresent()) {
            MagicType own = GrimoirePages.magicOf(existing.get());
            sp.displayClientMessage(Component.literal("You already have a grimoire (" + own.displayName + "). Use /nusmp grimoire reset first to swap it.")
                    .withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        GrimoireCover chosenCover = cover(stack);
        MagicType chosenMagic = MagicType.byName(tag.getString("Magic"));
        Devil chosenDevil = Devil.byName(tag.getString("Devil"));
        // transactional: the new grimoire is made and handed over first; the unbound copy is used up only once that worked
        String canon = tag.getString("Canon");
        GrimoireAcceptance.grantExact(sp, chosenCover, chosenMagic, chosenDevil);
        if (GrimoirePages.grimoireOf(sp).isPresent()) {
            if (!canon.isEmpty()) applyCanon(sp, chosenMagic, canon);
            stack.shrink(1);
            com.newuniverse.nusmp.multiverse.MultiverseProfile.setAccepted(sp, true, "a bound creative grimoire");
        }
        return InteractionResultHolder.success(stack);
    }

    /** Gives the player's own grimoire of this magic a canon book's look (after binding a canon copy). */
    public static void applyCanon(ServerPlayer p, MagicType magic, String canon) {
        var inv = p.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack s = inv.getItem(slot);
            if (isOwnedBy(s, p.getUUID()) && magic.name().equals(data(s).getString("Magic"))) {
                ItemStack copy = s.copy();
                setCanon(copy, canon);
                CanonBook b = CanonBook.byId(canon);
                if (b != null) copy.set(DataComponents.CUSTOM_NAME, Component.literal(b.owner + "'s Grimoire").withStyle(ChatFormatting.GOLD));
                inv.setItem(slot, copy);
                return;
            }
        }
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
        BookLook look = look(stack);
        if (look.canon() != null) tooltip.add(Component.literal("The grimoire of " + look.canon().owner).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Cover ornament: " + look.motif().displayName()).withStyle(ChatFormatting.DARK_GRAY));
        if (tag.hasUUID("Owner")) tooltip.add(Component.literal("Owner: " + tag.getString("OwnerName")).withStyle(ChatFormatting.GRAY));
        Devil devil = Devil.byName(tag.getString("Devil"));
        if (devil != null) {
            tooltip.add(Component.literal("Devil: " + devil.displayName).withStyle(ChatFormatting.DARK_RED));
        }
    }
}
