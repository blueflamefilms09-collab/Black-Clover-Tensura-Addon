package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Why: the three drinks of wave 29 that change the drinker for a while (Potion of Vampirism, Romlo's Potion of Sleep, Spider Brew) keep their acts and
 * their once-a-second upkeep here: the d20 table of the vampire potion and what each result leaves on the player, the wisdom save and the sleep, and
 * the spider's body and claws. Every timer is an end time in persistent data (players: the part that survives death); the numbers are in {@link Wave29Rules}.
 */
final class Wave29Brews {
    private Wave29Brews() {}

    static final String K_CURSE = "curse_until", K_REGEN = "regen", K_REGEN_HOLD = "regen_hold", K_CHILDREN = "children_until", K_PACK_CD = "pack_cd",
            K_PACK_AT = "pack_at", K_PACK_OUT = "pack_out", K_SLEEP = "sleep_until", K_SPIDER = "spider_until";

    // ================================================================ Potion of Vampirism
    /**
     * Potion of Vampirism: the card's d20 is rolled on the server and told to the drinker.
     * <ul>
     *   <li>1-12 Cursed: for three Minecraft days (until you die) you take a vampire's weaknesses: sunlight burns (one health a second, never below two
     *       hearts) and weakens, flowing water burns the same way, and radiant, holy and sacred damage is doubled.</li>
     *   <li>13-18 Children of the Night: for five Minecraft days, when a hostile creature wounds you, a pack answers (once in five minutes): after 1d4
     *       rounds, wolves outdoors or bats indoors arrive as allies for five minutes (see {@link #packArrives}).</li>
     *   <li>19-20 Regeneration, for good: every other second you heal 3 health, but not while sunlight or flowing water touches you and not for a round
     *       after radiant, holy or sacred damage.</li>
     * </ul>
     */
    static void vampirism(ActCtx c) {
        ServerPlayer p = c.player();
        int roll = Wave29Rules.d20(p.getRandom()::nextInt);
        CompoundTag d = Wave29Util.data(p);
        long now = p.level().getGameTime();
        ServerLevel sl = p.serverLevel();
        com.newuniverse.nusmp.particle.fx.Fx.spray(sl, FxThemes.RELIC_WAVE_29, FxTone.DARK, p.getX(), p.getY() + 1.2, p.getZ(), 24, 0.5, 0.7, 0.5, 0.03);
        sl.playSound(null, p.blockPosition(), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 1.0f, 0.6f);
        switch (Wave29Rules.vampire(roll)) {
            case CURSE -> {
                d.putLong(K_CURSE, now + Wave29Rules.CURSE_TICKS);
                sl.playSound(null, p.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 0.6f, 1.6f);
                Wave29Util.tell(p, "Potion of Vampirism (rolled " + roll + "): Cursed. For three days you carry a vampire's weaknesses: sunlight and flowing water burn you, and holy light hurts twice as much. Death ends the curse.", ChatFormatting.DARK_RED);
            }
            case CHILDREN -> {
                d.putLong(K_CHILDREN, now + Wave29Rules.CHILDREN_TICKS);
                d.remove(K_PACK_CD);
                d.remove(K_PACK_AT);
                Wave29Util.tell(p, "Potion of Vampirism (rolled " + roll + "): Children of the Night. For five days, when something hostile wounds you, wolves (outdoors) or bats (underground) come to your side.", ChatFormatting.DARK_PURPLE);
            }
            case REGEN -> {
                boolean had = d.getBoolean(K_REGEN);
                d.putBoolean(K_REGEN, true);
                Wave29Util.tell(p, "Potion of Vampirism (rolled " + roll + "): Regeneration" + (had ? " (you already had it: nothing more to give)" : ", for good") + ". Your wounds close on their own, unless sunlight, flowing water or holy light touches you.", ChatFormatting.RED);
            }
        }
    }

    /** Once a second per player: the vampire's curse, regeneration, the pack arriving. */
    static void vampirePass(ServerPlayer p, CompoundTag d) {
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        boolean cursed = d.getLong(K_CURSE) > now;
        if (d.getLong(K_CURSE) > 0 && !cursed) {
            d.remove(K_CURSE);
            Wave29Util.tell(p, "Potion of Vampirism: the curse lifts. The sun is only the sun again.", ChatFormatting.GRAY);
        }
        boolean exposed = false;
        if (cursed || d.getBoolean(K_REGEN)) {
            boolean sun = Wave29Rules.sunlit(sl.isDay(), sl.isRaining(), sl.canSeeSky(p.blockPosition()));
            FluidState fs = sl.getFluidState(p.blockPosition());
            boolean flowing = fs.is(FluidTags.WATER) && !fs.isSource();
            exposed = sun || flowing;
            if (cursed && exposed && !p.isCreative() && Wave29Rules.burns(p.getHealth())) {
                p.hurt(p.damageSources().onFire(), Wave29Rules.BURN);
                com.newuniverse.nusmp.particle.fx.Fx.aura(sl, FxThemes.RELIC_WAVE_29, p, FxTone.DARK);
                if (sun) Wave29Util.give(p, "minecraft:weakness", 60, 0);
            }
        }
        if (d.getBoolean(K_REGEN) && !exposed && d.getLong(K_REGEN_HOLD) <= now && p.getHealth() < p.getMaxHealth() && (p.tickCount / 20) % 2 == 0) {
            Fx.heal(p, Wave29Rules.REGEN_HEAL);
            com.newuniverse.nusmp.particle.fx.Fx.aura(sl, FxThemes.RELIC_WAVE_29, p, FxTone.DARK);
        }
        long at = d.getLong(K_PACK_AT);
        if (at > 0 && now >= at) {
            d.remove(K_PACK_AT);
            packArrives(p, d.getBoolean(K_PACK_OUT));
        }
    }

    /** A hostile creature wounded a player who has the boon: the pack is called (once in five minutes) and comes 1d4 rounds later. */
    static void woundedBy(ServerPlayer p, LivingEntity attacker) {
        CompoundTag d = Wave29Util.peek(p);
        if (d == null || attacker == null) return;
        long now = p.level().getGameTime();
        if (d.getLong(K_CHILDREN) <= now || d.getLong(K_PACK_CD) > now || d.getLong(K_PACK_AT) > 0) return;
        if (!Wave29Util.foe(p, attacker)) return;
        d.putLong(K_PACK_CD, now + Wave29Rules.PACK_COOLDOWN);
        d.putLong(K_PACK_AT, now + Wave29Rules.packDelay(Wave29Rules.die(p.getRandom()::nextInt, 4)));
        d.putBoolean(K_PACK_OUT, p.serverLevel().canSeeSky(p.blockPosition()));
        Wave29Util.tell(p, "Children of the Night: you call, and the dark answers. They are coming.", ChatFormatting.DARK_PURPLE);
        com.newuniverse.nusmp.particle.fx.Fx.gather(p.serverLevel(), FxThemes.RELIC_WAVE_29, p, FxTone.DARK, 8);
    }

    /** The pack: wolves if the call was made under the open sky, bats otherwise; allies for five minutes, driven by the relics' summon sweep. */
    private static void packArrives(ServerPlayer p, boolean outdoors) {
        int n = Wave29Rules.packSize(outdoors, p.getRandom()::nextInt);
        int made = 0;
        for (int i = 0; i < n; i++) {
            LivingEntity e = outdoors ? Fx.summon(p, "minecraft:wolf", Wave29Rules.PACK_LIFE) : bat(p);
            if (e == null) continue;
            made++;
            com.newuniverse.nusmp.particle.fx.Fx.burst(p.serverLevel(), FxThemes.RELIC_WAVE_29, e, FxTone.DARK, 4);
        }
        Wave29Util.tell(p, made > 0 ? "Children of the Night: " + made + (outdoors ? " wolves" : " bats") + " arrive and take your side for five minutes."
                : "Children of the Night: no creature answers this time.", ChatFormatting.DARK_PURPLE);
    }

    private static LivingEntity bat(ServerPlayer p) {
        LivingEntity e = Fx.summon(p, "tensura:giant_bat", Wave29Rules.PACK_LIFE);
        return e != null ? e : Fx.summon(p, "minecraft:bat", Wave29Rules.PACK_LIFE);
    }

    // ================================================================ Romlo's Potion of Sleep
    /**
     * Romlo's Potion of Sleep. Drunk, it makes you save against it; sneak while you drink to pour it into the mouth of the creature you face (within
     * four blocks) instead. The Wisdom save is a d20 plus one per ten health (and a little for a full pool of magicules) against 15; a creature that saves
     * is only drowsy for ten seconds. One that fails sleeps for five minutes (the card's five hours at a minute an hour): it cannot move, fight, mine or
     * use things, and any damage wakes it. Creatures of 100 health or more cannot be put to sleep.
     */
    static void sleep(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity target = p;
        if (c.sneaking()) {
            target = faced(p);
            if (target == null) {
                Wave29Util.keep(p, c.stack());
                Wave29Util.tell(p, "Romlo's Potion of Sleep: nobody in front of you to pour it into. The bottle is kept.", ChatFormatting.GRAY);
                return;
            }
        }
        if (target.getMaxHealth() >= Wave29Rules.SLEEP_MAX_HEALTH) {
            Wave29Util.keep(p, c.stack());
            Wave29Util.tell(p, "Romlo's Potion of Sleep: something that mighty only yawns. The bottle is kept.", ChatFormatting.GRAY);
            return;
        }
        int roll = Wave29Rules.d20(p.getRandom()::nextInt);
        int bonus = Wave29Rules.saveBonus(target.getMaxHealth(), Wave29Util.magiculeFraction(target));
        ServerLevel sl = p.serverLevel();
        String who = target == p ? "You" : target.getName().getString();
        if (Wave29Rules.saves(roll, bonus)) {
            Wave29Util.give(target, "minecraft:slowness", Wave29Rules.DROWSY_TICKS, 0);
            com.newuniverse.nusmp.particle.fx.Fx.spray(sl, FxThemes.RELIC_WAVE_29, FxTone.LIGHT, target.getX(), target.getY() + target.getBbHeight() * 0.8, target.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
            Wave29Util.say(p, "Romlo's Potion of Sleep: " + who + " save" + (target == p ? "" : "s") + " against it (" + roll + "+" + bonus + " against DC " + Wave29Rules.SLEEP_DC + ") and only yawn" + (target == p ? "" : "s") + ".");
            if (target instanceof ServerPlayer tp && tp != p) Wave29Util.say(tp, "Something sleepy was poured into your mouth, but you shake it off.");
            return;
        }
        fallAsleep(target, Wave29Rules.SLEEP_TICKS);
        sl.playSound(null, target.blockPosition(), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 1.0f, 0.5f);
        Wave29Util.say(p, "Romlo's Potion of Sleep: " + who + " fail" + (target == p ? "" : "s") + " the save (" + roll + "+" + bonus + " against DC " + Wave29Rules.SLEEP_DC + ") and drop" + (target == p ? "" : "s") + " fast asleep. Any damage will wake " + (target == p ? "you" : "them") + ".");
        if (target instanceof ServerPlayer tp && tp != p) Wave29Util.say(tp, "Something sleepy was poured into your mouth, and the world goes dark.");
    }

    /** The creature the player faces within four blocks: the best-aligned living thing that is not a pet or a creative or spectator player. */
    private static LivingEntity faced(ServerPlayer p) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        double r = Wave29Rules.POUR_REACH;
        LivingEntity best = null;
        double bestDot = 0.9;
        List<LivingEntity> near = p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r), e -> e != p && e.isAlive() && !e.isSpectator());
        for (LivingEntity e : near) {
            if (e instanceof Player o && (o.isCreative() || !p.canHarmPlayer(o))) continue;
            if (e instanceof OwnableEntity own && p.getUUID().equals(own.getOwnerUUID())) continue;
            if (e.getPersistentData().hasUUID("nusmp_relic_owner")) continue;
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double dist = to.length();
            if (dist > r || dist < 1.0e-3) continue;
            double dot = to.scale(1.0 / dist).dot(look);
            if (dot > bestDot) { bestDot = dot; best = e; }
        }
        return best;
    }

    /** Puts a creature to sleep: a timer in its data and the effects that hold it still (Tensura's Sleep, then slowness, weakness, fatigue, blindness for a player). */
    static void fallAsleep(LivingEntity e, int ticks) {
        Wave29Util.data(e).putLong(K_SLEEP, e.level().getGameTime() + ticks);
        holdSleeper(e, ticks);
        if (e instanceof Mob m) m.setTarget(null);
    }

    private static void holdSleeper(LivingEntity e, int ticks) {
        Wave29Util.give(e, "tensura:sleep", ticks, 0);
        Wave29Util.give(e, "minecraft:slowness", ticks, 9);
        Wave29Util.give(e, "minecraft:weakness", ticks, 9);
        Wave29Util.give(e, "minecraft:mining_fatigue", ticks, 9);
        if (e instanceof Player) Wave29Util.give(e, "minecraft:blindness", ticks, 0);
    }

    /** Damage wakes a sleeper (player or creature): the effects the potion put on it come off. */
    static void wake(LivingEntity e) {
        CompoundTag d = Wave29Util.peek(e);
        if (d == null || d.getLong(K_SLEEP) <= 0) return;
        boolean was = d.getLong(K_SLEEP) > e.level().getGameTime();
        d.remove(K_SLEEP);
        if (!was) return;
        Wave29Util.strip(e, "tensura:sleep", 0);
        Wave29Util.strip(e, "minecraft:slowness", 9);
        Wave29Util.strip(e, "minecraft:weakness", 9);
        Wave29Util.strip(e, "minecraft:mining_fatigue", 9);
        Wave29Util.strip(e, "minecraft:blindness", 0);
        if (e instanceof ServerPlayer p) Wave29Util.tell(p, "You wake with a start.", ChatFormatting.GRAY);
    }

    /** Once a second per player: a sleeper keeps its effects (milk does not cure it) and wakes when the time is up. */
    static void sleepPass(ServerPlayer p, CompoundTag d) {
        long left = d.getLong(K_SLEEP) - p.level().getGameTime();
        if (d.getLong(K_SLEEP) <= 0) return;
        if (left <= 0) {
            d.remove(K_SLEEP);
            Wave29Util.tell(p, "Romlo's Potion of Sleep: you wake, rested and a little ashamed.", ChatFormatting.GRAY);
            return;
        }
        boolean held = Wave29Util.effect("minecraft:slowness").map(h -> p.hasEffect(h)).orElse(true);
        if (!held) holdSleeper(p, (int) Math.min(left, Integer.MAX_VALUE));
    }

    // ================================================================ Spider Brew
    private static final ResourceLocation M_HEALTH = id("health"), M_ARMOR = id("armor"), M_ATTACK = id("attack"), M_JUMP = id("jump"), M_SCALE = id("scale");

    private static ResourceLocation id(String s) { return ResourceLocation.fromNamespaceAndPath("nusmp", "relic_w29_spider_" + s); }

    /**
     * Spider Brew: for 1d4 "hours" (five minutes each) you are a giant spider in body and keep your own mind: +6 maximum health, +3 armour, +2 attack, a
     * higher jump, a low long body (a smaller scale), night vision, and you climb any wall you press against (sneak to cling still). Your bite poisons
     * everything you hit in melee for six seconds and hits 3 harder when your hand is empty. Tensura lends you its Poison Resistance and Sticky Steel Thread
     * (web) skills where the world has them. Drinking another while you are a spider only renews the time.
     */
    static void spiderBrew(ActCtx c) {
        ServerPlayer p = c.player();
        int d4 = Wave29Rules.die(p.getRandom()::nextInt, 4);
        int ticks = Wave29Rules.spiderTicks(d4);
        CompoundTag d = Wave29Util.data(p);
        long now = p.level().getGameTime();
        d.putLong(K_SPIDER, Math.max(d.getLong(K_SPIDER), now + ticks));
        shape(p);
        int lent = Fx.lendSkills(p, ticks, "tensura:poison_resistance", "tensura:sticky_steel_thread");
        Wave29Util.give(p, "minecraft:night_vision", ticks, 0);
        ServerLevel sl = p.serverLevel();
        com.newuniverse.nusmp.particle.fx.Fx.spray(sl, FxThemes.RELIC_WAVE_29, FxTone.DARK, p.getX(), p.getY() + 1.0, p.getZ(), 30, 0.5, 0.6, 0.5, 0.04);
        sl.playSound(null, p.blockPosition(), SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 1.0f, 0.7f);
        Wave29Util.say(p, "Spider Brew (rolled " + d4 + "): your limbs fold and lengthen and you scuttle as a giant spider for " + d4 * 5 + " minutes" + (lent > 0 ? " (Tensura skills lent: " + lent + ")" : "") + ". Press against a wall to climb it; sneak to cling.");
    }

    /** Puts the spider's body on the player (stable modifier ids, so doing it twice changes nothing). */
    private static void shape(ServerPlayer p) {
        put(p, Attributes.MAX_HEALTH, M_HEALTH, Wave29Rules.SPIDER_HEALTH, AttributeModifier.Operation.ADD_VALUE);
        put(p, Attributes.ARMOR, M_ARMOR, Wave29Rules.SPIDER_ARMOR, AttributeModifier.Operation.ADD_VALUE);
        put(p, Attributes.ATTACK_DAMAGE, M_ATTACK, Wave29Rules.SPIDER_ATTACK, AttributeModifier.Operation.ADD_VALUE);
        put(p, Attributes.JUMP_STRENGTH, M_JUMP, Wave29Rules.SPIDER_JUMP, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        put(p, Attributes.SCALE, M_SCALE, Wave29Rules.SPIDER_SCALE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    private static void unshape(ServerPlayer p) {
        for (ResourceLocation m : List.of(M_HEALTH, M_ARMOR, M_ATTACK, M_JUMP, M_SCALE)) {
            for (Holder<Attribute> a : List.of(Attributes.MAX_HEALTH, Attributes.ARMOR, Attributes.ATTACK_DAMAGE, Attributes.JUMP_STRENGTH, Attributes.SCALE)) {
                AttributeInstance inst = p.getAttribute(a);
                if (inst != null) inst.removeModifier(m);
            }
        }
    }

    private static void put(ServerPlayer p, Holder<Attribute> attr, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null || inst.getModifier(id) != null) return;
        inst.addTransientModifier(new AttributeModifier(id, amount, op));
    }

    /** Once a second per player: the body is kept while the time runs (it is not saved with the player, so a relog puts it back) and goes when it is up. */
    static void spiderPass(ServerPlayer p, CompoundTag d) {
        long until = d.getLong(K_SPIDER);
        if (until <= 0) return;
        if (until <= p.level().getGameTime()) {
            d.remove(K_SPIDER);
            unshape(p);
            Wave29Util.tell(p, "Spider Brew: your body unfolds, and you are yourself again.", ChatFormatting.GRAY);
            return;
        }
        shape(p);
    }

    /** Called when the player dies in a spider body: the modifiers are not saved, but the timer must not outlive the death. */
    static void endSpider(ServerPlayer p) {
        CompoundTag d = Wave29Util.peek(p);
        if (d != null) d.remove(K_SPIDER);
        unshape(p);
    }

    /** Every tick: a spider pressed against a wall climbs it (sneaking, it clings still). */
    static void climb(ServerPlayer p) {
        double vy = Wave29Rules.climb(p.horizontalCollision, p.isShiftKeyDown());
        if (Double.isNaN(vy) || p.isPassenger() || p.isFallFlying() || p.getAbilities().flying || !Wave29Util.running(p, K_SPIDER)) return;
        Vec3 m = p.getDeltaMovement();
        p.setDeltaMovement(m.x, vy, m.z);
        p.hurtMarked = true;
        p.fallDistance = 0;
    }

    /** The bite: poison on a melee hit, and extra damage when the main hand is empty. Returns the extra damage (0 if none). */
    static float bite(ServerPlayer attacker, LivingEntity victim) {
        Wave29Util.give(victim, "minecraft:poison", Wave29Rules.POISON_TICKS, 0);
        com.newuniverse.nusmp.particle.fx.Fx.burst(attacker.serverLevel(), FxThemes.RELIC_WAVE_29, victim, FxTone.DARK, 3);
        return attacker.getMainHandItem().isEmpty() ? Wave29Rules.BITE : 0f;
    }
}
