package com.newuniverse.nusmp.book.pages15;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.pages15.Pages15Rules.Tier;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Batch 6 of the "15 spells for every grimoire" project: Space, Time, Game and Mirror.
 *
 * <p>Books of this batch (the only books {@link Pages15} asks this class about; see {@link Pages15Plan}): {@code book_compass}, {@code book_copy}, {@code book_dice}, {@code book_slash}, {@code book_gravity}, {@code book_game}, {@code book_spatial}, {@code book_mirror}, {@code book_time}, {@code book_legion}, {@code book_key}, {@code book_kotodama}.
 *
 * <p>THE BUILDER OF THIS BATCH EDITS ONLY THIS FILE (plus tools/lang/pages15_batch6.json, src/test/resources/armour_policy/pages15_batch6_new_pages.json and docs/magic/pages15_batch6.md). How to write a
 * page: docs/magic/pages15_recipe.md. Shape of an entry, one {@code register} per book (the page counter script and the tests read this exact shape):
 * <pre>
 *     private static final List&lt;BookPage&gt; EXPLOSION = register(BOOK_NAME_HERE, List.of(
 *             Pages15Kit.mid(PAGE_ID_HERE, PAGE_NAME_HERE, Pages15Kit.nova(Tier.MID, 4, false, VfxShape.FLAME_EXPLOSION, Pages15Kit.ignite(3))).withCooldown(200)));
 * </pre>
 * (the real call takes string literals; they are not written out here because the armour-policy test scans the sources for page ids). Pages are appended AFTER every old page of the book, in the order
 * written here; never reorder or rename one once released (page ids are save keys).
 */
public final class Batch6 {
    private Batch6() {}

    /** Book registry path -> the pages this batch appends to it. */
    private static final Map<String, List<BookPage>> PAGES = new LinkedHashMap<>();

    private static List<BookPage> register(String book, List<BookPage> pages) {
        PAGES.put(book, List.copyOf(new ArrayList<>(pages)));
        return pages;
    }

    // ---------------------------------------------------------------- small helpers shared by the custom casts of this batch (all bounded; no state, no entities spawned)
    /** A target the caster may affect: alive, not the caster, not an ally. */
    private static boolean foe(Pages15Kit.Ctx c, LivingEntity t) { return t != null && t.isAlive() && t != c.caster && !t.isAlliedTo(c.caster); }

    private static void say(Pages15Kit.Ctx c, String text) { c.caster.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.GOLD), true); }

    /** Pushes {@code t} towards {@code to} (a pull) with a speed that never goes past {@code max} blocks per tick. */
    private static void pull(LivingEntity t, Vec3 to, double strength, double max) {
        Vec3 d = to.subtract(t.position());
        if (d.lengthSqr() < 0.04) return;
        Vec3 v = d.normalize().scale(Math.min(max, strength + d.length() * 0.05));
        t.setDeltaMovement(t.getDeltaMovement().add(v.x, 0.15, v.z));
        t.hurtMarked = true;
    }

    // ---------------------------------------------------------------- the books of this batch go here, one register(...) each

    // ---- Compass Magic (12 new): needles, bearings, lodestones and meridians; all original (the wiki lists only the three spells the book already has)
    private static final List<BookPage> COMPASS = register("book_compass", List.of(
            Pages15Kit.starter("needle_flick", "Needle Flick",
                    Pages15Kit.bolt(Tier.STARTER, 0, VfxShape.COMPASS_NEEDLE, null, Pages15Kit.NONE)).withAnim("thrust"),
            Pages15Kit.starter("bearing_step", "Bearing Step",
                    Pages15Kit.dash(Tier.STARTER, false, 8, false, VfxShape.COMPASS_LOCK, Pages15Kit.NONE)).withCooldown(100).withAnim("out"),
            Pages15Kit.starter("magnetic_pull", "Magnetic Pull", Pages15Kit.cast(Tier.STARTER, c -> {
                LivingEntity t = c.target(16);
                if (!foe(c, t)) return c.fail("The needle finds no signature to draw in.");
                pull(t, c.caster.position(), 0.35, 1.2);
                c.hit(t);
                c.circle(0.6f);
                c.fx(VfxShape.COMPASS_NEEDLE, c.caster.getEyePosition(), t.getBoundingBox().getCenter(), 14, 0.7f);
                c.fx(VfxShape.COMPASS_LOCK, t.position().add(0, 0.05, 0), t.position(), 24, 0.9f);
                return true;
            })).withCooldown(120),
            Pages15Kit.starter("true_heading", "True Heading",
                    Pages15Kit.empower(Tier.STARTER, 200, false, VfxShape.COMPASS_FX2, () -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0))).withCooldown(240).withAnim("up"),
            Pages15Kit.mid("lodestone_snare", "Lodestone Snare",
                    Pages15Kit.bind(Tier.MID, 18, false, VfxShape.COMPASS_NEEDLE, Pages15Kit.NONE)).withCooldown(260),
            Pages15Kit.mid("cardinal_volley", "Cardinal Volley",
                    Pages15Kit.volley(Tier.MID, 4, true, VfxShape.COMPASS_NEEDLE, Pages15Kit.NONE)),
            Pages15Kit.mid("declination_wave", "Declination Wave",
                    Pages15Kit.cone(Tier.MID, 8, 0.5, VfxShape.COMPASS_FX1, Pages15Kit.knock(1.0))).withAnim("sweep"),
            Pages15Kit.mid("dead_reckoning", "Dead Reckoning",
                    Pages15Kit.line(Tier.MID, 18, 1.0, VfxShape.COMPASS_NEEDLE, Pages15Kit.NONE)).withCooldown(200).withAnim("thrust"),
            Pages15Kit.mid("safe_bearing", "Safe Bearing",
                    Pages15Kit.healAllies(6, 0.12f)).withCooldown(400).withAnim("up"),
            Pages15Kit.zone("magnetic_storm", "Magnetic Storm",
                    Pages15Kit.field(Tier.ZONE, 5, 80, 10, true, VfxShape.COMPASS_FX3,
                            Pages15Kit.effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 24, 1)))).withCooldown(450),
            Pages15Kit.zone("meridian_lance", "Meridian Lance",
                    Pages15Kit.line(Tier.ZONE, 22, 1.8, VfxShape.COMPASS_NEEDLE, Pages15Kit.knock(0.5))).withCooldown(380).withAnim("thrust"),
            Pages15Kit.zone("cardinal_burst", "Cardinal Burst",
                    Pages15Kit.nova(Tier.ZONE, 6, false, VfxShape.COMPASS_FX2, Pages15Kit.knock(0.9))).withCooldown(400).withAnim("slam")));

    /** The pages this batch appends to the book (empty for a book it has not written yet, or a book of another batch). Never null. */
    static List<BookPage> pages(String bookPath) {
        return PAGES.getOrDefault(bookPath, List.of());
    }
}
