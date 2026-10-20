package com.newuniverse.nusmp.relics.wave;


import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicEngine;
import com.newuniverse.nusmp.relics.RelicFlags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.FxRelic;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Wave 15, the bracers and bracelets and the Crest of Foresight: Phasing, Initiative, the Phantom's Hand, Manhandling, the Arcane Blades, the Bear
 * Paw, the Faithful Friendship bond and the Crest's foresight. One static method per ability; the damage-time parts are called from
 * {@link Wave15Events}.
 */
final class Wave15Hands {
    private Wave15Hands() {}

    private static final String ARMOR = "minecraft:generic.armor";

    // ================================================================ Bracers of Phasing
    static void phase(ActCtx c) {
        if (Wave15Field.startPhase(c.player())) Wave15Util.tell(c.player(), "You phase out: thirty seconds to walk through walls.", ChatFormatting.GOLD);
    }

    // ================================================================ Bracer of Initiative
    /** Roll Initiative: eight seconds of Speed II and Haste II. */
    static void rollInitiative(ActCtx c) {
        ServerPlayer p = c.player();
        Wave15Util.give(p, 160, 1, "minecraft:speed");
        Wave15Util.give(p, 160, 1, "minecraft:haste");
        com.newuniverse.nusmp.particle.fx.Fx.spray(p.serverLevel(), FxThemes.RELIC_WAVE_15, FxTone.LIGHT, p.getX(), p.getY() + 1.0, p.getZ(), 16, 0.5, 0.7, 0.5, 0.08);
        Wave15Util.tell(p, "Initiative! Your hair streams in a breeze that is only for you.", ChatFormatting.AQUA);
    }

    /** The first blow on a creature that is not yet fighting you lands +3 harder. Called with the incoming damage of a hit by the wearer. */
    static void ambush(ServerPlayer p, LivingEntity victim, LivingIncomingDamageEvent e) {
        if (victim instanceof Mob m && m.getTarget() != p) e.setAmount(e.getAmount() + 3.0f);
    }

    // ================================================================ Phantom's Bracelet
    /** The hand goes through the wall: the first container, lever or button within six blocks along the look is worked, if the coin lands heads. */
    static void ghostHand(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel lv = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        BlockPos found = null;
        for (double d = 0.5; d <= 6.0 && found == null; d += 0.5) {
            BlockPos bp = BlockPos.containing(eye.add(look.scale(d)));
            if (reachable(lv, bp, lv.getBlockState(bp)) && lv.mayInteract(p, bp)) found = bp;      // spawn protection and the border still apply
        }
        if (found == null) {
            Wave15Util.tell(p, "Your hand finds nothing to touch.", ChatFormatting.GRAY);
            return;
        }
        Wave15Util.trail(lv, eye.add(0, -0.4, 0), Vec3.atCenterOf(found), Wave15Util.dust(0xC8A8F0, 1.0f));
        if (!Wave15Rules.coin(p.getRandom().nextInt())) {
            Wave15Util.tell(p, "Tails: your ghostly fingers slip over it.", ChatFormatting.GRAY);
            return;
        }
        BlockState st = lv.getBlockState(found);
        st.useWithoutItem(lv, p, new BlockHitResult(Vec3.atCenterOf(found), Direction.UP, found, false));
        Wave15Util.tell(p, "Heads: your hand closes on it through the stone.", ChatFormatting.LIGHT_PURPLE);
    }

    private static boolean reachable(ServerLevel lv, BlockPos bp, BlockState st) {
        if (st.isAir()) return false;
        if (st.getBlock() instanceof ButtonBlock || st.getBlock() instanceof LeverBlock || st.is(Blocks.ENDER_CHEST)) return true;
        return lv.getBlockEntity(bp) instanceof Container;
    }

    // ================================================================ Bracers of Manhandling
    /** Seize and throw: a creature within reach is lifted and flung along the look; small ones go twice as far, large ones half, giants not at all. */
    static void seizeAndThrow(ActCtx c) {
        ServerPlayer p = c.player();
        Wave15Util.Aim a = Wave15Util.aim(p, 3.5);
        LivingEntity t = a.hit();
        if (t == null) {
            Wave15Util.tell(p, "Nothing within reach to seize.", ChatFormatting.GRAY);
            return;
        }
        double mine = p.getBbWidth() * p.getBbWidth() * p.getBbHeight();
        double theirs = t.getBbWidth() * t.getBbWidth() * t.getBbHeight();
        double power = t.getMaxHealth() > 250f ? 0.0 : Wave15Rules.throwPower(theirs / Math.max(0.1, mine));
        if (power <= 0.0) {
            Wave15Util.tell(p, "Far too big to lift.", ChatFormatting.GRAY);
            return;
        }
        if (Wave15Util.saves(p, t)) {
            Wave15Util.tell(p, "It wrenches free of your grip.", ChatFormatting.GRAY);
            return;
        }
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0e-4 ? Vec3.directionFromRotation(0, p.getYRot()) : flat.normalize();
        Vec3 v = flat.scale(Wave15Rules.THROW_SPEED * power).add(0, Wave15Rules.THROW_LIFT, 0);
        t.setDeltaMovement(v);
        t.hurtMarked = true;
        t.hasImpulse = true;
        t.fallDistance = 0f;
        Wave15Field.thrown(p, t);
        Wave15Util.trail(p.serverLevel(), t.position().add(0, 0.5, 0), t.position().add(0, 0.5, 0).add(v.scale(3)), com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_15, FxTone.LIGHT));
        Wave15Util.tell(p, "YEET.", ChatFormatting.AQUA);
    }

    // ================================================================ Bracers of Arcane Blades
    /** Two daggers of arcane energy fly from the bracers: force damage that ignores armour, the second at a neighbour if there is one. */
    static void throwDaggers(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel lv = p.serverLevel();
        Wave15Util.Aim a = Wave15Util.aim(p, 24.0);
        Vec3 look = p.getLookAngle();
        Vec3 side = look.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0e-4 ? new Vec3(0.35, 0, 0) : side.normalize().scale(0.35);
        Vec3 hand = p.getEyePosition().add(look.scale(0.5)).add(0, -0.35, 0);
        var glow = Wave15Util.dust(0x9A7CFF, 1.0f);
        Wave15Util.trail(lv, hand.add(side), a.end(), glow);
        Wave15Util.trail(lv, hand.subtract(side), a.end(), com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_15, FxTone.LIGHT));
        LivingEntity first = a.hit();
        if (first == null) return;
        LivingEntity second = null;
        for (LivingEntity e : Wave15Util.near(lv, first.position(), 4.0, x -> x != first && Wave15Util.target(p, x))) { second = e; break; }
        float dmg = second == null ? 5.0f : 3.5f;
        Fx.damage(p, first, dmg, "minecraft:magic");
        if (second != null) {
            Fx.damage(p, second, dmg, "minecraft:magic");
            Wave15Util.trail(lv, first.position().add(0, 1, 0), second.position().add(0, 1, 0), glow);
        }
    }

    // ================================================================ Bear Bracer
    static void bearPaw(ActCtx c) {
        Wave15Util.tell(c.player(), c.isOn("bear_paw") ? "Your hand becomes a bear's paw." : "Your hand is a hand again.", ChatFormatting.GOLD);
    }

    static void bladeMode(ActCtx c) {
        Wave15Util.tell(c.player(), c.isOn("blade_mode") ? "A dagger of arcane energy hums in each fist." : "The arcane daggers fade.", ChatFormatting.LIGHT_PURPLE);
    }

    /** Bare-handed blows: +3 with the bear paw (claws), +3 with the arcane daggers (force). Called with the incoming damage of a player attack. */
    static void bareBonus(ServerPlayer p, LivingEntity victim, DamageSource src, LivingIncomingDamageEvent e) {
        if (src.getDirectEntity() != p || !src.is(DamageTypes.PLAYER_ATTACK) || !p.getMainHandItem().isEmpty()) return;
        float add = 0f;
        if (RelicFlags.has(p, Wave15Rules.F_BEAR) && RelicEngine.isOn(p, Wave15Rules.ID_BEAR, "bear_paw")) {
            add += 3.0f;
            com.newuniverse.nusmp.particle.fx.Fx.spray(p.serverLevel(), FxThemes.RELIC_WAVE_15, FxTone.LIGHT, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(), 1, 0, 0, 0, 0);
        }
        if (RelicFlags.has(p, Wave15Rules.F_BLADES) && RelicEngine.isOn(p, Wave15Rules.ID_BLADES, "blade_mode")) {
            add += 3.0f;
            com.newuniverse.nusmp.particle.fx.Fx.spray(p.serverLevel(), FxThemes.RELIC_WAVE_15, Wave15Util.align(0x9A7CFF), victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(), 6, 0.2, 0.3, 0.2, 0.0);
        }
        if (add > 0f) e.setAmount(e.getAmount() + add);
    }

    // ================================================================ Bracelets of Faithful Friendship
    /** Binds the creature you look at (a player, or a pet of yours) as the other wearer of the pair. */
    static void bind(ActCtx c) {
        ServerPlayer p = c.player();
        Wave15Util.Aim a = Wave15Util.aim(p, 6.0, e -> e != p && e.isAlive() && !e.isSpectator()
                && (e instanceof ServerPlayer || (e instanceof OwnableEntity o && p.getUUID().equals(o.getOwnerUUID()))));
        if (a.hit() == null) {
            Wave15Util.tell(p, "Look at a friend (a player, or a pet of yours) within six blocks.", ChatFormatting.GRAY);
            return;
        }
        Wave15Util.data(p).putUUID("friend", a.hit().getUUID());
        Wave15Util.trail(p.serverLevel(), p.getEyePosition().add(0, -0.4, 0), a.hit().position().add(0, 1, 0), Wave15Util.dust(0x2FB5B0, 1.0f));
        Wave15Util.tell(p, "Bound to " + a.hit().getName().getString() + ".", ChatFormatting.AQUA);
        if (a.hit() instanceof ServerPlayer o) Wave15Util.tell(o, p.getName().getString() + " has bound their bracelet to you.", ChatFormatting.AQUA);
    }

    static void unbind(ActCtx c) {
        Wave15Util.data(c.player()).remove("friend");
        Wave15Util.tell(c.player(), "The bond is let go.", ChatFormatting.GRAY);
    }

    /** Once a second: while the bound friend is within ten blocks, both of you carry a little Resistance, and the bond is remembered for the damage handler. */
    static void friendsPass(ServerPlayer p) {
        CompoundTag d = Wave15Util.peek(p);
        if (d == null || !d.hasUUID("friend")) return;
        UUID fid = d.getUUID("friend");
        Entity f = p.serverLevel().getEntity(fid);
        long now = p.level().getGameTime();
        if (!(f instanceof LivingEntity le) || !le.isAlive() || !Wave15Rules.inBond(p.distanceToSqr(le))) return;
        Wave15Field.bond(fid, p.getUUID(), now + 60);
        Wave15Util.giveQuiet(p, 100, 0, "minecraft:resistance");
        Wave15Util.giveQuiet(le, 100, 0, "minecraft:resistance");
        if ((p.tickCount / 20) % 3 == 0) Wave15Util.trail(p.serverLevel(), p.position().add(0, 1.2, 0), le.position().add(0, 1.2, 0), Wave15Util.dust(0x2FB5B0, 0.8f));
    }

    /** A blow that would kill one of the pair is cut to leave one hit point if the other is within ten blocks (once a day for each). */
    static void rescue(ServerPlayer victim, LivingDamageEvent.Pre e) {
        UUID partnerId = null;
        long now = victim.level().getGameTime();
        if (RelicFlags.has(victim, Wave15Rules.F_FRIENDS)) {
            CompoundTag d = Wave15Util.peek(victim);
            if (d != null && d.hasUUID("friend")) partnerId = d.getUUID("friend");
        } else {
            Wave15Field.Bond b = Wave15Field.bondOf(victim.getUUID());
            if (b != null && now < b.until()) partnerId = b.wearer();
        }
        if (partnerId == null) return;
        Entity partner = victim.serverLevel().getEntity(partnerId);
        if (!(partner instanceof LivingEntity pl) || !pl.isAlive() || !Wave15Rules.inBond(victim.distanceToSqr(pl))) return;
        float hp = victim.getHealth();
        float after = Wave15Rules.stabilised(hp, e.getNewDamage());
        if (after == e.getNewDamage()) return;
        CompoundTag vd = Wave15Util.data(victim);
        long today = Wave15Rules.dayOf(victim.level().getDayTime());
        if (!Wave15Rules.dailyReady(vd.getLong("bond_save"), today)) return;
        vd.putLong("bond_save", Wave15Rules.dailyMark(today));
        e.setNewDamage(after);
        Wave15Util.give(victim, 60, 1, "minecraft:resistance");
        Wave15Util.tell(victim, "Your friend's hand pulls you back from the edge.", ChatFormatting.AQUA);
        if (pl instanceof ServerPlayer sp) Wave15Util.tell(sp, "You pulled your friend back from the edge.", ChatFormatting.AQUA);
    }

    /** A night's sleep with the bound friend close by: both wake with six extra hit points. */
    static void rested(ServerPlayer p) {
        CompoundTag d = Wave15Util.peek(p);
        if (d == null || !d.hasUUID("friend")) return;
        Entity f = p.serverLevel().getEntity(d.getUUID("friend"));
        if (!(f instanceof LivingEntity le) || !le.isAlive() || p.distanceToSqr(le) > Wave15Rules.REST_RANGE * Wave15Rules.REST_RANGE) return;
        Fx.heal(p, 6f);
        if (le instanceof ServerPlayer sp) Fx.heal(sp, 6f);
        else le.heal(6f);
        Wave15Util.tell(p, "Rested side by side: the pair wakes mended.", ChatFormatting.AQUA);
    }

    // ================================================================ Crest of Foresight
    /** The moment of foresight: ten minutes of +2 armour and twenty seconds of Tensura's future vision. */
    static void foresight(ActCtx c) {
        ServerPlayer p = c.player();
        Wave15Util.data(p).putLong("foresight_until", p.level().getGameTime() + 12000);
        Wave15Util.ensureMod(p, ARMOR, "foresight_ac", 2.0, Operation.ADD_VALUE);
        Wave15Util.give(p, 400, 0, "tensura:future_vision");
        FxRelic.ring(p.serverLevel(), p.position(), 2.5, 0xFFF0A0);
        Wave15Util.tell(p, "You have already seen this moment.", ChatFormatting.GOLD);
    }
}
