package com.newuniverse.nusmp.heroweapon.forms;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.heroweapon.HeroComponents;
import com.newuniverse.nusmp.heroweapon.HeroItems;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: a form is only real when the wielder feels it, and that is a set of attribute modifiers and one data component that must always agree with the bond: the
 * permanent bonuses of finished forms while the bond exists, the active form's numbers while the weapon is summoned, and the look the renderer draws. Doing it
 * wrongly is easy and invisible (a bonus that stays after the bond ends, a stat that doubles on every call, an item whose damage is never reset), so it is one
 * routine, {@link #refresh}, that computes what SHOULD be there from the bond and makes the player and the stack match it, changing nothing that already matches:
 * calling it twice, or every tick, is harmless. The plan of which stat goes where is {@link FormStats}; this class only maps a key to an attribute.
 *
 * <p>Ids: {@code nusmp:hero_bonus_<key>} for the permanent bonuses (added as PERMANENT modifiers so the extra health survives a restart), {@code nusmp:hero_form_<key>}
 * for the active form (TRANSIENT, so they never outlive the weapon in the hand). The active form's attack damage and attack speed go on the weapon stack itself
 * (main hand only) beside whatever the core's item already carries; everything else is a player attribute. Movement speed is a share of the base.
 */
final class FormLoadout {
    private FormLoadout() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static boolean warned;

    /** The attribute a stat key changes, or null for an unknown key. */
    static Holder<Attribute> attribute(String key) {
        return switch (key) {
            case "attack_damage" -> Attributes.ATTACK_DAMAGE;
            case "attack_speed" -> Attributes.ATTACK_SPEED;
            case "armor" -> Attributes.ARMOR;
            case "armor_toughness" -> Attributes.ARMOR_TOUGHNESS;
            case "max_health" -> Attributes.MAX_HEALTH;
            case "movement_speed" -> Attributes.MOVEMENT_SPEED;
            case "knockback_resistance" -> Attributes.KNOCKBACK_RESISTANCE;
            case "reach" -> Attributes.ENTITY_INTERACTION_RANGE;
            case "luck" -> Attributes.LUCK;
            default -> null;
        };
    }

    static ResourceLocation id(String prefix, String key) { return ResourceLocation.fromNamespaceAndPath("nusmp", FormStats.path(prefix, key)); }

    /** Makes the player and the weapon stacks match the bond. Never throws. */
    static void refresh(ServerPlayer player) {
        try {
            refreshUnsafe(player);
        } catch (RuntimeException e) {
            if (!warned) {
                warned = true;
                LOG.warn("[nusmp] hero forms: refresh failed once (shown no more)", e);
            }
        }
    }

    private static void refreshUnsafe(ServerPlayer player) {
        HeroBond bond = HeroBonds.of(player);
        if (!bond.isBound()) {
            clear(player);
            return;
        }
        HeroKind kind = bond.kind();
        List<HeroForm> forms = HeroForms.of(kind);
        HeroForm active = HeroForms.active(bond);
        if (!active.id().equals(bond.form())) bond.setForm(active.id());      // a data reload removed the form the bond was in: it follows the base form instead of naming a form that is gone
        boolean on = bond.summoned();

        Map<String, Double> bonus = new HashMap<>();
        for (FormStats.Mod m : FormStats.bonusMods(forms, bond.bonusesGranted())) bonus.put(m.key(), m.amount());
        Map<String, Double> form = new HashMap<>();
        if (on) for (FormStats.Mod m : FormStats.formMods(active)) if (m.target() == FormStats.Target.PLAYER) form.put(m.key(), m.amount());
        for (String key : HeroForm.STAT_KEYS) {
            apply(player, key, FormStats.BONUS_PREFIX, bonus.getOrDefault(key, 0.0), true);
            apply(player, key, FormStats.FORM_PREFIX, FormStats.onStack(key) ? 0.0 : form.getOrDefault(key, 0.0), false);
        }
        Inventory inv = player.getInventory();
        for (ItemStack s : inv.items) if (HeroItems.kindOf(s) == kind) shape(s, active, on);
        for (ItemStack s : inv.offhand) if (HeroItems.kindOf(s) == kind) shape(s, active, on);
    }

    /** Removes every modifier this system ever adds (the bond ended, or there never was one). */
    static void clear(ServerPlayer player) {
        for (String key : HeroForm.STAT_KEYS) {
            apply(player, key, FormStats.BONUS_PREFIX, 0.0, true);
            apply(player, key, FormStats.FORM_PREFIX, 0.0, false);
        }
    }

    /** Sets one modifier to {@code amount} (removing it for zero), touching nothing when it already matches. */
    private static void apply(ServerPlayer player, String key, String prefix, double amount, boolean permanent) {
        Holder<Attribute> holder = attribute(key);
        AttributeInstance inst = holder == null ? null : player.getAttribute(holder);
        if (inst == null) return;
        ResourceLocation id = id(prefix, key);
        AttributeModifier current = inst.getModifier(id);
        if (Math.abs(amount) < 1e-9) {
            if (current != null) inst.removeModifier(id);
            return;
        }
        AttributeModifier.Operation op = FormStats.multiplied(key) ? AttributeModifier.Operation.ADD_MULTIPLIED_BASE : AttributeModifier.Operation.ADD_VALUE;
        if (current != null && current.operation() == op && Math.abs(current.amount() - amount) < 1e-9) return;
        AttributeModifier wanted = new AttributeModifier(id, amount, op);
        if (permanent) inst.addOrReplacePermanentModifier(wanted);
        else inst.addOrUpdateTransientModifier(wanted);
    }

    /** True for an item modifier this system added (so it is replaced, never doubled, and the core's own are kept). */
    private static boolean mine(AttributeModifier m) {
        ResourceLocation id = m.id();
        return "nusmp".equals(id.getNamespace()) && id.getPath().startsWith(FormStats.FORM_PREFIX);
    }

    /** The weapon stack: the active form's attack damage and speed (main hand only) and the look. */
    private static void shape(ItemStack stack, HeroForm active, boolean on) {
        ItemAttributeModifiers current = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        List<ItemAttributeModifiers.Entry> keep = new ArrayList<>();
        for (ItemAttributeModifiers.Entry e : current.modifiers()) if (!mine(e.modifier())) keep.add(e);
        if (on) {
            for (FormStats.Mod m : FormStats.formMods(active)) {
                Holder<Attribute> holder = m.target() == FormStats.Target.STACK ? attribute(m.key()) : null;
                if (holder == null) continue;
                keep.add(new ItemAttributeModifiers.Entry(holder, new AttributeModifier(id(FormStats.FORM_PREFIX, m.key()), m.amount(), AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND));
            }
        }
        ItemAttributeModifiers wanted = new ItemAttributeModifiers(keep, current.showInTooltip());
        if (!wanted.equals(current)) stack.set(DataComponents.ATTRIBUTE_MODIFIERS, wanted);
        String look = active.look();
        if (!look.equals(stack.get(HeroComponents.LOOK.get()))) stack.set(HeroComponents.LOOK.get(), look);
    }
}
