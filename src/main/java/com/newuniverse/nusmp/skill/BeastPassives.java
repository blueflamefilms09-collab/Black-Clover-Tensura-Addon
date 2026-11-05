package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.skill.rules.BeastSkillRules;
import com.newuniverse.nusmp.skill.rules.BeastSkillRules.Passive;
import com.newuniverse.nusmp.skill.rules.BeastSkillRules.Spec;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
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

/**
 * 3.58.0: the passives of the Beast Will skills. {@link #second}: once a second per owner whose skill is toggled ON, the passives of the form (attribute modifiers, timed effects, at most
 * {@link BeastSkillRules#AURA_FOES} foes or {@link BeastSkillRules#BOND_ALLIES} allies touched per aura, one entity query per aura); nothing at all when the skill is off. {@link #onDamage}: one listener that
 * returns at once unless the victim is a player carrying the form tag (the illusion dodge). All state is the owner's own persistent tag and transient attribute modifiers; {@link #clear} removes it all
 * when the skill is toggled off, forgotten, disabled by the server setting or the owner leaves. Server thread only.
 */
public final class BeastPassives {
    private BeastPassives() {}

    public static final String FORM = "nusmp_bws_form", ULT = "nusmp_bws_ult";

    private static final ResourceLocation SPEED = id("bw_speed"), ATTACK_SPEED = id("bw_attack_speed"), DAMAGE = id("bw_damage"), ARMOR = id("bw_armor"), KNOCK = id("bw_knock");

    private static ResourceLocation id(String p) { return ResourceLocation.fromNamespaceAndPath("nusmp", p); }

    public static void register(IEventBus bus) { bus.addListener(BeastPassives::onDamage); }

    // ---------------------------------------------------------------- once a second
    static void second(ManasSkillInstance inst, ServerPlayer p, Spec spec, boolean ult, boolean enabled) {
        if (!enabled || !inst.isToggled() || !p.isAlive()) { clear(p); return; }
        CompoundTag d = p.getPersistentData();
        d.putString(FORM, spec.key());
        d.putBoolean(ULT, ult);
        double k = BeastSkillRules.power(spec, ult), speed = 0, atkSpeed = 0, dmg = 0, armor = 0, kb = 0;
        for (Passive ps : spec.passivesOf(ult)) {
            switch (ps.kind()) {
                case SWIFT -> { speed += ps.amount() * k; atkSpeed += ps.amount() * k * 0.5; }
                case MIGHT -> dmg += ps.amount() * k;
                case ARMOR -> { armor += ps.amount() * k; kb += Math.min(0.8, ps.amount() * k / 10.0); }
                case REGEN -> {
                    if (p.getHealth() < p.getMaxHealth()) p.heal((float) (p.getMaxHealth() * ps.amount() * k));
                    if (p.tickCount % 100 == 0) SkillUtil.cleanse(p);
                }
                case STEALTH -> { if (p.isShiftKeyDown() && p.hurtTime == 0) p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, false)); }
                case VENOM -> aura(p, ps.radius(), MobEffects.POISON, 60, ult ? 1 : 0);
                case BLIGHT -> aura(p, ps.radius(), MobEffects.WITHER, 40, 0);
                case FROST -> aura(p, ps.radius(), MobEffects.MOVEMENT_SLOWDOWN, 40, ult ? 1 : 0);
                case SCORCH -> {
                    p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, false, false, false));
                    int n = 0;
                    for (LivingEntity t : SkillTargets.foes(p, p.position(), ps.radius())) {
                        if (n++ >= BeastSkillRules.AURA_FOES) break;
                        t.setRemainingFireTicks(Math.max(t.getRemainingFireTicks(), 40));
                    }
                }
                case FLIGHT -> {
                    p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false, false));
                    p.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 1, false, false, false));
                }
                case FURY -> {
                    if (BeastSkillRules.lowHealth(p.getHealth(), p.getMaxHealth())) {
                        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, ult ? 2 : 1, false, false, true));
                        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 1, false, false, true));
                    }
                }
                case BOND -> {
                    int n = 0;
                    for (LivingEntity a : SkillTargets.alliesAround(p, p.position(), ps.radius())) {
                        if (a == p) continue;
                        if (n++ >= BeastSkillRules.BOND_ALLIES) break;
                        a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, false, false, false));
                    }
                }
                case TIDE -> {
                    p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, false, false, false));
                    p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 60, 0, false, false, false));
                }
                case SENSE -> {
                    int n = 0;
                    for (LivingEntity t : SkillTargets.foes(p, p.position(), ps.radius())) {
                        if (n++ >= BeastSkillRules.AURA_FOES * 2) break;
                        t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false, false));
                    }
                }
                default -> { }   // ILLUSION is the damage listener's
            }
        }
        set(p, Attributes.MOVEMENT_SPEED, SPEED, speed, Operation.ADD_MULTIPLIED_TOTAL);
        set(p, Attributes.ATTACK_SPEED, ATTACK_SPEED, atkSpeed, Operation.ADD_MULTIPLIED_TOTAL);
        set(p, Attributes.ATTACK_DAMAGE, DAMAGE, dmg, Operation.ADD_VALUE);
        set(p, Attributes.ARMOR, ARMOR, armor, Operation.ADD_VALUE);
        set(p, Attributes.KNOCKBACK_RESISTANCE, KNOCK, kb, Operation.ADD_VALUE);
    }

    /** An effect on the foes within the radius, at most {@link BeastSkillRules#AURA_FOES} of them. */
    private static void aura(ServerPlayer p, double radius, Holder<net.minecraft.world.effect.MobEffect> effect, int ticks, int amp) {
        int n = 0;
        for (LivingEntity t : SkillTargets.foes(p, p.position(), radius)) {
            if (n++ >= BeastSkillRules.AURA_FOES) break;
            t.addEffect(new MobEffectInstance(effect, ticks, amp, false, true, false));
        }
    }

    /** Sets a transient modifier to {@code amount}, or removes it when the amount is zero. */
    private static void set(ServerPlayer p, Holder<Attribute> attr, ResourceLocation id, double amount, Operation op) {
        AttributeInstance a = p.getAttribute(attr);
        if (a == null) return;
        AttributeModifier cur = a.getModifier(id);
        if (amount <= 0) { if (cur != null) a.removeModifier(id); return; }
        if (cur != null && Math.abs(cur.amount() - amount) < 1.0E-6) return;
        a.addOrUpdateTransientModifier(new AttributeModifier(id, amount, op));
    }

    /** Removes everything this file put on the player: the form tag and the modifiers. */
    public static void clear(ServerPlayer p) {
        CompoundTag d = p.getPersistentData();
        if (!d.contains(FORM)) return;
        d.remove(FORM);
        d.remove(ULT);
        set(p, Attributes.MOVEMENT_SPEED, SPEED, 0, Operation.ADD_MULTIPLIED_TOTAL);
        set(p, Attributes.ATTACK_SPEED, ATTACK_SPEED, 0, Operation.ADD_MULTIPLIED_TOTAL);
        set(p, Attributes.ATTACK_DAMAGE, DAMAGE, 0, Operation.ADD_VALUE);
        set(p, Attributes.ARMOR, ARMOR, 0, Operation.ADD_VALUE);
        set(p, Attributes.KNOCKBACK_RESISTANCE, KNOCK, 0, Operation.ADD_VALUE);
    }

    /** The cooldown of a mode in seconds, after the server's cooldown multiplier. */
    static int cooldownSeconds(int base) {
        if (base <= 0) return 0;
        return (int) Math.max(1, Math.round(DMUtil.cooldown(base * 20) / 20.0));
    }

    // ---------------------------------------------------------------- the damage event
    static void onDamage(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer v) || e.getAmount() <= 0f || !v.getPersistentData().contains(FORM)) return;
        Spec s = BeastSkillRules.spec(v.getPersistentData().getString(FORM));
        if (s == null || e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        boolean ult = v.getPersistentData().getBoolean(ULT);
        for (Passive ps : s.passivesOf(ult)) {
            if (ps.kind() != BeastSkillRules.Pas.ILLUSION) continue;
            double chance = BeastSkillRules.dodge(ps, ult) * (e.getSource().is(DamageTypeTags.IS_PROJECTILE) ? 0.8 : 1.0);
            if (v.getRandom().nextDouble() < chance) {
                e.setCanceled(true);
                SkillFx.burst(v.serverLevel(), v.position().add(0, 1, 0), true, 6, 0.4);
                return;
            }
        }
    }
}
