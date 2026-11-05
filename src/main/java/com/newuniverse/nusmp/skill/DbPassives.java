package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.entity.veldanava.skills.JudgmentEffects;
import com.newuniverse.nusmp.entity.veldanava.skills.JudgmentRules;
import com.newuniverse.nusmp.entity.veldanava.skills.TemporalDebt;
import com.newuniverse.nusmp.skill.rules.DbRules;
import com.newuniverse.nusmp.skill.rules.DbRules.Mode;
import com.newuniverse.nusmp.skill.rules.DbRules.Prim;
import com.newuniverse.nusmp.skill.rules.DbRules.Spec;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 3.56.0: the passives of the Dragon Ball skills. Two parts. (1) {@link #second}: once a second per owner whose skill is toggled ON, the passives that need a look (speed, regeneration, sensing, the Royal
 * Authority aura, the reactor, a snapshot to rewind to); at most one entity query of at most 24 creatures per passive, nothing when the skill is off. (2) {@link #onDamage}: one damage-event listener that
 * returns at once unless the attacker or the victim is a player carrying the form tag {@link #FORM} (dodge, God Bind, Latent Rage, Ego's Fuel, the guard, the death save, the Orange damage). All state is
 * the owner's own persistent data and attribute modifiers; {@link #clear} removes it all when the skill is toggled off, forgotten or the owner leaves. Server thread only.
 */
public final class DbPassives {
    private DbPassives() {}

    public static final String FORM = "nusmp_db_form", ULT = "nusmp_db_ult", MASTERED = "nusmp_db_mst", GUARD_UNTIL = "nusmp_db_guard", GUARD_PROJ = "nusmp_db_guard_proj", VEIL_UNTIL = "nusmp_db_veil",
            STORED = "nusmp_db_stored", EGO_STORED = "nusmp_db_ego", DEATH_UNTIL = "nusmp_db_death", BIND_UNTIL = "nusmp_db_bind", ORANGE_ON = "nusmp_db_orange", SNAP_X = "nusmp_db_sx", SNAP_Y = "nusmp_db_sy", SNAP_Z = "nusmp_db_sz", SNAP_DIM = "nusmp_db_sd";

    private static final ResourceLocation SPEED = id("db_speed"), ATTACK = id("db_attack"), FLY = id("db_fly"), KNOCK = id("db_knock"), SCALE = id("db_scale");

    private static ResourceLocation id(String p) { return ResourceLocation.fromNamespaceAndPath("nusmp", p); }

    public static void register(IEventBus bus) { bus.addListener(DbPassives::onDamage); }

    /** The passives a form has: every passive of an Ultimate, the first of a Unique. */
    static List<Mode> forms(Spec s, boolean ult) {
        if (ult) return s.passives();
        Mode f = s.firstPassive();
        return f == null ? List.of() : List.of(f);
    }

    private static Mode find(Spec s, boolean ult, Prim prim) {
        for (Mode m : forms(s, ult)) if (m.move().prim() == prim) return m;
        return null;
    }

    private static double strength(boolean ult, Spec s) { return DbRules.power(s, ult); }

    // ---------------------------------------------------------------- once a second
    static void second(ManasSkillInstance inst, ServerPlayer p, Spec spec, boolean ult, boolean mastered, boolean enabled) {
        if (!enabled || !inst.isToggled() || !p.isAlive()) { clear(p); return; }
        CompoundTag d = p.getPersistentData();
        d.putString(FORM, spec.id());
        d.putBoolean(ULT, ult);
        d.putBoolean(MASTERED, mastered);
        ServerLevel level = p.serverLevel();
        double k = strength(ult, spec);
        boolean orange = false;
        for (Mode m : forms(spec, ult)) {
            DbRules.Move mv = m.move();
            switch (mv.prim()) {
                case SPEED -> { mod(p, Attributes.MOVEMENT_SPEED, SPEED, mv.dmg() * k); mod(p, Attributes.ATTACK_SPEED, ATTACK, mv.range() * k); mod(p, Attributes.FLYING_SPEED, FLY, mv.dmg() * k); }
                case CLEANSE_REGEN -> {
                    if (p.getHealth() < p.getMaxHealth()) p.heal((float) (p.getMaxHealth() * mv.dmg() * k));
                    if (p.tickCount % 100 == 0) SkillUtil.cleanse(p);
                }
                case SENSE -> { for (LivingEntity t : SkillTargets.foes(p, p.position(), mv.radius())) t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false, false)); }
                case AURA -> aura(p, mv, ult);
                case EXTEND -> { extendSelf(p, 60); stealFrom(p, mv.radius()); }
                case STAMINA -> { extendSelf(p, 60); }
                case REACTOR -> reactor(p, k);
                case PACK -> pack(p, mv, k);
                case GENIUS -> genius(p, mv);
                case FLOAT -> { p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false, false)); p.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 1, false, false, false)); }
                case ORANGE -> orange = orange(p, ult);
                case DEATHSAVE -> { if (p.tickCount % 100 == 0 && p.onGround()) snapshot(p); }
                default -> { }
            }
        }
        d.putBoolean(ORANGE_ON, orange);
        if (!orange) { unmod(p, Attributes.KNOCKBACK_RESISTANCE, KNOCK); unmod(p, Attributes.SCALE, SCALE); }
        if (find(spec, ult, Prim.SPEED) == null && find(spec, ult, Prim.PACK) == null) { unmod(p, Attributes.MOVEMENT_SPEED, SPEED); unmod(p, Attributes.ATTACK_SPEED, ATTACK); unmod(p, Attributes.FLYING_SPEED, FLY); }
    }

    private static void mod(ServerPlayer p, Holder<Attribute> attr, ResourceLocation id, double amount) {
        AttributeInstance a = p.getAttribute(attr);
        if (a == null) return;
        AttributeModifier cur = a.getModifier(id);
        if (cur != null && Math.abs(cur.amount() - amount) < 1.0E-6) return;
        a.addOrUpdateTransientModifier(new AttributeModifier(id, amount, Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void unmod(ServerPlayer p, Holder<Attribute> attr, ResourceLocation id) {
        AttributeInstance a = p.getAttribute(attr);
        if (a != null && a.getModifier(id) != null) a.removeModifier(id);
    }

    private static void aura(ServerPlayer p, DbRules.Move mv, boolean ult) {
        int n = 0;
        for (LivingEntity t : SkillTargets.foes(p, p.position(), mv.radius())) {
            if (n++ >= 16) break;
            var look = t.getLookAngle();
            boolean watching = JudgmentRules.looksAt(look.x, look.y, look.z, p.getX() - t.getX(), p.getEyeY() - t.getEyeY(), p.getZ() - t.getZ(), 0.9);
            if (!watching && t.distanceToSqr(p) > 25.0) continue;
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, ult ? 1 : 0, false, true, true));
            t.addEffect(new MobEffectInstance(JudgmentEffects.TEMPORAL_STUTTER, BalanceLaw.controlTicks(t, 30), 0, false, true, true));
            TemporalDebt.add(t, Math.max(1, mv.n()));
        }
    }

    private static void extendSelf(ServerPlayer p, int ticks) {
        int n = 0;
        for (MobEffectInstance e : new ArrayList<>(p.getActiveEffects())) {
            if (n++ >= 6) break;
            if (e.getEffect().value().isBeneficial()) {
                if (e.getDuration() < 1200) p.addEffect(new MobEffectInstance(e.getEffect(), e.getDuration() + ticks, e.getAmplifier(), e.isAmbient(), e.isVisible(), e.showIcon()));
            } else if (e.getDuration() > 40) {
                p.removeEffect(e.getEffect());
                p.addEffect(new MobEffectInstance(e.getEffect(), e.getDuration() - 40, e.getAmplifier(), e.isAmbient(), e.isVisible(), e.showIcon()));
            }
        }
    }

    /** Hermit's Wisdom: one good effect is taken from each foe nearby (at most 12 foes) and lent to the owner for at most 30 seconds. */
    private static void stealFrom(ServerPlayer p, double radius) {
        int n = 0;
        for (LivingEntity t : SkillTargets.foes(p, p.position(), radius)) {
            if (n++ >= 12) break;
            for (MobEffectInstance e : new ArrayList<>(t.getActiveEffects())) {
                if (!e.getEffect().value().isBeneficial()) continue;
                t.removeEffect(e.getEffect());
                p.addEffect(new MobEffectInstance(e.getEffect(), Math.min(600, Math.max(100, e.getDuration())), e.getAmplifier()));
                break;
            }
        }
    }

    private static void reactor(ServerPlayer p, double k) {
        IExistence ex = TensuraStorages.getExistenceFrom(p);
        if (ex != null) {
            double max = EnergyHelper.getMaxMagicule(p);
            if (ex.getMagicule() < max) { ex.setMagicule(Math.min(max, ex.getMagicule() + max * DbRules.REACTOR_SHARE * k)); ex.markDirty(); }
        }
        for (Holder<net.minecraft.world.effect.MobEffect> h : List.of(MobEffects.HUNGER, MobEffects.WEAKNESS, MobEffects.DIG_SLOWDOWN, MobEffects.CONFUSION)) p.removeEffect(h);
    }

    private static void pack(ServerPlayer p, DbRules.Move mv, double k) {
        int allies = 0;
        for (LivingEntity a : SkillTargets.alliesAround(p, p.position(), mv.radius())) if (a != p && allies < mv.n()) allies++;
        double bonus = mv.dmg() * allies * k;
        if (bonus <= 0) { unmod(p, Attributes.MOVEMENT_SPEED, SPEED); unmod(p, Attributes.ATTACK_SPEED, ATTACK); unmod(p, Attributes.FLYING_SPEED, FLY); return; }
        mod(p, Attributes.MOVEMENT_SPEED, SPEED, bonus);
        mod(p, Attributes.ATTACK_SPEED, ATTACK, bonus);
        mod(p, Attributes.FLYING_SPEED, FLY, bonus);
    }

    private static void genius(ServerPlayer p, DbRules.Move mv) {
        p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 60, 0, false, false, false));
        int n = 0;
        for (LivingEntity a : SkillTargets.alliesAround(p, p.position(), mv.radius())) {
            if (a == p || n++ >= DbRules.MAX_TARGETS) continue;
            a.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 60, 0, false, false, false));
        }
    }

    private static boolean orange(ServerPlayer p, boolean ult) {
        if (!DbRules.lowHealth(p.getHealth(), p.getMaxHealth())) return false;
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, ult ? 2 : 1, false, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 1, false, false, true));
        AttributeInstance kb = p.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kb != null && kb.getModifier(KNOCK) == null) kb.addOrUpdateTransientModifier(new AttributeModifier(KNOCK, 1.0, Operation.ADD_VALUE));
        AttributeInstance sc = p.getAttribute(Attributes.SCALE);
        if (sc != null && sc.getModifier(SCALE) == null) sc.addOrUpdateTransientModifier(new AttributeModifier(SCALE, 0.3, Operation.ADD_MULTIPLIED_BASE));
        return true;
    }

    private static void snapshot(ServerPlayer p) {
        CompoundTag d = p.getPersistentData();
        d.putDouble(SNAP_X, p.getX());
        d.putDouble(SNAP_Y, p.getY());
        d.putDouble(SNAP_Z, p.getZ());
        d.putString(SNAP_DIM, p.level().dimension().location().toString());
    }

    /** Removes everything this file put on the player: the form tag, the modifiers, the windows. */
    public static void clear(ServerPlayer p) {
        CompoundTag d = p.getPersistentData();
        if (!d.contains(FORM) && !d.contains(ORANGE_ON)) return;
        for (String k : new String[] {FORM, ULT, MASTERED, GUARD_UNTIL, GUARD_PROJ, VEIL_UNTIL, STORED, EGO_STORED, DEATH_UNTIL, BIND_UNTIL, ORANGE_ON, SNAP_X, SNAP_Y, SNAP_Z, SNAP_DIM}) d.remove(k);
        unmod(p, Attributes.MOVEMENT_SPEED, SPEED);
        unmod(p, Attributes.ATTACK_SPEED, ATTACK);
        unmod(p, Attributes.FLYING_SPEED, FLY);
        unmod(p, Attributes.KNOCKBACK_RESISTANCE, KNOCK);
        unmod(p, Attributes.SCALE, SCALE);
    }

    /** The cooldown of a mode in seconds after Genius Intellect (Bulma's own modes are 30% shorter while it is on). */
    static int cooldownSeconds(int base, ServerPlayer p, Spec spec) {
        if (base <= 0) return 0;
        Mode g = spec.id().equals(p.getPersistentData().getString(FORM)) ? find(spec, p.getPersistentData().getBoolean(ULT), Prim.GENIUS) : null;
        int ticks = DMUtil.cooldown(base * 20);
        double scaled = g != null ? ticks * (1.0 - g.move().dmg()) : ticks;
        return (int) Math.max(1, Math.round(scaled / 20.0));
    }

    // ---------------------------------------------------------------- the damage event
    static void onDamage(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide || e.getAmount() <= 0f) return;
        if (e.getSource().getEntity() instanceof ServerPlayer a && a.getPersistentData().contains(FORM) && a != victim) outgoing(e, a, victim);
        if (victim instanceof ServerPlayer v) {
            if (v.getPersistentData().contains(FORM)) incoming(e, v);
            rage(e, v);
        }
    }

    private static Spec formOf(ServerPlayer p) { return DbRules.spec(p.getPersistentData().getString(FORM)); }

    private static void outgoing(LivingIncomingDamageEvent e, ServerPlayer a, LivingEntity victim) {
        Spec s = formOf(a);
        if (s == null) return;
        CompoundTag d = a.getPersistentData();
        boolean ult = d.getBoolean(ULT);
        float amount = e.getAmount();
        if (d.getBoolean(ORANGE_ON)) amount *= (float) DbRules.ORANGE_DAMAGE;
        double stored = d.getDouble(STORED) + d.getDouble(EGO_STORED);
        if (stored > 0) { amount += (float) stored; d.remove(STORED); d.remove(EGO_STORED); }
        e.setAmount(amount);
        Mode bind = find(s, ult, Prim.BIND);
        long now = a.level().getGameTime();
        if (bind != null && now >= d.getLong(BIND_UNTIL) && a.getRandom().nextFloat() < 0.15f * (ult ? 1f : 0.5f)) {
            d.putLong(BIND_UNTIL, now + 120);
            victim.addEffect(new MobEffectInstance(JudgmentEffects.ACTION_LOCK, BalanceLaw.controlTicks(victim, 30), 0, false, true, true));
        }
    }

    private static void incoming(LivingIncomingDamageEvent e, ServerPlayer v) {
        Spec s = formOf(v);
        if (s == null) return;
        CompoundTag d = v.getPersistentData();
        boolean ult = d.getBoolean(ULT);
        long now = v.level().getGameTime();
        boolean projectile = e.getSource().is(DamageTypeTags.IS_PROJECTILE);
        if (now < d.getLong(GUARD_UNTIL)) {
            if (projectile && d.getBoolean(GUARD_PROJ)) { d.putDouble(STORED, Math.min(DbRules.RAGE_CAP, d.getDouble(STORED) + DbRules.stored(e.getAmount(), DbRules.RAGE_CAP))); e.setCanceled(true); return; }
            e.setAmount(e.getAmount() * 0.4f);
        }
        if (now < d.getLong(VEIL_UNTIL)) { d.remove(VEIL_UNTIL); e.setCanceled(true); return; }
        Mode dodge = find(s, ult, Prim.DODGE);
        if (dodge != null && !e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            double chance = (projectile ? dodge.move().range() : dodge.move().dmg()) * (ult ? 1.0 : DbRules.UNIQUE_FACTOR + 0.25);
            if (v.getRandom().nextDouble() < chance) { e.setCanceled(true); SkillFx.burst(v.serverLevel(), v.position().add(0, 1, 0), false, 6, 0.4); return; }
        }
        Mode save = find(s, ult, Prim.DEATHSAVE);
        if (save != null && e.getAmount() >= v.getHealth() + v.getAbsorptionAmount() && now >= d.getLong(DEATH_UNTIL) && (d.getBoolean(MASTERED) || save.move().range() >= 1.0)) {
            e.setCanceled(true);
            d.putLong(DEATH_UNTIL, now + 20L * DbRules.DEATHSAVE_SECONDS);
            v.setHealth((float) (v.getMaxHealth() * save.move().dmg()));
            SkillUtil.cleanse(v);
            if (d.contains(SNAP_DIM) && d.getString(SNAP_DIM).equals(v.level().dimension().location().toString())) v.teleportTo(d.getDouble(SNAP_X), d.getDouble(SNAP_Y), d.getDouble(SNAP_Z));
            SkillFx.burst(v.serverLevel(), v.position().add(0, 1, 0), false, 20, 0.8);
            SkillUtil.actionbar(v, SkillMsg.t("db.rewound", "The lethal blow is denied: you are rewound."));
            return;
        }
        if (find(s, ult, Prim.EGO) != null && e.getSource().getEntity() instanceof LivingEntity src && src.getMaxHealth() > v.getMaxHealth())
            d.putDouble(EGO_STORED, Math.min(DbRules.EGO_CAP, d.getDouble(EGO_STORED) + DbRules.stored(e.getAmount(), DbRules.EGO_CAP)));
    }

    /** Latent Rage: damage to an ally within reach stores pressure on every Latent Rage owner nearby (one pass over the level's players, only when a player is hurt). */
    private static void rage(LivingIncomingDamageEvent e, ServerPlayer victim) {
        for (ServerPlayer o : victim.serverLevel().players()) {
            if (o == victim || !o.getPersistentData().contains(FORM) || o.distanceToSqr(victim) > 256.0) continue;
            Spec s = formOf(o);
            if (s == null) continue;
            Mode m = find(s, o.getPersistentData().getBoolean(ULT), Prim.RAGE);
            if (m == null || !SkillTargets.alliesAround(o, o.position(), m.move().radius()).contains(victim)) continue;
            o.getPersistentData().putDouble(STORED, Math.min(DbRules.RAGE_CAP, o.getPersistentData().getDouble(STORED) + DbRules.stored(e.getAmount(), DbRules.RAGE_CAP)));
        }
    }
}
