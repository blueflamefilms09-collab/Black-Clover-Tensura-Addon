package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.prop.ShakudoPropState;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 1.93.0 Nail Magic: the pages appended after the five generic ones. Nail is the body-and-iron magic of nails driven through things and fingernails grown
 * into claws; the repo's wiki extract has no page for it, so the book is the nearest honest version (see docs/attributes/nail_rebuild.md).
 * Pages (appended modes 5 to 9):
 * <ul>
 *   <li>{@code nail_barrage} Nail Barrage (starter): three to five giant nails fly at the crosshair, pin what they hit (Slowness III) and stay driven in a wall;</li>
 *   <li>{@code nail_driver} Nail Driver (mid, behind the control gate): a spectral hammer comes down on the target and pins it to the ground (2 s, shortened for bosses);</li>
 *   <li>{@code nail_mail} Nail Mail (mid): the temporary armour, 25 percent of every blow softened, attackers pricked and slowed, Resistance II, 20 s;</li>
 *   <li>{@code nail_pin} Pinning Nails (mid, behind the control gate): three nails driven through the ground at the target's feet; it cannot run or jump for 3 s (shortened for bosses);</li>
 *   <li>{@code nail_storm} Nail Storm (signature, 100 s): for 4 s nails fall in a 6 block ring round the crosshair, each hurting what stands where it lands.</li>
 * </ul>
 */
public final class NailSpells {
    private NailSpells() {}

    public static final int BOLT_TICKS = 60, HAMMER_TICKS = 70, ARMOUR_TICKS = 400, STORM_TICKS = 80;

    public static List<BookPage> pages() {
        return List.of(
                BookPage.starter("nail_barrage", "Nail Barrage", NailSpells::barrage),
                BookPage.mid("nail_driver", "Nail Driver", NailSpells::driver),
                BookPage.mid("nail_mail", "Nail Mail", (b, i, p, m) -> ShakudoKit.guard(b, p, MagicType.NAIL, ARMOUR_TICKS, 0.25f, 2f + 0.3f * ShakudoKit.mighty(p), 1)),
                BookPage.mid("nail_pin", "Pinning Nails", NailSpells::pin),
                BookPage.signature("nail_storm", "Nail Storm", NailSpells::storm).withCooldown(1800));
    }

    /** Three nails, five for a mighty caster, each in its own direction of a small fan. */
    static boolean barrage(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int n = ShakudoKit.mighty(p) >= 4 ? 5 : 3;
        Vec3 start = p.getEyePosition().add(0, -0.2, 0), look = p.getViewVector(1f), side = look.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : side.normalize();
        LivingEntity foe = GrimoireBook.target(p, 30);
        float s = ShakudoKit.scaleFor(p, 1.0f) * ShakudoKit.k(i, p) * 0.9f;
        int made = 0;
        for (int k = 0; k < n; k++) {
            double spread = (k - (n - 1) / 2.0) * 0.07;
            Vec3 dir = look.add(side.scale(spread)).add(0, 0.015 * (k % 2), 0).normalize();
            MagicPropEntity e = ShakudoKit.spawn(p, PropKind.NAIL_1, start.add(dir.scale(0.7)).add(side.scale(spread * 6)), p.getYRot(), Math.min(1.6f, s), BOLT_TICKS, 0, foe);
            if (e == null) continue;
            e.setDeltaMovement(dir.scale(1.6));
            made++;
        }
        if (made == 0) return false;
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.NAIL_FX1, start, start.add(look.scale(18)), 14, 0.9f);
        return true;
    }

    /** The hammer: needs the control gate; lands on the foe under the crosshair within 18 blocks. */
    static boolean driver(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 18);
        if (t == null) { GrimoireBook.fail(p, "No target."); return false; }
        if (!GrimoireBook.control(p)) return false;
        MagicPropEntity e = ShakudoKit.spawn(p, PropKind.NAIL_2, t.position(), p.getYRot(), ShakudoKit.scaleFor(p, 1.0f), HAMMER_TICKS, 0, t);
        if (e == null) return false;
        b.castCircle(p, 1.0f);
        b.vfx(p, VfxShape.NAIL_FX2, t.position(), t.position().add(0, 1, 0), 24, 2.2f);
        return true;
    }

    /** Three nails driven into the ground round the foe, and it is held: Slowness and no jumping for the shortened control time. */
    static boolean pin(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 20);
        if (t == null) { GrimoireBook.fail(p, "No target."); return false; }
        if (!GrimoireBook.control(p)) return false;
        int ticks = BalanceLaw.controlTicks(t, 60);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6));
        t.addEffect(new MobEffectInstance(MobEffects.JUMP, ticks, 128));
        b.hurt(i, p, t, mode, 4f * ShakudoKit.k(i, p));
        for (int k = 0; k < 3; k++) {
            double a = Mth.TWO_PI * k / 3.0 + 0.5;
            Vec3 at = t.position().add(Math.cos(a) * 0.7, 0, Math.sin(a) * 0.7);
            MagicPropEntity e = ShakudoKit.spawn(p, PropKind.NAIL_1, at.add(0, 0.5, 0), p.getYRot(), 0.8f, ticks + 10, 0, null);
            if (e == null) continue;
            e.setParam(ShakudoPropState.pack(0, ShakudoPropState.DONE, p.level().getGameTime()));
            e.setXRot(90f);
        }
        b.castCircle(p, 0.9f);
        VfxSpawn.send(p.serverLevel(), VfxShape.NAIL_FX3, t.position(), t.position().add(0, 1, 0), b.color, 20, 0.9f);
        return true;
    }

    /** The Nail Storm: for 4 s, every 4 ticks a nail lands at a random point of a 6 block ring round the crosshair and hurts what is there. */
    static boolean storm(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        Vec3 c = GrimoireBook.aim(p, 24);
        float k = ShakudoKit.k(i, p);
        b.castCircle(p, 1.6f);
        b.vfx(p, VfxShape.NAIL_FX2, c, c.add(0, 1, 0), STORM_TICKS, 6.0f);
        SpellRuntime.zone(level, STORM_TICKS, 4, age -> {
            if (!p.isAlive()) return;
            double a = level.random.nextDouble() * Mth.TWO_PI, r = Math.sqrt(level.random.nextDouble()) * 6.0;
            Vec3 at = c.add(Math.cos(a) * r, 0, Math.sin(a) * r);
            for (LivingEntity t : GrimoireBook.around(p, at, 1.4)) {
                b.hurt(i, p, t, mode, 4f * k);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.isBoss(t) ? 15 : 40, 2));
            }
        });
        return true;
    }
}
