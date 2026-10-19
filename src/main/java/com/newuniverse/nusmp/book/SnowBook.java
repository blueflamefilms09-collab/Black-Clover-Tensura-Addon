package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.prop.AirPropKit;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;

import static com.newuniverse.nusmp.book.BookPage.*;

/**
 * 1.93.0 Snow Magic, rebuilt. The wiki could not be reached while this was written (docs/attributes/snow_rebuild.md): the spells are the nearest honest versions of
 * what Snow Magic is (snow and cold shaped into bullets, blades, storms and soldiers), named after it. Page ids and order are the ones every Snow grimoire has
 * (attribute_bolt, attribute_cut, attribute_field, attribute_domain, attribute_guard); three pages are appended.
 * <pre>
 *   0 attribute_bolt    Snow Bullet      starter  a packed snowball: 9 damage in a 1.3 block burst, slow II and frostbite
 *   1 attribute_cut     Snow Crescent    mid      a crescent of hard snow cut 9 blocks ahead: 10 damage, slow and frostbite along the line
 *   2 attribute_field   Blizzard         zone     6 s of blizzard at the aim point: slow, frostbite, 2 damage every 0.8 s, foes are pushed outwards
 *   3 attribute_domain  Snow Golem       signature  a golem of snow (SNOW_1 variant 0) fights for you for 20 s (more with EP); 5 s snow armour
 *   4 attribute_guard   Snow Armour      mid      9 s of snow mantle: damage resistance, attackers are chilled, the empower bonus and the armour set
 *   5 snow_drift        Snow Drift       mid      you slide on a drift up to 10 blocks, freezing the path
 *   6 frost_crystal     Frost Crystal    zone     an ice crystal grows at the aim point for 10 s and pulses frost on all within 4.5 blocks
 *   7 whiteout          Whiteout         zone     a ring of white-out around you: blind, slow, 8 damage
 * </pre>
 */
public final class SnowBook extends ElementBook {
    private static final String LIVE = "nusmp_snow_until";
    private static final int COLOR = 0xFFE7F7FF;

    private SnowBook(List<BookPage> pages) { super(MagicType.SNOW, COLOR, TensuraDamageTypes.ICE_ELEMENTAL, pages); }

    private static void frost(LivingEntity t, int ticks, int amp) {
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(t, ticks), amp));
        t.setTicksFrozen(Math.min(t.getTicksRequiredToFreeze() + 60, t.getTicksFrozen() + 80));
    }

    private static final Rider CHILL = (t, p) -> frost(t, 70, 1);

    public static SnowBook create() {
        return new SnowBook(List.of(
                starter("attribute_bolt", "Snow Bullet", ElementBook.bolt(9f, 1.8, 0.55, 26, false, 1.3, VfxShape.SNOW_FX1, VfxShape.SNOW_FX3, CHILL)),
                mid("attribute_cut", "Snow Crescent", ElementBook.line(10f, 9, 0.9, VfxShape.SNOW_FX1, CHILL)),
                zone("attribute_field", "Blizzard", SnowBook::blizzard),
                signature("attribute_domain", "Snow Golem", SnowBook::golem).withCooldown(900),
                mid("attribute_guard", "Snow Armour", SnowBook::armour),
                mid("snow_drift", "Snow Drift", ElementBook.dash(5f, 10, false, VfxShape.SNOW_FX1, CHILL)),
                zone("frost_crystal", "Frost Crystal", SnowBook::crystal),
                zone("whiteout", "Whiteout", SnowBook::whiteout)));
    }

    // ------------------------------------------------------------------ the effects of this book
    @Override
    public void vfx(ServerPlayer p, VfxShape shape, Vec3 from, Vec3 to, int ticks, float power) {
        VfxShape mine = switch (shape) {
            case WIKI_MAGIC_CAST -> VfxShape.SNOW_FX1;
            case WIKI_MAGIC_FIELD -> VfxShape.SNOW_FX2;
            case WIKI_MAGIC_BURST -> VfxShape.SNOW_FX3;
            default -> shape;
        };
        super.vfx(p, mine, from, to, ticks, power);
    }

    @Override
    public void castCircle(ServerPlayer p, float power) {
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.SNOW_FX2, p, p.position().add(0, 0.05, 0), color, 22, Math.max(0.6f, power) * 0.8f);
    }

    @Override
    public void impact(ServerPlayer p, Vec3 at, float power) { vfx(p, VfxShape.SNOW_FX3, at, at.add(0, 1, 0), 20, power * 0.4f); }

    // ------------------------------------------------------------------ 2 Blizzard
    private static boolean blizzard(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 22);
        float s = GrimoireBook.size(i, p);
        double r = 4.8 * s;
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.SNOW_FX2, c.add(0, 0.05, 0), c.add(0, 1, 0), 120, (float) r / 1.5f);
        SpellRuntime.zone(p.serverLevel(), 120, 16, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                frost(t, 40, 1);
                b.hurt(i, p, t, mode, 2f);
                Vec3 push = t.position().subtract(c).multiply(1, 0, 1);
                if (push.lengthSqr() > 0.01) {
                    Vec3 v = push.normalize().scale(0.18);
                    t.setDeltaMovement(t.getDeltaMovement().add(v.x, 0.02, v.z));
                    t.hurtMarked = true;
                }
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ 3 Snow Golem
    private static boolean golem(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.3f);
        Vec3 look = p.getViewVector(1f).multiply(1, 0, 1).normalize();
        Vec3 at = p.position().add(look.scale(2.4));
        MagicProps.spawn(p.serverLevel(), PropKind.SNOW_1, at, p.getYRot() + 180f, 1.0f, AirPropKit.lifeFor(p, 400), AirPropKit.pack(0, 0, 0), p);
        PlayerAuras.set(p, Aura.SNOW, 100);
        b.vfx(p, VfxShape.SNOW_FX3, at, at.add(0, 1, 0), 26, 0.7f);
        return true;
    }

    // ------------------------------------------------------------------ 4 Snow Armour
    private static boolean armour(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 180;
        var tag = i.getOrCreateTag();
        tag.putLong("EmpowerUntil", p.level().getGameTime() + ticks);
        tag.putFloat("EmpowerBonus", 4f);
        tag.putLong("ThornsUntil", p.level().getGameTime() + ticks);
        p.getPersistentData().putLong(LIVE, p.level().getGameTime() + ticks);
        p.addEffect(new MobEffectInstance(b.magic.guardEffect(), ticks, 0));
        PlayerAuras.set(p, Aura.SNOW, ticks);
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.SNOW_FX3, p, p.position().add(0, 1, 0), b.color, 26, 0.5f);
        return true;
    }

    /** While the mantle is on, whatever strikes the wearer in melee is chilled (hook registered by SnowProps.init). */
    public static void frostThorns(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (p.getPersistentData().getLong(LIVE) <= p.level().getGameTime()) return;
        if (e.getSource().getEntity() instanceof LivingEntity a && a != p && !a.isAlliedTo(p) && e.getSource().getDirectEntity() == a) frost(a, 50, 0);
    }

    // ------------------------------------------------------------------ 6 Frost Crystal
    private static boolean crystal(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 20);
        b.castCircle(p, 0.9f);
        MagicPropEntity e = MagicProps.spawn(p.serverLevel(), PropKind.SNOW_1, c, p.getYRot(), 1.0f * GrimoireBook.size(i, p), AirPropKit.lifeFor(p, 200), AirPropKit.pack(1, 0, 0), p);
        return e != null;
    }

    // ------------------------------------------------------------------ 7 Whiteout
    private static boolean whiteout(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 7 * GrimoireBook.size(i, p);
        b.castCircle(p, 1.2f);
        b.vfx(p, VfxShape.SNOW_FX3, p.position(), p.position().add(0, 1, 0), 28, (float) r / 6f);
        b.vfx(p, VfxShape.SNOW_FX2, p.position().add(0, 0.05, 0), p.position().add(0, 1, 0), 50, (float) r / 1.5f);
        for (LivingEntity t : GrimoireBook.around(p, p.position(), r)) {
            b.hurt(i, p, t, mode, 8f);
            t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BalanceLaw.controlTicks(t, 50), 0));
            frost(t, 80, 2);
        }
        return true;
    }
}
