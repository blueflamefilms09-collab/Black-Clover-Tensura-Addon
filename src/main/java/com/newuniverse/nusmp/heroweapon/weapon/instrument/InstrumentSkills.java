package com.newuniverse.nusmp.heroweapon.weapon.instrument;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Effects;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Foes;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Geo;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.PlayerState;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Safe;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the bodies of the Vassal Musical Instrument's techniques, kept apart from {@link InstrumentLogic} so each can be read on its own. The weapon is the violin of
 * Glass' world (the Shield Hero lore: notes shot like arrows, a guitar that stuns, a poisonous melody, a grilled-fish instrument that helps digestion, a "0" form that
 * counters illegitimate power; the series says an untrained wielder's notes fly in random directions, which is the idea behind the harmony below). Every technique goes
 * through {@link #perform}: the gap since the last technique decides the harmony of the phrase ({@link Tempo}), and the harmony tightens the notes and lengthens the
 * songs. The songs are auras that follow the wielder and stop when the instrument leaves their hand; the notes are hitscan lines. It is NOT the voice-driven Song Magic
 * of {@code song/**}: no microphone, no per-pulse magicule bill, fixed techniques, and the rhythm is the timing of the keys. Damage goes through {@code HeroCtx.hit},
 * healing through {@code HeroCtx.heal}, hard control behind {@code HeroCtx.control}; no block is ever changed.
 */
final class InstrumentSkills {
    private InstrumentSkills() {}

    /** The beat of each wielder's current phrase; dropped on logout and when the server stops. */
    static final PlayerState<Tempo> TEMPO = new PlayerState<>(Tempo::new);

    private static final int PINK = 0xFFFFB4E8, GOLD = 0xFFFFD27A, RED = 0xFFFF7A5C, LAVENDER = 0xFFB9B4FF, PURPLE = 0xFFC070D8, PEACH = 0xFFFFC080, WHITE = 0xFFFFFFFF;
    private static final int PINK_RGB = 0xFFB4E8;

    /** A technique that wants to know the harmony of the phrase it is part of; returns false to refuse (nothing counts then). */
    @FunctionalInterface
    interface Perf {
        boolean run(HeroCtx c, int harmony);
    }

    /** Runs a technique inside the beat: previews the harmony, runs it, and only when it really happened counts it into the phrase. */
    static boolean perform(HeroCtx c, Perf perf) {
        Tempo t = TEMPO.of(c.player);
        long now = c.level.getGameTime();
        boolean steady = c.form.has("steady_beat");
        if (!perf.run(c, t.preview(now, steady))) return false;
        int earned = t.commit(now, steady);
        if (earned > 0) c.say("Harmony " + earned + "/" + Tempo.MAX);
        return true;
    }

    private static void note(HeroCtx c, int harmony, float volume) {
        c.sound(SoundEvents.NOTE_BLOCK_HARP.value(), volume, 0.7f + 0.15f * harmony);
    }

    private static double radius(HeroCtx c, double base) {
        return c.form.has("fairy_choir") ? base * InstrumentNumbers.CHOIR_RADIUS : base;
    }

    private static Vec3 scatter(HeroCtx c, Vec3 look, double deg) {
        if (deg <= 0.0) return look;
        RandomSource r = c.player.getRandom();
        double[] d = Geo.aimOff(look.x, look.y, look.z, (r.nextDouble() * 2 - 1) * deg, (r.nextDouble() * 2 - 1) * deg * 0.5);
        return new Vec3(d[0], d[1], d[2]);
    }

    // ================================================================== Music Strike
    /** A note shot like an arrow (canon: Music Strike). It strays with no harmony and flies true with it. */
    static boolean musicStrike(HeroCtx c, int h) {
        ServerPlayer p = c.player;
        Vec3 eye = c.eye(), dir = scatter(c, c.look(), InstrumentNumbers.spreadDeg(h, c.form.has("perfect_pitch")));
        LivingEntity t = Foes.ray(p, eye, dir, InstrumentNumbers.STRIKE_RANGE, InstrumentNumbers.STRIKE_WIDTH);
        Vec3 end = t != null ? t.getBoundingBox().getCenter() : Foes.wallEnd(p, eye, eye.add(dir.scale(InstrumentNumbers.STRIKE_RANGE)));
        c.fx(VfxShape.STAR_MAGIC, eye.add(dir.scale(0.8)), end, PINK, 10, 0.8f);
        note(c, h, 1.0f);
        if (t == null) {
            c.say("The note finds no one.");
            return false;
        }
        c.hit(t, (float) InstrumentNumbers.strikeRaw(h));
        VfxSpawn.sendFollowing(c.level, VfxShape.SONG_NOTES, t, t.position().add(0, t.getBbHeight(), 0), PINK, 30, 0.8f);
        return true;
    }

    // ================================================================== Demon's Melody
    /** Countless notes (canon: Demon's Melody): a fan of seven that strays less as the harmony builds; a foe takes at most three of them. */
    static boolean demonsMelody(HeroCtx c, int h) {
        ServerPlayer p = c.player;
        Vec3 eye = c.eye(), look = c.look();
        double half = InstrumentNumbers.melodyAngle(h, c.form.has("perfect_pitch"));
        RandomSource r = p.getRandom();
        Map<LivingEntity, Integer> hits = new LinkedHashMap<>();
        for (int k = 0; k < InstrumentNumbers.MELODY_NOTES; k++) {
            double[] d = Geo.aimOff(look.x, look.y, look.z, (r.nextDouble() * 2 - 1) * half, (r.nextDouble() * 2 - 1) * half * 0.5);
            Vec3 dir = new Vec3(d[0], d[1], d[2]);
            LivingEntity t = Foes.ray(p, eye, dir, InstrumentNumbers.MELODY_RANGE, InstrumentNumbers.MELODY_WIDTH);
            Vec3 end = t != null ? t.getBoundingBox().getCenter() : Foes.wallEnd(p, eye, eye.add(dir.scale(InstrumentNumbers.MELODY_RANGE)));
            c.fx(VfxShape.STAR_MAGIC, eye.add(dir.scale(0.8)), end, PINK, 8, 0.5f);
            if (t != null) hits.merge(t, 1, Integer::sum);
        }
        note(c, h, 1.1f);
        if (hits.isEmpty()) {
            c.say("The melody finds no one.");
            return false;
        }
        int touched = 0;
        for (Map.Entry<LivingEntity, Integer> e : hits.entrySet()) {
            if (touched++ >= InstrumentNumbers.DEMONS_MELODY.targets()) break;
            c.hit(e.getKey(), (float) InstrumentNumbers.melodyDamage(e.getValue()));
        }
        return true;
    }

    // ================================================================== Stun Beat
    /** The guitar's note (canon: Stun Beat): it flies to a point and bursts in a flash that hurts and, behind the control lock, stuns and dazzles. */
    static boolean stunBeat(HeroCtx c, int h) {
        ServerPlayer p = c.player;
        LivingEntity aimed = Foes.target(p, InstrumentNumbers.STUN_RANGE);
        Vec3 point = aimed != null ? aimed.position() : Foes.groundBelow(p, c.aim(InstrumentNumbers.STUN_RANGE), 6.0);
        if (Foes.around(p, point, InstrumentNumbers.STUN_RADIUS, 1).isEmpty()) {
            c.say("No one stands where the beat would fall.");
            return false;
        }
        Tells.ring(c.level, point, InstrumentNumbers.STUN_RADIUS, PINK_RGB);
        c.fx(VfxShape.STAR_MAGIC, c.eye().add(c.look().scale(0.8)), point.add(0, 1, 0), PINK, InstrumentNumbers.STUN_FLIGHT, 1.0f);
        note(c, h, 1.2f);
        Safe.later(c, InstrumentNumbers.STUN_FLIGHT, "instrument.stun", f -> {
            f.fx(VfxShape.LIGHT_FLARE, point.add(0, 1, 0), point.add(0, 1, 0), WHITE, 16, 1.6f);
            f.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4f, 1.9f);
            List<LivingEntity> foes = Foes.around(f.player, point, InstrumentNumbers.STUN_RADIUS, InstrumentNumbers.STUN_BEAT.targets());
            if (foes.isEmpty()) return;
            boolean stun = f.control();
            for (LivingEntity t : foes) {
                f.hit(t, (float) InstrumentNumbers.STUN_BEAT.raw());
                if (!stun) continue;
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(t, InstrumentNumbers.STUN_CONTROL_TICKS), 9, false, true));
                t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, InstrumentNumbers.STUN_BLIND_TICKS, 0, false, true));
            }
        });
        return true;
    }

    // ================================================================== the songs (auras)
    @FunctionalInterface
    private interface Pulse {
        void run(HeroCtx f, List<LivingEntity> allies, List<LivingEntity> foes);
    }

    /**
     * An aura round the wielder: it follows them, pulses every second for the song's length (longer with harmony), and ends when the instrument leaves their hand.
     * {@code allies} and {@code foes} say which of the two lists each pulse is given (the other is empty).
     */
    private static void sing(HeroCtx c, int h, int baseTicks, int color, boolean allies, boolean foes, Pulse pulse) {
        double radius = radius(c, InstrumentNumbers.SONG_RADIUS);
        Vec3 foot = c.player.position();
        c.fx(VfxShape.SONG_RING, foot, foot, color, 28, (float) radius);
        VfxSpawn.sendFollowing(c.level, VfxShape.SONG_NOTES, c.player, foot.add(0, 1.6, 0), color, 60, 1.0f);
        note(c, h, 1.2f);
        Safe.zone(c, InstrumentNumbers.songTicks(baseTicks, h), InstrumentNumbers.PULSE_EVERY, "instrument.song", true, (f, age) -> {
            Vec3 here = f.player.position();
            pulse.run(f, allies ? Foes.alliesAround(f.player, here, radius, 8) : List.of(), foes ? Foes.around(f.player, here, radius, 8) : List.of());
            if (age > 0 && age % (2 * InstrumentNumbers.PULSE_EVERY) == 0) f.fx(VfxShape.SONG_RING, here, here, color, 24, (float) radius);
            return true;
        });
    }

    /** The March (canon: the Vanguard's march): allies in range move faster and work faster; Speed II from harmony three. */
    static boolean march(HeroCtx c, int h) {
        sing(c, h, InstrumentNumbers.MARCH_TICKS, GOLD, true, false, (f, allies, foes) -> {
            for (LivingEntity a : allies) {
                a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, InstrumentNumbers.EFFECT_TICKS, InstrumentNumbers.marchAmp(h), false, true));
                a.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, InstrumentNumbers.EFFECT_TICKS, 0, false, true));
            }
        });
        return true;
    }

    /** The War Song: allies in range hit harder (Strength I), and from harmony three take less damage (Resistance I). */
    static boolean warSong(HeroCtx c, int h) {
        sing(c, h, InstrumentNumbers.WAR_TICKS, RED, true, false, (f, allies, foes) -> {
            for (LivingEntity a : allies) {
                a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, InstrumentNumbers.EFFECT_TICKS, 0, false, true));
                if (h >= InstrumentNumbers.STRONG_HARMONY) a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, InstrumentNumbers.EFFECT_TICKS, 0, false, true));
            }
        });
        return true;
    }

    /** The Lullaby: a slow, drowsy tune that slows and weakens the foes in range (behind the shared control lock). */
    static boolean lullaby(HeroCtx c, int h) {
        ServerPlayer p = c.player;
        double radius = radius(c, InstrumentNumbers.LULLABY_RADIUS);
        List<LivingEntity> foes = Foes.around(p, p.position(), radius, InstrumentNumbers.LULLABY.targets());
        if (foes.isEmpty()) {
            c.say("No one is near enough to hear it.");
            return false;
        }
        if (!c.control()) {
            c.say("Your control magic needs " + (BalanceLaw.controlCooldownLeft(p) + 19) / 20 + "s.");
            return false;
        }
        for (LivingEntity t : foes) {
            int ticks = BalanceLaw.controlTicks(t, InstrumentNumbers.LULLABY_TICKS);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 2, false, true));
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0, false, true));
            if (t instanceof Mob m) m.getNavigation().stop();
        }
        Vec3 foot = p.position();
        c.fx(VfxShape.SONG_RING, foot, foot, LAVENDER, 28, (float) radius);
        VfxSpawn.sendFollowing(c.level, VfxShape.SONG_NOTES, p, foot.add(0, 1.6, 0), LAVENDER, 50, 1.0f);
        c.sound(SoundEvents.NOTE_BLOCK_FLUTE.value(), 1.2f, 0.7f);
        return true;
    }

    /**
     * The Dirge (canon: Deadly Poison Music): a funeral tune that poisons and weakens the foes in range; it cannot kill a player (poison never does). Both effects
     * are level I on purpose: poison is damage that does not go through the balance law, and level I over eight seconds is about six health, inside the ordinary
     * budget and under the cap a single blow may take from a player.
     */
    static boolean dirge(HeroCtx c, int h) {
        ServerPlayer p = c.player;
        double radius = radius(c, InstrumentNumbers.DIRGE_RADIUS);
        List<LivingEntity> foes = Foes.around(p, p.position(), radius, InstrumentNumbers.DIRGE.targets());
        if (foes.isEmpty()) {
            c.say("No one is near enough to hear it.");
            return false;
        }
        int ticks = InstrumentNumbers.DIRGE_TICKS + (h >= InstrumentNumbers.STRONG_HARMONY ? InstrumentNumbers.DIRGE_EXTRA_TICKS : 0);
        for (LivingEntity t : foes) {
            t.addEffect(new MobEffectInstance(MobEffects.POISON, ticks, 0, false, true));
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0, false, true));
        }
        Vec3 foot = p.position();
        c.fx(VfxShape.SONG_RING, foot, foot, PURPLE, 28, (float) radius);
        VfxSpawn.sendFollowing(c.level, VfxShape.SONG_NOTES, p, foot.add(0, 1.6, 0), PURPLE, 50, 1.0f);
        c.sound(SoundEvents.NOTE_BLOCK_BASS.value(), 1.3f, 0.6f);
        return true;
    }

    // ================================================================== Glutton God Tango
    /** The fish-shaped instrument's tune (canon: Glutton God Tango, "enhances digestion"): allies in range eat a little and mend a little. */
    static boolean gluttonGodTango(HeroCtx c, int h) {
        ServerPlayer p = c.player;
        boolean any = false;
        for (LivingEntity a : Foes.alliesAround(p, p.position(), InstrumentNumbers.GLUTTON_RADIUS, InstrumentNumbers.GLUTTON.targets())) {
            boolean fed = false;
            if (a instanceof Player pl && pl.getFoodData().needsFood()) {
                pl.getFoodData().eat(InstrumentNumbers.GLUTTON_FOOD, InstrumentNumbers.GLUTTON_SATURATION);
                fed = true;
            }
            boolean hurt = a.getHealth() < a.getMaxHealth();
            if (hurt) c.heal(a, (float) (a.getMaxHealth() * InstrumentNumbers.GLUTTON_HEAL_SHARE));
            if (fed || hurt) any = true;
        }
        if (!any) {
            c.say("No one needs a meal.");
            return false;
        }
        Vec3 foot = p.position();
        c.fx(VfxShape.SONG_RING, foot, foot, PEACH, 28, (float) InstrumentNumbers.GLUTTON_RADIUS);
        VfxSpawn.sendFollowing(c.level, VfxShape.SONG_NOTES, p, foot.add(0, 1.6, 0), PEACH, 50, 1.0f);
        c.sound(SoundEvents.NOTE_BLOCK_BANJO.value(), 1.2f, 0.9f);
        return true;
    }

    // ================================================================== Zero Note
    /** Musical Instrument 0 (canon: no damage, effective against illegitimate power): cleanses two harmful effects and a sealed grimoire from every ally in range. */
    static boolean zeroNote(HeroCtx c, int h) {
        ServerPlayer p = c.player;
        long now = c.level.getGameTime();
        int freed = 0;
        for (LivingEntity a : Foes.alliesAround(p, p.position(), InstrumentNumbers.ZERO_RADIUS, InstrumentNumbers.ZERO_NOTE.targets())) {
            freed += Effects.strip(a, false, InstrumentNumbers.ZERO_CLEANSE).size();
            if (a.getPersistentData().getLong("nusmp_sealed_until") > now) {
                a.getPersistentData().putLong("nusmp_sealed_until", 0L);
                freed++;
            }
        }
        if (freed == 0) {
            c.say("There is nothing to unbind.");
            return false;
        }
        Vec3 foot = p.position();
        c.fx(VfxShape.SONG_RING, foot, foot, WHITE, 30, (float) InstrumentNumbers.ZERO_RADIUS);
        VfxSpawn.sendFollowing(c.level, VfxShape.SONG_NOTES, p, foot.add(0, 1.6, 0), WHITE, 50, 1.0f);
        c.sound(SoundEvents.NOTE_BLOCK_BELL.value(), 1.4f, 1.0f);
        c.say("A silent note frees " + freed + " bond(s).");
        return true;
    }

    // ================================================================== Crescendo
    /** The ultimate: needs harmony three; after a long breath the song swells for six pulses, hurting and slowing foes and mending allies. It spends the whole phrase. */
    static boolean crescendo(HeroCtx c) {
        ServerPlayer p = c.player;
        Tempo t = TEMPO.of(p);
        long now = c.level.getGameTime();
        int h = t.preview(now, c.form.has("steady_beat"));
        if (h < InstrumentNumbers.CRESCENDO_MIN_HARMONY) {
            c.say("Keep the beat: the Crescendo needs Harmony " + InstrumentNumbers.CRESCENDO_MIN_HARMONY + " (it is " + h + ").");
            return false;
        }
        t.reset();
        double radius = radius(c, InstrumentNumbers.CRESCENDO_RADIUS);
        Vec3 foot = p.position();
        Tells.ring(c.level, foot, radius, PINK_RGB);
        c.fx(VfxShape.SONG_RING, foot, foot, PINK, InstrumentNumbers.CRESCENDO_TELL + 8, (float) (radius * 0.5));
        VfxSpawn.sendFollowing(c.level, VfxShape.SONG_NOTES, p, foot.add(0, 1.6, 0), PINK, InstrumentNumbers.CRESCENDO_TELL + 20, 1.6f);
        c.sound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.4f, 0.6f);
        Safe.later(c, InstrumentNumbers.CRESCENDO_TELL, "instrument.crescendo.start", f ->
                Safe.zone(f, (InstrumentNumbers.CRESCENDO_PULSES - 1) * InstrumentNumbers.CRESCENDO_EVERY, InstrumentNumbers.CRESCENDO_EVERY, "instrument.crescendo", true, (g, age) -> {
                    Vec3 here = g.player.position();
                    for (LivingEntity foe : Foes.around(g.player, here, radius, InstrumentNumbers.CRESCENDO.targets())) {
                        g.hit(foe, (float) InstrumentNumbers.CRESCENDO.raw());
                        foe.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0, false, true));
                    }
                    for (LivingEntity ally : Foes.alliesAround(g.player, here, radius, 8)) g.heal(ally, (float) (ally.getMaxHealth() * InstrumentNumbers.CRESCENDO_HEAL_SHARE));
                    g.fx(VfxShape.SONG_RING, here, here, PINK, 24, (float) radius);
                    VfxSpawn.sendFollowing(g.level, VfxShape.SONG_NOTES, g.player, here.add(0, 1.6, 0), PINK, 40, 1.6f);
                    g.sound(SoundEvents.NOTE_BLOCK_HARP.value(), 1.2f, 0.7f + 0.1f * (age / InstrumentNumbers.CRESCENDO_EVERY));
                    return true;
                }));
        return true;
    }
}
