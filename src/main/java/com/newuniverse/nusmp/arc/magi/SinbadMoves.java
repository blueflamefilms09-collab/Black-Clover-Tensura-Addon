package com.newuniverse.nusmp.arc.magi;

import com.newuniverse.nusmp.arc.sds.SdsaStrikes;
import com.newuniverse.nusmp.entity.zogratis.Tells;
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

    /** Command Scream: anyone LOOKING at him when it lands is put to "sleep" for 2 s (blind and rooted); the others only get nausea. The tell says: look away. */
    static void commandScream(SinbadBoss b, ServerLevel sl) {
        Vec3 head = b.getEyePosition();
        for (LivingEntity v : SdsaStrikes.foesInDisc(b, b.position(), 16.0)) {
            b.strike(v, SinbadTable.SCREAM_DAMAGE, true);
            Vec3 toBoss = head.subtract(v.getEyePosition()).normalize();
            if (v.getViewVector(1.0F).dot(toBoss) > 0.5) {
                v.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), b);
                slow(v, b, 40, 9);
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
                    for (LivingEntity v : SdsaStrikes.foesInDisc(b, p, 3.0)) b.strike(v, SinbadTable.SEVEN_SEAS_STEP_DAMAGE, true);
                });
            });
        }
    }
}
