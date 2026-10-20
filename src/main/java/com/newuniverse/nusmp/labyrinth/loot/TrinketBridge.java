package com.newuniverse.nusmp.labyrinth.loot;

import com.newuniverse.nusmp.item.CuriosLink;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: a ring in a Curios slot has no vanilla equipment slot, so no vanilla rule applies its modifiers; the mod reads the Curios inventory by reflection ({@link CuriosLink},
 * the build cannot reach Curios' maven) the way it already carries the sheathed weapons' bonuses, and this applies the rolled lines of the worn trinkets as transient attribute
 * modifiers. Once a second (the reads are reflective) it collects the trinkets worn, adds up the lines per stat, and sets one modifier per stat with a fixed id
 * ({@code nusmp:loot_trinket/<stat>}), replacing it when the number changed and removing it when the trinket came off, so taking a ring off ends its effect within a second and
 * logging in again re-applies it (transient modifiers are not saved). Without Curios the off hand stands in for the slot. Two worn trinkets of the same stat add up, each at
 * its own value, because the sum is what is applied. The critical-chance lines are read by {@link CritHandler}, not here. No hunger or exhaustion anywhere.
 */
public final class TrinketBridge {
    private TrinketBridge() {}

    /** The trinkets worn in Curios slots (or the off hand without Curios) that carry a roll. */
    public static List<ItemStack> worn(Player p) {
        List<ItemStack> out = new ArrayList<>(4);
        if (!LootConfig.get(LootConfig.TRINKETS)) return out;
        if (CuriosLink.available()) {
            for (ItemStack s : CuriosLink.equipped(p)) if (s.getItem() instanceof LootTrinketItem && GearStacks.rollOf(s) != null) out.add(s);
        } else {
            ItemStack off = p.getItemBySlot(EquipmentSlot.OFFHAND);
            if (off.getItem() instanceof LootTrinketItem && GearStacks.rollOf(off) != null) out.add(off);
        }
        return out;
    }

    public static void onTick(PlayerTickEvent.Post e) {
        Player p = e.getEntity();
        if (p.level().isClientSide || (p.tickCount + p.getId()) % 20 != 0) return;
        try {
            apply(p);
        } catch (RuntimeException ignored) {
            // a bad read leaves the last state; the next second tries again
        }
    }

    /** Sets, replaces or removes the one modifier per stat so that the player has exactly the worn trinkets' sum. */
    public static void apply(Player p) { apply(p, worn(p)); }

    /** The same for an explicit list of worn trinkets (the self-test passes its own, so it does not depend on Curios being installed). */
    public static void apply(Player p, List<ItemStack> worn) {
        double[] sum = new double[StatKind.values().length];
        for (ItemStack s : worn) {
            GearRoll r = GearStacks.rollOf(s);
            for (GearRoll.Line l : r.lines()) sum[l.kind().ordinal()] += l.value();
        }
        for (StatKind k : StatKind.values()) {
            Holder<Attribute> a = GearStacks.attribute(k);
            if (a == null || !k.allowedOn(GearKind.TRINKET)) continue;
            AttributeInstance inst = p.getAttribute(a);
            if (inst == null) continue;
            ResourceLocation id = id(k);
            AttributeModifier have = inst.getModifier(id);
            double want = sum[k.ordinal()];
            if (want <= 0.0) {
                if (have != null) inst.removeModifier(id);
            } else if (have == null || Math.abs(have.amount() - want) > 1e-9) {
                if (have != null) inst.removeModifier(id);
                inst.addTransientModifier(new AttributeModifier(id, want, GearStacks.operation(k)));
            }
        }
    }

    static ResourceLocation id(StatKind k) { return ResourceLocation.fromNamespaceAndPath("nusmp", "loot_trinket/" + k.id()); }

    /** Removes every trinket modifier from the player (a self-test cleans up with it). */
    public static void clear(Player p) {
        for (StatKind k : StatKind.values()) {
            Holder<Attribute> a = GearStacks.attribute(k);
            AttributeInstance inst = a == null ? null : p.getAttribute(a);
            if (inst != null) inst.removeModifier(id(k));
        }
    }
}
