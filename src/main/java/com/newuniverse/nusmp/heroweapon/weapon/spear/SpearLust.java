package com.newuniverse.nusmp.heroweapon.weapon.spear;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.heroweapon.curse.HeroCurse;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Foes;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Hits;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Moves;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Rays;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Tasks;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: the Spear's curse is Lust (canon: the Lust Series, whose weapon draws those around it towards the wielder; Motoyasu falls into it after he is
 * abandoned by his companions). The owner asked for a curse that draws the wielder to the nearest foe, makes blows grow and takes restraint away. What
 * the curse ENGINE does to the wielder's body (the pull of the wielder's own steps, the corruption, the overrun) is its builder's; this class is what the
 * WEAPON adds, and every part of it is bounded: the blows grow by a few stacks for a few seconds, the aim snaps to prey (see {@link SpearAim}), and the four
 * Lust techniques either draw foes in (Temptation, the canon skill), run one down and drink from it (Rapture), cage a place in chains and thorns, or draw
 * everything in and crush it (Lust's Embrace, which adds corruption on top of the curse's own). Hard control goes through the shared control lock.
 */
final class SpearLust {
    private SpearLust() {}

    private static final ResourceLocation FERVOR_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "spear_fervor");

    // ================================================================ the blows grow
    /** A melee blow landed: one more stack of fervor, kept for a few seconds. */
    static void onHit(HeroCtx c) {
        if (!c.form.has(SpearIds.LUST_FERVOR)) return;
        long now = c.level.getGameTime();
        int stacks = Math.min(SpearNumbers.FERVOR_MAX, SpearState.fervor(c, now) + 1);
        SpearState.setFervor(c, stacks, now + SpearNumbers.FERVOR_TICKS);
        apply(c.player, stacks);
    }

    /** Every few ticks: let the stacks run out. */
    static void tick(HeroCtx c) {
        if (!c.form.has(SpearIds.LUST_FERVOR)) return;
        apply(c.player, SpearState.fervor(c, c.level.getGameTime()));
    }

    /** Forget the stacks and take the bonus off (form changed, weapon put away). */
    static void clear(HeroCtx c) {
        SpearState.clearFervor(c);
        apply(c.player, 0);
    }

    /** Makes the wielder's attack damage carry exactly {@code stacks} stacks: a transient bonus, never saved, replaced only when the number changes. */
    private static void apply(ServerPlayer p, int stacks) {
        AttributeInstance attack = p.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack == null) return;
        if (stacks <= 0) {
            if (attack.hasModifier(FERVOR_ID)) attack.removeModifier(FERVOR_ID);
            return;
        }
        double amount = stacks * SpearNumbers.FERVOR_PER_STACK;
        AttributeModifier now = attack.getModifier(FERVOR_ID);
        if (now != null && Math.abs(now.amount() - amount) < 1.0E-9) return;
        attack.addOrUpdateTransientModifier(new AttributeModifier(FERVOR_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void corrupt(ServerPlayer p, int amount) {
        try {
            HeroCurse.addCorruption(p, amount);
        } catch (RuntimeException ignored) {
            // the curse engine must never stop a technique
        }
    }

    /** Draws the foes in the list one step closer to {@code o} at {@code speed}; a boss is never moved. Returns how many are within {@code arrive} of it. */
    private static int pull(List<LivingEntity> foes, Vec3 o, double speed, double arrive) {
        int near = 0;
        for (LivingEntity e : foes) {
            if (!e.isAlive() || BalanceLaw.isBoss(e)) continue;
            Vec3 d = o.subtract(e.position()).multiply(1.0, 0.0, 1.0);
            double dist = d.length();
            if (dist <= arrive) {
                near++;
                continue;
            }
            Vec3 v = d.normalize().scale(speed);
            e.setDeltaMovement(v.x, e.getDeltaMovement().y, v.z);
            e.hurtMarked = true;
        }
        return near;
    }

    // ================================================================ Temptation
    static boolean temptation(HeroCtx c) {
        ServerPlayer p = c.player;
        ServerLevel level = c.level;
        Vec3 centre = p.position();
        List<LivingEntity> foes = Foes.around(c, centre, SpearNumbers.TEMPT_RADIUS, SpearNumbers.TEMPT_TARGETS);
        if (foes.isEmpty()) {
            c.say("No one to lure.");
            return false;
        }
        if (!c.control()) {
            c.say("Your allure needs a moment to gather again.");
            return false;
        }
        Tells.ring(level, centre, SpearNumbers.TEMPT_RADIUS, SpearFx.rgb(c));
        VfxSpawn.sendFollowing(level, VfxShape.MAGIC_CIRCLE, p, centre.add(0.0, 0.1, 0.0), SpearFx.LUST, SpearNumbers.TEMPT_DELAY + SpearNumbers.TEMPT_STEPS * 2, 2.2f);
        SpearFx.swing(c);
        SpearFx.sound(c, centre, SoundEvents.EVOKER_PREPARE_ATTACK, 1.2f, 1.5f);
        for (LivingEntity e : foes) if (BalanceLaw.isBoss(e)) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
        Tasks.pulses(c, SpearNumbers.TEMPT_DELAY, SpearNumbers.TEMPT_STEPS, 2, s -> {
            Vec3 o = p.position();
            pull(foes, o, SpearNumbers.TEMPT_PULL, SpearNumbers.TEMPT_ARRIVE);
            if (s % 3 == 0) for (LivingEntity e : foes) if (e.isAlive()) c.fx(VfxShape.THREAD_LINE, SpearFx.mid(e), o.add(0.0, 1.0, 0.0), SpearFx.LUST, 8, 0.6f);
            if (s == SpearNumbers.TEMPT_STEPS - 1) {
                Hits hits = new Hits();
                for (LivingEntity e : foes) if (e.isAlive() && e.position().distanceTo(o) <= SpearNumbers.TEMPT_ARRIVE + 1.0) hits.add(e, (float) SpearNumbers.TEMPT_RAW);
                for (LivingEntity e : hits.apply(c, SpearNumbers.TEMPT_TARGETS)) SpearFx.flare(c, SpearFx.mid(e), 0.6f);
                SpearFx.sound(c, o, SoundEvents.ELDER_GUARDIAN_CURSE, 0.6f, 1.8f);
            }
        });
        return true;
    }

    // ================================================================ Rapture
    /** The place beside the prey (on the wielder's side of it) where the wielder can stand, or null when there is none. */
    private static Vec3 beside(HeroCtx c, LivingEntity prey) {
        ServerPlayer p = c.player;
        ServerLevel level = c.level;
        Vec3 pm = prey.position();
        Vec3 toPrey = pm.subtract(p.position()).multiply(1.0, 0.0, 1.0);
        Vec3 flat = toPrey.lengthSqr() < 1.0E-6 ? Moves.flat(c.look(), new Vec3(0.0, 0.0, 1.0)) : toPrey.normalize();
        for (double gap = 1.4; gap <= 3.4; gap += 0.8) {
            Vec3 g = pm.subtract(flat.scale(gap + prey.getBbWidth() * 0.5));
            int y = Moves.surfaceBelow(level, g.x, Math.max(p.getY(), pm.y) + 1.0, g.z, 5);
            if (y != Integer.MIN_VALUE && Moves.standable(level, p, (int) Math.floor(g.x), y, (int) Math.floor(g.z))) return Moves.feet(level, g.x, y, g.z);
        }
        return null;
    }

    static boolean rapture(HeroCtx c) {
        ServerPlayer p = c.player;
        if (p.isPassenger()) {                                              // a mount would drag the wielder straight back
            c.say("Get down first.");
            return false;
        }
        Vec3 eye = c.eye(), look = c.look();
        LivingEntity prey = Foes.nearestInCone(c, eye, look, SpearNumbers.RAPTURE_RANGE, 80.0);
        if (prey == null) {
            c.say("Nothing to devour.");
            return false;
        }
        if (beside(c, prey) == null) {
            c.say("There is no footing beside it.");
            return false;
        }
        Vec3 hand = SpearFx.hand(c, Rays.dir(eye, SpearFx.mid(prey), look));
        c.fx(VfxShape.THREAD_LINE, hand, SpearFx.mid(prey), SpearFx.LUST, SpearNumbers.RAPTURE_DELAY + 2, 0.9f);       // the glint: the point is fixed on its prey
        SpearFx.flare(c, SpearFx.mid(prey), 0.7f);
        SpearFx.swing(c);
        SpearFx.sound(c, p.position(), SoundEvents.PLAYER_ATTACK_CRIT, 0.8f, 0.5f);
        Tasks.later(c, SpearNumbers.RAPTURE_DELAY, () -> {
            if (!prey.isAlive() || p.isPassenger() || p.distanceTo(prey) > SpearNumbers.RAPTURE_RANGE + 3.0) return;       // it got away in the moment the glint lasted
            Vec3 dest = beside(c, prey);
            if (dest == null) return;
            c.fx(VfxShape.LIGHT_BLADE, p.position().add(0.0, 1.0, 0.0), SpearFx.mid(prey), SpearFx.LUST, 8, 1.0f);
            if (!Moves.teleport(p, dest)) return;
            if (Foes.hit(c, prey, (float) SpearNumbers.RAPTURE_RAW)) {
                c.heal(p, (float) (p.getMaxHealth() * SpearNumbers.RAPTURE_HEAL));
                SpearFx.flare(c, SpearFx.mid(prey), 1.0f);
                c.fx(VfxShape.THREAD_LINE, SpearFx.mid(prey), p.position().add(0.0, 1.0, 0.0), SpearFx.LUST, 10, 0.8f);
            }
            corrupt(p, SpearNumbers.RAPTURE_CORRUPTION);
            SpearFx.sound(c, dest, SoundEvents.PLAYER_ATTACK_CRIT, 1.2f, 0.7f);
        });
        return true;
    }

    // ================================================================ Thorn Chains
    static boolean chains(HeroCtx c) {
        ServerLevel level = c.level;
        Vec3 aim = c.aim(SpearNumbers.CHAINS_RANGE);
        int y = Moves.surfaceBelow(level, aim.x, aim.y + 0.5, aim.z, 6);
        Vec3 centre = new Vec3(aim.x, y == Integer.MIN_VALUE ? aim.y : y, aim.z);
        int lasts = SpearNumbers.CHAINS_DELAY + (SpearNumbers.CHAINS_PULSES - 1) * SpearNumbers.CHAINS_INTERVAL + 10;
        Tells.ring(level, centre, SpearNumbers.CHAINS_RADIUS, SpearFx.rgb(c));
        c.fx(VfxShape.CHAIN_FX2, centre, centre, SpearFx.LUST, lasts, (float) SpearNumbers.CHAINS_RADIUS);
        SpearFx.swing(c);
        SpearFx.sound(c, centre, SoundEvents.CHAIN_PLACE, 1.4f, 0.7f);
        Tasks.pulses(c, SpearNumbers.CHAINS_DELAY, SpearNumbers.CHAINS_PULSES, SpearNumbers.CHAINS_INTERVAL, s -> {
            Hits hits = new Hits();
            for (LivingEntity e : Foes.around(c, centre, SpearNumbers.CHAINS_RADIUS, Foes.MAX)) hits.add(e, (float) SpearNumbers.CHAINS_RAW_PULSE);
            for (LivingEntity e : hits.apply(c, Foes.MAX)) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 1));
                SpearFx.flare(c, SpearFx.mid(e), 0.4f);
            }
            SpearFx.sound(c, centre, SoundEvents.CHAIN_HIT, 0.8f, 0.9f);
        });
        return true;
    }

    // ================================================================ Lust's Embrace
    static boolean embrace(HeroCtx c) {
        ServerPlayer p = c.player;
        ServerLevel level = c.level;
        Vec3 centre = p.position();
        List<LivingEntity> foes = Foes.around(c, centre, SpearNumbers.EMBRACE_RADIUS, Foes.MAX);
        if (foes.isEmpty()) {
            c.say("No one to embrace.");
            return false;
        }
        if (!c.control()) {
            c.say("Your allure needs a moment to gather again.");
            return false;
        }
        Tells.ring(level, centre, SpearNumbers.EMBRACE_RADIUS, SpearFx.rgb(c));
        VfxSpawn.sendFollowing(level, VfxShape.MAGIC_CIRCLE, p, centre.add(0.0, 0.1, 0.0), SpearFx.LUST, SpearNumbers.EMBRACE_DELAY + SpearNumbers.EMBRACE_PULL_STEPS * 2 + 10, 3.0f);
        SpearFx.swing(c);
        SpearFx.sound(c, centre, SoundEvents.WARDEN_SONIC_CHARGE, 1.4f, 0.6f);
        int pullEnds = SpearNumbers.EMBRACE_DELAY + SpearNumbers.EMBRACE_PULL_STEPS * 2;
        Tasks.pulses(c, SpearNumbers.EMBRACE_DELAY, SpearNumbers.EMBRACE_PULL_STEPS, 2, s -> {
            Vec3 o = p.position();
            pull(foes, o, 0.55, SpearNumbers.EMBRACE_BURST_RADIUS * 0.5);
            if (s % 3 == 0) for (LivingEntity e : foes) if (e.isAlive()) c.fx(VfxShape.THREAD_LINE, SpearFx.mid(e), o.add(0.0, 1.0, 0.0), SpearFx.LUST, 8, 0.7f);
        });
        Tasks.later(c, pullEnds + 2, () -> {
            Vec3 o = p.position();
            Hits hits = new Hits();
            for (LivingEntity e : Foes.around(c, o, SpearNumbers.EMBRACE_BURST_RADIUS, Foes.MAX)) hits.add(e, (float) SpearNumbers.EMBRACE_RAW);
            c.fx(VfxShape.MAGIC_CIRCLE_EXPLOSION, o.add(0.0, 0.1, 0.0), o.add(0.0, 1.0, 0.0), SpearFx.LUST, 30, 2.6f);
            for (LivingEntity e : hits.apply(c, Foes.MAX)) {
                SpearFx.flare(c, SpearFx.mid(e), 1.0f);
                c.shove(e, o, 1.1);
            }
            SpearFx.flare(c, o.add(0.0, 1.0, 0.0), 2.0f);
            SpearFx.sound(c, o, SoundEvents.GENERIC_EXPLODE.value(), 1.0f, 1.3f);
        });
        corrupt(p, SpearNumbers.EMBRACE_CORRUPTION);
        return true;
    }
}
