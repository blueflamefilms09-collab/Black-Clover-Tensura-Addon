package com.newuniverse.nusmp.entity.zogratis;

import com.newuniverse.nusmp.boss.god.DodgeEvents;
import com.newuniverse.nusmp.boss.god.FlightGuard;
import com.newuniverse.nusmp.boss.god.LuciusGodConfig;
import com.newuniverse.nusmp.boss.god.RegenGuard;
import com.newuniverse.nusmp.boss.god.StarvationMath;
import com.newuniverse.nusmp.relics.Severance;
import com.newuniverse.nusmp.relics.djinn.DjinnEngine;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: half of the god-tier Lucius is about what happens to <i>players</i> near him, and the game tells us about players through its
 * event bus, not through the boss: a heal arrives as a {@code LivingHealEvent}, a player's flight is a flag we must look at every tick,
 * an ender pearl is an {@code EntityTeleportEvent}, a hit that a dodge system cancelled arrives as a cancelled
 * {@code LivingIncomingDamageEvent}. This class is the one subscriber for all of it (wire it once from NUSMP with
 * {@link #register()}); it finds the boss through the registry of live god-tier fights ({@link LuciusGod}) and does nothing at all
 * while there is none, so a server without the fight pays one empty-set check per player tick.
 * <ul>
 *   <li><b>Time Decay</b> (anti-regeneration aura, radius {@code luciusAuraRadius}): every heal event of a player or a player's pet in the
 *       aura is cancelled unless a potion or food is being used and the heal is a burst ({@link RegenGuard}); and because Tensura's
 *       regeneration may use {@code setHealth} directly, which no event sees, each tick the health of the players in the aura is
 *       compared with the last tick's and a rise that no allowed heal explains is reverted. Julius's window lifts the aura (the blessing
 *       is a heal on purpose). The boss's own whitelisted heals are not touched: this class only looks at players and their companions.</li>
 *   <li><b>Magicule Starvation</b>: a percent of the pool a second to everyone in the arena ({@link StarvationMath}: never health, never below the floor).</li>
 *   <li><b>Anti-flight gravity</b>: mayfly and flying cleared every tick for survival players in the arena (creative too, if the config
 *       says so), elytra stopped, a pull to the ground when more than three blocks up; given back on leaving by the evidence rules of
 *       {@link FlightGuard}.</li>
 *   <li><b>Escape teleports</b>: ender pearls and chorus fruit are refused for a player in the arena.</li>
 *   <li><b>Dodges</b>: a hit of Lucius's side that some other system cancelled is a dodge ({@link DodgeEvents}); three in six seconds lay
 *       a Time Stasis Trap. The traps are also checked here, every other tick, against each player.</li>
 *   <li><b>The sabotage effects</b>: a player leaving the arena, or the end of the fight, takes the three effects off; the Djinn suspended
 *       by a severance comes back the moment the effect ends, by whatever means.</li>
 * </ul>
 */
public final class LuciusBossEventHandler {
    private LuciusBossEventHandler() {}

    private static boolean done;

    /** What the handler keeps about one player while a fight is on. */
    private static final class PlayerState {
        final RegenGuard.Tracker regen = new RegenGuard.Tracker();
        final FlightGuard.Record flight = new FlightGuard.Record();
        boolean severed, decayNoted;
        long lastHint;
    }

    private static final Map<UUID, PlayerState> STATE = new HashMap<>();
    private static final Map<UUID, Long> USE_WINDOW = new HashMap<>();

    /** Wire once from the mod constructor. Idempotent. */
    public static void register() {
        if (done) return;
        done = true;
        NeoForge.EVENT_BUS.addListener(LuciusBossEventHandler::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, LuciusBossEventHandler::onHeal);
        NeoForge.EVENT_BUS.addListener(LuciusBossEventHandler::onUseStart);
        NeoForge.EVENT_BUS.addListener(LuciusBossEventHandler::onUseFinish);
        NeoForge.EVENT_BUS.addListener(LuciusBossEventHandler::onUseStop);
        NeoForge.EVENT_BUS.addListener(LuciusBossEventHandler::onProjectile);
        NeoForge.EVENT_BUS.addListener(LuciusBossEventHandler::onTeleport);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, LivingIncomingDamageEvent.class, LuciusBossEventHandler::onIncomingLate);
        NeoForge.EVENT_BUS.addListener(LuciusBossEventHandler::onLogout);
        NeoForge.EVENT_BUS.addListener(LuciusBossEventHandler::onRespawn);
        NeoForge.EVENT_BUS.addListener(LuciusBossEventHandler::onStopping);
        DodgeEvents.install(LuciusGod::anyActive, LuciusBossEventHandler::onDodgeSpam);
        LuciusCapabilities.register();
    }

    // ================================================================ the player tick
    private static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.level().isClientSide) return;
        boolean fight = LuciusGod.anyActive();
        if (!fight && STATE.isEmpty()) return;
        long now = p.level().getGameTime();
        LuciusBoss b = fight ? LuciusCapabilities.arenaBoss(p) : null;
        PlayerState st = STATE.get(p.getUUID());
        if (b == null) {
            if (st != null) release(p, st, now);
            return;
        }
        if (st == null) {
            st = new PlayerState();
            STATE.put(p.getUUID(), st);
        }
        boolean fair = ArcSteps.fair(p);
        // the effects of the sabotage: the Djinn is suspended while severed, and comes back when the effect ends however it ends
        boolean sev = Severance.isSevered(p);
        if (sev != st.severed) {
            st.severed = sev;
            DjinnEngine.severed(p, sev);
        }
        timeDecay(b, p, st, now, fair);
        if (fair && LuciusGodConfig.starvation() && p.tickCount % 10 == 0) {
            double max = EnergyHelper.getMaxMagicule(p);
            LuciusCapabilities.drainMagicule(p, StarvationMath.perTick(max, LuciusGodConfig.starvationPctPerSec()) * 10.0 / Math.max(1.0, max));
        }
        antiFlight(p, st, now);
        if (fair && (p.tickCount & 1) == 0) b.god().checkTraps(p, now);
        if (fair && p.tickCount % 4 == 0 && LuciusCapabilities.skillChannelMassive(p, now)) b.god().requestCheckmate(p);
    }

    /** The player left the arena or the fight ended: everything of ours comes off them. */
    private static void release(ServerPlayer p, PlayerState st, long now) {
        restoreFlight(p, st, now);
        st.regen.reset();
        if (st.severed) {
            st.severed = false;
            LuciusCapabilities.clearSabotage(p);
            DjinnEngine.severed(p, false);
        } else {
            LuciusCapabilities.clearSabotage(p);
        }
        STATE.remove(p.getUUID());
    }

    // ================================================================ Time Decay
    private static boolean inAura(LuciusBoss b, Entity who) {
        double r = LuciusGodConfig.auraRadius();
        return LuciusGodConfig.timeDecay() && b.fightActive() && !b.julius() && who.level() == b.level() && who.distanceToSqr(b) <= r * r;
    }

    private static void timeDecay(LuciusBoss b, ServerPlayer p, PlayerState st, long now, boolean fair) {
        if (!fair || !inAura(b, p)) {
            st.regen.reset();
            if (st.decayNoted) {
                st.decayNoted = false;
                p.removeEffect(LuciusGodEffects.TIME_DECAY);
            }
            return;
        }
        if (!st.decayNoted) {
            st.decayNoted = true;
            p.displayClientMessage(Component.literal("Time Decay: you cannot regenerate near Lucius. Potions and food still work.").withStyle(ChatFormatting.GOLD), true);
        }
        if (p.tickCount % 20 == 0) LuciusGodEffects.mark(p, LuciusGodEffects.TIME_DECAY, 40);
        float fix = st.regen.check(p.getHealth(), p.getMaxHealth(), p.isAlive());
        if (!Float.isNaN(fix) && p.getHealth() > fix) p.setHealth(fix);
    }

    private static boolean windowOpen(UUID id, long now) {
        Long t = USE_WINDOW.get(id);
        return t != null && now <= t;
    }

    private static void onHeal(LivingHealEvent e) {
        if (!LuciusGod.anyActive()) return;
        LivingEntity who = e.getEntity();
        if (who.level().isClientSide || who instanceof LuciusBoss || ZogratisEntities.isFamily(who) || PaladinTurns.isTurned(who)) return;
        boolean player = who instanceof ServerPlayer;
        if (player) {
            if (!ArcSteps.fair((ServerPlayer) who)) return;
        } else if (PaladinTurns.ownerOf(who) == null || SoulPaladins.isPaladin(who)) {
            return;                                                           // only players and the companions of players
        }
        LuciusBoss b = null;
        for (LuciusBoss c : LuciusGod.active()) if (inAura(c, who)) { b = c; break; }
        if (b == null) return;
        long now = who.level().getGameTime();
        if (RegenGuard.allowsHeal(e.getAmount(), windowOpen(who.getUUID(), now))) {
            if (player) {
                PlayerState st = STATE.get(who.getUUID());
                if (st != null) st.regen.credit(e.getAmount());
            }
            return;
        }
        e.setAmount(0f);
        e.setCanceled(true);
        if (player && now - hintAt((ServerPlayer) who) >= 100) {
            PlayerState st = STATE.get(who.getUUID());
            if (st != null) st.lastHint = now;
            ((ServerPlayer) who).displayClientMessage(Component.literal("Time Decay: the heal fades before it lands.").withStyle(ChatFormatting.GOLD), true);
        }
    }

    private static long hintAt(ServerPlayer p) {
        PlayerState st = STATE.get(p.getUUID());
        return st == null ? 0 : st.lastHint;
    }

    private static void onUseStart(LivingEntityUseItemEvent.Start e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !LuciusGod.anyActive()) return;
        UseAnim anim = e.getItem().getUseAnimation();
        if (anim != UseAnim.DRINK && anim != UseAnim.EAT) return;
        USE_WINDOW.put(p.getUUID(), p.level().getGameTime() + Math.max(0, e.getDuration()) + RegenGuard.WINDOW_AFTER);
    }

    private static void onUseFinish(LivingEntityUseItemEvent.Finish e) {
        if (e.getEntity() instanceof ServerPlayer p && USE_WINDOW.containsKey(p.getUUID())) USE_WINDOW.put(p.getUUID(), p.level().getGameTime() + RegenGuard.WINDOW_AFTER);
    }

    private static void onUseStop(LivingEntityUseItemEvent.Stop e) {
        if (e.getEntity() instanceof ServerPlayer p) USE_WINDOW.remove(p.getUUID());
    }

    /** A thrown potion landing opens the window for every player near it, for a few ticks (the splash heals through the same door). */
    private static void onProjectile(ProjectileImpactEvent e) {
        if (!LuciusGod.anyActive() || !(e.getProjectile() instanceof ThrownPotion pot) || pot.level().isClientSide) return;
        long until = pot.level().getGameTime() + RegenGuard.WINDOW_AFTER;
        for (ServerPlayer p : ((ServerLevel) pot.level()).players()) if (p.distanceToSqr(pot) <= 36.0) USE_WINDOW.put(p.getUUID(), until);
    }

    // ================================================================ flight
    private static void antiFlight(ServerPlayer p, PlayerState st, long now) {
        boolean applies = LuciusGodConfig.antiFlight() && !p.isSpectator() && (!p.isCreative() || LuciusGodConfig.affectCreative());
        if (!applies) {
            restoreFlight(p, st, now);
            return;
        }
        Abilities ab = p.getAbilities();
        if (st.flight.tick(ab.mayfly, ab.flying, p.isCreative(), now) == FlightGuard.Action.GROUND) {
            ab.mayfly = false;
            ab.flying = false;
            p.onUpdateAbilities();
        }
        if (p.isFallFlying()) p.stopFallFlying();
        if (p.onGround() || p.isInWater() || p.isInLava() || p.onClimbable() || p.isPassenger()) return;
        double h = heightAboveGround(p);
        double g = FlightGuard.extraGravity(h);
        if (g > 0.0) {
            Vec3 dm = p.getDeltaMovement();
            p.setDeltaMovement(dm.x, dm.y - g, dm.z);
            p.hurtMarked = true;
        }
    }

    private static double heightAboveGround(ServerPlayer p) {
        var pos = p.blockPosition();
        for (int i = 0; i <= 14; i++) if (!p.level().getBlockState(pos.below(i)).isAir()) return p.getY() - pos.below(i).getY() - 1.0;
        return 15.0;
    }

    private static void restoreFlight(ServerPlayer p, PlayerState st, long now) {
        if (!st.flight.entered()) return;
        boolean give = st.flight.restore(now);
        st.flight.reset();
        if (!give || p.isSpectator()) return;
        Abilities ab = p.getAbilities();
        if (!ab.mayfly) {
            ab.mayfly = true;
            p.onUpdateAbilities();
        }
    }

    // ================================================================ teleports, dodges
    private static void onTeleport(EntityTeleportEvent e) {
        if (!LuciusGod.anyActive() || !LuciusGodConfig.blockEscapeTeleports()) return;
        if (!(e instanceof EntityTeleportEvent.EnderPearl) && !(e instanceof EntityTeleportEvent.ChorusFruit)) return;
        if (!(e.getEntity() instanceof ServerPlayer p) || p.isCreative() || p.isSpectator()) return;
        if (LuciusCapabilities.arenaBoss(p) == null) return;
        e.setCanceled(true);
        p.displayClientMessage(Component.literal("Space is folded shut around the arena. You cannot teleport away.").withStyle(ChatFormatting.DARK_PURPLE), true);
    }

    /** A blow of Lucius's side that someone else already cancelled (a dodge, a rewrite of fate, a negation) is a dodge. */
    private static void onIncomingLate(LivingIncomingDamageEvent e) {
        if (!e.isCanceled() || !LuciusGod.anyActive() || !(e.getEntity() instanceof ServerPlayer p) || p.level().isClientSide) return;
        if (LuciusArcHooks.ownCancel == e) return;
        Entity attacker = e.getSource().getEntity();
        if (attacker != null && p.level() instanceof ServerLevel sl && LuciusArcHooks.luciusSide(sl, attacker)) DodgeEvents.record(p);
    }

    /** Three dodges in six seconds: a trap under the dodger. */
    private static void onDodgeSpam(ServerPlayer p) {
        LuciusBoss b = LuciusCapabilities.arenaBoss(p);
        if (b != null) b.god().layTrap(p);
    }

    // ================================================================ housekeeping
    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        PlayerState st = STATE.remove(id);
        if (st != null && e.getEntity() instanceof ServerPlayer p) {
            if (st.severed) DjinnEngine.severed(p, false);
            LuciusCapabilities.clearSabotage(p);
        }
        USE_WINDOW.remove(id);
        LuciusCapabilities.forget(id);
        DodgeEvents.forget(id);
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        PlayerState st = STATE.get(e.getEntity().getUUID());
        if (st != null) {
            st.regen.reset();
            st.flight.reset();
            st.severed = false;
            st.decayNoted = false;
        }
        USE_WINDOW.remove(e.getEntity().getUUID());
    }

    private static void onStopping(ServerStoppingEvent e) {
        STATE.clear();
        USE_WINDOW.clear();
        LuciusGod.forgetAll();
        LuciusCapabilities.clearStatics();
        DodgeEvents.clear();
    }
}
