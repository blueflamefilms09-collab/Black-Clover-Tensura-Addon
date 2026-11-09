package com.newuniverse.nusmp.saiyan;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxSlot;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Why: the Son Goku path of the Saiyan system (3.65.0, Stage 5), the new Saiyan Ultimate that lives NEXT TO the old {@code KiSkill} / {@code db_goku} content, which is not touched. AWAKENING: every five
 * seconds a Saiyan who holds no Ultimate is checked against the requirements of the registry ({@link SaiyanUltimates}) and, when all are met, awakens "Son Goku, King of Martial Arts" with a title, a sound and
 * a burst; only a Saiyan can (the entry is restricted to the race). KAMEHAMEHA: hold to charge (Ki per second), release to fire a beam that is ONE raycast for blocks and ONE entity query along the line, passes
 * through up to eight foes, hits through the balance law and destroys no block. ROCK, SCISSORS 'N' PAPER: a three-part timed martial combo (a lunging punch, a fast strike, an open-palm shockwave) whose
 * blows go through the balance law and count as martial hits. Cost: one entity query per Kamehameha release and per combo part, at most {@link #MAX_CANDIDATES} creatures looked at, at most 26 particles
 * for a beam; no entity is spawned.
 */
final class GokuAbilities {
    private GokuAbilities() {}

    static final int MAX_CANDIDATES = 64, MAX_BEAM_PARTICLES = 24;

    static boolean isGoku(SaiyanState s) { return SaiyanUltimates.GOKU.equals(s.ultimate); }

    // ================================================================== the awakening
    static SaiyanUltimates.Progress progress(SaiyanState s) {
        return new SaiyanUltimates.Progress(s.battlePower, s.mastered(), SaiyanFlight.tier(s).ordinal(), SaiyanSenses.tier(s).ordinal(), s.hardMartialKills, s.zenkai, s.trialKills);
    }

    /** Every five seconds: a Saiyan with no Ultimate who meets the requirements of the Goku entry awakens it. */
    static void awakeningCheck(ServerPlayer p, SaiyanState s, SaiyanManager.Run r) {
        if (!SaiyanConfig.gokuEnabled() || !s.awakened || !SaiyanUltimates.mayTake(s.ultimate)) return;
        SaiyanUltimates.Ultimate u = SaiyanUltimates.byId(SaiyanUltimates.GOKU);
        if (!SaiyanUltimates.openTo(u, s.saiyan ? SaiyanUltimates.RACE_SAIYAN : "")) return;
        if (SaiyanUltimates.ready(SaiyanConfig.gokuNeed(), progress(s))) awaken(p, s, r, u);
    }

    /** Gives the Ultimate (the check above, or the operator command): the announcement is a title, a sound and a burst. */
    static void awaken(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, SaiyanUltimates.Ultimate u) {
        s.ultimate = u.id();
        r.dirty = true;
        Component name = Component.translatable(u.nameKey());
        if (p.connection != null) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable(u.epithetKey())));
            p.connection.send(new ClientboundSetTitleTextPacket(name));
        }
        p.displayClientMessage(Component.translatable("nusmp.saiyan.msg.ultimate", name), false);
        ServerLevel level = p.serverLevel();
        Fx.spray(level, FxThemes.WORLD, FxOld.FIREWORK, p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.8, 1.0, 0.8, 0.2);
        Fx.sprayTinted(level, FxThemes.WORLD, FxSlot.SPARK, 0xFFB020, 1.5f, p.getX(), p.getY() + 1.0, p.getZ(), 24, 0.6, 1.0, 0.6, 0.15);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    // ================================================================== the gate every Goku technique shares
    private static boolean open(ServerPlayer p, SaiyanState s) {
        if (!SaiyanConfig.gokuEnabled() || p.isSpectator()) return false;
        if (!isGoku(s)) {
            SaiyanAbilities.say(p, "goku_locked");
            return false;
        }
        if (!s.awakened) {
            SaiyanAbilities.say(p, "not_awakened");
            return false;
        }
        return true;
    }

    // ================================================================== Kamehameha
    static boolean kameStart(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        if (r.charging || !open(p, s) || !s.ready("kame", now)) return false;
        if (!SaiyanKi.canPay(p, SaiyanConfig.kameChargeCost())) {
            SaiyanAbilities.say(p, "no_ki");
            return false;
        }
        r.charging = true;
        r.kameStart = now;
        r.dirty = true;
        p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6f, 0.8f);
        return true;
    }

    private static double maxChargeSeconds(SaiyanState s) {
        return GokuRules.maxChargeSeconds(SaiyanConfig.kameChargeSeconds(), SaiyanTraining.mastery01("kame", s.uses("kame")));
    }

    /** The charge in percent (what the HUD shows), 0 when not charging. */
    static int chargePercent(SaiyanState s, SaiyanManager.Run r, long now) {
        return r.charging ? (int) Math.round(GokuRules.charge(now - r.kameStart, maxChargeSeconds(s)) * 100.0) : 0;
    }

    /** Stops a charge without firing (death, logout, race removal, a disabled system). */
    static void cancel(SaiyanManager.Run r) {
        r.charging = false;
        r.dirty = true;
    }

    /** Every tick while charging: the Ki per second, the motes at the hands, and a safety release five seconds after a full charge (a stuck key can not hold a charge forever). */
    static void chargeTick(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        if (!r.charging) return;
        if (!SaiyanConfig.enabled() || !SaiyanConfig.gokuEnabled() || !s.awakened || p.isSpectator() || !p.isAlive()) {
            cancel(r);
            return;
        }
        long held = now - r.kameStart;
        if (held > 0 && held % 20 == 0 && !SaiyanKi.pay(p, SaiyanConfig.kameChargeCost())) {
            cancel(r);
            SaiyanAbilities.say(p, "no_ki");
            return;
        }
        if (held % 10 == 0) {
            double c = GokuRules.charge(held, maxChargeSeconds(s));
            Vec3 at = p.getEyePosition().add(p.getLookAngle().scale(0.9)).add(0, -0.4, 0);
            Fx.sprayTinted(p.serverLevel(), FxThemes.WORLD, FxSlot.MOTE, 0x60C8FF, (float) (0.8 + c), at.x, at.y, at.z, 2, 0.12, 0.12, 0.12, 0.01);
        }
        if (held >= (long) (maxChargeSeconds(s) * 20.0) + 100L) kameRelease(p, s, r, now);
    }

    /** The release: fires what was charged (a release that came too soon fizzles). */
    static boolean kameRelease(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        if (!r.charging) return false;
        r.charging = false;
        r.dirty = true;
        long held = now - r.kameStart;
        if (!GokuRules.canFire(held, SaiyanConfig.kameMinTicks())) {
            SaiyanAbilities.say(p, "kame_fizzle");
            return true;
        }
        double charge = GokuRules.charge(held, maxChargeSeconds(s));
        if (!SaiyanKi.pay(p, GokuRules.kiPercent(SaiyanConfig.kameCost(), charge))) {
            SaiyanAbilities.say(p, "no_ki");
            return true;
        }
        s.startCooldown("kame", now, SaiyanConfig.kameCooldown());
        fire(p, s, charge);
        s.use("kame", 1);
        return true;
    }

    /** The beam: one raycast for the blocks, one query for the creatures along it, the first {@code pierce} foes on the line are hit. */
    private static void fire(ServerPlayer p, SaiyanState s, double charge) {
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getLookAngle();
        Vec3 from = p.getEyePosition().add(look.scale(0.8)).add(0, -0.25, 0);
        double range = GokuRules.range(SaiyanConfig.kameRange(), charge), width = GokuRules.width(charge);
        Vec3 far = from.add(look.scale(range));
        var block = level.clip(new ClipContext(from, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 end = block.getType() == HitResult.Type.MISS ? far : block.getLocation();
        double growth = SaiyanRules.growth(s.battlePower);
        double raw = GokuRules.damage(SaiyanConfig.kameDamage(), charge) * (1.0 + SaiyanConfig.kiDamageGrowth() * growth);
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, new AABB(from, end).inflate(width + 0.5), e -> e != p && e.isAlive());
        List<LivingEntity> line = new ArrayList<>();
        int seen = 0;
        for (LivingEntity t : near) {
            if (++seen > MAX_CANDIDATES) break;
            AABB box = t.getBoundingBox().inflate(width);
            Optional<Vec3> hit = box.clip(from, end);
            if ((hit.isPresent() || box.contains(from)) && SaiyanAbilities.allowed(p, t)) line.add(t);
        }
        line.sort(Comparator.comparingDouble((LivingEntity t) -> t.distanceToSqr(from)));
        int pierce = GokuRules.pierce(charge);
        long now = level.getGameTime();
        for (int i = 0; i < line.size() && i < pierce; i++) {
            LivingEntity t = line.get(i);
            if (SaiyanAbilities.hurt(p, t, raw, growth) > 0f && !BalanceLaw.isBoss(t)) t.knockback(GokuRules.knockback(charge), -look.x, -look.z);
            SaiyanCombat.trainOn(s, t, SaiyanTraining.Cat.KI, now);
        }
        beamFx(level, from, end, charge);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, (float) (0.4 + 0.5 * charge), 1.6f);
    }

    private static void beamFx(ServerLevel level, Vec3 from, Vec3 to, double charge) {
        Vec3 d = to.subtract(from);
        int n = (int) Math.min(MAX_BEAM_PARTICLES, Math.max(4, d.length() / 2.0));
        float scale = (float) (1.0 + 1.5 * charge);
        for (int i = 1; i <= n; i++) {
            Vec3 q = from.add(d.scale(i / (double) n));
            Fx.sprayTinted(level, FxThemes.WORLD, FxSlot.MOTE, 0x60C8FF, scale, q.x, q.y, q.z, 2, 0.12, 0.12, 0.12, 0.0);
        }
        Fx.spray(level, FxThemes.WORLD, FxOld.FLASH, to.x, to.y, to.z, 1, 0.0, 0.0, 0.0, 0.0);
        Fx.spray(level, FxThemes.WORLD, FxOld.ELECTRIC_SPARK, to.x, to.y, to.z, 8, 0.4, 0.4, 0.4, 0.1);
    }

    // ================================================================== Rock, Scissors 'N' Paper
    private static final String[] STAGE_KEYS = {"", "rsp_rock", "rsp_scissors", "rsp_paper"};

    /** The combo key: each press is the next part if it comes inside the window and not too soon; Rock starts a new combo. */
    static boolean combo(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        if (!open(p, s)) return false;
        int stage = GokuRules.nextStage(r.comboStage, now, r.comboAt, SaiyanConfig.rspGap(), SaiyanConfig.rspWindow());
        if (stage == 0 || (stage == GokuRules.ROCK && !s.ready("combo", now))) return false;
        if (!SaiyanAbilities.pay(p, s, SaiyanConfig.rspStamina(), SaiyanConfig.rspCost())) {
            r.comboStage = 0;
            return false;
        }
        boolean chained = GokuRules.chained(stage, r.comboStage);
        Vec3 look = p.getLookAngle();
        if (stage == GokuRules.ROCK) {
            Vec3 flat = new Vec3(look.x, 0.0, look.z);
            if (flat.lengthSqr() > 1.0e-6) {
                Vec3 push = flat.normalize().scale(0.6);
                p.setDeltaMovement(p.getDeltaMovement().add(push.x, 0.1, push.z));
                p.hurtMarked = true;
            }
        }
        double growth = SaiyanRules.growth(s.battlePower);
        double base = SaiyanConfig.rspDamage() * GokuRules.stageDamage(stage, chained) * SaiyanRules.damageFactor(SaiyanConfig.bonuses(s.battlePower).strength()) * SaiyanFormEngine.damageFactor(s, r);
        ServerLevel level = p.serverLevel();
        for (LivingEntity t : targetsInFront(p, look, GokuRules.stageReach(stage), GokuRules.stageTargets(stage, SaiyanConfig.rspPaperMax()))) {
            if (SaiyanAbilities.hurtBlow(p, t, base, growth) > 0f && !BalanceLaw.isBoss(t)) t.knockback(GokuRules.stageKnockback(stage), -look.x, -look.z);
            SaiyanCombat.trainOn(s, t, SaiyanTraining.Cat.MARTIAL, now);
            Fx.spray(level, FxThemes.WORLD, stage == GokuRules.PAPER ? FxOld.SONIC_BOOM : stage == GokuRules.SCISSORS ? FxOld.SWEEP_ATTACK : FxOld.CRIT, t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(), stage == GokuRules.ROCK ? 6 : 1, 0.2, 0.2, 0.2, 0.05);
        }
        r.lastCombat = now;
        SaiyanAbilities.say(p, STAGE_KEYS[stage]);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), stage == GokuRules.PAPER ? SoundEvents.GENERIC_EXPLODE.value() : SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.6f, 1.0f + 0.15f * stage);
        if (stage == GokuRules.PAPER) {
            s.startCooldown("combo", now, SaiyanConfig.rspCooldown());
            s.use("combo", 1);
            r.comboStage = 0;
        } else {
            r.comboStage = stage;
        }
        r.comboAt = now;
        r.dirty = true;
        return true;
    }

    /** The foes in front of the Saiyan inside the reach, nearest first, at most {@code max} (one query, at most {@link #MAX_CANDIDATES} looked at). */
    private static List<LivingEntity> targetsInFront(ServerPlayer p, Vec3 look, double reach, int max) {
        Vec3 eye = p.getEyePosition();
        AABB box = new AABB(eye, eye.add(look.scale(reach))).inflate(1.2);
        List<LivingEntity> near = p.serverLevel().getEntitiesOfClass(LivingEntity.class, box, e -> e != p && e.isAlive());
        double cone = max > 1 ? 0.5 : 0.8;
        List<LivingEntity> out = new ArrayList<>();
        int seen = 0;
        for (LivingEntity t : near) {
            if (++seen > MAX_CANDIDATES) break;
            Vec3 to = t.getBoundingBox().getCenter().subtract(eye);
            double dist = to.length();
            if (dist > reach + t.getBbWidth() * 0.5) continue;
            if (dist > 1.0e-6 && to.scale(1.0 / dist).dot(look) < cone) continue;
            if (SaiyanAbilities.allowed(p, t)) out.add(t);
        }
        out.sort(Comparator.comparingDouble((LivingEntity t) -> t.distanceToSqr(p)));
        return out.size() > max ? out.subList(0, max) : out;
    }

    /** Once a second: forget a combo whose window has run out (the HUD then shows no part). */
    static void second(SaiyanManager.Run r, long now) {
        if (r.comboStage > 0 && now - r.comboAt > SaiyanConfig.rspWindow()) {
            r.comboStage = 0;
            r.dirty = true;
        }
    }
}
