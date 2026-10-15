package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.particle.rukh.RukhFx;

import com.newuniverse.nusmp.dungeon.LivingDungeon;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Wave 18: what the seven Bountiful Bags do besides holding things. Opening one is the first act of each; the Bags of Colding, Molding, Scolding
 * stuff a foe inside (the card's "creatures inside must make a saving throw each minute" becomes three pulses over three seconds, each a roll
 * against the foe's own bracing), the Bag of Devouring gulps one after a telegraphed three seconds, the Bag of Colding serves a chilled snack and the
 * Bag of Unfolding unlatches. All of it is server side, bounded, and goes through the Fx damage rules (never past 30% of a player's health).
 */
final class Wave18Bags {
    private Wave18Bags() {}

    static void open(ActCtx c, Wave18Pure.Bag bag) { Wave18Pockets.open(c.player(), c.stack(), bag, c.def().cardName()); }

    // ---------------------------------------------------------------- stuffing a foe
    /** The nearest foe in front of the player that fits through the bag's mouth, or null. */
    static LivingEntity pick(ServerPlayer p) {
        for (LivingEntity e : Wave18Kit.inFront(p, Fx.foesAround(p, Wave18Tune.STUFF_REACH))) {
            if (Wave18Pure.fitsBag(e.getBbWidth(), e.getBbHeight(), e.getMaxHealth())) return e;
        }
        return null;
    }

    /** Holds the foe where it stands: a mob is pinned and lit up; a player is only slowed, for a few seconds. */
    static void restrain(LivingEntity t, int ticks) {
        if (t instanceof Player) {
            Fx.addEffect(t, "minecraft:slowness", Math.min(ticks, 80), 2);
            return;
        }
        Fx.addEffect(t, "minecraft:slowness", ticks, 6);
        Fx.addEffect(t, "minecraft:weakness", ticks, 1);
        Fx.addEffect(t, "minecraft:glowing", ticks, 0);
    }

    static void stuff(ActCtx c, Wave18Pure.Bag bag) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        LivingEntity t = pick(p);
        if (t == null) {
            Fx.say(p, "Nothing small enough to fit the bag's mouth is within reach.");
            return;
        }
        long now = level.getGameTime();
        UUID pid = p.getUUID(), tid = t.getUUID();
        level.playSound(null, t.blockPosition(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 1.0f, 0.7f);
        if (bag.hazard() == Wave18Pure.Hazard.GULP) {
            gulp(level, p, t, now);
            return;
        }
        restrain(t, Wave18Tune.STUFF_PULSE * Wave18Tune.STUFF_PULSES + 10);
        int[] fails = {0};
        for (int k = 1; k <= Wave18Tune.STUFF_PULSES; k++) {
            final int pulse = k;
            if (!Wave18Events.QUEUE.at(now + (long) Wave18Tune.STUFF_PULSE * k, () -> pulse(level, pid, tid, bag, pulse, fails))) break;
        }
    }

    private static RukhFx.Alignment puff(Wave18Pure.Hazard h) {
        return switch (h) {
            case COLD -> RukhFx.Alignment.LIGHT;
            case POISON -> RukhFx.Alignment.DARK;
            case PSYCHIC -> RukhFx.Alignment.LIGHT;
            default -> RukhFx.Alignment.DARK;
        };
    }

    /** One "end of a minute" inside the bag: a roll against the foe's bracing; a failed one hurts, and a foe that fails twice of three is worn down. */
    private static void pulse(ServerLevel level, UUID pid, UUID tid, Wave18Pure.Bag bag, int pulse, int[] fails) {
        ServerPlayer owner = Wave18Kit.player(level, pid);
        LivingEntity t = Wave18Kit.living(level, tid);
        if (owner == null || t == null) return;
        RukhFx.spray(level, puff(bag.hazard()), t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), 10, 0.3, 0.4, 0.3, 0.02);
        double kb = t.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        if (!Wave18Pure.failsSave(level.getRandom().nextDouble(), kb)) return;
        fails[0]++;
        switch (bag.hazard()) {
            case COLD -> {
                Fx.damage(owner, t, Wave18Tune.PULSE_DAMAGE, "minecraft:freeze");
                t.setTicksFrozen(Math.max(t.getTicksFrozen(), t.getTicksRequiredToFreeze() + 40));
                Fx.addEffect(t, "tensura:chill", 60, 0);
            }
            case POISON -> {
                Fx.damage(owner, t, Wave18Tune.PULSE_DAMAGE, "minecraft:indirect_magic");
                Fx.addEffect(t, "minecraft:poison", 60, 0);
            }
            case PSYCHIC -> {
                Fx.damage(owner, t, Wave18Tune.PULSE_DAMAGE, "minecraft:magic");
                Fx.addEffect(t, "tensura:fear", 60, 0);
                Fx.addEffect(t, "minecraft:weakness", 100, 1);                     // "disadvantage on the next attack"
            }
            default -> { }
        }
        if (pulse == Wave18Tune.STUFF_PULSES && fails[0] >= 2) {
            switch (bag.hazard()) {
                case COLD -> {                                                       // a level of exhaustion
                    Fx.addEffect(t, "minecraft:mining_fatigue", 400, 1);
                    Fx.addEffect(t, "minecraft:weakness", 400, 0);
                    Fx.addEffect(t, "minecraft:hunger", 200, 1);
                }
                case POISON -> {                                                     // poisoned for an hour
                    Fx.addEffect(t, "minecraft:poison", 200, 0);
                    Fx.addEffect(t, "minecraft:weakness", 600, 0);
                    Fx.addEffect(t, "minecraft:nausea", 100, 0);
                }
                default -> { }
            }
        }
    }

    /** The Bag of Devouring: half the time it pulls a foe in (less if the foe braces); three seconds later it gulps. */
    private static void gulp(ServerLevel level, ServerPlayer p, LivingEntity t, long now) {
        double kb = t.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        if (!Wave18Pure.pulledIn(level.getRandom().nextDouble(), kb)) {
            Fx.say(p, "The bag snaps at it and misses.");
            return;
        }
        restrain(t, Wave18Tune.GULP_DELAY + 20);
        Fx.addEffect(t, "minecraft:glowing", Wave18Tune.GULP_DELAY + 20, 0);
        level.playSound(null, t.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.8f, 1.4f);
        UUID pid = p.getUUID(), tid = t.getUUID();
        Wave18Events.QUEUE.at(now + Wave18Tune.GULP_DELAY, () -> {
            ServerPlayer owner = Wave18Kit.player(level, pid);
            LivingEntity target = Wave18Kit.living(level, tid);
            if (owner == null || target == null) return;
            Fx.damageHeavy(owner, target, Wave18Pure.gulp(target.getMaxHealth()), "minecraft:magic");
            RukhFx.spray(level, RukhFx.Alignment.DARK, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 24, 0.4, 0.5, 0.4, 0.03);
            level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.2f, 0.6f);
        });
    }

    // ---------------------------------------------------------------- the chilled snack
    /** The Bag of Colding: eats the best-fed morsel from the larder inside, fresh and chilled: its bad side effects are left behind and it feeds half again. */
    static void snack(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack one = Wave18Pockets.takeOne(p, c.stack(), s -> s.has(DataComponents.FOOD));
        if (one.isEmpty()) {
            Fx.say(p, "The larder is empty.");
            return;
        }
        FoodProperties f = one.get(DataComponents.FOOD);
        if (f == null) return;
        p.getFoodData().eat(f.nutrition(), f.saturation() * 1.5f);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    // ---------------------------------------------------------------- unfolding
    /** The Bag of Unfolding: shakes off what holds the bearer (webs, locks, a crushing slow) and unlatches the iron door, trapdoor or gate it is pointed at. */
    static void unlatch(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel level = p.serverLevel();
        List<MobEffectInstance> held = new ArrayList<>();
        for (MobEffectInstance inst : p.getActiveEffects()) if (Wave6Kit.restraint(inst)) held.add(inst);
        for (MobEffectInstance inst : held) p.removeEffect(inst.getEffect());
        HitResult hit = p.pick(Wave18Tune.UNLATCH_REACH, 1.0f, false);
        if (!(hit instanceof BlockHitResult b) || hit.getType() != HitResult.Type.BLOCK) {
            if (!held.isEmpty()) Fx.say(p, "What held you comes loose.");
            else Fx.say(p, "Point the bag at a latched door, trapdoor or gate.");
            return;
        }
        if (LivingDungeon.in(level)) {
            Fx.say(p, "The Dungeon's locks are not yours to open.");
            return;
        }
        BlockPos pos = b.getBlockPos();
        BlockState st = level.getBlockState(pos);
        Block block = st.getBlock();
        boolean opened = false;
        if (block instanceof DoorBlock door && !st.getValue(DoorBlock.OPEN)) {
            door.setOpen(p, level, st, pos, true);
            opened = true;
        } else if (block instanceof TrapDoorBlock && !st.getValue(BlockStateProperties.OPEN)) {
            level.setBlock(pos, st.setValue(BlockStateProperties.OPEN, true), 10);
            opened = true;
        } else if (block instanceof FenceGateBlock && !st.getValue(BlockStateProperties.OPEN)) {
            level.setBlock(pos, st.setValue(BlockStateProperties.OPEN, true), 10);
            opened = true;
        }
        if (opened) {
            level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0f, 1.2f);
            RukhFx.spray(level, RukhFx.Alignment.LIGHT, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 16, 0.4, 0.5, 0.4, 0.5);
        } else if (held.isEmpty()) {
            Fx.say(p, "Nothing here is latched.");
        }
    }

    /** True when this relic id is one of the bags. */
    static boolean isBag(String id) { return Wave18Pure.Bag.isBag(id) && Relics.byId(id) != null; }
}
