package com.newuniverse.nusmp.labyrinth.mega.decennial;

import com.newuniverse.nusmp.boss.rework.ReworkFx;
import com.newuniverse.nusmp.boss.rules.BossRulesConfig;
import com.newuniverse.nusmp.labyrinth.mega.decennial.DecennialFamilies.Family;
import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Boss rework wave 3, part 3 (docs/bosses/rework_wave3.md): the visual signature and the extra move of the tier family of a Decennial boss ({@link DecennialFamilies}). One instance
 * per boss, runtime state only (a reload restarts the timers, the kind direction to be wrong in). The move is a floor shape shown for {@code warn} ticks (particles every fourth
 * tick, at most 40 points) and then landing once: each survival player in the hall inside the shape takes a share of the boss's base attack after the player's hit cap
 * ({@link DecennialFx#hurt}) and the family's effect from the closed effect list; nothing is spawned, no block changes, no food bar is touched. The signature effect (one shaped
 * effect through the boss effect budget, {@code ReworkFx}: 12 a tick, players near only) goes out at the boss's feet every 3 seconds of a fight and as the move lands. The move
 * is off with the server setting {@code bossRework3DecennialMoves}, the effects with {@code bossRework1Vfx}.
 *
 * <p>Worst case per boss per tick: one timer comparison; per cast (one in at least 8 seconds): the players in the hall hurt once each, 40 particles per fourth tick of the warning,
 * one budgeted shaped effect.
 */
final class FamilyMoves {
    private static final long AMBIENT_EVERY = 60;
    private static final double LINE_FROM_BOSS_OFFSET = 1.0;

    private final Family fam;
    private long nextAt = -1, landAt = -1, ambientAt = -1;
    private double ax, ay, az, dx, dz;

    FamilyMoves(Family fam) { this.fam = fam; }

    Family family() { return fam; }

    void reset() {
        nextAt = -1;
        landAt = -1;
        ambientAt = -1;
    }

    /** Called every tick of a fight (and not during a phase shift). */
    void tick(LabyrinthGuardianEntity g, ServerLevel sl, long now, List<Player> players) {
        if (fam == null) return;
        ambient(g, sl, now);
        if (!BossRulesConfig.rework3Decennial() || players.isEmpty()) return;
        if (landAt >= 0) {
            if (now % 4 == 0) show(sl);
            if (now >= landAt) land(g, sl, players);
            return;
        }
        if (nextAt < 0) nextAt = now + fam.period() * 10L;                            // the first one after half a period
        if (now < nextAt) return;
        nextAt = now + fam.period() * 20L;
        Player t = players.get(sl.random.nextInt(players.size()));
        switch (fam.pattern()) {
            case LINE -> {
                double ex = t.getX() - g.getX(), ez = t.getZ() - g.getZ(), len = Math.hypot(ex, ez);
                if (len < 1e-3) { ex = 0; ez = 1; len = 1; }
                dx = ex / len;
                dz = ez / len;
                ax = g.getX() + dx * LINE_FROM_BOSS_OFFSET;
                az = g.getZ() + dz * LINE_FROM_BOSS_OFFSET;
                ay = g.getY();
            }
            default -> {
                ax = t.getX();
                ay = t.getY();
                az = t.getZ();
                dx = 0;
                dz = 1;
            }
        }
        landAt = now + fam.warn();
        sl.playSound(null, g.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 1.2f, 0.5f);
    }

    /** The signature effect at the boss's feet now and then (budgeted; off with the effects setting). */
    private void ambient(LabyrinthGuardianEntity g, ServerLevel sl, long now) {
        if (now < ambientAt) return;
        ambientAt = now + AMBIENT_EVERY;
        Vec3 feet = g.position().add(0, 0.1, 0);
        shape(sl, feet, feet.add(0, 1.5, 0), 40, Math.min(2.4, 0.8 + 0.5 * g.getScale()));
    }

    private void shape(ServerLevel sl, Vec3 from, Vec3 to, int ticks, double size) {
        try {
            ReworkFx.plain(sl, VfxShape.valueOf(fam.shape()), from, to, fam.rgb(), ticks, size);
        } catch (IllegalArgumentException unknown) {
            // a data row naming a shape the enum lacks draws no signature (the tests refuse such a row)
        }
    }

    /** The warning on the floor: the outline of the shape in particles. */
    private void show(ServerLevel sl) {
        var type = Fx.options(FxThemes.DUNGEON_DECENNIAL, FxOld.END_ROD);
        switch (fam.pattern()) {
            case LINE -> DecennialFx.line(sl, type, ax, ay + 0.2, az, ax + dx * fam.radius(), ay + 0.2, az + dz * fam.radius(), 24);
            case DONUT -> {
                DecennialFx.ring(sl, type, ax, ay + 0.15, az, fam.radius(), 28);
                DecennialFx.ring(sl, type, ax, ay + 0.15, az, fam.radius() * 0.45, 14);
            }
            default -> DecennialFx.ring(sl, type, ax, ay + 0.15, az, fam.radius(), 32);
        }
    }

    private void land(LabyrinthGuardianEntity g, ServerLevel sl, List<Player> players) {
        landAt = -1;
        double amount = DecennialFx.baseAttack(g) * fam.share();
        for (Player p : players) {
            if (!DecennialFamilies.inside(fam.pattern(), fam.radius(), ax, az, dx, dz, p.getX(), p.getZ()) || Math.abs(p.getY() - ay) > 4.0) continue;
            if (DecennialFx.hurt(g, p, amount, true) && fam.effect() != DecennialVocab.Effect.NONE) DecennialFx.effect(g, sl, p, fam.effect(), fam.effectTicks(), ax, az);
        }
        Vec3 at = new Vec3(ax, ay + 0.1, az);
        if (fam.pattern() == DecennialFamilies.Pattern.LINE) shape(sl, at, at.add(dx * fam.radius(), 0, dz * fam.radius()), 24, 1.6);
        else shape(sl, at, at.add(0, 1, 0), 30, Math.min(3.0, fam.radius() * 0.4));
        sl.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.8f, 1.3f);
    }
}
