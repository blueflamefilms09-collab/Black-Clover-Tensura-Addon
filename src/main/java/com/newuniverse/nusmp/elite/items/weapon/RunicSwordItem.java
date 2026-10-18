package com.newuniverse.nusmp.elite.items.weapon;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.elite.items.EliteComponents;
import com.newuniverse.nusmp.elite.items.EliteItemsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Why: the three Runic Swords (Renewed, Ancient, Mythical) are the elite weapons with RUNE SLOTS (1, 2, 3 by tier). A rune is a Tensura engraving: drag an enchanted book that carries one, or a rune
 * item from {@code data/nusmp/elite/items/runes/*.json}, onto the sword in the inventory with the right mouse button (the way a bundle takes an item) and the engraving is put on the sword as a real
 * enchantment and remembered in {@code nusmp:runes}. {@link RuneRules} decides the slots; each active rune adds flat attack damage (an attribute modifier that is rewritten whenever the runes change,
 * so the tooltip shows the real number) and strengthens the sword's techniques (the {@code rune_scale} of its ability file). A rune whose enchantment a grindstone removed frees its slot again, checked
 * once a second in the inventory. The sword keeps its own lore engravings ({@code item/WeaponEngravings}); those do not count as runes.
 */
public class RunicSwordItem extends EliteWeaponItem {
    private static final ResourceLocation BONUS_ID = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "rune_bonus");
    private static final TagKey<Enchantment> ENGRAVING_TAG = TagKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("tensura", "engraving"));

    public RunicSwordItem(Def def) { super(def); }

    public int slots() { return RuneRules.slots(def.id); }

    @Override
    int runeCount(ItemStack stack, ServerPlayer p) { return EliteComponents.runes(stack).size(); }

    // ------------------------------------------------------------------ slotting by drag and drop
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || other.isEmpty() || !EliteItemsConfig.enabled() || !EliteItemsConfig.get(EliteItemsConfig.ABILITIES_ENABLED)) return false;
        HolderLookup.Provider reg = player.level().registryAccess();
        Found f = find(other, reg);
        if (f == null) return false;
        String rune = f.key.location().toString();
        List<String> active = active(stack, reg);
        RuneRules.Slotting r = RuneRules.check(active, slots(), rune);
        if (r != RuneRules.Slotting.OK) {
            if (!player.level().isClientSide) player.displayClientMessage(Component.translatableWithFallback(
                    r == RuneRules.Slotting.FULL ? "message.nusmp.runic.full" : "message.nusmp.runic.duplicate",
                    r == RuneRules.Slotting.FULL ? "All %s rune slots are in use." : "This rune is already slotted.", slots()).withStyle(ChatFormatting.GRAY), true);
            return true;
        }
        int max = f.holder.value().getMaxLevel();
        stack.enchant(f.holder, Math.max(1, Math.min(f.level, max)));
        EliteComponents.setRunes(stack, RuneRules.add(active, slots(), rune));
        applyAttributes(stack);
        other.shrink(1);
        if (!player.level().isClientSide) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8f, 1.4f);
            player.displayClientMessage(Component.translatableWithFallback("message.nusmp.runic.slotted", "Rune slotted: %s (%s/%s)", Component.translatable("enchantment." + f.key.location().getNamespace() + "." + f.key.location().getPath()),
                    active.size() + 1, slots()).withStyle(ChatFormatting.AQUA), true);
        }
        return true;
    }

    /** The engraving found in a stack: from the rune table, else the first engraving of an enchanted book. */
    private record Found(Holder<Enchantment> holder, ResourceKey<Enchantment> key, int level) {}

    private static Found find(ItemStack other, HolderLookup.Provider reg) {
        HolderLookup.RegistryLookup<Enchantment> look = reg.lookupOrThrow(Registries.ENCHANTMENT);
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(other.getItem());
        String mapped = RuneTable.engravingOf(itemKey.toString());
        if (mapped != null) {
            ResourceLocation rl = ResourceLocation.tryParse(mapped);
            if (rl != null) {
                ResourceKey<Enchantment> k = ResourceKey.create(Registries.ENCHANTMENT, rl);
                Optional<? extends Holder<Enchantment>> h = look.get(k);
                if (h.isPresent()) return new Found(h.get(), k, 1);
            }
        }
        ItemEnchantments stored = other.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored != null) {
            for (Holder<Enchantment> h : stored.keySet()) {
                Optional<ResourceKey<Enchantment>> k = h.unwrapKey();
                if (k.isEmpty()) continue;
                if (h.is(ENGRAVING_TAG) || NUSMP.MODID.equals(k.get().location().getNamespace())) return new Found(h, k.get(), stored.getLevel(h));
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ active runes and the damage bonus
    /** The runes of this stack whose enchantment is still on it, in slot order. */
    static List<String> active(ItemStack stack, HolderLookup.Provider reg) {
        List<String> stored = EliteComponents.runes(stack);
        if (stored.isEmpty()) return List.of();
        HolderLookup.RegistryLookup<Enchantment> look = reg.lookupOrThrow(Registries.ENCHANTMENT);
        ItemEnchantments ench = stack.getEnchantments();
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return RuneRules.reconcile(stored, RuneRules.slots(id), r -> {
            ResourceLocation rl = ResourceLocation.tryParse(r);
            if (rl == null) return false;
            Optional<? extends Holder<Enchantment>> h = look.get(ResourceKey.create(Registries.ENCHANTMENT, rl));
            return h.isPresent() && ench.getLevel(h.get()) > 0;
        });
    }

    /** Rewrites the stack's attack modifiers: the weapon's own, plus the flat damage of its active runes. */
    void applyAttributes(ItemStack stack) {
        ItemAttributeModifiers base = SwordItem.createAttributes(def.tier, def.damage, def.speed);
        double bonus = RuneRules.bonusDamage(def.id, EliteComponents.runes(stack).size());
        if (bonus > 0) base = base.withModifierAdded(Attributes.ATTACK_DAMAGE, new AttributeModifier(BONUS_ID, bonus, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, base);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity holder, int slot, boolean selected) {
        super.inventoryTick(stack, level, holder, slot, selected);
        if (level.isClientSide || level.getGameTime() % 20 != 7) return;
        List<String> stored = EliteComponents.runes(stack);
        if (stored.isEmpty()) return;
        List<String> now = new ArrayList<>(active(stack, level.registryAccess()));
        if (!now.equals(stored)) {                                                      // a grindstone or an anvil took a rune's enchantment away
            EliteComponents.setRunes(stack, now);
            applyAttributes(stack);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) { return def == Def.RUNIC_SWORD_MYTHICAL || super.isFoil(stack); }

    // ------------------------------------------------------------------ tooltip
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, tip, flag);
        List<String> runes = EliteComponents.runes(stack);
        tip.add(Component.translatableWithFallback("tooltip.nusmp.runic.slots", "Rune slots: %s of %s used", runes.size(), slots()).withStyle(ChatFormatting.GOLD));
        for (String r : runes) {
            ResourceLocation rl = ResourceLocation.tryParse(r);
            if (rl != null) tip.add(Component.literal("  ").append(Component.translatable("enchantment." + rl.getNamespace() + "." + rl.getPath())).withStyle(ChatFormatting.AQUA));
        }
        for (int i = runes.size(); i < slots(); i++) tip.add(Component.translatableWithFallback("tooltip.nusmp.runic.empty", "  (empty slot)").withStyle(ChatFormatting.DARK_GRAY));
        tip.add(Component.translatableWithFallback("tooltip.nusmp.runic.how", "Drag an engraving book or a rune onto the sword with the right mouse button.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
