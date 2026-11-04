package com.newuniverse.nusmp.labyrinth.mega;

import com.newuniverse.nusmp.labyrinth.Labyrinth;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Why (3.48.2): a server hung for 60 seconds inside Tensura's {@code SetRandomDigTarget.getTargetPosition} (the Armorsaurus's "pick a random place to dig" AI) while an Armorsaurus stood
 * in the Mega-Dungeon, whose floors are unbreakable and leave that search no dig spot. The AI is Tensura's, so this mod does not patch it; it keeps the diggers out of the dimension instead:
 * an Armorsaurus (or any id in {@link #DIGGERS}) that would join a level of the Mega-Dungeon is not added, whatever spawned it (a roster tag, a wanderer, a spawn egg). Server side only, one
 * set lookup per entity join in that one dimension and nothing anywhere else; no state. The switch is {@code mega.keepDiggersOut} (default true); off, the old behaviour.
 */
public final class DiggerGuard {
    private DiggerGuard() {}

    /** Entity ids whose dig-target AI can hang the server when the ground cannot be dug. */
    static final java.util.Set<ResourceLocation> DIGGERS = java.util.Set.of(ResourceLocation.fromNamespaceAndPath("tensura", "armorsaurus"));

    /** Pure decision: is this entity type id kept out of a Mega-Dungeon level? */
    public static boolean refuses(boolean switchOn, boolean inMega, ResourceLocation typeId) {
        return switchOn && inMega && typeId != null && DIGGERS.contains(typeId);
    }

    public static void onJoin(EntityJoinLevelEvent e) {
        Entity en = e.getEntity();
        if (e.getLevel().isClientSide() || !Labyrinth.inMega(e.getLevel())) return;
        if (refuses(MegaConfig.get(MegaConfig.KEEP_DIGGERS_OUT), true, BuiltInRegistries.ENTITY_TYPE.getKey(en.getType()))) e.setCanceled(true);
    }
}
