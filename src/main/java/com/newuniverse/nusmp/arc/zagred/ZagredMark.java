package com.newuniverse.nusmp.arc.zagred;

import com.newuniverse.nusmp.entity.zogratis.DanteMoves;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Why: a verdict gives a player a word and three seconds, and "did they answer" must be judged on what the player really did over the last half second, not
 * on one tick (a player's position reaches the server in packets, and one tick of lag must not turn a good answer into a failure). This is the tracker of one
 * marked player: every tick it records how far they moved, whether they were off the ground, whether they held a shield up, and at the deadline it turns
 * that into the {@link ZagredAnswers.Snapshot} the pure rule judges. It holds no world state beyond the player's id.
 */
final class ZagredMark {
    final UUID id;
    final ZagredWord word;
    final long deadline;
    private final double[] steps = new double[10];
    private int cursor, sinceAir = 99, sinceShield = 99;
    private Vec3 last;

    ZagredMark(UUID id, ZagredWord word, long deadline) {
        this.id = id;
        this.word = word;
        this.deadline = deadline;
    }

    /** Called once per tick while the verdict is open. */
    void observe(ServerPlayer p) {
        Vec3 now = p.position();
        steps[cursor] = last == null ? 0 : Math.hypot(now.x - last.x, now.z - last.z);
        cursor = (cursor + 1) % steps.length;
        last = now;
        sinceAir = p.onGround() ? Math.min(99, sinceAir + 1) : 0;
        sinceShield = p.isBlocking() ? 0 : Math.min(99, sinceShield + 1);
    }

    /** The facts at this moment, from the boss's point of view (his line of sight, where his eyes are). */
    ZagredAnswers.Snapshot snapshot(ServerPlayer p, LivingEntity boss) {
        double moved = 0;
        for (double s : steps) moved += s;
        Vec3 toBoss = boss.getEyePosition().subtract(p.getEyePosition());
        double gaze = toBoss.lengthSqr() < 1e-6 ? 1.0 : p.getViewVector(1f).normalize().dot(toBoss.normalize());
        return new ZagredAnswers.Snapshot(sinceAir <= 5, moved, sinceShield <= 10, boss.hasLineOfSight(p), DanteMoves.antiMagicHeld(p), gaze);
    }
}
