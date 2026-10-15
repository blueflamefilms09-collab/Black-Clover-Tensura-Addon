package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.particle.rukh.RukhFx;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicFlags;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: the eight gloves and gauntlets of wave 27 (Arcane Trickster's Glove, Bear's Grip, Dragon's Fire Gauntlet, Gauntlet Mimic, Gauntlet of the
 * Stormcaller, Glove of Appreciation, Gloves of Opportunity, Goristro Charger) as Minecraft mechanics. One static method per ability; the
 * damage-time parts are called from {@link Wave27Events}.
 */
final class Wave27Hands {
    private Wave27Hands() {}

    private static final String LIGHTNING = "minecraft:lightning_bolt", FIRE = "minecraft:in_fire", HIT = "minecraft:player_attack";
    private static final String[] STUN = {"tensura:paralysis", "tensura:petrification", "tensura:webbed"};

    // ================================================================ Arcane Trickster's Glove
    /**
     * Invisible Mage Hand: an unseen hand reaches up to eight blocks to the creature you look at and lifts something from its hands (or a villager's
     * pockets); failing that it fetches the dropped item you look at, up to twelve blocks away. It leaves no trail. A creature that catches the hand
     * turns on you. Stealth and sleight of hand are your Arcana here: the chance is better against the frail.
     */
    static void mageHand(ActCtx c) {
        ServerPlayer p = c.player();
        Wave27Util.Aim a = Wave27Util.aim(p, 8.0, e -> Wave27Util.softMob(p, e) && Wave27Util.holdsSomething(e) || e instanceof net.minecraft.world.entity.npc.Villager);
        if (a.hit() != null) {
            LivingEntity t = a.hit();
            if (!Wave27Rules.steals(Wave27Rules.stealChance(t.getMaxHealth()), p.getRandom().nextInt(100))) {
                notice(p, t, "Your unseen hand was felt: it turns on you.");
                return;
            }
            ItemStack got = Wave27Util.steal(p, t);
            if (got.isEmpty()) Wave27Util.tell(p, "Its pockets are empty.", ChatFormatting.GRAY);
            else Wave27Util.tell(p, "Your hand lifts " + got.getHoverName().getString() + ".", ChatFormatting.LIGHT_PURPLE);
            return;
        }
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        ItemEntity best = null;
        double bestDot = 0.96;
        for (ItemEntity it : p.serverLevel().getEntitiesOfClass(ItemEntity.class, new AABB(eye, eye).inflate(12.0), i -> i.isAlive() && !i.hasPickUpDelay())) {
            Vec3 d = it.position().subtract(eye);
            if (d.lengthSqr() > 144.0 || d.lengthSqr() < 0.04) continue;
            double dot = d.normalize().dot(look);
            if (dot > bestDot) {
                bestDot = dot;
                best = it;
            }
        }
        if (best == null) {
            Wave27Util.tell(p, "Your hand finds nothing to take.", ChatFormatting.GRAY);
            return;
        }
        ItemStack s = best.getItem();
        if (p.getInventory().add(s.copy())) best.discard();
        else best.setPos(p.getX(), p.getY(), p.getZ());
        Wave27Util.tell(p, "Your unseen hand brings it to you.", ChatFormatting.LIGHT_PURPLE);
    }

    /** A creature that caught a thieving hand: a monster turns on the thief, anything else simply looks. */
    private static void notice(ServerPlayer p, LivingEntity t, String text) {
        if (t instanceof Mob m && Wave27Util.hostile(p, t)) m.setTarget(p);
        else if (t instanceof Mob m) m.getLookControl().setLookAt(p);
        Wave27Util.tell(p, text, ChatFormatting.GRAY);
    }

    // ================================================================ Bear's Grip
    /**
     * Bear Grapple: seize the creature in reach (3.5 blocks) and hold it: Slowness VI and Weakness for four seconds. A target of far higher existence
     * (its "Strength save") wrenches free.
     */
    static void bearGrapple(ActCtx c) {
        ServerPlayer p = c.player();
        Wave27Util.Aim a = Wave27Util.aim(p, 3.5);
        LivingEntity t = a.hit();
        if (t == null) {
            Wave27Util.tell(p, "Nothing within reach to grab.", ChatFormatting.GRAY);
            return;
        }
        if (Wave27Util.saves(p, t)) {
            Wave27Util.tell(p, "It wrenches free of your grip.", ChatFormatting.GRAY);
            return;
        }
        Wave27Util.give(t, 80, 5, "minecraft:slowness");
        Wave27Util.give(t, 80, 0, "minecraft:weakness");
        Wave27Util.puff(p.serverLevel(), RukhFx.Alignment.LIGHT, t, 8);
        Wave27Util.tell(p, "You have " + t.getName().getString() + " in a bear's grip.", ChatFormatting.GOLD);
    }

    // ================================================================ Dragon's Fire Gauntlet
    private static Wave27Rules.Charges dragon(ServerPlayer p) {
        return Wave27Util.charges(p, "dragon", Wave27Rules.DRAGON_MAX, Wave27Rules.dawnGainD3(p.getRandom().nextInt(3)));
    }

    /** Burning Hands at the fourth level: a cone of fire five blocks long (14 fire damage, set alight); the gauntlet's charges are not spent. */
    static void burningHands(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel lv = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        for (int i = 1; i <= 5; i++) {
            Vec3 at = eye.add(look.scale(i)).add(0, -0.3, 0);
            RukhFx.spray(lv, RukhFx.Alignment.LIGHT, at.x, at.y, at.z, 4, 0.2 + 0.08 * i, 0.2, 0.2 + 0.08 * i, 0.02);
        }
        int hit = 0;
        for (LivingEntity e : Fx.foesAround(p, 5.0)) {
            if (!Wave27Rules.inCone(e.getX() - eye.x, e.getY() + e.getBbHeight() * 0.5 - eye.y, e.getZ() - eye.z, look.x, look.y, look.z, 5.5, 0.6)) continue;
            Fx.damage(p, e, 14.0f, FIRE);
            e.setRemainingFireTicks(100);
            hit++;
        }
        lv.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8f, 0.8f);
        if (hit == 0) Wave27Util.tell(p, "The fire roars out and catches nothing.", ChatFormatting.GRAY);
    }

    /**
     * The Dexterity save: when an explosion, a shot or a lightning bolt would hit the wearer, the gauntlet spends a charge and the blow is turned aside
     * whole. Three charges; 1d3 come back at each dawn. At most once a second.
     */
    static void dragonSave(ServerPlayer v, LivingIncomingDamageEvent e) {
        DamageSource src = e.getSource();
        if (!(src.is(DamageTypeTags.IS_EXPLOSION) || src.is(DamageTypeTags.IS_PROJECTILE) || src.is(DamageTypeTags.IS_LIGHTNING))) return;
        CompoundTag d = Wave27Util.data(v);
        long now = v.level().getGameTime();
        if (now < d.getLong("dragon_save_at")) return;
        Wave27Rules.Charges c = dragon(v);
        if (!c.canSpend(1)) return;
        d.putLong("dragon_save_at", now + 20);
        Wave27Util.store(v, "dragon", c.spend(1));
        e.setCanceled(true);
        RukhFx.burst(v.serverLevel(), v, RukhFx.Alignment.LIGHT, 8);
        Wave27Util.tell(v, "The gauntlet moves your hand: saved (" + (c.left() - 1) + " of " + Wave27Rules.DRAGON_MAX + " charges left).", ChatFormatting.GOLD);
    }

    // ================================================================ Gauntlet Mimic
    /** Shapechanger: the gauntlet turns from bludgeoning to piercing to slashing. */
    static void mimicShape(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag d = Wave27Util.data(p);
        Wave27Rules.Shape next = Wave27Rules.Shape.of(d.getInt("mimic_shape")).next();
        d.putInt("mimic_shape", next.ordinal());
        Wave27Util.tell(p, "The mimic shifts: " + next.name().toLowerCase() + ".", ChatFormatting.LIGHT_PURPLE);
    }

    // ================================================================ Gauntlet of the Stormcaller
    private static Wave27Rules.Charges storm(ServerPlayer p) {
        return Wave27Util.charges(p, "storm", Wave27Rules.STORM_MAX, Wave27Rules.dawnGainD4p1(p.getRandom().nextInt(4)));
    }

    private static boolean spend(ServerPlayer p, int n) {
        Wave27Rules.Charges c = storm(p);
        if (!c.canSpend(n)) {
            Wave27Util.tell(p, "The gems are dark: " + c.left() + " of " + Wave27Rules.STORM_MAX + " charges.", ChatFormatting.GRAY);
            return false;
        }
        Wave27Util.store(p, "storm", c.spend(n));
        return true;
    }

    /** Thunderwave (1 charge): a boom that throws every foe within four blocks back and hurts it (6 lightning damage). */
    static void thunderwave(ActCtx c) {
        ServerPlayer p = c.player();
        if (!spend(p, Wave27Rules.THUNDERWAVE_COST)) return;
        ServerLevel lv = p.serverLevel();
        for (LivingEntity t : Fx.foesAround(p, 4.0)) {
            Fx.damage(p, t, 6.0f, LIGHTNING);
            t.knockback(1.2, p.getX() - t.getX(), p.getZ() - t.getZ());
        }
        lv.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8f, 1.5f);
        Fx.ring(lv, p.position().add(0, 0.5, 0), 4.0, 0x58C8FF);
    }

    /** Call Lightning (3 charges): a bolt falls where you look (up to 24 blocks) and burns what stands within three blocks of it (12 lightning damage). */
    static void callLightning(ActCtx c) {
        ServerPlayer p = c.player();
        Wave27Util.Aim a = Wave27Util.aim(p, 24.0);
        if (!spend(p, Wave27Rules.LIGHTNING_COST)) return;
        ServerLevel lv = p.serverLevel();
        Vec3 at = a.hit() != null ? a.hit().position() : a.end();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(lv);
        if (bolt != null) {
            bolt.moveTo(at.x, at.y, at.z);
            bolt.setVisualOnly(true);                 // the world is never set alight or changed: damage is done by the relic
            lv.addFreshEntity(bolt);
        }
        for (LivingEntity t : Wave27Util.near(lv, at, 3.0, e -> Wave27Util.target(p, e))) Fx.damage(p, t, 12.0f, LIGHTNING);
        Fx.ring(lv, at.add(0, 0.3, 0), 3.0, 0x58C8FF);
    }

    /** Shocking Grasp: a touch (3.5 blocks) that shocks for 5 lightning damage, 2 more against a target in heavy armour; it works even while stunned. */
    static void shockingGrasp(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity t = Wave27Util.aim(p, 3.5).hit();
        if (t == null) {
            Wave27Util.tell(p, "Nothing within reach to touch.", ChatFormatting.GRAY);
            return;
        }
        shock(p, t);
    }

    private static void shock(ServerPlayer p, LivingEntity t) {
        Fx.damage(p, t, t.getArmorValue() >= 8 ? 7.0f : 5.0f, LIGHTNING);
        Wave27Util.give(t, 20, 1, "minecraft:slowness");
        Wave27Util.puff(p.serverLevel(), RukhFx.Alignment.LIGHT, t, 8);
    }

    /** Once a second for a wearer: arcs run through the metal in battle; while stunned the gauntlet shocks the nearest foe by itself (every three seconds). */
    static void stormPass(ServerPlayer p) {
        List<LivingEntity> foes = Fx.foesAround(p, 10.0);
        if (foes.isEmpty()) return;
        RukhFx.aura(p.serverLevel(), p, RukhFx.Alignment.LIGHT);
        boolean stunned = Wave27Rules.stunned(Wave27Util.amp(p, STUN[0]) >= 0, Wave27Util.amp(p, STUN[1]) >= 0, Wave27Util.amp(p, STUN[2]) >= 0, Wave27Util.amp(p, "minecraft:slowness"));
        if (!stunned) return;
        CompoundTag d = Wave27Util.data(p);
        long now = p.level().getGameTime();
        if (now < d.getLong("storm_zap_at")) return;
        for (LivingEntity f : foes) {
            if (f.distanceToSqr(p) <= 9.0) {
                d.putLong("storm_zap_at", now + 60);
                shock(p, f);
                Wave27Util.tell(p, "Stunned, your gauntlet shocks " + f.getName().getString() + " for you.", ChatFormatting.AQUA);
                return;
            }
        }
    }

    // ================================================================ Glove of Appreciation
    private static CompoundTag givers(CompoundTag d) {
        if (!d.contains("glove_from", Tag.TAG_COMPOUND)) d.put("glove_from", new CompoundTag());
        return d.getCompound("glove_from");
    }

    /** A kindness from this giver (a player's compliment, an animal won over): one charge, if the gap has passed and the glove is not full. */
    static void appreciate(ServerPlayer wearer, UUID giver, String why) {
        CompoundTag d = Wave27Util.data(wearer);
        CompoundTag from = givers(d);
        long now = wearer.level().getGameTime();
        String key = giver.toString();
        if (!Wave27Rules.giftAccepted(d.getInt("glove_n"), from.getLong(key), now)) return;
        for (String k : new ArrayList<>(from.getAllKeys())) if (now - from.getLong(k) > Wave27Rules.GLOVE_GAP * 3L) from.remove(k);
        from.putLong(key, now);
        d.putInt("glove_n", Math.min(Wave27Rules.GLOVE_MAX, d.getInt("glove_n") + 1));
        Wave27Util.tell(wearer, why + " The glove glows warmer (" + d.getInt("glove_n") + " of " + Wave27Rules.GLOVE_MAX + ").", ChatFormatting.GOLD);
        RukhFx.burst(wearer.serverLevel(), wearer, RukhFx.Alignment.LIGHT, 6);
    }

    /**
     * High-Five (1 charge): say "Thank you!" and the glove flies up to twelve blocks to the creature you look at and high-fives it. A friend (a
     * player, a villager, a pet) is mended (6 health, a few seconds of Regeneration). An unwilling creature gets a wisdom save (its existence); if it
     * fails it is, against its will, high-fived: it forgets what it was doing and staggers (Slowness II, three seconds).
     */
    static void highFive(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag d = Wave27Util.data(p);
        if (d.getInt("glove_n") < 1) {
            Wave27Util.tell(p, "The glove has no charge: be complimented.", ChatFormatting.GRAY);
            return;
        }
        LivingEntity t = Wave27Util.aim(p, 12.0, e -> e != p && e.isAlive() && !e.isSpectator()).hit();
        if (t == null) {
            Wave27Util.tell(p, "Nobody there to high-five.", ChatFormatting.GRAY);
            return;
        }
        d.putInt("glove_n", d.getInt("glove_n") - 1);
        ServerLevel lv = p.serverLevel();
        RukhFx.stream(lv, p, t, RukhFx.Alignment.LIGHT, 4);
        p.sendSystemMessage(net.minecraft.network.chat.Component.literal("Thank you!").withStyle(ChatFormatting.GOLD));
        if (Wave27Util.friend(p, t) || !Wave27Util.hostile(p, t)) {
            t.heal(6.0f);
            Wave27Util.give(t, 100, 0, "minecraft:regeneration");
            Wave27Util.puff(lv, RukhFx.Alignment.LIGHT, t, 10);
            Wave27Util.tell(p, "High-five with " + t.getName().getString() + "!", ChatFormatting.GOLD);
        } else if (Wave27Util.saves(p, t)) {
            Wave27Util.tell(p, t.getName().getString() + " refuses the high-five.", ChatFormatting.GRAY);
        } else {
            if (t instanceof Mob m) m.setTarget(null);
            Wave27Util.give(t, 60, 1, "minecraft:slowness");
            Wave27Util.puff(lv, RukhFx.Alignment.LIGHT, t, 10);
            Wave27Util.tell(p, t.getName().getString() + " is high-fived against its will.", ChatFormatting.GOLD);
        }
    }

    // ================================================================ Gloves of Opportunity
    /**
     * The critical 1: five in a hundred melee blows of a creature that holds something in its hands go wide, and the gloves lift what it holds
     * (a reaction, at most once every five seconds; never from a boss or another player). Called with the incoming damage of a blow on the wearer.
     */
    static void opportunity(ServerPlayer v, LivingIncomingDamageEvent e) {
        DamageSource src = e.getSource();
        if (!(src.getEntity() instanceof Mob attacker) || src.getDirectEntity() != attacker || !Wave27Util.holdsSomething(attacker)) return;
        if (attacker.getMaxHealth() >= 150f || !Wave27Rules.fumbles(v.getRandom().nextInt(100))) return;
        CompoundTag d = Wave27Util.data(v);
        long now = v.level().getGameTime();
        if (now < d.getLong("opp_at")) return;
        d.putLong("opp_at", now + Wave27Rules.FUMBLE_GAP);
        e.setCanceled(true);
        ItemStack got = Wave27Util.steal(v, attacker);
        RukhFx.burst(v.serverLevel(), attacker, RukhFx.Alignment.LIGHT, 8);
        Wave27Util.tell(v, got.isEmpty() ? "It swings wide!" : "It swings wide, and your gloves take " + got.getHoverName().getString() + "!", ChatFormatting.AQUA);
    }

    /** Lift: the gloves reach (4 blocks) and take what the creature in front holds, as the critical 1 would let them. Never from a boss or a player. */
    static void lift(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity t = Wave27Util.aim(p, 4.0, e -> Wave27Util.softMob(p, e) && Wave27Util.holdsSomething(e)).hit();
        if (t == null) {
            Wave27Util.tell(p, "Nothing in reach holds anything to take.", ChatFormatting.GRAY);
            return;
        }
        if (!Wave27Rules.steals(Wave27Rules.stealChance(t.getMaxHealth()), p.getRandom().nextInt(100))) {
            notice(p, t, "It snatched its hand away.");
            return;
        }
        ItemStack got = Wave27Util.steal(p, t);
        Wave27Util.tell(p, got.isEmpty() ? "Nothing to take." : "Lifted: " + got.getHoverName().getString() + ".", ChatFormatting.AQUA);
    }

    // ================================================================ Goristro Charger
    /**
     * Demonic Charge (3 times a day, costs existence): rush the creature you look at (up to seven blocks) and strike it with the weight of a goristro
     * for 16 extra damage and a heavy knock back.
     */
    static void demonicCharge(ActCtx c) {
        ServerPlayer p = c.player();
        Wave27Util.Aim a = Wave27Util.aim(p, Wave27Rules.GORISTRO_RANGE);
        LivingEntity t = a.hit();
        if (t == null) {
            Wave27Util.tell(p, "No target within seven blocks.", ChatFormatting.GRAY);
            return;
        }
        ServerLevel lv = p.serverLevel();
        Vec3 from = p.position();
        Vec3 away = from.subtract(t.position());
        away = new Vec3(away.x, 0, away.z);
        away = away.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : away.normalize();
        Vec3[] tries = {away, away.yRot((float) (Math.PI / 2)), away.yRot((float) (-Math.PI / 2)), away.reverse()};
        for (Vec3 dir : tries) {
            Vec3 spot = t.position().add(dir.scale(t.getBbWidth() * 0.5 + p.getBbWidth() * 0.5 + 0.4));
            AABB box = p.getBoundingBox().move(spot.x - p.getX(), spot.y - p.getY(), spot.z - p.getZ());
            if (lv.noCollision(p, box)) {
                p.teleportTo(spot.x, spot.y, spot.z);
                break;
            }
        }
        Fx.damageHeavy(p, t, Wave27Rules.GORISTRO_CHARGE_DAMAGE, HIT);
        t.knockback(1.6, p.getX() - t.getX(), p.getZ() - t.getZ());
        RukhFx.stream(lv, from.add(0, 1, 0), p.position().add(0, 1, 0), RukhFx.Alignment.DARK, 5);
        RukhFx.burst(lv, t, RukhFx.Alignment.DARK, 12);
        lv.playSound(null, p.blockPosition(), SoundEvents.RAVAGER_ATTACK, SoundSource.PLAYERS, 0.9f, 0.7f);
    }

    // ================================================================ bare-handed blows (the mimic, the goristro bones)
    /** A player's blow with nothing in the hand: the mimic's shape and glue, the goristro's weight. Called with the incoming damage of a player attack. */
    static void bareBlow(ServerPlayer p, LivingEntity victim, DamageSource src, LivingIncomingDamageEvent e) {
        if (src.getDirectEntity() != p || !src.is(DamageTypes.PLAYER_ATTACK) || !p.getMainHandItem().isEmpty()) return;
        ServerLevel lv = p.serverLevel();
        float add = 0f;
        if (RelicFlags.has(p, Wave27Rules.F_GORISTRO)) {
            add += Wave27Rules.GORISTRO_BONUS;
            Wave27Util.puff(lv, RukhFx.Alignment.DARK, victim, 3);
        }
        if (RelicFlags.has(p, Wave27Rules.F_MIMIC)) {
            CompoundTag d = Wave27Util.data(p);
            Wave27Rules.Shape shape = Wave27Rules.Shape.of(d.getInt("mimic_shape"));
            add += Wave27Rules.mimicDamage(shape, victim.getArmorValue());
            if (shape == Wave27Rules.Shape.BLUDGEONING) victim.knockback(0.6, p.getX() - victim.getX(), p.getZ() - victim.getZ());
            if (shape == Wave27Rules.Shape.SLASHING) Wave27Util.give(victim, 60, 0, "minecraft:poison");
            adhere(p, victim, d);
        }
        if (add > 0f) e.setAmount(e.getAmount() + add);
    }

    /** Adhesive and Hungry: the glued target is held (webbed, Slowness V for three seconds) unless its existence saves it, and the gauntlet saps two health from the wearer. */
    private static void adhere(ServerPlayer p, LivingEntity victim, CompoundTag d) {
        long now = p.level().getGameTime();
        if (now < d.getLong("mimic_glue_at") || !Wave27Rules.canSap(p.getHealth()) || victim.getMaxHealth() >= 400f) return;
        d.putLong("mimic_glue_at", now + Wave27Rules.ADHESIVE_GAP);
        if (Wave27Util.saves(p, victim)) return;
        Wave27Util.give(victim, Wave27Rules.ADHESIVE_TICKS, 4, "minecraft:slowness");
        Wave27Util.give(victim, Wave27Rules.ADHESIVE_TICKS, 0, "tensura:webbed");
        p.hurt(p.damageSources().magic(), 2.0f);
        Wave27Util.tell(p, "The mimic sticks to " + victim.getName().getString() + " and drinks two of your health.", ChatFormatting.LIGHT_PURPLE);
    }
}
