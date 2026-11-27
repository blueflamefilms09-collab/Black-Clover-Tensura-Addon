package com.newuniverse.nusmp.arc.magi;

import com.newuniverse.nusmp.arc.BossFx;
import com.newuniverse.nusmp.arc.BossShapes;
import com.newuniverse.nusmp.arc.sds.SdsaStrikes;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: what each of Sinbad's moves does, server side only, called from {@link SinbadBoss#moves()}. Every hit goes through {@code boss.strike}, so the power tuning, the victim's EP factor,
 * the balance law (no hit over the pvp share of a player's health) and the boss's per-second damage budget all apply, and nothing here is an unavoidable instant kill: effects on
 * players are short (at most 5 s) and each one has a tell. Every effect packet goes through {@code VfxSpawn} (its per-tick budget) and every dust telegraph through {@code Tells}; no particle
 * is sent here directly. Worst case of one move: Vepar Istella, 12 spots (4 per volley, 3 volleys) = 12 foe queries, 12 effect packets; Seven Seas: 7 steps = 7 foe queries, 14 packets.
 */
final class SinbadMoves {
    private static final double SLASH_HALF = Math.toRadians(65.0), SABER_HALF = Math.toRadians(50.0);
    /** Spots per volley of the sword rain, and ticks between volleys. */
    static final int RAIN_PER_VOLLEY = 4, RAIN_GAP = 3;

    private SinbadMoves() {}

    static Vec3 chest(LivingEntity e) { return e.position().add(0.0, e.getBbHeight() * 0.55, 0.0); }

    static Vec3 face(SinbadBoss b, LivingEntity t) {
        return SdsaStrikes.unitFlat(t.position().subtract(b.position()), SdsaStrikes.unitFlat(b.getViewVector(1.0F), new Vec3(0.0, 0.0, 1.0)));
    }

    private static void slow(LivingEntity v, SinbadBoss b, int ticks, int amp) {
        v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amp), b);
    }

    private static void launch(LivingEntity v, Vec3 dir, double horizontal, double up) {
        Vec3 d = SdsaStrikes.unitFlat(dir, new Vec3(0.0, 0.0, 1.0));
        v.push(d.x * horizontal, up, d.z * horizontal);
        v.hurtMarked = true;
    }

    // ------------------------------------------------------------------ telegraph helpers (called from the tell of the wind-up)

    /** A small dust ring on every locked spot (8 points each: a mark, not a full circle, to keep the packet count down). */
    static void tellSpots(ServerLevel sl, List<Vec3> spots, double radius, int rgb) {
        for (Vec3 p : spots) Tells.ring(sl, p, radius, rgb, 1.2f, 8);
    }

    // ------------------------------------------------------------------ base kit (no djinn)

    /** A wide sword arc in front of him. */
    static void swordSlash(SinbadBoss b, ServerLevel sl, Vec3 aim) {
        Vec3 apex = b.position().add(0.0, 1.0, 0.0);
        for (LivingEntity v : SdsaStrikes.foesInCone(b, apex, aim, 6.0, SLASH_HALF)) b.strike(v, SinbadTable.SLASH_DAMAGE, false);
        VfxSpawn.send(sl, VfxShape.SLASH_WAVE, apex, apex.add(aim.scale(5.0)), b.equip().argb, 10, 1.2F);
        SdsaStrikes.sound(sl, apex, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.8F);
    }

    /** Magoi Manipulation: a straight thrust of pure magoi that also strips one beneficial effect (canon: magoi cancels magic). */
    static void magoiThrust(SinbadBoss b, ServerLevel sl, Vec3 aim) {
        Vec3 from = chest(b);
        for (LivingEntity v : SdsaStrikes.foesInStrip(b, from, aim, 10.0, 1.1)) {
            if (b.strike(v, SinbadTable.THRUST_DAMAGE, false)) {
                v.getActiveEffects().stream().filter(e -> e.getEffect().value().isBeneficial()).findFirst().ifPresent(e -> v.removeEffect(e.getEffect()));
            }
        }
        VfxSpawn.send(sl, VfxShape.LIGHT_BLADE, from, from.add(aim.scale(10.0)), 0xFFE8D9A8, 10, 1.0F);
    }

    // ------------------------------------------------------------------ Baal: lightning

    /** Bararaq: five marked rings, then lightning on each. */
    static void bararaqMark(SinbadBoss b, LivingEntity t) {
        b.lockSpots(SinbadTable.BARARAQ, SdsaStrikes.spread(b, t, 5, 6.0));
    }

    static void bararaqFire(SinbadBoss b, ServerLevel sl, LivingEntity t) {
        for (Vec3 p : b.takeSpots(SinbadTable.BARARAQ, t)) {
            VfxSpawn.send(sl, VfxShape.STORM_BOLT, p.add(0.0, 18.0, 0.0), p, SinbadDjinn.BAAL.argb, 12, 1.4F);
            for (LivingEntity v : SdsaStrikes.foesInDisc(b, p, 2.5)) b.strike(v, SinbadTable.BOLT_DAMAGE, true);
        }
        SdsaStrikes.sound(sl, t.position(), SoundEvents.LIGHTNING_BOLT_THUNDER, 2.0F, 1.0F);
    }

    /** Bararaq Saiqa: lightning drawn into the sword, released as one long blast (28 blocks, one raycast, one foe query). */
    static void bararaqSaiqa(SinbadBoss b, ServerLevel sl, Vec3 aim) {
        Vec3 from = chest(b);
        Vec3 end = SdsaStrikes.clipEnd(sl, b, from, from.add(aim.scale(28.0)));
        for (LivingEntity v : SdsaStrikes.foesInStrip(b, from, aim, from.distanceTo(end), 1.6)) b.strike(v, SinbadTable.SAIQA_DAMAGE, true);
        VfxSpawn.send(sl, VfxShape.LIGHTNING_SPEAR, from, end, SinbadDjinn.BAAL.argb, 16, 2.0F);
        SinbadFx.rukh(sl, from, end, SinbadDjinn.BAAL, 1.2F);
        SdsaStrikes.sound(sl, from, SoundEvents.LIGHTNING_BOLT_IMPACT, 2.5F, 0.9F);
    }

    /** Bararaq Inqerad-Saiqa (Baal's Extreme Magic): three golden safe circles are marked; everyone inside the 22-block field who is not standing in one is struck. */
    static void inqeradMark(SinbadBoss b, LivingEntity t) {
        b.lockSpots(SinbadTable.INQERAD_SAIQA, SdsaStrikes.spread(b, t, 3, 12.0));
        b.announce("The sky over Sinbad turns white with lightning. Stand in a golden circle!");
    }

    static void inqeradFire(SinbadBoss b, ServerLevel sl, LivingEntity t) {
        List<Vec3> safe = b.takeSpots(SinbadTable.INQERAD_SAIQA, t);
        VfxSpawn.send(sl, VfxShape.LIGHTNING_GOD, b.position().add(0.0, 20.0, 0.0), b.position(), SinbadDjinn.BAAL.argb, 30, 4.0F);
        SinbadFx.extreme(sl, b.position(), SinbadDjinn.BAAL, 14.0F);
        for (LivingEntity v : SdsaStrikes.foesInDisc(b, b.position(), 22.0)) {
            boolean covered = false;
            for (Vec3 p : safe) {
                if (v.position().distanceToSqr(p) <= 9.0) {
                    covered = true;
                    break;
                }
            }
            if (!covered) b.strike(v, SinbadTable.INQERAD_DAMAGE, true);
        }
        SdsaStrikes.sound(sl, b.position(), SoundEvents.LIGHTNING_BOLT_THUNDER, 4.0F, 0.6F);
    }

    // ------------------------------------------------------------------ Valefor: ice

    /** Garufor Zairu: a volley of seven ice picks that slow. */
    static void garuforZairu(SinbadBoss b, ServerLevel sl, LivingEntity t) {
        Vec3 from = chest(b);
        for (Vec3 p : SdsaStrikes.spread(b, t, 7, 2.5)) {
            VfxSpawn.send(sl, VfxShape.ICE_WEDGE_FX1, from, p, SinbadDjinn.VALEFOR.argb, 10, 0.8F);
            for (LivingEntity v : SdsaStrikes.foesInDisc(b, p, 1.5)) if (b.strike(v, SinbadTable.ICE_PICK_DAMAGE, true)) slow(v, b, 40, 1);
        }
        SdsaStrikes.sound(sl, from, SoundEvents.GLASS_BREAK, 1.2F, 1.4F);
    }

    /** Garufor Kirestal: the target is frozen inside ice for 2.5 s (players) or 4 s (other targets). */
    static void garuforKirestal(SinbadBoss b, ServerLevel sl, LivingEntity t) {
        if (b.strike(t, SinbadTable.KIRESTAL_DAMAGE, true)) {
            int ticks = t instanceof Player ? 50 : 80;
            slow(t, b, ticks, 9);
            t.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ticks, 4), b);
            VfxSpawn.send(sl, VfxShape.ICE_FX3, t.position(), t.position().add(0.0, 2.0, 0.0), SinbadDjinn.VALEFOR.argb, ticks, 1.2F);
        }
        SdsaStrikes.sound(sl, t.position(), SoundEvents.GLASS_BREAK, 1.5F, 0.6F);
    }

    /** Stagnation: molecules stop; everyone within 9 blocks slows, thoughts included (mining fatigue), for 5 s. */
    static void stagnation(SinbadBoss b, ServerLevel sl) {
        for (LivingEntity v : SdsaStrikes.foesInDisc(b, b.position(), 9.0)) {
            slow(v, b, 100, 2);
            v.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 2), b);
        }
        VfxSpawn.sendFollowing(sl, VfxShape.SNOW_FX2, b, b.position(), SinbadDjinn.VALEFOR.argb, 100, 3.0F);
    }

    // ------------------------------------------------------------------ Zepar: sound

    /** Command Scream: anyone LOOKING at him when it lands is put to "sleep" for 2 s (blind and slowed, never fully rooted); the others only get nausea. The tell says: look away. */
    static void commandScream(SinbadBoss b, ServerLevel sl) {
        Vec3 head = b.getEyePosition();
        for (LivingEntity v : SdsaStrikes.foesInDisc(b, b.position(), 16.0)) {
            b.strike(v, SinbadTable.SCREAM_DAMAGE, true);
            Vec3 toBoss = head.subtract(v.getEyePosition()).normalize();
            if (v.getViewVector(1.0F).dot(toBoss) > 0.5) {
                v.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), b);
                slow(v, b, 40, SinbadBossConfig.screamSlowAmp());
            } else {
                v.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0), b);
            }
        }
        VfxSpawn.send(sl, VfxShape.SOUND_FX3, head, head, SinbadDjinn.ZEPAR.argb, 20, 3.0F);
        SdsaStrikes.sound(sl, head, SoundEvents.WARDEN_SONIC_BOOM, 2.0F, 1.6F);
    }

    /** Rukh Intrusion: sound pushes his Rukh into one target (weakness and nausea). */
    static void rukhIntrusion(SinbadBoss b, ServerLevel sl, LivingEntity t) {
        if (b.strike(t, SinbadTable.INTRUSION_DAMAGE, true)) {
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1), b);
            t.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0), b);
        }
        VfxSpawn.send(sl, VfxShape.SOUND_FX1, chest(b), chest(t), SinbadDjinn.ZEPAR.argb, 14, 1.0F);
    }

    // ------------------------------------------------------------------ Focalor: wind

    /** Foraz Zora: two tornadoes join over his head and are thrown onto the marked spot. */
    static void forazZoraMark(SinbadBoss b, ServerLevel sl, LivingEntity t) {
        b.lockSpots(SinbadTable.FORAZ_ZORA, List.of(t.position()));
        VfxSpawn.sendFollowing(sl, VfxShape.WIND_TORNADO, b, b.position().add(0.0, 4.0, 0.0), SinbadDjinn.FOCALOR.argb, 40, 1.5F);
    }

    static void forazZoraFire(SinbadBoss b, ServerLevel sl, LivingEntity t) {
        for (Vec3 p : b.takeSpots(SinbadTable.FORAZ_ZORA, t)) {
            VfxSpawn.send(sl, VfxShape.WIND_TORNADO, p, p.add(0.0, 10.0, 0.0), SinbadDjinn.FOCALOR.argb, 40, 3.0F);
            for (LivingEntity v : SdsaStrikes.foesInDisc(b, p, 5.0)) if (b.strike(v, SinbadTable.TORNADO_DAMAGE, true)) launch(v, v.position().subtract(p), 0.6, 1.0);
            SdsaStrikes.sound(sl, p, SoundEvents.ELYTRA_FLYING, 2.0F, 0.6F);
        }
    }

    /** Gale Step: flies behind the target on the wind (only if his body fits there) and blasts a knockback ring. */
    static void galeStep(SinbadBoss b, ServerLevel sl, LivingEntity t) {
        b.blinkTo(t.position().add(face(b, t).scale(2.5)));
        for (LivingEntity v : SdsaStrikes.foesInDisc(b, b.position(), 5.0)) if (b.strike(v, SinbadTable.GALE_DAMAGE, true)) launch(v, v.position().subtract(b.position()), 1.0, 0.4);
        VfxSpawn.send(sl, VfxShape.WIND_RING, b.position(), b.position().add(0.0, 1.0, 0.0), SinbadDjinn.FOCALOR.argb, 12, 2.0F);
    }

    // ------------------------------------------------------------------ Vepar: magnetism / swords

    /** Vepar Istella ("Thousand Sword Shower"): twelve marked spots. */
    static void veparIstellaMark(SinbadBoss b, LivingEntity t) {
        b.lockSpots(SinbadTable.VEPAR_ISTELLA, SdsaStrikes.spread(b, t, SinbadTable.MAX_SPOTS, 7.0));
    }

    /** The swords fall in volleys of {@link #RAIN_PER_VOLLEY} spots every {@link #RAIN_GAP} ticks, so one tick never handles all twelve. */
    static void veparIstellaFire(SinbadBoss b, ServerLevel sl, LivingEntity t) {
        List<Vec3> spots = b.takeSpots(SinbadTable.VEPAR_ISTELLA, t);
        int n = Math.min(spots.size(), SinbadTable.MAX_SPOTS);
        for (int from = 0; from < n; from += RAIN_PER_VOLLEY) {
            final int lo = from, hi = Math.min(n, from + RAIN_PER_VOLLEY);
            b.later((from / RAIN_PER_VOLLEY) * RAIN_GAP, () -> {
                for (int i = lo; i < hi; i++) {
                    Vec3 p = spots.get(i);
                    VfxSpawn.send(sl, VfxShape.KOTO_SWORDS, p.add(0.0, 14.0, 0.0), p, SinbadDjinn.VEPAR.argb, 12, 1.0F);
                    for (LivingEntity v : SdsaStrikes.foesInDisc(b, p, 1.6)) b.strike(v, SinbadTable.SWORD_RAIN_DAMAGE, true);
                }
                SdsaStrikes.sound(sl, spots.get(lo), SoundEvents.ANVIL_LAND, 1.0F, 1.6F);
            });
        }
    }

    // ------------------------------------------------------------------ Crocell: saber (invented)

    /** Crocell's equip fights with a saber: three cuts, ten ticks apart (so the victim's hurt frames do not swallow them). */
    static void saberFlurry(SinbadBoss b, ServerLevel sl, LivingEntity t) {
        for (int i = 0; i < 3; i++) {
            b.later(i * 10, () -> {
                if (!t.isAlive()) return;
                Vec3 aim = face(b, t);
                Vec3 apex = b.position().add(0.0, 1.0, 0.0);
                for (LivingEntity v : SdsaStrikes.foesInCone(b, apex, aim, 6.5, SABER_HALF)) b.strike(v, SinbadTable.SABER_DAMAGE, false);
                VfxSpawn.send(sl, VfxShape.SLASH_FX2, apex, apex.add(aim.scale(5.0)), SinbadDjinn.CROCELL.argb, 8, 1.0F);
                SdsaStrikes.sound(sl, apex, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 1.3F);
            });
        }
    }

    // ------------------------------------------------------------------ Furfur: darkness (invented)

    /** Rending Darkness: a tearing dark slash that leaves Darkness for 5 s. */
    static void rendingDarkness(SinbadBoss b, ServerLevel sl, Vec3 aim) {
        Vec3 from = chest(b);
        for (LivingEntity v : SdsaStrikes.foesInStrip(b, from, aim, 22.0, 1.4)) {
            if (b.strike(v, SinbadTable.DARKNESS_DAMAGE, true)) v.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0), b);
        }
        VfxSpawn.send(sl, VfxShape.DARK_SLASH_DIMENSION, from, from.add(aim.scale(22.0)), SinbadDjinn.FURFUR.argb, 16, 1.5F);
    }

    // ------------------------------------------------------------------ the final phase (invented)

    /**
     * Seven Seas Sovereign (Gates only): he runs through all seven djinn in seven seconds, one marked strike each (a ring in the djinn's colour, then the bolt half a second later). He is
     * exposed for the whole sequence (the move's recovery is longer than it). Each step re-checks that the target is alive; every delayed step is dropped when he himself is gone.
     */
    static void sevenSeas(SinbadBoss b, ServerLevel sl, LivingEntity t) {
        SinbadDjinn[] order = {SinbadDjinn.BAAL, SinbadDjinn.FOCALOR, SinbadDjinn.VALEFOR, SinbadDjinn.ZEPAR, SinbadDjinn.VEPAR, SinbadDjinn.CROCELL, SinbadDjinn.FURFUR};
        for (int i = 0; i < order.length; i++) {
            SinbadDjinn d = order[i];
            b.later(i * 20, () -> {
                if (!t.isAlive()) return;
                b.setEquip(sl, d, false);
                Vec3 p = t.position();
                Tells.ring(sl, p, 3.0, d.rgb(), 1.3f, 16);
                b.later(10, () -> {
                    VfxSpawn.send(sl, VfxShape.STORM_BOLT, p.add(0.0, 10.0, 0.0), p, d.argb, 10, 1.5F);
                    SinbadFx.rukh(sl, b.position().add(0.0, 2.0, 0.0), p, d, 1.0F);
                    if (d == SinbadDjinn.FURFUR) SinbadFx.extreme(sl, p, d, 16.0F);
                    for (LivingEntity v : SdsaStrikes.foesInDisc(b, p, 3.0)) b.strike(v, SinbadTable.SEVEN_SEAS_STEP_DAMAGE, true);
                });
            });
        }
    }

    // ------------------------------------------------------------------ made for this boss: the Rukh Orrery and the Kingdom Sigil (docs/boss_redesign_goku_sinbad.md)

    /** The seven djinn in the order of the orrery's motes (the order of the rainbow he wears). */
    static final SinbadDjinn[] SEVEN = {SinbadDjinn.BAAL, SinbadDjinn.VALEFOR, SinbadDjinn.ZEPAR, SinbadDjinn.FOCALOR, SinbadDjinn.VEPAR, SinbadDjinn.CROCELL, SinbadDjinn.FURFUR};
    static final int ORRERY_SHOTS = 7, ORRERY_GAP = 4;
    static final double SIGIL_RADIUS = 11.0;
    static final int SIGIL_SECTORS = 6;

    /** The element's own spark for a djinn (one packet of at most six particles). */
    static FxOld sparkOf(SinbadDjinn d) {
        return switch (d) {
            case BAAL -> FxOld.ELECTRIC_SPARK;
            case VALEFOR -> FxOld.SNOWFLAKE;
            case ZEPAR -> FxOld.ENCHANT;
            case FOCALOR -> FxOld.CLOUD;
            case VEPAR -> FxOld.CRIT;
            case CROCELL -> FxOld.END_ROD;
            case FURFUR -> FxOld.SCULK_SOUL;
            case NONE -> FxOld.END_ROD;
        };
    }

    /** What a hit of the orrery or the sigil adds, by djinn: short and mild, each with its own element's feel (nothing lasts over 2 s). */
    static void rider(SinbadBoss b, LivingEntity v, SinbadDjinn d) {
        switch (d) {
            case VALEFOR -> slow(v, b, 30, 1);
            case ZEPAR -> v.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 40, 0), b);
            case FOCALOR -> launch(v, v.position().subtract(b.position()), 0.3, 0.55);
            case VEPAR -> v.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0), b);
            case FURFUR -> v.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0), b);
            case BAAL -> slow(v, b, 12, 0);
            default -> { }
        }
    }

    /** {@code n} points of a straight line from {@code a} to {@code b}, relative to {@code a} (the caller draws it from {@code a}). */
    static double[] lineRel(Vec3 a, Vec3 b, int n) {
        n = Math.max(1, Math.min(BossShapes.MAX_POINTS, n));
        double[] out = new double[n * 3];
        for (int i = 0; i < n; i++) {
            double k = (i + 1) / (double) (n + 1);
            out[i * 3] = (b.x - a.x) * k;
            out[i * 3 + 1] = (b.y - a.y) * k;
            out[i * 3 + 2] = (b.z - a.z) * k;
        }
        return out;
    }

    private static int[] sevenRgb() {
        int[] out = new int[SEVEN.length];
        for (int i = 0; i < out.length; i++) out[i] = SEVEN[i].rgb();
        return out;
    }

    // ---- Rukh Orrery: seven motes, one per djinn, orbit him; then each flies to its own marked spot, one every four ticks, in the rainbow's order.

    static void orreryMark(SinbadBoss b, LivingEntity t) {
        b.lockSpots(SinbadTable.RUKH_ORRERY, SdsaStrikes.spread(b, t, ORRERY_SHOTS, 3.2));
    }

    static void orreryTell(SinbadBoss b, ServerLevel sl, List<Vec3> spots, int left, int total) {
        double spin = (total - left) * 0.17 * (1.0 + (total - left) / (double) Math.max(1, total));
        double[] rel = new double[ORRERY_SHOTS * 3];
        for (int i = 0; i < ORRERY_SHOTS; i++) System.arraycopy(BossShapes.orbit(i, ORRERY_SHOTS, 2.4, 2.3, spin, 0.25), 0, rel, i * 3, 3);
        BossFx.pointsMulti(sl, b.position(), rel, sevenRgb(), 1.7f);
        BossFx.points(sl, b.position().add(0, 2.3, 0), BossShapes.ring(2.4, 0.0, 14, -spin * 0.5), 0xE8D9A8, 0.8f);
        for (int i = 0; i < spots.size() && i < ORRERY_SHOTS; i++) Tells.ring(sl, spots.get(i), 1.5, SEVEN[i].rgb(), 1.2f, 8);
    }

    static void orreryFire(SinbadBoss b, ServerLevel sl, LivingEntity t) {
        List<Vec3> spots = b.takeSpots(SinbadTable.RUKH_ORRERY, t);
        int n = Math.min(spots.size(), ORRERY_SHOTS);
        for (int i = 0; i < n; i++) {
            final int k = i;
            b.later(i * ORRERY_GAP, () -> orreryShot(b, sl, k, spots.get(k)));
        }
    }

    /** One mote lands: its trail, a ring and a column in the djinn's colour, the element's spark, and a hit with the djinn's rider. Returns the foes hit. */
    static int orreryShot(SinbadBoss b, ServerLevel sl, int i, Vec3 p) {
        SinbadDjinn d = SEVEN[Math.floorMod(i, SEVEN.length)];
        Vec3 from = b.position().add(0.0, 2.3, 0.0);
        BossFx.points(sl, from, lineRel(from, p.add(0.0, 0.4, 0.0), 10), d.rgb(), 1.2f);
        BossFx.points(sl, p.add(0.0, 0.1, 0.0), BossShapes.ring(1.7, 0.0, 12, 0.0), d.rgb(), 1.5f);
        BossFx.column(sl, p, 3.2, 8, d.rgb(), 1.3f);
        BossFx.spark(sl, sparkOf(d), p.add(0.0, 0.6, 0.0), 5, 0.5, 0.2);
        int hits = 0;
        for (LivingEntity v : SdsaStrikes.foesInDisc(b, p, 1.8)) {
            if (b.strike(v, SinbadTable.ORRERY_DAMAGE, true)) {
                hits++;
                rider(b, v, d);
            }
        }
        SdsaStrikes.sound(sl, p, SoundEvents.BEACON_ACTIVATE, 0.9F, 0.8F + 0.12F * i);
        return hits;
    }

    // ---- Kingdom Sigil: a six-sector circle draws itself round him (rim, then spokes, then the lit sectors); only the lit sectors erupt, the others are safe.

    static void sigilMark(SinbadBoss b, LivingEntity t) {
        Vec3 aim = face(b, t);
        double phase = Math.atan2(aim.z, aim.x);
        b.lockSpots(SinbadTable.KINGDOM_SIGIL, List.of(b.position(), new Vec3(b.getRandom().nextInt(2), phase, 0.0)));
    }

    static void sigilTell(SinbadBoss b, ServerLevel sl, List<Vec3> spots, int left, int total) {
        if (spots.size() < 2) return;
        Vec3 c = spots.get(0);
        int pattern = (int) spots.get(1).x;
        double phase = spots.get(1).y, k = (total - left) / (double) Math.max(1, total);
        int rgb = b.equip().rgb();
        BossFx.points(sl, c.add(0, 0.1, 0), BossShapes.ring(SIGIL_RADIUS, 0.0, 32, 0.0), rgb, 1.3f);
        if (k > 0.33) {
            double[] rel = new double[30 * 3];
            for (int s = 0; s < SIGIL_SECTORS; s++) {
                double a = phase + s * Math.PI * 2.0 / SIGIL_SECTORS;
                for (int j = 0; j < 5; j++) {
                    double r = SIGIL_RADIUS * (j + 1) / 6.0;
                    rel[(s * 5 + j) * 3] = Math.cos(a) * r;
                    rel[(s * 5 + j) * 3 + 1] = 0.1;
                    rel[(s * 5 + j) * 3 + 2] = Math.sin(a) * r;
                }
            }
            BossFx.points(sl, c, rel, 0xE8D9A8, 1.0f);
        }
        if (k > 0.66) {
            for (int s = 0; s < SIGIL_SECTORS; s++) {
                if (!BossShapes.lit(s, pattern)) continue;
                BossFx.points(sl, c.add(0, 0.12, 0), BossShapes.sectorFill(SIGIL_RADIUS, SIGIL_SECTORS, s, phase, 10, 0.0), 0xFFD54F, 1.5f);
                double[] mid = BossShapes.sectorMid(SIGIL_RADIUS * 0.75, SIGIL_SECTORS, s, phase);
                BossFx.column(sl, c.add(mid[0], 0.05, mid[2]), 3.0, 6, rgb, 1.4f);
            }
        }
    }

    /** The foes standing in a lit sector of the sigil (the pure rule {@link BossShapes#sigilHit}). */
    static List<LivingEntity> sigilFoes(SinbadBoss b, Vec3 c, double phase, int pattern) {
        List<LivingEntity> out = new java.util.ArrayList<>();
        for (LivingEntity v : SdsaStrikes.foesInDisc(b, c, SIGIL_RADIUS)) {
            if (BossShapes.sigilHit(v.getX() - c.x, v.getZ() - c.z, SIGIL_RADIUS, SIGIL_SECTORS, phase, pattern)) out.add(v);
        }
        return out;
    }

    static void sigilFire(SinbadBoss b, ServerLevel sl, LivingEntity t) {
        List<Vec3> spots = b.takeSpots(SinbadTable.KINGDOM_SIGIL, t);
        if (spots.size() < 2) return;
        sigilLand(b, sl, spots.get(0), spots.get(1).y, (int) spots.get(1).x);
    }

    /** The hit check is done at once; the lit sectors then erupt clockwise, one every three ticks (so no tick draws more than one sector). Returns the foes hit. */
    static int sigilLand(SinbadBoss b, ServerLevel sl, Vec3 c, double phase, int pattern) {
        SinbadDjinn d = b.equip();
        int hits = 0;
        for (LivingEntity v : sigilFoes(b, c, phase, pattern)) {
            if (b.strike(v, SinbadTable.SIGIL_DAMAGE, true)) {
                hits++;
                rider(b, v, d);
            }
        }
        int order = 0;
        for (int s = 0; s < SIGIL_SECTORS; s++) {
            if (!BossShapes.lit(s, pattern)) continue;
            final int sec = s;
            b.later(order++ * 3, () -> {
                double[] mid = BossShapes.sectorMid(SIGIL_RADIUS * 0.7, SIGIL_SECTORS, sec, phase);
                BossFx.points(sl, c.add(0, 0.15, 0), BossShapes.sectorFill(SIGIL_RADIUS, SIGIL_SECTORS, sec, phase, 24, 0.0), 0xFFFFFF, 1.7f);
                BossFx.column(sl, c.add(mid[0], 0.05, mid[2]), 6.0, 12, d.rgb(), 1.7f);
                BossFx.spark(sl, sparkOf(d), c.add(mid[0], 1.0, mid[2]), 6, 1.0, 0.3);
            });
        }
        SdsaStrikes.sound(sl, c, SoundEvents.BEACON_ACTIVATE, 2.0F, 1.4F);
        return hits;
    }
}
