package com.newuniverse.nusmp.dungeon.nexus.nav;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Ki-Resonance Lantern (docs/living_dungeon.md section 3): a craftable off-hand lantern burning raw Magic Stones, shards, crystals and
 * slime (the tag {@code nusmp:lantern_fuel}; what each is worth is {@link LanternFuel}). Fuel goes in by dragging it onto the lantern in
 * an inventory (a click feeds all the stack that fits, a right click one), or by right-clicking with fuel in the other hand (sneak: all of
 * it). The fuel is in the stack's {@code fuel} component and burns while the lantern is in the off-hand ({@link NavTicker}), which also
 * keeps the {@code flame} component (a {@link FlameMode} ordinal) that the client item property {@code nusmp:flame} reads.
 */
public final class LanternItem extends Item {
    /** What counts as lantern fuel (every entry of the tag is optional). */
    public static final TagKey<Item> FUEL = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "lantern_fuel"));

    LanternItem(Item.Properties props) { super(props); }

    // ---------------------------------------------------------------- the stack's data
    /** The ticks of burning left in this lantern. */
    public static int fuelOf(ItemStack stack) {
        Integer v = stack.get(NavComponents.FUEL.get());
        return v == null ? 0 : Math.max(0, v);
    }

    /** The flame this lantern shows now (out when the component is absent). */
    public static FlameMode flameOf(ItemStack stack) {
        Integer v = stack.get(NavComponents.FLAME.get());
        return v == null ? FlameMode.OFF : FlameMode.of(v);
    }

    /**
     * The value of the item property {@code nusmp:flame} for this stack: 0 out, 0.34 amber, 0.67 blue, 1.0 red. The one number that both the
     * client property (client/nexus/NavClient) and the in-hand mesh (the renderer's mesh variant: below 0.17 _off, below 0.5 the plain mesh,
     * below 0.84 _blue, else _red) follow.
     */
    public static float flameValue(ItemStack stack) { return flameOf(stack).property; }

    /** Sets the fuel (zero removes the component), only if it changed. Returns whether it did. */
    static boolean setFuel(ItemStack stack, int fuel) {
        int was = fuelOf(stack);
        int now = Math.max(0, Math.min(LanternFuel.CAP, fuel));
        if (was == now) return false;
        if (now == 0) stack.remove(NavComponents.FUEL.get());
        else stack.set(NavComponents.FUEL.get(), now);
        return true;
    }

    /** Sets the flame (out removes the component), only if it changed. Returns whether it did. */
    static boolean setFlame(ItemStack stack, FlameMode mode) {
        if (flameOf(stack) == mode) return false;
        if (mode == FlameMode.OFF) stack.remove(NavComponents.FLAME.get());
        else stack.set(NavComponents.FLAME.get(), mode.ordinal());
        return true;
    }

    /** Feeds the lantern from a stack of fuel, taking up to {@code wanted} items. Returns how many were taken. */
    static int feed(ItemStack lantern, ItemStack from, int wanted) {
        if (from.isEmpty() || !from.is(FUEL)) return 0;
        int per = LanternFuel.ticksFor(BuiltInRegistries.ITEM.getKey(from.getItem()).toString());
        int fuel = fuelOf(lantern);
        int n = LanternFuel.howMany(fuel, per, from.getCount(), wanted);
        if (n <= 0) return 0;
        setFuel(lantern, fuel + n * per);
        from.shrink(n);
        return n;
    }

    // ---------------------------------------------------------------- feeding it
    /** Dragging fuel onto the lantern in an inventory: a click feeds all of it that fits, a right click one. */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (other.isEmpty() || !other.is(FUEL) || !slot.allowModification(player)) return false;
        int n = feed(stack, other, action == ClickAction.SECONDARY ? 1 : other.getCount());
        if (n > 0) player.playSound(SoundEvents.BOTTLE_FILL, 0.6f, 1.2f);
        if (!player.level().isClientSide) say(player, n > 0 ? "The lantern takes the fuel: " + LanternFuel.clock(fuelOf(stack)) + " of flame." : "The lantern is full.");
        return true;
    }

    /** Right-click with fuel in the other hand feeds one (sneaking: all); with nothing to feed it says how much is left. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!other.isEmpty() && other.is(FUEL)) {
            int n = feed(stack, other, player.isShiftKeyDown() ? other.getCount() : 1);
            if (n > 0) player.playSound(SoundEvents.BOTTLE_FILL, 0.6f, 1.2f);
            if (!level.isClientSide) say(player, n > 0 ? "The lantern takes the fuel: " + LanternFuel.clock(fuelOf(stack)) + " of flame." : "The lantern is full.");
            return n > 0 ? InteractionResultHolder.sidedSuccess(stack, level.isClientSide()) : InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            int fuel = fuelOf(stack);
            say(player, fuel > 0 ? "Ki-Resonance Lantern: " + LanternFuel.clock(fuel) + " of flame left. Hold it in your off-hand." : "The lantern is out of fuel: feed it Magic Stones or slime.");
        }
        return InteractionResultHolder.pass(stack);
    }

    private static void say(Player player, String text) {
        player.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.GOLD), true);
    }

    // ---------------------------------------------------------------- look
    /** The fuel as a bar under the icon, coloured like the flame. */
    @Override
    public boolean isBarVisible(ItemStack stack) { return fuelOf(stack) > 0; }

    @Override
    public int getBarWidth(ItemStack stack) { return Math.max(1, Math.round(13f * LanternFuel.fill(fuelOf(stack)))); }

    @Override
    public int getBarColor(ItemStack stack) {
        return switch (flameOf(stack)) {
            case BLUE -> 0x4FA8FF;
            case RED -> 0xE5483B;
            default -> 0xE8A33C;
        };
    }

    /** The fuel burns down and the flame changes colour in the background: that must not replay the hand's equip animation. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        int fuel = fuelOf(stack);
        tip.add(Component.literal(fuel > 0 ? "Fuel: " + LanternFuel.clock(fuel) : "Out of fuel").withStyle(fuel > 0 ? ChatFormatting.GOLD : ChatFormatting.DARK_RED));
        tip.add(Component.literal("Held in the off-hand it burns amber; blue facing an illusory wall, red facing the way down.").withStyle(ChatFormatting.AQUA));
        tip.add(Component.literal("Feed it Magic Stones, shards, crystals or slime: drag them onto it, or right-click with them in the other hand.").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("\"The wall that is not there keeps its warmth.\"").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
