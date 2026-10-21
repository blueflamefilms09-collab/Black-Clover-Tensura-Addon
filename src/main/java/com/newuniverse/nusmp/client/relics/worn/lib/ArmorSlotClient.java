package com.newuniverse.nusmp.client.relics.worn.lib;

import com.newuniverse.nusmp.client.relics.fx.RelicFxOptions;
import com.newuniverse.nusmp.client.relics.worn.ArmorSlotPlan;
import com.newuniverse.nusmp.client.relics.worn.WornExec;
import com.newuniverse.nusmp.client.relics.worn.WornPlan;
import com.newuniverse.nusmp.client.relics.worn.WornRenderers;
import com.newuniverse.nusmp.relics.RelicBearer;
import com.newuniverse.nusmp.relics.RelicDef;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * The client half of {@link ArmorSlotPlan}: whether the vanilla armour layer should draw nothing for a relic in an armour slot of this wearer right now (the item's
 * {@code getHumanoidArmorModel} hands the empty model back when it should). Decided on every frame from the same plan the worn layers use, so a relic is never both
 * drawn twice nor drawn not at all. Client render thread only.
 */
public final class ArmorSlotClient {
    private ArmorSlotClient() {}

    private static final double[] DIST = new double[1];

    /** The verdict for the stack worn in {@code slot} by {@code wearer}, or null when it is not an armour relic of that slot. */
    public static ArmorSlotPlan.Verdict verdict(LivingEntity wearer, ItemStack stack, EquipmentSlot slot) {
        if (stack == null || !(stack.getItem() instanceof RelicBearer b)) return null;
        RelicDef d = b.relic();
        if (!d.slot().isArmor()) return null;
        if ((d.slot() == com.newuniverse.nusmp.relics.RelicSlot.HEAD) != (slot == EquipmentSlot.HEAD)
                || (d.slot() == com.newuniverse.nusmp.relics.RelicSlot.FEET) != (slot == EquipmentSlot.FEET)
                || (d.slot() == com.newuniverse.nusmp.relics.RelicSlot.CHEST) != (slot == EquipmentSlot.CHEST)) return null;
        boolean player = wearer instanceof AbstractClientPlayer;
        WornPlan.View view = player ? WornExec.viewOf(wearer) : WornPlan.View.OTHER;
        boolean far = WornExec.tooFar(wearer, DIST);
        return ArmorSlotPlan.decide(d.slot().name(), WornLibrary.of(d) != null, player && WornRenderers.get(d.id()) != null, RelicFxOptions.models(),
                RelicFxOptions.hideArmourRelics(), view, far);
    }

    /** True when the painted layer of this relic must not be drawn (a model, an own renderer or the player's switch stands in for it). */
    public static boolean hidesPainted(LivingEntity wearer, ItemStack stack, EquipmentSlot slot) {
        ArmorSlotPlan.Verdict v = verdict(wearer, stack, slot);
        boolean hides = v != null && v.layer() == ArmorSlotPlan.Layer.EMPTY;
        if (v != null && !hides) {                                                    // 1.111.0: a fact for the failsafe of the worn layer: the vanilla layer is about to draw the painted layer of this piece
            try {
                com.newuniverse.nusmp.client.relics.worn.ArmorTrace.markPainted(wearer.getUUID(), ((RelicBearer) stack.getItem()).relic().slot().name());
            } catch (RuntimeException ignored) {
                // the record is only a witness
            }
        }
        return hides;
    }
}
