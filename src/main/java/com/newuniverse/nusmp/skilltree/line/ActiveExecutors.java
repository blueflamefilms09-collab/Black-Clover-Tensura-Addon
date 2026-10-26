package com.newuniverse.nusmp.skilltree.line;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import com.newuniverse.nusmp.skilltree.kit.KitAllies;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: this is the whole of the 26 arts and finishers (plus the stance) of the effect catalogue, one small method per KIND. A line of 22 nodes needs no Java of its own: its arts say "kind
 * {@code cone_strike}, damage 6, range 3.5" in data and the method below does it, with every number read from the node's tables at the level the player bought. What is common to all of them is
 * not here but in the cast service ({@code LineCast}): the gate (line equipped, node bought), the cooldown, the magicule price, the practice count. What is common to the effects is in
 * {@link ActiveCtx} and {@link LineGeo}. The rules each method keeps, which are the owner's balance rules for tree techniques: damage only through {@code AbilityKit.hit} (the balance law caps it and scales
 * it with the caster's EP), heals only through {@code AbilityKit.heal} (the law's 50 % cap and its fade when healed in a row), a hard control (a pull, a freeze) only after {@code AbilityKit.control}
 * and for the duration the law allows, at most 16 creatures per cast, no block is ever changed, and the only visuals are the mod's effect shapes and the Rukh. A method that needs a target
 * says so in its {@link Check} so the cast is refused BEFORE it costs magicules or starts a cooldown. The registry is filled by the {@code reg} calls below; a test reads this file and the catalogue and fails
 * when a kind has no line here or a line here has no kind.
 */
public final class ActiveExecutors {
    private ActiveExecutors() {}

    /** The effect of an art. */
    @FunctionalInterface
    public interface Exec {
        void run(ActiveCtx c);
    }

    /** Whether an art can be used at all right now (a target to hit); a refused cast costs nothing. */
    @FunctionalInterface
    public interface Check {
        boolean ok(ActiveCtx c);
    }

    /** One registered kind. */
    public record Art(Check canUse, Exec run) {}

    private static final Map<String, Art> KINDS = new LinkedHashMap<>();
    private static final int MAX_TARGETS = 16;

    private static void reg(String kind, Exec run) { KINDS.put(kind, new Art(c -> true, run)); }

    private static void reg(String kind, Check canUse, Exec run) { KINDS.put(kind, new Art(canUse, run)); }

    static {
        reg("cone_strike", ActiveExecutors::coneStrike);
        reg("ring_burst", ActiveExecutors::ringBurst);
        reg("dash_strike", ActiveExecutors::dashStrike);
        reg("leap_slam", ActiveExecutors::leapSlam);
        reg("ray_shot", ActiveExecutors::rayShot);
        reg("chain_hit", c -> chainStart(c) != null, ActiveExecutors::chainHit);
        reg("buff_self", ActiveExecutors::buffSelf);
        reg("buff_aura", ActiveExecutors::buffAura);
        reg("heal_self", ActiveExecutors::healSelf);
        reg("heal_pulse", ActiveExecutors::healPulse);
        reg("blink", c -> blinkTarget(c).distanceToSqr(c.p.position()) > 1.0, ActiveExecutors::blink);
        reg("shield", ActiveExecutors::shield);
        reg("taunt", ActiveExecutors::taunt);
        reg("debuff_cone", ActiveExecutors::debuffCone);
        reg("pull", ActiveExecutors::pull);
        reg("uppercut", c -> meleeTarget(c) != null, ActiveExecutors::uppercut);
        reg("whirlwind", ActiveExecutors::whirlwind);
        reg("barrage", ActiveExecutors::barrage);
        reg("ground_wave", ActiveExecutors::groundWave);
        reg("execute", c -> meleeTarget(c) != null, ActiveExecutors::execute);
        reg("drain_bite", c -> meleeTarget(c) != null, ActiveExecutors::drainBite);
        reg("stun_ring", ActiveExecutors::stunRing);
        reg("smoke", ActiveExecutors::smoke);
        reg("mark", c -> LineGeo.lookTarget(c.p, c.num("range")) != null, ActiveExecutors::mark);
        reg("ward_reflect", ActiveExecutors::wardReflect);
        reg("meteor_drop", ActiveExecutors::meteorDrop);
        reg("stance", ActiveExecutors::stance);
        reg("debt_burst", c -> Debts.canBurst(c), ActiveExecutors::debtBurst);   // 3.26.0 (Evolution skills): consumes the Debt on the foes around and bursts it
    }

    /** The registered kind, or null. */
    public static Art get(String kind) { return kind == null ? null : KINDS.get(kind); }

    /** Every kind that has an executor (the test compares it with the catalogue). */
    public static Set<String> kinds() { return Collections.unmodifiableSet(KINDS.keySet()); }

    // ================================================================== helpers
    private static List<LivingEntity> foesAround(ActiveCtx c, Vec3 centre, double radius, int n) {
        return LineGeo.nearest(AbilityKit.around(c.p, centre, radius), centre, Math.min(n, MAX_TARGETS));
    }

    /** The creature a melee-range art strikes: the one looked at, else the nearest in a narrow cone. */
    private static LivingEntity meleeTarget(ActiveCtx c) {
        double range = c.num("range");
        LivingEntity t = LineGeo.lookTarget(c.p, range);
        if (t != null) return t;
        List<LivingEntity> cone = LineGeo.nearest(AbilityKit.cone(c.p, range, 35), c.p.position(), 1);
        return cone.isEmpty() ? null : cone.get(0);
    }

    private static Vec3 chest(ServerPlayer p) { return p.position().add(0, p.getBbHeight() * 0.6, 0); }

    private static void sound(ActiveCtx c, net.minecraft.sounds.SoundEvent s, float pitch) { AbilityKit.sound(c.p, s, 0.8f, pitch); }

    // ================================================================== strikes
    private static void coneStrike(ActiveCtx c) {
        double range = c.num("range");
        Vec3 from = c.eye(), to = from.add(c.look().scale(range));
        c.fx(from.add(0, -0.3, 0), to, 12, (float) Math.max(0.8, range / 3.0));
        for (LivingEntity t : LineGeo.nearest(AbilityKit.cone(c.p, range, c.num("half_angle")), c.p.position(), Math.min(c.whole("targets"), MAX_TARGETS))) {
            if (c.hit(t, c.num("damage"))) LineGeo.shove(c.p, t, c.num("knockback"));
        }
        c.rukh(to, 5);
        sound(c, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f);
    }

    private static void ringBurst(ActiveCtx c) {
        double radius = c.num("radius");
        Vec3 at = c.p.position().add(0, 0.2, 0);
        c.fx(at, at, 16, (float) radius);
        for (LivingEntity t : foesAround(c, c.p.position(), radius, c.whole("targets"))) {
            if (c.hit(t, c.num("damage"))) LineGeo.shove(c.p, t, c.num("knockback"));
        }
        c.rukh(chest(c.p), 8);
        sound(c, SoundEvents.GENERIC_EXPLODE.value(), 1.4f);
    }

    private static void dashStrike(ActiveCtx c) {
        Vec3 start = c.p.position(), look = c.look();
        Vec3 dir = new Vec3(look.x, look.y * 0.3, look.z);
        dir = dir.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : dir.normalize();
        Vec3 end = LineGeo.freeAlong(c.p, start.add(dir.scale(c.num("distance"))));
        Vec3 a = start.add(0, 1, 0), b = end.add(0, 1, 0);
        c.fx(a, b, 10, 1.0f);
        for (LivingEntity t : LineGeo.nearest(AbilityKit.along(c.p, a, b, 1.0), start, Math.min(c.whole("targets"), MAX_TARGETS))) c.hit(t, c.num("damage"));
        LineGeo.moveTo(c.p, end);
        c.rukh(b, 6);
        sound(c, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2f);
    }

    private static void leapSlam(ActiveCtx c) {
        Vec3 flat = LineGeo.flat(c.look());
        c.p.setDeltaMovement(flat.x * 0.7, c.num("height"), flat.z * 0.7);
        c.p.hurtMarked = true;
        c.p.fallDistance = 0f;
        c.fx(c.p.position(), c.p.position().add(0, 2, 0), 10, 1.0f);
        sound(c, SoundEvents.PLAYER_ATTACK_STRONG, 0.8f);
        ServerPlayer p = c.p;
        LineTimers.waitUntil(p, 6, 2, 40, p::onGround, () -> {            // comes down (or 2 s pass): the slam
            double radius = c.num("radius");
            Vec3 at = p.position().add(0, 0.2, 0);
            c.fx(at, at, 16, (float) radius);
            for (LivingEntity t : foesAround(c, p.position(), radius, MAX_TARGETS)) {
                if (c.hit(t, c.num("damage"))) LineGeo.shove(p, t, c.num("knockback"));
            }
            p.fallDistance = 0f;
            c.rukh(at, 8);
            sound(c, SoundEvents.GENERIC_EXPLODE.value(), 1.2f);
        });
    }

    private static void rayShot(ActiveCtx c) {
        double range = c.num("range");
        Vec3 from = c.eye().add(0, -0.2, 0), end = LineGeo.lookEnd(c.p, range);
        c.fx(from, end, 10, 1.0f);
        for (LivingEntity t : LineGeo.nearest(AbilityKit.along(c.p, from, end, 0.6), from, c.whole("pierce"))) c.hit(t, c.num("damage"));
        c.rukh(end, 5);
        sound(c, SoundEvents.FIREWORK_ROCKET_BLAST, 1.0f);
    }

    private static LivingEntity chainStart(ActiveCtx c) {
        LivingEntity t = LineGeo.lookTarget(c.p, 14);
        if (t != null) return t;
        List<LivingEntity> cone = LineGeo.nearest(AbilityKit.cone(c.p, 12, 35), c.p.position(), 1);
        return cone.isEmpty() ? null : cone.get(0);
    }

    private static void chainHit(ActiveCtx c) {
        LivingEntity cur = chainStart(c);
        if (cur == null) return;
        List<LivingEntity> seen = new ArrayList<>();
        Vec3 prev = chest(c.p);
        double dmg = c.num("damage");
        double keep = 1.0 - c.num("falloff");
        int hits = 1 + Math.max(0, c.whole("jumps"));
        for (int i = 0; i < hits && cur != null; i++) {
            Vec3 at = ActiveCtx.centre(cur);
            c.fx(prev, at, 8, 0.8f);
            c.hit(cur, dmg);
            seen.add(cur);
            dmg *= keep;
            prev = at;
            LivingEntity from = cur;
            cur = null;
            double best = c.num("jump_range");
            for (LivingEntity e : AbilityKit.around(c.p, from.position(), best)) {
                if (seen.contains(e)) continue;
                double d = e.distanceTo(from);
                if (d <= best) {
                    best = d;
                    cur = e;
                }
            }
        }
        c.rukh(prev, 5);
        sound(c, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.8f);
    }

    // ================================================================== support
    private static void buffSelf(ActiveCtx c) {
        int ticks = Math.max(20, c.whole("seconds") * 20);
        LineLookups.give(c.p, c.str("effect", ""), ticks, c.whole("amp"), true);
        LineLookups.give(c.p, c.str("effect2", "none"), ticks, c.whole("amp2"), true);
        c.fx(c.p.position(), c.p.position(), 14, 1.0f);
        c.rukh(chest(c.p), 6);
        sound(c, SoundEvents.BEACON_POWER_SELECT, 1.2f);
    }

    private static void buffAura(ActiveCtx c) {
        double radius = c.num("radius");
        int ticks = Math.max(20, c.whole("seconds") * 20);
        int given = 0, allies = 0;
        for (LivingEntity e : KitAllies.around(c.p, c.p.position(), radius)) {
            if (given++ >= MAX_TARGETS) break;
            LineLookups.give(e, c.str("effect", ""), ticks, c.whole("amp"), true);
            if (e != c.p) allies++;
        }
        Evolution.onAllyBuffs(c.p, c.route, allies);                                    // 3.26.0: the "ally_buffs" evolution counter
        c.fx(c.p.position(), c.p.position(), 18, (float) radius);
        c.rukh(chest(c.p), 8);
        sound(c, SoundEvents.BEACON_ACTIVATE, 1.2f);
    }

    private static void healSelf(ActiveCtx c) {
        AbilityKit.heal(c.p, (float) c.num("amount"));
        int regen = c.whole("regen_seconds");
        if (regen > 0) LineLookups.give(c.p, "regeneration", regen * 20, 0, true);
        c.fx(c.p.position(), c.p.position(), 14, 1.0f);
        c.rukh(chest(c.p), 6);
        sound(c, SoundEvents.PLAYER_LEVELUP, 1.4f);
    }

    private static void healPulse(ActiveCtx c) {
        double radius = c.num("radius");
        int healed = 0, allies = 0;
        for (LivingEntity e : KitAllies.around(c.p, c.p.position(), radius)) {
            if (healed++ >= MAX_TARGETS) break;
            AbilityKit.heal(e, (float) c.num("amount"));
            if (e != c.p) allies++;
        }
        Evolution.onAllyBuffs(c.p, c.route, allies);                                    // 3.26.0: the "ally_buffs" evolution counter
        c.fx(c.p.position(), c.p.position(), 18, (float) radius);
        c.rukh(chest(c.p), 8);
        sound(c, SoundEvents.PLAYER_LEVELUP, 1.0f);
    }

    private static Vec3 blinkTarget(ActiveCtx c) {
        Vec3 look = c.look();
        return LineGeo.freeAlong(c.p, c.p.position().add(look.scale(c.num("distance"))));
    }

    private static void blink(ActiveCtx c) {
        Vec3 from = c.p.position(), to = blinkTarget(c);
        c.fx(from.add(0, 1, 0), to.add(0, 1, 0), 10, 1.0f);
        LineGeo.moveTo(c.p, to);
        c.rukh(to.add(0, 1, 0), 6);
        sound(c, SoundEvents.ENDERMAN_TELEPORT, 1.0f);
    }

    private static void shield(ActiveCtx c) {
        int ticks = Math.max(20, c.whole("seconds") * 20);
        int amp = Math.max(0, (int) Math.round(c.num("absorb_hearts") / 2.0) - 1);          // Absorption I is 4 health (2 hearts), the same sum the Heavy Knight's guard uses
        c.p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, Math.min(amp, 9), true, true, true));
        int resist = c.whole("resist_amp");
        if (resist > 0) c.p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, resist - 1, true, true, true));
        c.fx(c.p.position(), c.p.position(), 16, 1.2f);
        c.rukh(chest(c.p), 6);
        sound(c, SoundEvents.SHIELD_BLOCK, 1.0f);
    }

    private static void taunt(ActiveCtx c) {
        double radius = c.num("radius");
        int n = 0;
        for (LivingEntity e : foesAround(c, c.p.position(), radius, c.whole("targets"))) {
            if (e instanceof Mob m) {
                m.setTarget(c.p);
                n++;
            }
        }
        LineLookups.give(c.p, "resistance", Math.max(20, c.whole("resist_seconds") * 20), 0, true);
        c.fx(c.p.position(), c.p.position(), 16, (float) radius);
        c.rukh(chest(c.p), 8);
        sound(c, SoundEvents.RAVAGER_ROAR, 1.2f);
        if (n == 0) AbilityKit.say(c.p, "Nothing answers the call.");
    }

    private static void debuffCone(ActiveCtx c) {
        double range = c.num("range");
        Vec3 from = c.eye(), to = from.add(c.look().scale(range));
        c.fx(from.add(0, -0.3, 0), to, 14, (float) Math.max(0.8, range / 3.0));
        int ticks = Math.max(20, c.whole("seconds") * 20), ticks2 = c.whole("seconds2") * 20;
        for (LivingEntity t : LineGeo.nearest(AbilityKit.cone(c.p, range, c.num("half_angle")), c.p.position(), MAX_TARGETS)) {
            LineLookups.give(t, c.str("effect", ""), ticks, c.whole("amp"), true);
            if (ticks2 > 0) LineLookups.give(t, c.str("effect2", "none"), ticks2, c.whole("amp"), true);
        }
        c.rukh(to, 5);
        sound(c, SoundEvents.EVOKER_CAST_SPELL, 1.0f);
    }

    private static void pull(ActiveCtx c) {
        double radius = c.num("radius");
        boolean control = AbilityKit.control(c.p);                    // the shared hard-control gate: one pull, stun or root per 12 s
        c.fx(c.p.position(), c.p.position(), 16, (float) radius);
        for (LivingEntity t : foesAround(c, c.p.position(), radius, c.whole("targets"))) {
            if (c.num("damage") > 0) c.hit(t, c.num("damage"));
            if (control && t.isAlive()) {
                Vec3 dir = c.p.position().subtract(t.position());
                if (dir.lengthSqr() > 1e-6) t.setDeltaMovement(dir.normalize().scale(c.num("strength")).add(0, 0.25, 0));
                t.hurtMarked = true;
            }
        }
        if (!control) AbilityKit.say(c.p, "Hard control is still recovering: the pull only hurts.");
        c.rukh(chest(c.p), 8);
        sound(c, SoundEvents.ENDERMAN_SCREAM, 0.6f);
    }

    private static void uppercut(ActiveCtx c) {
        LivingEntity t = meleeTarget(c);
        if (t == null) return;
        c.fx(c.p.position().add(0, 0.5, 0), ActiveCtx.centre(t), 10, 1.0f);
        if (c.hit(t, c.num("damage"))) {
            Vec3 v = t.getDeltaMovement();
            t.setDeltaMovement(v.x * 0.3, c.num("launch"), v.z * 0.3);
            t.hurtMarked = true;
        }
        c.rukh(ActiveCtx.centre(t), 5);
        sound(c, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.8f);
    }

    private static void whirlwind(ActiveCtx c) {
        sound(c, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7f);
        LineTimers.repeat(c.p, c.whole("hits"), c.whole("interval"), i -> {
            double radius = c.num("radius");
            Vec3 at = c.p.position().add(0, 0.5, 0);
            c.fx(at, at, 8, (float) radius);
            for (LivingEntity t : foesAround(c, c.p.position(), radius, MAX_TARGETS)) c.hit(t, c.num("damage"));
            c.rukh(chest(c.p), 4);
            if (i % 2 == 0) sound(c, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f + 0.1f * i);
        });
    }

    private static void barrage(ActiveCtx c) {
        double range = c.num("range");
        LineTimers.repeat(c.p, c.whole("shots"), c.whole("interval"), i -> {
            LivingEntity t = LineGeo.lookTarget(c.p, range);
            Vec3 from = c.eye().add(0, -0.2, 0);
            if (t == null) {
                c.fx(from, LineGeo.lookEnd(c.p, range), 6, 0.6f);
                return;
            }
            Vec3 at = ActiveCtx.centre(t);
            c.fx(from, at, 6, 0.6f);
            c.hit(t, c.num("damage"));
            c.rukh(at, 3);
            sound(c, SoundEvents.ARROW_SHOOT, 1.2f + 0.05f * i);
        });
    }

    private static void groundWave(ActiveCtx c) {
        Vec3 dir = LineGeo.flat(c.look());
        if (dir.equals(Vec3.ZERO)) dir = new Vec3(0, 0, 1);
        double length = c.num("length"), width = c.num("width");
        Vec3 from = c.p.position().add(dir.scale(1.0)).add(0, 0.2, 0), to = c.p.position().add(dir.scale(length)).add(0, 0.2, 0);
        c.fx(from, to, 16, (float) Math.max(1.0, width));
        for (LivingEntity t : LineGeo.nearest(AbilityKit.along(c.p, from, to, width / 2.0), c.p.position(), MAX_TARGETS)) {
            if (c.hit(t, c.num("damage"))) LineGeo.shove(c.p, t, c.num("knockback"));
        }
        c.rukh(to, 6);
        sound(c, SoundEvents.GENERIC_EXPLODE.value(), 1.6f);
    }

    private static void execute(ActiveCtx c) {
        LivingEntity t = meleeTarget(c);
        if (t == null) return;
        double missing = 1.0 - Math.max(0.0, Math.min(1.0, t.getHealth() / Math.max(1f, t.getMaxHealth())));
        double dmg = c.num("damage") * (1.0 + c.num("bonus") * 2.0 * missing);                 // a foe at its last breath takes up to (1 + 2 x bonus) times the blow
        c.fx(c.eye().add(0, -0.3, 0), ActiveCtx.centre(t), 10, 1.0f);
        c.hit(t, dmg);
        c.rukh(ActiveCtx.centre(t), 6);
        sound(c, SoundEvents.PLAYER_ATTACK_CRIT, 0.9f);
    }

    private static void drainBite(ActiveCtx c) {
        LivingEntity t = meleeTarget(c);
        if (t == null) return;
        float before = t.getHealth();
        c.fx(c.eye().add(0, -0.3, 0), ActiveCtx.centre(t), 10, 1.0f);
        c.hit(t, c.num("damage"));
        float dealt = Math.max(0f, before - Math.max(0f, t.getHealth()));
        if (dealt > 0f) AbilityKit.heal(c.p, (float) (dealt * c.num("heal_pct")));
        c.rukh(ActiveCtx.centre(t), 6);
        sound(c, SoundEvents.PHANTOM_BITE, 1.0f);
    }

    private static void stunRing(ActiveCtx c) {
        double radius = c.num("radius");
        c.fx(c.p.position(), c.p.position(), 16, (float) radius);
        if (!AbilityKit.control(c.p)) {
            AbilityKit.say(c.p, "Hard control is still recovering.");
            return;
        }
        int ticks = Math.max(20, c.whole("seconds") * 20);
        for (LivingEntity t : foesAround(c, c.p.position(), radius, c.whole("targets"))) {
            int held = BalanceLaw.controlTicks(t, ticks);                // the law caps the hold (3 s a player, 5 s a creature, 1 s a boss) and fades repeats
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, held, 6, false, true, true));
            if (t instanceof Mob m) m.setTarget(null);
        }
        c.rukh(chest(c.p), 8);
        sound(c, SoundEvents.ANVIL_LAND, 0.6f);
    }

    private static void smoke(ActiveCtx c) {
        double radius = c.num("radius");
        int ticks = Math.max(20, Math.min(c.whole("seconds"), 12) * 20);
        for (LivingEntity t : foesAround(c, c.p.position(), radius, MAX_TARGETS)) LineLookups.give(t, "blindness", ticks, 0, true);
        LineLookups.give(c.p, "speed", ticks, 0, true);
        c.fx(c.p.position(), c.p.position(), 20, (float) radius);
        c.rukh(chest(c.p), 8);
        sound(c, SoundEvents.FIRECHARGE_USE, 0.8f);
    }

    private static void mark(ActiveCtx c) {
        LivingEntity t = LineGeo.lookTarget(c.p, c.num("range"));
        if (t == null) return;
        long now = c.world().getGameTime();
        int ticks = Math.max(20, c.whole("seconds") * 20);
        LineState.putLong(t, LineEvents.MARK_UNTIL, now + ticks);
        LineState.putDouble(t, LineEvents.MARK_BONUS, c.num("bonus"));
        LineLookups.give(t, "glowing", ticks, 0, false);
        c.fx(ActiveCtx.centre(t), ActiveCtx.centre(t), 12, 1.0f);
        c.rukh(ActiveCtx.centre(t), 5);
        sound(c, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6f);
    }

    private static void wardReflect(ActiveCtx c) {
        long now = c.world().getGameTime();
        LineState.putLong(c.p, LineEvents.WARD_UNTIL, now + Math.max(20, c.whole("seconds") * 20));
        LineState.putDouble(c.p, LineEvents.WARD_PCT, c.num("reflect_pct"));
        c.fx(c.p.position(), c.p.position(), 16, 1.2f);
        c.rukh(chest(c.p), 6);
        sound(c, SoundEvents.BEACON_POWER_SELECT, 0.7f);
    }

    private static void meteorDrop(ActiveCtx c) {
        double range = c.num("range"), radius = c.num("radius");
        LivingEntity look = LineGeo.lookTarget(c.p, range);
        Vec3 point = look != null ? look.position() : LineGeo.lookEnd(c.p, range);
        c.fx(point.add(0, 0.1, 0), point.add(0, 0.1, 0), Math.max(10, c.whole("delay")), (float) radius);       // the warning ring
        sound(c, SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f);
        LineTimers.later(c.p, c.whole("delay"), () -> {
            c.world().playSound(null, point.x, point.y, point.z, SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.PLAYERS, 1.5f, 0.8f);
            c.fx(point.add(0, 12, 0), point, 14, (float) radius);
            for (LivingEntity t : foesAround(c, point, radius, MAX_TARGETS)) c.hit(t, c.num("damage"));
            c.rukh(point.add(0, 0.5, 0), 10);
        });
    }

    /** 3.26.0 (Evolution skills): the art that takes the Debt the player's blows stored on the foes around and bursts it; the work and its bounds are in {@link Debts#burst}. */
    private static void debtBurst(ActiveCtx c) { Debts.burst(c); }

    private static void stance(ActiveCtx c) {
        boolean on = LineStances.toggle(c);
        c.fx(c.p.position(), c.p.position(), on ? 16 : 8, on ? 1.2f : 0.8f);
        c.rukh(chest(c.p), on ? 8 : 3);
        sound(c, on ? SoundEvents.ARMOR_EQUIP_IRON.value() : SoundEvents.ARMOR_EQUIP_LEATHER.value(), on ? 0.9f : 1.2f);
        c.p.displayClientMessage(LineText.tr(on ? LineText.STANCE_UP : LineText.STANCE_DOWN, c.node.name()), true);
    }
}
