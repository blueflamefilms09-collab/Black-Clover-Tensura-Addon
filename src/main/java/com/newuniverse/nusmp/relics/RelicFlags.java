package com.newuniverse.nusmp.relics;

import net.minecraft.world.entity.LivingEntity;

/** The named switches of {@link Passive#flag}: a wave's own handlers ask whether the entity carries one. */
public final class RelicFlags {
    private RelicFlags() {}

    /** True while any equipped relic that works for this entity (attuned, if it needs that) carries the flag. Cheap: answered from a short-lived snapshot. */
    public static boolean has(LivingEntity e, String key) {
        return e != null && key != null && Relics.loadout(e).flags().contains(key);
    }
}
