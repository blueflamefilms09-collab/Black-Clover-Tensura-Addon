package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.prop.AirPropKit;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.prop.VortexProps;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.newuniverse.nusmp.book.BookPage.*;

/**
 * 1.93.0 Vortex Magic, rebuilt. The wiki could not be reached while this was written (docs/attributes/vortex_rebuild.md): the spells are the nearest honest versions
 * of what Vortex Magic is (spinning wind that draws things in), named after it. Page ids and order are those every Vortex grimoire has (attribute_bolt,
 * attribute_cut, attribute_field, attribute_domain, attribute_guard); three pages are appended.
 * <pre>
 *   0 attribute_bolt    Vortex Shot      starter  a spinning bolt: 9 damage in a 1.5 block burst that pulls everything in the burst towards its middle
 *   1 attribute_cut     Spiral Lance     mid      a lance of wind 11 blocks long: 11 damage, everything on the line is pulled towards you
 *   2 attribute_field   Whirlpool        zone     6 s of whirlwind at the aim point: foes are drawn to its middle, slowed and cut a little
 *   3 attribute_domain  Great Vortex     signature  a cyclone (VORTEX_1 variant 1) walks 8 s along your aim pulling in and lifting everything; 5 s armour
 *   4 attribute_guard   Vortex Mantle    mid      9 s of whirling mantle: enemy projectiles within 3.5 blocks are swept away, speed I, the empower bonus, the armour set
 *   5 vortex_step       Vortex Step      mid      you spin forward up to 10 blocks, leaving a pull behind you
 *   6 maelstrom_core    Maelstrom Core   zone     a core of wind hangs where you aim for 8 s, drawing in everything within 8 blocks, and implodes
 *   7 spiral_gale       Spiral Gale      zone     a spiral gale in a cone: damage, a push, a lift
 * </pre>
 * Pulls are changes of velocity, never teleports; bosses are pulled at a third of the strength.
 */
public final class VortexBook extends ElementBook {
    private static final String LIVE = "nusmp_vortex_until";
    private static final int COLOR = 0xFF59B8C9;

    private VortexBook(List<BookPage> pages) { super(MagicType.VORTEX, COLOR, TensuraDamageTypes.WIND_ELEMENTAL, pages); }

    /** Draws the target towards the caster. */
    private static final Rider PULL = (t, p) -> VortexProps.pull(t, p.position().add(0, 1, 0), 0.9, 0.05);

    public static VortexBook create() {
        return new VortexBook(List.of(
                starter("attribute_bolt", "Vortex Shot", VortexBook::shot),
                mid("attribute_cut", "Spiral Lance", ElementBook.line(11f, 11, 0.9, VfxShape.VORTEX_FX1, PULL)),
                zone("attribute_field", "Whirlpool", VortexBook::whirlpool),
                signature("attribute_domain", "Great Vortex", VortexBook::cyclone).withCooldown(900),
                mid("attribute_guard", "Vortex Mantle", VortexBook::mantleCast),
                mid("vortex_step", "Vortex Step", ElementBook.dash(6f, 10, false, VfxShape.VORTEX_FX1, PULL)),
                zone("maelstrom_core", "Maelstrom Core", VortexBook::core),
                zone("spiral_gale", "Spiral Gale", ElementBook.cone(10f, 9.0, 0.35, VfxShape.VORTEX_FX1, ElementBook.all(ElementBook.knock(1.1), ElementBook.lift(0.4))))));
    }

    @Override
    public void vfx(ServerPlayer p, VfxShape shape, Vec3 from, Vec3 to, int ticks, float power) {
        VfxShape mine = switch (shape) {
            case WIKI_MAGIC_CAST -> VfxShape.VORTEX_FX1;
            case WIKI_MAGIC_FIELD -> VfxShape.VORTEX_FX2;
            case WIKI_MAGIC_BURST -> VfxShape.VORTEX_FX3;
            default -> shape;
        };
        super.vfx(p, mine, from, to, ticks, power);
    }

    @Override
    public void castCircle(ServerPlayer p, float power) {
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.VORTEX_FX2, p, p.position().add(0, 0.05, 0), color, 22, Math.max(0.6f, power) * 0.8f);
    }

    @Override
    public void impact(ServerPlayer p, Vec3 at, float power) { vfx(p, VfxShape.VORTEX_FX3, at, at.add(0, 1, 0), 20, power * 0.4f); }

    // ------------------------------------------------------------------ 0 Vortex Shot
    private static boolean shot(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 0.7f);
        b.vfx(p, VfxShape.VORTEX_FX1, start, start.add(dir.scale(1.9 * 24)), 24, 0.34f * s);
        SpellRuntime.bolt(p, start, dir.scale(1.9), 0.5 * s, 24, false, null, (bolt, t) -> { }, (bolt, at) -> {
            for (LivingEntity t : GrimoireBook.around(p, at, 2.4 * s)) {
                if (t.position().distanceToSqr(at) <= 1.5 * 1.5 * s * s) b.hurt(i, p, t, mode, 9f);
                VortexProps.pull(t, at.add(0, 0.5, 0), 0.8, 0.0);
            }
            b.vfx(p, VfxShape.VORTEX_FX3, at, at.add(0, 1, 0), 22, 0.4f * s);
        });
        return true;
    }

    // ------------------------------------------------------------------ 2 Whirlpool
    private static boolean whirlpool(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 22);
        float s = GrimoireBook.size(i, p);
        double r = 5.0 * s;
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.VORTEX_FX2, c.add(0, 0.05, 0), c.add(0, 1, 0), 120, (float) r / 1.5f);
        SpellRuntime.zone(p.serverLevel(), 120, 4, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                VortexProps.pull(t, c.add(0, 0.6, 0), 0.22, 0.0);
                if (age % 20 == 0) {
                    b.hurt(i, p, t, mode, 2f);
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0));
                }
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ 3 Great Vortex
    private static boolean cyclone(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.3f);
        Vec3 look = p.getViewVector(1f).multiply(1, 0, 1).normalize();
        Vec3 at = p.position().add(look.scale(3.0));
        MagicProps.spawn(p.serverLevel(), PropKind.VORTEX_1, at, p.getYRot(), 1.0f * GrimoireBook.size(i, p), AirPropKit.lifeFor(p, 160), AirPropKit.pack(1, 0, 0), p);
        b.vfx(p, VfxShape.VORTEX_FX3, at, at.add(0, 1, 0), 26, 0.7f);
        return true;
    }

    // ------------------------------------------------------------------ 4 Vortex Mantle
    private static boolean mantleCast(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 180;
        var tag = i.getOrCreateTag();
        tag.putLong("EmpowerUntil", p.level().getGameTime() + ticks);
        tag.putFloat("EmpowerBonus", 4f);
        tag.putLong("ThornsUntil", p.level().getGameTime() + ticks);
        p.getPersistentData().putLong(LIVE, p.level().getGameTime() + ticks);
        p.addEffect(new MobEffectInstance(b.magic.guardEffect(), ticks, 0, false, false));
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.VORTEX_FX3, p, p.position().add(0, 1, 0), b.color, 26, 0.5f);
        return true;
    }

    /** While the mantle turns, enemy projectiles that come within 3.5 blocks are swept away (hook registered by VortexProps.init). */
    public static void mantle(ServerPlayer p) {
        if (p.getPersistentData().getLong(LIVE) <= p.level().getGameTime() || p.tickCount % 2 != 0) return;
        for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(3.5))) {
            if (pr.getOwner() == p || (pr.getOwner() instanceof LivingEntity o && o.isAlliedTo(p))) continue;
            pr.discard();
            VfxSpawn.send(p.serverLevel(), VfxShape.VORTEX_FX3, pr.position(), pr.position().add(0, 0.5, 0), COLOR, 12, 0.2f);
        }
    }

    // ------------------------------------------------------------------ 6 Maelstrom Core
    private static boolean core(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 20).add(0, 1.2, 0);
        b.castCircle(p, 0.9f);
        MagicPropEntity e = MagicProps.spawn(p.serverLevel(), PropKind.VORTEX_1, c, p.getYRot(), 1.0f * GrimoireBook.size(i, p), AirPropKit.lifeFor(p, 160), AirPropKit.pack(0, 0, 0), p);
        return e != null;
    }
}
