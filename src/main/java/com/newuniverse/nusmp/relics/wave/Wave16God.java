package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import com.newuniverse.nusmp.particle.fx.FxRelic;

/**
 * Wave 16: the Portable God. Twelve charges (three come back each dawn) buy six favours; the damage is of a kind that fits the pictured god (here a
 * plain magic blow: the figure's god is whichever the wearer worships). "Next attack roll" and "reaction" become short, armed states on the wearer
 * that the listeners in {@link Wave16Events} spend on the next blow dealt or taken.
 */
final class Wave16God {
    private Wave16God() {}

    private static final String POOL = "god";

    private static boolean pay(ActCtx c, int n) {
        ServerPlayer p = c.player();
        ItemStack s = Wave6Kit.find(p, Wave16Tune.ID_GOD);
        if (s.isEmpty()) s = c.stack();
        int have = Wave16Kit.charges(s, p, POOL, Wave16Tune.GOD_MAX, () -> Wave16Tune.GOD_GAIN);
        if (!Wave16Kit.spend(s, p, POOL, Wave16Tune.GOD_MAX, () -> Wave16Tune.GOD_GAIN, n)) {
            Fx.say(p, "The figure is still: it needs " + n + " charge" + (n > 1 ? "s" : "") + " and holds " + have + ". Three come back at dawn.");
            return false;
        }
        return true;
    }

    private static int d10(ServerPlayer p) { return p.getRandom().nextInt(10) + 1; }

    /** Punishment (1 charge): d10 damage to the creature you look at. */
    static void punish(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity t = Wave16Kit.lookTarget(p, 24.0);
        if (t == null) { Fx.say(p, "No one to punish under thy gaze."); return; }
        if (!pay(c, Wave16Tune.GOD_PUNISH)) return;
        Fx.damage(p, t, d10(p), "minecraft:magic");
        Wave16Kit.puff(p.serverLevel(), t.position().add(0, t.getBbHeight() * 0.6, 0), 0xFF3030, 16, 0.3);
    }

    /** Rescue (2 charges): d10 temporary hit points for you or the friend you look at. */
    static void rescue(ActCtx c) {
        ServerPlayer p = c.player();
        if (!pay(c, Wave16Tune.GOD_RESCUE)) return;
        LivingEntity t = Wave16Kit.lookTarget(p, 16.0);
        LivingEntity who = t != null && Wave6Kit.isAlly(p, t) ? t : p;
        int amp = Math.max(0, (int) Math.ceil(d10(p) / 4.0) - 1);
        Fx.addEffect(who, "minecraft:absorption", 600, amp);
        Wave16Kit.puff(p.serverLevel(), who.position().add(0, 1.0, 0), 0xFFE0A0, 16, 0.4);
    }

    /** Holy Spirit (3 charges): an extra d10 on your next blow within a minute. */
    static void spirit(ActCtx c) {
        ServerPlayer p = c.player();
        if (!pay(c, Wave16Tune.GOD_SPIRIT)) return;
        CompoundTag d = p.getPersistentData();
        d.putLong(Wave16Tune.K_GOD_SPIRIT, p.level().getGameTime() + Wave16Tune.GOD_ARM_TICKS);
        d.putInt(Wave16Tune.K_GOD_SPIRIT_ROLL, d10(p));
        Fx.say(p, "A holy spirit rests on thy next blow.");
    }

    /** Revenge (4 charges): the next foe who strikes you takes 3d10 in return, within a minute. */
    static void revenge(ActCtx c) {
        ServerPlayer p = c.player();
        if (!pay(c, Wave16Tune.GOD_REVENGE)) return;
        p.getPersistentData().putLong(Wave16Tune.K_GOD_REVENGE, p.level().getGameTime() + Wave16Tune.GOD_ARM_TICKS);
        Fx.say(p, "The god waits at thy shoulder: the next who strikes thee is struck back.");
    }

    /** Deus Ex (5 charges): for a minute you are the avatar; every blow carries an extra d10, and a god's patience guards you. */
    static void avatar(ActCtx c) {
        ServerPlayer p = c.player();
        if (!pay(c, Wave16Tune.GOD_AVATAR)) return;
        p.getPersistentData().putLong(Wave16Tune.K_GOD_AVATAR, p.level().getGameTime() + Wave16Tune.GOD_ARM_TICKS);
        Fx.addEffect(p, "minecraft:resistance", Wave16Tune.GOD_ARM_TICKS, 0);
        Fx.addEffect(p, "minecraft:glowing", Wave16Tune.GOD_ARM_TICKS, 0);
        FxRelic.ring(p.serverLevel(), p.position(), 3.0, 0xFF3030);
        Fx.say(p, "Thou art the god's avatar for a minute.");
    }

    /** Wonder (7 charges): the wild magic table, d100; a hundred is a Wish. */
    static void wonder(ActCtx c) {
        ServerPlayer p = c.player();
        if (!pay(c, Wave16Tune.GOD_WONDER)) return;
        ServerLevel lv = p.serverLevel();
        int roll = p.getRandom().nextInt(100) + 1;
        int row = Wave16Rules.wildRow(roll);
        Fx.say(p, "Wonder: d100 = " + roll + (row < 0 ? ". A WISH." : "."));
        Wave16Kit.puff(lv, p.position().add(0, 1.2, 0), 0xFFFFFF, 30, 0.7);
        switch (row) {
            case -1 -> {
                Fx.heal(p, p.getMaxHealth());
                for (String bad : new String[]{"minecraft:poison", "minecraft:wither", "minecraft:weakness", "minecraft:slowness", "minecraft:blindness", "minecraft:hunger", "minecraft:nausea", "tensura:fear"}) Wave6Kit.lift(p, bad);
                Fx.addEffect(p, "minecraft:regeneration", 600, 2);
                Fx.addEffect(p, "minecraft:resistance", 600, 1);
                Fx.addEffect(p, "minecraft:strength", 600, 1);
                Fx.shield(p, 8f, 1200);
            }
            case 0 -> { FxRelic.ring(lv, p.position(), 5.0, 0xFF6020); Fx.aoe(p, 5.0, 6f, "minecraft:in_fire", null, 0, 0); }
            case 1 -> {
                LivingEntity t = Wave16Kit.lookTarget(p, 24.0);
                if (t != null) { Fx.damage(p, t, 8f, "minecraft:lightning_bolt"); Wave16Kit.puff(lv, t.position().add(0, 1.0, 0), 0xFFFF80, 20, 0.4); }
            }
            case 2 -> { Fx.heal(p, 8f); Fx.addEffect(p, "minecraft:regeneration", 100, 1); }
            case 3 -> { Fx.addEffect(p, "minecraft:levitation", 100, 0); Fx.addEffect(p, "minecraft:slow_falling", 300, 0); }
            case 4 -> { Fx.addEffect(p, "minecraft:speed", 600, 1); Fx.addEffect(p, "minecraft:haste", 600, 1); }
            case 5 -> Fx.shield(p, 8f, 600);
            case 6 -> { Fx.addEffect(p, "minecraft:invisibility", 600, 0); Fx.addEffect(p, "minecraft:night_vision", 600, 0); }
            case 7 -> { Fx.addEffect(p, "minecraft:nausea", 120, 0); Fx.say(p, "The god laughs at thee."); }
            case 8 -> shove(p, 6.0);
            default -> { Fx.addEffect(p, "minecraft:strength", 600, 0); Fx.addEffect(p, "minecraft:resistance", 600, 0); }
        }
    }

    /** A shock wave: every foe within the radius is thrown back and slowed. */
    private static void shove(ServerPlayer p, double radius) {
        FxRelic.ring(p.serverLevel(), p.position(), radius, 0xC0C0FF);
        for (LivingEntity t : Fx.foesAround(p, radius)) {
            Vec3 away = t.position().subtract(p.position());
            if (away.lengthSqr() < 1.0e-4) continue;
            Vec3 push = away.normalize().scale(1.2);
            t.setDeltaMovement(t.getDeltaMovement().add(push.x, 0.4, push.z));
            t.hurtMarked = true;
            Fx.addEffect(t, "minecraft:slowness", 60, 1);
        }
    }
}
