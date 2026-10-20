package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import com.newuniverse.nusmp.particle.fx.FxRelic;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Why: the staff's rules (charge pool, holds that obey the boss control caps, the backlash with its madness table) are one mechanism; none of it touches
 * the Time Magic effects, the staff only uses Rukh particles.
 * <p>
 * Wave 30: the staff Paradox. Twenty charges that refill with 2d8+4 at dawn; spells that spend them (Slow, Phase Lock, Phase Stasis, Temporal
 * Movement, Time Stop, Alternate Reality, Turn the Clock, Missing Time); blows with a +3 and a 2d4 extra and a critical on 19 and 20 (see the damage
 * handler); and the Paradox itself: the two strongest spells may force the staff, and a failed save (DC 17) casts the wielder out of the timeline,
 * back to where they stood about ten seconds ago, tired and with the knowledge of all time in their head (the card's madness table, in effects).
 */
final class Wave30Paradox {
    private Wave30Paradox() {}

    // ================================================================ charges
    private static com.newuniverse.nusmp.relics.wave.Wave6Pool.State pool(ServerPlayer p, ItemStack s) {
        return Wave6Kit.pool(s, Wave30Tune.POOL_PARADOX, Wave30Tune.PARADOX_MAX_CHARGES, Wave6Kit.today(p.serverLevel()),
                () -> Wave30Rules.paradoxRegain(p.getRandom().nextInt(8) + 1, p.getRandom().nextInt(8) + 1));
    }

    static int charges(ServerPlayer p, ItemStack s) { return pool(p, s).charges(); }

    /** Pays {@code cost} charges; says so and returns false if the staff has fewer. */
    private static boolean spend(ActCtx c, int cost) {
        ServerPlayer p = c.player();
        Wave6Pool.State st = pool(p, c.stack());
        if (!Wave6Pool.canSpend(st, cost)) {
            Wave30Kit.say(p, "Paradox has " + st.charges() + " charges; " + c.def().cardName() + " needs " + cost + ". It regains 2d8+4 at dawn.");
            return false;
        }
        Wave6Pool.State after = Wave6Pool.spend(st, cost);
        Wave6Kit.put(c.stack(), Wave30Tune.POOL_PARADOX, after);
        Wave30Kit.hint(p, "Paradox: " + after.charges() + " / " + Wave30Tune.PARADOX_MAX_CHARGES + " charges");
        return true;
    }

    // ================================================================ the spells
    /** Slow: foes within ten blocks wade through treacle. */
    static void slow(ActCtx c) {
        ServerPlayer p = c.player();
        if (!spend(c, Wave30Tune.PARADOX_SLOW)) return;
        int n = 0;
        for (LivingEntity t : Fx.foesAround(p, 10)) {
            Fx.addEffect(t, "minecraft:slowness", t instanceof Player ? 80 : Wave30Tune.PARADOX_SLOW_TICKS, 2);
            Fx.addEffect(t, "minecraft:weakness", t instanceof Player ? 80 : Wave30Tune.PARADOX_SLOW_TICKS, 0);
            n++;
        }
        FxRelic.ring(p.serverLevel(), p.position(), 10, 0xB070FF);
        Wave30Kit.say(p, n == 0 ? "Time thickens round you, but no foe is near." : "Time thickens round " + n + (n == 1 ? " foe." : " foes."));
    }

    private static void hold(ServerPlayer p, LivingEntity t, boolean personOnly) {
        int ticks = BalanceLaw.controlTicks(t, Wave30Tune.PARADOX_HOLD_TICKS);
        Fx.addEffect(t, "minecraft:slowness", ticks, 6);
        Fx.addEffect(t, "minecraft:weakness", ticks, 1);
        Fx.addEffect(t, "minecraft:glowing", ticks, 0);
        com.newuniverse.nusmp.particle.fx.Fx.gather(p.serverLevel(), FxThemes.RELIC_WAVE_30, t, FxTone.LIGHT, 12);
        Wave30Kit.say(p, t.getName().getString() + " is caught between moments for " + (ticks / 20) + " seconds.");
    }

    /** Phase Lock (Hold Monster): a creature in sight is held in a still moment, for as long as the control caps allow (a boss only a second). */
    static void phaseLock(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity t = Wave30Kit.target(p, Wave30Tune.PARADOX_REACH);
        if (t == null || (t instanceof Player o && !Wave30Kit.harmable(p, o))) {
            Wave30Kit.say(p, "Phase Lock needs a creature in your sight.");
            return;
        }
        if (!spend(c, Wave30Tune.PARADOX_LOCK)) return;
        hold(p, t, false);
    }

    private static boolean person(LivingEntity e) { return e instanceof Player || e instanceof Npc || e instanceof Raider || e instanceof Piglin || e instanceof Witch; }

    /** Phase Stasis (Hold Person): the same for a person (a player, villager, illager, piglin, witch), for fewer charges. */
    static void phaseStasis(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity t = Wave30Kit.target(p, Wave30Tune.PARADOX_REACH);
        if (t == null || !person(t) || (t instanceof Player o && !Wave30Kit.harmable(p, o))) {
            Wave30Kit.say(p, "Phase Stasis needs a person in your sight.");
            return;
        }
        if (!spend(c, Wave30Tune.PARADOX_STASIS)) return;
        hold(p, t, true);
    }

    /** Temporal Movement (Haste): you move and work faster for fifteen seconds. */
    static void temporalMovement(ActCtx c) {
        ServerPlayer p = c.player();
        if (!spend(c, Wave30Tune.PARADOX_HASTE)) return;
        Fx.addEffect(p, "minecraft:speed", Wave30Tune.PARADOX_HASTE_TICKS, 1);
        Fx.addEffect(p, "minecraft:haste", Wave30Tune.PARADOX_HASTE_TICKS, 1);
        com.newuniverse.nusmp.particle.fx.Fx.aura(p.serverLevel(), FxThemes.RELIC_WAVE_30, p, FxTone.LIGHT);
        Wave30Kit.say(p, "Time runs quick for you.");
    }

    /** Time Stop: the world holds its breath. Every foe within sixteen blocks is frozen (for as long as the control caps allow), you move at speed. May force the staff. */
    static void timeStop(ActCtx c) {
        ServerPlayer p = c.player();
        if (!spend(c, Wave30Tune.PARADOX_STOP)) return;
        int n = 0;
        for (LivingEntity t : Fx.foesAround(p, 16)) {
            int ticks = BalanceLaw.controlTicks(t, Wave30Tune.PARADOX_STOP_TICKS);
            Fx.addEffect(t, "minecraft:slowness", ticks, 6);
            Fx.addEffect(t, "minecraft:weakness", ticks, 2);
            n++;
        }
        Fx.addEffect(p, "minecraft:speed", Wave30Tune.PARADOX_STOP_TICKS, 2);
        Fx.addEffect(p, "minecraft:haste", Wave30Tune.PARADOX_STOP_TICKS, 2);
        FxRelic.ring(p.serverLevel(), p.position(), 16, 0xD8C8FF);
        Wave30Kit.shout(p, "TIME STOPS: " + n + (n == 1 ? " foe is" : " foes are") + " held in the moment.", net.minecraft.ChatFormatting.LIGHT_PURPLE);
        forced(p);
    }

    /** Alternate Reality: every foe within eighteen blocks "rerolls initiative": it loses its target and its footing for five seconds. */
    static void alternateReality(ActCtx c) {
        ServerPlayer p = c.player();
        if (!spend(c, Wave30Tune.PARADOX_ALT)) return;
        long until = p.level().getGameTime() + Wave30Tune.PARADOX_ALT_TICKS;
        int n = 0;
        for (LivingEntity t : Fx.foesAround(p, Wave30Tune.PARADOX_RADIUS)) {
            if (t instanceof Mob m && !Wave30Kit.isBoss(m)) Wave30Events.confuse(m, p, until);
            Fx.addEffect(t, Wave30Tune.EF_CONFUSION, t instanceof Player ? 60 : Wave30Tune.PARADOX_ALT_TICKS, 0);
            n++;
        }
        FxRelic.ring(p.serverLevel(), p.position(), Wave30Tune.PARADOX_RADIUS, 0xB070FF);
        Wave30Kit.say(p, "The timeline forks: " + n + (n == 1 ? " foe" : " foes") + " lose the thread.");
    }

    /** Turn the Clock: the item in your off hand (inorganic: a tool or a piece of armour) is turned back to new. */
    static void turnTheClock(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack off = p.getOffhandItem();
        if (off.isEmpty() || !off.isDamaged() || Relics.defOf(off) != null) {
            Wave30Kit.say(p, "Hold a damaged tool or piece of armour in your off hand: Paradox turns it back to new.");
            return;
        }
        if (!spend(c, Wave30Tune.PARADOX_CLOCK)) return;
        off.setDamageValue(0);
        com.newuniverse.nusmp.particle.fx.Fx.burst(p.serverLevel(), FxThemes.RELIC_WAVE_30, p.position().add(0, 1, 0), FxTone.LIGHT, 12, 0.5);
        Wave30Kit.say(p, "The clock turns back: your " + off.getHoverName().getString() + " is as new.");
    }

    /** Missing Time: everyone within eighteen blocks, you included, saves (DC 15) or is aged or made young: slowed and weak, or quickened and mending, for twenty seconds. May force the staff. */
    static void missingTime(ActCtx c) {
        ServerPlayer p = c.player();
        if (!spend(c, Wave30Tune.PARADOX_MISSING)) return;
        ServerLevel level = p.serverLevel();
        double r = Wave30Tune.PARADOX_RADIUS;
        List<LivingEntity> all = level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r), e -> e.isAlive() && !e.isSpectator() && !(e instanceof Player o && o.isCreative())
                && !(e instanceof net.minecraft.world.entity.decoration.ArmorStand) && e.distanceToSqr(p) <= r * r);
        int changed = 0;
        for (LivingEntity e : all) {
            if (changed >= 24) break;
            if (Wave30Rules.saves(p.getRandom().nextInt(20) + 1, 0, Wave30Tune.PARADOX_MISSING_DC)) continue;
            boolean older = p.getRandom().nextBoolean();
            if (older) {
                Fx.addEffect(e, "minecraft:slowness", 400, 1);
                Fx.addEffect(e, "minecraft:weakness", 400, 0);
            } else {
                Fx.addEffect(e, "minecraft:regeneration", 400, 1);
                Fx.addEffect(e, "minecraft:haste", 400, 1);
            }
            changed++;
        }
        FxRelic.ring(level, p.position(), r, 0xFFC8FF);
        Wave30Kit.say(p, "Time slips for " + changed + (changed == 1 ? " creature" : " creatures") + " near you: some grow old, some young, for twenty seconds.");
        forced(p);
    }

    // ================================================================ the Paradox itself
    /** The staff is forced into serving: a DC 17 save, or the wielder is cast out of the timeline. */
    private static void forced(ServerPlayer p) {
        if (Wave30Rules.saves(p.getRandom().nextInt(20) + 1, Wave30Tune.PARADOX_SAVE_BONUS, Wave30Tune.PARADOX_SAVE_DC)) {
            Wave30Kit.say(p, "Paradox strains against you, but you hold it.");
            return;
        }
        double[] back = Wave30Events.pathBack(p);
        if (back != null) {
            p.teleportTo(back[0], back[1], back[2]);
            p.fallDistance = 0;
        }
        Fx.addEffect(p, "minecraft:slowness", 100, 1);
        int roll = p.getRandom().nextInt(101);
        Wave30Rules.Madness m = Wave30Rules.madness(roll);
        int ticks = Wave30Rules.madnessTicks(m, p.getRandom().nextInt(10) + 1);
        switch (m) {
            case PRONE -> Fx.addEffect(p, "minecraft:slowness", ticks, 5);
            case SHORT -> {
                Fx.addEffect(p, "minecraft:nausea", ticks, 0);
                Fx.addEffect(p, "minecraft:slowness", ticks, 0);
            }
            case LONG, INDEFINITE -> {
                Fx.addEffect(p, "minecraft:nausea", ticks, 0);
                Fx.addEffect(p, "minecraft:weakness", ticks, 0);
                Fx.addEffect(p, "minecraft:mining_fatigue", ticks, 0);
            }
        }
        com.newuniverse.nusmp.particle.fx.Fx.burst(p.serverLevel(), FxThemes.RELIC_WAVE_30, p.position().add(0, 1, 0), FxTone.DARK, 12, 0.8);
        String how = switch (m) {
            case PRONE -> "You are knocked flat, and nothing more.";
            case SHORT -> "Short-term madness for " + (ticks / 20) + " seconds.";
            case LONG -> "Long-term madness for " + (ticks / 1200) + " minutes (milk or a golden apple ends it).";
            case INDEFINITE -> "Indefinite madness: it lasts ten minutes, or until you drink milk.";
        };
        Wave30Kit.shout(p, "PARADOX: the staff drags you out of the timeline to a moment before you held it, and all of time floods your mind. " + how, net.minecraft.ChatFormatting.DARK_PURPLE);
    }
}
