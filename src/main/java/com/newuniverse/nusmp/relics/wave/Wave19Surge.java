package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Wave 19: the wild magic table played out. Used by the Scroll of Unreadable Spells (one surge per letter read) and by Fey Honey (a surge when
 * the honey wears off). Every outcome is bounded, telegraphed by a burst of colour, and none can kill: self-inflicted harm leaves the caster at
 * least four health points, damage on others goes through {@link Fx}, and creative or spectator players are never touched.
 */
final class Wave19Surge {
    private Wave19Surge() {}

    /** Rolls the d100 and plays what comes up; returns the outcome (for the message). */
    static Wave19Rules.Surge roll(ServerPlayer p) {
        Wave19Rules.Surge s = Wave19Rules.surge(Wave19Rules.d100(p.getRandom()::nextInt));
        play(p, s);
        return s;
    }

    static String line(Wave19Rules.Surge s) {
        return switch (s) {
            case GLITTER -> "the letters fizzle into a puff of glitter.";
            case LEVITATE -> "you drift off the ground.";
            case MENDING -> "a warm light knits your wounds.";
            case FIRE_BURST -> "sparks burst out of you in a ring of fire.";
            case FEATHER_FALL -> "you weigh no more than a feather.";
            case VANISH -> "you wink out of sight.";
            case SWIFT_FEET -> "your feet fly.";
            case MISTY_STEP -> "the world folds and you step through.";
            case SWAP_PLACES -> "you trade places with the nearest foe.";
            case STINK_CLOUD -> "a reeking green cloud rolls out.";
            case CHAIN_SPARK -> "a bolt leaps from the page to the nearest foe.";
            case SHEEP_FLOCK -> "a flock of sheep pops into being.";
            case DREAD -> "every foe nearby is gripped by sudden dread.";
            case FLASH -> "a white flash blinds everyone nearby, you too.";
            case MANA_SURGE -> "raw magic floods your veins.";
            case BLOOM -> "flowers and fresh growth burst around you.";
            case BACKFIRE -> "the spell turns on you.";
            default -> "a hush; the air holds its breath, and then gives you a wish.";
        };
    }

    private static void play(ServerPlayer p, Wave19Rules.Surge s) {
        ServerLevel sl = p.serverLevel();
        Vec3 at = p.position();
        Fx.burst(sl, at, 0xC060FF);
        sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 0.6f + p.getRandom().nextFloat() * 0.8f);
        switch (s) {
            case LEVITATE -> Wave19Util.give(p, "minecraft:levitation", 100, 0);
            case MENDING -> Fx.heal(p, 6f);
            case FIRE_BURST -> {
                Fx.ring(sl, at, 4.0, 0xFF7A20);
                for (LivingEntity t : Wave19Util.foesAt(p, at, 4.0)) {
                    Fx.damage(p, t, 5f, "minecraft:in_fire");
                    t.igniteForSeconds(3f);
                }
                if (!p.isCreative()) p.igniteForSeconds(1f);
            }
            case FEATHER_FALL -> {
                Wave19Util.give(p, "minecraft:slow_falling", 400, 0);
                Wave19Util.give(p, "minecraft:jump_boost", 400, 2);
            }
            case VANISH -> Wave19Util.give(p, "minecraft:invisibility", 200, 0);
            case SWIFT_FEET -> Wave19Util.give(p, "minecraft:speed", 200, 2);
            case MISTY_STEP -> Fx.teleportLook(p, 10);
            case SWAP_PLACES -> swap(p);
            case STINK_CLOUD -> {
                Fx.ring(sl, at, 5.0, 0x7AC030);
                Fx.aoe(p, 5.0, 0f, null, "minecraft:nausea", 160, 0);
                Fx.aoe(p, 5.0, 0f, null, "minecraft:poison", 80, 0);
            }
            case CHAIN_SPARK -> chain(p);
            case SHEEP_FLOCK -> {
                for (int i = 0; i < 3; i++) Fx.summon(p, "minecraft:sheep", 400);
            }
            case DREAD -> {
                Fx.aoe(p, 8.0, 0f, null, "tensura:fear", 160, 0);
                Fx.aoe(p, 8.0, 0f, null, "minecraft:slowness", 100, 1);
            }
            case FLASH -> {
                Fx.aoe(p, 6.0, 0f, null, "minecraft:blindness", 80, 0);
                Wave19Util.give(p, "minecraft:blindness", 40, 0);
                sl.sendParticles(ParticleTypes.FLASH, at.x, at.y + 1.0, at.z, 1, 0, 0, 0, 0);
            }
            case MANA_SURGE -> Wave19Util.addMagicules(p, 0.20);
            case BLOOM -> {
                Wave19Util.give(p, "minecraft:regeneration", 200, 1);
                p.getFoodData().eat(4, 0.4f);
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y + 1.0, at.z, 40, 1.5, 0.8, 1.5, 0.05);
                sl.sendParticles(ParticleTypes.CHERRY_LEAVES, at.x, at.y + 2.0, at.z, 30, 1.5, 0.6, 1.5, 0.02);
            }
            case BACKFIRE -> {
                Wave19Util.give(p, "minecraft:weakness", 120, 0);
                Wave19Util.give(p, "minecraft:slowness", 120, 0);
                if (!p.isCreative() && p.getHealth() > 7f) p.hurt(p.damageSources().magic(), 3f);   // a sting, never a kill: it only fires with seven or more health
            }
            case WISH -> {
                Fx.shield(p, 8f, 400);
                Wave19Util.give(p, "minecraft:regeneration", 200, 1);
                Wave19Util.give(p, "minecraft:resistance", 300, 0);
                Wave19Util.addMagicules(p, 0.10);
            }
            default -> {
                // GLITTER: colour and a chime, nothing more
            }
        }
    }

    /** Trades places with the nearest foe within twelve blocks (same dimension; both land safely where the other stood). */
    private static void swap(ServerPlayer p) {
        List<LivingEntity> foes = Wave19Util.foesAt(p, p.position(), 12.0);
        if (foes.isEmpty()) return;
        LivingEntity t = foes.get(0);
        if (t instanceof ServerPlayer) return;                                           // never drag another player about
        Vec3 mine = p.position(), theirs = t.position();
        t.teleportTo(mine.x, mine.y, mine.z);
        p.teleportTo(theirs.x, theirs.y, theirs.z);
        p.fallDistance = 0;
    }

    /** A bolt that only dazzles the sky (visual) plus six magic damage on the nearest foe in twelve blocks. */
    private static void chain(ServerPlayer p) {
        List<LivingEntity> foes = Wave19Util.foesAt(p, p.position(), 12.0);
        if (foes.isEmpty()) return;
        LivingEntity t = foes.get(0);
        try {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(p.serverLevel());
            if (bolt != null) {
                bolt.moveTo(t.getX(), t.getY(), t.getZ());
                bolt.setVisualOnly(true);
                p.serverLevel().addFreshEntity(bolt);
            }
        } catch (RuntimeException | LinkageError none) {
            // the sky stays quiet
        }
        Fx.damage(p, t, 6f, "minecraft:lightning_bolt");
    }
}
