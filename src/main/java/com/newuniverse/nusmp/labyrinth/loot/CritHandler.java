package com.newuniverse.nusmp.labyrinth.loot;

import com.newuniverse.nusmp.item.CuriosLink;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;

/**
 * Why: Minecraft has no critical-chance attribute, so the "crit" line of a loot roll cannot be a vanilla modifier. It is read from the pieces the player wears instead: when
 * the player lands a blow the game asks {@code CriticalHitEvent}, and this adds the lines of the weapon in the main hand, the four armour pieces and the worn trinkets (the Curios
 * slots, or the off hand without Curios), capped at sixty percent. A blow that is not already a critical (the vanilla jump-and-fall crit) becomes one with that chance, with the
 * vanilla multiplier of one and a half. The same event therefore serves both, and a vanilla critical is never turned off. Server side by nature (the event is raised where
 * the damage is dealt).
 */
public final class CritHandler {
    private CritHandler() {}

    static final double CAP = 0.60;
    static final float MULTIPLIER = 1.5F;

    public static void onCrit(CriticalHitEvent e) {
        if (!LootConfig.get(LootConfig.CRITS) || e.isCriticalHit()) return;
        Player p = e.getEntity();
        if (p.level().isClientSide) return;
        double chance = chance(p);
        if (chance > 0.0 && p.getRandom().nextDouble() < chance) {
            e.setCriticalHit(true);
            e.setDamageMultiplier(MULTIPLIER);
        }
    }

    /** The critical chance of what the player wears and holds, 0 to {@link #CAP}. */
    public static double chance(Player p) {
        double sum = crit(p.getMainHandItem());
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) sum += crit(p.getItemBySlot(s));
        for (ItemStack worn : TrinketBridge.worn(p)) sum += crit(worn);
        return Math.min(CAP, sum);
    }

    private static double crit(ItemStack s) {
        GearRoll r = GearStacks.rollOf(s);
        return r == null ? 0.0 : r.sum(StatKind.CRIT_CHANCE);
    }

    static boolean curios() { return CuriosLink.available(); }
}
