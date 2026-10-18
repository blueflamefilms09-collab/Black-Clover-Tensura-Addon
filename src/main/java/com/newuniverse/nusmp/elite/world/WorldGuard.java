package com.newuniverse.nusmp.elite.world;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import net.minecraft.world.level.Level;

/**
 * Why: one answer to "may the world engine act in this level". The engine changes blocks (crystal growth, ore conversion), spawns and replaces mobs and wakes bosses, none of which may
 * ever happen in the Dungeon dimensions (their blocks are held by {@code DungeonGuard}) or in the Hub (the Sanctuary, passive mobs only), and nothing may happen when the master switch is off.
 * The engine acts only in the dimensions listed in the config ({@code magicule.dimensions}, the Overworld by default).
 */
public final class WorldGuard {
    private WorldGuard() {}

    /** The dimensions that belong to the Dungeon or the Hub or a boss arena, which the engine never touches whatever the config says. */
    public static boolean protectedLevel(Level level) {
        if (level == null) return true;
        try {
            if (DungeonGuard.on(level)) return true;
            if (com.newuniverse.nusmp.labyrinth.Labyrinth.in(level) || com.newuniverse.nusmp.labyrinth.Labyrinth.inSanctuary(level) || com.newuniverse.nusmp.labyrinth.Labyrinth.inRemovedDungeon(level)) return true;
            if (com.newuniverse.nusmp.arena.ZogratisArena.in(level) || com.newuniverse.nusmp.arc.arena.ArcArena.in(level)) return true;
        } catch (RuntimeException unreadable) {
            return true;                                   // when in doubt, leave the level alone
        }
        return false;
    }

    /** May the engine act in this level now (switch on, a listed dimension, not protected)? */
    public static boolean allowed(Level level) {
        if (level == null || level.isClientSide() || !WorldConfig.enabled() || protectedLevel(level)) return false;
        String dim = level.dimension().location().toString();
        return WorldConfig.dimensions().contains(dim);
    }
}
