package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.93.0 Hair Magic: the pages appended after the five generic ones. Hair is the body magic of hair grown, hardened, wound and thrown; the repo's wiki
 * extract has no page for it, so the book is the nearest honest version (see docs/attributes/hair_rebuild.md). Pages (appended modes 5 to 9):
 * <ul>
 *   <li>{@code hair_lash} Hair Lash (starter): a tendril rises under the foe in the crosshair, lashes it and holds it for a moment;</li>
 *   <li>{@code hair_bind} Hair Bind (mid, behind the control gate): the same tendril, kept for 4 s: the foe is held (shortened for bosses);</li>
 *   <li>{@code hair_cocoon} Hair Cocoon (mid): an egg of hair round the caster for 7 s: absorption hearts, projectiles stopped, 1.5 hit points a second and a burst of healing when it opens;</li>
 *   <li>{@code hair_weave} Hair Weave (mid): the temporary armour, 20 percent of every blow softened, attackers bound by strands (Slowness II), a regeneration pulse, 20 s;</li>
 *   <li>{@code hair_mane} Mane of a Hundred Strands (signature, 100 s): for 5 s, every second a tendril lashes each of the three nearest foes in 7 blocks and some of their magicules flow to the caster.</li>
 * </ul>
 */
public final class HairSpells {
    private HairSpells() {}

    public static final int LASH_TICKS = 28, BIND_TICKS = 80, COCOON_TICKS = 140, ARMOUR_TICKS = 400, MANE_TICKS = 100;

    public static List<BookPage> pages() {
        return List.of(
                BookPage.starter("hair_lash", "Hair Lash", HairSpells::lash),
                BookPage.mid("hair_bind", "Hair Bind", HairSpells::bind),
                BookPage.mid("hair_cocoon", "Hair Cocoon", HairSpells::cocoon),
                BookPage.mid("hair_weave", "Hair Weave", (b, i, p, m) -> ShakudoKit.guard(b, p, MagicType.HAIR, ARMOUR_TICKS, 0.20f, 0f, 0)),
                BookPage.signature("hair_mane", "Mane of a Hundred Strands", HairSpells::mane).withCooldown(1800));
    }

    private static MagicPropEntity tendril(ServerPlayer p, LivingEntity t, int life, float scale) {
        return ShakudoKit.spawn(p, PropKind.HAIR_1, t.position(), p.getYRot(), scale, life, 0, t);
    }

    static boolean lash(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 16);
        if (t == null) { GrimoireBook.fail(p, "No target."); return false; }
        if (tendril(p, t, LASH_TICKS, ShakudoKit.scaleFor(p, 1.0f) * Math.min(1.5f, ShakudoKit.k(i, p))) == null) return false;
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.HAIR_FX1, p.getEyePosition().add(0, -0.3, 0), t.getBoundingBox().getCenter(), 14, 0.8f);
        return true;
    }

    static boolean bind(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 16);
        if (t == null) { GrimoireBook.fail(p, "No target."); return false; }
        if (!GrimoireBook.control(p)) return false;
        if (tendril(p, t, BIND_TICKS, ShakudoKit.scaleFor(p, 1.1f)) == null) return false;
        b.castCircle(p, 1.0f);
        b.vfx(p, VfxShape.HAIR_FX1, p.getEyePosition().add(0, -0.3, 0), t.getBoundingBox().getCenter(), 14, 1.0f);
        b.vfx(p, VfxShape.HAIR_FX3, t.position(), t.position().add(0, 1, 0), 28, 1.2f);
        return true;
    }

    /** The cocoon: Absorption (two hearts, more for a mighty caster) and the egg that stops projectiles and mends. */
    static boolean cocoon(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ShakudoKit.dismiss(p, PropKind.HAIR_2);
        int ticks = ShakudoKit.span(COCOON_TICKS, p);
        MagicPropEntity e = ShakudoKit.spawn(p, PropKind.HAIR_2, p.position(), p.getYRot(), ShakudoKit.scaleFor(p, 1.0f), ticks, 0, null);
        if (e == null) { GrimoireBook.fail(p, "The hair would not take form."); return false; }
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, ShakudoKit.mighty(p) >= 5 ? 2 : 1));
        b.castCircle(p, 1.2f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.HAIR_FX3, p, p.position().add(0, 1, 0), b.color, 30, 1.2f);
        return true;
    }

    /** The Mane: every second for 5 s, a tendril under each of the three nearest foes and a 2 percent draw of their magicules. */
    static boolean mane(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        float s = ShakudoKit.scaleFor(p, 1.0f);
        b.castCircle(p, 1.6f);
        b.vfx(p, VfxShape.HAIR_FX2, p.position(), p.position().add(0, 1, 0), MANE_TICKS, 7.0f);
        SpellRuntime.zone(level, MANE_TICKS, 20, age -> {
            if (!p.isAlive()) return;
            List<LivingEntity> foes = new ArrayList<>(GrimoireBook.around(p, p.position(), 7.0));
            foes.sort((x, y) -> Double.compare(x.distanceToSqr(p), y.distanceToSqr(p)));
            int n = 0;
            for (LivingEntity t : foes) {
                if (n++ >= 3) break;
                tendril(p, t, 26, s);
                EnergyBridge.drain(t, p, BalanceLaw.isBoss(t) ? 0.005 : 0.02);
            }
        });
        return true;
    }
}
