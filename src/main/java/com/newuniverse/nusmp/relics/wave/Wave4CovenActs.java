package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.antimagic.NihilityZone;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * Wave 4: the three powers of the Amulet of the Coven (a hag goddess's eye).
 * <ul>
 *   <li><b>Distant Watcher</b>: the eye looks out, and every hostile creature within 40 blocks in front of you glows for 12 s; Tensura's
 *       Magic Sense and Danger Sense are lent to you for 15 s.</li>
 *   <li><b>Bloody Presage</b>: Scrying. Three hearts of your own blood (never the last of it) open the eye on what you look at (or on
 *       whoever last drew your blood, wherever it is in this world) and you learn where it is (and, with Sage lent for 30 s, how much of its
 *       magicules it has left); once per game day.</li>
 *   <li><b>Weakening Gaze</b>: a 10-block burst of anti-magic (the mod's Nihility zone for 8 s: barriers shatter, foes are silenced and their
 *       Tensura skills jammed): foes lose Resistance, Fire Resistance and Absorption, are weakened and take 30 percent more from every blow.</li>
 * </ul>
 */
final class Wave4CovenActs {
    private Wave4CovenActs() {}

    static final double WATCH_RADIUS = 40.0;
    static final int WATCH_GLOW = 240;
    static final double GAZE_RADIUS = 10.0;
    static final int GAZE_TICKS = 160;
    static final float GAZE_EXTRA = 1.3f;
    static final float PRESAGE_COST = 6f;
    static final double PRESAGE_RANGE = 96.0;
    static final int PRESAGE_GLOW = 400;
    static final int PRESAGE_BLOOD_GLOW = 800;
    static final int BLOOD_MEMORY = 1200;
    static final int RUBY = 0xC8202A;
    /** The eye's Tensura senses: lent for 15 s when it opens, and for 30 s (Sage) when it scries. */
    static final int WATCH_SKILL_TICKS = 300;
    static final int PRESAGE_SKILL_TICKS = 600;

    /** Distant Watcher. */
    static void watcher(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel lv = p.serverLevel();
        Vec3 look = p.getLookAngle();
        int seen = 0;
        for (LivingEntity e : Fx.foesAround(p, WATCH_RADIUS)) {
            Vec3 d = e.position().subtract(p.position());
            if (d.dot(look) <= 0) continue;                                    // the eye sees only the half in front of you
            Fx.addEffect(e, "minecraft:glowing", WATCH_GLOW, 0);
            seen++;
        }
        Wave4Look.ambient(p, "minecraft:night_vision", 300, 0);
        Fx.lendSkills(p, WATCH_SKILL_TICKS, "tensura:magic_sense", "tensura:danger_sense");      // it sees through walls the way Magic Sense does
        Fx.ring(lv, p.position().add(0, 1.0, 0), 2.5, RUBY);
        Fx.say(p, seen == 0 ? "The eye opens and sees nothing hostile ahead (Magic Sense and Danger Sense stay with you for 15 seconds)."
                : "The eye opens: " + seen + (seen == 1 ? " creature glows" : " creatures glow") + " before you (Magic Sense and Danger Sense stay with you for 15 seconds).");
    }

    /** Bloody Presage (Scrying). */
    static void presage(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel lv = p.serverLevel();
        final long now = lv.getGameTime();
        CompoundTag t = Wave4Store.read(c.stack());
        long last = t.contains(Wave4Store.K_PRESAGE, 4) ? t.getLong(Wave4Store.K_PRESAGE) : -1L;
        if (!Wave4Charges.ready(last, now, Wave4Charges.DAY)) {
            Fx.say(p, "The eye has drunk once already: it opens again in about " + Wave4Charges.minutesLeft(last, now, Wave4Charges.DAY) + " minutes.");
            return;
        }
        if (p.getHealth() <= PRESAGE_COST + 2f) {
            Fx.say(p, "You have too little blood left to pour.");
            return;
        }
        LivingEntity hurter = p.getLastHurtByMob();
        boolean fresh = hurter != null && hurter.isAlive() && p.tickCount - p.getLastHurtByMobTimestamp() < BLOOD_MEMORY;
        LivingEntity target = Wave4Look.target(p, PRESAGE_RANGE, false, e -> true);
        boolean blood = false;
        if (target == null && fresh && hurter.level() == p.level() && Wave4Look.plain(hurter)) {
            target = hurter;
            blood = true;
        } else if (target != null && fresh && target == hurter) {
            blood = true;
        }
        if (target == null) {
            Fx.say(p, "Look at a creature, or open the eye after something has drawn your blood.");
            return;
        }
        p.setHealth(p.getHealth() - PRESAGE_COST);                             // the vial: it cannot kill you
        Wave4Store.edit(c.stack(), x -> x.putLong(Wave4Store.K_PRESAGE, now));
        Fx.addEffect(target, "minecraft:glowing", blood ? PRESAGE_BLOOD_GLOW : PRESAGE_GLOW, 0);
        Fx.ring(lv, p.position().add(0, 1.0, 0), 1.5, RUBY);
        if (target.level() instanceof ServerLevel tl) Fx.ring(tl, target.position().add(0, target.getBbHeight() * 0.5, 0), 1.2, RUBY);
        BlockPosText at = new BlockPosText(target);
        Fx.lendSkill(p, "tensura:sage", PRESAGE_SKILL_TICKS);                                   // the eye reads its target through Sage for half a minute
        int mp = Wave4Look.magiculePercent(target);
        Fx.say(p, String.format(Locale.ROOT, "Scrying: %s, %.0f/%.0f health%s, at %d %d %d (%s).%s", target.getName().getString(),
                target.getHealth(), target.getMaxHealth(), mp >= 0 ? ", " + mp + "% of its magicules" : "", at.x, at.y, at.z, at.dimension,
                blood ? " Your blood binds it: it cannot hide, and the eye keeps it lit twice as long." : ""));
        if (target instanceof ServerPlayer tp && tp != p) Fx.say(tp, "You feel an unseen eye settle on you.");
    }

    /** Weakening Gaze. */
    static void gaze(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel lv = p.serverLevel();
        long now = lv.getGameTime();
        int n = 0;
        for (LivingEntity e : Fx.foesAround(p, GAZE_RADIUS)) {
            e.removeEffect(MobEffects.DAMAGE_RESISTANCE);
            e.removeEffect(MobEffects.FIRE_RESISTANCE);
            e.removeEffect(MobEffects.ABSORPTION);
            Fx.addEffect(e, "minecraft:weakness", GAZE_TICKS, 0);
            Wave4State.GAZE.put(e.getUUID(), now + GAZE_TICKS, now);
            n++;
        }
        Vec3 at = p.position().add(0, 1.0, 0);
        NihilityZone.start(lv, p, p.position().add(0, 0.2, 0), GAZE_RADIUS, GAZE_TICKS);       // the anti-magic itself: barriers shatter, foes are silenced and their skills jammed for the 8 seconds
        Fx.ring(lv, at, GAZE_RADIUS, RUBY);
        Fx.burst(lv, at, RUBY);
        Fx.say(p, n == 0 ? "The gaze opens; nothing hostile is near enough to feel it."
                : "The gaze strips " + n + (n == 1 ? " foe" : " foes") + " of their defences for 8 seconds (they take 30% more, are silenced, and their skills are jammed).");
    }

    /** Where a creature is, as text. */
    private static final class BlockPosText {
        final int x, y, z;
        final String dimension;
        BlockPosText(LivingEntity e) {
            x = (int) Math.floor(e.getX());
            y = (int) Math.floor(e.getY());
            z = (int) Math.floor(e.getZ());
            dimension = e.level().dimension().location().toString();
        }
    }
}
