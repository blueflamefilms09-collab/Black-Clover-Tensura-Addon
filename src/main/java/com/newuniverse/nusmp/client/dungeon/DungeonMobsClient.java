package com.newuniverse.nusmp.client.dungeon;

import com.newuniverse.nusmp.dungeon.DungeonEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The renderers of the Living Dungeon's creatures. */
public final class DungeonMobsClient {
    private DungeonMobsClient() {}

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(DungeonEntities.SLIME.get(), DungeonSlimeRenderer::new);
        e.registerEntityRenderer(DungeonEntities.LIVING_ARMOR.get(), LivingArmorRenderer::new);
        e.registerEntityRenderer(DungeonEntities.DIREWOLF.get(), DirewolfRenderer::new);     // geo models (tools/gen_dungeon_mobs.py)
        e.registerEntityRenderer(DungeonEntities.BASILISK.get(), BasiliskRenderer::new);
    }
}
