package com.newuniverse.nusmp.stones;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: the two Mind Stone acts that Phase A left over. Telekinesis lifts the one mob in sight for three seconds (it hangs before the bearer's eyes) and hurls it where the bearer looks when the time is
 * up; never a player, never a boss (so no forced-movement rule can be broken), one lift per bearer, a server-wide cap, the control through the balance law. Mind Link shares the bearer's good effects
 * (short, small, renewed every two seconds, never chained) with up to four allied players or pets within twenty-four blocks for thirty seconds. Neither takes a will: no mind control anywhere. The
 * per-tick work is in {@link CarrierEvents} (at most the capped number of lifts and links). Server side.
 */
final class MindActs {
    private MindActs() {}

    // ================================================================ telekinesis
    static void telekinesis(ActCtx c) {
        ServerPlayer p = c.player();
        if (!StoneConfig.telekinesis()) {
            Fx.say(p, "Telekinesis is switched off on this server.");
            return;
        }
        if (CarrierState.lift(p.getUUID()) != null) {
            Fx.say(p, "You already hold something in the air.");
            return;
        }
        LivingEntity t = StoneActs.aim(p, CarrierRules.LIFT_REACH);
        if (t == null || !CarrierRules.liftable(t instanceof Player, SnapActs.isBoss(t), t.isAlive())) {
            Fx.say(p, t == null ? "There is nothing in sight to lift." : "That cannot be lifted.");
            return;
        }
        if (!BalanceLaw.beginControl(p)) {
            Fx.say(p, "The mind cannot hold again so soon.");
            return;
        }
        ServerLevel l = p.serverLevel();
        CarrierState.Lift lift = new CarrierState.Lift(t.getUUID(), l.dimension().location().toString(), l.getGameTime() + CarrierRules.LIFT_TICKS);
        if (!CarrierState.openLift(p.getUUID(), lift, StoneConfig.maxLifts())) {
            Fx.say(p, "Too many things hang in the air on this server; wait a moment.");
            return;
        }
        StoneActs.flash(p, InfinityStone.MIND, t.position(), 2.0);
        Fx.say(p, t.getName().getString() + " rises. It is hurled where you look in three seconds.");
    }

    /** The throw: where the bearer looks now; one hit through the balance law. */
    static void hurl(ServerPlayer p, LivingEntity t) {
        Vec3 look = p.getLookAngle();
        t.setDeltaMovement(look.scale(CarrierRules.THROW_SPEED).add(0, 0.2, 0));
        t.hurtMarked = true;
        t.fallDistance = 0;
        StoneActs.hit(p, t, (float) (CarrierRules.THROW_DAMAGE * StoneSlot.scale(p)));
        StoneActs.flash(p, InfinityStone.MIND, t.position(), 2.0);
        if (StoneConfig.telekinesisSweep()) sweep(p, t, look);
    }

    /** 4.104.0: what the missile crosses. Mobs within 1.6 blocks of the 14-block line ahead of the thrown one take 60% of the throw (at most six, never a player, a boss or an ally), the end of the line bursts. One box query. */
    private static void sweep(ServerPlayer p, LivingEntity thrown, Vec3 look) {
        Vec3 from = thrown.position().add(0.0, thrown.getBbHeight() * 0.5, 0.0);
        Vec3 to = from.add(look.scale(SWEEP_LENGTH));
        AABB box = new AABB(from, to).inflate(SWEEP_WIDTH);
        int hits = 0;
        for (LivingEntity m : p.serverLevel().getEntitiesOfClass(LivingEntity.class, box, e -> e != thrown && e != p && e.isAlive() && !(e instanceof Player) && !SnapActs.isBoss(e) && !e.isAlliedTo(p))) {
            if (hits >= SWEEP_MAX) break;
            Vec3 c = m.position().add(0.0, m.getBbHeight() * 0.5, 0.0);
            Vec3 d = to.subtract(from);
            double along = Math.max(0.0, Math.min(1.0, c.subtract(from).dot(d) / d.lengthSqr()));
            if (c.distanceTo(from.add(d.scale(along))) > SWEEP_WIDTH) continue;
            StoneActs.hit(p, m, (float) (CarrierRules.THROW_DAMAGE * SWEEP_SHARE * StoneSlot.scale(p)));
            m.setDeltaMovement(look.scale(0.9).add(0.0, 0.25, 0.0));
            m.hurtMarked = true;
            hits++;
        }
        StoneActs.flash(p, InfinityStone.MIND, to, 2.5);
    }

    private static final double SWEEP_LENGTH = 14.0, SWEEP_WIDTH = 1.6, SWEEP_SHARE = 0.6;
    private static final int SWEEP_MAX = 6;

    /** Once a tick for each open lift (at most the cap): keep the thing before the bearer's eyes, throw it at the end, drop it if anything is wrong. */
    static void tickLift(MinecraftServer server, UUID owner) {
        CarrierState.Lift l = CarrierState.lift(owner);
        if (l == null) return;
        ServerPlayer p = server.getPlayerList().getPlayer(owner);
        if (p == null || !p.isAlive() || !p.serverLevel().dimension().location().toString().equals(l.dim())) {
            CarrierState.closeLift(owner);
            return;
        }
        Entity e = p.serverLevel().getEntity(l.target());
        if (!(e instanceof LivingEntity t) || !t.isAlive()) {
            CarrierState.closeLift(owner);
            return;
        }
        if (p.level().getGameTime() >= l.until()) {
            CarrierState.closeLift(owner);
            hurl(p, t);
            return;
        }
        Vec3 hold = p.getEyePosition().add(p.getLookAngle().scale(CarrierRules.LIFT_HOLD_DISTANCE));
        Vec3 d = hold.subtract(t.position().add(0, t.getBbHeight() * 0.5, 0));
        t.setDeltaMovement(d.scale(0.35));
        t.hurtMarked = true;
        t.fallDistance = 0;
    }

    // ================================================================ Mind Link
    static void mindLink(ActCtx c) {
        ServerPlayer p = c.player();
        if (!StoneConfig.mindLink()) {
            Fx.say(p, "Mind Link is switched off on this server.");
            return;
        }
        List<LivingEntity> near = CarrierActs.allies(p, CarrierRules.LINK_RADIUS, CarrierRules.LINK_ALLIES + 1);
        near.remove(p);
        if (near.isEmpty()) {
            Fx.say(p, "No ally is near enough to link with.");
            return;
        }
        List<UUID> ids = new ArrayList<>(near.size());
        StringBuilder names = new StringBuilder();
        for (LivingEntity a : near) {
            ids.add(a.getUUID());
            if (names.length() > 0) names.append(", ");
            names.append(a.getName().getString()).append(" ").append(Math.round(a.getHealth())).append("/").append(Math.round(a.getMaxHealth()));
        }
        long now = p.level().getGameTime();
        CarrierState.Link link = new CarrierState.Link(ids, p.serverLevel().dimension().location().toString(), now + CarrierRules.LINK_TICKS, now + CarrierRules.LINK_PULSE);
        if (!CarrierState.openLink(p.getUUID(), link, StoneConfig.maxLinks())) {
            Fx.say(p, "Too many minds are linked on this server; wait a moment.");
            return;
        }
        StoneActs.flash(p, InfinityStone.MIND, p.position(), 5.0);
        Fx.say(p, "Linked for thirty seconds: " + names + ". Your good effects are shared with them.");
    }

    /** Every two seconds for each open link (at most the cap): the bearer's good effects, short and small, go to the allies who are still near; the link ends with its time, its bearer or a new dimension. */
    static void tickLink(MinecraftServer server, UUID owner) {
        CarrierState.Link l = CarrierState.link(owner);
        if (l == null) return;
        ServerPlayer p = server.getPlayerList().getPlayer(owner);
        long now = p == null ? 0 : p.level().getGameTime();
        if (p == null || !p.isAlive() || now >= l.until() || !p.serverLevel().dimension().location().toString().equals(l.dim())) {
            CarrierState.closeLink(owner);
            return;
        }
        if (now < l.nextPulse()) return;
        CarrierState.replaceLink(owner, new CarrierState.Link(l.allies(), l.dim(), l.until(), now + CarrierRules.LINK_PULSE));
        List<MobEffectInstance> good = new ArrayList<>(CarrierRules.LINK_EFFECTS_PER_PULSE);
        for (MobEffectInstance i : p.getActiveEffects()) {
            if (good.size() >= CarrierRules.LINK_EFFECTS_PER_PULSE) break;
            if (i.getEffect().value().getCategory() != MobEffectCategory.BENEFICIAL || i.getDuration() <= CarrierRules.LINK_EFFECT_TICKS) continue;   // a copy of a link is short: never re-shared
            good.add(i);
        }
        if (good.isEmpty()) return;
        double r2 = CarrierRules.LINK_SHARE_RADIUS * CarrierRules.LINK_SHARE_RADIUS;
        for (UUID id : l.allies()) {
            Entity e = p.serverLevel().getEntity(id);
            if (!(e instanceof LivingEntity ally) || !ally.isAlive() || ally.distanceToSqr(p) > r2) continue;
            for (MobEffectInstance i : good) {
                int amp = CarrierRules.linkAmplifier(i.getAmplifier());
                MobEffectInstance have = ally.getEffect(i.getEffect());
                if (have != null && have.getAmplifier() >= amp && have.getDuration() > CarrierRules.LINK_PULSE) continue;
                ally.addEffect(new MobEffectInstance(i.getEffect(), CarrierRules.linkTicks(i.getDuration()), amp, true, false, true));
            }
        }
    }
}
