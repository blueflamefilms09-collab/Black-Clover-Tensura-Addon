package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import com.newuniverse.nusmp.particle.fx.FxRelic;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Wave 8: the bodies of the active abilities of the ten blades and axes. Every one runs on the server after the engine has settled the cost,
 * the cooldown and the attunement. State lives in the user's persistent data as absolute game times (keys in {@link Wave8Events}).
 */
final class Wave8Acts {
    private Wave8Acts() {}

    static final String K_SPOT = "nusmp_w8_spot_until", K_STALK = "nusmp_w8_stalk_until", K_FANG = "nusmp_w8_fang_until",
            K_SLAM = "nusmp_w8_slam_until", K_SLAM_T0 = "nusmp_w8_slam_t0", K_AWAKE = "nusmp_w8_awake_until",
            K_RET_AT = "nusmp_w8_ret_at", K_RET_X = "nusmp_w8_ret_x", K_RET_Y = "nusmp_w8_ret_y", K_RET_Z = "nusmp_w8_ret_z",
            K_IMMUNE = "nusmp_w8_eye_immune";

    /** Set while an ability deals its own damage, so the on-hit bonuses of {@link Wave8Events} do not stack onto it. */
    static boolean acting;

    private static final int COLD = 0x9FE3FF, SHADOW = 0x6A3CC8, LIGHT = 0xFFF2A0;

    // ================================================================ Spotlight
    /** Once a day: a beam of light finds you; for 20 seconds you see in the dark and your next blow lands true ("advantage on your next die roll"). */
    static void spotlightBeam(ActCtx c) {
        ServerPlayer p = c.player();
        long now = p.level().getGameTime();
        p.getPersistentData().putLong(K_SPOT, now + 400);
        Fx.addEffect(p, "minecraft:night_vision", 400, 0);
        ServerLevel l = p.serverLevel();
        for (int i = 0; i < 24; i++) com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_8, FxTone.LIGHT, p.getX(), p.getY() + 0.2 + i * 0.35, p.getZ(), 1, 0.15, 0.0, 0.15, 0.01);
        l.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.6f);
        Wave8Util.tell(p, "A beam of light finds you. Your next blow lands true.");
    }

    // ================================================================ Duality Blade
    static void streamers(ActCtx c) {
        ServerPlayer p = c.player();
        boolean on = c.isOn("streamers");
        Wave8Util.tell(p, on ? "Streamers of Light: the blade is wrapped in flowing light." : "Blade of Darkness: the light folds away.");
        ServerLevel l = p.serverLevel();
        FxRelic.burst(l, p.position(), on ? 0xE6DAFF : 0x3A2A55);
    }

    // ================================================================ Frostfang
    /** Cone of Cold (the card's 5th-level spell): a cone of frost ten blocks long. */
    static void coneOfCold(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel l = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        acting = true;
        try {
            for (LivingEntity t : Fx.foesAround(p, 10)) {
                Vec3 d = t.position().add(0, t.getBbHeight() * 0.5, 0).subtract(eye);
                if (!Wave8Rules.inCone(d.x, d.y, d.z, look.x, look.y, look.z, 10, 0.80)) continue;
                Fx.damageHeavy(p, t, 14f, "minecraft:freeze");
                Wave8Util.chill(t, 120);
                Fx.addEffect(t, "minecraft:slowness", 100, 2);
            }
        } finally {
            acting = false;
        }
        for (int i = 0; i < 70; i++) {
            double dist = 1.0 + l.random.nextDouble() * 9.0, spread = dist * 0.28;
            com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_8, FxTone.LIGHT, eye.x + look.x * dist, eye.y + look.y * dist - 0.2, eye.z + look.z * dist, 1, spread * 0.5, spread * 0.5, spread * 0.5, 0.02);
        }
        l.playSound(null, p.blockPosition(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.2f, 0.7f);
    }

    static void footfalls(ActCtx c) {
        boolean on = c.isOn("footfalls");
        Wave8Util.tell(c.player(), on ? "Footfalls of Ice: frost follows your steps." : "Footfalls of Ice: the frost stills.");
    }

    // ================================================================ Soul Shatter Blade
    /** Inflict Wounds (5 fragments): a necrotic touch on the foe in front of you; uses one of the day's charges. */
    static void inflictWounds(ActCtx c) {
        ServerPlayer p = c.player();
        int souls = Wave8Util.souls(p);
        if (Wave8Rules.maxCharges(souls) == 0) { Wave8Util.tell(p, "The blade is still hungry: 5 soul fragments wake it."); return; }
        if (Wave8Util.chargesLeft(p) <= 0) { Wave8Util.tell(p, "No charges left today (they return at dawn)."); return; }
        LivingEntity t = Wave8Util.aimed(p, 4.5, 0.85);
        if (t == null) { Wave8Util.tell(p, "Nothing within reach of the blade."); return; }
        Wave8Util.useCharge(p);
        acting = true;
        try {
            Fx.damage(p, t, 14f, "minecraft:wither");
        } finally {
            acting = false;
        }
        Fx.addEffect(t, "minecraft:wither", 60, 0);
        ServerLevel l = p.serverLevel();
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_8, FxTone.DARK, t.getX(), t.getY() + 1.0, t.getZ(), 14, 0.3, 0.5, 0.3, 0.04);
        Wave8Util.tell(p, "Inflict Wounds (" + Wave8Util.chargesLeft(p) + " charges left today)");
    }

    /** Animate Dead and Speak with Dead (20 fragments): two skeleton servants for a minute, and the dead name every living thing near you. */
    static void animateDead(ActCtx c) {
        ServerPlayer p = c.player();
        int souls = Wave8Util.souls(p);
        if (!Wave8Rules.canRaise(souls)) { Wave8Util.tell(p, "The dead do not answer yet: 20 soul fragments."); return; }
        if (Wave8Util.chargesLeft(p) <= 0) { Wave8Util.tell(p, "No charges left today (they return at dawn)."); return; }
        LivingEntity first = Fx.summon(p, "minecraft:skeleton", 1200);
        if (first == null) { Wave8Util.tell(p, "No one answers the blade."); return; }
        Wave8Util.useCharge(p);
        Fx.summon(p, "minecraft:skeleton", 1200);
        for (LivingEntity t : Fx.foesAround(p, 24)) Fx.addEffect(t, "minecraft:glowing", 120, 0);          // the dead speak of what lives near
        Fx.lendSkill(p, "tensura:magic_sense", 600);
        Wave8Util.tell(p, "Animate Dead. The dead whisper what lives near you.");
    }

    // ================================================================ Buster Sword
    /** Braver: leap high and come down with the whole blade; the landing (see the tick) is a shockwave. */
    static void braver(ActCtx c) {
        ServerPlayer p = c.player();
        long now = p.level().getGameTime();
        Fx.dash(p, 5);
        Vec3 dm = p.getDeltaMovement();
        p.setDeltaMovement(dm.x, 0.85, dm.z);
        p.hurtMarked = true;
        CompoundTag pd = p.getPersistentData();
        pd.putLong(K_SLAM, now + 70);
        pd.putLong(K_SLAM_T0, now);
    }

    static void slamLanding(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        acting = true;
        try {
            for (LivingEntity t : Fx.foesAround(p, 4.5)) {
                Fx.damage(p, t, 9f, "minecraft:player_attack");
                Vec3 away = t.position().subtract(p.position());
                Vec3 n = away.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 0) : away.normalize();
                t.setDeltaMovement(n.x * 0.7, 0.45, n.z * 0.7);
                t.hurtMarked = true;
            }
        } finally {
            acting = false;
        }
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_8, FxTone.LIGHT, p.getX(), p.getY() + 0.2, p.getZ(), 3, 1.2, 0.1, 1.2, 0.0);
        FxRelic.ring(l, p.position(), 4.0, 0xC8CED6);
        l.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7f, 1.3f);
    }

    /** Cross-Slash: two cuts in an X in front of you. */
    static void crossSlash(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel l = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        acting = true;
        try {
            for (LivingEntity t : Fx.foesAround(p, 4.5)) {
                Vec3 d = t.position().add(0, t.getBbHeight() * 0.5, 0).subtract(eye);
                if (!Wave8Rules.inCone(d.x, d.y, d.z, look.x, look.y, look.z, 4.5, 0.5)) continue;
                Fx.damage(p, t, 8f, "minecraft:player_attack");
                Fx.addEffect(t, "minecraft:slowness", 40, 1);
            }
        } finally {
            acting = false;
        }
        Vec3 at = eye.add(look.scale(2.2));
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_8, FxTone.LIGHT, at.x, at.y - 0.3, at.z, 2, 0.5, 0.3, 0.5, 0.0);
        l.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    // ================================================================ Dragon Tooth Blade
    /** Searing Fang: for ten seconds every blow sets the foe alight. */
    static void searingFang(ActCtx c) {
        ServerPlayer p = c.player();
        p.getPersistentData().putLong(K_FANG, p.level().getGameTime() + 200);
        ServerLevel l = p.serverLevel();
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_8, FxTone.LIGHT, p.getX(), p.getY() + 1.0, p.getZ(), 20, 0.4, 0.5, 0.4, 0.04);
        l.playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8f, 0.8f);
        Wave8Util.tell(p, "The fang burns: your blows set foes alight for 10 seconds.");
    }

    // ================================================================ Corpse Eater
    /** The eye opens: those in front of you who see it must resist or be paralysed; a resisting creature is immune for five minutes. */
    static void eyeGaze(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel l = p.serverLevel();
        long now = l.getGameTime();
        boolean control = BalanceLaw.beginControl(p);
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        int hit = 0;
        for (LivingEntity t : Fx.foesAround(p, 12)) {
            Vec3 d = t.position().add(0, t.getBbHeight() * 0.5, 0).subtract(eye);
            if (!Wave8Rules.inCone(d.x, d.y, d.z, look.x, look.y, look.z, 12, 0.5) || !p.hasLineOfSight(t)) continue;
            CompoundTag td = t.getPersistentData();
            if (td.getLong(K_IMMUNE) > now) continue;
            if (!control) continue;                                                                    // the balance law refuses hard control right now: no one is held, no one is made immune
            int pct = Wave8Rules.savePercent(t instanceof Player, BalanceLaw.isBoss(t), t.getMaxHealth(), 40);
            if (Wave8Rules.saves(l.random.nextInt(100), pct)) {
                td.putLong(K_IMMUNE, now + 6000);
                continue;
            }
            Wave8Util.paralyse(t, BalanceLaw.controlTicks(t, 100));
            com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_8, FxTone.DARK, t.getX(), t.getEyeY(), t.getZ(), 8, 0.2, 0.2, 0.2, 0.02);
            hit++;
        }
        l.playSound(null, p.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.5f, 0.6f);
        Wave8Util.tell(p, hit > 0 ? "The eye holds " + hit + " in its gaze." : "The eye opens, and finds no one who fails to resist.");
    }

    /** Rending Bite: the blade tears the foe in front of you; the wound festers and the wielder is fed. */
    static void rendingBite(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity t = Wave8Util.aimed(p, 5.0, 0.8);
        if (t == null) { Wave8Util.tell(p, "Nothing within the blade's bite."); return; }
        acting = true;
        try {
            Fx.damage(p, t, 8f, "minecraft:wither");
        } finally {
            acting = false;
        }
        Fx.addEffect(t, "minecraft:wither", 80, 1);
        Fx.heal(p, 3f);
        com.newuniverse.nusmp.particle.fx.Fx.spray(p.serverLevel(), FxThemes.RELIC_WAVE_8, FxTone.DARK, t.getX(), t.getY() + 1.0, t.getZ(), 10, 0.3, 0.4, 0.3, 0.1);
    }

    // ================================================================ Random Axe of Charity
    /** Throws the handaxe at the foe in front of you (12 blocks). The axe is back in hand at once: it is a thrown strike, not a lost weapon. */
    static void throwHandaxe(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity t = Wave8Util.aimed(p, 12.0, 0.93);
        if (t == null || !p.hasLineOfSight(t)) { Wave8Util.tell(p, "No clear throw."); return; }
        acting = true;
        try {
            Fx.damage(p, t, 7f, "minecraft:player_attack");
        } finally {
            acting = false;
        }
        trail(p.serverLevel(), p.getEyePosition().add(0, -0.3, 0), t.position().add(0, t.getBbHeight() * 0.5, 0), com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_8, FxTone.LIGHT), 14);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.8f, 1.4f);
    }

    // ================================================================ Leviathan Axe
    /** Throws the axe in a straight line (20 blocks, stopped by walls): it chills everything it passes and comes back to the hand a moment later. */
    static void leviathanThrow(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel l = p.serverLevel();
        Vec3 from = p.getEyePosition().add(0, -0.3, 0), dir = p.getLookAngle();
        double len = 20.0;
        HitResult hit = p.pick(len, 1.0f, false);
        if (hit.getType() == HitResult.Type.BLOCK) len = Math.max(1.0, Math.sqrt(hit.getLocation().distanceToSqr(p.getEyePosition())) - 0.3);
        Vec3 to = from.add(dir.scale(len));
        passLine(p, from, to, 10f, true);
        trail(l, from, to, com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_8, FxTone.LIGHT), (int) (len * 2));
        CompoundTag pd = p.getPersistentData();
        pd.putLong(K_RET_AT, l.getGameTime() + 14);
        pd.putDouble(K_RET_X, to.x);
        pd.putDouble(K_RET_Y, to.y);
        pd.putDouble(K_RET_Z, to.z);
        l.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0f, 0.6f);
    }

    /** The recall: the axe flies back along a line to the hand, hurting and dragging whatever it meets. Called from the player tick when it is due. */
    static void leviathanReturn(ServerPlayer p, CompoundTag pd) {
        Vec3 from = new Vec3(pd.getDouble(K_RET_X), pd.getDouble(K_RET_Y), pd.getDouble(K_RET_Z));
        Vec3 to = p.getEyePosition().add(0, -0.3, 0);
        passLine(p, from, to, 6f, false);
        trail(p.serverLevel(), from, to, com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_8, FxTone.LIGHT), (int) Math.max(6, from.distanceTo(to) * 2));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    private static void passLine(ServerPlayer p, Vec3 a, Vec3 b, float damage, boolean outward) {
        double reach = a.distanceTo(b) + 3.0;
        acting = true;
        try {
            List<LivingEntity> foes = Fx.foesAround(p, Math.min(32.0, reach));
            for (LivingEntity t : foes) {
                Vec3 m = t.position().add(0, t.getBbHeight() * 0.5, 0);
                if (Wave8Rules.segDist2(m.x, m.y, m.z, a.x, a.y, a.z, b.x, b.y, b.z) > 1.7 * 1.7) continue;
                if (outward || !(t instanceof Player)) Fx.damage(p, t, damage, "minecraft:freeze");      // one throw hurts a player once (out), the return only drags and chills
                Wave8Util.chill(t, 100);
                if (outward) {
                    Fx.addEffect(t, "minecraft:slowness", 60, 3);
                } else {
                    Vec3 pull = p.position().subtract(t.position());
                    Vec3 n = pull.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 0) : pull.normalize();
                    t.setDeltaMovement(n.x * 0.8, 0.2, n.z * 0.8);
                    t.hurtMarked = true;
                }
            }
        } finally {
            acting = false;
        }
    }

    /** Frost Awakening: for twelve seconds the axe's runes burn and a freezing fog hangs around you (see the tick). */
    static void frostAwakening(ActCtx c) {
        ServerPlayer p = c.player();
        p.getPersistentData().putLong(K_AWAKE, p.level().getGameTime() + 240);
        FxRelic.ring(p.serverLevel(), p.position(), 3.5, COLD);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 0.5f);
        Wave8Util.tell(p, "The runes of the axe awaken.");
    }

    // ================================================================ Shadow Burn
    /** Burning Hold: the blade is driven into the foe's shadow; a failed save means fire and paralysis, a passed one only half the fire. */
    static void burningHold(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel l = p.serverLevel();
        LivingEntity t = Wave8Util.aimed(p, 5.5, 0.8);
        if (t == null) { Wave8Util.tell(p, "No shadow within the blade's reach."); return; }
        boolean tooBig = t.getBbHeight() > p.getBbHeight() * 1.5f;                                      // "must be your size or smaller"
        int pct = Wave8Rules.savePercent(t instanceof Player, BalanceLaw.isBoss(t), t.getMaxHealth(), 45);
        boolean saved = tooBig || Wave8Rules.saves(l.random.nextInt(100), pct);
        acting = true;
        try {
            Fx.damage(p, t, saved ? 4.5f : 9f, "minecraft:on_fire");
        } finally {
            acting = false;
        }
        t.setRemainingFireTicks(saved ? 40 : 100);
        if (!saved && BalanceLaw.beginControl(p)) Wave8Util.paralyse(t, BalanceLaw.controlTicks(t, 100));
        com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_8, FxTone.LIGHT, t.getX(), t.getY() + 0.1, t.getZ(), 18, 0.4, 0.05, 0.4, 0.03);
        FxRelic.ring(l, t.position(), 1.4, SHADOW);
        Wave8Util.tell(p, saved ? "Burning Hold: it slips half free." : "Burning Hold: pinned in its own shadow.");
    }

    /** Shadow Stalk (two a day): sink into your shadow and rise behind a foe within twenty blocks, with "advantage" on your next blow. */
    static void shadowStalk(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel l = p.serverLevel();
        LivingEntity t = null;
        Vec3 look = p.getLookAngle();
        double best = -2.0;
        for (LivingEntity f : Fx.foesAround(p, 20)) {                                                    // the foe nearest the line of sight wins
            Vec3 d = f.position().subtract(p.position());
            double n = d.length();
            if (n < 1.0e-3) continue;
            double dot = d.dot(look) / n;
            if (dot > best) { best = dot; t = f; }
        }
        if (t == null) { Wave8Util.tell(p, "No shadow to step into within 20 blocks."); return; }
        Vec3 face = t.getLookAngle();
        double[] xz = Wave8Rules.behind(t.getX(), t.getZ(), face.x, face.z, 1.6);
        Vec3 spot = Wave8Util.landing(l, p, xz[0], t.getY(), xz[1]);
        if (spot == null) spot = Wave8Util.landing(l, p, t.getX() + 1.6, t.getY(), t.getZ());
        if (spot == null) spot = Wave8Util.landing(l, p, t.getX() - 1.6, t.getY(), t.getZ());
        if (spot == null) { Wave8Util.tell(p, "There is no room to rise there."); return; }
        FxRelic.burst(l, p.position(), SHADOW);
        double dx = t.getX() - spot.x, dz = t.getZ() - spot.z;
        float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        p.teleportTo(l, spot.x, spot.y, spot.z, yaw, 0.0f);
        p.fallDistance = 0;
        FxRelic.burst(l, spot, SHADOW);
        p.getPersistentData().putLong(K_STALK, l.getGameTime() + 120);
        Wave8Util.tell(p, "Shadow Stalk: your next blow lands true.");
    }

    // ================================================================ shared
    private static void trail(ServerLevel l, Vec3 a, Vec3 b, net.minecraft.core.particles.ParticleOptions type, int steps) {
        int n = Math.max(2, Math.min(60, steps));
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_8, com.newuniverse.nusmp.particle.fx.Fx.toneOf(type), a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }
}
