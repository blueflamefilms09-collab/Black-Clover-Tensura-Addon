package com.newuniverse.nusmp.book.pages15;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.ElementBook;
import com.newuniverse.nusmp.book.pages15.Pages15Rules.Tier;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Batch 4 of the "15 spells for every grimoire" project: Earth, Stone and Metal.
 *
 * <p>Books of this batch (the only books {@link Pages15} asks this class about; see {@link Pages15Plan}): {@code book_mercury}, {@code book_sand}, {@code book_steel}, {@code book_rock}, {@code book_red_ochre}, {@code book_stone}, {@code book_mineral}, {@code book_sandstone}, {@code book_earth}, {@code book_shakudo}, {@code book_crystal}, {@code book_glass}, {@code book_iron}, {@code book_copper}, {@code book_corundum}, {@code book_bronze}.
 *
 * <p>96 new pages, all APPENDED after every old page of their book (ids are save keys, never renamed or reordered). Every page is a bounded server cast of the kit: damage by tier through the book's
 * balance law, at most the tier's targets, one zone of at most 100 ticks at 4+ tick steps, at most 10 effect packets to nearby players. The few custom casts below (guard, leap, pull, strikeAt, thornWall)
 * use only the kit's {@code Ctx} (bounded lists, {@code hit}, {@code fx}, {@code later}) and a self effect; none spawns an entity, scans beyond its own box or keeps state. Which pages are canon and which are original is in
 * docs/magic/pages15_batch4.md.
 */
public final class Batch4 {
    private Batch4() {}

    /** Book registry path -> the pages this batch appends to it. */
    private static final Map<String, List<BookPage>> PAGES = new LinkedHashMap<>();

    private static List<BookPage> register(String book, List<BookPage> pages) {
        PAGES.put(book, List.copyOf(new ArrayList<>(pages)));
        return pages;
    }

    // ---------------------------------------------------------------- riders (effects on a hit)
    private static ElementBook.Rider slow(int ticks, int amp) { return Pages15Kit.effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amp)); }
    private static ElementBook.Rider weak(int ticks, int amp) { return Pages15Kit.effect(() -> new MobEffectInstance(MobEffects.WEAKNESS, ticks, amp)); }
    private static ElementBook.Rider blind(int ticks, int amp) { return Pages15Kit.effect(() -> new MobEffectInstance(MobEffects.BLINDNESS, ticks, amp)); }
    private static ElementBook.Rider poison(int ticks, int amp) { return Pages15Kit.effect(() -> new MobEffectInstance(MobEffects.POISON, ticks, amp)); }
    private static ElementBook.Rider glow(int ticks) { return Pages15Kit.effect(() -> new MobEffectInstance(MobEffects.GLOWING, ticks, 0)); }
    private static ElementBook.Rider fatigue(int ticks, int amp) { return Pages15Kit.effect(() -> new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ticks, amp)); }

    // ---------------------------------------------------------------- the few custom casts (all bounded by the kit's Ctx)
    /** Self defence: Resistance (amp) and absorption hearts for the given ticks (at most 200). Worst case: 2 effects on the caster, 1 packet. */
    private static BookPage.Cast guard(Tier t, int ticks, int resist, int absorbHearts, VfxShape shape) {
        int tk = Math.max(20, Math.min(ticks, 200));
        return Pages15Kit.cast(t, c -> {
            c.circle(0.9f);
            c.caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, tk, Math.max(0, Math.min(resist, 1))));
            if (absorbHearts > 0) c.caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, tk, Math.max(0, Math.min(absorbHearts, 3) - 1)));
            Vec3 at = c.caster.position();
            c.fx(shape, at, at.add(0, 1, 0), Math.min(tk, 60), 1.2f);
            return true;
        });
    }

    /** A leap forward (reach in blocks, at most 10) that lands with a hit on the foes round the landing point 16 ticks later. Worst case: 1 delayed task, tier targets, 2 packets. */
    private static BookPage.Cast leap(Tier t, double reach, double radius, VfxShape shape, ElementBook.Rider rider) {
        double rc = Math.max(2.0, Math.min(reach, 10.0));
        return Pages15Kit.cast(t, c -> {
            ServerPlayer p = c.caster;
            Vec3 d = p.getViewVector(1f).multiply(1, 0, 1);
            d = d.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : d.normalize();
            c.circle(0.7f);
            p.setDeltaMovement(d.x * rc / 9.0, 0.75, d.z * rc / 9.0);
            p.hurtMarked = true;
            c.later(16, () -> {
                Vec3 at = p.position();
                p.fallDistance = 0f;
                for (LivingEntity e : c.foes(at, radius * c.size())) {
                    c.hit(e);
                    rider.apply(e, p);
                }
                c.fx(shape, at, at.add(0, 1, 0), 24, 1.4f);
                c.impact(at, 1f);
            });
            return true;
        });
    }

    /** Pulls the foes round the aim point toward it and hits them once. Worst case: tier targets, 1 packet, no task. */
    private static BookPage.Cast pull(Tier t, double range, double radius, VfxShape shape, ElementBook.Rider rider) {
        return Pages15Kit.cast(t, c -> {
            Vec3 at = c.aim(range);
            double r = radius * c.size();
            List<LivingEntity> foes = c.foes(at, r);
            if (foes.isEmpty()) return c.fail("No foes there.");
            c.circle(0.9f);
            for (LivingEntity e : foes) {
                Vec3 h = new Vec3(at.x - e.getX(), 0, at.z - e.getZ());
                double len = Math.max(1.0, h.length());
                e.setDeltaMovement(e.getDeltaMovement().add(h.x * 0.5 / len, 0.15, h.z * 0.5 / len));
                e.hurtMarked = true;
                c.hit(e);
                rider.apply(e, c.caster);
            }
            c.fx(shape, at, at.add(0, 1, 0), 30, (float) (r / 2.5));
            return true;
        });
    }

    /** A marked spot that strikes after a short delay (at most 100 ticks) on the foes within the radius (at most 8). Worst case: 1 delayed task, tier targets, 3 packets. */
    private static BookPage.Cast strikeAt(Tier t, int delay, double radius, VfxShape warn, VfxShape hitShape, ElementBook.Rider rider) {
        int dl = Math.max(4, Math.min(delay, 100));
        return Pages15Kit.cast(t, c -> {
            Vec3 at = c.aim(24);
            double r = Math.min(8.0, radius * c.size());
            c.circle(0.8f);
            c.fx(warn, at.add(0, 0.05, 0), at.add(0, 1, 0), dl + 10, (float) (r / 1.6));
            c.later(dl, () -> {
                for (LivingEntity e : c.foes(at, r)) {
                    c.hit(e);
                    rider.apply(e, c.caster);
                }
                c.fx(hitShape, at, at.add(0, 1, 0), 24, (float) (r / 3));
                c.impact(at, 1f);
            });
            return true;
        });
    }

    /** The kit's temporary wall (12 short-lived blocks) with thorns on the caster for the same time: the wall's attackers are cut. Worst case: 12 temp blocks, 1 packet. */
    private static BookPage.Cast thornWall(BlockState state, int ticks) {
        BookPage.Cast wall = ElementBook.wall(state);
        return (b, i, p, mode) -> {
            if (!wall.cast(b, i, p, mode)) return false;
            i.getOrCreateTag().putLong("ThornsUntil", p.level().getGameTime() + Math.min(ticks, 200));
            return true;
        };
    }

    // ---------------------------------------------------------------- the books of this batch, one register(...) each
    private static final List<BookPage> MERCURY = register("book_mercury", List.of(
                Pages15Kit.starter("quicksilver_pool", "Quicksilver Pool",
                        Pages15Kit.field(Tier.STARTER, 3.5, 60, 20, true, VfxShape.MERCURY_FX2, slow(60, 2))).withCooldown(160),
                Pages15Kit.starter("silver_needles", "Silver Needles",
                        Pages15Kit.volley(Tier.STARTER, 4, false, VfxShape.MERCURY_FX1, poison(60, 0))).withAnim("thrust"),
                Pages15Kit.starter("mercury_whip", "Mercury Whip",
                        Pages15Kit.line(Tier.STARTER, 9, 1.0, VfxShape.MERCURY_FX1, Pages15Kit.knock(0.5))).withAnim("sweep"),
                Pages15Kit.starter("silver_skate", "Silver Skate",
                        Pages15Kit.dash(Tier.STARTER, false, 9, false, VfxShape.MERCURY_FX1, Pages15Kit.NONE)).withCooldown(120).withAnim("out"),
                Pages15Kit.mid("silver_dome", "Silver Dome",
                        guard(Tier.MID, 120, 1, 2, VfxShape.MERCURY_DOME)).withCooldown(300).withAnim("up"),
                Pages15Kit.mid("silver_shackles", "Silver Shackles",
                        Pages15Kit.bind(Tier.MID, 18, false, VfxShape.MERCURY_FX1, weak(100, 0))).withAnim("thrust"),
                Pages15Kit.mid("silver_wave", "Silver Wave",
                        Pages15Kit.cone(Tier.MID, 8, 0.5, VfxShape.MERCURY_FX3, Pages15Kit.knock(0.9))).withAnim("sweep"),
                Pages15Kit.mid("toxic_quicksilver", "Toxic Quicksilver",
                        Pages15Kit.bolt(Tier.MID, 0, VfxShape.MERCURY_FX1, VfxShape.MERCURY_FX3, Pages15Kit.all(poison(100, 1), weak(100, 0)))).withAnim("thrust"),
                Pages15Kit.zone("liquid_metal_tide", "Liquid Metal Tide",
                        Pages15Kit.line(Tier.ZONE, 14, 3, VfxShape.MERCURY_FX1, Pages15Kit.all(Pages15Kit.knock(0.9), slow(60, 1)))).withAnim("sweep"),
                Pages15Kit.zone("mercury_vortex", "Mercury Vortex",
                        pull(Tier.ZONE, 18, 5, VfxShape.MERCURY_FX2, slow(40, 1))).withCooldown(360).withAnim("out"),
                Pages15Kit.signature("silver_wing_barrage", "Silver Wing Barrage",
                        Pages15Kit.volley(Tier.SIGNATURE, 5, true, VfxShape.MERCURY_SPEAR, poison(80, 0))).withCooldown(800).withAnim("out")));

    private static final List<BookPage> SAND = register("book_sand", List.of(
                Pages15Kit.starter("sand_bullets", "Sand Bullets",
                        Pages15Kit.volley(Tier.STARTER, 3, false, VfxShape.SAND_MAGIC, Pages15Kit.NONE)).withAnim("thrust"),
                Pages15Kit.starter("dust_veil", "Dust Veil",
                        Pages15Kit.nova(Tier.STARTER, 4, false, VfxShape.SAND_MAGIC, blind(60, 0))).withAnim("out"),
                Pages15Kit.starter("dune_step", "Dune Step",
                        Pages15Kit.dash(Tier.STARTER, false, 9, false, VfxShape.SAND_MAGIC, Pages15Kit.NONE)).withCooldown(120).withAnim("out"),
                Pages15Kit.starter("sand_whip", "Sand Whip",
                        Pages15Kit.line(Tier.STARTER, 9, 1.0, VfxShape.SAND_MAGIC, Pages15Kit.knock(0.4))).withAnim("sweep"),
                Pages15Kit.mid("sand_wall", "Sand Wall",
                        ElementBook.wall(ElementBook.block(Blocks.SANDSTONE))).withAnim("up"),
                Pages15Kit.mid("sand_mirage", "Sand Mirage",
                        ElementBook.decoy()).withAnim("out"),
                Pages15Kit.mid("scouring_sand", "Scouring Sand",
                        Pages15Kit.cone(Tier.MID, 7, 0.5, VfxShape.SAND_MAGIC, Pages15Kit.all(blind(60, 0), slow(40, 0)))).withAnim("sweep"),
                Pages15Kit.zone("sinking_dune", "Sinking Dune",
                        Pages15Kit.field(Tier.ZONE, 5, 80, 10, true, VfxShape.SAND_MAGIC, Pages15Kit.all(slow(40, 3), fatigue(40, 1)))).withAnim("slam"),
                Pages15Kit.zone("sand_cyclone", "Sand Cyclone",
                        pull(Tier.ZONE, 18, 5, VfxShape.SAND_MAGIC, blind(40, 0))).withCooldown(360).withAnim("out"),
                Pages15Kit.signature("dune_collapse", "Dune Collapse",
                        Pages15Kit.nova(Tier.SIGNATURE, 7, true, VfxShape.SAND_MAGIC, Pages15Kit.all(Pages15Kit.knock(1.0), slow(80, 1), blind(60, 0)))).withCooldown(800).withAnim("slam")));

    private static final List<BookPage> STEEL = register("book_steel", List.of(
                Pages15Kit.starter("steel_needle", "Steel Needle",
                        Pages15Kit.line(Tier.STARTER, 16, 0.6, VfxShape.EARTHFAM_CAST, Pages15Kit.NONE)).withAnim("thrust"),
                Pages15Kit.starter("steel_strike", "Steel Strike",
                        Pages15Kit.cone(Tier.STARTER, 3.5, 0.5, VfxShape.EARTHFAM_BURST, Pages15Kit.knock(0.8))).withAnim("slam"),
                Pages15Kit.starter("lance_vault", "Lance Vault",
                        leap(Tier.STARTER, 8, 3.5, VfxShape.EARTHFAM_BURST, Pages15Kit.knock(0.5))).withCooldown(160).withAnim("up"),
                Pages15Kit.mid("steel_chariot", "Steel Chariot",
                        Pages15Kit.dash(Tier.MID, true, 12, false, VfxShape.EARTHFAM_CAST, Pages15Kit.knock(0.6))).withCooldown(200).withAnim("out"),
                Pages15Kit.mid("steel_coffin", "Steel Coffin",
                        Pages15Kit.bind(Tier.MID, 16, false, VfxShape.EARTHFAM_ZONE, weak(100, 1))).withAnim("thrust"),
                Pages15Kit.mid("restraining_steel", "Restraining Steel",
                        Pages15Kit.field(Tier.MID, 4, 60, 20, true, VfxShape.EARTHFAM_ZONE, slow(60, 3))).withAnim("slam"),
                Pages15Kit.mid("steel_plating", "Steel Plating",
                        guard(Tier.MID, 200, 1, 2, VfxShape.EARTHFAM_ZONE)).withCooldown(400).withAnim("up"),
                Pages15Kit.zone("lance_downpour", "Lance Downpour",
                        Pages15Kit.field(Tier.ZONE, 5, 60, 12, true, VfxShape.EARTHFAM_CAST, Pages15Kit.knock(0.4))).withAnim("up"),
                Pages15Kit.zone("steel_blade_storm", "Steel Blade Storm",
                        Pages15Kit.cone(Tier.ZONE, 9, 0.3, VfxShape.EARTHFAM_BURST, Pages15Kit.knock(0.6))).withAnim("sweep"),
                Pages15Kit.signature("thousand_lances", "Thousand Lances",
                        Pages15Kit.volley(Tier.SIGNATURE, 5, true, VfxShape.EARTHFAM_CAST, Pages15Kit.knock(0.4))).withCooldown(800).withAnim("out")));

    private static final List<BookPage> ROCK = register("book_rock", List.of(
                Pages15Kit.starter("gravel_spray", "Gravel Spray",
                        Pages15Kit.cone(Tier.STARTER, 5, 0.5, VfxShape.EARTHFAM_BURST, slow(40, 0))).withAnim("sweep"),
                Pages15Kit.starter("flint_shards", "Flint Shards",
                        Pages15Kit.volley(Tier.STARTER, 4, false, VfxShape.EARTHFAM_CAST, Pages15Kit.NONE)).withAnim("thrust"),
                Pages15Kit.starter("rockslide_skate", "Rockslide Skate",
                        Pages15Kit.dash(Tier.STARTER, false, 8, false, VfxShape.EARTHFAM_CAST, Pages15Kit.NONE)).withCooldown(120).withAnim("out"),
                Pages15Kit.mid("rock_fortress", "Rock Fortress",
                        thornWall(ElementBook.block(Blocks.COBBLESTONE), 160)).withAnim("up"),
                Pages15Kit.mid("rolling_boulder", "Rolling Boulder",
                        Pages15Kit.line(Tier.MID, 12, 2, VfxShape.EARTHFAM_CAST, Pages15Kit.knock(1.0))).withAnim("slam"),
                Pages15Kit.mid("rock_hide", "Rock Hide",
                        Pages15Kit.empower(Tier.MID, 200, true, VfxShape.EARTHFAM_ZONE, () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0))).withCooldown(400).withAnim("up"),
                Pages15Kit.zone("avalanche_crush", "Avalanche Crush",
                        Pages15Kit.field(Tier.ZONE, 6, 60, 10, true, VfxShape.EARTHFAM_ZONE, Pages15Kit.all(Pages15Kit.knock(0.5), slow(40, 1)))).withAnim("slam"),
                Pages15Kit.signature("mountain_fall", "Mountain Fall",
                        Pages15Kit.nova(Tier.SIGNATURE, 8, true, VfxShape.EARTHFAM_BURST, Pages15Kit.all(Pages15Kit.knock(1.2), slow(80, 1)))).withCooldown(800).withAnim("slam")));

    private static final List<BookPage> RED_OCHRE = register("book_red_ochre", List.of(
                Pages15Kit.starter("ochre_dust", "Ochre Dust",
                        Pages15Kit.nova(Tier.STARTER, 4, false, VfxShape.EARTHFAM_BURST, weak(80, 0))).withAnim("out"),
                Pages15Kit.starter("pigment_darts", "Pigment Darts",
                        Pages15Kit.volley(Tier.STARTER, 3, false, VfxShape.EARTHFAM_CAST, glow(100))).withAnim("thrust"),
                Pages15Kit.starter("red_handprint", "Red Handprint",
                        Pages15Kit.bolt(Tier.STARTER, 0, VfxShape.EARTHFAM_CAST, VfxShape.EARTHFAM_BURST, Pages15Kit.all(glow(120), weak(80, 0)))).withAnim("thrust"),
                Pages15Kit.mid("ancestral_dance", "Ancestral Dance",
                        Pages15Kit.healAllies(8, 0.15f)).withCooldown(300).withAnim("up"),
                Pages15Kit.mid("ochre_spear", "Ochre Spear",
                        Pages15Kit.bolt(Tier.MID, 2.5, VfxShape.EARTHFAM_CAST, VfxShape.EARTHFAM_BURST, Pages15Kit.knock(0.6))).withAnim("thrust"),
                Pages15Kit.mid("ochre_wall", "Ochre Wall",
                        ElementBook.wall(ElementBook.block(Blocks.RED_TERRACOTTA))).withAnim("up"),
                Pages15Kit.zone("ochre_mire", "Ochre Mire",
                        Pages15Kit.field(Tier.ZONE, 5, 80, 10, true, VfxShape.EARTHFAM_ZONE, Pages15Kit.all(slow(40, 3), weak(60, 1)))).withAnim("slam"),
                Pages15Kit.signature("red_earth_rite", "Red Earth Rite",
                        Pages15Kit.nova(Tier.SIGNATURE, 7, false, VfxShape.EARTHFAM_ZONE, Pages15Kit.all(weak(120, 1), slow(80, 1), Pages15Kit.knock(0.8)))).withCooldown(800).withAnim("out")));

    private static final List<BookPage> STONE = register("book_stone", List.of(
                Pages15Kit.starter("pebble_shot", "Pebble Shot",
                        Pages15Kit.volley(Tier.STARTER, 3, false, VfxShape.EARTHFAM_CAST, Pages15Kit.NONE)).withAnim("thrust"),
                Pages15Kit.starter("stone_fist", "Stone Fist",
                        Pages15Kit.cone(Tier.STARTER, 3.5, 0.6, VfxShape.EARTHFAM_BURST, Pages15Kit.knock(0.9))).withAnim("slam"),
                Pages15Kit.starter("stone_skin", "Stone Skin",
                        Pages15Kit.empower(Tier.STARTER, 160, false, VfxShape.EARTHFAM_ZONE, () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0))).withCooldown(300).withAnim("up"),
                Pages15Kit.mid("granite_lance", "Granite Lance",
                        Pages15Kit.line(Tier.MID, 14, 1.2, VfxShape.STONE_SPIKES, Pages15Kit.lift(0.4))).withAnim("thrust"),
                Pages15Kit.mid("pillar_burst", "Pillar Burst",
                        Pages15Kit.nova(Tier.MID, 4, true, VfxShape.STONE_SPIKES, Pages15Kit.lift(0.7))).withAnim("slam"),
                Pages15Kit.mid("boulder_drop", "Boulder Drop",
                        strikeAt(Tier.MID, 20, 3.5, VfxShape.EARTHFAM_ZONE, VfxShape.EARTHFAM_BURST, Pages15Kit.knock(0.6))).withAnim("up"),
                Pages15Kit.zone("quarry_collapse", "Quarry Collapse",
                        Pages15Kit.field(Tier.ZONE, 6, 80, 10, true, VfxShape.STONE_SPIKES, slow(40, 1))).withAnim("slam"),
                Pages15Kit.signature("colossus_stampede", "Colossus Stampede",
                        Pages15Kit.line(Tier.SIGNATURE, 22, 3, VfxShape.STONE_SPIKES, Pages15Kit.all(Pages15Kit.knock(1.2), Pages15Kit.lift(0.5)))).withCooldown(800).withAnim("sweep")));

    private static final List<BookPage> MINERAL = register("book_mineral", List.of(
                Pages15Kit.starter("ore_shard_shot", "Ore Shard Shot",
                        Pages15Kit.bolt(Tier.STARTER, 0, VfxShape.EARTHFAM_CAST, VfxShape.EARTHFAM_BURST, Pages15Kit.NONE)).withAnim("thrust"),
                Pages15Kit.starter("mineral_assault", "Mineral Assault",
                        Pages15Kit.line(Tier.STARTER, 8, 1.5, VfxShape.EARTHFAM_CAST, Pages15Kit.lift(0.4))).withAnim("slam"),
                Pages15Kit.starter("ore_sight", "Ore Sight",
                        Pages15Kit.nova(Tier.STARTER, 5, false, VfxShape.EARTHFAM_CAST, glow(160))).withAnim("out"),
                Pages15Kit.mid("talos_doll", "Talos Doll",
                        ElementBook.decoy()).withAnim("out"),
                Pages15Kit.mid("heavy_armored_titan", "Heavy Armored Titan",
                        Pages15Kit.empower(Tier.MID, 240, true, VfxShape.EARTHFAM_ZONE, () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1), () -> new MobEffectInstance(MobEffects.ABSORPTION, 200, 1))).withCooldown(500).withAnim("up"),
                Pages15Kit.mid("geode_burst", "Geode Burst",
                        Pages15Kit.bolt(Tier.MID, 3.5, VfxShape.EARTHFAM_CAST, VfxShape.EARTHFAM_BURST, Pages15Kit.knock(0.5))).withAnim("thrust"),
                Pages15Kit.zone("talos_doll_army", "Talos Doll Army",
                        Pages15Kit.field(Tier.ZONE, 5, 80, 10, false, VfxShape.EARTHFAM_ZONE, Pages15Kit.knock(0.3))).withAnim("out"),
                Pages15Kit.signature("colossal_ore_fist", "Colossal Ore Fist",
                        Pages15Kit.line(Tier.SIGNATURE, 16, 3, VfxShape.EARTHFAM_BURST, Pages15Kit.all(Pages15Kit.knock(1.5), Pages15Kit.lift(0.4)))).withCooldown(800).withAnim("slam")));

    private static final List<BookPage> SANDSTONE = register("book_sandstone", List.of(
                Pages15Kit.starter("grit_slash", "Grit Slash",
                        Pages15Kit.cone(Tier.STARTER, 4.5, 0.5, VfxShape.EARTHFAM_BURST, blind(40, 0))).withAnim("sweep"),
                Pages15Kit.starter("sandstone_hurl", "Sandstone Hurl",
                        Pages15Kit.bolt(Tier.STARTER, 2, VfxShape.EARTHFAM_CAST, VfxShape.EARTHFAM_BURST, Pages15Kit.NONE)).withAnim("thrust"),
                Pages15Kit.starter("sandstone_leap", "Sandstone Leap",
                        leap(Tier.STARTER, 7, 3, VfxShape.EARTHFAM_BURST, slow(40, 0))).withCooldown(160).withAnim("up"),
                Pages15Kit.mid("rock_fall", "Rock Fall",
                        strikeAt(Tier.MID, 24, 4, VfxShape.EARTHFAM_ZONE, VfxShape.EARTHFAM_BURST, Pages15Kit.knock(0.5))).withAnim("up"),
                Pages15Kit.mid("sandstone_shell", "Sandstone Shell",
                        Pages15Kit.empower(Tier.MID, 200, true, VfxShape.EARTHFAM_ZONE, () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0))).withCooldown(400).withAnim("up"),
                Pages15Kit.mid("quarry_sweep", "Quarry Sweep",
                        Pages15Kit.line(Tier.MID, 12, 2, VfxShape.EARTHFAM_CAST, Pages15Kit.knock(0.8))).withAnim("sweep"),
                Pages15Kit.zone("rock_storm", "Rock Storm",
                        Pages15Kit.field(Tier.ZONE, 6, 80, 10, true, VfxShape.EARTHFAM_ZONE, Pages15Kit.all(Pages15Kit.knock(0.3), slow(40, 1)))).withAnim("out")));

    private static final List<BookPage> EARTH = register("book_earth", List.of(
                Pages15Kit.starter("rising_ground", "Rising Ground",
                        Pages15Kit.nova(Tier.STARTER, 3.5, true, VfxShape.EARTHFAM_BURST, Pages15Kit.lift(0.8))).withAnim("up"),
                Pages15Kit.starter("ground_wall", "Ground Wall",
                        guard(Tier.STARTER, 100, 1, 1, VfxShape.EARTHFAM_ZONE)).withCooldown(300).withAnim("up"),
                Pages15Kit.mid("mud_wall_cliff", "Mud Wall Cliff",
                        ElementBook.wall(ElementBook.block(Blocks.PACKED_MUD))).withAnim("up"),
                Pages15Kit.mid("dirt_clod_seal", "Dirt Clod Seal",
                        Pages15Kit.bind(Tier.MID, 14, false, VfxShape.EARTHFAM_ZONE, weak(80, 0))).withAnim("thrust"),
                Pages15Kit.zone("giant_king_of_wisdom", "Giant King of Wisdom",
                        Pages15Kit.field(Tier.ZONE, 6, 80, 10, false, VfxShape.EARTHFAM_ZONE, Pages15Kit.knock(0.7))).withAnim("slam"),
                Pages15Kit.zone("march_of_holy_fools", "March of Holy Fools",
                        Pages15Kit.line(Tier.ZONE, 18, 2.5, VfxShape.EARTH_RAMPAGE, Pages15Kit.all(Pages15Kit.knock(0.9), slow(60, 1)))).withAnim("sweep")));

    private static final List<BookPage> SHAKUDO = register("book_shakudo", List.of(
                Pages15Kit.starter("gilded_darts", "Gilded Darts",
                        Pages15Kit.volley(Tier.STARTER, 3, false, VfxShape.SHAKUDO_FX1, Pages15Kit.NONE)).withAnim("thrust"),
                Pages15Kit.starter("patina_step", "Patina Step",
                        Pages15Kit.dash(Tier.STARTER, false, 8, false, VfxShape.SHAKUDO_FX1, Pages15Kit.NONE)).withCooldown(120).withAnim("out"),
                Pages15Kit.mid("inlay_snare", "Inlay Snare",
                        Pages15Kit.bind(Tier.MID, 16, false, VfxShape.SHAKUDO_FX2, weak(100, 0))).withAnim("thrust"),
                Pages15Kit.mid("black_gold_wave", "Black Gold Wave",
                        Pages15Kit.cone(Tier.MID, 8, 0.5, VfxShape.SHAKUDO_FX3, Pages15Kit.knock(0.8))).withAnim("sweep"),
                Pages15Kit.zone("gilded_kiln", "Gilded Kiln",
                        Pages15Kit.field(Tier.ZONE, 5, 80, 10, false, VfxShape.SHAKUDO_FX2, Pages15Kit.ignite(3))).withAnim("out")));

    private static final List<BookPage> CRYSTAL = register("book_crystal", List.of(
                Pages15Kit.starter("refraction_veil", "Refraction Veil",
                        ElementBook.decoy()).withAnim("out"),
                Pages15Kit.mid("geode_trap", "Geode Trap",
                        strikeAt(Tier.MID, 24, 3.5, VfxShape.CRYSTAL_FX2, VfxShape.CRYSTAL_FX3, slow(60, 1))).withAnim("up"),
                Pages15Kit.zone("crystal_tide", "Crystal Tide",
                        Pages15Kit.line(Tier.ZONE, 16, 3, VfxShape.CRYSTAL_FX1, Pages15Kit.all(Pages15Kit.knock(0.8), slow(60, 1)))).withAnim("sweep")));

    private static final List<BookPage> GLASS = register("book_glass", List.of(
                Pages15Kit.starter("lens_flare", "Lens Flare",
                        Pages15Kit.nova(Tier.STARTER, 4, false, VfxShape.GLASS_FX3, blind(60, 0))).withAnim("out"),
                Pages15Kit.mid("glass_wall", "Glass Wall",
                        ElementBook.wall(ElementBook.block(Blocks.GLASS))).withAnim("up"),
                Pages15Kit.zone("stained_glass_field", "Stained Glass Field",
                        Pages15Kit.field(Tier.ZONE, 5, 80, 10, true, VfxShape.GLASS_FX2, Pages15Kit.all(slow(40, 1), blind(40, 0)))).withAnim("slam")));

    private static final List<BookPage> IRON = register("book_iron", List.of(
                Pages15Kit.starter("iron_filings", "Iron Filings",
                        Pages15Kit.cone(Tier.STARTER, 5, 0.5, VfxShape.IRON_FX3, Pages15Kit.all(slow(40, 0), blind(40, 0)))).withAnim("sweep"),
                Pages15Kit.mid("magnetic_pull", "Magnetic Pull",
                        pull(Tier.MID, 16, 4, VfxShape.IRON_FX2, Pages15Kit.NONE)).withAnim("out"),
                Pages15Kit.zone("iron_curtain", "Iron Curtain",
                        guard(Tier.ZONE, 160, 1, 3, VfxShape.IRON_FX2)).withCooldown(400).withAnim("up")));

    private static final List<BookPage> COPPER = register("book_copper", List.of(
                Pages15Kit.starter("spark_coil", "Spark Coil",
                        Pages15Kit.bolt(Tier.STARTER, 2, VfxShape.COPPER_FX1, VfxShape.COPPER_FX3, slow(30, 1))).withAnim("thrust"),
                Pages15Kit.mid("voltaic_pulse", "Voltaic Pulse",
                        Pages15Kit.nova(Tier.MID, 5, false, VfxShape.COPPER_FX3, Pages15Kit.all(Pages15Kit.knock(0.5), slow(40, 1)))).withAnim("out"),
                Pages15Kit.zone("lightning_rod", "Lightning Rod",
                        strikeAt(Tier.ZONE, 20, 4.5, VfxShape.COPPER_FX2, VfxShape.COPPER_FX3, slow(60, 1))).withAnim("up")));

    private static final List<BookPage> CORUNDUM = register("book_corundum", List.of(
                Pages15Kit.starter("ruby_flare", "Ruby Flare",
                        Pages15Kit.nova(Tier.STARTER, 3.5, false, VfxShape.CORUNDUM_FX3, Pages15Kit.ignite(2))).withAnim("out"),
                Pages15Kit.mid("sapphire_salve", "Sapphire Salve",
                        Pages15Kit.healAllies(8, 0.15f)).withCooldown(300).withAnim("up")));

    private static final List<BookPage> BRONZE = register("book_bronze", List.of(
                Pages15Kit.mid("bronze_gong", "Bronze Gong",
                        Pages15Kit.nova(Tier.MID, 5, false, VfxShape.BRONZE_FX3, Pages15Kit.all(Pages15Kit.knock(0.8), weak(80, 0)))).withAnim("slam")));

    /** The pages this batch appends to the book (empty for a book it has not written yet, or a book of another batch). Never null. */
    static List<BookPage> pages(String bookPath) {
        return PAGES.getOrDefault(bookPath, List.of());
    }
}
