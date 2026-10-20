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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;

import static com.newuniverse.nusmp.book.BookPage.*;

/**
 * 1.93.0 Smoke Magic, rebuilt. The wiki could not be reached while this was written (see docs/attributes/smoke_rebuild.md): the spells are the nearest honest
 * versions of what Smoke Magic is in the story (a caster who hides, blinds and misleads in a cloud of his own smoke), named after it. Page ids and their order
 * are the ones every Smoke grimoire already has (attribute_bolt, attribute_cut, attribute_field, attribute_domain, attribute_guard); the rebuilt pages keep
 * those ids and tiers, three pages are appended.
 * <pre>
 *   0 attribute_bolt    Smoke Bullet      starter  a bolt of dense smoke: bursts into a cloud that blinds and slows what stands in it for 2.5 s
 *   1 attribute_cut     Smoke Serpent     mid      a serpent of smoke lunges 10 blocks along your aim, biting everything it passes through (SMOKE_1 variant 1)
 *   2 attribute_field   Smoke Screen      zone     5 s of thick smoke at the aim point: foes inside are blinded and slowed and lose track of you, allies vanish in it
 *   3 attribute_domain  Smoke Clones      signature  three clones of smoke (SMOKE_1 variant 0) stand round you for 10 s, you vanish for 5 s in the smoke armour
 *   4 attribute_guard   Smoke Armour      mid      9 s of smoke armour: a 30 % chance to turn a blow into smoke, thorns, the old empower bonus, the armour set
 *   5 smoke_step        Smoke Step        mid      you turn to smoke and flow up to 11 blocks, blinding what you pass, and are unseen for 2 s
 *   6 coiling_serpent   Coiling Serpent   zone     a serpent coils round one foe (control, shortened on bosses): held and squeezed
 *   7 smoke_wave        Smoke Wave        zone     a rolling wall of smoke in a cone: damage, push, blindness
 * </pre>
 * Damage goes through GrimoireBook.hurt (the balance law, scaled by EP); every control goes through GrimoireBook.control and BalanceLaw.controlTicks.
 */
public final class SmokeBook extends ElementBook {
    private static final String LIVE = "nusmp_smoke_until";
    private static final int COLOR = 0xFF82788F;

    private SmokeBook(List<BookPage> pages) { super(MagicType.SMOKE, COLOR, TensuraDamageTypes.DARKNESS_ELEMENTAL, pages); }

    public static SmokeBook create() {
        return new SmokeBook(List.of(
                starter("attribute_bolt", "Smoke Bullet", SmokeBook::bullet),
                mid("attribute_cut", "Smoke Serpent", SmokeBook::serpentPage),
                zone("attribute_field", "Smoke Screen", SmokeBook::screen),
                signature("attribute_domain", "Smoke Clones", SmokeBook::clones).withCooldown(900),
                mid("attribute_guard", "Smoke Armour", SmokeBook::armour),
                mid("smoke_step", "Smoke Step", SmokeBook::step),
                zone("coiling_serpent", "Coiling Serpent", SmokeBook::coil),
                zone("smoke_wave", "Smoke Wave", SmokeBook::wave)));
    }

    // ------------------------------------------------------------------ the effects of this book (the shared wiki shapes become Smoke's own)
    @Override
    public void vfx(ServerPlayer p, VfxShape shape, Vec3 from, Vec3 to, int ticks, float power) {
        VfxShape mine = switch (shape) {
            case WIKI_MAGIC_CAST -> VfxShape.SMOKE_FX1;
            case WIKI_MAGIC_FIELD -> VfxShape.SMOKE_FX2;
            case WIKI_MAGIC_BURST -> VfxShape.SMOKE_FX3;
            default -> shape;
        };
        super.vfx(p, mine, from, to, ticks, power);
    }

    @Override
    public void castCircle(ServerPlayer p, float power) {
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.SMOKE_FX2, p, p.position().add(0, 0.05, 0), color, 22, Math.max(0.6f, power) * 0.8f);
    }

    @Override
    public void impact(ServerPlayer p, Vec3 at, float power) { vfx(p, VfxShape.SMOKE_FX3, at, at.add(0, 1, 0), 20, power * 0.4f); }

    private static boolean ally(ServerPlayer p, LivingEntity t) { return t == p || t.isAlliedTo(p); }

    private static void blind(LivingEntity t, int ticks) { t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BalanceLaw.controlTicks(t, ticks), 0)); }

    private static void choke(LivingEntity t, int ticks) {
        blind(t, ticks);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 0));
    }

    /** Mobs that hunt the caster lose the trail. */
    private static void loseTrail(ServerPlayer p, Vec3 c, double r) {
        for (Mob m : p.serverLevel().getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(c, c).inflate(r), m -> m.getTarget() == p)) m.setTarget(null);
    }

    // ------------------------------------------------------------------ 0 Smoke Bullet
    private static boolean bullet(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 0.7f);
        b.vfx(p, VfxShape.SMOKE_FX1, start, start.add(dir.scale(1.9 * 24)), 24, 0.34f * s);
        SpellRuntime.bolt(p, start, dir.scale(1.9), 0.55 * s, 24, false, null, (bolt, t) -> { }, (bolt, at) -> {
            for (LivingEntity t : GrimoireBook.around(p, at, 1.7 * s)) { b.hurt(i, p, t, mode, 9f); choke(t, 60); }
            b.vfx(p, VfxShape.SMOKE_FX3, at, at.add(0, 1, 0), 22, 0.4f * s);
            Vec3 c = at;
            b.vfx(p, VfxShape.SMOKE_FX2, c, c.add(0, 1, 0), 50, 2.2f * s / 1.5f);
            SpellRuntime.zone(p.serverLevel(), 50, 10, age -> {
                for (LivingEntity t : GrimoireBook.around(p, c, 2.2 * s)) choke(t, 30);
            });
        });
        return true;
    }

    // ------------------------------------------------------------------ 1 Smoke Serpent
    private static boolean serpentPage(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 0.8f);
        Vec3 look = p.getViewVector(1f).multiply(1, 0, 1).normalize();
        Vec3 at = p.position().add(look.scale(1.2)).add(0, 0.1, 0);
        MagicPropEntity e = MagicProps.spawn(p.serverLevel(), PropKind.SMOKE_1, at, p.getYRot(), 1.0f * GrimoireBook.size(i, p), 26, AirPropKit.pack(1, 0, 0), p);
        return e != null;
    }

    /** Called by the serpent prop for every foe it goes through (once each). */
    public static void serpentBite(LivingEntity owner, MagicPropEntity serpent, LivingEntity t) {
        if (!(owner.level() instanceof net.minecraft.server.level.ServerLevel sl)) return;
        AirPropKit.hit(serpent, sl, owner, t, 10f * AirPropKit.powerOf(owner), TensuraDamageTypes.DARKNESS_ELEMENTAL);
        blind(t, 50);
    }

    // ------------------------------------------------------------------ 2 Smoke Screen
    private static boolean screen(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 22);
        float s = GrimoireBook.size(i, p);
        double r = 4.5 * s;
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.SMOKE_FX2, c.add(0, 0.05, 0), c.add(0, 1, 0), 100, (float) r / 1.5f);
        SpellRuntime.zone(p.serverLevel(), 100, 10, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                choke(t, 30);
                if (age % 20 == 0) b.hurt(i, p, t, mode, 2f);
            }
            loseTrail(p, c, r + 6);
            for (Player a : p.serverLevel().getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(c, c).inflate(r), a -> ally(p, a) && a.distanceToSqr(c) <= r * r))
                a.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, false, false));
        });
        return true;
    }

    // ------------------------------------------------------------------ 3 Smoke Clones (signature)
    private static boolean clones(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.3f);
        int life = AirPropKit.lifeFor(p, 200);
        int n = 3;
        for (int k = 0; k < n; k++) {
            double a = Math.toRadians(p.getYRot()) + k * 2 * Math.PI / n + 0.6;
            Vec3 at = p.position().add(Math.cos(a) * 2.4, 0, Math.sin(a) * 2.4);
            MagicProps.spawn(p.serverLevel(), PropKind.SMOKE_1, at, p.getYRot() + k * 120f, 1.0f, life, AirPropKit.pack(0, 0, 0), p);
        }
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0, false, false));
        loseTrail(p, p.position(), 20);
        b.vfx(p, VfxShape.SMOKE_FX3, p.position(), p.position().add(0, 1, 0), 28, 0.6f);
        return true;
    }

    // ------------------------------------------------------------------ 4 Smoke Armour (guard)
    private static boolean armour(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 180;
        var tag = i.getOrCreateTag();
        tag.putLong("EmpowerUntil", p.level().getGameTime() + ticks);
        tag.putFloat("EmpowerBonus", 4f);
        tag.putLong("ThornsUntil", p.level().getGameTime() + ticks);
        p.getPersistentData().putLong(LIVE, p.level().getGameTime() + ticks);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 0));
        p.addEffect(new MobEffectInstance(b.magic.guardEffect(), ticks, 0, false, false));
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.SMOKE_FX3, p, p.position().add(0, 1, 0), b.color, 26, 0.5f);
        return true;
    }

    /** The armour turns 3 blows in 10 into smoke (hook registered by SmokeProps.init). */
    public static void dodge(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (p.getPersistentData().getLong(LIVE) <= p.level().getGameTime()) return;
        if (e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (p.getRandom().nextFloat() >= 0.3f) return;
        e.setCanceled(true);
        VfxSpawn.send(p.serverLevel(), VfxShape.SMOKE_FX3, p.position(), p.position().add(0, 1, 0), COLOR, 18, 0.35f);
    }

    // ------------------------------------------------------------------ 5 Smoke Step
    private static final BookPage.Cast STEP = ElementBook.dash(6f, 11, false, VfxShape.SMOKE_FX1, ElementBook.effect(() -> new MobEffectInstance(MobEffects.BLINDNESS, 50, 0)));

    private static boolean step(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!STEP.cast(b, i, p, mode)) return false;
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false));
        loseTrail(p, p.position(), 12);
        b.vfx(p, VfxShape.SMOKE_FX3, p.position(), p.position().add(0, 1, 0), 22, 0.5f);
        return true;
    }

    // ------------------------------------------------------------------ 6 Coiling Serpent
    private static boolean coil(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 14);
        if (t == null) { GrimoireBook.fail(p, "Nothing to coil round."); return false; }
        if (!GrimoireBook.control(p)) return false;
        int ticks = BalanceLaw.controlTicks(t, 60);
        b.castCircle(p, 0.9f);
        MagicPropEntity e = MagicProps.spawn(p.serverLevel(), PropKind.SMOKE_1, t.position(), t.getYRot(), Math.max(0.8f, t.getBbHeight() / 1.8f), ticks, AirPropKit.pack(2, 0, 0), p);
        if (e == null) return false;
        e.setTarget(t);
        b.hurt(i, p, t, mode, 5f);
        return true;
    }

    /** Called by the coil prop every half second. */
    public static void coilSqueeze(LivingEntity owner, MagicPropEntity coil, LivingEntity t) {
        if (!(owner.level() instanceof net.minecraft.server.level.ServerLevel sl)) return;
        AirPropKit.hit(coil, sl, owner, t, 3f * AirPropKit.powerOf(owner), TensuraDamageTypes.DARKNESS_ELEMENTAL);
    }

    // ------------------------------------------------------------------ 7 Smoke Wave
    private static final BookPage.Cast WAVE = ElementBook.cone(11f, 9.0, 0.35, VfxShape.SMOKE_FX1,
            ElementBook.all(ElementBook.knock(0.9), ElementBook.effect(() -> new MobEffectInstance(MobEffects.BLINDNESS, 60, 0))));

    private static boolean wave(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 look = p.getViewVector(1f);
        b.vfx(p, VfxShape.SMOKE_FX2, p.position().add(look.scale(3.5)), p.position().add(look.scale(3.5)).add(0, 1, 0), 40, 2.4f);
        return WAVE.cast(b, i, p, mode);
    }
}
