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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Amulet of the Warmage: four charges (one cantrip each) from the Wizard's list, and 1d4 charges back when a new day begins. The four cantrips
 * are four abilities of the amulet that draw on one shared pool kept in the stack. The Force resistance, the deeper magicule well and the lent
 * Magic Sense are passives (see Wave5.defs); the rest of the Arcana bonus (a faster magicule stream, cheaper spells, magic
 * resistance) is Tensura's, kept in step once a second by {@link #tick}.
 */
final class Wave5Warmage {
    private Wave5Warmage() {}

    static final String ID = "amulet_of_the_warmage";
    static final String FLAG = "w5_warmage";
    static final int FIRE_BOLT = 0, RAY_OF_FROST = 1, SHOCKING_GRASP = 2, ACID_SPLASH = 3;

    /** Arcana, the Tensura half (once a second): a faster magicule stream, cheaper spells, magic resistance. */
    static void tick(ServerPlayer p) {
        boolean worn = RelicFlags.has(p, FLAG);
        Wave5Attr.sync(p, TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER, Wave5Attr.id("warmage_regen"), 0.10, Operation.ADD_MULTIPLIED_BASE, worn);
        Wave5Attr.sync(p, TensuraAttributes.MAGIC_COST_MULTIPLIER, Wave5Attr.id("warmage_cost"), -0.05, Operation.ADD_MULTIPLIED_BASE, worn);   // Arcana: spells cost a little less
        Wave5Attr.sync(p, TensuraAttributes.MAGIC_RESISTANCE, Wave5Attr.id("warmage_resist"), 0.10, Operation.ADD_VALUE, worn);                // Force resistance, the Tensura way
    }

    /** One cantrip: finds a target, spends a charge only if it was cast. */
    static void cast(ActCtx c, int which) {
        ServerPlayer p = c.player();
        ItemStack s = Wave5Data.live(p, ID, c.stack());
        if (s.isEmpty()) return;
        CompoundTag st = Wave5Data.stack(s);
        long today = Wave5Pure.day(p.serverLevel().getServer().overworld().getDayTime());
        int charges = st.contains("charges") ? st.getInt("charges") : Wave5Pure.WARMAGE_MAX;
        long last = st.contains("day") ? st.getLong("day") : today;
        charges = Wave5Pure.regen(charges, Wave5Pure.WARMAGE_MAX, last, today, 1 + p.getRandom().nextInt(Wave5Pure.WARMAGE_DIE));
        st.putInt("charges", charges);
        st.putLong("day", today);
        if (!Wave5Pure.canAfford(charges, 1)) {
            Wave5Data.save(s, st);
            Wave5Aim.msg(p, "The amulet is spent: it gives back 1d4 charges when a new day begins.", ChatFormatting.GRAY);
            return;
        }
        boolean cast = switch (which) {
            case FIRE_BOLT -> fireBolt(p);
            case RAY_OF_FROST -> rayOfFrost(p);
            case SHOCKING_GRASP -> shockingGrasp(p);
            case ACID_SPLASH -> acidSplash(p);
            default -> false;
        };
        if (cast) {
            charges--;
            st.putInt("charges", charges);
            Wave5Aim.msg(p, "Warmage's amulet: " + charges + "/" + Wave5Pure.WARMAGE_MAX + " charges", ChatFormatting.AQUA);
        }
        Wave5Data.save(s, st);
    }

    private static LivingEntity need(ServerPlayer p, double range) {
        LivingEntity t = Wave5Aim.pickFoe(p, range);
        if (t == null) Wave5Aim.msg(p, "No foe in your sights.", ChatFormatting.GRAY);
        return t;
    }

    private static boolean fireBolt(ServerPlayer p) {
        LivingEntity t = need(p, 24);
        if (t == null) return false;
        ServerLevel l = p.serverLevel();
        Vec3 to = t.getBoundingBox().getCenter();
        Wave5Aim.beam(l, Wave5Aim.muzzle(p), to, com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), 20);
        Wave5Aim.puff(l, to, com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), 6, 0.25);
        Wave5Aim.sound(l, to, SoundEvents.FIRECHARGE_USE, 1.0f, 1.2f);
        Fx.damage(p, t, Wave5Pure.cantrip(4, p.experienceLevel), "minecraft:in_fire");
        t.igniteForSeconds(3);
        return true;
    }

    private static boolean rayOfFrost(ServerPlayer p) {
        LivingEntity t = need(p, 24);
        if (t == null) return false;
        ServerLevel l = p.serverLevel();
        Vec3 to = t.getBoundingBox().getCenter();
        Wave5Aim.beam(l, Wave5Aim.muzzle(p), to, com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), 24);
        Wave5Aim.puff(l, to, com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), 10, 0.3);
        Wave5Aim.sound(l, to, SoundEvents.GLASS_BREAK, 0.7f, 1.6f);
        Fx.damage(p, t, Wave5Pure.cantrip(3, p.experienceLevel), "minecraft:freeze");
        Fx.addEffect(t, "minecraft:slowness", 100, 1);
        return true;
    }

    private static boolean shockingGrasp(ServerPlayer p) {
        LivingEntity t = need(p, 4.5);
        if (t == null) return false;
        ServerLevel l = p.serverLevel();
        Vec3 to = t.getBoundingBox().getCenter();
        Wave5Aim.puff(l, to, com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), 18, 0.35);
        Wave5Aim.sound(l, to, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.35f, 2.0f);
        Fx.damage(p, t, Wave5Pure.cantrip(4, p.experienceLevel), "minecraft:lightning_bolt");
        Fx.addEffect(t, "minecraft:slowness", 30, 3);          // it can take no reaction for a moment
        return true;
    }

    private static boolean acidSplash(ServerPlayer p) {
        LivingEntity t = need(p, 16);
        if (t == null) return false;
        ServerLevel l = p.serverLevel();
        Vec3 to = t.getBoundingBox().getCenter();
        Wave5Aim.beam(l, Wave5Aim.muzzle(p), to, com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), 16);
        Wave5Aim.puff(l, to, com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), 14, 0.4);
        Wave5Aim.sound(l, to, SoundEvents.EVOKER_CAST_SPELL, 0.6f, 1.8f);
        float dmg = Wave5Pure.cantrip(3, p.experienceLevel);
        Fx.damage(p, t, dmg, "minecraft:magic");
        List<LivingEntity> near = Wave5Aim.foesNear(p, to, 3.0);
        int splashed = 0;
        for (LivingEntity o : near) {
            if (o == t || splashed >= 1) continue;               // the acid splashes one more creature next to the first
            Fx.damage(p, o, dmg, "minecraft:magic");
            splashed++;
        }
        return true;
    }
}
