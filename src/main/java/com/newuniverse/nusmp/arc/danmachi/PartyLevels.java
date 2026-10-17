package com.newuniverse.nusmp.arc.danmachi;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: {@link DungeonLevel} is pure and knows only numbers; the game has to feed it the existence points (EP) of the fighters that stand near a boss. This
 * is that one reading, so the Minotaur's hunt, the Howl's fear and the Level-Up at the Gates all measure a fighter the same way (the Tensura EP the whole
 * balance law already reads, through {@link Nullification#maxEP}, which is 0 for a fighter without Tensura data and therefore Level 1).
 */
final class PartyLevels {
    private PartyLevels() {}

    /** The foes within this many blocks of the boss are its party (players first; a lone summon is measured only when no player is near). */
    static final double RADIUS = 48.0;

    /** The Dungeon Level of one fighter. */
    static int levelOf(LivingEntity e) {
        return DungeonLevel.levelOfEp(Nullification.maxEP(e), DanMachiConfig.levelBaseEp(), DanMachiConfig.levelDecades());
    }

    /** The Levels of the players near the boss (the summons of players when there is no player), in no particular order. */
    static int[] levelsAround(ZogratisBoss boss) {
        List<LivingEntity> foes = boss.foesAround(boss.position(), RADIUS);
        List<LivingEntity> players = new ArrayList<>();
        for (LivingEntity e : foes) if (e instanceof Player) players.add(e);
        List<LivingEntity> measured = players.isEmpty() ? foes : players;
        int[] out = new int[measured.size()];
        for (int i = 0; i < out.length; i++) out[i] = levelOf(measured.get(i));
        return out;
    }

    /** The party's Level by the configured mode; {@link DungeonLevel#NO_PARTY} when nobody is near. */
    static int partyLevel(ZogratisBoss boss) {
        return DungeonLevel.partyLevel(levelsAround(boss), DanMachiConfig.partyMode());
    }
}
