package com.newuniverse.nusmp.heroweapon.weapon.bow;

import com.newuniverse.nusmp.heroweapon.HeroConfig;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Foes;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Hits;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Rays;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Tasks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: this is where the string of the Legendary Bow turns into REAL arrows (the setting {@code heroweapon.bow.realArrows}, on by default; off keeps the
 * hitscan arrows of {@link BowShots} exactly as they were). It makes the arrow entities ({@link HeroArrow}), gives them the damage the hitscan arrow dealt
 * (the physical hit; the magic hit is added by the arrow from the form's share), the effect of the form or of the armed technique, and the flight of the
 * technique (straight and fast for the sniper arrow, homing for the seeker, a pierce for the piercing forms). It also fires the volleys of the rain
 * techniques and the Arrow Squall. Every arrow takes a place in {@link ArrowBudget} first: over the per-wielder or global cap nothing is spawned, so the
 * worst case is the cap, whatever is asked.
 */
final class BowArrows {
    private BowArrows() {}

    /** Flight flags: no gravity; a volley arrow (never lingers stuck, never bursts); a child arrow (never splits again). */
    static final int STRAIGHT = 1, VOLLEY = 2, CHILD = 4;
    /** How high above the target a rain arrow starts, and how fast it falls (blocks per tick). */
    static final double RAIN_HEIGHT = 12.0;
    static final float RAIN_SPEED = 2.0f;

    /** True when the wielder already has as many hero arrows out as the cap allows (the shot would not leave the string). */
    static boolean full(HeroCtx c) {
        return ArrowBudget.count(c.player.getUUID()) >= HeroConfig.bowArrowsPerPlayer() || ArrowBudget.total() >= HeroConfig.bowArrowsGlobal();
    }

    // ================================================================ one arrow
    /** Spawns one hero arrow at {@code pos} flying along {@code dir}; null (and nothing spawned) when the setting is off or a cap is reached. */
    static HeroArrow fire(HeroCtx c, Vec3 pos, Vec3 dir, float speed, float inaccuracy, double raw, ArrowKind kind, int flags) {
        if (!HeroConfig.realArrows()) return null;
        if (!ArrowBudget.tryAcquire(c.player.getUUID(), HeroConfig.bowArrowsPerPlayer(), HeroConfig.bowArrowsGlobal())) return null;
        HeroArrow a = HeroArrowEntities.HERO_ARROW.get().create(c.level);
        if (a == null) {
            ArrowBudget.release(c.player.getUUID());
            return null;
        }
        BowArrowForms.Look look = BowArrowForms.of(c.form.id());
        a.configure(c, kind, BowArrowForms.index(c.form.id()), raw, look.magicShare(), (flags & VOLLEY) != 0, (flags & CHILD) != 0);
        a.setOwner(c.player);
        a.pickup = AbstractArrow.Pickup.DISALLOWED;
        a.setPos(pos.x, pos.y, pos.z);
        a.setCritArrow(false);
        if ((flags & STRAIGHT) != 0) a.setNoGravity(true);
        a.shoot(dir.x, dir.y, dir.z, speed, inaccuracy);
        if (!c.level.addFreshEntity(a)) {
            a.releaseBudget();
            return null;
        }
        return a;
    }

    // ================================================================ the string is let go
    /**
     * The release of a drawn bow as real arrows: the plain arrow of the form (with the form's effect), or the arrow of the armed technique. Returns true
     * when it handled the shot (always, once the setting is on); the caller has already paid and checked the cap.
     */
    static boolean release(HeroCtx c, ArmedMode mode, double f) {
        BowArrowForms.Look look = BowArrowForms.of(c.form.id());
        ArrowKind kind = ArrowKind.of(mode, look.kind());
        Vec3 dir = c.look(), muzzle = BowFx.muzzle(c, dir);
        float speed = (float) (1.2 + 1.8 * f);
        if (c.form.has(BowIds.FARSIGHT)) speed = Math.min(3.6f, speed * 1.15f);
        HeroArrow a;                                                                 // only read inside the arm that sets it
        switch (mode) {
            case SNIPER -> {
                a = fire(c, muzzle, dir, 4.0f, 0.0f, BowNumbers.SNIPER_RAW, kind, STRAIGHT);
                if (a != null) {
                    a.setPierce(BowNumbers.SNIPER_TARGETS - 1, 1.0);
                    a.setLife(25);                                                   // 64 blocks at 4 a tick
                }
            }
            case NET -> {
                a = fire(c, muzzle, dir, 3.0f, 0.0f, BowNumbers.NET_RAW, kind, 0);
                if (a != null) a.setLife(40);
            }
            case MARK -> {
                a = fire(c, muzzle, dir, 3.0f, 0.0f, BowNumbers.MARK_RAW, kind, 0);
                if (a != null) a.setLife(40);
            }
            case BURST -> a = fire(c, muzzle, dir, 3.0f, 0.0f, 0.0, kind, 0);        // no hit of its own: the burst is the damage
            case SEEK -> {
                a = fire(c, muzzle, dir, (float) BowNumbers.SEEK_SPEED, 0.0f, BowNumbers.SEEK_RAW, kind, STRAIGHT);
                if (a != null) {
                    a.setSeek(Foes.mostAligned(c, c.eye(), dir, BowNumbers.SEEK_RANGE, BowNumbers.SEEK_CONE));
                    a.setLife(60);
                }
            }
            case COMMAND -> a = fire(c, muzzle, dir, 3.0f, 0.0f, BowNumbers.COMMAND_RAW, kind, 0);
            case SECOND -> {
                double first = BowShots.plainRaw(c, f);
                plain(c, muzzle, dir, speed, f, first, kind);
                final float speedNow = speed; final double fNow = f;
                Tasks.later(c, BowNumbers.SECOND_DELAY, () -> {
                    BowFx.swing(c);
                    BowFx.sound(c, c.eye(), SoundEvents.ARROW_SHOOT, 1.0f, 1.5f);
                    Vec3 d = c.look();
                    plain(c, BowFx.muzzle(c, d), d, speedNow, fNow, BowNumbers.secondRaw(first), kind);
                });
                return true;
            }
            default -> plain(c, muzzle, dir, speed, f, BowShots.plainRaw(c, f), kind);
        }
        return true;
    }

    /** The plain arrow of the form: pierces a second foe with Piercing Arrows, one more with a piercing form, farther with Eagle Eye. */
    private static HeroArrow plain(HeroCtx c, Vec3 pos, Vec3 dir, float speed, double f, double raw, ArrowKind kind) {
        HeroArrow a = fire(c, pos, dir, speed, (float) (1.0 - f) * 0.6f, raw, kind, 0);
        if (a == null) return null;
        int pierce = (c.form.has(BowIds.PIERCING_ARROWS) ? 1 : 0) + (kind == ArrowKind.PIERCE ? 1 : 0);
        if (pierce > 0) a.setPierce(pierce, BowNumbers.PIERCE_FACTOR);
        a.setEagle(c.form.has(BowIds.EAGLE_EYE));
        return a;
    }

    // ================================================================ volleys
    /**
     * One wave of a rain technique as real arrows: up to {@code arrows} of them fall from above; the first ones are aimed at the foes inside the circle
     * (each carries the wave's damage), the rest scatter over the circle (half of it). {@code forced} is the technique's own effect (frost, stars, pride),
     * null for the form's. An aimed arrow that cannot be spawned (a cap) falls back to the old direct wave hit on its foe, so a capped rain still rains.
     */
    static void rainWave(HeroCtx c, Vec3 centre, double radius, int arrows, double rawWave, int wave, ArrowKind forced) {
        ArrowKind kind = forced != null ? forced : childKind(BowArrowForms.of(c.form.id()).kind());
        List<LivingEntity> foes = Foes.around(c, centre, radius, Math.min(arrows, Foes.MAX));
        double[][] scatter = Volley.scatter(arrows, radius, wave);
        Hits fallback = new Hits();
        for (int i = 0; i < arrows; i++) {
            LivingEntity target = i < foes.size() ? foes.get(i) : null;
            double x, y, z, raw;
            if (target != null) {
                x = target.getX();
                y = target.getY();
                z = target.getZ();
                raw = rawWave;
            } else {
                x = centre.x + scatter[i][0];
                y = centre.y;
                z = centre.z + scatter[i][1];
                raw = rawWave * ArrowSplit.SCATTER_SHARE;
            }
            Vec3 base = new Vec3(x, y + 0.5, z);
            Vec3 top = Rays.clipEnd(c, base, base.add(0.0, RAIN_HEIGHT, 0.0));      // a roof over the spot lowers the start
            HeroArrow a = null;
            if (top.y - base.y > 1.5) a = fire(c, top.add(0.0, -0.5, 0.0), new Vec3(0.0, -1.0, 0.0), RAIN_SPEED, 0.05f, raw, kind, VOLLEY | CHILD);
            if (a != null) a.setLife(40);
            else if (target != null) fallback.add(target, (float) raw);
        }
        fallback.apply(c, Foes.MAX);
    }

    /** The effect of a volley arrow: the form's, except that a volley arrow never splits again. */
    private static ArrowKind childKind(ArrowKind k) { return k == ArrowKind.SPLIT ? ArrowKind.LIGHT : k; }

    /** The Arrow Squall: one arrow along each of the fan's directions. */
    static void squall(HeroCtx c, Vec3 muzzle, List<Vec3> dirs, double rawEach) {
        ArrowKind kind = childKind(BowArrowForms.of(c.form.id()).kind());
        for (Vec3 d : dirs) {
            HeroArrow a = fire(c, muzzle, d, 2.4f, 0.0f, rawEach, kind, VOLLEY | CHILD);
            if (a != null) a.setLife(20);
        }
    }
}
