package com.newuniverse.nusmp.relics.wave;

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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;

/**
 * Amulet of Dragon Rage. The command word turns the wearer into an Adult Silver Dragon for two minutes (the tabletop hour), once a day, for
 * a slice of their existence points. The form is a bundle of transient attribute modifiers (armour, bite, speed, flight, a big pool of
 * absorption that is the dragon's own hit points) and the frost breath; the Minecraft model is not swapped, the transformation shows in
 * particles and the pool; in Tensura's terms the body grows by Tensura's own size stats, magic resistance rises, and Physical Attack Resistance,
 * Danger Sense and Magic Sense are lent as one merged form for as long as it lasts. When the dragon's pool is gone the form ends; if the blow left more than the pool could hold the wearer dies, if it
 * left less but enough to kill them they fall unconscious (1 HP, stunned), as the card says. Cold and falls do nothing to the dragon.
 */
final class Wave5Dragon {
    private Wave5Dragon() {}

    static final String ID = "amulet_of_dragon_rage";
    static final String FLAG = "w5_dragon";
    private static final String UNTIL = "form_until", START = "form_start", FLY = "fly_granted";
    private static final int SILVER = 0xC8D8E8;

    // ---------------------------------------------------------------- the state
    static boolean inForm(ServerPlayer p) {
        return Wave5Data.hasPlayerData(p) && Wave5Data.player(p).getLong(UNTIL) > p.level().getGameTime();
    }

    /** Has a form been started that has not been ended yet (even if its time has run out)? */
    static boolean pending(ServerPlayer p) {
        return Wave5Data.hasPlayerData(p) && Wave5Data.player(p).getLong(UNTIL) > 0L;
    }

    // ---------------------------------------------------------------- the abilities
    /** Speak the command word. */
    static void transform(ActCtx c) {
        ServerPlayer p = c.player();
        if (inForm(p)) {
            Wave5Aim.msg(p, "You already wear the dragon's shape.", ChatFormatting.GRAY);
            return;
        }
        long now = p.level().getGameTime();
        CompoundTag d = Wave5Data.player(p);
        d.putLong(START, now);
        d.putLong(UNTIL, now + Wave5Pure.FORM_TICKS);
        apply(p, true);
        Wave5Skills.lendAll(p, 60, Wave5Skills.PHYSICAL_RESIST, Wave5Skills.DANGER_SENSE, Wave5Skills.MAGIC_SENSE);
        p.setAbsorptionAmount(Wave5Pure.DRAGON_POOL);
        ServerLevel l = p.serverLevel();
        Fx.burst(l, p.position().add(0, 1.0, 0), SILVER);
        Fx.ring(l, p.position().add(0, 0.2, 0), 3.0, SILVER);
        Wave5Aim.puff(l, p.position().add(0, 1.0, 0), ParticleTypes.END_ROD, 30, 0.9);
        Wave5Aim.sound(l, p.position(), SoundEvents.ENDER_DRAGON_GROWL, 1.0f, 1.3f);
        Wave5Aim.msg(p, "The command word: you are an Adult Silver Dragon. Two minutes. Your hit points are the dragon's now.", ChatFormatting.AQUA);
    }

    /** Speak the command word again: back to your own shape. */
    static void speak(ActCtx c) {
        ServerPlayer p = c.player();
        if (!inForm(p)) {
            Wave5Aim.msg(p, "You are not a dragon.", ChatFormatting.GRAY);
            return;
        }
        revert(p, "You speak the word and settle back into your own shape.");
    }

    /** Silver frost breath: a cone of cold in front of the dragon. */
    static void breath(ActCtx c) {
        ServerPlayer p = c.player();
        if (!inForm(p)) {
            Wave5Aim.msg(p, "Only the dragon can breathe frost.", ChatFormatting.GRAY);
            return;
        }
        ServerLevel l = p.serverLevel();
        Vec3 from = Wave5Aim.muzzle(p);
        Vec3 look = p.getLookAngle();
        for (int i = 1; i <= 5; i++) Wave5Aim.puff(l, from.add(look.scale(9.0 * i / 5.0)), ParticleTypes.SNOWFLAKE, 10, 0.25 * i);
        Wave5Aim.sound(l, from, SoundEvents.ENDER_DRAGON_GROWL, 0.8f, 1.8f);
        List<LivingEntity> hit = Wave5Aim.cone(p, 9.0, 0.7, 8);
        for (LivingEntity e : hit) {
            Fx.damage(p, e, Wave5Pure.cap(6), "minecraft:freeze");
            Fx.addEffect(e, "minecraft:slowness", 80, 2);
            e.setTicksFrozen(Math.max(e.getTicksFrozen(), 100));
        }
    }

    // ---------------------------------------------------------------- putting the form on and taking it off
    private static void apply(ServerPlayer p, boolean on) {
        Wave5Attr.sync(p, Attributes.ARMOR, Wave5Attr.id("dragon_armor"), 8.0, Operation.ADD_VALUE, on);
        Wave5Attr.sync(p, Attributes.ARMOR_TOUGHNESS, Wave5Attr.id("dragon_toughness"), 4.0, Operation.ADD_VALUE, on);
        Wave5Attr.sync(p, Attributes.ATTACK_DAMAGE, Wave5Attr.id("dragon_bite"), 5.0, Operation.ADD_VALUE, on);
        Wave5Attr.sync(p, Attributes.MOVEMENT_SPEED, Wave5Attr.id("dragon_speed"), 0.25, Operation.ADD_MULTIPLIED_BASE, on);
        Wave5Attr.sync(p, Attributes.KNOCKBACK_RESISTANCE, Wave5Attr.id("dragon_weight"), 0.8, Operation.ADD_VALUE, on);
        Wave5Attr.sync(p, Attributes.MAX_ABSORPTION, Wave5Attr.id("dragon_pool"), Wave5Pure.DRAGON_POOL, Operation.ADD_VALUE, on);
        Wave5Attr.sync(p, Attributes.STEP_HEIGHT, Wave5Attr.id("dragon_step"), 1.0, Operation.ADD_VALUE, on);
        Wave5Attr.sync(p, Attributes.SAFE_FALL_DISTANCE, Wave5Attr.id("dragon_fall"), 32.0, Operation.ADD_VALUE, on);
        // the Tensura half of the transformation: the body itself grows (Tensura's own size stats; "Speak the Word Again" is the way out of a tight spot) and magic runs off the scales
        Wave5Attr.sync(p, TensuraAttributes.HEIGHT_MULTIPLIER, Wave5Attr.id("dragon_height"), 0.3, Operation.ADD_MULTIPLIED_BASE, on);
        Wave5Attr.sync(p, TensuraAttributes.WIDTH_MULTIPLIER, Wave5Attr.id("dragon_width"), 0.3, Operation.ADD_MULTIPLIED_BASE, on);
        Wave5Attr.sync(p, TensuraAttributes.MAGIC_RESISTANCE, Wave5Attr.id("dragon_magic"), 0.3, Operation.ADD_VALUE, on);
        flight(p, on);
    }

    /** Wings: NeoForge's creative-flight attribute; if a build lacks it on players, the abilities are set by hand and cleaned up the same way. */
    private static void flight(ServerPlayer p, boolean on) {
        if (p.getAttribute(NeoForgeMod.CREATIVE_FLIGHT) != null) {
            Wave5Attr.sync(p, NeoForgeMod.CREATIVE_FLIGHT, Wave5Attr.id("dragon_wings"), 1.0, Operation.ADD_VALUE, on);
            return;
        }
        if (p.isCreative() || p.isSpectator()) return;
        CompoundTag d = Wave5Data.player(p);
        Abilities a = p.getAbilities();
        if (on) {
            a.mayfly = true;
            d.putBoolean(FLY, true);
            p.onUpdateAbilities();
        } else if (d.getBoolean(FLY)) {
            a.mayfly = false;
            a.flying = false;
            d.remove(FLY);
            p.onUpdateAbilities();
        }
    }

    /** End the form (time, the word, death, a relog): modifiers off, flight off, the pool gone. */
    static void revert(ServerPlayer p, String message) {
        if (Wave5Data.hasPlayerData(p)) {
            CompoundTag d = Wave5Data.player(p);
            d.putLong(UNTIL, 0L);
            d.putLong(START, 0L);
        }
        apply(p, false);
        p.setAbsorptionAmount(0.0f);
        Wave5Attr.clamp(p);
        if (message != null) {
            ServerLevel l = p.serverLevel();
            Fx.ring(l, p.position().add(0, 0.2, 0), 2.0, SILVER);
            Wave5Aim.puff(l, p.position().add(0, 1.0, 0), ParticleTypes.END_ROD, 16, 0.7);
            Wave5Aim.msg(p, message, ChatFormatting.AQUA);
        }
    }

    /** A login or a respawn: whatever the old body had on is not on this one (the modifiers are transient, the stored times are not). */
    static void cleanup(ServerPlayer p) {
        if (!Wave5Data.hasPlayerData(p)) return;
        CompoundTag d = Wave5Data.player(p);
        if (d.getLong(UNTIL) > 0L || d.getBoolean(FLY)) revert(p, null);
    }

    // ---------------------------------------------------------------- ticking and taking hits
    /** Five times a second while in the form: time, the pool, the dragon's glitter. */
    static void tick(ServerPlayer p) {
        long now = p.level().getGameTime();
        CompoundTag d = Wave5Data.player(p);
        long until = d.getLong(UNTIL);
        if (until <= 0L) return;
        if (now >= until || !RelicFlags.has(p, FLAG)) {
            revert(p, "The dragon fades; you are yourself again.");
            return;
        }
        if (now - d.getLong(START) > 10L && p.getAbsorptionAmount() <= 0.01f) {
            revert(p, "The dragon's strength is spent: its shape breaks and leaves you reeling.");
            stun(p);
            return;
        }
        if (p.tickCount % 20 == 0) {
            // the dragon's senses and scales as Tensura skills, merged into the one form for as long as it lasts
            Wave5Skills.lendAll(p, 60, Wave5Skills.PHYSICAL_RESIST, Wave5Skills.DANGER_SENSE, Wave5Skills.MAGIC_SENSE);
            Wave5Aim.puff(p.serverLevel(), p.position().add(0, 1.2, 0), ParticleTypes.END_ROD, 6, 0.8);
            Wave5Aim.puff(p.serverLevel(), p.position().add(0, 1.0, 0), ParticleTypes.SNOWFLAKE, 6, 1.0);
        }
    }

    private static void stun(ServerPlayer p) {
        Fx.addEffect(p, "minecraft:slowness", 80, 3);
        Fx.addEffect(p, "minecraft:weakness", 100, 1);
    }

    /** Cold and falls do nothing to the silver dragon. */
    static void onIncoming(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !inForm(p)) return;
        if (e.getSource().is(DamageTypeTags.IS_FREEZING) || e.getSource().is(DamageTypeTags.IS_FALL)) e.setCanceled(true);
    }

    /** The card's rule for a blow that empties the dragon: see {@link Wave5Pure#dragonHit}. True when the blow was turned into the fall. */
    static boolean onHit(LivingDamageEvent.Pre e, ServerPlayer p) {
        if (!inForm(p)) return false;
        Wave5Pure.Lethal r = Wave5Pure.dragonHit(e.getNewDamage(), p.getAbsorptionAmount(), p.getHealth(), Wave5Pure.DRAGON_POOL);
        if (r != Wave5Pure.Lethal.UNCONSCIOUS) return false;                   // NONE and REVERT: the pool takes it and the tick ends the form; DIE: the blow stands
        e.setNewDamage(Wave5Pure.leaveOneHp(p.getAbsorptionAmount(), p.getHealth()));
        stun(p);
        Wave5Aim.msg(p, "The dragon falls and you with it: unconscious, hanging on at 1 HP.", ChatFormatting.RED);
        return true;
    }
}
