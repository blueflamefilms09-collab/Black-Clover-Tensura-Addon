package com.newuniverse.nusmp.entity.zogratis;

import com.newuniverse.nusmp.boss.god.LuciusGodConfig;
import com.newuniverse.nusmp.boss.god.StarvationMath;
import com.newuniverse.nusmp.boss.god.TrueDamage;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Why: {@link ArcSteps} says what the 1.42.0 chain steps do in the world; the god tier adds six steps (a soul lance from range, the
 * relic pulse, the severance binding, the starvation pulse, Vanica's blood tide as a chain step, and the counter strike), and putting
 * them next to the old ones would bury the old file. Same shape as there: one method that draws the telegraph for exactly the ticks the
 * chain gives, one that does the effect. Every harmful step goes through {@link LuciusBoss#strike} (so the true-damage percent, the
 * balance law and the shared pressure cap all apply) and every effect on a player respects the same fairness rules as the old steps
 * (never a creative or spectator player, never without the telegraph, anti-magic is the designed counter to what binds).
 */
final class GodSteps {
    private GodSteps() {}

    static final int ICE = 0x7FD7FF, INDIGO = 0x6A2C9C, BLOOD = 0xB0121E;
    static final double EMP_RADIUS = 14.0, STARVE_RADIUS = 18.0, COUNTER_RING = 2.8;
    static final float RAY_BASE = 16f;

    private static Vec3 hand(LivingEntity b) { return b.position().add(0, b.getBbHeight() * 0.75, 0); }

    // ================================================================ the soul lance (stage 1: the Soul Touch from range)
    static void soulRayTell(LuciusBoss b, ServerLevel sl, LivingEntity t, int ticks) {
        ArcSteps.faceTowards(b, t);
        ZogratisVfx.ray(sl, hand(b), t.getBoundingBox().getCenter(), 0.7, ticks + 2, ArcSteps.SOUL, true);
        ZogratisVfx.soulVortex(sl, t.position(), 1.6, ticks + 2, ArcSteps.SOUL);
    }

    /** A lance of soul at the target: hurts like a lesser touch, and puts Soul Corruption level one on a target that has none. */
    static boolean soulRayFire(LuciusBoss b, LivingEntity t) {
        if (!(b.level() instanceof ServerLevel sl) || !ArcSteps.fair(t)) return false;
        if (!b.hasLineOfSight(t)) {
            b.announce("The soul lance finds only a wall.");
            return false;
        }
        ArcSteps.faceTowards(b, t);
        b.swing(InteractionHand.MAIN_HAND);
        boolean landed = b.strike(t, RAY_BASE, true);
        ZogratisVfx.ray(sl, hand(b), t.getBoundingBox().getCenter(), 1.0, 10, ArcSteps.SOUL, false);
        ZogratisVfx.soulVortex(sl, t.position(), 1.8, 24, ArcSteps.SOUL);
        LuciusFx.sound(sl, t.position(), SoundEvents.BEACON_DEACTIVATE, 1.2f, 0.9f);
        if (landed && !t.hasEffect(LuciusEffects.SOUL_CORRUPTION)) LuciusEffects.corrupt(t);
        return landed;
    }

    // ================================================================ the relic pulse (stage 3: space)
    static void empTell(LuciusBoss b, ServerLevel sl, int ticks) {
        ZogratisVfx.divineSigil(sl, b.position(), EMP_RADIUS, ticks + 2, ICE, true);
        ZogratisVfx.gate(sl, b.position().add(0, 1.2, 0), b.getViewVector(1f), 1.5, ticks + 2, ICE);
        b.announce("Spatial pulse: every relic inside the pale ring goes silent for " + LuciusGodConfig.empTicks() / 20 + " seconds. Leave the ring, or hold anti-magic.");
    }

    /** The pulse: every player inside the ring has every relic de-attuned for the configured time. Returns how many. */
    static int empFire(LuciusBoss b, ServerLevel sl) {
        ZogratisVfx.spaceShatter(sl, b.position().add(0, 1, 0), EMP_RADIUS * 0.5, ICE);
        ZogratisVfx.divineSigil(sl, b.position(), EMP_RADIUS, 24, ICE, false);
        LuciusFx.sound(sl, b.position(), SoundEvents.AMETHYST_BLOCK_BREAK, 2.5f, 0.5f);
        int n = 0;
        for (LivingEntity f : b.foesAround(b.position(), EMP_RADIUS)) {
            if (!(f instanceof ServerPlayer p) || !ArcSteps.fair(p)) continue;
            if (LuciusFx.antiMagicHeld(p)) {
                p.displayClientMessage(Component.literal("The pulse breaks against your anti-magic.").withStyle(ChatFormatting.GRAY), true);
                continue;
            }
            LuciusCapabilities.nullifyRelics(p, LuciusGodConfig.empTicks());
            n++;
        }
        return n;
    }

    // ================================================================ the severance binding (a single player, line of sight)
    static void severTell(LuciusBoss b, ServerLevel sl, LivingEntity t, int ticks) {
        ArcSteps.faceTowards(b, t);
        ZogratisVfx.ray(sl, hand(b), t.getBoundingBox().getCenter(), 0.9, ticks + 2, ArcSteps.SOUL, true);
        ZogratisVfx.soulVortex(sl, t.position(), 2.0, ticks + 2, ArcSteps.SOUL);
        ZogratisVfx.divineSigil(sl, t.position(), 1.8, ticks + 2, ArcSteps.SOUL, true);
        if (t instanceof ServerPlayer p) p.displayClientMessage(Component.literal("Lucius reaches for your bond. Break his line of sight, or hold anti-magic.")
                .withStyle(ChatFormatting.DARK_PURPLE), true);
    }

    /** The binding lands only if the target is still in his line of sight and holds no anti-magic. */
    static boolean severFire(LuciusBoss b, LivingEntity t) {
        if (!(b.level() instanceof ServerLevel sl) || !(t instanceof ServerPlayer p) || !ArcSteps.fair(p)) return false;
        if (!b.hasLineOfSight(p) || LuciusFx.antiMagicHeld(p)) {
            p.displayClientMessage(Component.literal("The binding slides off you.").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ZogratisVfx.ray(sl, hand(b), p.getBoundingBox().getCenter(), 1.1, 12, ArcSteps.SOUL, false);
        ZogratisVfx.spaceShatter(sl, p.position().add(0, 1, 0), 1.8, ArcSteps.SOUL);
        LuciusFx.sound(sl, p.position(), SoundEvents.BEACON_DEACTIVATE, 1.6f, 0.5f);
        LuciusCapabilities.sever(p, LuciusGodConfig.severanceTicks());
        return true;
    }

    // ================================================================ the starvation pulse
    static void starveTell(LuciusBoss b, ServerLevel sl, int ticks) {
        ZogratisVfx.divineSigil(sl, b.position(), STARVE_RADIUS, ticks + 2, INDIGO, true);
        ZogratisVfx.soulVortex(sl, b.position(), 4.0, ticks + 2, INDIGO);
        b.announce("Magicule Starvation: the violet field pulses. Everyone inside it loses " + (int) Math.round(LuciusGodConfig.starvationPulsePct()) + "% of their magicule.");
    }

    /** Drains the pulse's share of the magicule of every player inside the field (the pool only, never health). Returns how many. */
    static int starveFire(LuciusBoss b, ServerLevel sl) {
        ZogratisVfx.divineSigil(sl, b.position(), STARVE_RADIUS, 24, INDIGO, false);
        LuciusFx.sound(sl, b.position(), SoundEvents.BEACON_DEACTIVATE, 2.0f, 0.4f);
        int n = 0;
        double pct = LuciusGodConfig.starvationPulsePct();
        for (LivingEntity f : b.foesAround(b.position(), STARVE_RADIUS)) {
            if (!(f instanceof ServerPlayer p) || !ArcSteps.fair(p)) continue;
            double max = io.github.manasmods.tensura.util.EnergyHelper.getMaxMagicule(p);
            if (LuciusCapabilities.drainMagicule(p, StarvationMath.pulse(max, pct) / Math.max(1.0, max))) n++;
        }
        return n;
    }

    // ================================================================ blood tide (stage 2: blood), as a chain step
    static void bloodTell(LuciusBoss b, ServerLevel sl, LivingEntity t, int ticks) {
        ArcSteps.faceTowards(b, t);
        ZogratisVfx.bloodLance(sl, hand(b), t.getBoundingBox().getCenter(), 0.9, ticks + 2, BLOOD, true);
        ZogratisVfx.bloodSigil(sl, t.position(), 2.4, ticks + 2, BLOOD, true);
    }

    static void bloodFire(LuciusBoss b, LivingEntity t) {
        if (t == null || !ArcSteps.fair(t)) return;
        LuciusMoves.bloodTide(b, t);
    }

    // ================================================================ the counter strike (the Checkmate counter and the reply to a clamped hit)
    static void counterTell(LuciusBoss b, ServerLevel sl, Vec3 mark, LivingEntity t, int ticks) {
        ArcSteps.faceTowards(b, t);
        ZogratisVfx.divineSigil(sl, mark, COUNTER_RING, ticks + 2, FxMath.divine(100), true);
        ZogratisVfx.ray(sl, hand(b), mark.add(0, 1, 0), 0.6, ticks + 2, FxMath.divine(100), true);
        ZogratisVfx.pillar(sl, mark, 1.0, ticks + 2, FxMath.divine(100), true);
    }

    /**
     * The strike: he steps beside the mark and strikes whoever is still inside the shown ring, with true damage that no shield stops (a
     * raised shield is lowered first), and breaks the channel of whoever is hit. A target that left the ring is missed, and the chat says so.
     */
    static boolean counterFire(LuciusBoss b, Vec3 mark, LivingEntity t, TrueDamage.Tier tier) {
        if (!(b.level() instanceof ServerLevel sl) || t == null || !ArcSteps.fair(t) || mark == null) return false;
        double dx = t.getX() - mark.x, dz = t.getZ() - mark.z;
        boolean inside = dx * dx + dz * dz <= COUNTER_RING * COUNTER_RING && Math.abs(t.getY() - mark.y) <= 3.5;
        if (!inside) {
            LuciusFx.sound(sl, b.position(), SoundEvents.AMETHYST_BLOCK_BREAK, 1.0f, 0.6f);
            b.announce("Lucius's strike meets only air.");
            return false;
        }
        if (b.distanceTo(t) > 2.2) {
            Vec3 dir = LuciusFx.flatDir(t.position(), b.position(), b.getViewVector(1f));
            Vec3 spot = LuciusFx.standable(sl, b, t.position().add(dir.scale(1.6)));
            if (spot != null) b.blinkTo(spot);
        }
        ArcSteps.faceTowards(b, t);
        b.swing(InteractionHand.MAIN_HAND);
        if (t instanceof Player p && p.isBlocking()) p.stopUsingItem();
        boolean landed = b.god().landTrue(t, tier);
        ZogratisVfx.spaceShatter(sl, t.position().add(0, 1, 0), 2.2, FxMath.divine(100));
        LuciusFx.sound(sl, t.position(), SoundEvents.AMETHYST_BLOCK_BREAK, 1.8f, 0.5f);
        if (landed && t instanceof ServerPlayer p) {
            LuciusCapabilities.interruptCast(p, 30);
            p.displayClientMessage(Component.literal("Checkmate. Your channel is broken.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
        }
        return landed;
    }
}
