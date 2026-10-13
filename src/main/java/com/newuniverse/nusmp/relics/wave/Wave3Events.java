package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.RelicFlags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Wave 3: the game-bus handlers, registered once by {@link Wave3#hooks}. Each leaves at once for anyone the wave does not concern: the
 * relics' own flag {@link Wave3Rules#F_ANY} is one cached lookup, and nothing here scans the world on a tick.
 */
final class Wave3Events {
    private Wave3Events() {}

    /** Once a second per player (staggered by entity id), plus the particle ring of the Circlet's motes every fourth tick. */
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !p.isAlive()) return;
        int phase = (p.tickCount + p.getId()) % 20;
        if (phase == 3) {                                                                // clocks that must run even if the relic came off
            Wave3Head.ascensionPass(p);
            Wave3Belt.pass(p);
            Wave3Hands.gauntletPass(p);
        }
        if (phase % 4 != 1 || !RelicFlags.has(p, Wave3Rules.F_ANY)) return;
        if (RelicFlags.has(p, Wave3Rules.F_MOTES)) Wave3Head.orbit(p, p.level().getGameTime());
        if (phase != 5) return;
        int second = p.tickCount / 20;
        if (second % 2 == 0 && RelicFlags.has(p, Wave3Rules.F_PRIDE)) Wave3Head.prideAura(p);
        if (RelicFlags.has(p, Wave3Rules.F_TRICORN)) Wave3Head.tricornPass(p);
        if (second % 5 == 0 && RelicFlags.has(p, Wave3Rules.F_CLEAN)) Wave3Hands.cleanPass(p);
    }

    static void onServerTick(ServerTickEvent.Post e) { Wave3Field.tick(e.getServer()); }

    /** A blow landed: it frees a creature its maker had charmed; bare fists with the wraps or Thermo-Gloves deal their extra; the claws may snatch. */
    static void onDamagePost(LivingDamageEvent.Post e) {
        DamageSource src = e.getSource();
        Entity attacker = src.getEntity();
        if (attacker == null) return;
        LivingEntity victim = e.getEntity();
        Wave3Field.struck(victim, attacker);
        if (attacker instanceof ServerPlayer p && src.getDirectEntity() == p && src.is(DamageTypes.PLAYER_ATTACK)) Wave3Hands.onHit(p, victim);
    }

    static void onIncoming(LivingIncomingDamageEvent e) { Wave3Belt.fortitude(e); }

    /** Pride's Crown turns a charm or a fright back; the Dreamwalker Cowl refuses magical sleep. */
    static void onApplicable(MobEffectEvent.Applicable e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        MobEffect eff = e.getEffectInstance().getEffect().value();
        boolean sleep = Wave3Head.isSleepEffect(eff);
        boolean mind = !sleep && Wave3Head.isMindEffect(eff);
        if (!sleep && !mind) return;
        if (sleep && RelicFlags.has(p, Wave3Rules.F_DREAMER)) e.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        else if (mind && RelicFlags.has(p, Wave3Rules.F_PRIDE)) Wave3Head.hubris(p, e);
    }

    /** A creature that has been charmed or calmed does not pick the one it was charmed for (or, if calm, anyone) as its target. */
    static void onChangeTarget(LivingChangeTargetEvent e) {
        LivingEntity target = e.getNewAboutToBeSetTarget();
        if (target != null && Wave3Field.spares(e.getEntity(), target)) e.setCanceled(true);
    }

    /** A night's sleep: the Dreamwalker Cowl's charges come back, the Gauntlets' price is paid off. */
    static void onWake(PlayerWakeUpEvent e) {
        if (e.wakeImmediately() || !(e.getEntity() instanceof ServerPlayer p)) return;
        Wave3Head.dreamRecharge(p);
        Wave3Hands.rested(p);
    }

    static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) Wave3Head.forget(p);
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) Wave3Head.forget(p);
    }

    static void onStopping(ServerStoppingEvent e) { Wave3Field.clear(); }
}
