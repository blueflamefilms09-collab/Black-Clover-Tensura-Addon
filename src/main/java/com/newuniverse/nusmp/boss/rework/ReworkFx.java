package com.newuniverse.nusmp.boss.rework;

import com.newuniverse.nusmp.boss.rules.BossRulesConfig;
import com.newuniverse.nusmp.boss.vfx.BossMoveVfx;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the small things every boss of rework wave 1 does the same way (docs/bosses/rework_wave1.md): a signature effect that goes out only while the setting
 * {@code bossRework1Vfx} is on and always through the boss effect budget ({@link BossMoveVfx}: 12 a tick, players near only), and a roar or a field that cancels magic by
 * taking timed blessings off a foe. Server side only, never touches a block, keeps no state.
 */
public final class ReworkFx {
    private ReworkFx() {}

    /** Are the shaped effects of this wave on? */
    public static boolean on() { return BossRulesConfig.reworkVfx(); }

    /** Are the appended moves of this wave on? */
    public static boolean moves() { return BossRulesConfig.reworkMoves(); }

    /** One shaped effect of a move landing (a {@code BOSS_*} shape is marked as a strike). False when the setting is off or this tick's budget is spent. */
    public static boolean strike(ServerLevel sl, VfxShape shape, Vec3 from, Vec3 to, int rgb, double size) {
        return on() && BossMoveVfx.strike(sl, shape, from, to, rgb, size);
    }

    /** One shaped effect of a move landing that lasts {@code ticks}. */
    public static boolean strikeFor(ServerLevel sl, VfxShape shape, Vec3 from, Vec3 to, int rgb, int ticks, double size) {
        return on() && BossMoveVfx.strikeFor(sl, shape, from, to, rgb, ticks, size);
    }

    /** The wind-up of a move, drawn once (the particle tells keep running every third tick whatever this setting says). */
    public static boolean telegraph(ServerLevel sl, VfxShape shape, Vec3 from, Vec3 to, int rgb, int windUp, double size) {
        return on() && BossMoveVfx.telegraph(sl, shape, from, to, rgb, windUp, size);
    }

    /** A shape of another family (a gravity sphere, a pillar, a circle, a crescent) through the same budget. */
    public static boolean plain(ServerLevel sl, VfxShape shape, Vec3 from, Vec3 to, int rgb, int ticks, double size) {
        return on() && BossMoveVfx.plain(sl, shape, from, to, rgb, ticks, size);
    }

    /** Takes up to {@code max} beneficial timed effects off {@code e} (spells and blessings cancelled; a permanent effect, as a skill's own, is left); returns how many. */
    public static int cancelMagic(LivingEntity e, int max) {
        if (e == null || max <= 0) return 0;
        List<Holder<MobEffect>> found = new ArrayList<>(2);
        for (MobEffectInstance i : e.getActiveEffects()) {
            if (!i.getEffect().value().isBeneficial() || i.isInfiniteDuration()) continue;
            found.add(i.getEffect());
            if (found.size() >= max) break;
        }
        int n = 0;
        for (Holder<MobEffect> h : found) if (e.removeEffect(h)) n++;
        if (n > 0 && e instanceof ServerPlayer p) p.displayClientMessage(n == 1
                ? Component.translatableWithFallback("anime_multiverse.boss_rework1.blessings_torn", "A blessing on you was torn away.")
                : Component.translatableWithFallback("anime_multiverse.boss_rework1.blessings_torn_many", n + " blessings on you were torn away.", n), true);
        return n;
    }
}
