package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.rift.RiftRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Why: "never fires inside safe zones, the Hub (Floor 0) or rift pockets". One place answers both questions the engine has: may a mechanic exist at a position, and may a
 * player be touched by one. The Hub is a different dimension ({@code nusmp:sanctuary}), so "only the Mega-Dungeon" excludes Floor 0 by construction; a rift pocket is a cell in a
 * far field of the Mega-Dungeon's own dimension ({@link RiftRules#slotAt}); a safe zone is a Landing, a Safe Zone room or a stairwell ({@code MegaDungeon.inSafeZone}), where
 * the rest anchors are and where nothing may hurt a resting party. A creative or spectating player is never affected (they may still wake a mechanic when the config lets them).
 */
public final class Safety {
    private Safety() {}

    /** May a mechanic act at this position? */
    public static boolean allowedAt(Level level, BlockPos pos) {
        if (level == null || pos == null || !MegaDungeon.in(level)) return false;
        if (RiftRules.slotAt(pos.getX(), pos.getZ()) >= 0) return false;
        return !MegaDungeon.inSafeZone(level, pos);
    }

    /** May this player be affected by a mechanic where they stand? */
    public static boolean affects(ServerPlayer p) {
        return p != null && p.isAlive() && !p.isCreative() && !p.isSpectator() && allowedAt(p.level(), p.blockPosition());
    }
}
