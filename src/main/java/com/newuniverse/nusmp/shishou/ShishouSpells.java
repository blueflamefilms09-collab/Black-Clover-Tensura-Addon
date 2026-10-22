package com.newuniverse.nusmp.shishou;

import com.newuniverse.nusmp.perf.PerfParticles;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Why: every active skill of Shishou in one place, each server-decided and each BOUNDED: one box query at most per use ({@link ShishouTargets#near}, capped), at most a few dozen particles through the perf
 * budget, no entity spawned, no block placed or broken (the spells are hit-scan or area effects, never terrain), no chunk loaded. A cast comes from a {@link Caster}: the wielder (a player holding the sword) or the
 * thrown sword's own body (his autonomous spells). {@link #cast} returns the {@link Result}: what the cast costs and its cooldown, or {@link Result#FAIL} (nothing happened, nothing is paid). The service pays and starts the cooldown.
 * Worst cases per skill are in docs/items/shishou.md.
 */
public final class ShishouSpells {
    private ShishouSpells() {}

    /** The one casting: the level, who casts (a wielder and/or the body), where from and which way, the sword's level and record. */
    public record Caster(ServerLevel level, ServerPlayer wielder, ShishouEntity body, Vec3 origin, Vec3 look, int swordLevel, ShishouRecord record, long now, UUID key) {
        public LivingEntity attacker() { return wielder != null && body == null ? wielder : body != null ? body : wielder; }
    }

    /** What a successful cast costs in mana and the cooldown it starts; {@link #FAIL} = nothing happened. */
    public record Result(double cost, int cooldown, boolean ok) {
        public static final Result FAIL = new Result(0.0, 0, false);

        public static Result of(double cost, int cooldown) { return new Result(cost, cooldown, true); }
    }

    private static final DustParticleOptions DARK = new DustParticleOptions(new Vector3f(0.20f, 0.04f, 0.30f), 1.4f);
    private static final DustParticleOptions LIGHT = new DustParticleOptions(new Vector3f(0.55f, 0.92f, 1.0f), 1.2f);

    /** The skills a body may cast on its own (no wielder-only effects). */
    public static final ShishouSkill[] AUTONOMOUS = {ShishouSkill.BLACK_LIGHTNING, ShishouSkill.FIRE_MAGIC, ShishouSkill.EARTH_MAGIC, ShishouSkill.WATER_ICE_MAGIC, ShishouSkill.DARKNESS_MAGIC, ShishouSkill.WIND_MAGIC};

    // ================================================================ the dispatcher
    public static Result cast(ShishouSkill s, Caster c) {
        return switch (s) {
            case STOMACH -> stomach(c);
            case MAGIC_SENSE -> magicSense(c);
            case MULTILAYER_BARRIER -> barrier(c);
            case STEALTH -> stealth(c);
            case SPATIAL_MOTION -> leap(c, ShishouRules.LEAP_RANGE, s);
            case SPATIAL_MOTION_SHORT -> leap(c, ShishouRules.LEAP_RANGE_SHORT, s);
            case DIVINE_SWORD_RELEASE -> divine(c);
            case BLACK_LIGHTNING -> blackLightning(c);
            case DOPPELGANGER -> doppelganger(c);
            case FIRE_MAGIC -> fire(c);
            case WIND_MAGIC -> wind(c);
            case EARTH_MAGIC -> earth(c);
            case WATER_ICE_MAGIC -> waterIce(c);
            case DARKNESS_MAGIC -> darkness(c);
            case HEALING_MAGIC -> healing(c);
            case PURIFICATION -> purification(c);
            default -> Result.FAIL;
        };
    }

    // ================================================================ helpers
    static void fx(ServerLevel l, ParticleOptions p, Vec3 at, int count, double spread) { PerfParticles.send(l, p, at.x, at.y, at.z, count, spread, spread, spread, 0.02); }

    private static DamageSource magic(Caster c) {
        LivingEntity a = c.attacker();
        return a == null ? c.level().damageSources().magic() : c.level().damageSources().indirectMagic(a, a);
    }

    /** A spell hit: it is its own hit, so the wielder's melee i-frames do not eat it. */
    static boolean hit(Caster c, LivingEntity t, double dmg) {
        if (dmg <= 0.0) return false;
        t.invulnerableTime = 0;
        return t.hurt(magic(c), (float) dmg);
    }

    private static Vec3 center(LivingEntity e) { return e.position().add(0, e.getBbHeight() * 0.5, 0); }

    /** The first hostile creature the look ray meets within {@code range}, not hidden behind a block; null when none. One box query. */
    static Mob rayTarget(Caster c, double range) {
        Vec3 end = c.origin().add(c.look().scale(range));
        BlockHitResult wall = c.level().clip(new ClipContext(c.origin(), end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, c.attacker()));
        double limit = wall.getType() == HitResult.Type.MISS ? range : c.origin().distanceTo(wall.getLocation());
        AABB box = new AABB(c.origin(), end).inflate(1.5);
        Mob best = null;
        double bestD = Double.MAX_VALUE;
        for (Mob m : c.level().getEntitiesOfClass(Mob.class, box, e -> ShishouTargets.hostileFor(c.wielder(), e))) {
            var hit = m.getBoundingBox().inflate(0.3).clip(c.origin(), end);
            if (hit.isEmpty()) continue;
            double d = c.origin().distanceTo(hit.get());
            if (d <= limit && d < bestD) {
                bestD = d;
                best = m;
            }
        }
        return best;
    }

    /** The point the cast is aimed at: the hostile under the look ray, else the block it hits, else the end of the range. */
    static Vec3 aimPoint(Caster c, double range) {
        Mob m = rayTarget(c, range);
        if (m != null) return center(m);
        Vec3 end = c.origin().add(c.look().scale(range));
        BlockHitResult wall = c.level().clip(new ClipContext(c.origin(), end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, c.attacker()));
        return wall.getType() == HitResult.Type.MISS ? end : wall.getLocation().subtract(c.look().scale(0.3));
    }

    private static void sound(ServerLevel l, Vec3 at, net.minecraft.sounds.SoundEvent e, float vol, float pitch) { l.playSound(null, at.x, at.y, at.z, e, SoundSource.PLAYERS, vol, pitch); }

    // ================================================================ Stomach (Ibukuro): the vacuum
    /** Takes the loot lying near the wielder into the storage: at most {@link ShishouRules#STOMACH_ITEMS} item entities from one box query. Nothing the storage refuses, nothing a player threw. */
    private static Result stomach(Caster c) {
        if (c.wielder() == null) return Result.FAIL;
        int moved = vacuum(c.level(), c.wielder(), ShishouRules.STOMACH_RADIUS, ShishouRules.STOMACH_ITEMS);
        return moved > 0 ? Result.of(ShishouSkill.STOMACH.manaCost(), ShishouSkill.STOMACH.cooldown()) : Result.FAIL;
    }

    /** The vacuum itself (also the auto toggle's once-a-second pass): returns how many item entities were taken in. */
    public static int vacuum(ServerLevel level, ServerPlayer who, int radius, int cap) {
        ShishouStorage st = ShishouData.get().storage();
        Vec3 at = who.position().add(0, 0.5, 0);
        List<ItemEntity> items = ShishouTargets.near(level, ItemEntity.class, at, radius, cap * 2, ie -> ie.isAlive() && ie.getOwner() == null && ie.getItem().getItem() != ShishouItems.SHISHOU.get() && ShishouStorage.accepts(ie.getItem()));
        int moved = 0;
        for (ItemEntity ie : items) {
            if (moved >= cap) break;
            ItemStack rest = st.insert(ie.getItem());
            if (rest.getCount() == ie.getItem().getCount()) continue;
            if (rest.isEmpty()) ie.discard(); else ie.setItem(rest);
            fx(level, Fx.options(FxThemes.SKILL_SHISHOU, FxTone.LIGHT), ie.position(), 1, 0.1);
            moved++;
        }
        return moved;
    }

    // ================================================================ Magic Sense (Magikansi)
    private static Result magicSense(Caster c) {
        if (c.wielder() == null) return Result.FAIL;
        ShishouWindows w = ShishouLive.WINDOWS;
        if (w.active(c.key(), ShishouWindows.Kind.SENSE, c.now())) {
            w.stop(c.key(), ShishouWindows.Kind.SENSE);
            return Result.of(0.0, 0);                                                   // switched off early: free, no cooldown
        }
        w.start(c.key(), ShishouWindows.Kind.SENSE, c.now(), ShishouRules.SENSE_TICKS);
        senseScan(c.level(), c.wielder());
        return Result.of(ShishouSkill.MAGIC_SENSE.manaCost(), ShishouSkill.MAGIC_SENSE.cooldown());
    }

    /** One pulse of Magic Sense: Glowing on up to {@link ShishouRules#SENSE_TARGETS} non-player creatures within 35 blocks, through walls (Glowing is drawn through blocks). One box query; never a chunk load. */
    public static int senseScan(ServerLevel level, ServerPlayer who) {
        boolean neutrals = ShishouConfig.senseNeutrals();
        List<LivingEntity> marked = ShishouTargets.near(level, LivingEntity.class, who.position(), ShishouRules.SENSE_RADIUS, ShishouRules.SENSE_TARGETS, e -> ShishouTargets.markable(who, e, neutrals));
        for (LivingEntity e : marked) e.addEffect(new MobEffectInstance(MobEffects.GLOWING, ShishouRules.SENSE_GLOW_TICKS, 0, false, false));
        return marked.size();
    }

    // ================================================================ Multilayer Barrier (Taju Kekkai)
    private static Result barrier(Caster c) {
        if (c.wielder() == null) return Result.FAIL;
        ShishouWindows w = ShishouLive.WINDOWS;
        if (w.active(c.key(), ShishouWindows.Kind.BARRIER, c.now())) {
            stopBarrier(c.wielder());
            return Result.of(0.0, 0);
        }
        w.start(c.key(), ShishouWindows.Kind.BARRIER, c.now(), 1_000_000);              // lasts until it is toggled off, he is dropped or the wielder leaves
        applyBarrier(c.wielder(), c.swordLevel());
        fx(c.level(), LIGHT, c.wielder().position().add(0, 1, 0), 14, 0.6);
        return Result.of(0.0, 0);
    }

    /** Gives (or refreshes) the Absorption hearts of the barrier: 4 to 8 hearts by level. */
    public static void applyBarrier(ServerPlayer p, int level) {
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ShishouRules.BARRIER_REFRESH + 60, ShishouRules.barrierAmplifier(ShishouRules.barrierHearts(level)), false, false, true));
    }

    public static void stopBarrier(ServerPlayer p) {
        ShishouLive.WINDOWS.stop(p.getUUID(), ShishouWindows.Kind.BARRIER);
        p.removeEffect(MobEffects.ABSORPTION);
    }

    // ================================================================ Stealth and Camouflage
    private static Result stealth(Caster c) {
        if (c.wielder() == null) return Result.FAIL;
        ShishouWindows w = ShishouLive.WINDOWS;
        if (w.active(c.key(), ShishouWindows.Kind.STEALTH, c.now())) {
            w.stop(c.key(), ShishouWindows.Kind.STEALTH);
            ShishouService.setMinionVeil(c.wielder(), false);
            return Result.of(0.0, 0);
        }
        w.start(c.key(), ShishouWindows.Kind.STEALTH, c.now(), ShishouRules.STEALTH_TICKS);
        ShishouService.setMinionVeil(c.wielder(), true);
        fx(c.level(), DARK, c.wielder().position().add(0, 1, 0), 10, 0.5);
        return Result.of(ShishouSkill.STEALTH.manaCost(), ShishouSkill.STEALTH.cooldown());
    }

    // ================================================================ Spatial Motion (Kuukan Idou)
    private static Result leap(Caster c, double range, ShishouSkill skill) {
        ServerPlayer p = c.wielder();
        if (p == null) return Result.FAIL;
        Vec3 dir = c.look().normalize();
        BlockHitResult wall = c.level().clip(new ClipContext(c.origin(), c.origin().add(dir.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        double max = wall.getType() == HitResult.Type.MISS ? range : Math.max(0.0, c.origin().distanceTo(wall.getLocation()) - 0.6);
        int steps = 0;
        for (double d = max; d >= 1.0 && steps < ShishouRules.LEAP_STEPS_MAX; d -= 0.5, steps++) {
            Vec3 shift = dir.scale(d);
            AABB box = p.getBoundingBox().move(shift);
            Vec3 to = p.position().add(shift);
            if (!c.level().noCollision(p, box) || !c.level().getWorldBorder().isWithinBounds(box)) continue;
            Vec3 from = p.position().add(0, 1, 0);
            p.teleportTo(to.x, to.y, to.z);
            p.fallDistance = 0.0f;
            ShishouLive.WINDOWS.start(p.getUUID(), ShishouWindows.Kind.IFRAMES, c.now(), ShishouRules.LEAP_IFRAMES);
            fx(c.level(), Fx.options(FxThemes.SKILL_SHISHOU, FxTone.LIGHT), from, 6, 0.4);
            fx(c.level(), Fx.options(FxThemes.SKILL_SHISHOU, FxTone.LIGHT), to.add(0, 1, 0), 6, 0.4);
            sound(c.level(), to, SoundEvents.ENDERMAN_TELEPORT, 0.5f, 1.4f);
            return Result.of(skill.manaCost(), skill.cooldown());
        }
        return Result.FAIL;                                                              // no free spot on the way: nothing happens, nothing is paid
    }

    // ================================================================ Divine Sword Release
    private static Result divine(Caster c) {
        if (c.wielder() == null) return Result.FAIL;
        Vec3 at = aimPoint(c, ShishouRules.DIVINE_RANGE);
        double dmg = ShishouRules.divine(c.swordLevel(), ShishouConfig.divineCap());
        for (Mob m : ShishouTargets.hostilesNear(c.level(), c.wielder(), at, ShishouRules.DIVINE_RADIUS, ShishouRules.DIVINE_TARGETS)) hit(c, m, dmg);
        for (int y = 0; y <= 12; y += 2) fx(c.level(), Fx.options(FxThemes.SKILL_SHISHOU, FxTone.LIGHT), at.add(0, y, 0), 2, 0.35);                  // the light pillar: seven rows of two Rukh
        fx(c.level(), Fx.options(FxThemes.SKILL_SHISHOU, FxOld.END_ROD), at.add(0, 1, 0), 10, 0.8);
        sound(c.level(), at, SoundEvents.BEACON_ACTIVATE, 1.0f, 1.6f);
        ShishouLive.WINDOWS.start(c.key(), ShishouWindows.Kind.RELEASE, c.now(), ShishouRules.DIVINE_RELEASE_TICKS);           // the temporary release: +25% on his blows for eight seconds
        return Result.of(ShishouSkill.DIVINE_SWORD_RELEASE.manaCost(), ShishouSkill.DIVINE_SWORD_RELEASE.cooldown());
    }

    // ================================================================ Black Lightning (Kurokaminari)
    private static Result blackLightning(Caster c) {
        Mob t = rayTarget(c, ShishouRules.BLACK_LIGHTNING_RANGE);
        if (t == null) return Result.FAIL;
        double dmg = ShishouRules.blackLightning(c.swordLevel(), ShishouConfig.lightningCap());
        Vec3 at = center(t);
        hit(c, t, dmg);
        for (Mob m : ShishouTargets.near(c.level(), Mob.class, at, ShishouRules.BLACK_LIGHTNING_SPLASH_RADIUS, ShishouRules.BLACK_LIGHTNING_SPLASH_TARGETS + 1, e -> e != t && ShishouTargets.hostileFor(c.wielder(), e)))
            hit(c, m, dmg * ShishouRules.BLACK_LIGHTNING_SPLASH_SHARE);
        for (int i = 0; i < 12; i++) fx(c.level(), DARK, at.add(0, 12 - i, 0), 1, 0.12);                                      // the bolt: a dark column from the sky (a particle visual, never a real lightning bolt)
        fx(c.level(), Fx.options(FxThemes.SKILL_SHISHOU, FxOld.ELECTRIC_SPARK), at, 8, 0.5);
        fx(c.level(), Fx.options(FxThemes.SKILL_SHISHOU, FxTone.DARK), at, 3, 0.4);
        sound(c.level(), at, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.6f, 1.5f);
        return Result.of(ShishouSkill.BLACK_LIGHTNING.manaCost(), ShishouSkill.BLACK_LIGHTNING.cooldown());
    }

    // ================================================================ Doppelganger (the clones are table rows, not entities)
    private static Result doppelganger(Caster c) {
        if (c.wielder() == null) return Result.FAIL;
        Mob t = rayTarget(c, ShishouRules.SPELL_RANGE);
        if (t == null) return Result.FAIL;
        int made = ShishouLive.CLONES.spawn(c.key(), t.getUUID(), ShishouRules.CLONES_PER_CAST, c.now(), ShishouRules.CLONE_LIFE, ShishouRules.CLONES_PER_CAST, ShishouRules.CLONES_GLOBAL);
        if (made <= 0) return Result.FAIL;
        fx(c.level(), Fx.options(FxThemes.SKILL_SHISHOU, FxTone.LIGHT), c.origin(), 8, 0.6);
        return Result.of(ShishouSkill.DOPPELGANGER.manaCost(), ShishouSkill.DOPPELGANGER.cooldown());
    }

    /**
     * One step of every live clone (called every {@link ShishouRules#CLONE_PERIOD} ticks while any exists): each strikes its target for a share of the wielder's strike, with a particle trail; a clone whose
     * wielder or target is gone, or whose life is over, dissolves. At most {@link ShishouRules#CLONES_GLOBAL} clones, so at most six hits and twelve particles per step.
     */
    public static void tickClones(MinecraftServer server, long now) {
        ShishouClones table = ShishouLive.CLONES;
        table.sweep(now);
        Iterator<ShishouClones.Clone> it = new ArrayList<>(table.live()).iterator();
        while (it.hasNext()) {
            ShishouClones.Clone k = it.next();
            ServerPlayer w = server.getPlayerList().getPlayer(k.wielder);
            Entity t = w == null ? null : w.serverLevel().getEntity(k.target);
            if (!(t instanceof LivingEntity victim) || !victim.isAlive()) {
                table.live().remove(k);
                continue;
            }
            if (now < k.nextStrike) continue;
            k.nextStrike = now + ShishouRules.CLONE_PERIOD;
            ServerLevel sl = w.serverLevel();
            double ang = now * 0.3 + k.slot * Math.PI / 2.0;
            Vec3 pos = center(victim).add(Math.cos(ang) * 1.3, 0.3 + k.slot * 0.15, Math.sin(ang) * 1.3);
            double dmg = ShishouRules.strikeDamage(ShishouData.get().record().level(), ShishouData.get().record().attackSteps(), 0.0, ShishouConfig.strikeCap()) * ShishouRules.CLONE_SHARE;
            Caster c = new Caster(sl, w, null, pos, center(victim).subtract(pos), ShishouData.get().record().level(), ShishouData.get().record(), now, k.wielder);
            hit(c, victim, dmg);
            fx(sl, Fx.options(FxThemes.SKILL_SHISHOU, FxTone.LIGHT), pos, 2, 0.1);
        }
    }

    // ================================================================ the absorbed elemental magics
    private static Result fire(Caster c) {
        Vec3 at = aimPoint(c, ShishouRules.SPELL_RANGE);
        double dmg = ShishouRules.spellDamage(c.swordLevel(), 8.0, 0.3, 40.0);
        for (Mob m : ShishouTargets.hostilesNear(c.level(), c.wielder(), at, 3.0, ShishouRules.SPELL_TARGETS)) {
            if (hit(c, m, dmg)) m.setRemainingFireTicks(100);
        }
        line(c, at, Fx.options(FxThemes.SKILL_SHISHOU, FxOld.FLAME), 10);
        fx(c.level(), Fx.options(FxThemes.SKILL_SHISHOU, FxOld.FLAME), at, 14, 0.7);
        sound(c.level(), at, SoundEvents.FIRECHARGE_USE, 0.8f, 1.0f);
        return Result.of(ShishouSkill.FIRE_MAGIC.manaCost(), ShishouSkill.FIRE_MAGIC.cooldown());
    }

    private static Result wind(Caster c) {
        Vec3 from = c.origin();
        double dmg = ShishouRules.spellDamage(c.swordLevel(), 4.0, 0.2, 25.0);
        for (Mob m : ShishouTargets.hostilesNear(c.level(), c.wielder(), from, 5.0, ShishouRules.SPELL_TARGETS)) {
            if (hit(c, m, dmg)) {
                Vec3 away = m.position().subtract(from).multiply(1, 0, 1).normalize();
                m.knockback(0.9, -away.x, -away.z);
            }
        }
        if (c.wielder() != null) {                                                       // a wind burst that changes his flight: the wielder is carried along the look direction
            Vec3 v = c.look().normalize().scale(1.2);
            c.wielder().setDeltaMovement(v.x, Math.max(v.y, 0.2), v.z);
            c.wielder().hurtMarked = true;
            c.wielder().fallDistance = 0.0f;
        }
        fx(c.level(), Fx.options(FxThemes.SKILL_SHISHOU, FxOld.CLOUD), from, 18, 1.4);
        sound(c.level(), from, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 0.8f, 1.2f);
        return Result.of(ShishouSkill.WIND_MAGIC.manaCost(), ShishouSkill.WIND_MAGIC.cooldown());
    }

    private static Result earth(Caster c) {
        Vec3 at = aimPoint(c, ShishouRules.SPELL_RANGE);
        double dmg = ShishouRules.spellDamage(c.swordLevel(), 6.0, 0.25, 30.0);
        BlockParticleOption stone = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
        for (Mob m : ShishouTargets.hostilesNear(c.level(), c.wielder(), at, ShishouRules.SPELL_RADIUS * 0.7, ShishouRules.SPELL_TARGETS)) {
            hit(c, m, dmg);
            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 5, false, false));                      // the pitfall: held in place, no blocks moved
            fx(c.level(), stone, m.position().add(0, 0.2, 0), 8, 0.4);
        }
        fx(c.level(), stone, at, 10, 0.8);
        sound(c.level(), at, SoundEvents.STONE_BREAK, 1.0f, 0.8f);
        return Result.of(ShishouSkill.EARTH_MAGIC.manaCost(), ShishouSkill.EARTH_MAGIC.cooldown());
    }

    private static Result waterIce(Caster c) {
        Vec3 at = aimPoint(c, ShishouRules.SPELL_RANGE);
        double dmg = ShishouRules.spellDamage(c.swordLevel(), 4.0, 0.2, 25.0);
        for (Mob m : ShishouTargets.hostilesNear(c.level(), c.wielder(), at, 6.0, ShishouRules.SPELL_TARGETS)) {
            hit(c, m, dmg);
            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2, false, false));
            m.setTicksFrozen(m.getTicksRequiredToFreeze() + 40);
            fx(c.level(), Fx.options(FxThemes.SKILL_SHISHOU, FxOld.SNOWFLAKE), center(m), 8, 0.4);
        }
        if (c.wielder() != null) c.wielder().addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 0, false, false));   // the water barrier: one small shield, no blocks
        fx(c.level(), Fx.options(FxThemes.SKILL_SHISHOU, FxOld.SNOWFLAKE), at, 14, 1.0);
        sound(c.level(), at, SoundEvents.GLASS_BREAK, 0.8f, 1.4f);
        return Result.of(ShishouSkill.WATER_ICE_MAGIC.manaCost(), ShishouSkill.WATER_ICE_MAGIC.cooldown());
    }

    private static Result darkness(Caster c) {
        Vec3 at = aimPoint(c, ShishouRules.SPELL_RANGE);
        for (Mob m : ShishouTargets.hostilesNear(c.level(), c.wielder(), at, ShishouRules.SPELL_RADIUS, ShishouRules.SPELL_TARGETS)) {
            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 5, false, false));                      // bound by shadows: rooted for two seconds
            m.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
            fx(c.level(), DARK, center(m), 6, 0.4);
        }
        fx(c.level(), Fx.options(FxThemes.SKILL_SHISHOU, FxTone.DARK), at, 6, 0.8);
        sound(c.level(), at, SoundEvents.WITHER_SHOOT, 0.5f, 1.6f);
        return Result.of(ShishouSkill.DARKNESS_MAGIC.manaCost(), ShishouSkill.DARKNESS_MAGIC.cooldown());
    }

    private static Result healing(Caster c) {
        ServerPlayer p = c.wielder();
        if (p == null || p.getHealth() >= p.getMaxHealth() && !hasHarmful(p)) return Result.FAIL;
        p.heal((float) Math.min(16.0, 4.0 + 0.15 * c.swordLevel()));
        cleanse(p, false);
        fx(c.level(), Fx.options(FxThemes.SKILL_SHISHOU, FxOld.HAPPY_VILLAGER), p.position().add(0, 1, 0), 8, 0.5);
        fx(c.level(), Fx.options(FxThemes.SKILL_SHISHOU, FxTone.LIGHT), p.position().add(0, 1, 0), 4, 0.5);
        return Result.of(ShishouSkill.HEALING_MAGIC.manaCost(), ShishouSkill.HEALING_MAGIC.cooldown());
    }

    private static Result purification(Caster c) {
        ServerPlayer p = c.wielder();
        if (p == null || !hasHarmful(p)) return Result.FAIL;
        cleanse(p, true);
        fx(c.level(), Fx.options(FxThemes.SKILL_SHISHOU, FxOld.END_ROD), p.position().add(0, 1, 0), 10, 0.6);
        fx(c.level(), Fx.options(FxThemes.SKILL_SHISHOU, FxTone.LIGHT), p.position().add(0, 1, 0), 4, 0.5);
        return Result.of(ShishouSkill.PURIFICATION.manaCost(), ShishouSkill.PURIFICATION.cooldown());
    }

    private static boolean hasHarmful(LivingEntity e) {
        for (MobEffectInstance i : e.getActiveEffects()) if (curse(i)) return true;
        return false;
    }

    private static boolean curse(MobEffectInstance i) {
        if (i.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) return true;
        var key = i.getEffect().unwrapKey();
        return key.isPresent() && (key.get().location().getPath().contains("curse") || key.get().location().getPath().contains("doom"));
    }

    /** Removes the harmful effects (all of them for Purification, which also takes the curse-named ones; Healing takes the harmful ones too). It never touches the food or hunger. */
    private static void cleanse(ServerPlayer p, boolean full) {
        for (MobEffectInstance i : new ArrayList<>(p.getActiveEffects())) {
            if (!curse(i)) continue;
            var key = i.getEffect().unwrapKey();
            if (key.isPresent() && key.get().location().getPath().equals("hunger")) continue;                       // hunger is not ours to touch either way
            p.removeEffect(i.getEffect());
        }
    }

    /** A line of particles from the caster toward a point (at most {@code n} dots). */
    private static void line(Caster c, Vec3 to, ParticleOptions p, int n) {
        Vec3 d = to.subtract(c.origin());
        for (int i = 1; i <= n; i++) fx(c.level(), p, c.origin().add(d.scale(i / (double) n)), 1, 0.05);
    }
}
