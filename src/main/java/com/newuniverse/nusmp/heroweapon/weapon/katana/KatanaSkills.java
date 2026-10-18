package com.newuniverse.nusmp.heroweapon.weapon.katana;

import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.skill.SimpleSkill;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.Budget;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.Geo;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.GlassKit;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the nine techniques of the Vassal Katana, one small method each, so the whole weapon reads as a list: a long thin slash (Crescent Moon Sword), the draw
 * cut of iaido (Instant Blade: Mist), the parry (Counter Stance), the dodge (Release Evade), three thrusts, a flurry on one foe (Stardust Blade), a phantom
 * second blade (Double Sword), a cut that drains magicules (Soul Slice) and the dance of sakura. Every technique but the stances is cast "in tempo": the
 * wielder's {@code TempoChain} counts how many different techniques have flowed into each other and the cuts, thrusts and the dance grow with it. Every number
 * comes from {@link KatanaNumbers}, every word from {@link KatanaText}; damage and costs go through {@code HeroCtx} (the balance law), effects through the
 * mod's own effect system, and nothing changes a block or spawns an entity. A technique with nothing to act on refuses (returns false) so it costs nothing, and
 * every technique above 8 raw damage shows a tell first.
 */
final class KatanaSkills {
    private KatanaSkills() {}

    // the palette: sakura pink, pale steel white
    private static final int PINK = 0xFFFFB8D6, WHITE = 0xFFF4F7FF, STEEL = 0xFFDCE6F5;
    private static final int PINK_RGB = 0xFFB8D6;

    static List<HeroSkill> all() {
        List<HeroSkill> l = new ArrayList<>();
        l.add(crescentMoon());
        l.add(instantBlade());
        l.add(counterStance());
        l.add(releaseEvade());
        l.add(tripleThrust());
        l.add(stardustBlade());
        l.add(doubleSword());
        l.add(soulSlice());
        l.add(sakuraDance());
        return List.copyOf(l);
    }

    private static SimpleSkill make(String id, SimpleSkill.Body body) {
        Budget.Spec s = KatanaNumbers.spec(id);
        return SimpleSkill.of(id, KatanaText.skillName(id), KatanaText.skillDesc(id), s.cooldown(), s.cost(), body);
    }

    /** The wielder's session (tempo, windows). */
    static KatanaSession session(HeroCtx c) { return KatanaState.SESSIONS.of(c.player.getUUID()); }

    /** Counts a technique into the tempo chain and tells the wielder; returns the tempo it is cast with. */
    private static int flow(HeroCtx c, String key) {
        KatanaSession s = session(c);
        long now = c.level.getGameTime();
        int tempo = s.chain().record(key, now);
        s.acted(now);
        if (tempo > 0) c.say("Tempo " + tempo + "/" + KatanaNumbers.TEMPO_MAX);
        return tempo;
    }

    private static Vec3 flat(Vec3 v) {
        Vec3 f = new Vec3(v.x, 0.0, v.z);
        return f.lengthSqr() < 1.0e-6 ? new Vec3(0.0, 0.0, 1.0) : f.normalize();
    }

    // ------------------------------------------------------------------ Crescent Moon Sword
    private static HeroSkill crescentMoon() {
        return make(KatanaNumbers.CRESCENT_MOON, c -> {
            Vec3 look = c.look();
            Vec3 start = c.eye().add(look.scale(0.5));
            Vec3 end = GlassKit.clipEnd(c, start, start.add(look.scale(KatanaNumbers.CRESCENT_RANGE)));
            List<LivingEntity> foes = GlassKit.foesAlong(c, start, end, KatanaNumbers.CRESCENT_WIDTH, KatanaNumbers.CRESCENT_TARGETS);
            if (foes.isEmpty()) {
                c.say("Nothing in the line of the cut.");
                return false;
            }
            double raw = KatanaNumbers.crescentRaw(flow(c, KatanaNumbers.CRESCENT_MOON));
            for (LivingEntity e : foes) GlassKit.strike(c, e, raw);
            c.fx(VfxShape.SLASH_WAVE, start, end, PINK, 12, 1.2f);
            c.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);
            GlassKit.swing(c);
            return true;
        });
    }

    // ------------------------------------------------------------------ Instant Blade: Mist
    private static HeroSkill instantBlade() {
        return make(KatanaNumbers.INSTANT_BLADE, c -> {
            int tempo = flow(c, KatanaNumbers.INSTANT_BLADE);
            Vec3 hip = c.player.position().add(0.0, 0.9, 0.0).add(flat(c.look()).scale(0.5));
            // the tell: the sword is sheathed, a glint at the hip, the click of the guard; then the cut
            c.fx(VfxShape.STAR_MAGIC, hip, hip.add(flat(c.look()).scale(0.8)), WHITE, KatanaNumbers.INSTANT_DELAY + 3, 0.8f);
            c.sound(SoundEvents.CHAIN_HIT, 1.0f, 0.6f);
            GlassKit.later(c, KatanaNumbers.INSTANT_DELAY, () -> {
                Vec3 dir = flat(c.look());
                Vec3 start = GlassKit.chest(c.player);
                double wall = GlassKit.reach(c, start, dir, KatanaNumbers.INSTANT_DASH + KatanaNumbers.INSTANT_MARGIN);
                double dist = Geo.stopShort(KatanaNumbers.INSTANT_DASH, wall, KatanaNumbers.INSTANT_MARGIN);
                Vec3 end = start.add(dir.scale(dist));
                double raw = KatanaNumbers.instantRaw(tempo);
                for (LivingEntity e : GlassKit.foesAlong(c, start, end, KatanaNumbers.INSTANT_WIDTH, KatanaNumbers.INSTANT_TARGETS)) GlassKit.strike(c, e, raw);
                if (dist > 0.3) {
                    Geo.Impulse v = Geo.dash(dir.x, 0.0, dir.z, Geo.dashSpeedFor(dist), 0.3, 0.2, KatanaNumbers.INSTANT_LIFT, c.player.onGround());
                    GlassKit.impulse(c.player, new Vec3(v.x(), v.y(), v.z()), false);
                }
                c.fx(VfxShape.DARK_IAI, start, end, WHITE, 12, 1.1f);
                c.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 1.9f);
                GlassKit.swing(c);
            });
            return true;
        });
    }

    // ------------------------------------------------------------------ Counter Stance
    private static HeroSkill counterStance() {
        return make(KatanaNumbers.COUNTER_STANCE, c -> {
            KatanaSession s = session(c);
            flow(c, KatanaNumbers.COUNTER_STANCE);
            s.openCounter(c.level.getGameTime(), KatanaNumbers.PARRY_TICKS);
            c.fx(VfxShape.STAR_MAGIC, GlassKit.chest(c.player), GlassKit.chest(c.player).add(c.look().scale(0.9)), WHITE, KatanaNumbers.PARRY_TICKS, 0.9f);
            c.sound(SoundEvents.CHAIN_HIT, 1.0f, 0.5f);
            c.say("Counter stance");
            return true;
        });
    }

    // ------------------------------------------------------------------ Release Evade
    private static HeroSkill releaseEvade() {
        return make(KatanaNumbers.RELEASE_EVADE, c -> {
            Vec3 feet = c.player.position(), look = c.look();
            List<LivingEntity> near = GlassKit.foes(c, feet, 8.0, 1);
            double[] away = near.isEmpty() ? Geo.awayFrom(0.0, 0.0, look.x, look.z, -look.x, -look.z)
                    : Geo.awayFrom(feet.x, feet.z, near.get(0).getX(), near.get(0).getZ(), -look.x, -look.z);
            flow(c, KatanaNumbers.RELEASE_EVADE);
            session(c).openEvade(c.level.getGameTime(), KatanaNumbers.EVADE_TICKS);
            GlassKit.impulse(c.player, new Vec3(away[0] * KatanaNumbers.EVADE_SPEED, c.player.onGround() ? KatanaNumbers.EVADE_LIFT : 0.0, away[1] * KatanaNumbers.EVADE_SPEED), false);
            c.fx(VfxShape.WIND_RING, feet, feet.add(0.0, 1.0, 0.0), STEEL, 14, 0.8f);
            c.fx(VfxShape.STAR_MAGIC, feet.add(0.0, 1.0, 0.0), feet.add(away[0] * 1.5, 1.0, away[1] * 1.5), WHITE, 12, 0.7f);
            c.sound(SoundEvents.PHANTOM_FLAP, 0.8f, 1.8f);
            return true;
        });
    }

    // ------------------------------------------------------------------ Triple Thrust
    private static HeroSkill tripleThrust() {
        return make(KatanaNumbers.TRIPLE_THRUST, c -> {
            if (GlassKit.foesInCone(c, KatanaNumbers.THRUST_LENGTH + 2.0, 50.0, 1).isEmpty()) {
                c.say("There is nobody ahead to thrust at.");
                return false;
            }
            int tempo = flow(c, KatanaNumbers.TRIPLE_THRUST);
            double raw = KatanaNumbers.thrustRaw(tempo);
            // the tell: the blade is drawn back and a glint gathers on its tip
            Vec3 tip = c.eye().subtract(0.0, 0.4, 0.0).add(c.look().scale(0.9));
            c.fx(VfxShape.STAR_MAGIC, tip, tip.add(c.look().scale(0.7)), WHITE, KatanaNumbers.THRUST_DELAY + 2, 0.9f);
            c.sound(SoundEvents.GRINDSTONE_USE, 0.7f, 1.6f);
            GlassKit.swing(c);
            GlassKit.later(c, KatanaNumbers.THRUST_DELAY, () -> GlassKit.pulses(c, KatanaNumbers.THRUSTS, KatanaNumbers.THRUST_INTERVAL, i -> {
                Vec3 from = c.eye().subtract(0.0, 0.4, 0.0), dir = c.look();
                Vec3 end = GlassKit.clipEnd(c, from, from.add(dir.scale(KatanaNumbers.THRUST_LENGTH)));
                for (LivingEntity e : GlassKit.foesAlong(c, from, end, KatanaNumbers.THRUST_WIDTH, KatanaNumbers.THRUST_TARGETS)) GlassKit.strike(c, e, raw, true);       // a quick series: every thrust lands on a mob
                Vec3 step = flat(dir).scale(KatanaNumbers.THRUST_STEP);
                GlassKit.impulse(c.player, new Vec3(step.x, 0.0, step.z), true);
                c.fx(VfxShape.LIGHT_BLADE, from, end, WHITE, 6, 0.8f);
                c.sound(SoundEvents.PLAYER_ATTACK_STRONG, 0.7f, 1.4f + 0.1f * i);
                GlassKit.swing(c);
            }));
            return true;
        });
    }

    // ------------------------------------------------------------------ Stardust Blade
    private static HeroSkill stardustBlade() {
        return make(KatanaNumbers.STARDUST_BLADE, c -> {
            LivingEntity target = GlassKit.focus(c, KatanaNumbers.STARDUST_RANGE);
            if (target == null) {
                c.say("No foe on your crosshair.");
                return false;
            }
            flow(c, KatanaNumbers.STARDUST_BLADE);
            GlassKit.pulses(c, KatanaNumbers.STARDUST_CUTS, KatanaNumbers.STARDUST_INTERVAL, i -> {
                if (!target.isAlive() || target.distanceTo(c.player) > KatanaNumbers.STARDUST_RANGE + 3.0) return;
                GlassKit.strike(c, target, KatanaNumbers.STARDUST_RAW, true);                       // a quick series: every cut lands on a mob
                Vec3 centre = target.getBoundingBox().getCenter();
                double spread = target.getBbWidth() * 0.4;
                Vec3 spot = centre.add(c.level.random.nextGaussian() * spread, c.level.random.nextGaussian() * spread * 0.5, c.level.random.nextGaussian() * spread);
                c.fx(VfxShape.STAR_MAGIC, spot.add(0.0, 0.8, 0.0), spot, WHITE, 10, 0.7f);
                c.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.7f, 1.0f + 0.15f * i);
                GlassKit.swing(c);
            });
            return true;
        });
    }

    // ------------------------------------------------------------------ Double Sword
    private static HeroSkill doubleSword() {
        return make(KatanaNumbers.DOUBLE_SWORD, c -> {
            flow(c, KatanaNumbers.DOUBLE_SWORD);
            session(c).openDouble(c.level.getGameTime(), KatanaNumbers.DOUBLE_TICKS, KatanaNumbers.ECHO_MAX);
            GlassKit.follow(c, VfxShape.SPIRIT_AURA, PINK, KatanaNumbers.DOUBLE_TICKS, 0.9f);
            c.sound(SoundEvents.EVOKER_PREPARE_ATTACK, 0.7f, 1.8f);
            c.say("A phantom blade follows yours.");
            return true;
        });
    }

    // ------------------------------------------------------------------ Soul Slice
    /** Gives the wielder back part of the magicules a cut took, never more than a small share of their own pool. */
    private static void takeBack(HeroCtx c, double taken) {
        var ex = TensuraStorages.getExistenceFrom(c.player);
        if (ex == null || taken <= 0.0) return;
        double max = EnergyHelper.getMaxMagicule(c.player);
        double add = Math.min(taken * KatanaNumbers.SLICE_TAKE_BACK, max * KatanaNumbers.SLICE_GAIN_CAP);
        ex.setMagicule(Math.min(max, ex.getMagicule() + add));
        ex.markDirty();
    }

    private static HeroSkill soulSlice() {
        return make(KatanaNumbers.SOUL_SLICE, c -> {
            LivingEntity target = GlassKit.focus(c, KatanaNumbers.SLICE_RANGE);
            if (target == null) {
                c.say("No foe within reach on your crosshair.");
                return false;
            }
            flow(c, KatanaNumbers.SOUL_SLICE);
            GlassKit.strike(c, target, KatanaNumbers.SLICE_RAW);
            double taken = EnergyBridge.drain(target, null, KatanaNumbers.SLICE_DRAIN);
            takeBack(c, taken);
            c.fx(VfxShape.KOTO_SHATTER, target.getBoundingBox().getCenter(), GlassKit.chest(c.player), PINK, 16, 1.0f);
            c.sound(SoundEvents.VEX_CHARGE, 0.8f, 1.5f);
            GlassKit.swing(c);
            return true;
        });
    }

    // ------------------------------------------------------------------ Kagura Dance of Sakura
    private static HeroSkill sakuraDance() {
        return make(KatanaNumbers.SAKURA_DANCE, c -> {
            Vec3 feet = c.player.position();
            if (GlassKit.foes(c, feet, KatanaNumbers.SAKURA_LINE, 1).isEmpty()) {
                c.say("There are no foes near enough to dance for.");
                return false;
            }
            KatanaSession s = session(c);
            long now = c.level.getGameTime();
            int tempo = s.chain().level(now);
            s.chain().reset();
            s.acted(now);
            double per = KatanaNumbers.sakuraFormation(tempo);
            // the tell: a pink seal of petals under you; the three formations follow ten ticks apart
            GlassKit.ring(c, feet, KatanaNumbers.SAKURA_RADIUS, PINK_RGB);
            c.fx(VfxShape.CHERRY_BLOSSOM_FX2, feet, feet, PINK, KatanaNumbers.SAKURA_FORMATIONS * KatanaNumbers.SAKURA_GAP + 6, (float) KatanaNumbers.SAKURA_RADIUS);
            c.sound(SoundEvents.ENDER_DRAGON_FLAP, 0.8f, 1.8f);
            c.say("Kagura Dance of Sakura" + (tempo > 0 ? " at tempo " + tempo : ""));
            GlassKit.swing(c);
            // the first formation: the blossom, a ring round you
            GlassKit.later(c, KatanaNumbers.SAKURA_GAP, () -> {
                Vec3 at = c.player.position();
                for (LivingEntity e : GlassKit.foes(c, at, KatanaNumbers.SAKURA_RADIUS, KatanaNumbers.SAKURA_TARGETS)) {
                    if (GlassKit.strike(c, e, per)) GlassKit.push(c, e, at, 0.7);
                }
                c.fx(VfxShape.CHERRY_BLOSSOM_FX3, at.add(0.0, 1.0, 0.0), at.add(0.0, 1.5, 0.0), PINK, 22, 1.5f);
                c.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);
                GlassKit.swing(c);
            });
            // the second formation: the beginning blossom, a cone ahead
            GlassKit.later(c, 2 * KatanaNumbers.SAKURA_GAP, () -> {
                Vec3 chest = GlassKit.chest(c.player);
                for (LivingEntity e : GlassKit.foesInCone(c, KatanaNumbers.SAKURA_CONE_RANGE, KatanaNumbers.SAKURA_CONE_HALF, KatanaNumbers.SAKURA_TARGETS)) GlassKit.strike(c, e, per);
                c.fx(VfxShape.CHERRY_BLOSSOM_FX3, chest, chest.add(c.look().scale(5.0)), PINK, 22, 1.3f);
                c.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);
                GlassKit.swing(c);
            });
            // the third formation: the final line
            GlassKit.later(c, 3 * KatanaNumbers.SAKURA_GAP, () -> {
                Vec3 from = c.eye().subtract(0.0, 0.3, 0.0), look = c.look();
                Vec3 end = GlassKit.clipEnd(c, from, from.add(look.scale(KatanaNumbers.SAKURA_LINE)));
                for (LivingEntity e : GlassKit.foesAlong(c, from, end, KatanaNumbers.SAKURA_LINE_WIDTH, KatanaNumbers.SAKURA_TARGETS)) GlassKit.strike(c, e, per);
                c.fx(VfxShape.DARK_IAI, from, end, WHITE, 14, 1.6f);
                c.fx(VfxShape.CHERRY_BLOSSOM_FX3, end, end.add(0.0, 1.0, 0.0), PINK, 26, 2.0f);
                c.sound(SoundEvents.PLAYER_ATTACK_CRIT, 1.2f, 1.2f);
                GlassKit.swing(c);
            });
            return true;
        }).asUltimate();
    }
}
