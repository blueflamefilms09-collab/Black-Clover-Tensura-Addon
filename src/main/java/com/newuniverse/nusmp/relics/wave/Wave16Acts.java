package com.newuniverse.nusmp.relics.wave;


import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import com.newuniverse.nusmp.particle.fx.FxRelic;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Wave 16: the act bodies of the necklace pieces and the single-purpose charms (Sigil, Clasp, Po'eur, Handgrenade, Doll, Perfume, Sunscreen, Coin, Idol,
 * Dryad's band). The engine has already paid the cost and set the cooldown when a body runs. The portable god, the chalks, the satchel, the swarms and
 * the wallet have their own classes.
 */
final class Wave16Acts {
    private Wave16Acts() {}

    // ================================================================ Sigil of the Faint Flame
    /** Faint Flame: a ring of fire around the wearer, foes burn, allies are warded. */
    static void sigilFlame(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel lv = p.serverLevel();
        FxRelic.ring(lv, p.position(), 4.0, 0xFF8A2A);
        for (LivingEntity t : Fx.foesAround(p, 4.0)) {
            Fx.damage(p, t, 5f, "minecraft:in_fire");
            t.setRemainingFireTicks(60);
        }
        Fx.addEffect(p, "minecraft:fire_resistance", 400, 0);
        Fx.lendSkill(p, "tensura:flame_attack_resistance", 400);
        Fx.say(p, "The faint flame answers: a ring of fire, and a warmth that cannot be burned.");
    }

    /** Sunlit Warmth: the cold lets go, and a little healing comes with the light. */
    static void sigilWarmth(ActCtx c) {
        ServerPlayer p = c.player();
        p.setTicksFrozen(0);
        Wave6Kit.lift(p, "minecraft:slowness");
        Fx.addEffect(p, "minecraft:regeneration", 160, 0);
        Fx.addEffect(p, "minecraft:fire_resistance", 900, 0);
        Wave16Kit.puff(p.serverLevel(), p.position().add(0, 1.0, 0), 0x58E8F0, 24, 0.5);
        Fx.say(p, "Sun-kissed warmth fills you, even here.");
    }

    // ================================================================ True Heart Clasp
    static void clasp(ActCtx c) {
        ServerPlayer p = c.player();
        int harm = 0, calm = 0;
        for (LivingEntity f : Fx.foesAround(p, Wave16Tune.CLASP_RADIUS)) {
            if (f instanceof Mob m && m.getTarget() == p) {
                Fx.addEffect(f, "minecraft:glowing", 200, 0);
                harm++;
            } else {
                calm++;
            }
        }
        Fx.lendSkills(p, 200, "tensura:danger_sense", "tensura:telepathy");
        Fx.say(p, harm + " mean you harm" + (harm > 0 ? " (they glow)" : "") + "; " + calm + " keep their peace.");
    }

    // ================================================================ Brooch of Po'eur
    /** Starburst: a ring of cold shards (the brooch is a snowflake). */
    static void poeurStarburst(ActCtx c) {
        ServerPlayer p = c.player();
        FxRelic.ring(p.serverLevel(), p.position(), 7.0, 0xBFEFFF);
        Fx.aoe(p, 7.0, 6f, "minecraft:freeze", "minecraft:slowness", 120, 2);
        Fx.say(p, "The brooch bursts into a starburst of frost.");
    }

    // ================================================================ Holy Handgrenade
    static void grenade(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel lv = p.serverLevel();
        boolean tainted = Wave16Events.tainted(p);
        int roll = Wave16Rules.grenadeRoll(n -> p.getRandom().nextInt(n), tainted);
        switch (Wave16Rules.grenade(roll)) {
            case DUD -> Fx.say(p, "Two. Thou lobbest it, and it clatters, and does not explode.");
            case NOTHING -> {
                Wave16Kit.give(p, 0.05, 0.05);
                Fx.say(p, "Four. Nothing happens: the grenade stays in thy hand, and thy strength comes back to thee.");
            }
            case BLAST -> blast(p, lv);
            default -> backfire(p, lv, tainted);
        }
    }

    private static void blast(ServerPlayer p, ServerLevel lv) {
        net.minecraft.world.phys.BlockHitResult hit = Wave16Kit.lookBlock(p, Wave16Tune.GRENADE_THROW);
        Vec3 at = hit != null ? hit.getLocation().add(0, 0.2, 0) : p.getEyePosition().add(p.getLookAngle().scale(Wave16Tune.GRENADE_THROW));
        FxRelic.ring(lv, at, Wave16Tune.GRENADE_RADIUS, 0xFFF2B0);
        FxRelic.burst(lv, at, 0xFFF2B0);
        com.newuniverse.nusmp.particle.fx.Fx.spray(lv, FxThemes.RELIC_WAVE_16, FxTone.LIGHT, at.x, at.y + 0.5, at.z, 1, 0, 0, 0, 0);
        String holy = Wave6Kit.damageType(lv, "tensura:holy_damage", "minecraft:magic");
        double r2 = Wave16Tune.GRENADE_RADIUS * Wave16Tune.GRENADE_RADIUS;
        int smitten = 0, healed = 0, seen = 0;
        for (LivingEntity e : lv.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(Wave16Tune.GRENADE_RADIUS), x -> x.isAlive() && !x.isSpectator() && x.distanceToSqr(at) <= r2)) {
            if (++seen > Wave16Tune.GRENADE_MAX_TARGETS) break;
            if (Wave16Kit.good(p, e)) {
                e.heal(Wave16Tune.GRENADE_HEAL);
                healed++;
            } else if (Wave16Kit.evil(p, e)) {
                Fx.damageHeavy(p, e, Wave16Tune.GRENADE_SMITE, holy);
                smitten++;
            }
        }
        Fx.say(p, "Three! The holy grenade bursts: " + smitten + " evil smitten, " + healed + " good mended.");
    }

    private static void backfire(ServerPlayer p, ServerLevel lv, boolean tainted) {
        FxRelic.ring(lv, p.position(), Wave16Tune.BACKFIRE_RADIUS, 0x6A1B3A);
        FxRelic.burst(lv, p.position(), 0x6A1B3A);
        String dark = Wave6Kit.damageType(lv, "tensura:darkness_elemental", "minecraft:magic");
        for (LivingEntity t : Fx.foesAround(p, Wave16Tune.BACKFIRE_RADIUS)) Fx.damageHeavy(p, t, Wave16Tune.BACKFIRE_DAMAGE, dark);
        float self = Wave16Rules.backfireSelf(p.getHealth());
        if (self > 0f && !p.isCreative()) p.hurt(p.damageSources().magic(), self);
        Fx.say(p, tainted ? "Five is right out. The grenade turns on a hand that has spilt innocent blood."
                : "Five! The grenade goes off in thy hand.");
    }

    // ================================================================ Snugglable Doll
    static void dollAlive(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity cat = Wave16Swarm.snuggles(p);
        Fx.say(p, cat == null ? "Snuggles does not come." : "Snuggles comes alive, grinning with all its stitches.");
    }

    static void dollCuddle(ActCtx c) {
        ServerPlayer p = c.player();
        Fx.heal(p, 4f);
        Fx.say(p, Wave16Swarm.cuddle(p) ? "You hug Snuggles; its stitches pull tight again." : "Snuggles is not here to hug.");
    }

    // ================================================================ Paralyzing Perfume
    static void perfume(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack s = Wave6Kit.find(p, Wave16Tune.ID_PERFUME);
        if (s.isEmpty()) s = c.stack();
        if (!Wave16Kit.spend(s, p, "perfume", Wave16Tune.PERFUME_MAX, () -> 1, 1)) {
            Fx.say(p, "The bottle only hisses: it is empty until the next dawn.");
            return;
        }
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        ServerLevel lv = p.serverLevel();
        for (int i = 1; i <= 5; i++) Wave16Kit.puff(lv, eye.add(look.scale(i)), 0xB070FF, 8, 0.3 + 0.1 * i);
        int held = 0, shrugged = 0;
        boolean tensura = Wave6Kit.effect("tensura:paralysis").isPresent();
        for (LivingEntity t : Wave16Kit.cap(Fx.foesAround(p, Wave16Tune.PERFUME_RANGE), Wave16Tune.PERFUME_MAX_TARGETS)) {
            Vec3 to = t.position().add(0, t.getBbHeight() * 0.5, 0).subtract(eye);
            if (to.lengthSqr() < 1.0e-4 || to.normalize().dot(look) < Wave16Tune.PERFUME_CONE) continue;
            if (Wave16Rules.saved(t.getMaxHealth(), p.getRandom().nextInt(20) + 1)) { shrugged++; continue; }
            int ticks = t instanceof net.minecraft.world.entity.player.Player ? Wave16Tune.PERFUME_PLAYER_TICKS : Wave16Tune.PERFUME_TICKS;
            if (tensura) {
                Fx.addEffect(t, "tensura:paralysis", ticks, 0);
            } else {
                Fx.addEffect(t, "minecraft:slowness", ticks, 6);
                Fx.addEffect(t, "minecraft:weakness", ticks, 1);
            }
            held++;
        }
        Fx.say(p, "Spray! " + held + " held fast" + (shrugged > 0 ? ", " + shrugged + " shook it off" : "") + ".");
    }

    // ================================================================ Drow Sunscreen
    static void sunscreen(ActCtx c) {
        ServerPlayer p = c.player();
        p.getPersistentData().putLong(Wave16Tune.K_SUN, p.level().getGameTime() + Wave16Tune.SUN_TICKS);
        Wave16Kit.puff(p.serverLevel(), p.position().add(0, 1.0, 0), 0xF2D030, 24, 0.5);
        Fx.say(p, "You rub in the sunscreen: for three hours the sun's sting is blunted. Don't forget to reapply!");
    }

    // ================================================================ Coin of Draconic Luck
    static void coin(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack s = Wave6Kit.find(p, Wave16Tune.ID_COIN);
        if (s.isEmpty()) s = c.stack();
        ServerLevel lv = p.serverLevel();
        long today = Wave6Kit.today(lv);
        CompoundTag t = Wave6Kit.tag(s);
        if (t.getLong(Wave16Tune.S_COIN_CRACKED) > today) {
            Fx.say(p, "The coin is cracked in two and holds no magic until dawn of day " + t.getLong(Wave16Tune.S_COIN_CRACKED) + ".");
            return;
        }
        boolean heads = Wave16Rules.heads(p.getRandom().nextInt(2));
        int streak = Wave16Rules.streakAfter(heads, t.getInt(Wave16Tune.S_COIN_STREAK), t.getLong(Wave16Tune.S_COIN_DAY) == today);
        boolean cracked = !heads && Wave16Rules.cracks(streak, p.getRandom().nextInt(20) + 1);
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, s, tag -> {
            tag.putInt(Wave16Tune.S_COIN_STREAK, cracked ? 0 : streak);
            tag.putLong(Wave16Tune.S_COIN_DAY, today);
            if (cracked) tag.putLong(Wave16Tune.S_COIN_CRACKED, today + Wave16Tune.COIN_CRACK_DAYS);
        });
        if (heads) {
            Fx.addEffect(p, "minecraft:luck", Wave16Tune.COIN_BUFF_TICKS, 1);
            Fx.addEffect(p, "minecraft:strength", Wave16Tune.COIN_BUFF_TICKS, 0);
            Wave16Kit.puff(lv, p.position().add(0, 1.6, 0), 0xFFD870, 20, 0.4);
            Fx.say(p, "Heads! The dragon grins: luck is on thy side (+2).");
        } else {
            Fx.addEffect(p, "minecraft:unluck", Wave16Tune.COIN_BUFF_TICKS, 1);
            Fx.addEffect(p, "minecraft:weakness", Wave16Tune.COIN_BUFF_TICKS, 0);
            Wave16Kit.puff(lv, p.position().add(0, 1.6, 0), 0x504030, 20, 0.4);
            Fx.say(p, "Tails. The dragon's bargain bites: -2." + (streak >= 3 ? " (" + streak + " penalties in a row.)" : ""));
        }
        if (cracked) Fx.say(p, "The coin cracks in two and loses its magic for " + Wave16Tune.COIN_CRACK_DAYS + " dawns.");
    }

    // ================================================================ Strange Idol
    private static final String[] BOONS = {"minecraft:luck", "minecraft:strength", "minecraft:resistance", "minecraft:speed", "minecraft:haste", "minecraft:regeneration"};

    static void idol(ActCtx c) {
        ServerPlayer p = c.player();
        int level = Wave16Rules.idolLevel(n -> p.getRandom().nextInt(n));
        String boon = BOONS[p.getRandom().nextInt(BOONS.length)];
        boolean capped = boon.endsWith("strength") || boon.endsWith("resistance") || boon.endsWith("regeneration");
        Fx.addEffect(p, boon, 6000, capped ? Math.min(level, 2) : level);
        Wave16Kit.puff(p.serverLevel(), p.position().add(0, 1.6, 0), 0x70FF80, 24, 0.5);
        List<LivingEntity> foes = Fx.foesAround(p, 16.0);
        if (foes.isEmpty()) {
            Fx.say(p, "The idol's eyes flare: a boon (" + boon.substring(boon.indexOf(':') + 1) + " " + (level + 1) + "), and no one near to curse.");
            return;
        }
        LivingEntity victim = foes.get(p.getRandom().nextInt(foes.size()));
        boolean player = victim instanceof net.minecraft.world.entity.player.Player;
        int ticks = player ? 200 : 600;
        Fx.addEffect(victim, "minecraft:unluck", ticks, 1);
        Fx.addEffect(victim, "minecraft:weakness", ticks, 0);
        if (!player) Fx.addEffect(victim, "minecraft:slowness", ticks, 0);
        Fx.addEffect(victim, "minecraft:glowing", ticks, 0);
        Fx.say(p, "The idol's eyes flare: the higher of two rolls is thine (" + boon.substring(boon.indexOf(':') + 1) + " " + (level + 1) + "), and someone near rolls with disadvantage.");
    }

    // ================================================================ Dryad's Wedding Band
    /** Speak with Plants: crops and saplings within five blocks grow; the vines hold foes in reach. */
    static void dryadSpeak(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel lv = p.serverLevel();
        BlockPos o = p.blockPosition();
        int grown = 0;
        outer:
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    BlockPos pos = o.offset(dx, dy, dz);
                    BlockState st = lv.getBlockState(pos);
                    if (!(st.getBlock() instanceof CropBlock || st.getBlock() instanceof SaplingBlock || st.getBlock() instanceof StemBlock)) continue;
                    if (!(st.getBlock() instanceof BonemealableBlock b) || !b.isValidBonemealTarget(lv, pos, st)) continue;
                    b.performBonemeal(lv, p.getRandom(), pos, st);
                    com.newuniverse.nusmp.particle.fx.Fx.spray(lv, FxThemes.RELIC_WAVE_16, FxTone.LIGHT, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.0);
                    if (++grown >= 12) break outer;
                }
            }
        }
        Fx.aoe(p, 4.0, 0f, null, "minecraft:slowness", 100, 2);
        Fx.addEffect(p, "minecraft:regeneration", 120, 0);
        FxRelic.ring(lv, p.position(), 4.0, 0x58C060);
        Fx.say(p, "You speak with the plants. " + grown + " grow at once, and the vines hold whatever is near you.");
    }
}
