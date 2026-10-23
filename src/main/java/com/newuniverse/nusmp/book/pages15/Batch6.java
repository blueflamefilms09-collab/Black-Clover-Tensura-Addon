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

    // ---- Copy (Imitation) Magic (11 new): borrowing, echoing and reproducing; all original (the wiki has no spell pages for it)
    private static final List<BookPage> COPY = register("book_copy", List.of(
            Pages15Kit.starter("borrowed_strike", "Borrowed Strike",
                    Pages15Kit.cone(Tier.STARTER, 5, 0.6, VfxShape.IMITATION_FX1, Pages15Kit.knock(0.5))).withAnim("sweep"),
            Pages15Kit.starter("echo_step", "Echo Step",
                    Pages15Kit.dash(Tier.STARTER, false, 7, false, VfxShape.IMITATION_FX2, Pages15Kit.NONE)).withCooldown(100).withAnim("out"),
            Pages15Kit.starter("touch_and_learn", "Touch and Learn",
                    Pages15Kit.nova(Tier.STARTER, 3, false, VfxShape.IMITATION_FX1, Pages15Kit.leech(1.5f))).withCooldown(140).withAnim("slam"),
            Pages15Kit.starter("returned_blow", "Returned Blow",
                    Pages15Kit.empower(Tier.STARTER, 120, true, VfxShape.IMITATION_FX2)).withCooldown(220).withAnim("up"),
            Pages15Kit.mid("delayed_echo", "Delayed Echo", Pages15Kit.cast(Tier.MID, c -> {
                LivingEntity t = c.target(18);
                if (!foe(c, t)) return c.fail("There is nothing to echo against.");
                c.circle(0.7f);
                c.hit(t);
                c.fx(VfxShape.IMITATION_FX1, c.caster.getEyePosition(), t.getBoundingBox().getCenter(), 14, 0.8f);
                c.later(14, () -> {
                    if (!t.isAlive()) return;
                    c.hit(t, c.tier.hit * 0.6f);
                    c.fx(VfxShape.IMITATION_FX3, t.position().add(0, 0.05, 0), t.position().add(0, 1, 0), 20, 0.8f);
                });
                return true;
            })).withCooldown(200).withAnim("thrust"),
            Pages15Kit.mid("mimic_fan", "Mimic Fan",
                    Pages15Kit.cone(Tier.MID, 8, 0.5, VfxShape.IMITATION_FX3, Pages15Kit.effect(() -> new MobEffectInstance(MobEffects.WEAKNESS, 80, 0)))).withAnim("sweep"),
            Pages15Kit.mid("imitated_fetters", "Imitated Fetters",
                    Pages15Kit.bind(Tier.MID, 18, false, VfxShape.IMITATION_FX2, Pages15Kit.NONE)).withCooldown(260),
            Pages15Kit.mid("copycat_pounce", "Copycat Pounce",
                    Pages15Kit.dash(Tier.MID, true, 10, true, VfxShape.IMITATION_FX1, Pages15Kit.NONE)).withCooldown(180).withAnim("out"),
            Pages15Kit.zone("hall_of_echoes", "Hall of Echoes",
                    Pages15Kit.field(Tier.ZONE, 5, 80, 10, true, VfxShape.IMITATION_FX3,
                            Pages15Kit.effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 24, 0)))).withCooldown(450),
            Pages15Kit.zone("chorus_of_copies", "Chorus of Copies",
                    Pages15Kit.nova(Tier.ZONE, 6, true, VfxShape.IMITATION_FX3, Pages15Kit.knock(0.6))).withCooldown(380).withAnim("slam"),
            Pages15Kit.signature("perfect_reproduction", "Perfect Reproduction",
                    Pages15Kit.nova(Tier.SIGNATURE, 7, false, VfxShape.IMITATION_FX3, Pages15Kit.all(Pages15Kit.knock(0.8), Pages15Kit.strip()))).withCooldown(900).withAnim("slam")));

    // ---- Dice Magic (11 new): rolls, odds and loaded luck; "Gambler's Fallacy" (the wiki's only dice spell) is already a page, so every new page is original
    private static final List<BookPage> DICE = register("book_dice", List.of(
            Pages15Kit.starter("snake_eyes", "Snake Eyes",
                    Pages15Kit.bolt(Tier.STARTER, 1.5, VfxShape.DICE_D6, VfxShape.DICE_FX1, Pages15Kit.effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0)))).withAnim("thrust"),
            Pages15Kit.starter("pip_toss", "Pip Toss",
                    Pages15Kit.volley(Tier.STARTER, 3, false, VfxShape.DICE_D6, Pages15Kit.NONE)),
            Pages15Kit.starter("lucky_seven", "Lucky Seven",
                    Pages15Kit.empower(Tier.STARTER, 200, false, VfxShape.DICE_FX2, () -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0),
                            () -> new MobEffectInstance(MobEffects.LUCK, 200, 1))).withCooldown(260).withAnim("up"),
            Pages15Kit.starter("dice_cup_shake", "Dice Cup Shake",
                    Pages15Kit.cone(Tier.STARTER, 5, 0.5, VfxShape.DICE_FX3, Pages15Kit.knock(0.8))).withAnim("sweep"),
            Pages15Kit.mid("critical_roll", "Critical Roll", Pages15Kit.cast(Tier.MID, c -> {
                LivingEntity t = c.target(20);
                if (!foe(c, t)) return c.fail("There is nothing to roll against.");
                int face = 1 + c.caster.getRandom().nextInt(20);
                float scale = face == 20 ? 1.5f : face == 1 ? 0.2f : 0.45f + 0.5f * face / 20f;
                c.circle(0.7f);
                c.hit(t, c.tier.hit * scale);
                say(c, "d20: " + face + (face == 20 ? "  critical!" : face == 1 ? "  fumble" : ""));
                c.fx(VfxShape.DICE_FX2, c.caster.getEyePosition(), t.getBoundingBox().getCenter(), 16, 0.8f);
                c.impact(t.getBoundingBox().getCenter(), face == 20 ? 0.9f : 0.5f);
                return true;
            })).withCooldown(200).withAnim("thrust"),
            Pages15Kit.mid("double_or_nothing", "Double or Nothing", Pages15Kit.cast(Tier.MID, c -> {
                int a = 1 + c.caster.getRandom().nextInt(6), b = 1 + c.caster.getRandom().nextInt(6);
                Vec3 at = c.caster.position();
                c.circle(0.9f);
                float scale = a + b >= 7 ? 1.2f : 0.4f;
                for (LivingEntity t : c.foes(at, 6)) c.hit(t, c.tier.hit * scale);
                say(c, "Two dice: " + a + " + " + b + (a + b >= 7 ? "  double!" : "  nothing much"));
                c.fx(VfxShape.DICE_FX3, at, at.add(0, 1, 0), 24, a + b >= 7 ? 2f : 1f);
                return true;
            })).withCooldown(240).withAnim("slam"),
            Pages15Kit.mid("bowling_roll", "Bowling Roll",
                    Pages15Kit.line(Tier.MID, 16, 1.0, VfxShape.DICE_D6, Pages15Kit.knock(0.8))).withCooldown(200).withAnim("thrust"),
            Pages15Kit.mid("snare_roll", "Snare Roll",
                    Pages15Kit.bind(Tier.MID, 18, false, VfxShape.DICE_D20, Pages15Kit.NONE)).withCooldown(260),
            Pages15Kit.mid("reroll", "Reroll", Pages15Kit.cast(Tier.MID, c -> {
                int cleared = 0;
                for (MobEffectInstance e : new ArrayList<>(c.caster.getActiveEffects())) {
                    if (cleared >= 4) break;
                    if (!e.getEffect().value().isBeneficial()) { c.caster.removeEffect(e.getEffect()); cleared++; }
                }
                c.caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0));
                c.circle(0.8f);
                c.fx(VfxShape.DICE_FX1, c.caster.position(), c.caster.position().add(0, 1, 0), 24, 1f);
                return true;
            })).withCooldown(400).withAnim("up"),
            Pages15Kit.zone("craps_table", "Craps Table",
                    Pages15Kit.field(Tier.ZONE, 5, 80, 10, true, VfxShape.DICE_FX2,
                            Pages15Kit.effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 24, 0)))).withCooldown(450),
            Pages15Kit.zone("rain_of_dice", "Rain of Dice",
                    Pages15Kit.volley(Tier.ZONE, 5, true, VfxShape.DICE_D6, Pages15Kit.knock(0.4))).withCooldown(380).withAnim("up")));

    // ---- Slash Magic (11 new): Ripper Cut is named in Jack's character notes, Final Slash in the Japanese summaries; the rest is original
    private static final List<BookPage> SLASH = register("book_slash", List.of(
            Pages15Kit.starter("ripper_cut", "Ripper Cut",
                    Pages15Kit.cone(Tier.STARTER, 4, 0.6, VfxShape.SLASH_WAVE, Pages15Kit.NONE)).withAnim("sweep"),
            Pages15Kit.starter("cross_slash", "Cross Slash",
                    Pages15Kit.volley(Tier.STARTER, 2, false, VfxShape.SLASH_WAVE, Pages15Kit.NONE)).withAnim("sweep"),
            Pages15Kit.starter("blade_flurry", "Blade Flurry",
                    Pages15Kit.nova(Tier.STARTER, 3, false, VfxShape.SLASH_FX2, Pages15Kit.NONE)).withCooldown(100).withAnim("sweep"),
            Pages15Kit.starter("edge_guard", "Edge Guard",
                    Pages15Kit.empower(Tier.STARTER, 120, true, VfxShape.SLASH_BLADES)).withCooldown(240).withAnim("up"),
            Pages15Kit.mid("crescent_barrage", "Crescent Barrage",
                    Pages15Kit.volley(Tier.MID, 5, false, VfxShape.SLASH_WAVE, Pages15Kit.NONE)).withAnim("sweep"),
            Pages15Kit.mid("severing_line", "Severing Line",
                    Pages15Kit.line(Tier.MID, 14, 1.2, VfxShape.SLASH_WAVE, Pages15Kit.NONE)).withCooldown(200).withAnim("sweep"),
            Pages15Kit.mid("blade_cyclone", "Blade Cyclone",
                    Pages15Kit.nova(Tier.MID, 5, false, VfxShape.SLASH_FX3, Pages15Kit.lift(0.3))).withCooldown(240).withAnim("sweep"),
            Pages15Kit.mid("hamstring_cut", "Hamstring Cut",
                    Pages15Kit.bolt(Tier.MID, 0, VfxShape.SLASH_WAVE, VfxShape.SLASH_FX1, Pages15Kit.effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2)))).withCooldown(200).withAnim("sweep"),
            Pages15Kit.mid("parrying_blades", "Parrying Blades", Pages15Kit.cast(Tier.MID, c -> {
                // three seconds: arrows and bolts that come within 3.5 blocks are cut out of the air (at most 3 per step, a small box around the caster)
                boolean held = c.zone(60, 4, age -> {
                    int cut = 0;
                    for (Projectile pr : c.caster.serverLevel().getEntitiesOfClass(Projectile.class, c.caster.getBoundingBox().inflate(3.5), x -> x.getOwner() != c.caster)) {
                        if (cut++ >= 3) break;
                        pr.discard();
                    }
                });
                if (!held) return false;
                c.circle(0.8f);
                c.fx(VfxShape.SLASH_FX2, c.caster.position(), c.caster.position().add(0, 1, 0), 60, 1.2f);
                return true;
            })).withCooldown(400).withAnim("sweep"),
            Pages15Kit.zone("final_slash", "Final Slash",
                    Pages15Kit.line(Tier.ZONE, 20, 2.5, VfxShape.SLASH_WAVE, Pages15Kit.knock(0.7))).withCooldown(420).withAnim("sweep"),
            Pages15Kit.zone("reaper_field", "Reaper Field",
                    Pages15Kit.field(Tier.ZONE, 4, 60, 10, false, VfxShape.SLASH_FX2, Pages15Kit.NONE)).withCooldown(420)));

    /** The pages this batch appends to the book (empty for a book it has not written yet, or a book of another batch). Never null. */
    static List<BookPage> pages(String bookPath) {
        return PAGES.getOrDefault(bookPath, List.of());
    }
}
