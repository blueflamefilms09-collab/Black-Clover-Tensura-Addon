package com.newuniverse.nusmp.boss.rework3;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.arc.tier.TierMoves;
import com.newuniverse.nusmp.boss.rework.ReworkFx;
import com.newuniverse.nusmp.boss.rules.BossRulesConfig;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.List;

/**
 * Boss rework wave 3, part 2 (docs/bosses/rework_wave3.md): turns the row of {@link Rework3Rows} of a boss into a framework {@link Move} (the bounded shape, its own telegraph and
 * the damage law come from {@link TierMoves#build}) and adds the themed effect that goes out as the move lands: one shaped effect per cast through the boss effect budget
 * ({@code ReworkFx}: 12 a tick, players near only), only while {@code bossRework1Vfx} is on. Server side only; no state, no entity spawned, no block touched. With the server
 * setting {@code bossRework3Moves} off {@link #forBoss} answers an empty list: the boss fights with the list it had.
 */
public final class Rework3Moves {
    private static final Logger LOG = LogUtils.getLogger();

    private Rework3Moves() {}

    /** Are the appended moves of this wave on? */
    public static boolean on() { return BossRulesConfig.rework3Moves(); }

    /** The moves appended to the boss of this character id (at most one), or an empty list when it has none or the setting is off. */
    public static List<Move> forBoss(String bossId) {
        Rework3Rows.Row row = Rework3Rows.of(bossId);
        if (row == null || !on()) return List.of();
        try {
            Move base = TierMoves.build(row.move());
            return List.of(Move.builder(base.id(), base.label()).minPower(base.minPower()).windUp(base.windUpTicks()).cooldown(base.cooldownTicks())
                    .range(base.minRange(), base.maxRange()).weight(base.weight()).tell(base.tell()).run((boss, target) -> {
                        base.run().run(boss, target);
                        land(row, boss, target);
                    }));
        } catch (RuntimeException ex) {
            LOG.warn("[anime_multiverse] the wave 3 move of {} could not be built: {}", bossId, ex.toString());
            return List.of();
        }
    }

    /** The themed effect of the move landing: at the target for a marked strike, from the eye toward the target for a line, round the boss otherwise. */
    private static void land(Rework3Rows.Row row, ZogratisBoss boss, LivingEntity target) {
        if (!ReworkFx.on() || !(boss.level() instanceof ServerLevel sl)) return;
        try {
            VfxShape shape = VfxShape.valueOf(row.shape());
            Vec3 pos = boss.position(), eye = boss.getEyePosition();
            Vec3 mid = target == null ? pos.add(0, 1, 1) : target.getBoundingBox().getCenter();
            Vec3 flat = new Vec3(mid.x - pos.x, 0, mid.z - pos.z);
            Vec3 face = flat.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : flat.normalize();
            switch (row.move().kind()) {
                case MARKED -> {
                    Vec3 g = ZogratisBoss.ground(sl, mid).add(0, 0.05, 0);
                    ReworkFx.plain(sl, shape, g, g.add(0, 1, 0), row.rgb(), 30, row.fxSize());
                }
                case BEAM, VOLLEY, BOLT -> ReworkFx.plain(sl, shape, eye, eye.add(face.scale(Math.min(row.move().size(), 24.0))), row.rgb(), 18, row.fxSize());
                case CLEAVE -> ReworkFx.plain(sl, shape, mid, mid.add(face), row.rgb(), 16, row.fxSize());
                default -> ReworkFx.plain(sl, shape, pos.add(0, 0.1, 0), pos.add(face.scale(row.move().size())).add(0, 0.1, 0), row.rgb(), 24, row.fxSize());
            }
        } catch (RuntimeException ex) {
            LOG.debug("[anime_multiverse] the wave 3 effect {} failed: {}", row.move().id(), ex.toString());
        }
    }
}
