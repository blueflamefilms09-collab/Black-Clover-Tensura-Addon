package com.newuniverse.nusmp.saiyan;

import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxSlot;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.TamableAnimal;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: the Super Saiyan God ritual (Dragon Ball rework, phase 4; the spec's "Battle of Gods" scene) and the Divine Saiyan gift. A Saiyan with the mastered Super Saiyan and enough power starts it with
 * {@code /saiyan ritual}; the other players (five by default) stand in a ring 2.5 to 7.5 blocks away and sneak until the time is up, and the caster is given the Super Saiyan God form and the ritual flag (stage 3
 * of the race). The power is Battle Power by default or Tensura Existence Points on the owner's say ({@code phase.owner_decisions.ritualUsesExistencePoints}). Everything is checked on the server every ten
 * ticks; a broken ring stops it and starts a short retry delay. Cost: at most {@code ritual.maxAtOnce} rituals on the whole server (a UUID set), each one player query of at most 8 helpers every ten ticks; the
 * gift is one query of at most 8 tame animals, once per ritual.
 */
final class SaiyanRitual {
    private SaiyanRitual() {}

    /** The casters whose ritual runs (UUIDs only; the cap of the whole server). */
    private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();

    static int running() { return ACTIVE.size(); }

    /** Drops a caster's ritual (death, logout, a disabled system). */
    static void end(ServerPlayer p) { ACTIVE.remove(p.getUUID()); }

    private static double existencePoints(ServerPlayer p) {
        try { return EnergyHelper.getMaxEP(p); } catch (Throwable ignored) { return 0.0; }
    }

    /** True if this Saiyan may start the ritual now (said to the player when not). */
    static boolean start(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        if (!SaiyanConfig.enabled() || !SaiyanPhaseConfig.enabled() || !SaiyanPhaseConfig.ritualEnabled() || !s.saiyan) return false;
        if (r.tech4.ritualStart >= 0) return false;
        if (!s.ready("ritual", now)) {
            SaiyanAbilities.say(p, "t4_ritual_wait");
            return false;
        }
        boolean done = s.flags.contains(SaiyanRaceRules.RITUAL_FLAG) || s.unlocked.contains("super_saiyan_god");
        boolean mastered = s.mastery("super_saiyan") >= 1.0;
        boolean ok = SaiyanRitualRules.casterReady(SaiyanPhaseConfig.ritualUsesEp(), s.battlePower, SaiyanPhaseConfig.ritualBp(), existencePoints(p), SaiyanPhaseConfig.ritualEp(), mastered, done);
        if (!ok) {
            SaiyanAbilities.say(p, done ? "t4_ritual_done" : "t4_ritual_not_ready");
            return false;
        }
        if (ACTIVE.size() >= SaiyanPhaseConfig.ritualMax()) {
            SaiyanAbilities.say(p, "t4_ritual_busy");
            return false;
        }
        ACTIVE.add(p.getUUID());
        r.tech4.ritualStart = now;
        r.dirty = true;
        p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 0.5f);
        SaiyanAbilities.say(p, "t4_ritual_start");
        return true;
    }

    /** Every tick while a ritual runs; the circle is read only every tenth tick. */
    static void tick(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        SaiyanTechniques4.State t = r.tech4;
        if (!p.isAlive() || p.isSpectator() || !SaiyanPhaseConfig.ritualEnabled() || !SaiyanPhaseConfig.enabled()) {
            stop(p, s, r, now, false);
            return;
        }
        if (!SaiyanRitualRules.checkTick(now, t.ritualStart)) return;
        int need = SaiyanPhaseConfig.ritualHelpers(), ticks = SaiyanPhaseConfig.ritualTicks();
        ServerLevel level = p.serverLevel();
        int helpers = 0;
        for (ServerPlayer o : level.getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(SaiyanRitualRules.MAX_RING, 4.0, SaiyanRitualRules.MAX_RING),
                x -> x != p && x.isAlive() && !x.isSpectator() && x.isShiftKeyDown())) {
            if (SaiyanRitualRules.inRing(o.getX() - p.getX(), o.getZ() - p.getZ())) helpers++;
            if (helpers >= SaiyanRitualRules.MAX_HELPERS) break;
        }
        if (!SaiyanRitualRules.circleHolds(helpers, need)) {
            stop(p, s, r, now, false);
            return;
        }
        for (int k = 0; k < 6; k++) {
            double a = (now * 0.15) + k * Math.PI / 3.0;
            Fx.sprayTinted(level, FxThemes.WORLD, FxSlot.SPARK, 0xFF3030, 1.8f, p.getX() + Math.cos(a) * 1.4, p.getY() + 0.6 + (now % 40) * 0.04, p.getZ() + Math.sin(a) * 1.4, 1, 0.05, 0.1, 0.05, 0.02);
        }
        p.displayClientMessage(Component.translatable("anime_multiverse.saiyan.msg.t4_ritual_progress", SaiyanRitualRules.percent(now, t.ritualStart, ticks)), true);
        if (SaiyanRitualRules.done(now, t.ritualStart, ticks)) stop(p, s, r, now, true);
    }

    private static void stop(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now, boolean success) {
        r.tech4.ritualStart = -1;
        ACTIVE.remove(p.getUUID());
        r.dirty = true;
        if (!success) {
            s.startCooldown("ritual", now, SaiyanPhaseConfig.ritualRetryTicks());
            SaiyanAbilities.say(p, "t4_ritual_broken");
            return;
        }
        s.unlock("super_saiyan_god");
        s.grantFlag(SaiyanRaceRules.RITUAL_FLAG);
        ServerLevel level = p.serverLevel();
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.0f, 1.5f);
        Fx.sprayTinted(level, FxThemes.WORLD, FxSlot.SPARK, 0xFF3030, 2.6f, p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.8, 1.0, 0.8, 0.3);
        if (SaiyanPhaseConfig.giftsEnabled() && SaiyanPhaseConfig.giftTargets() > 0) gift(p);
        SaiyanAbilities.say(p, "t4_ritual_done_now");
    }

    /** The Divine Saiyan gift: the caster's own tame animals nearby (at most 8) are blessed for a few minutes. */
    private static void gift(ServerPlayer p) {
        List<TamableAnimal> pets = p.serverLevel().getEntitiesOfClass(TamableAnimal.class, p.getBoundingBox().inflate(16.0), a -> a.isAlive() && a.isTame() && a.isOwnedBy(p));
        int n = 0;
        for (TamableAnimal a : pets) {
            if (n++ >= SaiyanPhaseConfig.giftTargets()) break;
            a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, SaiyanPhaseConfig.giftTicks(), 1));
            a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, SaiyanPhaseConfig.giftTicks(), 0));
        }
    }
}
