package com.newuniverse.nusmp.book.pages15;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.TimeStop;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.ElementBook.Rider;
import com.newuniverse.nusmp.book.pages15.Pages15Kit.Ctx;
import com.newuniverse.nusmp.book.pages15.Pages15Rules.Tier;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Batch 3 of the "15 spells for every grimoire" project: Wind, Air, Sound, Mind and Soul. 87 new pages, appended after every old page of their book (docs/magic/pages15_batch3.md has the
 * table: canon or original, tier, what each does, the worst case).
 *
 * <p>Books of this batch (the only books {@link Pages15} asks this class about; see {@link Pages15Plan}): {@code book_dream} (+11), {@code book_illusion} (+10), {@code book_air} (+7),
 * {@code book_smoke} (+7), {@code book_vortex} (+7), {@code book_wing} (+7), {@code book_wind} (+6), {@code book_sound} (+6), {@code book_memory} (+6), {@code book_permeation} (+6),
 * {@code book_soul} (+6), {@code book_soul_corpse} (+6), {@code book_song} (+2).
 *
 * <p>Most pages are the kit's standard casts ({@link Pages15Kit}) with the tier's damage; the few that need their own logic use {@link Pages15Kit#cast} and only the bounded {@link Ctx}
 * (the helpers below add nothing unbounded: at most 6 allied players, 8 mobs or projectiles touched per cast, one kit zone of at most 100 ticks in steps of 4 or more, at most 10 effect
 * packets per cast, no entity spawned, no block placed, nothing kept between casts). Page ids are save keys: never renamed, never reordered.
 */
public final class Batch3 {
    private Batch3() {}

    /** Book registry path -> the pages this batch appends to it. */
    private static final Map<String, List<BookPage>> PAGES = new LinkedHashMap<>();

    private static List<BookPage> register(String book, List<BookPage> pages) {
        PAGES.put(book, List.copyOf(new ArrayList<>(pages)));
        return pages;
    }

    // ---------------------------------------------------------------- small bounded helpers (pure Java + the kit; nothing here touches a client class)
    /** A potion effect to hand out. */
    private static Supplier<MobEffectInstance> eff(Holder<MobEffect> e, int ticks, int amp) { return () -> new MobEffectInstance(e, ticks, amp); }

    /** The same as a hit rider. */
    private static Rider aff(Holder<MobEffect> e, int ticks, int amp) { return Pages15Kit.effect(eff(e, ticks, amp)); }

    /** A standard cast, then something for the caster (only when the cast really happened). */
    private static BookPage.Cast then(BookPage.Cast base, Consumer<ServerPlayer> after) {
        return (b, i, p, m) -> {
            if (!base.cast(b, i, p, m)) return false;
            after.accept(p);
            return true;
        };
    }

    /** The caster (when within {@code r} of {@code centre}) and the allied players within {@code r} (r up to 12), at most 6. */
    private static List<ServerPlayer> party(ServerPlayer caster, Vec3 centre, double r) {
        double rr = Math.min(r, 12.0);
        List<ServerPlayer> out = new ArrayList<>(6);
        if (caster.distanceToSqr(centre) <= rr * rr) out.add(caster);
        for (ServerPlayer a : caster.serverLevel().getEntitiesOfClass(ServerPlayer.class, new AABB(centre, centre).inflate(rr),
                a -> a != caster && a.isAlive() && !a.isSpectator() && a.isAlliedTo(caster) && a.distanceToSqr(centre) <= rr * rr)) {
            if (out.size() >= 6) break;
            out.add(a);
        }
        return out;
    }

    /** Takes the harmful effects off one entity. */
    private static void cleanse(LivingEntity e) {
        for (MobEffectInstance m : new ArrayList<>(e.getActiveEffects())) if (!m.getEffect().value().isBeneficial()) e.removeEffect(m.getEffect());
    }

    /** Pulls a target toward a point (a boss only about a third as far). */
    private static void pull(LivingEntity t, Vec3 to, double s) {
        Vec3 d = to.subtract(t.position());
        double len = d.length();
        if (len < 0.8) return;
        double k = Math.min(BalanceLaw.isBoss(t) ? s * 0.35 : s, len * 0.5);
        Vec3 v = d.scale(k / len);
        t.setDeltaMovement(t.getDeltaMovement().add(v.x, 0.12, v.z));
        t.hurtMarked = true;
    }

    private static Rider pullRider(double s) { return (t, p) -> pull(t, p.position(), s); }

    /** Pushes a target away from a point (a boss only about a third as far). */
    private static void shove(LivingEntity t, Vec3 from, double s) {
        Vec3 d = t.position().subtract(from).multiply(1, 0, 1);
        if (d.lengthSqr() < 1e-4) d = new Vec3(0, 0, 1);
        Vec3 v = d.normalize().scale(BalanceLaw.isBoss(t) ? s * 0.35 : s);
        t.setDeltaMovement(t.getDeltaMovement().add(v.x, 0.15, v.z));
        t.hurtMarked = true;
    }

    /** What a hit makes a mob forget: whom it hunted. Plus a little dizziness and a slow. */
    private static Rider forgetRider() {
        return (t, p) -> {
            if (t instanceof Mob m) m.setTarget(null);
            t.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
        };
    }

    /** Mobs within {@code r} (up to 16) that hunt the caster lose the trail; at most 8. */
    private static int calm(ServerPlayer p, double r) {
        int n = 0;
        for (Mob m : p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(Math.min(r, 18.0)), m -> m.getTarget() == p)) {
            if (n >= 8) break;
            m.setTarget(null);
            n++;
        }
        return n;
    }

    /** Removes enemy projectiles within {@code r} (up to 8) of a point; at most {@code max}. */
    private static int clearShots(ServerPlayer p, Vec3 at, double r, int max) {
        int n = 0;
        for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, new AABB(at, at).inflate(Math.min(r, 8.0)))) {
            if (n >= max) break;
            if (pr.getOwner() == p || (pr.getOwner() instanceof LivingEntity o && o.isAlliedTo(p))) continue;
            pr.discard();
            n++;
        }
        return n;
    }

    /** Effects for you and your party (up to 6 players within {@code r}); optionally shakes off their harmful effects. */
    @SafeVarargs
    private static BookPage.Cast partyBuff(Tier t, double r, VfxShape shape, float power, boolean cleanse, Supplier<MobEffectInstance>... effects) {
        return Pages15Kit.cast(t, c -> {
            c.circle(0.8f);
            for (ServerPlayer a : party(c.caster, c.caster.position(), r)) {
                if (cleanse) cleanse(a);
                for (Supplier<MobEffectInstance> s : effects) a.addEffect(s.get());
            }
            c.fx(shape, c.caster.position().add(0, 0.1, 0), c.caster.position().add(0, 1, 0), 30, power);
            return true;
        });
    }

    /** Effects (and optionally one standard hit) on the foes round you or the aim point; fails when it must find someone and finds no one. */
    @SafeVarargs
    private static BookPage.Cast mark(Tier t, double r, boolean atAim, boolean hit, boolean needFoes, VfxShape shape, float power, Supplier<MobEffectInstance>... effects) {
        return Pages15Kit.cast(t, c -> {
            Vec3 centre = atAim ? c.aim(22) : c.caster.position();
            List<LivingEntity> foes = c.foes(centre, r);
            if (needFoes && foes.isEmpty()) return c.fail("No one is near enough.");
            c.circle(0.8f);
            for (LivingEntity f : foes) {
                if (hit) c.hit(f);
                for (Supplier<MobEffectInstance> s : effects) f.addEffect(s.get());
            }
            c.fx(shape, centre.add(0, 0.1, 0), centre.add(0, 1, 0), 30, power);
            return true;
        });
    }

    /** Mobs hunting you lose the trail, and you turn invisible and quick for {@code ticks}. */
    private static BookPage.Cast sneak(Tier t, double r, VfxShape shape, int ticks, int speedAmp) {
        return Pages15Kit.cast(t, c -> {
            ServerPlayer p = c.caster;
            c.circle(0.7f);
            calm(p, r);
            p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, speedAmp));
            c.fx(shape, p.position().add(0, 0.1, 0), p.position().add(0, 1, 0), 30, 1.2f);
            return true;
        });
    }

    /** Heals you and your party by a fraction of their health (the book's heal decay applies), shakes off harmful effects, optionally Regeneration. */
    private static BookPage.Cast healSouls(float fraction, double r, VfxShape shape, boolean regen) {
        return Pages15Kit.cast(Tier.MID, c -> {
            c.circle(0.9f);
            for (ServerPlayer a : party(c.caster, c.caster.position(), r)) {
                cleanse(a);
                BalanceLaw.heal(a, a.getMaxHealth() * Math.min(fraction, 0.25f));
                if (regen) a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            }
            c.fx(shape, c.caster.position().add(0, 0.1, 0), c.caster.position().add(0, 1, 0), 30, (float) r / 3f);
            return true;
        });
    }

    /** A standing ward round the caster: enemy projectiles are removed, foes are pushed away, optionally a small pulse on them and Resistance on the party. */
    private static BookPage.Cast wardZone(Tier t, int ticks, double r, boolean harm, float power, VfxShape shape, boolean guardAllies) {
        return Pages15Kit.cast(t, c -> {
            if (!c.zone(ticks, 10, age -> {
                Vec3 at = c.caster.position();
                clearShots(c.caster, at, r, 6);
                for (LivingEntity f : c.foes(at, r)) {
                    shove(f, at, 0.8);
                    if (harm && age % 20 == 0) c.hit(f, c.tier.pulse);
                }
                if (guardAllies && age % 20 == 0) for (ServerPlayer a : party(c.caster, at, r)) a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0));
            })) return false;
            c.circle(1f);
            c.fx(shape, c.caster.position().add(0, 0.1, 0), c.caster.position().add(0, 1, 0), ticks, power);
            return true;
        });
    }

    // ---------------------------------------------------------------- the custom casts
    private static BookPage.Cast pastelGarden() {
        return Pages15Kit.cast(Tier.ZONE, c -> {
            Vec3 centre = c.aim(18);
            if (!c.zone(80, 20, age -> {
                for (ServerPlayer a : party(c.caster, centre, 5)) {
                    BalanceLaw.heal(a, a.getMaxHealth() * 0.04f);
                    a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0));
                }
                for (LivingEntity f : c.foes(centre, 5)) f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            })) return false;
            c.circle(1f);
            c.fx(VfxShape.DREAM_DOME, centre, centre.add(0, 1, 0), 80, 1.6f);
            return true;
        });
    }

    private static BookPage.Cast eternalSlumber() {
        return Pages15Kit.cast(Tier.SIGNATURE, c -> {
            Vec3 at = c.caster.position();
            List<LivingEntity> foes = c.foes(at, 9);
            if (foes.isEmpty()) return c.fail("No one to put to sleep.");
            c.circle(1.6f);
            boolean hold = BalanceLaw.beginControl(c.caster);
            for (LivingEntity f : foes) {
                c.hit(f);
                f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3));
                f.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                f.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
                if (hold) TimeStop.freeze(f, BalanceLaw.controlTicks(f, 60));
            }
            c.fx(VfxShape.DREAM_DOME, at, at.add(0, 1, 0), 60, 3f);
            return true;
        });
    }

    private static BookPage.Cast endlessCorridor() {
        return Pages15Kit.cast(Tier.ZONE, c -> {
            Vec3 centre = c.aim(20);
            if (!c.zone(80, 20, age -> {
                for (LivingEntity f : c.foes(centre, 5.5)) {
                    pull(f, centre, 0.6);
                    f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                    if (age % 40 == 0) c.hit(f, c.tier.pulse);
                }
            })) return false;
            c.circle(1f);
            c.fx(VfxShape.MAGIC_CIRCLE, centre.add(0, 0.05, 0), centre.add(0, 1, 0), 80, 3f);
            c.fx(VfxShape.MIRROR_PANE, centre.add(0, 1, 0), centre.add(c.caster.getViewVector(1f)).add(0, 1, 0), 60, 1.2f);
            return true;
        });
    }

    private static BookPage.Cast curtainOfLies() {
        return Pages15Kit.cast(Tier.ZONE, c -> {
            ServerPlayer p = c.caster;
            c.circle(1f);
            for (ServerPlayer a : party(p, p.position(), 8)) {
                a.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 160, 0));
                a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0));
            }
            calm(p, 18);
            c.fx(VfxShape.ELF_CIRCLE, p.position().add(0, 0.1, 0), p.position().add(0, 1, 0), 40, 2.6f);
            return true;
        });
    }

    private static BookPage.Cast pressureWall() {
        return Pages15Kit.cast(Tier.MID, c -> {
            ServerPlayer p = c.caster;
            Vec3 look = p.getViewVector(1f).multiply(1, 0, 1).normalize();
            Vec3 at = p.position().add(look.scale(2.5));
            c.circle(0.8f);
            clearShots(p, at.add(0, 1, 0), 4, 8);
            for (LivingEntity f : c.foes(at, 3.5)) {
                c.hit(f);
                shove(f, p.position(), 1.0);
            }
            c.fx(VfxShape.AIR_FX3, at.add(0, 0.1, 0), at.add(0, 1.5, 0), 20, 1.3f);
            return true;
        });
    }

    private static BookPage.Cast soaringLeap() {
        return Pages15Kit.cast(Tier.MID, c -> {
            ServerPlayer p = c.caster;
            Vec3 look = p.getViewVector(1f).multiply(1, 0, 1).normalize();
            c.circle(0.7f);
            p.setDeltaMovement(look.x * 0.9, 1.1, look.z * 0.9);
            p.hurtMarked = true;
            p.fallDistance = 0;
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0));
            c.fx(VfxShape.WING_FX3, p.position(), p.position().add(0, 1, 0), 20, 1f);
            return true;
        });
    }

    private static BookPage.Cast blankStare() {
        return Pages15Kit.cast(Tier.STARTER, c -> {
            Vec3 centre = c.aim(18);
            List<LivingEntity> foes = c.foes(centre, 3.5);
            if (foes.isEmpty()) return c.fail("No one there to confuse.");
            c.circle(0.7f);
            for (LivingEntity f : foes) {
                if (f instanceof Mob m) m.setTarget(null);
                f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                f.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
            }
            c.fx(VfxShape.MEMORY_FX3, centre, centre.add(0, 1, 0), 24, 1.2f);
            return true;
        });
    }

    private static BookPage.Cast mindProbe() {
        return Pages15Kit.cast(Tier.STARTER, c -> {
            LivingEntity t = c.target(20);
            if (t == null) return c.fail("No one to read.");
            c.circle(0.7f);
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
            c.fx(VfxShape.MEMORY_FX1, c.caster.getEyePosition(), t.getBoundingBox().getCenter(), 24, 1f);
            return true;
        });
    }

    private static BookPage.Cast magicSlip() {
        return Pages15Kit.cast(Tier.STARTER, c -> {
            ServerPlayer p = c.caster;
            c.circle(0.7f);
            clearShots(p, p.position().add(0, 1, 0), 4, 8);
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 2));
            c.fx(VfxShape.PERMEATION_FX3, p.position().add(0, 1, 0), p.position().add(0, 1, 1), 20, 0.9f);
            return true;
        });
    }

    // ---------------------------------------------------------------- the books of this batch, one register(...) each (generated from docs/magic/pages15_batch3.md; edit both)
    private static final List<BookPage> DREAM = register("book_dream", List.of(
                Pages15Kit.starter("drowsy_mist", "Drowsy Mist", Pages15Kit.cone(Tier.STARTER, 6, 0.45, VfxShape.DREAM_TRANSITION, Pages15Kit.all(aff(MobEffects.MOVEMENT_SLOWDOWN, 80, 1), aff(MobEffects.WEAKNESS, 80, 0)))),
                Pages15Kit.starter("counting_sheep", "Counting Sheep", Pages15Kit.volley(Tier.STARTER, 3, true, VfxShape.DREAM_MANIFEST, aff(MobEffects.MOVEMENT_SLOWDOWN, 30, 0))),
                Pages15Kit.starter("pillow_burst", "Pillow Burst", Pages15Kit.nova(Tier.STARTER, 3.5, false, VfxShape.DREAM_MANIFEST, Pages15Kit.knock(0.8))),
                Pages15Kit.mid("lucid_step", "Lucid Step", then(Pages15Kit.dash(Tier.MID, false, 8, false, VfxShape.DREAM_TRANSITION, Pages15Kit.NONE), p -> p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1)))).withAnim("out"),
                Pages15Kit.mid("dreamcatcher_ward", "Dreamcatcher Ward", partyBuff(Tier.MID, 7, VfxShape.DREAM_DOME, 1.0f, true, eff(MobEffects.ABSORPTION, 200, 1), eff(MobEffects.DAMAGE_RESISTANCE, 200, 0))),
                Pages15Kit.mid("mind_read", "Mind Read", mark(Tier.MID, 12, false, false, true, VfxShape.DREAM_TRANSITION, 2.5f, eff(MobEffects.GLOWING, 160, 0), eff(MobEffects.WEAKNESS, 100, 0))),
                Pages15Kit.mid("dream_nightmare_seed", "Nightmare Seed", Pages15Kit.bolt(Tier.MID, 2.5, VfxShape.DREAM_MANIFEST, VfxShape.DREAM_SHATTER, Pages15Kit.all(aff(MobEffects.DARKNESS, 100, 0), aff(MobEffects.WEAKNESS, 100, 0)))),
                Pages15Kit.zone("slumber_field", "Slumber Field", Pages15Kit.field(Tier.ZONE, 5, 80, 20, true, VfxShape.DREAM_TRANSITION, Pages15Kit.all(aff(MobEffects.MOVEMENT_SLOWDOWN, 50, 2), aff(MobEffects.WEAKNESS, 50, 0)))).withCooldown(450),
                Pages15Kit.zone("pastel_garden", "Pastel Garden", pastelGarden()).withCooldown(500),
                Pages15Kit.mid("sleepwalker", "Sleepwalker", sneak(Tier.MID, 16, VfxShape.DREAM_MANIFEST, 100, 1)),
                Pages15Kit.signature("eternal_slumber", "Eternal Slumber", eternalSlumber()).withCooldown(1800)));

    private static final List<BookPage> ILLUSION = register("book_illusion", List.of(
                Pages15Kit.starter("mirage_prism_ray", "Prism Ray", Pages15Kit.line(Tier.STARTER, 10, 1.2, VfxShape.MIRROR_PANE, aff(MobEffects.MOVEMENT_SLOWDOWN, 40, 0))),
                Pages15Kit.starter("afterimage_bolt", "Afterimage Bolt", Pages15Kit.volley(Tier.STARTER, 3, false, VfxShape.MIRROR_PANE, aff(MobEffects.CONFUSION, 40, 0))),
                Pages15Kit.starter("vertigo_ring", "Vertigo Ring", Pages15Kit.nova(Tier.STARTER, 3.5, false, VfxShape.ELF_CIRCLE, Pages15Kit.all(aff(MobEffects.CONFUSION, 80, 0), Pages15Kit.knock(0.4)))),
                Pages15Kit.mid("illusion_mirage_walk", "Mirage Walk", Pages15Kit.dash(Tier.MID, true, 9, false, VfxShape.MIRROR_PANE, aff(MobEffects.BLINDNESS, 30, 0))).withAnim("thrust"),
                Pages15Kit.mid("illusory_visage", "Illusory Visage", Pages15Kit.cone(Tier.MID, 7, 0.4, VfxShape.ELF_CIRCLE, Pages15Kit.all(aff(MobEffects.WEAKNESS, 100, 1), aff(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), Pages15Kit.knock(0.8)))),
                Pages15Kit.mid("phantom_host", "Phantom Host", Pages15Kit.volley(Tier.MID, 5, true, VfxShape.MIRROR_PANE, aff(MobEffects.BLINDNESS, 20, 0))),
                Pages15Kit.mid("paralyzing_illusion", "Paralyzing Illusion", Pages15Kit.bind(Tier.MID, 18, false, VfxShape.ELF_CIRCLE, aff(MobEffects.CONFUSION, 100, 0))),
                Pages15Kit.zone("endless_corridor", "Endless Corridor", endlessCorridor()).withCooldown(480),
                Pages15Kit.zone("curtain_of_lies", "Curtain of Lies", curtainOfLies()).withCooldown(600),
                Pages15Kit.signature("illusion_reality_break", "Reality Break", Pages15Kit.nova(Tier.SIGNATURE, 7, false, VfxShape.MAGIC_CIRCLE_EXPLOSION, Pages15Kit.all(aff(MobEffects.CONFUSION, 120, 0), aff(MobEffects.BLINDNESS, 60, 0), Pages15Kit.knock(0.9)))).withCooldown(1500)));

    private static final List<BookPage> AIR = register("book_air", List.of(
                Pages15Kit.starter("air_gust_jab", "Gust Jab", Pages15Kit.cone(Tier.STARTER, 4, 0.6, VfxShape.AIR_FX1, Pages15Kit.knock(0.9))),
                Pages15Kit.starter("air_needles", "Air Needles", Pages15Kit.volley(Tier.STARTER, 4, false, VfxShape.AIR_FX1, Pages15Kit.NONE)),
                Pages15Kit.starter("featherfall_gift", "Featherfall Gift", partyBuff(Tier.STARTER, 8, VfxShape.AIR_FX2, 1.0f, false, eff(MobEffects.SLOW_FALLING, 400, 0), eff(MobEffects.MOVEMENT_SPEED, 100, 0))),
                Pages15Kit.mid("air_updraft", "Updraft", Pages15Kit.nova(Tier.MID, 3.5, false, VfxShape.AIR_FX3, Pages15Kit.lift(0.75))).withAnim("up"),
                Pages15Kit.mid("compression_lance", "Compression Lance", Pages15Kit.line(Tier.MID, 14, 1.0, VfxShape.AIR_FX1, Pages15Kit.knock(0.6))).withAnim("thrust"),
                Pages15Kit.mid("air_pressure_wall", "Pressure Wall", pressureWall()),
                Pages15Kit.zone("pressure_rampart", "Pressure Rampart", Pages15Kit.field(Tier.ZONE, 4, 80, 10, false, VfxShape.AIR_FX2, Pages15Kit.knock(1.0))).withCooldown(420)));

    private static final List<BookPage> SMOKE = register("book_smoke", List.of(
                Pages15Kit.starter("smoke_bomb", "Smoke Bomb", Pages15Kit.bolt(Tier.STARTER, 3, VfxShape.SMOKE_FX1, VfxShape.SMOKE_FX3, aff(MobEffects.BLINDNESS, 60, 0))),
                Pages15Kit.starter("hermits_thick_smoke", "Hermit's Thick Smoke", then(mark(Tier.STARTER, 5, false, false, false, VfxShape.SMOKE_FX3, 1.3f, eff(MobEffects.BLINDNESS, 80, 0), eff(MobEffects.MOVEMENT_SLOWDOWN, 40, 0)), p -> p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0)))),
                Pages15Kit.mid("smoke_double", "Smoke Double", sneak(Tier.MID, 16, VfxShape.SMOKE_FX3, 80, 1)),
                Pages15Kit.mid("smoke_choking_fumes", "Choking Fumes", Pages15Kit.cone(Tier.MID, 7, 0.45, VfxShape.SMOKE_FX1, Pages15Kit.all(aff(MobEffects.POISON, 60, 0), aff(MobEffects.MOVEMENT_SLOWDOWN, 60, 0)))),
                Pages15Kit.mid("degenerate_kings_smoke_prison", "Degenerate King's Smoke Prison", Pages15Kit.bind(Tier.MID, 16, false, VfxShape.SMOKE_FX2, aff(MobEffects.BLINDNESS, 80, 0))),
                Pages15Kit.zone("billowing_wall", "Billowing Wall", Pages15Kit.field(Tier.ZONE, 4, 80, 20, false, VfxShape.SMOKE_FX2, Pages15Kit.all(aff(MobEffects.BLINDNESS, 40, 0), aff(MobEffects.MOVEMENT_SLOWDOWN, 40, 1)))).withCooldown(450),
                Pages15Kit.zone("smoke_garden_of_usurpation", "Smoke Garden of Usurpation", Pages15Kit.field(Tier.ZONE, 5, 100, 20, true, VfxShape.SMOKE_FX2, Pages15Kit.all(aff(MobEffects.POISON, 80, 0), aff(MobEffects.WEAKNESS, 80, 0), aff(MobEffects.MOVEMENT_SLOWDOWN, 40, 0)))).withCooldown(600)));

    private static final List<BookPage> VORTEX = register("book_vortex", List.of(
                Pages15Kit.starter("eddy_bolt", "Eddy Bolt", Pages15Kit.bolt(Tier.STARTER, 2, VfxShape.VORTEX_FX1, VfxShape.VORTEX_FX3, pullRider(0.7))),
                Pages15Kit.starter("swirl_kick", "Swirl Kick", Pages15Kit.cone(Tier.STARTER, 4, 0.5, VfxShape.VORTEX_FX1, Pages15Kit.all(Pages15Kit.knock(0.6), Pages15Kit.lift(0.3)))).withAnim("sweep"),
                Pages15Kit.starter("vortex_centrifuge", "Centrifuge", Pages15Kit.nova(Tier.STARTER, 3.5, false, VfxShape.VORTEX_FX3, Pages15Kit.knock(1.2))),
                Pages15Kit.mid("vortex_undertow", "Undertow", Pages15Kit.nova(Tier.MID, 6, false, VfxShape.VORTEX_FX2, pullRider(0.9))),
                Pages15Kit.mid("vortex_whirlpool_snare", "Whirlpool Snare", Pages15Kit.bind(Tier.MID, 16, false, VfxShape.VORTEX_FX2, pullRider(0.5))),
                Pages15Kit.mid("vortex_cyclone_lance", "Cyclone Lance", Pages15Kit.line(Tier.MID, 14, 1.2, VfxShape.VORTEX_FX1, pullRider(0.8))).withAnim("thrust"),
                Pages15Kit.zone("vortex_maelstrom_ring", "Maelstrom Ring", Pages15Kit.field(Tier.ZONE, 5, 80, 10, false, VfxShape.VORTEX_FX2, pullRider(0.5))).withCooldown(420)));

    private static final List<BookPage> WING = register("book_wing", List.of(
                Pages15Kit.starter("feather_flick", "Feather Flick", Pages15Kit.bolt(Tier.STARTER, 1.5, VfxShape.WING_FX1, VfxShape.WING_FX3, Pages15Kit.NONE)),
                Pages15Kit.starter("wing_downbeat", "Downbeat", Pages15Kit.nova(Tier.STARTER, 3.5, false, VfxShape.WING_FX3, Pages15Kit.knock(0.9))),
                Pages15Kit.starter("glide_grace", "Glide Grace", partyBuff(Tier.STARTER, 7, VfxShape.WING_FX2, 1.0f, false, eff(MobEffects.SLOW_FALLING, 400, 0), eff(MobEffects.JUMP, 200, 1))),
                Pages15Kit.mid("wing_soaring_leap", "Soaring Leap", soaringLeap()).withAnim("up"),
                Pages15Kit.mid("wing_talon_rake", "Talon Rake", Pages15Kit.cone(Tier.MID, 5, 0.5, VfxShape.WING_FX1, Pages15Kit.all(Pages15Kit.knock(0.5), aff(MobEffects.MOVEMENT_SLOWDOWN, 40, 0)))).withAnim("sweep"),
                Pages15Kit.mid("feather_mantle", "Feather Mantle", Pages15Kit.empower(Tier.MID, 200, true, VfxShape.WING_FX2, eff(MobEffects.SLOW_FALLING, 200, 0), eff(MobEffects.DAMAGE_RESISTANCE, 200, 0))),
                Pages15Kit.zone("skyward_gale", "Skyward Gale", Pages15Kit.field(Tier.ZONE, 5, 60, 20, false, VfxShape.WING_FX2, Pages15Kit.all(Pages15Kit.lift(0.5), Pages15Kit.knock(0.6)))).withCooldown(420)));

    private static final List<BookPage> WIND = register("book_wind", List.of(
                Pages15Kit.starter("cutting_breeze", "Cutting Breeze", Pages15Kit.volley(Tier.STARTER, 3, false, VfxShape.WIND_SLASH, Pages15Kit.NONE)),
                Pages15Kit.mid("thiefs_wind_cloud", "Thief's Wind Cloud", then(Pages15Kit.dash(Tier.MID, false, 12, false, VfxShape.WIND_GALE, Pages15Kit.NONE), p -> { p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0)); p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1)); })).withAnim("out"),
                Pages15Kit.mid("wind_veil", "Wind Veil", Pages15Kit.empower(Tier.MID, 200, true, VfxShape.WIND_ZEPHYR, eff(MobEffects.MOVEMENT_SPEED, 200, 1), eff(MobEffects.SLOW_FALLING, 200, 0))),
                Pages15Kit.mid("spirit_of_euros", "Spirit of Euros", Pages15Kit.line(Tier.MID, 16, 1.5, VfxShape.WIND_GALE, Pages15Kit.all(Pages15Kit.knock(1.0), aff(MobEffects.WEAKNESS, 60, 0)))).withAnim("thrust"),
                Pages15Kit.zone("spirit_of_notos", "Spirit of Notos", wardZone(Tier.ZONE, 60, 5.0, false, 1.6f, VfxShape.WIND_RING, true)).withCooldown(500),
                Pages15Kit.signature("saint_spirit_of_zephyr", "Saint Spirit of Zephyr", then(Pages15Kit.nova(Tier.SIGNATURE, 6, false, VfxShape.WIND_TORNADO, Pages15Kit.knock(1.0)), p -> { p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 2)); p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1)); p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0)); p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 300, 0)); })).withCooldown(2000)));

    private static final List<BookPage> SOUND = register("book_sound", List.of(
                Pages15Kit.starter("tuning_fork_strike", "Sound: Tuning Fork Strike", Pages15Kit.bolt(Tier.STARTER, 1.5, VfxShape.SOUND_FX1, VfxShape.SOUND_FX3, aff(MobEffects.WEAKNESS, 40, 0))),
                Pages15Kit.starter("piercing_shriek", "Sound: Piercing Shriek", Pages15Kit.cone(Tier.STARTER, 6, 0.4, VfxShape.SOUND_FX1, aff(MobEffects.CONFUSION, 60, 0))),
                Pages15Kit.mid("sonic_echolocation", "Sound: Echolocation", mark(Tier.MID, 16, false, false, true, VfxShape.SOUND_FX2, 2.5f, eff(MobEffects.GLOWING, 200, 0))),
                Pages15Kit.mid("bass_drop", "Sound: Bass Drop", Pages15Kit.nova(Tier.MID, 5, false, VfxShape.SOUND_FX3, Pages15Kit.all(Pages15Kit.knock(1.2), aff(MobEffects.MOVEMENT_SLOWDOWN, 40, 1)))).withAnim("slam"),
                Pages15Kit.mid("subsonic_lance", "Sound: Subsonic Lance", Pages15Kit.line(Tier.MID, 16, 1.2, VfxShape.SOUND_FX1, Pages15Kit.all(Pages15Kit.knock(0.5), aff(MobEffects.WEAKNESS, 80, 0)))).withAnim("thrust"),
                Pages15Kit.zone("sound_barrier", "Sound: Sound Barrier", wardZone(Tier.ZONE, 60, 4.5, true, 1.4f, VfxShape.SOUND_FX3, false)).withCooldown(450)));

    private static final List<BookPage> MEMORY = register("book_memory", List.of(
                Pages15Kit.starter("memory_flashback", "Memory: Flashback", Pages15Kit.cone(Tier.STARTER, 5, 0.5, VfxShape.MEMORY_FX1, Pages15Kit.all(aff(MobEffects.BLINDNESS, 40, 0), aff(MobEffects.CONFUSION, 40, 0)))),
                Pages15Kit.starter("blank_stare", "Memory: Blank Stare", blankStare()),
                Pages15Kit.starter("mind_probe", "Memory: Mind Probe", mindProbe()),
                Pages15Kit.mid("oblivion_azusa_arrow", "Memory: Oblivion's Azusa Arrow", Pages15Kit.bolt(Tier.MID, 0, VfxShape.MEMORY_FX1, VfxShape.MEMORY_FX3, forgetRider())).withAnim("thrust"),
                Pages15Kit.mid("shared_memory", "Memory: Shared Memory", partyBuff(Tier.MID, 12, VfxShape.MEMORY_FX2, 1.5f, false, eff(MobEffects.MOVEMENT_SPEED, 200, 0), eff(MobEffects.NIGHT_VISION, 300, 0), eff(MobEffects.DAMAGE_RESISTANCE, 200, 0))),
                Pages15Kit.zone("memory_labyrinth", "Memory: Labyrinth of Recollection", Pages15Kit.field(Tier.ZONE, 5, 80, 20, true, VfxShape.MEMORY_FX2, Pages15Kit.all(aff(MobEffects.WEAKNESS, 60, 0), aff(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), aff(MobEffects.GLOWING, 60, 0), forgetRider()))).withCooldown(480)));

    private static final List<BookPage> PERMEATION = register("book_permeation", List.of(
                Pages15Kit.starter("magic_slip", "Permeation: Magic Slip", magicSlip()),
                Pages15Kit.starter("seeping_touch", "Permeation: Seeping Touch", Pages15Kit.cone(Tier.STARTER, 3, 0.7, VfxShape.PERMEATION_FX1, Pages15Kit.all(aff(MobEffects.WEAKNESS, 80, 1), aff(MobEffects.MOVEMENT_SLOWDOWN, 40, 0)))),
                Pages15Kit.mid("vanish_from_sight", "Permeation: Vanish from Sight", sneak(Tier.MID, 16, VfxShape.PERMEATION_FX3, 140, 1)),
                Pages15Kit.mid("unseen_creation", "Permeation: Unseen Creation", Pages15Kit.volley(Tier.MID, 3, true, VfxShape.PERMEATION_FX1, Pages15Kit.NONE)),
                Pages15Kit.mid("seepage_trap", "Permeation: Seepage Trap", Pages15Kit.bind(Tier.MID, 14, false, VfxShape.PERMEATION_FX2, aff(MobEffects.WEAKNESS, 80, 0))),
                Pages15Kit.zone("permeation_hollow_beam", "Permeation: Hollow Beam", Pages15Kit.line(Tier.ZONE, 18, 1.5, VfxShape.PERMEATION_FX1, Pages15Kit.all(aff(MobEffects.WEAKNESS, 100, 1), aff(MobEffects.MOVEMENT_SLOWDOWN, 60, 1)))).withCooldown(420).withAnim("thrust")));

    private static final List<BookPage> SOUL = register("book_soul", List.of(
                Pages15Kit.starter("soul_chill", "Soul: Soul Chill", Pages15Kit.cone(Tier.STARTER, 5, 0.5, VfxShape.SOUL_FX1, Pages15Kit.all(aff(MobEffects.WEAKNESS, 80, 0), aff(MobEffects.MOVEMENT_SLOWDOWN, 40, 0)))),
                Pages15Kit.starter("soul_spirit_drain", "Soul: Spirit Drain", Pages15Kit.bolt(Tier.STARTER, 1.5, VfxShape.SOUL_FX1, VfxShape.SOUL_FX3, Pages15Kit.leech(1.5f))),
                Pages15Kit.mid("soul_ward", "Soul: Soul Ward", partyBuff(Tier.MID, 8, VfxShape.SOUL_FX2, 1.2f, false, eff(MobEffects.ABSORPTION, 300, 1), eff(MobEffects.DAMAGE_RESISTANCE, 200, 0))),
                Pages15Kit.mid("soul_mend", "Soul: Soul Mend", healSouls(0.18f, 8, VfxShape.SOUL_FX2, false)),
                Pages15Kit.mid("imprint_of_ideals", "Soul: Imprint of Ideals", Pages15Kit.bind(Tier.MID, 16, false, VfxShape.SOUL_FX2, Pages15Kit.all(forgetRider(), aff(MobEffects.WEAKNESS, 100, 0)))),
                Pages15Kit.zone("grimoire_tower_link", "Soul: Grimoire Tower Link", then(Pages15Kit.nova(Tier.ZONE, 6, false, VfxShape.SOUL_VORTEX, Pages15Kit.strip()), p -> { for (ServerPlayer a : party(p, p.position(), 8)) a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0)); })).withCooldown(450)));

    private static final List<BookPage> SOUL_CORPSE = register("book_soul_corpse", List.of(
                Pages15Kit.starter("grasping_dead", "Soul Corpse: Grasping Dead", Pages15Kit.nova(Tier.STARTER, 3.5, true, VfxShape.SOUL_CORPSE_FX3, aff(MobEffects.MOVEMENT_SLOWDOWN, 60, 2))),
                Pages15Kit.starter("corpse_bone_rattle", "Soul Corpse: Bone Rattle", Pages15Kit.volley(Tier.STARTER, 3, false, VfxShape.SOUL_CORPSE_FX1, aff(MobEffects.WEAKNESS, 60, 0))),
                Pages15Kit.mid("corpse_puppet_strings", "Soul Corpse: Puppet Strings", Pages15Kit.line(Tier.MID, 12, 1.0, VfxShape.SOUL_CORPSE_FX1, pullRider(0.9))).withAnim("thrust"),
                Pages15Kit.mid("corpse_death_rattle", "Soul Corpse: Death Rattle", Pages15Kit.nova(Tier.MID, 5, false, VfxShape.SOUL_CORPSE_FX3, Pages15Kit.all(aff(MobEffects.WITHER, 60, 0), Pages15Kit.knock(0.7)))).withAnim("slam"),
                Pages15Kit.mid("corpse_revenant_rush", "Soul Corpse: Revenant Rush", Pages15Kit.dash(Tier.MID, true, 8, false, VfxShape.SOUL_CORPSE_FX1, aff(MobEffects.WITHER, 40, 0))).withAnim("thrust"),
                Pages15Kit.zone("soul_abductor", "Soul Corpse: Soul Abductor", healSouls(0.25f, 9, VfxShape.SOUL_CORPSE_FX2, true)).withCooldown(700)));

    private static final List<BookPage> SONG = register("book_song", List.of(
                Pages15Kit.starter("song_forte_bubbles", "Song: Forte Bubbles", Pages15Kit.bolt(Tier.STARTER, 2.5, VfxShape.SONG_FX1, VfxShape.SONG_FX3, aff(MobEffects.WEAKNESS, 40, 0))),
                Pages15Kit.mid("song_silver_spiral", "Song: Silver Spiral", Pages15Kit.line(Tier.MID, 14, 1.2, VfxShape.SONG_FX1, Pages15Kit.all(Pages15Kit.knock(0.4), aff(MobEffects.MOVEMENT_SLOWDOWN, 40, 0)))).withAnim("thrust")));

    /** The pages this batch appends to the book (empty for a book it has not written yet, or a book of another batch). Never null. */
    static List<BookPage> pages(String bookPath) {
        return PAGES.getOrDefault(bookPath, List.of());
    }
}
