package com.newuniverse.nusmp.relics;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * What an {@link ActBody} is told: who fired it, with which stack, whether they were sneaking, and for an ability that is held, how many ticks
 * it was charged.
 */
public record ActCtx(ServerPlayer player, ItemStack stack, RelicDef def, boolean sneaking, int chargeTicks) {
    /** For a toggle ability: is it now switched on (the engine flips it before the body runs)? */
    public boolean isOn(String actKey) { return RelicEngine.isOn(player, def.id(), actKey); }
}
