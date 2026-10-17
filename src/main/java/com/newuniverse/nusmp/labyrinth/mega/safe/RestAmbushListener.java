package com.newuniverse.nusmp.labyrinth.mega.safe;

import com.newuniverse.nusmp.labyrinth.mega.Sector;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: a long rest in the dungeon is a bet. Its chance of an ambush grows with the sector's threat ({@link RestRules#ambushChance}) so that
 * sleeping deep in the maze costs something, but the Safe-Zone builder must not spawn monsters itself: choosing what comes, from where and how
 * hard is the Dungeon Master's job (the encounter director), and the two are written apart and must not import each other. So the rest system
 * only ANNOUNCES the ambush, part-way through the rest of a player whose roll came up, to whoever registered here with
 * {@link RestEvents#addAmbushListener}; the default is that nobody listens and nothing happens. The ambush then plays out through the rest's own
 * rules: monsters that arrive near the bed or hurt the sleeper break the rest, so the party that is "defending the zone" has to wake up and fight.
 */
@FunctionalInterface
public interface RestAmbushListener {
    /**
     * Called once, on the server thread, part-way through the long rest of {@code player} in a sector of the given threat. Return true when the
     * ambush was handled (something was spawned or announced); the rest system sends its own warning only if some listener says yes.
     */
    boolean onAmbush(ServerPlayer player, Sector sector, int threat);
}
