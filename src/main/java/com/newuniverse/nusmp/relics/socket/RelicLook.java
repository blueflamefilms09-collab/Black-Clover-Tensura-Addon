package com.newuniverse.nusmp.relics.socket;

import com.newuniverse.nusmp.relics.Grade;
import net.minecraft.world.item.ItemStack;

/**
 * The tiny API for the worn-relic layer (and any other renderer): how far a relic has evolved. {@code RelicLook.evolved(stack)} answers the effective
 * tier, the element key of the first bound socket ("" if none) and one colour (the socket's, else the grade's). Pure reading, no allocation beyond the
 * answer, safe on the client (the component is synced) and the server.
 */
public final class RelicLook {
    private RelicLook() {}

    public static RelicLookRules.Evolved evolved(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return RelicLookRules.PLAIN;
        int tier = SocketGrades.tier(stack);
        Grade[] all = Grade.values();
        return RelicLookRules.evolved(tier, SocketComponents.firstId(stack), all[Math.max(0, Math.min(all.length - 1, tier - 1))].rgb());
    }
}
