package com.newuniverse.nusmp.dungeon.rework2;

import com.newuniverse.nusmp.boss.rules.BossRulesConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

/**
 * Why: the one public door for code outside the dungeon-boss package (the Foundry mobs' telegraph goal) to play a cue of the boss sound set ({@link DungeonBossSoundRules}). SERVER ONLY: it plays through the
 * level, so the game sends the sound to the players inside its range only; it plays nothing while {@code dungeonBosses.sounds} (or the master switch) is off, so the old silent behaviour is exactly what the
 * setting restores; and it goes through the caller's own {@link DungeonBossSoundRules.Limiter} (one per mob: a cooldown per cue and four a second), so a hall full of mobs cannot flood the sound engine.
 */
public final class BossCues {
    private BossCues() {}

    /** Plays {@code cue} from {@code at} at the cue's volume times {@code volumeScale} (0 to 1), if the setting is on and the limiter lets it through. */
    public static void play(ServerLevel sl, Entity at, DungeonBossSoundRules.Limiter limiter, String cue, float volumeScale) {
        DungeonBossSoundRules.Cue c = DungeonBossSoundRules.cue(cue);
        if (c == null || at == null || at.isSilent() || !BossRulesConfig.dungeonSounds()) return;
        SoundEvent ev = DungeonBossSounds.event(cue);
        if (ev == null || (limiter != null && !limiter.allow(cue, sl.getGameTime()))) return;
        sl.playSound(null, at.getX(), at.getY(), at.getZ(), ev, SoundSource.HOSTILE, c.volume() * Math.max(0f, Math.min(1f, volumeScale)), c.pitch(sl.getRandom().nextDouble()));
    }
}
