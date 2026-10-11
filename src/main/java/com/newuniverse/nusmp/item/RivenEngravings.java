package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.entity.riven.RivenBossEntity;
import com.newuniverse.nusmp.entity.riven.RivenConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Riven Remake's engravings, same shape as {@link WeaponEngravings}: item path -> engraving, applied once, flagged "NUEngraved" plus
 * "RivenEngraved" so the two systems never double-stamp. They exist only on weapons he manifests (not in the enchanting table); the weapon
 * leaves with the construct, and a copy a player tosses loses the engraving ({@code engravingDropStripped}).
 */
public final class RivenEngravings {
    private RivenEngravings() {}

    static final String FLAG = "RivenEngraved", ID_TAG = "RivenEngraving";

    /** Custom engraving (id, display name) and the existing Tensura engravings stamped on the same construct. */
    record Entry(String id, String name, List<WeaponEngravings.Engraving> tensura) {}

    static final Map<String, Entry> ENGRAVINGS = Map.of(
            "story_rapier", new Entry("nusmp:page_edge", "Page Edge", List.of(WeaponEngravings.e("magic_weapon", 1))),
            "story_blade", new Entry("nusmp:bull_brand", "Bull Brand", List.of()),
            "hexblade", new Entry("nusmp:pact_groove", "Pact Groove", List.of(WeaponEngravings.e("energy_steal", 1))),
            "grimoire_edge", new Entry("nusmp:rewrite_groove", "Rewrite Groove", List.of()),
            "shadow_knife", new Entry("nusmp:dream_notch", "Dream Notch", List.of()),
            "sever_quill", new Entry("nusmp:story_sever", "Story Sever", List.of(WeaponEngravings.e("magic_interference", 1))),
            "last_line", new Entry("nusmp:last_line", "Last Line", List.of()));          // soul_eater is deliberately not used

    public static boolean isManifested(String path) { return ENGRAVINGS.containsKey(path); }

    /** A fresh, engraved stack of one of his manifested weapons (empty if the path is not one). */
    public static ItemStack manifest(String path, HolderLookup.Provider registries) {
        Entry en = ENGRAVINGS.get(path);
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("nusmp", path));
        if (en == null || item == net.minecraft.world.item.Items.AIR) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item);
        HolderLookup.RegistryLookup<Enchantment> lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
        for (WeaponEngravings.Engraving g : en.tensura()) {
            Optional<? extends Holder<Enchantment>> h = lookup.get(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("tensura", g.id())));
            if (h.isEmpty()) continue;
            stack.enchant(h.get(), Math.max(1, Math.min(g.level(), h.get().value().getMaxLevel())));
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, (CompoundTag t) -> {
            t.putBoolean(WeaponEngravings.FLAG, true);
            t.putBoolean(FLAG, true);
            t.putString(ID_TAG, en.id());
        });
        return stack;
    }

    static String engravingId(ItemStack s) {
        CustomData cd = s.get(DataComponents.CUSTOM_DATA);
        return cd == null ? "" : cd.copyTag().getString(ID_TAG);
    }

    /** True for a stack carrying a Tensura holy_weapon engraving (Licht-style holy engravings are not a hard counter against him). */
    public static boolean isHolyWeapon(ItemStack s) {
        for (var e : s.getEnchantments().keySet()) if (e.is(ResourceLocation.fromNamespaceAndPath("tensura", "holy_weapon"))) return true;
        return false;
    }

    /** The on-hit effect of the weapon in his hand. 'bond' = the target is his Soul Bond tether. */
    public static void onHit(RivenBossEntity boss, LivingEntity target, ItemStack weapon, boolean bond, long tick) {
        String id = engravingId(weapon);
        if (id.isEmpty()) return;
        switch (id) {
            case "nusmp:page_edge" -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1)); // 1 s cast slow stand-in
            case "nusmp:bull_brand" -> { if (target.getAbsorptionAmount() > 0) target.setAbsorptionAmount(Math.max(0, target.getAbsorptionAmount() - 4f)); }  // a barrier takes double
            case "nusmp:pact_groove" -> { if (bond) target.hurt(boss.damageSources().indirectMagic(boss, boss), 4f); }
            case "nusmp:rewrite_groove" -> { if (boss.getRandom().nextFloat() < 0.10f) target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 160, 2)); } // a page burns for 8 s
            case "nusmp:dream_notch" -> { if (boss.getTarget() == target) target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 9)); }
            case "nusmp:story_sever" -> target.hurt(boss.damageSources().indirectMagic(boss, boss), 3f);
            case "nusmp:last_line" -> { target.hurt(boss.damageSources().indirectMagic(boss, boss), 10f); boss.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY); } // one swing, then it folds back into the book
            default -> {}
        }
    }

    /** A tossed copy loses its engraving. */
    public static void onToss(ItemTossEvent e) {
        if (!RivenConfig.ENGRAVING_DROP_STRIPPED.get()) return;
        ItemStack s = e.getEntity().getItem();
        if (engravingId(s).isEmpty()) return;
        ItemStack out = s.copy();
        out.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        CustomData.update(DataComponents.CUSTOM_DATA, out, (CompoundTag t) -> { t.remove(ID_TAG); t.remove(FLAG); });
        e.getEntity().setItem(out);
    }

    public static void onTooltip(ItemTooltipEvent e) {
        String id = engravingId(e.getItemStack());
        if (id.isEmpty()) return;
        Entry en = ENGRAVINGS.get(BuiltInRegistries.ITEM.getKey(e.getItemStack().getItem()).getPath());
        e.getToolTip().add(Component.literal("Engraved \u2014 " + (en != null ? en.name() : id)).withStyle(ChatFormatting.DARK_PURPLE));
    }
}
