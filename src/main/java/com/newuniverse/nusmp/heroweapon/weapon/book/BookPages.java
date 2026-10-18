package com.newuniverse.nusmp.heroweapon.weapon.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.GlassKit;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: a page of the Vassal Book is one bounded effect, and the six pages (a bird of flame, a rain of needles, a beam of feathers; a wall of paper, a verse that
 * mends, a cage that binds) are used in two places: the chapters, which read them in order, and the key techniques that cast two of them at will (the
 * Constriction, the burst). Written once here, each is a method that returns false when it finds nothing to act on (so the page is NOT read and nothing is spent)
 * and true when it worked. Damage goes through {@code HeroCtx} (the balance law), heals through {@code HeroCtx.heal} (capped at a quarter of a friend's health
 * per verse), the cage through the shared hard-control lock and {@code BalanceLaw.controlTicks}; nothing changes a block or spawns an entity, and every page
 * that hurts shows a ring or a gathering first. {@code revised} is the Revised Edition passive: 25 percent stronger within the limits of the page's kind.
 */
final class BookPages {
    private BookPages() {}

    // the palette: warm paper, the flame of the bird, the gold of the feathers
    private static final int PAPER = 0xFFF1E6C8, FLAME = 0xFFFF8A3C, GOLD = 0xFFFFE9A0, WHITE = 0xFFFFFFFF, THUNDER = 0xFFFFF5B0;
    private static final int PAPER_RGB = 0xF1E6C8, GOLD_RGB = 0xFFE9A0;

    /** Casts the page; false when it found nothing to act on. */
    static boolean cast(HeroCtx c, String page, boolean revised) {
        return switch (page) {
            case BookNumbers.BIRD -> bird(c, revised);
            case BookNumbers.NEEDLES -> needles(c, revised);
            case BookNumbers.FEATHERS -> feathers(c, revised);
            case BookNumbers.WALL -> wall(c, revised);
            case BookNumbers.VERSE -> verse(c, revised);
            case BookNumbers.CAGE -> cage(c, revised);
            default -> false;
        };
    }

    // ------------------------------------------------------------------ the offence chapter
    /** Bird of Flame: a bird made of pages flies down the line and burns up to four foes. */
    static boolean bird(HeroCtx c, boolean revised) {
        Vec3 eye = c.eye(), look = c.look();
        Vec3 end = GlassKit.clipEnd(c, eye, eye.add(look.scale(BookNumbers.BIRD_RANGE)));
        List<LivingEntity> foes = GlassKit.foesAlong(c, eye, end, BookNumbers.BIRD_WIDTH, BookNumbers.BIRD_TARGETS);
        if (foes.isEmpty()) {
            c.say("The bird finds nothing in its path.");
            return false;
        }
        double raw = BookNumbers.pageRaw(BookNumbers.BIRD_RAW, 1, revised);
        for (LivingEntity e : foes) GlassKit.strike(c, e, raw);
        Vec3 first = foes.get(0).getBoundingBox().getCenter();
        c.fx(VfxShape.WIND_GALE, eye.add(look.scale(0.8)), end, FLAME, 18, 1.2f);
        c.fx(VfxShape.FIRE_BURST, first, first, FLAME, 14, 0.8f);
        c.sound(SoundEvents.BLAZE_SHOOT, 0.8f, 1.2f);
        return true;
    }

    /** Needle Rain: a circle of paper opens over the aimed point and four volleys of needles fall through it. */
    static boolean needles(HeroCtx c, boolean revised) {
        Vec3 at = GlassKit.ground(c, c.aim(BookNumbers.NEEDLE_AIM), 8.0);
        if (GlassKit.foes(c, at, BookNumbers.NEEDLE_RADIUS + 1.0, 1).isEmpty()) {
            c.say("There are no foes under the page you aim at.");
            return false;
        }
        double raw = BookNumbers.pageRaw(BookNumbers.NEEDLE_RAW, BookNumbers.NEEDLE_PULSES, revised);
        GlassKit.ring(c, at, BookNumbers.NEEDLE_RADIUS, PAPER_RGB);
        c.fx(VfxShape.SEAL_BASIC, at, at, PAPER, BookNumbers.NEEDLE_DELAY + BookNumbers.NEEDLE_PULSES * BookNumbers.NEEDLE_INTERVAL, (float) BookNumbers.NEEDLE_RADIUS);
        c.sound(SoundEvents.BOOK_PAGE_TURN, 1.0f, 0.8f);
        GlassKit.later(c, BookNumbers.NEEDLE_DELAY, () -> GlassKit.pulses(c, BookNumbers.NEEDLE_PULSES, BookNumbers.NEEDLE_INTERVAL, i -> {
            List<LivingEntity> hit = GlassKit.foes(c, at, BookNumbers.NEEDLE_RADIUS, BookNumbers.NEEDLE_TARGETS);
            for (LivingEntity e : hit) GlassKit.strike(c, e, raw, true);                      // a quick series: every volley lands on a mob
            for (int k = 0; k < Math.min(4, hit.size()); k++) {
                Vec3 centre = hit.get(k).getBoundingBox().getCenter();
                c.fx(VfxShape.THREAD_LINE, centre.add(0.0, 5.0, 0.0), centre, WHITE, 6, 0.6f);
            }
            c.sound(SoundEvents.AMETHYST_CLUSTER_BREAK, 0.6f, 1.6f);
        }));
        return true;
    }

    /** Feather Volley: feathers gather, then fly as one beam down the line; a foe hit mends the wielder a little. */
    static boolean feathers(HeroCtx c, boolean revised) {
        Vec3 eye = c.eye(), look = c.look();
        Vec3 probe = GlassKit.clipEnd(c, eye, eye.add(look.scale(BookNumbers.FEATHER_RANGE)));
        if (GlassKit.foesAlong(c, eye, probe, BookNumbers.FEATHER_WIDTH, BookNumbers.FEATHER_TARGETS).isEmpty()) {
            c.say("The feathers have no foe in line.");
            return false;
        }
        double raw = BookNumbers.pageRaw(BookNumbers.FEATHER_RAW, 1, revised);
        Vec3 tip = eye.add(look.scale(0.9));
        Tells.gather(c.level, tip, 0.7, GOLD_RGB, 14);
        c.fx(VfxShape.STAR_MAGIC, tip, tip.add(look.scale(0.8)), GOLD, BookNumbers.FEATHER_DELAY + 2, 0.9f);
        c.sound(SoundEvents.ENCHANTMENT_TABLE_USE, 0.9f, 1.8f);
        GlassKit.later(c, BookNumbers.FEATHER_DELAY, () -> {
            Vec3 from = c.eye(), dir = c.look();
            Vec3 end = GlassKit.clipEnd(c, from, from.add(dir.scale(BookNumbers.FEATHER_RANGE)));
            int hits = 0;
            for (LivingEntity e : GlassKit.foesAlong(c, from, end, BookNumbers.FEATHER_WIDTH, BookNumbers.FEATHER_TARGETS)) if (GlassKit.strike(c, e, raw)) hits++;
            if (hits > 0) c.heal(c.player, (float) BookNumbers.featherHeal(c.player.getMaxHealth()));
            c.fx(VfxShape.LIGHT_BLADE, from.add(dir.scale(0.9)), end, GOLD, 10, 1.1f);
            c.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f, 1.6f);
        });
        return true;
    }

    // ------------------------------------------------------------------ the support chapter
    /** Paper Wall: a wall of paper wards you and your friends, a few hearts of absorption for ten seconds. */
    static boolean wall(HeroCtx c, boolean revised) {
        int ticks = (int) Math.round(BookNumbers.WALL_TICKS * (revised ? BookNumbers.REVISED : 1.0));
        for (LivingEntity f : GlassKit.friends(c, BookNumbers.WALL_RADIUS, BookNumbers.WALL_TARGETS)) {
            f.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, 0, false, true, true));
        }
        Vec3 feet = c.player.position(), front = GlassKit.chest(c.player).add(c.look().scale(1.4));
        c.fx(VfxShape.SEAL_BASIC, feet, feet, GOLD, 40, (float) BookNumbers.WALL_RADIUS);
        c.fx(VfxShape.MIRROR_PANE, front, front.add(c.look()), PAPER, 40, 1.6f);
        c.sound(SoundEvents.BOOK_PUT, 1.0f, 1.2f);
        return true;
    }

    /** Healing Verse: three pulses of healing for you and your friends, each friend never healed past a quarter of their health by the whole verse. */
    static boolean verse(HeroCtx c, boolean revised) {
        List<LivingEntity> friends = GlassKit.friends(c, BookNumbers.VERSE_RADIUS, BookNumbers.VERSE_TARGETS);
        Vec3 feet = c.player.position();
        c.fx(VfxShape.ELF_CIRCLE, feet, feet.add(0.0, 1.0, 0.0), GOLD, BookNumbers.VERSE_PULSES * BookNumbers.VERSE_INTERVAL, 2.4f);
        c.sound(SoundEvents.ALLAY_ITEM_GIVEN, 1.0f, 1.2f);
        GlassKit.pulses(c, BookNumbers.VERSE_PULSES, BookNumbers.VERSE_INTERVAL, i -> {
            for (LivingEntity f : friends) {
                if (!f.isAlive() || f.getHealth() >= f.getMaxHealth()) continue;
                c.heal(f, (float) BookNumbers.versePulse(f.getMaxHealth(), revised));
                if (f != c.player) c.fx(VfxShape.SEAL_WOUND, GlassKit.chest(c.player), f.getBoundingBox().getCenter(), GOLD, 16, 0.8f);
            }
        });
        return true;
    }

    /** Constriction and the False Ceiling: pages close into a cage round the foe on the crosshair (hard control), and needles fall through the ceiling. */
    static boolean cage(HeroCtx c, boolean revised) {
        LivingEntity target = GlassKit.focus(c, BookNumbers.CAGE_RANGE);
        if (target == null) {
            c.say("No foe on your crosshair to bind.");
            return false;
        }
        if (!GlassKit.control(c)) return false;
        int ticks = BalanceLaw.controlTicks(target, BookNumbers.CAGE_ROOT);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6));
        Vec3 centre = target.getBoundingBox().getCenter();
        c.fx(VfxShape.SEAL_CUBE, centre, centre, PAPER, ticks + BookNumbers.CAGE_CEILING_DELAY, (float) BookNumbers.CAGE_EDGE);
        c.sound(SoundEvents.BOOK_PAGE_TURN, 1.0f, 0.6f);
        double raw = BookNumbers.pageRaw(BookNumbers.CAGE_CEILING_RAW, 1, revised);
        GlassKit.later(c, BookNumbers.CAGE_CEILING_DELAY, () -> {
            if (!target.isAlive()) return;
            Vec3 now = target.getBoundingBox().getCenter();
            GlassKit.strike(c, target, raw);
            for (int k = 0; k < 5; k++) {
                double dx = (c.level.random.nextDouble() - 0.5) * 1.2, dz = (c.level.random.nextDouble() - 0.5) * 1.2;
                c.fx(VfxShape.THREAD_LINE, now.add(dx, 4.0, dz), now.add(dx, -0.4, dz), WHITE, 6, 0.5f);
            }
            c.sound(SoundEvents.AMETHYST_CLUSTER_BREAK, 0.8f, 1.2f);
        });
        return true;
    }

    // ------------------------------------------------------------------ the shows of Libraria
    /** One volley's effect on one foe: fire birds, lightning, boulders, a beam, in turn. */
    static void volleyShow(HeroCtx c, int volley, LivingEntity foe) {
        Vec3 centre = foe.getBoundingBox().getCenter(), chest = GlassKit.chest(c.player);
        switch (Math.floorMod(volley, 4)) {
            case 0 -> c.fx(VfxShape.WIND_GALE, chest, centre, FLAME, 14, 0.9f);
            case 1 -> c.fx(VfxShape.LIGHTNING_SPEAR, centre.add(0.0, 8.0, 0.0), centre, THUNDER, 8, 0.7f);
            case 2 -> c.fx(VfxShape.STONE_SPIKES, foe.position(), foe.position().add(0.0, 0.0, 0.8), PAPER, 12, 0.8f);
            default -> c.fx(VfxShape.LIGHT_BLADE, chest, centre, GOLD, 8, 0.9f);
        }
    }
}
