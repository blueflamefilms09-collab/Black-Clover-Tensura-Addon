package com.newuniverse.nusmp.skilltree.shop;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.arc.ArcNpcEntity;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Why: the witch is an ordinary {@code ArcNpcEntity}, the one entity every friendly character shares, and that class must not learn what a shop is. So the whole connection is
 * one door: the entity's interaction method asks {@link #tryOpen} first (the single three-line edit the design allows in that class) and carries on with the normal conversation
 * when the answer is false. The door answers true only for the witch ({@link BlackMarketWitch#ID}, so no other character can ever open a shop), only while the shop is switched on,
 * and only when the player is sneaking: a plain right click keeps the dialogue, where she hints that a sneaking player gets to see her wares, which is how the owner's "hidden"
 * shop is discovered. It never throws: an error in the shop must not turn into a kicked player or a dead conversation, so any exception is logged and answered with false.
 */
public final class ShopHook {
    private ShopHook() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** True when this right click opened the shop (the caller then skips the dialogue). */
    public static boolean tryOpen(ServerPlayer player, ArcNpcEntity npc) {
        try {
            if (npc == null || player == null || !BlackMarketWitch.ID.equals(npc.characterId())) return false;
            if (!ShopConfig.enabled() || !player.isShiftKeyDown()) return false;
            return ShopService.open(player, npc);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] the Black Market could not open for {}: {}", player == null ? "nobody" : player.getGameProfile().getName(), e.toString());
            return false;
        }
    }
}
