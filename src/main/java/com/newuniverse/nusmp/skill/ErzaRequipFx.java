package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.item.WeaponEngravings;
import com.newuniverse.nusmp.requip.Armour;
import com.newuniverse.nusmp.requip.PartialArmourMask;
import com.newuniverse.nusmp.requip.RequipItems;
import com.newuniverse.nusmp.requip.RequipParticles;
import com.newuniverse.nusmp.requip.RequipRules;
import com.newuniverse.nusmp.requip.RequipWeaponItem;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Why: everything the skill does to the world when an armour goes on, stays on and comes off, kept apart from the skill's decisions: the attribute modifiers of the armour (transient, so a relog
 * never leaves a stray bonus; the skill re-applies them every second), the render aura that the client paints as the forged-in armour ({@code client.aura.ErzaAura}, forged in and shattered out by
 * {@code client.aura.EquipAnimation}), the flash of light and the themed particles of a swap, the weapon in the hand, and the flight of the winged armours. The numbers all come from
 * {@link Armour} and {@link RequipRules}.
 */
final class ErzaRequipFx {
    private ErzaRequipFx() {}

    // ---------------------------------------------------------------- attribute modifiers
    static Holder<Attribute> attribute(String path) {
        return switch (path) {
            case "movement_speed" -> Attributes.MOVEMENT_SPEED;
            case "attack_damage" -> Attributes.ATTACK_DAMAGE;
            case "armor" -> Attributes.ARMOR;
            case "armor_toughness" -> Attributes.ARMOR_TOUGHNESS;
            case "attack_speed" -> Attributes.ATTACK_SPEED;
            case "knockback_resistance" -> Attributes.KNOCKBACK_RESISTANCE;
            default -> null;
        };
    }

    static ResourceLocation modifierId(String attr) { return ResourceLocation.fromNamespaceAndPath("nusmp", "erza_requip_" + attr); }

    private static AttributeModifier.Operation op(RequipRules.Op o) {
        return o == RequipRules.Op.ADD_VALUE ? AttributeModifier.Operation.ADD_VALUE : AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
    }

    /** Puts the armour's attribute modifiers on the wearer (replacing whatever armour's were there). */
    static void applyModifiers(LivingEntity who, Armour a, double mastery) { applyModifiers(who, a, mastery, 1.0); }

    /** The same for a partial armour: every modifier is the share {@code fraction} (0..1) of the whole armour's. */
    static void applyModifiers(LivingEntity who, Armour a, double mastery, double fraction) {
        clearModifiers(who);
        for (RequipRules.Mod m : RequipRules.modifiers(a, mastery, fraction)) {
            AttributeInstance inst = who.getAttribute(attribute(m.attribute()));
            if (inst != null) inst.addTransientModifier(new AttributeModifier(modifierId(m.attribute()), m.amount(), op(m.op())));
        }
    }

    static void clearModifiers(LivingEntity who) {
        for (String attr : RequipRules.ATTRIBUTES) {
            AttributeInstance inst = who.getAttribute(attribute(attr));
            if (inst != null) inst.removeModifier(modifierId(attr));
        }
    }

    /** True when every modifier of the armour is on the wearer (the skill re-applies them when a relog or another mod removed them). */
    static boolean hasModifiers(LivingEntity who, Armour a, double mastery) {
        for (RequipRules.Mod m : RequipRules.modifiers(a, mastery)) {
            AttributeInstance inst = who.getAttribute(attribute(m.attribute()));
            if (inst == null || !inst.hasModifier(modifierId(m.attribute()))) return false;
        }
        return true;
    }

    // ---------------------------------------------------------------- the render aura
    /** The armour as the clients see it: {@code ticks} long, style = the armour's ordinal. A refresh of the same style keeps the pieces assembled. */
    static void setAura(ServerPlayer p, Armour a, int ticks) { setAura(p, a, ticks, PartialArmourMask.FULL); }

    /** A partial armour: the style carries the mask of the pieces next to the ordinal (a whole set is the plain ordinal), so every client draws the same subset. */
    static void setAura(ServerPlayer p, Armour a, int ticks, int mask) { PlayerAuras.set(p, Aura.ERZA, ticks, PartialArmourMask.encode(a.ordinal(), mask)); }

    /** The old armour shatters out over the Djinn's shatter window and is gone. */
    static void shatter(ServerPlayer p, Armour a) { shatter(p, a, PartialArmourMask.FULL); }

    /** The same for a partial armour: the pieces that were on are the pieces that shatter. */
    static void shatter(ServerPlayer p, Armour a, int mask) {
        PlayerAuras.set(p, Aura.ERZA, RequipRules.SHATTER_WINDOW, PartialArmourMask.encode(a.ordinal(), mask));
        ServerLevel level = p.serverLevel();
        themed(level, a, p.position().add(0, 1.0, 0), 28, 0.7);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    static void clearAura(ServerPlayer p) { PlayerAuras.clear(p, Aura.ERZA); }

    // ---------------------------------------------------------------- particles (the armour's own kinds and colours live in requip.RequipParticles)
    static void themed(ServerLevel level, Armour a, Vec3 at, int count, double spread) { RequipParticles.themed(level, a, at, count, spread); }

    /** The flash of light of a swap: the white flash, the rising ring and the theme particles, a ring of light on the ground and the chimes. */
    static void flash(ServerPlayer p, Armour a) {
        ServerLevel level = p.serverLevel();
        RequipParticles.flash(level, p, a);
        SkillFx.ring(level, p.position(), 1.8, a.theme());
        SkillFx.sound(level, p.position(), SoundEvents.BEACON_ACTIVATE, 0.9F, 1.5F);
        SkillFx.sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.1F);
    }

    // ---------------------------------------------------------------- the weapon in the hand
    /** Removes every weapon the skill requipped from the wearer's inventory and hands. */
    static int removeWeapons(ServerPlayer p) {
        int n = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (RequipWeaponItem.flagged(inv.getItem(i))) { inv.setItem(i, ItemStack.EMPTY); n++; }
        }
        return n;
    }

    /** Puts the armour's weapon (if it has one) in the hand: the selected slot if it is empty, else anywhere in the inventory. Engraved with its Tensura engravings at once. */
    static void giveWeapon(ServerPlayer p, Armour a) {
        removeWeapons(p);
        Item item = RequipItems.weapon(a.weapon());
        if (item == null) return;
        ItemStack s = RequipWeaponItem.flagged(item);
        WeaponEngravings.engrave(s, p.registryAccess());
        Inventory inv = p.getInventory();
        if (inv.getItem(inv.selected).isEmpty()) inv.setItem(inv.selected, s);
        else inv.placeItemBackInInventory(s);
    }

    /** True if the wearer carries a requipped weapon that is not the one this armour makes. */
    static boolean strayWeapon(ServerPlayer p, Armour a) {
        Inventory inv = p.getInventory();
        Item mine = a == null ? null : RequipItems.weapon(a.weapon());
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (RequipWeaponItem.flagged(s) && s.getItem() != mine) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- flight
    /**
     * The winged armours: sprint in the air to thrust along your look (the armour's thrust, in blocks per tick, builds up and is capped), sneak in the air to glide (GLIDE: sink slowly) or to
     * hover (HOVER: hold your height). Fall damage is cancelled while the armour flies.
     */
    static void flightTick(ServerPlayer p, Armour a) {
        if (a.flight() == Armour.Flight.NONE) return;
        if (p.onGround() || p.isInWater() || p.isPassenger() || p.getAbilities().flying || p.isFallFlying()) return;
        Vec3 v = p.getDeltaMovement();
        double vy = v.y;
        boolean moved = false;
        if (p.isSprinting()) {
            Vec3 look = p.getLookAngle();
            double cap = a.thrust() * 0.9;
            Vec3 nv = v.add(look.scale(a.thrust() * 0.06));
            if (nv.length() > cap) nv = nv.scale(cap / nv.length());
            v = nv; vy = nv.y; moved = true;
        }
        if (p.isShiftKeyDown()) {
            vy = a.flight() == Armour.Flight.HOVER ? Math.max(vy, 0.0) * 0.5 : Math.max(vy, -0.08);
            moved = true;
        } else if (a.flight() == Armour.Flight.HOVER && vy < -0.25) {
            vy = -0.25; moved = true;
        }
        if (moved) { p.setDeltaMovement(v.x, vy, v.z); p.hurtMarked = true; }
        p.fallDistance = 0;
        if (p.tickCount % 5 == 0) RequipParticles.trail(p.serverLevel(), p, a);
    }
}
