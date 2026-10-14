package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.UseRules;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The enchanted blades of wave 12: the Warmage's Longsword (five missiles a day), the Sword Crystal (a crystal arm, four charges, moonlit
 * recharge), the Bottle Blade (a stream that freezes, water to be refilled), the Stabshroom (poison, spores, natural 1 and 20) and the
 * Disintegration Dagger (a kill leaves a delayed fiery blast).
 */
final class Wave12Magic {
    private Wave12Magic() {}

    static final String WARMAGE = "warmages_longsword", CRYSTAL = "sword_crystal", BOTTLE = "bottle_blade", SHROOM = "the_stabshroom", DAGGER = "disintegration_dagger";
    static final String WARMAGE_FLAG = "w12_warmages_longsword", CRYSTAL_FLAG = "w12_sword_crystal", BOTTLE_FLAG = "w12_bottle_blade",
            SHROOM_FLAG = "w12_the_stabshroom", DAGGER_FLAG = "w12_disintegration_dagger";

    // ================================================================ Warmage's Longsword
    /** Fires one missile, or with {@code all} every missile the sword still holds, at up to three foes in front of the wielder. */
    static void missiles(ActCtx c, boolean all) {
        ServerPlayer p = c.player();
        ItemStack s = Wave12Data.live(p, WARMAGE, c.stack());
        if (s.isEmpty()) return;
        CompoundTag st = Wave12Data.stack(s);
        long today = UseRules.day(p.level().getDayTime());
        int left = Wave12Rules.missilesLeft(st.getInt("missiles"), st.getLong("day"), today, st.contains("missiles"));
        if (left <= 0) {
            Wave5Aim.msg(p, "The sword holds no missiles until dawn.", ChatFormatting.GRAY);
            return;
        }
        List<LivingEntity> targets;
        if (all) {
            targets = Wave5Aim.cone(p, 24.0, 0.8, 3);
        } else {
            LivingEntity one = Wave5Aim.pickFoe(p, 24.0);
            targets = one == null ? List.of() : List.of(one);
        }
        if (targets.isEmpty()) {
            Wave5Aim.msg(p, "No foe in your sights: the missiles stay in the blade.", ChatFormatting.GRAY);
            return;
        }
        int count = all ? left : 1;
        int[] share = Wave12Rules.spread(count, targets.size());
        ServerLevel l = p.serverLevel();
        Vec3 from = Wave5Aim.muzzle(p);
        for (int i = 0; i < targets.size(); i++) {
            if (share[i] == 0) continue;
            LivingEntity t = targets.get(i);
            float dmg = 0f;
            for (int k = 0; k < share[i]; k++) dmg += Wave12Rules.missileDamage(1 + p.getRandom().nextInt(4));   // each 1d4 + 1 force
            Wave5Aim.beam(l, from, t.getBoundingBox().getCenter(), ParticleTypes.WITCH, 14);
            Wave5Aim.puff(l, t.getBoundingBox().getCenter(), ParticleTypes.ENCHANTED_HIT, 6 + 2 * share[i], 0.3);
            Wave0Util.extra(p, t, dmg, "minecraft:magic");                              // one blow per foe: the core caps it for players
        }
        Wave5Aim.sound(l, p.position(), SoundEvents.EVOKER_CAST_SPELL, 0.8f, 1.5f);
        st.putInt("missiles", left - count);
        st.putLong("day", today);
        Wave12Data.save(s, st);
        Wave5Aim.msg(p, "Arcane Barrage: " + (left - count) + "/" + Wave12Rules.MISSILES_PER_DAY + " missiles left.", ChatFormatting.LIGHT_PURPLE);
    }

    // ================================================================ Sword Crystal
    static boolean armActive(ServerPlayer p) {
        return Wave12Data.hasPlayerData(p) && Wave12Data.player(p).getLong("crystal_until") > p.level().getGameTime();
    }

    static void prick(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack s = Wave12Data.live(p, CRYSTAL, c.stack());
        if (s.isEmpty()) return;
        if (armActive(p)) {
            Wave5Aim.msg(p, "Your arm is already crystal.", ChatFormatting.AQUA);
            return;
        }
        CompoundTag st = Wave12Data.stack(s);
        int charges = st.contains("charges") ? st.getInt("charges") : Wave12Rules.CRYSTAL_CHARGES;
        if (charges <= 0) {
            Wave5Aim.msg(p, "No charges: the crystal drinks one back from each night's moonlight.", ChatFormatting.GRAY);
            return;
        }
        st.putInt("charges", charges - 1);
        if (!st.contains("night")) st.putLong("night", Wave12Rules.nightIndex(p.level().getDayTime()));
        Wave12Data.save(s, st);
        CompoundTag d = Wave12Data.player(p);
        d.putLong("crystal_until", p.level().getGameTime() + Wave12Rules.CRYSTAL_TICKS);
        d.putFloat("crystal_hp", Wave12Rules.CRYSTAL_HP);
        if (p.getHealth() > 3f) p.hurt(p.damageSources().generic(), 1.0f);               // the prick of the finger
        ServerLevel l = p.serverLevel();
        Fx.burst(l, p.position(), 0x7FD8FF);
        Wave5Aim.puff(l, p.position().add(0, 1.0, 0), ParticleTypes.SNOWFLAKE, 24, 0.5);
        Wave5Aim.sound(l, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 0.7f);
        Wave5Aim.msg(p, "Stay cool, Bird Boy. Crystal arm: " + Math.round(Wave12Rules.CRYSTAL_HP) + " HP, " + (charges - 1) + "/" + Wave12Rules.CRYSTAL_CHARGES + " charges left.", ChatFormatting.AQUA);
    }

    static void melt(ActCtx c) {
        ServerPlayer p = c.player();
        if (!armActive(p)) {
            Wave5Aim.msg(p, "Your arm is flesh and blood.", ChatFormatting.GRAY);
            return;
        }
        endArm(p, false);
    }

    static void endArm(ServerPlayer p, boolean shattered) {
        CompoundTag d = Wave12Data.player(p);
        d.putLong("crystal_until", 0);
        d.putFloat("crystal_hp", 0f);
        ServerLevel l = p.serverLevel();
        if (shattered) {
            float back = Wave12Rules.shatterBackfire(1 + p.getRandom().nextInt(4), 1 + p.getRandom().nextInt(4));
            float safe = Wave12Rules.safeBackfire(back, p.getHealth());
            if (safe > 0f) p.hurt(p.damageSources().freeze(), safe);                       // 2d4 of cold, never a killing one
            Wave5Aim.puff(l, p.position().add(0, 1.0, 0), ParticleTypes.SNOWFLAKE, 40, 0.8);
            Wave5Aim.sound(l, p.position(), SoundEvents.GLASS_BREAK, 1.0f, 0.8f);
            Wave5Aim.msg(p, "The crystal arm shatters!", ChatFormatting.RED);
        } else {
            Wave5Aim.puff(l, p.position().add(0, 1.0, 0), ParticleTypes.CLOUD, 12, 0.4);
            Wave5Aim.msg(p, "The crystal melts away.", ChatFormatting.AQUA);
        }
    }

    /** The blow of the crystal arm after it landed: cold, frost, and the arm takes the wear. */
    static void crystalAfter(ServerPlayer p, LivingEntity t) {
        Wave0Util.extra(p, t, 3.0f, "minecraft:freeze");
        Fx.addEffect(t, "tensura:frost", 60, 0);
        Fx.addEffect(t, "minecraft:slowness", 40, 0);
        Wave5Aim.puff(p.serverLevel(), t.position().add(0, t.getBbHeight() * 0.6, 0), ParticleTypes.SNOWFLAKE, 10, 0.3);
        CompoundTag d = Wave12Data.player(p);
        float hp = Wave12Rules.crystalAfterBlow(d.getFloat("crystal_hp"));
        d.putFloat("crystal_hp", hp);
        if (hp <= 0f) endArm(p, true);
    }

    /** Once a second while the sword is carried in a hand: the arm melts when its time is up, and the moon gives back a charge. */
    static void crystalTick(ServerPlayer p) {
        if (Wave12Data.hasPlayerData(p)) {
            CompoundTag d = Wave12Data.player(p);
            long until = d.getLong("crystal_until");
            if (until > 0 && until <= p.level().getGameTime()) endArm(p, false);
        }
        if ((p.tickCount / 20) % 5 != 0 || !Wave12Rules.moonlit(p.level().getDayTime()) || !p.level().canSeeSky(p.blockPosition())) return;
        ItemStack s = Wave12Data.live(p, CRYSTAL, ItemStack.EMPTY);
        if (s.isEmpty()) return;
        CompoundTag st = Wave12Data.stack(s);
        int charges = st.contains("charges") ? st.getInt("charges") : Wave12Rules.CRYSTAL_CHARGES;
        long night = Wave12Rules.nightIndex(p.level().getDayTime());
        long last = st.contains("night") ? st.getLong("night") : night - 1;
        if (!Wave12Rules.regains(charges, last, night)) return;
        st.putInt("charges", charges + 1);
        st.putLong("night", night);
        Wave12Data.save(s, st);
        Wave5Aim.puff(p.serverLevel(), p.position().add(0, 1.4, 0), ParticleTypes.END_ROD, 10, 0.4);
        Wave5Aim.msg(p, "The Sword Crystal drinks the moonlight: " + (charges + 1) + "/" + Wave12Rules.CRYSTAL_CHARGES + " charges.", ChatFormatting.AQUA);
    }

    // ================================================================ Bottle Blade
    static boolean dry(ItemStack s) { return Wave12Data.stack(s).getBoolean("dry"); }

    static void setDry(ItemStack s, boolean dry) {
        CompoundTag st = Wave12Data.stack(s);
        st.putBoolean("dry", dry);
        Wave12Data.save(s, st);
    }

    /** Shake and open: a long stream that soaks and freezes everything in a narrow cone; it empties the bottle. */
    static void stream(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack s = Wave12Data.live(p, BOTTLE, c.stack());
        if (s.isEmpty()) return;
        if (dry(s)) {
            Wave5Aim.msg(p, "The bottle is empty: sneak and use it at water to refill it.", ChatFormatting.GRAY);
            return;
        }
        List<LivingEntity> hit = Wave5Aim.cone(p, 10.0, 0.95, 4);
        if (hit.isEmpty()) {
            Wave5Aim.msg(p, "Nothing in the line of the stream.", ChatFormatting.GRAY);
            return;
        }
        ServerLevel l = p.serverLevel();
        Vec3 from = Wave5Aim.muzzle(p);
        for (LivingEntity t : hit) {
            Wave5Aim.beam(l, from, t.getBoundingBox().getCenter(), ParticleTypes.SPLASH, 16);
            Wave0Util.extra(p, t, 5.0f, "minecraft:freeze");
            Fx.addEffect(t, "minecraft:slowness", 60, 1);
            Fx.addEffect(t, "tensura:frost", 60, 0);
            Wave5Aim.puff(l, t.position().add(0, t.getBbHeight() * 0.6, 0), ParticleTypes.SNOWFLAKE, 10, 0.3);
        }
        setDry(s, true);
        Wave5Aim.sound(l, p.position(), SoundEvents.PLAYER_SPLASH, 1.0f, 0.8f);
        Wave5Aim.msg(p, "The stream freezes " + hit.size() + (hit.size() == 1 ? " foe" : " foes") + ". The bottle is empty.", ChatFormatting.AQUA);
    }

    /** Control Water: with an empty bottle at water, refills it; with a full one, throws a wave round you (and empties it). */
    static void control(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack s = Wave12Data.live(p, BOTTLE, c.stack());
        if (s.isEmpty()) return;
        ServerLevel l = p.serverLevel();
        if (dry(s)) {
            if (!waterAtHand(p)) {
                Wave5Aim.msg(p, "No water in reach: stand in it, in rain, or look at a pool.", ChatFormatting.GRAY);
                return;
            }
            setDry(s, false);
            Wave5Aim.sound(l, p.position(), SoundEvents.BOTTLE_FILL, 1.0f, 1.0f);
            Wave5Aim.msg(p, "The bottle fills from the water.", ChatFormatting.AQUA);
            return;
        }
        Fx.ring(l, p.position(), 5.0, 0x3FB8E0);
        for (LivingEntity t : Fx.foesAround(p, 5.0)) {
            Wave0Util.extra(p, t, 2.0f, "minecraft:drown");
            t.knockback(1.1, p.getX() - t.getX(), p.getZ() - t.getZ());
            Fx.addEffect(t, "minecraft:slowness", 60, 1);
        }
        setDry(s, true);                                                                // "the sword cannot be used until the bottle is refilled"
        Wave5Aim.sound(l, p.position(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.0f, 0.7f);
        Wave5Aim.msg(p, "Control Water: the bottle's water is spent.", ChatFormatting.AQUA);
    }

    private static boolean waterAtHand(ServerPlayer p) {
        if (p.isInWater() || p.level().isRainingAt(p.blockPosition())) return true;
        Vec3 eye = p.getEyePosition();
        BlockHitResult r = p.level().clip(new ClipContext(eye, eye.add(p.getLookAngle().scale(6.0)), ClipContext.Block.COLLIDER, ClipContext.Fluid.SOURCE_ONLY, p));
        if (r.getType() == HitResult.Type.MISS) return false;
        BlockPos at = r.getBlockPos();
        return p.level().getFluidState(at).is(FluidTags.WATER);
    }

    // ================================================================ The Stabshroom
    /** The d20 of the card: spores on a 1, a double blow on a 20. Returns the new damage amount. */
    static float shroomRoll(ServerPlayer p, LivingEntity t, float amount) {
        // a half-swing is no real attack roll. The swing timer is already spent when the damage event fires, so the strength is read off the blow itself:
        // vanilla scales the attack damage by 0.2 + 0.8 * strength squared, which is about 0.85 at the 0.9 strength a full swing needs
        if (amount < 0.8f * (float) p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) return amount;
        int d20 = 1 + p.getRandom().nextInt(20);
        if (Wave12Rules.critical(d20)) {
            Fx.addEffect(t, "minecraft:poison", 400, 0);                                // "poisoned": a long poison and a weakened foe
            Fx.addEffect(t, "minecraft:weakness", 400, 0);
            Wave5Aim.puff(p.serverLevel(), t.position().add(0, t.getBbHeight() * 0.6, 0), ParticleTypes.HAPPY_VILLAGER, 14, 0.4);
            Wave5Aim.msg(p, "Natural 20: a deep stab, and the poison takes hold.", ChatFormatting.GREEN);
            return amount * 2.0f;
        }
        if (Wave12Rules.fumble(d20)) spores(p);
        return amount;
    }

    /** Natural 1: a burst of spores round the wielder; everyone in melee range is poisoned and blinded for a moment. */
    private static void spores(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        Wave5Aim.puff(l, p.position().add(0, 1.0, 0), ParticleTypes.SPORE_BLOSSOM_AIR, 40, 1.0);
        Wave5Aim.puff(l, p.position().add(0, 1.0, 0), ParticleTypes.MYCELIUM, 20, 1.0);
        Fx.addEffect(p, "minecraft:poison", 40, 0);
        Fx.addEffect(p, "minecraft:blindness", 30, 0);
        for (LivingEntity t : Fx.foesAround(p, 2.0)) {
            Fx.addEffect(t, "minecraft:poison", 60, 0);
            Fx.addEffect(t, "minecraft:blindness", 30, 0);
        }
        Wave5Aim.msg(p, "Natural 1: the Stabshroom bursts into spores!", ChatFormatting.DARK_GREEN);
    }

    /** Every blow carries the poison of the card (1d4). */
    static void shroomAfter(ServerPlayer p, LivingEntity t) {
        Wave0Util.extra(p, t, 2.5f, "minecraft:magic");
        Fx.addEffect(t, "minecraft:poison", 40, 0);
    }

    // ================================================================ Disintegration Dagger
    /** A fatal strike: after a short telegraphed moment the body blows apart. */
    static void daggerKill(ServerPlayer p, LivingEntity dead) {
        CompoundTag d = Wave12Data.player(p);
        long now = p.level().getGameTime();
        if (d.getLong("dagger_at") > now) return;                                      // one blast at a time: no chains
        d.putLong("dagger_at", now + Wave12Rules.BLAST_DELAY);
        ServerLevel l = p.serverLevel();
        Vec3 at = dead.position().add(0, dead.getBbHeight() * 0.5, 0);
        Wave5Aim.puff(l, at, ParticleTypes.ASH, 30, 0.5);                              // the body starts to crumble
        Wave5Aim.puff(l, at, ParticleTypes.SMOKE, 14, 0.4);
        java.util.UUID who = p.getUUID();
        SpellRuntime.later(l, Wave12Rules.BLAST_DELAY, () -> blast(l, who, at));
    }

    private static void blast(ServerLevel l, java.util.UUID who, Vec3 at) {
        ServerPlayer p = l.getServer().getPlayerList().getPlayer(who);
        if (p == null || !p.isAlive() || p.level() != l) return;
        l.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 3, 0.5, 0.4, 0.5, 0.0);
        l.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 40, 1.2, 0.6, 1.2, 0.08);
        l.sendParticles(ParticleTypes.LAVA, at.x, at.y, at.z, 10, 1.0, 0.4, 1.0, 0.0);
        Wave5Aim.sound(l, at, SoundEvents.GENERIC_EXPLODE.value(), 0.7f, 1.3f);
        for (LivingEntity t : Wave5Aim.foesNear(p, at, Wave12Rules.BLAST_RADIUS)) {
            double dist = t.getBoundingBox().getCenter().distanceTo(at);
            Wave0Util.extra(p, t, Wave12Rules.blastDamage(dist), "minecraft:in_fire");
            t.setRemainingFireTicks(Math.max(t.getRemainingFireTicks(), 60));
        }
    }
}
