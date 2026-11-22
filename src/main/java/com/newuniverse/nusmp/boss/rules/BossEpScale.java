package com.newuniverse.nusmp.boss.rules;

import com.newuniverse.nusmp.avatar.TensuraBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why (4.102.0): the owner asked that a boss's base Existence Points (EP) follow the players it fights, set when it spawns in its dimension, heaviest for Goku, middle for Sinbad, lenient for Kakashi.
 * {@link #base} is the pure rule (own EP raised to strictness x the strongest player's EP x a small group bonus); {@link #applyAtSpawn} reads the players of the boss's own dimension ONCE and writes the
 * result with Tensura's EP helper. Server thread only. Cost: one pass over the dimension's players (at most {@link #MAX_READ} EP reads) and one EP write per boss spawn; nothing per tick.
 */
public final class BossEpScale {
    private BossEpScale() {}

    public static final int MAX_READ = 64;
    public static final int MAX_COUNTED = 10;
    public static final double CAP = 1.0e18;
    public static final double GOKU = 1.0, SINBAD = 0.6, KAKASHI = 0.3, OTHER = 0.4;

    /** The share of the players' EP this boss takes (before the setting's multiplier). */
    public static double strictness(String characterId) {
        if (characterId == null) return OTHER;
        String id = characterId.contains(":") ? characterId.substring(characterId.indexOf(':') + 1) : characterId;
        return switch (id) {
            case "goku", "goku_boss" -> GOKU;
            case "sinbad", "sinbad_boss" -> SINBAD;
            case "kakashi", "kakashi_hatake", "kakashi_boss" -> KAKASHI;
            default -> OTHER;
        };
    }

    /** The EP a boss with {@code own} EP starts with: never lower than {@code own}; {@code strictness x mult x topPlayerEp x (1 + perPlayer x (players-1))} when that is more. Bad numbers change nothing. */
    public static double base(double own, double topPlayerEp, int players, double strictness, double mult, double perPlayer) {
        double o = own > 0.0 && Double.isFinite(own) ? own : 0.0;
        if (!(topPlayerEp > 0.0) || !Double.isFinite(topPlayerEp) || !(strictness > 0.0) || !(mult > 0.0) || !Double.isFinite(strictness * mult)) return o;
        double group = 1.0 + Math.max(0.0, Math.min(2.0, Double.isNaN(perPlayer) ? 0.0 : perPlayer)) * (Math.max(1, Math.min(players, MAX_COUNTED)) - 1);
        return Math.min(CAP, Math.max(o, topPlayerEp * strictness * mult * group));
    }

    /** The strongest EP among the players of the level (alive, not creative or spectator), and how many there are. {@code out[0]} = top EP, {@code out[1]} = count. */
    public static double[] readPlayers(ServerLevel sl) {
        double top = 0.0;
        int n = 0;
        for (ServerPlayer p : sl.players()) {
            if (n >= MAX_READ) break;
            if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            n++;
            top = Math.max(top, TensuraBridge.maxEp(p));
        }
        return new double[] {top, n};
    }

    /** Smart spawn: raises the new boss's base EP from the players of its dimension. Returns the EP written, or 0 when nothing changed. */
    public static double applyAtSpawn(ServerLevel sl, LivingEntity boss, String characterId) {
        if (!BossRulesConfig.epFromPlayers() || sl.isClientSide) return 0.0;
        double[] r = readPlayers(sl);
        double own = TensuraBridge.baseMaxEp(boss);
        double want = base(own, r[0], (int) r[1], strictness(characterId), BossRulesConfig.epStrictness(), BossRulesConfig.epPerPlayer());
        if (want <= own) return 0.0;
        try {
            io.github.manasmods.tensura.util.EnergyHelper.setBaseMaxEP(boss, want);
            return want;
        } catch (RuntimeException | LinkageError noTensuraData) {
            return 0.0;                                                  // no Tensura data on this creature: left as it was
        }
    }
}
