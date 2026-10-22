package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.prop.ShakudoPropKit;
import com.newuniverse.nusmp.scalecast.ScaleCastKit;
import com.newuniverse.nusmp.scalecast.ScaleCastRules;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.93.0 Scale Magic: the pages appended after the five generic ones. Scale is the body magic of fish, lizards and dragons: overlapping scales that turn
 * blows, shed and regrow; the repo's wiki extract has no page for it, so the book is the nearest honest version (see docs/attributes/scale_rebuild.md).
 * Pages (appended modes 5 to 9):
 * <ul>
 *   <li>{@code scale_volley} Scale Volley (starter): five scales in a fan, each a small piercing bolt (Weakness);</li>
 *   <li>{@code scale_serpent} Scale Serpent (zone tier): a wyrm of scales (two for a mighty caster) that fights for 25 s;</li>
 *   <li>{@code scale_shield} Scale Shield (mid): a kite shield of scales hovering in front of the caster for 15 s that turns every projectile back;</li>
 *   <li>{@code scale_mail} Scale Mail (mid): the temporary armour, 25 percent of every blow softened and 20 percent more against projectiles, Resistance II, 20 s;</li>
 *   <li>{@code scale_tide} Shedding Tide (signature, 90 s): the scales are shed in a wave (6 blocks, thrown back) and the caster sloughs every harmful effect and heals.</li>
 * </ul>
 */
public final class ScaleSpells {
    private ScaleSpells() {}

    public static final int SERPENT_TICKS = 500, SHIELD_TICKS = 300, ARMOUR_TICKS = 400;

    public static List<BookPage> pages() {
        return List.of(
                BookPage.starter("scale_volley", "Scale Volley", ScaleSpells::volley),
                BookPage.zone("scale_serpent", "Scale Serpent", ScaleSpells::serpent),
                BookPage.mid("scale_shield", "Scale Shield", ScaleSpells::shield),
                BookPage.mid("scale_mail", "Scale Mail", (b, i, p, m) -> ShakudoKit.guard(b, p, MagicType.SCALE, ARMOUR_TICKS, 0.25f, 0f, 1)),
                BookPage.signature("scale_tide", "Shedding Tide", ScaleSpells::tide).withCooldown(1800));
    }

    /** Five scales in a fan of 8 degrees steps; each flies 22 blocks at most and hurts the first thing it meets. */
    static boolean volley(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float k = ShakudoKit.k(i, p);
        Vec3 start = p.getEyePosition().add(0, -0.15, 0), look = p.getViewVector(1f);
        ScaleCastKit.show(p, ScaleCastRules.LINGER);                                           // Scale casting: the scales leave the floating balance scale (magic.scaleCasting off: the old start at the caster's eyes)
        Vec3 from = ScaleCastKit.origin(p, start);
        if (from != start) {                                                                    // aimed at the point under the crosshair, so the shift to the Scale does not move the volley off its mark
            start = from;
            look = p.getEyePosition().add(look.scale(18)).subtract(start).normalize();
        }
        b.castCircle(p, 0.7f);
        Vec3 axis = new Vec3(0, 1, 0);
        Vec3 side = look.cross(axis);
        side = side.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : side.normalize();
        for (int n = -2; n <= 2; n++) {
            Vec3 dir = look.add(side.scale(n * 0.14)).normalize();
            b.vfx(p, VfxShape.SCALE_FX1, start, start.add(dir.scale(16)), 14, 0.7f);
            SpellRuntime.bolt(p, start, dir.scale(1.6), 0.5, 14, false, null, (bolt, t) -> {
                b.hurt(i, p, t, mode, 5f * k);
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, BalanceLaw.isBoss(t) ? 30 : 70, 0));
            }, (bolt, at) -> VfxSpawn.send(p.serverLevel(), VfxShape.SCALE_FX3, at, at.add(0, 1, 0), b.color, 14, 0.4f));
        }
        return true;
    }

    /** The serpent: one wyrm, two for a caster of 5 or more EP scale; a new cast replaces the old ones. */
    static boolean serpent(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 look = ShakudoKit.flatLook(p), side = new Vec3(-look.z, 0, look.x);
        ShakudoKit.dismiss(p, PropKind.SCALE_1);
        int n = ShakudoKit.mighty(p) >= 5 ? 2 : 1, made = 0;
        for (int k = 0; k < n; k++) {
            Vec3 at = p.position().add(look.scale(2.0)).add(side.scale((k == 0 ? 1 : -1) * 1.2));
            if (ShakudoKit.spawn(p, PropKind.SCALE_1, at, p.getYRot(), ShakudoKit.scaleFor(p, 0.9f), ShakudoPropKit.lifeFor(p, SERPENT_TICKS), 0, null) != null) made++;
        }
        if (made == 0) { GrimoireBook.fail(p, "The scales would not take form."); return false; }
        b.castCircle(p, 1.2f);
        VfxSpawn.send(p.serverLevel(), VfxShape.SCALE_FX2, p.position().add(look.scale(2.0)), p.position().add(0, 1, 0), b.color, 30, 2.4f);
        return true;
    }

    /** The shield: hovers 1.4 blocks in front of the caster and follows them. */
    static boolean shield(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ShakudoKit.dismiss(p, PropKind.SCALE_2);
        Vec3 at = p.position().add(ShakudoKit.flatLook(p).scale(1.4));
        MagicPropEntity s = ShakudoKit.spawn(p, PropKind.SCALE_2, at, p.getYRot(), ShakudoKit.scaleFor(p, 1.0f), ShakudoPropKit.lifeFor(p, SHIELD_TICKS), 0, null);
        if (s == null) { GrimoireBook.fail(p, "The scales would not take form."); return false; }
        b.castCircle(p, 1.0f);
        return true;
    }

    /** The Shedding Tide: a wave of shed scales cuts everything within 6 blocks and throws it back; the caster sheds every harmful effect and heals 15 percent. */
    static boolean tide(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float k = ShakudoKit.k(i, p);
        Vec3 c = p.position();
        int n = 0;
        for (LivingEntity t : GrimoireBook.around(p, c, 6.0)) {
            if (n++ >= 16) break;
            b.hurt(i, p, t, mode, 12f * k);
            Vec3 out = t.position().subtract(c).multiply(1, 0, 1);
            if (out.lengthSqr() > 1e-4) {
                t.setDeltaMovement(t.getDeltaMovement().add(out.normalize().scale(BalanceLaw.isBoss(t) ? 0.2 : 0.9)).add(0, 0.25, 0));
                t.hurtMarked = true;
            }
        }
        List<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> bad = new ArrayList<>();
        for (MobEffectInstance e : p.getActiveEffects()) if (!e.getEffect().value().isBeneficial()) bad.add(e.getEffect());
        for (var e : bad) p.removeEffect(e);
        BalanceLaw.heal(p, p.getMaxHealth() * 0.15f);
        b.castCircle(p, 1.6f);
        b.vfx(p, VfxShape.SCALE_FX2, c, c.add(0, 1, 0), 36, 2.0f);
        b.vfx(p, VfxShape.SCALE_FX3, c.add(0, 0.1, 0), c.add(0, 1, 0), 28, 2.0f);
        return true;
    }
}
