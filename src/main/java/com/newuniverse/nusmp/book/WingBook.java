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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;

import static com.newuniverse.nusmp.book.BookPage.*;

/**
 * 1.93.0 Wing Magic, rebuilt. The wiki could not be reached while this was written (docs/attributes/wing_rebuild.md): the spells are the nearest honest versions of
 * what Wing Magic is (wings and feathers as weapons, flight and lift), named after it. Page ids and order are those every Wing grimoire has (attribute_bolt,
 * attribute_cut, attribute_field, attribute_domain, attribute_guard); three pages are appended.
 * <pre>
 *   0 attribute_bolt    Feather Shot     starter  three feathers in a fan: 6 damage each in a 1 block burst, a short levitation
 *   1 attribute_cut     Wing Slash       mid      a slash of the wing 10 blocks long: 11 damage, the foes are thrown up
 *   2 attribute_field   Updraft          zone     6 s of rising wind at the aim point: foes float up (shortened on bosses), allies fall slowly
 *   3 attribute_domain  Skyward          signature  you are flung into the sky on your wings, glide for 8 s and two feather hawks hunt for you (20 s); 5 s armour
 *   4 attribute_guard   Wing Armour      mid      9 s of winged armour: slow falling, no fall damage, speed I, jump boost, the empower bonus, the armour set
 *   5 wing_dash         Wing Dash        mid      a beat of the wings carries you 12 blocks in a line, 7 damage, a push
 *   6 feather_storm     Feather Storm    zone     four feather blades orbit you for 15 s and fly at foes
 *   7 hawk_strike       Hawk Strike      mid      a feather hawk dives at the foe you look at (control free: damage and a push only)
 * </pre>
 */
public final class WingBook extends ElementBook {
    private static final String LIVE = "nusmp_wing_until";
    private static final int COLOR = 0xFFE6E2F4;

    private WingBook(List<BookPage> pages) { super(MagicType.WING, COLOR, TensuraDamageTypes.WIND_ELEMENTAL, pages); }

    private static final Rider LIFT = ElementBook.effect(() -> new MobEffectInstance(MobEffects.LEVITATION, 25, 0));

    public static WingBook create() {
        return new WingBook(List.of(
                starter("attribute_bolt", "Feather Shot", ElementBook.volley(3, 6f, 1.8, false, VfxShape.WING_FX1, LIFT)),
                mid("attribute_cut", "Wing Slash", ElementBook.line(11f, 10, 1.0, VfxShape.WING_FX1, ElementBook.lift(0.7))),
                zone("attribute_field", "Updraft", WingBook::updraft),
                signature("attribute_domain", "Skyward", WingBook::skyward).withCooldown(900),
                mid("attribute_guard", "Wing Armour", WingBook::armour),
                mid("wing_dash", "Wing Dash", ElementBook.dash(7f, 12, false, VfxShape.WING_FX1, ElementBook.knock(0.8))),
                zone("feather_storm", "Feather Storm", WingBook::storm),
                mid("hawk_strike", "Hawk Strike", WingBook::hawk)));
    }

    @Override
    public void vfx(ServerPlayer p, VfxShape shape, Vec3 from, Vec3 to, int ticks, float power) {
        VfxShape mine = switch (shape) {
            case WIKI_MAGIC_CAST -> VfxShape.WING_FX1;
            case WIKI_MAGIC_FIELD -> VfxShape.WING_FX2;
            case WIKI_MAGIC_BURST -> VfxShape.WING_FX3;
            default -> shape;
        };
        super.vfx(p, mine, from, to, ticks, power);
    }

    @Override
    public void castCircle(ServerPlayer p, float power) {
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.WING_FX2, p, p.position().add(0, 0.05, 0), color, 22, Math.max(0.6f, power) * 0.8f);
    }

    @Override
    public void impact(ServerPlayer p, Vec3 at, float power) { vfx(p, VfxShape.WING_FX3, at, at.add(0, 1, 0), 20, power * 0.4f); }

    // ------------------------------------------------------------------ 2 Updraft
    private static boolean updraft(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 22);
        float s = GrimoireBook.size(i, p);
        double r = 4.5 * s;
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.WING_FX2, c.add(0, 0.05, 0), c.add(0, 1, 0), 120, (float) r / 1.5f);
        boolean hold = BalanceLaw.beginControl(p);
        SpellRuntime.zone(p.serverLevel(), 120, 10, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (hold) t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, BalanceLaw.controlTicks(t, 20), 0));
                if (age % 20 == 0) b.hurt(i, p, t, mode, 2f);
            }
            for (ServerPlayer a : p.serverLevel().getEntitiesOfClass(ServerPlayer.class, new net.minecraft.world.phys.AABB(c, c).inflate(r), a -> (a == p || a.isAlliedTo(p)) && a.distanceToSqr(c) <= r * r))
                a.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false));
        });
        return true;
    }

    // ------------------------------------------------------------------ 3 Skyward (signature)
    private static boolean skyward(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.3f);
        p.setDeltaMovement(p.getDeltaMovement().add(0, 1.5, 0));
        p.hurtMarked = true;
        p.fallDistance = 0;
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0));
        p.getPersistentData().putLong(LIVE, p.level().getGameTime() + 160);
        int life = AirPropKit.lifeFor(p, 400);
        for (int k = 0; k < 2; k++)
            MagicProps.spawn(p.serverLevel(), PropKind.WING_1, p.position().add(k == 0 ? 1.5 : -1.5, 2.0, 0), p.getYRot(), 1.0f, life, AirPropKit.pack(0, 0, 0), p);
        PlayerAuras.set(p, Aura.WING, 100);
        b.vfx(p, VfxShape.WING_FX3, p.position(), p.position().add(0, 1, 0), 28, 0.7f);
        return true;
    }

    // ------------------------------------------------------------------ 4 Wing Armour
    private static boolean armour(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 180;
        var tag = i.getOrCreateTag();
        tag.putLong("EmpowerUntil", p.level().getGameTime() + ticks);
        tag.putFloat("EmpowerBonus", 4f);
        tag.putLong("ThornsUntil", p.level().getGameTime() + ticks);
        p.getPersistentData().putLong(LIVE, p.level().getGameTime() + ticks);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 0));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP, ticks, 1));
        p.addEffect(new MobEffectInstance(b.magic.guardEffect(), ticks, 0, false, false));
        PlayerAuras.set(p, Aura.WING, ticks);
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.WING_FX3, p, p.position().add(0, 1, 0), b.color, 26, 0.5f);
        return true;
    }

    /** While the wings are on, falls cost nothing (hook registered by WingProps.init). */
    public static void featherFall(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (p.getPersistentData().getLong(LIVE) <= p.level().getGameTime()) return;
        if (e.getSource().is(DamageTypeTags.IS_FALL)) e.setCanceled(true);
    }

    // ------------------------------------------------------------------ 6 Feather Storm
    private static boolean storm(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1f);
        int life = AirPropKit.lifeFor(p, 300);
        for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 2;
            MagicProps.spawn(p.serverLevel(), PropKind.WING_1, p.position().add(Math.cos(a) * 1.7, 1.4, Math.sin(a) * 1.7), p.getYRot(), 1.0f, life, AirPropKit.pack(1, 0, 0), p);
        }
        b.vfx(p, VfxShape.WING_FX3, p.position(), p.position().add(0, 1, 0), 24, 0.5f);
        return true;
    }

    // ------------------------------------------------------------------ 7 Hawk Strike
    private static boolean hawk(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 24);
        if (t == null || t.isAlliedTo(p)) { GrimoireBook.fail(p, "No one to dive at."); return false; }
        b.castCircle(p, 0.8f);
        MagicPropEntity e = MagicProps.spawn(p.serverLevel(), PropKind.WING_1, p.position().add(0, 2.4, 0), p.getYRot(), 1.2f * GrimoireBook.size(i, p), 90, AirPropKit.pack(0, 0, 0), p);
        if (e == null) return false;
        e.setTarget(t);
        return true;
    }
}
