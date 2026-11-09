package com.newuniverse.nusmp.saiyan;

import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

/**
 * Why: Ki Flight (3.65.0, Stage 2) with the same ownership rule as the wings of the other skills: the server GIVES the flight (creative-style flight with the tier's speed) and TAKES BACK exactly what it gave
 * (a marker on the player's own data), never touching creative or spectator flight or a flight another skill gave. It costs Ki every second spent in the air, a Novice can only hover for a few seconds, the
 * speed rises with the seconds really flown (five tiers, {@link SaiyanFlightRules}), and a Master has a burst dash. The server also CHECKS what the player really did each second ({@link
 * SaiyanFlightRules#speedOk}): a client that flies faster than its tier allows loses the flight. A short landing grace removes the fall damage so ending the flight in the air never kills. Cost: one upkeep
 * a second per flying Saiyan, no entity, a few particles for the burst only.
 */
final class SaiyanFlight {
    private SaiyanFlight() {}

    /** The marker that the flight of this player was given by Ki Flight (so it is Ki Flight's to take back). */
    static final String MARK = "nusmp_saiyan_flight";

    static SaiyanFlightRules.Tier tier(SaiyanState s) { return SaiyanFlightRules.tierOf(s.uses("flight"), SaiyanConfig.flightSeconds()); }

    private static float speedOf(SaiyanState s) { return (float) SaiyanFlightRules.speed(tier(s), SaiyanConfig.flightSpeedScale()); }

    /** The flight key: starts the flight (paying the start cost) or ends it. */
    static boolean toggle(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        if (r.flying) {
            stop(p, r, now);
            return true;
        }
        if (!SaiyanConfig.flightEnabled()) {
            SaiyanAbilities.say(p, "flight_off");
            return false;
        }
        if (!s.awakened) {
            SaiyanAbilities.say(p, "not_awakened");
            return false;
        }
        if (p.isSpectator() || p.isPassenger()) return false;
        if (!SaiyanKi.pay(p, SaiyanConfig.flightStart())) {
            SaiyanAbilities.say(p, "no_ki");
            return false;
        }
        r.flying = true;
        r.groundSecs = 0;
        r.hoverSecs = 0;
        r.flightTier = tier(s).ordinal();
        grant(p, s);
        p.setDeltaMovement(p.getDeltaMovement().add(0.0, 0.35, 0.0));
        p.hurtMarked = true;
        return true;
    }

    /** Gives the flight (only if the player has none of their own) with the speed of the tier. */
    private static void grant(ServerPlayer p, SaiyanState s) {
        var ab = p.getAbilities();
        CompoundTag d = p.getPersistentData();
        if (!ab.mayfly) {
            ab.mayfly = true;
            d.putBoolean(MARK, true);
        }
        if (d.getBoolean(MARK)) {
            float v = speedOf(s);
            if (ab.getFlyingSpeed() != v) ab.setFlyingSpeed(v);
            ab.flying = true;
            p.onUpdateAbilities();
        }
    }

    /** Ends the flight; the next seconds are safe from fall damage. */
    static void stop(ServerPlayer p, SaiyanManager.Run r, long now) {
        r.flying = false;
        r.burstUntil = -1;
        r.safeFallUntil = now + 100;
        r.dirty = true;
        release(p);
    }

    /** Takes back the flight if Ki Flight gave it; returns true when there was something to take back. Safe to call for any player (logout, death, race removal). */
    static boolean release(ServerPlayer p) {
        CompoundTag d = p.getPersistentData();
        if (!d.getBoolean(MARK)) return false;
        d.remove(MARK);
        var ab = p.getAbilities();
        boolean keep = p.isCreative() || p.isSpectator();
        ab.mayfly = keep;
        if (!keep) ab.flying = false;
        ab.setFlyingSpeed(0.05f);
        p.onUpdateAbilities();
        return true;
    }

    /** Once a second while flying: the checks, the Ki, the hover limit and the seconds that earn the next tier. {@code moved} is the ground covered in the last second. */
    static void second(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now, double moved) {
        if (!r.flying) return;
        if (!SaiyanConfig.flightEnabled() || p.isSpectator() || p.isPassenger() || !p.isAlive()) {
            stop(p, r, now);
            return;
        }
        SaiyanFlightRules.Tier t = tier(s);
        if (t.ordinal() != r.flightTier) {
            if (t.ordinal() > r.flightTier) p.displayClientMessage(Component.translatable("nusmp.saiyan.msg.flight_tier", Component.translatable(t.key())), true);
            r.flightTier = t.ordinal();
            grant(p, s);
            r.dirty = true;
        }
        var ab = p.getAbilities();
        if (ab.flying && !SaiyanFlightRules.speedOk(moved, ab.getFlyingSpeed(), now <= r.burstUntil + 20)) {
            SaiyanAbilities.say(p, "flight_fast");
            stop(p, r, now);
            return;
        }
        if (p.onGround()) {
            if (++r.groundSecs >= 3) {
                SaiyanAbilities.say(p, "flight_land");
                stop(p, r, now);
            }
            return;
        }
        r.groundSecs = 0;
        if (!SaiyanKi.pay(p, SaiyanFlightRules.kiPerSecond(t, SaiyanConfig.flightKiScale()))) {
            SaiyanAbilities.say(p, "no_ki");
            stop(p, r, now);
            return;
        }
        if (SaiyanFlightRules.countsAsFlight(moved)) {
            r.hoverSecs = 0;
            s.use("flight", 1);
        } else if (SaiyanFlightRules.hoverLimited(t) && !SaiyanFlightRules.hoverLeft(++r.hoverSecs, SaiyanConfig.hoverSeconds())) {
            SaiyanAbilities.say(p, "hover_over");
            stop(p, r, now);
        }
    }

    /** The burst dash (Master only): a short, very fast push along the look direction, with afterimages. */
    static boolean burst(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        if (!r.flying) return false;
        if (!SaiyanFlightRules.canBurst(tier(s))) {
            SaiyanAbilities.say(p, "burst_locked");
            return false;
        }
        if (!s.ready("burst", now) || !p.getAbilities().flying) return false;
        if (!SaiyanAbilities.pay(p, s, SaiyanConfig.burstStamina(), SaiyanConfig.burstCost())) return false;
        s.startCooldown("burst", now, SaiyanConfig.burstCooldown());
        Vec3 look = p.getLookAngle();
        p.setDeltaMovement(look.scale(SaiyanConfig.burstSpeed()));
        p.hurtMarked = true;
        r.burstUntil = now + 20;
        var level = p.serverLevel();
        Fx.spray(level, FxThemes.WORLD, FxOld.CLOUD, p.getX(), p.getY() + 1.0, p.getZ(), 6, 0.3, 0.5, 0.3, 0.02);
        Fx.spray(level, FxThemes.WORLD, FxOld.ELECTRIC_SPARK, p.getX(), p.getY() + 1.0, p.getZ(), 8, 0.4, 0.6, 0.4, 0.15);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5f, 1.7f);
        return true;
    }

    /** Ending the flight in the air, or falling out of it, never costs health. */
    static void onFall(LivingFallEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        SaiyanManager.Run r = SaiyanManager.peekRun(p);
        if (r != null && (r.flying || p.level().getGameTime() <= r.safeFallUntil)) e.setDamageMultiplier(0f);
    }
}
