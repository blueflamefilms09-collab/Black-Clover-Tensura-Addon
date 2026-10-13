package com.newuniverse.nusmp.relics.order;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** What the Order needs to know about a relic stack: which relic it is, whether the relic is unique, and its translated name. */
final class OrderRelics {
    private OrderRelics() {}

    /** The relic a stack is, or null (not a relic, or empty). */
    static RelicDef defOf(ItemStack stack) { return Relics.defOf(stack); }

    /** Every relic may exist once, except the consumables (a potion, a scroll, an arrow: those are meant to be many). */
    static boolean unique(RelicDef def) { return def != null && def.isUnique(); }

    /** The relic id of a stack that is a unique relic, else null. */
    static String uniqueId(ItemStack stack) {
        RelicDef d = defOf(stack);
        return unique(d) ? d.id() : null;
    }

    /** The translated name of a relic, as the client will show it. */
    static Component name(String relicId) { return Component.translatable("item." + NUSMP.MODID + "." + relicId); }
}
