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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;

import static com.newuniverse.nusmp.book.BookPage.*;

/**
 * 1.93.0 Air Magic, rebuilt, and the one dispatch from {@link WikiBooks#attribute} to the rebuilt books of the wiki magics (Smoke, Snow, Wing, Vortex and Air:
 * {@link #rebuilt}), so WikiBooks gets a single new line. Ash has its own class since 0.34 (AshBook, rebuilt in place).
 *
 * <p>The wiki could not be reached while this was written (docs/attributes/air_rebuild.md): the spells are the nearest honest versions of what Air Magic is
 * (compressed and cut air), named after it. Page ids and order are those every Air grimoire has (attribute_bolt, attribute_cut, attribute_field,
 * attribute_domain, attribute_guard); three pages are appended.
 * <pre>
 *   0 attribute_bolt    Air Bullet          starter  a fast bullet of compressed air: 8 damage in a 1.2 block burst, a hard push
 *   1 attribute_cut     Air Blade           mid      a crescent blade of air (AIR_1 variant 0) flies 12 blocks, cutting everything it passes once
 *   2 attribute_field   Dead Air            zone     5 s of thin air at the aim point: foes slow, weak and silenced (short, shortened on bosses), projectiles drop
 *   3 attribute_domain  Air Blade Storm     signature  five blades (variant 2) orbit you for 15 s and cut foes; 5 s armour
 *   4 attribute_guard   Air Cushion Armour  mid      9 s of glass-clear air armour: no fall damage, 35 percent of enemy projectiles turned aside, speed I, empower, armour set
 *   5 air_step          Air Step            mid      you burst forward up to 9 blocks and up a little
 *   6 pressure_burst    Pressure Burst      zone     a pressure orb (variant 1) swells where you aim for 1.5 s and bursts: a hard push and damage
 *   7 vacuum_seal       Vacuum Seal         zone     the air leaves one foe: it is pulled in front of you, silenced and slowed (control)
 * </pre>
 */
public final class AirBook extends ElementBook {
    private static final String LIVE = "nusmp_air_until";
    private static final int COLOR = 0xFF9FE8FF;

    private AirBook(List<BookPage> pages) { super(MagicType.AIR, COLOR, TensuraDamageTypes.WIND_ELEMENTAL, pages); }

    /** The rebuilt book of this magic, or null when the generic wiki book is still the one. */
    public static GrimoireBook rebuilt(MagicType magic) {
        return switch (magic) {
            case SMOKE -> SmokeBook.create();
            case SNOW -> SnowBook.create();
            case WING -> WingBook.create();
            case VORTEX -> VortexBook.create();
            case AIR -> create();
            default -> null;
        };
    }

    private static final Rider SHOVE = ElementBook.knock(1.1);

    public static AirBook create() {
        return new AirBook(List.of(
                starter("attribute_bolt", "Air Bullet", ElementBook.bolt(8f, 2.6, 0.4, 20, false, 1.2, VfxShape.AIR_FX1, VfxShape.AIR_FX3, SHOVE)),
                mid("attribute_cut", "Air Blade", AirBook::blade),
                zone("attribute_field", "Dead Air", AirBook::deadAir),
                signature("attribute_domain", "Air Blade Storm", AirBook::storm).withCooldown(900),
                mid("attribute_guard", "Air Cushion Armour", AirBook::armour),
                mid("air_step", "Air Step", AirBook::step),
                zone("pressure_burst", "Pressure Burst", AirBook::pressure),
                zone("vacuum_seal", "Vacuum Seal", AirBook::seal)));
    }

    @Override
    public void vfx(ServerPlayer p, VfxShape shape, Vec3 from, Vec3 to, int ticks, float power) {
        VfxShape mine = switch (shape) {
            case WIKI_MAGIC_CAST -> VfxShape.AIR_FX1;
            case WIKI_MAGIC_FIELD -> VfxShape.AIR_FX2;
            case WIKI_MAGIC_BURST -> VfxShape.AIR_FX3;
            default -> shape;
        };
        super.vfx(p, mine, from, to, ticks, power);
    }

    @Override
    public void castCircle(ServerPlayer p, float power) {
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.AIR_FX2, p, p.position().add(0, 0.05, 0), color, 22, Math.max(0.6f, power) * 0.8f);
    }

    @Override
    public void impact(ServerPlayer p, Vec3 at, float power) { vfx(p, VfxShape.AIR_FX3, at, at.add(0, 1, 0), 20, power * 0.4f); }

    // ------------------------------------------------------------------ 1 Air Blade
    private static boolean blade(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 0.8f);
        Vec3 look = p.getViewVector(1f);
        Vec3 flat = look.multiply(1, 0, 1).normalize();
        Vec3 at = p.getEyePosition().add(flat.scale(0.8)).add(0, -0.5, 0);
        MagicPropEntity e = MagicProps.spawn(p.serverLevel(), PropKind.AIR_1, at, p.getYRot(), 1.3f * GrimoireBook.size(i, p), 20, AirPropKit.pack(0, 0, 0), p);
        if (e != null) e.setXRot(p.getXRot());
        return e != null;
    }

    /** Called by the flying blade prop for every foe it passes through (once each). */
    public static void bladeCut(LivingEntity owner, MagicPropEntity blade, LivingEntity t) {
        if (!(owner.level() instanceof net.minecraft.server.level.ServerLevel sl)) return;
        AirPropKit.hit(blade, sl, owner, t, 11f * AirPropKit.powerOf(owner), TensuraDamageTypes.WIND_ELEMENTAL);
        Vec3 side = t.position().subtract(blade.position()).multiply(1, 0, 1);
        if (side.lengthSqr() > 1e-4 && !BalanceLaw.isBoss(t)) { Vec3 v = side.normalize().scale(0.5); t.setDeltaMovement(t.getDeltaMovement().add(v.x, 0.15, v.z)); t.hurtMarked = true; }
    }

    // ------------------------------------------------------------------ 2 Dead Air
    private static boolean deadAir(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 22);
        float s = GrimoireBook.size(i, p);
        double r = 4.5 * s;
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.AIR_FX2, c.add(0, 0.05, 0), c.add(0, 1, 0), 100, (float) r / 1.5f);
        boolean hold = BalanceLaw.beginControl(p);
        SpellRuntime.zone(p.serverLevel(), 100, 10, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0));
                if (hold && age % 20 == 0) EnergyBridge.effect(t, "silence", BalanceLaw.controlTicks(t, 40), 0);
                if (age % 20 == 0) b.hurt(i, p, t, mode, 2f);
            }
            SpellRuntime.dissolveBolts(p.serverLevel(), c, r, p);
            for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, new net.minecraft.world.phys.AABB(c, c).inflate(r)))
                if (pr.getOwner() != p && !(pr.getOwner() instanceof LivingEntity o && o.isAlliedTo(p))) pr.discard();
        });
        return true;
    }

    // ------------------------------------------------------------------ 3 Air Blade Storm
    private static boolean storm(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.3f);
        int life = AirPropKit.lifeFor(p, 300);
        for (int k = 0; k < 5; k++) {
            double a = k * 2 * Math.PI / 5;
            MagicProps.spawn(p.serverLevel(), PropKind.AIR_1, p.position().add(Math.cos(a) * 1.8, 1.3, Math.sin(a) * 1.8), p.getYRot(), 1.0f, life, AirPropKit.pack(2, 0, 0), p);
        }
        b.vfx(p, VfxShape.AIR_FX3, p.position(), p.position().add(0, 1, 0), 26, 0.6f);
        return true;
    }

    // ------------------------------------------------------------------ 4 Air Cushion Armour
    private static boolean armour(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 180;
        var tag = i.getOrCreateTag();
        tag.putLong("EmpowerUntil", p.level().getGameTime() + ticks);
        tag.putFloat("EmpowerBonus", 4f);
        tag.putLong("ThornsUntil", p.level().getGameTime() + ticks);
        p.getPersistentData().putLong(LIVE, p.level().getGameTime() + ticks);
        p.addEffect(new MobEffectInstance(b.magic.guardEffect(), ticks, 0, false, false));
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.AIR_FX3, p, p.position().add(0, 1, 0), b.color, 26, 0.5f);
        return true;
    }

    /** The cushion of air: falls cost nothing and 35 percent of enemy projectiles are turned aside (hook registered by AirProps.init). */
    public static void cushion(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (p.getPersistentData().getLong(LIVE) <= p.level().getGameTime()) return;
        if (e.getSource().is(DamageTypeTags.IS_FALL)) { e.setCanceled(true); return; }
        if (e.getSource().is(DamageTypeTags.IS_PROJECTILE) && p.getRandom().nextFloat() < 0.35f) {
            e.setCanceled(true);
            VfxSpawn.send(p.serverLevel(), VfxShape.AIR_FX3, p.position(), p.position().add(0, 1, 0), COLOR, 14, 0.25f);
        }
    }

    // ------------------------------------------------------------------ 5 Air Step
    private static final BookPage.Cast STEP = ElementBook.dash(5f, 9, false, VfxShape.AIR_FX1, SHOVE);

    private static boolean step(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!STEP.cast(b, i, p, mode)) return false;
        p.setDeltaMovement(p.getDeltaMovement().add(0, 0.45, 0));
        p.hurtMarked = true;
        p.fallDistance = 0;
        return true;
    }

    // ------------------------------------------------------------------ 6 Pressure Burst
    private static boolean pressure(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 18).add(0, 1.2, 0);
        b.castCircle(p, 0.9f);
        MagicPropEntity e = MagicProps.spawn(p.serverLevel(), PropKind.AIR_1, c, p.getYRot(), 1.0f * GrimoireBook.size(i, p), 30, AirPropKit.pack(1, 0, 0), p);
        return e != null;
    }

    // ------------------------------------------------------------------ 7 Vacuum Seal
    private static boolean seal(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 16);
        if (t == null || t.isAlliedTo(p)) { GrimoireBook.fail(p, "No one to seal."); return false; }
        if (!GrimoireBook.control(p)) return false;
        int ticks = BalanceLaw.controlTicks(t, 80);
        b.castCircle(p, 0.9f);
        Vec3 front = p.position().add(p.getViewVector(1f).multiply(1, 0, 1).normalize().scale(2.0));
        Vec3 pull = front.subtract(t.position());
        if (pull.lengthSqr() > 1e-4 && !BalanceLaw.isBoss(t)) {
            Vec3 v = pull.normalize().scale(Math.min(1.4, pull.length() * 0.3));
            t.setDeltaMovement(v.x, 0.2, v.z);
            t.hurtMarked = true;
        }
        EnergyBridge.effect(t, "silence", ticks, 0);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 2));
        b.hurt(i, p, t, mode, 6f);
        b.vfx(p, VfxShape.AIR_FX2, t.position().add(0, 0.05, 0), t.position().add(0, 1, 0), ticks, 1.3f);
        return true;
    }
}
