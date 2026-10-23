package com.newuniverse.nusmp.client.aura;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Spirit Dive, ANIME look: the one question the painters ask of the wearer's equipment: is something worn on the head? (Then the glowing eyes and the hair cap are not drawn: they would sit
 * inside the helmet.) Kept in its own class so the painters stay free of the item classes (the render preview compiles them against a stub of this one).
 */
public final class DiveHead {
    private DiveHead() {}

    public static boolean helmet(AbstractClientPlayer p) { return !p.getItemBySlot(EquipmentSlot.HEAD).isEmpty(); }
}
