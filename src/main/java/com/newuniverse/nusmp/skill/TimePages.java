package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.blackclover.BlockHistory;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.blackclover.TimeStop;
import com.newuniverse.nusmp.blackclover.TimedModifiers;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** LEGACY: superseded by book.TimeBook (real Tensura magic). Kept registered so old saves load; players are migrated automatically. */
public final class TimePages {
    static final int TIME_GOLD = 0xFFF5D76E;
    private TimePages() {}

    // ------------------------------------------------------------------ Chrono Stasis
    /** Freeze one target in time. Hits on it are stored and land all at once when time resumes. */
    public static class ChronoStasis extends GrimoirePageSkill {
        public ChronoStasis() { super(MagicType.TIME); }

        @Override
        public void onPressed(ManasSkillInstance i, LivingEntity e, int key, int mode) {
            if (!(e instanceof ServerPlayer p) || !ready(p)) return;
            if (i.onCoolDown(mode)) { SkillUtil.fail(p, "The seconds are still settling."); return; }
            LivingEntity target = GodTier.lookLiving(p, 32);
            if (target == null) { SkillUtil.fail(p, "There is nothing there to stop."); return; }
            if (!pay(p, 1500)) return;
            int ticks = (int) ((i.isMastered(p) ? 120 : 70) * Math.min(1.5, power(p)));
            TimeStop.freeze(target, ticks);
            shout(p, "Chrono Stasis", ChatFormatting.GOLD);
            VfxSpawn.send(p.serverLevel(), VfxShape.MAGIC_CIRCLE, target.position().add(0, target.getBbHeight() / 2, 0),
                    p.getEyePosition(), TIME_GOLD, ticks, Math.max(0.6F, target.getBbWidth()));
            p.serverLevel().playSound(null, target.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.0F, 0.6F);
            i.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(DMUtil.cooldown(300)), mode);
            mastery(i, p, 1);
        }
    }

    // ------------------------------------------------------------------ Chrono Stasis Grigora
    /**
     * Toggle a zone where everything except you stops. Sneak while casting to spare players.
     * Heavy magicule drain every second; stepping outside the zone drags it after you for extra cost.
     */
    public static class ChronoStasisGrigora extends GrimoirePageSkill {
        public ChronoStasisGrigora() { super(MagicType.TIME); }

        @Override public boolean canTick(ManasSkillInstance i, LivingEntity e) { return true; }

        @Override
        public void onPressed(ManasSkillInstance i, LivingEntity e, int key, int mode) {
            if (!(e instanceof ServerPlayer p) || !ready(p)) return;
            CompoundTag t = i.getOrCreateTag();
            if (t.getBoolean("Active")) { stop(i, p, "You let time flow again."); return; }
            if (i.onCoolDown(mode)) { SkillUtil.fail(p, "Grigora is still recovering."); return; }
            if (!pay(p, 2500)) return;
            float radius = (i.isMastered(p) ? 12 : 8) * (float) Math.min(1.4, power(p));
            t.putBoolean("Active", true);
            t.putDouble("X", p.getX()); t.putDouble("Y", p.getY()); t.putDouble("Z", p.getZ());
            t.putFloat("R", radius);
            t.putBoolean("SparePlayers", p.isShiftKeyDown());
            i.markDirty();
            shout(p, "Chrono Stasis Grigora", ChatFormatting.GOLD);
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 0.5F);
        }

        private void stop(ManasSkillInstance i, ServerPlayer p, String msg) {
            i.getOrCreateTag().putBoolean("Active", false);
            i.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(DMUtil.cooldown(600)), 0);
            i.markDirty();
            p.displayClientMessage(Component.literal(msg).withStyle(ChatFormatting.GRAY), true);
        }

        @Override
        public void onTick(ManasSkillInstance i, LivingEntity e) {
            if (!(e instanceof ServerPlayer p)) return;
            CompoundTag t = i.getOrCreateTag();
            if (!t.getBoolean("Active")) return;
            if (!p.isAlive()) { stop(i, p, "Time resumes."); return; }
            Vec3 c = new Vec3(t.getDouble("X"), t.getDouble("Y"), t.getDouble("Z"));
            float r = t.getFloat("R");
            // Caster walked out: drag the zone along, at a price.
            if (p.position().distanceTo(c) > r) {
                if (!pay(p, 800)) { stop(i, p, "The zone slips from your grasp."); return; }
                c = p.position();
                t.putDouble("X", c.x); t.putDouble("Y", c.y); t.putDouble("Z", c.z);
            }
            if (p.tickCount % 20 == 0 && !pay(p, i.isMastered(p) ? 300 : 450)) { stop(i, p, "Your magicule runs dry. Time resumes."); return; }
            boolean spare = t.getBoolean("SparePlayers");
            for (Entity x : p.serverLevel().getEntities(p, new AABB(c, c).inflate(r))) {
                if (x.distanceToSqr(c) > r * r) continue;
                if (spare && x instanceof Player) continue;
                if (x instanceof LivingEntity || x instanceof Projectile || x instanceof net.minecraft.world.entity.item.FallingBlockEntity) {
                    TimeStop.freeze(x, 3);
                }
            }
            if (p.tickCount % 30 == 0) {
                VfxSpawn.send(p.serverLevel(), VfxShape.MAGIC_CIRCLE, c.add(0, 0.05, 0), c.add(0, 1, 0), TIME_GOLD, 32, r / 1.6F);
            }
            if (p.tickCount % 200 == 0) mastery(i, p, 1);
        }
    }

    // ------------------------------------------------------------------ Time Acceleration
    /** Speed up your own time; hostile projectiles near you crawl. */
    public static class TimeAcceleration extends GrimoirePageSkill {
        public TimeAcceleration() { super(MagicType.TIME); }

        @Override public boolean canTick(ManasSkillInstance i, LivingEntity e) { return true; }

        @Override
        public void onPressed(ManasSkillInstance i, LivingEntity e, int key, int mode) {
            if (!(e instanceof ServerPlayer p) || !ready(p)) return;
            if (i.onCoolDown(mode)) { SkillUtil.fail(p, "Your own clock is still unwinding."); return; }
            if (!pay(p, 1200)) return;
            int ticks = i.isMastered(p) ? 400 : 240;
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 2));
            p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, ticks, 3));   // haste also speeds up attacks
            p.addEffect(new MobEffectInstance(MobEffects.JUMP, ticks, 1));
            i.getOrCreateTag().putLong("Until", p.serverLevel().getGameTime() + ticks);
            shout(p, "Time Acceleration", ChatFormatting.GOLD);
            circle(p, p.position().add(0, 0.05, 0), new Vec3(0, 1, 0), TIME_GOLD, 1.0F);
            i.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(DMUtil.cooldown(ticks + 200)), mode);
            mastery(i, p, 1);
        }

        @Override
        public void onTick(ManasSkillInstance i, LivingEntity e) {
            if (!(e instanceof ServerPlayer p) || p.serverLevel().getGameTime() > i.getOrCreateTag().getLong("Until")) return;
            for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(7))) {
                if (pr.getOwner() == p) continue;
                pr.setDeltaMovement(pr.getDeltaMovement().scale(0.8));
                pr.hurtMarked = true;
            }
        }
    }

    // ------------------------------------------------------------------ Time Reversal (objects)
    /** Rewind broken blocks (last minute) and wounded golems around the point you look at. Not resurrection. */
    public static class TimeReversal extends GrimoirePageSkill {
        public TimeReversal() { super(MagicType.TIME); }

        @Override
        public void onPressed(ManasSkillInstance i, LivingEntity e, int key, int mode) {
            if (!(e instanceof ServerPlayer p) || !ready(p)) return;
            if (i.onCoolDown(mode)) { SkillUtil.fail(p, "The past is still too close to touch."); return; }
            if (!pay(p, 1800)) return;
            ServerLevel level = p.serverLevel();
            Vec3 at = DMUtil.lookPoint(p, 24);
            double radius = i.isMastered(p) ? 10 : 7;
            int blocks = BlockHistory.rewind(level, BlockPos.containing(at), radius, i.isMastered(p) ? 2400 : 1200);
            int golems = 0;
            for (IronGolem g : level.getEntitiesOfClass(IronGolem.class, new AABB(at, at).inflate(radius))) {
                if (g.getHealth() < g.getMaxHealth()) { g.setHealth(g.getMaxHealth()); golems++; }
            }
            shout(p, "Time Reversal", ChatFormatting.GOLD);
            VfxSpawn.send(level, VfxShape.MAGIC_CIRCLE, at.add(0, 0.1, 0), at.add(0, 1, 0), TIME_GOLD, 40, (float) radius / 1.6F);
            p.displayClientMessage(Component.literal("Rewound " + blocks + " blocks" + (golems > 0 ? " and " + golems + " golems" : "") + ".")
                    .withStyle(ChatFormatting.GOLD), true);
            i.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(DMUtil.cooldown(400)), mode);
            mastery(i, p, 1);
        }
    }

    // ------------------------------------------------------------------ Stolen Time
    /** A slash that steals the target's time: they age (wither, slowness, armor shred); you pay with brief weakness. */
    public static class StolenTime extends GrimoirePageSkill {
        private static final ResourceLocation SHRED = ResourceLocation.fromNamespaceAndPath("nusmp", "stolen_time_shred");
        public StolenTime() { super(MagicType.TIME); }

        @Override
        public void onPressed(ManasSkillInstance i, LivingEntity e, int key, int mode) {
            if (!(e instanceof ServerPlayer p) || !ready(p)) return;
            if (i.onCoolDown(mode)) { SkillUtil.fail(p, "You have no time left to steal."); return; }
            if (!pay(p, 1000)) return;
            Vec3 eye = p.getEyePosition(), look = p.getViewVector(1.0F);
            float dmg = (float) (12 * power(p) * (i.isMastered(p) ? 1.4 : 1.0));
            int hits = 0;
            for (LivingEntity t : DMUtil.around(p, p.position(), 5)) {
                Vec3 to = t.getBoundingBox().getCenter().subtract(eye);
                if (to.normalize().dot(look) < 0.55) continue;
                t.hurt(p.damageSources().indirectMagic(p, p), dmg);
                t.addEffect(new MobEffectInstance(MobEffects.WITHER, 160, 1));
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2));
                TimedModifiers.apply(t, Attributes.ARMOR, SHRED, i.isMastered(p) ? -8 : -5, 160);
                hits++;
            }
            shout(p, "Stolen Time", ChatFormatting.GOLD);
            VfxSpawn.send(p.serverLevel(), VfxShape.WIND_SLASH, eye, eye.add(look.scale(5)), TIME_GOLD, 14, 1.0F);
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));   // the price: your own time stumbles
            i.setCoolDown(com.newuniverse.nusmp.NUSMP.ticksToSeconds(DMUtil.cooldown(160)), mode);
            if (hits > 0) mastery(i, p, 1);
        }
    }
}
