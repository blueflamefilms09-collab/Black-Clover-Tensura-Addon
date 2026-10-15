package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.particle.rukh.RukhFx;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Wave 11: the act bodies of the ten blades (Mystic Reflection, Shape Water, Encircle, Control Winds, Temporal Loop, Wall of Fire, Burn, Bianca's drum,
 * the Dragon's Truth's teeth). All run on the server, are bounded (at most a few dozen entities, short timers) and never touch a creative or spectator
 * player. Dice live in {@link Wave11Pure}.
 */
final class Wave11Acts {
    private Wave11Acts() {}

    static final String K_CLONE = "nusmp_w11_clone", K_CLONE_UNTIL = "nusmp_w11_clone_until", K_REACT = "nusmp_w11_react",
            K_RIPOSTE = "nusmp_w11_riposte", K_RIPOSTE_UNTIL = "nusmp_w11_riposte_until", K_BURN_SEEN = "nusmp_w11_burn_seen";

    /** True while one of the wave's own ability hits is being dealt, so the held blade's on-hit bonuses (Flamecutter) do not stack onto it. */
    static boolean acting;

    // ================================================================ Wavebreaker
    /** Mystic Reflection: a watery double (a summoned water spirit where Tensura has one) for a minute, and the extra reaction it grants. */
    static void reflection(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel l = p.serverLevel();
        dismissClone(p);
        CompoundTag pd = p.getPersistentData();
        pd.putLong(K_CLONE_UNTIL, l.getGameTime() + 1200);
        LivingEntity clone = Fx.summon(p, "tensura:undine", 1200);
        if (clone != null) pd.putUUID(K_CLONE, clone.getUUID());
        l.playSound(null, p.blockPosition(), SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 1.0f, 1.3f);
        Fx.burst(l, p.position(), 0x7FE6FF);
        Fx.say(p, "A reflection of water rises beside you for one minute. Sheathe the blade to dismiss it.");
    }

    /** Ends the clone: the summon goes, the timers are cleared. */
    static void dismissClone(ServerPlayer p) {
        CompoundTag pd = p.getPersistentData();
        if (pd.hasUUID(K_CLONE)) {
            Entity e = p.serverLevel().getEntity(pd.getUUID(K_CLONE));
            if (e != null) e.discard();
            pd.remove(K_CLONE);
        }
        pd.remove(K_CLONE_UNTIL);
        pd.remove(K_RIPOSTE);
        pd.remove(K_RIPOSTE_UNTIL);
    }

    static boolean cloneActive(ServerPlayer p) { return p.getPersistentData().getLong(K_CLONE_UNTIL) > p.level().getGameTime(); }

    /** Shape Water: a 3 x 3 patch of still water under the crosshair turns to ice for 20 seconds (a path over the water); a burning bearer is doused. */
    static void shapeWater(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel l = p.serverLevel();
        if (p.isOnFire()) {
            p.clearFire();
            Fx.say(p, "The blade draws the water to you and the flames go out.");
        }
        if (LivingDungeon.in(l)) {
            Fx.say(p, "The Dungeon's water will not be shaped.");
            return;
        }
        HitResult hr = p.pick(10.0, 1.0F, true);
        if (!(hr instanceof BlockHitResult bhr) || hr.getType() != HitResult.Type.BLOCK || !l.getFluidState(bhr.getBlockPos()).is(FluidTags.WATER)) {
            Fx.say(p, "Point the blade at still water to shape it.");
            return;
        }
        BlockPos centre = bhr.getBlockPos();
        List<BlockPos> frozen = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos q = centre.offset(dx, 0, dz);
                if (!l.isLoaded(q) || !l.mayInteract(p, q)) continue;
                if (l.getBlockState(q).is(Blocks.WATER) && l.getFluidState(q).isSource()) {
                    l.setBlock(q, Blocks.ICE.defaultBlockState(), 3);
                    frozen.add(q.immutable());
                }
            }
        }
        if (frozen.isEmpty()) {
            Fx.say(p, "There is no still water to shape here.");
            return;
        }
        RukhFx.spray(l, RukhFx.Alignment.LIGHT, centre.getX() + 0.5, centre.getY() + 1.0, centre.getZ() + 0.5, 24, 1.0, 0.3, 1.0, 0.02);
        SpellRuntime.later(l, 400, () -> {
            for (BlockPos q : frozen) if (l.isLoaded(q) && l.getBlockState(q).is(Blocks.ICE)) l.setBlock(q, Blocks.WATER.defaultBlockState(), 3);
        });
    }

    // ================================================================ Encircled Blade
    /** Encircle: a sphere of force round a creature in sight. A foe is held (10 s, 6 s for a player, 2 s for a boss-sized one); a friend is sheltered for 8 s. */
    static void encircle(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel l = p.serverLevel();
        LivingEntity t = Wave1Aim.aimedFoe(p, 20);
        boolean friend = false;
        if (t == null) {
            LivingEntity f = Wave1Aim.aimedFriend(p, 20);                                  // only a teammate or one's own pet may be sheltered: no one is locked in place against their will
            if (f != null && (f.isAlliedTo(p) || f instanceof OwnableEntity o && p.getUUID().equals(o.getOwnerUUID()))) {
                t = f;
                friend = true;
            }
        }
        if (t == null) {
            Fx.say(p, "Aim the blade at a creature within 20 blocks.");
            return;
        }
        int ticks = friend ? 160 : t instanceof Player ? 120 : 200;
        if (!friend) ticks = BalanceLaw.beginControl(p) ? BalanceLaw.controlTicks(t, ticks) : Math.min(ticks, 20);      // the balance law: 3 s a player, 5 s a mob, 1 s a boss
        final LivingEntity target = t;
        final boolean ally = friend;
        final int hold = ticks;
        grip(target, ally, ally ? Math.max(30, hold) : hold);
        l.playSound(null, target.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.6f);
        double r = Math.max(target.getBbWidth(), target.getBbHeight()) * 0.5 + 0.7;
        SpellRuntime.zone(l, ticks, 4, age -> {
            if (!target.isAlive()) return;
            Vec3 ctr = target.getBoundingBox().getCenter();
            for (int k = 0; k < 20; k++) {
                double a = k * 2.399963 + age * 0.2;
                double y = 1.0 - 2.0 * (k + 0.5) / 20.0;
                double rr = Math.sqrt(Math.max(0.0, 1.0 - y * y));
                RukhFx.spray(l, RukhFx.Alignment.LIGHT, ctr.x + Math.cos(a) * rr * r, ctr.y + y * r, ctr.z + Math.sin(a) * rr * r, 1, 0, 0, 0, 0);
            }
            if (age % 20 == 0) grip(target, ally, ally ? 30 : Math.min(30, hold));
        });
        Fx.say(p, ally ? "A sphere of force shelters your ally." : "A sphere of force closes round the creature.");
    }

    private static void grip(LivingEntity t, boolean ally, int ticks) {
        if (ally) {
            Fx.addEffect(t, "minecraft:resistance", ticks, 3);
            Fx.addEffect(t, "minecraft:slowness", ticks, 5);
        } else {
            Fx.addEffect(t, "minecraft:slowness", ticks, 9);
            Fx.addEffect(t, "minecraft:weakness", ticks, 2);
            if (t instanceof Mob m) m.getNavigation().stop();
        }
    }

    // ================================================================ Windcutter
    /** Control Winds: a gust that throws foes and missiles back (use), or, sneaking, an updraft that lifts you and your allies and slows the fall. */
    static void controlWinds(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel l = p.serverLevel();
        Vec3 look = p.getLookAngle();
        if (c.sneaking()) {
            lift(p);
            AABB box = p.getBoundingBox().inflate(6.0);
            int n = 0;
            for (LivingEntity a : l.getEntitiesOfClass(LivingEntity.class, box)) {
                if (a == p || !a.isAlive() || ++n > 12) continue;
                boolean ally = a instanceof ServerPlayer sp ? (sp.isAlliedTo(p) && !sp.isSpectator()) : a instanceof OwnableEntity o && p.getUUID().equals(o.getOwnerUUID());
                if (ally) lift(a);
            }
            RukhFx.spray(l, RukhFx.Alignment.LIGHT, p.getX(), p.getY() + 0.2, p.getZ(), 40, 1.2, 0.1, 1.2, 0.15);
            l.playSound(null, p.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
            return;
        }
        Vec3 eye = p.getEyePosition();
        for (LivingEntity e : Fx.foesAround(p, 12.0)) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double d = to.length();
            if (d < 0.1 || to.dot(look) / d < 0.35) continue;
            Fx.damage(p, e, 3.0f, null);
            e.setDeltaMovement(look.x * 1.8, 0.45, look.z * 1.8);
            e.hurtMarked = true;
        }
        int n = 0;
        for (Projectile pr : l.getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(12.0), q -> q.getOwner() != p)) {
            if (++n > 16) break;
            Vec3 to = pr.position().subtract(eye);
            double d = to.length();
            if (d < 0.1 || to.dot(look) / d < 0.35) continue;
            pr.setDeltaMovement(look.scale(Math.max(0.9, pr.getDeltaMovement().length())));
            pr.hurtMarked = true;
        }
        for (int i = 1; i <= 10; i++) {
            Vec3 at = eye.add(look.scale(i * 1.1));
            RukhFx.spray(l, RukhFx.Alignment.LIGHT, at.x, at.y, at.z, 4, 0.3 + i * 0.07, 0.3, 0.3 + i * 0.07, 0.05);
        }
        l.playSound(null, p.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.2f, 0.8f);
    }

    private static void lift(LivingEntity e) {
        Vec3 dm = e.getDeltaMovement();
        e.setDeltaMovement(dm.x, 1.1, dm.z);
        e.hurtMarked = true;
        e.fallDistance = 0;
        Fx.addEffect(e, "minecraft:slow_falling", 400, 0);
    }

    // ================================================================ Dagger of Many Daggers
    /** Temporal Loop: 2d4 copies of the dagger are pulled out of the throws that might have been, one every other tick; every fourth falls short. */
    static void temporalLoop(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel l = p.serverLevel();
        LivingEntity t = Wave1Aim.aimedFoe(p, 24);
        int copies = Wave11Pure.copies(1 + p.getRandom().nextInt(4), 1 + p.getRandom().nextInt(4));
        int hits = t == null ? 0 : Wave11Pure.hitsAgainst(copies, t instanceof Player);
        Vec3 from = p.getEyePosition().add(p.getLookAngle().scale(0.6));
        Vec3 end = t == null ? p.getEyePosition().add(p.getLookAngle().scale(20.0)) : t.getBoundingBox().getCenter();
        l.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.8f);
        int landed = 0;
        for (int i = 0; i < copies; i++) {
            boolean lands = t != null && landed < hits && !Wave11Pure.misses(i);
            if (lands) landed++;
            final boolean hit = lands;
            final double spread = (i - copies / 2.0) * 0.12;
            SpellRuntime.later(l, i * 2, () -> {
                Vec3 to = hit && t.isAlive() ? t.getBoundingBox().getCenter() : end.add(spread, 0.0, spread);
                trail(l, from, to, com.newuniverse.nusmp.particle.rukh.RukhParticles.light());
                RukhFx.spray(l, RukhFx.Alignment.LIGHT, from.x, from.y, from.z, 3, 0.1, 0.1, 0.1, 0.02);
                if (hit && t.isAlive()) Fx.damage(p, t, 2.0f, null);
            });
        }
        Fx.say(p, t == null ? "The dagger multiplies in the air and finds nothing." : copies + " daggers from " + copies + " throws that might have been.");
    }

    static void trail(ServerLevel l, Vec3 a, Vec3 b, ParticleOptions what) {
        int steps = (int) Math.min(24, Math.max(3, a.distanceTo(b) * 1.5));
        for (int s = 0; s <= steps; s++) {
            Vec3 q = a.lerp(b, s / (double) steps);
            RukhFx.spray(l, RukhFx.Alignment.of(what == com.newuniverse.nusmp.particle.rukh.RukhParticles.dark()), q.x, q.y, q.z, 1, 0, 0, 0, 0);
        }
    }

    // ================================================================ Flamecutter
    /** Wall of Fire: a 7 wide, 3 high wall four blocks ahead for 8 seconds; foes inside burn (at most 6 damage each from one wall). */
    static void wallOfFire(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel l = p.serverLevel();
        Vec3 look = p.getLookAngle();
        Vec3 fwd = new Vec3(look.x, 0, look.z);
        fwd = fwd.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : fwd.normalize();
        final Vec3 f = fwd;
        final Vec3 side = new Vec3(-f.z, 0, f.x);
        final Vec3 centre = p.position().add(f.scale(4.0));
        final Map<UUID, Float> taken = new HashMap<>();
        l.playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.2f, 0.6f);
        SpellRuntime.zone(l, 160, 10, age -> {
            for (int i = -3; i <= 3; i++) {
                for (int h = 0; h < 3; h++) {
                    Vec3 at = centre.add(side.scale(i)).add(0, h + 0.2, 0);
                    RukhFx.spray(l, RukhFx.Alignment.LIGHT, at.x, at.y, at.z, 2, 0.25, 0.25, 0.25, 0.02);
                }
            }
            RukhFx.spray(l, RukhFx.Alignment.LIGHT, centre.x, centre.y + 1.0, centre.z, 3, 2.5, 0.8, 0.3, 0.0);
            if (!p.isAlive()) return;
            for (LivingEntity e : Fx.foesAround(p, 10.0)) {
                Vec3 d = e.position().subtract(centre);
                if (Math.abs(d.dot(side)) > 3.8 || Math.abs(d.dot(f)) > 1.2 || d.y < -1.0 || d.y > 3.6) continue;
                float so = taken.getOrDefault(e.getUUID(), 0f);
                if (so >= (e instanceof Player ? 2.0f : 6.0f)) continue;                   // PvP: a wall burns a player twice at most
                taken.put(e.getUUID(), so + 1.0f);
                acting = true;
                try {
                    Fx.damage(p, e, 1.0f, "minecraft:in_fire");
                } finally {
                    acting = false;
                }
                e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), 60));
            }
        });
        Fx.say(p, "A wall of fire stands before you.");
    }

    // ================================================================ Heart of the Flame
    /** Burn: the command word heats the blade (or cools it again). The heat is kept while the sword is held; it cools when sheathed. */
    static void burn(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack s = Wave11Store.stack(c);
        boolean on = Wave11Store.get(s, "burn") != 0L;
        Wave11Store.put(s, "burn", on ? 0L : 1L);
        p.getPersistentData().putLong(K_BURN_SEEN, p.level().getGameTime());
        ServerLevel l = p.serverLevel();
        if (on) {
            Fx.say(p, "The blade cools.");
        } else {
            Fx.say(p, "\"Burn!\" The blade glows white-hot.");
            RukhFx.spray(l, RukhFx.Alignment.LIGHT, p.getX(), p.getY() + 1.2, p.getZ(), 24, 0.5, 0.5, 0.5, 0.05);
            l.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
    }

    // ================================================================ Bianca
    /** Reload: all six cylinders at once, from the arrows in the pack (free in creative). */
    static void reload(ActCtx c) {
        ServerPlayer p = c.player();
        ItemStack s = Wave11Store.stack(c);
        int drum = (int) Wave11Store.get(s, "drum");
        if (drum >= Wave11Pure.DRUM) {
            Fx.say(p, "Bianca's six cylinders are full.");
            return;
        }
        int have = 0;
        var inv = p.getInventory();
        if (p.isCreative()) {
            have = Wave11Pure.DRUM;
        } else {
            for (int i = 0; i < inv.getContainerSize() && have < Wave11Pure.DRUM; i++) {
                ItemStack q = inv.getItem(i);
                if (q.is(ItemTags.ARROWS)) have += q.getCount();
            }
        }
        int take = Wave11Pure.reloaded(drum, have);
        if (take <= 0) {
            Fx.say(p, "You have no bolts for her cylinders (arrows from your pack).");
            return;
        }
        if (!p.isCreative()) {
            int left = take;
            for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
                ItemStack q = inv.getItem(i);
                if (!q.is(ItemTags.ARROWS)) continue;
                int n = Math.min(left, q.getCount());
                q.shrink(n);
                left -= n;
            }
        }
        Wave11Store.put(s, "drum", drum + take);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 1.0f, 0.9f);
        Fx.say(p, "Bianca: " + (drum + take) + " of " + Wave11Pure.DRUM + " bolts in her cylinders.");
    }

    // ================================================================ The Dragon's Truth
    private static final String[] BREATH_TYPE = {"minecraft:magic", "minecraft:freeze", "minecraft:in_fire", "minecraft:lightning_bolt", "minecraft:magic",
            "minecraft:wither", "minecraft:magic", "minecraft:sonic_boom"};
    private static final int[] BREATH_RGB = {0x9BE04A, 0x9FE8FF, 0xFF7A2A, 0xFFF27A, 0x5BA83A, 0x4A3A5E, 0xFFF6C8, 0xC8C8E8};

    /** Throw a Tooth: a dragon-tooth dagger flies to the foe in sight (1d8 became 4), and a d8 breath of acid, cold, fire, lightning, poison, necrotic, radiant or thunder blasts through it. The belt returns it. */
    static void throwTooth(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel l = p.serverLevel();
        LivingEntity t = Wave1Aim.aimedFoe(p, 20);
        Vec3 from = p.getEyePosition().add(p.getLookAngle().scale(0.5));
        int d8 = 1 + p.getRandom().nextInt(8);
        l.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.5f);
        if (t == null) {
            trail(l, from, p.getEyePosition().add(p.getLookAngle().scale(14.0)), com.newuniverse.nusmp.particle.rukh.RukhParticles.light());
            Fx.say(p, "The tooth flies wide and the belt draws it home.");
            return;
        }
        Vec3 to = t.getBoundingBox().getCenter();
        trail(l, from, to, com.newuniverse.nusmp.particle.rukh.RukhParticles.light());
        boolean pvp = t instanceof Player;                                                // PvP-safe: 5 in all against a player, 9 against anything else
        Fx.damage(p, t, pvp ? 2.0f : 4.0f, null);
        if (!t.isAlive()) return;
        String kind = Wave11Pure.breath(d8);
        Fx.damage(p, t, pvp ? 3.0f : 5.0f, BREATH_TYPE[d8 - 1]);
        switch (kind) {
            case "acid" -> Fx.addEffect(t, "minecraft:poison", 80, 0);
            case "cold" -> {
                Fx.addEffect(t, "minecraft:slowness", 80, 1);
                Fx.addEffect(t, "tensura:frost", 80, 0);
            }
            case "fire" -> t.setRemainingFireTicks(Math.max(t.getRemainingFireTicks(), 80));
            case "poison" -> Fx.addEffect(t, "minecraft:poison", 100, 1);
            case "necrotic" -> Fx.addEffect(t, "minecraft:wither", 60, 0);
            case "radiant" -> {
                Fx.addEffect(t, "minecraft:glowing", 160, 0);
                Fx.addEffect(t, "minecraft:blindness", 40, 0);
            }
            case "thunder" -> {
                Vec3 push = to.subtract(p.position()).normalize().scale(1.2);
                t.setDeltaMovement(push.x, 0.3, push.z);
                t.hurtMarked = true;
            }
            default -> { }
        }
        Fx.burst(l, to.subtract(0, 0.6, 0), BREATH_RGB[d8 - 1]);
        Fx.say(p, "The tooth strikes and its breath of " + kind + " blasts through the foe.");
    }

    /** Extraction: the creature in front of you must answer: its name, health, armour, effects and who it hunts; a foe too strong in will shakes it off. */
    static void extraction(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity t = Wave1Aim.aimedFoe(p, 10);
        if (t == null) t = Wave1Aim.aimedFriend(p, 10);
        if (t == null) {
            Fx.say(p, "Face a creature within 10 blocks to question it.");
            return;
        }
        Fx.lendSkill(p, "tensura:analytical_appraisal", 600);
        if (t.getMaxHealth() >= 250f) {
            Fx.say(p, t.getDisplayName().getString() + "'s will is too strong: it does not answer.");
            return;
        }
        Fx.addEffect(t, "minecraft:glowing", 200, 0);
        StringBuilder fx = new StringBuilder();
        int n = 0;
        for (MobEffectInstance m : t.getActiveEffects()) {
            if (n++ >= 6) break;
            if (fx.length() > 0) fx.append(", ");
            fx.append(m.getEffect().value().getDisplayName().getString());
        }
        String hunts = t instanceof Mob m && m.getTarget() != null ? m.getTarget().getDisplayName().getString() : "no one";
        Fx.say(p, "The truth of " + t.getDisplayName().getString() + ": health " + Math.round(t.getHealth()) + " of " + Math.round(t.getMaxHealth())
                + ", armour " + Math.round((float) t.getArmorValue()) + ", " + (fx.length() == 0 ? "no effects" : fx.toString()) + ", hunting " + hunts + ".");
    }
}
