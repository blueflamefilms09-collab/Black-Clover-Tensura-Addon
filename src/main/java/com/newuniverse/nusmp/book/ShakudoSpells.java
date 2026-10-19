package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.prop.ShakudoPropKit;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 1.93.0 Shakudo Magic: the pages appended after the five generic ones (their ids, order and mastery bits are untouched; see ExclusiveExt). Shakudo is the
 * black-purple alloy of copper and a little gold with fine gold inlay; the repo's wiki extract names Cesc as its user, the rest of the book is the nearest honest
 * version of a metal-construct magic (see docs/attributes/shakudo_rebuild.md). Pages (appended modes 5 to 9):
 * <ul>
 *   <li>{@code shakudo_lance} Gilded Inlay Lance (starter): a gold-inlaid lance thrust 14 blocks, pierces everything on the line, staggers what it hits;</li>
 *   <li>{@code shakudo_golem} Shakudo Golem (zone tier): a 2.3 block alloy golem fights for 25 s (longer with EP), one at a time;</li>
 *   <li>{@code shakudo_bulwark} Shakudo Bulwark (mid): a solid slab that stops every projectile and blocks the way for 10 s, one at a time;</li>
 *   <li>{@code shakudo_plate_armor} Shakudo Plate Armor (mid): the temporary armour, 30 percent of every blow softened, melee attackers staggered, Resistance II, 20 s;</li>
 *   <li>{@code shakudo_colossus} Shakudo Colossus (signature, 120 s): the golem at 1.7 times the size with a heavier fist for 20 s.</li>
 * </ul>
 */
public final class ShakudoSpells {
    private ShakudoSpells() {}

    public static final int GOLEM_TICKS = 500, BULWARK_TICKS = 200, ARMOUR_TICKS = 400, COLOSSUS_TICKS = 400;

    public static List<BookPage> pages() {
        return List.of(
                BookPage.starter("shakudo_lance", "Gilded Inlay Lance", ShakudoSpells::lance),
                BookPage.zone("shakudo_golem", "Shakudo Golem", (b, i, p, m) -> golem(b, i, p, 0.9f, GOLEM_TICKS)),
                BookPage.mid("shakudo_bulwark", "Shakudo Bulwark", ShakudoSpells::bulwark),
                BookPage.mid("shakudo_plate_armor", "Shakudo Plate Armor",
                        (b, i, p, m) -> ShakudoKit.guard(b, p, MagicType.SHAKUDO, ARMOUR_TICKS, 0.30f, 2.5f + 0.4f * ShakudoKit.mighty(p), 1)),
                BookPage.signature("shakudo_colossus", "Shakudo Colossus", (b, i, p, m) -> golem(b, i, p, 1.7f, COLOSSUS_TICKS)).withCooldown(2400));
    }

    /** Gilded Inlay Lance: everything on a 14 block line, 1.2 wide, takes the hit and is staggered; the line stops at a wall. */
    static boolean lance(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 from = p.getEyePosition().add(0, -0.2, 0), end = GrimoireBook.aim(p, 14);
        Vec3 dir = end.subtract(from);
        if (dir.lengthSqr() < 0.25) return false;
        float k = ShakudoKit.k(i, p);
        int n = 0;
        for (LivingEntity t : GrimoireBook.along(p, from, end, 1.2)) {
            if (n++ >= 12) break;
            b.hurt(i, p, t, mode, 11f * k);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.isBoss(t) ? 20 : 50, 1));
            Vec3 push = dir.multiply(1, 0, 1);
            if (push.lengthSqr() > 1e-4) {
                t.setDeltaMovement(t.getDeltaMovement().add(push.normalize().scale(BalanceLaw.isBoss(t) ? 0.1 : 0.5)).add(0, 0.1, 0));
                t.hurtMarked = true;
            }
            VfxSpawn.send(p.serverLevel(), VfxShape.SHAKUDO_FX3, t.getBoundingBox().getCenter(), t.getBoundingBox().getCenter().add(0, 1, 0), b.color, 16, 0.6f);
        }
        b.castCircle(p, 0.9f);
        b.vfx(p, VfxShape.SHAKUDO_FX1, from, end, 16, 0.9f * k);
        return true;
    }

    /** The golem (scale 0.9) or the colossus (1.7): follows the caster, fights the nearest hostile; a new cast replaces the old one. */
    static boolean golem(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, float base, int ticks) {
        ServerLevel level = p.serverLevel();
        Vec3 look = ShakudoKit.flatLook(p), at = p.position().add(look.scale(2.2 * base));
        ShakudoKit.dismiss(p, PropKind.SHAKUDO_1);
        MagicPropEntity g = ShakudoKit.spawn(p, PropKind.SHAKUDO_1, at, p.getYRot(), ShakudoKit.scaleFor(p, base), ShakudoPropKit.lifeFor(p, ticks), 0, null);
        if (g == null) { GrimoireBook.fail(p, "The alloy would not take form."); return false; }
        b.castCircle(p, 1.2f);
        VfxSpawn.send(level, VfxShape.SHAKUDO_FX2, at, at.add(0, 1, 0), b.color, 30, 2.2f * base);
        return true;
    }

    /** The bulwark: a solid slab 3 blocks in front of the caster, facing back at them. */
    static boolean bulwark(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 look = ShakudoKit.flatLook(p), at = p.position().add(look.scale(3.0));
        ShakudoKit.dismiss(p, PropKind.SHAKUDO_2);
        MagicPropEntity w = ShakudoKit.spawn(p, PropKind.SHAKUDO_2, at, p.getYRot() + 180f, ShakudoKit.scaleFor(p, 1.0f), ShakudoPropKit.lifeFor(p, BULWARK_TICKS), 0, null);
        if (w == null) { GrimoireBook.fail(p, "The alloy would not take form."); return false; }
        b.castCircle(p, 1.0f);
        VfxSpawn.send(p.serverLevel(), VfxShape.SHAKUDO_FX2, at, at.add(0, 1, 0), b.color, 24, 1.6f);
        return true;
    }
}
