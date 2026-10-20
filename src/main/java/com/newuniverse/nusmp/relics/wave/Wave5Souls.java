package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicFlags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.List;
import com.newuniverse.nusmp.particle.fx.FxRelic;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * The Amulet of Lost Souls: eight souls, ten charges (1d10 back when a new day begins). One soul is called on at a time; each has three spells
 * costing 1, 2 and 3 charges, which are the amulet's three spell abilities. "Commune" turns to the next soul (the first turn of a day is free,
 * later ones cost a charge). Spending the LAST charge risks a natural 1 on a d20: the soul in use is freed and its spells are gone for good;
 * when the eighth is freed the ruby shatters and the amulet is no longer magical. Every spell stays under the PvP cap (6 per target); the
 * longer ones lean on effects. State (charges, soul, freed mask) lives in the stack.
 */
final class Wave5Souls {
    private Wave5Souls() {}

    static final String ID = "the_amulet_of_lost_souls";
    static final String FLAG = "w5_souls";
    private static final String GUARD = "guard_until", GUARD_NEXT = "guard_next", SHROUD = "shroud_until";

    @FunctionalInterface
    private interface Spell { boolean cast(ServerPlayer p); }

    private static final Spell[][] SPELLS = {
            {Wave5Souls::witchBolt, Wave5Souls::mistyStep, Wave5Souls::lightningBolt},
            {Wave5Souls::healingWord, Wave5Souls::lesserRestoration, Wave5Souls::spiritGuardians},
            {Wave5Souls::thunderwave, Wave5Souls::shatter, Wave5Souls::thunderStep},
            {Wave5Souls::raySickness, Wave5Souls::dragonsBreath, Wave5Souls::auraOfVitality},
            {Wave5Souls::inflictWounds, Wave5Souls::rayOfEnfeeblement, Wave5Souls::vampiricTouch},
            {Wave5Souls::burningHands, Wave5Souls::scorchingRay, Wave5Souls::fireball},
            {Wave5Souls::caustic, Wave5Souls::acidArrow, Wave5Souls::vitriolicSphere},
            {Wave5Souls::iceKnife, Wave5Souls::snowballSwarm, Wave5Souls::spiritShroud}};

    // ---------------------------------------------------------------- the pool
    /** The amulet's record after the day's refill (not yet saved). */
    private record Pool(ItemStack stack, CompoundTag st, int charges) {}

    private static Pool load(ServerPlayer p, ActCtx c) {
        ItemStack s = Wave5Data.live(p, ID, c.stack());
        if (s.isEmpty()) return null;
        CompoundTag st = Wave5Data.stack(s);
        if (st.getBoolean("shattered")) {
            Wave5Aim.msg(p, "The ruby has shattered: the amulet is no longer magical.", ChatFormatting.GRAY);
            return null;
        }
        long today = Wave5Pure.day(p.serverLevel().getServer().overworld().getDayTime());
        int charges = st.contains("charges") ? st.getInt("charges") : Wave5Pure.SOULS_MAX;
        long last = st.contains("day") ? st.getLong("day") : today;
        charges = Wave5Pure.regen(charges, Wave5Pure.SOULS_MAX, last, today, 1 + p.getRandom().nextInt(Wave5Pure.SOULS_DIE));
        st.putInt("charges", charges);
        st.putLong("day", today);
        if (!st.contains("soul")) st.putInt("soul", 0);
        return new Pool(s, st, charges);
    }

    /** Spend charges; the last one risks the soul in use. True when that soul was freed. */
    private static boolean spend(ServerPlayer p, Pool pool, int cost) {
        CompoundTag st = pool.st();
        int charges = Math.max(0, st.getInt("charges") - cost);
        st.putInt("charges", charges);
        int soul = st.getInt("soul");
        if (Wave5Pure.freesSoul(charges, 1 + p.getRandom().nextInt(Wave5Pure.SOUL_FREE_DIE))) {
            int mask = Wave5Pure.free(st.getInt("freed"), soul);
            st.putInt("freed", mask);
            ServerLevel l = p.serverLevel();
            FxRelic.burst(l, p.position().add(0, 1.2, 0), 0xB05CFF);
            Wave5Aim.puff(l, p.position().add(0, 1.2, 0), com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_5, FxTone.DARK), 20, 0.6);
            Wave5Aim.sound(l, p.position(), SoundEvents.ENCHANTMENT_TABLE_USE, 1.0f, 0.5f);
            Fx.addEffect(p, "minecraft:regeneration", 200, 1);
            if (Wave5Pure.allFreed(mask)) {
                st.putBoolean("shattered", true);
                Wave5Aim.msg(p, Wave5Pure.SOUL_NAMES[soul] + " is free. The last soul has left: the ruby shatters and the amulet is no longer magical.", ChatFormatting.DARK_PURPLE);
            } else {
                st.putInt("soul", Wave5Pure.nextSoul(mask, soul));
                Wave5Aim.msg(p, Wave5Pure.SOUL_NAMES[soul] + "'s soul slips out of the amulet, free, with a thank you. Its spells are gone for good.", ChatFormatting.DARK_PURPLE);
            }
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- the abilities
    /** Cast the current soul's spell of this tier (1 to 3 charges). */
    static void cast(ActCtx c, int tier) {
        ServerPlayer p = c.player();
        Pool pool = load(p, c);
        if (pool == null) return;
        CompoundTag st = pool.st();
        int soul = st.getInt("soul");
        if (Wave5Pure.isFreed(st.getInt("freed"), soul)) {
            int next = Wave5Pure.nextSoul(st.getInt("freed"), soul);
            if (next < 0) { Wave5Data.save(pool.stack(), st); return; }
            soul = next;
            st.putInt("soul", soul);
        }
        if (!Wave5Pure.canAfford(pool.charges(), tier)) {
            Wave5Data.save(pool.stack(), st);
            Wave5Aim.msg(p, "The souls are quiet: " + pool.charges() + " charges, " + Wave5Pure.SOUL_SPELLS[soul][tier - 1] + " needs " + tier + ".", ChatFormatting.GRAY);
            return;
        }
        boolean cast;
        try {
            cast = SPELLS[soul][tier - 1].cast(p);
        } catch (RuntimeException broken) {
            cast = false;                                          // a spell that fails costs nothing
        }
        if (cast && !spend(p, pool, tier)) {
            Wave5Aim.msg(p, Wave5Pure.SOUL_NAMES[soul] + ": " + Wave5Pure.SOUL_SPELLS[soul][tier - 1] + "  (" + st.getInt("charges") + "/" + Wave5Pure.SOULS_MAX + " charges)", ChatFormatting.LIGHT_PURPLE);
        }
        Wave5Data.save(pool.stack(), st);
    }

    /** Turn to the next soul that is still in the amulet. */
    static void commune(ActCtx c) {
        ServerPlayer p = c.player();
        Pool pool = load(p, c);
        if (pool == null) return;
        CompoundTag st = pool.st();
        int soul = st.getInt("soul");
        int next = Wave5Pure.nextSoul(st.getInt("freed"), soul);
        if (next < 0) { Wave5Data.save(pool.stack(), st); return; }
        long today = st.getLong("day");
        boolean first = st.getLong("commune_day") != today || !st.contains("commune_day");
        if (!first) {
            if (!Wave5Pure.canAfford(pool.charges(), 1)) {
                Wave5Data.save(pool.stack(), st);
                Wave5Aim.msg(p, "Turning to another soul again today costs a charge, and the amulet has none.", ChatFormatting.GRAY);
                return;
            }
            if (spend(p, pool, 1)) {                              // the soul turned away on its own (the amulet has already moved on)
                Wave5Data.save(pool.stack(), st);
                return;
            }
        }
        st.putInt("soul", next);
        st.putLong("commune_day", today);
        Wave5Data.save(pool.stack(), st);
        String[] sp = Wave5Pure.SOUL_SPELLS[next];
        Wave5Aim.msg(p, Wave5Pure.SOUL_NAMES[next] + ", " + Wave5Pure.SOUL_KINDS[next] + ": " + sp[0] + " / " + sp[1] + " / " + sp[2], ChatFormatting.LIGHT_PURPLE);
    }

    // ---------------------------------------------------------------- shared shapes
    private static void none(ServerPlayer p) { Wave5Aim.msg(p, "No foe in reach.", ChatFormatting.GRAY); }

    /** One ray at the foe in the sights. */
    private static boolean ray(ServerPlayer p, double range, float dmg, String type, ParticleOptions fx, SoundEvent snd, float pitch, String eff, int ticks, int amp) {
        LivingEntity t = Wave5Aim.pickFoe(p, range);
        if (t == null) { none(p); return false; }
        ServerLevel l = p.serverLevel();
        Vec3 to = t.getBoundingBox().getCenter();
        Wave5Aim.beam(l, Wave5Aim.muzzle(p), to, fx, 20);
        Wave5Aim.puff(l, to, fx, 8, 0.3);
        Wave5Aim.sound(l, to, snd, 0.8f, pitch);
        if (dmg > 0f) Fx.damage(p, t, Wave5Pure.cap(dmg), type);
        if (eff != null) Fx.addEffect(t, eff, ticks, amp);
        return true;
    }

    /** A cone in front of the wearer. */
    private static boolean cone(ServerPlayer p, double range, double dot, int cap, float dmg, String type, ParticleOptions fx, SoundEvent snd, String eff, int ticks, int amp, int ignite) {
        List<LivingEntity> hit = Wave5Aim.cone(p, range, dot, cap);
        if (hit.isEmpty()) { none(p); return false; }
        ServerLevel l = p.serverLevel();
        Vec3 from = Wave5Aim.muzzle(p);
        Vec3 look = p.getLookAngle();
        for (int i = 1; i <= 4; i++) Wave5Aim.puff(l, from.add(look.scale(range * i / 4.0)), fx, 6, range * 0.1 * i);
        Wave5Aim.sound(l, from, snd, 0.8f, 1.0f);
        for (LivingEntity e : hit) {
            Fx.damage(p, e, Wave5Pure.cap(dmg), type);
            if (eff != null) Fx.addEffect(e, eff, ticks, amp);
            if (ignite > 0) e.igniteForSeconds(ignite);
        }
        return true;
    }

    /** A blast where the wearer looks (it goes off on empty ground too, as a spell does). */
    private static boolean blast(ServerPlayer p, double range, double radius, float dmg, String type, ParticleOptions fx, SoundEvent snd, int colour, String eff, int ticks, int amp, int ignite) {
        ServerLevel l = p.serverLevel();
        Vec3 at = Wave5Aim.aimPoint(p, range);
        Wave5Aim.beam(l, Wave5Aim.muzzle(p), at, fx, 16);
        Wave5Aim.puff(l, at, fx, 24, radius * 0.5);
        FxRelic.ring(l, at, radius, colour);
        Wave5Aim.sound(l, at, snd, 1.0f, 0.9f);
        for (LivingEntity e : Wave5Aim.foesNear(p, at, radius)) {
            Fx.damage(p, e, Wave5Pure.cap(dmg), type);
            if (eff != null) Fx.addEffect(e, eff, ticks, amp);
            if (ignite > 0) e.igniteForSeconds(ignite);
        }
        return true;
    }

    private static void bolt(ServerLevel l, Vec3 at) {
        LightningBolt b = EntityType.LIGHTNING_BOLT.create(l);
        if (b == null) return;
        b.moveTo(at.x, at.y, at.z);
        b.setVisualOnly(true);
        l.addFreshEntity(b);
    }

    private static void push(ServerPlayer p, LivingEntity e, double strength) {
        e.knockback(strength, p.getX() - e.getX(), p.getZ() - e.getZ());
        e.hurtMarked = true;
    }

    private static void healFriends(ServerPlayer p, float amount) {
        int n = 0;
        for (TamableAnimal t : p.serverLevel().getEntitiesOfClass(TamableAnimal.class, p.getBoundingBox().inflate(10.0), t -> t.isAlive() && t.isOwnedBy(p))) {
            if (n++ >= 8) break;
            t.heal(amount);
        }
    }

    // ---------------------------------------------------------------- Comos Parolath (Triton: lightning and water)
    private static boolean witchBolt(ServerPlayer p) {
        return ray(p, 18, 5, "minecraft:lightning_bolt", com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_5, FxTone.LIGHT), SoundEvents.LIGHTNING_BOLT_THUNDER, 2.0f, "minecraft:slowness", 40, 0);
    }

    private static boolean mistyStep(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        Vec3 from = p.position();
        if (!Fx.teleportLook(p, 10.0)) { Wave5Aim.msg(p, "No safe place to step to.", ChatFormatting.GRAY); return false; }
        FxRelic.burst(l, from.add(0, 1.0, 0), 0x80C0FF);
        Wave5Aim.puff(l, from.add(0, 1.0, 0), com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_5, FxTone.LIGHT), 20, 0.5);
        FxRelic.burst(p.serverLevel(), p.position().add(0, 1.0, 0), 0x80C0FF);
        Wave5Skills.lend(p, Wave5Skills.DANGER_SENSE, 200);          // merged with Tensura's Danger Sense: you step to where nothing waits for you
        Wave5Aim.sound(p.serverLevel(), p.position(), SoundEvents.ENDERMAN_TELEPORT, 0.8f, 1.5f);
        return true;
    }

    private static boolean lightningBolt(ServerPlayer p) {
        List<LivingEntity> line = Wave5Aim.cone(p, 16.0, 0.97, 6);
        if (line.isEmpty()) { none(p); return false; }
        ServerLevel l = p.serverLevel();
        Wave5Aim.beam(l, Wave5Aim.muzzle(p), line.get(line.size() - 1).getBoundingBox().getCenter(), com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_5, FxTone.LIGHT), 28);
        bolt(l, line.get(0).position());
        for (LivingEntity e : line) {
            Fx.damage(p, e, Wave5Pure.cap(6), "minecraft:lightning_bolt");
            Fx.addEffect(e, "minecraft:slowness", 60, 1);
        }
        return true;
    }

    // ---------------------------------------------------------------- Helbi Finemane (Gnome: healing)
    private static boolean healingWord(ServerPlayer p) {
        Fx.heal(p, 6.0f);
        healFriends(p, 6.0f);
        Wave5Aim.puff(p.serverLevel(), p.position().add(0, 1.4, 0), com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_5, FxTone.LIGHT), 6, 0.4);
        Wave5Aim.sound(p.serverLevel(), p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
        return true;
    }

    private static boolean lesserRestoration(ServerPlayer p) {
        int cured = 0;
        for (MobEffectInstance i : new ArrayList<>(p.getActiveEffects())) {
            Holder<MobEffect> eff = i.getEffect();
            if (eff.value().getCategory() == MobEffectCategory.HARMFUL && !eff.equals(MobEffects.WITHER)) {
                if (p.removeEffect(eff)) cured++;
            }
        }
        if (cured == 0) { Wave5Aim.msg(p, "Nothing ails you that this can mend.", ChatFormatting.GRAY); return false; }
        Wave5Skills.lend(p, Wave5Skills.POISON_RESIST, 1200);        // merged with Tensura's Poison Resistance for a minute: the cure holds
        Wave5Aim.puff(p.serverLevel(), p.position().add(0, 1.0, 0), com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_5, FxTone.LIGHT), 12, 0.5);
        Wave5Aim.sound(p.serverLevel(), p.position(), SoundEvents.ZOMBIE_VILLAGER_CURE, 0.6f, 1.6f);
        return true;
    }

    /** Twelve seconds of radiant ward: every two seconds, foes within five blocks take 1 and are slowed (so six in all). */
    private static boolean spiritGuardians(ServerPlayer p) {
        long now = p.level().getGameTime();
        CompoundTag d = Wave5Data.player(p);
        d.putLong(GUARD, now + 240L);
        d.putLong(GUARD_NEXT, now);
        Wave5Skills.lend(p, Wave5Skills.PHYSICAL_RESIST, 260);       // merged with Tensura's Physical Attack Resistance: the guardians turn blows aside while they stand
        FxRelic.ring(p.serverLevel(), p.position().add(0, 0.2, 0), 5.0, 0xFFE9A0);
        Wave5Aim.sound(p.serverLevel(), p.position(), SoundEvents.BEACON_ACTIVATE, 0.8f, 1.6f);
        return true;
    }

    // ---------------------------------------------------------------- Hurod (Half-Orc: thunder)
    private static boolean thunderwave(ServerPlayer p) {
        List<LivingEntity> foes = Fx.foesAround(p, 5.0);
        if (foes.isEmpty()) { none(p); return false; }
        ServerLevel l = p.serverLevel();
        FxRelic.ring(l, p.position().add(0, 0.3, 0), 5.0, 0x9CC8FF);
        Wave5Aim.puff(l, p.position().add(0, 1.0, 0), com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_5, FxTone.LIGHT), 30, 1.8);
        Wave5Aim.sound(l, p.position(), SoundEvents.LIGHTNING_BOLT_THUNDER, 0.5f, 1.8f);
        int n = 0;
        for (LivingEntity e : foes) {
            if (n++ >= 12) break;
            Fx.damage(p, e, Wave5Pure.cap(3), "minecraft:magic");
            push(p, e, 1.1);
        }
        return true;
    }

    private static boolean shatter(ServerPlayer p) {
        return blast(p, 14, 3.0, 5, "minecraft:sonic_boom", com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_5, FxTone.DARK), SoundEvents.GLASS_BREAK, 0xC8E0FF, "minecraft:weakness", 100, 0, 0);
    }

    private static boolean thunderStep(ServerPlayer p) {
        Vec3 from = p.position();
        if (!Fx.teleportLook(p, 12.0)) { Wave5Aim.msg(p, "No safe place to step to.", ChatFormatting.GRAY); return false; }
        ServerLevel l = p.serverLevel();
        bolt(l, from);
        FxRelic.ring(l, from.add(0, 0.2, 0), 4.0, 0x9CC8FF);
        for (LivingEntity e : Wave5Aim.foesNear(p, from, 4.0)) {
            Fx.damage(p, e, Wave5Pure.cap(6), "minecraft:lightning_bolt");
            e.knockback(1.0, from.x - e.getX(), from.z - e.getZ());
            e.hurtMarked = true;
        }
        return true;
    }

    // ---------------------------------------------------------------- Ilia (Yuan-Ti: poison)
    private static boolean raySickness(ServerPlayer p) {
        return ray(p, 16, 2, "minecraft:magic", com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_5, FxTone.LIGHT), SoundEvents.EVOKER_CAST_SPELL, 1.4f, "minecraft:poison", 100, 0);
    }

    private static boolean dragonsBreath(ServerPlayer p) {
        return cone(p, 6.0, 0.8, 8, 2.5f, "minecraft:magic", com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_5, FxTone.DARK), SoundEvents.FIRECHARGE_USE, "minecraft:poison", 80, 0, 0);
    }

    private static boolean auraOfVitality(ServerPlayer p) {
        Fx.heal(p, 4.0f);
        Fx.addEffect(p, "minecraft:regeneration", 300, 1);
        Wave5Skills.lend(p, Wave5Skills.SELF_REGENERATION, 300);     // merged with Tensura's Self Regeneration for the same fifteen seconds (skipped if the skill is not there)
        healFriends(p, 4.0f);
        FxRelic.ring(p.serverLevel(), p.position().add(0, 0.2, 0), 2.5, 0x6EE07A);
        Wave5Aim.puff(p.serverLevel(), p.position().add(0, 1.0, 0), com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_5, FxTone.LIGHT), 14, 0.6);
        Wave5Aim.sound(p.serverLevel(), p.position(), SoundEvents.ZOMBIE_VILLAGER_CURE, 0.6f, 1.2f);
        return true;
    }

    // ---------------------------------------------------------------- Klaroven of the Torch (Elf: necrotic)
    private static boolean inflictWounds(ServerPlayer p) {
        return ray(p, 4.5, 6, "minecraft:wither", com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_5, FxTone.DARK), SoundEvents.EVOKER_CAST_SPELL, 0.6f, null, 0, 0);
    }

    private static boolean rayOfEnfeeblement(ServerPlayer p) {
        LivingEntity t = Wave5Aim.pickFoe(p, 16.0);
        if (t == null) { none(p); return false; }
        ServerLevel l = p.serverLevel();
        Wave5Aim.beam(l, Wave5Aim.muzzle(p), t.getBoundingBox().getCenter(), com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_5, FxTone.DARK), 20);
        Wave5Aim.sound(l, t.position(), SoundEvents.EVOKER_CAST_SPELL, 0.8f, 0.8f);
        Fx.damage(p, t, Wave5Pure.cap(1), "minecraft:magic");
        Fx.addEffect(t, "minecraft:weakness", 240, 1);
        Fx.addEffect(t, "minecraft:slowness", 120, 0);
        return true;
    }

    private static boolean vampiricTouch(ServerPlayer p) {
        LivingEntity t = Wave5Aim.pickFoe(p, 4.5);
        if (t == null) { none(p); return false; }
        ServerLevel l = p.serverLevel();
        Wave5Aim.puff(l, t.getBoundingBox().getCenter(), com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_5, FxTone.DARK), 14, 0.4);
        Wave5Aim.puff(l, p.position().add(0, 1.2, 0), com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_5, FxTone.LIGHT), 5, 0.4);
        Wave5Aim.sound(l, t.position(), SoundEvents.EVOKER_CAST_SPELL, 0.8f, 0.5f);
        Fx.damage(p, t, Wave5Pure.cap(6), "minecraft:wither");
        Fx.heal(p, 6.0f);
        return true;
    }

    // ---------------------------------------------------------------- Mantra Tarlem (Tiefling: fire)
    private static boolean burningHands(ServerPlayer p) {
        return cone(p, 4.5, 0.6, 6, 4, "minecraft:in_fire", com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_5, FxTone.LIGHT), SoundEvents.FIRECHARGE_USE, null, 0, 0, 4);
    }

    private static boolean scorchingRay(ServerPlayer p) {
        List<LivingEntity> foes = Wave5Aim.cone(p, 18.0, 0.9, 3);
        if (foes.isEmpty()) { none(p); return false; }
        ServerLevel l = p.serverLevel();
        for (int i = 0; i < 3; i++) {                              // three bolts, shared out among the foes in view
            LivingEntity t = foes.get(i % foes.size());
            Wave5Aim.beam(l, Wave5Aim.muzzle(p), t.getBoundingBox().getCenter(), com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_5, FxTone.LIGHT), 16);
            Fx.damage(p, t, Wave5Pure.cap(2), "minecraft:in_fire");
            t.igniteForSeconds(2);
        }
        Wave5Aim.sound(l, p.position(), SoundEvents.FIRECHARGE_USE, 1.0f, 1.4f);
        return true;
    }

    private static boolean fireball(ServerPlayer p) {
        boolean went = blast(p, 22, 4.0, 6, "minecraft:in_fire", com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_5, FxTone.LIGHT), SoundEvents.GENERIC_EXPLODE.value(), 0xFF8A2A, null, 0, 0, 5);
        Vec3 at = Wave5Aim.aimPoint(p, 22);
        com.newuniverse.nusmp.particle.fx.Fx.spray(p.serverLevel(), FxThemes.RELIC_WAVE_5, FxTone.LIGHT, at.x, at.y, at.z, 3, 0.8, 0.8, 0.8, 0.0);   // the blast is for show: no block is ever touched
        return went;
    }

    // ---------------------------------------------------------------- Prammac Caerkul (Dragonborn: acid)
    private static boolean caustic(ServerPlayer p) {
        return cone(p, 10.0, 0.95, 4, 2, "minecraft:magic", com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_5, FxTone.LIGHT), SoundEvents.EVOKER_CAST_SPELL, "minecraft:poison", 60, 0, 0);
    }

    private static boolean acidArrow(ServerPlayer p) {
        return ray(p, 20, 2.5f, "minecraft:magic", com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_5, FxTone.LIGHT), SoundEvents.FIRECHARGE_USE, 1.8f, "minecraft:poison", 80, 0);
    }

    private static boolean vitriolicSphere(ServerPlayer p) {
        return blast(p, 20, 3.5, 5, "minecraft:magic", com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_5, FxTone.LIGHT), SoundEvents.GLASS_BREAK, 0x9AE04A, "minecraft:weakness", 160, 1, 0);
    }

    // ---------------------------------------------------------------- Vagan Nightfist (Goliath: cold)
    private static boolean iceKnife(ServerPlayer p) {
        return ray(p, 18, 4, "minecraft:freeze", com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_5, FxTone.LIGHT), SoundEvents.GLASS_BREAK, 1.6f, "minecraft:slowness", 60, 1);
    }

    private static boolean snowballSwarm(ServerPlayer p) {
        return blast(p, 18, 3.0, 5, "minecraft:freeze", com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_5, FxTone.LIGHT), SoundEvents.SNOW_GOLEM_SHOOT, 0xCFEFFF, "minecraft:slowness", 80, 1, 0);
    }

    /** Thirty seconds in which the wearer's blows carry the cold of the dead: +2 and a slow on every melee hit. */
    private static boolean spiritShroud(ServerPlayer p) {
        long now = p.level().getGameTime();
        Wave5Data.player(p).putLong(SHROUD, now + 600L);
        FxRelic.ring(p.serverLevel(), p.position().add(0, 0.2, 0), 1.8, 0xCFEFFF);
        Wave5Aim.puff(p.serverLevel(), p.position().add(0, 1.0, 0), com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_5, FxTone.LIGHT), 24, 0.7);
        Wave5Aim.sound(p.serverLevel(), p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 0.5f);
        return true;
    }

    // ---------------------------------------------------------------- the slow tick and the shroud's blows
    /** Every second: the ward's pulse and the shroud's mist; both end if the amulet is taken off. */
    static void tick(ServerPlayer p) {
        if (!Wave5Data.hasPlayerData(p)) return;
        CompoundTag d = Wave5Data.player(p);
        long guard = d.getLong(GUARD), shroud = d.getLong(SHROUD);
        if (guard <= 0L && shroud <= 0L) return;
        long now = p.level().getGameTime();
        boolean worn = RelicFlags.has(p, FLAG);
        ServerLevel l = p.serverLevel();
        if (guard > 0L) {
            if (!worn || now >= guard) {
                d.putLong(GUARD, 0L);
            } else if (now >= d.getLong(GUARD_NEXT)) {
                d.putLong(GUARD_NEXT, now + 40L);
                FxRelic.ring(l, p.position().add(0, 0.2, 0), 5.0, 0xFFE9A0);
                int n = 0;
                for (LivingEntity e : Fx.foesAround(p, 5.0)) {
                    if (n++ >= 10) break;
                    Fx.damage(p, e, 1.0f, "minecraft:magic");
                    Fx.addEffect(e, "minecraft:slowness", 50, 0);
                }
            }
        }
        if (shroud > 0L) {
            if (!worn || now >= shroud) d.putLong(SHROUD, 0L);
            else Wave5Aim.puff(l, p.position().add(0, 1.0, 0), com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_5, FxTone.LIGHT), 8, 0.6);
        }
    }

    /** Spirit Shroud: the wearer's own melee hits carry +2 and a slow. */
    static void onOutgoing(LivingIncomingDamageEvent e) {
        DamageSource src = e.getSource();
        if (!(src.getEntity() instanceof ServerPlayer p) || src.getDirectEntity() != p) return;
        if (!Wave5Data.hasPlayerData(p) || Wave5Data.player(p).getLong(SHROUD) <= p.level().getGameTime()) return;
        if (!RelicFlags.has(p, FLAG)) return;
        e.setAmount(e.getAmount() + 2.0f);
        Fx.addEffect(e.getEntity(), "minecraft:slowness", 40, 0);
    }

    /** Taking the amulet off, dying or leaving: the ward and the shroud end with it. */
    static void clear(ServerPlayer p) {
        if (!Wave5Data.hasPlayerData(p)) return;
        CompoundTag d = Wave5Data.player(p);
        d.putLong(GUARD, 0L);
        d.putLong(SHROUD, 0L);
    }
}
