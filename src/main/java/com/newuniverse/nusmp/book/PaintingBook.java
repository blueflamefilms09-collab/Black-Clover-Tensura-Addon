package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.newuniverse.nusmp.book.BookPage.*;

/**
 * Painting Magic (0.40), after the Black Clover wiki: the mage manifests a palette and brush; what they paint comes to life, and
 * the paint can become any element. Rill Boismortier (Aqua Deer) and Lira use it. Spells from the wiki's list (Deux Tempêtes of
 * Fire and Ice, Souterrain Giant's Strong Arm, Master of Valhalla, God's Game, Spring of Restriction, Elemental Quintet) plus a
 * brushstroke starter, Camouflage and the Painted Menagerie.
 *
 * <ul>
 *   <li><b>Ink & brushwork:</b> every spell is painted: wet strokes laid through the air ({@link VfxShape#PAINT_STROKE}), and
 *       paintings that lift off the canvas into glowing living outlines ({@link VfxShape#PAINT_BEAST}).</li>
 *   <li><b>Viscosity:</b> ink splashes ({@link VfxShape#PAINT_SPLAT}) are sticky: they slow whatever walks through them, and as
 *       they dry to lacquer they hold fast (rooted).</li>
 *   <li><b>Camouflage:</b> a refraction-shimmer coat of paint that hides the painter until they strike
 *       ({@link VfxShape#PAINT_CAMO}).</li>
 * </ul>
 * 0.44 remake: the palette & brush are real items the grimoire manifests, the paint can become any element (and counters
 * them), the painter's mood and EP scale everything, and paintings come to life as solid constructs. See {@link PaintStudio}.
 */
public final class PaintingBook {
    /** Ink colours: the painter's blue, and the elements the paint can become. */
    static final int INK = 0xFF3A7BFF, FIRE = 0xFFFF5A3A, ICE = 0xFF8AE6FF, EARTH = 0xFFB0864A, WIND = 0xFF7CF0B0, BOLT = 0xFFFFE65A, GOLD = 0xFFFFC84A;

    private PaintingBook() {}

    public static GrimoireBook create() {
        return new ElementBook(MagicType.PAINTING, INK, TensuraDamageTypes.MAGIC_GENERIC, List.of(
                starter("brushstroke", "Brushstroke", PaintingBook::brushstroke),
                mid("spring_of_restriction", "Spring of Restriction", PaintingBook::springOfRestriction),
                mid("camouflage", "Camouflage", PaintingBook::camouflage).withCooldown(600),
                mid("souterrain_giant_arm", "Souterrain Giant's Strong Arm", PaintingBook::giantArm),
                zone("deux_tempetes", "Deux Tempêtes of Fire and Ice", PaintingBook::deuxTempetes),
                signature("gods_game", "God's Game", PaintingBook::godsGame).withCooldown(900),
                signature("elemental_quintet", "Elemental Quintet", PaintingBook::elementalQuintet),
                signature("master_of_valhalla", "Master of Valhalla", PaintingBook::masterOfValhalla).withCooldown(1200),
                signature("painted_menagerie", "Painted Menagerie", PaintStudio::menagerie).withCooldown(900),   // 0.44: living painted beasts
                // 0.44: appended (page order is the unlock order; old pages keep their places)
                mid("living_illustration", "Living Illustration", PaintStudio::livingIllustration).withCooldown(400),
                mid("counter_palette", "Counter Palette", PaintStudio::counterPalette).withCooldown(300))).plus(com.newuniverse.nusmp.book.ext.PaintingExt.pages());
    }

    // ---------------------------------------------------------------- shared
    static void stroke(ServerPlayer p, Vec3 from, Vec3 to, int color, int ticks, float width) {
        VfxSpawn.send(p.serverLevel(), VfxShape.PAINT_STROKE, from, to, color, ticks, width);
    }

    /** A sticky ink splash at 'at': slows what walks in it; once dry (after half its time) it roots them. */
    static void stickyInk(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, Vec3 at, float r, int ticks, int color, boolean root) {
        ServerLevel level = p.serverLevel();
        VfxSpawn.send(level, VfxShape.PAINT_SPLAT, at, at.add(0, 1, 0), color, ticks, r);
        level.playSound(null, net.minecraft.core.BlockPos.containing(at), SoundEvents.SLIME_BLOCK_PLACE, SoundSource.PLAYERS, 1.2f, 0.8f);
        SpellRuntime.zone(level, ticks, 5, age -> {
            boolean dry = age * 5 >= ticks / 2;
            for (LivingEntity t : GrimoireBook.around(p, at, r)) {
                if (Math.abs(t.getY() - at.y) > 1.5) continue;
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, dry && root ? 6 : 2, false, true));
                if (dry && root) {                                                    // dried lacquer: held fast
                    t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
                    t.hurtMarked = true;
                }
            }
        });
    }

    // ---------------------------------------------------------------- the spells
    /** Brushstroke: a wet stroke of ink flung through the air; it splashes where it lands and leaves a sticky puddle. */
    static boolean brushstroke(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 eye = p.getEyePosition().add(p.getLookAngle().scale(0.6)).add(0, -0.3, 0);
        Vec3 dir = p.getLookAngle();
        PaintStudio.Paint paint = PaintStudio.paint(p);                                  // 0.44: in the palette's current paint
        float pw = PaintStudio.power(p);
        SpellRuntime.bolt(p, eye, dir.scale(1.6), 0.6, 12, false, null,
                (bolt, t) -> {
                    b.hurt(i, p, t, mode, 7f * pw);
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    PaintStudio.applyPaint(p, t, paint, pw);
                },
                (bolt, at) -> {
                    VfxSpawn.send(p.serverLevel(), VfxShape.PAINT_TRAIL, eye, at, paint.color, 16, 0.7f);
                    Vec3 ground = p.level().clip(new net.minecraft.world.level.ClipContext(at, at.add(0, -4, 0),
                            net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, p)).getLocation();
                    stickyInk(b, i, p, mode, ground, 1.6f, 60, paint.color, false);
                });
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 1.2f, 1.3f);
        return true;
    }

    /** Spring of Restriction: a spring of sticky paint wells up under the target and sets like lacquer, holding them fast. */
    static boolean springOfRestriction(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!GrimoireBook.control(p)) return false;
        LivingEntity t = GrimoireBook.target(p, 24);
        Vec3 at = t != null ? t.position() : GrimoireBook.aim(p, 24);
        b.castCircle(p, 1.1f);
        stroke(p, p.getEyePosition(), at.add(0, 0.2, 0), INK, 16, 0.5f);
        int ticks = BalanceLaw.controlTicks(t != null ? t : p, 100);
        SpellRuntime.later(p.serverLevel(), 6, () -> stickyInk(b, i, p, mode, at, 3f * PaintStudio.size(i, p), ticks, 0xFF6A4AFF, true));
        return true;
    }

    /** Camouflage: paint over yourself in the colours of the world behind you; hidden until you strike (15 s). */
    static boolean camouflage(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 300, 0, false, false));
        p.getPersistentData().putLong("nusmp_camo_until", p.level().getGameTime() + 300);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.PAINT_CAMO, p, p.position(), 0xFFE8F4FF, 300, 1f);
        VfxSpawn.send(p.serverLevel(), VfxShape.PAINT_SPLAT, p.position(), p.position().add(0, 1, 0), INK, 30, 1.2f);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 1f, 0.7f);
        p.displayClientMessage(Component.literal("You paint yourself into the scenery.").withStyle(ChatFormatting.AQUA), true);
        return true;
    }

    /** Striking breaks the camouflage (GrimoireBook.onDamageEntity, every book). */
    public static void breakCamouflage(ServerPlayer p) {
        if (p.getPersistentData().getLong("nusmp_camo_until") <= p.level().getGameTime()) return;
        p.getPersistentData().putLong("nusmp_camo_until", 0);
        p.removeEffect(MobEffects.INVISIBILITY);
    }

    /** Souterrain Giant's Strong Arm: a painted giant's arm bursts up from the ground and flings the target skyward. */
    static boolean giantArm(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 24);
        Vec3 at = t != null ? t.position() : GrimoireBook.aim(p, 24);
        b.castCircle(p, 1.2f);
        stroke(p, p.getEyePosition(), at, EARTH, 14, 0.6f);
        VfxSpawn.send(p.serverLevel(), VfxShape.PAINT_BEAST, at, at.add(0, 3, 0), EARTH, 30, 1.8f);
        SpellRuntime.later(p.serverLevel(), 10, () -> {
            p.serverLevel().playSound(null, net.minecraft.core.BlockPos.containing(at), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 1.5f, 0.6f);
            for (LivingEntity v : GrimoireBook.around(p, at, 2.5 * PaintStudio.size(i, p))) {
                b.hurt(i, p, v, mode, 12f * PaintStudio.power(p));
                v.setDeltaMovement(v.getDeltaMovement().x, 1.1, v.getDeltaMovement().z);
                v.hurtMarked = true;
            }
        });
        return true;
    }

    /** Deux Tempêtes of Fire and Ice: two painted storms turn round you for 5 s - fire on your right, ice on your left. */
    static boolean deuxTempetes(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.5f);
        ServerLevel level = p.serverLevel();
        float r = 6 * PaintStudio.size(i, p);
        SpellRuntime.zone(level, 100, 5, age -> {
            float spin = age * 0.12f;
            for (int k = 0; k < 2; k++) {
                float a = spin + k * Mth.PI;
                Vec3 c = p.position().add(Mth.cos(a) * r * 0.55, 0.2, Mth.sin(a) * r * 0.55);
                int col = k == 0 ? FIRE : ICE;
                if (age % 15 == 0) {
                    VfxSpawn.send(level, VfxShape.PAINT_SPLAT, c, c.add(0, 1, 0), col, 20, 1.8f);
                    stroke(p, c.add(Mth.cos(a + 1.2f) * 2, 2.5, Mth.sin(a + 1.2f) * 2), c, col, 12, 0.9f);
                }
                for (LivingEntity t : GrimoireBook.around(p, c, 2.6)) {
                    b.hurt(i, p, t, mode, 2.5f * PaintStudio.power(p));
                    if (k == 0) t.igniteForSeconds(3);
                    else {
                        t.setTicksFrozen(Math.max(t.getTicksFrozen(), 200));
                        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
                    }
                }
            }
        });
        return true;
    }

    /** God's Game: a painted world of your own rules for 8 s - shots that reach you turn to paint, and you are hard to hurt. */
    static boolean godsGame(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.6f);
        ServerLevel level = p.serverLevel();
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 2));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, 2));
        VfxSpawn.send(level, VfxShape.PAINT_BEAST, p.position(), p.position().add(0, 2, 0), GOLD, 36, 2.6f);
        SpellRuntime.zone(level, 160, 2, age -> {
            for (Projectile pr : level.getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(4.5),
                    x -> x.getOwner() != p && !(x.getOwner() instanceof LivingEntity o && o.isAlliedTo(p)))) {
                VfxSpawn.send(level, VfxShape.PAINT_SPLAT, pr.position(), pr.position().add(0, 1, 0), GOLD, 20, 0.6f);
                pr.discard();
            }
            if (age % 40 == 0) VfxSpawn.send(level, VfxShape.PAINT_SPLAT, p.position(), p.position().add(0, 1, 0), GOLD, 40, 3.5f);
        });
        return true;
    }

    /** Elemental Quintet: five strokes of five elements at once - fire, water, wind, earth and lightning - on up to five foes. */
    static boolean elementalQuintet(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.6f);
        List<LivingEntity> foes = GrimoireBook.around(p, p.position(), 16 * PaintStudio.size(i, p));
        foes.sort((x, y) -> Double.compare(x.distanceToSqr(p), y.distanceToSqr(p)));
        int[] cols = {FIRE, 0xFF4AA8FF, WIND, EARTH, BOLT};
        for (int k = 0; k < 5; k++) {
            LivingEntity t = foes.isEmpty() ? null : foes.get(k % foes.size());
            Vec3 at = t != null ? t.getBoundingBox().getCenter() : GrimoireBook.aim(p, 20).add((k - 2) * 1.5, 0, 0);
            int el = k;
            float a = Mth.TWO_PI * k / 5;
            Vec3 start = p.getEyePosition().add(Mth.cos(a) * 1.2, 0.6 + Mth.sin(a) * 0.6, Mth.sin(a) * 1.2);
            SpellRuntime.later(p.serverLevel(), 2 + k * 3, () -> {
                stroke(p, start, at, cols[el], 16, 0.8f);
                if (t == null || !t.isAlive()) return;
                b.hurt(i, p, t, mode, 8f * PaintStudio.power(p));
                switch (el) {
                    case 0 -> t.igniteForSeconds(5);
                    case 1 -> t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
                    case 2 -> { Vec3 away = t.position().subtract(p.position()).normalize(); t.knockback(1.6, -away.x, -away.z); }
                    case 3 -> t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
                    default -> b.hurt(i, p, t, mode, 6f * PaintStudio.power(p));                                    // lightning hits twice
                }
            });
        }
        return true;
    }

    /** Master of Valhalla: a painted host of einherjar charges out ahead of you, cutting down everything in its path. */
    static boolean masterOfValhalla(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 2f);
        ServerLevel level = p.serverLevel();
        Vec3 dir = p.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        float len = 16 * PaintStudio.size(i, p);
        level.playSound(null, p.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, 0.8f, 1.4f);
        for (int k = -3; k <= 3; k++) {
            Vec3 start = p.position().add(side.scale(k * 1.4)).add(dir.scale(1.5));
            VfxSpawn.send(level, VfxShape.PAINT_BEAST, start, start.add(0, 2, 0), GOLD, 30, 1.2f);
            int lane = k;
            SpellRuntime.later(level, 14, () -> stroke(p, start.add(0, 1, 0), start.add(dir.scale(len)).add(0, 1, 0), lane % 2 == 0 ? GOLD : INK, 18, 1.1f));
        }
        for (int k = 0; k < 4; k++) {                                                    // 0.44: four einherjar stay to fight
            Vec3 at = p.position().add(side.scale((k - 1.5) * 2.2)).add(dir.scale(3));
            PaintStudio.illustrate(p, com.newuniverse.nusmp.entity.PaintedConstructEntity.Kind.KNIGHT, at, PaintStudio.Paint.LIGHTNING, 400);
        }
        SpellRuntime.later(level, 18, () -> {
            for (LivingEntity t : GrimoireBook.along(p, p.position().add(0, 1, 0), p.position().add(dir.scale(len)).add(0, 1, 0), 4.5)) {
                b.hurt(i, p, t, mode, 16f * PaintStudio.power(p));
                t.knockback(1.2, -dir.x, -dir.z);
            }
        });
        return true;
    }
}
