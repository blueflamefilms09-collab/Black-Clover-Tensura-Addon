package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicFlags;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import java.util.List;

/**
 * Hemlock's Amulet. The Mask of the Living (an ability, paid in mana) hides the wearer from the dead for a few minutes: undead creatures can not
 * take them as a target and let go of them, and detecting effects (Glowing) do not take hold. The amulet drinks energy (undead slain, and spells
 * that strike the wearer) and grows: Awakened (+hearts, +toughness, a longer mask) and Exalted (more of both, and the Reliquary: it holds the
 * wearer's soul and keeps them in the world through one lethal blow a day). The energy is kept in the amulet itself. In Tensura's terms the
 * mask raises the wearer's presence concealment (and lends the Presence Concealment skill while it lasts) and a grown amulet holds more of the
 * wearer's spiritual health.
 */
final class Wave5Hemlock {
    private Wave5Hemlock() {}

    static final String ID = "hemlocks_amulet";
    static final String FLAG = "w5_hemlock";
    private static final String VEIL = "veil_until";
    private static final int SWEEP_CAP = 16;
    private static final int MASK_COLOUR = 0xB050D8;

    // ---------------------------------------------------------------- the mask
    static void don(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack s = Wave5Data.live(p, ID, c.stack());
        if (s.isEmpty()) return;
        long now = p.level().getGameTime();
        CompoundTag d = Wave5Data.player(p);
        if (d.getLong(VEIL) > now) {
            Wave5Aim.msg(p, "The mask is already on.", ChatFormatting.GRAY);
            return;
        }
        int tier = Wave5Pure.tier(Wave5Data.stack(s).getInt("energy"));
        d.putLong(VEIL, now + Wave5Pure.veilTicks(tier));
        ServerLevel l = p.serverLevel();
        Fx.ring(l, p.position().add(0, 0.2, 0), 1.6, MASK_COLOUR);
        Wave5Aim.sound(l, p.position(), SoundEvents.ILLUSIONER_CAST_SPELL, 0.8f, 0.7f);
        Wave5Aim.msg(p, "You wear the face of a living man: the dead can not find you for " + Wave5Pure.veilTicks(tier) / 1200 + " minutes.", ChatFormatting.LIGHT_PURPLE);
    }

    static void drop(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag d = Wave5Data.player(p);
        if (d.getLong(VEIL) <= p.level().getGameTime()) {
            Wave5Aim.msg(p, "You are not wearing the mask.", ChatFormatting.GRAY);
            return;
        }
        d.putLong(VEIL, 0L);
        Wave5Aim.msg(p, "You let the mask fall: the dead may see you again.", ChatFormatting.GRAY);
    }

    static boolean veiled(ServerPlayer p) {
        return Wave5Data.hasPlayerData(p) && Wave5Data.player(p).getLong(VEIL) > p.level().getGameTime() && RelicFlags.has(p, FLAG);
    }

    /** An undead creature is about to take a masked wearer as its target: it does not. */
    static void onChangeTarget(LivingChangeTargetEvent e) {
        if (!(e.getNewAboutToBeSetTarget() instanceof ServerPlayer p)) return;
        if (!e.getEntity().getType().is(EntityTypeTags.UNDEAD)) return;
        if (veiled(p)) e.setCanceled(true);
    }

    /** Detecting effects do not take hold on a masked wearer. */
    static void onEffect(MobEffectEvent.Applicable e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.getEffectInstance() == null) return;
        if (!e.getEffectInstance().getEffect().equals(MobEffects.GLOWING)) return;
        if (veiled(p)) e.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }

    // ---------------------------------------------------------------- the slow tick: tier bonuses, the veil's sweep and expiry
    static void tick(ServerPlayer p) {
        boolean worn = RelicFlags.has(p, FLAG);
        int tier = 0;
        if (worn) {
            ItemStack s = Wave5Data.live(p, ID);
            if (!s.isEmpty()) tier = Wave5Pure.tier(Wave5Data.stack(s).getInt("energy"));
        }
        Wave5Attr.sync(p, Attributes.MAX_HEALTH, Wave5Attr.id("hemlock_health"), Wave5Pure.healthBonus(tier), Operation.ADD_VALUE, tier > 0);
        Wave5Attr.sync(p, Attributes.ARMOR_TOUGHNESS, Wave5Attr.id("hemlock_toughness"), Wave5Pure.toughnessBonus(tier), Operation.ADD_VALUE, tier > 0);
        Wave5Attr.sync(p, TensuraAttributes.MAX_SPIRITUAL_HEALTH, Wave5Attr.id("hemlock_soul"), Wave5Pure.soulBonus(tier), Operation.ADD_MULTIPLIED_BASE, tier > 0);
        if (tier == 0) Wave5Attr.clamp(p);
        long now = p.level().getGameTime();
        CompoundTag d = Wave5Data.hasPlayerData(p) ? Wave5Data.player(p) : null;
        long until = d == null ? 0L : d.getLong(VEIL);
        boolean veil = worn && until > now;
        // the mask in Tensura's terms: the wearer's presence is concealed from every sense (the skill is lent while it lasts, the stat is raised either way)
        Wave5Attr.sync(p, TensuraAttributes.PRESENCE_CONCEALMENT, Wave5Attr.id("hemlock_veil"), 0.5, Operation.ADD_VALUE, veil);
        if (veil) Wave5Skills.lend(p, Wave5Skills.PRESENCE_CONCEALMENT, 60);
        if (d == null || until <= 0L) return;
        if (!worn || now >= until) {
            d.putLong(VEIL, 0L);
            Wave5Aim.msg(p, "The mask thins away.", ChatFormatting.GRAY);
            return;
        }
        List<Mob> hunters = p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(24.0),
                m -> m.isAlive() && m.getType().is(EntityTypeTags.UNDEAD) && m.getTarget() == p);
        int n = 0;
        for (Mob m : hunters) {
            if (n++ >= SWEEP_CAP) break;
            forget(m);
        }
    }

    private static void forget(Mob m) {
        try {
            m.setTarget(null);
            m.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            m.getNavigation().stop();
        } catch (RuntimeException | LinkageError otherBrain) {
            // a brain of another shape: the plain target slot is already clear
        }
    }

    // ---------------------------------------------------------------- feeding it
    static void onDeath(LivingDeathEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || !RelicFlags.has(p, FLAG)) return;
        LivingEntity dead = e.getEntity();
        if (!dead.getType().is(EntityTypeTags.UNDEAD)) return;
        feed(p, BalanceLaw.isBoss(dead) ? 12 : 2);
    }

    /** The amulet drinks a spell that strikes its wearer (at most one sip every five seconds). */
    static void onIncoming(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !RelicFlags.has(p, FLAG)) return;
        DamageSource src = e.getSource();
        if (!(src.is(DamageTypes.MAGIC) || src.is(DamageTypes.INDIRECT_MAGIC) || src.is(DamageTypes.WITHER))) return;
        ItemStack s = Wave5Data.live(p, ID);
        if (s.isEmpty()) return;
        CompoundTag st = Wave5Data.stack(s);
        long now = p.level().getGameTime();
        if (now < st.getLong("sip_ready")) return;
        st.putLong("sip_ready", now + 100L);
        Wave5Data.save(s, st);
        feed(p, 1);
    }

    static void feed(ServerPlayer p, int gain) {
        ItemStack s = Wave5Data.live(p, ID);
        if (s.isEmpty()) return;
        CompoundTag st = Wave5Data.stack(s);
        int before = st.getInt("energy");
        int after = Wave5Pure.addEnergy(before, gain);
        if (after == before) return;
        st.putInt("energy", after);
        Wave5Data.save(s, st);
        int was = Wave5Pure.tier(before), now = Wave5Pure.tier(after);
        if (now > was) {
            ServerLevel l = p.serverLevel();
            Fx.burst(l, p.position().add(0, 1.0, 0), MASK_COLOUR);
            Wave5Aim.sound(l, p.position(), SoundEvents.BEACON_ACTIVATE, 0.9f, 0.6f);
            Wave5Aim.msg(p, now == 1
                    ? "Hemlock's Amulet is Awakened: it has drunk enough to hold more than it did."
                    : "Hemlock's Amulet is Exalted: it has become the Reliquary of your soul.", ChatFormatting.DARK_PURPLE);
        }
    }

    // ---------------------------------------------------------------- the Reliquary
    /** An Exalted amulet holds the soul of its wearer: one lethal blow a day is turned into a fall to 40% of their health. True when it saved them. */
    static boolean reliquary(LivingDamageEvent.Pre e, ServerPlayer p) {
        if (!RelicFlags.has(p, FLAG)) return false;
        DamageSource src = e.getSource();
        if (src.is(DamageTypes.GENERIC_KILL) || src.is(DamageTypes.FELL_OUT_OF_WORLD)) return false;
        ItemStack s = Wave5Data.live(p, ID);
        if (s.isEmpty()) return false;
        CompoundTag st = Wave5Data.stack(s);
        if (Wave5Pure.tier(st.getInt("energy")) < 2) return false;
        long now = p.level().getGameTime();
        if (now < st.getLong("reliq_ready")) return false;
        e.setNewDamage(0f);
        st.putLong("reliq_ready", now + Wave5Pure.RELIQUARY_COOLDOWN);
        Wave5Data.save(s, st);
        p.setHealth(Math.max(1.0f, p.getMaxHealth() * 0.4f));
        p.setRemainingFireTicks(0);
        Fx.addEffect(p, "minecraft:resistance", 120, 3);
        Fx.addEffect(p, "minecraft:weakness", 1800, 0);          // soul-torn: weak for a minute and a half
        Fx.addEffect(p, "minecraft:slowness", 600, 0);
        ServerLevel l = p.serverLevel();
        Fx.burst(l, p.position().add(0, 1.0, 0), MASK_COLOUR);
        Fx.ring(l, p.position().add(0, 0.2, 0), 2.5, MASK_COLOUR);
        Wave5Aim.puff(l, p.position().add(0, 1.0, 0), ParticleTypes.SOUL, 24, 0.6);
        Wave5Aim.sound(l, p.position(), SoundEvents.TOTEM_USE, 1.0f, 0.6f);
        Wave5Aim.msg(p, "The Reliquary holds your soul in the world: you rise, soul-torn. It will not do this again for a day.", ChatFormatting.DARK_PURPLE);
        return true;
    }
}
