package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the strikes of Escanor's moves (the axe's arcs and smash, the flare, the flame waves, the charge, the heat hazes, the sunbeam pillars, the spin, the Cruel
 * Sun and the Zenith Cleave) as plain static functions on a {@link ZogratisBoss}, so that the boss class holds only his rules (the sun, the power it allows,
 * the forms) and a reader can see in one place what every blow does and what dodges it. The axe is steel, so its blows are the boss's plain physical blow
 * (a raised shield helps); the flames, the heat and the light are typed Tensura damage, so the resistances the Charter grimoire of the same name lends
 * count. Every blow goes through the boss's damage law, every zone is bounded in time and in the foes it looks at, a step that throws only ends its own zone,
 * and nothing changes a block.
 */
public final class EscanorMoves {
    private EscanorMoves() {}

    /** Colours (0xRRGGBB): the sun's gold, its pale rim, and the red of a warning. */
    static final int GOLD = 0xFFB040, PALE = 0xFFE680, WARN = 0xFF4A4A;
    private static final ResourceKey<DamageType> FIRE = TensuraDamageTypes.FIRE_ELEMENTAL, HEAT = TensuraDamageTypes.HEAT_WAVE, LIGHT = TensuraDamageTypes.LIGHT_ELEMENTAL;

    // ================================================================ the axe
    /** A wide cut of Rhitta in a cone in front of him (steel: the boss's plain blow). Returns how many it hit. */
    static int cone(ZogratisBoss b, ServerLevel sl, Vec3 facing, double reach, double halfAngle, float damage, double knock) {
        Vec3 apex = b.position(), chest = apex.add(0, b.getBbHeight() * 0.6, 0);
        SdsaStrikes.fx(sl, VfxShape.SLASH_WAVE, chest, chest.add(facing.scale(reach)), GOLD, 10, 3.0f, true);
        SdsaStrikes.sound(sl, apex, SoundEvents.PLAYER_ATTACK_SWEEP, 1.8f, 0.55f);
        int n = 0;
        for (LivingEntity e : SdsaStrikes.foesInCone(b, apex, facing, reach, halfAngle)) {
            if (SdsaStrikes.covered(sl, chest, e)) continue;
            if (b.strike(e, damage, false)) {
                SdsaStrikes.push(e, apex, knock, 0.1);
                n++;
            }
        }
        return n;
    }

    /** The smash: a blow on the ground {@code ahead} blocks in front of him (a disc of {@code radius}), and a low ring wave that runs out from it. */
    static void smash(ZogratisBoss b, ServerLevel sl, Vec3 facing, double ahead, double radius, float damage, double waveReach, float waveDamage) {
        Vec3 c = ZogratisBoss.ground(sl, b.position().add(facing.scale(ahead)));
        SdsaStrikes.fx(sl, VfxShape.GRAVITY_IMPACT, c, c.add(0, 1, 0), GOLD, 22, (float) radius * 1.4f, true);
        SdsaStrikes.sound(sl, c, SoundEvents.ANVIL_LAND, 1.8f, 0.5f);
        for (LivingEntity e : SdsaStrikes.foesInDisc(b, c, radius)) {
            if (b.strike(e, damage, false)) SdsaStrikes.push(e, c, 0.9, 0.3);
        }
        SdsaStrikes.wave(b, sl, c, waveReach, waveDamage, 0.5, GOLD, null);
    }

    /** The spin: a full circle of cuts round him, twice, 8 ticks apart. Back away or have a shield up. */
    static void spin(ZogratisBoss b, ServerLevel sl, double radius, float damage) {
        for (int k = 0; k < 2; k++) {
            b.later(k * 8, SdsaStrikes.safe("sun spin", () -> {
                Vec3 c = b.position();
                SdsaStrikes.fx(sl, VfxShape.SLASH_SCYTHE, c.add(0, 1, 0), c.add(0, 1, 1), GOLD, 12, (float) radius, true);
                SdsaStrikes.sound(sl, c, SoundEvents.PLAYER_ATTACK_SWEEP, 1.6f, 0.6f);
                for (LivingEntity e : SdsaStrikes.foesInDisc(b, c, radius)) {
                    if (b.strike(e, damage, false)) SdsaStrikes.push(e, c, 0.55, 0.12);
                }
            }));
        }
    }

    // ================================================================ the flames
    /** The flare: the air round him catches fire and shoves everything outward. */
    static void flare(ZogratisBoss b, ServerLevel sl, double radius, float damage) {
        Vec3 c = b.position();
        SdsaStrikes.fx(sl, VfxShape.FIRE_BURST, c, c.add(0, 1, 0), GOLD, 20, (float) radius, true);
        SdsaStrikes.fx(sl, VfxShape.LIGHT_FLARE, c.add(0, 1.2, 0), c.add(0, 2.2, 0), PALE, 12, 3.0f, true);
        SdsaStrikes.sound(sl, c, SoundEvents.FIRECHARGE_USE, 1.8f, 0.6f);
        for (LivingEntity e : SdsaStrikes.foesInDisc(b, c, radius)) {
            if (SdsaStrikes.hit(b, e, damage * (SdsaStrikes.covered(sl, c.add(0, 1, 0), e) ? 0.4f : 1f), FIRE)) SdsaStrikes.push(e, c, 1.0, 0.2);
        }
    }

    /** One flame wave of Rhitta: a lion of fire that runs along the floor and bursts on the first foe it meets. */
    static void flameWave(ZogratisBoss b, ServerLevel sl, Vec3 start, Vec3 dir, float damage) {
        SdsaStrikes.fx(sl, VfxShape.FIRE_LION, start, start.add(dir.scale(16)), GOLD, 24, 1.4f, true);
        SdsaStrikes.shoot(b, sl, start, dir, 1.4, 26, 0.9, damage, FIRE,
                (e, at) -> {
                    Tells.burst(sl, at, GOLD, 14);
                    SdsaStrikes.push(e, start, 0.6, 0.1);
                },
                p -> Tells.gather(sl, p, 0.15, GOLD, 2));
    }

    /**
     * The charge: along {@code dir} for up to {@code length}, cutting everything in the strip, and at the end a low ring wave. He stops short of the end of the
     * strip by a body's width, so he never stands inside the one he hit.
     */
    static void charge(ZogratisBoss b, ServerLevel sl, Vec3 dir, double length, double halfWidth, float damage, double ringReach, float ringDamage) {
        Vec3 from = b.position(), eye = from.add(0, 1.0, 0);
        Vec3 want = SdsaStrikes.clipEnd(sl, b, eye, eye.add(dir.scale(length)));
        double reach = Math.max(0.0, SdsaStrikes.along(from, dir, want));
        Vec3 stop = ZogratisBoss.groundOrNull(sl, from.add(dir.scale(Math.max(0.0, reach - 1.0))));
        SdsaStrikes.fx(sl, VfxShape.FIRE_LION, from.add(0, 0.5, 0), from.add(dir.scale(reach)).add(0, 0.5, 0), GOLD, 12, 2.0f, true);
        SdsaStrikes.sound(sl, from, SoundEvents.PLAYER_ATTACK_STRONG, 1.8f, 0.5f);
        for (LivingEntity e : SdsaStrikes.foesInStrip(b, from, dir, reach + 0.5, halfWidth)) {
            if (b.strike(e, damage, false)) SdsaStrikes.push(e, from, 0.8, 0.2);
        }
        if (stop != null && reach >= 1.8) SdsaStrikes.dashTo(b, stop);
        Vec3 end = b.position();
        SdsaStrikes.wave(b, sl, end, ringReach, ringDamage, 0.5, GOLD, FIRE);
    }

    // ================================================================ the light and the heat
    /** The heat hazes: discs of shimmering heat on the marks that burn whoever stands in them every half second for {@code ticks}. */
    static void haze(ZogratisBoss b, ServerLevel sl, List<Vec3> marks, double radius, int ticks, float damage) {
        for (Vec3 m : marks) {
            SdsaStrikes.fx(sl, VfxShape.FIRE_PILLAR, m, m.add(0, 1, 0), GOLD, 24, (float) radius, true);
            SpellRuntime.zone(sl, ticks, 5, SdsaStrikes.safe("heat haze", age -> {
                if (!b.isAlive() || b.isRemoved()) return;
                if (age % 10 == 0) {
                    Tells.ring(sl, m, radius, GOLD, 1.3f, (int) Math.min(64, 12 + radius * 5));
                    Tells.gather(sl, m.add(0, 0.5, 0), radius * 0.6, PALE, 4);
                    for (LivingEntity e : SdsaStrikes.foesInDisc(b, m, radius)) SdsaStrikes.hit(b, e, damage, HEAT);
                }
            }));
        }
    }

    /** The sunbeam pillars: a column of light on every mark the wind-up showed. A foe is hurt once, however many columns overlap; a shield or anti-magic cuts the blow. */
    static void pillars(ZogratisBoss b, ServerLevel sl, List<Vec3> marks, double radius, float damage) {
        Set<Integer> hit = new HashSet<>();
        SdsaStrikes.sound(sl, b.position(), SoundEvents.BEACON_ACTIVATE, 2.0f, 1.4f);
        for (Vec3 m : marks) {
            SdsaStrikes.fx(sl, VfxShape.DIVINE_PILLAR, m, m.add(0, 1, 0), GOLD, 30, (float) radius * 1.6f, true);
            for (LivingEntity e : SdsaStrikes.foesInDisc(b, m, radius)) {
                if (!hit.add(e.getId())) continue;
                if (SdsaStrikes.hit(b, e, damage * SdsaStrikes.guard(e), LIGHT)) SdsaStrikes.push(e, m, 0.5, 0.45);
            }
        }
    }

    // ================================================================ the ultimates
    /**
     * Cruel Sun: a small sun thrown from his hand to the mark, {@link CruelSunMath#FLIGHT} ticks. It bursts over {@code radius} for the sun's damage; a block
     * between the sun and a foe, or a raised shield, cuts the blow ({@link CruelSunMath#factor}), and the ground burns on afterwards. If the party broke it
     * during the wind-up it falls apart in his hand: a small flash and nothing else.
     */
    static void cruelSun(ZogratisBoss b, ServerLevel sl, Vec3 mark, double radius, float damage, int scorchTicks, boolean broken) {
        Vec3 hand = b.position().add(0, b.getBbHeight() * 0.9, 0), sun = mark.add(0, 5.0, 0);
        if (broken) {
            SdsaStrikes.fx(sl, VfxShape.LIGHT_FLARE, hand, hand.add(0, 1, 0), PALE, 16, 3.0f, true);
            SdsaStrikes.sound(sl, hand, SoundEvents.SHIELD_BREAK, 2.0f, 0.5f);
            return;
        }
        SdsaStrikes.fx(sl, VfxShape.FIRE_SPEAR, hand, sun, GOLD, CruelSunMath.FLIGHT, 2.4f, true);
        SdsaStrikes.sound(sl, hand, SoundEvents.WITHER_SHOOT, 2.0f, 0.6f);
        b.later(CruelSunMath.FLIGHT, SdsaStrikes.safe("cruel sun", () -> {
            SdsaStrikes.fx(sl, VfxShape.FIRE_WILD, mark, mark.add(0, 1, 0), GOLD, 44, (float) radius, true);
            SdsaStrikes.fx(sl, VfxShape.LIGHT_FLARE, sun, sun.add(0, 1, 0), PALE, 20, 6.0f, true);
            SdsaStrikes.sound(sl, mark, SoundEvents.WARDEN_SONIC_BOOM, 3.0f, 0.5f);
            SdsaStrikes.sound(sl, mark, SoundEvents.ENDER_DRAGON_GROWL, 2.5f, 0.6f);
            for (LivingEntity e : SdsaStrikes.foesInDisc(b, mark, radius)) {
                float f = CruelSunMath.factor(SdsaStrikes.covered(sl, sun, e), e.isBlocking());
                if (SdsaStrikes.hit(b, e, damage * f, FIRE)) SdsaStrikes.push(e, mark, 1.2 * f, 0.35 * f);
            }
            scorch(b, sl, mark, CruelSunMath.scorchRadius(radius), scorchTicks);
        }));
    }

    /** The ground left burning after a Cruel Sun: a small disc that burns once a second. */
    private static void scorch(ZogratisBoss b, ServerLevel sl, Vec3 centre, double radius, int ticks) {
        SpellRuntime.zone(sl, ticks, 10, SdsaStrikes.safe("scorched ground", age -> {
            if (!b.isAlive() || b.isRemoved()) return;
            Tells.ring(sl, centre, radius, GOLD, 1.3f, (int) Math.min(64, 12 + radius * 5));
            if (age % 20 == 0) for (LivingEntity e : SdsaStrikes.foesInDisc(b, centre, radius)) SdsaStrikes.hit(b, e, CruelSunMath.SCORCH_DAMAGE, HEAT);
        }));
    }

    /**
     * Zenith Cleave: the One's own blow. Rhitta comes down on the mark: the centre takes the full cleave (a shield or anti-magic cuts it), and a low shock runs out
     * from it as far as {@link CruelSunMath#ZENITH_REACH} (jump it).
     */
    static void zenith(ZogratisBoss b, ServerLevel sl, Vec3 centre, float centreDamage, float waveDamage) {
        SdsaStrikes.fx(sl, VfxShape.DIVINE_PILLAR, centre, centre.add(0, 1, 0), PALE, 40, (float) CruelSunMath.ZENITH_CENTRE * 2.2f, true);
        SdsaStrikes.fx(sl, VfxShape.GRAVITY_IMPACT, centre, centre.add(0, 1, 0), GOLD, 26, (float) CruelSunMath.ZENITH_CENTRE * 1.8f, true);
        SdsaStrikes.sound(sl, centre, SoundEvents.ANVIL_LAND, 3.0f, 0.4f);
        SdsaStrikes.sound(sl, centre, SoundEvents.WARDEN_SONIC_BOOM, 3.0f, 0.5f);
        for (LivingEntity e : SdsaStrikes.foesInDisc(b, centre, CruelSunMath.ZENITH_CENTRE)) {
            if (b.strike(e, centreDamage * SdsaStrikes.guard(e), false)) SdsaStrikes.push(e, centre, 1.3, 0.45);
        }
        SdsaStrikes.wave(b, sl, centre, CruelSunMath.ZENITH_REACH, waveDamage, 0.8, GOLD, HEAT);
    }
}
