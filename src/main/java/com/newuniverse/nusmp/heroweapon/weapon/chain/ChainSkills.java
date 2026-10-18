package com.newuniverse.nusmp.heroweapon.weapon.chain;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.heroweapon.curse.HeroCurse;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Foes;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Geo;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Moves;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.PlayerState;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Safe;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: the bodies of the Vassal Chain's techniques, kept apart from {@link ChainLogic}. The weapon is the war chain of S'yne's world (the Shield Hero lore: chains that
 * wrap and bind, a slam that sends many chains racing along the ground, a binding of four groups of chains called the Multi-Headed Orochi, a chain that shields the
 * people it wraps). It is a melee weapon with reach, so its first technique is a lash along the look direction and its passive is the weight of its links. The Sloth
 * techniques belong to the Curse Series that the design gives this weapon (canon ties Sloth to the Hunting Tool, not to a chain): slowness and delay, for the foes AND for
 * the wielder, and a bill that comes due. It shares the chain VFX of the grimoire's Chain Magic ({@code book.ChainArts}) but none of its mechanics: no cage, no drain of
 * magicules, no grapple to a block (the design changes no block); the pulls here move creatures, and the wielder only ever to a creature. Damage goes through
 * {@code HeroCtx.hit}, hard control behind {@code HeroCtx.control}, every delayed step through {@link Safe}.
 */
final class ChainSkills {
    private ChainSkills() {}

    /** The heavy links of each wielder's blows; dropped on logout and when the server stops. */
    static final PlayerState<WeightLedger> WEIGHT = new PlayerState<>(WeightLedger::new);

    private static final int STEEL = 0xFFC0C8E0, SLATE = 0xFF6A7B8C;
    private static final int STEEL_RGB = 0xC0C8E0, SLATE_RGB = 0x6A7B8C;

    private static Vec3 hand(HeroCtx c) {
        return c.eye().add(c.look().scale(0.5)).add(0, -0.35, 0);
    }

    private static Vec3 flatLook(HeroCtx c) {
        Vec3 look = c.look(), flat = new Vec3(look.x, 0, look.z);
        return flat.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : flat.normalize();
    }

    private static String controlMessage(ServerPlayer p) {
        return "Your control magic needs " + (BalanceLaw.controlCooldownLeft(p) + 19) / 20 + "s.";
    }

    // ================================================================== Chain Lash
    /** A lash of the chain along your look direction: hurts what it crosses and tugs the far ones towards you. */
    static boolean chainLash(HeroCtx c) {
        ServerPlayer p = c.player;
        Vec3 eye = c.eye();
        Vec3 end = Foes.wallEnd(p, eye, eye.add(c.look().scale(ChainNumbers.LASH_RANGE)));
        List<LivingEntity> foes = Foes.along(p, eye, end, ChainNumbers.LASH_WIDTH, ChainNumbers.LASH.targets());
        c.fx(VfxShape.CHAIN_FX1, hand(c), foes.isEmpty() ? end : foes.get(foes.size() - 1).getBoundingBox().getCenter(), STEEL, 8, 0.9f);
        c.sound(SoundEvents.CHAIN_HIT, 1.0f, 0.9f);
        if (foes.isEmpty()) {
            c.say("The chain finds no one.");
            return false;
        }
        for (LivingEntity t : foes) {
            c.hit(t, (float) ChainNumbers.LASH.raw());
            if (t.distanceToSqr(p) > ChainNumbers.LASH_TUG_FROM * ChainNumbers.LASH_TUG_FROM) {
                Vec3 toward = p.position().subtract(t.position()).normalize().scale(ChainNumbers.LASH_TUG);
                t.setDeltaMovement(t.getDeltaMovement().add(toward.x, 0.1, toward.z));
                t.hurtMarked = true;
            }
            if (c.form.has("heavy_links")) hangLink(c, t);
        }
        return true;
    }

    // ================================================================== Chain Pull
    /** Hook the first foe in sight and yank it to you (a boss barely moves). Behind the shared control lock. */
    static boolean chainPull(HeroCtx c) {
        ServerPlayer p = c.player;
        LivingEntity t = Foes.target(p, ChainNumbers.PULL_RANGE);
        if (t == null) {
            c.say("There is no one to hook.");
            return false;
        }
        if (!c.control()) {
            c.say(controlMessage(p));
            return false;
        }
        double weight = ChainNumbers.pullWeight(BalanceLaw.isBoss(t));
        UUID id = t.getUUID();
        boolean[] arrived = {false};
        c.fx(VfxShape.CHAIN_FX1, hand(c), t.getBoundingBox().getCenter(), STEEL, 10, 1.1f);
        c.sound(SoundEvents.CHAIN_PLACE, 1.1f, 0.8f);
        Safe.zone(c, ChainNumbers.PULL_TICKS, 1, "chain.pull", false, (f, age) -> {
            LivingEntity target = Foes.byId(f.level, id);
            if (target == null || !target.isAlive()) return false;
            if (!Moves.pullStep(target, f.player.position(), ChainNumbers.PULL_SPEED, ChainNumbers.PULL_STOP, weight)) return true;
            if (!arrived[0]) {
                arrived[0] = true;
                f.hit(target, (float) ChainNumbers.PULL.raw());
            }
            return false;
        });
        return true;
    }

    // ================================================================== Chain Binding
    /** Chains coil round one foe and hold it fast. Behind the shared control lock. */
    static boolean chainBinding(HeroCtx c) {
        ServerPlayer p = c.player;
        LivingEntity t = Foes.target(p, ChainNumbers.BINDING_RANGE);
        if (t == null) {
            c.say("There is no one to bind.");
            return false;
        }
        if (!c.control()) {
            c.say(controlMessage(p));
            return false;
        }
        int ticks = BalanceLaw.controlTicks(t, ChainNumbers.BINDING_TICKS);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9, false, true));
        t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
        t.hurtMarked = true;
        c.hit(t, (float) ChainNumbers.BINDING.raw());
        c.fx(VfxShape.CHAIN_FX1, hand(c), t.getBoundingBox().getCenter(), STEEL, 10, 1.0f);
        c.fx(VfxShape.CHAIN_FX2, t.position(), t.position().add(0, 1, 0), STEEL, ticks, (float) Math.max(1.4, t.getBbWidth() + 1.0));
        c.sound(SoundEvents.CHAIN_PLACE, 1.2f, 0.6f);
        return true;
    }

    // ================================================================== Hydra
    /** The chain is slammed into the ground and many chains race out along the floor in a fan; a foe in any of them is struck once. A short, visible gathering comes first. */
    static boolean hydra(HeroCtx c) {
        ServerPlayer p = c.player;
        Vec3 flat = flatLook(c);
        Vec3 origin = p.position().add(0, 0.9, 0);
        double[][] dirs = Geo.fan(flat.x, flat.z, ChainNumbers.HYDRA_LINES, ChainNumbers.HYDRA_SPREAD);
        Vec3 slam = p.position().add(flat.scale(2.0));
        Tells.ring(c.level, slam, 1.2, STEEL_RGB);
        for (double[] d : dirs) Tells.line(c.level, origin, Foes.wallEnd(p, origin, origin.add(d[0] * ChainNumbers.HYDRA_LENGTH, 0, d[1] * ChainNumbers.HYDRA_LENGTH)), STEEL_RGB);
        c.sound(SoundEvents.CHAIN_FALL, 1.2f, 0.7f);
        Safe.later(c, ChainNumbers.HYDRA_TELL, "chain.hydra", f -> {
            Vec3 o = f.player.position().add(0, 0.9, 0);
            List<LivingEntity> struck = new ArrayList<>();
            for (double[] d : dirs) {
                Vec3 end = Foes.wallEnd(f.player, o, o.add(d[0] * ChainNumbers.HYDRA_LENGTH, 0, d[1] * ChainNumbers.HYDRA_LENGTH));
                f.fx(VfxShape.CHAIN_FX1, o, end, STEEL, 10, 1.0f);
                for (LivingEntity t : Foes.along(f.player, o, end, ChainNumbers.HYDRA_WIDTH, ChainNumbers.HYDRA.targets())) if (!struck.contains(t) && struck.size() < ChainNumbers.HYDRA.targets()) struck.add(t);
            }
            f.fx(VfxShape.CHAIN_FX3, slam, slam.add(0, 1, 0), STEEL, 22, 1.2f);
            f.sound(SoundEvents.ANVIL_LAND, 0.9f, 1.2f);
            for (LivingEntity t : struck) {
                f.hit(t, (float) ChainNumbers.HYDRA.raw());
                t.setDeltaMovement(t.getDeltaMovement().add(0, ChainNumbers.HYDRA_POP, 0));
                t.hurtMarked = true;
            }
        });
        return true;
    }

    // ================================================================== Chain Defence
    /** Chains wrap the people beside you (and you): Absorption and a little less damage for six seconds. */
    static boolean chainDefence(HeroCtx c) {
        ServerPlayer p = c.player;
        List<LivingEntity> group = Foes.alliesAround(p, p.position(), ChainNumbers.DEFENCE_RADIUS, ChainNumbers.DEFENCE.targets());
        if (!group.contains(p)) group.add(0, p);
        while (group.size() > ChainNumbers.DEFENCE.targets()) group.remove(group.size() - 1);
        for (LivingEntity e : group) {
            e.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ChainNumbers.DEFENCE_TICKS, ChainNumbers.absorptionAmp(e.getMaxHealth()), false, true));
            e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ChainNumbers.DEFENCE_TICKS, 0, false, true));
        }
        VfxSpawn.sendFollowing(c.level, VfxShape.CHAIN_FX2, p, p.position(), STEEL, ChainNumbers.DEFENCE_TICKS, (float) ChainNumbers.DEFENCE_RADIUS);
        c.sound(SoundEvents.CHAIN_PLACE, 1.2f, 1.1f);
        return true;
    }

    // ================================================================== Grapple
    /** Hook the first foe in sight and swing yourself to it, striking it where you land. */
    static boolean grapple(HeroCtx c) {
        ServerPlayer p = c.player;
        LivingEntity t = Foes.target(p, ChainNumbers.GRAPPLE_RANGE);
        if (t == null) {
            c.say("There is no one to swing to.");
            return false;
        }
        UUID id = t.getUUID();
        boolean[] arrived = {false};
        c.fx(VfxShape.CHAIN_FX1, hand(c), t.getBoundingBox().getCenter(), STEEL, 10, 1.0f);
        c.sound(SoundEvents.CHAIN_PLACE, 1.1f, 1.2f);
        Safe.zone(c, ChainNumbers.PULL_TICKS, 1, "chain.grapple", false, (f, age) -> {
            LivingEntity target = Foes.byId(f.level, id);
            if (target == null || !target.isAlive()) return false;
            if (!Moves.pullStep(f.player, target.position(), ChainNumbers.GRAPPLE_SPEED, ChainNumbers.GRAPPLE_STOP, 1.0)) return true;
            if (!arrived[0]) {
                arrived[0] = true;
                f.hit(target, (float) ChainNumbers.GRAPPLE.raw());
                f.sound(SoundEvents.CHAIN_HIT, 1.2f, 0.7f);
            }
            return false;
        });
        return true;
    }

    // ================================================================== Orochi
    /** The ultimate (canon: Binding, Multi-Headed Orochi): four groups of chains fall on the ground you aim at, bind up to four foes in it and squeeze them twice. Shared control lock. */
    static boolean orochi(HeroCtx c) {
        ServerPlayer p = c.player;
        Vec3 centre = Foes.groundBelow(p, c.aim(ChainNumbers.OROCHI_RANGE), 8.0);
        if (Foes.around(p, centre, ChainNumbers.OROCHI_RADIUS, 1).isEmpty()) {
            c.say("No one stands where the chains would fall.");
            return false;
        }
        if (!c.control()) {
            c.say(controlMessage(p));
            return false;
        }
        Tells.ring(c.level, centre, ChainNumbers.OROCHI_RADIUS, STEEL_RGB);
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 2 * k + Math.PI / 4;
            c.fx(VfxShape.CHAIN_FX1, centre.add(Math.cos(a) * 4.0, 7.0, Math.sin(a) * 4.0), centre.add(0, 0.5, 0), STEEL, ChainNumbers.OROCHI_TELL, 1.4f);
        }
        c.sound(SoundEvents.CHAIN_FALL, 1.4f, 0.6f);
        Safe.later(c, ChainNumbers.OROCHI_TELL, "chain.orochi", f -> {
            List<UUID> bound = new ArrayList<>();
            for (LivingEntity t : Foes.around(f.player, centre, ChainNumbers.OROCHI_RADIUS, ChainNumbers.OROCHI.targets())) {
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(t, ChainNumbers.OROCHI_BIND_TICKS), 9, false, true));
                f.hit(t, (float) ChainNumbers.OROCHI.raw());
                f.fx(VfxShape.CHAIN_FX3, t.position(), t.position().add(0, 1, 0), STEEL, 24, 1.2f);
                bound.add(t.getUUID());
            }
            f.sound(SoundEvents.ANVIL_LAND, 1.2f, 0.7f);
            Safe.later(f, ChainNumbers.OROCHI_SQUEEZE, "chain.orochi.squeeze", g -> {
                for (UUID id : bound) {
                    LivingEntity t = Foes.byId(g.level, id);
                    if (t == null || !t.isAlive()) continue;
                    g.hit(t, (float) ChainNumbers.OROCHI.raw());
                    g.fx(VfxShape.CHAIN_FX3, t.position(), t.position().add(0, 1, 0), STEEL, 20, 1.0f);
                }
                g.sound(SoundEvents.CHAIN_BREAK, 1.2f, 0.7f);
            });
        });
        return true;
    }

    // ================================================================== the heavy links (passive)
    /** Every melee blow hangs a link on the creature; the third makes the weight take hold, and it is slowed. */
    static void onHit(HeroCtx c, LivingEntity target) {
        if (c.form.has("heavy_links")) hangLink(c, target);
    }

    private static void hangLink(HeroCtx c, LivingEntity t) {
        if (WEIGHT.of(c.player).add(t.getUUID(), c.level.getGameTime()) < WeightLedger.MAX_STACKS) return;
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ChainNumbers.WEIGHT_SLOW_TICKS, 0, false, true));
        c.fx(VfxShape.CHAIN_FX3, t.position(), t.position().add(0, 1, 0), SLATE, 14, 0.6f);
        c.sound(SoundEvents.CHAIN_HIT, 0.9f, 0.5f);
    }

    // ================================================================== Sloth: Weight
    /** Sloth: a heavy drowsiness spreads from the chain. Foes in range are slowed and weakened (shared control lock); you are slowed a little too. */
    static boolean slothWeight(HeroCtx c) {
        ServerPlayer p = c.player;
        List<LivingEntity> foes = Foes.around(p, p.position(), ChainNumbers.SLOTH_RADIUS, ChainNumbers.SLOTH_WEIGHT.targets());
        if (foes.isEmpty()) {
            c.say("No one is near enough to grow heavy.");
            return false;
        }
        if (!c.control()) {
            c.say(controlMessage(p));
            return false;
        }
        for (LivingEntity t : foes) {
            int ticks = BalanceLaw.controlTicks(t, ChainNumbers.SLOTH_TICKS);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 2, false, true));
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0, false, true));
        }
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ChainNumbers.SLOTH_TICKS, 0, false, true));
        c.fx(VfxShape.CHAIN_FX2, p.position(), p.position().add(0, 1, 0), SLATE, ChainNumbers.SLOTH_TICKS, (float) ChainNumbers.SLOTH_RADIUS);
        c.sound(SoundEvents.CHAIN_FALL, 1.2f, 0.5f);
        HeroCurse.addCorruption(p, ChainNumbers.SLOTH_WEIGHT_CORRUPTION);
        return true;
    }

    // ================================================================== Sloth: Snap
    /** Sloth: delay. A chain wraps one foe now (and slows it, behind the control lock); a second after it, the chain snaps tight for one heavy blow if the foe is still within reach. */
    static boolean slothSnap(HeroCtx c) {
        ServerPlayer p = c.player;
        LivingEntity t = Foes.target(p, ChainNumbers.SNAP_RANGE);
        if (t == null) {
            c.say("There is no one to wrap.");
            return false;
        }
        if (c.control()) {
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(t, ChainNumbers.SNAP_DELAY), 3, false, true));
        }
        UUID id = t.getUUID();
        c.fx(VfxShape.CHAIN_FX1, hand(c), t.getBoundingBox().getCenter(), SLATE, 12, 1.2f);
        Tells.ring(c.level, t.position(), 1.6, SLATE_RGB);
        c.sound(SoundEvents.CHAIN_PLACE, 1.2f, 0.5f);
        Safe.later(c, ChainNumbers.SNAP_DELAY, "chain.snap", f -> {
            LivingEntity target = Foes.byId(f.level, id);
            if (target == null || !target.isAlive() || target.distanceToSqr(f.player) > ChainNumbers.SNAP_KEEP * ChainNumbers.SNAP_KEEP) return;
            f.hit(target, (float) ChainNumbers.SLOTH_SNAP.raw());
            f.fx(VfxShape.CHAIN_FX3, target.position(), target.position().add(0, 1, 0), SLATE, 26, 1.4f);
            f.sound(SoundEvents.ANVIL_LAND, 1.0f, 0.6f);
        });
        HeroCurse.addCorruption(p, ChainNumbers.SNAP_CORRUPTION);
        return true;
    }

    // ================================================================== Sloth: the bill
    /**
     * The Sloth ultimate: for six seconds a field of heavy chains lies round you. Foes in it are slowed and tired (shared control lock, taken once at the cast) and you are
     * slowed too; at the end the whole bill of what was put off comes due on every foe within eleven blocks, in one great blow and a stagger (the same control use).
     */
    static boolean debtComesDue(HeroCtx c) {
        ServerPlayer p = c.player;
        List<LivingEntity> foes = Foes.around(p, p.position(), ChainNumbers.DEBT_RADIUS, ChainNumbers.DEBT.targets());
        if (foes.isEmpty()) {
            c.say("No one is near enough to run up a bill.");
            return false;
        }
        if (!c.control()) {
            c.say(controlMessage(p));
            return false;
        }
        for (LivingEntity t : foes) {
            int ticks = BalanceLaw.controlTicks(t, ChainNumbers.DEBT_TICKS);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 3, false, true));
            t.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ticks, 1, false, true));
        }
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ChainNumbers.DEBT_TICKS, 1, false, true));
        Vec3 here = p.position();
        c.fx(VfxShape.CHAIN_FX2, here, here.add(0, 1, 0), SLATE, ChainNumbers.DEBT_TICKS, (float) ChainNumbers.DEBT_RADIUS);
        Tells.ring(c.level, here, ChainNumbers.DEBT_BILL_RADIUS, SLATE_RGB);
        c.sound(SoundEvents.BEACON_DEACTIVATE, 1.4f, 0.5f);
        Safe.later(c, ChainNumbers.DEBT_TICKS, "chain.debt", f -> {
            Vec3 at = f.player.position();
            List<LivingEntity> owing = Foes.around(f.player, at, ChainNumbers.DEBT_BILL_RADIUS, ChainNumbers.DEBT.targets());
            for (LivingEntity t : owing) {
                f.hit(t, (float) ChainNumbers.DEBT.raw());
                // the stagger belongs to the control use taken at the cast: the lock lasts 12 s, so asking for it again here (6 s later) could never succeed
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(t, ChainNumbers.DEBT_STAGGER), 9, false, true));
                f.fx(VfxShape.CHAIN_FX3, t.position(), t.position().add(0, 1, 0), SLATE, 26, 1.4f);
            }
            f.fx(VfxShape.CHAIN_FX3, at, at.add(0, 1, 0), SLATE, 30, 2.0f);
            f.sound(SoundEvents.ANVIL_LAND, 1.3f, 0.5f);
        });
        HeroCurse.addCorruption(p, ChainNumbers.DEBT_CORRUPTION);
        return true;
    }
}
