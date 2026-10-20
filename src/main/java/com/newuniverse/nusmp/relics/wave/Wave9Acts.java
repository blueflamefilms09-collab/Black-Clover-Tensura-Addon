package com.newuniverse.nusmp.relics.wave;


import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.FxRelic;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Wave 9: the active abilities of the ten weapons. Each runs on the server, after the engine has settled attunement, cooldown, uses of the day and
 * the mana or existence cost. State that must outlive the act (the armed bolt, the Viper's mark, the stance) is kept in the player's persistent data
 * under the keys of {@link Wave9Events}.
 */
final class Wave9Acts {
    private Wave9Acts() {}

    // ---------------------------------------------------------------- aiming helpers
    /** The end of a ray of this length from the eyes, shortened where it meets a block. */
    static Vec3 rayEnd(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(range));
        BlockHitResult b = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return b.getType() == HitResult.Type.MISS ? end : b.getLocation();
    }

    /** The nearest foe on the line of sight within the range (the ray stops at walls), or null. */
    static LivingEntity rayFoe(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = rayEnd(p, range);
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity f : Fx.foesAround(p, range)) {
            Optional<Vec3> hit = f.getBoundingBox().inflate(0.35).clip(eye, end);
            if (hit.isPresent()) {
                double d = hit.get().distanceToSqr(eye);
                if (d < bd) {
                    bd = d;
                    best = f;
                }
            }
        }
        return best;
    }

    /** Dots of the particle along a line, about every {@code step} blocks (at most 40). */
    static void trail(ServerLevel l, Vec3 from, Vec3 to, ParticleOptions what, double step) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 0.1) return;
        int n = (int) Math.min(40, Math.max(2, len / Math.max(0.2, step)));
        for (int i = 0; i <= n; i++) {
            Vec3 at = from.add(d.scale(i / (double) n));
            com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_9, FxTone.of(com.newuniverse.nusmp.particle.fx.Fx.toneOf(what) == FxTone.DARK), at.x, at.y, at.z, 1, 0.03, 0.03, 0.03, 0.0);
        }
    }

    /** Runs damage that the weapon deals by its own ability, so that its on-hit bonuses do not apply to it a second time. */
    static void quiet(Runnable r) {
        boolean was = Wave9Events.busy;
        Wave9Events.busy = true;
        try {
            r.run();
        } finally {
            Wave9Events.busy = was;
        }
    }

    private static CompoundTag pd(ServerPlayer p) { return p.getPersistentData(); }

    // ---------------------------------------------------------------- thrown axes (Axe of the Exile, Thornblight)
    /**
     * A thrown handaxe that comes straight back: the throw is a line of sight, so there is nothing to lose; it hits the nearest foe in range, and
     * the axe is in the hand again before the next swing (the cooldown is the flight). Returns the foe that was hit, or null.
     */
    static LivingEntity throwAxe(ServerPlayer p, float damage, double range) {
        ServerLevel l = p.serverLevel();
        Vec3 from = p.getEyePosition().add(0, -0.3, 0);
        LivingEntity t = rayFoe(p, range);
        Vec3 end = t != null ? t.position().add(0, t.getBbHeight() * 0.6, 0) : rayEnd(p, range);
        trail(l, from, end, com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_9, FxTone.LIGHT), 0.8);
        trail(l, end.add(0, 0.25, 0), from, com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_9, FxTone.LIGHT), 1.4);                    // the way back
        l.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.9f, 1.4f);
        if (t == null) return null;
        quiet(() -> Fx.damage(p, t, damage, "minecraft:player_attack"));
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_9, FxTone.LIGHT, t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(), 1, 0, 0, 0, 0);
        return t;
    }

    // ---------------------------------------------------------------- Hyperborean Crossbow
    /** Flash Freeze on a surface: the next bolt leaves a frost patch where it lands. */
    static void frostShot(ActCtx c) {
        ServerPlayer p = c.player();
        pd(p).putLong(Wave9Events.K_ARMED, p.level().getGameTime() + Wave9Rules.FROST_ARMED_TICKS);
        FxRelic.ring(p.serverLevel(), p.position(), 1.6, 0x8FD8FF);
        com.newuniverse.nusmp.particle.fx.Fx.spray(p.serverLevel(), FxThemes.RELIC_WAVE_9, FxTone.LIGHT, p.getX(), p.getY() + 1.2, p.getZ(), 16, 0.5, 0.5, 0.5, 0.02);
        Fx.say(p, "The next bolt will freeze the ground where it lands (30 seconds).");
    }

    // ---------------------------------------------------------------- Druidic Blade
    /** Entangle: the grass and vines rise round a point you look at (16 blocks) and hold everything in 5 blocks; a boss is held for half as long. */
    static void entangle(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel l = p.serverLevel();
        LivingEntity t = rayFoe(p, 16);
        Vec3 at = t != null ? t.position() : rayEnd(p, 16);
        FxRelic.ring(l, at, 5.0, 0x3FB950);
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_9, FxTone.LIGHT, at.x, at.y + 0.3, at.z, 30, 2.4, 0.3, 2.4, 0.02);
        l.playSound(null, p.blockPosition(), SoundEvents.GRASS_BREAK, SoundSource.PLAYERS, 1.2f, 0.6f);
        int held = 0;
        for (LivingEntity f : Fx.foesAround(p, 28)) {
            if (f.position().distanceToSqr(at) > 25.0) continue;
            int ticks = Wave9Rules.controlTicks(80, f.getMaxHealth());
            Fx.addEffect(f, "minecraft:slowness", ticks, 6);
            Fx.addEffect(f, "tensura:earth_lock", ticks, 0);
            com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_9, FxTone.LIGHT, f.getX(), f.getY() + 0.2, f.getZ(), 14, 0.4, 0.5, 0.4, 0.02);
            held++;
        }
        if (held == 0) Fx.say(p, "The vines rise, and find nothing to hold.");
    }

    // ---------------------------------------------------------------- Null Blade
    /** Blindness / Deafness, paid with a charge the blade earns by shrugging off a hostile spell. */
    static void blindDeafen(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag d = pd(p);
        int charges = d.getInt(Wave9Events.K_CHARGE);
        if (charges <= 0) {
            Fx.say(p, "The blade holds no charge: it takes one each time you endure a hostile spell.");
            return;
        }
        d.putInt(Wave9Events.K_CHARGE, charges - 1);
        ServerLevel l = p.serverLevel();
        FxRelic.ring(l, p.position(), 8.0, 0x6A2CA8);
        for (LivingEntity f : Fx.foesAround(p, 8)) {
            int ticks = Wave9Rules.controlTicks(100, f.getMaxHealth());
            Fx.addEffect(f, "minecraft:blindness", ticks, 0);
            Fx.addEffect(f, "tensura:silence", ticks, 0);
        }
        Fx.say(p, "The eye in the cross opens. Charges left: " + (charges - 1) + ".");
    }

    /** Armor of Agathys, from the awakened blade: ten points of frost-rimed absorption, and what strikes you takes cold for it. */
    static void agathys(ActCtx c) {
        ServerPlayer p = c.player();
        if (!Wave9Events.awakened(c.stack())) {
            Fx.say(p, "The blade is not awakened: it must take twenty lives first (" + Wave9Store.get(c.stack(), "kills") + "/" + Wave9Rules.AWAKEN_KILLS + ").");
            return;
        }
        Fx.shield(p, 10f, 1200);
        p.setAbsorptionAmount(Math.max(p.getAbsorptionAmount(), 10f));
        pd(p).putLong(Wave9Events.K_AGATHYS, p.level().getGameTime() + 1200);
        FxRelic.burst(p.serverLevel(), p.position(), 0x9FE8FF);
        Fx.say(p, "Frost closes round you: attackers take cold damage while it lasts.");
    }

    // ---------------------------------------------------------------- Astralaria
    /** A burst of the night sky round you: radiant damage and a shining mark on every foe within 6 blocks. */
    static void astralSweep(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel l = p.serverLevel();
        FxRelic.ring(l, p.position(), 6.0, 0x5B3FD0);
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_9, FxTone.LIGHT, p.getX(), p.getY() + 1.0, p.getZ(), 40, 2.5, 0.8, 2.5, 0.05);
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_9, FxTone.DARK, p.getX(), p.getY() + 1.0, p.getZ(), 40, 2.5, 0.8, 2.5, 0.4);
        for (LivingEntity f : Fx.foesAround(p, 6)) {
            quiet(() -> Fx.damage(p, f, 9f, "minecraft:magic"));
            Fx.addEffect(f, "minecraft:glowing", 100, 0);
        }
    }

    /** Cosmic footprints: sneak + use switches the glittering trail behind your steps on or off (the body flips it; the tick draws it). */
    static void footprints(ActCtx c) {
        boolean on = c.isOn("footprints");
        Fx.say(c.player(), on ? "Stars bloom under your steps." : "The stars under your feet go dark.");
    }

    // ---------------------------------------------------------------- Viper Whip
    /** The viper bites: the target is marked for half a day and the whip tells you which way it is, wherever it goes (1000 metres). */
    static void viperBite(ActCtx c) {
        ServerPlayer p = c.player();
        long now = p.level().getGameTime();
        LivingEntity t = Wave9Events.lastHit(p, 200);
        if (t == null) t = rayFoe(p, 6);
        if (t == null) {
            List<LivingEntity> near = Fx.foesAround(p, 5);
            if (!near.isEmpty()) t = near.get(0);
        }
        if (t == null) {
            Fx.say(p, "The viper finds nothing to bite: strike a foe, or face one within six blocks.");
            return;
        }
        CompoundTag d = pd(p);
        d.putUUID(Wave9Events.K_MARK_UUID, t.getUUID());
        d.putLong(Wave9Events.K_MARK_UNTIL, now + Wave9Rules.MARK_TICKS);
        d.putDouble(Wave9Events.K_MARK_X, t.getX());
        d.putDouble(Wave9Events.K_MARK_Y, t.getY());
        d.putDouble(Wave9Events.K_MARK_Z, t.getZ());
        d.putString(Wave9Events.K_MARK_DIM, t.level().dimension().location().toString());
        Fx.addEffect(t, "minecraft:poison", 80, 0);
        ServerLevel l = p.serverLevel();
        trail(l, p.getEyePosition().add(0, -0.4, 0), t.position().add(0, t.getBbHeight() * 0.7, 0), com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_9, FxTone.LIGHT), 0.6);
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_9, FxTone.LIGHT, t.getX(), t.getY() + t.getBbHeight() * 0.7, t.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
        Fx.say(p, "Bitten: you will feel which way " + t.getName().getString() + " lies for ten minutes.");
    }

    // ---------------------------------------------------------------- Caesar's Gladius
    /** Three gladiators, tamed to you, for five minutes. One time in ten they remember the Republic (see {@link Wave9Events#tickRebels}). */
    static void conjure(ActCtx c) {
        ServerPlayer p = c.player();
        long now = p.level().getGameTime();
        ResourceLocation liz = ResourceLocation.fromNamespaceAndPath("tensura", "lizardman");
        String type = BuiltInRegistries.ENTITY_TYPE.containsKey(liz) ? liz.toString() : "minecraft:iron_golem";
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < Wave9Rules.GLADIATORS; i++) {
            LivingEntity g = Fx.summon(p, type, Wave9Rules.GLADIATOR_LIFE);
            if (g == null) continue;
            g.setCustomName(Component.literal("Gladiator"));
            ids.add(g.getUUID());
        }
        if (ids.isEmpty()) {
            Fx.say(p, "No one answers the gladius.");
            return;
        }
        Fx.say(p, "\"Ave, Caesar!\" " + ids.size() + " gladiators take the sand beside you.");
        if (Wave9Rules.rebels(p.getRandom().nextDouble())) Wave9Events.markRebels(p.getUUID(), ids, now + Wave9Rules.rebelDelay(p.getRandom().nextInt()));
    }

    // ---------------------------------------------------------------- Twin Swords of Deflection
    /** A parry: for two seconds missiles turn back on the sender and melee blows lose six tenths of their force. */
    static void deflect(ActCtx c) {
        ServerPlayer p = c.player();
        pd(p).putLong(Wave9Events.K_DEFLECT, p.level().getGameTime() + Wave9Rules.DEFLECT_TICKS);
        ServerLevel l = p.serverLevel();
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_9, FxTone.LIGHT, p.getX(), p.getY() + 1.1, p.getZ(), 2, 0.5, 0.2, 0.5, 0.0);
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_9, FxTone.LIGHT, p.getX(), p.getY() + 1.1, p.getZ(), 20, 0.7, 0.5, 0.7, 0.1);
        l.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 1.5f);
    }

    // ---------------------------------------------------------------- Hunger
    /** Devouring: for twenty seconds the next hit that draws blood feeds you as much temporary health as it dealt. */
    static void devour(ActCtx c) {
        ServerPlayer p = c.player();
        pd(p).putLong(Wave9Events.K_DEVOUR, p.level().getGameTime() + Wave9Rules.DEVOUR_ARMED_TICKS);
        com.newuniverse.nusmp.particle.fx.Fx.spray(p.serverLevel(), FxThemes.RELIC_WAVE_9, FxTone.DARK, p.getX(), p.getY() + 1.4, p.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
        Fx.say(p, "Hunger opens its mouth. The next hit will be eaten.");
    }

    // ---------------------------------------------------------------- Thornblight
    /** Spiteful Thorns: thorns grow in the wound of the foe you struck last, and fester for twelve seconds. */
    static void spitefulThorns(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity t = Wave9Events.lastHit(p, Wave9Rules.THORN_WINDOW);
        if (t == null || t.distanceToSqr(p) > 28.0 * 28.0) {
            Fx.say(p, "The thorns need a fresh wound: strike a foe, then call them.");
            return;
        }
        Fx.addEffect(t, "minecraft:poison", Wave9Rules.THORN_TICKS, 0);
        Fx.addEffect(t, "minecraft:slowness", 60, 0);
        ServerLevel l = p.serverLevel();
        FxRelic.burst(l, t.position(), 0x35E08A);
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_9, FxTone.LIGHT, t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(), 20, 0.3, 0.5, 0.3, 0.02);
    }
}
